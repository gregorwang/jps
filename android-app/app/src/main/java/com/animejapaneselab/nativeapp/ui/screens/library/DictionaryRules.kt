package com.animejapaneselab.nativeapp.ui.screens.library

import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.VocabItem
import java.text.Normalizer

/**
 * Pure rules behind the 辞書 tab: 五十音 grouping and ordering, JLPT buckets, find-in-page
 * matching and example-line lookup. No Compose here so every rule is unit-tested.
 */
internal object Gojuon {
    /** Index rows in dictionary order; [Other] collects kanji-only, Latin and digits. */
    val Rows: List<String> = listOf("あ", "か", "さ", "た", "な", "は", "ま", "や", "ら", "わ")
    const val Other = "他"
    val RailRows: List<String> = Rows + Other

    private val RowMembers: Map<Char, String> = buildMap {
        fun row(head: String, members: String) = members.forEach { put(it, head) }
        row("あ", "あいうえお")
        row("か", "かきくけこ")
        row("さ", "さしすせそ")
        row("た", "たちつてと")
        row("な", "なにぬねの")
        row("は", "はひふへほ")
        row("ま", "まみむめも")
        row("や", "やゆよ")
        row("ら", "らりるれろ")
        row("わ", "わゐゑをん")
    }

    private val SmallToLarge: Map<Char, Char> = mapOf(
        'ぁ' to 'あ', 'ぃ' to 'い', 'ぅ' to 'う', 'ぇ' to 'え', 'ぉ' to 'お',
        'っ' to 'つ', 'ゃ' to 'や', 'ゅ' to 'ゆ', 'ょ' to 'よ', 'ゎ' to 'わ',
        'ゕ' to 'か', 'ゖ' to 'け',
    )

    /** Katakana → hiragana (ァ..ヶ shift by 0x60); everything else unchanged. */
    fun toHiragana(ch: Char): Char = if (ch in 'ァ'..'ヶ') (ch.code - 0x60).toChar() else ch

    /** Hiragana with dakuten/handakuten removed and small kana enlarged: が→か, ぱ→は, ゃ→や. */
    fun baseKana(ch: Char): Char {
        val hira = toHiragana(ch)
        val stripped = Normalizer.normalize(hira.toString(), Normalizer.Form.NFD).first()
        return SmallToLarge[stripped] ?: stripped
    }

    /** NFKC (half-width kana, full-width Latin) and leading symbols (〜, 「, ・, ー) removed. */
    fun headOf(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFKC).dropWhile { !it.isLetter() }

    /** Index row of [text] by its first kana; kanji, Latin and digits fall into [Other]. */
    fun rowOf(text: String): String {
        val first = headOf(text).firstOrNull() ?: return Other
        return RowMembers[baseKana(first)] ?: Other
    }

    fun rowIndex(row: String): Int = RailRows.indexOf(row).let { if (it < 0) RailRows.size else it }

    /** Secondary sort key: folded kana first (か = が), then the exact hiragana spelling. */
    fun sortKey(text: String): String {
        val head = headOf(text)
        val folded = head.map { baseKana(it) }.joinToString("")
        val exact = head.map { toHiragana(it) }.joinToString("")
        return "$folded\u0000$exact"
    }
}

internal data class DictGroup<T>(val row: String, val entries: List<T>)

/**
 * Groups [items] under 五十音 rows (あ か さ … わ, then 他) and sorts each row like a paper
 * dictionary. [readingOf] gives the kana used for the index (reading, or the headword).
 */
internal fun <T> groupByGojuon(items: List<T>, readingOf: (T) -> String): List<DictGroup<T>> {
    if (items.isEmpty()) return emptyList()
    val keyed = items.map { item ->
        val reading = readingOf(item)
        Triple(item, Gojuon.rowOf(reading), Gojuon.sortKey(reading))
    }
    return keyed
        .groupBy { it.second }
        .toList()
        .sortedBy { (row, _) -> Gojuon.rowIndex(row) }
        .map { (row, members) -> DictGroup(row, members.sortedBy { it.third }.map { it.first }) }
}

/** Index reading of a vocab entry: its kana reading, or the headword when the reading is blank. */
internal fun VocabItem.indexReading(): String = reading.ifBlank { surface }

/** Grammar patterns are indexed by their first kana (〜ないと → な). */
internal fun GrammarPoint.indexReading(): String = pattern

// ---------------------------------------------------------------------------
// JLPT
// ---------------------------------------------------------------------------

internal object Jlpt {
    const val All = "ALL"
    const val Other = "OTHER"
    /** Easy → hard, the order the filter pills are shown in. */
    val Levels: List<String> = listOf("N5", "N4", "N3", "N2", "N1")

    /** trim + uppercase and exact N1..N5; "N?", "N/A", blank and noise go to [Other]. */
    fun normalize(raw: String): String {
        val normalized = raw.trim().uppercase()
        return if (normalized in Levels) normalized else Other
    }
}

internal data class LevelBucket(val key: String, val label: String, val count: Int)

/** 全部 first, then N5→N1 that actually occur, 其他 last. Empty levels produce no pill. */
internal fun levelBuckets(vocab: List<VocabItem>): List<LevelBucket> {
    if (vocab.isEmpty()) return emptyList()
    val counts = vocab.groupingBy { Jlpt.normalize(it.level) }.eachCount()
    return buildList {
        add(LevelBucket(Jlpt.All, "全部", vocab.size))
        Jlpt.Levels.forEach { key -> counts[key]?.takeIf { it > 0 }?.let { add(LevelBucket(key, key, it)) } }
        counts[Jlpt.Other]?.takeIf { it > 0 }?.let { add(LevelBucket(Jlpt.Other, "其他", it)) }
    }
}

internal fun filterByLevel(vocab: List<VocabItem>, levelKey: String): List<VocabItem> =
    if (levelKey == Jlpt.All) vocab else vocab.filter { Jlpt.normalize(it.level) == levelKey }

// ---------------------------------------------------------------------------
// Find in page (引く)
// ---------------------------------------------------------------------------

private fun matchesAny(query: String, vararg fields: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    val lower = q.lowercase()
    val hira = q.map { Gojuon.toHiragana(it) }.joinToString("")
    return fields.any { field ->
        field.lowercase().contains(lower) ||
            field.map { Gojuon.toHiragana(it) }.joinToString("").contains(hira)
    }
}

internal fun VocabItem.matches(query: String): Boolean =
    matchesAny(query, surface, reading, romanization, meaningZh)

internal fun GrammarPoint.matches(query: String): Boolean =
    matchesAny(query, pattern, titleZh, exampleJa, exampleZh)

internal fun ShadowingSentence.matches(query: String): Boolean =
    matchesAny(query, ja, reading, meaningZh, romaji)

// ---------------------------------------------------------------------------
// Example line (出典)
// ---------------------------------------------------------------------------

private fun Char.isKanji(): Boolean = this in '一'..'鿿' || this in '㐀'..'䶿' || this == '々'

/**
 * The episode line that shows [surface] in use. Tries the headword, then its stem when the
 * headword is an inflecting kanji word (頑張る → 頑張), then a reading of 2+ kana.
 */
internal fun findExampleLine(
    surface: String,
    reading: String,
    lines: List<ShadowingSentence>,
): ShadowingSentence? {
    if (lines.isEmpty()) return null
    val candidates = buildList {
        val word = surface.trim()
        if (word.isNotEmpty()) add(word)
        if (word.length >= 2 && !word.last().isKanji() && word.dropLast(1).any { it.isKanji() }) {
            add(word.dropLast(1))
        }
        val kana = reading.trim()
        if (kana.length >= 2 && kana != word) add(kana)
    }
    candidates.forEach { needle ->
        lines.firstOrNull { it.ja.contains(needle) }?.let { return it }
    }
    return null
}

/** "L12 憂" — line number plus the speaker when the line carries a （name） marker. */
internal fun exampleSource(line: ShadowingSentence): String {
    val speaker = parseSpokenLine(line.ja).speaker
    val number = line.sourceLineNo.takeIf { it > 0 }?.let { "L$it" }
    return listOfNotNull(number, speaker).joinToString(" ")
}

// ---------------------------------------------------------------------------
// AI 講解 prompts
// ---------------------------------------------------------------------------

internal fun VocabItem.aiKey(): String = "vocab:$id"
internal fun GrammarPoint.aiKey(): String = "grammar:$id"
internal fun ShadowingSentence.aiKey(): String = "sentence:$id"

internal fun VocabItem.aiContext(episodeLabel: String): String = buildString {
    append("辞書 AI 精讲。章节：").append(episodeLabel)
    append("\n词：").append(surface)
    append("\n读音：").append(reading)
    append("\n罗马音：").append(romanization)
    append("\n中文：").append(meaningZh)
    append("\n词性：").append(partOfSpeech)
    append("\n难度：").append(level)
    append("\n出现：").append(occurrence)
    append("\n请解释核心意思、语气、现实可用性、常见误解，并给出一个短记忆点。")
}

internal fun GrammarPoint.aiContext(episodeLabel: String): String = buildString {
    append("辞書 AI 精讲。章节：").append(episodeLabel)
    append("\n语法：").append(pattern)
    append("\n标题：").append(titleZh)
    append("\n日文例句：").append(exampleJa)
    append("\n中文：").append(exampleZh)
    append("\n说明：").append(explanationZh)
    append("\n语气：").append(pragmaticsNote)
    append("\n现实使用：").append(realWorldNote)
    append("\n请解释这句里的用法、口语语气、相近表达差异，并给出训练提示。")
}

internal fun ShadowingSentence.aiContext(episodeLabel: String): String = buildString {
    append("辞書 AI 精讲。章节：").append(episodeLabel)
    append("\n日文台词：").append(ja)
    append("\n读音：").append(reading)
    append("\n中文：").append(meaningZh)
    append("\n来源：").append(sourceLabel)
    append("\n请解释字面意思、句子结构、语气、跟读重点和现实可用性。")
}

/** Short part-of-speech tag for the entry head: 名詞 → 名, 動詞・五段 → 動・五. Blank stays blank. */
internal fun shortPartOfSpeech(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed
        .replace("詞", "")
        .replace("段", "")
        .replace("动词", "動")
        .replace("形容动", "形動")
        .replace("形容", "形")
        .replace("词", "")
        .take(6)
}
