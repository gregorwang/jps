package com.animejapaneselab.nativeapp.ui

import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningXpTest {

    private fun item(id: String, state: ReviewState): ProgressItem {
        return ProgressItem(
            itemId = id,
            itemType = "vocab",
            workSlug = "k-on",
            episode = 1,
            state = state,
            label = id,
        )
    }

    @Test
    fun emptyProgressEarnsZeroXp() {
        assertEquals(0, learningXp(emptyList()))
    }

    @Test
    fun masteredItemsEarnMoreThanStrugglingOnes() {
        val xp = learningXp(
            listOf(
                item("a", ReviewState.Good),
                item("b", ReviewState.Known),
                item("c", ReviewState.Ok),
                item("d", ReviewState.Fuzzy),
                item("e", ReviewState.Bad),
                item("f", ReviewState.Unknown),
            ),
        )
        assertEquals(10 + 10 + 6 + 4 + 2 + 2, xp)
    }

    @Test
    fun everyAttemptEarnsAtLeastSomething() {
        val xp = learningXp(listOf(item("a", ReviewState.Bad)))
        assertEquals(2, xp)
    }
}
