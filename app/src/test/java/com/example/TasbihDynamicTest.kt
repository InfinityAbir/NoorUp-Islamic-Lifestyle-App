package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.TasbihRepository
import com.example.ui.noorup.TasbihStage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Dynamic tasbih coverage: CRUD, built-in protection, stages, exact counting,
 * exactly-once completion feedback, and persistence/restoration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TasbihDynamicTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun vm(): NoorUpViewModel = NoorUpViewModel(app)

    @Test
    fun `increment is exactly one per call and stops at the target`() {
        val viewModel = vm()
        viewModel.incrementTasbih()
        assertEquals(1, viewModel.tasbihCount.value)
        viewModel.incrementTasbih()
        assertEquals(2, viewModel.tasbihCount.value)
        repeat(31) { viewModel.incrementTasbih() }
        assertEquals(33, viewModel.tasbihCount.value)
        assertTrue(viewModel.tasbihCompleted.value)
        viewModel.incrementTasbih()
        viewModel.incrementTasbih()
        assertEquals("Extra taps must not count past a completed round", 33, viewModel.tasbihCount.value)
    }

    @Test
    fun `single-stage round emits exactly one completion event`() = runBlocking {
        val viewModel = vm()
        val events = mutableListOf<com.example.ui.noorup.TasbihCompletionEvent>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.completionEvents.collect { events.add(it) }
        }
        repeat(33) { viewModel.incrementTasbih() }
        yield()
        assertEquals(1, events.size)
        assertEquals(33, events.single().totalTarget)
        viewModel.incrementTasbih()
        yield()
        assertEquals("No duplicate event after completion", 1, events.size)
        job.cancelAndJoin()
    }

    @Test
    fun `multi-stage sequence advances and celebrates only after the final stage`() = runBlocking {
        val viewModel = vm()
        assertTrue(viewModel.selectDhikr("builtin_tasbih_fatimah"))
        val event = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.completionEvents.first()
        }

        repeat(33) { viewModel.incrementTasbih() }
        assertEquals(1, viewModel.tasbihStageIndex.value)
        assertEquals(0, viewModel.tasbihCount.value)
        assertEquals(33, viewModel.tasbihGoal.value)
        assertFalse(viewModel.tasbihCompleted.value)
        assertFalse("No celebration after stage 1", event.isCompleted)

        repeat(33) { viewModel.incrementTasbih() }
        assertEquals(2, viewModel.tasbihStageIndex.value)
        assertEquals(0, viewModel.tasbihCount.value)
        assertEquals(34, viewModel.tasbihGoal.value)
        assertFalse("No celebration after stage 2", event.isCompleted)

        repeat(34) { viewModel.incrementTasbih() }
        assertTrue(viewModel.tasbihCompleted.value)
        assertEquals(34, viewModel.tasbihCount.value)
        val completion = event.await()
        assertEquals(100, completion.totalTarget)
        assertEquals(3, completion.stageCount)
    }

    @Test
    fun `restoring a completed round does not replay feedback`() = runBlocking {
        val viewModel = vm()
        repeat(33) { viewModel.incrementTasbih() }
        assertTrue(viewModel.tasbihCompleted.value)

        val restored = vm()
        assertTrue(restored.tasbihCompleted.value)
        assertEquals(33, restored.tasbihCount.value)
        assertEquals("builtin_subhanallah", restored.activeDhikr.value.id)
        assertNull(
            "Restoration must not emit a completion event",
            withTimeoutOrNull(120) { restored.completionEvents.first() }
        )
    }

    @Test
    fun `custom zikr crud persists and falls back safely after delete`() {
        val viewModel = vm()
        val id = viewModel.addCustomDhikr(
            titleBangla = "কাস্টম যিকির",
            titleEnglish = "Custom Zikr",
            arabicText = "سُبْحَانَ اللَّهِ",
            stages = listOf(TasbihStage("ধাপ এক", "Step one", 3))
        )
        assertNotNull(id)
        val safeId = id!!
        assertEquals(1, viewModel.customDhikrs.value.size)
        assertEquals(safeId, viewModel.activeDhikr.value.id)
        assertEquals(3, viewModel.tasbihGoal.value)

        repeat(3) { viewModel.incrementTasbih() }
        assertTrue(viewModel.tasbihCompleted.value)

        val restored = vm()
        val restoredCustom = restored.customDhikrs.value.single { it.id == safeId }
        assertEquals("কাস্টম যিকির", restoredCustom.titleBangla)
        assertEquals(safeId, restored.activeDhikr.value.id)
        assertTrue(restored.tasbihCompleted.value)

        assertTrue(restored.deleteCustomDhikr(safeId))
        assertTrue(restored.customDhikrs.value.isEmpty())
        assertEquals("builtin_subhanallah", restored.activeDhikr.value.id)
    }

    @Test
    fun `custom target edit persists and keeps custom protection`() {
        val viewModel = vm()
        val id = viewModel.addCustomDhikr(
            "লক্ষ্য", "Target", "",
            listOf(TasbihStage("লক্ষ্য", "Target", 5))
        )!!
        viewModel.setTasbihGoal(12)
        assertEquals(12, viewModel.tasbihGoal.value)
        assertEquals(12, viewModel.activeDhikr.value.stages.single().target)

        val restored = vm()
        assertEquals(id, restored.activeDhikr.value.id)
        assertEquals(12, restored.tasbihGoal.value)
        assertEquals(12, restored.activeDhikr.value.stages.single().target)
    }

    @Test
    fun `built-ins cannot be edited or deleted but can be duplicated`() {
        val viewModel = vm()
        assertFalse(viewModel.deleteCustomDhikr("builtin_subhanallah"))
        assertFalse(
            viewModel.updateCustomDhikr(
                id = "builtin_subhanallah",
                titleBangla = "Hacked",
                titleEnglish = "Hacked",
                arabicText = "",
                stages = listOf(TasbihStage("Hacked", "Hacked", 1))
            )
        )
        assertTrue(viewModel.customDhikrs.value.isEmpty())
        assertEquals("সুবহানাল্লাহ", viewModel.activeDhikr.value.titleBangla)
        assertTrue(viewModel.activeDhikr.value.isBuiltIn)

        val copyId = viewModel.duplicateBuiltInDhikr("builtin_subhanallah")
        assertNotNull(copyId)
        assertEquals(1, viewModel.customDhikrs.value.size)
        val copy = viewModel.customDhikrs.value.single()
        assertFalse(copy.isBuiltIn)
        assertTrue(copy.titleEnglish.contains("Copy"))
        assertTrue(TasbihRepository.isBuiltInId("builtin_subhanallah"))
        assertTrue(TasbihRepository.builtIns.any { it.id == "builtin_subhanallah" })
    }

    @Test
    fun `selecting a built-in resets stage count and goal`() {
        val viewModel = vm()
        repeat(10) { viewModel.incrementTasbih() }
        assertTrue(viewModel.selectDhikr("builtin_tasbih_fatimah"))
        assertEquals(0, viewModel.tasbihStageIndex.value)
        assertEquals(0, viewModel.tasbihCount.value)
        assertEquals(33, viewModel.tasbihGoal.value)
        assertFalse(viewModel.tasbihCompleted.value)
    }
}
