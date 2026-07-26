package com.animejapaneselab.nativeapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.R

object LabPalette {
    val Green = Color(0xFF2E9862)
    val GreenDark = Color(0xFF1F7449)
    val GreenSoft = Color(0xFFDFF3E7)
    val Blue = Color(0xFF4C6FA8)
    val BlueSoft = Color(0xFFE8EEF7)
    val Yellow = Color(0xFFB78216)
    val YellowSoft = Color(0xFFFAF0DA)
    val Coral = Color(0xFFB5473F)
    val CoralSoft = Color(0xFFFBE5E3)
    val Sakura = Color(0xFF9A5E79)
    val SakuraDark = Color(0xFF7A435D)
    val SakuraSoft = Color(0xFFF7EDF1)
    val Violet = Color(0xFF6558E8)
    val VioletDark = Color(0xFF4C40BE)
    val VioletBright = Color(0xFF8B7BFF)
    val Orange = Color(0xFFC26A24)
    val OrangeSoft = Color(0xFFFBEDDE)
    val Gold = Color(0xFFD99A1B)
    val Ink = Color(0xFF241F2D)
    val Muted = Color(0xFF6C6676)
    val Paper = Color(0xFFFBF9F6)
    val Panel = Color(0xFFFFFFFF)
    val BluePanel = Color(0xFFF1F4F8)
    val VioletPanel = Color(0xFFF3F0FF)
    val Outline = Color(0xFFE4DFE9)
}

object LabSpacing {
    val XXSmall = 4.dp
    val XSmall = 8.dp
    val Small = 12.dp
    val Medium = 16.dp
    val Large = 20.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
    val Screen = 20.dp
}

val AppSpacing = LabSpacing.Medium

/**
 * Semantic colors that Material's scheme has no slot for: learning-feedback
 * states, streak/XP accents and the branded hero gradient. Resolved per
 * light/dark theme and read through [LabTheme.colors].
 */
@Immutable
data class LabExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val streak: Color,
    val streakContainer: Color,
    val xp: Color,
    val xpContainer: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val onHero: Color,
)

private val LightExtendedColors = LabExtendedColors(
    success = LabPalette.Green,
    onSuccess = Color.White,
    successContainer = LabPalette.GreenSoft,
    onSuccessContainer = Color(0xFF14512F),
    warning = Color(0xFFA36E0A),
    onWarning = Color.White,
    warningContainer = LabPalette.YellowSoft,
    onWarningContainer = Color(0xFF6B4A00),
    info = LabPalette.Blue,
    onInfo = Color.White,
    infoContainer = LabPalette.BlueSoft,
    onInfoContainer = Color(0xFF2C4368),
    streak = Color(0xFFE8862C),
    streakContainer = Color(0xFFFCEEDF),
    xp = LabPalette.Gold,
    xpContainer = Color(0xFFFBF1DC),
    heroGradientStart = Color(0xFF6558E8),
    heroGradientEnd = Color(0xFF8B72F5),
    onHero = Color.White,
)

private val DarkExtendedColors = LabExtendedColors(
    success = Color(0xFF7DD3A4),
    onSuccess = Color(0xFF0C2E1B),
    successContainer = Color(0xFF1D4A31),
    onSuccessContainer = Color(0xFFD5F3E1),
    warning = Color(0xFFEDC06C),
    onWarning = Color(0xFF3E2C00),
    warningContainer = Color(0xFF54400D),
    onWarningContainer = Color(0xFFFAE8C6),
    info = Color(0xFF9DB8E4),
    onInfo = Color(0xFF15263F),
    infoContainer = Color(0xFF2C3F5D),
    onInfoContainer = Color(0xFFDBE6F8),
    streak = Color(0xFFF2A45B),
    streakContainer = Color(0xFF4C3115),
    xp = Color(0xFFEFC161),
    xpContainer = Color(0xFF4A3A12),
    heroGradientStart = Color(0xFF4B3ECD),
    heroGradientEnd = Color(0xFF7A66F0),
    onHero = Color(0xFFF3EFFF),
)

val LocalLabExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object LabTheme {
    val colors: LabExtendedColors
        @Composable get() = LocalLabExtendedColors.current

    /** Branded diagonal gradient for hero cards and celebratory surfaces. */
    @Composable
    fun heroBrush(): Brush {
        val extended = LocalLabExtendedColors.current
        return Brush.linearGradient(
            colors = listOf(extended.heroGradientStart, extended.heroGradientEnd),
        )
    }
}

private val LightColors = lightColorScheme(
    primary = LabPalette.Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E4FF),
    onPrimaryContainer = Color(0xFF352A93),
    inversePrimary = Color(0xFFC5BBFF),
    secondary = LabPalette.VioletDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE8FA),
    onSecondaryContainer = Color(0xFF2E2766),
    tertiary = LabPalette.Green,
    onTertiary = Color.White,
    tertiaryContainer = LabPalette.GreenSoft,
    onTertiaryContainer = Color(0xFF14512F),
    background = LabPalette.Paper,
    onBackground = LabPalette.Ink,
    surface = Color.White,
    onSurface = LabPalette.Ink,
    surfaceVariant = Color(0xFFF4F1F5),
    onSurfaceVariant = LabPalette.Muted,
    surfaceTint = LabPalette.Violet,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDEDAE1),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F4F6),
    surfaceContainer = Color(0xFFF2EEF3),
    surfaceContainerHigh = Color(0xFFECE8EF),
    surfaceContainerHighest = Color(0xFFE7E2EA),
    inverseSurface = Color(0xFF2B2634),
    inverseOnSurface = Color(0xFFF4EFF6),
    outline = LabPalette.Outline,
    outlineVariant = Color(0xFFEFEBF2),
    error = LabPalette.Coral,
    onError = Color.White,
    errorContainer = LabPalette.CoralSoft,
    onErrorContainer = Color(0xFF7A2A24),
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC2B8FF),
    onPrimary = Color(0xFF2B1F70),
    primaryContainer = Color(0xFF453A9E),
    onPrimaryContainer = Color(0xFFE7E2FF),
    inversePrimary = LabPalette.Violet,
    secondary = Color(0xFFCCC3F7),
    onSecondary = Color(0xFF292156),
    secondaryContainer = Color(0xFF3C3378),
    onSecondaryContainer = Color(0xFFE9E4FF),
    tertiary = Color(0xFF7DD3A4),
    onTertiary = Color(0xFF0C2E1B),
    tertiaryContainer = Color(0xFF1D4A31),
    onTertiaryContainer = Color(0xFFD5F3E1),
    background = Color(0xFF141019),
    onBackground = Color(0xFFEFEAF4),
    surface = Color(0xFF1D1825),
    onSurface = Color(0xFFEFEAF4),
    surfaceVariant = Color(0xFF322A3C),
    onSurfaceVariant = Color(0xFFCDC3D6),
    surfaceTint = Color(0xFFC2B8FF),
    surfaceBright = Color(0xFF3A3146),
    surfaceDim = Color(0xFF120F17),
    surfaceContainerLowest = Color(0xFF0F0C14),
    surfaceContainerLow = Color(0xFF1A1521),
    surfaceContainer = Color(0xFF201A29),
    surfaceContainerHigh = Color(0xFF271F31),
    surfaceContainerHighest = Color(0xFF2E263A),
    inverseSurface = Color(0xFFEFEAF4),
    inverseOnSurface = Color(0xFF211C29),
    outline = Color(0xFF4E4459),
    outlineVariant = Color(0xFF362E42),
    error = Color(0xFFFF9A92),
    onError = Color(0xFF4A100B),
    errorContainer = Color(0xFF7A2E28),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
)

private val LabFontFamily = FontFamily(
    Font(R.font.duolingosans_regular, FontWeight.Normal),
    Font(R.font.duolingosans_medium, FontWeight.Medium),
    Font(R.font.duolingosans_bold, FontWeight.Bold),
)

/**
 * Centered line-height keeps mixed CJK/Latin lines vertically balanced;
 * disabling font padding keeps kana/kanji from drifting inside pills and chips.
 */
private val LabLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun TextStyle.lab(
    fontSize: androidx.compose.ui.unit.TextUnit = this.fontSize,
    lineHeight: androidx.compose.ui.unit.TextUnit = this.lineHeight,
    fontWeight: FontWeight? = this.fontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit = this.letterSpacing,
): TextStyle = copy(
    fontFamily = LabFontFamily,
    fontSize = fontSize,
    lineHeight = lineHeight,
    fontWeight = fontWeight,
    letterSpacing = letterSpacing,
    lineHeightStyle = LabLineHeightStyle,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val LabTypography = Typography().run {
    copy(
        displayLarge = displayLarge.lab(fontWeight = FontWeight.Bold),
        displayMedium = displayMedium.lab(fontWeight = FontWeight.Bold),
        displaySmall = displaySmall.lab(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.lab(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.lab(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.lab(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.lab(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.lab(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
        titleSmall = titleSmall.lab(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.lab(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
        bodyMedium = bodyMedium.lab(fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
        bodySmall = bodySmall.lab(fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = labelLarge.lab(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
        labelMedium = labelMedium.lab(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
        labelSmall = labelSmall.lab(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    )
}

private val LabShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalLabExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LabTypography,
            shapes = LabShapes,
            content = content,
        )
    }
}
