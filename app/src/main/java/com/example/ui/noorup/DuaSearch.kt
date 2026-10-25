package com.example.ui.noorup

import java.text.Normalizer
import java.util.Locale

/**
 * Pure, deterministic search/filter logic for Masnoon Duas.
 *
 * The UI used to compare the selected category label with dataset labels that
 * did not match (for example "ক্ষমা ও তাওবা" vs "ক্ষমা ও তওবা"), and kept the
 * Bengali label after switching to English. This utility uses a stable
 * category key derived from the dataset, searches every user-visible field
 * plus optional tags, and only folds text for comparison — displayed Arabic
 * diacritics and original wording are never modified.
 */
object DuaSearch {

    const val ALL_CATEGORY_KEY = "__all__"

    data class Category(
        val key: String,
        val labelBangla: String,
        val labelEnglish: String
    ) {
        fun label(isEnglish: Boolean): String =
            if (isEnglish) labelEnglish.ifBlank { labelBangla }
            else labelBangla.ifBlank { labelEnglish }
    }

    fun allCategory(): Category = Category(ALL_CATEGORY_KEY, "সকল", "All")

    /** Categories are derived from the actual dataset, never a hardcoded list. */
    fun categories(duas: List<DuaItem>): List<Category> {
        val seen = LinkedHashMap<String, Category>()
        for (dua in duas) {
            val key = canonicalCategoryKey(dua)
            if (key.isBlank()) continue
            val dedupe = key.lowercase(Locale.ROOT)
            if (!seen.containsKey(dedupe)) {
                seen[dedupe] = Category(
                    key = key,
                    labelBangla = dua.categoryBangla.trim().ifBlank { key },
                    labelEnglish = dua.categoryEnglish.trim().ifBlank { dua.categoryBangla.trim() }
                )
            }
        }
        val sorted = seen.values.sortedBy { it.labelEnglish.lowercase(Locale.ROOT) }
        return listOf(allCategory()) + sorted
    }

    fun canonicalCategoryKey(dua: DuaItem): String =
        dua.categoryEnglish.trim().ifBlank { dua.categoryBangla.trim() }

    /**
     * Trim, collapse all Unicode whitespace runs, normalize compatibility
     * forms, remove Arabic-only diacritics/tatweel, unify common alef/ya
     * variants, and lowercase for case-insensitive matching.
     *
     * This is a search key only. The original strings shown to the user are
     * returned untouched by [filter].
     */
    fun foldForSearch(raw: String): String {
        if (raw.isEmpty()) return ""
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFC)
        val out = StringBuilder(normalized.length)
        var i = 0
        while (i < normalized.length) {
            val cp = normalized.codePointAt(i)
            val charCount = Character.charCount(cp)
            when {
                // Arabic combining marks (harakat, Quranic annotation marks).
                cp in 0x0610..0x061A ||
                    cp in 0x064B..0x065F ||
                    cp == 0x0670 ||
                    cp in 0x06D6..0x06ED ||
                    cp in 0x08D3..0x08FF -> Unit

                cp == 0x0640 -> Unit // tatweel

                // **NEW**: Zero Width Non-Joiner and Zero Width Space (unify variants)
                cp == 0x200C -> Unit // ZWNJ
                cp == 0x200B -> Unit // ZWSP

                // Common letter variants: search should not require exact
                // hamza/maksura spelling. Display text is unchanged.
                cp == 0x0622 || cp == 0x0623 || cp == 0x0625 -> out.append('\u0627')
                cp == 0x0649 -> out.append('\u064A')
                cp == 0x0629 -> out.append('\u0647')

                // Arabic-Indic and Bengali digits -> ASCII digits.
                cp in 0x0660..0x0669 -> out.append(('0'.code + (cp - 0x0660)))
                cp in 0x06F0..0x06F9 -> out.append(('0'.code + (cp - 0x06F0)))
                cp in 0x09E6..0x09EF -> out.append(('0'.code + (cp - 0x09E6)))

                Character.isWhitespace(cp) -> out.append(' ')
                else -> out.appendCodePoint(cp)
            }
            i += charCount
        }
        return out.toString()
            .replace(Regex(" +"), " ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    fun matchesQuery(dua: DuaItem, rawQuery: String): Boolean {
        val query = foldForSearch(rawQuery)
        if (query.isBlank()) return true
        val haystack = buildString {
            append(dua.titleBangla).append(' ')
            append(dua.titleEnglish).append(' ')
            append(dua.categoryBangla).append(' ')
            append(dua.categoryEnglish).append(' ')
            append(dua.reference).append(' ')
            append(dua.arabicText).append(' ')
            append(dua.phoneticBangla).append(' ')
            append(dua.phoneticEnglish).append(' ')
            append(dua.meaningBangla).append(' ')
            append(dua.meaningEnglish)
            for (tag in dua.searchTags) append(' ').append(tag)
        }.let(::foldForSearch)
        // Every whitespace-separated token must match, so "anxiety debt"
        // works even when the two words occur in different fields/tags.
        return query.split(' ').filter { it.isNotBlank() }.all { haystack.contains(it) }
    }

    fun matchesCategory(dua: DuaItem, categoryKey: String): Boolean {
        val key = categoryKey.trim()
        if (key.isBlank() || key == ALL_CATEGORY_KEY) return true
        return key == canonicalCategoryKey(dua) ||
            key.equals(dua.categoryEnglish.trim(), ignoreCase = true) ||
            key == dua.categoryBangla.trim()
    }

    /**
     * Category and query are intersected. Input order is preserved so results
     * are deterministic for a given dataset.
     */
    fun filter(
        duas: List<DuaItem>,
        rawQuery: String,
        categoryKey: String = ALL_CATEGORY_KEY
    ): List<DuaItem> = duas.filter { dua ->
        matchesCategory(dua, categoryKey) && matchesQuery(dua, rawQuery)
    }
}
