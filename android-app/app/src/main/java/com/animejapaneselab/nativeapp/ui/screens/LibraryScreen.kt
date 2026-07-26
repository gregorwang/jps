package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.EpisodeOption
import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonTarget
import com.animejapaneselab.nativeapp.data.LinguisticCardPayload
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
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.StructuredAiResultCard
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.PressablePrimaryButton
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.CharacterCatalog
import com.animejapaneselab.nativeapp.ui.reading.CharacterProfileSheet
import com.animejapaneselab.nativeapp.ui.reading.DeepDiveTarget
import com.animejapaneselab.nativeapp.ui.reading.RubyText
import com.animejapaneselab.nativeapp.ui.reading.SentenceDeepDiveSheet
import com.animejapaneselab.nativeapp.ui.reading.rememberCharacterProfile
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.reading.rememberSentenceDeepDive
import com.animejapaneselab.nativeapp.ui.theme.LabTheme

@Composable
fun LibraryScreen(
    uiState: LabUiState,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode) -> Unit,
    onStartReadAir: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSettings: () -> Unit,
    onTargetLesson: (LessonTarget) -> Unit,
    onAskAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
    onOpenSearch: () -> Unit = {},
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
    val characterProfile = rememberCharacterProfile(uiState.settings, uiState.selection.workSlug)
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
            onOpenSubtitles = onOpenSubtitles,
            onOpenSettings = onOpenSettings,
            onOpenSearch = onOpenSearch,
            onOpenCharacterProfile = characterProfile::open,
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
    CharacterProfileSheet(characterProfile)
}

private data class LibraryTabSpec(
    val label: String,
    val count: Int,
    val eyebrow: String,
)

private const val VocabPageSize = 40

/** 「全部」筛选键：不做等级过滤。 */
private const val VocabLevelAll = "ALL"

/** 「其他」筛选键：收纳 "N?" / "N/A" / 空串等无法识别的杂值。 */
private const val VocabLevelOther = "OTHER"

/** JLPT chips 的展示顺序：由易到难。 */
private val JlptLevelKeys = listOf("N5", "N4", "N3", "N2", "N1")

/**
 * 把 [VocabItem.level] 归一化成筛选键：trim + uppercase 后精确匹配 N1..N5，
 * 其余（"N?"、"N/A"、空串、杂值）一律归入「其他」。
 */
private fun normalizeJlptLevel(rawLevel: String): String {
    val normalized = rawLevel.trim().uppercase()
    return if (normalized in JlptLevelKeys) normalized else VocabLevelOther
}

private data class VocabLevelBucket(
    val key: String,
    val label: String,
    val count: Int,
)

/**
 * 统计当前集各等级词数。首项恒为「全部」，随后按 N5→N1 排列，「其他」垫底；
 * 词数为 0 的等级不产出 chip。
 */
private fun buildVocabLevelBuckets(vocab: List<VocabItem>): List<VocabLevelBucket> {
    if (vocab.isEmpty()) return emptyList()
    val counts = mutableMapOf<String, Int>()
    vocab.forEach { item ->
        val key = normalizeJlptLevel(item.level)
        counts[key] = (counts[key] ?: 0) + 1
    }
    val buckets = mutableListOf(VocabLevelBucket(VocabLevelAll, "全部", vocab.size))
    JlptLevelKeys.forEach { key ->
        val count = counts[key] ?: 0
        if (count > 0) {
            buckets += VocabLevelBucket(key, key, count)
        }
    }
    val otherCount = counts[VocabLevelOther] ?: 0
    if (otherCount > 0) {
        buckets += VocabLevelBucket(VocabLevelOther, "其他", otherCount)
    }
    return buckets
}

private fun libraryTabHasContent(selectedTab: Int, uiState: LabUiState): Boolean {
    return when (selectedTab) {
        0 -> uiState.vocab.isNotEmpty()
        1 -> uiState.grammar.isNotEmpty()
        else -> uiState.shadowing.isNotEmpty()
    }
}

private data class LibrarySelectionColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

@Composable
private fun animateLibrarySelectionColors(
    selected: Boolean,
    unselectedContainer: Color = MaterialTheme.colorScheme.surface,
): LibrarySelectionColors {
    val reducedMotion = rememberReducedMotion()
    val spec = tween<Color>(
        durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
        easing = MotionTokens.Curve.Standard,
    )
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else unselectedContainer,
        animationSpec = spec,
        label = "library-selection-container",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = spec,
        label = "library-selection-content",
    )
    val border by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = spec,
        label = "library-selection-border",
    )
    return LibrarySelectionColors(container = container, content = content, border = border)
}

@Composable
private fun LibraryEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        items(works, key = { it.slug }, contentType = { "work-tab" }) { work ->
            val selected = work.slug == selectedWorkSlug
            val colors = animateLibrarySelectionColors(selected = selected)
            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .semantics {
                        this.selected = selected
                        role = Role.Tab
                    },
                color = colors.container,
                contentColor = colors.content,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, colors.border),
                tonalElevation = 0.dp,
                onClick = { onWorkSelected(work.slug) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(work.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                        Text(
                            "${work.episodeCount} 集",
                            style = MaterialTheme.typography.labelSmall,
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

    val reducedMotion = rememberReducedMotion()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                    easing = MotionTokens.Curve.Standard,
                ),
            ),
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
                val colors = animateLibrarySelectionColors(selected = selected)
                Surface(
                    color = colors.container,
                    contentColor = colors.content,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, colors.border),
                    onClick = { selectedPage = index },
                ) {
                    Text(
                        text = "EP${start.toString().padStart(2, '0')}-${end.toString().padStart(2, '0')}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeBrowserSection(
    episodes: List<EpisodeOption>,
    selectedEpisode: Int,
    onEpisodeSelected: (Int) -> Unit,
    revealRequest: Int,
    modifier: Modifier = Modifier,
) {
    val selected = episodes.firstOrNull { it.episode == selectedEpisode }
    var expanded by rememberSaveable(episodes.firstOrNull()?.workSlug ?: "episodes") {
        mutableStateOf(false)
    }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val reducedMotion = rememberReducedMotion()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MotionTokens.microSpec(reducedMotion),
        label = "episode-browser-chevron",
    )

    LaunchedEffect(revealRequest) {
        if (revealRequest > 0) {
            expanded = true
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
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = selected?.label ?: "选择一集",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        selected?.let { "${it.totalCues} 行台词 · ${it.chunkCount} 个片段" } ?: "切换当前学习内容",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(chevronRotation),
                    )
                    Text(
                        if (expanded) "收起" else "换一集",
                        modifier = Modifier.padding(start = 4.dp),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                        easing = MotionTokens.Curve.Decelerate,
                    ),
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                        easing = MotionTokens.Curve.Standard,
                    ),
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                        easing = MotionTokens.Curve.Standard,
                    ),
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                        easing = MotionTokens.Curve.Standard,
                    ),
                ),
            ) {
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
    val colors = animateLibrarySelectionColors(selected = selected)
    Surface(
        modifier = modifier
            .width(98.dp)
            .heightIn(min = 68.dp)
            .semantics { this.selected = selected },
        color = colors.container,
        contentColor = colors.content,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, colors.border),
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(episode.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Text(
                "${episode.totalCues} 行台词 · ${episode.chunkCount} 个片段",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibraryQuickAccessPanel(
    episodeLabel: String,
    subtitleCount: Int,
    readAirCount: Int,
    tabs: List<LibraryTabSpec>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode) -> Unit,
    onStartReadAir: () -> Unit,
    onOpenSubtitles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                tabs.forEachIndexed { index, tab ->
                    LibraryModeButton(
                        tab = tab,
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            PressablePrimaryButton(
                onClick = {
                    onStartModeLesson(
                        when (selectedTab) {
                            0 -> LessonMode.Vocab
                            1 -> LessonMode.Grammar
                            else -> LessonMode.Shadowing
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                Text("开始本组练习", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.Black)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onOpenSubtitles,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("台词浏览 $subtitleCount", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onStartReadAir,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("语感专项 $readAirCount", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LibrarySearchEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { role = Role.Button },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(19.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "搜索台词：中文描述或日语都可以",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "语义检索全作品字幕",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibraryCharacterProfileEntry(
    workName: String,
    characterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics { role = Role.Button },
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Face, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "角色语言画像",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    text = "$workName · $characterCount 位角色的口癖与语气",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
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
    onOpenSubtitles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCharacterProfile: () -> Unit,
    revealEpisodeActionsRequest: Int,
    modifier: Modifier = Modifier,
) {
    val selectedEpisode = uiState.episodes.firstOrNull { it.episode == uiState.selection.episode }
    val selectedWork = uiState.works.firstOrNull { it.slug == uiState.selection.workSlug }
    val characterOptions = remember(uiState.selection.workSlug) {
        CharacterCatalog.charactersFor(uiState.selection.workSlug)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CourseHeaderBar(onOpenSettings = onOpenSettings)
        LibrarySearchEntry(onClick = onOpenSearch)
        WorkTabs(
            works = uiState.works,
            selectedWorkSlug = uiState.selection.workSlug,
            onWorkSelected = onWorkSelected,
        )
        if (characterOptions.isNotEmpty()) {
            LibraryCharacterProfileEntry(
                workName = selectedWork?.displayName ?: "当前作品",
                characterCount = characterOptions.size,
                onClick = onOpenCharacterProfile,
            )
        }
        EpisodeBrowserSection(
            episodes = uiState.episodes,
            selectedEpisode = uiState.selection.episode,
            onEpisodeSelected = onEpisodeSelected,
            revealRequest = revealEpisodeActionsRequest,
        )
        LibraryQuickAccessPanel(
            episodeLabel = uiState.focus.episodeLabel,
            subtitleCount = selectedEpisode?.totalCues ?: 0,
            readAirCount = uiState.readAir.exercises.size,
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onStartLesson = onStartLesson,
            onStartModeLesson = onStartModeLesson,
            onStartReadAir = onStartReadAir,
            onOpenSubtitles = onOpenSubtitles,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CourseHeaderBar(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "资料库",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "打开设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }
    val colors = animateLibrarySelectionColors(
        selected = selected,
        unselectedContainer = Color.Transparent,
    )
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                this.selected = selected
                role = Role.Tab
            },
        color = colors.container,
        contentColor = colors.content,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, colors.border),
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(tab.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Text(
                    "${tab.count}${tab.eyebrow}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
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
    var query by rememberSaveable(uiState.selection.workSlug, uiState.selection.episode) { mutableStateOf("") }
    // 换作品 / 换集时 rememberSaveable 自动重建 → 等级筛选归「全部」。
    var selectedLevel by rememberSaveable(uiState.selection.workSlug, uiState.selection.episode) {
        mutableStateOf(VocabLevelAll)
    }
    val levelBuckets = remember(vocab) { buildVocabLevelBuckets(vocab) }
    // 兜底：选中的等级若在当前集不存在（数据热更新等），按「全部」处理。
    val activeLevel = remember(levelBuckets, selectedLevel) {
        if (levelBuckets.any { it.key == selectedLevel }) selectedLevel else VocabLevelAll
    }
    val activeLevelLabel = remember(levelBuckets, activeLevel) {
        levelBuckets.firstOrNull { it.key == activeLevel }?.label.orEmpty()
    }
    var visibleCount by rememberSaveable(
        uiState.selection.workSlug,
        uiState.selection.episode,
        vocab.size,
        activeLevel,
    ) {
        mutableIntStateOf(minOf(VocabPageSize, vocab.size))
    }
    // 先按搜索词过滤，再按 JLPT 等级过滤：两个条件叠加取交集。
    val searchedVocab = remember(vocab, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) {
            vocab
        } else {
            vocab.filter { item ->
                listOf(item.surface, item.reading, item.romanization, item.meaningZh)
                    .any { value -> value.lowercase().contains(normalized) }
            }
        }
    }
    val filteredVocab = remember(searchedVocab, activeLevel) {
        if (activeLevel == VocabLevelAll) {
            searchedVocab
        } else {
            searchedVocab.filter { item -> normalizeJlptLevel(item.level) == activeLevel }
        }
    }
    val visibleVocab = remember(filteredVocab, visibleCount) { filteredVocab.take(visibleCount) }

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
        item(key = "vocab-search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("搜索日语、假名或中文释义") },
                shape = MaterialTheme.shapes.medium,
            )
        }
        // 「全部」之外至少有两个等级时才值得筛选，否则这行 chips 没有意义。
        if (levelBuckets.size > 2) {
            item(key = "vocab-level-filter") {
                VocabLevelFilterRow(
                    buckets = levelBuckets,
                    selectedKey = activeLevel,
                    onSelect = { key -> selectedLevel = key },
                )
            }
        }
        if (playbackState.message.isNotBlank()) {
            item(key = "audio-status") {
                AudioStatusBanner(playbackState = playbackState)
            }
        }
        if (filteredVocab.isNotEmpty()) {
            item(key = "vocab-visible-count") {
                Text(
                    text = if (activeLevel == VocabLevelAll) {
                        "${filteredVocab.size} 个词 · 点击条目开始练习"
                    } else {
                        "$activeLevelLabel · ${filteredVocab.size} 个词 · 点击条目开始练习"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item(key = "vocab-empty") {
                when {
                    query.isNotBlank() && searchedVocab.isEmpty() -> LibraryEmptyState(
                        icon = Icons.Rounded.Search,
                        title = "没有找到匹配的词",
                        subtitle = "试试换个假名或中文关键词",
                    )

                    vocab.isEmpty() -> LibraryEmptyState(
                        icon = Icons.Rounded.AutoStories,
                        title = "本集还没有词汇",
                        subtitle = "换一集看看，或先从跟读句开始",
                    )

                    else -> LibraryEmptyState(
                        icon = Icons.Rounded.School,
                        title = "该等级暂无词汇",
                        subtitle = if (query.isBlank()) {
                            "点「全部」看看本集其它等级的词"
                        } else {
                            "换个关键词，或点「全部」看所有等级"
                        },
                    )
                }
            }
        }
        items(
            visibleVocab,
            key = { it.id },
            contentType = { "vocab-row" },
        ) { item ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = { onTargetLesson(LessonTarget.Vocab(item.id)) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        item.surface,
                                        modifier = Modifier.alignByBaseline(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        item.reading,
                                        modifier = Modifier.alignByBaseline(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LabTheme.colors.info,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                if (item.meaningZh.isNotBlank()) {
                                    Text(
                                        text = item.meaningZh,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (item.partOfSpeech.isNotBlank() || item.level.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (item.partOfSpeech.isNotBlank()) {
                                            TagChip(item.partOfSpeech)
                                        }
                                        if (item.level.isNotBlank()) {
                                            VocabLevelTagChip(level = item.level)
                                        }
                                    }
                                }
                            }
                            LibraryRowActions(
                                onPlay = { onSpeak(item.surface) },
                                playDescription = "播放 ${item.surface}",
                                aiDescription = "精讲 ${item.surface}",
                                onAskAi = {
                                    onAskAi(
                                        item.libraryAiKey("vocab"),
                                        "vocab",
                                        item.surface,
                                        item.aiContext(uiState.focus.episodeLabel),
                                    )
                                },
                            )
                        }
                        LinguisticNerdSection(payload = item.linguistic)
                    }
                }
                LibraryAiPanel(targetKey = item.libraryAiKey("vocab"), uiState = uiState)
            }
        }
        if (visibleCount < filteredVocab.size) {
            item(key = "load-more-vocab") {
                SecondaryButton(
                    text = "加载更多 ${minOf(VocabPageSize, filteredVocab.size - visibleCount)} 个词",
                    onClick = {
                        visibleCount = minOf(visibleCount + VocabPageSize, filteredVocab.size)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
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
        if (grammar.isEmpty()) {
            item(key = "grammar-empty") {
                LibraryEmptyState(
                    icon = Icons.Rounded.School,
                    title = "本集还没有语法点",
                    subtitle = "换一集看看，或先积累一些词汇",
                )
            }
        }
        items(
            grammar,
            key = { it.id },
            contentType = { "grammar-row" },
        ) { item ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = { onTargetLesson(LessonTarget.Grammar(item.id)) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.pattern, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    item.titleZh,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LibraryRowActions(
                                onPlay = null,
                                playDescription = "",
                                aiDescription = "精讲 ${item.pattern}",
                                onAskAi = {
                                    onAskAi(
                                        item.libraryAiKey("grammar"),
                                        "grammar",
                                        item.pattern,
                                        item.aiContext(uiState.focus.episodeLabel),
                                    )
                                },
                            )
                        }
                        if (item.exampleJa.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        item.exampleJa,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (item.exampleZh.isNotBlank()) {
                                        Text(
                                            item.exampleZh,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        if (item.explanationZh.isNotBlank()) {
                            Text(
                                item.explanationZh,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        LinguisticNerdSection(payload = item.linguistic)
                    }
                }
                LibraryAiPanel(targetKey = item.libraryAiKey("grammar"), uiState = uiState)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val annotator = rememberFuriganaAnnotator(uiState.settings)
    val deepDive = rememberSentenceDeepDive(uiState.settings)
    val showFurigana = uiState.settings.showFurigana
    val showRomaji = uiState.settings.showRomaji
    var sourceAudioOnly by rememberSaveable(uiState.selection.workSlug, uiState.selection.episode) {
        mutableStateOf(false)
    }
    val sourceAudioCount = remember(uiState.shadowing) { uiState.shadowing.count { it.hasSourceAudio } }
    val sentences = remember(uiState.shadowing, sourceAudioOnly) {
        if (sourceAudioOnly) uiState.shadowing.filter { it.hasSourceAudio } else uiState.shadowing
    }
    val hiddenCount = uiState.shadowing.size - sentences.size
    val furiganaTexts = remember(sentences) { sentences.map { it.ja } }

    LaunchedEffect(furiganaTexts, showFurigana) {
        if (showFurigana) {
            annotator.request("sentence", furiganaTexts)
        }
    }

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
        if (uiState.shadowing.isNotEmpty()) {
            item(key = "shadowing-filters") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LibraryFilterChip(
                        text = "只看原声 $sourceAudioCount",
                        selected = sourceAudioOnly,
                        onClick = { sourceAudioOnly = !sourceAudioOnly },
                    )
                    Text(
                        text = if (sourceAudioOnly) {
                            "已隐藏 $hiddenCount 句无原声"
                        } else {
                            "${uiState.shadowing.size} 句 · 点击条目开始练习"
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (uiState.shadowing.isEmpty()) {
            item(key = "shadowing-empty") {
                LibraryEmptyState(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    title = "本集还没有跟读句",
                    subtitle = "换一集看看，或先浏览台词找感觉",
                )
            }
        } else if (sentences.isEmpty()) {
            item(key = "shadowing-filtered-empty") {
                LibraryEmptyState(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    title = "本集没有原声句",
                    subtitle = "关掉「只看原声」就能看到全部跟读句",
                )
            }
        }
        items(
            sentences,
            key = { it.id },
            contentType = { "shadowing-row" },
        ) { item ->
            val audio = remember(uiState.selection.workSlug, item) {
                promptAudioForSentence(uiState.selection.workSlug, item, autoPlay = false)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = { onTargetLesson(LessonTarget.Sentence(item.id)) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.sourceLabel,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            LibraryRowActions(
                                onPlay = { onPlay(audio) },
                                playDescription = "播放跟读句",
                                aiDescription = "精讲这句话",
                                onAskAi = {
                                    onAskAi(
                                        item.libraryAiKey("sentence"),
                                        "sentence",
                                        item.ja,
                                        item.aiContext(uiState.focus.episodeLabel),
                                    )
                                },
                            )
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (item.hasSourceAudio) {
                                LibraryAccentChip(
                                    text = audio.labelForChip(),
                                    container = LabTheme.colors.successContainer,
                                    content = LabTheme.colors.onSuccessContainer,
                                )
                            } else {
                                TagChip(audio.labelForChip())
                            }
                            if (item.difficulty.isNotBlank()) {
                                TagChip(item.difficulty)
                            }
                            item.toneTags.take(3).forEach { tag ->
                                TagChip(tag)
                            }
                        }
                        if (showFurigana) {
                            RubyText(
                                text = item.ja,
                                furigana = annotator.resultFor(item.ja),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                rubyStyle = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                rubyColor = LabTheme.colors.info,
                            )
                        } else {
                            Text(
                                item.ja,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (item.reading.isNotBlank()) {
                            Text(
                                item.reading,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LabTheme.colors.info,
                            )
                        }
                        if (showRomaji && item.romaji.isNotBlank()) {
                            Text(
                                item.romaji,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (item.meaningZh.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    item.meaningZh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = {
                                    deepDive.request(
                                        DeepDiveTarget(
                                            workSlug = uiState.selection.workSlug,
                                            episode = uiState.selection.episode,
                                            lineNo = item.sourceLineNo,
                                            jaText = item.ja,
                                            zhText = item.meaningZh,
                                        ),
                                    )
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    "精读",
                                    modifier = Modifier.padding(start = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                        LinguisticNerdSection(payload = item.linguistic)
                    }
                }
                LibraryAiPanel(targetKey = item.libraryAiKey("sentence"), uiState = uiState)
            }
        }
    }
    SentenceDeepDiveSheet(deepDive)
}

@Composable
private fun LibraryFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = animateLibrarySelectionColors(selected = selected)
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics { this.selected = selected },
        color = colors.container,
        contentColor = colors.content,
        shape = CircleShape,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, colors.border),
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

/**
 * 词汇区的 JLPT 等级筛选行：「全部」+ 本集实际出现的 N5..N1 +「其他」，
 * 每个 chip 带当前集词数。单选，与搜索框叠加生效。
 */
@Composable
private fun VocabLevelFilterRow(
    buckets: List<VocabLevelBucket>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(buckets, key = { it.key }, contentType = { "vocab-level-chip" }) { bucket ->
            VocabLevelChip(
                label = bucket.label,
                count = bucket.count,
                selected = bucket.key == selectedKey,
                onClick = { onSelect(bucket.key) },
            )
        }
    }
}

@Composable
private fun VocabLevelChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = animateLibrarySelectionColors(selected = selected)
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                this.selected = selected
                role = Role.Tab
            },
        color = colors.container,
        contentColor = colors.content,
        shape = CircleShape,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, colors.border),
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Text(
            text = "$label · $count",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 词汇卡上的等级标签：N5/N4 走 success 系、N3 走 info、N2 走 warning、N1 走 error 系浅容器色，
 * 无法识别的杂值保持中性 TagChip。复用同文件的 LibraryAccentChip（与 TagChip 同尺寸/同形状），
 * 因此不需要改 Common.kt。
 */
@Composable
private fun VocabLevelTagChip(
    level: String,
    modifier: Modifier = Modifier,
) {
    val levelKey = remember(level) { normalizeJlptLevel(level) }
    val accent: Pair<Color, Color>? = when (levelKey) {
        "N5", "N4" -> LabTheme.colors.successContainer to LabTheme.colors.onSuccessContainer
        "N3" -> LabTheme.colors.infoContainer to LabTheme.colors.onInfoContainer
        "N2" -> LabTheme.colors.warningContainer to LabTheme.colors.onWarningContainer
        "N1" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> null
    }
    if (accent == null) {
        TagChip(level, modifier = modifier)
    } else {
        LibraryAccentChip(
            text = level,
            container = accent.first,
            content = accent.second,
            modifier = modifier,
        )
    }
}

@Composable
private fun LibraryAccentChip(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, content.copy(alpha = 0.28f)),
        shape = CircleShape,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Collapsed-by-default "语言学加餐" addendum shown at the tail of vocab / grammar /
 * sentence cards. Purely explanatory: it never changes the card's tap target or answers.
 */
@Composable
private fun LinguisticNerdSection(
    payload: LinguisticCardPayload?,
    modifier: Modifier = Modifier,
) {
    if (payload == null || !payload.hasContent) return
    var expanded by rememberSaveable(payload.headlineZh) { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MotionTokens.microSpec(reducedMotion),
        label = "linguistic-nerd-chevron",
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = "语言学加餐",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            if (payload.level.isNotBlank()) {
                Text(
                    text = payload.level,
                    modifier = Modifier.padding(end = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "收起语言学加餐" else "展开语言学加餐",
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                    easing = MotionTokens.Curve.Decelerate,
                ),
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                    easing = MotionTokens.Curve.Standard,
                ),
            ),
            exit = shrinkVertically(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                    easing = MotionTokens.Curve.Standard,
                ),
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                    easing = MotionTokens.Curve.Standard,
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (payload.headlineZh.isNotBlank()) {
                    Text(
                        payload.headlineZh,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                payload.terms.forEach { term ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                term.termZh,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                            )
                            if (term.plainZh.isNotBlank()) {
                                Text(
                                    term.plainZh,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                payload.domains.forEach { domain ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            domain.titleZh.ifBlank { domain.domain },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (domain.takeawayZh.isNotBlank()) {
                            Text(
                                domain.takeawayZh,
                                style = MaterialTheme.typography.labelMedium,
                                color = LabTheme.colors.info,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (domain.explanationZh.isNotBlank()) {
                            Text(
                                domain.explanationZh,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (payload.cautionZh.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LabTheme.colors.warningContainer,
                        contentColor = LabTheme.colors.onWarningContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "注意：${payload.cautionZh}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (payload.historicalNoteZh.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "源流：${payload.historicalNoteZh}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRowActions(
    onPlay: (() -> Unit)?,
    playDescription: String,
    aiDescription: String,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (onPlay != null) {
            IconButton(onClick = onPlay) {
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = playDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作：$aiDescription")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("AI 精讲", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onAskAi()
                    },
                )
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = "进入练习", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("正在智能精讲…", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
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
    val isError = playbackState.phase == AudioPlaybackPhase.Error
    LabCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isError) MaterialTheme.colorScheme.error else LabTheme.colors.info,
            )
            Text(
                text = playbackState.message,
                style = MaterialTheme.typography.labelLarge,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun PromptAudio.labelForChip(): String {
    return when (this) {
        PromptAudio.None -> "无语音"
        is PromptAudio.Tts -> "语音"
        is PromptAudio.Source -> if (reliability == AudioReliability.Verified) "原声" else "原声待确认"
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
        append("\n日文台词：")
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
