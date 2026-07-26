package com.animejapaneselab.nativeapp.ui.completion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterArtwork
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterRole
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonButton
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonProgressBar
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonTone
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.fusion.AnimeLabFusionAssetResolver
import com.animejapaneselab.nativeapp.ui.fusion.AnimeLabFusionRollout
import com.animejapaneselab.nativeapp.ui.fusion.FusionMotionHost
import com.animejapaneselab.nativeapp.ui.fusion.FusionVisualKey
import com.animejapaneselab.nativeapp.ui.motion.CountUpText
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing
import com.animejapaneselab.nativeapp.ui.theme.LabTheme

@Composable
fun LessonCompleteScreen(
    uiState: LabUiState,
    exitLabel: String,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rollout = AnimeLabFusionRollout.current
    if (!rollout.useFusionSessionComplete) {
        LegacyLessonCompleteScreen(
            uiState = uiState,
            exitLabel = exitLabel,
            onExit = onExit,
            onRestart = onRestart,
            onNextBatch = onNextBatch,
            modifier = modifier,
        )
        return
    }

    val result = remember(uiState.lesson, uiState.sessionXp, uiState.hasNextLessonBatch, uiState.focus) {
        uiState.toLessonResultUiState()
    }
    val feedback = LocalFeedbackEngine.current
    val reducedMotion = rememberReducedMotion()
    val resolution = remember {
        AnimeLabFusionAssetResolver.resolve(FusionVisualKey.SessionCompleteCelebration)
    }
    var completionFeedbackEmitted by rememberSaveable(result.completedCount, result.correctCount, result.xp) {
        mutableStateOf(false)
    }
    LaunchedEffect(result.completedCount, result.correctCount, result.xp) {
        if (!completionFeedbackEmitted) {
            completionFeedbackEmitted = true
            feedback?.emit(FeedbackEvent.LessonComplete)
        }
    }

    LessonCompleteContent(
        result = result,
        exitLabel = exitLabel,
        onExit = onExit,
        onRestart = onRestart,
        onNextBatch = onNextBatch,
        modifier = modifier,
        heroContent = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                FusionMotionHost(
                    resolution = resolution,
                    motionEnabled = uiState.settings.richAnimationsEnabled && !reducedMotion,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {},
                )
                CourseCharacterArtwork(
                    workSlug = uiState.selection.workSlug,
                    role = CourseCharacterRole.Celebration,
                    motionEnabled = uiState.settings.richAnimationsEnabled && !reducedMotion,
                    stableSeed = uiState.selection.episode,
                    modifier = Modifier.size(220.dp),
                )
            }
        },
    )
}

@Composable
internal fun LessonCompleteContent(
    result: LessonResultUiState,
    exitLabel: String,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    modifier: Modifier = Modifier,
    heroContent: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = LabSpacing.Screen,
            top = LabSpacing.Small,
            end = LabSpacing.Screen,
            bottom = LabSpacing.XXLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            CompletionTopBar(
                episodeLabel = result.episodeLabel,
                exitLabel = exitLabel,
                onExit = onExit,
            )
        }
        item {
            CompletionHero(
                result = result,
                heroContent = heroContent,
            )
        }
        item {
            CompletionMetricsBar(result = result)
        }
        item {
            LearningGainCard(result = result)
        }
        if (result.correctCount < result.completedCount) {
            item { MistakeSummaryCard(result = result) }
        }
        item { ReviewScheduleCard(result = result) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            ) {
                JuicyLessonButton(
                    text = if (result.hasNextBatch) "继续下一组" else "领取经验，返回",
                    onClick = if (result.hasNextBatch) onNextBatch else onExit,
                    modifier = Modifier.fillMaxWidth(),
                    tone = JuicyLessonTone.Purple,
                    trailingIcon = if (result.hasNextBatch) Icons.Rounded.Stars else Icons.Rounded.Check,
                )
                SecondaryButton(
                    text = "再练一轮",
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CompletionTopBar(
    episodeLabel: String,
    exitLabel: String,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = exitLabel)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ANIME JAPANESE LAB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = episodeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompletionHero(
    result: LessonResultUiState,
    heroContent: (@Composable () -> Unit)?,
) {
    val reducedMotion = rememberReducedMotion()
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val heroScale by animateFloatAsState(
        targetValue = if (appeared || reducedMotion) 1f else 0.92f,
        animationSpec = MotionTokens.popSpring(reducedMotion),
        label = "completion-hero-scale",
    )
    val onHero = LabTheme.colors.onHero
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = heroScale
                scaleY = heroScale
            }
            .clip(MaterialTheme.shapes.extraLarge)
            .background(LabTheme.heroBrush())
            .padding(horizontal = LabSpacing.Large, vertical = LabSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            color = onHero.copy(alpha = 0.16f),
            contentColor = onHero,
            shape = CircleShape,
            border = BorderStroke(1.dp, onHero.copy(alpha = 0.38f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = result.completionBadge(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center,
        ) {
            heroContent?.invoke() ?: AnimeLabCompletionFallback()
        }
        Text(
            text = result.completionHeadline(),
            style = MaterialTheme.typography.headlineMedium,
            color = onHero,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = result.title,
            style = MaterialTheme.typography.titleMedium,
            color = onHero.copy(alpha = 0.86f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompletionMetricsBar(
    result: LessonResultUiState,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedbackEngine.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LabTheme.colors.xpContainer,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.5.dp, LabTheme.colors.xp.copy(alpha = 0.45f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = LabSpacing.Medium, vertical = LabSpacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.Stars,
                        contentDescription = null,
                        tint = LabTheme.colors.xp,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "获得经验",
                        style = MaterialTheme.typography.labelLarge,
                        color = LabTheme.colors.xp,
                        fontWeight = FontWeight.Black,
                    )
                }
                CountUpText(
                    target = result.xp,
                    prefix = "+",
                    suffix = " XP",
                    style = MaterialTheme.typography.displaySmall,
                    color = LabTheme.colors.xp,
                    onTick = { feedback?.emit(FeedbackEvent.XpGain(it)) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        ) {
            CompletionMetric(
                label = "正确率",
                value = "${result.accuracyPercent}%",
                icon = Icons.Rounded.CheckCircle,
                accent = LabTheme.colors.success,
                container = LabTheme.colors.successContainer,
                valueColor = LabTheme.colors.onSuccessContainer,
                modifier = Modifier.weight(1f),
            )
            CompletionMetric(
                label = "连续学习",
                value = "${result.streakDays} 天",
                icon = Icons.Rounded.LocalFireDepartment,
                accent = LabTheme.colors.streak,
                container = LabTheme.colors.streakContainer,
                valueColor = LabTheme.colors.streak,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompletionMetric(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    container: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = container,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LearningGainCard(result: LessonResultUiState) {
    LabCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "这一轮真正学到了什么",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "${result.correctCount}/${result.completedCount}",
                style = MaterialTheme.typography.titleMedium,
                color = LabTheme.colors.success,
                fontWeight = FontWeight.Black,
            )
        }
        JuicyLessonProgressBar(
            progress = result.accuracyPercent / 100f,
            modifier = Modifier.fillMaxWidth(),
            milestoneVisible = result.accuracyPercent == 100,
            pulsing = result.accuracyPercent == 100,
            progressColor = LabTheme.colors.success,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Text(
            text = result.masteredContent,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AnimeLabCompletionFallback() {
    Box(
        modifier = Modifier
            .size(172.dp)
            .clip(CircleShape)
            .background(LabTheme.colors.onHero.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(104.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "JL",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private fun LessonResultUiState.completionBadge(): String {
    return when {
        accuracyPercent == 100 -> "全对通关"
        accuracyPercent >= 80 -> "稳稳完成"
        else -> "坚持完成"
    }
}

private fun LessonResultUiState.completionHeadline(): String {
    return when {
        accuracyPercent == 100 -> "这一集，完全听懂！"
        accuracyPercent >= 80 -> "动漫日语又前进了一段！"
        else -> "完成比完美更重要"
    }
}
