package com.animejapaneselab.nativeapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.EpisodePlan
import com.animejapaneselab.nativeapp.data.FuriganaResult
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LessonExerciseKind
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.LinguisticCardPayload
import com.animejapaneselab.nativeapp.data.PairMatchNode
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.PronunciationAssessmentStatus
import com.animejapaneselab.nativeapp.data.PronunciationEvaluation
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import com.animejapaneselab.nativeapp.data.StudyCardNode
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.data.TileOrderNode
import com.animejapaneselab.nativeapp.data.WorkOption
import com.animejapaneselab.nativeapp.data.buildExternalQuestionPrompt
import com.animejapaneselab.nativeapp.domain.AnswerFeedback
import com.animejapaneselab.nativeapp.ui.LabTab
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.PronunciationEvaluationPhase
import com.animejapaneselab.nativeapp.ui.PronunciationEvaluationState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.PronunciationMaximumDurationMs
import com.animejapaneselab.nativeapp.ui.audio.PronunciationMinimumDurationMs
import com.animejapaneselab.nativeapp.ui.audio.PronunciationWavRecorder
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.completion.LessonCompleteScreen
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonButton
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonChoiceCard
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonProgressBar
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonTone
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterArtwork
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterRole
import com.animejapaneselab.nativeapp.ui.components.LearningTileButton
import com.animejapaneselab.nativeapp.ui.components.RewardMetricCard
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LearningAssetRegistry
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.fusion.AnimeLabFusionDrawableResolver
import com.animejapaneselab.nativeapp.ui.fusion.FusionDrawableHost
import com.animejapaneselab.nativeapp.ui.fusion.FusionVisualKey
import com.animejapaneselab.nativeapp.ui.motion.LessonPageTransition
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.RubyText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.rive.FusionCtaLightningRive
import com.animejapaneselab.nativeapp.ui.rive.FusionMidLessonStreakRive
import com.animejapaneselab.nativeapp.ui.theme.AnimeJapaneseLabTheme
import com.animejapaneselab.nativeapp.ui.theme.LabPalette
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LessonScreen(
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
) {
    val session = uiState.lesson
    val audioController = rememberLessonAudioController()
    val feedbackEngine = LocalFeedbackEngine.current
    val lessonScrollState = rememberScrollState()

    BackHandler(onBack = onExit)

    if (session.isComplete) {
        LessonCompleteScreen(
            uiState = uiState,
            exitLabel = lessonExitLabel(uiState.selectedTab),
            onExit = onExit,
            onRestart = onRestart,
            onNextBatch = onNextBatch,
            modifier = modifier,
        )
        return
    }

    val node = session.currentNode ?: return
    val exitLabel = lessonExitLabel(uiState.selectedTab)
    val visualStyle = node.fusionVisualStyle(uiState.selection.workSlug)
    val reducedMotion = rememberReducedMotion()
    val motionEnabled = uiState.settings.richAnimationsEnabled && !reducedMotion
    val displayedProgress = if (session.nodes.isEmpty()) {
        0f
    } else {
        (session.index + if (session.feedback != null) 1 else 0)
            .toFloat()
            .div(session.nodes.size.toFloat())
            .coerceIn(0f, 1f)
    }
    var streakDelightCombo by remember { mutableStateOf(0) }
    var streakDelightVisible by remember { mutableStateOf(false) }
    var lastStreakDelightAnswer by rememberSaveable { mutableStateOf(-1) }
    var lastAutoPlayKey by rememberSaveable { mutableStateOf("") }
    var lastFeedbackAudioKey by rememberSaveable { mutableStateOf("") }
    var lastFeedbackEventKey by rememberSaveable { mutableStateOf("") }
    val courseArtworkRes = LearningAssetRegistry.courseArtworkFor(
        uiState.selection.workSlug,
        uiState.selection.episode,
    )
    LaunchedEffect(node.id) {
        lessonScrollState.scrollTo(0)
    }
    LaunchedEffect(session.index, node.id) {
        val eventKey = "${session.answered}:${session.index}:${node.id}"
        if (uiState.settings.autoSpeak && shouldAutoPlayLessonAudio(node.audio) && lastAutoPlayKey != eventKey) {
            lastAutoPlayKey = eventKey
            audioController.play(node.audio, uiState.settings.ttsWorkerUrl, autoAttempt = true)
        }
    }
    LaunchedEffect(node.id, session.feedback?.selected) {
        val eventKey = "${node.id}:${session.feedback?.selected}"
        if (
            uiState.settings.autoSpeak &&
            node is SingleChoiceNode &&
            session.feedback != null &&
            node.audio != PromptAudio.None &&
            !node.audio.autoPlay &&
            lastFeedbackAudioKey != eventKey
        ) {
            lastFeedbackAudioKey = eventKey
            audioController.play(node.audio, uiState.settings.ttsWorkerUrl, autoAttempt = true)
        }
    }
    LaunchedEffect(node.id, session.answered, session.feedback?.selected, session.feedback?.correct) {
        val feedback = session.feedback ?: return@LaunchedEffect
        val eventKey = "${session.answered}:${node.id}:${feedback.selected}:${feedback.correct}"
        if (lastFeedbackEventKey == eventKey) return@LaunchedEffect
        lastFeedbackEventKey = eventKey
        feedbackEngine?.emit(
            if (feedback.correct) FeedbackEvent.AnswerCorrect(xp = 12) else FeedbackEvent.AnswerWrong,
        )
    }
    LaunchedEffect(node.id, session.feedback?.selected) {
        if (session.feedback != null) {
            delay(90)
            lessonScrollState.animateScrollTo(0)
        }
    }
    LaunchedEffect(session.answered, session.currentStreak, session.feedback?.correct, node.id) {
        if (session.answered == 0 && session.currentStreak == 0) {
            lastStreakDelightAnswer = -1
            streakDelightVisible = false
            return@LaunchedEffect
        }
        val isMilestone = session.currentStreak == 5 ||
            (session.currentStreak >= 10 && session.currentStreak % 5 == 0)
        val shouldShow = session.feedback?.correct == true &&
            isMilestone &&
            lastStreakDelightAnswer != session.answered
        if (!shouldShow) {
            streakDelightVisible = false
            return@LaunchedEffect
        }
        lastStreakDelightAnswer = session.answered
        streakDelightCombo = session.currentStreak
        streakDelightVisible = true
        try {
            delay(if (motionEnabled) 1_800L else 900L)
        } finally {
            streakDelightVisible = false
        }
    }
    LaunchedEffect(uiState.aiCoach.status, uiState.aiCoach.answer) {
        if (uiState.aiCoach.status == SyncStatus.Loading) {
            delay(120)
            lessonScrollState.animateScrollTo(lessonScrollState.maxValue)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        visualStyle.softContainer.copy(alpha = 0.40f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LessonTopBar(
                index = session.index,
                total = session.nodes.size,
                progress = displayedProgress,
                episodeLabel = uiState.focus.episodeLabel,
                exitLabel = exitLabel,
                artworkRes = courseArtworkRes,
                style = visualStyle,
                onExit = onExit,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(lessonScrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LessonPageTransition(targetIndex = session.index) { animatedIndex ->
                    val animatedNode = session.nodes.getOrNull(animatedIndex) ?: node
                    val animatedFeedback = session.feedback.takeIf { animatedIndex == session.index }
                    val animatedStyle = animatedNode.fusionVisualStyle(uiState.selection.workSlug)
                    val animatedOwnsTaskHeader = animatedNode is PairMatchNode || animatedNode is ClozeNode || animatedNode is TileOrderNode
                    val animatedSubmit = onSubmitAnswer.takeIf { animatedIndex == session.index } ?: {}
                    if (animatedNode is StudyCardNode) {
                        LessonNodeContent(
                            node = animatedNode,
                            workSlug = uiState.selection.workSlug,
                            autoSpeak = uiState.settings.autoSpeak,
                            feedback = animatedFeedback,
                            style = animatedStyle,
                            motionEnabled = motionEnabled,
                            playbackState = audioController.playbackState,
                            onSubmitAnswer = animatedSubmit,
                            onPlayAudio = { cue ->
                                audioController.play(cue, uiState.settings.ttsWorkerUrl)
                            },
                            onSpeakText = { text ->
                                audioController.speakText(text, uiState.settings.ttsWorkerUrl)
                            },
                            onPairFeedback = { correct ->
                                feedbackEngine?.emit(
                                    if (correct) FeedbackEvent.AnswerCorrect(xp = 1) else FeedbackEvent.AnswerWrong,
                                )
                            },
                            pronunciationEvaluation = uiState.pronunciationEvaluation,
                            onEvaluatePronunciation = onEvaluatePronunciation,
                            onRetryPronunciation = onRetryPronunciation,
                            onResetPronunciation = onResetPronunciation,
                            settings = uiState.settings,
                        )
                    } else {
                        FusionLessonStage(
                            prompt = if (animatedOwnsTaskHeader) "" else animatedNode.prompt,
                            style = animatedStyle,
                            modifier = Modifier.fillMaxWidth(),
                            promptMaxLines = if (animatedNode is ShadowingNode) 1 else 4,
                            headerAction = if (
                                animatedOwnsTaskHeader ||
                                animatedNode.audio == PromptAudio.None ||
                                (animatedNode is SingleChoiceNode && !animatedNode.audio.autoPlay && animatedFeedback == null)
                            ) null else {
                                {
                                    LessonAudioButton(
                                        audio = animatedNode.audio,
                                        playbackState = audioController.playbackState,
                                        style = animatedStyle,
                                        onPlay = { cue ->
                                            audioController.play(cue, uiState.settings.ttsWorkerUrl)
                                        },
                                    )
                                }
                            },
                            heroContent = if (animatedOwnsTaskHeader) null else {
                                {
                                    LessonNodeHero(
                                        node = animatedNode,
                                        feedback = animatedFeedback,
                                        workSlug = uiState.selection.workSlug,
                                        episode = uiState.selection.episode,
                                        motionEnabled = motionEnabled,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            },
                        ) {
                            LessonNodeContent(
                                node = animatedNode,
                                workSlug = uiState.selection.workSlug,
                                autoSpeak = uiState.settings.autoSpeak,
                                feedback = animatedFeedback,
                                style = animatedStyle,
                                motionEnabled = motionEnabled,
                                playbackState = audioController.playbackState,
                                onSubmitAnswer = animatedSubmit,
                                onPlayAudio = { cue ->
                                    audioController.play(cue, uiState.settings.ttsWorkerUrl)
                                },
                                onSpeakText = { text ->
                                    audioController.speakText(text, uiState.settings.ttsWorkerUrl)
                                },
                                onPairFeedback = { correct ->
                                    feedbackEngine?.emit(
                                        if (correct) FeedbackEvent.AnswerCorrect(xp = 1) else FeedbackEvent.AnswerWrong,
                                    )
                                },
                                pronunciationEvaluation = uiState.pronunciationEvaluation,
                                onEvaluatePronunciation = onEvaluatePronunciation,
                                onRetryPronunciation = onRetryPronunciation,
                                onResetPronunciation = onResetPronunciation,
                                settings = uiState.settings,
                            )
                        }
                    }
                }
            }
            if (node is StudyCardNode && session.feedback == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    shadowElevation = 8.dp,
                ) {
                    JuicyLessonButton(
                        text = "记住了，继续",
                        onClick = { onSubmitAnswer(node.expectedAnswer) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        tone = visualStyle.actionTone,
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    )
                }
            }
            FeedbackDock(
                feedback = session.feedback,
                node = node,
                correctXp = 12,
                combo = session.currentStreak,
                feedbackEventId = (session.answered.toLong() shl 32) xor node.id.hashCode().toLong(),
                motionEnabled = motionEnabled,
                isLastQuestion = session.index >= session.nodes.lastIndex,
                onContinue = onContinue,
            )
        }
        AnimatedVisibility(
            visible = streakDelightVisible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FusionMidLessonStreakRive(
                combo = streakDelightCombo,
                motionEnabled = motionEnabled,
                modifier = Modifier.size(width = 320.dp, height = 220.dp),
            )
        }
    }
}

@Composable
private fun LessonNodeHero(
    node: LessonNode,
    feedback: AnswerFeedback?,
    workSlug: String,
    episode: Int,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val role = when {
        feedback?.correct == false -> CourseCharacterRole.Encouragement
        feedback?.correct == true -> CourseCharacterRole.Celebration
        node is ShadowingNode -> CourseCharacterRole.Shadowing
        node is SingleChoiceNode -> CourseCharacterRole.Linguistics
        node is StudyCardNode -> CourseCharacterRole.Grammar
        else -> CourseCharacterRole.Translation
    }
    CourseCharacterArtwork(
        workSlug = workSlug,
        role = role,
        motionEnabled = motionEnabled,
        stableSeed = episode,
        modifier = modifier,
    )
}

@Composable
private fun LessonTopBar(
    index: Int,
    total: Int,
    progress: Float,
    episodeLabel: String,
    exitLabel: String,
    @DrawableRes artworkRes: Int,
    style: FusionLessonVisualStyle,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, style.accent.copy(alpha = 0.28f)),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    color = style.softContainer,
                    contentColor = style.accentDark,
                    shape = CircleShape,
                ) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = exitLabel)
                    }
                }
                Image(
                    painter = painterResource(artworkRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(MaterialTheme.shapes.medium),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = "${index + 1} / $total  ·  ${style.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = episodeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = style.accentDark,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    color = style.softContainer,
                    contentColor = style.accentDark,
                    shape = CircleShape,
                ) {
                    Text(
                        text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            JuicyLessonProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(13.dp),
                heightDp = 11,
                milestoneVisible = progress >= 1f,
                pulsing = false,
                progressColor = style.accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }
    }
}

@Composable
private fun LessonAudioButton(
    audio: PromptAudio,
    playbackState: AudioPlaybackState,
    style: FusionLessonVisualStyle,
    onPlay: (PromptAudio) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (audio == PromptAudio.None) return
    Surface(
        modifier = modifier.size(50.dp),
        color = style.softContainer,
        contentColor = style.accentDark,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, style.accent.copy(alpha = 0.30f)),
    ) {
        IconButton(
            onClick = { onPlay(audio) },
            modifier = Modifier.semantics {
                contentDescription = playbackState.message.ifBlank { "播放语音" }
            },
        ) {
            if (playbackState.phase == AudioPlaybackPhase.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = style.accentDark,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

private fun shouldAutoPlayLessonAudio(audio: PromptAudio): Boolean {
    return when (audio) {
        PromptAudio.None -> false
        is PromptAudio.Tts -> audio.autoPlay
        is PromptAudio.Source -> audio.autoPlay
    }
}

@Composable
private fun LessonNodeContent(
    node: LessonNode,
    workSlug: String,
    autoSpeak: Boolean,
    feedback: AnswerFeedback?,
    style: FusionLessonVisualStyle,
    motionEnabled: Boolean,
    playbackState: AudioPlaybackState,
    onSubmitAnswer: (String) -> Unit,
    onPlayAudio: (PromptAudio) -> Unit,
    onSpeakText: (String) -> Unit,
    onPairFeedback: (Boolean) -> Unit,
    pronunciationEvaluation: PronunciationEvaluationState,
    onEvaluatePronunciation: (String, String, ByteArray, Long) -> Unit,
    onRetryPronunciation: () -> Unit,
    onResetPronunciation: () -> Unit,
    settings: LabSettings = LabSettings(),
) {
    when (node) {
        is StudyCardNode -> StudyCardNodeView(
            node = node,
            style = style,
            onPlayAudio = onPlayAudio,
        )
        is PairMatchNode -> PairMatchNodeView(
            node = node,
            workSlug = workSlug,
            autoSpeak = autoSpeak,
            disabled = feedback != null,
            style = style,
            motionEnabled = motionEnabled,
            onSubmitAnswer = onSubmitAnswer,
            onSpeakText = onSpeakText,
            onPairFeedback = onPairFeedback,
        )
        is SingleChoiceNode -> SingleChoiceNodeView(
            node = node,
            feedback = feedback,
            style = style,
            onSubmitAnswer = onSubmitAnswer,
        )
        is ClozeNode -> ClozeNodeView(
            node = node,
            feedback = feedback,
            style = style,
            playbackState = playbackState,
            onPlayAudio = onPlayAudio,
            onSubmitAnswer = onSubmitAnswer,
        )
        is TileOrderNode -> TileOrderNodeView(
            node = node,
            feedback = feedback,
            style = style,
            playbackState = playbackState,
            onPlayAudio = onPlayAudio,
            onSubmitAnswer = onSubmitAnswer,
        )
        is ShadowingNode -> ShadowingNodeView(
            node = node,
            feedback = feedback,
            style = style,
            onSubmitAnswer = onSubmitAnswer,
            pronunciationEvaluation = pronunciationEvaluation,
            onEvaluatePronunciation = onEvaluatePronunciation,
            onRetryPronunciation = onRetryPronunciation,
            onResetPronunciation = onResetPronunciation,
            settings = settings,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyCardNodeView(
    node: StudyCardNode,
    style: FusionLessonVisualStyle,
    onPlayAudio: (PromptAudio) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FusionStudyFlashcard(
            japanese = node.japanese,
            reading = node.reading,
            meaningZh = node.meaningZh,
            notes = node.notes,
            style = style,
            modifier = Modifier.fillMaxWidth(),
            onPlayAudio = if (node.audio == PromptAudio.None) null else {
                { onPlayAudio(node.audio) }
            },
        )
        LinguisticBonusSection(
            payload = node.linguistic,
            style = style,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Collapsed-by-default "语言学加餐" addendum for study cards. Purely explanatory: it never
 * gates the answer flow, so it stays quiet until the learner opens it.
 */
@Composable
private fun LinguisticBonusSection(
    payload: LinguisticCardPayload?,
    style: FusionLessonVisualStyle,
    modifier: Modifier = Modifier,
) {
    if (payload == null || !payload.hasContent) return
    var expanded by rememberSaveable(payload) { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val motionDuration = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion)
    val labColors = LabTheme.colors
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, style.accent.copy(alpha = 0.30f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = style.accentDark,
                )
                Text(
                    text = "语言学加餐",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = style.accentDark,
                )
                if (payload.level.isNotBlank()) {
                    Text(
                        text = payload.level,
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起语言学加餐" else "展开语言学加餐",
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                    tint = style.accentDark,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = motionDuration, easing = MotionTokens.Curve.Standard),
                ) + fadeIn(animationSpec = tween(durationMillis = motionDuration)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = motionDuration, easing = MotionTokens.Curve.Standard),
                ) + fadeOut(animationSpec = tween(durationMillis = motionDuration)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (payload.headlineZh.isNotBlank()) {
                        Text(
                            text = payload.headlineZh,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    payload.terms.forEach { term ->
                        if (term.termZh.isBlank() && term.plainZh.isBlank()) return@forEach
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (term.termZh.isNotBlank()) {
                                Text(
                                    text = term.termZh,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (term.plainZh.isNotBlank()) {
                                Text(
                                    text = term.plainZh,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mutedColor,
                                )
                            }
                        }
                    }
                    payload.domains.forEach { domain ->
                        val heading = listOf(linguisticDomainLabel(domain.domain), domain.titleZh)
                            .filter { it.isNotBlank() }
                            .joinToString(separator = " · ")
                        if (heading.isBlank() && domain.takeawayZh.isBlank() && domain.explanationZh.isBlank()) {
                            return@forEach
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (heading.isNotBlank()) {
                                Text(
                                    text = heading,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (domain.takeawayZh.isNotBlank()) {
                                Text(
                                    text = domain.takeawayZh,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = labColors.info,
                                )
                            }
                            if (domain.explanationZh.isNotBlank()) {
                                Text(
                                    text = domain.explanationZh,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mutedColor,
                                )
                            }
                        }
                    }
                    if (payload.historicalNoteZh.isNotBlank()) {
                        Text(
                            text = payload.historicalNoteZh,
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedColor,
                        )
                    }
                    if (payload.cautionZh.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = labColors.warningContainer,
                            contentColor = labColors.onWarningContainer,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                text = payload.cautionZh,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun linguisticDomainLabel(domain: String): String = when (domain.trim()) {
    "" -> ""
    "phonology" -> "音系学"
    "morphology" -> "形态学"
    "syntax" -> "句法学"
    "semantics" -> "语义学"
    "pragmatics" -> "语用学"
    "historical" -> "历史语言学"
    "sociolinguistics" -> "社会语言学"
    else -> domain.trim()
}

@Composable
private fun PairMatchNodeView(
    node: PairMatchNode,
    workSlug: String,
    autoSpeak: Boolean,
    disabled: Boolean,
    style: FusionLessonVisualStyle,
    motionEnabled: Boolean,
    onSubmitAnswer: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onPairFeedback: (Boolean) -> Unit,
) {
    var matched by rememberSaveable(node.id) { mutableStateOf(emptyList<String>()) }
    var selectedAudio by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    var wrongMeaning by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    val meaningItems = remember(node.id) { node.pairs.reversed() }
    val latestOnSpeakText by rememberUpdatedState(onSpeakText)

    LaunchedEffect(node.id) {
        val firstPair = node.pairs.firstOrNull()
        if (autoSpeak && firstPair != null && firstPair.audioText.isNotBlank()) {
            selectedAudio = firstPair.id
            latestOnSpeakText(firstPair.audioText)
        }
    }

    LaunchedEffect(wrongMeaning) {
        if (wrongMeaning != null) {
            delay(520)
            wrongMeaning = null
            selectedAudio = null
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp),
            contentAlignment = Alignment.Center,
        ) {
            CourseCharacterArtwork(
                workSlug = workSlug,
                role = CourseCharacterRole.Listening,
                motionEnabled = motionEnabled,
                modifier = Modifier.size(104.dp),
                stableSeed = node.id.hashCode(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            node.pairs.forEachIndexed { index, audioPair ->
                val meaningPair = meaningItems[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AudioMatchWaveCard(
                        waveRes = MatchWaveDrawables[index % MatchWaveDrawables.size],
                        revealText = audioPair.right.takeIf { matched.contains(audioPair.id) },
                        selected = selectedAudio == audioPair.id,
                        matched = matched.contains(audioPair.id),
                        enabled = !disabled && !matched.contains(audioPair.id),
                        style = style,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            selectedAudio = audioPair.id
                            if (audioPair.audioText.isNotBlank()) onSpeakText(audioPair.audioText)
                        },
                    )
                    PairMeaningCard(
                        text = meaningPair.left,
                        correct = matched.contains(meaningPair.id),
                        wrong = wrongMeaning == meaningPair.id,
                        enabled = !disabled && !matched.contains(meaningPair.id) && selectedAudio != null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            if (selectedAudio == meaningPair.id) {
                                matched = matched + meaningPair.id
                                selectedAudio = null
                                if (matched.size == node.pairs.size) {
                                    onSubmitAnswer(node.expectedAnswer)
                                } else {
                                    onPairFeedback(true)
                                }
                            } else {
                                wrongMeaning = meaningPair.id
                                onPairFeedback(false)
                            }
                        },
                    )
                }
            }
        }
    }
}

private val MatchWaveDrawables = listOf(
    com.animejapaneselab.nativeapp.R.drawable.listen_match_wave_1,
    com.animejapaneselab.nativeapp.R.drawable.listen_match_wave_2,
    com.animejapaneselab.nativeapp.R.drawable.listen_match_wave_3,
    com.animejapaneselab.nativeapp.R.drawable.listen_match_wave_4,
)

@Composable
private fun AudioMatchWaveCard(
    @DrawableRes waveRes: Int,
    revealText: String?,
    selected: Boolean,
    matched: Boolean,
    enabled: Boolean,
    style: FusionLessonVisualStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        matched -> LabTheme.colors.success
        selected -> style.accent
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val container = when {
        matched -> LabTheme.colors.successContainer
        selected -> style.softContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp),
        color = container,
        contentColor = if (matched) LabTheme.colors.onSuccessContainer else style.accentDark,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(2.dp, borderColor),
        shadowElevation = if (selected || matched) 1.dp else 3.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "播放这条日语语音",
                    modifier = Modifier.size(23.dp),
                )
                Image(
                    painter = painterResource(waveRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(style.accent),
                    modifier = Modifier
                        .weight(1f)
                        .height(25.dp),
                    contentScale = ContentScale.FillBounds,
                )
            }
            if (!revealText.isNullOrBlank()) {
                Text(
                    text = revealText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = LabTheme.colors.onSuccessContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PairMeaningCard(
    text: String,
    correct: Boolean,
    wrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        correct -> LabTheme.colors.success
        wrong -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val containerColor = when {
        correct -> LabTheme.colors.successContainer
        wrong -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        correct -> LabTheme.colors.onSuccessContainer
        wrong -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 84.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (correct || wrong) 2.dp else 1.5.dp, borderColor),
        shadowElevation = if (correct || wrong) 1.dp else 3.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (correct || wrong) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (correct) LabTheme.colors.success else MaterialTheme.colorScheme.error),
                )
            }
        }
    }
}

@Composable
private fun SingleChoiceNodeView(
    node: SingleChoiceNode,
    feedback: AnswerFeedback?,
    style: FusionLessonVisualStyle,
    onSubmitAnswer: (String) -> Unit,
) {
    var selected by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        node.body?.let {
            FusionLessonFocusCard(
                label = "台词线索",
                text = it,
                style = style,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        node.choices.forEachIndexed { index, choice ->
            ChoiceButton(
                text = choice,
                selected = selected == choice,
                correct = feedback != null && choice == node.answer,
                wrong = feedback != null && selected == choice && choice != node.answer,
                enabled = feedback == null,
                onClick = {
                    selected = choice
                },
                badgeLabel = ('A'.code + index).toChar().toString(),
                badgeColor = style.accent,
            )
        }
        if (feedback == null) {
            JuicyLessonButton(
                text = "确认答案",
                onClick = { selected?.let(onSubmitAnswer) },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
                tone = style.actionTone,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}

@Composable
private fun LessonSentenceCard(
    text: String,
    audio: PromptAudio,
    playbackState: AudioPlaybackState,
    style: FusionLessonVisualStyle,
    onPlayAudio: (PromptAudio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 96.dp),
        color = style.softContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, style.accent.copy(alpha = 0.42f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (audio == PromptAudio.None) 0.dp else 52.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            if (audio != PromptAudio.None) {
                LessonAudioButton(
                    audio = audio,
                    playbackState = playbackState,
                    style = style,
                    onPlay = onPlayAudio,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

@Composable
private fun ClozeNodeView(
    node: ClozeNode,
    feedback: AnswerFeedback?,
    style: FusionLessonVisualStyle,
    playbackState: AudioPlaybackState,
    onPlayAudio: (PromptAudio) -> Unit,
    onSubmitAnswer: (String) -> Unit,
) {
    var selected by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LessonSentenceCard(
            text = buildString {
                append(node.before)
                append(selected ?: "＿＿＿＿")
                append(node.after)
            },
            audio = node.audio,
            playbackState = playbackState,
            onPlayAudio = onPlayAudio,
            style = style,
            modifier = Modifier.fillMaxWidth(),
        )
        node.choices.forEachIndexed { index, choice ->
            ChoiceButton(
                text = choice.value,
                detail = choice.note.takeIf { feedback != null },
                selected = selected == choice.value,
                correct = feedback != null && choice.value == node.answer,
                wrong = feedback != null && selected == choice.value && choice.value != node.answer,
                enabled = feedback == null,
                onClick = { selected = choice.value },
                badgeLabel = (index + 1).toString(),
                badgeColor = style.accent,
            )
        }
        if (feedback == null) {
            JuicyLessonButton(
                text = "检查",
                onClick = { selected?.let(onSubmitAnswer) },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
                tone = style.actionTone,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TileOrderNodeView(
    node: TileOrderNode,
    feedback: AnswerFeedback?,
    style: FusionLessonVisualStyle,
    playbackState: AudioPlaybackState,
    onPlayAudio: (PromptAudio) -> Unit,
    onSubmitAnswer: (String) -> Unit,
) {
    var selected by rememberSaveable(node.id) { mutableStateOf(emptyList<String>()) }
    val disabled = feedback != null

    if (!node.audioTile) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LessonSentenceCard(
                text = node.displayText,
                audio = node.audio,
                playbackState = playbackState,
                onPlayAudio = onPlayAudio,
                style = style,
                modifier = Modifier.fillMaxWidth(),
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 158.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(158.dp)
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                ) {
                    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    Canvas(modifier = Modifier.matchParentSize()) {
                        listOf(0.31f, 0.61f, 0.91f).forEach { fraction ->
                            drawLine(
                                color = lineColor,
                                start = androidx.compose.ui.geometry.Offset(0f, size.height * fraction),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height * fraction),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        selected.forEachIndexed { index, tile ->
                            LearningTileButton(
                                text = tile,
                                onClick = { if (!disabled) selected = selected.toMutableList().also { it.removeAt(index) } },
                                selected = true,
                                enabled = !disabled,
                                accentColor = style.accent,
                            )
                        }
                    }
                }
            }
            if (!disabled) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        node.bankTiles.forEach { tile ->
                            val used = selected.count { it == tile }
                            val available = node.bankTiles.count { it == tile }
                            if (used < available) {
                                LearningTileButton(
                                    text = tile,
                                    onClick = { selected = selected + tile },
                                    accentColor = style.accent,
                                )
                            }
                        }
                    }
                }
                JuicyLessonButton(
                    text = "检查",
                    onClick = { onSubmitAnswer(selected.joinToString("")) },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    tone = style.actionTone,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (node.audio != PromptAudio.None) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LessonAudioButton(
                    audio = node.audio,
                    playbackState = playbackState,
                    style = style,
                    onPlay = onPlayAudio,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = style.softContainer,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.5.dp, style.accent.copy(alpha = 0.42f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (selected.isEmpty()) {
                        Text(
                            text = "＿＿＿＿＿＿＿＿",
                            color = style.accent.copy(alpha = 0.42f),
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    selected.forEachIndexed { index, tile ->
                        LearningTileButton(
                            text = tile,
                            onClick = { if (!disabled) selected = selected.toMutableList().also { it.removeAt(index) } },
                            selected = true,
                            enabled = !disabled,
                            accentColor = style.accent,
                        )
                    }
                }
            }
        }
        if (!disabled) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                node.bankTiles.forEach { tile ->
                    val used = selected.count { it == tile }
                    val available = node.bankTiles.count { it == tile }
                    if (used < available) {
                        LearningTileButton(
                            text = tile,
                            onClick = { selected = selected + tile },
                            accentColor = style.accent,
                        )
                    }
                }
            }
            JuicyLessonButton(
                text = "检查",
                onClick = { onSubmitAnswer(selected.joinToString("")) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                tone = style.actionTone,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}

@Composable
private fun ShadowingNodeView(
    node: ShadowingNode,
    feedback: AnswerFeedback?,
    style: FusionLessonVisualStyle,
    onSubmitAnswer: (String) -> Unit,
    pronunciationEvaluation: PronunciationEvaluationState,
    onEvaluatePronunciation: (String, String, ByteArray, Long) -> Unit,
    onRetryPronunciation: () -> Unit,
    onResetPronunciation: () -> Unit,
    settings: LabSettings = LabSettings(),
) {
    var selected by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    var useSelfAssessment by rememberSaveable(node.id) { mutableStateOf(false) }
    val furiganaAnnotator = rememberFuriganaAnnotator(settings)
    LaunchedEffect(node.sentence.ja, settings.showFurigana) {
        if (settings.showFurigana) {
            furiganaAnnotator.request("sentence", listOf(node.sentence.ja))
        }
    }
    val sentenceFurigana = if (settings.showFurigana) {
        furiganaAnnotator.resultFor(node.sentence.ja)
    } else {
        null
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShadowingSentenceCard(
            sentence = node.sentence,
            style = style,
            furigana = sentenceFurigana,
            showRomaji = settings.showRomaji,
            modifier = Modifier.fillMaxWidth(),
        )
        val pronunciationSentenceId = node.pronunciationSentenceId
        if (pronunciationSentenceId != null && !useSelfAssessment) {
            RealPronunciationAssessmentPanel(
                node = node,
                sentenceId = pronunciationSentenceId,
                evaluation = pronunciationEvaluation,
                answered = feedback != null,
                style = style,
                onEvaluatePronunciation = onEvaluatePronunciation,
                onRetryPronunciation = onRetryPronunciation,
                onResetPronunciation = onResetPronunciation,
                onSubmitAnswer = onSubmitAnswer,
                onUseSelfAssessment = {
                    onResetPronunciation()
                    useSelfAssessment = true
                },
            )
            return@Column
        }
        node.ratings.forEachIndexed { index, rating ->
            ChoiceButton(
                text = rating,
                detail = when (index) {
                    0 -> "节奏、停顿和语气都很接近"
                    1 -> "基本跟上，还有细节可调整"
                    else -> "先听一遍，再重新模仿"
                },
                selected = selected == rating,
                correct = feedback != null && selected == rating && feedback.correct,
                wrong = feedback != null && selected == rating && !feedback.correct,
                enabled = feedback == null,
                onClick = {
                    selected = rating
                    onSubmitAnswer(rating)
                },
                badgeLabel = (index + 1).toString(),
                badgeColor = style.accent,
            )
        }
    }
}

/**
 * Shadowing prompt card: mirrors [FusionLessonFocusCard]'s chrome but renders the Japanese
 * line through [RubyText] so furigana can sit above the kanji, and carries the source-audio /
 * difficulty / tone badges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShadowingSentenceCard(
    sentence: ShadowingSentence,
    style: FusionLessonVisualStyle,
    furigana: FuriganaResult?,
    showRomaji: Boolean,
    modifier: Modifier = Modifier,
) {
    val labColors = LabTheme.colors
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val toneTags = sentence.toneTags.filter { it.isNotBlank() }.take(2)
    Surface(
        modifier = modifier,
        color = style.softContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, style.accent.copy(alpha = 0.42f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "角色声线",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = style.accentDark,
                )
                if (sentence.hasSourceAudio) {
                    ShadowingBadge(
                        text = "原声",
                        containerColor = labColors.successContainer,
                        contentColor = labColors.onSuccessContainer,
                    )
                }
                if (sentence.difficulty.isNotBlank()) {
                    ShadowingBadge(
                        text = sentence.difficulty,
                        containerColor = labColors.infoContainer,
                        contentColor = labColors.onInfoContainer,
                    )
                }
            }
            RubyText(
                text = sentence.ja,
                furigana = furigana,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                rubyColor = mutedColor,
            )
            if (sentence.reading.isNotBlank()) {
                Text(
                    text = sentence.reading,
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (showRomaji && sentence.romaji.isNotBlank()) {
                Text(
                    text = sentence.romaji,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor.copy(alpha = 0.78f),
                )
            }
            if (toneTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    toneTags.forEach { tag -> TagChip(text = tag) }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            ) {
                Text(
                    text = sentence.meaningZh,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun ShadowingBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun RealPronunciationAssessmentPanel(
    node: ShadowingNode,
    sentenceId: String,
    evaluation: PronunciationEvaluationState,
    answered: Boolean,
    style: FusionLessonVisualStyle,
    onEvaluatePronunciation: (String, String, ByteArray, Long) -> Unit,
    onRetryPronunciation: () -> Unit,
    onResetPronunciation: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onUseSelfAssessment: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember(node.id) { PronunciationWavRecorder() }
    val activeEvaluation = evaluation.takeIf { it.nodeId == node.id } ?: PronunciationEvaluationState()
    var recording by remember(node.id) { mutableStateOf(false) }
    var stopping by remember(node.id) { mutableStateOf(false) }
    var elapsedMs by remember(node.id) { mutableLongStateOf(0L) }
    var localMessage by remember(node.id) { mutableStateOf("") }
    val latestOnEvaluate by rememberUpdatedState(onEvaluatePronunciation)
    val latestOnReset by rememberUpdatedState(onResetPronunciation)

    fun startRecordingNow() {
        if (recording || stopping || activeEvaluation.phase == PronunciationEvaluationPhase.Loading) return
        latestOnReset()
        localMessage = ""
        elapsedMs = 0L
        runCatching { recorder.start() }
            .onSuccess { recording = true }
            .onFailure { localMessage = "无法启动麦克风，请检查系统录音权限后重试。" }
    }

    val latestStartRecording by rememberUpdatedState<() -> Unit> { startRecordingNow() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            latestStartRecording()
        } else {
            localMessage = "需要麦克风权限才能进行真实发音测评。"
        }
    }

    fun requestRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecordingNow()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAndEvaluate() {
        if (!recording || stopping) return
        recording = false
        stopping = true
        scope.launch {
            val capture = try {
                Result.success(withContext(Dispatchers.IO) { recorder.stop() })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }
            stopping = false
            capture.fold(
                onSuccess = { audio ->
                    elapsedMs = audio.durationMs
                    if (audio.durationMs < PronunciationMinimumDurationMs) {
                        localMessage = "录音太短，请完整读完这句话。"
                    } else {
                        localMessage = ""
                        latestOnEvaluate(
                            node.id,
                            sentenceId,
                            audio.wavBytes,
                            audio.durationMs,
                        )
                    }
                },
                onFailure = {
                    localMessage = "录音处理失败，请重新录制。"
                },
            )
        }
    }

    val latestStopAndEvaluate by rememberUpdatedState<() -> Unit> { stopAndEvaluate() }
    LaunchedEffect(recording, node.id) {
        if (!recording) return@LaunchedEffect
        val startedAt = android.os.SystemClock.elapsedRealtime()
        while (recording && elapsedMs < PronunciationMaximumDurationMs) {
            delay(100)
            elapsedMs = (android.os.SystemClock.elapsedRealtime() - startedAt)
                .coerceAtMost(PronunciationMaximumDurationMs)
        }
        if (recording) latestStopAndEvaluate()
    }
    DisposableEffect(recorder) {
        onDispose { recorder.cancel() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = style.softContainer.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(2.dp, style.accent.copy(alpha = 0.42f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "真实发音测评",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "录音仅用于本次即时识别与评分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = LabTheme.colors.warningContainer,
                    contentColor = LabTheme.colors.onWarningContainer,
                    shape = CircleShape,
                ) {
                    Text(
                        text = "BETA",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            when {
                recording -> PronunciationRecordingContent(
                    elapsedMs = elapsedMs,
                    accent = style.accent,
                    onStop = ::stopAndEvaluate,
                )

                stopping -> PronunciationLoadingContent("正在封装 16 kHz WAV 录音…")

                activeEvaluation.phase == PronunciationEvaluationPhase.Loading -> {
                    PronunciationLoadingContent(activeEvaluation.message)
                }

                activeEvaluation.phase == PronunciationEvaluationPhase.Error -> {
                    PronunciationRetryContent(
                        message = activeEvaluation.message,
                        canRetryUpload = activeEvaluation.canRetry,
                        onRetryUpload = onRetryPronunciation,
                        onRecordAgain = {
                            onResetPronunciation()
                            requestRecording()
                        },
                    )
                }

                activeEvaluation.result != null -> {
                    PronunciationResultContent(
                        result = activeEvaluation.result,
                        message = activeEvaluation.message,
                        answered = answered,
                        style = style,
                        onRecordAgain = {
                            onResetPronunciation()
                            requestRecording()
                        },
                        onSubmitAnswer = onSubmitAnswer,
                    )
                }

                activeEvaluation.phase == PronunciationEvaluationPhase.Complete || localMessage.isNotBlank() -> {
                    PronunciationRetryContent(
                        message = localMessage.ifBlank { activeEvaluation.message },
                        canRetryUpload = false,
                        onRetryUpload = {},
                        onRecordAgain = {
                            onResetPronunciation()
                            requestRecording()
                        },
                    )
                }

                else -> {
                    Text(
                        text = "听完原声后，点击录音并完整读出这句日语。系统会返回体验评分、判断可靠度和需要关注的片段。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    JuicyLessonButton(
                        text = "开始录音",
                        onClick = ::requestRecording,
                        modifier = Modifier.fillMaxWidth(),
                        tone = JuicyLessonTone.Blue,
                        trailingIcon = Icons.Rounded.Mic,
                    )
                    Text(
                        text = "请录制 0.4–15 秒；建议在安静环境中距离麦克风约一掌。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (!answered) {
                OutlinedButton(
                    onClick = onUseSelfAssessment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text("跳过测评，改用自评继续", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PronunciationRecordingContent(
    elapsedMs: Long,
    accent: Color,
    onStop: () -> Unit,
) {
    val seconds = elapsedMs / 1_000L
    val tenths = (elapsedMs % 1_000L) / 100L
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "正在录音  $seconds.$tenths 秒",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Black,
        )
        LinearProgressIndicator(
            progress = { (elapsedMs / PronunciationMaximumDurationMs.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeCap = StrokeCap.Round,
        )
        JuicyLessonButton(
            text = "结束录音并测评",
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            tone = JuicyLessonTone.Red,
            trailingIcon = Icons.Rounded.Stop,
        )
    }
}

@Composable
private fun PronunciationLoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "票据在录音完成后即时申请，通常需要几秒钟。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PronunciationRetryContent(
    message: String,
    canRetryUpload: Boolean,
    onRetryUpload: () -> Unit,
    onRecordAgain: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (canRetryUpload) {
            JuicyLessonButton(
                text = "重试上传当前录音",
                onClick = onRetryUpload,
                modifier = Modifier.fillMaxWidth(),
                tone = JuicyLessonTone.Blue,
                trailingIcon = Icons.Rounded.Replay,
            )
        }
        OutlinedButton(
            onClick = onRecordAgain,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null)
            Text("重新录音", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PronunciationResultContent(
    result: PronunciationEvaluation,
    message: String,
    answered: Boolean,
    style: FusionLessonVisualStyle,
    onRecordAgain: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
) {
    when (result.assessmentStatus) {
        PronunciationAssessmentStatus.Scored -> {
            val score = result.score
            if (score == null) {
                PronunciationRetryContent(
                    message = "服务没有返回完整分数，请重新录制。",
                    canRetryUpload = false,
                    onRetryUpload = {},
                    onRecordAgain = onRecordAgain,
                )
                return
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(92.dp),
                        color = style.accent,
                        contentColor = Color.White,
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = score.overall.toString(),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Black,
                                )
                                Text("体验分", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = pronunciationBandLabel(score.band),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "判断可靠度 ${(result.engine.reliability * 100).toInt().coerceIn(0, 100)}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "分数和可靠度是两个独立指标",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                PronunciationScoreGrid(result)
                result.recognized?.text?.takeIf(String::isNotBlank)?.let { recognized ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("系统听到", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                            Text(recognized, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                val issues = result.segments.filter { it.needsAttention }.take(3)
                if (issues.isNotEmpty()) {
                    Text("需要关注", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    issues.forEach { segment -> PronunciationSegmentRow(segment) }
                }
                result.feedback.take(3).forEach { item ->
                    Text("• $item", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!answered) {
                    JuicyLessonButton(
                        text = "采用结果，完成本题",
                        onClick = { onSubmitAnswer(score.lessonRating) },
                        modifier = Modifier.fillMaxWidth(),
                        tone = JuicyLessonTone.Green,
                        trailingIcon = Icons.Rounded.Check,
                    )
                    OutlinedButton(
                        onClick = onRecordAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Rounded.Replay, contentDescription = null)
                        Text("再测一次", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        PronunciationAssessmentStatus.Uncertain,
        PronunciationAssessmentStatus.ReRecord -> PronunciationRetryContent(
            message = message,
            canRetryUpload = false,
            onRetryUpload = {},
            onRecordAgain = onRecordAgain,
        )
    }
}

@Composable
private fun PronunciationScoreGrid(result: PronunciationEvaluation) {
    val score = result.score ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PronunciationMetric("准确", score.accuracy, Modifier.weight(1f))
            PronunciationMetric("完整", score.completeness, Modifier.weight(1f))
            PronunciationMetric("清晰", score.clarity, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PronunciationMetric("流利", score.fluency, Modifier.weight(1f))
            PronunciationMetric(
                if (result.reference.usesEstimatedTiming) "节奏参考" else "节奏",
                score.rhythm,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PronunciationMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PronunciationSegmentRow(segment: com.animejapaneselab.nativeapp.data.PronunciationSegment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LabTheme.colors.warningContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, LabTheme.colors.warning.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = segment.surface.ifBlank { segment.expected },
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Black,
                )
                segment.score?.let { Text("$it 分", fontWeight = FontWeight.Black) }
            }
            val detail = segment.message.ifBlank {
                when (segment.status) {
                    "omission" -> "疑似漏读这一段"
                    "extra" -> "疑似多读或重复"
                    "pause" -> "这一段停顿可能偏长"
                    else -> "这一段读音疑似不够清楚"
                }
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun pronunciationBandLabel(band: String): String = when (band) {
    "excellent" -> "非常接近原声"
    "good" -> "整体读得不错"
    "fair" -> "基本跟上了"
    else -> "建议再练一次"
}

@Composable
private fun ChoiceButton(
    text: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    selected: Boolean,
    correct: Boolean,
    wrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    badgeLabel: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.secondary,
) {
    JuicyLessonChoiceCard(
        text = text,
        detail = detail,
        onClick = { if (enabled) onClick() },
        modifier = modifier,
        selected = selected,
        correct = correct,
        wrong = wrong,
        answered = !enabled,
        selectionColor = badgeColor,
        selectionContainer = badgeColor.copy(alpha = 0.12f),
        leadingContent = badgeLabel?.let { label ->
            {
                Surface(
                    modifier = Modifier.size(34.dp),
                    color = badgeColor.copy(alpha = if (selected || correct || wrong) 1f else 0.14f),
                    contentColor = if (selected || correct || wrong) Color.White else badgeColor,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun FeedbackDock(
    feedback: AnswerFeedback?,
    node: LessonNode,
    correctXp: Int,
    combo: Int,
    feedbackEventId: Long,
    motionEnabled: Boolean,
    isLastQuestion: Boolean,
    onContinue: () -> Unit,
) {
    if (feedback == null) {
        return
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val color = if (feedback.correct) LabTheme.colors.successContainer else MaterialTheme.colorScheme.errorContainer
        val darkColor = if (feedback.correct) LabTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
        val buttonTone = if (feedback.correct) JuicyLessonTone.Green else JuicyLessonTone.Red
        val clipboard = LocalClipboardManager.current
        var copyStatus by remember(feedback.selected, node.id) { mutableStateOf(false) }
        val actionLabel = if (isLastQuestion) "查看结果" else "继续"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            color = color,
            contentColor = darkColor,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.5.dp, darkColor.copy(alpha = 0.28f)),
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(62.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            FusionDrawableHost(
                                resolution = AnimeLabFusionDrawableResolver.resolveDrawable(
                                    if (feedback.correct) {
                                        FusionVisualKey.LessonAnswerCorrectIcon
                                    } else {
                                        FusionVisualKey.LessonAnswerWrongIcon
                                    },
                                ),
                                modifier = Modifier.size(38.dp),
                                fallback = {
                                    Icon(
                                        if (feedback.correct) Icons.Rounded.Check else Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(34.dp),
                                        tint = darkColor,
                                    )
                                },
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (feedback.correct) "不错哦！" else "不正确",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                            )
                            if (feedback.correct) {
                                Surface(
                                    color = LabTheme.colors.xpContainer,
                                    contentColor = LabTheme.colors.xp,
                                    shape = CircleShape,
                                ) {
                                    Text(
                                        text = "+$correctXp XP",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }
                        FeedbackSummary(feedback = feedback)
                    }
                }
                if (!feedback.correct) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(buildExternalQuestionPrompt(node, feedback)))
                            copyStatus = true
                        },
                        modifier = Modifier.heightIn(min = 38.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = darkColor),
                        border = BorderStroke(1.dp, darkColor.copy(alpha = 0.54f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        Text(
                            text = if (copyStatus) "已复制题目" else "复制题目",
                            modifier = Modifier.padding(start = 4.dp),
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    JuicyLessonButton(
                        text = actionLabel,
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        tone = buttonTone,
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    )
                    if (feedback.correct) {
                        FusionCtaLightningRive(
                            big = combo >= 5,
                            eventId = feedbackEventId,
                            motionEnabled = motionEnabled,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackSummary(feedback: AnswerFeedback) {
    val showExpected = feedback.expected != "studied" && !feedback.expected.contains('=')
    if (showExpected) {
        Text(
            text = "正确答案：${feedback.expected}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val explanation = feedback.explanation.trim()
    val repeatsAnswer = explanation.startsWith("正确答案：${feedback.expected}")
    if (explanation.isNotBlank() && !repeatsAnswer) {
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LessonNode.audioText(): String {
    return when (this) {
        is StudyCardNode -> japanese
        is SingleChoiceNode -> body ?: answer
        is ClozeNode -> before + answer + after
        is TileOrderNode -> targetTiles.joinToString("")
        is ShadowingNode -> sentence.ja
        is PairMatchNode -> pairs.firstOrNull()?.right.orEmpty()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonComplete(
    uiState: LabUiState,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedbackEngine = LocalFeedbackEngine.current
    val answered = uiState.lesson.answered
    val accuracy = if (answered == 0) 0 else (uiState.lesson.correct * 100 / answered)
    val hasMistakes = uiState.lesson.correct < answered
    val exitLabel = lessonExitLabel(uiState.selectedTab)
    val summaryMessage = if (hasMistakes) {
        "错题已经进入复习页，下一轮会优先补弱项。"
    } else {
        "本轮全对，下一轮可以继续挑战更后面的内容。"
    }
    val practicedTypes = uiState.lesson.nodes
        .map { it.typeLabel }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)
    LaunchedEffect(answered, uiState.settings.feedbackSounds) {
        delay(260)
        feedbackEngine?.emit(FeedbackEvent.LessonComplete)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(82.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                Text(
                    text = "本轮完成",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = uiState.focus.lessonTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summaryMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            RewardMetricCard(
                label = "XP",
                value = "+${uiState.sessionXp}",
                icon = Icons.Rounded.Stars,
                progress = 1f,
                modifier = Modifier.weight(1f),
                highlighted = true,
            )
            RewardMetricCard(
                label = "正确率",
                value = "$accuracy%",
                icon = Icons.Rounded.Check,
                progress = accuracy / 100f,
                modifier = Modifier.weight(1f),
            )
            RewardMetricCard(
                label = "能量",
                value = "${uiState.focus.energy}/5",
                icon = Icons.Rounded.LocalFireDepartment,
                progress = uiState.focus.energy / 5f,
                modifier = Modifier.weight(1f),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "训练结算",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                SettlementLine("完成题数", "${uiState.lesson.answered} / ${uiState.lesson.nodes.size}")
                SettlementLine("正确题数", "${uiState.lesson.correct} 题 · 正确率 $accuracy%")
                SettlementLine("新增掌握内容", "${uiState.lesson.correct.coerceAtLeast(0)} 个")
                SettlementLine("本次主要错误类型", lessonErrorTypeSummary(uiState))
                SettlementLine("系统已安排的后续复习", if (hasMistakes) "错题已进入复盘队列" else "暂无新增错题")
                SettlementLine("下一步建议", if (uiState.hasNextLessonBatch) "继续下一批材料" else "回到复盘页查看弱点")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    practicedTypes.forEach { type ->
                        TagChip(type)
                    }
                }
            }
        }

        if (uiState.hasNextLessonBatch) {
            Button(
                onClick = onNextBatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text("继续下一批", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
            }
        } else {
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = null)
                Text("再练一轮", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
            }
        }
        OutlinedButton(
            onClick = if (uiState.hasNextLessonBatch) onRestart else onExit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(if (uiState.hasNextLessonBatch) "再练本轮" else exitLabel, fontWeight = FontWeight.Black)
        }
        if (uiState.hasNextLessonBatch) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(exitLabel, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SettlementLine(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun lessonErrorTypeSummary(uiState: LabUiState): String {
    if (uiState.lesson.correct >= uiState.lesson.answered) return "本轮没有明显错误"
    val activeTypes = uiState.lesson.nodes
        .map { it.typeLabel }
        .filter { it.isNotBlank() }
        .distinct()
    return when {
        activeTypes.any { it.contains("填空") || it.contains("语法") } -> "语法功能误判"
        activeTypes.any { it.contains("听") || it.contains("跟读") } -> "听力/跟读不稳"
        activeTypes.any { it.contains("语言") || it.contains("读空气") } -> "潜台词判断"
        else -> "词义混淆"
    }
}

private fun lessonExitLabel(tab: LabTab): String {
    return when (tab) {
        LabTab.Library -> "返回本集入口"
        LabTab.Today -> "返回今日页"
        LabTab.Review -> "返回错题页"
        else -> "返回训练入口"
    }
}
