package com.example.ui.noorup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Maths for the Surah / Para side index; kept free of Compose so it can be unit tested. */
object QuickIndex {
    /** 1-based entry under a touch at [fraction] (0..1) of the rail height, for a list of [count] entries. */
    fun entryAt(fraction: Float, count: Int): Int {
        if (count <= 1) return 1
        val f = fraction.coerceIn(0f, 1f)
        return Math.round(f * (count - 1)) + 1
    }

    /** Where entry [entry] (1-based) sits on the rail as a fraction of its height. */
    fun fractionOf(entry: Int, count: Int): Float =
        if (count <= 1) 0f else ((entry - 1).toFloat() / (count - 1)).coerceIn(0f, 1f)

    /** Labels shown on the rail: first, last and evenly spread entries in between. */
    fun markers(count: Int, step: Int): List<Int> =
        (listOf(1) + (step..count step step) + listOf(count)).distinct().filter { it in 1..count }
}

/** One Surah row: number badge, names, Arabic title and a favourite heart that never triggers the row's open action. */
@Composable
fun SurahListCard(
    surah: Surah,
    isEnglish: Boolean,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    GlassCard(modifier = Modifier.clickable { onOpen() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = surahFavoriteLabel(surah, isEnglish, isFavorite),
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun surahFavoriteLabel(surah: Surah, isEnglish: Boolean, isFavorite: Boolean): String =
    if (isEnglish) {
        if (isFavorite) "Remove ${surah.nameEnglish} from favorites" else "Add ${surah.nameEnglish} to favorites"
    } else {
        if (isFavorite) "${surah.nameBangla} প্রিয় থেকে সরান" else "${surah.nameBangla} প্রিয়তে যোগ করুন"
    }

/**
 * Compact side index for fast jumping. Tap or drag along it to pick an entry (1..[count]);
 * [onSelect] fires for every new entry and [label] names it in the floating bubble.
 */
@Composable
fun QuickIndexRail(
    count: Int,
    markerStep: Int,
    description: String,
    markerText: (Int) -> String,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf<Int?>(null) }
    var heightPx by remember { mutableStateOf(1f) }
    val markers = remember(count, markerStep) { QuickIndex.markers(count, markerStep) }

    fun pick(y: Float) {
        val entry = QuickIndex.entryAt(y / heightPx, count)
        if (entry != active) {
            active = entry
            onSelect(entry)
        }
    }

    Box(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(30.dp)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                .semantics { contentDescription = description }
                .pointerInput(count) {
                    detectTapGestures(onPress = { offset ->
                        pick(offset.y)
                        // false = the touch turned into a drag, which clears the bubble itself
                        if (tryAwaitRelease()) active = null
                    })
                }
                .pointerInput(count) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> pick(offset.y) },
                        onDragEnd = { active = null },
                        onDragCancel = { active = null },
                        onVerticalDrag = { change, _ -> pick(change.position.y) }
                    )
                }
        ) {
            val railHeight = maxHeight
            markers.forEach { m ->
                Text(
                    text = markerText(m),
                    color = if (active == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = railHeight * QuickIndex.fractionOf(m, count) - 6.dp)
                )
            }
        }

        active?.let { entry ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    label(entry),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
