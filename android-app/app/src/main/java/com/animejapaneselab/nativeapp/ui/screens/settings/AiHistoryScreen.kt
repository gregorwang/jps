package com.animejapaneselab.nativeapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AiExplainResult
import com.animejapaneselab.nativeapp.data.AiHistoryDetail
import com.animejapaneselab.nativeapp.data.AiHistoryEntry
import com.animejapaneselab.nativeapp.data.AiHistoryGroup
import com.animejapaneselab.nativeapp.data.AiHistorySnapshot
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ToolPanel
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private sealed interface HistoryListState {
    data object Loading : HistoryListState
    data class Ready(val snapshot: AiHistorySnapshot) : HistoryListState
    data class Failed(val message: String) : HistoryListState
}

private sealed interface HistoryDetailState {
    data object Loading : HistoryDetailState
    data class Ready(val detail: AiHistoryDetail?) : HistoryDetailState
    data class Failed(val message: String) : HistoryDetailState
}

/**
 * AI 讲解历史 (v2 AiHistory structure in v3 type and hairlines): group pills, day eyebrows,
 * one hairline row per explanation, detail in a v3 bottom sheet. Loads its own data.
 */
@Composable
fun AiHistoryScreen(
    uiState: LabUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { LocalLabStore(context) }
    val apiBaseUrl = uiState.settings.apiBaseUrl
    val colors = AjlTheme.colors

    var listState by remember { mutableStateOf<HistoryListState>(HistoryListState.Loading) }
    var refreshing by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var groupFilter by rememberSaveable { mutableStateOf<AiHistoryGroup?>(null) }
    var selected by remember { mutableStateOf<AiHistoryEntry?>(null) }
    var detailState by remember { mutableStateOf<HistoryDetailState>(HistoryDetailState.Loading) }
    var detailToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(apiBaseUrl, reloadToken) {
        refreshing = true
        if (listState !is HistoryListState.Ready) listState = HistoryListState.Loading
        val outcome = runCatching {
            withContext(Dispatchers.IO) {
                RemoteLabClient(apiBaseUrl, store.readSessionCookie()).fetchAiHistory(store.deviceId())
            }
        }
        listState = outcome.fold(
            onSuccess = { HistoryListState.Ready(it) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                HistoryListState.Failed(error.message.orEmpty().ifBlank { "无法连接服务器" })
            },
        )
        refreshing = false
    }

    Column(modifier.fillMaxSize()) {
        TopBar(
            title = "讲解历史",
            onNav = onBack,
            actions = {
                if (refreshing) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { LoadingDots(delayMillis = 0) }
                } else {
                    IconButton44(Icons.Rounded.Refresh, "刷新记录", onClick = { reloadToken += 1 })
                }
            },
        )
        when (val state = listState) {
            HistoryListState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
            is HistoryListState.Failed -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(state.message, style = AjlTheme.type.body, color = colors.bad)
                OutlineButton("重新加载", onClick = { reloadToken += 1 })
            }
            is HistoryListState.Ready -> if (state.snapshot.entries.isEmpty()) {
                EmptyNote("まだ記録はない", gloss = "还没有 AI 讲解记录")
            } else {
                HistoryList(
                    snapshot = state.snapshot,
                    filter = groupFilter,
                    onFilter = { groupFilter = it },
                    onOpen = { entry ->
                        detailState = HistoryDetailState.Loading
                        selected = entry
                    },
                )
            }
        }
    }

    val entry = selected
    if (entry != null) {
        LaunchedEffect(apiBaseUrl, entry.group, entry.id, detailToken) {
            detailState = HistoryDetailState.Loading
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    RemoteLabClient(apiBaseUrl, store.readSessionCookie()).fetchAiHistoryDetail(
                        type = entry.group.detailType(),
                        id = entry.id,
                        deviceId = store.deviceId(),
                    )
                }
            }
            detailState = outcome.fold(
                onSuccess = { HistoryDetailState.Ready(it) },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    HistoryDetailState.Failed(error.message.orEmpty().ifBlank { "详情加载失败" })
                },
            )
        }
        HistoryDetailSheet(
            entry = entry,
            state = detailState,
            onRetry = { detailToken += 1 },
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun HistoryList(
    snapshot: AiHistorySnapshot,
    filter: AiHistoryGroup?,
    onFilter: (AiHistoryGroup?) -> Unit,
    onOpen: (AiHistoryEntry) -> Unit,
) {
    val groups = remember(snapshot) { AiHistoryGroup.entries.filter { g -> snapshot.entries.any { it.group == g } } }
    val today = remember { LocalDate.now() }
    val days = remember(snapshot, filter) {
        snapshot.entries
            .filter { filter == null || it.group == filter }
            .sortedByDescending { it.timestamp }
            .groupBy { historyDayLabel(it.timestamp, today) }
            .toList()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        if (groups.size > 1) {
            item(key = "filters") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                ) {
                    item { FilterPill("全部 ${snapshot.entries.size}", selected = filter == null, onClick = { onFilter(null) }) }
                    items(groups) { g -> FilterPill(g.shortLabel(), selected = filter == g, onClick = { onFilter(g) }) }
                }
            }
        }
        days.forEach { (day, rows) ->
            item(key = "day-$day") {
                Eyebrow(day, Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp))
            }
            items(rows, key = { "${it.group.name}-${it.id}" }) { entry ->
                HistoryRow(entry = entry, onClick = { onOpen(entry) })
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: AiHistoryEntry, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (pressed) colors.sunken else colors.bg)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        Hairline()
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    entry.title.ifBlank { entry.group.label },
                    style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 25.sp),
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val time = historyClock(entry.timestamp)
                if (time.isNotBlank()) Text(time, style = AjlTheme.type.meta, color = colors.ink3, modifier = Modifier.padding(top = 5.dp))
            }
            if (entry.summary.isNotBlank()) {
                Text(
                    entry.summary,
                    style = AjlTheme.type.caption.copy(lineHeight = 20.sp),
                    color = colors.ink2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(historySourceLine(entry), style = AjlTheme.type.meta, color = colors.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HistoryDetailSheet(
    entry: AiHistoryEntry,
    state: HistoryDetailState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AjlTheme.colors
    AjlBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                entry.title.ifBlank { entry.group.label },
                style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, lineHeight = 28.sp),
                color = colors.ink,
            )
            Eyebrow(historyMetaLine(entry))
            when (state) {
                HistoryDetailState.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    LoadingDots()
                }
                is HistoryDetailState.Failed -> {
                    Text(state.message, style = AjlTheme.type.body, color = colors.bad)
                    OutlineButton("重试", onClick = onRetry)
                }
                is HistoryDetailState.Ready -> {
                    val detail = state.detail
                    if (detail == null) {
                        EmptyNote("記録なし", gloss = "这条记录没有详情")
                    } else {
                        if (detail.promptText.isNotBlank()) {
                            ToolPanel(Modifier.fillMaxWidth(), background = colors.sunken) {
                                Text(
                                    detail.promptText,
                                    style = AjlTheme.type.caption,
                                    color = colors.ink2,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                        AiResultBody(
                            result = detail.result,
                            fallback = detail.result?.text ?: detail.summary.ifBlank { detail.promptText },
                        )
                    }
                }
            }
        }
    }
}

/** Structured AI explanation: summary, then titled sections with markdown-ish lines cleaned. */
@Composable
private fun AiResultBody(result: AiExplainResult?, fallback: String) {
    val colors = AjlTheme.colors
    val summary = result?.summary.orEmpty().ifBlank { fallback }
    if (summary.isNotBlank()) {
        Text(summary, style = AjlTheme.type.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
    }
    val sections = result?.sections.orEmpty().filter { it.title.isNotBlank() || it.body.isNotBlank() }
    if (sections.isNotEmpty()) {
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Hairline()
                Text(
                    section.title.ifBlank { "说明" },
                    style = AjlTheme.type.label.copy(fontSize = 15.sp),
                    color = colors.ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
                PlainLines(section.body)
            }
        }
    } else if (fallback.isNotBlank() && fallback != summary) {
        PlainLines(fallback)
    }
}

@Composable
private fun PlainLines(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        cleanAiLines(text).forEach { line ->
            Text(line, style = AjlTheme.type.body, color = AjlTheme.colors.ink2)
        }
    }
}

// ---------------------------------------------------------------------------
// Pure helpers
// ---------------------------------------------------------------------------

internal fun cleanAiLines(text: String): List<String> = text
    .replace("\r\n", "\n")
    .replace("\r", "\n")
    .split("\n")
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .map { line ->
        line.replace(Regex("^#{1,6}\\s*"), "")
            .replace(Regex("^\\d+[.、]\\s*"), "")
            .replace(Regex("^[-*•]\\s+"), "・")
            .replace("**", "")
            .trim()
    }

private fun AiHistoryGroup.detailType(): String = when (this) {
    AiHistoryGroup.Ai -> "ai"
    AiHistoryGroup.Correction -> "correction"
    AiHistoryGroup.Profile -> "profile"
}

private fun AiHistoryGroup.shortLabel(): String = when (this) {
    AiHistoryGroup.Ai -> "精讲"
    AiHistoryGroup.Correction -> "批改"
    AiHistoryGroup.Profile -> "画像"
}

private fun historyKindLabel(entry: AiHistoryEntry): String {
    val raw = entry.kind.trim()
    if (raw.isBlank()) return entry.group.shortLabel()
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

private fun historySource(workSlug: String, episode: Int): String {
    val slug = workSlug.trim()
    if (slug.isBlank()) return ""
    val name = WorkIdentity.displayName(slug, slug)
    return if (episode > 0) "$name ${episode.toString().padStart(2, '0')}" else name
}

/** 来自 错题 · けいおん！ 03 */
private fun historySourceLine(entry: AiHistoryEntry): String =
    listOf(entry.group.shortLabel(), historyKindLabel(entry), historySource(entry.workSlug, entry.episode))
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" · ")

private fun historyMetaLine(entry: AiHistoryEntry): String =
    listOf(
        historySourceLine(entry),
        entry.model.trim(),
        entry.timestamp.trim().take(16).replace('T', ' '),
    ).filter { it.isNotBlank() }.joinToString(" · ")

/** HH:mm from an ISO-ish timestamp; blank when absent. */
internal fun historyClock(timestamp: String): String {
    val t = timestamp.trim()
    return if (t.length >= 16 && (t[10] == 'T' || t[10] == ' ')) t.substring(11, 16) else ""
}

/** 今天 / 昨天 / 9月23日 / 2025年9月23日 — the day eyebrow. */
internal fun historyDayLabel(timestamp: String, today: LocalDate): String {
    val day = runCatching { LocalDate.parse(timestamp.trim().take(10)) }.getOrNull() ?: return "更早"
    return when {
        day == today -> "今天"
        day == today.minusDays(1) -> "昨天"
        day.year == today.year -> "${day.monthValue}月${day.dayOfMonth}日"
        else -> "${day.year}年${day.monthValue}月${day.dayOfMonth}日"
    }
}
