package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.EpisodeOption
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.MetricPill
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.theme.LabPalette

@Composable
fun TodayScreen(
    uiState: LabUiState,
    onStartLesson: () -> Unit,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onOpenLibrary: () -> Unit,
    onStartReadAir: () -> Unit,
    onRefresh: () -> Unit,
    onLessonModeSelected: (LessonMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentNode = uiState.lesson.nodes.getOrNull(uiState.lesson.index)
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TodayHero(uiState = uiState)
        }
        item {
            CourseSelector(
                uiState = uiState,
                onWorkSelected = onWorkSelected,
                onEpisodeSelected = onEpisodeSelected,
                onRefresh = onRefresh,
            )
        }
        item {
            LessonModeSelector(
                selectedMode = uiState.lessonMode,
                onModeSelected = onLessonModeSelected,
            )
        }
        item {
            ReadAirShortcut(
                episodeLabel = uiState.focus.episodeLabel,
                onStartReadAir = onStartReadAir,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(eyebrow = "今日路径", title = "今天下一步")
                TagChip(
                    text = "剩余 ${maxOf(0, uiState.lesson.nodes.size - uiState.lesson.answered)}",
                    selected = true,
                )
            }
        }
        itemsIndexed(uiState.lesson.nodes, key = { _, node -> node.id }) { index, node ->
            LearningPathNode(
                index = index,
                node = node,
                done = index < uiState.lesson.index,
                current = index == uiState.lesson.index,
                onStartLesson = onStartLesson,
            )
        }
        item {
            LabCard {
                SectionTitle(eyebrow = "本集重点", title = uiState.focus.episodeLabel)
                Text(uiState.focus.guidebook, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip("词汇 ${uiState.vocab.size}")
                    TagChip("语法 ${uiState.grammar.size}")
                    TagChip("跟读 ${uiState.shadowing.size}")
                }
                if (currentNode != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "下一题：${uiState.lesson.index + 1}. ${currentNode.title}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = currentNode.prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PrimaryButton(
                        text = "开始${uiState.lessonMode.titleLabel}",
                        onClick = onStartLesson,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(text = "资料", onClick = onOpenLibrary, modifier = Modifier.weight(1f))
                    SecondaryButton(text = "读空气", onClick = onStartReadAir, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReadAirShortcut(
    episodeLabel: String,
    onStartReadAir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LabCard(modifier = modifier) {
        SectionTitle(eyebrow = "读空气专项", title = "读懂这一集的潜台词")
        Text(
            text = "当前集 · $episodeLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PrimaryButton(
            text = "开始读空气专项",
            onClick = onStartReadAir,
        )
    }
}

@Composable
private fun LessonModeSelector(
    selectedMode: LessonMode,
    onModeSelected: (LessonMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "训练重点",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LessonMode.entries, key = { it.name }) { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.titleLabel, fontWeight = FontWeight.Bold) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseSelector(
    uiState: LabUiState,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedEpisodePage = episodePageFor(uiState.selection.episode)
    var episodePage by rememberSaveable(uiState.selection.workSlug) { mutableStateOf(selectedEpisodePage) }
    LaunchedEffect(uiState.selection.workSlug, uiState.selection.episode) {
        episodePage = episodePageFor(uiState.selection.episode)
    }
    val maxEpisode = uiState.works
        .firstOrNull { it.slug == uiState.selection.workSlug }
        ?.episodeCount
        ?: uiState.episodes.maxOfOrNull { it.episode }
        ?: uiState.selection.episode
    val selectableEpisodes = uiState.episodes.filter { it.episode <= maxEpisode }
    val episodePages = selectableEpisodes
        .map { episodePageFor(it.episode) }
        .distinct()
    val visibleEpisodes = selectableEpisodes.filter { episodePageFor(it.episode) == episodePage }
    val selectedEpisode = selectableEpisodes.firstOrNull { it.episode == uiState.selection.episode }
    var episodePickerOpen by rememberSaveable(uiState.selection.workSlug) { mutableStateOf(false) }

    LabCard(modifier = modifier) {
        SectionTitle(eyebrow = "作品 / 单集", title = uiState.focus.episodeLabel)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.works, key = { it.slug }) { work ->
                CourseWorkChip(
                    label = work.displayName,
                    selected = work.slug == uiState.selection.workSlug,
                    onClick = { onWorkSelected(work.slug) },
                )
            }
        }

        selectedEpisode?.let { episode ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill(
                    label = "句子",
                    value = episode.usableJaLines.toString(),
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                MetricPill(
                    label = "chunks",
                    value = episode.chunkCount.toString(),
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                MetricPill(
                    label = "字幕",
                    value = episode.totalCues.toString(),
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(
                text = if (episodePickerOpen) "收起选集" else "换集",
                onClick = { episodePickerOpen = !episodePickerOpen },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(text = "更新资料", onClick = onRefresh, modifier = Modifier.weight(1f))
        }

        if (episodePickerOpen) {
            if (episodePages.size > 1) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    episodePages.forEach { page ->
                        FilterChip(
                            selected = page == episodePage,
                            onClick = { episodePage = page },
                            label = { Text(episodePageRangeLabel(page, selectableEpisodes), fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                visibleEpisodes.forEach { episode ->
                    FilterChip(
                        selected = episode.episode == uiState.selection.episode,
                        onClick = { onEpisodeSelected(episode.episode) },
                        label = { Text(episode.label) },
                    )
                }
            }
        }
        MetricPill(
            label = "同步",
            value = uiState.sync.status.labelForToday(),
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        if (uiState.sync.message.isNotBlank()) {
            Text(uiState.sync.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CourseWorkChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .minimumInteractiveComponentSize()
            .clickable(
                role = Role.Button,
                onClickLabel = "选择作品 $label",
                onClick = onClick,
            ),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = shape,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val EpisodePageSize = 12

private fun episodePageFor(episode: Int): Int {
    return ((episode.coerceAtLeast(1) - 1) / EpisodePageSize)
}

private fun episodePageRangeLabel(page: Int, episodes: List<EpisodeOption>): String {
    val pageEpisodes = episodes.filter { episodePageFor(it.episode) == page }
    val start = pageEpisodes.firstOrNull()?.episode ?: (page * EpisodePageSize + 1)
    val end = pageEpisodes.lastOrNull()?.episode ?: (start + EpisodePageSize - 1)
    return "查看 EP${start.toString().padStart(2, '0')}-${end.toString().padStart(2, '0')}"
}

private fun SyncStatus.labelForToday(): String {
    return when (this) {
        SyncStatus.Idle -> "待同步"
        SyncStatus.Loading -> "同步中"
        SyncStatus.Success -> "已同步"
        SyncStatus.Error -> "需重试"
    }
}

@Composable
private fun TodayHero(
    uiState: LabUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "今日单元",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                    )
                    Text(
                        text = uiState.focus.lessonTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${uiState.focus.workTitle} · ${uiState.focus.episodeLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    modifier = Modifier.size(50.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stars,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { uiState.lesson.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TodayHeroStat(label = "连续", value = "${uiState.focus.streakDays}天", modifier = Modifier.weight(1f))
                TodayHeroStat(label = "能量", value = "${uiState.focus.energy}/5", modifier = Modifier.weight(1f))
                TodayHeroStat(label = "XP", value = "${uiState.focus.xp + uiState.sessionXp}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TodayHeroStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LearningPathNode(
    index: Int,
    node: LessonNode,
    done: Boolean,
    current: Boolean,
    onStartLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alignRight = index % 2 == 1
    val locked = !done && !current
    val statusLabel = pathStatusLabel(done = done, current = current)
    Box(modifier = modifier.fillMaxWidth()) {
        TodayPathNodeRow(
            index = index,
            node = node,
            alignRight = alignRight,
            current = current,
            done = done,
            locked = locked,
            statusLabel = statusLabel,
            modifier = Modifier.fillMaxWidth(),
        )
        if (current) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        onClickLabel = "开始当前训练",
                        role = Role.Button,
                        onClick = onStartLesson,
                    ),
            )
        }
    }
}

@Composable
private fun TodayPathNodeRow(
    index: Int,
    node: LessonNode,
    alignRight: Boolean,
    current: Boolean,
    done: Boolean,
    locked: Boolean,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (alignRight) Spacer(modifier = Modifier.weight(0.16f))
        if (alignRight && current) {
            TodayCurrentPathBubble(
                index = index,
                node = node,
                modifier = Modifier.weight(1f),
            )
        }
        TodayPathOrb(
            current = current,
            done = done,
            locked = locked,
            node = node,
        )
        if (!alignRight && current) {
            TodayCurrentPathBubble(
                index = index,
                node = node,
                modifier = Modifier.weight(1f),
            )
        }
        if (!current) {
            TodayPathCaption(
                index = index,
                title = node.title,
                statusLabel = statusLabel,
                highlighted = done,
                alignRight = alignRight,
                modifier = Modifier.weight(1f),
            )
        }
        if (!alignRight) Spacer(modifier = Modifier.weight(0.16f))
    }
}

@Composable
private fun TodayPathOrb(
    current: Boolean,
    done: Boolean,
    locked: Boolean,
    node: LessonNode,
) {
    val color = when {
        done -> MaterialTheme.colorScheme.primary
        current -> LabPalette.Yellow
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        done -> MaterialTheme.colorScheme.onPrimary
        current -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val modifier = Modifier.size(if (current) 72.dp else 58.dp)

    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = CircleShape,
        tonalElevation = if (current) 6.dp else 1.dp,
    ) {
        TodayPathOrbContent(done = done, current = current, locked = locked, node = node)
    }
}

@Composable
private fun TodayPathOrbContent(
    done: Boolean,
    current: Boolean,
    locked: Boolean,
    node: LessonNode,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = when {
                done -> Icons.Rounded.CheckCircle
                current -> Icons.Rounded.PlayArrow
                locked -> Icons.Rounded.Lock
                else -> node.pathIcon()
            },
            contentDescription = null,
        )
    }
}

@Composable
private fun TodayCurrentPathBubble(
    index: Int,
    node: LessonNode,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}. ${node.title}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TagChip(text = "继续", selected = true)
            }
            Text(
                text = node.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TodayPathCaption(
    index: Int,
    title: String,
    statusLabel: String,
    highlighted: Boolean,
    alignRight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "${index + 1}. $title",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            textAlign = if (alignRight) TextAlign.End else TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun LessonNode.pathIcon() = when (typeLabel) {
    "读空气" -> Icons.Rounded.Psychology
    "学习卡" -> Icons.Rounded.AutoStories
    else -> Icons.Rounded.Bolt
}

private fun pathStatusLabel(done: Boolean, current: Boolean): String {
    return when {
        done -> "已完成"
        current -> "继续"
        else -> "待解锁"
    }
}
