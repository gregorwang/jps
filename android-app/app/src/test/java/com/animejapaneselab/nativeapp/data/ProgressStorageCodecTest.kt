package com.animejapaneselab.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressStorageCodecTest {
    @Test
    fun roundTripPreservesLinguisticSelectedAnswer() {
        val expected = listOf(
            ProgressItem(
                itemId = "linguistic-1",
                itemType = "exercise",
                workSlug = "k-on",
                episode = 3,
                state = ReviewState.Good,
                label = "为什么这里省略了主语？",
                lastReviewedAt = "2026-07-13T08:00:00Z",
                nextReviewOn = "2026-07-14",
                payload = mapOf(
                    "selected" to "因为语境中双方都知道主语",
                    "answer" to "因为语境中双方都知道主语",
                    "sourceId" to "cue-25",
                ),
            ),
        )

        val restored = ProgressStorageCodec.decode(ProgressStorageCodec.encode(expected))

        assertEquals(expected, restored)
    }

    @Test
    fun malformedStorageFailsClosedWithoutInventingProgress() {
        assertEquals(emptyList<ProgressItem>(), ProgressStorageCodec.decode("not-json"))
    }
}
