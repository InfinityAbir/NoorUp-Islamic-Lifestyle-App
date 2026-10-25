package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.quran.QuranRepository
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private data class TestAssetVerse(
  val number: Int = 0,
  val arabic: String = "",
  val bangla: String = "",
  val english: String = ""
)

private data class TestAssetSurah(
  val number: Int = 0,
  val totalVerses: Int = 0,
  val verses: List<TestAssetVerse> = emptyList()
)

/**
 * Regression tests for the missing-ayat class of bug (Baqarah showing 5 of 286).
 *
 * Verifies that every bundled offline surah file is gap-free (1..totalVerses) and that
 * the in-memory surah lists always match the canonical totals on any device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuranOfflineCompletenessTest {

  private val gson = Gson()

  private fun loadAssetSurah(context: Context, surahNumber: Int): TestAssetSurah {
    val fileName = "quran/surah_%03d.json".format(surahNumber)
    context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { reader ->
      return gson.fromJson(reader, TestAssetSurah::class.java)
    }
  }

  @Test
  fun `all 114 offline surah assets are present`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val files = context.assets.list("quran")
      ?.filter { it.startsWith("surah_") && it.endsWith(".json") }
      .orEmpty()
    assertEquals("Expected 114 bundled surah files", 114, files.size)
  }

  @Test
  fun `every offline surah has a gap-free verse sequence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var grandTotal = 0
    for (n in 1..114) {
      val surah = loadAssetSurah(context, n)
      assertEquals("Surah $n file number mismatch", n, surah.number)
      assertEquals(
        "Surah $n must contain exactly totalVerses entries (no missing ayat)",
        surah.totalVerses, surah.verses.size
      )
      val numbers = surah.verses.map { it.number }
      assertEquals(
        "Surah $n verses must be exactly 1..totalVerses with no gaps",
        (1..surah.totalVerses).toList(), numbers
      )
      assertTrue(
        "Surah $n verses must carry Arabic, Bangla and English text",
        surah.verses.all { it.arabic.isNotBlank() && it.bangla.isNotBlank() && it.english.isNotBlank() }
      )
      grandTotal += surah.totalVerses
    }
    assertEquals("Full Quran must total 6236 ayahs", 6236, grandTotal)
  }

  @Test
  fun `metadata totals match bundled assets for all surahs`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    for (meta in QuranRepository.all114Surahs) {
      val asset = loadAssetSurah(context, meta.number)
      assertEquals(
        "Surah ${meta.number} metadata totalVerses must match bundled asset",
        asset.totalVerses, meta.totalVerses
      )
    }
  }

  @Test
  fun `viewmodel starts unloaded, then exposes complete verses after open`() {
    // Contract since the v1.2.0 placeholder fix: unopened surahs expose
    // metadata with EMPTY verse lists (never partial or synthetic content).
    // Full verses appear only after openSurah loads the validated bundle.
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = NoorUpViewModel(application)
    val surahs = vm.surahs.value
    assertEquals(114, surahs.size)
    for (surah in surahs) {
      assertTrue(
        "Surah ${surah.number} must start unloaded, never partially filled",
        surah.verses.isEmpty()
      )
    }

    // Representative surah converges to the complete validated list on open.
    vm.openSurah(112)
    val deadline = System.currentTimeMillis() + 20000
    while (vm.isQuranLoading.value || vm.selectedSurahVerses.value.size != 4) {
      if (System.currentTimeMillis() > deadline) break
      Thread.sleep(50)
    }
    val verses = vm.selectedSurahVerses.value
    assertEquals(4, verses.size)
    assertEquals(
      listOf(1, 2, 3, 4), verses.map { it.number }
    )
    assertTrue(
      "Loaded verses must carry Arabic, Bangla and English text",
      verses.all { it.arabicText.isNotBlank() && it.banglaTranslation.isNotBlank() && it.englishTranslation.isNotBlank() }
    )
  }
}
