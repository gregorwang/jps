package com.animejapaneselab.nativeapp.ui.reading

import com.animejapaneselab.nativeapp.data.FuriganaResult

/**
 * Kana helpers for reading aids: katakana → hiragana, Hepburn romaji, and splitting a line into
 * reading units (one kanji word with its kana, or one mora of kana) so furigana and romaji can
 * sit right above / below what they spell.
 */
object Kana {
    fun isKanji(c: Char): Boolean = c in '一'..'鿿' || c in '㐀'..'䶿' || c == '々' || c == '〆' || c == 'ヶ'

    fun isHiragana(c: Char): Boolean = c in 'ぁ'..'ゖ'

    fun isKatakana(c: Char): Boolean = c in 'ァ'..'ヺ' || c == 'ー'

    fun isKana(c: Char): Boolean = isHiragana(c) || isKatakana(c)

    fun hasKanji(s: String): Boolean = s.any(::isKanji)

    fun toHiragana(s: String): String = buildString {
        s.forEach { c -> append(if (c in 'ァ'..'ヶ') c - 0x60 else c) }
    }

    private const val Small = "ゃゅょぁぃぅぇぉゎャュョァィゥェォヮ"

    private val Base = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'ゐ' to "i", 'ゑ' to "e", 'を' to "o", 'ん' to "n", 'ゔ' to "vu",
        'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
        'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゎ' to "wa",
    )

    /** Two-kana sounds that are not just consonant + small ya/yu/yo. */
    private val Pairs = mapOf(
        "しゃ" to "sha", "しゅ" to "shu", "しぇ" to "she", "しょ" to "sho",
        "じゃ" to "ja", "じゅ" to "ju", "じぇ" to "je", "じょ" to "jo",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちぇ" to "che", "ちょ" to "cho",
        "ぢゃ" to "ja", "ぢゅ" to "ju", "ぢょ" to "jo",
        "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
        "てぃ" to "ti", "でぃ" to "di", "とぅ" to "tu", "どぅ" to "du",
        "うぃ" to "wi", "うぇ" to "we", "うぉ" to "wo",
        "ゔぁ" to "va", "ゔぃ" to "vi", "ゔぇ" to "ve", "ゔぉ" to "vo",
        "つぁ" to "tsa", "つぃ" to "tsi", "つぇ" to "tse", "つぉ" to "tso",
        "いぇ" to "ye", "くぁ" to "kwa",
    )

    /**
     * Hepburn romaji for [kana] (hiragana or katakana). [next] is the kana right after it in the
     * line, so a trailing っ can still double the following consonant. Non-kana pass through.
     */
    fun romaji(kana: String, next: Char? = null): String {
        val s = toHiragana(kana)
        val out = StringBuilder()
        var i = 0
        var geminate = false
        while (i < s.length) {
            val c = s[i]
            val pair = if (i + 1 < s.length) s.substring(i, i + 2) else ""
            val syllable: String? = when {
                c == 'っ' -> { geminate = true; i++; continue }
                c == 'ー' -> { out.append(out.lastOrNull { it in "aeiou" } ?: '-'); i++; continue }
                pair.length == 2 && Pairs.containsKey(pair) -> { i += 2; Pairs.getValue(pair) }
                pair.length == 2 && pair[1] in "ゃゅょ" && Base[c]?.endsWith("i") == true -> {
                    i += 2
                    Base.getValue(c).dropLast(1) + Base.getValue(pair[1])
                }
                Base.containsKey(c) -> { i++; Base.getValue(c) }
                else -> { i++; null }
            }
            if (syllable == null) {
                if (geminate) geminate = false
                out.append(s[i - 1])
                continue
            }
            if (geminate) {
                out.append(if (syllable.startsWith("ch")) 't' else syllable.first())
                geminate = false
            }
            out.append(syllable)
        }
        if (geminate) {
            val following = next?.let { romaji(it.toString()) }.orEmpty()
            out.append(if (following.startsWith("ch")) 't' else following.firstOrNull()?.takeIf { it !in "aeiou" } ?: '\'')
        }
        return out.toString()
    }

    /** Kana run → morae: small kana join the one before, っ joins the one after, ー the one before. */
    fun morae(run: String): List<String> {
        val out = mutableListOf<String>()
        var pendingSokuon = ""
        run.forEach { c ->
            when {
                (c in Small || c == 'ー') && out.isNotEmpty() && pendingSokuon.isEmpty() -> out[out.size - 1] = out.last() + c
                c == 'っ' || c == 'ッ' -> pendingSokuon += c
                else -> {
                    out += pendingSokuon + c
                    pendingSokuon = ""
                }
            }
        }
        if (pendingSokuon.isNotEmpty()) out += pendingSokuon
        return out
    }
}

/** One reading unit of a line: [text] at [start]; [ruby] is the kana over a kanji word (empty otherwise). */
data class ReadingUnit(val text: String, val start: Int, val ruby: String, val romaji: String) {
    val end: Int get() = start + text.length
    /** What this unit sounds like, in kana ("" for kanji we have no reading for, punctuation, latin). */
    val kana: String get() = ruby.ifEmpty { text.takeIf { t -> t.all { Kana.isKana(it) } }.orEmpty() }
}

data class WordReading(val kana: String, val romaji: String)

/** A line split into [ReadingUnit]s, from the Worker's furigana when it matches [text]. */
class LineReading(val text: String, val units: List<ReadingUnit>) {

    /** Kana + romaji for [start, end); null when a kanji inside it has no known reading. */
    fun readingOf(start: Int, end: Int): WordReading? {
        if (start < 0 || end > text.length || start >= end) return null
        val kana = StringBuilder()
        for (u in units) {
            if (u.end <= start || u.start >= end) continue
            val inside = u.start >= start && u.end <= end
            when {
                u.ruby.isNotEmpty() && inside -> kana.append(u.ruby)
                u.ruby.isNotEmpty() -> return null
                else -> {
                    val part = u.text.substring((start - u.start).coerceAtLeast(0), (end - u.start).coerceAtMost(u.text.length))
                    if (part.any { Kana.isKanji(it) }) return null
                    kana.append(part.filter { Kana.isKana(it) })
                }
            }
        }
        if (kana.isEmpty()) return null
        val next = units.firstOrNull { it.start >= end }?.kana?.firstOrNull()
        return WordReading(Kana.toHiragana(kana.toString()), Kana.romaji(kana.toString(), next))
    }

    /** Readings for the 拆解 words, located in order from around [from] in the line. */
    fun readingsOf(words: List<String>, from: Int): List<WordReading?> {
        var cursor = (from - 2).coerceAtLeast(0)
        return words.map { word ->
            val at = text.indexOf(word, cursor).takeIf { it >= 0 } ?: text.indexOf(word)
            if (at < 0) {
                if (word.isNotEmpty() && word.all { Kana.isKana(it) }) WordReading(Kana.toHiragana(word), Kana.romaji(word)) else null
            } else {
                cursor = at + word.length
                readingOf(at, at + word.length)
            }
        }
    }

    companion object {
        fun build(text: String, furigana: FuriganaResult?): LineReading {
            val segments = furigana?.takeIf { it.plainText == text }?.segments
                ?.flatMap { splitOkurigana(it.text, it.reading) }
                ?: listOf(text to "")
            val raw = mutableListOf<Pair<String, String>>()
            segments.forEach { (seg, reading) ->
                if (reading.isNotBlank() && seg.any { Kana.isKanji(it) }) {
                    raw += seg to Kana.toHiragana(reading)
                } else {
                    // Split plain text into kana morae and single other characters.
                    var i = 0
                    while (i < seg.length) {
                        if (Kana.isKana(seg[i])) {
                            var j = i
                            while (j < seg.length && Kana.isKana(seg[j])) j++
                            Kana.morae(seg.substring(i, j)).forEach { raw += it to "" }
                            i = j
                        } else {
                            raw += seg[i].toString() to ""
                            i++
                        }
                    }
                }
            }
            val units = mutableListOf<ReadingUnit>()
            var pos = 0
            raw.forEachIndexed { index, (t, ruby) ->
                val kana = ruby.ifEmpty { t.takeIf { s -> s.all { Kana.isKana(it) } }.orEmpty() }
                val nextKana = raw.getOrNull(index + 1)?.let { (nt, nr) -> nr.ifEmpty { nt } }?.firstOrNull()
                val romaji = when {
                    kana.isEmpty() -> ""
                    kana == "は" && isParticle(text, pos) -> "wa"
                    kana == "へ" && isParticle(text, pos) -> "e"
                    else -> Kana.romaji(kana, nextKana)
                }
                units += ReadingUnit(t, pos, ruby, romaji)
                pos += t.length
            }
            return LineReading(text, units)
        }

        /** は/へ after something and before a break or a new word reads as the particle (wa/e). */
        private fun isParticle(text: String, at: Int): Boolean {
            if (at == 0) return false
            val prev = text[at - 1]
            val next = text.getOrNull(at + 1)
            if (prev.isWhitespace()) return false
            return next == null || next.isWhitespace() || !Kana.isHiragana(next) || next in "、。？！?!…"
        }

        /** 「届か」+「とどか」 → 届/とど + か: kana at either end of a segment is its own reading. */
        private fun splitOkurigana(text: String, reading: String): List<Pair<String, String>> {
            if (reading.isBlank() || text.none { Kana.isKanji(it) }) return listOf(text to reading)
            val r = Kana.toHiragana(reading)
            var head = 0
            while (head < text.length && Kana.isKana(text[head]) && head < r.length && Kana.toHiragana(text[head].toString())[0] == r[head]) head++
            var tail = 0
            while (tail < text.length - head && Kana.isKana(text[text.length - 1 - tail]) && tail < r.length - head &&
                Kana.toHiragana(text[text.length - 1 - tail].toString())[0] == r[r.length - 1 - tail]
            ) tail++
            val core = text.substring(head, text.length - tail)
            val coreReading = r.substring(head, r.length - tail)
            if (core.isEmpty() || coreReading.isEmpty() || core.any { Kana.isKana(it) }) return listOf(text to reading)
            return buildList {
                if (head > 0) add(text.substring(0, head) to "")
                add(core to coreReading)
                if (tail > 0) add(text.substring(text.length - tail) to "")
            }
        }
    }
}
