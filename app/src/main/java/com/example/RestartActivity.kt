package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.core.content.IntentCompat

/**
 * Runs in its own process (see the manifest) so it can kill the main process and then reopen the app.
 * Needed after a backup restore: the ViewModels cache preferences, so a cold start is the safe way to load them.
 */
class RestartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainPid = intent.getIntExtra(EXTRA_PID, -1)
        val next = IntentCompat.getParcelableExtra(intent, EXTRA_NEXT, Intent::class.java)
        if (mainPid > 0 && mainPid != Process.myPid()) Process.killProcess(mainPid)
        if (next != null) startActivity(next)
        finish()
        Runtime.getRuntime().exit(0)
    }

    companion object {
        private const val EXTRA_PID = "main_pid"
        private const val EXTRA_NEXT = "next_intent"

        /** Restarts the whole app. Call from the main process. */
        fun restartApp(activity: Activity) {
            val launch = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) ?: return
            activity.startActivity(
                Intent(activity, RestartActivity::class.java)
                    .putExtra(EXTRA_PID, Process.myPid())
                    .putExtra(EXTRA_NEXT, launch)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
