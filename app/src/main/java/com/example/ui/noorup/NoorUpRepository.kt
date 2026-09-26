package com.example.ui.noorup

object NoorUpRepository {

    val prayerTimes = listOf(
        PrayerTime("ফজর", "Fajr", "الفجر", "04:48 AM", "05:58 AM", false, true, false),
        PrayerTime("সূর্যোদয়", "Sunrise", "الشروق", "05:59 AM", "06:15 AM", false, true, true, "সূর্য ওঠার সময় নামাজ নিষিদ্ধ", "Prohibited during sunrise"),
        PrayerTime("যোহর", "Dhuhr", "الظهر", "12:05 PM", "04:21 PM", true, false, false),
        PrayerTime("আসর", "Asr", "العصر", "04:22 PM", "06:17 PM", false, false, false),
        PrayerTime("মাগরিব", "Maghrib", "المغرب", "06:18 PM", "07:32 PM", false, false, false),
        PrayerTime("এশা", "Isha", "العشاء", "07:33 PM", "04:47 AM", false, false, false),
        PrayerTime("তাহাজ্জুদ", "Tahajjud", "التهجد", "03:00 AM", "04:27 AM", false, false, false)
    )

    val defaultHabits = listOf(
        DailyHabitItem("habit_fajr", "ফজর নামাজ আদায়", "Fajr Prayer Performed", "নামাজ", false, "🕌"),
        DailyHabitItem("habit_dhuhr", "যোহর নামাজ আদায়", "Dhuhr Prayer Performed", "নামাজ", false, "🕌"),
        DailyHabitItem("habit_asr", "আসর নামাজ আদায়", "Asr Prayer Performed", "নামাজ", false, "🕌"),
        DailyHabitItem("habit_maghrib", "মাগরিব নামাজ আদায়", "Maghrib Prayer Performed", "নামাজ", false, "🕌"),
        DailyHabitItem("habit_isha", "এশা নামাজ আদায়", "Isha Prayer Performed", "নামাজ", false, "🕌"),
        DailyHabitItem("habit_quran", "দৈনিক কুরআন তিলাওয়াত", "Daily Quran Recitation", "ইবাদত", false, "📖"),
        DailyHabitItem("habit_dhikr", "সকাল-সন্ধ্যার জিকির ও তাসবীহ", "Morning/Evening Dhikr", "জিকির", false, "📿"),
        DailyHabitItem("habit_sadaqah", "দান ও সদকা প্রদান", "Given Charity (Sadaqah)", "সদকা", false, "🌱")
    )

    val surahs: List<Surah> get() = com.example.ui.noorup.quran.QuranRepository.all114Surahs

    val duas: List<DuaItem> = listOf(
        DuaItem(
            id = "dua_anxiety",
            titleBangla = "চিন্তা ও ঋণমুক্তির দুআ",
            titleEnglish = "Dua for Relief from Anxiety and Debt",
            categoryBangla = "কষ্ট ও পেরেশানি",
            categoryEnglish = "Anxiety & Distress",
            reference = "সহীহ বুখারী, হাদিস: ২৮৯৩",
            arabicText = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ",
            phoneticBangla = "আল্লাহুম্মা ইন্নি আউজু বিকা মিনাল হাম্মি ওয়াল হাযানি, ওয়াল আজযি ওয়াল কাসালি, ওয়াল বুখলি ওয়াল জুবনি, ওয়া দ্বালা'ইদ দাইনি, ওয়া গালাবাতির রিজাল।",
            phoneticEnglish = "Allahumma inni a'udhu bika minal-hammi wal-hazani, wal-'ajzi wal-kasali, wal-bukhli wal-jubni, wa dala'id-dayni, wa ghalabatir-rijal.",
            meaningBangla = "হে আল্লাহ! নিশ্চয়ই আমি আপনার আশ্রয় প্রার্থনা করছি দুশ্চিন্তা ও শোক হতে, অপারগতা ও অলসতা হতে, কৃপণতা ও কাপুরুষতা হতে এবং ঋণের বোঝা ও মানুষের অত্যাচার হতে।",
            meaningEnglish = "O Allah, I seek refuge in You from grief and sadness, from weakness and laziness, from miserliness and cowardice, from the burden of debt and from being overpowered by men."
        ),
        DuaItem(
            id = "dua_forgiveness",
            titleBangla = "সায়্যিদুল ইস্তিগফার (শ্রেষ্ঠ ক্ষমা প্রার্থনা)",
            titleEnglish = "Sayyidul Istighfar (Chief of Forgiveness)",
            categoryBangla = "ক্ষমা ও তওবা",
            categoryEnglish = "Forgiveness & Repentance",
            reference = "সহীহ বুখারী, হাদিস: ৬৩০৬",
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            phoneticBangla = "আল্লাহুম্মা আনতা রব্বি লা ইলাহা ইল্লা আনতা, খালাকতানি ওয়া আনা আবদুকা, ওয়া আনা আলা আহদিকা ওয়া ওয়াদিকা মাস্তাতাতু, আউজু বিকা মিন শাররি মা সানাতু, আবূউ লাকা বিনি'মাতিকা আলাইয়্যা, ওয়া আবূউ বিযানবি ফাগফিরলি, ফাইন্নাহু লা ইয়াগফিরুজ জুনূবা ইল্লা আনতা।",
            phoneticEnglish = "Allahumma anta Rabbi la ilaha illa Anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bini'matika 'alayya, wa abu'u bidhanbi faghfir li, fa-innahu la yaghfirudh-dhunuba illa Anta.",
            meaningBangla = "হে আল্লাহ! আপনিই আমার প্রতিপালক। আপনি ছাড়া কোনো সত্য উপাস্য নেই। আপনি আমাকে সৃষ্টি করেছেন এবং আমি আপনার বান্দা। আমি সাধ্যমত আপনার প্রতিশ্রুতি ও অঙ্গীকারে অঙ্গীকারবদ্ধ আছি। আমি আমার কৃতকর্মের অনিষ্ট থেকে আপনার আশ্রয় চাই। আমার উপর আপনার নিয়ামত স্বীকার করছি এবং আমার অপরাধও স্বীকার করছি। অতএব আমাকে ক্ষমা করুন, কারণ আপনি ছাড়া কেউ গুনাহ ক্ষমা করতে পারে না।",
            meaningEnglish = "O Allah, You are my Lord, there is no deity worthy of worship except You. You created me and I am Your slave. I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favors upon me and I acknowledge my sin, so forgive me, for none forgives sins except You."
        ),
        DuaItem(
            id = "dua_parents",
            titleBangla = "পিতা-মাতার জন্য দুআ",
            titleEnglish = "Dua for Parents",
            categoryBangla = "পিতা-মাতা ও পরিবার",
            categoryEnglish = "Parents & Family",
            reference = "সূরা বনী ইসরাঈল, আয়াত: ২৪",
            arabicText = "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            phoneticBangla = "রব্বির হামহুমা কামা রব্বায়ানি সাগিরা।",
            phoneticEnglish = "Rabbir-hamhuma kama rabbayani sagheera.",
            meaningBangla = "হে আমার প্রতিপালক! তাঁদের প্রতি দয়া করুন, যেমন শৈশবে তাঁরা আমাকে স্নেহে লালন-পালন করেছেন।",
            meaningEnglish = "My Lord, have mercy upon them as they brought me up [when I was] small."
        ),
        DuaItem(
            id = "dua_knowledge",
            titleBangla = "ইলম ও জ্ঞান বৃদ্ধির দুআ",
            titleEnglish = "Dua for Increase in Knowledge",
            categoryBangla = "জ্ঞান ও প্রজ্ঞা",
            categoryEnglish = "Knowledge & Wisdom",
            reference = "সূরা ত্বাহা, আয়াত: ১১৪",
            arabicText = "رَبِّ زِدْنِي عِلْمًا",
            phoneticBangla = "রব্বি যিদনি ইলমা।",
            phoneticEnglish = "Rabbi zidni 'ilma.",
            meaningBangla = "হে আমার প্রতিপালক! আমার জ্ঞান বৃদ্ধি করে দিন।",
            meaningEnglish = "My Lord, increase me in knowledge."
        ),
        DuaItem(
            id = "dua_home_exit",
            titleBangla = "ঘর থেকে বের হওয়ার দুআ",
            titleEnglish = "Dua upon Leaving the House",
            categoryBangla = "দৈনন্দিন আদব",
            categoryEnglish = "Daily Etiquettes",
            reference = "সুনানে আবু দাউদ, হাদিস: ৫০৯৫",
            arabicText = "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            phoneticBangla = "বিসমিল্লাহি, তাওয়াক্কালতু আলাল্লাহি, ওয়া লা হাওলা ওয়া লা কুওয়াতা ইল্লা বিল্লাহ।",
            phoneticEnglish = "Bismillahi tawakkaltu 'alallahi, wa la hawla wa la quwwata illa billah.",
            meaningBangla = "আল্লাহর নামে, আল্লাহর ওপরই ভরসা করলাম। আল্লাহর সাহায্য ছাড়া পাপ থেকে বাঁচার বা পুণ্য অর্জনের কোনো শক্তি নেই।",
            meaningEnglish = "In the name of Allah, I place my trust in Allah; there is no might nor power except with Allah."
        ),
        DuaItem(
            id = "dua_dunya_akhirah",
            titleBangla = "দুনিয়া ও আখিরাতে কল্যাণের দুআ",
            titleEnglish = "Dua for Good in Dunya and Akhirah",
            categoryBangla = "সার্বিক কল্যাণ",
            categoryEnglish = "Overall Wellbeing",
            reference = "সূরা আল-বাক্বারা, আয়াত: ২০১",
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            phoneticBangla = "রব্বানা আতিনা ফিদ দুনইয়া হাসানাতান ওয়া ফিল আখিরাতি হাসানাতান ওয়া কিনা আজাবান নার।",
            phoneticEnglish = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar.",
            meaningBangla = "হে আমাদের প্রতিপালক! আমাদেরকে দুনিয়াতেও কল্যাণ দান করুন এবং আখিরাতেও কল্যাণ দান করুন এবং জাহান্নামের শাস্তি থেকে রক্ষা করুন।",
            meaningEnglish = "Our Lord, give us in this world [that which is] good and in the Hereafter [that which is] good and protect us from the punishment of the Fire."
        )
    )
}
