package com.example

import com.example.ui.noorup.ResponsiveBreakpoints
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression tests for the responsive layout rules.
 *
 * Guards the cross-device clipping fixes: carousel cards must never exceed the
 * available viewport (the 300dp-card-on-288dp-screen overflow class of bug).
 */
class ResponsiveBreakpointsTest {

  @Test
  fun `wide viewport keeps preferred card width`() {
    assertEquals(300f, ResponsiveBreakpoints.carouselCardWidth(412f, 300f))
    assertEquals(260f, ResponsiveBreakpoints.carouselCardWidth(412f, 260f))
  }

  @Test
  fun `narrow viewport shrinks card to peek fraction`() {
    // 320dp phone minus 32dp screen padding = 288dp available
    assertEquals(288f * 0.85f, ResponsiveBreakpoints.carouselCardWidth(288f, 300f))
    // Compact 280dp viewport with 260dp preferred card
    assertEquals(280f * 0.85f, ResponsiveBreakpoints.carouselCardWidth(280f, 260f), 0.001f)
  }

  @Test
  fun `card width never drops below readable minimum`() {
    assertEquals(120f, ResponsiveBreakpoints.carouselCardWidth(100f, 300f))
  }

  @Test
  fun `invalid input falls back to preferred width`() {
    assertEquals(300f, ResponsiveBreakpoints.carouselCardWidth(0f, 300f))
    assertEquals(300f, ResponsiveBreakpoints.carouselCardWidth(-50f, 300f))
  }

  @Test
  fun `custom peek fraction is honored`() {
    assertEquals(288f * 0.9f, ResponsiveBreakpoints.carouselCardWidth(288f, 300f, peekFraction = 0.9f))
  }
}
