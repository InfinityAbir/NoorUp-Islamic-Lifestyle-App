package com.example.ui.noorup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NoorUpWidgetProvider

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Never do slow alarm/widget work on the receiver thread (ANR risk if the
            // system kills us mid-boot leaving alarms unarmed): hand it to the worker.
            try {
                val request = androidx.work.OneTimeWorkRequestBuilder<PrayerRescheduleWorker>().build()
                androidx.work.WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(
                        "noorup_boot_reschedule",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        request
                    )
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to enqueue boot reschedule", e)
            }
        }
    }
}
