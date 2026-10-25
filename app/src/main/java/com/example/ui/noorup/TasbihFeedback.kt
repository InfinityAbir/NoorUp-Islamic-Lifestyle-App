package com.example.ui.noorup

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings

/**
 * System-respecting tasbih feedback.
 *
 * Tap feedback uses Compose's LocalHapticFeedback (which honours the user's
 * touch-feedback setting). Completion vibration is only attempted when the
 * device has a vibrator and Android's haptic-feedback setting is enabled.
 * Animation decisions also honour the system animator duration scale, so a
 * user with "remove animations" gets a static celebration.
 */
object TasbihFeedback {

    fun isReducedMotion(context: Context): Boolean = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }.getOrDefault(false)

    fun hapticFeedbackEnabled(context: Context): Boolean = runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1
        ) == 1
    }.getOrDefault(true)

    fun vibrateCompletion(context: Context) {
        if (!hapticFeedbackEnabled(context)) return
        val vibrator = runCatching {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }.getOrNull() ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            // Soft double pulse: noticeable without being jarring.
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 55L, 45L, 110L),
                    -1
                )
            )
        }
    }
}
