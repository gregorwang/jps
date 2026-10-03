package com.animejapaneselab.nativeapp.ui.screens.kyoka

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.kyoka.KkBook
import com.animejapaneselab.nativeapp.ui.kyoka.KkLesson
import com.animejapaneselab.nativeapp.ui.kyoka.Kyoka
import com.animejapaneselab.nativeapp.ui.kyoka.KyokaBooks
import com.animejapaneselab.nativeapp.ui.kyoka.KyokaState
import com.animejapaneselab.nativeapp.ui.katsuyou.KyStep
import com.animejapaneselab.nativeapp.ui.screens.katsuyou.KyTableScreen
import com.animejapaneselab.nativeapp.ui.screens.katsuyou.PeekScreen
import com.animejapaneselab.nativeapp.ui.screens.katsuyou.StepSitting
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuPreview
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuScreen
import com.animejapaneselab.nativeapp.ui.screens.zougo.BookCover
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** 自習 → 第五巻 起的教科書: the 目次, its まとめ, or the 課 being played. */
@Composable
fun KyokaStudy(settings: LabSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val data = remember { KyokaBooks.load(context) }
    val state by Kyoka.state.collectAsState()
    val book = state.book?.let(data::book)
    if (book == null) {
        LaunchedEffect(state.book) { Kyoka.closeBook() }
        return
    }
    val lesson = state.lesson?.let(book::lesson)
    val table = book.table
    when {
        lesson != null -> KyokaLesson(book, lesson, state, settings, modifier)
        state.summary && table != null -> KyTableScreen(table, "${book.volume} ${book.title} · まとめ", settings, modifier, onBack = Kyoka::closeSummary)
        else -> KyokaIndex(book, state, modifier)
    }
}

private fun eyebrowOf(book: KkBook, lesson: KkLesson) = "${book.volume} ${book.title} · 第 ${lesson.number} 課"

/** One 課: 課前の一眼 → its plays in order → つづく (marks it learned). */
@Composable
private fun KyokaLesson(book: KkBook, lesson: KkLesson, state: KyokaState, settings: LabSettings, modifier: Modifier) {
    val context = LocalContext.current
    val play = lesson.play
    val eyebrow = eyebrowOf(book, lesson)
    // -1 = 課前の一眼, steps.size = つづく.
    var step by rememberSaveable(lesson.id) { mutableIntStateOf(if (play.peek != null) -1 else 0) }
    var right by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    var asked by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    val missed = remember(lesson.id) { mutableStateListOf<TsuzukuLine>() }
    val onClose = Kyoka::exitLesson

    when {
        step < 0 -> PeekScreen(play.peek!!, eyebrow, lesson.title, play.steps.first().count, onClose, onStart = { step = 0 }, modifier = modifier)

        step < play.steps.size -> {
            val s = play.steps[step]
            val nextTitle = play.steps.getOrNull(step + 1)?.title
            StepSitting(
                step = s,
                key = "kk:${lesson.id}:$step",
                eyebrow = eyebrow,
                title = s.title.ifBlank { lesson.title },
                settings = settings,
                onClose = onClose,
                onAnswer = { ok -> Kyoka.answer(context, ok) },
                onDone = { r, n, wrong ->
                    right += r; asked += n
                    missed.addAll(wrong)
                    step++
                },
                lastLabel = nextTitle?.let { "接着：$it" } ?: "完成",
                modifier = modifier,
            )
        }

        else -> {
            LaunchedEffect(lesson.id) { Kyoka.markLearned(context, lesson.id) }
            val learned = state.learned + lesson.id
            val next = book.lessons.dropWhile { it.id != lesson.id }.drop(1).firstOrNull { it.id !in learned }
                ?: book.lessons.firstOrNull { it.id !in learned }
            TsuzukuScreen(
                eyebrow = "第 ${lesson.number} 課 · ${lesson.title}",
                tally = if (asked > 0) "答对 $right / $asked" else "",
                meta = "${book.volume} ${book.title} · 已学 ${book.lessons.count { it.id in learned }} / ${book.lessons.size} 課",
                noted = missed.toList(),
                notedTitle = "再看一眼的 ${missed.size} 个",
                preview = next?.let { n ->
                    TsuzukuPreview(title = "第 ${n.number} 課 · ${n.title}", meta = n.gloss.ifBlank { null }, line = n.play.preview.joinToString("・").ifBlank { null })
                },
                primaryLabel = if (next != null) "次の課" else "回到目次",
                onPrimary = { if (next != null) Kyoka.start(next.id) else Kyoka.exitLesson() },
                onClose = onClose,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun KyokaIndex(book: KkBook, state: KyokaState, modifier: Modifier) {
    BackHandler(onBack = Kyoka::closeBook)
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val next = book.lessons.firstOrNull { it.id !in state.learned }
    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(nav = TopBarNav.Back, onNav = Kyoka::closeBook)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            BookCover(book.volume, book.title, book.sub, learned = book.lessons.count { it.id in state.learned }, total = book.lessons.size)
            val sections = book.sections.ifEmpty { listOf(null) }
            sections.forEachIndexed { s, section ->
                if (section != null) {
                    SectionHeading(title = "${section.no}　${section.title}", meta = section.zh, modifier = Modifier.padding(top = 24.dp))
                } else {
                    Spacer(Modifier.height(16.dp))
                }
                book.lessons.filter { section == null || it.section == s }.forEach { lesson ->
                    val learned = lesson.id in state.learned
                    val isNext = lesson.id == next?.id
                    IndexRow(
                        number = "%02d".format(lesson.number),
                        title = lesson.title,
                        sub = lesson.gloss,
                        meta = if (learned) "済" else "${lesson.play.count} 题",
                        metaColor = when {
                            learned -> colors.ok
                            isNext -> work.accent
                            else -> colors.ink3
                        },
                        numberColor = if (learned) colors.faint else if (isNext) work.accent else colors.ink,
                        dim = learned,
                        onClick = { Kyoka.start(lesson.id) },
                    )
                }
            }
            if (book.table != null) {
                SectionHeading(title = "まとめ", meta = "做完再看", modifier = Modifier.padding(top = 24.dp))
                IndexRow(number = "表", title = book.table.title, sub = "", meta = "→", metaColor = colors.ink3, numberColor = colors.ink, dim = false, onClick = Kyoka::openSummary)
            }
            Spacer(Modifier.height(16.dp))
        }
        if (next != null) {
            InkButton(
                text = "続き · 第 ${next.number} 課",
                onClick = { Kyoka.start(next.id) },
                caption = "${next.title} · ${next.play.count} 题",
                trailingArrow = true,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun IndexRow(
    number: String,
    title: String,
    sub: String,
    meta: String,
    metaColor: androidx.compose.ui.graphics.Color,
    numberColor: androidx.compose.ui.graphics.Color,
    dim: Boolean,
    onClick: () -> Unit,
) {
    val colors = AjlTheme.colors
    Column(Modifier.fillMaxWidth().clickableNoRipple(onClick).semantics { role = Role.Button }) {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(number, style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = numberColor, modifier = Modifier.width(26.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = if (dim) colors.ink2 else colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sub.isNotBlank()) Text(sub, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(meta, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = metaColor)
        }
        Box(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(colors.line))
    }
}

// ------------------------------------------------------------------ 練習

/** 練習 → 文法: the drawn steps one after another, then つづく. */
@Composable
fun KyokaPractice(steps: List<KyStep>, bookId: String?, settings: LabSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val data = remember { KyokaBooks.load(context) }
    val name = bookId?.let(data::book)?.let { "${it.volume} ${it.title}" } ?: "教科書"
    var step by remember(steps) { mutableIntStateOf(0) }
    var right by remember(steps) { mutableIntStateOf(0) }
    var asked by remember(steps) { mutableIntStateOf(0) }
    val missed = remember(steps) { mutableStateListOf<TsuzukuLine>() }
    val onClose = Kyoka::endPractice
    BackHandler(onBack = onClose)

    if (step < steps.size) {
        val s = steps[step]
        StepSitting(
            step = s,
            key = "kkp:${System.identityHashCode(steps)}:$step",
            eyebrow = "練習 · $name",
            title = s.title.ifBlank { "已学的課" },
            settings = settings,
            onClose = onClose,
            onAnswer = { ok -> Kyoka.answer(context, ok) },
            onDone = { r, n, wrong ->
                right += r; asked += n
                missed.addAll(wrong)
                step++
            },
            lastLabel = steps.getOrNull(step + 1)?.title?.let { "接着：$it" } ?: "完成",
            modifier = modifier,
        )
    } else {
        TsuzukuScreen(
            eyebrow = "練習 · $name",
            tally = if (asked > 0) "答对 $right / $asked" else "",
            meta = "已学的課",
            noted = missed.toList(),
            notedTitle = "再看一眼的 ${missed.size} 个",
            primaryLabel = "完成",
            onPrimary = onClose,
            onClose = onClose,
            modifier = modifier,
        )
    }
}
