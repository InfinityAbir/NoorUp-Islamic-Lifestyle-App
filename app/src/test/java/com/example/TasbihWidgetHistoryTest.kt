package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.TasbihCounter
import com.example.ui.noorup.TasbihHistory
import com.example.ui.noorup.TasbihRepository
import com.example.ui.noorup.TasbihState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TasbihWidgetHistoryTest {

    private lateinit var app: Application
    private val prefs get() = app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
    private val subhanallah = TasbihRepository.builtIns.first { it.id == "builtin_subhanallah" }
    private val fatimah = com.example.ui.noorup.TasbihDhikr(
        id = "custom_fatimah_test",
        titleBangla = "ফাতিমী",
        titleEnglish = "Fatimi",
        stages = listOf(
            com.example.ui.noorup.TasbihStage("সুবহানাল্লাহ", "Subhanallah", 33),
            com.example.ui.noorup.TasbihStage("আলহামদুলিল্লাহ", "Alhamdulillah", 33),
            com.example.ui.noorup.TasbihStage("আল্লাহু আকবার", "Allahu Akbar", 34)
        )
    )

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        prefs.edit().clear().commit()
    }

    private fun fresh(d: com.example.ui.noorup.TasbihDhikr) =
        TasbihState(d, 0, 0, d.stages.first().target, false)

    // ---- counting rules -------------------------------------------------

    @Test
    fun `single stage counts up then completes exactly once`() {
        var s = fresh(subhanallah)
        repeat(32) { s = TasbihCounter.increment(s).state }
        assertEquals(32, s.count)
        assertFalse(s.completed)

        val last = TasbihCounter.increment(s)
        assertTrue(last.counted)
        assertTrue(last.roundCompleted)
        assertTrue(last.state.completed)
        assertEquals(33, last.state.count)

        val extra = TasbihCounter.increment(last.state)
        assertFalse("taps after completion are ignored", extra.counted)
        assertEquals(last.state, extra.state)
    }

    @Test
    fun `multi stage round advances stage and counts every tap`() {
        var s = fresh(fatimah)
        var counted = 0
        var completions = 0
        repeat(33 + 33 + 34) {
            val t = TasbihCounter.increment(s)
            if (t.counted) counted++
            if (t.roundCompleted) completions++
            s = t.state
        }
        assertEquals(100, counted)
        assertEquals(1, completions)
        assertTrue(s.completed)
        assertEquals(2, s.stageIndex)
    }

    @Test
    fun `reset returns to first stage and its goal`() {
        var s = fresh(fatimah)
        repeat(40) { s = TasbihCounter.increment(s).state }
        assertEquals(1, s.stageIndex)
        val r = TasbihCounter.reset(s)
        assertEquals(0, r.stageIndex)
        assertEquals(0, r.count)
        assertEquals(33, r.goal)
        assertFalse(r.completed)
    }

    // ---- widget path ------------------------------------------------------

    @Test
    fun `widget tap persists progress and writes history`() {
        val today = LocalDate.of(2026, 11, 6)
        repeat(5) { TasbihCounter.tapFromWidget(prefs, today) }

        val state = TasbihCounter.load(prefs, emptyList())
        assertEquals(5, state.count)
        val day = TasbihHistory.loadDay(prefs, today)
        assertEquals(5, day.total)
        assertEquals("builtin_subhanallah", day.entries.single().id)
    }

    @Test
    fun `widget completing a round records one round and no phantom counts`() {
        val today = LocalDate.of(2026, 11, 6)
        repeat(40) { TasbihCounter.tapFromWidget(prefs, today) }
        val day = TasbihHistory.loadDay(prefs, today)
        assertEquals(33, day.total)
        assertEquals(1, day.rounds)
        assertTrue(TasbihCounter.load(prefs, emptyList()).completed)
    }

    @Test
    fun `widget reset clears the round but keeps history`() {
        val today = LocalDate.now()
        repeat(10) { TasbihCounter.tapFromWidget(prefs, today) }
        TasbihCounter.resetFromWidget(prefs)
        assertEquals(0, TasbihCounter.load(prefs, emptyList()).count)
        assertEquals(10, TasbihHistory.loadDay(prefs, today).total)
    }

    @Test
    fun `app picks up counts made by the widget`() {
        val vm = NoorUpViewModel(app)
        assertEquals(0, vm.tasbihCount.value)
        repeat(7) { TasbihCounter.tapFromWidget(prefs) }
        vm.refreshTasbihFromStorage()
        assertEquals(7, vm.tasbihCount.value)
    }

    @Test
    fun `in app taps are written to history and survive a new view model`() {
        val vm = NoorUpViewModel(app)
        repeat(3) { vm.incrementTasbih() }
        assertEquals(3, TasbihHistory.loadDay(prefs, LocalDate.now()).total)
        assertEquals(3, NoorUpViewModel(app).tasbihCount.value)
    }

    // ---- switching zikr from the widget -----------------------------------

    @Test
    fun `widget next and previous cycle through built in dhikrs and wrap`() {
        val all = TasbihRepository.builtIns
        assertEquals(all.first().id, TasbihCounter.load(prefs, emptyList()).active.id)

        assertEquals(all[1].id, TasbihCounter.selectAdjacentFromWidget(prefs, +1).id)
        assertEquals(all[1].id, TasbihCounter.load(prefs, emptyList()).active.id)

        assertEquals(all.first().id, TasbihCounter.selectAdjacentFromWidget(prefs, -1).id)
        // previous from the first wraps to the last
        assertEquals(all.last().id, TasbihCounter.selectAdjacentFromWidget(prefs, -1).id)
        // next from the last wraps to the first
        assertEquals(all.first().id, TasbihCounter.selectAdjacentFromWidget(prefs, +1).id)
    }

    @Test
    fun `switching zikr starts a fresh round at its first stage and keeps history`() {
        val today = LocalDate.now()
        repeat(10) { TasbihCounter.tapFromWidget(prefs, today) }
        TasbihCounter.selectAdjacentFromWidget(prefs, +1)
        val s = TasbihCounter.load(prefs, emptyList())
        assertEquals(0, s.count)
        assertEquals(0, s.stageIndex)
        assertEquals(33, s.goal)
        assertFalse(s.completed)
        assertEquals(10, TasbihHistory.loadDay(prefs, today).total)

        TasbihCounter.tapFromWidget(prefs, today)
        val day = TasbihHistory.loadDay(prefs, today)
        assertEquals(11, day.total)
        assertEquals(2, day.entries.size)
    }

    @Test
    fun `widget switch includes the users own dhikrs after the built ins`() {
        val vm = NoorUpViewModel(app)
        val id = vm.addCustomDhikr(
            "আমার যিকির", "My Zikr", "",
            listOf(com.example.ui.noorup.TasbihStage("আমার যিকির", "My Zikr", 7))
        )!!
        // Park the active dhikr on the last built in, then step forward onto the custom one.
        prefs.edit().putString("tasbih_active_id", TasbihRepository.builtIns.last().id).commit()
        val next = TasbihCounter.selectAdjacentFromWidget(prefs, +1)
        assertEquals(id, next.id)
        assertEquals(7, TasbihCounter.load(prefs, TasbihRepository.loadCustom(prefs)).goal)
    }

    @Test
    fun `widget cycling follows the user chosen built in order`() {
        val vm = NoorUpViewModel(app)
        val first = TasbihRepository.builtIns.first().id
        vm.moveBuiltInDhikr(first, +1)
        val ordered = TasbihRepository.orderedBuiltIns(TasbihRepository.loadBuiltInOrder(prefs))
        // active is the old first built-in, now at index 1, so next is index 2
        assertEquals(ordered[2].id, TasbihCounter.selectAdjacentFromWidget(prefs, +1).id)
    }

    @Test
    fun `a saved selection of the removed fatimah built in falls back to the first built in`() {
        prefs.edit().putString("tasbih_active_id", "builtin_tasbih_fatimah")
            .putInt("tasbih_stage", 2).putInt("tasbih_count", 5).commit()
        val s = TasbihCounter.load(prefs, emptyList())
        assertEquals(TasbihRepository.builtIns.first().id, s.active.id)
        assertEquals(0, s.stageIndex)
        assertEquals(0, s.count)
    }

    @Test
    fun `app adopts a zikr switched from the widget`() {
        val vm = NoorUpViewModel(app)
        TasbihCounter.selectAdjacentFromWidget(prefs, +1)
        vm.refreshTasbihFromStorage()
        assertEquals(TasbihRepository.builtIns[1].id, vm.activeDhikr.value.id)
        assertEquals(0, vm.tasbihCount.value)
    }

    // ---- history store ----------------------------------------------------

    @Test
    fun `history accumulates per dhikr and per day`() {
        val d1 = LocalDate.of(2026, 11, 5)
        val d2 = LocalDate.of(2026, 11, 6)
        TasbihHistory.record(prefs, d1, subhanallah, count = 3)
        TasbihHistory.record(prefs, d1, fatimah, count = 2)
        TasbihHistory.record(prefs, d2, subhanallah, count = 4, rounds = 1)

        assertEquals(5, TasbihHistory.loadDay(prefs, d1).total)
        assertEquals(2, TasbihHistory.loadDay(prefs, d1).entries.size)
        assertEquals(1, TasbihHistory.loadDay(prefs, d2).rounds)

        val range = TasbihHistory.loadRange(prefs, d2, 3)
        assertEquals(listOf(d2, d1, d1.minusDays(1)), range.map { it.date })
        assertEquals(listOf(4, 5, 0), range.map { it.total })

        val (total, activeDays) = TasbihHistory.totals(prefs)
        assertEquals(9, total)
        assertEquals(2, activeDays)
    }

    @Test
    fun `history keeps the dhikr title after a custom dhikr is deleted`() {
        val vm = NoorUpViewModel(app)
        val id = vm.addCustomDhikr(
            "আমার যিকির", "My Zikr", "",
            listOf(com.example.ui.noorup.TasbihStage("আমার যিকির", "My Zikr", 5))
        )!!
        repeat(2) { vm.incrementTasbih() }
        vm.deleteCustomDhikr(id)
        val entry = TasbihHistory.loadDay(prefs, LocalDate.now()).entries.single()
        assertEquals("My Zikr", entry.title(true))
        assertEquals(2, entry.count)
    }

    @Test
    fun `corrupt history value is ignored not fatal`() {
        val d = LocalDate.of(2026, 1, 2)
        prefs.edit().putString("tasbih_hist_$d", "{not json").commit()
        assertTrue(TasbihHistory.loadDay(prefs, d).isEmpty)
        TasbihHistory.record(prefs, d, subhanallah, count = 1)
        assertEquals(1, TasbihHistory.loadDay(prefs, d).total)
    }

    @Test
    fun `entries older than the retention window are pruned when a new day starts`() {
        val today = LocalDate.of(2026, 11, 6)
        val old = today.minusDays(TasbihHistory.RETENTION_DAYS + 5)
        TasbihHistory.record(prefs, old, subhanallah, count = 9)
        assertEquals(9, TasbihHistory.loadDay(prefs, old).total)
        TasbihHistory.record(prefs, today, subhanallah, count = 1)
        assertTrue(TasbihHistory.loadDay(prefs, old).isEmpty)
        assertEquals(1, TasbihHistory.loadDay(prefs, today).total)
    }

    @Test
    fun `history keys are part of the backup whitelist`() {
        assertTrue(com.example.ui.noorup.backup.BackupCodec.isBackedUp("tasbih_hist_2026-11-06"))
    }
}
