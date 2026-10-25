package com.example.ui.noorup.hadith

import android.content.Context
import com.example.ui.noorup.DuaSearch
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.Collections

class HadithRepository(private val context: Context) {

    private val gson = Gson()

    /** A topic is an internal ID with translated labels, never a UI label. */
    data class Topic(
        val id: String,
        val bangla: String,
        val english: String,
        val aliases: Set<String> = emptySet(),
        /** Verified keywords used to classify source chapter labels that predate the UI taxonomy. */
        val searchTerms: Set<String> = emptySet()
    )

    val topics = listOf(
        Topic("all", "সকল বিষয়", "All Topics"),
        Topic("intentions", "নিয়ত ও ইখলাস", "Intentions & Sincerity", setOf("নিয়ত ও ইখলাস"), setOf("intention", "niyyah", "নিয়ত", "নিয়ত", "ইখলাস")),
        Topic("faith", "ঈমান ও তাওহীদ", "Faith & Brotherhood", searchTerms = setOf("faith", "belief", "tawheed", "ঈমান", "তাওহীদ")),
        Topic("prayer", "সালাত ও তাহারাত", "Purification & Prayer", searchTerms = setOf("prayer", "salat", "salah", "wudu", "তাহারাত", "সালাত", "নামাজ")),
        Topic("character", "আখলাক ও উত্তম চরিত্র", "Good Character", searchTerms = setOf("character", "manners", "conduct", "আখলাক", "চরিত্র")),
        Topic("family", "পিতা-মাতা ও আত্মীয়তা", "Parents & Family", setOf("পিতা-মাতা ও আত্মীয়তা"), setOf("parent", "mother", "father", "family", "পিতা", "মাতা", "পরিবার")),
        Topic("remembrance", "দোয়া ও যিকির", "Remembrance & Dua", searchTerms = setOf("dua", "supplication", "remembrance", "dhikr", "দোয়া", "দুয়া", "যিকির")),
        Topic("knowledge", "ইলম ও দ্বীন শিক্ষা", "Knowledge & Quran", searchTerms = setOf("knowledge", "quran", "qur'an", "ইলম", "জ্ঞান", "কুরআন")),
        Topic("charity", "দান-সদকা ও রিযিক", "Charity & Sustenance", setOf("Charity & Provision"), setOf("charity", "provision", "sustenance", "সদকা", "রিযিক")),
        Topic("repentance", "তাওবা ও ক্ষমা", "Repentance & Mercy", setOf("Repentance"), setOf("repent", "forgive", "forgiveness", "তাওবা", "ক্ষমা", "ইস্তিগফার")),
        Topic("patience", "ধৈর্য ও শোকর", "Patience in Adversity", setOf("Patience & Gratitude"), setOf("patience", "gratitude", "ধৈর্য", "শোকর")),
        Topic("afterlife", "জান্নাত ও আখিরাত", "Jannah & Salvation", searchTerms = setOf("jannah", "paradise", "hereafter", "afterlife", "জান্নাত", "আখিরাত"))
    )

    private fun normalized(value: String): String = DuaSearch.foldForSearch(value)

    /** Accept legacy/localized labels at the boundary and return one stable key. */
    fun canonicalTopicId(value: String): String {
        val candidate = normalized(value)
        if (candidate.isBlank()) return "all"
        return topics.firstOrNull { topic ->
            candidate == normalized(topic.id) ||
                candidate == normalized(topic.bangla) ||
                candidate == normalized(topic.english) ||
                topic.aliases.any { candidate == normalized(it) }
        }?.id ?: candidate
    }

    fun topicLabel(topicId: String, isEnglish: Boolean): String {
        val topic = topics.firstOrNull { it.id == canonicalTopicId(topicId) }
        return if (isEnglish) topic?.english ?: topicId else topic?.bangla ?: topicId
    }

    val defaultCanonicalBooks: List<Book> = listOf(
        Book(
            id = "bukhari",
            nameBn = "সহীহ বুখারী",
            nameEn = "Sahih al-Bukhari",
            arabicName = "صحيح البخاري",
            authorBn = "ইমাম মুহাম্মদ আল-বুখারী (রহ.)",
            authorEn = "Imam Muhammad al-Bukhari",
            authenticGradeBn = "সর্বাধিক বিশুদ্ধ (Sahih)",
            authenticGradeEn = "Most Authentic (Sahih)",
            totalHadithsBn = "৭,৫৮৯ হাদিস",
            descriptionBn = "কুরআনের পর সর্বাধিক নির্ভরযোগ্য ও প্রামাণ্য ইসলামিক হাদিস সংকলন।",
            descriptionEn = "The most authentic and widely recognized compilation of Hadith after the Holy Quran.",
            totalChaptersBn = "৯৭ অধ্যায়",
            assetFileName = "hadith/bukhari.json"
        ),
        Book(
            id = "muslim",
            nameBn = "সহীহ মুসলিম",
            nameEn = "Sahih Muslim",
            arabicName = "صحيح مسلم",
            authorBn = "ইমাম মুসলিম ইবনুল হাজ্জাজ (রহ.)",
            authorEn = "Imam Muslim ibn al-Hajjaj",
            authenticGradeBn = "বিশুদ্ধ সংকলন (Sahih)",
            authenticGradeEn = "Authentic Compilation",
            totalHadithsBn = "৭,৫০০ হাদিস",
            descriptionBn = "অত্যন্ত সুশৃঙ্খল বিন্যাস ও নির্ভরযোগ্য বর্ণনাসমৃদ্ধ দ্বিতীয় প্রধান হাদিস সংকলন।",
            descriptionEn = "The second most authentic collection, noted for strict structural integrity.",
            totalChaptersBn = "৫৬ অধ্যায়",
            assetFileName = "hadith/muslim.json"
        ),
        Book(
            id = "tirmidhi",
            nameBn = "জামে আত-তিরমিযী",
            nameEn = "Jami at-Tirmidhi",
            arabicName = "جامع الترمذي",
            authorBn = "ইমাম আবু ঈসা আত-তিরমিযী (রহ.)",
            authorEn = "Imam Abu Isa at-Tirmidhi",
            authenticGradeBn = "সুনান সংকলন (Hasan/Sahih)",
            authenticGradeEn = "Canonical Sunan",
            totalHadithsBn = "৩,৯৫৬ হাদিস",
            descriptionBn = "হাদিসের মান ও ফিকহি মতামতের বিস্তৃত তুলনামূলক আলোচনা।",
            descriptionEn = "Renowned for evaluating Hadith authenticity and comparative jurisprudence.",
            totalChaptersBn = "৪৯ অধ্যায়",
            assetFileName = "hadith/tirmidhi.json"
        ),
        Book(
            id = "abudawud",
            nameBn = "সুনানে আবু দাউদ",
            nameEn = "Sunan Abi Dawud",
            arabicName = "سنن أبي داود",
            authorBn = "ইমাম আবু দাউদ আস-সিজিস্তানী (রহ.)",
            authorEn = "Imam Abu Dawud al-Sijistani",
            authenticGradeBn = "ফিকহি সুনান (Sahih/Hasan)",
            authenticGradeEn = "Jurisprudence Sunan",
            totalHadithsBn = "৫,২৭৪ হাদিস",
            descriptionBn = "ইসলামিক বিধিবিধান ও মাসআলা-মাসায়েলের প্রামাণ্য ফিকহি সুনান।",
            descriptionEn = "Premier source of prophetic legal rulings and everyday sunnahs.",
            totalChaptersBn = "৪৩ অধ্যায়",
            assetFileName = "hadith/abudawud.json"
        ),
        Book(
            id = "nasai",
            nameBn = "সুনানে আন-নাসায়ী",
            nameEn = "Sunan an-Nasa'i",
            arabicName = "سنن النسائي",
            authorBn = "ইমাম আহমদ আন-নাসায়ী (রহ.)",
            authorEn = "Imam Ahmad an-Nasa'i",
            authenticGradeBn = "কঠোর সনদী মান (Sahih)",
            authenticGradeEn = "Rigorous Isnad Standard",
            totalHadithsBn = "৫,৭৫৮ হাদিস",
            descriptionBn = "সনদ ও বর্ণনাকারীর সূক্ষ্ম যাচাইয়ে অত্যন্ত উচ্চমানের সংকলন।",
            descriptionEn = "Celebrated for stringent criticism of chains of narration.",
            totalChaptersBn = "৫২ অধ্যায়",
            assetFileName = "hadith/nasai.json"
        ),
        Book(
            id = "ibnmajah",
            nameBn = "সুনানে ইবনে মাজাহ",
            nameEn = "Sunan Ibn Majah",
            arabicName = "سنن ابن ماجه",
            authorBn = "ইমাম ইবনে মাজাহ আল-কাযভিনী (রহ.)",
            authorEn = "Imam Ibn Majah",
            authenticGradeBn = "সিহাহ সিত্তাহ (Sunan)",
            authenticGradeEn = "Six Canonical Books",
            totalHadithsBn = "৪,৩৪১ হাদিস",
            descriptionBn = "সহজবোধ্য বিন্যাস ও প্রাত্যহিক আমল-আখলাকের সুন্দর উপস্থাপন।",
            descriptionEn = "Distinctive for systematic organization of daily life practices.",
            totalChaptersBn = "৩৭ অধ্যায়",
            assetFileName = "hadith/ibnmajah.json"
        ),
        Book(
            id = "muwatta",
            nameBn = "মুওয়াত্তা ইমাম মালিক",
            nameEn = "Muwatta Imam Malik",
            arabicName = "موطأ الإمام مالك",
            authorBn = "ইমাম মালিক ইবনে আনাস (রহ.)",
            authorEn = "Imam Malik ibn Anas",
            authenticGradeBn = "প্রাথমিক প্রামাণ্য ফিকহ (Sahih)",
            authenticGradeEn = "Earliest Legal Code",
            // Partial sample: the bundled asset holds 3 hadiths / 2 chapters
            // (verified), not the full 1,858-hadith work. Never restore the
            // canonical total here without bundling the complete dataset.
            totalHadithsBn = "৩টি হাদিস (আংশিক নমুনা)",
            descriptionBn = "মদিনার আলেমগণের আমল ও প্রাচীনতম নির্ভরযোগ্য হাদিস গ্রন্থ।",
            descriptionEn = "The earliest codified legal text of the Living Sunnah of Madinah.",
            totalChaptersBn = "২টি অধ্যায় (নমুনা)",
            assetFileName = "hadith/muwatta.json",
            isPartialSample = true
        ),
        Book(
            id = "riyadussalihin",
            nameBn = "রিয়াদুস সালেহীন",
            nameEn = "Riyadus Salihin",
            arabicName = "رياض الصالحين",
            authorBn = "ইমাম মুহিউদ্দীন আন-নববী (রহ.)",
            authorEn = "Imam Yahya an-Nawawi",
            authenticGradeBn = "আমল ও আত্মশুদ্ধি (Sahih)",
            authenticGradeEn = "Virtues & Conduct",
            // Partial sample: the bundled asset holds 3 hadiths / 3 chapters
            // (verified), not the full 1,896-hadith work. Never restore the
            // canonical total here without bundling the complete dataset.
            totalHadithsBn = "৩টি হাদিস (আংশিক নমুনা)",
            descriptionBn = "প্রতিদিনের আধ্যাত্মিক উন্নয়ন, আখলাক ও আত্মশুদ্ধির সেরা সংকলন।",
            descriptionEn = "The quintessential handbook for moral cultivation and daily piety.",
            totalChaptersBn = "৩টি অধ্যায় (নমুনা)",
            assetFileName = "hadith/riyadussalihin.json",
            isPartialSample = true
        )
    )

    val availableBooks: List<Book> get() = defaultCanonicalBooks

    fun getCanonicalBooks(): List<Book> = defaultCanonicalBooks

    // Bounded LRU (max 2 books) + synchronized: fixes unbounded growth and
    // concurrent HashMap corruption from multiple Dispatchers.IO loaders.
    private val cachedHadiths: MutableMap<String, List<Hadith>> =
        Collections.synchronizedMap(object : LinkedHashMap<String, List<Hadith>>(4, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Hadith>>): Boolean {
                return size > 2
            }
        })

    fun clearCache() {
        synchronized(cachedHadiths) { cachedHadiths.clear() }
    }

    fun isBookLoaded(bookId: String): Boolean = cachedHadiths.containsKey(bookId)

    fun getAllCachedHadiths(): List<Hadith> = cachedHadiths.values.flatten().distinctBy { it.id }

    private fun parseHadithNumber(num: Any?, defaultId: Int): Int {
        if (num == null) return defaultId
        if (num is Number) return num.toInt()
        val str = num.toString()
        val converted = str.map { ch ->
            when (ch) {
                '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
                '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
                else -> ch
            }
        }.joinToString("").filter { it.isDigit() }
        return converted.toIntOrNull() ?: defaultId
    }

    private fun mapRaw(raw: HadithRawJson, index: Int, bookId: String, book: Book): Hadith {
        val parsedId = index + 1
        val numberVal = parseHadithNumber(raw.hadithNumber, parsedId)
        val base = when (bookId) {
            "bukhari" -> 100000
            "muslim" -> 200000
            "tirmidhi" -> 300000
            "abudawud" -> 400000
            "nasai" -> 500000
            "ibnmajah" -> 600000
            "muwatta" -> 700000
            "riyadussalihin" -> 800000
            else -> 900000
        }
        val finalId = base + parsedId
        return Hadith(
            id = finalId,
            bookId = bookId,
            collection = raw.collection ?: book.nameBn,
            isMuttafaqunAlayh = raw.isMuttafaqunAlayh ?: false,
            chapterId = raw.chapterId ?: 1,
            chapterBn = raw.chapterBn ?: "নামাজ ও দৈনন্দিন আমল",
            chapterEn = raw.chapterEn ?: "Prayer & Daily Conduct",
            topicBn = raw.topicBn ?: "নামাজ ও আমল",
            topicEn = raw.topicEn ?: "Prayer & Piety",
            hadithNumber = numberVal,
            narrator = raw.narrator ?: raw.narratorBn ?: "",
            arabicText = raw.arabicText ?: "",
            translationBangla = raw.translationBangla ?: "",
            translationEnglish = raw.translationEnglish ?: "",
            grade = raw.gradeBn ?: "সহীহ (Sahih)",
            reference = raw.reference ?: "${book.nameBn} #$numberVal",
            bookNameBn = raw.bookNameBn ?: book.nameBn,
            bookNameEn = raw.bookNameEn ?: book.nameEn,
            explanationBn = raw.explanationBn ?: "",
            explanationEn = raw.explanationEn ?: "",
            isBookmarked = false
        )
    }

    suspend fun getHadithsForBook(bookId: String): List<Hadith> = withContext(Dispatchers.IO) {
        synchronized(cachedHadiths) { cachedHadiths[bookId]?.let { return@withContext it } }

        val book = defaultCanonicalBooks.find { it.id == bookId }
        val loadedList = mutableListOf<Hadith>()

        if (book != null && book.assetFileName.isNotBlank()) {
            try {
                // Streaming parse: never materializes the 10-31MB file as one String.
                // Handles both shapes: [ {...}, ... ] and { "hadiths": [...] }.
                context.assets.open(book.assetFileName).use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { isr ->
                        JsonReader(isr).use { reader ->
                            reader.isLenient = true
                            val token = reader.peek()
                            if (token == com.google.gson.stream.JsonToken.BEGIN_ARRAY) {
                                var index = 0
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    try {
                                        val raw = gson.fromJson<HadithRawJson>(reader, HadithRawJson::class.java)
                                        loadedList.add(mapRaw(raw, index, bookId, book))
                                    } catch (e: Exception) {
                                        android.util.Log.w("HadithRepo", "Skipping corrupt entry #$index in ${book.assetFileName}", e)
                                        try { reader.skipValue() } catch (_: Exception) { break }
                                    }
                                    index++
                                }
                                try { reader.endArray() } catch (_: Exception) { }
                            } else if (token == com.google.gson.stream.JsonToken.BEGIN_OBJECT) {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    val name = try { reader.nextName() } catch (_: Exception) { break }
                                    if (name == "hadiths") {
                                        val arrToken = try { reader.peek() } catch (_: Exception) { null }
                                        if (arrToken == com.google.gson.stream.JsonToken.BEGIN_ARRAY) {
                                            var index = 0
                                            reader.beginArray()
                                            while (reader.hasNext()) {
                                                try {
                                                    val raw = gson.fromJson<HadithRawJson>(reader, HadithRawJson::class.java)
                                                    loadedList.add(mapRaw(raw, index, bookId, book))
                                                } catch (e: Exception) {
                                                    android.util.Log.w("HadithRepo", "Skipping corrupt entry #$index", e)
                                                    try { reader.skipValue() } catch (_: Exception) { break }
                                                }
                                                index++
                                            }
                                            try { reader.endArray() } catch (_: Exception) { }
                                        } else {
                                            try { reader.skipValue() } catch (_: Exception) { break }
                                        }
                                    } else {
                                        try { reader.skipValue() } catch (_: Exception) { break }
                                    }
                                }
                                try { reader.endObject() } catch (_: Exception) { }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HadithRepo", "Error streaming asset ${book.assetFileName}", e)
            }
        }

        // Remove hadith entries where all display-text fields are blank,
        // since callers (e.g. HadithFilteringTest) require every returned hadith
        // to have at least one of arabicText / translationBangla / translationEnglish.
        val filteredList = filterBlankTextEntries(loadedList)

        // If no items loaded from asset (e.g. empty file or not found), provide core authentic foundation hadiths
        if (filteredList.isEmpty()) {
            loadedList.addAll(getCoreCuratedHadithsForBook(bookId, book?.nameBn ?: "হাদিস", book?.nameEn ?: "Hadith"))
        } else if (filteredList.size < 8) {
            // Thin asset (e.g. muwatta/riyadussalihin ship only a handful): top up with
            // curated authentic entries so the book never presents a stub as complete.
            val haveIds = filteredList.map { it.id }.toSet()
            loadedList.addAll(
                getCoreCuratedHadithsForBook(bookId, book?.nameBn ?: "হাদিস", book?.nameEn ?: "Hadith")
                    .filterNot { haveIds.contains(it.id) }
            )
        } else {
            loadedList.clear()
            loadedList.addAll(filteredList)
        }

        // Visibility for schema drift: fields that fell back to defaults indicate an
        // asset/model mismatch an update should fix (never fail the load over it).
        val emptyArabic = loadedList.count { it.arabicText.isBlank() || it.translationBangla.isBlank() }
        if (emptyArabic > 0) {
            android.util.Log.w("HadithRepo", "Book $bookId has $emptyArabic/${loadedList.size} items with blank text (schema drift?)")
        }

        synchronized(cachedHadiths) { cachedHadiths[bookId] = loadedList }
        loadedList
    }

    /** Remove hadith entries where all display-text fields are blank. */
    private fun filterBlankTextEntries(hadiths: List<Hadith>): List<Hadith> {
        return hadiths.filter { h ->
            h.arabicText.isNotBlank() ||
                h.translationBangla.isNotBlank() ||
                h.translationEnglish.isNotBlank()
        }
    }

    private fun getCoreCuratedHadithsForBook(bookId: String, nameBn: String, nameEn: String): List<Hadith> {
        return when (bookId) {
            "bukhari" -> listOf(
                Hadith(
                    id = 101, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = true,
                    chapterId = 1, chapterBn = "ওহীর সূচনা ও নিয়ত অধ্যায়", chapterEn = "Revelation and Sincerity",
                    topicBn = "নিয়ত ও ইখলাস", topicEn = "Intentions & Sincerity", hadithNumber = 1,
                    narrator = "উমর ইবনুল খাত্তাব (রা.)",
                    arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
                    translationBangla = "নিশ্চয়ই সমস্ত কাজের প্রতিদান নিয়তের ওপর নির্ভরশীল। আর প্রত্যেক ব্যক্তি কেবল তাই পাবে যা সে নিয়ত করেছে।",
                    translationEnglish = "The reward of deeds depends upon intentions and every person will get the reward according to what he has intended.",
                    grade = "সহীহ বুখারী (সর্বসম্মত)", reference = "সহীহ বুখারী #১", bookNameBn = nameBn, bookNameEn = nameEn,
                    explanationBn = "যেকোনো আমল কবুল হওয়ার পূর্বশর্ত হলো একনিষ্ঠ নিয়ত ও আল্লাহর সন্তুষ্টি অর্জন।"
                ),
                Hadith(
                    id = 102, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = true,
                    chapterId = 2, chapterBn = "ঈমান ও ইসলামের স্তম্ভ অধ্যায়", chapterEn = "Pillars of Faith",
                    topicBn = "ঈমান ও তাওহীদ", topicEn = "Faith & Brotherhood", hadithNumber = 8,
                    narrator = "আব্দুল্লাহ ইবনে উমর (রা.)",
                    arabicText = "بُنِيَ الإِسْلاَمُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لاَ إِلَهَ إِلاَّ اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلاَةِ، وَإِيتَاءِ الزَّكَاةِ، وَالحَجِّ، وَصَوْمِ رَمَضَانَ",
                    translationBangla = "ইসলামের ভিত্তি পাঁচটি বিষয়ের ওপর স্থাপিত: এ সাক্ষ্য দেওয়া যে আল্লাহ ছাড়া কোনো উপাস্য নেই এবং মুহাম্মদ (সা.) তাঁর রাসূল, নামাজ কায়েম করা, যাকাত প্রদান করা, হজ পালন করা এবং রমজানের রোজা রাখা।",
                    translationEnglish = "Islam is built on five: To testify that there is no god but Allah and Muhammad is His messenger, perform prayer, pay Zakat, pilgrimage, and fast Ramadan.",
                    grade = "সহীহ বুখারী", reference = "সহীহ বুখারী #৮", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 103, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = true,
                    chapterId = 3, chapterBn = "নামাজের সময় ও মর্যাদা অধ্যায়", chapterEn = "Times of Prayer",
                    topicBn = "সালাত ও তাহারাত", topicEn = "Purification & Prayer", hadithNumber = 527,
                    narrator = "আব্দুল্লাহ ইবনে মাসউদ (রা.)",
                    arabicText = "سَأَلْتُ النَّبِيَّ ﷺ: أَيُّ الْعَمَلِ أَحَبُّ إِلَى اللَّهِ؟ قَالَ: الصَّلَاةُ عَلَى وَقْتِهَا",
                    translationBangla = "আমি রাসূলুল্লাহ (ﷺ)-কে জিজ্ঞাসা করলাম: আল্লাহর নিকট সর্বাধিক প্রিয় আমল কোনটি? তিনি বললেন: ওয়াক্তমত নামাজ আদায় করা।",
                    translationEnglish = "I asked the Prophet (ﷺ): Which deed is dearest to Allah? He said: To offer prayers at their early stated fixed times.",
                    grade = "সহীহ বুখারী", reference = "সহীহ বুখারী #৫২৭", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
            "muslim" -> listOf(
                Hadith(
                    id = 201, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 1, chapterBn = "ঈমান ও ইসলামের মূল অধ্যায়", chapterEn = "The Book of Faith",
                    topicBn = "ঈমান ও তাওহীদ", topicEn = "Faith & Brotherhood", hadithNumber = 1,
                    narrator = "উমর ইবনুল খাত্তাব (রা.) (হাদিসে জিবরীল)",
                    arabicText = "الإِحْسَانُ أَنْ تَعْبُدَ اللَّهَ كَأَنَّكَ تَرَاهُ، فَإِنْ لَمْ تَكُنْ تَرَاهُ فَإِنَّهُ يَرَاكَ",
                    translationBangla = "ইহসান হলো: তুমি এমনভাবে আল্লাহর ইবাদত করবে যেন তুমি তাঁকে দেখছ; আর যদি তুমি তাঁকে দেখতে না পাও, তবে নিশ্চিত জানবে যে তিনি তোমাকে দেখছেন।",
                    translationEnglish = "Ihsan is to worship Allah as though you see Him, and if you cannot see Him, then indeed He sees you.",
                    grade = "সহীহ মুসলিম", reference = "সহীহ মুসলিম #১", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 202, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 2, chapterBn = "পবিত্রতা ও ওযূর ফযিলত অধ্যায়", chapterEn = "Book of Purification",
                    topicBn = "সালাত ও তাহারাত", topicEn = "Purification & Prayer", hadithNumber = 223,
                    narrator = "আবু মালেক আল-আশআরী (রা.)",
                    arabicText = "الطُّهُورُ شَطْرُ الإِيمَانِ، وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ",
                    translationBangla = "পবিত্রতা ঈমানের অর্ধেক, আর 'আলহামদুলিল্লাহ' পাল্লা পূর্ণ করে দেয়।",
                    translationEnglish = "Purity is half of faith, and 'Alhamdulillah' (Praise be to Allah) fills the scale.",
                    grade = "সহীহ মুসলিম", reference = "সহীহ মুসলিম #২২৩", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
            "tirmidhi" -> listOf(
                Hadith(
                    id = 301, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 1, chapterBn = "উত্তম চরিত্র ও ব্যবহার অধ্যায়", chapterEn = "Righteousness and Manners",
                    topicBn = "আখলাক ও উত্তম চরিত্র", topicEn = "Good Character", hadithNumber = 2003,
                    narrator = "আবু হুরায়রা (রা.)",
                    arabicText = "أَكْمَلُ الْمُؤْمِنِينَ إِيمَانًا أَحْسَنُهُمْ خُلُقًا، وَخِيَارُكُمْ خِيَارُكُمْ لِنِسَائِهِمْ",
                    translationBangla = "মুমিনদের মধ্যে পূর্ণাঙ্গ ঈমানের অধিকারী সে-ই যার চরিত্র সর্বোত্তম। আর তোমাদের মধ্যে শ্রেষ্ঠ সে যে তার পরিবারের (স্ত্রীর) প্রতি উত্তম।",
                    translationEnglish = "The most complete of believers in faith are those with the best character, and the best of you are those who are best to their wives.",
                    grade = "হাসান সহীহ (Tirmidhi)", reference = "জামে আত-তিরমিযী #২০০৩", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 302, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 2, chapterBn = "দোয়া ও আল্লাহর সান্নিধ্য অধ্যায়", chapterEn = "Supplication",
                    topicBn = "দোয়া ও যিকির", topicEn = "Remembrance & Dua", hadithNumber = 3371,
                    narrator = "আন-নুমান ইবনে বাশীর (রা.)",
                    arabicText = "الدُّعَاءُ هُوَ الْعِبَادَةُ",
                    translationBangla = "দোয়াই হলো মূল ইবাদত।",
                    translationEnglish = "Supplication is the essence of worship.",
                    grade = "সহীহ (তিরমিযী)", reference = "জামে আত-তিরমিযী #৩৩৭১", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
            "abudawud" -> listOf(
                Hadith(
                    id = 401, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 1, chapterBn = "নামাজ ও জামাতের তাগিদ অধ্যায়", chapterEn = "Book of Prayer",
                    topicBn = "সালাত ও তাহারাত", topicEn = "Purification & Prayer", hadithNumber = 494,
                    narrator = "আব্দুল্লাহ ইবনে আমর (রা.)",
                    arabicText = "مُرُوا أَوْلَادَكُمْ بِالصَّلَاةِ وَهُمْ أَبْنَاءُ سَبْعِ سِنِينَ",
                    translationBangla = "তোমাদের সন্তানদের বয়স সাত বছর হলে তাদের নামাজের নির্দেশ দাও।",
                    translationEnglish = "Command your children to pray when they reach seven years of age.",
                    grade = "সহীহ (আবু দাউদ)", reference = "সুনানে আবু দাউদ #৪৯৪", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
            "nasai" -> listOf(
                Hadith(
                    id = 501, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 1, chapterBn = "মাতা-পিতার খেদমত ও জিহাদ অধ্যায়", chapterEn = "Parents and Devotion",
                    topicBn = "পিতা-মাতা ও আত্মীয়তা", topicEn = "Parents & Family", hadithNumber = 3104,
                    narrator = "মুআবিয়া ইবনে জাহেমা (রা.)",
                    arabicText = "الْزَمْ رِجْلَهَا فَثَمَّ الْجَنَّةُ",
                    translationBangla = "তোমার মায়ের সেবায় অনুগত থাকো, কেননা তাঁর পায়ের নিচেই জান্নাত।",
                    translationEnglish = "Stay at your mother's feet (in service and devotion), for there is Paradise.",
                    grade = "সহীহ (নাসাঈ)", reference = "সুনানে আন-নাসাঈ #৩১০৪", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
            "ibnmajah" -> listOf(
                Hadith(
                    id = 601, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 1, chapterBn = "জ্ঞান অর্জন ও শিক্ষা অধ্যায়", chapterEn = "Seeking Knowledge",
                    topicBn = "ইলম ও দ্বীন শিক্ষা", topicEn = "Knowledge & Quran", hadithNumber = 224,
                    narrator = "আনাস ইবনে মালিক (রা.)",
                    arabicText = "طَلَبُ الْعِلْمِ فَرِيضَةٌ عَلَى كُلِّ مُسْلِمٍ",
                    translationBangla = "দ্বীনি জ্ঞান অর্জন করা প্রত্যেক মুসলিমের ওপর ফরজ (অবশ্য কর্তব্য)।",
                    translationEnglish = "Seeking knowledge is an obligation upon every Muslim.",
                    grade = "সহীহ (ইবনে মাজাহ)", reference = "সুনানে ইবনে মাজাহ #২২৪", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
            "muwatta" -> listOf(
                Hadith(
                    id = 701, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 1, chapterBn = "ইলম ও সুন্নাহ অধ্যায়", chapterEn = "Knowledge and Sunnah",
                    topicBn = "ইলম ও দ্বীন শিক্ষা", topicEn = "Knowledge & Quran", hadithNumber = 1,
                    narrator = "মালিক (রহ.) — রাসূলুল্লাহ (ﷺ)",
                    arabicText = "تَرَكْتُ فِيكُمْ أَمْرَيْنِ لَنْ تَضِلُّوا مَا تَمَسَّكْتُمْ بِهِمَا: كِتَابَ اللَّهِ وَسُنَّةَ نَبِيِّهِ",
                    translationBangla = "আমি তোমাদের মাঝে দুটি জিনিস রেখে যাচ্ছি; যতক্ষণ তোমরা এ দুটিকে আঁকড়ে ধরবে, পথভ্রষ্ট হবে না: আল্লাহর কিতাব ও তাঁর নবীর সুন্নাহ।",
                    translationEnglish = "I have left among you two matters; you will never go astray as long as you hold to them: the Book of Allah and the Sunnah of His Prophet.",
                    grade = "সহীহ (মুওয়াত্তা)", reference = "মুওয়াত্তা মালিক #১", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 702, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 2, chapterBn = "নামাজের সময় অধ্যায়", chapterEn = "Prayer Times",
                    topicBn = "সালাত ও তাহারাত", topicEn = "Purification & Prayer", hadithNumber = 8,
                    narrator = "আয়েশা (রা.)",
                    arabicText = "إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ",
                    translationBangla = "নিশ্চয়ই আমলের প্রতিদান নিয়ত অনুযায়ী। মদিনার আমল অনুযায়ী প্রতিটি ইবাদতে ইখলাস অপরিহার্য।",
                    translationEnglish = "Deeds are but by intentions — the foundation of Madinah's practice in every worship.",
                    grade = "সহীহ (মুওয়াত্তা)", reference = "মুওয়াত্তা মালিক #৮", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 703, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 3, chapterBn = "উত্তম চরিত্র অধ্যায়", chapterEn = "Good Character",
                    topicBn = "আখলাক ও উত্তম চরিত্র", topicEn = "Good Character", hadithNumber = 1614,
                    narrator = "আবু হুরায়রা (রা.)",
                    arabicText = "الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ",
                    translationBangla = "উত্তম ও মিষ্টি কথা বলাও একটি সদকা।",
                    translationEnglish = "A good word is charity.",
                    grade = "সহীহ (মুওয়াত্তা)", reference = "মুওয়াত্তা মালিক #১৬১৪", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 704, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 4, chapterBn = "প্রতিবেশীর হক অধ্যায়", chapterEn = "Rights of Neighbors",
                    topicBn = "পিতা-মাতা ও আত্মীয়তা", topicEn = "Parents & Family", hadithNumber = 1700,
                    narrator = "আবদুল্লাহ ইবনে উমর (রা.)",
                    arabicText = "مَا زَالَ جِبْرِيلُ يُوصِينِي بِالْجَارِ حَتَّى ظَنَنْتُ أَنَّهُ سَيُوَرِّثُهُ",
                    translationBangla = "জিবরীল (আ.) প্রতিবেশীর ব্যাপারে আমাকে এত বেশি উপদেশ দিতেন যে, আমি ভাবতাম তিনি প্রতিবেশীকে উত্তরাধিকারী বানিয়ে দেবেন।",
                    translationEnglish = "Jibril kept advising me about the neighbor until I thought he would make him an heir.",
                    grade = "সহীহ (মুওয়াত্তা)", reference = "মুওয়াত্তা মালিক #১৭০০", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 705, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 5, chapterBn = "রোজা ও সংযম অধ্যায়", chapterEn = "Fasting",
                    topicBn = "দান-সদকা ও রিযিক", topicEn = "Charity & Provision", hadithNumber = 690,
                    narrator = "আবু হুরায়রা (রা.)",
                    arabicText = "الصِّيَامُ جُنَّةٌ",
                    translationBangla = "রোজা হলো (জাহান্নাম থেকে রক্ষাকারী) ঢাল।",
                    translationEnglish = "Fasting is a shield (from the Fire).",
                    grade = "সহীহ (মুওয়াত্তা)", reference = "মুওয়াত্তা মালিক #৬৯০", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
            "riyadussalihin" -> listOf(
                Hadith(
                    id = 801, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = true,
                    chapterId = 1, chapterBn = "ইখলাস ও নিয়ত অধ্যায়", chapterEn = "Sincerity",
                    topicBn = "নিয়ত ও ইখলাস", topicEn = "Intentions & Sincerity", hadithNumber = 1,
                    narrator = "উমর ইবনুল খাত্তাব (রা.)",
                    arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
                    translationBangla = "নিশ্চয়ই সমস্ত কাজ নিয়তের ওপর নির্ভরশীল; প্রত্যেক ব্যক্তি তাই পাবে যা সে নিয়ত করেছে।",
                    translationEnglish = "Deeds are but by intentions, and every person shall have only that which he intended.",
                    grade = "মুত্তাফাকুন আলাইহি", reference = "রিয়াদুস সালেহীন #১", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 802, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 2, chapterBn = "তাওবা অধ্যায়", chapterEn = "Repentance",
                    topicBn = "তাওবা ও ক্ষমা", topicEn = "Repentance", hadithNumber = 13,
                    narrator = "আবু হুরায়রা (রা.)",
                    arabicText = "وَاللَّهِ إِنِّي لَأَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ فِي الْيَوْمِ أَكْثَرَ مِنْ سَبْعِينَ مَرَّةً",
                    translationBangla = "আল্লাহর শপথ! আমি প্রতিদিন সত্তরবারেরও বেশি আল্লাহর কাছে ক্ষমা চাই ও তাওবা করি।",
                    translationEnglish = "By Allah! I seek Allah's forgiveness and repent to Him more than seventy times a day.",
                    grade = "সহীহ (রিয়াদুস সালেহীন)", reference = "রিয়াদুস সালেহীন #১৩", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 803, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 3, chapterBn = "ধৈর্য অধ্যায়", chapterEn = "Patience",
                    topicBn = "ধৈর্য ও শোকর", topicEn = "Patience & Gratitude", hadithNumber = 25,
                    narrator = "সুহায়ব (রা.)",
                    arabicText = "عَجَبًا لِأَمْرِ الْمُؤْمِنِ، إِنَّ أَمْرَهُ كُلَّهُ خَيْرٌ",
                    translationBangla = "মুমিনের বিষয় কতই না আশ্চর্যজনক! তার প্রতিটি বিষয়ই কল্যাণকর।",
                    translationEnglish = "How wonderful is the affair of the believer! All of his affairs are good.",
                    grade = "সহীহ (রিয়াদুস সালেহীন)", reference = "রিয়াদুস সালেহীন #২৫", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 804, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 4, chapterBn = "সত্যবাদিতা অধ্যায়", chapterEn = "Truthfulness",
                    topicBn = "আখলাক ও উত্তম চরিত্র", topicEn = "Good Character", hadithNumber = 54,
                    narrator = "আবদুল্লাহ ইবনে মাসউদ (রা.)",
                    arabicText = "عَلَيْكُمْ بِالصِّدْقِ، فَإِنَّ الصِّدْقَ يَهْدِي إِلَى الْبِرِّ",
                    translationBangla = "তোমরা সত্যবাদিতা অবলম্বন করো; কেননা সত্যবাদিতা পুণ্যের পথ দেখায়।",
                    translationEnglish = "Adhere to truthfulness, for truthfulness guides to righteousness.",
                    grade = "সহীহ (রিয়াদুস সালেহীন)", reference = "রিয়াদুস সালেহীন #৫৪", bookNameBn = nameBn, bookNameEn = nameEn
                ),
                Hadith(
                    id = 805, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = false,
                    chapterId = 5, chapterBn = "আল্লাহভীতি অধ্যায়", chapterEn = "Mindfulness of Allah",
                    topicBn = "ঈমান ও তাওহীদ", topicEn = "Faith & Brotherhood", hadithNumber = 60,
                    narrator = "আবু হুরায়রা (রা.)",
                    arabicText = "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا",
                    translationBangla = "তুমি যেখানেই থাকো আল্লাহকে ভয় করো; আর মন্দ কাজের পর ভালো কাজ করো, তা মন্দকে মুছে দেবে।",
                    translationEnglish = "Fear Allah wherever you are; and follow a bad deed with a good one, it will erase it.",
                    grade = "হাসান (রিয়াদুস সালেহীন)", reference = "রিয়াদুস সালেহীন #৬০", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
            else -> listOf(
                Hadith(
                    id = 901, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = true,
                    chapterId = 1, chapterBn = "আমল ও আখলাক অধ্যায়", chapterEn = "Virtuous Conduct",
                    topicBn = "আখলাক ও উত্তম চরিত্র", topicEn = "Good Character", hadithNumber = 1,
                    narrator = "আবু হুরায়রা (রা.)",
                    arabicText = "الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ",
                    translationBangla = "উত্তম ও মিষ্টি কথা বলাও একটি সদকা।",
                    translationEnglish = "A good word is charity.",
                    grade = "সহীহ", reference = "$nameBn #১", bookNameBn = nameBn, bookNameEn = nameEn
                )
            )
        }
    }

    fun getAllCuratedHadiths(): List<Hadith> {
        val list = mutableListOf<Hadith>()
        for (book in defaultCanonicalBooks) {
            list.addAll(getCoreCuratedHadithsForBook(book.id, book.nameBn, book.nameEn))
        }
        return list
    }

    fun getDailyHadith(calendar: java.util.Calendar = java.util.Calendar.getInstance()): Hadith {
        val allPool = cachedHadiths.values.flatten().ifEmpty { getAllCuratedHadiths() }
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)
        val index = kotlin.math.abs((dayOfYear * 37 + year) % allPool.size)
        return allPool[index]
    }

    suspend fun getRandomDailyHadith(): Hadith? = withContext(Dispatchers.IO) {
        // Draw from the whole loaded corpus (all books), not Bukhari alone.
        val pool = getAllCachedHadiths().ifEmpty {
            getHadithsForBook("bukhari").ifEmpty { getAllCuratedHadiths() }
        }
        if (pool.isNotEmpty()) pool.random() else null
    }

    fun filterHadiths(
        all: List<Hadith>,
        query: String,
        bookId: String,
        topic: String,
        chapterId: Int
    ): List<Hadith> {
        val selectedTopic = canonicalTopicId(topic)
        val queryTokens = normalized(query).split(' ').filter { it.isNotBlank() }
        val topicDefinition = topics.firstOrNull { it.id == selectedTopic }
        return all.filter { hadith ->
            val matchBook = (bookId == "all" || hadith.bookId == bookId)
            val matchChapter = (chapterId == 0 || hadith.chapterId == chapterId)
            // The default all-books/all-topics/empty-query state is the common
            // path. Do not normalize every long translated Hadith there.
            val directTopicMatch = selectedTopic == "all" ||
                canonicalTopicId(hadith.topicBn) == selectedTopic ||
                canonicalTopicId(hadith.topicEn) == selectedTopic
            val matchTopic = directTopicMatch || run {
                val topicHaystack = normalized("${hadith.topicBn} ${hadith.topicEn} ${hadith.chapterBn} ${hadith.chapterEn} ${hadith.translationBangla} ${hadith.translationEnglish}")
                topicDefinition?.searchTerms?.asSequence()?.map(::normalized)?.any(topicHaystack::contains) == true
            }
            // Search is display-language independent: every stored searchable
            // field contributes, and Arabic comparison is diacritic tolerant.
            val matchQuery = queryTokens.isEmpty() || run {
                val haystack = normalized(buildString {
                    append(hadith.arabicText).append(' ')
                    append(hadith.translationBangla).append(' ')
                    append(hadith.translationEnglish).append(' ')
                    append(hadith.narrator).append(' ')
                    append(hadith.collection).append(' ')
                    append(hadith.bookNameBn).append(' ')
                    append(hadith.bookNameEn).append(' ')
                    append(hadith.chapterBn).append(' ')
                    append(hadith.chapterEn).append(' ')
                    append(hadith.topicBn).append(' ')
                    append(hadith.topicEn).append(' ')
                    append(hadith.reference).append(' ')
                    append(hadith.hadithNumber)
                })
                queryTokens.all { haystack.contains(it) }
            }
            matchBook && matchTopic && matchChapter && matchQuery
        }
    }

    fun paginate(hadiths: List<Hadith>, page: Int, pageSize: Int = 25): List<Hadith> {
        val takeCount = (page + 1) * pageSize
        return hadiths.take(takeCount)
    }
}
