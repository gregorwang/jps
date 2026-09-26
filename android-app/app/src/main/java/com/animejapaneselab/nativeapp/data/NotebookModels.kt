package com.animejapaneselab.nativeapp.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 栞 — one word, pattern or line the learner tucked away. It carries everything it needs to be
 * shown and replayed later, so the notebook works across episodes without refetching content.
 * Review is a small local Leitner ladder (1/2/4/8/16 days), same shape as 活用道場.
 */
data class NotebookEntry(
    /** `kind:itemId`, stable across episodes. */
    val key: String,
    val kind: NotebookKind,
    /** Headword, grammar pattern, or the line itself. */
    val headline: String,
    val reading: String = "",
    val meaning: String = "",
    /** A line where it appears (vocab/grammar) — empty for lines. */
    val example: String = "",
    val workSlug: String = "",
    val episode: Int = 0,
    val lineNo: Int = 0,
    val audioUrl: String = "",
    val storagePath: String = "",
    val savedAtMillis: Long = 0L,
    /** Leitner box 0..5; 0 = never reviewed. */
    val box: Int = 0,
    /** Epoch day on which the card is due; 0 = due now. */
    val dueDay: Long = 0L,
    val reviews: Int = 0,
    val lapses: Int = 0,
)

enum class NotebookKind(val id: String, val label: String) {
    Vocab("vocab", "词"),
    Grammar("grammar", "文型"),
    Line("line", "台词");

    companion object {
        fun of(id: String): NotebookKind = entries.firstOrNull { it.id == id } ?: Vocab
    }
}

object NotebookRules {
    private val IntervalsDays = listOf(1L, 2L, 4L, 8L, 16L)

    fun key(kind: NotebookKind, itemId: String): String = "${kind.id}:$itemId"

    fun isDue(entry: NotebookEntry, today: Long): Boolean = entry.dueDay <= today

    fun dueCount(entries: List<NotebookEntry>, today: Long): Int = entries.count { isDue(it, today) }

    /** Due cards first (oldest due first, then the ones that lapse most), capped at [limit]. */
    fun dueQueue(entries: List<NotebookEntry>, today: Long, limit: Int = 20): List<NotebookEntry> =
        entries.filter { isDue(it, today) }
            .sortedWith(compareBy<NotebookEntry> { it.dueDay }.thenByDescending { it.lapses }.thenBy { it.savedAtMillis })
            .take(limit)

    /** Remembered → up one box; forgotten → back to box 1, due tomorrow. */
    fun grade(entry: NotebookEntry, remembered: Boolean, today: Long): NotebookEntry {
        val box = if (remembered) (entry.box + 1).coerceAtMost(IntervalsDays.size) else 1
        return entry.copy(
            box = box,
            dueDay = today + IntervalsDays[(box - 1).coerceIn(0, IntervalsDays.lastIndex)],
            reviews = entry.reviews + 1,
            lapses = entry.lapses + if (remembered) 0 else 1,
        )
    }

    /** 「覚えた」 once a card reaches the top box. */
    fun isMastered(entry: NotebookEntry): Boolean = entry.box >= IntervalsDays.size

    fun encode(entries: List<NotebookEntry>): String {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject()
                    .put("k", e.key)
                    .put("t", e.kind.id)
                    .put("h", e.headline)
                    .put("r", e.reading)
                    .put("m", e.meaning)
                    .put("x", e.example)
                    .put("w", e.workSlug)
                    .put("e", e.episode)
                    .put("l", e.lineNo)
                    .put("a", e.audioUrl)
                    .put("p", e.storagePath)
                    .put("s", e.savedAtMillis)
                    .put("b", e.box)
                    .put("d", e.dueDay)
                    .put("n", e.reviews)
                    .put("f", e.lapses),
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<NotebookEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val key = o.optString("k")
                val headline = o.optString("h")
                if (key.isBlank() || headline.isBlank()) return@mapNotNull null
                NotebookEntry(
                    key = key,
                    kind = NotebookKind.of(o.optString("t")),
                    headline = headline,
                    reading = o.optString("r"),
                    meaning = o.optString("m"),
                    example = o.optString("x"),
                    workSlug = o.optString("w"),
                    episode = o.optInt("e"),
                    lineNo = o.optInt("l"),
                    audioUrl = o.optString("a"),
                    storagePath = o.optString("p"),
                    savedAtMillis = o.optLong("s"),
                    box = o.optInt("b"),
                    dueDay = o.optLong("d"),
                    reviews = o.optInt("n"),
                    lapses = o.optInt("f"),
                )
            }
        }.getOrDefault(emptyList())
    }
}

fun VocabItem.toNotebookEntry(workSlug: String, episode: Int, example: ShadowingSentence?): NotebookEntry =
    NotebookEntry(
        key = NotebookRules.key(NotebookKind.Vocab, id),
        kind = NotebookKind.Vocab,
        headline = surface,
        reading = reading.takeIf { it != surface }.orEmpty(),
        meaning = meaningZh,
        example = example?.ja.orEmpty(),
        workSlug = workSlug,
        episode = episode,
        lineNo = example?.sourceLineNo ?: 0,
    )

fun GrammarPoint.toNotebookEntry(workSlug: String, episode: Int): NotebookEntry =
    NotebookEntry(
        key = NotebookRules.key(NotebookKind.Grammar, id),
        kind = NotebookKind.Grammar,
        headline = pattern,
        meaning = titleZh,
        example = exampleJa,
        workSlug = workSlug,
        episode = episode,
        lineNo = sourceLineNo,
    )

fun ShadowingSentence.toNotebookEntry(workSlug: String, episode: Int): NotebookEntry =
    NotebookEntry(
        key = NotebookRules.key(NotebookKind.Line, id),
        kind = NotebookKind.Line,
        headline = ja,
        reading = reading.takeIf { it != ja }.orEmpty(),
        meaning = meaningZh,
        workSlug = workSlug,
        episode = episode,
        lineNo = sourceLineNo,
        audioUrl = audioUrl,
        storagePath = storagePath,
    )

/** Rebuilds just enough of a sentence to reuse [promptAudioForSentence] for a saved line. */
fun NotebookEntry.asShadowingSentence(): ShadowingSentence =
    ShadowingSentence(
        id = key.substringAfter(':'),
        ja = headline,
        reading = reading,
        meaningZh = meaning,
        sourceLabel = "",
        audioKind = AudioKind.Tts,
        sourceLineNo = lineNo,
        audioUrl = audioUrl,
        storagePath = storagePath,
    )
