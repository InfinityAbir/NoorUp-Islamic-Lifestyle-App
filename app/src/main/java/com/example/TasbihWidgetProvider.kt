package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.ui.noorup.TasbihCounter
import com.example.ui.noorup.TasbihFeedback
import com.example.ui.noorup.TasbihHistory
import com.example.ui.noorup.TasbihRepository
import com.example.ui.noorup.toBanglaDigits
import java.time.LocalDate

/**
 * Home-screen tasbih counter. Tapping counts without opening the app; the round position and
 * the zikr history are stored by the same code the in-app counter uses (TasbihCounter).
 */
class TasbihWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) render(context, appWidgetManager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TAP, ACTION_RESET, ACTION_PREV, ACTION_NEXT -> {
                val pending = goAsync()
                val app = context.applicationContext
                Thread {
                    try {
                        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        when (intent.action) {
                            ACTION_TAP -> {
                                val tap = TasbihCounter.tapFromWidget(prefs)
                                if (tap.roundCompleted) TasbihFeedback.vibrateCompletion(app)
                            }
                            ACTION_PREV -> TasbihCounter.selectAdjacentFromWidget(prefs, -1)
                            ACTION_NEXT -> TasbihCounter.selectAdjacentFromWidget(prefs, +1)
                            else -> TasbihCounter.resetFromWidget(prefs)
                        }
                        updateAll(app)
                    } catch (e: Exception) {
                        android.util.Log.e("TasbihWidget", "action failed", e)
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_TAP = "com.example.action.TASBIH_TAP"
        const val ACTION_RESET = "com.example.action.TASBIH_RESET"
        const val ACTION_PREV = "com.example.action.TASBIH_PREV"
        const val ACTION_NEXT = "com.example.action.TASBIH_NEXT"
        const val EXTRA_OPEN_TASBIH = "tasbih_open_tool"
        private const val PREFS = "noorup_prefs"

        /** Cheap when no tasbih widget is placed; safe from any thread. */
        fun updateAll(context: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, TasbihWidgetProvider::class.java))
                for (id in ids) render(context, mgr, id)
            } catch (e: Exception) {
                android.util.Log.e("TasbihWidget", "updateAll failed", e)
            }
        }

        private fun broadcast(context: Context, action: String, code: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, code,
                Intent(context, TasbihWidgetProvider::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        private fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val isEnglish = prefs.getBoolean("is_english", false)
            val views = RemoteViews(context.packageName, R.layout.tasbih_widget)
            try {
                val state = synchronized(TasbihCounter.lock) {
                    TasbihCounter.load(prefs, TasbihRepository.loadCustom(prefs))
                }
                val today = TasbihHistory.loadDay(prefs, LocalDate.now()).total
                fun n(v: Int) = if (isEnglish) v.toString() else v.toBanglaDigits()

                views.setTextViewText(R.id.tasbih_title, state.active.title(isEnglish))
                views.setTextViewText(
                    R.id.tasbih_today,
                    if (isEnglish) "Today ${n(today)}" else "আজ ${n(today)}"
                )
                views.setTextViewText(R.id.tasbih_count, n(state.count))
                views.setTextViewText(R.id.tasbih_goal, "/ ${n(state.goal)}")

                val multiStage = state.active.stages.size > 1
                views.setViewVisibility(R.id.tasbih_stage, if (multiStage) android.view.View.VISIBLE else android.view.View.GONE)
                if (multiStage) {
                    val label = state.active.stages.getOrNull(state.stageIndex)?.label(isEnglish).orEmpty()
                    views.setTextViewText(
                        R.id.tasbih_stage,
                        if (isEnglish) "Stage ${state.stageIndex + 1}/${state.active.stages.size} • $label"
                        else "স্তর ${n(state.stageIndex + 1)}/${n(state.active.stages.size)} • $label"
                    )
                }

                views.setProgressBar(R.id.tasbih_progress, 1000, (state.count * 1000L / state.goal.coerceAtLeast(1)).toInt().coerceIn(0, 1000), false)

                views.setTextViewText(
                    R.id.tasbih_btn_tap,
                    if (state.completed) (if (isEnglish) "↺ New round" else "↺ নতুন রাউন্ড")
                    else (if (isEnglish) "+ Count" else "+ গণনা")
                )
                views.setTextViewText(R.id.tasbih_btn_reset, if (isEnglish) "Reset" else "রিসেট")
                views.setTextViewText(R.id.tasbih_btn_open, if (isEnglish) "Open ›" else "খুলুন ›")
                views.setContentDescription(
                    R.id.tasbih_count_area,
                    if (isEnglish) "${state.active.title(true)}: ${state.count} of ${state.goal}. Tap to count."
                    else "${state.active.title(false)}: ${state.goal} এর মধ্যে ${state.count}। গণনা করতে চাপুন।"
                )

                val tapPi = broadcast(context, ACTION_TAP, 21)
                val resetPi = broadcast(context, ACTION_RESET, 22)
                val open = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EXTRA_OPEN_TASBIH, true)
                }
                val openPi = PendingIntent.getActivity(
                    context, 7100 + widgetId, open,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                views.setOnClickPendingIntent(R.id.tasbih_count_area, tapPi)
                views.setOnClickPendingIntent(R.id.tasbih_btn_tap, if (state.completed) resetPi else tapPi)
                views.setOnClickPendingIntent(R.id.tasbih_btn_reset, resetPi)
                views.setOnClickPendingIntent(R.id.tasbih_btn_prev, broadcast(context, ACTION_PREV, 23))
                views.setOnClickPendingIntent(R.id.tasbih_btn_next, broadcast(context, ACTION_NEXT, 24))
                views.setContentDescription(R.id.tasbih_btn_prev, if (isEnglish) "Previous zikr" else "আগের যিকির")
                views.setContentDescription(R.id.tasbih_btn_next, if (isEnglish) "Next zikr" else "পরের যিকির")
                views.setOnClickPendingIntent(R.id.tasbih_btn_open, openPi)
                views.setOnClickPendingIntent(R.id.tasbih_title, openPi)
            } catch (e: Exception) {
                android.util.Log.e("TasbihWidget", "render failed", e)
            }
            mgr.updateAppWidget(widgetId, views)
        }
    }
}
