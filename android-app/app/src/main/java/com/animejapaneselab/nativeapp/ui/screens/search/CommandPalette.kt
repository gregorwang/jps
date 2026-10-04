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
import com.animejapaneselab.nativeapp.data.LevelDict
import com.animejapaneselab.nativeapp.data.RagSceneSuggestion
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCard
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCards
import com.animejapaneselab.nativeapp.ui.screens.library.DictTarget
import com.animejapaneselab.nativeapp.ui.screens.library.partOfSpeechLabel
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import com.animejapaneselab.nativeapp.data.RagSearchSource
import com.animejapaneselab.nativeapp.data.SubtitleLine
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.screens.library.parseSpokenLine
import com.animejapaneselab.nativeapp.ui.search.SceneRules
import com.animejapaneselab.nativeapp.ui.search.SceneSearch
import com.animejapaneselab.nativeapp.ui.theme.ProvideWorkTheme
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

/** Scene descriptions to try in the vector search (fixed examples, not AI picks). */
private val Suggestions = listOf(
    "傲娇地拒绝别人",
    "ありがとう的场景",
    "第一次见面的自我介绍",
    "下定决心的台词",
    "被吓到时的惊呼",
)

/**
 * 搜索 · 命令面板. An overlay above the current page: 28% scrim + a hairline tool panel that
 * scales 0.98→1 and fades in over 160ms (closes in 120ms, no translation). No cancel button and
 * no keyboard hints — tap the scrim or go back to close. [scope] decides what it looks in:
 * 辞書 entries and knowledge cards match while typing (a hit opens the card itself); on the IME
 * action 今日 also runs the scene search (`SceneSearch`) and shows its first two hits, the rest
 * is on the 场景 page.
 */
@Composable
fun CommandPalette(
    visible: Boolean,
    scope: SearchScope,
    uiState: LabUiState,
    onDismiss: () -> Unit,
    onOpenScenes: () -> Unit,
    onOpenEntry: (DictTarget) -> Unit,
    onOpenKnowledge: (KnowledgeCard) -> Unit,
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
            // A fresh panel per scope: what was typed in 辞書 does not follow you to 知識.
            key(scope) {
                PalettePanel(
                    scope = scope,
                    uiState = uiState,
                    onOpenScenes = onOpenScenes,
                    onOpenEntry = onOpenEntry,
                    onOpenKnowledge = onOpenKnowledge,
                )
            }
        }
    }
}

@Composable
private fun PalettePanel(
    scope: SearchScope,
    uiState: LabUiState,
    onOpenScenes: () -> Unit,
    onOpenEntry: (DictTarget) -> Unit,
    onOpenKnowledge: (KnowledgeCard) -> Unit,
) {
    val colors = AjlTheme.colors
    val context = LocalContext.current.applicationContext
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scenes by SceneSearch.state.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    // Scenes show only once this query was sent with the IME action (each search is two model calls).
    var asked by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // Local indexes, built once off the main thread: the whole dictionary and every knowledge card.
    val dictIndex by produceState<List<Indexed<DictTarget>>>(emptyList(), scope, uiState.vocab, uiState.grammar) {
        if (!scope.dict) return@produceState
        value = withContext(Dispatchers.Default) {
            val dict = LevelDict.load(context)
            indexDict(dict.vocab + dict.freqVocab + uiState.vocab, dict.grammar + dict.freqGrammar + uiState.grammar)
        }
    }
    val knowIndex by produceState<List<Indexed<KnowledgeCard>>>(emptyList(), scope) {
        if (!scope.know) return@produceState
        value = withContext(Dispatchers.Default) { indexKnowledge(KnowledgeCards.all(context)) }
    }
    val entryLimit = if (scope == SearchScope.Dict) 40 else 5
    val knowLimit = if (scope == SearchScope.Knowledge) 40 else 3
    val entryHits by produceState(emptyList<DictTarget>(), query, dictIndex) {
        value = if (query.isBlank()) emptyList() else withContext(Dispatchers.Default) { rank(dictIndex, query, entryLimit) }
    }
    val knowHits by produceState(emptyList<KnowledgeCard>(), query, knowIndex) {
        value = if (query.isBlank()) emptyList() else withContext(Dispatchers.Default) { rank(knowIndex, query, knowLimit) }
    }

    val submit: (String) -> Unit = submit@{ raw ->
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return@submit
        query = trimmed
        focusManager.clearFocus()
        if (!scope.scenes) return@submit
        asked = trimmed
        SceneSearch.search(context, trimmed)
    }
    val q = query.trim()
    val sceneState = scenes.takeIf { scope.scenes && asked.isNotBlank() && it.query == asked && q == asked }

    Column(
        Modifier
            .fillMaxWidth()
            .semantics { paneTitle = scope.title }
            .background(colors.surface, AjlShape.Tool)
            .border(AjlStroke.Hair, colors.line2, AjlShape.Tool)
            // Swallow taps so they never reach the scrim.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
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
                    Text(scope.placeholder, style = AjlTheme.type.body, color = colors.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        .semantics { contentDescription = scope.title },
                )
            }
            if (sceneState?.loading == true) {
                LoadingDots(delayMillis = 0, modifier = Modifier.padding(end = 10.dp))
            } else if (query.isNotEmpty()) {
                IconButton44(Icons.Rounded.Close, "清空", onClick = { query = "" }, tint = colors.ink3, iconSize = 18.dp)
            }
        }
        Hairline()
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 8.dp),
        ) {
            if (entryHits.isNotEmpty()) {
                val words = entryHits.filterIsInstance<DictTarget.Word>()
                val patterns = entryHits.filterIsInstance<DictTarget.Pattern>()
                if (words.isNotEmpty()) {
                    sectionLabel("words", "词汇")
                    items(words, key = { "w-${it.item.id}" }) { hit ->
                        val v = hit.item
                        EntryHitRow(
                            head = v.surface,
                            gloss = v.meaningZh,
                            meta = listOf(partOfSpeechLabel(v.partOfSpeech), v.level).filter { it.isNotBlank() }.joinToString(" · "),
                            query = q,
                            onClick = { onOpenEntry(hit) },
                        )
                    }
                }
                if (patterns.isNotEmpty()) {
                    sectionLabel("patterns", "语法")
                    items(patterns, key = { "g-${it.item.id}" }) { hit ->
                        val g = hit.item
                        EntryHitRow(head = g.pattern, gloss = g.titleZh, meta = g.difficulty, query = q, onClick = { onOpenEntry(hit) })
                    }
                }
            }
            if (knowHits.isNotEmpty()) {
                sectionLabel("know", "知识点")
                items(knowHits, key = { "k-${it.id}" }) { card ->
                    EntryHitRow(
                        head = card.title,
                        gloss = "",
                        meta = card.deckShort.ifBlank { card.deckTitle },
                        query = q,
                        onClick = { onOpenKnowledge(card) },
                    )
                }
            }
            if (!scope.scenes) {
                if (q.isNotEmpty() && entryHits.isEmpty() && knowHits.isEmpty()) {
                    item(key = "none") {
                        Text(
                            "見つからない",
                            style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp),
                            color = colors.ink3,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                        )
                    }
                }
                return@LazyColumn
            }
            when {
                q.isEmpty() -> {
                    sectionLabel("suggest", "按场景找")
                    items(Suggestions, key = { "suggest-$it" }) { suggestion ->
                        ActionRow(suggestion, onClick = {
                            SceneSearch.search(context, suggestion)
                            onOpenScenes()
                        })
                    }
                }
                sceneState == null -> item(key = "find-lines") { ActionRow("在原作里按意思找「$q」", onClick = { submit(q) }) }
                sceneState.loading -> item(key = "loading") { Box(Modifier.fillMaxWidth().height(56.dp)) }
                sceneState.error != null -> item(key = "failed") {
                    ActionRow(sceneState.error, onClick = { SceneSearch.retry(context) })
                }
                else -> {
                    val result = sceneState.result ?: return@LazyColumn
                    val shown = SceneRules.shown(result)
                    sectionLabel("scenes", "原作里的场景")
                    if (shown.isEmpty()) {
                        item(key = "scenes-none") { ActionRow("原作里没有很像的 · 看常见说法", onClick = onOpenScenes) }
                    } else {
                        items(shown.take(2), key = { "scene-${it.id}" }) { source ->
                            val line = SceneRules.hitLine(source) ?: return@items
                            ProvideWorkTheme(source.workSlug) {
                                SceneHitCard(source, line, onClick = {
                                    SceneSearch.focus(source.id)
                                    onOpenScenes()
                                })
                            }
                        }
                        item(key = "scenes-all") { ActionRow("全部场景 · ${shown.size} 个", onClick = onOpenScenes) }
                    }
                }
            }
        }
    }
}

/** A scene hit in the palette: where it is from, the line with its key words marked, the AI's Chinese. */
@Composable
private fun SceneHitCard(source: RagSearchSource, line: SubtitleLine, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val text = parseSpokenLine(line.jaText).text
    val mark = source.match?.mark.orEmpty()
    MangaPanel(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = "打开这个场景", onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "${WorkIdentity.displayName(source.workSlug, source.workSlug)} 第${source.episode}話 · ${SceneRules.clock(line.startTime)}",
                style = AjlTheme.type.meta,
                color = accent,
            )
            Text(
                highlight(text, mark, SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)),
                style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 25.sp),
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            source.match?.zh?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = AjlTheme.type.caption, color = colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
private fun EntryHitRow(head: String, gloss: String, meta: String, query: String, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val accent = AjlTheme.work.accent
    val headText = remember(head, query, accent) { highlight(head, query, SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (pressed) colors.sunken else colors.surface, RoundedCornerShape(8.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClickLabel = "打开", onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(headText, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 24.sp), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = if (gloss.isBlank()) Modifier.weight(1f) else Modifier)
        if (gloss.isNotBlank()) Text(
            gloss,
            style = AjlTheme.type.body.copy(fontSize = 14.sp),
            color = colors.ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (meta.isNotBlank()) Text(meta, style = AjlTheme.type.meta, color = colors.ink3, maxLines = 1)
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
