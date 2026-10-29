package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the widget responsive rule: width alone decides stacked vs
 * two-column. No per-language, per-model, or fixed-pixel corrections.
 */
class WidgetResponsiveTest {

    @Test
    fun `narrow widths stack`() {
        assertTrue(NoorUpWidgetProvider.isNarrowWidth(180))
        assertTrue(NoorUpWidgetProvider.isNarrowWidth(300))
        assertTrue(NoorUpWidgetProvider.isNarrowWidth(319))
    }

    @Test
    fun `breakpoint and above keep two columns`() {
        assertFalse(NoorUpWidgetProvider.isNarrowWidth(320))
        assertFalse(NoorUpWidgetProvider.isNarrowWidth(400))
        assertFalse(NoorUpWidgetProvider.isNarrowWidth(640))
    }

    @Test
    fun `unknown width falls back to wide`() {
        assertFalse(NoorUpWidgetProvider.isNarrowWidth(0))
        assertFalse(NoorUpWidgetProvider.isNarrowWidth(-50))
    }

    @Test
    fun `prohibited times text grows with width but never below readable size`() {
        val sizes = listOf(200, 260, 330, 400, 600).map { NoorUpWidgetProvider.prohibitedTimesSizeSp(it) }
        assertEquals(sizes.sorted(), sizes) // wider never gets smaller text
        assertTrue(sizes.first() >= 10f)    // was 9sp; the smallest case is now larger
        assertTrue(sizes.last() <= 13f)     // capped so it cannot overflow roomy widgets either
        assertEquals(11f, NoorUpWidgetProvider.prohibitedTimesSizeSp(0), 0.001f) // unknown width
    }
}
