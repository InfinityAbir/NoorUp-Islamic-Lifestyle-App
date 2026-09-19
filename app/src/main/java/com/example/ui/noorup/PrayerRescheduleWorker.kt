package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PrayerRescheduleWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("prayer_notifications_enabled", true)
        if (enabled) {
            val lat = prefs.getFloat("last_lat", 23.8759f).toDouble()
            val lon = prefs.getFloat("last_lon", 90.3795f).toDouble()
            val isEng = prefs.getBoolean("is_english", false)
            PrayerNotificationScheduler.scheduleAllPrayerAlerts(
                context = appContext,
                latitude = lat,
                longitude = lon,
                isEnglish = isEng
            )
        }
        return Result.success()
    }
}
