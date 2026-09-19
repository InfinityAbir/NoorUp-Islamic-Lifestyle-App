package com.example.ui.noorup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.NoorUpWidgetProvider

class PrayerNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("prayer_notifications_enabled", true)
        if (!notificationsEnabled) {
            android.util.Log.i("PrayerNotificationWorker", "Prayer notifications are disabled by user. Suppressing alert.")
            return Result.success()
        }

        val title = inputData.getString(KEY_TITLE) ?: "নামাজের ওয়াক্ত হয়েছে"
        val message = inputData.getString(KEY_MESSAGE) ?: "সালাত আদায়ের প্রস্তুতি গ্রহণ করুন।"
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, PrayerNotificationHelper.NOTIFICATION_ID_FAJR)
        val channelId = inputData.getString(KEY_CHANNEL_ID) ?: PrayerNotificationHelper.CHANNEL_ID_PRAYER
        val subText = inputData.getString(KEY_SUBTEXT) ?: "নূরআপ • নূর ও শান্তি"

        // Display the native tray notification with high priority
        PrayerNotificationHelper.showNotification(
            context = appContext,
            title = title,
            message = message,
            notificationId = notificationId,
            channelId = channelId,
            subText = subText
        )

        // Update home screen widget in sync with prayer status
        try {
            NoorUpWidgetProvider.updateAllWidgets(appContext)
        } catch (e: Exception) {
            // Non-fatal if widget update fails
        }

        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "key_notification_title"
        const val KEY_MESSAGE = "key_notification_message"
        const val KEY_NOTIFICATION_ID = "key_notification_id"
        const val KEY_CHANNEL_ID = "key_notification_channel_id"
        const val KEY_SUBTEXT = "key_notification_subtext"
        const val KEY_PRAYER_TYPE = "key_prayer_type"
    }
}
