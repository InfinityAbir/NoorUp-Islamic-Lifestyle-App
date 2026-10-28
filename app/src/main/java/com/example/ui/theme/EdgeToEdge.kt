package com.example.ui.theme

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat

/**
 * NoorUp-owned edge-to-edge setup.
 *
 * Why this exists instead of `androidx.activity.enableEdgeToEdge()`:
 * Activity 1.13.0's helper calls the deprecated `Window.setStatusBarColor()` /
 * `Window.setNavigationBarColor()` setters on every API level, and sets
 * `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` on API 28+. Those calls are what
 * Play Console reports as "Deprecated APIs" for this app.
 *
 * This helper keeps the same visual result without touching those setters:
 *  - both bars stay transparent through `Theme.NoorUp` (theme attributes are
 *    not runtime API calls and remain valid);
 *  - API 26-34 draw behind the bars via `decorFitsSystemWindows = false`;
 *  - API 35+ is edge-to-edge by platform default for targetSdk 35+, so no
 *    window call is needed there;
 *  - API 30-34 use the non-deprecated `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`
 *    so content can reach the screen edges while `safeDrawing` insets keep
 *    controls out of the cutout;
 *  - navigation-bar contrast is left to the system, which keeps three-button
 *    navigation legible over the bottom bar.
 *
 * Bar icon appearance (light/dark) is applied from the Compose theme, where the
 * app's own dark-mode preference is known. See [NoorUpTheme].
 */
fun Activity.enableNoorUpEdgeToEdge() {
    val window = window

    // targetSdk 35+ is always edge-to-edge on API 35+. Calling the deprecated
    // decor-fits setter there is unnecessary; below 35 we opt in explicitly.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    // ALWAYS was added in API 30. It lets content use the short edges of a
    // cutout; safeDrawing insets still protect all interactive content.
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.R until Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Not deprecated. Matches the default enableEdgeToEdge() behaviour:
        // three-button navigation gets a contrast scrim when needed, gesture
        // navigation keeps a fully transparent bar.
        window.isNavigationBarContrastEnforced = true
    }
}
