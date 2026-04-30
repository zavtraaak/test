package com.zavtrak.keyboard.ime

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.edit

/**
 * Emoji picker with category tabs, recently-used, and a scrollable grid.
 * Persists "recents" in a private SharedPreferences (skipped in incognito mode).
 */
class EmojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onEmojiPicked(emoji: String)
        fun onClose()
        fun onBackspace()
    }

    var listener: Listener? = null
    private var palette: KeyboardPalette = ThemeMapper.resolve(context, com.zavtrak.keyboard.data.AppSettings())
    private var incognito: Boolean = false
    private val density = resources.displayMetrics.density

    private val categories: List<Pair<String, List<String>>> = listOf(
        "Recents" to emptyList(),
        "😀" to "😀😃😄😁😆😅🤣😂🙂🙃😉😊😇🥰😍🤩😘😗☺😚😙🥲😋😛😜🤪😝🤑🤗🤭🤫🤔🤐🤨😐😑😶😏😒🙄😬🤥😌😔😪🤤😴😷🤒🤕🤧🥵🥶🥴😵🤯🤠🥳🥸😎🤓🧐😕😟🙁☹😮😯😲😳🥺😦😧😨😰😥😢😭😱😖😣😞😓😩😫🥱😤😡😠🤬😈👿💀☠💩🤡👹👺👻👽👾🤖😺😸😹😻😼😽🙀😿😾".toCharSeq(),
        "🐶" to "🐶🐱🐭🐹🐰🦊🐻🐼🐨🐯🦁🐮🐷🐽🐸🐵🙈🙉🙊🐒🐔🐧🐦🐤🐣🐥🦆🦅🦉🦇🐺🐗🐴🦄🐝🪱🐛🦋🐌🐞🐜🪰🪲🦗🕷🕸🦂🐢🐍🦎🦖🦕🐙🦑🦐🦞🦀🐡🐠🐟🐬🐳🐋🦈🐊🐅🐆🦓🦍🦧🦣🐘🦛🦏🐪🐫🦒🦘🐃🐂🐄🐎🐖🐏🐑🦙🐐🦌🐕🐩🦮🐈🪶🐓🦃🦤🦚🦜🦢🦩🕊🐇🦝🦨🦡🦫🦦🦥🐁🐀🐿🦔".toCharSeq(),
        "🍕" to "🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍆🥑🥦🥬🥒🌶🫑🌽🥕🫒🧄🧅🥔🍠🥐🥯🍞🥖🥨🧀🥚🍳🧈🥞🧇🥓🥩🍗🍖🦴🌭🍔🍟🍕🥪🥙🧆🌮🌯🫔🥗🥘🫕🥫🍝🍜🍲🍛🍣🍱🥟🦪🍤🍙🍚🍘🍥🥠🥮🍢🍡🍧🍨🍦🥧🧁🍰🎂🍮🍭🍬🍫🍿🍩🍪🌰🥜🍯🥛🍼☕🍵🧃🥤🧋🍶🍺🍻🥂🍷🥃🍸🍹🧉🍾🧊🥄🍴🍽🥣🥡🥢🧂".toCharSeq(),
        "⚽" to "⚽🏀🏈⚾🥎🎾🏐🏉🥏🎱🪀🏓🏸🏒🏑🥍🏏🪃🥅⛳🪁🏹🎣🤿🥊🥋🎽🛹🛼🛷⛸🥌🎿⛷🏂🪂🏋⛹⛹⛹🤺🤾🏌🏇🧘🏄🏊🤽🚣🧗🚵🚴🏆🥇🥈🥉🏅🎖🏵🎗🎫🎟🎪🤹🎭🩰🎨🎬🎤🎧🎼🎹🥁🪘🎷🎺🎸🪕🎻🎲♟🎯🎳🎮🎰🧩".toCharSeq(),
        "🚗" to "🚗🚕🚙🚌🚎🏎🚓🚑🚒🚐🛻🚚🚛🚜🦯🦽🦼🛴🚲🛵🏍🛺🚨🚔🚍🚘🚖🚡🚠🚟🚃🚋🚞🚝🚄🚅🚈🚂🚆🚇🚊🚉✈🛫🛬🛩💺🛰🚀🛸🚁🛶⛵🚤🛥🛳⛴🚢⚓⛽🚧🚦🚥🚏🗺🗿🗽🗼🏰🏯🏟🎡🎢🎠⛲⛱🏖🏝🏜🌋⛰🏔🗻🏕⛺🏠🏡🏘🏚🏗🏭🏢🏬🏣🏤🏥🏦🏨🏪🏫🏩💒🏛⛪🕌🕍🛕🕋⛩🛤🛣🗾🎑🏞🌅🌄🌠🎇🎆🌇🌆🏙🌃🌌🌉🌁".toCharSeq(),
        "💡" to "⌚📱📲💻⌨🖥🖨🖱🖲🕹🗜💽💾💿📀📼📷📸📹🎥📽🎞📞☎📟📠📺📻🎙🎚🎛🧭⏱⏲⏰🕰⌛⏳📡🔋🔌💡🔦🕯🪔🧯🛢💸💵💴💶💷🪙💰💳💎⚖🪜🧰🪛🔧🔨⚒🛠⛏🪚🔩⚙🪤🧱⛓🧲🔫💣🧨🪓🔪🗡⚔🛡🚬⚰🪦⚱🏺🔮📿🧿💈⚗🔭🔬🕳🩹🩺💊💉🩸🧬🦠🧫🧪🌡🧹🪠🧺🧻🚽🚰🚿🛁🛀🧼🪥🪒🧽🧴🛎🔑🗝🚪🪑🛋🛏🛌🧸🪆🖼🪞🪟🛍🛒🎁🎈🎏🎀🪄🪅🎊🎉🎎🏮🎐🧧✉📩📨📧💌📥📤📦🏷🪧📪📫📬📭📮📯📜📃📄📑🧾📊📈📉🗒🗓📆📅🗑📇🗃🗳🗄📋📁📂🗂🗞📰📓📔📒📕📗📘📙📚📖🔖🧷🔗📎🖇📐📏🧮📌📍✂🖊🖋✒🖌🖍📝✏🔍🔎🔏🔐🔒🔓".toCharSeq(),
        "❤️" to "❤️🧡💛💚💙💜🖤🤍🤎💔❣💕💞💓💗💖💘💝💟☮✝☪🕉☸✡🔯🕎☯☦🛐⛎♈♉♊♋♌♍♎♏♐♑♒♓🆔⚛🉑☢☣📴📳🈶🈚🈸🈺🈷✴🆚💮🉐㊙㊗🈴🈵🈹🈲🅰🅱🆎🆑🅾🆘❌⭕🛑⛔📛🚫💯💢♨🚷🚯🚳🚱🔞📵🚭❗❕❓❔‼⁉🔅🔆〽⚠🚸🔱⚜🔰♻✅🈯💹❇✳❎🌐💠Ⓜ🌀💤🏧🚾♿🅿🛗🈳🈂🛂🛃🛄🛅🚹🚺🚼⚧🚻🚮🎦📶🈁🔣ℹ🔤🔡🔠🆖🆗🆙🆒🆕🆓0️⃣1️⃣2️⃣3️⃣4️⃣5️⃣6️⃣7️⃣8️⃣9️⃣🔟🔢#️⃣*️⃣⏏▶⏸⏯⏹⏺⏭⏮⏩⏪⏫⏬◀🔼🔽➡⬅⬆⬇↗↘↙↖↕↔↪↩⤴⤵🔀🔁🔂🔄🔃🎵🎶➕➖➗✖🟰♾💲💱™©®〰➰➿🔚🔙🔛🔝🔜✔☑🔘🔴🟠🟡🟢🔵🟣⚫⚪🟤🔺🔻🔸🔹🔶🔷🔳🔲▪▫◾◽◼◻🟥🟧🟨🟩🟦🟪⬛⬜🟫🔈🔇🔉🔊🔔🔕📣📢👁‍🗨💬💭🗯♠♣♥♦🃏🎴🀄🕐🕑🕒🕓🕔🕕🕖🕗🕘🕙🕚🕛🕜🕝🕞🕟🕠🕡🕢🕣🕤🕥🕦🕧".toCharSeq(),
        "🏁" to "🏁🚩🎌🏴🏳🏳‍🌈🏳‍⚧🏴‍☠🇷🇺🇺🇸🇬🇧🇨🇦🇩🇪🇫🇷🇪🇸🇮🇹🇯🇵🇨🇳🇰🇷🇮🇳🇧🇷🇲🇽🇦🇺🇳🇿🇸🇪🇳🇴🇩🇰🇫🇮🇮🇪🇵🇱🇨🇿🇦🇹🇨🇭🇧🇪🇳🇱🇵🇹🇬🇷🇹🇷🇺🇦🇧🇾🇰🇿🇦🇿🇬🇪🇦🇲🇲🇩🇪🇪🇱🇻🇱🇹🇮🇸🇭🇰🇸🇬🇹🇭🇻🇳🇮🇩🇲🇾🇵🇭".toCharSeq(),
    )

    private fun String.toCharSeq(): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < length) {
            val cp = codePointAt(i)
            val cc = Character.charCount(cp)
            // Try to merge ZWJ sequences and variation selectors with previous emoji
            val ch = substring(i, i + cc)
            // Naïvely add — this list is hand-curated so each glyph is a single-code-point emoji.
            out += ch
            i += cc
        }
        return out
    }

    private val recents: MutableList<String> = mutableListOf()
    private val recentsPrefs = context.getSharedPreferences("emoji_recents", Context.MODE_PRIVATE)

    init {
        orientation = VERTICAL
        loadRecents()
        rebuild()
    }

    fun setPalette(p: KeyboardPalette, incognito: Boolean) {
        this.palette = p
        this.incognito = incognito
        rebuild()
    }

    private fun loadRecents() {
        recents.clear()
        recentsPrefs.getString("list", "")
            ?.split("\u0001")
            ?.filter { it.isNotEmpty() }
            ?.let { recents.addAll(it) }
    }

    private fun saveRecents() {
        if (incognito) return
        recentsPrefs.edit { putString("list", recents.joinToString("\u0001")) }
    }

    private fun pushRecent(e: String) {
        recents.remove(e)
        recents.add(0, e)
        if (recents.size > 32) recents.removeAt(recents.size - 1)
        saveRecents()
    }

    private fun rebuild() {
        removeAllViews()
        setBackgroundColor(palette.background)

        // Tabs
        val tabs = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(palette.suggestionStripBg)
        }
        val tabsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        tabs.addView(tabsRow)
        addView(tabs, LayoutParams(LayoutParams.MATCH_PARENT, dp(40)))

        // Content
        val scroll = ScrollView(context).apply {
            setBackgroundColor(palette.background)
        }
        val contentHolder = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        scroll.addView(contentHolder)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0).apply { weight = 1f })

        // Bottom row: ABC | space | backspace
        val bottom = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(palette.suggestionStripBg)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        bottom.addView(textButton("ABC") { listener?.onClose() }, lpWeight(1f))
        bottom.addView(textButton("⌫") { listener?.onBackspace() }, lpWeight(1f))
        addView(bottom, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))

        // Build grids per category, switch by tab click
        val grids = mutableListOf<View>()
        for ((label, glyphs) in categories) {
            val list = if (label == "Recents") recents else glyphs
            val grid = buildGrid(list)
            grids += grid
        }

        for ((idx, cat) in categories.withIndex()) {
            val tab = TextView(context).apply {
                text = cat.first
                setTextColor(palette.keyText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(6), dp(12), dp(6))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    contentHolder.removeAllViews()
                    contentHolder.addView(grids[idx])
                }
            }
            tabsRow.addView(tab)
        }

        // Show first non-empty by default
        val initial = if (recents.isEmpty()) 1 else 0
        contentHolder.addView(grids[initial])
    }

    private fun buildGrid(emojis: List<String>): View {
        val cols = 8
        val grid = GridLayout(context).apply {
            columnCount = cols
            useDefaultMargins = false
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        if (emojis.isEmpty()) {
            val empty = TextView(context).apply {
                text = "—"
                setTextColor(palette.keyHintText)
                gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, dp(40))
            }
            val ll = LinearLayout(context).apply { orientation = VERTICAL; addView(empty) }
            return ll
        }
        for (e in emojis) {
            val tv = TextView(context).apply {
                text = e
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(6), dp(6), dp(6))
                setOnClickListener {
                    pushRecent(e)
                    listener?.onEmojiPicked(e)
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(44)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            grid.addView(tv, params)
        }
        return grid
    }

    private fun textButton(text: String, onClick: () -> Unit): View {
        return Button(context).apply {
            this.text = text
            setTextColor(palette.keyText)
            setBackgroundColor(palette.keySpecialBg)
            setOnClickListener { onClick() }
        }
    }

    private fun lpWeight(w: Float) = LayoutParams(0, LayoutParams.MATCH_PARENT).apply { weight = w; setMargins(dp(2), 0, dp(2), 0) }
    private fun dp(v: Int): Int = (v * density).toInt()
    private fun dp(v: Float): Int = (v * density).toInt()
}
