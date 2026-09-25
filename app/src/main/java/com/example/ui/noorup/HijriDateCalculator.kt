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
    val fullDateEn: String
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
    val fullDateEn: String
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

    fun toBanglaDigits(numberStr: String): String {
        val enToBn = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return numberStr.map { enToBn[it] ?: it }.joinToString("")
    }

    /**
     * Calculates the Hijri date for the given Gregorian Calendar instance.
     * Supports manual day offset (e.g., -1, 0, +1 for moon-sighting adjustment).
     */
    fun calculateHijriDate(cal: Calendar, dayOffset: Int = 0): HijriDateResult {
        val adjustedCal = (cal.clone() as Calendar).apply {
            if (dayOffset != 0) {
                add(Calendar.DAY_OF_MONTH, dayOffset)
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

        // 3. Mathematical civil Islamic calendar fallback
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

        // Bound month to valid range
        val safeMonthIdx = (hMonth - 1).coerceIn(0, 11)
        val monthBn = hijriMonthsBn[safeMonthIdx]
        val monthEn = hijriMonthsEn[safeMonthIdx]

        val dayStrBn = toBanglaDigits(hDay.toString())
        val yearStrBn = toBanglaDigits(hYear.toString())
        val fullDateBn = "$dayStrBn $monthBn $yearStrBn"

        val dayStrEn = hDay.toString()
        val yearStrEn = hYear.toString()
        val fullDateEn = "$dayStrEn $monthEn $yearStrEn"

        return HijriDateResult(
            day = hDay,
            month = hMonth,
            year = hYear,
            dayStrBn = dayStrBn,
            monthNameBn = monthBn,
            yearStrBn = yearStrBn,
            fullDateBn = fullDateBn,
            dayStrEn = dayStrEn,
            monthNameEn = monthEn,
            yearStrEn = yearStrEn,
            fullDateEn = fullDateEn
        )
    }

    /**
     * Formats Gregorian date for display.
     * Example Bn: "২৫ সেপ্টেম্বর ২০২৬"
     * Example En: "25 September 2026"
     */
    fun calculateGregorianDate(cal: Calendar): GregorianDateResult {
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val safeMonthIdx = (month - 1).coerceIn(0, 11)
        val monthBn = gregorianMonthsBn[safeMonthIdx]
        val monthEn = gregorianMonthsEn[safeMonthIdx]

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
            fullDateEn = fullDateEn
        )
    }

    private fun calculateTabularHijri(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
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
}
