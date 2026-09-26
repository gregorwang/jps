package com.animejapaneselab.nativeapp.ui.screens.learn

import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.ui.design.AttendanceCell
import com.animejapaneselab.nativeapp.ui.design.AttendanceState
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.WorkSeason
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug

// ---------------------------------------------------------------------------
// 課程 as anime grammar: a work → 第X話 → 場面 01–06. Pure (no Compose) so it is JVM-testable.
// Batch keys ("vocab-1", "mixed-2", "read-air-1" …) are the same path-node keys the ViewModel
// already records on completion, so v2 progress carries straight over.
// ---------------------------------------------------------------------------

/** The six scenes of one episode, in scene-number order. */
enum class SceneKind(
    val keyPrefix: String,
    val title: String,
    val unit: String,
    val batchSize: Int,
    val lessonMode: LessonMode?,
) {
    Vocab("vocab", "词汇", "个", 20, LessonMode.Vocab),
    Grammar("grammar", "语法", "条", 6, LessonMode.Grammar),
    Listening("mixed", "听音拼句", "组", 10, LessonMode.Mixed),
    Shadowing("shadowing", "跟读", "句", 6, LessonMode.Shadowing),
    ReadAir("read-air", "读空气", "问", 7, null),
    Review("review", "本話复习", "项", 0, null),
    ;

    val isLesson: Boolean get() = lessonMode != null

    companion object {
        /** Scenes that make up "learning the episode" (01–05); 06 本話复习 is the tail. */
        val Main: List<SceneKind> = listOf(Vocab, Grammar, Listening, Shadowing, ReadAir)
        /** The four lesson scenes used to judge episodes whose materials are not loaded. */
        val LessonKinds: List<SceneKind> = listOf(Vocab, Grammar, Listening, Shadowing)
    }
}

data class MaterialRef(val itemType: String, val itemId: String)

/** A progress row reduced to what the course needs (mirrors data.ProgressItem). */
data class ProgressRecord(
    val itemType: String,
    val itemId: String,
    val state: ReviewState,
    val payload: Map<String, String> = emptyMap(),
    val lastReviewedAt: String = "",
) {
    val isDone: Boolean get() = state == ReviewState.Good || state == ReviewState.Known
}

/** What the selected episode contains. Empty id lists fall back to the episode plan counts. */
data class EpisodeMaterials(
    val vocabIds: List<String> = emptyList(),
    val grammarIds: List<String> = emptyList(),
    val shadowingIds: List<String> = emptyList(),
    val exerciseIds: List<String> = emptyList(),
    val readAirIds: List<String> = emptyList(),
    val planVocabIds: List<String> = emptyList(),
    val planVocabCount: Int = 0,
    val planGrammarIds: List<String> = emptyList(),
    val planGrammarCount: Int = 0,
    val planShadowingIds: List<String> = emptyList(),
    val planShadowingCount: Int = 0,
    val planExerciseIds: List<String> = emptyList(),
    val planExerciseCount: Int = 0,
    /** Size of the currently built lesson; v2 used it to size 综合 when no exercises exist. */
    val mixedFallbackCount: Int = 0,
    val reviewDue: Int = 0,
    val mistakes: Int = 0,
    /** Up to two grammar patterns for the scene name (语法 · ～ないと／～って). */
    val grammarPatterns: List<String> = emptyList(),
)

data class SceneBatch(
    val key: String,
    val batch: Int,
    val materials: List<MaterialRef>,
    val done: Boolean,
)

enum class SceneStatus { Done, Current, Todo, Empty }

data class CourseScene(
    val number: Int,
    val kind: SceneKind,
    val status: SceneStatus,
    val total: Int,
    val completed: Int,
    val batches: List<SceneBatch>,
) {
    /** First unfinished batch; a finished scene replays from batch 1. */
    val nextBatch: SceneBatch? get() = batches.firstOrNull { !it.done } ?: batches.firstOrNull()
    val startable: Boolean get() = status != SceneStatus.Empty
}

data class EpisodeCourse(
    val scenes: List<CourseScene>,
    val currentIndex: Int?,
) {
    val current: CourseScene? get() = currentIndex?.let { scenes.getOrNull(it) }

    private val mainScenes: List<CourseScene>
        get() = scenes.filter { it.kind != SceneKind.Review && it.status != SceneStatus.Empty }

    /** Every non-empty scene 01–05 is finished. */
    val done: Boolean get() = mainScenes.isNotEmpty() && mainScenes.all { it.status == SceneStatus.Done }

    val progress: Float
        get() {
            val main = mainScenes
            if (main.isEmpty()) return 0f
            return main.count { it.status == SceneStatus.Done }.toFloat() / main.size
        }
}

/** Summary of an episode that is not the selected one (only progress rows are known). */
data class EpisodeSummary(val progress: Float, val done: Boolean) {
    companion object {
        val Empty = EpisodeSummary(0f, false)
    }
}

enum class EpisodeRowState { Done, Past, Current, Upcoming }

data class EpisodeRow(
    val number: Int,
    val title: String,
    val state: EpisodeRowState,
    val progress: Float,
) {
    val label: String get() = TextRules.episodeLabel(number)
}

/** One row of the course switcher: a season of a work. */
data class SwitcherRow(
    val workSlug: String,
    val season: WorkSeason,
    val seasonCount: Int,
    val enrolled: Boolean,
    val doneCount: Int,
    val meta: String,
    /** Episode to jump to when chosen; null = keep the work's remembered episode. */
    val jumpTo: Int?,
)

object CourseModel {
    // ---- progress ------------------------------------------------------------------------

    fun completedPathKeys(records: List<ProgressRecord>): Set<String> =
        records.asSequence()
            .filter { it.isDone }
            .mapNotNull { it.payload["pathNodeKey"]?.takeIf(String::isNotBlank) }
            .toSet()

    /** Materials whose latest state is Good/Known. */
    fun completedRefs(records: List<ProgressRecord>): Set<MaterialRef> {
        val latest = linkedMapOf<MaterialRef, ProgressRecord>()
        records.forEach { record ->
            materialRefs(record).forEach { ref ->
                val existing = latest[ref]
                if (existing == null || record.lastReviewedAt > existing.lastReviewedAt) latest[ref] = record
            }
        }
        return latest.filterValues { it.isDone }.keys
    }

    internal fun materialRefs(record: ProgressRecord): Set<MaterialRef> {
        if (record.itemType == "path_node" || !record.payload["pathNodeKey"].isNullOrBlank()) return emptySet()
        val raw = listOf(record.payload["sourceId"], record.payload["source_id"], record.payload["source"])
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            .orEmpty()
            .ifBlank { record.itemId.trim() }
        return buildSet {
            raw.split(',').map(String::trim).filter(String::isNotBlank).forEach { id ->
                add(MaterialRef(record.itemType, id.removeNodeSuffix()))
            }
            // Read-air progress is keyed by exercise id while sourceId points at the subtitle.
            if (record.itemType == "exercise" && record.itemId.isNotBlank()) add(MaterialRef("exercise", record.itemId))
        }
    }

    private fun String.removeNodeSuffix(): String = removeSuffix("-meaning-to-ja")
        .removeSuffix("-ja-to-meaning")
        .removeSuffix("-audio-tiles")
        .removeSuffix("-translation-tiles")
        .removeSuffix("-shadowing-self-check")
        .removeSuffix("-function-choice")
        .removeSuffix("-cloze")
        .removeSuffix("-study")

    // ---- the selected episode -------------------------------------------------------------

    fun buildEpisodeCourse(materials: EpisodeMaterials, records: List<ProgressRecord>): EpisodeCourse {
        val doneKeys = completedPathKeys(records)
        val doneRefs = completedRefs(records)
        val vocab = completeIds(materials.vocabIds, materials.planVocabIds, materials.planVocabCount, "vocab")
        val grammar = completeIds(materials.grammarIds, materials.planGrammarIds, materials.planGrammarCount, "grammar")
        val sentences = completeIds(materials.shadowingIds, materials.planShadowingIds, materials.planShadowingCount, "sentence")
        val exercises = completeIds(materials.exerciseIds, materials.planExerciseIds, materials.planExerciseCount, "ordinary-exercise")
        val readAir = materials.readAirIds.filter(String::isNotBlank).distinct()

        fun batches(kind: SceneKind, ids: List<String>, itemType: String): List<SceneBatch> =
            ids.chunked(kind.batchSize).mapIndexed { index, chunk ->
                val refs = chunk.map { MaterialRef(itemType, it) }
                batch(kind, index + 1, refs, doneKeys, doneRefs)
            }

        val mixedUnits = unitsFor(maxOf(exercises.size, materials.mixedFallbackCount.coerceAtLeast(0)), SceneKind.Listening.batchSize)
        val mixedBatches = (1..mixedUnits).map { b ->
            val refs = buildList {
                addAll(vocab.window(b, 2).map { MaterialRef("vocab", it) })
                addAll(grammar.window(b, 1).map { MaterialRef("grammar", it) })
                addAll(sentences.window(b, 1).map { MaterialRef("sentence", it) })
                if (isEmpty()) addAll(exercises.window(b, SceneKind.Listening.batchSize).map { MaterialRef("exercise", it) })
            }
            batch(SceneKind.Listening, b, refs, doneKeys, doneRefs)
        }.filter { it.materials.isNotEmpty() }

        val byKind = linkedMapOf(
            SceneKind.Vocab to batches(SceneKind.Vocab, vocab, "vocab"),
            SceneKind.Grammar to batches(SceneKind.Grammar, grammar, "grammar"),
            SceneKind.Listening to mixedBatches,
            SceneKind.Shadowing to batches(SceneKind.Shadowing, sentences, "sentence"),
            SceneKind.ReadAir to batches(SceneKind.ReadAir, readAir, "exercise"),
        )
        val mainScenes = byKind.entries.mapIndexed { index, (kind, list) ->
            val total = if (kind == SceneKind.Listening) list.size else list.sumOf { it.materials.size }
            val completed = if (kind == SceneKind.Listening) {
                list.count { it.done }
            } else {
                list.sumOf { b -> if (b.done) b.materials.size else b.materials.count(doneRefs::contains) }
            }
            CourseScene(
                number = index + 1,
                kind = kind,
                status = when {
                    list.isEmpty() -> SceneStatus.Empty
                    list.all { it.done } -> SceneStatus.Done
                    else -> SceneStatus.Todo
                },
                total = total,
                completed = completed.coerceAtMost(total),
                batches = list,
            )
        }
        val reviewTotal = materials.reviewDue.coerceAtLeast(0) + materials.mistakes.coerceAtLeast(0)
        val mainDone = mainScenes.any { it.status != SceneStatus.Empty } &&
            mainScenes.all { it.status == SceneStatus.Done || it.status == SceneStatus.Empty }
        val review = CourseScene(
            number = 6,
            kind = SceneKind.Review,
            status = when {
                reviewTotal > 0 -> SceneStatus.Todo
                mainDone -> SceneStatus.Done
                else -> SceneStatus.Empty
            },
            total = reviewTotal,
            completed = 0,
            batches = emptyList(),
        )
        val scenes = mainScenes + review
        val currentIndex = scenes.indexOfFirst { it.kind != SceneKind.Review && it.status == SceneStatus.Todo }
            .takeIf { it >= 0 }
            ?: scenes.indexOfFirst { it.kind == SceneKind.Review && it.status == SceneStatus.Todo }.takeIf { it >= 0 }
        return EpisodeCourse(
            scenes = scenes.mapIndexed { i, s -> if (i == currentIndex) s.copy(status = SceneStatus.Current) else s },
            currentIndex = currentIndex,
        )
    }

    private fun batch(
        kind: SceneKind,
        number: Int,
        refs: List<MaterialRef>,
        doneKeys: Set<String>,
        doneRefs: Set<MaterialRef>,
    ): SceneBatch {
        val key = "${kind.keyPrefix}-$number"
        val done = key in doneKeys || (refs.isNotEmpty() && refs.all(doneRefs::contains))
        return SceneBatch(key, number, refs, done)
    }

    /** Scene name (left) — 词汇 · 12 个, 语法 · ～ないと／～って, 本話复习. */
    fun sceneName(scene: CourseScene, materials: EpisodeMaterials): String = when (scene.kind) {
        SceneKind.Vocab -> "词汇 · ${scene.total} 个"
        SceneKind.Grammar -> materials.grammarPatterns.filter(String::isNotBlank).take(2)
            .takeIf { it.isNotEmpty() }
            ?.let { "语法 · ${it.joinToString("／")}" }
            ?: "语法 · ${scene.total} 条"
        SceneKind.Listening -> "听音拼句 · 综合"
        SceneKind.Shadowing -> "跟读 · ${scene.total} 句"
        SceneKind.ReadAir -> "读空气 · 没说出口的话"
        SceneKind.Review -> "本話复习"
    }

    /** Scene meta (right, mono) — 済 / 7/12 / 5 句 / 错题 2 · 到期 3 / —. */
    fun sceneMeta(scene: CourseScene, materials: EpisodeMaterials): String = when {
        scene.status == SceneStatus.Empty -> "—"
        scene.status == SceneStatus.Done -> "済"
        scene.kind == SceneKind.Review -> listOfNotNull(
            materials.mistakes.takeIf { it > 0 }?.let { "错题 $it" },
            materials.reviewDue.takeIf { it > 0 }?.let { "到期 $it" },
        ).joinToString(" · ")
        scene.status == SceneStatus.Current || scene.completed > 0 -> "${scene.completed}/${scene.total}"
        else -> "${scene.total} ${scene.kind.unit}"
    }

    // ---- other episodes -------------------------------------------------------------------

    /**
     * An unselected episode only has progress rows. Each lesson scene (词汇/语法/听音拼句/跟读) with
     * at least one finished batch counts a quarter; all four = 済. Material-only progress shows a
     * sliver so a touched episode never looks untouched.
     */
    fun episodeSummary(records: List<ProgressRecord>): EpisodeSummary {
        if (records.isEmpty()) return EpisodeSummary.Empty
        val keys = completedPathKeys(records)
        val kinds = SceneKind.LessonKinds.count { kind -> keys.any { it.startsWith("${kind.keyPrefix}-") } }
        val touched = records.any { it.isDone }
        val progress = when {
            kinds > 0 -> kinds.toFloat() / SceneKind.LessonKinds.size
            touched -> 0.1f
            else -> 0f
        }
        return EpisodeSummary(progress, kinds == SceneKind.LessonKinds.size)
    }

    /**
     * The 話 list: [before] episodes above the selected one, the selected one, [after] below —
     * always inside [range] (the season). The switcher's 出席カード reaches every other episode.
     */
    fun episodeRows(
        workSlug: String,
        range: IntRange,
        selected: Int,
        selectedCourse: EpisodeCourse,
        summaries: Map<Int, EpisodeSummary>,
        before: Int = 2,
        after: Int = 2,
    ): List<EpisodeRow> {
        if (range.isEmpty()) return emptyList()
        val sel = selected.coerceIn(range.first, range.last)
        val start = (sel - before).coerceAtLeast(range.first)
        val end = (sel + after).coerceAtMost(range.last)
        return (start..end).map { n ->
            val summary = if (n == sel) {
                EpisodeSummary(selectedCourse.progress, selectedCourse.done)
            } else {
                summaries[n] ?: EpisodeSummary.Empty
            }
            val state = when {
                n == sel -> EpisodeRowState.Current
                summary.done -> EpisodeRowState.Done
                n < sel -> EpisodeRowState.Past
                else -> EpisodeRowState.Upcoming
            }
            EpisodeRow(n, episodeTitle(workSlug, n).orEmpty(), state, summary.progress)
        }
    }

    /** 出席カード cells: the selected episode keeps its いま strip even when finished. */
    fun attendanceCells(
        range: IntRange,
        selected: Int,
        summaries: Map<Int, EpisodeSummary>,
    ): List<AttendanceCell> = range.map { n ->
        val state = when {
            n == selected -> AttendanceState.Now
            summaries[n]?.done == true -> AttendanceState.Done
            else -> AttendanceState.Todo
        }
        AttendanceCell(n, state)
    }

    // ---- switcher -------------------------------------------------------------------------

    /**
     * Every season of every work. Multi-season works (Re:ゼロ) jump to the season's first episode;
     * single-season works keep their remembered episode.
     */
    fun switcherRows(
        works: List<Pair<String, Int>>,
        selectedWork: String,
        selectedEpisode: Int,
        doneEpisodes: (workSlug: String) -> Set<Int>,
    ): List<SwitcherRow> = works.flatMap { (slug, count) ->
        val seasons = WorkIdentity.seasons(slug, count)
        val done = doneEpisodes(slug)
        val sameWork = normalizeWorkSlug(slug) == normalizeWorkSlug(selectedWork)
        seasons.map { season ->
            val enrolled = sameWork && selectedEpisode in season
            val doneCount = done.count { it in season }
            val head = "全${TextRules.kanjiNumber(season.episodeCount)}話"
            val tail = when {
                enrolled -> "いま${TextRules.episodeLabel(selectedEpisode)}"
                doneCount > 0 -> "$doneCount 済"
                else -> "未入学"
            }
            SwitcherRow(
                workSlug = slug,
                season = season,
                seasonCount = seasons.size,
                enrolled = enrolled,
                doneCount = doneCount,
                meta = "$head · $tail",
                jumpTo = when {
                    enrolled -> null
                    seasons.size > 1 -> season.firstEpisode
                    else -> null
                },
            )
        }
    }

    /** Hero meta: 全十三話 · いま第三話. */
    fun heroMeta(season: WorkSeason, selectedEpisode: Int): String =
        "全${TextRules.kanjiNumber(season.episodeCount)}話 · いま${TextRules.episodeLabel(selectedEpisode)}"

    /** Episode title when known (no ViewModel field yet — see design/v3-requests/C.md). */
    fun episodeTitle(workSlug: String, episode: Int): String? = when (normalizeWorkSlug(workSlug)) {
        "k-on" -> KOnTitles[episode]
        "re-zero" -> ReZeroTitles[episode]
        else -> null
    }

    private val KOnTitles = mapOf(
        1 to "廃部！", 2 to "楽器！", 3 to "特訓！", 4 to "合宿！", 5 to "顧問！", 6 to "学園祭！",
        7 to "クリスマス！", 8 to "新歓！", 9 to "新入部員！", 10 to "また合宿！", 11 to "ピンチ！",
        12 to "軽音！", 13 to "冬の日！", 14 to "ライブハウス！",
    )

    private val ReZeroTitles = mapOf(
        1 to "始まりの終わりと終わりの始まり", 2 to "再会の魔女", 3 to "ゼロから始まる異世界生活",
        4 to "ロズワール邸の団欒", 5 to "約束した朝は遠く", 6 to "鎖の音", 7 to "ナツキ・スバルのリスタート",
        8 to "泣いて泣き喚いて泣き止んだから", 9 to "勇気の意味", 10 to "鬼がかったやり方", 11 to "レム",
        12 to "再来の王都", 13 to "自称騎士ナツキ・スバル", 14 to "絶望という病", 15 to "狂気の外側",
        16 to "豚の欲望", 17 to "醜態の果てに", 18 to "ゼロから", 19 to "白鯨攻略戦",
        20 to "ヴィルヘルム・ヴァン・アストレア", 21 to "絶望に抗う賭け", 22 to "怠惰一閃", 23 to "悪辣なる怠惰",
        24 to "自称騎士と最優の騎士", 25 to "ただそれだけの物語", 26 to "それぞれの誓い",
    )

    // ---- helpers --------------------------------------------------------------------------

    /** Loaded ids in plan order; with nothing loaded, synthetic ids sized by the plan. */
    internal fun completeIds(actual: List<String>, plan: List<String>, planCount: Int, prefix: String): List<String> {
        val loaded = actual.filter(String::isNotBlank).distinct()
        if (loaded.isNotEmpty()) {
            val loadedSet = loaded.toSet()
            val preferred = plan.filter(String::isNotBlank).distinct().filter(loadedSet::contains)
            return preferred + loaded.filterNot(preferred.toSet()::contains)
        }
        val planned = plan.filter(String::isNotBlank).distinct()
        val total = maxOf(planned.size, planCount.coerceAtLeast(0))
        return planned + (planned.size until total).map { "$prefix-${it + 1}" }
    }

    private fun <T> List<T>.window(batch: Int, size: Int): List<T> = drop((batch - 1) * size).take(size)

    private fun unitsFor(total: Int, size: Int): Int = if (total <= 0) 0 else (total + size - 1) / size
}
