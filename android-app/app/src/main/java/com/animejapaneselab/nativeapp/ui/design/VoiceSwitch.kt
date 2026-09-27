package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Voice pill with more than one voice (原声 / TTS): swipe it sideways to switch, tap to play.
 * The label slides with the finger and the next one comes in from the swipe side; dots on the
 * right show which voice is on.
 */
@Composable
fun VoiceSwitchPill(
    playing: Boolean,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val reduced = rememberReducedMotion()
    val shape = RoundedCornerShape(22.dp)
    val content = if (playing) work.accent else colors.ink
    var drag by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableFloatStateOf(1f) }
    val settle by animateFloatAsState(drag, tween(if (drag == 0f) 180 else 0), label = "voice-drag")
    Row(
        modifier
            .heightIn(min = 44.dp)
            .widthIn(min = 116.dp)
            .clip(shape)
            .background(if (playing) work.soft else colors.surface)
            .border(AjlStroke.Ink, colors.ink, shape)
            .pointerInput(selected, options.size) {
                val threshold = 28.dp.toPx()
                val limit = 44.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(drag) > threshold && options.size > 1) {
                            val step = if (drag < 0) 1 else -1
                            direction = step.toFloat()
                            onSelect((selected + step).mod(options.size))
                        }
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                ) { change, amount ->
                    change.consume()
                    drag = (drag + amount).coerceIn(-limit, limit)
                }
            }
            .clickable(onClickLabel = "播放${options.getOrNull(selected).orEmpty()}", role = Role.Button, onClick = onClick)
            .semantics { stateDescription = options.getOrNull(selected).orEmpty() }
            .padding(start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VoiceBars(active = playing, color = content)
        Box(Modifier.widthIn(min = 40.dp).graphicsLayer {
            translationX = settle * 0.6f
            alpha = 1f - (abs(settle) / 44.dp.toPx()) * 0.6f
        }) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(80))
                    } else {
                        val d = direction
                        (slideInHorizontally(tween(MotionTokens.Dur.State)) { (it * d).toInt() } + fadeIn(tween(160))) togetherWith
                            (slideOutHorizontally(tween(MotionTokens.Dur.State)) { (-it * d).toInt() } + fadeOut(tween(120)))
                    }
                },
                label = "voice-option",
            ) { index ->
                Text(options.getOrNull(index).orEmpty(), style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = content, maxLines = 1)
            }
        }
        if (options.size > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                options.indices.forEach { i ->
                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (i == selected) content else colors.line2))
                }
            }
        }
    }
}

/**
 * Screentone that speaks: a dot field that, while [playing], ripples out from [origin] (a
 * fraction of the box) — dots swell and push outward on each wave crest like a speaker cone,
 * fading with distance. Still under reduced motion.
 */
@Composable
fun VoiceTone(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AjlTheme.work.tone(0.22f),
    crest: Color = AjlTheme.work.accent,
    origin: Offset = Offset(0.5f, 0.5f),
    spacing: androidx.compose.ui.unit.Dp = 7.dp,
    dotRadius: androidx.compose.ui.unit.Dp = 1.2.dp,
) {
    val phase by rememberVoicePhase(playing, periodMillis = 1100)
    val level by animateFloatAsState(if (playing) 1f else 0f, tween(MotionTokens.Dur.State * 2), label = "tone-level")
    Canvas(modifier) {
        val step = spacing.toPx()
        val base = dotRadius.toPx()
        val o = Offset(size.width * origin.x, size.height * origin.y)
        val wavelength = 26.dp.toPx()
        val reach = 110.dp.toPx()
        val push = 2.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                val dx = x - o.x
                val dy = y - o.y
                val d = hypot(dx, dy)
                val wave = if (level > 0f) {
                    max(0f, sin(2 * Math.PI * (d / wavelength - phase * 1f)).toFloat()) * exp(-d / reach) * level
                } else {
                    0f
                }
                val dir = if (d > 0.1f) Offset(dx / d, dy / d) else Offset.Zero
                val c = if (wave > 0.02f) androidx.compose.ui.graphics.lerp(color, crest.copy(alpha = 0.55f), wave) else color
                drawCircle(c, radius = base * (1f + 1.4f * wave), center = Offset(x, y) + dir * (push * wave))
                x += step
            }
            y += step
        }
    }
}
