package com.example.ui.noorup.audio

import android.net.Uri

/** Arabic reciters served as per-ayah MP3 files by EveryAyah (https://everyayah.com). */
enum class Reciter(
    val folder: String,
    val nameEn: String,
    val nameBn: String
) {
    ALAFASY("Alafasy_128kbps", "Mishary Rashid Alafasy", "মিশারী রাশিদ আল-আফাসী"),
    HUSARY("Husary_128kbps", "Mahmoud Khalil Al-Husary", "মাহমুদ খলিল আল-হুসারী"),
    ABDUL_BASIT("Abdul_Basit_Murattal_192kbps", "Abdul Basit Abdus-Samad", "আব্দুল বাসিত আব্দুস সামাদ");

    fun ayahUrl(surah: Int, ayah: Int): String =
        "https://everyayah.com/data/$folder/%03d%03d.mp3".format(surah, ayah)

    companion object {
        fun fromName(name: String?): Reciter = entries.firstOrNull { it.name == name } ?: ALAFASY
    }
}

enum class PlaybackMode(val titleEn: String, val titleBn: String) {
    DUAL("Arabic + translation", "আরবি + অনুবাদ"),
    ARABIC_ONLY("Arabic only", "শুধু আরবি"),
    TRANSLATION_ONLY("Translation only", "শুধু অনুবাদ");

    /** Number of playlist items per ayah. */
    val itemsPerAyah: Int get() = if (this == DUAL) 2 else 1

    companion object {
        fun fromName(name: String?): PlaybackMode = when (name) {
            "BANGLA_ONLY" -> TRANSLATION_ONLY // saved by versions before English audio existed
            else -> entries.firstOrNull { it.name == name } ?: DUAL
        }
    }
}

/**
 * Which translation is spoken. Bangla has no published recording, so it is synthesised on the phone;
 * English is a real human recording (Sahih International, read by Ibrahim Walk).
 */
enum class TranslationLang(val lang: AudioLang, val titleEn: String, val titleBn: String) {
    BANGLA(AudioLang.BN, "Bangla", "বাংলা"),
    ENGLISH(AudioLang.EN, "English", "ইংরেজি");

    fun ayahUrl(surah: Int, ayah: Int): String? =
        if (this == ENGLISH) "https://everyayah.com/data/English/Sahih_Intnl_Ibrahim_Walk_192kbps/%03d%03d.mp3".format(surah, ayah) else null

    companion object {
        fun fromName(name: String?): TranslationLang? = entries.firstOrNull { it.name == name }
    }
}

enum class LoopMode { OFF, AYAH, SURAH }

/** Which language track is sounding right now; drives which text gets highlighted. */
enum class AudioLang { AR, BN, EN }

data class AudioUiState(
    val active: Boolean = false,
    val surah: Int = 0,
    val ayah: Int = 0,
    val totalAyahs: Int = 0,
    val lang: AudioLang = AudioLang.AR,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val mode: PlaybackMode = PlaybackMode.DUAL,
    val translation: TranslationLang = TranslationLang.BANGLA,
    val reciter: Reciter = Reciter.ALAFASY,
    val loopMode: LoopMode = LoopMode.OFF,
    val ayahRepeatTarget: Int = 3,
    val ayahRepeatDone: Int = 0,
    val surahTitle: String = "",
    val error: AudioError? = null
)

enum class AudioError { NETWORK, BANGLA_VOICE_MISSING, GENERIC }

/** Item identity inside the playlist: "ar:2:255" / "bn:2:255". */
object AudioIds {
    const val TTS_SCHEME = "noorup-tts"

    fun mediaId(lang: AudioLang, surah: Int, ayah: Int): String =
        "${lang.name.lowercase()}:$surah:$ayah"

    fun parse(mediaId: String?): Triple<AudioLang, Int, Int>? {
        val parts = mediaId?.split(':') ?: return null
        if (parts.size != 3) return null
        val lang = when (parts[0]) {
            "ar" -> AudioLang.AR
            "bn" -> AudioLang.BN
            "en" -> AudioLang.EN
            else -> return null
        }
        val s = parts[1].toIntOrNull() ?: return null
        val a = parts[2].toIntOrNull() ?: return null
        return Triple(lang, s, a)
    }

    fun ttsUri(surah: Int, ayah: Int): Uri = Uri.parse("$TTS_SCHEME://bn/$surah/$ayah")
}
