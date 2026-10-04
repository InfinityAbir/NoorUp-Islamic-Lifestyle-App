package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NoorUpWidgetProvider

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            PrayerNotificationHelper.createNotificationChannels(context)
            PrayerNotificationScheduler.scheduleAllPrayerAlerts(context)
            PrayerNotificationScheduler.scheduleDailyTasks(context)
            NoorUpWidgetProvider.updateAllWidgets(context)
            WidgetUpdateScheduler.scheduleNextWidgetUpdate(context)
        }
    }
}
