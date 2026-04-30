package com.zavtrak.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.zavtrak.keyboard.data.AppSettings
import com.zavtrak.keyboard.data.OneHandedMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Custom view that renders [Row]s of [Key]s and dispatches input events.
 *
 * - Touch handling: tap, long-press popup, swipe (space=cursor, backspace=word).
 * - Visual: rounded rect keys with optional borders, hint corner labels, special-key tinting.
 * - Stateless w.r.t. text — rendering depends only on [layout], [palette], [shifted], [capsLock].
 */
@Suppress("TooManyFunctions")
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    interface Listener {
        fun onKey(key: Key)
        fun onLongPressKey(key: Key, alt: String)
        fun onSpaceSwipe(deltaX: Int)
        fun onBackspaceWord()
        fun onShiftLongPress()
    }

    var listener: Listener? = null

    private var layoutData: KeyboardLayout = Layouts.English
    private var settings: AppSettings = AppSettings()
    private var palette: KeyboardPalette = ThemeMapper.resolve(context, settings)
    private var prependedRows: MutableList<Row> = mutableListOf()
    private var appendedRows: MutableList<Row> = mutableListOf()
    var shifted: Boolean = false
        set(value) { field = value; invalidate() }
    var capsLock: Boolean = false
        set(value) { field = value; invalidate() }
    var symbolMode: SymbolMode = SymbolMode.None
        private set

    enum class SymbolMode { None, Symbols1, Symbols2 }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }

    /** Cache: row Y offset and per-key bounds in pixels for hit testing. */
    private data class KeyBounds(val key: Key, val rect: RectF)
    private var bounds: List<KeyBounds> = emptyList()

    private val popup = KeyPopup(context)
    private val handlerRef = Handler(Looper.getMainLooper())

    // Touch state
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var pressedKey: Key? = null
    private var pressedRect: RectF? = null
    private var swipeDetected = false
    private var spaceCursorAccum = 0f
    private var longPressFired = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val longPressRunnable = Runnable {
        val key = pressedKey ?: return@Runnable
        val rect = pressedRect ?: return@Runnable
        if (key.code == KeyCodes.SHIFT) {
            longPressFired = true
            listener?.onShiftLongPress()
            return@Runnable
        }
        val alts = computeLongPressAlts(key)
        if (alts.isNotEmpty()) {
            longPressFired = true
            popup.show(this, rect, alts) { picked ->
                listener?.onLongPressKey(key, picked)
            }
        }
    }

    fun configure(
        layoutData: KeyboardLayout,
        settings: AppSettings,
        palette: KeyboardPalette,
        symbolMode: SymbolMode = SymbolMode.None,
    ) {
        this.layoutData = layoutData
        this.settings = settings
        this.palette = palette
        this.symbolMode = symbolMode
        prependedRows.clear()
        appendedRows.clear()
        if (settings.showNumberRow && symbolMode == SymbolMode.None) {
            prependedRows += Layouts.NumberRow
        }
        if (settings.showArrowsRow) {
            appendedRows += Layouts.ArrowsRow
        }
        requestLayout()
        invalidate()
    }

    private fun allRows(): List<Row> = prependedRows + layoutData.rows + appendedRows

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val w = MeasureSpec.getSize(widthSpec)
        val rowCount = allRows().size
        val rowHeight = dp(settings.keyHeight)
        val h = (rowCount * rowHeight).roundToInt()
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeBounds()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(palette.background)
        val keyRadius = dp(settings.keyCornerRadius)
        val gap = dp(3f)
        textPaint.textSize = dp(settings.fontSize)
        hintPaint.textSize = dp(settings.fontSize * 0.55f)
        textPaint.color = palette.keyText
        hintPaint.color = palette.keyHintText

        bounds.forEach { kb ->
            val r = kb.rect
            val key = kb.key
            val isSpace = key.code == KeyCodes.SPACE
            val isPressed = pressedKey === key
            val bg = when {
                isPressed -> palette.keyPressedBg
                key.special -> palette.keySpecialBg
                isSpace -> palette.keySpecialBg
                else -> palette.keyBg
            }
            keyPaint.color = bg
            val inset = gap / 2f
            val rr = RectF(r.left + inset, r.top + inset, r.right - inset, r.bottom - inset)
            canvas.drawRoundRect(rr, keyRadius, keyRadius, keyPaint)
            if (settings.showKeyBorders) {
                borderPaint.color = palette.borderColor
                borderPaint.strokeWidth = dp(0.7f)
                canvas.drawRoundRect(rr, keyRadius, keyRadius, borderPaint)
            }
            // Shift indicator
            if (key.code == KeyCodes.SHIFT) {
                if (capsLock) {
                    keyPaint.color = palette.accent
                    val cy = rr.bottom - dp(6f)
                    canvas.drawCircle(rr.centerX(), cy, dp(2f), keyPaint)
                }
            }
            drawKeyLabel(canvas, key, rr)
            // Number hint
            if (layoutData.numberHints && !key.special && key.numberHint != null && symbolMode == SymbolMode.None && !settings.showNumberRow && settings.longPressNumbers) {
                canvas.drawText(key.numberHint, rr.right - dp(6f), rr.top + dp(11f), hintPaint)
            }
        }
    }

    private fun drawKeyLabel(canvas: Canvas, key: Key, rr: RectF) {
        val raw = visibleLabel(key)
        val baseSize = when {
            key.special && raw.length > 2 -> dp(settings.fontSize * 0.7f)
            else -> dp(settings.fontSize)
        }
        textPaint.textSize = baseSize
        val fm = textPaint.fontMetrics
        val cx = rr.centerX()
        val cy = rr.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(raw, cx, cy, textPaint)
    }

    private fun visibleLabel(key: Key): String {
        if (key.code == KeyCodes.SPACE) {
            return if (layoutData.isAscii) "English" else "Русский"
        }
        if (key.code < 0) return key.label
        val l = key.label
        val shifted = capsLock || shifted
        return if (shifted && l.length == 1 && l[0].isLetter()) l.uppercase() else l
    }

    private fun recomputeBounds() {
        val list = mutableListOf<KeyBounds>()
        val rows = allRows()
        val rowHeight = dp(settings.keyHeight)
        val totalWidth = (width - sidePadding() * 2).toFloat()
        rows.forEachIndexed { rIdx, row ->
            val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
            var x = sidePadding().toFloat()
            val y = rIdx * rowHeight
            for (k in row.keys) {
                val w = totalWidth * (k.weight / totalWeight)
                val rect = RectF(x, y, x + w, y + rowHeight)
                list += KeyBounds(k, rect)
                x += w
            }
        }
        bounds = list
    }

    /** Side padding for one-handed mode. */
    private fun sidePadding(): Int {
        val full = width
        if (settings.oneHanded == OneHandedMode.Off) return 0
        // Reserve 25% of width as padding on the opposite side for one-handed mode.
        val p = (full * 0.25f).toInt()
        return p
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (settings.oneHanded == OneHandedMode.Left) {
            // shift bounds left
        }
        recomputeBounds()
    }

    private fun keyAt(x: Float, y: Float): KeyBounds? = bounds.firstOrNull { kb ->
        when (settings.oneHanded) {
            OneHandedMode.Off -> kb.rect.contains(x, y)
            OneHandedMode.Left -> kb.rect.contains(x, y) // simplified
            OneHandedMode.Right -> kb.rect.contains(x, y)
        }
    }

    private fun computeLongPressAlts(key: Key): List<String> {
        if (key.longPress.isNotEmpty()) return key.longPress
        // Auto-derive: if number hint exists and longPressNumbers enabled — show digit
        if (settings.longPressNumbers && key.numberHint != null && symbolMode == SymbolMode.None && !settings.showNumberRow) {
            return listOf(key.numberHint)
        }
        // Uppercase variants on letter keys
        return emptyList()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; lastX = event.x
                spaceCursorAccum = 0f
                swipeDetected = false
                longPressFired = false
                val kb = keyAt(event.x, event.y) ?: return true
                pressedKey = kb.key
                pressedRect = kb.rect
                invalidate()
                handlerRef.removeCallbacks(longPressRunnable)
                handlerRef.postDelayed(longPressRunnable, longPressTimeout)
            }
            MotionEvent.ACTION_MOVE -> {
                val key = pressedKey
                if (key != null && popup.isShowing) {
                    popup.update(event.rawX, event.rawY)
                    return true
                }
                if (key?.code == KeyCodes.SPACE && settings.swipeSpaceCursor) {
                    val dx = event.x - lastX
                    if (abs(event.x - downX) > touchSlop) {
                        swipeDetected = true
                        handlerRef.removeCallbacks(longPressRunnable)
                        spaceCursorAccum += dx
                        val step = dp(20f)
                        if (abs(spaceCursorAccum) >= step) {
                            val cells = (spaceCursorAccum / step).toInt()
                            listener?.onSpaceSwipe(cells)
                            spaceCursorAccum -= cells * step
                        }
                    }
                    lastX = event.x
                } else if (key?.code == KeyCodes.DELETE && settings.swipeBackspaceWord) {
                    if (downX - event.x > dp(48f) && !swipeDetected) {
                        swipeDetected = true
                        handlerRef.removeCallbacks(longPressRunnable)
                        listener?.onBackspaceWord()
                    }
                } else {
                    if (abs(event.x - downX) > touchSlop * 2 || abs(event.y - downY) > touchSlop * 2) {
                        // Re-target nearest key (sliding finger). Cancel long-press.
                        handlerRef.removeCallbacks(longPressRunnable)
                        val kb = keyAt(event.x, event.y)
                        if (kb != null && kb.key !== pressedKey) {
                            pressedKey = kb.key
                            pressedRect = kb.rect
                            invalidate()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                handlerRef.removeCallbacks(longPressRunnable)
                if (popup.isShowing) {
                    popup.dismiss()
                    pressedKey = null; pressedRect = null
                    invalidate()
                    return true
                }
                val key = pressedKey
                if (key != null && !swipeDetected && !longPressFired) {
                    listener?.onKey(key)
                }
                pressedKey = null; pressedRect = null
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                handlerRef.removeCallbacks(longPressRunnable)
                if (popup.isShowing) popup.dismiss()
                pressedKey = null; pressedRect = null
                invalidate()
            }
        }
        return true
    }

    fun applyPalette(p: KeyboardPalette) {
        palette = p
        invalidate()
    }
}
