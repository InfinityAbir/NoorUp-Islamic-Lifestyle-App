package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ui.noorup.DuaItem
import com.example.ui.noorup.NoorUpRepository
import com.example.ui.noorup.quran.QuranRepository
import com.example.ui.noorup.toBanglaDigits
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** One thing the widget can show: a Quran ayah or a Masnoon dua. */
sealed interface AyatWidgetItem {
    val arabic: String
    val bangla: String
    val english: String
    val reference: String

    data class Verse(
        val surah: Int, val ayah: Int,
        override val arabic: String, override val bangla: String, override val english: String,
        val surahNameEn: String, val surahNameBn: String
    ) : AyatWidgetItem {
        override val reference get() = "$surahNameEn $surah:$ayah"
    }

    data class Dua(val dua: DuaItem) : AyatWidgetItem {
        override val arabic get() = dua.arabicText
        override val bangla get() = dua.meaningBangla
        override val english get() = dua.meaningEnglish
        override val reference get() = dua.reference
    }
}

object DailyAyatContent {
    /** Short, well-known, comforting ayat (verified against the bundled text before use). */
    private val curatedVerses = listOf(
        2 to 255, 2 to 286, 2 to 152, 2 to 153, 2 to 186, 2 to 201, 3 to 139, 3 to 159, 3 to 173,
        13 to 28, 14 to 7, 20 to 114, 29 to 69, 33 to 41, 39 to 53, 40 to 60, 49 to 13, 59 to 22,
        65 to 2, 65 to 3, 93 to 3, 93 to 5, 94 to 5, 94 to 6, 112 to 1
    )

    private const val PREFS = "noorup_prefs"
    private const val KEY_OVERRIDE_INDEX = "ayat_widget_override_index"
    private const val KEY_OVERRIDE_DAY = "ayat_widget_override_day"

    /** Alternates ayat and duas so a week mixes both. */
    private fun poolSize(): Int = curatedVerses.size + NoorUpRepository.duas.size

    private fun dayKey(cal: Calendar = Calendar.getInstance()) =
        cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)

    fun indexForToday(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = dayKey()
        if (prefs.getInt(KEY_OVERRIDE_DAY, 0) == today) {
            prefs.getInt(KEY_OVERRIDE_INDEX, -1).takeIf { it in 0 until poolSize() }?.let { return it }
        }
        return Math.floorMod(today * 31, poolSize())
    }

    /** "Refresh random": picks a different item for the rest of today. */
    fun pickRandom(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = indexForToday(context)
        var next = current
        val size = poolSize()
        if (size > 1) while (next == current) next = (0 until size).random()
        prefs.edit().putInt(KEY_OVERRIDE_INDEX, next).putInt(KEY_OVERRIDE_DAY, dayKey()).apply()
    }

    /** Blocking: call from a background thread. */
    fun itemAt(context: Context, index: Int): AyatWidgetItem {
        val verseCount = curatedVerses.size
        if (index >= verseCount) {
            val duas = NoorUpRepository.duas
            return AyatWidgetItem.Dua(duas[(index - verseCount).coerceIn(0, duas.lastIndex)])
        }
        val (s, a) = curatedVerses[index]
        val verse = runBlocking { QuranRepository.loadFullSurah(context, s) }.firstOrNull { it.number == a }
        val meta = QuranRepository.getSurahMeta(s)
        return if (verse != null) {
            AyatWidgetItem.Verse(
                s, a, verse.arabicText, verse.banglaTranslation, verse.englishTranslation,
                meta?.nameEnglish.orEmpty(), meta?.nameBangla.orEmpty()
            )
        } else {
            AyatWidgetItem.Dua(NoorUpRepository.duas.first())
        }
    }

    fun isBookmarked(context: Context, item: AyatWidgetItem): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return when (item) {
            is AyatWidgetItem.Verse -> prefs.getBoolean("bm_${item.surah}_${item.ayah}", false)
            is AyatWidgetItem.Dua -> prefs.getBoolean("dua_bm_${item.dua.id}", false)
        }
    }

    fun toggleBookmark(context: Context, item: AyatWidgetItem) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = when (item) {
            is AyatWidgetItem.Verse -> "bm_${item.surah}_${item.ayah}"
            is AyatWidgetItem.Dua -> "dua_bm_${item.dua.id}"
        }
        prefs.edit().putBoolean(key, !prefs.getBoolean(key, false)).apply()
    }
}

class DailyAyatWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        Thread {
            try {
                for (id in appWidgetIds) render(context, appWidgetManager, id)
            } finally {
                pending.finish()
            }
        }.start()
    }

    override fun onEnabled(context: Context) {
        scheduleNextDailyUpdate(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH, ACTION_BOOKMARK -> {
                val pending = goAsync()
                Thread {
                    try {
                        if (intent.action == ACTION_REFRESH) {
                            DailyAyatContent.pickRandom(context)
                        } else {
                            val item = DailyAyatContent.itemAt(context, DailyAyatContent.indexForToday(context))
                            DailyAyatContent.toggleBookmark(context, item)
                        }
                        updateAll(context)
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.action.AYAT_REFRESH"
        const val ACTION_BOOKMARK = "com.example.action.AYAT_BOOKMARK"
        const val EXTRA_OPEN_VERSE_SURAH = "ayat_open_surah"
        const val EXTRA_OPEN_VERSE_AYAH = "ayat_open_ayah"
        const val EXTRA_OPEN_DUA_ID = "ayat_open_dua_id"
        private const val WORK_NAME = "daily_ayat_widget_update"

        /** Blocking; call off the main thread. */
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, DailyAyatWidgetProvider::class.java))
            for (id in ids) render(context, mgr, id)
        }

        private fun broadcast(context: Context, action: String, code: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, code,
                Intent(context, DailyAyatWidgetProvider::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        private fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            val isEnglish = prefs.getBoolean("is_english", false)
            val views = RemoteViews(context.packageName, R.layout.daily_ayat_widget)
            try {
                val item = DailyAyatContent.itemAt(context, DailyAyatContent.indexForToday(context))
                val saved = DailyAyatContent.isBookmarked(context, item)
                val open = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    when (item) {
                        is AyatWidgetItem.Verse -> {
                            putExtra(EXTRA_OPEN_VERSE_SURAH, item.surah)
                            putExtra(EXTRA_OPEN_VERSE_AYAH, item.ayah)
                        }
                        is AyatWidgetItem.Dua -> putExtra(EXTRA_OPEN_DUA_ID, item.dua.id)
                    }
                }
                val openPi = PendingIntent.getActivity(
                    context, widgetId + 7000, open,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val kind = when (item) {
                    is AyatWidgetItem.Verse -> if (isEnglish) "Verse of the Day" else "আজকের আয়াত"
                    is AyatWidgetItem.Dua -> if (isEnglish) "Dua of the Day" else "আজকের দুআ"
                }
                val ref = when (item) {
                    is AyatWidgetItem.Verse ->
                        if (isEnglish) "${item.surahNameEn} ${item.surah}:${item.ayah}"
                        else "${item.surahNameBn} ${"${item.surah}:${item.ayah}".toBanglaDigits()}"
                    is AyatWidgetItem.Dua -> item.reference
                }
                val translation = if (isEnglish) item.english.ifBlank { item.bangla } else item.bangla.ifBlank { item.english }

                views.setTextViewText(R.id.ayat_kind, kind)
                views.setTextViewText(R.id.ayat_ref, ref)
                views.setTextViewText(R.id.ayat_arabic, item.arabic)
                views.setTextViewText(R.id.ayat_translation, translation)
                views.setTextViewText(R.id.ayat_btn_refresh, if (isEnglish) "⟳ Refresh" else "⟳ নতুন")
                views.setTextViewText(
                    R.id.ayat_btn_bookmark,
                    if (saved) (if (isEnglish) "★ Saved" else "★ সংরক্ষিত") else (if (isEnglish) "☆ Save" else "☆ সংরক্ষণ")
                )
                views.setTextViewText(R.id.ayat_btn_open, if (isEnglish) "Open ›" else "খুলুন ›")
                views.setOnClickPendingIntent(R.id.ayat_btn_refresh, broadcast(context, ACTION_REFRESH, 1))
                views.setOnClickPendingIntent(R.id.ayat_btn_bookmark, broadcast(context, ACTION_BOOKMARK, 2))
                views.setOnClickPendingIntent(R.id.ayat_btn_open, openPi)
                views.setOnClickPendingIntent(R.id.ayat_root, openPi)
            } catch (e: Exception) {
                android.util.Log.e("DailyAyatWidget", "render failed", e)
                views.setTextViewText(R.id.ayat_arabic, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")
                views.setTextViewText(R.id.ayat_translation, "")
            }
            mgr.updateAppWidget(widgetId, views)
        }

        /** Re-run shortly after the next local midnight so the widget rolls to the new day. */
        fun scheduleNextDailyUpdate(context: Context) {
            val now = Calendar.getInstance()
            val next = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 5); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val delay = (next.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DailyAyatWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
            )
        }
    }
}

class DailyAyatWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        DailyAyatWidgetProvider.updateAll(context)
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, DailyAyatWidgetProvider::class.java))
        if (ids.isNotEmpty()) DailyAyatWidgetProvider.scheduleNextDailyUpdate(context)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}
