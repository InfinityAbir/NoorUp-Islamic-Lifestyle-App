package com.example.ui.noorup

object NoorUpRepository {

    // NOTE: stale static prayer-time/habit snapshots were removed — all live data
    // comes from SolarPrayerEngine via NoorUpViewModel. Do not reintroduce
    // hardcoded time tables here.

    val surahs: List<Surah> get() = com.example.ui.noorup.quran.QuranRepository.all114Surahs

    private val baseDuas: List<DuaItem> = listOf(
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
        ),
        DuaItem(
            id = "dua_morning",
            titleBangla = "সকালের যিকির",
            titleEnglish = "Morning Dhikr",
            categoryBangla = "সকাল ও সন্ধ্যা",
            categoryEnglish = "Morning & Evening",
            reference = "সুনানে আবু দাউদ, হাদিস: ৫০৭২",
            arabicText = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ",
            phoneticBangla = "আসবাহনা ওয়া আসবাহাল মুলকু লিল্লাহ, ওয়াল হামদু লিল্লাহ।",
            phoneticEnglish = "Asbahna wa asbahal-mulku lillah, wal-hamdu lillah.",
            meaningBangla = "আমরা সকালে উপনীত হলাম এবং সমগ্র রাজত্ব আল্লাহরই; সমস্ত প্রশংসা আল্লাহর।",
            meaningEnglish = "We have reached the morning and all sovereignty belongs to Allah; praise is due to Allah."
        ),
        DuaItem(
            id = "dua_evening",
            titleBangla = "সন্ধ্যার যিকির",
            titleEnglish = "Evening Dhikr",
            categoryBangla = "সকাল ও সন্ধ্যা",
            categoryEnglish = "Morning & Evening",
            reference = "সহীহ মুসলিম, হাদিস: ২৭২৩",
            arabicText = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ",
            phoneticBangla = "আমসাইনা ওয়া আমসাল মুলকু লিল্লাহ, ওয়াল হামদু লিল্লাহ।",
            phoneticEnglish = "Amsayna wa amsal-mulku lillah, wal-hamdu lillah.",
            meaningBangla = "আমরা সন্ধ্যায় উপনীত হলাম এবং সমগ্র রাজত্ব আল্লাহরই; সমস্ত প্রশংসা আল্লাহর।",
            meaningEnglish = "We have reached the evening and all sovereignty belongs to Allah; praise is due to Allah."
        ),
        DuaItem(
            id = "dua_protection_evil",
            titleBangla = "সকল অনিষ্ট থেকে সুরক্ষার দুআ",
            titleEnglish = "Dua for Protection from All Evil",
            categoryBangla = "সুরক্ষা",
            categoryEnglish = "Protection",
            reference = "সহীহ মুসলিম, হাদিস: ২৭০৮",
            arabicText = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            phoneticBangla = "আউজু বিকালিমাতিল্লাহিত তাম্মাতি মিন শাররি মা খালাক্ব।",
            phoneticEnglish = "A'udhu bikalimatillahit-tammati min sharri ma khalaq.",
            meaningBangla = "আল্লাহর পরিপূর্ণ বাণীসমূহের মাধ্যমে তাঁর সৃষ্টির সকল অনিষ্ট থেকে আশ্রয় চাই।",
            meaningEnglish = "I seek refuge in the perfect words of Allah from the evil of what He has created."
        ),
        DuaItem(
            id = "dua_travel",
            titleBangla = "সফরের দুআ",
            titleEnglish = "Dua for Travel",
            categoryBangla = "সফর",
            categoryEnglish = "Travel",
            reference = "সহীহ মুসলিম, হাদিস: ১৩৪২",
            arabicText = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَىٰ رَبِّنَا لَمُنقَلِبُونَ",
            phoneticBangla = "সুবহানাল্লাযি সাখখারা লানা হাযা ওয়ামা কুন্না লাহু মুক্বরিনীন, ওয়া ইন্না ইলা রব্বিনা লামুনক্বালিবূন।",
            phoneticEnglish = "Subhanalladhi sakhkhara lana hadha wa ma kunna lahu muqrinin, wa inna ila rabbina lamunqalibun.",
            meaningBangla = "পবিত্র সেই সত্তা যিনি একে আমাদের অধীন করেছেন, অথচ আমরা একে বশ করতে সক্ষম ছিলাম না; আর নিশ্চয়ই আমরা আমাদের প্রতিপালকের দিকেই প্রত্যাবর্তনকারী।",
            meaningEnglish = "Glory to Him who has subjected this to us, and we could never have it by our efforts; and indeed to our Lord we shall return."
        ),
        DuaItem(
            id = "dua_before_food",
            titleBangla = "খাবার শুরুর দুআ",
            titleEnglish = "Dua Before Eating",
            categoryBangla = "খাবার ও রোজা",
            categoryEnglish = "Food & Fasting",
            reference = "সুনানে আবু দাউদ, হাদিস: ৩৭৬৭",
            arabicText = "بِسْمِ اللَّهِ",
            phoneticBangla = "বিসমিল্লাহ।",
            phoneticEnglish = "Bismillah.",
            meaningBangla = "আল্লাহর নামে (শুরু করছি)।",
            meaningEnglish = "In the name of Allah."
        ),
        DuaItem(
            id = "dua_after_food",
            titleBangla = "খাবার শেষের দুআ",
            titleEnglish = "Dua After Eating",
            categoryBangla = "খাবার ও রোজা",
            categoryEnglish = "Food & Fasting",
            reference = "সুনানে আবু দাউদ, হাদিস: ৩৮৫০",
            arabicText = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            phoneticBangla = "আলহামদু লিল্লাহিল্লাযি আত্ব'আমানি হাযা ওয়া রাযাক্বানিহি মিন গাইরি হাওলিম মিন্নি ওয়ালা কুওয়াহ।",
            phoneticEnglish = "Alhamdu lillahilladhi at'amani hadha wa razaqanihi min ghayri hawlin minni wa la quwwah.",
            meaningBangla = "সমস্ত প্রশংসা আল্লাহর, যিনি আমাকে এটি খাওয়ালেন এবং আমার কোনো শক্তি-সামর্থ্য ছাড়াই এটি দান করলেন।",
            meaningEnglish = "Praise is to Allah who fed me this and provided it for me without any might or power on my part."
        ),
        DuaItem(
            id = "dua_rizq",
            titleBangla = "রিযিক ও বরকতের দুআ",
            titleEnglish = "Dua for Sustenance and Blessing",
            categoryBangla = "রিযিক ও বরকত",
            categoryEnglish = "Sustenance",
            reference = "সুনানে ইবনে মাজাহ, হাদিস: ৯২৫",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
            phoneticBangla = "আল্লাহুম্মা ইন্নি আসআলুকা ইলমান নাফিআন, ওয়া রিযক্বান ত্বাইয়্যিবান, ওয়া আমালাম মুতাক্বাব্বালান।",
            phoneticEnglish = "Allahumma inni as'aluka 'ilman nafi'an, wa rizqan tayyiban, wa 'amalan mutaqabalan.",
            meaningBangla = "হে আল্লাহ! আমি আপনার কাছে উপকারী জ্ঞান, পবিত্র রিযিক এবং কবুলযোগ্য আমল প্রার্থনা করি।",
            meaningEnglish = "O Allah, I ask You for beneficial knowledge, wholesome provision, and acceptable deeds."
        ),
        DuaItem(
            id = "dua_family_peace",
            titleBangla = "পারিবারিক শান্তির দুআ",
            titleEnglish = "Dua for Family Peace",
            categoryBangla = "পারিবারিক শান্তি",
            categoryEnglish = "Home & Family",
            reference = "সূরা আল-ফুরকান, আয়াত: ৭৪",
            arabicText = "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
            phoneticBangla = "রব্বানা হাবলানা মিন আযওয়াজিনা ওয়া যুররিয়্যাতিনা ক্বুররাতা আ'ইউনিন ওয়াজআলনা লিল মুত্তাক্বীনা ইমামা।",
            phoneticEnglish = "Rabbana hab lana min azwajina wa dhurriyyatina qurrata a'yunin waj'alna lil-muttaqina imama.",
            meaningBangla = "হে আমাদের প্রতিপালক! আমাদের স্ত্রী ও সন্তানদের থেকে আমাদের চোখের শীতলতা দান করুন এবং আমাদের মুত্তাকীদের নেতা বানান।",
            meaningEnglish = "Our Lord, grant us from among our wives and offspring comfort to our eyes and make us an example for the righteous."
        ),
        DuaItem(
            id = "dua_mosque_entry",
            titleBangla = "মসজিদে প্রবেশের দুআ",
            titleEnglish = "Dua upon Entering the Mosque",
            categoryBangla = "ইবাদত ও মসজিদ",
            categoryEnglish = "Worship & Mosque",
            reference = "সহীহ মুসলিম, হাদিস: ৭১৩",
            arabicText = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            phoneticBangla = "আল্লাহুম্মাফতাহলি আবওয়াবা রাহমাতিক।",
            phoneticEnglish = "Allahummaftah li abwaba rahmatik.",
            meaningBangla = "হে আল্লাহ! আমার জন্য আপনার রহমতের দরজাসমূহ খুলে দিন।",
            meaningEnglish = "O Allah, open for me the doors of Your mercy."
        ),
        DuaItem(
            id = "dua_jannah",
            titleBangla = "জান্নাত প্রার্থনার দুআ",
            titleEnglish = "Dua for Paradise",
            categoryBangla = "আখিরাত ও জান্নাত",
            categoryEnglish = "Hereafter & Jannah",
            reference = "সুনানে তিরমিযী, হাদিস: ২৫৭২",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْجَنَّةَ، وَأَعُوذُ بِكَ مِنَ النَّارِ",
            phoneticBangla = "আল্লাহুম্মা ইন্নি আসআলুকাল জান্নাহ, ওয়া আউজু বিকা মিনান নার।",
            phoneticEnglish = "Allahumma inni as'alukal-jannah, wa a'udhu bika minan-nar.",
            meaningBangla = "হে আল্লাহ! আমি আপনার কাছে জান্নাত প্রার্থনা করি এবং জাহান্নাম থেকে আশ্রয় চাই।",
            meaningEnglish = "O Allah, I ask You for Paradise and seek refuge in You from the Fire."
        ),
        DuaItem(
            id = "dua_distress",
            titleBangla = "দুশ্চিন্তা ও বিপদমুক্তির দুআ (ইউনুস)",
            titleEnglish = "Dua of Yunus for Distress",
            categoryBangla = "উদ্বেগ ও দুশ্চিন্তা",
            categoryEnglish = "Distress & Anxiety",
            reference = "সূরা আল-আম্বিয়া, আয়াত: ৮৭",
            arabicText = "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            phoneticBangla = "লা ইলাহা ইল্লা আনতা সুবহানাকা ইন্নি কুনতু মিনায যালিমীন।",
            phoneticEnglish = "La ilaha illa anta subhanaka inni kuntu minaz-zalimin.",
            meaningBangla = "আপনি ছাড়া কোনো সত্য উপাস্য নেই; আপনি পবিত্র; নিশ্চয়ই আমি যালিমদের অন্তর্ভুক্ত ছিলাম।",
            meaningEnglish = "There is no deity except You; glory be to You. Indeed, I have been of the wrongdoers."
        ),
        DuaItem(
            id = "dua_iman",
            titleBangla = "ঈমান দৃঢ় রাখার দুআ",
            titleEnglish = "Dua for Steadfast Faith",
            categoryBangla = "জ্ঞান ও ঈমান",
            categoryEnglish = "Knowledge & Faith",
            reference = "সুনানে তিরমিযী, হাদিস: ৩৫২২",
            arabicText = "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            phoneticBangla = "ইয়া মুক্বাল্লিবাল ক্বুলূবি সাব্বিত ক্বালবি আলা দীনিক।",
            phoneticEnglish = "Ya muqallibal-qulubi thabbit qalbi 'ala dinik.",
            meaningBangla = "হে অন্তরসমূহের পরিবর্তনকারী! আমার অন্তরকে আপনার দ্বীনের ওপর সুদৃঢ় রাখুন।",
            meaningEnglish = "O Turner of hearts, make my heart firm upon Your religion."
        )
    )

    /**
     * Search-only synonyms. These never replace the displayed title, meaning,
     * pronunciation or reference; they only help users find a dua using words
     * that are not already present in the text.
     */
    private val duaSearchTags: Map<String, List<String>> = mapOf(
        "dua_anxiety" to listOf("চিন্তা", "দুশ্চিন্তা", "ঋণ", "পেরেশানি", "anxiety", "stress", "worry", "debt", "sadness"),
        "dua_forgiveness" to listOf("ক্ষমা", "তওবা", "ইস্তিগফার", "গুনাহ", "forgiveness", "repentance", "istighfar", "sin"),
        "dua_parents" to listOf("পিতা", "মাতা", "মা", "বাবা", "পিতা-মাতা", "parents", "mother", "father", "family"),
        "dua_knowledge" to listOf("জ্ঞান", "ইলম", "পড়াশোনা", "পরীক্ষা", "knowledge", "study", "exam", "ilm"),
        "dua_home_exit" to listOf("ঘর", "বের", "বাইরে", "নিরাপত্তা", "home", "leave", "outside", "safety"),
        "dua_dunya_akhirah" to listOf("দুনিয়া", "আখিরাত", "কল্যাণ", "ভালো", "world", "hereafter", "good", "success"),
        "dua_morning" to listOf("সকাল", "ফজর", "ভোর", "morning", "fajr", "dawn"),
        "dua_evening" to listOf("সন্ধ্যা", "মাগরিব", "বিকেল", "evening", "maghrib", "sunset"),
        "dua_protection_evil" to listOf("সুরক্ষা", "অনিষ্ট", "নজর", "শত্রু", "protection", "evil", "harm", "enemy"),
        "dua_travel" to listOf("সফর", "যাত্রা", "গাড়ি", "বাস", "travel", "journey", "car", "bus", "trip"),
        "dua_before_food" to listOf("খাবার", "খাওয়া", "ভাত", "food", "eating", "meal", "before"),
        "dua_after_food" to listOf("খাবার", "শেষ", "খাওয়া", "food", "after", "meal", "gratitude"),
        "dua_rizq" to listOf("রিযিক", "বরকত", "উপার্জন", "চাকরি", "sustenance", "provision", "blessing", "job", "income"),
        "dua_family_peace" to listOf("পরিবার", "স্ত্রী", "স্বামী", "সন্তান", "family", "wife", "husband", "children", "peace"),
        "dua_mosque_entry" to listOf("মসজিদ", "মসজিদে", "ইবাদত", "mosque", "masjid", "worship"),
        "dua_jannah" to listOf("জান্নাত", "আখিরাত", "জাহান্নাম", "paradise", "jannah", "hereafter", "fire"),
        "dua_distress" to listOf("ইউনুস", "বিপদ", "উদ্বেগ", "মুসিবত", "distress", "yunus", "calamity", "hardship"),
        "dua_iman" to listOf("ঈমান", "দ্বীন", "হৃদয়", "faith", "iman", "heart", "religion", "steadfast")
    )

    val duas: List<DuaItem> = baseDuas.map { dua ->
        dua.copy(searchTags = duaSearchTags[dua.id].orEmpty())
    }
}
