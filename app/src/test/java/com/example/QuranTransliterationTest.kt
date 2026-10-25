package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.Verse
import com.example.ui.noorup.quran.QuranRepository
import com.example.ui.noorup.quran.QuranTransliterationRepository
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
import org.robolectric.annotation.Config

/**
 * Validates the bundled Bengali pronunciation without inventing or altering
 * Quranic text. Source/license: assets/quran/TRANSLITERATION_SOURCE.txt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuranTransliterationTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `bundle loads with source metadata and all 6236 entries`() {
        val bundle = QuranTransliterationRepository.load(context())
        assertNotNull(bundle)
        bundle!!
        assertEquals("Khalidhassan3011/al_quran", bundle.source)
        assertEquals("MIT", bundle.license)
        assertTrue(bundle.copyright.contains("Khalid Hassan"))
        assertEquals(114, bundle.surahs.size)
        assertEquals(6236, bundle.surahs.values.sumOf { it.size })
    }

    @Test
    fun `exact surah and ayah mapping is preserved`() {
        val bundle = QuranTransliterationRepository.load(context())!!
        assertEquals(
            "বিছমিল্লাহির রাহমানির রাহিম।",
            bundle.surahs.getValue(1)[0]
        )
        assertTrue(
            bundle.surahs.getValue(2)[254].startsWith("আল্লা-হু লাইলা-হা ইল্লা-হুওয়া")
        )
        assertEquals("মিনাল জিন্নাতি ওয়ান্না-ছ।", bundle.surahs.getValue(114)[5])
    }

    @Test
    fun `two upstream blanks stay blank and are not fabricated`() {
        val bundle = QuranTransliterationRepository.load(context())!!
        val surah26 = bundle.surahs.getValue(26)
        assertEquals(227, surah26.size)
        assertTrue("26:1 must remain blank", surah26[0].isBlank())
        assertTrue("26:227 must remain blank", surah26[226].isBlank())
        val allEntries = bundle.surahs.values.flatten()
        assertEquals(2, allEntries.count { it.isBlank() })
        assertFalse(
            "Pronunciation must not contain Arabic script",
            allEntries.any { Regex("[\\u0600-\\u06FF]").containsMatchIn(it) }
        )
    }

    @Test
    fun `loader attaches pronunciation without changing Quran text`() {
        val context = context()
        val verses = runBlocking(Dispatchers.IO) {
            QuranRepository.loadFullSurah(context, 1)
        }
        assertEquals(7, verses.size)

        // Compare against the untouched asset, including the BOM and all
        // Arabic diacritics. This proves pronunciation attachment is additive.
        val raw = context.assets.open("quran/surah_001.json").bufferedReader(Charsets.UTF_8)
            .use { reader ->
                com.google.gson.JsonParser.parseString(reader.readText())
                    .asJsonObject.getAsJsonArray("verses")[0].asJsonObject
            }
        assertEquals(raw.get("arabic").asString, verses[0].arabicText)
        assertEquals(raw.get("bangla").asString, verses[0].banglaTranslation)
        assertEquals(raw.get("english").asString, verses[0].englishTranslation)
        assertEquals("বিছমিল্লাহির রাহমানির রাহিম।", verses[0].banglaTransliteration)
    }

    @Test
    fun `surah 26 missing entries become null while other ayat keep text`() {
        val context = context()
        val verses = runBlocking(Dispatchers.IO) {
            QuranRepository.loadFullSurah(context, 26)
        }
        assertEquals(227, verses.size)
        assertNull(verses.first { it.number == 1 }.banglaTransliteration)
        assertNull(verses.first { it.number == 227 }.banglaTransliteration)
        val verse2 = verses.first { it.number == 2 }
        assertNotNull(verse2.banglaTransliteration)
        assertTrue(verse2.banglaTransliteration!!.isNotBlank())
        assertTrue(verse2.arabicText.isNotBlank())
        assertTrue(verse2.banglaTranslation.isNotBlank())
        assertTrue(verse2.englishTranslation.isNotBlank())
    }

    @Test
    fun `attach with no bundle clears pronunciation only`() {
        val verses = listOf(
            Verse(1, "arabic-1", "bangla-1", "english-1", banglaTransliteration = "old"),
            Verse(2, "arabic-2", "bangla-2", "english-2", banglaTransliteration = "old")
        )
        val attached = QuranTransliterationRepository.attach(verses, 1, null)
        assertEquals(verses.map { it.arabicText }, attached.map { it.arabicText })
        assertEquals(verses.map { it.banglaTranslation }, attached.map { it.banglaTranslation })
        assertEquals(verses.map { it.englishTranslation }, attached.map { it.englishTranslation })
        assertTrue(attached.all { it.banglaTransliteration == null })
    }

    @Test
    fun `attach refuses a misaligned list instead of shifting ayat`() {
        val bundle = QuranTransliterationRepository.load(context())!!
        val verses = listOf(Verse(1, "a", "b", "c"))
        val attached = QuranTransliterationRepository.attach(verses, 1, bundle)
        assertNull(attached.single().banglaTransliteration)
    }

    @Test
    fun `validation rejects partial bundles and wrong schema`() {
        val valid = QuranTransliterationRepository.load(context())!!
        val asAsset = QuranTransliterationRepository.Asset(
            schemaVersion = 1,
            source = valid.source,
            license = valid.license,
            copyright = valid.copyright,
            surahs = valid.surahs.mapKeys { it.key.toString() }
                .mapValues { it.value }
        )
        assertNotNull(QuranTransliterationRepository.parseAndValidate(asAsset))
        assertNull(
            QuranTransliterationRepository.parseAndValidate(
                asAsset.copy(schemaVersion = 99)
            )
        )
        assertNull(
            "Missing one surah must invalidate the whole bundle",
            QuranTransliterationRepository.parseAndValidate(
                asAsset.copy(surahs = asAsset.surahs - "114")
            )
        )
        val short = asAsset.surahs.toMutableMap()
        short["1"] = short.getValue("1").dropLast(1)
        assertNull(
            "A wrong ayah count must invalidate the whole bundle",
            QuranTransliterationRepository.parseAndValidate(asAsset.copy(surahs = short))
        )
    }
}
