package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.CharacterRef
import com.animejapaneselab.nativeapp.ui.design.EmphasisText
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.FeedbackSheet
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.drill.DrillQuestion
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

data class ConjugationSessionActions(
    val onAnswer: (String) -> Unit,
    val onNext: () -> Unit,
    val onRestart: () -> Unit,
    val onExit: () -> Unit,
)

/**
 * 第三巻 活用 answering: the anime line in a manga panel with the target under 着重号 and a
 * 原声 button; one question generated from the line's annotation; [OptionRow]s; one ink 检查;
 * then the [FeedbackSheet] with the 拆解公式, 用法, 译文 and the original voice replayed.
 */
@Composable
fun ConjugationSession(
    state: ConjugationDrillState,
    actions: ConjugationSessionActions,
    ttsWorkerUrl: String,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onExit)
    val audio = rememberLessonAudioController()
    val question = state.current
    when {
        state.isComplete -> ConjugationSetEnd(state, actions, modifier)
        question == null -> ReadAirQuietState(
            onExit = actions.onExit,
            modifier = modifier,
            text = "今日の活用は、おしまい",
            gloss = "这个范围里没有到期或新的句子",
        ) { QuietButton("返回", actions.onExit) }

        else -> Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
            val total = state.session.size
            ReadAirSessionTopBar(
                ReadAirProgress(state.index + 1, total, state.answers.size.toFloat() / total.coerceAtLeast(1)),
                actions.onExit,
            )
            key(question.item.id, state.index) {
                ConjugationQuestionBody(
                    question = question,
                    topic = state.topicFor(question.item),
                    committed = state.answers[state.index],
                    isLast = state.index >= state.session.lastIndex,
                    audio = audio,
                    ttsWorkerUrl = ttsWorkerUrl,
                    actions = actions,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConjugationQuestionBody(
    question: DrillQuestion,
    topic: FoundationTopic?,
    committed: String?,
    isLast: Boolean,
    audio: LessonAudioController,
    ttsWorkerUrl: String,
    actions: ConjugationSessionActions,
    modifier: Modifier = Modifier,
) {
    val item = question.item
    val feedback = LocalFeedbackEngine.current
    val density = LocalDensity.current
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var moving by remember { mutableStateOf(false) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    val answered = !committed.isNullOrBlank()
    val correct = answered && committed == question.answerId
    val scroll = remember { ScrollState(0) }
    val cue = remember(item.id) {
        PromptAudio.Source(item.audioUrl, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = item.jaText)
    }
    val play = { audio.play(cue, ttsWorkerUrl) }
    LaunchedEffect(answered, sheetHeight) {
        if (answered && sheetHeight > 0) scroll.animateScrollTo(scroll.maxValue)
    }
    LaunchedEffect(answered) { if (answered) play() }

    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Eyebrow(listOf("活用", item.episodeLabel, item.startTime.substringBefore(',').removePrefix("00:")).filter { it.isNotBlank() }.joinToString(" · "))
                MangaPanel(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            EmphasisText(
                                item.jaText,
                                listOf(item.spanStart until item.spanEnd),
                                style = AjlTheme.type.jpBody.copy(fontSize = 22.sp, lineHeight = 40.sp),
                            )
                            if (answered && item.reading.isNotBlank() && item.reading.filterNot(Char::isWhitespace) != item.jaText.filterNot(Char::isWhitespace)) {
                                Text(item.reading, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
                            }
                        }
                        IconButton44(Icons.AutoMirrored.Rounded.VolumeUp, "播放原声", play)
                    }
                }
                ReadAirQuestion(question.prompt)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.options.forEach { option ->
                        OptionRow(
                            text = option.text,
                            state = ReadAirRules.optionState(option.id, pending, committed, question.answerId),
                            onClick = { if (!answered) pending = option.id },
                            japanese = option.japanese,
                            leading = option.id,
                        )
                    }
                }
                val bottom = if (answered) with(density) { sheetHeight.toDp() } + 16.dp else 24.dp
                Spacer(Modifier.height(bottom))
            }
            if (!answered) {
                ReadAirCheckBar(
                    checkEnabled = pending != null,
                    onCheck = {
                        val choice = pending ?: return@ReadAirCheckBar
                        feedback?.emit(if (choice == question.answerId) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                        actions.onAnswer(choice)
                    },
                )
            }
        }
        if (answered) {
            ConjugationFeedback(
                question = question,
                topic = topic,
                committed = committed.orEmpty(),
                correct = correct,
                continueLabel = if (isLast) "完成" else "继续",
                onContinue = {
                    if (!moving) {
                        moving = true
                        actions.onNext()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { sheetHeight = it.height },
            )
        }
    }
}

@Composable
private fun ConjugationFeedback(
    question: DrillQuestion,
    topic: FoundationTopic?,
    committed: String,
    correct: Boolean,
    continueLabel: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = question.item
    val hue = AjlTheme.work.hue
    val character = remember(hue) {
        FoundationRules.workSlugFor(hue)?.let { WorkIdentity.representative(it) } ?: CharacterRef("学", "学", null)
    }
    val reaction = remember(item.id, correct) { ReadAirRules.reaction(correct, item.id, inScene = false) }
    val answerText = question.options.firstOrNull { it.id == question.answerId }?.text.orEmpty()
    val notes = buildList {
        if (item.formula.isNotBlank()) add("拆解" to item.formula.trim())
        add("语法点" to listOf(item.pointTitle, item.sense).filter { it.isNotBlank() }.joinToString(" · "))
        if (item.note.isNotBlank()) add("说明" to item.note.trim())
        if (item.zh.isNotBlank()) add("译文" to item.zh.trim())
    }
    var deep by rememberSaveable(item.id) { mutableStateOf(false) }
    val deepNotes = topic?.let { t ->
        buildList {
            add(t.titleJa to listOf(t.titleZh, t.shortDefinitionZh).filter { it.isNotBlank() }.joinToString("：").trim())
            if (t.beginnerExplanationZh.isNotBlank()) add("讲解" to t.beginnerExplanationZh.trim())
            if (t.deepExplanationZh.isNotBlank()) add("深入" to t.deepExplanationZh.trim())
            if (t.cautionNoteZh.isNotBlank()) add("注意" to t.cautionNoteZh.trim())
        }
    }
    FeedbackSheet(
        correct = correct,
        onContinue = onContinue,
        modifier = modifier,
        character = character,
        line = reaction.ja,
        lineGloss = reaction.zh,
        explanation = if (correct) null else "正确答案是「$answerText」。",
        continueLabel = continueLabel,
        extra = {
            key(deep) { ReadAirNotes(if (deep && deepNotes != null) deepNotes else notes) }
            if (deepNotes != null) {
                QuietButton(if (deep) "回到拆解" else "深入讲解 · ${topic?.titleZh.orEmpty()}", { deep = !deep })
            }
        },
    )
}

/** つづく for a drill set: tally, the lines missed this time, 再来一组. */
@Composable
private fun ConjugationSetEnd(state: ConjugationDrillState, actions: ConjugationSessionActions, modifier: Modifier = Modifier) {
    val answered = state.answers.size
    val correct = state.correctCount
    val missed = state.session.filterIndexed { i, q -> state.answers[i]?.let { it != q.answerId } == true }
    var moving by remember { mutableStateOf(false) }
    TsuzukuScreen(
        eyebrow = listOfNotNull("活用", state.group?.let { it.substringAfter(' ').substringBefore('（') }).joinToString(" · "),
        tally = ReadAirRules.tally(answered, correct),
        meta = ReadAirRules.accuracy(answered, correct),
        noted = missed.take(6).map { TsuzukuLine(it.item.jaText, true, it.item.target) },
        notedTitle = "这次答错的 ${missed.size.coerceAtMost(6)} 句",
        primaryLabel = "再来一组",
        onPrimary = {
            if (!moving) {
                moving = true
                actions.onRestart()
            }
        },
        onClose = actions.onExit,
        modifier = modifier,
    )
}
