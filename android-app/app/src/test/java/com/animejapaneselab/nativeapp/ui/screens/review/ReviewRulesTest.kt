package com.animejapaneselab.nativeapp.ui.screens.review

import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.domain.ReviewDueBucket
import com.animejapaneselab.nativeapp.domain.buildSmartReviewPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ReviewRulesTest {
    private val today = LocalDate.of(2026, 9, 25)

    private fun task(id: String, next: String, state: ReviewState = ReviewState.Fuzzy, type: String = "vocab") = ProgressItem(
        itemId = id,
        itemType = type,
        workSlug = "k-on",
        episode = 1,
        state = state,
        label = "task $id",
        nextReviewOn = next,
    )

    private fun mistake(id: String, attempts: Int = 1, sourceLabel: String = "") = MistakeRecord(
        itemId = id,
        typeLabel = "拼句",
        prompt = "軽音部って何？",
        selected = "a",
        expected = "b",
        explanation = "",
        sourceLabel = sourceLabel,
        attempts = attempts,
        lastState = ReviewState.Bad,
        workSlug = "k-on",
        episode = 1,
    )

    @Test
    fun dueLabels() {
        assertEquals("今天", ReviewRules.dueLabel("2026-09-25", today))
        assertEquals("逾期 3 天", ReviewRules.dueLabel("2026-09-22T00:00:00Z", today))
        assertEquals("10.2", ReviewRules.dueLabel("2026-10-02", today))
        assertNull(ReviewRules.dueLabel("", today))
        assertNull(ReviewRules.dueLabel("not a date", today))
    }

    @Test
    fun cardMetaReadsLikeTheArtboard() {
        assertEquals("拼句 · 第一話 · 错 2 次", ReviewRules.cardMeta("拼句", 1, 2))
        assertEquals("词汇 · 第十三話", ReviewRules.cardMeta("vocab", 13, 0))
        assertEquals("语法", ReviewRules.cardMeta("grammar", 0, 0))
    }

    @Test
    fun deckAndDueCountPutDueCardsFirst() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(
                task("later", "2026-10-01", state = ReviewState.Bad),
                task("today", "2026-09-25"),
                task("old", "2026-09-20"),
            ),
            mistakes = listOf(mistake("m1")),
            today = today,
        )
        val deck = ReviewRules.deck(plan)
        assertEquals(4, deck.size)
        assertEquals(ReviewDueBucket.Upcoming, deck.last().dueBucket)
        assertEquals(3, ReviewRules.dueCount(plan))
    }

    @Test
    fun queueGroupsFollowBucketOrderWithGlobalPositions() {
        val plan = buildSmartReviewPlan(
            reviewTasks = listOf(task("later", "2026-10-01"), task("today", "2026-09-25"), task("old", "2026-09-20")),
            mistakes = listOf(mistake("m1")),
            today = today,
        )
        val groups = ReviewRules.queueGroups(plan.entries)
        assertEquals(
            listOf(ReviewDueBucket.Overdue, ReviewDueBucket.DueToday, ReviewDueBucket.NoSchedule, ReviewDueBucket.Upcoming),
            groups.map { it.bucket },
        )
        assertEquals(listOf(1, 2, 3, 4), groups.flatMap { g -> g.rows.map { it.position } })
        assertEquals("old", ReviewRules.firstInQueue(plan.entries)?.itemId)
        assertTrue(ReviewRules.queueGroups(emptyList()).isEmpty())
    }

    @Test
    fun weakSpotsUseTheLastSevenDaysWeakestFirst() {
        fun at(day: LocalDate) = day.atStartOfDay().toInstant(ZoneOffset.UTC).toString()
        val items = listOf(
            // grammar: 1/3 right
            task("g1", "", ReviewState.Good, "grammar").copy(lastReviewedAt = at(today)),
            task("g2", "", ReviewState.Bad, "grammar").copy(lastReviewedAt = at(today.minusDays(2))),
            task("g3", "", ReviewState.Fuzzy, "grammar").copy(lastReviewedAt = at(today.minusDays(6))),
            // vocab: 2/2 right
            task("v1", "", ReviewState.Known, "vocab").copy(lastReviewedAt = at(today)),
            task("v2", "", ReviewState.Good, "vocab").copy(lastReviewedAt = at(today.minusDays(1))),
            // too old: ignored
            task("v3", "", ReviewState.Bad, "vocab").copy(lastReviewedAt = at(today.minusDays(9))),
            // only one attempt: ignored
            task("s1", "", ReviewState.Bad, "sentence").copy(lastReviewedAt = at(today)),
        )
        val spots = ReviewRules.weakSpots(items, today, zoneId = ZoneOffset.UTC)
        assertEquals(listOf("语法", "词汇"), spots.map { it.name })
        assertEquals(1f / 3f, spots[0].accuracy, 0.0001f)
        assertEquals(1f, spots[1].accuracy, 0.0001f)
    }

    @Test
    fun sourceLineNumbers() {
        assertEquals(16, ReviewRules.sourceLineNo(mistake("m", sourceLabel = "EP03 第 16 行")))
        assertEquals(0, ReviewRules.sourceLineNo(mistake("m", sourceLabel = "EP03")))
        assertEquals(12, ReviewRules.sourceLineNo(task("t", "").copy(payload = mapOf("source_line_no" to " 12 "))))
        assertEquals(0, ReviewRules.sourceLineNo(task("t", "")))
        assertEquals("查看原句", ReviewRules.viewSourceLabel(3))
        assertEquals("查看本集台词", ReviewRules.viewSourceLabel(0))
    }

    @Test
    fun taskLabelsAreCleaned() {
        assertEquals("单选题 第 12 句", ReviewRules.taskLabel("single_choice [cue 12 / foo]"))
        assertEquals("第 4 句", ReviewRules.taskLabel("line 4"))
    }
}
