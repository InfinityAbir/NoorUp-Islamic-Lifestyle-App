package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.NoorUpWidgetProvider

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NoorUp::PrayerAlarmWakeLock"
        )
        wakeLock?.acquire(15_000) // Hold partial wake lock for up to 15s to safely post alert

        try {
            val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("prayer_notifications_enabled", true)
            if (!enabled) {
                Log.i("PrayerAlarmReceiver", "Prayer notifications disabled by user. Ignoring alarm trigger.")
                return
            }

            val prayerType = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: "prayer"

            if (prayerType == "midnight_reschedule") {
                // Midnight auto-recalibration of prayer times for the new day
                Log.i("PrayerAlarmReceiver", "Midnight alarm triggered. Recalibrating all prayer alerts...")
                val lat = prefs.getFloat("last_lat", 23.8759f).toDouble()
                val lon = prefs.getFloat("last_lon", 90.3795f).toDouble()
                val isEng = prefs.getBoolean("is_english", false)
                PrayerNotificationScheduler.scheduleAllPrayerAlerts(
                    context = context,
                    latitude = lat,
                    longitude = lon,
                    isEnglish = isEng
                )
            } else if (prayerType == "garden_reminder") {
                Log.i("PrayerAlarmReceiver", "10:00 PM Exact Garden/Prayer Check alarm fired. Evaluating progress...")
                val todayDateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                val mandatoryPrayers = listOf("ফজর", "যোহর", "আসর", "মাগরিব", "এশা")
                val completedCount = mandatoryPrayers.count { prayer ->
                    prefs.getBoolean("prayer_habit_${todayDateKey}_$prayer", false)
                }
                val isEng = prefs.getBoolean("is_english", false)
                val isAllCompleted = completedCount >= 5

                val title = if (isEng) {
                    if (isAllCompleted) "Noor Garden Complete! 🌱" else "Noor Garden Evening Check"
                } else {
                    if (isAllCompleted) "মাশাআল্লাহ! নূর বাগান পূর্ণাঙ্গ 🌱" else "নূর বাগান ও নামাজের হিসাব"
                }

                val message = if (isEng) {
                    if (isAllCompleted) "Alhamdulillah! All 5 mandatory prayers logged today (5/5). Your garden is flourishing with divine blessings!"
                    else "Have you logged all today's prayers in your Noor Garden? Tap to review ($completedCount/5 logged)."
                } else {
                    if (isAllCompleted) "আলহামদুলিল্লাহ! আজকের সকল ৫ ওয়াক্ত নামাজ সম্পন্ন হয়েছে (৫/৫)। আপনার নূর বাগান সবুজে প্রস্ফুটিত!"
                    else "আজকের ওয়াক্তগুলো কি আদায় করা হয়েছে? নূর বাগানে নামাজের হিসাব লগ করুন ($completedCount/৫ সম্পন্ন)।"
                }

                val subText = if (isEng) "Noor Garden • Evening Check" else "নূর বাগান • রাতের পর্যালোচনা"

                // 1. Post native notification immediately with sound & vibration
                PrayerNotificationHelper.showNotification(
                    context = context,
                    title = title,
                    message = message,
                    notificationId = PrayerNotificationHelper.NOTIFICATION_ID_GARDEN_REMINDER,
                    channelId = PrayerNotificationHelper.CHANNEL_ID_DAILY,
                    subText = subText
                )

                // 2. Reschedule for tomorrow's 10:00 PM
                PrayerNotificationScheduler.scheduleDailyGardenReminder(context)
            } else {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "নামাজের ওয়াক্ত হয়েছে"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "সালাত আদায়ের প্রস্তুতি গ্রহণ করুন।"
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, PrayerNotificationHelper.NOTIFICATION_ID_FAJR)
                val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: PrayerNotificationHelper.CHANNEL_ID_PRAYER
                val subText = intent.getStringExtra(EXTRA_SUBTEXT) ?: "নূরআপ • নূর ও শান্তি"

                Log.i("PrayerAlarmReceiver", "Exact alarm fired on time for: $prayerType. Posting notification...")

                // 1. Post native notification immediately with sound & vibration
                PrayerNotificationHelper.showNotification(
                    context = context,
                    title = title,
                    message = message,
                    notificationId = notificationId,
                    channelId = channelId,
                    subText = subText
                )

                // 2. Synchronize all home screen widgets
                try {
                    NoorUpWidgetProvider.updateAllWidgets(context)
                } catch (e: Exception) {
                    Log.e("PrayerAlarmReceiver", "Error updating widget", e)
                }

                // 3. Reschedule all prayers so next upcoming occurrence is ready
                val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
                val enabled = prefs.getBoolean("prayer_notifications_enabled", true)
                if (enabled) {
                    val lat = prefs.getFloat("last_lat", 23.8759f).toDouble()
                    val lon = prefs.getFloat("last_lon", 90.3795f).toDouble()
                    val isEng = prefs.getBoolean("is_english", false)
                    PrayerNotificationScheduler.scheduleAllPrayerAlerts(
                        context = context,
                        latitude = lat,
                        longitude = lon,
                        isEnglish = isEng
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PrayerAlarmReceiver", "Exception in onReceive", e)
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.noorup.ACTION_PRAYER_ALARM"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_SUBTEXT = "extra_subtext"
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
    }
}
