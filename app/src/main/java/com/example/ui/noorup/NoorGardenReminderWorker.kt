package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NoorGardenReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val isEnglish = prefs.getBoolean("is_english", false)

        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = PrayerNotificationScheduler.GARDEN_NOTIFICATION_ID,
            title = if (isEnglish) "🌱 Noor Garden (10:00 PM Reminder)" else "🌱 নূর বাগান (রাত ১০:০০ ঘটিকার অনুস্মারক)",
            message = if (isEnglish) "Complete today's prayers and daily deeds to keep your garden vibrant and green."
                      else "আজকের নামাজের ওয়াক্ত ও আমলগুলো সম্পূর্ণ করে বাগান সবুজ রাখুন।",
            channelId = "garden_reminders_channel"
        )
        // Ensure exact alarm for next day 10:00 PM is in place
        PrayerNotificationScheduler.scheduleGardenExactAlarm(context)
        return Result.success()
    }
}
