package com.example.ui.noorup.hadith

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HadithRepository(private val context: Context) {

    private val gson = Gson()

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
            totalHadithsBn = "১,৮৫৮ হাদিস",
            descriptionBn = "মদিনার আলেমগণের আমল ও প্রাচীনতম নির্ভরযোগ্য হাদিস গ্রন্থ।",
            descriptionEn = "The earliest codified legal text of the Living Sunnah of Madinah.",
            totalChaptersBn = "৬১ অধ্যায়",
            assetFileName = "hadith/muwatta.json"
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
            totalHadithsBn = "১,৮৯৬ হাদিস",
            descriptionBn = "প্রতিদিনের আধ্যাত্মিক উন্নয়ন, আখলাক ও আত্মশুদ্ধির সেরা সংকলন।",
            descriptionEn = "The quintessential handbook for moral cultivation and daily piety.",
            totalChaptersBn = "৩৭১ অধ্যায়",
            assetFileName = "hadith/riyadussalihin.json"
        )
    )

    val availableBooks: List<Book> get() = defaultCanonicalBooks

    fun getCanonicalBooks(): List<Book> = defaultCanonicalBooks

    private val cachedHadiths = mutableMapOf<String, List<Hadith>>()

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

    suspend fun getHadithsForBook(bookId: String): List<Hadith> = withContext(Dispatchers.IO) {
        cachedHadiths[bookId]?.let { return@withContext it }

        val book = defaultCanonicalBooks.find { it.id == bookId }
        val loadedList = mutableListOf<Hadith>()

        if (book != null && book.assetFileName.isNotBlank()) {
            try {
                val jsonString = context.assets.open(book.assetFileName).bufferedReader().use { it.readText() }.trim()
                if (jsonString.isNotEmpty()) {
                    var rawList: List<HadithRawJson>? = null
                    
                    // Try parsing as structured object: { "metadata": ..., "chapters": ..., "hadiths": [...] }
                    if (jsonString.startsWith("{")) {
                        try {
                            val bookObj = gson.fromJson(jsonString, HadithBookFileJson::class.java)
                            rawList = bookObj.hadiths
                        } catch (_: Exception) { }
                    }

                    // If not parsed as object, try parsing as direct list: [ { ... }, { ... } ]
                    if (rawList == null && jsonString.startsWith("[")) {
                        try {
                            val listType = object : TypeToken<List<HadithRawJson>>() {}.type
                            rawList = gson.fromJson(jsonString, listType)
                        } catch (_: Exception) { }
                    }

                    if (!rawList.isNullOrEmpty()) {
                        val items = rawList.mapIndexed { index, raw ->
                            val parsedId = raw.id ?: (index + 1)
                            val numberVal = parseHadithNumber(raw.hadithNumber, parsedId)
                            val finalId = when (bookId) {
                                "bukhari" -> 10000 + parsedId
                                "muslim" -> 20000 + parsedId
                                "tirmidhi" -> 30000 + parsedId
                                "abudawud" -> 40000 + parsedId
                                "nasai" -> 50000 + parsedId
                                "ibnmajah" -> 60000 + parsedId
                                "muwatta" -> 70000 + parsedId
                                "riyadussalihin" -> 80000 + parsedId
                                else -> 90000 + parsedId
                            }
                            Hadith(
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
                        loadedList.addAll(items)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HadithRepo", "Error reading asset ${book.assetFileName}", e)
            }
        }

        // If no items loaded from asset (e.g. empty file or not found), provide core authentic foundation hadiths
        if (loadedList.isEmpty()) {
            loadedList.addAll(getCoreCuratedHadithsForBook(bookId, book?.nameBn ?: "হাদিস", book?.nameEn ?: "Hadith"))
        }

        cachedHadiths[bookId] = loadedList
        loadedList
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
            else -> listOf(
                Hadith(
                    id = 701, bookId = bookId, collection = nameBn, isMuttafaqunAlayh = true,
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

    suspend fun getRandomDailyHadith(): Hadith? = withContext(Dispatchers.IO) {
        val bukhariHadiths = getHadithsForBook("bukhari")
        if (bukhariHadiths.isNotEmpty()) bukhariHadiths.random() else null
    }

    fun filterHadiths(
        all: List<Hadith>,
        query: String,
        bookId: String,
        topic: String,
        chapterId: Int
    ): List<Hadith> {
        return all.filter { hadith ->
            val matchBook = (bookId == "all" || hadith.bookId == bookId)
            val matchTopic = (topic == "সকল বিষয়" || topic == "All Topics" || hadith.topicBn.contains(topic) || hadith.topicEn.contains(topic, true))
            val matchChapter = (chapterId == 0 || hadith.chapterId == chapterId)
            val matchQuery = if (query.isBlank()) true else {
                hadith.translationBangla.contains(query, true) ||
                hadith.translationEnglish.contains(query, true) ||
                hadith.arabicText.contains(query) ||
                hadith.narrator.contains(query, true) ||
                hadith.hadithNumber.toString().contains(query) ||
                hadith.collection.contains(query, true)
            }
            matchBook && matchTopic && matchChapter && matchQuery
        }
    }

    fun paginate(hadiths: List<Hadith>, page: Int, pageSize: Int = 25): List<Hadith> {
        val takeCount = (page + 1) * pageSize
        return hadiths.take(takeCount)
    }
}
