package com.animejapaneselab.nativeapp.ui.screens.jishu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.ui.design.EmphasisText
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.VerticalText
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.design.solidShadow
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillRules
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillViewModel
import com.animejapaneselab.nativeapp.ui.drill.DrillMode
import com.animejapaneselab.nativeapp.ui.drill.DrillPhase
import com.animejapaneselab.nativeapp.ui.jishu.JishuPage
import com.animejapaneselab.nativeapp.ui.jishu.JishuState
import com.animejapaneselab.nativeapp.ui.jishu.JishuViewModel
import com.animejapaneselab.nativeapp.ui.screens.session.ConjugationSession
import com.animejapaneselab.nativeapp.ui.screens.session.ConjugationSessionActions
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuPreview
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuScreen
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.ProvideWorkTheme

/** The 自習 material is Re:ゼロ's voiced lines, so the tab wears its 菫. */
private const val JishuWork = "re-zero"

/**
 * 自習 tab: learn a grammar point from anime lines before it is drilled. 首页 (今日の自習 +
 * 教科書 shelf) → 目次 → a sitting (板書, then scene cards) → つづく; the つづく's 小テスト runs
 * the 課's 練習 in place. Finishing a sitting marks the 課 learned, which lets its lines into 練習.
 */
@Composable
fun JishuScreen(ttsWorkerUrl: String, modifier: Modifier = Modifier) {
    val drill: ConjugationDrillViewModel = viewModel()
    val jishu: JishuViewModel = viewModel()
    val drillState by drill.state.collectAsState()
    val state by jishu.state.collectAsState()
    LaunchedEffect(Unit) { drill.ensureLoaded() }

    fun start(point: String) {
        val s = drill.state.value
        val lines = s.linesOf(point)
        val studied = jishu.state.value.studiedIn(point, lines)
        val total = lines.distinctBy { it.sentenceId }.size
        // The 板書 opens a 課 the first time and a full re-read; continuing goes straight to the lines.
        jishu.startSitting(point, lines, s.items, s.progress, hasBoard = studied == 0 || studied >= total)
    }

    ProvideWorkTheme(JishuWork) {
        val sitting = state.sitting
        when {
            drillState.mode == DrillMode.Lesson && drillState.session.isNotEmpty() -> ConjugationSession(
                state = drillState,
                actions = ConjugationSessionActions(
                    onAnswer = drill::answer,
                    onNext = drill::next,
                    onRestart = drill::startLesson,
                    onExit = drill::exitSession,
                    onNextLesson = {
                        val next = drillState.nextLesson(drillState.openBook) ?: drillState.nextLesson()
                        drill.exitSession()
                        next?.let(::start)
                    },
                ),
                ttsWorkerUrl = ttsWorkerUrl,
                modifier = modifier,
            )

            sitting != null && sitting.isComplete -> {
                LaunchedEffect(sitting.pointId) { if (sitting.remembered > 0) drill.markLearned(sitting.pointId) }
                SittingEnd(
                    state = state,
                    drill = drillState,
                    onContinue = { point -> start(point) },
                    onTest = { point ->
                        jishu.endSitting()
                        drill.startLessonFor(point)
                    },
                    onClose = jishu::endSitting,
                    modifier = modifier,
                )
            }

            sitting != null -> JishuSittingScreen(
                sitting = sitting,
                drill = drillState,
                context = state.context,
                cover = state.cover,
                ttsWorkerUrl = ttsWorkerUrl,
                actions = SittingActions(
                    onExit = jishu::endSitting,
                    onBack = jishu::back,
                    onNext = jishu::next,
                    onRemember = jishu::remember,
                    onAgain = jishu::again,
                    onToggleCover = jishu::toggleCover,
                ),
                modifier = modifier,
            )

            state.openBook != null -> TextbookIndex(
                group = state.openBook.orEmpty(),
                drill = drillState,
                state = state,
                onBack = jishu::closeBook,
                onLesson = ::start,
                modifier = modifier,
            )

            else -> JishuHome(
                drill = drillState,
                state = state,
                onRefresh = drill::refresh,
                onLesson = ::start,
                onBook = jishu::openBook,
                modifier = modifier,
            )
        }
    }
}

// ------------------------------------------------------------------ progress rules

/** 課 progress as 自習 sees it: lines learned (覚えた) out of the 課's distinct lines. */
private class Progress(private val drill: ConjugationDrillState, private val state: JishuState) {
    fun total(point: String) = drill.linesOf(point).distinctBy { it.sentenceId }.size
    fun studied(point: String) = state.studiedIn(point, drill.linesOf(point))
    fun done(point: String) = total(point) > 0 && studied(point) >= total(point)
    fun learned(point: String) = point in drill.learned || done(point)

    /** The 課 to continue: one half-way through, else the first never learned, else one with lines left. */
    fun current(): String? {
        val order = drill.pointOrder
        return order.firstOrNull { studied(it) in 1 until total(it) }
            ?: order.firstOrNull { !learned(it) }
            ?: order.firstOrNull { !done(it) }
    }

    /** The line a 課 shows next: its first line not learned yet. */
    fun nextLine(point: String): ConjugationDrillItem? {
        val lines = drill.linesOf(point).sortedBy { it.sortOrder }.distinctBy { it.sentenceId }
        return lines.firstOrNull { JishuState.key(point, it.sentenceId) !in state.studied } ?: lines.firstOrNull()
    }
}

// ------------------------------------------------------------------ 首页

@Composable
private fun JishuHome(
    drill: ConjugationDrillState,
    state: JishuState,
    onRefresh: () -> Unit,
    onLesson: (String) -> Unit,
    onBook: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val progress = remember(drill, state) { Progress(drill, state) }
    val points = drill.pointOrder
    val learnedCount = points.count(progress::learned)
    Column(modifier.fillMaxSize().background(colors.bg)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("自習", style = AjlTheme.type.jpDisplay.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 1.sp), color = colors.ink, modifier = Modifier.weight(1f))
            if (points.isNotEmpty()) Text("已学 $learnedCount / ${points.size} 課", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
        }
        when {
            drill.phase == DrillPhase.Error && drill.items.isEmpty() -> Column(
                Modifier.fillMaxWidth().padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("连不上题库服务", style = AjlTheme.type.caption, color = colors.ink3)
                OutlineButton("重新加载", onRefresh)
            }

            drill.items.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 120.dp), contentAlignment = Alignment.Center) { LoadingDots() }

            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                val current = progress.current()
                if (current != null) {
                    TodayCard(current, drill, progress, onClick = { onLesson(current) })
                    val left = progress.total(current) - progress.studied(current)
                    InkButton(
                        text = if (progress.studied(current) > 0) "続きから" else "始める",
                        onClick = { onLesson(current) },
                        caption = "板書 · 台詞 ${left.coerceIn(1, JishuViewModel.SittingSize)} 句",
                        trailingArrow = true,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp),
                    )
                }
                SectionHeading(
                    title = "教科書",
                    meta = "${drill.groups.size} 冊 · ${points.size} 課",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp),
                )
                val currentGroup = current?.let(drill::groupOf)
                LazyRow(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(drill.groups, key = { it }) { group ->
                        val inBook = drill.lessonsIn(group)
                        ShelfBook(
                            group = group,
                            learned = inBook.count(progress::learned),
                            total = inBook.size,
                            current = group == currentGroup,
                            onClick = { onBook(group) },
                        )
                    }
                }
            }
        }
    }
}

/** 今日の自習: the 課 to continue, with the next anime line it will show. */
@Composable
private fun TodayCard(point: String, drill: ConjugationDrillState, progress: Progress, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val group = drill.groupOf(point)
    val (jp, gloss) = splitTitle(drill.titleOf(point))
    val line = progress.nextLine(point)
    val total = progress.total(point)
    val studied = progress.studied(point)
    MangaPanel(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp)
            .clickableNoRipple(onClick)
            .semantics { role = Role.Button },
    ) {
        Screentone(
            Modifier.align(Alignment.TopEnd).offset(x = 50.dp, y = (-30).dp).size(230.dp, 120.dp).rotate(-12f),
            color = work.tone(0.30f),
        )
        Column(Modifier.padding(16.dp)) {
            Eyebrow("今日の自習 · VOL.${ConjugationDrillRules.groupKey(group)} ${ConjugationDrillRules.groupTitle(group)}")
            Text("第 ${drill.lessonNumber(point)} 課", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = work.accent, modifier = Modifier.padding(top = 14.dp))
            Text(jp, style = AjlTheme.type.jpDisplay.copy(fontSize = 28.sp, lineHeight = 38.sp), color = colors.ink)
            if (gloss.isNotBlank()) Text(gloss, style = AjlTheme.type.body.copy(fontSize = 13.sp), color = colors.ink2)
            if (line != null) {
                Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(1.dp).background(colors.line))
                EmphasisText(
                    line.jaText,
                    listOf(line.spanStart until line.spanEnd),
                    style = AjlTheme.type.jpBody.copy(fontSize = 19.sp, lineHeight = 34.sp),
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (line.zh.isNotBlank()) Text(line.zh, style = AjlTheme.type.caption.copy(fontSize = 13.sp), color = colors.ink3, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressLine(if (total == 0) 0f else studied.toFloat() / total, Modifier.weight(1f))
                Text("$studied / $total 句", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
            }
        }
    }
}

/** A small 教科書 on the shelf: spine, VOL, vertical title, 課 learned and a progress line. */
@Composable
private fun ShelfBook(group: String, learned: Int, total: Int, current: Boolean, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    val title = ConjugationDrillRules.groupTitle(group)
    Box(
        Modifier
            .width(118.dp)
            .height(172.dp)
            .solidShadow(if (current) AjlStroke.SolidShadowLarge else 0.dp, colors.ink, shape)
            .clip(shape)
            .background(if (current) work.soft else colors.surface)
            .border(AjlStroke.Ink, colors.ink, shape)
            .clickableNoRipple(onClick)
            .semantics { role = Role.Button; contentDescription = "$title，已学 $learned / $total 課" },
    ) {
        Box(Modifier.fillMaxHeight().width(10.dp).background(if (current) work.accent else colors.sunken))
        Box(Modifier.fillMaxHeight().padding(start = 10.dp).width(AjlStroke.Ink).background(colors.ink))
        Screentone(
            Modifier.align(Alignment.BottomEnd).offset(x = 24.dp, y = (-26).dp).size(110.dp, 56.dp).rotate(-12f),
            color = if (current) work.tone(0.34f) else colors.ink.copy(alpha = 0.12f),
        )
        Text("VOL.${ConjugationDrillRules.groupKey(group)}", style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3, modifier = Modifier.padding(start = 18.dp, top = 12.dp))
        if (current) {
            Text(
                "いま",
                style = AjlTheme.type.jpLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = work.onAccent,
                modifier = Modifier.padding(start = 18.dp, top = 34.dp).clip(RoundedCornerShape(2.dp)).background(work.accent).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        VerticalText(
            text = title,
            style = AjlTheme.type.title.copy(fontSize = 17.sp, letterSpacing = 1.5.sp),
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 12.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 10.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$learned/$total 課", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink2)
            ProgressLine(if (total == 0) 0f else learned.toFloat() / total)
        }
    }
}

// ------------------------------------------------------------------ 目次

@Composable
private fun TextbookIndex(
    group: String,
    drill: ConjugationDrillState,
    state: JishuState,
    onBack: () -> Unit,
    onLesson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val progress = remember(drill, state) { Progress(drill, state) }
    val points = drill.lessonsIn(group)
    val learned = points.count(progress::learned)
    val next = points.firstOrNull { progress.studied(it) in 1 until progress.total(it) }
        ?: points.firstOrNull { !progress.learned(it) }
        ?: points.firstOrNull { !progress.done(it) }
    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(nav = TopBarNav.Back, onNav = onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            BookHeader(group, points.map { splitTitle(drill.titleOf(it)).first }, learned, points.size)
            SectionHeading(
                title = "目次",
                meta = "${points.size} 課 · ${points.sumOf(progress::total)} 句",
                modifier = Modifier.padding(top = 24.dp),
            )
            points.forEachIndexed { i, point ->
                val (jp, gloss) = splitTitle(drill.titleOf(point))
                val total = progress.total(point)
                val studied = progress.studied(point)
                val done = progress.done(point)
                val isNext = point == next
                val (meta, metaColor) = when {
                    done -> "済" to colors.ok
                    studied > 0 -> "$studied/$total" to (if (isNext) work.accent else colors.ink3)
                    progress.learned(point) -> "已学" to colors.ok
                    else -> "$total 句" to (if (isNext) work.accent else colors.ink3)
                }
                Column(Modifier.fillMaxWidth().clickableNoRipple({ onLesson(point) }).semantics { role = Role.Button }) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("%02d".format(i + 1), style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = if (done) colors.faint else if (isNext) work.accent else colors.ink, modifier = Modifier.width(26.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                jp,
                                style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, textDecoration = if (done) TextDecoration.LineThrough else null),
                                color = if (done) colors.faint else colors.ink,
                            )
                            if (gloss.isNotBlank()) Text(gloss, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = if (done) colors.faint else colors.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(meta, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = metaColor)
                    }
                    Box(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(colors.line))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (next != null) {
            InkButton(
                text = "続き · 第 ${drill.lessonNumber(next)} 課",
                onClick = { onLesson(next) },
                caption = "${splitTitle(drill.titleOf(next)).first} · ${progress.studied(next)} / ${progress.total(next)} 句",
                trailingArrow = true,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun BookHeader(group: String, lessonNames: List<String>, learned: Int, total: Int) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(work.soft)
            .border(AjlStroke.Ink, colors.ink, shape),
    ) {
        Box(Modifier.fillMaxHeight().width(14.dp).background(work.accent))
        Box(Modifier.fillMaxHeight().padding(start = 14.dp).width(AjlStroke.Ink).background(colors.ink))
        Screentone(
            Modifier.align(Alignment.BottomEnd).offset(x = 30.dp, y = (-10).dp).size(200.dp, 80.dp).rotate(-12f),
            color = work.tone(0.34f),
        )
        Column(Modifier.padding(start = 30.dp, end = 18.dp, top = 16.dp, bottom = 16.dp).fillMaxHeight()) {
            Eyebrow("VOL.${ConjugationDrillRules.groupKey(group)} · 教科書", color = colors.ink3)
            Text(ConjugationDrillRules.groupTitle(group), style = AjlTheme.type.title.copy(fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = 1.sp), color = colors.ink, modifier = Modifier.padding(top = 6.dp))
            Text(
                lessonNames.take(4).joinToString(" · "),
                style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = colors.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressLine(if (total == 0) 0f else learned.toFloat() / total, Modifier.weight(1f), trackColor = colors.ink.copy(alpha = 0.12f))
                Text("已学 $learned / $total", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink2)
            }
        }
    }
}

// ------------------------------------------------------------------ つづく

@Composable
private fun SittingEnd(
    state: JishuState,
    drill: ConjugationDrillState,
    onContinue: (String) -> Unit,
    onTest: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sitting = state.sitting ?: return
    val point = sitting.pointId
    val progress = remember(drill, state) { Progress(drill, state) }
    val total = progress.total(point)
    val studied = progress.studied(point)
    val left = total - studied
    val againLines = sitting.pages.filterIsInstance<JishuPage.Card>().filter { it.item.sentenceId in sitting.requeued }.distinctBy { it.item.sentenceId }
    val nextPoint = if (left > 0) null else drill.pointOrder.let { order ->
        val after = order.dropWhile { it != point }.drop(1)
        after.firstOrNull { !progress.done(it) } ?: order.firstOrNull { !progress.done(it) && it != point }
    }
    val practiceCount = drill.lessons[point]?.practice.orEmpty().size +
        ConjugationDrillRules.pickLesson(drill.linesOf(point), drill.progress).size
    var moving by remember { mutableStateOf(false) }
    val (jp, _) = splitTitle(drill.titleOf(point))
    TsuzukuScreen(
        eyebrow = "第 ${drill.lessonNumber(point)} 課 · $jp",
        tally = "覚えた ${sitting.remembered} 句",
        meta = "本课 $studied / $total 句",
        noted = againLines.take(6).map { TsuzukuLine(it.item.jaText, true, it.item.target) },
        notedTitle = "もう一回 了 ${againLines.size} 句",
        preview = nextPoint?.let { p ->
            val (nextJp, nextGloss) = splitTitle(drill.titleOf(p))
            TsuzukuPreview(
                title = "第 ${drill.lessonNumber(p)} 課 · $nextJp",
                meta = listOf(nextGloss, "${progress.total(p)} 句").filter { it.isNotBlank() }.joinToString(" · "),
                line = progress.nextLine(p)?.jaText,
            )
        },
        primaryLabel = when {
            left > 0 -> "続き · 本课还剩 $left 句"
            nextPoint != null -> "次の課"
            else -> "回到自習"
        },
        onPrimary = {
            if (!moving) {
                moving = true
                when {
                    left > 0 -> onContinue(point)
                    nextPoint != null -> onContinue(nextPoint)
                    else -> onClose()
                }
            }
        },
        onClose = onClose,
        quietLabel = if (practiceCount > 0) "小テスト · $practiceCount 题" else null,
        onQuiet = if (practiceCount > 0) ({ if (!moving) { moving = true; onTest(point) } }) else null,
        extra = { SentencePractice(point, drill.titleOf(point)) },
        modifier = modifier,
    )
}
