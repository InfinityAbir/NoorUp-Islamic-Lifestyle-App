package com.example.ui.noorup.audio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.ui.noorup.quran.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * One process-wide SimpleCache (SimpleCache must be a singleton per directory).
 * Everything the player streams lands here, so played ayat replay offline, and
 * [QuranAudioDownloader] can pre-fill it for a whole surah.
 */
@OptIn(UnstableApi::class)
object QuranAudioCache {
    private const val MAX_BYTES = 1024L * 1024L * 1024L

    @Volatile private var cache: SimpleCache? = null

    fun cache(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "quran_audio"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext)
        ).also { cache = it }
    }

    /** Resolves noorup-tts:// items by synthesising the Bangla translation to a local WAV. */
    private fun ttsResolver(context: Context) = ResolvingDataSource.Resolver { spec: DataSpec ->
        if (spec.uri.scheme == AudioIds.TTS_SCHEME) {
            val segments = spec.uri.pathSegments
            val surah = segments.getOrNull(0)?.toIntOrNull()
            val ayah = segments.getOrNull(1)?.toIntOrNull()
            if (surah == null || ayah == null) {
                spec
            } else {
                val text = runBlocking {
                    QuranRepository.loadFullSurah(context, surah).firstOrNull { it.number == ayah }?.banglaTranslation
                }.orEmpty()
                val file = BanglaTtsSynth.synthesize(context, surah, ayah, text)
                spec.withUri(Uri.fromFile(file))
            }
        } else spec
    }

    fun dataSourceFactory(context: Context): CacheDataSource.Factory {
        val app = context.applicationContext
        val http = DefaultDataSource.Factory(app)
        val resolving: DataSource.Factory = ResolvingDataSource.Factory(http, ttsResolver(app))
        return CacheDataSource.Factory()
            .setCache(cache(app))
            .setUpstreamDataSourceFactory(resolving)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun isOnUnmeteredNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun hasInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/** Pre-downloads a surah's Arabic recitation into the shared cache for offline playback. */
@OptIn(UnstableApi::class)
object QuranAudioDownloader {

    private const val PREFS = "noorup_prefs"

    private fun flag(reciter: Reciter, surah: Int) = "audio_dl_${reciter.name}_$surah"
    private fun englishFlag(surah: Int) = "audio_dl_en_$surah"

    fun isDownloaded(context: Context, reciter: Reciter, surah: Int): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(flag(reciter, surah), false)

    fun isEnglishDownloaded(context: Context, surah: Int): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(englishFlag(surah), false)

    /**
     * Downloads every ayah of [surah]. [onProgress] gets (done, total). Returns true when
     * all ayat are cached. Throws on network failure so the UI can say so; cancelling the
     * calling coroutine stops after the current file.
     */
    suspend fun downloadSurah(
        context: Context,
        reciter: Reciter,
        surah: Int,
        totalAyahs: Int,
        arabic: Boolean,
        english: Boolean,
        onProgress: (Int, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val factory = QuranAudioCache.dataSourceFactory(app)
        val total = totalAyahs * ((if (arabic) 1 else 0) + (if (english) 1 else 0))
        var done = 0
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        fun fetch(urlFor: (Int) -> String) {
            for (ayah in 1..totalAyahs) {
                CacheWriter(factory.createDataSource(), DataSpec(Uri.parse(urlFor(ayah))), null, null).cache()
                onProgress(++done, total)
            }
        }
        if (arabic) {
            currentCoroutineContext().ensureActive()
            fetch { reciter.ayahUrl(surah, it) }
            prefs.edit().putBoolean(flag(reciter, surah), true).apply()
        }
        if (english) {
            currentCoroutineContext().ensureActive()
            fetch { TranslationLang.ENGLISH.ayahUrl(surah, it)!! }
            prefs.edit().putBoolean(englishFlag(surah), true).apply()
        }
        true
    }
}
