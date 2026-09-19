package com.example.ui.noorup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MainActivity
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object PrayerNotificationScheduler {

    private const val TAG = "PrayerNotificationScheduler"

    // AlarmManager Request Codes
    const val REQUEST_CODE_FAJR = 2001
    const val REQUEST_CODE_SEHRI = 2002
    const val REQUEST_CODE_DHUHR = 2003
    const val REQUEST_CODE_ASR = 2004
    const val REQUEST_CODE_MAGHRIB = 2005
    const val REQUEST_CODE_ISHA = 2006
    const val REQUEST_CODE_DAILY_HADITH = 2007
    const val REQUEST_CODE_MIDNIGHT = 2008
    const val REQUEST_CODE_TEST_EXACT = 2009
    const val REQUEST_CODE_GARDEN_REMINDER = 2010

    // Legacy WorkManager work names to cancel so old delayed tasks never fire
    const val WORK_FAJR = "noorup_work_fajr"
    const val WORK_SEHRI = "noorup_work_sehri"
    const val WORK_DHUHR = "noorup_work_dhuhr"
    const val WORK_ASR = "noorup_work_asr"
    const val WORK_MAGHRIB = "noorup_work_maghrib"
    const val WORK_ISHA = "noorup_work_isha"
    const val WORK_DAILY_HADITH = "noorup_work_daily_hadith"
    const val WORK_MIDNIGHT_RESCHEDULE = "noorup_work_midnight_reschedule"
    const val WORK_GARDEN_REMINDER = "noorup_work_garden_reminder_10pm"

    fun isExactAlarmPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return alarmManager?.canScheduleExactAlarms() ?: true
        }
        return true
    }

    private fun parsePrayerTimeToCalendar(timeStr: String, baseDate: Calendar): Calendar {
        val cal = baseDate.clone() as Calendar
        try {
            val parts = timeStr.trim().split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val min = timeParts[1].toInt()
            val amPm = parts.getOrNull(1)?.uppercase() ?: "AM"
            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeStr", e)
        }
        return cal
    }

    private fun scheduleExactAlarm(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        intent: Intent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }

                if (canExact) {
                    // Highest priority exact wakeup, immune to Doze mode
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to setAlarmClock which is exempt from exact alarm permission and Doze mode
                    scheduleAlarmClockFallback(context, alarmManager, requestCode, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully armed exact alarm $requestCode for $triggerAtMillis")
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException on setExactAndAllowWhileIdle, falling back to setAlarmClock", se)
            scheduleAlarmClockFallback(context, alarmManager, requestCode, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm $requestCode", e)
        }
    }

    private fun scheduleAlarmClockFallback(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            val showIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                requestCode + 10000,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Armed alarm via setAlarmClock for $requestCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed setAlarmClock fallback for $requestCode", e)
        }
    }

    private fun cancelExactAlarm(context: Context, requestCode: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alarm $requestCode", e)
        }
    }

    fun scheduleAllPrayerAlerts(
        context: Context,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod = CalculationMethod.KARACHI,
        juristic: JuristicMethod = JuristicMethod.HANAFI,
        isEnglish: Boolean = false
    ) {
        try {
            val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("last_lat", latitude.toFloat())
                .putFloat("last_lon", longitude.toFloat())
                .putBoolean("is_english", isEnglish)
                .putBoolean("prayer_notifications_enabled", true)
                .apply()

            PrayerNotificationHelper.createNotificationChannels(context)

            // CRITICAL: Cancel legacy WorkManager prayer tasks so they NEVER deliver late notifications!
            cancelLegacyWorkManagerAlerts(context)

            val now = Calendar.getInstance()
            val tzOffsetHours = (TimeZone.getDefault().getOffset(now.timeInMillis) / 3600000.0)

            // 1. Calculate today's times
            val todayCal = now.clone() as Calendar
            val todayTimes = SolarPrayerEngine.calculateTimes(
                latitude = latitude,
                longitude = longitude,
                calendar = todayCal,
                timezoneOffset = tzOffsetHours,
                method = method,
                juristic = juristic
            )

            // 2. Calculate tomorrow's times for prayers that have already passed today
            val tomorrowCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowTimes = SolarPrayerEngine.calculateTimes(
                latitude = latitude,
                longitude = longitude,
                calendar = tomorrowCal,
                timezoneOffset = tzOffsetHours,
                method = method,
                juristic = juristic
            )

            data class PrayerAlarmTarget(
                val requestCode: Int,
                val notifId: Int,
                val timeStr: String,
                val targetCal: Calendar,
                val title: String,
                val message: String,
                val subText: String,
                val prayerType: String
            )

            fun resolveAlarmTarget(
                requestCode: Int,
                notifId: Int,
                todayTimeStr: String,
                tomorrowTimeStr: String,
                titleBn: String,
                titleEn: String,
                msgBnGenerator: (String) -> String,
                msgEnGenerator: (String) -> String,
                subTextBn: String,
                subTextEn: String,
                prayerType: String,
                minuteOffset: Int = 0
            ): PrayerAlarmTarget {
                var targetCal = parsePrayerTimeToCalendar(todayTimeStr, todayCal)
                if (minuteOffset != 0) {
                    targetCal.add(Calendar.MINUTE, minuteOffset)
                }
                val timeToUse: String

                if (targetCal.timeInMillis <= now.timeInMillis) {
                    // Today's occurrence has already passed, schedule for tomorrow
                    targetCal = parsePrayerTimeToCalendar(tomorrowTimeStr, tomorrowCal)
                    if (minuteOffset != 0) {
                        targetCal.add(Calendar.MINUTE, minuteOffset)
                    }
                    timeToUse = tomorrowTimeStr
                } else {
                    timeToUse = todayTimeStr
                }

                return PrayerAlarmTarget(
                    requestCode = requestCode,
                    notifId = notifId,
                    timeStr = timeToUse,
                    targetCal = targetCal,
                    title = if (isEnglish) titleEn else titleBn,
                    message = if (isEnglish) msgEnGenerator(timeToUse) else msgBnGenerator(timeToUse),
                    subText = if (isEnglish) subTextEn else subTextBn,
                    prayerType = prayerType
                )
            }

            val targets = listOf(
                // Fajr Prayer
                resolveAlarmTarget(
                    requestCode = REQUEST_CODE_FAJR,
                    notifId = PrayerNotificationHelper.NOTIFICATION_ID_FAJR,
                    todayTimeStr = todayTimes.fajrStart,
                    tomorrowTimeStr = tomorrowTimes.fajrStart,
                    titleBn = "ফজরের নামাজের ওয়াক্ত",
                    titleEn = "Fajr Prayer Time",
                    msgBnGenerator = { "আস-সালাতু খাইরুম মিনান নাওম (ঘুম হতে নামাজ উত্তম)। ওয়াক্ত শুরু: $it" },
                    msgEnGenerator = { "Prayer is better than sleep. Fajr begins at $it" },
                    subTextBn = "নূরআপ • ফজর",
                    subTextEn = "NoorUp • Fajr",
                    prayerType = "fajr"
                ),
                // Sehri End Alert (20 mins before Fajr)
                resolveAlarmTarget(
                    requestCode = REQUEST_CODE_SEHRI,
                    notifId = PrayerNotificationHelper.NOTIFICATION_ID_SEHRI,
                    todayTimeStr = todayTimes.sehriEnd,
                    tomorrowTimeStr = tomorrowTimes.sehriEnd,
                    titleBn = "সেহরি ও তাহাজ্জুদ সতর্কবার্তা",
                    titleEn = "Sehri Ending Alert",
                    msgBnGenerator = { "সেহরির শেষ সময় আসন্ন ($it)। বরকতময় সেহরি সম্পন্ন করে রোযার নিয়ত করুন।" },
                    msgEnGenerator = { "Sehri ending soon at $it. Complete your pre-dawn meal." },
                    subTextBn = "নূরআপ • সেহরি ও রোজা",
                    subTextEn = "NoorUp • Sehri Alert",
                    prayerType = "sehri",
                    minuteOffset = -20
                ),
                // Dhuhr Prayer
                resolveAlarmTarget(
                    requestCode = REQUEST_CODE_DHUHR,
                    notifId = PrayerNotificationHelper.NOTIFICATION_ID_DHUHR,
                    todayTimeStr = todayTimes.dhuhrStart,
                    tomorrowTimeStr = tomorrowTimes.dhuhrStart,
                    titleBn = "যোহরের নামাজের ওয়াক্ত",
                    titleEn = "Dhuhr Prayer Time",
                    msgBnGenerator = { "আল্লাহর স্মরণে নামাজ কায়েম করুন। যোহরের ওয়াক্ত শুরু: $it" },
                    msgEnGenerator = { "Establish prayer to remember Allah. Dhuhr begins at $it" },
                    subTextBn = "নূরআপ • যোহর",
                    subTextEn = "NoorUp • Dhuhr",
                    prayerType = "dhuhr"
                ),
                // Asr Prayer
                resolveAlarmTarget(
                    requestCode = REQUEST_CODE_ASR,
                    notifId = PrayerNotificationHelper.NOTIFICATION_ID_ASR,
                    todayTimeStr = todayTimes.asrStart,
                    tomorrowTimeStr = tomorrowTimes.asrStart,
                    titleBn = "আসরের নামাজের ওয়াক্ত",
                    titleEn = "Asr Prayer Time",
                    msgBnGenerator = { "মধ্যবর্তী নামাজের (আসর) বিশেষ যত্ন নিন। আসরের ওয়াক্ত শুরু: $it" },
                    msgEnGenerator = { "Guard strictly the middle prayer. Asr begins at $it" },
                    subTextBn = "নূরআপ • আসর",
                    subTextEn = "NoorUp • Asr",
                    prayerType = "asr"
                ),
                // Maghrib & Iftar
                resolveAlarmTarget(
                    requestCode = REQUEST_CODE_MAGHRIB,
                    notifId = PrayerNotificationHelper.NOTIFICATION_ID_MAGHRIB,
                    todayTimeStr = todayTimes.maghribStart,
                    tomorrowTimeStr = tomorrowTimes.maghribStart,
                    titleBn = "মাগরিবের নামাজের ওয়াক্ত ও ইফতার",
                    titleEn = "Maghrib Prayer & Iftar Time",
                    msgBnGenerator = { "মাগরিবের ওয়াক্ত ও ইফতারের সময় হয়েছে ($it)। দুআ: যাহাবাজ জামাউ ওয়াবতাল্লাতিল উরূকু..." },
                    msgEnGenerator = { "Maghrib and Iftar time has arrived at $it." },
                    subTextBn = "নূরআপ • মাগরিব ও ইফতার",
                    subTextEn = "NoorUp • Maghrib & Iftar",
                    prayerType = "maghrib"
                ),
                // Isha Prayer
                resolveAlarmTarget(
                    requestCode = REQUEST_CODE_ISHA,
                    notifId = PrayerNotificationHelper.NOTIFICATION_ID_ISHA,
                    todayTimeStr = todayTimes.ishaStart,
                    tomorrowTimeStr = tomorrowTimes.ishaStart,
                    titleBn = "এশার নামাজের ওয়াক্ত",
                    titleEn = "Isha Prayer Time",
                    msgBnGenerator = { "শান্তিময় রাত্রির পূর্বে এশা ও বিতর নামাজ আদায় করে নিন। ওয়াক্ত শুরু: $it" },
                    msgEnGenerator = { "Perform Isha and Witr before resting. Isha begins at $it" },
                    subTextBn = "নূরআপ • এশা",
                    subTextEn = "NoorUp • Isha",
                    prayerType = "isha"
                )
            )

            // Arm each prayer alert using AlarmManager RTC_WAKEUP for exact, on-time delivery
            for (target in targets) {
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                    putExtra(PrayerAlarmReceiver.EXTRA_TITLE, target.title)
                    putExtra(PrayerAlarmReceiver.EXTRA_MESSAGE, target.message)
                    putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, target.notifId)
                    putExtra(PrayerAlarmReceiver.EXTRA_CHANNEL_ID, PrayerNotificationHelper.CHANNEL_ID_PRAYER)
                    putExtra(PrayerAlarmReceiver.EXTRA_SUBTEXT, target.subText)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, target.prayerType)
                }

                scheduleExactAlarm(
                    context = context,
                    requestCode = target.requestCode,
                    triggerAtMillis = target.targetCal.timeInMillis,
                    intent = intent
                )
            }

            // 3. Schedule Daily 09:00 AM Islamic Hadith & Wisdom Reminder
            val hadithCal = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (hadithCal.timeInMillis <= now.timeInMillis) {
                hadithCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val hadithIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(
                    PrayerAlarmReceiver.EXTRA_TITLE,
                    if (isEnglish) "Daily Hadith & Wisdom" else "আজকের নূর হাদিস ও নসিহত"
                )
                putExtra(
                    PrayerAlarmReceiver.EXTRA_MESSAGE,
                    if (isEnglish) {
                        "The Prophet (ﷺ) said: 'The best of people are those that bring the most benefit to the rest of mankind.' (Tabarani)"
                    } else {
                        "রাসূলুল্লাহ (সা.) বলেছেন: 'তোমাদের মধ্যে সর্বোত্তম ব্যক্তি সে, যার দ্বারা মানুষের সবচেয়ে বেশি কল্যাণ সাধিত হয়।' (সহীহ আত-তারগীব)"
                    }
                )
                putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, PrayerNotificationHelper.NOTIFICATION_ID_DAILY_HADITH)
                putExtra(PrayerAlarmReceiver.EXTRA_CHANNEL_ID, PrayerNotificationHelper.CHANNEL_ID_DAILY)
                putExtra(PrayerAlarmReceiver.EXTRA_SUBTEXT, if (isEnglish) "Daily Spiritual Boost" else "দৈনিক আত্মশুদ্ধি")
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, "daily_hadith")
            }

            scheduleExactAlarm(
                context = context,
                requestCode = REQUEST_CODE_DAILY_HADITH,
                triggerAtMillis = hadithCal.timeInMillis,
                intent = hadithIntent
            )

            // 4. Midnight auto-recalibration alarm (at 00:05 AM) to recalculate the upcoming day
            val midnightCal = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val midnightIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, "midnight_reschedule")
            }

            scheduleExactAlarm(
                context = context,
                requestCode = REQUEST_CODE_MIDNIGHT,
                triggerAtMillis = midnightCal.timeInMillis,
                intent = midnightIntent
            )

            // Secondary WorkManager fallback for midnight reschedule
            scheduleMidnightWorkManagerFallback(context, midnightCal.timeInMillis - now.timeInMillis)

            // 5. Schedule Daily 10:00 PM Noor Garden Check via WorkManager
            scheduleDailyGardenReminder(context)

        } catch (e: Throwable) {
            Log.e(TAG, "Error scheduling prayer alerts", e)
        }
    }

    private fun scheduleMidnightWorkManagerFallback(context: Context, delayMs: Long) {
        try {
            if (delayMs <= 0) return
            val workManager = WorkManager.getInstance(context)
            val midnightWork = OneTimeWorkRequestBuilder<PrayerRescheduleWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniqueWork(
                WORK_MIDNIGHT_RESCHEDULE,
                ExistingWorkPolicy.REPLACE,
                midnightWork
            )
        } catch (e: Exception) {
            // Non-fatal
        }
    }

    private fun cancelLegacyWorkManagerAlerts(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_FAJR)
            workManager.cancelUniqueWork(WORK_SEHRI)
            workManager.cancelUniqueWork(WORK_DHUHR)
            workManager.cancelUniqueWork(WORK_ASR)
            workManager.cancelUniqueWork(WORK_MAGHRIB)
            workManager.cancelUniqueWork(WORK_ISHA)
            workManager.cancelUniqueWork(WORK_DAILY_HADITH)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun cancelAllPrayerAlerts(context: Context) {
        try {
            val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("prayer_notifications_enabled", false).apply()

            val requestCodes = listOf(
                REQUEST_CODE_FAJR,
                REQUEST_CODE_SEHRI,
                REQUEST_CODE_DHUHR,
                REQUEST_CODE_ASR,
                REQUEST_CODE_MAGHRIB,
                REQUEST_CODE_ISHA,
                REQUEST_CODE_DAILY_HADITH,
                REQUEST_CODE_MIDNIGHT,
                REQUEST_CODE_TEST_EXACT,
                REQUEST_CODE_GARDEN_REMINDER
            )
            for (rc in requestCodes) {
                cancelExactAlarm(context, rc)
            }

            cancelLegacyWorkManagerAlerts(context)
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_MIDNIGHT_RESCHEDULE)
                WorkManager.getInstance(context).cancelUniqueWork(WORK_GARDEN_REMINDER)
            } catch (e: Exception) {
                // Ignore
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error cancelling prayer alerts", e)
        }
    }

    /**
     * Schedules a daily 10:00 PM check via exact AlarmManager.
     * Evaluates daily prayer progress and prompts the user with an encouraging notification.
     */
    fun scheduleDailyGardenReminder(context: Context) {
        try {
            val now = Calendar.getInstance()
            val targetCal = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 22) // 10:00 PM
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If 10:00 PM has already passed today, target tomorrow 10:00 PM
            if (targetCal.timeInMillis <= now.timeInMillis) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            val isEnglish = prefs.getBoolean("is_english", false)

            val gardenIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, "garden_reminder")
                putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, PrayerNotificationHelper.NOTIFICATION_ID_GARDEN_REMINDER)
                putExtra(PrayerAlarmReceiver.EXTRA_CHANNEL_ID, PrayerNotificationHelper.CHANNEL_ID_DAILY)
                putExtra(
                    PrayerAlarmReceiver.EXTRA_TITLE,
                    if (isEnglish) "Noor Garden & Prayer Check" else "নূর বাগান ও নামাজের হিসাব"
                )
                putExtra(
                    PrayerAlarmReceiver.EXTRA_MESSAGE,
                    if (isEnglish) "Review your daily prayers in Noor Garden. Log your completed prayers before sleep!"
                    else "আজকের সকল ওয়াক্তের নামাজ কি আদায় করা হয়েছে? নূর বাগানে আপনার নামাজের হিসাব হালনাগাদ করুন।"
                )
                putExtra(PrayerAlarmReceiver.EXTRA_SUBTEXT, if (isEnglish) "Evening Check" else "রাতের পর্যালোচনা")
            }

            scheduleExactAlarm(
                context = context,
                requestCode = REQUEST_CODE_GARDEN_REMINDER,
                triggerAtMillis = targetCal.timeInMillis,
                intent = gardenIntent
            )

            // Cancel any old legacy WorkManager tasks for garden reminder
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_GARDEN_REMINDER)
            } catch (e: Exception) {
                // Ignore
            }

            Log.i(TAG, "Scheduled 10:00 PM exact Alarm for Noor Garden at: ${targetCal.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling 10:00 PM exact garden reminder", e)
        }
    }

    /**
     * Testing utility: Immediately triggers the 10:00 PM Noor Garden reminder notification
     * with the real user progress check so the user can verify the notification appearance and text.
     */
    fun triggerGardenReminderNowForTesting(context: Context, isEnglish: Boolean = false) {
        val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val todayDateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val mandatoryPrayers = listOf("ফজর", "যোহর", "আসর", "মাগরিব", "এশা")
        val completedCount = mandatoryPrayers.count { prayer ->
            prefs.getBoolean("prayer_habit_${todayDateKey}_$prayer", false)
        }

        val isAllCompleted = completedCount >= 5
        val title = if (isEnglish) {
            if (isAllCompleted) "Noor Garden Complete! 🌱" else "Update Your Noor Garden"
        } else {
            if (isAllCompleted) "মাশাআল্লাহ! নূর বাগান পূর্ণাঙ্গ 🌱" else "নূর বাগান আপডেট করুন"
        }
        val message = if (isEnglish) {
            if (isAllCompleted) "Alhamdulillah! All 5 mandatory prayers completed today (5/5). Your garden is flourishing with divine blessings!"
            else "Update your Noor Garden: Have today's prayers been completed? Log quickly to keep your garden growing ($completedCount/5 completed)."
        } else {
            if (isAllCompleted) "আলহামদুলিল্লাহ! আজকের সকল ৫ ওয়াক্ত নামাজ সম্পন্ন হয়েছে (৫/৫)। আপনার নূর বাগান সবুজে প্রস্ফুটিত!"
            else "নূর বাগান আপডেট করুন: আজকের ওয়াক্তগুলো কি আদায় করা হয়েছে? আপনার বাগান টিকিয়ে রাখতে দ্রুত লগ করুন ($completedCount/৫ সম্পন্ন)।"
        }
        val subText = if (isEnglish) "Noor Garden • Daily Tracker" else "নূর বাগান • দৈনিক ট্র্যাকার"

        PrayerNotificationHelper.showNotification(
            context = context,
            title = title,
            message = message,
            notificationId = PrayerNotificationHelper.NOTIFICATION_ID_GARDEN_REMINDER,
            channelId = PrayerNotificationHelper.CHANNEL_ID_DAILY,
            subText = subText
        )
    }

    fun sendInstantTestNotification(context: Context, isEnglish: Boolean = false) {
        PrayerNotificationHelper.showNotification(
            context = context,
            title = if (isEnglish) "NoorUp Prayer Alert Test" else "নূরআপ নামাজের ওয়াক্ত রিমাইন্ডার টেস্ট",
            message = if (isEnglish) {
                "Exact timing active! Real-time alerts for Fajr, Dhuhr, Asr, Maghrib, and Isha are armed with zero delay."
            } else {
                "সঠিক সময়ে নোটিফিকেশন ইঞ্জিন সক্রিয়! ফজর, যোহর, আসর, মাগরিব, এশা এবং সেহরি-ইফতারের ওয়াক্তে নির্ধারিত সময়ে কোনো দেরি ছাড়াই অ্যালার্ট আসবে ইনশাআল্লাহ।"
            },
            notificationId = PrayerNotificationHelper.NOTIFICATION_ID_TEST,
            channelId = PrayerNotificationHelper.CHANNEL_ID_PRAYER,
            subText = if (isEnglish) "NoorUp • Exact Timing" else "নূরআপ • সঠিক সময়ে অ্যালার্ট"
        )
    }

    fun scheduleExactTestAlarmInSeconds(context: Context, seconds: Int = 5, isEnglish: Boolean = false) {
        val triggerTime = System.currentTimeMillis() + (seconds * 1000L)
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(
                PrayerAlarmReceiver.EXTRA_TITLE,
                if (isEnglish) "Exact Alarm Test ($seconds s)" else "সঠিক সময়ের টেস্ট অ্যালার্ম ($seconds সেকেন্ড)"
            )
            putExtra(
                PrayerAlarmReceiver.EXTRA_MESSAGE,
                if (isEnglish) {
                    "Exact AlarmManager RTC Wakeup verified! Your prayer notifications will trigger exactly on time."
                } else {
                    "অ্যালার্ম ম্যানেজার আরটিসি ওয়েকআপ সফলভাবে যাচাইকৃত! আপনার নামাজের ওয়াক্তের নোটিফিকেশন নির্ধারিত সময়ে একদম অন-টাইম পৌঁছাবে।"
                }
            )
            putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, PrayerNotificationHelper.NOTIFICATION_ID_TEST)
            putExtra(PrayerAlarmReceiver.EXTRA_CHANNEL_ID, PrayerNotificationHelper.CHANNEL_ID_PRAYER)
            putExtra(PrayerAlarmReceiver.EXTRA_SUBTEXT, if (isEnglish) "Exact Test • On Time" else "টেস্ট • সঠিক সময়ে")
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, "test_exact")
        }

        scheduleExactAlarm(
            context = context,
            requestCode = REQUEST_CODE_TEST_EXACT,
            triggerAtMillis = triggerTime,
            intent = intent
        )
    }
}
