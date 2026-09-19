package com.example.ui.noorup.hadith

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * Text utility to clean mojibake, double-encoded UTF-8 strings, and HTML entities
 * for flawless Bangla, Arabic, and English typography.
 */
object HadithTextNormalizer {

    private val UNICODE_ESCAPE_REGEX = Regex("\\\\u([0-9a-fA-F]{4})")

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var text = raw.trim()

        // 1. Decode literal Unicode escapes if present (e.g., \u0627 or \u0995)
        if (text.contains("\\u")) {
            try {
                text = UNICODE_ESCAPE_REGEX.replace(text) { matchResult ->
                    val hex = matchResult.groupValues[1]
                    hex.toInt(16).toChar().toString()
                }
            } catch (_: Exception) {}
        }

        // 2. Repair common UTF-8 Mojibake (when UTF-8 bytes were misread as Latin1 / ISO-8859-1 / Windows-1252)
        if (text.contains("à¦") || text.contains("à§") || text.contains("Ø§") || text.contains("Ù") || text.contains("Ã") || text.contains("â€") || text.contains("â")) {
            try {
                val bytes = text.toByteArray(Charsets.ISO_8859_1)
                val recovered = String(bytes, Charsets.UTF_8)
                if (recovered.isNotBlank() && !recovered.contains("\uFFFD")) {
                    text = recovered
                }
            } catch (_: Exception) {
                // Keep original if recovery fails
            }
        }

        // 3. Decode standard HTML / XML entities
        text = text
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&#8216;", "‘")
            .replace("&#8217;", "’")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&lrm;", "")
            .replace("&rlm;", "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "") // Strip HTML tags
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("[ \\t]+"), " ") // Normalize internal horizontal spaces without collapsing newlines
            .trim()

        return text
    }

    /**
     * Converts English numbers to Bengali numerals if needed.
     */
    fun toBengaliNumerals(numberStr: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (ch in numberStr) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}

/**
 * Data class representing a Canonical Hadith Book compilation (e.g., Sahih al-Bukhari, Sahih Muslim).
 */
data class Book(
    @SerializedName("id", alternate = ["book_id", "book_slug", "slug", "code"])
    val id: String = "",

    @SerializedName("name_bn", alternate = ["nameBn", "title_bn", "name", "title", "book_name_bn", "bookNameBn"])
    val nameBn: String = "",

    @SerializedName("name_en", alternate = ["nameEn", "title_en", "book_name_en", "bookNameEn"])
    val nameEn: String = "",

    @SerializedName("arabic_name", alternate = ["arabicName", "title_ar", "name_ar", "arabic_title", "book_name_ar"])
    val arabicName: String = "",

    @SerializedName("author_bn", alternate = ["authorBn", "author", "author_name_bn", "compiler_bn"])
    val authorBn: String = "",

    @SerializedName("author_en", alternate = ["authorEn", "author_name_en", "compiler_en"])
    val authorEn: String = "",

    @SerializedName("total_hadiths_bn", alternate = ["totalHadithsBn", "hadith_count_bn"])
    val totalHadithsBn: String = "",

    @SerializedName("total_hadiths_count", alternate = ["totalHadithsCount", "total_hadiths", "hadiths_count", "count", "total"])
    val totalHadithsCount: Int = 0,

    @SerializedName("total_chapters_bn", alternate = ["totalChaptersBn", "total_chapters", "chapters_count"])
    val totalChaptersBn: String = "",

    @SerializedName("authentic_grade_bn", alternate = ["authenticGradeBn", "grade_bn", "gradeBn", "authenticity_bn"])
    val authenticGradeBn: String = "সহীহ (Authentic)",

    @SerializedName("authentic_grade_en", alternate = ["authenticGradeEn", "grade_en", "gradeEn", "authenticity_en"])
    val authenticGradeEn: String = "Authentic",

    @SerializedName("description_bn", alternate = ["descriptionBn", "description", "desc_bn", "about_bn"])
    val descriptionBn: String = "",

    @SerializedName("description_en", alternate = ["descriptionEn", "about_en"])
    val descriptionEn: String = "",

    @SerializedName("asset_file", alternate = ["assetFile", "filename", "file_name"])
    val assetFileName: String = "$id.json"
)

/**
 * Data class representing an individual Chapter within a Hadith compilation.
 */
data class Chapter(
    @SerializedName("id", alternate = ["chapter_id", "chapterId", "number", "chapter_number"])
    val id: Int = 0,

    @SerializedName("book_id", alternate = ["bookId", "book_slug", "book"])
    val bookId: String = "",

    @SerializedName("chapter_number", alternate = ["chapterNumber", "chapter_no", "no"])
    val chapterNumber: String = id.toString(),

    @SerializedName("title_bn", alternate = ["titleBn", "name_bn", "chapter_bn", "chapter_title_bn", "name", "title"])
    val titleBn: String = "",

    @SerializedName("title_en", alternate = ["titleEn", "name_en", "chapter_en", "chapter_title_en"])
    val titleEn: String = "",

    @SerializedName("title_ar", alternate = ["titleAr", "name_ar", "chapter_ar", "chapter_title_ar"])
    val titleAr: String = "",

    @SerializedName("hadith_count", alternate = ["hadithCount", "count", "hadiths_count"])
    val hadithCount: Int = 0
)

/**
 * Data class representing an authentic Hadith entry with full multilingual texts, narrator, grading, and explanation.
 */
data class Hadith(
    @SerializedName("id", alternate = ["hadith_id", "hadithId", "item_id"])
    val id: Int = 0,

    @SerializedName("collection", alternate = ["citation", "reference_title", "book_ref", "source", "source_title"])
    val collection: String = "",

    @SerializedName("book_id", alternate = ["bookId", "book_slug", "book_name", "book"])
    val bookId: String = "bukhari",

    @SerializedName("book_name_bn", alternate = ["bookNameBn", "book_title_bn", "kitab_bn"])
    val bookNameBn: String = "সহীহ আল-বুখারী",

    @SerializedName("book_name_en", alternate = ["bookNameEn", "book_title_en", "kitab_en"])
    val bookNameEn: String = "Sahih al-Bukhari",

    @SerializedName("hadith_number", alternate = ["hadithNumber", "number", "hadith_no", "hadithNo", "no", "id_in_book", "serial"])
    val hadithNumber: String = "১",

    @SerializedName("chapter_id", alternate = ["chapterId", "chapter_no"])
    val chapterId: Int = 1,

    @SerializedName("chapter_bn", alternate = ["chapterBn", "chapter_title_bn", "chapter", "chapter_name", "kitab", "bab", "bab_bn"])
    val chapterBn: String = "",

    @SerializedName("chapter_en", alternate = ["chapterEn", "chapter_title_en", "chapter_title"])
    val chapterEn: String = "",

    @SerializedName("topic_bn", alternate = ["topicBn", "topic", "topic_name", "category_bn", "category", "theme_bn", "theme", "section_bn", "section", "topic_name_bn"])
    val topicBn: String = "নিয়ত ও ইখলাস",

    @SerializedName("topic_en", alternate = ["topicEn", "category_en", "theme_en", "section_en"])
    val topicEn: String = "Intentions",

    @SerializedName("narrator", alternate = ["narrator_bn", "narratorBn", "narrated_by", "rawi", "ravi", "rawi_bn", "narrator_name", "sanad", "sanad_bn"])
    val narrator: String = "",

    @SerializedName("arabic_text", alternate = ["arabicText", "arabic", "text_ar", "ar", "hadith_arabic", "content_ar", "matn_ar", "matn", "ar_text", "body_ar", "textArabic"])
    val arabicText: String = "",

    @SerializedName("translation_bangla", alternate = ["translationBangla", "bengali", "bangla", "bn", "text_bn", "translation_bn", "hadith_bangla", "content_bn", "matn_bn", "bn_text", "body_bn", "meaning_bn", "meaning", "translation", "textBangla", "hadithBangla", "bengali_text"])
    val translationBangla: String = "",

    @SerializedName("translation_english", alternate = ["translationEnglish", "english", "en", "text_en", "translation_en", "hadith_english", "content_en", "matn_en", "en_text", "body_en", "meaning_en", "textEnglish"])
    val translationEnglish: String = "",

    @SerializedName("grade_bn", alternate = ["gradeBn", "grading_bn", "grade", "grading", "tahqiq", "status", "authenticity", "grade_bangla", "hukm", "grade_bn_text"])
    val gradeBn: String = "সহীহ (Sahih)",

    @SerializedName("grade_en", alternate = ["gradeEn", "grading_en", "grade_english", "status_en"])
    val gradeEn: String = "Sahih",

    @SerializedName("is_muttafaqun_alayh", alternate = ["isMuttafaqunAlayh", "muttafaqun_alayh", "muttafaq", "agreed_upon"])
    val isMuttafaqunAlayh: Boolean = false,

    @SerializedName("explanation_bn", alternate = ["explanationBn", "lesson_bn", "lessons_bn", "benefits_bn", "sharh_bn", "note_bn", "tafsir_bn", "note", "notes", "explanation", "life_lesson", "lesson"])
    val explanationBn: String = "",

    @SerializedName("explanation_en", alternate = ["explanationEn", "lesson_en", "lessons_en", "sharh_en"])
    val explanationEn: String = "",

    @SerializedName("reference", alternate = ["ref", "reference_info", "source_info"])
    val reference: String = "",

    var isBookmarked: Boolean = false
)

/**
 * Custom flexible JsonDeserializer for Hadith that handles varied key formats,
 * mixed data types (int vs string), and normalizes text for UTF-8 / Mojibake integrity.
 */
class HadithJsonDeserializer : JsonDeserializer<Hadith> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Hadith {
        if (!json.isJsonObject) {
            return Hadith()
        }
        val obj = json.asJsonObject

        fun getString(vararg keys: String): String {
            for (key in keys) {
                if (obj.has(key)) {
                    val el = obj.get(key)
                    if (!el.isJsonNull) {
                        return HadithTextNormalizer.normalize(el.asString)
                    }
                }
            }
            return ""
        }

        fun getInt(vararg keys: String, defaultVal: Int = 0): Int {
            for (key in keys) {
                if (obj.has(key)) {
                    val el = obj.get(key)
                    if (!el.isJsonNull) {
                        return try {
                            el.asInt
                        } catch (_: Exception) {
                            try {
                                el.asString.filter { it.isDigit() }.toInt()
                            } catch (_: Exception) {
                                defaultVal
                            }
                        }
                    }
                }
            }
            return defaultVal
        }

        fun getBoolean(vararg keys: String, defaultVal: Boolean = false): Boolean {
            for (key in keys) {
                if (obj.has(key)) {
                    val el = obj.get(key)
                    if (!el.isJsonNull) {
                        return try {
                            if (el.isJsonPrimitive && el.asJsonPrimitive.isBoolean) {
                                el.asBoolean
                            } else {
                                val s = el.asString.lowercase()
                                s == "true" || s == "1" || s == "yes" || s.contains("muttafaq") || s.contains("মুত্তাফাক")
                            }
                        } catch (_: Exception) {
                            defaultVal
                        }
                    }
                }
            }
            return defaultVal
        }

        val id = getInt("id", "hadith_id", "hadithId", "item_id", defaultVal = 0)
        val bookId = getString("book_id", "bookId", "book_slug", "book", "book_name").ifBlank { "bukhari" }
        val bookNameBn = getString("book_name_bn", "bookNameBn", "book_title_bn", "kitab_bn").ifBlank { "সহীহ হাদিস" }
        val bookNameEn = getString("book_name_en", "bookNameEn", "book_title_en", "kitab_en").ifBlank { "Authentic Hadith" }

        val hadithNumberRaw = getString("hadith_number", "hadithNumber", "number", "hadith_no", "hadithNo", "no", "id_in_book", "serial")
        val hadithNumber = if (hadithNumberRaw.isNotBlank()) hadithNumberRaw else if (id > 0) id.toString() else "১"

        val chapterId = getInt("chapter_id", "chapterId", "chapter_no", defaultVal = 1)
        val chapterBn = getString("chapter_bn", "chapterBn", "chapter_title_bn", "chapter", "chapter_name", "kitab", "bab", "bab_bn")
        val chapterEn = getString("chapter_en", "chapterEn", "chapter_title_en", "chapter_title")

        val topicBn = getString("topic_bn", "topicBn", "topic", "topic_name", "category_bn", "category", "theme_bn", "theme", "section_bn", "section", "topic_name_bn").ifBlank { "হাদিস ও নসীহত" }
        val topicEn = getString("topic_en", "topicEn", "category_en", "theme_en", "section_en").ifBlank { "Hadith & Guidance" }

        val narrator = getString("narrator", "narrator_bn", "narratorBn", "narrated_by", "rawi", "ravi", "rawi_bn", "narrator_name", "sanad", "sanad_bn")
        val arabicText = getString("arabic_text", "arabicText", "arabic", "text_ar", "ar", "hadith_arabic", "content_ar", "matn_ar", "matn", "ar_text", "body_ar", "textArabic")
        val translationBangla = getString("translation_bangla", "translationBangla", "bengali", "bangla", "bn", "text_bn", "translation_bn", "hadith_bangla", "content_bn", "matn_bn", "bn_text", "body_bn", "meaning_bn", "meaning", "translation", "textBangla", "hadithBangla", "bengali_text")
        val translationEnglish = getString("translation_english", "translationEnglish", "english", "en", "text_en", "translation_en", "hadith_english", "content_en", "matn_en", "en_text", "body_en", "meaning_en", "textEnglish")

        val gradeBn = getString("grade_bn", "gradeBn", "grading_bn", "grade", "grading", "tahqiq", "status", "authenticity", "grade_bangla", "hukm", "grade_bn_text").ifBlank { "সহীহ" }
        val gradeEn = getString("grade_en", "gradeEn", "grading_en", "grade_english", "status_en").ifBlank { "Sahih" }

        val isMuttafaqunAlayh = getBoolean("is_muttafaqun_alayh", "isMuttafaqunAlayh", "muttafaqun_alayh", "muttafaq", "agreed_upon") ||
                gradeBn.contains("মুত্তাফাক") || gradeEn.contains("Agreed")

        val explanationBn = getString("explanation_bn", "explanationBn", "lesson_bn", "lessons_bn", "benefits_bn", "sharh_bn", "note_bn", "tafsir_bn", "note", "notes", "explanation", "life_lesson", "lesson")
        val explanationEn = getString("explanation_en", "explanationEn", "lesson_en", "lessons_en", "sharh_en")

        val reference = getString("reference", "ref", "reference_info", "source_info", "source", "collection", "citation").ifBlank {
            "$bookNameBn: $hadithNumber"
        }

        val collection = getString("collection", "citation", "reference_title", "book_ref", "source_title").ifBlank {
            reference
        }

        return Hadith(
            id = id,
            collection = collection,
            bookId = bookId,
            bookNameBn = bookNameBn,
            bookNameEn = bookNameEn,
            hadithNumber = hadithNumber,
            chapterId = chapterId,
            chapterBn = chapterBn,
            chapterEn = chapterEn,
            topicBn = topicBn,
            topicEn = topicEn,
            narrator = narrator,
            arabicText = arabicText,
            translationBangla = translationBangla,
            translationEnglish = translationEnglish,
            gradeBn = gradeBn,
            gradeEn = gradeEn,
            isMuttafaqunAlayh = isMuttafaqunAlayh,
            explanationBn = explanationBn,
            explanationEn = explanationEn,
            reference = reference,
            isBookmarked = false
        )
    }
}

/**
 * Top-level JSON wrapper mapping datasets that include metadata, chapters, and hadiths.
 */
data class HadithBookDataset(
    @SerializedName("metadata", alternate = ["book", "info", "header"])
    val metadata: Book? = null,

    @SerializedName("chapters", alternate = ["chapter_list", "sections", "books"])
    val chapters: List<Chapter> = emptyList(),

    @SerializedName("hadiths", alternate = ["items", "data", "records", "list", "hadith", "result"])
    val hadiths: List<Hadith> = emptyList()
)

// Aliases for seamless semantic interchangeability
typealias HadithBook = Book
typealias HadithChapter = Chapter
typealias HadithItem = Hadith
