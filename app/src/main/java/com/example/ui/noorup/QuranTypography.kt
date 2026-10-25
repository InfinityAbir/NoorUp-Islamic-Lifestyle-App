package com.example.ui.noorup

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import com.example.R

/**
 * Bundled Amiri Quran (SIL OFL, see assets/fonts/AmiriQuran-OFL.txt) renders the
 * Uthmani diacritics with the baselines and spacing Quranic text needs. The
 * platform default font clips/crowds them at reading sizes.
 */
val QuranArabicFont: FontFamily = FontFamily(Font(R.font.amiri_quran))

/** Ayat-end marker (U+06DD) followed by the Ayat number in Arabic-Indic digits. */
fun formatQuranArabic(arabic: String, ayahNumber: Int): String =
    arabic.trimEnd() + "\u0020\u06DD" + ayahNumber.toArabicDigits()

/**
 * Single source of truth for Quranic Arabic typography: bundled font, RTL
 * direction, proportional line height, and the Ayat-end marker. Every Quran
 * surface (reader, bookmarks, Focus) uses this so they cannot drift apart.
 */
@Composable
fun QuranArabicText(
    arabic: String,
    ayahNumber: Int,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = formatQuranArabic(arabic, ayahNumber),
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * 1.85f,
        fontWeight = FontWeight.Normal,
        fontFamily = QuranArabicFont,
        textAlign = textAlign,
        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
        modifier = modifier
    )
}
