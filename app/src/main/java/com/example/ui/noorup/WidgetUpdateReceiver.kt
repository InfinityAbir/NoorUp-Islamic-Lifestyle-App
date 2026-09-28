package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NoorUpWidgetProvider

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            // Update all active NoorUp widgets on the home screen
            NoorUpWidgetProvider.updateAllWidgets(context)

            // Re-schedule next exact prayer transition alarm
            WidgetUpdateScheduler.scheduleNextWidgetUpdate(context)
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateReceiver", "Error processing widget update broadcast", e)
        }
    }
}
