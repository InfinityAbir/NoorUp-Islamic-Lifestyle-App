package com.example.ui.noorup

import java.util.Calendar

// --- Extension Helpers for Bangla Digits ---
fun String.toBanglaDigits(): String {
    return this.map { char ->
        when (char) {
            '0' -> '০'
            '1' -> '১'
            '2' -> '২'
            '3' -> '৩'
            '4' -> '৪'
            '5' -> '৫'
            '6' -> '৬'
            '7' -> '৭'
            '8' -> '৮'
            '9' -> '৯'
            else -> char
        }
    }.joinToString("")
}

fun Int.toBanglaDigits(): String = this.toString().toBanglaDigits()
fun Long.toBanglaDigits(): String = this.toString().toBanglaDigits()

// --- Extension Helpers for Arabic-Indic Digits (Ayat-end markers) ---
fun String.toArabicDigits(): String = this.map { char ->
    when (char) {
        '0' -> '٠'
        '1' -> '١'
        '2' -> '٢'
        '3' -> '٣'
        '4' -> '٤'
        '5' -> '٥'
        '6' -> '٦'
        '7' -> '٧'
        '8' -> '٨'
        '9' -> '٩'
        else -> char
    }
}.joinToString("")

fun Int.toArabicDigits(): String = this.toString().toArabicDigits()
fun Long.toArabicDigits(): String = this.toString().toArabicDigits()

data class PrayerTime(
    val nameBn: String,
    val nameEn: String,
    val arabic: String,
    val startTime: String,
    val endTime: String,
    val isActive: Boolean = false,
    val isPassed: Boolean = false,
    val isProhibited: Boolean = false,
    val prohibitedReasonBn: String = "",
    val prohibitedReasonEn: String = ""
)

data class CityLocation(
    val nameBn: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double,
    /**
     * IANA timezone ID used for prayer-time math (e.g. "Asia/Dhaka").
     * Blank = fall back to the device timezone (used for live GPS positions).
     */
    val zoneId: String = "",
    val fajrStart: String,
    val fajrEnd: String,
    val makruhSunriseStart: String,
    val makruhSunriseEnd: String,
    val dhuhrStart: String,
    val dhuhrEnd: String,
    val makruhZawalStart: String,
    val makruhZawalEnd: String,
    val asrStart: String,
    val asrEnd: String,
    val makruhSunsetStart: String,
    val makruhSunsetEnd: String,
    val maghribStart: String,
    val maghribEnd: String,
    val ishaStart: String,
    val ishaEnd: String
)

data class DailyHabitItem(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val category: String,
    val isDone: Boolean = false,
    val icon: String = "🌱"
)

// --- Quran & Verse Models ---
data class Surah(
    val number: Int,
    val nameBangla: String,
    val nameEnglish: String,
    val nameArabic: String = "",
    val totalVerses: Int,
    val revelationTypeBangla: String = "মাক্কী",
    val revelationTypeEnglish: String = "Meccan",
    val verses: List<Verse> = emptyList()
) {
    val nameBn: String get() = nameBangla
    val nameEn: String get() = nameEnglish
    val nameAr: String get() = nameArabic
}

data class Verse(
    val number: Int,
    val arabicText: String,
    val banglaTranslation: String,
    val englishTranslation: String,
    val isBookmarked: Boolean = false,
    val isMemorized: Boolean = false,
    /**
     * Bengali-script pronunciation from the bundled, MIT-licensed source.
     * Null means the source has no entry for this ayah (or the bundle failed
     * validation); the UI must say so explicitly rather than invent text.
     */
    val banglaTransliteration: String? = null
) {
    val arabic: String get() = arabicText
    val bangla: String get() = banglaTranslation
    val english: String get() = englishTranslation
}

typealias SurahInfo = Surah
typealias VerseInfo = Verse

// --- Masnoon Dua Model ---
data class DuaItem(
    val id: String,
    val titleBangla: String,
    val titleEnglish: String,
    val categoryBangla: String,
    val categoryEnglish: String,
    val reference: String,
    val arabicText: String,
    val phoneticBangla: String,
    val phoneticEnglish: String = "",
    val meaningBangla: String,
    val meaningEnglish: String,
    /**
     * Extra searchable terms (synonyms, common misspellings, related needs).
     * Display text is never changed by search; these only widen matching.
     */
    val searchTags: List<String> = emptyList()
)

// --- Habit Analytics Models ---
data class DailyRecord(
    val dateKey: String = "",
    val dayLabel: String,
    val completedCount: Int,
    val totalCount: Int,
    val scorePercent: Int,
    val isStreakMaintained: Boolean
)

data class MissedDayRecord(
    val dateKey: String = "",
    val daysAgo: Int,
    val dayLabel: String,
    val missedPrayersBn: List<String>,
    val missedPrayersEn: List<String>,
    val hasLogged: Boolean
)

// --- Qibla Calculation Model ---
data class QiblaInfo(
    val bearingDegrees: Float,
    val distanceKm: Double,
    val cardinalDirectionEn: String,
    val cardinalDirectionBn: String
)

// --- AI Chat Message Model ---
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timeLabel: String
)

enum class CalculationMethod(
    val titleBn: String,
    val titleEn: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val isIshaFixedMinutes: Boolean = false,
    val isIshaFixedMinutesCount: Int = 0
) {
    KARACHI("ইসলামিক ফাউন্ডেশন / করাচি (১৮° / ১৮°)", "Karachi / Islamic Foundation (18° / 18°)", 18.0, 18.0),
    UMM_AL_QURA("উম্মুল কুরা, মক্কা (১৮.৫° / +৯০ মি.)", "Umm al-Qura, Makkah (18.5° / +90 min)", 18.5, 0.0, true, 90),
    EGYPT("মিশরীয় সাধারণ কর্তৃপক্ষ (১৯.৫° / ১৭.৫°)", "Egyptian General Authority (19.5° / 17.5°)", 19.5, 17.5),
    MWL("মুসলিম ওয়ার্ল্ড লীগ (১৮° / ১৭°)", "Muslim World League (18° / 17°)", 18.0, 17.0),
    ISNA("উত্তর আমেরিকা / ISNA (১৫° / ১৫°)", "North America / ISNA (15° / 15°)", 15.0, 15.0),
    DUBAI("দুবাই ইসলামিক অ্যাফেয়ার্স (১৮.২° / ১৮.২°)", "Dubai Islamic Affairs (18.2° / 18.2°)", 18.2, 18.2),
    KUWAIT("কুয়েত ইসলামিক পদ্ধতি (১৮° / ১৭.৫°)", "Kuwait Islamic Method (18° / 17.5°)", 18.0, 17.5),
    QATAR("কাতার ওয়াকফ ও ইসলাম বিষয়ক (১৮° / +৯০ মি.)", "Qatar Ministry of Awqaf (18° / +90 min)", 18.0, 0.0, true, 90)
}

enum class JuristicMethod(val titleBn: String, val titleEn: String) {
    HANAFI("হানাফী (ছায়া দ্বিগুণ / 2x)", "Hanafi (Shadow 2x)"),
    STANDARD_SHAFI("শাফিয়ী, মালেকী, হাম্বলী (ছায়া একগুণ / 1x)", "Shafi, Maliki, Hanbali (Shadow 1x)")
}

data class CalculatedPrayerTimes(
    val date: Calendar,
    val fajrStart: String,
    val fajrEnd: String,
    val sunrise: String,
    val makruhSunriseStart: String,
    val makruhSunriseEnd: String,
    val dhuhrStart: String,
    val dhuhrEnd: String,
    val makruhZawalStart: String,
    val makruhZawalEnd: String,
    val asrStart: String,
    val asrEnd: String,
    val makruhSunsetStart: String,
    val makruhSunsetEnd: String,
    val maghribStart: String,
    val maghribEnd: String,
    val ishaStart: String,
    val ishaEnd: String,
    val tahajjudStart: String,
    val sehriEnd: String,
    val iftarStart: String,
    val calculationMethod: CalculationMethod,
    val juristicMethod: JuristicMethod
)
