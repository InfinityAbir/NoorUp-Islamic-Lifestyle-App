package com.example.ui.noorup

import android.os.Build
import java.util.Calendar

data class HijriDateResult(
    val day: Int,
    val month: Int, // 1 to 12
    val year: Int,
    val dayStrBn: String,
    val monthNameBn: String,
    val yearStrBn: String,
    val fullDateBn: String,
    val dayStrEn: String,
    val monthNameEn: String,
    val yearStrEn: String,
    val fullDateEn: String,
    val dayOfWeekBn: String = "",
    val dayOfWeekEn: String = "",
    val formattedDisplayBn: String = "",
    val formattedDisplayEn: String = "",
    val isBangladeshStandard: Boolean = false,
    val standardLabelBn: String = "আন্তর্জাতিক / উম্মুল কুরা মান",
    val standardLabelEn: String = "Umm al-Qura Standard"
)

data class GregorianDateResult(
    val day: Int,
    val month: Int, // 1 to 12
    val year: Int,
    val dayStrBn: String,
    val monthNameBn: String,
    val yearStrBn: String,
    val fullDateBn: String,
    val dayStrEn: String,
    val monthNameEn: String,
    val yearStrEn: String,
    val fullDateEn: String,
    val dayOfWeekBn: String = "",
    val dayOfWeekEn: String = ""
)

data class MoonPhaseInfo(
    val phaseNameBn: String,
    val phaseNameEn: String,
    val illuminationPercent: Int,
    val moonEmoji: String,
    val moonAgeDays: Double
)

data class HijriMonthDay(
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val gregorianDay: Int,
    val gregorianMonth: Int,
    val gregorianYear: Int,
    val dayOfWeek: Int, // Calendar.SUNDAY to Calendar.SATURDAY
    val isToday: Boolean,
    val isAyyamAlBeed: Boolean, // 13, 14, 15
    val isFriday: Boolean,
    val specialEventBn: String? = null,
    val specialEventEn: String? = null
)

data class DynamicIslamicMilestone(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val hijriDateStrBn: String,
    val hijriDateStrEn: String,
    val targetMonth: Int,
    val targetDay: Int,
    val descriptionBn: String,
    val descriptionEn: String,
    val daysRemaining: Int,
    val isPassedThisYear: Boolean
)

object HijriDateCalculator {

    val hijriMonthsBn = listOf(
        "মুহাররম",
        "সফর",
        "রবিউল আউয়াল",
        "রবিউস সানি",
        "জমাদিউল আউয়াল",
        "জমাদিউস সানি",
        "রজব",
        "শাবান",
        "রমজান",
        "শাওয়াল",
        "জিলকদ",
        "জিলহজ্জ"
    )

    val hijriMonthsEn = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Awwal",
        "Jumada al-Thani",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    val gregorianMonthsBn = listOf(
        "জানুয়ারি",
        "ফেব্রুয়ারি",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টেম্বর",
        "অক্টোবর",
        "নভেম্বর",
        "ডিসেম্বর"
    )

    val gregorianMonthsEn = listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    )

    val daysOfWeekBn = listOf(
        "রবিবার",
        "সোমবার",
        "মঙ্গলবার",
        "বুধবার",
        "বৃহস্পতিবার",
        "শুক্রবার",
        "শনিবার"
    )

    val daysOfWeekEn = listOf(
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
    )

    fun toBanglaDigits(numberStr: String): String {
        val enToBn = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return numberStr.map { enToBn[it] ?: it }.joinToString("")
    }

    /**
     * Determines whether the given coordinates or place name correspond to Bangladesh.
     */
    fun isBangladeshLocation(latitude: Double, longitude: Double, locationName: String = ""): Boolean {
        val inCoordinates = (latitude in 20.0..27.0) && (longitude in 88.0..93.0)
        val lowerName = locationName.lowercase()
        val nameMatch = lowerName.contains("bangladesh") || lowerName.contains("বাংলাদেশ") ||
                lowerName.contains("dhaka") || lowerName.contains("ঢাকা") ||
                lowerName.contains("chittagong") || lowerName.contains("চট্টগ্রাম") ||
                lowerName.contains("chattogram") ||
                lowerName.contains("sylhet") || lowerName.contains("সিলেট") ||
                lowerName.contains("rajshahi") || lowerName.contains("রাজশাহী") ||
                lowerName.contains("khulna") || lowerName.contains("খুলনা") ||
                lowerName.contains("barisal") || lowerName.contains("বরিশাল") ||
                lowerName.contains("barishal") ||
                lowerName.contains("rangpur") || lowerName.contains("রংপুর") ||
                lowerName.contains("mymensingh") || lowerName.contains("ময়মনসিংহ") ||
                lowerName.contains("comilla") || lowerName.contains("cumilla") || lowerName.contains("কুমিল্লা") ||
                lowerName.contains("gazipur") || lowerName.contains("গাজীপুর") ||
                lowerName.contains("narayanganj") || lowerName.contains("নারায়ণগঞ্জ") ||
                lowerName.contains("uttara") || lowerName.contains("উত্তরা") ||
                lowerName.contains("mirpur") || lowerName.contains("মিরপুর") ||
                lowerName.contains("dhanmondi") || lowerName.contains("ধানমন্ডি") ||
                lowerName.contains("gulshan") || lowerName.contains("গুলশান") ||
                lowerName.contains("banani") || lowerName.contains("বনানী") ||
                lowerName.contains("bd")
        return inCoordinates || nameMatch
    }

    /**
     * Calculates the Hijri date for the given Gregorian Calendar instance.
     * Supports manual day offset and location awareness for Bangladesh Islamic Foundation calendar standard.
     */
    fun calculateHijriDate(cal: Calendar, dayOffset: Int = 0, isBangladesh: Boolean = false): HijriDateResult {
        val regionalOffset = if (isBangladesh) -1 else 0
        val totalOffset = regionalOffset + dayOffset

        val adjustedCal = (cal.clone() as Calendar).apply {
            if (totalOffset != 0) {
                add(Calendar.DAY_OF_MONTH, totalOffset)
            }
        }

        var hDay = 1
        var hMonth = 1
        var hYear = 1448

        var calculated = false

        // 1. Try Java 8 / Android 8.0+ HijrahDate (Umm al-Qura calendar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val localDate = java.time.LocalDate.of(
                    adjustedCal.get(Calendar.YEAR),
                    adjustedCal.get(Calendar.MONTH) + 1,
                    adjustedCal.get(Calendar.DAY_OF_MONTH)
                )
                val hijrah = java.time.chrono.HijrahDate.from(localDate)
                hYear = hijrah.get(java.time.temporal.ChronoField.YEAR)
                hMonth = hijrah.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                hDay = hijrah.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
                calculated = true
            } catch (_: Throwable) {
                calculated = false
            }
        }

        // 2. Try android.icu.util.IslamicCalendar (API 24+)
        if (!calculated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val icuCal = android.icu.util.IslamicCalendar()
                icuCal.time = adjustedCal.time
                hDay = icuCal.get(android.icu.util.IslamicCalendar.DAY_OF_MONTH)
                hMonth = icuCal.get(android.icu.util.IslamicCalendar.MONTH) + 1
                hYear = icuCal.get(android.icu.util.IslamicCalendar.YEAR)
                calculated = true
            } catch (_: Throwable) {
                calculated = false
            }
        }

        // 3. Tabular Islamic Calendar algorithm fallback
        if (!calculated) {
            val (d, m, y) = calculateTabularHijri(
                adjustedCal.get(Calendar.YEAR),
                adjustedCal.get(Calendar.MONTH) + 1,
                adjustedCal.get(Calendar.DAY_OF_MONTH)
            )
            hDay = d
            hMonth = m
            hYear = y
        }

        // Bound month to valid range (1..12)
        val safeMonth = hMonth.coerceIn(1, 12)
        val safeMonthIdx = safeMonth - 1
        val monthBn = hijriMonthsBn[safeMonthIdx]
        val monthEn = hijriMonthsEn[safeMonthIdx]

        val dayOfWeekIdx = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
        val dowBn = daysOfWeekBn[dayOfWeekIdx]
        val dowEn = daysOfWeekEn[dayOfWeekIdx]

        val dayStrBn = toBanglaDigits(hDay.toString())
        val yearStrBn = toBanglaDigits(hYear.toString())
        val fullDateBn = "$dayStrBn $monthBn $yearStrBn হিজরি"

        val dayStrEn = hDay.toString()
        val yearStrEn = hYear.toString()
        val fullDateEn = "$dayStrEn $monthEn $yearStrEn AH"

        val formattedDisplayBn = "$dayStrBn $monthBn $yearStrBn • $dowBn"
        val formattedDisplayEn = "$dayStrEn $monthEn $yearStrEn • $dowEn"

        val standardLabelBn = if (isBangladesh) "🇧🇩 ইসলামিক ফাউন্ডেশন বাংলাদেশ মান" else "🌐 আন্তর্জাতিক / উম্মুল কুরা মান"
        val standardLabelEn = if (isBangladesh) "🇧🇩 Islamic Foundation Bangladesh Standard" else "🌐 Umm al-Qura Standard"

        return HijriDateResult(
            day = hDay,
            month = safeMonth,
            year = hYear,
            dayStrBn = dayStrBn,
            monthNameBn = monthBn,
            yearStrBn = yearStrBn,
            fullDateBn = fullDateBn,
            dayStrEn = dayStrEn,
            monthNameEn = monthEn,
            yearStrEn = yearStrEn,
            fullDateEn = fullDateEn,
            dayOfWeekBn = dowBn,
            dayOfWeekEn = dowEn,
            formattedDisplayBn = formattedDisplayBn,
            formattedDisplayEn = formattedDisplayEn,
            isBangladeshStandard = isBangladesh,
            standardLabelBn = standardLabelBn,
            standardLabelEn = standardLabelEn
        )
    }

    fun calculateGregorianDate(cal: Calendar): GregorianDateResult {
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val safeMonthIdx = (month - 1).coerceIn(0, 11)
        val monthBn = gregorianMonthsBn[safeMonthIdx]
        val monthEn = gregorianMonthsEn[safeMonthIdx]

        val dayOfWeekIdx = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
        val dowBn = daysOfWeekBn[dayOfWeekIdx]
        val dowEn = daysOfWeekEn[dayOfWeekIdx]

        val dayStrBn = toBanglaDigits(day.toString())
        val yearStrBn = toBanglaDigits(year.toString())
        val fullDateBn = "$dayStrBn $monthBn $yearStrBn"

        val dayStrEn = day.toString()
        val yearStrEn = year.toString()
        val fullDateEn = "$dayStrEn $monthEn $yearStrEn"

        return GregorianDateResult(
            day = day,
            month = month,
            year = year,
            dayStrBn = dayStrBn,
            monthNameBn = monthBn,
            yearStrBn = yearStrBn,
            fullDateBn = fullDateBn,
            dayStrEn = dayStrEn,
            monthNameEn = monthEn,
            yearStrEn = yearStrEn,
            fullDateEn = fullDateEn,
            dayOfWeekBn = dowBn,
            dayOfWeekEn = dowEn
        )
    }

    fun calculateTabularHijri(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        var m = month
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = (y / 100).toInt()
        val b = 2 - a + (a / 4).toInt()
        val jd = (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524.5

        val daysSinceEpoch = jd - 1948439.5 + 0.5
        val cycle = (daysSinceEpoch / 10631.0).toInt()
        val remainingDaysInCycle = daysSinceEpoch - (cycle * 10631)
        val yearInCycle = ((remainingDaysInCycle - 0.5) / 354.366).toInt().coerceIn(0, 29)
        val hYear = cycle * 30 + yearInCycle + 1

        val monthLengths = intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)
        val leapYears = intArrayOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        if (leapYears.contains(yearInCycle + 1)) {
            monthLengths[11] = 30
        }
        var dayOfYear = (remainingDaysInCycle - (yearInCycle * 354.366).toInt()).toInt()
        if (dayOfYear <= 0) dayOfYear = 1
        var hMonth = 1
        var d = dayOfYear
        for (i in 0 until 12) {
            if (d <= monthLengths[i]) {
                hMonth = i + 1
                break
            }
            d -= monthLengths[i]
        }
        val hDay = d.coerceAtLeast(1)
        return Triple(hDay, hMonth, hYear)
    }

    fun getMoonPhase(cal: Calendar, dayOffset: Int = 0, isBangladesh: Boolean = false): MoonPhaseInfo {
        val regionalOffset = if (isBangladesh) -1 else 0
        val totalOffset = regionalOffset + dayOffset

        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH) + totalOffset

        val c = if (m < 3) y - 1 else y
        val e = if (m < 3) m + 12 else m
        val b = c / 100
        val a = 2 - b + b / 4
        val jd = (365.25 * (c + 4716)).toInt() + (30.6001 * (e + 1)).toInt() + d + a - 1524.5

        val daysSinceNew = (jd - 2451549.5) % 29.53058867
        val moonAge = if (daysSinceNew < 0) daysSinceNew + 29.53058867 else daysSinceNew

        val phaseAngle = (moonAge / 29.53058867) * 2 * Math.PI
        val illuminationPercent = ((1 - Math.cos(phaseAngle)) / 2 * 100).toInt().coerceIn(0, 100)

        return when {
            moonAge < 1.84566 -> MoonPhaseInfo("নতুন চাঁদ (হিলাল)", "New Moon", illuminationPercent, "🌑", moonAge)
            moonAge < 5.53699 -> MoonPhaseInfo("ক্রমবর্ধমান অর্ধচন্দ্র", "Waxing Crescent", illuminationPercent, "🌒", moonAge)
            moonAge < 9.22831 -> MoonPhaseInfo("প্রথম পাদ (আধাচাঁদ)", "First Quarter", illuminationPercent, "🌓", moonAge)
            moonAge < 12.91963 -> MoonPhaseInfo("ক্রমবর্ধমান পূর্ণচন্দ্র", "Waxing Gibbous", illuminationPercent, "🌔", moonAge)
            moonAge < 16.61096 -> MoonPhaseInfo("পূর্ণিমা (বদর)", "Full Moon (Badr)", illuminationPercent, "🌕", moonAge)
            moonAge < 20.30228 -> MoonPhaseInfo("ক্ষীয়মাণ পূর্ণচন্দ্র", "Waning Gibbous", illuminationPercent, "🌖", moonAge)
            moonAge < 23.99361 -> MoonPhaseInfo("শেষ পাদ (আধাচাঁদ)", "Last Quarter", illuminationPercent, "🌗", moonAge)
            moonAge < 27.68493 -> MoonPhaseInfo("ক্ষীয়মাণ অর্ধচন্দ্র", "Waning Crescent", illuminationPercent, "🌘", moonAge)
            else -> MoonPhaseInfo("নতুন চাঁদ (হিলাল)", "New Moon", illuminationPercent, "🌑", moonAge)
        }
    }

    fun getHijriMonthGrid(hijriYear: Int, hijriMonth: Int, dayOffset: Int = 0, isBangladesh: Boolean = false): List<HijriMonthDay> {
        val todayCal = Calendar.getInstance()
        val todayHijri = calculateHijriDate(todayCal, dayOffset, isBangladesh)

        val daysList = mutableListOf<HijriMonthDay>()
        val maxDays = if (hijriMonth % 2 == 1 || (hijriMonth == 12 && hijriYear % 30 in listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29))) 30 else 29

        val searchCal = (todayCal.clone() as Calendar).apply {
            val monthDiff = (hijriYear - todayHijri.year) * 12 + (hijriMonth - todayHijri.month)
            add(Calendar.DAY_OF_MONTH, (monthDiff * 29.53).toInt() - todayHijri.day + 1)
        }

        var hCheck = calculateHijriDate(searchCal, dayOffset, isBangladesh)
        var guard = 0
        while ((hCheck.year != hijriYear || hCheck.month != hijriMonth || hCheck.day != 1) && guard < 60) {
            if (hCheck.year < hijriYear || (hCheck.year == hijriYear && hCheck.month < hijriMonth)) {
                searchCal.add(Calendar.DAY_OF_MONTH, 1)
            } else if (hCheck.year > hijriYear || (hCheck.year == hijriYear && hCheck.month > hijriMonth)) {
                searchCal.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                searchCal.add(Calendar.DAY_OF_MONTH, 1 - hCheck.day)
            }
            hCheck = calculateHijriDate(searchCal, dayOffset, isBangladesh)
            guard++
        }

        for (d in 1..maxDays) {
            val currentCal = (searchCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, d - 1)
            }
            val currentHijri = calculateHijriDate(currentCal, dayOffset, isBangladesh)

            val isToday = (currentHijri.year == todayHijri.year && currentHijri.month == todayHijri.month && currentHijri.day == todayHijri.day)
            val isAyyamAlBeed = currentHijri.day in listOf(13, 14, 15)
            val isFriday = currentCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

            val (specialBn, specialEn) = getSpecialEventForDay(currentHijri.month, currentHijri.day)

            daysList.add(
                HijriMonthDay(
                    hijriDay = currentHijri.day,
                    hijriMonth = currentHijri.month,
                    hijriYear = currentHijri.year,
                    gregorianDay = currentCal.get(Calendar.DAY_OF_MONTH),
                    gregorianMonth = currentCal.get(Calendar.MONTH) + 1,
                    gregorianYear = currentCal.get(Calendar.YEAR),
                    dayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK),
                    isToday = isToday,
                    isAyyamAlBeed = isAyyamAlBeed,
                    isFriday = isFriday,
                    specialEventBn = specialBn,
                    specialEventEn = specialEn
                )
            )
        }

        return daysList
    }

    private fun getSpecialEventForDay(month: Int, day: Int): Pair<String?, String?> {
        return when (month) {
            1 -> when (day) {
                1 -> Pair("হিজরি নববর্ষ", "Islamic New Year")
                9 -> Pair("তাসূআ রোজা", "Tasu'a Fast")
                10 -> Pair("পবিত্র আশুরা", "Day of Ashura")
                else -> Pair(null, null)
            }
            3 -> when (day) {
                12 -> Pair("ঈদে মিলাদুন্নবী", "Mawlid an-Nabi")
                else -> Pair(null, null)
            }
            7 -> when (day) {
                27 -> Pair("পবিত্র শবে মেরাজ", "Shab-e-Meraj")
                else -> Pair(null, null)
            }
            8 -> when (day) {
                15 -> Pair("পবিত্র শবে বরাত", "Shab-e-Barat")
                else -> Pair(null, null)
            }
            9 -> when (day) {
                1 -> Pair("১ম রমজান শুরু", "1st Ramadan")
                21, 23, 25, 27, 29 -> Pair("সম্ভাব্য শবে কদর", "Laylat al-Qadr")
                else -> Pair(null, null)
            }
            10 -> when (day) {
                1 -> Pair("পবিত্র ঈদুল ফিতর", "Eid al-Fitr")
                2 -> Pair("ঈদের ২য় দিন", "2nd Day of Eid")
                else -> Pair(null, null)
            }
            12 -> when (day) {
                1 -> Pair("জিলহজ্জের প্রথম দশক", "1st 10 Days of Dhul Hijjah")
                8 -> Pair("হজ্জের প্রথম দিন (ইয়াওমুত তারবিয়াহ)", "Day of Tarwiyah")
                9 -> Pair("পবিত্র আরাফাত দিবস", "Day of Arafah")
                10 -> Pair("পবিত্র ঈদুল আজহা (কোরবানি)", "Eid al-Adha")
                11, 12, 13 -> Pair("আইয়ামে তাশরিক", "Ayyam at-Tashreeq")
                else -> Pair(null, null)
            }
            else -> Pair(null, null)
        }
    }

    fun getDynamicIslamicMilestones(currentHijri: HijriDateResult): List<DynamicIslamicMilestone> {
        val milestones = listOf(
            Triple(1, 1, Pair("হিজরি নববর্ষ (১ মুহাররম)", "Islamic New Year (1 Muharram)")),
            Triple(1, 10, Pair("পবিত্র আশুরা (১০ মুহাররম)", "Day of Ashura (10 Muharram)")),
            Triple(3, 12, Pair("পবিত্র ঈদে মিলাদুন্নবী (১২ রবিউল আউয়াল)", "Mawlid an-Nabi (12 Rabi' al-Awwal)")),
            Triple(7, 27, Pair("পবিত্র শবে মেরাজ (২৭ রজব)", "Shab-e-Meraj (27 Rajab)")),
            Triple(8, 15, Pair("পবিত্র শবে বরাত (১৫ শাবান)", "Shab-e-Barat (15 Sha'ban)")),
            Triple(9, 1, Pair("পবিত্র মাহে রমজান (১ রমজান)", "1st Day of Ramadan (1 Ramadan)")),
            Triple(9, 27, Pair("পবিত্র শবে কদর (২৭ রমজান)", "Laylatul Qadr (27 Ramadan)")),
            Triple(10, 1, Pair("পবিত্র ঈদুল ফিতর (১ শাওয়াল)", "Eid al-Fitr (1 Shawwal)")),
            Triple(12, 9, Pair("পবিত্র আরাফাত দিবস (৯ জিলহজ্জ)", "Day of Arafah (9 Dhu al-Hijjah)")),
            Triple(12, 10, Pair("পবিত্র ঈদুল আজহা (১০ জিলহজ্জ)", "Eid al-Adha (10 Dhu al-Hijjah)"))
        )

        val curYear = currentHijri.year
        val curMonth = currentHijri.month
        val curDay = currentHijri.day

        fun hijriDayOfYear(m: Int, d: Int): Int {
            var days = d
            for (i in 1 until m) {
                days += if (i % 2 == 1) 30 else 29
            }
            return days
        }

        val curDayOfYear = hijriDayOfYear(curMonth, curDay)

        return milestones.map { (targetM, targetD, titles) ->
            val targetDayOfYear = hijriDayOfYear(targetM, targetD)
            val diff = targetDayOfYear - curDayOfYear
            val daysRem = if (diff >= 0) diff else (354 + diff)
            val isPassed = diff < 0

            val descBn = when (targetM) {
                1 -> "ঐতিহাসিক ও মহিমান্বিত তওবা এবং বরকতময় রোজা রাখার দিবস।"
                3 -> "বিশ্বনবী হযরত মুহাম্মদ (সা.)-এর পবিত্র বেলাদত ও শুভাগমন।"
                7 -> "রাসূলুল্লাহ (সা.)-এর ঊর্ধ্বাকাশে পরিভ্রমণ ও পাঁচ ওয়াক্ত নামাজের উপহার লাভ।"
                8 -> "সৌভাগ্য রজনী, গুনাহ মাফ ও আল্লাহর রহমত প্রার্থনার বরকতময় রাত।"
                9 -> if (targetD == 1) "রোজা, তারাবীহ, কুরআন তেলাওয়াত ও আত্মশুদ্ধির সেরা মাস শুরু।" else "হাজার মাসের চেয়েও শ্রেষ্ঠতম মর্যাদাপূর্ণ বরকতময় রাত।"
                10 -> "একমাস সিয়াম সাধনার পর মুসলমানদের মহা আনন্দের উৎসব।"
                12 -> if (targetD == 9) "হজ্জের প্রধান রুকন ও সগিরা গুনাহ ক্ষমার মহাসুযোগের রোজা।" else "হযরত ইব্রাহিম (আ.)-এর মহান ত্যাগের স্মরণে ওয়াজিব কোরবানি।"
                else -> "ইসলামের গুরুত্বপূর্ণ ফজিলতপূর্ণ দিবস।"
            }

            val descEn = when (targetM) {
                1 -> "Sacred day of Ashura, fasting and profound Islamic reflection."
                3 -> "Commemoration of the birth of the Prophet Muhammad (ﷺ)."
                7 -> "The miraculous Night Journey and Heavenly Ascension."
                8 -> "Night of Records and seeking forgiveness from Allah SWT."
                9 -> if (targetD == 1) "Beginning of the blessed fasting and spiritual rejuvenation." else "The Night of Decree, better than a thousand months."
                10 -> "Joyous celebration marking the conclusion of Ramadan."
                12 -> if (targetD == 9) "The climax of the Hajj pilgrimage and day of immense mercy." else "Festival of Sacrifice honoring the obedience of Prophet Ibrahim (AS)."
                else -> "Significant Islamic milestone."
            }

            val dateStrBn = "${toBanglaDigits(targetD.toString())} ${hijriMonthsBn[targetM - 1]} ${toBanglaDigits(if (isPassed) (curYear + 1).toString() else curYear.toString())} হিজরি"
            val dateStrEn = "$targetD ${hijriMonthsEn[targetM - 1]} ${if (isPassed) curYear + 1 else curYear} AH"

            DynamicIslamicMilestone(
                id = "${targetM}_${targetD}",
                titleBn = titles.first,
                titleEn = titles.second,
                hijriDateStrBn = dateStrBn,
                hijriDateStrEn = dateStrEn,
                targetMonth = targetM,
                targetDay = targetD,
                descriptionBn = descBn,
                descriptionEn = descEn,
                daysRemaining = daysRem,
                isPassedThisYear = isPassed
            )
        }.sortedBy { it.daysRemaining }
    }
}
