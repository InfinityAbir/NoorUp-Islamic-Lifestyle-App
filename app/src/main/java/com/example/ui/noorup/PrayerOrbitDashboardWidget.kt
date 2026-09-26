package com.example.ui.noorup

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Solar & Prayer Stages for the Orbit Visualizer (Includes 3 Prohibited / Makruh Times)
 */
enum class OrbitPrayerStage(
    val key: String,
    val nameBn: String,
    val nameEn: String,
    val arabic: String,
    val isMandatory: Boolean,
    val isMakruh: Boolean = false,
    val makruhReasonBn: String = "",
    val makruhReasonEn: String = ""
) {
    FAJR("ফজর", "ফজর", "Fajr", "الفجر", true),
    SUNRISE("সূর্যোদয়", "সূর্যোদয়", "Sunrise", "الشروق", false, true, "সূর্যোদয়কালীন সময়ে সালাত নিষিদ্ধ", "Prohibited during sunrise"),
    ZAWAL("দ্বিপ্রহর", "দ্বিপ্রহর / যাওয়াল", "Zawal Zenith", "نصف النهار", false, true, "সূর্য মাথার উপর থাকাকালে সালাত নিষিদ্ধ", "Prohibited at solar zenith"),
    DHUHR("যোহর", "যোহর", "Dhuhr", "الظهر", true),
    ASR("আসর", "আসর", "Asr", "العصر", true),
    SUNSET("সূর্যাস্ত", "সূর্যাস্ত", "Sunset", "الغروب", false, true, "সূর্যাস্তের রক্তিম সময়ে সালাত নিষিদ্ধ", "Prohibited right before sunset"),
    MAGHRIB("মাগরিব", "মাগরিব", "Maghrib", "المغرب", true),
    ISHA("এশা", "এশা", "Isha", "العشاء", true)
}

/**
 * Minimalist Custom Vector Graphics for Each Prayer Stage (Drawn with Compose Canvas)
 */
@Composable
fun PrayerStageVector(
    stage: OrbitPrayerStage,
    isActive: Boolean,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = if (stage.isMakruh) Color(0xFFF59E0B) else Color(0xFF10B981)
) {
    val normalColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val tint = if (isActive) activeColor else normalColor

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (stage) {
            OrbitPrayerStage.FAJR -> {
                // Horizon baseline + rising dawn sun crescent + 3 ascending dawn rays
                val baseLineY = cy + h * 0.2f
                drawLine(
                    color = tint.copy(alpha = 0.6f),
                    start = Offset(w * 0.15f, baseLineY),
                    end = Offset(w * 0.85f, baseLineY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Rising semi-disc
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - w * 0.22f, baseLineY - w * 0.22f),
                    size = Size(w * 0.44f, w * 0.44f)
                )
                // 3 Dawn Rays ascending
                val rayLength = h * 0.22f
                val angles = listOf(-135.0, -90.0, -45.0)
                for (deg in angles) {
                    val rad = Math.toRadians(deg)
                    val rStart = w * 0.30f
                    val rEnd = rStart + rayLength
                    drawLine(
                        color = tint,
                        start = Offset(cx + (rStart * cos(rad)).toFloat(), baseLineY + (rStart * sin(rad)).toFloat()),
                        end = Offset(cx + (rEnd * cos(rad)).toFloat(), baseLineY + (rEnd * sin(rad)).toFloat()),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            OrbitPrayerStage.SUNRISE -> {
                // Horizon line + full golden rising orb resting on the horizon + 3 solar sparkles
                val groundY = cy + h * 0.22f
                drawLine(
                    color = tint.copy(alpha = 0.6f),
                    start = Offset(w * 0.12f, groundY),
                    end = Offset(w * 0.88f, groundY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Sun orb sitting on horizon
                drawCircle(
                    color = tint,
                    radius = w * 0.24f,
                    center = Offset(cx, groundY - w * 0.24f)
                )
                val flareOffsets = listOf(
                    Offset(cx - w * 0.32f, groundY - w * 0.52f),
                    Offset(cx + w * 0.32f, groundY - w * 0.52f),
                    Offset(cx, groundY - w * 0.60f)
                )
                for (pt in flareOffsets) {
                    drawCircle(color = tint, radius = 2.dp.toPx(), center = pt)
                }
            }

            OrbitPrayerStage.ZAWAL -> {
                // Solar Zenith: Midday sun right overhead with warning apex triangle
                drawCircle(color = tint, radius = w * 0.22f, center = Offset(cx, cy))
                // Apex marker
                val path = Path().apply {
                    moveTo(cx, cy - w * 0.38f)
                    lineTo(cx + 3.dp.toPx(), cy - w * 0.26f)
                    lineTo(cx - 3.dp.toPx(), cy - w * 0.26f)
                    close()
                }
                drawPath(path, color = tint)
            }

            OrbitPrayerStage.DHUHR -> {
                // High zenith sun disc with 8 clean geometric radiant rays
                val r = w * 0.22f
                drawCircle(color = tint, radius = r, center = Offset(cx, cy))
                val rayInner = r + 3.dp.toPx()
                val rayOuter = rayInner + 5.dp.toPx()
                for (i in 0 until 8) {
                    val rad = i * (PI / 4.0)
                    drawLine(
                        color = tint,
                        start = Offset(cx + (rayInner * cos(rad)).toFloat(), cy + (rayInner * sin(rad)).toFloat()),
                        end = Offset(cx + (rayOuter * cos(rad)).toFloat(), cy + (rayOuter * sin(rad)).toFloat()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            OrbitPrayerStage.ASR -> {
                // Afternoon sun with distinct angle shadow projection
                val sunCenter = Offset(cx - w * 0.18f, cy - h * 0.18f)
                drawCircle(color = tint, radius = w * 0.20f, center = sunCenter)

                // Ground plane
                val groundY = cy + h * 0.30f
                drawLine(
                    color = tint.copy(alpha = 0.5f),
                    start = Offset(w * 0.15f, groundY),
                    end = Offset(w * 0.88f, groundY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Angular ray beam
                drawLine(
                    color = tint,
                    start = Offset(sunCenter.x + w * 0.12f, sunCenter.y + h * 0.12f),
                    end = Offset(cx + w * 0.32f, groundY),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Shadow indicator dot
                drawCircle(color = tint, radius = 3.dp.toPx(), center = Offset(cx + w * 0.32f, groundY))
            }

            OrbitPrayerStage.SUNSET -> {
                // Dusk horizon with sun halfway setting into red horizon
                val duskY = cy + h * 0.12f
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - w * 0.22f, duskY - w * 0.22f),
                    size = Size(w * 0.44f, w * 0.44f)
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, duskY),
                    end = Offset(w * 0.85f, duskY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            OrbitPrayerStage.MAGHRIB -> {
                // Sun dipping beneath the dusk wave + evening twilight diamond star
                val duskWaveY = cy + h * 0.15f
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - w * 0.22f - w * 0.08f, duskWaveY - w * 0.22f),
                    size = Size(w * 0.44f, w * 0.44f)
                )
                drawLine(
                    color = tint.copy(alpha = 0.7f),
                    start = Offset(w * 0.12f, duskWaveY),
                    end = Offset(w * 0.88f, duskWaveY),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val starCenter = Offset(cx + w * 0.26f, cy - h * 0.25f)
                val starPath = Path().apply {
                    moveTo(starCenter.x, starCenter.y - 6.dp.toPx())
                    lineTo(starCenter.x + 4.dp.toPx(), starCenter.y)
                    lineTo(starCenter.x, starCenter.y + 6.dp.toPx())
                    lineTo(starCenter.x - 4.dp.toPx(), starCenter.y)
                    close()
                }
                drawPath(starPath, color = tint)
            }

            OrbitPrayerStage.ISHA -> {
                // Slender celestial crescent moon cradling two diamond stars
                val crescentPath = Path().apply {
                    val rOuter = w * 0.32f
                    addOval(androidx.compose.ui.geometry.Rect(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter))
                }
                val cutoutPath = Path().apply {
                    val rInner = w * 0.30f
                    addOval(androidx.compose.ui.geometry.Rect(cx - rInner + w * 0.16f, cy - rInner - h * 0.08f, cx + rInner + w * 0.16f, cy + rInner - h * 0.08f))
                }
                val moonPath = Path.combine(PathOperation.Difference, crescentPath, cutoutPath)
                drawPath(moonPath, color = tint)

                val starCenter = Offset(cx + w * 0.24f, cy + h * 0.15f)
                drawCircle(color = tint, radius = 2.2.dp.toPx(), center = starCenter)
            }
        }
    }
}

/**
 * Modern Hybrid Orbit & Timeline Hero Widget (Glassmorphic with Integrated Noor Garden and 3 Prohibited Times)
 */
@Composable
fun ModernPrayerOrbitWidget(
    viewModel: NoorUpViewModel,
    selectedCityLocation: CityLocation,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var viewMode by remember { mutableStateOf(0) } // 0: Curved Orbit Arc, 1: Vertical Timeline
    var showProhibitedDrawer by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()
    val curMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    val fajrS = viewModel.parseTimeToMins(selectedCityLocation.fajrStart)
    val fajrE = viewModel.parseTimeToMins(selectedCityLocation.fajrEnd)
    val sunriseS = viewModel.parseTimeToMins(selectedCityLocation.makruhSunriseStart)
    val sunriseE = viewModel.parseTimeToMins(selectedCityLocation.makruhSunriseEnd)
    val zawalS = viewModel.parseTimeToMins(selectedCityLocation.makruhZawalStart)
    val zawalE = viewModel.parseTimeToMins(selectedCityLocation.makruhZawalEnd)
    val dhuhrS = viewModel.parseTimeToMins(selectedCityLocation.dhuhrStart)
    val dhuhrE = viewModel.parseTimeToMins(selectedCityLocation.dhuhrEnd)
    val asrS = viewModel.parseTimeToMins(selectedCityLocation.asrStart)
    val asrE = viewModel.parseTimeToMins(selectedCityLocation.asrEnd)
    val sunsetS = viewModel.parseTimeToMins(selectedCityLocation.makruhSunsetStart)
    val sunsetE = viewModel.parseTimeToMins(selectedCityLocation.makruhSunsetEnd)
    val maghribS = viewModel.parseTimeToMins(selectedCityLocation.maghribStart)
    val maghribE = viewModel.parseTimeToMins(selectedCityLocation.maghribEnd)
    val ishaS = viewModel.parseTimeToMins(selectedCityLocation.ishaStart)
    val ishaE = 24 * 60 + viewModel.parseTimeToMins(selectedCityLocation.ishaEnd)

    // Accurate determination of active prayer stage / Makruh prohibited period
    val (activeStage, remainingMins, nextStage) = remember(curMins, selectedCityLocation) {
        when {
            curMins in fajrS..fajrE -> Triple(OrbitPrayerStage.FAJR, (fajrE - curMins + 1).coerceAtLeast(1), OrbitPrayerStage.SUNRISE)
            curMins in sunriseS..sunriseE -> Triple(OrbitPrayerStage.SUNRISE, (sunriseE - curMins + 1).coerceAtLeast(1), OrbitPrayerStage.DHUHR)
            curMins in (sunriseE + 1) until zawalS -> Triple(OrbitPrayerStage.SUNRISE, (zawalS - curMins).coerceAtLeast(1), OrbitPrayerStage.ZAWAL)
            curMins in zawalS..zawalE -> Triple(OrbitPrayerStage.ZAWAL, (zawalE - curMins + 1).coerceAtLeast(1), OrbitPrayerStage.DHUHR)
            curMins in dhuhrS..dhuhrE -> Triple(OrbitPrayerStage.DHUHR, (dhuhrE - curMins + 1).coerceAtLeast(1), OrbitPrayerStage.ASR)
            curMins in asrS until sunsetS -> Triple(OrbitPrayerStage.ASR, (sunsetS - curMins).coerceAtLeast(1), OrbitPrayerStage.SUNSET)
            curMins in sunsetS..sunsetE -> Triple(OrbitPrayerStage.SUNSET, (sunsetE - curMins + 1).coerceAtLeast(1), OrbitPrayerStage.MAGHRIB)
            curMins in maghribS..maghribE -> Triple(OrbitPrayerStage.MAGHRIB, (maghribE - curMins + 1).coerceAtLeast(1), OrbitPrayerStage.ISHA)
            curMins >= ishaS -> Triple(OrbitPrayerStage.ISHA, ((1440 - curMins) + fajrS).coerceAtLeast(1), OrbitPrayerStage.FAJR)
            else -> Triple(OrbitPrayerStage.ISHA, (fajrS - curMins).coerceAtLeast(1), OrbitPrayerStage.FAJR)
        }
    }

    val (activeStartTime, activeEndTime) = when (activeStage) {
        OrbitPrayerStage.FAJR -> Pair(selectedCityLocation.fajrStart, selectedCityLocation.fajrEnd)
        OrbitPrayerStage.SUNRISE -> Pair(selectedCityLocation.makruhSunriseStart, selectedCityLocation.makruhSunriseEnd)
        OrbitPrayerStage.ZAWAL -> Pair(selectedCityLocation.makruhZawalStart, selectedCityLocation.makruhZawalEnd)
        OrbitPrayerStage.DHUHR -> Pair(selectedCityLocation.dhuhrStart, selectedCityLocation.dhuhrEnd)
        OrbitPrayerStage.ASR -> Pair(selectedCityLocation.asrStart, selectedCityLocation.asrEnd)
        OrbitPrayerStage.SUNSET -> Pair(selectedCityLocation.makruhSunsetStart, selectedCityLocation.makruhSunsetEnd)
        OrbitPrayerStage.MAGHRIB -> Pair(selectedCityLocation.maghribStart, selectedCityLocation.maghribEnd)
        OrbitPrayerStage.ISHA -> Pair(selectedCityLocation.ishaStart, selectedCityLocation.ishaEnd)
    }

    val activeName = if (isEnglish) activeStage.nameEn else activeStage.nameBn
    val nextName = if (isEnglish) nextStage.nameEn else nextStage.nameBn
    val hrs = remainingMins / 60
    val mins = remainingMins % 60
    val countdownText = if (isEnglish) {
        if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    } else {
        if (hrs > 0) "${hrs.toBanglaDigits()} ঘ. ${mins.toBanglaDigits()} মি." else "${mins.toBanglaDigits()} মি."
    }

    // Noor Garden habit progress state
    val habitPrayers by viewModel.habitPrayers.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val completedCount = habitPrayers.values.count { it }
    val progressRatio = completedCount.toFloat() / 5f
    val percentInt = (completedCount * 100) / 5

    // Glowing animation for active prayer indicator
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Dynamic Theme Styling
    val cardBackground = if (isDarkMode) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F172A).copy(alpha = 0.95f),
                Color(0xFF090D16).copy(alpha = 0.98f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF0FDF4)
            )
        )
    }

    val cardBorder = if (isDarkMode) {
        BorderStroke(
            1.4.dp,
            Brush.sweepGradient(
                if (activeStage.isMakruh) {
                    listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.8f),
                        Color(0xFFEF4444).copy(alpha = 0.6f),
                        Color(0xFFF59E0B).copy(alpha = 0.8f)
                    )
                } else {
                    listOf(
                        Color(0xFF10B981).copy(alpha = 0.7f),
                        Color(0xFF06B6D4).copy(alpha = 0.3f),
                        Color(0xFFF59E0B).copy(alpha = 0.5f),
                        Color(0xFF10B981).copy(alpha = 0.7f)
                    )
                }
            )
        )
    } else {
        BorderStroke(
            1.2.dp,
            if (activeStage.isMakruh) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.35f)
        )
    }

    val primaryTextColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val surfaceButtonColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.8f) else Color(0xFFF1F5F9)
    val surfaceBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
    val dividerColor = if (isDarkMode) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)

    // Floating Glass Card Container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDarkMode) 16.dp else 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.06f),
                spotColor = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackground)
            .border(cardBorder, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- 1. Top Header Row: Status & View Mode Switcher ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val statusDotColor = if (activeStage.isMakruh) Color(0xFFF59E0B) else Color(0xFF10B981)
                    Box(
                        modifier = Modifier
                            .size(10.dp * pulseScale)
                            .clip(CircleShape)
                            .background(statusDotColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (activeStage.isMakruh) {
                                    if (isEnglish) "⚠️ PROHIBITED TIME" else "⚠️ নামাজের নিষিদ্ধ সময়"
                                } else {
                                    if (isEnglish) "LIVE PRAYER ORBIT" else "লাইভ নামাজের অরবিট"
                                },
                                color = if (activeStage.isMakruh) {
                                    if (isDarkMode) Color(0xFFFDE68A) else Color(0xFFD97706)
                                } else {
                                    if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "$activeName • ${if (isEnglish) "Ends in" else "বাকি"} $countdownText",
                            color = primaryTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (isEnglish) "Time window" else "ওয়াক্তের সময়কাল"}: $activeStartTime – $activeEndTime",
                            color = secondaryTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (activeStage.isMakruh) {
                            Text(
                                text = if (isEnglish) activeStage.makruhReasonEn else activeStage.makruhReasonBn,
                                color = if (isDarkMode) Color(0xFFFCD34D) else Color(0xFFB45309),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Sleek View Mode Switcher (Curved Arc vs. Vertical Timeline)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceButtonColor)
                        .border(1.dp, surfaceBorderColor, RoundedCornerShape(16.dp))
                        .padding(2.dp)
                ) {
                    // Arc Orbit Icon Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (viewMode == 0) Color(0xFF10B981) else Color.Transparent)
                            .clickable { viewMode = 0 }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Arc Orbit",
                            tint = if (viewMode == 0) Color.White else secondaryTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    // Vertical Timeline Icon Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (viewMode == 1) Color(0xFF10B981) else Color.Transparent)
                            .clickable { viewMode = 1 }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Vertical Timeline",
                            tint = if (viewMode == 1) Color.White else secondaryTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 2. Interactive Stage Visualizer (Curved Arc or Vertical Timeline) ---
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "visualizer_mode"
            ) { mode ->
                if (mode == 0) {
                    // Modern Curved Arc Solar Orbit
                    Column {
                        PrayerOrbitArcCanvas(
                            activeStage = activeStage,
                            isEnglish = isEnglish,
                            isDarkMode = isDarkMode,
                            selectedCityLocation = selectedCityLocation
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Integrated 3 Makruh Prohibited Times Strip
                        ProhibitedTimesStrip(
                            selectedCityLocation = selectedCityLocation,
                            curMins = curMins,
                            viewModel = viewModel,
                            isEnglish = isEnglish,
                            isDarkMode = isDarkMode,
                            onToggleExpand = { showProhibitedDrawer = !showProhibitedDrawer }
                        )
                    }
                } else {
                    // Complete Vertical Stage Timeline with 3 Prohibited Times Integrated
                    PrayerVerticalTimeline(
                        activeStage = activeStage,
                        isEnglish = isEnglish,
                        isDarkMode = isDarkMode,
                        selectedCityLocation = selectedCityLocation
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = dividerColor,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- 3. Integrated "Noor Garden" Visualization & Quick Habit Check Bar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circular Progress Mini Dial
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 3.5.dp.toPx()
                            // Track
                            drawCircle(
                                color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0),
                                radius = (size.minDimension - strokeWidth) / 2f,
                                style = Stroke(strokeWidth)
                            )
                            // Progress
                            drawArc(
                                color = Color(0xFF10B981),
                                startAngle = -90f,
                                sweepAngle = 360f * progressRatio,
                                useCenter = false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = if (isEnglish) "$percentInt%" else "${percentInt.toBanglaDigits()}%",
                            color = primaryTextColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isEnglish) "Noor Garden Today" else "আজকের নূর বাগান",
                                color = primaryTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentStreak > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                        .border(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "🔥 $currentStreak d" else "🔥 ${currentStreak.toBanglaDigits()} দিন",
                                        color = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFFD97706),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isEnglish) "$completedCount/5 Prayers Offered" else "${completedCount.toBanglaDigits()}/৫ ওয়াক্ত আদায় হয়েছে",
                            color = secondaryTextColor,
                            fontSize = 11.sp
                        )
                    }
                }

                // Next upcoming prayer quick indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceButtonColor)
                        .border(1.dp, surfaceBorderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEnglish) "Next: $nextName" else "পরবর্তী: $nextName",
                            color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Garden Interactive Check Blossoms (Fajr, Dhuhr, Asr, Maghrib, Isha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val mandatory = listOf(
                    OrbitPrayerStage.FAJR,
                    OrbitPrayerStage.DHUHR,
                    OrbitPrayerStage.ASR,
                    OrbitPrayerStage.MAGHRIB,
                    OrbitPrayerStage.ISHA
                )

                mandatory.forEach { stage ->
                    val isChecked = habitPrayers[stage.key] ?: false
                    val label = if (isEnglish) stage.nameEn else stage.nameBn

                    val blossomBg = when {
                        isChecked -> if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFECFDF5)
                        else -> if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9)
                    }
                    val blossomBorder = when {
                        isChecked -> Color(0xFF10B981)
                        else -> surfaceBorderColor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(blossomBg)
                            .border(1.dp, blossomBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleHabitPrayer(stage.key) }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isChecked) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8))
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                color = if (isChecked) Color(0xFF10B981) else secondaryTextColor,
                                fontSize = 10.5.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Prohibited / Makruh Times Summary Strip for Curved Orbit Arc View
 */
@Composable
private fun ProhibitedTimesStrip(
    selectedCityLocation: CityLocation,
    curMins: Int,
    viewModel: NoorUpViewModel,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    onToggleExpand: () -> Unit
) {
    val sunriseS = viewModel.parseTimeToMins(selectedCityLocation.makruhSunriseStart)
    val sunriseE = viewModel.parseTimeToMins(selectedCityLocation.makruhSunriseEnd)
    val zawalS = viewModel.parseTimeToMins(selectedCityLocation.makruhZawalStart)
    val zawalE = viewModel.parseTimeToMins(selectedCityLocation.makruhZawalEnd)
    val sunsetS = viewModel.parseTimeToMins(selectedCityLocation.makruhSunsetStart)
    val sunsetE = viewModel.parseTimeToMins(selectedCityLocation.makruhSunsetEnd)

    val isSunriseActive = curMins in sunriseS..sunriseE
    val isZawalActive = curMins in zawalS..zawalE
    val isSunsetActive = curMins in sunsetS..sunsetE

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isEnglish) "3 Prohibited Times Today" else "আজকের ৩টি নিষিদ্ধ সময়সূচী",
                    color = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFFB45309),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Sunrise Makruh
            ProhibitedItemChip(
                name = if (isEnglish) "Sunrise" else "সূর্যোদয়",
                time = "${selectedCityLocation.makruhSunriseStart.replace(" AM","")} - ${selectedCityLocation.makruhSunriseEnd.replace(" AM","")}",
                isActive = isSunriseActive,
                isPassed = curMins > sunriseE,
                isEnglish = isEnglish,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )

            // 2. Zawal Zenith Makruh
            ProhibitedItemChip(
                name = if (isEnglish) "Zawal" else "দ্বিপ্রহর",
                time = "${selectedCityLocation.makruhZawalStart.replace(" AM","")} - ${selectedCityLocation.makruhZawalEnd.replace(" PM","")}",
                isActive = isZawalActive,
                isPassed = curMins > zawalE,
                isEnglish = isEnglish,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )

            // 3. Sunset Makruh
            ProhibitedItemChip(
                name = if (isEnglish) "Sunset" else "সূর্যাস্ত",
                time = "${selectedCityLocation.makruhSunsetStart.replace(" PM","")} - ${selectedCityLocation.makruhSunsetEnd.replace(" PM","")}",
                isActive = isSunsetActive,
                isPassed = curMins > sunsetE,
                isEnglish = isEnglish,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProhibitedItemChip(
    name: String,
    time: String,
    isActive: Boolean,
    isPassed: Boolean,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val chipBg = when {
        isActive -> if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFFFEF3C7)
        isPassed -> if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.35f) else Color(0xFFF1F5F9).copy(alpha = 0.6f)
        else -> if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF8FAFC)
    }

    val chipBorder = when {
        isActive -> Color(0xFFF59E0B)
        else -> if (isDarkMode) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(chipBg)
            .border(if (isActive) 1.2.dp else 0.8.dp, chipBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 5.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = when {
                        isActive -> Color(0xFFF59E0B)
                        isPassed -> if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                        else -> if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155)
                    },
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                )
                if (isActive) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }
            }
            Text(
                text = time,
                color = when {
                    isActive -> if (isDarkMode) Color(0xFFFDE68A) else Color(0xFFB45309)
                    else -> if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                },
                fontSize = 8.5.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Curved Arc Solar Orbit Visualizer with Concentric Celestial Alignment & Centered Glow
 */
@Composable
private fun PrayerOrbitArcCanvas(
    activeStage: OrbitPrayerStage,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    selectedCityLocation: CityLocation
) {
    val stages = listOf(
        OrbitPrayerStage.FAJR,
        OrbitPrayerStage.SUNRISE,
        OrbitPrayerStage.DHUHR,
        OrbitPrayerStage.ASR,
        OrbitPrayerStage.MAGHRIB,
        OrbitPrayerStage.ISHA
    )

    val infiniteTransition = rememberInfiniteTransition(label = "celestial_orbit")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbit_pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbit_pulse_alpha"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbit_ring_alpha"
    )

    val hPadding = 6.dp
    val archHeight = 20.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Celestial Arc Track Canvas drawn cleanly behind the icon waypoints with matched padding
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = hPadding)
        ) {
            val w = size.width
            val n = stages.size
            val waypoints = (0 until n).map { i ->
                val xFrac = (i + 0.5f) / n
                val archFactor = sin((i.toFloat() / (n - 1).toFloat()) * PI.toFloat())
                val cx = w * xFrac
                val cy = 40.dp.toPx() - (archFactor * archHeight.toPx())
                Offset(cx, cy)
            }

            // Draw smooth connecting celestial spline path
            val path = Path().apply {
                if (waypoints.isNotEmpty()) {
                    moveTo(waypoints.first().x, waypoints.first().y)
                    for (i in 0 until waypoints.size - 1) {
                        val p0 = waypoints[i]
                        val p1 = waypoints[i + 1]
                        val controlX = (p0.x + p1.x) / 2f
                        val controlY = (p0.y + p1.y) / 2f
                        quadraticBezierTo(p0.x, p0.y, controlX, controlY)
                    }
                    lineTo(waypoints.last().x, waypoints.last().y)
                }
            }

            // Glow Background Arc
            val arcGradient = if (isDarkMode) {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFF43F5E).copy(alpha = 0.6f),
                        Color(0xFFFBBF24).copy(alpha = 0.7f),
                        Color(0xFFF59E0B).copy(alpha = 0.8f),
                        Color(0xFFEA580C).copy(alpha = 0.7f),
                        Color(0xFF8B5CF6).copy(alpha = 0.6f),
                        Color(0xFF38BDF8).copy(alpha = 0.7f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFE11D48).copy(alpha = 0.5f),
                        Color(0xFFD97706).copy(alpha = 0.6f),
                        Color(0xFFF59E0B).copy(alpha = 0.7f),
                        Color(0xFFEA580C).copy(alpha = 0.6f),
                        Color(0xFF7C3AED).copy(alpha = 0.5f),
                        Color(0xFF0284C7).copy(alpha = 0.6f)
                    )
                )
            }

            drawPath(
                path = path,
                brush = arcGradient,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 6 Stage Waypoints (Non-overlapping, arched naturally with clear text space and concentric glow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            val n = stages.size
            stages.forEachIndexed { i, stage ->
                val isActive = (stage == activeStage)
                val archFactor = sin((i.toFloat() / (n - 1).toFloat()) * PI.toFloat())
                val yLift = archHeight * archFactor
                val topPadding = (20.dp - yLift).coerceAtLeast(0.dp)

                val (startTime, endTime) = when (stage) {
                    OrbitPrayerStage.FAJR -> Pair(selectedCityLocation.fajrStart, selectedCityLocation.fajrEnd)
                    OrbitPrayerStage.SUNRISE -> Pair(selectedCityLocation.makruhSunriseStart, selectedCityLocation.makruhSunriseEnd)
                    OrbitPrayerStage.DHUHR -> Pair(selectedCityLocation.dhuhrStart, selectedCityLocation.dhuhrEnd)
                    OrbitPrayerStage.ASR -> Pair(selectedCityLocation.asrStart, selectedCityLocation.asrEnd)
                    OrbitPrayerStage.MAGHRIB -> Pair(selectedCityLocation.maghribStart, selectedCityLocation.maghribEnd)
                    OrbitPrayerStage.ISHA -> Pair(selectedCityLocation.ishaStart, selectedCityLocation.ishaEnd)
                    else -> Pair("", "")
                }

                val highlightColor = if (stage.isMakruh) Color(0xFFF59E0B) else Color(0xFF10B981)
                val badgeBg = when {
                    isActive -> if (isDarkMode) highlightColor.copy(alpha = 0.25f) else (if (stage.isMakruh) Color(0xFFFEF3C7) else Color(0xFFECFDF5))
                    else -> if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                }
                val badgeBorder = when {
                    isActive -> highlightColor
                    else -> if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = topPadding)
                ) {
                    // Node container with perfectly concentric pulsating glow
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            // Concentric pulsating blooming halo
                            Box(
                                modifier = Modifier
                                    .size(36.dp * pulseScale)
                                    .clip(CircleShape)
                                    .background(highlightColor.copy(alpha = pulseAlpha))
                            )
                            // Concentric accent glowing ring
                            Box(
                                modifier = Modifier
                                    .size(37.dp)
                                    .clip(CircleShape)
                                    .border(1.6.dp, highlightColor.copy(alpha = ringAlpha), CircleShape)
                            )
                        }

                        // Custom Minimalist Vector Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(badgeBg)
                                .border(if (isActive) 1.5.dp else 1.dp, badgeBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            PrayerStageVector(
                                stage = stage,
                                isActive = isActive,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isEnglish) stage.nameEn else stage.nameBn,
                        color = if (isActive) highlightColor else (if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B)),
                        fontSize = 10.5.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )

                    val cleanStart = startTime.replace(" AM", "").replace(" PM", "")
                    val cleanEnd = endTime.replace(" AM", "").replace(" PM", "")

                    Text(
                        text = cleanStart,
                        color = if (isActive) (if (isDarkMode) (if (stage.isMakruh) Color(0xFFFDE68A) else Color(0xFFA7F3D0)) else (if (stage.isMakruh) Color(0xFFB45309) else Color(0xFF047857))) else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        fontSize = 9.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                    if (cleanEnd.isNotEmpty()) {
                        Text(
                            text = "– $cleanEnd",
                            color = if (isActive) (if (isDarkMode) Color(0xFFFDE68A).copy(alpha = 0.8f) else Color(0xFF047857)) else (if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Complete Vertical Stage Timeline Visualizer (Includes 5 Prayers + 3 Prohibited Makruh Times)
 */
@Composable
private fun PrayerVerticalTimeline(
    activeStage: OrbitPrayerStage,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    selectedCityLocation: CityLocation
) {
    // Chronological order of 5 obligatory prayers + 3 prohibited times
    val stages = listOf(
        OrbitPrayerStage.FAJR,
        OrbitPrayerStage.SUNRISE,
        OrbitPrayerStage.ZAWAL,
        OrbitPrayerStage.DHUHR,
        OrbitPrayerStage.ASR,
        OrbitPrayerStage.SUNSET,
        OrbitPrayerStage.MAGHRIB,
        OrbitPrayerStage.ISHA
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        stages.forEach { stage ->
            val isActive = (stage == activeStage)
            val isMakruh = stage.isMakruh
            val (startTime, endTime) = when (stage) {
                OrbitPrayerStage.FAJR -> Pair(selectedCityLocation.fajrStart, selectedCityLocation.fajrEnd)
                OrbitPrayerStage.SUNRISE -> Pair(selectedCityLocation.makruhSunriseStart, selectedCityLocation.makruhSunriseEnd)
                OrbitPrayerStage.ZAWAL -> Pair(selectedCityLocation.makruhZawalStart, selectedCityLocation.makruhZawalEnd)
                OrbitPrayerStage.DHUHR -> Pair(selectedCityLocation.dhuhrStart, selectedCityLocation.dhuhrEnd)
                OrbitPrayerStage.ASR -> Pair(selectedCityLocation.asrStart, selectedCityLocation.asrEnd)
                OrbitPrayerStage.SUNSET -> Pair(selectedCityLocation.makruhSunsetStart, selectedCityLocation.makruhSunsetEnd)
                OrbitPrayerStage.MAGHRIB -> Pair(selectedCityLocation.maghribStart, selectedCityLocation.maghribEnd)
                OrbitPrayerStage.ISHA -> Pair(selectedCityLocation.ishaStart, selectedCityLocation.ishaEnd)
            }

            val itemBg = when {
                isActive && isMakruh -> if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.20f) else Color(0xFFFEF3C7)
                isActive -> if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFECFDF5)
                isMakruh -> if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.08f) else Color(0xFFFFFBEB)
                else -> if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.45f) else Color(0xFFF8FAFC)
            }

            val itemBorder = when {
                isActive && isMakruh -> Color(0xFFF59E0B)
                isActive -> Color(0xFF10B981)
                isMakruh -> if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.35f) else Color(0xFFFCD34D)
                else -> if (isDarkMode) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(itemBg)
                    .border(if (isActive) 1.3.dp else 1.dp, itemBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        // Custom Vector Graphic Icon
                        val iconContainerBg = when {
                            isActive && isMakruh -> if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFFFDE68A)
                            isActive -> if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFD1FAE5)
                            else -> if (isDarkMode) Color(0xFF0F172A) else Color(0xFFFFFFFF)
                        }
                        val iconBorder = when {
                            isActive && isMakruh -> Color(0xFFF59E0B)
                            isActive -> Color(0xFF10B981)
                            else -> if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(iconContainerBg)
                                .border(1.dp, iconBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            PrayerStageVector(
                                stage = stage,
                                isActive = isActive,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isEnglish) stage.nameEn else stage.nameBn,
                                    color = when {
                                        isActive && isMakruh -> Color(0xFFF59E0B)
                                        isActive -> Color(0xFF10B981)
                                        isMakruh -> if (isDarkMode) Color(0xFFFCD34D) else Color(0xFFB45309)
                                        else -> if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                                )
                                if (isMakruh) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Prohibited" else "নিষিদ্ধ",
                                            color = if (isDarkMode) Color(0xFFF59E0B) else Color(0xFFD97706),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isMakruh) {
                                    if (isEnglish) stage.makruhReasonEn else stage.makruhReasonBn
                                } else {
                                    stage.arabic
                                },
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    // Start - End Time interval
                    Text(
                        text = "$startTime - $endTime",
                        color = when {
                            isActive && isMakruh -> Color(0xFFF59E0B)
                            isActive -> Color(0xFF10B981)
                            else -> if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                        },
                        fontSize = 11.5.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
