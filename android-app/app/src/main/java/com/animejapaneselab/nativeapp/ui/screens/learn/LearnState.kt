package com.animejapaneselab.nativeapp.ui.screens.learn

import androidx.annotation.DrawableRes
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.data.LessonExerciseKind
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.AttendanceCell
import com.animejapaneselab.nativeapp.ui.design.AttendanceState
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.WorkSeason
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug

// Adapts LabUiState (read-only) to the pure course models in CourseModels.kt.

internal data class CourseScreenModel(
    val workSlug: String,
    val workName: String,
    val season: WorkSeason,
    val heroMeta: String,
    @DrawableRes val heroArt: Int?,
    val selectedEpisode: Int,
    val lastEpisode: Int,
    val materials: EpisodeMaterials,
    val course: EpisodeCourse,
    val rows: List<EpisodeRow>,
    val attendance: List<AttendanceCell>,
    val attendanceMeta: String,
    val switcher: List<SwitcherRow>,
    val drills: List<DrillRow>,
)

/** 題型別 practice row (the always-open exercise lab). */
internal data class DrillRow(val kind: LessonExerciseKind?, val label: String, val covered: Int, val total: Int)

internal fun ProgressItem.toRecord() = ProgressRecord(itemType, itemId, state, payload, lastReviewedAt)

internal fun LabUiState.episodeCountFor(workSlug: String): Int {
    val norm = normalizeWorkSlug(workSlug)
    val work = works.firstOrNull { normalizeWorkSlug(it.slug) == norm }
    val current = normalizeWorkSlug(selection.workSlug) == norm
    return listOfNotNull(
        work?.episodeCount,
        if (current) episodes.filter { normalizeWorkSlug(it.workSlug) == norm }.maxOfOrNull { it.episode } else null,
        if (current) selection.episode else null,
    ).maxOrNull()?.coerceAtLeast(1) ?: 1
}

/** Per-episode summaries for a work, from its progress rows. */
internal fun LabUiState.episodeSummaries(workSlug: String): Map<Int, EpisodeSummary> {
    val norm = normalizeWorkSlug(workSlug)
    return progressItems
        .filter { normalizeWorkSlug(it.workSlug) == norm && it.episode > 0 }
        .groupBy { it.episode }
        .mapValues { (_, items) -> CourseModel.episodeSummary(items.map { it.toRecord() }) }
}

internal fun LabUiState.episodeMaterials(): EpisodeMaterials {
    val norm = normalizeWorkSlug(selection.workSlug)
    val ep = selection.episode
    val plan = episodePlan?.takeIf { normalizeWorkSlug(it.workSlug) == norm && it.episode == ep }
    return EpisodeMaterials(
        vocabIds = vocab.map { it.id },
        grammarIds = grammar.map { it.id },
        shadowingIds = shadowing.map { it.id },
        exerciseIds = exercises.map { it.id },
        readAirIds = readAir.exercises
            .filter { normalizeWorkSlug(it.workSlug) == norm && it.episode == ep }
            .map { it.id },
        planVocabIds = plan?.vocabItemIds.orEmpty(),
        planVocabCount = plan?.vocabCount ?: 0,
        planGrammarIds = plan?.grammarPointIds.orEmpty(),
        planGrammarCount = plan?.grammarCount ?: 0,
        planShadowingIds = plan?.shadowingSentenceIds.orEmpty(),
        planShadowingCount = plan?.shadowingCount ?: 0,
        planExerciseIds = plan?.exerciseIds.orEmpty(),
        planExerciseCount = plan?.exerciseCount ?: 0,
        mixedFallbackCount = lesson.nodes.size,
        reviewDue = reviewTasks.count { normalizeWorkSlug(it.workSlug) == norm && it.episode == ep },
        mistakes = mistakes.count { normalizeWorkSlug(it.workSlug) == norm && it.episode == ep },
        grammarPatterns = grammar.map { it.pattern },
    )
}

@DrawableRes
internal fun heroArtFor(workSlug: String, episode: Int): Int? = when (normalizeWorkSlug(workSlug)) {
    "k-on" -> R.drawable.course_kon
    "re-zero" -> when (episode) {
        in 1..25 -> R.drawable.course_rezero
        in 26..50 -> R.drawable.course_rezero_s2
        else -> R.drawable.course_rezero_s3
    }
    else -> null
}

internal fun LabUiState.courseScreenModel(): CourseScreenModel {
    val slug = selection.workSlug
    val norm = normalizeWorkSlug(slug)
    val count = episodeCountFor(slug)
    val episode = selection.episode.coerceIn(1, count)
    val seasons = WorkIdentity.seasons(slug, count)
    val season = seasons.firstOrNull { episode in it } ?: seasons.first()
    val materials = episodeMaterials()
    val records = progressItems
        .filter { normalizeWorkSlug(it.workSlug) == norm && it.episode == episode }
        .map { it.toRecord() }
    val course = CourseModel.buildEpisodeCourse(materials, records)
    val summaries = episodeSummaries(slug) + (episode to EpisodeSummary(course.progress, course.done))
    val range = season.firstEpisode..season.lastEpisode
    val attendance = CourseModel.attendanceCells(range, episode, summaries)
    val doneInSeason = attendance.count { it.state == AttendanceState.Done }
    val workName = if (seasons.size > 1) season.title else WorkIdentity.displayName(slug, focus.workTitle)
    return CourseScreenModel(
        workSlug = slug,
        workName = workName,
        season = season,
        heroMeta = CourseModel.heroMeta(season, episode),
        heroArt = heroArtFor(slug, episode),
        selectedEpisode = episode,
        lastEpisode = count,
        materials = materials,
        course = course,
        rows = CourseModel.episodeRows(slug, range, episode, course, summaries),
        attendance = attendance,
        attendanceMeta = "$workName · $doneInSeason 済 · 1 いま",
        switcher = CourseModel.switcherRows(
            works = works.map { it.slug to episodeCountFor(it.slug) },
            selectedWork = slug,
            selectedEpisode = episode,
            doneEpisodes = { work ->
                val base = episodeSummaries(work).filterValues { it.done }.keys
                if (normalizeWorkSlug(work) == norm && course.done) base + episode else base
            },
        ),
        drills = drillRows(records, materials),
    )
}

private fun drillRows(records: List<ProgressRecord>, materials: EpisodeMaterials): List<DrillRow> {
    val done = CourseModel.completedRefs(records)
    fun covered(type: String, ids: List<String>) = ids.distinct().count { MaterialRef(type, it) in done }
    val vocab = materials.vocabIds.distinct()
    val grammar = materials.grammarIds.distinct()
    val sentences = materials.shadowingIds.distinct()
    val v = covered("vocab", vocab)
    val g = covered("grammar", grammar)
    val s = covered("sentence", sentences)
    return LessonExerciseKind.entries.map { kind ->
        when (kind) {
            LessonExerciseKind.PairMatch, LessonExerciseKind.SingleChoice -> DrillRow(kind, kind.label, v, vocab.size)
            LessonExerciseKind.Cloze -> DrillRow(kind, kind.label, g, grammar.size)
            LessonExerciseKind.TranslationOrder, LessonExerciseKind.AudioOrder, LessonExerciseKind.Shadowing ->
                DrillRow(kind, kind.label, s, sentences.size)
        }
    } + DrillRow(null, "六类混合", v + g + s, vocab.size + grammar.size + sentences.size)
}
