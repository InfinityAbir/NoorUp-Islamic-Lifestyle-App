package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "নামাজ"
        val message = intent.getStringExtra("message") ?: "নামাজের ওয়াক্ত হয়েছে।"
        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = (2000..3000).random(),
            title = prayerName,
            message = message
        )
    }
}
