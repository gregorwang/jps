package com.animejapaneselab.nativeapp.ui.foundation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.data.FoundationDomain
import com.animejapaneselab.nativeapp.data.FoundationQuestion
import com.animejapaneselab.nativeapp.data.FoundationQuestionOption
import com.animejapaneselab.nativeapp.data.FoundationQuestionPack
import com.animejapaneselab.nativeapp.data.FoundationQuestionType
import com.animejapaneselab.nativeapp.data.FoundationStage
import com.animejapaneselab.nativeapp.data.FoundationStimulus
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing
import com.animejapaneselab.nativeapp.ui.theme.LabTheme

private enum class FoundationViewMode {
    Training,
    Catalog,
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FoundationTrainingScreen(
    state: FoundationTrainingState,
    onRefresh: () -> Unit,
    onPackSelected: (String) -> Unit,
    onDomainSelected: (FoundationDomain?) -> Unit,
    onTopicSelected: (String?) -> Unit,
    onStageSelected: (FoundationStage?) -> Unit,
    onAnswerSelected: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var modeName by rememberSaveable { mutableStateOf(FoundationViewMode.Training.name) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var selectedTopicId by rememberSaveable { mutableStateOf<String?>(null) }
    var catalogQuery by rememberSaveable { mutableStateOf("") }
    var catalogDomainValue by rememberSaveable { mutableStateOf<String?>(null) }

    val mode = FoundationViewMode.entries.firstOrNull { it.name == modeName }
        ?: FoundationViewMode.Training
    val catalogDomain = FoundationDomain.entries.firstOrNull {
        it.wireValue == catalogDomainValue
    }
    val catalogTopics = remember(state.topics, catalogQuery, catalogDomain) {
        val normalizedQuery = catalogQuery.trim().lowercase()
        state.topics.asSequence()
            .filter { catalogDomain == null || it.domain == catalogDomain }
            .filter { topic ->
                normalizedQuery.isEmpty() ||
                    topic.titleZh.lowercase().contains(normalizedQuery) ||
                    topic.titleJa.lowercase().contains(normalizedQuery) ||
                    topic.shortDefinitionZh.lowercase().contains(normalizedQuery) ||
                    topic.tags.any { it.lowercase().contains(normalizedQuery) }
            }
            .sortedWith(compareBy(FoundationTopic::domain, FoundationTopic::sortOrder, FoundationTopic::id))
            .toList()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = LabSpacing.Screen,
            vertical = LabSpacing.Medium,
        ),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
    ) {
        item(contentType = "foundation-hero") {
            FoundationHero(
                state = state,
                onRefresh = onRefresh,
            )
        }
        item(contentType = "foundation-mode") {
            FoundationModeSwitcher(
                selected = mode,
                onSelected = { modeName = it.name },
            )
        }

        if (mode == FoundationViewMode.Training) {
            if (state.packs.isNotEmpty()) {
                item(contentType = "foundation-scope") {
                    FoundationScopeCard(
                        state = state,
                        onOpenFilters = { showFilters = true },
                    )
                }
            }
            when (state.phase) {
                FoundationTrainingPhase.Idle,
                FoundationTrainingPhase.LoadingCatalog,
                FoundationTrainingPhase.LoadingQuestions -> item(contentType = "foundation-loading") {
                    FoundationLoadingState(state.phase)
                }

                FoundationTrainingPhase.Error -> item(contentType = "foundation-error") {
                    FoundationErrorState(
                        error = state.error,
                        onRetry = onRefresh,
                    )
                }

                FoundationTrainingPhase.Ready -> when {
                    state.isComplete -> item(contentType = "foundation-complete") {
                        FoundationCompleteState(
                            answeredCount = state.answeredCount,
                            canGoPrevious = state.currentIndex > 0,
                            onPrevious = onPrevious,
                            onRestart = onRestart,
                        )
                    }

                    state.currentQuestion == null -> item(contentType = "foundation-empty") {
                        FoundationEmptyState()
                    }

                    else -> {
                        val question = requireNotNull(state.currentQuestion)
                        item(
                            key = question.id,
                            contentType = "foundation-question",
                        ) {
                            FoundationQuestionCard(
                                question = question,
                                topic = state.topics.firstOrNull { it.id == question.topicId },
                                selectedOptionId = state.currentSelectedOptionId,
                                position = state.currentIndex + 1,
                                total = state.filteredQuestions.size,
                                canGoPrevious = state.currentIndex > 0,
                                onAnswerSelected = onAnswerSelected,
                                onPrevious = onPrevious,
                                onNext = onNext,
                            )
                        }
                    }
                }
            }
        } else {
            when {
                state.topics.isNotEmpty() -> {
                    item(contentType = "foundation-catalog-search") {
                        FoundationCatalogControls(
                            query = catalogQuery,
                            selectedDomain = catalogDomain,
                            resultCount = catalogTopics.size,
                            onQueryChanged = { catalogQuery = it },
                            onDomainSelected = { catalogDomainValue = it?.wireValue },
                        )
                    }
                    FoundationDomain.entries.forEach { domain ->
                        val topicsInDomain = catalogTopics.filter { it.domain == domain }
                        if (topicsInDomain.isNotEmpty()) {
                            item(
                                key = "foundation-domain-${domain.wireValue}",
                                contentType = "foundation-domain-heading",
                            ) {
                                FoundationDomainHeading(
                                    domain = domain,
                                    count = topicsInDomain.size,
                                )
                            }
                            items(
                                items = topicsInDomain,
                                key = FoundationTopic::id,
                                contentType = { "foundation-topic-card" },
                            ) { topic ->
                                FoundationTopicCard(
                                    topic = topic,
                                    onClick = { selectedTopicId = topic.id },
                                )
                            }
                        }
                    }
                    if (catalogTopics.isEmpty()) {
                        item(contentType = "foundation-catalog-empty") {
                            FoundationCatalogEmptyState()
                        }
                    }
                }

                state.phase == FoundationTrainingPhase.Error -> item(contentType = "foundation-error") {
                    FoundationErrorState(
                        error = state.error,
                        onRetry = onRefresh,
                    )
                }

                else -> item(contentType = "foundation-loading") {
                    FoundationLoadingState(state.phase)
                }
            }
        }
    }

    if (showFilters && state.packs.isNotEmpty()) {
        FoundationFiltersSheet(
            state = state,
            onPackSelected = onPackSelected,
            onDomainSelected = onDomainSelected,
            onTopicSelected = onTopicSelected,
            onStageSelected = onStageSelected,
            onReset = {
                onDomainSelected(null)
                onTopicSelected(null)
                onStageSelected(null)
            },
            onDismiss = { showFilters = false },
        )
    }

    val selectedTopic = selectedTopicId?.let { topicId ->
        state.topics.firstOrNull { it.id == topicId }
    }
    if (selectedTopic != null) {
        val canPractice = state.questions.any { it.topicId == selectedTopic.id }
        FoundationTopicDetailSheet(
            topic = selectedTopic,
            allTopics = state.topics,
            onPractice = if (canPractice) {
                {
                    selectedTopicId = null
                    modeName = FoundationViewMode.Training.name
                    onDomainSelected(selectedTopic.domain)
                    onTopicSelected(selectedTopic.id)
                }
            } else {
                null
            },
            onDismiss = { selectedTopicId = null },
        )
    }
}

@Composable
private fun FoundationHero(
    state: FoundationTrainingState,
    onRefresh: () -> Unit,
) {
    val selectedPack = state.packs.firstOrNull { it.id == state.filters.packId }
    val totalQuestions = state.packs.sumOf(FoundationQuestionPack::questionCount)
    val scopedTotal = state.filteredQuestions.size
    val progress = if (scopedTotal == 0) 0f else state.answeredCount.toFloat() / scopedTotal
    val onHero = LabTheme.colors.onHero

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        contentColor = onHero,
    ) {
        Column(
            modifier = Modifier
                .background(LabTheme.heroBrush())
                .padding(LabSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall),
                ) {
                    Text(
                        text = stringResource(R.string.foundation_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.foundation_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHero.copy(alpha = 0.86f),
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = onHero.copy(alpha = 0.16f),
                    contentColor = onHero,
                ) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = state.phase != FoundationTrainingPhase.LoadingCatalog,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.foundation_refresh),
                        )
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.foundation_hero_stats,
                    state.topics.size,
                    totalQuestions,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            selectedPack?.let {
                Text(
                    text = it.titleZh,
                    style = MaterialTheme.typography.labelLarge,
                    color = onHero.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = onHero,
                trackColor = onHero.copy(alpha = 0.24f),
                strokeCap = StrokeCap.Round,
                gapSize = (-1).dp,
                drawStopIndicator = {},
            )
            Text(
                text = stringResource(
                    R.string.foundation_hero_progress,
                    state.answeredCount,
                    scopedTotal,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = onHero.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun FoundationModeSwitcher(
    selected: FoundationViewMode,
    onSelected: (FoundationViewMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(LabSpacing.XXSmall),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall),
        ) {
            FoundationModeButton(
                label = stringResource(R.string.foundation_mode_training),
                icon = Icons.Rounded.Psychology,
                selected = selected == FoundationViewMode.Training,
                onClick = { onSelected(FoundationViewMode.Training) },
                modifier = Modifier.weight(1f),
            )
            FoundationModeButton(
                label = stringResource(R.string.foundation_mode_catalog),
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                selected = selected == FoundationViewMode.Catalog,
                onClick = { onSelected(FoundationViewMode.Catalog) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FoundationModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LabSpacing.Small, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Text(
                text = label,
                modifier = Modifier.padding(start = LabSpacing.XSmall),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoundationScopeCard(
    state: FoundationTrainingState,
    onOpenFilters: () -> Unit,
) {
    val selectedPack = state.packs.firstOrNull { it.id == state.filters.packId }
    val selectedTopic = state.topics.firstOrNull { it.id == state.filters.topicId }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(LabSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            ) {
                Text(
                    text = stringResource(R.string.foundation_current_scope),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = selectedPack?.titleZh.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                selectedPack?.let { pack ->
                    Text(
                        text = stringResource(
                            R.string.foundation_pack_summary,
                            pack.topicCount,
                            pack.questionCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FoundationScopeChip(
                        stringResource(
                            R.string.foundation_active_scope_count,
                            state.filteredQuestions.size,
                        ),
                    )
                    state.filters.domain?.let { FoundationScopeChip(it.localizedLabel()) }
                    selectedTopic?.let { FoundationScopeChip(it.titleZh) }
                    state.filters.stage?.let { FoundationScopeChip(it.localizedLabel()) }
                }
            }
            OutlinedButton(
                onClick = onOpenFilters,
                modifier = Modifier.heightIn(min = 48.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.foundation_adjust_filters),
                    modifier = Modifier.padding(start = 5.dp),
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun FoundationScopeChip(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoundationFiltersSheet(
    state: FoundationTrainingState,
    onPackSelected: (String) -> Unit,
    onDomainSelected: (FoundationDomain?) -> Unit,
    onTopicSelected: (String?) -> Unit,
    onStageSelected: (FoundationStage?) -> Unit,
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
                .padding(start = LabSpacing.Screen, end = LabSpacing.Screen, bottom = LabSpacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.foundation_filters_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(
                            R.string.foundation_active_scope_count,
                            state.filteredQuestions.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onReset) {
                    Text(
                        text = stringResource(R.string.foundation_reset_filters),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            FoundationPackFilter(
                packs = state.packOptions,
                selectedPackId = state.filters.packId,
                onSelected = onPackSelected,
            )
            FoundationNullableFilter(
                title = stringResource(R.string.foundation_filter_domain),
                options = state.domainOptions,
                selected = state.filters.domain,
                key = FoundationDomain::wireValue,
                label = { it.localizedLabel() },
                onSelected = onDomainSelected,
            )
            FoundationNullableFilter(
                title = stringResource(R.string.foundation_filter_topic),
                options = state.topicOptions,
                selected = state.filters.topicId?.let { selectedId ->
                    state.topicOptions.firstOrNull { it.id == selectedId }
                },
                key = FoundationTopic::id,
                label = { it.titleZh },
                onSelected = { onTopicSelected(it?.id) },
            )
            FoundationNullableFilter(
                title = stringResource(R.string.foundation_filter_stage),
                options = state.stageOptions,
                selected = state.filters.stage,
                key = FoundationStage::wireValue,
                label = { it.localizedLabel() },
                onSelected = onStageSelected,
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(
                    text = stringResource(R.string.foundation_close_filters),
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun FoundationPackFilter(
    packs: List<FoundationQuestionPack>,
    selectedPackId: String?,
    onSelected: (String) -> Unit,
) {
    FoundationFilterTitle(stringResource(R.string.foundation_filter_pack))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall)) {
        items(
            items = packs,
            key = FoundationQuestionPack::id,
            contentType = { "foundation-pack-filter" },
        ) { pack ->
            FoundationFilterChip(
                label = pack.titleZh,
                selected = pack.id == selectedPackId,
                onClick = { onSelected(pack.id) },
            )
        }
    }
}

@Composable
private fun <T> FoundationNullableFilter(
    title: String,
    options: List<T>,
    selected: T?,
    key: (T) -> String,
    label: @Composable (T) -> String,
    onSelected: (T?) -> Unit,
) {
    if (options.isEmpty()) return
    FoundationFilterTitle(title)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall)) {
        item(key = "$title-all", contentType = "foundation-filter-all") {
            FoundationFilterChip(
                label = stringResource(R.string.foundation_filter_all),
                selected = selected == null,
                onClick = { onSelected(null) },
            )
        }
        items(
            items = options,
            key = key,
            contentType = { "foundation-filter-option" },
        ) { option ->
            FoundationFilterChip(
                label = label(option),
                selected = option == selected,
                onClick = { onSelected(option) },
            )
        }
    }
}

@Composable
private fun FoundationFilterTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun FoundationFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val selectedDescription = stringResource(R.string.foundation_state_selected)
    val unselectedDescription = stringResource(R.string.foundation_state_not_selected)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
            }
        } else {
            null
        },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                stateDescription = if (selected) selectedDescription else unselectedDescription
            },
    )
}

@Composable
private fun FoundationLoadingState(phase: FoundationTrainingPhase) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(LabSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
            Text(
                text = if (phase == FoundationTrainingPhase.LoadingQuestions) {
                    stringResource(R.string.foundation_loading_questions)
                } else {
                    stringResource(R.string.foundation_loading_catalog)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun FoundationErrorState(
    error: FoundationTrainingError?,
    onRetry: () -> Unit,
) {
    val message = when (error) {
        FoundationTrainingError.MalformedData -> stringResource(R.string.foundation_error_malformed)
        FoundationTrainingError.EmptyCatalog -> stringResource(R.string.foundation_error_empty_catalog)
        FoundationTrainingError.Network,
        null -> stringResource(R.string.foundation_error_network)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(LabSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(30.dp))
            Text(
                text = stringResource(R.string.foundation_error_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.foundation_retry), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoundationQuestionCard(
    question: FoundationQuestion,
    topic: FoundationTopic?,
    selectedOptionId: String?,
    position: Int,
    total: Int,
    canGoPrevious: Boolean,
    onAnswerSelected: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var hintVisible by rememberSaveable(question.id) { mutableStateOf(false) }
    val progress = if (total == 0) 0f else position.toFloat() / total
    val selectedCorrect = selectedOptionId?.let(question::isCorrect)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(LabSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.foundation_question_progress, position, total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = StrokeCap.Round,
                gapSize = (-1).dp,
                drawStopIndicator = {},
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FoundationQuestionBadge(question.stage.localizedLabel())
                FoundationQuestionBadge(question.questionType.localizedLabel())
                topic?.let { FoundationQuestionBadge(it.domain.localizedLabel()) }
            }
            if (topic != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(LabSpacing.Medium),
                        verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall),
                    ) {
                        Text(
                            text = "${topic.titleZh} · ${topic.titleJa}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = topic.shortDefinitionZh,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            FoundationStimulusCard(stimulus = question.stimulus)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = question.promptZh,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
            TextButton(
                onClick = { hintVisible = !hintVisible },
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = LabSpacing.XSmall),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = stringResource(
                        if (hintVisible) R.string.foundation_hide_hint else R.string.foundation_reveal_hint,
                    ),
                    modifier = Modifier.padding(start = 6.dp),
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = if (hintVisible) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(18.dp),
                )
            }
            AnimatedVisibility(visible = hintVisible) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = LabTheme.colors.infoContainer,
                    contentColor = LabTheme.colors.onInfoContainer,
                ) {
                    Text(
                        text = stringResource(R.string.foundation_hint, question.hintZh),
                        modifier = Modifier.padding(LabSpacing.Medium),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.Small)) {
                question.options.forEach { option ->
                    FoundationAnswerOption(
                        option = option,
                        correctOptionId = question.answerOptionId,
                        selectedOptionId = selectedOptionId,
                        onSelected = { onAnswerSelected(option.id) },
                    )
                }
            }
            if (selectedOptionId != null && selectedCorrect != null) {
                FoundationAnswerFeedback(
                    question = question,
                    selectedOptionId = selectedOptionId,
                    correct = selectedCorrect,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = canGoPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.foundation_previous),
                        modifier = Modifier.padding(start = 5.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = onNext,
                    enabled = selectedOptionId != null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (position == total) R.string.foundation_finish else R.string.foundation_next,
                        ),
                        fontWeight = FontWeight.Black,
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FoundationQuestionBadge(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun FoundationStimulusCard(stimulus: FoundationStimulus) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(LabSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
        ) {
            when (stimulus) {
                is FoundationStimulus.Sentence -> {
                    Text(
                        text = stimulus.jaText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    stimulus.zhContext?.let {
                        FoundationStimulusContext(it)
                    }
                }

                is FoundationStimulus.Dialogue -> {
                    stimulus.turns.forEach { turn ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = buildString {
                                    turn.speaker?.let {
                                        append(it)
                                        append("：")
                                    }
                                    append(turn.jaText)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            turn.zhText?.let { FoundationStimulusContext(it) }
                        }
                    }
                    stimulus.zhContext?.let { FoundationStimulusContext(it) }
                }

                is FoundationStimulus.Contrast -> {
                    stimulus.items.forEach { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                modifier = Modifier.padding(LabSpacing.Small),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = buildString {
                                        item.label?.let {
                                            append(it)
                                            append("：")
                                        }
                                        append(item.text)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                item.noteZh?.let { FoundationStimulusContext(it) }
                            }
                        }
                    }
                    stimulus.zhContext?.let { FoundationStimulusContext(it) }
                }

                is FoundationStimulus.Metalinguistic -> {
                    stimulus.form?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = stimulus.descriptionZh,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun FoundationStimulusContext(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FoundationAnswerOption(
    option: FoundationQuestionOption,
    correctOptionId: String,
    selectedOptionId: String?,
    onSelected: () -> Unit,
) {
    val answered = selectedOptionId != null
    val selected = option.id == selectedOptionId
    val correctAnswer = option.id == correctOptionId
    val stateLabel = when {
        !answered -> stringResource(R.string.foundation_answer_available)
        selected && correctAnswer -> stringResource(R.string.foundation_answer_correct)
        selected -> stringResource(R.string.foundation_answer_incorrect)
        correctAnswer -> stringResource(R.string.foundation_answer_expected)
        else -> stringResource(R.string.foundation_state_not_selected)
    }
    val containerColor = when {
        answered && correctAnswer -> LabTheme.colors.successContainer
        answered && selected -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        answered && correctAnswer -> LabTheme.colors.onSuccessContainer
        answered && selected -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        answered && correctAnswer -> LabTheme.colors.success
        answered && selected -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(
                selected = selected,
                enabled = !answered,
                role = Role.RadioButton,
                onClick = onSelected,
            )
            .semantics {
                this.selected = selected
                stateDescription = stateLabel
            },
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (selected || correctAnswer && answered) 2.dp else 1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(LabSpacing.Small),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = when {
                    answered && correctAnswer -> LabTheme.colors.success
                    answered && selected -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = when {
                    answered && (correctAnswer || selected) -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = option.id,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Text(
                text = option.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected || correctAnswer && answered) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
            )
            when {
                answered && correctAnswer -> Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = stateLabel,
                    tint = LabTheme.colors.success,
                )

                answered && selected -> Icon(
                    Icons.Rounded.Close,
                    contentDescription = stateLabel,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FoundationAnswerFeedback(
    question: FoundationQuestion,
    selectedOptionId: String,
    correct: Boolean,
) {
    var expanded by rememberSaveable(question.id) { mutableStateOf(false) }
    val containerColor = if (correct) {
        LabTheme.colors.successContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (correct) {
        LabTheme.colors.onSuccessContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(LabSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                )
                Text(
                    text = if (correct) {
                        stringResource(R.string.foundation_feedback_correct)
                    } else {
                        stringResource(R.string.foundation_feedback_incorrect)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (!correct) {
                question.wrongExplanations[selectedOptionId]?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            FeedbackSection(
                title = stringResource(R.string.foundation_feedback_explanation),
                body = question.explanationZh,
            )
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
            ) {
                Text(
                    text = stringResource(
                        if (expanded) R.string.foundation_feedback_less else R.string.foundation_feedback_more,
                    ),
                    fontWeight = FontWeight.Black,
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.Small)) {
                    FeedbackSection(
                        title = stringResource(R.string.foundation_feedback_deep),
                        body = question.deepExplanationZh,
                    )
                    FeedbackSection(
                        title = stringResource(R.string.foundation_feedback_caution),
                        body = question.cautionNoteZh,
                    )
                    if (
                        question.transferExampleJa != null &&
                        question.transferExplanationZh != null
                    ) {
                        FeedbackSection(
                            title = stringResource(R.string.foundation_feedback_transfer),
                            body = "${question.transferExampleJa}\n${question.transferExplanationZh}",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun FoundationCompleteState(
    answeredCount: Int,
    canGoPrevious: Boolean,
    onPrevious: () -> Unit,
    onRestart: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = LabTheme.colors.successContainer,
        contentColor = LabTheme.colors.onSuccessContainer,
    ) {
        Column(
            modifier = Modifier.padding(LabSpacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.foundation_complete_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.foundation_complete_body, answeredCount),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = canGoPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.foundation_previous), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.foundation_restart), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun FoundationEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            text = stringResource(R.string.foundation_empty_questions),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(LabSpacing.Large),
        )
    }
}

@Composable
private fun FoundationCatalogControls(
    query: String,
    selectedDomain: FoundationDomain?,
    resultCount: Int,
    onQueryChanged: (String) -> Unit,
    onDomainSelected: (FoundationDomain?) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(LabSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.foundation_mode_catalog),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.foundation_catalog_result_count, resultCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.foundation_catalog_search_label)) },
                placeholder = {
                    Text(stringResource(R.string.foundation_catalog_search_placeholder))
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(
                                    R.string.foundation_catalog_clear_search,
                                ),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall)) {
                item(key = "catalog-all") {
                    FoundationFilterChip(
                        label = stringResource(R.string.foundation_filter_all),
                        selected = selectedDomain == null,
                        onClick = { onDomainSelected(null) },
                    )
                }
                items(
                    items = FoundationDomain.entries,
                    key = FoundationDomain::wireValue,
                ) { domain ->
                    FoundationFilterChip(
                        label = domain.localizedLabel(),
                        selected = selectedDomain == domain,
                        onClick = { onDomainSelected(domain) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FoundationDomainHeading(
    domain: FoundationDomain,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LabSpacing.XSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (container, content) = domain.tone()
            Surface(
                shape = CircleShape,
                color = container,
                contentColor = content,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Text(
                text = domain.localizedLabel(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            text = stringResource(R.string.foundation_catalog_result_count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoundationTopicCard(
    topic: FoundationTopic,
    onClick: () -> Unit,
) {
    val (container, content) = topic.domain.tone()
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 118.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(LabSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = container,
                contentColor = content,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (topic.sortOrder + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = topic.titleZh,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = topic.titleJa,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = topic.shortDefinitionZh,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FoundationCatalogEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(LabSpacing.XLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.foundation_catalog_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FoundationTopicDetailSheet(
    topic: FoundationTopic,
    allTopics: List<FoundationTopic>,
    onPractice: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val prerequisiteNames = topic.prerequisiteTopicIds.map { prerequisiteId ->
        allTopics.firstOrNull { it.id == prerequisiteId }?.titleZh ?: prerequisiteId
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = LabSpacing.Screen, end = LabSpacing.Screen, bottom = LabSpacing.XXLarge),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        ) {
            val (domainContainer, domainContent) = topic.domain.tone()
            Surface(
                shape = CircleShape,
                color = domainContainer,
                contentColor = domainContent,
            ) {
                Text(
                    text = topic.domain.localizedLabel(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall)) {
                Text(
                    text = topic.titleZh,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = topic.titleJa,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = topic.shortDefinitionZh,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FoundationTopicExplanationCard(
                title = stringResource(R.string.foundation_topic_beginner),
                body = topic.beginnerExplanationZh,
                containerColor = LabTheme.colors.infoContainer,
                contentColor = LabTheme.colors.onInfoContainer,
            )
            FoundationTopicExplanationCard(
                title = stringResource(R.string.foundation_topic_deep),
                body = topic.deepExplanationZh,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            FoundationTopicExplanationCard(
                title = stringResource(R.string.foundation_topic_caution),
                body = topic.cautionNoteZh,
                containerColor = LabTheme.colors.warningContainer,
                contentColor = LabTheme.colors.onWarningContainer,
            )
            Text(
                text = stringResource(R.string.foundation_topic_objectives),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
            FoundationObjectiveRow(
                stage = FoundationStage.F1,
                body = topic.learningObjectives.f1Zh,
            )
            FoundationObjectiveRow(
                stage = FoundationStage.F2,
                body = topic.learningObjectives.f2Zh,
            )
            FoundationObjectiveRow(
                stage = FoundationStage.F3,
                body = topic.learningObjectives.f3Zh,
            )
            FoundationObjectiveRow(
                stage = FoundationStage.F4,
                body = topic.learningObjectives.f4Zh,
            )
            FoundationDetailSection(
                title = stringResource(R.string.foundation_topic_examples),
            ) {
                FoundationLabeledBody(
                    stringResource(R.string.foundation_topic_example_form),
                    topic.exampleSpec.formZh,
                )
                FoundationLabeledBody(
                    stringResource(R.string.foundation_topic_example_contrast),
                    topic.exampleSpec.contrastZh,
                )
                FoundationLabeledBody(
                    stringResource(R.string.foundation_topic_example_constraints),
                    topic.exampleSpec.constraintsZh,
                )
            }
            FoundationDetailSection(
                title = stringResource(R.string.foundation_topic_prerequisites),
            ) {
                Text(
                    text = prerequisiteNames.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                        ?: stringResource(R.string.foundation_topic_no_prerequisites),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (topic.tags.isNotEmpty()) {
                FoundationDetailSection(
                    title = stringResource(R.string.foundation_topic_tags),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        topic.tags.forEach { FoundationScopeChip(it) }
                    }
                }
            }
            onPractice?.let {
                Button(
                    onClick = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                ) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null)
                    Text(
                        text = stringResource(R.string.foundation_topic_practice),
                        modifier = Modifier.padding(start = LabSpacing.XSmall),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun FoundationTopicExplanationCard(
    title: String,
    body: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(LabSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FoundationObjectiveRow(
    stage: FoundationStage,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(LabSpacing.Small),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    text = stage.wireValue,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stage.localizedLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FoundationDetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun FoundationLabeledBody(
    label: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
        )
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FoundationDomain.tone(): Pair<Color, Color> {
    return when (this) {
        FoundationDomain.PhonologyWriting -> LabTheme.colors.infoContainer to LabTheme.colors.onInfoContainer
        FoundationDomain.Morphology -> LabTheme.colors.successContainer to LabTheme.colors.onSuccessContainer
        FoundationDomain.Syntax -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        FoundationDomain.Semantics -> LabTheme.colors.warningContainer to LabTheme.colors.onWarningContainer
        FoundationDomain.PragmaticsDiscourse ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        FoundationDomain.Sociolinguistics ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        FoundationDomain.HistoricalGrammaticalization ->
            MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun FoundationDomain.localizedLabel(): String {
    return stringResource(
        when (this) {
            FoundationDomain.PhonologyWriting -> R.string.foundation_domain_phonology_writing
            FoundationDomain.Morphology -> R.string.foundation_domain_morphology
            FoundationDomain.Syntax -> R.string.foundation_domain_syntax
            FoundationDomain.Semantics -> R.string.foundation_domain_semantics
            FoundationDomain.PragmaticsDiscourse -> R.string.foundation_domain_pragmatics
            FoundationDomain.Sociolinguistics -> R.string.foundation_domain_sociolinguistics
            FoundationDomain.HistoricalGrammaticalization -> R.string.foundation_domain_historical
        },
    )
}

@Composable
private fun FoundationStage.localizedLabel(): String {
    return stringResource(
        when (this) {
            FoundationStage.F1 -> R.string.foundation_stage_f1
            FoundationStage.F2 -> R.string.foundation_stage_f2
            FoundationStage.F3 -> R.string.foundation_stage_f3
            FoundationStage.F4 -> R.string.foundation_stage_f4
        },
    )
}

@Composable
private fun FoundationQuestionType.localizedLabel(): String {
    return stringResource(
        when (this) {
            FoundationQuestionType.SingleChoice -> R.string.foundation_question_type_single_choice
            FoundationQuestionType.MorphologyAnalysis -> R.string.foundation_question_type_morphology
            FoundationQuestionType.SyntaxRelation -> R.string.foundation_question_type_syntax
            FoundationQuestionType.ContrastChoice -> R.string.foundation_question_type_contrast
            FoundationQuestionType.KuukiYomi -> R.string.foundation_question_type_kuuki
        },
    )
}
