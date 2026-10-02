package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NoorGardenReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Completely cancel any WorkManager jobs so no midnight/batch alerts fire
        try {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("noor_garden_daily_reminder")
        } catch (e: Exception) {
            // ignore
        }
        return Result.success()
    }
}
