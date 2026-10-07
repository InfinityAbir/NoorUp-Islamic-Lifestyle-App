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
        }.start()
    }
}
