package com.example.ui.noorup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object PrayerNotificationScheduler {

    private const val ALARM_REQ_BASE = 5000

    fun scheduleDailyTasks(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val gardenRequest = PeriodicWorkRequestBuilder<NoorGardenReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(6, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "noor_garden_daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            gardenRequest
        )
    }

    fun isExactAlarmPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun scheduleAllPrayerAlerts(
        context: Context,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        juristic: JuristicMethod,
        isEnglish: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Compute today's calculated prayer times
        val cal = Calendar.getInstance()
        val tzOffset = (cal.timeZone.getOffset(cal.timeInMillis) / (1000.0 * 60.0 * 60.0))
        val times = SolarPrayerEngine.calculateTimes(latitude, longitude, cal, tzOffset, method, juristic)

        val prayerList = listOf(
            Triple(0, if (isEnglish) "Fajr Prayer" else "ফজর নামাজ", times.fajrStart),
            Triple(1, if (isEnglish) "Dhuhr Prayer" else "যোহর নামাজ", times.dhuhrStart),
            Triple(2, if (isEnglish) "Asr Prayer" else "আসর নামাজ", times.asrStart),
            Triple(3, if (isEnglish) "Maghrib Prayer" else "মাগরিব নামাজ", times.maghribStart),
            Triple(4, if (isEnglish) "Isha Prayer" else "এশা নামাজ", times.ishaStart)
        )

        for ((idx, name, timeStr) in prayerList) {
            val targetCal = parseTimeToTodayCalendar(timeStr)
            if (targetCal.timeInMillis <= System.currentTimeMillis()) {
                // If today's prayer already passed, schedule for tomorrow
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra("prayer_name", name)
                putExtra(
                    "message",
                    if (isEnglish) "It is time for $name. Come to prayer, come to success."
                    else "$name-এর ওয়াক্ত হয়েছে। নামাজের দিকে আসুন, কল্যাণের দিকে আসুন।"
                )
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_BASE + idx,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pendingIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("PrayerScheduler", "Error scheduling alarm for $name", e)
            }
        }
    }

    fun cancelAllPrayerAlerts(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (i in 0..4) {
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_BASE + i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    fun sendInstantTestNotification(context: Context, isEnglish: Boolean) {
        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = 9999,
            title = if (isEnglish) "🕌 NoorUp Prayer Alert" else "🕌 নূরআপ নামাজ ওয়াক্ত",
            message = if (isEnglish) "Test notification: Prayer reminder is working perfectly."
            else "টেস্ট নোটিফিকেশন: নামাজের ওয়াক্ত অ্যালার্ট সক্রিয় আছে।"
        )
    }

    fun scheduleExactTestAlarmInSeconds(context: Context, seconds: Int, isEnglish: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetMillis = System.currentTimeMillis() + (seconds * 1000L)

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("prayer_name", if (isEnglish) "🕌 Test Adhan Alert" else "🕌 টেস্ট আজান অ্যালার্ট")
            putExtra("message", if (isEnglish) "Exact alarm fired accurately after $seconds seconds." else "$seconds সেকেন্ড পর সঠিক অ্যালার্ম সম্পন্ন হয়েছে।")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            8888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("PrayerScheduler", "Error scheduling exact test alarm", e)
        }
    }

    fun triggerGardenReminderNowForTesting(context: Context, isEnglish: Boolean) {
        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = 1001,
            title = if (isEnglish) "🌱 Noor Garden Evening Reminder" else "🌱 নূর বাগান সন্ধ্যার অনুস্মারক",
            message = if (isEnglish) "Keep your garden green by recording your today's prayers and dhikr!"
            else "আজকের নামাজের ওয়াক্ত ও আমলগুলো পূরণ করে আপনার নূর বাগান সবুজ রাখুন!",
            channelId = "garden_reminders_channel"
        )
    }

    private fun parseTimeToTodayCalendar(timeStr: String): Calendar {
        val cal = Calendar.getInstance()
        try {
            val parts = timeStr.trim().split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            if (parts.size > 1 && parts[1].equals("PM", ignoreCase = true) && hour < 12) {
                hour += 12
            } else if (parts.size > 1 && parts[1].equals("AM", ignoreCase = true) && hour == 12) {
                hour = 0
            }
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } catch (e: Exception) {
            android.util.Log.e("PrayerScheduler", "Failed to parse time $timeStr", e)
        }
        return cal
    }
}
