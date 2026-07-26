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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.EpisodeOption
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
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.LearningChoiceButton
import com.animejapaneselab.nativeapp.ui.components.LearningTileButton
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton
import com.animejapaneselab.nativeapp.ui.components.RewardMetricCard
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LearningAssetRegistry
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.feedback.LocalRiveMascotController
import com.animejapaneselab.nativeapp.ui.feedback.VisualAsset
import com.animejapaneselab.nativeapp.ui.fusion.AnimeLabFusionDrawableResolver
import com.animejapaneselab.nativeapp.ui.fusion.AnimeLabFusionRollout
import com.animejapaneselab.nativeapp.ui.fusion.FusionDrawableHost
import com.animejapaneselab.nativeapp.ui.fusion.FusionVisualKey
import com.animejapaneselab.nativeapp.ui.motion.AnimatedAnswerOption
import com.animejapaneselab.nativeapp.ui.motion.AnimatedLessonNode
import com.animejapaneselab.nativeapp.ui.motion.AnswerOptionState
import com.animejapaneselab.nativeapp.ui.motion.LessonPageTransition
import com.animejapaneselab.nativeapp.ui.motion.LessonNodeVisualState
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.PressablePrimaryButton
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.RubyText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.rive.DuolingoLikeVisualHost
import com.animejapaneselab.nativeapp.ui.rive.FusionCtaLightningRive
import com.animejapaneselab.nativeapp.ui.rive.FusionMidLessonStreakRive
import com.animejapaneselab.nativeapp.ui.rive.RiveAnimationHost
import com.animejapaneselab.nativeapp.ui.theme.LabPalette
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LessonHubScreen(
    uiState: LabUiState,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode, Int, String) -> Unit,
    onStartExercise: (LessonExerciseKind) -> Unit,
    onStartExerciseMix: () -> Unit,
    onStartReadAir: (Int) -> Unit,
    onOpenReadAir: () -> Unit,
    onStartReview: () -> Unit,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pathPlan = remember(uiState) { buildTrainingPathPlan(uiState.trainingPathInput()) }
    val stageSummary = remember(uiState, pathPlan) { uiState.courseStageSummary(pathPlan) }
    val exerciseLabUi = remember(uiState) { uiState.exerciseLabUiState() }
    val feedbackEngine = LocalFeedbackEngine.current
    val reducedMotion = rememberReducedMotion()
    val motionEnabled = uiState.settings.richAnimationsEnabled && !reducedMotion
    val courseArtworkRes = LearningAssetRegistry.courseBannerArtworkFor(
        uiState.selection.workSlug,
        uiState.selection.episode,
    )
    var showCourseSwitcher by rememberSaveable { mutableStateOf(false) }
    var showCourseDirectory by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showCourseSwitcher || showCourseDirectory) {
        if (showCourseDirectory) {
            showCourseDirectory = false
        } else {
            showCourseSwitcher = false
        }
    }
    val handleNodeSelected: (TrainingPathNode) -> Unit = { node ->
        val action = node.action
        feedbackEngine?.emit(action.trainingPathFeedbackEvent())
        when (action) {
            TrainingPathNodeAction.Mixed -> {
                onStartModeLesson(LessonMode.Mixed, node.batch, node.key)
            }

            TrainingPathNodeAction.Vocab -> {
                onStartModeLesson(LessonMode.Vocab, node.batch, node.key)
            }

            TrainingPathNodeAction.Grammar -> {
                onStartModeLesson(LessonMode.Grammar, node.batch, node.key)
            }

            TrainingPathNodeAction.Shadowing -> {
                onStartModeLesson(LessonMode.Shadowing, node.batch, node.key)
            }

            TrainingPathNodeAction.ReadAir -> onStartReadAir(node.batch)
            TrainingPathNodeAction.Review -> onStartReview()
            TrainingPathNodeAction.NextEpisode -> onEpisodeSelected(uiState.selection.episode + 1)
            TrainingPathNodeAction.None -> Unit
        }
    }
    val handleCourseSelected: (CourseSwitcherItem) -> Unit = { item ->
        feedbackEngine?.emit(FeedbackEvent.OptionSelect)
        showCourseSwitcher = false
        if (item.workSlug != uiState.selection.workSlug) {
            onWorkSelected(item.workSlug)
        }
        item.startEpisode?.takeIf { it > 0 && it != uiState.selection.episode }?.let(onEpisodeSelected)
    }
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                DuolingoPathTopStats(
                    plan = pathPlan,
                    stageSummary = stageSummary,
                    onCourseClick = {
                        feedbackEngine?.emit(FeedbackEvent.TapSecondary)
                        showCourseSwitcher = true
                    },
                )
            }
            item {
                DuolingoStageBanner(
                    summary = stageSummary,
                    onDirectoryClick = {
                        feedbackEngine?.emit(FeedbackEvent.OptionSelect)
                        showCourseDirectory = true
                    },
                ) {
                    CourseStageArtwork(
                        artworkRes = courseArtworkRes,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            item {
                AnimeExerciseLabSection(
                    uiState = exerciseLabUi,
                    workSlug = uiState.selection.workSlug,
                    motionEnabled = motionEnabled,
                    loading = uiState.exerciseLabLoading,
                    coursePalette = lessonCoursePalette(uiState.selection.workSlug),
                    onStartExercise = onStartExercise,
                    onStartMix = onStartExerciseMix,
                )
            }
            val episodePlan = uiState.episodePlan
            if (episodePlan != null && episodePlan.hasPlanSummary()) {
                item { EpisodePlanSummaryCard(plan = episodePlan) }
            }
            item { TrainingPathSectionHeader(plan = pathPlan) }
            item {
                DuolingoPathMap(
                    plan = pathPlan,
                    workSlug = uiState.selection.workSlug,
                    motionEnabled = motionEnabled,
                    onNodeSelected = handleNodeSelected,
                )
            }
        }
        if (showCourseSwitcher) {
            CourseSwitcherOverlay(
                uiState = uiState,
                onDismiss = { showCourseSwitcher = false },
                onCourseSelected = handleCourseSelected,
            )
        }
        if (showCourseDirectory) {
            CourseDirectoryOverlay(
                uiState = uiState,
                onDismiss = { showCourseDirectory = false },
                onEpisodeSelected = { episode ->
                    feedbackEngine?.emit(FeedbackEvent.OptionSelect)
                    onEpisodeSelected(episode)
                    showCourseDirectory = false
                },
                onStartLesson = {
                    showCourseDirectory = false
                    onStartLesson()
                },
            )
        }
    }
}

@Composable
private fun AnimeExerciseLabSection(
    uiState: ExerciseLabUiState,
    workSlug: String,
    motionEnabled: Boolean,
    loading: Boolean,
    coursePalette: FusionCoursePalette,
    onStartExercise: (LessonExerciseKind) -> Unit,
    onStartMix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("题型实验室", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "六种自由训练 · 完成后可继续下一组",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                onClick = onStartMix,
                enabled = !loading && uiState.totalMaterialCount > 0,
                color = coursePalette.accent,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Text(
                    if (loading) "准备中…" else "六类快练",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(LessonExerciseKind.entries, key = { it.name }) { kind ->
                ExerciseLabCard(
                    kind = kind,
                    stats = uiState.stats.getValue(kind),
                    workSlug = workSlug,
                    motionEnabled = motionEnabled,
                    enabled = !loading && uiState.stats.getValue(kind).totalCount > 0,
                    coursePalette = coursePalette,
                    onClick = { onStartExercise(kind) },
                )
            }
        }
    }
}

private data class ExerciseLabCardStyle(
    val icon: ImageVector,
)

private data class ExerciseLabUiState(
    val coveredCount: Int,
    val totalMaterialCount: Int,
    val stats: Map<LessonExerciseKind, ExerciseLabKindUiState>,
)

private data class ExerciseLabKindUiState(
    val coveredCount: Int,
    val totalCount: Int,
)

private fun LabUiState.exerciseLabUiState(): ExerciseLabUiState {
    val selectedWork = normalizeTrainingPathWorkSlug(selection.workSlug)
    val scopedProgress = progressItems.filter { item ->
        normalizeTrainingPathWorkSlug(item.workSlug) == selectedWork &&
            item.episode == selection.episode
    }
    val latestByMaterial = linkedMapOf<Pair<String, String>, com.animejapaneselab.nativeapp.data.ProgressItem>()
    scopedProgress.forEach { item ->
        val raw = listOf(item.payload["sourceId"], item.payload["source_id"], item.payload["source"])
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
            .ifBlank { item.itemId }
        raw.split(',').map(String::trim).filter(String::isNotBlank).forEach { sourceId ->
            val key = item.itemType to sourceId
            val existing = latestByMaterial[key]
            if (existing == null || item.lastReviewedAt > existing.lastReviewedAt) {
                latestByMaterial[key] = item
            }
        }
    }
    val completedByType = latestByMaterial.filterValues { item ->
        item.state == ReviewState.Good || item.state == ReviewState.Known
    }.keys
    val vocabIds = vocab.map { it.id }.toSet()
    val grammarIds = grammar.map { it.id }.toSet()
    val sentenceIds = shadowing.map { it.id }.toSet()
    fun covered(type: String, ids: Set<String>): Int = ids.count { id -> type to id in completedByType }
    val vocabCovered = covered("vocab", vocabIds)
    val grammarCovered = covered("grammar", grammarIds)
    val sentenceCovered = covered("sentence", sentenceIds)
    return ExerciseLabUiState(
        coveredCount = vocabCovered + grammarCovered + sentenceCovered,
        totalMaterialCount = vocabIds.size + grammarIds.size + sentenceIds.size,
        stats = mapOf(
            LessonExerciseKind.TranslationOrder to ExerciseLabKindUiState(sentenceCovered, sentenceIds.size),
            LessonExerciseKind.PairMatch to ExerciseLabKindUiState(vocabCovered, vocabIds.size),
            LessonExerciseKind.SingleChoice to ExerciseLabKindUiState(vocabCovered, vocabIds.size),
            LessonExerciseKind.Cloze to ExerciseLabKindUiState(grammarCovered, grammarIds.size),
            LessonExerciseKind.AudioOrder to ExerciseLabKindUiState(sentenceCovered, sentenceIds.size),
            LessonExerciseKind.Shadowing to ExerciseLabKindUiState(sentenceCovered, sentenceIds.size),
        ),
    )
}

@Composable
private fun TrainingPathSectionHeader(
    plan: TrainingPathPlan,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("本集学习路径", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "覆盖完整词库 ${plan.fullVocabCount} · 核心 ${plan.coreVocabCount} 优先",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TagChip("${plan.completedPathNodeCount}/${plan.totalPathNodeCount} 节点", selected = true)
            }
            LinearProgressIndicator(
                progress = { plan.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

private fun EpisodePlan.hasPlanSummary(): Boolean {
    return vocabCount > 0 || shadowingCount > 0 || grammarCount > 0 || exerciseCount > 0 || notes.isNotBlank()
}

private data class EpisodePlanTile(
    val label: String,
    val count: Int,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodePlanSummaryCard(
    plan: EpisodePlan,
    modifier: Modifier = Modifier,
) {
    val labColors = LabTheme.colors
    val tiles = listOf(
        EpisodePlanTile(
            label = "词汇",
            count = plan.vocabCount,
            icon = Icons.Rounded.AutoStories,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        EpisodePlanTile(
            label = "跟读",
            count = plan.shadowingCount,
            icon = Icons.Rounded.Mic,
            containerColor = labColors.infoContainer,
            contentColor = labColors.onInfoContainer,
        ),
        EpisodePlanTile(
            label = "语法",
            count = plan.grammarCount,
            icon = Icons.Rounded.Psychology,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        EpisodePlanTile(
            label = "练习",
            count = plan.exerciseCount,
            icon = Icons.Rounded.Bolt,
            containerColor = labColors.successContainer,
            contentColor = labColors.onSuccessContainer,
        ),
    ).filter { it.count > 0 }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(
                    text = "本集计划",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                plan.planSlot?.let { slot ->
                    TagChip("第 $slot 期")
                }
            }
            if (tiles.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tiles.forEach { tile -> EpisodePlanCountPill(tile = tile) }
                }
            }
            if (plan.notes.isNotBlank()) {
                Text(
                    text = plan.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EpisodePlanCountPill(
    tile: EpisodePlanTile,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = tile.containerColor,
        contentColor = tile.contentColor,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(tile.icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = tile.count.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ExerciseLabCard(
    kind: LessonExerciseKind,
    stats: ExerciseLabKindUiState,
    workSlug: String,
    motionEnabled: Boolean,
    enabled: Boolean,
    coursePalette: FusionCoursePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = kind.exerciseLabStyle()
    Surface(
        modifier = modifier
            .width(206.dp)
            .height(192.dp),
        color = coursePalette.softContainer,
        contentColor = LabPalette.Ink,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(2.dp, coursePalette.accent.copy(alpha = 0.42f)),
        shadowElevation = 3.dp,
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 80.dp, height = 74.dp),
                contentAlignment = Alignment.Center,
            ) {
                ExerciseLabArtwork(
                    kind = kind,
                    workSlug = workSlug,
                    motionEnabled = motionEnabled,
                    fallbackIcon = style.icon,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Surface(
                    color = coursePalette.accent,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(18.dp),
                    )
                }
                Text(
                    text = kind.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = kind.shortDescription,
                    style = MaterialTheme.typography.labelMedium,
                    color = LabPalette.Muted,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (stats.totalCount > 0) {
                        "素材进度 ${stats.coveredCount}/${stats.totalCount} · 本轮最多 6 题"
                    } else {
                        "本集暂无可练材料"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = coursePalette.accent,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExerciseLabArtwork(
    kind: LessonExerciseKind,
    workSlug: String,
    motionEnabled: Boolean,
    fallbackIcon: ImageVector,
) {
    val fallback: @Composable () -> Unit = {
        Icon(fallbackIcon, contentDescription = null, modifier = Modifier.size(34.dp))
    }
    when (kind) {
        LessonExerciseKind.AudioOrder -> DuolingoLikeVisualHost(
            asset = VisualAsset.Lottie("listening_waveform_speaker", iterations = Int.MAX_VALUE),
            motionEnabled = motionEnabled,
            modifier = Modifier.fillMaxSize(),
            fallback = fallback,
        )

        else -> CourseCharacterArtwork(
            workSlug = workSlug,
            role = kind.courseCharacterRole(),
            motionEnabled = motionEnabled,
            modifier = Modifier.fillMaxSize(),
            stableSeed = kind.ordinal,
        )
    }
}

private fun LessonExerciseKind.courseCharacterRole(): CourseCharacterRole {
    return when (this) {
        LessonExerciseKind.TranslationOrder -> CourseCharacterRole.Translation
        LessonExerciseKind.PairMatch -> CourseCharacterRole.Listening
        LessonExerciseKind.SingleChoice -> CourseCharacterRole.Linguistics
        LessonExerciseKind.Cloze -> CourseCharacterRole.Grammar
        LessonExerciseKind.AudioOrder -> CourseCharacterRole.Listening
        LessonExerciseKind.Shadowing -> CourseCharacterRole.Shadowing
    }
}

private fun LessonExerciseKind.exerciseLabStyle(): ExerciseLabCardStyle {
    return when (this) {
        LessonExerciseKind.TranslationOrder -> ExerciseLabCardStyle(
            icon = Icons.Rounded.AutoStories,
        )
        LessonExerciseKind.PairMatch -> ExerciseLabCardStyle(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
        )
        LessonExerciseKind.SingleChoice -> ExerciseLabCardStyle(
            icon = Icons.Rounded.Psychology,
        )
        LessonExerciseKind.Cloze -> ExerciseLabCardStyle(
            icon = Icons.Rounded.Bolt,
        )
        LessonExerciseKind.AudioOrder -> ExerciseLabCardStyle(
            icon = Icons.Rounded.PlayArrow,
        )
        LessonExerciseKind.Shadowing -> ExerciseLabCardStyle(
            icon = Icons.Rounded.Stars,
        )
    }
}

private fun TrainingPathNodeAction.trainingPathFeedbackEvent(): FeedbackEvent {
    return when (this) {
        TrainingPathNodeAction.Mixed -> FeedbackEvent.LessonNodeUnlock
        TrainingPathNodeAction.Vocab,
        TrainingPathNodeAction.Grammar,
        TrainingPathNodeAction.Shadowing,
        TrainingPathNodeAction.ReadAir -> FeedbackEvent.OptionSelect
        TrainingPathNodeAction.Review -> FeedbackEvent.ReviewScheduled(count = 1)
        TrainingPathNodeAction.NextEpisode -> FeedbackEvent.OptionSelect
        TrainingPathNodeAction.None -> FeedbackEvent.TapSecondary
    }
}

private data class CourseEpisodeRange(
    val start: Int,
    val end: Int,
)

private data class CourseStageSummary(
    val eyebrow: String,
    val title: String,
    val detail: String,
    val progress: Float,
    val courseStat: String,
)

private data class CourseSwitcherItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val workSlug: String,
    val startEpisode: Int?,
    val range: CourseEpisodeRange,
    val selected: Boolean,
    val artworkRes: Int,
    val accent: Color,
)

@Composable
private fun CourseSwitcherOverlay(
    uiState: LabUiState,
    onDismiss: () -> Unit,
    onCourseSelected: (CourseSwitcherItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val courseItems = remember(uiState) {
        buildCourseSwitcherItems(uiState).let { items ->
            items.filter { it.selected } + items.filterNot { it.selected }
        }
    }
    val selected = courseItems.firstOrNull { it.selected } ?: courseItems.firstOrNull()
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(role = Role.Button, onClick = onDismiss),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CourseSwitcherHud(
                    selected = selected,
                    stageSummary = uiState.courseStageSummary(),
                    uiState = uiState,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    items(courseItems, key = { it.id }) { item ->
                        CourseSwitcherCard(
                            item = item,
                            onClick = { onCourseSelected(item) },
                        )
                    }
                }
                selected?.let {
                    CourseSwitcherProgressCard(
                        item = it,
                        uiState = uiState,
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseSwitcherHud(
    selected: CourseSwitcherItem?,
    stageSummary: CourseStageSummary,
    uiState: LabUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DuolingoStatPill(icon = "EP", value = stageSummary.courseStat, color = MaterialTheme.colorScheme.onSurface)
        DuolingoHudStat(value = uiState.focus.streakDays.coerceAtLeast(0).toString(), color = LabTheme.colors.streak) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = LabTheme.colors.streak, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun CourseSwitcherCard(
    item: CourseSwitcherItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(132.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(width = 116.dp, height = 82.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(
                width = if (item.selected) 4.dp else 2.dp,
                color = if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(item.artworkRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    color = item.accent,
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = courseSwitcherBadge(item),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = if (item.selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CourseSwitcherProgressCard(
    item: CourseSwitcherItem,
    uiState: LabUiState,
    modifier: Modifier = Modifier,
) {
    val progress = courseRangeProgressFromDatabase(uiState, item.workSlug, item.range)
    val leftLabel = "EP${episodeNumberLabel(item.range.start)}"
    val rightLabel = "EP${episodeNumberLabel(item.range.end)}"
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(leftLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(22.dp)
                            .clip(CircleShape)
                            .background(item.accent),
                    )
                }
                Text(rightLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            Text(
                text = "${item.title} · 当前 ${uiState.focus.episodeLabel}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "点右侧书本查看当前作品 EP 选集",
                style = MaterialTheme.typography.titleSmall,
                color = item.accent,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun CourseDirectoryOverlay(
    uiState: LabUiState,
    onDismiss: () -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onStartLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val episodes = remember(uiState) { buildCourseDirectoryEpisodes(uiState) }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CourseDirectoryTopBar(
                title = "${shortCourseTitle(uiState.focus.workTitle)} 选集",
                onDismiss = onDismiss,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(episodes, key = { it.key }) { episode ->
                    if (episode.selected) {
                        CourseDirectoryFeaturedEpisode(
                            episode = episode,
                            uiState = uiState,
                            onClick = onStartLesson,
                        )
                    } else {
                        CourseDirectoryCompactEpisode(
                            episode = episode,
                            onClick = { onEpisodeSelected(episode.episode) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseDirectoryTopBar(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp),
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "关闭课程目录", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(38.dp))
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

private data class CourseDirectoryEpisode(
    val key: String,
    val workSlug: String,
    val episode: Int,
    val selected: Boolean,
    val completed: Boolean,
    val progress: Float,
    val seasonLabel: String,
    val title: String,
    val subtitle: String,
    val rangeLabel: String,
    val materialLabel: String,
)

@Composable
private fun CourseDirectoryCompactEpisode(
    episode: CourseDirectoryEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(126.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "EP${episodeNumberLabel(episode.episode)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CourseRangePill(episode.rangeLabel)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                if (episode.progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (episode.completed) 1f else episode.progress.coerceIn(0.08f, 1f))
                            .height(16.dp)
                            .clip(CircleShape)
                            .background(lessonCoursePalette(episode.workSlug).accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseDirectoryFeaturedEpisode(
    episode: CourseDirectoryEpisode,
    uiState: LabUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(336.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 3.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp),
            ) {
                Image(
                    painter = painterResource(LearningAssetRegistry.courseBannerArtworkFor(episode.workSlug, episode.episode)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                0.48f to MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                                1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.10f),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 22.dp)
                        .fillMaxWidth(0.66f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = episode.seasonLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "EP${episodeNumberLabel(episode.episode)} ${episode.title}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "EP${episodeNumberLabel(episode.episode)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    CourseRangePill(episode.rangeLabel)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    if (episode.progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(episode.progress.coerceIn(0.08f, 1f))
                                .height(20.dp)
                                .clip(CircleShape)
                                .background(lessonCoursePalette(episode.workSlug).accent),
                        )
                    }
                }
                Text(
                    text = episode.materialLabel.ifBlank {
                        val plan = uiState.episodePlan
                        if (plan != null) {
                            "核心词 ${plan.vocabCount} · 语法 ${plan.grammarCount} · 跟读 ${plan.shadowingCount} · 题库 ${plan.exerciseCount}"
                        } else {
                            "本集材料正在同步"
                        }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CourseRangePill(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

private fun buildCourseSwitcherItems(uiState: LabUiState): List<CourseSwitcherItem> {
    return uiState.works.flatMap { work ->
        val count = effectiveEpisodeCountForWork(work, uiState)
        val ranges = courseSwitcherRangesFor(work.slug, count)
        ranges.mapIndexed { index, range ->
            val selected = normalizeTrainingPathWorkSlug(work.slug) == normalizeTrainingPathWorkSlug(uiState.selection.workSlug) &&
                uiState.selection.episode in range.start..range.end
            CourseSwitcherItem(
                id = "${work.slug}-${index + 1}",
                title = courseSwitcherTitle(work, index, ranges.size),
                subtitle = if (ranges.size == 1) {
                    "$count 集"
                } else {
                    "EP${episodeNumberLabel(range.start)}-${episodeNumberLabel(range.end)}"
                },
                workSlug = work.slug,
                startEpisode = if (selected) null else range.start,
                range = range,
                selected = selected,
                artworkRes = LearningAssetRegistry.courseArtworkFor(work.slug, range.start),
                accent = lessonCoursePalette(work.slug).accent,
            )
        }
    }
}

private fun courseSwitcherRangesFor(workSlug: String, episodeCount: Int): List<CourseEpisodeRange> {
    val count = episodeCount.coerceAtLeast(1)
    if (normalizeTrainingPathWorkSlug(workSlug) == "re-zero" && count > 30) {
        return listOf(
            CourseEpisodeRange(1, 25.coerceAtMost(count)),
            CourseEpisodeRange(26, 50.coerceAtMost(count)),
            CourseEpisodeRange(51.coerceAtMost(count), count),
        ).filter { it.start <= it.end }
    }
    return listOf(CourseEpisodeRange(1, count))
}

private fun buildCourseDirectoryEpisodes(uiState: LabUiState): List<CourseDirectoryEpisode> {
    val work = uiState.currentWorkOption()
    val normalizedWork = normalizeTrainingPathWorkSlug(work.slug)
    val count = effectiveEpisodeCountForWork(work, uiState)
    val loadedEpisodes = uiState.episodes.associateBy { it.episode }
    return (1..count).map { episodeNumber ->
        val loaded = loadedEpisodes[episodeNumber]
        val selected = episodeNumber == uiState.selection.episode
        val episodeProgress = uiState.courseEpisodeProgress(work.slug, episodeNumber)
        val completed = episodeProgress >= 0.999f
        val title = episodeDisplayTitle(normalizedWork, episodeNumber)
        val plan = uiState.episodePlan.takeIf { selected }
        val readAirCount = if (selected) {
            uiState.readAir.exercises.count { exercise ->
                normalizeTrainingPathWorkSlug(exercise.workSlug) == normalizedWork &&
                    exercise.episode == episodeNumber
            }
        } else {
            0
        }
        val materialLabel = when {
            selected -> uiState.currentMaterialSummary()
            loaded != null -> "台词 ${loaded.totalCues} · 可学台词 ${loaded.usableJaLines} · 场景 ${loaded.chunkCount}"
            else -> "等待导入本集材料"
        }
        CourseDirectoryEpisode(
            key = "${work.slug}-$episodeNumber",
            workSlug = work.slug,
            episode = episodeNumber,
            selected = selected,
            completed = completed,
            progress = episodeProgress,
            seasonLabel = courseEpisodeSeasonLabel(normalizedWork, episodeNumber),
            title = title,
            subtitle = "${shortCourseTitle(work.displayName)} EP${episodeNumberLabel(episodeNumber)} $title",
            rangeLabel = loaded?.let { "台词 ${it.totalCues.coerceAtLeast(0)}" } ?: "EP${episodeNumberLabel(episodeNumber)}",
            materialLabel = materialLabel,
        )
    }
}

private fun LabUiState.courseStageSummary(
    pathPlan: TrainingPathPlan = buildTrainingPathPlan(trainingPathInput()),
): CourseStageSummary {
    val selectionWork = normalizeTrainingPathWorkSlug(selection.workSlug)
    val detail = currentMaterialSummary(pathPlan)
    val shortEpisodeTitle = "${shortCourseTitle(focus.workTitle)} EP${episodeNumberLabel(selection.episode)}"
    return CourseStageSummary(
        eyebrow = courseEpisodeSeasonLabel(selectionWork, selection.episode),
        title = shortEpisodeTitle,
        detail = detail,
        progress = pathPlan.progress,
        courseStat = "本集路径 ${pathPlan.completedPathNodeCount}/${pathPlan.totalPathNodeCount}",
    )
}

private fun LabUiState.currentWorkOption(): WorkOption {
    return works.firstOrNull {
        normalizeTrainingPathWorkSlug(it.slug) == normalizeTrainingPathWorkSlug(selection.workSlug)
    } ?: WorkOption(
        id = selection.workSlug,
        slug = selection.workSlug,
        displayName = focus.workTitle,
        episodeCount = listOfNotNull(
            episodes.maxOfOrNull { it.episode },
            knownCourseEpisodeCount(selection.workSlug),
            selection.episode,
        ).maxOrNull()?.coerceAtLeast(1) ?: 1,
    )
}

private fun LabUiState.currentMaterialSummary(
    pathPlan: TrainingPathPlan = buildTrainingPathPlan(trainingPathInput()),
): String {
    val pathInput = trainingPathInput()
    val plan = pathInput.episodePlan
    val grammarTotal = effectiveUiMaterialCount(pathInput.grammarCount, plan?.grammarCount, plan?.grammarPointIds)
    val shadowingTotal = effectiveUiMaterialCount(pathInput.shadowingCount, plan?.shadowingCount, plan?.shadowingSentenceIds)
    val exerciseTotal = effectiveUiMaterialCount(pathInput.exerciseCount, plan?.exerciseCount, plan?.exerciseIds)
    val extensionCount = (pathPlan.fullVocabCount - pathPlan.coreVocabCount).coerceAtLeast(0)
    return buildString {
        append("本集词库 ${pathPlan.fullVocabCount} · 核心优先 ${pathPlan.coreVocabCount}")
        append("\n扩展 $extensionCount · 语法 $grammarTotal · 跟读 $shadowingTotal · 题库 $exerciseTotal")
    }
}

private fun effectiveUiMaterialCount(actualCount: Int, planCount: Int?, planIds: List<String>?): Int {
    return maxOf(actualCount.coerceAtLeast(0), planCount ?: 0, planIds.orEmpty().distinct().size)
}

private fun effectiveEpisodeCountForWork(work: WorkOption, uiState: LabUiState): Int {
    val normalizedWork = normalizeTrainingPathWorkSlug(work.slug)
    val currentWork = normalizeTrainingPathWorkSlug(uiState.selection.workSlug)
    val loadedEpisodeMax = if (normalizedWork == currentWork) {
        uiState.episodes.maxOfOrNull { it.episode }
    } else {
        null
    }
    return listOfNotNull(
        loadedEpisodeMax,
        knownCourseEpisodeCount(work.slug),
        work.episodeCount,
        if (normalizedWork == currentWork) uiState.selection.episode else null,
    ).maxOrNull()?.coerceAtLeast(1) ?: 1
}

private fun knownCourseEpisodeCount(workSlug: String): Int? {
    return when (normalizeTrainingPathWorkSlug(workSlug)) {
        "k-on" -> 14
        "re-zero" -> 66
        else -> null
    }
}

private fun courseSwitcherTitle(work: WorkOption, index: Int, rangeCount: Int): String {
    return if (rangeCount == 1) {
        work.displayName
    } else {
        "${shortCourseTitle(work.displayName)} S${index + 1}"
    }
}

private fun courseSwitcherBadge(item: CourseSwitcherItem): String {
    return when (normalizeTrainingPathWorkSlug(item.workSlug)) {
        "k-on" -> "K"
        "re-zero" -> "S${seasonIndexFromId(item.id)}"
        else -> "JP"
    }
}

private fun shortCourseTitle(title: String): String {
    return when {
        title.contains("Re:", ignoreCase = true) || title.contains("ゼロ") -> "Re:Zero"
        title.contains("K-ON", ignoreCase = true) -> "K-ON!"
        title.length > 10 -> title.take(10)
        else -> title
    }
}

private fun LabUiState.courseEpisodeProgress(workSlug: String, episode: Int): Float {
    val normalizedWork = normalizeTrainingPathWorkSlug(workSlug)
    val completedFromDatabase = progressItems.count { item ->
        normalizeTrainingPathWorkSlug(item.workSlug) == normalizedWork &&
            item.episode == episode &&
            (item.state == com.animejapaneselab.nativeapp.data.ReviewState.Good ||
                item.state == com.animejapaneselab.nativeapp.data.ReviewState.Known)
    }
    val selected = normalizeTrainingPathWorkSlug(selection.workSlug) == normalizedWork && selection.episode == episode
    if (selected) {
        return buildTrainingPathPlan(trainingPathInput()).progress
    }
    val loaded = episodes.firstOrNull { item ->
        item.episode == episode && normalizeTrainingPathWorkSlug(item.workSlug) == normalizedWork
    }
    val denominator = when {
        loaded != null -> loaded.usableJaLines
            .coerceAtLeast(loaded.chunkCount)
            .coerceAtLeast(completedFromDatabase)
            .coerceAtLeast(1)
        else -> completedFromDatabase.coerceAtLeast(1)
    }
    val databaseProgress = (completedFromDatabase.toFloat() / denominator.toFloat()).coerceIn(0f, 1f)
    return databaseProgress
}

private fun courseRangeProgressFromDatabase(uiState: LabUiState, workSlug: String, range: CourseEpisodeRange): Float {
    val episodes = (range.start..range.end).toList()
    if (episodes.isEmpty()) return 0f
    return episodes
        .map { episode -> uiState.courseEpisodeProgress(workSlug, episode) }
        .average()
        .toFloat()
        .coerceIn(0f, 1f)
}

private fun episodeNumberLabel(value: Int): String = value.toString().padStart(2, '0')

private fun courseEpisodeSeasonLabel(workSlug: String, episode: Int): String {
    return when (normalizeTrainingPathWorkSlug(workSlug)) {
        "re-zero" -> when (episode) {
            in 1..25 -> "Re:Zero Season 1"
            in 26..50 -> "Re:Zero Season 2"
            else -> "Re:Zero Season 3"
        }
        "k-on" -> "K-ON! Season 1"
        else -> "日语课程"
    }
}

private fun episodeDisplayTitle(workSlug: String, episode: Int): String {
    return when (normalizeTrainingPathWorkSlug(workSlug)) {
        "k-on" -> kOnEpisodeTitles[episode]
        "re-zero" -> reZeroEpisodeTitles[episode]
        else -> null
    } ?: "第 ${episodeNumberLabel(episode)} 集"
}

private fun seasonIndexFromId(id: String): String = id.substringAfterLast("-", "1")

private val kOnEpisodeTitles = mapOf(
    1 to "廃部!",
    2 to "楽器!",
    3 to "特訓!",
    4 to "合宿!",
    5 to "顧問!",
    6 to "学園祭!",
    7 to "クリスマス!",
    8 to "新歓!",
    9 to "新入部員!",
    10 to "また合宿!",
    11 to "ピンチ!",
    12 to "軽音!",
    13 to "冬の日!",
    14 to "ライブハウス!",
)

private val reZeroEpisodeTitles = mapOf(
    1 to "始まりの終わりと終わりの始まり",
    2 to "再会の魔女",
    3 to "ゼロから始まる異世界生活",
    4 to "ロズワール邸の団欒",
    5 to "約束した朝は遠く",
    6 to "鎖の音",
    7 to "ナツキ・スバルのリスタート",
    8 to "泣いて泣き喚いて泣き止んだから",
    9 to "勇気の意味",
    10 to "鬼がかったやり方",
    11 to "レム",
    12 to "再来の王都",
    13 to "自称騎士ナツキ・スバル",
    14 to "絶望という病",
    15 to "狂気の外側",
    16 to "豚の欲望",
    17 to "醜態の果てに",
    18 to "ゼロから",
    19 to "白鯨攻略戦",
    20 to "ヴィルヘルム・ヴァン・アストレア",
    21 to "絶望に抗う賭け",
    22 to "怠惰一閃",
    23 to "悪辣なる怠惰",
    24 to "自称騎士と最優の騎士",
    25 to "ただそれだけの物語",
    26 to "それぞれの誓い",
)

@Composable
private fun DuolingoPathTopStats(
    plan: TrainingPathPlan,
    stageSummary: CourseStageSummary,
    onCourseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val streakValue = plan.streakLabel.takeWhile { it.isDigit() }.ifBlank { "0" }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DuolingoStatPill(
            icon = "EP",
            value = stageSummary.title.substringAfterLast(' '),
            color = MaterialTheme.colorScheme.onSurface,
            onClick = onCourseClick,
        )
        DuolingoHudStat(value = streakValue, color = LabTheme.colors.streak) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = LabTheme.colors.streak, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun DuolingoStatPill(
    icon: String,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = if (onClick == null) modifier else modifier.clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        if (onClick != null) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun DuolingoHudStat(
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun DuolingoStageBanner(
    summary: CourseStageSummary,
    onDirectoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(146.dp)
            .shadow(5.dp, MaterialTheme.shapes.extraLarge),
        color = LabTheme.colors.heroGradientStart,
        contentColor = LabTheme.colors.onHero,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(LabTheme.heroBrush()),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 22.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = summary.eyebrow,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = LabTheme.colors.onHero.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = summary.detail,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = LabTheme.colors.onHero.copy(alpha = 0.76f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = summary.courseStat,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = LabTheme.colors.onHero.copy(alpha = 0.88f),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(LabTheme.colors.onHero.copy(alpha = 0.26f)),
                    ) {
                        if (summary.progress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(summary.progress.coerceIn(0f, 1f))
                                    .height(9.dp)
                                    .clip(CircleShape)
                                    .background(LabTheme.colors.onHero.copy(alpha = 0.92f)),
                            )
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .width(104.dp)
                    .fillMaxSize(),
                color = LabTheme.colors.heroGradientEnd,
                contentColor = LabTheme.colors.onHero,
                onClick = onDirectoryClick,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    trailingContent()
                }
            }
        }
    }
}

@Composable
private fun CourseStageArtwork(
    @DrawableRes artworkRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(artworkRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            LabTheme.colors.heroGradientStart.copy(alpha = 0.48f),
                        ),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(38.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                FusionDrawableHost(
                    resolution = AnimeLabFusionDrawableResolver.resolveDrawable(
                        FusionVisualKey.TrainingPathGuidebookIcon,
                    ),
                    modifier = Modifier.size(24.dp),
                    fallback = {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(23.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DuolingoPathMap(
    plan: TrainingPathPlan,
    workSlug: String,
    motionEnabled: Boolean,
    onNodeSelected: (TrainingPathNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val useFusionVisuals = AnimeLabFusionRollout.current.useFusionTrainingPathVisuals
    val mapHeight = (plan.nodes.size * 154).coerceAtLeast(760).dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight),
    ) {
        DuolingoPathRoute(
            nodeCount = plan.nodes.size,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            plan.nodes.forEachIndexed { index, node ->
                DuolingoPathNodeSlot(
                    node = node,
                    index = index,
                    motionEnabled = motionEnabled,
                    useFusionVisuals = useFusionVisuals,
                    workSlug = workSlug,
                    onClick = { onNodeSelected(node) },
                )
            }
        }
    }
}

@Composable
private fun DuolingoPathRoute(
    nodeCount: Int,
    modifier: Modifier = Modifier,
) {
    val routeColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        if (nodeCount < 2) return@Canvas
        val route = Path()
        val verticalStep = 154.dp.toPx()
        val firstCenterY = 45.dp.toPx()
        repeat(nodeCount) { index ->
            val x = size.width / 2f + pathNodeXOffset(index).toPx()
            val y = firstCenterY + verticalStep * index
            if (index == 0) route.moveTo(x, y) else route.lineTo(x, y)
        }
        drawPath(
            path = route,
            color = routeColor,
            style = Stroke(
                width = 9.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(4.dp.toPx(), 13.dp.toPx()),
                ),
            ),
        )
    }
}

@Composable
private fun DuolingoPathNodeSlot(
    node: TrainingPathNode,
    index: Int,
    motionEnabled: Boolean,
    useFusionVisuals: Boolean,
    workSlug: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val xOffset = pathNodeXOffset(index)
    val visual = node.pathNodeVisual()
    val companionAlignment = if (xOffset <= 0.dp) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(144.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            node.state == TrainingPathNodeState.Current || node.state == TrainingPathNodeState.ReviewDue -> {
                CourseCharacterArtwork(
                    workSlug = workSlug,
                    role = CourseCharacterRole.PathActive,
                    motionEnabled = motionEnabled,
                    stableSeed = index,
                    modifier = Modifier
                        .align(companionAlignment)
                        .offset(y = (-4).dp)
                        .size(112.dp),
                )
            }

            node.state == TrainingPathNodeState.Locked &&
                (index % 8 == 3 || index % 8 == 6) -> {
                CourseCharacterArtwork(
                    workSlug = workSlug,
                    role = CourseCharacterRole.PathLocked,
                    motionEnabled = false,
                    stableSeed = index,
                    modifier = Modifier
                        .align(companionAlignment)
                        .offset(y = (-2).dp)
                        .size(104.dp)
                        .graphicsLayer(alpha = 0.74f),
                )
            }
        }
        Column(
            modifier = Modifier.offset(x = xOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DuolingoPathNodeButton(
                node = node,
                visual = visual,
                onClick = onClick,
                visualContent = {
                    if (node.state == TrainingPathNodeState.Reward) {
                        if (useFusionVisuals) {
                            FusionDrawableHost(
                                resolution = AnimeLabFusionDrawableResolver.resolveDrawable(
                                    FusionVisualKey.TrainingPathLockedRewardChest,
                                ),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(52.dp),
                                fallback = {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(30.dp),
                                    )
                                },
                            )
                        } else {
                            DuolingoLikeVisualHost(
                                asset = LearningAssetRegistry.rewardChest,
                                motionEnabled = motionEnabled,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp),
                                fallback = {
                                    Icon(
                                        Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(34.dp),
                                    )
                                },
                            )
                        }
                    } else {
                        Icon(
                            node.pathIcon(),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(34.dp),
                        )
                    }
                },
            )
            DuolingoPathNodeLabel(node = node, visual = visual)
        }
    }
}

private fun pathNodeXOffset(index: Int) = when (index % 5) {
    0 -> 0.dp
    1 -> (-52).dp
    2 -> 74.dp
    3 -> (-18).dp
    else -> 86.dp
}

@Composable
private fun DuolingoPathNodeLabel(
    node: TrainingPathNode,
    visual: PathNodeVisual,
    modifier: Modifier = Modifier,
) {
    val active = node.state == TrainingPathNodeState.Current ||
        node.state == TrainingPathNodeState.Available ||
        node.state == TrainingPathNodeState.ReviewDue
    val detail = when {
        node.scopeLabel.isNotBlank() && node.state == TrainingPathNodeState.Completed -> "${node.scopeLabel} · 已完成"
        node.scopeLabel.isNotBlank() -> "${node.scopeLabel} · ${node.countLabel}"
        else -> node.countLabel
    }
    Surface(
        modifier = modifier.width(190.dp),
        color = if (active) visual.face else MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        contentColor = if (active) visual.content else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (active) 3.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DuolingoPathNodeButton(
    node: TrainingPathNode,
    visual: PathNodeVisual,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualContent: @Composable BoxScope.() -> Unit,
) {
    val enabled = node.action != TrainingPathNodeAction.None && node.state != TrainingPathNodeState.Locked
    val highlighted = node.state == TrainingPathNodeState.Current || node.state == TrainingPathNodeState.ReviewDue
    val nodeSize = if (highlighted) 86.dp else 76.dp
    val faceSize = if (highlighted) 78.dp else 68.dp
    Box(
        modifier = modifier
            .size(nodeSize)
            .semantics(mergeDescendants = true) {
                contentDescription = "${node.title}，${node.scopeLabel}，${node.state.label}，${node.completedCount}/${node.totalCount}"
            }
            .minimumPathTouchTarget(enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (highlighted) {
            Surface(
                modifier = Modifier.size(86.dp),
                color = visual.face.copy(alpha = 0.13f),
                shape = CircleShape,
            ) {}
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 7.dp)
                .size(faceSize),
            color = visual.lip,
            shape = CircleShape,
        ) {}
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(faceSize),
            color = visual.face,
            contentColor = visual.content,
            shape = CircleShape,
            shadowElevation = if (highlighted) 5.dp else 1.dp,
            border = if (highlighted) BorderStroke(7.dp, MaterialTheme.colorScheme.surface) else null,
        ) {
            Box(contentAlignment = Alignment.Center) {
                visualContent()
            }
        }
    }
}

private data class PathNodeVisual(
    val face: Color,
    val lip: Color,
    val content: Color,
)

@Composable
private fun TrainingPathNode.pathNodeVisual(): PathNodeVisual {
    return when (state) {
        TrainingPathNodeState.Current -> PathNodeVisual(
            face = LabPalette.Violet,
            lip = LabPalette.VioletDark,
            content = Color.White,
        )

        TrainingPathNodeState.Completed -> PathNodeVisual(
            face = LabPalette.Green,
            lip = LabPalette.GreenDark,
            content = Color.White,
        )

        TrainingPathNodeState.Available -> PathNodeVisual(
            face = LabPalette.Violet,
            lip = LabPalette.VioletDark,
            content = Color.White,
        )

        TrainingPathNodeState.ReviewDue -> PathNodeVisual(
            face = LabPalette.Violet,
            lip = LabPalette.VioletDark,
            content = Color.White,
        )

        TrainingPathNodeState.Reward -> PathNodeVisual(
            face = LabPalette.Gold,
            lip = LabPalette.Yellow,
            content = LabPalette.Ink,
        )

        TrainingPathNodeState.Locked -> PathNodeVisual(
            face = MaterialTheme.colorScheme.surfaceContainerHighest,
            lip = MaterialTheme.colorScheme.outlineVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
    }
}

private fun Modifier.minimumPathTouchTarget(
    enabled: Boolean,
    onClick: () -> Unit,
): Modifier {
    return if (!enabled) {
        this
    } else {
        clickable(role = Role.Button, onClick = onClick)
    }
}

@Composable
private fun TrainingPathOrb(
    node: TrainingPathNode,
    modifier: Modifier = Modifier,
) {
    val colors = node.trainingPathColors()
    Surface(
        modifier = modifier.size(if (node.state == TrainingPathNodeState.Current) 68.dp else 58.dp),
        color = colors.container,
        contentColor = colors.content,
        shape = CircleShape,
        shadowElevation = if (node.state == TrainingPathNodeState.Current) 6.dp else 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(node.pathIcon(), contentDescription = null, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun TrainingPathNodeBubble(
    node: TrainingPathNode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = node.trainingPathColors()
    val enabled = node.action != TrainingPathNodeAction.None && node.state != TrainingPathNodeState.Locked
    if (enabled) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            color = colors.softContainer,
            contentColor = colors.softContent,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, colors.container.copy(alpha = 0.42f)),
        ) {
            TrainingPathNodeBubbleContent(node = node)
        }
    } else {
        Surface(
            modifier = modifier,
            color = colors.softContainer,
            contentColor = colors.softContent,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, colors.container.copy(alpha = 0.28f)),
        ) {
            TrainingPathNodeBubbleContent(node = node)
        }
    }
}

@Composable
private fun TrainingPathNodeBubbleContent(node: TrainingPathNode) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = node.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TagChip(node.countLabel, selected = node.state != TrainingPathNodeState.Locked)
        }
        Text(
            text = node.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = node.state.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun LessonPathOrbContent(
    done: Boolean,
    current: Boolean,
    locked: Boolean,
    pendingFeedback: Boolean,
    node: LessonNode,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = when {
                done -> Icons.Rounded.CheckCircle
                pendingFeedback -> Icons.AutoMirrored.Rounded.ArrowForward
                current -> Icons.Rounded.PlayArrow
                locked -> Icons.Rounded.Lock
                else -> node.pathIcon()
            },
            contentDescription = null,
        )
    }
}

private fun LessonNode.pathIcon() = when (typeLabel) {
    "语言学题" -> Icons.Rounded.Psychology
    "读空气" -> Icons.Rounded.Psychology
    "学习卡" -> Icons.Rounded.AutoStories
    else -> Icons.Rounded.Bolt
}

private val TrainingPathNodeState.label: String
    get() = when (this) {
        TrainingPathNodeState.Completed -> "已完成"
        TrainingPathNodeState.Current -> "当前关卡"
        TrainingPathNodeState.Available -> "可选专项"
        TrainingPathNodeState.ReviewDue -> "需要回炉"
        TrainingPathNodeState.Locked -> "暂未解锁"
        TrainingPathNodeState.Reward -> "奖励宝箱"
    }

private fun TrainingPathNode.pathIcon(): ImageVector {
    return when (action) {
        TrainingPathNodeAction.Mixed -> Icons.Rounded.Stars
        TrainingPathNodeAction.Vocab -> Icons.Rounded.AutoStories
        TrainingPathNodeAction.Grammar -> Icons.Rounded.Bolt
        TrainingPathNodeAction.Shadowing -> Icons.AutoMirrored.Rounded.VolumeUp
        TrainingPathNodeAction.ReadAir -> Icons.Rounded.Psychology
        TrainingPathNodeAction.Review -> Icons.Rounded.Replay
        TrainingPathNodeAction.NextEpisode -> Icons.AutoMirrored.Rounded.ArrowForward
        TrainingPathNodeAction.None -> if (key.startsWith("reward-")) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock
    }
}

private data class TrainingPathNodeColors(
    val container: Color,
    val content: Color,
    val softContainer: Color,
    val softContent: Color,
)

@Composable
private fun TrainingPathNode.trainingPathColors(): TrainingPathNodeColors {
    return when (state) {
        TrainingPathNodeState.Completed -> TrainingPathNodeColors(
            container = LabPalette.Green,
            content = Color.White,
            softContainer = LabPalette.Green.copy(alpha = 0.14f),
            softContent = LabPalette.GreenDark,
        )

        TrainingPathNodeState.Current -> TrainingPathNodeColors(
            container = LabPalette.Violet,
            content = Color.White,
            softContainer = LabPalette.VioletPanel,
            softContent = LabPalette.VioletDark,
        )

        TrainingPathNodeState.Available -> TrainingPathNodeColors(
            container = LabPalette.Violet,
            content = Color.White,
            softContainer = LabPalette.VioletPanel,
            softContent = LabPalette.VioletDark,
        )

        TrainingPathNodeState.ReviewDue -> TrainingPathNodeColors(
            container = LabPalette.Violet,
            content = Color.White,
            softContainer = LabPalette.VioletPanel,
            softContent = LabPalette.VioletDark,
        )

        TrainingPathNodeState.Locked -> TrainingPathNodeColors(
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            softContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            softContent = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TrainingPathNodeState.Reward -> TrainingPathNodeColors(
            container = LabPalette.Yellow,
            content = LabPalette.Ink,
            softContainer = LabPalette.Yellow.copy(alpha = 0.22f),
            softContent = LabPalette.Ink,
        )
    }
}

private fun LabUiState.trainingPathInput(): TrainingPathInput {
    val selectionWork = normalizeTrainingPathWorkSlug(selection.workSlug)
    val currentProgress = progressItems.filter { item ->
        normalizeTrainingPathWorkSlug(item.workSlug) == selectionWork && item.episode == selection.episode
    }
    val currentReadAirCount = readAir.exercises.count { exercise ->
        normalizeTrainingPathWorkSlug(exercise.workSlug) == selectionWork && exercise.episode == selection.episode
    }
    return TrainingPathInput(
        workTitle = focus.workTitle,
        episodeLabel = focus.episodeLabel,
        lessonTitle = focus.lessonTitle,
        energy = focus.energy,
        streakDays = focus.streakDays,
        sessionXp = sessionXp,
        lessonNodeCount = lesson.nodes.size,
        lessonAnswered = lesson.answered,
        lessonCorrect = lesson.correct,
        vocabCount = vocab.size,
        vocabIds = vocab.map { it.id },
        grammarCount = grammar.size,
        grammarIds = grammar.map { it.id },
        shadowingCount = shadowing.size,
        shadowingIds = shadowing.map { it.id },
        exerciseCount = exercises.size,
        exerciseIds = exercises.map { it.id },
        readAirCount = currentReadAirCount,
        readAirIds = readAir.exercises.filter { exercise ->
            normalizeTrainingPathWorkSlug(exercise.workSlug) == selectionWork && exercise.episode == selection.episode
        }.map { it.id },
        reviewDueCount = reviewTasks.count { item ->
            normalizeTrainingPathWorkSlug(item.workSlug) == selectionWork && item.episode == selection.episode
        },
        localMistakeCount = mistakes.count { mistake ->
            normalizeTrainingPathWorkSlug(mistake.workSlug) == selectionWork && mistake.episode == selection.episode
        },
        progressItems = currentProgress.map { item ->
            TrainingPathProgressItem(
                itemId = item.itemId,
                itemType = item.itemType,
                state = item.state,
                payload = item.payload,
                lastReviewedAt = item.lastReviewedAt,
            )
        },
        episodePlan = episodePlan,
        accessPolicy = TrainingPathAccessPolicy.OpenDuringDevelopment,
        hasNextEpisode = selection.episode < (currentWorkOption().episodeCount.coerceAtLeast(selection.episode)),
    )
}

private fun normalizeTrainingPathWorkSlug(workSlug: String): String {
    return when (workSlug) {
        "rezero" -> "re-zero"
        else -> workSlug
    }
}

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
