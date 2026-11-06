package com.example

import com.example.ui.noorup.journey.JourneyEngine
import com.example.ui.noorup.journey.MilestoneId
import com.example.ui.noorup.journey.Prayer
import com.example.ui.noorup.journey.PrayerRecordSource
import com.example.ui.noorup.journey.PrayerStatus
import com.example.ui.noorup.journey.TodayTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** M3-M6 engine behaviour: timed statuses, milestones, period analytics, Friday window. */
class JourneyAnalyticsEngineTest {

    private val today = LocalDate.of(2026, 11, 6) // a Friday

    private class FakeSource : PrayerRecordSource {
        val completed = mutableMapOf<LocalDate, Set<Prayer>>()
        val missed = mutableMapOf<LocalDate, Set<Prayer>>()
        val logged = mutableSetOf<LocalDate>()
        override fun isCompleted(date: LocalDate, prayer: Prayer) = completed[date]?.contains(prayer) == true
        override fun isConfirmedMissed(date: LocalDate, prayer: Prayer) = missed[date]?.contains(prayer) == true
        override fun isDayLogged(date: LocalDate) = date in logged
        fun track(date: LocalDate, count: Int) {
            logged += date
            completed[date] = Prayer.entries.take(count).toSet()
        }
    }

    private fun daysAgo(n: Int) = today.minusDays(n.toLong())

    // ---- timed statuses ----

    @Test
    fun `today prayers before their start are upcoming and after are pending`() {
        val s = FakeSource().apply { track(today, 1) }
        val timing = TodayTiming(13 * 60, listOf(290, 702, 941, 1037, 1114))
        val day = JourneyEngine.dayRecord(s, today, today, timing)
        assertEquals(PrayerStatus.COMPLETED, day.statuses[0])
        assertEquals(PrayerStatus.PENDING, day.statuses[1])
        assertEquals(PrayerStatus.UPCOMING, day.statuses[2])
        assertEquals(PrayerStatus.UPCOMING, day.statuses[3])
        assertEquals(PrayerStatus.UPCOMING, day.statuses[4])
        assertEquals(0, day.unrecorded)
    }

    @Test
    fun `completed stays completed before its time and timing is ignored for past days`() {
        val s = FakeSource().apply {
            track(today, 5)
            track(daysAgo(1), 1)
        }
        val timing = TodayTiming(60, listOf(290, 702, 941, 1037, 1114))
        assertTrue(JourneyEngine.dayRecord(s, today, today, timing).statuses.all { it == PrayerStatus.COMPLETED })
        assertEquals(PrayerStatus.UNRECORDED, JourneyEngine.dayRecord(s, daysAgo(1), today, timing).statuses[1])
    }

    @Test
    fun `a minute passing flips upcoming to pending`() {
        val s = FakeSource()
        val starts = listOf(290, 702, 941, 1037, 1114)
        assertEquals(PrayerStatus.UPCOMING, JourneyEngine.dayRecord(s, today, today, TodayTiming(940, starts)).statuses[2])
        assertEquals(PrayerStatus.PENDING, JourneyEngine.dayRecord(s, today, today, TodayTiming(941, starts)).statuses[2])
    }

    @Test
    fun `clock parsing`() {
        assertEquals(17 * 60 + 17, JourneyEngine.parseClockMinutes("05:17 PM"))
        assertEquals(4 * 60 + 50, JourneyEngine.parseClockMinutes("04:50 AM"))
        assertEquals(0, JourneyEngine.parseClockMinutes("12:00 AM"))
        assertEquals(12 * 60, JourneyEngine.parseClockMinutes("12:00 PM"))
        assertEquals(23 * 60 + 5, JourneyEngine.parseClockMinutes("23:05"))
        assertNull(JourneyEngine.parseClockMinutes("garbage"))
        assertNull(JourneyEngine.parseClockMinutes("13:00 PM"))
        assertNull(JourneyEngine.parseClockMinutes("10:75 AM"))
    }

    // ---- milestones ----

    @Test
    fun `no records means no milestones`() {
        assertTrue(JourneyEngine.milestoneDates(FakeSource(), today).isEmpty())
    }

    @Test
    fun `first complete day is the first five of five`() {
        val s = FakeSource().apply {
            track(daysAgo(5), 3)
            track(daysAgo(4), 5)
            track(daysAgo(2), 5)
        }
        assertEquals(daysAgo(4), JourneyEngine.milestoneDates(s, today)[MilestoneId.FIRST_DAY])
    }

    @Test
    fun `first week needs seven consecutive tracked days and a gap resets it`() {
        val s = FakeSource().apply {
            (10 downTo 8).forEach { track(daysAgo(it), 1) }
            (6 downTo 0).forEach { track(daysAgo(it), 1) }
        }
        assertEquals(today, JourneyEngine.milestoneDates(s, today)[MilestoneId.FIRST_WEEK])
        val broken = FakeSource().apply {
            (6 downTo 4).forEach { track(daysAgo(it), 1) }
            (2 downTo 0).forEach { track(daysAgo(it), 1) }
        }
        assertNull(JourneyEngine.milestoneDates(broken, today)[MilestoneId.FIRST_WEEK])
    }

    @Test
    fun `day count milestones use tracked days not consecutive days`() {
        val s = FakeSource().apply { (1..60).forEach { track(daysAgo(it * 2), 2) } }
        val m = JourneyEngine.milestoneDates(s, today)
        assertEquals(daysAgo(120 - 29 * 2), m[MilestoneId.DAYS_30])
        assertNull(m[MilestoneId.DAYS_90])
        assertNull(m[MilestoneId.FIRST_WEEK])
    }

    @Test
    fun `a year of tracking unlocks every milestone`() {
        val s = FakeSource().apply { (0..370).forEach { track(daysAgo(it), 5) } }
        assertEquals(MilestoneId.entries.toSet(), JourneyEngine.milestoneDates(s, today).keys)
    }

    // ---- period analytics ----

    @Test
    fun `period excludes today by default and includes it on request`() {
        val s = FakeSource().apply { track(today, 5); track(daysAgo(1), 5) }
        val p = JourneyEngine.period(s, daysAgo(2), today, today)
        assertEquals(1, p.trackedDays)
        assertEquals(5, p.completed)
        assertEquals(2, JourneyEngine.period(s, daysAgo(2), today, today, includeToday = true).trackedDays)
    }

    @Test
    fun `period counts completed missed unrecorded and untracked separately`() {
        val s = FakeSource().apply {
            track(daysAgo(1), 3)
            track(daysAgo(2), 1)
            missed[daysAgo(2)] = setOf(Prayer.ASR)
        }
        val p = JourneyEngine.period(s, daysAgo(3), daysAgo(1), today)
        assertEquals(4, p.completed)
        assertEquals(1, p.confirmedMissed)
        assertEquals(5, p.unrecorded)
        assertEquals(2, p.trackedDays)
        assertEquals(1, p.untrackedDays)
        assertEquals(0, p.completeDays)
        assertEquals(40, p.consistency)
    }

    @Test
    fun `per prayer tallies and filtered consistency`() {
        val s = FakeSource().apply {
            (1..4).forEach { track(daysAgo(it), 0) }
            completed[daysAgo(1)] = setOf(Prayer.FAJR)
            completed[daysAgo(2)] = setOf(Prayer.FAJR)
            completed[daysAgo(3)] = setOf(Prayer.FAJR, Prayer.ISHA)
        }
        val p = JourneyEngine.period(s, daysAgo(4), daysAgo(1), today)
        assertEquals(3, p.perPrayer[Prayer.FAJR.ordinal].completed)
        assertEquals(1, p.perPrayer[Prayer.FAJR.ordinal].unrecorded)
        assertEquals(75, p.consistencyFor(Prayer.FAJR))
        assertEquals(25, p.consistencyFor(Prayer.ISHA))
        assertEquals(0, p.consistencyFor(Prayer.ASR))
    }

    @Test
    fun `empty period has no consistency`() {
        val p = JourneyEngine.period(FakeSource(), daysAgo(7), daysAgo(1), today)
        assertNull(p.consistency)
        assertNull(p.consistencyFor(Prayer.FAJR))
        assertEquals(7, p.untrackedDays)
    }

    @Test
    fun `week and month windows`() {
        val s = FakeSource().apply { track(daysAgo(1), 5) }
        assertEquals(7, JourneyEngine.week(s, daysAgo(1), today).days.size)
        assertEquals(1, JourneyEngine.month(s, YearMonth.from(daysAgo(1)), today).trackedDays)
    }

    @Test
    fun `future months have no days`() {
        val s = FakeSource()
        assertTrue(JourneyEngine.month(s, YearMonth.of(2027, 1), today).days.isEmpty())
    }

    @Test
    fun `incomplete history is reported as untracked not missed`() {
        val s = FakeSource().apply { track(daysAgo(1), 5) }
        val p = JourneyEngine.week(s, daysAgo(1), today)
        assertEquals(6, p.untrackedDays)
        assertEquals(0, p.confirmedMissed)
        assertEquals(0, p.unrecorded)
        assertEquals(100, p.consistency)
    }

    // ---- Friday window ----

    @Test
    fun `latest friday is on or before today`() {
        assertEquals(DayOfWeek.FRIDAY, today.dayOfWeek)
        assertEquals(today, JourneyEngine.latestFriday(today))
        (0..6).forEach { assertEquals(today, JourneyEngine.latestFriday(today.plusDays(it.toLong()))) }
        assertEquals(today.plusDays(7), JourneyEngine.latestFriday(today.plusDays(7)))
        assertEquals(today.minusDays(7), JourneyEngine.latestFriday(today.minusDays(1)))
    }

    @Test
    fun `friday summary covers the seven completed days ending thursday and never counts friday itself`() {
        val s = FakeSource().apply {
            track(today, 5)
            (1..7).forEach { track(today.minusDays(it.toLong()), 5) }
            track(today.minusDays(8), 5)
        }
        val w = JourneyEngine.fridaySummary(s, today, today)
        assertEquals(LocalDate.of(2026, 10, 30), w.start)
        assertEquals(LocalDate.of(2026, 11, 5), w.end)
        assertEquals(7, w.trackedDays)
        assertEquals(35, w.completed)
        assertEquals(7, w.completeDays)
    }

    @Test
    fun `previous friday summary is the week before`() {
        val s = FakeSource().apply { (8..14).forEach { track(today.minusDays(it.toLong()), 2) } }
        val prev = JourneyEngine.fridayPreviousSummary(s, today, today)
        assertEquals(LocalDate.of(2026, 10, 23), prev.start)
        assertEquals(LocalDate.of(2026, 10, 29), prev.end)
        assertEquals(40, prev.consistency)
    }

    @Test
    fun `friday summary stays correct when the app is opened on a later day`() {
        val s = FakeSource().apply { (1..7).forEach { track(today.minusDays(it.toLong()), 5) } }
        val saturday = today.plusDays(1)
        val w = JourneyEngine.fridaySummary(s, JourneyEngine.latestFriday(saturday), saturday)
        assertEquals(35, w.completed)
        assertFalse(w.days.any { it.date == today })
    }
}
