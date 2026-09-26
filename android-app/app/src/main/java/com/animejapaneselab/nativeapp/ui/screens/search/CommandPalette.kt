package com.animejapaneselab.nativeapp.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RagSearchResult
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SearchRequest(val query: String, val workSlug: String, val seq: Int)

private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Failed(val message: String) : SearchState
    data class Ready(val result: RagSearchResult) : SearchState
}

private val Suggestions = listOf(
    "傲娇地拒绝别人",
    "ありがとう的场景",
    "第一次见面的自我介绍",
    "下定决心的台词",
    "被吓到时的惊呼",
)

/**
 * 全局搜索 · 命令面板. An overlay above the current page: 28% scrim + a hairline tool panel that
 * scales 0.98→1 and fades in over 160ms (closes in 120ms, no translation). No cancel button and
 * no keyboard hints — tap the scrim or go back to close. Search is the v2 RAG subtitle search;
 * a hit jumps to the line in the subtitle browser.
 */
@Composable
fun CommandPalette(
    visible: Boolean,
    uiState: LabUiState,
    onDismiss: () -> Unit,
    onOpenSubtitleLine: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = rememberReducedMotion()
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = if (reduced) EnterTransition.None else fadeIn(tween(MotionTokens.Dur.PaletteOpen, easing = MotionTokens.Ease.Decelerate)),
            exit = if (reduced) ExitTransition.None else fadeOut(tween(MotionTokens.Dur.PaletteClose, easing = MotionTokens.Ease.Accelerate)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(AjlTheme.colors.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "关闭搜索",
                        onClick = onDismiss,
                    ),
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = if (reduced) {
                EnterTransition.None
            } else {
                scaleIn(
                    tween(MotionTokens.Dur.PaletteOpen, easing = MotionTokens.Ease.Decelerate),
                    initialScale = 0.98f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                ) + fadeIn(tween(MotionTokens.Dur.PaletteOpen, easing = MotionTokens.Ease.Decelerate))
            },
            exit = if (reduced) {
                ExitTransition.None
            } else {
                scaleOut(
                    tween(MotionTokens.Dur.PaletteClose, easing = MotionTokens.Ease.Accelerate),
                    targetScale = 0.98f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                ) + fadeOut(tween(MotionTokens.Dur.PaletteClose, easing = MotionTokens.Ease.Accelerate))
            },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 16.dp),
        ) {
            PalettePanel(uiState = uiState, onOpenSubtitleLine = onOpenSubtitleLine, onOpenLibrary = onOpenLibrary)
        }
    }
}

@Composable
private fun PalettePanel(
    uiState: LabUiState,
    onOpenSubtitleLine: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val colors = AjlTheme.colors
    val context = LocalContext.current.applicationContext
    val store = remember(context) { LocalLabStore(context) }
    val apiBaseUrl = uiState.settings.apiBaseUrl
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var query by rememberSaveable { mutableStateOf("") }
    var workSlug by rememberSaveable { mutableStateOf(uiState.selection.workSlug) }
    var seq by remember { mutableIntStateOf(0) }
    var request by remember { mutableStateOf<SearchRequest?>(null) }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val active = request
    LaunchedEffect(apiBaseUrl, active) {
        if (active == null) return@LaunchedEffect
        state = SearchState.Loading
        val outcome = runCatching {
            withContext(Dispatchers.IO) {
                RemoteLabClient(apiBaseUrl, store.readSessionCookie()).searchSubtitles(
                    query = active.query,
                    workSlug = active.workSlug,
                    deviceId = store.deviceId(),
                    episode = null,
                    topK = 8,
                )
            }
        }
        state = outcome.fold(
            onSuccess = { SearchState.Ready(it) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                SearchState.Failed(error.message.orEmpty().ifBlank { "搜索失败" })
            },
        )
    }

    // 词汇 / 语法 of the current episode match while typing; subtitles wait for the IME search.
    val localHits = remember(query, uiState.vocab, uiState.grammar) {
        localEntryHits(query, uiState.vocab, uiState.grammar)
    }

    val submit: (String) -> Unit = submit@{ raw ->
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return@submit
        query = trimmed
        focusManager.clearFocus()
        seq += 1
        request = SearchRequest(trimmed, workSlug, seq)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .semantics { paneTitle = "全局搜索" }
            .background(colors.surface, AjlShape.Tool)
            .border(AjlStroke.Hair, colors.line2, AjlShape.Tool)
            // Swallow taps so they never reach the scrim.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        // Query row
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.ink3, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("描述一个场景，或输入台词", style = AjlTheme.type.body.copy(fontSize = 16.sp), color = colors.faint, maxLines = 1)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 24.sp, color = colors.ink),
                    cursorBrush = SolidColor(colors.ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit(query) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "搜索字幕" },
                )
            }
            when (val s = state) {
                SearchState.Loading -> LoadingDots(delayMillis = 0, modifier = Modifier.padding(end = 10.dp))
                is SearchState.Ready -> Text(
                    "${s.result.sources.sumOf { it.lines.size.coerceAtLeast(1) }} 条",
                    style = AjlTheme.type.meta,
                    color = colors.ink3,
                    modifier = Modifier.padding(end = 10.dp),
                )
                else -> if (query.isNotEmpty()) {
                    IconButton44(Icons.Rounded.Close, "清空", onClick = { query = "" }, tint = colors.ink3, iconSize = 18.dp)
                }
            }
        }
        Hairline()
        if (uiState.works.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                items(uiState.works, key = { it.slug }) { work ->
                    FilterPill(
                        text = WorkIdentity.displayName(work.slug, work.displayName.ifBlank { work.slug }),
                        selected = work.slug == workSlug,
                        onClick = {
                            if (work.slug != workSlug) {
                                workSlug = work.slug
                                val pending = query.trim()
                                if (pending.isNotBlank() && request != null) {
                                    seq += 1
                                    request = SearchRequest(pending, work.slug, seq)
                                }
                            }
                        },
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 8.dp),
        ) {
            if (localHits.isNotEmpty()) {
                sectionLabel("entries", "词汇")
                items(localHits, key = { "entry-${it.key}" }) { hit ->
                    EntryHitRow(hit = hit, query = query.trim(), onClick = onOpenLibrary)
                }
            }
            when (val s = state) {
                SearchState.Idle -> suggestionSection("试试", onPick = submit)
                SearchState.Loading -> item(key = "loading") { Box(Modifier.fillMaxWidth().height(64.dp)) }
                is SearchState.Failed -> {
                    item(key = "failed") {
                        Text(
                            s.message,
                            style = AjlTheme.type.caption,
                            color = colors.bad,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        )
                    }
                    item(key = "retry") {
                        ActionRow("重新搜索", onClick = {
                            val pending = request
                            if (pending != null) {
                                seq += 1
                                request = pending.copy(seq = seq)
                            }
                        })
                    }
                }
                is SearchState.Ready -> readySections(
                    result = s.result,
                    query = s.result.query.ifBlank { query },
                    onOpen = onOpenSubtitleLine,
                    onPick = submit,
                )
            }
        }
    }
}

private fun LazyListScope.sectionLabel(key: String, text: String) {
    item(key = "label-$key") {
        Text(
            text,
            style = AjlTheme.type.meta.copy(letterSpacing = 0.6.sp),
            color = AjlTheme.colors.ink3,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
        )
    }
}

private fun LazyListScope.suggestionSection(label: String, onPick: (String) -> Unit) {
    sectionLabel("suggest", label)
    items(Suggestions, key = { "suggest-$it" }) { suggestion ->
        ActionRow(suggestion, onClick = { onPick(suggestion) })
    }
}

private fun LazyListScope.readySections(
    result: RagSearchResult,
    query: String,
    onOpen: (String, Int, Int) -> Unit,
    onPick: (String) -> Unit,
) {
    if (result.sources.isEmpty()) {
        item(key = "empty") {
            Text(
                "没搜到相关台词",
                style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp),
                color = AjlTheme.colors.ink3,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            )
        }
        suggestionSection("换个说法", onPick)
        return
    }
    val analysis = result.analysis
    if (analysis != null && (analysis.title.isNotBlank() || analysis.summary.isNotBlank())) {
        sectionLabel("analysis", "AI 解读")
        item(key = "analysis") {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (analysis.title.isNotBlank()) {
                    Text(analysis.title, style = AjlTheme.type.label.copy(fontSize = 15.sp), color = AjlTheme.colors.ink)
                }
                if (analysis.summary.isNotBlank()) {
                    Text(analysis.summary, style = AjlTheme.type.caption.copy(lineHeight = 20.sp), color = AjlTheme.colors.ink2)
                }
                analysis.bullets.filter { it.isNotBlank() }.forEach { bullet ->
                    Text("・$bullet", style = AjlTheme.type.caption.copy(lineHeight = 20.sp), color = AjlTheme.colors.ink2)
                }
            }
        }
    }
    sectionLabel("lines", "字幕")
    result.sources.forEachIndexed { index, source ->
        val work = WorkIdentity.displayName(source.workSlug, source.workSlug)
        val ep = "$work ${source.episode.toString().padStart(2, '0')}"
        if (source.lines.isEmpty()) {
            item(key = "src-$index-${source.id}") {
                LineHit(
                    ja = source.text,
                    zh = "",
                    meta = listOf(ep, source.startTime.trim().take(8).removePrefix("00:")).filter { it.isNotBlank() }.joinToString(" · "),
                    query = query,
                    onClick = { onOpen(source.workSlug, source.episode, 0) },
                )
            }
        } else {
            source.lines.forEachIndexed { lineIndex, line ->
                item(key = "src-$index-${source.id}-$lineIndex") {
                    LineHit(
                        ja = line.jaText,
                        zh = line.zhText,
                        meta = listOf(ep, line.startTime.trim().take(8).removePrefix("00:")).filter { it.isNotBlank() }.joinToString(" · "),
                        query = query,
                        onClick = { onOpen(source.workSlug, source.episode, line.lineNo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LineHit(ja: String, zh: String, meta: String, query: String, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val accent = AjlTheme.work.accent
    val text = remember(ja, query, accent) { highlight(ja, query, SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(if (pressed) colors.sunken else colors.surface, RoundedCornerShape(8.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClickLabel = "在字幕里定位", onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 22.sp), color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (zh.isNotBlank()) {
                Text(zh, style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 16.sp), color = colors.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(meta, style = AjlTheme.type.meta, color = colors.ink3, maxLines = 1)
    }
}

internal data class EntryHit(val key: String, val head: String, val gloss: String, val meta: String)

/** Literal matches in the loaded 词汇 / 语法 (surface, reading, pattern or Chinese gloss). */
internal fun localEntryHits(
    query: String,
    vocab: List<VocabItem>,
    grammar: List<GrammarPoint>,
    limit: Int = 4,
): List<EntryHit> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    val words = vocab.asSequence()
        .filter { it.surface.contains(q) || it.reading.contains(q) || it.meaningZh.contains(q) }
        .map { v ->
            EntryHit(
                key = "v-${v.id}",
                head = v.surface,
                gloss = v.meaningZh,
                meta = listOf(v.partOfSpeech, v.level).filter { it.isNotBlank() }.joinToString(" · "),
            )
        }
    val patterns = grammar.asSequence()
        .filter { it.pattern.contains(q) || it.titleZh.contains(q) }
        .map { g ->
            EntryHit(
                key = "g-${g.id}",
                head = g.pattern,
                gloss = g.titleZh,
                meta = listOf("文法", g.difficulty).filter { it.isNotBlank() }.joinToString(" · "),
            )
        }
    return (words + patterns).take(limit).toList()
}

@Composable
private fun EntryHitRow(hit: EntryHit, query: String, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val accent = AjlTheme.work.accent
    val head = remember(hit.head, query, accent) { highlight(hit.head, query, SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (pressed) colors.sunken else colors.surface, RoundedCornerShape(8.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClickLabel = "在辞書里查看", onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(head, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 24.sp), color = colors.ink, maxLines = 1)
        Text(
            hit.gloss,
            style = AjlTheme.type.body.copy(fontSize = 14.sp),
            color = colors.ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (hit.meta.isNotBlank()) Text(hit.meta, style = AjlTheme.type.meta, color = colors.ink3, maxLines = 1)
    }
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (pressed) colors.sunken else colors.surface, RoundedCornerShape(8.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = colors.ink3, modifier = Modifier.size(16.dp))
        Text(text, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Marks every literal occurrence of [query] in [text] (semantic hits often have none). */
internal fun highlight(text: String, query: String, style: SpanStyle): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty() || !text.contains(q)) return AnnotatedString(text)
    return buildAnnotatedString {
        var start = 0
        while (true) {
            val i = text.indexOf(q, start)
            if (i < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, i))
            withStyle(style) { append(q) }
            start = i + q.length
        }
    }
}
