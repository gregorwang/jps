package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.TagChip

@Composable
fun ReviewScreen(
    uiState: LabUiState,
    onOpenLesson: () -> Unit,
    onPracticeWeakest: () -> Unit,
    onMistakeReviewed: (String) -> Unit,
    onPracticeMistake: (String) -> Unit,
    onPracticeRemoteTask: (ProgressItem) -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            val hasMistakes = uiState.mistakes.isNotEmpty()
            val hasRemoteReview = uiState.reviewTasks.isNotEmpty()
            ReviewHero(
                mistakeCount = uiState.mistakes.size,
                remoteReviewCount = uiState.reviewTasks.size,
                sessionXp = uiState.sessionXp,
                syncMessage = uiState.sync.message,
                onSync = onSync,
                onPractice = if (hasMistakes || hasRemoteReview) onPracticeWeakest else onOpenLesson,
                practiceLabel = when {
                    hasMistakes -> "练错题"
                    hasRemoteReview -> "练复习"
                    else -> "去训练"
                },
            )
        }
        if (uiState.mistakes.isEmpty() && uiState.reviewTasks.isEmpty()) {
            item {
                EmptyReviewState(onOpenLesson = onOpenLesson)
            }
        }
        if (uiState.reviewTasks.isNotEmpty()) {
            item {
                SectionTitle(eyebrow = "今日复习", title = "同步题也直接回到训练流")
            }
        }
        items(uiState.reviewTasks, key = { it.itemId }) { task ->
            RemoteReviewCard(task = task, onPractice = { onPracticeRemoteTask(task) })
        }
        if (uiState.mistakes.isNotEmpty()) {
            item {
                SectionTitle(eyebrow = "薄弱项", title = "先处理最容易忘的题")
            }
        }
        items(uiState.mistakes, key = { it.itemId }) { mistake ->
            MistakeCard(
                mistake = mistake,
                onPractice = { onPracticeMistake(mistake.itemId) },
                onReviewed = { onMistakeReviewed(mistake.itemId) },
            )
        }
    }
}

@Composable
private fun ReviewHero(
    mistakeCount: Int,
    remoteReviewCount: Int,
    sessionXp: Int,
    syncMessage: String,
    onSync: () -> Unit,
    onPractice: () -> Unit,
    practiceLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (mistakeCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    color = Color.White,
                    contentColor = if (mistakeCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Icon(
                        imageVector = if (mistakeCount > 0) Icons.Rounded.Error else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(13.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("复习", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            mistakeCount > 0 -> "今日先修补弱项"
                            remoteReviewCount > 0 -> "今日复习已排好"
                            else -> "今天状态不错"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HeroMetric(label = "错题", value = mistakeCount.toString(), modifier = Modifier.weight(1f))
                HeroMetric(label = "复习", value = remoteReviewCount.toString(), modifier = Modifier.weight(1f))
                HeroMetric(label = "XP", value = sessionXp.toString(), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onSync,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color.White)
                    Text("同步", modifier = Modifier.padding(start = 6.dp), color = Color.White, fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onPractice,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = if (mistakeCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Whatshot, contentDescription = null)
                        Text(practiceLabel, modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                    }
                }
            }
            if (syncMessage.isNotBlank()) {
                Text(syncMessage, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.16f),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun EmptyReviewState(
    onOpenLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LabCard(modifier = modifier) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("本地错题本为空", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text("继续训练会自动把薄弱项收进这里。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton("开始下一轮", onClick = onOpenLesson, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MistakeCard(
    mistake: MistakeRecord,
    onPractice: () -> Unit,
    onReviewed: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Replay, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                TagChip(mistake.typeLabel, selected = true)
                TagChip("x${mistake.attempts}")
            }
            Text(
                mistake.prompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            AnswerCompareStrip(
                selected = mistake.selected,
                expected = mistake.expected,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onPractice,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text("再练一次", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
                }
                SecondaryButton("我记住了", onClick = onReviewed, modifier = Modifier.weight(1f))
            }
            Text(
                mistake.explanation,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                reviewSourceLabel(mistake.workSlug, mistake.episode, mistake.sourceLabel),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AnswerCompareStrip(
    selected: String,
    expected: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnswerLine(label = "你的答案", value = selected, isCorrect = false)
        AnswerLine(label = "正确答案", value = expected, isCorrect = true)
    }
}

@Composable
private fun AnswerLine(
    label: String,
    value: String,
    isCorrect: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RemoteReviewCard(task: ProgressItem, onPractice: () -> Unit) {
    LabCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip(task.itemType.reviewTypeLabel(), selected = true)
            TagChip(task.state.label)
        }
        Text(reviewTaskLabel(task.label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(
            "${reviewSourceLabel(task.workSlug, task.episode)} · 下次复习 ${task.nextReviewOn.ifBlank { "今天" }}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onPractice,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Text("再练一次", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Black)
        }
    }
}

private fun String.reviewTypeLabel(): String {
    return when (this) {
        "vocab" -> "词汇"
        "grammar" -> "语法"
        "sentence" -> "跟读句"
        "exercise" -> "读空气"
        else -> "训练"
    }
}

private fun reviewSourceLabel(workSlug: String, episode: Int, sourceLabel: String = ""): String {
    val work = when (workSlug) {
        "k-on" -> "K-ON!"
        "re-zero", "rezero" -> "Re:Zero"
        else -> workSlug.ifBlank { "当前作品" }
    }
    val episodeLabel = episode.takeIf { it > 0 }?.let { "EP${it.toString().padStart(2, '0')}" }
    val localizedSource = sourceLabel
        .replace(Regex("""\bline\s+(\d+)""", RegexOption.IGNORE_CASE), "第 $1 行")
        .trim()
    return listOfNotNull(
        work,
        episodeLabel,
        localizedSource.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
}

private fun reviewTaskLabel(label: String): String {
    return label
        .replace(Regex("""\bline\s+(\d+)""", RegexOption.IGNORE_CASE), "第 $1 行")
        .trim()
}
