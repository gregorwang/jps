package com.animejapaneselab.nativeapp.ui.screens.zougo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuPreview
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuScreen
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.zougo.ZgBookData
import com.animejapaneselab.nativeapp.ui.zougo.ZgKind
import com.animejapaneselab.nativeapp.ui.zougo.ZgLesson
import com.animejapaneselab.nativeapp.ui.zougo.ZgQuestion
import com.animejapaneselab.nativeapp.ui.zougo.Zougo
import com.animejapaneselab.nativeapp.ui.zougo.ZougoBook
import com.animejapaneselab.nativeapp.ui.zougo.ZougoRules
import com.animejapaneselab.nativeapp.ui.zougo.ZougoState

const val ZougoBookTitle = "造語"
const val ZougoBookVolume = "第四巻"

/** 自習 → 造語 教科書: the 目次, or the 課 being studied ([ZougoState.lesson]). */
@Composable
fun ZougoStudy(settings: LabSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val book = remember { ZougoBook.load(context) }
    val state by Zougo.state.collectAsState()
    val lesson = state.lesson?.let(book::lesson)
    if (lesson != null) {
        LessonFlow(book, lesson, state, settings, modifier)
    } else {
        ZougoIndex(
            book = book,
            state = state,
            onBack = Zougo::closeBook,
            onLesson = { Zougo.start(it.id) },
            modifier = modifier,
        )
    }
}

private enum class Phase { Learn, Quiz, End }

/** One 課: study (拼合台 / 矩阵) → for a matrix, the 小テスト → つづく (which marks the 課 learned). */
@Composable
private fun LessonFlow(book: ZgBookData, lesson: ZgLesson, state: ZougoState, settings: LabSettings, modifier: Modifier) {
    val context = LocalContext.current
    var phase by rememberSaveable(lesson.id) { mutableStateOf(Phase.Learn) }
    var right by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    var asked by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    val missed = remember(lesson.id) { mutableStateListOf<TsuzukuLine>() }
    val quiz = remember(lesson.id, phase == Phase.Quiz) { if (phase == Phase.Quiz) ZougoRules.lessonQuiz(lesson) else emptyList() }
    val eyebrow = "第四巻 造語 · 第 ${lesson.number} 課"
    when (phase) {
        Phase.Learn -> when (lesson.kind) {
            ZgKind.Fuse -> FuseSitting(
                items = lesson.fuse,
                key = lesson.id,
                eyebrow = eyebrow,
                title = lesson.title,
                settings = settings,
                onClose = Zougo::exitLesson,
                onAnswer = { item, ok -> Zougo.answer(context, item.word, ok) },
                onDone = { r, wrong ->
                    right = r; asked = lesson.fuse.size
                    missed.clear(); missed.addAll(wrong.map { TsuzukuLine(it.word, true, it.romaji) })
                    Zougo.markLearned(context, lesson.id)
                    phase = Phase.End
                },
                modifier = modifier,
            )
            ZgKind.Matrix -> MatrixSitting(
                lesson = lesson,
                settings = settings,
                onClose = Zougo::exitLesson,
                onTest = { phase = Phase.Quiz },
                modifier = modifier,
            )
        }

        Phase.Quiz -> ZougoQuiz(
            questions = quiz,
            eyebrow = eyebrow,
            title = "小テスト",
            settings = settings,
            onClose = { phase = Phase.Learn },
            onDone = { r, wrong ->
                right = r; asked = quiz.size
                missed.clear(); missed.addAll(wrong.map(::tsuzukuLineOf))
                Zougo.markLearned(context, lesson.id)
                phase = Phase.End
            },
            modifier = modifier,
        )

        Phase.End -> {
            val next = book.lessons.dropWhile { it.id != lesson.id }.drop(1).firstOrNull { it.id !in state.learned }
                ?: ZougoRules.next(book, state)?.takeIf { it.id != lesson.id }
            TsuzukuScreen(
                eyebrow = "第 ${lesson.number} 課 · ${lesson.title}",
                tally = "答对 $right / $asked",
                meta = "$ZougoBookVolume $ZougoBookTitle · 已学 ${state.learned.size} / ${book.lessons.size} 課",
                noted = missed.toList(),
                notedTitle = "再看一眼的 ${missed.size} 个",
                preview = next?.let { n ->
                    TsuzukuPreview(title = "第 ${n.number} 課 · ${n.title}", meta = n.gloss.ifBlank { null }, line = n.words.take(4).joinToString("・"))
                },
                primaryLabel = if (next != null) "次の課" else "回到目次",
                onPrimary = { if (next != null) Zougo.start(next.id) else Zougo.exitLesson() },
                onClose = Zougo::exitLesson,
                modifier = modifier,
            )
        }
    }
}

private fun tsuzukuLineOf(q: ZgQuestion): TsuzukuLine = when (q) {
    is ZgQuestion.Reading -> TsuzukuLine(q.item.word, true, q.item.romaji)
    is ZgQuestion.Meaning -> TsuzukuLine(q.cell.word, true, q.cell.meaning)
}

// ------------------------------------------------------------------ 目次

@Composable
private fun ZougoIndex(book: ZgBookData, state: ZougoState, onBack: () -> Unit, onLesson: (ZgLesson) -> Unit, modifier: Modifier) {
    BackHandler(onBack = onBack)
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val next = ZougoRules.next(book, state)
    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(nav = TopBarNav.Back, onNav = onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            BookCover(ZougoBookVolume, ZougoBookTitle, "ことばの組み立て", learned = book.lessons.count { it.id in state.learned }, total = book.lessons.size)
            book.sections.forEachIndexed { s, section ->
                val lessons = book.lessons.filter { it.section == s }
                SectionHeading(
                    title = "${section.number}　${section.title}",
                    meta = section.zh,
                    modifier = Modifier.padding(top = 24.dp),
                )
                lessons.forEach { lesson ->
                    val learned = lesson.id in state.learned
                    val isNext = lesson.id == next?.id
                    val seen = state.seenIn(lesson)
                    val total = lesson.words.size
                    val (meta, metaColor) = when {
                        learned -> "済" to colors.ok
                        lesson.kind == ZgKind.Matrix && seen > 0 -> "$seen/$total" to (if (isNext) work.accent else colors.ink3)
                        else -> "$total 词" to (if (isNext) work.accent else colors.ink3)
                    }
                    Column(Modifier.fillMaxWidth().clickableNoRipple({ onLesson(lesson) }).semantics { role = Role.Button }) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("%02d".format(lesson.number), style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = if (learned) colors.faint else if (isNext) work.accent else colors.ink, modifier = Modifier.width(26.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(lesson.title, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = if (learned) colors.ink2 else colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(lesson.words.take(3).joinToString("・"), style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(meta, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = metaColor)
                        }
                        Box(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(colors.line))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (next != null) {
            InkButton(
                text = "続き · 第 ${next.number} 課",
                onClick = { onLesson(next) },
                caption = "${next.title} · ${if (next.kind == ZgKind.Fuse) "拼合" else "矩阵"}",
                trailingArrow = true,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            )
        }
    }
}

/** The 教科書 cover on top of a 目次: spine in the work colour, volume, title, subtitle, 已学 line. */
@Composable
internal fun BookCover(volume: String, title: String, sub: String, learned: Int, total: Int) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    Box(Modifier.fillMaxWidth().height(132.dp).clip(shape).background(work.soft).border(AjlStroke.Ink, colors.ink, shape)) {
        Box(Modifier.fillMaxHeight().width(14.dp).background(work.accent))
        Box(Modifier.fillMaxHeight().padding(start = 14.dp).width(AjlStroke.Ink).background(colors.ink))
        Screentone(Modifier.align(Alignment.BottomEnd).offset(x = 30.dp, y = (-10).dp).size(200.dp, 80.dp).rotate(-12f), color = work.tone(0.34f))
        Column(Modifier.padding(start = 30.dp, end = 18.dp, top = 14.dp, bottom = 14.dp).fillMaxHeight()) {
            Eyebrow("$volume · 教科書", color = colors.ink3)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text(title, style = AjlTheme.type.title.copy(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 1.sp), color = colors.ink)
                Text(sub, style = AjlTheme.type.jpBody.copy(fontSize = 13.sp), color = colors.ink2, modifier = Modifier.padding(bottom = 5.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressLine(if (total == 0) 0f else learned.toFloat() / total, Modifier.weight(1f), trackColor = colors.ink.copy(alpha = 0.12f))
                Text("已学 $learned / $total", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink2)
            }
        }
    }
}

// ------------------------------------------------------------------ 練習

/** 練習 → 第四巻 造語: a practice over the learned 課, then つづく. */
@Composable
fun ZougoPractice(questions: List<ZgQuestion>, settings: LabSettings, onExit: () -> Unit, modifier: Modifier = Modifier) {
    var done by remember(questions) { mutableStateOf<Pair<Int, List<ZgQuestion>>?>(null) }
    val result = done
    if (result == null) {
        ZougoQuiz(
            questions = questions,
            eyebrow = "練習 · $ZougoBookVolume $ZougoBookTitle",
            title = "已学的課",
            settings = settings,
            onClose = onExit,
            onDone = { r, wrong -> done = r to wrong },
            modifier = modifier,
        )
    } else {
        TsuzukuScreen(
            eyebrow = "練習 · $ZougoBookTitle",
            tally = "答对 ${result.first} / ${questions.size}",
            meta = "$ZougoBookVolume $ZougoBookTitle",
            noted = result.second.map(::tsuzukuLineOf),
            notedTitle = "再看一眼的 ${result.second.size} 个",
            primaryLabel = "完成",
            onPrimary = onExit,
            onClose = onExit,
            modifier = modifier,
        )
    }
}
