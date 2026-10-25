package com.example

import com.example.ui.noorup.DuaSearch
import com.example.ui.noorup.NoorUpRepository
import com.example.ui.noorup.DuaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure regression tests for the Dua search/filter pipeline.
 *
 * These cover the original failure modes: stale hardcoded category labels,
 * language switches that emptied the list, untrimmed whitespace, missing
 * category-name search, missing tag search, and category+query intersection.
 */
class DuaSearchTest {

    private val duas = NoorUpRepository.duas

    private fun idsFor(query: String, category: String = DuaSearch.ALL_CATEGORY_KEY): List<String> =
        DuaSearch.filter(duas, query, category).map { it.id }

    @Test
    fun `dataset has the expected eighteen duas`() {
        assertEquals(18, duas.size)
        assertEquals(18, duas.map { it.id }.distinct().size)
    }

    @Test
    fun `categories are derived from the dataset with stable unique keys`() {
        val categories = DuaSearch.categories(duas)
        assertEquals(DuaSearch.ALL_CATEGORY_KEY, categories.first().key)
        assertEquals("সকল", categories.first().labelBangla)
        assertEquals("All", categories.first().labelEnglish)
        assertEquals(
            "Category keys must be unique",
            categories.size,
            categories.map { it.key }.distinct().size
        )
        assertTrue(categories.any { it.key == "Forgiveness & Repentance" })
        assertTrue(categories.any { it.key == "Anxiety & Distress" })
        assertTrue(categories.any { it.key == "Distress & Anxiety" })
        // The old UI hardcoded "ক্ষমা ও তাওবা"; the dataset says "ক্ষমা ও তওবা".
        assertTrue(categories.any {
            it.key == "Forgiveness & Repentance" && it.labelBangla == "ক্ষমা ও তওবা"
        })
    }

    @Test
    fun `empty query and all category preserve dataset order`() {
        val all = DuaSearch.filter(duas, "", DuaSearch.ALL_CATEGORY_KEY)
        assertEquals(duas.map { it.id }, all.map { it.id })
        assertEquals(duas.map { it.id }, idsFor("   "))
    }

    @Test
    fun `english search is case insensitive and trims and collapses whitespace`() {
        val ids = idsFor("  ANXIETY    DEBT ")
        assertTrue("Expected the anxiety/debt dua, got $ids", ids.contains("dua_anxiety"))
    }

    @Test
    fun `bengali search finds title and category text`() {
        assertTrue(idsFor("ক্ষমা").contains("dua_forgiveness"))
        assertTrue(idsFor("কষ্ট ও পেরেশানি").contains("dua_anxiety"))
    }

    @Test
    fun `arabic search folds diacritics only for comparison and preserves display text`() {
        val withDiacritics = DuaSearch.filter(duas, "اللَّهُمَّ", DuaSearch.ALL_CATEGORY_KEY)
        val withoutDiacritics = DuaSearch.filter(duas, "اللهم", DuaSearch.ALL_CATEGORY_KEY)
        assertTrue(withDiacritics.isNotEmpty())
        assertTrue(withoutDiacritics.isNotEmpty())
        assertEquals(
            withDiacritics.map { it.id },
            withoutDiacritics.map { it.id }
        )
        val anxiety = withDiacritics.first { it.id == "dua_anxiety" }
        assertSame(duas.first { it.id == "dua_anxiety" }, anxiety)
        assertTrue(
            "Displayed Arabic diacritics must be untouched",
            anxiety.arabicText.contains("اللَّهُمَّ")
        )
    }

    @Test
    fun `category names are searchable even in the all category`() {
        assertTrue(idsFor("Forgiveness").contains("dua_forgiveness"))
        assertTrue(idsFor("আখিরাত ও জান্নাত").contains("dua_jannah"))
    }

    @Test
    fun `tags expand search beyond the displayed fields`() {
        // "গাড়ি" (car) is a search tag, not the travel dua's title/meaning.
        assertTrue(idsFor("গাড়ি").contains("dua_travel"))
        assertTrue(idsFor("job").contains("dua_rizq"))
        assertTrue(idsFor("mother").contains("dua_parents"))
    }

    @Test
    fun `category and query are intersected`() {
        val matching = idsFor("ইস্তিগফার", "Forgiveness & Repentance")
        assertEquals(listOf("dua_forgiveness"), matching)
        assertTrue(
            "Travel query must not leak through the forgiveness category",
            idsFor("travel", "Forgiveness & Repentance").isEmpty()
        )
    }

    @Test
    fun `category filter uses the stable english key not the current ui language`() {
        val forgiveness = DuaSearch.filter(
            duas,
            rawQuery = "",
            categoryKey = "Forgiveness & Repentance"
        )
        assertEquals(listOf("dua_forgiveness"), forgiveness.map { it.id })
        assertEquals(
            "The Bengali dataset label must not be the selected key",
            true,
            DuaSearch.matchesCategory(
                duas.first { it.id == "dua_forgiveness" },
                "Forgiveness & Repentance"
            )
        )
        assertFalse(
            DuaSearch.matchesCategory(
                duas.first { it.id == "dua_forgiveness" },
                "Travel"
            )
        )
    }

    @Test
    fun `no result is stable and empty`() {
        assertTrue(idsFor("zzzz-no-such-dua").isEmpty())
        assertTrue(idsFor("অসম্ভব-কিছু-নেই").isEmpty())
    }

    @Test
    fun `missing optional localized text never removes a dua from its stable category`() {
        val missingBangla = DuaItem(
            id = "translation-gap", titleBangla = "", titleEnglish = "Translation gap",
            categoryBangla = "পরীক্ষা", categoryEnglish = "Verification",
            reference = "Test", arabicText = "اللَّهُمَّ", phoneticBangla = "",
            phoneticEnglish = "Allahumma", meaningBangla = "", meaningEnglish = "Verified English meaning"
        )
        val missingEnglish = missingBangla.copy(
            id = "english-gap", titleBangla = "বাংলা অনুবাদ", titleEnglish = "", meaningBangla = "যাচাইকৃত বাংলা অর্থ", meaningEnglish = ""
        )
        assertEquals(listOf("translation-gap", "english-gap"), DuaSearch.filter(listOf(missingBangla, missingEnglish), "", "Verification").map { it.id })
        assertTrue(DuaSearch.matchesQuery(missingBangla, "اللهم"))
        assertTrue(DuaSearch.matchesQuery(missingEnglish, "বাংলা"))
    }

    @Test
    fun `fold helper removes arabic diacritics but not bengali vowel signs`() {
        assertEquals("اللهم", DuaSearch.foldForSearch("اللَّهُمَّ"))
        assertEquals("ক্ষমা", DuaSearch.foldForSearch("  ক্ষমা  "))
        assertEquals("anxiety debt", DuaSearch.foldForSearch("  Anxiety   Debt "))
    }
}
