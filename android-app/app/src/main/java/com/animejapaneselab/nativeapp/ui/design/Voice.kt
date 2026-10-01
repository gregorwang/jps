package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 0→1 loop while [active]; stays at 0 when idle or under reduced motion. */
@Composable
fun rememberVoicePhase(active: Boolean, periodMillis: Int = 900): State<Float> {
    val reduced = rememberReducedMotion()
    if (!active || reduced) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "voice").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "voice-phase",
    )
}

/** 声 — four bars that bounce while a line is being voiced; they settle flat when it stops. */
@Composable
fun VoiceBars(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    val phase by rememberVoicePhase(active)
    val level by animateFloatAsState(if (active) 1f else 0f, tween(MotionTokens.Dur.State), label = "voice-level")
    val offsets = remember { floatArrayOf(0f, 0.35f, 0.7f, 0.15f) }
    Canvas(modifier.size(width = 16.dp, height = 14.dp)) {
        val barW = 2.2.dp.toPx()
        val gap = (size.width - barW * 4) / 3
        val minH = 3.dp.toPx()
        offsets.forEachIndexed { i, offset ->
            val wave = abs(sin(2 * PI * (phase + offset))).toFloat()
            val h = minH + (size.height - minH) * (0.25f + 0.75f * wave) * level
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barW + gap), (size.height - h) / 2),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2),
            )
        }
    }
}

/**
 * 効果線 — three short manga emphasis strokes that burst from the top-left corner of the
 * element while [active], the way a panel marks someone speaking. Draws outside the bounds.
 */
fun Modifier.speechLines(active: Boolean, color: Color): Modifier = composed {
    val phase by rememberVoicePhase(active, periodMillis = 720)
    val level by animateFloatAsState(if (active) 1f else 0f, tween(MotionTokens.Dur.State), label = "speech-level")
    drawBehind {
        if (level <= 0f) return@drawBehind
        val origin = Offset(6.dp.toPx(), 6.dp.toPx())
        val stroke = 1.6.dp.toPx()
        listOf(200f, 235f, 270f).forEachIndexed { i, angle ->
            val t = (phase + i * 0.18f) % 1f
            val rad = angle * PI.toFloat() / 180f
            val dir = Offset(cos(rad), sin(rad))
            val start = 10.dp.toPx() + 4.dp.toPx() * t
            val length = 7.dp.toPx() * (0.6f + 0.4f * sin(PI.toFloat() * t))
            drawLine(
                color = color.copy(alpha = color.alpha * level * (1f - 0.6f * t)),
                start = origin + dir * start,
                end = origin + dir * (start + length),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * 原声 pill: [VoiceBars] + 「原声」 in a 1.5px ink capsule, work-soft while the line plays.
 * Tapping replays the line.
 */
@Composable
fun VoicePill(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "原声") {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shape = RoundedCornerShape(20.dp)
    val content = if (playing) work.accent else colors.ink
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (playing) work.soft else colors.surface)
            .border(AjlStroke.Ink, colors.ink, shape)
            .clickable(onClickLabel = "播放$label", role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VoiceBars(active = playing, color = content)
        Text(label, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = content)
    }
}

/**
 * 声波 as the play control itself (no round ▶): a short row of bars, low and grey at rest; tap it
 * and it bounces in the work colour while the line sounds, tap again to stop. [synthetic] (TTS)
 * bounces grey instead, so 原声 and 合成 tell apart without a label.
 */
@Composable
fun VoiceWave(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    synthetic: Boolean = false,
) {
    val colors = AjlTheme.colors
    val phase by rememberVoicePhase(playing, periodMillis = 1100)
    val level by animateFloatAsState(if (playing) 1f else 0f, tween(MotionTokens.Dur.State), label = "wave-level")
    val rest = remember { floatArrayOf(0.35f, 0.6f, 0.45f, 0.8f, 0.5f, 0.7f, 0.4f, 0.55f, 0.3f) }
    val color = if (playing && !synthetic) AjlTheme.work.accent else colors.ink3
    Canvas(
        modifier
            .heightIn(min = 44.dp)
            .size(width = 64.dp, height = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = if (playing) "停止" else "播放", role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        val n = rest.size
        val barW = 2.4.dp.toPx()
        val gap = (size.width - barW * n) / (n - 1)
        val minH = 3.dp.toPx()
        rest.forEachIndexed { i, r ->
            val wave = abs(sin(2 * PI * (phase + i / n.toFloat()))).toFloat()
            val idle = minH + (size.height - minH) * r * 0.45f
            val live = minH + (size.height - minH) * (0.3f + 0.7f * wave)
            val h = idle + (live - idle) * level
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barW + gap), (size.height - h) / 2),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2),
            )
        }
    }
}
