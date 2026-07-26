package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.domain.ReviewDueBucket
import com.animejapaneselab.nativeapp.domain.SmartReviewEntry
import com.animejapaneselab.nativeapp.domain.SmartReviewPlan
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.PressablePrimaryButton
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabTheme

@Composable
fun SmartReviewQueueScreen(
    plan: SmartReviewPlan,
    onBack: () -> Unit,
    onStartItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(plan.entries) { buildQueueGroups(plan.entries) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "smart-review-header", contentType = "header") {
            SmartReviewQueueHeader(onBack = onBack)
        }
        item(key = "smart-review-summary", contentType = "summary") {
            SmartReviewQueueSummary(plan = plan, onStart = plan.entries.firstOrNull()?.let { entry ->
                { onStartItem(entry.key) }
            })
        }
        item(key = "smart-review-explanation", contentType = "explanation") {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SectionTitle(eyebrow = "智能排序", title = "按优先级安排")
                Text(
                    text = "综合重复错误、掌握状态、逾期天数与 SRS 熟练度排序；重复任务已自动合并。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (groups.isNotEmpty()) {
            groups.forEach { group ->
                item(key = "bucket-${group.bucket.name}", contentType = "bucket-header") {
                    SmartReviewBucketHeader(
                        bucket = group.bucket,
                        count = group.rows.size,
                        modifier = animatedQueueItem(),
                    )
                }
                items(
                    items = group.rows,
                    key = { row -> "${group.bucket.name}|${row.entry.key}" },
                    contentType = { "smart-review-row" },
                ) { row ->
                    SmartReviewQueueRow(
                        position = row.position,
                        entry = row.entry,
                        onClick = { onStartItem(row.entry.key) },
                        modifier = animatedQueueItem(),
                    )
                }
            }
        } else {
            item(key = "smart-review-empty", contentType = "empty-state") {
                SmartReviewQueueEmptyCard(modifier = animatedQueueItem())
            }
        }
    }
}

/** 列表项进出与重排动画,尊重系统「减少动态效果」设置。 */
@Composable
private fun LazyItemScope.animatedQueueItem(): Modifier {
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
private fun SmartReviewQueueHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回复盘",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("智能复盘队列", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("先看安排，再开始训练", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmartReviewQueueSummary(
    plan: SmartReviewPlan,
    onStart: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val onHero = LabTheme.colors.onHero
    val dueBreakdown = remember(plan.overdueCount, plan.dueTodayCount) {
        buildList {
            if (plan.overdueCount > 0) add("逾期 ${plan.overdueCount}")
            if (plan.dueTodayCount > 0) add("今天 ${plan.dueTodayCount}")
        }.joinToString(" · ")
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(LabTheme.heroBrush()),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = onHero.copy(alpha = 0.16f),
                    contentColor = onHero,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.padding(8.dp))
                }
                Text(
                    text = "为你智能安排的一轮复盘",
                    color = onHero.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = plan.entries.size.toString(),
                    color = onHero,
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = "项待复习",
                    color = onHero.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
                if (dueBreakdown.isNotEmpty()) {
                    Text(
                        text = dueBreakdown,
                        color = onHero.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(bottom = 7.dp),
                    )
                }
            }
            Text(
                text = "重点：${plan.focusLabel} · 约 ${plan.estimatedMinutes} 分钟",
                color = onHero.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (plan.mergedDuplicateCount > 0) {
                Surface(
                    color = onHero.copy(alpha = 0.16f),
                    contentColor = onHero,
                    shape = CircleShape,
                ) {
                    Text(
                        text = "已自动合并 ${plan.mergedDuplicateCount} 条重复任务",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (onStart != null) {
                PressablePrimaryButton(
                    text = "从第 1 项开始",
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = onHero,
                    contentColor = LabTheme.colors.heroGradientStart,
                )
            }
        }
    }
}

/** 到期分桶组头:逾期用 error 强调,今天到期用 primary,其余保持中性。 */
@Composable
private fun SmartReviewBucketHeader(
    bucket: ReviewDueBucket,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (bucket) {
        ReviewDueBucket.Overdue -> MaterialTheme.colorScheme.error
        ReviewDueBucket.DueToday -> MaterialTheme.colorScheme.primary
        ReviewDueBucket.Upcoming, ReviewDueBucket.NoSchedule -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    SectionTitle(
        eyebrow = "${bucket.label} · $count 项",
        title = queueBucketTitle(bucket),
        modifier = modifier.padding(top = 8.dp),
        accentColor = accentColor,
    )
}

@Composable
private fun SmartReviewQueueRow(
    position: Int,
    entry: SmartReviewEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val labColors = LabTheme.colors
    val (priorityContainer, priorityContent, priorityLabel) = when {
        entry.priority >= QueueHighPriority -> Triple(colorScheme.errorContainer, colorScheme.onErrorContainer, "高优先")
        entry.priority >= QueueMediumPriority -> Triple(labColors.infoContainer, labColors.onInfoContainer, "中优先")
        else -> Triple(colorScheme.surfaceContainerHigh, colorScheme.onSurfaceVariant, "低优先")
    }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                color = priorityContainer,
                contentColor = priorityContent,
                shape = MaterialTheme.shapes.small,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${entry.category} · ${entry.sourceLabel} · ${queueSourceLabel(entry.workSlug, entry.episode)}",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = priorityContainer,
                        contentColor = priorityContent,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = priorityLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = entry.reason,
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                val showEaseChip = entry.ease in QueueHardEaseRange
                if (showEaseChip || entry.reviewCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showEaseChip) {
                            Surface(
                                color = labColors.warningContainer,
                                contentColor = labColors.onWarningContainer,
                                shape = CircleShape,
                            ) {
                                Text(
                                    text = "吃力",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        if (entry.reviewCount > 0) {
                            Text(
                                text = "已复习 ${entry.reviewCount} 次",
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "开始这一项", tint = colorScheme.primary)
        }
    }
}

@Composable
private fun SmartReviewQueueEmptyCard(modifier: Modifier = Modifier) {
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "队列已清空，干得漂亮！",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "继续训练后，新的复习任务会自动排进来。",
                color = LabTheme.colors.onSuccessContainer.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// 与 SmartReviewPlanner 的评分对齐:本地错题基础 70 分,错题+到期任务合并另加 25 分。
private const val QueueHighPriority = 110
private const val QueueMediumPriority = 70

// SM-2 风格 ease:1..2 视为「吃力」,0 表示服务端未下发。
private val QueueHardEaseRange = 1..2

// 组渲染顺序:先补逾期,再看今天,然后是没有排程的错题,最后才是提前量。
private val QueueBucketOrder = listOf(
    ReviewDueBucket.Overdue,
    ReviewDueBucket.DueToday,
    ReviewDueBucket.NoSchedule,
    ReviewDueBucket.Upcoming,
)

/** 队列行:position 取自 plan.entries 的全局优先级序号,分组后依然连续可读。 */
private data class QueueRow(val position: Int, val entry: SmartReviewEntry)

private data class QueueGroup(val bucket: ReviewDueBucket, val rows: List<QueueRow>)

/** 保持 planner 的 priority 主排序,只按 dueBucket 切分成组;空组不产出。 */
private fun buildQueueGroups(entries: List<SmartReviewEntry>): List<QueueGroup> {
    if (entries.isEmpty()) return emptyList()
    val rows = entries.mapIndexed { index, entry -> QueueRow(position = index + 1, entry = entry) }
    return QueueBucketOrder.mapNotNull { bucket ->
        val bucketRows = rows.filter { it.entry.dueBucket == bucket }
        if (bucketRows.isEmpty()) null else QueueGroup(bucket = bucket, rows = bucketRows)
    }
}

private fun queueBucketTitle(bucket: ReviewDueBucket): String = when (bucket) {
    ReviewDueBucket.Overdue -> "先把逾期的补回来"
    ReviewDueBucket.DueToday -> "今天该练的"
    ReviewDueBucket.NoSchedule -> "错题随时补练"
    ReviewDueBucket.Upcoming -> "提前练一练"
}

private fun queueSourceLabel(workSlug: String, episode: Int): String {
    val work = when (workSlug.trim().lowercase()) {
        "k-on" -> "K-ON!"
        "re-zero", "rezero" -> "Re:Zero"
        else -> workSlug.ifBlank { "当前作品" }
    }
    return if (episode > 0) "$work · EP${episode.toString().padStart(2, '0')}" else work
}
