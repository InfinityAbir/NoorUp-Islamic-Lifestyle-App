package com.example

import android.app.Application
import com.example.ui.noorup.PrayerNotificationHelper
import com.example.ui.noorup.PrayerNotificationScheduler
import com.example.ui.noorup.WidgetUpdateScheduler

class NoorUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrayerNotificationHelper.createNotificationChannels(this)
        PrayerNotificationScheduler.scheduleAllPrayerAlerts(this)
        PrayerNotificationScheduler.scheduleDailyTasks(this)
        NoorUpWidgetProvider.updateAllWidgets(this)
        WidgetUpdateScheduler.scheduleNextWidgetUpdate(this)
    }
}
