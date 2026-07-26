package com.animejapaneselab.nativeapp.ui.completion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterArtwork
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterRole
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.CountUpText
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.PressablePrimaryButton
import com.animejapaneselab.nativeapp.ui.motion.XpFlyout
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import kotlinx.coroutines.delay

@Composable
internal fun LegacyLessonCompleteScreen(
    uiState: LabUiState,
    exitLabel: String,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = remember(uiState.lesson, uiState.sessionXp, uiState.hasNextLessonBatch) {
        uiState.toLessonResultUiState()
    }
    val feedback = LocalFeedbackEngine.current
    val reducedMotion = rememberReducedMotion()
    var showReward by rememberSaveable { mutableStateOf(false) }
    var showReview by rememberSaveable { mutableStateOf(false) }
    var completionFeedbackEmitted by rememberSaveable(result.completedCount, result.xp) { mutableStateOf(false) }
    LaunchedEffect(result.completedCount, result.xp) {
        if (!completionFeedbackEmitted) {
            completionFeedbackEmitted = true
            feedback?.emit(FeedbackEvent.LessonComplete)
        }
        delay(MotionTokens.duration(380, reducedMotion).toLong())
        showReward = true
        delay(MotionTokens.duration(480, reducedMotion).toLong())
        showReview = true
    }
    val cardEnter = remember(reducedMotion) {
        fadeIn(
            tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                easing = MotionTokens.Curve.Standard,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                easing = MotionTokens.Curve.Decelerate,
            ),
            initialOffsetY = { it / 8 },
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = LabSpacing.Screen, vertical = LabSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = exitLabel)
                }
                Text(
                    text = "训练结算",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(LabTheme.heroBrush())
                        .padding(horizontal = LabSpacing.Large, vertical = LabSpacing.XLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
                ) {
                    Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                        CourseCharacterArtwork(
                            workSlug = uiState.selection.workSlug,
                            role = CourseCharacterRole.Celebration,
                            motionEnabled = uiState.settings.richAnimationsEnabled && !reducedMotion,
                            stableSeed = uiState.selection.episode,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        text = "单元完成！",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = LabTheme.colors.onHero,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LabTheme.colors.onHero.copy(alpha = 0.86f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                XpFlyout(
                    xp = result.xp,
                    visible = showReward,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = LabSpacing.Medium, end = LabSpacing.Medium),
                )
            }
        }
        item {
            AnimatedVisibility(visible = showReward, enter = cardEnter) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
                ) {
                    SettlementStat(
                        label = "获得经验",
                        value = "+${result.xp}",
                        icon = Icons.Rounded.Stars,
                        accent = LabTheme.colors.xp,
                        container = LabTheme.colors.xpContainer,
                        valueColor = LabTheme.colors.xp,
                        modifier = Modifier.weight(1f),
                    )
                    SettlementStat(
                        label = "正确率",
                        value = "${result.accuracyPercent}%",
                        icon = Icons.Rounded.CheckCircle,
                        accent = LabTheme.colors.success,
                        container = LabTheme.colors.successContainer,
                        valueColor = LabTheme.colors.onSuccessContainer,
                        modifier = Modifier.weight(1f),
                    )
                    SettlementStat(
                        label = "连续天数",
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
        item {
            AnimatedVisibility(visible = showReward, enter = cardEnter) {
                LabCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "奖励结算",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LabTheme.colors.xpContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, LabTheme.colors.xp.copy(alpha = 0.45f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LabSpacing.Medium, vertical = LabSpacing.Small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Stars,
                                contentDescription = null,
                                tint = LabTheme.colors.xp,
                            )
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
                    SettlementLine("完成题数", "${result.completedCount} / ${result.totalCount}")
                    SettlementLine("正确题数", "${result.correctCount} 题 · 正确率 ${result.accuracyPercent}%")
                    SettlementLine("新增掌握内容", result.masteredContent)
                }
            }
        }
        item {
            AnimatedVisibility(visible = showReview, enter = cardEnter) {
                MistakeSummaryCard(result = result)
            }
        }
        item {
            AnimatedVisibility(visible = showReview, enter = cardEnter) {
                ReviewScheduleCard(result = result)
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            ) {
                PressablePrimaryButton(
                    onClick = if (result.hasNextBatch) onNextBatch else onExit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (result.hasNextBatch) Icons.Rounded.PlayArrow else Icons.Rounded.Stars,
                        contentDescription = null,
                    )
                    Text(
                        text = if (result.hasNextBatch) "领取经验并继续" else "领取经验",
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Black,
                    )
                }
                SecondaryButton(
                    text = exitLabel,
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SettlementStat(
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
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LabSpacing.XSmall, vertical = LabSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
