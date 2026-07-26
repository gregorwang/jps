package com.animejapaneselab.nativeapp.domain

import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState

/** Rebuilds a regular training session at its first unfinished item from persisted progress. */
fun resumeLessonFromProgress(
    nodes: List<LessonNode>,
    progressItems: List<ProgressItem>,
): LessonSession {
    if (nodes.isEmpty() || progressItems.isEmpty()) return LessonEngine.start(nodes)

    val completedProgress = progressItems.filter { item ->
        item.state == ReviewState.Good || item.state == ReviewState.Known
    }
    val (completed, remaining) = nodes.partition { node ->
        completedProgress.any { item -> item.matches(node) }
    }
    if (completed.isEmpty()) return LessonEngine.start(nodes)

    return LessonSession(
        nodes = completed + remaining,
        index = completed.size,
        correct = completed.size,
        answered = completed.size,
    )
}

private fun ProgressItem.matches(node: LessonNode): Boolean {
    val exactNodeId = payload["nodeId"].orEmpty()
    if (exactNodeId.isNotBlank()) return exactNodeId == node.id

    if (itemId == node.id) return true

    // Compatibility for progress written by older Android builds, which used sourceId as itemId.
    val sourceId = node.sourceId.trim()
    val legacyItemId = when {
        node.sourceKind == "exercise" -> node.id
        sourceId.isBlank() || sourceId.contains(",") -> node.id
        else -> sourceId
    }
    return itemType == node.sourceKind && itemId == legacyItemId
}
