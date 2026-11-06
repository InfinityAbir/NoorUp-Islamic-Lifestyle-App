package com.example

import com.example.ui.noorup.journey.JourneyEngine
import com.example.ui.noorup.journey.Prayer
import com.example.ui.noorup.journey.PrayerRecordSource
import com.example.ui.noorup.journey.PrayerStatus
import com.example.ui.noorup.journey.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JourneyEngineTest {

    private val today = LocalDate.of(2026, 11, 6)

    private class FakeSource : PrayerRecordSource {
        val completed = mutableMapOf<LocalDate, Set<Prayer>>()
        val missed = mutableMapOf<LocalDate, Set<Prayer>>()
        val logged = mutableSetOf<LocalDate>()

        override fun isCompleted(date: LocalDate, prayer: Prayer) = completed[date]?.contains(prayer) == true
        override fun isConfirmedMissed(date: LocalDate, prayer: Prayer) = missed[date]?.contains(prayer) == true
        override fun isDayLogged(date: LocalDate) = date in logged

        /** Logs [date] with the first [count] prayers completed. */
        fun track(date: LocalDate, count: Int) {
            logged += date
            completed[date] = Prayer.entries.take(count).toSet()
        }
    }

    private fun daysAgo(n: Int) = today.minusDays(n.toLong())

    // ---- daily completion ----

    @Test
    fun `daily percent is completed over five`() {
        assertEquals(listOf(0, 20, 40, 60, 80, 100), (0..5).map { JourneyEngine.dailyPercent(it) })
    }

    @Test
    fun `day record counts follow the five prayer statuses`() {
        val s = FakeSource().apply { track(daysAgo(1), 3) }
        val day = JourneyEngine.dayRecord(s, daysAgo(1), today)
        assertEquals(3, day.completed)
        assertEquals(60, day.percent)
        assertEquals(0.6f, day.illumination, 0.0001f)
        assertTrue(day.tracked)
    }

    @Test
    fun `unrecorded prayer on a past day is never classified as missed`() {
        val s = FakeSource().apply { track(daysAgo(2), 2) }
        val day = JourneyEngine.dayRecord(s, daysAgo(2), today)
        assertEquals(0, day.confirmedMissed)
        assertEquals(3, day.unrecorded)
        assertEquals(PrayerStatus.UNRECORDED, day.statuses[4])
    }

    @Test
    fun `only an explicit mark makes a prayer confirmed missed`() {
        val s = FakeSource().apply {
            track(daysAgo(2), 1)
            missed[daysAgo(2)] = setOf(Prayer.DHUHR)
        }
        val day = JourneyEngine.dayRecord(s, daysAgo(2), today)
        assertEquals(PrayerStatus.COMPLETED, day.statuses[0])
        assertEquals(PrayerStatus.CONFIRMED_MISSED, day.statuses[1])
        assertEquals(PrayerStatus.UNRECORDED, day.statuses[2])
        assertEquals(1, day.confirmedMissed)
    }

    @Test
    fun `today open prayers are pending not unrecorded`() {
        val s = FakeSource().apply { track(today, 2) }
        val day = JourneyEngine.dayRecord(s, today, today)
        assertEquals(0, day.unrecorded)
        assertEquals(3, day.statuses.count { it == PrayerStatus.PENDING })
    }

    @Test
    fun `future dates are upcoming and never tracked`() {
        val s = FakeSource().apply { logged += today.plusDays(1) }
        val day = JourneyEngine.dayRecord(s, today.plusDays(1), today)
        assertTrue(day.statuses.all { it == PrayerStatus.UPCOMING })
        assertFalse(day.tracked)
    }

    // ---- rolling windows ----

    @Test
    fun `rolling never counts today or future days`() {
        val s = FakeSource().apply {
            track(today, 5)
            track(today.plusDays(1), 5)
        }
        val r = JourneyEngine.rolling(s, today.plusDays(1), 30, today)
        assertEquals(0, r.trackedDays)
        assertNull(r.percent)
    }

    @Test
    fun `rolling skips untracked days instead of counting them as missed`() {
        val s = FakeSource().apply {
            track(daysAgo(3), 5)
            track(daysAgo(9), 5)
        }
        val r = JourneyEngine.rolling(s, daysAgo(1), 30, today)
        assertEquals(2, r.trackedDays)
        assertEquals(10, r.completed)
        assertEquals(100, r.percent)
    }

    @Test
    fun `unrecorded prayers on a tracked day lower rolling consistency`() {
        val s = FakeSource().apply { track(daysAgo(1), 3) }
        assertEquals(60, JourneyEngine.rolling(s, daysAgo(1), 30, today).percent)
    }

    @Test
    fun `window respects its length`() {
        val s = FakeSource().apply {
            track(daysAgo(1), 5)
            track(daysAgo(10), 5)
        }
        assertEquals(1, JourneyEngine.rolling(s, daysAgo(1), 7, today).trackedDays)
        assertEquals(2, JourneyEngine.rolling(s, daysAgo(1), 30, today).trackedDays)
    }

    // ---- stages ----

    @Test
    fun `stage thresholds`() {
        val expected = mapOf(
            0.0 to 1, 19.9 to 1, 20.0 to 2, 39.9 to 2, 40.0 to 3, 59.9 to 3,
            60.0 to 4, 74.9 to 4, 75.0 to 5, 89.9 to 5, 90.0 to 6, 100.0 to 6
        )
        expected.forEach { (score, stage) -> assertEquals("score $score", stage, JourneyEngine.stageForScore(score).number) }
    }

    @Test
    fun `history caps limit how high short histories can climb`() {
        assertEquals(Stage.FIRST_LIGHT, JourneyEngine.historyCap(6))
        assertEquals(Stage.RISING_CRESCENT, JourneyEngine.historyCap(7))
        assertEquals(Stage.RADIANT_HORIZON, JourneyEngine.historyCap(14))
        assertEquals(Stage.CELESTIAL_HARMONY, JourneyEngine.historyCap(30))
        assertEquals(Stage.ETERNAL_LIGHT, JourneyEngine.historyCap(60))
    }

    @Test
    fun `no tracking gives First Light with no percentage`() {
        val r = JourneyEngine.computeStage(FakeSource(), today)
        assertEquals(Stage.FIRST_LIGHT, r.stage)
        assertNull(r.r30.percent)
        assertEquals(0.0, r.score, 0.0)
    }

    @Test
    fun `a perfect new user is capped by history not handed Eternal Light`() {
        val s = FakeSource().apply { (1..5).forEach { track(daysAgo(it), 5) } }
        val r = JourneyEngine.computeStage(s, today)
        assertEquals(Stage.ETERNAL_LIGHT, r.scoreStage)
        assertEquals(Stage.FIRST_LIGHT, r.stage)
        assertTrue(r.cappedByHistory)
    }

    @Test
    fun `sixty perfect tracked days reach Eternal Light`() {
        val s = FakeSource().apply { (1..120).forEach { track(daysAgo(it), 5) } }
        val r = JourneyEngine.computeStage(s, today)
        assertEquals(Stage.ETERNAL_LIGHT, r.stage)
        assertEquals(100.0, r.score, 0.0001)
        assertFalse(r.cappedByHistory)
    }

    /** 120 tracked days where every 10th day has [low] prayers and the rest [high]: uniform in every window. */
    private fun patterned(high: Int, low: Int, lowPerTen: Int): FakeSource = FakeSource().apply {
        (1..120).forEach { track(daysAgo(it), if (it % 10 < lowPerTen) low else high) }
    }

    @Test
    fun `hysteresis keeps the previous stage just under its threshold`() {
        val s = patterned(high = 3, low = 2, lowPerTen = 1) // 58.0
        assertEquals(58.0, JourneyEngine.computeStage(s, today).score, 0.0001)
        assertEquals(Stage.SHINING_CONSTELLATION, JourneyEngine.computeStage(s, today).stage)
        assertEquals(Stage.RADIANT_HORIZON, JourneyEngine.computeStage(s, today, Stage.RADIANT_HORIZON).stage)
    }

    @Test
    fun `hysteresis lets the stage step down once clearly below`() {
        val s = patterned(high = 3, low = 2, lowPerTen = 3) // 54.0
        assertEquals(54.0, JourneyEngine.computeStage(s, today).score, 0.0001)
        assertEquals(Stage.SHINING_CONSTELLATION, JourneyEngine.computeStage(s, today, Stage.RADIANT_HORIZON).stage)
    }

    @Test
    fun `recovery after a hard period climbs back naturally`() {
        val s = FakeSource().apply {
            (1..30).forEach { track(daysAgo(it), 5) }
            (31..90).forEach { track(daysAgo(it), 1) }
        }
        val r = JourneyEngine.computeStage(s, today, previousStage = Stage.CELESTIAL_HARMONY)
        assertTrue("recent consistency must lift the score", r.score > 60.0)
    }

    @Test
    fun `improvement bonus is clamped to plus three points`() {
        val s = FakeSource().apply {
            (1..30).forEach { track(daysAgo(it), 3) }
            (31..90).forEach { track(daysAgo(it), 0) }
        }
        // r30 = 60, r90 = 20, previous 30 = 0 -> trend would be +6, clamped to +3
        assertEquals(60.0 * 2 / 3 + 20.0 / 3 + 3.0, JourneyEngine.computeStage(s, today).score, 0.0001)
    }

    @Test
    fun `decline penalty is clamped to minus three points`() {
        val s = FakeSource().apply {
            (1..30).forEach { track(daysAgo(it), 0) }
            (31..60).forEach { track(daysAgo(it), 5) }
            (61..90).forEach { track(daysAgo(it), 5) }
        }
        // r30 = 0, r90 = 66.67, previous 30 = 100 -> trend would be -10, clamped to -3
        assertEquals(0.0 + (200.0 / 3) / 3 - 3.0, JourneyEngine.computeStage(s, today).score, 0.0001)
    }

    @Test
    fun `calculation is deterministic`() {
        val s = patterned(high = 4, low = 2, lowPerTen = 4)
        assertEquals(JourneyEngine.computeStage(s, today), JourneyEngine.computeStage(s, today))
        assertEquals(JourneyEngine.summary(s, today), JourneyEngine.summary(s, today))
    }

    @Test
    fun `changing a prayer status updates the day and the summary immediately`() {
        val s = FakeSource().apply { track(today, 2) }
        assertEquals(40, JourneyEngine.summary(s, today).today.percent)
        s.completed[today] = Prayer.entries.take(3).toSet()
        assertEquals(60, JourneyEngine.summary(s, today).today.percent)
        s.completed[today] = Prayer.entries.take(1).toSet()
        assertEquals(20, JourneyEngine.summary(s, today).today.percent)
    }
}
