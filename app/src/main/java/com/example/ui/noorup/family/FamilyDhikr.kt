package com.example.ui.noorup.family

import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Family Dhikr works without any server. A goal is fully described by a `noorup://dhikr?...` link;
 * each member counts on their own phone and shares a progress link, which merges into the goal on the
 * receiving phone. Merging keeps the highest count per member, so re-opening a link is harmless.
 */
data class FamilyGoal(
    val id: String,
    val title: String,
    val target: Int,
    val createdBy: String,
    /** member name -> that member's counted total. */
    val contributions: Map<String, Int> = emptyMap(),
    /** Bumped on every title/target edit; the highest revision wins when links are merged. */
    val rev: Int = 0
) {
    val total: Int get() = contributions.values.sum()
    val progress: Float get() = if (target <= 0) 0f else (total / target.toFloat()).coerceIn(0f, 1f)
    val isComplete: Boolean get() = target > 0 && total >= target
}

/** What a scanned/opened link carries. */
data class FamilyLinkPayload(
    val goalId: String,
    val title: String,
    val target: Int,
    val createdBy: String,
    val rev: Int = 0,
    val member: String? = null,
    val count: Int? = null,
    /** Prayers (0..5) that member completed on [dateKey]. */
    val prayers: Int? = null,
    val dateKey: String? = null
)

object FamilyLink {
    const val SCHEME = "noorup"
    const val HOST = "dhikr"

    fun build(goal: FamilyGoal, member: String? = null, count: Int? = null, prayers: Int? = null, dateKey: String? = null): String {
        val b = Uri.Builder().scheme(SCHEME).authority(HOST)
            .appendQueryParameter("id", goal.id)
            .appendQueryParameter("t", goal.title)
            .appendQueryParameter("g", goal.target.toString())
            .appendQueryParameter("by", goal.createdBy)
        if (goal.rev > 0) b.appendQueryParameter("r", goal.rev.toString())
        if (member != null) b.appendQueryParameter("m", member)
        if (count != null) b.appendQueryParameter("c", count.toString())
        if (prayers != null) b.appendQueryParameter("p", prayers.toString())
        if (dateKey != null) b.appendQueryParameter("d", dateKey)
        return b.build().toString()
    }

    fun parse(uri: Uri?): FamilyLinkPayload? {
        // Keyboards and chat apps sometimes capitalise the scheme ("NoorUp://"); URI schemes are case-insensitive.
        if (uri == null || !SCHEME.equals(uri.scheme, ignoreCase = true) || !HOST.equals(uri.host, ignoreCase = true)) return null
        val id = uri.getQueryParameter("id")?.takeIf { it.isNotBlank() && it.length <= 64 } ?: return null
        val title = uri.getQueryParameter("t")?.trim()?.take(60)?.takeIf { it.isNotBlank() } ?: return null
        val target = uri.getQueryParameter("g")?.toIntOrNull()?.coerceIn(1, 100_000_000) ?: return null
        return FamilyLinkPayload(
            goalId = id,
            title = title,
            target = target,
            createdBy = uri.getQueryParameter("by")?.trim()?.take(40).orEmpty(),
            rev = uri.getQueryParameter("r")?.toIntOrNull()?.coerceIn(0, 100_000) ?: 0,
            member = uri.getQueryParameter("m")?.trim()?.take(40)?.takeIf { it.isNotBlank() },
            count = uri.getQueryParameter("c")?.toIntOrNull()?.coerceIn(0, 100_000_000),
            prayers = uri.getQueryParameter("p")?.toIntOrNull()?.coerceIn(0, 5),
            dateKey = uri.getQueryParameter("d")?.takeIf { Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) }
        )
    }

    private val linkPattern = Regex("noorup://dhikr\\?[^\\s\"'<>]+", RegexOption.IGNORE_CASE)

    /**
     * Finds the first family link inside any text. Chat apps don't make custom-scheme links tappable, so people
     * copy the whole shared message; this lets them paste it as it is.
     */
    fun extract(text: String): String? =
        linkPattern.find(text)?.value?.trimEnd('.', ',', ';', ')', '!', ']')

    fun parseText(text: String): FamilyLinkPayload? =
        extract(text)?.let { runCatching { parse(Uri.parse(it)) }.getOrNull() }
}

object FamilyDhikrLogic {
    /** Applies [payload] to [goals]: creates the goal if unknown, merges the member's count (max wins). */
    fun merge(goals: List<FamilyGoal>, payload: FamilyLinkPayload): List<FamilyGoal> {
        val existing = goals.firstOrNull { it.id == payload.goalId }
        var base = existing ?: FamilyGoal(payload.goalId, payload.title, payload.target, payload.createdBy, rev = payload.rev)
        // A newer edit (renamed dhikr or changed target) made on another phone replaces ours.
        if (existing != null && payload.rev > existing.rev) {
            base = base.copy(title = payload.title, target = payload.target, rev = payload.rev)
        }
        val updated = if (payload.member != null && payload.count != null) {
            val prev = base.contributions[payload.member] ?: 0
            base.copy(contributions = base.contributions + (payload.member to maxOf(prev, payload.count)))
        } else base
        return if (existing == null) goals + updated else goals.map { if (it.id == updated.id) updated else it }
    }

    /**
     * Consecutive days, ending [today] (or yesterday if today isn't complete yet), on which every member
     * who checked in prayed all five.
     */
    fun familyStreak(checkIns: Map<String, Map<String, Int>>, today: String, previousDay: (String) -> String): Int {
        fun full(day: String): Boolean {
            val m = checkIns[day] ?: return false
            return m.isNotEmpty() && m.values.all { it >= 5 }
        }
        var day = if (full(today)) today else previousDay(today)
        var n = 0
        while (full(day)) { n++; day = previousDay(day) }
        return n
    }

    fun newId(): String = UUID.randomUUID().toString().take(8)
}

class FamilyStore(private val prefs: SharedPreferences) {
    private val gson = Gson()

    fun myName(): String = prefs.getString("family_my_name", "")?.trim().orEmpty()
    fun setMyName(name: String) = prefs.edit().putString("family_my_name", name.trim().take(40)).apply()

    fun goals(): List<FamilyGoal> = runCatching {
        gson.fromJson<List<FamilyGoal>>(
            prefs.getString("family_goals_json", "[]"), object : TypeToken<List<FamilyGoal>>() {}.type
        ).orEmpty()
    }.getOrDefault(emptyList())

    fun saveGoals(goals: List<FamilyGoal>) = prefs.edit().putString("family_goals_json", gson.toJson(goals)).apply()

    /** dateKey -> (member -> prayers completed). */
    fun checkIns(): Map<String, Map<String, Int>> = runCatching {
        gson.fromJson<Map<String, Map<String, Int>>>(
            prefs.getString("family_checkins_json", "{}"), object : TypeToken<Map<String, Map<String, Int>>>() {}.type
        ).orEmpty()
    }.getOrDefault(emptyMap())

    fun saveCheckIns(map: Map<String, Map<String, Int>>) {
        // Only the last 60 days matter for streaks.
        val trimmed = map.entries.sortedByDescending { it.key }.take(60).associate { it.key to it.value }
        prefs.edit().putString("family_checkins_json", gson.toJson(trimmed)).apply()
    }
}
