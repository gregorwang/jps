package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * Vertical-rl stacked text (縦書き). Compose has no vertical writing mode, so each glyph is a
 * row; `\n` starts a new column to the left. Punctuation is swapped for vertical forms via
 * [TextRules.verticalGlyph].
 *
 * Reveal (optional): when [revealStaggerMillis] is set each glyph rises [revealRise] and fades
 * in over [revealDurationMillis], staggered in reading order. Used by アイキャッチ (60ms/220ms/10dp)
 * and 今日の一句 on the first open of the day (40ms). Reduced motion shows the final state.
 */
@Composable
fun VerticalText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AjlTheme.type.jpDisplay,
    color: Color = AjlTheme.colors.ink,
    columnGap: Dp = 6.dp,
    glyphSpacing: Float = 1.12f,
    revealStaggerMillis: Int? = null,
    revealDurationMillis: Int = 220,
    revealRise: Dp = 10.dp,
    revealStartDelayMillis: Int = 0,
) {
    val columns = remember(text) { TextRules.verticalColumns(text) }
    val reduced = rememberReducedMotion()
    val totalGlyphs = columns.sumOf { it.size }
    val animate = revealStaggerMillis != null && !reduced
    val clock = remember(text, animate) { Animatable(if (animate) 0f else Float.MAX_VALUE) }
    if (animate) {
        LaunchedEffect(text) {
            val total = revealStartDelayMillis + totalGlyphs * revealStaggerMillis!! + revealDurationMillis
            clock.snapTo(0f)
            clock.animateTo(total.toFloat(), tween(total, easing = LinearEasing))
        }
    }
    val risePx = with(LocalDensity.current) { revealRise.toPx() }
    val fontSize = style.fontSize.takeIf { it != androidx.compose.ui.unit.TextUnit.Unspecified } ?: 20.sp
    val cellHeight = with(LocalDensity.current) { (fontSize.toPx() * glyphSpacing).toDp() }
    val cellWidth = with(LocalDensity.current) { (fontSize.toPx() * 1.18f).toDp() }
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = text.replace("\n", "") },
        horizontalArrangement = Arrangement.spacedBy(columnGap),
    ) {
        // vertical-rl: first column is right-most.
        var glyphIndexBase = 0
        val bases = columns.map { col -> glyphIndexBase.also { glyphIndexBase += col.size } }
        columns.indices.reversed().forEach { columnIndex ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                columns[columnIndex].forEachIndexed { rowIndex, glyph ->
                    val index = bases[columnIndex] + rowIndex
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .graphicsLayer {
                                if (animate) {
                                    val start = revealStartDelayMillis + index * revealStaggerMillis!!
                                    val t = ((clock.value - start) / revealDurationMillis).coerceIn(0f, 1f)
                                    val eased = MotionTokens.Ease.Decelerate.transform(t)
                                    alpha = eased
                                    translationY = (1f - eased) * risePx
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = glyph,
                            style = style.copy(lineHeight = with(LocalDensity.current) { cellHeight.toSp() }),
                            color = color,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 着重号 — sesame dots above the characters in [emphasis] ranges. Dot colour fades from
 * transparent to [dotColor] over 300ms whenever [emphasisVisible] flips true (answer correct).
 */
@Composable
fun EmphasisText(
    text: String,
    emphasis: List<IntRange>,
    modifier: Modifier = Modifier,
    style: TextStyle = AjlTheme.type.jpBody,
    color: Color = AjlTheme.colors.ink,
    dotColor: Color = AjlTheme.work.accent,
    emphasisVisible: Boolean = true,
    dotRadius: Dp = 1.8.dp,
) {
    val reduced = rememberReducedMotion()
    val alpha by animateFloatAsState(
        targetValue = if (emphasisVisible) 1f else 0f,
        animationSpec = MotionTokens.standard(300, reduced),
        label = "emphasis-alpha",
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val radiusPx = with(LocalDensity.current) { dotRadius.toPx() }
    val gapPx = with(LocalDensity.current) { 3.dp.toPx() }
    Text(
        text = AnnotatedString(text),
        modifier = modifier
            .padding(top = dotRadius * 2 + 3.dp)
            .drawBehind {
                val result = layout ?: return@drawBehind
                if (alpha <= 0f) return@drawBehind
                emphasis.forEach { range ->
                    for (i in range) {
                        if (i !in text.indices || text[i].isWhitespace()) continue
                        val box = result.getBoundingBox(i)
                        drawCircle(
                            color = dotColor.copy(alpha = dotColor.alpha * alpha),
                            radius = radiusPx,
                            center = Offset(box.center.x, box.top - gapPx - radiusPx),
                        )
                    }
                }
            },
        style = style,
        color = color,
        onTextLayout = { layout = it },
    )
}

/** Finds every occurrence of [keyword] in [text] as ranges for [EmphasisText]. */
fun emphasisRanges(text: String, keyword: String): List<IntRange> {
    if (keyword.isBlank()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var start = text.indexOf(keyword)
    while (start >= 0) {
        ranges += start until start + keyword.length
        start = text.indexOf(keyword, start + keyword.length)
    }
    return ranges
}

/**
 * Section heading: Japanese serif title, optional Chinese gloss and mono meta, sitting on a
 * 1.5px ink underline (本日の時間割 · 还剩 3 节).
 */
@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    gloss: String? = null,
    meta: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = type.jpTitle, color = colors.ink)
            if (gloss != null) Text(gloss, style = type.caption, color = colors.ink3)
            if (meta != null) Text(meta, style = type.meta, color = colors.ink3)
            if (trailing != null) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { trailing() }
            }
        }
        Hairline(strong = true)
    }
}

/** Mono eyebrow label (場面 03 · 平沢家の朝 · 12:31). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = AjlTheme.colors.ink3) {
    Text(
        text = text,
        modifier = modifier,
        style = AjlTheme.type.meta.copy(letterSpacing = 0.6.sp),
        color = color,
        maxLines = 1,
    )
}
