package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PrayerRescheduleWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        PrayerNotificationScheduler.scheduleDailyTasks(context)
        return Result.success()
    }
}
