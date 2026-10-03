package com.animejapaneselab.nativeapp.ui.zougo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/*
 * 第四巻 造語 — word building, after 《日语词汇与构词法》. Two kinds of 課 for now:
 *  - fuse (拼合台): two parts → guess how the compound reads → the changed sound lights up
 *    (連濁 / 母音交替 / 音韻添加 / 促音);
 *  - matrix (组合矩阵): front verb × back verb; a header shows what the part adds, a cell is a word.
 * Content: `assets/zougo_lessons.json`, written by archive-content-sources/zougo/build_zougo.py.
 */

/**
 * What happened to a piece of romaji when the parts joined; [Already] marks the voiced sound that blocks 連濁
 * (or, in the 活用 books, the part that stays as it is), [Gone] the ending that drops out, [Bad] an exception.
 */
enum class SegKind { Same, Voiced, Vowel, Insert, Gem, Already, Gone, Bad;
    companion object {
        fun of(s: String) = when (s) {
            "voiced" -> Voiced
            "vowel" -> Vowel
            "insert" -> Insert
            "gem" -> Gem
            "already" -> Already
            "gone" -> Gone
            "bad" -> Bad
            else -> Same
        }
    }
}

data class ZgSeg(val text: String, val kind: SegKind)

data class ZgPart(val kanji: String, val kana: String, val romaji: List<ZgSeg>)

/** A line for a word: from the anime ([fromAnime], maybe with 原声 [audioUrl]) or a written example. */
data class ZgLine(
    val ja: String,
    val pre: String,
    val target: String,
    val post: String,
    val romaji: String,
    val zh: String,
    val fromAnime: Boolean,
    val audioUrl: String,
)

data class ZgFuse(
    val word: String,
    val kana: String,
    val a: ZgPart,
    val b: ZgPart,
    val segs: List<ZgSeg>,
    val options: List<String>,
    val answer: Int,
    val tags: List<Pair<String, SegKind>>,
    val rule: String,
    val line: ZgLine,
    /** The question over the result panel; blank = "合起来怎么读？". */
    val ask: String = "",
) {
    val romaji: String get() = segs.joinToString("") { it.text }
}

data class ZgHead(val text: String, val romaji: String, val core: List<String>)

sealed interface ZgCell {
    data class Word(
        val word: String,
        val kana: String,
        val romaji: String,
        val front: String,
        val back: String,
        val meaning: String,
        val line: ZgLine,
    ) : ZgCell

    /** In the dictionary, hardly ever said. */
    data object Rare : ZgCell

    /** Not a word. */
    data object None : ZgCell
}

data class ZgMatrix(val focusRow: Boolean, val cols: List<ZgHead>, val rows: List<ZgHead>, val cells: List<List<ZgCell>>) {
    val words: List<Triple<Int, Int, ZgCell.Word>>
        get() = cells.flatMapIndexed { r, row -> row.mapIndexedNotNull { c, x -> (x as? ZgCell.Word)?.let { Triple(r, c, it) } } }
}

enum class ZgKind { Fuse, Matrix }

data class ZgLesson(
    val id: String,
    val number: Int,
    val section: Int,
    val kind: ZgKind,
    val title: String,
    val gloss: String,
    val fuse: List<ZgFuse>,
    val matrix: ZgMatrix?,
) {
    /** Words this 課 teaches (for counts and the 目次 sample). */
    val words: List<String>
        get() = if (kind == ZgKind.Fuse) fuse.map { it.word } else matrix?.words?.map { it.third.word }.orEmpty()
}

data class ZgSection(val number: String, val title: String, val zh: String, val kind: ZgKind)

data class ZgBookData(val sections: List<ZgSection>, val lessons: List<ZgLesson>) {
    fun lesson(id: String): ZgLesson? = lessons.firstOrNull { it.id == id }
}

object ZougoBook {
    private const val Asset = "zougo_lessons.json"
    @Volatile private var cached: ZgBookData? = null

    fun load(context: Context): ZgBookData {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: runCatching {
                parse(context.applicationContext.assets.open(Asset).bufferedReader().use { it.readText() })
            }.getOrElse { ZgBookData(emptyList(), emptyList()) }.also { cached = it }
        }
    }

    internal fun parse(raw: String): ZgBookData {
        val top = JSONObject(raw)
        val base = top.optString("audioBase")
        val sections = top.getJSONArray("sections").objects().map {
            ZgSection(it.getString("no"), it.getString("title"), it.getString("zh"), kindOf(it.getString("kind")))
        }
        val lessons = top.getJSONArray("lessons").objects().mapIndexed { i, o ->
            val kind = kindOf(o.getString("kind"))
            ZgLesson(
                id = o.getString("id"),
                number = i + 1,
                section = o.getInt("section"),
                kind = kind,
                title = o.getString("title"),
                gloss = o.optString("gloss"),
                fuse = if (kind == ZgKind.Fuse) o.getJSONArray("items").objects().map { fuseOf(it, base) } else emptyList(),
                matrix = o.optJSONObject("matrix")?.let { matrixOf(it, base) },
            )
        }
        return ZgBookData(sections, lessons)
    }

    private fun kindOf(s: String) = if (s == "matrix") ZgKind.Matrix else ZgKind.Fuse

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    internal fun segsOf(a: JSONArray): List<ZgSeg> = (0 until a.length()).map {
        val p = a.getJSONArray(it)
        ZgSeg(p.getString(0), SegKind.of(p.getString(1)))
    }

    private fun partOf(o: JSONObject) = ZgPart(o.getString("k"), o.getString("kana"), segsOf(o.getJSONArray("ro")))

    internal fun lineOf(o: JSONObject, base: String): ZgLine {
        val audio = o.optString("audio")
        return ZgLine(
            ja = o.getString("ja"),
            pre = o.getString("pre"),
            target = o.getString("target"),
            post = o.getString("post"),
            romaji = o.getString("ro"),
            zh = o.getString("zh"),
            fromAnime = o.optString("src") == "原作",
            audioUrl = if (audio.isBlank()) "" else base + audio,
        )
    }

    internal fun fuseOf(o: JSONObject, base: String) = ZgFuse(
        word = o.getString("word"),
        kana = o.getString("kana"),
        a = partOf(o.getJSONObject("a")),
        b = partOf(o.getJSONObject("b")),
        segs = segsOf(o.getJSONArray("segs")),
        options = o.getJSONArray("options").strings(),
        answer = o.getInt("answer"),
        tags = o.getJSONArray("tags").let { t -> (0 until t.length()).map { t.getJSONArray(it).let { p -> p.getString(0) to SegKind.of(p.getString(1)) } } },
        rule = o.getString("rule"),
        line = lineOf(o.getJSONObject("line"), base),
        ask = o.optString("ask"),
    )

    private fun headOf(o: JSONObject) = ZgHead(o.getString("v"), o.getString("ro"), o.optJSONArray("core")?.strings().orEmpty())

    private fun matrixOf(o: JSONObject, base: String) = ZgMatrix(
        focusRow = o.optString("focus") == "row",
        cols = o.getJSONArray("cols").objects().map(::headOf),
        rows = o.getJSONArray("rows").objects().map(::headOf),
        cells = o.getJSONArray("cells").let { rows ->
            (0 until rows.length()).map { r ->
                rows.getJSONArray(r).objects().map { c ->
                    when (c.getString("s")) {
                        "ok" -> ZgCell.Word(
                            word = c.getString("word"),
                            kana = c.getString("kana"),
                            romaji = c.getString("ro"),
                            front = c.getString("a"),
                            back = c.getString("b"),
                            meaning = c.getString("mean"),
                            line = lineOf(c.getJSONObject("line"), base),
                        )
                        "rare" -> ZgCell.Rare
                        else -> ZgCell.None
                    }
                }
            }
        },
    )
}
