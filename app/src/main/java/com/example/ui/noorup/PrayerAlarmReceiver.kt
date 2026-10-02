package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isGarden = intent.getBooleanExtra("is_garden_reminder", false)

        if (isGarden) {
            val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastPostedDate = prefs.getString("last_garden_reminder_date_posted", "")
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY) // 0..23

            // Strictly allow only 1 reminder per day at 10 PM (21:00 - 23:59), never at midnight/early morning (0:00 - 6:00)
            val isValidEveningHour = (hour in 21..23)
            val alreadyPostedToday = (lastPostedDate == todayDateKey)

            if (!alreadyPostedToday && isValidEveningHour) {
                val isEnglish = prefs.getBoolean("is_english", false)
                val prayerName = intent.getStringExtra("prayer_name")
                    ?: if (isEnglish) "🌱 Noor Garden (10:00 PM Reminder)" else "🌱 নূর বাগান (রাত ১০:০০ ঘটিকার অনুস্মারক)"
                val message = intent.getStringExtra("message")
                    ?: if (isEnglish) "Complete today's prayers and daily deeds to keep your garden vibrant and green."
                    else "আজকের নামাজের ওয়াক্ত ও আমলগুলো সম্পূর্ণ করে বাগান সবুজ রাখুন।"

                PrayerNotificationHelper.showPrayerAlert(
                    context = context,
                    notificationId = PrayerNotificationScheduler.GARDEN_NOTIFICATION_ID,
                    title = prayerName,
                    message = message,
                    channelId = "garden_reminders_channel"
                )

                prefs.edit().putString("last_garden_reminder_date_posted", todayDateKey).apply()
            }

            // Always ensure exact alarm is scheduled for the next 10:00 PM
            PrayerNotificationScheduler.scheduleGardenExactAlarm(context)
            return
        }

        val isHadith = intent.getBooleanExtra("is_hadith_reminder", false)
        if (isHadith) {
            val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastPostedDate = prefs.getString("last_hadith_reminder_date_posted", "")
            val isEnglish = prefs.getBoolean("is_english", false)

            val alreadyPostedToday = (lastPostedDate == todayDateKey)
            if (!alreadyPostedToday) {
                val hadith = com.example.ui.noorup.hadith.DailyHadithProvider.getTodayHadith()
                val title = if (isEnglish) "📖 Daily Hadith • ${hadith.referenceEn}" else "📖 আজকের নূর হাদিস • ${hadith.referenceBn}"
                val message = if (isEnglish) {
                    "\"${hadith.textEn}\"\n— ${hadith.narratorEn}\n💡 ${hadith.lessonEn}"
                } else {
                    "\"${hadith.textBn}\"\n— ${hadith.narratorBn}\n💡 ${hadith.lessonBn}"
                }

                PrayerNotificationHelper.showPrayerAlert(
                    context = context,
                    notificationId = PrayerNotificationScheduler.HADITH_NOTIFICATION_ID,
                    title = title,
                    message = message,
                    channelId = "hadith_reminders_channel"
                )

                prefs.edit().putString("last_hadith_reminder_date_posted", todayDateKey).apply()
            }

            // Always ensure exact alarm is scheduled for the next 09:00 AM
            PrayerNotificationScheduler.scheduleDailyHadithExactAlarm(context)
            return
        }

        // Standard 5 Daily Prayer Alarms
        val prayerName = intent.getStringExtra("prayer_name") ?: "নামাজ"
        val message = intent.getStringExtra("message") ?: "নামাজের ওয়াক্ত হয়েছে।"
        val channelId = intent.getStringExtra("channel_id") ?: "prayer_reminders_channel"
        val notificationId = intent.getIntExtra("notification_id", (2000..3000).random())

        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = notificationId,
            title = prayerName,
            message = message,
            channelId = channelId
        )
    }
}
