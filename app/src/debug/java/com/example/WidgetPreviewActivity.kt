package com.example

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Debug-only harness: renders the real widget RemoteViews at an exact size so every size and
 * language can be checked without resizing it on a launcher.
 *
 * adb shell am start -n <pkg>/com.example.WidgetPreviewActivity --ei w 400 --ei h 300 --es lang en
 *
 * Logs one `WidgetPreview` line per problem (ellipsized or clipped text, content taller than the cell).
 */
class WidgetPreviewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wDp = intent.getIntExtra("w", 400)
        val hDp = intent.getIntExtra("h", 300)
        val english = intent.getStringExtra("lang") == "en"
        getSharedPreferences("noorup_prefs", MODE_PRIVATE).edit().putBoolean("is_english", english).commit()

        val density = resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()

        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#303030")); fitsSystemWindows = true }
        val cell = FrameLayout(this)
        root.addView(cell, FrameLayout.LayoutParams(wPx, hPx))
        setContentView(root)

        // Optional --es at 06:20 renders the widget as if it were that local time (today).
        val at = intent.getStringExtra("at")
        val now = if (at != null && at.contains(":")) {
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, at.substringBefore(":").toInt())
                set(java.util.Calendar.MINUTE, at.substringAfter(":").toInt())
                set(java.util.Calendar.SECOND, 0)
            }.timeInMillis
        } else System.currentTimeMillis()
        val views = NoorUpWidgetProvider.buildWidgetViews(this, wDp, hDp, now)
        val widget = views.apply(this, cell)
        cell.addView(widget, FrameLayout.LayoutParams(wPx, hPx))

        // Fit very wide cells on screen (layout is still done at the real size).
        val screenW = resources.displayMetrics.widthPixels
        if (wPx > screenW) {
            val s = screenW.toFloat() / wPx
            cell.pivotX = 0f; cell.pivotY = 0f; cell.scaleX = s; cell.scaleY = s
        }

        val tag = "WidgetPreview"
        val label = "${wDp}x${hDp} ${if (english) "en" else "bn"}"
        if (intent.getBooleanExtra("natural", false)) {
            // Natural (unconstrained) height of the content at this width, in dp.
            widget.measure(
                View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            Log.i(tag, "$label NATURAL_DP=${"%.1f".format(widget.measuredHeight / density)}")
            return
        }
        widget.post {
            var problems = 0
            fun walk(v: View) {
                if (v is TextView && v.visibility == View.VISIBLE && v.width > 0) {
                    val l = v.layout
                    if (l != null) {
                        var ell = false
                        for (i in 0 until l.lineCount) if (l.getEllipsisCount(i) > 0) ell = true
                        val name = runCatching { resources.getResourceEntryName(v.id) }.getOrDefault("text")
                        if (ell) { problems++; Log.w(tag, "$label ELLIPSIZED $name '${v.text}'") }
                        if (l.height > v.height + 2) { problems++; Log.w(tag, "$label CLIPPED $name '${v.text}' need=${l.height} have=${v.height}") }
                        if (l.getLineWidth(0) > v.width + 2f && v.maxLines == 1) { problems++; Log.w(tag, "$label WIDE $name '${v.text}'") }
                    }
                }
                if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
            walk(widget)
            val textSizes = HashSet<Float>()
            fun sizes(v: View) {
                if (v is TextView) textSizes.add(v.textSize / resources.displayMetrics.scaledDensity)
                if (v is ViewGroup) for (i in 0 until v.childCount) sizes(v.getChildAt(i))
            }
            sizes(widget)
            Log.i(tag, "$label DONE problems=$problems")
        }
    }
}
