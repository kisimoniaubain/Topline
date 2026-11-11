package com.kisimoni.topline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ToplineLightColors = lightColorScheme(
    primary = Color(0xFFF57F17),
    onPrimary = Color.White,
    background = Color(0xFFF7F7F7),
    surface = Color.White,
    onSurface = Color(0xFF0A0A0A),
    secondary = Color(0xFF0A0A0A),
    onSecondary = Color.White,
)

private val ToplineDarkColors = darkColorScheme(
    primary = Color(0xFFFF7A00),
    onPrimary = Color.Black,
    background = Color(0xFF121315),
    surface = Color(0xFF1F2022),
    onSurface = Color.White,
    secondary = Color(0xFFFFB77D),
    onSecondary = Color.Black,
    surfaceVariant = Color(0xFF343537),
    onSurfaceVariant = Color(0xFFE0C0AF),
    outline = Color(0xFFA78B7C),
    outlineVariant = Color(0xFF584235),
)

@Composable
fun ToplineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) ToplineDarkColors else ToplineLightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
