package com.animejapaneselab.nativeapp.ui

import com.animejapaneselab.nativeapp.data.EpisodeFocus
import com.animejapaneselab.nativeapp.domain.LessonSession
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.drill.DrillMode
import com.animejapaneselab.nativeapp.ui.jishu.JishuSitting

data class LearningSessionStatus(
    val kind: TrainingSessionKind,
    val title: String,
    val subtitle: String,
    val completed: Int,
    val position: Int,
    val total: Int,
    val workSlug: String = "",
    val episode: Int = 0,
    /** What one step is called: 问 for drills, 句 for 自習 cards. */
    val unit: String = "问",
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

/** 自習 sitting in progress: 板書 + scene cards. */
internal fun buildJishuSessionStatus(sitting: JishuSitting?, lessonTitle: String, workSlug: String): LearningSessionStatus? {
    if (sitting == null || sitting.isComplete || sitting.pages.isEmpty()) return null
    val total = sitting.pages.size
    return LearningSessionStatus(
        kind = TrainingSessionKind.Lesson,
        title = listOf("自習", lessonTitle).filter { it.isNotBlank() }.joinToString(" · "),
        subtitle = "",
        completed = sitting.index.coerceIn(0, total),
        position = (sitting.index + 1).coerceIn(1, total),
        total = total,
        workSlug = workSlug,
        unit = "页",
    )
}

/** 活用 set in progress: 小テスト after a lesson, or a 復習 round. */
internal fun buildDrillSessionStatus(drill: ConjugationDrillState, workSlug: String): LearningSessionStatus? {
    val total = drill.session.size
    if (total <= 0 || drill.isComplete) return null
    val point = drill.openLesson ?: drill.pointId
    val title = if (drill.mode == DrillMode.Lesson) {
        listOf("小テスト", point?.let(drill::titleOf).orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
    } else {
        "復習 · 活用"
    }
    return LearningSessionStatus(
        kind = TrainingSessionKind.Lesson,
        title = title,
        subtitle = "",
        completed = drill.answers.size.coerceIn(0, total),
        position = (drill.index + 1).coerceIn(1, total),
        total = total,
        workSlug = workSlug,
    )
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
