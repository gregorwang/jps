package com.animejapaneselab.nativeapp.ui.screens.library

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LevelDict
import com.animejapaneselab.nativeapp.data.PromptAudio
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
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.words.HomoGroup
import com.animejapaneselab.nativeapp.ui.words.HomoQuestion
import com.animejapaneselab.nativeapp.ui.words.HomoWord
import com.animejapaneselab.nativeapp.ui.words.HomophoneRoom
import com.animejapaneselab.nativeapp.ui.words.HomophoneRules
import com.animejapaneselab.nativeapp.ui.words.Homophones
import com.animejapaneselab.nativeapp.ui.words.TangoLine
import com.animejapaneselab.nativeapp.ui.words.TangoLines
import com.animejapaneselab.nativeapp.ui.voicepack.VoicePack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// 辞書 → 词汇 → 同音: one row per reading, its kanji side by side
// ---------------------------------------------------------------------------

/** The 同音 list of the 辞書 词汇 tab for [level]. Tap a row → its room; the first row starts a mixed round. */
@Composable
internal fun HomophonePage(level: String, modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val groups by produceState(Homophones.peek()) { if (value == null) value = withContext(Dispatchers.Default) { Homophones.load(appContext) } }
    val levels = rememberLevelIndex()
    var filter by rememberSaveable { mutableStateOf(HomophoneRules.Filter.All) }
    val atLevel = remember(groups, levels, level) {
        HomophoneRules.atLevel(groups.orEmpty(), { levels[it] }, level)
    }
    val shown = remember(atLevel, filter) { atLevel.filter { HomophoneRules.matches(it, filter) } }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomophoneRules.Filter.entries.forEach { f ->
                FilterChip(f.label, f == filter) { filter = f }
            }
        }
        if (groups != null && shown.isEmpty()) {
            EmptyNote("この級には ない", gloss = "换个级别看看")
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            if (atLevel.isNotEmpty()) {
                item(key = "round") {
                    RoundRow(count = atLevel.size, onClick = { HomophoneRoom.openRound(level) })
                }
            }
            items(shown, key = { it.reading }) { g ->
                GroupRow(g, onClick = { HomophoneRoom.openRoom(g.reading) })
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, on: Boolean, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val shape = RoundedCornerShape(15.dp)
    Text(
        label,
        style = AjlTheme.type.caption.copy(fontSize = 12.sp),
        color = if (on) colors.bg else colors.ink2,
        modifier = Modifier
            .heightIn(min = 30.dp)
            .background(if (on) colors.ink else colors.bg, shape)
            .border(AjlStroke.Hair, if (on) colors.ink else colors.line2, shape)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun RoundRow(count: Int, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickableNoRipple(onClick = onClick)
                .semantics { contentDescription = "听原声猜字，这一级 $count 组" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("听原声猜字", style = AjlTheme.type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = AjlTheme.work.accent)
                Text("这一级 $count 组，随机 10 句", style = AjlTheme.type.caption, color = colors.ink3)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AjlTheme.work.accent)
        }
        Hairline()
    }
}

@Composable
private fun GroupRow(group: HomoGroup, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick = onClick)
            .semantics { contentDescription = "${group.reading}：${group.words.joinToString("、") { it.surface }}" }
            .padding(top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(group.romaji, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
                Text(group.reading, style = AjlTheme.type.jpTitle.copy(fontSize = 24.sp, lineHeight = 30.sp), color = AjlTheme.work.accent)
            }
            Spacer(Modifier.weight(1f))
            Text("${group.words.size} 個", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3, modifier = Modifier.padding(bottom = 4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.words.forEach { w -> KanjiTile(w, Modifier.weight(1f)) }
        }
    }
    Hairline()
}

@Composable
private fun KanjiTile(word: HomoWord, modifier: Modifier = Modifier, current: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(
        modifier
            .background(if (current) work.soft else colors.surface, AjlShape.Panel)
            .border(AjlStroke.Ink, if (current) work.accent else colors.ink, AjlShape.Panel)
            .then(if (onClick != null) Modifier.clickableNoRipple(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(word.surface, style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, lineHeight = 26.sp), color = colors.ink, maxLines = 1)
        Text(word.meaning, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink2, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// 词卡 → 同じ音 row
// ---------------------------------------------------------------------------

/** Under a word card: the other words read the same, and the way into their room. */
@Composable
internal fun SameSoundRow(surface: String) {
    val appContext = LocalContext.current.applicationContext
    val groups by produceState(Homophones.peek()) { if (value == null) value = withContext(Dispatchers.Default) { Homophones.load(appContext) } }
    val group = remember(groups, surface) { if (groups == null) null else Homophones.groupOf(surface) } ?: return
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Hairline()
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("同じ音", style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp), color = colors.ink)
            Text("${group.romaji} · 还有 ${group.words.size - 1} 个", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.words.forEach { w ->
                KanjiTile(w, Modifier.weight(1f), current = w.surface == surface, onClick = { HomophoneRoom.openRoom(group.reading) })
            }
        }
        Row(
            Modifier.heightIn(min = 44.dp).clickableNoRipple(onClick = { HomophoneRoom.openRoom(group.reading) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("进同音の部屋", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = AjlTheme.work.accent)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AjlTheme.work.accent)
        }
    }
}

// ---------------------------------------------------------------------------
// The room and the quiz, over everything (LabApp hosts it)
// ---------------------------------------------------------------------------

@Composable
fun HomophoneRoomHost(settings: LabSettings) {
    val request by HomophoneRoom.open.collectAsState()
    val req = request ?: return
    val appContext = LocalContext.current.applicationContext
    val data by produceState<Pair<List<HomoGroup>, Map<String, TangoLine>>?>(null) {
        value = withContext(Dispatchers.Default) { Homophones.load(appContext) to TangoLines.load(appContext) }
    }
    val levels = rememberLevelIndex()
    val audio = rememberLessonAudioController()
    Dialog(
        onDismissRequest = {
            audio.stop()
            HomophoneRoom.close()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(AjlTheme.colors.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
            val (groups, lines) = data ?: return@Box
            val close = {
                audio.stop()
                HomophoneRoom.close()
            }
            when (req) {
                is HomophoneRoom.Request.Room -> {
                    val group = groups.firstOrNull { it.reading == req.reading }
                    if (group == null) {
                        LaunchedEffect(Unit) { HomophoneRoom.close() }
                    } else {
                        RoomFlow(group, lines, levels, settings, audio, close)
                    }
                }
                is HomophoneRoom.Request.Round -> {
                    val questions = remember(req) {
                        HomophoneRules.mixedRound(HomophoneRules.atLevel(groups, { levels[it] }, req.level), lines, System.currentTimeMillis())
                    }
                    QuizFlow("${req.level} · 混合", questions, settings, audio, onDone = close, onClose = close)
                }
            }
        }
    }
}

/** id → JLPT level of every 辞書 word (原作 + 高频补充). */
@Composable
private fun rememberLevelIndex(): Map<String, String> {
    val appContext = LocalContext.current.applicationContext
    val loaded by produceState(LevelDict.peek()) { if (value == null) value = withContext(Dispatchers.Default) { LevelDict.load(appContext) } }
    return remember(loaded) {
        loaded?.let { d -> (d.vocab + d.freqVocab).associate { it.id to it.level } }.orEmpty()
    }
}

@Composable
private fun RoomFlow(
    group: HomoGroup,
    lines: Map<String, TangoLine>,
    levels: Map<String, String>,
    settings: LabSettings,
    audio: LessonAudioController,
    onClose: () -> Unit,
) {
    var quizzing by rememberSaveable(group.reading) { mutableStateOf(false) }
    val questions = remember(group, lines) { HomophoneRules.roomQuestions(group, lines, System.currentTimeMillis()) }
    if (quizzing && questions.isNotEmpty()) {
        QuizFlow(group.reading, questions, settings, audio, onDone = { quizzing = false }, onClose = onClose)
    } else {
        RoomScreen(group, lines, levels, settings, audio, canQuiz = questions.isNotEmpty(), onQuiz = { quizzing = true }, onClose = onClose)
    }
}

@Composable
private fun RoomScreen(
    group: HomoGroup,
    lines: Map<String, TangoLine>,
    levels: Map<String, String>,
    settings: LabSettings,
    audio: LessonAudioController,
    canQuiz: Boolean,
    onQuiz: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    var playingId by remember { mutableStateOf<String?>(null) }
    val sounding = audio.playbackState.phase == AudioPlaybackPhase.Loading || audio.playbackState.phase == AudioPlaybackPhase.Playing
    fun play(id: String, action: () -> Unit) {
        if (sounding && playingId == id) {
            audio.stop()
        } else {
            playingId = id
            action()
        }
    }

    var selectedId by rememberSaveable(group.reading) { mutableStateOf(group.words.firstOrNull()?.id) }
    var dropKey by remember { mutableIntStateOf(0) }
    var pickKey by remember { mutableIntStateOf(0) }
    val lineOf = { w: HomoWord -> lines[w.id]?.takeIf { w.cut.isNotEmpty() && w.cut in it.ja } }

    Column(Modifier.fillMaxSize()) {
        TopBar(nav = TopBarNav.Close, onNav = onClose, navContentDescription = "关闭", title = "同音の部屋")
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomophonePond(
                group,
                selectedId = selectedId,
                dropKey = dropKey,
                pickKey = pickKey,
                onStone = {
                    dropKey++
                    play("reading") { audio.speakText(sayReading(context, group), settings.ttsWorkerUrl) }
                },
                onWord = { w ->
                    selectedId = w.id
                    pickKey++
                    lineOf(w)?.let { line -> play(w.id) { playLine(line, audio, settings.ttsWorkerUrl) } }
                },
            )
            PondLegend(group)
            if (group.note.isNotBlank()) NoteText(group.note, modifier = Modifier.fillMaxWidth())
            group.words.firstOrNull { it.id == selectedId }?.let { w ->
                WordCaption(
                    w,
                    sameWord = group.sameWord.any { c -> c.any { it.id == w.id } },
                    line = lineOf(w),
                    level = levels[w.id],
                    playing = sounding && playingId == w.id,
                    onPlay = { line -> play(w.id) { playLine(line, audio, settings.ttsWorkerUrl) } },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (canQuiz) {
            InkButton("听原声，猜波纹停在哪个字", onQuiz, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}

/** The picked word: kanji, meaning, which ring it is on, and its line from the anime. */
@Composable
private fun WordCaption(
    w: HomoWord,
    sameWord: Boolean,
    line: TangoLine?,
    level: String?,
    playing: Boolean,
    onPlay: (TangoLine) -> Unit,
) {
    val colors = AjlTheme.colors
    MangaPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(w.surface, style = AjlTheme.type.jpTitle.copy(fontSize = 28.sp, lineHeight = 36.sp), color = colors.ink)
                Text(w.meaning, style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, modifier = Modifier.weight(1f).padding(bottom = 5.dp))
                level?.takeIf { it.startsWith("N") }?.let {
                    Text(it, style = AjlTheme.type.metaSmall.copy(fontSize = 11.sp), color = AjlTheme.work.accent, modifier = Modifier.padding(bottom = 7.dp))
                }
            }
            Text(
                if (sameWord) "同一个词 · 换字" else "碰巧同音",
                style = AjlTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = if (sameWord) colors.info else colors.ink2,
                modifier = Modifier.background(if (sameWord) colors.infoSoft else colors.sunken, RoundedCornerShape(11.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
            if (line != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VoiceWave(playing = playing, onClick = { onPlay(line) }, synthetic = line.audioUrl.isEmpty())
                    val start = line.ja.indexOf(w.cut)
                    MarkedLine(
                        line.ja,
                        start until start + w.cut.length,
                        Modifier.weight(1f).clickableNoRipple(onClick = { onPlay(line) }),
                        style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 23.sp),
                    )
                }
            }
        }
    }
}

/** Hear the line, the word is a kana blank, pick its kanji. [onDone] after the last one; [onClose] = ✕. */
@Composable
private fun QuizFlow(
    title: String,
    questions: List<HomoQuestion>,
    settings: LabSettings,
    audio: LessonAudioController,
    onDone: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val feedback = LocalFeedbackEngine.current
    val furigana = rememberFuriganaAnnotator(settings)
    var index by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    var right by rememberSaveable { mutableIntStateOf(0) }
    val sounding = audio.playbackState.phase == AudioPlaybackPhase.Loading || audio.playbackState.phase == AudioPlaybackPhase.Playing

    Column(Modifier.fillMaxSize()) {
        TopBar(nav = TopBarNav.Close, onNav = onClose, navContentDescription = "关闭", title = "同音の部屋 · $title", actions = {
            Text("${(index + 1).coerceAtMost(questions.size)} / ${questions.size}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3, modifier = Modifier.padding(end = 8.dp))
        })
        ProgressLine((index + if (picked != null) 1 else 0) / questions.size.coerceAtLeast(1).toFloat(), Modifier.fillMaxWidth().padding(horizontal = 20.dp))

        if (questions.isEmpty() || index >= questions.size) {
            LaunchedEffect(Unit) { if (questions.isNotEmpty()) StudyLog.finishSession(context) }
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Text("${right} / ${questions.size}", style = AjlTheme.type.jpDisplay.copy(fontSize = 48.sp, lineHeight = 56.sp), color = work.accent)
                Text(if (questions.isEmpty()) "这一级还没有能听的句子" else "听对的句数", style = AjlTheme.type.caption, color = colors.ink2)
            }
            InkButton("完成", onDone, modifier = Modifier.fillMaxWidth().padding(16.dp))
            return@Column
        }

        val q = questions[index]
        LaunchedEffect(index) {
            picked = null
            playLine(q.line, audio, settings.ttsWorkerUrl)
        }
        val shownText = if (picked == null) q.heard else q.line.ja
        val mark = if (picked == null) q.start until q.start + q.word.cutKana.length else q.start until q.start + q.word.cut.length
        LaunchedEffect(shownText, settings.showFurigana, settings.showRomaji) {
            if (settings.showFurigana || settings.showRomaji) furigana.request("sentence", listOf(shownText))
        }
        val reading = remember(shownText, furigana.resultFor(shownText)) { LineReading.build(shownText, furigana.resultFor(shownText)) }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MangaPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceWave(
                            playing = sounding,
                            onClick = { if (sounding) audio.stop() else playLine(q.line, audio, settings.ttsWorkerUrl) },
                            synthetic = q.line.audioUrl.isEmpty(),
                        )
                        Text(voiceLabel(context, q.line), style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
                    }
                    ReadingLineText(
                        reading,
                        mark,
                        showRuby = settings.showFurigana,
                        showRomaji = settings.showRomaji,
                        style = AjlTheme.type.jpBody.copy(fontSize = 21.sp, lineHeight = 34.sp),
                        color = colors.ink,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                q.group.words.forEachIndexed { i, w ->
                    val answered = picked != null
                    val tint = when {
                        !answered -> colors.ink
                        i == q.answer -> colors.ok
                        i == picked -> colors.bad
                        else -> colors.ink3
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 92.dp)
                            .background(colors.surface, AjlShape.Panel)
                            .border(if (answered && (i == q.answer || i == picked)) 2.dp else AjlStroke.Ink, if (answered && i != q.answer && i != picked) colors.line2 else tint, AjlShape.Panel)
                            .clickableNoRipple(onClick = {
                                if (picked == null) {
                                    picked = i
                                    val ok = i == q.answer
                                    if (ok) right++
                                    feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                                    StudyLog.record(context, 1, if (ok) 1 else 0)
                                }
                            })
                            .semantics { contentDescription = if (answered) "${w.surface}，${w.meaning}" else w.surface }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        Text(w.surface, style = AjlTheme.type.jpTitle.copy(fontSize = 26.sp, lineHeight = 32.sp), color = tint, textAlign = TextAlign.Center, maxLines = 1)
                        if (answered) Text(w.meaning, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink2, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
            }
            AnimatedVisibility(picked != null, enter = fadeIn() + slideInVertically { it / 4 }) {
                val p = picked ?: 0
                val ok = p == q.answer
                Column(
                    Modifier.fillMaxWidth().background(colors.infoSoft, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        if (ok) "对了：${q.word.surface} = ${q.word.meaning}" else "是 ${q.word.surface}（${q.word.meaning}），不是 ${q.group.words.getOrNull(p)?.surface.orEmpty()}",
                        style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = if (ok) colors.ok else colors.bad,
                    )
                    if (q.group.note.isNotBlank()) NoteText(q.group.note, color = colors.ink)
                }
            }
        }
        if (picked != null) {
            InkButton(if (index == questions.lastIndex) "看结果" else "下一句", { index++ }, modifier = Modifier.fillMaxWidth().padding(16.dp))
        } else {
            OutlineButton("再听一遍", { playLine(q.line, audio, settings.ttsWorkerUrl) }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp))
        }
    }
}

private fun voiceLabel(context: Context, line: TangoLine): String = when {
    line.audioUrl.isNotEmpty() -> "原声"
    VoicePack.fileFor(context, line.ja) != null -> "エミリア"
    else -> "合成"
}

/** All of the group's words sound alike: say one the Emilia pack has (bare kana readings mostly aren't in it). */
private fun sayReading(context: Context, group: HomoGroup): String =
    group.words.firstOrNull { VoicePack.fileFor(context, it.surface) != null }?.surface ?: group.reading

private fun playLine(line: TangoLine, audio: LessonAudioController, ttsWorkerUrl: String) {
    if (line.audioUrl.isNotEmpty()) {
        audio.play(
            PromptAudio.Source(line.audioUrl, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = line.ja),
            ttsWorkerUrl,
        )
    } else {
        audio.speakText(line.ja, ttsWorkerUrl)
    }
}
