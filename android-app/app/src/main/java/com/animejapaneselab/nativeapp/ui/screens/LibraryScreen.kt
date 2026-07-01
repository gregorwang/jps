package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.EpisodeOption
import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonTarget
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.data.WorkOption
import com.animejapaneselab.nativeapp.data.promptAudioForSentence
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackState
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.StructuredAiResultCard
import com.animejapaneselab.nativeapp.ui.components.TagChip

@Composable
fun LibraryScreen(
    uiState: LabUiState,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode) -> Unit,
    onStartReadAir: () -> Unit,
    onTargetLesson: (LessonTarget) -> Unit,
    onAskAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var revealContentRequest by rememberSaveable { mutableIntStateOf(0) }
    var revealEpisodeActionsRequest by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    fun selectTab(tab: Int, revealContent: Boolean = true) {
        selectedTab = tab
        if (revealContent) {
            revealContentRequest += 1
        }
    }
    LaunchedEffect(revealContentRequest, selectedTab, uiState.selection.workSlug, uiState.selection.episode) {
        if (revealContentRequest > 0 && libraryTabHasContent(selectedTab, uiState)) {
            withFrameNanos { }
            withFrameNanos { }
            listState.animateScrollToItem(1)
        }
    }
    val onHeaderEpisodeSelected: (Int) -> Unit = { episode ->
        revealEpisodeActionsRequest += 1
        onEpisodeSelected(episode)
    }
    val revealEpisodeActionsTotalRequest =
        revealEpisodeActionsRequest + uiState.libraryRevealEpisodeActionsRequest
    val tabs = listOf(
        LibraryTabSpec("词汇", uiState.vocab.size, "个词"),
        LibraryTabSpec("语法", uiState.grammar.size, "个点"),
        LibraryTabSpec("跟读", uiState.shadowing.size, "句"),
    )
    val audioController = rememberLessonAudioController()
    val header: @Composable () -> Unit = {
        LibraryGuideHeader(
            uiState = uiState,
            selectedTab = selectedTab,
            tabs = tabs,
            onTabSelected = { selectTab(it) },
            onWorkSelected = onWorkSelected,
            onEpisodeSelected = onHeaderEpisodeSelected,
            onStartLesson = onStartLesson,
            onStartModeLesson = onStartModeLesson,
            onStartReadAir = onStartReadAir,
            revealEpisodeActionsRequest = revealEpisodeActionsTotalRequest,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    when (selectedTab) {
        0 -> VocabList(
            vocab = uiState.vocab,
            playbackState = audioController.playbackState,
            onSpeak = { text -> audioController.speakText(text, uiState.settings.ttsWorkerUrl) },
            onTargetLesson = onTargetLesson,
            uiState = uiState,
            onAskAi = onAskAi,
            header = header,
            listState = listState,
            modifier = modifier,
        )
        1 -> GrammarList(
            grammar = uiState.grammar,
            uiState = uiState,
            onTargetLesson = onTargetLesson,
            onAskAi = onAskAi,
            header = header,
            listState = listState,
            modifier = modifier,
        )
        else -> ShadowingList(
            uiState = uiState,
            playbackState = audioController.playbackState,
            onPlay = { cue -> audioController.play(cue, uiState.settings.ttsWorkerUrl) },
            onTargetLesson = onTargetLesson,
            onAskAi = onAskAi,
            header = header,
            listState = listState,
            modifier = modifier,
        )
    }
}

private data class LibraryTabSpec(
    val label: String,
    val count: Int,
    val eyebrow: String,
)

private fun libraryTabHasContent(selectedTab: Int, uiState: LabUiState): Boolean {
    return when (selectedTab) {
        0 -> uiState.vocab.isNotEmpty()
        1 -> uiState.grammar.isNotEmpty()
        else -> uiState.shadowing.isNotEmpty()
    }
}

@Composable
private fun WorkTabs(
    works: List<WorkOption>,
    selectedWorkSlug: String,
    onWorkSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(works, key = { it.slug }) { work ->
            val selected = work.slug == selectedWorkSlug
            Surface(
                modifier = Modifier.minimumInteractiveComponentSize(),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
                tonalElevation = if (selected) 3.dp else 1.dp,
                onClick = { onWorkSelected(work.slug) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AutoStories, contentDescription = null)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(work.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                        Text("${work.episodeCount} 集", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeBrowser(
    episodes: List<EpisodeOption>,
    selectedEpisode: Int,
    onEpisodeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = episodes.chunked(12)
    val selectedEpisodePage = pages.indexOfFirst { page -> page.any { it.episode == selectedEpisode } }
        .coerceAtLeast(0)
    var selectedPage by rememberSaveable(episodes.firstOrNull()?.workSlug ?: "episodes") {
        mutableIntStateOf(selectedEpisodePage)
    }
    val safePage = selectedPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val currentPage = pages.getOrElse(safePage) { emptyList() }

    LaunchedEffect(selectedEpisode, episodes.size) {
        if (selectedEpisodePage >= 0) {
            selectedPage = selectedEpisodePage
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pages.forEachIndexed { index, page ->
                val start = page.firstOrNull()?.episode ?: return@forEachIndexed
                val end = page.lastOrNull()?.episode ?: start
                val selected = index == safePage
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                    onClick = { selectedPage = index },
                ) {
                    Text(
                        text = "查看 EP${start.toString().padStart(2, '0')}-${end.toString().padStart(2, '0')}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            currentPage.forEach { episode ->
                EpisodeTile(
                    episode = episode,
                    selected = episode.episode == selectedEpisode,
                    onClick = { onEpisodeSelected(episode.episode) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun EpisodeBrowserSection(
    episodes: List<EpisodeOption>,
    selectedEpisode: Int,
    onEpisodeSelected: (Int) -> Unit,
    tabs: List<LibraryTabSpec>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    readAirCount: Int,
    subtitleCount: Int,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode) -> Unit,
    onStartReadAir: () -> Unit,
    revealRequest: Int,
    modifier: Modifier = Modifier,
) {
    val selected = episodes.firstOrNull { it.episode == selectedEpisode }
    var expanded by rememberSaveable(episodes.firstOrNull()?.workSlug ?: "episodes") {
        mutableStateOf(true)
    }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(revealRequest) {
        if (revealRequest > 0) {
            withFrameNanos { }
            withFrameNanos { }
            bringIntoViewRequester.bringIntoView()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("选集", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        selected?.let { "${it.label} · ${it.usableJaLines} 句 · ${it.chunkCount} chunks" } ?: "选择一集后进入本集训练",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                    )
                    Text(
                        if (expanded) "收起" else "展开",
                        modifier = Modifier.padding(start = 4.dp),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Text("本集入口", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryEntryButton(
                    label = "综合训练",
                    meta = "混合队列",
                    icon = Icons.Rounded.PlayCircle,
                    selected = true,
                    onClick = onStartLesson,
                )
                LibraryEntryButton(
                    label = "读空气专项",
                    meta = "${readAirCount} 道题",
                    icon = Icons.Rounded.Bolt,
                    selected = false,
                    onClick = onStartReadAir,
                )
                LibraryEntryButton(
                    label = "词汇专项",
                    meta = "${tabs.getOrNull(0)?.count ?: 0} 个词",
                    icon = Icons.Rounded.AutoStories,
                    selected = selectedTab == 0,
                    onClick = { onStartModeLesson(LessonMode.Vocab) },
                )
                LibraryEntryButton(
                    label = "语法专项",
                    meta = "${tabs.getOrNull(1)?.count ?: 0} 个点",
                    icon = Icons.Rounded.School,
                    selected = selectedTab == 1,
                    onClick = { onStartModeLesson(LessonMode.Grammar) },
                )
                LibraryEntryButton(
                    label = "跟读专项",
                    meta = "${tabs.getOrNull(2)?.count ?: 0} 句",
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    selected = selectedTab == 2,
                    onClick = { onStartModeLesson(LessonMode.Shadowing) },
                )
                LibraryEntryButton(
                    label = "台词浏览",
                    meta = "${subtitleCount} 行字幕",
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    selected = false,
                    onClick = { onTabSelected(2) },
                )
            }
            if (expanded) {
                EpisodeBrowser(
                    episodes = episodes,
                    selectedEpisode = selectedEpisode,
                    onEpisodeSelected = onEpisodeSelected,
                )
            }
        }
    }
}

@Composable
private fun EpisodeTile(
    episode: EpisodeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(98.dp)
            .heightIn(min = 68.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        tonalElevation = if (selected) 4.dp else 1.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(episode.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Text(
                "${episode.usableJaLines} 句 · ${episode.chunkCount}c",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
@Composable
private fun LibraryEntryButton(
    label: String,
    meta: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(142.dp)
            .heightIn(min = 70.dp)
            .minimumInteractiveComponentSize(),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (selected) 3.dp else 0.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, maxLines = 1)
            Text(meta, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun LibraryGuideHeader(
    uiState: LabUiState,
    selectedTab: Int,
    tabs: List<LibraryTabSpec>,
    onTabSelected: (Int) -> Unit,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode) -> Unit,
    onStartReadAir: () -> Unit,
    revealEpisodeActionsRequest: Int,
    modifier: Modifier = Modifier,
) {
    val selectedEpisode = uiState.episodes.firstOrNull { it.episode == uiState.selection.episode }
    val selectedWork = uiState.works.firstOrNull { it.slug == uiState.selection.workSlug }
    val selectedReadAirCount = uiState.readAir.exercises.count { exercise ->
        exercise.episode == uiState.selection.episode && libraryWorkMatches(exercise.workSlug, uiState.selection.workSlug)
    }
    val workReadAirCount = uiState.readAir.exercises.count { exercise ->
        libraryWorkMatches(exercise.workSlug, uiState.selection.workSlug)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WorkTabs(
            works = uiState.works,
            selectedWorkSlug = uiState.selection.workSlug,
            onWorkSelected = onWorkSelected,
        )
        WorkLibraryOverview(
            work = selectedWork,
            episodes = uiState.episodes,
            readAirCount = workReadAirCount,
        )
        EpisodeBrowserSection(
            episodes = uiState.episodes,
            selectedEpisode = uiState.selection.episode,
            onEpisodeSelected = onEpisodeSelected,
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            readAirCount = selectedReadAirCount,
            subtitleCount = selectedEpisode?.totalCues ?: 0,
            onStartLesson = onStartLesson,
            onStartModeLesson = onStartModeLesson,
            onStartReadAir = onStartReadAir,
            revealRequest = revealEpisodeActionsRequest,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("学习手册", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(uiState.focus.episodeLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text(uiState.focus.sectionTitle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    text = uiState.focus.guidebook,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        GuideMetric(label = "句子", value = selectedEpisode?.usableJaLines?.toString() ?: "-", modifier = Modifier.weight(1f))
                        GuideMetric(label = "chunks", value = selectedEpisode?.chunkCount?.toString() ?: "-", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        GuideMetric(label = "字幕", value = selectedEpisode?.totalCues?.toString() ?: "-", modifier = Modifier.weight(1f))
                        GuideMetric(label = "语言学", value = selectedReadAirCount.toString(), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                LibraryModeButton(
                    tab = tab,
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WorkLibraryOverview(
    work: WorkOption?,
    episodes: List<EpisodeOption>,
    readAirCount: Int,
    modifier: Modifier = Modifier,
) {
    val importedEpisodes = episodes.size
    val totalSentences = episodes.sumOf { it.usableJaLines }
    val totalChunks = episodes.sumOf { it.chunkCount }
    val totalCues = episodes.sumOf { it.totalCues }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("作品总览", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text(
                        work?.displayName ?: "当前作品",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TagChip("已导入 $importedEpisodes/${work?.episodeCount ?: importedEpisodes} 集", selected = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                WorkOverviewMetric(label = "句子", value = totalSentences.toString(), modifier = Modifier.weight(1f))
                WorkOverviewMetric(label = "chunks", value = totalChunks.toString(), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                WorkOverviewMetric(label = "字幕", value = totalCues.toString(), modifier = Modifier.weight(1f))
                WorkOverviewMetric(label = "读空气", value = readAirCount.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WorkOverviewMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

private fun libraryWorkMatches(exerciseWorkSlug: String, selectedWorkSlug: String): Boolean {
    return normalizeLibraryWorkSlug(exerciseWorkSlug) == normalizeLibraryWorkSlug(selectedWorkSlug)
}

private fun normalizeLibraryWorkSlug(workSlug: String): String {
    return when (workSlug) {
        "rezero" -> "re-zero"
        else -> workSlug
    }
}

@Composable
private fun GuideMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.onSecondary,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LibraryModeButton(
    tab: LibraryTabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (tab.label) {
        "词汇" -> Icons.Rounded.AutoStories
        "语法" -> Icons.Rounded.School
        else -> Icons.Rounded.Bolt
    }
    Surface(
        modifier = modifier,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        tonalElevation = if (selected) 4.dp else 1.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(tab.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text("${tab.count}${tab.eyebrow}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VocabList(
    vocab: List<VocabItem>,
    playbackState: AudioPlaybackState,
    onSpeak: (String) -> Unit,
    onTargetLesson: (LessonTarget) -> Unit,
    uiState: LabUiState,
    onAskAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
    header: @Composable () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "guide-header") {
            header()
        }
        item(key = "library-content-anchor") {
            Spacer(modifier = Modifier.heightIn(min = 1.dp))
        }
        if (playbackState.message.isNotBlank()) {
            item(key = "audio-status") {
                AudioStatusBanner(playbackState = playbackState)
            }
        }
        items(vocab, key = { it.id }) { item ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.surface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("${item.reading} · ${item.romanization}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.meaningZh, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                    TagChip(item.level, selected = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TagChip(item.partOfSpeech)
                    item.toneTags.take(2).forEach { TagChip(it) }
                }
                Text(item.occurrence, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = { onTargetLesson(LessonTarget.Vocab(item.id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("练这个词", fontWeight = FontWeight.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onSpeak(item.surface) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                        Text("播放", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(
                        onClick = {
                            onAskAi(
                                item.libraryAiKey("vocab"),
                                "vocab",
                                item.surface,
                                item.aiContext(uiState.focus.episodeLabel),
                            )
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Text("精讲", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                    }
                }
                }
            }
                LibraryAiPanel(targetKey = item.libraryAiKey("vocab"), uiState = uiState)
            }
        }
    }
}

@Composable
private fun GrammarList(
    grammar: List<GrammarPoint>,
    uiState: LabUiState,
    onTargetLesson: (LessonTarget) -> Unit,
    onAskAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
    header: @Composable () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "guide-header") {
            header()
        }
        item(key = "library-content-anchor") {
            Spacer(modifier = Modifier.heightIn(min = 1.dp))
        }
        items(grammar, key = { it.id }) { item ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TagChip(item.pattern, selected = true)
                        Text(item.titleZh, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                    Text(item.exampleJa, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(item.exampleZh, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(item.explanationZh, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = { onTargetLesson(LessonTarget.Grammar(item.id)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    ) {
                        Text("练这个语法", fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(
                        onClick = {
                            onAskAi(
                                item.libraryAiKey("grammar"),
                                "grammar",
                                item.pattern,
                                item.aiContext(uiState.focus.episodeLabel),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Text("精讲", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                    }
                }
                LibraryAiPanel(targetKey = item.libraryAiKey("grammar"), uiState = uiState)
            }
        }
    }
}

@Composable
private fun ShadowingList(
    uiState: LabUiState,
    playbackState: AudioPlaybackState,
    onPlay: (PromptAudio) -> Unit,
    onTargetLesson: (LessonTarget) -> Unit,
    onAskAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
    header: @Composable () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "guide-header") {
            header()
        }
        item(key = "library-content-anchor") {
            Spacer(modifier = Modifier.heightIn(min = 1.dp))
        }
        if (playbackState.message.isNotBlank()) {
            item(key = "audio-status") {
                AudioStatusBanner(playbackState = playbackState)
            }
        }
        items(uiState.shadowing, key = { it.id }) { item ->
            val audio = promptAudioForSentence(uiState.selection.workSlug, item, autoPlay = false)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        TagChip(audio.labelForChip(), selected = audio is PromptAudio.Source)
                        Text(item.sourceLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        item.ja,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(item.reading, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.meaningZh, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    ShadowingActions(
                        audio = audio,
                        sentence = item,
                        episodeLabel = uiState.focus.episodeLabel,
                        onPlay = onPlay,
                        onTargetLesson = onTargetLesson,
                        onAskAi = onAskAi,
                    )
                }
                LibraryAiPanel(targetKey = item.libraryAiKey("sentence"), uiState = uiState)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShadowingActions(
    audio: PromptAudio,
    sentence: ShadowingSentence,
    episodeLabel: String,
    onPlay: (PromptAudio) -> Unit,
    onTargetLesson: (LessonTarget) -> Unit,
    onAskAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { onTargetLesson(LessonTarget.Sentence(sentence.id)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text("练这句", fontWeight = FontWeight.Black)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onPlay(audio) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                Text(audio.label, modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
            }
            val fallbackAudio = (audio as? PromptAudio.Source)
                ?.takeIf { it.fallbackTtsText.isNotBlank() }
            if (fallbackAudio != null) {
                OutlinedButton(
                    onClick = {
                        onPlay(PromptAudio.Tts(fallbackAudio.fallbackTtsText, autoPlay = false, label = fallbackAudio.fallbackLabel))
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(fallbackAudio.fallbackLabel, fontWeight = FontWeight.Black)
                }
            }
        }
        OutlinedButton(
            onClick = {
                onAskAi(
                    sentence.libraryAiKey("sentence"),
                    "sentence",
                    sentence.ja,
                    sentence.aiContext(episodeLabel),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
            Text("精讲", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LibraryAiPanel(
    targetKey: String,
    uiState: LabUiState,
) {
    if (uiState.libraryAiTargetKey != targetKey) return
    when (uiState.aiCoach.status) {
        SyncStatus.Loading -> LabCard {
            Text("正在智能精讲...", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
        SyncStatus.Success, SyncStatus.Error -> StructuredAiResultCard(
            result = uiState.aiCoach.result,
            fallbackText = uiState.aiCoach.answer,
        )
        else -> Unit
    }
}

@Composable
private fun AudioStatusBanner(playbackState: AudioPlaybackState) {
    LabCard {
        Text(
            text = playbackState.message,
            style = MaterialTheme.typography.labelLarge,
            color = if (playbackState.phase == AudioPlaybackPhase.Error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun PromptAudio.labelForChip(): String {
    return when (this) {
        PromptAudio.None -> "No audio"
        is PromptAudio.Tts -> "标准语音"
        is PromptAudio.Source -> if (reliability == AudioReliability.Verified) "Source" else "Flagged"
    }
}

private fun VocabItem.libraryAiKey(kind: String) = "$kind:$id"

private fun GrammarPoint.libraryAiKey(kind: String) = "$kind:$id"

private fun ShadowingSentence.libraryAiKey(kind: String) = "$kind:$id"

private fun VocabItem.aiContext(episodeLabel: String): String {
    return buildString {
        append("资料页 AI 精讲。章节：")
        append(episodeLabel)
        append("\n词：")
        append(surface)
        append("\n读音：")
        append(reading)
        append("\n罗马音：")
        append(romanization)
        append("\n中文：")
        append(meaningZh)
        append("\n词性：")
        append(partOfSpeech)
        append("\n难度：")
        append(level)
        append("\n出现：")
        append(occurrence)
        append("\n请解释核心意思、语气、现实可用性、常见误解，并给出一个短记忆点。")
    }
}

private fun GrammarPoint.aiContext(episodeLabel: String): String {
    return buildString {
        append("资料页 AI 精讲。章节：")
        append(episodeLabel)
        append("\n语法：")
        append(pattern)
        append("\n标题：")
        append(titleZh)
        append("\n日文例句：")
        append(exampleJa)
        append("\n中文：")
        append(exampleZh)
        append("\n说明：")
        append(explanationZh)
        append("\n语气：")
        append(pragmaticsNote)
        append("\n现实使用：")
        append(realWorldNote)
        append("\n请解释这句里的用法、口语语气、相近表达差异，并给出训练提示。")
    }
}

private fun ShadowingSentence.aiContext(episodeLabel: String): String {
    return buildString {
        append("资料页 AI 精讲。章节：")
        append(episodeLabel)
        append("\n日文句子：")
        append(ja)
        append("\n读音：")
        append(reading)
        append("\n中文：")
        append(meaningZh)
        append("\n来源：")
        append(sourceLabel)
        append("\n请解释字面意思、句子结构、语气、跟读重点和现实可用性。")
    }
}
