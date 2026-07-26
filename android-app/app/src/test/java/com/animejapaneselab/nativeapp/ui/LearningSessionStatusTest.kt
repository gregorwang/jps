package com.animejapaneselab.nativeapp.ui

import com.animejapaneselab.nativeapp.data.EpisodeFocus
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import com.animejapaneselab.nativeapp.domain.LessonSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearningSessionStatusTest {
    private val focus = EpisodeFocus(
        workSlug = "re-zero",
        episodeNumber = 1,
        workTitle = "Re:Zero",
        episodeLabel = "EP01",
        lessonTitle = "跟读训练",
        sectionTitle = "测试",
        guidebook = "测试",
        dailyGoal = 20,
        xp = 0,
        streakDays = 0,
        energy = 5,
    )
    private val nodes = (1..5).map { index ->
        SingleChoiceNode(
            id = "node-$index",
            title = "题目 $index",
            prompt = "prompt",
            explanation = "",
            sourceLabel = "",
            body = null,
            choices = listOf("A", "B"),
            answer = "A",
        )
    }

    @Test
    fun lessonStatusSeparatesCurrentPositionFromCompletedProgress() {
        val status = buildLessonSessionStatus(
            focus = focus,
            lesson = LessonSession(nodes = nodes, index = 2, answered = 2),
            sessionXp = 18,
        )!!

        assertEquals(2, status.completed)
        assertEquals(3, status.position)
        assertEquals(5, status.total)
        assertEquals("3/5", status.chipText)
        assertEquals("Re:Zero · EP01 · XP 18", status.subtitle)
    }

    @Test
    fun readAirStatusClampsCompletedCountAndMarksFinalPosition() {
        val status = buildReadAirSessionStatus(focus, completed = 9, total = 7, sessionXp = 12)!!

        assertEquals(7, status.completed)
        assertEquals(7, status.position)
        assertEquals("7/7", status.chipText)
    }

    @Test
    fun emptySessionsDoNotCreateNotifications() {
        assertNull(buildLessonSessionStatus(focus, LessonSession(emptyList()), 0))
        assertNull(buildReadAirSessionStatus(focus, completed = 0, total = 0, sessionXp = 0))
    }
}
