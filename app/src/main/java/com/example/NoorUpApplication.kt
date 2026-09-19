package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.ui.noorup.PrayerNotificationHelper

class NoorUpApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            PrayerNotificationHelper.createNotificationChannels(this)
            val prefs = getSharedPreferences("noorup_prefs", MODE_PRIVATE)
            if (prefs.getBoolean("prayer_notifications_enabled", true)) {
                com.example.ui.noorup.PrayerNotificationScheduler.scheduleDailyGardenReminder(this)
            }
        } catch (e: Exception) {
            Log.e("NoorUpApplication", "Error initializing notification channels", e)
        }
    }
}
