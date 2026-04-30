package com.zavtrak.keyboard.ime

/**
 * Static layout definitions. The keyboard renders each [Row] as a horizontal
 * line of keys. [Key.weight] controls width relative to siblings (default 1.0).
 *
 * Layout codes:
 *  - Negative codes are special actions handled in [KeyboardService].
 *  - Positive codes are characters typed directly.
 */
object KeyCodes {
    const val SHIFT = -1
    const val DELETE = -2
    const val ENTER = -3
    const val MODE_LETTERS = -4
    const val MODE_SYMBOLS_1 = -5
    const val MODE_SYMBOLS_2 = -6
    const val LANGUAGE = -7
    const val EMOJI = -8
    const val SPACE = -9
    const val COMMA = -10
    const val PERIOD = -11
    const val SETTINGS = -12
    const val CLIPBOARD = -13
    const val CURSOR_LEFT = -14
    const val CURSOR_RIGHT = -15
    const val CURSOR_UP = -16
    const val CURSOR_DOWN = -17
    const val NUMBER_TOGGLE = -18
    const val TAB = -19
}

data class Key(
    val label: String,
    val code: Int = if (label.isNotEmpty()) label.codePointAt(0) else 0,
    /** Long-press popup characters, in order. */
    val longPress: List<String> = emptyList(),
    /** Visible label when shifted, e.g. "Q"/"q". null = uppercase the label. */
    val shiftLabel: String? = null,
    /** True for command keys (shift, delete, …) — different background. */
    val special: Boolean = false,
    /** Relative width within a row. */
    val weight: Float = 1f,
    /** A drawable resource id to render instead of label, optional. */
    val iconName: String? = null,
    /** Number digit shown in top-right corner if [LayoutKind.numberHints] is on. */
    val numberHint: String? = null,
)

data class Row(val keys: List<Key>)

data class KeyboardLayout(
    val name: String,
    val rows: List<Row>,
    val isAscii: Boolean,
    val numberHints: Boolean = false,
)

private fun letterRow(s: String, longPress: Map<Char, String> = emptyMap(), digits: String? = null): Row =
    Row(s.mapIndexed { i, ch ->
        Key(
            label = ch.toString(),
            longPress = longPress[ch]?.map { it.toString() } ?: emptyList(),
            numberHint = digits?.getOrNull(i)?.toString(),
        )
    })

object Layouts {

    private val enLongPress = mapOf(
        'a' to "àáâäæãåā",
        'c' to "çćč",
        'e' to "èéêëēėę",
        'i' to "îïíīįì",
        'l' to "ł",
        'n' to "ñń",
        'o' to "ôöòóœøōõ",
        's' to "ßśš",
        'u' to "ûüùúū",
        'y' to "ÿ",
        'z' to "žźż",
    )

    val English = KeyboardLayout(
        name = "QWERTY",
        isAscii = true,
        numberHints = true,
        rows = listOf(
            letterRow("qwertyuiop", enLongPress, digits = "1234567890"),
            letterRow("asdfghjkl", enLongPress),
            Row(
                listOf(Key("⇧", code = KeyCodes.SHIFT, special = true, weight = 1.5f)) +
                    "zxcvbnm".map { ch ->
                        Key(label = ch.toString(), longPress = enLongPress[ch]?.map { it.toString() } ?: emptyList())
                    } +
                    listOf(Key("⌫", code = KeyCodes.DELETE, special = true, weight = 1.5f))
            ),
            Row(
                listOf(
                    Key("?123", code = KeyCodes.MODE_SYMBOLS_1, special = true, weight = 1.5f),
                    Key(",", code = KeyCodes.COMMA, special = true, longPress = listOf(";", ":", "!", "?", "&", "$", "%")),
                    Key("🌐", code = KeyCodes.LANGUAGE, special = true),
                    Key(" ", code = KeyCodes.SPACE, weight = 4f),
                    Key(".", code = KeyCodes.PERIOD, special = true, longPress = listOf("…", "!", "?", "—", "/", "\"", "'")),
                    Key("⏎", code = KeyCodes.ENTER, special = true, weight = 1.5f),
                ),
            ),
        ),
    )

    private val ruLongPress = mapOf(
        'е' to "ё",
        'ь' to "ъ",
    )

    val Russian = KeyboardLayout(
        name = "ЙЦУКЕН",
        isAscii = false,
        numberHints = true,
        rows = listOf(
            letterRow("йцукенгшщзх", digits = "12345678901").let { row ->
                // Russian top row has 11 keys; map number hints to first 10
                Row(row.keys.mapIndexed { i, k -> if (i < 10) k.copy(numberHint = (i + 1).mod(10).toString()) else k })
            },
            letterRow("фывапролджэ", ruLongPress),
            Row(
                listOf(Key("⇧", code = KeyCodes.SHIFT, special = true, weight = 1.5f)) +
                    "ячсмитьбю".map { ch ->
                        Key(label = ch.toString(), longPress = ruLongPress[ch]?.map { it.toString() } ?: emptyList())
                    } +
                    listOf(Key("⌫", code = KeyCodes.DELETE, special = true, weight = 1.5f))
            ),
            Row(
                listOf(
                    Key("?123", code = KeyCodes.MODE_SYMBOLS_1, special = true, weight = 1.5f),
                    Key(",", code = KeyCodes.COMMA, special = true, longPress = listOf(";", ":", "!", "?", "—", "«", "»")),
                    Key("🌐", code = KeyCodes.LANGUAGE, special = true),
                    Key(" ", code = KeyCodes.SPACE, weight = 4f),
                    Key(".", code = KeyCodes.PERIOD, special = true, longPress = listOf("…", "!", "?", "—", "—", "\"")),
                    Key("⏎", code = KeyCodes.ENTER, special = true, weight = 1.5f),
                ),
            ),
        ),
    )

    val Symbols1 = KeyboardLayout(
        name = "Symbols",
        isAscii = true,
        rows = listOf(
            letterRow("1234567890"),
            Row("@#\$_&-+()/".map { Key(it.toString()) } + listOf(Key("*"))),
            Row(
                listOf(Key("=\\<", code = KeyCodes.MODE_SYMBOLS_2, special = true, weight = 1.5f)) +
                    "!\"';:,.?".map { Key(it.toString()) } +
                    listOf(Key("⌫", code = KeyCodes.DELETE, special = true, weight = 1.5f))
            ),
            Row(
                listOf(
                    Key("ABC", code = KeyCodes.MODE_LETTERS, special = true, weight = 1.5f),
                    Key(",", code = KeyCodes.COMMA, special = true),
                    Key("🌐", code = KeyCodes.LANGUAGE, special = true),
                    Key(" ", code = KeyCodes.SPACE, weight = 4f),
                    Key(".", code = KeyCodes.PERIOD, special = true),
                    Key("⏎", code = KeyCodes.ENTER, special = true, weight = 1.5f),
                ),
            ),
        ),
    )

    val Symbols2 = KeyboardLayout(
        name = "More Symbols",
        isAscii = true,
        rows = listOf(
            letterRow("~`|•√π÷×¶∆"),
            letterRow("£¥€¢^°={}\\"),
            Row(
                listOf(Key("?123", code = KeyCodes.MODE_SYMBOLS_1, special = true, weight = 1.5f)) +
                    "%©®™✓[]".map { Key(it.toString()) } +
                    listOf(Key("⌫", code = KeyCodes.DELETE, special = true, weight = 1.5f))
            ),
            Row(
                listOf(
                    Key("ABC", code = KeyCodes.MODE_LETTERS, special = true, weight = 1.5f),
                    Key("<", special = true),
                    Key("🌐", code = KeyCodes.LANGUAGE, special = true),
                    Key(" ", code = KeyCodes.SPACE, weight = 4f),
                    Key(">", special = true),
                    Key("⏎", code = KeyCodes.ENTER, special = true, weight = 1.5f),
                ),
            ),
        ),
    )

    /** Optional top row of digits 1-0 prepended to letter layouts. */
    val NumberRow: Row = letterRow("1234567890")

    /** Optional cursor row: ←, ↓, ↑, →, Tab. */
    val ArrowsRow: Row = Row(
        listOf(
            Key("←", code = KeyCodes.CURSOR_LEFT, special = true),
            Key("↓", code = KeyCodes.CURSOR_DOWN, special = true),
            Key("↑", code = KeyCodes.CURSOR_UP, special = true),
            Key("→", code = KeyCodes.CURSOR_RIGHT, special = true),
            Key("⇥", code = KeyCodes.TAB, special = true, weight = 1.2f),
            Key("📋", code = KeyCodes.CLIPBOARD, special = true),
            Key("⚙", code = KeyCodes.SETTINGS, special = true),
        ),
    )

    fun forLanguage(lang: String): KeyboardLayout = when (lang) {
        "ru" -> Russian
        else -> English
    }
}
