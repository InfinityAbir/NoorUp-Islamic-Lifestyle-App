package com.example.ui.noorup.ramadan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.GlassCard
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.QuranArabicFont
import com.example.ui.noorup.toBanglaDigits
import com.example.ui.noorup.ramadan.RamadanLogic.FastStatus
import kotlinx.coroutines.delay
import java.util.Calendar

private fun n(v: Int, en: Boolean) = if (en) v.toString() else v.toBanglaDigits()
private fun n(v: String, en: Boolean) = if (en) v else v.toBanglaDigits()

private fun clock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

/** Seconds since local midnight, ticking every second. */
@Composable
private fun rememberNowSeconds(): Long {
    val state = produceState(initialValue = secondsOfDay()) {
        while (true) { delay(1000); value = secondsOfDay() }
    }
    return state.value
}

private fun secondsOfDay(): Long {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) * 3600L + c.get(Calendar.MINUTE) * 60L + c.get(Calendar.SECOND)
}

/** Day to use for trackers: today's Ramadan day, or day 1 when previewing outside Ramadan. */
private fun trackerDay(month: Int, day: Int) = if (month == RamadanLogic.RAMADAN_MONTH) day.coerceIn(1, 30) else 1

@Composable
fun RamadanHomeCard(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val hijri by viewModel.currentHijriDate.collectAsState()
    val times by viewModel.calculatedPrayerTimes.collectAsState()
    val tick by viewModel.ramadanTick.collectAsState()
    val perPrayer by viewModel.perPrayerEnabled.collectAsState()
    val context = LocalContext.current
    val now = rememberNowSeconds()

    val sehriSec = viewModel.parseTimeToMins(times.sehriEnd) * 60L
    val iftarSec = viewModel.parseTimeToMins(times.iftarStart) * 60L
    val (labelEn, labelBn, remaining) = when {
        now < sehriSec -> Triple("Sehri ends in", "সেহরি শেষ হতে বাকি", sehriSec - now)
        now < iftarSec -> Triple("Iftar in", "ইফতার হতে বাকি", iftarSec - now)
        else -> Triple("Next Sehri ends in", "পরবর্তী সেহরি শেষ হতে বাকি", 24 * 3600L - now + sehriSec)
    }
    val fasting = now in sehriSec until iftarSec
    val ramadanDay = trackerDay(hijri.month, hijri.day)
    val year = hijri.year
    val store = viewModel.ramadanStore
    val status = remember(tick, year, ramadanDay) { store.fasting(year)[ramadanDay] ?: FastStatus.NONE }
    val streak = remember(tick, year, ramadanDay) { RamadanLogic.streak(store.fasting(year), ramadanDay) }
    val night = RamadanLogic.nightNumber(ramadanDay, afterMaghrib = now >= iftarSec)
    val oddNight = hijri.month == RamadanLogic.RAMADAN_MONTH && RamadanLogic.isOddLastTenNight(night)

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌙", fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isEnglish) "Ramadan Mubarak" else "রমজান মোবারক",
                    color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                Text(
                    if (hijri.month == RamadanLogic.RAMADAN_MONTH)
                        (if (isEnglish) "Day ${n(ramadanDay, true)} of ${hijri.monthNameEn}" else "${hijri.monthNameBn}-এর ${n(ramadanDay, false)} তম দিন")
                    else (if (isEnglish) "Preview mode (Ramadan has not started)" else "প্রিভিউ মোড (রমজান শুরু হয়নি)"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                )
            }
            if (streak > 0) {
                Text(
                    "🔥 ${n(streak, isEnglish)}",
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (isEnglish) labelEn else labelBn,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
        )
        Text(
            n(clock(remaining), isEnglish),
            color = MaterialTheme.colorScheme.onSurface, fontSize = 38.sp, fontWeight = FontWeight.Bold
        )
        val total = (iftarSec - sehriSec).coerceAtLeast(1)
        LinearProgressIndicator(
            progress = { if (fasting) ((now - sehriSec) / total.toFloat()).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                (if (isEnglish) "Sehri ends " else "সেহরি শেষ ") + n(times.sehriEnd, isEnglish),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                (if (isEnglish) "Iftar " else "ইফতার ") + n(times.iftarStart, isEnglish),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    val next = if (status == FastStatus.FASTED) FastStatus.NONE else FastStatus.FASTED
                    store.setFasting(year, ramadanDay, next); viewModel.ramadanChanged()
                },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (status == FastStatus.FASTED) (if (isEnglish) "✓ Fasting today" else "✓ আজ রোজা রেখেছি")
                    else (if (isEnglish) "Mark today fasted" else "আজকের রোজা চিহ্নিত করুন"),
                    fontSize = 12.sp, maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        AlertSwitchRow(
            if (isEnglish) "Sehri-end alert (at Fajr)" else "সেহরি শেষ অ্যালার্ট (ফজরে)",
            perPrayer["Fajr"] ?: true
        ) { viewModel.setRamadanAlert("Fajr", it, context) }
        AlertSwitchRow(
            if (isEnglish) "Iftar alert (at Maghrib)" else "ইফতার অ্যালার্ট (মাগরিবে)",
            perPrayer["Maghrib"] ?: true
        ) { viewModel.setRamadanAlert("Maghrib", it, context) }
        if (oddNight) {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)).padding(10.dp)
            ) {
                Text(
                    if (isEnglish) "✨ Tonight may be Laylatul Qadr (night ${n(night, true)}, an odd night). Seek it with prayer, Quran and dua."
                    else "✨ আজ রাত লাইলাতুল কদর হতে পারে (${n(night, false)} তম রাত, বিজোড়)। নামাজ, কুরআন ও দুআয় রাত জাগুন।",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AlertSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------------------------------------------------------------------------------------
// Tools tab: full Ramadan hub
// ---------------------------------------------------------------------------------------------

@Composable
fun RamadanToolsView(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val hijri by viewModel.currentHijriDate.collectAsState()
    val mode by viewModel.ramadanMode.collectAsState()
    val active by viewModel.ramadanActive.collectAsState()
    val tick by viewModel.ramadanTick.collectAsState()
    val year = hijri.year
    val today = trackerDay(hijri.month, hijri.day)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // --- Mode ---
        GlassCard {
            Text(if (isEnglish) "Ramadan Mode" else "রমজান মোড", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (isEnglish) "Auto turns the Ramadan dashboard on for the whole holy month (Hijri month 9). Use On to preview it any time."
                else "অটো মোড পুরো রমজান মাসে (হিজরি ৯ম মাস) রমজান ড্যাশবোর্ড চালু করে। যেকোনো সময় দেখতে \"চালু\" বেছে নিন।",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RamadanModePref.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m, onClick = { viewModel.setRamadanMode(m) },
                        label = {
                            Text(
                                when (m) {
                                    RamadanModePref.AUTO -> if (isEnglish) "Auto" else "অটো"
                                    RamadanModePref.ON -> if (isEnglish) "On" else "চালু"
                                    RamadanModePref.OFF -> if (isEnglish) "Off" else "বন্ধ"
                                }
                            )
                        }
                    )
                }
            }
            Text(
                if (active) (if (isEnglish) "Ramadan dashboard is showing on Home." else "রমজান ড্যাশবোর্ড হোমে দেখা যাচ্ছে।")
                else (if (isEnglish) "Ramadan dashboard is hidden." else "রমজান ড্যাশবোর্ড লুকানো আছে।"),
                fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary
            )
        }

        RamadanHomeCard(viewModel)

        FastingTracker(viewModel, year, today, tick)
        TaraweehTracker(viewModel, year, today, tick)
        FitrCalculator(viewModel)
        LaylatulQadrCard(viewModel, year, today, tick)
    }
}

@Composable
private fun FastingTracker(viewModel: NoorUpViewModel, year: Int, today: Int, tick: Int) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val store = viewModel.ramadanStore
    val statuses = remember(tick, year) { store.fasting(year) }
    val streak = RamadanLogic.streak(statuses, today)
    val fasted = statuses.values.count { it == FastStatus.FASTED }
    val missed = statuses.values.count { it == FastStatus.MISSED }
    val owed = remember(tick) { store.qadaOwed() }

    GlassCard {
        Text(if (isEnglish) "Daily Fasting Tracker" else "দৈনিক রোজা ট্র্যাকার", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (isEnglish) "Tap a day to cycle: fasted → missed → clear." else "দিনে ট্যাপ করুন: রোজা রাখা → ছুটে গেছে → মুছুন।",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip("🔥", if (isEnglish) "Streak" else "ধারাবাহিক", n(streak, isEnglish))
            StatChip("✅", if (isEnglish) "Fasted" else "রাখা", n(fasted, isEnglish))
            StatChip("⏳", if (isEnglish) "Missed" else "ছুটেছে", n(missed, isEnglish))
        }
        Spacer(Modifier.height(10.dp))
        (1..30).chunked(6).forEach { rowDays ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                rowDays.forEach { d ->
                    val st = statuses[d] ?: FastStatus.NONE
                    val bg = when (st) {
                        FastStatus.FASTED -> MaterialTheme.colorScheme.primary
                        FastStatus.MISSED -> MaterialTheme.colorScheme.error
                        FastStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(bg)
                            .then(if (d == today) Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape) else Modifier)
                            .clickable { store.setFasting(year, d, st.next()); viewModel.ramadanChanged() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            n(d, isEnglish), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (st == FastStatus.NONE) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(if (isEnglish) "Qada (make-up) log" else "কাযা (পূরণ) লগ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (isEnglish) "Fasts still owed: ${n(owed, true)} • Made up so far: ${n(store.qadaMadeUp(), true)}"
            else "বাকি কাযা রোজা: ${n(owed, false)} • আদায় হয়েছে: ${n(store.qadaMadeUp(), false)}",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { store.addQadaMadeUp(1); viewModel.ramadanChanged() },
                enabled = owed > 0, shape = RoundedCornerShape(12.dp)
            ) { Text(if (isEnglish) "+1 made up" else "+১ কাযা আদায়", fontSize = 12.sp) }
            OutlinedButton(
                onClick = { store.addQadaMadeUp(-1); viewModel.ramadanChanged() },
                enabled = store.qadaMadeUp() > 0, shape = RoundedCornerShape(12.dp)
            ) { Text(if (isEnglish) "Undo" else "ফিরিয়ে নিন", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun StatChip(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$icon $value", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TaraweehTracker(viewModel: NoorUpViewModel, year: Int, today: Int, tick: Int) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val store = viewModel.ramadanStore
    val tar = remember(tick, year) { store.taraweeh(year) }
    val tonight = tar[today] ?: 0
    val nights = tar.values.count { it > 0 }
    val totalRakat = tar.values.sum()

    GlassCard {
        Text(if (isEnglish) "Taraweeh Tracker" else "তারাবীহ ট্র্যাকার", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (isEnglish) "Tonight (night ${n(today, true)}): ${n(tonight, true)} rakat" else "আজ রাত (${n(today, false)} তম): ${n(tonight, false)} রাকাত",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tonight == 8, onClick = { store.setTaraweeh(year, today, 8); viewModel.ramadanChanged() }, label = { Text(if (isEnglish) "8 rakat" else "৮ রাকাত") })
            FilterChip(selected = tonight == 20, onClick = { store.setTaraweeh(year, today, 20); viewModel.ramadanChanged() }, label = { Text(if (isEnglish) "20 rakat" else "২০ রাকাত") })
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { store.setTaraweeh(year, today, (tonight - 2).coerceAtLeast(0)); viewModel.ramadanChanged() }, shape = RoundedCornerShape(12.dp)) { Text("−2") }
            OutlinedButton(onClick = { store.setTaraweeh(year, today, tonight + 2); viewModel.ramadanChanged() }, shape = RoundedCornerShape(12.dp)) { Text("+2") }
            OutlinedButton(onClick = { store.setTaraweeh(year, today, 0); viewModel.ramadanChanged() }, shape = RoundedCornerShape(12.dp)) { Text(if (isEnglish) "Reset" else "রিসেট") }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (isEnglish) "This Ramadan: ${n(nights, true)} nights • ${n(totalRakat, true)} rakat in total"
            else "এই রমজানে: ${n(nights, false)} রাত • মোট ${n(totalRakat, false)} রাকাত",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FitrCalculator(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val store = viewModel.ramadanStore
    var grain by remember { mutableStateOf(store.fitrGrain()) }
    var members by remember { mutableIntStateOf(store.fitrMembers()) }
    var priceText by remember(grain) { mutableStateOf(store.fitrPrice(grain).toString()) }
    val price = priceText.toDoubleOrNull() ?: 0.0
    val total = RamadanLogic.fitrAmount(members, grain, price)
    val perPerson = RamadanLogic.fitrAmount(1, grain, price)

    GlassCard {
        Text(if (isEnglish) "Sadaqatul Fitr / Zakat al-Fitr" else "সাদাকাতুল ফিতর / ফিতরা", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (isEnglish) "Preset quantities (Hanafi): wheat 1.75 kg, others 3.5 kg per person. Prices are editable estimates in BDT — use the rate announced by the Islamic Foundation or your local imam."
            else "প্রিসেট পরিমাণ (হানাফী): গম ১.৭৫ কেজি, অন্যগুলো ৩.৫ কেজি জনপ্রতি। দাম টাকায় সম্পাদনযোগ্য আনুমানিক মান — ইসলামিক ফাউন্ডেশন বা স্থানীয় ইমামের ঘোষিত হার ব্যবহার করুন।",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RamadanLogic.Grain.entries.take(2).forEach { g ->
                FilterChip(selected = grain == g, onClick = { grain = g }, label = { Text(if (isEnglish) g.nameEn else g.nameBn, fontSize = 12.sp, maxLines = 1) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RamadanLogic.Grain.entries.drop(2).forEach { g ->
                FilterChip(selected = grain == g, onClick = { grain = g }, label = { Text(if (isEnglish) g.nameEn else g.nameBn, fontSize = 12.sp, maxLines = 1) })
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = priceText, onValueChange = { priceText = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text(if (isEnglish) "৳ per kg" else "৳ প্রতি কেজি") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isEnglish) "Members" else "সদস্য", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (members > 1) members-- }, shape = CircleShape, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), modifier = Modifier.size(34.dp)) { Text("−") }
                    Text(n(members, isEnglish), modifier = Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { if (members < 100) members++ }, shape = CircleShape, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), modifier = Modifier.size(34.dp)) { Text("+") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            (if (isEnglish) "Per person: ৳" else "জনপ্রতি: ৳") + n("%,.0f".format(perPerson), isEnglish),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            (if (isEnglish) "Household total: ৳" else "পরিবারের মোট: ৳") + n("%,.0f".format(total), isEnglish),
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = { store.saveFitr(members, grain, priceText.toIntOrNull() ?: grain.defaultPricePerKg) },
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (isEnglish) "Save these values" else "মান সংরক্ষণ করুন", fontSize = 12.sp) }
    }
}

private data class QadrDua(val arabic: String, val translitBn: String, val bn: String, val en: String, val ref: String)

private val qadrDuas = listOf(
    QadrDua(
        "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
        "আল্লাহুম্মা ইন্নাকা আফুউউন তুহিব্বুল আফওয়া ফা'ফু আন্নী",
        "হে আল্লাহ, আপনি ক্ষমাশীল, ক্ষমা করতে ভালোবাসেন; অতএব আমাকে ক্ষমা করুন।",
        "O Allah, You are Forgiving and love to forgive, so forgive me.",
        "Tirmidhi 3513, Ibn Majah 3850 (Aisha, may Allah be pleased with her)"
    ),
    QadrDua(
        "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
        "রব্বানা আতিনা ফিদ্দুনইয়া হাসানাতাও ওয়া ফিল আখিরাতি হাসানাতাও ওয়া কিনা আযাবান নার",
        "হে আমাদের রব, আমাদের দুনিয়াতে কল্যাণ দিন, আখিরাতে কল্যাণ দিন এবং আমাদের জাহান্নামের আযাব থেকে রক্ষা করুন।",
        "Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
        "Quran 2:201"
    ),
    QadrDua(
        "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْجَنَّةَ وَأَعُوذُ بِكَ مِنَ النَّارِ",
        "আল্লাহুম্মা ইন্নী আসআলুকাল জান্নাতা ওয়া আউযু বিকা মিনান নার",
        "হে আল্লাহ, আমি আপনার কাছে জান্নাত চাই এবং জাহান্নাম থেকে আপনার আশ্রয় চাই।",
        "O Allah, I ask You for Paradise and seek refuge in You from the Fire.",
        "Abu Dawud 792"
    )
)

private val qadrChecklistItems = listOf(
    "Prayed Isha and Fajr in congregation" to "জামাতে এশা ও ফজর পড়েছি",
    "Stood in night prayer (Qiyam)" to "রাতের নামাজ (কিয়াম) পড়েছি",
    "Recited Quran" to "কুরআন তিলাওয়াত করেছি",
    "Made dua and istighfar" to "দুআ ও ইস্তিগফার করেছি",
    "Gave sadaqah" to "সদকা দিয়েছি"
)

@Composable
private fun LaylatulQadrCard(viewModel: NoorUpViewModel, year: Int, today: Int, tick: Int) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val store = viewModel.ramadanStore
    var night by remember { mutableIntStateOf(RamadanLogic.ODD_NIGHTS.firstOrNull { it >= today } ?: 27) }
    val mask = remember(tick, year, night) { store.qadrChecklist(year, night) }

    GlassCard {
        Text(if (isEnglish) "Laylatul Qadr" else "লাইলাতুল কদর", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (isEnglish) "\"Seek Laylatul Qadr in the odd nights of the last ten nights of Ramadan.\" (Bukhari 2017)"
            else "\"রমজানের শেষ দশকের বিজোড় রাতগুলোতে লাইলাতুল কদর তালাশ করো।\" (বুখারি ২০১৭)",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RamadanLogic.ODD_NIGHTS.forEach { nn ->
                FilterChip(selected = night == nn, onClick = { night = nn }, label = { Text(n(nn, isEnglish)) })
            }
        }
        Spacer(Modifier.height(4.dp))
        qadrChecklistItems.forEachIndexed { i, (en, bn) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = (mask shr i) and 1 == 1,
                    onCheckedChange = { on ->
                        val m = if (on) mask or (1 shl i) else mask and (1 shl i).inv()
                        store.setQadrChecklist(year, night, m); viewModel.ramadanChanged()
                    }
                )
                Text(if (isEnglish) en else bn, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(if (isEnglish) "Duas for the night" else "রাতের দুআ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
        qadrDuas.forEach { d ->
            Spacer(Modifier.height(8.dp))
            Text(
                d.arabic, fontFamily = QuranArabicFont, fontSize = 20.sp, lineHeight = 34.sp,
                textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface
            )
            if (!isEnglish) Text(d.translitBn, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (isEnglish) d.en else d.bn, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(d.ref, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
