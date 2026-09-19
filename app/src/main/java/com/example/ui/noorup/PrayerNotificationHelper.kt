package com.example.ui.noorup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object PrayerNotificationHelper {

    const val CHANNEL_ID_PRAYER = "noorup_prayer_channel"
    const val CHANNEL_ID_DAILY = "noorup_daily_reminder_channel"

    const val NOTIFICATION_ID_FAJR = 1001
    const val NOTIFICATION_ID_DHUHR = 1002
    const val NOTIFICATION_ID_ASR = 1003
    const val NOTIFICATION_ID_MAGHRIB = 1004
    const val NOTIFICATION_ID_ISHA = 1005
    const val NOTIFICATION_ID_SEHRI = 1006
    const val NOTIFICATION_ID_DAILY_HADITH = 1007
    const val NOTIFICATION_ID_GARDEN_REMINDER = 1008
    const val NOTIFICATION_ID_TEST = 9999

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. High Priority Channel for Prayer Times & Fasting Milestones
            val prayerChannel = NotificationChannel(
                CHANNEL_ID_PRAYER,
                "নামাজের ওয়াক্ত ও আজান রিমাইন্ডার (Prayer Times)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "নূরআপ অ্যাপের পাঁচ ওয়াক্ত নামাজের শুরু ও সেহরি-ইফতারের জরুরি সতর্কবার্তা"
                enableLights(true)
                lightColor = Color.parseColor("#10B981") // Emerald Green
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 250, 400, 250, 600)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                setSound(soundUri, audioAttributes)
            }

            // 2. Channel for Daily Islamic Hadith & Spiritual Reminders
            val dailyChannel = NotificationChannel(
                CHANNEL_ID_DAILY,
                "দৈনিক ইসলামিক হাদিস ও নসিহত (Daily Islamic Reminders)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "প্রতিদিনের প্রামাণ্য হাদিস, দোয়া ও আত্মশুদ্ধিমূলক বার্তা"
                enableLights(true)
                lightColor = Color.parseColor("#F59E0B") // Amber
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(prayerChannel)
            notificationManager.createNotificationChannel(dailyChannel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        channelId: String = CHANNEL_ID_PRAYER,
        subText: String? = null
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("opened_from_notification", true)
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_noorup_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText(subText ?: "নূরআপ • নূর ও শান্তি")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(0xFF10B981.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 250, 400, 250, 600))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission on Android 13+
        }
    }
}
