package com.example.ui.noorup

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AyatWidgetItem
import com.example.DailyAyatContent
import com.example.DailyAyatWidgetProvider
import com.example.NoorUpWidgetProvider
import com.example.TasbihWidgetProvider
import com.example.ui.theme.EmeraldGlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Explore > Home Widget. Shows every NoorUp home-screen widget (Prayer Timeline, Daily Verse & Dua,
 * Tasbih Counter) with a live preview and its own pin button.
 */
@Composable
fun HomeWidgetsHub(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard {
            Text(
                if (isEnglish) "Home Screen Widgets" else "হোম স্ক্রিন উইজেট",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isEnglish) "3 widgets: Prayer Timeline, Daily Verse & Dua, and Tasbih Counter. Add any of them to see or count without opening the app."
                else "৩টি উইজেট: নামাজের সময়সূচী, আজকের আয়াত ও দুআ এবং তসবীহ কাউন্টার। অ্যাপ না খুলেই দেখুন বা গুনুন।",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        PrayerWidgetCard(viewModel, isEnglish)
        AyatWidgetCard(isEnglish)
        TasbihWidgetCard(viewModel, isEnglish)
        HowToAddWidgetsCard(isEnglish)
    }
}

@Composable
private fun WidgetCardHeader(number: Int, title: String, subtitle: String, isEnglish: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(EmeraldGlow.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isEnglish) "$number" else number.toBanglaDigits(),
                color = EmeraldGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** Same look as the real widgets' glass background. */
@Composable
private fun WidgetFrame(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF0F172A))))
            .border(1.dp, Color(0x6634D399), RoundedCornerShape(22.dp))
            .padding(12.dp)
    ) { Column(content = content) }
}

@Composable
private fun WidgetPinButton(provider: Class<*>, isEnglish: Boolean, label: String) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    val supported = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && runCatching { manager.isRequestPinAppWidgetSupported }.getOrDefault(false)
    }
    if (!supported) return
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            try {
                manager.requestPinAppWidget(ComponentName(context, provider), null, null)
                Toast.makeText(
                    context,
                    if (isEnglish) "Check your home screen or accept the prompt to place the widget"
                    else "উইজেট বসাতে হোম স্ক্রিনের পপআপ নিশ্চিত করুন",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isEnglish) "Please add the widget manually from your home screen"
                    else "অনুগ্রহ করে হোম স্ক্রিন থেকে উইজেটটি ম্যানুয়ালি যুক্ত করুন",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
    ) {
        Icon(Icons.Default.Widgets, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ---------------------------------------------------------------- 1. Prayer timeline

@Composable
private fun PrayerWidgetCard(viewModel: NoorUpViewModel, isEnglish: Boolean) {
    val city by viewModel.selectedCityLocation.collectAsState()
    GlassCard {
        WidgetCardHeader(
            1,
            if (isEnglish) "Prayer Timeline" else "নামাজের সময়সূচী",
            if (isEnglish) "Today's 5 prayers with start and end times, countdown and streak."
            else "আজকের ৫ ওয়াক্তের শুরু-শেষের সময়, কাউন্টডাউন ও স্ট্রিক।",
            isEnglish
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0E1624))
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEnglish) "NoorUp • Prayer Schedule" else "নূরআপ • নামাজের সময়সূচী",
                        color = EmeraldGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEnglish) city.nameEn else city.nameBn,
                        color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1, softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                val rows = listOf(
                    Triple(if (isEnglish) "Fajr" else "ফজর", city.fajrStart, city.fajrEnd),
                    Triple(if (isEnglish) "Dhuhr" else "যোহর", city.dhuhrStart, city.dhuhrEnd),
                    Triple(if (isEnglish) "Asr" else "আসর", city.asrStart, city.asrEnd),
                    Triple(if (isEnglish) "Maghrib" else "মাগরিব", city.maghribStart, city.maghribEnd),
                    Triple(if (isEnglish) "Isha" else "এশা", city.ishaStart, city.ishaEnd)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEachIndexed { index, (name, start, end) ->
                        val current = index == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (current) EmeraldGlow.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.07f))
                                .border(1.dp, if (current) EmeraldGlow else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(name, color = if (current) EmeraldGlow else Color(0xFFF3F4F6), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Spacer(Modifier.height(3.dp))
                                Text(start.replace(" AM", "").replace(" PM", ""), color = if (current) EmeraldGlow else Color(0xFFE5E7EB), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("- " + end.replace(" AM", "").replace(" PM", ""), color = if (current) Color(0xFF6EE7B7) else Color(0xFF9CA3AF), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        WidgetPinButton(
            NoorUpWidgetProvider::class.java, isEnglish,
            if (isEnglish) "Add Prayer Timeline widget" else "নামাজের সময়সূচী উইজেট যুক্ত করুন"
        )
    }
}

// ---------------------------------------------------------------- 2. Daily verse & dua

@Composable
private fun AyatWidgetCard(isEnglish: Boolean) {
    val context = LocalContext.current
    val item by produceState<AyatWidgetItem?>(null, context) {
        value = withContext(Dispatchers.IO) {
            runCatching { DailyAyatContent.itemAt(context, DailyAyatContent.indexForToday(context)) }.getOrNull()
        }
    }
    GlassCard {
        WidgetCardHeader(
            2,
            if (isEnglish) "Daily Verse & Dua" else "আজকের আয়াত ও দুআ",
            if (isEnglish) "A curated Quran verse or Masnoon dua every day, with refresh, save and open."
            else "প্রতিদিন একটি নির্বাচিত আয়াত বা মাসনুন দুআ — রিফ্রেশ, সংরক্ষণ ও খোলার বোতামসহ।",
            isEnglish
        )
        WidgetFrame {
            val shown = item
            val kind = when (shown) {
                is AyatWidgetItem.Dua -> if (isEnglish) "Dua of the Day" else "আজকের দুআ"
                else -> if (isEnglish) "Verse of the Day" else "আজকের আয়াত"
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(kind, color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                if (shown != null) Text(shown.reference, color = Color(0xFFCBD5E1), fontSize = 11.sp, maxLines = 1)
            }
            Spacer(Modifier.height(8.dp))
            if (shown == null) {
                Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = EmeraldGlow)
                }
            } else {
                Text(
                    shown.arabic, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                val translation = if (isEnglish) shown.english.ifBlank { shown.bangla } else shown.bangla.ifBlank { shown.english }
                Text(
                    translation, color = Color(0xFFE2E8F0), fontSize = 12.sp, textAlign = TextAlign.Center,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    if (isEnglish) "⟳ Refresh" else "⟳ নতুন",
                    if (isEnglish) "☆ Save" else "☆ সংরক্ষণ",
                    if (isEnglish) "Open ›" else "খুলুন ›"
                ).forEach { label ->
                    Box(
                        Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1) }
                }
            }
        }
        WidgetPinButton(
            DailyAyatWidgetProvider::class.java, isEnglish,
            if (isEnglish) "Add Daily Verse & Dua widget" else "আজকের আয়াত ও দুআ উইজেট যুক্ত করুন"
        )
    }
}

// ---------------------------------------------------------------- 3. Tasbih counter

@Composable
private fun TasbihWidgetCard(viewModel: NoorUpViewModel, isEnglish: Boolean) {
    val context = LocalContext.current
    val active by viewModel.activeDhikr.collectAsState()
    val count by viewModel.tasbihCount.collectAsState()
    val goal by viewModel.tasbihGoal.collectAsState()
    val completed by viewModel.tasbihCompleted.collectAsState()
    val today = remember(count, active.id) {
        TasbihHistory.loadDay(context.getSharedPreferences("noorup_prefs", android.content.Context.MODE_PRIVATE), LocalDate.now()).total
    }
    fun n(v: Int) = if (isEnglish) "$v" else v.toBanglaDigits()

    GlassCard {
        WidgetCardHeader(
            3,
            if (isEnglish) "Tasbih Counter" else "তসবীহ কাউন্টার",
            if (isEnglish) "Count your dhikr from the home screen and switch zikr with ‹ ›. Every tap is saved to your zikr history."
            else "হোম স্ক্রিন থেকেই যিকির গুনুন, ‹ › দিয়ে যিকির বদলান। প্রতিটি গণনা ইতিহাসে সংরক্ষিত হয়।",
            isEnglish
        )
        WidgetFrame {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(40.dp).height(30.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(active.title(isEnglish), color = Color(0xFF34D399), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(horizontal = 6.dp))
                Box(Modifier.width(40.dp).height(30.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text("›", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.fillMaxWidth()) {
                Text(if (isEnglish) "Today ${n(today)}" else "আজ ${n(today)}", color = Color(0xFFCBD5E1), fontSize = 10.5.sp, maxLines = 1, modifier = Modifier.align(Alignment.TopEnd))
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                Text(n(count), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                Text("/ ${n(goal)}", color = Color(0xFF94A3B8), fontSize = 15.sp, modifier = Modifier.padding(bottom = 5.dp), maxLines = 1)
            }
            LinearProgressIndicator(
                progress = { if (goal > 0) (count.toFloat() / goal).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF34D399), trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text(if (isEnglish) "Reset" else "রিসেট", color = Color.White, fontSize = 11.sp, maxLines = 1)
                }
                Box(Modifier.weight(2f).height(34.dp).clip(RoundedCornerShape(12.dp)).background(EmeraldGlow), contentAlignment = Alignment.Center) {
                    Text(
                        if (completed) (if (isEnglish) "↺ New round" else "↺ নতুন রাউন্ড") else (if (isEnglish) "+ Count" else "+ গণনা"),
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                }
                Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text(if (isEnglish) "Open ›" else "খুলুন ›", color = Color.White, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
        Text(
            if (isEnglish) "Preview shows your current dhikr and count."
            else "প্রিভিউতে আপনার বর্তমান যিকির ও গণনা দেখানো হচ্ছে।",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        WidgetPinButton(
            TasbihWidgetProvider::class.java, isEnglish,
            if (isEnglish) "Add Tasbih Counter widget" else "তসবীহ কাউন্টার উইজেট যুক্ত করুন"
        )
    }
}

// ---------------------------------------------------------------- how to add

@Composable
private fun HowToAddWidgetsCard(isEnglish: Boolean) {
    GlassCard {
        Text(
            if (isEnglish) "✨ How to add a widget to your Home Screen:" else "✨ যেভাবে হোম স্ক্রিনে উইজেট যুক্ত করবেন:",
            color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (isEnglish) "1. Long press any empty space on your Android Home Screen.\n2. Tap 'Widgets' and scroll to 'NoorUp'.\n3. Choose Prayer Timeline, Daily Verse & Dua or Tasbih Counter and drag it onto your screen. You can add all three."
            else "১. আপনার ফোনের হোম স্ক্রিনের ফাঁকা জায়গায় কিছুক্ষণ চেপে ধরে রাখুন।\n২. 'Widgets' অপশনে ট্যাপ করে 'NoorUp' খুঁজে নিন।\n৩. নামাজের সময়সূচী, আজকের আয়াত ও দুআ বা তসবীহ কাউন্টার বেছে স্ক্রিনে টেনে বসান। তিনটিই যুক্ত করা যায়।",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp
        )
    }
}
