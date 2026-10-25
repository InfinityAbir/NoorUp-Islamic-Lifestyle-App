package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.NoorUpWidgetProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Exact-alarm permission revoked while armed: re-arm path degrades to
        // inexact instead of crashing, and notifies nothing.
        if (intent.action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED") {
            try {
                PrayerNotificationScheduler.scheduleAllPrayerAlerts(context)
                PrayerNotificationScheduler.scheduleDailyTasks(context)
            } catch (_: Exception) { }
            return
        }
        val isGarden = intent.getBooleanExtra("is_garden_reminder", false)

        if (isGarden) {
            val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY) // 0..23

            // The reminder belongs to the evening slot it was scheduled for: a Doze/OEM
            // delay pushing 22:00 past midnight still counts as *yesterday's* instance,
            // so tonight's 10 PM reminder is never wrongly skipped.
            val slotCal = (cal.clone() as Calendar).apply {
                if (hour < 12) add(Calendar.DAY_OF_YEAR, -1)
            }
            val slotKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(slotCal.time)
            val lastPostedDate = prefs.getString("last_garden_reminder_date_posted", "")

            // 21:00-23:59 normal window, plus 00:00-04:00 grace for delayed alarms.
            // Anything later is genuinely stale and stays skipped (but logged).
            val isValidEveningHour = (hour in 21..23) || (hour in 0..4)
            val alreadyPostedToday = (lastPostedDate == slotKey)

            if (!isValidEveningHour) {
                Log.w("PrayerAlarmReceiver", "Garden reminder fired at stale hour $hour; skipping")
            }

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

                prefs.edit().putString("last_garden_reminder_date_posted", slotKey).apply()
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
                    channelId = "hadith_reminders_channel",
                    deepLinkHadithId = hadith.id
                )

                prefs.edit().putString("last_hadith_reminder_date_posted", todayDateKey).apply()
            }

            // Always ensure exact alarm is scheduled for the next 09:00 AM
            PrayerNotificationScheduler.scheduleDailyHadithExactAlarm(context)
            return
        }

        // Standard 5 Daily Prayer Alarms (Fajr, Dhuhr, Asr, Maghrib, Isha)
        val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("prayer_notifications_enabled", true)) {
            // User disabled alerts while this intent was in flight: do not notify.
            // Re-running the scheduler self-heals (it cancels when disabled).
            Log.d("PrayerAlarmReceiver", "Prayer alarm arrived while disabled; skipping post")
            PrayerNotificationScheduler.scheduleAllPrayerAlerts(context)
            return
        }
        val prayerName = intent.getStringExtra("prayer_name") ?: "নামাজ"
        val message = intent.getStringExtra("message") ?: "নামাজের ওয়াক্ত হয়েছে।"
        val channelId = intent.getStringExtra("channel_id") ?: "prayer_reminders_channel"
        // -1 = corrupt intent without an ID: never draw a random ID that could
        // overwrite a real prayer notification (old range overlapped 2000-2004).
        val notificationId = intent.getIntExtra("notification_id", -1)

        Log.d("PrayerAlarmReceiver", "Received prayer alarm for $prayerName (id=$notificationId)")

        if (notificationId != -1) {
            // Guarded: denied POST_NOTIFICATIONS = silent no-op, never claim Active.
            PrayerNotificationHelper.showPrayerAlert(
                context = context,
                notificationId = notificationId,
                title = prayerName,
                message = message,
                channelId = channelId
            )
        } else {
            Log.e("PrayerAlarmReceiver", "Prayer alarm missing notification_id; skipped post")
        }

        // Reschedule off the main thread: onReceive has a ~10s budget and solar
        // math + 5× setAlarmClock IPC + WorkManager DB must not ANR it.
        // PrayerRescheduleWorker already contains this body; use goAsync.
        val pending = goAsync()
        Thread {
            try {
                PrayerNotificationScheduler.scheduleAllPrayerAlerts(context)
                NoorUpWidgetProvider.updateAllWidgets(context)
                WidgetUpdateScheduler.scheduleNextWidgetUpdate(context)
            } catch (e: Exception) {
                Log.e("PrayerAlarmReceiver", "Error rescheduling prayers after alarm fire", e)
            } finally {
                try { pending.finish() } catch (_: Exception) { }
            }
        }.start()
    }
}
