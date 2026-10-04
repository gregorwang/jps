package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.NoteText
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.katsuyou.KyGlLine
import com.animejapaneselab.nativeapp.ui.katsuyou.KyGlSeg
import com.animejapaneselab.nativeapp.ui.katsuyou.KyGlance
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.screens.zougo.ZougoHeader
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.delay

/**
 * 課前の一眼, the newer layout: the hero line in the panel (before, ↓, after — the after slides in a beat
 * later), the beats numbered under it, then a pair or two to look at again. Romaji sits over each block.
 */
@Composable
internal fun GlanceScreen(
    g: KyGlance,
    go: String,
    eyebrow: String,
    title: String,
    groups: Int,
    onClose: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onClose)
    val colors = AjlTheme.colors
    val reduced = rememberReducedMotion()
    var shown by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) { delay(420); shown = true }
    val reveal by animateFloatAsState(if (shown) 1f else 0f, MotionTokens.decelerate(360, reduced), label = "glance-after")

    Column(modifier.fillMaxSize().background(colors.bg)) {
        ZougoHeader(eyebrow = eyebrow, title = title, counter = "", progress = 0f, onClose = onClose)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        ) {
            Text("課前の一眼 · 10 秒", style = AjlTheme.type.meta.copy(fontSize = 11.sp, letterSpacing = 0.6.sp), color = colors.ink3)
            Spacer(Modifier.height(16.dp))
            HeroPanel(g.hero, reveal)
            Spacer(Modifier.height(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                g.beats.forEachIndexed { i, beat ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "${i + 1}",
                            style = AjlTheme.type.meta.copy(fontSize = 12.sp, lineHeight = 25.sp),
                            color = AjlTheme.work.accent,
                            modifier = Modifier.width(12.dp),
                        )
                        NoteText(beat, style = BeatStyle(), color = colors.ink2)
                    }
                }
            }
            if (g.pairs.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text("再看${if (g.pairs.size > 1) " ${g.pairs.size} 个" else "一个"}", style = AjlTheme.type.meta.copy(fontSize = 11.sp, letterSpacing = 0.6.sp), color = colors.ink3)
                Spacer(Modifier.height(4.dp))
                g.pairs.forEach { p ->
                    PairRow(p)
                    Spacer(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(colors.line))
                }
            }
        }
        InkButton(
            text = go,
            onClick = onStart,
            caption = "$groups 个",
            trailingArrow = true,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun BeatStyle() = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 25.sp, letterSpacing = 0.3.sp)

@Composable
private fun HeroPanel(hero: KyGlLine, reveal: Float) {
    val colors = AjlTheme.colors
    MangaPanel(Modifier.fillMaxWidth()) {
        // The tone sits in the bottom-left corner, under the padding, so it never runs over the words.
        Screentone(
            Modifier.align(Alignment.BottomStart).offset(x = (-28).dp, y = 26.dp).size(150.dp, 50.dp).rotate(-12f),
            color = AjlTheme.work.tone(0.22f),
        )
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (hero.to.isEmpty()) {
                GlLineView(hero.from, size = heroSize(hero.from), center = true)
            } else {
                GlLineView(hero.from, size = (heroSize(hero.from).value * 0.72f).coerceAtLeast(17f).sp, center = true, quiet = true)
                Text("↓", style = AjlTheme.type.meta.copy(fontSize = 16.sp), color = colors.ink3, modifier = Modifier.padding(vertical = 8.dp))
                GlLineView(
                    hero.to,
                    size = heroSize(hero.to),
                    center = true,
                    modifier = Modifier.graphicsLayer { alpha = reveal; translationY = (1f - reveal) * 10.dp.toPx() },
                )
            }
            if (hero.zh.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                NoteText(
                    hero.zh,
                    style = AjlTheme.type.body.copy(fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.3.sp, textAlign = TextAlign.Center),
                    color = colors.ink2,
                    modifier = Modifier.graphicsLayer { alpha = if (hero.to.isEmpty()) 1f else reveal },
                )
            }
        }
    }
}

@Composable
private fun PairRow(p: KyGlLine) {
    val colors = AjlTheme.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GlLineView(p.from, size = 18.sp, quiet = p.to.isNotEmpty())
        if (p.to.isNotEmpty()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("→", style = AjlTheme.type.meta.copy(fontSize = 14.sp), color = colors.ink3, modifier = Modifier.padding(end = 8.dp, bottom = 6.dp))
                GlLineView(p.to, size = 20.sp)
            }
        }
        if (p.zh.isNotBlank()) NoteText(p.zh, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp), color = colors.ink3)
    }
}

/** Short lines read big; a sentence drops to the 20sp reading size so it wraps by blocks, not mid-word. */
private fun heroSize(segs: List<KyGlSeg>): TextUnit {
    val n = segs.sumOf { it.text.length }
    return when {
        n <= 6 -> 32.sp
        n <= 10 -> 26.sp
        n <= 16 -> 22.sp
        else -> 20.sp
    }
}

/**
 * A line in blocks, romaji over each block, wrapping between blocks. [quiet] = the "before" line (ink2,
 * lighter). insert = ok green underlined, mark = work colour underlined, gone = struck through, scope = a
 * sunken band under the stretch.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GlLineView(segs: List<KyGlSeg>, size: TextUnit, modifier: Modifier = Modifier, center: Boolean = false, quiet: Boolean = false) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    val roSize = if (size.value >= 22f) 11.sp else 10.sp
    val fontSize = size
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp, if (center) Alignment.CenterHorizontally else Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        segs.forEach { s ->
            val base = if (quiet) colors.ink2 else colors.ink
            val fg: Color = when (s.kind) {
                "insert" -> colors.ok
                "mark" -> work
                "gone" -> colors.ink3
                else -> base
            }
            val line: Color? = when (s.kind) {
                "insert" -> colors.ok
                "mark" -> work
                else -> null
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (s.kind == "scope") Modifier.background(colors.sunken, RoundedCornerShape(2.dp)).padding(horizontal = 3.dp) else Modifier,
            ) {
                Text(s.romaji, style = AjlTheme.type.meta.copy(fontSize = roSize, lineHeight = (roSize.value + 5).sp, letterSpacing = 0.2.sp), color = if (line != null) fg.copy(alpha = 0.8f) else colors.ink3, maxLines = 1, softWrap = false)
                Text(
                    s.text,
                    style = AjlTheme.type.jpDisplay.copy(
                        fontSize = size,
                        lineHeight = (size.value * 1.4f).sp,
                        fontWeight = when {
                            quiet -> FontWeight.Medium
                            s.kind == "insert" || s.kind == "mark" -> FontWeight.Black
                            else -> FontWeight.Bold
                        },
                        letterSpacing = if (size.value <= 20f) 0.6.sp else 0.sp,
                        textDecoration = if (s.kind == "gone") TextDecoration.LineThrough else null,
                    ),
                    color = fg,
                    modifier = if (line != null) Modifier.drawBehind {
                        val y = this.size.height / 2f + fontSize.toPx() * 0.6f
                        drawLine(line, Offset(0f, y), Offset(this.size.width, y), strokeWidth = 2.dp.toPx())
                    } else Modifier,
                )
            }
        }
    }
}
