package com.animejapaneselab.nativeapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.PairMatchNode
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import com.animejapaneselab.nativeapp.data.StudyCardNode
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.data.TileOrderNode
import com.animejapaneselab.nativeapp.domain.AnswerFeedback
import com.animejapaneselab.nativeapp.ui.LabTab
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackState
import com.animejapaneselab.nativeapp.ui.audio.rememberFeedbackSoundController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.LearningChoiceButton
import com.animejapaneselab.nativeapp.ui.components.LearningTileButton
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton
import com.animejapaneselab.nativeapp.ui.components.RewardMetricCard
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.StructuredAiResultCard
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.feedback.performAnswerFeedbackHaptic
import com.animejapaneselab.nativeapp.ui.feedback.performCompletionFeedbackHaptic
import com.animejapaneselab.nativeapp.ui.theme.LabPalette
import kotlinx.coroutines.delay

@Composable
fun LessonHubScreen(
    uiState: LabUiState,
    onStartLesson: () -> Unit,
    onLessonModeSelected: (LessonMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LessonHubHero(uiState = uiState)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(eyebrow = "训练路线", title = "切换训练重点")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LessonMode.entries, key = { it.name }) { mode ->
                        FilterChip(
                            selected = uiState.lessonMode == mode,
                            onClick = { onLessonModeSelected(mode) },
                            label = { Text(mode.titleLabel, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(eyebrow = "训练地图", title = "今天路径")
                TagChip(
                    text = "剩余 ${maxOf(0, uiState.lesson.nodes.size - uiState.lesson.answered)}",
                    selected = true,
                )
            }
        }
        itemsIndexed(uiState.lesson.nodes, key = { _, node -> node.id }) { index, node ->
            val pendingFeedback = uiState.lesson.feedback != null && index == uiState.lesson.index
            LessonPreviewNode(
                index = index,
                node = node,
                done = index < uiState.lesson.index,
                current = index == uiState.lesson.index,
                pendingFeedback = pendingFeedback,
                onStartLesson = onStartLesson,
            )
        }
    }
}

@Composable
private fun LessonHubHero(
    uiState: LabUiState,
    modifier: Modifier = Modifier,
) {
    val currentNode = uiState.lesson.currentNode
    val feedback = uiState.lesson.feedback
    val currentPrompt = when {
        feedback?.correct == true -> "已答对，回来继续下一步"
        feedback?.correct == false -> "已作答，回来查看反馈"
        else -> currentNode?.prompt ?: "选择一个训练重点后开始"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "今日训练",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                    )
                    Text(
                        text = uiState.focus.lessonTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = currentPrompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    modifier = Modifier.size(50.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Stars, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            }
            LinearProgressIndicator(
                progress = { uiState.lesson.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LessonHubStat(label = "题目", value = "${uiState.lesson.nodes.size}", modifier = Modifier.weight(1f))
                LessonHubStat(label = "已答", value = "${uiState.lesson.answered}", modifier = Modifier.weight(1f))
                LessonHubStat(label = "正确", value = "${uiState.lesson.correct}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LessonHubStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LessonPreviewNode(
    index: Int,
    node: LessonNode,
    done: Boolean,
    current: Boolean,
    pendingFeedback: Boolean,
    onStartLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alignRight = index % 2 == 1
    val locked = !done && !current
    val statusLabel = pathStatusLabel(done = done, current = current, pendingFeedback = pendingFeedback)
    if (current) {
        Surface(
            onClick = onStartLesson,
            modifier = modifier.fillMaxWidth(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            LessonPreviewNodeRow(
                index = index,
                node = node,
                alignRight = alignRight,
                current = current,
                done = done,
                locked = locked,
                pendingFeedback = pendingFeedback,
                statusLabel = statusLabel,
                onStartLesson = onStartLesson,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        LessonPreviewNodeRow(
            index = index,
            node = node,
            alignRight = alignRight,
            current = current,
            done = done,
            locked = locked,
            pendingFeedback = pendingFeedback,
            statusLabel = statusLabel,
            onStartLesson = onStartLesson,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LessonPreviewNodeRow(
    index: Int,
    node: LessonNode,
    alignRight: Boolean,
    current: Boolean,
    done: Boolean,
    locked: Boolean,
    pendingFeedback: Boolean,
    statusLabel: String,
    onStartLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (alignRight) Spacer(modifier = Modifier.weight(0.16f))
        if (alignRight && current) {
            CurrentLessonBubble(
                index = index,
                node = node,
                pendingFeedback = pendingFeedback,
                onStartLesson = onStartLesson,
                modifier = Modifier.weight(1f),
            )
        }
        LessonPathOrb(
            current = current,
            done = done,
            locked = locked,
            pendingFeedback = pendingFeedback,
            node = node,
            onStartLesson = onStartLesson,
        )
        if (!alignRight && current) {
            CurrentLessonBubble(
                index = index,
                node = node,
                pendingFeedback = pendingFeedback,
                onStartLesson = onStartLesson,
                modifier = Modifier.weight(1f),
            )
        }
        if (!current) {
            PathNodeCaption(
                index = index,
                title = node.title,
                statusLabel = statusLabel,
                highlighted = done,
                alignRight = alignRight,
                modifier = Modifier.weight(1f),
            )
        }
        if (!alignRight) Spacer(modifier = Modifier.weight(0.16f))
    }
}

@Composable
private fun LessonPathOrb(
    current: Boolean,
    done: Boolean,
    locked: Boolean,
    pendingFeedback: Boolean,
    node: LessonNode,
    onStartLesson: () -> Unit,
) {
    val modifier = Modifier.size(if (current) 72.dp else 58.dp)
    val color = when {
        done -> MaterialTheme.colorScheme.primary
        current -> LabPalette.Yellow
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        done -> MaterialTheme.colorScheme.onPrimary
        current -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (current) {
        Surface(
            onClick = onStartLesson,
            modifier = modifier,
            color = color,
            contentColor = contentColor,
            shape = CircleShape,
            tonalElevation = 6.dp,
        ) {
            LessonPathOrbContent(
                done = done,
                current = current,
                locked = locked,
                pendingFeedback = pendingFeedback,
                node = node,
            )
        }
    } else {
        Surface(
            modifier = modifier,
            color = color,
            contentColor = contentColor,
            shape = CircleShape,
            tonalElevation = 0.dp,
        ) {
            LessonPathOrbContent(
                done = done,
                current = current,
                locked = locked,
                pendingFeedback = pendingFeedback,
                node = node,
            )
        }
    }
}

@Composable
private fun LessonPathOrbContent(
    done: Boolean,
    current: Boolean,
    locked: Boolean,
    pendingFeedback: Boolean,
    node: LessonNode,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = when {
                done -> Icons.Rounded.CheckCircle
                pendingFeedback -> Icons.AutoMirrored.Rounded.ArrowForward
                current -> Icons.Rounded.PlayArrow
                locked -> Icons.Rounded.Lock
                else -> node.pathIcon()
            },
            contentDescription = null,
        )
    }
}

@Composable
private fun CurrentLessonBubble(
    index: Int,
    node: LessonNode,
    pendingFeedback: Boolean,
    onStartLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onStartLesson,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}. ${node.title}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TagChip(if (pendingFeedback) "下一步" else "继续", selected = true)
            }
            Text(
                text = if (pendingFeedback) "已作答，回到反馈页继续" else node.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PathNodeCaption(
    index: Int,
    title: String,
    statusLabel: String,
    highlighted: Boolean,
    alignRight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "${index + 1}. $title",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            textAlign = if (alignRight) TextAlign.End else TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun LessonNode.pathIcon() = when (typeLabel) {
    "读空气" -> Icons.Rounded.Psychology
    "学习卡" -> Icons.Rounded.AutoStories
    else -> Icons.Rounded.Bolt
}

private fun pathStatusLabel(done: Boolean, current: Boolean, pendingFeedback: Boolean): String {
    return when {
        done -> "已完成"
        pendingFeedback -> "待继续"
        current -> "继续"
        else -> "待解锁"
    }
}

@Composable
fun LessonScreen(
    uiState: LabUiState,
    onExit: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    onAskAi: () -> Unit,
    onAiQuestionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = uiState.lesson
    val audioController = rememberLessonAudioController()
    val feedbackSoundController = rememberFeedbackSoundController()
    val feedbackView = LocalView.current
    val lessonScrollState = rememberScrollState()

    BackHandler(onBack = onExit)

    if (session.isComplete) {
        LessonComplete(
            uiState = uiState,
            onExit = onExit,
            onRestart = onRestart,
            onNextBatch = onNextBatch,
            modifier = modifier,
        )
        return
    }

    val node = session.currentNode ?: return
    val exitLabel = lessonExitLabel(uiState.selectedTab)
    LaunchedEffect(node.id) {
        lessonScrollState.scrollTo(0)
    }
    LaunchedEffect(node.id, uiState.settings.autoSpeak, node.audio) {
        if (uiState.settings.autoSpeak && node.audio.autoPlay) {
            audioController.play(node.audio, uiState.settings.ttsWorkerUrl, autoAttempt = true)
        }
    }
    LaunchedEffect(session.feedback?.selected, session.feedback?.correct) {
        val feedback = session.feedback ?: return@LaunchedEffect
        if (uiState.settings.feedbackSounds) feedbackSoundController.play(feedback.correct)
        feedbackView.performAnswerFeedbackHaptic(feedback.correct)
    }
    LaunchedEffect(node.id, session.feedback?.selected) {
        if (session.feedback != null) {
            delay(90)
            lessonScrollState.animateScrollTo(0)
        }
    }
    LaunchedEffect(uiState.aiCoach.status, uiState.aiCoach.answer) {
        if (uiState.aiCoach.status == SyncStatus.Loading) {
            delay(120)
            lessonScrollState.animateScrollTo(lessonScrollState.maxValue)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LessonTopBar(
            index = session.index,
            total = session.nodes.size,
            correct = session.correct,
            answered = session.answered,
            progress = session.progress,
            episodeLabel = uiState.focus.episodeLabel,
            exitLabel = exitLabel,
            onExit = onExit,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(lessonScrollState)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(eyebrow = node.title, title = node.prompt)
            AudioStrip(
                node = node,
                audio = node.audio,
                playbackState = audioController.playbackState,
                onPlay = { cue -> audioController.play(cue, uiState.settings.ttsWorkerUrl) },
            )
            LessonNodeContent(
                node = node,
                feedback = session.feedback,
                onSubmitAnswer = onSubmitAnswer,
                onSpeakText = { text -> audioController.speakText(text, uiState.settings.ttsWorkerUrl) },
                onPairFeedback = { correct ->
                    if (uiState.settings.feedbackSounds) feedbackSoundController.play(correct)
                    feedbackView.performAnswerFeedbackHaptic(correct)
                },
            )
            if (uiState.aiCoach.answer.isNotBlank() || uiState.aiCoach.status == SyncStatus.Loading || uiState.aiCoach.status == SyncStatus.Error) {
                AiCoachCard(
                    uiState = uiState,
                    onAskAi = onAskAi,
                    onQuestionChange = onAiQuestionChange,
                )
            }
        }
        FeedbackDock(
            feedback = session.feedback,
            correctXp = 12,
            onContinue = onContinue,
            aiStatus = uiState.aiCoach.status,
            onAskAi = onAskAi,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetLessonBar(
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TagChip("单点训练", selected = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonModeBar(
    selectedMode: LessonMode,
    onModeSelected: (LessonMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LessonMode.entries.forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label, fontWeight = FontWeight.Bold) },
            )
        }
    }
}

@Composable
private fun LessonTopBar(
    index: Int,
    total: Int,
    correct: Int,
    answered: Int,
    progress: Float,
    episodeLabel: String,
    exitLabel: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = exitLabel)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${index + 1} / $total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = episodeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "正确 $correct / 已答 $answered",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun AudioStrip(
    node: LessonNode,
    audio: PromptAudio,
    playbackState: AudioPlaybackState,
    onPlay: (PromptAudio) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (audio == PromptAudio.None) return

    val isReliableSource = audio is PromptAudio.Source && audio.reliability == AudioReliability.Verified
    val accentColor = if (isReliableSource) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val compactHint = when {
        playbackState.message.isNotBlank() -> playbackState.message
        isReliableSource -> "原声可靠"
        audio is PromptAudio.Source -> "原声需确认"
        else -> "标准语音"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { onPlay(audio) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                Text(audio.label, modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = node.sourceLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = compactHint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            (audio as? PromptAudio.Source)?.let { sourceAudio ->
                val fallback = sourceAudio.fallbackTtsText
                if (fallback.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onPlay(PromptAudio.Tts(fallback, autoPlay = false, label = sourceAudio.fallbackLabel)) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(sourceAudio.fallbackLabel, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiCoachCard(
    uiState: LabUiState,
    onAskAi: () -> Unit,
    onQuestionChange: (String) -> Unit,
) {
    LabCard {
        SectionTitle(eyebrow = "智能讲解", title = "解释这题")
        OutlinedTextField(
            value = uiState.aiCoach.question,
            onValueChange = onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            label = { Text("想问什么") },
        )
        PrimaryButton(
            text = when (uiState.aiCoach.status) {
                com.animejapaneselab.nativeapp.data.SyncStatus.Loading -> "分析中"
                com.animejapaneselab.nativeapp.data.SyncStatus.Error -> "重试讲解"
                else -> "讲解这题"
            },
            onClick = onAskAi,
            enabled = uiState.aiCoach.status != com.animejapaneselab.nativeapp.data.SyncStatus.Loading,
        )
        AnimatedVisibility(
            visible = uiState.aiCoach.answer.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            StructuredAiResultCard(
                result = uiState.aiCoach.result,
                fallbackText = uiState.aiCoach.answer,
            )
        }
    }
}

@Composable
private fun LessonNodeContent(
    node: LessonNode,
    feedback: AnswerFeedback?,
    onSubmitAnswer: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onPairFeedback: (Boolean) -> Unit,
) {
    when (node) {
        is StudyCardNode -> StudyCardNodeView(node = node, disabled = feedback != null, onSubmitAnswer = onSubmitAnswer)
        is PairMatchNode -> PairMatchNodeView(
            node = node,
            disabled = feedback != null,
            onSubmitAnswer = onSubmitAnswer,
            onSpeakText = onSpeakText,
            onPairFeedback = onPairFeedback,
        )
        is SingleChoiceNode -> SingleChoiceNodeView(node = node, feedback = feedback, onSubmitAnswer = onSubmitAnswer)
        is ClozeNode -> ClozeNodeView(node = node, feedback = feedback, onSubmitAnswer = onSubmitAnswer)
        is TileOrderNode -> TileOrderNodeView(node = node, feedback = feedback, onSubmitAnswer = onSubmitAnswer)
        is ShadowingNode -> ShadowingNodeView(node = node, feedback = feedback, onSubmitAnswer = onSubmitAnswer)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyCardNodeView(
    node: StudyCardNode,
    disabled: Boolean,
    onSubmitAnswer: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = node.japanese,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = node.reading,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = node.meaningZh,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            node.notes.forEach { note -> TagChip(text = note) }
        }
        Text(
            text = node.explanation,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!disabled) {
            PrimaryButton(text = "我学完了", onClick = { onSubmitAnswer(node.expectedAnswer) })
        }
    }
}

@Composable
private fun PairMatchNodeView(
    node: PairMatchNode,
    disabled: Boolean,
    onSubmitAnswer: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onPairFeedback: (Boolean) -> Unit,
) {
    val matched = remember(node.id) { mutableStateListOf<String>() }
    var selectedLeft by remember(node.id) { mutableStateOf<String?>(null) }
    var wrongRight by remember(node.id) { mutableStateOf<String?>(null) }
    val rightItems = remember(node.id) { node.pairs.reversed() }

    LaunchedEffect(wrongRight) {
        if (wrongRight != null) {
            delay(480)
            wrongRight = null
            selectedLeft = null
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            node.pairs.forEach { pair ->
                ChoiceButton(
                    text = pair.left,
                    selected = selectedLeft == pair.id,
                    correct = matched.contains(pair.id),
                    wrong = false,
                    enabled = !disabled && !matched.contains(pair.id),
                    onClick = { selectedLeft = pair.id },
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            rightItems.forEach { pair ->
                ChoiceButton(
                    text = pair.right,
                    selected = false,
                    correct = matched.contains(pair.id),
                    wrong = wrongRight == pair.id,
                    enabled = !disabled && !matched.contains(pair.id),
                    onClick = {
                        if (pair.audioText.isNotBlank()) onSpeakText(pair.audioText)
                        if (selectedLeft == pair.id) {
                            matched.add(pair.id)
                            selectedLeft = null
                            if (matched.size == node.pairs.size) {
                                onSubmitAnswer(node.expectedAnswer)
                            } else {
                                onPairFeedback(true)
                            }
                        } else {
                            wrongRight = pair.id
                            onPairFeedback(false)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SingleChoiceNodeView(
    node: SingleChoiceNode,
    feedback: AnswerFeedback?,
    onSubmitAnswer: (String) -> Unit,
) {
    var selected by remember(node.id) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        node.body?.let {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "提示",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        node.choices.forEach { choice ->
            ChoiceButton(
                text = choice,
                selected = selected == choice,
                correct = feedback != null && choice == node.answer,
                wrong = feedback != null && selected == choice && choice != node.answer,
                enabled = feedback == null,
                onClick = {
                    selected = choice
                    onSubmitAnswer(choice)
                },
            )
        }
    }
}

@Composable
private fun ClozeNodeView(
    node: ClozeNode,
    feedback: AnswerFeedback?,
    onSubmitAnswer: (String) -> Unit,
) {
    var selected by remember(node.id) { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "填空",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = buildString {
                    append(node.before)
                    append(selected ?: "____")
                    append(node.after)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        }
        node.choices.forEach { choice ->
            ChoiceButton(
                text = choice.value,
                detail = choice.note,
                selected = selected == choice.value,
                correct = feedback != null && choice.value == node.answer,
                wrong = feedback != null && selected == choice.value && choice.value != node.answer,
                enabled = feedback == null,
                onClick = { selected = choice.value },
            )
        }
        if (feedback == null) {
            PrimaryButton(
                text = "检查",
                onClick = { selected?.let(onSubmitAnswer) },
                enabled = selected != null,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TileOrderNodeView(
    node: TileOrderNode,
    feedback: AnswerFeedback?,
    onSubmitAnswer: (String) -> Unit,
) {
    val selected = remember(node.id) { mutableStateListOf<String>() }
    val disabled = feedback != null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "组句",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = node.displayText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
            shadowElevation = 1.dp,
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selected.isEmpty()) {
                    Text(
                        text = "选择下方语块",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                selected.forEachIndexed { index, tile ->
                    LearningTileButton(
                        text = tile,
                        onClick = { if (!disabled) selected.removeAt(index) },
                        selected = true,
                        enabled = !disabled,
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.bankTiles.forEach { tile ->
                val used = selected.count { it == tile }
                val available = node.bankTiles.count { it == tile }
                LearningTileButton(
                    text = tile,
                    onClick = { selected.add(tile) },
                    enabled = !disabled && used < available,
                )
            }
        }
        if (!disabled) {
            PrimaryButton(
                text = "检查",
                onClick = { onSubmitAnswer(selected.joinToString("")) },
                enabled = selected.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun ShadowingNodeView(
    node: ShadowingNode,
    feedback: AnswerFeedback?,
    onSubmitAnswer: (String) -> Unit,
) {
    var selected by remember(node.id) { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "跟读",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = node.sentence.ja,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = node.sentence.reading,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = node.sentence.meaningZh,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
        node.ratings.forEach { rating ->
            ChoiceButton(
                text = rating,
                selected = selected == rating,
                correct = feedback != null && selected == rating && feedback.correct,
                wrong = feedback != null && selected == rating && !feedback.correct,
                enabled = feedback == null,
                onClick = {
                    selected = rating
                    onSubmitAnswer(rating)
                },
            )
        }
    }
}

@Composable
private fun ChoiceButton(
    text: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    selected: Boolean,
    correct: Boolean,
    wrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    LearningChoiceButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        detail = detail,
        selected = selected,
        correct = correct,
        wrong = wrong,
        enabled = enabled,
    )
}

@Composable
private fun FeedbackDock(
    feedback: AnswerFeedback?,
    correctXp: Int,
    onContinue: () -> Unit,
    aiStatus: SyncStatus,
    onAskAi: () -> Unit,
) {
    if (feedback == null) {
        return
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val color = if (feedback.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = color,
            contentColor = Color.White,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(if (feedback.correct) Icons.Rounded.Check else Icons.Rounded.Close, contentDescription = null)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (feedback.correct) "正确" else "进入错题本",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                            )
                            Surface(
                                color = Color.White,
                                contentColor = color,
                                shape = CircleShape,
                            ) {
                                Text(
                                    text = if (feedback.correct) "+$correctXp XP" else "加入复习",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                        Text(
                            text = if (feedback.correct) feedback.explanation else "正确答案：${feedback.expected}。${feedback.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = color),
                ) {
                    Text("继续", fontWeight = FontWeight.Black)
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onAskAi,
                        enabled = aiStatus != SyncStatus.Loading,
                        modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null)
                        Text(
                            text = if (aiStatus == SyncStatus.Loading) "解释中" else "讲解",
                            modifier = Modifier.padding(start = 4.dp),
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

private fun LessonNode.audioText(): String {
    return when (this) {
        is StudyCardNode -> japanese
        is SingleChoiceNode -> body ?: answer
        is ClozeNode -> before + answer + after
        is TileOrderNode -> targetTiles.joinToString("")
        is ShadowingNode -> sentence.ja
        is PairMatchNode -> pairs.firstOrNull()?.right.orEmpty()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonComplete(
    uiState: LabUiState,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedbackSoundController = rememberFeedbackSoundController()
    val feedbackView = LocalView.current
    val answered = uiState.lesson.answered
    val accuracy = if (answered == 0) 0 else (uiState.lesson.correct * 100 / answered)
    val hasMistakes = uiState.lesson.correct < answered
    val exitLabel = lessonExitLabel(uiState.selectedTab)
    val summaryMessage = if (hasMistakes) {
        "错题已经进入复习页，下一轮会优先补弱项。"
    } else {
        "本轮全对，下一轮可以继续挑战更后面的内容。"
    }
    val practicedTypes = uiState.lesson.nodes
        .map { it.typeLabel }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)
    LaunchedEffect(answered, uiState.settings.feedbackSounds) {
        delay(260)
        if (uiState.settings.feedbackSounds) feedbackSoundController.playCompletion()
        feedbackView.performCompletionFeedbackHaptic()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(82.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                Text(
                    text = "本轮完成",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = uiState.focus.lessonTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summaryMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            RewardMetricCard(
                label = "XP",
                value = "+${uiState.sessionXp}",
                icon = Icons.Rounded.Stars,
                progress = 1f,
                modifier = Modifier.weight(1f),
                highlighted = true,
            )
            RewardMetricCard(
                label = "正确率",
                value = "$accuracy%",
                icon = Icons.Rounded.Check,
                progress = accuracy / 100f,
                modifier = Modifier.weight(1f),
            )
            RewardMetricCard(
                label = "能量",
                value = "${uiState.focus.energy}/5",
                icon = Icons.Rounded.LocalFireDepartment,
                progress = uiState.focus.energy / 5f,
                modifier = Modifier.weight(1f),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "练习摘要",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "正确 ${uiState.lesson.correct} / 已答 ${uiState.lesson.answered} · 共 ${uiState.lesson.nodes.size} 题",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    practicedTypes.forEach { type ->
                        TagChip(type)
                    }
                }
            }
        }

        if (uiState.hasNextLessonBatch) {
            Button(
                onClick = onNextBatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text("继续下一批", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
            }
        } else {
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = null)
                Text("再练一轮", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
            }
        }
        OutlinedButton(
            onClick = if (uiState.hasNextLessonBatch) onRestart else onExit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(if (uiState.hasNextLessonBatch) "再练本轮" else exitLabel, fontWeight = FontWeight.Black)
        }
        if (uiState.hasNextLessonBatch) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(exitLabel, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun lessonExitLabel(tab: LabTab): String {
    return when (tab) {
        LabTab.Library -> "返回本集入口"
        LabTab.Today -> "返回今日页"
        LabTab.Review -> "返回错题页"
        else -> "返回训练入口"
    }
}
