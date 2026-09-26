package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.Conjugator
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.toNotebookEntry
import com.animejapaneselab.nativeapp.ui.notebook.NotebookMark
import com.animejapaneselab.nativeapp.ui.notebook.NotebookPage
import com.animejapaneselab.nativeapp.ui.notebook.NotebookToggleButton
import com.animejapaneselab.nativeapp.ui.notebook.rememberNotebookEntries
import com.animejapaneselab.nativeapp.data.LessonTarget
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.data.promptAudioForSentence
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.Avatar
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.reading.DeepDiveTarget
import com.animejapaneselab.nativeapp.ui.reading.rememberCharacterProfile
import com.animejapaneselab.nativeapp.ui.reading.rememberSentenceDeepDive
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.launch

/** 辞書 tab: 词汇 / 语法 / 台词, dictionary entries, 五十音 index. */
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
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onViewSource: (workSlug: String, episode: Int, lineNo: Int) -> Unit = { _, _, _ -> },
) {
    val colors = AjlTheme.colors
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val audio = rememberLessonAudioController()
    val deepDive = rememberSentenceDeepDive(uiState.settings)
    val characterProfile = rememberCharacterProfile(uiState.settings, workSlug)
    val episodeLabel = uiState.focus.episodeLabel.ifBlank { episodeTitle(episode) }
    val notebook = rememberNotebookEntries()
    val savedKeys = remember(notebook) { notebook.mapTo(HashSet()) { it.key } }

    val tabs = listOf(
        DictTab("词汇", uiState.vocab.size),
        DictTab("语法", uiState.grammar.size),
        DictTab("台词", uiState.shadowing.size),
        DictTab("栞", notebook.size),
    )

    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(
            nav = TopBarNav.None,
            title = "辞書",
            actions = {
                IconButton44(Icons.Rounded.Subtitles, "字幕", onOpenSubtitles)
                IconButton44(Icons.Rounded.Search, "搜索", onOpenSearch)
            },
        )
        DictTabs(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 20.dp),
            trailing = { if (selectedTab != NotebookTab) EpisodeChip(episodeTitle(episode), onClick = { pickerOpen = true }) },
        )
        Box(Modifier.fillMaxWidth().weight(1f)) {
            val scope = "$workSlug#$episode#$selectedTab"
            when (selectedTab) {
                0 -> VocabPage(
                    key = scope,
                    uiState = uiState,
                    savedKeys = savedKeys,
                    onSpeak = { audio.speakText(it, uiState.settings.ttsWorkerUrl) },
                    onAsk = { item -> onAskAi(item.aiKey(), "vocab", item.surface, item.aiContext(episodeLabel)) },
                    onLearn = { onTargetLesson(LessonTarget.Vocab(it.id)) },
                )
                1 -> GrammarPage(
                    key = scope,
                    uiState = uiState,
                    savedKeys = savedKeys,
                    onAsk = { item -> onAskAi(item.aiKey(), "grammar", item.pattern, item.aiContext(episodeLabel)) },
                    onLearn = { onTargetLesson(LessonTarget.Grammar(it.id)) },
                )
                NotebookTab -> NotebookPage(
                    ttsWorkerUrl = uiState.settings.ttsWorkerUrl,
                    onViewSource = onViewSource,
                )
                else -> LinesPage(
                    key = scope,
                    uiState = uiState,
                    savedKeys = savedKeys,
                    onPlay = { line -> audio.play(promptAudioForSentence(workSlug, line, autoPlay = false), uiState.settings.ttsWorkerUrl) },
                    onDeepDive = { line ->
                        deepDive.request(
                            DeepDiveTarget(
                                workSlug = workSlug,
                                episode = episode,
                                lineNo = line.sourceLineNo,
                                jaText = line.ja,
                                zhText = line.meaningZh,
                            ),
                        )
                    },
                    onAsk = { item -> onAskAi(item.aiKey(), "sentence", item.ja, item.aiContext(episodeLabel)) },
                    onLearn = { onTargetLesson(LessonTarget.Sentence(it.id)) },
                )
            }
        }
    }

    if (pickerOpen) {
        EpisodePickerSheet(
            works = uiState.works,
            episodes = uiState.episodes,
            selectedWork = workSlug,
            selectedEpisode = episode,
            onWorkSelected = onWorkSelected,
            onEpisodeSelected = onEpisodeSelected,
            onDismiss = { pickerOpen = false },
            actions = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LearnEpisodeButton(episode) {
                        pickerOpen = false
                        when (selectedTab) {
                            0 -> onStartModeLesson(LessonMode.Vocab)
                            1 -> onStartModeLesson(LessonMode.Grammar)
                            else -> onStartLesson()
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlineButton("読空気", {
                            pickerOpen = false
                            onStartReadAir()
                        }, modifier = Modifier.weight(1f))
                        OutlineButton("登場人物", {
                            pickerOpen = false
                            characterProfile.open()
                        }, modifier = Modifier.weight(1f))
                    }
                }
            },
        )
    }
    DeepDiveSheet(deepDive)
    CharacterSheet(characterProfile)
}

private const val NotebookTab = 3

// ---------------------------------------------------------------------------
// Row model
// ---------------------------------------------------------------------------

private sealed interface DictRow {
    val key: String

    data object Tools : DictRow {
        override val key = "tools"
    }

    data class Empty(val text: String, val gloss: String?) : DictRow {
        override val key = "empty"
    }

    data class Head(val row: String) : DictRow {
        override val key: String get() = "head-$row"
    }

    data class Entry<T>(val value: T, override val key: String) : DictRow
}

private fun <T> buildDictRows(
    groups: List<DictGroup<T>>,
    keyOf: (T) -> String,
    empty: DictRow.Empty?,
): Pair<List<DictRow>, Map<String, Int>> {
    val rows = mutableListOf<DictRow>(DictRow.Tools)
    val starts = linkedMapOf<String, Int>()
    if (empty != null) {
        rows += empty
    } else {
        groups.forEach { group ->
            starts[group.row] = rows.size
            rows += DictRow.Head(group.row)
            group.entries.forEach { rows += DictRow.Entry(it, keyOf(it)) }
        }
    }
    return rows to starts
}

/** The 五十音 row whose section is at the top of the viewport. */
@Composable
private fun rememberCurrentRow(listState: LazyListState, starts: Map<String, Int>): String? {
    val current by remember(listState, starts) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            starts.entries.lastOrNull { it.value <= first }?.key ?: starts.keys.firstOrNull()
        }
    }
    return current
}

/** List + rail frame shared by 词汇 and 语法. */
@Composable
private fun IndexedList(
    key: String,
    rows: List<DictRow>,
    starts: Map<String, Int>,
    tools: @Composable () -> Unit,
    entry: @Composable (DictRow.Entry<*>) -> Unit,
) {
    val listState = remember(key) { LazyListState() }
    val coroutine = rememberCoroutineScope()
    val current = rememberCurrentRow(listState, starts)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = if (starts.isEmpty()) 20.dp else 44.dp, bottom = 32.dp),
        ) {
            items(rows, key = { it.key }, contentType = { it::class.simpleName }) { row ->
                when (row) {
                    DictRow.Tools -> tools()
                    is DictRow.Empty -> EmptyNote(row.text, gloss = row.gloss)
                    is DictRow.Head -> RowHead(row.row)
                    is DictRow.Entry<*> -> entry(row)
                }
            }
        }
        if (starts.size > 1) {
            KanaIndexRail(
                present = starts.keys,
                current = current,
                onJump = { target -> starts[target]?.let { index -> coroutine.launch { listState.scrollToItem(index) } } },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 8.dp),
            )
        }
    }
}

/** あ / か … section head: serif kana and a hairline, the way a paper dictionary breaks rows. */
@Composable
private fun RowHead(row: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(row, style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp, lineHeight = 20.sp), color = AjlTheme.work.accent)
        Hairline(Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------
// 词汇
// ---------------------------------------------------------------------------

@Composable
private fun VocabPage(
    key: String,
    uiState: LabUiState,
    savedKeys: Set<String>,
    onSpeak: (String) -> Unit,
    onAsk: (VocabItem) -> Unit,
    onLearn: (VocabItem) -> Unit,
) {
    var query by rememberSaveable(key) { mutableStateOf("") }
    var level by rememberSaveable(key) { mutableStateOf(Jlpt.All) }
    var expanded by rememberExpandedKey(key)
    val vocab = uiState.vocab
    val buckets = remember(vocab) { levelBuckets(vocab) }
    val filtered = remember(vocab, level, query) { filterByLevel(vocab, level).filter { it.matches(query) } }
    val groups = remember(filtered) { groupByGojuon(filtered) { it.indexReading() } }
    val empty = when {
        vocab.isEmpty() -> DictRow.Empty("この話の単語はまだない", null)
        filtered.isEmpty() -> DictRow.Empty("見つからない", query.takeIf { it.isNotBlank() })
        else -> null
    }
    val (rows, starts) = remember(groups, empty) { buildDictRows(groups, { "v-${it.id}" }, empty) }
    val examples = remember(vocab, uiState.shadowing) {
        vocab.associate { it.id to findExampleLine(it.surface, it.reading, uiState.shadowing) }
    }
    IndexedList(
        key = key,
        rows = rows,
        starts = starts,
        tools = {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FindField(query, { query = it }, placeholder = "引く · 词、读音、中文")
                if (buckets.size > 2) LevelPills(buckets, level) { level = it }
            }
        },
        entry = { row ->
            val item = row.value as VocabItem
            VocabEntry(
                item = item,
                example = examples[item.id],
                saved = NotebookRules.key(NotebookKind.Vocab, item.id) in savedKeys,
                expanded = expanded == item.id,
                onToggle = { expanded = if (expanded == item.id) null else item.id },
                onSpeak = { onSpeak(item.surface) },
                onSpeakText = onSpeak,
                onAsk = { onAsk(item) },
                onLearn = { onLearn(item) },
                uiState = uiState,
            )
        },
    )
}

/**
 * 級 ruler: one hairline strip split evenly (全部 · N5 … N1), so it never scrolls sideways or
 * crowds the kana rail. The selected level sits on the work-soft tint in work colour.
 */
@Composable
private fun LevelPills(buckets: List<LevelBucket>, selected: String, onSelect: (String) -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .border(AjlStroke.Hair, colors.line2, shape),
    ) {
        buckets.forEachIndexed { index, bucket ->
            val on = bucket.key == selected
            if (index > 0) Box(Modifier.fillMaxHeight().width(AjlStroke.Hair).background(colors.line))
            Box(
                Modifier
                    .weight(if (bucket.key == Jlpt.All) 1.25f else 1f)
                    .fillMaxHeight()
                    .background(if (on) work.soft else colors.surface)
                    .clickableNoRipple(onClick = { onSelect(bucket.key) })
                    .semantics { contentDescription = "${bucket.label} ${bucket.count} 个" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    buildAnnotatedString {
                        append(bucket.label)
                        withStyle(SpanStyle(fontSize = 9.sp, baselineShift = BaselineShift(0.45f), color = if (on) work.accent else colors.ink3)) {
                            append(" ${bucket.count}")
                        }
                    },
                    style = AjlTheme.type.meta.copy(fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (on) work.accent else colors.ink2,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VocabEntry(
    item: VocabItem,
    example: ShadowingSentence?,
    saved: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSpeak: () -> Unit,
    onSpeakText: (String) -> Unit,
    onAsk: () -> Unit,
    onLearn: () -> Unit,
    uiState: LabUiState,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val accent = AjlTheme.work.accent
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clickableNoRipple(onToggle).padding(top = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Headword + plain reading; the level sits right as a small work-colour tag.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    item.surface,
                    style = type.jpDisplay.copy(fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                    modifier = Modifier.alignByBaseline(),
                )
                if (item.reading.isNotBlank() && item.reading != item.surface) {
                    Text(item.reading, style = type.jpBody.copy(fontSize = 14.sp), color = colors.ink3, modifier = Modifier.alignByBaseline())
                }
                Spacer(Modifier.weight(1f))
                if (saved) NotebookMark(Modifier.alignByBaseline())
                val level = Jlpt.normalize(item.level).takeIf { it in Jlpt.Levels }
                if (level != null) {
                    Text(
                        level,
                        style = type.metaSmall.copy(fontSize = 10.sp),
                        color = accent,
                        modifier = Modifier
                            .alignByBaseline()
                            .background(AjlTheme.work.soft, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            // Part of speech in plain Chinese (名词 / 五段动词 / な形容词) ahead of the meaning.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                val pos = partOfSpeechLabel(item.partOfSpeech)
                if (pos.isNotEmpty()) {
                    Text(
                        pos,
                        style = type.caption.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = colors.ink2,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
                Box(Modifier.weight(1f)) { Meaning(item.meaningZh) }
            }
            if (example != null) ExampleLine(parseSpokenLine(example.ja).text, exampleSource(example))
        }
        if (expanded) {
            Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.romanization.isNotBlank()) Text(item.romanization, style = type.meta, color = colors.ink3)
                if (item.realWorldNote.isNotBlank()) Text(item.realWorldNote, style = type.body, color = colors.ink2)
                val conjugation = remember(item.surface, item.reading, item.partOfSpeech) {
                    Conjugator.tableFor(item.surface, item.reading, item.partOfSpeech)
                }
                ConjugationGrid(conjugation, onSpeak = onSpeakText)
                EnrichmentNote(item.enrichment)
                LinguisticNote(item.linguistic)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton("発音", onSpeak, compact = true, leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp)
                    OutlineButton("講解", onAsk, compact = true)
                    OutlineButton("この語を練習", onLearn, compact = true)
                    NotebookToggleButton(
                        entry = { item.toNotebookEntry(uiState.selection.workSlug, uiState.selection.episode, example) },
                        saved = saved,
                    )
                }
                LibraryAiNote(item.aiKey(), uiState)
            }
        }
        Hairline()
    }
}

/** ① meaning — the work-colour circled number, as in a paper dictionary. */
@Composable
private fun Meaning(text: String) {
    if (text.isBlank()) return
    val accent = AjlTheme.work.accent
    val numbered = text.trim().firstOrNull() in '①'..'⑳'
    Text(
        buildAnnotatedString {
            if (!numbered) {
                withStyle(SpanStyle(color = accent)) { append("① ") }
            }
            append(text.trim())
        },
        style = AjlTheme.type.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
        color = AjlTheme.colors.ink,
    )
}

/** 「example」 with its mono source on the right (L12 憂). */
@Composable
private fun ExampleLine(text: String, source: String) {
    if (text.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "「$text」",
            style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 22.sp),
            color = AjlTheme.colors.ink2,
            modifier = Modifier.weight(1f),
        )
        if (source.isNotBlank()) {
            Text(source, style = AjlTheme.type.metaSmall.copy(lineHeight = 22.sp), color = AjlTheme.colors.ink3)
        }
    }
}

// ---------------------------------------------------------------------------
// 语法
// ---------------------------------------------------------------------------

@Composable
private fun GrammarPage(
    key: String,
    uiState: LabUiState,
    savedKeys: Set<String>,
    onAsk: (GrammarPoint) -> Unit,
    onLearn: (GrammarPoint) -> Unit,
) {
    var query by rememberSaveable(key) { mutableStateOf("") }
    var expanded by rememberExpandedKey(key)
    val grammar = uiState.grammar
    val filtered = remember(grammar, query) { grammar.filter { it.matches(query) } }
    val groups = remember(filtered) { groupByGojuon(filtered) { it.indexReading() } }
    val empty = when {
        grammar.isEmpty() -> DictRow.Empty("この話の文法はまだない", null)
        filtered.isEmpty() -> DictRow.Empty("見つからない", query.takeIf { it.isNotBlank() })
        else -> null
    }
    val (rows, starts) = remember(groups, empty) { buildDictRows(groups, { "g-${it.id}" }, empty) }
    IndexedList(
        key = key,
        rows = rows,
        starts = starts,
        tools = {
            FindField(query, { query = it }, placeholder = "引く · 句型、例句", modifier = Modifier.padding(top = 12.dp))
        },
        entry = { row ->
            val item = row.value as GrammarPoint
            GrammarEntry(
                item = item,
                saved = NotebookRules.key(NotebookKind.Grammar, item.id) in savedKeys,
                expanded = expanded == item.id,
                onToggle = { expanded = if (expanded == item.id) null else item.id },
                onAsk = { onAsk(item) },
                onLearn = { onLearn(item) },
                uiState = uiState,
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrammarEntry(
    item: GrammarPoint,
    saved: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAsk: () -> Unit,
    onLearn: () -> Unit,
    uiState: LabUiState,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clickableNoRipple(onToggle).padding(top = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    item.pattern,
                    style = type.jpDisplay.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                    modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                )
                Spacer(Modifier.weight(1f))
                if (saved) NotebookMark(Modifier.alignByBaseline())
                if (item.difficulty.isNotBlank()) {
                    Text(item.difficulty.trim().uppercase(), style = type.meta, color = AjlTheme.work.accent, modifier = Modifier.alignByBaseline())
                }
            }
            Meaning(item.titleZh)
            ExampleLine(item.exampleJa, item.sourceLineNo.takeIf { it > 0 }?.let { "L$it" }.orEmpty())
        }
        if (expanded) {
            Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.exampleZh.isNotBlank()) Text(item.exampleZh, style = type.caption, color = colors.ink3)
                if (item.explanationZh.isNotBlank()) Text(item.explanationZh, style = type.body, color = colors.ink)
                if (item.pragmaticsNote.isNotBlank()) Note("語気", item.pragmaticsNote)
                if (item.realWorldNote.isNotBlank()) Note("実際", item.realWorldNote)
                EnrichmentNote(item.enrichment)
                LinguisticNote(item.linguistic)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton("講解", onAsk, compact = true)
                    OutlineButton("この文型を練習", onLearn, compact = true)
                    NotebookToggleButton(
                        entry = { item.toNotebookEntry(uiState.selection.workSlug, uiState.selection.episode) },
                        saved = saved,
                    )
                }
                LibraryAiNote(item.aiKey(), uiState)
            }
        }
        Hairline()
    }
}

@Composable
private fun Note(label: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = AjlTheme.type.jpLabel.copy(fontSize = 12.sp), color = AjlTheme.work.accent, modifier = Modifier.width(32.dp))
        Text(text, style = AjlTheme.type.body, color = AjlTheme.colors.ink2, modifier = Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------
// 台词
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinesPage(
    key: String,
    uiState: LabUiState,
    savedKeys: Set<String>,
    onPlay: (ShadowingSentence) -> Unit,
    onDeepDive: (ShadowingSentence) -> Unit,
    onAsk: (ShadowingSentence) -> Unit,
    onLearn: (ShadowingSentence) -> Unit,
) {
    var query by rememberSaveable(key) { mutableStateOf("") }
    var expanded by rememberExpandedKey(key)
    val lines = uiState.shadowing
    val filtered = remember(lines, query) { lines.filter { it.matches(query) } }
    val listState = remember(key) { LazyListState() }
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        item(key = "tools", contentType = "tools") {
            FindField(query, { query = it }, placeholder = "引く · 台词、中文", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        }
        if (filtered.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                if (lines.isEmpty()) EmptyNote("この話の台詞はまだない") else EmptyNote("見つからない", gloss = query)
            }
        }
        items(filtered, key = { "s-${it.id}" }, contentType = { "line" }) { line ->
            val spoken = remember(line.ja) { parseSpokenLine(line.ja) }
            val open = expanded == line.id
            val saved = NotebookRules.key(NotebookKind.Line, line.id) in savedKeys
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickableNoRipple(onClick = { expanded = if (open) null else line.id })
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.width(36.dp).padding(top = 2.dp)) {
                        if (spoken.speaker != null) Avatar(WorkIdentity.character(spoken.speaker), size = 36.dp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        val meta = listOfNotNull(
                            spoken.speaker,
                            line.sourceLineNo.takeIf { it > 0 }?.let { "L$it" },
                            "原声".takeIf { line.hasSourceAudio },
                        ).joinToString(" · ")
                        if (meta.isNotEmpty() || saved) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(meta, style = type.metaSmall, color = colors.ink3, modifier = Modifier.weight(1f))
                                if (saved) NotebookMark()
                            }
                        }
                        Text(spoken.text, style = type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
                        if (line.meaningZh.isNotBlank()) {
                            Text(line.meaningZh, style = type.caption.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink3)
                        }
                    }
                }
                if (open) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (line.romaji.isNotBlank()) Text(line.romaji, style = type.meta, color = colors.ink3)
                        EnrichmentNote(line.enrichment)
                        LinguisticNote(line.linguistic)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlineButton("聞く", { onPlay(line) }, compact = true, leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp)
                            OutlineButton("精読", { onDeepDive(line) }, compact = true)
                            OutlineButton("講解", { onAsk(line) }, compact = true)
                            OutlineButton("シャドーイング", { onLearn(line) }, compact = true)
                            NotebookToggleButton(
                                entry = { line.toNotebookEntry(uiState.selection.workSlug, uiState.selection.episode) },
                                saved = saved,
                            )
                        }
                        LibraryAiNote(line.aiKey(), uiState)
                    }
                }
                Hairline()
            }
        }
    }
}
