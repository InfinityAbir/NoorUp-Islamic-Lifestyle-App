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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

/** NOORUP_ALARM = Color(0xFF10B981): the app's emerald accent, reused for the alarm surfaces. */
private val PrayerAlarmAccent = Color(0xFF10B981)

/**
 * Compact Islamic-themed onboarding banner for first-time users.
 *
 * Stacked layout (icon + copy, then a full-width action) so it never clips on
 * narrow screens, unlike a fixed horizontal row.
 */
@Composable
fun PrayerAlarmOnboardingBanner(
    isEnglish: Boolean,
    onEnableClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, PrayerAlarmAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(PrayerAlarmAccent, Color(0xFF047857))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mosque,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEnglish) "Enable Prayer Alarms" else "নামাজের অ্যালার্ম চালু করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (isEnglish) "Get an alarm and notification when it is time for prayer."
                        else "নামাজের সময় হলে অ্যালার্ম ও নোটিফিকেশন পান।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onEnableClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrayerAlarmAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (isEnglish) "Enable Alarm" else "অ্যালার্ম চালু করুন",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onDismissClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isEnglish) "Later" else "পরে",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Compact status row used once the user dismissed the banner or a permission was
 * revoked. Always offers a retry so a denial never becomes a dead end.
 */
@Composable
fun PrayerAlarmStatusCard(
    isEnglish: Boolean,
    status: PrayerAlarmSetupStatus,
    onActionClick: () -> Unit
) {
    val (title, description, actionLabel) = when (status) {
        PrayerAlarmSetupStatus.NOTIFICATIONS_DENIED -> Triple(
            if (isEnglish) "Alarm permission needed" else "অ্যালার্মের অনুমতি প্রয়োজন",
            if (isEnglish) "Notifications are off. Allow them to receive prayer alarms."
            else "নোটিফিকেশন বন্ধ আছে। চালু করলে নামাজের সময় অ্যালার্ম পাবেন।",
            if (isEnglish) "Allow" else "অনুমতি দিন"
        )
        PrayerAlarmSetupStatus.EXACT_ALARM_BLOCKED -> Triple(
            if (isEnglish) "Exact alarms are blocked" else "সঠিক সময়ের অ্যালার্ম বন্ধ",
            if (isEnglish) "Allow alarms & reminders so prayer alerts fire on time."
            else "নামাজের সময়ে ঠিক সময়ে অ্যালার্ম পেতে 'অ্যালার্ম ও রিমাইন্ডার' অনুমতি দিন।",
            if (isEnglish) "Settings" else "সেটিংস"
        )
        PrayerAlarmSetupStatus.PROMO_DISMISSED -> Triple(
            if (isEnglish) "Prayer alarms are off" else "নামাজের অ্যালার্ম বন্ধ আছে",
            if (isEnglish) "Turn on prayer reminders whenever you are ready."
            else "প্রস্তুত হলে নামাজের অ্যালার্ম ও নোটিফিকেশন চালু করুন।",
            if (isEnglish) "Turn on" else "চালু করুন"
        )
        else -> Triple("", "", "")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PrayerAlarmAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (status == PrayerAlarmSetupStatus.EXACT_ALARM_BLOCKED) Icons.Default.Alarm
                else Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = PrayerAlarmAccent,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrayerAlarmAccent,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Home-screen prayer-alarm onboarding: rationale -> notification permission ->
 * exact-alarm special access -> verified scheduling through the existing service.
 *
 * One section owns the permission snapshot so the banner and the retry row can
 * never disagree, and so a denied result routes to Settings instead of looping
 * the system dialog.
 */
@Composable
fun PrayerAlarmOnboardingSection(viewModel: NoorUpViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    val isEnglish by viewModel.isEnglish.collectAsState()
    val onboardingComplete by viewModel.isPrayerAlarmOnboardingComplete.collectAsState()
    val promoDismissed by viewModel.isPrayerAlarmPromoDismissed.collectAsState()
    val notificationDenied by viewModel.isPrayerAlarmNotificationDenied.collectAsState()

    var notificationsAllowed by remember {
        mutableStateOf(PrayerAlarmOnboarding.notificationsGranted(context))
    }
    var exactAlarmAllowed by remember {
        mutableStateOf(PrayerAlarmOnboarding.exactAlarmAccessGranted(context))
    }
    var showRationale by remember { mutableStateOf(false) }
    var showExactAlarmHelp by remember { mutableStateOf(false) }
    var awaitingSettingsReturn by rememberSaveable { mutableStateOf(false) }
    var activating by remember { mutableStateOf(false) }

    fun activate() {
        if (activating) return
        activating = true
        viewModel.enablePrayerAlarms(context) { verified ->
            activating = false
            if (!verified) {
                Toast.makeText(
                    context,
                    if (isEnglish) "Prayer alarms could not be scheduled. Check notifications and exact alarms."
                    else "নামাজের অ্যালার্ম নির্ধারণ করা যায়নি। নোটিফিকেশন ও সঠিক অ্যালার্মের অনুমতি দিন।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun continueToExactAlarmCheck() {
        exactAlarmAllowed = PrayerAlarmOnboarding.exactAlarmAccessGranted(context)
        if (!exactAlarmAllowed) showExactAlarmHelp = true else activate()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = granted
        if (granted) {
            viewModel.clearPrayerAlarmNotificationDenied()
            continueToExactAlarmCheck()
        } else {
            viewModel.notePrayerAlarmNotificationDenied()
        }
    }

    fun requestNotificationAccess() {
        notificationsAllowed = PrayerAlarmOnboarding.notificationsGranted(context)
        when {
            notificationsAllowed -> continueToExactAlarmCheck()
            // Denied before: Android will not show the dialog again, so route to Settings.
            notificationDenied -> PrayerAlarmOnboarding.openNotificationSettings(context)
            else -> notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Permissions can change while the app is backgrounded (Settings / system dialog).
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = PrayerAlarmOnboarding.notificationsGranted(context)
                exactAlarmAllowed = PrayerAlarmOnboarding.exactAlarmAccessGranted(context)
                if (awaitingSettingsReturn && notificationsAllowed && exactAlarmAllowed) {
                    awaitingSettingsReturn = false
                    activate()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val status = PrayerAlarmOnboarding.resolveStatus(
        onboardingComplete = onboardingComplete,
        promoDismissed = promoDismissed,
        notificationsGranted = notificationsAllowed,
        exactAlarmAccess = exactAlarmAllowed,
        notificationDenied = notificationDenied
    )

    when (status) {
        PrayerAlarmSetupStatus.ACTIVE -> Unit
        PrayerAlarmSetupStatus.NOT_STARTED -> PrayerAlarmOnboardingBanner(
            isEnglish = isEnglish,
            onEnableClick = {
                // Only explain before an actual system prompt: once notifications
                // are granted, jump straight to the exact-alarm check.
                if (notificationsAllowed) continueToExactAlarmCheck() else showRationale = true
            },
            onDismissClick = { viewModel.dismissPrayerAlarmPromo() }
        )
        else -> PrayerAlarmStatusCard(
            isEnglish = isEnglish,
            status = status,
            onActionClick = {
                when {
                    !notificationsAllowed -> showRationale = true
                    !exactAlarmAllowed -> showExactAlarmHelp = true
                    else -> activate()
                }
            }
        )
    }

    if (showRationale) {
        val mustUseSettings = !notificationsAllowed && notificationDenied
        AlertDialog(
            onDismissRequest = { showRationale = false },
            icon = {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = PrayerAlarmAccent
                )
            },
            title = {
                Text(
                    if (isEnglish) "Never miss a prayer" else "নামাজের ওয়াক্ত আর মিস হবে না",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    when {
                        mustUseSettings && isEnglish ->
                            "Notifications are turned off for NoorUp. Enable them in Settings so Fajr, Dhuhr, Asr, Maghrib and Isha alerts can reach you."
                        mustUseSettings ->
                            "নূরআপের নোটিফিকেশন বন্ধ আছে। সেটিংস থেকে চালু করলে ফজর, যোহর, আসর, মাগরিব ও এশার সময় অ্যালার্ম পাবেন।"
                        isEnglish ->
                            "NoorUp can alert you exactly when each prayer begins. Only notification permission is needed — nothing leaves your device."
                        else ->
                            "প্রতিটি নামাজের ওয়াক্ত শুরু হলে নূরআপ অ্যালার্ম ও নোটিফিকেশন দিয়ে মনে করিয়ে দেবে। শুধু নোটিফিকেশনের অনুমতি প্রয়োজন — আপনার তথ্য ফোনের বাইরে যায় না।"
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    if (mustUseSettings) {
                        awaitingSettingsReturn = true
                        PrayerAlarmOnboarding.openNotificationSettings(context)
                    } else {
                        requestNotificationAccess()
                    }
                }) {
                    Text(
                        when {
                            mustUseSettings && isEnglish -> "Open settings"
                            mustUseSettings -> "সেটিংস খুলুন"
                            isEnglish -> "Continue"
                            else -> "চালিয়ে যান"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(if (isEnglish) "Not now" else "পরে")
                }
            }
        )
    }

    if (showExactAlarmHelp) {
        AlertDialog(
            onDismissRequest = { showExactAlarmHelp = false },
            icon = {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = PrayerAlarmAccent)
            },
            title = {
                Text(
                    if (isEnglish) "Exact prayer alarms" else "সঠিক সময়ের নামাজের অ্যালার্ম",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isEnglish)
                        "Android 12 and newer needs the \"Alarms & reminders\" permission to fire an alarm at the exact prayer minute. Without it alerts may arrive late. Open the next screen and allow it for NoorUp."
                    else
                        "অ্যান্ড্রয়েড ১২ ও তার পরে নামাজের সময়ে ঠিক সেই মিনিটে অ্যালার্ম বাজাতে 'অ্যালার্ম ও রিমাইন্ডার' অনুমতি প্রয়োজন। এটি না দিলে অ্যালার্ম দেরিতে আসতে পারে। পরের স্ক্রিনে নূরআপের জন্য অনুমতিটি চালু করুন।",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmHelp = false
                    awaitingSettingsReturn = true
                    PrayerAlarmOnboarding.openExactAlarmSettings(context)
                }) {
                    Text(
                        if (isEnglish) "Open settings" else "সেটিংস খুলুন",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmHelp = false }) {
                    Text(if (isEnglish) "Not now" else "পরে")
                }
            }
        )
    }
}

/** Resolves the hosting Activity's LifecycleOwner through the Compose context chain. */
private fun Context.findLifecycleOwner(): androidx.lifecycle.LifecycleOwner? {
    var current: Context? = this
    while (current != null) {
        if (current is androidx.lifecycle.LifecycleOwner) return current
        current = (current as? android.content.ContextWrapper)?.baseContext
    }
    return null
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
        // Responsive: flexes to remaining width; long GPS names ellipsize instead of
        // pushing the language/theme controls off-screen on narrow devices.
        Box(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(19.dp))
                    .clickable { showLocationDialog = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
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
                                .verticalScroll(rememberScrollState())
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
        // Responsive: intrinsic size only — never shrinks, never pushes or gets pushed.
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language Toggle Pill
            Row(
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(19.dp))
                    .clickable { viewModel.toggleLanguage() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
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
        // Scrollable: 5 chips overflow 360dp screens under large font scaling.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(NoorUpViewModel.MANDATORY_PRAYERS.size) { index ->
                val prayer = NoorUpViewModel.MANDATORY_PRAYERS[index]
                val isChecked = habitPrayers[prayer] == true
                FilterChip(
                    selected = isChecked,
                    onClick = { viewModel.toggleHabitPrayer(prayer) },
                    label = { Text(prayer, fontSize = 11.sp, maxLines = 1, softWrap = false) },
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
    val dayOfMonthBn = selectedDate.get(Calendar.DAY_OF_MONTH).toString().toBanglaDigits()
    val yearBn = selectedDate.get(Calendar.YEAR).toString().toBanglaDigits()

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
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Responsive: stacked layout. The date navigator stays on its own row and the
        // method pill wraps to a centered second row, so the center date column is
        // never squeezed into vertical character-by-character wrapping on narrow
        // viewports (e.g. 320dp phones = ~264dp available after padding).
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Day Button
                IconButton(
                    onClick = { viewModel.goToPreviousDay() },
                    modifier = Modifier.size(40.dp)
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
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    // Prominent Hijri Date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            formattedHijriDateStr,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false)
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
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        if (isToday) {
                            if (isEnglish) "Today's Live Solar Position" else "আজকের লাইভ সৌর ও হিজরি সময়"
                        } else {
                            if (isEnglish) "Calibrated for selected date (Tap to reset)" else "নির্বাচিত তারিখের সময় (আজকে ফিরতে চাপুন)"
                        },
                        color = if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Next Day Button
                IconButton(
                    onClick = { viewModel.goToNextDay() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Day",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Method Settings Pill — own centered row so it never squeezes the date.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { showMethodDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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

    // Live clock: ticks every 30s (was remember{} frozen at first composition).
    var currentTimeStr by remember { mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            try {
                currentTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            } catch (_: Exception) { }
        }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEnglish) "Assalamu Alaikum" else "আসসালামু আলাইকুম",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isEnglish) "NoorUp Companion" else "নূরআপ (NoorUp)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isEnglish) "🌙 ${liveHijriDate.fullDateEn}" else "🌙 ${liveHijriDate.fullDateBn}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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

        /// 2b. Prayer Alarm Onboarding (banner for new users, compact retry row afterwards)
        item {
            PrayerAlarmOnboardingSection(viewModel)
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

        // 6. Hadith of the Day Glass Card
        item {
            GlassCard(
                modifier = Modifier.clickable {
                    viewModel.selectTab(2)
                    viewModel.setDuaSegment(1)
                }
            ) {
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

// --- Shared Quran navigation dialogs -------------------------------------------------

/** Localized message for a rejected "Go to Ayat" input. */
private fun ayatErrorText(error: NoorUpViewModel.AyatInputError, isEnglish: Boolean, total: Int): String =
    when (error) {
        NoorUpViewModel.AyatInputError.EMPTY ->
            if (isEnglish) "Enter an Ayat number." else "একটি আয়াত নম্বর লিখুন।"
        NoorUpViewModel.AyatInputError.NOT_A_WHOLE_NUMBER ->
            if (isEnglish) "Enter a whole number only (no signs, decimals or letters)."
            else "শুধু পূর্ণসংখ্যা লিখুন (ঋণাত্মক, দশমিক বা অক্ষর নয়)।"
        NoorUpViewModel.AyatInputError.OUT_OF_RANGE ->
            if (isEnglish) "Ayat must be between 1 and $total."
            else "আয়াত ১ থেকে ${total.toBanglaDigits()} এর মধ্যে হতে হবে।"
    }

/**
 * Ayat jump dialog shared by Focus Mode and the reader. Prefills the current
 * Ayat, disables Go while the field is blank, submits from the keyboard, and
 * keeps the error visible until the user edits the value.
 */
@Composable
private fun AyatJumpDialog(
    isEnglish: Boolean,
    surahName: String,
    totalAyat: Int,
    initialAyat: Int,
    onDismiss: () -> Unit,
    onSubmit: (String) -> NoorUpViewModel.AyatInputError?
) {
    var input by remember { mutableStateOf(initialAyat.toString()) }
    var error by remember { mutableStateOf<NoorUpViewModel.AyatInputError?>(null) }

    fun submit() {
        val result = onSubmit(input)
        if (result == null) onDismiss() else error = result
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEnglish) "Go to Ayat" else "নির্দিষ্ট আয়াতে যান",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isEnglish) "$surahName • Ayat 1–$totalAyat"
                    else "$surahName • আয়াত ১–${totalAyat.toBanglaDigits()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = { Text(if (isEnglish) "Ayat number" else "আয়াত নম্বর") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        val current = error
                        if (current != null) {
                            Text(ayatErrorText(current, isEnglish, totalAyat))
                        } else {
                            Text(
                                if (isEnglish) "Enter 1–$totalAyat, then press Go"
                                else "১–${totalAyat.toBanglaDigits()} লিখে ‘যান’ চাপুন",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = input.isNotBlank(), onClick = { submit() }) {
                Text(if (isEnglish) "Go" else "যান")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isEnglish) "Cancel" else "বাতিল") }
        }
    )
}

/** Searchable Surah picker: a list of all 114 Surahs, never a number-only field. */
@Composable
private fun SurahPickerDialog(
    isEnglish: Boolean,
    surahs: List<Surah>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, surahs) {
        if (query.isBlank()) surahs
        else surahs.filter { s ->
            s.nameEnglish.contains(query, true) ||
                s.nameBangla.contains(query, true) ||
                s.number.toString() == query.trim()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEnglish) "Choose Surah" else "সূরা নির্বাচন করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (isEnglish) "Search Surah..." else "সূরা খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        if (isEnglish) "No Surah matches \"$query\"."
                        else "\"$query\" এর সাথে কোনো সূরা মেলেনি।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        items(filtered, key = { it.number }) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelect(s.number) }
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isEnglish) "${s.number}" else s.number.toBanglaDigits(),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isEnglish) s.nameEnglish else s.nameBangla,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (isEnglish) "${s.totalVerses} verses"
                                        else "${s.totalVerses.toBanglaDigits()} আয়াত",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    s.nameArabic,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 15.sp,
                                    fontFamily = QuranArabicFont,
                                    maxLines = 1
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isEnglish) "Close" else "বন্ধ") }
        }
    )
}

// --- Feature 2: Ambient Quran Soundscape & Focus Mode & Full Surah List ---
@Composable
fun FocusModeView(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val focusSurahNumber by viewModel.focusSurahNumber.collectAsState()
    val focusVerseNumber by viewModel.focusVerseNumber.collectAsState()
    val focusVerse by viewModel.focusVerse.collectAsState()
    val fontScale by viewModel.focusFontScale.collectAsState()
    val showTranslation by viewModel.focusShowTranslation.collectAsState()
    val showPronunciation by viewModel.quranShowTransliteration.collectAsState()
    val sessionCount by viewModel.focusSessionCount.collectAsState()
    val isFocusLoading by viewModel.isFocusLoading.collectAsState()
    val quranLoadError by viewModel.quranLoadError.collectAsState()
    var sessionSeconds by remember { mutableStateOf(0) }
    var showAyatDialog by remember { mutableStateOf(false) }
    var showSurahPicker by remember { mutableStateOf(false) }

    val surah = surahs.find { it.number == focusSurahNumber }
    val total = surah?.totalVerses ?: 0
    val progress = if (total > 0) focusVerseNumber.coerceIn(0, total).toFloat() / total.toFloat() else 0f
    val haptic = LocalHapticFeedback.current
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    LaunchedEffect(isEnglish) { /* re-compose translations */ }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            sessionSeconds += 1
        }
    }

    fun fmtTime(s: Int): String {
        val m = s / 60
        val r = s % 60
        val base = "%d:%02d".format(m, r)
        return if (isEnglish) base else base.toBanglaDigits()
    }

    // Softer glow in the (default) dark theme: 22% primary is glary on OLED.
    val glowAlpha = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.16f else 0.22f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), MaterialTheme.colorScheme.background)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isEnglish) "Ambient Focus Mode" else "অ্যাম্বিয়েন্ট ফোকাস মোড",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isEnglish) "${surah?.nameEnglish ?: ""} • Verse $focusVerseNumber of $total" else "${surah?.nameBangla ?: ""} • ${focusVerseNumber.toBanglaDigits()} / ${total.toBanglaDigits()} আয়াত",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            if (isEnglish) "⏱ $sessionCount read • ${fmtTime(sessionSeconds)} in focus" else "⏱ ${sessionCount.toBanglaDigits()} পঠিত • ফোকাসে ${fmtTime(sessionSeconds)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { viewModel.exitFocus() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (isEnglish) "Exit Focus Mode" else "ফোকাস মোড বন্ধ করুন",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        item {
            GlassCard(
                modifier = Modifier.pointerInput(Unit) {
                    // Horizontal swipe is a one-handed shortcut for Prev/Next; the
                    // buttons below stay as the discoverable control.
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            if (dragTotal <= -swipeThreshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.focusNextVerse()
                            } else if (dragTotal >= swipeThreshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.focusPrevVerse()
                            }
                        }
                    ) { _, dragAmount -> dragTotal += dragAmount }
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Surah jump
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (focusSurahNumber > 1) viewModel.focusJumpTo(focusSurahNumber - 1, 1)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (isEnglish) "‹ Prev Surah" else "‹ পূর্ববর্তী সূরা",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { showSurahPicker = true },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                if (isEnglish) "${surah?.nameEnglish ?: ""} (${surah?.number ?: ""})" else "${surah?.nameBangla ?: ""} (${(surah?.number ?: 0).toBanglaDigits()})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        TextButton(
                            onClick = {
                                if (focusSurahNumber < 114) viewModel.focusJumpTo(focusSurahNumber + 1, 1)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (isEnglish) "Next Surah ›" else "পরবর্তী সূরা ›",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    val v = focusVerse
                    if (v == null) {
                        if (quranLoadError && !isFocusLoading) {
                            Text(
                                if (isEnglish) "Could not load ${surah?.nameEnglish ?: "this Surah"}, Ayat $focusVerseNumber from the offline bundle."
                                else "${surah?.nameBangla ?: "এই সূরা"} সূরার ${focusVerseNumber.toBanglaDigits()} নম্বর আয়াতটি অফলাইন ভাণ্ডার থেকে লোড করা যায়নি।",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.retryFocusLoad() }) {
                                    Text(if (isEnglish) "Retry" else "পুনরায় চেষ্টা")
                                }
                                OutlinedButton(onClick = {
                                    viewModel.openSurahAt(focusSurahNumber, focusVerseNumber)
                                    viewModel.exitFocus()
                                }) {
                                    Text(if (isEnglish) "Open in reader" else "পাঠে খুলুন")
                                }
                            }
                        } else {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (isEnglish) "Loading verse…" else "আয়াত লোড হচ্ছে…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        ) {
                            Text(
                                if (isEnglish) "Verse ${v.number} of $total" else "আয়াত ${v.number.toBanglaDigits()} / ${total.toBanglaDigits()}",
                                color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        QuranArabicText(
                            arabic = v.arabicText,
                            ayahNumber = v.number,
                            fontSize = (26 * fontScale).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (showPronunciation) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                if (isEnglish) "Bengali pronunciation" else "বাংলা উচ্চারণ",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                v.banglaTransliteration
                                    ?: if (isEnglish) "Bengali pronunciation is not available for this verse."
                                    else "এই আয়াতের বাংলা উচ্চারণ ভাণ্ডারে নেই।",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = (15 * fontScale).sp,
                                lineHeight = (23 * fontScale).sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (showTranslation) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (isEnglish) v.englishTranslation else v.banglaTranslation,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = (15 * fontScale).sp,
                                textAlign = TextAlign.Center,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.toggleBookmark(focusSurahNumber, v.number) }) {
                                Icon(
                                    if (v.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (v.isBookmarked) {
                                        if (isEnglish) "Remove bookmark" else "বুকমার্ক সরান"
                                    } else {
                                        if (isEnglish) "Bookmark this Ayat" else "এই আয়াত বুকমার্ক করুন"
                                    },
                                    tint = if (v.isBookmarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.toggleMemorized(focusSurahNumber, v.number) }) {
                                Icon(
                                    if (v.isMemorized) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (v.isMemorized) {
                                        if (isEnglish) "Memorized, tap to unmark" else "মুখস্থ হয়েছে, তুলে নিতে চাপুন"
                                    } else {
                                        if (isEnglish) "Mark as memorized" else "মুখস্থ হিসেবে চিহ্নিত করুন"
                                    },
                                    tint = if (v.isMemorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.cycleFocusFontScale() }) {
                                Icon(
                                    Icons.Default.FormatSize,
                                    contentDescription = if (isEnglish) "Change Arabic text size" else "আরবি লেখার আকার পরিবর্তন করুন",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFocusTranslation() }) {
                                Icon(
                                    if (showTranslation) Icons.Default.Translate else Icons.Default.GTranslate,
                                    contentDescription = if (showTranslation) {
                                        if (isEnglish) "Hide translation" else "অনুবাদ লুকান"
                                    } else {
                                        if (isEnglish) "Show translation" else "অনুবাদ দেখান"
                                    },
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { viewModel.toggleQuranTransliteration() }) {
                                Icon(
                                    Icons.Default.RecordVoiceOver,
                                    contentDescription = if (isEnglish) {
                                        "Bengali pronunciation, ${if (showPronunciation) "on" else "off"}"
                                    } else {
                                        "বাংলা উচ্চারণ, ${if (showPronunciation) "চালু" else "বন্ধ"}"
                                    },
                                    tint = if (showPronunciation) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.focusPrevVerse() },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isEnglish) "Prev" else "পূর্ববর্তী",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = { showAyatDialog = true },
                    modifier = Modifier
                        .weight(0.8f)
                        .heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        Icons.Default.PinDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isEnglish) "Ayat" else "আয়াত",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Button(
                    onClick = { viewModel.focusNextVerse() },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (isEnglish) "Next" else "পরবর্তী",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }

        item {
            // No duplicate Exit here: the close control in the header ends Focus.
            Text(
                if (isEnglish) "Swipe left or right, or use ◀ ▶, to move between Ayat."
                else "আয়াত পরিবর্তনে বাঁয়ে-ডানে সোয়াইপ করুন অথবা ◀ ▶ চাপুন।",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showAyatDialog) {
        AyatJumpDialog(
            isEnglish = isEnglish,
            surahName = if (isEnglish) surah?.nameEnglish ?: "" else surah?.nameBangla ?: "",
            totalAyat = total,
            initialAyat = focusVerseNumber,
            onDismiss = { showAyatDialog = false },
            onSubmit = { input -> viewModel.goToAyat(focusSurahNumber, input) }
        )
    }

    if (showSurahPicker) {
        SurahPickerDialog(
            isEnglish = isEnglish,
            surahs = surahs,
            onDismiss = { showSurahPicker = false },
            onSelect = { number ->
                // Explicit Surah choice: Ayat 1, never a stale position.
                viewModel.focusJumpTo(number, 1)
                showSurahPicker = false
            }
        )
    }
}

@Composable
fun QuranScreen(viewModel: NoorUpViewModel) {
    val surahs by viewModel.surahs.collectAsState()
    val selectedSurahNumber by viewModel.selectedSurahNumber.collectAsState()
    val selectedVerses by viewModel.selectedSurahVerses.collectAsState()
    val isQuranLoading by viewModel.isQuranLoading.collectAsState()
    val quranLoadError by viewModel.quranLoadError.collectAsState()
    val bookmarkedVerses by viewModel.bookmarkedVerses.collectAsState()
    val selectedSurah = selectedSurahNumber?.let { num -> surahs.find { it.number == num } }
    var showBookmarksOnly by remember { mutableStateOf(false) }
    val isAmbientFocus by viewModel.isAmbientFocusActive.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val showPronunciation by viewModel.quranShowTransliteration.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showPronunciationSource by remember { mutableStateOf(false) }
    var bookmarkQuery by remember { mutableStateOf("") }
    var expandedBookmark by remember { mutableStateOf<String?>(null) }
    var showReaderAyatDialog by remember { mutableStateOf(false) }
    val readerResume by viewModel.readerResume.collectAsState()
    val pendingReaderAyat by viewModel.pendingReaderAyat.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(showBookmarksOnly) {
        if (showBookmarksOnly) viewModel.refreshBookmarkedVerses()
    }

    // One-shot scroll to an explicitly requested Ayat once its verses land.
    // Item 0 is the Surah header, so Ayat n sits at list index n.
    LaunchedEffect(pendingReaderAyat, selectedVerses, selectedSurahNumber, showBookmarksOnly) {
        val target = pendingReaderAyat ?: return@LaunchedEffect
        if (selectedSurahNumber == null || selectedVerses.isEmpty()) return@LaunchedEffect
        // "Open in reader" can arrive while the bookmark list is showing; the
        // requested Ayat is meaningless there, so switch to the reader and let
        // the effect restart against the verse items before scrolling.
        if (showBookmarksOnly) {
            showBookmarksOnly = false
            return@LaunchedEffect
        }
        if (target in 1..selectedVerses.size) {
            listState.scrollToItem(target)
            viewModel.consumeReaderAyatJump()
        }
    }

    if (showPronunciationSource) {
        AlertDialog(
            onDismissRequest = { showPronunciationSource = false },
            title = {
                Text(
                    if (isEnglish) "Bengali pronunciation source" else "বাংলা উচ্চারণের সূত্র",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isEnglish) {
                            "Pronunciation strings come from the MIT-licensed Khalidhassan3011/al_quran dataset (pronunciation_bn, commit b2fd1a6), which references alquran.cloud. Arabic and translations remain from NoorUp's offline Quran bundle. Two verses (26:1 and 26:227) are blank upstream and are shown as unavailable instead of being filled with invented text."
                        } else {
                            "উচ্চারণের লেখা MIT লাইসেন্সপ্রাপ্ত Khalidhassan3011/al_quran ডেটাসেট (pronunciation_bn, কমিট b2fd1a6) থেকে নেওয়া হয়েছে; ডেটাসেটটি alquran.cloud-কে সূত্র হিসেবে উল্লেখ করে। আরবি ও অনুবাদ NoorUp-এর অফলাইন কুরআন ভাণ্ডার থেকেই রয়েছে। সূত্রে সূরা ২৬:১ ও ২৬:২২৭ খালি থাকায় সেখানে অনুমান না করে স্পষ্টভাবে 'উপলভ্য নয়' দেখানো হয়।"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Text(
                        "MIT License · Copyright (c) 2022 Khalid Hassan\nhttps://github.com/Khalidhassan3011/al_quran",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPronunciationSource = false }) {
                    Text(if (isEnglish) "Close" else "বন্ধ করুন")
                }
            }
        )
    }

    if (isAmbientFocus) {
        FocusModeView(viewModel)
    } else {
        LazyColumn(
            state = listState,
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
                    // Responsive: title flexes, Focus button keeps intrinsic width so its
                    // label can never be squeezed into vertical character-by-character wrap.
                    AdaptiveHeaderTexts(
                        title = if (isEnglish) "Al-Qur'an al-Kareem" else "আল-কুরআনুল কারীম",
                        subtitle = if (isEnglish) "Full Surah Library & Hifz Tracker" else "পবিত্র কুরআন তিলাওয়াত ও হিফজ ট্র্যাকার"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // An opened Surah always enters Focus at Ayat 1; the
                            // library-level button resumes the last session instead.
                            val openSurah = selectedSurah
                            if (openSurah != null) viewModel.enterFocusAtSurahStart(openSurah.number)
                            else viewModel.toggleAmbientFocus()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isEnglish) "Focus" else "ফোকাস",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.toggleQuranTransliteration() },
                        border = BorderStroke(
                            1.dp,
                            if (showPronunciation) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (showPronunciation) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isEnglish) "Bengali pronunciation" else "বাংলা উচ্চারণ",
                            color = if (showPronunciation) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (showPronunciation) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    TextButton(onClick = { showPronunciationSource = true }) {
                        Text(
                            if (isEnglish) "Source" else "সূত্র",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
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
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (selectedSurah == null && !showBookmarksOnly) {
                val resume = readerResume
                val resumeSurah = resume?.let { (surahNumber, _) ->
                    surahs.find { it.number == surahNumber }
                }
                if (resume != null && resumeSurah != null) {
                    item {
                        GlassCard(
                            modifier = Modifier.clickable {
                                viewModel.openSurahAt(resumeSurah.number, resume.second)
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isEnglish) "Continue reading" else "পড়া চালিয়ে যান",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isEnglish) "${resumeSurah.nameEnglish} • Ayat ${resume.second}"
                                        else "${resumeSurah.nameBangla} • আয়াত ${resume.second.toBanglaDigits()}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    if (isEnglish) "Resume" else "চালিয়ে যান",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (showBookmarksOnly) {
                if (bookmarkedVerses.isEmpty()) {
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
                    item {
                        OutlinedTextField(
                            value = bookmarkQuery,
                            onValueChange = { bookmarkQuery = it },
                            placeholder = {
                                Text(if (isEnglish) "Search bookmarks..." else "বুকমার্ক খুঁজুন...")
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    val filteredBookmarks = bookmarkedVerses.filter { (surah, verse) ->
                        bookmarkQuery.isBlank() ||
                            surah.nameEnglish.contains(bookmarkQuery, true) ||
                            surah.nameBangla.contains(bookmarkQuery, true) ||
                            verse.number.toString() == bookmarkQuery.trim() ||
                            verse.arabicText.contains(bookmarkQuery, true) ||
                            verse.englishTranslation.contains(bookmarkQuery, true) ||
                            verse.banglaTranslation.contains(bookmarkQuery, true)
                    }
                    if (filteredBookmarks.isEmpty()) {
                        item {
                            Text(
                                if (isEnglish) "No bookmarks match \"$bookmarkQuery\"."
                                else "\"$bookmarkQuery\" এর সাথে কোনো বুকমার্ক মেলেনি।",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        filteredBookmarks.groupBy { it.first.number }.toSortedMap()
                            .forEach { (_, entries) ->
                                val surahMeta = entries.first().first
                                item(key = "bm_header_${surahMeta.number}") {
                                    Text(
                                        if (isEnglish) "${surahMeta.nameEnglish} (${surahMeta.number}) • ${entries.size} saved"
                                        else "সূরা ${surahMeta.nameBangla} (${surahMeta.number.toBanglaDigits()}) • ${entries.size.toBanglaDigits()}টি সংরক্ষিত",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(entries, key = { "${it.first.number}:${it.second.number}" }) { pair ->
                                    val (surah, verse) = pair
                                    val itemKey = "${surah.number}:${verse.number}"
                                    val expanded = expandedBookmark == itemKey
                                    GlassCard(
                                        // The whole card opens Focus at this exact Ayat.
                                        modifier = Modifier.clickable {
                                            viewModel.enterFocusAtBookmark(surah.number, verse.number)
                                        }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    if (isEnglish) "Surah ${surah.nameEnglish} (${surah.number}), Verse ${verse.number}"
                                                    else "সূরা ${surah.nameBangla} (${surah.number.toBanglaDigits()}), আয়াত ${verse.number.toBanglaDigits()}",
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    if (isEnglish) verse.englishTranslation else verse.banglaTranslation,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    expandedBookmark = if (expanded) null else itemKey
                                                },
                                                modifier = Modifier.size(ResponsiveRules.pillMinHeight)
                                            ) {
                                                Icon(
                                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = if (isEnglish) {
                                                        if (expanded) "Collapse Ayat" else "Expand Ayat"
                                                    } else {
                                                        if (expanded) "আয়াত সংকুচিত করুন" else "আয়াত দেখান"
                                                    },
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.toggleBookmark(surah.number, verse.number) },
                                                modifier = Modifier.size(ResponsiveRules.pillMinHeight)
                                            ) {
                                                Icon(
                                                    Icons.Default.Bookmark,
                                                    contentDescription = if (isEnglish) "Remove bookmark" else "বুকমার্ক সরান",
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                        if (expanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            QuranArabicText(
                                                arabic = verse.arabicText,
                                                ayahNumber = verse.number,
                                                fontSize = 18.sp,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.End
                                            )
                                            if (showPronunciation) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    if (isEnglish) "Bengali pronunciation" else "বাংলা উচ্চারণ",
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    verse.banglaTransliteration
                                                        ?: if (isEnglish) "Bengali pronunciation is not available for this verse."
                                                        else "এই আয়াতের বাংলা উচ্চারণ ভাণ্ডারে নেই।",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp,
                                                    textAlign = TextAlign.Start,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                if (isEnglish) verse.englishTranslation else verse.banglaTranslation,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
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
                        modifier = Modifier.clickable { viewModel.openSurah(surah.number) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                                Column(modifier = Modifier.weight(1f)) {
                                    val sName = if (isEnglish) surah.nameEnglish else surah.nameBangla
                                    val revType = if (isEnglish) surah.revelationTypeEnglish else surah.revelationTypeBangla
                                    Text(sName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("$revType • ${if (isEnglish) surah.totalVerses.toString() else surah.totalVerses.toBanglaDigits()} ${if (isEnglish) "Verses" else "আয়াত"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                surah.nameArabic,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = QuranArabicFont,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.closeSurah() }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sName, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    selectedSurah!!.nameArabic,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = QuranArabicFont
                                )
                                Text(
                                    if (isEnglish) "Revelation: $revType | Total Verses: ${selectedSurah!!.totalVerses} | Showing: ${selectedVerses.size}" else "অবতরণ: $revType | মোট আয়াত: ${selectedSurah!!.totalVerses.toBanglaDigits()} | প্রদর্শিত: ${selectedVerses.size.toBanglaDigits()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.enterFocusAtSurahStart(selectedSurah!!.number) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isEnglish) "Focus" else "ফোকাস",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showReaderAyatDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isEnglish) "Go to Ayat" else "নির্দিষ্ট আয়াতে যান",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isQuranLoading) {
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    if (isEnglish) "Loading complete ${selectedSurah!!.totalVerses} verses offline…" else "সম্পূর্ণ ${selectedSurah!!.totalVerses.toBanglaDigits()} আয়াত অফলাইনে লোড হচ্ছে…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (quranLoadError && selectedVerses.isEmpty() && !isQuranLoading) {
                        GlassCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (isEnglish) "Could not load this surah's verses from the offline bundle." else "এই সূরার আয়াত অফলাইন ভাণ্ডার থেকে লোড করা যায়নি।",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isEnglish) "No verses are shown rather than guesses. Please retry." else "অনুমান দেখানোর বদলে কিছুই দেখানো হচ্ছে না। আবার চেষ্টা করুন।",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.retrySurahLoad() }) {
                                    Text(if (isEnglish) "Retry" else "পুনরায় চেষ্টা")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(selectedVerses) { verse ->
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
                                Text(
                                    if (isEnglish) "${verse.number}" else verse.number.toBanglaDigits(),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                        QuranArabicText(
                            arabic = verse.arabicText,
                            ayahNumber = verse.number,
                            fontSize = 20.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        if (showPronunciation) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (isEnglish) "Bengali pronunciation" else "বাংলা উচ্চারণ",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                verse.banglaTransliteration
                                    ?: if (isEnglish) "Bengali pronunciation is not available for this verse."
                                    else "এই আয়াতের বাংলা উচ্চারণ ভাণ্ডারে নেই।",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val trans = if (isEnglish) verse.englishTranslation else verse.banglaTranslation
                        Text(trans, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showReaderAyatDialog) {
        selectedSurah?.let { surah ->
            AyatJumpDialog(
                isEnglish = isEnglish,
                surahName = if (isEnglish) surah.nameEnglish else surah.nameBangla,
                totalAyat = surah.totalVerses,
                initialAyat = readerResume?.takeIf { it.first == surah.number }?.second ?: 1,
                onDismiss = { showReaderAyatDialog = false },
                onSubmit = { input -> viewModel.goToAyatInReader(surah.number, input) }
            )
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
    val hadithLoadError by viewModel.hadithLoadError.collectAsState()
    val isLoadingMoreHadiths by viewModel.isLoadingMoreHadiths.collectAsState()
    val hasMoreHadiths by viewModel.hasMoreHadiths.collectAsState()

    var activeSegment by remember { mutableStateOf(0) } // 0: Duas, 1: Hadith Library
    // Notification deep link drives the segment from the ViewModel.
    val duaSegment by viewModel.duaSegment.collectAsState()
    LaunchedEffect(duaSegment) { activeSegment = duaSegment }
    // Full content of the tapped daily-hadith notification.
    val dailyHadith by viewModel.dailyHadithDialog.collectAsState()
    // Category selection is stored as a stable dataset key, not a translated
    // label, so switching language can never empty the list.
    var selectedCategoryKey by rememberSaveable { mutableStateOf(DuaSearch.ALL_CATEGORY_KEY) }
    val selectedHadithBookId by viewModel.selectedHadithBookId.collectAsState()
    val selectedHadithTopic by viewModel.selectedHadithTopic.collectAsState()
    val hadithSearchQuery by viewModel.hadithSearchQuery.collectAsState()
    var duaSearchQuery by rememberSaveable { mutableStateOf("") }

    val isEnglish by viewModel.isEnglish.collectAsState()
    val bookmarkedHadiths by viewModel.bookmarkedHadiths.collectAsState()

    // Derived from the actual dataset; no hardcoded labels that can drift.
    val duaCategories = remember(duas) { DuaSearch.categories(duas) }
    val effectiveCategoryKey = if (duaCategories.any { it.key == selectedCategoryKey }) {
        selectedCategoryKey
    } else {
        DuaSearch.ALL_CATEGORY_KEY
    }

    val hadithTopics = remember { viewModel.hadithRepository.topics }

    // Defer the expensive all-books parse until the library is actually
    // opened. This also covers notification deep links that select the tab.
    LaunchedEffect(activeSegment) {
        if (activeSegment == 1) viewModel.selectHadithBook(selectedHadithBookId)
    }

    val openedHadith = dailyHadith
    if (openedHadith != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDailyHadithDialog() },
            title = {
                Text(
                    if (isEnglish) "Hadith of the Day" else "আজকের হাদিস",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (openedHadith.arabicText.isNotBlank()) {
                        Text(
                            openedHadith.arabicText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        "\"${if (isEnglish) openedHadith.textEn else openedHadith.textBn}\"",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                    Text(
                        "— ${if (isEnglish) openedHadith.narratorEn else openedHadith.narratorBn}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        if (isEnglish) openedHadith.referenceEn else openedHadith.referenceBn,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val lesson = if (isEnglish) openedHadith.lessonEn else openedHadith.lessonBn
                    if (lesson.isNotBlank()) {
                        Text(
                            "💡 $lesson",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDailyHadithDialog() }) {
                    Text(
                        if (isEnglish) "Open Hadith Library" else "হাদিস লাইব্রেরি দেখুন",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDailyHadithDialog() }) {
                    Text(if (isEnglish) "Close" else "বন্ধ করুন")
                }
            }
        )
    }

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
                // Responsive: equal segment heights even if a label wraps.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Segment 1: Masnoon Duas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (activeSegment == 0) {
                                    Brush.horizontalGradient(listOf(Color(0xFF00897B), Color(0xFF00BFA5)))
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { activeSegment = 0; viewModel.setDuaSegment(0) }
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
                                fontWeight = if (activeSegment == 0) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    // Segment 2: Authentic Hadith Collections (8 Books)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (activeSegment == 1) {
                                    Brush.horizontalGradient(listOf(Color(0xFF00897B), Color(0xFF00BFA5)))
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { activeSegment = 1; viewModel.setDuaSegment(1) }
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
                                fontWeight = if (activeSegment == 1) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
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

                    // Responsive: card caps at 300dp on standard phones but shrinks to a
                    // viewport fraction on narrow screens (never wider than available).
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val duaCardWidth =
                            ResponsiveBreakpoints.carouselCardWidth(maxWidth.value, 300f).dp
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(duas.take(6)) { dua ->
                            Box(
                                modifier = Modifier
                                    .width(duaCardWidth)
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
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            dua.reference.substringBefore(",").substringBefore("(").trim(),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
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
                                    Text(meaning, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
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
                    items(duaCategories, key = { it.key }) { cat ->
                        val isSelected = effectiveCategoryKey == cat.key
                        Button(
                            onClick = { selectedCategoryKey = cat.key },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                cat.label(isEnglish),
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
                    // Responsive: same viewport-fraction cap as the dua carousel.
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val bookCardWidth =
                            ResponsiveBreakpoints.carouselCardWidth(maxWidth.value, 260f).dp
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(hadithBooks) { book ->
                            val isSelected = selectedHadithBookId == book.id
                            val count = allLoadedHadiths.count { it.bookId == book.id }
                            Box(
                                modifier = Modifier
                                    .width(bookCardWidth)
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
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            book.arabicName,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (isEnglish) book.authorEn else book.authorBn,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Authentic Grade Badge & Total Counts
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (book.isPartialSample) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFD97706).copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    if (isEnglish) "Partial sample" else "আংশিক নমুনা",
                                                    color = Color(0xFFD97706),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
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
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Action / Selection Indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (isEnglish) {
                                                val digits = book.totalChaptersBn.map { c ->
                                                    when (c) {
                                                        '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
                                                        '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
                                                        else -> c
                                                    }
                                                }.joinToString("").filter { it.isDigit() }.ifEmpty { "?" }
                                                "$digits chapters"
                                            } else book.totalChaptersBn,
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
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
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

                // Topic categories are meaningful only for the all-books
                // corpus. Individual books use their original chapter labels,
                // which are not a shared verified topic taxonomy.
                if (selectedHadithBookId == "all") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (isEnglish) "Topic Filter" else "বিষয়ভিত্তিক হাদিস ফিল্টার",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hadithTopics, key = { it.id }) { topic ->
                            val isSelected = selectedHadithTopic == topic.id
                            Button(
                                onClick = { viewModel.selectHadithTopic(topic.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    if (isEnglish) topic.english else topic.bangla,
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
            } else if (hadithLoadError != null) {
                item {
                    GlassCard(modifier = Modifier.padding(vertical = 20.dp)) {
                        Text(
                            if (isEnglish) "Hadith data could not be loaded. Please try again." else "হাদিসের তথ্য লোড করা যায়নি। আবার চেষ্টা করুন।",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center
                        )
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
                                        .weight(1f, fill = false)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        hadith.collection,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
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
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
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
                                modifier = Modifier.size(48.dp)
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
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
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
                                    modifier = Modifier.size(48.dp)
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
                                    modifier = Modifier.size(48.dp)
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
            // Category and query are intersected by the pure, tested DuaSearch
            // utility. Results preserve dataset order for deterministic output.
            val filteredDuas = DuaSearch.filter(
                duas = duas,
                rawQuery = duaSearchQuery,
                categoryKey = effectiveCategoryKey
            )
            val selectedCategoryLabel = duaCategories
                .firstOrNull { it.key == effectiveCategoryKey }
                ?.label(isEnglish)
                ?: if (isEnglish) "All" else "সকল"
            val hasActiveFilters = duaSearchQuery.isNotBlank() ||
                effectiveCategoryKey != DuaSearch.ALL_CATEGORY_KEY

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
                                if (isEnglish) "No Duas found" else "কোনো দুআ খুঁজে পাওয়া যায়নি",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isEnglish) {
                                    "No results for \"${duaSearchQuery.trim()}\" in $selectedCategoryLabel."
                                } else {
                                    "\"${duaSearchQuery.trim()}\" এবং \"$selectedCategoryLabel\" শর্তে কোনো ফলাফল নেই।"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            if (hasActiveFilters) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(onClick = {
                                    duaSearchQuery = ""
                                    selectedCategoryKey = DuaSearch.ALL_CATEGORY_KEY
                                }) {
                                    Text(if (isEnglish) "Clear search & category" else "অনুসন্ধান ও ক্যাটাগরি মুছুন")
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        if (isEnglish) {
                            "${filteredDuas.size} dua${if (filteredDuas.size == 1) "" else "s"} • $selectedCategoryLabel"
                        } else {
                            "${filteredDuas.size.toBanglaDigits()}টি দুআ • $selectedCategoryLabel"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                items(filteredDuas, key = { it.id }) { dua ->
                    // Content is never filtered by translation availability. The
                    // selected-language field wins, then the verified opposite
                    // language; the final message is explicit rather than blank.
                    val unavailable = if (isEnglish) "Translation unavailable" else "অনুবাদ পাওয়া যায়নি"
                    val title = if (isEnglish) dua.titleEnglish.ifBlank { dua.titleBangla } else dua.titleBangla.ifBlank { dua.titleEnglish }
                    val phonetic = if (isEnglish) dua.phoneticEnglish.ifBlank { dua.phoneticBangla } else dua.phoneticBangla.ifBlank { dua.phoneticEnglish }
                    val meaning = if (isEnglish) dua.meaningEnglish.ifBlank { dua.meaningBangla } else dua.meaningBangla.ifBlank { dua.meaningEnglish }
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                title.ifBlank { unavailable },
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
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
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
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
                        Text(
                            if (isEnglish) "Pronunciation: ${phonetic.ifBlank { unavailable }}" else "উচ্চারণ: ${phonetic.ifBlank { unavailable }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (isEnglish) "Meaning: ${meaning.ifBlank { unavailable }}" else "অর্থ: ${meaning.ifBlank { unavailable }}",
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
                                if (isEnglish) "Category: ${dua.categoryEnglish.ifBlank { dua.categoryBangla }}" else "বিভাগ: ${dua.categoryBangla.ifBlank { dua.categoryEnglish }}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val formatted = if (isEnglish) {
                                            """
                                            🤲 ${dua.titleEnglish} (${dua.titleBangla})
                                            ${dua.arabicText}

                                            Pronunciation: ${dua.phoneticEnglish}
                                            Meaning: ${dua.meaningEnglish}
                                            Reference: ${dua.reference}
                                            - NoorUp
                                            """.trimIndent()
                                        } else {
                                            """
                                            🤲 ${dua.titleBangla} (${dua.titleEnglish})
                                            ${dua.arabicText}
                                            
                                            উচ্চারণ: ${dua.phoneticBangla}
                                            অর্থ: ${dua.meaningBangla}
                                            রেফারেন্স: ${dua.reference}
                                            - নূরআপ (NoorUp)
                                            """.trimIndent()
                                        }
                                        val clip = ClipData.newPlainText("Dua", formatted)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isEnglish) "Dua copied to clipboard" else "দুআ কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, if (isEnglish) dua.titleEnglish else dua.titleBangla)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                if (isEnglish) {
                                                    """
                                                    🤲 ${dua.titleEnglish}
                                                    ${dua.arabicText}

                                                    Pronunciation: ${dua.phoneticEnglish}
                                                    Meaning: ${dua.meaningEnglish}
                                                    Reference: ${dua.reference}

                                                    — NoorUp Islamic Library
                                                    """.trimIndent()
                                                } else {
                                                    """
                                                    🤲 ${dua.titleBangla}
                                                    ${dua.arabicText}
                                                    
                                                    উচ্চারণ: ${dua.phoneticBangla}
                                                    অর্থ: ${dua.meaningBangla}
                                                    রেফারেন্স: ${dua.reference}
                                                    
                                                    — নূরআপ (NoorUp) ইসলামিক লাইব্রেরি
                                                    """.trimIndent()
                                                }
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Dua"))
                                    },
                                    modifier = Modifier.size(48.dp)
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
    val perPrayer by viewModel.perPrayerEnabled.collectAsState()
    // Permission state is re-checked every time the user returns to the app
    // (e.g. back from Settings after granting): a one-shot read would leave the
    // warning banner stuck on screen after the permission was granted.
    var notificationsAllowed by remember {
        mutableStateOf(
            try { PrayerNotificationHelper.areNotificationsEnabled(context) }
            catch (_: Exception) { true }
        )
    }
    var exactReady by remember {
        mutableStateOf(
            try { PrayerNotificationScheduler.isExactAlarmPermissionGranted(context) }
            catch (_: Exception) { true }
        )
    }
    val activity = context.applicationContext as? android.app.Application
    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose {}
        val callbacks = object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(resumed: android.app.Activity) {
                notificationsAllowed = try { PrayerNotificationHelper.areNotificationsEnabled(context) }
                catch (_: Exception) { true }
                exactReady = try { PrayerNotificationScheduler.isExactAlarmPermissionGranted(context) }
                catch (_: Exception) { true }
            }
            override fun onActivityCreated(a: android.app.Activity, b: android.os.Bundle?) {}
            override fun onActivityStarted(a: android.app.Activity) {}
            override fun onActivityPaused(a: android.app.Activity) {}
            override fun onActivityStopped(a: android.app.Activity) {}
            override fun onActivitySaveInstanceState(a: android.app.Activity, b: android.os.Bundle) {}
            override fun onActivityDestroyed(a: android.app.Activity) {}
        }
        activity.registerActivityLifecycleCallbacks(callbacks)
        onDispose { activity.unregisterActivityLifecycleCallbacks(callbacks) }
    }
    val selectedCityLocation by viewModel.selectedCityLocation.collectAsState()
    var testFeedbackMessage by remember { mutableStateOf<String?>(null) }

    GlassCard {
        // Top row with status & master switch
        // Responsive: text block flexes (weight) so the Switch is never pushed off-screen.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEnglish) "Prayer & Fasting Alerts" else "নামাজ ও সেহরি-ইফতার রিমাইন্ডার",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Honest status: the master toggle alone must never claim
                    // "Active" while the OS is actually blocking alerts.
                    val statusText = when {
                        !isNotificationEnabled -> if (isEnglish) "Disabled" else "বন্ধ রয়েছে"
                        !notificationsAllowed -> if (isEnglish) "Blocked • enable notifications" else "বন্ধ • নোটিফিকেশন চালু করুন"
                        !exactReady -> if (isEnglish) "Active • may delay (allow exact)" else "সক্রিয় • দেরি হতে পারে"
                        else -> if (isEnglish) "Active • High Priority" else "সক্রিয় • উচ্চ গুরুত্ব সতর্কতা"
                    }
                    val statusColor = when {
                        !isNotificationEnabled -> Color.Gray
                        !notificationsAllowed -> MaterialTheme.colorScheme.error
                        !exactReady -> GoldAccent
                        else -> Color(0xFF10B981)
                    }
                    Text(
                        statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
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

        // Exact On-Time Engine Badge (reflects reality: amber when degraded)
        val engineActive = isNotificationEnabled && notificationsAllowed && exactReady
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = (if (engineActive) Color(0xFF10B981) else GoldAccent).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, (if (engineActive) Color(0xFF10B981) else GoldAccent).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (engineActive) Color(0xFF10B981) else GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (engineActive) {
                        if (isEnglish) "Exact On-Time Engine: Active (AlarmManager RTC Wakeup)"
                        else "অন-টাইম ইঞ্জিন: সক্রিয় (অ্যালার্ম ম্যানেজার আরটিসি ওয়েকআপ)"
                    } else {
                        if (isEnglish) "Alert engine degraded — grant permissions above for exact timing"
                        else "অ্যালার্ট ইঞ্জিন সীমিত — সঠিক সময়ের জন্য উপরে অনুমতি দিন"
                    },
                    color = if (engineActive) Color(0xFF10B981) else GoldAccent,
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
        // Responsive: row height follows the tallest button so wrapped labels
        // always leave both buttons exactly equal height.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight().pairedButtonHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                TwoLineCenterActionLabel(
                    text = if (isEnglish) "Instant Alert" else "তাত্ক্ষণিক টেস্ট",
                    color = Color.White
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight().pairedButtonHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                TwoLineCenterActionLabel(
                    text = if (isEnglish) "Test Exact (5s)" else "অন-টাইম টেস্ট (৫ সে)",
                    color = Color(0xFF10B981)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.triggerHadithReminderTest(context)
                    testFeedbackMessage = if (isEnglish) {
                        "📖 09:00 AM Daily Hadith alert posted! Check your notification shade."
                    } else {
                        "📖 সকাল ০৯:০০ দৈনিক নূর হাদিস পাঠানো হয়েছে! নোটিফিকেশন চেক করুন।"
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight().pairedButtonHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                TwoLineCenterActionLabel(
                    text = if (isEnglish) "Daily Hadith Test" else "দৈনিক হাদিস টেস্ট",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
            }

            OutlinedButton(
                onClick = {
                    viewModel.triggerGardenReminderTest(context)
                    testFeedbackMessage = if (isEnglish) {
                        "🌿 10:00 PM Noor Garden reminder posted! Check your notification shade."
                    } else {
                        "🌿 রাত ১০:০০ নূর বাগান রিমাইন্ডার পাঠানো হয়েছে! ড্রয়ার চেক করুন।"
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight().pairedButtonHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldAccent)
            ) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                TwoLineCenterActionLabel(
                    text = if (isEnglish) "Garden 10PM Test" else "বাগান ১০PM টেস্ট",
                    color = GoldAccent,
                    fontSize = 11.sp
                )
            }
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

        // Permission status (fixes silent-dead reminders on Android 13+).
        if (!notificationsAllowed || !exactReady) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        if (!notificationsAllowed) {
                            if (isEnglish) "⚠️ Notifications blocked — alerts won't show. Tap Allow."
                            else "⚠️ নোটিফিকেশন বন্ধ — অ্যালার্ট আসবে না। অনুমতি দিন।"
                        } else {
                            if (isEnglish) "⚠️ Exact alarms blocked — alerts may delay. Tap to allow."
                            else "⚠️ সঠিক অ্যালার্ম বন্ধ — দেরি হতে পারে। অনুমতি দিন।"
                        },
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!notificationsAllowed) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) { }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (isEnglish) "Open Settings" else "সেটিংস খুলুন", fontSize = 12.sp)
                            }
                        }
                        if (!exactReady) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    } catch (_: Exception) { }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (isEnglish) "Allow exact" else "সঠিক অ্যালার্ম দিন", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Per-prayer toggles (were missing entirely — master switch only).
        Text(
            if (isEnglish) "Per-prayer alerts:" else "প্রতি ওয়াক্তের অ্যালার্ট:",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        val prayerToggleKeys = listOf("Fajr" to "ফজর", "Dhuhr" to "যোহর", "Asr" to "আসর", "Maghrib" to "মাগরিব", "Isha" to "এশা")
        prayerToggleKeys.forEach { (key, bn) ->
            val checked = perPrayer[key] != false
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isEnglish) {
                        when (key) {
                            "Fajr" -> "Fajr (incl. Sehri)"
                            "Maghrib" -> "Maghrib (incl. Iftar)"
                            else -> key
                        }
                    } else bn,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = checked && isNotificationEnabled,
                    enabled = isNotificationEnabled,
                    onCheckedChange = { viewModel.setPrayerEnabled(key, it, context) }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

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
            Triple(if (isEnglish) "Daily Hadith" else "দৈনিক নূর হাদিস", "09:00 AM", if (isEnglish) "Spiritual boost & authentic guidance" else "প্রতিদিন সকাল ৯টায় আত্মশুদ্ধির বাণী"),
            Triple(if (isEnglish) "🌱 Noor Garden Habit Update" else "🌱 নূর বাগান আমল রিমাইন্ডার", "10:00 PM", if (isEnglish) "Daily 10:00 PM check to cultivate habits & complete prayer log" else "প্রতিদিন রাত ১০:০০ টায় সারাদিনের আমল পূর্ণ করার সতর্কবার্তা")
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        time,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
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

// --- ToolsScreen (Prayer Alerts, Tasbih, Qibla, Hijri Calendar, Zakat, Home Widget) ---
@Composable
fun ToolsScreen(viewModel: NoorUpViewModel) {
    val context = LocalContext.current
    var toolTab by remember { mutableStateOf(0) }
    val isEnglish by viewModel.isEnglish.collectAsState()
    val toolNames = if (isEnglish) {
        listOf("Prayer Alerts", "Digital Tasbih", "Solar Engine", "Qibla", "Hijri", "Zakat", "Home Widget")
    } else {
        listOf("ওয়াক্ত অ্যালার্ট", "ডিজিটাল তসবীহ", "সৌর ইঞ্জিন", "কিবলা কম্পাস", "হিজরি", "যাকাত", "হোম উইজেট")
    }

    val tasbihCount by viewModel.tasbihCount.collectAsState()
    val tasbihGoal by viewModel.tasbihGoal.collectAsState()
    val activeDhikr by viewModel.activeDhikr.collectAsState()
    val customDhikrs by viewModel.customDhikrs.collectAsState()
    val tasbihStageIndex by viewModel.tasbihStageIndex.collectAsState()
    val tasbihCompleted by viewModel.tasbihCompleted.collectAsState()
    val selectedCityLocation by viewModel.selectedCityLocation.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingDhikr by remember { mutableStateOf<TasbihDhikr?>(null) }
    var deleteTarget by remember { mutableStateOf<TasbihDhikr?>(null) }
    var completionEvent by remember { mutableStateOf<TasbihCompletionEvent?>(null) }

    // No replay: this only fires for a round completed while this screen is
    // observing. Restoring an already-completed round does not re-celebrate.
    LaunchedEffect(Unit) {
        viewModel.completionEvents.collect { completionEvent = it }
    }

    if (showGoalDialog) {
        var goalInput by remember(tasbihGoal) { mutableStateOf("$tasbihGoal") }
        var goalError by remember { mutableStateOf(false) }
        fun parseGoal(): Int? {
            // Accept Bengali/Arabic-Indic digits too, then clamp in the ViewModel.
            val ascii = goalInput.trim().map { ch ->
                when (ch) {
                    '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
                    '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
                    '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
                    '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                    else -> ch
                }
            }.joinToString("").filter { it in '0'..'9' }
            val parsed = ascii.toIntOrNull()
            return if (parsed == null || parsed < 1) null else parsed
        }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    if (isEnglish) "Set Tasbih Goal" else "তসবীহ লক্ষ্য নির্ধারণ",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isEnglish) "Choose a preset or type any number (1–100000). Your current count is kept."
                        else "প্রিসেট বেছে নিন বা যেকোনো সংখ্যা লিখুন (১–১০০০০০)। বর্তমান গণনা অপরিবর্তিত থাকবে।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    val presets = listOf(33, 100, 500, 1000)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            val label = if (isEnglish) "$preset" else "$preset".toBanglaDigits()
                            val isChosen = goalInput.trim() == "$preset"
                            OutlinedButton(
                                onClick = { goalInput = "$preset"; goalError = false },
                                border = if (isChosen) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Text(
                                    label,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it; goalError = false },
                        label = { Text(if (isEnglish) "Goal" else "লক্ষ্য") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = goalError,
                        supportingText = if (goalError) {
                            {
                                Text(
                                    if (isEnglish) "Enter a number from 1 to 100000"
                                    else "১ থেকে ১০০০০০-এর মধ্যে সংখ্যা দিন"
                                )
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = parseGoal()
                    if (parsed == null) {
                        goalError = true
                    } else {
                        viewModel.setTasbihGoal(parsed)
                        showGoalDialog = false
                    }
                }) {
                    Text(
                        if (isEnglish) "Save" else "সংরক্ষণ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }

    completionEvent?.let { event ->
        TasbihCompletionDialog(
            event = event,
            isEnglish = isEnglish,
            onDismiss = { completionEvent = null }
        )
    }

    if (showManageDialog) {
        TasbihManageDialog(
            builtIns = TasbihRepository.builtIns,
            customDhikrs = customDhikrs,
            isEnglish = isEnglish,
            onDismiss = { showManageDialog = false },
            onCreate = {
                editingDhikr = null
                showManageDialog = false
                showEditorDialog = true
            },
            onEdit = { target ->
                editingDhikr = target
                showManageDialog = false
                showEditorDialog = true
            },
            onDelete = { target -> deleteTarget = target },
            onDuplicate = { target ->
                viewModel.duplicateBuiltInDhikr(target.id)
                Toast.makeText(
                    context,
                    if (isEnglish) "Created an editable copy" else "সম্পাদনযোগ্য কপি তৈরি হয়েছে",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showEditorDialog) {
        TasbihEditorDialog(
            initial = editingDhikr,
            isEnglish = isEnglish,
            onDismiss = {
                showEditorDialog = false
                editingDhikr = null
            },
            onSave = { titleBn, titleEn, arabic, stages ->
                val saved = if (editingDhikr == null) {
                    viewModel.addCustomDhikr(titleBn, titleEn, arabic, stages) != null
                } else {
                    viewModel.updateCustomDhikr(
                        id = editingDhikr!!.id,
                        titleBangla = titleBn,
                        titleEnglish = titleEn,
                        arabicText = arabic,
                        stages = stages
                    )
                }
                if (saved) {
                    showEditorDialog = false
                    editingDhikr = null
                }
                saved
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    if (isEnglish) "Delete this zikr?" else "এই যিকিরটি মুছে ফেলবেন?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isEnglish) {
                        "\"${target.title(true)}\" and its saved stages will be removed. Built-in zikr are protected and never appear here."
                    } else {
                        "\"${target.title(false)}\" এবং এর স্তরগুলো মুছে যাবে। বিল্ট-ইন যিকির সুরক্ষিত, তাই এখানে আসে না।"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomDhikr(target.id)
                    deleteTarget = null
                }) {
                    Text(if (isEnglish) "Delete" else "মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }

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
                Icons.Default.WbSunny,
                Icons.Default.Explore,
                Icons.Default.CalendarMonth,
                Icons.Default.AccountBalanceWallet,
                Icons.Default.Widgets
            )

            // Responsive: scrollable row keeps full labels (no squeezing); trailing
            // padding keeps the last pill from hard-clipping at the viewport edge.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(toolNames.size) { index ->
                    val isSelected = toolTab == index
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
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
            // Digital Tasbih — built-in + user-created definitions, optional
            // multi-stage rounds, one completion event per round.
            item {
                GlassCard {
                    val haptic = LocalHapticFeedback.current
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val tasbihBrush = if (isDark) {
                        Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.background))
                    } else {
                        Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.surface))
                    }
                    val activeStage = activeDhikr.stages.getOrNull(tasbihStageIndex)
                    val progress = if (tasbihGoal > 0) {
                        (tasbihCount.toFloat() / tasbihGoal.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val countText = if (isEnglish) "$tasbihCount" else tasbihCount.toBanglaDigits()
                    val goalText = if (isEnglish) "$tasbihGoal" else tasbihGoal.toBanglaDigits()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            activeDhikr.fullTitle(isEnglish),
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeDhikr.arabicText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                activeDhikr.arabicText,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 21.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (activeDhikr.stages.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                if (isEnglish) {
                                    "Stage ${tasbihStageIndex + 1} of ${activeDhikr.stages.size}: ${activeStage?.label(true).orEmpty()}"
                                } else {
                                    "স্তর ${(tasbihStageIndex + 1).toBanglaDigits()}/${activeDhikr.stages.size.toBanglaDigits()}: ${activeStage?.label(false).orEmpty()}"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                activeDhikr.stages.indices.forEach { index ->
                                    Box(
                                        modifier = Modifier
                                            .width(if (index == tasbihStageIndex) 26.dp else 16.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                when {
                                                    index < tasbihStageIndex -> MaterialTheme.colorScheme.primary
                                                    index == tasbihStageIndex -> MaterialTheme.colorScheme.secondary
                                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                                }
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.size(204.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 7.dp,
                                color = if (tasbihCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(178.dp)
                                    .clip(CircleShape)
                                    .background(tasbihBrush)
                                    .border(
                                        2.dp,
                                        if (tasbihCompleted) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                    .clickable {
                                        if (!tasbihCompleted) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        viewModel.incrementTasbih()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        countText,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "/ $goalText",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (tasbihCompleted) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                if (isEnglish) "Complete" else "সম্পন্ন",
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Text(
                                            if (isEnglish) "Tap to count" else "গণনার জন্য চাপুন",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (tasbihCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        if (tasbihCompleted) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        if (isEnglish) "Round complete — tap New round to begin again."
                                        else "তসবীহ পূর্ণ হয়েছে — আবার শুরু করতে 'নতুন রাউন্ড' চাপুন।",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.resetTasbih() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tasbihCompleted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    if (tasbihCompleted) {
                                        if (isEnglish) "New round" else "নতুন রাউন্ড"
                                    } else {
                                        if (isEnglish) "Reset" else "রিসেট"
                                    },
                                    color = if (tasbihCompleted) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(onClick = { showGoalDialog = true }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    if (activeDhikr.stages.size > 1) {
                                        if (isEnglish) "Stage goal" else "স্তরের লক্ষ্য"
                                    } else {
                                        if (isEnglish) "Goal" else "লক্ষ্য"
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isEnglish) "Built-in Dhikr" else "বিল্ট-ইন যিকির",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(onClick = { showManageDialog = true }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Manage" else "ম্যানেজ")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(TasbihRepository.builtIns, key = { it.id }) { dhikr ->
                                TasbihDhikrChip(
                                    label = dhikr.title(isEnglish),
                                    selected = activeDhikr.id == dhikr.id,
                                    onClick = { viewModel.selectDhikr(dhikr.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isEnglish) "My Zikr" else "আমার যিকির",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(onClick = {
                                editingDhikr = null
                                showEditorDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Create" else "তৈরি করুন")
                            }
                        }
                        if (customDhikrs.isEmpty()) {
                            Text(
                                if (isEnglish) {
                                    "Create your own zikr with a target, or a multi-stage sequence such as Subhanallah → Alhamdulillah → Allahu Akbar."
                                } else {
                                    "নিজের যিকির ও লক্ষ্য তৈরি করুন, অথবা সুবহানাল্লাহ → আলহামদুলিল্লাহ → আল্লাহু আকবারের মতো ধাপগুলো দিয়ে একটি রাউন্ড সাজান।"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(customDhikrs, key = { it.id }) { dhikr ->
                                    TasbihDhikrChip(
                                        label = dhikr.title(isEnglish),
                                        selected = activeDhikr.id == dhikr.id,
                                        onClick = { viewModel.selectDhikr(dhikr.id) }
                                    )
                                }
                                item {
                                    OutlinedButton(
                                        onClick = {
                                            editingDhikr = null
                                            showEditorDialog = true
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "New" else "নতুন", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (toolTab == 2) {
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
                                Text(
                                    label,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                timeStr,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        } else if (toolTab == 3) {
            // Live Interactive Qibla Compass
            item {
                QiblaCompassView(viewModel = viewModel)
            }
        } else if (toolTab == 4) {
            // Comprehensive Interactive Hijri Calendar & Milestones Hub
            item {
                HijriCalendarHubView(viewModel = viewModel)
            }
        } else if (toolTab == 5) {
            // Comprehensive Zakat & Sadaqah Calculator
            item {
                ZakatCalculatorView(viewModel = viewModel)
            }
        } else if (toolTab == 6) {
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
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
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isEnglish) selectedCityLocation.nameEn else selectedCityLocation.nameBn,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
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
                                                fontSize = 10.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

// --- Tasbih UI helpers ---
@Composable
private fun TasbihDhikrChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
    ) {
        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TasbihCompletionDialog(
    event: TasbihCompletionEvent,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { TasbihFeedback.isReducedMotion(context) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (reducedMotion || entered) 1f else 0.92f,
        animationSpec = tween(durationMillis = 220),
        label = "tasbihCompletionScale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(if (reducedMotion) 1f else scale),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    if (isEnglish) "MashaAllah!" else "আলহামদুলিল্লাহ!",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    event.title(isEnglish),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isEnglish) {
                        "Round complete • ${event.totalTarget} counts" +
                            if (event.stageCount > 1) " • ${event.stageCount} stages" else ""
                    } else {
                        "একটি রাউন্ড সম্পন্ন হয়েছে • ${event.totalTarget.toBanglaDigits()} বার যিকির" +
                            if (event.stageCount > 1) " • ${event.stageCount.toBanglaDigits()}টি ধাপ" else ""
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (isEnglish) "May your dhikr be accepted." else "আপনার যিকির কবুল হোক।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEnglish) "Alhamdulillah" else "আলহামদুলিল্লাহ")
                }
            }
        }
    }
}

private fun TasbihCompletionEvent.title(isEnglish: Boolean): String =
    if (isEnglish) titleEnglish.ifBlank { titleBangla } else titleBangla.ifBlank { titleEnglish }

@Composable
private fun TasbihManageDialog(
    builtIns: List<TasbihDhikr>,
    customDhikrs: List<TasbihDhikr>,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (TasbihDhikr) -> Unit,
    onDelete: (TasbihDhikr) -> Unit,
    onDuplicate: (TasbihDhikr) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEnglish) "Manage Zikr" else "যিকির ম্যানেজ করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        if (isEnglish) "Built-in (protected)" else "বিল্ট-ইন (সুরক্ষিত)",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(builtIns, key = { "builtin_${it.id}" }) { dhikr ->
                    TasbihManageRow(
                        dhikr = dhikr,
                        isEnglish = isEnglish,
                        trailing = {
                            TextButton(onClick = { onDuplicate(dhikr) }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isEnglish) "Copy" else "কপি", fontSize = 11.sp)
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isEnglish) "My Zikr" else "আমার যিকির",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (customDhikrs.isEmpty()) {
                    item {
                        Text(
                            if (isEnglish) "No custom zikr yet." else "এখনো কোনো কাস্টম যিকির নেই।",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    items(customDhikrs, key = { "custom_${it.id}" }) { dhikr ->
                        TasbihManageRow(
                            dhikr = dhikr,
                            isEnglish = isEnglish,
                            trailing = {
                                Row {
                                    IconButton(onClick = { onEdit(dhikr) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = if (isEnglish) "Edit" else "সম্পাদনা",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { onDelete(dhikr) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = if (isEnglish) "Delete" else "মুছুন",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isEnglish) "Create new" else "নতুন তৈরি করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Close" else "বন্ধ করুন")
            }
        }
    )
}

@Composable
private fun TasbihManageRow(
    dhikr: TasbihDhikr,
    isEnglish: Boolean,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(start = 10.dp, top = 7.dp, bottom = 7.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                dhikr.title(isEnglish),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (dhikr.stages.size > 1) {
                    if (isEnglish) "${dhikr.stages.size} stages • ${dhikr.totalTarget} counts"
                    else "${dhikr.stages.size.toBanglaDigits()} স্তর • ${dhikr.totalTarget.toBanglaDigits()} গণনা"
                } else {
                    if (isEnglish) "${dhikr.totalTarget} counts" else "${dhikr.totalTarget.toBanglaDigits()} গণনা"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
        trailing()
    }
}

private data class TasbihStageDraft(
    val label: String,
    val target: String
)

private fun parseTasbihNumber(raw: String): Int? {
    val ascii = raw.trim().map { ch ->
        when (ch) {
            '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
            '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
            '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
            '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
            else -> ch
        }
    }.joinToString("")
    if (ascii.isEmpty() || ascii.any { it !in '0'..'9' }) return null
    return ascii.toIntOrNull()
}

@Composable
private fun TasbihEditorDialog(
    initial: TasbihDhikr?,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onSave: (titleBn: String, titleEn: String, arabic: String, stages: List<TasbihStage>) -> Boolean
) {
    var titleBn by remember(initial?.id) { mutableStateOf(initial?.titleBangla.orEmpty()) }
    var titleEn by remember(initial?.id) { mutableStateOf(initial?.titleEnglish.orEmpty()) }
    var arabic by remember(initial?.id) { mutableStateOf(initial?.arabicText.orEmpty()) }
    var drafts by remember(initial?.id) {
        mutableStateOf(
            initial?.stages?.map { TasbihStageDraft(it.labelBangla, it.target.toString()) }
                ?: listOf(TasbihStageDraft("", "33"))
        )
    }
    var error by remember(initial?.id) { mutableStateOf("") }

    fun save() {
        val targets = drafts.map { parseTasbihNumber(it.target) }
        if (titleBn.isBlank() && titleEn.isBlank()) {
            error = if (isEnglish) "Enter a title first." else "প্রথমে একটি শিরোনাম দিন।"
            return
        }
        if (targets.any { it == null || it !in 1..TasbihRepository.MAX_TARGET }) {
            error = if (isEnglish) {
                "Every stage count must be a number from 1 to ${TasbihRepository.MAX_TARGET}."
            } else {
                "প্রতিটি স্তরের গণনা ১ থেকে ${TasbihRepository.MAX_TARGET.toBanglaDigits()} এর মধ্যে হতে হবে।"
            }
            return
        }
        val stages = drafts.mapIndexed { index, draft ->
            val label = draft.label.trim()
            TasbihStage(
                labelBangla = label,
                labelEnglish = label,
                target = targets[index]!!
            )
        }
        val ok = onSave(titleBn.trim(), titleEn.trim(), arabic.trim(), stages)
        if (!ok) {
            error = if (isEnglish) "Could not save. Check the values and try again."
            else "সংরক্ষণ করা যায়নি। মানগুলো যাচাই করে আবার চেষ্টা করুন।"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    initial != null && isEnglish -> "Edit Zikr"
                    initial != null -> "যিকির সম্পাদনা"
                    isEnglish -> "Create Zikr"
                    else -> "নতুন যিকির"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleBn,
                    onValueChange = { titleBn = it; error = "" },
                    label = { Text(if (isEnglish) "Title (Bangla)" else "শিরোনাম (বাংলা)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it; error = "" },
                    label = { Text(if (isEnglish) "Title (English)" else "শিরোনাম (ইংরেজি)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = arabic,
                    onValueChange = { arabic = it; error = "" },
                    label = { Text(if (isEnglish) "Arabic text (optional)" else "আরবি লেখা (ঐচ্ছিক)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (isEnglish) "Stages" else "স্তর",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isEnglish) {
                        "One stage is a simple counter. Add stages for a sequence; the celebration appears only after the final stage."
                    } else {
                        "একটি স্তর সাধারণ কাউন্টার। ধাপে ধাপে রাউন্ড সাজাতে একাধিক স্তর যোগ করুন; শেষ স্তর শেষ হলেই সম্পন্ন হওয়ার বার্তা দেখানো হবে।"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                drafts.forEachIndexed { index, draft ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.label,
                            onValueChange = { value ->
                                drafts = drafts.toMutableList().also {
                                    it[index] = it[index].copy(label = value)
                                }
                                error = ""
                            },
                            label = {
                                Text(
                                    if (isEnglish) "Stage ${index + 1} label"
                                    else "স্তর ${(index + 1).toBanglaDigits()} নাম"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.target,
                            onValueChange = { value ->
                                drafts = drafts.toMutableList().also {
                                    it[index] = it[index].copy(target = value)
                                }
                                error = ""
                            },
                            label = { Text(if (isEnglish) "Count" else "গণনা") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(92.dp)
                        )
                        if (drafts.size > 1) {
                            IconButton(onClick = {
                                drafts = drafts.toMutableList().also { it.removeAt(index) }
                                error = ""
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = if (isEnglish) "Remove stage" else "স্তর সরান",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                if (drafts.size < TasbihRepository.MAX_STAGES) {
                    OutlinedButton(
                        onClick = {
                            drafts = drafts + TasbihStageDraft("", "33")
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(if (isEnglish) "Add stage" else "স্তর যোগ করুন")
                    }
                }
                if (error.isNotBlank()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { save() }) {
                Text(
                    if (isEnglish) "Save" else "সংরক্ষণ",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Cancel" else "বাতিল")
            }
        }
    )
}

// --- AiAssistantScreen (offline Islamic companion) ---
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
            if (isEnglish) "Islamic Assistant" else "ইসলামিক সহকারী",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (isEnglish) "100% offline — answers from built-in Islamic knowledge" else "১০০% অফলাইন — ভেতরের ইসলামিক জ্ঞান থেকে উত্তর",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
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
                                text = if (msg.isUser) (if (isEnglish) "You" else "আপনি") else (if (isEnglish) "NoorUp Assistant" else "নূরআপ সহকারী"),
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
            listOf("আসসালামু আলাইকুম", "ওযুর নিয়ম", "নামাজের ওয়াক্ত ও নিয়ম", "জুমার আমল", "তাহাজ্জুদ নামাজের ফযিলত", "রমজান ও রোজা", "সেহরি ও ইফতার", "যাকাতের নিসাব", "হজ ও ওমরাহ", "কুরআন শিখুন", "আয়াতুল কুরসি", "মাসনূন দোয়াসমূহ", "সকাল-সন্ধ্যার আমল", "তওবা ও ইস্তিগফার", "পিতামাতার হক")
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
    var gardenRecord by remember { mutableStateOf<DailyRecord?>(null) }

    // Recalculate date-based Qada whenever this screen is opened.
    LaunchedEffect(Unit) { viewModel.refreshConsistencyHistory() }

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
                        // Responsive: equal shares so the two metrics never collide on narrow screens.
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isEnglish) "Overall Prayer Health" else "সামগ্রিক নামাজের স্বাস্থ্য",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (isEnglish) "$overallHealthPercent%" else "${overallHealthPercent.toBanglaDigits()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                if (isEnglish) "Consistency Streak" else "ধারাবাহিকতা স্ট্রিক",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (isEnglish) "🔥 $currentStreak Days" else "🔥 ${currentStreak.toBanglaDigits()} দিন",
                                color = GoldAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
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
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Fluid Micro-Interactions: Sub-tabs (Timeline vs Qada Ledger)
            // Responsive: equal heights even when one label wraps to two lines.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { subTab = 0 },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight().pairedButtonHeight(),
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
                    TwoLineCenterActionLabel(
                        text = if (isEnglish) "Consistency Timeline" else "ধারাবাহিকতা টাইমলাইন",
                        color = if (subTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (subTab == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }
                Button(
                    onClick = { subTab = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight().pairedButtonHeight(),
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
                    TwoLineCenterActionLabel(
                        text = if (isEnglish) "Qada Ledger" else "বকেয়া কাজা খাতা",
                        color = if (subTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
                    GlassCard(
                        modifier = Modifier.clickable {
                            viewModel.openGardenForDate(record.dateKey)
                            gardenRecord = record
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(record.dayLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (isEnglish) "${record.completedCount}/${record.totalCount} Prayers completed" else "${record.completedCount.toBanglaDigits()}/${record.totalCount.toBanglaDigits()} ওয়াক্ত আদায় হয়েছে",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${record.scorePercent}%",
                                color = if (record.scorePercent >= 80) MaterialTheme.colorScheme.primary else GoldAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (isEnglish) "Pending Qada count" else "বাকি কাজা সংখ্যা",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.recordMissedQada(key) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = if (isEnglish) "Record missed prayer" else "হারানো নামাজ রেকর্ড করুন", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (isEnglish) "$count" else count.toBanglaDigits(),
                                color = if (count > 0) GoldAccent else MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 32.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { viewModel.fulfillQada(key) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = if (isEnglish) "Mark fulfilled" else "আদায়কৃত চিহ্নিত করুন", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Keep the timeline visible while the user corrects the chosen day's prayers.
    gardenRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { gardenRecord = null },
            title = {
                Column {
                    Text(
                        if (isEnglish) "Update Noor Garden" else "নূর বাগান আপডেট করুন",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        record.dayLabel,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = { NoorGardenCard(viewModel) },
            confirmButton = {
                TextButton(onClick = { gardenRecord = null }) {
                    Text(if (isEnglish) "Done" else "সম্পন্ন")
                }
            }
        )
    }
}

private fun fetchDeviceLocation(context: Context, viewModel: NoorUpViewModel, onStatus: (String) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
        val isNetworkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }

        if (!isGpsEnabled && !isNetworkEnabled) {
            onStatus("GPS is disabled. Please enable location services.")
            return
        }

        val location = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            onStatus("Location permission required.")
            return
        } catch (_: Exception) { null }

        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            // Geocoder is blocking IO — never on the main thread (ANR). Resolve off-thread.
            onStatus("Resolving area name… / এলাকার নাম খোঁজা হচ্ছে…")
            Thread {
                var areaEn = "Live Area"
                var areaBn = "বর্তমান এলাকা"
                try {
                    val geocoder = Geocoder(context, Locale("bn", "BD"))
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        areaEn = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "GPS Location"
                        areaBn = addr.locality ?: addr.subAdminArea ?: "জিপিএস অবস্থান"
                    }
                } catch (_: Exception) { }
                try {
                    viewModel.updateGpsLocation(lat, lng, areaEn, areaBn)
                } catch (_: Exception) { }
                try {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onStatus("GPS Location acquired successfully! / সফলভাবে জিপিএস লোকেশন পাওয়া গেছে!")
                    }
                } catch (_: Exception) { }
            }.start()
        } else {
            // Honest fallback: last-known is null (fresh boot/indoors). Never claim live fix.
            viewModel.updateGpsLocation(23.8759, 90.3795, "Uttara, Dhaka", "উত্তরা, ঢাকা")
            onStatus("GPS unavailable — using default Uttara, Dhaka. Enable GPS and retry. / জিপিএস পাওয়া যায়নি — ডিফল্ট উত্তরা, ঢাকা ব্যবহার হচ্ছে।")
        }
    } catch (e: SecurityException) {
        onStatus("Location permission required.")
    } catch (e: Exception) {
        onStatus("Error: ${e.localizedMessage}")
    }
}

