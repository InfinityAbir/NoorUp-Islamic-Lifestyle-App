package com.example.ui.noorup.journey

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.toBanglaDigits
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------ formatting

fun jNum(n: Int, en: Boolean): String = if (en) n.toString() else n.toBanglaDigits()
fun jPct(p: Int?, en: Boolean): String = when {
    p == null -> "—"
    en -> "$p%"
    else -> "${p.toBanglaDigits()}%"
}

private val BN_MONTHS_FULL = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
private val EN_MONTHS_FULL = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val BN_MONTHS_SHORT = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন", "জুলা", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
private val EN_MONTHS_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val EN_DOW = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val BN_DOW = listOf("সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি", "রবি")

/** Bangla digits plus Bangla month and weekday names for text produced with Locale.US patterns. */
fun toBanglaDateText(text: String): String {
    var t = text
    EN_MONTHS_FULL.forEachIndexed { i, m -> t = t.replace(Regex("\\b$m\\b"), BN_MONTHS_FULL[i]) }
    EN_MONTHS_SHORT.forEachIndexed { i, m -> t = t.replace(Regex("\\b$m\\b"), BN_MONTHS_SHORT[i]) }
    EN_DOW.forEachIndexed { i, d -> t = t.replace(Regex("\\b$d\\b"), BN_DOW[i]) }
    return t.toBanglaDigits()
}

fun jDate(date: LocalDate, en: Boolean): String {
    val t = date.format(DateTimeFormatter.ofPattern("dd MMM", Locale.US))
    return if (en) t else toBanglaDateText(t)
}

fun jDateLong(date: LocalDate, en: Boolean): String {
    val t = date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.US))
    return if (en) t else toBanglaDateText(t)
}

fun jMonth(month: YearMonth, en: Boolean): String {
    val t = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    return if (en) t else toBanglaDateText(t)
}

fun jTime(millis: Long, en: Boolean): String {
    val t = SimpleDateFormat("hh:mm a", Locale.US).format(Date(millis))
    return if (en) t else t.toBanglaDigits()
}

fun jDateTime(millis: Long, en: Boolean): String {
    val t = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(millis))
    return if (en) t else toBanglaDateText(t)
}

/** Signed change in points, neutral wording on purpose ("+4 points", never "better"/"worse"). */
fun jDelta(delta: Int, en: Boolean): String {
    val sign = if (delta > 0) "+" else if (delta < 0) "−" else "±"
    val n = kotlin.math.abs(delta)
    return if (en) "$sign$n ${if (n == 1) "point" else "points"}" else "$sign${n.toBanglaDigits()} পয়েন্ট"
}

fun prayerDetailLine(source: PrayerRecordSource, date: LocalDate, prayer: Prayer, status: PrayerStatus, en: Boolean): String {
    val base = when (status) {
        PrayerStatus.COMPLETED -> source.completedAt(date, prayer)?.let {
            if (en) "Completed at ${jTime(it, true)}" else "আদায় হয়েছে ${jTime(it, false)}"
        } ?: if (en) "Completed (time not recorded)" else "আদায় হয়েছে (সময় রেকর্ড নেই)"
        else -> statusLabel(status, en).replaceFirstChar { it.uppercase() }
    }
    val changed = source.modifiedAt(date, prayer)?.let {
        if (en) " · changed ${jDateTime(it, true)}" else " · পরিবর্তন ${jDateTime(it, false)}"
    } ?: ""
    return base + changed
}

// ------------------------------------------------------------------ editor

/**
 * Three-way prayer record control: Completed / Missed / Not recorded.
 * "Missed" is only ever set by the user; nothing in the app infers it.
 */
@Composable
fun PrayerRecordEditor(
    status: PrayerStatus,
    isEnglish: Boolean,
    onChoose: (RecordChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = when (status) {
        PrayerStatus.COMPLETED -> RecordChoice.COMPLETED
        PrayerStatus.CONFIRMED_MISSED -> RecordChoice.MISSED
        else -> RecordChoice.UNRECORDED
    }
    val options = listOf(
        RecordChoice.COMPLETED to (if (isEnglish) "Completed" else "আদায়"),
        RecordChoice.MISSED to (if (isEnglish) "Missed" else "ছুটে গেছে"),
        RecordChoice.UNRECORDED to (if (isEnglish) "Not recorded" else "রেকর্ড নেই")
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (choice, label) ->
            val on = choice == selected
            val enabled = choice != RecordChoice.MISSED || status != PrayerStatus.UPCOMING
            val shape = RoundedCornerShape(12.dp)
            val itemModifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .semantics { this.selected = on }
            val padding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            if (on) {
                Button(
                    onClick = {},
                    enabled = enabled,
                    modifier = itemModifier,
                    shape = shape,
                    contentPadding = padding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2) }
            } else {
                OutlinedButton(
                    onClick = { onChoose(choice) },
                    enabled = enabled,
                    modifier = itemModifier,
                    shape = shape,
                    contentPadding = padding,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) { Text(label, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2, color = MaterialTheme.colorScheme.onSurface) }
            }
        }
    }
}

/** Five prayer rows with the three-way editor, for one date. Used by the day-edit dialog and Daily history. */
@Composable
fun DayRecordEditorList(viewModel: NoorUpViewModel, dateKey: String, isEnglish: Boolean) {
    val revision by viewModel.journeyRevision.collectAsState()
    val context = LocalContext.current
    val source = remember(context) { PrefsPrayerRecordSource(context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)) }
    val date = remember(dateKey) { runCatching { LocalDate.parse(dateKey) }.getOrNull() } ?: return
    val today = LocalDate.now()
    val day = remember(revision, dateKey) { JourneyEngine.dayRecord(source, date, today) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Prayer.entries.forEachIndexed { i, prayer ->
            val status = day.statuses[i]
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrayerStar(status, 22.dp, neutral = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prayer.label(isEnglish), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            prayerDetailLine(source, date, prayer, status, isEnglish),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                PrayerRecordEditor(status, isEnglish, onChoose = { viewModel.setPrayerRecord(dateKey, prayer, it) })
            }
        }
    }
}

// ------------------------------------------------------------------ charts

/** Simple accessible bar chart. null value = no data (drawn as a small dot, not as zero). */
@Composable
fun PercentBars(
    values: List<Int?>,
    labels: List<String>,
    color: Color,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 96.dp
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            val label = labels.getOrElse(i) { "" }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$label: " + (if (v == null) (if (isEnglish) "no data" else "তথ্য নেই") else jPct(v, isEnglish))
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.height(chartHeight), contentAlignment = Alignment.BottomCenter) {
                    if (v == null) {
                        Box(
                            Modifier
                                .width(6.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((chartHeight.value * v.coerceIn(0, 100) / 100f).coerceAtLeast(3f).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(color)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        }
    }
}

/**
 * Calendar heatmap for one month. Intensity = share of the five prayers completed (or whether the
 * chosen prayer was completed). Untracked days are an empty outline, never shaded as "missed".
 */
@Composable
fun MonthHeatmap(
    month: YearMonth,
    source: PrayerRecordSource,
    today: LocalDate,
    filter: Prayer?,
    isEnglish: Boolean,
    color: Color,
    revisionKey: Any
) {
    val cells = remember(month, filter, revisionKey, today) {
        val lead = month.atDay(1).dayOfWeek.value - 1 // Monday first
        val list = ArrayList<DayRecord?>()
        repeat(lead) { list += null }
        for (d in 1..month.lengthOfMonth()) list += JourneyEngine.dayRecord(source, month.atDay(d), today)
        while (list.size % 7 != 0) list += null
        list
    }
    val heads = if (isEnglish) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("সো", "ম", "বু", "বৃ", "শু", "শ", "র")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            heads.forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { rec ->
                    val m = Modifier.weight(1f).aspectRatio(1f)
                    if (rec == null) {
                        Box(m)
                    } else {
                        val future = rec.date.isAfter(today)
                        val value = if (filter == null) rec.completed / 5f
                        else if (rec.statuses[filter.ordinal] == PrayerStatus.COMPLETED) 1f else 0f
                        val desc = jDate(rec.date, isEnglish) + ": " + when {
                            future -> if (isEnglish) "upcoming" else "আসন্ন"
                            !rec.tracked -> if (isEnglish) "not tracked" else "ট্র্যাক করা হয়নি"
                            filter == null -> if (isEnglish) "${rec.completed} of 5 prayers completed" else "৫টির মধ্যে ${rec.completed.toBanglaDigits()}টি আদায়"
                            else -> statusLabel(rec.statuses[filter.ordinal], isEnglish)
                        }
                        Box(
                            modifier = m
                                .clip(RoundedCornerShape(6.dp))
                                .then(
                                    if (rec.tracked && !future) Modifier.background(color.copy(alpha = 0.15f + 0.85f * value))
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (future) 0.2f else 0.5f), RoundedCornerShape(6.dp))
                                )
                                .semantics { contentDescription = desc },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                jNum(rec.date.dayOfMonth, isEnglish),
                                fontSize = 11.sp,
                                color = if (rec.tracked && !future && value > 0.55f) Color(0xFF04201A) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" }) {
        Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier, size: Int = 12) {
    Text(text, modifier = modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = size.sp)
}

/** Prayer filter chips: "All" plus five prayers. [selected] null = all. */
@Composable
fun PrayerFilterRow(selected: Prayer?, isEnglish: Boolean, onSelect: (Prayer?) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            androidx.compose.material3.FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(if (isEnglish) "All prayers" else "সব ওয়াক্ত") },
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }
        items(Prayer.entries.size) { i ->
            val p = Prayer.entries[i]
            androidx.compose.material3.FilterChip(
                selected = selected == p,
                onClick = { onSelect(p) },
                label = { Text(p.label(isEnglish)) },
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }
    }
}
