package com.animejapaneselab.nativeapp.ui.words

import android.content.Context
import com.animejapaneselab.nativeapp.ui.reading.Kana
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import kotlin.random.Random

/*
 * 同音の部屋: words of the 辞書 that share a reading (かし = 歌詞 / 菓子 / 貸し). Curated offline in
 * archive-content-sources/homophones (groups.txt → build.py → assets/homophones.json):
 * - [HomoWord.cluster]: words with the same non-zero number are one Japanese word written with
 *   different kanji (取る / 撮る); 0 = only the sound is shared (歌詞 / 菓子).
 * - [HomoWord.cut] / [HomoWord.cutKana]: the piece of the word's vocab_lines line to blank out in
 *   the quiz (hear the line, pick the kanji); empty when the line can't carry a question.
 */

data class HomoWord(
    val id: String,
    val surface: String,
    val meaning: String,
    val cluster: Int,
    val cut: String,
    val cutKana: String,
)

data class HomoGroup(val reading: String, val words: List<HomoWord>, val note: String) {
    val romaji: String get() = Kana.romaji(reading)

    /** Words that are one word with several kanji, cluster by cluster (each list has 2+ words). */
    val sameWord: List<List<HomoWord>>
        get() = words.filter { it.cluster != 0 }.groupBy { it.cluster }.values.filter { it.size >= 2 }

    /** Words that only share the sound with the others. */
    val soundOnly: List<HomoWord>
        get() = sameWord.flatten().map { it.id }.toSet().let { same -> words.filter { it.id !in same } }
}

/** One quiz question: [line] with [word]'s [HomoWord.cut] blanked, options = the group's words. */
data class HomoQuestion(val group: HomoGroup, val word: HomoWord, val line: TangoLine) {
    val start: Int get() = line.ja.indexOf(word.cut)
    val answer: Int get() = group.words.indexOf(word)
    /** The line with the blank filled in kana (what the ear hears, not what the eye would give away). */
    val heard: String get() = line.ja.replaceFirst(word.cut, word.cutKana)
}

object HomophoneRules {
    enum class Filter(val label: String) { All("全部"), SameWord("同一个词 · 换字"), SoundOnly("碰巧同音") }

    fun matches(group: HomoGroup, filter: Filter): Boolean = when (filter) {
        Filter.All -> true
        Filter.SameWord -> group.sameWord.isNotEmpty()
        Filter.SoundOnly -> group.sameWord.isEmpty()
    }

    /** Groups shown under a JLPT level: any of its words is at that level. */
    fun atLevel(groups: List<HomoGroup>, levelOf: (String) -> String?, level: String): List<HomoGroup> =
        groups.filter { g -> g.words.any { levelOf(it.id) == level } }

    /** Questions for [groups]: every word with a usable line, shuffled by [seed]. */
    fun questions(groups: List<HomoGroup>, lines: Map<String, TangoLine>, seed: Long): List<HomoQuestion> =
        groups.flatMap { g ->
            g.words.mapNotNull { w ->
                val line = lines[w.id] ?: return@mapNotNull null
                if (w.cut.isEmpty() || w.cut !in line.ja) null else HomoQuestion(g, w, line)
            }
        }.shuffled(Random(seed))

    /** A mixed round over [groups]: at most one question per room, original voice first. */
    fun mixedRound(groups: List<HomoGroup>, lines: Map<String, TangoLine>, seed: Long, size: Int = 10): List<HomoQuestion> =
        questions(groups, lines, seed)
            .distinctBy { it.group.reading }
            .sortedBy { if (it.line.audioUrl.isEmpty()) 1 else 0 }
            .take(size)
            .shuffled(Random(seed + 1))
}

/** `assets/homophones.json`, parsed once (off the main thread: [load] is synchronized). */
object Homophones {
    private const val Asset = "homophones.json"

    @Volatile
    private var groups: List<HomoGroup>? = null

    @Volatile
    private var bySurface: Map<String, HomoGroup> = emptyMap()

    fun peek(): List<HomoGroup>? = groups

    /** The room a word belongs to, if it has one (only after [load]). */
    fun groupOf(surface: String): HomoGroup? = bySurface[surface]

    fun load(context: Context): List<HomoGroup> {
        groups?.let { return it }
        synchronized(this) {
            groups?.let { return it }
            val parsed = runCatching {
                val root = JSONObject(context.applicationContext.assets.open(Asset).bufferedReader().use { it.readText() })
                val arr = root.getJSONArray("groups")
                (0 until arr.length()).map { i ->
                    val g = arr.getJSONObject(i)
                    val ws = g.getJSONArray("w")
                    HomoGroup(
                        reading = g.getString("r"),
                        words = (0 until ws.length()).map { j ->
                            val a = ws.getJSONArray(j)
                            HomoWord(a.getString(0), a.getString(1), a.getString(2), a.optInt(3), a.optString(4), a.optString(5))
                        },
                        note = g.optString("note"),
                    )
                }
            }.getOrDefault(emptyList())
            bySurface = parsed.flatMap { g -> g.words.map { it.surface to g } }.toMap()
            groups = parsed
            return parsed
        }
    }
}

/**
 * Whether the mixed listening round is open (the 辞書 同音 list opens it).
 */
object HomophoneRoom {
    sealed interface Request {
        /** A mixed round over the rooms of one JLPT level. */
        data class Round(val level: String) : Request
    }

    private val _open = MutableStateFlow<Request?>(null)
    val open: StateFlow<Request?> = _open.asStateFlow()

    fun openRound(level: String) {
        _open.value = Request.Round(level)
    }

    fun close() {
        _open.value = null
    }
}
