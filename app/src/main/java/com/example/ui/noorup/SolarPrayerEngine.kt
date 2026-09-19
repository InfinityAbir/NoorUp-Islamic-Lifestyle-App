package com.example.ui.noorup

import java.util.Calendar
import kotlin.math.*

enum class CalculationMethod(
    val titleBn: String,
    val titleEn: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val isIshaFixedMinutes: Boolean = false,
    val isIshaFixedMinutesCount: Int = 0
) {
    KARACHI("ইসলামিক ইউনিভার্সিটি, করাচি (সাউথ এশিয়া)", "Univ. of Islamic Sciences, Karachi (South Asia)", 18.0, 18.0),
    MWL("মুসলিম ওয়ার্ল্ড লীগ (MWL)", "Muslim World League (MWL)", 18.0, 17.0),
    EGYPT("মিশরীয় জরিপ কর্তৃপক্ষ (EGAS)", "Egyptian General Authority of Survey", 19.5, 17.5),
    ISNA("ইসলামিক সোসাইটি অব নর্থ আমেরিকা (ISNA)", "Islamic Society of North America (ISNA)", 15.0, 15.0),
    MAKKAH("উম্মুল কুরা বিশ্ববিদ্যালয়, মক্কা", "Umm al-Qura University, Makkah", 18.5, 0.0, true, 90),
    TEHRAN("তেহরান বিশ্ববিদ্যালয়", "Institute of Geophysics, Tehran", 17.7, 14.0)
}

enum class JuristicMethod(val titleBn: String, val titleEn: String, val shadowFactor: Double) {
    HANAFI("হানাফী (দ্বিগুণ ছায়া)", "Hanafi (2x Shadow)", 2.0),
    STANDARD("শাফেয়ী, মালেকী, হাম্বলী (একগুণ ছায়া)", "Shafi'i / Standard (1x Shadow)", 1.0)
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
    val tahajjudEnd: String,
    val sehriEnd: String,
    val iftarStart: String,
    val calculationMethod: CalculationMethod = CalculationMethod.KARACHI,
    val juristicMethod: JuristicMethod = JuristicMethod.HANAFI
)

data class QiblaInfo(
    val bearingDegrees: Double,
    val distanceKm: Double,
    val cardinalDirectionEn: String,
    val cardinalDirectionBn: String,
    val latitude: Double,
    val longitude: Double
)

object SolarPrayerEngine {

    private fun d2r(d: Double): Double = d * Math.PI / 180.0
    private fun r2d(r: Double): Double = r * 180.0 / Math.PI

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    fun getJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val ra = r2d(atan2(cos(d2r(e)) * sin(d2r(l)), cos(d2r(l)))) / 15.0
        val declination = r2d(asin(sin(d2r(e)) * sin(d2r(l))))
        val eqTime = (q / 15.0 - fixHour(ra)) * 60.0

        return Pair(declination, eqTime)
    }

    fun calculateTimes(
        latitude: Double,
        longitude: Double,
        calendar: Calendar,
        timezoneOffset: Double = 6.0,
        method: CalculationMethod = CalculationMethod.KARACHI,
        juristic: JuristicMethod = JuristicMethod.HANAFI
    ): CalculatedPrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = getJulianDate(year, month, day)
        val (decl, eqTime) = sunPosition(jd)

        // Solar Noon (Dhuhr Transit)
        val transit = 12.0 + timezoneOffset - (longitude / 15.0) - (eqTime / 60.0)
        val dhuhrHours = transit + (2.0 / 60.0) // 2 minutes safety buffer

        fun hourAngle(angle: Double): Double {
            val cosHA = (sin(d2r(angle)) - sin(d2r(latitude)) * sin(d2r(decl))) /
                    (cos(d2r(latitude)) * cos(d2r(decl)))
            if (cosHA < -1.0) return 180.0
            if (cosHA > 1.0) return 0.0
            return r2d(acos(cosHA))
        }

        // Sunrise & Sunset (Standard atmospheric refraction & disk radius = -0.8333 deg)
        val sunriseHA = hourAngle(-0.8333)
        val sunriseHours = transit - (sunriseHA / 15.0)
        val sunsetHours = transit + (sunriseHA / 15.0)

        // Fajr (Astronomical dawn)
        val fajrHA = hourAngle(-method.fajrAngle)
        val fajrHours = transit - (fajrHA / 15.0)

        // Asr (Shadow ratio calculation)
        val asrAlt = r2d(atan(1.0 / (juristic.shadowFactor + tan(d2r(abs(latitude - decl))))))
        val asrHA = hourAngle(asrAlt)
        val asrHours = transit + (asrHA / 15.0)

        // Maghrib (Sunset + 2 min buffer)
        val maghribHours = sunsetHours + (2.0 / 60.0)

        // Isha (Astronomical twilight end)
        val ishaHours = if (method.isIshaFixedMinutes) {
            maghribHours + (method.isIshaFixedMinutesCount / 60.0)
        } else {
            val ishaHA = hourAngle(-method.ishaAngle)
            transit + (ishaHA / 15.0)
        }

        fun formatTime(hours: Double): String {
            val hTotalMins = (fixHour(hours) * 60.0).roundToInt()
            var h = (hTotalMins / 60) % 24
            val m = hTotalMins % 60
            val isPm = h >= 12
            if (h > 12) h -= 12
            if (h == 0) h = 12
            val amPm = if (isPm) "PM" else "AM"
            return String.format("%02d:%02d %s", h, m, amPm)
        }

        fun formatTimePlusMins(hours: Double, addedMins: Int): String {
            var hTotalMins = (fixHour(hours) * 60.0).roundToInt() + addedMins
            if (hTotalMins < 0) hTotalMins += 1440
            hTotalMins %= 1440
            var h = (hTotalMins / 60) % 24
            val m = hTotalMins % 60
            val isPm = h >= 12
            if (h > 12) h -= 12
            if (h == 0) h = 12
            val amPm = if (isPm) "PM" else "AM"
            return String.format("%02d:%02d %s", h, m, amPm)
        }

        val fajrStr = formatTime(fajrHours)
        val sunriseStr = formatTime(sunriseHours)
        val sunriseEndStr = formatTimePlusMins(sunriseHours, 16)

        val dhuhrStartStr = formatTime(dhuhrHours)
        val zawalStartStr = formatTimePlusMins(dhuhrHours, -15)

        val asrStartStr = formatTime(asrHours)
        val sunsetStartStr = formatTimePlusMins(sunsetHours, -18)
        val maghribStartStr = formatTime(maghribHours)

        val ishaStartStr = formatTime(ishaHours)
        val ishaEndStr = fajrStr

        // Tahajjud (Recommended in last third of the night)
        val maghribMins = (fixHour(maghribHours) * 60.0).roundToInt()
        val fajrMins = (fixHour(fajrHours) * 60.0).roundToInt()
        val nightDuration = if (fajrMins >= maghribMins) fajrMins - maghribMins else (1440 - maghribMins + fajrMins)
        val lastThirdStartMins = (maghribMins + (nightDuration * 2 / 3)) % 1440
        var tH = lastThirdStartMins / 60
        val tM = lastThirdStartMins % 60
        val tIsPm = tH >= 12
        if (tH > 12) tH -= 12
        if (tH == 0) tH = 12
        val tahajjudStartStr = String.format("%02d:%02d %s", tH, tM, if (tIsPm) "PM" else "AM")
        val tahajjudEndStr = formatTimePlusMins(fajrHours, -20)

        return CalculatedPrayerTimes(
            date = calendar.clone() as Calendar,
            fajrStart = fajrStr,
            fajrEnd = sunriseStr,
            sunrise = sunriseStr,
            makruhSunriseStart = sunriseStr,
            makruhSunriseEnd = sunriseEndStr,
            dhuhrStart = dhuhrStartStr,
            dhuhrEnd = asrStartStr,
            makruhZawalStart = zawalStartStr,
            makruhZawalEnd = dhuhrStartStr,
            asrStart = asrStartStr,
            asrEnd = maghribStartStr,
            makruhSunsetStart = sunsetStartStr,
            makruhSunsetEnd = maghribStartStr,
            maghribStart = maghribStartStr,
            maghribEnd = ishaStartStr,
            ishaStart = ishaStartStr,
            ishaEnd = ishaEndStr,
            tahajjudStart = tahajjudStartStr,
            tahajjudEnd = tahajjudEndStr,
            sehriEnd = fajrStr,
            iftarStart = maghribStartStr,
            calculationMethod = method,
            juristicMethod = juristic
        )
    }

    /**
     * Calculates the Qibla bearing (azimuth from true North) and distance from the given coordinates
     * to the Holy Kaaba in Makkah (21.422487° N, 39.826206° E).
     */
    fun calculateQibla(latitude: Double, longitude: Double): QiblaInfo {
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(21.422487)
        val lon2 = Math.toRadians(39.826206)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val initialBearing = Math.toDegrees(atan2(y, x))
        val bearing = (initialBearing + 360.0) % 360.0

        // Haversine distance
        val dLat = lat2 - lat1
        val a = sin(dLat / 2.0).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        val distance = 6371.0 * c

        val (cardinalEn, cardinalBn) = getCardinalName(bearing)

        return QiblaInfo(
            bearingDegrees = bearing,
            distanceKm = distance,
            cardinalDirectionEn = cardinalEn,
            cardinalDirectionBn = cardinalBn,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun getCardinalName(degrees: Double): Pair<String, String> {
        val normalized = (degrees % 360.0 + 360.0) % 360.0
        return when {
            normalized >= 348.75 || normalized < 11.25 -> Pair("North (N)", "উত্তর (N)")
            normalized < 33.75 -> Pair("North-Northeast (NNE)", "উত্তর-উত্তর-পূর্ব (NNE)")
            normalized < 56.25 -> Pair("Northeast (NE)", "উত্তর-পূর্ব (NE)")
            normalized < 78.75 -> Pair("East-Northeast (ENE)", "পূর্ব-উত্তর-পূর্ব (ENE)")
            normalized < 101.25 -> Pair("East (E)", "পূর্ব (E)")
            normalized < 123.75 -> Pair("East-Southeast (ESE)", "পূর্ব-দক্ষিণ-পূর্ব (ESE)")
            normalized < 146.25 -> Pair("Southeast (SE)", "দক্ষিণ-পূর্ব (SE)")
            normalized < 168.75 -> Pair("South-Southeast (SSE)", "দক্ষিণ-দক্ষিণ-পূর্ব (SSE)")
            normalized < 191.25 -> Pair("South (S)", "দক্ষিণ (S)")
            normalized < 213.75 -> Pair("South-Southwest (SSW)", "দক্ষিণ-দক্ষিণ-পশ্চিম (SSW)")
            normalized < 236.25 -> Pair("Southwest (SW)", "দক্ষিণ-পশ্চিম (SW)")
            normalized < 258.75 -> Pair("West-Southwest (WSW)", "পশ্চিম-দক্ষিণ-পশ্চিম (WSW)")
            normalized < 281.25 -> Pair("West (W)", "পশ্চিম (W)")
            normalized < 303.75 -> Pair("West-Northwest (WNW)", "পশ্চিম-উত্তর-পশ্চিম (WNW)")
            normalized < 326.25 -> Pair("Northwest (NW)", "উত্তর-পশ্চিম (NW)")
            else -> Pair("North-Northwest (NNW)", "উত্তর-উত্তর-পশ্চিম (NNW)")
        }
    }
}
