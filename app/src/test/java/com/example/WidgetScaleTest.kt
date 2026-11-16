package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetScaleTest {

    @Test
    fun `design size gives scale one`() {
        assertEquals(1f, WidgetScale.scaleFor(340, 208, narrow = false, isEnglish = false), 0.001f)
        assertEquals(1f, WidgetScale.scaleFor(250, 385, narrow = true, isEnglish = true), 0.001f)
    }

    @Test
    fun `bigger cell scales up, smaller scales down`() {
        assertTrue(WidgetScale.scaleFor(510, 400, false, false) > 1.3f)
        assertTrue(WidgetScale.scaleFor(340, 150, false, false) < 0.8f)
    }

    @Test
    fun `scale is limited by the tighter dimension`() {
        // Very wide but short: height decides, so content does not overflow.
        val s = WidgetScale.scaleFor(640, 150, false, false)
        assertEquals(150f / 208f, s, 0.001f)
    }

    @Test
    fun `scale is clamped`() {
        assertEquals(WidgetScale.MAX_SCALE, WidgetScale.scaleFor(2000, 2000, false, false), 0.0001f)
        assertEquals(WidgetScale.MIN_SCALE, WidgetScale.scaleFor(50, 50, false, false), 0.0001f)
    }

    @Test
    fun `unknown size keeps the original look`() {
        assertEquals(1f, WidgetScale.scaleFor(0, 0, false, false), 0.0001f)
        assertEquals(340f / 340f, WidgetScale.scaleFor(340, 0, false, true), 0.0001f)
    }

    @Test
    fun `english needs slightly more height than bengali`() {
        assertTrue(WidgetScale.designHeightDp(false, true) > WidgetScale.designHeightDp(false, false))
    }

    @Test
    fun `footer text never exceeds the scaled base size and stays readable`() {
        val sp = WidgetScale.prohibitedTimesSp(12f, 1f, 340, false, 14, 39)
        assertTrue(sp <= 12f)
        assertTrue(sp >= 11f) // two-line English footer keeps its normal size on a normal widget
        val tiny = WidgetScale.prohibitedTimesSp(12f, 0.6f, 180, true, 14, 60)
        assertTrue(tiny >= WidgetScale.MIN_FOOTER_SP)
    }

    @Test
    fun `footer line counting`() {
        val html = "Sunrise 06:14–06:30 • Zawal 11:28–11:42<br><font color='#EF4444'>🔥 Sunset 04:54–05:11</font>"
        assertEquals(39, WidgetScale.longestFooterLine(html, wrapsInTwoLines = false))
        assertTrue(WidgetScale.longestFooterLine("a".repeat(40), wrapsInTwoLines = true) < 40)
    }
}
