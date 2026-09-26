package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
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
import com.animejapaneselab.nativeapp.data.FoundationQuestion
import com.animejapaneselab.nativeapp.data.FoundationStimulus
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.ui.design.CharacterRef
import com.animejapaneselab.nativeapp.ui.design.EmphasisText
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.FeedbackSheet
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingError
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingPhase
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingState
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.screens.FoundationActions
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 第二巻 基礎 answering, same visual language as ReadAirSession: × · progress · n/N; mono eyebrow;
 * the stimulus in a manga panel (sentence with 着重号, dialogue as bubbles, contrast pairs,
 * metalinguistic form); the Chinese question with an optional 提示; [OptionRow]s; 上一题 + one ink
 * 检查; then the [FeedbackSheet] (a classmate's line, explanation, 学习点, 深入解析) → 继续.
 *
 * Only answering lives here — pack / domain / stage selection belongs to package C's 言語学 page,
 * which shows this screen and handles [onExit]. With nothing to answer a quiet empty state offers
 * 返回. The system back gesture also calls [onExit].
 */
@Composable
fun FoundationSession(
    state: FoundationTrainingState,
    actions: FoundationActions,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onExit)
    when (state.phase) {
        FoundationTrainingPhase.Idle,
        FoundationTrainingPhase.LoadingCatalog,
        FoundationTrainingPhase.LoadingQuestions -> ReadAirQuietState(onExit = onExit, modifier = modifier, loading = true)

        FoundationTrainingPhase.Error -> ReadAirQuietState(
            onExit = onExit,
            modifier = modifier,
            text = "問題が読み込めない",
            gloss = when (state.error) {
                FoundationTrainingError.MalformedData -> "题库数据不完整，已停止展示"
                FoundationTrainingError.EmptyCatalog -> "当前没有已发布的题包"
                FoundationTrainingError.Network, null -> "连不上题库服务"
            },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                QuietButton("返回", onExit)
                OutlineButton("重新加载", actions.onRefresh)
            }
        }

        FoundationTrainingPhase.Ready -> {
            val question = state.currentQuestion
            when {
                state.isComplete -> FoundationSetEnd(state, actions, onExit, modifier)
                question == null -> ReadAirQuietState(
                    onExit = onExit,
                    modifier = modifier,
                    text = "ここには、まだ問題がない",
                    gloss = "这个范围里没有可答的题",
                ) {
                    QuietButton("返回", onExit)
                }

                else -> Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
                    ReadAirSessionTopBar(FoundationRules.progress(state), onExit)
                    key(question.id, state.currentIndex) {
                        FoundationQuestionBody(
                            question = question,
                            topic = state.topics.firstOrNull { it.id == question.topicId },
                            committed = state.currentSelectedOptionId,
                            canGoPrevious = state.currentIndex > 0,
                            isLast = state.currentIndex >= state.filteredQuestions.lastIndex,
                            actions = actions,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoundationQuestionBody(
    question: FoundationQuestion,
    topic: FoundationTopic?,
    committed: String?,
    canGoPrevious: Boolean,
    isLast: Boolean,
    actions: FoundationActions,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedbackEngine.current
    val reduced = rememberReducedMotion()
    val density = LocalDensity.current
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var hintVisible by rememberSaveable { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    val answered = !committed.isNullOrBlank()
    val correct = answered && question.isCorrect(committed.orEmpty())
    val scroll = remember { ScrollState(0) }
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) appear.animateTo(1f, tween(MotionTokens.Dur.State, easing = MotionTokens.Ease.Standard))
    }
    LaunchedEffect(answered, sheetHeight) {
        if (answered && sheetHeight > 0) {
            if (reduced) scroll.scrollTo(scroll.maxValue) else scroll.animateScrollTo(scroll.maxValue)
        }
    }
    val once: (() -> Unit) -> Unit = { action ->
        if (!moving) {
            moving = true
            action()
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
                Eyebrow(FoundationRules.eyebrow(question, topic))
                FoundationStimulusPanel(question, topic, emphasisVisible = correct)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReadAirQuestion(question.promptZh.trim())
                    if (!answered && question.hintZh.isNotBlank()) {
                        if (hintVisible) {
                            Text(
                                question.hintZh.trim(),
                                style = AjlTheme.type.body.copy(fontSize = 14.sp),
                                color = AjlTheme.colors.ink2,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        } else {
                            QuietButton("提示", { hintVisible = true }, Modifier.graphicsLayer { translationX = -12.dp.toPx() })
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.options.forEach { option ->
                        OptionRow(
                            text = option.text,
                            state = FoundationRules.optionState(option.id, pending, committed, question.answerOptionId),
                            onClick = { if (!answered) pending = option.id },
                            japanese = ReadAirRules.looksJapanese(option.text),
                            leading = FoundationRules.optionMark(option.id),
                        )
                    }
                }
                val bottom = if (answered) with(density) { sheetHeight.toDp() } + 16.dp else 24.dp
                Spacer(Modifier.height(bottom))
            }
            if (!answered) {
                ReadAirCheckBar(
                    checkEnabled = pending != null && !checking,
                    onCheck = {
                        val choice = pending ?: return@ReadAirCheckBar
                        if (checking) return@ReadAirCheckBar
                        checking = true
                        feedback?.emit(
                            if (question.isCorrect(choice)) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong,
                        )
                        actions.onAnswerSelected(choice)
                    },
                    quietLabel = "上一题",
                    quietEnabled = canGoPrevious && !checking,
                    onQuiet = { once(actions.onPrevious) },
                )
            }
        }
        if (answered) {
            FoundationFeedback(
                question = question,
                topic = topic,
                committed = committed.orEmpty(),
                correct = correct,
                canGoPrevious = canGoPrevious,
                continueLabel = if (isLast) "完成" else "继续",
                onContinue = { once(actions.onNext) },
                onPrevious = { once(actions.onPrevious) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { sheetHeight = it.height },
            )
        }
    }
}

/** The stimulus in a manga panel, one layout per stimulus kind. */
@Composable
private fun FoundationStimulusPanel(question: FoundationQuestion, topic: FoundationTopic?, emphasisVisible: Boolean) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val bigJp = type.jpBody.copy(fontSize = 22.sp, lineHeight = 40.sp)
    val context: @Composable (String?) -> Unit = { text ->
        if (!text.isNullOrBlank()) Text(text.trim(), style = type.caption, color = colors.ink3)
    }
    when (val stimulus = question.stimulus) {
        is FoundationStimulus.Dialogue -> {
            val lines = remember(question.id) { FoundationRules.dialogueLines(stimulus, question.questionType, topic?.domain) }
            ReadAirScenePanel(
                lines = lines,
                emphasisFor = { line -> ReadAirRules.emphasisFor(line.ja, question.promptZh) },
                emphasisVisible = emphasisVisible,
                footer = if (stimulus.zhContext.isNullOrBlank()) null else { { context(stimulus.zhContext) } },
            )
        }

        is FoundationStimulus.Sentence -> MangaPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val text = stimulus.jaText.trim()
                EmphasisText(
                    text,
                    remember(question.id) { ReadAirRules.emphasisFor(text, question.promptZh) },
                    style = bigJp,
                    emphasisVisible = emphasisVisible,
                )
                context(stimulus.zhContext)
            }
        }

        is FoundationStimulus.Contrast -> MangaPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                stimulus.items.forEachIndexed { index, item ->
                    if (index > 0) Hairline()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            item.label?.trim()?.takeIf { it.isNotBlank() } ?: ('A' + index).toString(),
                            style = type.meta,
                            color = colors.ink3,
                            modifier = Modifier
                                .width(28.dp)
                                .padding(top = 8.dp),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                item.text.trim(),
                                style = if (ReadAirRules.looksJapanese(item.text)) type.jpBody else type.body,
                                color = colors.ink,
                            )
                            context(item.noteZh)
                        }
                    }
                }
                if (!stimulus.zhContext.isNullOrBlank()) {
                    Box(Modifier.padding(bottom = 6.dp)) { context(stimulus.zhContext) }
                }
            }
        }

        is FoundationStimulus.Metalinguistic -> MangaPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stimulus.form?.trim()?.takeIf { it.isNotBlank() }?.let { form ->
                    Text(form, style = if (ReadAirRules.looksJapanese(form)) bigJp else type.title, color = colors.ink)
                }
                Text(stimulus.descriptionZh.trim(), style = type.body, color = colors.ink2)
            }
        }
    }
}

@Composable
private fun FoundationFeedback(
    question: FoundationQuestion,
    topic: FoundationTopic?,
    committed: String,
    correct: Boolean,
    canGoPrevious: Boolean,
    continueLabel: String,
    onContinue: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var deep by rememberSaveable { mutableStateOf(false) }
    val hue = AjlTheme.work.hue
    val character = remember(hue) {
        WorkIdentity.representative(FoundationRules.workSlugFor(hue)).takeIf { FoundationRules.workSlugFor(hue) != null }
            ?: CharacterRef("学", "学", null)
    }
    val reaction = remember(question.id, correct) { ReadAirRules.reaction(correct, question.id, inScene = false) }
    val correctText = question.options.firstOrNull { it.id == question.answerOptionId }?.text.orEmpty()
    val explanation = if (correct) {
        question.explanationZh.trim().ifBlank { null }
    } else {
        question.wrongExplanations[committed]?.trim()?.takeIf { it.isNotBlank() } ?: "正确答案是「$correctText」。"
    }
    val notes = buildList {
        if (!correct && question.explanationZh.isNotBlank()) add("解释" to question.explanationZh.trim())
        topic?.let { t ->
            val point = listOf(t.titleZh.trim(), t.titleJa.trim().takeIf { it.isNotBlank() }?.let { "（$it）" }.orEmpty()).joinToString("")
            val body = listOf(point, t.shortDefinitionZh.trim()).filter { it.isNotBlank() }.joinToString(" — ")
            if (body.isNotBlank()) add("学习点" to body)
        }
        if (deep) {
            if (question.deepExplanationZh.isNotBlank()) add("深入理解" to question.deepExplanationZh.trim())
            if (question.cautionNoteZh.isNotBlank()) add("容易混淆" to question.cautionNoteZh.trim())
            val transfer = listOfNotNull(question.transferExampleJa?.trim(), question.transferExplanationZh?.trim())
                .filter { it.isNotBlank() }
            if (transfer.isNotEmpty()) add("迁移例句" to transfer.joinToString("\n"))
        }
    }
    val hasDeep = question.deepExplanationZh.isNotBlank() || question.cautionNoteZh.isNotBlank() ||
        !question.transferExampleJa.isNullOrBlank()
    FeedbackSheet(
        correct = correct,
        onContinue = onContinue,
        modifier = modifier,
        character = character,
        line = reaction.ja,
        lineGloss = reaction.zh,
        explanation = explanation,
        continueLabel = continueLabel,
        extra = {
            ReadAirNotes(notes)
            if (canGoPrevious || (hasDeep && !deep)) {
                Row(
                    Modifier.graphicsLayer { translationX = -12.dp.toPx() },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (canGoPrevious) QuietButton("上一题", onPrevious)
                    if (hasDeep && !deep) QuietButton("深入解析", { deep = true })
                }
            }
        },
    )
}

/** つづく for the foundation set: tally, missed questions, 上一题 / 重新练习. */
@Composable
private fun FoundationSetEnd(
    state: FoundationTrainingState,
    actions: FoundationActions,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val answered = state.answeredCount
    val correct = FoundationRules.correctCount(state)
    val missed = state.filteredQuestions.filter { q -> state.selectedAnswers[q.id]?.let { !q.isCorrect(it) } == true }
    val scope = listOfNotNull(
        state.filters.domain?.let(FoundationRules::domainLabel),
        state.filters.stage?.let(FoundationRules::stageLabel),
    ).joinToString(" · ")
    var moving by remember { mutableStateOf(false) }
    TsuzukuScreen(
        eyebrow = listOf("基礎", scope).filter { it.isNotBlank() }.joinToString(" · "),
        tally = ReadAirRules.tally(answered, correct),
        meta = ReadAirRules.accuracy(answered, correct),
        noted = missed.take(6).map { q ->
            val text = FoundationRules.summaryText(q)
            TsuzukuLine(text, ReadAirRules.looksJapanese(text), q.stage.name)
        },
        notedTitle = "这次答错的 ${missed.size.coerceAtMost(6)} 题",
        primaryLabel = "重新练习",
        onPrimary = {
            if (!moving) {
                moving = true
                actions.onRestart()
            }
        },
        onClose = onExit,
        modifier = modifier,
        quietLabel = "上一题",
        quietEnabled = state.currentIndex > 0,
        onQuiet = {
            if (!moving) {
                moving = true
                actions.onPrevious()
            }
        },
    )
}
