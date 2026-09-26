package com.animejapaneselab.nativeapp.ui.foundation

import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.data.FoundationTopicQuery
import com.animejapaneselab.nativeapp.data.RemoteLabClient

/** Walks the cursor-paged topic endpoint to the end; shared by 基础题库 and 活用道場. */
internal fun RemoteLabClient.fetchAllFoundationTopics(): List<FoundationTopic> {
    val result = mutableListOf<FoundationTopic>()
    val seenCursors = mutableSetOf<String>()
    var cursor: String? = null
    do {
        val page = fetchFoundationTopics(FoundationTopicQuery(cursor = cursor))
        result += page.items
        cursor = page.page.nextCursor
        if (page.page.hasMore) {
            require(cursor != null && seenCursors.add(cursor)) {
                "Foundation topic pagination returned an invalid cursor"
            }
        }
    } while (page.page.hasMore)
    require(result.distinctBy(FoundationTopic::id).size == result.size) {
        "Foundation topic pagination returned duplicate ids"
    }
    return result
}
