package com.animejapaneselab.nativeapp.ui.screens.learn

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.ConjugationLesson
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.DanGrid
import com.animejapaneselab.nativeapp.ui.design.DerivationLine
import com.animejapaneselab.nativeapp.ui.design.EmphasisText
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LineRow
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.SlotState
import com.animejapaneselab.nativeapp.ui.design.TimetableRow
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillRules
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.drill.LessonStatus
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

data class ConjugationStudyActions(
    val onCloseBook: () -> Unit,
    val onOpenLesson: (String) -> Unit,
    val onCloseLesson: () -> Unit,
    val onStartLesson: () -> Unit,
    val onStartReview: () -> Unit,
)

/**
 * 第三巻 教科書 目次: every 課 as a 時間割 row (mastered = 済, the next unlearned = current),
 * one ink 「继续 · 第 N 課」, and an outline 复习 over the learned 課 of this book.
 */
@Composable
fun ConjugationTextbookScreen(
    state: ConjugationDrillState,
    group: String,
    actions: ConjugationStudyActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onCloseBook)
    val colors = AjlTheme.colors
    val points = state.lessonsIn(group)
    val learned = points.count { it in state.learned }
    val mastered = points.count { state.statusOf(it) == LessonStatus.Mastered }
    val next = state.nextLesson(group)
    val due = state.dueInScope
    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = ConjugationDrillRules.groupTitle(group), nav = TopBarNav.Back, onNav = actions.onCloseBook)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Eyebrow("VOL.${ConjugationDrillRules.groupKey(group)} · ${ConjugationDrillRules.groupGloss(group)}")
            ProgressLine(if (points.isEmpty()) 0f else learned.toFloat() / points.size)
            SectionHeading(title = "目次", meta = "已学 $learned / ${points.size} · 掌握 $mastered")
            Column {
                points.forEachIndexed { i, point ->
                    val status = state.statusOf(point)
                    TimetableRow(
                        period = "%02d".format(i + 1),
                        title = state.titleOf(point),
                        meta = when (status) {
                            LessonStatus.Mastered -> "済"
                            LessonStatus.Learned -> "已学"
                            LessonStatus.New -> "${state.linesOf(point).size} 句"
                        },
                        state = when {
                            status == LessonStatus.Mastered -> SlotState.Done
                            point == next -> SlotState.Current
                            else -> SlotState.Upcoming
                        },
                        onClick = { actions.onOpenLesson(point) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (learned > 0) {
                OutlineButton(if (due > 0) "复习 · 到期 $due 句" else "复习已学的课", actions.onStartReview, Modifier.fillMaxWidth())
            }
            if (next != null) {
                InkButton("继续 · 第 ${state.lessonNumber(next)} 課", { actions.onOpenLesson(next) }, Modifier.fillMaxWidth(), caption = state.titleOf(next))
            }
        }
    }
}

/**
 * 授業: one 課 before its 練習. 板書 (rule, formula, 行×段 grid, points, derivations, 易错,
 * 别混, 先学) then 用例 (anime lines with the original voice and their 拆解), and the ink
 * 「練習」 at the bottom. A 課 whose lesson has not been written yet teaches from its lines.
 */
@Composable
fun ConjugationLessonScreen(
    state: ConjugationDrillState,
    pointId: String,
    ttsWorkerUrl: String,
    actions: ConjugationStudyActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onCloseLesson)
    val colors = AjlTheme.colors
    val lesson = state.lessons[pointId]
    val lines = remember(state.items, pointId) { state.linesOf(pointId).sortedBy { it.sortOrder }.take(ExampleLines) }
    val audio = rememberLessonAudioController()
    var playingId by remember { mutableStateOf<String?>(null) }
    val group = state.groupOf(pointId)
    val number = state.lessonNumber(pointId)
    val practiceCount = lesson?.practice.orEmpty().size + ConjugationDrillRules.pickLesson(state.linesOf(pointId), state.progress).size
    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = "第 $number 課", nav = TopBarNav.Back, onNav = actions.onCloseLesson)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Eyebrow(listOf("VOL.${ConjugationDrillRules.groupKey(group)}", ConjugationDrillRules.groupTitle(group)).joinToString(" · "))
            Text(state.titleOf(pointId), style = AjlTheme.type.title.copy(fontSize = 22.sp, lineHeight = 30.sp), color = colors.ink)
            SectionHeading(title = "板書")
            if (lesson != null) LessonBoard(lesson, state, actions) else FallbackBoard(state, lines.firstOrNull())
            if (lines.isNotEmpty()) {
                SectionHeading(title = "用例", meta = "${state.linesOf(pointId).size} 句原声")
                lines.forEach { line ->
                    ExampleLine(line, playing = playingId == line.id && audio.playbackState.phase == AudioPlaybackPhase.Playing) {
                        playingId = line.id
                        audio.play(
                            PromptAudio.Source(line.audioUrl, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = line.jaText),
                            ttsWorkerUrl,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)) {
            if (practiceCount > 0) {
                InkButton(
                    if (pointId in state.learned) "再练一次 · $practiceCount 题" else "練習 · $practiceCount 题",
                    actions.onStartLesson,
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val ExampleLines = 4

@Composable
private fun LessonBoard(lesson: ConjugationLesson, state: ConjugationDrillState, actions: ConjugationStudyActions) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (lesson.rule.isNotBlank()) Text(lesson.rule, style = AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
        if (lesson.formula.isNotBlank()) {
            MangaPanel(Modifier.fillMaxWidth()) {
                Text(
                    lesson.formula,
                    style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp),
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        DanGrid(lesson.dan, lesson.endings)
        if (lesson.points.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lesson.points.forEach { BoardPoint(it) }
            }
        }
        if (lesson.derivations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lesson.derivations.forEach { DerivationLine(it.steps, it.zh) }
            }
        }
        if (lesson.pitfall.isNotBlank()) BoardNote("易错", lesson.pitfall, colors.bad)
        lesson.contrast?.takeIf { state.titleOf(it.pointId).isNotBlank() }?.let { c ->
            BoardNote("别混", c.text, AjlTheme.work.accent, link = "第 ${state.lessonNumber(c.pointId)} 課 ${state.titleOf(c.pointId)}") {
                actions.onOpenLesson(c.pointId)
            }
        }
        val prereq = lesson.prereq.filter { it !in state.learned && state.titleOf(it).isNotBlank() }
        if (prereq.isNotEmpty()) {
            Column {
                Eyebrow("先学")
                prereq.forEach { p ->
                    LineRow(onClick = { actions.onOpenLesson(p) }, minHeight = 40.dp) {
                        Text(state.titleOf(p), style = AjlTheme.type.body, color = colors.ink2, modifier = Modifier.weight(1f))
                        Text("VOL.${ConjugationDrillRules.groupKey(state.groupOf(p))}", style = AjlTheme.type.meta, color = colors.ink3)
                    }
                }
            }
        }
    }
}

/** No written lesson yet: the point's 拆解 from its first line, and the 基础题库 explanation if linked. */
@Composable
private fun FallbackBoard(state: ConjugationDrillState, line: ConjugationDrillItem?) {
    val colors = AjlTheme.colors
    val topic = line?.let(state::topicFor)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (line != null && line.formula.isNotBlank()) {
            MangaPanel(Modifier.fillMaxWidth()) {
                Text(
                    line.formula,
                    style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 26.sp),
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        topic?.shortDefinitionZh?.takeIf { it.isNotBlank() }?.let { BoardPoint(it) }
        topic?.beginnerExplanationZh?.takeIf { it.isNotBlank() }?.let { BoardPoint(it) }
        topic?.cautionNoteZh?.takeIf { it.isNotBlank() }?.let { BoardNote("注意", it, colors.bad) }
    }
}

@Composable
private fun BoardPoint(text: String) {
    val colors = AjlTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Text("・", style = AjlTheme.type.body, color = AjlTheme.work.accent)
        Spacer(Modifier.width(4.dp))
        Text(text, style = AjlTheme.type.body.copy(lineHeight = 24.sp), color = colors.ink)
    }
}

@Composable
private fun BoardNote(
    label: String,
    text: String,
    tone: androidx.compose.ui.graphics.Color,
    link: String? = null,
    onLink: (() -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(label, style = AjlTheme.type.label.copy(fontSize = 13.sp), color = tone, modifier = Modifier.width(40.dp))
            Text(text, style = AjlTheme.type.body.copy(lineHeight = 24.sp), color = colors.ink2, modifier = Modifier.weight(1f))
        }
        if (link != null && onLink != null) {
            LineRow(onClick = onLink, minHeight = 36.dp, divider = false) {
                Spacer(Modifier.width(40.dp))
                Text("→ $link", style = AjlTheme.type.caption, color = tone)
            }
        }
    }
}

/** One 用例: the line with its target under 着重号, the voice button (bars while playing), 拆解 and 译文. */
@Composable
private fun ExampleLine(item: ConjugationDrillItem, playing: Boolean, onPlay: () -> Unit) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EmphasisText(
                    item.jaText,
                    listOf(item.spanStart until item.spanEnd),
                    style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 32.sp),
                )
                if (item.zh.isNotBlank()) Text(item.zh, style = AjlTheme.type.caption, color = colors.ink3)
            }
            if (playing) {
                VoiceBars(active = true, color = AjlTheme.work.accent, modifier = Modifier.padding(14.dp))
            } else {
                IconButton44(Icons.AutoMirrored.Rounded.VolumeUp, "播放原声", onPlay)
            }
        }
        if (item.formula.isNotBlank()) Text(item.formula, style = AjlTheme.type.caption.copy(lineHeight = 20.sp), color = colors.ink2)
        Hairline()
    }
}
