package com.example.ui.noorup.journey

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure, deterministic Journey calculations (calc version [CALC_VERSION]).
 *
 * No Android, no clock: callers pass `today`. Nothing here writes data.
 *
 * Rules (see docs/CRESCENT_JOURNEY_M1_AUDIT.md, D2/D3):
 *  - A *tracked day* is a day the user logged, or that has any completed / confirmed-missed prayer.
 *  - Rolling consistency = completed / (5 x tracked days) over fully elapsed days only.
 *    Today and future dates are never counted. Untracked days are excluded, not counted as missed.
 *  - Unrecorded prayers on a tracked day lower the percentage but are never labelled missed.
 */
object JourneyEngine {
    const val CALC_VERSION = 1
    const val PRAYERS_PER_DAY = 5

    private const val W30 = 2.0 / 3.0
    private const val W90 = 1.0 / 3.0
    private const val HYSTERESIS = 3.0
    private const val TREND_WEIGHT = 0.10
    private const val TREND_LIMIT = 3.0

    /** Upper bound on how far back a history scan goes when the tracking start date is unknown. */
    private const val MAX_SCAN_DAYS = 800L

    fun dailyPercent(completed: Int): Int = completed.coerceIn(0, PRAYERS_PER_DAY) * 100 / PRAYERS_PER_DAY

    fun dayRecord(
        source: PrayerRecordSource,
        date: LocalDate,
        today: LocalDate,
        timing: TodayTiming? = null
    ): DayRecord {
        val statuses = Prayer.entries.mapIndexed { index, prayer ->
            when {
                date.isAfter(today) -> PrayerStatus.UPCOMING
                source.isCompleted(date, prayer) -> PrayerStatus.COMPLETED
                source.isConfirmedMissed(date, prayer) -> PrayerStatus.CONFIRMED_MISSED
                date == today -> {
                    val start = timing?.startMinutes?.getOrNull(index)
                    if (timing != null && start != null && start > timing.nowMinutes) PrayerStatus.UPCOMING
                    else PrayerStatus.PENDING
                }
                else -> PrayerStatus.UNRECORDED
            }
        }
        val tracked = !date.isAfter(today) && (
            source.isDayLogged(date) ||
                statuses.any { it == PrayerStatus.COMPLETED || it == PrayerStatus.CONFIRMED_MISSED }
            )
        return DayRecord(date, statuses, tracked)
    }

    /**
     * Rolling consistency over [days] calendar days ending at [endInclusive].
     * The window is clamped so it never includes today or a future date.
     */
    fun rolling(source: PrayerRecordSource, endInclusive: LocalDate, days: Int, today: LocalDate): Rolling {
        val lastCounted = if (endInclusive.isBefore(today)) endInclusive else today.minusDays(1)
        var completed = 0
        var tracked = 0
        for (offset in 0 until days) {
            val record = dayRecord(source, lastCounted.minusDays(offset.toLong()), today)
            if (record.tracked) {
                tracked++
                completed += record.completed
            }
        }
        return Rolling(completed, tracked)
    }

    /** Highest stage the amount of tracking history allows. Short histories cannot jump to the top. */
    fun historyCap(trackedDays: Int): Stage = when {
        trackedDays < 7 -> Stage.FIRST_LIGHT
        trackedDays < 14 -> Stage.RISING_CRESCENT
        trackedDays < 30 -> Stage.RADIANT_HORIZON
        trackedDays < 60 -> Stage.CELESTIAL_HARMONY
        else -> Stage.ETERNAL_LIGHT
    }

    fun stageForScore(score: Double): Stage =
        Stage.entries.last { score >= it.minScore }

    /**
     * score = 2/3 x r30 + 1/3 x r90 + trend, in 0..100 (the 2:1 weighting from the plan, normalised so
     * the weights sum to 1 and a perfect record scores exactly 100), where trend is the 30-day change
     * against the previous 30 days scaled by 0.10 and limited to +/-3 points.
     * With no 30-day data the score falls back to the 90-day figure alone.
     *
     * [previousStage] enables hysteresis: the stage only steps down once the score is more than
     * 3 points under that stage's threshold, so one rough day cannot flicker the display.
     */
    fun computeStage(
        source: PrayerRecordSource,
        today: LocalDate,
        previousStage: Stage? = null
    ): StageResult {
        val yesterday = today.minusDays(1)
        val r30 = rolling(source, yesterday, 30, today)
        val r90 = rolling(source, yesterday, 90, today)
        val prev30 = rolling(source, yesterday.minusDays(30), 30, today)

        val a = r30.ratio?.times(100.0)
        val b = r90.ratio?.times(100.0)
        val p = prev30.ratio?.times(100.0)
        val trend = if (a != null && p != null) ((a - p) * TREND_WEIGHT).coerceIn(-TREND_LIMIT, TREND_LIMIT) else 0.0
        val score = when {
            a != null && b != null -> W30 * a + W90 * b + trend
            b != null -> b
            else -> 0.0
        }.coerceIn(0.0, 100.0)

        val scoreStage = stageForScore(score)
        var shown = scoreStage
        if (previousStage != null && scoreStage.number < previousStage.number &&
            score >= previousStage.minScore - HYSTERESIS
        ) {
            shown = previousStage
        }
        val cap = historyCap(r90.trackedDays)
        val capped = shown.number > cap.number
        if (capped) shown = cap
        return StageResult(shown, scoreStage, score, r30, r90, prev30, capped)
    }

    fun summary(
        source: PrayerRecordSource,
        today: LocalDate,
        previousStage: Stage? = null,
        timing: TodayTiming? = null
    ): JourneySummary {
        val stage = computeStage(source, today, previousStage)
        return JourneySummary(dayRecord(source, today, today, timing), stage, stage.r30)
    }

    // ---------------------------------------------------------------- period analytics

    /**
     * Aggregates [start]..[end]. Today is excluded unless [includeToday]; future dates never count.
     * Counts come from tracked days only.
     */
    fun period(
        source: PrayerRecordSource,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
        includeToday: Boolean = false
    ): PeriodStats {
        val limit = if (includeToday) today else today.minusDays(1)
        val last = if (end.isAfter(limit)) limit else end
        val days = ArrayList<DayRecord>()
        var completed = 0
        var missed = 0
        var unrecorded = 0
        var tracked = 0
        var untracked = 0
        var complete = 0
        val tallies = Array(5) { IntArray(3) }
        var d = start
        while (!d.isAfter(last)) {
            val record = dayRecord(source, d, today)
            days += record
            if (record.tracked) {
                tracked++
                completed += record.completed
                missed += record.confirmedMissed
                unrecorded += record.unrecorded
                if (record.completed == PRAYERS_PER_DAY) complete++
                record.statuses.forEachIndexed { i, s ->
                    when (s) {
                        PrayerStatus.COMPLETED -> tallies[i][0]++
                        PrayerStatus.CONFIRMED_MISSED -> tallies[i][1]++
                        PrayerStatus.UNRECORDED -> tallies[i][2]++
                        else -> Unit
                    }
                }
            } else {
                untracked++
            }
            d = d.plusDays(1)
        }
        return PeriodStats(
            start, end, days, completed, missed, unrecorded, tracked, untracked, complete,
            tallies.map { PrayerTally(it[0], it[1], it[2]) }
        )
    }

    /** The seven completed days ending [endInclusive] (inclusive). */
    fun week(source: PrayerRecordSource, endInclusive: LocalDate, today: LocalDate): PeriodStats =
        period(source, endInclusive.minusDays(6), endInclusive, today)

    fun month(source: PrayerRecordSource, month: YearMonth, today: LocalDate): PeriodStats =
        period(source, month.atDay(1), month.atEndOfMonth(), today)

    // ---------------------------------------------------------------- Friday window

    /** The latest Friday on or before [today]. */
    fun latestFriday(today: LocalDate): LocalDate {
        val back = (today.dayOfWeek.value - DayOfWeek.FRIDAY.value + 7) % 7
        return today.minusDays(back.toLong())
    }

    /** Friday reflection covers the seven completed days Friday..Thursday before [friday]. */
    fun fridayWindowStart(friday: LocalDate): LocalDate = friday.minusDays(7)
    fun fridayWindowEnd(friday: LocalDate): LocalDate = friday.minusDays(1)

    fun fridaySummary(source: PrayerRecordSource, friday: LocalDate, today: LocalDate): PeriodStats =
        period(source, fridayWindowStart(friday), fridayWindowEnd(friday), today)

    fun fridayPreviousSummary(source: PrayerRecordSource, friday: LocalDate, today: LocalDate): PeriodStats =
        period(source, fridayWindowStart(friday).minusDays(7), fridayWindowEnd(friday).minusDays(7), today)

    // ---------------------------------------------------------------- milestones

    /**
     * Dates each milestone was first reached. Participation based: tracked days, not perfection,
     * and nothing here says anything about acceptance of worship. A milestone that is reached
     * stays reached: callers persist the dates and never revoke them.
     */
    fun milestoneDates(source: PrayerRecordSource, today: LocalDate): Map<MilestoneId, LocalDate> {
        val earliest = today.minusDays(MAX_SCAN_DAYS)
        val scanFrom = source.trackingStart()?.let { if (it.isBefore(earliest)) earliest else it } ?: earliest
        val result = LinkedHashMap<MilestoneId, LocalDate>()
        val thresholds = listOf(
            30 to MilestoneId.DAYS_30,
            90 to MilestoneId.DAYS_90,
            180 to MilestoneId.DAYS_180,
            365 to MilestoneId.DAYS_365
        )
        var trackedCount = 0
        var run = 0
        var d = scanFrom
        while (!d.isAfter(today)) {
            val record = dayRecord(source, d, today)
            if (record.tracked) {
                trackedCount++
                run++
                if (record.completed == PRAYERS_PER_DAY) result.putIfAbsent(MilestoneId.FIRST_DAY, d)
                if (run >= 7) result.putIfAbsent(MilestoneId.FIRST_WEEK, d)
                thresholds.forEach { (n, id) -> if (trackedCount >= n) result.putIfAbsent(id, d) }
            } else {
                run = 0
            }
            d = d.plusDays(1)
        }
        return result
    }

    // ---------------------------------------------------------------- clock parsing

    /** "05:17 PM" -> minutes since midnight, or null when unparseable. */
    fun parseClockMinutes(text: String): Int? {
        val m = Regex("""^\s*(\d{1,2}):(\d{2})\s*(AM|PM)?\s*$""", RegexOption.IGNORE_CASE).find(text) ?: return null
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        val ampm = m.groupValues[3].uppercase()
        if (min > 59) return null
        if (ampm.isNotEmpty()) {
            if (h !in 1..12) return null
            h %= 12
            if (ampm == "PM") h += 12
        } else if (h > 23) return null
        return h * 60 + min
    }
}
