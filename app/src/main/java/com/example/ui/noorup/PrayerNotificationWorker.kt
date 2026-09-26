package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PrayerNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prayerName = inputData.getString("prayer_name") ?: "ওয়াক্তের নামাজ"
        val message = inputData.getString("message") ?: "নামাজের ওয়াক্ত শুরু হয়েছে।"

        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = (100..999).random(),
            title = prayerName,
            message = message
        )
        return Result.success()
    }
}
