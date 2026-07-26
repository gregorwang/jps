package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterArtwork
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterRole
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.PressablePrimaryButton
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import java.util.Calendar

@Composable
fun TodayScreen(
    uiState: LabUiState,
    onStartLesson: () -> Unit,
    onStartReadAir: () -> Unit,
    onStartReview: () -> Unit,
    onOpenSubtitles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plan = rememberTodayPlan(uiState)
    val reducedMotion = rememberReducedMotion()
    val headerLine = remember { todayHeaderLine() }
    val enterState = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = LabSpacing.Screen,
                end = LabSpacing.Screen,
                top = LabSpacing.Large,
                bottom = LabSpacing.XLarge,
            ),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        ) {
            item(key = "today-header", contentType = "header") {
                TodayEntrance(visibleState = enterState, order = 0, reducedMotion = reducedMotion) {
                    TodayHeader(
                        subtitle = headerLine,
                        streakDays = uiState.focus.streakDays,
                        xp = uiState.focus.xp,
                    )
                }
            }
            item(key = "today-hero", contentType = "hero") {
                TodayEntrance(visibleState = enterState, order = 1, reducedMotion = reducedMotion) {
                    TodayHero(
                        plan = plan,
                        workSlug = uiState.selection.workSlug,
                        episode = uiState.selection.episode,
                        motionEnabled = uiState.settings.richAnimationsEnabled && !reducedMotion,
                        onStartLesson = onStartLesson,
                    )
                }
            }
            item(key = "today-section", contentType = "section") {
                TodayEntrance(visibleState = enterState, order = 2, reducedMotion = reducedMotion) {
                    TodaySectionHeader(
                        eyebrow = "现在就做",
                        title = "今天想练哪一块？",
                    )
                }
            }
            item(key = "today-actions", contentType = "actions") {
                TodayEntrance(visibleState = enterState, order = 3, reducedMotion = reducedMotion) {
                    TodayActionGrid(
                        dueReviewCount = plan.dueReviewCount,
                        onStartLesson = onStartLesson,
                        onStartReview = onStartReview,
                        onStartReadAir = onStartReadAir,
                        onOpenSubtitles = onOpenSubtitles,
                    )
                }
            }
            item(key = "today-metrics", contentType = "metrics") {
                TodayEntrance(visibleState = enterState, order = 4, reducedMotion = reducedMotion) {
                    TodayMetricsBar(plan = plan)
                }
            }
            item(key = "today-focus", contentType = "focus") {
                TodayEntrance(visibleState = enterState, order = 5, reducedMotion = reducedMotion) {
                    TodayFocusCard(weaknesses = plan.weaknesses)
                }
            }
        }
    }
}

/** Page header: greeting line, big title and the streak / XP semantic stat chips. */
@Composable
private fun TodayHeader(
    subtitle: String,
    streakDays: Int,
    xp: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "今日",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        TodayStatChip(
            icon = Icons.Rounded.LocalFireDepartment,
            value = "${streakDays.coerceAtLeast(0)} 天",
            containerColor = LabTheme.colors.streakContainer,
            contentColor = LabTheme.colors.streak,
        )
        TodayStatChip(
            icon = Icons.Rounded.Star,
            value = "${xp.coerceAtLeast(0)} XP",
            containerColor = LabTheme.colors.xpContainer,
            contentColor = LabTheme.colors.xp,
        )
    }
}

@Composable
private fun TodayStatChip(
    icon: ImageVector,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

/**
 * Gentle first-frame entrance: fade + small upward slide, staggered per item.
 * Uses a shared transition state so scrolling back never replays the animation,
 * and collapses to an instant reveal when reduced motion is requested.
 */
@Composable
private fun TodayEntrance(
    visibleState: MutableTransitionState<Boolean>,
    order: Int,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val duration = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion)
    val delay = if (reducedMotion) 0 else order * 40
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = delay,
                easing = MotionTokens.Curve.Decelerate,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = delay,
                easing = MotionTokens.Curve.Decelerate,
            ),
            initialOffsetY = { it / 10 },
        ),
        exit = ExitTransition.None,
        label = "today-entrance-$order",
    ) {
        content()
    }
}

@Composable
private fun TodayHero(
    plan: TodayPlan,
    workSlug: String,
    episode: Int,
    motionEnabled: Boolean,
    onStartLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onHero = LabTheme.colors.onHero
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = LabTheme.colors.heroGradientStart,
                spotColor = LabTheme.colors.heroGradientStart,
            )
            .clip(MaterialTheme.shapes.extraLarge)
            .background(LabTheme.heroBrush()),
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(x = 44.dp, y = (-52).dp)
                .background(onHero.copy(alpha = 0.08f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-34).dp, y = 38.dp)
                .background(onHero.copy(alpha = 0.06f), CircleShape),
        )
        Column(
            modifier = Modifier.padding(LabSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
                ) {
                    Surface(
                        color = onHero.copy(alpha = 0.16f),
                        contentColor = onHero,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = plan.nextEpisodeLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "继续上次的训练",
                        style = MaterialTheme.typography.headlineSmall,
                        color = onHero,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (plan.dueReviewCount > 0) {
                            "先清掉 ${plan.dueReviewCount} 项复习，再推进 ${plan.newContentCount} 项新内容，约 ${plan.estimatedMinutes} 分钟"
                        } else {
                            "没有积压复习，直接推进 ${plan.newContentCount} 项新内容，约 ${plan.estimatedMinutes} 分钟"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHero.copy(alpha = 0.82f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CourseCharacterArtwork(
                    workSlug = workSlug,
                    role = CourseCharacterRole.Today,
                    motionEnabled = motionEnabled,
                    modifier = Modifier.size(72.dp),
                    stableSeed = episode,
                )
            }
            PressablePrimaryButton(
                onClick = onStartLesson,
                modifier = Modifier.fillMaxWidth(),
                containerColor = onHero,
                contentColor = LabTheme.colors.heroGradientStart,
            ) {
                Text("继续本集训练", fontWeight = FontWeight.Bold)
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = LabSpacing.XSmall)
                        .size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun TodaySectionHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    SectionTitle(
        eyebrow = eyebrow,
        title = title,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun TodayActionGrid(
    dueReviewCount: Int,
    onStartLesson: () -> Unit,
    onStartReview: () -> Unit,
    onStartReadAir: () -> Unit,
    onOpenSubtitles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        ) {
            TodayActionCard(
                icon = Icons.Rounded.Bolt,
                title = "快速训练",
                body = "词汇、语法混合练",
                highlighted = true,
                onClick = onStartLesson,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            TodayActionCard(
                icon = Icons.Rounded.BarChart,
                title = "智能复盘",
                body = if (dueReviewCount > 0) "${dueReviewCount} 项待处理" else "保持记忆手感",
                onClick = onStartReview,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        ) {
            TodayActionCard(
                icon = Icons.Rounded.Psychology,
                title = "语言学训练",
                body = "语用、句法与潜台词",
                onClick = onStartReadAir,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            TodayActionCard(
                icon = Icons.Rounded.AutoStories,
                title = "台词浏览",
                body = "回看本集上下文",
                onClick = onOpenSubtitles,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun TodayActionCard(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) MotionTokens.Scale.OptionPressed else 1f,
        animationSpec = if (pressed) {
            MotionTokens.microSpec(reducedMotion)
        } else {
            MotionTokens.popSpring(reducedMotion)
        },
        label = "today-action-press",
    )
    val accent = todayEntryAccent(icon)
    val containerTone = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentTone = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 132.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = containerTone,
        contentColor = contentTone,
        shape = MaterialTheme.shapes.large,
        border = if (highlighted) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        shadowElevation = if (highlighted) 4.dp else 1.dp,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(LabSpacing.Medium),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = if (highlighted) contentTone.copy(alpha = 0.14f) else accent.container,
                    contentColor = if (highlighted) contentTone else accent.content,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(22.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentTone.copy(alpha = if (highlighted) 0.75f else 0.4f),
                )
            }
            Column(
                modifier = Modifier.padding(top = LabSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlighted) contentTone.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class TodayEntryAccent(
    val container: Color,
    val content: Color,
)

/** Feature-entry accent mapping keyed on the entry icon declared in [TodayActionGrid]. */
@Composable
private fun todayEntryAccent(icon: ImageVector): TodayEntryAccent {
    val extended = LabTheme.colors
    val scheme = MaterialTheme.colorScheme
    return when (icon) {
        Icons.Rounded.BarChart -> TodayEntryAccent(extended.successContainer, extended.success)
        Icons.Rounded.Psychology -> TodayEntryAccent(extended.infoContainer, extended.info)
        Icons.Rounded.AutoStories -> TodayEntryAccent(extended.warningContainer, extended.warning)
        else -> TodayEntryAccent(scheme.primaryContainer, scheme.primary)
    }
}

@Composable
private fun TodayMetricsBar(
    plan: TodayPlan,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LabSpacing.XSmall, vertical = LabSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TodayMetric("待复盘", plan.dueReviewCount.toString(), Modifier.weight(1f))
            VerticalDivider(
                modifier = Modifier.height(28.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            TodayMetric("本轮内容", plan.newContentCount.toString(), Modifier.weight(1f))
            VerticalDivider(
                modifier = Modifier.height(28.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            TodayMetric("预计", "${plan.estimatedMinutes} 分", Modifier.weight(1f))
        }
    }
}

@Composable
private fun TodayMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val accent = when {
        label == "待复盘" && value == "0" -> LabTheme.colors.success
        label == "待复盘" -> LabTheme.colors.warning
        label == "本轮内容" -> LabTheme.colors.info
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = modifier.padding(horizontal = LabSpacing.XSmall, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = accent,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayFocusCard(
    weaknesses: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(LabSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = LabTheme.colors.infoContainer,
                contentColor = LabTheme.colors.info,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.TrackChanges,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            ) {
                Text(
                    text = "今天只盯这几个点",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "结合错题信号与常见易错点提炼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    weaknesses.forEach { weakness ->
                        TagChip(text = weakness)
                    }
                }
            }
        }
    }
}

private data class TodayPlan(
    val dueReviewCount: Int,
    val newContentCount: Int,
    val estimatedMinutes: Int,
    val nextEpisodeLabel: String,
    val weaknesses: List<String>,
)

@Composable
private fun rememberTodayPlan(uiState: LabUiState): TodayPlan {
    return remember(uiState.reviewTasks, uiState.mistakes, uiState.lesson.nodes, uiState.focus.episodeLabel) {
        val due = (uiState.reviewTasks.map { it.itemId } + uiState.mistakes.map { it.itemId }).distinct().size
        val newContent = uiState.lesson.nodes.size.coerceAtLeast(6)
        val minutes = ((due.coerceAtMost(8) * 2) + (newContent / 4)).coerceIn(5, 25)
        TodayPlan(
            dueReviewCount = due,
            newContentCount = newContent,
            estimatedMinutes = minutes,
            nextEpisodeLabel = uiState.focus.episodeLabel,
            weaknesses = weaknessLabels(uiState).take(3),
        )
    }
}

private fun weaknessLabels(uiState: LabUiState): List<String> {
    val fromMistakes = uiState.mistakes
        .map { it.typeLabel }
        .map { label ->
            when {
                label.contains("词") || label.contains("学习卡") -> "词义混淆"
                label.contains("语法") || label.contains("填空") -> "语法功能"
                label.contains("听") || label.contains("跟读") -> "听力跟读"
                label.contains("语言") || label.contains("读空气") -> "语境判断"
                else -> "语境判断"
            }
        }
        .distinct()
    return (fromMistakes + listOf("语境判断", "词义混淆", "语法功能")).distinct()
}

/** Greeting + date line for the page header, e.g. "下午好 · 7月26日 周日". */
private fun todayHeaderLine(): String {
    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..22 -> "晚上好"
        else -> "夜深了"
    }
    val weekdays = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    val weekday = weekdays[(calendar.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "$greeting · ${month}月${day}日 $weekday"
}
