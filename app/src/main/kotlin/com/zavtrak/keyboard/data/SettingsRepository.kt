package com.zavtrak.keyboard.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "zavtra_keyboard_settings")

object Keys {
    val themeMode = stringPreferencesKey("themeMode")
    val preset = stringPreferencesKey("preset")
    val materialYou = booleanPreferencesKey("materialYou")
    val accentColor = longPreferencesKey("accentColor")
    val backgroundOpacity = floatPreferencesKey("backgroundOpacity")
    val keyCornerRadius = floatPreferencesKey("keyCornerRadius")
    val keyHeight = floatPreferencesKey("keyHeight")
    val fontSize = floatPreferencesKey("fontSize")
    val showKeyBorders = booleanPreferencesKey("showKeyBorders")

    val enableEnglish = booleanPreferencesKey("enableEnglish")
    val enableRussian = booleanPreferencesKey("enableRussian")
    val showNumberRow = booleanPreferencesKey("showNumberRow")
    val showArrowsRow = booleanPreferencesKey("showArrowsRow")
    val longPressNumbers = booleanPreferencesKey("longPressNumbers")

    val autoCapitalize = booleanPreferencesKey("autoCapitalize")
    val smartPunctuation = booleanPreferencesKey("smartPunctuation")
    val autocorrect = booleanPreferencesKey("autocorrect")
    val showSuggestions = booleanPreferencesKey("showSuggestions")
    val learnWords = booleanPreferencesKey("learnWords")
    val incognitoMode = booleanPreferencesKey("incognitoMode")
    val swipeSpaceCursor = booleanPreferencesKey("swipeSpaceCursor")
    val swipeBackspaceWord = booleanPreferencesKey("swipeBackspaceWord")

    val haptic = booleanPreferencesKey("haptic")
    val hapticStrength = stringPreferencesKey("hapticStrength")
    val sound = booleanPreferencesKey("sound")
    val soundVolume = floatPreferencesKey("soundVolume")

    val oneHanded = stringPreferencesKey("oneHanded")
    val splitKeyboard = booleanPreferencesKey("splitKeyboard")
    val floatingKeyboard = booleanPreferencesKey("floatingKeyboard")
    val activeLanguage = stringPreferencesKey("activeLanguage")
    val schemaVersion = intPreferencesKey("schemaVersion")
}

class SettingsRepository(private val context: Context) {

    val flow: Flow<AppSettings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toSettings()
            val updated = transform(current)
            prefs.applySettings(updated)
        }
    }

    suspend fun replace(settings: AppSettings) = update { settings }

    suspend fun reset() = replace(AppSettings())

    fun exportJson(settings: AppSettings): String =
        Json { prettyPrint = true; encodeDefaults = true }.encodeToString(AppSettings.serializer(), settings)

    fun importJson(json: String): AppSettings? =
        runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString(AppSettings.serializer(), json)
        }.getOrNull()

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = (this[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }) ?: d.themeMode,
            preset = (this[Keys.preset]?.let { runCatching { ThemePreset.valueOf(it) }.getOrNull() }) ?: d.preset,
            materialYou = this[Keys.materialYou] ?: d.materialYou,
            accentColor = this[Keys.accentColor] ?: d.accentColor,
            backgroundOpacity = this[Keys.backgroundOpacity] ?: d.backgroundOpacity,
            keyCornerRadius = this[Keys.keyCornerRadius] ?: d.keyCornerRadius,
            keyHeight = this[Keys.keyHeight] ?: d.keyHeight,
            fontSize = this[Keys.fontSize] ?: d.fontSize,
            showKeyBorders = this[Keys.showKeyBorders] ?: d.showKeyBorders,
            enableEnglish = this[Keys.enableEnglish] ?: d.enableEnglish,
            enableRussian = this[Keys.enableRussian] ?: d.enableRussian,
            showNumberRow = this[Keys.showNumberRow] ?: d.showNumberRow,
            showArrowsRow = this[Keys.showArrowsRow] ?: d.showArrowsRow,
            longPressNumbers = this[Keys.longPressNumbers] ?: d.longPressNumbers,
            autoCapitalize = this[Keys.autoCapitalize] ?: d.autoCapitalize,
            smartPunctuation = this[Keys.smartPunctuation] ?: d.smartPunctuation,
            autocorrect = this[Keys.autocorrect] ?: d.autocorrect,
            showSuggestions = this[Keys.showSuggestions] ?: d.showSuggestions,
            learnWords = this[Keys.learnWords] ?: d.learnWords,
            incognitoMode = this[Keys.incognitoMode] ?: d.incognitoMode,
            swipeSpaceCursor = this[Keys.swipeSpaceCursor] ?: d.swipeSpaceCursor,
            swipeBackspaceWord = this[Keys.swipeBackspaceWord] ?: d.swipeBackspaceWord,
            haptic = this[Keys.haptic] ?: d.haptic,
            hapticStrength = (this[Keys.hapticStrength]?.let { runCatching { HapticStrength.valueOf(it) }.getOrNull() }) ?: d.hapticStrength,
            sound = this[Keys.sound] ?: d.sound,
            soundVolume = this[Keys.soundVolume] ?: d.soundVolume,
            oneHanded = (this[Keys.oneHanded]?.let { runCatching { OneHandedMode.valueOf(it) }.getOrNull() }) ?: d.oneHanded,
            splitKeyboard = this[Keys.splitKeyboard] ?: d.splitKeyboard,
            floatingKeyboard = this[Keys.floatingKeyboard] ?: d.floatingKeyboard,
            activeLanguage = this[Keys.activeLanguage] ?: d.activeLanguage,
            schemaVersion = this[Keys.schemaVersion] ?: d.schemaVersion,
        )
    }

    private fun MutablePreferences.applySettings(s: AppSettings) {
        this[Keys.themeMode] = s.themeMode.name
        this[Keys.preset] = s.preset.name
        this[Keys.materialYou] = s.materialYou
        this[Keys.accentColor] = s.accentColor
        this[Keys.backgroundOpacity] = s.backgroundOpacity
        this[Keys.keyCornerRadius] = s.keyCornerRadius
        this[Keys.keyHeight] = s.keyHeight
        this[Keys.fontSize] = s.fontSize
        this[Keys.showKeyBorders] = s.showKeyBorders
        this[Keys.enableEnglish] = s.enableEnglish
        this[Keys.enableRussian] = s.enableRussian
        this[Keys.showNumberRow] = s.showNumberRow
        this[Keys.showArrowsRow] = s.showArrowsRow
        this[Keys.longPressNumbers] = s.longPressNumbers
        this[Keys.autoCapitalize] = s.autoCapitalize
        this[Keys.smartPunctuation] = s.smartPunctuation
        this[Keys.autocorrect] = s.autocorrect
        this[Keys.showSuggestions] = s.showSuggestions
        this[Keys.learnWords] = s.learnWords
        this[Keys.incognitoMode] = s.incognitoMode
        this[Keys.swipeSpaceCursor] = s.swipeSpaceCursor
        this[Keys.swipeBackspaceWord] = s.swipeBackspaceWord
        this[Keys.haptic] = s.haptic
        this[Keys.hapticStrength] = s.hapticStrength.name
        this[Keys.sound] = s.sound
        this[Keys.soundVolume] = s.soundVolume
        this[Keys.oneHanded] = s.oneHanded.name
        this[Keys.splitKeyboard] = s.splitKeyboard
        this[Keys.floatingKeyboard] = s.floatingKeyboard
        this[Keys.activeLanguage] = s.activeLanguage
        this[Keys.schemaVersion] = s.schemaVersion
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
