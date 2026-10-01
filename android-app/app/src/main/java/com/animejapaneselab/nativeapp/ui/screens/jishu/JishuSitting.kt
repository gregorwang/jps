package com.animejapaneselab.nativeapp.ui.screens.jishu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.design.CoveredLine
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.VoiceSwitchPill
import com.animejapaneselab.nativeapp.ui.voicepack.rememberVoiceOptions
import com.animejapaneselab.nativeapp.ui.design.VoiceTone
import com.animejapaneselab.nativeapp.ui.reading.FuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.ConjugationLesson
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.DanGrid
import com.animejapaneselab.nativeapp.ui.design.DerivationLine
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillRules
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.jishu.JishuPage
import com.animejapaneselab.nativeapp.ui.jishu.JishuSitting
import com.animejapaneselab.nativeapp.ui.jishu.SceneContext
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

internal data class SittingActions(
    val onExit: () -> Unit,
    val onBack: () -> Unit,
    val onNext: () -> Unit,
    val onRemember: () -> Unit,
    val onAgain: () -> Unit,
    val onToggleCover: () -> Unit,
)

/**
 * One 自習 sitting: segmented progress (板書 + one segment per line), then the 板書 page and one
 * scene card per anime line. Cards auto-play the original voice; 遮る hides the translation.
 */
@Composable
internal fun JishuSittingScreen(
    sitting: JishuSitting,
    drill: ConjugationDrillState,
    context: Map<String, SceneContext>,
    cover: Boolean,
    ttsWorkerUrl: String,
    settings: LabSettings,
    actions: SittingActions,
    modifier: Modifier = Modifier,
) {
    val furigana = rememberFuriganaAnnotator(settings)
    val reading = ReadingAids(settings.showFurigana, settings.showRomaji, furigana)
    BackHandler(onBack = if (sitting.index > 0) actions.onBack else actions.onExit)
    val page = sitting.current ?: return
    val reduced = rememberReducedMotion()
    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        SittingTopBar(sitting, cover, actions)
        AnimatedContent(
            targetState = sitting.index,
            transitionSpec = {
                if (reduced) fadeIn(tween(120)) togetherWith fadeOut(tween(80))
                else (slideInHorizontally(tween(260)) { it / 8 } + fadeIn(tween(220))) togetherWith fadeOut(tween(120))
            },
            modifier = Modifier.weight(1f),
            label = "jishu-page",
        ) { index ->
            when (val p = sitting.pages.getOrNull(index) ?: page) {
                JishuPage.Board -> BoardPage(sitting, drill, actions.onNext)
                is JishuPage.Card -> CardPage(p, index, sitting, drill, context[p.item.sentenceId], cover, ttsWorkerUrl, settings.autoSpeak, reading, actions)
            }
        }
    }
}

@Composable
private fun SittingTopBar(sitting: JishuSitting, cover: Boolean, actions: SittingActions) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val total = sitting.pages.size
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconButton44(Icons.Rounded.Close, "退出", actions.onExit)
        ProgressLine(
            progress = (sitting.index + 1f) / total.coerceAtLeast(1),
            modifier = Modifier.weight(1f),
            contentDescription = "第 ${sitting.index + 1} 页，共 $total 页",
        )
        if (sitting.current is JishuPage.Board) {
            Text("板書", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = work.accent, modifier = Modifier.padding(end = 16.dp))
        } else {
            IconButton44(
                if (cover) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                if (cover) "显示中文" else "遮住中文",
                actions.onToggleCover,
            )
        }
    }
}

// ------------------------------------------------------------------ 板書

@Composable
private fun BoardPage(sitting: JishuSitting, drill: ConjugationDrillState, onNext: () -> Unit) {
    val colors = AjlTheme.colors
    val point = sitting.pointId
    val lesson = drill.lessons[point]
    val (jp, gloss) = splitTitle(drill.titleOf(point))
    val group = drill.groupOf(point)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("VOL.${ConjugationDrillRules.groupKey(group)} ${ConjugationDrillRules.groupTitle(group)} · 第 ${drill.lessonNumber(point)} 課")
                Text(jp, style = AjlTheme.type.jpDisplay.copy(fontSize = 30.sp, lineHeight = 40.sp), color = colors.ink)
                if (gloss.isNotBlank()) Text(gloss, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2)
            }
            if (lesson != null) LessonBoard(lesson, drill) else FallbackBoard(drill, point)
        }
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)) {
            InkButton(
                text = if (sitting.cardCount > 0) "台詞へ · ${sitting.cardCount} 句原声" else "完了",
                onClick = onNext,
                trailingArrow = true,
                height = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LessonBoard(lesson: ConjugationLesson, drill: ConjugationDrillState) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MangaPanel(Modifier.fillMaxWidth()) {
            Screentone(
                Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-24).dp).size(170.dp, 80.dp).rotate(-12f),
                color = work.tone(0.24f),
            )
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                Eyebrow("公式")
                Text(
                    formulaText(lesson.formula, work.accent),
                    style = AjlTheme.type.jpBody.copy(fontSize = 20.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (lesson.rule.isNotBlank()) {
                    Hairline(Modifier.padding(top = 14.dp))
                    Text(lesson.rule, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 24.sp), color = colors.ink, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        DanGrid(lesson.dan, lesson.endings)
        if (lesson.points.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lesson.points.forEachIndexed { i, text ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("%02d".format(i + 1), style = AjlTheme.type.meta.copy(fontSize = 12.sp, lineHeight = 24.sp), color = work.accent, modifier = Modifier.width(30.dp))
                        Text(text, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 24.sp), color = colors.ink)
                    }
                }
            }
        }
        if (lesson.derivations.isNotEmpty()) {
            Column {
                Eyebrow("推导", Modifier.padding(bottom = 6.dp))
                lesson.derivations.forEach { d ->
                    Hairline()
                    DerivationLine(d.steps, d.zh, Modifier.padding(vertical = 10.dp))
                }
            }
        }
        if (lesson.pitfall.isNotBlank() || lesson.contrast != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (lesson.pitfall.isNotBlank()) BoardNote("易错", lesson.pitfall, colors.bad)
                lesson.contrast?.let { c ->
                    val other = drill.titleOf(c.pointId)
                    val link = if (other.isBlank()) "" else " → VOL.${ConjugationDrillRules.groupKey(drill.groupOf(c.pointId))} 第 ${drill.lessonNumber(c.pointId)} 課 ${splitTitle(other).first}"
                    BoardNote("别混", readableIds(c.text, drill) + link, work.accent)
                }
            }
        }
    }
}

/** No written lesson: the first line's 拆解 and the linked 基础 topic's explanation. */
@Composable
private fun FallbackBoard(drill: ConjugationDrillState, point: String) {
    val colors = AjlTheme.colors
    val line = drill.linesOf(point).minByOrNull { it.sortOrder }
    val topic = line?.let(drill::topicFor)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (line != null && line.formula.isNotBlank()) {
            MangaPanel(Modifier.fillMaxWidth()) {
                Text(line.formula, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 26.sp), color = colors.ink, modifier = Modifier.padding(16.dp))
            }
        }
        listOfNotNull(topic?.shortDefinitionZh, topic?.beginnerExplanationZh).filter { it.isNotBlank() }.forEach {
            Text(it, style = AjlTheme.type.body.copy(lineHeight = 24.sp), color = colors.ink)
        }
    }
}

@Composable
private fun BoardNote(label: String, text: String, tone: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, style = AjlTheme.type.label.copy(fontSize = 13.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = tone, modifier = Modifier.width(46.dp))
        Text(text, style = AjlTheme.type.body.copy(fontSize = 14.sp, lineHeight = 22.sp), color = AjlTheme.colors.ink2)
    }
}

/** 「动词连用形（て形去て）＋ちゃう／じゃう」: ＋ and the last segment in the work colour. */
private fun formulaText(formula: String, accent: androidx.compose.ui.graphics.Color) = buildAnnotatedString {
    val parts = formula.split('＋').map { it.trim() }
    parts.forEachIndexed { i, part ->
        if (i > 0) withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Normal)) { append(" ＋ ") }
        if (i == parts.lastIndex && parts.size > 1) withStyle(SpanStyle(color = accent)) { append(part) } else append(part)
    }
}

// ------------------------------------------------------------------ scene card

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardPage(
    card: JishuPage.Card,
    pageIndex: Int,
    sitting: JishuSitting,
    drill: ConjugationDrillState,
    scene: SceneContext?,
    cover: Boolean,
    ttsWorkerUrl: String,
    autoSpeak: Boolean,
    aids: ReadingAids,
    actions: SittingActions,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val item = card.item
    val voice = rememberVoiceOptions(item.jaText, hasSource = item.audioUrl.isNotBlank())
    val audio = rememberLessonAudioController()
    val cue = remember(item.id, voice.selected) {
        voice.cue(
            PromptAudio.Source(item.audioUrl, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = item.jaText)
                .takeIf { item.audioUrl.isNotBlank() },
            item.jaText,
        )
    }
    val play = { audio.play(cue, ttsWorkerUrl) }
    // A new card speaks only when 自动朗读 is on; switching the voice always lets you hear it.
    LaunchedEffect(item.id, pageIndex) { if (autoSpeak) play() }
    var heardVoice by remember { mutableStateOf(voice.selected) }
    LaunchedEffect(voice.selected) {
        if (voice.selected != heardVoice) {
            heardVoice = voice.selected
            play()
        }
    }
    val aided = aids.ruby || aids.romaji
    LaunchedEffect(item.jaText, aided) { if (aided) aids.annotator.request("sentence", listOf(item.jaText)) }
    val line = remember(item.jaText, aids.annotator.resultFor(item.jaText)) {
        LineReading.build(item.jaText, aids.annotator.resultFor(item.jaText))
    }
    val playing = audio.playbackState.phase == AudioPlaybackPhase.Playing || audio.playbackState.phase == AudioPlaybackPhase.Loading
    var revealed by rememberSaveable(item.id, pageIndex) { mutableStateOf(false) }
    val cardNumber = sitting.pages.take(pageIndex + 1).count { it is JishuPage.Card }
    val (jp, _) = splitTitle(drill.titleOf(sitting.pointId))
    var moving by remember(pageIndex) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("第 ${drill.lessonNumber(sitting.pointId)} 課 · $jp", Modifier.weight(1f))
                Eyebrow("$cardNumber / ${sitting.cardCount}")
            }

            // The scene: line before (sunken), the line itself, line after (sunken).
            MangaPanel(Modifier.fillMaxWidth()) {
                Column {
                    scene?.before?.takeIf { it.isNotBlank() }?.let { SceneBand(it, top = true) }
                    Box {
                        // The tone behind the voice pill ripples out from it while the line plays.
                        VoiceTone(
                            playing = playing,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 36.dp, y = 18.dp).size(200.dp, 96.dp).rotate(-12f),
                            origin = Offset(0.45f, 0.42f),
                        )
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)) {
                            ReadingLineText(
                                line,
                                item.spanStart until item.spanEnd,
                                showRuby = aids.ruby,
                                showRomaji = aids.romaji,
                                style = AjlTheme.type.jpBody.copy(fontSize = 25.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium),
                            )
                            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                                VoiceSwitchPill(
                                    playing = playing,
                                    options = voice.labels,
                                    selected = voice.index,
                                    onSelect = voice.onSelect,
                                    onClick = { audio.toggle(cue, ttsWorkerUrl) },
                                )
                            }
                        }
                    }
                    scene?.after?.takeIf { it.isNotBlank() }?.let { SceneBand(it, top = false) }
                }
            }

            // 拆解: word blocks, the grammar point's own block in the work colour.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("拆解")
                FormulaRow(
                    item.formula,
                    item.group,
                    readings = { words -> line.readingsOf(words, item.spanStart) },
                    showRuby = aids.ruby,
                    showRomaji = aids.romaji,
                )
            }

            // 意思: the translation (hidden under 遮る until tapped), then 用法 + 说明.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.zh.isNotBlank()) {
                    if (cover && !revealed) {
                        CoveredLine { revealed = true }
                    } else {
                        Text(item.zh, style = AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
                    }
                }
                if (item.sense.isNotBlank() || item.note.isNotBlank()) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (item.sense.isNotBlank()) {
                            Text(
                                item.sense,
                                style = AjlTheme.type.caption.copy(fontSize = 12.sp),
                                color = work.accent,
                                modifier = Modifier
                                    .border(1.dp, colors.line2, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                        if (item.note.isNotBlank()) {
                            Text(item.note, style = AjlTheme.type.body.copy(fontSize = 14.sp, lineHeight = 22.sp), color = colors.ink2, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (card.others.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("这句还有")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        card.others.forEach { other ->
                            Text(
                                splitTitle(other.pointTitle).first,
                                style = AjlTheme.type.jpBody.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                color = colors.ink2,
                                modifier = Modifier
                                    .border(1.dp, colors.line2, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlineButton(
                "もう一回",
                { if (!moving) { moving = true; actions.onAgain() } },
                Modifier.weight(2f).height(56.dp),
                ink = true,
            )
            InkButton(
                text = "覚えた",
                onClick = { if (!moving) { moving = true; actions.onRemember() } },
                trailingArrow = true,
                height = 56.dp,
                modifier = Modifier.weight(3f),
            )
        }
    }
}

@Composable
private fun SceneBand(text: String, top: Boolean) {
    val colors = AjlTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.sunken)) {
        if (!top) Hairline()
        Text(
            stripSpeaker(text),
            style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 24.sp),
            color = colors.ink3,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        if (top) Hairline()
    }
}

// ------------------------------------------------------------------ helpers

/** 「ちゃう／じゃう（てしまう的缩约）」 → (ちゃう／じゃう, てしまう的缩约); titles not ending in a bracket stay whole. */
internal fun splitTitle(title: String): Pair<String, String> {
    val open = title.lastIndexOf('（')
    if (open <= 0 || !title.endsWith('）')) return title.trim() to ""
    return title.substring(0, open).trim() to title.substring(open + 1, title.length - 1).trim()
}

/** Subtitle lines carry the speaker as 「（エミリア）」; the card shows the words only. */
internal fun stripSpeaker(line: String): String =
    line.replace(Regex("""[（(][^）)]{1,12}[）)]"""), "").trim()

private val PointIdPattern = Regex("""\b[a-h]_[a-z_]+\b""")

/** Lesson text sometimes names another 課 by its id (c_teshimau); show its title instead. */
private fun readableIds(text: String, drill: ConjugationDrillState): String =
    PointIdPattern.replace(text) { m -> drill.titleOf(m.value).takeIf { it.isNotBlank() }?.let { splitTitle(it).first } ?: m.value }

/** Reading aids from 設定 · 読み方 (假名注音 / 罗马音) and the furigana source for this sitting. */
internal class ReadingAids(val ruby: Boolean, val romaji: Boolean, val annotator: FuriganaAnnotator)
