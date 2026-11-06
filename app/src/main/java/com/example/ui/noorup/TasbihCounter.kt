package com.example.ui.noorup

import android.content.SharedPreferences
import java.time.LocalDate

/** Persisted position inside a tasbih round. */
data class TasbihState(
    val active: TasbihDhikr,
    val stageIndex: Int,
    val count: Int,
    val goal: Int,
    val completed: Boolean
)

/**
 * Result of one tap. [counted] is true when the tap added one to the dhikr tally
 * (it is false for a tap that only closes a round or is ignored after completion).
 */
data class TasbihTap(
    val state: TasbihState,
    val counted: Boolean,
    val roundCompleted: Boolean
)

/**
 * The tasbih counting rules, shared by the in-app counter and the home-screen widget so
 * both always agree. State lives in the same `noorup_prefs` keys the app already used.
 */
object TasbihCounter {

    private const val MAX_GOAL = TasbihRepository.MAX_TARGET

    /** Serialises widget taps, in-app taps and history writes so read-modify-write never loses a count. */
    val lock = Any()

    fun load(prefs: SharedPreferences, custom: List<TasbihDhikr>): TasbihState {
        val savedActiveId = prefs.getString("tasbih_active_id", null)
        val savedActive = savedActiveId?.let { TasbihRepository.findById(it, custom) }
        val legacyText = prefs.getString("tasbih_dhikr", null)
        val active = savedActive
            ?: legacyText?.let { text ->
                TasbihRepository.findBuiltInByLegacyText(text)
                    ?: TasbihRepository.legacyDefinition(
                        text,
                        prefs.getInt("tasbih_goal", TasbihRepository.builtIns.first().stages.first().target)
                    )
            }
            ?: TasbihRepository.builtIns.first()

        val stageIndex = prefs.getInt("tasbih_stage", 0).coerceIn(0, active.stages.lastIndex)
        val savedGoal = prefs.getInt("tasbih_goal", active.stages[stageIndex].target).coerceIn(1, MAX_GOAL)
        val savedCount = prefs.getInt("tasbih_count", 0).coerceIn(0, savedGoal)
        val completed = prefs.getBoolean("tasbih_completed", false) ||
            (savedCount >= savedGoal && stageIndex == active.stages.lastIndex)
        return TasbihState(active, stageIndex, savedCount, savedGoal, completed)
    }

    /** Exactly one increment per call. Taps after completion are ignored. */
    fun increment(state: TasbihState): TasbihTap {
        if (state.completed) return TasbihTap(state, counted = false, roundCompleted = false)
        val active = state.active
        val last = active.stages.lastIndex
        val stageIndex = state.stageIndex.coerceIn(0, last)
        val goal = state.goal.coerceIn(1, MAX_GOAL)

        if (state.count >= goal) {
            return if (stageIndex == last) {
                TasbihTap(complete(state.copy(goal = goal)), counted = false, roundCompleted = true)
            } else {
                TasbihTap(advance(state, stageIndex), counted = false, roundCompleted = false)
            }
        }

        val next = state.count + 1
        if (next < goal) {
            return TasbihTap(state.copy(count = next, goal = goal), counted = true, roundCompleted = false)
        }
        return if (stageIndex < last) {
            TasbihTap(advance(state, stageIndex), counted = true, roundCompleted = false)
        } else {
            TasbihTap(complete(state.copy(count = next, goal = goal)), counted = true, roundCompleted = true)
        }
    }

    fun reset(state: TasbihState): TasbihState {
        val goal = if (state.stageIndex != 0) {
            state.active.stages.first().target.coerceIn(1, MAX_GOAL)
        } else state.goal
        return state.copy(stageIndex = 0, count = 0, goal = goal, completed = false)
    }

    private fun advance(state: TasbihState, stageIndex: Int): TasbihState {
        val nextIndex = (stageIndex + 1).coerceAtMost(state.active.stages.lastIndex)
        return state.copy(
            stageIndex = nextIndex,
            count = 0,
            goal = state.active.stages[nextIndex].target.coerceIn(1, MAX_GOAL),
            completed = false
        )
    }

    private fun complete(state: TasbihState): TasbihState = state.copy(count = state.goal, completed = true)

    /** Writes only the round position; the dhikr identity keys are owned by the app. */
    fun saveProgress(prefs: SharedPreferences, state: TasbihState, sync: Boolean = false) {
        val editor = prefs.edit()
            .putInt("tasbih_count", state.count)
            .putInt("tasbih_goal", state.goal)
            .putInt("tasbih_stage", state.stageIndex)
            .putBoolean("tasbih_completed", state.completed)
        if (sync) editor.commit() else editor.apply()
    }

    /** Widget entry: one tap, persisted and logged to history. Safe to call from any thread. */
    fun tapFromWidget(prefs: SharedPreferences, today: LocalDate = LocalDate.now()): TasbihTap = synchronized(lock) {
        val custom = TasbihRepository.loadCustom(prefs)
        val tap = increment(load(prefs, custom))
        saveProgress(prefs, tap.state, sync = true)
        if (tap.counted || tap.roundCompleted) {
            TasbihHistory.record(
                prefs, today, tap.state.active,
                count = if (tap.counted) 1 else 0,
                rounds = if (tap.roundCompleted) 1 else 0,
                sync = true
            )
        }
        tap
    }

    /**
     * Widget "‹ ›": switch to the previous (-1) or next (+1) dhikr, built-ins first then the
     * user's own, wrapping around. Starts that dhikr at its first stage, exactly like picking it
     * in the app. Safe from any thread.
     */
    fun selectAdjacentFromWidget(prefs: SharedPreferences, delta: Int): TasbihDhikr = synchronized(lock) {
        val custom = TasbihRepository.loadCustom(prefs)
        val all = TasbihRepository.builtIns + custom
        val current = load(prefs, custom).active
        val index = all.indexOfFirst { it.id == current.id }
        val target = all[Math.floorMod((if (index < 0) 0 else index) + delta, all.size)]
        val first = target.stages.first().target.coerceIn(1, MAX_GOAL)
        prefs.edit()
            .putString("tasbih_active_id", target.id)
            .putString("tasbih_dhikr", target.fullTitle(false))
            .putInt("tasbih_stage", 0)
            .putInt("tasbih_count", 0)
            .putInt("tasbih_goal", first)
            .putBoolean("tasbih_completed", false)
            .commit()
        target
    }

    fun resetFromWidget(prefs: SharedPreferences): TasbihState = synchronized(lock) {
        val custom = TasbihRepository.loadCustom(prefs)
        val state = reset(load(prefs, custom))
        saveProgress(prefs, state, sync = true)
        state
    }
}
