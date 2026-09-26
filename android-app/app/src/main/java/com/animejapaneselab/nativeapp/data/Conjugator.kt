package com.animejapaneselab.nativeapp.data

/**
 * Rule-based 活用表 for dictionary entries: derives the common forms of a verb or adjective from
 * its dictionary form (+ kana reading and part of speech). No data needed, so every vocab row
 * gets a table. Returns null for anything it cannot place with confidence (expressions, nouns,
 * words not in dictionary form).
 */
data class ConjugationTable(
    /** 五段动词 · カ行 / 一段动词 / サ变动词 / い形容词 / な形容词 */
    val typeLabel: String,
    val forms: List<ConjugatedForm>,
)

data class ConjugatedForm(val label: String, val value: String)

object Conjugator {
    private val UToA = mapOf('う' to 'わ', 'く' to 'か', 'ぐ' to 'が', 'す' to 'さ', 'つ' to 'た', 'ぬ' to 'な', 'ぶ' to 'ば', 'む' to 'ま', 'る' to 'ら')
    private val UToI = mapOf('う' to 'い', 'く' to 'き', 'ぐ' to 'ぎ', 'す' to 'し', 'つ' to 'ち', 'ぬ' to 'に', 'ぶ' to 'び', 'む' to 'み', 'る' to 'り')
    private val UToE = mapOf('う' to 'え', 'く' to 'け', 'ぐ' to 'げ', 'す' to 'せ', 'つ' to 'て', 'ぬ' to 'ね', 'ぶ' to 'べ', 'む' to 'め', 'る' to 'れ')
    private val UToO = mapOf('う' to 'お', 'く' to 'こ', 'ぐ' to 'ご', 'す' to 'そ', 'つ' to 'と', 'ぬ' to 'の', 'ぶ' to 'ぼ', 'む' to 'も', 'る' to 'ろ')
    private val RowName = mapOf('う' to "ワ行", 'く' to "カ行", 'ぐ' to "ガ行", 'す' to "サ行", 'つ' to "タ行", 'ぬ' to "ナ行", 'ぶ' to "バ行", 'む' to "マ行", 'る' to "ラ行")

    private val IRow = "いきぎしじちぢにひびぴみりゐ".toSet()
    private val ERow = "えけげせぜてでねへべぺめれゑ".toSet()

    /** Godan verbs that look like ichidan (-iru / -eru). Matched on the end of the surface. */
    private val GodanRuExceptions = listOf(
        "帰る", "還る", "入る", "走る", "知る", "切る", "要る", "限る", "喋る", "減る", "蹴る", "滑る", "握る",
        "参る", "焦る", "散る", "照る", "練る", "嘲る", "覆る", "遮る", "茂る", "湿る", "陥る", "捻る", "罵る",
        "蘇る", "甦る", "混じる", "交じる", "詰る", "耽る", "翻る", "阿る", "しゃべる", "かえる", "はいる",
    )

    /** Honorific godan verbs whose ます / 命令 forms drop the り. */
    private val HonorificGodan = listOf("いらっしゃる", "おっしゃる", "なさる", "くださる", "下さる", "ござる")

    fun tableFor(surface: String, reading: String, partOfSpeech: String): ConjugationTable? {
        val word = surface.trim()
        val kana = reading.trim().ifBlank { word }
        if (word.isEmpty() || word.any { it.isWhitespace() }) return null
        val pos = partOfSpeech.trim().lowercase()
        return when {
            pos.contains("サ変") || pos.contains("する動") || pos.contains("する动") ||
                pos == "名詞/動詞" || pos == "noun/verb" -> suru(if (word.endsWith("する")) word.dropLast(2) else word)
            pos.startsWith("動") || pos.startsWith("动") || pos.startsWith("verb") -> verb(word, kana)
            pos.contains("形容動") || pos.contains("形動") || pos.startsWith("na-adj") -> naAdjective(word)
            pos.startsWith("形容") || pos.startsWith("i-adj") -> iAdjective(word)
            else -> null
        }
    }

    private fun verb(word: String, kana: String): ConjugationTable? {
        val last = word.last()
        if (last !in UToA) return null
        if (word == "する" || (word.endsWith("する") && word.length > 2)) return suru(word.dropLast(2))
        if (word == "来る" || word == "くる") return kuru(word)
        val kanaLast = kana.lastOrNull()
        val beforeRu = if (kanaLast == 'る' && kana.length >= 2) kana[kana.length - 2] else null
        val godanException = GodanRuExceptions.any { word.endsWith(it) }
        return if (last == 'る' && beforeRu != null && (beforeRu in IRow || beforeRu in ERow) && !godanException) {
            ichidan(word)
        } else {
            godan(word)
        }
    }

    private fun godan(word: String): ConjugationTable {
        val stem = word.dropLast(1)
        val u = word.last()
        val a = stem + UToA.getValue(u)
        val i = stem + UToI.getValue(u)
        val e = stem + UToE.getValue(u)
        val o = stem + UToO.getValue(u)
        val iku = word.endsWith("行く") || word == "いく"
        val te = when {
            iku -> stem + "って"
            word == "問う" || word == "とう" -> stem + "うて"
            u == 'う' || u == 'つ' || u == 'る' -> stem + "って"
            u == 'く' -> stem + "いて"
            u == 'ぐ' -> stem + "いで"
            u == 'す' -> stem + "して"
            else -> stem + "んで"
        }
        val ta = te.dropLast(1) + if (te.endsWith("で")) "だ" else "た"
        val honorific = HonorificGodan.any { word.endsWith(it) }
        val masu = if (honorific) "${stem}います" else "${i}ます"
        val nai = if (word == "ある") "ない" else "${a}ない"
        return ConjugationTable(
            typeLabel = "五段动词 · ${RowName.getValue(u)}",
            forms = listOf(
                ConjugatedForm("ます形", masu),
                ConjugatedForm("ない形", nai),
                ConjugatedForm("て形", te),
                ConjugatedForm("た形", ta),
                ConjugatedForm("可能", "${e}る"),
                ConjugatedForm("受身", "${a}れる"),
                ConjugatedForm("使役", "${a}せる"),
                ConjugatedForm("意向", "${o}う"),
                ConjugatedForm("命令", if (honorific) "${stem}い" else e),
                ConjugatedForm("仮定", "${e}ば"),
            ),
        )
    }

    private fun ichidan(word: String): ConjugationTable {
        val stem = word.dropLast(1)
        return ConjugationTable(
            typeLabel = "一段动词",
            forms = listOf(
                ConjugatedForm("ます形", "${stem}ます"),
                ConjugatedForm("ない形", "${stem}ない"),
                ConjugatedForm("て形", "${stem}て"),
                ConjugatedForm("た形", "${stem}た"),
                ConjugatedForm("可能", "${stem}られる"),
                ConjugatedForm("受身", "${stem}られる"),
                ConjugatedForm("使役", "${stem}させる"),
                ConjugatedForm("意向", "${stem}よう"),
                ConjugatedForm("命令", "${stem}ろ"),
                ConjugatedForm("仮定", "${stem}れば"),
            ),
        )
    }

    private fun suru(noun: String): ConjugationTable = ConjugationTable(
        typeLabel = "サ变动词",
        forms = listOf(
            ConjugatedForm("ます形", "${noun}します"),
            ConjugatedForm("ない形", "${noun}しない"),
            ConjugatedForm("て形", "${noun}して"),
            ConjugatedForm("た形", "${noun}した"),
            ConjugatedForm("可能", "${noun}できる"),
            ConjugatedForm("受身", "${noun}される"),
            ConjugatedForm("使役", "${noun}させる"),
            ConjugatedForm("意向", "${noun}しよう"),
            ConjugatedForm("命令", "${noun}しろ"),
            ConjugatedForm("仮定", "${noun}すれば"),
        ),
    )

    private fun kuru(word: String): ConjugationTable {
        val kanji = word == "来る"
        fun k(kanjiForm: String, kanaForm: String) = if (kanji) kanjiForm else kanaForm
        return ConjugationTable(
            typeLabel = "カ变动词",
            forms = listOf(
                ConjugatedForm("ます形", k("来ます（きます）", "きます")),
                ConjugatedForm("ない形", k("来ない（こない）", "こない")),
                ConjugatedForm("て形", k("来て（きて）", "きて")),
                ConjugatedForm("た形", k("来た（きた）", "きた")),
                ConjugatedForm("可能", k("来られる（こられる）", "こられる")),
                ConjugatedForm("受身", k("来られる", "こられる")),
                ConjugatedForm("使役", k("来させる（こさせる）", "こさせる")),
                ConjugatedForm("意向", k("来よう（こよう）", "こよう")),
                ConjugatedForm("命令", k("来い（こい）", "こい")),
                ConjugatedForm("仮定", k("来れば（くれば）", "くれば")),
            ),
        )
    }

    private fun iAdjective(word: String): ConjugationTable? {
        if (!word.endsWith("い")) return null
        // いい (and かっこいい) conjugate on よ-; 良い / かわいい are regular.
        val yoi = word == "いい" || word.endsWith("っこいい")
        val stem = if (yoi) word.dropLast(2) + "よ" else word.dropLast(1)
        return ConjugationTable(
            typeLabel = "い形容词",
            forms = listOf(
                ConjugatedForm("否定", "${stem}くない"),
                ConjugatedForm("过去", "${stem}かった"),
                ConjugatedForm("过去否定", "${stem}くなかった"),
                ConjugatedForm("て形", "${stem}くて"),
                ConjugatedForm("副词", "${stem}く"),
                ConjugatedForm("仮定", "${stem}ければ"),
                ConjugatedForm("样态", if (yoi || word.endsWith("良い")) "${stem}さそう" else "${stem}そう"),
                ConjugatedForm("名词化", "${stem}さ"),
            ),
        )
    }

    private fun naAdjective(word: String): ConjugationTable? {
        val stem = word.removeSuffix("だ").removeSuffix("な")
        if (stem.isEmpty()) return null
        return ConjugationTable(
            typeLabel = "な形容词",
            forms = listOf(
                ConjugatedForm("修饰", "${stem}な〜"),
                ConjugatedForm("否定", "${stem}じゃない"),
                ConjugatedForm("过去", "${stem}だった"),
                ConjugatedForm("过去否定", "${stem}じゃなかった"),
                ConjugatedForm("て形", "${stem}で"),
                ConjugatedForm("副词", "${stem}に"),
                ConjugatedForm("仮定", "${stem}なら"),
                ConjugatedForm("礼貌", "${stem}です"),
            ),
        )
    }
}
