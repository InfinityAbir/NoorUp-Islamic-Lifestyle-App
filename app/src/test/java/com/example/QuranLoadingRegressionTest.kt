package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.Verse
import com.example.ui.noorup.quran.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Release-blocker regressions (v1.2.0): synthetic placeholder verses must never
 * render as genuine Quranic text. Covers scenarios 1-8 of
 * docs/NOORUP_PRE_RELEASE_VERIFICATION.md section 8.
 *
 * Conventions mirror QuranOfflineCompletenessTest: Robolectric + ApplicationProvider,
 * plain JUnit asserts, no new test libraries. ViewModel IO work runs on real
 * background threads, so async assertions poll with a deadline instead of
 * depending on exact scheduling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuranLoadingRegressionTest {

  companion object {
    private const val PLACEHOLDER_AR = "آيَةٌ كَرِيمَةٌ"
    private const val PLACEHOLDER_BN = "অফলাইন সম্পূর্ণ পাঠ লোড হচ্ছে"
    private const val PLACEHOLDER_EN = "Divine guidance and wisdom from the Holy Quran."
  }

  private fun app(): Application = ApplicationProvider.getApplicationContext()

  private fun containsPlaceholder(verses: List<Verse>): Boolean =
    verses.any {
      it.arabicText.contains(PLACEHOLDER_AR) ||
        it.banglaTranslation.contains(PLACEHOLDER_BN) ||
        it.englishTranslation.contains(PLACEHOLDER_EN)
    }

  /** Poll until [cond] holds or the deadline passes; pumps the Main looper for safety. */
  private fun awaitSettled(timeoutMs: Long = 20000, cond: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!cond()) {
      try { Shadows.shadowOf(Looper.getMainLooper()).idle() } catch (_: Exception) { }
      if (System.currentTimeMillis() > deadline) break
      Thread.sleep(50)
    }
  }

  // --- 1. Unopened surahs never expose synthetic verses as content ---

  @Test
  fun `unopened surahs expose empty verse lists, never placeholders`() {
    val vm = NoorUpViewModel(app())
    val all = vm.surahs.value
    assertEquals(114, all.size)
    // Metadata must stay available without fabricating content.
    assertEquals(286, all.find { it.number == 2 }?.totalVerses)
    for (surah in all) {
      assertTrue(
        "Surah ${surah.number} must start unloaded (empty verses)",
        surah.verses.isEmpty()
      )
    }
    assertFalse(
      "No placeholder text anywhere in initial state",
      all.any { containsPlaceholder(it.verses) }
    )
    assertFalse(
      "No placeholder text in the selected-verse list either",
      containsPlaceholder(vm.selectedSurahVerses.value)
    )
  }

  // --- 2. Loading state while genuine content is unavailable ---

  @Test
  fun `opening a surah shows loading with empty list, never placeholders mid-load`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(112)
    val snapshots = mutableListOf<List<Verse>>()
    // Sample throughout the load; every snapshot must be empty or authentic.
    repeat(40) {
      snapshots.add(vm.selectedSurahVerses.value.toList())
      try { Shadows.shadowOf(Looper.getMainLooper()).idle() } catch (_: Exception) { }
      Thread.sleep(25)
    }
    awaitSettled { !vm.isQuranLoading.value }
    for ((i, snap) in snapshots.withIndex()) {
      assertFalse(
        "Snapshot $i during load must not contain placeholders",
        containsPlaceholder(snap)
      )
    }
    assertEquals(112, vm.selectedSurahNumber.value)
    assertEquals(4, vm.selectedSurahVerses.value.size)
    assertFalse(vm.quranLoadError.value)
  }

  // --- 3. Successful loading renders the actual bundled verses ---

  @Test
  fun `loaded verses match the validated bundled asset`() {
    val context = app()
    for (n in listOf(1, 2, 112)) {
      val loaded = runBlocking(Dispatchers.IO) { QuranRepository.loadFullSurah(context, n) }
      val total = QuranRepository.getSurahMeta(n)!!.totalVerses
      assertTrue(
        "Surah $n must pass the authenticity gate",
        QuranRepository.isAuthenticVerseList(loaded, total)
      )
      assertEquals(total, loaded.size)
      assertEquals((1..total).toList(), loaded.map { it.number })
      assertFalse(containsPlaceholder(loaded))
    }
  }

  @Test
  fun `openSurah publishes authentic verses for short and long surahs`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(2)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 286 }
    val verses = vm.selectedSurahVerses.value
    assertEquals(286, verses.size)
    assertEquals((1..286).toList(), verses.map { it.number })
    assertFalse(containsPlaceholder(verses))
    assertFalse(vm.quranLoadError.value)
    // Master list converges to the same authentic content.
    val master = vm.surahs.value.find { it.number == 2 }!!
    assertEquals(286, master.verses.size)
    assertFalse(containsPlaceholder(master.verses))
  }

  // --- 4. Loading failure shows no fabricated content + exposes error ---

  @Test
  fun `invalid surah loads nothing and reports an error`() {
    val context = app()
    val bad = runBlocking(Dispatchers.IO) { QuranRepository.loadFullSurah(context, 0) }
    assertTrue(bad.isEmpty())
    val bad2 = runBlocking(Dispatchers.IO) { QuranRepository.loadFullSurah(context, 115) }
    assertTrue(bad2.isEmpty())

    val vm = NoorUpViewModel(app())
    vm.openSurah(0)
    awaitSettled { !vm.isQuranLoading.value }
    assertTrue(vm.selectedSurahVerses.value.isEmpty())
    assertTrue("Failed load must expose quranLoadError", vm.quranLoadError.value)
    // Retry of an unloadable surah keeps error state, never fabricates.
    vm.retrySurahLoad()
    awaitSettled { !vm.isQuranLoading.value }
    assertTrue(vm.selectedSurahVerses.value.isEmpty())
    assertTrue(vm.quranLoadError.value)
  }

  @Test
  fun `validator rejects wrong size, gaps, and blank text`() {
    fun verse(n: Int, ar: String = "ب", bn: String = "ব", en: String = "e") =
      Verse(n, ar, bn, en)
    assertTrue(QuranRepository.isAuthenticVerseList(listOf(verse(1), verse(2)), 2))
    assertFalse(QuranRepository.isAuthenticVerseList(listOf(verse(1)), 2))
    assertFalse(QuranRepository.isAuthenticVerseList(listOf(verse(1), verse(3)), 2))
    assertFalse(QuranRepository.isAuthenticVerseList(listOf(verse(2), verse(1)), 2))
    assertFalse(QuranRepository.isAuthenticVerseList(listOf(verse(1, ar = "  "), verse(2)), 2))
    assertFalse(QuranRepository.isAuthenticVerseList(listOf(verse(1, bn = ""), verse(2)), 2))
    assertFalse(QuranRepository.isAuthenticVerseList(listOf(verse(1, en = "  "), verse(2)), 2))
    assertFalse(QuranRepository.isAuthenticVerseList(emptyList(), 7))
    assertFalse(QuranRepository.isAuthenticVerseList(listOf(verse(1)), 0))
  }

  // --- 5. Switching surahs never shows stale verses ---

  @Test
  fun `rapid surah switch lands on the latest selection with authentic verses`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(2)
    vm.openSurah(112)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 4 }
    assertEquals(112, vm.selectedSurahNumber.value)
    val verses = vm.selectedSurahVerses.value
    assertEquals(listOf(1, 2, 3, 4), verses.map { it.number })
    assertFalse(containsPlaceholder(verses))
    assertFalse(vm.quranLoadError.value)
  }

  // --- 6. Reopen works, bookmarks and memorized flags survive ---

  @Test
  fun `reopening a surah restores authentic verses with persisted flags`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(112)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 4 }
    vm.toggleBookmark(112, 1)
    vm.closeSurah()
    assertTrue(vm.selectedSurahVerses.value.isEmpty())
    assertFalse(vm.quranLoadError.value)
    vm.openSurah(112)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 4 }
    val first = vm.selectedSurahVerses.value.first { it.number == 1 }
    assertTrue("Bookmark flag must survive close + reopen", first.isBookmarked)
    assertFalse(containsPlaceholder(vm.selectedSurahVerses.value))
  }

  // --- 7. Offline repeat-open keeps working ---

  @Test
  fun `consecutive open close cycles stay authentic`() {
    val vm = NoorUpViewModel(app())
    repeat(3) {
      vm.openSurah(1)
      awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 7 }
      assertEquals((1..7).toList(), vm.selectedSurahVerses.value.map { it.number })
      assertFalse(containsPlaceholder(vm.selectedSurahVerses.value))
      vm.closeSurah()
    }
    assertTrue(vm.selectedSurahVerses.value.isEmpty())
  }

  // --- 8. Completeness expectations still hold at the metadata level ---

  @Test
  fun `metadata still covers 114 surahs and 6236 verses`() {
    val all = QuranRepository.all114Surahs
    assertEquals(114, all.size)
    assertEquals(6236, all.sumOf { it.totalVerses })
  }
}
