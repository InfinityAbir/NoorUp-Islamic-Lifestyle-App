package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NoorUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.ui.noorup.PrayerNotificationHelper.createNotificationChannels(this)
        NoorUpWidgetProvider.updateAllWidgets(this)
        com.example.ui.noorup.WidgetUpdateScheduler.scheduleNextWidgetUpdate(this)
        com.example.ui.noorup.PrayerNotificationScheduler.scheduleDailyTasks(this)
    }
}
