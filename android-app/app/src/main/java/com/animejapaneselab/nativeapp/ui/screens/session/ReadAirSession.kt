package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.data.buildExternalQuestionPrompt
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.ReadAirAllFilter
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.FeedbackSheet
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 読空気 session in the manga-dialogue language (XReadAirManga): × · progress line · 3/10; mono
 * eyebrow; the scene as a manga panel whose unsaid line is a dashed bubble; the Chinese question;
 * [OptionRow]s; 跳过 + one ink 检查. Checking plays MOTION §3-05 and slides up the [FeedbackSheet]
 * with the speaker's reaction; 继续 → [onNext]. The set ends on a simplified つづく.
 *
 * [onSkip]: 跳过 is shown only when a skip action is wired (the ViewModel has none yet — D2.md).
 * Back gestures are handled by LabApp's predictive back (exits the session).
 */
@Composable
fun ReadAirSessionScreen(
    uiState: LabUiState,
    onExit: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
) {
    val readAir = uiState.readAir
    val exercise = readAir.currentExercise
    val scoped = readAir.scopedExercises
    val total = scoped.size
    val answered = scoped.count { readAir.selectedAnswerFor(it.id).isNotBlank() }

    if (exercise == null) {
        if (total == 0) {
            val loading = readAir.status == SyncStatus.Loading || readAir.status == SyncStatus.Idle && readAir.exercises.isEmpty()
            ReadAirQuietState(
                onExit = onExit,
                modifier = modifier,
                loading = loading,
                text = "ここには、まだ問題がない",
                gloss = "这个范围里没有可答的题",
            ) {
                QuietButton("返回", onExit)
            }
        } else {
            ReadAirSetEnd(uiState, answered, onExit, onRestart, modifier)
        }
        return
    }

    val committed = readAir.selectedAnswerFor(exercise.id)
    val progress = ReadAirRules.progress(
        total = total,
        answered = answered,
        currentAnswered = committed.isNotBlank(),
        hasCurrent = true,
    )
    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        ReadAirSessionTopBar(progress, onExit)
        key(exercise.id) {
            ReadAirQuestionBody(
                exercise = exercise,
                committed = committed,
                isLast = answered >= total,
                onAnswerSelected = onAnswerSelected,
                onNext = onNext,
                onSkip = onSkip,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReadAirQuestionBody(
    exercise: LinguisticExercise,
    committed: String,
    isLast: Boolean,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedbackEngine.current
    val reduced = rememberReducedMotion()
    val density = LocalDensity.current
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    // Guards against double taps between the tap and the ViewModel's recomposition.
    var checking by remember { mutableStateOf(false) }
    var advancing by remember { mutableStateOf(false) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    val answeredNow = committed.isNotBlank()
    val correct = answeredNow && exercise.isCorrect(committed)
    val lines = remember(exercise) { ReadAirRules.sceneLines(exercise) }
    val prompt = remember(exercise.prompt) { ReadAirRules.promptForDisplay(exercise.prompt) }
    val keywords = remember(exercise) { listOf(exercise.phenomenonNameJa) }
    val scroll = remember { ScrollState(0) }
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) appear.animateTo(1f, tween(MotionTokens.Dur.State, easing = MotionTokens.Ease.Standard))
    }
    LaunchedEffect(answeredNow, sheetHeight) {
        if (answeredNow && sheetHeight > 0) {
            if (reduced) scroll.scrollTo(scroll.maxValue) else scroll.animateScrollTo(scroll.maxValue)
        }
    }

    Box(modifier.fillMaxWidth()) {
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
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Eyebrow(ReadAirRules.eyebrow(exercise))
                if (lines.isNotEmpty()) {
                    ReadAirScenePanel(
                        lines = lines,
                        emphasisFor = { line -> if (line.isTarget) ReadAirRules.emphasisFor(line.ja, exercise.prompt, keywords) else emptyList() },
                        emphasisVisible = correct,
                    )
                }
                ReadAirQuestion(prompt)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercise.options.forEach { option ->
                        OptionRow(
                            text = option,
                            state = ReadAirRules.optionState(option, pending, committed, exercise.correctOption),
                            onClick = { if (!answeredNow) pending = option },
                            japanese = ReadAirRules.looksJapanese(option),
                        )
                    }
                }
                val bottom = if (answeredNow) with(density) { sheetHeight.toDp() } + 16.dp else 24.dp
                Spacer(Modifier.height(bottom))
            }
            if (!answeredNow) {
                ReadAirCheckBar(
                    checkEnabled = pending != null && !checking,
                    onCheck = {
                        val choice = pending ?: return@ReadAirCheckBar
                        if (checking) return@ReadAirCheckBar
                        checking = true
                        feedback?.emit(
                            if (exercise.isCorrect(choice)) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong,
                        )
                        onAnswerSelected(choice)
                    },
                    quietLabel = "跳过".takeIf { onSkip != null },
                    onQuiet = onSkip,
                )
            }
        }
        if (answeredNow) {
            ReadAirFeedback(
                exercise = exercise,
                lines = lines,
                committed = committed,
                correct = correct,
                continueLabel = if (isLast) "完成" else "继续",
                onContinue = {
                    if (!advancing) {
                        advancing = true
                        onNext()
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
private fun ReadAirFeedback(
    exercise: LinguisticExercise,
    lines: List<ReadAirSceneLine>,
    committed: String,
    correct: Boolean,
    continueLabel: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable { mutableStateOf(false) }
    val reaction = remember(exercise.id, correct) { ReadAirRules.reaction(correct, exercise.id, inScene = true) }
    val character = WorkIdentity.character(ReadAirRules.targetSpeaker(lines))
        ?: WorkIdentity.representative(exercise.workSlug, exercise.episode.coerceAtLeast(1))
    val basic = exercise.basicExplanationZh.trim()
    val explanation = if (correct) {
        basic.ifBlank { null }
    } else {
        listOf("正确答案是「${exercise.correctOption}」。", basic).filter { it.isNotBlank() }.joinToString("")
    }
    val notes = remember(exercise) { readAirNotes(exercise) }
    FeedbackSheet(
        correct = correct,
        onContinue = onContinue,
        modifier = modifier,
        character = character,
        line = reaction.ja,
        lineGloss = reaction.zh,
        explanation = explanation,
        continueLabel = continueLabel,
        extra = if (notes.isEmpty() && correct) {
            null
        } else {
            {
                ReadAirNotes(notes)
                if (!correct) {
                    OutlineButton(
                        text = if (copied) "已复制题目" else "复制题目",
                        onClick = {
                            clipboard.setText(AnnotatedString(buildExternalQuestionPrompt(exercise, committed)))
                            copied = true
                        },
                        compact = true,
                        leadingIcon = Icons.Rounded.ContentCopy,
                    )
                }
            }
        },
    )
}

/** 证据 / 学习点 / 注意 — whatever the exercise carries. */
private fun readAirNotes(exercise: LinguisticExercise): List<Pair<String, String>> = buildList {
    val evidence = exercise.answer.rationaleZh.trim().ifBlank { exercise.animeContextNoteZh.trim() }
    if (evidence.isNotBlank()) add("证据" to evidence)
    val name = listOf(exercise.phenomenonNameZh.trim(), exercise.phenomenonNameJa.trim().takeIf { it.isNotBlank() }?.let { "（$it）" }.orEmpty())
        .joinToString("")
    val definition = exercise.phenomenonDefinitionZh.trim().ifBlank { exercise.deepExplanationZh.trim() }
    val learning = listOf(name, definition).filter { it.isNotBlank() }.joinToString(" — ")
    if (learning.isNotBlank()) add("学习点" to learning)
    if (exercise.cautionNoteZh.isNotBlank()) add("注意" to exercise.cautionNoteZh.trim())
}

/** つづく for a finished read-air set: tally, missed lines, 今天到这 / 再来一组. */
@Composable
private fun ReadAirSetEnd(
    uiState: LabUiState,
    answered: Int,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readAir = uiState.readAir
    val scoped = readAir.scopedExercises
    val correct = scoped.count { item ->
        val answer = readAir.selectedAnswerFor(item.id)
        answer.isNotBlank() && item.isCorrect(answer)
    }
    val missed = scoped.filter { item ->
        val answer = readAir.selectedAnswerFor(item.id)
        answer.isNotBlank() && !item.isCorrect(answer)
    }
    val filters = readAir.filters
    val workSlug = filters.workSlug.takeUnless { it == ReadAirAllFilter } ?: uiState.selection.workSlug
    val scope = ReadAirRules.scopeLabel(
        workLabel = WorkIdentity.displayName(workSlug, fallback = workSlug),
        episode = filters.episode,
        domain = filters.domain,
    )
    var restarting by remember { mutableStateOf(false) }
    ReadAirTsuzuku(
        eyebrow = "读空气 · $scope",
        tally = ReadAirRules.tally(answered, correct),
        meta = ReadAirRules.accuracy(answered, correct),
        noted = missed.take(6).map { item ->
            val line = ReadAirRules.sceneLines(item).firstOrNull { it.isTarget }?.ja ?: item.jaText
            val text = line.ifBlank { ReadAirRules.promptForDisplay(item.prompt) }
            ReadAirNotedLine(text, ReadAirRules.looksJapanese(text), "错题本")
        },
        primaryLabel = "再来一组",
        onPrimary = {
            if (!restarting) {
                restarting = true
                onRestart()
            }
        },
        onClose = onExit,
        modifier = modifier,
        quietLabel = "今天到这",
        onQuiet = onExit,
    )
}
