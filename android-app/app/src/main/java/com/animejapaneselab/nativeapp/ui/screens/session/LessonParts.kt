package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.buildExternalQuestionPrompt
import com.animejapaneselab.nativeapp.domain.AnswerFeedback
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackState
import com.animejapaneselab.nativeapp.ui.design.CharacterRef
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.FeedbackSheet
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.PortraitPanel
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.speechLines
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

// Shared chrome of the lesson session (task package D1): top bar, question scaffold, bottom
// action bar, 原声 chip, scene panel and the feedback sheet with the character's reaction.

/** What every question composable gets from [LessonSessionScreen]. */
internal class LessonQuestionEnv(
    val node: LessonNode,
    val feedback: AnswerFeedback?,
    val eyebrow: String,
    val speaker: String?,
    val character: CharacterRef?,
    /** Height of the feedback sheet once answered, so the content can scroll above it. */
    val sheetInset: Dp,
    val playback: AudioPlaybackState,
    val onPlay: (PromptAudio) -> Unit,
    val onSpeak: (String) -> Unit,
    val onSubmit: (String) -> Unit,
    val onWrongTap: () -> Unit,
    /** 跳过 on the bottom bar's left; null hides it. */
    val onSkip: (() -> Unit)? = null,
) {
    val answered: Boolean get() = feedback != null
}

/** × · continuous work-colour progress line · mono "7/12". */
@Composable
internal fun LessonTopBar(
    position: Int,
    total: Int,
    fraction: Float,
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
                progress = fraction,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentDescription = "本组进度 $position/$total",
            )
            Text(
                "$position/$total",
                style = AjlTheme.type.meta,
                color = AjlTheme.colors.ink3,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            )
        },
    )
}

/**
 * One question: mono eyebrow, Chinese heading, content, and the bottom bar while unanswered.
 * Once answered the bar is gone and a spacer the height of the feedback sheet keeps the last
 * content reachable above it.
 */
@Composable
internal fun LessonQuestionScaffold(
    env: LessonQuestionEnv,
    modifier: Modifier = Modifier,
    heading: String? = LessonRules.heading(env.node),
    scroll: ScrollState = remember { ScrollState(0) },
    bottomBar: (@Composable () -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduced = rememberReducedMotion()
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) appear.animateTo(1f, tween(MotionTokens.Dur.State, easing = MotionTokens.Ease.Standard))
    }
    LaunchedEffect(env.answered, env.sheetInset) {
        if (env.answered && env.sheetInset > 0.dp) {
            if (reduced) scroll.scrollTo(scroll.maxValue) else scroll.animateScrollTo(scroll.maxValue)
        }
    }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .graphicsLayer {
                        alpha = appear.value
                        translationY = (1f - appear.value) * 8.dp.toPx()
                    }
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (env.eyebrow.isNotBlank()) Eyebrow(env.eyebrow)
                    if (!heading.isNullOrBlank()) {
                        Text(
                            heading,
                            style = AjlTheme.type.title,
                            color = AjlTheme.colors.ink,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                }
                content()
                Spacer(Modifier.height(if (env.answered) env.sheetInset + 16.dp else 24.dp))
            }
            if (!env.answered && bottomBar != null) bottomBar()
        }
        overlay?.invoke(this)
    }
}

/** Bottom bar: an optional quiet action on the left + the one ink button. */
@Composable
internal fun LessonActionBar(
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    caption: String? = null,
    progress: Float? = null,
    quietLabel: String? = null,
    onQuiet: (() -> Unit)? = null,
    quietEnabled: Boolean = true,
) {
    Column(modifier.fillMaxWidth().background(AjlTheme.colors.bg)) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (quietLabel != null && onQuiet != null) QuietButton(quietLabel, onQuiet, enabled = quietEnabled)
            InkButton(
                primaryLabel,
                onPrimary,
                Modifier.weight(1f),
                enabled = enabled,
                loading = loading,
                caption = caption,
                progress = progress,
                height = if (caption != null) 56.dp else 52.dp,
            )
        }
    }
}

/** 「▶ 原声 0:03」— 36dp pill with a 1.5px ink outline; mono label. */
@Composable
internal fun AudioChip(
    audio: PromptAudio,
    playback: AudioPlaybackState,
    onPlay: (PromptAudio) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (audio == PromptAudio.None) return
    val colors = AjlTheme.colors
    val label = when (audio) {
        is PromptAudio.Source -> "原声"
        else -> "朗读"
    }
    val shape = RoundedCornerShape(18.dp)
    // While the line is voiced the chip turns ink, bars bounce and 効果線 burst from its corner.
    val speaking = playback.phase == AudioPlaybackPhase.Playing
    val fill by animateColorAsState(if (speaking) colors.ink else colors.surface, tween(MotionTokens.Dur.State), label = "chip-fill")
    val content by animateColorAsState(if (speaking) colors.onInk else colors.ink, tween(MotionTokens.Dur.State), label = "chip-ink")
    Row(
        modifier = modifier
            .height(36.dp)
            .speechLines(speaking, AjlTheme.work.accent)
            .clip(shape)
            .background(fill)
            .border(AjlStroke.Ink, colors.ink, shape)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) { onPlay(audio) }
            .semantics { contentDescription = audio.label.ifBlank { "播放" } }
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when (playback.phase) {
            AudioPlaybackPhase.Loading -> LoadingDots(delayMillis = 0, modifier = Modifier.size(width = 18.dp, height = 14.dp))
            AudioPlaybackPhase.Playing -> VoiceBars(active = true, color = content, modifier = Modifier.padding(horizontal = 1.dp))
            else -> Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(label, style = AjlTheme.type.meta.copy(fontSize = AjlTheme.type.meta.fontSize * 1.09f), color = content)
    }
}

/**
 * 場面 — the shot above a dialogue box: manga panel with the character's portrait on a
 * screentone, 原声 chip in the top-right corner.
 */
@Composable
internal fun ScenePanel(
    character: CharacterRef?,
    audio: PromptAudio,
    playback: AudioPlaybackState,
    onPlay: (PromptAudio) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    Box(modifier.fillMaxWidth().height(height)) {
        PortraitPanel(character, Modifier.matchParentSize(), speaking = playback.phase == AudioPlaybackPhase.Playing)
        AudioChip(
            audio,
            playback,
            onPlay,
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
        )
    }
}

/** A row of word tiles / chips that wraps. */
@Composable
internal fun TileFlow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = { content() },
    )
}

/**
 * The answer panel: the character reacts (avatar + bubble), verdict · explanation, and one ink
 * 继续. Wrong answers add a quiet "复制题目" and the mono note that it went into the 错题本.
 */
@Composable
internal fun LessonFeedback(
    node: LessonNode,
    feedback: AnswerFeedback,
    character: CharacterRef?,
    isLast: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable(node.id) { mutableStateOf(false) }
    var leaving by remember(node.id) { mutableStateOf(false) }
    val reaction = remember(node.id, feedback.correct) { LessonRules.reaction(feedback.correct, node) }
    FeedbackSheet(
        correct = feedback.correct,
        onContinue = {
            if (!leaving) {
                leaving = true
                onContinue()
            }
        },
        modifier = modifier,
        character = character,
        line = reaction.ja,
        lineGloss = reaction.zh,
        verdict = if (feedback.correct) "正确" else "不太对",
        explanation = LessonRules.explanation(node, feedback),
        continueLabel = if (isLast) "看结算" else "继续",
        extra = if (feedback.correct) {
            null
        } else {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlineButton(
                        text = if (copied) "已复制题目" else "复制题目",
                        onClick = {
                            clipboard.setText(AnnotatedString(buildExternalQuestionPrompt(node, feedback)))
                            copied = true
                        },
                        compact = true,
                        leadingIcon = Icons.Rounded.ContentCopy,
                    )
                    Spacer(Modifier.weight(1f))
                    Text("已记入错题本", style = AjlTheme.type.meta, color = AjlTheme.colors.ink3)
                }
            }
        },
    )
}
