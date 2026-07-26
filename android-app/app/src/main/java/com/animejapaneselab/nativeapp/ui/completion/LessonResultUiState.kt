package com.animejapaneselab.nativeapp.ui.completion

import com.animejapaneselab.nativeapp.ui.LabUiState

data class LessonResultUiState(
    val workTitle: String,
    val episodeLabel: String,
    val title: String,
    val completedCount: Int,
    val correctCount: Int,
    val totalCount: Int,
    val xp: Int,
    val streakDays: Int,
    val accuracyPercent: Int,
    val masteredContent: String,
    val primaryMistakeType: String,
    val reviewSchedule: String,
    val nextSuggestion: String,
    val hasNextBatch: Boolean,
)

fun LabUiState.toLessonResultUiState(): LessonResultUiState {
    val answered = lesson.answered
    val accuracy = if (answered == 0) 0 else (lesson.correct * 100 / answered)
    val hasMistakes = lesson.correct < answered
    val activeTypes = lesson.nodes
        .map { it.typeLabel }
        .filter { it.isNotBlank() }
        .distinct()
    val mistakeType = when {
        !hasMistakes -> "本轮没有明显错误"
        activeTypes.any { it.contains("填空") || it.contains("语法") } -> "语法功能误判"
        activeTypes.any { it.contains("听") || it.contains("跟读") } -> "听力/跟读节奏"
        activeTypes.any { it.contains("语言") || it.contains("读空气") } -> "语境判断"
        else -> "词义混淆"
    }
    return LessonResultUiState(
        workTitle = focus.workTitle,
        episodeLabel = focus.episodeLabel,
        title = focus.lessonTitle,
        completedCount = answered,
        correctCount = lesson.correct,
        totalCount = lesson.nodes.size,
        xp = sessionXp,
        streakDays = focus.streakDays.coerceAtLeast(1),
        accuracyPercent = accuracy,
        masteredContent = "${lesson.correct.coerceAtLeast(0)} 个内容进入掌握队列",
        primaryMistakeType = mistakeType,
        reviewSchedule = if (hasMistakes) "错题已安排到复盘队列" else "暂无新增错题，明天继续巩固",
        nextSuggestion = if (hasNextLessonBatch) "继续下一组材料" else "回到今日页，等待下一轮安排",
        hasNextBatch = hasNextLessonBatch,
    )
}
