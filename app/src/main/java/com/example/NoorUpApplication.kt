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
        registerScreenOnWidgetRefresh()
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

    /**
     * Alarms can be late under Doze or aggressive battery managers. Whenever the user wakes or
     * unlocks the phone (the moment they actually look at the widget) re-render it, so a
     * prayer-window change that was missed is corrected immediately. These broadcasts cannot be
     * declared in the manifest, so the receiver lives as long as the app process does.
     */
    private fun registerScreenOnWidgetRefresh() {
        try {
            val appContext = applicationContext
            val receiver = object : android.content.BroadcastReceiver() {
                @Volatile private var lastRefresh = 0L
                override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastRefresh < 15_000L) return
                    lastRefresh = now
                    Thread {
                        try {
                            NoorUpWidgetProvider.updateAllWidgets(appContext)
                            WidgetUpdateScheduler.scheduleNextWidgetUpdate(appContext)
                        } catch (e: Exception) {
                            android.util.Log.e("NoorUpApp", "screen-on widget refresh failed", e)
                        }
                    }.start()
                }
            }
            val filter = android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_SCREEN_ON)
                addAction(android.content.Intent.ACTION_USER_PRESENT)
            }
            androidx.core.content.ContextCompat.registerReceiver(
                this, receiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            android.util.Log.e("NoorUpApp", "registering screen-on widget refresh failed", e)
        }
    }
}
