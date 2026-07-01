package com.animejapaneselab.nativeapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.LinguisticSceneLine
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.ui.LabTab
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.ReadAirAllFilter
import com.animejapaneselab.nativeapp.ui.ReadAirFilters
import com.animejapaneselab.nativeapp.ui.ReadAirMode
import com.animejapaneselab.nativeapp.ui.audio.rememberFeedbackSoundController
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.LearningChoiceButton
import com.animejapaneselab.nativeapp.ui.components.RewardMetricCard
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.StructuredAiResultCard
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.feedback.performAnswerFeedbackHaptic
import com.animejapaneselab.nativeapp.ui.feedback.performCompletionFeedbackHaptic
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadAirScreen(
    uiState: LabUiState,
    onRefresh: () -> Unit,
    onWorkSelected: (String) -> Unit,
    onDomainSelected: (String) -> Unit,
    onPhenomenonSelected: (String) -> Unit,
    onQuestionTypeSelected: (String) -> Unit,
    onDifficultySelected: (String) -> Unit,
    onEpisodeSelected: (Int?) -> Unit,
    onModeSelected: (ReadAirMode) -> Unit,
    onResetFilters: () -> Unit,
    onResetQueue: () -> Unit,
    onStartSession: () -> Unit,
    onBrowseAnswer: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readAir = uiState.readAir
    val exercise = readAir.currentExercise
    val scopedExercises = readAir.scopedExercises
    val filteredExercises = readAir.filteredExercises
    val answeredScopedCount = readAir.answeredScopedCount
    val remainingScopedCount = readAir.remainingScopedCount
    val currentPosition = if (exercise == null) 0 else readAir.currentIndex.coerceIn(0, filteredExercises.lastIndex) + 1
    val scopeTitle = readAirScopeTitle(readAir.filters)
    val scopeDetail = readAirScopeDetail(readAir.filters, scopedExercises.size, readAir.exercises.size)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ReadAirHubHero(
                exercise = exercise,
                total = readAir.exercises.size,
                scoped = scopedExercises.size,
                remaining = remainingScopedCount,
                answered = answeredScopedCount,
                current = currentPosition,
                scopeTitle = scopeTitle,
                scopeDetail = scopeDetail,
                status = readAir.status,
                usingFallback = readAir.usingFallback,
                message = readAir.message,
                onRefresh = onRefresh,
            )
        }

        item {
            ReadAirModePanel(
                selectedMode = readAir.mode,
                queue = filteredExercises.size,
                scoped = scopedExercises.size,
                remaining = remainingScopedCount,
                answered = answeredScopedCount,
                total = readAir.exercises.size,
                scopeTitle = scopeTitle,
                onModeSelected = onModeSelected,
                onResetFilters = onResetFilters,
                onResetQueue = onResetQueue,
            )
        }

        if (readAir.mode == ReadAirMode.Train) {
            item {
                ReadAirTaskPath(
                    currentExercise = exercise,
                    exercises = filteredExercises,
                    currentPosition = currentPosition,
                    onStartSession = onStartSession,
                    onResetFilters = onResetFilters,
                    onResetQueue = onResetQueue,
                )
            }
        }

        item {
            FilterPanel(
                works = readAir.workOptions,
                selectedWork = readAir.filters.workSlug,
                onWorkSelected = onWorkSelected,
                domains = readAir.domainOptions,
                selectedDomain = readAir.filters.domain,
                onDomainSelected = onDomainSelected,
                phenomena = readAir.phenomenonOptions,
                selectedPhenomenon = readAir.filters.phenomenonKey,
                onPhenomenonSelected = onPhenomenonSelected,
                questionTypes = readAir.questionTypeOptions,
                selectedQuestionType = readAir.filters.questionType,
                onQuestionTypeSelected = onQuestionTypeSelected,
                difficulties = readAir.difficultyOptions,
                selectedDifficulty = readAir.filters.difficulty,
                onDifficultySelected = onDifficultySelected,
                episodes = readAir.episodeOptions,
                selectedEpisode = readAir.filters.episode,
                onEpisodeSelected = onEpisodeSelected,
            )
        }

        if (readAir.mode == ReadAirMode.Browse) {
            item {
                ReadAirBrowseHeader(
                    count = scopedExercises.size,
                    answered = answeredScopedCount,
                    onResetFilters = onResetFilters,
                )
            }
            if (scopedExercises.isEmpty()) {
                item { ReadAirEmptyNode(onResetFilters = onResetFilters) }
            } else {
                itemsIndexed(
                    items = scopedExercises,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "read-air-browse-card" },
                ) { index, item ->
                    BrowseExerciseCard(
                        index = index + 1,
                        exercise = item,
                        selectedAnswer = readAir.browseAnswerFor(item.id),
                        onAnswerSelected = { option -> onBrowseAnswer(item.id, option) },
                    )
                }
            }
        } else if (exercise == null) {
            item {
                LabCard {
                    val queueDrained = scopedExercises.isNotEmpty() && filteredExercises.isEmpty()
                    Text(
                        if (queueDrained) "今日练习已完成" else "没有可练题目",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (queueDrained) "这些题已经记录进度，可在错题页复习或调整筛选。" else "换个范围，或从云端更新练习。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = onResetFilters,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Text("清空筛选", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                    }
                }
            }
        } else if (readAir.message.isNotBlank() || readAir.usingFallback || readAir.status == SyncStatus.Error) {
            item {
                StatusRow(
                    message = readAir.message,
                    total = readAir.exercises.size,
                    scoped = scopedExercises.size,
                    queue = filteredExercises.size,
                    current = currentPosition,
                    usingFallback = readAir.usingFallback,
                    status = readAir.status,
                )
            }
        }
    }
}

@Composable
private fun ReadAirTaskPath(
    currentExercise: LinguisticExercise?,
    exercises: List<LinguisticExercise>,
    currentPosition: Int,
    onStartSession: () -> Unit,
    onResetFilters: () -> Unit,
    onResetQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(eyebrow = "今日语感路径", title = "读懂下一句")
            TagChip("剩余 ${exercises.size}", selected = true)
        }
        val previewItems = exercises.take(4)
        if (previewItems.isEmpty()) {
            ReadAirEmptyNode(
                onResetFilters = onResetFilters,
                onResetQueue = onResetQueue,
            )
        } else {
            Button(
                onClick = onStartSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text("开始训练", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
            }
            previewItems.forEachIndexed { index, item ->
                val isCurrent = item.id == currentExercise?.id
                ReadAirPathNode(
                    index = currentPosition.takeIf { isCurrent } ?: (currentPosition + index),
                    exercise = item,
                    current = isCurrent,
                    onStartSession = onStartSession,
                )
            }
        }
    }
}

@Composable
private fun ReadAirPathNode(
    index: Int,
    exercise: LinguisticExercise,
    current: Boolean,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alignRight = index % 2 == 0
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (alignRight) {
                ReadAirPathCaption(
                    index = index,
                    exercise = exercise,
                    current = current,
                    alignRight = true,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(0.18f))
            }
            Surface(
                modifier = Modifier.size(if (current) 72.dp else 58.dp),
                color = if (current) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (current) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
                tonalElevation = if (current) 6.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (current) Icons.Rounded.PlayArrow else Icons.Rounded.Psychology,
                        contentDescription = null,
                    )
                }
            }
            if (!alignRight) {
                ReadAirPathCaption(
                    index = index,
                    exercise = exercise,
                    current = current,
                    alignRight = false,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(0.18f))
            }
        }
        if (current) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        onClickLabel = "开始读空气训练",
                        role = Role.Button,
                        onClick = onStartSession,
                    ),
            )
        }
    }
}

@Composable
private fun ReadAirPathCaption(
    index: Int,
    exercise: LinguisticExercise,
    current: Boolean,
    alignRight: Boolean,
    modifier: Modifier = Modifier,
) {
    val phrase = exercise.jaText.ifBlank { exercise.sceneLines.firstOrNull { it.isTarget }?.jaText.orEmpty() }
    Surface(
        modifier = modifier,
        color = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (current) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = if (current) 2.dp else 1.dp,
            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        shadowElevation = if (current) 3.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$index. ${questionTypeLabel(exercise.questionType)}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = if (alignRight) TextAlign.End else TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (current) TagChip("继续", selected = true)
            }
            Text(
                text = phrase.ifBlank { exercise.prompt },
                color = if (current) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = if (alignRight) TextAlign.End else TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReadAirEmptyNode(
    modifier: Modifier = Modifier,
    onResetFilters: (() -> Unit)? = null,
    onResetQueue: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "当前范围没有可练题目",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            if (onResetQueue != null) {
                OutlinedButton(
                    onClick = onResetQueue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp),
                ) {
                    Icon(Icons.Rounded.Replay, contentDescription = null)
                    Text("重置队列再练", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
            }
            if (onResetFilters != null) {
                OutlinedButton(
                    onClick = onResetFilters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("清空筛选", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ReadAirHubHero(
    exercise: LinguisticExercise?,
    total: Int,
    scoped: Int,
    remaining: Int,
    answered: Int,
    current: Int,
    scopeTitle: String,
    scopeDetail: String,
    status: SyncStatus,
    usingFallback: Boolean,
    message: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "语感训练",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                    )
                    Text(
                        text = scopeTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = scopeDetail,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = exercise?.prompt ?: if (scoped == 0) "换个范围，或更新今日练习" else "今日练习已完成",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                        maxLines = 2,
                    )
                }
                Surface(
                    modifier = Modifier.size(50.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = onRefresh,
                            enabled = status != SyncStatus.Loading,
                        ) {
                            Icon(
                                imageVector = if (status == SyncStatus.Loading) Icons.Rounded.Psychology else Icons.Rounded.Refresh,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReadAirHubStat(label = "待练", value = "$remaining", modifier = Modifier.weight(1f))
                ReadAirHubStat(label = "筛选", value = "$scoped", modifier = Modifier.weight(1f))
                ReadAirHubStat(label = "已答", value = "$answered", modifier = Modifier.weight(1f))
            }
            exercise?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReadAirMiniChip(text = domainLabel(it.domain), selected = true)
                    ReadAirMiniChip(text = questionTypeLabel(it.questionType), selected = false)
                    if (usingFallback) ReadAirMiniChip(text = "样例题", selected = false)
                }
            }
            if (message.isNotBlank() && status == SyncStatus.Error) {
                Text(message, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReadAirHubStat(
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
private fun ReadAirMiniChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (selected) 0.20f else 0.12f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadAirModePanel(
    selectedMode: ReadAirMode,
    queue: Int,
    scoped: Int,
    remaining: Int,
    answered: Int,
    total: Int,
    scopeTitle: String,
    onModeSelected: (ReadAirMode) -> Unit,
    onResetFilters: () -> Unit,
    onResetQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LabCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReadAirModeButton(
                mode = ReadAirMode.Train,
                selected = selectedMode == ReadAirMode.Train,
                onSelected = onModeSelected,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp),
            )
            ReadAirModeButton(
                mode = ReadAirMode.Browse,
                selected = selectedMode == ReadAirMode.Browse,
                onSelected = onModeSelected,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TagChip(scopeTitle, selected = true)
            TagChip(
                if (selectedMode == ReadAirMode.Train) "队列 $queue · 待练 $remaining/$scoped" else "浏览结果 $scoped",
            )
            TagChip("已答 $answered")
            TagChip("总题库 $total")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onResetQueue,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = null)
                Text("重置队列", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onResetFilters,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Text("清空筛选", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReadAirModeButton(
    mode: ReadAirMode,
    selected: Boolean,
    onSelected: (ReadAirMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = { onSelected(mode) },
            modifier = modifier,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        OutlinedButton(
            onClick = { onSelected(mode) },
            modifier = modifier,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ReadAirSessionScreen(
    uiState: LabUiState,
    onExit: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readAir = uiState.readAir
    val exercise = readAir.currentExercise
    val selectedAnswer = exercise?.let { readAir.selectedAnswerFor(it.id) }.orEmpty()
    val scopedExercises = readAir.scopedExercises
    val sessionTotal = scopedExercises.size
    val answeredCount = scopedExercises.count { readAir.selectedAnswerFor(it.id).isNotBlank() }
    val correctCount = scopedExercises.count { item ->
        val answer = readAir.selectedAnswerFor(item.id)
        answer.isNotBlank() && item.isCorrect(answer)
    }
    val scopeTitle = readAirScopeTitle(readAir.filters)
    val scopeDetail = readAirScopeDetail(readAir.filters, scopedExercises.size, readAir.exercises.size)
    val exitLabel = when (uiState.selectedTab) {
        LabTab.Library -> "返回本集入口"
        LabTab.Today -> "返回今日页"
        LabTab.Review -> "返回错题页"
        else -> "返回读空气入口"
    }
    val currentPosition = when {
        sessionTotal == 0 -> 0
        exercise == null -> sessionTotal
        selectedAnswer.isNotBlank() -> answeredCount.coerceIn(1, sessionTotal)
        else -> (answeredCount + 1).coerceIn(1, sessionTotal)
    }
    val progress = when {
        sessionTotal == 0 -> 0f
        exercise == null -> 1f
        selectedAnswer.isNotBlank() -> answeredCount.toFloat() / sessionTotal.toFloat()
        else -> (currentPosition - 1).coerceAtLeast(0).toFloat() / sessionTotal.toFloat()
    }
    val feedbackSoundController = rememberFeedbackSoundController()
    val feedbackView = LocalView.current
    val readAirScrollState = rememberScrollState()

    BackHandler(onBack = onExit)

    LaunchedEffect(exercise?.id) {
        readAirScrollState.scrollTo(0)
    }
    LaunchedEffect(exercise?.id, selectedAnswer) {
        if (exercise != null && selectedAnswer.isNotBlank()) {
            val correct = exercise.isCorrect(selectedAnswer)
            if (uiState.settings.feedbackSounds) feedbackSoundController.play(correct)
            feedbackView.performAnswerFeedbackHaptic(correct)
        }
    }
    LaunchedEffect(readAir.aiCoach.status, readAir.aiCoach.answer) {
        if (readAir.aiCoach.status == SyncStatus.Loading) {
            delay(120)
            readAirScrollState.animateScrollTo(readAirScrollState.maxValue)
        }
    }

    if (exercise == null) {
        ReadAirComplete(
            completed = answeredCount,
            correct = correctCount,
            xp = uiState.sessionXp,
            energy = uiState.focus.energy,
            scopeTitle = scopeTitle,
            feedbackSounds = uiState.settings.feedbackSounds,
            exitLabel = exitLabel,
            onRestart = onRestart,
            onExit = onExit,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        ReadAirSessionTopBar(
            current = currentPosition,
            total = sessionTotal,
            progress = progress,
            scopeTitle = scopeTitle,
            scopeDetail = scopeDetail,
            exitLabel = exitLabel,
            onExit = onExit,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(readAirScrollState)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionTitle(
                eyebrow = exercise.sessionEyebrow(),
                title = "读出隐藏语气",
            )
            ExerciseCard(
                exercise = exercise,
                selectedAnswer = selectedAnswer,
                onAnswerSelected = onAnswerSelected,
            )
            if (readAir.aiCoach.answer.isNotBlank() || readAir.aiCoach.status == SyncStatus.Loading || readAir.aiCoach.status == SyncStatus.Error) {
                StructuredAiResultCard(
                    result = readAir.aiCoach.result,
                    fallbackText = readAir.aiCoach.answer,
                )
            }
        }
        if (selectedAnswer.isNotBlank()) {
            ReadAirSessionDock(
                exercise = exercise,
                selectedAnswer = selectedAnswer,
                correctXp = 8,
                remainingCount = (sessionTotal - answeredCount).coerceAtLeast(0),
                isLastQuestion = answeredCount >= sessionTotal,
                onNext = onNext,
                onAskAi = onAskAi,
                aiStatus = readAir.aiCoach.status,
            )
        }
    }
}

@Composable
private fun ReadAirSessionTopBar(
    current: Int,
    total: Int,
    progress: Float,
    scopeTitle: String,
    scopeDetail: String,
    exitLabel: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = exitLabel)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (total == 0) "0 / 0" else "$current / $total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = scopeTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = scopeDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ReadAirComplete(
    completed: Int,
    correct: Int,
    xp: Int,
    energy: Int,
    scopeTitle: String,
    feedbackSounds: Boolean,
    exitLabel: String,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedbackSoundController = rememberFeedbackSoundController()
    val feedbackView = LocalView.current
    val accuracy = if (completed == 0) 0 else correct * 100 / completed
    LaunchedEffect(completed, feedbackSounds) {
        if (completed > 0) {
            delay(260)
            if (feedbackSounds) feedbackSoundController.playCompletion()
            feedbackView.performCompletionFeedbackHaptic()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
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
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(42.dp))
                    }
                }
                Text(
                    text = "读空气完成",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "你已经读完当前筛选队列。",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                )
                Text(
                    text = "本轮范围：$scopeTitle。错题会留在复习页，下一轮可以换范围或重练这一组。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RewardMetricCard(
                label = "完成",
                value = completed.toString(),
                icon = Icons.Rounded.Check,
                progress = 1f,
                modifier = Modifier.weight(1f),
            )
            RewardMetricCard(
                label = "正确率",
                value = "$accuracy%",
                icon = Icons.Rounded.Psychology,
                progress = accuracy / 100f,
                modifier = Modifier.weight(1f),
            )
            RewardMetricCard(
                label = "XP",
                value = "+$xp",
                icon = Icons.Rounded.EmojiEvents,
                progress = 1f,
                modifier = Modifier.weight(1f),
                highlighted = true,
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("本轮摘要", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    text = "答对 $correct / 已答 $completed，当前能量 $energy/5。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TagChip("语用")
                    TagChip("语气")
                    TagChip("场景")
                }
            }
        }
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .heightIn(min = 54.dp),
        ) {
            Icon(Icons.Rounded.Replay, contentDescription = null)
            Text("再练这一组", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
        }
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .heightIn(min = 52.dp),
        ) {
            Text(exitLabel, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ReadAirSessionDock(
    exercise: LinguisticExercise,
    selectedAnswer: String,
    correctXp: Int,
    remainingCount: Int,
    isLastQuestion: Boolean,
    onNext: () -> Unit,
    onAskAi: () -> Unit,
    aiStatus: SyncStatus,
    modifier: Modifier = Modifier,
) {
    val correct = exercise.isCorrect(selectedAnswer)
    val color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val answerExplanation = exercise.basicExplanationZh.ifBlank {
        if (correct) "这次读出了台词里的关系变化。" else "先记住正确语气，后面会在复习里再遇到。"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color,
        contentColor = Color.White,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (correct) Icons.Rounded.Check else Icons.Rounded.Close, contentDescription = null)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (correct) "答对了" else "再读一下语气",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (correct) {
                            answerExplanation
                        } else {
                            "正确答案：${exercise.correctOption}。$answerExplanation"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        color = Color.White,
                        contentColor = color,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = if (correct) "+$correctXp XP" else "已加入复习",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = color),
            ) {
                Text(if (isLastQuestion) "完成" else "继续", fontWeight = FontWeight.Black)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (isLastQuestion) "本轮已完成" else "还剩 $remainingCount 题",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
                OutlinedButton(
                    onClick = onAskAi,
                    enabled = aiStatus != SyncStatus.Loading,
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                ) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null)
                    Text(if (aiStatus == SyncStatus.Loading) "解释中" else "讲解", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun LinguisticExercise.sessionEyebrow(): String {
    return phenomenonNameZh.ifBlank { domainLabel(domain) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadAirBrowseHeader(
    count: Int,
    answered: Int,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(eyebrow = "题库浏览", title = "全部筛选结果")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TagChip("$count 题", selected = true)
            TagChip("已答 $answered")
            OutlinedButton(
                onClick = onResetFilters,
                modifier = Modifier.heightIn(min = 40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("清空筛选", modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowseExerciseCard(
    index: Int,
    exercise: LinguisticExercise,
    selectedAnswer: String,
    onAnswerSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LabCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#$index",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            TagChip(sourceLabel(exercise), selected = true)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip(domainLabel(exercise.domain))
            TagChip(phenomenonLabel(exercise.phenomenonKey))
            TagChip(questionTypeLabel(exercise.questionType))
            if (exercise.difficulty.isNotBlank()) TagChip(difficultyLabel(exercise.difficulty))
        }
        DialogueBlock(exercise)
        Text(
            text = exercise.prompt,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            exercise.options.forEach { option ->
                OptionButton(
                    option = option,
                    selected = selectedAnswer == option,
                    correct = selectedAnswer.isNotBlank() && option == exercise.correctOption,
                    answered = selectedAnswer.isNotBlank(),
                    onClick = { onAnswerSelected(option) },
                )
            }
        }
        if (selectedAnswer.isNotBlank()) {
            BrowseAnswerSummary(exercise = exercise, selectedAnswer = selectedAnswer)
        }
    }
}

@Composable
private fun BrowseAnswerSummary(
    exercise: LinguisticExercise,
    selectedAnswer: String,
    modifier: Modifier = Modifier,
) {
    val correct = exercise.isCorrect(selectedAnswer)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (correct) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (correct) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (correct) Icons.Rounded.Check else Icons.Rounded.Close, contentDescription = null)
                Text(
                    text = if (correct) "判断正确" else "这题需要回炉",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                )
            }
            Text("你的选择：$selectedAnswer", fontWeight = FontWeight.Bold)
            Text("正确答案：${exercise.correctOption}", fontWeight = FontWeight.Bold)
            listOf(
                "基础说明" to exercise.basicExplanationZh,
                "深入解释" to exercise.deepExplanationZh,
                "动画语境" to exercise.animeContextNoteZh,
                "注意事项" to exercise.cautionNoteZh,
            ).forEach { (label, value) ->
                if (value.isNotBlank()) {
                    Text("$label：$value", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusRow(
    message: String,
    total: Int,
    scoped: Int,
    queue: Int,
    current: Int,
    usingFallback: Boolean,
    status: SyncStatus,
) {
    LabCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip("总题 $total", selected = true)
            TagChip("可练 $scoped")
            TagChip("今日 $queue")
            if (current > 0) TagChip("$current/$queue")
            if (usingFallback) TagChip("样例题")
            if (status == SyncStatus.Error) TagChip("更新失败")
        }
        if (message.isNotBlank()) {
            Text(
                text = message,
                color = if (status == SyncStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    works: List<String>,
    selectedWork: String,
    onWorkSelected: (String) -> Unit,
    domains: List<String>,
    selectedDomain: String,
    onDomainSelected: (String) -> Unit,
    phenomena: List<String>,
    selectedPhenomenon: String,
    onPhenomenonSelected: (String) -> Unit,
    questionTypes: List<String>,
    selectedQuestionType: String,
    onQuestionTypeSelected: (String) -> Unit,
    difficulties: List<String>,
    selectedDifficulty: String,
    onDifficultySelected: (String) -> Unit,
    episodes: List<Int>,
    selectedEpisode: Int?,
    onEpisodeSelected: (Int?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "训练范围",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    if (expanded) "收起筛选" else "展开筛选",
                    modifier = Modifier.padding(start = 4.dp),
                    fontWeight = FontWeight.Black,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TagChip(workLabel(selectedWork), selected = true)
            TagChip(selectedEpisode?.let { "EP${it.toString().padStart(2, '0')}" } ?: "全部集数", selected = true)
            TagChip(domainLabel(selectedDomain))
            TagChip(phenomenonLabel(selectedPhenomenon))
            TagChip(questionTypeLabel(selectedQuestionType))
            TagChip(difficultyLabel(selectedDifficulty))
        }
        if (expanded) {
            if (works.size > 1) {
                FilterGroup(label = "作品") {
                    works.forEach { work ->
                        FilterChip(
                            selected = selectedWork == work,
                            onClick = { onWorkSelected(work) },
                            label = { Text(workLabel(work)) },
                        )
                    }
                }
            }
            if (episodes.size > 1) {
                EpisodeFilterGroup(
                    episodes = episodes,
                    selectedEpisode = selectedEpisode,
                    onEpisodeSelected = onEpisodeSelected,
                )
            }
            FilterGroup(label = "领域") {
                domains.forEach { domain ->
                    FilterChip(
                        selected = selectedDomain == domain,
                        onClick = { onDomainSelected(domain) },
                        label = { Text(domainLabel(domain)) },
                    )
                }
            }
            FilterGroup(label = "语言现象") {
                phenomena.forEach { phenomenon ->
                    FilterChip(
                        selected = selectedPhenomenon == phenomenon,
                        onClick = { onPhenomenonSelected(phenomenon) },
                        label = { Text(phenomenonLabel(phenomenon)) },
                    )
                }
            }
            FilterGroup(label = "题型") {
                questionTypes.forEach { questionType ->
                    FilterChip(
                        selected = selectedQuestionType == questionType,
                        onClick = { onQuestionTypeSelected(questionType) },
                        label = { Text(questionTypeLabel(questionType)) },
                    )
                }
            }
            if (difficulties.size > 1) {
                FilterGroup(label = "难度") {
                    difficulties.forEach { difficulty ->
                        FilterChip(
                            selected = selectedDifficulty == difficulty,
                            onClick = { onDifficultySelected(difficulty) },
                            label = { Text(difficultyLabel(difficulty)) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeFilterGroup(
    episodes: List<Int>,
    selectedEpisode: Int?,
    onEpisodeSelected: (Int?) -> Unit,
) {
    val pages = episodes.chunked(ReadAirEpisodeFilterPageSize)
    val selectedEpisodePage = selectedEpisode
        ?.let { episode -> pages.indexOfFirst { page -> episode in page } }
        ?.takeIf { it >= 0 }
        ?: 0
    var selectedPage by rememberSaveable(
        episodes.firstOrNull() ?: 0,
        episodes.lastOrNull() ?: 0,
        episodes.size,
    ) {
        mutableIntStateOf(selectedEpisodePage)
    }
    val safePage = selectedPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val currentPage = pages.getOrElse(safePage) { emptyList() }

    LaunchedEffect(selectedEpisode, episodes.size) {
        if (selectedEpisodePage != safePage) {
            selectedPage = selectedEpisodePage
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "集数",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${episodes.size} 集可筛",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        if (pages.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.forEachIndexed { index, page ->
                    val start = page.firstOrNull() ?: return@forEachIndexed
                    val end = page.lastOrNull() ?: start
                    FilterChip(
                        selected = safePage == index,
                        onClick = { selectedPage = index },
                        label = { Text("查看 EP${start.twoDigit()}-${end.twoDigit()}") },
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedEpisode == null,
                onClick = { onEpisodeSelected(null) },
                label = { Text("全部集数") },
            )
            currentPage.forEach { episode ->
                FilterChip(
                    selected = selectedEpisode == episode,
                    onClick = { onEpisodeSelected(episode) },
                    label = { Text("EP${episode.twoDigit()}") },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    exercise: LinguisticExercise,
    selectedAnswer: String,
    onAnswerSelected: (String) -> Unit,
) {
    val answered = selectedAnswer.isNotBlank()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip(domainLabel(exercise.domain), selected = true)
            TagChip(questionTypeLabel(exercise.questionType))
            if (exercise.difficulty.isNotBlank()) TagChip(difficultyLabel(exercise.difficulty))
            if (exercise.sourceLineNo > 0) TagChip("第 ${exercise.sourceLineNo} 行")
            if (exercise.status == "draft") TagChip("草稿题")
        }
        Text(
            text = exercise.prompt,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        DialogueBlock(exercise)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            exercise.options.forEach { option ->
                OptionButton(
                    option = option,
                    selected = selectedAnswer == option,
                    correct = answered && option == exercise.correctOption,
                    answered = answered,
                    onClick = { onAnswerSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun DialogueBlock(exercise: LinguisticExercise) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "台词场景",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
        if (exercise.sceneLines.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercise.sceneLines.forEach { line ->
                    DialogueLine(line)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = exercise.jaText.ifBlank { "未提供日文原句" },
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                if (exercise.zhText.isNotBlank()) {
                    Text(
                        text = exercise.zhText,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogueLine(line: LinguisticSceneLine) {
    val backgroundColor = if (line.isTarget) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (line.isTarget) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .heightIn(min = 44.dp)
                .background(
                    if (line.isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape,
                ),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (line.speaker.isNotBlank()) TagChip(line.speaker, selected = line.isTarget)
                if (line.isTarget) TagChip("目标", selected = true)
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = line.jaText,
                    modifier = Modifier.fillMaxWidth(),
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                if (line.zhText.isNotBlank()) {
                    Text(
                        text = line.zhText,
                        modifier = Modifier.fillMaxWidth(),
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    option: String,
    selected: Boolean,
    correct: Boolean,
    answered: Boolean,
    onClick: () -> Unit,
) {
    LearningChoiceButton(
        text = option,
        onClick = { if (!answered) onClick() },
        selected = selected && !answered,
        correct = correct,
        wrong = answered && selected && !correct,
        enabled = !answered,
        trailing = {
            if (correct) {
                Icon(Icons.Rounded.Check, contentDescription = null)
            } else if (answered && selected) {
                Icon(Icons.Rounded.Close, contentDescription = null)
            }
        },
    )
}


private fun domainLabel(domain: String): String {
    return when (domain) {
        ReadAirAllFilter -> "全部领域"
        "phonology" -> "音系学"
        "morphology" -> "形态学"
        "syntax" -> "句法学"
        "pragmatics" -> "语用学"
        "historical" -> "历史语言学"
        "sociolinguistics" -> "社会语言学"
        else -> domain
    }
}

private fun workLabel(workSlug: String): String {
    return when (workSlug) {
        ReadAirAllFilter -> "全部作品"
        "rezero", "re-zero" -> "Re:Zero"
        "k-on" -> "K-ON!"
        else -> workSlug
    }
}

private fun sourceLabel(exercise: LinguisticExercise): String {
    val episode = if (exercise.episode > 0) "EP${exercise.episode.twoDigit()}" else "EP--"
    val line = if (exercise.sourceLineNo > 0) "Line ${exercise.sourceLineNo}" else "Line --"
    return "${workLabel(exercise.workSlug)} · $episode · $line"
}

private fun phenomenonLabel(phenomenon: String): String {
    return when (phenomenon) {
        ReadAirAllFilter -> "全部语言现象"
        "soft_obligation_ellipsis" -> "委婉省略"
        "soft_obligation" -> "委婉义务"
        "relationship_check" -> "关系确认"
        "relationship_reading" -> "关系读解"
        "rhetorical_pressure" -> "反问压力"
        "topic_marker_tte" -> "って 话题化"
        "soft_refusal" -> "柔和拒绝"
        "shared_joke" -> "玩笑纠正"
        "politeness_distance" -> "礼貌距离"
        "self_blame_signal" -> "自责信号"
        "promise_witness" -> "承诺见证"
        "topic_shift" -> "话题转换"
        "ellipsis" -> "省略读解"
        else -> phenomenon
    }
}

private fun questionTypeLabel(questionType: String): String {
    return when (questionType) {
        ReadAirAllFilter -> "全部题型"
        "kuuki_yomi" -> "读空气"
        "listening_reasoning" -> "听感解释"
        "morphology_analysis" -> "形态分析"
        "syntax_relation" -> "句法关系"
        "multiple_choice" -> "选择题"
        "contrast_choice" -> "对比判断"
        "implicit_intent" -> "隐含意图"
        "relationship_reading" -> "关系读解"
        else -> questionType
    }
}

private fun difficultyLabel(difficulty: String): String {
    return when (difficulty) {
        ReadAirAllFilter -> "全部难度"
        "starter" -> "入门"
        "easy" -> "基础"
        "medium" -> "进阶"
        "hard" -> "挑战"
        else -> difficulty
    }
}

private fun readAirScopeTitle(filters: ReadAirFilters): String {
    val work = workLabel(filters.workSlug)
    val episode = filters.episode?.let { "EP${it.twoDigit()}" }
    val base = when {
        filters.workSlug == ReadAirAllFilter && episode == null -> "读空气全题库"
        filters.workSlug == ReadAirAllFilter -> "全部作品 $episode"
        episode == null -> "$work 全集"
        else -> "$work $episode 专项"
    }
    val narrowFilters = listOfNotNull(
        filters.domain.takeUnless { it == ReadAirAllFilter }?.let(::domainLabel),
        filters.phenomenonKey.takeUnless { it == ReadAirAllFilter }?.let(::phenomenonLabel),
        filters.questionType.takeUnless { it == ReadAirAllFilter }?.let(::questionTypeLabel),
        filters.difficulty.takeUnless { it == ReadAirAllFilter }?.let(::difficultyLabel),
    )
    return (listOf(base) + narrowFilters).joinToString(" · ")
}

private fun readAirScopeDetail(filters: ReadAirFilters, scoped: Int, total: Int): String {
    val parts = mutableListOf<String>()
    parts += if (filters.workSlug == ReadAirAllFilter) "全部作品" else workLabel(filters.workSlug)
    parts += filters.episode?.let { "EP${it.twoDigit()}" } ?: "全部集数"
    if (filters.domain != ReadAirAllFilter) parts += domainLabel(filters.domain)
    if (filters.phenomenonKey != ReadAirAllFilter) parts += phenomenonLabel(filters.phenomenonKey)
    if (filters.questionType != ReadAirAllFilter) parts += questionTypeLabel(filters.questionType)
    if (filters.difficulty != ReadAirAllFilter) parts += difficultyLabel(filters.difficulty)
    parts += "$scoped/$total 题"
    return parts.joinToString(" · ")
}

private const val ReadAirEpisodeFilterPageSize = 12

private fun Int.twoDigit(): String = toString().padStart(2, '0')
