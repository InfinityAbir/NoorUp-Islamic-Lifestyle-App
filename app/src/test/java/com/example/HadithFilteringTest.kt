package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.hadith.HadithRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression coverage for the language-independent Hadith filtering contract. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HadithFilteringTest {
    private val repo = HadithRepository(ApplicationProvider.getApplicationContext<Context>())
    private val all = repo.getAllCuratedHadiths()

    @Test
    fun `all canonical books are represented in the searchable corpus`() {
        assertEquals(repo.defaultCanonicalBooks.map { it.id }.toSet(), all.map { it.bookId }.toSet())
    }

    @Test
    fun `bundled assets load into every configured collection with unique IDs`() = runBlocking {
        val loaded = mutableListOf<com.example.ui.noorup.hadith.Hadith>()
        for (book in repo.defaultCanonicalBooks) {
            loaded += repo.getHadithsForBook(book.id)
        }
        assertEquals(repo.defaultCanonicalBooks.map { it.id }.toSet(), loaded.map { it.bookId }.toSet())
        assertEquals("Hadith IDs must be unique across collection assets", loaded.size, loaded.map { it.id }.distinct().size)
        assertTrue("Every loaded record needs content in at least one display language", loaded.all {
            it.arabicText.isNotBlank() || it.translationBangla.isNotBlank() || it.translationEnglish.isNotBlank()
        })
    }

    @Test
    fun `translated and legacy topic labels resolve to the same stable topic`() {
        assertEquals("intentions", repo.canonicalTopicId("Intentions & Sincerity"))
        assertEquals("intentions", repo.canonicalTopicId("নিয়ত ও ইখলাস"))
        assertEquals("intentions", repo.canonicalTopicId("নিয়ত ও ইখলাস"))
        assertEquals("repentance", repo.canonicalTopicId("Repentance"))
        assertEquals("repentance", repo.canonicalTopicId("Repentance & Mercy"))
    }

    @Test
    fun `topic filter searches every book and does not depend on display language`() {
        val english = repo.filterHadiths(all, "", "all", "Intentions & Sincerity", 0)
        val bangla = repo.filterHadiths(all, "", "all", "নিয়ত ও ইখলাস", 0)
        assertTrue(english.isNotEmpty())
        assertEquals(english.map { it.id }, bangla.map { it.id })
        assertTrue(english.all { repo.canonicalTopicId(it.topicEn) == "intentions" || repo.canonicalTopicId(it.topicBn) == "intentions" })
    }

    @Test
    fun `collection filter is applied before the language independent search`() {
        val bukhari = repo.filterHadiths(all, "intentions", "bukhari", "all", 0)
        assertTrue(bukhari.isNotEmpty())
        assertTrue(bukhari.all { it.bookId == "bukhari" })
        assertFalse(repo.filterHadiths(all, "intentionally-impossible", "all", "all", 0).isNotEmpty())
    }

    @Test
    fun `one collection and one topic never return the unfiltered first record`() {
        val bukhariFaith = repo.filterHadiths(all, "", "bukhari", "faith", 0)
        assertTrue("Bukhari faith filter must produce matching records", bukhariFaith.isNotEmpty())
        assertTrue(bukhariFaith.all { it.bookId == "bukhari" })
        assertFalse(
            "The Revelation record must not survive a Faith filter",
            bukhariFaith.any { it.hadithNumber == 1 && it.topicEn.equals("Revelation", ignoreCase = true) }
        )
    }

    @Test
    fun `search includes arabic bengali english narrator chapter topic and reference`() {
        val hadith = all.first { it.arabicText.isNotBlank() && it.translationBangla.isNotBlank() && it.translationEnglish.isNotBlank() }
        val probes = listOf(
            hadith.arabicText.take(5), hadith.translationBangla.take(5), hadith.translationEnglish.take(5),
            hadith.narrator.take(4), hadith.chapterEn.take(4), hadith.topicEn.take(4), hadith.reference.take(4),
            hadith.hadithNumber.toString()
        ).filter { it.isNotBlank() }
        probes.forEach { probe ->
            assertTrue("Expected searchable field probe '$probe'", repo.filterHadiths(all, probe, "all", "all", 0).any { it.id == hadith.id })
        }
    }
}
