package com.example.ui.noorup.journey

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.GlassCard
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.theme.GoldAccent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun q1Options(en: Boolean) = if (en) listOf("Very consistent", "Better than last week", "Some difficulties", "I want to improve")
else listOf("খুব ধারাবাহিক", "গত সপ্তাহের চেয়ে ভালো", "কিছু অসুবিধা হয়েছে", "আমি আরও উন্নতি করতে চাই")

private fun q2Options(en: Boolean) = Prayer.entries.map { it.label(en) } +
    (if (en) "No particular difficulty" else "বিশেষ কোনো অসুবিধা নেই")

private fun intentionPresets(en: Boolean) = if (en) listOf(
    "Become more consistent with Fajr",
    "Avoid delaying Salah",
    "Maintain consistency across all five prayers",
    "Improve my daily prayer routine"
) else listOf(
    "ফজরে আরও ধারাবাহিক হওয়া",
    "নামাজ বিলম্ব না করা",
    "পাঁচ ওয়াক্তেই ধারাবাহিকতা বজায় রাখা",
    "আমার দৈনিক নামাজের রুটিন উন্নত করা"
)

/** Section F on the Stats screen: availability, previous reflection, last week's summary, entry to history. */
@Composable
fun WeeklyReflectionCard(repo: JourneyRepository, isEnglish: Boolean, onOpen: (history: Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE) }
    val store = remember(prefs) { ReflectionStore(prefs) }
    val today = LocalDate.now()
    val friday = JourneyEngine.latestFriday(today)
    val entries = remember(today) { store.all() }
    val current = entries.firstOrNull { it.fridayDate == friday.toString() }
    val last = entries.firstOrNull()
    val summary = remember(today) { JourneyEngine.fridaySummary(repo.source, friday, today) }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isEnglish) "Friday Reflection" else "শুক্রবারের প্রতিফলন",
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
                MutedText(
                    when {
                        current != null -> if (isEnglish) "Saved for Friday ${jDate(friday, true)}. You can still edit it." else "${jDate(friday, false)} শুক্রবারের জন্য সংরক্ষিত। এখনও সম্পাদনা করা যাবে।"
                        today == friday -> if (isEnglish) "Today is Friday. Your weekly reflection is ready." else "আজ শুক্রবার। আপনার সাপ্তাহিক প্রতিফলন প্রস্তুত।"
                        else -> if (isEnglish) "Reflection for Friday ${jDate(friday, true)} is open until next Friday." else "${jDate(friday, false)} শুক্রবারের প্রতিফলন পরের শুক্রবার পর্যন্ত খোলা।"
                    }
                )
            }
            Canvas(modifier = Modifier.size(40.dp)) {
                drawCrescent(0.9f, Offset(size.width / 2f, size.height / 2f), size.minDimension * 0.34f)
            }
        }
        Spacer(Modifier.height(8.dp))
        MutedText(
            if (summary.trackedDays == 0) (if (isEnglish) "Last week (${jDate(summary.start, true)} – ${jDate(summary.end, true)}): no tracked days." else "গত সপ্তাহ (${jDate(summary.start, false)} – ${jDate(summary.end, false)}): কোনো ট্র্যাক করা দিন নেই।")
            else if (isEnglish) "Last week: ${summary.completed} completed · ${jPct(summary.consistency, true)} · ${summary.completeDays} full days"
            else "গত সপ্তাহ: ${jNum(summary.completed, false)} আদায় · ${jPct(summary.consistency, false)} · ${jNum(summary.completeDays, false)} পূর্ণ দিন"
        )
        MutedText(
            if (last == null) (if (isEnglish) "No reflection saved yet." else "এখনও কোনো প্রতিফলন সংরক্ষিত নেই।")
            else if (isEnglish) "Previous reflection: Friday ${jDate(LocalDate.parse(last.fridayDate), true)}"
            else "আগের প্রতিফলন: ${jDate(LocalDate.parse(last.fridayDate), false)} শুক্রবার"
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onOpen(false) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(if (current != null) (if (isEnglish) "Edit reflection" else "সম্পাদনা") else (if (isEnglish) "Reflect" else "প্রতিফলন করুন"), maxLines = 1)
            }
            OutlinedButton(onClick = { onOpen(true) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text((if (isEnglish) "History" else "ইতিহাস") + if (entries.isNotEmpty()) " (${jNum(entries.size, isEnglish)})" else "", maxLines = 1)
            }
        }
    }
}

/** "Your Weekly Journey": weekly summary, three optional questions, one personal intention, and history. */
@Composable
fun FridayReflectionScreen(viewModel: NoorUpViewModel, startOnHistory: Boolean, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isEnglish by viewModel.isEnglish.collectAsState()
    val prefs = remember(context) { context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE) }
    val repo = remember(prefs) { JourneyRepository(prefs) }
    val store = remember(prefs) { ReflectionStore(prefs) }
    val settings = remember(prefs) { JourneyPrefs(prefs) }
    var tab by rememberSaveable { mutableIntStateOf(if (startOnHistory) 1 else 0) }
    var version by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isEnglish) "Back" else "পেছনে", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                if (isEnglish) "Your Weekly Journey" else "আপনার সাপ্তাহিক যাত্রা",
                color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .celestialSky(Stage.RISING_CRESCENT, null)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().semantics { contentDescription = if (isEnglish) "Golden crescent moon" else "সোনালি অর্ধচন্দ্র" }) {
                drawCrescent(0.95f, Offset(size.width / 2f, size.height / 2f), size.height * 0.34f)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(2) { i ->
                FilterChip(
                    selected = tab == i,
                    onClick = { tab = i },
                    label = { Text(if (i == 0) (if (isEnglish) "This week" else "এই সপ্তাহ") else (if (isEnglish) "History" else "ইতিহাস")) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            if (tab == 0) {
                ThisWeekTab(repo, store, settings, isEnglish, version) { version++ }
            } else {
                HistoryTab(repo, store, isEnglish, version) { version++ }
            }
        }
    }
}

@Composable
private fun ThisWeekTab(repo: JourneyRepository, store: ReflectionStore, settings: JourneyPrefs, isEnglish: Boolean, version: Int, onChanged: () -> Unit) {
    val today = LocalDate.now()
    val friday = JourneyEngine.latestFriday(today)
    val summary = remember(today, version) { JourneyEngine.fridaySummary(repo.source, friday, today) }
    val previous = remember(today, version) { JourneyEngine.fridayPreviousSummary(repo.source, friday, today) }
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            MutedText(
                if (today == friday) (if (isEnglish) "Today is Friday. Take a quiet moment." else "আজ শুক্রবার। একটু শান্ত হয়ে বসুন।")
                else (if (isEnglish) "This reflection is for Friday ${jDateLong(friday, true)}. You can complete it any time this week." else "এই প্রতিফলন ${jDateLong(friday, false)} শুক্রবারের জন্য। এই সপ্তাহের যেকোনো সময় সম্পন্ন করতে পারেন।")
            )
        }
        WeeklySummaryCard(summary, previous, isEnglish)
        ReflectionEditor(friday, store, summary, isEnglish, refreshSnapshot = true, version = version, onChanged = onChanged)
        ReminderSettings(isEnglish, settings) { }
    }
}

@Composable
private fun WeeklySummaryCard(summary: PeriodStats, previous: PeriodStats, isEnglish: Boolean) {
    GlassCard {
        SectionLabel(if (isEnglish) "Your week" else "আপনার সপ্তাহ")
        MutedText(
            "${jDateLong(summary.start, isEnglish)} → ${jDateLong(summary.end, isEnglish)}",
            size = 11
        )
        Spacer(Modifier.height(8.dp))
        if (summary.trackedDays == 0) {
            MutedText(
                if (isEnglish) "You did not track any day last week, and that is okay. Nothing is counted as missed. Your reflection can still be written."
                else "গত সপ্তাহে কোনো দিন ট্র্যাক করেননি, তাতে সমস্যা নেই। কিছুই ছুটে যাওয়া হিসেবে গণ্য হয় না। প্রতিফলন তবুও লেখা যায়।"
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(jPct(summary.consistency, isEnglish), color = GoldAccent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                MutedText(if (isEnglish) "weekly consistency" else "সাপ্তাহিক ধারাবাহিকতা", modifier = Modifier.padding(bottom = 6.dp))
            }
            ComparisonLine(summary.consistency, previous.consistency, "week", "সপ্তাহ", isEnglish)
            Spacer(Modifier.height(10.dp))
            TotalsRow(summary, isEnglish)
            Spacer(Modifier.height(12.dp))
            PrayerBreakdown(summary, null, isEnglish)
        }
    }
}

@Composable
private fun ChoiceGrid(options: List<String>, selected: Int?, columns: Int, onSelect: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.withIndex().chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (i, label) ->
                    FilterChip(
                        selected = selected == i,
                        onClick = { onSelect(if (selected == i) null else i) },
                        label = { Text(label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ReflectionEditor(
    friday: LocalDate,
    store: ReflectionStore,
    summary: PeriodStats,
    isEnglish: Boolean,
    refreshSnapshot: Boolean,
    version: Int,
    onChanged: () -> Unit
) {
    val existing = remember(friday, version) { store.get(friday) }
    var q1 by rememberSaveable(friday) { mutableStateOf(existing?.q1) }
    var q2 by rememberSaveable(friday) { mutableStateOf(existing?.q2) }
    var q3 by rememberSaveable(friday) { mutableStateOf(existing?.q3 ?: "") }
    var intention by rememberSaveable(friday) { mutableStateOf(existing?.intention ?: "") }
    var savedAt by remember(friday) { mutableStateOf<Long?>(null) }
    var confirmDelete by remember(friday) { mutableStateOf(false) }

    GlassCard {
        SectionLabel(if (isEnglish) "Reflect on your week" else "সপ্তাহটি নিয়ে ভাবুন")
        MutedText(if (isEnglish) "Every question is optional. Your answers stay on this device and are never shared." else "সব প্রশ্নই ঐচ্ছিক। আপনার উত্তর এই ডিভাইসেই থাকে, কখনও শেয়ার হয় না।", size = 11)
        Spacer(Modifier.height(12.dp))
        Text(if (isEnglish) "How was your Salah this week?" else "এই সপ্তাহে আপনার নামাজ কেমন ছিল?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        ChoiceGrid(q1Options(isEnglish), q1, 2) { q1 = it }
        Spacer(Modifier.height(14.dp))
        Text(if (isEnglish) "Which prayer was most challenging?" else "কোন ওয়াক্তটি সবচেয়ে কঠিন ছিল?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        ChoiceGrid(q2Options(isEnglish), q2, 2) { q2 = it }
        Spacer(Modifier.height(14.dp))
        Text(if (isEnglish) "What would you like to improve next week?" else "আগামী সপ্তাহে আপনি কী উন্নত করতে চান?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = q3,
            onValueChange = { if (it.length <= ReflectionStore.MAX_TEXT) q3 = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text(if (isEnglish) "Private note, only for you" else "ব্যক্তিগত নোট, শুধু আপনার জন্য") }
        )
    }
    GlassCard {
        SectionLabel(if (isEnglish) "Your intention for the coming week" else "আগামী সপ্তাহের জন্য আপনার নিয়ত")
        MutedText(if (isEnglish) "One gentle direction. It is never marked as achieved or failed." else "একটি কোমল দিকনির্দেশ। এটি কখনও অর্জিত বা ব্যর্থ হিসেবে চিহ্নিত হয় না।", size = 11)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(intentionPresets(isEnglish)) { preset ->
                FilterChip(selected = intention == preset, onClick = { intention = preset }, label = { Text(preset, fontSize = 12.sp) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = intention,
            onValueChange = { if (it.length <= ReflectionStore.MAX_INTENTION) intention = it },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            placeholder = { Text(if (isEnglish) "Write your own, or pick one above" else "নিজের মতো লিখুন, অথবা উপরের একটি বেছে নিন") }
        )
        if (existing?.intention?.isNotBlank() == true || intention.isNotBlank()) {
            TextButton(
                onClick = {
                    intention = ""
                    store.clearIntention(friday, System.currentTimeMillis())
                    onChanged()
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(if (isEnglish) "Delete intention" else "নিয়ত মুছুন") }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                val keep = existing != null && !refreshSnapshot
                val entry = ReflectionEntry(
                    fridayDate = friday.toString(),
                    periodStart = JourneyEngine.fridayWindowStart(friday).toString(),
                    periodEnd = JourneyEngine.fridayWindowEnd(friday).toString(),
                    q1 = q1, q2 = q2, q3 = q3, intention = intention,
                    snapCompleted = if (keep) existing!!.snapCompleted else summary.completed,
                    snapMissed = if (keep) existing!!.snapMissed else summary.confirmedMissed,
                    snapUnrecorded = if (keep) existing!!.snapUnrecorded else summary.unrecorded,
                    snapCompleteDays = if (keep) existing!!.snapCompleteDays else summary.completeDays,
                    snapTrackedDays = if (keep) existing!!.snapTrackedDays else summary.trackedDays,
                    snapPercent = if (keep) existing!!.snapPercent else summary.consistency
                )
                val saved = store.save(entry, System.currentTimeMillis())
                savedAt = saved.updatedAt
                onChanged()
            },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
        ) { Text(if (isEnglish) (if (existing != null) "Update reflection" else "Save reflection") else (if (existing != null) "হালনাগাদ করুন" else "সংরক্ষণ করুন")) }
        if (existing != null) {
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (isEnglish) "Delete" else "মুছুন")
            }
        }
    }
    savedAt?.let {
        MutedText(if (isEnglish) "Saved privately at ${jTime(it, true)}." else "${jTime(it, false)}-এ ব্যক্তিগতভাবে সংরক্ষিত।")
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isEnglish) "Delete this reflection?" else "এই প্রতিফলন মুছবেন?") },
            text = { Text(if (isEnglish) "Your answers and intention for this Friday will be removed from this device." else "এই শুক্রবারের উত্তর ও নিয়ত এই ডিভাইস থেকে মুছে যাবে।") },
            confirmButton = {
                TextButton(onClick = {
                    store.delete(friday)
                    q1 = null; q2 = null; q3 = ""; intention = ""; savedAt = null
                    confirmDelete = false
                    onChanged()
                }) { Text(if (isEnglish) "Delete" else "মুছুন") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(if (isEnglish) "Keep" else "রাখুন") } }
        )
    }
}

@Composable
private fun HistoryTab(repo: JourneyRepository, store: ReflectionStore, isEnglish: Boolean, version: Int, onChanged: () -> Unit) {
    val entries = remember(version) { store.all() }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    if (entries.isEmpty()) {
        GlassCard {
            Text(if (isEnglish) "No reflections yet" else "এখনও কোনো প্রতিফলন নেই", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText(if (isEnglish) "When you save a Friday reflection it will appear here, privately, on this device." else "শুক্রবারের প্রতিফলন সংরক্ষণ করলে তা এখানে, ব্যক্তিগতভাবে এই ডিভাইসে দেখা যাবে।")
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        items(entries, key = { it.fridayDate }) { e ->
            val friday = LocalDate.parse(e.fridayDate)
            val isEditing = editing == e.fridayDate
            GlassCard {
                Text(jDateLong(friday, isEnglish), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                MutedText(
                    "${jDate(LocalDate.parse(e.periodStart), isEnglish)} – ${jDate(LocalDate.parse(e.periodEnd), isEnglish)}  ·  " +
                        (if (isEnglish) "saved ${jDateTime(e.updatedAt, true)}" else "সংরক্ষিত ${jDateTime(e.updatedAt, false)}"),
                    size = 11
                )
                Spacer(Modifier.height(8.dp))
                MutedText(
                    if (e.snapTrackedDays == 0) (if (isEnglish) "No tracked days that week." else "ওই সপ্তাহে কোনো ট্র্যাক করা দিন ছিল না।")
                    else if (isEnglish) "${e.snapCompleted} completed · ${jPct(e.snapPercent, true)} · ${e.snapCompleteDays} full days · ${e.snapMissed} marked missed · ${e.snapUnrecorded} not recorded"
                    else "${jNum(e.snapCompleted, false)} আদায় · ${jPct(e.snapPercent, false)} · ${jNum(e.snapCompleteDays, false)} পূর্ণ দিন · ${jNum(e.snapMissed, false)} চিহ্নিত ছুটে গেছে · ${jNum(e.snapUnrecorded, false)} রেকর্ড নেই"
                )
                e.q1?.let { MutedText((if (isEnglish) "Salah this week: " else "এই সপ্তাহের নামাজ: ") + q1Options(isEnglish).getOrElse(it) { "" }) }
                e.q2?.let { MutedText((if (isEnglish) "Most challenging: " else "সবচেয়ে কঠিন: ") + q2Options(isEnglish).getOrElse(it) { "" }) }
                if (e.q3.isNotBlank()) MutedText((if (isEnglish) "To improve: " else "উন্নতির জন্য: ") + e.q3)
                if (e.intention.isNotBlank()) {
                    Text(
                        (if (isEnglish) "Intention: " else "নিয়ত: ") + e.intention,
                        color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { editing = if (isEditing) null else e.fridayDate },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (isEditing) (if (isEnglish) "Close" else "বন্ধ করুন") else (if (isEnglish) "Edit or delete" else "সম্পাদনা বা মুছুন")) }
                if (isEditing) {
                    Spacer(Modifier.height(10.dp))
                    val summary = remember(e.fridayDate, version) { JourneyEngine.fridaySummary(repo.source, friday, today) }
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ReflectionEditor(friday, store, summary, isEnglish, refreshSnapshot = friday == JourneyEngine.latestFriday(today), version = version) {
                            if (!store.hasEntry(friday)) editing = null
                            onChanged()
                        }
                    }
                }
            }
        }
    }
}
