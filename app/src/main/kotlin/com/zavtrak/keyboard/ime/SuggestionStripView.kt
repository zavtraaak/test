package com.zavtrak.keyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * 3-slot strip: shows up to N suggestions plus a quick-actions zone.
 * Center suggestion is the one applied on space-tap autocorrect.
 */
class SuggestionStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener { fun onSuggestionPicked(text: String) }

    var listener: Listener? = null

    private var palette: KeyboardPalette = ThemeMapper.resolve(context, com.zavtrak.keyboard.data.AppSettings())
    private var suggestions: List<String> = emptyList()
    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 15 * density
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setPalette(p: KeyboardPalette) { palette = p; invalidate() }
    fun setSuggestions(list: List<String>) { suggestions = list; invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (40 * density).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(palette.suggestionStripBg)
        if (suggestions.isEmpty()) return
        val cellW = width.toFloat() / suggestions.size
        val cy = height / 2f - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f
        textPaint.color = palette.keyText
        dividerPaint.color = palette.divider
        highlightPaint.color = palette.accent and 0x33FFFFFF or (0x22 shl 24)
        for (i in suggestions.indices) {
            val cx = (i + 0.5f) * cellW
            // Highlight middle suggestion (autocorrect candidate)
            if (i == 1 && suggestions.size >= 2) {
                val r = RectF(i * cellW + 6 * density, 4 * density, (i + 1) * cellW - 6 * density, height - 4 * density)
                canvas.drawRoundRect(r, 8 * density, 8 * density, highlightPaint)
            }
            canvas.drawText(suggestions[i], cx, cy, textPaint)
            if (i > 0) {
                val x = i * cellW
                canvas.drawLine(x, 8 * density, x, height - 8 * density, dividerPaint)
            }
        }
    }

    private var downIdx = -1
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (suggestions.isEmpty()) return false
                downIdx = (event.x / (width.toFloat() / suggestions.size)).toInt().coerceIn(0, suggestions.size - 1)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (downIdx >= 0 && downIdx < suggestions.size) {
                    listener?.onSuggestionPicked(suggestions[downIdx])
                }
                downIdx = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** Returns the autocorrect candidate (middle slot) or null if no autocorrect. */
    fun autocorrectCandidate(): String? =
        if (suggestions.size >= 2) suggestions[1] else null
}
