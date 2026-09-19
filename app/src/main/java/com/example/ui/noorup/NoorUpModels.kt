package com.example.ui.noorup

data class PrayerTime(
    val nameBangla: String,
    val nameEnglish: String,
    val arabicName: String,
    val startTime: String,
    val endTime: String,
    val isCurrent: Boolean = false,
    val isPassed: Boolean = false,
    val isMakruh: Boolean = false,
    val makruhReasonBn: String = "",
    val makruhReasonEn: String = ""
)

data class Surah(
    val number: Int,
    val nameBangla: String,
    val nameEnglish: String,
    val nameArabic: String,
    val totalVerses: Int,
    val revelationTypeBangla: String,
    val revelationTypeEnglish: String,
    val verses: List<Verse>
)

data class Verse(
    val number: Int,
    val arabicText: String,
    val banglaTranslation: String,
    val englishTranslation: String,
    var isBookmarked: Boolean = false,
    var isMemorized: Boolean = false
)

data class Dua(
    val id: Int,
    val categoryBangla: String,
    val categoryEnglish: String,
    val titleBangla: String,
    val titleEnglish: String,
    val arabicText: String,
    val phoneticBangla: String,
    val phoneticEnglish: String,
    val meaningBangla: String,
    val meaningEnglish: String,
    val reference: String
)

typealias HadithBook = com.example.ui.noorup.hadith.Book
typealias HadithChapter = com.example.ui.noorup.hadith.Chapter
typealias Hadith = com.example.ui.noorup.hadith.Hadith

data class HijriEvent(
    val titleBangla: String,
    val titleEnglish: String,
    val hijriDate: String,
    val gregorianDate: String,
    val descriptionBangla: String,
    val descriptionEnglish: String,
    val daysLeft: Int
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)

data class FamilyMember(
    val name: String,
    val count: Int,
    val relation: String,
    val pairingCode: String = "",
    val isLiveSyncing: Boolean = true,
    val lastSyncTime: String = "",
    val lastZikrPhrase: String = "সুবহানাল্লাহ"
)

data class FamilyLiveEvent(
    val memberName: String,
    val relation: String,
    val pairingCode: String,
    val phrase: String,
    val increment: Int,
    val timeLabel: String
)

data class CityLocation(
    val nameBn: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double,
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

data class PrayerCalc(
    val nameBn: String,
    val nameEn: String,
    val timeStr: String,
    val totalMins: Int
)

data class DailyRecord(
    val dayLabel: String,
    val completedCount: Int,
    val totalCount: Int,
    val scorePercent: Int,
    val isStreakMaintained: Boolean
)

data class MissedDayRecord(
    val dateKey: String,
    val daysAgo: Int,
    val dayLabel: String,
    val missedPrayersBn: List<String>,
    val missedPrayersEn: List<String>,
    val hasLogged: Boolean
)

private val EN_TO_BN_DIGITS = mapOf(
    '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
    '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
)

fun Number.toBanglaDigits(): String {
    return this.toString().map { EN_TO_BN_DIGITS[it] ?: it }.joinToString("")
}

fun String.toBanglaDigits(): String {
    return this.map { EN_TO_BN_DIGITS[it] ?: it }.joinToString("")
}

