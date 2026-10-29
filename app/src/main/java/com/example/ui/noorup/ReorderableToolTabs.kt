package com.example.ui.noorup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.EmeraldGlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Tools sub-tab chips. Tap selects. Press and hold a chip, then drag it left or right to reorder; the row
 * scrolls itself when a chip is held near an edge. The new order is reported through [onOrderChange].
 */
@Composable
fun ReorderableToolTabs(
    order: List<Int>,
    selectedId: Int,
    isEnglish: Boolean,
    label: (Int) -> String,
    icon: (Int) -> ImageVector,
    onSelect: (Int) -> Unit,
    onOrderChange: (List<Int>) -> Unit,
    onReset: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val spacingPx = with(density) { 8.dp.toPx() }
    val edgePx = with(density) { 56.dp.toPx() }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val currentOrder by rememberUpdatedState(order)
    val currentOnChange by rememberUpdatedState(onOrderChange)
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // Keep the selected chip in view when the selection changes (not while dragging).
    // Only scrolls when the chip is off screen, so opening the screen shows the row from its start.
    LaunchedEffect(selectedId) {
        val index = order.indexOf(selectedId)
        if (index < 0 || draggingId != null) return@LaunchedEffect
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo.firstOrNull { it.index == index }
        val fullyVisible = visible != null &&
            visible.offset >= info.viewportStartOffset &&
            visible.offset + visible.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem(index)
    }

    // Auto-scroll while a chip is held near either edge of the row.
    LaunchedEffect(draggingId) {
        val id = draggingId ?: return@LaunchedEffect
        while (draggingId == id) {
            val info = listState.layoutInfo
            val me = info.visibleItemsInfo.firstOrNull { it.key == id }
            if (me != null) {
                val center = me.offset + me.size / 2f + dragOffset
                val viewportEnd = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                val step = when {
                    center > viewportEnd - edgePx && listState.canScrollForward -> 14f
                    center < edgePx && listState.canScrollBackward -> -14f
                    else -> 0f
                }
                if (step != 0f) {
                    val consumed = listState.scrollBy(step)
                    // Content moved under the finger; keep the chip under it.
                    dragOffset += consumed
                }
            }
            delay(16)
        }
    }

    Column {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(order, key = { it }) { id ->
                val isSelected = selectedId == id
                val dragging = draggingId == id
                Surface(
                    modifier = Modifier
                        .then(if (dragging) Modifier else Modifier.animateItem())
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationX = if (dragging) dragOffset else 0f
                            val s = if (dragging) 1.06f else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .then(if (dragging) Modifier.shadow(10.dp, RoundedCornerShape(20.dp)) else Modifier)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = id
                                    dragOffset = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.x
                                    val items = listState.layoutInfo.visibleItemsInfo
                                    val me = items.firstOrNull { it.key == id } ?: return@detectDragGesturesAfterLongPress
                                    val center = me.offset + me.size / 2f + dragOffset
                                    val target = items.firstOrNull {
                                        it.key != id && center > it.offset && center < it.offset + it.size
                                    } ?: return@detectDragGesturesAfterLongPress
                                    val list = currentOrder
                                    val from = list.indexOf(id)
                                    val to = list.indexOf(target.key as Int)
                                    if (from >= 0 && to >= 0 && from != to) {
                                        currentOnChange(ToolTabOrder.move(list, from, to))
                                        // The chip's slot moved by the neighbour's width; offset so it stays under the finger.
                                        dragOffset += if (to > from) -(target.size + spacingPx) else (target.size + spacingPx)
                                    }
                                },
                                onDragEnd = { draggingId = null; dragOffset = 0f },
                                onDragCancel = { draggingId = null; dragOffset = 0f }
                            )
                        }
                        .clickable { onSelect(id) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        if (isDark) EmeraldGlow else Color(0xFF047857)
                    } else {
                        if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            dragging -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                            isSelected -> if (isDark) EmeraldGlow else Color(0xFF047857)
                            else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            icon(id),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Text(
                            label(id),
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isEnglish) "Tip: press and hold a tab, then drag to reorder" else "টিপ: ট্যাব চেপে ধরে টেনে সাজান",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                modifier = Modifier.weight(1f)
            )
            if (order != ToolTabOrder.DEFAULT) {
                TextButton(onClick = { onReset(); scope.launch { listState.scrollToItem(0) } }) {
                    Text(if (isEnglish) "Reset order" else "ক্রম রিসেট", fontSize = 11.sp)
                }
            }
        }
    }
}
