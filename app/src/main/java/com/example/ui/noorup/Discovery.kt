package com.example.ui.noorup

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import kotlin.math.roundToInt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Bump [WHATS_NEW_ID] (and the copy in [WhatsNewDialog]) when a release deserves a new "What's new" sheet. */
object DiscoveryContent {
    const val WHATS_NEW_ID = 1
}

private fun bilingual(isEnglish: Boolean, en: String, bn: String) = if (isEnglish) en else bn

// ---------------------------------------------------------------------------------------------
// Home: search bar, launchpad, continue-reading
// ---------------------------------------------------------------------------------------------

/** Big, obvious search entry. Opens the global search (Quran, duas, hadith, assistant). */
@Composable
fun HomeSearchBar(viewModel: NoorUpViewModel, isEnglish: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
            .clickable { viewModel.openGlobalSearch() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(
            bilingual(isEnglish, "Search Quran, duas, hadith…", "কুরআন, দুআ, হাদিস খুঁজুন…"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class LaunchTile(
    val icon: ImageVector,
    val en: String,
    val bn: String,
    val onClick: () -> Unit
)

/** 4 x 2 grid of the features people look for most; the last tile opens the full Explore hub. */
@Composable
fun HomeLaunchpad(viewModel: NoorUpViewModel, isEnglish: Boolean) {
    val tiles = listOf(
        LaunchTile(Icons.Default.MenuBook, "Quran", "কুরআন") { viewModel.selectTab(1) },
        LaunchTile(Icons.Default.Favorite, "Duas", "দুআ") { viewModel.setDuaSegment(0); viewModel.selectTab(2) },
        LaunchTile(Icons.Default.LibraryBooks, "Hadith", "হাদিস") { viewModel.setDuaSegment(1); viewModel.selectTab(2) },
        LaunchTile(Icons.Default.SmartToy, "Ask AI", "জিজ্ঞাসা") { viewModel.selectTab(4) },
        LaunchTile(Icons.Default.TouchApp, "Tasbih", "তসবীহ") { viewModel.openTool(1) },
        LaunchTile(Icons.Default.Explore, "Qibla", "কিবলা") { viewModel.openTool(3) },
        LaunchTile(Icons.Default.AccountBalanceWallet, "Zakat", "যাকাত") { viewModel.openTool(5) },
        LaunchTile(Icons.Default.Apps, "More", "আরও") { viewModel.openExploreHub() }
    )
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tiles.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tile ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tile.onClick()
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tile.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            bilingual(isEnglish, tile.en, tile.bn),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** After a couple of days of use, suggests the home-screen widget once. Hidden after dismissal. */
@Composable
fun HomeWidgetHint(viewModel: NoorUpViewModel, isEnglish: Boolean) {
    var visible by remember { mutableStateOf(!viewModel.hintSeen("home_widget") && viewModel.installAgeDays() >= 2) }
    // One nudge at a time: wait until the prayer-alarm setup card is out of the way.
    val alarmSetupDone by viewModel.isPrayerAlarmOnboardingComplete.collectAsState()
    if (!visible || !alarmSetupDone) return
    Column {
        Spacer(Modifier.height(12.dp))
        HintBubble(
            text = bilingual(
                isEnglish,
                "Add the NoorUp widget to see prayer times without opening the app.",
                "অ্যাপ না খুলেই নামাজের সময় দেখতে নূরআপ উইজেট যোগ করুন।"
            ),
            dismissLabel = bilingual(isEnglish, "Set up", "চালু করুন"),
            onDismiss = {
                viewModel.markHintSeen("home_widget")
                visible = false
                viewModel.openTool(6)
            },
            secondaryLabel = bilingual(isEnglish, "Not now", "এখন নয়"),
            onSecondary = { viewModel.markHintSeen("home_widget"); visible = false }
        )
    }
}

/** One-tap resume (or a gentle first step for new readers). */
@Composable
fun HomeContinueCard(viewModel: NoorUpViewModel, isEnglish: Boolean) {
    val resume by viewModel.readerResume.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val resumeSurah = resume?.let { (n, _) -> surahs.find { it.number == n } }
    val ayah = resume?.second
    val target = resumeSurah ?: surahs.firstOrNull() ?: return
    val isResume = resumeSurah != null && ayah != null

    GlassCard(
        modifier = Modifier.clickable {
            viewModel.selectTab(1)
            if (isResume) viewModel.openSurahAt(target.number, ayah!!) else viewModel.openSurah(target.number)
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bilingual(
                        isEnglish,
                        if (isResume) "Continue reading" else "Start reading the Qur'an",
                        if (isResume) "পড়া চালিয়ে যান" else "কুরআন পড়া শুরু করুন"
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                val name = if (isEnglish) target.nameEnglish else target.nameBangla
                Text(
                    if (isResume) {
                        if (isEnglish) "$name • Ayat $ayah" else "$name • আয়াত ${ayah!!.toBanglaDigits()}"
                    } else name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Explore hub (replaces the bare tab strip as the Tools landing page)
// ---------------------------------------------------------------------------------------------

private data class HubItem(
    val icon: ImageVector,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String,
    val onClick: () -> Unit
)

private data class HubSection(val titleEn: String, val titleBn: String, val items: List<HubItem>)

/** Grouped, described cards for every feature. [onOpenTool] receives a [ToolTabOrder] id. */
@Composable
fun ExploreHub(viewModel: NoorUpViewModel, isEnglish: Boolean, onOpenTool: (Int) -> Unit) {
    val sections = listOf(
        HubSection(
            "Worship", "ইবাদত",
            listOf(
                HubItem(Icons.Default.NotificationsActive, "Prayer Alerts", "ওয়াক্ত অ্যালার্ট", "Azan reminders for every prayer", "প্রতি ওয়াক্তে আযানের রিমাইন্ডার") { onOpenTool(0) },
                HubItem(Icons.Default.Explore, "Qibla", "কিবলা কম্পাস", "Live compass to the Kaaba", "কাবার দিক দেখানো লাইভ কম্পাস") { onOpenTool(3) },
                HubItem(Icons.Default.NightsStay, "Ramadan", "রমজান", "Sehri, Iftar and fasting tracker", "সাহরি, ইফতার ও রোযা ট্র্যাকার") { onOpenTool(7) },
                HubItem(Icons.Default.AccountBalanceWallet, "Zakat", "যাকাত", "Calculate what you owe", "আপনার যাকাত হিসাব করুন") { onOpenTool(5) }
            )
        ),
        HubSection(
            "Tasbih & Dhikr", "তসবীহ ও যিকর",
            listOf(
                HubItem(Icons.Default.TouchApp, "Digital Tasbih", "ডিজিটাল তসবীহ", "Count dhikr with goals and rounds", "লক্ষ্যসহ যিকর গণনা করুন") { onOpenTool(1) },
                HubItem(Icons.Default.Groups, "Family Dhikr", "পারিবারিক যিকর", "Share dhikr goals with loved ones", "প্রিয়জনদের সাথে যিকরের লক্ষ্য শেয়ার করুন") { onOpenTool(8) }
            )
        ),
        HubSection(
            "Read & learn", "পড়ুন ও জানুন",
            listOf(
                HubItem(Icons.Default.MenuBook, "Quran", "কুরআন", "114 surahs with translation", "অনুবাদসহ ১১৪টি সূরা") { viewModel.selectTab(1) },
                HubItem(Icons.Default.Headphones, "Quran Audio", "কুরআন তিলাওয়াত", "Listen ayah by ayah", "আয়াত ধরে ধরে শুনুন") { viewModel.selectTab(1) },
                HubItem(Icons.Default.Favorite, "Duas", "দুআ", "Daily supplications", "দৈনন্দিন দুআ") { viewModel.setDuaSegment(0); viewModel.selectTab(2) },
                HubItem(Icons.Default.LibraryBooks, "Hadith Library", "হাদিস ভাণ্ডার", "Browse and search hadith books", "হাদিস গ্রন্থ ব্রাউজ ও সার্চ") { viewModel.setDuaSegment(1); viewModel.selectTab(2) },
                HubItem(Icons.Default.SmartToy, "AI Assistant", "এআই সহকারী", "Ask Islamic questions", "ইসলামিক প্রশ্ন করুন") { viewModel.selectTab(4) },
                HubItem(Icons.Default.Search, "Search everything", "সবকিছু খুঁজুন", "Quran, duas and hadith at once", "কুরআন, দুআ ও হাদিস একসাথে") { viewModel.openGlobalSearch() }
            )
        ),
        HubSection(
            "Utilities & progress", "টুলস ও অগ্রগতি",
            listOf(
                HubItem(Icons.Default.BarChart, "Stats", "পরিসংখ্যান", "Prayer and reading streaks", "নামাজ ও তিলাওয়াতের ধারাবাহিকতা") { viewModel.selectTab(5) },
                HubItem(Icons.Default.CalendarMonth, "Hijri Calendar", "হিজরি ক্যালেন্ডার", "Dates and Islamic events", "তারিখ ও ইসলামিক দিবস") { onOpenTool(4) },
                HubItem(Icons.Default.WbSunny, "Solar Engine", "সৌর ইঞ্জিন", "Sun-based prayer calibration", "সূর্য-ভিত্তিক ওয়াক্ত ক্যালিব্রেশন") { onOpenTool(2) },
                HubItem(Icons.Default.Widgets, "Home Widget", "হোম উইজেট", "Prayer times, daily verse and tasbih widgets", "নামাজের সময়, আয়াত ও তসবীহ উইজেট") { onOpenTool(6) },
                HubItem(Icons.Default.CloudUpload, "Backup", "ব্যাকআপ", "Encrypted Google Drive backup", "এনক্রিপ্টেড গুগল ড্রাইভ ব্যাকআপ") { onOpenTool(9) }
            )
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    bilingual(isEnglish, section.titleEn, section.titleBn),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                section.items.chunked(2).forEach { pair ->
                    if (pair.size == 1) {
                        HubCard(pair[0], isEnglish, Modifier.fillMaxWidth())
                    } else {
                        // Equal heights per row even when one description wraps to a second line.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            pair.forEach { item -> HubCard(item, isEnglish, Modifier.weight(1f).fillMaxHeight()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubCard(item: HubItem, isEnglish: Boolean, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .heightIn(min = 112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                item.onClick()
            }
            .padding(14.dp)
    ) {
        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            bilingual(isEnglish, item.titleEn, item.titleBn),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            bilingual(isEnglish, item.descEn, item.descBn),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------------------------
// First-run tour and What's new
// ---------------------------------------------------------------------------------------------

private data class TourPage(
    val icon: ImageVector,
    val titleEn: String,
    val titleBn: String,
    val bodyEn: String,
    val bodyBn: String
)

private val tourPages = listOf(
    TourPage(
        Icons.Default.NightsStay,
        "Welcome to NoorUp", "নূরআপে স্বাগতম",
        "Your daily Islamic companion: prayer times, Quran, duas, hadith and more. Choose your language to begin.",
        "আপনার দৈনন্দিন ইসলামিক সঙ্গী: নামাজের সময়, কুরআন, দুআ, হাদিস এবং আরও অনেক কিছু। শুরু করতে ভাষা বেছে নিন।"
    ),
    TourPage(
        Icons.Default.NotificationsActive,
        "Prayer times that fit you", "আপনার জন্য সঠিক ওয়াক্ত",
        "Tap the location pill at the top of Home to pick your city or use GPS. Turn on prayer alerts on Home so you never miss a prayer.",
        "হোমের উপরের লোকেশনে ট্যাপ করে শহর বা জিপিএস বেছে নিন। কোনো ওয়াক্ত যেন বাদ না যায়, তাই হোম থেকে অ্যালার্ট চালু করুন।"
    ),
    TourPage(
        Icons.Default.MenuBook,
        "Read, listen, remember", "পড়ুন, শুনুন, মনে রাখুন",
        "The Quran has large Arabic text with A+ / A− size control, ayah-by-ayah audio, Juz browsing, Bengali pronunciation, bookmarks and sepia or night reading themes.",
        "কুরআনে আছে বড় আরবি লেখা ও A+ / A− নিয়ন্ত্রণ, আয়াত ধরে ধরে তিলাওয়াত, পারা ব্রাউজ, বাংলা উচ্চারণ, বুকমার্ক এবং সেপিয়া বা নাইট রিডিং থিম।"
    ),
    TourPage(
        Icons.Default.Apps,
        "Everything in Explore", "সবকিছু এক্সপ্লোরে",
        "Tasbih, Qibla, Zakat, Ramadan, Family Dhikr, Hijri calendar, widget and backup live in the Explore tab. The search bar on Home finds any Quran verse, dua or hadith.",
        "তসবীহ, কিবলা, যাকাত, রমজান, পারিবারিক যিকর, হিজরি ক্যালেন্ডার, উইজেট ও ব্যাকআপ আছে এক্সপ্লোর ট্যাবে। হোমের সার্চ বার থেকে যেকোনো আয়াত, দুআ বা হাদিস খুঁজুন।"
    )
)

@Composable
fun OnboardingTour(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    var page by remember { mutableIntStateOf(0) }
    val last = tourPages.lastIndex

    Dialog(
        onDismissRequest = { viewModel.finishTour() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.finishTour() }) {
                            Text(bilingual(isEnglish, "Skip", "এড়িয়ে যান"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Crossfade(targetState = page, label = "tour", modifier = Modifier.weight(1f)) { index ->
                        val p = tourPages[index]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(p.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            }
                            Spacer(Modifier.height(28.dp))
                            Text(
                                bilingual(isEnglish, p.titleEn, p.titleBn),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                bilingual(isEnglish, p.bodyEn, p.bodyBn),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center
                            )
                            if (index == 0) {
                                Spacer(Modifier.height(24.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LanguageChoice("English", selected = isEnglish) { if (!isEnglish) viewModel.toggleLanguage() }
                                    LanguageChoice("বাংলা", selected = !isEnglish) { if (isEnglish) viewModel.toggleLanguage() }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        tourPages.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (i == page) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == page) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        if (page > 0) {
                            OutlinedButton(
                                onClick = { page -= 1 },
                                modifier = Modifier.heightIn(min = 52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text(bilingual(isEnglish, "Back", "পিছনে")) }
                        }
                        Button(
                            onClick = { if (page < last) page += 1 else viewModel.finishTour() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                if (page < last) bilingual(isEnglish, "Next", "পরবর্তী")
                                else bilingual(isEnglish, "Get started", "শুরু করুন"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun WhatsNewDialog(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val items = listOf(
        Triple(Icons.Default.MenuBook, bilingual(isEnglish, "Bigger Quran text", "বড় কুরআনের লেখা"),
            bilingual(isEnglish, "Larger Arabic with A+ / A− controls, Juz browsing, and sepia or night reading themes.", "বড় আরবি লেখা, A+ / A− নিয়ন্ত্রণ, পারা ব্রাউজ এবং সেপিয়া বা নাইট রিডিং থিম।")),
        Triple(Icons.Default.Apps, bilingual(isEnglish, "New Home and Explore", "নতুন হোম ও এক্সপ্লোর"),
            bilingual(isEnglish, "Quick-access grid, one-tap continue reading, and Explore with every feature described in one place.", "দ্রুত অ্যাক্সেস গ্রিড, এক ট্যাপে পড়া চালিয়ে যাওয়া এবং সব ফিচার একসাথে এক্সপ্লোরে।")),
        Triple(Icons.Default.Search, bilingual(isEnglish, "Search from Home", "হোম থেকে সার্চ"),
            bilingual(isEnglish, "Find any verse, dua or hadith from the search bar at the top of Home.", "হোমের উপরের সার্চ বার থেকে যেকোনো আয়াত, দুআ বা হাদিস খুঁজুন।"))
    )
    AlertDialog(
        onDismissRequest = { viewModel.dismissWhatsNew() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(bilingual(isEnglish, "What's new in NoorUp", "নূরআপে নতুন কী"), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items.forEach { (icon, title, body) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissWhatsNew() }) {
                Text(bilingual(isEnglish, "Got it", "বুঝেছি"), fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ---------------------------------------------------------------------------------------------
// Shared polish: one-time hint, empty state, skeleton loader
// ---------------------------------------------------------------------------------------------

/**
 * Small dismissible tip. Show it while `!viewModel.hintSeen(key)` and call [onDismiss] to remember it.
 * Actions sit under the text so long Bengali copy never gets squeezed by a side button.
 */
@Composable
fun HintBubble(
    text: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(start = 14.dp, top = 10.dp, bottom = 2.dp, end = 6.dp)
    ) {
        Text(
            "💡 $text",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary) {
                    Text(secondaryLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/** Selected chips use the emerald container from the theme (the stock tonal purple clashes with the brand). */
@Composable
fun brandChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
)

/** Quran reading preferences in one place: text size (with live preview), reader theme, Bengali pronunciation. */
@Composable
fun ReaderSettingsDialog(
    viewModel: NoorUpViewModel,
    isEnglish: Boolean,
    scale: Float,
    theme: Int,
    showPronunciation: Boolean,
    onShowSource: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(bilingual(isEnglish, "Reading settings", "পড়ার সেটিংস"), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuranBasmalaHeader(fontSize = (READER_ARABIC_BASE_SP * scale).sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        bilingual(isEnglish, "Arabic size", "আরবি লেখার আকার"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.adjustReaderArabicScale(-READER_ARABIC_SCALE_STEP) },
                        enabled = scale > READER_ARABIC_SCALE_MIN + 0.001f,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                    ) { Text("A−", fontWeight = FontWeight.Bold) }
                    Text(
                        "${(scale * 100).roundToInt()}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(52.dp)
                    )
                    OutlinedButton(
                        onClick = { viewModel.adjustReaderArabicScale(READER_ARABIC_SCALE_STEP) },
                        enabled = scale < READER_ARABIC_SCALE_MAX - 0.001f,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                    ) { Text("A+", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(bilingual(isEnglish, "Reading theme", "পড়ার থিম"), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Text(
                        bilingual(
                            isEnglish,
                            "Quran tab only. Auto follows the app's light/dark setting.",
                            "শুধু কুরআন ট্যাবে প্রযোজ্য। অটো অ্যাপের লাইট/ডার্ক সেটিং অনুসরণ করে।"
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to bilingual(isEnglish, "Auto", "অটো"),
                            1 to bilingual(isEnglish, "Sepia", "সেপিয়া"),
                            2 to bilingual(isEnglish, "Night", "নাইট")
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = theme == id,
                                onClick = { viewModel.setReaderTheme(id) },
                                label = { Text(label) },
                                colors = brandChipColors()
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            bilingual(isEnglish, "Bengali pronunciation", "বাংলা উচ্চারণ"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        TextButton(onClick = onShowSource, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                            Text(bilingual(isEnglish, "Source", "সূত্র"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                    Switch(checked = showPronunciation, onCheckedChange = { viewModel.toggleQuranTransliteration() })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(bilingual(isEnglish, "Done", "ঠিক আছে"), fontWeight = FontWeight.Bold) }
        }
    )
}

/** Consistent empty state: icon, headline, helper line and an optional call to action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Pulsing placeholder bars shown while a surah's verses load, instead of a bare spinner. */
@Composable
fun VerseSkeleton(count: Int = 3) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeat(),
        label = "skeletonAlpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            GlassCard {
                Column(modifier = Modifier.alpha(alpha), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBar(widthFraction = 0.12f, height = 22.dp)
                    SkeletonBar(widthFraction = 1f, height = 28.dp)
                    SkeletonBar(widthFraction = 0.8f, height = 28.dp)
                    SkeletonBar(widthFraction = 0.9f, height = 14.dp)
                }
            }
        }
    }
}

private fun infiniteRepeat() = infiniteRepeatable<Float>(tween(900, easing = LinearEasing), RepeatMode.Reverse)

@Composable
private fun SkeletonBar(widthFraction: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    )
}
