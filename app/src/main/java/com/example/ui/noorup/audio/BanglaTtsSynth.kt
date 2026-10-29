package com.example.ui.noorup.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * No public per-ayah Bangla recitation exists, so the Bangla translation track is
 * synthesised on the device with the system Bengali text-to-speech voice and fed to
 * the player as an ordinary audio file. Synthesis happens off the main thread.
 */
object BanglaTtsSynth {

    private val lock = Any()
    private var tts: TextToSpeech? = null
    private var ready = false

    private val bengali = Locale.forLanguageTag("bn-BD")

    private fun ensureEngine(context: Context): TextToSpeech {
        synchronized(lock) {
            tts?.takeIf { ready }?.let { return it }
            tts?.shutdown()
            tts = null
            ready = false
            val latch = CountDownLatch(1)
            var status = TextToSpeech.ERROR
            val engine = TextToSpeech(context.applicationContext) { s ->
                status = s
                latch.countDown()
            }
            if (!latch.await(10, TimeUnit.SECONDS) || status != TextToSpeech.SUCCESS) {
                engine.shutdown()
                throw IOException("TTS engine unavailable")
            }
            val langResult = engine.setLanguage(bengali)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.shutdown()
                throw BanglaVoiceMissingException()
            }
            tts = engine
            ready = true
            return engine
        }
    }

    /** True when a Bengali voice is installed. Blocking: call from a background thread. */
    fun isBanglaVoiceAvailable(context: Context): Boolean = try {
        ensureEngine(context); true
    } catch (_: Exception) {
        false
    }

    fun cacheFile(context: Context, surah: Int, ayah: Int): File =
        File(File(context.cacheDir, "bn_tts").apply { mkdirs() }, "%03d_%03d.wav".format(surah, ayah))

    /** Synthesises (or reuses) the WAV for one ayah. Blocking. */
    fun synthesize(context: Context, surah: Int, ayah: Int, text: String): File {
        val out = cacheFile(context, surah, ayah)
        if (out.exists() && out.length() > 1024) return out
        if (text.isBlank()) throw IOException("empty text")
        val engine = ensureEngine(context)
        val tmp = File(out.parentFile, out.name + "." + UUID.randomUUID() + ".tmp")
        val latch = CountDownLatch(1)
        var failed = false
        val utteranceId = UUID.randomUUID().toString()
        synchronized(lock) {
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { if (id == utteranceId) latch.countDown() }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) { if (id == utteranceId) { failed = true; latch.countDown() } }
                override fun onError(id: String?, errorCode: Int) { if (id == utteranceId) { failed = true; latch.countDown() } }
            })
            val r = engine.synthesizeToFile(text, null, tmp, utteranceId)
            if (r != TextToSpeech.SUCCESS) throw IOException("TTS synth rejected")
        }
        if (!latch.await(60, TimeUnit.SECONDS) || failed || !tmp.exists() || tmp.length() <= 1024) {
            tmp.delete()
            throw IOException("TTS synth failed")
        }
        if (!tmp.renameTo(out)) { tmp.copyTo(out, overwrite = true); tmp.delete() }
        return out
    }

    class BanglaVoiceMissingException : IOException("Bengali TTS voice not installed")
}
