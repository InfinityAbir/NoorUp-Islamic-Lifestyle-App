package com.example.ui.noorup.journey

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.BackNavigator
import com.example.ui.noorup.GlassCard
import com.example.ui.noorup.NestedBackHandler
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.TasbihFeedback
import com.example.ui.noorup.toBanglaDigits
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay
import java.time.LocalDate

private val OnSky = Color(0xFFE2E8F0)
private val OnSkyMuted = Color(0xFFA5B4CC)

private enum class JourneySection(val en: String, val bn: String) {
    TODAY("Today", "আজ"),
    PROGRESS("Progress", "অগ্রগতি"),
    ANALYTICS("Analytics", "বিশ্লেষণ"),
    SETTINGS("Settings", "সেটিংস")
}

/**
 * Full-screen Crescent Journey, opened from the Stats hero (nested route on the Stats tab; Back closes it).
 * Every number comes from the existing prayer records through [JourneyRepository].
 */
@Composable
fun CrescentJourneyScreen(viewModel: NoorUpViewModel, onOpenFriday: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val isEnglish by viewModel.isEnglish.collectAsState()
    val revision by viewModel.journeyRevision.collectAsState()
    val prefs = remember(ctx) { ctx.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE) }
    val repo = remember(prefs) { JourneyRepository(prefs) }
    val settings = remember(prefs) { JourneyPrefs(prefs) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    val systemReduced = remember(ctx) { TasbihFeedback.isReducedMotion(ctx) }
    val motion = remember(settingsVersion) { settings.motionAllowed(systemReduced) }

    // Minute tick: a prayer whose time arrives flips UPCOMING -> PENDING without reopening the screen.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            tick++
        }
    }
    var seenTick by remember { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val data = remember(revision, tick, today, seenTick) { repo.snapshot(today, viewModel.journeyTodayTiming()) }
    LaunchedEffect(data) { repo.persist(data) }

    var sectionIndex by rememberSaveable { mutableIntStateOf(0) }
    val section = JourneySection.entries[sectionIndex.coerceIn(0, JourneySection.entries.size - 1)]
    NestedBackHandler(viewModel.backNavigator, enabled = sectionIndex != 0, priority = BackNavigator.PRIORITY_SCREEN + 1) {
        sectionIndex = 0
    }

    // Several constellations can unlock at once (an existing user's history): celebrate the newest one
    // and quietly mark the rest as seen, so opening the Journey never turns into a stack of pop-ups.
    val allUnseen = data.milestones.filter { it.unlockedOn != null && !it.seen }
    val unseen = allUnseen.maxByOrNull { it.unlockedOn!! }
    if (unseen != null) {
        MilestoneDiscoveryDialog(unseen, isEnglish, motion) {
            allUnseen.forEach { repo.markSeen(it.id) }
            seenTick++
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.setJourneyRoute(null) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isEnglish) "Back" else "পেছনে", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                if (isEnglish) "Crescent Journey" else "চাঁদের যাত্রা",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(JourneySection.entries.size) { i ->
                val s = JourneySection.entries[i]
                FilterChip(
                    selected = i == sectionIndex,
                    onClick = { sectionIndex = i },
                    label = { Text(if (isEnglish) s.en else s.bn) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            when (section) {
                JourneySection.TODAY -> todayItems(viewModel, repo, data, isEnglish, motion, onOpenFriday)
                JourneySection.PROGRESS -> progressItems(data, isEnglish)
                JourneySection.ANALYTICS -> analyticsItems(viewModel, repo, data, isEnglish, revision)
                JourneySection.SETTINGS -> item {
                    JourneySettingsCard(isEnglish, settings) { settingsVersion++ }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Today

private fun LazyListScope.todayItems(
    viewModel: NoorUpViewModel,
    repo: JourneyRepository,
    data: JourneyData,
    isEnglish: Boolean,
    motion: Boolean,
    onOpenFriday: (Boolean) -> Unit
) {
    val day = data.summary.today
    item {
        var selectedRaw by rememberSaveable { mutableIntStateOf(-1) }
        val selected = if (selectedRaw in 0..4) selectedRaw
        else day.statuses.indexOfFirst { it == PrayerStatus.PENDING }.takeIf { it >= 0 } ?: 0
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            JourneySkyCard(data, isEnglish, motion, selected) { selectedRaw = it }
            PrayerDetailCard(viewModel, repo, day, Prayer.entries[selected], isEnglish)
        }
    }
    item { WeeklyReflectionCard(repo, isEnglish, onOpenFriday) }
    item {
        GlassCard {
            Text(
                if (isEnglish) "How the crescent lights up" else "চাঁদ কীভাবে আলোকিত হয়",
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            MutedText(
                if (isEnglish) "Each of the five prayers you record as completed adds one fifth of the light. Brightness is a visual guide to your own record, not a judgement of worship. Prayers you have not recorded are simply unlit, never counted as missed."
                else "পাঁচ ওয়াক্তের প্রতিটি আদায় রেকর্ড করলে আলোর পাঁচ ভাগের এক ভাগ যোগ হয়। উজ্জ্বলতা আপনার নিজের রেকর্ডের একটি দৃশ্যমান নির্দেশিকা, ইবাদতের মূল্যায়ন নয়। রেকর্ড না করা ওয়াক্ত কেবল অনালোকিত থাকে, কখনও ছুটে যাওয়া হিসেবে গণ্য হয় না।"
            )
        }
    }
}

@Composable
private fun JourneySkyCard(data: JourneyData, isEnglish: Boolean, motion: Boolean, selected: Int, onSelect: (Int) -> Unit) {
    val day = data.summary.today
    val stage = data.summary.stage.stage
    val illumination by animateFloatAsState(day.illumination, tween(if (motion) 700 else 0), label = "illumination")
    val twinkle = rememberTwinkle(motion)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .celestialSky(stage, twinkle)
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stage.label(isEnglish), color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (isEnglish) "Stage ${stage.number} of 6" else "ধাপ ${jNum(stage.number, false)}/৬",
                        color = OnSkyMuted, fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${jNum(day.completed, isEnglish)}/${jNum(5, isEnglish)}",
                        color = OnSky, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false
                    )
                    Text(if (isEnglish) "prayers today" else "আজকের ওয়াক্ত", color = OnSkyMuted, fontSize = 11.sp)
                }
            }
            Canvas(
                modifier = Modifier
                    .size(176.dp)
                    .semantics {
                        contentDescription = if (isEnglish) "Crescent moon, ${day.percent} percent illuminated"
                        else "অর্ধচন্দ্র, ${jNum(day.percent, false)} শতাংশ আলোকিত"
                    }
            ) { drawCrescent(illumination) }

            val arc = listOf(16.dp, 4.dp, 0.dp, 4.dp, 16.dp)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Prayer.entries.forEachIndexed { i, prayer ->
                    val status = day.statuses[i]
                    val isSelected = i == selected
                    val desc = "${prayer.label(isEnglish)}, ${statusLabel(status, isEnglish)}"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = arc[i])
                            .clip(RoundedCornerShape(14.dp))
                            .then(if (isSelected) Modifier.border(1.5.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(14.dp)) else Modifier)
                            .clickable(
                                onClickLabel = if (isEnglish) "Inspect ${prayer.en}" else "${prayer.bn} দেখুন",
                                role = Role.Button
                            ) { onSelect(i) }
                            .semantics { contentDescription = desc; this.selected = isSelected }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PrayerStar(status, 38.dp, motion = motion)
                        Text(
                            prayer.label(isEnglish),
                            color = if (status == PrayerStatus.COMPLETED) GoldAccent else OnSkyMuted,
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(if (isEnglish) "Daily illumination" else "দৈনিক আলো", color = OnSkyMuted, fontSize = 11.sp)
                    Text(jPct(day.percent, isEnglish), color = GoldAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isEnglish) "30-day consistency" else "৩০ দিনের ধারাবাহিকতা", color = OnSkyMuted, fontSize = 11.sp)
                    Text(
                        data.summary.rolling30.percent?.let { jPct(it, isEnglish) } ?: if (isEnglish) "Not enough data yet" else "এখনও পর্যাপ্ত তথ্য নেই",
                        color = Color(0xFF34D399), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { illumination },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = GoldAccent,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun PrayerDetailCard(viewModel: NoorUpViewModel, repo: JourneyRepository, day: DayRecord, prayer: Prayer, isEnglish: Boolean) {
    val status = day.statuses[prayer.ordinal]
    val window = remember(prayer) { viewModel.journeyTodayWindow(prayer) }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrayerStar(status, 28.dp, neutral = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(prayer.label(isEnglish), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (window != null) MutedText((if (isEnglish) "Time: " else "সময়: ") + (if (isEnglish) window else window.toBanglaDigits()), size = 12)
            }
            Text(
                statusLabel(status, isEnglish).replaceFirstChar { it.uppercase() },
                color = if (status == PrayerStatus.COMPLETED) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        MutedText(prayerDetailLine(repo.source, day.date, prayer, status, isEnglish))
        Spacer(Modifier.height(10.dp))
        PrayerRecordEditor(status, isEnglish, onChoose = { viewModel.setPrayerRecord(day.date.toString(), prayer, it) })
    }
}

// ------------------------------------------------------------------ Progress

private fun LazyListScope.progressItems(data: JourneyData, isEnglish: Boolean) {
    val result = data.summary.stage
    item {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StagePreview(result.stage, Modifier.size(width = 96.dp, height = 72.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(result.stage.label(isEnglish), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    MutedText(if (isEnglish) "Stage ${result.stage.number} of 6" else "ধাপ ${jNum(result.stage.number, false)}/৬")
                    MutedText(
                        (if (isEnglish) "Highest reached: " else "সর্বোচ্চ পৌঁছানো: ") + data.maxStage.label(isEnglish),
                        size = 11
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill(if (isEnglish) "30-day" else "৩০ দিন", jPct(result.r30.percent, isEnglish), Modifier.weight(1f))
                StatPill(if (isEnglish) "90-day" else "৯০ দিন", jPct(result.r90.percent, isEnglish), Modifier.weight(1f))
                StatPill(if (isEnglish) "Tracked days (90)" else "ট্র্যাক করা দিন (৯০)", jNum(result.r90.trackedDays, isEnglish), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            val delta = if (result.r30.percent != null && result.previous30.percent != null) result.r30.percent!! - result.previous30.percent!! else null
            MutedText(
                if (delta == null) (if (isEnglish) "Compared with the previous 30 days: not enough data yet." else "আগের ৩০ দিনের সঙ্গে তুলনা: এখনও পর্যাপ্ত তথ্য নেই।")
                else (if (isEnglish) "Compared with the previous 30 days: ${jDelta(delta, true)}." else "আগের ৩০ দিনের সঙ্গে তুলনা: ${jDelta(delta, false)}।")
            )
            if (result.cappedByHistory) {
                Spacer(Modifier.height(6.dp))
                MutedText(
                    if (isEnglish) "Your record is still young, so the stage is held at ${result.stage.en} until you have tracked more days. Nothing is lost; it opens up as you keep your record."
                    else "আপনার রেকর্ড এখনও নতুন, তাই আরও দিন ট্র্যাক করা পর্যন্ত ধাপ ${result.stage.bn}-এ ধরা আছে। কিছুই হারায়নি; রেকর্ড রাখলে এটি এগোবে।"
                )
            }
        }
    }
    item {
        GlassCard {
            Text(if (isEnglish) "The six stages" else "ছয়টি ধাপ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Stage.entries.forEach { stage ->
                val current = stage == result.stage
                val high = stage == data.maxStage
                val hi = Stage.entries.getOrNull(stage.number)?.minScore?.minus(1) ?: 100
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(vertical = 4.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = stage.label(isEnglish) +
                                (if (current) (if (isEnglish) ", current stage" else ", বর্তমান ধাপ") else "")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StagePreview(stage, Modifier.size(width = 64.dp, height = 44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stage.label(isEnglish), color = if (current) GoldAccent else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        MutedText(if (isEnglish) "${stage.minScore}–$hi% visual progress" else "${jNum(stage.minScore, false)}–${jNum(hi, false)}% দৃশ্যমান অগ্রগতি", size = 11)
                    }
                    if (current) Text(if (isEnglish) "Now" else "এখন", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    else if (high) Text(if (isEnglish) "Highest" else "সর্বোচ্চ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            MutedText(
                if (isEnglish) "How it is worked out: two thirds of your 30-day consistency plus one third of your 90-day consistency, with a small nudge (up to 3 points) for how you compare with the previous 30 days. Days you did not track are left out, not counted against you. These are visual stages, not religious ranks."
                else "হিসাবের পদ্ধতি: ৩০ দিনের ধারাবাহিকতার দুই-তৃতীয়াংশ ও ৯০ দিনের ধারাবাহিকতার এক-তৃতীয়াংশ, সঙ্গে আগের ৩০ দিনের তুলনায় সামান্য (সর্বোচ্চ ৩ পয়েন্ট) সমন্বয়। যে দিনগুলো ট্র্যাক করেননি সেগুলো বাদ থাকে, আপনার বিপক্ষে গণ্য হয় না। এগুলো দৃশ্যমান ধাপ, ধর্মীয় মর্যাদা নয়।",
                size = 11
            )
        }
    }
    item {
        Text(
            if (isEnglish) "Constellations" else "নক্ষত্রপুঞ্জ",
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp
        )
    }
    val chunks = data.milestones.chunked(2)
    chunks.forEach { pair ->
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { m -> ConstellationCard(m, isEnglish, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    item {
        val unlocked = data.milestones.filter { it.unlockedOn != null }.sortedByDescending { it.unlockedOn }
        GlassCard {
            Text(if (isEnglish) "Milestone history" else "মাইলফলকের ইতিহাস", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            if (unlocked.isEmpty()) {
                MutedText(if (isEnglish) "Keep your record and your first constellation will appear here." else "রেকর্ড রাখুন, আপনার প্রথম নক্ষত্রপুঞ্জ এখানে দেখা যাবে।")
            } else {
                unlocked.forEach { m ->
                    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.id.label(isEnglish), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        MutedText(jDateLong(m.unlockedOn!!, isEnglish), size = 12)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            MutedText(
                if (isEnglish) "Milestones celebrate showing up and keeping a record. They say nothing about how your worship is received."
                else "মাইলফলক উপস্থিতি ও রেকর্ড রাখাকে উদ্‌যাপন করে। আপনার ইবাদত কীভাবে গৃহীত হয় সে বিষয়ে এগুলো কিছু বলে না।",
                size = 11
            )
        }
    }
}

@Composable
fun StagePreview(stage: Stage, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).celestialSky(stage, null)) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawCrescent(0.85f, Offset(size.width * 0.5f, size.height * 0.45f), size.height * 0.28f) }
    }
}

@Composable
private fun ConstellationCard(m: MilestoneState, isEnglish: Boolean, modifier: Modifier) {
    val unlocked = m.unlockedOn != null
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .celestialSky(Stage.FIRST_LIGHT, null)
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = m.id.label(isEnglish) + ", " +
                    (if (unlocked) jDateLong(m.unlockedOn!!, isEnglish) else if (isEnglish) "locked" else "বন্ধ")
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            ConstellationCanvas(m.id, unlocked, 84.dp)
            Spacer(Modifier.height(6.dp))
            Text(m.id.label(isEnglish), color = if (unlocked) GoldAccent else OnSkyMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
            Text(m.id.description(isEnglish), color = OnSkyMuted, fontSize = 10.sp, textAlign = TextAlign.Center, minLines = 3, maxLines = 4)
            Spacer(Modifier.height(4.dp))
            Text(
                if (unlocked) jDate(m.unlockedOn!!, isEnglish) else if (isEnglish) "Locked" else "বন্ধ",
                color = if (unlocked) OnSky else OnSkyMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MilestoneDiscoveryDialog(m: MilestoneState, isEnglish: Boolean, motion: Boolean, onDismiss: () -> Unit) {
    val start = remember { androidx.compose.animation.core.Animatable(if (motion) 0.6f else 1f) }
    LaunchedEffect(Unit) { start.animateTo(1f, tween(if (motion) 600 else 0)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (isEnglish) "Lovely" else "চমৎকার") } },
        title = { Text(if (isEnglish) "A new constellation" else "নতুন নক্ষত্রপুঞ্জ", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).celestialSky(Stage.FIRST_LIGHT, null).padding(16.dp)) {
                    ConstellationCanvas(m.id, true, 120.dp, Modifier.scale(start.value))
                }
                Spacer(Modifier.height(10.dp))
                Text(m.id.label(isEnglish), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoldAccent)
                Spacer(Modifier.height(4.dp))
                Text(m.id.description(isEnglish), fontSize = 13.sp, textAlign = TextAlign.Center)
                m.unlockedOn?.let { Text(jDateLong(it, isEnglish), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    )
}
