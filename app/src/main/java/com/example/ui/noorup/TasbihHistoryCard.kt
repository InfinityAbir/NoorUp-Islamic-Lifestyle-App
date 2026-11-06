package com.example.ui.noorup

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val PAGE_DAYS = 14

/**
 * Zikr history for the Digital Tasbih screen: today / last 7 days / total, a 7-day chart,
 * a per-dhikr breakdown and a day-by-day log. Reads the same store the widget writes to.
 *
 * [refreshKey] changes whenever the live count changes so today's numbers stay current.
 */
@Composable
fun TasbihHistoryCard(isEnglish: Boolean, refreshKey: Any) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE) }
    val today = remember { LocalDate.now() }

    // Older days never change while the screen is open: load them once. Today is reloaded per tap.
    val older = remember(today) { TasbihHistory.loadRange(prefs, today.minusDays(1), 89) }
    val todayLog = remember(refreshKey, today) { TasbihHistory.loadDay(prefs, today) }
    val days = remember(older, todayLog) { listOf(todayLog) + older }
    val totals = remember(refreshKey, older) { TasbihHistory.totals(prefs) }

    val week = days.take(7)
    val weekTotal = week.sumOf { it.total }
    var visibleDays by rememberSaveable { mutableStateOf(PAGE_DAYS) }

    fun n(v: Int) = if (isEnglish) "$v" else v.toBanglaDigits()

    GlassCard {
        Text(
            if (isEnglish) "Zikr History" else "যিকিরের ইতিহাস",
            color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
        Text(
            if (isEnglish) "Every count is saved on this device, including counts made from the home-screen widget."
            else "প্রতিটি গণনা এই ডিভাইসে সংরক্ষিত হয়, হোম স্ক্রিন উইজেটের গণনাসহ।",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp
        )
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(if (isEnglish) "Today" else "আজ", n(todayLog.total), Modifier.weight(1f))
            StatTile(if (isEnglish) "Last 7 days" else "গত ৭ দিন", n(weekTotal), Modifier.weight(1f))
            StatTile(if (isEnglish) "All recorded" else "মোট রেকর্ড", n(totals.first), Modifier.weight(1f))
        }

        if (totals.first == 0) {
            Spacer(Modifier.height(14.dp))
            Text(
                if (isEnglish) "No zikr recorded yet. Your counts will appear here as you use the tasbih."
                else "এখনো কোনো যিকির রেকর্ড হয়নি। তসবীহ ব্যবহার করলে গণনাগুলো এখানে দেখা যাবে।",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            return@GlassCard
        }

        Spacer(Modifier.height(16.dp))
        Text(
            if (isEnglish) "Last 7 days" else "গত ৭ দিন",
            color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        WeekBars(week.reversed(), isEnglish)

        // Per-dhikr breakdown for the week.
        val byDhikr = week.flatMap { it.entries }
            .groupBy { it.id }
            .map { (_, list) -> list.first().title(isEnglish) to list.sumOf { it.count } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
        if (byDhikr.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                if (isEnglish) "By zikr this week" else "এই সপ্তাহের যিকির",
                color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            val top = byDhikr.first().second.coerceAtLeast(1)
            byDhikr.take(5).forEach { (title, count) ->
                Column(Modifier.padding(vertical = 3.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(n(count), color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { count.toFloat() / top },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Day-by-day log (only days with activity).
        val log = days.filter { !it.isEmpty }
        Spacer(Modifier.height(16.dp))
        Text(
            if (isEnglish) "Daily log" else "দিনভিত্তিক হিসাব",
            color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        if (log.isEmpty()) {
            Text(
                if (isEnglish) "Nothing recorded in the last 90 days." else "গত ৯০ দিনে কিছু রেকর্ড নেই।",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
            )
        } else {
            val shown = log.filter { !it.date.isBefore(today.minusDays(visibleDays.toLong() - 1)) }
            shown.forEach { day -> DayRow(day, today, isEnglish) }
            if (log.size > shown.size) {
                TextButton(onClick = { visibleDays += PAGE_DAYS * 2 }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (isEnglish) "Show earlier days" else "আগের দিনগুলো দেখুন")
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WeekBars(daysOldestFirst: List<DhikrDay>, isEnglish: Boolean) {
    val max = (daysOldestFirst.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    val locale = if (isEnglish) Locale.ENGLISH else Locale("bn")
    Row(
        modifier = Modifier.fillMaxWidth().height(132.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        daysOldestFirst.forEachIndexed { index, day ->
            val isToday = index == daysOldestFirst.lastIndex
            val label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            val desc = if (isEnglish) "${day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)}: ${day.total} counts"
            else "${day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)}: ${day.total.toBanglaDigits()} বার"
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = desc },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    if (day.total == 0) "" else if (isEnglish) "${day.total}" else day.total.toBanglaDigits(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1, softWrap = false
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.fillMaxWidth().height((6 + 56f * day.total / max).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (day.total == 0) MaterialTheme.colorScheme.surfaceVariant
                            else if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label, fontSize = 10.sp, maxLines = 1, softWrap = false,
                    color = if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun DayRow(day: DhikrDay, today: LocalDate, isEnglish: Boolean) {
    val locale = if (isEnglish) Locale.ENGLISH else Locale("bn")
    val label = when (day.date) {
        today -> if (isEnglish) "Today" else "আজ"
        today.minusDays(1) -> if (isEnglish) "Yesterday" else "গতকাল"
        else -> {
            val d = if (isEnglish) "${day.date.dayOfMonth}" else day.date.dayOfMonth.toBanglaDigits()
            "$d ${day.date.month.getDisplayName(TextStyle.SHORT, locale)}"
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                (if (isEnglish) "${day.total}" else day.total.toBanglaDigits()) +
                    if (day.rounds > 0) (if (isEnglish) " • ${day.rounds} round${if (day.rounds > 1) "s" else ""}" else " • ${day.rounds.toBanglaDigits()} রাউন্ড") else "",
                color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            day.entries.sortedByDescending { it.count }.joinToString("  •  ") {
                "${it.title(isEnglish)} ×${if (isEnglish) "${it.count}" else it.count.toBanglaDigits()}"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp
        )
        HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
}
