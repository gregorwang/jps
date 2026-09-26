package com.animejapaneselab.nativeapp.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import com.animejapaneselab.nativeapp.data.promptAudioForSentence
import com.animejapaneselab.nativeapp.data.AudioKind
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.design.speechLines
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.ui.notebook.rememberNotebookEntries
import com.animejapaneselab.nativeapp.widget.TodayWidget
import com.animejapaneselab.nativeapp.domain.buildSmartReviewPlan
import com.animejapaneselab.nativeapp.ui.LabUiState
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
import com.animejapaneselab.nativeapp.ui.study.StudyLog
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
    onTodayLineRevealed: (date: String) -> Unit = {},
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
    val context = LocalContext.current
    remember { StudyLog.init(context) }
    val studyDays by StudyLog.days.collectAsState()
    val heatmap = remember(studyDays, uiState.progressItems, today) {
        StudyHeatmapRules.build(studyDays, uiState.progressItems, today)
    }
    val audio = rememberLessonAudioController()
    val speaking = audio.playbackState.phase.let { it == AudioPlaybackPhase.Playing || it == AudioPlaybackPhase.Loading }
    val lineSentence = remember(line, uiState.shadowing, uiState.subtitles) {
        line?.let { l ->
            uiState.shadowing.firstOrNull { l.lineNo > 0 && it.sourceLineNo == l.lineNo }
                ?: uiState.shadowing.firstOrNull { TodayRules.splitSpeakerPrefix(it.ja).second == l.ja }
                ?: uiState.subtitles.firstOrNull { l.lineNo > 0 && it.lineNo == l.lineNo && it.hasSourceAudio }?.let { sub ->
                    ShadowingSentence(
                        id = "${normalizeWorkSlug(workSlug)}-$episode-${sub.lineNo}",
                        ja = l.ja,
                        reading = "",
                        meaningZh = l.zh,
                        sourceLabel = "",
                        audioKind = AudioKind.Source,
                        sourceLineNo = sub.lineNo,
                        audioUrl = sub.audioUrl,
                        storagePath = sub.storagePath,
                    )
                }
        }
    }
    val lineEntry = remember(line, lineSentence, workSlug, episode) {
        line?.let { TodayRules.notebookEntry(it, lineSentence, workSlug, episode) }
    }
    val notebook = rememberNotebookEntries()
    val lineSaved = lineEntry != null && notebook.any { it.key == lineEntry.key }
    val playLine: () -> Unit = {
        if (line != null) {
            if (lineSentence != null) {
                audio.play(promptAudioForSentence(workSlug, lineSentence, autoPlay = false), uiState.settings.ttsWorkerUrl)
            } else {
                audio.speakText(line.ja, uiState.settings.ttsWorkerUrl)
            }
        }
    }
    LaunchedEffect(line, today) { line?.let { TodayWidget.publish(context, it, workSlug, episodeLabel, today) } }
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
                    revealedOn = uiState.todayLineRevealedOn,
                    onRevealed = onTodayLineRevealed,
                    workSlug = workSlug,
                    episodeLabel = episodeLabel,
                    height = panelHeight,
                    onOpenSubtitles = onOpenSubtitles,
                    speaking = speaking,
                    saved = lineSaved,
                    onPlay = playLine,
                    onToggleSaved = { lineEntry?.let { Notebook.toggle(context, it) } },
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
                Spacer(Modifier.height(28.dp))
                StudyHeatmapSection(heatmap)
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
    revealedOn: String?,
    onRevealed: (date: String) -> Unit,
    workSlug: String,
    episodeLabel: String,
    height: Dp,
    onOpenSubtitles: () -> Unit,
    speaking: Boolean = false,
    saved: Boolean = false,
    onPlay: () -> Unit = {},
    onToggleSaved: () -> Unit = {},
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val density = LocalDensity.current
    MangaPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .speechLines(speaking, AjlTheme.work.accent)
            .clickableNoRipple(onClick = onPlay)
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
        // Decided once per line/day so the persisted mark below doesn't cut the reveal short.
        val firstOpen = remember(line != null, today) { line != null && revealedOn != today.toString() }
        LaunchedEffect(firstOpen, today) { if (firstOpen) onRevealed(today.toString()) }
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
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VoiceBars(active = speaking, color = AjlTheme.work.accent, modifier = Modifier.padding(end = 4.dp))
                IconButton44(
                    icon = if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (saved) "取下栞" else "夹进栞",
                    onClick = onToggleSaved,
                    tint = if (saved) AjlTheme.work.accent else colors.ink3,
                )
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
