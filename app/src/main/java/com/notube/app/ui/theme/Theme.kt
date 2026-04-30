package com.notube.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// YouTube-like palette: red accent on white/black backgrounds.
val YouTubeRed = Color(0xFFFF0000)
val YouTubeRedDark = Color(0xFFCC0000)

private val LightColors = lightColorScheme(
    primary = YouTubeRed,
    onPrimary = Color.White,
    secondary = YouTubeRedDark,
    onSecondary = Color.White,
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF0F0F0F),
    surface = Color.White,
    onSurface = Color(0xFF0F0F0F),
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF606060),
)

private val DarkColors = darkColorScheme(
    primary = YouTubeRed,
    onPrimary = Color.White,
    secondary = YouTubeRed,
    onSecondary = Color.White,
    background = Color(0xFF0F0F0F),
    onBackground = Color.White,
    surface = Color(0xFF0F0F0F),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF272727),
    onSurfaceVariant = Color(0xFFAAAAAA),
)

@Composable
fun NoTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NoTubeTypography,
        content = content,
    )
}
