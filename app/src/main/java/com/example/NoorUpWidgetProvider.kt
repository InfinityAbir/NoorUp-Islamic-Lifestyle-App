package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.ui.noorup.CalculationMethod
import com.example.ui.noorup.HijriDateCalculator
import com.example.ui.noorup.JuristicMethod
import com.example.ui.noorup.SolarPrayerEngine
import java.util.Calendar

class NoorUpWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        } catch (e: Exception) {
            android.util.Log.e("NoorUpWidget", "Error in onUpdate", e)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NoorUpWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NoorUpWidget", "Error in onReceive", e)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, NoorUpWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, NoorUpWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("NoorUpWidget", "Error updating all widgets", e)
            }
        }

        private fun toBanglaDigits(str: String): String {
            val enToBn = mapOf(
                '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
                '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
            )
            return str.map { enToBn[it] ?: it }.joinToString("")
        }

        private fun toEnglishDigits(str: String): String {
            val bnToEn = mapOf(
                '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
                '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
            )
            return str.map { bnToEn[it] ?: it }.joinToString("")
        }

        fun parseTimeToMins(timeStr: String): Int {
            return try {
                val enStr = toEnglishDigits(timeStr).trim().uppercase()
                val isPm = enStr.contains("PM")
                val cleanTime = enStr.replace("AM", "").replace("PM", "").trim()
                val parts = cleanTime.split(":")
                var hour = parts[0].trim().toInt()
                val min = parts[1].trim().toInt()
                if (isPm && hour != 12) hour += 12
                if (!isPm && hour == 12) hour = 0
                hour * 60 + min
            } catch (e: Exception) {
                0
            }
        }

        fun formatShortTime(timeStr: String, isEnglish: Boolean): String {
            return try {
                val totalMins = parseTimeToMins(timeStr)
                val h24 = totalMins / 60
                val m = totalMins % 60
                var h12 = h24 % 12
                if (h12 == 0) h12 = 12
                val hStr = String.format("%02d", h12)
                val mStr = String.format("%02d", m)
                if (isEnglish) "$hStr:$mStr" else "${toBanglaDigits(hStr)}:${toBanglaDigits(mStr)}"
            } catch (e: Exception) {
                timeStr
            }
        }

        fun formatTimeWithAmPm(timeStr: String, isEnglish: Boolean): String {
            return try {
                val totalMins = parseTimeToMins(timeStr)
                val h24 = totalMins / 60
                val m = totalMins % 60
                val isPm = h24 >= 12
                var h12 = h24 % 12
                if (h12 == 0) h12 = 12
                val suffix = if (isPm) "PM" else "AM"
                val hStr = String.format("%02d", h12)
                val mStr = String.format("%02d", m)
                if (isEnglish) "$hStr:$mStr $suffix" else "${toBanglaDigits(hStr)}:${toBanglaDigits(mStr)} $suffix"
            } catch (e: Exception) {
                timeStr
            }
        }

        fun formatRemTime(remMins: Int, isEnglish: Boolean): String {
            val clamped = remMins.coerceAtLeast(0)
            val hrs = clamped / 60
            val mins = clamped % 60
            return if (isEnglish) {
                if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
            } else {
                if (hrs > 0) "${toBanglaDigits(hrs.toString())} ঘ. ${toBanglaDigits(mins.toString())} মি." else "${toBanglaDigits(mins.toString())} মি."
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.noorup_widget)

                // Retrieve preferences for language, city, and GPS coordinates
                val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
                val isEnglish = prefs.getBoolean("is_english", false)
                val defaultCityBn = "উত্তরা, ঢাকা"
                val defaultCityEn = "Uttara, Dhaka"
                val cityName = if (isEnglish) {
                    prefs.getString("selected_city_name_en", defaultCityEn) ?: defaultCityEn
                } else {
                    prefs.getString("selected_city_name_bn", defaultCityBn) ?: defaultCityBn
                }
                val latitude = prefs.getFloat("selected_city_lat", 23.8759f).toDouble()
                val longitude = prefs.getFloat("selected_city_lng", 90.3795f).toDouble()

                // Current Time and Local Timezone
                val cal = Calendar.getInstance()
                val tzOffsetHours = cal.timeZone.getOffset(cal.timeInMillis) / 3600000.0
                val curHour = cal.get(Calendar.HOUR_OF_DAY)
                val curMin = cal.get(Calendar.MINUTE)
                val curMins = curHour * 60 + curMin

                // Method and Juristic
                val methodStr = prefs.getString("calculation_method", CalculationMethod.KARACHI.name)
                val method = CalculationMethod.entries.find { it.name == methodStr } ?: CalculationMethod.KARACHI
                val juristicStr = prefs.getString("juristic_method", JuristicMethod.HANAFI.name)
                val juristic = JuristicMethod.entries.find { it.name == juristicStr } ?: JuristicMethod.HANAFI

                // Calculate astronomical solar prayer times for today and tomorrow
                val todayTimes = SolarPrayerEngine.calculateTimes(
                    latitude = latitude,
                    longitude = longitude,
                    calendar = cal,
                    timezoneOffset = tzOffsetHours,
                    method = method,
                    juristic = juristic
                )

                val tomorrowCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowTimes = SolarPrayerEngine.calculateTimes(
                    latitude = latitude,
                    longitude = longitude,
                    calendar = tomorrowCal,
                    timezoneOffset = tzOffsetHours,
                    method = method,
                    juristic = juristic
                )

                // Parse prayer time boundary minutes
                val fajrS = parseTimeToMins(todayTimes.fajrStart)
                val sunriseS = parseTimeToMins(todayTimes.sunrise)
                val sunriseE = sunriseS + 15
                val zawalS = parseTimeToMins(todayTimes.makruhZawalStart)
                val zawalE = parseTimeToMins(todayTimes.dhuhrStart)
                val dhuhrS = parseTimeToMins(todayTimes.dhuhrStart)
                val asrS = parseTimeToMins(todayTimes.asrStart)
                val sunsetS = parseTimeToMins(todayTimes.makruhSunsetStart)
                val sunsetE = parseTimeToMins(todayTimes.maghribStart)
                val maghribS = parseTimeToMins(todayTimes.maghribStart)
                val ishaS = parseTimeToMins(todayTimes.ishaStart)
                val tomorrowFajrS = parseTimeToMins(tomorrowTimes.fajrStart)

                // Determine active prayer state & remaining countdown
                val activePrayerIndex: Int // 0: Fajr, 1: Dhuhr, 2: Asr, 3: Maghrib, 4: Isha
                val stageLabel: String
                val activeName: String
                val activeWindowStr: String
                val countdownStr: String

                when {
                    // 1. Midnight to Fajr (Tahajjud / Isha period)
                    curMins < fajrS -> {
                        activePrayerIndex = 4
                        val remMins = fajrS - curMins
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (isEnglish) "ACTIVE PRAYER" else "চলমান ওয়াক্ত"
                        activeName = if (isEnglish) "Isha (Tahajjud)" else "এশা (তাহাজ্জুদ)"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.ishaStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.ishaEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Ends in $remStr" else "বাকি $remStr"
                    }
                    // 2. Fajr time (Fajr start to Sunrise)
                    curMins in fajrS until sunriseS -> {
                        activePrayerIndex = 0
                        val remMins = sunriseS - curMins
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (isEnglish) "ACTIVE PRAYER" else "চলমান ওয়াক্ত"
                        activeName = if (isEnglish) "Fajr" else "ফজর"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.fajrStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.fajrEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Ends in $remStr" else "বাকি $remStr"
                    }
                    // 3. Sunrise prohibited interval (Sunrise to Sunrise + 15 min)
                    curMins in sunriseS..sunriseE -> {
                        activePrayerIndex = 1
                        val remMins = dhuhrS - curMins
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (isEnglish) "⚠️ PROHIBITED (SUNRISE)" else "⚠️ মাকরুহ সময় (সূর্যোদয়)"
                        activeName = if (isEnglish) "Next: Dhuhr" else "পরবর্তী: যোহর"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.dhuhrStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.dhuhrEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Starts in $remStr" else "শুরু হতে বাকি $remStr"
                    }
                    // 4. Duha / Chasht morning time
                    curMins in (sunriseE + 1) until zawalS -> {
                        activePrayerIndex = 1
                        val remMins = dhuhrS - curMins
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (isEnglish) "NEXT PRAYER (DUHA)" else "পরবর্তী ওয়াক্ত (চাশত)"
                        activeName = if (isEnglish) "Dhuhr" else "যোহর"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.dhuhrStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.dhuhrEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Starts in $remStr" else "শুরু হতে বাকি $remStr"
                    }
                    // 5. Zawal zenith prohibited interval
                    curMins in zawalS until zawalE -> {
                        activePrayerIndex = 1
                        val remMins = dhuhrS - curMins
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (isEnglish) "⚠️ PROHIBITED (ZAWAL)" else "⚠️ মাকরুহ সময় (দ্বিপ্রহর)"
                        activeName = if (isEnglish) "Next: Dhuhr" else "পরবর্তী: যোহর"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.dhuhrStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.dhuhrEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Starts in $remStr" else "শুরু হতে বাকি $remStr"
                    }
                    // 6. Dhuhr time (Dhuhr start to Asr start)
                    curMins in dhuhrS until asrS -> {
                        activePrayerIndex = 1
                        val remMins = asrS - curMins
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (isEnglish) "ACTIVE PRAYER" else "চলমান ওয়াক্ত"
                        activeName = if (isEnglish) "Dhuhr" else "যোহর"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.dhuhrStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.dhuhrEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Ends in $remStr" else "বাকি $remStr"
                    }
                    // 7. Asr time (Asr start to Maghrib start)
                    curMins in asrS until maghribS -> {
                        activePrayerIndex = 2
                        val remMins = maghribS - curMins
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (curMins in sunsetS..sunsetE) {
                            if (isEnglish) "⚠️ MAKRUH (SUNSET)" else "⚠️ মাকরুহ সময় (সূর্যাস্ত পূর্ব)"
                        } else {
                            if (isEnglish) "ACTIVE PRAYER" else "চলমান ওয়াক্ত"
                        }
                        activeName = if (isEnglish) "Asr" else "আসর"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.asrStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.asrEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Ends in $remStr" else "বাকি $remStr"
                    }
                    // 8. Maghrib time (Maghrib start to Isha start)
                    curMins in maghribS until ishaS -> {
                        activePrayerIndex = 3
                        val remMins = ishaS - curMins
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (isEnglish) "ACTIVE PRAYER" else "চলমান ওয়াক্ত"
                        activeName = if (isEnglish) "Maghrib" else "মাগরিব"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.maghribStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.maghribEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Ends in $remStr" else "বাকি $remStr"
                    }
                    // 9. Isha time (Isha start to midnight)
                    else -> {
                        activePrayerIndex = 4
                        val remMins = (1440 - curMins) + tomorrowFajrS
                        val remStr = formatRemTime(remMins, isEnglish)
                        stageLabel = if (isEnglish) "ACTIVE PRAYER" else "চলমান ওয়াক্ত"
                        activeName = if (isEnglish) "Isha" else "এশা"
                        activeWindowStr = "${formatTimeWithAmPm(todayTimes.ishaStart, isEnglish)} – ${formatTimeWithAmPm(todayTimes.ishaEnd, isEnglish)}"
                        countdownStr = if (isEnglish) "Ends in $remStr" else "বাকি $remStr"
                    }
                }

                // Set Header Views: Brand, Dynamic Hijri & Gregorian Dates (Top-Center), Location
                views.setTextViewText(R.id.widget_brand_name, if (isEnglish) "NoorUp" else "নূরআপ")

                // Calculate dynamic Hijri date & Gregorian date
                val dayOffset = prefs.getInt("hijri_day_offset", 0)
                val isBangladesh = HijriDateCalculator.isBangladeshLocation(latitude, longitude, cityName)
                val hijriResult = HijriDateCalculator.calculateHijriDate(cal, dayOffset, isBangladesh)
                val gregorianResult = HijriDateCalculator.calculateGregorianDate(cal)

                val hijriDisplay = if (isEnglish) hijriResult.fullDateEn else hijriResult.fullDateBn
                val gregorianDisplay = if (isEnglish) gregorianResult.fullDateEn else gregorianResult.fullDateBn

                views.setTextViewText(R.id.widget_hijri_date, hijriDisplay)
                views.setTextViewText(R.id.widget_gregorian_date, gregorianDisplay)
                views.setTextViewText(R.id.widget_city, cityName)

                // Bind Left Hero Card
                views.setTextViewText(R.id.widget_active_stage_label, stageLabel)
                views.setTextViewText(R.id.widget_active_name, activeName)
                views.setTextViewText(R.id.widget_active_window, activeWindowStr)
                views.setTextViewText(R.id.widget_active_countdown, countdownStr)

                // Integrated Noor Garden Progress
                val todayDateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
                val todayCompleted = prefs.getInt("prayer_record_${todayDateKey}_completed", 0)
                val todayPercent = (todayCompleted * 100) / 5

                views.setTextViewText(R.id.widget_garden_label, if (isEnglish) "🌱 Noor Garden" else "🌱 নূর বাগান")
                views.setTextViewText(
                    R.id.widget_garden_ratio,
                    if (isEnglish) "$todayCompleted/5" else "${toBanglaDigits(todayCompleted.toString())}/৫"
                )
                views.setProgressBar(R.id.widget_garden_progress, 5, todayCompleted, false)
                val currentStreak = prefs.getInt("current_prayer_streak", 0)
                val streakSubtitle = if (currentStreak > 0) {
                    if (isEnglish) "🔥 $currentStreak d streak • $todayPercent%" else "🔥 ${toBanglaDigits(currentStreak.toString())} দিন স্ট্রিক • ${toBanglaDigits(todayPercent.toString())}%"
                } else {
                    if (isEnglish) "Progress • $todayPercent%" else "অগ্রগতি • ${toBanglaDigits(todayPercent.toString())}%"
                }
                views.setTextViewText(R.id.widget_garden_streak, streakSubtitle)

                // High-End Glassmorphic Color Palette
                val colActive = Color.parseColor("#34D399")
                val colActiveSub = Color.parseColor("#6EE7B7")
                val colInactiveName = Color.parseColor("#9CA3AF")
                val colInactiveStart = Color.parseColor("#E5E7EB")
                val colInactiveDash = Color.parseColor("#6B7280")
                val colInactiveEnd = Color.parseColor("#9CA3AF")

                // Prayer Names (Localized)
                views.setTextViewText(R.id.widget_fajr_name, if (isEnglish) "Fajr" else "ফজর")
                views.setTextViewText(R.id.widget_dhuhr_name, if (isEnglish) "Dhuhr" else "যোহর")
                views.setTextViewText(R.id.widget_asr_name, if (isEnglish) "Asr" else "আসর")
                views.setTextViewText(R.id.widget_maghrib_name, if (isEnglish) "Maghrib" else "মাগরিব")
                views.setTextViewText(R.id.widget_isha_name, if (isEnglish) "Isha" else "এশা")

                // Dual Timestamps (Start Time & End Time) for All 5 Daily Prayers
                // 1. Fajr (Start: fajrStart, End: fajrEnd)
                views.setTextViewText(R.id.widget_fajr_start, formatShortTime(todayTimes.fajrStart, isEnglish))
                views.setTextViewText(R.id.widget_fajr_end, formatTimeWithAmPm(todayTimes.fajrEnd, isEnglish))

                // 2. Dhuhr (Start: dhuhrStart, End: dhuhrEnd)
                views.setTextViewText(R.id.widget_dhuhr_start, formatShortTime(todayTimes.dhuhrStart, isEnglish))
                views.setTextViewText(R.id.widget_dhuhr_end, formatTimeWithAmPm(todayTimes.dhuhrEnd, isEnglish))

                // 3. Asr (Start: asrStart, End: asrEnd)
                views.setTextViewText(R.id.widget_asr_start, formatShortTime(todayTimes.asrStart, isEnglish))
                views.setTextViewText(R.id.widget_asr_end, formatTimeWithAmPm(todayTimes.asrEnd, isEnglish))

                // 4. Maghrib (Start: maghribStart, End: maghribEnd)
                views.setTextViewText(R.id.widget_maghrib_start, formatShortTime(todayTimes.maghribStart, isEnglish))
                views.setTextViewText(R.id.widget_maghrib_end, formatTimeWithAmPm(todayTimes.maghribEnd, isEnglish))

                // 5. Isha (Start: ishaStart, End: ishaEnd)
                views.setTextViewText(R.id.widget_isha_start, formatShortTime(todayTimes.ishaStart, isEnglish))
                views.setTextViewText(R.id.widget_isha_end, formatTimeWithAmPm(todayTimes.ishaEnd, isEnglish))

                // Highlight Active Prayer Row
                // 1. Fajr Row
                val isFajrActive = (activePrayerIndex == 0)
                views.setInt(R.id.widget_fajr_row, "setBackgroundResource", if (isFajrActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
                views.setTextColor(R.id.widget_fajr_name, if (isFajrActive) colActive else colInactiveName)
                views.setTextColor(R.id.widget_fajr_start, if (isFajrActive) colActive else colInactiveStart)
                views.setTextColor(R.id.widget_fajr_end, if (isFajrActive) colActiveSub else colInactiveEnd)

                // 2. Dhuhr Row
                val isDhuhrActive = (activePrayerIndex == 1)
                views.setInt(R.id.widget_dhuhr_row, "setBackgroundResource", if (isDhuhrActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
                views.setTextColor(R.id.widget_dhuhr_name, if (isDhuhrActive) colActive else colInactiveName)
                views.setTextColor(R.id.widget_dhuhr_start, if (isDhuhrActive) colActive else colInactiveStart)
                views.setTextColor(R.id.widget_dhuhr_end, if (isDhuhrActive) colActiveSub else colInactiveEnd)

                // 3. Asr Row
                val isAsrActive = (activePrayerIndex == 2)
                views.setInt(R.id.widget_asr_row, "setBackgroundResource", if (isAsrActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
                views.setTextColor(R.id.widget_asr_name, if (isAsrActive) colActive else colInactiveName)
                views.setTextColor(R.id.widget_asr_start, if (isAsrActive) colActive else colInactiveStart)
                views.setTextColor(R.id.widget_asr_end, if (isAsrActive) colActiveSub else colInactiveEnd)

                // 4. Maghrib Row
                val isMaghribActive = (activePrayerIndex == 3)
                views.setInt(R.id.widget_maghrib_row, "setBackgroundResource", if (isMaghribActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
                views.setTextColor(R.id.widget_maghrib_name, if (isMaghribActive) colActive else colInactiveName)
                views.setTextColor(R.id.widget_maghrib_start, if (isMaghribActive) colActive else colInactiveStart)
                views.setTextColor(R.id.widget_maghrib_end, if (isMaghribActive) colActiveSub else colInactiveEnd)

                // 5. Isha Row
                val isIshaActive = (activePrayerIndex == 4)
                views.setInt(R.id.widget_isha_row, "setBackgroundResource", if (isIshaActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
                views.setTextColor(R.id.widget_isha_name, if (isIshaActive) colActive else colInactiveName)
                views.setTextColor(R.id.widget_isha_start, if (isIshaActive) colActive else colInactiveStart)
                views.setTextColor(R.id.widget_isha_end, if (isIshaActive) colActiveSub else colInactiveEnd)

                // Bind 3 Prohibited / Makruh Times Bar
                views.setTextViewText(
                    R.id.widget_prohibited_label,
                    if (isEnglish) "⚠️ Prohibited:" else "⚠️ নিষিদ্ধ সময়:"
                )

                val sunriseRange = "${formatShortTime(todayTimes.makruhSunriseStart, isEnglish)}–${formatShortTime(todayTimes.makruhSunriseEnd, isEnglish)}"
                val zawalRange = "${formatShortTime(todayTimes.makruhZawalStart, isEnglish)}–${formatShortTime(todayTimes.makruhZawalEnd, isEnglish)}"
                val sunsetRange = "${formatShortTime(todayTimes.makruhSunsetStart, isEnglish)}–${formatShortTime(todayTimes.makruhSunsetEnd, isEnglish)}"

                val isSunriseActive = curMins in sunriseS..sunriseE
                val isZawalActive = curMins in zawalS until zawalE
                val isSunsetActive = curMins in sunsetS..sunsetE

                val sunriseText = if (isEnglish) "Sunrise: $sunriseRange" else "সূর্যোদয়: $sunriseRange"
                val zawalText = if (isEnglish) "Zawal: $zawalRange" else "দ্বিপ্রহর: $zawalRange"
                val sunsetText = if (isEnglish) "Sunset: $sunsetRange" else "সূর্যাস্ত: $sunsetRange"

                views.setTextViewText(R.id.widget_prohibited_sunrise, if (isSunriseActive) "🔥 $sunriseText" else sunriseText)
                views.setTextViewText(R.id.widget_prohibited_zawal, if (isZawalActive) "🔥 $zawalText" else zawalText)
                views.setTextViewText(R.id.widget_prohibited_sunset, if (isSunsetActive) "🔥 $sunsetText" else sunsetText)

                val colProhibitedActive = Color.parseColor("#EF4444")
                val colProhibitedNormal = Color.parseColor("#E5E7EB")

                views.setTextColor(R.id.widget_prohibited_sunrise, if (isSunriseActive) colProhibitedActive else colProhibitedNormal)
                views.setTextColor(R.id.widget_prohibited_zawal, if (isZawalActive) colProhibitedActive else colProhibitedNormal)
                views.setTextColor(R.id.widget_prohibited_sunset, if (isSunsetActive) colProhibitedActive else colProhibitedNormal)

                // Set Open App Intent on widget click
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                // Commit update
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("NoorUpWidget", "Error updating widget $appWidgetId", e)
            }
        }
    }
}
