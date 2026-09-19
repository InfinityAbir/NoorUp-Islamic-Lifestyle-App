package com.example.ui.noorup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily 10:00 PM WorkManager task to check if the user has completed and logged
 * all 5 daily mandatory prayers in their Noor Garden.
 * If incomplete or unlogged (< 5 prayers), prompts the user with an encouraging reminder.
 */
class NoorGardenReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NoorGardenReminder"
    }

    override suspend fun doWork(): Result {
        try {
            val prefs = appContext.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            val isEnglish = prefs.getBoolean("is_english", false)
            val notificationsEnabled = prefs.getBoolean("prayer_notifications_enabled", true)

            // Current date key (e.g., "2026-09-10")
            val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val mandatoryPrayers = listOf("ফজর", "যোহর", "আসর", "মাগরিব", "এশা")

            // Count prayers explicitly checked by the user today
            val completedCount = mandatoryPrayers.count { prayer ->
                prefs.getBoolean("prayer_habit_${todayDateKey}_$prayer", false)
            }

            Log.i(TAG, "10:00 PM Noor Garden check executed. Date: $todayDateKey, Completed: $completedCount / 5")

            // If user has not logged all 5 prayers, post reminder notification
            if (completedCount < 5 && notificationsEnabled) {
                val title = if (isEnglish) "Update Your Noor Garden" else "নূর বাগান আপডেট করুন"
                val message = if (isEnglish) {
                    "Update your Noor Garden: Have today's prayers been completed? Log quickly to keep your garden growing ($completedCount/5 completed)."
                } else {
                    "নূর বাগান আপডেট করুন: আজকের ওয়াক্তগুলো কি আদায় করা হয়েছে? আপনার বাগান টিকিয়ে রাখতে দ্রুত লগ করুন।"
                }
                val subText = if (isEnglish) "Noor Garden • Daily Reminder" else "নূর বাগান • দৈনিক রিমাইন্ডার"

                PrayerNotificationHelper.showNotification(
                    context = appContext,
                    title = title,
                    message = message,
                    notificationId = PrayerNotificationHelper.NOTIFICATION_ID_GARDEN_REMINDER,
                    channelId = PrayerNotificationHelper.CHANNEL_ID_DAILY,
                    subText = subText
                )
            }

            // Reschedule for the next day's 10:00 PM
            PrayerNotificationScheduler.scheduleDailyGardenReminder(appContext)

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing 10:00 PM garden reminder check", e)
            return Result.failure()
        }
    }
}
