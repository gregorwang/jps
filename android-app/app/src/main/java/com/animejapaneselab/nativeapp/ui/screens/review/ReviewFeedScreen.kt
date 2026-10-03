package com.animejapaneselab.nativeapp.ui.screens.review

import android.content.Context
import com.animejapaneselab.nativeapp.ui.voicepack.LineVoicePill
import com.animejapaneselab.nativeapp.ui.voicepack.lineCue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AiCoachState
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.EpisodeContentCache
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.HeartBurst
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.MarkedLine
import com.animejapaneselab.nativeapp.ui.design.NoteText
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.StampMark
import com.animejapaneselab.nativeapp.ui.design.TextTabs
import com.animejapaneselab.nativeapp.ui.design.ToolPanel
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.VoiceTone
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.drill.DrillPhase
import com.animejapaneselab.nativeapp.ui.jishu.JishuViewModel
import com.animejapaneselab.nativeapp.ui.knowledge.KnowQuiz
import com.animejapaneselab.nativeapp.ui.knowledge.Knowledge
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCard
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCards
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeDeck
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeRules
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.ui.notebook.rememberNotebookEntries
import com.animejapaneselab.nativeapp.ui.notebook.speakEntry
import com.animejapaneselab.nativeapp.ui.reading.FuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.reading.Kana
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.review.FeedCard
import com.animejapaneselab.nativeapp.ui.review.FeedKind
import com.animejapaneselab.nativeapp.ui.review.FeedRules
import com.animejapaneselab.nativeapp.ui.review.FeedSession
import com.animejapaneselab.nativeapp.ui.review.FeedSource
import com.animejapaneselab.nativeapp.ui.review.FeedSources
import com.animejapaneselab.nativeapp.ui.review.ReviewFeed
import com.animejapaneselab.nativeapp.ui.review.ReviewSinks
import com.animejapaneselab.nativeapp.ui.review.Verdict
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.ui.screens.jishu.FormulaRow
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.words.KnownWords
import com.animejapaneselab.nativeapp.ui.words.Tango
import com.animejapaneselab.nativeapp.ui.words.VocabCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.animejapaneselab.nativeapp.ui.words.TangoLines
import com.animejapaneselab.nativeapp.ui.words.TangoRules
import kotlin.math.roundToInt

/** Callbacks of the 復習 tab (the feed writes verdicts itself through [sinks]). */
class ReviewFeedActions(
    val sinks: ReviewSinks,
    val ensureDrill: () -> Unit,
    val askAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
    val practiceMistake: (itemId: String) -> Unit,
    val practiceTask: (ProgressItem) -> Unit,
    /** 苦手 card: the category name from [ReviewRules.weakCategory]. */
    val practiceWeak: (category: String) -> Unit,
    val viewSource: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    val openKnownWords: () -> Unit,
    val openSearch: () -> Unit,
    /** おわり 完成: back to 今日. */
    val done: () -> Unit,
)

private enum class Sheet { Ai, Context }

/** 復習 tab: 知識 (the endless card feed, due cards woven in) and 帳面 (decks, 掌握了的, 苦手, the books). */
@Composable
fun ReviewFeedScreen(
    uiState: LabUiState,
    drill: ConjugationDrillState,
    actions: ReviewFeedActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val today = remember { LocalDate.now().toEpochDay() }
    val notebook = rememberNotebookEntries()
    remember { KnownWords.init(context); ReviewFeed.init(context); Knowledge.init(context) }
    val known by KnownWords.words.collectAsState()
    val mistakeDue by ReviewFeed.mistakeDue.collectAsState()
    val marks by Knowledge.marks.collectAsState()
    // Both assets are local, parsed once off the main thread (usually already at app start).
    val decks by produceState(KnowledgeCards.peek()) { if (value == null) value = withContext(Dispatchers.IO) { KnowledgeCards.decks(context) } }
    val vocab by produceState(VocabCards.peek()) { if (value == null) value = withContext(Dispatchers.IO) { VocabCards.load(context) } }
    val know = remember(decks) { decks.orEmpty().flatMap { it.cards } }
    val words = remember(vocab, known) { KnowledgeRules.vocabPool(vocab.orEmpty(), known) }
    val sources = remember(drill, notebook, uiState.mistakes, uiState.reviewTasks, uiState.progressItems, known, mistakeDue, know, marks, words) {
        FeedSources(drill, notebook, uiState.mistakes, uiState.reviewTasks, uiState.progressItems, known, mistakeDue, know, marks, words)
    }
    // Only the local cards are waited for. The 活用 lines come from the network: until they are in, the
    // feed runs without them and their due cards are woven in when they arrive.
    val ready = decks != null && vocab != null
    LaunchedEffect(Unit) { actions.ensureDrill() }
    // Knowledge points saved to the 收藏 notebook before 0.18 become 收藏 stars of the card itself.
    LaunchedEffect(notebook) {
        val prefix = NotebookRules.key(NotebookKind.Grammar, "know-")
        val old = notebook.filter { it.key.startsWith(prefix) }
        if (old.isNotEmpty()) {
            Knowledge.adoptStars(context, old.map { it.key.removePrefix(prefix) }, today)
            old.forEach { Notebook.remove(context, it.key) }
        }
    }
    LaunchedEffect(sources, ready) { if (ready) ReviewFeed.sync(context, sources, today) }
    val session by ReviewFeed.session.collectAsState()
    val deck by ReviewFeed.deck.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // A knowledge card picked in search: show the 知識 feed, which has moved onto it.
    val jumps by ReviewFeed.jumps.collectAsState()
    LaunchedEffect(jumps) { if (ReviewFeed.takeJump()) tab = 0 }
    val current = session?.takeIf { it.day == today }
    val hearts = remember(marks) { marks.count { it.value.hearted } }

    Column(modifier.fillMaxSize().background(colors.bg)) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextTabs(items = listOf("知識", "単語", "帳面"), selectedIndex = tab, onSelect = { tab = it })
            Spacer(Modifier.weight(1f))
            if (tab == 0) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = colors.heart, modifier = Modifier.size(13.dp))
                Text(
                    "$hearts",
                    style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                    color = colors.ink3,
                    modifier = Modifier.padding(start = 4.dp, end = 10.dp),
                )
                if (current != null) {
                    Text("今日 ${current.read}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
                }
            }
            if (tab == 1) {
                val tango by Tango.state.collectAsState()
                Text(
                    "今日 ${tango.today(today)} · 已会 ${tango.learnedCount}",
                    style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                    color = colors.ink3,
                )
            }
            IconButton44(Icons.Rounded.Search, "搜知识点", actions.openSearch)
        }
        Hairline(Modifier.padding(horizontal = 20.dp))
        if (tab == 0) {
            val deckTitle = current?.deck?.let { id ->
                when (id) {
                    KnowledgeRules.StarDeck -> "收藏的知识点"
                    KnowledgeRules.VocabDeck -> "单词"
                    else -> decks?.firstOrNull { it.id == id }?.title
                }
            }
            when {
                !ready && deck.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
                current == null || deck.isEmpty() -> EmptyNote("还没有卡片", gloss = "知识点卡片还没装进来")
                else -> FeedPager(
                    uiState = uiState,
                    session = current,
                    deck = deck,
                    sources = sources,
                    filterLabel = current.filter?.label ?: deckTitle,
                    actions = actions,
                    onClearFilter = {
                        if (current.deck != null) ReviewFeed.deck(context, null, sources, today) else ReviewFeed.filter(context, null, sources, today)
                    },
                )
            }
        } else if (tab == 1) {
            if (!ready) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
            } else {
                TangoScreen(uiState = uiState, vocab = vocab.orEmpty(), pool = words, know = know, drill = drill)
            }
        } else {
            Ledger(
                sources = sources,
                decks = decks.orEmpty(),
                today = today,
                onPickDeck = { id ->
                    ReviewFeed.deck(context, id, sources, today)
                    tab = 0
                },
                onOpenWords = { tab = 1 },
            )
        }
    }
}

// ---------------------------------------------------------------------------------- feed

@Composable
private fun FeedPager(
    uiState: LabUiState,
    session: FeedSession,
    deck: List<FeedCard>,
    sources: FeedSources,
    filterLabel: String?,
    actions: ReviewFeedActions,
    onClearFilter: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = uiState.settings
    val pager = rememberPagerState(initialPage = session.index.coerceIn(0, (deck.size - 1).coerceAtLeast(0))) { deck.size }
    val audio = rememberLessonAudioController()
    val furigana = rememberFuriganaAnnotator(settings)
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val today = session.day
    val latestSources by androidx.compose.runtime.rememberUpdatedState(sources)

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page -> ReviewFeed.settle(context, page, actions.sinks, latestSources) }
    }
    // 帳面 restarts the feed at 0, a search hit moves it onto the picked card: follow it.
    val jumps by ReviewFeed.jumps.collectAsState()
    LaunchedEffect(session.filter, session.deck, session.keys.firstOrNull(), jumps) {
        if (pager.settledPage != session.index && !pager.isScrollInProgress) pager.scrollToPage(session.index.coerceIn(0, (deck.size - 1).coerceAtLeast(0)))
    }
    val settledCard = deck.getOrNull(pager.settledPage)
    // TikTok-style: the line of the card on screen starts by itself.
    LaunchedEffect(settledCard?.key) {
        sheet = null
        val card = settledCard ?: return@LaunchedEffect
        if (settings.autoSpeak && (card.kind == FeedKind.Conj || card.kind == FeedKind.Listen)) play(context, card, audio, settings.ttsWorkerUrl)
    }
    LaunchedEffect(toast) { if (toast != null) { delay(1400); toast = null } }

    val handoff = rememberFeedPageHandoff(pager)

    fun advance(from: Int) {
        scope.launch { if (from + 1 < deck.size) pager.animateScrollToPage(from + 1) }
    }

    Box(Modifier.fillMaxSize()) {
        VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 26.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            flingBehavior = feedFlingBehavior(pager),
            key = { deck.getOrNull(it)?.key ?: it },
        ) { page ->
            val card = deck[page]
            FeedCardView(
                card = card,
                graded = card.key in session.graded,
                modifier = Modifier.nestedScroll(handoff),
                filterLabel = filterLabel,
                hearted = heartedOf(card, sources),
                starred = card.know != null && sources.marks[card.know.id]?.starred == true,
                active = page == pager.settledPage,
                audio = audio,
                ttsWorkerUrl = settings.ttsWorkerUrl,
                aids = Aids(settings.showFurigana, settings.showRomaji, furigana),
                onVerdict = { verdict ->
                    ReviewFeed.grade(context, card.key, verdict, actions.sinks)
                    if (verdict == Verdict.Again) toast = "待会儿再来"
                    if (verdict != Verdict.Mastered) advance(page)
                },
                onHeart = { on ->
                    when {
                        card.know != null -> {
                            Knowledge.heart(context, card.know.id, on, today)
                            toast = if (on) "掌握了 · 不再推，30 天后回来考一次" else "取消掌握"
                        }
                        card.vocab != null -> {
                            KnownWords.setWord(context, card.vocab.head, on)
                            toast = if (on) "掌握了 · 这个词不再出现" else "取消掌握"
                        }
                    }
                },
                onStar = { on ->
                    card.know?.let { Knowledge.star(context, it.id, on, today) }
                    toast = if (on) "收藏了 · 每天回来一次" else "取消收藏"
                },
                onHeartDone = { if (card.isDue) advance(page) },
                onAnswer = { quiz, right ->
                    StudyLog.record(context, answers = 1, correct = if (right) 1 else 0)
                    val lost = Knowledge.answer(context, quiz, right, today)
                    if (lost) {
                        val title = sources.know.firstOrNull { it.id == quiz.tests }?.title.orEmpty()
                        toast = "「$title」取消掌握，之后再推给你"
                    }
                },
                onCut = {
                    ReviewFeed.cut(context, card.key)
                    toast = "斩 · 不再出现"
                    advance(page)
                },
                onToast = { toast = it },
                onSheet = { sheet = it },
                onFilterClear = onClearFilter,
                onPracticeMistake = actions.practiceMistake,
                onPracticeWeak = actions.practiceWeak,
            )
        }
        AnimatedVisibility(
            visible = toast != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                toast.orEmpty(),
                style = AjlTheme.type.body.copy(fontSize = 13.sp),
                color = AjlTheme.colors.bg,
                modifier = Modifier
                    .background(AjlTheme.colors.ink, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
        FeedSheet(
            sheet = sheet,
            card = settledCard,
            uiState = uiState,
            actions = actions,
            onClose = { sheet = null },
        )
    }
}

/** ♥ state of a stream card; due cards show theirs only while it is being given. */
private fun heartedOf(card: FeedCard, sources: FeedSources): Boolean = when {
    card.know != null -> sources.marks[card.know.id]?.hearted == true
    card.vocab != null -> card.vocab.head in sources.known
    else -> false
}

private fun play(context: Context, card: FeedCard, audio: LessonAudioController, ttsWorkerUrl: String) {
    card.line?.let { line -> audio.play(lineCue(context, line.jaText, line.audioUrl), ttsWorkerUrl) }
    card.entry?.let { speakEntry(it, audio, ttsWorkerUrl) }
}

private class Aids(val ruby: Boolean, val romaji: Boolean, val annotator: FuriganaAnnotator)

@Composable
private fun FeedCardView(
    card: FeedCard,
    graded: Boolean,
    modifier: Modifier,
    filterLabel: String?,
    hearted: Boolean,
    starred: Boolean,
    active: Boolean,
    audio: LessonAudioController,
    ttsWorkerUrl: String,
    aids: Aids,
    onVerdict: (Verdict) -> Unit,
    onHeart: (Boolean) -> Unit,
    onStar: (Boolean) -> Unit,
    onHeartDone: () -> Unit,
    onAnswer: (KnowQuiz, Boolean) -> Unit,
    onCut: () -> Unit,
    onToast: (String) -> Unit,
    onSheet: (Sheet) -> Unit,
    onFilterClear: () -> Unit,
    onPracticeMistake: (String) -> Unit,
    onPracticeWeak: (String) -> Unit,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    var revealed by rememberSaveable(card.key) { mutableStateOf(false) }
    var bursting by remember(card.key) { mutableStateOf(false) }
    var dueHeart by rememberSaveable(card.key) { mutableStateOf(false) }
    val stream = card.know != null || card.vocab != null
    val heartOn = if (stream) hearted else dueHeart
    val playing = active && (audio.playbackState.phase == AudioPlaybackPhase.Playing || audio.playbackState.phase == AudioPlaybackPhase.Loading)
    val notebook = rememberNotebookEntries()

    // ♥ = 掌握: on a due card it is the 覚えた verdict (then the next card); on a stream card it can be undone.
    fun heart(on: Boolean) {
        when {
            card.isDue -> if (on && !graded) {
                dueHeart = true
                revealed = true
                bursting = true
                onVerdict(Verdict.Mastered)
            }
            stream -> {
                if (on) bursting = true
                onHeart(on)
            }
        }
    }

    MangaPanel(
        modifier
            .fillMaxSize()
            .pointerInput(card.key, graded, heartOn) {
                detectTapGestures(
                    onTap = { if (card.isDue) revealed = true },
                    // Double tap = ♥, like TikTok.
                    onDoubleTap = { if ((card.isDue || stream) && !heartOn && !bursting) heart(true) },
                )
            },
    ) {
        Screentone(Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-22).dp).size(190.dp, 80.dp).rotate(-12f))
        Column(Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
            if (filterLabel != null) {
                Text(
                    "只看 $filterLabel  ×",
                    style = AjlTheme.type.caption.copy(fontSize = 12.sp),
                    color = work.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, work.accent, RoundedCornerShape(14.dp))
                        .clickable(onClickLabel = "取消筛选", onClick = onFilterClear)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow(card.eyebrow, Modifier.weight(1f))
                val label = card.know?.kind?.label ?: card.dueLabel
                if (label != null) {
                    if (card.know != null) {
                        Text(
                            label,
                            style = AjlTheme.type.caption.copy(fontSize = 11.sp),
                            color = work.accent,
                            modifier = Modifier.border(1.dp, work.accent, RoundedCornerShape(11.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    } else {
                        Eyebrow(label, color = work.accent)
                    }
                }
            }
            when (card.kind) {
                FeedKind.Conj -> ConjBody(card, revealed, aids, audio, ttsWorkerUrl, onReveal = { revealed = true })
                FeedKind.Word -> WordBody(card.entry!!, revealed, playing, onPlay = { if (audio.isSounding) audio.stop() else play(context, card, audio, ttsWorkerUrl) }, onReveal = { revealed = true })
                FeedKind.Listen -> ListenBody(card.entry!!, revealed, playing, aids, onPlay = { if (audio.isSounding) audio.stop() else play(context, card, audio, ttsWorkerUrl) }, onReveal = { revealed = true })
                FeedKind.Mistake -> MistakeBody(card.mistake!!, revealed, onReveal = { revealed = true })
                FeedKind.Weak -> WeakBody(card.weak!!, onPractice = { onPracticeWeak(card.weak.name) })
                FeedKind.Know -> KnowBody(card.know!!, aids.annotator.takeIf { aids.romaji }, onAnswer)
                FeedKind.Vocab -> VocabBody(card.vocab!!, aids.romaji)
            }
        }
        if (card.isDue || stream) {
            val saveEntry = remember(card.key) { saveEntryOf(card) }
            val saved = saveEntry != null && notebook.any { it.key == saveEntry.key }
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RailButton(
                    if (heartOn) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (heartOn) "已掌握" else "掌握",
                    tint = if (heartOn) colors.heart else null,
                ) { if (card.isDue) heart(true) else heart(!heartOn) }
                if (card.know != null && card.know.kind != com.animejapaneselab.nativeapp.ui.knowledge.KnowKind.Quiz) {
                    RailButton(if (starred) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (starred) "已收藏" else "收藏", active = starred) {
                        onStar(!starred)
                    }
                } else if (saveEntry != null) {
                    RailButton(if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (saved) "已收藏" else "收藏", active = saved) {
                        Notebook.toggle(context, saveEntry)
                        onToast(if (saved) "取消收藏" else "已收藏")
                    }
                }
                if (card.mistake != null) RailButton(Icons.Rounded.PlayArrow, "再练") { onPracticeMistake(card.mistake.itemId) }
                if (card.entry?.kind == NotebookKind.Vocab && !graded) RailButton(Icons.Rounded.ContentCut, "斩", onClick = onCut)
                if (card.isDue) RailButton(Icons.Rounded.ChatBubbleOutline, "讲解") { onSheet(Sheet.Ai) }
                if (contextSource(card) != null) RailButton(Icons.AutoMirrored.Rounded.Notes, "前后句") { onSheet(Sheet.Context) }
                if (card.isDue && !graded) RailButton(Icons.Rounded.Replay, "再来") { onVerdict(Verdict.Again) }
            }
        }
        if (bursting) {
            HeartBurst(
                modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp),
                onDone = {
                    bursting = false
                    onHeartDone()
                },
            )
        }
    }
}

/** What 收藏 saves to the notebook for this card (a 活用 line, a word); knowledge cards are starred instead. */
private fun saveEntryOf(card: FeedCard): NotebookEntry? {
    card.line?.let { return lineEntry(it) }
    card.vocab?.let { w ->
        return NotebookEntry(
            key = NotebookRules.key(NotebookKind.Vocab, w.id),
            kind = NotebookKind.Vocab,
            headline = w.head,
            reading = w.fix.reading,
            meaning = w.fix.meaning,
        )
    }
    return null
}

@Composable
internal fun Screentone(modifier: Modifier) {
    Box(modifier.screentone(AjlTheme.work.tone(0.22f)))
}

@Composable
internal fun RailButton(icon: ImageVector, label: String, active: Boolean = false, tint: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val tint = tint ?: if (active) AjlTheme.work.accent else colors.ink
    Column(
        Modifier
            .width(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.size(44.dp).background(colors.sunken, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, style = AjlTheme.type.metaSmall.copy(fontSize = 10.sp, lineHeight = 14.sp), color = tint)
    }
}

/** The 遮る box under a card: tap to see the answer. */
@Composable
internal fun RevealBox(modifier: Modifier = Modifier, tone: Boolean = false, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = AjlTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.sunken)
            .then(if (tone) Modifier.screentone(AjlTheme.work.tone(0.22f)) else Modifier)
            .clickable(onClickLabel = "翻开", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
internal fun VoicePill(playing: Boolean, label: String, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val tint = if (playing) work.accent else colors.ink
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (playing) colors.sunken else colors.surface)
            .border(1.5.dp, colors.ink, RoundedCornerShape(20.dp))
            .clickable(onClickLabel = "播放", onClick = onClick)
            .padding(start = 12.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VoiceBars(active = playing, color = tint)
        Text(label, style = AjlTheme.type.meta.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = tint)
    }
}

private val JpSerif = FontFamily.Serif

@Composable
private fun ColumnScope.ConjBody(card: FeedCard, revealed: Boolean, aids: Aids, audio: LessonAudioController, ttsWorkerUrl: String, onReveal: () -> Unit) {
    val item = card.line ?: return
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val aided = aids.ruby || aids.romaji
    LaunchedEffect(item.jaText, aided) { if (aided) aids.annotator.request("sentence", listOf(item.jaText)) }
    val reading = remember(item.jaText, aids.annotator.resultFor(item.jaText)) { LineReading.build(item.jaText, aids.annotator.resultFor(item.jaText)) }
    val size = if (item.jaText.length > 22) 22.sp else 26.sp
    val lineStyle = AjlTheme.type.jpBody.copy(fontSize = size, lineHeight = size * 1.7f, fontWeight = FontWeight.Medium)
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(end = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        if (revealed) {
            ReadingLineText(reading, item.spanStart until item.spanEnd, showRuby = aids.ruby, showRomaji = aids.romaji, style = lineStyle)
        } else {
            val tone = work.tone(0.22f)
            val blank = buildAnnotatedString {
                append(item.jaText.substring(0, item.spanStart))
                appendInlineContent("blank", item.target)
                append(item.jaText.substring(item.spanEnd))
            }
            val width = (item.spanEnd - item.spanStart).coerceAtLeast(2).em
            Text(
                blank,
                style = lineStyle,
                color = colors.ink,
                inlineContent = mapOf(
                    "blank" to InlineTextContent(Placeholder(width, 1.1.em, PlaceholderVerticalAlign.TextCenter)) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                                .screentone(tone, spacing = 6.dp, dotRadius = 1.3.dp)
                                .drawUnderline(work.accent),
                        )
                    },
                ),
            )
        }
        LineVoicePill(item.jaText, item.audioUrl, audio, ttsWorkerUrl)
    }
    Column(
        Modifier.fillMaxWidth().padding(end = 56.dp).heightIn(min = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
    ) {
        if (!revealed) {
            RevealBox(Modifier.heightIn(min = 72.dp), onClick = onReveal) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        item.head.base.ifBlank { item.target },
                        style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                    )
                    Text("→ ？", style = AjlTheme.type.meta.copy(fontSize = 16.sp), color = work.accent)
                    if (item.sense.isNotBlank()) Chip(item.sense, maxWidth = 120)
                }
            }
        } else {
            FormulaRow(
                item.formula,
                item.group,
                wordSize = 22.sp,
                readings = { words -> reading.readingsOf(words, item.spanStart) },
                showRuby = aids.ruby,
                showRomaji = aids.romaji,
            )
            if (item.zh.isNotBlank()) Text(item.zh, style = AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
        }
    }
}

private fun Modifier.drawUnderline(color: androidx.compose.ui.graphics.Color): Modifier = drawBehind {
    val h = 2.dp.toPx()
    drawRect(color, topLeft = Offset(0f, size.height - h), size = androidx.compose.ui.geometry.Size(size.width, h))
}

@Composable
private fun Chip(text: String, maxWidth: Int = 200) {
    Text(
        text,
        style = AjlTheme.type.caption.copy(fontSize = 12.sp),
        color = AjlTheme.work.accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = maxWidth.dp)
            .border(1.dp, AjlTheme.colors.line2, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun ColumnScope.WordBody(entry: NotebookEntry, revealed: Boolean, playing: Boolean, onPlay: () -> Unit, onReveal: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val head = entry.headline.trim()
    val headSize = when {
        head.length <= 3 -> 72.sp
        head.length <= 6 -> 48.sp
        else -> 30.sp
    }
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(end = 56.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text(head, style = AjlTheme.type.jpTitle.copy(fontSize = headSize, lineHeight = headSize * 1.25f, fontWeight = FontWeight.Bold), color = colors.ink)
        if (entry.reading.isNotBlank() && entry.reading != head) {
            if (revealed) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(entry.reading, style = AjlTheme.type.jpBody.copy(fontSize = 22.sp), color = work.accent)
                    Text(Kana.romaji(entry.reading), style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = colors.ink3, modifier = Modifier.padding(bottom = 3.dp))
                }
            } else {
                Box(Modifier.width(150.dp).height(26.dp).clip(RoundedCornerShape(3.dp)).screentone(colors.ink.copy(alpha = 0.16f), spacing = 6.dp))
            }
        }
        if (entry.example.isNotBlank()) {
            val at = entry.example.indexOf(head)
            MarkedLine(
                entry.example,
                if (at >= 0 && head.isNotEmpty()) at until at + head.length else null,
                modifier = Modifier.padding(top = 14.dp),
                style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 28.sp),
                color = colors.ink2,
            )
        }
        VoicePill(playing, "读音", onPlay)
    }
    Column(
        Modifier.fillMaxWidth().padding(end = 56.dp).heightIn(min = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
    ) {
        if (!revealed) {
            RevealBox(onClick = onReveal) { Text(if (entry.reading.isNotBlank()) "读音和意思" else "意思", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink3) }
        } else {
            if (entry.meaning.isNotBlank()) Text(entry.meaning, style = AjlTheme.type.title.copy(fontSize = 21.sp, lineHeight = 30.sp), color = colors.ink)
            Chip(entry.kind.label)
        }
    }
}

@Composable
private fun ColumnScope.ListenBody(entry: NotebookEntry, revealed: Boolean, playing: Boolean, aids: Aids, onPlay: () -> Unit, onReveal: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val text = entry.headline
    val aided = aids.ruby || aids.romaji
    LaunchedEffect(text, aided, revealed) { if (aided && revealed) aids.annotator.request("sentence", listOf(text)) }
    val reading = remember(text, aids.annotator.resultFor(text)) { LineReading.build(text, aids.annotator.resultFor(text)) }
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(end = 56.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(200.dp, 96.dp)
                .clickable(onClickLabel = "再听一遍", onClick = onPlay),
            contentAlignment = Alignment.CenterStart,
        ) {
            VoiceTone(playing = playing, modifier = Modifier.fillMaxSize(), origin = Offset(0.15f, 0.5f))
            VoiceBars(active = playing, color = work.accent, modifier = Modifier.padding(start = 12.dp).size(56.dp, 48.dp))
        }
        if (revealed) {
            ReadingLineText(
                reading,
                null,
                showRuby = aids.ruby,
                showRomaji = aids.romaji,
                style = AjlTheme.type.jpBody.copy(fontSize = 24.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val tone = colors.ink.copy(alpha = 0.16f)
                Box(Modifier.fillMaxWidth(0.92f).height(26.dp).clip(RoundedCornerShape(3.dp)).screentone(tone, spacing = 6.dp))
                Box(Modifier.fillMaxWidth(0.58f).height(26.dp).clip(RoundedCornerShape(3.dp)).screentone(tone, spacing = 6.dp))
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(end = 56.dp).heightIn(min = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
    ) {
        if (!revealed) {
            RevealBox(onClick = onReveal) { Text("看原文", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink3) }
        } else if (entry.meaning.isNotBlank()) {
            Text(entry.meaning, style = AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
        }
    }
}

@Composable
private fun ColumnScope.MistakeBody(mistake: MistakeRecord, revealed: Boolean, onReveal: () -> Unit) {
    val colors = AjlTheme.colors
    val japanese = mistake.prompt.any(Kana::isKana)
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(end = 56.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        MangaPanel(Modifier.fillMaxWidth()) {
            Text(
                mistake.prompt,
                style = if (japanese) {
                    AjlTheme.type.jpBody.copy(fontSize = 20.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)
                } else {
                    AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 27.sp)
                },
                color = colors.ink,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        AnswerLine("你答", mistake.selected.ifBlank { "未作答" }, colors.bad, strike = true)
    }
    Column(
        Modifier.fillMaxWidth().padding(end = 56.dp).heightIn(min = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
    ) {
        if (!revealed) {
            RevealBox(onClick = onReveal) { Text("看正解", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink3) }
        } else {
            AnswerLine("正解", mistake.expected, colors.ok, strike = false)
            if (mistake.explanation.isNotBlank()) NoteText(mistake.explanation)
        }
    }
}

@Composable
private fun AnswerLine(label: String, value: String, tint: androidx.compose.ui.graphics.Color, strike: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3, modifier = Modifier.width(40.dp).padding(top = 3.dp))
        Text(
            value,
            style = AjlTheme.type.body.copy(
                fontSize = 16.sp,
                fontWeight = if (strike) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (strike) TextDecoration.LineThrough else null,
            ),
            color = tint,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ColumnScope.WeakBody(spot: ReviewRules.WeakSpot, onPractice: () -> Unit) {
    val colors = AjlTheme.colors
    val pct = (spot.accuracy * 100).roundToInt()
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Text("$pct%", style = AjlTheme.type.meta.copy(fontSize = 80.sp, lineHeight = 88.sp, fontWeight = FontWeight.Medium), color = AjlTheme.work.accent)
        Text(spot.name, style = AjlTheme.type.jpTitle.copy(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold), color = colors.ink)
        Text("${spot.attempts} 题", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
        ProgressLine(progress = spot.accuracy, modifier = Modifier.padding(top = 20.dp), contentDescription = "${spot.name} 正确率 $pct%")
    }
    OutlineButton("去练这一类", onClick = onPractice, modifier = Modifier.fillMaxWidth())
}

/** A 活用 line as a 栞 台词 card, so 收藏 on the rail can save it. */
private fun lineEntry(line: com.animejapaneselab.nativeapp.data.ConjugationDrillItem): NotebookEntry {
    val episode = JishuViewModel.episodeOf(line.sentenceId)
    return NotebookEntry(
        key = NotebookRules.key(NotebookKind.Line, line.sentenceId),
        kind = NotebookKind.Line,
        headline = line.jaText,
        meaning = line.zh,
        workSlug = episode?.workSlug.orEmpty(),
        episode = episode?.episode ?: 0,
        audioUrl = line.audioUrl,
    )
}

// ---------------------------------------------------------------------------------- sheets

/** Where a card's line sits in its episode, for 前后句; null when it has no episode. */
private data class ContextSource(val workSlug: String, val episode: Int, val lineNo: Int, val text: String)

private fun contextSource(card: FeedCard): ContextSource? {
    card.line?.let { line ->
        val ep = JishuViewModel.episodeOf(line.sentenceId) ?: return null
        return ContextSource(ep.workSlug, ep.episode, 0, line.jaText)
    }
    card.entry?.let { entry ->
        if (entry.kind != NotebookKind.Line || entry.workSlug.isBlank() || entry.episode <= 0) return null
        return ContextSource(entry.workSlug, entry.episode, entry.lineNo, entry.headline)
    }
    card.mistake?.let { m ->
        if (m.workSlug.isBlank() || m.episode <= 0) return null
        val lineNo = ReviewRules.sourceLineNo(m)
        if (lineNo <= 0) return null
        return ContextSource(m.workSlug, m.episode, lineNo, "")
    }
    return null
}

private fun aiKey(card: FeedCard) = "review:" + FeedRules.baseKey(card.key)

/** What the card is, for AI 讲解: kind, the text asked about, and the context. */
private fun aiRequest(card: FeedCard): Triple<String, String, String>? {
    card.line?.let { line ->
        return Triple(
            "sentence",
            line.jaText,
            buildString {
                append("请用简体中文讲解这句动漫台词里的「").append(line.target).append("」这个活用。")
                append("\n台词：").append(line.jaText)
                if (line.zh.isNotBlank()) append("\n中文：").append(line.zh)
                if (line.formula.isNotBlank()) append("\n拆解：").append(line.formula)
                append("\n讲清：怎么变形、这里的语气、两三个同类说法。")
            },
        )
    }
    card.entry?.let { entry ->
        val kind = when (entry.kind) {
            NotebookKind.Vocab -> "vocab"
            NotebookKind.Grammar -> "grammar"
            NotebookKind.Line -> "sentence"
        }
        return Triple(
            kind,
            entry.headline,
            buildString {
                append("请用简体中文讲解：").append(entry.headline)
                if (entry.reading.isNotBlank()) append("（").append(entry.reading).append("）")
                if (entry.meaning.isNotBlank()) append("\n意思：").append(entry.meaning)
                if (entry.example.isNotBlank()) append("\n例句：").append(entry.example)
            },
        )
    }
    card.mistake?.let { m ->
        return Triple(
            if (m.typeLabel == "语言学题" || m.typeLabel == "读空气") "linguistic" else "exercise",
            m.prompt,
            buildString {
                append("请用简体中文讲解这道错题。\n题目：").append(m.prompt)
                append("\n我的答案：").append(m.selected)
                append("\n正确答案：").append(m.expected)
                if (m.explanation.isNotBlank()) append("\n站内说明：").append(m.explanation)
                append("\n请按“语境线索 -> 错因 -> 正确判断 -> 下次判断方法”讲解。")
            },
        )
    }
    return null
}

@Composable
private fun FeedSheet(
    sheet: Sheet?,
    card: FeedCard?,
    uiState: LabUiState,
    actions: ReviewFeedActions,
    onClose: () -> Unit,
) {
    val colors = AjlTheme.colors
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = sheet != null, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.bg.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "关闭",
                        onClick = onClose,
                    ),
            )
        }
        AnimatedVisibility(
            visible = sheet != null && card != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .clip(shape)
                    .background(colors.bg)
                    .border(1.dp, colors.line2, shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).size(36.dp, 4.dp).background(colors.line2, RoundedCornerShape(2.dp)))
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (sheet == Sheet.Context) "前后句" else "讲解",
                        style = AjlTheme.type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton44(Icons.Rounded.Close, "关闭", onClose, tint = colors.ink2)
                }
                Hairline()
                if (card != null) {
                    when (sheet) {
                        Sheet.Ai -> AiSheet(card, uiState, actions)
                        Sheet.Context -> ContextSheet(card, actions)
                        null -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.AiSheet(card: FeedCard, uiState: LabUiState, actions: ReviewFeedActions) {
    val colors = AjlTheme.colors
    val key = aiKey(card)
    val request = remember(card.key) { aiRequest(card) }
    val ai = uiState.aiCoach.takeIf { uiState.libraryAiTargetKey == key }
    LaunchedEffect(key) {
        if (uiState.libraryAiTargetKey != key && request != null) actions.askAi(key, request.first, request.second, request.third)
    }
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        LazyColumn(Modifier.weight(1f)) {
            item { AiResult(ai) }
        }
    }
    var question by remember(card.key) { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 14.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(AjlShape.Tool)
                .background(colors.surface)
                .border(1.dp, colors.line2, AjlShape.Tool)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (question.isEmpty()) Text("追问这张卡…", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink3)
            BasicTextField(
                value = question,
                onValueChange = { question = it },
                textStyle = AjlTheme.type.body.copy(fontSize = 14.sp, color = colors.ink),
                cursorBrush = SolidColor(AjlTheme.work.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton44(
            Icons.AutoMirrored.Rounded.Send,
            "发送",
            onClick = {
                val q = question.trim()
                if (q.isNotEmpty() && request != null) {
                    actions.askAi(key, request.first, q, request.third + "\n追问：" + q)
                    question = ""
                }
            },
            enabled = question.isNotBlank(),
            tint = AjlTheme.work.accent,
        )
    }
}

/** AI 讲解: structured sections when present (same shape as the old 错题本 panel). */
@Composable
private fun AiResult(ai: AiCoachState?) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val result = ai?.result
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            ai == null || (ai.status == SyncStatus.Loading && result == null) -> LoadingDots(delayMillis = 0)
            result != null -> {
                if (result.title.isNotBlank()) Text(result.title, style = type.title.copy(fontSize = 17.sp, lineHeight = 24.sp), color = colors.ink)
                if (result.summary.isNotBlank()) NoteText(result.summary)
                result.sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (section.title.isNotBlank()) Text(section.title, style = type.caption.copy(fontWeight = FontWeight.SemiBold), color = AjlTheme.work.accent)
                        NoteText(section.body)
                    }
                }
                if (result.sections.isEmpty() && result.summary.isBlank() && result.text.isNotBlank()) NoteText(result.text)
            }
            else -> Text(ai.answer, style = type.body, color = if (ai.status == SyncStatus.Error) colors.bad else colors.ink2)
        }
    }
}

private data class ContextLine(val text: String, val current: Boolean, val lineNo: Int)

@Composable
private fun ColumnScope.ContextSheet(card: FeedCard, actions: ReviewFeedActions) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val source = remember(card.key) { contextSource(card) } ?: return
    var lines by remember(card.key) { mutableStateOf<List<ContextLine>?>(null) }
    LaunchedEffect(card.key) {
        lines = withContext(Dispatchers.IO) {
            runCatching {
                val store = LocalLabStore(context)
                val client = RemoteLabClient(
                    store.readSettings().apiBaseUrl,
                    store.readSessionCookie(),
                    contentCache = EpisodeContentCache(context.filesDir),
                )
                val all = client.fetchSubtitleLines(com.animejapaneselab.nativeapp.data.EpisodeSelection(source.workSlug, source.episode))
                fun norm(s: String) = s.filterNot { it.isWhitespace() }
                val at = all.indexOfFirst { source.lineNo > 0 && it.lineNo == source.lineNo }.takeIf { it >= 0 }
                    ?: all.indexOfFirst { source.text.isNotBlank() && norm(it.jaText) == norm(source.text) }
                if (at < 0) emptyList() else {
                    (maxOf(0, at - 2)..minOf(all.lastIndex, at + 2)).map { i -> ContextLine(all[i].jaText.trim(), i == at, all[i].lineNo) }
                }
            }.getOrDefault(emptyList())
        }
    }
    val accent = AjlTheme.work.accent
    Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
        val loaded = lines
        when {
            loaded == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingDots(delayMillis = 0) }
            loaded.isEmpty() -> EmptyNote("前後が見つからない", gloss = "这句在字幕里没对上")
            else -> loaded.forEach { line ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(if (line.current) colors.sunken else colors.bg)
                        .drawBehind {
                            if (line.current) drawRect(accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        line.text,
                        style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp),
                        color = if (line.current) colors.ink else colors.ink3,
                    )
                }
            }
        }
    }
    QuietButton(
        "看整集字幕",
        onClick = { actions.viewSource(source.workSlug, source.episode, lines?.firstOrNull { it.current }?.lineNo ?: source.lineNo) },
        modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
    )
}

// ---------------------------------------------------------------------------------- 帳面

@Composable
private fun Ledger(
    sources: FeedSources,
    decks: List<KnowledgeDeck>,
    today: Long,
    onPickDeck: (String) -> Unit,
    onOpenWords: () -> Unit,
) {
    val context = LocalContext.current
    val known by KnownWords.words.collectAsState()
    val tango by Tango.state.collectAsState()
    val wordTotal = remember(sources.words) { TangoLines.peek()?.let { TangoRules.pool(sources.words, it).size } }
    val starred = remember(sources.know, sources.marks) {
        sources.know.filter { sources.marks[it.id]?.starred == true }.sortedByDescending { sources.marks[it.id]?.starDay ?: 0L }
    }
    val mastered = remember(sources.know, sources.marks) {
        sources.know.filter { sources.marks[it.id]?.hearted == true }.sortedByDescending { sources.marks[it.id]?.heartDay ?: 0L }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
    ) {
        if (decks.isNotEmpty()) {
            item(key = "decks-heading") { LedgerHeading("合集", "点一个，只刷这一个", first = true) }
            items(decks, key = { "deck-" + it.id }) { deck ->
                val read = deck.cards.count { (sources.marks[it.id]?.seen ?: 0) > 0 }
                val hearted = deck.cards.count { sources.marks[it.id]?.hearted == true }
                val short = deck.cards.firstOrNull()?.deckShort?.ifBlank { null } ?: deck.title
                BookRow(deckMark(deck.id, short), short, "${deck.cards.size} 张 · 读过 $read · ♥ $hearted") { onPickDeck(deck.id) }
            }
        }
        item(key = "stars-heading") { LedgerHeading("收藏的知识点", "每天回来一次") }
        if (starred.isEmpty()) item(key = "stars-empty") { EmptyRow() }
        items(starred, key = { "s-" + it.id }) { card ->
            val day = sources.marks[card.id]?.starDay ?: today
            BookRow(
                "★",
                card.title.replace("【", "").replace("】", ""),
                listOf(card.deckShort.ifBlank { card.deckTitle }, monthDay(day)).joinToString(" · "),
                markColor = AjlTheme.work.accent,
            ) { onPickDeck(KnowledgeRules.StarDeck) }
        }
        item(key = "mastered-heading") { LedgerHeading("掌握了的", "30 天、90 天各回来考一次") }
        if (mastered.isEmpty()) item(key = "mastered-empty") { EmptyRow() }
        items(mastered, key = { "m-" + it.id }) { card ->
            val mark = sources.marks[card.id] ?: return@items
            val checkDay = mark.heartDay + if (mark.checks == 0) 30 else 90
            val meta = if (mark.checks >= 2) monthDay(mark.heartDay) else "${monthDay(mark.heartDay)} · ${(checkDay - today).coerceAtLeast(0)} 天后考"
            MasteredRow(card, meta, onUndo = { Knowledge.heart(context, card.id, false, today) })
        }
        item(key = "words-heading") { LedgerHeading("単語", "和知識分开刷") }
        item(key = "words") {
            BookRow(
                "単",
                "单词",
                listOfNotNull(
                    wordTotal?.let { "共 $it 个" },
                    "已会 ${tango.learnedCount}",
                    "在学 ${tango.learningCount}",
                    "斩 ${known.size}",
                ).joinToString(" · "),
                trailing = tango.group?.takeIf { !it.finished }?.let { g -> "今日 ${g.ids.size - g.results.size}" },
                onClick = onOpenWords,
            )
        }
    }
}

/** The big glyph in front of a 合集: the number of 第X篇, else the first character of its short name. */
private fun deckMark(id: String, short: String): String {
    Regex("第(.)篇").find(short)?.let { return it.groupValues[1] }
    if (id == "threads") return "線"
    return short.substringAfter("·").trim().take(1).ifBlank { short.take(1) }
}

private fun monthDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).let { "${it.monthValue}/${it.dayOfMonth}" }

@Composable
private fun LedgerHeading(title: String, meta: String, first: Boolean = false) {
    val colors = AjlTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = if (first) 0.dp else 24.dp)
            .drawBehind { drawLine(colors.ink, Offset(0f, size.height), Offset(size.width, size.height), 1.5.dp.toPx()) }
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = colors.ink)
        Text(meta, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3, modifier = Modifier.padding(bottom = 2.dp))
    }
}

@Composable
private fun EmptyRow() {
    Column {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
            Text("还没有", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = AjlTheme.colors.ink3, modifier = Modifier.padding(start = 44.dp))
        }
        Hairline()
    }
}

@Composable
private fun BookRow(
    mark: String,
    title: String,
    meta: String,
    markColor: androidx.compose.ui.graphics.Color? = null,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    val colors = AjlTheme.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(onClickLabel = title, onClick = onClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(mark, style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = markColor ?: colors.ink, modifier = Modifier.width(30.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = AjlTheme.type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) Text(trailing, style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = AjlTheme.work.accent)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.ink3, modifier = Modifier.size(16.dp))
        }
        Hairline()
    }
}

@Composable
private fun MasteredRow(card: KnowledgeCard, meta: String, onUndo: () -> Unit) {
    val colors = AjlTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("♥", style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = colors.heart, modifier = Modifier.width(30.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(card.title.replace("【", "").replace("】", ""), style = AjlTheme.type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
            }
            QuietButton("取消", onClick = onUndo)
        }
        Hairline()
    }
}
