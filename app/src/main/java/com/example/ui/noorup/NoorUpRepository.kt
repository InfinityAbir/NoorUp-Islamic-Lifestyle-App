package com.example.ui.noorup

data class SurahInfo(
    val number: Int,
    val nameBn: String,
    val nameEn: String,
    val arabicName: String,
    val totalVerses: Int,
    val revelationTypeBn: String
)

object NoorUpRepository {

    val cities = emptyList<CityLocation>()

    val prayerTimes = listOf(
        PrayerTime("ফজর", "Fajr", "الفجر", "04:48 AM", "05:59 AM", false, true, false),
        PrayerTime("সূর্যোদয়", "Sunrise", "الشروق", "05:59 AM", "06:15 AM", false, true, true, "সূর্য ওঠার সময় নামাজ নিষিদ্ধ", "Prohibited during sunrise"),
        PrayerTime("যোহর", "Dhuhr", "الظهر", "12:05 PM", "04:22 PM", true, false, false),
        PrayerTime("আসর", "Asr", "العصر", "04:22 PM", "06:18 PM", false, false, false),
        PrayerTime("মাগরিব", "Maghrib", "المغرب", "06:18 PM", "07:33 PM", false, false, false),
        PrayerTime("এশা", "Isha", "العشاء", "07:33 PM", "04:48 AM", false, false, false),
        PrayerTime("তাহাজ্জুদ", "Tahajjud", "التهجد", "03:00 AM", "04:30 AM", false, false, false)
    )

    private val surahInfoList = listOf(
        SurahInfo(1, "আল-ফাতিহা", "Al-Fatihah", "الفاتحة", 7, "মাক্কী"),
        SurahInfo(2, "আল-বাকারা", "Al-Baqarah", "البقرة", 286, "মাদানী"),
        SurahInfo(3, "আলে ইমরান", "Ali 'Imran", "آل عمران", 200, "মাদানী"),
        SurahInfo(4, "আন-নিসা", "An-Nisa'", "النساء", 176, "মাদানী"),
        SurahInfo(5, "আল-মািদাহ", "Al-Ma'idah", "المائدة", 120, "মাদানী"),
        SurahInfo(6, "আল-আনআম", "Al-An'am", "الأنعام", 165, "মাক্কী"),
        SurahInfo(7, "আল-আরাফ", "Al-A'raf", "الأعراف", 206, "মাক্কী"),
        SurahInfo(8, "আল-আনফাল", "Al-Anfal", "الأنفال", 75, "মাদানী"),
        SurahInfo(9, "আত-তওবা", "At-Tawbah", "التوبة", 129, "মাদানী"),
        SurahInfo(10, "ইউনুস", "Yunus", "يونس", 109, "মাক্কী"),
        SurahInfo(11, "হুদ", "Hud", "هود", 123, "মাক্কী"),
        SurahInfo(12, "ইউসুফ", "Yusuf", "يوسف", 111, "মাক্কী"),
        SurahInfo(13, "আর-রাদ", "Ar-Ra'd", "الرعد", 43, "মাদানী"),
        SurahInfo(14, "ইব্রাহিম", "Ibrahim", "إبراهيم", 52, "মাক্কী"),
        SurahInfo(15, "আল-হিজর", "Al-Hijr", "الحجر", 99, "মাক্কী"),
        SurahInfo(16, "আন-নাহল", "An-Nahl", "النحل", 128, "মাক্কী"),
        SurahInfo(17, "বনি ইসরাঈল", "Al-Isra", "الإسراء", 111, "মাক্কী"),
        SurahInfo(18, "আল-কাহফ", "Al-Kahf", "الكهف", 110, "মাক্কী"),
        SurahInfo(19, "মারিয়াম", "Maryam", "مريم", 98, "মাক্কী"),
        SurahInfo(20, "ত্বোয়া-হা", "Ta-Ha", "طه", 135, "মাক্কী"),
        SurahInfo(21, "আল-আম্বিয়া", "Al-Anbiya'", "الأنبياء", 112, "মাক্কী"),
        SurahInfo(22, "আল-হজ", "Al-Hajj", "الحج", 78, "মাদানী"),
        SurahInfo(23, "আল-মুমিনুন", "Al-Mu'minun", "المؤمنون", 118, "মাক্কী"),
        SurahInfo(24, "আন-নূর", "An-Nur", "النور", 64, "মাদানী"),
        SurahInfo(25, "আল-ফুরকান", "Al-Furqan", "الفرقان", 77, "মাক্কী"),
        SurahInfo(26, "আশ-শুআরা", "Ash-Shu'ara'", "الشعراء", 227, "মাক্কী"),
        SurahInfo(27, "আন-নামল", "An-Naml", "النمل", 93, "মাক্কী"),
        SurahInfo(28, "আল-কাসাস", "Al-Qasas", "القصص", 88, "মাক্কী"),
        SurahInfo(29, "আল-আনকাবুত", "Al-'Ankabut", "العنكبوت", 69, "মাক্কী"),
        SurahInfo(30, "আর-রূম", "Ar-Rum", "الروم", 60, "মাক্কী"),
        SurahInfo(31, "লুকমান", "Luqman", "لقمان", 34, "মাক্কী"),
        SurahInfo(32, "আস-সাজদাহ", "As-Sajdah", "السجدة", 30, "মাক্কী"),
        SurahInfo(33, "আল-আহযাব", "Al-Ahzab", "الأحزاب", 73, "মাদানী"),
        SurahInfo(34, "সাবা", "Saba'", "سبأ", 54, "মাক্কী"),
        SurahInfo(35, "ফাতির", "Fatir", "فاطر", 45, "মাক্কী"),
        SurahInfo(36, "ইয়াসীন", "Ya-Sin", "يس", 83, "মাক্কী"),
        SurahInfo(37, "আস-সাফফাত", "As-Saffat", "الصافات", 182, "মাক্কী"),
        SurahInfo(38, "সোয়াদ", "Sad", "ص", 88, "মাক্কী"),
        SurahInfo(39, "আজ-যুমার", "Az-Zumar", "الزمر", 75, "মাক্কী"),
        SurahInfo(40, "আল-মুমিন", "Ghafir", "غافر", 85, "মাক্কী"),
        SurahInfo(41, "ফুসসিলাত", "Fussilat", "فصلت", 54, "মাক্কী"),
        SurahInfo(42, "আশ-শূরা", "Ash-Shura", "الشورى", 53, "মাক্কী"),
        SurahInfo(43, "আজ-যুখরুফ", "Az-Zukhruf", "الزخرف", 89, "মাক্কী"),
        SurahInfo(44, "আদ-দুখান", "Ad-Dukhan", "الدخان", 59, "মাক্কী"),
        SurahInfo(45, "আল-জাসিয়াহ", "Al-Jathiyah", "الجاثية", 37, "মাক্কী"),
        SurahInfo(46, "আল-আহকাফ", "Al-Ahqaf", "الأحقاف", 35, "মাক্কী"),
        SurahInfo(47, "মুহাম্মদ", "Muhammad", "محمد", 38, "মাদানী"),
        SurahInfo(48, "আল-ফাতহ", "Al-Fath", "الفتح", 29, "মাদানী"),
        SurahInfo(49, "আল-হুজুরাত", "Al-Hujurat", "الحجرات", 18, "মাদানী"),
        SurahInfo(50, "ক্বাফ", "Qaf", "ق", 45, "মাক্কী"),
        SurahInfo(51, "আয-যারিয়াত", "Adh-Dhariyat", "الذاريات", 60, "মাক্কী"),
        SurahInfo(52, "আত-তূর", "At-Tur", "الطور", 49, "মাক্কী"),
        SurahInfo(53, "আন-নাজম", "An-Najm", "النجم", 62, "মাক্কী"),
        SurahInfo(54, "আল-ক্বমার", "Al-Qamar", "القمر", 55, "মাক্কী"),
        SurahInfo(55, "আর-রহমান", "Ar-Rahman", "الرحمن", 78, "মাদানী"),
        SurahInfo(56, "আল-ওয়াকিয়া", "Al-Waqi'ah", "الواقعة", 96, "মাক্কী"),
        SurahInfo(57, "আল-হাদিদ", "Al-Hadid", "الحديد", 29, "মাদানী"),
        SurahInfo(58, "আল-মুজাদালাহ", "Al-Mujadilah", "المجادلة", 22, "মাদানী"),
        SurahInfo(59, "আল-হাশর", "Al-Hashr", "الحشر", 24, "মাদানী"),
        SurahInfo(60, "আল-মুমতাহানাহ", "Al-Mumtahanah", "الممتحنة", 13, "মাদানী"),
        SurahInfo(61, "আস-সাফ", "As-Saff", "الصف", 14, "মাদানী"),
        SurahInfo(62, "আল-জুমুআহ", "Al-Jumu'ah", "الجمعة", 11, "মাদানী"),
        SurahInfo(63, "আল-মুনাফিকুন", "Al-Munafiqun", "المنافقون", 11, "মাদানী"),
        SurahInfo(64, "আত-তাগাবুন", "At-Taghabun", "التغابن", 18, "মাদানী"),
        SurahInfo(65, "আত-ত্বালাক", "At-Talaq", "الطلاق", 12, "মাদানী"),
        SurahInfo(66, "আত-তাহরীম", "At-Tahrim", "التحريم", 12, "মাদানী"),
        SurahInfo(67, "আল-মুলক", "Al-Mulk", "الملك", 30, "মাক্কী"),
        SurahInfo(68, "আল-কলম", "Al-Qalam", "القلم", 52, "মাক্কী"),
        SurahInfo(69, "আল-হাক্বক্বাহ", "Al-Haqqah", "الحاقة", 52, "মাক্কী"),
        SurahInfo(70, "আল-মাআরিজ", "Al-Ma'arij", "المعارج", 44, "মাক্কী"),
        SurahInfo(71, "নূহ", "Nuh", "نوح", 28, "মাক্কী"),
        SurahInfo(72, "আল-জিন", "Al-Jinn", "الجن", 28, "মাক্কী"),
        SurahInfo(73, "আল-মুযযাম্মিল", "Al-Muzzammil", "المزمل", 20, "মাক্কী"),
        SurahInfo(74, "আল-মুদ্দাসসির", "Al-Muddaththir", "المدثر", 56, "মাক্কী"),
        SurahInfo(75, "আল-ক্বিয়ামাহ", "Al-Qiyamah", "القيامة", 40, "মাক্কী"),
        SurahInfo(76, "আল-ইনসান", "Al-Insan", "الإنسان", 31, "মাদানী"),
        SurahInfo(77, "আল-মুরসালাত", "Al-Mursalat", "المرسلات", 50, "মাক্কী"),
        SurahInfo(78, "আন-নাবা", "An-Naba'", "النبأ", 40, "মাক্কী"),
        SurahInfo(79, "আন-নাযিয়াত", "An-Nazi'at", "النازعات", 46, "মাক্কী"),
        SurahInfo(80, "আবাসা", "'Abasa", "عبس", 42, "মাক্কী"),
        SurahInfo(81, "আত-তাকভীর", "At-Takwir", "التكوير", 29, "মাক্কী"),
        SurahInfo(82, "আল-ইনফিতার", "Al-Infitar", "الانفطار", 19, "মাক্কী"),
        SurahInfo(83, "আল-মুতাফফিফীন", "Al-Mutaffifin", "المطففين", 36, "মাক্কী"),
        SurahInfo(84, "আল-ইনশিকাক", "Al-Inshiqaq", "الانشقاق", 25, "মাক্কী"),
        SurahInfo(85, "আল-বুরুজ", "Al-Buruj", "البروج", 22, "মাক্কী"),
        SurahInfo(86, "আত-তারিক্ব", "At-Tariq", "الطارق", 17, "মাক্কী"),
        SurahInfo(87, "আল-আলা", "Al-A'la", "الأعلى", 19, "মাক্কী"),
        SurahInfo(88, "আল-ঘাতশিয়াহ", "Al-Ghashiyah", "الغاشية", 26, "মাক্কী"),
        SurahInfo(89, "আল-ফজর", "Al-Fajr", "الفجر", 30, "মাক্কী"),
        SurahInfo(90, "আল-বালাদ", "Al-Balad", "البلد", 20, "মাক্কী"),
        SurahInfo(91, "আশ-শামস", "Ash-Shams", "الشمس", 15, "মাক্কী"),
        SurahInfo(92, "আল-লাইল", "Al-Layl", "اللَّيل", 21, "মাক্কী"),
        SurahInfo(93, "আদ-দুহা", "Ad-Duha", "الضحى", 11, "মাক্কী"),
        SurahInfo(94, "আল-ইনশিরাh", "Ash-Sharh", "الشرح", 8, "মাক্কী"),
        SurahInfo(95, "আত-তীন", "At-Tin", "التين", 8, "মাক্কী"),
        SurahInfo(96, "আল-আলাক", "Al-'Alaq", "العلق", 19, "মাক্কী"),
        SurahInfo(97, "আল-কদর", "Al-Qadr", "القدر", 5, "মাক্কী"),
        SurahInfo(98, "আল-বাই্যিনাহ", "Al-Bayyinah", "البينة", 8, "মাদানী"),
        SurahInfo(99, "আল-যিলযাল", "Az-Zalzalah", "الزلزلة", 8, "মাদানী"),
        SurahInfo(100, "আল-আদিয়াত", "Al-'Adiyat", "العاديات", 11, "মাক্কী"),
        SurahInfo(101, "আল-কারিআহ", "Al-Qari'ah", "القارعة", 11, "মাক্কী"),
        SurahInfo(102, "আত-তাকাসুর", "At-Takathur", "التكاثر", 8, "মাক্কী"),
        SurahInfo(103, "আল-আসর", "Al-'Asr", "العصر", 3, "মাক্কী"),
        SurahInfo(104, "আল-হুমাযাহ", "Al-Humazah", "الهمزة", 9, "মাক্কী"),
        SurahInfo(105, "আল-ফীল", "Al-Fil", "الفيل", 5, "মাক্কী"),
        SurahInfo(106, "কুরাইশ", "Quraish", "قريش", 4, "মাক্কী"),
        SurahInfo(107, "আল-মাউন", "Al-Ma'un", "الماعون", 7, "মাক্কী"),
        SurahInfo(108, "আল-কাউসার", "Al-Kawthar", "الكوثر", 3, "মাক্কী"),
        SurahInfo(109, "আল-কাফিরুন", "Al-Kafirun", "الكافرون", 6, "মাক্কী"),
        SurahInfo(110, "আন-নাসর", "An-Nasr", "النصر", 3, "মাদানী"),
        SurahInfo(111, "আল-মাসাদ", "Al-Masad", "المسد", 5, "মাক্কী"),
        SurahInfo(112, "আল-ইখলাস", "Al-Ikhlas", "الإخلاص", 4, "মাক্কী"),
        SurahInfo(113, "আল-ফালাক", "Al-Falaq", "الفلق", 5, "মাক্কী"),
        SurahInfo(114, "আন-নাস", "An-Nas", "الناس", 6, "মাক্কী")
    )

    val surahs: List<Surah> by lazy {
        surahInfoList.map { info ->
            val count = if (info.totalVerses < 5) info.totalVerses else 5
            val versesList = (1..count).map { vNum ->
                if (info.number == 1) {
                    fatihahVerses[vNum - 1]
                } else if (info.number == 112) {
                    when(vNum) {
                        1 -> Verse(1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "বলুন, তিনি আল্লাহ, এক ও অদ্বিতীয়।", "Say, \"He is Allah, [who is] One,")
                        2 -> Verse(2, "اللَّهُ الصَّمَدُ", "আল্লাহ কারো মুখাপেক্ষী নন, সবাই তাঁর মুখাপেক্ষী।", "Allah, the Eternal Refuge.")
                        3 -> Verse(3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "তিনি কাউকে জন্ম দেননি এবং তাঁকেও জন্ম দেওয়া হয়নি।", "He neither begets nor is born,")
                        else -> Verse(4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "আর তাঁর সমতুল্য কেউ নেই।", "Nor is there to Him any equivalent.")
                    }
                } else {
                    Verse(
                        number = vNum,
                        arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ (آية $vNum)",
                        banglaTranslation = "সূরা ${info.nameBn} এর $vNum নং আয়াত।",
                        englishTranslation = "Verse $vNum of Surah ${info.nameEn}."
                    )
                }
            }

            Surah(
                number = info.number,
                nameBangla = info.nameBn,
                nameEnglish = info.nameEn,
                nameArabic = info.arabicName,
                totalVerses = info.totalVerses,
                revelationTypeBangla = info.revelationTypeBn,
                revelationTypeEnglish = if (info.revelationTypeBn == "মাক্কী") "Makki" else "Madani",
                verses = versesList
            )
        }
    }

    private val fatihahVerses = listOf(
        Verse(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "শুরু করছি আল্লাহর নামে যিনি পরম দয়ালু, অতি মেহেরবান।", "In the name of Allah, the Entirely Merciful, the Especially Merciful."),
        Verse(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "সমস্ত প্রশংসা আল্লাহ তাআলার জন্য, যিনি সমস্ত সৃষ্টিজগতের পালনকর্তা।", "[All] praise is [due] to Allah, Lord of the worlds -"),
        Verse(3, "الرَّحْمَٰنِ الرَّحِيمِ", "যিনি পরম দয়ালু ও অতি মেহেরবান।", "The Entirely Merciful, the Especially Merciful,"),
        Verse(4, "مَالِكِ يَوْمِ الدِّينِ", "বিচার দিনের মালিক।", "Sovereign of the Day of Recompense."),
        Verse(5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "আমরা কেবল আপনারই ইবাদত করি এবং কেবল আপনারই সাহায্য প্রার্থনা করি।", "It is You we worship and You we ask for help."),
        Verse(6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "আমাদের সরল পথ প্রদর্শন করুন।", "Guide us to the straight path -"),
        Verse(7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهمْ وَلَا الضَّالِّينَ", "তাদের পথ, যাদের প্রতি আপনি নিয়ামত দান করেছেন; তাদের পথ নয়, যারা গযবপ্রাপ্ত এবং পথভ্রষ্ট নয়।", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.")
    )

    val duas = listOf(
        // 1. ঘুম ও জাগরণ (Sleeping & Waking)
        Dua(
            1,
            "সকাল ও সন্ধ্যা",
            "Morning & Evening",
            "ঘুম থেকে ওঠার পর পড়ার দুআ",
            "Dua Upon Waking Up",
            "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            "আলহামদু লিল্লাহিল্লাযী আহয়ানা বা’দা মা আমাতানা ওয়া ইলাইহিন নুশুর।",
            "Alhamdu lillahilladhi ahyana ba'da ma amatana wa ilaihin-nushur.",
            "সমস্ত প্রশংসা আল্লাহর জন্য, যিনি আমাদের মৃত্যুর (ঘুমের) পর পুনরায় জীবিত করলেন এবং তাঁরই সমীপে সকলের পুনরুত্থান।",
            "All praise is for Allah who gave us life after having taken it from us and unto Him is the resurrection.",
            "সহীহ বুখারী: ৬৩১২, সহীহ মুসলিম: ২৭১১"
        ),
        Dua(
            2,
            "সকাল ও সন্ধ্যা",
            "Morning & Evening",
            "ঘুমানোর পূর্বে পড়ার দুআ",
            "Dua Before Sleeping",
            "اللَّهُمَّ بِاسْمِكَ أَمُوتُ وَأَحْيَا",
            "আল্লাহুম্মা বিসমিকা আমূতু ওয়া আহয়া।",
            "Allahumma bismika amootu wa ahya.",
            "হে আল্লাহ! আপনারই নামে আমি মৃত্যুবরণ (ঘুমাই) করি এবং জীবিত (জাগ্রত) হই।",
            "O Allah, in Your name I die and I live.",
            "সহীহ বুখারী: ৬৩২৪, সহীহ মুসলিম: ২৭১১"
        ),
        Dua(
            3,
            "সকাল ও সন্ধ্যা",
            "Morning & Evening",
            "সকাল-সন্ধ্যার বিশেষ সুরক্ষার দুআ",
            "Morning & Evening Protection",
            "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
            "আসবাহনা ওয়া আসবাহাল মুলকু লিল্লাহ, ওয়ালহামদুলিল্লাহ, লা ইলাহা ইল্লাল্লাহু ওয়াহদাহু লা শারীকা লাহু।",
            "Asbahna wa asbahal-mulku lillah, wal-hamdu lillah, la ilaha illallahu wahdahu la sharika lah.",
            "আমরা সকালে উপনীত হয়েছি এবং নিখিল জাহানের রাজত্বও আল্লাহর জন্যই সকালে উপনীত হয়েছে; সমস্ত প্রশংসা আল্লাহর, আল্লাহ ব্যতীত কোনো সত্য উপাস্য নেই, তিনি একক, তাঁর কোনো অংশীদার নেই।",
            "We have entered the morning and the kingdom belongs to Allah, praise be to Allah. There is no god except Allah alone, without partner.",
            "সহীহ মুসলিম: ২৭২৩"
        ),

        // 2. সুরক্ষা ও নিরাময় (Protection & Healing)
        Dua(
            4,
            "সুরক্ষা",
            "Protection",
            "বিপদ ও অনিষ্ট থেকে সুরক্ষার দুআ",
            "Dua for Protection from Harm",
            "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            "বিসমিল্লাহিল্লাযী লা ইয়াদুররু মা’আ ইসমিহী শাইউন ফিল আরদি ওয়া লা ফিস সামা’ই ওয়াহুওয়াস সামীউল আলীম।",
            "Bismillahilladhi la yadurru ma'asmihi shay'un fil-ardi wa la fis-sama'i wa huwas-sami'ul-'alim.",
            "আল্লাহর নামে, যাঁর নামের বরকতে আসমান ও জমিনের কোনো কিছুই ক্ষতি করতে পারে না। তিনি সর্বশ্রোতা, সর্বজ্ঞ। (সকাল-সন্ধ্যায় ৩ বার)",
            "In the name of Allah, with whose name nothing on earth or in the heavens can cause harm. And He is the Hearing, the Knowing. (3 times)",
            "সুনান আত-তিরমিযী: ৩৩৮৮, সুনান আবু দাউদ: ৫০৮৮ (সহীহ)"
        ),
        Dua(
            5,
            "সুরক্ষা",
            "Protection",
            "কঠিন রোগ-ব্যাধি ও মহামারী থেকে মুক্তির দুআ",
            "Dua for Protection from Severe Illness",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْبَرَصِ، وَالْجُنُونِ، وَالْجُذَامِ، وَمِنْ سَيِّئِ الْأَسْقَامِ",
            "আল্লাহুম্মা ইন্নী আ’ঊযু বিকা মিনাল বারাসি, ওয়াল জুনূনি, ওয়াল জুযামি, ওয়া মিন সাইয়িইল আসক্বা-ম।",
            "Allahumma inni a'udhu bika minal-barasi wal-jununi wal-judhami wa min sayyi'il-asqam.",
            "হে আল্লাহ! আমি আপনার নিকট শেতি (শ্বেতী), উন্মাদনা, কুষ্ঠরোগ এবং সকল প্রকার মারাত্মক ব্যাধি থেকে আশ্রয় প্রার্থনা করছি।",
            "O Allah, I seek refuge in You from vitiligo, madness, leprosy, and evil diseases.",
            "সুনান আবু দাউদ: ১৫৫৪, সুনান নাসাঈ: ৫৪৯৩ (সহীহ)"
        ),
        Dua(
            6,
            "সুরক্ষা",
            "Protection",
            "শয়তান ও ক্ষতিকর প্রাণী থেকে আশ্রয়",
            "Seeking Refuge in Allah's Perfect Words",
            "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            "আ’ঊযু বিকালিমা-তিল্লাহিত তা-ম্মা-তি মিন শাররি মা খালাক্ব।",
            "A'udhu bikalimatil-lahit-tammati min sharri ma khalaq.",
            "আমি আল্লাহর পরিপূর্ণ বাণীর সাহায্যে তাঁর সৃষ্ট সকল অনিষ্ট থেকে আশ্রয় চাইছি।",
            "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            "সহীহ মুসলিম: ২৭০৮"
        ),
        Dua(
            7,
            "সুরক্ষা",
            "Protection",
            "অসুস্থ ব্যক্তির জন্য রোগমুক্তির দুআ",
            "Dua for Sick Person",
            "أَذْهِبِ الْبَاسَ رَبَّ النَّاسِ، وَاشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا",
            "আযহিবিল বা’সা রাব্বান নাস, ওয়াশফি আন্তাশ শাফী, লা শিফা-আ ইল্লা শিফা-উকা, শিফা-আন লা ইউগাদিরু সাক্বামা।",
            "Adh-hibil-ba'sa rabban-nas, washfi antash-shafi, la shifa'a illa shifa'uk, shifa'an la yughadiru saqama.",
            "হে মানুষের রব! আপনি এই রোগ দূর করুন এবং আরোগ্য দান করুন; আপনিই প্রকৃত আরোগ্যকারী। আপনার আরোগ্য ব্যতীত কোনো আরোগ্য নেই, এমন আরোগ্য দিন যা কোনো ব্যাধি অবশিষ্ট না রাখে।",
            "Remove the hardship, O Lord of mankind, and heal, for You are the Healer. There is no cure except Your cure, a cure that leaves no disease behind.",
            "সহীহ বুখারী: ৫৬৭৫, সহীহ মুসলিম: ২১৯১"
        ),

        // 3. তওবা ও ক্ষমা (Repentance & Forgiveness)
        Dua(
            8,
            "ক্ষমা ও তাওবা",
            "Forgiveness",
            "সাইয়্যিদুল ইস্তিগফার (শ্রেষ্ঠ ক্ষমাপ্রার্থনার দুআ)",
            "Sayyidul Istighfar (Chief of Prayers for Forgiveness)",
            "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            "আল্লাহুম্মা আন্তা রব্বী লা ইলা-হা ইল্লা আন্তা, খালাক্বতানী ওয়া আনা ‘আবদুকা, ওয়া আনা ‘আলা ‘আহদিকা ওয়া ওয়া’দিকা মাসতাত্বা’তু, আ’ঊযু বিকা মিন শাররি মা সানা’তু, আবূউ লাকা বিনি’মাতিকা ‘আলাইয়্যা, ওয়া আবূউ লাকা বিযাম্বী ফাগফির লী, ফাইন্নাহূ লা ইয়াগফিরুয যুনূবা ইল্লা আন্তা।",
            "Allahumma anta rabbi la ilaha illa anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bini'matika 'alayya, wa abu'u laka bidhanbi faghfir li, fa'innahu la yaghfirudh-dhunuba illa anta.",
            "হে আল্লাহ! আপনি আমার রব, আপনি ছাড়া কোনো উপাস্য নেই। আপনি আমাকে সৃষ্টি করেছেন এবং আমি আপনার বান্দা। আমি আমার সাধ্যানুযায়ী আপনার অঙ্গীকার ও প্রতিশ্রুতির উপর প্রতিষ্ঠিত আছি। আমি আমার কৃতকর্মের অনিষ্ট থেকে আপনার আশ্রয় চাইছি। আমার প্রতি আপনার নিয়ামত আমি স্বীকার করছি এবং আমার পাপও স্বীকার করছি। অতএব আমাকে ক্ষমা করুন, কেননা আপনি ছাড়া আর কেউ পাপ ক্ষমা করতে পারে না।",
            "O Allah, You are my Lord, there is no god but You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me, and I acknowledge my sin, so forgive me, for none forgives sins except You.",
            "সহীহ বুখারী: ৬৩০৬"
        ),
        Dua(
            9,
            "ক্ষমা ও তাওবা",
            "Forgiveness",
            "তাওবা ও সার্বক্ষণিক ক্ষমা প্রার্থনার দুআ",
            "Seeking Forgiveness",
            "أَسْتَغْفِرُ اللَّهَ الَّذِي لَا إِلَهَ إِلَّا هُوَ الْحَيَّ الْقَيُّومَ وَأَتُوبُ إِلَيْهِ",
            "আস্তাগফিরুল্লা-হাল্লাযী লা ইলা-হা ইল্লা হুওয়াল হাইয়ুল ক্বাইয়ূমু ওয়া আতূবু ইলাইহ।",
            "Astaghfirullahalladhi la ilaha illa huwal-hayyul-qayyumu wa atubu ilayh.",
            "আমি সেই আল্লাহর কাছে ক্ষমা প্রার্থনা করছি যিনি ছাড়া কোনো উপাস্য নেই, যিনি চিরঞ্জীব, চিরস্থায়ী এবং আমি তাঁর দিকেই প্রত্যাবর্তন করছি।",
            "I seek forgiveness of Allah, besides Whom there is no God, the Ever-Living, the Eternal Guardian, and I turn to Him in repentance.",
            "সুনান আত-তিরমিযী: ৩৫৭৭, সুনান আবু দাউদ: ১৫১৭ (সহীহ)"
        ),
        Dua(
            10,
            "ক্ষমা ও তাওবা",
            "Forgiveness",
            "নামাজের শেষ বৈঠকে তাশাহহুদের পর ক্ষমার দুআ",
            "Dua in Tashahhud for Forgiveness",
            "اللَّهُمَّ إِنِّي ظَلَمْتُ نَفْسِي ظُلْمًا كَثِيرًا، وَلَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ، فَاغْفِرْ لِي مَغْفِرَةً مِنْ عِنْدِكَ وَارْحَمْنِي، إِنَّكَ أَنْتَ الْغَفُورُ الرَّحِيمُ",
            "আল্লাহুম্মা ইন্নী যালামতু নাফসী যুলমান কাসীরাওঁ ওয়া লা ইয়াগফিরুয যুনূবা ইল্লা আন্তা, ফাগফির লী মাগফিরাতাম মিন ‘ইনদিকা ওয়ারহামনী, ইন্নাকা আন্তাল গাফূরুর রহীম।",
            "Allahumma inni zalamtu nafsi zulman kathiran, wa la yaghfirudh-dhunuba illa anta, faghfir li maghfiratan min 'indika warhamni, innaka antal-ghafurur-rahim.",
            "হে আল্লাহ! আমি আমার নিজের উপর অনেক বেশি অন্যায় করেছি, আর আপনি ব্যতীত পাপ ক্ষমা করার কেউ নেই। অতএব আপনার পক্ষ থেকে বিশেষ ক্ষমা দ্বারা আমাকে ক্ষমা করুন এবং আমার উপর রহম করুন। নিশ্চয়ই আপনি অতীব ক্ষমাশীল, পরম দয়ালু।",
            "O Allah, I have greatly wronged myself, and none forgives sins except You, so grant me forgiveness from You and have mercy upon me. Truly You are the Forgiving, the Merciful.",
            "সহীহ বুখারী: ৮৩৪, সহীহ মুসলিম: ২৭০৫"
        ),

        // 4. দুশ্চিন্তা ও ঋণমুক্তি (Anxiety & Debt Relief)
        Dua(
            11,
            "উদ্বেগ ও দুশ্চিন্তা",
            "Distress & Anxiety",
            "দুশ্চিন্তা, ঋণ ও অলসতা থেকে মুক্তির দুআ",
            "Dua Against Anxiety, Debt & Helplessness",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            "আল্লাহুম্মা ইন্নী আ’ঊযু বিকা মিনাল হাম্মি ওয়াল হাযানি, ওয়াল ‘আজযি ওয়াল কাসালি, ওয়াল বুখলি ওয়াল জুবনি, ওয়া দ্বালা’ইদ দাইনি ওয়া গালাবাতির রিজা-ল।",
            "Allahumma inni a'udhu bika minal-hammi wal-hazani, wal-'ajzi wal-kasali, wal-bukhli wal-jubni, wa dala'id-dayni wa ghalabatir-rijal.",
            "হে আল্লাহ! আমি আপনার নিকট দুশ্চিন্তা ও পেরেশানি থেকে, অক্ষমতা ও অলসতা থেকে, কৃপণতা ও কাপুরুষতা থেকে এবং ঋণের বোঝা ও মানুষের আধিপত্য থেকে আশ্রয় চাইছি।",
            "O Allah, I seek refuge in You from grief and sadness, from weakness and laziness, from miserliness and cowardice, from being overcome by debt and overpowered by men.",
            "সহীহ বুখারী: ২৮৯৩, সহীহ মুসলিম: ২৭০৬"
        ),
        Dua(
            12,
            "উদ্বেগ ও দুশ্চিন্তা",
            "Distress & Anxiety",
            "বিপদ ও চরম সংকটে ইউনুস (আ.) এর দুআ (দোয়া ইউনুস)",
            "Dua of Prophet Yunus (Dua Yunus)",
            "لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            "লা ইলা-হা ইল্লা আন্তা সুবহানাকা ইন্নী কুনতু মিনায য-লিমীন।",
            "La ilaha illa anta subhanaka inni kuntu minaz-zalimin.",
            "আপনি ছাড়া কোনো সত্য উপাস্য নেই, আপনি পবিত্র মহান। নিশ্চয়ই আমি অপরাধীদের অন্তর্ভুক্ত হয়েছি।",
            "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            "সূরা আল-আম্বিয়া: ৮৭, সুনান আত-তিরমিযী: ৩৫০৫ (সহীহ)"
        ),
        Dua(
            13,
            "উদ্বেগ ও দুশ্চিন্তা",
            "Distress & Anxiety",
            "কঠিন পরিস্থিতি সহজ হওয়ার দুআ",
            "Dua for Easing Hardships",
            "اللَّهُمَّ لَا سَهْلَ إِلَّا مَا جَعَلْتَهُ سَهْلًا، وَأَنْتَ تَجْعَلُ الْحَزْنَ إِذَا شِئْتَ سَهْلًا",
            "আল্লাহুম্মা লা সাহলা ইল্লা মা জা’আলতাহূ সাহলা, ওয়া আন্তা তাজ’আলুল হাযনা ইযা শি’তা সাহলা।",
            "Allahumma la sahla illa ma ja'altahu sahla, wa anta taj'alul-hazna idha shi'ta sahla.",
            "হে আল্লাহ! আপনি যা সহজ করে দেন তা ছাড়া কোনো কিছুই সহজ নয়; আর আপনি চাইলে যে কোনো কঠিন বিষয়কেও সহজ করে দিতে পারেন।",
            "O Allah, nothing is easy except what You make easy, and You make the sorrow easy if You wish.",
            "সহীহ ইবনে হিব্বান: ৯৭৪ (সহীহ)"
        ),

        // 5. রিযিক ও বরকত (Provision & Barakah)
        Dua(
            14,
            "রিযিক ও বরকত",
            "Sustenance",
            "হালাল রিযিকের প্রশস্ততা ও ঋণমুক্তির দুআ",
            "Dua for Halal Wealth & Freedom from Debt",
            "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ، وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ",
            "আল্লাহুম্মাকফিনী বিহালা-লিকা ‘আন হারা-মিকা, ওয়া আগনিনী বিফাদলিকা ‘আম্মান সিওয়া-ক।",
            "Allahummakfini bihalalika 'an haramika, wa aghnini bifadlika 'amman siwak.",
            "হে আল্লাহ! আপনার হালাল রিজিকের মাধ্যমে আমাকে আপনার হারাম থেকে বাঁচিয়ে যথেষ্ট করুন এবং আপনার অশেষ করুণা দিয়ে আমাকে আপনি ছাড়া অন্যদের মুখাপেক্ষীহীন করুন।",
            "O Allah, suffice me with Your lawful against Your prohibited, and make me independent of all besides You by Your bounty.",
            "সুনান আত-তিরমিযী: ৩৫৬৩ (হাসান)"
        ),
        Dua(
            15,
            "রিযিক ও বরকত",
            "Sustenance",
            "উপকারী জ্ঞান, উত্তম রিযিক ও কবুল আমলের দুআ",
            "Dua for Beneficial Knowledge, Pure Provision & Accepted Deeds",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
            "আল্লাহুম্মা ইন্নী আসআলুকা ‘ইলমান না-ফি’আওঁ, ওয়া রিযক্বান ত্বাইয়িবাওঁ, ওয়া ‘আমালান মুতাক্বাব্বালা।",
            "Allahumma inni as'aluka 'ilman nafi'an, wa rizqan tayyiban, wa 'amalan mutaqabbala.",
            "হে আল্লাহ! আমি আপনার কাছে উপকারী ইলম (জ্ঞান), পবিত্র জীবিকা এবং কবুলযোগ্য নেক আমল প্রার্থনা করছি। (ফজরের পর পড়ার সুন্নত)",
            "O Allah, I ask You for beneficial knowledge, good (pure) provision, and accepted deeds.",
            "সুনান ইবনে মাজাহ: ৯২৫, সুনান নাসাঈ: ১০২ (সহীহ)"
        ),

        // 6. পরিবার ও সন্তান (Family & Children)
        Dua(
            16,
            "পিতা-মাতা",
            "Parents",
            "পিতা-মাতার মাগফিরাত ও করুণার জন্য দুআ",
            "Dua for Parents' Mercy",
            "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            "রব্বিরহামহুমা কামা রব্বায়ানী সাগীরা।",
            "Rabbir-hamhuma kama rabbayani saghira.",
            "হে আমার পালনকর্তা! তাদের উভয়ের প্রতি দয়া করুন যেভাবে তারা শৈশবে আমাকে স্নেহ-মমতায় লালন-পালন করেছেন।",
            "My Lord, have mercy upon them as they brought me up when I was small.",
            "পবিত্র কুরআন - সূরা আল-ইসরা: ২৪"
        ),
        Dua(
            17,
            "পারিবারিক শান্তি",
            "Home & Family",
            "উত্তম স্ত্রী, সৎ সন্তান ও চোখের শীতলতার দুআ",
            "Dua for Righteous Spouse & Children",
            "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
            "রব্বানা হাব লানা মিন আযওয়াজিনা ওয়া যুররিইয়্যাতিনা কুর্রাতা আ’ইউনিওঁ ওয়াজ’আলনা লিল মুত্তাকীনা ইমা-মা।",
            "Rabbana hab lana min azwajina wa dhurriyyatina qurrata a'yunin waj'alna lil-muttaqina imama.",
            "হে আমাদের রব! আমাদের জন্য এমন স্ত্রী ও সন্তান-সন্ততি দান করুন যারা আমাদের চোখের শীতলতা স্বরূপ হবে এবং আমাদের পরহেযগারদের জন্য আদর্শ নেতা বানান।",
            "Our Lord, grant us from among our wives and offspring comfort to our eyes and make us an example for the righteous.",
            "পবিত্র কুরআন - সূরা আল-ফুরকান: ৭৪"
        ),

        // 7. জ্ঞান ও হেদায়েত (Knowledge & Guidance)
        Dua(
            18,
            "জ্ঞান ও ঈমান",
            "Knowledge & Faith",
            "জ্ঞান ও স্মৃতিশক্তি বৃদ্ধির দুআ",
            "Dua for Increase in Knowledge",
            "رَبِّ زِدْنِي عِلْمًا",
            "রব্বি যিদনী ‘ইলমা।",
            "Rabbi zidni 'ilma.",
            "হে আমার রব! আমার জ্ঞান বৃদ্ধি করে দিন।",
            "O my Lord, increase me in knowledge.",
            "পবিত্র কুরআন - সূরা ত্বোয়া-হা: ১১৪"
        ),
        Dua(
            19,
            "জ্ঞান ও ঈমান",
            "Knowledge & Faith",
            "দ্বীনের উপর অবিচল থাকার দুআ",
            "Dua for Steadfastness in Faith",
            "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            "ইয়া মুক্বাল্লিবাল কুলূবি সাব্বিত ক্বালবী ‘আলা দীনিক।",
            "Ya muqallibal-qulubi thabbit qalbi 'ala dinik.",
            "হে অন্তরসমূহের পরিবর্তনকারী! আমার অন্তরকে আপনার দ্বীনের উপর অবিচল রাখুন।",
            "O Turner of the hearts, keep my heart firm upon Your religion.",
            "সুনান আত-তিরমিযী: ২১৪০, সুনান ইবনে মাজাহ: ৩৮৩৪ (সহীহ)"
        ),

        // 8. সফর ও ঘর (Travel & Home)
        Dua(
            20,
            "সফর",
            "Travel",
            "সফরের বাহনে ওঠার পর দুআ",
            "Dua for Boarding a Vehicle / Travel",
            "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَى رَبِّنَا لَمُنقَلِبُونَ",
            "সুবহা-নাল্লাযী সাখখারা লানা হা-যা ওয়া মা কুন্না লাহূ মুক্বরিনীন, ওয়া ইন্না ইলা রব্বিনা লামুনক্বালিবূন।",
            "Subhanalladhi sakhkhara lana hadha wa ma kunna lahu muqrinin, wa inna ila rabbina lamunqalibun.",
            "পবিত্র ও মহান সেই সত্তা যিনি একে আমাদের বশীভূত করে দিয়েছেন, অথচ আমরা একে বশ করতে পারতাম না। আর নিশ্চয়ই আমরা আমাদের রবের দিকেই প্রত্যাবর্তনকারী।",
            "Glory to Him who has brought this under our control, though we were unable to subdue it by ourselves. And indeed, to our Lord we will surely return.",
            "সহীহ মুসলিম: ১৩৪২, সুনান আবু দাউদ: ২৫৯৯"
        ),
        Dua(
            21,
            "সফর",
            "Travel",
            "ঘর থেকে বের হওয়ার দুআ",
            "Dua When Leaving the House",
            "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ، لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "বিসমিল্লা-হি তাওয়াক্কালতু ‘আলাল্লা-হি, লা হাওলা ওয়ালা কুওয়াতা ইল্লা বিল্লা-হ।",
            "Bismillahi tawakkaltu 'alallahi, la hawla wa la quwwata illa billah.",
            "আল্লাহর নামে বের হচ্ছি, আল্লাহর উপর ভরসা করলাম। আল্লাহর সাহায্য ছাড়া গুনাহ থেকে বাঁচার কোনো উপায় এবং নেক কাজ করার কোনো শক্তি নেই।",
            "In the name of Allah, I place my trust in Allah; there is no power and no strength except with Allah.",
            "সুনান আবু দাউদ: ৫০৯৫, সুনান আত-তিরমিযী: ৩৪২৬ (সহীহ)"
        ),
        Dua(
            22,
            "সফর",
            "Travel",
            "ঘরে প্রবেশের দুআ",
            "Dua When Entering the House",
            "بِسْمِ اللَّهِ وَلَجْنَا، وَبِسْمِ اللَّهِ خَرَجْنَا، وَعَلَى اللَّهِ رَبِّنَا تَوَكَّلْنَا",
            "বিসমিল্লা-হি ওয়ালাজনা, ওয়া বিসমিল্লা-হি খারাজনা, ওয়া ‘আলাল্লা-হি রব্বিনা তাওয়াক্কালনা।",
            "Bismillahi walajna, wa bismillahi kharajna, wa 'alallahi rabbina tawakkalna.",
            "আমরা আল্লাহর নামে প্রবেশ করলাম, আল্লাহর নামেই বের হয়েছিলাম এবং আমাদের প্রতিপালক আল্লাহর উপরই ভরসা করলাম।",
            "In the name of Allah we enter, and in the name of Allah we leave, and upon Allah our Lord we rely.",
            "সুনান আবু দাউদ: ৫০৯৬ (হাসান)"
        ),

        // 9. মসজিদ ও ইবাদত (Mosque & Worship)
        Dua(
            23,
            "ইবাদত ও মসজিদ",
            "Worship & Mosque",
            "মসজিদে প্রবেশের দুআ",
            "Dua for Entering the Mosque",
            "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            "আল্লাহুম্মাফ তাহলী আবওয়া-বা রহমাতিক।",
            "Allahummaf-tah li abwaba rahmatik.",
            "হে আল্লাহ! আমার জন্য আপনার রহমতের সকল দরজা উন্মুক্ত করে দিন।",
            "O Allah, open for me the doors of Your mercy.",
            "সহীহ মুসলিম: ৭১৩, সুনান নাসাঈ: ৭২৯"
        ),
        Dua(
            24,
            "ইবাদত ও মসজিদ",
            "Worship & Mosque",
            "মসজিদ থেকে বের হওয়ার দুআ",
            "Dua for Leaving the Mosque",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            "আল্লাহুম্মা ইন্নী আসআলুকা মিন ফাদলিক।",
            "Allahumma inni as'aluka min fadlik.",
            "হে আল্লাহ! নিশ্চয়ই আমি আপনার কাছে আপনার অনুগ্রহ ও প্রাচুর্য প্রার্থনা করছি।",
            "O Allah, I ask You from Your bounty.",
            "সহীহ মুসলিম: ৭১৩"
        ),

        // 10. খাদ্য ও রোজা (Food & Fasting)
        Dua(
            25,
            "খাবার ও রোজা",
            "Food & Fasting",
            "খাবার খাওয়ার শুরুর দুআ",
            "Dua Before Eating",
            "بِسْمِ اللَّهِ وَعَلَى بَرَكَةِ اللَّهِ",
            "বিসমিল্লা-হি ওয়া ‘আলা বারাকাতিল্লা-হ।",
            "Bismillahi wa 'ala barakatillah.",
            "আল্লাহর নামে এবং আল্লাহর বরকতের উপর শুরু করছি।",
            "In the name of Allah and upon the blessing of Allah.",
            "সহীহ বুখারী: ৫৩৭৬, মুসতাদরাকে হাকেম: ৭১২৭"
        ),
        Dua(
            26,
            "খাবার ও রোজা",
            "Food & Fasting",
            "খাবার শেষ করার পর দুআ",
            "Dua After Eating",
            "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ",
            "আলহামদু লিল্লাহিল্লাযী আত‘আমানা ওয়া সাক্বানা ওয়া জা’আলানা মুসলিমীন।",
            "Alhamdu lillahilladhi at'amana wa saqana wa ja'alana muslimin.",
            "সমস্ত প্রশংসা আল্লাহর জন্য যিনি আমাদের আহার করিয়েছেন, পান করিয়েছেন এবং মুসলিম বানিয়েছেন।",
            "Praise belongs to Allah who provided us with food and drink and enabled us to be Muslims.",
            "সুনান আবু দাউদ: ৩৮৫০, সুনান আত-তিরমিযী: ৩৪৫৭"
        ),
        Dua(
            27,
            "খাবার ও রোজা",
            "Food & Fasting",
            "ইফতারের সময় পড়ার দুআ",
            "Dua at the Time of Iftar",
            "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ، وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ",
            "যাহাবায জামা-উ ওয়াবতাল্লাতিল ‘উরূক্বু, ওয়া সাবাতাল আজরু ইনশাআল্লাহ।",
            "Dhahabadh-dhama'u wabtallatil-'uruqu, wa thabatal-ajru in sha' Allah.",
            "পিপাসা দূরীভূত হয়েছে, শিরা-উপশিরা সিক্ত হয়েছে এবং আল্লাহ চাহে তো পুরস্কারও নিশ্চিত হয়েছে।",
            "The thirst is gone, the veins are moistened, and the reward is confirmed, if Allah wills.",
            "সুনান আবু দাউদ: ২৩৫৭, সুনান নাসাঈ: ৩৩১৫ (সহীহ)"
        ),

        // 11. আখিরাত ও জান্নাত (Hereafter & Jannah)
        Dua(
            28,
            "আখিরাত ও জান্নাত",
            "Hereafter & Jannah",
            "দুনিয়া ও আখিরাতের সর্বাঙ্গীন কল্যাণের দুআ",
            "Dua for Good in This Life and Hereafter",
            "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            "রব্বানা আতিনা ফিদ্দুনিয়া হাসানাতাওঁ ওয়া ফিল আখিরাতি হাসানাতাওঁ ওয়া ক্বিনা ‘আযাবান নার।",
            "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar.",
            "হে আমাদের প্রতিপালক! আমাদের দুনিয়াতেও কল্যাণ দান করুন এবং আখিরাতেও কল্যাণ দান করুন আর আমাদের জাহান্নামের আযাব থেকে রক্ষা করুন।",
            "Our Lord, give us in this world that which is good and in the Hereafter that which is good and protect us from the punishment of the Fire.",
            "পবিত্র কুরআন - সূরা আল-বাকারা: ২০১, সহীহ বুখারী: ৪৫২২"
        ),
        Dua(
            29,
            "আখিরাত ও জান্নাত",
            "Hereafter & Jannah",
            "জান্নাতুল ফিরদাউস লাভ ও জাহান্নাম থেকে মুক্তি",
            "Dua for Jannah & Refuge from Hellfire",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْجَنَّةَ، وَأَعُوذُ بِكَ مِنَ النَّارِ",
            "আল্লাহুম্মা ইন্নী আসআলুকাল জান্নাহ, ওয়া আ’ঊযু বিকা মিনান নার।",
            "Allahumma inni as'alukal-jannah, wa a'udhu bika minan-nar.",
            "হে আল্লাহ! আমি আপনার নিকট জান্নাত প্রার্থনা করছি এবং জাহান্নামের আগুন থেকে আপনার আশ্রয় চাচ্ছি। (প্রতিদিন ৩ বার)",
            "O Allah, I ask You for Paradise, and I seek refuge in You from the Fire. (3 times)",
            "সুনান আত-তিরমিযী: ২৫৭২, সুনান ইবনে মাজাহ: ৪৩৪১ (সহীহ)"
        ),
        Dua(
            30,
            "আখিরাত ও জান্নাত",
            "Hereafter & Jannah",
            "কবরের আযাব ও ফিতনা থেকে মুক্তির দুআ",
            "Dua for Protection from Punishment of the Grave",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، وَمِنْ عَذَابِ جَهَنَّمَ، وَمِنْ فِتْنَةِ الْمَحْيَا وَالْمَمَاتِ، وَمِنْ شَرِّ فِتْنَةِ الْمَسِيحِ الدَّجَّالِ",
            "আল্লাহুম্মা ইন্নী আ’ঊযু বিকা মিন ‘আযাবিল ক্বাবরি, ওয়া মিন ‘আযাবি জাহান্নাম, ওয়া মিন ফিতনাতিল মাহয়া ওয়াল মামা-তি, ওয়া মিন শাররি ফিতনাতিল মাসীহিদ দাজ্জা-ল।",
            "Allahumma inni a'udhu bika min 'adhabil-qabri, wa min 'adhabi jahannam, wa min fitnatil-mahya wal-mamati, wa min sharri fitnatil-masihid-dajjal.",
            "হে আল্লাহ! আমি আপনার নিকট কবরের আযাব থেকে, জাহান্নামের আযাব থেকে, জীবন ও মৃত্যুর ফিতনা থেকে এবং দাজ্জালের চরম অনিষ্টকারী ফিতনা থেকে আশ্রয় প্রার্থনা করছি।",
            "O Allah, I seek refuge in You from the punishment of the grave, and from the torment of Hellfire, and from the trials of life and death, and from the evil trial of the False Messiah (Dajjal).",
            "সহীহ বুখারী: ১৩৭৭, সহীহ মুসলিম: ৫৮৮"
        )
    )

    val hadithBooks = listOf(
        HadithBook(
            id = "bukhari",
            nameBn = "সহীহ আল-বুখারী",
            nameEn = "Sahih al-Bukhari",
            arabicName = "صحيح البخاري",
            authorBn = "ইমাম মুহাম্মদ বিন ইসমাইল আল-বুখারী (রহ.)",
            authorEn = "Imam Muhammad al-Bukhari (RA)",
            totalHadithsBn = "৭,৫৬৩টি হাদিস",
            totalHadithsCount = 7563,
            totalChaptersBn = "৯৭টি কিতাব / অধ্যায়",
            authenticGradeBn = "সর্বোচ্চ বিশুদ্ধ সনদ (১ম স্থান)",
            authenticGradeEn = "Highest Authenticity Rank",
            descriptionBn = "পবিত্র কুরআনের পর সর্বাধিক বিশুদ্ধ ও প্রামাণ্য হাদিস সংকলন।",
            descriptionEn = "Regarded as the most authentic collection of Hadith following the Holy Quran."
        ),
        HadithBook(
            id = "muslim",
            nameBn = "সহীহ মুসলিম",
            nameEn = "Sahih Muslim",
            arabicName = "صحيح مسلم",
            authorBn = "ইমাম মুসলিম বিন আল-হাজ্জাজ (রহ.)",
            authorEn = "Imam Muslim ibn al-Hajjaj (RA)",
            totalHadithsBn = "৭,৫০০টি হাদিস",
            totalHadithsCount = 7500,
            totalChaptersBn = "৫৬টি কিতাব / অধ্যায়",
            authenticGradeBn = "সহীহ সনদের বিশুদ্ধ মানদণ্ড (২য় স্থান)",
            authenticGradeEn = "Strict Sahih Criterion (2nd Rank)",
            descriptionBn = "সুশৃঙ্খল বিষয়ভিত্তিক অধ্যায়বিন্যাস ও বিশুদ্ধ সনদের অনন্য সংকলন।",
            descriptionEn = "Second only to Bukhari in authenticity and rigorous collection methodology."
        ),
        HadithBook(
            id = "nasai",
            nameBn = "সুনান আন-নাসায়ী",
            nameEn = "Sunan an-Nasa'i",
            arabicName = "سنن النسائي",
            authorBn = "ইমাম আহমদ বিন শুয়াইব আন-নাসায়ী (রহ.)",
            authorEn = "Imam Ahmad an-Nasa'i (RA)",
            totalHadithsBn = "৫,৭৫৮টি হাদিস",
            totalHadithsCount = 5758,
            totalChaptersBn = "৫০টি কিতাব / অধ্যায়",
            authenticGradeBn = "বর্ণনাকারী যাচাইয়ে কঠোর মানদণ্ড",
            authenticGradeEn = "Rigorous Sanad Scrutiny",
            descriptionBn = "ইবাদত ও দৈনন্দিন আহকামের অত্যন্ত সূক্ষ্ম সনদ-নিরীক্ষিত প্রামাণ্য গ্রন্থ।",
            descriptionEn = "Noted for its strict criteria in accepting narrators and assessing chains."
        ),
        HadithBook(
            id = "abudawud",
            nameBn = "সুনান আবু দাউদ",
            nameEn = "Sunan Abu Dawud",
            arabicName = "سنن أبي داود",
            authorBn = "ইমাম আবু দাউদ আস-সিজিস্তানী (রহ.)",
            authorEn = "Imam Abu Dawud as-Sijistani (RA)",
            totalHadithsBn = "৫,২৭৪টি হাদিস",
            totalHadithsCount = 5274,
            totalChaptersBn = "৪৩টি কিতাব / অধ্যায়",
            authenticGradeBn = "ফিকহ ও ইসলামি আইনের মূল ভিত্তি",
            authenticGradeEn = "Primary Source of Legal Rulings",
            descriptionBn = "ইসলামি আইনশাস্ত্র, হালাল-হারাম ও ব্যবহারিক বিধান সংবলিত সুবিখ্যাত গ্রন্থ।",
            descriptionEn = "Focuses heavily on legal rulings (fiqh), jurisprudence, and daily ethics."
        ),
        HadithBook(
            id = "tirmidhi",
            nameBn = "জামে' আত-তিরমিযী",
            nameEn = "Jami' at-Tirmidhi",
            arabicName = "جامع الترمذي",
            authorBn = "ইমাম আবু ঈসা মুহাম্মদ আত-তিরমিযী (রহ.)",
            authorEn = "Imam Abu 'Isa at-Tirmidhi (RA)",
            totalHadithsBn = "৩,৯৫৬টি হাদিস",
            totalHadithsCount = 3956,
            totalChaptersBn = "৫০টি কিতাব / অধ্যায়",
            authenticGradeBn = "সহীহ-হাসান ও ফিকহি তাহকীক",
            authenticGradeEn = "Sahih & Hasan Fiqh Analysis",
            descriptionBn = "হাদিসের মান নির্ধারণ এবং বিভিন্ন ফিকহি মাযহাবের তুলনামূলক বিশ্লেষণে সমৃদ্ধ।",
            descriptionEn = "Valuable for containing legal views of various schools of thought and grading classifications."
        ),
        HadithBook(
            id = "ibnmajah",
            nameBn = "সুনান ইবনে মাজাহ",
            nameEn = "Sunan Ibn Majah",
            arabicName = "سنن ابن ماجه",
            authorBn = "ইমাম মুহাম্মদ ইবনে মাজাহ আল-কাজবিনী (রহ.)",
            authorEn = "Imam Ibn Majah al-Qazwini (RA)",
            totalHadithsBn = "৪,৩৪১টি হাদিস",
            totalHadithsCount = 4341,
            totalChaptersBn = "৩৭টি কিতাব / অধ্যায়",
            authenticGradeBn = "সিহাহ সিত্তাহর ষষ্ঠতম প্রামাণ্য গ্রন্থ",
            authenticGradeEn = "Sixth Canonical Collection",
            descriptionBn = "চমৎকার অধ্যায়বিন্যাস, প্রাঞ্জল বিন্যাস এবং একক গুরুত্বপূর্ণ সনদের সমন্বয়।",
            descriptionEn = "The sixth of the primary canonical books, noted for excellent organization and rare narrations."
        ),
        HadithBook(
            id = "muwatta",
            nameBn = "মুয়াত্তা ইমাম মালিক",
            nameEn = "Muwatta Malik",
            arabicName = "موطأ الإمام مالك",
            authorBn = "ইমাম মালিক ইবনে আনাস (রহ.)",
            authorEn = "Imam Malik ibn Anas (RA)",
            totalHadithsBn = "১,৮৫৮টি হাদিস ও আসার",
            totalHadithsCount = 1858,
            totalChaptersBn = "৬১টি কিতাব / অধ্যায়",
            authenticGradeBn = "প্রথম লিখিত প্রামাণ্য হাদিস সংকলন",
            authenticGradeEn = "Earliest Foundational Compilation",
            descriptionBn = "মদিনার সোনালী সনদ (সিলসিলাতুজ জাহাব) ও ইসলামি আইনশাস্ত্রের প্রাচীনতম বিশ্বস্ত স্তম্ভ।",
            descriptionEn = "One of the earliest written collections of Hadith and Islamic law with pristine chains."
        ),
        HadithBook(
            id = "riyad",
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
            descriptionEn = "A popular, highly practical thematic selection of hadiths for daily life and spiritual growth."
        )
    )

    // Hadiths are dynamically loaded from Android assets/hadith/*.json datasets via HadithRepository
    val hadiths = emptyList<Hadith>()

    val hijriEvents = listOf(
        HijriEvent("পবিত্র রমজানুল মোবারক", "Blessed Ramadan", "১ রমজান ১৪৪৭ হিজরি", "1 Ramadan 1447 AH", "আত্মশুদ্ধি ও রহমতের মাস।", "Month of mercy and self-purification.", 12),
        HijriEvent("ঈদুল ফিতর", "Eid al-Fitr", "১ শাওয়াল ১৪৪৭ হিজরি", "1 Shawwal 1447 AH", "মুসলমানদের সর্ববৃহৎ আনন্দের উৎসব.", "Greatest festival of joy for Muslims.", 42),
        HijriEvent("পবিত্র আশুরা", "Ashura", "১০ মুহাররম ১৪৪৭ হিজরি", "10 Muharram 1447 AH", "ঐতিহাসিক ও তাৎপর্যপূর্ণ দিন।", "Historical and significant day.", 140),
        HijriEvent("শবে বরাত", "Shab-e-Barat", "১৫ শাবান ১৪৪৭ হিজরি", "15 Sha'ban 1447 AH", "সৌভাগ্য রজনী।", "Night of Fortune.", 0)
    )
}
