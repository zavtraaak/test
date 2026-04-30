package com.zavtrak.keyboard.data

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { System, Light, Dark, Amoled }

@Serializable
enum class ThemePreset {
    Default, Neon, Glass, Sunset, Mono, Pastel, Forest, Ocean
}

@Serializable
enum class OneHandedMode { Off, Left, Right }

@Serializable
enum class HapticStrength { Off, Light, Medium, Strong }

@Serializable
data class AppSettings(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.System,
    val preset: ThemePreset = ThemePreset.Default,
    val materialYou: Boolean = true,
    val accentColor: Long = 0xFF7C5CFFL,
    val backgroundOpacity: Float = 1.0f,
    val keyCornerRadius: Float = 12f,    // dp
    val keyHeight: Float = 56f,          // dp
    val fontSize: Float = 18f,           // sp
    val showKeyBorders: Boolean = false,

    // Layout
    val enableEnglish: Boolean = true,
    val enableRussian: Boolean = true,
    val showNumberRow: Boolean = false,
    val showArrowsRow: Boolean = false,
    val longPressNumbers: Boolean = true,

    // Typing
    val autoCapitalize: Boolean = true,
    val smartPunctuation: Boolean = true,
    val autocorrect: Boolean = true,
    val showSuggestions: Boolean = true,
    val learnWords: Boolean = true,
    val incognitoMode: Boolean = false,
    val swipeSpaceCursor: Boolean = true,
    val swipeBackspaceWord: Boolean = true,

    // Feedback
    val haptic: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.Light,
    val sound: Boolean = false,
    val soundVolume: Float = 0.4f,

    // Ergonomics
    val oneHanded: OneHandedMode = OneHandedMode.Off,
    val splitKeyboard: Boolean = false,
    val floatingKeyboard: Boolean = false,

    // Internal
    val activeLanguage: String = "en", // "en" or "ru"
    val schemaVersion: Int = 1,
)
