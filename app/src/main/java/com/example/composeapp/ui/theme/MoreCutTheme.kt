package com.example.composeapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// Studio Grade Obsidian & Electric Cyan Palette (CapCut aesthetic)
val StudioBackground = Color(0xFF090A0E)
val StudioSurface = Color(0xFF13151D)
val StudioSurfaceVariant = Color(0xFF1B1E29)
val StudioSurfaceHigh = Color(0xFF242837)
val StudioBorder = Color(0xFF282D3D)
val StudioBorderSubtle = Color(0xFF1E2230)

val StudioCyan = Color(0xFF00E5FF)
val StudioPink = Color(0xFFFF2A55)
val StudioGreen = Color(0xFF00E676)
val StudioAmber = Color(0xFFFFB300)

private val MoreCutDarkColors = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color(0xFF090A0E),
    primaryContainer = Color(0xFF112635),
    onPrimaryContainer = StudioCyan,
    secondary = StudioPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF38121D),
    onSecondaryContainer = StudioPink,
    tertiary = Color(0xFF9E86FF),
    background = StudioBackground,
    onBackground = Color(0xFFF1F3F9),
    surface = StudioSurface,
    onSurface = Color(0xFFF1F3F9),
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = Color(0xFFA1A8BA),
    outline = StudioBorder,
    outlineVariant = StudioBorderSubtle,
)

private val MoreCutLightColors = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = StudioPink,
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF12141B),
    surface = Color.White,
    onSurface = Color(0xFF12141B),
    surfaceVariant = Color(0xFFEAECEF),
    onSurfaceVariant = Color(0xFF5A6072),
    outline = Color(0xFFD0D5DD),
)

val MoreCutTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.2.sp,
    ),
)

val MoreCutShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MoreCutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) MoreCutDarkColors else MoreCutLightColors,
        typography = MoreCutTypography,
        shapes = MoreCutShapes,
        content = content,
    )
}
