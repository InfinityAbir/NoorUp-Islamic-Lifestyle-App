package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.NoorUpWidgetProvider

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // Update widgets and calculate next exact schedule
            NoorUpWidgetProvider.updateAllWidgets(context)
            WidgetUpdateScheduler.scheduleNextWidgetUpdate(context)
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateWorker", "Error in background widget update worker", e)
        }
        return Result.success()
    }
}
