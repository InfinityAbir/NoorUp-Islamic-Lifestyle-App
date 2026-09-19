package com.example.ui.noorup.hadith

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, offline-first Repository for parsing, caching, and serving
 * canonical Hadith books directly from Android assets folder.
 * Uses Kotlin Coroutines with Dispatchers.IO, BOM-safe UTF-8 streaming, and flexible schema
 * mapping to handle diverse JSON formats without crashes, mojibake, or OutOfMemory errors.
 */
class HadithRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Hadith::class.java, HadithJsonDeserializer())
        .setLenient()
        .create()

    // In-memory thread-safe caches for loaded books, hadiths, and chapters
    private val booksMetadataCache = ConcurrentHashMap<String, Book>()
    private val hadithsCache = ConcurrentHashMap<String, List<Hadith>>()
    private val chaptersCache = ConcurrentHashMap<String, List<Chapter>>()

    // Default canonical book definitions used for fast directory initialization
    val defaultCanonicalBooks = listOf(
        Book(
            id = "bukhari",
            nameBn = "সহীহ আল-বুখারী",
            nameEn = "Sahih al-Bukhari",
            arabicName = "صحيح البخاري",
            authorBn = "ইমাম মুহাম্মদ বিন ইসমাইল আল-বুখারী (রহ.)",
            authorEn = "Imam Muhammad al-Bukhari (RA)",
            totalHadithsBn = "৭,৫৬৩টি হাদিস",
            totalHadithsCount = 7563,
            totalChaptersBn = "৯৭টি অধ্যায়",
            authenticGradeBn = "সর্বোচ্চ বিশুদ্ধ সনদ (১ম স্থান)",
            authenticGradeEn = "Highest Authenticity Rank",
            descriptionBn = "পবিত্র কুরআনের পর সর্বাধিক বিশুদ্ধ ও প্রামাণ্য হাদিস সংকলন।",
            descriptionEn = "Regarded as the most authentic collection of Hadith following the Holy Quran.",
            assetFileName = "bukhari.json"
        ),
        Book(
            id = "muslim",
            nameBn = "সহীহ মুসলিম",
            nameEn = "Sahih Muslim",
            arabicName = "صحيح مسلم",
            authorBn = "ইমাম মুসলিম বিন আল-হাজ্জাজ (রহ.)",
            authorEn = "Imam Muslim ibn al-Hajjaj (RA)",
            totalHadithsBn = "৭,৫০০টি হাদিস",
            totalHadithsCount = 7500,
            totalChaptersBn = "৫৬টি অধ্যায়",
            authenticGradeBn = "সহীহ সনদের বিশুদ্ধ মানদণ্ড (২য় স্থান)",
            authenticGradeEn = "Strict Sahih Criterion (2nd Rank)",
            descriptionBn = "সুশৃঙ্খল বিষয়ভিত্তিক অধ্যায়বিন্যাস ও বিশুদ্ধ সনদের অনন্য সংকলন।",
            descriptionEn = "Second only to Bukhari in authenticity and rigorous collection methodology.",
            assetFileName = "muslim.json"
        ),
        Book(
            id = "nasai",
            nameBn = "সুনান আন-নাসায়ী",
            nameEn = "Sunan an-Nasa'i",
            arabicName = "سنن النسائي",
            authorBn = "ইমাম আহমদ বিন শুয়াইব আন-নাসায়ী (রহ.)",
            authorEn = "Imam Ahmad an-Nasa'i (RA)",
            totalHadithsBn = "৫,৭৫৮টি হাদিস",
            totalHadithsCount = 5758,
            totalChaptersBn = "৫০টি অধ্যায়",
            authenticGradeBn = "বর্ণনাকারী যাচাইয়ে কঠোর মানদণ্ড",
            authenticGradeEn = "Rigorous Sanad Scrutiny",
            descriptionBn = "ইবাদত ও দৈনন্দিন আহকামের অত্যন্ত সূক্ষ্ম সনদ-নিরীক্ষিত প্রামাণ্য গ্রন্থ।",
            descriptionEn = "Noted for its strict criteria in accepting narrators and assessing chains.",
            assetFileName = "nasai.json"
        ),
        Book(
            id = "abudawud",
            nameBn = "সুনান আবু দাউদ",
            nameEn = "Sunan Abu Dawud",
            arabicName = "سنن أبي داود",
            authorBn = "ইমাম আবু দাউদ সুলাইমান আস-সিজিস্তানী (রহ.)",
            authorEn = "Imam Abu Dawud (RA)",
            totalHadithsBn = "৫,২৭৪টি হাদিস",
            totalHadithsCount = 5274,
            totalChaptersBn = "৪৩টি অধ্যায়",
            authenticGradeBn = "ফিকহি আহকামের নির্ভরযোগ্য দলিল",
            authenticGradeEn = "Primary Source of Legal Rulings",
            descriptionBn = "ইসলামি আইনশাস্ত্র, হালাল-হারাম ও ব্যবহারিক বিধান সংবলিত সুবিখ্যাত গ্রন্থ।",
            descriptionEn = "Focuses heavily on legal rulings (fiqh), jurisprudence, and daily ethics.",
            assetFileName = "abudawud.json"
        ),
        Book(
            id = "tirmidhi",
            nameBn = "জামে' আত-তিরমিযী",
            nameEn = "Jami' at-Tirmidhi",
            arabicName = "جامع الترمذي",
            authorBn = "ইমাম আবু ঈসা মুহাম্মদ আত-তিরমিযী (রহ.)",
            authorEn = "Imam Abu 'Isa at-Tirmidhi (RA)",
            totalHadithsBn = "৩,৯৫৬টি হাদিস",
            totalHadithsCount = 3956,
            totalChaptersBn = "৫০টি অধ্যায়",
            authenticGradeBn = "সহীহ-হাসান ও ফিকহি তাহকীক",
            authenticGradeEn = "Sahih & Hasan Fiqh Analysis",
            descriptionBn = "হাদিসের মান নির্ধারণ এবং বিভিন্ন ফিকহি মাযহাবের তুলনামূলক বিশ্লেষণে সমৃদ্ধ।",
            descriptionEn = "Valuable for containing legal views of various schools of thought and grading classifications.",
            assetFileName = "tirmidhi.json"
        ),
        Book(
            id = "ibnmajah",
            nameBn = "সুনান ইবনে মাজাহ",
            nameEn = "Sunan Ibn Majah",
            arabicName = "سنن ابن ماجه",
            authorBn = "ইমাম মুহাম্মদ ইবনে মাজাহ আল-কাজবিনী (রহ.)",
            authorEn = "Imam Ibn Majah al-Qazwini (RA)",
            totalHadithsBn = "৪,৩৪১টি হাদিস",
            totalHadithsCount = 4341,
            totalChaptersBn = "৩৭টি অধ্যায়",
            authenticGradeBn = "সিহাহ সিত্তাহর ষষ্ঠতম প্রামাণ্য গ্রন্থ",
            authenticGradeEn = "Sixth Canonical Collection",
            descriptionBn = "চমৎকার অধ্যায়বিন্যাস, প্রাঞ্জল বিন্যাস এবং একক গুরুত্বপূর্ণ সনদের সমন্বয়।",
            descriptionEn = "The sixth of the primary canonical books, noted for excellent organization and rare narrations.",
            assetFileName = "ibnmajah.json"
        ),
        Book(
            id = "muwatta",
            nameBn = "মুয়াত্তা ইমাম মালিক",
            nameEn = "Muwatta Malik",
            arabicName = "موطأ الإمام مالك",
            authorBn = "ইমাম মালিক ইবনে আনাস (রহ.)",
            authorEn = "Imam Malik ibn Anas (RA)",
            totalHadithsBn = "১,৮৫৮টি হাদিস ও আসার",
            totalHadithsCount = 1858,
            totalChaptersBn = "৬১টি অধ্যায়",
            authenticGradeBn = "প্রথম লিখিত প্রামাণ্য হাদিস সংকলন",
            authenticGradeEn = "Earliest Foundational Compilation",
            descriptionBn = "মদিনার সোনালী সনদ (সিলসিলাতুজ জাহাব) ও ইসলামি আইনশাস্ত্রের প্রাচীনতম বিশ্বস্ত স্তম্ভ।",
            descriptionEn = "One of the earliest written collections of Hadith and Islamic law with pristine chains.",
            assetFileName = "muwatta.json"
        ),
        Book(
            id = "riyadussalihin",
            nameBn = "রিয়াদুস সালেহীন",
            nameEn = "Riyad as-Salihin",
            arabicName = "رياض الصالحين",
            authorBn = "ইমাম মুহিউদ্দীন ইয়াহইয়া আন-নববী (রহ.)",
            authorEn = "Imam Yahya an-Nawawi (RA)",
            totalHadithsBn = "১,৮৯৬টি হাদিস",
            totalHadithsCount = 1896,
            totalChaptersBn = "২০টি খণ্ড / ৩৭২টি পরিচ্ছেদ",
            authenticGradeBn = "নির্বাচিত বিশুদ্ধ সংকলন",
            authenticGradeEn = "Curated Authentic Traditions",
            descriptionBn = "নৈতিকতা, আত্মশুদ্ধি, চরিত্র গঠন ও আধ্যাত্মিক উন্নতির শ্রেষ্ঠ ব্যবহারিক গাইড।",
            descriptionEn = "A popular, highly practical thematic selection of hadiths for daily life and spiritual growth.",
            assetFileName = "riyadussalihin.json"
        )
    )

    init {
        // Pre-populate canonical metadata cache
        defaultCanonicalBooks.forEach { booksMetadataCache[it.id] = it }
    }

    /**
     * Creates a BOM-safe Reader decoding UTF-8 cleanly.
     * Prevents UTF-8 BOM bytes (\uFEFF) from breaking JSON token parsing.
     */
    private fun createUtf8BomSafeReader(inputStream: InputStream): Reader {
        val pushbackStream = PushbackInputStream(inputStream, 3)
        val bom = ByteArray(3)
        val n = pushbackStream.read(bom, 0, 3)
        if (n >= 3 && bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
            // UTF-8 BOM detected and stripped
        } else if (n > 0) {
            pushbackStream.unread(bom, 0, n)
        }
        return InputStreamReader(pushbackStream, StandardCharsets.UTF_8)
    }

    /**
     * Retrieves the list of all available canonical books, dynamically inspecting
     * asset files to update actual counts if available.
     */
    suspend fun getCanonicalBooks(): List<Book> = withContext(Dispatchers.IO) {
        val orderedIds = listOf("bukhari", "muslim", "nasai", "abudawud", "tirmidhi", "ibnmajah", "muwatta", "riyadussalihin")
        val result = mutableListOf<Book>()
        orderedIds.forEach { id ->
            booksMetadataCache[id]?.let { result.add(it) }
        }
        booksMetadataCache.values.forEach { book ->
            if (!orderedIds.contains(book.id)) {
                result.add(book)
            }
        }
        result
    }

    /**
     * Asynchronously loads and parses a Hadith book JSON from assets (e.g. `hadith/bukhari.json` or `bukhari.json`).
     * Employs streaming parsing and dynamic key extraction.
     */
    suspend fun getHadithsForBook(bookId: String): List<Hadith> = withContext(Dispatchers.IO) {
        hadithsCache[bookId]?.let { return@withContext it }

        val targetBook = booksMetadataCache[bookId] ?: defaultCanonicalBooks.find { it.id == bookId }
        val fileName = targetBook?.assetFileName ?: "$bookId.json"

        val parsed = parseHadithsFromAsset(fileName, bookId)
        hadithsCache[bookId] = parsed
        parsed
    }

    /**
     * Checks if a book has already been loaded into memory.
     */
    fun isBookLoaded(bookId: String): Boolean = hadithsCache.containsKey(bookId)

    /**
     * Returns all hadiths currently loaded in cache across any book.
     */
    fun getAllCachedHadiths(): List<Hadith> = hadithsCache.values.flatten()

    /**
     * Loads chapters for a given book.
     */
    suspend fun getChaptersForBook(bookId: String): List<Chapter> = withContext(Dispatchers.IO) {
        chaptersCache[bookId]?.let { return@withContext it }
        getHadithsForBook(bookId)
        chaptersCache[bookId] ?: emptyList()
    }

    /**
     * Asynchronously loads all hadiths across all canonical books, caching them efficiently.
     */
    suspend fun getAllHadiths(): List<Hadith> = withContext(Dispatchers.IO) {
        val allList = mutableListOf<Hadith>()
        for (book in defaultCanonicalBooks) {
            allList.addAll(getHadithsForBook(book.id))
        }
        allList
    }

    /**
     * Robust parser for an asset file. Supports all canonical and custom schemas:
     * 1. Structured JSON: { "metadata": {...}, "chapters": [...], "hadiths": [...] }
     * 2. Flat Array: [ { ... }, { ... } ]
     * 3. Wrapped Objects with alternative keys: "hadith", "data", "items", "records", "result"
     * 4. Object Dictionaries: { "1": {...}, "2": {...} }
     */
    private fun parseHadithsFromAsset(fileName: String, expectedBookId: String): List<Hadith> {
        val hadithList = mutableListOf<Hadith>()
        val chapterList = mutableListOf<Chapter>()

        val assetPath = findAssetPath(fileName) ?: return emptyList()

        try {
            context.assets.open(assetPath).use { inputStream ->
                createUtf8BomSafeReader(inputStream).use { reader ->
                    val rootElement: JsonElement = JsonParser.parseReader(reader)

                    if (rootElement.isJsonArray) {
                        // Direct array of Hadith items
                        val array = rootElement.asJsonArray
                        for (itemElement in array) {
                            if (itemElement.isJsonObject) {
                                val hadith = gson.fromJson(itemElement, Hadith::class.java)
                                if (hadith != null) hadithList.add(hadith)
                            }
                        }
                    } else if (rootElement.isJsonObject) {
                        val rootObj = rootElement.asJsonObject

                        // 1. Extract metadata / book info if present
                        val metaKeys = listOf("metadata", "book", "info", "header")
                        for (key in metaKeys) {
                            if (rootObj.has(key) && rootObj.get(key).isJsonObject) {
                                try {
                                    val bookMeta: Book? = gson.fromJson(rootObj.getAsJsonObject(key), Book::class.java)
                                    if (bookMeta != null && bookMeta.id.isNotBlank()) {
                                        booksMetadataCache[bookMeta.id] = bookMeta
                                    }
                                } catch (_: Exception) {}
                                break
                            }
                        }

                        // 2. Extract chapters if present
                        val chapterKeys = listOf("chapters", "chapter_list", "sections", "books")
                        for (key in chapterKeys) {
                            if (rootObj.has(key) && rootObj.get(key).isJsonArray) {
                                try {
                                    val chapters: List<Chapter>? = gson.fromJson(
                                        rootObj.getAsJsonArray(key),
                                        object : TypeToken<List<Chapter>>() {}.type
                                    )
                                    if (chapters != null) chapterList.addAll(chapters)
                                } catch (_: Exception) {}
                                break
                            }
                        }

                        // 3. Extract hadith items
                        val hadithArrayKeys = listOf("hadiths", "items", "data", "records", "list", "hadith", "result", expectedBookId)
                        var foundArray = false

                        for (key in hadithArrayKeys) {
                            if (rootObj.has(key) && rootObj.get(key).isJsonArray) {
                                val array = rootObj.getAsJsonArray(key)
                                for (itemElement in array) {
                                    if (itemElement.isJsonObject) {
                                        val hadith = gson.fromJson(itemElement, Hadith::class.java)
                                        if (hadith != null) hadithList.add(hadith)
                                    }
                                }
                                foundArray = true
                                break
                            }
                        }

                        // If not found as array, check if rootObj is an object map of items: { "1": {...}, "2": {...} }
                        if (!foundArray) {
                            for ((key, value) in rootObj.entrySet()) {
                                if (key in metaKeys || key in chapterKeys) continue
                                if (value.isJsonObject) {
                                    val hadith = gson.fromJson(value, Hadith::class.java)
                                    if (hadith != null && (hadith.arabicText.isNotBlank() || hadith.translationBangla.isNotBlank())) {
                                        hadithList.add(hadith)
                                    }
                                } else if (value.isJsonArray && !foundArray) {
                                    for (itemElement in value.asJsonArray) {
                                        if (itemElement.isJsonObject) {
                                            val hadith = gson.fromJson(itemElement, Hadith::class.java)
                                            if (hadith != null) hadithList.add(hadith)
                                        }
                                    }
                                    foundArray = true
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HadithRepository", "Error parsing Hadith JSON asset: $assetPath", e)
        }

        if (chapterList.isNotEmpty()) {
            chaptersCache[expectedBookId] = chapterList
        }

        val chapterMap = chapterList.associateBy { it.id }

        val defaultBook = booksMetadataCache[expectedBookId] ?: defaultCanonicalBooks.find { it.id == expectedBookId }
        val defaultBookNameBn = defaultBook?.nameBn ?: "সহীহ হাদিস"
        val defaultBookNameEn = defaultBook?.nameEn ?: "Authentic Hadith"

        // Generate clean fallback IDs & ensure book metadata is properly populated
        val mappedList = hadithList.mapIndexed { index, item ->
            val uniqueId = if (item.id > 0) item.id else (expectedBookId.hashCode() * 100000 + (index + 1))
            val finalHadithNumber = if (item.hadithNumber.isNotBlank()) item.hadithNumber else HadithTextNormalizer.toBengaliNumerals((index + 1).toString())
            val finalBookNameBn = if (item.bookNameBn.isNotBlank() && item.bookNameBn != "সহীহ হাদিস") item.bookNameBn else defaultBookNameBn
            val finalBookNameEn = if (item.bookNameEn.isNotBlank() && item.bookNameEn != "Authentic Hadith") item.bookNameEn else defaultBookNameEn
            val finalCollection = if (item.collection.isNotBlank()) item.collection else "$finalBookNameBn: $finalHadithNumber"

            val chapter = chapterMap[item.chapterId]
            val resolvedChapterBn = if (item.chapterBn.isNotBlank()) HadithTextNormalizer.normalize(item.chapterBn) else HadithTextNormalizer.normalize(chapter?.titleBn?.ifBlank { chapter.titleAr } ?: "")
            val resolvedChapterEn = if (item.chapterEn.isNotBlank()) HadithTextNormalizer.normalize(item.chapterEn) else HadithTextNormalizer.normalize(chapter?.titleEn ?: "")
            val resolvedTopicBn = if (item.topicBn.isNotBlank()) HadithTextNormalizer.normalize(item.topicBn) else resolvedChapterBn.ifBlank { "হাদিস ও নসীহত" }
            val resolvedTopicEn = if (item.topicEn.isNotBlank()) HadithTextNormalizer.normalize(item.topicEn) else resolvedChapterEn.ifBlank { "Hadith & Guidance" }

            val cleanNarrator = HadithTextNormalizer.normalize(item.narrator).trim().removeSurrounding("\"").removeSurrounding("'").trim()
            val cleanArabic = HadithTextNormalizer.normalize(item.arabicText)
            val cleanBn = HadithTextNormalizer.normalize(item.translationBangla)
            val cleanEn = HadithTextNormalizer.normalize(item.translationEnglish)
            val cleanGrade = HadithTextNormalizer.normalize(item.gradeBn).ifBlank { "সহীহ" }

            item.copy(
                id = uniqueId,
                bookId = if (item.bookId.isNotBlank()) item.bookId else expectedBookId,
                bookNameBn = finalBookNameBn,
                bookNameEn = finalBookNameEn,
                hadithNumber = finalHadithNumber,
                collection = finalCollection,
                arabicText = cleanArabic,
                translationBangla = cleanBn,
                translationEnglish = cleanEn,
                topicBn = resolvedTopicBn,
                topicEn = resolvedTopicEn,
                narrator = cleanNarrator,
                gradeBn = cleanGrade,
                chapterBn = resolvedChapterBn,
                chapterEn = resolvedChapterEn,
                explanationBn = HadithTextNormalizer.normalize(item.explanationBn)
            )
        }

        return mappedList
    }

    /**
     * Checks multiple common paths for the requested asset:
     * 1. hadith/{fileName}
     * 2. {fileName}
     * 3. hadith/{lowercase}
     * 4. {lowercase}
     */
    private fun findAssetPath(fileName: String): String? {
        val candidatePaths = listOf(
            "hadith/$fileName",
            fileName,
            "hadith/${fileName.lowercase()}",
            fileName.lowercase()
        )

        for (path in candidatePaths) {
            try {
                context.assets.open(path).close()
                return path
            } catch (_: Exception) {
                // Not found at this path, continue checking
            }
        }
        return null
    }

    /**
     * Real-time search and filter algorithm with multilingual matching.
     */
    fun filterHadiths(
        allHadiths: List<Hadith>,
        query: String,
        selectedBookId: String,
        selectedTopic: String,
        selectedChapterId: Int = 0
    ): List<Hadith> {
        val cleanQuery = HadithTextNormalizer.normalize(query).trim()
        val isAllBooks = selectedBookId == "all"
        val isAllTopics = selectedTopic == "সকল বিষয়" || selectedTopic == "All Topics" || selectedTopic.isBlank()

        return allHadiths.filter { hadith ->
            // Book match
            val bookMatch = isAllBooks || hadith.bookId.equals(selectedBookId, ignoreCase = true)

            // Topic match
            val topicMatch = isAllTopics ||
                    hadith.topicBn.contains(selectedTopic, ignoreCase = true) ||
                    hadith.topicEn.contains(selectedTopic, ignoreCase = true) ||
                    selectedTopic.contains(hadith.topicBn, ignoreCase = true)

            // Chapter match
            val chapterMatch = selectedChapterId == 0 || hadith.chapterId == selectedChapterId

            // Query match
            val queryMatch = if (cleanQuery.isBlank()) {
                true
            } else {
                hadith.hadithNumber.contains(cleanQuery, ignoreCase = true) ||
                        hadith.arabicText.contains(cleanQuery, ignoreCase = true) ||
                        hadith.translationBangla.contains(cleanQuery, ignoreCase = true) ||
                        hadith.translationEnglish.contains(cleanQuery, ignoreCase = true) ||
                        hadith.collection.contains(cleanQuery, ignoreCase = true) ||
                        hadith.narrator.contains(cleanQuery, ignoreCase = true) ||
                        hadith.chapterBn.contains(cleanQuery, ignoreCase = true) ||
                        hadith.chapterEn.contains(cleanQuery, ignoreCase = true) ||
                        hadith.topicBn.contains(cleanQuery, ignoreCase = true) ||
                        hadith.gradeBn.contains(cleanQuery, ignoreCase = true) ||
                        hadith.gradeEn.contains(cleanQuery, ignoreCase = true)
            }

            bookMatch && topicMatch && chapterMatch && queryMatch
        }
    }

    /**
     * Paginates a list of Hadiths to prevent UI thread lag and memory spikes.
     */
    fun paginate(items: List<Hadith>, page: Int, pageSize: Int = 30): List<Hadith> {
        val totalCount = items.size
        val maxItems = ((page + 1) * pageSize).coerceAtMost(totalCount)
        return items.take(maxItems)
    }
}
