package com.animejapaneselab.nativeapp.ui.screens.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LevelDict
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.MarkedLine
import com.animejapaneselab.nativeapp.ui.design.NoteText
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.VoiceWave
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.screens.library.Jlpt
import com.animejapaneselab.nativeapp.ui.screens.library.LevelChip
import com.animejapaneselab.nativeapp.ui.screens.library.LevelOption
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.voicepack.LineVoicePill
import com.animejapaneselab.nativeapp.ui.voicepack.lineCue
import com.animejapaneselab.nativeapp.ui.words.Homophones
import com.animejapaneselab.nativeapp.ui.words.KnownWords
import com.animejapaneselab.nativeapp.ui.words.TangoLines
import com.animejapaneselab.nativeapp.ui.words.WordPractice
import com.animejapaneselab.nativeapp.ui.words.WordPracticeRules
import com.animejapaneselab.nativeapp.ui.words.WpKind
import com.animejapaneselab.nativeapp.ui.words.WpSession
import com.animejapaneselab.nativeapp.ui.words.WpStep
import com.animejapaneselab.nativeapp.ui.words.WpWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val LevelPref = "word-practice-level"

/**
 * 練習 → 単語: home (this level's 已掌握 / 今日 / today's new words) or, while a round is open, the
 * round itself. Rules in [WordPracticeRules].
 */
@Composable
internal fun WordPracticeTab(settings: LabSettings, modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    remember { WordPractice.init(appContext); KnownWords.init(appContext) }
    val loaded by produceState<Boolean>(false) {
        withContext(Dispatchers.Default) {
            LevelDict.load(appContext)
            TangoLines.load(appContext)
            Homophones.load(appContext)
        }
        value = true
    }
    val prefs = remember { appContext.getSharedPreferences("ajl-dict", android.content.Context.MODE_PRIVATE) }
    var level by rememberSaveable { mutableStateOf(prefs.getString(LevelPref, null) ?: "N5") }
    val known by KnownWords.words.collectAsState()
    val state by WordPractice.state.collectAsState()
    val session by WordPractice.session.collectAsState()
    val today = remember { LocalDate.now().toEpochDay() }
    if (!loaded) return

    val dict = LevelDict.peek() ?: return
    val lines = TangoLines.peek().orEmpty()
    val pool = remember(level, known) {
        WordPracticeRules.pool(dict, lines, level, known) { s -> Homophones.groupOf(s)?.words?.firstOrNull { it.surface == s } }
    }
    val audio = rememberLessonAudioController()

    val open = session
    if (open != null && open.level == level) {
        WordRound(open, pool, settings, audio, today, modifier)
        return
    }

    val counts = remember(known) {
        Jlpt.Levels.map { lv -> LevelOption(lv, lv, WordPracticeRules.pool(dict, lines, lv, known) { null }.size) }
    }
    val mastered = pool.count { (state.progress[it.id]?.box ?: 0) >= WordPracticeRules.Mastered }
    val learning = pool.count { (state.progress[it.id]?.box ?: 0) in 1 until WordPracticeRules.Mastered }
    val cut = remember(level, known) { WordPracticeRules.cutCount(dict, level, known) }
    val due = remember(pool, state) { WordPracticeRules.due(pool, state.progress, today) }
    val fresh = remember(pool, state) { WordPracticeRules.newOrder(pool, state.progress).take(WordPracticeRules.newLeft(state, today)) }
    val colors = AjlTheme.colors
    val work = AjlTheme.work

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$mastered", style = AjlTheme.type.meta.copy(fontSize = 44.sp, lineHeight = 48.sp), color = work.accent)
                Text("/ ${pool.size}", style = AjlTheme.type.meta.copy(fontSize = 15.sp), color = colors.ink3, modifier = Modifier.padding(bottom = 6.dp))
                Text("已掌握", style = AjlTheme.type.caption, color = colors.ink2, modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                Spacer(Modifier.weight(1f))
                LevelChip(level, counts, onSelect = {
                    level = it
                    prefs.edit().putString(LevelPref, it).apply()
                }, modifier = Modifier.padding(bottom = 6.dp))
            }
            ProgressLine(if (pool.isEmpty()) 0f else mastered / pool.size.toFloat(), Modifier.fillMaxWidth(), thickness = 2.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat("在学", learning)
                Stat("未学", pool.size - mastered - learning)
                Stat("斩", cut)
            }
            Hairline()
            Text("今日", style = AjlTheme.type.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.ink3)
            Column {
                TodayRow("復習", "到期", due.size)
                Hairline()
                TodayRow("新出", "新词", fresh.size)
            }
            if (fresh.isNotEmpty()) {
                fresh.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { w -> NewTile(w, Modifier.weight(1f)) { audio.speakText(w.surface, settings.ttsWorkerUrl) } }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (due.isEmpty() && fresh.isEmpty()) {
                EmptyNote(if (pool.isEmpty()) "この級は ぜんぶ 斬った" else "今日の分は おしまい")
            }
        }
        val total = due.size + fresh.size * 2
        if (total > 0) {
            InkButton(
                "始める · ${due.size + fresh.size} 题",
                { WordPractice.start(WordPracticeRules.session(level, pool, state, today, System.currentTimeMillis())) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        } else if (pool.any { it.id !in state.progress }) {
            OutlineButton("再学 10 个", { WordPractice.moreToday(today) }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp))
        }
    }
}

@Composable
private fun Stat(label: String, n: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
        Text("$n", style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = AjlTheme.colors.ink)
    }
}

@Composable
private fun TodayRow(jp: String, zh: String, n: Int) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(jp, style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp), color = AjlTheme.colors.ink)
        Text(zh, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = AjlTheme.colors.ink2, modifier = Modifier.weight(1f))
        Text("$n", style = AjlTheme.type.meta.copy(fontSize = 18.sp), color = AjlTheme.colors.ink)
    }
}

@Composable
private fun NewTile(w: WpWord, modifier: Modifier, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val homo = Homophones.groupOf(w.surface) != null
    val edge = if (homo) AjlTheme.work.accent else colors.ink
    Column(
        modifier
            .background(colors.surface, AjlShape.Panel)
            .border(AjlStroke.Ink, edge, AjlShape.Panel)
            .clickableNoRipple(onClick = onClick)
            .semantics { contentDescription = "听 ${w.surface}" }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(w.romaji, style = AjlTheme.type.metaSmall.copy(fontSize = 9.sp), color = colors.ink3, maxLines = 1)
        Text(w.surface, style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp, lineHeight = 21.sp), color = if (homo) AjlTheme.work.accent else colors.ink, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// The round
// ---------------------------------------------------------------------------

@Composable
private fun WordRound(
    session: WpSession,
    pool: List<WpWord>,
    settings: LabSettings,
    audio: LessonAudioController,
    today: Long,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    var index by rememberSaveable { mutableIntStateOf(0) }
    val close = {
        audio.stop()
        WordPractice.close()
    }
    val step = session.steps.getOrNull(index)
    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(
            nav = TopBarNav.Close,
            onNav = close,
            navContentDescription = "结束",
            title = when {
                step == null -> "単語"
                step is WpStep.Card || (step as WpStep.Quiz).fresh -> "新出"
                else -> "復習"
            },
            actions = {
                Text("${(index + 1).coerceAtMost(session.steps.size)} / ${session.steps.size}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3, modifier = Modifier.padding(end = 8.dp))
            },
        )
        ProgressLine(index / session.steps.size.coerceAtLeast(1).toFloat(), Modifier.fillMaxWidth().padding(horizontal = 20.dp), thickness = 2.dp)
        when (step) {
            null -> RoundEnd(session, onDone = close)
            is WpStep.Card -> WordCardStep(step.word, settings, audio, onNext = { index++ })
            is WpStep.Quiz -> androidx.compose.runtime.key(index) {
                QuizStep(step, settings, audio, last = index == session.steps.lastIndex, onAnswer = { right ->
                    WordPractice.answer(step, right, pool, today)
                    StudyLog.record(context, 1, if (right) 1 else 0)
                }, onNext = { index++ })
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RoundEnd(session: WpSession, onDone: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { if (session.asked > 0) StudyLog.finishSession(context) }
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text("${session.right} / ${session.asked}", style = AjlTheme.type.jpDisplay.copy(fontSize = 48.sp, lineHeight = 56.sp), color = AjlTheme.work.accent)
        Text("答对", style = AjlTheme.type.caption, color = AjlTheme.colors.ink2)
        if (session.newWords > 0) Text("新学 ${session.newWords} 个", style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
    }
    InkButton("完成", onDone, modifier = Modifier.fillMaxWidth().padding(16.dp))
}

/** A new word: romaji → kana → kanji, meaning, its anime line, the words that sound the same. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.WordCardStep(w: WpWord, settings: LabSettings, audio: LessonAudioController, onNext: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    LaunchedEffect(w.id) { audio.speakText(w.surface, settings.ttsWorkerUrl) }
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().clickableNoRipple(onClick = { audio.speakText(w.surface, settings.ttsWorkerUrl) }).padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(w.romaji, style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = colors.ink3)
            if (w.reading != w.surface) Text(w.reading, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp), color = work.accent)
            Text(w.surface, style = AjlTheme.type.jpHero, color = colors.ink, textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Text(w.meaning, style = AjlTheme.type.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
            if (w.pos.isNotBlank()) {
                Text(
                    w.pos,
                    style = AjlTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.ink2,
                    modifier = Modifier.background(colors.sunken, RoundedCornerShape(11.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        w.line?.let { line ->
            MangaPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LineVoicePill(line.ja, line.audioUrl, audio, settings.ttsWorkerUrl)
                    val c = w.cloze
                    MarkedLine(line.ja, if (c != null) c.start until c.start + c.cut.length else IntRange.EMPTY, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp))
                }
            }
        }
        Homophones.groupOf(w.surface)?.let { g ->
            val others = g.words.filter { it.surface != w.surface }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("同じ音", style = AjlTheme.type.jpTitle.copy(fontSize = 14.sp), color = colors.ink2)
                others.take(3).forEach { o ->
                    Row(
                        Modifier.clickableNoRipple(onClick = { audio.speakText(o.surface, settings.ttsWorkerUrl) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            o.surface,
                            style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp),
                            color = colors.ink,
                            modifier = Modifier.background(colors.surface, AjlShape.Panel).border(AjlStroke.Ink, colors.ink, AjlShape.Panel).padding(horizontal = 8.dp, vertical = 1.dp),
                        )
                        Text(o.meaning, style = AjlTheme.type.caption, color = colors.ink2, maxLines = 1)
                    }
                }
            }
        }
    }
    InkButton("下一个", onNext, modifier = Modifier.fillMaxWidth().padding(16.dp))
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.QuizStep(
    q: WpStep.Quiz,
    settings: LabSettings,
    audio: LessonAudioController,
    last: Boolean,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val feedback = LocalFeedbackEngine.current
    val w = q.word
    var picked by remember { mutableStateOf<Int?>(null) }
    val sounding = audio.playbackState.phase == AudioPlaybackPhase.Loading || audio.playbackState.phase == AudioPlaybackPhase.Playing
    val sayWord = { audio.speakText(w.surface, settings.ttsWorkerUrl) }
    LaunchedEffect(Unit) {
        when (q.kind) {
            WpKind.Listen -> sayWord()
            WpKind.Line -> w.line?.let { audio.play(lineCue(context, it.ja, it.audioUrl), settings.ttsWorkerUrl) }
            else -> Unit
        }
    }

    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        when (q.kind) {
            WpKind.Listen -> Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VoiceWave(playing = sounding, onClick = { if (sounding) audio.stop() else sayWord() })
                if (picked != null) {
                    Text(w.surface, style = AjlTheme.type.jpHead, color = colors.ink)
                    Text("${w.reading} · ${w.romaji}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
                }
            }
            WpKind.Look, WpKind.LookReading -> Column(
                Modifier.fillMaxWidth().clickableNoRipple(onClick = { if (picked != null) sayWord() }).padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(w.surface, style = AjlTheme.type.jpHero, color = colors.ink, textAlign = TextAlign.Center)
                if (picked != null) Text("${w.reading} · ${w.romaji}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
            }
            WpKind.Line -> LineCloze(q, settings, audio, answered = picked != null)
        }

        q.options.chunked(2).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { c, text ->
                    val i = r * 2 + c
                    OptionTile(
                        text = text,
                        japanese = q.kind == WpKind.Line || q.kind == WpKind.LookReading,
                        state = when {
                            picked == null -> OptionState.Open
                            i == q.answer -> OptionState.Right
                            i == picked -> OptionState.Wrong
                            else -> OptionState.Dim
                        },
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (picked == null) {
                                picked = i
                                val ok = i == q.answer
                                feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                                onAnswer(ok)
                                if (q.kind == WpKind.Look || q.kind == WpKind.LookReading) sayWord()
                            }
                        },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        AnimatedVisibility(picked != null, enter = fadeIn() + slideInVertically { it / 4 }) {
            val ok = picked == q.answer
            Column(
                Modifier.fillMaxWidth().background(colors.infoSoft, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    if (ok) "对了：${w.surface} = ${w.short}" else "是 ${w.surface}（${w.reading}），${w.meaning}",
                    style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = if (ok) colors.ok else colors.bad,
                )
                if (q.kind == WpKind.Line) {
                    Homophones.groupOf(w.surface)?.note?.takeIf { it.isNotBlank() }?.let { NoteText(it, color = colors.ink) }
                }
            }
        }
    }
    if (picked != null) {
        InkButton(if (last) "看结果" else "下一题", onNext, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}

/** The line with the word blanked: its kana in the work colour (a gap for kana words); after the answer, the real line. */
@Composable
private fun LineCloze(q: WpStep.Quiz, settings: LabSettings, audio: LessonAudioController, answered: Boolean) {
    val w = q.word
    val line = w.line ?: return
    val c = w.cloze ?: return
    val furigana = rememberFuriganaAnnotator(settings)
    val gap = c.cutKana.ifEmpty { "＿".repeat(c.cut.length.coerceIn(2, 4)) }
    val shown = if (answered) line.ja else line.ja.substring(0, c.start) + gap + line.ja.substring(c.start + c.cut.length)
    val mark = c.start until c.start + if (answered) c.cut.length else gap.length
    LaunchedEffect(shown, settings.showFurigana, settings.showRomaji) {
        if (settings.showFurigana || settings.showRomaji) furigana.request("sentence", listOf(shown))
    }
    val reading = remember(shown, furigana.resultFor(shown)) { LineReading.build(shown, furigana.resultFor(shown)) }
    MangaPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LineVoicePill(line.ja, line.audioUrl, audio, settings.ttsWorkerUrl)
            ReadingLineText(
                reading,
                mark,
                showRuby = settings.showFurigana,
                showRomaji = settings.showRomaji,
                style = AjlTheme.type.jpBody.copy(fontSize = 21.sp, lineHeight = 34.sp),
                color = AjlTheme.colors.ink,
            )
        }
    }
}

private enum class OptionState { Open, Right, Wrong, Dim }

@Composable
private fun OptionTile(text: String, japanese: Boolean, state: OptionState, modifier: Modifier, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val tint = when (state) {
        OptionState.Open -> colors.ink
        OptionState.Right -> colors.ok
        OptionState.Wrong -> colors.bad
        OptionState.Dim -> colors.ink3
    }
    val edge = when (state) {
        OptionState.Dim -> colors.line2
        else -> tint
    }
    Box(
        modifier
            .heightIn(min = 76.dp)
            .background(colors.surface, AjlShape.Panel)
            .border(if (state == OptionState.Right || state == OptionState.Wrong) 2.dp else AjlStroke.Ink, edge, AjlShape.Panel)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (japanese) AjlTheme.type.jpTitle.copy(fontSize = 22.sp, lineHeight = 30.sp) else AjlTheme.type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
