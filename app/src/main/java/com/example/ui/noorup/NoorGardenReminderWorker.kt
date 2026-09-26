package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NoorGardenReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = 1001,
            title = "🌱 নূর বাগান (Noor Garden)",
            message = "আজকের নামাজের ওয়াক্ত ও আমলগুলো সম্পূর্ণ করে বাগান সবুজ রাখুন।",
            channelId = "garden_reminders_channel"
        )
        return Result.success()
    }
}
