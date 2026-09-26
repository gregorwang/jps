package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import com.animejapaneselab.nativeapp.ui.design.Avatar
import com.animejapaneselab.nativeapp.ui.design.BubbleShape
import com.animejapaneselab.nativeapp.ui.design.EmphasisText
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.SpeechBubble
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

// Shared building blocks of the 読空気 and 基礎 answering screens (task package D2). Everything
// here is assembled from ui/design; the few private stand-ins are listed in design/v3-requests/D2.md.

/** Session top bar: × · continuous progress line · mono "3/10". */
@Composable
internal fun ReadAirSessionTopBar(
    progress: ReadAirProgress,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopBar(
        modifier = modifier,
        nav = TopBarNav.Close,
        onNav = onExit,
        navContentDescription = "退出训练",
        center = {
            ProgressLine(
                progress = progress.fraction,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentDescription = "本组进度 ${progress.position}/${progress.total}",
            )
            Text(
                "${progress.position}/${progress.total}",
                style = AjlTheme.type.meta,
                color = AjlTheme.colors.ink3,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            )
        },
    )
}

/**
 * The scene as a manga panel: 1.5px ink frame, a strip of work-colour screentone on the right,
 * one [ReadAirLine] per line of dialogue.
 */
@Composable
internal fun ReadAirScenePanel(
    lines: List<ReadAirSceneLine>,
    modifier: Modifier = Modifier,
    emphasisFor: (ReadAirSceneLine) -> List<IntRange> = { emptyList() },
    emphasisVisible: Boolean = false,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val work = AjlTheme.work
    MangaPanel(modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .width(120.dp)
                    .fillMaxHeight()
                    .screentone(work.tone(0.18f), angleDegrees = 0f, spacing = 6.dp, dotRadius = 1.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            lines.forEach { line ->
                ReadAirLine(line, emphasis = emphasisFor(line), emphasisVisible = emphasisVisible)
            }
            footer?.invoke(this)
        }
    }
}

/** Bubble for the right-hand speaker: the point sits top-right, towards their avatar. */
private val MirroredBubbleShape: Shape =
    RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)

/**
 * A line of dialogue. Start: avatar · bubble; End: bubble · avatar (mirrored); Narration: a quiet
 * serif line with no avatar. The target speaker's avatar gets the work-colour ring.
 */
@Composable
internal fun ReadAirLine(
    line: ReadAirSceneLine,
    modifier: Modifier = Modifier,
    emphasis: List<IntRange> = emptyList(),
    emphasisVisible: Boolean = false,
) {
    val colors = AjlTheme.colors
    when (line.side) {
        ReadAirLineSide.Narration -> Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (emphasis.isEmpty()) {
                Text(line.ja, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
            } else {
                EmphasisText(
                    line.ja,
                    emphasis,
                    style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp),
                    emphasisVisible = emphasisVisible,
                )
            }
            if (line.zh.isNotBlank()) Text(line.zh, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
        }

        ReadAirLineSide.Start, ReadAirLineSide.End -> {
            val end = line.side == ReadAirLineSide.End
            val character = WorkIdentity.character(line.speaker)
            Row(
                modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, if (end) Alignment.End else Alignment.Start),
                verticalAlignment = Alignment.Top,
            ) {
                if (!end) Avatar(character, size = 40.dp, highlighted = line.isTarget)
                ReadAirBubble(
                    text = line.ja,
                    gloss = line.zh.takeIf { it.isNotBlank() },
                    unfinished = line.unfinished,
                    shape = if (end) MirroredBubbleShape else BubbleShape,
                    emphasis = emphasis,
                    emphasisVisible = emphasisVisible,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (end) Avatar(character, size = 40.dp, highlighted = line.isTarget)
            }
        }
    }
}

/**
 * [SpeechBubble] when nothing is emphasised; with emphasis a private copy of its frame around an
 * [EmphasisText] (SpeechBubble has no content slot — requested in D2.md).
 */
@Composable
private fun ReadAirBubble(
    text: String,
    gloss: String?,
    unfinished: Boolean,
    shape: Shape,
    emphasis: List<IntRange>,
    emphasisVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (emphasis.isEmpty()) {
        SpeechBubble(text, modifier = modifier, gloss = gloss, unfinished = unfinished, shape = shape)
        return
    }
    val colors = AjlTheme.colors
    val ink = colors.ink
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline,
                    ink,
                    style = Stroke(
                        width = AjlStroke.Ink.toPx() * 2,
                        pathEffect = if (unfinished) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null,
                    ),
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        EmphasisText(
            text,
            emphasis,
            style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp),
            emphasisVisible = emphasisVisible,
        )
        if (gloss != null) Text(gloss, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
    }
}

/** The Chinese question under the panel (澪真正想说的是？). */
@Composable
internal fun ReadAirQuestion(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = AjlTheme.type.title.copy(fontSize = 18.sp, lineHeight = 26.sp),
        color = AjlTheme.colors.ink,
        modifier = modifier.semantics { heading() },
    )
}

/** Bottom bar before checking: an optional quiet action on the left + the one ink 检查. */
@Composable
internal fun ReadAirCheckBar(
    checkEnabled: Boolean,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
    quietLabel: String? = null,
    quietEnabled: Boolean = true,
    onQuiet: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().background(AjlTheme.colors.bg)) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (quietLabel != null && onQuiet != null) QuietButton(quietLabel, onQuiet, enabled = quietEnabled)
            InkButton("检查", onCheck, Modifier.weight(1f), enabled = checkEnabled, height = 48.dp)
        }
    }
}

/** A labelled note inside the feedback sheet (证据 / 学习点). */
@Composable
internal fun ReadAirNote(label: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Eyebrow(label)
        Text(body, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = AjlTheme.colors.ink2)
    }
}

/** Notes block in the sheet, capped in height so the sheet never covers the whole screen. */
@Composable
internal fun ReadAirNotes(notes: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    if (notes.isEmpty()) return
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        notes.forEach { (label, body) -> ReadAirNote(label, body) }
    }
}

/** Loading / empty / error body under a plain × top bar. */
@Composable
internal fun ReadAirQuietState(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    text: String = "",
    gloss: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        TopBar(nav = TopBarNav.Close, onNav = onExit, navContentDescription = "退出训练")
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                LoadingDots()
            } else {
                Text(text, style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp), color = AjlTheme.colors.ink3)
                if (gloss != null) {
                    Text(gloss, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(16.dp))
                content?.invoke(this)
            }
        }
    }
}

/** One noted line on the つづく screen (a missed question) with a mono tag on the right. */
data class ReadAirNotedLine(val text: String, val japanese: Boolean, val tag: String)

/**
 * つづく — the end of a set, simplified from XTsuzuku: a screentone header with the tally and the
 * big serif つづく sliding in glyph by glyph (40dp, 240ms, 50ms apart); the missed lines; a quiet
 * action and one ink action. No XP, no rolling numbers.
 */
@Composable
internal fun ReadAirTsuzuku(
    eyebrow: String,
    tally: String,
    meta: String,
    noted: List<ReadAirNotedLine>,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    notedTitle: String = "这次记下的 ${noted.size} 句",
    quietLabel: String? = null,
    quietEnabled: Boolean = true,
    onQuiet: (() -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(modifier.fillMaxSize().background(colors.bg)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp),
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .screentone(work.tone(0.28f), angleDegrees = 0f, spacing = 7.dp, dotRadius = 1.2.dp),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(0.1f to colors.bg, 0.7f to colors.bg.copy(alpha = 0f))),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow(eyebrow, Modifier.weight(1f))
                IconButton44(Icons.Rounded.Close, "关闭", onClose, tint = colors.ink2)
            }
            Column(
                Modifier.padding(start = 20.dp, top = 76.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(tally, style = AjlTheme.type.body, color = colors.ink2)
                if (meta.isNotBlank()) Text(meta, style = AjlTheme.type.meta.copy(fontSize = 13.sp, lineHeight = 18.sp), color = colors.ink3)
            }
            TsuzukuTitle(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 18.dp),
            )
            Hairline(Modifier.align(Alignment.BottomCenter), strong = true)
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (noted.isNotEmpty()) {
                Text(notedTitle, style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                Column {
                    Hairline()
                    noted.forEach { line ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                line.text,
                                style = if (line.japanese) AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 22.sp) else AjlTheme.type.body,
                                color = colors.ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text(line.tag, style = AjlTheme.type.meta, color = colors.ink3)
                        }
                        Hairline()
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth()) {
            Hairline()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (quietLabel != null && onQuiet != null) QuietButton(quietLabel, onQuiet, enabled = quietEnabled)
                InkButton(primaryLabel, onPrimary, Modifier.weight(1f), height = 48.dp)
            }
        }
    }
}

/** 「つづく」 in 64sp serif; each glyph slides in from 40dp right (MOTION §3-06, simplified). */
@Composable
private fun TsuzukuTitle(modifier: Modifier = Modifier) {
    val text = "つづく"
    val reduced = rememberReducedMotion()
    val start = 100
    val stagger = 50
    val duration = 240
    val total = start + stagger * (text.length - 1) + duration
    val clock = remember(reduced) { Animatable(if (reduced) total.toFloat() else 0f) }
    LaunchedEffect(reduced) {
        if (!reduced) clock.animateTo(total.toFloat(), tween(total, easing = LinearEasing))
    }
    val shift = with(LocalDensity.current) { 40.dp.toPx() }
    Row(modifier.clearAndSetSemantics { contentDescription = text }) {
        text.forEachIndexed { index, ch ->
            Text(
                ch.toString(),
                style = AjlTheme.type.jpDisplay.copy(fontSize = 64.sp, lineHeight = 68.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = AjlTheme.colors.ink,
                modifier = Modifier.graphicsLayer {
                    val t = ((clock.value - start - index * stagger) / duration).coerceIn(0f, 1f)
                    val eased = MotionTokens.Ease.Decelerate.transform(t)
                    alpha = eased
                    translationX = (1f - eased) * shift
                },
            )
        }
    }
}
