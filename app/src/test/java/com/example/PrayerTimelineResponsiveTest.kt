package com.example

import com.example.ui.noorup.ResponsiveBreakpoints
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the prayer-timetable layout rules.
 *
 * Card content width is screen width minus 88dp (16dp screen padding on each
 * side, 16dp widget padding on each side, 12dp card padding on each side):
 *   320dp -> 232dp, 360dp -> 272dp, 390dp -> 302dp, 412dp -> 324dp.
 *
 * The prohibited strip is screen width minus 64dp:
 *   320dp -> 256dp, 360dp -> 296dp, 390dp -> 326dp, 412dp -> 348dp.
 */
class PrayerTimelineResponsiveTest {

    @Test
    fun `time range stacks on 320 360 and 390 dp phones`() {
        assertTrue(ResponsiveBreakpoints.prayerTimelineStacksTime(232f))
        assertTrue(ResponsiveBreakpoints.prayerTimelineStacksTime(272f))
        assertTrue(ResponsiveBreakpoints.prayerTimelineStacksTime(302f))
    }

    @Test
    fun `time range stays inline on a 412 dp phone`() {
        assertFalse(ResponsiveBreakpoints.prayerTimelineStacksTime(324f))
    }

    @Test
    fun `large font scale stacks even on the widest phone`() {
        assertTrue(ResponsiveBreakpoints.prayerTimelineStacksTime(324f, fontScale = 1.3f))
        assertTrue(ResponsiveBreakpoints.prayerTimelineStacksTime(324f, fontScale = 2.0f))
    }

    @Test
    fun `normal font scale keeps the inline layout above the breakpoint`() {
        assertFalse(ResponsiveBreakpoints.prayerTimelineStacksTime(324f, fontScale = 1.0f))
        assertFalse(ResponsiveBreakpoints.prayerTimelineStacksTime(320f, fontScale = 1.0f))
    }

    @Test
    fun `prohibited chips stack on 320 360 and 390 dp phones`() {
        assertTrue(ResponsiveBreakpoints.prohibitedChipsStack(256f))
        assertTrue(ResponsiveBreakpoints.prohibitedChipsStack(296f))
        assertTrue(ResponsiveBreakpoints.prohibitedChipsStack(326f))
    }

    @Test
    fun `prohibited chips keep three columns on a 412 dp phone`() {
        assertFalse(ResponsiveBreakpoints.prohibitedChipsStack(348f))
    }

    @Test
    fun `prohibited chips stack for large font scales`() {
        assertTrue(ResponsiveBreakpoints.prohibitedChipsStack(348f, fontScale = 1.3f))
    }

    @Test
    fun `unknown or zero widths fall back to the stacked layout`() {
        assertTrue(ResponsiveBreakpoints.prayerTimelineStacksTime(0f))
        assertTrue(ResponsiveBreakpoints.prayerTimelineStacksTime(-10f))
        assertTrue(ResponsiveBreakpoints.prohibitedChipsStack(0f))
        assertTrue(ResponsiveBreakpoints.prohibitedChipsStack(-10f))
    }
}
