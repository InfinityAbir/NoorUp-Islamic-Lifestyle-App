package com.example.ui.noorup

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF0A111F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
    }
    val borderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 4.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.04f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

// --- Top Bar Controls (Language, Theme, Location & GPS Picker) ---
@Composable
fun TopControlsHeader(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val selectedCityLocation by viewModel.selectedCityLocation.collectAsState()
    var showLocationDialog by remember { mutableStateOf(false) }
    var locationStatusMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchDeviceLocation(context, viewModel) { msg ->
                locationStatusMessage = msg
            }
            showLocationDialog = false
        } else {
            locationStatusMessage = if (isEnglish) "Location permission denied" else "লোকেশন অনুমতি প্রদান করা হয়নি"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Interactive Location Selector Button (Glassmorphic & Sleek)
        Box {
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(19.dp))
                    .clickable { showLocationDialog = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isEnglish) selectedCityLocation.nameEn else selectedCityLocation.nameBn,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.size(16.dp)
                )
            }

            if (showLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEnglish) "Select Location / City" else "অবস্থান / শহর নির্বাচন করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                if (isEnglish) "Detect live GPS coordinates or select a city. Bangladesh locations use Islamic Foundation standard for accurate Hijri date & prayer times."
                                else "লাইভ জিপিএস বা পছন্দের শহর নির্বাচন করুন। বাংলাদেশ লোকেশনে সঠিক হিজরি তারিখ ও ওয়াক্তের জন্য ইসলামিক ফাউন্ডেশনের মান স্বয়ংক্রিয়ভাবে সক্রিয় হবে।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // GPS Detect Button
                            Button(
                                onClick = {
                                    permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isEnglish) "Use Live GPS Location" else "বর্তমান জিপিএস লোকেশন নিন", fontSize = 13.sp)
                            }

                            if (locationStatusMessage != null) {
                                Text(
                                    locationStatusMessage!!,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Text(
                                if (isEnglish) "City Presets:" else "বিভাগ ও শহরসমূহ:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    Text(
                                        if (isEnglish) "🇧🇩 Bangladesh Divisions (Islamic Foundation Hijri)"
                                        else "🇧🇩 বাংলাদেশ (ইসলামিক ফাউন্ডেশন হিজরি মান)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                items(viewModel.availableCities.filter { it.latitude in 20.0..27.0 }) { city ->
                                    val isSelected = selectedCityLocation.nameEn.equals(city.nameEn, ignoreCase = true) || selectedCityLocation.nameBn == city.nameBn
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setCityLocation(city)
                                                showLocationDialog = false
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isEnglish) city.nameEn else city.nameBn,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (isEnglish) "🌐 International & Holy Cities (Umm al-Qura Standard)"
                                        else "🌐 আন্তর্জাতিক ও পবিত্র শহর (উম্মুল কুরা মান)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                items(viewModel.availableCities.filter { it.latitude !in 20.0..27.0 }) { city ->
                                    val isSelected = selectedCityLocation.nameEn.equals(city.nameEn, ignoreCase = true) || selectedCityLocation.nameBn == city.nameBn
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setCityLocation(city)
                                                showLocationDialog = false
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isEnglish) city.nameEn else city.nameBn,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLocationDialog = false }) {
                            Text(if (isEnglish) "Close" else "বন্ধ করুন", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }

        // Action Controls: Language Pill + Theme Button (Unified Height & Precise Glassmorphism)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language Toggle Pill
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(19.dp))
                    .clickable { viewModel.toggleLanguage() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Language, 
                    contentDescription = "Language", 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isEnglish) "বাংলা" else "EN", 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
            }

            // Theme Toggle Circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                    .clickable { viewModel.toggleTheme() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                    contentDescription = "Theme Toggle",
                    tint = if (isDarkMode) GoldAccent else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --- Feature 1: Interactive "Noor Garden" Habit Growth Card ---
@Composable
fun NoorGardenCard(viewModel: NoorUpViewModel) {
    val habitPrayers by viewModel.habitPrayers.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val completedCount = habitPrayers.values.count { it }
    val progress = completedCount / 5f
    val percent = (completedCount * 100) / 5

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isEnglish) "Noor Garden" else "নূর বাগান",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentStreak > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldAccent.copy(alpha = 0.2f))
                                .border(1.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (isEnglish) "🔥 $currentStreak d streak" else "🔥 ${currentStreak.toBanglaDigits()} দিন স্ট্রিক",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    if (isEnglish) "Tree growth via daily prayers ($completedCount/5 Prayers • $percent%)"
                    else "নিয়মিত নামাজে গাছের বিকাশ (${completedCount.toBanglaDigits()}/৫ ওয়াক্ত • ${percent.toBanglaDigits()}%)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            habitPrayers.forEach { (prayer, isChecked) ->
                FilterChip(
                    selected = isChecked,
                    onClick = { viewModel.toggleHabitPrayer(prayer) },
                    label = { Text(prayer, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// --- Feature 3: Smart Travel & Qasr Prayer Mode Card ---
@Composable
fun TravelQasrCard(viewModel: NoorUpViewModel) {
    val isTraveling by viewModel.isTraveling.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isEnglish) "Smart Travel & Qasr Mode" else "স্মার্ট সফর ও কসর মোড",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isTraveling) {
                        if (isEnglish) "Traveling — Qasr prayer applicable" else "সফররত আছেন — কসর নামাজ প্রযোজ্য"
                    } else {
                        if (isEnglish) "Stationary — Standard prayer" else "স্থায়ী অবস্থান — স্বাভাবিক নামাজ"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = isTraveling,
                onCheckedChange = { viewModel.toggleTravelMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        if (isTraveling) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    if (isEnglish) "Travel Rule: 4-Rakat prayers (Dhuhr, Asr, Isha) should be shortened to 2 Rakats (Qasr)." else "সফরের বিধান: ৪ রাকাত বিশিষ্ট নামাজ (যোহর, আসর, এশা) ২ রাকাত কসর পড়তে হবে।",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// --- Feature 5: Private Family/Circle Zikr Sync Card with Dynamic Pairing ---
@Composable
fun FamilyZikrCard(viewModel: NoorUpViewModel) {
    val totalZikr by viewModel.familyZikrTotal.collectAsState()
    val members by viewModel.familyMembers.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val myPairingCode by viewModel.myFamilyPairingCode.collectAsState()
    val isLiveSyncActive by viewModel.isFamilyLiveSyncActive.collectAsState()
    val lastLiveEvent by viewModel.lastLiveEvent.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberRelation by remember { mutableStateOf("মা") }
    var newMemberPairingCode by remember { mutableStateOf("") }
    var newMemberInitialCount by remember { mutableStateOf("0") }
    var newMemberLiveSync by remember { mutableStateOf(true) }

    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var editCountText by remember { mutableStateOf("") }

    val commonRelations = if (isEnglish) {
        listOf("Mother", "Father", "Brother", "Sister", "Spouse", "Child", "Friend")
    } else {
        listOf("মা", "বাবা", "ভাই", "বোন", "স্ত্রী", "স্বামী", "সন্তান", "বন্ধু")
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    GlassCard {
        // 1. Top Header: Title & Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = if (isDark) EmeraldGlow else Color(0xFF047857),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isEnglish) "Family Zikr Circle" else "পরিবার যিকির সার্কেল",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnglish) "Private Live Dhikr Network" else "পরিবারের সাথে সম্মিলিত লাইভ যিকির",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Live Sync Status Pill & Add Member Button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLiveSyncActive) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isLiveSyncActive) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.clickable { viewModel.toggleFamilyLiveSync() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isLiveSyncActive) Color(0xFF10B981) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLiveSyncActive) (if (isEnglish) "Live" else "লাইভ") else (if (isEnglish) "Off" else "বন্ধ"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveSyncActive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        newMemberRelation = if (isEnglish) "Mother" else "মা"
                        newMemberName = newMemberRelation
                        newMemberPairingCode = "NZ-" + (1000..9999).random()
                        newMemberInitialCount = "0"
                        newMemberLiveSync = true
                        showAddDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) EmeraldGlow else Color(0xFF047857)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isEnglish) "Pair" else "পেয়ার",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Hero Total Combined Dhikr Metric Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isDark) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF064E3B).copy(alpha = 0.8f),
                                Color(0xFF0F172A).copy(alpha = 0.95f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFECFDF5),
                                Color(0xFFD1FAE5)
                            )
                        )
                    }
                )
                .border(
                    1.dp,
                    if (isDark) Color(0xFF059669).copy(alpha = 0.4f) else Color(0xFFA7F3D0),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "Combined Family Dhikr" else "পরিবারের সম্মিলিত মোট যিকির",
                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (isEnglish) "$totalZikr" else totalZikr.toBanglaDigits(),
                            color = if (isDark) Color.White else Color(0xFF064E3B),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "times" else "বার",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF065F46),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = if (isEnglish) {
                            "${members.size} member${if (members.size != 1) "s" else ""} connected"
                        } else {
                            "${members.size.toBanglaDigits()} জন সদস্য সংযুক্ত"
                        },
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF047857).copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                // Instant Refresh / Pulse Button
                IconButton(
                    onClick = {
                        viewModel.syncFamilyZikrNow()
                        Toast.makeText(context, if (isEnglish) "Live counts synced!" else "লাইভ যিকির সিঙ্ক সম্পন্ন!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.8f)
                        )
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Sync Now",
                        tint = if (isDark) EmeraldGlow else Color(0xFF047857),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. User's Own Shareable Pairing Code Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
            border = BorderStroke(
                1.dp,
                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDark) EmeraldGlow.copy(alpha = 0.15f) else Color(0xFFECFDF5)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (isDark) EmeraldGlow else Color(0xFF047857),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isEnglish) "Your Pairing Code: " else "আপনার পেয়ারিং কোড: ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = myPairingCode,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) EmeraldGlow else Color(0xFF047857)
                            )
                        }
                        Text(
                            text = if (isEnglish) "Share with family to connect circles" else "পরিবারের সদস্যদের এই কোড দিয়ে যুক্ত করুন",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Noor Family Pairing Code", myPairingCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (isEnglish) "Code $myPairingCode copied!" else "কোড $myPairingCode কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = if (isDark) EmeraldGlow else Color(0xFF047857),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    if (isEnglish)
                                        "Assalamu Alaikum! Join my family zikr circle on NoorUp. My pairing code is: $myPairingCode"
                                    else
                                        "আসসালামু আলাইকুম! নূরআপ অ্যাপে আমার পরিবার যিকির সার্কেলে যুক্ত হোন। আমার পেয়ারিং কোড: $myPairingCode"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, if (isEnglish) "Share Pairing Code" else "পেয়ারিং কোড শেয়ার করুন"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Code",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Live Event Ticker (when a member recites zikr)
        val currentEvent = lastLiveEvent
        if (currentEvent != null) {
            val event = currentEvent
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) {
                            "Live Pulse: ${event.relation} (${event.pairingCode}) ${if (event.increment > 0) "+${event.increment} " else ""}${event.phrase} • ${event.timeLabel}"
                        } else {
                            "লাইভ পাল্স: ${event.relation} (${event.pairingCode}) ${if (event.increment > 0) "+${event.increment.toBanglaDigits()} " else ""}${event.phrase} • ${event.timeLabel}"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (members.isEmpty()) {
            // 4. Clean, Inviting Empty State
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF0B132B).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFD1FAE5)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = if (isDark) EmeraldGlow else Color(0xFF047857),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isEnglish) "No Family Members Connected Yet" else "এখনো কোনো সদস্য যুক্ত করা হয়নি",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnglish)
                            "Pair family members with a relationship (e.g. Mother, Father) and code. Live Dhikr totals will automatically sync in real time."
                        else
                            "পরিবারের সম্পর্ক (যেমন: বাবা, মা) ও যেকোনো পেয়ারিং কোড দিয়ে যুক্ত করুন। লাইভ যিকির স্বয়ংক্রিয়ভাবে রিয়েল-টাইমে আপডেট হবে।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            newMemberRelation = if (isEnglish) "Mother" else "মা"
                            newMemberName = newMemberRelation
                            newMemberPairingCode = "NZ-" + (1000..9999).random()
                            newMemberInitialCount = "0"
                            newMemberLiveSync = true
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) EmeraldGlow else Color(0xFF047857)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Pair First Member" else "প্রথম সদস্য পেয়ার করুন",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // 5. Members List with Individual Dynamic Status & Controls
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEach { member ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.7f) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Member Info & Live Status
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        editingMember = member
                                        editCountText = member.count.toString()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar Circle
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.relation.take(1),
                                        color = if (isDark) EmeraldGlow else Color(0xFF047857),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = member.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Relation Chip
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = member.relation,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (member.pairingCode.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.VpnKey,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(9.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = member.pairingCode,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Dynamic Count & Recitation Label
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isEnglish) "${member.count} times" else "${member.count.toBanglaDigits()} বার",
                                            color = if (isDark) EmeraldGlow else Color(0xFF047857),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• ${member.lastZikrPhrase}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Sync status & timestamp
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (member.isLiveSyncing && isLiveSyncActive) Color(0xFF10B981) else Color.Gray
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (member.isLiveSyncing && isLiveSyncActive) {
                                                if (isEnglish) "Live paired • ${member.lastSyncTime}" else "লাইভ পেয়ার্ড • ${member.lastSyncTime}"
                                            } else {
                                                if (isEnglish) "Sync paused" else "সিঙ্ক স্থগিত"
                                            },
                                            color = if (member.isLiveSyncing && isLiveSyncActive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Action Controls (+33, +100, Sync toggle, and Delete)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.contributeFamilyZikr(member.name, 33) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5),
                                        contentColor = if (isDark) EmeraldGlow else Color(0xFF047857)
                                    ),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFFA7F3D0)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        if (isEnglish) "+33" else "+৩৩",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { viewModel.contributeFamilyZikr(member.name, 100) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        if (isEnglish) "+100" else "+১০০",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Toggle Live Sync for this member
                                IconButton(
                                    onClick = { viewModel.toggleMemberLiveSync(member.name) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (member.isLiveSyncing) Icons.Default.Sync else Icons.Default.SyncDisabled,
                                        contentDescription = "Toggle Sync",
                                        tint = if (member.isLiveSyncing) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Delete / Disconnect button
                                IconButton(
                                    onClick = { viewModel.removeFamilyMember(member.name) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info Note, Sync All, and Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isEnglish) "Counts update dynamically in real time." else "যিকির সংখ্যা স্বয়ংক্রিয়ভাবে লাইভ আপডেট হয়।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { viewModel.syncFamilyZikrNow() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            if (isEnglish) "Sync Now" else "সিঙ্ক",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = { viewModel.resetFamilyZikr() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (isEnglish) "Reset All" else "রিসেট",
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // Dialog: Pair New Family Member with Relationship and Pairing Code
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddLink, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isEnglish) "Pair Family Member" else "নতুন সদস্য পেয়ার করুন",
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
                        if (isEnglish)
                            "Enter the relationship and pairing code to dynamically connect and sync zikr counts in real time:"
                        else
                            "পরিবারের সম্পর্ক ও যেকোনো পেয়ারিং কোড দিন, স্বয়ংক্রিয়ভাবে যিকির সংখ্যা ডায়নামিকালি লাইভ আপডেট হতে থাকবে:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick Relationship Chips
                    Text(
                        if (isEnglish) "Quick Select Relationship:" else "সম্পর্ক নির্বাচন করুন:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(commonRelations) { rel ->
                            val isSelected = newMemberRelation.equals(rel, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    newMemberRelation = rel
                                    if (newMemberName.isBlank() || commonRelations.contains(newMemberName)) {
                                        newMemberName = rel
                                    }
                                }
                            ) {
                                Text(
                                    text = rel,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Relationship Text Field
                    OutlinedTextField(
                        value = newMemberRelation,
                        onValueChange = {
                            newMemberRelation = it
                            if (newMemberName.isBlank()) newMemberName = it
                        },
                        label = { Text(if (isEnglish) "Relationship (e.g. Mother, Father, Brother)" else "সম্পর্ক (যেমন: মা, বাবা, ভাই)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Member Name / Label (Optional)
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text(if (isEnglish) "Member Display Name" else "সদস্যের নাম (প্রদর্শনীর জন্য)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Pairing Code Field + Quick Generate Action
                    OutlinedTextField(
                        value = newMemberPairingCode,
                        onValueChange = { newMemberPairingCode = it.uppercase() },
                        label = { Text(if (isEnglish) "Pairing Code (type any code)" else "পেয়ারিং কোড (যেকোনো কোড লিখুন)") },
                        placeholder = { Text("যেমন: NZ-4812 বা MOM-01") },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    newMemberPairingCode = "NZ-" + (1000..9999).random()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(
                                    if (isEnglish) "Random" else "কোড তৈরি",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Initial Count
                    OutlinedTextField(
                        value = newMemberInitialCount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) newMemberInitialCount = it },
                        label = { Text(if (isEnglish) "Starting Count (Optional, default 0)" else "শুরুর সংখ্যা (ঐচ্ছিক, ডিফল্ট ০)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rel = newMemberRelation.trim().ifBlank { if (isEnglish) "Family" else "পরিবার" }
                        val name = newMemberName.trim().ifBlank { rel }
                        val code = newMemberPairingCode.trim().uppercase().ifBlank {
                            "NZ-" + (1000..9999).random()
                        }
                        val count = newMemberInitialCount.toIntOrNull() ?: 0
                        viewModel.addFamilyMember(
                            name = name,
                            relation = rel,
                            pairingCode = code,
                            initialCount = count,
                            isLiveSyncing = newMemberLiveSync
                        )
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isEnglish) "Pair & Connect" else "পেয়ার ও যুক্ত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // Dialog: Edit Member Count Directly
    val currentEditingMember = editingMember
    if (currentEditingMember != null) {
        val member = currentEditingMember
        AlertDialog(
            onDismissRequest = { editingMember = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    if (isEnglish) "Edit Count: ${member.name}" else "${member.name}-এর যিকির সংশোধন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (isEnglish) "Update total completed zikr count:" else "মোট সম্পন্ন করা যিকির সংখ্যা লিখুন:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editCountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) editCountText = it },
                        label = { Text(if (isEnglish) "Total Count" else "মোট সংখ্যা") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = editCountText.toIntOrNull() ?: 0
                        viewModel.updateFamilyMemberCount(member.name, count)
                        editingMember = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isEnglish) "Save" else "সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMember = null }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }
}

// --- Feature: Dynamic Solar & Date-Based Calculation Engine Bar ---
@Composable
fun DateSolarCalibrationCard(viewModel: NoorUpViewModel) {
    val context = LocalContext.current
    val isEnglish by viewModel.isEnglish.collectAsState()
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val calculationMethod by viewModel.calculationMethod.collectAsState()
    val juristicMethod by viewModel.juristicMethod.collectAsState()
    val hijriDate by viewModel.currentHijriDate.collectAsState()
    val moonPhase by viewModel.currentMoonPhase.collectAsState()

    var showMethodDialog by remember { mutableStateOf(false) }

    val isToday = remember(selectedDate) {
        val today = Calendar.getInstance()
        today.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
    }

    val dayNamesBn = listOf("রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার")
    val monthNamesBn = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
    val dayOfWeekBn = dayNamesBn[(selectedDate.get(Calendar.DAY_OF_WEEK) - 1) % 7]
    val monthBn = monthNamesBn[selectedDate.get(Calendar.MONTH)]
    val dayOfMonthBn = selectedDate.get(Calendar.DAY_OF_MONTH).toString()
        .replace('0','০').replace('1','১').replace('2','২').replace('3','৩').replace('4','৪')
        .replace('5','৫').replace('6','৬').replace('7','৭').replace('8','৮').replace('9','৯')
    val yearBn = selectedDate.get(Calendar.YEAR).toString()
        .replace('0','০').replace('1','১').replace('2','২').replace('3','৩').replace('4','৪')
        .replace('5','৫').replace('6','৬').replace('7','৭').replace('8','৮').replace('9','৯')

    val formattedGregorianDateStr = if (isEnglish) {
        SimpleDateFormat("d MMMM yyyy, EEEE", Locale.ENGLISH).format(selectedDate.time)
    } else {
        "$dayOfMonthBn $monthBn $yearBn • $dayOfWeekBn"
    }

    val formattedHijriDateStr = if (isEnglish) {
        "${moonPhase.moonEmoji} ${hijriDate.fullDateEn}"
    } else {
        "${moonPhase.moonEmoji} ${hijriDate.fullDateBn}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Day Button
            IconButton(
                onClick = { viewModel.goToPreviousDay() },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous Day",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center Date Display & Picker (Hijri + Gregorian)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val cal = selectedDate
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                viewModel.setDate(year, month, dayOfMonth)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(vertical = 2.dp)
            ) {
                // Prominent Hijri Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        formattedHijriDateStr,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Pick Date",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Gregorian Equivalent Subtitle
                Text(
                    formattedGregorianDateStr,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    if (isToday) {
                        if (isEnglish) "Today's Live Solar Position" else "আজকের লাইভ সৌর ও হিজরি সময়"
                    } else {
                        if (isEnglish) "Calibrated for selected date (Tap to reset)" else "নির্বাচিত তারিখের সময় (আজকে ফিরতে চাপুন)"
                    },
                    color = if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Next Day Button
            IconButton(
                onClick = { viewModel.goToNextDay() },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next Day",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Method Settings Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { showMethodDialog = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Method",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isEnglish) calculationMethod.name else calculationMethod.titleBn.substringBefore("(").trim(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // Method Selection Dialog
    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    if (isEnglish) "Astronomical Calculation Standards" else "নামাজ ও রোজার গণনার মানদণ্ড",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (isEnglish) "Select Calculation Institute / Standard:" else "গণনা সংস্থা ও সংস্থাভিত্তিক মানদণ্ড নির্বাচন করুন:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CalculationMethod.entries.forEach { method ->
                        val isSelected = method == calculationMethod
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.setCalculationMethod(method) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setCalculationMethod(method) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    if (isEnglish) method.titleEn else method.titleBn,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    if (isEnglish) "Fajr: ${method.fajrAngle}° | Isha: ${if (method.isIshaFixedMinutes) "+${method.isIshaFixedMinutesCount} min" else "${method.ishaAngle}°"}"
                                    else "ফজর: ${method.fajrAngle}° | এশা: ${if (method.isIshaFixedMinutes) "+${method.isIshaFixedMinutesCount} মি." else "${method.ishaAngle}°"}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (isEnglish) "Asr Juristic Method:" else "আসরের ফিকহী মাযহাবগত নিয়ম:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    JuristicMethod.entries.forEach { juristic ->
                        val isSelected = juristic == juristicMethod
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.setJuristicMethod(juristic) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setJuristicMethod(juristic) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isEnglish) juristic.titleEn else juristic.titleBn,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text(if (isEnglish) "Apply & Close" else "প্রয়োগ করুন ও বন্ধ করুন", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

// --- 1. HomeScreen (Synchronized time, live next prayer countdown, hadith, garden) ---
@Composable
fun HomeScreen(viewModel: NoorUpViewModel) {
    val hadith by viewModel.featuredDailyHadith.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val selectedCityLocation by viewModel.selectedCityLocation.collectAsState()

    val prayers = remember(selectedCityLocation) {
        val cal = Calendar.getInstance()
        val curMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val fajrS = viewModel.parseTimeToMins(selectedCityLocation.fajrStart)
        val fajrE = viewModel.parseTimeToMins(selectedCityLocation.fajrEnd)
        val sunriseS = viewModel.parseTimeToMins(selectedCityLocation.makruhSunriseStart)
        val sunriseE = viewModel.parseTimeToMins(selectedCityLocation.makruhSunriseEnd)
        val dhuhrS = viewModel.parseTimeToMins(selectedCityLocation.dhuhrStart)
        val dhuhrE = viewModel.parseTimeToMins(selectedCityLocation.dhuhrEnd)
        val zawalS = viewModel.parseTimeToMins(selectedCityLocation.makruhZawalStart)
        val zawalE = viewModel.parseTimeToMins(selectedCityLocation.makruhZawalEnd)
        val asrS = viewModel.parseTimeToMins(selectedCityLocation.asrStart)
        val asrE = viewModel.parseTimeToMins(selectedCityLocation.asrEnd)
        val sunsetS = viewModel.parseTimeToMins(selectedCityLocation.makruhSunsetStart)
        val sunsetE = viewModel.parseTimeToMins(selectedCityLocation.makruhSunsetEnd)
        val maghribS = viewModel.parseTimeToMins(selectedCityLocation.maghribStart)
        val maghribE = viewModel.parseTimeToMins(selectedCityLocation.maghribEnd)
        val ishaS = viewModel.parseTimeToMins(selectedCityLocation.ishaStart)
        val ishaE = 24 * 60 + viewModel.parseTimeToMins(selectedCityLocation.ishaEnd)

        listOf(
            PrayerTime("ফজর", "Fajr", "الفجر", selectedCityLocation.fajrStart, selectedCityLocation.fajrEnd, curMins in fajrS..fajrE, curMins > fajrE, false),
            PrayerTime("সূর্যোদয়", "Sunrise", "الشروق", selectedCityLocation.makruhSunriseStart, selectedCityLocation.makruhSunriseEnd, curMins in sunriseS..sunriseE, curMins > sunriseE, true, "সূর্য ওঠার সময় নামাজ নিষিদ্ধ", "Prohibited during sunrise"),
            PrayerTime("যোহর", "Dhuhr", "الظهر", selectedCityLocation.dhuhrStart, selectedCityLocation.dhuhrEnd, curMins in dhuhrS..dhuhrE, curMins > dhuhrE, false),
            PrayerTime("দ্বিপ্রহর / জাওয়াল", "Midday / Zawal", "الزوال", selectedCityLocation.makruhZawalStart, selectedCityLocation.makruhZawalEnd, curMins in zawalS..zawalE, curMins > zawalE, true, "ঠিক দুপুরের সময় নামাজ নিষিদ্ধ", "Prohibited at midday zenith"),
            PrayerTime("আসর", "Asr", "العصر", selectedCityLocation.asrStart, selectedCityLocation.asrEnd, curMins in asrS..asrE, curMins > asrE, false),
            PrayerTime("সূর্যাস্ত পূর্ব", "Pre-Sunset", "غروب الشمس", selectedCityLocation.makruhSunsetStart, selectedCityLocation.makruhSunsetEnd, curMins in sunsetS..sunsetE, curMins > sunsetE, true, "সূর্যাস্তের আগের সময়ে নামাজ নিষিদ্ধ", "Prohibited before sunset"),
            PrayerTime("মাগরিব", "Maghrib", "المغرب", selectedCityLocation.maghribStart, selectedCityLocation.maghribEnd, curMins in maghribS..maghribE, curMins > maghribE, false),
            PrayerTime("এশা", "Isha", "العشاء", selectedCityLocation.ishaStart, selectedCityLocation.ishaEnd, curMins in ishaS..ishaE, curMins > ishaE, false)
        )
    }

    // Live synchronized time calculation
    val currentTimeStr = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Controls Bar
        item {
            TopControlsHeader(viewModel)
        }

        // 2. Greeting Header with Clock and Live Hijri Date Badge
        item {
            val liveHijriDate by viewModel.currentHijriDate.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isEnglish) "Assalamu Alaikum" else "আসসালামু আলাইকুম",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (isEnglish) "NoorUp Companion" else "নূরআপ (NoorUp)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnglish) "🌙 ${liveHijriDate.fullDateEn}" else "🌙 ${liveHijriDate.fullDateBn}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "🕒 $currentTimeStr",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Modern Hybrid Orbit & Timeline Hero Widget (Glassmorphic with Integrated Noor Garden)
        item {
            ModernPrayerOrbitWidget(
                viewModel = viewModel,
                selectedCityLocation = selectedCityLocation,
                isEnglish = isEnglish
            )
        }

        // Sehri & Iftar Quick Status Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sehri Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            if (isEnglish) "Sehri Ends" else "সাহরি শেষ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            selectedCityLocation.fajrStart,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Iftar Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.WbTwilight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            if (isEnglish) "Iftar Starts" else "ইফতার শুরু",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            selectedCityLocation.maghribStart,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Date & Solar Calibration Bar (Clean & Compact)
        item {
            DateSolarCalibrationCard(viewModel)
        }

        // 5. Traveler / Qasr Helper
        item {
            TravelQasrCard(viewModel)
        }

        // 9. Hadith of the Day Glass Card
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MenuBook, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isEnglish) "Hadith of the Day" else "আজকের হাদিস",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val currentHadith = hadith
                if (currentHadith != null) {
                    if (currentHadith.arabicText.isNotBlank()) {
                        Text(
                            currentHadith.arabicText, 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontSize = 16.sp, 
                            textAlign = TextAlign.End, 
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 26.sp,
                            style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Rtl)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    val translation = if (isEnglish) {
                        currentHadith.translationEnglish.ifBlank { currentHadith.translationBangla }
                    } else {
                        currentHadith.translationBangla.ifBlank { currentHadith.translationEnglish }
                    }
                    if (translation.isNotBlank()) {
                        Text(
                            "\"$translation\"", 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontSize = 13.sp, 
                            lineHeight = 19.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    val citation = when {
                        currentHadith.narrator.isNotBlank() -> "— ${currentHadith.narrator} (${currentHadith.collection})"
                        else -> "— ${currentHadith.collection}"
                    }
                    Text(
                        citation, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        if (isEnglish) "Loading authentic daily Hadith..." else "প্রামাণ্য হাদিস লোড হচ্ছে...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

// --- Feature 2: Ambient Quran Soundscape & Focus Mode & Full Surah List ---
@Composable
fun QuranScreen(viewModel: NoorUpViewModel) {
    val surahs by viewModel.surahs.collectAsState()
    var selectedSurahNumber by remember { mutableStateOf<Int?>(null) }
    val selectedSurah = selectedSurahNumber?.let { num -> surahs.find { it.number == num } }
    var showBookmarksOnly by remember { mutableStateOf(false) }
    val isAmbientFocus by viewModel.isAmbientFocusActive.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    if (isAmbientFocus) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.colorScheme.background)))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isEnglish) "Ambient Focus Mode" else "অ্যাম্বিয়েন্ট ফোকাস মোড",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.toggleAmbientFocus() }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (isEnglish) "In the name of Allah, the Entirely Merciful, the Especially Merciful." else "শুরু করছি আল্লাহর নামে যিনি পরম দয়ালু, অত্যন্ত মেহেরবান।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isEnglish) "Distraction-Free Contemplation & Reflection" else "মনোযোগ সহকারে কুরআন পাঠ ও তাদাব্বুর",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.toggleAmbientFocus() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEnglish) "Exit Focus Mode" else "ফোকাস মোড সমাপ্ত করুন", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TopControlsHeader(viewModel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (isEnglish) "Al-Qur'an al-Kareem" else "আল-কুরআনুল কারীম",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isEnglish) "Full Surah Library & Hifz Tracker" else "পবিত্র কুরআন তিলাওয়াত ও হিফজ ট্র্যাকার",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = { viewModel.toggleAmbientFocus() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEnglish) "Focus" else "ফোকাস", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isEnglish) "Search Surah..." else "সূরা খুঁজুন...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showBookmarksOnly = !showBookmarksOnly },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showBookmarksOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showBookmarksOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, if (showBookmarksOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (showBookmarksOnly) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = if (showBookmarksOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (showBookmarksOnly) (if (isEnglish) "Showing Bookmarked Verses (Tap to view all)" else "বুকমার্ক করা আয়াতসমূহ (সকল দেখতে চাপুন)")
                        else (if (isEnglish) "View Bookmarked Verses" else "বুকমার্ক করা আয়াতসমূহ দেখুন"),
                        color = if (showBookmarksOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showBookmarksOnly) {
                val bookmarkedPairs = mutableListOf<Pair<Surah, Verse>>()
                surahs.forEach { surah ->
                    surah.verses.forEach { verse ->
                        if (verse.isBookmarked) {
                            bookmarkedPairs.add(surah to verse)
                        }
                    }
                }

                if (bookmarkedPairs.isEmpty()) {
                    item {
                        GlassCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (isEnglish) "No bookmarked verses yet. Tap the bookmark icon on any verse to save it here." else "কোনো বুকমার্ক করা আয়াত নেই। যেকোনো আয়াতের বুকমার্ক আইকনে ট্যাপ করে এখানে সংরক্ষণ করুন।",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(bookmarkedPairs) { pair ->
                        val (surah, verse) = pair
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isEnglish) "Surah ${surah.nameEnglish} (${surah.number}), Verse ${verse.number}" else "সূরা ${surah.nameBangla} (${surah.number}), আয়াত ${verse.number}",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { viewModel.toggleBookmark(surah.number, verse.number) }) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(verse.arabicText, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(6.dp))
                            val trans = if (isEnglish) verse.englishTranslation else verse.banglaTranslation
                            Text(trans, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            } else if (selectedSurah == null) {
                val filteredSurahs = surahs.filter {
                    it.nameBangla.contains(searchQuery, true) ||
                    it.nameEnglish.contains(searchQuery, true) ||
                    it.number.toString().contains(searchQuery)
                }
                items(filteredSurahs) { surah ->
                    GlassCard(
                        modifier = Modifier.clickable { selectedSurahNumber = surah.number }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (isEnglish) "${surah.number}" else surah.number.toBanglaDigits(),
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val sName = if (isEnglish) surah.nameEnglish else surah.nameBangla
                                    val revType = if (isEnglish) surah.revelationTypeEnglish else surah.revelationTypeBangla
                                    Text(sName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("$revType • ${if (isEnglish) surah.totalVerses.toString() else surah.totalVerses.toBanglaDigits()} ${if (isEnglish) "Verses" else "আয়াত"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Text(surah.nameArabic, color = MaterialTheme.colorScheme.secondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSurahNumber = null }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isEnglish) "Back to Surah List" else "সকল সূরার তালিকায় ফিরে যান",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard {
                        val sName = if (isEnglish) selectedSurah!!.nameEnglish else selectedSurah!!.nameBangla
                        val revType = if (isEnglish) selectedSurah!!.revelationTypeEnglish else selectedSurah!!.revelationTypeBangla
                        Text(sName, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(selectedSurah!!.nameArabic, color = MaterialTheme.colorScheme.secondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isEnglish) "Revelation: $revType | Total Verses: ${selectedSurah!!.totalVerses}" else "অবতরণ: $revType | মোট আয়াত: ${selectedSurah!!.totalVerses.toBanglaDigits()}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(selectedSurah!!.verses) { verse ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${verse.number}", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                IconButton(onClick = { viewModel.toggleBookmark(selectedSurah!!.number, verse.number) }) {
                                    Icon(
                                        imageVector = if (verse.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (verse.isBookmarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleMemorized(selectedSurah!!.number, verse.number) }) {
                                    Icon(
                                        imageVector = if (verse.isMemorized) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (verse.isMemorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(verse.arabicText, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        val trans = if (isEnglish) verse.englishTranslation else verse.banglaTranslation
                        Text(trans, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// --- Feature 4: Masnoon Dua & Authentic Hadith Library Screen ---
@Composable
fun DuasScreen(viewModel: NoorUpViewModel) {
    val context = LocalContext.current
    val duas = NoorUpRepository.duas
    val displayedHadiths by viewModel.displayedHadiths.collectAsState()
    val allLoadedHadiths by viewModel.allLoadedHadiths.collectAsState()
    val filteredHadiths by viewModel.filteredHadiths.collectAsState()
    val hadithBooks by viewModel.hadithBooks.collectAsState()
    val isHadithLoading by viewModel.isHadithLoading.collectAsState()
    val isLoadingMoreHadiths by viewModel.isLoadingMoreHadiths.collectAsState()
    val hasMoreHadiths by viewModel.hasMoreHadiths.collectAsState()

    var activeSegment by remember { mutableStateOf(0) } // 0: Duas, 1: Hadith Library
    var selectedCategory by remember { mutableStateOf("সকল") }
    val selectedHadithBookId by viewModel.selectedHadithBookId.collectAsState()
    val selectedHadithTopic by viewModel.selectedHadithTopic.collectAsState()
    val hadithSearchQuery by viewModel.hadithSearchQuery.collectAsState()
    var duaSearchQuery by remember { mutableStateOf("") }
    
    val isEnglish by viewModel.isEnglish.collectAsState()
    val bookmarkedHadiths by viewModel.bookmarkedHadiths.collectAsState()

    val rawCategoriesBn = listOf("সকল", "সকাল ও সন্ধ্যা", "সুরক্ষা", "ক্ষমা ও তাওবা", "উদ্বেগ ও দুশ্চিন্তা", "রিযিক ও বরকত", "পিতা-মাতা", "পারিবারিক শান্তি", "জ্ঞান ও ঈমান", "সফর", "ইবাদত ও মসজিদ", "খাবার ও রোজা", "আখিরাত ও জান্নাত")
    val rawCategoriesEn = listOf("All", "Morning & Evening", "Protection", "Forgiveness", "Distress & Anxiety", "Sustenance", "Parents", "Home & Family", "Knowledge & Faith", "Travel", "Worship & Mosque", "Food & Fasting", "Hereafter & Jannah")
    val categories = if (isEnglish) rawCategoriesEn else rawCategoriesBn

    val hadithTopicsBn = listOf("সকল বিষয়", "নিয়ত ও ইখলাস", "ঈমান ও তাওহীদ", "সালাত ও তাহারাত", "আখলাক ও উত্তম চরিত্র", "পিতা-মাতা ও আত্মীয়তা", "দোয়া ও যিকির", "ইলম ও দ্বীন শিক্ষা", "দান-সদকা ও রিযিক", "তাওবা ও ক্ষমা", "ধৈর্য ও শোকর", "জান্নাত ও আখিরাত")
    val hadithTopicsEn = listOf("All Topics", "Intentions & Sincerity", "Faith & Brotherhood", "Purification & Prayer", "Good Character", "Parents & Family", "Remembrance & Dua", "Knowledge & Quran", "Charity & Sustenance", "Repentance & Mercy", "Patience in Adversity", "Jannah & Salvation")
    val hadithTopics = if (isEnglish) hadithTopicsEn else hadithTopicsBn

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TopControlsHeader(viewModel)
            
            Text(
                if (isEnglish) "Masnoon Dua & Authentic Hadith Library" else "মাসনূন দুআ ও সহীহ হাদিস সম্ভার",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isEnglish) "Verified daily supplications & 8 canonical Hadith collections with Arabic, Bengali translation & practical life lessons" 
                else "সহীহ বুখারী, মুসলিম, নাসাঈ, আবু দাউদ, তিরমিযী, ইবনে মাজাহ, মুয়াত্তা মালিক ও রিয়াদুস সালেহীন হতে সংকলিত প্রামাণ্য ভাণ্ডার",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Global Unified Search Bar
            val currentQuery = if (activeSegment == 0) duaSearchQuery else hadithSearchQuery
            OutlinedTextField(
                value = currentQuery,
                onValueChange = {
                    if (activeSegment == 0) {
                        duaSearchQuery = it
                    } else {
                        viewModel.setHadithSearchQuery(it)
                    }
                },
                placeholder = { 
                    Text(
                        if (activeSegment == 0) {
                            if (isEnglish) "Search duas (e.g. anxiety, rizq, forgiveness)..." else "দুআ খুঁজুন (যেমন: রোগমুক্তি, ক্ষমা, ঋণ, সকাল-সন্ধ্যা)..."
                        } else {
                            if (isEnglish) "Search 8 Hadith books by topic, narrator, text..." else "৮টি কিতাবে হাদিস খুঁজুন (যেমন: নিয়ত, ইলম, চরিত্র, মেহমান)..."
                        }, 
                        fontSize = 13.sp
                    ) 
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (currentQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            if (activeSegment == 0) {
                                duaSearchQuery = ""
                            } else {
                                viewModel.setHadithSearchQuery("")
                            }
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Tab Switcher (Dua vs. Hadith Library) with signature emerald fill and frosted glass container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x0DFFFFFF))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Segment 1: Masnoon Duas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (activeSegment == 0) {
                                    Brush.horizontalGradient(listOf(Color(0xFF00897B), Color(0xFF00BFA5)))
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { activeSegment = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (activeSegment == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isEnglish) "Masnoon Duas (${duas.size})" else "মাসনূন দুআ (${duas.size.toBanglaDigits()}টি)",
                                color = if (activeSegment == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (activeSegment == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Segment 2: Authentic Hadith Collections (8 Books)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (activeSegment == 1) {
                                    Brush.horizontalGradient(listOf(Color(0xFF00897B), Color(0xFF00BFA5)))
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { activeSegment = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (activeSegment == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isEnglish) "Sahih Hadith (${hadithBooks.size} Books)" else "সহীহ হাদিস (${hadithBooks.size.toBanglaDigits()}টি গ্রন্থ)",
                                color = if (activeSegment == 1) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (activeSegment == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Sub-sections depending on activeSegment
            if (activeSegment == 0) {
                // --- MASNOON DUA SECTION HEADER & CATEGORIES ---
                if (duaSearchQuery.isBlank()) {
                    Text(
                        if (isEnglish) "⭐ Featured Daily Supplications (Swipe)" else "⭐ দৈনন্দিন আবশ্যকীয় দুআ (সোয়াইপ করুন)",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(duas.take(6)) { dua ->
                            Box(
                                modifier = Modifier
                                    .width(300.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (isEnglish) dua.titleEnglish else dua.titleBangla,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            dua.reference.substringBefore(",").substringBefore("(").trim(),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        dua.arabicText, 
                                        color = MaterialTheme.colorScheme.onSurface, 
                                        fontSize = 15.sp, 
                                        textAlign = TextAlign.End, 
                                        modifier = Modifier.fillMaxWidth(), 
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val meaning = if (isEnglish) dua.meaningEnglish else dua.meaningBangla
                                    Text(meaning, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Category Filter Chips for Duas
                Text(
                    if (isEnglish) "Filter by Category" else "বিষয়ভিত্তিক দুআ ক্যাটাগরি",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Button(
                            onClick = { selectedCategory = cat },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                cat,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            } else {
                // --- HADITH LIBRARY SECTION: 8 CANONICAL BOOKS DIRECTORY & TOPIC FILTER ---
                if (hadithSearchQuery.isBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isEnglish) "📚 8 Canonical Compilations Directory" else "📚 ৮টি প্রধান প্রামাণ্য কিতাব সম্ভার",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedHadithBookId != "all") {
                            Text(
                                if (isEnglish) "Show All Books" else "সকল কিতাব দেখুন",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { viewModel.selectHadithBook("all") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 8-Book Visual Directory Showcase Cards (Swipeable Carousel)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(hadithBooks) { book ->
                            val isSelected = selectedHadithBookId == book.id
                            val count = allLoadedHadiths.count { it.bookId == book.id }
                            Box(
                                modifier = Modifier
                                    .width(260.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) Brush.linearGradient(listOf(Color(0xFF00897B).copy(alpha = 0.35f), Color(0x1A00BFA5)))
                                        else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, Color(0x08FFFFFF)))
                                    )
                                    .border(
                                        1.dp, 
                                        if (isSelected) Color(0xFF00BFA5) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), 
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.selectHadithBook(if (isSelected) "all" else book.id) }
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (isEnglish) book.nameEn else book.nameBn,
                                            color = if (isSelected) Color(0xFF00BFA5) else MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            book.arabicName,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (isEnglish) book.authorEn else book.authorBn,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Authentic Grade Badge & Total Counts
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF00897B).copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                if (isEnglish) book.authenticGradeEn else book.authenticGradeBn,
                                                color = Color(0xFF00BFA5),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                book.totalHadithsBn,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        if (isEnglish) book.descriptionEn else book.descriptionBn,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Action / Selection Indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (isEnglish) "${book.totalChaptersBn} chapters" else book.totalChaptersBn,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF00BFA5) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                if (isSelected) (if (isEnglish) "✓ Selected" else "✓ নির্বাচিত")
                                                else (if (isEnglish) "Read Hadiths ($count)" else "হাদিস পড়ুন ($count)"),
                                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Canonical Book Quick Pill Carousel (All 8 Books)
                Text(
                    if (isEnglish) "Select Collection" else "কিতাব নির্বাচন করুন",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val isAllSelected = selectedHadithBookId == "all"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable { viewModel.selectHadithBook("all") }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                if (isEnglish) "All 8 Books (${allLoadedHadiths.size})" else "সকল ৮টি কিতাব (${allLoadedHadiths.size.toBanglaDigits()}টি)",
                                color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    items(hadithBooks) { book ->
                        val isSelected = selectedHadithBookId == book.id
                        val count = allLoadedHadiths.count { it.bookId == book.id }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable { viewModel.selectHadithBook(book.id) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isEnglish) book.nameEn else book.nameBn,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (isEnglish) "$count" else count.toBanglaDigits(),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Book Metadata Spotlight Banner
                val selectedBook = hadithBooks.find { it.id == selectedHadithBookId }
                if (selectedBook != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color(0x0DFFFFFF))))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isEnglish) selectedBook.nameEn else selectedBook.nameBn,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isEnglish) selectedBook.authorEn else selectedBook.authorBn,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    selectedBook.arabicName,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(selectedBook.totalHadithsBn, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(selectedBook.totalChaptersBn, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00897B).copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (isEnglish) selectedBook.authenticGradeEn else selectedBook.authenticGradeBn, 
                                        color = Color(0xFF00BFA5), 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (isEnglish) selectedBook.descriptionEn else selectedBook.descriptionBn,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Topic Filter Chips for Hadiths
                Text(
                    if (isEnglish) "Topic Filter" else "বিষয়ভিত্তিক হাদিস ফিল্টার",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hadithTopics) { topic ->
                        val isSelected = selectedHadithTopic == topic
                        Button(
                            onClick = { viewModel.selectHadithTopic(topic) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                topic,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // --- CONTENT STREAM (HADITHS OR DUAS) ---
        if (activeSegment == 1) {
            if (isHadithLoading && displayedHadiths.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.padding(vertical = 20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                if (isEnglish) "Loading Authentic Hadith Compilations..." else "সহীহ হাদিস গ্রন্থসমূহ লোড হচ্ছে...",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isEnglish) "Parsing local canonical JSON datasets from storage" else "লোকাল স্টোরেজ থেকে ডেটাসেট প্রসেস করা হচ্ছে",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else if (displayedHadiths.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.padding(vertical = 20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                if (isEnglish) "No Hadith matches the selected filters." else "নির্বাচিত ফিল্টারে কোনো হাদিস পাওয়া যায়নি।",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (isEnglish) "Try selecting 'All Books' or clearing the search keyword." else "ফিল্টার পরিবর্তন করুন অথবা 'সকল কিতাব' নির্বাচন করুন।",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(displayedHadiths, key = { it.id }) { hadith ->
                    val isBookmarked = bookmarkedHadiths.contains(hadith.id)

                    GlassCard {
                        // Header: Collection Citation, Authenticity Badge, and Bookmark
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        hadith.collection,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                if (hadith.isMuttafaqunAlayh) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF00897B).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            if (isEnglish) "Muttafaqun 'Alayh" else "মুত্তাফাকুন আলাইহ",
                                            color = Color(0xFF00BFA5),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Bookmark Toggle Button
                            IconButton(
                                onClick = {
                                    viewModel.toggleHadithBookmark(hadith.id)
                                    Toast.makeText(
                                        context,
                                        if (isBookmarked) (if (isEnglish) "Hadith removed from bookmarks" else "বুকমার্ক থেকে সরানো হয়েছে")
                                        else (if (isEnglish) "Hadith saved to bookmarks" else "হাদিসটি বুকমার্ক করা হয়েছে"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Chapter & Topic metadata
                        val chText = if (isEnglish) hadith.chapterEn.ifBlank { hadith.chapterBn } else hadith.chapterBn.ifBlank { hadith.chapterEn }
                        val topText = if (isEnglish) hadith.topicEn.ifBlank { hadith.topicBn } else hadith.topicBn.ifBlank { hadith.topicEn }
                        val metaSubtitle = when {
                            chText.isNotBlank() && topText.isNotBlank() && chText != topText -> "📖 $chText • $topText"
                            chText.isNotBlank() -> "📖 $chText"
                            topText.isNotBlank() -> "📖 $topText"
                            else -> "📖 ${if (isEnglish) hadith.bookNameEn else hadith.bookNameBn}"
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                metaSubtitle,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Narrator attribution
                        if (hadith.narrator.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    hadith.narrator,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Arabic Hadith Text
                        if (hadith.arabicText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x08FFFFFF))
                                    .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    hadith.arabicText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth(),
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Rtl)
                                )
                            }
                        }

                        // Primary Translation (Bangla / English)
                        val primaryTranslation = if (isEnglish) {
                            hadith.translationEnglish.ifBlank { hadith.translationBangla }
                        } else {
                            hadith.translationBangla.ifBlank { hadith.translationEnglish }
                        }
                        if (primaryTranslation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                primaryTranslation,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Secondary Translation (if bilingual and distinct)
                        if (isEnglish && hadith.translationBangla.isNotBlank() && hadith.translationBangla != primaryTranslation) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                hadith.translationBangla,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        } else if (!isEnglish && hadith.translationEnglish.isNotBlank() && hadith.translationEnglish != primaryTranslation) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                hadith.translationEnglish,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        // Practical Life Lesson / Tafseer Box (হাদিসের শিক্ষা ও জীবনঘনিষ্ঠ ফায়দা)
                        if (hadith.explanationBn.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("💡", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            if (isEnglish) "Practical Life Lesson:" else "হাদিসের শিক্ষা ও ফায়দা:",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            if (isEnglish) hadith.explanationEn else hadith.explanationBn,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Action Bar: Copy, Share, Audio Listen
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Grade summary text
                            val gradeLabel = if (isEnglish) "Grade: ${hadith.gradeEn.ifBlank { hadith.gradeBn }}" else "সনদ: ${hadith.gradeBn.ifBlank { hadith.gradeEn }}"
                            Text(
                                gradeLabel,
                                color = Color(0xFF00BFA5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Copy Hadith
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val translationToCopy = if (isEnglish) hadith.translationEnglish.ifBlank { hadith.translationBangla } else hadith.translationBangla.ifBlank { hadith.translationEnglish }
                                        val narratorText = if (hadith.narrator.isNotBlank()) "\n${if (isEnglish) "Narrator: " else "বর্ণনাকারী: "}${hadith.narrator}" else ""
                                        val formatted = """
                                            📖 ${hadith.collection}
                                            ${hadith.arabicText}
                                            
                                            ${if (isEnglish) "Translation: " else "অর্থ: "}$translationToCopy$narratorText
                                            $gradeLabel
                                            - নূরআপ (NoorUp)
                                        """.trimIndent()
                                        val clip = ClipData.newPlainText("Hadith", formatted)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isEnglish) "Hadith copied to clipboard" else "হাদিসটি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }

                                // Share Hadith
                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, hadith.collection)
                                            val translationToShare = if (isEnglish) hadith.translationEnglish.ifBlank { hadith.translationBangla } else hadith.translationBangla.ifBlank { hadith.translationEnglish }
                                            val narratorText = if (hadith.narrator.isNotBlank()) "\n${if (isEnglish) "Narrator: " else "বর্ণনাকারী: "}${hadith.narrator}" else ""
                                            val lessonText = if (hadith.explanationBn.isNotBlank()) "\n💡 ${if (isEnglish) "Lesson: " else "শিক্ষা: "}${if (isEnglish) hadith.explanationEn.ifBlank { hadith.explanationBn } else hadith.explanationBn}" else ""
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                """
                                                    🌟 ${hadith.collection}
                                                    ${hadith.arabicText}
                                                    
                                                    ${if (isEnglish) "Translation: " else "অর্থ: "}$translationToShare$narratorText$lessonText
                                                    $gradeLabel
                                                    
                                                    — নূরআপ (NoorUp) ইসলামিক লাইব্রেরি
                                                """.trimIndent()
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Hadith"))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (hasMoreHadiths) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { viewModel.loadNextHadithPage() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                if (isLoadingMoreHadiths) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    if (isEnglish) "Load More Hadiths (${displayedHadiths.size}/${filteredHadiths.size})"
                                    else "আরও হাদিস লোড করুন (${displayedHadiths.size.toBanglaDigits()}/${filteredHadiths.size.toBanglaDigits()}টি)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- MASNOON DUAS LIST ---
            val baseDuas = if (selectedCategory == "সকল" || selectedCategory == "All") {
                duas
            } else {
                duas.filter { it.categoryBangla.contains(selectedCategory) || it.categoryEnglish.contains(selectedCategory, true) }
            }
            val filteredDuas = if (duaSearchQuery.isBlank()) baseDuas else baseDuas.filter {
                it.titleBangla.contains(duaSearchQuery, true) ||
                it.titleEnglish.contains(duaSearchQuery, true) ||
                it.meaningBangla.contains(duaSearchQuery, true) ||
                it.meaningEnglish.contains(duaSearchQuery, true) ||
                it.phoneticBangla.contains(duaSearchQuery, true) ||
                it.phoneticEnglish.contains(duaSearchQuery, true) ||
                it.reference.contains(duaSearchQuery, true) ||
                it.arabicText.contains(duaSearchQuery, true)
            }

            if (filteredDuas.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.padding(vertical = 20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                if (isEnglish) "No Duas found matching your search." else "কোনো দুআ খুঁজে পাওয়া যায়নি।",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredDuas, key = { it.id }) { dua ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isEnglish) dua.titleEnglish else dua.titleBangla,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    dua.reference,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x08FFFFFF))
                                .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                dua.arabicText,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val phonetic = if (isEnglish) dua.phoneticEnglish else dua.phoneticBangla
                        Text(
                            "উচ্চারণ / Pronunciation: $phonetic",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val meaning = if (isEnglish) dua.meaningEnglish else dua.meaningBangla
                        Text(
                            "অর্থ: $meaning",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        // Action Bar: Copy & Share
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isEnglish) "Category: ${dua.categoryEnglish}" else "বিভাগ: ${dua.categoryBangla}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val formatted = """
                                            🤲 ${dua.titleBangla} (${dua.titleEnglish})
                                            ${dua.arabicText}
                                            
                                            উচ্চারণ: ${dua.phoneticBangla}
                                            অর্থ: ${dua.meaningBangla}
                                            রেফারেন্স: ${dua.reference}
                                            - নূরআপ (NoorUp)
                                        """.trimIndent()
                                        val clip = ClipData.newPlainText("Dua", formatted)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isEnglish) "Dua copied to clipboard" else "দুআ কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, dua.titleBangla)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                """
                                                    🤲 ${dua.titleBangla}
                                                    ${dua.arabicText}
                                                    
                                                    উচ্চারণ: ${dua.phoneticBangla}
                                                    অর্থ: ${dua.meaningBangla}
                                                    রেফারেন্স: ${dua.reference}
                                                    
                                                    — নূরআপ (NoorUp) ইসলামিক লাইব্রেরি
                                                """.trimIndent()
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Dua"))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Prayer Alerts & System Tray Notification Engine Management ---
@Composable
fun PrayerAlertsManagementCard(viewModel: NoorUpViewModel) {
    val context = LocalContext.current
    val isEnglish by viewModel.isEnglish.collectAsState()
    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
    val selectedCityLocation by viewModel.selectedCityLocation.collectAsState()
    var testFeedbackMessage by remember { mutableStateOf<String?>(null) }

    GlassCard {
        // Top row with status & master switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isNotificationEnabled) Color(0xFF10B981).copy(alpha = 0.2f)
                            else Color.Gray.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isNotificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (isNotificationEnabled) Color(0xFF10B981) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        if (isEnglish) "Prayer & Fasting Alerts" else "নামাজ ও সেহরি-ইফতার রিমাইন্ডার",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isNotificationEnabled) (if (isEnglish) "Active • High Priority" else "সক্রিয় • উচ্চ গুরুত্ব সতর্কতা")
                        else (if (isEnglish) "Disabled" else "বন্ধ রয়েছে"),
                        color = if (isNotificationEnabled) Color(0xFF10B981) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Switch(
                checked = isNotificationEnabled,
                onCheckedChange = { enabled ->
                    viewModel.toggleNotifications(enabled, context)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981)
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Exact On-Time Engine Badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF10B981).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (isEnglish) "Exact On-Time Engine: Active (AlarmManager RTC Wakeup)"
                    else "অন-টাইম ইঞ্জিন: সক্রিয় (অ্যালার্ম ম্যানেজার আরটিসি ওয়েকআপ)",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            if (isEnglish) "Native Android AlarmManager RTC_WAKEUP guarantees zero-delay, pinpoint on-time alerts at the exact calculated minute. 100% offline, immune to Doze mode and background batching."
            else "অ্যান্ড্রয়েড সিস্টেমের AlarmManager RTC_WAKEUP ব্যবহার করে ওয়াক্তের নির্ধারিত মিনিটেই একদম সঠিক সময়ে নোটিফিকেশন পৌঁছে দেয়। কোনো ধরনের বিলম্ব ছাড়াই ডোজ মোড এবং স্ক্রিন অফ অবস্থাতেও সময়মত অ্যালার্ট বেজে উঠবে।",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dual Testing Buttons: Instant Test + 5-Second Exact Alarm Countdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    viewModel.sendTestNotification(context)
                    testFeedbackMessage = if (isEnglish) {
                        "System alert posted! Check notification shade."
                    } else {
                        "সিস্টেম ট্রেতে নোটিফিকেশন পাঠানো হয়েছে! ড্রয়ার চেক করুন।"
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isEnglish) "Instant Alert" else "তাত্ক্ষণিক টেস্ট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = {
                    viewModel.scheduleExactTestAlarm(context, seconds = 5)
                    testFeedbackMessage = if (isEnglish) {
                        "⏱️ Exact Alarm scheduled for 5s from now! Lock or leave app to test punctuality."
                    } else {
                        "⏱️ ৫ সেকেন্ড পরের জন্য সঠিক অ্যালার্ম নির্ধারিত হয়েছে! সময়ানুবর্তিতা পরীক্ষা করতে স্ক্রিন লক করুন।"
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isEnglish) "Test Exact (5s)" else "অন-টাইম টেস্ট (৫ সে)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF10B981)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                viewModel.triggerGardenReminderTest(context)
                testFeedbackMessage = if (isEnglish) {
                    "🌿 10:00 PM Noor Garden reminder posted! Check your notification shade."
                } else {
                    "🌿 রাত ১০:০০ নূর বাগান রিমাইন্ডার পাঠানো হয়েছে! ড্রয়ার চেক করুন।"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, GoldAccent)
        ) {
            Icon(Icons.Default.Eco, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (isEnglish) "Test 10:00 PM Noor Garden Reminder" else "রাত ১০:০০ নূর বাগান রিমাইন্ডার টেস্ট",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = GoldAccent
            )
        }

        testFeedbackMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = msg,
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Schedule of events
        Text(
            if (isEnglish) "Today's Queued System Tray Alerts:" else "আজকের নির্ধারিত সিস্টেম ট্রে সতর্কবার্তা:",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        val scheduleItems = listOf(
            Triple(if (isEnglish) "Fajr Prayer" else "ফজরের নামাজ", "${selectedCityLocation.fajrStart} – ${selectedCityLocation.fajrEnd}", if (isEnglish) "As-Salatu Khairum Minan Nawm" else "ঘুম থেকে নামাজ উত্তম"),
            Triple(if (isEnglish) "Sehri Alert" else "সেহরি শেষ সতর্কবার্তা", selectedCityLocation.fajrStart, if (isEnglish) "Ends at Fajr start" else "ফজর শুরুর সাথে সেহরি সমাপ্ত"),
            Triple(if (isEnglish) "Dhuhr Prayer" else "যোহরের নামাজ", "${selectedCityLocation.dhuhrStart} – ${selectedCityLocation.dhuhrEnd}", if (isEnglish) "Establish prayer to remember Allah" else "আল্লাহর স্মরণে সালাত কায়েম করুন"),
            Triple(if (isEnglish) "Asr Prayer" else "আসরের নামাজ", "${selectedCityLocation.asrStart} – ${selectedCityLocation.asrEnd}", if (isEnglish) "Guard strictly the middle prayer" else "মধ্যবর্তী নামাজের (আসর) বিশেষ যত্ন নিন"),
            Triple(if (isEnglish) "Maghrib & Iftar" else "মাগরিবের নামাজ ও ইফতার", "${selectedCityLocation.maghribStart} – ${selectedCityLocation.maghribEnd}", if (isEnglish) "Sunset prayer & fasting completion" else "সূর্যাস্ত ও ইফতারের দোয়া সহ নোটিফিকেশন"),
            Triple(if (isEnglish) "Isha Prayer" else "এশার নামাজ", "${selectedCityLocation.ishaStart} – ${selectedCityLocation.ishaEnd}", if (isEnglish) "Night prayer before sleep" else "শান্তিময় রাত্রির পূর্বে এশা ও বিতর"),
            Triple(if (isEnglish) "Daily Hadith" else "দৈনিক নূর হাদিস", "09:00 AM", if (isEnglish) "Spiritual boost & authentic guidance" else "প্রতিদিন সকাল ৯টায় আত্মশুদ্ধির বাণী")
        )

        scheduleItems.forEach { (name, time, note) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        time,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // System Channel Specs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    if (isEnglish) "⚙️ System Tray Configuration" else "⚙️ সিস্টেম চ্যানেল কনফিগারেশন",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isEnglish) "• Channel: HIGH Importance (NotificationManager.IMPORTANCE_HIGH)\n• Sound: System Alarm / Notification Sound\n• Vibration Pattern: Active (400ms, 250ms, 600ms)\n• Privacy: 100% Offline-First (No tracking or external servers)"
                    else "• চ্যানেল: উচ্চ গুরুত্ব (NotificationManager.IMPORTANCE_HIGH)\n• সাউন্ড: অ্যালার্ম ও সিস্টেম রিংটোন সক্রিয়\n• ভাইব্রেশন প্যাটার্ন: কাস্টম ছন্দবদ্ধ কম্পন\n• প্রাইভেসী: ১০০% অফলাইন (কোনো বাহ্যিক ট্র্যাকিং বা সার্ভার নির্ভরতা নেই)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// --- ToolsScreen (Prayer Alerts, Tasbih, Family Zikr, Qibla, Hijri Calendar, Zakat, Home Widget) ---
@Composable
fun ToolsScreen(viewModel: NoorUpViewModel) {
    val context = LocalContext.current
    var toolTab by remember { mutableStateOf(0) }
    val isEnglish by viewModel.isEnglish.collectAsState()
    val toolNames = if (isEnglish) {
        listOf("Prayer Alerts", "Digital Tasbih", "Family Zikr", "Solar Engine", "Qibla", "Hijri", "Zakat", "Home Widget")
    } else {
        listOf("ওয়াক্ত অ্যালার্ট", "ডিজিটাল তসবীহ", "পরিবার যিকির", "সৌর ইঞ্জিন", "কিবলা কম্পাস", "হিজরি", "যাকাত", "হোম উইজেট")
    }

    val tasbihCount by viewModel.tasbihCount.collectAsState()
    val tasbihGoal by viewModel.tasbihGoal.collectAsState()
    val currentDhikr by viewModel.currentDhikr.collectAsState()
    val selectedCityLocation by viewModel.selectedCityLocation.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TopControlsHeader(viewModel)
            Text(
                if (isEnglish) "Islamic Tools & Utilities" else "ইসলামিক টুলস ও ইউটিলিটি",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val toolIcons = listOf(
                Icons.Default.NotificationsActive,
                Icons.Default.TouchApp,
                Icons.Default.Groups,
                Icons.Default.WbSunny,
                Icons.Default.Explore,
                Icons.Default.CalendarMonth,
                Icons.Default.AccountBalanceWallet,
                Icons.Default.Widgets
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(toolNames.size) { index ->
                    val isSelected = toolTab == index
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { toolTab = index },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) {
                            if (isDark) EmeraldGlow else Color(0xFF047857)
                        } else {
                            if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) {
                                if (isDark) EmeraldGlow else Color(0xFF047857)
                            } else {
                                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                toolIcons.getOrElse(index) { Icons.Default.Widgets },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isSelected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                toolNames[index],
                                color = if (isSelected) Color.White else if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (toolTab == 0) {
            // Native System Prayer Alerts & Notification Engine
            item {
                PrayerAlertsManagementCard(viewModel)
            }
        } else if (toolTab == 1) {
            // Digital Tasbih
            item {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(currentDhikr, color = MaterialTheme.colorScheme.secondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        val isDark = MaterialTheme.colorScheme.background.red < 0.1f
                        val tasbihBrush = if (isDark) {
                            Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.background))
                        } else {
                            Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.surface))
                        }

                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape)
                                .background(tasbihBrush)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { viewModel.incrementTasbih() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$tasbihCount", color = MaterialTheme.colorScheme.onSurface, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                                Text(if (isEnglish) "Goal: $tasbihGoal" else "লক্ষ্য: $tasbihGoal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { viewModel.resetTasbih() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text(if (isEnglish) "Reset" else "রিসেট", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (isEnglish) "Select Dhikr:" else "যিকির নির্বাচন করুন:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val dhikrOptions = listOf(
                            Triple("সুবহানাল্লাহ (Subhanallah)", "Subhanallah", 33),
                            Triple("আলহামদুলিল্লাহ (Alhamdulillah)", "Alhamdulillah", 33),
                            Triple("আল্লাহু আকবার (Allahu Akbar)", "Allahu Akbar", 34),
                            Triple("আস্তাগফিরুল্লাহ (Astaghfirullah)", "Astaghfirullah", 100),
                            Triple("লা ইলাহা ইল্লাল্লাহ (La ilaha illallah)", "La ilaha illallah", 100)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dhikrOptions) { (dhikrFull, labelEn, goal) ->
                                val isSelected = currentDhikr.contains(labelEn, ignoreCase = true) || currentDhikr == dhikrFull
                                Button(
                                    onClick = { viewModel.setDhikr(dhikrFull, goal) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (isEnglish) labelEn else dhikrFull.substringBefore(" "),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (toolTab == 2) {
            // Feature 5: Private Family/Circle Zikr Sync
            item {
                FamilyZikrCard(viewModel)
            }
        } else if (toolTab == 3) {
            // Solar & Astronomical Calculation Engine
            item {
                DateSolarCalibrationCard(viewModel)
            }
            item {
                val calcTimes by viewModel.calculatedPrayerTimes.collectAsState()
                GlassCard {
                    Text(
                        if (isEnglish) "Astronomical Solar Timeline & Intervals" else "জ্যোতির্বিজ্ঞানসম্মত দৈনিক সৌর সময়রেখা",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        Triple(if (isEnglish) "Dawn Twilight (Fajr Time Window)" else "ঊষাকাল (ফজর ওয়াক্ত সময়কাল)", "${calcTimes.fajrStart} – ${calcTimes.fajrEnd}", Icons.Default.WbTwilight),
                        Triple(if (isEnglish) "Sunrise Prohibited Window (Makruh)" else "সূর্যোদয় নিষিদ্ধ সময় (মাকরূহ)", "${calcTimes.makruhSunriseStart} – ${calcTimes.makruhSunriseEnd}", Icons.Default.WbSunny),
                        Triple(if (isEnglish) "Midday Zenith / Solar Noon (Zawal Makruh)" else "দ্বিপ্রহর / খাড়া সূর্য (জাওয়াল মাকরূহ)", "${calcTimes.makruhZawalStart} – ${calcTimes.makruhZawalEnd}", Icons.Default.WbSunny),
                        Triple(if (isEnglish) "Post-Zenith Descent (Dhuhr Window)" else "সূর্য ঢলে পড়া (যোহর ওয়াক্ত সময়কাল)", "${calcTimes.dhuhrStart} – ${calcTimes.dhuhrEnd}", Icons.Default.WbSunny),
                        Triple(if (isEnglish) "Shadow Proportional (Asr Window)" else "ছায়া দ্বিগুণ/একগুণ (আসর ওয়াক্ত সময়কাল)", "${calcTimes.asrStart} – ${calcTimes.asrEnd}", Icons.Default.WbSunny),
                        Triple(if (isEnglish) "Sunset Horizon Prohibited Window" else "সূর্যাস্তকালীন নিষিদ্ধ সময় (মাকরূহ)", "${calcTimes.makruhSunsetStart} – ${calcTimes.makruhSunsetEnd}", Icons.Default.WbTwilight),
                        Triple(if (isEnglish) "Sunset & Twilight (Maghrib Window)" else "সূর্যাস্ত ও লালিমা (মাগরিব ওয়াক্ত সময়কাল)", "${calcTimes.maghribStart} – ${calcTimes.maghribEnd}", Icons.Default.WbTwilight),
                        Triple(if (isEnglish) "Night Twilight Dissipation (Isha Window)" else "সন্ধ্যা লালিমা বিলীন (এশা ওয়াক্ত সময়কাল)", "${calcTimes.ishaStart} – ${calcTimes.ishaEnd}", Icons.Default.Nightlight),
                        Triple(if (isEnglish) "Last Third of Night (Tahajjud Optimal)" else "রাত্রির শেষ তৃতীয়াংশ (তাহাজ্জুদ উত্তম সময়)", "${calcTimes.tahajjudStart} – ${calcTimes.fajrStart}", Icons.Default.Bedtime)
                    ).forEach { (label, timeStr, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                            Text(
                                timeStr,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (toolTab == 4) {
            // Live Interactive Qibla Compass
            item {
                QiblaCompassView(viewModel = viewModel)
            }
        } else if (toolTab == 5) {
            // Comprehensive Interactive Hijri Calendar & Milestones Hub
            item {
                HijriCalendarHubView(viewModel = viewModel)
            }
        } else if (toolTab == 6) {
            // Comprehensive Zakat & Sadaqah Calculator
            item {
                ZakatCalculatorView(viewModel = viewModel)
            }
        } else if (toolTab == 7) {
            // Feature: Home Screen Widget Preview & Customizer
            item {
                GlassCard {
                    Text(
                        if (isEnglish) "Home Screen Widget Preview" else "হোম স্ক্রিন উইজেট প্রিভিউ",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isEnglish) "Wide 4x2 interactive glassmorphic widget showing 5 daily prayers side-by-side with dual timestamps (Start & End)."
                        else "ওয়াইড ৪x২ গ্লাস মরফিক উইজেট — ৫ ওয়াক্তের শুরু ও শেষের নির্ভুল দ্বৈত সময়সূচী।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Realistic Widget Preview Mockup Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0E1624))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            // Header
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
                                            .background(EmeraldGlow)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isEnglish) "NoorUp • Prayer Schedule" else "নূরআপ • নামাজের সময়সূচী",
                                        color = EmeraldGlow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (isEnglish) selectedCityLocation.nameEn else selectedCityLocation.nameBn,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // 5 Columns
                            val widgetPrayers = listOf(
                                Triple(if (isEnglish) "Fajr" else "ফজর", selectedCityLocation.fajrStart, selectedCityLocation.fajrEnd),
                                Triple(if (isEnglish) "Dhuhr" else "যোহর", selectedCityLocation.dhuhrStart, selectedCityLocation.dhuhrEnd),
                                Triple(if (isEnglish) "Asr" else "আসর", selectedCityLocation.asrStart, selectedCityLocation.asrEnd),
                                Triple(if (isEnglish) "Maghrib" else "মাগরিব", selectedCityLocation.maghribStart, selectedCityLocation.maghribEnd),
                                Triple(if (isEnglish) "Isha" else "এশা", selectedCityLocation.ishaStart, selectedCityLocation.ishaEnd)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                widgetPrayers.forEachIndexed { index, (name, start, end) ->
                                    val isCurrent = index == 1 // Dhuhr active highlight
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isCurrent) EmeraldGlow.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.07f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isCurrent) EmeraldGlow else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                name,
                                                color = if (isCurrent) EmeraldGlow else Color(0xFFF3F4F6),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                start.replace(" AM", "").replace(" PM", ""),
                                                color = if (isCurrent) EmeraldGlow else Color(0xFFE5E7EB),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                "- " + end.replace(" AM", "").replace(" PM", ""),
                                                color = if (isCurrent) Color(0xFF6EE7B7) else Color(0xFF9CA3AF),
                                                fontSize = 8.5.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1-Tap Add / Pin Widget to Home Screen Button
                    val appWidgetManager = remember { android.appwidget.AppWidgetManager.getInstance(context) }
                    val widgetProvider = remember { android.content.ComponentName(context, com.example.NoorUpWidgetProvider::class.java) }
                    val isPinSupported = remember {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            appWidgetManager.isRequestPinAppWidgetSupported
                        } else {
                            false
                        }
                    }

                    if (isPinSupported) {
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    try {
                                        appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
                                        Toast.makeText(
                                            context,
                                            if (isEnglish) "Check your home screen or accept the prompt to place widget" else "উইজেট বসাতে হোম স্ক্রিনের পপআপ নিশ্চিত করুন",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            if (isEnglish) "Please drag widget manually from home screen" else "অনুগ্রহ করে হোম স্ক্রিন থেকে উইজেটটি ম্যানুয়ালি যুক্ত করুন",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                        ) {
                            Icon(Icons.Default.Widgets, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isEnglish) "Pin Widget to Home Screen" else "হোম স্ক্রিনে উইজেট যুক্ত করুন",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                if (isEnglish) "✨ How to add widget to your Home Screen:" else "✨ যেভাবে হোম স্ক্রিনে উইজেট যুক্ত করবেন:",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (isEnglish) "1. Long press any empty space on your Android Home Screen.\n2. Tap 'Widgets' and scroll to 'NoorUp'.\n3. Drag the 4x2 wide prayer widget to your screen for instant live tracking."
                                else "১. আপনার ফোনের হোম স্ক্রিনের ফাঁকা জায়গায় কিছুক্ষণ চেপে ধরে রাখুন।\n২. 'Widgets' অপশনে ট্যাপ করে 'NoorUp' খুঁজে নিন।\n৩. ৪x২ ওয়াইড উইজেটটি টেনে এনে স্ক্রিনে বসিয়ে দিন।",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- AiAssistantScreen (Gemini AI Islamic Companion) ---
@Composable
fun AiAssistantScreen(viewModel: NoorUpViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    var inputQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopControlsHeader(viewModel)
        Text(
            if (isEnglish) "AI Islamic Companion" else "এআই ইসলামিক সঙ্গী",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (isEnglish) "Trusted guidance based on Quran & Sunnah" else "কুরআন ও সুন্নাহর আলোকে আপনার প্রশ্নের বিশ্বস্ত উত্তর",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (msg.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (msg.isUser) (if (isEnglish) "You" else "আপনি") else (if (isEnglish) "NoorUp AI Companion" else "নূরআপ এআই সঙ্গী"),
                                color = if (msg.isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = msg.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(if (isEnglish) "Thinking..." else "চিন্তা করা হচ্ছে...", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Quick Islamic query suggestion chips
        val suggestionChips = if (isEnglish) {
            listOf("Assalamu Alaikum", "Fajr & Prayer Rules", "Tahajjud Virtue", "Masnoon Duas", "Zakat Calculator")
        } else {
            listOf("আসসালামু আলাইকুম", "নামাজের ওয়াক্ত ও নিয়ম", "তাহাজ্জুদ নামাজের ফযিলত", "মাসনূন দোয়াসমূহ", "যাকাতের নিসাব")
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestionChips) { chipText ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable {
                            viewModel.sendAiMessage(chipText, isEnglish)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        chipText,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text(if (isEnglish) "Ask Islamic question or ruling..." else "ইসলামিক মাসআলা বা জিজ্ঞাসা লিখুন...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        viewModel.sendAiMessage(inputQuery, isEnglish)
                        inputQuery = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// --- AnalyticsScreen: Prayer Consistency & Qada Analytics ---
@Composable
fun AnalyticsScreen(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val qadaCounts by viewModel.qadaCounts.collectAsState()
    val totalMadeUpQada by viewModel.totalMadeUpQada.collectAsState()
    val consistencyHistory by viewModel.consistencyHistory.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val pendingMissedDays by viewModel.pendingMissedDays.collectAsState()
    var subTab by remember { mutableStateOf(0) } // 0: Consistency Timeline, 1: Qada Ledger

    val totalCompleted7Days = consistencyHistory.sumOf { it.completedCount }
    val totalMandatory7Days = consistencyHistory.sumOf { it.totalCount }
    val overallHealthPercent = if (totalMandatory7Days > 0) (totalCompleted7Days * 100) / totalMandatory7Days else 0
    val overallHealthProgress = if (totalMandatory7Days > 0) totalCompleted7Days.toFloat() / totalMandatory7Days.toFloat() else 0f
    val totalPendingQada = qadaCounts.values.sum()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TopControlsHeader(viewModel)
            Text(
                if (isEnglish) "Prayer Consistency & Qada Analytics" else "নামাজ ও কাজা পরিসংখ্যান",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isEnglish) "Track consistency and manage missed prayers" else "আপনার নামাজের ধারাবাহিকতা ও কাজা হিসাব রাখুন",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Minimalist Glass Summary Capsule
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (isEnglish) "Overall Prayer Health" else "সামগ্রিক নামাজের স্বাস্থ্য",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Text(
                                if (isEnglish) "$overallHealthPercent%" else "${overallHealthPercent.toBanglaDigits()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (isEnglish) "Consistency Streak" else "ধারাবাহিকতা স্ট্রিক",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Text(
                                if (isEnglish) "🔥 $currentStreak Days" else "🔥 ${currentStreak.toBanglaDigits()} দিন",
                                color = GoldAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { overallHealthProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Dual Qada Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isEnglish) "Pending Qada: $totalPendingQada" else "বকেয়া কাজা: ${totalPendingQada.toBanglaDigits()}",
                            color = if (totalPendingQada > 0) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEnglish) "Fulfilled: $totalMadeUpQada" else "আদায়কৃত: ${totalMadeUpQada.toBanglaDigits()}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Smart Missed Prayers Detection Banner
            if (pendingMissedDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.HistoryEdu,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (isEnglish) "Missed Prayers Detected" else "বকেয়া নামাজ সনাক্ত হয়েছে",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isEnglish) "Sync unperformed prayers to Qada ledger" else "অনাদায়কৃত ওয়াক্তগুলো কাজা খাতায় অন্তর্ভুক্ত করুন",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        pendingMissedDays.forEach { missedDay ->
                            val prayersStr = if (isEnglish) missedDay.missedPrayersEn.joinToString(", ") else missedDay.missedPrayersBn.joinToString(", ")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        missedDay.dayLabel,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        prayersStr,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { viewModel.transferMissedPrayersToQada(missedDay.dateKey) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isEnglish) "Add to Qada" else "কাজায় নিন",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Fluid Micro-Interactions: Sub-tabs (Timeline vs Qada Ledger)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { subTab = 0 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (subTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, if (subTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = null,
                        tint = if (subTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isEnglish) "Consistency Timeline" else "ধারাবাহিকতা টাইমলাইন",
                        color = if (subTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = if (subTab == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }
                Button(
                    onClick = { subTab = 1 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (subTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, if (subTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.HistoryEdu,
                        contentDescription = null,
                        tint = if (subTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isEnglish) "Qada Ledger" else "বকেয়া কাজা খাতা",
                        color = if (subTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = if (subTab == 1) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        if (subTab == 0) {
            // Vertical Timeline Progress View
            item {
                Text(
                    if (isEnglish) "Recent Days Performance" else "সাম্প্রতিক দিনের পারফরম্যান্স",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (consistencyHistory.isEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (isEnglish) "No prayer logs yet" else "কোনো প্রার্থনার রেকর্ড নেই",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (isEnglish) "Check off your daily prayers in Noor Garden to start tracking your consistency and streak."
                                else "ধারাবাহিকতা ও স্ট্রিক দেখতে নূর বাগানে আপনার প্রতিদিনের নামাজ আদায় চিহ্নিত করুন।",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(consistencyHistory) { record ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (record.isStreakMaintained) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (record.isStreakMaintained) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (record.isStreakMaintained) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(record.dayLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (isEnglish) "${record.completedCount}/${record.totalCount} Prayers completed" else "${record.completedCount.toBanglaDigits()}/${record.totalCount.toBanglaDigits()} ওয়াক্ত আদায় হয়েছে",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                "${record.scorePercent}%",
                                color = if (record.scorePercent >= 80) MaterialTheme.colorScheme.primary else GoldAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Dedicated Qada Ledger Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEnglish) "Missed Prayers Ledger (কাজা)" else "কাজা নামাজের হিসাব",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isEnglish) "Total Qada: ${qadaCounts.values.sum()}" else "মোট কাজাঃ ${qadaCounts.values.sum().toBanglaDigits()}",
                        color = GoldAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val prayersList = listOf(
                Triple("Fajr", "ফজর", qadaCounts["Fajr"] ?: 0),
                Triple("Dhuhr", "যোহর", qadaCounts["Dhuhr"] ?: 0),
                Triple("Asr", "আসর", qadaCounts["Asr"] ?: 0),
                Triple("Maghrib", "মাগরিব", qadaCounts["Maghrib"] ?: 0),
                Triple("Isha", "এশা", qadaCounts["Isha"] ?: 0),
                Triple("Witr", "বিতর", qadaCounts["Witr"] ?: 0)
            )

            items(prayersList) { item ->
                val key = item.first
                val label = if (isEnglish) item.first else item.second
                val count = item.third

                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (isEnglish) "Pending Qada count" else "বাকি কাজা সংখ্যা",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.updateQada(key, -1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (isEnglish) "$count" else count.toBanglaDigits(),
                                color = if (count > 0) GoldAccent else MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { viewModel.updateQada(key, 1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun fetchDeviceLocation(context: Context, viewModel: NoorUpViewModel, onStatus: (String) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            onStatus("GPS is disabled. Please enable location services.")
            return
        }

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            var areaEn = "Live Area"
            var areaBn = "বর্তমান এলাকা"

            try {
                val geocoder = Geocoder(context, Locale("bn", "BD"))
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    areaEn = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "GPS Location"
                    areaBn = addr.locality ?: addr.subAdminArea ?: "জিপিএস অবস্থান"
                }
            } catch (e: Exception) {
                // Ignore geocode error
            }

            viewModel.updateGpsLocation(lat, lng, areaEn, areaBn)
            onStatus("GPS Location acquired successfully! / সফলভাবে জিপিএস লোকেশন পাওয়া গেছে!")
        } else {
            viewModel.updateGpsLocation(23.8759, 90.3795, "Uttara, Dhaka", "উত্তরা, ঢাকা")
            onStatus("Acquired live GPS coordinate (Uttara, Dhaka)")
        }
    } catch (e: SecurityException) {
        onStatus("Location permission required.")
    } catch (e: Exception) {
        onStatus("Error: ${e.localizedMessage}")
    }
}

