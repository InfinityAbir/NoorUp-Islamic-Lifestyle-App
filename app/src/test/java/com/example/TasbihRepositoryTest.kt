package com.example

import com.example.ui.noorup.TasbihDhikr
import com.example.ui.noorup.TasbihRepository
import com.example.ui.noorup.TasbihStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasbihRepositoryTest {

    @Test
    fun `built-ins are protected and include a multi-stage sequence`() {
        val builtIns = TasbihRepository.builtIns
        assertEquals(6, builtIns.size)
        assertEquals(builtIns.size, builtIns.map { it.id }.distinct().size)
        assertTrue(builtIns.all { it.isBuiltIn })
        assertTrue(builtIns.all { TasbihRepository.isBuiltInId(it.id) })
        assertTrue(builtIns.all { it.stages.isNotEmpty() })
        assertTrue(builtIns.all { dhikr -> dhikr.stages.all { it.target in 1..TasbihRepository.MAX_TARGET } })

        val sequence = builtIns.first { it.id == "builtin_tasbih_fatimah" }
        assertEquals(3, sequence.stages.size)
        assertEquals(100, sequence.totalTarget)
    }

    @Test
    fun `sanitize rejects built-in ids and forces custom flag`() {
        val builtIn = TasbihRepository.builtIns.first()
        val disguisedAsCustom = builtIn.copy(isBuiltIn = false)
        assertTrue(TasbihRepository.sanitizeCustom(listOf(disguisedAsCustom)).isEmpty())

        val custom = TasbihDhikr(
            id = "custom_one",
            titleBangla = "পরীক্ষা",
            titleEnglish = "Test",
            stages = listOf(TasbihStage("পরীক্ষা", "Test", 7)),
            isBuiltIn = true
        )
        val safe = TasbihRepository.sanitizeCustom(listOf(custom)).single()
        assertFalse(safe.isBuiltIn)
        assertEquals("custom_one", safe.id)
        assertEquals(7, safe.totalTarget)
    }

    @Test
    fun `invalid custom definitions are dropped or repaired`() {
        val base = TasbihDhikr(
            id = "custom_base",
            titleBangla = "ভিত্তি",
            titleEnglish = "Base",
            stages = listOf(TasbihStage("ভিত্তি", "Base", 5))
        )
        assertTrue(
            TasbihRepository.sanitizeCustom(
                listOf(base.copy(titleBangla = "", titleEnglish = "", stages = base.stages))
            ).isEmpty()
        )
        assertTrue(
            TasbihRepository.sanitizeCustom(
                listOf(base.copy(stages = listOf(TasbihStage("x", "x", 0))))
            ).isEmpty()
        )
        assertTrue(
            TasbihRepository.sanitizeCustom(
                listOf(base.copy(stages = emptyList()))
            ).isEmpty()
        )
        assertTrue(
            TasbihRepository.sanitizeCustom(
                listOf(base.copy(id = "builtin_subhanallah"))
            ).isEmpty()
        )

        val duplicate = TasbihRepository.sanitizeCustom(listOf(base, base.copy(titleBangla = "দ্বিতীয়")))
        assertEquals(1, duplicate.size)

        val repaired = TasbihRepository.sanitizeCustom(
            listOf(base.copy(stages = listOf(TasbihStage("", "", 9))))
        ).single()
        assertEquals("ভিত্তি", repaired.stages.single().labelBangla)
        assertEquals(9, repaired.stages.single().target)
    }

    @Test
    fun `validation returns stable error codes`() {
        assertEquals(
            "title",
            TasbihRepository.validationError("", "", listOf(TasbihStage("x", "x", 1)))
        )
        assertEquals(
            "stages",
            TasbihRepository.validationError("x", "x", emptyList())
        )
        assertEquals(
            "target",
            TasbihRepository.validationError("x", "x", listOf(TasbihStage("x", "x", 0)))
        )
        assertNull(
            TasbihRepository.validationError("x", "y", listOf(TasbihStage("x", "x", 1)))
        )
    }

    @Test
    fun `legacy text maps to the right built-in`() {
        assertNotNull(TasbihRepository.findBuiltInByLegacyText("সুবহানাল্লাহ (Subhanallah)"))
        assertEquals(
            "builtin_alhamdulillah",
            TasbihRepository.findBuiltInByLegacyText("Alhamdulillah")?.id
        )
        assertEquals(
            "builtin_la_ilaha_illallah",
            TasbihRepository.findBuiltInByLegacyText("লা ইলাহা ইল্লাল্লাহ (La ilaha illallah)")?.id
        )
        assertEquals(
            "legacy_active",
            TasbihRepository.legacyDefinition("Some old custom text", 42).id
        )
        assertEquals(
            42,
            TasbihRepository.legacyDefinition("Some old custom text", 42).totalTarget
        )
    }
}
