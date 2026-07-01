package com.animejapaneselab.nativeapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object LabPalette {
    val Green = Color(0xFF58CC02)
    val GreenDark = Color(0xFF3F9E00)
    val Blue = Color(0xFF1CB0F6)
    val Yellow = Color(0xFFFFC800)
    val Coral = Color(0xFFFF6B6B)
    val Ink = Color(0xFF172033)
    val Muted = Color(0xFF667085)
    val Panel = Color(0xFFF7FAF4)
    val BluePanel = Color(0xFFEAF7FF)
}

val AppSpacing = 16.dp

private val LightColors = lightColorScheme(
    primary = LabPalette.Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4FFD4),
    onPrimaryContainer = Color(0xFF164800),
    secondary = LabPalette.Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF3FF),
    onSecondaryContainer = Color(0xFF06344F),
    tertiary = LabPalette.Yellow,
    onTertiary = LabPalette.Ink,
    background = Color(0xFFFBFCF8),
    onBackground = LabPalette.Ink,
    surface = Color.White,
    onSurface = LabPalette.Ink,
    surfaceVariant = Color(0xFFE9F1E3),
    onSurfaceVariant = Color(0xFF3F4A3A),
    outline = Color(0xFFD8E3D1),
    error = LabPalette.Coral,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FEA29),
    onPrimary = Color(0xFF0E2800),
    secondary = Color(0xFF64C8FF),
    onSecondary = Color(0xFF052236),
    tertiary = Color(0xFFFFD64D),
    onTertiary = Color(0xFF241A00),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE7EFE1),
    surface = Color(0xFF172017),
    onSurface = Color(0xFFE7EFE1),
    surfaceVariant = Color(0xFF263322),
    onSurfaceVariant = Color(0xFFC9D7C0),
    outline = Color(0xFF465842),
    error = Color(0xFFFF8A8A),
)

@Composable
fun AnimeJapaneseLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    @Suppress("UnusedVariable")
    val activity = context as? Activity

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}
