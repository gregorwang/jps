package com.animejapaneselab.nativeapp.ui.screens.library

import com.animejapaneselab.nativeapp.ui.words.KnownWords
import com.animejapaneselab.nativeapp.ui.words.VocabCards
import com.animejapaneselab.nativeapp.ui.design.NoteText
import androidx.compose.ui.platform.LocalContext
import com.animejapaneselab.nativeapp.ui.reading.Kana
import com.animejapaneselab.nativeapp.ui.words.WordRules
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.InkButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.animejapaneselab.nativeapp.ui.design.QuietButton
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.design.SwipeAction
import com.animejapaneselab.nativeapp.ui.design.SwipeActionRow
import com.animejapaneselab.nativeapp.ui.design.UndoBar
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import kotlinx.coroutines.delay
import androidx.compose.runtime.produceState
import com.animejapaneselab.nativeapp.data.LevelDict
import com.animejapaneselab.nativeapp.ui.words.TangoLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.animejapaneselab.nativeapp.data.AudioKind

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
    var studyIds by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val audioBusy = audio.playbackState.phase == AudioPlaybackPhase.Loading || audio.playbackState.phase == AudioPlaybackPhase.Playing
    var playingLineId by remember { mutableStateOf<String?>(null) }

    // 词汇 / 语法 with 按话浏览 off: the whole dictionary by level (assets/dict_*.json), a 词类 drawer for words.
    val appContext = LocalContext.current.applicationContext
    val levelMode = !uiState.settings.dictByEpisode && (selectedTab == 0 || selectedTab == 1)
    var level by rememberSaveable { mutableStateOf(LevelDict.lastLevel(appContext)) }
    var pos by rememberSaveable { mutableStateOf(PosCat.All) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val dictLoaded by produceState<LevelDict.Loaded?>(LevelDict.peek(), uiState.settings.dictByEpisode) {
        if (!uiState.settings.dictByEpisode && value == null) value = withContext(Dispatchers.Default) { LevelDict.load(appContext) }
    }
    val dictVocab = dictLoaded?.vocab.orEmpty()
    val dictGrammar = dictLoaded?.grammar.orEmpty()
    // Grammar has no 級外: on that tab the chip falls back to N5.
    val shownLevel = if (selectedTab == 1 && level == LevelDict.Outside) "N5" else level
    val levelOptions = remember(dictLoaded, selectedTab) {
        val counts = if (selectedTab == 1) dictGrammar.groupingBy { it.difficulty }.eachCount() else dictVocab.groupingBy { it.level }.eachCount()
        (Jlpt.Levels + if (selectedTab == 1) emptyList() else listOf(LevelDict.Outside)).map { LevelOption(it, levelLabel(it), counts[it] ?: 0) }
    }
    val baseVocab = remember(levelMode, dictLoaded, shownLevel, uiState.vocab) {
        if (levelMode) dictVocab.filter { it.level == shownLevel } else uiState.vocab
    }
    val posIndex = remember(baseVocab) { PosCat.index(baseVocab) }
    val posCounts = remember(posIndex) { PosCat.counts(posIndex) }
    val shownVocab = remember(baseVocab, posIndex, pos) {
        if (pos == PosCat.All) baseVocab else baseVocab.filter { PosCat.matches(posIndex[it.id], pos) }
    }
    val pageState = remember(uiState, levelMode, shownVocab, dictLoaded, shownLevel) {
        if (levelMode) {
            uiState.copy(vocab = shownVocab, grammar = dictGrammar.filter { it.difficulty == shownLevel }, shadowing = emptyList())
        } else {
            uiState.copy(vocab = shownVocab)
        }
    }

    val tabs = listOf(
        DictTab("词汇", if (levelMode) -1 else uiState.vocab.size),
        DictTab("语法", if (levelMode) -1 else uiState.grammar.size),
        DictTab("台词", uiState.shadowing.size),
        DictTab("收藏", notebook.size),
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
            trailing = {
                when {
                    selectedTab == NotebookTab -> Unit
                    levelMode -> LevelChip(
                        selected = shownLevel,
                        options = levelOptions,
                        onSelect = {
                            level = it
                            pos = PosCat.All
                            drawerOpen = false
                            LevelDict.setLastLevel(appContext, it)
                        },
                    )
                    else -> EpisodeChip(episodeTitle(episode), onClick = { pickerOpen = true })
                }
            },
        )
        if (selectedTab == 0) {
            PosHandle(
                selected = pos,
                open = drawerOpen,
                onToggle = { drawerOpen = !drawerOpen },
                onOpen = { drawerOpen = true },
                onClear = { pos = PosCat.All },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            val scope = if (levelMode) "lv#$shownLevel#$pos#$selectedTab" else "$workSlug#$episode#$selectedTab#$pos"
            when (selectedTab) {
                0 -> VocabPage(
                    key = scope,
                    uiState = pageState,
                    savedKeys = savedKeys,
                    audioBusy = audioBusy,
                    onSpeak = { audio.speakText(it, uiState.settings.ttsWorkerUrl) },
                    onPlayExample = { line -> audio.play(promptAudioForSentence(workSlug, line, autoPlay = false), uiState.settings.ttsWorkerUrl) },
                    onAsk = { item -> onAskAi(item.aiKey(), "vocab", item.surface, item.aiContext(episodeLabel)) },
                    onLearn = { studyIds = listOf(it.id) },
                    onLearnMany = { ids -> studyIds = ids },
                )
                1 -> GrammarPage(
                    key = scope,
                    uiState = pageState,
                    savedKeys = savedKeys,
                    audioBusy = audioBusy,
                    onSpeak = { audio.speakText(it, uiState.settings.ttsWorkerUrl) },
                    onPlayLine = { line -> audio.play(promptAudioForSentence(workSlug, line, autoPlay = false), uiState.settings.ttsWorkerUrl) },
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
                    playingId = playingLineId.takeIf { audioBusy },
                    onPlay = { line, tts ->
                        playingLineId = line.id
                        if (tts) {
                            audio.speakText(parseSpokenLine(line.ja).text, uiState.settings.ttsWorkerUrl)
                        } else {
                            audio.play(promptAudioForSentence(workSlug, line, autoPlay = false), uiState.settings.ttsWorkerUrl)
                        }
                    },
                    onSpeak = {
                        playingLineId = null
                        audio.speakText(it, uiState.settings.ttsWorkerUrl)
                    },
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
            if (selectedTab == 0 && drawerOpen) {
                PosPanel(
                    selected = pos,
                    counts = posCounts,
                    onSelect = {
                        pos = it
                        if (it != PosCat.Verb) drawerOpen = false
                    },
                    onClose = { drawerOpen = false },
                )
            }
        }
    }

    studyIds?.let { ids ->
        val words = ids.mapNotNull { id -> pageState.vocab.firstOrNull { it.id == id } }
        if (words.isEmpty()) {
            studyIds = null
        } else {
            WordStudyDialog(
                words = words,
                pool = pageState.vocab,
                lines = pageState.shadowing,
                settings = uiState.settings,
                workSlug = workSlug,
                episode = episode,
                onDismiss = { studyIds = null },
            )
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
        // The index only earns its place when the list runs past one screen.
        val scrolls by remember(listState) { derivedStateOf { listState.canScrollForward || listState.canScrollBackward } }
        if (starts.size > 1 && scrolls) {
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
    audioBusy: Boolean,
    onSpeak: (String) -> Unit,
    onPlayExample: (ShadowingSentence) -> Unit,
    onAsk: (VocabItem) -> Unit,
    onLearn: (VocabItem) -> Unit,
    onLearnMany: (List<String>) -> Unit,
) {
    var query by rememberSaveable(key) { mutableStateOf("") }
    // Long-press a word to start picking; tap more to add, then 斩 / 收藏 / 練習 them together.
    var picking by rememberSaveable(key) { mutableStateOf(false) }
    var picked by rememberSaveable(key) { mutableStateOf(listOf<String>()) }
    var level by rememberSaveable(key) { mutableStateOf(Jlpt.All) }
    var openId by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var speakingId by remember(key) { mutableStateOf<String?>(null) }
    var undo by remember(key) { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    // 斩: the 已斩 archive replaces the list while open; words can be restored from there.
    var archive by rememberSaveable(key) { mutableStateOf(false) }
    val appContext = LocalContext.current.applicationContext
    val haptics = LocalHapticFeedback.current
    remember { KnownWords.init(appContext) }
    val known by KnownWords.words.collectAsState()
    // Fragments the checked word cards marked keep=false (って, 〜ちゃ) are not listed.
    val studyable = remember(uiState.vocab) { uiState.vocab.filter { VocabCards.get(appContext, it.id)?.keep != false } }
    val cut = remember(studyable, known) { studyable.filter { KnownWords.key(it) in known } }
    val vocab = remember(studyable, known, archive) {
        if (archive) cut else studyable.filterNot { KnownWords.key(it) in known }
    }
    // Words every anime viewer knows (marked easy on the checked cards), offered to 斩 in one go.
    val easy = remember(vocab, archive) { if (archive) emptyList() else vocab.filter { VocabCards.get(appContext, it.id)?.easy == true || KnownWords.isObvious(it) } }
    LaunchedEffect(cut.isEmpty()) { if (cut.isEmpty()) archive = false }
    LaunchedEffect(audioBusy) { if (!audioBusy) speakingId = null }
    LaunchedEffect(undo) {
        if (undo != null) {
            delay(4000)
            undo = null
        }
    }
    val buckets = remember(vocab) { levelBuckets(vocab) }
    val filtered = remember(vocab, level, query) { filterByLevel(vocab, level).filter { it.matches(query) } }
    val groups = remember(filtered) { groupByGojuon(filtered) { it.indexReading() } }
    val empty = when {
        studyable.isEmpty() -> DictRow.Empty("この話の単語はまだない", null)
        vocab.isEmpty() -> DictRow.Empty("全部斩了", "这一话的词都会了，斩掉的在「已斩」里")
        filtered.isEmpty() -> DictRow.Empty("見つからない", query.takeIf { it.isNotBlank() })
        else -> null
    }
    val (rows, starts) = remember(groups, empty) { buildDictRows(groups, { "v-${it.id}" }, empty) }
    val offlineLines = remember { TangoLines.load(appContext) }
    val examples = remember(studyable, uiState.shadowing) {
        studyable.associate { it.id to (findExampleLine(it.surface, it.reading, uiState.shadowing) ?: offlineExample(offlineLines[it.id], it.id)) }
    }
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    fun entryOf(item: VocabItem) = item.toNotebookEntry(workSlug, episode, examples[item.id])
    fun speak(item: VocabItem, text: String = item.surface) {
        speakingId = item.id
        onSpeak(text)
    }
    fun cutWord(item: VocabItem) {
        KnownWords.cut(appContext, listOf(item))
        undo = "已斩「${item.surface}」" to { KnownWords.restore(appContext, item) }
    }
    fun toggleSave(item: VocabItem) {
        val wasSaved = NotebookRules.key(NotebookKind.Vocab, item.id) in savedKeys
        Notebook.toggle(appContext, entryOf(item))
        undo = (if (wasSaved) "已取消收藏「${item.surface}」" else "已收藏「${item.surface}」") to { Notebook.toggle(appContext, entryOf(item)) }
    }
    fun endPicking() {
        picking = false
        picked = emptyList()
    }
    Box(Modifier.fillMaxSize()) {
        IndexedList(
            key = key,
            rows = rows,
            starts = starts,
            tools = {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (picking) FilterPill(text = "取消", selected = true, onClick = { endPicking() })
                        if (cut.isNotEmpty()) {
                            FilterPill(
                                text = "已斩 ${cut.size}",
                                selected = archive,
                                onClick = {
                                    archive = !archive
                                    endPicking()
                                },
                            )
                        }
                    }
                    if (easy.isNotEmpty() && !picking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "动漫里天天听的词 ${easy.size} 个",
                                style = AjlTheme.type.caption,
                                color = AjlTheme.colors.ink3,
                                modifier = Modifier.weight(1f),
                            )
                            QuietButton("一键斩掉", onClick = { KnownWords.cut(appContext, easy) }, color = AjlTheme.work.accent)
                        }
                    }
                }
            },
            entry = { row ->
                val item = row.value as VocabItem
                VocabEntry(
                    item = item,
                    saved = NotebookRules.key(NotebookKind.Vocab, item.id) in savedKeys,
                    picked = if (picking) item.id in picked else null,
                    known = archive,
                    speaking = speakingId == item.id && audioBusy,
                    onOpen = {
                        if (picking) {
                            picked = if (item.id in picked) picked - item.id else (picked + item.id).takeLast(10)
                            if (picked.isEmpty()) picking = false
                        } else {
                            openId = item.id
                        }
                    },
                    onLongPress = {
                        if (!archive) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            picking = true
                            if (item.id !in picked) picked = (picked + item.id).takeLast(10)
                        }
                    },
                    onSpeak = { speak(item) },
                    onCut = { cutWord(item) },
                    onToggleSave = { toggleSave(item) },
                )
            },
        )
        if (picking && picked.isNotEmpty()) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlineButton(
                    "斩",
                    onClick = {
                        val words = vocab.filter { it.id in picked }
                        KnownWords.cut(appContext, words)
                        undo = "已斩 ${words.size} 个词" to { words.forEach { KnownWords.restore(appContext, it) } }
                        endPicking()
                    },
                    ink = true,
                    modifier = Modifier.height(52.dp),
                )
                OutlineButton(
                    "收藏",
                    onClick = {
                        val words = vocab.filter { it.id in picked }
                        val added = words.filter { Notebook.save(appContext, entryOf(it)) }
                        undo = "已收藏 ${words.size} 个词" to { added.forEach { Notebook.remove(appContext, NotebookRules.key(NotebookKind.Vocab, it.id)) } }
                        endPicking()
                    },
                    modifier = Modifier.height(52.dp),
                )
                InkButton(
                    "${picked.size} 語を練習",
                    onClick = {
                        onLearnMany(picked)
                        endPicking()
                    },
                    trailingArrow = true,
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
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
        val item = studyable.firstOrNull { it.id == id }
        if (item == null) {
            openId = null
        } else {
            val example = examples[item.id]
            WordCardSheet(
                item = item,
                example = example,
                saved = NotebookRules.key(NotebookKind.Vocab, item.id) in savedKeys,
                known = archive,
                speaking = speakingId == item.id && audioBusy,
                uiState = uiState,
                onSpeak = { speak(item, it) },
                onPlayExample = {
                    if (example != null) {
                        speakingId = null
                        onPlayExample(example)
                    }
                },
                onToggleSave = { toggleSave(item) },
                onCut = {
                    openId = null
                    if (archive) KnownWords.restore(appContext, item) else cutWord(item)
                },
                onAsk = { onAsk(item) },
                onLearn = {
                    openId = null
                    onLearn(item)
                },
                onDismiss = { openId = null },
            )
        }
    }
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun VocabEntry(
    item: VocabItem,
    saved: Boolean,
    /** Non-null in pick mode: whether this word is picked. */
    picked: Boolean?,
    /** Shown in the 已斩 archive: no swiping, the card offers 恢复. */
    known: Boolean,
    speaking: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onSpeak: () -> Unit,
    onCut: () -> Unit,
    onToggleSave: () -> Unit,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val work = AjlTheme.work
    val shownReading = rememberShownReading(item)
    val meaning = rememberShownMeaning(item)
    Column(Modifier.fillMaxWidth()) {
        SwipeActionRow(
            enabled = picked == null && !known,
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (picked == true) work.soft else colors.bg)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpen,
                        onLongClick = onLongPress,
                    )
                    .padding(top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (picked != null) PickMark(picked)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Headword (tap = 発音) + reading + romaji; the level sits right as a small work-colour tag.
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.surface,
                            style = type.jpDisplay.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
                            color = colors.ink,
                            textDecoration = if (speaking) TextDecoration.Underline else null,
                            maxLines = 1,
                            modifier = Modifier
                                .then(if (picked == null) Modifier.clickableNoRipple(onClick = onSpeak) else Modifier)
                                .semantics { contentDescription = "${item.surface}，点一下发音" },
                        )
                        if (speaking) VoiceBars(active = true, color = work.accent)
                        Row(
                            Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (shownReading.isNotBlank() && shownReading != item.surface) {
                                Text(shownReading, style = type.jpBody.copy(fontSize = 14.sp), color = colors.ink2, maxLines = 1)
                            }
                            val roma = remember(shownReading) { Kana.romaji(shownReading.ifBlank { item.surface }) }
                            if (roma.isNotBlank()) Text(roma, style = type.metaSmall, color = colors.ink3, maxLines = 1)
                        }
                        if (saved) NotebookMark()
                        val level = Jlpt.normalize(item.level).takeIf { it in Jlpt.Levels }
                        if (level != null) {
                            Text(
                                level,
                                style = type.metaSmall.copy(fontSize = 10.sp),
                                color = work.accent,
                                modifier = Modifier
                                    .background(work.soft, RoundedCornerShape(4.dp))
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
                        Box(Modifier.weight(1f)) { Meaning(meaning) }
                    }
                }
            }
        }
        Hairline()
    }
}

/** ① meaning — the work-colour circled number, as in a paper dictionary. */
@Composable
internal fun Meaning(text: String) {
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

/** Pick-mode check: an empty ring, or an ink disc with ✓. */
@Composable
private fun PickMark(on: Boolean, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    Box(
        modifier
            .size(22.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (on) colors.ink else colors.surface)
            .border(AjlStroke.Ink, if (on) colors.ink else colors.line2, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Icon(Icons.Rounded.Check, contentDescription = "已选", tint = colors.onInk, modifier = Modifier.size(14.dp))
    }
}

/** The offline anime line of a word (vocab_lines.json) as a shadowing line, so its card can play it. */
private fun offlineExample(line: com.animejapaneselab.nativeapp.ui.words.TangoLine?, id: String): ShadowingSentence? {
    if (line == null) return null
    return ShadowingSentence(
        id = "offline-$id",
        ja = line.ja,
        reading = "",
        meaningZh = "",
        sourceLabel = "",
        audioKind = if (line.audioUrl.isBlank()) AudioKind.Tts else AudioKind.Source,
        audioUrl = line.audioUrl,
    )
}
