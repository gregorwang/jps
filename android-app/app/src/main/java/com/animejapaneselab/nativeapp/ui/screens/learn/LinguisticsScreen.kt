package com.animejapaneselab.nativeapp.ui.screens.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.FoundationDomain
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.ReadAirAllFilter
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.TextbookCover
import com.animejapaneselab.nativeapp.ui.design.VolumeSwitch
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingPhase
import com.animejapaneselab.nativeapp.ui.foundation.LinguisticsTrack
import com.animejapaneselab.nativeapp.ui.screens.FoundationActions
import com.animejapaneselab.nativeapp.ui.screens.ReadAirHomeActions
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillRules
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.drill.DrillPhase

/**
 * 言語学: 第一巻 アニメの台詞 / 第二巻 基礎 ([VolumeSwitch]), 教科書を選ぶ (up to four covers; a
 * cover picks the domain), one ink button that opens the set. Other filters live in the sheet
 * behind the header's 筛选 icon ([filtersOpen]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinguisticsScreen(
    uiState: LabUiState,
    onTrackSelected: (LinguisticsTrack) -> Unit,
    readAir: ReadAirHomeActions,
    foundation: FoundationActions,
    onOpenFoundation: () -> Unit,
    drill: ConjugationDrillState,
    drillActions: DrillVolumeActions,
    filtersOpen: Boolean,
    onFiltersDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = uiState.linguisticsTrack
    val volume = when (track) {
        LinguisticsTrack.AnimeCorpus -> readAirVolume(uiState, readAir)
        LinguisticsTrack.Foundation -> foundationVolume(uiState, foundation, onOpenFoundation)
        LinguisticsTrack.Conjugation -> drillVolume(drill, drillActions)
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            VolumeSwitch(
                options = listOf("第一巻 台詞", "第二巻 基礎", "第三巻 活用"),
                selectedIndex = track.ordinal,
                onSelect = { onTrackSelected(LinguisticsTrack.entries[it]) },
            )
            Spacer(Modifier.height(18.dp))
            SectionHeading(title = "教科書を選ぶ", meta = volume.stats)
            Spacer(Modifier.height(14.dp))
            when {
                volume.loading -> Box(Modifier.fillMaxWidth().height(212.dp), contentAlignment = Alignment.Center) { LoadingDots() }
                volume.error != null -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(volume.error, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
                    OutlineButton("重新加载", volume.onRefresh)
                }
                volume.books.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("ここには、まだ教科書がない", style = AjlTheme.type.jpTitle, color = AjlTheme.colors.ink2)
                    QuietButton("清除筛选", volume.onResetFilters)
                }
                else -> BookGrid(volume.books, volume.onBook)
            }
            Spacer(Modifier.height(20.dp))
        }
        if (volume.startCount > 0 && !volume.loading && volume.error == null) {
            InkButton(
                text = volume.startLabel,
                onClick = volume.onStart,
                trailingArrow = true,
                height = 56.dp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
        }
    }

    if (filtersOpen) {
        AjlBottomSheet(onDismissRequest = onFiltersDismiss, title = "絞り込み", gloss = "筛选") {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                volume.filterGroups.forEach { group -> FilterGroupView(group) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    QuietButton("清除筛选", { volume.onResetFilters(); onFiltersDismiss() })
                }
            }
        }
    }
}

@Composable
private fun BookGrid(books: List<TextbookSpec>, onBook: (TextbookSpec) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        books.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { book ->
                    TextbookCover(
                        volume = book.volumeLabel,
                        title = book.title,
                        sampleLine = book.sampleLine,
                        gloss = book.gloss,
                        progress = book.progress,
                        current = book.current,
                        onClick = { onBook(book) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroupView(group: FilterGroup) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(group.title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.options.forEach { option ->
                FilterPill(text = option.label, selected = option.selected, onClick = option.onClick)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Volume adapters (UI state → what the page draws)
// ---------------------------------------------------------------------------

internal data class DrillVolumeActions(
    val onGroup: (String?) -> Unit,
    val onPoint: (String?) -> Unit,
    val onReset: () -> Unit,
    val onRefresh: () -> Unit,
    val onStart: () -> Unit,
)

/** 第三巻 活用: one 教科書 per grammar group; progress = lines at box ≥ [ConjugationDrillRules.MasteredBox]. */
private fun drillVolume(state: ConjugationDrillState, actions: DrillVolumeActions): VolumeUi {
    val books = state.groups.mapIndexed { index, group ->
        val inGroup = state.items.filter { it.group == group }
        TextbookSpec(
            key = group,
            volumeLabel = "VOL.${ConjugationDrillRules.groupKey(group)}",
            title = ConjugationDrillRules.groupTitle(group),
            sampleLine = inGroup.firstOrNull()?.target.orEmpty(),
            gloss = "${inGroup.map { it.pointId }.distinct().size} 课 · ${inGroup.size} 句",
            total = inGroup.size,
            answered = ConjugationDrillRules.masteredCount(inGroup, state.progress),
            current = group == state.group,
        )
    }
    val due = ConjugationDrillRules.dueCount(state.scoped, state.progress, state.today)
    val count = state.nextSessionSize
    val groups = buildList {
        add(
            FilterGroup(
                "语法点",
                listOf(FilterOption("全部", state.pointId == null) { actions.onPoint(null) }) +
                    state.points.map { (id, title) -> FilterOption(title, id == state.pointId) { actions.onPoint(id) } },
            ),
        )
    }
    return VolumeUi(
        books = books,
        stats = "${state.items.size} 句 · 复习 $due · 掌握 ${ConjugationDrillRules.masteredCount(state.items, state.progress)}",
        loading = state.phase == DrillPhase.Idle || state.phase == DrillPhase.Loading,
        error = if (state.phase == DrillPhase.Error) "连不上题库服务" else null,
        startLabel = if (due > 0) "活用 · 复习 $due · 共 $count 句" else "活用 · $count 句",
        startCount = count,
        onStart = actions.onStart,
        onBook = { actions.onGroup(it.key) },
        onRefresh = actions.onRefresh,
        onResetFilters = actions.onReset,
        filterGroups = groups,
    )
}

private data class FilterOption(val label: String, val selected: Boolean, val onClick: () -> Unit)

private data class FilterGroup(val title: String, val options: List<FilterOption>)

private data class VolumeUi(
    val books: List<TextbookSpec>,
    val stats: String,
    val loading: Boolean,
    val error: String?,
    val startLabel: String,
    val startCount: Int,
    val onStart: () -> Unit,
    val onBook: (TextbookSpec) -> Unit,
    val onRefresh: () -> Unit,
    val onResetFilters: () -> Unit,
    val filterGroups: List<FilterGroup>,
)

@Composable
private fun readAirVolume(uiState: LabUiState, actions: ReadAirHomeActions): VolumeUi {
    val state = uiState.readAir
    val books = remember(state.exercises, state.filters, state.selectedAnswers, state.currentIndex) {
        LinguisticsModel.readAirBooks(
            exercises = state.exercises,
            domainCounts = state.domainCounts,
            selectedDomain = state.filters.domain,
            upNextDomain = state.currentExercise?.domain,
            isAnswered = { state.selectedAnswers[it].orEmpty().isNotBlank() },
        )
    }
    val scoped = state.scopedExercises.size
    val remaining = state.remainingScopedCount
    val count = if (remaining > 0) remaining else scoped
    val filters = state.filters
    fun <T> group(title: String, options: List<T>, selected: T, label: (T) -> String, on: (T) -> Unit) =
        FilterGroup(title, options.map { FilterOption(label(it), it == selected) { on(it) } })
    val groups = buildList {
        add(group("作品", state.workOptions, filters.workSlug, LinguisticsModel::workLabel, actions.onWorkSelected))
        add(group("领域", state.domainOptions, filters.domain, LinguisticsModel::readAirDomainLabel, actions.onDomainSelected))
        add(group("题型", state.questionTypeOptions, filters.questionType, LinguisticsModel::questionTypeLabel, actions.onQuestionTypeSelected))
        add(group("难度", state.difficultyOptions, filters.difficulty, LinguisticsModel::difficultyLabel, actions.onDifficultySelected))
        if (state.topicOptions.size > 1) {
            add(group("专题", state.topicOptions, filters.topic, LinguisticsModel::topicLabel, actions.onTopicSelected))
        }
        if (state.episodeOptions.isNotEmpty()) {
            add(
                group(
                    "集数",
                    listOf<Int?>(null) + state.episodeOptions,
                    filters.episode,
                    { ep -> ep?.let { "第${it}話" } ?: "全部" },
                    actions.onEpisodeSelected,
                ),
            )
        }
    }
    return VolumeUi(
        books = books,
        stats = LinguisticsModel.statsLine(books, state.exercises.size),
        loading = state.exercises.isEmpty() && (state.status == SyncStatus.Loading || state.status == SyncStatus.Idle),
        error = if (state.exercises.isEmpty() && state.status == SyncStatus.Error) state.message.ifBlank { "连不上题库服务" } else null,
        startLabel = "读空气 · $count 问",
        startCount = count,
        onStart = actions.onStartSession,
        onBook = { book ->
            actions.onDomainSelected(if (book.key == filters.domain) ReadAirAllFilter else book.key)
        },
        onRefresh = actions.onRefresh,
        onResetFilters = actions.onResetFilters,
        filterGroups = groups,
    )
}

@Composable
private fun foundationVolume(
    uiState: LabUiState,
    actions: FoundationActions,
    onOpen: () -> Unit,
): VolumeUi {
    val state = uiState.foundation
    val books = remember(state.questions, state.topics, state.filters, state.selectedAnswers, state.currentIndex) {
        LinguisticsModel.foundationBooks(
            questions = state.questions,
            topics = state.topics,
            selectedDomain = state.filters.domain,
            upNextDomain = state.currentQuestion?.let { q -> state.topics.firstOrNull { it.id == q.topicId }?.domain },
            answeredIds = state.selectedAnswers.keys,
        )
    }
    val remaining = (state.filteredQuestions.size - state.answeredCount).coerceAtLeast(0)
    val count = if (remaining > 0) remaining else state.filteredQuestions.size
    val filters = state.filters
    val groups = buildList {
        if (state.packOptions.size > 1) {
            add(
                FilterGroup(
                    "题包",
                    state.packOptions.map { pack ->
                        FilterOption(pack.titleZh.ifBlank { "第${pack.batchNo}包" }, pack.id == filters.packId) { actions.onPackSelected(pack.id) }
                    },
                ),
            )
        }
        add(
            FilterGroup(
                "领域",
                listOf(FilterOption("全部", filters.domain == null) { actions.onDomainSelected(null) }) +
                    state.domainOptions.map { d: FoundationDomain ->
                        FilterOption(LinguisticsModel.foundationDomainLabel(d), d == filters.domain) { actions.onDomainSelected(d) }
                    },
            ),
        )
        if (state.topicOptions.isNotEmpty()) {
            add(
                FilterGroup(
                    "知识点",
                    listOf(FilterOption("全部", filters.topicId == null) { actions.onTopicSelected(null) }) +
                        state.topicOptions.map { t ->
                            FilterOption(t.titleZh.ifBlank { t.titleJa }, t.id == filters.topicId) { actions.onTopicSelected(t.id) }
                        },
                ),
            )
        }
        add(
            FilterGroup(
                "阶段",
                listOf(FilterOption("全部", filters.stage == null) { actions.onStageSelected(null) }) +
                    state.stageOptions.map { s ->
                        FilterOption(LinguisticsModel.stageLabel(s), s == filters.stage) { actions.onStageSelected(s) }
                    },
            ),
        )
    }
    val loading = state.phase == FoundationTrainingPhase.Idle ||
        state.phase == FoundationTrainingPhase.LoadingCatalog ||
        state.phase == FoundationTrainingPhase.LoadingQuestions
    return VolumeUi(
        books = books,
        stats = LinguisticsModel.statsLine(books, state.questions.size),
        loading = loading,
        error = if (state.phase == FoundationTrainingPhase.Error) "连不上题库服务" else null,
        startLabel = "基礎 · $count 问",
        startCount = count,
        onStart = onOpen,
        onBook = { book ->
            val domain = FoundationDomain.valueOf(book.key)
            actions.onDomainSelected(if (domain == filters.domain) null else domain)
        },
        onRefresh = actions.onRefresh,
        onResetFilters = {
            actions.onDomainSelected(null)
            actions.onTopicSelected(null)
            actions.onStageSelected(null)
        },
        filterGroups = groups,
    )
}
