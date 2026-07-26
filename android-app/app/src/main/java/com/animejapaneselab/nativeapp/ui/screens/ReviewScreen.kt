package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.domain.SmartReviewPlan
import com.animejapaneselab.nativeapp.domain.buildSmartReviewPlan
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.StructuredAiResultCard
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.PressablePrimaryButton
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun ReviewScreen(
    uiState: LabUiState,
    onOpenLesson: () -> Unit,
    onOpenSmartReviewQueue: () -> Unit,
    onMistakeReviewed: (String) -> Unit,
    onPracticeMistake: (String) -> Unit,
    onPracticeRemoteTask: (ProgressItem) -> Unit,
    onExplainMistake: (String) -> Unit,
    // 跳转字幕页并定位原句,lineNo = 0 表示只打开本集字幕、不定位具体行。
    onViewSource: (workSlug: String, episode: Int, lineNo: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val reviewTasks = remember(uiState.reviewTasks) {
        uiState.reviewTasks.distinctBy { it.reviewIdentity() }
    }
    val mistakes = remember(uiState.mistakes) {
        uiState.mistakes.distinctBy { it.reviewIdentity() }
    }
    val reviewPlan = remember(reviewTasks, mistakes) {
        buildSmartReviewPlan(reviewTasks = reviewTasks, mistakes = mistakes)
    }
    val visibleReviewTasks = reviewTasks.take(ReviewQueuePreviewLimit)
    val visibleMistakes = mistakes.take(ReviewQueuePreviewLimit)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "review-hero", contentType = "review-hero") {
            ReviewHero(
                plan = reviewPlan,
                onPractice = if (reviewPlan.entries.isNotEmpty()) onOpenSmartReviewQueue else onOpenLesson,
            )
        }
        if (reviewTasks.isNotEmpty()) {
            item(key = "review-tasks-header", contentType = "queue-header") {
                ReviewQueueHeader(
                    icon = Icons.Rounded.Schedule,
                    title = "到期任务",
                    total = reviewTasks.size,
                    visible = visibleReviewTasks.size,
                    modifier = animatedReviewItem(),
                )
            }
            items(
                items = visibleReviewTasks,
                key = { it.reviewIdentity() },
                contentType = { "remote-review-row" },
            ) { task ->
                RemoteReviewRow(
                    task = task,
                    onPractice = { onPracticeRemoteTask(task) },
                    onViewSource = { lineNo -> onViewSource(task.workSlug, task.episode, lineNo) },
                    modifier = animatedReviewItem(),
                )
            }
        }
        if (mistakes.isNotEmpty()) {
            item(key = "mistakes-header", contentType = "queue-header") {
                ReviewQueueHeader(
                    icon = Icons.Rounded.Error,
                    title = "错题队列",
                    total = mistakes.size,
                    visible = visibleMistakes.size,
                    modifier = animatedReviewItem(),
                )
            }
            items(
                items = visibleMistakes,
                key = { it.reviewIdentity() },
                contentType = { "mistake-row" },
            ) { mistake ->
                MistakeQueueRow(
                    mistake = mistake,
                    uiState = uiState,
                    onPractice = { onPracticeMistake(mistake.itemId) },
                    onExplain = { onExplainMistake(mistake.itemId) },
                    onReviewed = { onMistakeReviewed(mistake.itemId) },
                    onViewSource = { lineNo -> onViewSource(mistake.workSlug, mistake.episode, lineNo) },
                    modifier = animatedReviewItem(),
                )
            }
        } else if (reviewTasks.isEmpty()) {
            item(key = "review-empty", contentType = "empty-state") {
                ReviewEmptyState(modifier = animatedReviewItem())
            }
        }
    }
}

/** 列表项进出与重排动画,尊重系统「减少动态效果」设置。 */
@Composable
private fun LazyItemScope.animatedReviewItem(): Modifier {
    val reducedMotion = rememberReducedMotion()
    return Modifier.animateItem(
        fadeInSpec = MotionTokens.microSpec(reducedMotion),
        placementSpec = if (reducedMotion) {
            tween(durationMillis = 1)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold,
            )
        },
        fadeOutSpec = MotionTokens.microSpec(reducedMotion),
    )
}

@Composable
private fun ReviewHero(
    plan: SmartReviewPlan,
    onPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasQueue = plan.entries.isNotEmpty()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.padding(10.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("复习", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (hasQueue) "今天有 ${plan.entries.size} 项需要巩固" else "今天没有待处理内容，太棒了！",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReviewStatCard(
                    label = "到期任务",
                    value = plan.remoteDueCount.toString(),
                    containerColor = if (plan.remoteDueCount > 0) LabTheme.colors.infoContainer else LabTheme.colors.successContainer,
                    contentColor = if (plan.remoteDueCount > 0) LabTheme.colors.onInfoContainer else LabTheme.colors.onSuccessContainer,
                    modifier = Modifier.weight(1f),
                )
                ReviewStatCard(
                    label = "错题",
                    value = plan.localMistakeCount.toString(),
                    containerColor = if (plan.localMistakeCount > 0) LabTheme.colors.warningContainer else LabTheme.colors.successContainer,
                    contentColor = if (plan.localMistakeCount > 0) LabTheme.colors.onWarningContainer else LabTheme.colors.onSuccessContainer,
                    modifier = Modifier.weight(1f),
                )
                ReviewMetric(
                    label = "预计用时",
                    value = "${plan.estimatedMinutes} 分",
                    modifier = Modifier.weight(1f),
                )
            }
            if (hasQueue) {
                ReviewWeaknessSummary(
                    weakness = plan.focusLabel,
                    suggestion = "先从最容易再错的方向开始，巩固效果最好。",
                )
            }
            PressablePrimaryButton(
                text = if (hasQueue) "开始复习" else "去今日训练",
                onClick = onPractice,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 语义色统计卡:警示/信息状态各自着色,清零时用 success 色系庆祝。 */
@Composable
private fun ReviewStatCard(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                color = contentColor.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ReviewMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ReviewWeaknessSummary(
    weakness: String,
    suggestion: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "本轮重点 · $weakness",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = suggestion,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ReviewQueueHeader(
    icon: ImageVector,
    title: String,
    total: Int,
    visible: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .semantics { heading() },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(7.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(
                text = if (total > visible) "共 $total 条 · 先看前 $visible 条" else "共 $total 条",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun RemoteReviewRow(
    task: ProgressItem,
    onPractice: () -> Unit,
    onViewSource: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sourceLineNo = remember(task.payload) { task.sourceLineNoOrZero() }
    val canViewSource = task.workSlug.isNotBlank() && task.episode > 0
    Surface(
        onClick = onPractice,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = LabTheme.colors.infoContainer,
                contentColor = LabTheme.colors.onInfoContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.padding(9.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = reviewTaskLabel(task.label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${task.itemType.reviewTypeLabel()} · ${task.state.label} · ${reviewSourceLabel(task.workSlug, task.episode)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ReviewDuePill(nextReviewOn = task.nextReviewOn)
            }
            if (canViewSource) {
                IconButton(
                    onClick = { onViewSource(sourceLineNo) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Rounded.Subtitles,
                        contentDescription = viewSourceLabel(sourceLineNo),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** 到期状态胶囊:逾期 = warning 系,今天到期 = 主色系,未来日期 = 中性层。 */
@Composable
private fun ReviewDuePill(nextReviewOn: String, modifier: Modifier = Modifier) {
    val text = remember(nextReviewOn) { reviewDueLabel(nextReviewOn) }
    val overdue = text.startsWith("已逾期")
    val dueToday = text == "今天到期"
    val containerColor = when {
        overdue -> LabTheme.colors.warningContainer
        dueToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        overdue -> LabTheme.colors.onWarningContainer
        dueToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MistakeQueueRow(
    mistake: MistakeRecord,
    uiState: LabUiState,
    onPractice: () -> Unit,
    onExplain: () -> Unit,
    onReviewed: () -> Unit,
    onViewSource: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val explainingThis = uiState.reviewAiTargetId == mistake.itemId
    val cause = remember(mistake) { mistakeCause(mistake) }
    val sourceLineNo = remember(mistake.sourceLabel) { mistake.sourceLineNoOrZero() }
    val canViewSource = mistake.workSlug.isNotBlank() && mistake.episode > 0
    val reducedMotion = rememberReducedMotion()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Rounded.Error, contentDescription = null, modifier = Modifier.padding(7.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = mistake.prompt,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${mistake.typeLabel.reviewTypeLabel()} · ${mistake.lastState.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MistakeAnswerLine(
                    icon = Icons.Rounded.Close,
                    label = "你答",
                    value = mistake.selected.ifBlank { "未作答" },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
                MistakeAnswerLine(
                    icon = Icons.Rounded.Check,
                    label = "正解",
                    value = mistake.expected,
                    containerColor = LabTheme.colors.successContainer,
                    contentColor = LabTheme.colors.onSuccessContainer,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TagChip(text = cause)
                TagChip(text = reviewSourceLabel(mistake.workSlug, mistake.episode, mistake.sourceLabel))
                if (mistake.attempts > 1) {
                    Surface(
                        color = LabTheme.colors.warningContainer,
                        contentColor = LabTheme.colors.onWarningContainer,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = "答错 ${mistake.attempts} 次",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = onPractice,
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("再练", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = onExplain,
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("AI 解析", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
                if (canViewSource) {
                    TextButton(
                        onClick = { onViewSource(sourceLineNo) },
                        modifier = Modifier.heightIn(min = 44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Rounded.Subtitles, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = viewSourceLabel(sourceLineNo),
                            modifier = Modifier.padding(start = 6.dp),
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                TextButton(
                    onClick = onReviewed,
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.textButtonColors(contentColor = LabTheme.colors.success),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("已掌握", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
            }
            AnimatedVisibility(
                visible = explainingThis && (
                    uiState.aiCoach.answer.isNotBlank() ||
                        uiState.aiCoach.status == SyncStatus.Loading ||
                        uiState.aiCoach.status == SyncStatus.Error
                    ),
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                        easing = MotionTokens.Curve.Decelerate,
                    ),
                ) + expandVertically(
                    animationSpec = tween(
                        durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                        easing = MotionTokens.Curve.Decelerate,
                    ),
                    expandFrom = Alignment.Top,
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                    ),
                ) + shrinkVertically(
                    animationSpec = tween(
                        durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                    ),
                    shrinkTowards = Alignment.Top,
                ),
            ) {
                StructuredAiResultCard(
                    result = uiState.aiCoach.result,
                    fallbackText = if (uiState.aiCoach.status == SyncStatus.Loading) "分析中…" else uiState.aiCoach.answer,
                )
            }
        }
    }
}

/** 「你答 / 正解」对比行:error 系对 success 系,一眼看出差异。 */
@Composable
private fun MistakeAnswerLine(
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
            )
            Text(
                text = label,
                color = contentColor.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReviewEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = LabTheme.colors.successContainer,
        contentColor = LabTheme.colors.onSuccessContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, LabTheme.colors.success.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                color = LabTheme.colors.success,
                contentColor = LabTheme.colors.onSuccess,
                shape = CircleShape,
            ) {
                Icon(Icons.Rounded.Celebration, contentDescription = null, modifier = Modifier.padding(14.dp))
            }
            Text(
                text = "太棒了，全部清空！",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "现在没有错题，也没有到期任务。继续训练，保持这个势头！",
                color = LabTheme.colors.onSuccessContainer.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class ReviewCauseBucket(
    val title: String,
    val causes: List<String>,
    val matches: (MistakeRecord) -> Boolean,
)

private val reviewCauseBuckets = listOf(
    ReviewCauseBucket("词汇类", listOf("不认识这个词", "词义混淆", "读音不熟", "汉字看错", "口语义没理解")) {
        it.typeLabel.contains("词") || it.typeLabel.contains("学习卡")
    },
    ReviewCauseBucket("语法类", listOf("接续看错", "助词判断错", "语法功能误判", "修饰范围错误", "主语判断错误")) {
        it.typeLabel.contains("语法") || it.typeLabel.contains("填空")
    },
    ReviewCauseBucket("语感类", listOf("没读懂潜台词", "没判断出说话人态度", "不理解角色关系", "没看出反问", "礼貌程度判断错")) {
        it.typeLabel.contains("语言") || it.typeLabel.contains("读空气") || it.sourceKindLikeExercise()
    },
    ReviewCauseBucket("听力/跟读类", listOf("听不出关键词", "语速跟不上", "音变没识别", "停顿位置不对", "跟读节奏不稳")) {
        it.typeLabel.contains("听") || it.typeLabel.contains("跟读")
    },
)

private const val ReviewQueuePreviewLimit = 3

private fun ProgressItem.reviewIdentity(): String {
    return itemId.ifBlank {
        listOf(workSlug, episode.toString(), itemType, label).joinToString("|")
    }
}

private fun MistakeRecord.reviewIdentity(): String {
    return itemId.ifBlank {
        listOf(workSlug, episode.toString(), typeLabel, prompt, expected).joinToString("|")
    }
}

private fun MistakeRecord.sourceKindLikeExercise(): Boolean {
    return typeLabel.contains("选择") && explanation.contains("语境")
}

/** 只编译一次的行号正则,匹配 sourceLabel 里的「第 16 行」。 */
private val ReviewSourceLineRegex = Regex("""第\s*(\d+)\s*行""")

/** 从错题的 sourceLabel 里取行号;取不到返回 0(= 只打开本集字幕,不定位)。 */
private fun MistakeRecord.sourceLineNoOrZero(): Int {
    return ReviewSourceLineRegex.find(sourceLabel)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?: 0
}

/** 从远程任务 payload 里取行号,兼容驼峰与下划线两种键名;取不到返回 0。 */
private fun ProgressItem.sourceLineNoOrZero(): Int {
    val raw = payload["sourceLineNo"] ?: payload["source_line_no"] ?: return 0
    return raw.trim().toIntOrNull()?.takeIf { it > 0 } ?: 0
}

/** 有行号时能精确定位,没行号只能打开本集台词——文案要让用户预期准确。 */
private fun viewSourceLabel(lineNo: Int): String {
    return if (lineNo > 0) "查看原句" else "查看本集台词"
}

private fun mistakeCause(mistake: MistakeRecord): String {
    val bucket = reviewCauseBuckets.firstOrNull { it.matches(mistake) } ?: reviewCauseBuckets.first()
    return bucket.causes[(mistake.itemId.hashCode().absoluteValue) % bucket.causes.size]
}

private val Int.absoluteValue: Int
    get() = if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this)

private fun String.reviewTypeLabel(): String {
    return when (this) {
        "vocab", "学习卡", "选择" -> "词汇"
        "grammar", "填空" -> "语法"
        "sentence", "听音", "跟读" -> "听力/跟读"
        "exercise", "语言学题", "读空气" -> "语感"
        else -> this.ifBlank { "训练" }
    }
}

private fun reviewSourceLabel(workSlug: String, episode: Int, sourceLabel: String = ""): String {
    val work = when (workSlug) {
        "k-on" -> "K-ON!"
        "re-zero", "rezero" -> "Re:Zero"
        else -> workSlug.ifBlank { "当前作品" }
    }
    val episodeLabel = episode.takeIf { it > 0 }?.let { "EP${it.toString().padStart(2, '0')}" }
    val localizedSource = sourceLabel
        .replace(Regex("""\bline\s+(\d+)""", RegexOption.IGNORE_CASE), "第 $1 句")
        .trim()
    return listOfNotNull(work, episodeLabel, localizedSource.takeIf { it.isNotBlank() }).joinToString(" · ")
}

private fun reviewTaskLabel(label: String): String {
    return label
        .replace("single_choice", "单选题")
        .replace("morphology_analysis", "词形分析")
        .replace("historical", "上下文理解")
        .replace(Regex("""[【\[]\s*cue\s+(\d+)[^】\]]*[】\]]""", RegexOption.IGNORE_CASE), "第 $1 句")
        .replace(Regex("""[【\[]\s*第\s+(\d+)\s+句\s*/[^】\]]*[】\]]"""), "第 $1 句")
        .replace(Regex("""\[cue\s+(\d+)[^\]]*]""", RegexOption.IGNORE_CASE), "第 $1 句")
        .replace(Regex("""\bcue\s+(\d+)""", RegexOption.IGNORE_CASE), "第 $1 句")
        .replace(Regex("""\bline\s+(\d+)""", RegexOption.IGNORE_CASE), "第 $1 句")
        .trim()
}

private fun reviewDueLabel(value: String, today: LocalDate = LocalDate.now()): String {
    val due = try {
        value.takeIf { it.length >= 10 }?.let { LocalDate.parse(it.take(10)) }
    } catch (_: DateTimeParseException) {
        null
    }
    if (due == null) return "今天到期"
    val overdueDays = (today.toEpochDay() - due.toEpochDay()).coerceAtLeast(0)
    return when {
        overdueDays > 0 -> "已逾期 $overdueDays 天"
        due == today -> "今天到期"
        else -> "${due.monthValue} 月 ${due.dayOfMonth} 日到期"
    }
}
