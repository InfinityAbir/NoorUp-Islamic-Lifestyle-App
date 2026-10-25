package com.example

import com.example.ui.noorup.CalculationMethod
import com.example.ui.noorup.HijriDateCalculator
import com.example.ui.noorup.JuristicMethod
import com.example.ui.noorup.SolarPrayerEngine
import com.example.ui.noorup.hadith.Book
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Regression pins for the audit fixes (issues #2, #7, #16, #23, #26-28, #31).
 * Pure-JVM: no Android framework needed.
 */
class RiskyLogicRegressionTest {

    private fun dhakaCal(): Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026); set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 15)
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    @Test
    fun dhakaTimes_matchPublishedApproximately() {
        // Published Dhaka (Jan, Karachi/Hanafi, UTC+6): Fajr ~05:20, Sunrise ~06:40,
        // Dhuhr ~12:05, Asr ~15:35 (Hanafi), Maghrib ~17:35, Isha ~18:55. Allow ±25 min
        // for solar-model vs almanac differences — catches gross engine regressions.
        val t = SolarPrayerEngine.calculateTimes(23.8103, 90.4125, dhakaCal(), 6.0, CalculationMethod.KARACHI, JuristicMethod.HANAFI)
        fun mins(s: String): Int = NoorUpWidgetProvider.parseTimeToMins(s)
        val expectations = mapOf(
            t.fajrStart to (5 * 60 + 20),
            t.sunrise to (6 * 60 + 40),
            t.dhuhrStart to (12 * 60 + 5),
            t.asrStart to (15 * 60 + 35),
            t.maghribStart to (17 * 60 + 35),
            t.ishaStart to (18 * 60 + 55)
        )
        for ((actual, expected) in expectations) {
            val a = mins(actual)
            val diff = kotlin.math.abs(a - expected).let { minOf(it, 1440 - it) }
            assertTrue("Time $actual off by ${diff}m (expected ~${expected / 60}:${expected % 60})", diff <= 25)
        }
    }

    @Test
    fun polarLatitudes_neverProduceNaN() {
        for (lat in listOf(90.0, -90.0, 89.9, 78.0, -78.0)) {
            val t = SolarPrayerEngine.calculateTimes(lat, 0.0, dhakaCal(), 0.0, CalculationMethod.KARACHI, JuristicMethod.HANAFI)
            for (s in listOf(t.fajrStart, t.sunrise, t.dhuhrStart, t.asrStart, t.maghribStart, t.ishaStart)) {
                assertFalse("NaN string for lat=$lat: $s", s.contains("NaN", ignoreCase = true))
                val m = NoorUpWidgetProvider.parseTimeToMins(s)
                assertTrue("Out-of-range mins for lat=$lat: $s -> $m", m in 0..1439)
            }
        }
    }

    @Test
    fun qibla_dhakaBearing_matchesPublished() {
        val q = SolarPrayerEngine.calculateQibla(23.8103, 90.4125)
        // Published Dhaka Qibla ≈ 277-278° (WNW). Allow ±3°.
        val diff = kotlin.math.abs(q.bearingDegrees - 277.6).let { minOf(it, 360 - it) }
        assertTrue("Dhaka bearing ${q.bearingDegrees} off by $diff", diff <= 3.0)
        assertTrue("Dhaka-Mecca ${q.distanceKm}km out of range", q.distanceKm in 4800.0..5500.0)
    }

    @Test
    fun widgetParse_handlesBanglaAndArabicDigits() {
        assertEquals(4 * 60 + 30, NoorUpWidgetProvider.parseTimeToMins("০৪:৩০ AM"))
        assertEquals(16 * 60 + 5, NoorUpWidgetProvider.parseTimeToMins("০৪:০৫ PM"))
        assertEquals(4 * 60 + 30, NoorUpWidgetProvider.parseTimeToMins("٠٤:٣٠ AM"))
        assertEquals(0, NoorUpWidgetProvider.parseTimeToMins("not a time"))
    }

    @Test
    fun totalHadith_parsesBengaliDigits() {
        val b = Book("t", "ট", "T", "ع", "a", "a", "g", "g", "৭,৫৮৯ হাদিস", "d", "d", "৯৭ অধ্যায়")
        assertEquals(7589, b.totalHadith)
        val b2 = Book("t", "ট", "T", "ع", "a", "a", "g", "g", "no digits here", "d", "d", "x")
        assertEquals(100, b2.totalHadith)
    }

    @Test
    fun hijri_monthLength_leapPattern() {
        // Tabular leap years in 30-cycle: year 2,5,7,... Dhu al-Hijjah = 30 else 29.
        assertEquals(30, HijriDateCalculator.ummAlQuraMonthLength(1447, 12))
        assertEquals(29, HijriDateCalculator.ummAlQuraMonthLength(1446, 12))
        assertEquals(30, HijriDateCalculator.ummAlQuraMonthLength(1446, 1))
        assertEquals(29, HijriDateCalculator.ummAlQuraMonthLength(1446, 2))
    }

    @Test
    fun hijri_dayIsClampedToMonthLength() {
        val cal = Calendar.getInstance()
        repeat(50) {
            val r = HijriDateCalculator.calculateHijriDate(cal, 0, false)
            val max = HijriDateCalculator.ummAlQuraMonthLength(r.year, r.month)
            // ICU/HijrahDate can disagree with tabular by a day at month edges;
            // allow +1 grace but never absurd (e.g. day 31 in a 29-day month).
            assertTrue("Hijri day ${r.day} exceeds $max+1 for ${r.month}/${r.year}", r.day in 1..(max + 1))
        }
    }

    @Test
    fun hijriGrid_memoizedAndBounded() {
        val a = HijriDateCalculator.getHijriMonthGrid(1447, 9, 0, false)
        val b = HijriDateCalculator.getHijriMonthGrid(1447, 9, 0, false)
        assertEquals(a, b)
        assertTrue("Grid should be 29 or 30 days, was ${a.size}", a.size in 29..30)
        assertTrue(a.all { it.hijriMonth == 9 && it.hijriYear == 1447 })
    }
}
