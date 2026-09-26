package com.animejapaneselab.nativeapp.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 活用道場 item: one basic-grammar point exemplified by one audio-backed anime line
 * (`/api/conjugation-drill/items`, table `conjugation_drill_items`). [spanStart]..[spanEnd] is
 * the target inside [jaText]; [head] is the word whose conjugation the item drills.
 */
data class ConjugationDrillItem(
    val id: String,
    val pointId: String,
    val pointTitle: String,
    val group: String,
    val sentenceId: String,
    val episodeLabel: String,
    val startTime: String,
    val jaText: String,
    val reading: String,
    val spanStart: Int,
    val spanEnd: Int,
    val target: String,
    val head: ConjugationHead,
    val zh: String,
    val formula: String,
    val sense: String,
    val note: String,
    val audioUrl: String,
    val sortOrder: Int,
)

/** UniDic analysis of the conjugated word: `ctype` like 五段-カ行, `cform` like 連用形-促音便. */
data class ConjugationHead(
    val surface: String,
    val base: String,
    val kanaBase: String,
    val pos: String,
    val ctype: String,
    val cform: String,
)

/** Local Leitner state of one item: [box] 0..5, due on [dueDay] (epoch day). */
data class DrillProgress(val box: Int, val dueDay: Long, val seen: Int, val wrong: Int)

internal fun parseConjugationDrillItems(body: String): List<ConjugationDrillItem> {
    val array = JSONArray(body)
    return (0 until array.length()).mapNotNull { index ->
        val row = array.optJSONObject(index) ?: return@mapNotNull null
        val ja = row.optString("ja_text")
        val start = row.optInt("span_start", -1)
        val end = row.optInt("span_end", -1)
        val audio = row.optString("audio_url")
        if (ja.isBlank() || start < 0 || end <= start || end > ja.length || audio.isBlank()) return@mapNotNull null
        val head = row.optJSONObject("head_json") ?: JSONObject()
        ConjugationDrillItem(
            id = row.optString("id"),
            pointId = row.optString("point_id"),
            pointTitle = row.optString("point_title_zh"),
            group = row.optString("group_zh"),
            sentenceId = row.optString("sentence_id"),
            episodeLabel = row.optString("episode_label"),
            startTime = row.optString("start_time").takeUnless { it == "null" }.orEmpty(),
            jaText = ja,
            reading = row.optString("reading").takeUnless { it == "null" }.orEmpty(),
            spanStart = start,
            spanEnd = end,
            target = row.optString("target"),
            head = ConjugationHead(
                surface = head.optString("surface"),
                base = head.optString("base"),
                kanaBase = head.optString("kana_base"),
                pos = head.optString("pos"),
                ctype = head.optString("ctype"),
                cform = head.optString("cform"),
            ),
            zh = row.optString("zh"),
            formula = row.optString("formula"),
            sense = row.optString("sense"),
            note = row.optString("note"),
            audioUrl = audio,
            sortOrder = row.optInt("sort_order"),
        )
    }
}
