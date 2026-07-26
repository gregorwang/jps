package com.animejapaneselab.nativeapp.ui.screens

import com.animejapaneselab.nativeapp.data.EpisodePlan
import com.animejapaneselab.nativeapp.data.ReviewState

private const val VocabPathUnitSize = 20
private const val GrammarPathUnitSize = 6
private const val ShadowingPathUnitSize = 6
private const val ReadAirPathUnitSize = 7
private const val MixedPathUnitSize = 10
private const val MixedVocabPerUnit = 2
private const val MixedGrammarPerUnit = 1
private const val MixedSentencePerUnit = 1
private const val RewardEveryActionableNodes = 4

internal data class TrainingPathInput(
    val workTitle: String,
    val episodeLabel: String,
    val lessonTitle: String,
    val energy: Int,
    val streakDays: Int,
    val sessionXp: Int,
    val lessonNodeCount: Int,
    val lessonAnswered: Int,
    val lessonCorrect: Int,
    val vocabCount: Int,
    val grammarCount: Int,
    val shadowingCount: Int,
    val readAirCount: Int,
    val reviewDueCount: Int,
    val localMistakeCount: Int,
    val progressItems: List<TrainingPathProgressItem>,
    val episodePlan: EpisodePlan? = null,
    val accessPolicy: TrainingPathAccessPolicy = TrainingPathAccessPolicy.Sequential,
    val vocabIds: List<String> = emptyList(),
    val grammarIds: List<String> = emptyList(),
    val shadowingIds: List<String> = emptyList(),
    val exerciseIds: List<String> = emptyList(),
    val readAirIds: List<String> = emptyList(),
    val exerciseCount: Int = 0,
    val hasNextEpisode: Boolean = true,
)

internal enum class TrainingPathAccessPolicy {
    OpenDuringDevelopment,
    Sequential,
}

internal data class TrainingPathProgressItem(
    val itemType: String,
    val state: ReviewState,
    val itemId: String = "",
    val payload: Map<String, String> = emptyMap(),
    val lastReviewedAt: String = "",
)

internal data class TrainingPathPlan(
    val title: String,
    val subtitle: String,
    val progress: Float,
    val answeredLabel: String,
    val correctLabel: String,
    val energyLabel: String,
    val streakLabel: String,
    val xpLabel: String,
    val planNote: String,
    val nodes: List<TrainingPathNode>,
    val fullVocabCount: Int,
    val coreVocabCount: Int,
    val completedPathNodeCount: Int,
    val totalPathNodeCount: Int,
)

internal data class TrainingPathNode(
    val key: String,
    val title: String,
    val subtitle: String,
    val countLabel: String,
    val state: TrainingPathNodeState,
    val action: TrainingPathNodeAction,
    val batch: Int = 1,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val scopeLabel: String = "",
    val materialRefs: List<TrainingMaterialRef> = emptyList(),
) {
    val progress: Float
        get() = if (totalCount <= 0) 0f else (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
}

internal data class TrainingMaterialRef(
    val itemType: String,
    val itemId: String,
)

internal enum class TrainingPathNodeState {
    Completed,
    Current,
    Available,
    ReviewDue,
    Locked,
    Reward,
}

internal enum class TrainingPathNodeAction {
    Mixed,
    Vocab,
    Grammar,
    Shadowing,
    ReadAir,
    Review,
    NextEpisode,
    None,
}

internal fun buildTrainingPathPlan(input: TrainingPathInput): TrainingPathPlan {
    val completedRefs = latestMaterialStates(input.progressItems)
        .filterValues { state -> state == ReviewState.Good || state == ReviewState.Known }
        .keys
    val completedPathKeys = input.progressItems
        .filter { item ->
            !item.payload["pathNodeKey"].isNullOrBlank() &&
                (item.state == ReviewState.Good || item.state == ReviewState.Known)
        }
        .mapNotNull { item -> item.payload["pathNodeKey"]?.takeIf(String::isNotBlank) }
        .toSet()
    val weakCount = input.progressItems.count {
        it.state == ReviewState.Bad ||
            it.state == ReviewState.Fuzzy ||
            it.state == ReviewState.Unknown ||
            it.state == ReviewState.Ok
    }
    val reviewCount = input.reviewDueCount + input.localMistakeCount

    val planVocabIds = input.episodePlan?.vocabItemIds.orEmpty().distinct().filter(String::isNotBlank)
    val declaredCoreVocabCount = maxOf(input.episodePlan?.vocabCount ?: 0, planVocabIds.size)
    val vocabTotal = maxOf(input.vocabCount.coerceAtLeast(0), input.vocabIds.distinct().size, declaredCoreVocabCount)
    val vocabIds = completeMaterialIds(
        actualIds = input.vocabIds,
        preferredIds = planVocabIds,
        total = vocabTotal,
        syntheticPrefix = "vocab",
    )
    val coreVocabIds = when {
        planVocabIds.isNotEmpty() && input.vocabIds.isNotEmpty() -> planVocabIds.filter(vocabIds.toSet()::contains)
        planVocabIds.isNotEmpty() -> vocabIds.take(declaredCoreVocabCount)
        declaredCoreVocabCount > 0 -> vocabIds.take(declaredCoreVocabCount)
        else -> emptyList()
    }
    val grammarIds = completeMaterialIds(
        actualIds = input.grammarIds,
        preferredIds = input.episodePlan?.grammarPointIds.orEmpty(),
        total = maxOf(
            input.grammarCount.coerceAtLeast(0),
            input.grammarIds.distinct().size,
            input.episodePlan?.grammarCount ?: 0,
            input.episodePlan?.grammarPointIds.orEmpty().distinct().size,
        ),
        syntheticPrefix = "grammar",
    )
    val shadowingIds = completeMaterialIds(
        actualIds = input.shadowingIds,
        preferredIds = input.episodePlan?.shadowingSentenceIds.orEmpty(),
        total = maxOf(
            input.shadowingCount.coerceAtLeast(0),
            input.shadowingIds.distinct().size,
            input.episodePlan?.shadowingCount ?: 0,
            input.episodePlan?.shadowingSentenceIds.orEmpty().distinct().size,
        ),
        syntheticPrefix = "sentence",
    )
    val exerciseIds = completeMaterialIds(
        actualIds = input.exerciseIds,
        preferredIds = input.episodePlan?.exerciseIds.orEmpty(),
        total = maxOf(
            input.exerciseCount.coerceAtLeast(0),
            input.exerciseIds.distinct().size,
            input.episodePlan?.exerciseCount ?: 0,
            input.episodePlan?.exerciseIds.orEmpty().distinct().size,
        ),
        syntheticPrefix = "ordinary-exercise",
    )
    val readAirIds = completeMaterialIds(
        actualIds = input.readAirIds,
        preferredIds = emptyList(),
        total = maxOf(input.readAirCount.coerceAtLeast(0), input.readAirIds.distinct().size),
        syntheticPrefix = "exercise",
    )

    val nodeSpecs = buildPathNodeSpecs(
        vocabIds = vocabIds,
        coreVocabIds = coreVocabIds.toSet(),
        grammarIds = grammarIds,
        shadowingIds = shadowingIds,
        exerciseIds = exerciseIds,
        readAirIds = readAirIds,
        fallbackMixedCount = input.lessonNodeCount,
    )
    val nodes = materializePathNodes(
        input = input,
        specs = nodeSpecs,
        completedRefs = completedRefs,
        completedPathKeys = completedPathKeys,
        reviewCount = reviewCount,
        weakCount = weakCount,
    )
    val actionableNodes = nodes.filter { node ->
        node.action !in setOf(
            TrainingPathNodeAction.None,
            TrainingPathNodeAction.Review,
            TrainingPathNodeAction.NextEpisode,
        )
    }
    val completedPathNodeCount = actionableNodes.count { it.state == TrainingPathNodeState.Completed }
    val totalPathNodeCount = actionableNodes.size
    val progress = if (totalPathNodeCount == 0) 0f else completedPathNodeCount.toFloat() / totalPathNodeCount.toFloat()
    val coreCount = coreVocabIds.size.coerceAtMost(vocabIds.size)
    val extensionCount = (vocabIds.size - coreCount).coerceAtLeast(0)
    val planNote = buildString {
        append("完整词库 ${vocabIds.size}")
        if (coreCount > 0) append(" · 核心 $coreCount 优先")
        if (extensionCount > 0) append(" · 扩展 $extensionCount")
        append(" · 语法 ${grammarIds.size} · 跟读 ${shadowingIds.size}")
    }

    return TrainingPathPlan(
        title = if (input.episodeLabel.contains(input.workTitle)) input.episodeLabel else "${input.workTitle} ${input.episodeLabel}",
        subtitle = input.lessonTitle.ifBlank { "今天从这里开始" },
        progress = progress.coerceIn(0f, 1f),
        answeredLabel = "$completedPathNodeCount / $totalPathNodeCount",
        correctLabel = input.lessonCorrect.coerceAtLeast(0).toString(),
        energyLabel = input.energy.coerceAtLeast(0).toString(),
        streakLabel = "${input.streakDays.coerceAtLeast(0)} 天",
        xpLabel = "+${input.sessionXp.coerceAtLeast(0)}",
        planNote = planNote,
        nodes = nodes,
        fullVocabCount = vocabIds.size,
        coreVocabCount = coreCount,
        completedPathNodeCount = completedPathNodeCount,
        totalPathNodeCount = totalPathNodeCount,
    )
}

private data class PathNodeSpec(
    val key: String,
    val title: String,
    val subtitle: String,
    val scopeLabel: String,
    val action: TrainingPathNodeAction,
    val batch: Int,
    val materials: List<TrainingMaterialRef>,
)

private fun buildPathNodeSpecs(
    vocabIds: List<String>,
    coreVocabIds: Set<String>,
    grammarIds: List<String>,
    shadowingIds: List<String>,
    exerciseIds: List<String>,
    readAirIds: List<String>,
    fallbackMixedCount: Int,
): List<PathNodeSpec> {
    val mixedTotal = maxOf(exerciseIds.size, fallbackMixedCount.coerceAtLeast(0))
    val mixedUnitCount = unitsFor(mixedTotal, MixedPathUnitSize)
    val mixedSpecs = (1..mixedUnitCount).map { batch ->
        val refs = buildList {
            addAll(vocabIds.window(batch, MixedVocabPerUnit).map { TrainingMaterialRef("vocab", it) })
            addAll(grammarIds.window(batch, MixedGrammarPerUnit).map { TrainingMaterialRef("grammar", it) })
            addAll(shadowingIds.window(batch, MixedSentencePerUnit).map { TrainingMaterialRef("sentence", it) })
            if (isEmpty()) {
                addAll(exerciseIds.window(batch, MixedPathUnitSize).map { TrainingMaterialRef("exercise", it) })
            }
        }
        PathNodeSpec(
            key = "mixed-$batch",
            title = "综合练习 $batch",
            subtitle = "串联本组词汇、语法和原声材料",
            scopeLabel = "第 $batch 组",
            action = TrainingPathNodeAction.Mixed,
            batch = batch,
            materials = refs,
        )
    }
    val vocabSpecs = vocabIds.chunked(VocabPathUnitSize).mapIndexed { index, ids ->
        val batch = index + 1
        val positions = rangeLabel(batch, vocabIds.size, VocabPathUnitSize)
        val coreCount = ids.count(coreVocabIds::contains)
        val tier = when {
            coreCount == ids.size -> "核心"
            coreCount == 0 -> "扩展"
            else -> "核心+扩展"
        }
        PathNodeSpec(
            key = "vocab-$batch",
            title = "词汇练习 $batch",
            subtitle = "覆盖完整词库第 $positions 个词",
            scopeLabel = "$tier $positions",
            action = TrainingPathNodeAction.Vocab,
            batch = batch,
            materials = ids.map { TrainingMaterialRef("vocab", it) },
        )
    }
    val grammarSpecs = unitSpecs(grammarIds, GrammarPathUnitSize, "grammar", "语法练习", "语法", TrainingPathNodeAction.Grammar)
    val shadowingSpecs = unitSpecs(shadowingIds, ShadowingPathUnitSize, "sentence", "跟读训练", "原声", TrainingPathNodeAction.Shadowing)
    val readAirSpecs = unitSpecs(readAirIds, ReadAirPathUnitSize, "exercise", "读空气", "语感", TrainingPathNodeAction.ReadAir)

    val groups = listOf(mixedSpecs, vocabSpecs, grammarSpecs, shadowingSpecs, readAirSpecs)
    val maxUnits = groups.maxOfOrNull(List<PathNodeSpec>::size) ?: 0
    return buildList {
        repeat(maxUnits) { index ->
            groups.forEach { specs -> specs.getOrNull(index)?.let(::add) }
        }
    }
}

private fun unitSpecs(
    ids: List<String>,
    unitSize: Int,
    itemType: String,
    titlePrefix: String,
    scopePrefix: String,
    action: TrainingPathNodeAction,
): List<PathNodeSpec> {
    val keyPrefix = when (action) {
        TrainingPathNodeAction.ReadAir -> "read-air"
        else -> action.name.lowercase()
    }
    return ids.chunked(unitSize).mapIndexed { index, unitIds ->
        val batch = index + 1
        val positions = rangeLabel(batch, ids.size, unitSize)
        PathNodeSpec(
            key = "$keyPrefix-$batch",
            title = "$titlePrefix $batch",
            subtitle = "覆盖第 $positions 个${scopePrefix}学习点",
            scopeLabel = "$scopePrefix $positions",
            action = action,
            batch = batch,
            materials = unitIds.map { TrainingMaterialRef(itemType, it) },
        )
    }
}

private fun materializePathNodes(
    input: TrainingPathInput,
    specs: List<PathNodeSpec>,
    completedRefs: Set<TrainingMaterialRef>,
    completedPathKeys: Set<String>,
    reviewCount: Int,
    weakCount: Int,
): List<TrainingPathNode> {
    val nodes = mutableListOf<TrainingPathNode>()
    val recentActionStates = mutableListOf<TrainingPathNodeState>()
    var actionableIndex = 0
    var rewardIndex = 1
    var currentAssigned = false

    if (reviewCount > 0 || weakCount > 0) {
        nodes += TrainingPathNode(
            key = "review-due",
            title = "快速复盘",
            subtitle = "先处理到期复习和旧错题",
            countLabel = "${(reviewCount + weakCount).coerceAtLeast(1)} 项",
            state = TrainingPathNodeState.ReviewDue,
            action = TrainingPathNodeAction.Review,
        )
        currentAssigned = true
    }

    specs.forEach { spec ->
        val completedCount = spec.materials.count(completedRefs::contains)
        val complete = spec.key in completedPathKeys || (spec.materials.isNotEmpty() && completedCount >= spec.materials.size)
        val state = when {
            spec.materials.isEmpty() -> TrainingPathNodeState.Locked
            complete -> TrainingPathNodeState.Completed
            !currentAssigned -> {
                currentAssigned = true
                TrainingPathNodeState.Current
            }
            input.accessPolicy == TrainingPathAccessPolicy.OpenDuringDevelopment -> TrainingPathNodeState.Available
            else -> TrainingPathNodeState.Locked
        }
        nodes += TrainingPathNode(
            key = spec.key,
            title = spec.title,
            subtitle = spec.subtitle,
            countLabel = "$completedCount/${spec.materials.size}",
            state = state,
            action = spec.action,
            batch = spec.batch,
            completedCount = completedCount,
            totalCount = spec.materials.size,
            scopeLabel = spec.scopeLabel,
            materialRefs = spec.materials,
        )
        actionableIndex += 1
        recentActionStates += state
        if (actionableIndex % RewardEveryActionableNodes == 0) {
            val segmentComplete = recentActionStates.takeLast(RewardEveryActionableNodes)
                .all { it == TrainingPathNodeState.Completed }
            nodes += rewardNode(rewardIndex++, segmentComplete, input.lessonCorrect)
        }
    }

    if (nodes.none { it.state == TrainingPathNodeState.Reward }) {
        nodes += rewardNode(rewardIndex, unlocked = specs.all { it.key in completedPathKeys }, lessonCorrect = input.lessonCorrect)
    }
    val allPathComplete = specs.isNotEmpty() && specs.all { spec ->
        spec.key in completedPathKeys || (spec.materials.isNotEmpty() && spec.materials.all(completedRefs::contains))
    }
    nodes += TrainingPathNode(
        key = "next-episode",
        title = if (input.hasNextEpisode) "继续下一集" else "当前已是最新集",
        subtitle = if (allPathComplete) "本集路径已完成" else "完成本集主路径后解锁",
        countLabel = if (allPathComplete) "已解锁" else "未解锁",
        state = if (allPathComplete && input.hasNextEpisode) TrainingPathNodeState.Available else TrainingPathNodeState.Locked,
        action = if (input.hasNextEpisode) TrainingPathNodeAction.NextEpisode else TrainingPathNodeAction.None,
    )
    return nodes
}

private fun rewardNode(index: Int, unlocked: Boolean, lessonCorrect: Int): TrainingPathNode {
    return TrainingPathNode(
        key = "reward-$index",
        title = "奖励宝箱 $index",
        subtitle = if (unlocked) "前四个节点已完成" else "完成前四个节点后开启",
        countLabel = "+${(lessonCorrect * 5).coerceAtLeast(20) + index * 5} XP",
        state = TrainingPathNodeState.Reward,
        action = TrainingPathNodeAction.None,
    )
}

private fun latestMaterialStates(items: List<TrainingPathProgressItem>): Map<TrainingMaterialRef, ReviewState> {
    val latest = linkedMapOf<TrainingMaterialRef, TrainingPathProgressItem>()
    items.forEach { item ->
        item.materialRefs().forEach { ref ->
            val existing = latest[ref]
            if (existing == null || item.lastReviewedAt > existing.lastReviewedAt) {
                latest[ref] = item
            }
        }
    }
    return latest.mapValues { (_, item) -> item.state }
}

private fun TrainingPathProgressItem.materialRefs(): Set<TrainingMaterialRef> {
    if (itemType == "path_node" || !payload["pathNodeKey"].isNullOrBlank()) return emptySet()
    val raw = listOf(payload["sourceId"], payload["source_id"], payload["source"])
        .firstOrNull { !it.isNullOrBlank() }
        ?.trim()
        .orEmpty()
        .ifBlank { itemId.trim() }
    val sourceRefs = raw.split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .map { sourceId -> TrainingMaterialRef(itemType, sourceId.removeNodeSuffix()) }
    return buildSet {
        addAll(sourceRefs)
        // Linguistic/read-air progress uses the exercise id as its stable queue identity while
        // payload.sourceId points at the subtitle source. Keep both typed references.
        if (itemType == "exercise" && itemId.isNotBlank()) {
            add(TrainingMaterialRef("exercise", itemId))
        }
    }
}

private fun String.removeNodeSuffix(): String {
    return removeSuffix("-meaning-to-ja")
        .removeSuffix("-ja-to-meaning")
        .removeSuffix("-audio-tiles")
        .removeSuffix("-translation-tiles")
        .removeSuffix("-shadowing-self-check")
        .removeSuffix("-function-choice")
        .removeSuffix("-cloze")
        .removeSuffix("-study")
}

private fun completeMaterialIds(
    actualIds: List<String>,
    preferredIds: List<String>,
    total: Int,
    syntheticPrefix: String,
): List<String> {
    val actual = actualIds.filter(String::isNotBlank).distinct()
    val actualSet = actual.toSet()
    val preferredAvailable = preferredIds.filter(String::isNotBlank).distinct().filter(actualSet::contains)
    val ordered = (preferredAvailable + actual.filterNot(preferredAvailable.toSet()::contains)).toMutableList()
    val resolvedTotal = if (actual.isNotEmpty()) actual.size else total.coerceAtLeast(0)
    var syntheticIndex = 1
    while (ordered.size < resolvedTotal) {
        val candidate = "$syntheticPrefix-$syntheticIndex"
        if (candidate !in ordered) ordered += candidate
        syntheticIndex += 1
    }
    return ordered
}

private fun <T> List<T>.window(batch: Int, batchSize: Int): List<T> {
    val safeSize = batchSize.coerceAtLeast(1)
    return drop((batch.coerceAtLeast(1) - 1) * safeSize).take(safeSize)
}

private fun unitsFor(total: Int, unitSize: Int): Int {
    if (total <= 0) return 0
    return ((total + unitSize - 1) / unitSize).coerceAtLeast(1)
}

private fun rangeLabel(batch: Int, total: Int, unitSize: Int): String {
    val start = ((batch - 1) * unitSize + 1).coerceAtLeast(1)
    val end = (batch * unitSize).coerceAtMost(total.coerceAtLeast(start))
    return "$start–$end"
}
