package com.example.composeapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MoreCutDarkColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFBDBDBD),
    background = Color(0xFF080808),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFFB8B8B8),
    outline = Color(0xFF303030),
)

private val MoreCutLightColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color.White,
    background = Color(0xFFF6F6F6),
    surface = Color.White,
)

@Composable
fun MoreCutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) MoreCutDarkColors else MoreCutLightColors,
        content = content,
    )
}
