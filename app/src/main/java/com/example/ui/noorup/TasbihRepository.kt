package com.example.ui.noorup

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Locale
import java.util.UUID

/** One count target inside a tasbih round. A round may have several stages. */
data class TasbihStage(
    val labelBangla: String,
    val labelEnglish: String = "",
    val target: Int
) {
    fun label(isEnglish: Boolean): String =
        if (isEnglish) labelEnglish.ifBlank { labelBangla }
        else labelBangla.ifBlank { labelEnglish }
}

/**
 * A complete tasbih definition. Built-ins live in code and are never written
 * to the user's mutable list; only custom definitions can be edited or deleted.
 */
data class TasbihDhikr(
    val id: String,
    val titleBangla: String,
    val titleEnglish: String = "",
    val arabicText: String = "",
    val stages: List<TasbihStage>,
    val isBuiltIn: Boolean = false
) {
    val totalTarget: Int get() = stages.sumOf { it.target }

    fun title(isEnglish: Boolean): String =
        if (isEnglish) titleEnglish.ifBlank { titleBangla }
        else titleBangla.ifBlank { titleEnglish }

    fun fullTitle(isEnglish: Boolean): String {
        val primary = title(isEnglish)
        val secondary = if (isEnglish) titleBangla else titleEnglish
        return if (secondary.isBlank() || secondary.equals(primary, ignoreCase = true)) primary
        else "$primary ($secondary)"
    }
}

/** Emitted exactly once when the final stage of a round is completed. */
data class TasbihCompletionEvent(
    val titleBangla: String,
    val titleEnglish: String,
    val totalTarget: Int,
    val stageCount: Int
)

/**
 * Persistence and validation for custom tasbih definitions.
 *
 * Built-in protection is enforced in two places: built-ins are never part of
 * the saved list, and [sanitizeCustom] rejects any saved entry whose id
 * collides with a built-in id. Invalid/partial JSON is ignored rather than
 * crashing the app.
 */
object TasbihRepository {

    const val PREFS_KEY = "tasbih_custom_dhikrs_v1"
    const val MAX_TARGET = 100000
    const val MAX_STAGES = 12
    const val MAX_CUSTOM_DHIKRS = 100
    const val MAX_TITLE_LENGTH = 80
    const val MAX_LABEL_LENGTH = 80
    const val MAX_ARABIC_LENGTH = 1000

    private val gson = Gson()
    private val idPattern = Regex("[A-Za-z0-9_.-]+")
    private val stageId = "builtin_tasbih_fatimah"

    val builtIns: List<TasbihDhikr> = listOf(
        TasbihDhikr(
            id = "builtin_subhanallah",
            titleBangla = "সুবহানাল্লাহ",
            titleEnglish = "Subhanallah",
            arabicText = "سُبْحَانَ اللَّهِ",
            stages = listOf(TasbihStage("সুবহানাল্লাহ", "Subhanallah", 33)),
            isBuiltIn = true
        ),
        TasbihDhikr(
            id = "builtin_alhamdulillah",
            titleBangla = "আলহামদুলিল্লাহ",
            titleEnglish = "Alhamdulillah",
            arabicText = "الْحَمْدُ لِلَّهِ",
            stages = listOf(TasbihStage("আলহামদুলিল্লাহ", "Alhamdulillah", 33)),
            isBuiltIn = true
        ),
        TasbihDhikr(
            id = "builtin_allahu_akbar",
            titleBangla = "আল্লাহু আকবার",
            titleEnglish = "Allahu Akbar",
            arabicText = "اللَّهُ أَكْبَرُ",
            stages = listOf(TasbihStage("আল্লাহু আকবার", "Allahu Akbar", 34)),
            isBuiltIn = true
        ),
        TasbihDhikr(
            id = "builtin_astaghfirullah",
            titleBangla = "আস্তাগফিরুল্লাহ",
            titleEnglish = "Astaghfirullah",
            arabicText = "أَسْتَغْفِرُ اللَّهَ",
            stages = listOf(TasbihStage("আস্তাগফিরুল্লাহ", "Astaghfirullah", 100)),
            isBuiltIn = true
        ),
        TasbihDhikr(
            id = "builtin_la_ilaha_illallah",
            titleBangla = "লা ইলাহা ইল্লাল্লাহ",
            titleEnglish = "La ilaha illallah",
            arabicText = "لَا إِلَٰهَ إِلَّا اللَّهُ",
            stages = listOf(TasbihStage("লা ইলাহা ইল্লাল্লাহ", "La ilaha illallah", 100)),
            isBuiltIn = true
        ),
        TasbihDhikr(
            id = stageId,
            titleBangla = "তাসবীহে ফাতিমী",
            titleEnglish = "Tasbih of Fatimah",
            arabicText = "",
            stages = listOf(
                TasbihStage("সুবহানাল্লাহ", "Subhanallah", 33),
                TasbihStage("আলহামদুলিল্লাহ", "Alhamdulillah", 33),
                TasbihStage("আল্লাহু আকবার", "Allahu Akbar", 34)
            ),
            isBuiltIn = true
        )
    )

    private val builtInIds: Set<String> = builtIns.map { it.id }.toSet()

    fun isBuiltInId(id: String): Boolean = id in builtInIds

    fun findById(id: String, custom: List<TasbihDhikr>): TasbihDhikr? =
        builtIns.firstOrNull { it.id == id } ?: custom.firstOrNull { it.id == id }

    fun findBuiltInByLegacyText(text: String): TasbihDhikr? {
        val needle = text.trim()
        if (needle.isEmpty()) return null
        return builtIns.firstOrNull { builtIn ->
            needle.equals(builtIn.titleBangla, ignoreCase = true) ||
                needle.equals(builtIn.titleEnglish, ignoreCase = true) ||
                needle.contains(builtIn.titleEnglish, ignoreCase = true) ||
                needle.contains(builtIn.titleBangla) ||
                needle.contains(builtIn.id, ignoreCase = true)
        }
    }

    fun legacyDefinition(text: String, target: Int): TasbihDhikr {
        val saneTarget = target.coerceIn(1, MAX_TARGET)
        val title = text.trim().ifBlank { builtIns.first().titleBangla }
        return TasbihDhikr(
            id = "legacy_active",
            titleBangla = title,
            titleEnglish = title,
            stages = listOf(TasbihStage(title, title, saneTarget)),
            isBuiltIn = false
        )
    }

    fun newCustomId(): String = "custom_${UUID.randomUUID()}"

    /**
     * Returns null when the input is valid, or a stable error code for the UI.
     * Codes: title, stages, too_many_stages, target.
     */
    fun validationError(
        titleBangla: String,
        titleEnglish: String,
        stages: List<TasbihStage>
    ): String? {
        if (titleBangla.isBlank() && titleEnglish.isBlank()) return "title"
        if (stages.isEmpty()) return "stages"
        if (stages.size > MAX_STAGES) return "too_many_stages"
        if (stages.any { it.target !in 1..MAX_TARGET }) return "target"
        return null
    }

    fun sanitizeCustom(raw: List<TasbihDhikr>): List<TasbihDhikr> {
        val seenIds = HashSet<String>()
        return raw.asSequence()
            .mapNotNull { normalizeCustom(it) }
            .filter { seenIds.add(it.id.lowercase(Locale.ROOT)) }
            .take(MAX_CUSTOM_DHIKRS)
            .toList()
    }

    private fun normalizeCustom(item: TasbihDhikr): TasbihDhikr? {
        val id = item.id.trim()
        if (id.length !in 1..100 || !idPattern.matches(id) || isBuiltInId(id)) return null

        val titleBn = item.titleBangla.trim().take(MAX_TITLE_LENGTH)
        val titleEn = item.titleEnglish.trim().take(MAX_TITLE_LENGTH)
        if (titleBn.isBlank() && titleEn.isBlank()) return null

        val fallbackLabel = titleBn.ifBlank { titleEn }
        val stages = item.stages.asSequence()
            .take(MAX_STAGES)
            .mapNotNull { stage ->
                if (stage.target !in 1..MAX_TARGET) return@mapNotNull null
                val labelBn = stage.labelBangla.trim().take(MAX_LABEL_LENGTH)
                val labelEn = stage.labelEnglish.trim().take(MAX_LABEL_LENGTH)
                TasbihStage(
                    labelBangla = labelBn.ifBlank { labelEn }.ifBlank { fallbackLabel },
                    labelEnglish = labelEn.ifBlank { labelBn }.ifBlank { fallbackLabel },
                    target = stage.target
                )
            }
            .toList()
        if (stages.isEmpty()) return null

        return TasbihDhikr(
            id = id,
            titleBangla = titleBn.ifBlank { titleEn },
            titleEnglish = titleEn.ifBlank { titleBn },
            arabicText = item.arabicText.trim().take(MAX_ARABIC_LENGTH),
            stages = stages,
            isBuiltIn = false
        )
    }

    fun loadCustom(prefs: SharedPreferences): List<TasbihDhikr> {
        val raw = prefs.getString(PREFS_KEY, null) ?: return emptyList()
        return try {
            val array = JsonParser.parseString(raw).asJsonArray
            val parsed = array.mapNotNull { element ->
                runCatching {
                    val obj = element.asJsonObject
                    TasbihDhikr(
                        id = obj.string("id"),
                        titleBangla = obj.string("titleBangla"),
                        titleEnglish = obj.string("titleEnglish"),
                        arabicText = obj.string("arabicText"),
                        stages = obj.getAsJsonArray("stages")?.mapNotNull { stageElement ->
                            runCatching {
                                val stage = stageElement.asJsonObject
                                TasbihStage(
                                    labelBangla = stage.string("labelBangla"),
                                    labelEnglish = stage.string("labelEnglish"),
                                    target = stage.int("target")
                                )
                            }.getOrNull()
                        }.orEmpty(),
                        isBuiltIn = false
                    )
                }.getOrNull()
            }
            sanitizeCustom(parsed)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustom(prefs: SharedPreferences, custom: List<TasbihDhikr>) {
        val safe = sanitizeCustom(custom)
        prefs.edit().putString(PREFS_KEY, gson.toJson(safe)).apply()
    }

    private fun JsonObject.string(name: String): String {
        val element = get(name) ?: return ""
        return if (element.isJsonNull) "" else runCatching { element.asString }.getOrDefault("")
    }

    private fun JsonObject.int(name: String): Int {
        val element = get(name) ?: return 0
        return if (element.isJsonNull) 0 else runCatching { element.asInt }.getOrDefault(0)
    }
}
