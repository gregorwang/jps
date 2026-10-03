package com.animejapaneselab.nativeapp.ui.katsuyou

import android.content.Context
import com.animejapaneselab.nativeapp.ui.zougo.SegKind
import com.animejapaneselab.nativeapp.ui.zougo.ZgFuse
import com.animejapaneselab.nativeapp.ui.zougo.ZgLine
import com.animejapaneselab.nativeapp.ui.zougo.ZgSeg
import com.animejapaneselab.nativeapp.ui.zougo.ZougoBook
import org.json.JSONArray
import org.json.JSONObject

/*
 * The 活用 教科書 (VOL.A–H) rebuilt the 造語 way: a 課 is played, not read. Content per 課 is keyed by the
 * point id of `conjugation_lessons.json`, so 已学 / 練習 / 今日 keep working off ConjugationDrillViewModel.
 * `assets/katsuyou_lessons.json` is written by archive-content-sources/conjugation-rebuild/build_katsuyou.py,
 * one book at a time (VOL.B 音便 first). A 課 not in it still opens the old 板書 + 台詞 sitting.
 */

/** 課前の一眼: the endings, what they turn into, one rule line and two examples. */
data class KyPeek(val ends: List<String>, val result: List<ZgSeg>, val rule: String, val examples: List<KyPeekExample>)

data class KyPeekExample(val base: String, val stem: String, val tail: List<ZgSeg>, val romaji: String)

/** 倒推: a て形 from a line → which dictionary form ([bins] = endings, kana to romaji). */
data class KyBack(
    val te: String,
    val romaji: String,
    val stem: String,
    val stemRomaji: String,
    val bins: List<Pair<String, String>>,
    val answer: Int,
    val note: String,
    val line: ZgLine,
) {
    val base: String get() = stem + bins[answer].first
}

data class KyLesson(val point: String, val peek: KyPeek?, val fuse: List<ZgFuse>, val back: List<KyBack>)

/** One row of the まとめ map: endings → what they turn into, and the verbs played in the book. */
data class KyMapRow(
    val ends: List<Pair<String, String>>,
    val mid: ZgSeg,
    val midRomaji: String,
    val te: ZgSeg,
    val teRomaji: String,
    val name: String,
    /** base, て形, romaji of the て形. */
    val verbs: List<Triple<String, String, String>>,
)

data class KyBook(
    /** The drill group's key letter ("B"). */
    val group: String,
    /** How this book is played, for the 目次 chip ("拼合"). */
    val play: String,
    val mapTitle: String,
    val mapFootnote: Pair<String, String>?,
    val rows: List<KyMapRow>,
    val lessons: List<KyLesson>,
)

data class KyData(val books: List<KyBook>) {
    fun lesson(point: String): KyLesson? = books.firstNotNullOfOrNull { b -> b.lessons.firstOrNull { it.point == point } }
    fun book(groupKey: String): KyBook? = books.firstOrNull { it.group == groupKey }
    fun bookOf(point: String): KyBook? = books.firstOrNull { b -> b.lessons.any { it.point == point } }
}

object KatsuyouBook {
    private const val Asset = "katsuyou_lessons.json"
    @Volatile private var cached: KyData? = null

    fun load(context: Context): KyData {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: runCatching {
                parse(context.applicationContext.assets.open(Asset).bufferedReader().use { it.readText() })
            }.getOrElse { KyData(emptyList()) }.also { cached = it }
        }
    }

    internal fun parse(raw: String): KyData {
        val top = JSONObject(raw)
        val base = top.optString("audioBase")
        return KyData(top.getJSONArray("books").objects().map { b ->
            val map = b.optJSONObject("map")
            KyBook(
                group = b.getString("group"),
                play = b.optString("play"),
                mapTitle = map?.optString("title").orEmpty(),
                mapFootnote = map?.let { m -> m.optString("ichidan").takeIf { it.isNotBlank() }?.let { it to m.optString("note") } },
                rows = b.optJSONArray("rows")?.objects()?.map(::rowOf).orEmpty(),
                lessons = b.getJSONArray("lessons").objects().map { lessonOf(it, base) },
            )
        })
    }

    private fun lessonOf(o: JSONObject, base: String) = KyLesson(
        point = o.getString("point"),
        peek = o.optJSONObject("peek")?.let { p ->
            KyPeek(
                ends = p.getJSONArray("ends").strings(),
                result = ZougoBook.segsOf(p.getJSONArray("result")),
                rule = p.getString("rule"),
                examples = p.getJSONArray("examples").objects().map {
                    KyPeekExample(it.getString("base"), it.getString("stem"), ZougoBook.segsOf(it.getJSONArray("tail")), it.getString("ro"))
                },
            )
        },
        fuse = o.getJSONArray("fuse").objects().map { ZougoBook.fuseOf(it, base) },
        back = o.optJSONArray("back")?.objects()?.map {
            KyBack(
                te = it.getString("te"),
                romaji = it.getString("ro"),
                stem = it.getString("stem"),
                stemRomaji = it.getString("stemRo"),
                bins = it.getJSONArray("bins").let { a -> (0 until a.length()).map { i -> a.getJSONArray(i).let { p -> p.getString(0) to p.getString(1) } } },
                answer = it.getInt("answer"),
                note = it.getString("note"),
                line = ZougoBook.lineOf(it.getJSONObject("line"), base),
            )
        }.orEmpty(),
    )

    private fun rowOf(o: JSONObject): KyMapRow {
        val mid = o.getJSONArray("mid")
        val te = o.getJSONArray("te")
        return KyMapRow(
            ends = o.getJSONArray("ends").let { a -> (0 until a.length()).map { i -> a.getJSONArray(i).let { it.getString(0) to it.getString(1) } } },
            mid = ZgSeg(mid.getString(0), SegKind.of(mid.getString(2))),
            midRomaji = mid.getString(1),
            te = ZgSeg(te.getString(0), SegKind.of(te.getString(2))),
            teRomaji = te.getString(1),
            name = o.getString("name"),
            verbs = o.getJSONArray("verbs").let { a -> (0 until a.length()).map { i -> a.getJSONArray(i).let { Triple(it.getString(0), it.getString(1), it.getString(2)) } } },
        )
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
