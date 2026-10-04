package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Pieces shared by the 句型卡 and the 台词卡 (the 语法 / 台词 counterparts of the 词卡): the
 * sideways-swipe stage, the 收藏 / 斩 icons, coloured note labels and the pattern reveal.
 */

/** Sideways swipe on the card's top half = previous / next entry; the content drifts with the finger. */
@Composable
internal fun SwipeStage(
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val swipe = with(LocalDensity.current) { 40.dp.toPx() }
    val drift = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Column(
        modifier
            .fillMaxWidth()
            .pointerInput(onStep) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total <= -swipe) onStep(1) else if (total >= swipe) onStep(-1)
                        scope.launch { drift.animateTo(0f, tween(MotionTokens.Dur.Release)) }
                    },
                    onDragCancel = { scope.launch { drift.animateTo(0f) } },
                ) { change, delta ->
                    change.consume()
                    total += delta
                    scope.launch { drift.snapTo(total * 0.35f) }
                }
            }
            .graphicsLayer { translationX = drift.value },
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Level tag, a mono position, then 收藏 and 斩 as icons — the same header as the 词卡. */
@Composable
internal fun CardHeader(
    level: String?,
    meta: String,
    saved: Boolean,
    known: Boolean,
    onToggleSave: () -> Unit,
    onCut: () -> Unit,
    leading: @Composable () -> Unit = {},
) {
    val work = AjlTheme.work
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        leading()
        if (level != null) LevelTag(level)
        Text(meta, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = AjlTheme.colors.ink3, maxLines = 1, modifier = Modifier.weight(1f))
        IconButton44(
            if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            if (saved) "取消收藏" else "收藏",
            onToggleSave,
            tint = work.accent,
            iconSize = 22.dp,
        )
        if (known) {
            OutlineButton("恢复", onCut, compact = true)
        } else {
            Box(
                Modifier.size(44.dp).clickableNoRipple(onClick = onCut).semantics { contentDescription = "斩" },
                contentAlignment = Alignment.Center,
            ) { SealMark(armed = false, tint = AjlTheme.colors.ink) }
        }
    }
}

@Composable
internal fun LevelTag(level: String, fontSize: Int = 11) {
    Text(
        level,
        style = AjlTheme.type.metaSmall.copy(fontSize = fontSize.sp),
        color = AjlTheme.work.accent,
        modifier = Modifier.background(AjlTheme.work.soft, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** 讲解 / 语气 in light blue, 易错 in red: a filled label, then the text. */
@Composable
internal fun ColoredNote(label: String, text: String) {
    if (text.isBlank()) return
    val colors = AjlTheme.colors
    val warn = label == "易错"
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
            color = if (warn) colors.bad else colors.info,
            modifier = Modifier
                .padding(top = 3.dp)
                .background(if (warn) colors.badSoft else colors.infoSoft, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
        Text(text.trim(), style = AjlTheme.type.body, color = colors.ink, modifier = Modifier.weight(1f))
    }
}

/** 动词た形 ＋ って: 1.5px ink blocks, the last one (the pattern itself) in the work colour. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FormulaChips(parts: List<String>) {
    if (parts.isEmpty()) return
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { i, part ->
            if (i > 0) Text("＋", style = AjlTheme.type.meta.copy(fontSize = 16.sp), color = colors.ink3, modifier = Modifier.align(Alignment.CenterVertically))
            val last = i == parts.lastIndex
            Text(
                part,
                style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                color = if (last) work.accent else colors.ink,
                modifier = Modifier
                    .height(38.dp)
                    .background(if (last) work.soft else colors.surface, AjlShape.Panel)
                    .border(AjlStroke.Ink, colors.ink, AjlShape.Panel)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
    }
}

/** 名词/句子 + と仮定して → [名词/句子, と仮定して]. */
internal fun formulaParts(structure: String): List<String> =
    structure.split('+', '＋').map { it.trim() }.filter { it.isNotEmpty() }.takeIf { it.size > 1 }.orEmpty()

/** Where the pattern sits in its example (〜 and alternatives stripped); null when it doesn't appear verbatim. */
internal fun patternRange(example: String, pattern: String): IntRange? {
    val options = pattern.split('/', '／', '・', '、').map { it.trim().trim('〜', '～', '~', ' ') }.filter { it.isNotEmpty() }
    options.sortedByDescending { it.length }.forEach { option ->
        val start = example.indexOf(option)
        if (start >= 0) return start until start + option.length
    }
    return null
}

/**
 * The example line with its pattern fading in one character at a time a beat after the card
 * opens (and again whenever [key] changes, i.e. the card swipes to another pattern). Romaji and
 * kana sit over the words when those aids are on. Reduced motion shows it whole.
 */
@Composable
internal fun RevealLine(
    reading: LineReading,
    mark: IntRange?,
    key: Any,
    showRuby: Boolean,
    showRomaji: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = AjlTheme.colors.ink,
) {
    val reduced = rememberReducedMotion()
    val count = mark?.let { it.last - it.first + 1 } ?: 0
    val progress = remember(key) { Animatable(if (reduced || mark == null) count + 1f else 0f) }
    LaunchedEffect(key) {
        if (!reduced && mark != null) {
            delay(420)
            progress.animateTo(count + 1f, tween(130 * (count + 1), easing = LinearEasing))
        }
    }
    ReadingLineText(
        reading,
        mark,
        showRuby = showRuby,
        showRomaji = showRomaji,
        modifier = modifier,
        style = style,
        color = color,
        revealed = if (mark == null) null else progress.value,
    )
}
