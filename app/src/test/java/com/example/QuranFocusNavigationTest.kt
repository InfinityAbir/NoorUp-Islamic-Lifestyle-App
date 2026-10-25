package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.Verse
import com.example.ui.noorup.quran.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Focus Mode navigation regressions:
 *  1. A bookmark opens Focus at its exact Surah + Ayat (never Ayat 1).
 *  2. A normally opened Surah enters Focus at Ayat 1 (never a bookmark / stale position).
 *  3. "Go to Ayat" navigates to a validated Ayat of the current Surah.
 *  4. First/last Ayat, Next/Prev boundaries and session restore stay intact.
 *
 * Conventions mirror QuranLoadingRegressionTest: Robolectric + ApplicationProvider,
 * plain JUnit asserts, polling for the ViewModel's real background IO.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuranFocusNavigationTest {

  private fun app(): Application = ApplicationProvider.getApplicationContext()

  private fun prefs() = app().getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)

  /** Poll until [cond] holds or the deadline passes; pumps the Main looper for safety. */
  private fun awaitSettled(timeoutMs: Long = 20000, cond: () -> Boolean): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!cond()) {
      try { Shadows.shadowOf(Looper.getMainLooper()).idle() } catch (_: Exception) { }
      if (System.currentTimeMillis() > deadline) return false
      Thread.sleep(50)
    }
    return true
  }

  private fun awaitFocus(vm: NoorUpViewModel, surah: Int, verse: Int): Boolean =
    awaitSettled {
      !vm.isFocusLoading.value &&
        vm.focusSurahNumber.value == surah &&
        vm.focusVerseNumber.value == verse &&
        vm.focusVerse.value?.number == verse
    }

  private fun awaitFocusLoaded(vm: NoorUpViewModel, surah: Int): Boolean =
    awaitSettled { !vm.isFocusLoading.value && vm.focusSurahNumber.value == surah }

  private fun repositoryVerse(surah: Int, verse: Int): Verse =
    runBlocking(Dispatchers.IO) { QuranRepository.loadFullSurah(app(), surah) }
      .first { it.number == verse }

  private fun assertShowing(vm: NoorUpViewModel, surah: Int, verse: Int) {
    assertTrue(
      "Focus must land on $surah:$verse but was ${vm.focusSurahNumber.value}:${vm.focusVerseNumber.value}",
      awaitFocus(vm, surah, verse)
    )
    val expected = repositoryVerse(surah, verse)
    val actual = vm.focusVerse.value
    assertNotNull("Verse $surah:$verse must be loaded", actual)
    assertEquals("Arabic text must match the bundled asset", expected.arabicText, actual!!.arabicText)
    assertEquals("Bangla translation must match", expected.banglaTranslation, actual.banglaTranslation)
    assertEquals("English translation must match", expected.englishTranslation, actual.englishTranslation)
    assertEquals(
      "Bangla pronunciation must match",
      expected.banglaTransliteration,
      actual.banglaTransliteration
    )
  }

  // --- 1. Bookmark -> Focus uses the exact bookmarked position ---

  @Test
  fun `focus from a bookmark starts at the bookmarked surah and ayat`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(3)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 200 }
    vm.toggleBookmark(3, 8)
    awaitSettled {
      vm.bookmarkedVerses.value.any { it.first.number == 3 && it.second.number == 8 }
    }

    // Create a stale, unrelated Focus session first: the bookmark must still win.
    vm.enterFocusAtSurahStart(112)
    awaitFocusLoaded(vm, 112)

    vm.enterFocusAtBookmark(3, 8)
    assertShowing(vm, 3, 8)
    assertTrue("The bookmarked Ayat must show its bookmark flag", vm.focusVerse.value!!.isBookmarked)
    assertEquals(3, prefs().getInt("focus_surah", -1))
    assertEquals(8, prefs().getInt("focus_verse", -1))
  }

  @Test
  fun `bookmark focus survives after the reader surah is closed`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(3)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 200 }
    vm.toggleBookmark(3, 8)
    vm.closeSurah()

    vm.enterFocusAtBookmark(3, 8)
    assertShowing(vm, 3, 8)
  }

  // --- 2. Reader -> Focus always starts at Ayat 1 ---

  @Test
  fun `focus from the reader starts at ayat 1 even after another surah was read`() {
    val vm = NoorUpViewModel(app())
    // Establish a previous session elsewhere.
    vm.enterFocusAtBookmark(3, 8)
    awaitFocusLoaded(vm, 3)
    vm.exitFocus()

    vm.openSurah(2)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 286 }
    vm.enterFocusAtSurahStart(2)

    assertShowing(vm, 2, 1)
    assertEquals(1, vm.focusVerseNumber.value)
  }

  @Test
  fun `focus from the reader never reuses an unrelated bookmark position`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(3)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 200 }
    vm.toggleBookmark(3, 8)
    awaitSettled {
      vm.bookmarkedVerses.value.any { it.first.number == 3 && it.second.number == 8 }
    }

    vm.openSurah(2)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 286 }
    vm.enterFocusAtSurahStart(2)

    assertShowing(vm, 2, 1)
    assertFalse("Ayat 1 of Al-Baqarah is not the bookmark", vm.focusVerse.value!!.isBookmarked)
  }

  // --- 3. Direct Ayat navigation ---

  @Test
  fun `go to ayat 88 of al-baqarah shows the correct ayat`() {
    val vm = NoorUpViewModel(app())
    vm.enterFocusAtSurahStart(2)
    assertShowing(vm, 2, 1)

    assertNull("A valid Ayat must navigate", vm.goToAyat(2, "88"))
    assertShowing(vm, 2, 88)
    assertEquals(88, vm.focusVerseNumber.value)
  }

  @Test
  fun `go to ayat works for short and long surahs`() {
    val vm = NoorUpViewModel(app())
    vm.enterFocusAtSurahStart(112)
    assertShowing(vm, 112, 1)
    assertNull(vm.goToAyat(112, "3"))
    assertShowing(vm, 112, 3)

    assertNull(vm.goToAyat(112, "4"))
    assertShowing(vm, 112, 4)
    assertEquals(4, QuranRepository.getSurahMeta(112)!!.totalVerses)
  }

  @Test
  fun `go to ayat accepts padded and bangla digit input`() {
    val vm = NoorUpViewModel(app())
    vm.enterFocusAtSurahStart(2)
    assertShowing(vm, 2, 1)

    assertNull(vm.goToAyat(2, " 88 "))
    assertShowing(vm, 2, 88)

    assertNull(vm.goToAyat(2, "৮৮"))
    assertShowing(vm, 2, 88)
  }

  // --- 4. Invalid input is rejected without moving ---

  @Test
  fun `go to ayat rejects empty negative decimal non-numeric and out of range input`() {
    val vm = NoorUpViewModel(app())
    vm.enterFocusAtSurahStart(2)
    assertShowing(vm, 2, 1)

    val cases = listOf(
      "" to NoorUpViewModel.AyatInputError.EMPTY,
      "   " to NoorUpViewModel.AyatInputError.EMPTY,
      "0" to NoorUpViewModel.AyatInputError.OUT_OF_RANGE,
      "287" to NoorUpViewModel.AyatInputError.OUT_OF_RANGE,
      "999999999999999999999" to NoorUpViewModel.AyatInputError.OUT_OF_RANGE,
      "-1" to NoorUpViewModel.AyatInputError.NOT_A_WHOLE_NUMBER,
      "1.5" to NoorUpViewModel.AyatInputError.NOT_A_WHOLE_NUMBER,
      "abc" to NoorUpViewModel.AyatInputError.NOT_A_WHOLE_NUMBER,
      "+8" to NoorUpViewModel.AyatInputError.NOT_A_WHOLE_NUMBER
    )
    for ((input, expected) in cases) {
      assertEquals(
        "Input [$input] must be rejected with $expected",
        expected,
        vm.goToAyat(2, input)
      )
      assertEquals("Invalid input must not change the Surah", 2, vm.focusSurahNumber.value)
      assertEquals("Invalid input must not change the Ayat", 1, vm.focusVerseNumber.value)
      assertEquals("Invalid input must not change the verse text", 1, vm.focusVerse.value?.number)
    }
  }

  @Test
  fun `go to ayat validates against the selected surah not a global maximum`() {
    val vm = NoorUpViewModel(app())
    vm.enterFocusAtSurahStart(112)
    assertShowing(vm, 112, 1)

    // 5 is valid in Al-Baqarah but not in Al-Ikhlas (4 Ayat).
    assertEquals(NoorUpViewModel.AyatInputError.OUT_OF_RANGE, vm.goToAyat(112, "5"))
    assertEquals(112, vm.focusSurahNumber.value)
    assertEquals(1, vm.focusVerseNumber.value)

    // 286 is valid in Al-Baqarah.
    assertNull(vm.goToAyat(2, "286"))
    assertShowing(vm, 2, 286)
  }

  // --- 5. First/last Ayat across surahs, and Next/Prev boundaries ---

  @Test
  fun `programmatic jump reaches the first and last ayat of several surahs`() {
    val vm = NoorUpViewModel(app())
    for (surah in listOf(1, 2, 3, 112, 114)) {
      val total = QuranRepository.getSurahMeta(surah)!!.totalVerses
      assertTrue("Jump to $surah:1 must be accepted", vm.focusJumpTo(surah, 1))
      assertShowing(vm, surah, 1)

      assertTrue("Jump to $surah:$total must be accepted", vm.focusJumpTo(surah, total))
      assertShowing(vm, surah, total)

      assertFalse("Jump to $surah:${total + 1} must be refused", vm.focusJumpTo(surah, total + 1))
      assertFalse("Jump to $surah:0 must be refused", vm.focusJumpTo(surah, 0))
    }
  }

  @Test
  fun `next and previous navigate from a jumped ayat and stop at surah boundaries`() {
    val vm = NoorUpViewModel(app())
    vm.enterFocusAtSurahStart(2)
    assertShowing(vm, 2, 1)

    assertNull(vm.goToAyat(2, "88"))
    assertShowing(vm, 2, 88)

    vm.focusNextVerse()
    assertShowing(vm, 2, 89)

    vm.focusPrevVerse()
    assertShowing(vm, 2, 88)

    // Last Ayat of Al-Baqarah -> first Ayat of Ali 'Imran.
    assertTrue(vm.focusJumpTo(2, 286))
    assertShowing(vm, 2, 286)
    vm.focusNextVerse()
    assertShowing(vm, 3, 1)

    // ...and back again.
    vm.focusPrevVerse()
    assertShowing(vm, 2, 286)
  }

  @Test
  fun `previous at the very first ayat and next at the very last are no-ops`() {
    val vm = NoorUpViewModel(app())
    vm.focusJumpTo(1, 1)
    assertShowing(vm, 1, 1)
    vm.focusPrevVerse()
    assertShowing(vm, 1, 1)
    assertEquals(1, vm.focusVerse.value?.number)

    val lastTotal = QuranRepository.getSurahMeta(114)!!.totalVerses
    vm.focusJumpTo(114, lastTotal)
    assertShowing(vm, 114, lastTotal)
    vm.focusNextVerse()
    assertShowing(vm, 114, lastTotal)
    assertEquals(lastTotal, vm.focusVerse.value?.number)
  }

  // --- 6. Session restore and bookmark integrity ---

  @Test
  fun `closing focus keeps bookmarks and resumes the same position in a new session`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(3)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 200 }
    vm.toggleBookmark(3, 8)
    awaitSettled {
      vm.bookmarkedVerses.value.any { it.first.number == 3 && it.second.number == 8 }
    }

    vm.enterFocusAtBookmark(3, 8)
    assertShowing(vm, 3, 8)
    assertEquals(
      "The bookmarked Ayat must keep its bookmark flag in Focus",
      true,
      vm.focusVerse.value?.isBookmarked
    )
    vm.focusNextVerse()
    assertShowing(vm, 3, 9)
    vm.exitFocus()

    assertTrue("Focus must not clear the bookmark", prefs().getBoolean("bm_3_8", false))
    assertEquals(3, prefs().getInt("focus_surah", -1))
    assertEquals(9, prefs().getInt("focus_verse", -1))

    // A fresh ViewModel in the same session resumes the persisted position.
    val resumed = NoorUpViewModel(app())
    resumed.toggleAmbientFocus()
    assertShowing(resumed, 3, 9)

    // Explicit navigation still overrides the restored session.
    resumed.enterFocusAtBookmark(2, 88)
    assertShowing(resumed, 2, 88)

    resumed.refreshBookmarkedVerses()
    assertTrue(
      "Closing and reopening Focus must keep the bookmark list intact",
      awaitSettled {
        resumed.bookmarkedVerses.value.any { it.first.number == 3 && it.second.number == 8 }
      }
    )
  }

  // --- 7. Reader position, reader jumps and "Continue reading" ---

  @Test
  fun `reader jump validates input and publishes a one-shot scroll target`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(2)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 286 }
    assertNull("A normal open must not request a scroll", vm.pendingReaderAyat.value)

    assertNull("A valid reader jump must be accepted", vm.goToAyatInReader(2, "88"))
    assertEquals(88, vm.pendingReaderAyat.value)
    assertEquals(2 to 88, vm.readerResume.value)
    assertEquals(2, prefs().getInt("reader_last_surah", -1))
    assertEquals(88, prefs().getInt("reader_last_verse", -1))

    vm.consumeReaderAyatJump()
    assertNull("The scroll request must be consumed once", vm.pendingReaderAyat.value)
  }

  @Test
  fun `reader jump rejects invalid input without moving the reading position`() {
    val vm = NoorUpViewModel(app())
    vm.openSurah(2)
    awaitSettled { !vm.isQuranLoading.value }
    assertEquals(2 to 1, vm.readerResume.value)

    assertEquals(
      NoorUpViewModel.AyatInputError.OUT_OF_RANGE,
      vm.goToAyatInReader(2, "287")
    )
    assertEquals(
      NoorUpViewModel.AyatInputError.NOT_A_WHOLE_NUMBER,
      vm.goToAyatInReader(2, "8.5")
    )
    assertNull(vm.pendingReaderAyat.value)
    assertEquals(2 to 1, vm.readerResume.value)
  }

  @Test
  fun `opening a surah at an ayat persists a resume position for the next session`() {
    val vm = NoorUpViewModel(app())
    vm.openSurahAt(3, 8)
    awaitSettled { !vm.isQuranLoading.value && vm.selectedSurahVerses.value.size == 200 }
    assertEquals(8, vm.pendingReaderAyat.value)
    assertEquals(3 to 8, vm.readerResume.value)

    // A fresh ViewModel (app restart) offers the same "Continue reading" spot.
    val resumed = NoorUpViewModel(app())
    assertEquals(3 to 8, resumed.readerResume.value)

    // Opening a Surah normally starts its resume position at Ayat 1 again.
    resumed.openSurah(112)
    awaitSettled { !resumed.isQuranLoading.value }
    assertNull(resumed.pendingReaderAyat.value)
    assertEquals(112 to 1, resumed.readerResume.value)
  }

  @Test
  fun `jumping does not clear the focus session read count`() {
    val vm = NoorUpViewModel(app())
    vm.enterFocusAtSurahStart(2)
    assertShowing(vm, 2, 1)
    vm.focusNextVerse()
    awaitSettled { vm.focusSessionCount.value == 1 }

    assertNull(vm.goToAyat(2, "88"))
    assertShowing(vm, 2, 88)
    assertTrue(
      "Jumping is navigation, not a new session",
      vm.focusSessionCount.value >= 1
    )
  }
}
