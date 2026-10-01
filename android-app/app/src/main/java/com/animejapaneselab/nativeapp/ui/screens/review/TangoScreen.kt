package com.animejapaneselab.nativeapp.ui.screens.review

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import com.animejapaneselab.nativeapp.ui.words.VocabCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.animejapaneselab.nativeapp.ui.knowledge.VocabWord
import com.animejapaneselab.nativeapp.ui.words.Tango
import com.animejapaneselab.nativeapp.ui.words.TangoGroup
import com.animejapaneselab.nativeapp.ui.words.TangoLine
import com.animejapaneselab.nativeapp.ui.words.TangoLines
import com.animejapaneselab.nativeapp.ui.words.TangoRules
import com.animejapaneselab.nativeapp.ui.words.VocabCardFix


// ---------------------------------------------------------------------------------- 単語

/** 単語 tab: five words, think-then-look, one quiz at the end of the group (画布 Tango*). */
@Composable
fun TangoScreen(
    uiState: LabUiState,
    vocab: Map<String, VocabCardFix>,
    pool: List<VocabWord>,
    know: List<KnowledgeCard>,
    drill: ConjugationDrillState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now().toEpochDay() }
    remember { Tango.init(context) }
    val tango by Tango.state.collectAsState()
    val lines by produceState(TangoLines.peek()) { if (value == null) value = withContext(Dispatchers.IO) { TangoLines.load(context) } }
    val ranked by produceState<List<VocabWord>?>(null, pool, lines, know, drill.learned, drill.items) {
        val l = lines ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val lessonText = drill.items.asSequence().filter { it.pointId in drill.learned }.joinToString("\n") { it.jaText }
            TangoRules.rank(TangoRules.pool(pool, l), l, TangoRules.textOf(know), lessonText)
        }
    }
    LaunchedEffect(ranked) { ranked?.let { Tango.ensureGroup(context, it, today) } }
    val group = tango.group
    val l = lines
    // Today's group is already saved: show it right away, the ranking only matters for the next one.
    val r = ranked ?: pool.takeIf { group != null }
    Box(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        when {
            l == null || r == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
            group == null -> EmptyNote("今日の単語はここまで", gloss = "没有新词，也没有到期的词", modifier = Modifier.align(Alignment.Center))
            else -> {
                val words = remember(group.ids, vocab) {
                    group.ids.mapNotNull { id -> vocab[id]?.let { VocabWord(id, KnowledgeRules.headOf(id, it), it) } }
                }
                key(group.ids) {
                    TangoSession(
                        uiState = uiState,
                        words = words,
                        lines = l,
                        pool = r,
                        group = group,
                        today = today,
                        onNextGroup = { Tango.nextGroup(context, ranked ?: r, today) },
                    )
                }
            }
        }
    }
}

private class TangoAids(val ruby: Boolean, val romaji: Boolean, val annotator: FuriganaAnnotator)

private fun playTangoLine(line: TangoLine, audio: LessonAudioController, ttsWorkerUrl: String) {
    if (line.audioUrl.isNotEmpty()) {
        audio.play(
            PromptAudio.Source(line.audioUrl, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = line.ja),
            ttsWorkerUrl,
        )
    } else {
        audio.speakText(line.ja, ttsWorkerUrl)
    }
}

/** Where the word sits in its line: the word itself, else its stem (食べる → 食べ). */
private fun targetRange(ja: String, word: VocabWord): IntRange? {
    val forms = listOf(word.head, word.head.dropLast(1).takeIf { word.head.length >= 3 }).filterNotNull()
    for (f in forms) {
        val i = ja.indexOf(f)
        if (i >= 0) return i until i + f.length
    }
    return null
}

@Composable
private fun TangoSession(
    uiState: LabUiState,
    words: List<VocabWord>,
    lines: Map<String, TangoLine>,
    pool: List<VocabWord>,
    group: TangoGroup,
    today: Long,
    onNextGroup: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val scope = rememberCoroutineScope()
    val settings = uiState.settings
    val audio = rememberLessonAudioController()
    val furigana = rememberFuriganaAnnotator(settings)
    val aids = remember(settings.showFurigana, settings.showRomaji, furigana) { TangoAids(settings.showFurigana, settings.showRomaji, furigana) }
    val known by KnownWords.words.collectAsState()
    val notebook = rememberNotebookEntries()
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { if (toast != null) { delay(1400); toast = null } }

    // Page ids: a word, or a word sent back with 再来 (`id#n`); the quiz is always the last page.
    val queue = remember { mutableStateListOf<String>().apply { addAll(words.map { it.id }) } }
    val revealed = remember { mutableStateMapOf<String, Boolean>() }
    val seen = remember { mutableStateMapOf<String, Boolean>() }
    val pager = rememberPagerState(initialPage = if (group.results.isNotEmpty()) queue.size else 0) { queue.size + 1 }
    val handoff = rememberFeedPageHandoff(pager)
    val byId = remember(words) { words.associateBy { it.id } }

    val current = pager.currentPage
    val onQuiz = current >= queue.size
    val canFlick = onQuiz || revealed[queue.getOrNull(current)] == true
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            queue.getOrNull(page)?.let { seen[it.substringBefore('#')] = true }
        }
    }
    // The line of the card on screen starts by itself, like the 知識 feed.
    LaunchedEffect(pager.settledPage, queue.size) {
        val id = queue.getOrNull(pager.settledPage)?.substringBefore('#') ?: return@LaunchedEffect
        val line = lines[id] ?: return@LaunchedEffect
        if (settings.autoSpeak) playTangoLine(line, audio, settings.ttsWorkerUrl)
    }

    Box(Modifier.fillMaxSize()) {
        val peek = if (onQuiz) "単語 · 次の ${TangoRules.GroupSize} 個" else "単語 · ${(current + 1).coerceAtMost(words.size)} / ${words.size}"
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(22.dp)
                .background(colors.surface, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .drawBehind {
                    val w = 1.5.dp.toPx()
                    drawLine(colors.line2, Offset(w / 2, size.height), Offset(w / 2, w / 2), w)
                    drawLine(colors.line2, Offset(w / 2, w / 2), Offset(size.width - w / 2, w / 2), w)
                    drawLine(colors.line2, Offset(size.width - w / 2, w / 2), Offset(size.width - w / 2, size.height), w)
                }
                .padding(start = 14.dp, top = 5.dp),
        ) {
            Text(peek, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.ink3)
        }
        VerticalPager(
            state = pager,
            // A card not turned over yet can't be flicked on, but going back up always works.
            modifier = Modifier.fillMaxSize().blockForwardDrag(!canFlick),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 26.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            flingBehavior = feedFlingBehavior(pager),
            key = { if (it < queue.size) "w:" + queue[it] else "quiz" },
        ) { page ->
            if (page >= queue.size) {
                TangoQuizPage(
                    words = words,
                    lines = lines,
                    pool = pool,
                    group = group,
                    aids = aids,
                    audio = audio,
                    ttsWorkerUrl = settings.ttsWorkerUrl,
                    autoSpeak = settings.autoSpeak,
                    active = page == pager.settledPage,
                    modifier = Modifier.nestedScroll(handoff),
                    onAnswer = { word, right ->
                        StudyLog.record(context, answers = 1, correct = if (right) 1 else 0)
                        Tango.answer(context, word.id, right, today)
                    },
                    onNextGroup = {
                        StudyLog.finishSession(context)
                        onNextGroup()
                    },
                )
            } else {
                val pageId = queue[page]
                val word = byId[pageId.substringBefore('#')]
                val line = lines[pageId.substringBefore('#')]
                if (word != null && line != null) {
                    val hearted = word.head in known
                    val entry = remember(word.id) {
                        NotebookEntry(
                            key = NotebookRules.key(NotebookKind.Vocab, word.id),
                            kind = NotebookKind.Vocab,
                            headline = word.head,
                            reading = word.fix.reading,
                            meaning = word.fix.meaning,
                            example = line.ja,
                        )
                    }
                    val saved = notebook.any { it.key == entry.key }
                    TangoWordPage(
                        word = word,
                        line = line,
                        group = words,
                        seen = seen,
                        currentBase = pageId.substringBefore('#'),
                        revealed = revealed[pageId] == true,
                        hearted = hearted,
                        saved = saved,
                        canAgain = queue.drop(page + 1).none { it.substringBefore('#') == word.id },
                        aids = aids,
                        audio = audio,
                        ttsWorkerUrl = settings.ttsWorkerUrl,
                        todayLabel = "今日の ${words.size} 個",
                        modifier = Modifier.nestedScroll(handoff),
                        onReveal = { revealed[pageId] = true },
                        onHeart = { on ->
                            KnownWords.setWord(context, word.head, on)
                            toast = if (on) "掌握了 · 这个词不再出现" else "取消掌握"
                        },
                        onSave = {
                            Notebook.toggle(context, entry)
                            toast = if (saved) "取消收藏" else "已收藏"
                        },
                        onAgain = {
                            queue.add(word.id + "#" + (queue.size + 1))
                            toast = "待会儿再来"
                            scope.launch { pager.animateScrollToPage(page + 1) }
                        },
                        onPlay = { playTangoLine(line, audio, settings.ttsWorkerUrl) },
                    )
                }
            }
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
                color = colors.bg,
                modifier = Modifier.background(colors.ink, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

/**
 * Swallows the upward part of a drag (finger moving up = next card) before the pager sees it, once
 * it is past half the touch slop, so taps still land and a downward drag still goes to the card above.
 */
private fun Modifier.blockForwardDrag(enabled: Boolean): Modifier = if (!enabled) this else pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop / 2f
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var dy = 0f
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val step = change.position.y - change.previousPosition.y
            dy += step
            if (step < 0f && dy < -slop) change.consume()
        } while (event.changes.any { it.pressed })
    }
}

/** Five short bars: done (ink), this word (work colour), still to come (outline). */
@Composable
private fun TangoTicks(words: List<VocabWord>, seen: Map<String, Boolean>, currentBase: String?) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        words.forEach { w ->
            val done = seen[w.id] == true && w.id != currentBase
            val fill = when {
                w.id == currentBase -> work.accent
                done -> colors.ink
                else -> androidx.compose.ui.graphics.Color.Transparent
            }
            Box(
                Modifier
                    .size(width = 22.dp, height = 4.dp)
                    .background(fill, RoundedCornerShape(2.dp))
                    .border(1.dp, if (fill == androidx.compose.ui.graphics.Color.Transparent) colors.line2 else fill, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun TangoWordPage(
    word: VocabWord,
    line: TangoLine,
    group: List<VocabWord>,
    seen: Map<String, Boolean>,
    currentBase: String,
    revealed: Boolean,
    hearted: Boolean,
    saved: Boolean,
    canAgain: Boolean,
    aids: TangoAids,
    audio: LessonAudioController,
    ttsWorkerUrl: String,
    todayLabel: String,
    modifier: Modifier,
    onReveal: () -> Unit,
    onHeart: (Boolean) -> Unit,
    onSave: () -> Unit,
    onAgain: () -> Unit,
    onPlay: () -> Unit,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val fix = word.fix
    val head = word.head
    val playing = audio.playbackState.phase == AudioPlaybackPhase.Playing || audio.playbackState.phase == AudioPlaybackPhase.Loading
    val aided = aids.ruby || aids.romaji
    LaunchedEffect(line.ja, aided) { if (aided) aids.annotator.request("sentence", listOf(line.ja)) }
    val reading = remember(line.ja, aids.annotator.resultFor(line.ja)) { LineReading.build(line.ja, aids.annotator.resultFor(line.ja)) }
    val mark = remember(line.ja, word.id) { targetRange(line.ja, word) }
    val headSize = when {
        revealed && head.length <= 3 -> 56
        head.length <= 3 -> 72
        head.length <= 6 -> 44
        else -> 28
    }
    val collocations = remember(word.id) { TangoRules.collocations(word) }
    val lineStyle = AjlTheme.type.jpBody.copy(fontSize = 21.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)

    MangaPanel(modifier.fillMaxSize()) {
        Screentone(Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-22).dp).size(190.dp, 80.dp).rotate(-12f))
        Column(Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("単語 · $todayLabel", Modifier.weight(1f))
                if (fix.pos.isNotBlank()) {
                    Text(
                        fix.pos,
                        style = AjlTheme.type.caption.copy(fontSize = 11.sp),
                        color = work.accent,
                        modifier = Modifier.border(1.dp, work.accent, RoundedCornerShape(11.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            TangoTicks(group, seen, currentBase)
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(end = 56.dp).verticalScroll(rememberScrollState(), enabled = revealed),
                verticalArrangement = Arrangement.spacedBy(if (revealed) 16.dp else 24.dp, Alignment.CenterVertically),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (fix.reading.isNotBlank()) {
                        if (aids.romaji) Text(Kana.romaji(fix.reading), style = AjlTheme.type.meta.copy(fontSize = 14.sp), color = colors.ink3)
                        if (fix.reading != head) Text(fix.reading, style = AjlTheme.type.jpBody.copy(fontSize = 20.sp), color = work.accent)
                    }
                    Text(
                        head,
                        style = AjlTheme.type.jpTitle.copy(fontSize = headSize.sp, lineHeight = (headSize * 1.2f).sp, fontWeight = FontWeight.Bold),
                        color = colors.ink,
                    )
                }
                if (revealed) {
                    Text(fix.meaning, style = AjlTheme.type.title.copy(fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                    if (collocations.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("常一起出现", style = AjlTheme.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = work.accent)
                            collocations.forEach { Text(it, style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = colors.ink) }
                        }
                    }
                    if (fix.note.isNotBlank()) NoteText(fix.note)
                }
                if (!revealed) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReadingLineText(reading, mark, showRuby = aids.ruby, showRomaji = aids.romaji, style = lineStyle)
                        VoicePill(playing, if (line.audioUrl.isNotEmpty()) "原声" else "朗读", onPlay)
                    }
                }
            }
            if (revealed) {
                Column(Modifier.padding(end = 56.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReadingLineText(reading, mark, showRuby = aids.ruby, showRomaji = aids.romaji, style = lineStyle)
                    VoicePill(playing, if (line.audioUrl.isNotEmpty()) "原声" else "朗读", onPlay)
                }
            }
            if (!revealed) {
                RevealBox(Modifier.padding(end = 56.dp).height(64.dp), onClick = onReveal, tone = true) {
                    Text("意思？", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2)
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RailButton(
                if (hearted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (hearted) "已掌握" else "掌握",
                tint = if (hearted) colors.heart else null,
            ) { onHeart(!hearted) }
            RailButton(if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (saved) "已收藏" else "收藏", active = saved, onClick = onSave)
            if (revealed && canAgain) RailButton(Icons.Rounded.Replay, "再来", onClick = onAgain)
        }
    }
}

@Composable
private fun TangoQuizPage(
    words: List<VocabWord>,
    lines: Map<String, TangoLine>,
    pool: List<VocabWord>,
    group: TangoGroup,
    aids: TangoAids,
    audio: LessonAudioController,
    ttsWorkerUrl: String,
    autoSpeak: Boolean,
    active: Boolean,
    modifier: Modifier,
    onAnswer: (VocabWord, Boolean) -> Unit,
    onNextGroup: () -> Unit,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    var index by remember { mutableIntStateOf(group.results.size.coerceAtMost(words.lastIndex).coerceAtLeast(0)) }
    var picked by remember(index) { mutableStateOf<Int?>(null) }
    val word = words.getOrNull(index)
    val line = word?.let { lines[it.id] }
    val playing = audio.playbackState.phase == AudioPlaybackPhase.Playing || audio.playbackState.phase == AudioPlaybackPhase.Loading
    val question = remember(word?.id) { word?.let { TangoRules.options(it, pool) } }
    val aided = aids.ruby || aids.romaji
    LaunchedEffect(line?.ja, aided) { if (aided && line != null) aids.annotator.request("sentence", listOf(line.ja)) }
    val reading = remember(line?.ja, aids.annotator.resultFor(line?.ja.orEmpty())) {
        line?.let { LineReading.build(it.ja, aids.annotator.resultFor(it.ja)) }
    }
    LaunchedEffect(index, active) { if (active && autoSpeak && line != null) playTangoLine(line, audio, ttsWorkerUrl) }
    val done = group.results.size

    MangaPanel(modifier.fillMaxSize()) {
        Screentone(Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-22).dp).size(190.dp, 80.dp).rotate(-12f))
        Column(Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("単語 · 小テスト", Modifier.weight(1f))
                Text(
                    "${words.size} 個のうち ${done.coerceAtMost(words.size)}",
                    style = AjlTheme.type.caption.copy(fontSize = 11.sp),
                    color = work.accent,
                    modifier = Modifier.border(1.dp, work.accent, RoundedCornerShape(11.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().drawBehind {
                    drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                    drawLine(colors.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }.padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                words.forEachIndexed { i, w ->
                    val result = group.results[w.id]
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            w.head,
                            style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = if (i == index && result == null) colors.ink else if (result == null) colors.ink3 else colors.ink,
                            maxLines = 1,
                        )
                        Box(
                            Modifier.size(8.dp).background(
                                when (result) {
                                    true -> colors.ok
                                    false -> colors.bad
                                    null -> colors.line2
                                },
                                CircleShape,
                            ),
                        )
                    }
                }
            }
            if (word != null && line != null && question != null) {
                val (options, answer) = question
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoicePill(playing, "听", { playTangoLine(line, audio, ttsWorkerUrl) })
                        val mark = remember(line.ja, word.id) { targetRange(line.ja, word) }
                        val style = AjlTheme.type.jpBody.copy(fontSize = 21.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)
                        ReadingLineText(reading ?: LineReading.build(line.ja, null), mark, showRuby = aids.ruby, showRomaji = aids.romaji, style = style)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.forEachIndexed { i, text ->
                            val answered = picked != null
                            val tint = when {
                                !answered -> colors.ink
                                i == answer -> colors.ok
                                i == picked -> colors.bad
                                else -> colors.ink3
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.surface)
                                    .border(1.5.dp, if (answered && i != answer && i != picked) colors.line2 else tint, RoundedCornerShape(4.dp))
                                    .clickable(enabled = !answered && word.id !in group.results, onClickLabel = text) {
                                        picked = i
                                        onAnswer(word, i == answer)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text, style = AjlTheme.type.body.copy(fontSize = 16.sp, fontWeight = if (answered && i == answer) FontWeight.SemiBold else FontWeight.Normal), color = tint, modifier = Modifier.weight(1f))
                                if (answered && i == answer) Text("✓", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = tint)
                                if (answered && i == picked && i != answer) Text("✗", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = tint)
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            val last = index >= words.lastIndex
            if (picked != null || group.finished) {
                InkButton(
                    text = if (last) "次の ${TangoRules.GroupSize} 個" else "次へ",
                    onClick = { if (last) onNextGroup() else index++ },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
