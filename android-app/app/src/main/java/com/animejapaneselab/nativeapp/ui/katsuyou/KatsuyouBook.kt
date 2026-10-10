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
/** [go] = the button that starts the first play ("拼起来试试"). */
data class KyPeek(val ends: List<String>, val result: List<ZgSeg>, val rule: String, val examples: List<KyPeekExample>, val go: String, val glance: KyGlance? = null, val beats: List<String> = emptyList())

/**
 * 課前の一眼, the newer layout (第九巻 first): one hero (before → after, or one marked sentence), two to five short
 * beats, and a pair or two to look at again. Replaces [KyPeek.ends] / [KyPeek.examples] when present.
 */
data class KyGlance(
    val hero: KyGlLine,
    val beats: List<String>,
    val pairs: List<KyGlLine>,
    /** "" = the hero line; "track" = [track] drawn as one line (第十三巻); "mora" = [mora], two words cell by cell. */
    val kind: String = "",
    val track: List<KyGlNode> = emptyList(),
    val mora: KyGlMora? = null,
)

/** A node on the 課前の一眼 track: [at] on / off (strays) / ride (strays along, dashed) / snap (pulled back: 笑); [pivot] = the word it turns on. */
data class KyGlNode(val ja: String, val romaji: String, val tag: String, val at: String, val pivot: String)

/** Two words cut into beats: [aCols] = which of [b]'s cells each of [a]'s sits over; [bKinds] same / extra / tail. */
data class KyGlMora(
    val aWord: String, val aRomaji: String, val a: List<String>, val aCols: List<Int>,
    val bWord: String, val bRomaji: String, val b: List<String>, val bKinds: List<String>,
    val note: String, val zh: String,
)

/** A line in blocks; [to] empty = the line alone with its marks; [zh] = what it says / why. */
data class KyGlLine(val from: List<KyGlSeg>, val to: List<KyGlSeg>, val zh: String)

/** [kind]: same, insert (new), gone (dropped), mark (look here), scope (a stretch). */
data class KyGlSeg(val text: String, val romaji: String, val kind: String)

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

/** 還原台 / 换词 / 时间轴 / 证据 / 接续 / 敬语阶梯 / 换个人说: one answer out of a few, told by [KyPick.head] and [KyPick.layout]. */
data class KyOpt(
    val text: String,
    val romaji: String,
    /** "yes" / "ok" / "no" (接续: 原作 / 也说得通 / 不行); blank = right or wrong only. */
    val mark: String,
    val why: String,
    /** 证据: the column. 敬语: the level label. */
    val group: String,
    /** 换词 / 敬语: the line as it is with this option. */
    val line: ZgLine?,
    /** 第十三巻: the little line shape drawn on the tile (tennen / toboke / kanchigai / bousou). */
    val glyph: String = "",
)

/** ダジャレ: one [sound] (kana, [romaji]) that splits into two words; [hit] is the one the joke takes ("a" / "b"). */
data class KySplit(val sound: String, val romaji: String, val a: Pair<String, String>, val b: Pair<String, String>, val hit: String)

data class KyMark(val kind: String, val at: Int, val until: Int, val label: String)

data class KyPick(
    /** "line" (slot in the line) / "shuku" (short → full) / "timeline" / "context" / "speaker" / "show" (whole line, ask about it) / "listen" (only the voice until answered). */
    val head: String,
    /** "rows" / "chips" / "columns" / "ladder" / "floors" (第十三巻 四层楼). */
    val layout: String,
    val ask: String,
    val line: ZgLine?,
    val options: List<KyOpt>,
    val answer: Int,
    val rule: String,
    val tags: List<Pair<String, SegKind>>,
    val short: List<ZgSeg>,
    val shortRomaji: List<ZgSeg>,
    val full: List<ZgSeg>,
    val fullRomaji: List<ZgSeg>,
    val marks: List<KyMark>,
    val context: String,
    val who: String,
    val rel: String,
    val verdictRight: String,
    val verdictWrong: String,
    val split: KySplit? = null,
    /** 涙: after the pick, an empty slot after the line and a few ways to fill it (not graded): [ja, zh]. */
    val fills: List<Pair<String, String>> = emptyList(),
    /** 涙 まとめ: shown after the pick of this (last) item. */
    val table: KyDiffTable? = null,
) {
    val right: KyOpt get() = options[answer]
}

/** 语速档位: one sentence at four speeds; which one the anime says. */
data class KySpeed(val pre: String, val stops: List<Pair<String, String>>, val answer: Int, val note: String, val line: ZgLine)

/** 活用盘: the verb's ending slides along its 行 to a 段 ([answer] 5 = the [extra] button). */
data class KyDial(
    val verb: String,
    val stem: String,
    val stemRomaji: String,
    val kana: List<String>,
    val romaji: List<String>,
    val rowName: String,
    val cur: Int,
    val answer: Int,
    val form: String,
    val suffix: String,
    val suffixRomaji: String,
    val word: String,
    val tags: List<Pair<String, SegKind>>,
    val rule: String,
    val line: ZgLine,
    val extra: String,
)

/** 分拣: a card goes left or right ([answer] 0 / 1). */
data class KySwipe(val word: String, val romaji: String, val answer: Int, val result: String, val trap: String)

/** 翻牌: think first, then flip; [trap] = the ones that look like the other kind. */
data class KyFlip(val word: String, val romaji: String, val kind: String, val form: String, val note: String, val trap: Boolean)

data class KyBlock(val id: String, val rank: Int, val fin: Pair<String, String>, val mid: Pair<String, String>, val past: Pair<String, String>?, val gloss: String, val seam: String)

data class KyStackGoal(val goal: String, val need: String, val line: ZgLine)

/** 叠积木: blocks stacked in rank order onto [base]; [meanings] by the ids stacked ("srt"). */
data class KyStack(val base: Pair<String, String>, val baseNote: String, val blocks: List<KyBlock>, val goals: List<KyStackGoal>, val meanings: Map<String, String>)

/** 找错: a sentence in pieces, one of them wrong ([bad]); [fix] is what it should be. */
data class KySpot(
    val toks: List<Pair<String, String>>,
    val bad: Int,
    val fix: Pair<String, String>,
    val why: String,
    val zh: String,
    val line: ZgLine?,
) {
    val fixed: String get() = toks.mapIndexed { i, t -> if (i == bad) fix.first else t.first }.joinToString("")
}

/**
 * 括る / は的地盘 (第九巻): a sentence in bunsetsu blocks, one block ([anchor]) fixed. [dir] "start" = the anchor is the
 * noun being modified and you find where its modifier starts ([answer] = first block of the clause, which ends right
 * before the anchor); "end" = the anchor is a 〜は and you find the last block it governs ([answer]).
 * [gap] is the 补洞 restoration (内の関係), [tag] the 内 / 外 label.
 */
data class KySpan(
    val toks: List<Pair<String, String>>,
    val anchor: Int,
    val dir: String,
    val answer: Int,
    val ask: String,
    val why: String,
    val zh: String,
    val gap: String,
    val tag: String,
) {
    val first: Int get() = if (dir == "start") answer else anchor
    val lastBlock: Int get() = if (dir == "start") anchor - 1 else answer
    val text: String get() = toks.joinToString("") { it.first }
}

/** One numbered sentence of a passage; [role] is shown after the answer ("让步"), [mark] is the signal word in it. */
data class KySent(
    val ja: String,
    val ro: String,
    val zh: String,
    val role: String,
    val mark: String,
    /** 对话段 (第十三巻): who says it, its clip from the anime (may be blank). */
    val who: String = "",
    val audioUrl: String = "",
    /** tension: 0–3, or -1 when the passage has none. */
    val lv: Float = -1f,
    /** track: "on" the line of common sense, "off" (it strays), "snap" (where it's pulled back: the laugh). */
    val at: String = "",
)

/** 主张はどこ: a passage in numbered sentences, tap the one [ask] is about. */
/** [style]: "" = the 読解 list, "track" = one line down the left that strays and snaps back, "tension" = a tension curve on top. */
data class KyPassage(val sents: List<KySent>, val ask: String, val answer: Int, val why: String, val rule: String, val style: String = "")

/** One answer of a 模擬問題 / 毒を見抜く; [type] = what is wrong with it ("" = it is right), [poison] = the words that are the poison. */
data class KyJudgeOpt(val text: String, val ok: Boolean, val type: String, val why: String, val poison: String)

/**
 * 模擬問題 (no [types]: pick the right one of the options) / 毒を見抜く ([types] set: the one option is shown alone and
 * you name what is wrong with it; its [KyJudgeOpt.type] is the answer, "" type = "没毒"). [src] is the passage (collapsed
 * unless [open]), [ev] the sentences that are the evidence.
 */
data class KyJudge(
    val src: List<String>,
    val srcLabel: String,
    val open: Boolean,
    val stem: String,
    val options: List<KyJudgeOpt>,
    val answer: Int,
    val types: List<String>,
    val ev: List<Int>,
    val rule: String,
) {
    val claim: Boolean get() = types.isNotEmpty()
}

/**
 * 第十四巻 涙「平时 / 这一刻」: a grey [ghost] card of how this person usually talks (with a count), the line of
 * this moment in front cut into [blocks]; tap the block that is different. [also] = other blocks that differ too
 * (another 課's axis, shown after the pick). [fig] = a small figure in the shape of the point (stairs / floors / arc).
 */
data class KyDiff(
    val context: String,
    val ghostLabel: String,
    val ghostLines: List<String>,
    val ghostMark: String,
    val ghostCount: String,
    val nowLabel: String,
    val who: String,
    val blocks: List<Pair<String, String>>,
    val ja: String,
    val zh: String,
    val audioUrl: String,
    val nowCount: String,
    val answer: Int,
    val also: Map<Int, String>,
    val ask: String,
    val why: String,
    val rule: String,
    val fig: KyFig?,
)

/**
 * The small figure under a 涙 card. stairs: [steps] low → high, a jump from [from] to [to]. floors: [top] on the
 * upper floor, [bottom] below, [drop] the note on the fall, [labels] the two floors. arc: [a] → [b] over [mid], [note] under it.
 */
data class KyFig(
    val kind: String,
    val title: String,
    val steps: List<String> = emptyList(),
    val from: Int = 0,
    val to: Int = 0,
    val top: String = "",
    val bottom: List<String> = emptyList(),
    val drop: String = "",
    val labels: List<String> = emptyList(),
    val a: String = "",
    val b: String = "",
    val mid: String = "",
    val note: String = "",
)

/** 涙 まとめ: a scene line by line against the axes it turns over ([cols]); [flags] per row, one per col. */
data class KyDiffTable(val title: String, val cols: List<String>, val rows: List<Triple<String, String, List<Boolean>>>, val note: String)

/** 连线: left halves (pre to connective) to right halves; [match] = the left index of each right. */
data class KyConnect(val left: List<Pair<String, String>>, val right: List<String>, val match: List<Int>, val lines: List<ZgLine>)

sealed interface KyStep {
    val title: String
    val count: Int
    data class Fuse(override val title: String, val items: List<ZgFuse>) : KyStep { override val count get() = items.size }
    data class Back(override val title: String, val items: List<KyBack>) : KyStep { override val count get() = items.size }
    data class Pick(override val title: String, val items: List<KyPick>) : KyStep { override val count get() = items.size }
    data class Speed(override val title: String, val items: List<KySpeed>) : KyStep { override val count get() = items.size }
    data class Dial(override val title: String, val items: List<KyDial>) : KyStep { override val count get() = items.size }
    data class Swipe(override val title: String, val left: String, val right: String, val items: List<KySwipe>) : KyStep { override val count get() = items.size }
    data class Flip(override val title: String, val ask: String, val items: List<KyFlip>) : KyStep { override val count get() = items.size }
    data class Stack(override val title: String, val stack: KyStack) : KyStep { override val count get() = stack.goals.size }
    data class Connect(override val title: String, val connect: KyConnect) : KyStep { override val count get() = connect.left.size }
    data class Spot(override val title: String, val items: List<KySpot>) : KyStep { override val count get() = items.size }
    data class Span(override val title: String, val items: List<KySpan>) : KyStep { override val count get() = items.size }
    data class Passage(override val title: String, val items: List<KyPassage>) : KyStep { override val count get() = items.size }
    data class Judge(override val title: String, val items: List<KyJudge>) : KyStep { override val count get() = items.size }
    data class Diff(override val title: String, val items: List<KyDiff>) : KyStep { override val count get() = items.size }
}

data class KyLesson(val point: String, val peek: KyPeek?, val steps: List<KyStep>) {
    val count: Int get() = steps.sumOf { it.count }

    /** A few words for the つづく preview of this 課. */
    val preview: List<String>
        get() = steps.flatMap { s ->
            when (s) {
                is KyStep.Fuse -> s.items.map { it.word }
                is KyStep.Dial -> s.items.map { it.word }
                is KyStep.Pick -> s.items.map { it.right.text }
                is KyStep.Back -> s.items.map { it.te }
                is KyStep.Speed -> s.items.map { it.line.target }
                is KyStep.Spot -> s.items.map { it.fix.first }
                is KyStep.Span -> s.items.map { it.toks[it.anchor].first }
                is KyStep.Diff -> s.items.map { it.blocks[it.answer].first }
                else -> emptyList()
            }
        }.distinct().take(4)
}

/** まとめ as a table (every book but B): columns, rows in sections, cells; maybe a cover or a selection that shows lines. */
data class KyCell(val text: String, val sub: String, val segs: List<ZgSeg>, val mark: String)

data class KyRow(val label: String, val labelSub: String, val cells: List<KyCell>, val lines: List<ZgLine>)

/** [rail] = the rows are an order (drawn as a rail of dots). */
data class KySection(val title: String, val rail: Boolean, val rows: List<KyRow>)

data class KyTable(
    val title: String,
    /** Header per column: text, romaji. */
    val cols: List<Pair<String, String>>,
    val sections: List<KySection>,
    /** "col" = tabs pick a column (its [colNotes] / [colLines] show), "row" = a row shows its lines, "" = nothing to pick. */
    val select: String,
    val colNotes: List<String>,
    val colLines: List<ZgLine?>,
    /** Columns 遮る hides (a hidden cell shows when tapped). */
    val cover: List<Int>,
    /** Footnotes: label, text, note. */
    val notes: List<Triple<String, String, String>>,
)

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
    val table: KyTable? = null,
) {
    val hasSummary: Boolean get() = rows.isNotEmpty() || table != null
    val summaryTitle: String get() = table?.title ?: mapTitle
}

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
                table = b.optJSONObject("table")?.let { tableOf(it, base) },
            )
        })
    }

    internal fun lessonOf(o: JSONObject, base: String) = KyLesson(
        point = o.getString("point"),
        peek = o.optJSONObject("peek")?.let { p ->
            KyPeek(
                ends = p.getJSONArray("ends").strings(),
                result = ZougoBook.segsOf(p.getJSONArray("result")),
                rule = p.getString("rule"),
                go = p.optString("go").ifBlank { "拼起来试试" },
                examples = p.getJSONArray("examples").objects().map {
                    KyPeekExample(it.getString("base"), it.getString("stem"), ZougoBook.segsOf(it.getJSONArray("tail")), it.getString("ro"))
                },
                beats = p.optJSONArray("beats")?.strings().orEmpty(),
                glance = p.optJSONObject("glance")?.let { g ->
                    KyGlance(
                        hero = glLineOf(g.getJSONObject("hero")),
                        beats = g.getJSONArray("beats").strings(),
                        pairs = g.optJSONArray("pairs")?.objects()?.map(::glLineOf).orEmpty(),
                        kind = g.optString("kind"),
                        track = g.optJSONArray("track")?.objects()?.map {
                            KyGlNode(it.getString("ja"), it.optString("ro"), it.optString("tag"), it.optString("at"), it.optString("pivot"))
                        }.orEmpty(),
                        mora = g.optJSONObject("mora")?.let { m ->
                            val a = m.getJSONObject("a")
                            val b = m.getJSONObject("b")
                            val ac = a.getJSONArray("cells").let { c -> (0 until c.length()).map { c.getJSONArray(it) } }
                            val bc = b.getJSONArray("cells").let { c -> (0 until c.length()).map { c.getJSONArray(it) } }
                            KyGlMora(
                                a.getString("word"), a.optString("ro"), ac.map { it.getString(0) }, ac.map { it.getInt(1) },
                                b.getString("word"), b.optString("ro"), bc.map { it.getString(0) }, bc.map { it.getString(1) },
                                m.optString("note"), m.optString("zh"),
                            )
                        },
                    )
                },
            )
        },
        steps = o.getJSONArray("steps").objects().map { stepOf(it, base) },
    )

    private fun glLineOf(o: JSONObject): KyGlLine {
        fun segs(a: JSONArray?) = (0 until (a?.length() ?: 0)).map { i ->
            a!!.getJSONArray(i).let { KyGlSeg(it.getString(0), it.getString(1), it.getString(2)) }
        }
        return KyGlLine(segs(o.getJSONArray("from")), segs(o.optJSONArray("to")), o.optString("zh"))
    }

    private fun stepOf(o: JSONObject, base: String): KyStep {
        val title = o.optString("title")
        val items = o.optJSONArray("items")?.objects().orEmpty()
        return when (o.getString("type")) {
            "fuse" -> KyStep.Fuse(title, items.map { ZougoBook.fuseOf(it, base) })
            "back" -> KyStep.Back(title, items.map { backOf(it, base) })
            "pick" -> KyStep.Pick(title, items.map { pickOf(it, base) })
            "speed" -> KyStep.Speed(title, items.map {
                KySpeed(it.getString("pre"), it.getJSONArray("stops").pairs(), it.getInt("answer"), it.getString("note"), ZougoBook.lineOf(it.getJSONObject("line"), base))
            })
            "dial" -> KyStep.Dial(title, items.map {
                KyDial(
                    verb = it.getString("verb"), stem = it.getString("stem"), stemRomaji = it.getString("stemRo"),
                    kana = it.getJSONArray("kana").strings(), romaji = it.getJSONArray("ro").strings(), rowName = it.getString("rowName"),
                    cur = it.getInt("cur"), answer = it.getInt("answer"), form = it.getString("form"),
                    suffix = it.getString("suf"), suffixRomaji = it.getString("sufRo"), word = it.getString("word"),
                    tags = tagsOf(it), rule = it.getString("rule"), line = ZougoBook.lineOf(it.getJSONObject("line"), base),
                    extra = it.optString("extra"),
                )
            })
            "swipe" -> KyStep.Swipe(title, o.getString("left"), o.getString("right"), items.map {
                KySwipe(it.getString("w"), it.getString("ro"), it.getInt("answer"), it.getString("result"), it.optString("trap"))
            })
            "flip" -> KyStep.Flip(title, o.getString("ask"), items.map {
                KyFlip(it.getString("w"), it.getString("ro"), it.getString("kind"), it.getString("form"), it.optString("note"), it.optBoolean("trap"))
            })
            "stack" -> KyStep.Stack(title, o.getJSONObject("stack").let { k ->
                KyStack(
                    base = k.getJSONArray("base").pair(),
                    baseNote = k.getString("baseNote"),
                    blocks = k.getJSONArray("blocks").objects().map {
                        KyBlock(it.getString("id"), it.getInt("rank"), it.getJSONArray("fin").pair(), it.getJSONArray("mid").pair(),
                            it.optJSONArray("past")?.pair(), it.getString("gloss"), it.getString("seam"))
                    },
                    goals = k.getJSONArray("goals").objects().map { KyStackGoal(it.getString("goal"), it.getString("need"), ZougoBook.lineOf(it.getJSONObject("line"), base)) },
                    meanings = k.getJSONObject("meanings").let { m -> m.keys().asSequence().associateWith { m.getString(it) } },
                )
            })
            "connect" -> KyStep.Connect(title, o.getJSONObject("connect").let { k ->
                KyConnect(
                    left = k.getJSONArray("left").pairs(),
                    right = k.getJSONArray("right").strings(),
                    match = k.getJSONArray("match").let { a -> (0 until a.length()).map { a.getInt(it) } },
                    lines = k.getJSONArray("lines").objects().map { ZougoBook.lineOf(it, base) },
                )
            })
            "spot" -> KyStep.Spot(title, items.map {
                KySpot(
                    toks = it.getJSONArray("toks").pairs(),
                    bad = it.getInt("bad"),
                    fix = it.getJSONArray("fix").pair(),
                    why = it.getString("why"),
                    zh = it.optString("zh"),
                    line = it.optJSONObject("line")?.let { l -> ZougoBook.lineOf(l, base) },
                )
            })
            "span" -> KyStep.Span(title, items.map {
                KySpan(
                    toks = it.getJSONArray("toks").pairs(),
                    anchor = it.getInt("anchor"),
                    dir = it.optString("dir", "start"),
                    answer = it.getInt("answer"),
                    ask = it.optString("ask"),
                    why = it.getString("why"),
                    zh = it.optString("zh"),
                    gap = it.optString("gap"),
                    tag = it.optString("tag"),
                )
            })
            "passage" -> KyStep.Passage(title, items.map {
                KyPassage(
                    sents = it.getJSONArray("sents").objects().map { s ->
                        KySent(
                            s.getString("ja"), s.optString("ro"), s.optString("zh"), s.optString("role"), s.optString("mark"),
                            who = s.optString("who"),
                            audioUrl = s.optString("audio").let { a -> if (a.isBlank()) "" else base + a },
                            lv = s.optDouble("lv", -1.0).toFloat(),
                            at = s.optString("at"),
                        )
                    },
                    ask = it.getString("ask"),
                    answer = it.getInt("answer"),
                    why = it.optString("why"),
                    rule = it.optString("rule"),
                    style = it.optString("style"),
                )
            })
            "judge" -> KyStep.Judge(title, items.map {
                KyJudge(
                    src = it.getJSONArray("src").strings(),
                    srcLabel = it.optString("srcLabel"),
                    open = it.optBoolean("open"),
                    stem = it.getString("stem"),
                    options = it.getJSONArray("options").objects().map { o ->
                        KyJudgeOpt(o.getString("t"), o.optBoolean("ok"), o.optString("type"), o.optString("why"), o.optString("poison"))
                    },
                    answer = it.getInt("answer"),
                    types = it.optJSONArray("types")?.strings().orEmpty(),
                    ev = it.optJSONArray("ev")?.let { a -> (0 until a.length()).map { i -> a.getInt(i) } }.orEmpty(),
                    rule = it.optString("rule"),
                )
            })
            "diff" -> KyStep.Diff(title, items.map { diffOf(it, base) })
            else -> KyStep.Pick(title, emptyList())
        }
    }

    private fun diffOf(o: JSONObject, base: String): KyDiff {
        val g = o.getJSONObject("ghost")
        val n = o.getJSONObject("now")
        val audio = n.optString("audio")
        return KyDiff(
            context = o.optString("ctx"),
            ghostLabel = g.optString("label"),
            ghostLines = g.getJSONArray("lines").strings(),
            ghostMark = g.optString("mark"),
            ghostCount = g.optString("count"),
            nowLabel = n.optString("label"),
            who = n.optString("who"),
            blocks = n.getJSONArray("blocks").pairs(),
            ja = n.getString("ja"),
            zh = n.optString("zh"),
            audioUrl = if (audio.isBlank()) "" else base + audio,
            nowCount = n.optString("count"),
            answer = o.getInt("answer"),
            also = o.optJSONObject("also")?.let { a -> a.keys().asSequence().associate { it.toInt() to a.getString(it) } }.orEmpty(),
            ask = o.optString("ask"),
            why = o.optString("why"),
            rule = o.optString("rule"),
            fig = o.optJSONObject("fig")?.let(::figOf),
        )
    }

    private fun figOf(f: JSONObject) = KyFig(
        kind = f.getString("kind"),
        title = f.optString("title"),
        steps = f.optJSONArray("steps")?.strings().orEmpty(),
        from = f.optInt("from"),
        to = f.optInt("to"),
        top = f.optString("top"),
        bottom = f.optJSONArray("bottom")?.strings().orEmpty(),
        drop = f.optString("drop"),
        labels = f.optJSONArray("labels")?.strings().orEmpty(),
        a = f.optString("a"),
        b = f.optString("b"),
        mid = f.optString("mid"),
        note = f.optString("note"),
    )

    private fun diffTableOf(t: JSONObject) = KyDiffTable(
        title = t.optString("title"),
        cols = t.getJSONArray("cols").strings(),
        rows = t.getJSONArray("rows").objects().map { r ->
            val f = r.getJSONArray("flags")
            Triple(r.getString("ja"), r.optString("zh"), (0 until f.length()).map { f.getBoolean(it) })
        },
        note = t.optString("note"),
    )

    private fun backOf(it: JSONObject, base: String) = KyBack(
        te = it.getString("te"),
        romaji = it.getString("ro"),
        stem = it.getString("stem"),
        stemRomaji = it.getString("stemRo"),
        bins = it.getJSONArray("bins").pairs(),
        answer = it.getInt("answer"),
        note = it.getString("note"),
        line = ZougoBook.lineOf(it.getJSONObject("line"), base),
    )

    private fun pickOf(o: JSONObject, base: String) = KyPick(
        head = o.optString("head", "line"),
        layout = o.optString("layout", "rows"),
        ask = o.optString("ask"),
        line = o.optJSONObject("line")?.let { ZougoBook.lineOf(it, base) },
        options = o.getJSONArray("options").objects().map {
            KyOpt(it.getString("t"), it.optString("ro"), it.optString("mark"), it.optString("why"), it.optString("group"),
                it.optJSONObject("line")?.let { l -> ZougoBook.lineOf(l, base) }, it.optString("glyph"))
        },
        answer = o.getInt("answer"),
        rule = o.optString("rule"),
        tags = tagsOf(o),
        short = o.optJSONArray("sk")?.let { ZougoBook.segsOf(it) }.orEmpty(),
        shortRomaji = o.optJSONArray("sr")?.let { ZougoBook.segsOf(it) }.orEmpty(),
        full = o.optJSONArray("fk")?.let { ZougoBook.segsOf(it) }.orEmpty(),
        fullRomaji = o.optJSONArray("fr")?.let { ZougoBook.segsOf(it) }.orEmpty(),
        marks = o.optJSONArray("marks")?.objects()?.map { KyMark(it.getString("kind"), it.optInt("at", it.optInt("a")), it.optInt("b"), it.getString("label")) }.orEmpty(),
        context = o.optString("ctx"),
        who = o.optString("who"),
        rel = o.optString("rel"),
        verdictRight = o.optString("vr"),
        verdictWrong = o.optString("vw"),
        split = o.optJSONObject("split")?.let { sp ->
            fun pair(k: String) = sp.getJSONArray(k).let { it.getString(0) to it.getString(1) }
            KySplit(sp.getString("sound"), sp.optString("ro"), pair("a"), pair("b"), sp.optString("hit", "b"))
        },
        fills = o.optJSONArray("fills")?.pairs().orEmpty(),
        table = o.optJSONObject("table")?.let(::diffTableOf),
    )

    internal fun tableOf(o: JSONObject, base: String) = KyTable(
        title = o.getString("title"),
        cols = o.getJSONArray("cols").pairs(),
        sections = o.getJSONArray("sections").objects().map { sec ->
            KySection(sec.optString("title"), sec.optBoolean("rail"), sec.getJSONArray("rows").objects().map { r ->
                KyRow(
                    label = r.optString("label"),
                    labelSub = r.optString("sub"),
                    cells = r.getJSONArray("cells").objects().map { c ->
                        KyCell(c.optString("t"), c.optString("sub"), c.optJSONArray("segs")?.let { ZougoBook.segsOf(it) }.orEmpty(), c.optString("mark"))
                    },
                    lines = r.optJSONArray("lines")?.objects()?.map { ZougoBook.lineOf(it, base) }.orEmpty(),
                )
            })
        },
        select = o.optString("select"),
        colNotes = o.optJSONArray("colNotes")?.strings().orEmpty(),
        colLines = o.optJSONArray("colLines")?.let { a -> (0 until a.length()).map { i -> a.optJSONObject(i)?.let { ZougoBook.lineOf(it, base) } } }.orEmpty(),
        cover = o.optJSONArray("cover")?.let { a -> (0 until a.length()).map { a.getInt(it) } }.orEmpty(),
        notes = o.optJSONArray("notes")?.let { a -> (0 until a.length()).map { i -> a.getJSONArray(i).let { Triple(it.getString(0), it.getString(1), it.optString(2)) } } }.orEmpty(),
    )

    private fun tagsOf(o: JSONObject): List<Pair<String, SegKind>> =
        o.optJSONArray("tags")?.let { t -> (0 until t.length()).map { t.getJSONArray(it).let { p -> p.getString(0) to SegKind.of(p.getString(1)) } } }.orEmpty()

    private fun JSONArray.pair(): Pair<String, String> = getString(0) to getString(1)

    private fun JSONArray.pairs(): List<Pair<String, String>> = (0 until length()).map { getJSONArray(it).pair() }

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
