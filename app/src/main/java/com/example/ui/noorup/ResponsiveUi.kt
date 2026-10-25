package com.example.ui.noorup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NoorUp responsive layout rules — single source of truth.
 *
 * Root-cause summary (cross-device clipping audit): most clipped/cramped UI came from
 * three repeated anti-patterns, NOT from device-specific issues:
 *  1. `Row { Column (no weight) + Button/Switch }` — the text column claims its full
 *     intrinsic width and squeezes the control down to a sliver, so short labels such
 *     as "ফোকাস" wrap character-by-character vertically. Fix: text column ALWAYS gets
 *     `Modifier.weight(1f)`; the control keeps its intrinsic size.
 *  2. Fixed `height(X.dp)` on containers that hold text — under larger font/display
 *     scaling the text outgrows the box and clips. Fix: `heightIn(min = X.dp)`.
 *  3. Fixed `width(X.dp)` wider than narrow viewports (e.g. 300dp carousel cards on a
 *     320dp phone with 32dp padding = 288dp available). Fix: cap by viewport fraction
 *     via [ResponsiveBreakpoints.carouselCardWidth].
 *  4. Paired half-width buttons with different wrapped heights (taller left button,
 *     stubby right button). Fix: parent `Row(Modifier.height(IntrinsicSize.Max))` with
 *     each child `fillMaxHeight()` — both follow the tallest sibling on any width.
 *
 * Rules for every screen:
 *  - Text columns in a Row take `weight(1f)`; icons/switches/buttons keep intrinsic size.
 *  - Action labels: single-line + [TextOverflow.Ellipsis] OR centered 2-line max.
 *  - Headings wrap naturally (never clip); metadata may ellipsize after 2 lines.
 *  - Touch targets: pills >= 40dp height, paired buttons >= 52-56dp min height, and
 *    paired rows use IntrinsicSize.Max so siblings stay exactly equal height.
 *  - App-widget lists: prefer one wrapping line over squeezed side-by-side columns.
 *  - No device-model checks, no arbitrary scale multipliers. sp/dp only.
 */
object ResponsiveRules {
    /** Minimum height for selectable pills/chips (touch target + wrap room). */
    val pillMinHeight: Dp = 40.dp

    /** Minimum height for half-width paired action buttons (equal heights when wrapping). */
    val pairedButtonMinHeight: Dp = 56.dp

    /** Minimum height for Hijri/Zakat fixed cells (grows under font scaling). */
    val fixedCellMinHeight: Dp = 52.dp

    /** Screen edge padding used by all top-level LazyColumns. */
    val screenPadding: Dp = 16.dp
}

/**
 * Pure, device-independent breakpoint math. Fully unit-testable on the JVM.
 */
object ResponsiveBreakpoints {
    /**
     * Adaptive card width for horizontally-scrolling carousels.
     *
     * @param viewportWidthDp available width in dp (e.g. from BoxWithConstraints).
     * @param preferredWidthDp design width used on standard phones.
     * @param peekFraction fraction of the viewport the card may occupy at most, so a
     * peek of the next card remains visible on narrow screens.
     * @return width in dp, never below [minWidthDp], never above [preferredWidthDp].
     */
    fun carouselCardWidth(
        viewportWidthDp: Float,
        preferredWidthDp: Float,
        peekFraction: Float = 0.85f,
        minWidthDp: Float = 120f
    ): Float {
        if (viewportWidthDp <= 0f || preferredWidthDp <= 0f) return preferredWidthDp.coerceAtLeast(minWidthDp)
        return minOf(preferredWidthDp, viewportWidthDp * peekFraction).coerceAtLeast(minWidthDp)
    }
}

/**
 * Title + subtitle block for header rows. Must be placed with [Modifier.weight] (1f)
 * so neighboring controls are never squeezed.
 */
@Composable
fun RowScope.AdaptiveHeaderTexts(
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    titleSize: TextUnit = 22.sp,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    subtitleSize: TextUnit = 14.sp
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            color = titleColor,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = subtitleColor,
            fontSize = subtitleSize
        )
    }
}

/** Single-line action label that ellipsizes instead of wrapping vertically. */
@Composable
fun RowScope.SingleLineActionLabel(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false)
    )
}

/** Centered two-line action label for half-width buttons (equal heights via min-height). */
@Composable
fun RowScope.TwoLineCenterActionLabel(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f, fill = false)
    )
}

/** Enforces the paired-button minimum height rule. */
fun Modifier.pairedButtonHeight(): Modifier = this.heightIn(min = ResponsiveRules.pairedButtonMinHeight)

/** Enforces the pill minimum height rule. */
fun Modifier.pillHeight(): Modifier = this.heightIn(min = ResponsiveRules.pillMinHeight)

/** Relaxes a fixed cell height so content can grow under font scaling. */
fun Modifier.adaptiveCellHeight(): Modifier = this.heightIn(min = ResponsiveRules.fixedCellMinHeight)

/** Relaxes a fixed minimum width (e.g. counters) so larger values are never clipped. */
fun Modifier.adaptiveMinWidth(min: Dp): Modifier = this.widthIn(min = min)
