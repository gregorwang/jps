package com.animejapaneselab.nativeapp.ui

import com.animejapaneselab.nativeapp.data.EpisodeFocus
import com.animejapaneselab.nativeapp.domain.LessonSession

data class LearningSessionStatus(
    val kind: TrainingSessionKind,
    val title: String,
    val subtitle: String,
    val completed: Int,
    val position: Int,
    val total: Int,
    val workSlug: String = "",
    val episode: Int = 0,
) {
    val chipText: String get() = "$position/$total"
}

internal fun LabUiState.learningSessionStatus(): LearningSessionStatus? {
    return when (activeSession) {
        TrainingSessionKind.Lesson -> {
            buildLessonSessionStatus(focus, lesson)
        }
        TrainingSessionKind.ReadAir -> {
            buildReadAirSessionStatus(
                focus = focus,
                completed = readAir.answeredScopedCount,
                total = readAir.scopedExercises.size,
            )
        }
        null -> null
    }
}

internal fun buildLessonSessionStatus(
    focus: EpisodeFocus,
    lesson: LessonSession,
): LearningSessionStatus? {
    val total = lesson.nodes.size
    if (total <= 0) return null
    val completed = maxOf(lesson.index, lesson.answered).coerceIn(0, total)
    return LearningSessionStatus(
        kind = TrainingSessionKind.Lesson,
        title = focus.lessonTitle.ifBlank { "日语学习训练" },
        subtitle = "${focus.workTitle} · ${focus.episodeLabel}",
        completed = completed,
        position = if (lesson.isComplete) total else (lesson.index + 1).coerceIn(1, total),
        total = total,
        workSlug = focus.workSlug,
        episode = focus.episodeNumber,
    )
}

internal fun buildReadAirSessionStatus(
    focus: EpisodeFocus,
    completed: Int,
    total: Int,
): LearningSessionStatus? {
    if (total <= 0) return null
    val safeCompleted = completed.coerceIn(0, total)
    return LearningSessionStatus(
        kind = TrainingSessionKind.ReadAir,
        title = "读空气",
        subtitle = "${focus.workTitle} · ${focus.episodeLabel}",
        completed = safeCompleted,
        position = if (safeCompleted >= total) total else (safeCompleted + 1).coerceIn(1, total),
        total = total,
        workSlug = focus.workSlug,
        episode = focus.episodeNumber,
    )
}
