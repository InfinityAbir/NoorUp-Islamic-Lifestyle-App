package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PrayerRescheduleWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Full boot/update re-arm: prayers, daily garden+hadith tasks, widgets.
        // Runs off the receiver thread via WorkManager (see BootReceiver).
        PrayerNotificationHelper.createNotificationChannels(context)
        PrayerNotificationScheduler.scheduleAllPrayerAlerts(context)
        PrayerNotificationScheduler.scheduleDailyTasks(context)
        com.example.NoorUpWidgetProvider.updateAllWidgets(context)
        WidgetUpdateScheduler.scheduleNextWidgetUpdate(context)
        return Result.success()
    }

    companion object {
        private const val PERIODIC_TAG = "noorup_prayer_reschedule"

        /** Self-healing after force-stop (alarms cleared, no BOOT until reboot). */
        fun schedulePeriodic(context: Context) {
            try {
                val req = androidx.work.PeriodicWorkRequestBuilder<PrayerRescheduleWorker>(6, java.util.concurrent.TimeUnit.HOURS)
                    .addTag(PERIODIC_TAG)
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiresBatteryNotLow(false)
                            .build()
                    )
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_TAG,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    req
                )
            } catch (e: Exception) {
                android.util.Log.e("PrayerResched", "schedulePeriodic failed", e)
            }
        }
    }
}
