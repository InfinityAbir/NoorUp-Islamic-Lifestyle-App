package com.example

import android.app.Application
import com.example.ui.noorup.PrayerNotificationHelper
import com.example.ui.noorup.PrayerNotificationScheduler
import com.example.ui.noorup.WidgetUpdateScheduler

class NoorUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Notification channels are cheap — keep on main thread.
        PrayerNotificationHelper.createNotificationChannels(this)
        // Backup-restore sanity: a restored last_garden/hadith date of *today* would
        // suppress today's reminders on a fresh install that never showed them.
        // Future-dated entries (clock change/restore) are always stale — drop them.
        try {
            val prefs = getSharedPreferences("noorup_prefs", MODE_PRIVATE)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            val lastGarden = prefs.getString("last_garden_reminder_date_posted", "")
            val lastHadith = prefs.getString("last_hadith_reminder_date_posted", "")
            val ed = prefs.edit()
            var changed = false
            if (!lastGarden.isNullOrEmpty() && lastGarden > today) {
                ed.remove("last_garden_reminder_date_posted"); changed = true
            }
            if (!lastHadith.isNullOrEmpty() && lastHadith > today) {
                ed.remove("last_hadith_reminder_date_posted"); changed = true
            }
            if (changed) ed.apply()
        } catch (_: Exception) { }
        // AlarmManager + WorkManager + widget math are slow on first run
        // (emulator ANR risk) — run them off the main thread.
        val appContext = applicationContext
        Thread {
            try {
                PrayerNotificationScheduler.scheduleAllPrayerAlerts(appContext)
            } catch (e: Exception) {
                android.util.Log.e("NoorUpApp", "scheduleAllPrayerAlerts failed", e)
            }
            try {
                PrayerNotificationScheduler.scheduleDailyTasks(appContext)
            } catch (e: Exception) {
                android.util.Log.e("NoorUpApp", "scheduleDailyTasks failed", e)
            }
            try {
                NoorUpWidgetProvider.updateAllWidgets(appContext)
            } catch (e: Exception) {
                android.util.Log.e("NoorUpApp", "updateAllWidgets failed", e)
            }
            try {
                WidgetUpdateScheduler.scheduleNextWidgetUpdate(appContext)
            } catch (e: Exception) {
                android.util.Log.e("NoorUpApp", "scheduleNextWidgetUpdate failed", e)
            }
            // Self-healing: force-stop clears all alarms with no BOOT broadcast until
            // next reboot. Enqueue a periodic rescheduler as a safety net.
            try {
                com.example.ui.noorup.PrayerRescheduleWorker.schedulePeriodic(appContext)
            } catch (_: Exception) { }
        }.start()
    }
}
