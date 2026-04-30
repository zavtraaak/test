package com.zavtrak.keyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.PopupWindow
import kotlin.math.max

/**
 * Floating popup shown on long-press: a horizontal strip of alt characters.
 * Selection follows the user's finger position.
 */
class KeyPopup(private val context: Context) {

    private val window: PopupWindow = PopupWindow(context).apply {
        isOutsideTouchable = false
        isFocusable = false
        isClippingEnabled = false
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
    private var view: PopupView? = null
    private var onSelect: ((String) -> Unit)? = null

    val isShowing: Boolean get() = window.isShowing

    fun show(anchor: View, anchorRect: RectF, alts: List<String>, onSelect: (String) -> Unit) {
        if (alts.isEmpty()) return
        this.onSelect = onSelect
        val v = PopupView(context, alts).also { view = it }
        val density = context.resources.displayMetrics.density
        val cellW = (44 * density).toInt()
        val cellH = (52 * density).toInt()
        val width = cellW * alts.size
        val height = cellH
        window.contentView = v
        window.width = width
        window.height = height
        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val cx = (anchorRect.centerX() + anchorLoc[0]).toInt() - width / 2
        val cy = (anchorRect.top + anchorLoc[1]).toInt() - height - (4 * density).toInt()
        // Clamp to screen
        val screenW = context.resources.displayMetrics.widthPixels
        val x = max(0, cx).coerceAtMost(screenW - width)
        window.showAtLocation(anchor, 0, x, cy)
        v.setHighlight(alts.size / 2)
    }

    fun update(rawX: Float, rawY: Float) {
        val v = view ?: return
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        val rel = (rawX - loc[0]).toInt().coerceAtLeast(0)
        val idx = (rel / (v.cellWidth)).coerceIn(0, v.alts.size - 1)
        v.setHighlight(idx)
    }

    fun dismiss() {
        val v = view
        if (v != null) {
            onSelect?.invoke(v.alts[v.highlightIndex])
        }
        onSelect = null
        view = null
        if (window.isShowing) window.dismiss()
    }
}

private class PopupView(context: Context, val alts: List<String>) : View(context) {
    private val density = resources.displayMetrics.density
    val cellWidth: Int = (44 * density).toInt()
    private val cellHeight: Int = (52 * density).toInt()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2E2E38.toInt() }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7C5CFF.toInt() }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 22 * density
    }
    var highlightIndex: Int = 0
        private set

    fun setHighlight(i: Int) { highlightIndex = i; invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(cellWidth * alts.size, cellHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 16 * density
        canvas.drawRoundRect(RectF(0f, 0f, w, h), r, r, bgPaint)
        for ((i, c) in alts.withIndex()) {
            val left = (i * cellWidth).toFloat()
            val rect = RectF(left + 2, 4f, left + cellWidth - 2, h - 4)
            if (i == highlightIndex) canvas.drawRoundRect(rect, r * 0.6f, r * 0.6f, highlightPaint)
            val fm = textPaint.fontMetrics
            val cy = h / 2f - (fm.ascent + fm.descent) / 2f
            canvas.drawText(c, rect.centerX(), cy, textPaint)
        }
    }
}
