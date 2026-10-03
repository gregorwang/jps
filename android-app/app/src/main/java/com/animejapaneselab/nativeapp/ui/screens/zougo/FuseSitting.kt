package com.animejapaneselab.nativeapp.ui.screens.zougo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.OptionState
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.zougo.ZgFuse

/**
 * 拼合台: for each compound, two part tiles and "合起来怎么读？" (or the item's own [ZgFuse.ask]) with three
 * romaji options. Picking one slides the tiles together and pops the compound with the changed sounds lit;
 * the rule, the kind chips and a line take the options' place. The last one ends the run ([onDone] = wrong
 * ones). Shared by 造語 and the 活用 books ([key] keeps the place per 課; [onAnswer] records a judgement).
 */
@Composable
internal fun FuseSitting(
    items: List<ZgFuse>,
    key: String,
    eyebrow: String,
    title: String,
    settings: LabSettings,
    onClose: () -> Unit,
    onAnswer: (ZgFuse, Boolean) -> Unit,
    onDone: (right: Int, missed: List<ZgFuse>) -> Unit,
    modifier: Modifier = Modifier,
    lastLabel: String = "完成",
) {
    BackHandler(onBack = onClose)
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var picked by rememberSaveable(key) { mutableIntStateOf(-1) }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<String>() }
    val item = items.getOrNull(index) ?: return
    val revealed = picked >= 0

    LaunchedEffect(index, revealed) {
        if (revealed && settings.autoSpeak) audio.speakText(item.word, settings.ttsWorkerUrl)
    }

    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        ZougoHeader(
            eyebrow = eyebrow,
            title = title,
            counter = "${index + 1} / ${items.size}",
            progress = (index + if (revealed) 1 else 0).toFloat() / items.size,
            onClose = onClose,
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FuseStage(item, revealed, onSpeak = { audio.speakText(item.word, settings.ttsWorkerUrl) })
            if (!revealed) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.options.forEachIndexed { i, option ->
                        OptionRow(
                            text = option,
                            state = OptionState.Default,
                            leading = "ABC".getOrNull(i)?.toString(),
                            onClick = {
                                val ok = i == item.answer
                                feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                                onAnswer(item, ok)
                                if (ok) right++ else missed.add(item.word)
                                picked = i
                            },
                        )
                    }
                }
            } else {
                FuseExplain(item, picked, audio, settings.ttsWorkerUrl)
            }
        }
        if (revealed) {
            val last = index == items.lastIndex
            InkButton(
                text = if (last) lastLabel else "下一组",
                onClick = {
                    audio.stop()
                    if (last) onDone(right, items.filter { it.word in missed })
                    else { index++; picked = -1 }
                },
                trailingArrow = !last,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

/** The two tiles over the result panel; [revealed] slides them in and pops the compound. */
@Composable
internal fun FuseStage(item: ZgFuse, revealed: Boolean, onSpeak: () -> Unit, showResult: Boolean = true) {
    val reduced = rememberReducedMotion()
    val shift by animateDpAsState(if (revealed && !reduced) 12.dp else 0.dp, tween(420), label = "fuse-shift")
    val tilt by animateFloatAsState(if (revealed && !reduced) 2f else 0f, tween(420), label = "fuse-tilt")
    val dim by animateFloatAsState(if (revealed) 0.5f else 1f, tween(300), label = "fuse-dim")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().height(118.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            PartTile(item.a, lit = revealed, modifier = Modifier.offset(x = shift).rotate(-tilt).alpha(dim))
            Text("+", style = AjlTheme.type.meta.copy(fontSize = 22.sp), color = AjlTheme.colors.ink3, modifier = Modifier.alpha(if (revealed) 0f else 1f))
            PartTile(item.b, lit = revealed, modifier = Modifier.offset(x = -shift).rotate(tilt).alpha(dim))
        }
        if (showResult) {
            StagePanel(Modifier.height(150.dp)) {
                AnimatedContent(
                    targetState = revealed,
                    transitionSpec = {
                        (fadeIn(tween(MotionTokens.Dur.State)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.9f))
                            .togetherWith(fadeOut(tween(120)))
                    },
                    label = "fuse-result",
                ) { shown ->
                    if (shown) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SegText(item.segs, 26.sp, lit = true)
                            WordHead(romaji = "", kana = item.kana, word = item.word, onSpeak = onSpeak, size = 50.sp, center = true)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("? ? ?", style = AjlTheme.type.meta.copy(fontSize = 30.sp, letterSpacing = 6.sp), color = AjlTheme.colors.ink3)
                            Text(item.ask.ifBlank { "合起来怎么读？" }, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = AjlTheme.colors.ink2)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuseExplain(item: ZgFuse, picked: Int, audio: com.animejapaneselab.nativeapp.ui.audio.LessonAudioController, ttsWorkerUrl: String) {
    val colors = AjlTheme.colors
    val ok = picked == item.answer
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text(
                if (ok) "✓ ${item.romaji}" else "你选 ${item.options.getOrElse(picked) { "" }} · 正解 ${item.romaji}",
                style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                color = if (ok) colors.ok else colors.bad,
            )
            item.tags.forEach { (text, kind) -> KindChip(text, kind) }
        }
        Text(item.rule, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 23.sp), color = colors.ink)
        LineCard(item.line, audio, ttsWorkerUrl)
    }
}
