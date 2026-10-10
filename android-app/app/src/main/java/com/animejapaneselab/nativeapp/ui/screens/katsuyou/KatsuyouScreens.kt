package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.animejapaneselab.nativeapp.ui.design.NoteText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillRules
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KatsuyouBook
import com.animejapaneselab.nativeapp.ui.katsuyou.Katsuyou
import com.animejapaneselab.nativeapp.ui.katsuyou.KyBack
import com.animejapaneselab.nativeapp.ui.katsuyou.KyBook
import com.animejapaneselab.nativeapp.ui.katsuyou.KyLesson
import com.animejapaneselab.nativeapp.ui.katsuyou.KyPeek
import com.animejapaneselab.nativeapp.ui.katsuyou.KyStep
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuPreview
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuScreen
import com.animejapaneselab.nativeapp.ui.screens.zougo.FuseSitting
import com.animejapaneselab.nativeapp.ui.screens.zougo.KindChip
import com.animejapaneselab.nativeapp.ui.screens.zougo.LineCard
import com.animejapaneselab.nativeapp.ui.screens.zougo.StagePanel
import com.animejapaneselab.nativeapp.ui.screens.zougo.ZougoHeader
import com.animejapaneselab.nativeapp.ui.screens.zougo.segColor
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.zougo.SegKind
import com.animejapaneselab.nativeapp.ui.zougo.ZgSeg

/** The 課 title without the explanation: 「イ音便：く→いて／ぐ→いで」 → 「イ音便」. */
internal fun shortTitle(title: String): String = title.substringBefore('：').substringBefore('（').trim()

/**
 * One rebuilt 課: 課前の一眼 → its plays in order (拼合台, 倒推, 還原台, 活用盘…) → つづく, which marks the point
 * learned ([onLearned]) so its lines go into 練習 as before. [onNext] opens the next 課 of the book.
 */
@Composable
fun KatsuyouLesson(
    point: String,
    drill: ConjugationDrillState,
    settings: LabSettings,
    onLearned: (String) -> Unit,
    onNext: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val data = remember { KatsuyouBook.load(context) }
    val lesson = data.lesson(point)
    if (lesson == null || lesson.steps.isEmpty()) {
        LaunchedEffect(point) { Katsuyou.exit() }
        return
    }
    val group = drill.groupOf(point)
    val key = ConjugationDrillRules.groupKey(group)
    val number = drill.lessonNumber(point)
    val title = shortTitle(drill.titleOf(point))
    val eyebrow = "VOL.$key ${ConjugationDrillRules.groupTitle(group)} · 第 $number 課"
    // -1 = 課前の一眼, steps.size = つづく.
    var step by rememberSaveable(point) { mutableIntStateOf(if (lesson.peek != null) -1 else 0) }
    var right by rememberSaveable(point) { mutableIntStateOf(0) }
    var asked by rememberSaveable(point) { mutableIntStateOf(0) }
    val missed = remember(point) { mutableStateListOf<TsuzukuLine>() }
    val onClose = Katsuyou::exit

    when {
        step < 0 -> PeekScreen(lesson.peek!!, eyebrow, title, lesson.steps.first().count, onClose, onStart = { step = 0 }, modifier = modifier)

        step < lesson.steps.size -> {
            val s = lesson.steps[step]
            val nextTitle = lesson.steps.getOrNull(step + 1)?.title
            StepSitting(
                step = s,
                key = "ky:$point:$step",
                eyebrow = eyebrow,
                title = s.title.ifBlank { title },
                settings = settings,
                onClose = onClose,
                onAnswer = { ok -> Katsuyou.answer(context, ok) },
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
            LaunchedEffect(point) { onLearned(point) }
            val points = drill.lessonsIn(group)
            val learned = drill.learned + point
            val next = points.dropWhile { it != point }.drop(1).firstOrNull { it !in learned }
                ?: points.firstOrNull { it !in learned }
            TsuzukuScreen(
                eyebrow = "第 $number 課 · $title",
                tally = if (asked > 0) "答对 $right / $asked" else "",
                meta = "VOL.$key ${ConjugationDrillRules.groupTitle(group)} · 已学 ${points.count { it in learned }} / ${points.size} 課",
                noted = missed.toList(),
                notedTitle = "再看一眼的 ${missed.size} 个",
                preview = next?.let { n ->
                    val nl = data.lesson(n)
                    TsuzukuPreview(
                        title = "第 ${drill.lessonNumber(n)} 課 · ${shortTitle(drill.titleOf(n))}",
                        line = nl?.preview?.joinToString("・"),
                    )
                },
                primaryLabel = if (next != null) "次の課" else "回到目次",
                onPrimary = { if (next != null) onNext(next) else Katsuyou.exit() },
                onClose = onClose,
                modifier = modifier,
            )
        }
    }
}

/** One play of a 課; [onDone] gets right, asked and the ones to look at again. */
@Composable
internal fun StepSitting(
    step: KyStep,
    key: String,
    eyebrow: String,
    title: String,
    settings: LabSettings,
    onClose: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onDone: (right: Int, asked: Int, missed: List<TsuzukuLine>) -> Unit,
    lastLabel: String,
    modifier: Modifier,
) {
    when (step) {
        is KyStep.Fuse -> FuseSitting(
            items = step.items, key = key, eyebrow = eyebrow, title = title, settings = settings, onClose = onClose,
            onAnswer = { _, ok -> onAnswer(ok) },
            onDone = { r, wrong -> onDone(r, step.items.size, wrong.map { TsuzukuLine(it.word, true, it.romaji) }) },
            lastLabel = lastLabel, modifier = modifier,
        )
        is KyStep.Back -> BackTrackSitting(
            items = step.items, key = key, eyebrow = eyebrow, settings = settings, onClose = onClose, onAnswer = onAnswer,
            onDone = { r, wrong -> onDone(r, step.items.size, wrong.map { TsuzukuLine(it.te, true, "← ${it.base}") }) },
            lastLabel = lastLabel, modifier = modifier,
        )
        is KyStep.Pick -> PickSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Speed -> SpeedSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Dial -> DialSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Swipe -> SwipeSitting(step, key, eyebrow, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Flip -> FlipSitting(step, key, eyebrow, settings, onClose, onDone, lastLabel, modifier)
        is KyStep.Stack -> StackSitting(step, key, eyebrow, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Connect -> ConnectSitting(step, key, eyebrow, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Spot -> SpotSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Span -> SpanSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Passage -> PassageSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Judge -> JudgeSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Diff -> DiffSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Crack -> CrackSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Joint -> JointSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
        is KyStep.Decide -> DecideSitting(step.items, key, eyebrow, title, settings, onClose, onAnswer, onDone, lastLabel, modifier)
    }
}

// ------------------------------------------------------------------ 課前の一眼

@Composable
internal fun PeekScreen(peek: KyPeek, eyebrow: String, title: String, groups: Int, onClose: () -> Unit, onStart: () -> Unit, modifier: Modifier) {
    peek.glance?.let {
        GlanceScreen(it, peek.go, eyebrow, title, groups, onClose, onStart, modifier)
        return
    }
    BackHandler(onBack = onClose)
    val colors = AjlTheme.colors
    Column(modifier.fillMaxSize().background(colors.bg)) {
        ZougoHeader(eyebrow = eyebrow, title = title, counter = "", progress = 0f, onClose = onClose)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("課前の一眼", style = AjlTheme.type.meta.copy(fontSize = 11.sp, letterSpacing = 0.6.sp), color = colors.ink3)
            StagePanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        peek.ends.forEach { e ->
                            Box(
                                Modifier.heightIn(min = 44.dp).widthIn(min = 40.dp).border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(e, style = AjlTheme.type.jpDisplay.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = colors.ink) }
                        }
                    }
                    Text("→", style = AjlTheme.type.meta.copy(fontSize = 16.sp), color = colors.ink3)
                    KanaSegs(peek.result, 30)
                }
                if (peek.beats.isEmpty()) {
                    NoteText(
                        peek.rule,
                        style = AjlTheme.type.body,
                        color = colors.ink,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (peek.beats.isNotEmpty()) PeekBeats(peek.beats)
            Column {
                peek.examples.forEach { ex ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(ex.base, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 24.sp), color = colors.ink3, maxLines = 1, softWrap = false, modifier = Modifier.widthIn(min = 64.dp))
                        Text("→", style = AjlTheme.type.meta.copy(lineHeight = 24.sp), color = colors.ink3)
                        Column {
                            Text(ex.romaji, style = AjlTheme.type.meta.copy(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp), color = colors.ink3)
                            KanaSegs(ex.tail, 19, stem = ex.stem)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(colors.line))
                }
            }
        }
        InkButton(
            text = peek.go,
            onClick = onStart,
            caption = "$groups 个",
            trailingArrow = true,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        )
    }
}

/** Kana pieces in their kind's colour (ん green, で work colour…), plain for [SegKind.Same]. */
@Composable
private fun KanaSegs(segs: List<ZgSeg>, size: Int, stem: String = "") {
    val ink = AjlTheme.colors.ink
    val colorsBy = segs.map { segColor(it.kind) }
    Text(
        buildAnnotatedString {
            append(stem)
            segs.forEachIndexed { i, s ->
                val c = colorsBy[i].takeIf { it != Color.Unspecified && s.kind != SegKind.Already } ?: ink
                withStyle(SpanStyle(color = c)) { append(s.text) }
            }
        },
        style = AjlTheme.type.jpDisplay.copy(fontSize = size.sp, lineHeight = (size * 1.3f).sp, fontWeight = FontWeight.Bold),
    )
}

// ------------------------------------------------------------------ 倒推

/** 倒推: the line, the て形 big, and three dictionary forms to pick from. */
@Composable
private fun BackTrackSitting(
    items: List<KyBack>,
    key: String,
    eyebrow: String,
    settings: LabSettings,
    onClose: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onDone: (right: Int, missed: List<KyBack>) -> Unit,
    lastLabel: String,
    modifier: Modifier,
) {
    BackHandler(onBack = onClose)
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var picked by rememberSaveable(key) { mutableIntStateOf(-1) }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val item = items.getOrNull(index) ?: return
    val revealed = picked >= 0

    Column(modifier.fillMaxSize().background(colors.bg)) {
        ZougoHeader(eyebrow = eyebrow, title = "倒推：て形 → 原形", counter = "${index + 1} / ${items.size}", progress = (index + if (revealed) 1 else 0).toFloat() / items.size, onClose = onClose)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LineCard(item.line, audio, settings.ttsWorkerUrl)
            StagePanel {
                Text(item.romaji, style = AjlTheme.type.meta.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.padding(top = 4.dp))
                Text(
                    item.te,
                    style = AjlTheme.type.jpDisplay.copy(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                    modifier = Modifier.clickableNoRipple({ audio.speakText(item.te, settings.ttsWorkerUrl) }),
                )
                Text("↓", style = AjlTheme.type.meta.copy(fontSize = 18.sp), color = colors.ink3, modifier = Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.bins.forEachIndexed { i, (kana, ro) ->
                        val target = when {
                            revealed && i == item.answer -> work.accent
                            revealed && i == picked -> colors.surface
                            else -> colors.surface
                        }
                        val bg by animateColorAsState(target, tween(220), label = "bin-bg")
                        val fg = when {
                            revealed && i == item.answer -> colors.bg
                            revealed && i == picked -> colors.bad
                            revealed -> colors.ink3
                            else -> colors.ink
                        }
                        val border = when {
                            revealed && i == picked && i != item.answer -> colors.bad
                            revealed && i != item.answer -> colors.line2
                            else -> colors.ink
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 92.dp)
                                .background(bg, RoundedCornerShape(4.dp))
                                .border(AjlStroke.Ink, border, RoundedCornerShape(4.dp))
                                .clickableNoRipple({
                                    if (picked < 0) {
                                        val ok = i == item.answer
                                        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                                        onAnswer(ok)
                                        if (ok) right++ else missed.add(index)
                                        picked = i
                                        audio.speakText(item.base, settings.ttsWorkerUrl)
                                    }
                                })
                                .semantics { role = Role.Button; contentDescription = item.stem + kana }
                                .padding(bottom = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(item.stemRomaji + ro, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = fg.copy(alpha = 0.8f))
                            Text(item.stem + kana, style = AjlTheme.type.jpDisplay.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = fg)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            if (!revealed) {
                Text("它的原形是哪个？", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                val ok = picked == item.answer
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (ok) "✓ ${item.base}" else "正解 ${item.base}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = if (ok) colors.ok else colors.bad)
                    Text(item.note, style = AjlTheme.type.body, color = colors.ink)
                }
            }
        }
        if (revealed) {
            val last = index == items.lastIndex
            InkButton(
                text = if (last) lastLabel else "下一个",
                onClick = {
                    audio.stop()
                    if (last) onDone(right, missed.map { items[it] }) else { index++; picked = -1 }
                },
                trailingArrow = !last,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ まとめ（词尾地图）

/** 自習 → 目次 → まとめ: the book's map; て / た switch, a row opens to the verbs played (tap = hear). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KatsuyouMap(groupKey: String, bookTitle: String, settings: LabSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val book: KyBook? = remember { KatsuyouBook.load(context) }.book(groupKey)
    if (book == null) {
        LaunchedEffect(groupKey) { Katsuyou.exit() }
        return
    }
    book.table?.let {
        KyTableScreen(it, "VOL.$groupKey $bookTitle · まとめ", settings, modifier)
        return
    }
    BackHandler(onBack = Katsuyou::exit)
    val colors = AjlTheme.colors
    val audio = rememberLessonAudioController()
    var ta by rememberSaveable { mutableStateOf(false) }
    var open by rememberSaveable { mutableIntStateOf(0) }
    fun past(s: String) = if (!ta) s else s.replace(Regex("て$"), "た").replace(Regex("で$"), "だ").replace(Regex("te$"), "ta").replace(Regex("de$"), "da")

    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(
            nav = TopBarNav.Back,
            onNav = Katsuyou::exit,
            center = {
                Column(Modifier.weight(1f)) {
                    Text("VOL.$groupKey $bookTitle · まとめ", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3, maxLines = 1)
                    Text(book.mapTitle, style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                }
            },
            actions = {
                Row(Modifier.padding(end = 12.dp).border(AjlStroke.Hair, colors.line2, RoundedCornerShape(12.dp))) {
                    listOf("て", "た").forEachIndexed { i, label ->
                        val on = (i == 1) == ta
                        Box(
                            Modifier
                                .size(44.dp, 36.dp)
                                .background(if (on) colors.ink else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickableNoRipple({ ta = i == 1 })
                                .semantics { role = Role.Button; contentDescription = "接 $label" },
                            contentAlignment = Alignment.Center,
                        ) { Text(label, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = if (on) colors.bg else colors.ink2) }
                    }
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            book.rows.forEachIndexed { r, row ->
                val isOpen = open == r
                MangaPanel(Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickableNoRipple({ open = if (isOpen) -1 else r }).padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(Modifier.widthIn(min = 112.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.ends.forEach { (k, ro) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 30.dp)) {
                                        Text(ro, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3)
                                        Text(k, style = AjlTheme.type.jpDisplay.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                                    }
                                }
                            }
                            Text("→", style = AjlTheme.type.meta.copy(fontSize = 14.sp), color = colors.ink3)
                            Column {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = mapColor(row.mid.kind))) { append(row.midRomaji) }
                                        withStyle(SpanStyle(color = mapColor(row.te.kind))) { append(past(row.teRomaji)) }
                                    },
                                    style = AjlTheme.type.meta.copy(fontSize = 11.sp),
                                )
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = mapColor(row.mid.kind))) { append(row.mid.text) }
                                        withStyle(SpanStyle(color = mapColor(row.te.kind))) { append(past(row.te.text)) }
                                    },
                                    style = AjlTheme.type.jpDisplay.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            KindChip(row.name, row.mid.kind)
                        }
                        AnimatedVisibility(isOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column {
                                Box(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(colors.line))
                                FlowRow(
                                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    row.verbs.forEach { (base, form, ro) ->
                                        val shown = past(form)
                                        Column(
                                            Modifier
                                                .heightIn(min = 52.dp)
                                                .border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp))
                                                .background(colors.bg, RoundedCornerShape(4.dp))
                                                .clickableNoRipple({ audio.speakText(shown, settings.ttsWorkerUrl) })
                                                .semantics { role = Role.Button; contentDescription = "念 $shown" }
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                        ) {
                                            Text(past(ro), style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3)
                                            Text(shown, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                                            Text(base, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            book.mapFootnote?.let { (ichidan, note) ->
                Row(
                    Modifier.fillMaxWidth().border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ichidan + if (ta) "た" else "て", style = AjlTheme.type.jpBody.copy(fontSize = 15.sp), color = colors.ink2)
                    Spacer(Modifier.weight(1f))
                    Text(note, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun mapColor(kind: SegKind): Color = segColor(kind).takeIf { it != Color.Unspecified && kind != SegKind.Already } ?: AjlTheme.colors.ink
