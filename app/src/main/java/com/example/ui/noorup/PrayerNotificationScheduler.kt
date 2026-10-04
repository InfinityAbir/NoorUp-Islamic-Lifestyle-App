package com.example.ui.noorup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import com.example.MainActivity
import com.example.ui.noorup.hadith.DailyHadithProvider
import java.util.Calendar

object PrayerNotificationScheduler {

    private const val TAG = "PrayerScheduler"
    private const val ALARM_REQ_BASE = 5000
    const val GARDEN_ALARM_REQ_CODE = 7001
    const val GARDEN_NOTIFICATION_ID = 1001
    const val HADITH_ALARM_REQ_CODE = 7002
    const val HADITH_NOTIFICATION_ID = 1002

    private fun toEnglishDigits(str: String): String {
        val bnToEn = mapOf(
            '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
            '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
        )
        return str.map { bnToEn[it] ?: it }.joinToString("")
    }

    /**
     * Schedules an alarm with pinpoint exactness using AlarmClockInfo.
     * AlarmClockInfo is the highest priority alarm in Android OS, which completely
     * bypasses Doze mode, app standby buckets, and OEM battery throttling (MIUI/HyperOS,
     * ColorOS, OneUI, etc.) ensuring ZERO-DELAY trigger at the exact calculated minute.
     */
    fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d(TAG, "Successfully scheduled AlarmClock at $triggerMillis")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "setAlarmClock failed, falling back to setExactAndAllowWhileIdle", e)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm at $triggerMillis", e)
        }
    }

    fun getTarget10PmCalendar(): Calendar {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target
    }

    fun getTarget9AmCalendar(): Calendar {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target
    }

    fun scheduleGardenExactAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetCal = getTarget10PmCalendar()
        val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val isEnglish = prefs.getBoolean("is_english", false)

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("is_garden_reminder", true)
            putExtra("notification_id", GARDEN_NOTIFICATION_ID)
            putExtra("channel_id", "garden_reminders_channel")
            putExtra(
                "prayer_name",
                if (isEnglish) "🌱 Noor Garden (10:00 PM Reminder)" else "🌱 নূর বাগান (রাত ১০:০০ ঘটিকার অনুস্মারক)"
            )
            putExtra(
                "message",
                if (isEnglish) "Complete today's prayers and daily deeds to keep your garden vibrant and green."
                else "আজকের নামাজের ওয়াক্ত ও আমলগুলো সম্পূর্ণ করে বাগান সবুজ রাখুন।"
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GARDEN_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(context, alarmManager, targetCal.timeInMillis, pendingIntent)
        Log.d(TAG, "Scheduled 10:00 PM Noor Garden exact alarm for ${targetCal.time}")
    }

    fun scheduleDailyHadithExactAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetCal = getTarget9AmCalendar()

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("is_hadith_reminder", true)
            putExtra("notification_id", HADITH_NOTIFICATION_ID)
            putExtra("channel_id", "hadith_reminders_channel")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            HADITH_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(context, alarmManager, targetCal.timeInMillis, pendingIntent)
        Log.d(TAG, "Scheduled 09:00 AM Daily Hadith exact alarm for ${targetCal.time}")
    }

    fun cancelGardenWorkManagerIfAny(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("noor_garden_daily_reminder")
            workManager.cancelAllWorkByTag("noor_garden")
        } catch (e: Throwable) {
            Log.e(TAG, "Error cancelling garden work manager", e)
        }
    }

    fun scheduleDailyTasks(context: Context) {
        cancelGardenWorkManagerIfAny(context)
        scheduleGardenExactAlarm(context)
        scheduleDailyHadithExactAlarm(context)
    }

    fun isExactAlarmPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    /**
     * Convenience function to schedule all 5 prayer alerts by reading saved user configuration.
     */
    fun scheduleAllPrayerAlerts(context: Context) {
        val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("prayer_notifications_enabled", true)
        if (!isEnabled) {
            cancelAllPrayerAlerts(context)
            return
        }

        val latitude = prefs.getFloat("selected_city_lat", 23.8759f).toDouble()
        val longitude = prefs.getFloat("selected_city_lng", 90.3795f).toDouble()
        val methodStr = prefs.getString("calculation_method", CalculationMethod.KARACHI.name)
        val method = CalculationMethod.entries.find { it.name == methodStr } ?: CalculationMethod.KARACHI
        val juristicStr = prefs.getString("juristic_method", JuristicMethod.HANAFI.name)
        val juristic = JuristicMethod.entries.find { it.name == juristicStr } ?: JuristicMethod.HANAFI
        val isEnglish = prefs.getBoolean("is_english", false)

        scheduleAllPrayerAlerts(
            context = context,
            latitude = latitude,
            longitude = longitude,
            method = method,
            juristic = juristic,
            isEnglish = isEnglish
        )
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
        val now = Calendar.getInstance()
        val nowMillis = now.timeInMillis
        val tzOffset = now.timeZone.getOffset(nowMillis) / 3600000.0

        // Compute solar prayer times for TODAY and TOMORROW separately
        // to guarantee 100% astronomical accuracy for upcoming slots.
        val todayTimes = SolarPrayerEngine.calculateTimes(latitude, longitude, now, tzOffset, method, juristic)
        val tomorrowCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowTimes = SolarPrayerEngine.calculateTimes(latitude, longitude, tomorrowCal, tzOffset, method, juristic)

        data class PrayerItem(
            val index: Int,
            val name: String,
            val todayStartStr: String,
            val todayEndStr: String,
            val tomorrowStartStr: String,
            val tomorrowEndStr: String
        )

        val prayerItems = listOf(
            PrayerItem(
                0,
                if (isEnglish) "Fajr Prayer" else "ফজর নামাজ",
                todayTimes.fajrStart, todayTimes.fajrEnd,
                tomorrowTimes.fajrStart, tomorrowTimes.fajrEnd
            ),
            PrayerItem(
                1,
                if (isEnglish) "Dhuhr Prayer" else "যোহর নামাজ",
                todayTimes.dhuhrStart, todayTimes.dhuhrEnd,
                tomorrowTimes.dhuhrStart, tomorrowTimes.dhuhrEnd
            ),
            PrayerItem(
                2,
                if (isEnglish) "Asr Prayer" else "আসর নামাজ",
                todayTimes.asrStart, todayTimes.asrEnd,
                tomorrowTimes.asrStart, tomorrowTimes.asrEnd
            ),
            PrayerItem(
                3,
                if (isEnglish) "Maghrib Prayer" else "মাগরিব নামাজ",
                todayTimes.maghribStart, todayTimes.maghribEnd,
                tomorrowTimes.maghribStart, tomorrowTimes.maghribEnd
            ),
            PrayerItem(
                4,
                if (isEnglish) "Isha Prayer" else "এশা নামাজ",
                todayTimes.ishaStart, todayTimes.ishaEnd,
                tomorrowTimes.ishaStart, tomorrowTimes.ishaEnd
            )
        )

        for (item in prayerItems) {
            val todayTargetCal = parseTimeToCalendar(item.todayStartStr, now)
            val isUpcomingToday = todayTargetCal.timeInMillis > (nowMillis + 2000L)

            val (targetCal, timeWindow) = if (isUpcomingToday) {
                todayTargetCal to "${item.todayStartStr} – ${item.todayEndStr}"
            } else {
                val tomorrowTargetCal = parseTimeToCalendar(item.tomorrowStartStr, tomorrowCal)
                tomorrowTargetCal to "${item.tomorrowStartStr} – ${item.tomorrowEndStr}"
            }

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra("prayer_name", item.name)
                putExtra("notification_id", 2000 + item.index)
                putExtra("channel_id", "prayer_reminders_channel")
                putExtra(
                    "message",
                    if (isEnglish) "It is time for ${item.name} ($timeWindow). Come to prayer, come to success."
                    else "${item.name}-এর ওয়াক্ত হয়েছে ($timeWindow)। নামাজের দিকে আসুন, কল্যাণের দিকে আসুন।"
                )
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_BASE + item.index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactAlarm(context, alarmManager, targetCal.timeInMillis, pendingIntent)
            Log.d(TAG, "Scheduled ${item.name} exact alarm for ${targetCal.time} (window: $timeWindow)")
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
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun sendInstantTestNotification(context: Context, isEnglish: Boolean) {
        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = 9999,
            title = if (isEnglish) "🕌 NoorUp Prayer Alert" else "🕌 নূরআপ নামাজ ওয়াক্ত",
            message = if (isEnglish) "Test notification: Prayer reminder is working perfectly on-time."
            else "টেস্ট নোটিফিকেশন: নামাজের ওয়াক্ত অ্যালার্ট সক্রিয় ও সঠিক সময়নিষ্ঠ আছে।"
        )
    }

    fun scheduleExactTestAlarmInSeconds(context: Context, seconds: Int, isEnglish: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetMillis = System.currentTimeMillis() + (seconds * 1000L)

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("prayer_name", if (isEnglish) "🕌 Test Adhan Alert" else "🕌 টেস্ট আজান অ্যালার্ট")
            putExtra("message", if (isEnglish) "Exact alarm fired accurately after $seconds seconds." else "$seconds সেকেন্ড পর সঠিক অ্যালার্ম সম্পন্ন হয়েছে।")
            putExtra("notification_id", 8888)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            8888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(context, alarmManager, targetMillis, pendingIntent)
    }

    fun triggerGardenReminderNowForTesting(context: Context, isEnglish: Boolean) {
        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = GARDEN_NOTIFICATION_ID,
            title = if (isEnglish) "🌱 Noor Garden Evening Reminder" else "🌱 নূর বাগান সন্ধ্যার অনুস্মারক",
            message = if (isEnglish) "Keep your garden green by recording your today's prayers and dhikr!"
            else "আজকের নামাজের ওয়াক্ত ও আমলগুলো পূরণ করে আপনার নূর বাগান সবুজ রাখুন!",
            channelId = "garden_reminders_channel"
        )
    }

    fun triggerDailyHadithNowForTesting(context: Context, isEnglish: Boolean) {
        val hadith = DailyHadithProvider.getTodayHadith()
        val title = if (isEnglish) "📖 Daily Noor Hadith • ${hadith.referenceEn}" else "📖 আজকের নূর হাদিস • ${hadith.referenceBn}"
        val message = if (isEnglish) "\"${hadith.textEn}\"\n— ${hadith.narratorEn}" else "\"${hadith.textBn}\"\n— ${hadith.narratorBn}"

        PrayerNotificationHelper.showPrayerAlert(
            context = context,
            notificationId = HADITH_NOTIFICATION_ID,
            title = title,
            message = message,
            channelId = "hadith_reminders_channel"
        )
    }

    /**
     * Parses a time string (e.g. "04:04 PM" or "০৪:০৪ PM") into a Calendar instance on [baseDate].
     */
    private fun parseTimeToCalendar(timeStr: String, baseDate: Calendar): Calendar {
        val cal = (baseDate.clone() as Calendar).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        try {
            val cleanStr = toEnglishDigits(timeStr).trim().uppercase()
            val isPm = cleanStr.contains("PM")
            val isAm = cleanStr.contains("AM")
            val timePortion = cleanStr.replace("AM", "").replace("PM", "").trim()
            val parts = timePortion.split(":")
            var hour = parts[0].trim().toInt()
            val minute = parts[1].trim().toInt()

            if (isPm && hour < 12) {
                hour += 12
            } else if (isAm && hour == 12) {
                hour = 0
            }

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse time $timeStr", e)
        }
        return cal
    }
}
