package com.animejapaneselab.nativeapp.ui.screens.session

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.PronunciationAssessmentStatus
import com.animejapaneselab.nativeapp.data.PronunciationEvaluation
import com.animejapaneselab.nativeapp.data.PronunciationSegment
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.ui.PronunciationEvaluationPhase
import com.animejapaneselab.nativeapp.ui.PronunciationEvaluationState
import com.animejapaneselab.nativeapp.ui.audio.PronunciationMaximumDurationMs
import com.animejapaneselab.nativeapp.ui.audio.PronunciationMinimumDurationMs
import com.animejapaneselab.nativeapp.ui.audio.PronunciationWavRecorder
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.ToolPanel
import com.animejapaneselab.nativeapp.ui.design.rememberTypewriterState
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.RubyText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pronunciation callbacks, grouped so the question signature stays readable. */
internal class PronunciationActions(
    val state: PronunciationEvaluationState,
    val onEvaluate: (String, String, ByteArray, Long) -> Unit,
    val onRetry: () -> Unit,
    val onReset: () -> Unit,
)

/**
 * 跟读 — the line in the 会話窓 (furigana once typed out, Chinese gloss under it), then either the
 * real pronunciation assessment (record → score sheet) or, without a scorable sentence, the
 * three-way self check. Pronunciation details were not drawn: derived from the tool-panel rules
 * (1px line, 12dp) with mono numbers and work-colour lines.
 */
@Composable
internal fun ShadowingQuestion(
    env: LessonQuestionEnv,
    node: ShadowingNode,
    settings: LabSettings,
    pronunciation: PronunciationActions,
    modifier: Modifier = Modifier,
) {
    val sentence = node.sentence
    val furigana = rememberFuriganaAnnotator(settings)
    LaunchedEffect(sentence.ja, settings.showFurigana) {
        if (settings.showFurigana) furigana.request("sentence", listOf(sentence.ja))
    }
    val typewriter = rememberTypewriterState(sentence.ja)
    var selfCheck by rememberSaveable(node.id) { mutableStateOf(node.pronunciationSentenceId == null) }
    val sentenceId = node.pronunciationSentenceId

    val scene: @Composable () -> Unit = {
        SceneDialogue(env.character, env.speaker, node.audio, env, typewriter) { visible ->
            if (typewriter.isComplete) {
                RubyText(
                    text = sentence.ja,
                    furigana = if (settings.showFurigana) furigana.resultFor(sentence.ja) else null,
                    style = dialogueStyle(),
                    rubyStyle = AjlTheme.type.jpLabel,
                    color = AjlTheme.colors.ink,
                    rubyColor = AjlTheme.colors.ink3,
                )
            } else {
                Text(visible, style = dialogueStyle(), color = AjlTheme.colors.ink)
            }
            if (sentence.meaningZh.isNotBlank()) {
                Text(
                    sentence.meaningZh,
                    style = AjlTheme.type.caption,
                    color = AjlTheme.colors.ink3,
                    modifier = Modifier.graphicsLayer { alpha = if (typewriter.isComplete) 1f else 0f },
                )
            }
            if (settings.showRomaji && sentence.romaji.isNotBlank() && typewriter.isComplete) {
                Text(sentence.romaji, style = AjlTheme.type.meta, color = AjlTheme.colors.ink3)
            }
        }
    }

    if (selfCheck || sentenceId == null) {
        SelfCheck(env, node, scene, modifier)
    } else {
        Assessment(env, node, sentenceId, pronunciation, scene, onSelfCheck = {
            pronunciation.onReset()
            selfCheck = true
        }, modifier = modifier)
    }
}

@Composable
private fun SelfCheck(
    env: LessonQuestionEnv,
    node: ShadowingNode,
    scene: @Composable () -> Unit,
    modifier: Modifier,
) {
    var pending by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    var checking by remember(node.id) { mutableStateOf(false) }
    val committed = env.feedback?.selected
    LessonQuestionScaffold(
        env = env,
        modifier = modifier,
        bottomBar = {
            LessonActionBar(
                primaryLabel = "就是这样",
                enabled = pending != null && !checking,
                onPrimary = {
                    val choice = pending ?: return@LessonActionBar
                    if (!checking) {
                        checking = true
                        env.onSubmit(choice)
                    }
                },
                quietLabel = "跳过".takeIf { env.onSkip != null },
                onQuiet = env.onSkip,
                quietEnabled = !checking,
            )
        },
    ) {
        scene()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.ratings.forEachIndexed { index, rating ->
                OptionRow(
                    text = rating,
                    state = optionStateFor(rating, pending, committed, if (env.feedback?.correct == true) committed else null),
                    onClick = { if (!env.answered) pending = rating },
                    detail = when (index) {
                        0 -> "节奏、停顿、语气都贴近"
                        1 -> "跟上了，细节还能调"
                        else -> "再听一遍"
                    },
                    leading = "${index + 1}",
                )
            }
        }
    }
}

private enum class RecordPhase { Idle, Recording, Packing }

@Composable
private fun Assessment(
    env: LessonQuestionEnv,
    node: ShadowingNode,
    sentenceId: String,
    pronunciation: PronunciationActions,
    scene: @Composable () -> Unit,
    onSelfCheck: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember(node.id) { PronunciationWavRecorder() }
    val evaluation = pronunciation.state.takeIf { it.nodeId == node.id } ?: PronunciationEvaluationState()
    var phase by remember(node.id) { mutableStateOf(RecordPhase.Idle) }
    var elapsedMs by remember(node.id) { mutableLongStateOf(0L) }
    var localMessage by remember(node.id) { mutableStateOf("") }
    var submitted by remember(node.id) { mutableStateOf(false) }
    val latestEvaluate by rememberUpdatedState(pronunciation.onEvaluate)
    val latestReset by rememberUpdatedState(pronunciation.onReset)

    fun startNow() {
        if (phase != RecordPhase.Idle || evaluation.phase == PronunciationEvaluationPhase.Loading) return
        latestReset()
        localMessage = ""
        elapsedMs = 0L
        runCatching { recorder.start() }
            .onSuccess { phase = RecordPhase.Recording }
            .onFailure { localMessage = "麦克风没有启动，检查录音权限后再试。" }
    }

    val latestStart by rememberUpdatedState<() -> Unit> { startNow() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) latestStart() else localMessage = "需要麦克风权限才能评分。"
    }

    fun record() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startNow()
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun recordAgain() {
        pronunciation.onReset()
        record()
    }

    fun stop() {
        if (phase != RecordPhase.Recording) return
        phase = RecordPhase.Packing
        scope.launch {
            val capture = try {
                Result.success(withContext(Dispatchers.IO) { recorder.stop() })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }
            phase = RecordPhase.Idle
            capture.fold(
                onSuccess = { audio ->
                    elapsedMs = audio.durationMs
                    if (audio.durationMs < PronunciationMinimumDurationMs) {
                        localMessage = "太短了，把整句读完。"
                    } else {
                        localMessage = ""
                        latestEvaluate(node.id, sentenceId, audio.wavBytes, audio.durationMs)
                    }
                },
                onFailure = { localMessage = "录音没有处理成功，再录一次。" },
            )
        }
    }

    val latestStop by rememberUpdatedState<() -> Unit> { stop() }
    LaunchedEffect(phase, node.id) {
        if (phase != RecordPhase.Recording) return@LaunchedEffect
        val startedAt = android.os.SystemClock.elapsedRealtime()
        while (phase == RecordPhase.Recording && elapsedMs < PronunciationMaximumDurationMs) {
            delay(100)
            elapsedMs = (android.os.SystemClock.elapsedRealtime() - startedAt).coerceAtMost(PronunciationMaximumDurationMs)
        }
        if (phase == RecordPhase.Recording) latestStop()
    }
    DisposableEffect(recorder) { onDispose { recorder.cancel() } }

    val result = evaluation.result
    val score = result?.score?.takeIf { result.assessmentStatus == PronunciationAssessmentStatus.Scored }
    val loading = phase == RecordPhase.Packing || evaluation.phase == PronunciationEvaluationPhase.Loading
    val message = when {
        localMessage.isNotBlank() -> localMessage
        evaluation.phase == PronunciationEvaluationPhase.Error -> evaluation.message
        result != null && score == null -> evaluation.message.ifBlank { "这次没听清，再录一次。" }
        else -> ""
    }

    LessonQuestionScaffold(
        env = env,
        modifier = modifier,
        bottomBar = {
            when {
                phase == RecordPhase.Recording -> LessonActionBar(
                    primaryLabel = "停止",
                    onPrimary = ::stop,
                    caption = "录音中 · ${elapsedMs / 1000}.${(elapsedMs % 1000) / 100} s",
                    progress = (elapsedMs / PronunciationMaximumDurationMs.toFloat()).coerceIn(0f, 1f),
                )
                loading -> LessonActionBar(primaryLabel = "评分中", onPrimary = {}, loading = true)
                score != null -> LessonActionBar(
                    primaryLabel = "采用这次",
                    enabled = !submitted,
                    onPrimary = {
                        if (!submitted) {
                            submitted = true
                            env.onSubmit(score.lessonRating)
                        }
                    },
                    quietLabel = "再录一次",
                    onQuiet = ::recordAgain,
                )
                evaluation.phase == PronunciationEvaluationPhase.Error && evaluation.canRetry -> LessonActionBar(
                    primaryLabel = "重新上传",
                    onPrimary = pronunciation.onRetry,
                    quietLabel = "重新录音",
                    onQuiet = ::recordAgain,
                )
                message.isNotBlank() -> LessonActionBar(
                    primaryLabel = "重新录音",
                    onPrimary = ::recordAgain,
                    quietLabel = "改用自评",
                    onQuiet = onSelfCheck,
                )
                else -> LessonActionBar(
                    primaryLabel = "开始跟读",
                    onPrimary = ::record,
                    quietLabel = "改用自评",
                    onQuiet = onSelfCheck,
                )
            }
        },
    ) {
        scene()
        when {
            phase == RecordPhase.Recording -> RecordingMark(elapsedMs)
            loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LoadingDots(delayMillis = 0)
                if (evaluation.message.isNotBlank()) Text(evaluation.message, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
            }
            score != null && result != null -> ScoreSheet(result)
            message.isNotBlank() -> Text(message, style = AjlTheme.type.body, color = AjlTheme.colors.ink2)
        }
    }
}

/** A breathing bad-colour dot + mono timer while the mic is open. */
@Composable
private fun RecordingMark(elapsedMs: Long) {
    val colors = AjlTheme.colors
    val reduced = rememberReducedMotion()
    val pulse by if (reduced) {
        remember { mutableStateOf(1f) }
    } else {
        rememberInfiniteTransition(label = "rec").animateFloat(0.35f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "rec-dot")
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .graphicsLayer { alpha = pulse }
                .background(colors.bad, CircleShape),
        )
        Text(
            "${elapsedMs / 1000}.${(elapsedMs % 1000) / 100}",
            style = AjlTheme.type.meta.copy(fontSize = 28.sp, lineHeight = 32.sp),
            color = colors.ink,
        )
        Text("/ ${PronunciationMaximumDurationMs / 1000} s", style = AjlTheme.type.meta, color = colors.ink3)
    }
}

/**
 * 评分单: big mono overall + band, reliability kept apart (it is a separate measure), five
 * metric lines, what was heard, and up to three segments that need attention.
 */
@Composable
private fun ScoreSheet(result: PronunciationEvaluation) {
    val colors = AjlTheme.colors
    val score = result.score ?: return
    ToolPanel(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    score.overall.toString(),
                    style = AjlTheme.type.meta.copy(fontSize = 44.sp, lineHeight = 46.sp, fontWeight = FontWeight.Medium),
                    color = colors.ink,
                )
                Column(Modifier.weight(1f).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(bandLabel(score.band), style = AjlTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                    Text(
                        "可靠度 ${(result.engine.reliability * 100).toInt().coerceIn(0, 100)}%",
                        style = AjlTheme.type.meta,
                        color = colors.ink3,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("准确", score.accuracy)
                Metric("完整", score.completeness)
                Metric("清晰", score.clarity)
                Metric("流利", score.fluency)
                Metric(if (result.reference.usesEstimatedTiming) "节奏*" else "节奏", score.rhythm)
            }
            result.recognized?.text?.takeIf { it.isNotBlank() }?.let { heard ->
                Hairline()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("听到的", style = AjlTheme.type.meta, color = colors.ink3)
                    Text(heard, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
                }
            }
            val issues = result.segments.filter { it.needsAttention }.take(3)
            if (issues.isNotEmpty()) {
                Column {
                    Hairline()
                    issues.forEach { segment ->
                        SegmentRow(segment)
                        Hairline()
                    }
                }
            }
            result.feedback.take(3).forEach { tip ->
                Text(tip, style = AjlTheme.type.caption, color = colors.ink2)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = AjlTheme.type.caption, color = AjlTheme.colors.ink2, modifier = Modifier.width(40.dp))
        ProgressLine(value / 100f, Modifier.weight(1f))
        Text(value.toString(), style = AjlTheme.type.meta, color = AjlTheme.colors.ink, modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun SegmentRow(segment: PronunciationSegment) {
    val colors = AjlTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(segment.surface.ifBlank { segment.expected }, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 24.sp), color = colors.ink)
            Text(
                segment.message.ifBlank {
                    when (segment.status) {
                        "omission" -> "可能漏读了"
                        "extra" -> "可能多读或重复了"
                        "pause" -> "这里停得有点久"
                        else -> "这一段不够清楚"
                    }
                },
                style = AjlTheme.type.caption,
                color = colors.ink3,
            )
        }
        segment.score?.let { Text(it.toString(), style = AjlTheme.type.meta, color = colors.bad) }
    }
}

private fun bandLabel(band: String): String = when (band) {
    "excellent" -> "几乎就是原声"
    "good" -> "读得不错"
    "fair" -> "基本跟上了"
    else -> "再练一次"
}
