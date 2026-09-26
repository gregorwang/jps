package com.animejapaneselab.nativeapp.ui.design

import com.animejapaneselab.nativeapp.ui.motion.MotionTokens

/**
 * Pure text rules shared by [VerticalText], [DialogueBox] and headings. No Compose here so the
 * rules stay unit-testable.
 */
object TextRules {
    /** Horizontal punctuation → vertical presentation forms (縦書き). */
    private val VerticalForms = mapOf(
        '、' to '︑',
        '。' to '︒',
        'ー' to '丨',
        '－' to '丨',
        '—' to '︱',
        '「' to '﹁',
        '」' to '﹂',
        '『' to '﹃',
        '』' to '﹄',
        '（' to '︵',
        '）' to '︶',
        '(' to '︵',
        ')' to '︶',
        '…' to '︙',
        '‥' to '︰',
        '〜' to '≀',
        '～' to '≀',
        '！' to '︕',
        '？' to '︖',
        '!' to '︕',
        '?' to '︖',
        '，' to '︐',
        '：' to '︓',
        '；' to '︔',
        '【' to '︻',
        '】' to '︼',
    )

    fun verticalGlyph(ch: Char): Char = VerticalForms[ch] ?: ch

    /**
     * Splits [text] into one glyph per row for vertical setting. Line breaks start a new column.
     * Returns columns in reading order (the first column is drawn right-most).
     */
    fun verticalColumns(text: String): List<List<String>> =
        text.split('\n').map { column ->
            column.map { verticalGlyph(it).toString() }
        }

    /**
     * Per-character reveal delays for the dialogue typewriter: [perChar] ms each, plus a
     * breath after 、(+120ms) and 。(+200ms). Index i is the delay *before* char i appears.
     */
    fun typewriterDelays(
        text: String,
        perChar: Int = MotionTokens.Dur.TypeChar,
        commaPause: Int = MotionTokens.Dur.TypeCommaPause,
        periodPause: Int = MotionTokens.Dur.TypePeriodPause,
    ): IntArray {
        val delays = IntArray(text.length)
        for (i in text.indices) {
            var d = if (i == 0) 0 else perChar
            if (i > 0) {
                when (text[i - 1]) {
                    '、', '，', ',' -> d += commaPause
                    '。', '！', '？', '!', '?', '…' -> d += periodPause
                }
            }
            delays[i] = d
        }
        return delays
    }

    /**
     * Scales typewriter delays so the whole line finishes in [targetTotalMs] (used when source
     * audio plays: total = audio length × 0.9). Keeps the relative punctuation rhythm.
     */
    fun typewriterDelaysFor(text: String, targetTotalMs: Int?): IntArray {
        val base = typewriterDelays(text)
        if (targetTotalMs == null || targetTotalMs <= 0) return base
        val total = base.sum()
        if (total <= 0) return base
        val scale = targetTotalMs.toFloat() / total
        return IntArray(base.size) { (base[it] * scale).toInt() }
    }

    private val KanjiDigits = arrayOf("〇", "一", "二", "三", "四", "五", "六", "七", "八", "九")

    /** 1 → 一, 13 → 十三, 25 → 二十五, 66 → 六十六, 100 → 百. Falls back to ASCII above 999. */
    fun kanjiNumber(n: Int): String {
        if (n < 0 || n > 999) return n.toString()
        if (n == 0) return KanjiDigits[0]
        val hundreds = n / 100
        val tens = (n / 10) % 10
        val ones = n % 10
        return buildString {
            if (hundreds > 0) {
                if (hundreds > 1) append(KanjiDigits[hundreds])
                append('百')
            }
            if (tens > 0) {
                if (tens > 1) append(KanjiDigits[tens])
                append('十')
            }
            if (ones > 0) append(KanjiDigits[ones])
        }
    }

    /** 第三話 */
    fun episodeLabel(episode: Int): String = "第${kanjiNumber(episode)}話"

    /** 場面 03 */
    fun sceneLabel(scene: Int): String = "場面 ${scene.toString().padStart(2, '0')}"
}
