package com.animejapaneselab.nativeapp.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.domain.buildSmartReviewPlan
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.BroadcastLine
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.Seal
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.TimetableRow
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.VerticalText
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.screens.review.ReviewRules
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug
import java.time.LocalDate

/** 今日 tab: 今日の一句 + 本日の時間割. */
@Composable
fun TodayScreen(
    uiState: LabUiState,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode) -> Unit,
    onStartReadAir: () -> Unit,
    onStartReview: () -> Unit,
    onOpenLearn: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val colors = AjlTheme.colors
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    val episodeLabel = TextRules.episodeLabel(episode.coerceAtLeast(1))
    val workName = WorkIdentity.displayName(workSlug, fallback = uiState.focus.workTitle)

    val candidates = remember(workSlug, episode, uiState.shadowing, uiState.subtitles, uiState.readAir.exercises) {
        TodayRules.candidates(
            workSlug = workSlug,
            episode = episode,
            shadowing = uiState.shadowing,
            subtitles = uiState.subtitles,
            readAirExercises = uiState.readAir.exercises,
        )
    }
    val line = remember(today, workSlug, episode, candidates) {
        TodayRules.pickLine(today, workSlug, episode, candidates)
    }
    val slots = remember(uiState.lesson, uiState.lessonMode, uiState.reviewTasks, uiState.mistakes, uiState.readAir, uiState.shadowing, candidates, episodeLabel) {
        val plan = buildSmartReviewPlan(uiState.reviewTasks, uiState.mistakes, today)
        val slug = normalizeWorkSlug(workSlug)
        val readAirScope = uiState.readAir.exercises.filter {
            normalizeWorkSlug(it.workSlug) == slug && it.episode == episode
        }
        val readAirLoaded = uiState.readAir.exercises.isNotEmpty()
        TimetableRules.build(
            TimetableInput(
                episodeLabel = episodeLabel,
                lessonModeLabel = uiState.lessonMode.label,
                lessonTotal = uiState.lesson.nodes.size,
                lessonDone = uiState.lesson.index.coerceAtMost(uiState.lesson.nodes.size),
                reviewDue = ReviewRules.dueCount(plan),
                readAirTotal = if (readAirLoaded) readAirScope.size else null,
                readAirAnswered = readAirScope.count { !uiState.readAir.selectedAnswers[it.id].isNullOrBlank() },
                shadowingCount = uiState.shadowing.size,
                shadowingSpeaker = TodayRules.dominantSpeaker(
                    candidates,
                    uiState.shadowing.map { it.sourceLineNo }.filter { it > 0 }.toSet(),
                ),
            ),
        )
    }
    val start: (SlotAction) -> Unit = { action ->
        when (action) {
            SlotAction.Lesson -> onStartLesson()
            SlotAction.Review -> onStartReview()
            SlotAction.ReadAir -> onStartReadAir()
            SlotAction.Shadowing -> onStartModeLesson(LessonMode.Shadowing)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(colors.bg)) {
        val panelHeight = (maxHeight * 0.46f).coerceIn(300.dp, 372.dp)
        Column(Modifier.fillMaxSize()) {
            BroadcastLine()
            TopBar(
                nav = TopBarNav.None,
                center = {
                    Text(
                        text = buildAnnotatedString {
                            append(TodayRules.dateMeta(today))
                            append(" · ")
                            withStyle(SpanStyle(color = AjlTheme.work.accent)) { append(workName) }
                            append(" ")
                            append(episodeLabel)
                        },
                        style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                        color = colors.ink2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                },
                actions = {
                    IconButton44(Icons.Rounded.Search, "搜索", onOpenSearch)
                    IconButton44(Icons.Rounded.Tune, "设置", onOpenSettings)
                },
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                TodayLinePanel(
                    line = line,
                    today = today,
                    workSlug = workSlug,
                    episodeLabel = episodeLabel,
                    height = panelHeight,
                    onOpenSubtitles = onOpenSubtitles,
                )
                Spacer(Modifier.height(14.dp))
                val current = TimetableRules.current(slots)
                if (current != null) {
                    InkButton(
                        text = "继续 · ${current.title}",
                        caption = current.caption,
                        progress = current.progress,
                        trailingArrow = true,
                        onClick = { start(current.action) },
                    )
                } else {
                    InkButton(
                        text = "继续 · 学ぶ",
                        caption = "$workName · $episodeLabel",
                        trailingArrow = true,
                        onClick = onOpenLearn,
                    )
                }
                if (slots.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    SectionHeading(
                        title = "本日の時間割",
                        meta = TimetableRules.remaining(slots).let { if (it > 0) "还剩 $it 节" else "全部済" },
                    )
                    slots.forEach { slot ->
                        TimetableRow(
                            period = slot.period,
                            title = slot.title,
                            meta = slot.meta,
                            state = slot.state,
                            onClick = { start(slot.action) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 今日の一句 — the one hero of the tab: a manga panel with the day's line set vertically on the
 * right, a screentone band in the bottom-left corner, the work seal, and the translation with
 * 「speaker · time」 underneath. The line rises glyph by glyph (40ms) on the first open of the day.
 */
@Composable
private fun TodayLinePanel(
    line: TodayLine?,
    today: LocalDate,
    workSlug: String,
    episodeLabel: String,
    height: Dp,
    onOpenSubtitles: () -> Unit,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val density = LocalDensity.current
    MangaPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("今日の一句 ")
                    if (line != null) {
                        append(line.ja)
                        if (line.zh.isNotBlank()) append("，${line.zh}")
                        line.attribution?.let { append("，$it") }
                    } else {
                        append(episodeLabel)
                    }
                }
            },
    ) {
        Screentone(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 20.dp)
                .size(width = 300.dp, height = 170.dp)
                .graphicsLayer { rotationZ = -12f },
        )
        Eyebrow(
            text = "今日の一句",
            modifier = Modifier.padding(start = 16.dp, top = 14.dp),
        )
        Seal(
            text = WorkIdentity.sealText(workSlug),
            modifier = Modifier.padding(start = 18.dp, top = 48.dp),
        )

        val fontSize = if (height < 340.dp) 28.sp else 32.sp
        val glyphSpacing = 1.12f
        val glyphDp = with(density) { (fontSize.toPx() * glyphSpacing).toDp() }
        val available = height - 22.dp - 18.dp
        val perColumn = (available / glyphDp).toInt().coerceIn(4, 12)
        val text = line?.ja ?: episodeLabel
        val layout = remember(text, perColumn) { TodayRules.verticalLayout(text, perColumn) }
        val firstOpen = remember(line, today) {
            line != null && TodayLineRevealMemory.markFirstOpen(TodayRules.revealKey(today, line))
        }
        VerticalText(
            text = layout,
            style = type.jpDisplay.copy(fontSize = fontSize, fontWeight = FontWeight.Bold),
            glyphSpacing = glyphSpacing,
            columnGap = 8.dp,
            revealStaggerMillis = if (firstOpen) 40 else null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 22.dp),
        )

        if (line != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .widthIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (line.zh.isNotBlank()) {
                    Text(
                        line.zh,
                        style = type.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = colors.ink2,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                line.attribution?.let { Eyebrow(it) }
            }
        } else {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
            ) {
                com.animejapaneselab.nativeapp.ui.design.QuietButton(
                    text = "翻看本集台词",
                    onClick = onOpenSubtitles,
                )
            }
        }
    }
}
