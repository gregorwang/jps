package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.words.HomoGroup
import com.animejapaneselab.nativeapp.ui.words.HomoWord
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/*
 * 同音の部屋 as a pond (画布「同音の部屋」意象 D 波紋). The reading is a stone in the middle; one word
 * written with several kanji sits on the inner ring, its kanji joined by an arc; words that only share
 * the sound float on the outer ring. Dropping the stone sends ripples out and every word bobs as the
 * front passes; picking a word rings it.
 */

/** Where each word floats: ring radius (fraction of the pond's width) and angle in degrees (0 = right, clockwise). */
internal data class PondSpot(val word: HomoWord, val radius: Float, val angle: Float, val cluster: Int)

internal object PondRules {
    const val Inner = 0.27f
    const val Outer = 0.43f
    private const val Single = 0.36f

    fun layout(group: HomoGroup): List<PondSpot> {
        val clusters = group.sameWord
        val inner = clusters.flatMap { c -> c.map { it to it.cluster } }
        val outer = group.soundOnly.map { it to 0 }
        if (inner.isEmpty() || outer.isEmpty()) {
            val all = inner + outer
            val step = 360f / all.size
            return all.mapIndexed { i, (w, c) -> PondSpot(w, Single, -90f + step * i, c) }
        }
        return arc(inner, Inner, -90f, 160f, 58f) + arc(outer, Outer, 90f, 150f, 52f)
    }

    /** [items] side by side around [center] degrees, at most [maxStep] apart, within [span]. */
    private fun arc(items: List<Pair<HomoWord, Int>>, radius: Float, center: Float, span: Float, maxStep: Float): List<PondSpot> {
        val step = if (items.size > 1) min(maxStep, span / (items.size - 1)) else 0f
        return items.mapIndexed { i, (w, c) -> PondSpot(w, radius, center + (i - (items.size - 1) / 2f) * step, c) }
    }
}

@Composable
internal fun HomophonePond(
    group: HomoGroup,
    selectedId: String?,
    dropKey: Int,
    pickKey: Int,
    onStone: () -> Unit,
    onWord: (HomoWord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val reduced = rememberReducedMotion()
    val spots = remember(group) { PondRules.layout(group) }

    // 0..1 over one drop; ring k starts k * 0.14 later
    val drop = remember(group.reading) { Animatable(1f) }
    LaunchedEffect(dropKey) {
        if (dropKey > 0 && !reduced) {
            drop.snapTo(0f)
            drop.animateTo(1f, tween(DropMs.toInt(), easing = LinearEasing))
        }
    }
    val pick = remember(group.reading) { Animatable(1f) }
    LaunchedEffect(pickKey) {
        if (pickKey > 0 && !reduced) {
            pick.snapTo(0f)
            pick.animateTo(1f, tween(700, easing = LinearEasing))
        }
    }
    val ambient = if (reduced) 0f else rememberInfiniteTransition(label = "pond").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "ambient",
    ).value

    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f)) {
        val side = maxWidth
        val chip: Dp = if (spots.size > 5) 54.dp else 62.dp
        val stone = side * 0.27f
        val front = { d: Float -> ease(((d * DropMs) / RingMs).coerceIn(0f, 1f)) }

        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val stoneR = stone.toPx() / 2
            val maxR = size.width / 2 - 2.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))
            spots.map { it.radius }.distinct().forEach { r ->
                drawCircle(colors.line2, radius = r * size.width, center = c, style = Stroke(1.dp.toPx(), pathEffect = dash))
            }
            // one word, several kanji: an arc along the ring joins them
            spots.filter { it.cluster != 0 }.groupBy { it.cluster }.values.forEach { members ->
                val r = members.first().radius * size.width
                val a0 = members.minOf { it.angle }
                val a1 = members.maxOf { it.angle }
                drawArc(
                    colors.info, startAngle = a0, sweepAngle = a1 - a0, useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r),
                    style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            if (!reduced) {
                listOf(ambient, (ambient + 0.5f) % 1f).forEach { p ->
                    drawCircle(work.accent.copy(alpha = 0.32f * (1 - p)), radius = stoneR + (maxR - stoneR) * ease(p), center = c, style = Stroke(1.5.dp.toPx()))
                }
            }
            if (drop.value < 1f) {
                for (k in 0 until 3) {
                    val q = (((drop.value * DropMs) - k * 180f) / RingMs).coerceIn(0f, 1f)
                    if (q <= 0f || q >= 1f) continue
                    drawCircle(
                        work.accent.copy(alpha = 0.85f * (1 - q)),
                        radius = stoneR + (maxR - stoneR) * ease(q), center = c,
                        style = Stroke((2.5f - k * 0.7f).dp.toPx()),
                    )
                }
            }
            spots.firstOrNull { it.word.id == selectedId }?.takeIf { pick.value < 1f }?.let { s ->
                val rad = s.angle * PI.toFloat() / 180f
                val at = Offset(c.x + s.radius * size.width * cos(rad), c.y + s.radius * size.width * sin(rad))
                val q = pick.value
                drawCircle(work.accent.copy(alpha = 0.8f * (1 - q)), radius = chip.toPx() / 2 * (1 + 1.1f * ease(q)), center = at, style = Stroke(1.5.dp.toPx()))
            }
        }

        // the stone
        val press = if (drop.value < 1f) sin(PI.toFloat() * (drop.value * 6f).coerceAtMost(1f)) else 0f
        Column(
            Modifier
                .align(Alignment.Center)
                .size(stone)
                .graphicsLayer { scaleX = 1 - 0.07f * press; scaleY = 1 - 0.07f * press }
                .background(colors.surface, CircleShape)
                .border(AjlStroke.Ink, colors.ink, CircleShape)
                .clickableNoRipple(onClick = onStone)
                .semantics { contentDescription = "听 ${group.reading}" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(group.romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3, maxLines = 1)
            val fs = when {
                group.reading.length <= 3 -> 26.sp
                group.reading.length == 4 -> 21.sp
                else -> 17.sp
            }
            Text(group.reading, style = AjlTheme.type.jpTitle.copy(fontSize = fs, lineHeight = fs * 1.25f), color = work.accent, maxLines = 1)
        }

        // the words, bobbing as the front passes them
        spots.forEach { s ->
            val rad = s.angle * PI.toFloat() / 180f
            val x = side / 2 + side * s.radius * cos(rad) - chip / 2
            val y = side / 2 + side * s.radius * sin(rad) - chip / 2
            val bob = if (drop.value < 1f) {
                val frontR = 0.135f + (0.5f - 0.135f) * front(drop.value)
                val d = (frontR - s.radius) / 0.05f
                -9f * exp(-d * d)
            } else 0f
            val on = s.word.id == selectedId
            Box(
                Modifier
                    .offset(x, y)
                    .graphicsLayer { translationY = bob.dp.toPx() }
                    .size(chip)
                    .background(if (on) work.soft else colors.surface, CircleShape)
                    .border(if (on) 2.dp else AjlStroke.Ink, if (on) work.accent else colors.ink, CircleShape)
                    .clickableNoRipple(onClick = { onWord(s.word) })
                    .semantics { contentDescription = "${s.word.surface}，${s.word.meaning}" },
                contentAlignment = Alignment.Center,
            ) {
                val fs = when (s.word.surface.length) {
                    in 0..2 -> 21.sp
                    3 -> 17.sp
                    else -> 13.sp
                }
                Text(s.word.surface, style = AjlTheme.type.jpTitle.copy(fontSize = fs, lineHeight = fs * 1.2f), color = colors.ink, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Under the pond: what the arc and the outer ring mean (only when the group has both). */
@Composable
internal fun PondLegend(group: HomoGroup) {
    if (group.sameWord.isEmpty() || group.soundOnly.isEmpty()) return
    val colors = AjlTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(18.dp).height(2.5.dp).background(colors.info, CircleShape))
            Text("内圈 · 同一个词换字", style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.info)
        }
        Text("外圈 · 碰巧同音", style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3)
    }
}

private const val DropMs = 1500f
private const val RingMs = 1100f

private fun ease(t: Float): Float = 1 - (1 - t) * (1 - t) * (1 - t)
