package com.example.ui.noorup

import java.util.Calendar
import java.util.Locale
import kotlin.math.*

object SolarPrayerEngine {

    fun calculateTimes(
        latitude: Double,
        longitude: Double,
        calendar: Calendar,
        timezoneOffset: Double,
        method: CalculationMethod = CalculationMethod.KARACHI,
        juristic: JuristicMethod = JuristicMethod.HANAFI
    ): CalculatedPrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Julian Day Calculation
        val a = floor((14 - month) / 12.0)
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        var jd = day + floor((153 * m + 2) / 5.0) + 365 * y + floor(y / 4.0) - floor(y / 100.0) + floor(y / 400.0) - 32045.0
        val d = jd - 2451545.0

        // Sun position calculations
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(dtr(g)) + 0.020 * sin(dtr(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val declination = rtd(asin(sin(dtr(e)) * sin(dtr(l))))
        var ra = rtd(atan2(cos(dtr(e)) * sin(dtr(l)), cos(dtr(l)))) / 15.0
        ra = fixHour(ra)

        val eqt = q / 15.0 - ra
        val dhuhrHours = fixHour(12.0 + timezoneOffset - longitude / 15.0 - eqt)

        // Sunrise and Sunset (approx angle 0.833 degrees due to refraction)
        val alphaSun = 0.833
        val sunriseHourAngle = calculateHourAngle(latitude, declination, alphaSun)
        val sunriseHours = fixHour(dhuhrHours - sunriseHourAngle / 15.0)
        val sunsetHours = fixHour(dhuhrHours + sunriseHourAngle / 15.0)

        // Fajr
        val fajrHourAngle = calculateHourAngle(latitude, declination, method.fajrAngle)
        val fajrHours = fixHour(dhuhrHours - fajrHourAngle / 15.0)

        // Asr calculation
        val shadowFactor = if (juristic == JuristicMethod.HANAFI) 2.0 else 1.0
        val asrAngle = -rtd(atan(1.0 / (shadowFactor + tan(dtr(abs(latitude - declination))))))
        val asrHourAngle = calculateHourAngle(latitude, declination, asrAngle)
        val asrHours = fixHour(dhuhrHours + asrHourAngle / 15.0)

        // Maghrib
        val maghribHours = sunsetHours

        // Isha
        val ishaHours = if (method.isIshaFixedMinutes) {
            fixHour(maghribHours + method.isIshaFixedMinutesCount / 60.0)
        } else {
            val ishaHourAngle = calculateHourAngle(latitude, declination, method.ishaAngle)
            fixHour(dhuhrHours + ishaHourAngle / 15.0)
        }

        val fajrStr = formatTime(fajrHours)
        val fajrEndStr = formatTimePlusMins(sunriseHours, -1)
        val sunriseStr = formatTime(sunriseHours)
        val sunriseEndStr = formatTimePlusMins(sunriseHours, 16)

        val zawalStartStr = formatTimePlusMins(dhuhrHours, -15)
        val zawalEndStr = formatTimePlusMins(dhuhrHours, -1)
        val dhuhrStartStr = formatTime(dhuhrHours)
        val dhuhrEndStr = formatTimePlusMins(asrHours, -1)

        val asrStartStr = formatTime(asrHours)
        val asrEndStr = formatTimePlusMins(maghribHours, -1)
        val sunsetStartStr = formatTimePlusMins(sunsetHours, -18)
        val sunsetEndStr = formatTimePlusMins(maghribHours, -1)
        val maghribStartStr = formatTime(maghribHours)
        val maghribEndStr = formatTimePlusMins(ishaHours, -1)

        val ishaStartStr = formatTime(ishaHours)
        val ishaEndStr = formatTimePlusMins(fajrHours, -1)

        // Tahajjud (Recommended in last third of the night)
        val maghribMins = (fixHour(maghribHours) * 60.0).roundToInt()
        val fajrNextMins = (fixHour(fajrHours) * 60.0).roundToInt() + 1440
        val nightDuration = fajrNextMins - maghribMins
        val tahajjudStartMins = (maghribMins + (nightDuration * 2) / 3) % 1440
        val tahajjudStartStr = formatMinutes(tahajjudStartMins)

        return CalculatedPrayerTimes(
            date = calendar.clone() as Calendar,
            fajrStart = fajrStr,
            fajrEnd = fajrEndStr,
            sunrise = sunriseStr,
            makruhSunriseStart = sunriseStr,
            makruhSunriseEnd = sunriseEndStr,
            dhuhrStart = dhuhrStartStr,
            dhuhrEnd = dhuhrEndStr,
            makruhZawalStart = zawalStartStr,
            makruhZawalEnd = zawalEndStr,
            asrStart = asrStartStr,
            asrEnd = asrEndStr,
            makruhSunsetStart = sunsetStartStr,
            makruhSunsetEnd = sunsetEndStr,
            maghribStart = maghribStartStr,
            maghribEnd = maghribEndStr,
            ishaStart = ishaStartStr,
            ishaEnd = ishaEndStr,
            tahajjudStart = tahajjudStartStr,
            sehriEnd = formatTimePlusMins(fajrHours, -1),
            iftarStart = maghribStartStr,
            calculationMethod = method,
            juristicMethod = juristic
        )
    }

    private fun calculateHourAngle(lat: Double, decl: Double, angle: Double): Double {
        val cosHA = (-sin(dtr(angle)) - sin(dtr(lat)) * sin(dtr(decl))) / (cos(dtr(lat)) * cos(dtr(decl)))
        val clampedCos = cosHA.coerceIn(-1.0, 1.0)
        return rtd(acos(clampedCos))
    }

    private fun dtr(d: Double): Double = d * (PI / 180.0)
    private fun rtd(r: Double): Double = r * (180.0 / PI)
    private fun fixAngle(a: Double): Double = (a % 360.0 + 360.0) % 360.0
    private fun fixHour(h: Double): Double = (h % 24.0 + 24.0) % 24.0

    private fun formatTime(hours: Double): String {
        val totalMins = (fixHour(hours) * 60.0).roundToInt()
        return formatMinutes(totalMins)
    }

    private fun formatTimePlusMins(hours: Double, addMinutes: Int): String {
        val totalMins = ((fixHour(hours) * 60.0).roundToInt() + addMinutes + 1440) % 1440
        return formatMinutes(totalMins)
    }

    private fun formatMinutes(totalMins: Int): String {
        val h24 = totalMins / 60
        val m = totalMins % 60
        val isPm = h24 >= 12
        var h12 = h24 % 12
        if (h12 == 0) h12 = 12
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.ENGLISH, "%02d:%02d %s", h12, m, amPm)
    }

    fun calculateQibla(lat: Double, lng: Double): QiblaInfo {
        val makkahLat = Math.toRadians(21.422487)
        val makkahLng = Math.toRadians(39.826206)
        val userLat = Math.toRadians(lat)
        val userLng = Math.toRadians(lng)

        val deltaLng = makkahLng - userLng
        val y = sin(deltaLng)
        val x = cos(userLat) * tan(makkahLat) - sin(userLat) * cos(deltaLng)
        var qiblaAngle = Math.toDegrees(atan2(y, x))
        qiblaAngle = (qiblaAngle + 360.0) % 360.0

        val earthRadiusKm = 6371.0
        val deltaLat = makkahLat - userLat
        val a = sin(deltaLat / 2).pow(2) + cos(userLat) * cos(makkahLat) * sin(deltaLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = earthRadiusKm * c

        val dirEn = when {
            qiblaAngle in 22.5..67.5 -> "North-East"
            qiblaAngle in 67.5..112.5 -> "East"
            qiblaAngle in 112.5..157.5 -> "South-East"
            qiblaAngle in 157.5..202.5 -> "South"
            qiblaAngle in 202.5..247.5 -> "South-West"
            qiblaAngle in 247.5..292.5 -> "West"
            qiblaAngle in 292.5..337.5 -> "North-West"
            else -> "North"
        }

        val dirBn = when {
            qiblaAngle in 22.5..67.5 -> "উত্তর-পূর্ব"
            qiblaAngle in 67.5..112.5 -> "পূর্ব"
            qiblaAngle in 112.5..157.5 -> "দক্ষিণ-পূর্ব"
            qiblaAngle in 157.5..202.5 -> "দক্ষিণ"
            qiblaAngle in 202.5..247.5 -> "দক্ষিণ-পশ্চিম"
            qiblaAngle in 247.5..292.5 -> "পশ্চিম"
            qiblaAngle in 292.5..337.5 -> "উত্তর-পশ্চিম"
            else -> "উত্তর"
        }

        return QiblaInfo(
            bearingDegrees = qiblaAngle.toFloat(),
            distanceKm = distance,
            cardinalDirectionEn = dirEn,
            cardinalDirectionBn = dirBn
        )
    }
}
