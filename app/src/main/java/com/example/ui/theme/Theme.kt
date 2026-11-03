package com.example.ui.theme

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGlow,
    secondary = GoldAccent,
    tertiary = EmeraldLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    secondaryContainer = Color(0xFF0F3D33),
    onSecondaryContainer = Color(0xFFA7F3D0),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    secondary = Color(0xFFD97706),
    tertiary = Color(0xFF047857),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

/** Warm paper tones for long Quran reading sessions. */
val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF7A5A2E),
    secondary = Color(0xFF9A6A1A),
    tertiary = Color(0xFF5E6B3A),
    background = Color(0xFFEFE4CC),
    surface = Color(0xFFF4EBD6),
    surfaceVariant = Color(0xFFE6D8B8),
    secondaryContainer = Color(0xFFE0CFA6),
    onSecondaryContainer = Color(0xFF3E2E12),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF2E2415),
    onSurface = Color(0xFF2E2415),
    onSurfaceVariant = Color(0xFF5B4A30),
    outline = Color(0xFFC9B68C)
)

/** Near-black with a softer white for low-light reading. */
val NightColorScheme = darkColorScheme(
    primary = EmeraldGlow,
    secondary = GoldAccent,
    tertiary = EmeraldLight,
    background = Color(0xFF05080D),
    surface = Color(0xFF0B1118),
    surfaceVariant = Color(0xFF131B25),
    secondaryContainer = Color(0xFF0F3D33),
    onSecondaryContainer = Color(0xFFA7F3D0),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE6EAF0),
    onSurface = Color(0xFFE6EAF0),
    onSurfaceVariant = Color(0xFFB4BECC),
    outline = Color(0xFF273241)
)

/** Colour scheme for the Quran tab: 1 sepia, 2 night, anything else keeps the app theme (null). */
fun readerColorScheme(theme: Int): androidx.compose.material3.ColorScheme? = when (theme) {
    1 -> SepiaColorScheme
    2 -> NightColorScheme
    else -> null
}

@Composable
fun NoorUpTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val activity = LocalActivity.current
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            val window = activity.window
            // NOTE: Do NOT call window.statusBarColor / navigationBarColor here —
            // those setters are deprecated on Android 15+ and Play flags them.
            // enableNoorUpEdgeToEdge() in MainActivity keeps both bars transparent.
            // Set both icon appearances so three-button navigation stays legible
            // in light and dark themes; gesture navigation has no visible icons.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
