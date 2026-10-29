package com.example.ui.noorup.ramadan

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.noorup.HijriDateCalculator
import com.example.ui.noorup.HijriDateResult
import java.util.Calendar

/** Pure Ramadan rules (no Android types) so they can be unit-tested on the JVM. */
object RamadanLogic {
    const val RAMADAN_MONTH = 9
    val ODD_NIGHTS = listOf(21, 23, 25, 27, 29)

    fun isRamadan(hijriMonth: Int) = hijriMonth == RAMADAN_MONTH

    /**
     * Night number of Ramadan. Each night begins at sunset (Maghrib) of the previous day, so after Maghrib
     * on day 26 it is already the 27th night.
     */
    fun nightNumber(hijriDay: Int, afterMaghrib: Boolean): Int = if (afterMaghrib) hijriDay + 1 else hijriDay

    fun isOddLastTenNight(night: Int) = night in ODD_NIGHTS

    enum class FastStatus { NONE, FASTED, MISSED;
        fun next(): FastStatus = when (this) { NONE -> FASTED; FASTED -> MISSED; MISSED -> NONE }
    }

    /** Consecutive fasted days ending at [uptoDay] (an unmarked [uptoDay] doesn't break the streak). */
    fun streak(statuses: Map<Int, FastStatus>, uptoDay: Int): Int {
        var day = uptoDay
        if ((statuses[day] ?: FastStatus.NONE) == FastStatus.NONE) day--
        var n = 0
        while (day >= 1 && statuses[day] == FastStatus.FASTED) { n++; day-- }
        return n
    }

    enum class Grain(val nameEn: String, val nameBn: String, val kgPerPerson: Double, val defaultPricePerKg: Int) {
        WHEAT("Wheat / flour", "গম / আটা", 1.75, 60),
        BARLEY("Barley", "যব", 3.5, 120),
        DATES("Dates", "খেজুর", 3.5, 600),
        RAISINS("Raisins", "কিসমিস", 3.5, 700)
    }

    /** Fitrah for the household in BDT. */
    fun fitrAmount(members: Int, grain: Grain, pricePerKg: Double): Double =
        members.coerceAtLeast(0) * grain.kgPerPerson * pricePerKg.coerceAtLeast(0.0)

    // --- storage encoding: "day:value,day:value" ---

    fun encodeIntMap(map: Map<Int, Int>): String =
        map.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }

    fun decodeIntMap(raw: String?): Map<Int, Int> =
        raw.orEmpty().split(',').mapNotNull {
            val p = it.split(':')
            val k = p.getOrNull(0)?.toIntOrNull()
            val v = p.getOrNull(1)?.toIntOrNull()
            if (k != null && v != null) k to v else null
        }.toMap()

    /** Hijri date from stored prefs only (usable from receivers/widgets with no ViewModel). */
    fun hijriNow(context: Context, cal: Calendar = Calendar.getInstance()): HijriDateResult {
        val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getString("selected_city_lat_dbl", null)?.toDoubleOrNull()
            ?: prefs.getFloat("selected_city_lat", 23.8759f).toDouble()
        val lng = prefs.getString("selected_city_lng_dbl", null)?.toDoubleOrNull()
            ?: prefs.getFloat("selected_city_lng", 90.3795f).toDouble()
        val name = prefs.getString("selected_city_name_en", "Dhaka").orEmpty()
        val isBd = HijriDateCalculator.isBangladeshLocation(lat, lng, name)
        return HijriDateCalculator.calculateHijriDate(cal, prefs.getInt("hijri_day_offset", 0), isBd)
    }
}

enum class RamadanModePref { AUTO, ON, OFF }

/** Prefs-backed Ramadan trackers. Keys are plain so the Drive backup can carry them. */
class RamadanStore(private val prefs: SharedPreferences) {

    fun modePref(): RamadanModePref =
        RamadanModePref.entries.firstOrNull { it.name == prefs.getString("ramadan_mode", null) } ?: RamadanModePref.AUTO

    fun setModePref(m: RamadanModePref) = prefs.edit().putString("ramadan_mode", m.name).apply()

    fun fasting(year: Int): Map<Int, RamadanLogic.FastStatus> =
        RamadanLogic.decodeIntMap(prefs.getString("ramadan_fast_$year", null))
            .mapValues { RamadanLogic.FastStatus.entries.getOrElse(it.value) { RamadanLogic.FastStatus.NONE } }

    fun setFasting(year: Int, day: Int, status: RamadanLogic.FastStatus) {
        val m = fasting(year).toMutableMap()
        if (status == RamadanLogic.FastStatus.NONE) m.remove(day) else m[day] = status
        prefs.edit().putString("ramadan_fast_$year", RamadanLogic.encodeIntMap(m.mapValues { it.value.ordinal })).apply()
    }

    /** Fasts owed across all years minus fasts already made up. */
    fun qadaOwed(): Int {
        val missed = prefs.all.entries
            .filter { it.key.startsWith("ramadan_fast_") }
            .sumOf { e -> RamadanLogic.decodeIntMap(e.value as? String).values.count { it == RamadanLogic.FastStatus.MISSED.ordinal } }
        return (missed - qadaMadeUp()).coerceAtLeast(0)
    }

    fun qadaMadeUp(): Int = prefs.getInt("ramadan_qada_done", 0)

    fun addQadaMadeUp(delta: Int) {
        prefs.edit().putInt("ramadan_qada_done", (qadaMadeUp() + delta).coerceAtLeast(0)).apply()
    }

    fun taraweeh(year: Int): Map<Int, Int> = RamadanLogic.decodeIntMap(prefs.getString("ramadan_tar_$year", null))

    fun setTaraweeh(year: Int, night: Int, rakat: Int) {
        val m = taraweeh(year).toMutableMap()
        if (rakat <= 0) m.remove(night) else m[night] = rakat.coerceAtMost(40)
        prefs.edit().putString("ramadan_tar_$year", RamadanLogic.encodeIntMap(m)).apply()
    }

    /** Bitmask of completed checklist items for [night] (21/23/25/27/29). */
    fun qadrChecklist(year: Int, night: Int): Int = prefs.getInt("ramadan_lq_${year}_$night", 0)

    fun setQadrChecklist(year: Int, night: Int, mask: Int) {
        prefs.edit().putInt("ramadan_lq_${year}_$night", mask).apply()
    }

    fun fitrMembers(): Int = prefs.getInt("ramadan_fitr_members", 4)
    fun fitrGrain(): RamadanLogic.Grain =
        RamadanLogic.Grain.entries.firstOrNull { it.name == prefs.getString("ramadan_fitr_grain", null) } ?: RamadanLogic.Grain.WHEAT

    fun fitrPrice(grain: RamadanLogic.Grain): Int =
        prefs.getInt("ramadan_fitr_price_${grain.name}", grain.defaultPricePerKg)

    fun saveFitr(members: Int, grain: RamadanLogic.Grain, price: Int) {
        prefs.edit()
            .putInt("ramadan_fitr_members", members)
            .putString("ramadan_fitr_grain", grain.name)
            .putInt("ramadan_fitr_price_${grain.name}", price)
            .apply()
    }
}

/** Ramadan wording for the existing Maghrib alarm, so no second alarm pipeline is needed. */
object RamadanNotifications {
    /** (title, message) for the Maghrib alert during Ramadan, or null outside it. */
    fun maghribOverride(
        context: Context,
        isEnglish: Boolean,
        cal: java.util.Calendar = java.util.Calendar.getInstance()
    ): Pair<String, String>? {
        val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getString("ramadan_mode", RamadanModePref.AUTO.name)
        if (mode == RamadanModePref.OFF.name) return null
        val hijri = RamadanLogic.hijriNow(context, cal)
        if (!RamadanLogic.isRamadan(hijri.month)) return null
        val night = RamadanLogic.nightNumber(hijri.day, afterMaghrib = true)
        val base = if (isEnglish)
            "🌙 Iftar time" to "It is time to break your fast. Dua: \"Dhahaba az-zama'u wabtallatil-'uruqu wa thabatal-ajru in sha Allah.\" (Abu Dawud 2357)"
        else
            "🌙 ইফতারের সময়" to "রোজা ভাঙার সময় হয়েছে। দুআ: \"যাহাবায যামাউ ওয়াবতাল্লাতিল উরূকু ওয়া সাবাতাল আজরু ইনশাআল্লাহ।\" (আবু দাউদ ২৩৫৭)"
        if (!RamadanLogic.isOddLastTenNight(night)) return base
        val extra = if (isEnglish)
            "\n✨ Tonight is the ${night}th night — seek Laylatul Qadr."
        else
            "\n✨ আজ ${night.toString().map { c -> "০১২৩৪৫৬৭৮৯"[c - '0'] }.joinToString("")} তম রাত — লাইলাতুল কদর তালাশ করুন।"
        return base.first to (base.second + extra)
    }
}
