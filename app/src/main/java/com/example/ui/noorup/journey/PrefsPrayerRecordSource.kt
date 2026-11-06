package com.example.ui.noorup.journey

import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Read-through adapter over the existing tracker keys in `noorup_prefs`.
 *
 * Legacy mapping (no historical rewrite):
 *  - `prayer_habit_<date>_<bn> == true`  -> completed
 *  - `prayer_state_<date>_<En> == "missed"` -> confirmed missed (written only on explicit user action)
 *  - anything else on a past day -> unrecorded (never "missed")
 *
 * Additive keys written by the tracker hooks: `prayer_ts_<date>_<En>` (completion time) and
 * `prayer_mod_<date>_<En>` (last change time). Older records simply have neither.
 */
class PrefsPrayerRecordSource(private val prefs: SharedPreferences) : PrayerRecordSource {

    override fun isCompleted(date: LocalDate, prayer: Prayer): Boolean =
        prefs.getBoolean("prayer_habit_${date}_${prayer.bnKey}", false)

    override fun isConfirmedMissed(date: LocalDate, prayer: Prayer): Boolean =
        prefs.getString("prayer_state_${date}_${prayer.en}", null) == STATE_MISSED &&
            !isCompleted(date, prayer)

    override fun isDayLogged(date: LocalDate): Boolean =
        prefs.getBoolean("user_has_logged_$date", false)

    override fun completedAt(date: LocalDate, prayer: Prayer): Long? =
        prefs.getLong("prayer_ts_${date}_${prayer.en}", 0L).takeIf { it > 0L }

    override fun modifiedAt(date: LocalDate, prayer: Prayer): Long? =
        prefs.getLong("prayer_mod_${date}_${prayer.en}", 0L).takeIf { it > 0L }

    override fun trackingStart(): LocalDate? =
        prefs.getString("prayer_tracking_start_date", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    companion object {
        const val STATE_MISSED = "missed"
    }
}
