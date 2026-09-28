package com.animejapaneselab.nativeapp.ui.screens.today

import androidx.compose.foundation.background
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.design.VoiceSwitchPill
import com.animejapaneselab.nativeapp.ui.design.StampMark
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.data.AudioReliability
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.promptAudioForSentence
import com.animejapaneselab.nativeapp.data.AudioKind
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.platform.TodayLineAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.ui.notebook.rememberNotebookEntries
import com.animejapaneselab.nativeapp.widget.TodayWidget
import com.animejapaneselab.nativeapp.domain.buildSmartReviewPlan
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.screens.review.ReviewRules
import com.animejapaneselab.nativeapp.ui.screens.settings.ReminderHealthStrip
import com.animejapaneselab.nativeapp.ui.screens.settings.rememberReminderHealth
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.ui.review.ReviewFeed
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug
import java.time.LocalDate

/**
 * 今日 tab: 本日の時間割 as the main line (一限 自習 → 二限 練習 → 三限 復習, the current one open
 * as a card with its one ink button) and 学習記録 (12 weeks shaded by study time). 今日の一句 no
 * longer shows here; it still feeds the widget and 朝の一句.
 */
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
    /** 自習 → 練習 → 復習: the main line's state and entries. */
    mainLine: TodayMainLine = TodayMainLine(),
    onStartJishu: (pointId: String) -> Unit = {},
    onStartPractice: () -> Unit = {},
) {
    val colors = AjlTheme.colors
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    val episodeLabel = TextRules.episodeLabel(episode.coerceAtLeast(1))

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
    val context = LocalContext.current
    remember { StudyLog.init(context) }
    val studyDays by StudyLog.days.collectAsState()
    val notebook = rememberNotebookEntries()
    val shioriDue = remember(notebook, today) { NotebookRules.dueCount(notebook, today.toEpochDay()) }
    remember { ReviewFeed.init(context) }
    // 三限 復習 shows what is left of today's 復習 round once it exists (same count as the feed).
    val feed by ReviewFeed.session.collectAsState()
    val feedLeft = feed?.takeIf { it.day == today.toEpochDay() && it.filter == null && it.deck == null }?.remaining
    val audio = rememberLessonAudioController()
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
    LaunchedEffect(line, today, lineSentence, lineEntry) {
        val todayLine = line ?: return@LaunchedEffect
        val sourceUrl = lineSentence?.let { (promptAudioForSentence(workSlug, it, autoPlay = false) as? PromptAudio.Source)?.url }.orEmpty()
        // Two weeks ahead, so the widget and 朝の一句 turn the page at midnight on their own.
        val queue = (0L until 14L).mapNotNull { offset ->
            val day = today.plusDays(offset)
            if (offset == 0L) {
                TodayWidget.queueEntry(todayLine, workSlug, episodeLabel, day, sourceUrl, lineEntry)
            } else {
                TodayRules.pickLine(day, workSlug, episode, candidates)?.let { next ->
                    TodayWidget.queueEntry(next, workSlug, episodeLabel, day, "", TodayRules.notebookEntry(next, null, workSlug, episode))
                }
            }
        }
        TodayWidget.publish(context, queue)
        // 朝の一句 plays this clip straight from the notification.
        withContext(Dispatchers.IO) { TodayLineAudio.prepare(context, todayLine.ja, sourceUrl, uiState.settings.ttsWorkerUrl) }
    }
    val studyTotal by StudyLog.totalSeconds.collectAsState()
    val heatmap = remember(studyDays, studyTotal, uiState.progressItems, today) {
        StudyHeatmapRules.build(studyDays, uiState.progressItems, today, studyTotal)
    }
    val todayLog = studyDays[today.toString()]
    val periods = remember(mainLine, uiState.reviewTasks, uiState.mistakes, shioriDue, feed, todayLog, today) {
        val plan = buildSmartReviewPlan(uiState.reviewTasks, uiState.mistakes, today)
        val todaysRound = feed?.takeIf { it.day == today.toEpochDay() && it.filter == null && it.deck == null }
        PeriodRules.build(
            PeriodInput(
                main = mainLine,
                studiedToday = todayLog?.studied ?: 0,
                answeredToday = todayLog?.answers ?: 0,
                reviewDue = feedLeft ?: (ReviewRules.dueCount(plan) + shioriDue),
                reviewRoundDone = todaysRound != null && todaysRound.remaining <= 0,
            ),
        )
    }
    val start: (PeriodKind) -> Unit = { kind ->
        when (kind) {
            PeriodKind.Jishu -> mainLine.jishuPoint?.let(onStartJishu)
            PeriodKind.Practice -> onStartPractice()
            PeriodKind.Review -> onStartReview()
        }
    }
    // The open card: the one tapped, else the first period with work left.
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    val open = picked ?: PeriodRules.current(periods)
    val allDone = PeriodRules.allDone(periods)

    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(
            nav = TopBarNav.None,
            center = {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${today.monthValue}月${today.dayOfMonth}日",
                        style = AjlTheme.type.jpTitle.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink,
                    )
                    Text(
                        WeekdayKanji[today.dayOfWeek.value - 1] + "曜",
                        style = AjlTheme.type.jpLabel.copy(fontSize = 14.sp, lineHeight = 24.sp),
                        color = colors.ink3,
                    )
                }
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
            val reminderHealth = rememberReminderHealth()
            if (uiState.settings.studyReminder && !reminderHealth.ok) {
                ReminderHealthStrip(reminderHealth, onClick = onOpenSettings)
                Spacer(Modifier.height(12.dp))
            }
            SectionHeading(
                title = "本日の時間割",
                meta = "${PeriodRules.doneCount(periods)} / ${periods.size} 済",
            )
            periods.forEachIndexed { index, period ->
                if (index == open && period.state != PeriodState.Idle) {
                    PeriodCard(
                        period = period,
                        audio = audio,
                        ttsWorkerUrl = uiState.settings.ttsWorkerUrl,
                        settings = uiState.settings,
                        onStart = { start(period.kind) },
                    )
                } else {
                    PeriodRow(period, onClick = { picked = index })
                }
            }
            if (allDone && mainLine.jishuPoint != null) {
                Spacer(Modifier.height(14.dp))
                OutlineButton(
                    text = "再学一课 · ${listOfNotNull(mainLine.jishuLesson, mainLine.jishuHeadline.ifBlank { null }).joinToString(" ")}",
                    onClick = { mainLine.jishuPoint.let(onStartJishu) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(28.dp))
            StudyHeatmapSection(heatmap)
        }
    }
}

private val WeekdayKanji = listOf("月", "火", "水", "木", "金", "土", "日")

/** A closed period: 一限 · 自習 · 五段 未然形 · meta; 済 periods carry the seal, idle ones fade. */
@Composable
private fun PeriodRow(period: TodayPeriod, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val idle = period.state == PeriodState.Idle
    val done = period.state == PeriodState.Done
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickableNoRipple(onClick = onClick, enabled = !idle)
                .semantics(mergeDescendants = true) { contentDescription = "${period.no} ${period.tab} ${period.title} ${period.meta}" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                period.no,
                style = AjlTheme.type.jpLabel.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = if (done || idle) colors.ink3 else colors.ink2,
                modifier = Modifier.width(40.dp),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = AjlTheme.type.jpLabel.fontFamily, fontWeight = FontWeight.SemiBold)) { append(period.tab) }
                    if (period.title.isNotBlank()) {
                        withStyle(SpanStyle(color = colors.ink3)) { append(" · ") }
                        append(period.title)
                    }
                },
                style = AjlTheme.type.body.copy(fontSize = 15.sp),
                color = if (done || idle) colors.ink3 else colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (done) {
                StampMark(size = 30.dp, rotation = -10f, color = work.accent)
            } else {
                Text(
                    period.meta,
                    style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                    color = if (idle) colors.ink3 else work.accent,
                )
            }
        }
        Hairline()
    }
}

/**
 * The open period: what it is (headline + gloss), one anime line from it with the target marked
 * and a 原声 / TTS pill, where it stands, and the screen's one ink button.
 */
@Composable
private fun PeriodCard(
    period: TodayPeriod,
    audio: LessonAudioController,
    ttsWorkerUrl: String,
    settings: com.animejapaneselab.nativeapp.data.LabSettings,
    onStart: () -> Unit,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    MangaPanel(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Screentone(
            Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-28).dp).size(180.dp, 90.dp).graphicsLayer { rotationZ = -12f },
            color = work.tone(0.22f),
        )
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(period.no, style = AjlTheme.type.jpLabel.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = work.accent)
                Eyebrow(period.kicker)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(period.headline, style = AjlTheme.type.jpDisplay.copy(fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                if (period.sub.isNotBlank()) Text(period.sub, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2)
            }
            period.sample?.let { sample -> SampleLine(sample, audio, ttsWorkerUrl, settings) }
            if (period.progressLeft.isNotBlank() || period.progressRight.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    period.progress?.let { ProgressLine(it) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(period.progressLeft, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
                        Text(period.progressRight, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
                    }
                }
            }
            InkButton(text = period.action, onClick = onStart, trailingArrow = true)
        }
    }
}

/** The period's anime line: target marked, reading aids per settings, Chinese, 原声 / TTS pill. */
@Composable
private fun SampleLine(
    sample: TodaySample,
    audio: LessonAudioController,
    ttsWorkerUrl: String,
    settings: com.animejapaneselab.nativeapp.data.LabSettings,
) {
    val colors = AjlTheme.colors
    val furigana = rememberFuriganaAnnotator(settings)
    val aided = settings.showFurigana || settings.showRomaji
    LaunchedEffect(sample.ja, aided) { if (aided) furigana.request("sentence", listOf(sample.ja)) }
    val reading = remember(sample.ja, furigana.resultFor(sample.ja)) { LineReading.build(sample.ja, furigana.resultFor(sample.ja)) }
    val hasSource = sample.audioUrl.isNotBlank()
    var tts by rememberSaveable(sample.ja) { mutableStateOf(false) }
    val cue = if (hasSource && !tts) {
        PromptAudio.Source(sample.audioUrl, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = sample.ja)
    } else {
        PromptAudio.Tts(sample.ja, autoPlay = false)
    }
    val playing = audio.playbackState.phase == AudioPlaybackPhase.Playing || audio.playbackState.phase == AudioPlaybackPhase.Loading
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Hairline()
        ReadingLineText(
            reading,
            sample.mark,
            showRuby = settings.showFurigana,
            showRomaji = settings.showRomaji,
            style = AjlTheme.type.jpBody.copy(fontSize = 19.sp, lineHeight = 30.sp),
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                sample.zh,
                style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = colors.ink2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            VoiceSwitchPill(
                playing = playing,
                options = if (hasSource) listOf("原声", "TTS") else listOf("TTS"),
                selected = if (hasSource && tts) 1 else 0,
                onSelect = { tts = hasSource && it == 1 },
                onClick = { audio.play(cue, ttsWorkerUrl) },
            )
        }
    }
}
