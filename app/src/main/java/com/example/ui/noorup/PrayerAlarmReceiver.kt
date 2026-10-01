package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isGarden = intent.getBooleanExtra("is_garden_reminder", false)
        val prayerName = intent.getStringExtra("prayer_name") ?: if (isGarden) "🌱 নূর বাগান (Noor Garden)" else "নামাজ"
        val message = intent.getStringExtra("message") ?: if (isGarden) "আজকের নামাজের ওয়াক্ত ও আমলগুলো সম্পূর্ণ করে বাগান সবুজ রাখুন।" else "নামাজের ওয়াক্ত হয়েছে।"
        val channelId = intent.getStringExtra("channel_id") ?: if (isGarden) "garden_reminders_channel" else "prayer_reminders_channel"
        val notificationId = intent.getIntExtra("notification_id", if (isGarden) PrayerNotificationScheduler.GARDEN_NOTIFICATION_ID else (2000..3000).random())

        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = notificationId,
            title = prayerName,
            message = message,
            channelId = channelId
        )

        if (isGarden) {
            // Automatically schedule the next day's 10:00 PM exact alarm
            PrayerNotificationScheduler.scheduleGardenExactAlarm(context)
        }
    }
}
