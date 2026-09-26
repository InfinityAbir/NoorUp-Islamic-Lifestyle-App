package com.example.ui.noorup.hadith

data class Book(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val arabicName: String,
    val authorBn: String,
    val authorEn: String,
    val authenticGradeBn: String,
    val authenticGradeEn: String,
    val totalHadithsBn: String,
    val descriptionBn: String,
    val descriptionEn: String,
    val totalChaptersBn: String,
    val assetFileName: String = ""
) {
    val titleBn: String get() = nameBn
    val titleEn: String get() = nameEn
    val totalHadith: Int get() = totalHadithsBn.filter { it.isDigit() }.toIntOrNull() ?: 100
}

typealias HadithBookInfo = Book

data class Hadith(
    val id: Int,
    val bookId: String,
    val collection: String,
    val isMuttafaqunAlayh: Boolean = false,
    val chapterId: Int = 1,
    val chapterBn: String = "",
    val chapterEn: String = "",
    val topicBn: String = "",
    val topicEn: String = "",
    val hadithNumber: Int = 1,
    val narrator: String = "",
    val arabicText: String = "",
    val translationBangla: String = "",
    val translationEnglish: String = "",
    val grade: String = "সহীহ (Sahih)",
    val reference: String = "",
    val bookNameBn: String = "",
    val bookNameEn: String = "",
    val explanationBn: String = "",
    val explanationEn: String = "",
    val isBookmarked: Boolean = false
) {
    val arabic: String get() = arabicText
    val bangla: String get() = translationBangla
    val english: String get() = translationEnglish
    val narratorBn: String get() = narrator
    val narratorEn: String get() = narrator
    val gradeBn: String get() = grade
    val gradeEn: String get() = grade
}

typealias HadithItem = Hadith

data class HadithRawJson(
    val id: Int,
    val chapterId: Int? = 1,
    val chapterTitleBn: String? = null,
    val chapterTitleEn: String? = null,
    val hadithNumber: Int? = null,
    val narratorBn: String? = null,
    val narratorEn: String? = null,
    val arabic: String? = null,
    val bangla: String? = null,
    val english: String? = null,
    val gradeBn: String? = null,
    val gradeEn: String? = null
)
