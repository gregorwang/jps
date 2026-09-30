package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.toNotebookEntry
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.SwipeAction
import com.animejapaneselab.nativeapp.ui.design.SwipeActionRow
import com.animejapaneselab.nativeapp.ui.design.UndoBar
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.ui.notebook.NotebookMark
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.words.KnownWords
import kotlinx.coroutines.delay

/**
 * 语法 tab, built like 词汇: each row is the pattern, its meaning and the anime line with the
 * pattern in the work colour. Tap the pattern = hear it, tap the row = 句型卡, swipe right = 斩,
 * swipe left = 收藏.
 */
@Composable
internal fun GrammarPage(
    key: String,
    uiState: LabUiState,
    savedKeys: Set<String>,
    audioBusy: Boolean,
    onSpeak: (String) -> Unit,
    onPlayLine: (ShadowingSentence) -> Unit,
    onAsk: (GrammarPoint) -> Unit,
    onLearn: (GrammarPoint) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    var query by rememberSaveable(key) { mutableStateOf("") }
    var level by rememberSaveable(key) { mutableStateOf(Jlpt.All) }
    var archive by rememberSaveable(key) { mutableStateOf(false) }
    var openId by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var speakingId by remember(key) { mutableStateOf<String?>(null) }
    var undo by remember(key) { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    remember { KnownWords.init(appContext) }
    val known by KnownWords.words.collectAsState()
    val all = uiState.grammar
    val cut = remember(all, known) { all.filter { KnownWords.grammarKey(it.pattern) in known } }
    val grammar = remember(all, known, archive) { if (archive) cut else all.filterNot { KnownWords.grammarKey(it.pattern) in known } }
    val levels = remember(grammar) { grammar.map { Jlpt.normalize(it.difficulty) }.filter { it in Jlpt.Levels }.distinct().sorted() }
    val filtered = remember(grammar, level, query) {
        grammar.filter { (level == Jlpt.All || Jlpt.normalize(it.difficulty) == level) && it.matches(query) }
    }
    LaunchedEffect(cut.isEmpty()) { if (cut.isEmpty()) archive = false }
    LaunchedEffect(audioBusy) { if (!audioBusy) speakingId = null }
    LaunchedEffect(undo) {
        if (undo != null) {
            delay(4000)
            undo = null
        }
    }
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    fun speak(item: GrammarPoint) {
        speakingId = item.id
        onSpeak(item.pattern.trim('〜', '～'))
    }
    fun toggleSave(item: GrammarPoint) {
        val entry = item.toNotebookEntry(workSlug, episode)
        val wasSaved = NotebookRules.key(NotebookKind.Grammar, item.id) in savedKeys
        Notebook.toggle(appContext, entry)
        undo = (if (wasSaved) "已取消收藏「${item.pattern}」" else "已收藏「${item.pattern}」") to { Notebook.toggle(appContext, entry) }
    }
    fun cutPoint(item: GrammarPoint) {
        val k = KnownWords.grammarKey(item.pattern)
        if (archive) {
            KnownWords.restoreKey(appContext, k)
        } else {
            KnownWords.cutKey(appContext, k, NotebookRules.key(NotebookKind.Grammar, item.id))
            undo = "已斩「${item.pattern}」" to { KnownWords.restoreKey(appContext, k) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            item(key = "tools", contentType = "tools") {
                Column(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (levels.size > 1 || cut.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (levels.size > 1) {
                                FilterPill("全部", level == Jlpt.All, { level = Jlpt.All })
                                levels.forEach { lv -> FilterPill(lv, level == lv, { level = lv }) }
                            }
                            if (cut.isNotEmpty()) FilterPill("已斩 ${cut.size}", archive, { archive = !archive })
                        }
                    }
                }
            }
            when {
                all.isEmpty() -> item(key = "empty") { com.animejapaneselab.nativeapp.ui.design.EmptyNote("この話の文法はまだない") }
                grammar.isEmpty() -> item(key = "empty") { com.animejapaneselab.nativeapp.ui.design.EmptyNote("全部斩了", gloss = "斩掉的在「已斩」里") }
                filtered.isEmpty() -> item(key = "empty") { com.animejapaneselab.nativeapp.ui.design.EmptyNote("見つからない", gloss = query) }
            }
            items(filtered.size, key = { "g-${filtered[it].id}" }, contentType = { "grammar" }) { i ->
                val item = filtered[i]
                GrammarRow(
                    item = item,
                    saved = NotebookRules.key(NotebookKind.Grammar, item.id) in savedKeys,
                    archive = archive,
                    speaking = speakingId == item.id && audioBusy,
                    onOpen = { openId = item.id },
                    onSpeak = { speak(item) },
                    onCut = { cutPoint(item) },
                    onToggleSave = { toggleSave(item) },
                )
            }
        }
        undo?.let { (text, restore) ->
            UndoBar(
                text = text,
                onUndo = {
                    restore()
                    undo = null
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    openId?.let { id ->
        if (filtered.none { it.id == id }) {
            openId = null
        } else {
            GrammarCardSheet(
                points = filtered,
                openId = id,
                uiState = uiState,
                savedKeys = savedKeys,
                known = archive,
                speaking = speakingId == id && audioBusy,
                onMove = { openId = it },
                onSpeak = { speak(it) },
                onPlayExample = { point ->
                    speakingId = null
                    val line = uiState.shadowing.firstOrNull { point.sourceLineNo > 0 && it.sourceLineNo == point.sourceLineNo }
                    if (line != null) onPlayLine(line) else onSpeak(point.exampleJa)
                },
                onToggleSave = { toggleSave(it) },
                onCut = {
                    openId = null
                    cutPoint(it)
                },
                onAsk = onAsk,
                onLearn = {
                    openId = null
                    onLearn(it)
                },
                onDismiss = { openId = null },
            )
        }
    }
}

@Composable
private fun GrammarRow(
    item: GrammarPoint,
    saved: Boolean,
    archive: Boolean,
    speaking: Boolean,
    onOpen: () -> Unit,
    onSpeak: () -> Unit,
    onCut: () -> Unit,
    onToggleSave: () -> Unit,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val work = AjlTheme.work
    val mark = remember(item.exampleJa, item.pattern) { patternRange(item.exampleJa, item.pattern) }
    Column(Modifier.fillMaxWidth()) {
        SwipeActionRow(
            enabled = !archive,
            right = SwipeAction(
                label = "斩",
                armedLabel = "松手斩掉",
                background = colors.ink,
                content = colors.onInk,
                dismiss = true,
                icon = { armed -> SealMark(armed = armed, tint = colors.onInk, fill = colors.onInk, size = 36.dp) },
                onCommit = onCut,
            ),
            left = SwipeAction(
                label = if (saved) "取消收藏" else "收藏",
                armedLabel = if (saved) "松手取消" else "松手收藏",
                background = work.accent,
                content = work.onAccent,
                dismiss = false,
                icon = { armed ->
                    Icon(
                        if (armed != saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        tint = work.onAccent,
                        modifier = Modifier.size(28.dp),
                    )
                },
                onCommit = onToggleSave,
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .clickableNoRipple(onClick = onOpen)
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        item.pattern,
                        style = type.jpDisplay.copy(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink,
                        textDecoration = if (speaking) TextDecoration.Underline else null,
                        maxLines = 1,
                        modifier = Modifier
                            .clickableNoRipple(onClick = onSpeak)
                            .semantics { contentDescription = "${item.pattern}，点一下发音" },
                    )
                    if (speaking) VoiceBars(active = true, color = work.accent)
                    val structure = item.enrichment?.structureZh.orEmpty()
                    Text(
                        structure,
                        style = type.metaSmall,
                        color = colors.ink3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (saved) NotebookMark()
                    if (item.count > 1) {
                        Text("${item.count} 例", style = type.metaSmall, color = colors.ink3)
                    } else {
                        Jlpt.normalize(item.difficulty).takeIf { it in Jlpt.Levels }?.let { LevelTag(it, fontSize = 10) }
                    }
                }
                Meaning(item.titleZh)
                if (item.exampleJa.isNotBlank()) {
                    Text(
                        buildAnnotatedString {
                            append("「${item.exampleJa}」")
                            if (mark != null) addStyle(SpanStyle(color = work.accent, fontWeight = FontWeight.SemiBold), mark.first + 1, mark.last + 2)
                        },
                        style = type.jpBody.copy(fontSize = 15.sp, lineHeight = 23.sp),
                        color = colors.ink2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Hairline()
    }
}

/**
 * 句型卡: the pattern large (tap = hear it), its meaning, how it attaches as ink blocks, the anime
 * line with the pattern fading in, then 讲解 / 语气 / 易错. Swipe the top half for the next pattern.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GrammarCardSheet(
    points: List<GrammarPoint>,
    openId: String,
    uiState: LabUiState,
    savedKeys: Set<String>,
    known: Boolean,
    speaking: Boolean,
    onMove: (String) -> Unit,
    onSpeak: (GrammarPoint) -> Unit,
    onPlayExample: (GrammarPoint) -> Unit,
    onToggleSave: (GrammarPoint) -> Unit,
    onCut: (GrammarPoint) -> Unit,
    onAsk: (GrammarPoint) -> Unit,
    onLearn: (GrammarPoint) -> Unit,
    onDismiss: () -> Unit,
) {
    val index = points.indexOfFirst { it.id == openId }.coerceAtLeast(0)
    val item = points[index]
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val work = AjlTheme.work
    val settings = uiState.settings
    val furigana = rememberFuriganaAnnotator(settings)
    val aided = settings.showFurigana || settings.showRomaji
    LaunchedEffect(item.exampleJa, aided) { if (aided && item.exampleJa.isNotBlank()) furigana.request("sentence", listOf(item.exampleJa)) }
    val reading = remember(item.exampleJa, furigana.resultFor(item.exampleJa)) { LineReading.build(item.exampleJa, furigana.resultFor(item.exampleJa)) }
    val mark = remember(item.exampleJa, item.pattern) { patternRange(item.exampleJa, item.pattern) }
    val saved = NotebookRules.key(NotebookKind.Grammar, item.id) in savedKeys

    AjlBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardHeader(
                level = Jlpt.normalize(item.difficulty).takeIf { it in Jlpt.Levels },
                meta = "${index + 1} / ${points.size}",
                saved = saved,
                known = known,
                onToggleSave = { onToggleSave(item) },
                onCut = { onCut(item) },
            )
            SwipeStage(onStep = { step -> onMove(points[(index + step + points.size) % points.size].id) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val big = if (item.pattern.length > 6) 40.sp else 50.sp
                    Text(
                        item.pattern,
                        style = type.jpDisplay.copy(fontSize = big, lineHeight = big * 1.2f, fontWeight = FontWeight.Bold),
                        color = work.accent,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickableNoRipple(onClick = { onSpeak(item) }).semantics { contentDescription = "发音 ${item.pattern}" },
                    )
                    VoiceBars(active = speaking, color = work.accent)
                }
                if (item.titleZh.isNotBlank()) Text(item.titleZh, style = type.title.copy(fontSize = 21.sp, lineHeight = 29.sp), color = colors.ink)
                FormulaChips(formulaParts(item.enrichment?.structureZh.orEmpty()))
            }

            if (item.exampleJa.isNotBlank()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(AjlStroke.Ink, colors.ink, com.animejapaneselab.nativeapp.ui.theme.AjlShape.Panel)
                        .clickableNoRipple(onClick = { onPlayExample(item) })
                        .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("原作里", style = type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = work.accent, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).border(AjlStroke.Hair, colors.line2, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.PlayArrow, contentDescription = "播放原声", tint = colors.ink, modifier = Modifier.size(18.dp)) }
                    }
                    RevealLine(
                        reading = reading,
                        mark = mark,
                        key = item.id,
                        showRuby = settings.showFurigana,
                        showRomaji = settings.showRomaji,
                        style = type.jpBody.copy(fontSize = 19.sp, lineHeight = 30.sp),
                    )
                    if (item.exampleZh.isNotBlank()) Text(item.exampleZh, style = type.body.copy(fontSize = 14.sp, lineHeight = 21.sp), color = colors.ink2)
                }
            }

            val more = item.examples.filter { it.ja != item.exampleJa }.take(7)
            if (more.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("原作里的其他用法", style = type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = work.accent)
                        Text("${item.count} 例", style = type.metaSmall, color = colors.ink3)
                    }
                    more.forEach { ex ->
                        val range = patternRange(ex.ja, item.pattern)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                buildAnnotatedString {
                                    append(ex.ja)
                                    if (range != null) addStyle(SpanStyle(color = work.accent, fontWeight = FontWeight.SemiBold), range.first, range.last + 1)
                                },
                                style = type.jpBody.copy(fontSize = 16.sp, lineHeight = 25.sp),
                                color = colors.ink,
                            )
                            if (ex.zh.isNotBlank()) Text(ex.zh, style = type.body.copy(fontSize = 13.sp, lineHeight = 20.sp), color = colors.ink2)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ColoredNote("讲解", item.explanationZh)
                ColoredNote("语气", item.pragmaticsNote)
                if (!isLabelNote(item.realWorldNote)) ColoredNote("实际", item.realWorldNote)
                ColoredNote("场景", item.enrichment?.usageScenes.orEmpty().joinToString("、"))
                ColoredNote("易错", item.enrichment?.mistakes.orEmpty().joinToString("；"))
            }
            LibraryAiNote(item.aiKey(), uiState)

            if (points.size > 1) {
                Text(
                    "‹ 左右滑换句型 ›",
                    style = type.metaSmall,
                    color = colors.ink3,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton("讲解", { onAsk(item) }, modifier = Modifier.height(52.dp))
                InkButton("练这个句型", { onLearn(item) }, modifier = Modifier.weight(1f))
            }
        }
    }
}
