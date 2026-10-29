package com.example.ui.noorup.audio

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI-side handle to [QuranPlaybackService]. Builds the per-surah playlist
 * (Arabic, Bangla or both interleaved), mirrors player state into [state], and
 * implements ayah-repeat and surah-loop for Hifz.
 */
class QuranAudioController(
    private val app: Application,
    private val scope: CoroutineScope
) : Player.Listener {

    private val prefs = app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        AudioUiState(
            mode = PlaybackMode.fromName(prefs.getString("audio_mode", null)),
            translation = TranslationLang.fromName(prefs.getString("audio_translation", null))
                // First run: speak the translation in the language the app is set to.
                ?: if (prefs.getBoolean("is_english", false)) TranslationLang.ENGLISH else TranslationLang.BANGLA,
            reciter = Reciter.fromName(prefs.getString("audio_reciter", null)),
            speed = prefs.getFloat("audio_speed", 1f).takeIf { it in SPEEDS } ?: 1f,
            ayahRepeatTarget = prefs.getInt("audio_repeat_target", 3).coerceIn(2, 20)
        )
    )
    val state: StateFlow<AudioUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connecting = false
    private val queued = mutableListOf<(MediaController) -> Unit>()
    private var ticker: Job? = null

    private var perAyah = 2
    private var skippedBangla = false

    // --- connection ---------------------------------------------------------

    private fun withController(block: (MediaController) -> Unit) {
        controller?.let { block(it); return }
        queued += block
        if (connecting) return
        connecting = true
        val token = SessionToken(app, ComponentName(app, QuranPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            connecting = false
            val c = try { future.get() } catch (e: Exception) {
                queued.clear()
                _state.update { it.copy(error = AudioError.GENERIC) }
                return@addListener
            }
            controller = c
            c.addListener(this)
            queued.toList().also { queued.clear() }.forEach { it(c) }
            syncFromPlayer(c)
            startTicker()
        }, ContextCompat.getMainExecutor(app))
    }

    /** Re-attach to a session that kept playing after the UI was recreated. */
    fun attachIfPlaying() = withController { }

    private fun syncFromPlayer(c: MediaController) {
        if (c.mediaItemCount == 0) return
        val cur = AudioIds.parse(c.currentMediaItem?.mediaId) ?: return
        val last = AudioIds.parse(c.getMediaItemAt(c.mediaItemCount - 1).mediaId) ?: return
        val dual = c.mediaItemCount > 1 && AudioIds.parse(c.getMediaItemAt(1).mediaId)?.first != AudioIds.parse(c.getMediaItemAt(0).mediaId)?.first
        perAyah = if (dual) 2 else 1
        _state.update {
            it.copy(
                active = true, surah = cur.second, ayah = cur.third, lang = cur.first,
                totalAyahs = last.third, isPlaying = c.isPlaying,
                surahTitle = c.currentMediaItem?.mediaMetadata?.albumTitle?.toString().orEmpty(),
                mode = when {
                    dual -> PlaybackMode.DUAL
                    cur.first != AudioLang.AR -> PlaybackMode.TRANSLATION_ONLY
                    else -> PlaybackMode.ARABIC_ONLY
                },
                translation = if (c.getMediaItemAt(c.mediaItemCount - 1).mediaId.startsWith("en:")) TranslationLang.ENGLISH
                else if (c.getMediaItemAt(c.mediaItemCount - 1).mediaId.startsWith("bn:")) TranslationLang.BANGLA
                else it.translation,
                speed = c.playbackParameters.speed,
                loopMode = if (c.repeatMode == Player.REPEAT_MODE_ALL) LoopMode.SURAH else it.loopMode
            )
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(250)
                val c = controller ?: continue
                if (!_state.value.active) continue
                val dur = c.duration.takeIf { it > 0 } ?: 0L
                _state.update { it.copy(positionMs = c.currentPosition.coerceAtLeast(0), durationMs = dur) }
            }
        }
    }

    fun release() {
        ticker?.cancel()
        controller?.removeListener(this)
        controller?.release()
        controller = null
    }

    // --- commands -----------------------------------------------------------

    private fun groupStart(ayah: Int) = (ayah - 1) * perAyah

    private fun buildItems(
        surah: Int, total: Int, title: String, mode: PlaybackMode, translation: TranslationLang, reciter: Reciter
    ): List<MediaItem> {
        val items = ArrayList<MediaItem>(total * mode.itemsPerAyah)
        for (a in 1..total) {
            if (mode != PlaybackMode.TRANSLATION_ONLY) {
                items += item(AudioLang.AR, surah, a, title, android.net.Uri.parse(reciter.ayahUrl(surah, a)), reciter.nameEn)
            }
            if (mode != PlaybackMode.ARABIC_ONLY) {
                val uri = translation.ayahUrl(surah, a)?.let { android.net.Uri.parse(it) } ?: AudioIds.ttsUri(surah, a)
                val artist = if (translation == TranslationLang.ENGLISH) "English translation (Sahih International)" else "Bangla translation"
                items += item(translation.lang, surah, a, title, uri, artist)
            }
        }
        return items
    }

    private fun item(lang: AudioLang, surah: Int, ayah: Int, title: String, uri: android.net.Uri, artist: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(AudioIds.mediaId(lang, surah, ayah))
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("$title • $ayah")
                    .setArtist(artist)
                    .setAlbumTitle(title)
                    .build()
            )
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .build()

    /** Start (or restart) playback of [surah] from [startAyah] using the saved mode/reciter. */
    fun play(surah: Int, startAyah: Int, totalAyahs: Int, title: String) {
        if (totalAyahs <= 0) return
        val s = _state.value
        perAyah = s.mode.itemsPerAyah
        skippedBangla = false
        val start = startAyah.coerceIn(1, totalAyahs)
        val items = buildItems(surah, totalAyahs, title, s.mode, s.translation, s.reciter)
        val startLang = if (s.mode == PlaybackMode.TRANSLATION_ONLY) s.translation.lang else AudioLang.AR
        _state.update {
            it.copy(
                active = true, surah = surah, ayah = start, totalAyahs = totalAyahs, lang = startLang,
                surahTitle = title, positionMs = 0, durationMs = 0, ayahRepeatDone = 0, error = null, isBuffering = true
            )
        }
        withController { c ->
            c.setMediaItems(items, groupStart(start), 0L)
            c.repeatMode = if (s.loopMode == LoopMode.SURAH) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            c.setPlaybackSpeed(s.speed)
            c.prepare()
            c.play()
        }
    }

    /** Play one ayah then continue per the current mode. */
    fun playAyah(surah: Int, ayah: Int, totalAyahs: Int, title: String) = play(surah, ayah, totalAyahs, title)

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() = withController { c ->
        val s = _state.value
        when {
            s.ayah < s.totalAyahs -> { c.seekTo(groupStart(s.ayah + 1), 0L); resetRepeat() }
            s.loopMode == LoopMode.SURAH -> { c.seekTo(0, 0L); resetRepeat() }
        }
    }

    fun previous() = withController { c ->
        val s = _state.value
        if (c.currentPosition > 3000 || s.lang != AudioLang.AR || s.ayah <= 1) {
            c.seekTo(groupStart(s.ayah), 0L)
        } else {
            c.seekTo(groupStart(s.ayah - 1), 0L)
        }
        resetRepeat()
    }

    fun seekTo(ms: Long) = withController { it.seekTo(ms.coerceAtLeast(0L)) }

    fun setSpeed(speed: Float) {
        prefs.edit().putFloat("audio_speed", speed).apply()
        _state.update { it.copy(speed = speed) }
        withController { it.setPlaybackSpeed(speed) }
    }

    fun cycleLoop() {
        val next = when (_state.value.loopMode) {
            LoopMode.OFF -> LoopMode.AYAH
            LoopMode.AYAH -> LoopMode.SURAH
            LoopMode.SURAH -> LoopMode.OFF
        }
        _state.update { it.copy(loopMode = next, ayahRepeatDone = 0) }
        withController { it.repeatMode = if (next == LoopMode.SURAH) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF }
    }

    fun setRepeatTarget(n: Int) {
        val v = n.coerceIn(2, 20)
        prefs.edit().putInt("audio_repeat_target", v).apply()
        _state.update { it.copy(ayahRepeatTarget = v, ayahRepeatDone = 0) }
    }

    fun setMode(mode: PlaybackMode) {
        prefs.edit().putString("audio_mode", mode.name).apply()
        val before = _state.value
        _state.update { it.copy(mode = mode) }
        if (before.active) play(before.surah, before.ayah, before.totalAyahs, before.surahTitle)
    }

    fun setTranslation(t: TranslationLang) {
        prefs.edit().putString("audio_translation", t.name).apply()
        val before = _state.value
        _state.update { it.copy(translation = t) }
        if (before.active && before.mode != PlaybackMode.ARABIC_ONLY) {
            play(before.surah, before.ayah, before.totalAyahs, before.surahTitle)
        }
    }

    fun setReciter(reciter: Reciter) {
        prefs.edit().putString("audio_reciter", reciter.name).apply()
        val before = _state.value
        _state.update { it.copy(reciter = reciter) }
        if (before.active && before.mode != PlaybackMode.TRANSLATION_ONLY) {
            play(before.surah, before.ayah, before.totalAyahs, before.surahTitle)
        }
    }

    fun stop() {
        controller?.let { it.stop(); it.clearMediaItems() }
        _state.update { it.copy(active = false, isPlaying = false, positionMs = 0, durationMs = 0, ayahRepeatDone = 0, error = null) }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun resetRepeat() = _state.update { it.copy(ayahRepeatDone = 0) }

    // --- player callbacks ---------------------------------------------------

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val id = AudioIds.parse(mediaItem?.mediaId) ?: return
        val prev = _state.value
        val c = controller

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
            prev.loopMode == LoopMode.AYAH && id.third != prev.ayah && c != null
        ) {
            val done = prev.ayahRepeatDone + 1
            if (done < prev.ayahRepeatTarget) {
                _state.update { it.copy(ayahRepeatDone = done) }
                c.seekTo(groupStart(prev.ayah), 0L)
                return
            }
        }
        _state.update {
            it.copy(
                surah = id.second, ayah = id.third, lang = id.first, positionMs = 0,
                ayahRepeatDone = if (id.third != it.ayah) 0 else it.ayahRepeatDone
            )
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying) }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
        if (playbackState == Player.STATE_ENDED) {
            val s = _state.value
            val c = controller ?: return
            if (s.loopMode == LoopMode.AYAH) {
                val done = s.ayahRepeatDone + 1
                if (done < s.ayahRepeatTarget) {
                    _state.update { it.copy(ayahRepeatDone = done) }
                    c.seekTo(groupStart(s.ayah), 0L)
                    c.play()
                    return
                }
            }
            _state.update { it.copy(isPlaying = false, ayahRepeatDone = 0) }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        val chain = generateSequence<Throwable>(error) { it.cause }
        val c = controller
        when {
            chain.any { it is BanglaTtsSynth.BanglaVoiceMissingException } -> {
                // Skip the Bangla track rather than stall the recitation.
                _state.update { it.copy(error = AudioError.BANGLA_VOICE_MISSING) }
                skippedBangla = true
                if (c != null && c.hasNextMediaItem()) {
                    c.seekToNextMediaItem(); c.prepare(); c.play()
                }
            }
            chain.any { it is java.io.IOException } && !QuranAudioCache.hasInternet(app) ->
                _state.update { it.copy(error = AudioError.NETWORK, isPlaying = false) }
            else -> {
                _state.update { it.copy(error = AudioError.GENERIC, isPlaying = false) }
                // A single bad file (e.g. TTS failed on one ayah) should not end the session.
                if (c != null && AudioIds.parse(c.currentMediaItem?.mediaId)?.first == AudioLang.BN && c.hasNextMediaItem()) {
                    c.seekToNextMediaItem(); c.prepare(); c.play()
                }
            }
        }
    }

    // --- offline download -----------------------------------------------------

    data class DownloadUi(
        val surah: Int = 0,
        val done: Int = 0,
        val total: Int = 0,
        val running: Boolean = false,
        val failed: Boolean = false,
        val completedSurah: Int = 0
    )

    private val _download = MutableStateFlow(DownloadUi())
    val download: StateFlow<DownloadUi> = _download.asStateFlow()
    private var downloadJob: Job? = null

    private fun needsEnglishFiles(s: AudioUiState) = s.mode != PlaybackMode.ARABIC_ONLY && s.translation == TranslationLang.ENGLISH
    private fun needsArabicFiles(s: AudioUiState) = s.mode != PlaybackMode.TRANSLATION_ONLY

    /** True when every recording the current mode needs for [surah] is already on the phone. */
    fun isDownloaded(surah: Int): Boolean {
        val s = _state.value
        val arabic = !needsArabicFiles(s) || QuranAudioDownloader.isDownloaded(app, s.reciter, surah)
        val english = !needsEnglishFiles(s) || QuranAudioDownloader.isEnglishDownloaded(app, surah)
        // Bangla-only listening is synthesised on the device, so there is nothing to download.
        return arabic && english && (needsArabicFiles(s) || needsEnglishFiles(s))
    }

    /** Bangla-only has no files to fetch. */
    fun hasDownloadableAudio(): Boolean = needsArabicFiles(_state.value) || needsEnglishFiles(_state.value)

    fun downloadSurah(surah: Int, totalAyahs: Int) {
        if (_download.value.running) return
        val snapshot = _state.value
        val reciter = snapshot.reciter
        val arabic = needsArabicFiles(snapshot)
        val english = needsEnglishFiles(snapshot)
        if (!arabic && !english) return
        val steps = totalAyahs * ((if (arabic) 1 else 0) + (if (english) 1 else 0))
        _download.value = DownloadUi(surah = surah, total = steps, running = true)
        downloadJob = scope.launch {
            try {
                QuranAudioDownloader.downloadSurah(app, reciter, surah, totalAyahs, arabic, english) { done, total ->
                    _download.update { it.copy(done = done, total = total) }
                }
                _download.value = DownloadUi(surah = surah, total = steps, done = steps, completedSurah = surah)
            } catch (e: kotlinx.coroutines.CancellationException) {
                _download.value = DownloadUi()
                throw e
            } catch (e: Exception) {
                _download.update { it.copy(running = false, failed = true) }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _download.value = DownloadUi()
    }

    companion object {
        val SPEEDS = listOf(0.75f, 1.0f, 1.25f, 1.5f)
    }
}
