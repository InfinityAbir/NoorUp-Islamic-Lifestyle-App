package com.example.ui.noorup.hadith

import java.util.Calendar

data class DailyFeaturedHadith(
    val id: Int,
    val arabicText: String,
    val textBn: String,
    val textEn: String,
    val narratorBn: String,
    val narratorEn: String,
    val referenceBn: String,
    val referenceEn: String,
    val topicBn: String,
    val topicEn: String,
    val lessonBn: String,
    val lessonEn: String
)

object DailyHadithProvider {

    private val curatedHadiths = listOf(
        DailyFeaturedHadith(
            id = 1,
            arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            textBn = "নিশ্চয়ই সমস্ত কাজের ফলাফল নিয়তের ওপর নির্ভরশীল। আর প্রত্যেক ব্যক্তি কেবল তাই পাবে যার সে নিয়ত করেছে।",
            textEn = "Actions are judged by motives and each person will get the reward according to what was intended.",
            narratorBn = "উমর ইবনুল খাত্তাব (রা.)",
            narratorEn = "Umar ibn Al-Khattab (RA)",
            referenceBn = "সহীহ বুখারী #১, সহীহ মুসলিম #১৯০৭",
            referenceEn = "Sahih al-Bukhari #1, Sahih Muslim #1907",
            topicBn = "নিয়ত ও ইখলাস",
            topicEn = "Intentions & Sincerity",
            lessonBn = "সকল ইবাদতের মূল ভিত্তি আন্তরিকতা ও আল্লাহর সন্তুষ্টির নিয়ত।",
            lessonEn = "The foundation of every act of worship is sincere intention for the sake of Allah."
        ),
        DailyFeaturedHadith(
            id = 2,
            arabicText = "الطُّهُورُ شَطْرُ الإِيمَانِ، وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ",
            textBn = "পবিত্রতা ঈমানের অর্ধেক, আর 'আলহামদুলিল্লাহ' পাল্লা পূর্ণ করে দেয়।",
            textEn = "Purity is half of faith, and Alhamdulillah (Praise be to Allah) fills the scale.",
            narratorBn = "আবু মালেক আল-আশআরী (রা.)",
            narratorEn = "Abu Malik Al-Ashari (RA)",
            referenceBn = "সহীহ মুসলিম #২২৩",
            referenceEn = "Sahih Muslim #223",
            topicBn = "পবিত্রতা ও যিকির",
            topicEn = "Purification & Dhikr",
            lessonBn = "শারীরিক ও মানসিক পবিত্রতা এবং বেশি বেশি শুকরিয়া আদায় জান্নাতের সোপান।",
            lessonEn = "Inner and outer purity along with gratitude to Allah elevates one's spiritual station."
        ),
        DailyFeaturedHadith(
            id = 3,
            arabicText = "سَأَلْتُ النَّبِيَّ ﷺ: أَيُّ الْعَمَلِ أَحَبُّ إِلَى اللَّهِ؟ قَالَ: الصَّلَاةُ عَلَى وَقْتِهَا",
            textBn = "আমি রাসূলুল্লাহ (ﷺ)-কে জিজ্ঞাসা করলাম: আল্লাহর নিকট সর্বাধিক প্রিয় আমল কোনটি? তিনি বললেন: ওয়াক্তমত নামাজ আদায় করা।",
            textEn = "I asked the Prophet (ﷺ): Which deed is dearest to Allah? He said: To offer prayers at their early stated fixed times.",
            narratorBn = "আব্দুল্লাহ ইবনে মাসউদ (রা.)",
            narratorEn = "Abdullah ibn Mas'ud (RA)",
            referenceBn = "সহীহ বুখারী #৫২৭, সহীহ মুসলিম #৮৫",
            referenceEn = "Sahih al-Bukhari #527, Sahih Muslim #85",
            topicBn = "ওয়াক্তমত নামাজ",
            topicEn = "Punctual Prayer",
            lessonBn = "দেরি না করে ওয়াক্ত হওয়ামাত্রই নামাজ আদায় করা আল্লাহর অতি পছন্দের আমল।",
            lessonEn = "Performing obligatory prayers on time is the most beloved action to Allah."
        ),
        DailyFeaturedHadith(
            id = 4,
            arabicText = "أَكْمَلُ الْمُؤْمِنِينَ إِيمَانًا أَحْسَنُهُمْ خُلُقًا، وَخِيَارُكُمْ خِيَارُكُمْ لِنِسَائِهِمْ",
            textBn = "মুমিনদের মধ্যে পূর্ণাঙ্গ ঈমানের অধিকারী সে-ই যার চরিত্র সর্বোত্তম। আর তোমাদের মধ্যে শ্রেষ্ঠ সে যে তার পরিবারের (স্ত্রীর) প্রতি উত্তম।",
            textEn = "The most complete of believers in faith are those with the best character, and the best of you are those who are best to their wives.",
            narratorBn = "আবু হুরায়রা (রা.)",
            narratorEn = "Abu Huraira (RA)",
            referenceBn = "জামে আত-তিরমিযী #২০০৩",
            referenceEn = "Jami at-Tirmidhi #2003",
            topicBn = "আখলাক ও পরিবার",
            topicEn = "Good Character & Family",
            lessonBn = "উত্তম ব্যবহার এবং পরিবারের সাথে সদয় আচরণ ঈমানের পূর্ণতা প্রকাশ করে।",
            lessonEn = "Excellence of character and kindness to family are signs of true faith."
        ),
        DailyFeaturedHadith(
            id = 5,
            arabicText = "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
            textBn = "যে ব্যক্তি জ্ঞান অর্জনের পথে বের হয়, আল্লাহ তার জন্য জান্নাতের পথ সহজ করে দেন।",
            textEn = "Whoever travels a path seeking knowledge, Allah will make easy for them a path to Paradise.",
            narratorBn = "আবু হুরায়রা (রা.)",
            narratorEn = "Abu Huraira (RA)",
            referenceBn = "সহীহ মুসলিম #২৬৯৯",
            referenceEn = "Sahih Muslim #2699",
            topicBn = "ইলম ও দ্বীন শিক্ষা",
            topicEn = "Seeking Knowledge",
            lessonBn = "দ্বীনি জ্ঞান ও কল্যাণকর বিদ্যা অর্জন জান্নাতে প্রবেশের মহাসড়ক।",
            lessonEn = "Pursuing authentic knowledge is a direct gateway to Paradise."
        ),
        DailyFeaturedHadith(
            id = 6,
            arabicText = "لاَ يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
            textBn = "তোমাদের কেউ প্রকৃত মুমিন হতে পারবে না যতক্ষণ না সে তার ভাইয়ের জন্য তা-ই পছন্দ করে যা সে নিজের জন্য পছন্দ করে।",
            textEn = "None of you truly believes until he loves for his brother what he loves for himself.",
            narratorBn = "আনাস ইবনে মালিক (রা.)",
            narratorEn = "Anas ibn Malik (RA)",
            referenceBn = "সহীহ বুখারী #১৩, সহীহ মুসলিম #৪৫",
            referenceEn = "Sahih al-Bukhari #13, Sahih Muslim #45",
            topicBn = "ভ্রাতৃত্ব ও সহমর্মিতা",
            topicEn = "Brotherhood & Empathy",
            lessonBn = "অন্যের প্রতি সহমর্মিতা ও নিঃস্বার্থ ভালোবাসা খাঁটি মুমিনের পরিচায়ক।",
            lessonEn = "Selfless empathy and loving goodness for others defines true brotherhood in faith."
        ),
        DailyFeaturedHadith(
            id = 7,
            arabicText = "الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ",
            textBn = "উত্তম ও মিষ্টি কথা বলাও একটি সদকা (দান)।",
            textEn = "A good, kind word is an act of charity.",
            narratorBn = "আবু হুরায়রা (রা.)",
            narratorEn = "Abu Huraira (RA)",
            referenceBn = "সহীহ বুখারী #২৯৮৯, সহীহ মুসলিম #১০০৯",
            referenceEn = "Sahih al-Bukhari #2989, Sahih Muslim #1009",
            topicBn = "সদকা ও মিষ্টভাষিতা",
            topicEn = "Charity & Kind Words",
            lessonBn = "সুন্দর ব্যবহার ও উৎসাহমূলক বাক্য অপরের হৃদয়ে প্রশান্তি আনে ও সওয়াব বয়ে আনে।",
            lessonEn = "Spreading kindness through encouraging words is rewarded as charity in Islam."
        ),
        DailyFeaturedHadith(
            id = 8,
            arabicText = "الدُّعَاءُ هُوَ الْعِبَادَةُ",
            textBn = "দোয়াই হলো মূল ইবাদত।",
            textEn = "Supplication (Dua) is the very essence of worship.",
            narratorBn = "আন-নুমান ইবনে বাশীর (রা.)",
            narratorEn = "An-Nu'man ibn Bashir (RA)",
            referenceBn = "জামে আত-তিরমিযী #৩৩৭১, সুনানে আবু দাউদ #১৪৭৯",
            referenceEn = "Jami at-Tirmidhi #3371, Sunan Abi Dawud #1479",
            topicBn = "দোয়া ও আল্লাহর সান্নিধ্য",
            topicEn = "Supplication & Worship",
            lessonBn = "আল্লাহর কাছে সরাসরি প্রার্থনা করা বান্দা ও রবের মধ্যকার গভীরতম সম্পর্ক।",
            lessonEn = "Calling upon Allah directly with humility is the core of all worship."
        ),
        DailyFeaturedHadith(
            id = 9,
            arabicText = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ، وَمَا زَادَ اللَّهُ عَبْدًا بِعَفْوٍ إِلاَّ عِزًّا",
            textBn = "সদকা করার কারণে কখনো সম্পদ কমে না, আর ক্ষমার কারণে আল্লাহ বান্দার সম্মান কেবল বৃদ্ধিই করেন।",
            textEn = "Charity does not decrease wealth, and Allah increases the honor of one who forgives.",
            narratorBn = "আবু হুরায়রা (রা.)",
            narratorEn = "Abu Huraira (RA)",
            referenceBn = "সহীহ মুসলিম #২৫৮৮",
            referenceEn = "Sahih Muslim #2588",
            topicBn = "দান ও ক্ষমাশীলতা",
            topicEn = "Charity & Forgiveness",
            lessonBn = "অসহায়কে দান এবং মানুষকে ক্ষমা করে দেওয়া উভয় জগতেই মর্যাদা বাড়ায়।",
            lessonEn = "Giving in charity brings divine abundance, and forgiveness elevates dignity."
        ),
        DailyFeaturedHadith(
            id = 10,
            arabicText = "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ",
            textBn = "তুমি যেখানেই থাকো আল্লাহকে ভয় করো (তাকওয়া অবলম্বন করো), কোনো ভুল হয়ে গেলে সাথে সাথে নেক আমল করো যা পাপকে মুছে দেবে, এবং মানুষের সাথে সুন্দর আচরণ করো।",
            textEn = "Be mindful of Allah wherever you are, follow a bad deed with a good deed and it will wipe it out, and treat people with good character.",
            narratorBn = "আবু যর গিফারী (রা.)",
            narratorEn = "Abu Dharr Al-Ghifari (RA)",
            referenceBn = "জামে আত-তিরমিযী #১৯৮৭",
            referenceEn = "Jami at-Tirmidhi #1987",
            topicBn = "তাকওয়া ও আত্মশুদ্ধি",
            topicEn = "Taqwa & Good Character",
            lessonBn = "গোপনে ও প্রকাশ্যে আল্লাহর ভয়, দ্রুত তওবা ও সৎ চরিত্রই সফলতার মূল।",
            lessonEn = "Taqwa, swift repentance, and polite character form the pillars of a righteous life."
        )
    )

    fun getTodayHadith(): DailyFeaturedHadith {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val index = (dayOfYear % curatedHadiths.size)
        return curatedHadiths[index]
    }

    fun getAllHadiths(): List<DailyFeaturedHadith> = curatedHadiths
}
