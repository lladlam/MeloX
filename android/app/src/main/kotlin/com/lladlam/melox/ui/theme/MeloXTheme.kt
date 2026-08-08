package com.lladlam.melox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFE5484D),
    background = Color(0xFFF7F7FA),
    surface = Color(0xFFFDFDFE),
    onPrimary = Color.White,
    onBackground = Color(0xFF17171A),
    onSurface = Color(0xFF17171A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6369),
    background = Color(0xFF0B0B0D),
    surface = Color(0xFF151518),
    onPrimary = Color.White,
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
)

@Composable
fun MeloXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
