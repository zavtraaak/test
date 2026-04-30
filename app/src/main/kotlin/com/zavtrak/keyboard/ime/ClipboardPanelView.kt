package com.zavtrak.keyboard.ime

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.edit
import org.json.JSONArray

/**
 * In-memory clipboard manager with persistent pinned items.
 * Up to 16 most-recent entries shown; pinned items stay until removed.
 */
class ClipboardPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onClipboardEntryPicked(text: String)
        fun onClose()
    }

    var listener: Listener? = null

    data class Entry(val text: String, val pinned: Boolean = false, val ts: Long = System.currentTimeMillis())

    private var palette: KeyboardPalette = ThemeMapper.resolve(context, com.zavtrak.keyboard.data.AppSettings())
    private var incognito: Boolean = false
    private val density = resources.displayMetrics.density
    private val prefs = context.getSharedPreferences("clipboard", Context.MODE_PRIVATE)
    private var entries: MutableList<Entry> = mutableListOf()

    init {
        orientation = VERTICAL
        load()
        rebuild()
    }

    fun setPalette(p: KeyboardPalette, incognito: Boolean) {
        this.palette = p; this.incognito = incognito; rebuild()
    }

    fun addEntry(text: String) {
        if (incognito) return
        if (text.isBlank() || text.length > 4000) return
        // Avoid duplicates
        entries.removeAll { it.text == text && !it.pinned }
        entries.add(0, Entry(text))
        // Keep at most 16 unpinned
        var unpinned = 0
        entries = entries.filter { e ->
            if (e.pinned) true else { unpinned++; unpinned <= 16 }
        }.toMutableList()
        save()
        rebuild()
    }

    private fun load() {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            entries.clear()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                entries += Entry(o.getString("t"), o.optBoolean("p", false), o.optLong("ts", 0L))
            }
        } catch (_: Throwable) { entries.clear() }
    }

    private fun save() {
        val arr = JSONArray()
        for (e in entries) {
            val o = org.json.JSONObject()
            o.put("t", e.text); o.put("p", e.pinned); o.put("ts", e.ts)
            arr.put(o)
        }
        prefs.edit { putString("entries", arr.toString()) }
    }

    private fun rebuild() {
        removeAllViews()
        setBackgroundColor(palette.background)

        // Header: title + Close + Clear all
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(palette.suggestionStripBg)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(context).apply {
            text = "Clipboard"
            setTextColor(palette.keyText)
            textSize = 16f
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(Button(context).apply {
            text = "Clear"
            setTextColor(palette.keyText)
            setBackgroundColor(palette.keySpecialBg)
            setOnClickListener {
                entries.removeAll { !it.pinned }
                save(); rebuild()
            }
        })
        header.addView(Button(context).apply {
            text = "ABC"
            setTextColor(palette.keyText)
            setBackgroundColor(palette.keySpecialBg)
            setOnClickListener { listener?.onClose() }
        })
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val scroll = ScrollView(context).apply { setBackgroundColor(palette.background) }
        val list = LinearLayout(context).apply {
            orientation = VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(8))
        }
        scroll.addView(list)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0).apply { weight = 1f })

        if (entries.isEmpty()) {
            list.addView(TextView(context).apply {
                text = "Nothing copied yet"
                setTextColor(palette.keyHintText)
                gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, dp(40))
            })
            return
        }

        // Pinned first
        val sorted = entries.sortedByDescending { (if (it.pinned) 1 else 0) * 1_000_000L + it.ts }
        for (e in sorted) {
            val card = LinearLayout(context).apply {
                orientation = HORIZONTAL
                setBackgroundColor(palette.keyBg)
                setPadding(dp(12), dp(8), dp(8), dp(8))
                gravity = Gravity.CENTER_VERTICAL
            }
            card.addView(TextView(context).apply {
                text = e.text
                setTextColor(palette.keyText)
                maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.END
                setOnClickListener { listener?.onClipboardEntryPicked(e.text) }
            }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            card.addView(Button(context).apply {
                text = if (e.pinned) "📌" else "📍"
                setTextColor(palette.keyText)
                setBackgroundColor(palette.keySpecialBg)
                setOnClickListener {
                    val idx = entries.indexOfFirst { it.text == e.text && it.ts == e.ts }
                    if (idx >= 0) {
                        entries[idx] = entries[idx].copy(pinned = !entries[idx].pinned)
                        save(); rebuild()
                    }
                }
            })
            card.addView(Button(context).apply {
                text = "✕"
                setTextColor(palette.keyText)
                setBackgroundColor(palette.keySpecialBg)
                setOnClickListener {
                    entries.removeAll { it.text == e.text && it.ts == e.ts }
                    save(); rebuild()
                }
            })
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(2), 0, dp(2))
            }
            list.addView(card, params)
        }
    }

    private fun dp(v: Int): Int = (v * density).toInt()
}
