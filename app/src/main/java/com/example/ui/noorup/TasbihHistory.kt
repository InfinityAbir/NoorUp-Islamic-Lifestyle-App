package com.example.ui.noorup

import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDate

/** Tally of one dhikr on one day. Titles are snapshots so history survives a deleted custom dhikr. */
data class DhikrDayEntry(
    val id: String,
    val titleBangla: String,
    val titleEnglish: String,
    val count: Int,
    val rounds: Int
) {
    fun title(isEnglish: Boolean): String =
        if (isEnglish) titleEnglish.ifBlank { titleBangla } else titleBangla.ifBlank { titleEnglish }
}

data class DhikrDay(val date: LocalDate, val entries: List<DhikrDayEntry>) {
    val total: Int get() = entries.sumOf { it.count }
    val rounds: Int get() = entries.sumOf { it.rounds }
    val isEmpty: Boolean get() = entries.isEmpty()
}

/**
 * Per-day zikr log. One small JSON value per day under `tasbih_hist_<yyyy-MM-dd>`, so a tap only
 * rewrites today's entry. The `tasbih_` prefix is already part of the backup whitelist.
 */
object TasbihHistory {
    const val PREFIX = "tasbih_hist_"
    const val RETENTION_DAYS = 400L

    private fun key(date: LocalDate) = "$PREFIX$date"

    fun record(
        prefs: SharedPreferences,
        date: LocalDate,
        dhikr: TasbihDhikr,
        count: Int = 1,
        rounds: Int = 0,
        sync: Boolean = false
    ) {
        synchronized(TasbihCounter.lock) {
        if (count <= 0 && rounds <= 0) return@synchronized
        val k = key(date)
        val isNewDay = !prefs.contains(k)
        val entries = parse(prefs.getString(k, null)).toMutableList()
        val index = entries.indexOfFirst { it.id == dhikr.id }
        val old = entries.getOrNull(index)
        val updated = DhikrDayEntry(
            id = dhikr.id,
            titleBangla = dhikr.titleBangla,
            titleEnglish = dhikr.titleEnglish,
            count = ((old?.count ?: 0) + count).coerceAtLeast(0),
            rounds = ((old?.rounds ?: 0) + rounds).coerceAtLeast(0)
        )
        if (index >= 0) entries[index] = updated else entries.add(updated)

        val editor = prefs.edit().putString(k, serialize(entries))
        if (isNewDay) {
            val cutoff = key(date.minusDays(RETENTION_DAYS))
            prefs.all.keys.filter { it.startsWith(PREFIX) && it < cutoff }.forEach { editor.remove(it) }
        }
        if (sync) editor.commit() else editor.apply()
        }
    }

    fun loadDay(prefs: SharedPreferences, date: LocalDate): DhikrDay =
        DhikrDay(date, parse(prefs.getString(key(date), null)))

    /** [days] consecutive days ending at [endInclusive], newest first. Empty days are included. */
    fun loadRange(prefs: SharedPreferences, endInclusive: LocalDate, days: Int): List<DhikrDay> =
        (0 until days).map { loadDay(prefs, endInclusive.minusDays(it.toLong())) }

    /** Everything still retained: total count and number of days with activity. */
    fun totals(prefs: SharedPreferences): Pair<Int, Int> {
        var total = 0
        var activeDays = 0
        for ((k, v) in prefs.all) {
            if (!k.startsWith(PREFIX) || v !is String) continue
            val sum = parse(v).sumOf { it.count }
            if (sum > 0) { total += sum; activeDays++ }
        }
        return total to activeDays
    }

    internal fun parse(raw: String?): List<DhikrDayEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val obj = JsonParser.parseString(raw).asJsonObject
            obj.entrySet().mapNotNull { (id, element) ->
                runCatching {
                    val e = element.asJsonObject
                    DhikrDayEntry(
                        id = id,
                        titleBangla = e.str("b"),
                        titleEnglish = e.str("e"),
                        count = e.int("n").coerceAtLeast(0),
                        rounds = e.int("r").coerceAtLeast(0)
                    )
                }.getOrNull()
            }.filter { it.count > 0 || it.rounds > 0 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun serialize(entries: List<DhikrDayEntry>): String {
        val root = JsonObject()
        entries.forEach {
            val o = JsonObject()
            o.addProperty("n", it.count)
            o.addProperty("r", it.rounds)
            o.addProperty("b", it.titleBangla)
            o.addProperty("e", it.titleEnglish)
            root.add(it.id, o)
        }
        return root.toString()
    }

    private fun JsonObject.str(name: String): String {
        val el = get(name) ?: return ""
        return if (el.isJsonNull) "" else runCatching { el.asString }.getOrDefault("")
    }

    private fun JsonObject.int(name: String): Int {
        val el = get(name) ?: return 0
        return if (el.isJsonNull) 0 else runCatching { el.asInt }.getOrDefault(0)
    }
}
