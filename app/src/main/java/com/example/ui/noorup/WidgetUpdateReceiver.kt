package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NoorUpWidgetProvider

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            // Battery fix: widget tick (every ≤15 min) must NOT re-arm 5× setAlarmClock.
            // Prayer alarms re-arm themselves on fire; re-arm here only on wall-clock change.
            val isClockChange = intent.action == Intent.ACTION_TIME_CHANGED ||
                intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
                intent.action == "android.intent.action.DATE_CHANGED"
            // Update all active NoorUp widgets on the home screen
            NoorUpWidgetProvider.updateAllWidgets(context)

            // Re-schedule next exact prayer transition alarm for widget
            WidgetUpdateScheduler.scheduleNextWidgetUpdate(context)

            // On wall-clock changes the fixed 10PM/9AM garden+hadith alarms would stay
            // anchored to the pre-change clock — re-anchor them too.
            if (isClockChange) {
                PrayerNotificationScheduler.scheduleAllPrayerAlerts(context)
                PrayerNotificationScheduler.scheduleDailyTasks(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateReceiver", "Error processing widget update broadcast", e)
        }
    }
}
