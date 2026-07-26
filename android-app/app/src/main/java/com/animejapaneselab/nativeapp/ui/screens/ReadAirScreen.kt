package com.animejapaneselab.nativeapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.LinguisticSceneLine
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.data.buildExternalQuestionPrompt
import com.animejapaneselab.nativeapp.ui.LabTab
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.ReadAirAllFilter
import com.animejapaneselab.nativeapp.ui.ReadAirCognitiveTopic
import com.animejapaneselab.nativeapp.ui.ReadAirFilters
import com.animejapaneselab.nativeapp.ui.ReadAirMode
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterArtwork
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterRole
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonButton
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonChoiceCard
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonProgressBar
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonTone
import com.animejapaneselab.nativeapp.ui.components.RewardMetricCard
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LearningAssetRegistry
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.rive.DuolingoLikeVisualHost
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadAirScreen(
    uiState: LabUiState,
    onRefresh: () -> Unit,
    onWorkSelected: (String) -> Unit,
    onDomainSelected: (String) -> Unit,
    onQuestionTypeSelected: (String) -> Unit,
    onDifficultySelected: (String) -> Unit,
    onTopicSelected: (String) -> Unit,
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
    var showFilters by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LinguisticsHomeHero(
                exercise = exercise,
                total = readAir.exercises.size,
                remaining = remainingScopedCount,
                answered = answeredScopedCount,
                scopeTitle = scopeTitle,
                status = readAir.status,
                onRefresh = onRefresh,
            )
        }

        item {
            LinguisticsStartCard(
                selectedMode = readAir.mode,
                exercise = exercise,
                remaining = remainingScopedCount,
                answered = answeredScopedCount,
                scopeTitle = scopeTitle,
                onModeSelected = onModeSelected,
                onStart = onStartSession,
                onResetQueue = onResetQueue,
            )
        }

        item {
            LinguisticsFilterSummary(
                filters = readAir.filters,
                resultCount = scopedExercises.size,
                onOpenFilters = { showFilters = true },
            )
        }

        item {
            LinguisticsDomainGrid(
                domains = readAir.domainOptions,
                counts = readAir.domainCounts,
                selectedDomain = readAir.filters.domain,
                onDomainSelected = onDomainSelected,
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            color = if (queueDrained) LabTheme.colors.successContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (queueDrained) LabTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (queueDrained) Icons.Rounded.EmojiEvents else Icons.Rounded.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Text(
                            if (queueDrained) "今日练习已完成" else "没有可练题目",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            if (queueDrained) "这些题已经记录进度，可在错题页复习或调整筛选。" else "换个范围，或从云端更新练习。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    OutlinedButton(
                        onClick = onResetFilters,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
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

    if (showFilters) {
        LinguisticsFilterSheet(
            filters = readAir.filters,
            works = readAir.workOptions,
            episodes = readAir.episodeOptions,
            domains = readAir.domainOptions,
            questionTypes = readAir.questionTypeOptions,
            difficulties = readAir.difficultyOptions,
            topics = readAir.topicOptions,
            onWorkSelected = onWorkSelected,
            onEpisodeSelected = onEpisodeSelected,
            onDomainSelected = onDomainSelected,
            onQuestionTypeSelected = onQuestionTypeSelected,
            onDifficultySelected = onDifficultySelected,
            onTopicSelected = onTopicSelected,
            onReset = onResetFilters,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun LinguisticsHomeHero(
    exercise: LinguisticExercise?,
    total: Int,
    remaining: Int,
    answered: Int,
    scopeTitle: String,
    status: SyncStatus,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (total == 0) 0f else answered.toFloat() / total.toFloat()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        contentColor = LabTheme.colors.onHero,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .background(LabTheme.heroBrush())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text = "语言学训练", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "从动漫台词理解语气、结构与语言变化",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LabTheme.colors.onHero.copy(alpha = 0.85f),
                    )
                }
                Surface(
                    color = LabTheme.colors.onHero.copy(alpha = 0.16f),
                    contentColor = LabTheme.colors.onHero,
                    shape = CircleShape,
                ) {
                    IconButton(onClick = onRefresh, enabled = status != SyncStatus.Loading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新语言学题库")
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$answered 已完成  ·  $remaining 待练  ·  $total 题库",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = LabTheme.colors.onHero.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                )
                exercise?.let {
                    CourseCharacterArtwork(
                        workSlug = it.workSlug,
                        role = CourseCharacterRole.Linguistics,
                        motionEnabled = false,
                        stableSeed = it.id.hashCode(),
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
            JuicyLessonProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                progressColor = LabTheme.colors.onHero,
                trackColor = LabTheme.colors.onHero.copy(alpha = 0.24f),
            )
            Text(
                text = scopeTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = LabTheme.colors.onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinguisticsDomainGrid(
    domains: List<String>,
    counts: Map<String, Int>,
    selectedDomain: String,
    onDomainSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orderedDomains = remember(domains) {
        listOf("pragmatics", "syntax", "historical", "morphology", "phonology", "sociolinguistics")
            .filter(domains::contains)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(eyebrow = "分领域练习", title = "训练方向")
            ReadAirSelectChip(
                text = "全部",
                selected = selectedDomain == ReadAirAllFilter,
                onClick = { onDomainSelected(ReadAirAllFilter) },
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            orderedDomains.forEach { domain ->
                LinguisticsDomainCard(
                    domain = domain,
                    count = counts[domain] ?: 0,
                    selected = selectedDomain == domain,
                    onClick = { onDomainSelected(domain) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LinguisticsDomainCard(
    domain: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 76.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(21.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(domainLabel(domain), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Text(
                    "$count 题",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LinguisticsFilterSummary(
    filters: ReadAirFilters,
    resultCount: Int,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("当前范围 · $resultCount 题", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReadAirMiniFilterChip(workLabel(filters.workSlug))
                    ReadAirMiniFilterChip(filters.episode?.let { "EP${it.twoDigit()}" } ?: "全部集数")
                    if (filters.questionType != ReadAirAllFilter) ReadAirMiniFilterChip(questionTypeLabel(filters.questionType))
                    if (filters.difficulty != ReadAirAllFilter) ReadAirMiniFilterChip(difficultyLabel(filters.difficulty))
                    if (filters.topic != ReadAirAllFilter) ReadAirMiniFilterChip(topicLabel(filters.topic))
                }
            }
            OutlinedButton(
                onClick = onOpenFilters,
                modifier = Modifier.heightIn(min = 48.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("调整", modifier = Modifier.padding(start = 5.dp), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ReadAirMiniFilterChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = CircleShape,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LinguisticsStartCard(
    selectedMode: ReadAirMode,
    exercise: LinguisticExercise?,
    remaining: Int,
    answered: Int,
    scopeTitle: String,
    onModeSelected: (ReadAirMode) -> Unit,
    onStart: () -> Unit,
    onResetQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadAirModeButton(
                    mode = ReadAirMode.Train,
                    selected = selectedMode == ReadAirMode.Train,
                    onSelected = onModeSelected,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                )
                ReadAirModeButton(
                    mode = ReadAirMode.Browse,
                    selected = selectedMode == ReadAirMode.Browse,
                    onSelected = onModeSelected,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                )
            }
            if (selectedMode == ReadAirMode.Train) {
                Text(
                    text = if (answered > 0) "继续上次进度" else "开始第一组训练",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(scopeTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                exercise?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = linguisticPromptForDisplay(it.prompt),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                JuicyLessonButton(
                    text = if (remaining > 0) "继续学习 · 剩余 $remaining 题" else "当前范围已完成",
                    onClick = onStart,
                    enabled = exercise != null,
                    modifier = Modifier.fillMaxWidth(),
                    tone = JuicyLessonTone.Purple,
                    leadingIcon = Icons.Rounded.PlayArrow,
                )
                if (exercise == null && answered > 0) {
                    TextButton(onClick = onResetQueue, modifier = Modifier.fillMaxWidth()) { Text("重新练习当前范围", fontWeight = FontWeight.Bold) }
                }
            } else {
                Text("题库浏览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("在下方快速查看题目与解析，不影响训练队列。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LinguisticsFilterSheet(
    filters: ReadAirFilters,
    works: List<String>,
    episodes: List<Int>,
    domains: List<String>,
    questionTypes: List<String>,
    difficulties: List<String>,
    topics: List<String>,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int?) -> Unit,
    onDomainSelected: (String) -> Unit,
    onQuestionTypeSelected: (String) -> Unit,
    onDifficultySelected: (String) -> Unit,
    onTopicSelected: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("调整训练范围", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("筛选会立即更新题目数量", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onReset) { Text("重置", fontWeight = FontWeight.Bold) }
            }
            LinguisticsFilterChips("作品", works, filters.workSlug, ::workLabel, onWorkSelected)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("集数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "episode-all") {
                        ReadAirSelectChip(
                            text = "全部",
                            selected = filters.episode == null,
                            onClick = { onEpisodeSelected(null) },
                        )
                    }
                    items(episodes, key = { it }) { episode ->
                        ReadAirSelectChip(
                            text = "EP${episode.twoDigit()}",
                            selected = filters.episode == episode,
                            onClick = { onEpisodeSelected(episode) },
                        )
                    }
                }
            }
            LinguisticsFilterChips("学科领域", domains, filters.domain, ::domainLabel, onDomainSelected)
            LinguisticsFilterChips("题型", questionTypes, filters.questionType, ::questionTypeLabel, onQuestionTypeSelected)
            LinguisticsFilterChips("难度", difficulties, filters.difficulty, ::difficultyLabel, onDifficultySelected)
            LinguisticsFilterChips("专题", topics, filters.topic, ::topicLabel, onTopicSelected)
            JuicyLessonButton(
                text = "查看筛选结果",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                tone = JuicyLessonTone.Purple,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinguisticsFilterChips(
    title: String,
    options: List<String>,
    selected: String,
    label: (String) -> String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                ReadAirSelectChip(
                    text = label(option),
                    selected = selected == option,
                    onClick = { onSelected(option) },
                )
            }
        }
    }
}

/**
 * 统一的可点选筛选 chip：选中态与 TagChip 语义一致
 * （primaryContainer 容器 + 主色描边 + 打勾），未选中用层级背景。
 */
@Composable
private fun ReadAirSelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            )
        },
        modifier = modifier.heightIn(min = 40.dp),
        shape = CircleShape,
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

@Composable
private fun ReadAirEmptyNode(
    modifier: Modifier = Modifier,
    onResetFilters: (() -> Unit)? = null,
    onResetQueue: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Text(
                text = "当前范围没有可练题目",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "放宽筛选或重置进度，马上就能继续练习。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onResetQueue != null) {
                OutlinedButton(
                    onClick = onResetQueue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
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
                        .heightIn(min = 48.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("清空筛选", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
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
    OutlinedButton(
        onClick = { onSelected(mode) },
        modifier = modifier.semantics {
            this.selected = selected
            role = Role.Tab
        },
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline,
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
}

@Composable
fun ReadAirSessionScreen(
    uiState: LabUiState,
    onExit: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readAir = uiState.readAir
    val exercise = readAir.currentExercise
    val selectedAnswer = exercise?.let { readAir.selectedAnswerFor(it.id) }.orEmpty()
    var pendingAnswer by rememberSaveable(exercise?.id) { mutableStateOf("") }
    val displayedAnswer = selectedAnswer.ifBlank { pendingAnswer }
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
        LabTab.Library -> "返回资料页"
        LabTab.Today -> "返回今日页"
        LabTab.Review -> "返回错题页"
        else -> "返回题库入口"
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
    val feedbackEngine = LocalFeedbackEngine.current
    val readAirScrollState = rememberScrollState()
    val reducedMotion = rememberReducedMotion()
    val motionEnabled = uiState.settings.richAnimationsEnabled && !reducedMotion
    var lastFeedbackEventKey by rememberSaveable { mutableStateOf("") }

    BackHandler(onBack = onExit)

    LaunchedEffect(exercise?.id) {
        readAirScrollState.scrollTo(0)
    }
    LaunchedEffect(exercise?.id, selectedAnswer) {
        if (exercise != null && selectedAnswer.isNotBlank()) {
            val eventKey = "${exercise.id}:$selectedAnswer"
            if (lastFeedbackEventKey == eventKey) return@LaunchedEffect
            lastFeedbackEventKey = eventKey
            val correct = exercise.isCorrect(selectedAnswer)
            feedbackEngine?.emit(
                if (correct) FeedbackEvent.AnswerCorrect(xp = 8) else FeedbackEvent.AnswerWrong,
            )
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
            workSlug = readAir.filters.workSlug.takeUnless { it == ReadAirAllFilter }
                ?: uiState.selection.workSlug,
            episode = readAir.filters.episode ?: uiState.selection.episode,
            motionEnabled = motionEnabled,
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
            completed = answeredCount,
            remaining = (sessionTotal - answeredCount).coerceAtLeast(0),
            progress = progress,
            scopeTitle = scopeTitle,
            scopeDetail = scopeDetail,
            workSlug = exercise.workSlug,
            stableSeed = exercise.id.hashCode(),
            motionEnabled = motionEnabled,
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
            ExerciseCard(
                exercise = exercise,
                selectedAnswer = displayedAnswer,
                answerCommitted = selectedAnswer.isNotBlank(),
                motionEnabled = motionEnabled,
                onAnswerSelected = { pendingAnswer = it },
            )
        }
        if (selectedAnswer.isNotBlank()) {
            ReadAirSessionDock(
                exercise = exercise,
                selectedAnswer = selectedAnswer,
                correctXp = 8,
                isLastQuestion = answeredCount >= sessionTotal,
                onNext = onNext,
            )
        } else if (pendingAnswer.isNotBlank()) {
            ReadAirCheckDock(
                selectedAnswer = pendingAnswer,
                onCheck = { onAnswerSelected(pendingAnswer) },
            )
        }
    }
}

@Composable
private fun ReadAirSessionTopBar(
    current: Int,
    total: Int,
    completed: Int,
    remaining: Int,
    progress: Float,
    scopeTitle: String,
    scopeDetail: String,
    workSlug: String,
    stableSeed: Int,
    motionEnabled: Boolean,
    exitLabel: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = exitLabel,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "已完成 $completed / $total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = scopeTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "本次第 $current 题 · 剩余 $remaining 题",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CourseCharacterArtwork(
                    workSlug = workSlug,
                    role = CourseCharacterRole.Linguistics,
                    motionEnabled = motionEnabled,
                    stableSeed = stableSeed,
                    modifier = Modifier.size(58.dp),
                )
            }
            Text(
                text = "$scopeDetail · 返回即可调整范围，完成记录会自动续练",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            JuicyLessonProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                milestoneVisible = progress >= 1f,
                pulsing = progress >= 1f,
                progressColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

@Composable
private fun ReadAirComplete(
    completed: Int,
    correct: Int,
    xp: Int,
    energy: Int,
    scopeTitle: String,
    workSlug: String,
    episode: Int,
    motionEnabled: Boolean,
    exitLabel: String,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedbackEngine = LocalFeedbackEngine.current
    val accuracy = if (completed == 0) 0 else correct * 100 / completed
    var completionFeedbackEmitted by rememberSaveable(completed, correct, xp, scopeTitle) { mutableStateOf(false) }
    LaunchedEffect(completed) {
        if (completed > 0 && !completionFeedbackEmitted) {
            completionFeedbackEmitted = true
            delay(260)
            feedbackEngine?.emit(FeedbackEvent.LessonComplete)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            CourseCharacterArtwork(
                workSlug = workSlug,
                role = CourseCharacterRole.Celebration,
                motionEnabled = motionEnabled,
                stableSeed = episode,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = "单元完成!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = LabTheme.colors.xp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "你已经读完当前筛选队列。",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            val compactMetrics = maxWidth < 480.dp
            val metrics: @Composable (Modifier) -> Unit = { cardModifier ->
                RewardMetricCard("总经验", "+$xp", Icons.Rounded.Check, 1f, cardModifier)
                RewardMetricCard("稳扎稳打", "$accuracy%", Icons.Rounded.Psychology, accuracy / 100f, cardModifier)
                RewardMetricCard("能量", "$energy/5", Icons.Rounded.EmojiEvents, 1f, cardModifier, highlighted = true)
            }
            if (compactMetrics) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    metrics(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    metrics(Modifier.weight(1f))
                }
            }
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("本轮摘要", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val accuracyColor = when {
                        accuracy >= 80 -> LabTheme.colors.success
                        accuracy >= 50 -> LabTheme.colors.warning
                        else -> MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = "$accuracy%",
                        style = MaterialTheme.typography.displaySmall,
                        color = accuracyColor,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "本轮正确率",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "答对 $correct / 已答 $completed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "当前能量 $energy/5。本轮范围：$scopeTitle。",
                    style = MaterialTheme.typography.bodyMedium,
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
        JuicyLessonButton(
            text = "领取经验",
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            tone = JuicyLessonTone.Purple,
            trailingIcon = Icons.Rounded.EmojiEvents,
        )
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .heightIn(min = 52.dp),
        ) {
            Icon(Icons.Rounded.Replay, contentDescription = null)
            Text("再练这一组", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ReadAirSessionDock(
    exercise: LinguisticExercise,
    selectedAnswer: String,
    correctXp: Int,
    isLastQuestion: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val correct = exercise.isCorrect(selectedAnswer)
    val feedbackEngine = LocalFeedbackEngine.current
    val clipboard = LocalClipboardManager.current
    var copyStatus by rememberSaveable(exercise.id, selectedAnswer) { mutableStateOf(false) }
    val color = if (correct) LabTheme.colors.success else MaterialTheme.colorScheme.error
    val onColor = if (correct) LabTheme.colors.onSuccess else MaterialTheme.colorScheme.onError
    // 白色主按钮的面是主题无关的纯白，文字沿用组件自带的深色色板保证对比度。
    val darkColor = if (correct) JuicyLessonTone.Green.lip else JuicyLessonTone.Red.lip
    val answerExplanation = exercise.basicExplanationZh.ifBlank {
        if (correct) "这次读出了台词里的关系变化。" else "先记住正确语气，后面会在复习里再遇到。"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color,
        contentColor = onColor,
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
                        text = if (correct) "正确" else "不太对",
                        style = MaterialTheme.typography.titleLarge,
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
                    if (correct) {
                        Surface(
                            color = onColor,
                            contentColor = color,
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
            }
            if (!correct) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(buildExternalQuestionPrompt(exercise, selectedAnswer)))
                        copyStatus = true
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = onColor),
                    border = BorderStroke(1.dp, onColor.copy(alpha = 0.7f)),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                    Text(if (copyStatus) "已复制题目" else "复制题目", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
            }
            JuicyLessonButton(
                text = if (isLastQuestion) "完成" else "继续",
                onClick = {
                    if (correct) {
                        feedbackEngine?.emit(FeedbackEvent.LessonStepComplete)
                    }
                    onNext()
                },
                modifier = Modifier
                    .fillMaxWidth(),
                tone = JuicyLessonTone.White,
                contentColorOverride = darkColor,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}

@Composable
private fun ReadAirCheckDock(
    selectedAnswer: String,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "已选择：$selectedAnswer",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            JuicyLessonButton(
                text = "检查",
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth(),
                tone = JuicyLessonTone.Purple,
            )
        }
    }
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
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
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
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = "第 $index 题",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
            }
            TagChip(questionTypeLabel(exercise.questionType), selected = true)
        }
        DialogueBlock(exercise)
        Text(
            text = linguisticPromptForDisplay(exercise.prompt),
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
    val accentColor = if (correct) LabTheme.colors.success else MaterialTheme.colorScheme.error
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (correct) LabTheme.colors.successContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (correct) LabTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
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
            Text("你的选择：$selectedAnswer", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("正确答案：${exercise.correctOption}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            listOf(
                "基础说明" to exercise.basicExplanationZh,
                "深入解释" to exercise.deepExplanationZh,
                "动画语境" to exercise.animeContextNoteZh,
                "注意事项" to exercise.cautionNoteZh,
            ).forEach { (label, value) ->
                if (value.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (status == SyncStatus.Error) MaterialTheme.colorScheme.errorContainer else LabTheme.colors.infoContainer,
                contentColor = if (status == SyncStatus.Error) MaterialTheme.colorScheme.onErrorContainer else LabTheme.colors.onInfoContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    exercise: LinguisticExercise,
    selectedAnswer: String,
    answerCommitted: Boolean,
    motionEnabled: Boolean,
    onAnswerSelected: (String) -> Unit,
) {
    val answered = answerCommitted
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagChip(domainLabel(exercise.domain), selected = true)
                    TagChip(questionTypeLabel(exercise.questionType))
                    exercise.difficulty.takeIf { it.isNotBlank() }?.let { TagChip(it) }
                }
                Text(
                    text = linguisticPromptForDisplay(exercise.prompt),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
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
        AnimatedVisibility(
            visible = answered,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            AnswerReactionVisual(
                correct = exercise.isCorrect(selectedAnswer),
                workSlug = exercise.workSlug,
                episode = exercise.episode,
                motionEnabled = motionEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AnswerReactionVisual(
    correct: Boolean,
    workSlug: String,
    episode: Int,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (correct) LabTheme.colors.success else MaterialTheme.colorScheme.error
    val contentColor = if (correct) LabTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
    Surface(
        modifier = modifier,
        color = if (correct) LabTheme.colors.successContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (correct) {
                DuolingoLikeVisualHost(
                    asset = LearningAssetRegistry.answerCorrect,
                    motionEnabled = motionEnabled,
                    modifier = Modifier.size(76.dp),
                    fallback = {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = accentColor,
                        )
                    },
                )
            } else {
                CourseCharacterArtwork(
                    workSlug = workSlug,
                    role = CourseCharacterRole.Encouragement,
                    motionEnabled = motionEnabled,
                    stableSeed = episode,
                    modifier = Modifier.size(76.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = if (correct) "反应不错" else "这题需要再读一下",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                )
                Text(
                    text = if (correct) "继续保持这个语境判断。" else "先看正确语气，再进入下一题。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.85f),
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
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
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (line.isTarget) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (line.isTarget) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .border(1.dp, borderColor, MaterialTheme.shapes.medium)
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.85f),
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
    JuicyLessonChoiceCard(
        text = option,
        onClick = { if (!answered) onClick() },
        selected = selected && !answered,
        correct = correct,
        wrong = answered && selected && !correct,
        answered = answered,
        selectionColor = MaterialTheme.colorScheme.primary,
        selectionContainer = MaterialTheme.colorScheme.primaryContainer,
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

private val linguisticCuePrefixRegex = Regex(
    pattern = """^[（(]?\s*[【\[]\s*cue\b[^】\]]*[】\]](?:[^）)]*[）)])?\s*""",
    option = RegexOption.IGNORE_CASE,
)

internal fun linguisticPromptForDisplay(prompt: String): String {
    return prompt
        .trim()
        .replace(linguisticCuePrefixRegex, "")
        .trim()
}

private fun questionTypeLabel(questionType: String): String {
    return when (questionType) {
        ReadAirAllFilter -> "全部题型"
        "single_choice" -> "单选判断"
        "multiple_choice" -> "多选辨析"
        "kuuki_yomi" -> "语境判断"
        "syntax_relation" -> "句法关系"
        "morphology_analysis" -> "词形分析"
        "contrast_choice" -> "对比选择"
        "listening_reasoning" -> "听辨推理"
        else -> questionType
    }
}

private fun difficultyLabel(difficulty: String): String {
    return if (difficulty == ReadAirAllFilter) "全部难度" else difficulty
}

private fun topicLabel(topic: String): String {
    return when (topic) {
        ReadAirAllFilter -> "全部专题"
        ReadAirCognitiveTopic -> "认知语言学"
        else -> topic
    }
}

private fun readAirScopeTitle(filters: ReadAirFilters): String {
    val work = workLabel(filters.workSlug)
    val episode = filters.episode?.let { "EP${it.twoDigit()}" }
    val base = when {
        filters.workSlug == ReadAirAllFilter && episode == null -> "全库语言学训练"
        filters.workSlug == ReadAirAllFilter -> "全部作品 $episode"
        episode == null -> "$work 全集"
        else -> "$work $episode 专项"
    }
    val narrowFilters = listOfNotNull(
        filters.domain.takeUnless { it == ReadAirAllFilter }?.let(::domainLabel),
        filters.questionType.takeUnless { it == ReadAirAllFilter }?.let(::questionTypeLabel),
        filters.difficulty.takeUnless { it == ReadAirAllFilter }?.let(::difficultyLabel),
        filters.topic.takeUnless { it == ReadAirAllFilter }?.let(::topicLabel),
    )
    return (listOf(base) + narrowFilters).joinToString(" · ")
}

private fun readAirScopeDetail(filters: ReadAirFilters, scoped: Int, total: Int): String {
    val parts = mutableListOf<String>()
    parts += if (filters.workSlug == ReadAirAllFilter) "全部作品" else workLabel(filters.workSlug)
    parts += filters.episode?.let { "EP${it.twoDigit()}" } ?: "全部集数"
    if (filters.domain != ReadAirAllFilter) parts += domainLabel(filters.domain)
    if (filters.questionType != ReadAirAllFilter) parts += questionTypeLabel(filters.questionType)
    if (filters.difficulty != ReadAirAllFilter) parts += difficultyLabel(filters.difficulty)
    if (filters.topic != ReadAirAllFilter) parts += topicLabel(filters.topic)
    if (scoped == total) {
        parts += "全库 $total 题"
    } else {
        parts += "当前范围 $scoped 题"
        parts += "全库 $total 题"
    }
    return parts.joinToString(" · ")
}

private fun Int.twoDigit(): String = toString().padStart(2, '0')
