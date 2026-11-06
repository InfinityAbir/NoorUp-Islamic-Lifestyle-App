package com.example.ui.noorup.journey

import android.content.SharedPreferences
import com.google.gson.Gson
import java.time.LocalDate

/**
 * Personal Journey settings, all in `noorup_prefs` (so they follow the existing backup).
 * Defaults are conservative: Friday reminders are opt-in, motion follows the system setting.
 */
class JourneyPrefs(private val prefs: SharedPreferences) {

    var animationsEnabled: Boolean
        get() = prefs.getBoolean(K_ANIM, true)
        set(v) { prefs.edit().putBoolean(K_ANIM, v).apply() }

    /** In-app reduced-motion switch, on top of the Android "remove animations" setting. */
    var reducedMotion: Boolean
        get() = prefs.getBoolean(K_REDUCED, false)
        set(v) { prefs.edit().putBoolean(K_REDUCED, v).apply() }

    var fridayReminderEnabled: Boolean
        get() = prefs.getBoolean(K_FRIDAY_ON, false)
        set(v) { prefs.edit().putBoolean(K_FRIDAY_ON, v).apply() }

    /** Local reminder time as minutes after midnight. Default 20:00. */
    var fridayMinutes: Int
        get() = prefs.getInt(K_FRIDAY_MIN, 20 * 60).coerceIn(0, 1439)
        set(v) { prefs.edit().putInt(K_FRIDAY_MIN, v.coerceIn(0, 1439)).apply() }

    /** Skip the Friday reminder when this week's reflection is already saved. */
    var skipIfReflectionDone: Boolean
        get() = prefs.getBoolean(K_FRIDAY_SKIP, true)
        set(v) { prefs.edit().putBoolean(K_FRIDAY_SKIP, v).apply() }

    var intentionReminderEnabled: Boolean
        get() = prefs.getBoolean(K_INTENTION_ON, false)
        set(v) { prefs.edit().putBoolean(K_INTENTION_ON, v).apply() }

    /** Evening review only posts while a prayer is still not completed today. */
    var eveningOnlyIfPending: Boolean
        get() = prefs.getBoolean(K_EVENING_PENDING, true)
        set(v) { prefs.edit().putBoolean(K_EVENING_PENDING, v).apply() }

    /** Final decision for decorative animation: user setting AND not reduced AND system allows it. */
    fun motionAllowed(systemReduced: Boolean): Boolean = animationsEnabled && !reducedMotion && !systemReduced

    companion object {
        /**
         * True when the evening review should stay quiet: all five prayers are completed on [dateKey]
         * (yyyy-MM-dd) and the user keeps the gentle default.
         */
        fun shouldSkipEvening(prefs: SharedPreferences, dateKey: String): Boolean =
            prefs.getBoolean(K_EVENING_PENDING, true) &&
                Prayer.entries.all { prefs.getBoolean("prayer_habit_${dateKey}_${it.bnKey}", false) }

        const val K_ANIM = "journey_anim_enabled"
        const val K_REDUCED = "journey_reduced_motion"
        const val K_FRIDAY_ON = "friday_reminder_enabled"
        const val K_FRIDAY_MIN = "friday_reminder_minutes"
        const val K_FRIDAY_SKIP = "friday_reminder_skip_if_done"
        const val K_INTENTION_ON = "intention_reminder_enabled"
        const val K_EVENING_PENDING = "journey_evening_only_if_pending"
    }
}

/** Everything the Journey screens show about long-term progress. */
data class JourneyData(
    val summary: JourneySummary,
    /** Highest stage ever shown: never lowered, so a hard month cannot erase it. */
    val maxStage: Stage,
    val milestones: List<MilestoneState>,
    /** Milestones reached but not yet written down; [JourneyRepository.persist] stores them once. */
    val newDates: Map<MilestoneId, LocalDate>
)

/**
 * Reads the tracker (read-only), derives stage/milestones, and persists only the small
 * "never take away" facts: highest stage, shown stage (for hysteresis) and milestone dates.
 */
class JourneyRepository(private val prefs: SharedPreferences) {

    val source: PrefsPrayerRecordSource = PrefsPrayerRecordSource(prefs)

    private fun storedStage(key: String): Stage? =
        prefs.getInt(key, 0).takeIf { it in 1..6 }?.let { Stage.of(it) }

    fun snapshot(today: LocalDate, timing: TodayTiming? = null): JourneyData {
        val summary = JourneyEngine.summary(source, today, storedStage(K_STAGE_SHOWN), timing)
        val maxStage = listOfNotNull(storedStage(K_STAGE_MAX), summary.stage.stage).maxBy { it.number }
        val reached = JourneyEngine.milestoneDates(source, today)
        val newDates = LinkedHashMap<MilestoneId, LocalDate>()
        val states = MilestoneId.entries.map { id ->
            val stored = prefs.getString(K_MILESTONE + id.name, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val date = stored ?: reached[id]
            if (stored == null && date != null) newDates[id] = date
            MilestoneState(id, date, seen = stored != null && prefs.getBoolean(K_SEEN + id.name, false))
        }
        return JourneyData(summary, maxStage, states, newDates)
    }

    /** Idempotent: writing the same snapshot twice changes nothing. Never lowers anything. */
    fun persist(data: JourneyData) {
        val editor = prefs.edit()
        editor.putInt(K_STAGE_SHOWN, data.summary.stage.stage.number)
        editor.putInt(K_STAGE_MAX, data.maxStage.number)
        editor.putInt(K_CALC_VERSION, JourneyEngine.CALC_VERSION)
        data.newDates.forEach { (id, date) -> editor.putString(K_MILESTONE + id.name, date.toString()) }
        editor.apply()
    }

    fun markSeen(id: MilestoneId) {
        prefs.edit().putBoolean(K_SEEN + id.name, true).apply()
    }

    companion object {
        const val K_STAGE_SHOWN = "journey_stage_shown"
        const val K_STAGE_MAX = "journey_stage_max"
        const val K_CALC_VERSION = "journey_calc_version"
        const val K_MILESTONE = "journey_milestone_"
        const val K_SEEN = "journey_milestone_seen_"
    }
}

/** Private Friday reflection entries: one JSON string per Friday, keyed by date, so a Friday can never duplicate. */
class ReflectionStore(private val prefs: SharedPreferences, private val gson: Gson = Gson()) {

    private fun key(friday: LocalDate) = PREFIX + friday

    fun get(friday: LocalDate): ReflectionEntry? = read(prefs.getString(key(friday), null))

    fun hasEntry(friday: LocalDate): Boolean = prefs.contains(key(friday))

    private fun read(json: String?): ReflectionEntry? {
        if (json.isNullOrBlank()) return null
        return runCatching { gson.fromJson(json, ReflectionEntry::class.java) }.getOrNull()
            ?.takeIf { it.fridayDate.isNotBlank() }
    }

    /** Newest Friday first. */
    fun all(): List<ReflectionEntry> =
        prefs.all.entries
            .filter { it.key.startsWith(PREFIX) }
            .mapNotNull { read(it.value as? String) }
            .sortedByDescending { it.fridayDate }

    /** Upsert by Friday date. Keeps the original creation time on edits. */
    fun save(entry: ReflectionEntry, nowMillis: Long): ReflectionEntry {
        val date = LocalDate.parse(entry.fridayDate)
        val existing = get(date)
        val saved = entry.copy(
            q3 = entry.q3.trim().take(MAX_TEXT),
            intention = entry.intention.trim().take(MAX_INTENTION),
            createdAt = existing?.createdAt ?: nowMillis,
            updatedAt = nowMillis
        )
        prefs.edit().putString(key(date), gson.toJson(saved)).apply()
        return saved
    }

    fun delete(friday: LocalDate) {
        prefs.edit().remove(key(friday)).apply()
    }

    /** Removes only the intention; the rest of the reflection stays. */
    fun clearIntention(friday: LocalDate, nowMillis: Long) {
        val e = get(friday) ?: return
        prefs.edit().putString(key(friday), gson.toJson(e.copy(intention = "", updatedAt = nowMillis))).apply()
    }

    /**
     * The intention for the week that started on a Friday within the last seven days, if any.
     * Never labelled achieved or failed.
     */
    fun activeIntention(today: LocalDate): ReflectionEntry? =
        all().firstOrNull { e ->
            e.intention.isNotBlank() && runCatching {
                val f = LocalDate.parse(e.fridayDate)
                !today.isBefore(f) && !today.isAfter(f.plusDays(6))
            }.getOrDefault(false)
        }

    companion object {
        const val PREFIX = "reflection_"
        const val MAX_TEXT = 600
        const val MAX_INTENTION = 160
    }
}
