package com.zavtrak.keyboard.ime

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.zavtrak.keyboard.data.AppSettings
import com.zavtrak.keyboard.data.ThemeMode
import com.zavtrak.keyboard.data.ThemePreset

data class KeyboardPalette(
    val background: Int,
    val keyBg: Int,
    val keySpecialBg: Int,
    val keyPressedBg: Int,
    val keyText: Int,
    val keyHintText: Int,
    val accent: Int,
    val popupBg: Int,
    val suggestionStripBg: Int,
    val divider: Int,
    val borderColor: Int,
)

object ThemeMapper {

    fun resolve(context: Context, settings: AppSettings): KeyboardPalette {
        val night = isNight(context, settings.themeMode)
        val accent = settings.accentColor.toInt()
        val basePreset = preset(settings.preset, night, accent)
        val withOpacity = applyOpacity(basePreset, settings.backgroundOpacity)
        return withOpacity
    }

    private fun isNight(context: Context, mode: ThemeMode): Boolean = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Amoled -> true
        ThemeMode.System -> {
            val cfg = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            cfg == Configuration.UI_MODE_NIGHT_YES
        }
    }

    private fun preset(preset: ThemePreset, night: Boolean, accent: Int): KeyboardPalette {
        return when (preset) {
            ThemePreset.Default -> if (night) defaultDark(accent) else defaultLight(accent)
            ThemePreset.Neon -> neon(accent)
            ThemePreset.Glass -> if (night) glassDark(accent) else glassLight(accent)
            ThemePreset.Sunset -> sunset()
            ThemePreset.Mono -> if (night) monoDark() else monoLight()
            ThemePreset.Pastel -> pastel()
            ThemePreset.Forest -> forest()
            ThemePreset.Ocean -> ocean()
        }
    }

    private fun defaultLight(accent: Int) = KeyboardPalette(
        background = 0xFFF5F5FA.toInt(),
        keyBg = Color.WHITE,
        keySpecialBg = 0xFFE6E6EE.toInt(),
        keyPressedBg = 0xFFD8D8E2.toInt(),
        keyText = 0xFF14141B.toInt(),
        keyHintText = 0x80000000.toInt(),
        accent = accent,
        popupBg = Color.WHITE,
        suggestionStripBg = 0xFFEFEFF5.toInt(),
        divider = 0x14000000,
        borderColor = 0x14000000,
    )

    private fun defaultDark(accent: Int) = KeyboardPalette(
        background = 0xFF14141B.toInt(),
        keyBg = 0xFF26262F.toInt(),
        keySpecialBg = 0xFF1B1B22.toInt(),
        keyPressedBg = 0xFF3A3A45.toInt(),
        keyText = 0xFFEFEFF5.toInt(),
        keyHintText = 0x80FFFFFF.toInt(),
        accent = accent,
        popupBg = 0xFF2E2E38.toInt(),
        suggestionStripBg = 0xFF1A1A22.toInt(),
        divider = 0x18FFFFFF,
        borderColor = 0x22FFFFFF,
    )

    private fun neon(accent: Int) = KeyboardPalette(
        background = 0xFF0B0B12.toInt(),
        keyBg = 0xFF15151F.toInt(),
        keySpecialBg = 0xFF0F0F18.toInt(),
        keyPressedBg = ColorUtils.blendARGB(accent, Color.BLACK, 0.65f),
        keyText = ColorUtils.blendARGB(accent, Color.WHITE, 0.45f),
        keyHintText = ColorUtils.blendARGB(accent, Color.WHITE, 0.65f) and 0x80FFFFFF.toInt(),
        accent = accent,
        popupBg = 0xFF15151F.toInt(),
        suggestionStripBg = 0xFF0E0E18.toInt(),
        divider = ColorUtils.blendARGB(accent, Color.TRANSPARENT, 0.7f),
        borderColor = ColorUtils.blendARGB(accent, Color.TRANSPARENT, 0.4f),
    )

    private fun glassLight(accent: Int) = KeyboardPalette(
        background = 0xCCFFFFFF.toInt(),
        keyBg = 0x99FFFFFF.toInt(),
        keySpecialBg = 0x66FFFFFF.toInt(),
        keyPressedBg = 0xCCEEEEFF.toInt(),
        keyText = 0xFF14141B.toInt(),
        keyHintText = 0x80000000.toInt(),
        accent = accent,
        popupBg = 0xEEFFFFFF.toInt(),
        suggestionStripBg = 0x66FFFFFF.toInt(),
        divider = 0x22000000,
        borderColor = 0x33FFFFFF,
    )

    private fun glassDark(accent: Int) = KeyboardPalette(
        background = 0xCC101018.toInt(),
        keyBg = 0x9926262F.toInt(),
        keySpecialBg = 0x661B1B22.toInt(),
        keyPressedBg = 0xCC3A3A50.toInt(),
        keyText = 0xFFEFEFF5.toInt(),
        keyHintText = 0x80FFFFFF.toInt(),
        accent = accent,
        popupBg = 0xEE26262F.toInt(),
        suggestionStripBg = 0x66101018.toInt(),
        divider = 0x22FFFFFF,
        borderColor = 0x33FFFFFF,
    )

    private fun sunset() = KeyboardPalette(
        background = 0xFF2A1431.toInt(),
        keyBg = 0xFF3D2148.toInt(),
        keySpecialBg = 0xFF2F1538.toInt(),
        keyPressedBg = 0xFFFF7A5C.toInt(),
        keyText = 0xFFFFEFD5.toInt(),
        keyHintText = 0xAAFFEFD5.toInt(),
        accent = 0xFFFF7A5C.toInt(),
        popupBg = 0xFF3D2148.toInt(),
        suggestionStripBg = 0xFF221028.toInt(),
        divider = 0x33FFFFFF,
        borderColor = 0x33FFFFFF,
    )

    private fun monoLight() = KeyboardPalette(
        background = 0xFFF2F2F2.toInt(),
        keyBg = 0xFFFFFFFF.toInt(),
        keySpecialBg = 0xFFDADADA.toInt(),
        keyPressedBg = 0xFFC4C4C4.toInt(),
        keyText = 0xFF000000.toInt(),
        keyHintText = 0x66000000,
        accent = 0xFF000000.toInt(),
        popupBg = 0xFFFFFFFF.toInt(),
        suggestionStripBg = 0xFFEAEAEA.toInt(),
        divider = 0x14000000,
        borderColor = 0x33000000,
    )

    private fun monoDark() = KeyboardPalette(
        background = 0xFF000000.toInt(),
        keyBg = 0xFF1A1A1A.toInt(),
        keySpecialBg = 0xFF0E0E0E.toInt(),
        keyPressedBg = 0xFF2C2C2C.toInt(),
        keyText = 0xFFFFFFFF.toInt(),
        keyHintText = 0x66FFFFFF,
        accent = 0xFFFFFFFF.toInt(),
        popupBg = 0xFF1A1A1A.toInt(),
        suggestionStripBg = 0xFF0A0A0A.toInt(),
        divider = 0x22FFFFFF,
        borderColor = 0x33FFFFFF,
    )

    private fun pastel() = KeyboardPalette(
        background = 0xFFFCEFEF.toInt(),
        keyBg = 0xFFFFFFFF.toInt(),
        keySpecialBg = 0xFFFFE2E2.toInt(),
        keyPressedBg = 0xFFFFC4C4.toInt(),
        keyText = 0xFF4A2C2C.toInt(),
        keyHintText = 0x884A2C2C.toInt(),
        accent = 0xFFFF8FA3.toInt(),
        popupBg = 0xFFFFFFFF.toInt(),
        suggestionStripBg = 0xFFFFF5F5.toInt(),
        divider = 0x22000000,
        borderColor = 0x33000000,
    )

    private fun forest() = KeyboardPalette(
        background = 0xFF0F1F12.toInt(),
        keyBg = 0xFF1B3120.toInt(),
        keySpecialBg = 0xFF142519.toInt(),
        keyPressedBg = 0xFF2A4D31.toInt(),
        keyText = 0xFFE7F4EB.toInt(),
        keyHintText = 0xAAB8D6BE.toInt(),
        accent = 0xFF6FCF97.toInt(),
        popupBg = 0xFF1B3120.toInt(),
        suggestionStripBg = 0xFF0A1A0E.toInt(),
        divider = 0x22FFFFFF,
        borderColor = 0x22FFFFFF,
    )

    private fun ocean() = KeyboardPalette(
        background = 0xFF0A1B2A.toInt(),
        keyBg = 0xFF14304D.toInt(),
        keySpecialBg = 0xFF0F2438.toInt(),
        keyPressedBg = 0xFF2470B3.toInt(),
        keyText = 0xFFE3F1FF.toInt(),
        keyHintText = 0xAAB7DAFF.toInt(),
        accent = 0xFF40A6F0.toInt(),
        popupBg = 0xFF14304D.toInt(),
        suggestionStripBg = 0xFF071623.toInt(),
        divider = 0x22FFFFFF,
        borderColor = 0x22FFFFFF,
    )

    private fun applyOpacity(p: KeyboardPalette, opacity: Float): KeyboardPalette {
        if (opacity >= 0.99f) return p
        fun adj(c: Int): Int {
            val a = (Color.alpha(c) * opacity).toInt().coerceIn(0, 255)
            return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
        }
        return p.copy(background = adj(p.background))
    }
}
