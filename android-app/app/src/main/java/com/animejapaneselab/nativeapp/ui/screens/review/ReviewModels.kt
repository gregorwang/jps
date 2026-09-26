package com.animejapaneselab.nativeapp.ui.screens.review

import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.domain.ReviewDueBucket
import com.animejapaneselab.nativeapp.domain.SmartReviewEntry
import com.animejapaneselab.nativeapp.domain.SmartReviewPlan
import com.animejapaneselab.nativeapp.ui.design.TextRules
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

// Pure rules behind 復習 and the smart review queue (no Compose) so they stay unit-testable.

object ReviewRules {
    /** Buckets that count as "due now": unscheduled local mistakes are due immediately. */
    private val DueNow = setOf(ReviewDueBucket.Overdue, ReviewDueBucket.DueToday, ReviewDueBucket.NoSchedule)

    fun isDueNow(entry: SmartReviewEntry): Boolean = entry.dueBucket in DueNow

    /** 18 枚 — cards to flip now. */
    fun dueCount(plan: SmartReviewPlan): Int = plan.entries.count(::isDueNow)

    /**
     * The flip deck: due cards first (keeping the planner's priority order), upcoming last.
     */
    fun deck(plan: SmartReviewPlan): List<SmartReviewEntry> {
        val (now, later) = plan.entries.partition(::isDueNow)
        return now + later
    }

    /** Short type label: vocab → 词汇, 拼句 stays 拼句, exercise → 读空气 … */
    fun typeLabel(raw: String): String = when (raw.trim()) {
        "vocab", "学习卡", "选择", "词汇" -> "词汇"
        "grammar", "填空", "语法" -> "语法"
        "sentence", "听音", "跟读", "shadowing" -> "跟读"
        "exercise", "语言学题", "读空气", "readair" -> "读空气"
        "拼句", "tile", "tile_order" -> "拼句"
        else -> raw.trim().ifBlank { "训练" }
    }

    /** 拼句 · 第一話 · 错 2 次 */
    fun cardMeta(type: String, episode: Int, attempts: Int): String = listOfNotNull(
        typeLabel(type),
        episode.takeIf { it > 0 }?.let(TextRules::episodeLabel),
        attempts.takeIf { it > 0 }?.let { "错 $it 次" },
    ).joinToString(" · ")

    /** 今天 / 逾期 3 天 / 10.2 / null (no schedule). */
    fun dueLabel(nextReviewOn: String?, today: LocalDate): String? {
        val due = parseDate(nextReviewOn) ?: return null
        val overdue = today.toEpochDay() - due.toEpochDay()
        return when {
            overdue > 0 -> "逾期 $overdue 天"
            overdue == 0L -> "今天"
            else -> "${due.monthValue}.${due.dayOfMonth}"
        }
    }

    fun parseDate(value: String?): LocalDate? {
        val raw = value?.trim().orEmpty()
        if (raw.length < 10) return null
        return try {
            LocalDate.parse(raw.take(10))
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** 期限切れ / 今日 / 近日 / 未定 — Japanese heading for a due bucket, Chinese gloss from the enum. */
    fun bucketTitle(bucket: ReviewDueBucket): String = when (bucket) {
        ReviewDueBucket.Overdue -> "期限切れ"
        ReviewDueBucket.DueToday -> "今日"
        ReviewDueBucket.Upcoming -> "近日"
        ReviewDueBucket.NoSchedule -> "未定"
    }

    data class QueueRow(val position: Int, val entry: SmartReviewEntry)
    data class QueueGroup(val bucket: ReviewDueBucket, val rows: List<QueueRow>)

    /**
     * Groups the planner's entries by due bucket (逾期 → 今天 → 未安排 → 即将) while keeping the
     * planner's priority order inside each bucket. Positions are global (01, 02 …) in display order.
     */
    fun queueGroups(entries: List<SmartReviewEntry>): List<QueueGroup> {
        val order = listOf(
            ReviewDueBucket.Overdue,
            ReviewDueBucket.DueToday,
            ReviewDueBucket.NoSchedule,
            ReviewDueBucket.Upcoming,
        )
        var position = 0
        return order.mapNotNull { bucket ->
            val rows = entries.filter { it.dueBucket == bucket }
            if (rows.isEmpty()) {
                null
            } else {
                QueueGroup(bucket, rows.map { QueueRow(++position, it) })
            }
        }
    }

    /** The first entry of the queue as displayed. */
    fun firstInQueue(entries: List<SmartReviewEntry>): SmartReviewEntry? =
        queueGroups(entries).firstOrNull()?.rows?.firstOrNull()?.entry

    // ---- 苦手なところ -------------------------------------------------------------------

    data class WeakSpot(val name: String, val accuracy: Float, val attempts: Int)

    /** Category of a progress row for the weakness list. */
    fun weakCategory(itemType: String): String = when (itemType.trim().lowercase()) {
        "vocab", "handwriting" -> "词汇"
        "grammar" -> "语法"
        "sentence", "shadowing" -> "听力 · 跟读"
        "exercise", "readair", "read_air", "linguistic" -> "语感 · 读空气"
        "foundation" -> "基础题库"
        else -> "综合"
    }

    private fun ReviewState.isRight(): Boolean = this == ReviewState.Good || this == ReviewState.Known

    /**
     * Accuracy per category over the last [days] days (by `lastReviewedAt`), weakest first. Only
     * categories with at least [minAttempts] rows count, so one lucky answer never shows as 100%.
     */
    fun weakSpots(
        progressItems: List<ProgressItem>,
        today: LocalDate,
        days: Long = 7,
        minAttempts: Int = 2,
        limit: Int = 3,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<WeakSpot> {
        val since = today.minusDays(days - 1)
        return progressItems
            .filter { item ->
                val day = activityDate(item.lastReviewedAt, zoneId) ?: return@filter false
                !day.isBefore(since) && !day.isAfter(today)
            }
            .groupBy { item ->
                if (item.payload["track"] == "foundation") weakCategory("foundation") else weakCategory(item.itemType)
            }
            .filter { it.value.size >= minAttempts }
            .map { (name, items) ->
                WeakSpot(name, items.count { it.state.isRight() }.toFloat() / items.size, items.size)
            }
            .sortedWith(compareBy<WeakSpot> { it.accuracy }.thenByDescending { it.attempts }.thenBy { it.name })
            .take(limit)
    }

    /** ISO instant, offset date-time or plain date → local date; null when unparseable. */
    fun activityDate(raw: String, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        runCatching { return Instant.parse(value).atZone(zoneId).toLocalDate() }
        runCatching { return OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalDate() }
        return parseDate(value)
    }

    // ---- 错题本 -------------------------------------------------------------------------

    private val SourceLineRegex = Regex("""第\s*(\d+)\s*行""")

    /** Line number from a mistake's sourceLabel (「EP03 第 16 行」); 0 = open the episode only. */
    fun sourceLineNo(mistake: MistakeRecord): Int =
        SourceLineRegex.find(mistake.sourceLabel)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: 0

    /** Line number from a remote task payload (camelCase or snake_case); 0 when absent. */
    fun sourceLineNo(task: ProgressItem): Int {
        val raw = task.payload["sourceLineNo"] ?: task.payload["source_line_no"] ?: return 0
        return raw.trim().toIntOrNull()?.takeIf { it > 0 } ?: 0
    }

    /** 查看原句 when a line can be focused, otherwise 查看本集台词. */
    fun viewSourceLabel(lineNo: Int): String = if (lineNo > 0) "查看原句" else "查看本集台词"

    /** Cleans worker labels such as `single_choice [cue 12 …]` into readable Chinese. */
    fun taskLabel(label: String): String = label
        .replace("single_choice", "单选题")
        .replace("morphology_analysis", "词形分析")
        .replace("historical", "上下文理解")
        .replace(Regex("""[【\[]\s*cue\s+(\d+)[^】\]]*[】\]]""", RegexOption.IGNORE_CASE), "第 $1 句")
        .replace(Regex("""[【\[]\s*第\s+(\d+)\s+句\s*/[^】\]]*[】\]]"""), "第 $1 句")
        .replace(Regex("""\bcue\s+(\d+)""", RegexOption.IGNORE_CASE), "第 $1 句")
        .replace(Regex("""\bline\s+(\d+)""", RegexOption.IGNORE_CASE), "第 $1 句")
        .trim()

    fun mistakeIdentity(mistake: MistakeRecord): String = mistake.itemId.ifBlank {
        listOf(mistake.workSlug, mistake.episode.toString(), mistake.typeLabel, mistake.prompt, mistake.expected).joinToString("|")
    }

    fun taskIdentity(task: ProgressItem): String = task.itemId.ifBlank {
        listOf(task.workSlug, task.episode.toString(), task.itemType, task.label).joinToString("|")
    }
}
