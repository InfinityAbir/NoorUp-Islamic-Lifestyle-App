package com.example.ui.noorup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Where the prayer-alarm setup stands, as shown on the Home screen.
 *
 * The alarm controls themselves live on the alarm screen; Home only nudges the
 * user while setup is unfinished and surfaces the reason when it breaks.
 */
enum class PrayerAlarmSetupStatus {
    /** New user: nothing configured yet, show the onboarding banner. */
    NOT_STARTED,

    /** User tapped "Later": keep a compact row instead of nagging with the banner. */
    PROMO_DISMISSED,

    /** Runtime notification permission missing or revoked. */
    NOTIFICATIONS_DENIED,

    /** Android 12+ exact-alarm special access is not granted. */
    EXACT_ALARM_BLOCKED,

    /** Alarms configured and both permissions present: hide the onboarding UI. */
    ACTIVE
}

/**
 * Shared rules + settings routes for the prayer-alarm onboarding flow.
 *
 * Pure [resolveStatus] keeps the Home screen decision testable without a device;
 * the rest wraps the platform checks so UI and ViewModel agree on one source.
 */
object PrayerAlarmOnboarding {

    const val KEY_COMPLETE = "prayer_alarm_onboarding_complete"
    const val KEY_PROMO_DISMISSED = "prayer_alarm_promo_dismissed"
    const val KEY_NOTIFICATION_DENIED = "prayer_alarm_notification_denied"

    /** Android 13+ runtime notification permission; earlier versions are implicitly granted. */
    fun notificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Android 12+ special access needed by the existing AlarmClock-based scheduler. */
    fun exactAlarmAccessGranted(context: Context): Boolean {
        return PrayerNotificationScheduler.isExactAlarmPermissionGranted(context)
    }

    /**
     * Single decision point for the Home banner.
     *
     * Completed setup still needs its permissions re-checked on every resume:
     * a user can revoke them in Settings long after onboarding finished.
     */
    fun resolveStatus(
        onboardingComplete: Boolean,
        promoDismissed: Boolean,
        notificationsGranted: Boolean,
        exactAlarmAccess: Boolean,
        notificationDenied: Boolean
    ): PrayerAlarmSetupStatus {
        if (onboardingComplete) {
            return when {
                !notificationsGranted -> PrayerAlarmSetupStatus.NOTIFICATIONS_DENIED
                !exactAlarmAccess -> PrayerAlarmSetupStatus.EXACT_ALARM_BLOCKED
                else -> PrayerAlarmSetupStatus.ACTIVE
            }
        }
        return when {
            // Denied before finishing: show the compact retry row, not the
            // full banner again (and never re-open the system dialog blindly).
            !notificationsGranted && notificationDenied -> PrayerAlarmSetupStatus.NOTIFICATIONS_DENIED
            promoDismissed -> PrayerAlarmSetupStatus.PROMO_DISMISSED
            else -> PrayerAlarmSetupStatus.NOT_STARTED
        }
    }

    /** App notification settings: the only route once the runtime dialog was denied. */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }

    /** Direct route to the exact-alarm special access screen (Android 12+). */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }

    private fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // No settings activity on this device/emulator: the status row still
            // explains what is missing, so the user is never stuck in a loop.
        }
    }
}
