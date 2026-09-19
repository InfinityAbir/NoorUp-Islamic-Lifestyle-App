package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i("BootReceiver", "Device state change: $action. Re-arming exact prayer alarms...")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            try {
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
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error re-arming prayer alarms on boot", e)
            }
        }
    }
}
