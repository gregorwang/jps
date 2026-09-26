package com.animejapaneselab.nativeapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.R

// ---------------------------------------------------------------------------
// v3「アニメの文法」tokens. See design/V3_IMPLEMENTATION_PLAN.md §2.
// ---------------------------------------------------------------------------

/** Neutral paper/ink palette. Work colour lives in [WorkTheme], never here. */
@Immutable
data class AjlColors(
    val bg: Color,
    val surface: Color,
    val sunken: Color,
    val line: Color,
    val line2: Color,
    /** Body text and the (one per screen) primary button fill. */
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    /** Text/icons drawn on top of [ink]. */
    val onInk: Color,
    /** Secondary text on top of [ink] (button captions). */
    val onInk2: Color,
    /** Disabled / not-yet text (future episodes, inactive tabs). */
    val faint: Color,
    val ok: Color,
    val okSoft: Color,
    val bad: Color,
    val badSoft: Color,
    /** 朱印 — stamps and seals that mean "done" or "identity". */
    val stamp: Color,
    /** Scrim under sheets and the command palette (already at 28%). */
    val scrim: Color,
    val isDark: Boolean,
)

val LightAjlColors = AjlColors(
    bg = Color(0xFFF7F7F4),
    surface = Color(0xFFFFFFFF),
    sunken = Color(0xFFEFEEE9),
    line = Color(0xFFE4E3DD),
    line2 = Color(0xFFD3D2CB),
    ink = Color(0xFF1B1B19),
    ink2 = Color(0xFF55544F),
    ink3 = Color(0xFF6E6D67),
    onInk = Color(0xFFFFFFFF),
    onInk2 = Color(0xFFBDBCB6),
    faint = Color(0xFFA3A29C),
    ok = Color(0xFF2C7A55),
    okSoft = Color(0xFFE6F1EA),
    bad = Color(0xFFB8432F),
    badSoft = Color(0xFFF8E9E5),
    stamp = Color(0xFFB8432F),
    scrim = Color(0x471B1B19),
    isDark = false,
)

val DarkAjlColors = AjlColors(
    bg = Color(0xFF141413),
    surface = Color(0xFF1C1C1A),
    sunken = Color(0xFF232320),
    line = Color(0xFF2E2E2A),
    line2 = Color(0xFF3A3A36),
    ink = Color(0xFFEDEDE8),
    ink2 = Color(0xFFA9A8A1),
    ink3 = Color(0xFF86857E),
    onInk = Color(0xFF141413),
    onInk2 = Color(0xFF55544F),
    faint = Color(0xFF5E5D58),
    ok = Color(0xFF6FC79C),
    okSoft = Color(0xFF1E3328),
    bad = Color(0xFFF08E7A),
    badSoft = Color(0xFF3A2420),
    stamp = Color(0xFFF08E7A),
    scrim = Color(0x8C000000),
    isDark = true,
)

/** Which colour family a work belongs to. */
enum class WorkHue(val jpName: String) {
    Sakura("桜"),
    Sumire("菫"),
    Ai("藍"),
}

/**
 * Per-work accent. Only used for: the 3px broadcast line, progress lines, due counts, the
 * current item, seals, screentone and name plates. Structure never changes between works.
 */
@Immutable
data class WorkTheme(
    val hue: WorkHue,
    val accent: Color,
    val soft: Color,
    /** Start of the progress ramp (empty end). */
    val rampLight: Color,
    /** End of the progress ramp (full end). */
    val rampDeep: Color,
    /** Text/icons on top of [accent] (name plates, いま tabs). */
    val onAccent: Color,
) {
    val name: String get() = hue.jpName

    /** Screentone dot colour: accent at ~30%. */
    fun tone(alpha: Float = 0.30f): Color = accent.copy(alpha = alpha)

    /** Progress colour at [fraction]; right/wrong answers never tint progress. */
    fun progressColor(fraction: Float): Color = progressRamp(rampLight, rampDeep, fraction)
}

/** Linear interpolation between the light and deep end of a work's ramp. Pure, unit-tested. */
fun progressRamp(light: Color, deep: Color, fraction: Float): Color =
    lerp(light, deep, fraction.coerceIn(0f, 1f))

object WorkThemes {
    private val SakuraLight = WorkTheme(
        hue = WorkHue.Sakura,
        accent = Color(0xFFC4466F),
        soft = Color(0xFFF8E6EC),
        rampLight = Color(0xFFEBC3D0),
        rampDeep = Color(0xFFAA3C64),
        onAccent = Color.White,
    )
    private val SakuraDark = WorkTheme(
        hue = WorkHue.Sakura,
        accent = Color(0xFFE58AA7),
        soft = Color(0xFF3A2230),
        rampLight = Color(0xFF5C3444),
        rampDeep = Color(0xFFF0A9C0),
        onAccent = Color(0xFF141413),
    )
    private val SumireLight = WorkTheme(
        hue = WorkHue.Sumire,
        accent = Color(0xFF6A4FC4),
        soft = Color(0xFFEEEAFA),
        rampLight = Color(0xFFD3CAF2),
        rampDeep = Color(0xFF4E36A6),
        onAccent = Color.White,
    )
    private val SumireDark = WorkTheme(
        hue = WorkHue.Sumire,
        accent = Color(0xFFA894F2),
        soft = Color(0xFF2A2446),
        rampLight = Color(0xFF3E3566),
        rampDeep = Color(0xFFC2B4F7),
        onAccent = Color(0xFF141413),
    )
    private val AiLight = WorkTheme(
        hue = WorkHue.Ai,
        accent = Color(0xFF3A4FCB),
        soft = Color(0xFFECEEFB),
        rampLight = Color(0xFFC9D0F4),
        rampDeep = Color(0xFF2B3CA8),
        onAccent = Color.White,
    )
    private val AiDark = WorkTheme(
        hue = WorkHue.Ai,
        accent = Color(0xFF8C9BF2),
        soft = Color(0xFF262B45),
        rampLight = Color(0xFF363E66),
        rampDeep = Color(0xFFAEB9F6),
        onAccent = Color(0xFF141413),
    )

    fun of(hue: WorkHue, dark: Boolean): WorkTheme = when (hue) {
        WorkHue.Sakura -> if (dark) SakuraDark else SakuraLight
        WorkHue.Sumire -> if (dark) SumireDark else SumireLight
        WorkHue.Ai -> if (dark) AiDark else AiLight
    }

    /** k-on → 桜, re-zero (every season) → 菫, anything else / global pages → 藍. */
    fun hueFor(workSlug: String?): WorkHue = when (normalizeWorkSlug(workSlug)) {
        "k-on" -> WorkHue.Sakura
        "re-zero" -> WorkHue.Sumire
        else -> WorkHue.Ai
    }

    fun forSlug(workSlug: String?, dark: Boolean): WorkTheme = of(hueFor(workSlug), dark)
}

/** Canonical slug: lower case, `_`→`-`, known aliases folded (`rezero`, `re-zero-s2` …). */
fun normalizeWorkSlug(workSlug: String?): String {
    val normalized = workSlug.orEmpty().trim().lowercase().replace('_', '-')
    return when {
        normalized.isEmpty() -> ""
        normalized == "kon" || normalized.startsWith("k-on") -> "k-on"
        normalized.startsWith("rezero") || normalized.startsWith("re-zero") -> "re-zero"
        else -> normalized
    }
}

// ---------------------------------------------------------------------------
// Typography
// ---------------------------------------------------------------------------

/** IBM Plex Mono (OFL, bundled) — metadata only: episode numbers, counts, time codes. */
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

/** Japanese source text: the platform's Noto Serif CJK (not bundled; falls back to sans). */
val JpSerif: FontFamily = FontFamily.Serif

private val CenteredLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(
    family: FontFamily,
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = family,
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    lineHeightStyle = CenteredLineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Immutable
data class AjlType(
    /** Serif 900 — the one hero per screen: episode numbers, 今日の一句, page titles like 課程. */
    val jpDisplay: TextStyle,
    /** Serif 700 — section headings in Japanese (本日の時間割, 出席カード). */
    val jpTitle: TextStyle,
    /** Serif 500 — dialogue lines, word tiles, quoted source text. */
    val jpBody: TextStyle,
    /** Serif 400 small — labels such as 学籍 · 邮箱, tab captions. */
    val jpLabel: TextStyle,
    /** Sans 600 — Chinese prompts / question titles. */
    val title: TextStyle,
    /** Sans 400 — body copy. */
    val body: TextStyle,
    /** Sans 500 — button labels. */
    val label: TextStyle,
    /** Sans small — translations, secondary lines. */
    val caption: TextStyle,
    /** Plex Mono — episode numbers, counts, time codes, eyebrow labels. */
    val meta: TextStyle,
    /** Plex Mono tiny — corner indices (attendance grid numbers). */
    val metaSmall: TextStyle,
)

val DefaultAjlType = AjlType(
    jpDisplay = style(JpSerif, 34.sp, 44.sp, FontWeight.Black),
    jpTitle = style(JpSerif, 17.sp, 24.sp, FontWeight.Bold),
    jpBody = style(JpSerif, 19.sp, 30.sp, FontWeight.Medium),
    jpLabel = style(JpSerif, 12.sp, 16.sp, FontWeight.Normal),
    title = style(FontFamily.Default, 20.sp, 28.sp, FontWeight.SemiBold),
    body = style(FontFamily.Default, 15.sp, 22.sp, FontWeight.Normal),
    label = style(FontFamily.Default, 16.sp, 22.sp, FontWeight.Medium),
    caption = style(FontFamily.Default, 13.sp, 18.sp, FontWeight.Normal),
    meta = style(PlexMono, 11.sp, 16.sp, FontWeight.Normal, letterSpacing = 0.3.sp),
    metaSmall = style(PlexMono, 9.sp, 12.sp, FontWeight.Normal),
)

// ---------------------------------------------------------------------------
// Shape & stroke
// ---------------------------------------------------------------------------

object AjlShape {
    /** 漫画框 — story and content panels. Paired with [AjlStroke.Ink]. */
    val Panel = RoundedCornerShape(4.dp)
    /** Dialogue box / word tile. */
    val Tile = RoundedCornerShape(6.dp)
    /** Buttons. */
    val Button = RoundedCornerShape(10.dp)
    /** Tools and settings. Paired with [AjlStroke.Hair]. */
    val Tool = RoundedCornerShape(12.dp)
    /** Bottom sheets. */
    val Sheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
}

object AjlStroke {
    /** 1.5px ink line — story/content frames. */
    val Ink: Dp = 1.5.dp
    /** 1px hairline — tools/settings/lists. */
    val Hair: Dp = 1.dp
    /** Solid offset shadow used only by word tiles and the current textbook. */
    val SolidShadow: Dp = 2.dp
    val SolidShadowLarge: Dp = 3.dp
}

// ---------------------------------------------------------------------------
// Locals & accessors
// ---------------------------------------------------------------------------

val LocalAjlColors = staticCompositionLocalOf { LightAjlColors }
val LocalAjlType = staticCompositionLocalOf { DefaultAjlType }
val LocalWorkTheme = staticCompositionLocalOf { WorkThemes.of(WorkHue.Ai, dark = false) }

object AjlTheme {
    val colors: AjlColors
        @Composable @ReadOnlyComposable get() = LocalAjlColors.current
    val type: AjlType
        @Composable @ReadOnlyComposable get() = LocalAjlType.current
    val work: WorkTheme
        @Composable @ReadOnlyComposable get() = LocalWorkTheme.current
}

/** Re-colours [content] for the given work. Nest it wherever the current work is known. */
@Composable
fun ProvideWorkTheme(workSlug: String?, content: @Composable () -> Unit) {
    val dark = LocalAjlColors.current.isDark
    val theme = remember(workSlug, dark) { WorkThemes.forSlug(workSlug, dark) }
    CompositionLocalProvider(LocalWorkTheme provides theme, content = content)
}

private fun AjlColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = ink, onPrimary = onInk,
        primaryContainer = sunken, onPrimaryContainer = ink,
        secondary = ink2, onSecondary = onInk,
        secondaryContainer = sunken, onSecondaryContainer = ink,
        tertiary = ok, onTertiary = onInk,
        tertiaryContainer = okSoft, onTertiaryContainer = ok,
        background = bg, onBackground = ink,
        surface = bg, onSurface = ink,
        surfaceVariant = sunken, onSurfaceVariant = ink2,
        surfaceTint = Color.Transparent,
        surfaceBright = surface, surfaceDim = bg,
        surfaceContainerLowest = bg, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = sunken,
        surfaceContainerHighest = sunken,
        inverseSurface = ink, inverseOnSurface = bg, inversePrimary = bg,
        outline = line2, outlineVariant = line,
        error = bad, onError = onInk, errorContainer = badSoft, onErrorContainer = bad,
        scrim = Color.Black,
    )
} else {
    lightColorScheme(
        primary = ink, onPrimary = onInk,
        primaryContainer = sunken, onPrimaryContainer = ink,
        secondary = ink2, onSecondary = onInk,
        secondaryContainer = sunken, onSecondaryContainer = ink,
        tertiary = ok, onTertiary = onInk,
        tertiaryContainer = okSoft, onTertiaryContainer = ok,
        background = bg, onBackground = ink,
        surface = bg, onSurface = ink,
        surfaceVariant = sunken, onSurfaceVariant = ink2,
        surfaceTint = Color.Transparent,
        surfaceBright = surface, surfaceDim = sunken,
        surfaceContainerLowest = surface, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = sunken,
        surfaceContainerHighest = sunken,
        inverseSurface = ink, inverseOnSurface = bg, inversePrimary = bg,
        outline = line2, outlineVariant = line,
        error = bad, onError = onInk, errorContainer = badSoft, onErrorContainer = bad,
        scrim = Color.Black,
    )
}

private val MaterialTypography = Typography().run {
    fun TextStyle.sys() = copy(
        fontFamily = FontFamily.Default,
        lineHeightStyle = CenteredLineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    copy(
        displayLarge = displayLarge.sys(), displayMedium = displayMedium.sys(),
        displaySmall = displaySmall.sys(), headlineLarge = headlineLarge.sys(),
        headlineMedium = headlineMedium.sys(), headlineSmall = headlineSmall.sys(),
        titleLarge = titleLarge.sys(), titleMedium = titleMedium.sys(), titleSmall = titleSmall.sys(),
        bodyLarge = bodyLarge.sys(), bodyMedium = bodyMedium.sys(), bodySmall = bodySmall.sys(),
        labelLarge = labelLarge.sys(), labelMedium = labelMedium.sys(), labelSmall = labelSmall.sys(),
    )
}

private val MaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

/**
 * App root theme. Signature is kept from v2; [dynamicColor] is accepted but ignored — v3 is a
 * fixed paper/ink palette with per-work accents. Nest [ProvideWorkTheme] to change the accent.
 */
@Composable
fun AnimeJapaneseLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAjlColors else LightAjlColors
    val materialColors = remember(darkTheme) { colors.toMaterial() }
    CompositionLocalProvider(
        LocalAjlColors provides colors,
        LocalAjlType provides DefaultAjlType,
        LocalWorkTheme provides WorkThemes.of(WorkHue.Ai, darkTheme),
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = MaterialTypography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}

object LabSpacing {
    val XXSmall = 4.dp
    val XSmall = 8.dp
    val Small = 12.dp
    val Medium = 16.dp
    val Large = 20.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
    /** v3 screen gutter. */
    val Screen = 20.dp
}

