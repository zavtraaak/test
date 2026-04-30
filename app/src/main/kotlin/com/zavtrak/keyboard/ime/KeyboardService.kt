package com.zavtrak.keyboard.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.zavtrak.keyboard.MainActivity
import com.zavtrak.keyboard.data.AppSettings
import com.zavtrak.keyboard.data.HapticStrength
import com.zavtrak.keyboard.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Top-level Input Method service.
 *
 * Composition of views (vertical):
 *   [SuggestionStripView]
 *   [KeyboardView] / [EmojiPanelView] / [ClipboardPanelView]   (only one at a time)
 *
 * State:
 *   - [settings]   live snapshot from DataStore
 *   - [language]   "en" or "ru"
 *   - [shifted] / [capsLock]
 *   - [composing]  running prefix the user is typing (for suggestions)
 */
class KeyboardService : InputMethodService(), KeyboardView.Listener,
    SuggestionStripView.Listener, EmojiPanelView.Listener, ClipboardPanelView.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repo: SettingsRepository
    private var settings: AppSettings = AppSettings()

    private var rootView: LinearLayout? = null
    private var suggestionStrip: SuggestionStripView? = null
    private var keyboardView: KeyboardView? = null
    private var emojiPanel: EmojiPanelView? = null
    private var clipboardPanel: ClipboardPanelView? = null
    private var currentMode: Mode = Mode.Letters

    private var language: String = "en"
    private var shifted: Boolean = false
    private var capsLock: Boolean = false
    private var lastShiftTap: Long = 0L
    private var symbolMode: KeyboardView.SymbolMode = KeyboardView.SymbolMode.None

    private val composingBuf = StringBuilder()
    private var dictionary: Dictionary = Dictionary.forLanguage("en")

    private enum class Mode { Letters, Symbols1, Symbols2, Emoji, Clipboard }

    override fun onCreate() {
        super.onCreate()
        repo = SettingsRepository.get(applicationContext)
        // Load synchronously so first onCreateInputView already has settings
        settings = runBlocking { repo.flow.first() }
        language = settings.activeLanguage
        dictionary = Dictionary.forLanguage(language)
        repo.flow.onEach { s ->
            settings = s
            if (s.activeLanguage != language) switchLanguage(s.activeLanguage)
            applyConfigToView()
        }.launchIn(scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onCreateInputView(): View {
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = false
        }
        val strip = SuggestionStripView(ctx).also { it.listener = this }
        val kb = KeyboardView(ctx).also { it.listener = this }
        val emoji = EmojiPanelView(ctx).also { it.listener = this }
        val clip = ClipboardPanelView(ctx).also { it.listener = this }
        root.addView(strip)
        root.addView(kb)
        rootView = root
        suggestionStrip = strip
        keyboardView = kb
        emojiPanel = emoji
        clipboardPanel = clip
        applyConfigToView()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composingBuf.setLength(0)
        capsLock = false
        shifted = settings.autoCapitalize && shouldAutoCap(info)
        currentMode = Mode.Letters
        symbolMode = KeyboardView.SymbolMode.None
        applyConfigToView()
        captureClipboard()
        refreshSuggestions()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // If selection moved by something other than us, reset composing.
        if (composingBuf.isNotEmpty() && (newSelStart != oldSelStart + 1 && newSelEnd != oldSelEnd + 1)) {
            composingBuf.setLength(0)
            refreshSuggestions()
        }
        if (settings.autoCapitalize) {
            val ic = currentInputConnection ?: return
            val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            val needsCap = before.isEmpty() || before.endsWith(". ") || before.endsWith("? ") || before.endsWith("! ") || before == "\n" || before.endsWith("\n")
            if (needsCap != shifted && !capsLock) {
                shifted = needsCap
                keyboardView?.shifted = shifted
            }
        }
    }

    private fun applyConfigToView() {
        val palette = ThemeMapper.resolve(this, settings)
        val kb = keyboardView ?: return
        val strip = suggestionStrip
        kb.applyPalette(palette)
        strip?.setPalette(palette)
        emojiPanel?.setPalette(palette, settings.incognitoMode)
        clipboardPanel?.setPalette(palette, settings.incognitoMode)
        val layout = when (symbolMode) {
            KeyboardView.SymbolMode.Symbols1 -> Layouts.Symbols1
            KeyboardView.SymbolMode.Symbols2 -> Layouts.Symbols2
            else -> Layouts.forLanguage(language)
        }
        kb.configure(layout, settings, palette, symbolMode)
        kb.shifted = shifted
        kb.capsLock = capsLock
        kb.invalidate()
        showMode(currentMode)
    }

    private fun showMode(m: Mode) {
        val root = rootView ?: return
        currentMode = m
        // Detach all body views, attach the relevant one
        root.removeAllViews()
        suggestionStrip?.let {
            // Suggestion strip is hidden in emoji/clipboard panels for clarity
            if (m == Mode.Letters || m == Mode.Symbols1 || m == Mode.Symbols2) {
                if (settings.showSuggestions) root.addView(it)
            }
        }
        when (m) {
            Mode.Letters, Mode.Symbols1, Mode.Symbols2 -> keyboardView?.let { root.addView(it) }
            Mode.Emoji -> emojiPanel?.let { root.addView(it) }
            Mode.Clipboard -> clipboardPanel?.let { root.addView(it) }
        }
    }

    private fun shouldAutoCap(info: EditorInfo?): Boolean {
        info ?: return false
        return (info.inputType and EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0
    }

    // ─── KeyboardView.Listener ────────────────────────────────────────────

    override fun onKey(key: Key) {
        feedback()
        val ic = currentInputConnection ?: return
        when (key.code) {
            KeyCodes.SHIFT -> handleShift()
            KeyCodes.DELETE -> handleDelete(ic)
            KeyCodes.ENTER -> handleEnter(ic)
            KeyCodes.MODE_LETTERS -> { symbolMode = KeyboardView.SymbolMode.None; currentMode = Mode.Letters; applyConfigToView() }
            KeyCodes.MODE_SYMBOLS_1 -> { symbolMode = KeyboardView.SymbolMode.Symbols1; currentMode = Mode.Symbols1; applyConfigToView() }
            KeyCodes.MODE_SYMBOLS_2 -> { symbolMode = KeyboardView.SymbolMode.Symbols2; currentMode = Mode.Symbols2; applyConfigToView() }
            KeyCodes.LANGUAGE -> toggleLanguage()
            KeyCodes.EMOJI -> { currentMode = Mode.Emoji; applyConfigToView() }
            KeyCodes.SETTINGS -> openSettingsActivity()
            KeyCodes.CLIPBOARD -> { currentMode = Mode.Clipboard; applyConfigToView() }
            KeyCodes.SPACE -> handleSpace(ic)
            KeyCodes.COMMA -> commit(ic, ",")
            KeyCodes.PERIOD -> commit(ic, ".")
            KeyCodes.CURSOR_LEFT -> moveCursor(ic, -1)
            KeyCodes.CURSOR_RIGHT -> moveCursor(ic, 1)
            KeyCodes.CURSOR_UP -> sendKey(KeyEvent.KEYCODE_DPAD_UP)
            KeyCodes.CURSOR_DOWN -> sendKey(KeyEvent.KEYCODE_DPAD_DOWN)
            KeyCodes.NUMBER_TOGGLE -> { /* TODO */ }
            KeyCodes.TAB -> commit(ic, "\t")
            else -> {
                val ch = if (key.label.length == 1) {
                    val c = key.label[0]
                    if ((capsLock || shifted) && c.isLetter()) c.uppercase() else c.toString()
                } else key.label
                commit(ic, ch)
                if (shifted && !capsLock) {
                    shifted = false
                    keyboardView?.shifted = false
                }
            }
        }
    }

    override fun onLongPressKey(key: Key, alt: String) {
        feedback()
        val ic = currentInputConnection ?: return
        commit(ic, alt)
        if (shifted && !capsLock) {
            shifted = false; keyboardView?.shifted = false
        }
    }

    override fun onSpaceSwipe(deltaX: Int) {
        moveCursor(currentInputConnection ?: return, deltaX)
    }

    override fun onBackspaceWord() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        if (before.isEmpty()) return
        // Find the start of the previous word/whitespace block
        val trimmed = before.trimEnd()
        val lastSpace = trimmed.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '.', ',', ';', '!', '?'))
        val cut = if (lastSpace < 0) before.length else (before.length - lastSpace - 1)
        ic.deleteSurroundingText(cut.coerceAtLeast(1), 0)
        composingBuf.setLength(0)
        refreshSuggestions()
    }

    override fun onShiftLongPress() {
        capsLock = !capsLock
        shifted = capsLock
        keyboardView?.shifted = shifted
        keyboardView?.capsLock = capsLock
    }

    // ─── Suggestion strip ─────────────────────────────────────────────────

    override fun onSuggestionPicked(text: String) {
        val ic = currentInputConnection ?: return
        val cur = composingBuf.toString()
        if (cur.isEmpty()) {
            commit(ic, "$text ")
        } else {
            ic.deleteSurroundingText(cur.length, 0)
            commit(ic, "$text ")
            composingBuf.setLength(0)
            if (settings.learnWords && !settings.incognitoMode) dictionary.observeWord(text)
        }
        refreshSuggestions()
    }

    // ─── Emoji panel ──────────────────────────────────────────────────────

    override fun onEmojiPicked(emoji: String) {
        feedback()
        val ic = currentInputConnection ?: return
        commit(ic, emoji)
    }

    override fun onClose() {
        currentMode = Mode.Letters
        applyConfigToView()
    }

    override fun onBackspace() {
        feedback()
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)
    }

    override fun onClipboardEntryPicked(text: String) {
        feedback()
        val ic = currentInputConnection ?: return
        commit(ic, text)
        currentMode = Mode.Letters
        applyConfigToView()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun handleShift() {
        val now = SystemClock.uptimeMillis()
        if (now - lastShiftTap < 300) {
            // Double tap → caps lock
            capsLock = !capsLock
            shifted = capsLock
        } else {
            if (capsLock) { capsLock = false; shifted = false }
            else shifted = !shifted
        }
        lastShiftTap = now
        keyboardView?.shifted = shifted
        keyboardView?.capsLock = capsLock
    }

    private fun handleDelete(ic: InputConnection) {
        if (composingBuf.isNotEmpty()) {
            composingBuf.deleteCharAt(composingBuf.length - 1)
        }
        // If text is selected, delete selection; otherwise delete one char.
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) ic.commitText("", 1) else ic.deleteSurroundingText(1, 0)
        refreshSuggestions()
    }

    private fun handleEnter(ic: InputConnection) {
        // If editor has an action like SEND/DONE, fire it; otherwise insert newline.
        val ei = currentInputEditorInfo
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_UNSPECIFIED
        if (action != 0 && action != EditorInfo.IME_ACTION_NONE && (ei?.imeOptions?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0)) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
        composingBuf.setLength(0)
        refreshSuggestions()
    }

    private fun handleSpace(ic: InputConnection) {
        // Smart punctuation: if prev char is a space and one before is letter — convert to ". "
        if (settings.smartPunctuation) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            if (before.length == 2 && before[1] == ' ' && (before[0].isLetter() || before[0].isDigit())) {
                ic.deleteSurroundingText(1, 0)
                commit(ic, ". ")
                composingBuf.setLength(0)
                refreshSuggestions()
                return
            }
        }
        // Autocorrect on space
        if (settings.autocorrect && composingBuf.isNotEmpty()) {
            val candidate = dictionary.autocorrect(composingBuf.toString())
            if (candidate != null) {
                ic.deleteSurroundingText(composingBuf.length, 0)
                commit(ic, "$candidate ")
                composingBuf.setLength(0)
                refreshSuggestions()
                return
            }
        }
        if (settings.learnWords && !settings.incognitoMode && composingBuf.length >= 3) {
            dictionary.observeWord(composingBuf.toString().lowercase())
        }
        commit(ic, " ")
        composingBuf.setLength(0)
        refreshSuggestions()
    }

    private fun commit(ic: InputConnection, text: String) {
        ic.commitText(text, 1)
        if (text.length == 1 && text[0].isLetter()) {
            composingBuf.append(text[0].lowercaseChar())
            refreshSuggestions()
        } else if (text.length == 1 && (text[0] == ' ' || text[0] == '\n')) {
            composingBuf.setLength(0)
            refreshSuggestions()
        } else if (text.length == 1) {
            composingBuf.setLength(0)
            refreshSuggestions()
        }
    }

    private fun refreshSuggestions() {
        val strip = suggestionStrip ?: return
        if (!settings.showSuggestions) {
            strip.setSuggestions(emptyList())
            return
        }
        val s = composingBuf.toString()
        val list = if (s.isEmpty()) emptyList() else dictionary.suggest(s, n = 3)
        strip.setSuggestions(list)
    }

    private fun moveCursor(ic: InputConnection, delta: Int) {
        val before = ic.getTextBeforeCursor(Int.MAX_VALUE / 2, 0)?.length ?: 0
        val newPos = (before + delta).coerceAtLeast(0)
        ic.setSelection(newPos, newPos)
    }

    private fun sendKey(code: Int) {
        sendDownUpKeyEvents(code)
    }

    private fun toggleLanguage() {
        val next = if (language == "en") "ru" else "en"
        switchLanguage(next)
        scope.launch { repo.update { it.copy(activeLanguage = next) } }
    }

    private fun switchLanguage(lang: String) {
        language = lang
        dictionary = Dictionary.forLanguage(lang)
        composingBuf.setLength(0)
        applyConfigToView()
        refreshSuggestions()
    }

    private fun captureClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = cm.primaryClip ?: return
        for (i in 0 until clip.itemCount) {
            val text = clip.getItemAt(i).coerceToText(this)?.toString().orEmpty()
            if (text.isNotBlank()) clipboardPanel?.addEntry(text)
        }
    }

    private fun openSettingsActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun feedback() {
        if (settings.haptic) {
            val view = keyboardView ?: return
            val const = when (settings.hapticStrength) {
                HapticStrength.Off -> -1
                HapticStrength.Light -> HapticFeedbackConstants.KEYBOARD_TAP
                HapticStrength.Medium -> HapticFeedbackConstants.KEYBOARD_RELEASE
                HapticStrength.Strong -> HapticFeedbackConstants.LONG_PRESS
            }
            if (const >= 0) view.performHapticFeedback(const, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        }
        if (settings.sound) {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, settings.soundVolume.coerceIn(0f, 1f))
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false
}
