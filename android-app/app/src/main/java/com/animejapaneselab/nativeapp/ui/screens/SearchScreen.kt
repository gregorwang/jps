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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RagAnalysis
import com.animejapaneselab.nativeapp.data.RagSearchResult
import com.animejapaneselab.nativeapp.data.RagSearchSource
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.data.SubtitleLine
import com.animejapaneselab.nativeapp.data.WorkOption
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 一次检索请求。[seq] 单调自增，保证「同样的问题再搜一次」和「重试」也能重新触发
 * LaunchedEffect（data class 相等的话 effect 不会重启）。
 */
private data class SubtitleSearchRequest(
    val query: String,
    val workSlug: String,
    val seq: Int,
)

/** 台词搜索状态机：待输入 / 检索中 / 失败（可重试）/ 已就绪（可能没有命中）。 */
private sealed interface SubtitleSearchState {
    data object Idle : SubtitleSearchState
    data object Loading : SubtitleSearchState
    data class Failed(val message: String) : SubtitleSearchState
    data class Ready(val result: RagSearchResult) : SubtitleSearchState
}

/** 空态与引导里的示例问题，点一下直接搜。 */
private val SearchSuggestions = listOf(
    "傲娇地拒绝别人",
    "ありがとう的场景",
    "第一次见面的自我介绍",
    "下定决心的台词",
    "被吓到时的惊呼",
)

/**
 * 台词语义搜索。这个屏幕自己拉数据（LocalLabStore + RemoteLabClient），不经过 LabViewModel；
 * 网络请求只在 Dispatchers.IO 里用 runCatching 包住，取消异常照常向上抛。
 */
@Composable
fun SearchScreen(
    uiState: LabUiState,
    onBack: () -> Unit,
    onOpenSubtitleLine: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { LocalLabStore(context) }
    val apiBaseUrl = uiState.settings.apiBaseUrl
    val focusManager = LocalFocusManager.current

    var query by rememberSaveable { mutableStateOf("") }
    var selectedWorkSlug by rememberSaveable { mutableStateOf(uiState.selection.workSlug) }
    var requestSeq by remember { mutableIntStateOf(0) }
    var request by remember { mutableStateOf<SubtitleSearchRequest?>(null) }
    var state by remember { mutableStateOf<SubtitleSearchState>(SubtitleSearchState.Idle) }

    val activeRequest = request
    // 换服务器地址或发出新请求都会重跑；上一次没跑完的检索由 LaunchedEffect 自动取消。
    LaunchedEffect(apiBaseUrl, activeRequest) {
        if (activeRequest == null) return@LaunchedEffect
        state = SubtitleSearchState.Loading
        val outcome = runCatching {
            withContext(Dispatchers.IO) {
                RemoteLabClient(apiBaseUrl, store.readSessionCookie()).searchSubtitles(
                    query = activeRequest.query,
                    // 传目录 slug（re-zero），客户端内部会转成 RAG 索引用的 slug。
                    workSlug = activeRequest.workSlug,
                    deviceId = store.deviceId(),
                    episode = null,
                    topK = 8,
                )
            }
        }
        state = outcome.fold(
            onSuccess = { result -> SubtitleSearchState.Ready(result) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                SubtitleSearchState.Failed(
                    error.message.orEmpty().ifBlank { "搜索失败，请检查网络后重试。" },
                )
            },
        )
    }

    val submit: (String) -> Unit = submit@{ raw ->
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return@submit
        query = trimmed
        focusManager.clearFocus()
        requestSeq += 1
        request = SubtitleSearchRequest(query = trimmed, workSlug = selectedWorkSlug, seq = requestSeq)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SearchTopBar(onBack = onBack)
        SearchQueryPanel(
            query = query,
            works = uiState.works,
            selectedWorkSlug = selectedWorkSlug,
            searching = state is SubtitleSearchState.Loading,
            onQueryChange = { query = it },
            onSubmit = { submit(query) },
            onWorkSelected = { slug ->
                if (slug != selectedWorkSlug) {
                    selectedWorkSlug = slug
                    // 已经搜过一次的话，换作品直接用同一个问题重搜，省得再点一次。
                    val pending = query.trim()
                    if (pending.isNotBlank() && request != null) {
                        requestSeq += 1
                        request = SubtitleSearchRequest(query = pending, workSlug = slug, seq = requestSeq)
                    }
                }
            },
        )
        when (val current = state) {
            is SubtitleSearchState.Idle -> SearchIdleState(onSuggestionClick = submit)

            is SubtitleSearchState.Loading -> SearchLoadingState()

            is SubtitleSearchState.Failed -> SearchFailedState(
                message = current.message,
                onRetry = {
                    val pending = request
                    if (pending != null) {
                        requestSeq += 1
                        request = pending.copy(seq = requestSeq)
                    }
                },
            )

            is SubtitleSearchState.Ready -> SearchResultList(
                result = current.result,
                onOpenSubtitleLine = onOpenSubtitleLine,
                onSuggestionClick = submit,
            )
        }
    }
}

@Composable
private fun SearchTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LabSpacing.XSmall)
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("台词搜索", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                text = "用中文或日语描述，搜遍全部字幕",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 输入框 + 作品范围 chips。作品列表还没加载出来时只显示输入框。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchQueryPanel(
    query: String,
    works: List<WorkOption>,
    selectedWorkSlug: String,
    searching: Boolean,
    onQueryChange: (String) -> Unit,
    onWorkSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LabSpacing.Screen)
            .padding(bottom = LabSpacing.XSmall),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = labFieldColors(),
            placeholder = { Text("例：傲娇地拒绝别人 / ありがとう的场景") },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = if (searching || query.isNotEmpty()) {
                {
                    if (searching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "清空搜索词")
                        }
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        )
        if (works.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall),
            ) {
                works.forEach { work ->
                    val selected = work.slug == selectedWorkSlug
                    FilterChip(
                        selected = selected,
                        onClick = { onWorkSelected(work.slug) },
                        label = {
                            Text(
                                text = work.displayName.ifBlank { work.slug },
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = searchChipColors(),
                        border = searchChipBorder(selected),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchIdleState(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LabSpacing.Screen)
            .padding(top = LabSpacing.XSmall),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
    ) {
        LabCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "描述场景，而不是背台词",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "写一句中文或日语，系统会在整部作品的字幕里找出意思最接近的片段，" +
                    "并顺手解读它们的共同点。搜索要跑向量检索和 AI 分析，通常要等几秒。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
            )
        }
        Text(
            text = "试试这些说法",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        SearchSuggestionRow(onSuggestionClick = onSuggestionClick)
    }
}

@Composable
private fun SearchLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LabSpacing.Screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = "正在全库检索…",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "先做向量检索再让 AI 解读，大约要等几秒。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SearchFailedState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LabSpacing.Screen)
            .padding(top = LabSpacing.XSmall),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(LabSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall),
            ) {
                Text(
                    text = "搜索失败",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }
        }
        SecondaryButton(
            text = "重新搜索",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SearchResultList(
    result: RagSearchResult,
    onOpenSubtitleLine: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (result.sources.isEmpty()) {
        SearchEmptyState(onSuggestionClick = onSuggestionClick, modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LabSpacing.Screen,
            end = LabSpacing.Screen,
            top = LabSpacing.XSmall,
            bottom = LabSpacing.XXLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
    ) {
        val analysis = result.analysis
        if (analysis != null) {
            item(key = "search-analysis", contentType = "search-analysis") {
                SearchAnalysisCard(analysis = analysis, modifier = animatedSearchItem())
            }
        }
        item(key = "search-result-meta", contentType = "search-result-meta") {
            Text(
                text = "命中 ${result.sources.size} 个片段 · 点任意一句跳到字幕定位",
                modifier = Modifier.padding(horizontal = LabSpacing.XXSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        itemsIndexed(
            items = result.sources,
            // 服务端 id 理论上唯一，但兜底加上序号，避免重复 key 让列表崩掉。
            key = { index, source -> "search-source-$index-${source.id}" },
            contentType = { _, _ -> "search-source" },
        ) { _, source ->
            SearchSourceCard(
                source = source,
                onOpenSubtitleLine = onOpenSubtitleLine,
                modifier = animatedSearchItem(),
            )
        }
    }
}

/** 列表项进出与重排动画，尊重系统「减少动态效果」设置。 */
@Composable
private fun LazyItemScope.animatedSearchItem(): Modifier {
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
private fun SearchAnalysisCard(
    analysis: RagAnalysis,
    modifier: Modifier = Modifier,
) {
    val labColors = LabTheme.colors
    val bullets = remember(analysis.bullets) { analysis.bullets.filter { it.isNotBlank() } }
    LabCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                color = labColors.infoContainer,
                contentColor = labColors.onInfoContainer,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = "AI 解读",
                color = labColors.info,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = analysis.title.ifBlank { "这些台词的共同点" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
        )
        if (analysis.summary.isNotBlank()) {
            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
            )
        }
        if (bullets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall)) {
                bullets.forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSourceCard(
    source: RagSearchSource,
    onOpenSubtitleLine: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val timeLabel = remember(source.startTime, source.endTime) {
        searchTimeRangeLabel(source.startTime, source.endTime)
    }
    val scoreLabel = remember(source.score) { searchScoreLabel(source.score) }
    LabCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(LabSpacing.Medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TagChip(text = "EP${source.episode} · 场景${source.chunkNo}")
            if (timeLabel.isNotBlank()) {
                Text(
                    text = timeLabel,
                    modifier = Modifier.weight(1f),
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            if (scoreLabel.isNotBlank()) {
                Text(
                    text = scoreLabel,
                    color = LabTheme.colors.info,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
        }
        if (source.lines.isEmpty()) {
            // 服务端没返回逐行数据时，退回展示这个 chunk 的原文。
            Text(
                text = source.text.ifBlank { "这个片段没有可显示的台词。" },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall)) {
                source.lines.forEach { line ->
                    SearchLineRow(
                        line = line,
                        onClick = { onOpenSubtitleLine(source.workSlug, source.episode, line.lineNo) },
                    )
                }
            }
        }
    }
}

/** 单句台词：整行可点，点了跳到字幕页并定位到这一行。 */
@Composable
private fun SearchLineRow(
    line: SubtitleLine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        color = colorScheme.surfaceContainerLow,
        contentColor = colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LabSpacing.Small, vertical = LabSpacing.XSmall),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = line.jaText,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                )
                if (line.zhText.isNotBlank()) {
                    Text(
                        text = line.zhText,
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "在字幕里定位这一句",
                modifier = Modifier.size(18.dp),
                tint = colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SearchEmptyState(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LabSpacing.Screen)
            .padding(top = LabSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Text(
            text = "没搜到相关台词",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "换个说法试试：描述场景和语气（谁在做什么、什么情绪），比只写一个词更容易搜到。也可以换一部作品再搜。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
        SearchSuggestionRow(onSuggestionClick = onSuggestionClick)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchSuggestionRow(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
    ) {
        SearchSuggestions.forEach { suggestion ->
            SearchSuggestionChip(
                text = suggestion,
                onClick = { onSuggestionClick(suggestion) },
            )
        }
    }
}

@Composable
private fun SearchSuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        // minimumInteractiveComponentSize 把触控区撑到 48dp，视觉高度保持紧凑。
        modifier = modifier.minimumInteractiveComponentSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun searchChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurface,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun searchChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MaterialTheme.colorScheme.outline,
    selectedBorderColor = MaterialTheme.colorScheme.primary,
)

/** 时间戳只取前 8 位（HH:MM:SS）；拿到什么格式都不会崩。 */
private fun searchTimeRangeLabel(startTime: String, endTime: String): String {
    val start = startTime.trim().take(8)
    val end = endTime.trim().take(8)
    return when {
        start.isBlank() -> ""
        end.isBlank() || end == start -> start
        else -> "$start – $end"
    }
}

/** 相关度是 0–1 的相似度分数，展示成整数百分比；异常值直接不显示。 */
private fun searchScoreLabel(score: Double): String {
    if (!score.isFinite()) return ""
    return "%.0f%%".format(score.coerceIn(0.0, 1.0) * 100)
}
