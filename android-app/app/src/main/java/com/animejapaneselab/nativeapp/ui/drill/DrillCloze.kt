package com.animejapaneselab.nativeapp.ui.drill

import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.ConjugationHead

/**
 * Rule-only pieces of the 活用 練習 (no AI anywhere, so nothing here can be "wrong" the way the
 * old AI-reviewed labels were):
 *  - [wrongHeads]: the 挖空 question's distractors — the same word with a wrong stem / 段 / 音便,
 *    e.g. 立た+ない → 立ちない / 立つない / 立とない. Combinations that are real words (書けない
 *    potential, 書かば classical, ら抜き …) are left out on purpose.
 *  - [howTo]: the one-line 变法 shown after answering (立つ（五段动词）：「つ」变「た」→ 立た ＋ ない).
 *  - [fits]: the strict line ↔ 課 check. A line only stays in a 課 when the 課's form is really
 *    what the line shows; 止めなきゃ is no 一段 ない形 lesson (it is なきゃ = 必须), so it is dropped.
 */
object DrillCloze {
    private val A = mapOf('う' to 'わ', 'く' to 'か', 'ぐ' to 'が', 'す' to 'さ', 'つ' to 'た', 'ぬ' to 'な', 'ぶ' to 'ば', 'む' to 'ま', 'る' to 'ら')
    private val I = mapOf('う' to 'い', 'く' to 'き', 'ぐ' to 'ぎ', 'す' to 'し', 'つ' to 'ち', 'ぬ' to 'に', 'ぶ' to 'び', 'む' to 'み', 'る' to 'り')
    private val E = mapOf('う' to 'え', 'く' to 'け', 'ぐ' to 'げ', 'す' to 'せ', 'つ' to 'て', 'ぬ' to 'ね', 'ぶ' to 'べ', 'む' to 'め', 'る' to 'れ')
    private val O = mapOf('う' to 'お', 'く' to 'こ', 'ぐ' to 'ご', 'す' to 'そ', 'つ' to 'と', 'ぬ' to 'の', 'ぶ' to 'ぼ', 'む' to 'も', 'る' to 'ろ')

    /** Tails that turn a ない into another point (必须 / 不…的话): never part of a 行×段 lesson. */
    private val ObligationTails = listOf("なきゃ", "なくちゃ", "なくては", "なくっちゃ", "なければ", "なけりゃ", "ねば", "ないと", "ねえと")
    private val ContractedTails = listOf("ちゃ", "じゃ", "ちま", "じま")

    enum class Kind { Godan, Ichidan, Suru, Kuru, Adjective }

    fun kindOf(head: ConjugationHead): Kind? = when {
        head.pos == "動詞" && head.ctype.startsWith("五段-") -> Kind.Godan
        head.pos == "動詞" && (head.ctype.startsWith("上一段") || head.ctype.startsWith("下一段")) -> Kind.Ichidan
        head.pos == "動詞" && head.ctype == "サ行変格" -> Kind.Suru
        head.pos == "動詞" && head.ctype == "カ行変格" -> Kind.Kuru
        head.pos == "形容詞" || (head.pos == "助動詞" && head.base in setOf("ない", "たい")) -> Kind.Adjective
        else -> null
    }

    fun kindLabel(kind: Kind): String = when (kind) {
        Kind.Godan -> "五段动词"
        Kind.Ichidan -> "一段动词"
        Kind.Suru -> "する动词"
        Kind.Kuru -> "来る"
        Kind.Adjective -> "い形容词"
    }

    /** What follows the head inside the target (ない in 立たない); null if the head is not in it. */
    fun tailOf(item: ConjugationDrillItem): String? {
        val at = item.target.indexOf(item.head.surface)
        if (item.head.surface.isEmpty() || at < 0) return null
        return item.target.substring(at + item.head.surface.length)
    }

    /** A 挖空 question's answer and its three wrong options. */
    data class Choices(val answer: String, val wrong: List<String>)

    /**
     * The 挖空 options: the target, and the target with its head swapped for a wrong form.
     * Null when this line cannot be drilled that way (終止形, 助動詞 heads, odd parses …).
     */
    fun choices(item: ConjugationDrillItem): Choices? {
        val head = item.head
        val tail = tailOf(item) ?: return null
        val at = item.target.indexOf(head.surface)
        val prefix = item.target.substring(0, at)
        if (kindOf(head) == Kind.Kuru && head.surface == "来") return kanjiKuru(head.cform, prefix, tail)
        val heads = wrongHeads(head, tail) ?: return null
        val onbin = kindOf(head) == Kind.Godan && head.cform.startsWith("連用形")
        val wrong = heads
            .filter { it != head.surface && !(tail.isEmpty() && it == head.base) }
            .distinct()
            .map { prefix + it + (if (onbin) revoice(tail, it, head.base) else tail) }
            .filter { it != item.target }
        return if (wrong.size >= 3) Choices(item.target, wrong.take(3)) else null
    }

    /** て/で, た/だ follow the stem: ん → で, っ → て, い → で only for ぐ verbs (泳いで). */
    private fun revoice(tail: String, head: String, base: String): String {
        val first = tail.firstOrNull() ?: return tail
        if (first !in "てでただ") return tail
        val voiced = when (head.last()) {
            'ん' -> true
            'い' -> base.endsWith("ぐ")
            else -> false
        }
        val swapped = when (first) {
            'て', 'で' -> if (voiced) 'で' else 'て'
            else -> if (voiced) 'だ' else 'た'
        }
        return swapped + tail.drop(1)
    }

    /** 来 in kanji hides its reading, so the options spell it out: 来（こ）ない / 来（き）ない … */
    private fun kanjiKuru(cform: String, prefix: String, tail: String): Choices? {
        if (tail.isEmpty()) return null
        val right = when {
            cform.startsWith("未然形") -> "こ"
            cform.startsWith("連用形") -> "き"
            else -> return null
        }
        fun spell(reading: String) = "${prefix}来（$reading）$tail"
        return Choices(spell(right), listOf("こ", "き", "く", "け").filter { it != right }.map(::spell))
    }

    private fun wrongHeads(head: ConjugationHead, tail: String): List<String>? {
        val s = head.surface
        val base = head.base
        if (s.isEmpty() || base.isEmpty()) return null
        return when (kindOf(head)) {
            Kind.Godan -> godan(s, base, head.cform, tail)
            Kind.Ichidan -> ichidan(s, base, head.cform)
            Kind.Suru -> suru(s, base)
            Kind.Kuru -> kuru(s)
            Kind.Adjective -> adjective(s, base)
            null -> null
        }
    }

    private fun godan(s: String, base: String, cform: String, tail: String): List<String>? {
        val u = base.last()
        if (u !in A) return null
        val stem = base.dropLast(1)
        if (!s.startsWith(stem)) return null
        fun w(vararg ends: String) = ends.map { stem + it }
        val iku = base.endsWith("行く") || base == "いく"
        return when {
            cform.startsWith("未然形") -> if (u == 'う') w("あ", "い", "う") else w("${I[u]}", "$u", "${O[u]}")
            cform == "連用形-イ音便" -> w("っ", "ん", "${I[u]}")
            cform == "連用形-促音便" -> if (iku) w("い", "ん", "き") else if (u == 'う') w("い", "ん", "わ") else w("い", "ん", "${I[u]}")
            cform == "連用形-撥音便" -> w("っ", "い", "${I[u]}")
            // 話して: no 音便 for サ行, the classic slip is 話いて.
            cform.startsWith("連用形") && u == 'す' && (tail.startsWith("て") || tail.startsWith("た")) -> w("さ", "す", "い")
            cform.startsWith("連用形") -> w("${A[u]}", "$u", "${O[u]}")
            cform.startsWith("仮定形") -> w("${I[u]}", "$u", "${O[u]}")
            cform.startsWith("命令形") -> if (s == stem + E[u]) w("${A[u]}", "${I[u]}", "${E[u]}ろ") else null
            cform.startsWith("意志推量形") -> if (s == "$stem${O[u]}う") w("${I[u]}よう", "${u}よう", "${A[u]}う") else null
            else -> null
        }
    }

    private fun ichidan(s: String, base: String, cform: String): List<String>? {
        if (!base.endsWith("る")) return null
        val stem = base.dropLast(1)
        if (!s.startsWith(stem)) return null
        fun w(vararg ends: String) = ends.map { stem + it }
        return when {
            cform.startsWith("未然形") && s == stem -> w("ら", "る", "り")
            cform.startsWith("連用形") && s == stem -> w("り", "っ", "る")
            cform.startsWith("仮定形") && s == stem + "れ" -> w("ら", "る", "り")
            cform.startsWith("命令形") && s == stem + "ろ" -> w("れ", "り", "ら")
            cform.startsWith("意志推量形") && s == stem + "よう" -> w("ろう", "るよう", "おう")
            else -> null
        }
    }

    private fun suru(s: String, base: String): List<String>? {
        if (!base.endsWith("する")) return null // 案ずる, 文語す: skip
        val noun = base.dropLast(2)
        if (!s.startsWith(noun)) return null
        fun w(vararg ends: String) = ends.map { noun + it }
        return when (s.removePrefix(noun)) {
            "し" -> w("す", "さ", "する")
            "さ" -> w("す", "せ", "する")
            "すれ" -> w("しれ", "され", "せれ")
            "しろ" -> w("すろ", "しれ", "され")
            "しよう" -> w("すよう", "しろう", "するよう")
            else -> null
        }
    }

    private fun kuru(s: String): List<String>? = when (s) {
        "こ" -> listOf("き", "く", "くる")
        "き" -> listOf("こ", "く", "くる")
        "くれ" -> listOf("これ", "きれ", "けれ")
        "こい" -> listOf("きろ", "くい", "これ")
        "こよう" -> listOf("きよう", "くよう", "くるよう")
        else -> null // 来 written in kanji: the reading change is invisible
    }

    private fun adjective(s: String, base: String): List<String>? {
        if (!base.endsWith("い") || base == "いい" || base.endsWith("っこいい")) return null
        val stem = base.dropLast(1)
        if (!s.startsWith(stem)) return null
        fun w(vararg ends: String) = ends.map { stem + it }
        return when (s.removePrefix(stem)) {
            "く" -> w("い", "かっ", "さ")
            "かっ" -> w("く", "い", "けれ")
            "けれ" -> w("い", "かっ", "さ")
            else -> null
        }
    }

    /** 立つ（五段动词）：「つ」变「た」→ 立た ＋ ない. Empty when the head is not a known kind. */
    fun howTo(item: ConjugationDrillItem): String {
        val head = item.head
        val kind = kindOf(head) ?: return ""
        val tail = tailOf(item) ?: return ""
        val base = head.base
        val shared = base.commonPrefixWith(head.surface).length
        val removed = base.drop(shared)
        val added = head.surface.drop(shared)
        val change = when {
            kind == Kind.Kuru && head.surface == "来" && head.cform.startsWith("未然形") -> "汉字不变，读音变成「こ」"
            kind == Kind.Kuru && head.surface == "来" && head.cform.startsWith("連用形") -> "汉字不变，读音变成「き」"
            removed.isEmpty() && added.isEmpty() -> "原形不变"
            added.isEmpty() -> "去掉「$removed」"
            removed.isEmpty() -> "后面加「$added」"
            else -> "「$removed」变「$added」"
        }
        val result = if (tail.isEmpty()) head.surface else "${head.surface} ＋ $tail"
        return "$base（${kindLabel(kind)}）：$change → $result"
    }

    /**
     * Strict line ↔ 課 check for the 行×段 / 音便 books (A, B), where the 課 is exactly one form.
     * Other books keep their lines for now (their points are about meaning, rules can't judge).
     */
    fun fits(item: ConjugationDrillItem): Boolean {
        val p = item.pointId
        if (!p.startsWith("a_") && !p.startsWith("b_")) return true
        val head = item.head
        val kind = kindOf(head) ?: return false
        if (kind == Kind.Adjective) return false
        val tail = tailOf(item) ?: return false
        if (ObligationTails.any { tail.startsWith(it) } || ContractedTails.any { tail.startsWith(it) }) return false
        val f = head.cform
        val negative = tail.startsWith("な") || tail.startsWith("ね") || tail.startsWith("ず") || tail.startsWith("ん")
        val teTa = tail.startsWith("て") || tail.startsWith("で") || tail.startsWith("た") || tail.startsWith("だ")
        return when (p) {
            "a_mizen_nai" -> kind == Kind.Godan && f.startsWith("未然形") && negative
            "a_nai_other" -> kind != Kind.Godan && f.startsWith("未然形") && negative
            "a_renyo_masu" -> f.startsWith("連用形") && tail.startsWith("ま")
            "a_renyo_tai" -> f.startsWith("連用形") && tail.startsWith("た") && !tail.startsWith("たきゃ")
            "a_katei_ba" -> f.startsWith("仮定形") && tail.startsWith("ば")
            // 頑張れよ parses as 頑張れる＋命令: a 五段 imperative plus よ, not an 一段 one.
            "a_meirei" -> f.startsWith("命令形") && !(kind == Kind.Ichidan && head.surface.endsWith("よ"))
            "a_ishi" -> f.startsWith("意志推量形")
            "a_trap_ru" -> kind == Kind.Godan && head.base.endsWith("る")
            "a_suru" -> kind == Kind.Suru
            "a_kuru" -> kind == Kind.Kuru
            "b_i_onbin" -> f == "連用形-イ音便" && teTa
            "b_sokuon" -> f == "連用形-促音便" && teTa
            "b_hatsuon" -> f == "連用形-撥音便" && teTa
            "b_iku" -> (head.base.endsWith("行く") || head.base == "いく") && teTa
            "b_sa_row" -> kind == Kind.Godan && head.ctype == "五段-サ行" && f.startsWith("連用形") && teTa
            else -> true
        }
    }
}
