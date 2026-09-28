package com.example.ui.noorup

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.example.NoorUpWidgetProvider
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WidgetUpdateScheduler {

    const val ACTION_AUTO_UPDATE_WIDGET = "com.example.action.AUTO_UPDATE_WIDGET"
    private const val WIDGET_ALARM_REQ_CODE = 9001
    private const val WORK_WIDGET_FALLBACK = "noorup_widget_fallback_periodic"

    /**
     * Checks if at least one NoorUp widget is active on the home screen.
     */
    fun hasActiveWidgets(context: Context): Boolean {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NoorUpWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds != null && appWidgetIds.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Finds the next exact prayer or makruh transition timestamp strictly after [nowMillis].
     */
    fun getNextTransitionMillis(context: Context, nowMillis: Long = System.currentTimeMillis()): Long {
        val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val latitude = prefs.getFloat("selected_city_lat", 23.8759f).toDouble()
        val longitude = prefs.getFloat("selected_city_lng", 90.3795f).toDouble()

        val methodStr = prefs.getString("calculation_method", CalculationMethod.KARACHI.name)
        val method = CalculationMethod.entries.find { it.name == methodStr } ?: CalculationMethod.KARACHI
        val juristicStr = prefs.getString("juristic_method", JuristicMethod.HANAFI.name)
        val juristic = JuristicMethod.entries.find { it.name == juristicStr } ?: JuristicMethod.HANAFI

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val tzOffset = cal.timeZone.getOffset(cal.timeInMillis) / 3600000.0

        val transitionTimestamps = mutableListOf<Long>()

        // Collect transitions for today and tomorrow
        for (dayOffset in 0..1) {
            val targetCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            val times = SolarPrayerEngine.calculateTimes(
                latitude = latitude,
                longitude = longitude,
                calendar = targetCal,
                timezoneOffset = tzOffset,
                method = method,
                juristic = juristic
            )

            val fajrMins = NoorUpWidgetProvider.parseTimeToMins(times.fajrStart)
            val sunriseMins = NoorUpWidgetProvider.parseTimeToMins(times.sunrise)
            val sunriseMakruhEndMins = sunriseMins + 15
            val zawalStartMins = NoorUpWidgetProvider.parseTimeToMins(times.makruhZawalStart)
            val dhuhrMins = NoorUpWidgetProvider.parseTimeToMins(times.dhuhrStart)
            val asrMins = NoorUpWidgetProvider.parseTimeToMins(times.asrStart)
            val sunsetMakruhStartMins = NoorUpWidgetProvider.parseTimeToMins(times.makruhSunsetStart)
            val maghribMins = NoorUpWidgetProvider.parseTimeToMins(times.maghribStart)
            val ishaMins = NoorUpWidgetProvider.parseTimeToMins(times.ishaStart)

            val minuteMilestones = listOf(
                fajrMins,
                sunriseMins,
                sunriseMakruhEndMins,
                zawalStartMins,
                dhuhrMins,
                asrMins,
                sunsetMakruhStartMins,
                maghribMins,
                ishaMins,
                0 // Midnight boundary
            )

            for (minOfDay in minuteMilestones) {
                val epochCal = (targetCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, minOfDay / 60)
                    set(Calendar.MINUTE, minOfDay % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                transitionTimestamps.add(epochCal.timeInMillis)
            }
        }

        // Find the earliest upcoming transition strictly in the future
        val futureTransitions = transitionTimestamps
            .filter { it > nowMillis }
            .sorted()

        return futureTransitions.firstOrNull() ?: (nowMillis + 15 * 60 * 1000L)
    }

    /**
     * Schedules the next exact widget update using AlarmManager.setExactAndAllowWhileIdle(),
     * with WorkManager periodic background safety net.
     */
    fun scheduleNextWidgetUpdate(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = System.currentTimeMillis()

            // Calculate next prayer state change timestamp
            val nextTransitionMillis = getNextTransitionMillis(context, now)

            // We schedule at transition time + 1 second to ensure clock has ticked into the new prayer minute
            val exactTransitionTarget = nextTransitionMillis + 1000L

            // For countdown fluidity, update at least every 15 minutes or at next prayer change, whichever is sooner
            val countdownMaxInterval = 15 * 60 * 1000L
            val triggerTime = minOf(exactTransitionTarget, now + countdownMaxInterval)

            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_AUTO_UPDATE_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                WIDGET_ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }

            // Also ensure WorkManager periodic fallback is registered as safety net
            scheduleWorkManagerFallback(context)
        } catch (e: Exception) {
            android.util.Log.e("WidgetScheduler", "Error scheduling exact widget update", e)
        }
    }

    /**
     * Enqueues periodic WorkManager job as safety net against deep Doze or OEM task killer restrictions.
     */
    private fun scheduleWorkManagerFallback(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            val fallbackRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_WIDGET_FALLBACK,
                ExistingPeriodicWorkPolicy.KEEP,
                fallbackRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("WidgetScheduler", "Error scheduling WorkManager fallback", e)
        }
    }

    /**
     * Cancels all pending alarms and background work for widget updates.
     */
    fun cancelWidgetUpdates(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_AUTO_UPDATE_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                WIDGET_ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }

            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_WIDGET_FALLBACK)
        } catch (e: Exception) {
            android.util.Log.e("WidgetScheduler", "Error canceling widget updates", e)
        }
    }
}
