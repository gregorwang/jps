package com.animejapaneselab.nativeapp.ui.screens.zougo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.VoiceWave
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.voicepack.rememberVoiceOptions
import com.animejapaneselab.nativeapp.ui.zougo.SegKind
import com.animejapaneselab.nativeapp.ui.zougo.ZgLine
import com.animejapaneselab.nativeapp.ui.zougo.ZgPart
import com.animejapaneselab.nativeapp.ui.zougo.ZgSeg
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.offset

/** 連濁 / 半浊 = work colour, 母音交替 = blue, sounds that come in (s, っ) = green, the blocking voiced sound = work, dashed. */
@Composable
internal fun segColor(kind: SegKind): Color = when (kind) {
    SegKind.Same -> Color.Unspecified
    SegKind.Voiced, SegKind.Already -> AjlTheme.work.accent
    SegKind.Vowel -> AjlTheme.colors.info
    SegKind.Insert, SegKind.Gem -> AjlTheme.colors.ok
    SegKind.Gone -> AjlTheme.colors.ink3
    SegKind.Bad -> AjlTheme.colors.bad
}

/**
 * Romaji with the pieces that changed coloured and underlined ([lit] = false draws them plain). A [SegKind.Gone]
 * ending is dotted before (look here) and struck through after.
 */
@Composable
internal fun SegText(segs: List<ZgSeg>, fontSize: TextUnit, lit: Boolean, modifier: Modifier = Modifier, base: Color = AjlTheme.colors.ink) {
    val ink3 = AjlTheme.colors.ink3
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        segs.forEach { seg ->
            val gone = seg.kind == SegKind.Gone
            val c = if (lit && !gone) segColor(seg.kind) else Color.Unspecified
            val marked = c != Color.Unspecified
            val dashed = seg.kind == SegKind.Already
            Text(
                seg.text,
                style = AjlTheme.type.meta.copy(
                    fontSize = fontSize,
                    letterSpacing = 0.6.sp,
                    fontWeight = if (marked) FontWeight.SemiBold else FontWeight.Normal,
                    textDecoration = if (gone && lit) TextDecoration.LineThrough else null,
                ),
                color = when {
                    gone && lit -> ink3
                    marked -> c
                    else -> base
                },
                modifier = when {
                    gone && !lit -> Modifier.drawBehind {
                        val y = size.height - 1.dp.toPx()
                        drawLine(ink3, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 2.5.dp.toPx())))
                    }
                    !marked -> Modifier
                    else -> Modifier.drawBehind {
                        val y = size.height - 1.dp.toPx()
                        drawLine(
                            c, Offset(0f, y), Offset(size.width, y), 2.dp.toPx(),
                            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
                        )
                    }
                },
            )
        }
    }
}

/** One part of a compound: romaji on top, kana, then the kanji (the App's reading order). */
@Composable
internal fun PartTile(part: ZgPart, lit: Boolean, modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val colors = AjlTheme.colors
    val wide = part.kanji.length > 2
    MangaPanel(if (wide) modifier.height(size).widthIn(min = size).padding(horizontal = 0.dp) else modifier.size(size)) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = if (wide) 12.dp else 0.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SegText(part.romaji, 14.sp, lit, base = colors.ink2)
            Text(part.kana, style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 18.sp), color = colors.ink3)
            Text(part.kanji, style = AjlTheme.type.jpDisplay.copy(fontSize = 38.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold), color = colors.ink)
        }
    }
}

/** A small 1px chip in a kind's colour (連濁　k → g). */
@Composable
internal fun KindChip(text: String, kind: SegKind) {
    val c = segColor(kind).takeIf { it != Color.Unspecified && kind != SegKind.Already } ?: AjlTheme.colors.ink2
    val border = if (kind == SegKind.Already) AjlTheme.colors.line2 else c
    Text(
        text,
        style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 22.sp),
        color = c,
        maxLines = 1,
        modifier = Modifier.border(AjlStroke.Hair, border, RoundedCornerShape(11.dp)).padding(horizontal = 9.dp),
    )
}

/**
 * The line under a word: 声波 = play (原声 when the line has one, else エミリア / TTS by the global
 * voice choice), romaji, the line with the word marked, Chinese, and where it comes from.
 */
@Composable
internal fun LineCard(line: ZgLine, audio: LessonAudioController, ttsWorkerUrl: String, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val voice = rememberVoiceOptions(line.ja, hasSource = line.audioUrl.isNotEmpty())
    val source = line.audioUrl.takeIf { it.isNotEmpty() }?.let {
        PromptAudio.Source(it, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = line.ja)
    }
    val cue = voice.cue(source, line.ja)
    Column(
        modifier
            .fillMaxWidth()
            .border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp))
            .background(colors.surface, RoundedCornerShape(4.dp))
            .padding(start = 6.dp, end = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoiceWave(playing = audio.isSounding(cue), onClick = { audio.toggle(cue, ttsWorkerUrl) }, synthetic = source == null)
            Spacer(Modifier.weight(1f))
            Text(if (line.fromAnime) "原作" else "例句", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
        }
        Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(line.romaji, style = AjlTheme.type.meta.copy(fontSize = 12.sp, lineHeight = 17.sp), color = colors.ink3)
            Text(
                buildAnnotatedString {
                    append(line.pre)
                    withStyle(SpanStyle(color = work.accent, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) { append(line.target) }
                    append(line.post)
                },
                style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 28.sp),
                color = colors.ink,
            )
            Text(line.zh, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink2)
        }
    }
}

/** Session header for a 造語 課: × , the book and 課 over the title, a counter, then the progress line. */
@Composable
internal fun ZougoHeader(eyebrow: String, title: String, counter: String, progress: Float, onClose: () -> Unit) {
    val colors = AjlTheme.colors
    TopBar(
        nav = TopBarNav.Close,
        onNav = onClose,
        center = {
            Column(Modifier.weight(1f)) {
                Text(eyebrow, style = AjlTheme.type.meta.copy(fontSize = 11.sp, letterSpacing = 0.4.sp), color = colors.ink3, maxLines = 1)
                Text(title, style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold), color = colors.ink, maxLines = 1)
            }
        },
        actions = { Text(counter, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3, modifier = Modifier.padding(end = 8.dp)) },
    )
    ProgressLine(progress, Modifier.fillMaxWidth().padding(horizontal = 20.dp))
}

/** The big framed panel the compound / word appears in, with its screentone corner. */
@Composable
internal fun StagePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    MangaPanel(modifier.fillMaxWidth()) {
        Screentone(
            Modifier.align(Alignment.TopEnd).offset(x = 36.dp, y = (-20).dp).size(170.dp, 70.dp).rotate(-12f),
            color = AjlTheme.work.tone(0.26f),
        )
        Column(Modifier.align(Alignment.Center).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

/** A word heading: romaji, kana, then the word (tap = hear it). */
@Composable
internal fun WordHead(romaji: String, kana: String, word: String, onSpeak: () -> Unit, size: TextUnit = 34.sp, center: Boolean = false) {
    val colors = AjlTheme.colors
    Column(horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
        if (romaji.isNotBlank()) Text(romaji, style = AjlTheme.type.meta.copy(fontSize = 13.sp, letterSpacing = 0.6.sp), color = colors.ink3)
        Text(kana, style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 18.sp), color = AjlTheme.work.accent)
        Text(
            word,
            style = AjlTheme.type.jpDisplay.copy(fontSize = size, lineHeight = size * 1.24f, fontWeight = FontWeight.Bold),
            color = colors.ink,
            modifier = Modifier.clickableNoRipple(onSpeak),
        )
    }
}

@Composable
internal fun Gap(h: Dp) = Spacer(Modifier.height(h).width(1.dp))

@Composable
internal fun Divider() = Box(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(AjlTheme.colors.line))
