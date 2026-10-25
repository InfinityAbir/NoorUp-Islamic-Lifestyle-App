package com.example.ui.noorup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object PrayerNotificationHelper {

    /** Tap-action contract for the daily-hadith notification (read by MainActivity). */
    const val EXTRA_OPEN_DAILY_HADITH = "open_daily_hadith"
    const val EXTRA_DAILY_HADITH_ID = "daily_hadith_id"
    /** Prayer and Garden notification taps always return to the Home dashboard. */
    const val EXTRA_OPEN_HOME = "open_home"

    /** Attaches the "open this hadith in the app" deep link to a tap intent. */
    fun putDailyHadithExtras(intent: Intent, hadithId: Int): Intent =
        intent
            .putExtra(EXTRA_OPEN_DAILY_HADITH, true)
            .putExtra(EXTRA_DAILY_HADITH_ID, hadithId)

    @Volatile
    private var channelsCreated = false

    fun areNotificationsEnabled(context: Context): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            } else {
                val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                mgr.areNotificationsEnabled()
            }
        } catch (_: Exception) {
            true
        }
    }

    fun createNotificationChannels(context: Context, force: Boolean = false) {
        if (channelsCreated && !force) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val prayerChannel = NotificationChannel(
                "prayer_reminders_channel",
                "Prayer Times & Adhan Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily high-priority prayer time notifications and on-time alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(defaultSoundUri, audioAttributes)
            }

            val gardenChannel = NotificationChannel(
                "garden_reminders_channel",
                "Noor Garden Daily Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily evening reminder to check and cultivate your Noor Garden"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val hadithChannel = NotificationChannel(
                "hadith_reminders_channel",
                "Daily Hadith & Spiritual Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily morning authentic Sahih Hadith for spiritual reflection"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(prayerChannel)
            manager.createNotificationChannel(gardenChannel)
            manager.createNotificationChannel(hadithChannel)
            channelsCreated = true
        } else {
            channelsCreated = true
        }
    }

    /**
     * Returns false when POST_NOTIFICATIONS is denied (Android 13+): callers must
     * not claim "Active" in that case. Never throws.
     *
     * Duplicate safety: real prayer alerts (IDs 2000–2004) can never fire twice
     * within a minute legitimately, so a repost inside [DEDUP_WINDOW_MS] is
     * dropped (protects against system/OEM double-delivery). Posting a new
     * prayer also clears the previous wakt's alert, so the shade never piles up.
     */
    fun showPrayerAlert(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        channelId: String = "prayer_reminders_channel",
        deepLinkHadithId: Int? = null
    ): Boolean {
        if (!areNotificationsEnabled(context)) {
            android.util.Log.w("PrayerNotify", "Suppressed notify #$notificationId: POST_NOTIFICATIONS denied")
            return false
        }
        val isRealPrayerAlert = notificationId in 2000..2004
        val prefs = try {
            context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) { null }
        val now = System.currentTimeMillis()
        if (isRealPrayerAlert && prefs != null) {
            val lastPost = try { prefs.getLong("last_post_$notificationId", 0L) } catch (_: Exception) { 0L }
            if (now - lastPost < DEDUP_WINDOW_MS) {
                android.util.Log.w("PrayerNotify", "Dropped duplicate post #$notificationId within dedup window")
                return false
            }
        }
        // Idempotent: guarantees the channel exists so a call on a fresh path
        // is never silently dropped on Android O+ (cached after first creation).
        createNotificationChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (deepLinkHadithId != null) {
                putDailyHadithExtras(this, deepLinkHadithId)
            } else {
                putExtra(EXTRA_OPEN_HOME, true)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_noorup_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.w("PrayerNotify", "notify() denied (permission revoked?)", e)
            return false
        }
        try {
            prefs?.edit()?.putLong("last_post_$notificationId", now)?.apply()
            if (isRealPrayerAlert) {
                // Only the latest wakt stays visible: clear the previous prayer alert.
                val lastPrayerId = prefs?.getInt("last_prayer_post_id", -1) ?: -1
                if (lastPrayerId != -1 && lastPrayerId != notificationId) {
                    try { manager.cancel(lastPrayerId) } catch (_: Exception) { }
                }
                prefs?.edit()?.putInt("last_prayer_post_id", notificationId)?.apply()
            }
        } catch (_: Exception) { }
        return true
    }

    /** Same real-prayer alert reposted inside this window is a duplicate. */
    const val DEDUP_WINDOW_MS = 60_000L
}
