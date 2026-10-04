package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.VoiceWave
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.screens.zougo.ZougoHeader
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.voicepack.rememberVoiceOptions
import com.animejapaneselab.nativeapp.ui.zougo.ZgLine

/** The frame every 活用 play shares: header with progress, the scrolling body, and (when set) the one ink button. */
@Composable
internal fun KySitting(
    eyebrow: String,
    title: String,
    counter: String,
    progress: Float,
    onClose: () -> Unit,
    button: String?,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
    arrow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        ZougoHeader(eyebrow = eyebrow, title = title, counter = counter, progress = progress, onClose = onClose)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        if (button != null) {
            InkButton(text = button, onClick = onButton, trailingArrow = arrow, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
        }
    }
}

/** States of a tile you pick. */
internal enum class TileState { Idle, Right, Wrong, Dim, Current }

/** A 1.5px ink tile (4dp corners) that judges: right = ink fill, wrong = red outline. */
@Composable
internal fun PickTile(
    state: TileState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(fg: Color) -> Unit,
) {
    val colors = AjlTheme.colors
    val bgTarget = when (state) {
        TileState.Right, TileState.Current -> colors.ink
        else -> colors.surface
    }
    val bg by animateColorAsState(bgTarget, tween(200), label = "tile-bg")
    val fg = when (state) {
        TileState.Right, TileState.Current -> colors.bg
        TileState.Wrong -> colors.bad
        TileState.Dim -> colors.ink2
        TileState.Idle -> colors.ink
    }
    val border = when (state) {
        TileState.Wrong -> colors.bad
        TileState.Dim -> colors.line2
        else -> colors.ink
    }
    Column(
        modifier
            .heightIn(min = 48.dp)
            .background(bg, RoundedCornerShape(4.dp))
            .border(if (state == TileState.Dim) AjlStroke.Hair else AjlStroke.Ink, border, RoundedCornerShape(4.dp))
            .clickableNoRipple(onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) { content(fg) }
}

/** ✓ / 正解 line in mono. */
@Composable
internal fun Verdict(text: String, ok: Boolean) {
    Text(text, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = if (ok) AjlTheme.colors.ok else AjlTheme.colors.bad)
}

/**
 * A line with a gap: before [filled] the gap is a dashed box, after it shows [fill] in the work colour.
 * The wave plays [line] (原声 when it has one); [label] replaces 原作 / 例句 (换词: "换了一个词").
 */
@Composable
internal fun SlotLine(
    line: ZgLine,
    fill: String,
    filled: Boolean,
    audio: LessonAudioController,
    ttsWorkerUrl: String,
    romaji: String = line.romaji,
    zh: String = line.zh,
    label: String? = null,
    context: String = "",
    big: Boolean = true,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val voice = rememberVoiceOptions(line.ja, hasSource = line.audioUrl.isNotEmpty())
    val source = line.audioUrl.takeIf { it.isNotEmpty() }?.let {
        PromptAudio.Source(it, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = line.ja)
    }
    val cue = voice.cue(source, line.ja)
    Column(
        Modifier
            .fillMaxWidth()
            .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
            .background(colors.surface, RoundedCornerShape(4.dp))
            .padding(start = 6.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoiceWave(playing = audio.isSounding(cue), onClick = { audio.toggle(cue, ttsWorkerUrl) }, synthetic = source == null)
            Spacer(Modifier.weight(1f))
            Text(label ?: if (line.fromAnime) "原作" else "例句", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = if (label != null) work.accent else colors.ink3)
        }
        Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (context.isNotBlank()) Text(context, style = AjlTheme.type.caption, color = colors.ink2)
            if (filled && romaji.isNotBlank()) Text(romaji, style = AjlTheme.type.meta.copy(fontSize = 12.sp, lineHeight = 17.sp), color = colors.ink3)
            Text(
                buildAnnotatedString {
                    append(line.pre)
                    if (filled) {
                        withStyle(SpanStyle(color = work.accent, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) { append(fill) }
                    } else {
                        withStyle(SpanStyle(color = colors.ink3, background = colors.sunken)) { append("　？　") }
                    }
                    append(line.post)
                },
                style = AjlTheme.type.jpBody.copy(fontSize = if (big) 21.sp else 18.sp, lineHeight = if (big) 32.sp else 28.sp, fontWeight = FontWeight.Medium),
                color = colors.ink,
            )
            Text(zh, style = AjlTheme.type.caption, color = colors.ink2)
        }
    }
}
