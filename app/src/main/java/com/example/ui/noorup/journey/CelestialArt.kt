package com.example.ui.noorup.journey

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldAccent
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Vector-only celestial art. Nothing here loads an asset or runs a continuous animation by
 * itself: callers decide whether motion is allowed.
 */

/** Look of one long-term stage. Pure data so it can be tested and reused for previews. */
data class StageTheme(
    val skyTop: Color,
    val skyBottom: Color,
    val starCount: Int,
    val cloudAlpha: Float,
    val horizonGlow: Color?,
    val constellationLines: Boolean,
    val harmonyRings: Boolean,
    val aurora: Boolean,
    val rays: Boolean
)

fun stageTheme(stage: Stage): StageTheme = when (stage) {
    Stage.FIRST_LIGHT -> StageTheme(Color(0xFF0E1A3D), Color(0xFF060B1C), 18, 0f, null, false, false, false, false)
    Stage.RISING_CRESCENT -> StageTheme(Color(0xFF11224D), Color(0xFF070D22), 26, 0f, Color(0xFFB45309), false, false, false, false)
    Stage.SHINING_CONSTELLATION -> StageTheme(Color(0xFF122556), Color(0xFF070E26), 38, 0f, Color(0xFFB45309), true, false, false, false)
    Stage.RADIANT_HORIZON -> StageTheme(Color(0xFF14295C), Color(0xFF0A1330), 34, 0.07f, Color(0xFFF59E0B), true, false, false, false)
    Stage.CELESTIAL_HARMONY -> StageTheme(Color(0xFF0F2C57), Color(0xFF08142E), 40, 0.07f, Color(0xFF10B981), true, true, true, false)
    Stage.ETERNAL_LIGHT -> StageTheme(Color(0xFF16275E), Color(0xFF0A1233), 52, 0.06f, Color(0xFFFBBF24), true, true, true, true)
}

private class SkyStar(val x: Float, val y: Float, val radius: Float, val alpha: Float, val phase: Float)

private val skyStars: List<SkyStar> by lazy {
    val random = java.util.Random(20251106L) // fixed seed: the sky never reshuffles between frames or launches
    List(60) {
        SkyStar(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radius = 0.5f + random.nextFloat() * 0.9f,
            alpha = 0.25f + random.nextFloat() * 0.45f,
            phase = random.nextFloat()
        )
    }
}

/**
 * Night sky for [stage]. [phase] is a 0..1 twinkle phase, read lazily so it only invalidates drawing.
 * Pass `null` for a still sky (reduced motion).
 */
fun Modifier.celestialSky(stage: Stage, phase: State<Float>?): Modifier = drawBehind {
    val theme = stageTheme(stage)
    drawRect(Brush.verticalGradient(listOf(theme.skyTop, theme.skyBottom)))
    val w = size.width
    val h = size.height

    theme.horizonGlow?.let { glow ->
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, glow.copy(alpha = 0.22f)),
                startY = h * 0.55f,
                endY = h
            )
        )
    }
    if (theme.aurora) {
        val band = Path().apply {
            moveTo(0f, h * 0.30f)
            cubicTo(w * 0.25f, h * 0.18f, w * 0.55f, h * 0.42f, w, h * 0.26f)
            lineTo(w, h * 0.36f)
            cubicTo(w * 0.55f, h * 0.52f, w * 0.25f, h * 0.28f, 0f, h * 0.42f)
            close()
        }
        drawPath(band, Color(0xFF10B981).copy(alpha = 0.10f))
    }
    if (theme.cloudAlpha > 0f) {
        listOf(Offset(0.12f, 0.50f) to 0.18f, Offset(0.88f, 0.34f) to 0.16f, Offset(0.84f, 0.56f) to 0.14f).forEach { (c, r) ->
            val cx = w * c.x
            val cy = h * c.y
            val base = min(w, h) * r
            drawOval(Color.White.copy(alpha = theme.cloudAlpha), Offset(cx - base, cy - base * 0.28f), androidx.compose.ui.geometry.Size(base * 2f, base * 0.56f))
            drawOval(Color.White.copy(alpha = theme.cloudAlpha), Offset(cx - base * 0.5f, cy - base * 0.5f), androidx.compose.ui.geometry.Size(base * 1.1f, base * 0.6f))
        }
    }

    val count = theme.starCount.coerceAtMost(skyStars.size)
    val t = phase?.value ?: 0.5f
    for (i in 0 until count) {
        val s = skyStars[i]
        val wave = if (phase == null) 0.6f else 0.35f + 0.65f * ((t + s.phase) % 1f)
        drawCircle(
            Color.White.copy(alpha = s.alpha * wave),
            s.radius.dp.toPx(),
            Offset(w * s.x, h * s.y)
        )
    }
    if (theme.constellationLines) {
        val line = Color.White.copy(alpha = 0.10f)
        for (i in 0 until min(count, 9) - 1) {
            val a = skyStars[i]
            val b = skyStars[i + 1]
            drawLine(line, Offset(w * a.x, h * a.y), Offset(w * b.x, h * b.y), 1.dp.toPx())
        }
    }
    val moon = Offset(w / 2f, h * 0.42f)
    if (theme.harmonyRings) {
        listOf(0.30f, 0.42f).forEach { f ->
            drawCircle(Color(0xFF10B981).copy(alpha = 0.10f), min(w, h) * f, moon, style = Stroke(1.dp.toPx()))
        }
    }
    if (theme.rays) {
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0)
            val inner = min(w, h) * 0.22f
            val outer = min(w, h) * 0.52f
            drawLine(
                GoldAccent.copy(alpha = 0.10f),
                Offset(moon.x + inner * cos(angle).toFloat(), moon.y + inner * sin(angle).toFloat()),
                Offset(moon.x + outer * cos(angle).toFloat(), moon.y + outer * sin(angle).toFloat()),
                1.5.dp.toPx()
            )
        }
    }
}

/** Shared twinkle phase for one sky. Returns null when [motion] is off so nothing animates. */
@Composable
fun rememberTwinkle(motion: Boolean): State<Float>? {
    if (!motion) return null
    val transition = rememberInfiniteTransition(label = "twinkle")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkleValue"
    )
}

/** Golden crescent: a disc minus an offset disc, glow grows with [illumination] (0..1). */
fun DrawScope.drawCrescent(illumination: Float, center: Offset? = null, radius: Float? = null) {
    val c = center ?: Offset(size.width / 2f, size.height / 2f)
    val r = radius ?: (min(size.width, size.height) * 0.36f)
    val glow = 0.10f + 0.40f * illumination
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoldAccent.copy(alpha = glow), Color.Transparent),
            center = c,
            radius = r * 2.1f
        ),
        radius = r * 2.1f,
        center = c
    )
    val disc = Path().apply { addOval(Rect(c, r)) }
    val bite = Path().apply { addOval(Rect(Offset(c.x + r * 0.46f, c.y - r * 0.12f), r * 0.86f)) }
    val crescent = Path.combine(PathOperation.Difference, disc, bite)
    // Unlit body stays faintly visible so a 0% day still reads as a crescent, not an empty sky.
    drawCircle(color = Color.White.copy(alpha = 0.05f), radius = r, center = c)
    drawPath(crescent, GoldAccent.copy(alpha = 0.22f + 0.78f * illumination))
}

fun starPath(center: Offset, outer: Float, inner: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians(-90.0 + i * 36.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * One prayer star. Shape / fill differ per status, so state never relies on colour alone.
 * With [motion] a prayer turning COMPLETED fades gold in, and a completed star breathes softly.
 */
@Composable
fun PrayerStar(
    status: PrayerStatus,
    size: Dp,
    modifier: Modifier = Modifier,
    motion: Boolean = false,
    neutral: Color = Color.White
) {
    val lit by animateFloatAsState(
        targetValue = if (status == PrayerStatus.COMPLETED) 1f else 0f,
        animationSpec = tween(if (motion) 600 else 0),
        label = "starLit"
    )
    val pulse: State<Float> = if (motion && status == PrayerStatus.COMPLETED) {
        rememberInfiniteTransition(label = "starPulse").animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
            label = "starPulseValue"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    Canvas(modifier = modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val outer = min(this.size.width, this.size.height) / 2f * 0.92f
        val path = starPath(c, outer, outer * 0.46f)
        val base = if (status == PrayerStatus.COMPLETED) PrayerStatus.PENDING else status
        val baseFade = 1f - lit
        if (baseFade > 0.01f) {
            when (base) {
                PrayerStatus.PENDING -> drawPath(path, neutral.copy(alpha = 0.55f * baseFade))
                PrayerStatus.UPCOMING -> drawPath(path, neutral.copy(alpha = 0.45f * baseFade), style = Stroke(width = 1.5.dp.toPx()))
                PrayerStatus.CONFIRMED_MISSED -> drawPath(path, Color(0xFF64748B).copy(alpha = 0.55f * baseFade))
                PrayerStatus.UNRECORDED -> drawPath(
                    path,
                    neutral.copy(alpha = 0.5f * baseFade),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                    )
                )
                PrayerStatus.COMPLETED -> Unit
            }
        }
        if (lit > 0.01f) {
            val glowRadius = outer * 1.5f * pulse.value
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(GoldAccent.copy(alpha = 0.45f * lit), Color.Transparent),
                    center = c,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = c
            )
            drawPath(path, GoldAccent.copy(alpha = lit))
        }
    }
}

// ------------------------------------------------------------------ constellations

private class Constellation(val points: List<Offset>, val edges: List<Pair<Int, Int>>)

private fun ring(n: Int): List<Offset> = List(n) {
    val a = Math.toRadians(-90.0 + it * 360.0 / n)
    Offset(0.5f + 0.38f * cos(a).toFloat(), 0.5f + 0.38f * sin(a).toFloat())
}

private fun constellation(id: MilestoneId): Constellation = when (id) {
    MilestoneId.FIRST_DAY -> Constellation(
        listOf(Offset(.5f, .5f), Offset(.5f, .2f), Offset(.5f, .8f), Offset(.2f, .5f), Offset(.8f, .5f)),
        listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)
    )
    MilestoneId.FIRST_WEEK -> Constellation(
        listOf(Offset(.25f, .55f), Offset(.38f, .4f), Offset(.5f, .3f), Offset(.62f, .38f), Offset(.72f, .5f), Offset(.58f, .62f), Offset(.42f, .68f)),
        listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 0)
    )
    MilestoneId.DAYS_30 -> Constellation(
        listOf(Offset(.2f, .75f), Offset(.25f, .5f), Offset(.4f, .3f), Offset(.6f, .22f), Offset(.78f, .3f), Offset(.86f, .48f)),
        listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5)
    )
    MilestoneId.DAYS_90 -> Constellation(
        listOf(Offset(.5f, .12f), Offset(.35f, .3f), Offset(.65f, .3f), Offset(.3f, .55f), Offset(.7f, .55f), Offset(.4f, .85f), Offset(.6f, .85f)),
        listOf(0 to 1, 0 to 2, 1 to 3, 2 to 4, 3 to 5, 4 to 6, 5 to 6, 1 to 2, 3 to 4)
    )
    MilestoneId.DAYS_180 -> Constellation(
        listOf(Offset(.15f, .8f), Offset(.15f, .45f), Offset(.32f, .62f), Offset(.5f, .2f), Offset(.68f, .62f), Offset(.85f, .45f), Offset(.85f, .8f)),
        listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 0 to 6)
    )
    MilestoneId.DAYS_365 -> {
        val pts = ring(8)
        Constellation(pts, (0 until 8).map { it to (it + 1) % 8 } + listOf(0 to 4, 2 to 6))
    }
}

/** A milestone's constellation: gold when unlocked, a faint dashed silhouette while locked. */
@Composable
fun ConstellationCanvas(id: MilestoneId, unlocked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val data = remember(id) { constellation(id) }
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val line = if (unlocked) GoldAccent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.18f)
        val effect = if (unlocked) null else PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        data.edges.forEach { (a, b) ->
            drawLine(
                line,
                Offset(w * data.points[a].x, h * data.points[a].y),
                Offset(w * data.points[b].x, h * data.points[b].y),
                1.5.dp.toPx(),
                pathEffect = effect
            )
        }
        data.points.forEachIndexed { index, p ->
            val c = Offset(w * p.x, h * p.y)
            val r = (if (index == 0) 4.5.dp else 3.dp).toPx()
            if (unlocked) {
                drawCircle(
                    Brush.radialGradient(listOf(GoldAccent.copy(alpha = 0.40f), Color.Transparent), center = c, radius = r * 3f),
                    r * 3f,
                    c
                )
                drawCircle(GoldAccent, r, c)
            } else {
                drawCircle(Color.White.copy(alpha = 0.25f), r * 0.8f, c)
            }
        }
    }
}
