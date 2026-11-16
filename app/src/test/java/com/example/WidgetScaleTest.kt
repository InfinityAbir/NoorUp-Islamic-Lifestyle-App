package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetScaleTest {

    @Test
    fun `design size gives scale one`() {
        assertEquals(1f, WidgetScale.scaleFor(400, 275, narrow = false, isEnglish = false), 0.01f)
        assertEquals(1f, WidgetScale.scaleFor(250, 459, narrow = true, isEnglish = true), 0.01f)
    }

    @Test
    fun `bigger cell scales up, smaller scales down`() {
        assertTrue(WidgetScale.scaleFor(560, 400, false, false) > 1.3f)
        assertTrue(WidgetScale.scaleFor(340, 150, false, false) < 0.7f)
    }

    @Test
    fun `scale is limited by the tighter dimension`() {
        // Very wide but short: height decides, so content does not overflow.
        val s = WidgetScale.scaleFor(640, 170, false, false)
        assertEquals((170f - 20f) / (262f * 1.05f - 20f), s, 0.01f)
    }

    @Test
    fun `scale is clamped`() {
        assertEquals(WidgetScale.MAX_SCALE, WidgetScale.scaleFor(2000, 2000, false, false), 0.0001f)
        assertEquals(WidgetScale.MIN_SCALE, WidgetScale.scaleFor(50, 50, false, false), 0.0001f)
    }

    @Test
    fun `unknown size keeps the original look`() {
        assertEquals(1f, WidgetScale.scaleFor(0, 0, false, false), 0.0001f)
        assertEquals(1f, WidgetScale.scaleFor(400, 0, false, true), 0.0001f)
    }

    @Test
    fun `bengali is taller than english, as measured on device`() {
        assertTrue(WidgetScale.designHeightDp(false, false) > WidgetScale.designHeightDp(false, true))
        assertTrue(WidgetScale.designHeightDp(true, false) > WidgetScale.designHeightDp(true, true))
    }

    @Test
    fun `header date shrinks to fit beside brand and city`() {
        val roomy = WidgetScale.headerDateSp(11.5f, 1f, 400, false, 6, 13, 25)
        assertEquals(11.5f, roomy, 0.01f)
        val tight = WidgetScale.headerDateSp(11.5f, 1f, 250, true, 6, 13, 25)
        assertTrue(tight < 11.5f)
        assertTrue(tight >= WidgetScale.MIN_HEADER_SP)
    }
}
