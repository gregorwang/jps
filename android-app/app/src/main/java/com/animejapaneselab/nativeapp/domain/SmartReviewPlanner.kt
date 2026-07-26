package com.animejapaneselab.nativeapp.domain

import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class SmartReviewPlan(
    val entries: List<SmartReviewEntry> = emptyList(),
    val localMistakeCount: Int = 0,
    val remoteDueCount: Int = 0,
    val mergedDuplicateCount: Int = 0,
    val focusLabel: String = "暂无明显弱点",
    val estimatedMinutes: Int = 0,
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
)

data class SmartReviewEntry(
    val key: String,
    val itemId: String,
    val itemType: String,
    val workSlug: String,
    val episode: Int,
    val title: String,
    val category: String,
    val priority: Int,
    val reason: String,
    val localMistakeId: String? = null,
    val remoteTask: ProgressItem? = null,
    val dueBucket: ReviewDueBucket = ReviewDueBucket.NoSchedule,
    val ease: Int = 0,
    val reviewCount: Int = 0,
) {
    val sourceLabel: String
        get() = when {
            localMistakeId != null && remoteTask != null -> "错题 + 到期复习"
            localMistakeId != null -> "本地错题"
            else -> "到期复习"
        }
}

/** 由 nextReviewOn 与今天比较得出的到期分桶,仅作为条目元数据供 UI 分组,不参与主排序。 */
enum class ReviewDueBucket(val label: String) {
    Overdue("已逾期"),
    DueToday("今天到期"),
    Upcoming("即将到来"),
    NoSchedule("未安排"),
}

fun buildSmartReviewPlan(
    reviewTasks: List<ProgressItem>,
    mistakes: List<MistakeRecord>,
    today: LocalDate = LocalDate.now(),
): SmartReviewPlan {
    val candidates = linkedMapOf<String, ReviewCandidate>()

    reviewTasks.forEach { task ->
        val key = reviewKey(
            itemId = task.itemId,
            workSlug = task.workSlug,
            episode = task.episode,
            fallback = "${task.itemType}|${task.label}",
        )
        val candidate = candidates.getOrPut(key) { ReviewCandidate(key = key) }
        candidate.remoteTask = preferMoreUrgent(candidate.remoteTask, task, today)
    }

    mistakes.forEach { mistake ->
        val key = reviewKey(
            itemId = mistake.itemId,
            workSlug = mistake.workSlug,
            episode = mistake.episode,
            fallback = "${mistake.typeLabel}|${mistake.prompt}|${mistake.expected}",
        )
        val candidate = candidates.getOrPut(key) { ReviewCandidate(key = key) }
        candidate.localMistake = preferRepeated(candidate.localMistake, mistake)
    }

    val dominantCategory = candidates.values
        .groupingBy { it.category() }
        .eachCount()
        .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        ?.key

    val entries = candidates.values
        .map { it.toEntry(today = today, dominantCategory = dominantCategory) }
        .sortedWith(
            compareByDescending<SmartReviewEntry> { it.priority }
                .thenBy { it.remoteTask?.nextReviewOn.orEmpty() }
                .thenBy { it.title },
        )

    return SmartReviewPlan(
        entries = entries,
        localMistakeCount = mistakes.distinctBy { reviewKey(it.itemId, it.workSlug, it.episode, it.prompt) }.size,
        remoteDueCount = reviewTasks.distinctBy { reviewKey(it.itemId, it.workSlug, it.episode, it.label) }.size,
        mergedDuplicateCount = candidates.values.count { it.localMistake != null && it.remoteTask != null },
        focusLabel = dominantCategory ?: "暂无明显弱点",
        estimatedMinutes = if (entries.isEmpty()) 0 else (entries.size * 2).coerceIn(5, 25),
        overdueCount = entries.count { it.dueBucket == ReviewDueBucket.Overdue },
        dueTodayCount = entries.count { it.dueBucket == ReviewDueBucket.DueToday },
    )
}

private data class ReviewCandidate(
    val key: String,
    var localMistake: MistakeRecord? = null,
    var remoteTask: ProgressItem? = null,
) {
    fun category(): String {
        val label = localMistake?.typeLabel.orEmpty() + " " + remoteTask?.itemType.orEmpty()
        return when {
            label.contains("听") || label.contains("跟读") || label.contains("sentence", ignoreCase = true) -> "听力/跟读类"
            label.contains("语法") || label.contains("填空") || label.contains("grammar", ignoreCase = true) -> "语法类"
            label.contains("语言") || label.contains("读空气") || label.contains("exercise", ignoreCase = true) -> "语感类"
            label.contains("词") || label.contains("学习卡") || label.contains("vocab", ignoreCase = true) -> "词汇类"
            else -> "综合薄弱项"
        }
    }

    fun toEntry(today: LocalDate, dominantCategory: String?): SmartReviewEntry {
        val mistake = localMistake
        val task = remoteTask
        val category = category()
        val dueDate = task?.nextReviewOn.toReviewDate()
        val dueBucket = when {
            dueDate == null -> ReviewDueBucket.NoSchedule
            dueDate.isBefore(today) -> ReviewDueBucket.Overdue
            dueDate.isAfter(today) -> ReviewDueBucket.Upcoming
            else -> ReviewDueBucket.DueToday
        }
        val overdueDays = dueDate?.let { due ->
            (today.toEpochDay() - due.toEpochDay()).coerceAtLeast(0).toInt()
        } ?: 0
        val attempts = mistake?.attempts ?: 0
        val effectiveState = task?.state ?: mistake?.lastState
        val stateWeight = when (effectiveState) {
            ReviewState.Bad, ReviewState.Unknown -> 30
            ReviewState.Fuzzy -> 22
            ReviewState.Ok -> 12
            ReviewState.Good, ReviewState.Known, null -> 0
        }
        val ease = task?.ease ?: 0
        val reviewCount = task?.reviewCount ?: 0
        // SM-2 风格的 ease:越低越吃力;0 表示服务端未下发,不加权。
        val easeWeight = when (ease) {
            1, 2 -> 14
            3 -> 6
            else -> 0
        }
        // 已经复习过多次却仍是 Bad/Fuzzy,说明这条真的没吃透。
        val stillStruggling = reviewCount >= 3 &&
            (effectiveState == ReviewState.Bad || effectiveState == ReviewState.Fuzzy)
        val priority =
            (if (mistake != null) 70 else 25) +
                (attempts.coerceAtMost(4) * 10) +
                stateWeight +
                overdueDays.coerceAtMost(30) +
                (if (mistake != null && task != null) 25 else 0) +
                (if (category == dominantCategory) 10 else 0) +
                easeWeight +
                (if (stillStruggling) 8 else 0)

        val reasons = buildList {
            if (mistake != null && task != null) add("错题与到期任务已合并")
            if (attempts > 1) add("累计答错 $attempts 次")
            else if (mistake != null) add("最近答错")
            if (stillStruggling) add("反复复习仍不熟")
            if (overdueDays > 0) add("已逾期 $overdueDays 天")
            else if (task != null) add("今天到期")
            // category 非空,相等即说明 dominantCategory 非空。
            if (category == dominantCategory) add("属于本轮主要弱项")
        }

        return SmartReviewEntry(
            key = key,
            itemId = mistake?.itemId?.ifBlank { null } ?: task?.itemId.orEmpty(),
            itemType = task?.itemType ?: mistake.inferredItemType(),
            workSlug = mistake?.workSlug?.ifBlank { null } ?: task?.workSlug.orEmpty(),
            episode = mistake?.episode?.takeIf { it > 0 } ?: task?.episode ?: 0,
            title = mistake?.prompt?.ifBlank { null } ?: task?.label?.ifBlank { null } ?: "待复习内容",
            category = category,
            priority = priority,
            reason = reasons.joinToString(" · ").ifBlank { "按复习计划安排" },
            localMistakeId = mistake?.itemId,
            remoteTask = task,
            dueBucket = dueBucket,
            ease = ease,
            reviewCount = reviewCount,
        )
    }
}

private fun reviewKey(itemId: String, workSlug: String, episode: Int, fallback: String): String {
    val identity = itemId.ifBlank { fallback }.trim().lowercase()
    return "${workSlug.trim().lowercase()}|$episode|$identity"
}

private fun preferRepeated(current: MistakeRecord?, next: MistakeRecord): MistakeRecord {
    return if (current == null || next.attempts > current.attempts) next else current
}

private fun preferMoreUrgent(current: ProgressItem?, next: ProgressItem, today: LocalDate): ProgressItem {
    if (current == null) return next
    val currentDate = current.nextReviewOn.toReviewDate() ?: today
    val nextDate = next.nextReviewOn.toReviewDate() ?: today
    return if (nextDate.isBefore(currentDate)) next else current
}

private fun String?.toReviewDate(): LocalDate? {
    val value = this?.trim().orEmpty()
    if (value.length < 10) return null
    return try {
        LocalDate.parse(value.take(10))
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun MistakeRecord?.inferredItemType(): String {
    val label = this?.typeLabel.orEmpty()
    return when {
        label.contains("语法") || label.contains("填空") -> "grammar"
        label.contains("听") || label.contains("跟读") -> "sentence"
        label.contains("语言") || label.contains("读空气") || label.contains("选择") -> "exercise"
        else -> "vocab"
    }
}
