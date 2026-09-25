package com.example

import com.example.ui.noorup.HijriDateCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGregorianDateCalculation() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 25)
    }

    val result = HijriDateCalculator.calculateGregorianDate(cal)
    assertEquals(25, result.day)
    assertEquals(9, result.month)
    assertEquals(2026, result.year)
    assertEquals("২৫ সেপ্টেম্বর ২০২৬", result.fullDateBn)
    assertEquals("25 September 2026", result.fullDateEn)
  }

  @Test
  fun testHijriDateDynamicCalculation() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 25)
    }

    val result = HijriDateCalculator.calculateHijriDate(cal)
    assertTrue("Hijri day should be between 1 and 30", result.day in 1..30)
    assertTrue("Hijri month should be between 1 and 12", result.month in 1..12)
    assertEquals(1448, result.year)
    assertTrue("Bangla date should contain year in Bengali digits", result.fullDateBn.contains("১৪৪৮"))
    assertTrue("Bangla month name should be non-empty", result.monthNameBn.isNotEmpty())
    assertTrue("English date should contain 1448", result.fullDateEn.contains("1448"))
  }

  @Test
  fun testHijriDateDayProgression() {
    val day1 = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 25)
    }
    val day2 = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 26)
    }

    val res1 = HijriDateCalculator.calculateHijriDate(day1)
    val res2 = HijriDateCalculator.calculateHijriDate(day2)

    // Either the day increments by 1, or it rolled over to day 1 of the next month
    if (res1.month == res2.month) {
      assertEquals(res1.day + 1, res2.day)
    } else {
      assertEquals(1, res2.day)
    }
  }

  @Test
  fun testBanglaDigitsConversion() {
    assertEquals("০১২৩৪৫৬৭৮৯", HijriDateCalculator.toBanglaDigits("0123456789"))
    assertEquals("২৫", HijriDateCalculator.toBanglaDigits("25"))
    assertEquals("২০২৬", HijriDateCalculator.toBanglaDigits("2026"))
  }
}

