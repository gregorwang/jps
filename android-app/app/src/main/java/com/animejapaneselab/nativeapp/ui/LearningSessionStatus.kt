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
) {
    val chipText: String get() = "$position/$total"
}

internal fun LabUiState.learningSessionStatus(): LearningSessionStatus? {
    return when (activeSession) {
        TrainingSessionKind.Lesson -> {
            buildLessonSessionStatus(focus, lesson, sessionXp)
        }
        TrainingSessionKind.ReadAir -> {
            buildReadAirSessionStatus(
                focus = focus,
                completed = readAir.answeredScopedCount,
                total = readAir.scopedExercises.size,
                sessionXp = sessionXp,
            )
        }
        null -> null
    }
}

internal fun buildLessonSessionStatus(
    focus: EpisodeFocus,
    lesson: LessonSession,
    sessionXp: Int,
): LearningSessionStatus? {
    val total = lesson.nodes.size
    if (total <= 0) return null
    val completed = maxOf(lesson.index, lesson.answered).coerceIn(0, total)
    return LearningSessionStatus(
        kind = TrainingSessionKind.Lesson,
        title = focus.lessonTitle.ifBlank { "日语学习训练" },
        subtitle = "${focus.workTitle} · ${focus.episodeLabel} · XP $sessionXp",
        completed = completed,
        position = if (lesson.isComplete) total else (lesson.index + 1).coerceIn(1, total),
        total = total,
    )
}

internal fun buildReadAirSessionStatus(
    focus: EpisodeFocus,
    completed: Int,
    total: Int,
    sessionXp: Int,
): LearningSessionStatus? {
    if (total <= 0) return null
    val safeCompleted = completed.coerceIn(0, total)
    return LearningSessionStatus(
        kind = TrainingSessionKind.ReadAir,
        title = "语感专项训练",
        subtitle = "${focus.workTitle} · ${focus.episodeLabel} · XP $sessionXp",
        completed = safeCompleted,
        position = if (safeCompleted >= total) total else (safeCompleted + 1).coerceIn(1, total),
        total = total,
    )
}
