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
    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 1.8f

    /** Width the layouts were designed for. */
    fun designWidthDp(narrow: Boolean): Float = if (narrow) 250f else 400f

    /**
     * Content height at scale 1, measured on a phone with the debug preview (natural height at the
     * design width). English wide is a little taller: its footer takes two lines.
     */
    fun designHeightDp(narrow: Boolean, isEnglish: Boolean): Float = when {
        narrow && isEnglish -> 437f
        narrow -> 452f
        isEnglish -> 254f
        else -> 262f
    }

    /** Vertical dp that stay the same at every scale (card gaps, divider, progress bar, line gaps). */
    fun fixedHeightDp(narrow: Boolean): Float = if (narrow) 40f else 20f

    /**
     * Scale for a cell of [widthDp] x [heightDp]. An unknown size (<= 0) falls back to the other
     * dimension, or 1 when both are unknown, so a launcher that reports nothing keeps the old look.
     */
    fun scaleFor(widthDp: Int, heightDp: Int, narrow: Boolean, isEnglish: Boolean): Float {
        val byWidth = if (widthDp > 0) widthDp / designWidthDp(narrow) else null
        val byHeight = if (heightDp > 0) {
            // Margins, the divider and the progress bar do not scale, so only the rest of the
            // height is proportional to the scale.
            val fixed = fixedHeightDp(narrow)
            // 5% safety: text metrics differ a little between fonts and font-size settings.
            (heightDp - fixed) / (designHeightDp(narrow, isEnglish) * 1.05f - fixed)
        } else null
        val raw = when {
            byWidth != null && byHeight != null -> min(byWidth, byHeight)
            byWidth != null -> byWidth
            byHeight != null -> byHeight
            else -> 1f
        }
        return raw.coerceIn(MIN_SCALE, MAX_SCALE)
    }

    /** Average glyph advance relative to the font size (measured ~0.52 for digits + Latin on device); padded so text is not clipped. */
    private const val GLYPH_EM = 0.56f

    /**
     * Font size (sp) of the Hijri date in the header, which shares one row with the brand and the
     * city: the scaled base size, capped so the date fits the width they leave over.
     */
    fun headerDateSp(
        baseSp: Float,
        scale: Float,
        widthDp: Int,
        narrow: Boolean,
        brandChars: Int,
        cityChars: Int,
        dateChars: Int,
        fontScale: Float = 1f,
        ownLine: Boolean = false,
    ): Float {
        val wide = if (widthDp > 0) widthDp.toFloat() else designWidthDp(narrow) * scale
        val bold = 0.6f // brand and city are bold: slightly wider glyphs
        // On the narrow layout the date has its own line, so brand and city take no width from it.
        val brand = if (ownLine) 0f else brandChars * bold * 13f * scale * fontScale
        val city = if (ownLine) 0f else cityChars * bold * 11f * scale * fontScale
        val available = wide - 2 * 10f * scale - brand - city - 14f
        val wanted = baseSp * scale
        if (dateChars <= 0 || available <= 0f) return max(MIN_HEADER_SP, wanted * 0.7f)
        val fit = available / (dateChars * GLYPH_EM * fontScale)
        return min(wanted, fit).coerceAtLeast(MIN_HEADER_SP)
    }

    const val MIN_HEADER_SP = 7.5f
}
