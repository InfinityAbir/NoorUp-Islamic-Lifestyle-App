package com.example.ui.noorup.search

import android.content.Context
import com.example.ui.noorup.DuaItem
import com.example.ui.noorup.DuaSearch
import com.example.ui.noorup.Verse
import com.example.ui.noorup.hadith.HadithRepository
import com.example.ui.noorup.quran.QuranRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

enum class SearchFilter { ALL, QURAN, HADITH, DUAS }

sealed interface SearchHit {
    val key: String

    data class QuranHit(
        val surah: Int,
        val ayah: Int,
        val surahNameBn: String,
        val surahNameEn: String,
        val verse: Verse,
        /** Which text matched: "arabic", "bangla", "english" or "transliteration". */
        val matchedField: String,
        val isSurahMatch: Boolean = false
    ) : SearchHit {
        override val key get() = "q:$surah:$ayah:$isSurahMatch"
    }

    data class HadithHit(
        val id: Int,
        val bookId: String,
        val bookNameBn: String,
        val bookNameEn: String,
        val number: Int,
        val narrator: String,
        val snippetEn: String,
        val snippetBn: String
    ) : SearchHit {
        override val key get() = "h:$bookId:$id"
    }

    data class DuaHit(val dua: DuaItem) : SearchHit {
        override val key get() = "d:${dua.id}"
    }

    /** Offline assistant answer preview for the query. */
    data class KnowledgeHit(val query: String, val preview: String) : SearchHit {
        override val key get() = "k:$query"
    }
}

data class SearchResults(
    val quran: List<SearchHit.QuranHit> = emptyList(),
    val hadith: List<SearchHit.HadithHit> = emptyList(),
    val duas: List<SearchHit.DuaHit> = emptyList(),
    val knowledge: SearchHit.KnowledgeHit? = null,
    val hadithIndexing: Boolean = false
) {
    val isEmpty get() = quran.isEmpty() && hadith.isEmpty() && duas.isEmpty() && knowledge == null
}

/** In-memory Quran index: folded Arabic / Bangla / English / transliteration per ayah. */
object QuranSearchIndex {
    private class Entry(
        val surah: Int,
        val verse: Verse,
        val ar: String,
        val bn: String,
        val en: String,
        val tr: String
    ) {
        val all: String = "$ar $bn $en $tr"
    }

    private val mutex = Mutex()
    @Volatile private var entries: List<Entry>? = null

    private suspend fun ensure(context: Context): List<Entry> {
        entries?.let { return it }
        return mutex.withLock {
            entries ?: withContext(Dispatchers.Default) {
                val out = ArrayList<Entry>(6300)
                for (meta in QuranRepository.all114Surahs) {
                    val verses = QuranRepository.loadFullSurah(context, meta.number)
                    for (v in verses) {
                        out += Entry(
                            meta.number, v,
                            DuaSearch.foldForSearch(v.arabicText),
                            DuaSearch.foldForSearch(v.banglaTranslation),
                            DuaSearch.foldForSearch(v.englishTranslation),
                            DuaSearch.foldForSearch(v.banglaTransliteration.orEmpty())
                        )
                    }
                }
                out
            }.also { entries = it }
        }
    }

    private val refPattern = Regex("^(\\d{1,3})\\s*[:.]\\s*(\\d{1,3})$")

    suspend fun search(context: Context, rawQuery: String, limit: Int): List<SearchHit.QuranHit> {
        val q = DuaSearch.foldForSearch(rawQuery)
        if (q.isBlank()) return emptyList()
        val all = ensure(context)
        val hits = ArrayList<SearchHit.QuranHit>()

        refPattern.matchEntire(q)?.let { m ->
            val s = m.groupValues[1].toInt()
            val a = m.groupValues[2].toInt()
            all.firstOrNull { it.surah == s && it.verse.number == a }?.let { e ->
                val meta = QuranRepository.getSurahMeta(s)
                hits += SearchHit.QuranHit(s, a, meta?.nameBangla.orEmpty(), meta?.nameEnglish.orEmpty(), e.verse, "reference")
            }
        }

        // Surah-name matches (open the surah at ayah 1).
        if (q.length >= 3) {
            for (meta in QuranRepository.all114Surahs) {
                val names = DuaSearch.foldForSearch("${meta.nameEnglish} ${meta.nameBangla} ${meta.nameArabic}")
                if (names.contains(q) && hits.size < 4) {
                    all.firstOrNull { it.surah == meta.number && it.verse.number == 1 }?.let { e ->
                        hits += SearchHit.QuranHit(meta.number, 1, meta.nameBangla, meta.nameEnglish, e.verse, "surah", isSurahMatch = true)
                    }
                }
            }
        }

        val tokens = q.split(' ').filter { it.isNotBlank() }
        if (tokens.isNotEmpty()) {
            for (e in all) {
                if (hits.size >= limit) break
                if (tokens.all { e.all.contains(it) }) {
                    val first = tokens.first()
                    val field = when {
                        e.bn.contains(first) -> "bangla"
                        e.en.contains(first) -> "english"
                        e.tr.contains(first) -> "transliteration"
                        else -> "arabic"
                    }
                    val meta = QuranRepository.getSurahMeta(e.surah)
                    hits += SearchHit.QuranHit(
                        e.surah, e.verse.number, meta?.nameBangla.orEmpty(), meta?.nameEnglish.orEmpty(), e.verse, field
                    )
                }
            }
        }
        return hits.distinctBy { it.key }.take(limit)
    }
}

/**
 * Compact on-disk hadith index. The 8 collections are ~110 MB of JSON, far too big to hold in memory
 * or re-parse per keystroke, so each book is parsed once into a small TSV
 * (id, number, narrator, snippet, folded searchable text) in the cache dir and scanned line by line.
 */
object HadithSearchIndex {
    private const val DIR = "hadith_search_v2"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _progress = MutableStateFlow(0 to 0)
    /** (booksReady, totalBooks) while indexing; (n, n) when done. */
    val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()
    @Volatile private var building = false

    private fun file(context: Context, bookId: String) = File(File(context.cacheDir, DIR).apply { mkdirs() }, "$bookId.tsv")

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()

    fun ensureBuilt(context: Context, repo: HadithRepository) {
        val books = repo.defaultCanonicalBooks
        val app = context.applicationContext
        val ready = books.count { file(app, it.id).exists() }
        _progress.value = ready to books.size
        if (ready == books.size || building) return
        building = true
        scope.launch {
            try {
                for (book in books) {
                    val out = file(app, book.id)
                    if (out.exists()) continue
                    try {
                        val hadiths = repo.getHadithsForBook(book.id)
                        val tmp = File(out.parentFile, book.id + ".tmp")
                        tmp.bufferedWriter(Charsets.UTF_8).use { w ->
                            for (h in hadiths) {
                                val snipEn = clean(h.translationEnglish.ifBlank { h.translationBangla }).take(180)
                                val snipBn = clean(h.translationBangla.ifBlank { h.translationEnglish }).take(180)
                                val folded = DuaSearch.foldForSearch(
                                    clean(
                                        listOf(
                                            h.narrator, h.chapterBn, h.chapterEn, h.topicEn, h.reference,
                                            h.translationEnglish, h.translationBangla, h.hadithNumber.toString()
                                        ).joinToString(" ")
                                    )
                                )
                                w.write("${h.id}\t${h.hadithNumber}\t${clean(h.narrator)}\t$snipEn\t$snipBn\t$folded\n")
                            }
                        }
                        tmp.renameTo(out)
                    } catch (_: Exception) {
                        // Leave this book unindexed; it is retried next time search opens.
                    }
                    _progress.value = books.count { file(app, it.id).exists() } to books.size
                }
            } finally {
                building = false
            }
        }
    }

    fun search(context: Context, repo: HadithRepository, rawQuery: String, limit: Int): List<SearchHit.HadithHit> {
        val tokens = DuaSearch.foldForSearch(rawQuery).split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        val books = repo.defaultCanonicalBooks
        val perBook = maxOf(3, limit / books.size + 1)
        val out = ArrayList<SearchHit.HadithHit>()
        for (book in books) {
            val f = file(context, book.id)
            if (!f.exists()) continue
            var found = 0
            f.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val p = line.split('\t', limit = 6)
                    if (p.size < 6) continue
                    if (tokens.all { p[5].contains(it) }) {
                        out += SearchHit.HadithHit(
                            id = p[0].toIntOrNull() ?: continue,
                            bookId = book.id, bookNameBn = book.nameBn, bookNameEn = book.nameEn,
                            number = p[1].toIntOrNull() ?: 0, narrator = p[2], snippetEn = p[3], snippetBn = p[4]
                        )
                        if (++found >= perBook) break
                    }
                }
            }
        }
        return out.take(limit)
    }
}

object GlobalSearchEngine {

    suspend fun search(
        context: Context,
        hadithRepo: HadithRepository,
        duas: List<DuaItem>,
        query: String,
        filter: SearchFilter,
        knowledgeAnswer: (String) -> String?
    ): SearchResults = withContext(Dispatchers.Default) {
        val q = query.trim()
        if (q.length < 2) return@withContext SearchResults()
        val wide = filter != SearchFilter.ALL
        val wantQuran = filter == SearchFilter.ALL || filter == SearchFilter.QURAN
        val wantHadith = filter == SearchFilter.ALL || filter == SearchFilter.HADITH
        val wantDuas = filter == SearchFilter.ALL || filter == SearchFilter.DUAS

        val quran = if (wantQuran) QuranSearchIndex.search(context, q, if (wide) 80 else 10) else emptyList()
        val hadith = if (wantHadith) {
            withContext(Dispatchers.IO) { HadithSearchIndex.search(context, hadithRepo, q, if (wide) 60 else 10) }
        } else emptyList()
        val duaHits = if (wantDuas) {
            DuaSearch.filter(duas, q).take(if (wide) 40 else 8).map { SearchHit.DuaHit(it) }
        } else emptyList()
        val knowledge = if (filter == SearchFilter.ALL && q.length >= 3) {
            knowledgeAnswer(q)?.let { SearchHit.KnowledgeHit(q, it) }
        } else null

        val (ready, total) = HadithSearchIndex.progress.value
        SearchResults(quran, hadith, duaHits, knowledge, hadithIndexing = wantHadith && ready < total)
    }
}
