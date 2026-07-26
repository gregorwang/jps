package com.animejapaneselab.nativeapp.ui

import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressMergeTest {
    @Test
    fun newerLocalProgressWinsWhileRemoteOnlyItemsArePreserved() {
        val local = progress("shared", "2026-07-11T10:00:00Z", ReviewState.Good)
        val staleRemote = progress("shared", "2026-07-10T10:00:00Z", ReviewState.Bad)
        val remoteOnly = progress("remote-only", "2026-07-09T10:00:00Z", ReviewState.Ok)

        val merged = mergeProgressItems(listOf(local), listOf(staleRemote, remoteOnly))

        assertEquals(2, merged.size)
        assertEquals(ReviewState.Good, merged.first { it.itemId == "shared" }.state)
        assertEquals(ReviewState.Ok, merged.first { it.itemId == "remote-only" }.state)
    }

    @Test
    fun newerRemoteProgressKeepsLocalPayloadFieldsThatServerOmitted() {
        val local = progress("shared", "2026-07-11T10:00:00Z", ReviewState.Good).copy(
            payload = mapOf("selected" to "本机答案", "sourceId" to "source-1"),
        )
        val remote = progress("shared", "2026-07-12T10:00:00Z", ReviewState.Good).copy(
            payload = mapOf("answer" to "正确答案"),
        )

        val merged = mergeProgressItems(listOf(local), listOf(remote)).single()

        assertEquals("本机答案", merged.payload["selected"])
        assertEquals("source-1", merged.payload["sourceId"])
        assertEquals("正确答案", merged.payload["answer"])
        assertEquals("2026-07-12T10:00:00Z", merged.lastReviewedAt)
    }

    @Test
    fun successfulCloudResponseCannotEraseLocallyCommittedAnswerPayload() {
        val local = progress("linguistic-1", "2026-07-12T10:00:00Z", ReviewState.Good).copy(
            workSlug = "k-on",
            episode = 3,
            label = "本机题目",
            payload = mapOf("selected" to "本机答案", "sourceId" to "cue-3"),
        )
        val sparseServer = local.copy(
            workSlug = "",
            episode = 0,
            label = "",
            lastReviewedAt = "",
            payload = mapOf("answer" to "正确答案"),
        )

        val durable = mergeSyncedProgressItem(sparseServer, local)

        assertEquals("k-on", durable.workSlug)
        assertEquals(3, durable.episode)
        assertEquals("本机题目", durable.label)
        assertEquals("2026-07-12T10:00:00Z", durable.lastReviewedAt)
        assertEquals("本机答案", durable.payload["selected"])
        assertEquals("正确答案", durable.payload["answer"])
    }

    private fun progress(itemId: String, reviewedAt: String, state: ReviewState) = ProgressItem(
        itemId = itemId,
        itemType = "sentence",
        workSlug = "re-zero",
        episode = 1,
        state = state,
        label = itemId,
        lastReviewedAt = reviewedAt,
    )
}
