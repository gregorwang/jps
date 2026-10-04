package com.animejapaneselab.nativeapp.ui.screens.library

import com.animejapaneselab.nativeapp.ui.voicepack.LineVoicePill
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.FuriganaResult
import com.animejapaneselab.nativeapp.data.FuriganaSegment
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.data.toNotebookEntry
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.CharacterRef
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.FeedbackSheet
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.MarkedLine
import com.animejapaneselab.nativeapp.ui.design.NoteText
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.OptionState
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.VoicePill
import com.animejapaneselab.nativeapp.ui.design.VoiceTone
import com.animejapaneselab.nativeapp.ui.design.WordTile
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillViewModel
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.ui.reading.Kana
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.screens.session.ReadAirRules
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuScreen
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.words.ChoiceKind
import com.animejapaneselab.nativeapp.ui.words.KnownWords
import com.animejapaneselab.nativeapp.ui.words.WordCard
import com.animejapaneselab.nativeapp.ui.words.WordRules
import com.animejapaneselab.nativeapp.ui.words.WordStep
import com.animejapaneselab.nativeapp.ui.words.VocabCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 単語練習: the words picked in 辞書, each taught on a card (kana over the kanji, every mora with
 * its romaji, the meaning, the line from the anime) and then drilled — 拼写 from kana tiles, 意思,
 * and 例句填空 on that line (听音 when there is none). Missed words come back once at the end and
 * go into 栞; words already in 栞 are graded there.
 */
@Composable
internal fun WordStudyDialog(
    words: List<VocabItem>,
    pool: List<VocabItem>,
    lines: List<ShadowingSentence>,
    settings: LabSettings,
    workSlug: String,
    episode: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val drill: ConjugationDrillViewModel = viewModel()
    val drillState by drill.state.collectAsState()
    val annotator = rememberFuriganaAnnotator(settings)
    val audio = rememberLessonAudioController()
    LaunchedEffect(Unit) { drill.ensureLoaded() }

    var cards by remember { mutableStateOf<List<WordCard>?>(null) }
    LaunchedEffect(words) {
        val fixes = withContext(Dispatchers.IO) { VocabCards.load(context) }
        val ask = words.filter { WordRules.needsFurigana(it, fixes[it.id]) }.map { it.surface.trim() }
        if (ask.isNotEmpty()) {
            annotator.request("sentence", ask)
            withTimeoutOrNull(2500) { snapshotFlow { ask.all { annotator.resultFor(it) != null } }.first { it } }
        }
        withTimeoutOrNull(1500) { snapshotFlow { drill.state.value.items.isNotEmpty() }.first { it } }
        val drillItems = drill.state.value.items
        cards = words.map { item ->
            WordRules.card(item, annotator.resultFor(item.surface.trim()), WordRules.examples(item, lines, drillItems), fixes[item.id])
        }
    }

    var attempt by remember { mutableIntStateOf(0) }
    var steps by remember { mutableStateOf<List<WordStep>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var retried by remember { mutableStateOf(false) }
    val missed = remember { mutableStateListOf<String>() }
    LaunchedEffect(cards, attempt) {
        val ready = cards ?: return@LaunchedEffect
        steps = WordRules.steps(ready, pool, attempt)
        index = 0
        answered = 0
        correctCount = 0
        retried = false
        missed.clear()
    }

    fun judge(step: WordStep, ok: Boolean) {
        answered++
        if (ok) correctCount++ else if (step.card.item.id !in missed) missed += step.card.item.id
        StudyLog.record(context, answers = 1, correct = if (ok) 1 else 0)
    }

    // 斩 on the study card: the word is known, so its questions are dropped and it leaves 辞書.
    fun cutWord(card: WordCard) {
        KnownWords.cut(context, listOf(card.item))
        steps = steps.take(index) + steps.drop(index).filterNot { it.card.item.id == card.item.id }
        if (index < steps.size) return
        if (!retried && missed.isNotEmpty()) {
            retried = true
            steps = steps + WordRules.retry(steps, missed.toSet())
        } else if (answered == 0) {
            onDismiss()
        }
    }

    fun next() {
        if (index + 1 < steps.size) {
            index++
        } else if (!retried && missed.isNotEmpty()) {
            retried = true
            val again = WordRules.retry(steps, missed.toSet())
            steps = steps + again
            index++
        } else {
            index = steps.size
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val ready = cards
            val done = ready != null && steps.isNotEmpty() && index >= steps.size
            if (done) {
                LaunchedEffect(Unit) { recordNotebook(context, ready.orEmpty(), missed.toSet(), workSlug, episode) }
                TsuzukuScreen(
                    eyebrow = "単語 · ${ready.orEmpty().size} 語",
                    tally = ReadAirRules.tally(answered, correctCount),
                    meta = ReadAirRules.accuracy(answered, correctCount),
                    noted = ready.orEmpty().filter { it.item.id in missed }.map { TsuzukuLine(it.surface, true, it.meaning) },
                    notedTitle = "放进收藏的 ${missed.size} 个词",
                    // The words are done: finishing goes back to 辞書 (missed ones already came back once and sit in 栞).
                    primaryLabel = "完成",
                    onPrimary = onDismiss,
                    onClose = onDismiss,
                )
                return@Column
            }
            TopBar(
                nav = TopBarNav.Close,
                onNav = onDismiss,
                center = {
                    ProgressLine(progress = if (steps.isEmpty()) 0f else (index + 1f) / steps.size, modifier = Modifier.weight(1f))
                    Text(
                        "${(index + 1).coerceAtMost(steps.size)}/${steps.size}",
                        style = AjlTheme.type.meta,
                        color = colors.ink3,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                },
            )
            val step = steps.getOrNull(index)
            if (ready == null || step == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
                return@Column
            }
            val wordNo = ready.indexOfFirst { it.item.id == step.card.item.id } + 1
            // key: every step starts with fresh local state.
            androidx.compose.runtime.key(index, attempt, step.card.item.id) {
                when (step) {
                    is WordStep.Study -> StudyPage(step.card, wordNo, ready.size, settings, audio, onNext = ::next, onKnown = { cutWord(step.card) })
                    is WordStep.Spell -> SpellPage(step, workSlug, onJudged = { judge(step, it) }, onNext = ::next)
                    is WordStep.Choice -> ChoicePage(step, workSlug, settings, audio, onJudged = { judge(step, it) }, onNext = ::next)
                }
            }
        }
    }
}

private fun recordNotebook(context: android.content.Context, cards: List<WordCard>, missed: Set<String>, workSlug: String, episode: Int) {
    Notebook.init(context)
    cards.forEach { card ->
        val key = NotebookRules.key(NotebookKind.Vocab, card.item.id)
        if (card.item.id in missed) {
            Notebook.save(
                context,
                card.item.toNotebookEntry(workSlug, episode, null).copy(
                    reading = card.reading?.takeIf { it != card.surface }.orEmpty(),
                    meaning = card.meaning,
                    example = card.example?.ja.orEmpty(),
                ),
            )
            Notebook.grade(context, key, remembered = false)
        } else if (Notebook.contains(key)) {
            Notebook.grade(context, key, remembered = true)
        }
    }
}

private fun speaking(audio: LessonAudioController): Boolean =
    audio.playbackState.phase == AudioPlaybackPhase.Playing || audio.playbackState.phase == AudioPlaybackPhase.Loading

/** The headword's kana over its kanji, from the card's reading. */
private fun headReading(card: WordCard): LineReading {
    val furigana = card.reading?.takeIf { Kana.hasKanji(card.surface) }?.let { FuriganaResult(listOf(FuriganaSegment(card.surface, it))) }
    return LineReading.build(card.surface, furigana)
}

// ------------------------------------------------------------------ 学习卡

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyPage(
    card: WordCard,
    number: Int,
    total: Int,
    settings: LabSettings,
    audio: LessonAudioController,
    onNext: () -> Unit,
    onKnown: () -> Unit,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val say = { audio.play(PromptAudio.Tts(card.surface, autoPlay = false), settings.ttsWorkerUrl) }
    LaunchedEffect(card.item.id) { say() }
    val head = remember(card) { headReading(card) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Eyebrow("単語 $number / $total")
            MangaPanel(Modifier.fillMaxWidth()) {
                Box {
                    VoiceTone(
                        playing = speaking(audio),
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 36.dp, y = 18.dp).size(200.dp, 96.dp).rotate(-12f),
                        origin = Offset(0.45f, 0.42f),
                    )
                    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReadingLineText(
                            head,
                            mark = null,
                            showRuby = true,
                            showRomaji = false,
                            style = AjlTheme.type.jpBody.copy(fontSize = 44.sp, fontWeight = FontWeight.Medium),
                        )
                        Text(card.meaning, style = AjlTheme.type.title.copy(fontSize = 20.sp, lineHeight = 28.sp), color = colors.ink)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val pos = card.pos.ifBlank { partOfSpeechLabel(card.item.partOfSpeech) }
                            if (pos.isNotBlank()) Tag(pos, colors.ink2)
                            Jlpt.normalize(card.item.level).takeIf { it in Jlpt.Levels }?.let { Tag(it, work.accent) }
                            Spacer(Modifier.weight(1f))
                            VoicePill(playing = speaking(audio), onClick = say, label = "発音")
                        }
                    }
                }
            }

            // 拼法: every mora of the word with its romaji underneath.
            if (card.morae.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Eyebrow("拼法")
                        Text(card.romaji, style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = work.accent)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        card.morae.forEachIndexed { i, mora ->
                            val next = card.morae.getOrNull(i + 1)?.firstOrNull()
                            MoraCell(mora, Kana.romaji(mora, next))
                        }
                    }
                }
            }

            if (card.lemma != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("原形")
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(card.lemma, style = AjlTheme.type.jpBody.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                        card.lemmaReading?.let {
                            Text("$it · ${Kana.romaji(it)}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
                        }
                    }
                }
            }

            card.example?.let { line ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow("原作里", Modifier.weight(1f))
                        LineVoicePill(line.ja, line.audioUrl, audio, settings.ttsWorkerUrl)
                    }
                    MarkedLine(line.ja, line.mark, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 28.sp))
                    if (line.zh.isNotBlank()) Text(line.zh, style = AjlTheme.type.body, color = colors.ink3)
                }
            }

            // A checked card's note (empty on purpose when there is nothing worth adding) replaces the row's own.
            val notes = (if (card.checked) listOf(card.note) else listOfNotNull(
                card.note,
                card.item.enrichment?.coreZh,
                card.item.enrichment?.usageScenes?.firstOrNull(),
                card.item.realWorldNote,
            )).map { it.trim() }.filterNot(WordRules::isFiller).distinct().take(2)
            if (notes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("用法")
                    notes.forEach { NoteText(it) }
                }
            }
        }
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlineButton("斩 · 已会", onClick = onKnown, ink = true, modifier = Modifier.height(52.dp))
            InkButton("练一练", onClick = onNext, trailingArrow = true, height = 52.dp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MoraCell(kana: String, romaji: String) {
    val colors = AjlTheme.colors
    Column(
        Modifier
            .border(AjlStroke.Hair, colors.line2, RoundedCornerShape(6.dp))
            .background(colors.surface, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(romaji.ifEmpty { " " }, style = AjlTheme.type.meta.copy(fontSize = 11.sp, lineHeight = 14.sp), color = colors.ink3)
        Text(kana, style = AjlTheme.type.jpBody.copy(fontSize = 22.sp, lineHeight = 28.sp), color = colors.ink)
    }
}

@Composable
private fun Tag(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = AjlTheme.type.caption.copy(fontSize = 12.sp),
        color = color,
        modifier = Modifier
            .border(1.dp, AjlTheme.colors.line2, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

// ------------------------------------------------------------------ 拼写

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpellPage(step: WordStep.Spell, workSlug: String, onJudged: (Boolean) -> Unit, onNext: () -> Unit) {
    val colors = AjlTheme.colors
    val card = step.card
    val feedback = LocalFeedbackEngine.current
    val picked = remember { mutableStateListOf<Int>() }
    var result by remember { mutableStateOf<Boolean?>(null) }
    val target = card.morae
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Eyebrow(if (card.kanaOnly) "拼写 · 按罗马音拼出假名" else "拼写 · 怎么读")
            MangaPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (card.kanaOnly) {
                        Text(card.romaji, style = AjlTheme.type.meta.copy(fontSize = 30.sp, lineHeight = 38.sp), color = colors.ink)
                    } else {
                        Text(card.surface, style = AjlTheme.type.jpBody.copy(fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.Medium), color = colors.ink)
                    }
                    Text(card.meaning, style = AjlTheme.type.body.copy(fontSize = 16.sp), color = colors.ink2)
                }
            }
            // Answer slot: the tiles picked so far; tap one to put it back.
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .border(AjlStroke.Ink, if (result == null) colors.ink else if (result == true) colors.ok else colors.bad, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                picked.forEach { i ->
                    WordTile(step.tiles[i], onClick = { if (result == null) picked.remove(i) }, sub = if (card.kanaOnly) null else Kana.romaji(step.tiles[i]))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                step.tiles.forEachIndexed { i, tile ->
                    WordTile(
                        tile,
                        onClick = { if (result == null && picked.size < target.size) picked += i },
                        used = i in picked,
                        sub = if (card.kanaOnly) null else Kana.romaji(tile),
                    )
                }
            }
            Spacer(Modifier.height(if (result != null) 240.dp else 90.dp))
        }
        if (result == null) {
            InkButton(
                "检查",
                onClick = {
                    val ok = picked.map { step.tiles[it] } == target
                    feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                    result = ok
                    onJudged(ok)
                },
                enabled = picked.size == target.size,
                height = 52.dp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).fillMaxWidth(),
            )
        } else {
            WordFeedback(
                correct = result == true,
                seed = card.item.id + "spell",
                workSlug = workSlug,
                answer = "${target.joinToString("")} · ${card.romaji}",
                onContinue = onNext,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ------------------------------------------------------------------ 意思 / 听音 / 填空

@Composable
private fun ChoicePage(
    step: WordStep.Choice,
    workSlug: String,
    settings: LabSettings,
    audio: LessonAudioController,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val colors = AjlTheme.colors
    val card = step.card
    val feedback = LocalFeedbackEngine.current
    var pending by remember { mutableStateOf<Int?>(null) }
    var committed by remember { mutableStateOf<Int?>(null) }
    val say = { audio.play(PromptAudio.Tts(card.surface, autoPlay = false), settings.ttsWorkerUrl) }
    LaunchedEffect(Unit) { if (step.kind != ChoiceKind.Cloze) say() }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Eyebrow(
                when (step.kind) {
                    ChoiceKind.Meaning -> "意思 · 这个词是什么意思"
                    ChoiceKind.Listen -> "听音 · 听到的是哪个词"
                    ChoiceKind.Cloze -> "填空 · 原作台词里缺了哪个词"
                },
            )
            MangaPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (step.kind) {
                        ChoiceKind.Meaning -> {
                            ReadingLineText(
                                headReading(card),
                                mark = null,
                                showRuby = true,
                                showRomaji = settings.showRomaji,
                                style = AjlTheme.type.jpBody.copy(fontSize = 36.sp, fontWeight = FontWeight.Medium),
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                VoicePill(playing = speaking(audio), onClick = say, label = "発音")
                            }
                        }
                        ChoiceKind.Listen -> Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
                            VoicePill(playing = speaking(audio), onClick = say, label = "再听一遍")
                        }
                        ChoiceKind.Cloze -> {
                            val line = card.example
                            if (line != null) {
                                val blank = "＿".repeat(card.surface.length.coerceIn(2, 4))
                                val shown = if (committed == null) line.ja.replaceRange(line.mark.first, line.mark.last + 1, blank) else line.ja
                                val mark = if (committed == null) line.mark.first until line.mark.first + blank.length else line.mark
                                MarkedLine(shown, mark, style = AjlTheme.type.jpBody.copy(fontSize = 20.sp, lineHeight = 32.sp))
                                if (line.zh.isNotBlank()) Text(line.zh, style = AjlTheme.type.body, color = colors.ink3)
                            }
                        }
                    }
                }
            }
            step.options.forEachIndexed { i, option ->
                OptionRow(
                    text = option,
                    state = when {
                        committed == null -> if (pending == i) OptionState.Selected else OptionState.Default
                        i == step.answer -> OptionState.Correct
                        i == committed -> OptionState.Wrong
                        else -> OptionState.Dimmed
                    },
                    onClick = { if (committed == null) pending = i },
                    japanese = step.kind != ChoiceKind.Meaning,
                    leading = ('A' + i).toString(),
                )
            }
            Spacer(Modifier.height(if (committed != null) 240.dp else 90.dp))
        }
        if (committed == null) {
            InkButton(
                "检查",
                onClick = {
                    val choice = pending ?: return@InkButton
                    val ok = choice == step.answer
                    feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                    committed = choice
                    onJudged(ok)
                    if (step.kind == ChoiceKind.Cloze) say()
                },
                enabled = pending != null,
                height = 52.dp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).fillMaxWidth(),
            )
        } else {
            WordFeedback(
                correct = committed == step.answer,
                seed = card.item.id + step.kind,
                workSlug = workSlug,
                answer = listOfNotNull(card.surface, card.reading?.takeIf { it != card.surface }, card.meaning).joinToString(" · "),
                onContinue = onNext,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun WordFeedback(correct: Boolean, seed: String, workSlug: String, answer: String, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val reaction = remember(seed, correct) { ReadAirRules.reaction(correct, seed, inScene = false) }
    val character = remember(workSlug) { WorkIdentity.representative(workSlug) ?: CharacterRef("学", "学", null) }
    FeedbackSheet(
        correct = correct,
        onContinue = onContinue,
        modifier = modifier,
        character = character,
        line = reaction.ja,
        lineGloss = reaction.zh,
        explanation = answer,
    )
}
