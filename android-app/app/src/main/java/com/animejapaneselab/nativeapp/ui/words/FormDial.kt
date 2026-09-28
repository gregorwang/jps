package com.animejapaneselab.nativeapp.ui.words

import com.animejapaneselab.nativeapp.data.ConjugationTable

/**
 * The word card's 活用 dial: every form of a word split into the unchanged stem and the ending
 * that moves, plus the column of kana the endings start with (ま み む め も ん for 挑む,
 * い く か け そ さ for an い形容词). Tapping a cell cycles the forms that start with it.
 * Pure rules on top of [ConjugationTable]; no data needed.
 */
data class DialForm(
    /** ます形 / ない形 / 仮定 … as the table labels it; 辞書形 for the dictionary form. */
    val label: String,
    val value: String,
    val stem: String,
    val ending: String,
    val reading: String,
    /** Index into [FormDial.cells], or -1 when the word has no column. */
    val cell: Int,
    /** 不挑战 / 被挑战 … built from the word's first meaning. */
    val gloss: String,
    /** ま（あ段）＋ ない */
    val rule: String,
)

data class DialCell(val kana: String, val tag: String)

data class FormDial(
    val typeLabel: String,
    val forms: List<DialForm>,
    /** Empty for words whose stem never changes (一段, する, 来る, な形容词): chips and swipes only. */
    val cells: List<DialCell>,
) {
    fun index(label: String): Int = forms.indexOfFirst { it.label == label }.coerceAtLeast(0)

    /** The next form starting with [cell], after [current] when it is already in that cell. */
    fun cycle(cell: Int, current: Int): Int {
        val inCell = forms.indices.filter { forms[it].cell == cell }
        if (inCell.isEmpty()) return current
        val at = inCell.indexOf(current)
        return inCell[if (at < 0) 0 else (at + 1) % inCell.size]
    }

    companion object {
        const val Dictionary = "辞書形"
        private val Dan = listOf("あ段", "い段", "う段", "え段", "お段")
        private val GodanRows = mapOf(
            'う' to "わいうえお", 'く' to "かきくけこ", 'ぐ' to "がぎぐげご", 'す' to "さしすせそ", 'つ' to "たちつてと",
            'ぬ' to "なにぬねの", 'ぶ' to "ばびぶべぼ", 'む' to "まみむめも", 'る' to "らりるれろ",
        )

        fun of(table: ConjugationTable?, surface: String, reading: String, meaning: String): FormDial? {
            if (table == null || table.forms.isEmpty()) return null
            val word = surface.trim()
            val kana = reading.trim().ifBlank { word }
            // 来ます（きます）: the part before the bracket is the form, the bracket its reading.
            val dictionary = if (table.typeLabel.startsWith("サ") && !word.endsWith("する")) word + "する" else word
            val raw = listOf(Dictionary to (dictionary to null as String?)) + table.forms.map { form ->
                val value = form.value.removeSuffix("〜")
                form.label to (value.substringBefore('（') to value.substringAfter('（', "").removeSuffix("）").ifBlank { null })
            }
            val stem = raw.map { it.second.first }.reduce { a, b -> a.commonPrefixWith(b) }
            val dropped = word.length - stem.length
            val kanaStem = if (dropped in 0..kana.length) kana.dropLast(dropped) else ""
            val core = coreMeaning(meaning)
            val godan = table.typeLabel.startsWith("五段")
            val iAdjective = table.typeLabel.startsWith("い形")
            val row = if (godan) GodanRows[word.last()] else null

            val cells = mutableListOf<DialCell>()
            if (row != null) row.forEachIndexed { i, c -> cells += DialCell(c.toString(), "aiueo"[i].toString()) }
            fun cellOf(ending: String): Int {
                if (!(godan || iAdjective) || ending.isEmpty()) return -1
                val head = ending.first().toString()
                val found = cells.indexOfFirst { it.kana == head }
                if (found >= 0) return found
                cells += DialCell(head, if (godan) "て·た" else "")
                return cells.lastIndex
            }

            val forms = raw.map { (label, pair) ->
                val (value, bracketReading) = pair
                val ending = value.removePrefix(stem)
                val cell = cellOf(ending)
                val dan = if (row != null && cell in 0..4) Dan[cell] else if (godan && cell > 4) "音便" else null
                DialForm(
                    label = label,
                    value = value,
                    stem = stem,
                    ending = ending,
                    reading = bracketReading ?: if (kanaStem.isNotEmpty() || stem.isEmpty()) kanaStem + ending else value,
                    cell = cell,
                    gloss = glossFor(label, core),
                    rule = ruleFor(ending, dan),
                )
            }
            return FormDial(table.typeLabel, forms, if (godan || iAdjective) cells else emptyList())
        }

        /** 挑战、迎战 → 挑战; ① 努力；加油 → 努力. */
        private fun coreMeaning(meaning: String): String =
            meaning.trim().trimStart('①', ' ').split('、', '，', ',', '；', ';', '/', '（', '(').first().trim()

        private fun glossFor(label: String, m: String): String {
            if (m.isEmpty()) return ""
            return when (label) {
                Dictionary -> m
                "ます形", "礼貌" -> "$m（礼貌）"
                "ない形", "否定" -> "不$m"
                "て形" -> "$m，然后…"
                "た形", "过去" -> "${m}了"
                "过去否定" -> "没$m"
                "可能" -> "能$m"
                "受身" -> "被$m"
                "使役" -> "让人$m"
                "意向" -> "${m}吧"
                "命令" -> "给我$m！"
                "仮定" -> "如果$m"
                "副词" -> "${m}地"
                "样态" -> "看起来$m"
                "名词化" -> "${m}的程度"
                "修饰" -> "${m}的〜"
                else -> m
            }
        }

        private fun ruleFor(ending: String, dan: String?): String {
            if (ending.isEmpty()) return ""
            val head = ending.first().toString() + (dan?.let { "（$it）" } ?: "")
            val rest = ending.drop(1)
            return if (rest.isEmpty()) head else "$head ＋ $rest"
        }
    }
}
