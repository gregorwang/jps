package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.animejapaneselab.nativeapp.ui.design.MarkedLine
import com.animejapaneselab.nativeapp.ui.design.NoteText
import com.animejapaneselab.nativeapp.ui.screens.jishu.FormulaRow
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
import com.animejapaneselab.nativeapp.ui.drill.DrillMode
import com.animejapaneselab.nativeapp.ui.drill.DrillQuestion
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

data class ConjugationSessionActions(
    val onAnswer: (String) -> Unit,
    val onNext: () -> Unit,
    val onRestart: () -> Unit,
    val onExit: () -> Unit,
    /** つづく of a 課 → open the next unlearned 課. */
    val onNextLesson: () -> Unit = {},
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
    val learnedTitles = remember(state.learned, state.lessons) { state.learned.map(state::titleOf) }
    when {
        state.isComplete && state.mode == DrillMode.Lesson -> ConjugationLessonEnd(state, actions, modifier)
        state.isComplete -> ConjugationSetEnd(state, actions, modifier)
        question == null -> ReadAirQuietState(
            onExit = actions.onExit,
            modifier = modifier,
            text = "今日の活用は、おしまい",
            gloss = if (state.scoped.isEmpty()) "这本还没在自習里学过" else "这个范围里没有到期或新的句子",
        ) { QuietButton("返回", actions.onExit) }

        else -> Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
            val total = state.session.size
            ReadAirSessionTopBar(
                ReadAirProgress(state.index + 1, total, state.answers.size.toFloat() / total.coerceAtLeast(1)),
                actions.onExit,
            )
            key(question.item?.id ?: question.prompt, state.index) {
                ConjugationQuestionBody(
                    question = question,
                    learned = learnedTitles,
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
    learned: List<String>,
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
    val cue = remember(item?.id) {
        item?.let { PromptAudio.Source(it.audioUrl, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = it.jaText) }
    }
    val play: () -> Unit = { cue?.let { audio.play(it, ttsWorkerUrl) } }
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
                if (item == null) {
                    Eyebrow("活用 · 板書の練習")
                    PracticePrompt(question.prompt)
                } else {
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
                }
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
                learned = learned,
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

/**
 * Feedback for one 活用 question, laid out like the 自習 card it came from: the line with its
 * target marked, the translation, the 拆解 blocks, then the point (用法 tag + 说明). A wrong answer
 * adds the AI's one-line 为什么不是 on top.
 */
@Composable
private fun ConjugationFeedback(
    question: DrillQuestion,
    learned: List<String>,
    committed: String,
    correct: Boolean,
    continueLabel: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = question.item
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val hue = work.hue
    val character = remember(hue) {
        FoundationRules.workSlugFor(hue)?.let { WorkIdentity.representative(it) } ?: CharacterRef("学", "学", null)
    }
    val reaction = remember(question.prompt, correct) { ReadAirRules.reaction(correct, item?.id ?: question.prompt, inScene = false) }
    val answerText = question.options.firstOrNull { it.id == question.answerId }?.text.orEmpty()
    val chosenText = question.options.firstOrNull { it.id == committed }?.text.orEmpty()
    val aiRequest = remember(question.prompt, committed, correct) {
        if (correct || chosenText.isBlank()) {
            null
        } else {
            QuickFeedbackRequest(
                prompt = question.prompt,
                sentence = item?.jaText.orEmpty(),
                chosen = chosenText,
                answer = answerText,
                point = item?.pointTitle.orEmpty(),
                formula = item?.formula.orEmpty(),
                learned = learned,
            )
        }
    }
    val ai = rememberQuickFeedback(aiRequest)
    FeedbackSheet(
        correct = correct,
        onContinue = onContinue,
        modifier = modifier,
        character = character,
        line = reaction.ja,
        lineGloss = reaction.zh,
        explanation = if (correct) null else "正确答案是「$answerText」",
        continueLabel = continueLabel,
        extra = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (ai is QuickFeedbackState.Loading) QuickFeedbackLoading(chosenText)
                (ai as? QuickFeedbackState.Ready)?.let { ready ->
                    FeedbackBlock(quickFeedbackLabel(chosenText)) { NoteText(ready.text) }
                }
                if (item == null) {
                    // 板書 practice: the right form, then why.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MarkedLine(
                            answerText,
                            answerText.indices,
                            style = AjlTheme.type.jpBody.copy(fontSize = 22.sp, lineHeight = 32.sp),
                        )
                        if (question.why.isNotBlank()) NoteText(question.why.trim())
                    }
                    return@Column
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MarkedLine(
                        item.jaText,
                        item.spanStart until item.spanEnd,
                        style = AjlTheme.type.jpBody.copy(fontSize = 19.sp, lineHeight = 30.sp),
                    )
                    if (item.zh.isNotBlank()) {
                        Text(item.zh.trim(), style = AjlTheme.type.body.copy(fontSize = 14.sp, lineHeight = 21.sp), color = colors.ink3)
                    }
                }
                if (item.formula.isNotBlank()) {
                    FeedbackBlock("拆解") { FormulaRow(item.formula, item.group, wordSize = 20.sp) }
                }
                val title = item.pointTitle.trim()
                if (title.isNotBlank() || item.sense.isNotBlank() || item.note.isNotBlank()) {
                    FeedbackBlock("语法点") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (title.isNotBlank()) {
                                Text(title, style = AjlTheme.type.title.copy(fontSize = 15.sp), color = colors.ink, modifier = Modifier.weight(1f, fill = false))
                            }
                            if (item.sense.isNotBlank()) {
                                Text(
                                    item.sense.trim(),
                                    style = AjlTheme.type.caption.copy(fontSize = 12.sp),
                                    color = work.accent,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .border(1.dp, work.accent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                        if (item.note.isNotBlank()) NoteText(item.note.trim())
                    }
                }
            }
        },
    )
}

/** A labelled block in the feedback sheet: small mono label, then its content. */
@Composable
private fun FeedbackBlock(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(label)
        content()
    }
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
        noted = missed.mapNotNull { it.item }.take(6).map { TsuzukuLine(it.jaText, true, it.target) },
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

/** A 板書 practice prompt 「飲む → ない形」: the verb large in serif, the target form after the arrow. */
@Composable
private fun PracticePrompt(prompt: String, modifier: Modifier = Modifier) {
    val parts = prompt.split('→', limit = 2).map { it.trim() }
    MangaPanel(modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(parts.first(), style = AjlTheme.type.jpBody.copy(fontSize = 26.sp, lineHeight = 36.sp), color = AjlTheme.colors.ink)
            if (parts.size > 1) {
                Text("→", style = AjlTheme.type.meta.copy(fontSize = 16.sp), color = AjlTheme.work.accent)
                Text(parts[1], style = AjlTheme.type.title.copy(fontSize = 18.sp), color = AjlTheme.colors.ink2)
            }
        }
    }
}

/** つづく for a 課: tally, the lines missed, then the next 課 (the 課 is now 已学). */
@Composable
private fun ConjugationLessonEnd(state: ConjugationDrillState, actions: ConjugationSessionActions, modifier: Modifier = Modifier) {
    val point = state.openLesson.orEmpty()
    val answered = state.answers.size
    val correct = state.correctCount
    val missed = state.session.filterIndexed { i, q -> state.answers[i]?.let { it != q.answerId } == true }
    val next = state.nextLesson(state.openBook) ?: state.nextLesson()
    var moving by remember { mutableStateOf(false) }
    TsuzukuScreen(
        eyebrow = "活用 · 第 ${state.lessonNumber(point)} 課 · 已学",
        tally = ReadAirRules.tally(answered, correct),
        meta = ReadAirRules.accuracy(answered, correct),
        noted = missed.take(6).map { q ->
            q.item?.let { TsuzukuLine(it.jaText, true, it.target) }
                ?: TsuzukuLine(q.prompt, true, q.options.firstOrNull { it.id == q.answerId }?.text.orEmpty())
        },
        notedTitle = "这次答错的 ${missed.size.coerceAtMost(6)} 题",
        preview = next?.let { TsuzukuPreview(title = "第 ${state.lessonNumber(it)} 課 · ${state.titleOf(it)}", meta = "次回") },
        primaryLabel = if (next != null) "下一课" else "回到目次",
        onPrimary = {
            if (!moving) {
                moving = true
                if (next != null) actions.onNextLesson() else actions.onExit()
            }
        },
        onClose = actions.onExit,
        modifier = modifier,
    )
}
