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

        private fun parseTimeToMins(timeStr: String): Int {
            return try {
                val clean = timeStr.trim().uppercase()
                val isPm = clean.contains("PM")
                val timePart = clean.replace("AM", "").replace("PM", "").trim()
                val parts = timePart.split(":")
                var hour = parts[0].trim().toInt()
                val min = parts[1].trim().toInt()
                if (isPm && hour != 12) hour += 12
                if (!isPm && hour == 12) hour = 0
                hour * 60 + min
            } catch (e: Exception) {
                0
            }
        }

        private fun formatShortTime(timeStr: String, isEnglish: Boolean): String {
            return try {
                val clean = timeStr.trim().uppercase()
                val timePart = clean.replace("AM", "").replace("PM", "").trim()
                val parts = timePart.split(":")
                val h = parts[0].trim().toInt()
                val m = parts[1].trim().toInt()
                val hStr = if (h < 10) "0$h" else "$h"
                val mStr = if (m < 10) "0$m" else "$m"
                if (isEnglish) "$hStr:$mStr" else "${toBanglaDigits(hStr)}:${toBanglaDigits(mStr)}"
            } catch (e: Exception) {
                timeStr
            }
        }

        private fun formatTimeWithAmPm(timeStr: String, isEnglish: Boolean): String {
            return try {
                val clean = timeStr.trim().uppercase()
                val isPm = clean.contains("PM")
                val timePart = clean.replace("AM", "").replace("PM", "").trim()
                val parts = timePart.split(":")
                val h = parts[0].trim().toInt()
                val m = parts[1].trim().toInt()
                val suffix = if (isPm) "PM" else "AM"
                val hStr = if (h < 10) "0$h" else "$h"
                val mStr = if (m < 10) "0$m" else "$m"
                if (isEnglish) "$hStr:$mStr $suffix" else "${toBanglaDigits(hStr)}:${toBanglaDigits(mStr)} $suffix"
            } catch (e: Exception) {
                timeStr
            }
        }

        private fun formatDualTime(timeStr: String, isEnglish: Boolean): String {
            return formatTimeWithAmPm(timeStr, isEnglish)
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

            // Current Time and Date
            val cal = Calendar.getInstance()
            val curHour = cal.get(Calendar.HOUR_OF_DAY)
            val curMin = cal.get(Calendar.MINUTE)
            val curMins = curHour * 60 + curMin

            val banglaMonths = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
            val englishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val monthIdx = cal.get(Calendar.MONTH)
            val dateStr = if (isEnglish) {
                "${englishMonths[monthIdx]} $dayOfMonth"
            } else {
                "${toBanglaDigits(dayOfMonth.toString())} ${banglaMonths[monthIdx]}"
            }

            // Calculate solar astronomical prayer times
            val solarTimes = SolarPrayerEngine.calculateTimes(
                latitude = latitude,
                longitude = longitude,
                calendar = cal,
                timezoneOffset = 6.0,
                method = CalculationMethod.KARACHI,
                juristic = JuristicMethod.HANAFI
            )

            val fajrS = parseTimeToMins(solarTimes.fajrStart)
            val fajrE = parseTimeToMins(solarTimes.sunrise)
            val sunriseS = parseTimeToMins(solarTimes.sunrise)
            val sunriseE = sunriseS + 15
            val zawalS = parseTimeToMins(solarTimes.makruhZawalStart)
            val zawalE = parseTimeToMins(solarTimes.makruhZawalEnd)
            val dhuhrS = parseTimeToMins(solarTimes.dhuhrStart)
            val dhuhrE = parseTimeToMins(solarTimes.asrStart)
            val asrS = parseTimeToMins(solarTimes.asrStart)
            val asrE = parseTimeToMins(solarTimes.maghribStart)
            val sunsetS = asrE - 15
            val sunsetE = asrE
            val maghribS = parseTimeToMins(solarTimes.maghribStart)
            val maghribE = parseTimeToMins(solarTimes.ishaStart)
            val ishaS = parseTimeToMins(solarTimes.ishaStart)

            // Determine active prayer state & remaining time
            var activePrayerIndex = -1 // 0: Fajr, 1: Dhuhr, 2: Asr, 3: Maghrib, 4: Isha
            var statusText = ""
            var headerTitleColor = Color.parseColor("#34D399") // Vibrant Emerald

            fun formatRemTime(remMins: Int): String {
                val clamped = remMins.coerceAtLeast(0)
                val hrs = clamped / 60
                val mins = clamped % 60
                return if (isEnglish) {
                    if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                } else {
                    if (hrs > 0) "${toBanglaDigits(hrs.toString())} ঘ. ${toBanglaDigits(mins.toString())} মি." else "${toBanglaDigits(mins.toString())} মি."
                }
            }

            when {
                curMins in sunriseS..sunriseE -> {
                    activePrayerIndex = 1 // Next is Dhuhr
                    val rem = (sunriseE - curMins).coerceAtLeast(0)
                    statusText = if (isEnglish) "⚠️ Makruh (Sunrise • ${formatRemTime(rem)})" else "⚠️ মাকরুহ (সূর্যোদয় • ${formatRemTime(rem)})"
                    headerTitleColor = Color.parseColor("#FBBF24")
                }
                curMins in zawalS..zawalE -> {
                    activePrayerIndex = 1 // Next is Dhuhr
                    val rem = (zawalE - curMins).coerceAtLeast(0)
                    statusText = if (isEnglish) "⚠️ Makruh (Zawal • ${formatRemTime(rem)})" else "⚠️ মাকরুহ (দ্বিপ্রহর • ${formatRemTime(rem)})"
                    headerTitleColor = Color.parseColor("#FBBF24")
                }
                curMins in sunsetS..sunsetE -> {
                    activePrayerIndex = 3 // Next is Maghrib
                    val rem = (sunsetE - curMins).coerceAtLeast(0)
                    statusText = if (isEnglish) "⚠️ Makruh (Sunset • ${formatRemTime(rem)})" else "⚠️ মাকরুহ (সূর্যাস্ত পূর্ব • ${formatRemTime(rem)})"
                    headerTitleColor = Color.parseColor("#FBBF24")
                }
                curMins in fajrS until sunriseS -> {
                    activePrayerIndex = 0
                    val remStr = formatRemTime(fajrE - curMins)
                    statusText = if (isEnglish) "Fajr (ends in $remStr)" else "ফজর (বাকি $remStr)"
                    headerTitleColor = Color.parseColor("#34D399")
                }
                curMins in (sunriseE + 1) until zawalS -> {
                    activePrayerIndex = 1 // Chasht/Duha, upcoming is Dhuhr
                    val remStr = formatRemTime(dhuhrS - curMins)
                    statusText = if (isEnglish) "Duha • Next: Dhuhr ($remStr)" else "চাশত • পরবর্তী: যোহর ($remStr)"
                    headerTitleColor = Color.parseColor("#34D399")
                }
                curMins in dhuhrS until asrS -> {
                    activePrayerIndex = 1
                    val remStr = formatRemTime(dhuhrE - curMins)
                    statusText = if (isEnglish) "Dhuhr (ends in $remStr)" else "যোহর (বাকি $remStr)"
                    headerTitleColor = Color.parseColor("#34D399")
                }
                curMins in asrS until sunsetS -> {
                    activePrayerIndex = 2
                    val remStr = formatRemTime(asrE - curMins)
                    statusText = if (isEnglish) "Asr (ends in $remStr)" else "আসর (বাকি $remStr)"
                    headerTitleColor = Color.parseColor("#34D399")
                }
                curMins in maghribS until ishaS -> {
                    activePrayerIndex = 3
                    val remStr = formatRemTime(maghribE - curMins)
                    statusText = if (isEnglish) "Maghrib (ends in $remStr)" else "মাগরিব (বাকি $remStr)"
                    headerTitleColor = Color.parseColor("#34D399")
                }
                curMins >= ishaS -> {
                    activePrayerIndex = 4
                    val remMins = (1440 - curMins) + fajrS
                    val remStr = formatRemTime(remMins)
                    statusText = if (isEnglish) "Isha (ends in $remStr)" else "এশা (বাকি $remStr)"
                    headerTitleColor = Color.parseColor("#34D399")
                }
                else -> {
                    // Between midnight and Fajr start
                    activePrayerIndex = 4
                    val remStr = formatRemTime(fajrS - curMins)
                    statusText = if (isEnglish) "Tahajjud • Fajr ($remStr)" else "তাহাজ্জুদ • ফজর বাকি ($remStr)"
                    headerTitleColor = Color.parseColor("#34D399")
                }
            }

            // Set Header Views: Brand, Dynamic Hijri & Gregorian Dates (Top-Center), Location
            views.setTextViewText(R.id.widget_brand_name, if (isEnglish) "NoorUp" else "নূরআপ")

            // Calculate dynamic Hijri date & Gregorian date
            val dayOffset = prefs.getInt("hijri_day_offset", 0)
            val hijriResult = HijriDateCalculator.calculateHijriDate(cal, dayOffset)
            val gregorianResult = HijriDateCalculator.calculateGregorianDate(cal)

            val hijriDisplay = if (isEnglish) hijriResult.fullDateEn else hijriResult.fullDateBn
            val gregorianDisplay = if (isEnglish) gregorianResult.fullDateEn else gregorianResult.fullDateBn

            views.setTextViewText(R.id.widget_hijri_date, hijriDisplay)
            views.setTextViewText(R.id.widget_gregorian_date, gregorianDisplay)

            views.setTextViewText(R.id.widget_city, cityName)

            // Determine active prayer details for the Hero Card
            val activeNameEn = when (activePrayerIndex) {
                0 -> "Fajr"
                1 -> "Dhuhr"
                2 -> "Asr"
                3 -> "Maghrib"
                else -> "Isha"
            }
            val activeNameBn = when (activePrayerIndex) {
                0 -> "ফজর"
                1 -> "যোহর"
                2 -> "আসর"
                3 -> "মাগরিব"
                else -> "এশা"
            }
            val activeName = if (isEnglish) activeNameEn else activeNameBn
            val activeCountdownStr = when {
                curMins in fajrS until sunriseS -> formatRemTime(fajrE - curMins)
                curMins in dhuhrS until asrS -> formatRemTime(dhuhrE - curMins)
                curMins in asrS until sunsetS -> formatRemTime(asrE - curMins)
                curMins in maghribS until ishaS -> formatRemTime(maghribE - curMins)
                curMins >= ishaS -> formatRemTime((1440 - curMins) + fajrS)
                else -> formatRemTime((fajrS - curMins).coerceAtLeast(0))
            }

            // Bind Left Hero Card
            val activeWindowStr = when (activePrayerIndex) {
                0 -> "${formatDualTime(solarTimes.fajrStart, isEnglish)} – ${formatDualTime(solarTimes.sunrise, isEnglish)}"
                1 -> "${formatDualTime(solarTimes.dhuhrStart, isEnglish)} – ${formatDualTime(solarTimes.asrStart, isEnglish)}"
                2 -> "${formatDualTime(solarTimes.asrStart, isEnglish)} – ${formatDualTime(solarTimes.maghribStart, isEnglish)}"
                3 -> "${formatDualTime(solarTimes.maghribStart, isEnglish)} – ${formatDualTime(solarTimes.ishaStart, isEnglish)}"
                else -> "${formatDualTime(solarTimes.ishaStart, isEnglish)} – ${formatDualTime(solarTimes.fajrStart, isEnglish)}"
            }
            views.setTextViewText(R.id.widget_active_stage_label, if (isEnglish) "ACTIVE PRAYER" else "চলমান ওয়াক্ত")
            views.setTextViewText(R.id.widget_active_name, activeName)
            views.setTextViewText(R.id.widget_active_window, activeWindowStr)
            views.setTextViewText(R.id.widget_active_countdown, if (isEnglish) "Ends in $activeCountdownStr" else "বাকি $activeCountdownStr")

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
            // 1. Fajr (Start: fajrStart, End: sunrise)
            views.setTextViewText(R.id.widget_fajr_start, formatShortTime(solarTimes.fajrStart, isEnglish))
            views.setTextViewText(R.id.widget_fajr_end, formatTimeWithAmPm(solarTimes.sunrise, isEnglish))

            // 2. Dhuhr (Start: dhuhrStart, End: asrStart)
            views.setTextViewText(R.id.widget_dhuhr_start, formatShortTime(solarTimes.dhuhrStart, isEnglish))
            views.setTextViewText(R.id.widget_dhuhr_end, formatTimeWithAmPm(solarTimes.asrStart, isEnglish))

            // 3. Asr (Start: asrStart, End: maghribStart)
            views.setTextViewText(R.id.widget_asr_start, formatShortTime(solarTimes.asrStart, isEnglish))
            views.setTextViewText(R.id.widget_asr_end, formatTimeWithAmPm(solarTimes.maghribStart, isEnglish))

            // 4. Maghrib (Start: maghribStart, End: ishaStart)
            views.setTextViewText(R.id.widget_maghrib_start, formatShortTime(solarTimes.maghribStart, isEnglish))
            views.setTextViewText(R.id.widget_maghrib_end, formatTimeWithAmPm(solarTimes.ishaStart, isEnglish))

            // 5. Isha (Start: ishaStart, End: fajrStart / Dawn)
            views.setTextViewText(R.id.widget_isha_start, formatShortTime(solarTimes.ishaStart, isEnglish))
            views.setTextViewText(R.id.widget_isha_end, formatTimeWithAmPm(solarTimes.fajrStart, isEnglish))

            // Highlight Active Prayer Row
            // 1. Fajr Row
            val isFajrActive = (activePrayerIndex == 0)
            views.setInt(R.id.widget_fajr_box, "setBackgroundResource", if (isFajrActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
            views.setImageViewResource(R.id.widget_fajr_indicator, if (isFajrActive) R.drawable.widget_indicator_active else R.drawable.widget_indicator_inactive)
            views.setTextColor(R.id.widget_fajr_name, if (isFajrActive) colActive else colInactiveName)
            views.setTextColor(R.id.widget_fajr_start, if (isFajrActive) colActive else colInactiveStart)
            views.setTextColor(R.id.widget_fajr_dash, if (isFajrActive) colActive else colInactiveDash)
            views.setTextColor(R.id.widget_fajr_end, if (isFajrActive) colActiveSub else colInactiveEnd)

            // 2. Dhuhr Row
            val isDhuhrActive = (activePrayerIndex == 1)
            views.setInt(R.id.widget_dhuhr_box, "setBackgroundResource", if (isDhuhrActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
            views.setImageViewResource(R.id.widget_dhuhr_indicator, if (isDhuhrActive) R.drawable.widget_indicator_active else R.drawable.widget_indicator_inactive)
            views.setTextColor(R.id.widget_dhuhr_name, if (isDhuhrActive) colActive else colInactiveName)
            views.setTextColor(R.id.widget_dhuhr_start, if (isDhuhrActive) colActive else colInactiveStart)
            views.setTextColor(R.id.widget_dhuhr_dash, if (isDhuhrActive) colActive else colInactiveDash)
            views.setTextColor(R.id.widget_dhuhr_end, if (isDhuhrActive) colActiveSub else colInactiveEnd)

            // 3. Asr Row
            val isAsrActive = (activePrayerIndex == 2)
            views.setInt(R.id.widget_asr_box, "setBackgroundResource", if (isAsrActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
            views.setImageViewResource(R.id.widget_asr_indicator, if (isAsrActive) R.drawable.widget_indicator_active else R.drawable.widget_indicator_inactive)
            views.setTextColor(R.id.widget_asr_name, if (isAsrActive) colActive else colInactiveName)
            views.setTextColor(R.id.widget_asr_start, if (isAsrActive) colActive else colInactiveStart)
            views.setTextColor(R.id.widget_asr_dash, if (isAsrActive) colActive else colInactiveDash)
            views.setTextColor(R.id.widget_asr_end, if (isAsrActive) colActiveSub else colInactiveEnd)

            // 4. Maghrib Row
            val isMaghribActive = (activePrayerIndex == 3)
            views.setInt(R.id.widget_maghrib_box, "setBackgroundResource", if (isMaghribActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
            views.setImageViewResource(R.id.widget_maghrib_indicator, if (isMaghribActive) R.drawable.widget_indicator_active else R.drawable.widget_indicator_inactive)
            views.setTextColor(R.id.widget_maghrib_name, if (isMaghribActive) colActive else colInactiveName)
            views.setTextColor(R.id.widget_maghrib_start, if (isMaghribActive) colActive else colInactiveStart)
            views.setTextColor(R.id.widget_maghrib_dash, if (isMaghribActive) colActive else colInactiveDash)
            views.setTextColor(R.id.widget_maghrib_end, if (isMaghribActive) colActiveSub else colInactiveEnd)

            // 5. Isha Row
            val isIshaActive = (activePrayerIndex == 4)
            views.setInt(R.id.widget_isha_box, "setBackgroundResource", if (isIshaActive) R.drawable.widget_timeline_item_active else R.drawable.widget_timeline_item_normal)
            views.setImageViewResource(R.id.widget_isha_indicator, if (isIshaActive) R.drawable.widget_indicator_active else R.drawable.widget_indicator_inactive)
            views.setTextColor(R.id.widget_isha_name, if (isIshaActive) colActive else colInactiveName)
            views.setTextColor(R.id.widget_isha_start, if (isIshaActive) colActive else colInactiveStart)
            views.setTextColor(R.id.widget_isha_dash, if (isIshaActive) colActive else colInactiveDash)
            views.setTextColor(R.id.widget_isha_end, if (isIshaActive) colActiveSub else colInactiveEnd)

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

