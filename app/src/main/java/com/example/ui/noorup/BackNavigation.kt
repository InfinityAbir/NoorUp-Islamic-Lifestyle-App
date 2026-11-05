package com.example.ui.noorup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState

/**
 * Bottom-navigation history. Holds the tabs the user came from, oldest first; the
 * start destination ([HOME]) is the implicit bottom of the stack.
 *
 * Re-selecting a tab that is already in the history pops back to it instead of
 * pushing a duplicate, so Home -> Quran -> Home never builds a loop and Back from
 * the final Home exits.
 */
class TabHistory(private val homeTab: Int = HOME) {
    private val stack = ArrayDeque<Int>()

    val isEmpty: Boolean get() = stack.isEmpty()

    /** Record a switch from [from] to [to]. */
    fun onSelect(from: Int, to: Int) {
        if (from == to) return
        val existing = stack.lastIndexOf(to)
        if (existing >= 0) {
            while (stack.size > existing) stack.removeLast()
        } else {
            stack.remove(from)
            stack.addLast(from)
        }
    }

    /** Tab to return to from [current], or null when [current] is the root and the app may exit. */
    fun pop(current: Int): Int? {
        val previous = stack.removeLastOrNull()
        return when {
            previous != null && previous != current -> previous
            current != homeTab -> homeTab
            else -> null
        }
    }

    fun clear() = stack.clear()

    companion object {
        const val HOME = 0
    }
}

/**
 * Registry of in-screen destinations (an open reader, a tool inside Explore, the
 * search overlay...) that Back must close before the tab history is consulted.
 *
 * Screens register through [NestedBackHandler]; the activity owns the single real
 * `BackHandler` and asks this class what to do, so handlers never compete.
 */
@Stable
class BackNavigator {
    private class Entry(val priority: Int, val seq: Long, val onBack: () -> Unit)

    private val entries = mutableStateListOf<Entry>()
    private var nextSeq = 0L

    /** Observable: true while any screen has a destination Back should close first. */
    val hasNested: Boolean get() = entries.isNotEmpty()

    internal fun register(priority: Int, onBack: () -> Unit): () -> Unit {
        val entry = Entry(priority, nextSeq++, onBack)
        entries.add(entry)
        return { entries.remove(entry) }
    }

    /** Run the top-most nested handler (highest priority, then most recently registered). */
    fun handleNested(): Boolean {
        val top = entries.maxWithOrNull(compareBy<Entry>({ it.priority }, { it.seq })) ?: return false
        top.onBack()
        return true
    }

    companion object {
        /** In-screen detail (reader, tool inside Explore, filtered list). */
        const val PRIORITY_SCREEN = 10
        /** Full-screen overlay above the tabs (global search). */
        const val PRIORITY_OVERLAY = 100
    }
}

/**
 * Declares that, while [enabled], Back should run [onBack] instead of leaving the
 * current screen. Safe to call from any screen; it only registers with [navigator].
 */
@Composable
fun NestedBackHandler(
    navigator: BackNavigator,
    enabled: Boolean,
    priority: Int = BackNavigator.PRIORITY_SCREEN,
    onBack: () -> Unit
) {
    val current by rememberUpdatedState(onBack)
    DisposableEffect(navigator, enabled, priority) {
        val unregister = if (enabled) navigator.register(priority) { current() } else null
        onDispose { unregister?.invoke() }
    }
}
