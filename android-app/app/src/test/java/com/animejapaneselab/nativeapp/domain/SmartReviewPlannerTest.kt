package com.animejapaneselab.nativeapp.domain

import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartReviewPlannerTest {
    private val today = LocalDate.of(2026, 7, 11)

    @Test
    fun mergesTheSameLocalMistakeAndRemoteTask() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(remoteTask(itemId = "grammar-1")),
            mistakes = listOf(mistake(itemId = "grammar-1", attempts = 2)),
            today = today,
        )

        assertEquals(1, plan.entries.size)
        assertEquals(1, plan.mergedDuplicateCount)
        assertEquals("错题 + 到期复习", plan.entries.single().sourceLabel)
        assertTrue(plan.entries.single().reason.contains("已合并"))
        // 70 错题基础 + 20 重复答错 + 30 Bad + 0 逾期 + 25 合并 + 10 主要弱项
        assertEquals(155, plan.entries.single().priority)
        assertEquals(ReviewDueBucket.DueToday, plan.entries.single().dueBucket)
        assertEquals(0, plan.overdueCount)
        assertEquals(1, plan.dueTodayCount)
    }

    @Test
    fun repeatedMistakesRankAheadOfPlainDueTasks() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(remoteTask(itemId = "due-only", nextReviewOn = "2026-07-01")),
            mistakes = listOf(mistake(itemId = "repeated", attempts = 4)),
            today = today,
        )

        assertEquals("repeated", plan.entries.first().itemId)
        assertTrue(plan.entries.first().reason.contains("累计答错 4 次"))
        // 70 + 40 + 30 Bad + 10 主要弱项
        assertEquals(150, plan.entryFor("repeated").priority)
        // 25 + 30 Bad + 10 逾期天数 + 10 主要弱项
        assertEquals(75, plan.entryFor("due-only").priority)
        assertEquals(ReviewDueBucket.NoSchedule, plan.entryFor("repeated").dueBucket)
        assertEquals(ReviewDueBucket.Overdue, plan.entryFor("due-only").dueBucket)
        assertEquals(1, plan.overdueCount)
    }

    @Test
    fun overdueDaysAndDominantWeaknessAreExplained() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(
                remoteTask(itemId = "sentence-1", itemType = "sentence", nextReviewOn = "2026-07-01"),
                remoteTask(itemId = "sentence-2", itemType = "sentence", nextReviewOn = "2026-07-11"),
                remoteTask(itemId = "grammar-1", itemType = "grammar", nextReviewOn = "2026-07-11"),
            ),
            mistakes = emptyList(),
            today = today,
        )

        assertEquals("听力/跟读类", plan.focusLabel)
        assertEquals("sentence-1", plan.entries.first().itemId)
        assertTrue(plan.entries.first().reason.contains("已逾期 10 天"))
        assertEquals(listOf(75, 65, 55), plan.entries.map { it.priority })
        assertEquals(1, plan.overdueCount)
        assertEquals(2, plan.dueTodayCount)
    }

    @Test
    fun lowEaseTasksRankAheadOfComfortableOnes() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(
                remoteTask(itemId = "comfortable", ease = 5),
                remoteTask(itemId = "unknown-ease", ease = 0),
                remoteTask(itemId = "hard", ease = 2),
                remoteTask(itemId = "medium", ease = 3),
            ),
            mistakes = emptyList(),
            today = today,
        )

        // 基线 25 + 30 Bad + 10 主要弱项 = 65,ease 权重叠加其上。
        assertEquals(79, plan.entryFor("hard").priority)
        assertEquals(71, plan.entryFor("medium").priority)
        assertEquals(65, plan.entryFor("comfortable").priority)
        // ease 未知(0)与 ease 充足(>=4)同样不加权。
        assertEquals(plan.entryFor("comfortable").priority, plan.entryFor("unknown-ease").priority)
        assertEquals(listOf("hard", "medium"), plan.entries.take(2).map { it.itemId })
        assertEquals(2, plan.entryFor("hard").ease)
    }

    @Test
    fun repeatedReviewsThatAreStillFuzzyGetExtraWeight() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(
                remoteTask(itemId = "drilled", state = ReviewState.Fuzzy, reviewCount = 3),
                remoteTask(itemId = "twice", state = ReviewState.Fuzzy, reviewCount = 2),
                remoteTask(itemId = "settled", state = ReviewState.Good, reviewCount = 6),
            ),
            mistakes = emptyList(),
            today = today,
        )

        assertEquals("drilled", plan.entries.first().itemId)
        // 25 + 22 Fuzzy + 10 主要弱项 + 8 反复复习仍不熟
        assertEquals(65, plan.entryFor("drilled").priority)
        assertEquals(57, plan.entryFor("twice").priority)
        assertEquals(8, plan.entryFor("drilled").priority - plan.entryFor("twice").priority)
        assertEquals(3, plan.entryFor("drilled").reviewCount)
        assertTrue(plan.entryFor("drilled").reason.contains("反复复习仍不熟"))
        // 复习次数不足 3 次,或状态已经转好,都不该加权。
        assertFalse(plan.entryFor("twice").reason.contains("反复复习仍不熟"))
        assertFalse(plan.entryFor("settled").reason.contains("反复复习仍不熟"))
        assertEquals(35, plan.entryFor("settled").priority)
    }

    @Test
    fun dueBucketsClassifyEverySchedule() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(
                remoteTask(itemId = "overdue-1", nextReviewOn = "2026-07-01"),
                remoteTask(itemId = "overdue-2", nextReviewOn = "2026-07-10"),
                remoteTask(itemId = "today-1", nextReviewOn = "2026-07-11"),
                remoteTask(itemId = "today-2", nextReviewOn = "2026-07-11T09:00:00Z"),
                remoteTask(itemId = "future-1", nextReviewOn = "2026-08-01"),
                remoteTask(itemId = "broken-1", nextReviewOn = "not-a-date"),
                remoteTask(itemId = "blank-1", nextReviewOn = ""),
            ),
            mistakes = listOf(mistake(itemId = "local-only", attempts = 1)),
            today = today,
        )

        assertEquals(8, plan.entries.size)
        assertEquals(ReviewDueBucket.Overdue, plan.entryFor("overdue-1").dueBucket)
        assertEquals(ReviewDueBucket.Overdue, plan.entryFor("overdue-2").dueBucket)
        assertEquals(ReviewDueBucket.DueToday, plan.entryFor("today-1").dueBucket)
        assertEquals(ReviewDueBucket.DueToday, plan.entryFor("today-2").dueBucket)
        assertEquals(ReviewDueBucket.Upcoming, plan.entryFor("future-1").dueBucket)
        // 解析失败与空日期都归为「未安排」。
        assertEquals(ReviewDueBucket.NoSchedule, plan.entryFor("broken-1").dueBucket)
        assertEquals(ReviewDueBucket.NoSchedule, plan.entryFor("blank-1").dueBucket)
        // 纯本地错题没有排程,同样是「未安排」,但仍按 priority 排在最前面。
        assertEquals(ReviewDueBucket.NoSchedule, plan.entryFor("local-only").dueBucket)
        assertEquals("local-only", plan.entries.first().itemId)
        assertEquals("已逾期", ReviewDueBucket.Overdue.label)
    }

    @Test
    fun overdueAndDueTodayCountsAreReported() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(
                remoteTask(itemId = "overdue-1", nextReviewOn = "2026-07-01"),
                remoteTask(itemId = "overdue-2", nextReviewOn = "2026-07-09"),
                remoteTask(itemId = "today-1", nextReviewOn = "2026-07-11"),
                remoteTask(itemId = "today-2", nextReviewOn = "2026-07-11"),
                remoteTask(itemId = "today-3", nextReviewOn = "2026-07-11"),
                remoteTask(itemId = "future-1", nextReviewOn = "2026-07-20"),
                remoteTask(itemId = "unscheduled-1", nextReviewOn = ""),
            ),
            mistakes = emptyList(),
            today = today,
        )

        assertEquals(2, plan.overdueCount)
        assertEquals(3, plan.dueTodayCount)
        assertEquals(7, plan.entries.size)
    }

    private fun SmartReviewPlan.entryFor(itemId: String): SmartReviewEntry =
        entries.first { it.itemId == itemId }

    private fun remoteTask(
        itemId: String,
        itemType: String = "grammar",
        nextReviewOn: String = "2026-07-11",
        state: ReviewState = ReviewState.Bad,
        ease: Int = 0,
        reviewCount: Int = 0,
    ) = ProgressItem(
        itemId = itemId,
        itemType = itemType,
        workSlug = "re-zero",
        episode = 1,
        state = state,
        label = "复习 $itemId",
        nextReviewOn = nextReviewOn,
        ease = ease,
        reviewCount = reviewCount,
    )

    private fun mistake(
        itemId: String,
        attempts: Int,
        lastState: ReviewState = ReviewState.Bad,
    ) = MistakeRecord(
        itemId = itemId,
        typeLabel = "语法题",
        prompt = "错题 $itemId",
        selected = "A",
        expected = "B",
        explanation = "解析",
        sourceLabel = "Re:Zero EP01",
        attempts = attempts,
        lastState = lastState,
        workSlug = "re-zero",
        episode = 1,
    )
}
