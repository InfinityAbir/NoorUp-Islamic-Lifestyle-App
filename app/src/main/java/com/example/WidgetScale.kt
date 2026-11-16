package com.example

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Pure sizing rules that make the NoorUp home widget scale with its launcher cell, so text and
 * paddings grow and shrink together when the user resizes it (no Android dependencies, JVM-testable).
 *
 * The layouts are designed at a reference size (see [designWidthDp] / [designHeightDp]). The scale is
 * the largest factor at which that design still fits the real cell: width-limited for wide cells,
 * height-limited for short ones, so nothing clips and nothing is left tiny in a big cell.
 */
object WidgetScale {
    const val MIN_SCALE = 0.6f
    const val MAX_SCALE = 1.8f

    /** Width the layouts were designed for. */
    fun designWidthDp(narrow: Boolean): Float = if (narrow) 250f else 340f

    /**
     * Content height at scale 1. English is a little taller: its prohibited-time footer takes
     * two lines (wide) or wraps (narrow), Bengali stays on one.
     */
    fun designHeightDp(narrow: Boolean, isEnglish: Boolean): Float = when {
        narrow && isEnglish -> 385f
        narrow -> 365f
        isEnglish -> 224f
        else -> 208f
    }

    /**
     * Scale for a cell of [widthDp] x [heightDp]. An unknown size (<= 0) falls back to the other
     * dimension, or 1 when both are unknown, so a launcher that reports nothing keeps the old look.
     */
    fun scaleFor(widthDp: Int, heightDp: Int, narrow: Boolean, isEnglish: Boolean): Float {
        val byWidth = if (widthDp > 0) widthDp / designWidthDp(narrow) else null
        val byHeight = if (heightDp > 0) heightDp / designHeightDp(narrow, isEnglish) else null
        val raw = when {
            byWidth != null && byHeight != null -> min(byWidth, byHeight)
            byWidth != null -> byWidth
            byHeight != null -> byHeight
            else -> 1f
        }
        return raw.coerceIn(MIN_SCALE, MAX_SCALE)
    }

    /** Average glyph advance relative to the font size (measured ~0.42 on device); padded so text is not clipped. */
    private const val GLYPH_EM = 0.47f

    /**
     * Font size (sp) for the prohibited-times footer. It is sized in code, because Android ignores
     * text-size changes on an auto-sizing TextView: the scaled base size, capped so the longest
     * line still fits the width left beside the label.
     *
     * @param longestLineChars characters on the longest line the footer will render
     */
    fun prohibitedTimesSp(
        baseSp: Float,
        scale: Float,
        widthDp: Int,
        narrow: Boolean,
        labelChars: Int,
        longestLineChars: Int,
        fontScale: Float = 1f,
    ): Float {
        val wide = if (widthDp > 0) widthDp.toFloat() else designWidthDp(narrow) * scale
        val labelSp = baseSp * scale
        val labelWidth = labelChars * GLYPH_EM * labelSp * fontScale
        val chrome = (2 * (10f + 8f) + 6f) * scale // root + bar padding and the gap after the label
        val available = wide - chrome - labelWidth
        val wanted = baseSp * scale
        if (longestLineChars <= 0 || available <= 0f) return max(MIN_FOOTER_SP, wanted * 0.7f)
        val fit = available / (longestLineChars * GLYPH_EM * fontScale)
        return min(wanted, fit).coerceAtLeast(MIN_FOOTER_SP)
    }

    const val MIN_FOOTER_SP = 7f

    /** Characters on the longest rendered line of the footer text (markup removed). */
    fun longestFooterLine(html: String, wrapsInTwoLines: Boolean): Int {
        val lines = html.replace("<br>", "\n").replace(Regex("<[^>]*>"), "").split("\n")
        val longest = lines.maxOfOrNull { it.length } ?: 0
        // A free-wrapping two-line footer splits the text roughly in half.
        return if (wrapsInTwoLines && lines.size == 1) ceil(longest / 2f).toInt() + 3 else longest
    }
}
