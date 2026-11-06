package com.example.ui.noorup.journey

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.ui.noorup.PrayerAlarmReceiver
import com.example.ui.noorup.PrayerNotificationHelper
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Friday Reflection and weekly-intention reminders.
 *
 * - Plain exact-while-idle alarms (no alarm-clock status-bar icon); falls back to an inexact
 *   alarm when exact-alarm access is off, so it can only be late, never crash or vanish.
 * - Re-armed from the existing boot / package-replaced / timezone / periodic paths through
 *   `PrayerNotificationScheduler.scheduleDailyTasks`, so reboots and timezone changes are covered.
 * - Duplicate-proof: one post per Friday, and none when the reflection is already saved.
 */
object FridayReminderScheduler {

    private const val TAG = "FridayReminder"
    const val REQ_FRIDAY = 7003
    const val REQ_INTENTION = 7004
    const val NOTIFICATION_FRIDAY = 1003
    const val NOTIFICATION_INTENTION = 1004
    const val CHANNEL_ID = "journey_reminders_channel"
    const val EXTRA_FRIDAY = "is_friday_reflection"
    const val EXTRA_INTENTION = "is_intention_reminder"
    const val ROUTE_FRIDAY = "friday"

    private const val PREFS = "noorup_prefs"
    private const val LAST_FRIDAY_POSTED = "last_friday_reflection_posted"
    private const val LAST_INTENTION_POSTED = "last_intention_reminder_posted"

    /** Next [day] at [minutes] after midnight local time that is strictly after [nowMillis]. */
    fun nextTrigger(nowMillis: Long, zone: ZoneId, day: DayOfWeek, minutes: Int): Long {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        var target = now.with(TemporalAdjusters.nextOrSame(day))
            .withHour(minutes / 60).withMinute(minutes % 60).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return target.toInstant().toEpochMilli()
    }

    private fun pending(context: Context, request: Int, extra: String, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            request,
            Intent(context, PrayerAlarmReceiver::class.java).putExtra(extra, true),
            flags or PendingIntent.FLAG_IMMUTABLE
        )

    private fun arm(context: Context, request: Int, extra: String, triggerMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pending(context, request, extra, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            runCatching { am.canScheduleExactAlarms() }.getOrDefault(false)
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied, using inexact", e)
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi) }
        }
    }

    private fun disarm(context: Context, request: Int, extra: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pending(context, request, extra, PendingIntent.FLAG_NO_CREATE) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    fun cancelAll(context: Context) {
        disarm(context, REQ_FRIDAY, EXTRA_FRIDAY)
        disarm(context, REQ_INTENTION, EXTRA_INTENTION)
    }

    /** Idempotent: safe to call from boot, timezone change, settings changes and the periodic worker. */
    fun scheduleAll(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = JourneyPrefs(prefs)
        val master = prefs.getBoolean("prayer_notifications_enabled", true)
        val zone = ZoneId.systemDefault()
        if (master && settings.fridayReminderEnabled) {
            arm(context, REQ_FRIDAY, EXTRA_FRIDAY, nextTrigger(nowMillis, zone, DayOfWeek.FRIDAY, settings.fridayMinutes))
        } else {
            disarm(context, REQ_FRIDAY, EXTRA_FRIDAY)
        }
        if (master && settings.intentionReminderEnabled) {
            arm(context, REQ_INTENTION, EXTRA_INTENTION, nextTrigger(nowMillis, zone, DayOfWeek.MONDAY, settings.fridayMinutes))
        } else {
            disarm(context, REQ_INTENTION, EXTRA_INTENTION)
        }
    }

    /** Decision only (no Android calls beyond prefs), so it is unit-testable. */
    fun shouldPostFriday(prefs: android.content.SharedPreferences, today: LocalDate): Boolean {
        val settings = JourneyPrefs(prefs)
        if (!prefs.getBoolean("prayer_notifications_enabled", true)) return false
        if (!settings.fridayReminderEnabled) return false
        val friday = JourneyEngine.latestFriday(today)
        if (prefs.getString(LAST_FRIDAY_POSTED, null) == friday.toString()) return false
        if (settings.skipIfReflectionDone && ReflectionStore(prefs).hasEntry(friday)) return false
        return true
    }

    fun onFridayFire(context: Context, today: LocalDate = LocalDate.now()) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (shouldPostFriday(prefs, today)) {
            val en = prefs.getBoolean("is_english", false)
            val friday = JourneyEngine.latestFriday(today)
            val posted = PrayerNotificationHelper.showPrayerAlert(
                context = context,
                notificationId = NOTIFICATION_FRIDAY,
                title = if (en) "Your Weekly Journey" else "আপনার সাপ্তাহিক যাত্রা",
                message = if (en) "Jumu'ah mubarak. Take a quiet moment to look back on your week. Everything is optional."
                else "জুমু'আ মুবারক। একটু থেমে গত সপ্তাহটি ফিরে দেখুন। সবকিছুই ঐচ্ছিক।",
                channelId = CHANNEL_ID,
                deepLinkJourneyRoute = ROUTE_FRIDAY
            )
            if (posted) prefs.edit().putString(LAST_FRIDAY_POSTED, friday.toString()).apply()
        }
        scheduleAll(context)
    }

    fun shouldPostIntention(prefs: android.content.SharedPreferences, today: LocalDate): ReflectionEntry? {
        if (!prefs.getBoolean("prayer_notifications_enabled", true)) return null
        if (!JourneyPrefs(prefs).intentionReminderEnabled) return null
        if (prefs.getString(LAST_INTENTION_POSTED, null) == today.toString()) return null
        return ReflectionStore(prefs).activeIntention(today)
    }

    fun onIntentionFire(context: Context, today: LocalDate = LocalDate.now()) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val entry = shouldPostIntention(prefs, today)
        if (entry != null) {
            val en = prefs.getBoolean("is_english", false)
            val posted = PrayerNotificationHelper.showPrayerAlert(
                context = context,
                notificationId = NOTIFICATION_INTENTION,
                title = if (en) "Your intention this week" else "এই সপ্তাহের আপনার নিয়ত",
                message = entry.intention,
                channelId = CHANNEL_ID,
                deepLinkJourneyRoute = ROUTE_FRIDAY
            )
            if (posted) prefs.edit().putString(LAST_INTENTION_POSTED, today.toString()).apply()
        }
        scheduleAll(context)
    }
}
