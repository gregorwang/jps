package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.hypot

/** 1px separator. Use [strong] for the 1.5px ink underline under section headings. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    color: Color = if (strong) AjlTheme.colors.ink else AjlTheme.colors.line,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(if (strong) AjlStroke.Ink else AjlStroke.Hair)
            .background(color),
    )
}

/** The work-colour 3px broadcast line pinned to the very top of a main screen. */
@Composable
fun BroadcastLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(AjlTheme.work.accent),
    )
}

/**
 * Solid ink offset shadow (漫画の実影). Draws a filled copy of [shape] offset by [offset] behind
 * the content. [pressed] collapses it to zero — pair with a matching translation so the block
 * looks pushed into the page. The only shadow in v3.
 */
fun Modifier.solidShadow(
    offset: Dp,
    color: Color,
    shape: Shape = AjlShape.Tile,
): Modifier = drawBehind {
    if (offset <= 0.dp) return@drawBehind
    val o = offset.toPx()
    val outline = shape.createOutline(size, layoutDirection, this)
    translate(o, o) { drawOutline(outline, color) }
}

/**
 * 漫画框 — a story/content panel: 1.5px ink border, 4px corners, surface fill. Optionally a
 * solid offset shadow (dialogue box: 3dp; current textbook: 3dp).
 */
@Composable
fun MangaPanel(
    modifier: Modifier = Modifier,
    shape: Shape = AjlShape.Panel,
    background: Color = AjlTheme.colors.surface,
    borderColor: Color = AjlTheme.colors.ink,
    shadow: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val ink = AjlTheme.colors.ink
    Box(
        modifier = modifier
            .solidShadow(shadow, ink, shape)
            .clip(shape)
            .background(background)
            .border(AjlStroke.Ink, borderColor, shape),
        content = content,
    )
}

/** Tool/settings container: 1px hairline, 12px corners. */
@Composable
fun ToolPanel(
    modifier: Modifier = Modifier,
    background: Color = AjlTheme.colors.surface,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(AjlShape.Tool)
            .background(background)
            .border(AjlStroke.Hair, AjlTheme.colors.line, AjlShape.Tool),
        content = content,
    )
}

/**
 * 網点 — the only texture. Dot grid rotated by [angleDegrees] (12° by default), clipped to the
 * element. Use in one corner of the hero area, never as a full-screen background.
 */
fun Modifier.screentone(
    color: Color,
    angleDegrees: Float = 12f,
    spacing: Dp = 7.dp,
    dotRadius: Dp = 1.2.dp,
): Modifier = drawWithCache {
    val step = spacing.toPx().coerceAtLeast(2f)
    val r = dotRadius.toPx()
    val diag = hypot(size.width, size.height)
    val half = diag / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val count = ceil(diag / step).toInt() + 1
    val path = Path()
    for (row in 0..count) {
        val y = cy - half + row * step
        for (col in 0..count) {
            val x = cx - half + col * step
            path.addOval(Rect(x - r, y - r, x + r, y + r))
        }
    }
    onDrawBehind {
        clipRect {
            rotate(angleDegrees, pivot = Offset(cx, cy)) {
                drawPath(path, color)
            }
        }
    }
}

/** A standalone screentone patch — place it absolutely (e.g. a rotated band in a corner). */
@Composable
fun Screentone(
    modifier: Modifier = Modifier,
    color: Color = AjlTheme.work.tone(),
    angleDegrees: Float = 12f,
    spacing: Dp = 7.dp,
    dotRadius: Dp = 1.2.dp,
) {
    Box(modifier.screentone(color, angleDegrees, spacing, dotRadius))
}

/**
 * Continuous 3px progress line. Width eases over 300ms; the fill is a ramp from the work's light
 * end to the colour at the current fraction. Right/wrong never recolours it.
 */
@Composable
fun ProgressLine(
    progress: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = 3.dp,
    trackColor: Color = AjlTheme.colors.line,
    contentDescription: String? = null,
) {
    val reduced = rememberReducedMotion()
    val work = AjlTheme.work
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MotionTokens.standard(MotionTokens.Dur.Progress, reduced),
        label = "progress-line",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(trackColor, cornerRadius = radius)
                val w = size.width * animated
                if (w > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(work.rampLight, work.progressColor(animated)),
                            startX = 0f,
                            endX = w,
                        ),
                        size = Size(w, size.height),
                        cornerRadius = radius,
                    )
                }
            },
    )
}

/**
 * ・・・ — three ink dots darkening in turn (300ms apart). Stays invisible for [delayMillis] so
 * fast loads never flash a spinner. No spinners, no shimmer anywhere in v3.
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    delayMillis: Int = MotionTokens.Dur.LoadingDelay,
    color: Color = AjlTheme.colors.ink,
    dotSize: Dp = 5.dp,
) {
    var visible by remember { mutableStateOf(delayMillis <= 0) }
    LaunchedEffect(delayMillis) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
            visible = true
        }
    }
    if (!visible) {
        Box(modifier.size(width = dotSize * 5, height = dotSize))
        return
    }
    val reduced = rememberReducedMotion()
    val step = MotionTokens.Dur.LoadingDotStep
    val transition = rememberInfiniteTransition(label = "loading-dots")
    val phase by if (reduced) {
        remember { mutableStateOf(0f) }
    } else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(step * 3, easing = LinearEasing)),
            label = "loading-dots-phase",
        )
    }
    Row(
        modifier = modifier.semantics { contentDescription = "加载中" },
        horizontalArrangement = Arrangement.spacedBy(dotSize),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val active = !reduced && phase.toInt() == index
            Box(
                Modifier
                    .size(dotSize)
                    .graphicsLayer { alpha = if (reduced) 0.6f else if (active) 1f else 0.25f }
                    .background(color, CircleShape),
            )
        }
    }
}

/**
 * Pull-to-refresh indicator: a work-colour hairline that grows from the centre outward.
 * [fraction] 0..1 is the pull distance; [refreshing] keeps it breathing full width.
 */
@Composable
fun RefreshLine(
    fraction: Float,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = AjlTheme.work.accent
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "refresh-line")
    val pulse by if (refreshing && !reduced) {
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700, easing = MotionTokens.Ease.Standard), RepeatMode.Reverse),
            label = "refresh-pulse",
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                val f = if (refreshing) 1f else fraction.coerceIn(0f, 1f)
                val w = size.width * f
                drawRect(
                    color = accent.copy(alpha = if (refreshing) pulse else 1f),
                    topLeft = Offset((size.width - w) / 2f, 0f),
                    size = Size(w, size.height),
                )
            },
    )
}

/**
 * Wrong-answer nudge: ±6dp twice over 220ms on the element only — never the whole page.
 * Increment [trigger] to play it.
 */
@Composable
fun Modifier.shakeOnWrong(trigger: Int): Modifier {
    val reduced = rememberReducedMotion()
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0 || reduced) return@LaunchedEffect
        offset.snapTo(0f)
        offset.animateTo(
            0f,
            keyframes {
                durationMillis = 220
                6f at 40
                -6f at 95
                6f at 150
                -6f at 195
                0f at 220
            },
        )
    }
    return this.graphicsLayer { translationX = offset.value * density }
}

/** Absolute placement helper for decorative layers (dp offsets from the parent's top-start). */
fun Modifier.at(x: Dp, y: Dp): Modifier = offset(x = x, y = y)

/** Standard horizontal screen gutter (20dp). */
fun Modifier.screenGutter(): Modifier = padding(horizontal = 20.dp)
