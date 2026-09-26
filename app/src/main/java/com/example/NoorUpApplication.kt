package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NoorUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val prayerChannel = NotificationChannel(
                "prayer_reminders_channel",
                "Prayer Times & Adhan Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily prayer time notifications and countdown alerts"
            }

            val gardenChannel = NotificationChannel(
                "garden_reminders_channel",
                "Noor Garden Daily Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily evening reminder to check and cultivate your Noor Garden"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(prayerChannel)
            manager.createNotificationChannel(gardenChannel)
        }
    }
}
