package com.example.ui.noorup.hadith

import com.google.gson.annotations.SerializedName

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

data class HadithBookFileJson(
    val metadata: HadithMetadataJson? = null,
    val chapters: List<HadithChapterJson>? = null,
    val hadiths: List<HadithRawJson>? = null
)

data class HadithMetadataJson(
    val id: String? = null,
    @SerializedName("name_bn") val nameBn: String? = null,
    @SerializedName("name_en") val nameEn: String? = null,
    @SerializedName("arabic_name") val arabicName: String? = null,
    @SerializedName("author_bn") val authorBn: String? = null,
    @SerializedName("author_en") val authorEn: String? = null,
    @SerializedName("total_hadiths_bn") val totalHadithsBn: String? = null,
    @SerializedName("total_hadiths_count") val totalHadithsCount: Int? = null,
    @SerializedName("total_chapters_bn") val totalChaptersBn: String? = null,
    @SerializedName("authentic_grade_bn") val authenticGradeBn: String? = null,
    @SerializedName("authentic_grade_en") val authenticGradeEn: String? = null,
    @SerializedName("description_bn") val descriptionBn: String? = null,
    @SerializedName("description_en") val descriptionEn: String? = null
)

data class HadithChapterJson(
    val id: Int,
    @SerializedName("chapter_number") val chapterNumber: String? = null,
    @SerializedName("title_bn") val titleBn: String? = null,
    @SerializedName("title_en") val titleEn: String? = null,
    @SerializedName("title_ar") val titleAr: String? = null,
    @SerializedName("hadith_count") val hadithCount: Int? = null
)

data class HadithRawJson(
    val id: Int? = null,
    @SerializedName("book_id") val bookId: String? = null,
    @SerializedName("book_name_bn") val bookNameBn: String? = null,
    @SerializedName("book_name_en") val bookNameEn: String? = null,
    val collection: String? = null,
    @SerializedName("hadith_number") val hadithNumber: Any? = null,
    @SerializedName("chapter_id", alternate = ["chapterId"]) val chapterId: Int? = 1,
    @SerializedName("chapter_bn", alternate = ["chapterTitleBn"]) val chapterBn: String? = null,
    @SerializedName("chapter_en", alternate = ["chapterTitleEn"]) val chapterEn: String? = null,
    @SerializedName("topic_bn", alternate = ["topicBn"]) val topicBn: String? = null,
    @SerializedName("topic_en", alternate = ["topicEn"]) val topicEn: String? = null,
    val narrator: String? = null,
    @SerializedName("narrator_bn", alternate = ["narratorBn"]) val narratorBn: String? = null,
    @SerializedName("narrator_en", alternate = ["narratorEn"]) val narratorEn: String? = null,
    @SerializedName("arabic_text", alternate = ["arabic"]) val arabicText: String? = null,
    @SerializedName("translation_bangla", alternate = ["bangla"]) val translationBangla: String? = null,
    @SerializedName("translation_english", alternate = ["english"]) val translationEnglish: String? = null,
    @SerializedName("grade_bn", alternate = ["grade"]) val gradeBn: String? = null,
    @SerializedName("grade_en") val gradeEn: String? = null,
    @SerializedName("is_muttafaqun_alayh") val isMuttafaqunAlayh: Boolean? = null,
    @SerializedName("explanation_bn") val explanationBn: String? = null,
    @SerializedName("explanation_en") val explanationEn: String? = null,
    val reference: String? = null
)
