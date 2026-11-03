package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.QuickIndex
import com.example.ui.noorup.backup.BackupCodec
import com.example.ui.noorup.quran.JuzIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Favourite Surahs (ID-only, separate from Ayat bookmarks) and the Surah / Para quick index maths. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuranFavoritesTest {

    private fun app(): Application = ApplicationProvider.getApplicationContext()
    private fun prefs() = app().getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)

    @Test
    fun `toggle adds then removes a favourite surah`() {
        val vm = NoorUpViewModel(app())
        assertTrue(vm.favoriteSurahs.value.isEmpty())
        vm.toggleFavoriteSurah(18)
        vm.toggleFavoriteSurah(55)
        assertEquals(setOf(18, 55), vm.favoriteSurahs.value)
        vm.toggleFavoriteSurah(18)
        assertEquals(setOf(55), vm.favoriteSurahs.value)
    }

    @Test
    fun `favourites persist across a new view model (app restart)`() {
        NoorUpViewModel(app()).apply {
            toggleFavoriteSurah(20)
            toggleFavoriteSurah(65)
        }
        val restarted = NoorUpViewModel(app())
        assertEquals(setOf(20, 65), restarted.favoriteSurahs.value)
        restarted.toggleFavoriteSurah(20)
        assertEquals(setOf(65), NoorUpViewModel(app()).favoriteSurahs.value)
    }

    @Test
    fun `stores only the surah id as a boolean and ignores out of range numbers`() {
        val vm = NoorUpViewModel(app())
        vm.toggleFavoriteSurah(2)
        vm.toggleFavoriteSurah(0)
        vm.toggleFavoriteSurah(115)
        assertEquals(setOf(2), vm.favoriteSurahs.value)
        assertTrue(prefs().getBoolean("fav_surah_2", false))
        assertFalse(prefs().contains("fav_surah_0"))
        assertFalse(prefs().contains("fav_surah_115"))
    }

    private fun quranKeys() = prefs().all.filterKeys {
        it.startsWith("bm_") || it.startsWith("mem_") || it.startsWith("reader_last_") || it.startsWith("focus_")
    }

    @Test
    fun `favourites never touch ayat bookmarks or reading progress`() {
        prefs().edit().putBoolean("bm_2_255", true).putInt("reader_last_2", 40).apply()
        val vm = NoorUpViewModel(app())
        val before = quranKeys()
        vm.toggleFavoriteSurah(2)
        vm.toggleFavoriteSurah(2)
        vm.toggleFavoriteSurah(36)
        assertEquals(before, quranKeys())
        assertTrue(prefs().getBoolean("bm_2_255", false))
        assertFalse(vm.favoriteSurahs.value.contains(255))
    }

    @Test
    fun `favourites are part of backup and restore`() {
        assertTrue(BackupCodec.isBackedUp("fav_surah_18"))
        assertTrue(BackupCodec.snapshot(mapOf("fav_surah_18" to true)).prefs.containsKey("fav_surah_18"))
    }

    @Test
    fun `quick index maps rail position to every surah and para`() {
        for (count in listOf(114, 30)) {
            assertEquals(1, QuickIndex.entryAt(0f, count))
            assertEquals(count, QuickIndex.entryAt(1f, count))
            assertEquals(1, QuickIndex.entryAt(-5f, count))
            assertEquals(count, QuickIndex.entryAt(5f, count))
            val reached = (0..2000).map { QuickIndex.entryAt(it / 2000f, count) }.toSet()
            assertEquals((1..count).toSet(), reached)
            for (n in 1..count) assertEquals(n, QuickIndex.entryAt(QuickIndex.fractionOf(n, count), count))
        }
    }

    @Test
    fun `rail markers stay inside range and include the first and last entry`() {
        val surah = QuickIndex.markers(114, 20)
        assertEquals(listOf(1, 20, 40, 60, 80, 100, 114), surah)
        val juz = QuickIndex.markers(30, 5)
        assertEquals(listOf(1, 5, 10, 15, 20, 25, 30), juz)
    }

    @Test
    fun `all 30 paras are listed in order`() {
        assertEquals((1..30).toList(), JuzIndex.STARTS.map { it.juz })
    }
}
