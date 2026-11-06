package com.example

import com.example.ui.noorup.journey.jDate
import com.example.ui.noorup.journey.jDateLong
import com.example.ui.noorup.journey.jDelta
import com.example.ui.noorup.journey.jMonth
import com.example.ui.noorup.journey.jNum
import com.example.ui.noorup.journey.jPct
import com.example.ui.noorup.journey.toBanglaDateText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class JourneyFormatTest {
    private val friday = LocalDate.of(2026, 11, 6)

    @Test
    fun `english dates are unchanged`() {
        assertEquals("06 Nov", jDate(friday, true))
        assertEquals("Fri, 06 Nov 2026", jDateLong(friday, true))
        assertEquals("November 2026", jMonth(YearMonth.of(2026, 11), true))
    }

    @Test
    fun `bangla dates use bangla digits month and weekday names`() {
        assertEquals("০৬ নভে", jDate(friday, false))
        assertEquals("শুক্র, ০৬ নভে ২০২৬", jDateLong(friday, false))
        assertEquals("নভেম্বর ২০২৬", jMonth(YearMonth.of(2026, 11), false))
    }

    @Test
    fun `every month and weekday has a bangla name and none keeps latin letters`() {
        (1..12).forEach {
            val short = jDate(LocalDate.of(2026, it, 1), false)
            assertFalse("month $it: $short", short.any { c -> c in 'A'..'Z' || c in 'a'..'z' })
        }
        (0..6).forEach {
            val long = jDateLong(LocalDate.of(2026, 11, 2).plusDays(it.toLong()), false)
            assertFalse("weekday $it: $long", long.any { c -> c in 'A'..'Z' || c in 'a'..'z' })
        }
    }

    @Test
    fun `march and may are not mangled by shorter month names`() {
        assertEquals("০১ মার্চ", toBanglaDateText("01 Mar"))
        assertEquals("০১ মে", toBanglaDateText("01 May"))
        assertEquals("মার্চ ২০২৬", toBanglaDateText("March 2026"))
    }

    @Test
    fun `numbers and percentages`() {
        assertEquals("94%", jPct(94, true))
        assertEquals("৯৪%", jPct(94, false))
        assertEquals("—", jPct(null, true))
        assertEquals("৩", jNum(3, false))
        assertEquals("3", jNum(3, true))
    }

    @Test
    fun `deltas use neutral wording and singular point`() {
        assertEquals("+5 points", jDelta(5, true))
        assertEquals("−1 point", jDelta(-1, true))
        assertEquals("±0 points", jDelta(0, true))
        assertEquals("+৫ পয়েন্ট", jDelta(5, false))
    }
}
