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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.AiHistoryDetail
import com.animejapaneselab.nativeapp.data.AiHistoryEntry
import com.animejapaneselab.nativeapp.data.AiHistoryGroup
import com.animejapaneselab.nativeapp.data.AiHistorySnapshot
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.StructuredAiResultCard
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 列表状态机：加载中 / 失败（可重试）/ 已就绪（可能为空）。 */
private sealed interface AiHistoryListState {
    data object Loading : AiHistoryListState
    data class Ready(val snapshot: AiHistorySnapshot) : AiHistoryListState
    data class Failed(val message: String) : AiHistoryListState
}

/** 详情状态机；[AiHistoryDetailState.Ready.detail] 为空表示服务端没有这条详情。 */
private sealed interface AiHistoryDetailState {
    data object Loading : AiHistoryDetailState
    data class Ready(val detail: AiHistoryDetail?) : AiHistoryDetailState
    data class Failed(val message: String) : AiHistoryDetailState
}

/**
 * AI 讲解历史。这个屏幕自己拉数据（LocalLabStore + RemoteLabClient），
 * 不经过 LabViewModel；网络请求只在 Dispatchers.IO 里用 runCatching 包住。
 */
@Composable
fun AiHistoryScreen(
    uiState: LabUiState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { LocalLabStore(context) }
    val apiBaseUrl = uiState.settings.apiBaseUrl

    var listState by remember { mutableStateOf<AiHistoryListState>(AiHistoryListState.Loading) }
    var refreshing by remember { mutableStateOf(false) }
    var listReloadToken by remember { mutableIntStateOf(0) }
    var selectedEntry by remember { mutableStateOf<AiHistoryEntry?>(null) }
    var detailState by remember { mutableStateOf<AiHistoryDetailState>(AiHistoryDetailState.Loading) }
    var detailReloadToken by remember { mutableIntStateOf(0) }

    // 首次进入即拉取；刷新/重试通过自增 token 重跑同一个 effect。
    LaunchedEffect(apiBaseUrl, listReloadToken) {
        refreshing = true
        if (listState !is AiHistoryListState.Ready) {
            listState = AiHistoryListState.Loading
        }
        val outcome = runCatching {
            withContext(Dispatchers.IO) {
                RemoteLabClient(apiBaseUrl, store.readSessionCookie()).fetchAiHistory(store.deviceId())
            }
        }
        listState = outcome.fold(
            onSuccess = { snapshot -> AiHistoryListState.Ready(snapshot) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                AiHistoryListState.Failed(error.message.orEmpty().ifBlank { "无法连接服务器，请检查网络后重试。" })
            },
        )
        refreshing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AiHistoryTopBar(
            loading = refreshing,
            onBack = onBack,
            onRefresh = { listReloadToken += 1 },
        )
        when (val state = listState) {
            is AiHistoryListState.Loading -> AiHistoryLoadingState()

            is AiHistoryListState.Failed -> AiHistoryFailedState(
                message = state.message,
                onRetry = { listReloadToken += 1 },
            )

            is AiHistoryListState.Ready -> if (state.snapshot.entries.isEmpty()) {
                AiHistoryEmptyState()
            } else {
                AiHistoryList(
                    snapshot = state.snapshot,
                    onEntryClick = { entry ->
                        // 先清空，避免弹层闪一帧上一条记录的内容。
                        detailState = AiHistoryDetailState.Loading
                        selectedEntry = entry
                    },
                )
            }
        }
    }

    val activeEntry = selectedEntry
    if (activeEntry != null) {
        LaunchedEffect(apiBaseUrl, activeEntry.group, activeEntry.id, detailReloadToken) {
            detailState = AiHistoryDetailState.Loading
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    RemoteLabClient(apiBaseUrl, store.readSessionCookie()).fetchAiHistoryDetail(
                        type = activeEntry.group.detailType(),
                        id = activeEntry.id,
                        deviceId = store.deviceId(),
                    )
                }
            }
            detailState = outcome.fold(
                onSuccess = { detail -> AiHistoryDetailState.Ready(detail) },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    AiHistoryDetailState.Failed(error.message.orEmpty().ifBlank { "详情加载失败，请重试。" })
                },
            )
        }
        AiHistoryDetailSheet(
            entry = activeEntry,
            state = detailState,
            onRetry = { detailReloadToken += 1 },
            onDismiss = { selectedEntry = null },
        )
    }
}

@Composable
private fun AiHistoryTopBar(
    loading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
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
            Text("AI 讲解历史", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                text = "回看每一次智能讲解",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRefresh, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            } else {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "刷新记录",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AiHistoryList(
    snapshot: AiHistorySnapshot,
    onEntryClick: (AiHistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(snapshot) {
        AiHistoryGroup.entries.mapNotNull { group ->
            val rows = snapshot.entries.filter { it.group == group }
            if (rows.isEmpty()) null else group to rows
        }
    }
    val generatedAtLabel = remember(snapshot.generatedAt) { historyTimeLabel(snapshot.generatedAt) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = LabSpacing.Screen, vertical = LabSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "ai-history-meta", contentType = "ai-history-meta") {
            Text(
                text = if (generatedAtLabel.isBlank()) {
                    "共 ${snapshot.entries.size} 条记录"
                } else {
                    "共 ${snapshot.entries.size} 条记录 · 更新于 $generatedAtLabel"
                },
                modifier = Modifier.padding(horizontal = LabSpacing.XXSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        sections.forEach { (group, rows) ->
            item(key = "ai-history-section-${group.name}", contentType = "ai-history-section") {
                SectionTitle(
                    eyebrow = group.label,
                    title = "${rows.size} 条记录",
                    modifier = Modifier.padding(top = LabSpacing.XXSmall),
                )
            }
            items(
                items = rows,
                // 三个分组来自不同的表，id 可能撞车，前缀分组名保证 key 全局唯一。
                key = { entry -> "${group.name}-${entry.id}" },
                contentType = { "ai-history-entry" },
            ) { entry ->
                AiHistoryEntryCard(
                    entry = entry,
                    onClick = { onEntryClick(entry) },
                    modifier = animatedHistoryItem(),
                )
            }
        }
    }
}

/** 列表项进出与重排动画，尊重系统「减少动态效果」设置。 */
@Composable
private fun LazyItemScope.animatedHistoryItem(): Modifier {
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
private fun AiHistoryEntryCard(
    entry: AiHistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val sourceLabel = remember(entry.workSlug, entry.episode) { historySourceLabel(entry.workSlug, entry.episode) }
    val timeLabel = remember(entry.timestamp) { historyTimeLabel(entry.timestamp) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        color = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.8f)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = LabSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = entry.title.ifBlank { entry.group.label },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.summary.isNotBlank()) {
                Text(
                    text = entry.summary,
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TagChip(text = historyKindLabel(entry))
                if (sourceLabel.isNotBlank()) {
                    Text(
                        text = sourceLabel,
                        modifier = Modifier.weight(1f),
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (timeLabel.isNotBlank()) {
                    Text(
                        text = timeLabel,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiHistoryLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LabSpacing.Screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = "正在读取 AI 记录…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AiHistoryFailedState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LabSpacing.Screen, vertical = LabSpacing.Small),
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
                    text = "记录加载失败",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }
        }
        SecondaryButton(
            text = "重新加载",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AiHistoryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LabSpacing.Screen, vertical = LabSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Small, Alignment.CenterVertically),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.History,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Text(
            text = "还没有 AI 讲解记录",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "在课程、资料库或复习里向 AI 提问后，记录会出现在这里。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiHistoryDetailSheet(
    entry: AiHistoryEntry,
    state: AiHistoryDetailState,
    onRetry: () -> Unit,
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
                .padding(horizontal = LabSpacing.Screen)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        ) {
            Text(
                text = entry.title.ifBlank { entry.group.label },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            val metaLabel = remember(entry) { historyMetaLabel(entry) }
            if (metaLabel.isNotBlank()) {
                Text(
                    text = metaLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            when (state) {
                is AiHistoryDetailState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "正在读取这条记录…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                is AiHistoryDetailState.Failed -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = "详情加载失败：${state.message}",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    SecondaryButton(
                        text = "重试",
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is AiHistoryDetailState.Ready -> {
                    val detail = state.detail
                    if (detail == null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                text = "这条记录没有详情",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        if (detail.promptText.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    text = "提问：${detail.promptText}",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        StructuredAiResultCard(
                            result = detail.result,
                            fallbackText = detail.result?.text ?: detail.summary.ifBlank { detail.promptText },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** 分组 → `/api/history/detail` 的 type 参数。 */
private fun AiHistoryGroup.detailType(): String = when (this) {
    AiHistoryGroup.Ai -> "ai"
    AiHistoryGroup.Correction -> "correction"
    AiHistoryGroup.Profile -> "profile"
}

/** 服务端 kind 是原始英文串，能翻的翻，翻不了就原样显示，最后兜底成分组名。 */
private fun historyKindLabel(entry: AiHistoryEntry): String {
    val raw = entry.kind.trim()
    if (raw.isBlank()) return entry.group.label
    return when (raw.lowercase()) {
        "vocab", "word" -> "词汇"
        "grammar" -> "语法"
        "sentence", "subtitle", "line", "deep_dive" -> "台词"
        "exercise", "question" -> "练习"
        "mistake" -> "错题"
        "shadowing" -> "跟读"
        "correction" -> "批改"
        "profile", "character" -> "角色"
        else -> raw
    }
}

/** 作品 · EP 标签；作品为空时整条省略。 */
private fun historySourceLabel(workSlug: String, episode: Int): String {
    val slug = workSlug.trim()
    if (slug.isBlank()) return ""
    return if (episode > 0) "$slug · EP$episode" else slug
}

/** 时间戳只取前 16 位并把 ISO 的 T 换成空格；拿到什么格式都不会崩。 */
private fun historyTimeLabel(timestamp: String): String {
    val trimmed = timestamp.trim()
    if (trimmed.isBlank()) return ""
    return trimmed.take(16).replace('T', ' ').trim()
}

/** 详情弹层顶部的一行元信息：分组 · 类型 · 模型 · 作品 · 时间。 */
private fun historyMetaLabel(entry: AiHistoryEntry): String {
    return listOf(
        entry.group.label,
        historyKindLabel(entry),
        entry.model.trim(),
        historySourceLabel(entry.workSlug, entry.episode),
        historyTimeLabel(entry.timestamp),
    ).filter { it.isNotBlank() }.distinct().joinToString(" · ")
}
