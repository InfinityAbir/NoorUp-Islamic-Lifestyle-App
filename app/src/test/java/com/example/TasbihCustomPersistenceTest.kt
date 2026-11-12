package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.TasbihDhikr
import com.example.ui.noorup.TasbihRepository
import com.example.ui.noorup.TasbihStage
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression: release (R8) builds wrote obfuscated Gson keys, so saved custom dhikrs
 * were dropped on the next launch. Storage must use the explicit, stable key names.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TasbihCustomPersistenceTest {

    private val sample = TasbihDhikr(
        id = "custom_abc-123",
        titleBangla = "আমার যিকির",
        titleEnglish = "My Zikr",
        arabicText = "سُبْحَانَ اللَّهِ",
        stages = listOf(
            TasbihStage("এক", "One", 10),
            TasbihStage("দুই", "Two", 20)
        )
    )

    @Test
    fun encodedJsonUsesStableKeys() {
        val obj = JsonParser.parseString(TasbihRepository.encodeCustom(listOf(sample)))
            .asJsonArray[0].asJsonObject
        listOf("id", "titleBangla", "titleEnglish", "arabicText", "stages").forEach {
            assertTrue("missing key $it", obj.has(it))
        }
        val stage = obj.getAsJsonArray("stages")[0].asJsonObject
        listOf("labelBangla", "labelEnglish", "target").forEach {
            assertTrue("missing stage key $it", stage.has(it))
        }
    }

    @Test
    fun saveThenLoadRoundTrips() {
        val prefs = ApplicationProvider.getApplicationContext<android.app.Application>()
            .getSharedPreferences("tasbih_persist_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        TasbihRepository.saveCustom(prefs, listOf(sample))
        val loaded = TasbihRepository.loadCustom(prefs)
        assertEquals(listOf(sample), loaded)
    }
}
