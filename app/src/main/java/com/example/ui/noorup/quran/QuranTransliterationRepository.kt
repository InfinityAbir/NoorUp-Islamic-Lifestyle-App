package com.example.ui.noorup.quran

import android.content.Context
import android.util.Log
import com.example.ui.noorup.Verse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Bundled Bengali pronunciation for the Quran.
 *
 * The asset contains only the `pronunciation_bn` strings from the MIT-licensed
 * Khalidhassan3011/al_quran dataset, keyed by surah and ayah. It is validated
 * against NoorUp's existing surah metadata before any string is shown. If the
 * bundle is missing, partial, or misaligned, this repository returns no
 * pronunciation data at all — it never invents or reorders religious text.
 *
 * Source and license details are shipped in
 * `assets/quran/TRANSLITERATION_SOURCE.txt`.
 */
object QuranTransliterationRepository {

    const val ASSET_PATH = "quran/transliteration_bn.json"
    private const val TAG = "QuranTransliteration"
    private const val EXPECTED_SCHEMA_VERSION = 1

    data class Bundle(
        val source: String,
        val license: String,
        val copyright: String,
        val surahs: Map<Int, List<String>>
    )

    internal data class Asset(
        val schemaVersion: Int = 0,
        val source: String = "",
        val sourceFile: String = "",
        val sourceCommit: String = "",
        val sourceUrl: String = "",
        val license: String = "",
        val copyright: String = "",
        val note: String = "",
        val surahs: Map<String, List<String>> = emptyMap()
    )

    private val gson = Gson()
    private val assetType = object : TypeToken<Asset>() {}.type

    @Volatile
    private var cached: Bundle? = null

    @Volatile
    private var loadAttempted = false

    /** Returns validated pronunciation data, or null when it cannot be trusted. */
    fun load(context: Context): Bundle? {
        cached?.let { return it }
        if (loadAttempted) return cached
        synchronized(this) {
            cached?.let { return it }
            if (loadAttempted) return cached
            loadAttempted = true
            cached = try {
                context.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { reader ->
                    parseAndValidate(gson.fromJson(reader, assetType))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not load $ASSET_PATH", e)
                null
            }
            return cached
        }
    }

    /**
     * Pure validation used by the loader and by tests. A bundle is accepted only
     * when it contains exactly the 114 known surahs, every list length matches
     * the canonical ayah count, and there are no extra/misaligned entries.
     */
    internal fun parseAndValidate(asset: Asset?): Bundle? {
        if (asset == null || asset.schemaVersion != EXPECTED_SCHEMA_VERSION) return null
        if (asset.surahs.size != 114) return null

        val mapped = LinkedHashMap<Int, List<String>>(114)
        for (surah in QuranRepository.all114Surahs) {
            val values = asset.surahs[surah.number.toString()] ?: return null
            if (values.size != surah.totalVerses) return null
            mapped[surah.number] = values
        }
        if (mapped.size != 114) return null

        return Bundle(
            source = asset.source,
            license = asset.license,
            copyright = asset.copyright,
            surahs = mapped
        )
    }

    /**
     * Attaches pronunciation to a validated verse list. Missing source entries
     * become null so the UI can show an explicit unavailable message.
     */
    fun attach(
        verses: List<Verse>,
        surahNumber: Int,
        bundle: Bundle?
    ): List<Verse> {
        val values = bundle?.surahs?.get(surahNumber) ?: return verses.map {
            it.copy(banglaTransliteration = null)
        }
        if (values.size != verses.size) return verses.map {
            it.copy(banglaTransliteration = null)
        }
        return verses.mapIndexed { index, verse ->
            val pronunciation = values[index].trim()
            verse.copy(banglaTransliteration = pronunciation.ifBlank { null })
        }
    }
}
