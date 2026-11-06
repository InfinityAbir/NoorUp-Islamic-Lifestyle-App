package com.example.ui.noorup.journey

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.TasbihFeedback
import com.example.ui.noorup.toBanglaDigits
import com.example.ui.theme.GoldAccent
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val OnSky = Color(0xFFE2E8F0)
private val OnSkyMuted = Color(0xFFA5B4CC)
private val Emerald = Color(0xFF10B981)

private fun num(n: Int, isEnglish: Boolean) = if (isEnglish) n.toString() else n.toBanglaDigits()

fun statusLabel(status: PrayerStatus, isEnglish: Boolean): String = when (status) {
    PrayerStatus.COMPLETED -> if (isEnglish) "completed" else "আদায় হয়েছে"
    PrayerStatus.CONFIRMED_MISSED -> if (isEnglish) "marked missed" else "ছুটে গেছে চিহ্নিত"
    PrayerStatus.UNRECORDED -> if (isEnglish) "not recorded" else "রেকর্ড করা হয়নি"
    PrayerStatus.PENDING -> if (isEnglish) "pending" else "অপেক্ষমাণ"
    PrayerStatus.UPCOMING -> if (isEnglish) "upcoming" else "আসন্ন"
}

/**
 * Today's crescent: golden crescent + one star per prayer, driven only by real tracker records.
 * Tapping it (when [onOpen] is given) opens the full Crescent Journey screen.
 */
@Composable
fun CrescentJourneyHero(
    summary: JourneySummary,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    motion: Boolean = true,
    onOpen: (() -> Unit)? = null
) {
    val today = summary.today
    val illumination by animateFloatAsState(
        targetValue = today.illumination,
        animationSpec = tween(if (motion) 700 else 0),
        label = "illumination"
    )
    val twinkle = rememberTwinkle(motion)
    val stage = summary.stage.stage
    val percentLabel = today.percent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .celestialSky(stage, twinkle)
            .then(
                if (onOpen != null) Modifier
                    .clickable(onClickLabel = if (isEnglish) "Open Crescent Journey" else "চাঁদের যাত্রা খুলুন", role = Role.Button) { onOpen() }
                else Modifier
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEnglish) "Crescent Journey" else "চাঁদের যাত্রা",
                        color = OnSkyMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stage.label(isEnglish),
                        color = GoldAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isEnglish) "Stage ${stage.number} of 6" else "ধাপ ${num(stage.number, false)}/৬",
                        color = OnSkyMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${num(today.completed, isEnglish)}/${num(5, isEnglish)}",
                        color = OnSky,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        if (isEnglish) "prayers today" else "আজকের ওয়াক্ত",
                        color = OnSkyMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(132.dp)
                        .semantics {
                            contentDescription = if (isEnglish) {
                                "Crescent moon, $percentLabel percent illuminated"
                            } else {
                                "অর্ধচন্দ্র, ${num(percentLabel, false)} শতাংশ আলোকিত"
                            }
                        }
                ) { drawCrescent(illumination) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                val arc = listOf(14.dp, 4.dp, 0.dp, 4.dp, 14.dp)
                Prayer.entries.forEachIndexed { index, prayer ->
                    val status = today.statuses[index]
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = arc[index])
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${prayer.label(isEnglish)}, ${statusLabel(status, isEnglish)}"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PrayerStar(status, 30.dp, motion = motion)
                        Text(
                            prayer.label(isEnglish),
                            color = if (status == PrayerStatus.COMPLETED) GoldAccent else OnSkyMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEnglish) "Daily illumination" else "দৈনিক আলো",
                        color = OnSkyMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Text(
                        if (isEnglish) "$percentLabel%" else "${num(percentLabel, false)}%",
                        color = GoldAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        if (isEnglish) "30-day consistency" else "৩০ দিনের ধারাবাহিকতা",
                        color = OnSkyMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val rolling = summary.rolling30.percent
                    Text(
                        when {
                            rolling == null -> if (isEnglish) "Not enough data yet" else "এখনও পর্যাপ্ত তথ্য নেই"
                            isEnglish -> "$rolling%"
                            else -> "${num(rolling, false)}%"
                        },
                        color = if (rolling == null) OnSkyMuted else Emerald,
                        fontSize = if (rolling == null) 12.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onOpen != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isEnglish) "Open your journey  ›" else "আপনার যাত্রা দেখুন  ›",
                    color = GoldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Five small markers for a day in the list: filled check = completed, ring with cross = marked missed,
 * dashed ring with dash = not recorded. Order is Fajr -> Isha.
 */
@Composable
fun PrayerDayIndicators(
    statuses: List<PrayerStatus>,
    completedColor: Color,
    missedColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        statuses.forEach { status ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .drawBehind {
                        val stroke = 1.5.dp.toPx()
                        when (status) {
                            PrayerStatus.COMPLETED -> drawCircle(completedColor)
                            PrayerStatus.CONFIRMED_MISSED -> drawCircle(missedColor.copy(alpha = 0.7f), style = Stroke(stroke))
                            PrayerStatus.UNRECORDED -> drawCircle(
                                mutedColor.copy(alpha = 0.7f),
                                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
                            )
                            else -> drawCircle(mutedColor.copy(alpha = 0.5f), style = Stroke(stroke))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    PrayerStatus.COMPLETED -> Icon(Icons.Default.Check, null, tint = Color(0xFF04201A), modifier = Modifier.size(12.dp))
                    PrayerStatus.CONFIRMED_MISSED -> Icon(Icons.Default.Close, null, tint = missedColor, modifier = Modifier.size(12.dp))
                    PrayerStatus.UNRECORDED -> Icon(Icons.Default.Remove, null, tint = mutedColor, modifier = Modifier.size(12.dp))
                    else -> Unit
                }
            }
        }
    }
}
