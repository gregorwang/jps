package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.PairMatchNode
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import com.animejapaneselab.nativeapp.data.StudyCardNode
import com.animejapaneselab.nativeapp.data.TileOrderNode
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.screens.learn.SceneKind
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import java.time.LocalDate

/**
 * Lesson session: アイキャッチ → questions in the 会話窓 language → feedback sheet with the
 * character's reaction → つづく. The signature is the contract LabApp calls (task package D1).
 */
@Composable
fun LessonSessionScreen(
    uiState: LabUiState,
    onExit: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    onEvaluatePronunciation: (String, String, ByteArray, Long) -> Unit,
    onRetryPronunciation: () -> Unit,
    onResetPronunciation: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onEyecatchPlayed: (workSlug: String, episode: Int, date: String) -> Unit = { _, _, _ -> },
) {
    val session = uiState.lesson
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    val audio = rememberLessonAudioController()
    val feedbackEngine = LocalFeedbackEngine.current
    BackHandler(onBack = onExit)

    // A set is identified by its nodes; restart keeps the nodes but resets the counters.
    val setKey = remember(session.nodes) { session.nodes.joinToString("|") { it.id }.hashCode() }
    val atStart = session.index == 0 && session.answered == 0
    var startedAt by rememberSaveable(setKey) { mutableStateOf(android.os.SystemClock.elapsedRealtime()) }
    var finishedAt by rememberSaveable(setKey) { mutableStateOf(0L) }
    var missed by rememberSaveable(setKey) { mutableStateOf(listOf<String>()) }
    LaunchedEffect(setKey, atStart) {
        if (atStart) {
            startedAt = android.os.SystemClock.elapsedRealtime()
            finishedAt = 0L
            missed = emptyList()
        }
    }
    // 进场アイキャッチ can be switched off in settings; the full version plays once per episode per day.
    var showEyecatch by rememberSaveable(setKey) {
        mutableStateOf(uiState.settings.richAnimationsEnabled && atStart && !session.isComplete && session.nodes.isNotEmpty())
    }
    val today = remember(setKey) { LocalDate.now().toString() }
    val shortEyecatch = remember(setKey) { uiState.eyecatchPlayedOn["$workSlug:$episode"] == today }
    LaunchedEffect(setKey, showEyecatch) {
        if (showEyecatch && !shortEyecatch) onEyecatchPlayed(workSlug, episode, today)
    }

    if (session.isComplete) {
        LaunchedEffect(setKey) { if (finishedAt == 0L) finishedAt = android.os.SystemClock.elapsedRealtime() }
        LessonTsuzuku(
            uiState = uiState,
            missedIds = missed,
            elapsedMs = (if (finishedAt == 0L) android.os.SystemClock.elapsedRealtime() else finishedAt) - startedAt,
            onExit = onExit,
            onRestart = onRestart,
            onNextBatch = onNextBatch,
            modifier = modifier,
        )
        return
    }
    val node = session.currentNode ?: return

    if (showEyecatch) {
        Eyecatch(
            workSlug = workSlug,
            episode = episode,
            setLine = listOfNotNull(
                LessonRules.setName(uiState),
                LessonRules.batchLabel(uiState),
                "${session.nodes.size} 题",
            ).joinToString(" · "),
            quote = remember(setKey) { LessonRules.eyecatchLine(session.nodes) },
            short = shortEyecatch,
            onDone = { showEyecatch = false },
            modifier = modifier,
        )
        return
    }

    val feedback = session.feedback
    val settings = uiState.settings

    // Auto-play the prompt audio once per question.
    var lastAutoPlay by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(session.index, node.id) {
        val key = "${session.answered}:${session.index}:${node.id}"
        if (settings.autoSpeak && node.audio.autoPlay && lastAutoPlay != key) {
            lastAutoPlay = key
            audio.play(node.audio, settings.ttsWorkerUrl, autoAttempt = true)
        }
    }
    // Choice questions whose audio would give the answer away play it after answering.
    var lastRevealAudio by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(node.id, feedback?.selected) {
        val key = "${node.id}:${feedback?.selected}"
        if (settings.autoSpeak && node is SingleChoiceNode && feedback != null && node.audio != PromptAudio.None &&
            !node.audio.autoPlay && lastRevealAudio != key
        ) {
            lastRevealAudio = key
            audio.play(node.audio, settings.ttsWorkerUrl, autoAttempt = true)
        }
    }
    // Verdict haptics + sound once per answer; remember misses for つづく.
    var lastVerdict by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(node.id, session.answered, feedback?.selected) {
        val current = feedback ?: return@LaunchedEffect
        val key = "${session.answered}:${node.id}:${current.selected}"
        if (lastVerdict == key) return@LaunchedEffect
        lastVerdict = key
        if (node is StudyCardNode) return@LaunchedEffect
        feedbackEngine?.emit(if (current.correct) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        if (!current.correct && node.id !in missed) missed = missed + node.id
    }
    // A study card is not a question: acknowledging it moves straight on.
    val latestContinue by rememberUpdatedState(onContinue)
    LaunchedEffect(node.id, feedback != null) {
        if (node is StudyCardNode && feedback != null) latestContinue()
    }

    val lineIndex = remember(uiState.shadowing, uiState.subtitles, uiState.readAir.exercises, workSlug, episode) {
        LessonRules.lineIndex(uiState)
    }
    val info = LessonRules.lineInfo(node, uiState, lineIndex)
    val scene = LessonRules.sceneLabel(uiState.lessonMode, uiState.isExerciseLabSession)
    val total = session.nodes.size
    val fraction = if (total == 0) 0f else ((session.index + if (feedback != null) 1 else 0).toFloat() / total).coerceIn(0f, 1f)
    var sheetHeight by remember(node.id) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val env = LessonQuestionEnv(
        node = node,
        feedback = feedback,
        eyebrow = LessonRules.eyebrow(scene, node, info),
        speaker = info?.speaker,
        character = LessonRules.reactionCharacter(info?.speaker, workSlug, episode),
        sheetInset = with(density) { sheetHeight.toDp() },
        playback = audio.playbackState,
        onPlay = { cue -> audio.play(cue, settings.ttsWorkerUrl) },
        onSpeak = { text -> audio.speakText(text, settings.ttsWorkerUrl) },
        onSubmit = onSubmitAnswer,
        onWrongTap = { feedbackEngine?.emit(FeedbackEvent.AnswerWrong) },
        onSkip = onSkip?.takeIf { feedback == null },
    )

    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        LessonTopBar(
            position = (session.index + 1).coerceAtMost(total),
            total = total,
            fraction = fraction,
            onExit = onExit,
            modifier = Modifier.statusBarsPadding(),
        )
        Box(Modifier.fillMaxWidth().weight(1f)) {
            key(node.id) {
                when (node) {
                    is StudyCardNode -> StudyCardQuestion(env, node, settings)
                    is PairMatchNode -> PairMatchQuestion(env, node, autoSpeak = settings.autoSpeak)
                    is SingleChoiceNode -> ChoiceQuestion(env, node)
                    is ClozeNode -> ClozeQuestion(env, node)
                    is TileOrderNode -> TileOrderQuestion(env, node)
                    is ShadowingNode -> ShadowingQuestion(
                        env,
                        node,
                        settings,
                        PronunciationActions(
                            state = uiState.pronunciationEvaluation,
                            onEvaluate = onEvaluatePronunciation,
                            onRetry = onRetryPronunciation,
                            onReset = onResetPronunciation,
                        ),
                    )
                }
                if (feedback != null && node !is StudyCardNode) {
                    LessonFeedback(
                        node = node,
                        feedback = feedback,
                        character = env.character,
                        isLast = session.index >= session.nodes.lastIndex,
                        onContinue = onContinue,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { sheetHeight = it.height },
                    )
                }
            }
        }
    }
}

/** つづく for a lesson set: tally, time, missed lines, 次回予告, 今天到这 / 看下一回. */
@Composable
private fun LessonTsuzuku(
    uiState: LabUiState,
    missedIds: List<String>,
    elapsedMs: Long,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = uiState.lesson
    var moving by remember { mutableStateOf(false) }
    fun once(action: () -> Unit): () -> Unit = {
        if (!moving) {
            moving = true
            action()
        }
    }
    val answered = session.answered
    val accuracy = if (answered <= 0) null else "${(session.correct * 100 + answered / 2) / answered}%"
    val setName = LessonRules.setName(uiState)
    val noted = remember(missedIds, session.nodes) {
        session.nodes.filter { it.id in missedIds }.mapNotNull { node ->
            LessonRules.notedText(node)?.let { TsuzukuLine(it, LessonRules.looksJapanese(it), "错题本") }
        }.take(6)
    }
    val preview = remember(uiState.lessonMode, uiState.lessonBatch, uiState.hasNextLessonBatch, session.nodes) {
        lessonPreview(uiState)
    }
    val hasNext = uiState.hasNextLessonBatch
    TsuzukuScreen(
        eyebrow = listOfNotNull(
            LessonRules.broadcastTag(uiState.selection.workSlug, uiState.selection.episode),
            listOfNotNull(setName, LessonRules.batchLabel(uiState)).joinToString(" "),
        ).joinToString(" · "),
        tally = "${session.nodes.size} 题对了 ${session.correct.coerceAtMost(session.nodes.size)} 题",
        meta = listOfNotNull(accuracy, LessonRules.elapsed(elapsedMs)).joinToString(" · "),
        noted = noted,
        notedTitle = "这次记下的 ${noted.size} 句",
        preview = preview,
        primaryLabel = if (hasNext) "看下一回" else "回到本話",
        onPrimary = once(if (hasNext) onNextBatch else onExit),
        onClose = onExit,
        quietLabel = if (hasNext) "今天到这" else "再练一次",
        onQuiet = once(if (hasNext) onExit else onRestart),
        modifier = modifier,
    )
}

/**
 * 次回予告: the next batch of this scene when there is one (with the next sentence of the
 * episode as a teaser for sentence scenes), otherwise the next scene of the episode.
 */
private fun lessonPreview(uiState: LabUiState): TsuzukuPreview? {
    if (uiState.isExerciseLabSession || uiState.lessonMode == LessonMode.Review) return null
    val kind = SceneKind.entries.firstOrNull { it.lessonMode == uiState.lessonMode } ?: return null
    if (uiState.hasNextLessonBatch) {
        val teaser = if (kind == SceneKind.Listening || kind == SceneKind.Shadowing) {
            val used = uiState.lesson.nodes.mapNotNull { LessonRules.sentenceFor(it, uiState.shadowing)?.id }.toSet()
            val last = uiState.shadowing.indexOfLast { it.id in used }
            uiState.shadowing.drop(last + 1).firstOrNull { it.ja.isNotBlank() }?.ja
        } else {
            null
        }
        return TsuzukuPreview(
            title = "第 ${uiState.lessonBatch + 1} 组 · ${kind.title}",
            meta = TextRules.sceneLabel(kind.ordinal + 1),
            line = teaser,
        )
    }
    val next = SceneKind.Main.getOrNull(SceneKind.Main.indexOf(kind) + 1) ?: return TsuzukuPreview(
        title = "${TextRules.episodeLabel(uiState.selection.episode + 1)}",
        meta = "下一話",
    )
    return TsuzukuPreview(
        title = "${TextRules.sceneLabel(next.ordinal + 1)} · ${next.title}",
        meta = TextRules.episodeLabel(uiState.selection.episode),
    )
}
