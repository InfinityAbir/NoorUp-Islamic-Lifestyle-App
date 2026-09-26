package com.example.ui.noorup

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.GoldAccent

/**
 * Rich, Interactive Hijri Hub View:
 * 1. Current Live Hijri Date & Moon Phase Hero Banner
 * 2. Moon Sighting Offset Controller (-2 to +2 days)
 * 3. Full Hijri Month Grid with Gregorian Alignment & Sunnah Ayyam al-Beed Indicators
 * 4. Dynamic Islamic Milestones & Events with live countdowns
 */
@Composable
fun HijriCalendarHubView(
    viewModel: NoorUpViewModel,
    modifier: Modifier = Modifier
) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentHijri by viewModel.currentHijriDate.collectAsState()
    val currentGregorian by viewModel.currentGregorianDate.collectAsState()
    val currentMoonPhase by viewModel.currentMoonPhase.collectAsState()
    val hijriOffset by viewModel.hijriDayOffset.collectAsState()

    val browsingMonth by viewModel.browsingHijriMonth.collectAsState()
    val browsingYear by viewModel.browsingHijriYear.collectAsState()
    val monthGridDays by viewModel.hijriMonthGrid.collectAsState()
    val milestones by viewModel.dynamicIslamicMilestones.collectAsState()

    var selectedDayDetail by remember { mutableStateOf<HijriMonthDay?>(null) }
    var showOffsetDialog by remember { mutableStateOf(false) }

    val browsingMonthName = if (isEnglish) {
        HijriDateCalculator.hijriMonthsEn.getOrElse(browsingMonth - 1) { "Month $browsingMonth" }
    } else {
        HijriDateCalculator.hijriMonthsBn.getOrElse(browsingMonth - 1) { "মাস $browsingMonth" }
    }
    val browsingYearStr = if (isEnglish) "$browsingYear AH" else "${HijriDateCalculator.toBanglaDigits(browsingYear.toString())} হিজরি"

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. HERO CARD: TODAY'S LIVE HIJRI DATE & MOON PHASE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 12.dp else 4.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0xFF10B981).copy(alpha = 0.2f),
                    spotColor = Color(0xFF10B981).copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF064E3B).copy(alpha = 0.85f),
                                Color(0xFF0F172A).copy(alpha = 0.95f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFECFDF5),
                                Color(0xFFD1FAE5).copy(alpha = 0.7f)
                            )
                        )
                    }
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        if (isDark) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF34D399)
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentMoonPhase.moonEmoji,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "TODAY'S HIJRI DATE" else "আজকের হিজরি তারিখ",
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = if (isEnglish) currentHijri.dayOfWeekEn else currentHijri.dayOfWeekBn,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Moon Sighting Adjustment Pill Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.7f) else Color.White)
                            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                            .clickable { showOffsetDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Adjust",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val offsetText = when {
                                hijriOffset > 0 -> "+$hijriOffset"
                                hijriOffset < 0 -> "$hijriOffset"
                                else -> "0"
                            }
                            Text(
                                text = if (isEnglish) "Offset: $offsetText d" else "সমন্বয়: ${if (hijriOffset > 0) "+${hijriOffset.toBanglaDigits()}" else hijriOffset.toBanglaDigits()} দিন",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Prominent Hijri Date Display
                Text(
                    text = if (isEnglish) currentHijri.fullDateEn else currentHijri.fullDateBn,
                    color = if (isDark) Color.White else Color(0xFF064E3B),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location-Aware Standard Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (currentHijri.isBangladeshStandard) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(
                            0.8.dp,
                            if (currentHijri.isBangladeshStandard) Color(0xFF10B981).copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isEnglish) currentHijri.standardLabelEn else currentHijri.standardLabelBn,
                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Gregorian Equivalent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF047857),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Gregorian: ${currentGregorian.fullDateEn}" else "ইংরেজি তারিখ: ${currentGregorian.fullDateBn}",
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Moon Phase Info Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF022C22).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f))
                        .border(1.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.3f) else Color(0xFFA7F3D0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentMoonPhase.moonEmoji,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) currentMoonPhase.phaseNameEn else currentMoonPhase.phaseNameBn,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isEnglish) "${currentMoonPhase.illuminationPercent}% Illuminated" else "${currentMoonPhase.illuminationPercent.toBanglaDigits()}% আলোকিত",
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- 2. HIJRI MONTH CALENDAR GRID ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 4.dp else 2.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color.Black.copy(alpha = 0.1f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column {
                // Month Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.prevBrowsingHijriMonth() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.resetBrowsingHijriToCurrent() }
                    ) {
                        Text(
                            text = "$browsingMonthName $browsingYearStr",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (browsingMonth != currentHijri.month || browsingYear != currentHijri.year) {
                            Text(
                                text = if (isEnglish) "(Tap to return to today)" else "(আজকের মাসে ফিরতে চাপুন)",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.nextBrowsingHijriMonth() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sunnah Ayyam al-Beed Indicator Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "Ayyam al-Beed (13, 14, 15 Sunnah Fast)" else "আইয়ামে বীজ (১৩, ১৪, ১৫ সুন্নাত রোজা)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "Today" else "আজ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7 Day of Week Column Headers
                val dowHeaders = if (isEnglish) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                } else {
                    listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dowHeaders.forEachIndexed { idx, name ->
                        val isFri = idx == 5
                        Text(
                            text = name,
                            color = if (isFri) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = if (isFri) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid (Up to 30 days displayed with leading empty offset)
                if (monthGridDays.isNotEmpty()) {
                    val firstDay = monthGridDays.first()
                    val leadingEmptySlots = (firstDay.dayOfWeek - 1) % 7

                    val totalSlots = leadingEmptySlots + monthGridDays.size
                    val rowCount = (totalSlots + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in 0 until rowCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0 until 7) {
                                    val slotIndex = r * 7 + c
                                    val dayIndex = slotIndex - leadingEmptySlots

                                    if (dayIndex in monthGridDays.indices) {
                                        val dayItem = monthGridDays[dayIndex]
                                        val isSelected = selectedDayDetail?.hijriDay == dayItem.hijriDay

                                        val cellBg = when {
                                            dayItem.isToday -> if (isDark) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFD1FAE5)
                                            dayItem.isAyyamAlBeed -> if (isDark) GoldAccent.copy(alpha = 0.2f) else Color(0xFFFEF3C7)
                                            dayItem.isFriday -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                            else -> if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color.White
                                        }

                                        val cellBorder = when {
                                            dayItem.isToday -> Color(0xFF10B981)
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            dayItem.isAyyamAlBeed -> GoldAccent.copy(alpha = 0.8f)
                                            else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(cellBg)
                                                .border(if (dayItem.isToday || isSelected) 1.5.dp else 1.dp, cellBorder, RoundedCornerShape(10.dp))
                                                .clickable { selectedDayDetail = dayItem }
                                                .padding(3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                // Hijri Day (Bold)
                                                Text(
                                                    text = if (isEnglish) "${dayItem.hijriDay}" else dayItem.hijriDay.toBanglaDigits(),
                                                    color = when {
                                                        dayItem.isToday -> if (isDark) Color.White else Color(0xFF064E3B)
                                                        dayItem.isAyyamAlBeed -> if (isDark) GoldAccent else Color(0xFFD97706)
                                                        dayItem.isFriday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    },
                                                    fontSize = 13.sp,
                                                    fontWeight = if (dayItem.isToday || dayItem.isAyyamAlBeed) FontWeight.ExtraBold else FontWeight.Bold
                                                )
                                                // Gregorian Day (Subscript)
                                                Text(
                                                    text = if (isEnglish) "${dayItem.gregorianDay}" else dayItem.gregorianDay.toBanglaDigits(),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    fontSize = 9.sp
                                                )
                                                if (dayItem.specialEventBn != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Empty Slot
                                        Spacer(modifier = Modifier.weight(1f).height(52.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Details Card (if tapped)
        selectedDayDetail?.let { day ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF0FDF4))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val hMonthName = if (isEnglish) HijriDateCalculator.hijriMonthsEn[day.hijriMonth - 1] else HijriDateCalculator.hijriMonthsBn[day.hijriMonth - 1]
                        val gMonthName = if (isEnglish) HijriDateCalculator.gregorianMonthsEn[day.gregorianMonth - 1] else HijriDateCalculator.gregorianMonthsBn[day.gregorianMonth - 1]

                        Text(
                            text = if (isEnglish) "${day.hijriDay} $hMonthName ${day.hijriYear} AH" else "${day.hijriDay.toBanglaDigits()} $hMonthName ${day.hijriYear.toBanglaDigits()} হিজরি",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) "Gregorian: ${day.gregorianDay} $gMonthName ${day.gregorianYear}" else "ইংরেজি: ${day.gregorianDay.toBanglaDigits()} $gMonthName ${day.gregorianYear.toBanglaDigits()}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        if (day.isAyyamAlBeed) {
                            Text(
                                text = if (isEnglish) "⭐ Ayyam al-Beed: Recommended Sunnah Fasting Day" else "⭐ আইয়ামে বীজ: বরকতময় সুন্নাত রোজা রাখার দিন",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (day.specialEventBn != null) {
                            Text(
                                text = "🎉 ${if (isEnglish) day.specialEventEn else day.specialEventBn}",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = { selectedDayDetail = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // --- 3. DYNAMIC UPCOMING ISLAMIC MILESTONES ---
        Text(
            text = if (isEnglish) "🌙 Upcoming Islamic Milestones & Events" else "🌙 আসন্ন গুরুত্বপূর্ণ ইসলামি দিবস ও উৎসব",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        milestones.forEach { milestone ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 4.dp else 1.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnglish) milestone.titleEn else milestone.titleBn,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEnglish) milestone.hijriDateStrEn else milestone.hijriDateStrBn,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isEnglish) milestone.descriptionEn else milestone.descriptionBn,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Days remaining badge
                    val isToday = milestone.daysRemaining == 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isToday) Color(0xFF10B981).copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isToday) Color(0xFF10B981)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isToday -> if (isEnglish) "Today" else "আজ"
                                milestone.daysRemaining == 1 -> if (isEnglish) "Tomorrow" else "আগামীকাল"
                                isEnglish -> "${milestone.daysRemaining} days left"
                                else -> "${milestone.daysRemaining.toBanglaDigits()} দিন বাকি"
                            },
                            color = if (isToday) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // --- MOON SIGHTING OFFSET ADJUSTMENT DIALOG ---
    if (showOffsetDialog) {
        AlertDialog(
            onDismissRequest = { showOffsetDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isEnglish) "Moon Sighting Adjustment" else "চাঁদ দেখা অনুযায়ী দিন সমন্বয়",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (isEnglish) "Islamic dates depend on local crescent moon sighting. If your local Islamic authority (e.g. Islamic Foundation) differs by a day, adjust the offset below:"
                        else "ইসলামিক তারিখ স্থানীয় নতুন চাঁদ দেখার উপর নির্ভরশীল। ইসলামিক ফাউন্ডেশন বা স্থানীয় চাঁদ দেখা কমিটির ঘোষণার সাথে মিল রাখতে নিচে দিন সমন্বয় করুন:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    val offsetOptions = listOf(-2, -1, 0, 1, 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        offsetOptions.forEach { opt ->
                            val isSelected = opt == hijriOffset
                            val label = when {
                                opt > 0 -> "+$opt"
                                opt < 0 -> "$opt"
                                else -> "0 (Default)"
                            }
                            val labelBn = when {
                                opt > 0 -> "+${opt.toBanglaDigits()} দিন"
                                opt < 0 -> "${opt.toBanglaDigits()} দিন"
                                else -> "০ (ডিফল্ট)"
                            }
                            Button(
                                onClick = {
                                    viewModel.setHijriDayOffset(opt)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isEnglish) label else labelBn,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isEnglish) "Current: ${currentHijri.fullDateEn}" else "বর্তমান তারিখ: ${currentHijri.fullDateBn}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOffsetDialog = false }) {
                    Text(if (isEnglish) "Done" else "সম্পন্ন", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
