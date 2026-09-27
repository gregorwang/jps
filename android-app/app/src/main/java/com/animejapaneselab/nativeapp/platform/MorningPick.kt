package com.animejapaneselab.nativeapp.platform

import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.DrillProgress
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/**
 * 朝の一句 drawn from the main line: an anime line of the learned 課 that is fading fastest,
 * picked when the app is open (so its clip can be cached) for the next 08:30.
 */
data class MorningPick(
    val date: String,
    val ja: String,
    val zh: String,
    val point: String,
    val formula: String,
    val audioUrl: String,
    /** NotebookRules encoding of the line, for 挟む. */
    val entry: String,
) {
    fun encode(): String = JSONObject()
        .put("d", date).put("ja", ja).put("zh", zh).put("p", point)
        .put("f", formula).put("au", audioUrl).put("nb", entry)
        .toString()

    companion object {
        private val MorningAt: LocalTime = LocalTime.of(8, 30)

        fun decode(raw: String?): MorningPick? = runCatching {
            val o = JSONObject(raw ?: return null)
            MorningPick(
                date = o.optString("d"),
                ja = o.optString("ja"),
                zh = o.optString("zh"),
                point = o.optString("p"),
                formula = o.optString("f"),
                audioUrl = o.optString("au"),
                entry = o.optString("nb"),
            ).takeIf { it.ja.isNotBlank() }
        }.getOrNull()

        /**
         * The next morning's line: among learned 課, the one with the oldest due line; inside it an
         * unseen or due line, rotating by date so consecutive mornings differ.
         */
        fun choose(
            items: List<ConjugationDrillItem>,
            learned: Set<String>,
            progress: Map<String, DrillProgress>,
            titleOf: (String) -> String,
            now: java.time.LocalDateTime = java.time.LocalDateTime.now(),
        ): MorningPick? {
            val target = if (now.toLocalTime().isBefore(MorningAt)) now.toLocalDate() else now.toLocalDate().plusDays(1)
            val day = target.toEpochDay()
            val pool = items.filter { it.pointId in learned && it.jaText.isNotBlank() }
            if (pool.isEmpty()) return null
            val fadingPoint = pool.mapNotNull { item -> progress[item.id]?.let { item.pointId to it.dueDay } }
                .filter { it.second <= day }
                .minByOrNull { it.second }
                ?.first
            val inPoint = pool.filter { it.pointId == (fadingPoint ?: it.pointId) }
            val fresh = inPoint.filter { progress[it.id].let { p -> p == null || p.dueDay <= day } }
            val candidates = fresh.ifEmpty { inPoint }.distinctBy { it.sentenceId }
            val item = candidates[Math.floorMod(day, candidates.size.toLong()).toInt()]
            val entry = NotebookEntry(
                key = NotebookRules.key(NotebookKind.Line, item.sentenceId),
                kind = NotebookKind.Line,
                headline = item.jaText,
                reading = item.reading,
                meaning = item.zh,
                audioUrl = item.audioUrl,
            )
            return MorningPick(
                date = target.toString(),
                ja = item.jaText,
                zh = item.zh,
                point = titleOf(item.pointId).ifBlank { item.pointTitle },
                formula = item.formula,
                audioUrl = item.audioUrl,
                entry = NotebookRules.encode(listOf(entry)),
            )
        }

        fun isFor(pick: MorningPick?, today: LocalDate): Boolean = pick != null && pick.date == today.toString()
    }
}
