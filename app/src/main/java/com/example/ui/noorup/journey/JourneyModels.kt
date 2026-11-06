package com.example.ui.noorup.journey

import java.time.LocalDate

/** The five obligatory prayers, in daily order. [bnKey] is the key used by the existing prayer tracker. */
enum class Prayer(val bnKey: String, val en: String, val bn: String) {
    FAJR("ফজর", "Fajr", "ফজর"),
    DHUHR("যোহর", "Dhuhr", "যোহর"),
    ASR("আসর", "Asr", "আসর"),
    MAGHRIB("মাগরিব", "Maghrib", "মাগরিব"),
    ISHA("এশা", "Isha", "এশা");

    fun label(isEnglish: Boolean) = if (isEnglish) en else bn
}

/**
 * What the tracker knows about one prayer.
 *
 * UPCOMING and PENDING only describe today / future dates. A past prayer is always
 * COMPLETED, CONFIRMED_MISSED (the user explicitly said so) or UNRECORDED. UNRECORDED is
 * never treated as missed anywhere in the Journey.
 */
enum class PrayerStatus { UPCOMING, PENDING, COMPLETED, CONFIRMED_MISSED, UNRECORDED }

/** What a user may set on a prayer. Only these three are ever written. */
enum class RecordChoice { COMPLETED, MISSED, UNRECORDED }

/** Read-only view of the existing prayer tracker. The tracker stays the single source of truth. */
interface PrayerRecordSource {
    fun isCompleted(date: LocalDate, prayer: Prayer): Boolean
    /** True only when the user explicitly marked this prayer as missed. */
    fun isConfirmedMissed(date: LocalDate, prayer: Prayer): Boolean
    /** True when the user opened/edited this day in the tracker (existing `user_has_logged_*`). */
    fun isDayLogged(date: LocalDate): Boolean
    /** Epoch millis the prayer was checked off, or null for records made before timestamps existed. */
    fun completedAt(date: LocalDate, prayer: Prayer): Long? = null
    /** Epoch millis of the last change to this prayer record, if known. */
    fun modifiedAt(date: LocalDate, prayer: Prayer): Long? = null
    /** First date the user ever tracked, if known. Used to bound history scans. */
    fun trackingStart(): LocalDate? = null
}

/** Today's prayer start times so a not-yet-started prayer reads UPCOMING instead of PENDING. */
data class TodayTiming(val nowMinutes: Int, val startMinutes: List<Int?>)

data class DayRecord(
    val date: LocalDate,
    val statuses: List<PrayerStatus>,
    val tracked: Boolean
) {
    val completed: Int get() = statuses.count { it == PrayerStatus.COMPLETED }
    val confirmedMissed: Int get() = statuses.count { it == PrayerStatus.CONFIRMED_MISSED }
    val unrecorded: Int get() = statuses.count { it == PrayerStatus.UNRECORDED }

    /** Daily completion = completed / 5 x 100. Not a religious judgement; it only drives illumination. */
    val percent: Int get() = JourneyEngine.dailyPercent(completed)

    /** Daily illumination 0f..1f. */
    val illumination: Float get() = completed / 5f
}

/** Rolling consistency over a window of completed days. [percent] is null when nothing was tracked. */
data class Rolling(val completed: Int, val trackedDays: Int) {
    val ratio: Double? get() = if (trackedDays > 0) completed.toDouble() / (5.0 * trackedDays) else null
    val percent: Int? get() = ratio?.let { Math.round(it * 100.0).toInt() }
}

enum class Stage(val number: Int, val en: String, val bn: String, val minScore: Int) {
    FIRST_LIGHT(1, "First Light", "প্রথম আলো", 0),
    RISING_CRESCENT(2, "Rising Crescent", "উদীয়মান চাঁদ", 20),
    SHINING_CONSTELLATION(3, "Shining Constellation", "উজ্জ্বল নক্ষত্রপুঞ্জ", 40),
    RADIANT_HORIZON(4, "Radiant Horizon", "দীপ্ত দিগন্ত", 60),
    CELESTIAL_HARMONY(5, "Celestial Harmony", "আসমানি সুর", 75),
    ETERNAL_LIGHT(6, "Eternal Light", "চিরন্তন আলো", 90);

    fun label(isEnglish: Boolean) = if (isEnglish) en else bn

    companion object {
        fun of(number: Int): Stage = entries[(number - 1).coerceIn(0, entries.size - 1)]
    }
}

data class StageResult(
    val stage: Stage,
    /** Stage purely from the score, before eligibility caps. */
    val scoreStage: Stage,
    val score: Double,
    val r30: Rolling,
    val r90: Rolling,
    val previous30: Rolling,
    /** True when too little tracking history held the stage below what the score alone would give. */
    val cappedByHistory: Boolean
)

/** Everything the Stats hero needs, computed once per data revision. */
data class JourneySummary(
    val today: DayRecord,
    val stage: StageResult,
    val rolling30: Rolling
)

/** Per-prayer counts inside a period (tracked days only). */
data class PrayerTally(val completed: Int = 0, val missed: Int = 0, val unrecorded: Int = 0)

/**
 * Aggregate over a run of days. Only tracked days feed the counts; days the user never opened
 * are reported as [untrackedDays] and never as missed.
 */
data class PeriodStats(
    val start: LocalDate,
    val end: LocalDate,
    val days: List<DayRecord>,
    val completed: Int,
    val confirmedMissed: Int,
    val unrecorded: Int,
    val trackedDays: Int,
    val untrackedDays: Int,
    val completeDays: Int,
    val perPrayer: List<PrayerTally>
) {
    val consistency: Int?
        get() = if (trackedDays > 0) Math.round(completed * 100.0 / (5.0 * trackedDays)).toInt() else null

    /** Consistency for one prayer, or the overall figure when [prayer] is null. */
    fun consistencyFor(prayer: Prayer?): Int? {
        if (prayer == null) return consistency
        return if (trackedDays > 0) Math.round(perPrayer[prayer.ordinal].completed * 100.0 / trackedDays).toInt() else null
    }
}

enum class MilestoneId(
    val en: String,
    val bn: String,
    val descEn: String,
    val descBn: String
) {
    FIRST_DAY(
        "Dawn Star", "ভোরের তারা",
        "Your first day with all five prayers recorded as completed.",
        "পাঁচ ওয়াক্তই আদায় হিসেবে রেকর্ড করা আপনার প্রথম দিন।"
    ),
    FIRST_WEEK(
        "Seven Stars", "সপ্ততারা",
        "Seven days in a row that you kept your record.",
        "টানা সাত দিন আপনি রেকর্ড রেখেছেন।"
    ),
    DAYS_30(
        "Crescent Arc", "চন্দ্ররেখা",
        "30 days of keeping your prayer record.",
        "৩০ দিন নামাজের রেকর্ড রাখা।"
    ),
    DAYS_90(
        "Lantern", "ফানুস",
        "90 days of keeping your prayer record.",
        "৯০ দিন নামাজের রেকর্ড রাখা।"
    ),
    DAYS_180(
        "Minaret Crown", "মিনার-মুকুট",
        "180 days of keeping your prayer record.",
        "১৮০ দিন নামাজের রেকর্ড রাখা।"
    ),
    DAYS_365(
        "Eternal Circle", "চিরন্তন বৃত্ত",
        "One year of keeping your prayer record.",
        "এক বছর নামাজের রেকর্ড রাখা।"
    );

    fun label(isEnglish: Boolean) = if (isEnglish) en else bn
    fun description(isEnglish: Boolean) = if (isEnglish) descEn else descBn
}

/** Milestone as shown in the gallery. [unlockedOn] is null while locked. */
data class MilestoneState(val id: MilestoneId, val unlockedOn: LocalDate?, val seen: Boolean)

/** Persisted answers for one Friday reflection. Private to the device. */
data class ReflectionEntry(
    val fridayDate: String = "",
    val periodStart: String = "",
    val periodEnd: String = "",
    val q1: Int? = null,
    val q2: Int? = null,
    val q3: String = "",
    val intention: String = "",
    val snapCompleted: Int = 0,
    val snapMissed: Int = 0,
    val snapUnrecorded: Int = 0,
    val snapCompleteDays: Int = 0,
    val snapTrackedDays: Int = 0,
    val snapPercent: Int? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
