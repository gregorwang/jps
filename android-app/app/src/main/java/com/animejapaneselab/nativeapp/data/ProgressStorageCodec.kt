package com.animejapaneselab.nativeapp.data

import org.json.JSONArray
import org.json.JSONObject

internal object ProgressStorageCodec {
    fun decode(raw: String): List<ProgressItem> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val itemId = item.optString("itemId")
                    if (itemId.isBlank()) continue
                    val payloadJson = item.optJSONObject("payload") ?: JSONObject()
                    val payload = buildMap {
                        val keys = payloadJson.keys()
                        while (keys.hasNext()) {
                            val payloadKey = keys.next()
                            put(payloadKey, payloadJson.optString(payloadKey))
                        }
                    }
                    add(
                        ProgressItem(
                            itemId = itemId,
                            itemType = item.optString("itemType", "unknown"),
                            workSlug = item.optString("workSlug"),
                            episode = item.optInt("episode", 0),
                            state = reviewState(item.optString("state")),
                            label = item.optString("label", itemId),
                            lastReviewedAt = item.optString("lastReviewedAt"),
                            nextReviewOn = item.optString("nextReviewOn"),
                            payload = payload,
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun encode(progress: List<ProgressItem>): String {
        val array = JSONArray()
        progress.forEach { item ->
            val payload = JSONObject()
            item.payload.forEach(payload::put)
            array.put(
                JSONObject()
                    .put("itemId", item.itemId)
                    .put("itemType", item.itemType)
                    .put("workSlug", item.workSlug)
                    .put("episode", item.episode)
                    .put("state", item.state.remoteValue)
                    .put("label", item.label)
                    .put("lastReviewedAt", item.lastReviewedAt)
                    .put("nextReviewOn", item.nextReviewOn)
                    .put("payload", payload),
            )
        }
        return array.toString()
    }

    private fun reviewState(value: String): ReviewState {
        return ReviewState.entries.firstOrNull { it.remoteValue == value } ?: ReviewState.Bad
    }
}
