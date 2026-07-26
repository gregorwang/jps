package com.animejapaneselab.nativeapp.data

/**
 * Produces learner-friendly Japanese phrase boundaries for display only.
 *
 * This is deliberately conservative: database text stays unchanged and answer checking never
 * consumes these segments. The boundaries simply give Compose legal wrap points around common
 * adverbs, particles and sentence-final grammar instead of breaking a sentence at arbitrary glyphs.
 */
internal fun japaneseReadingSegments(
    text: String,
    grammarHint: String = "",
): List<String> {
    val source = text.trim().replace(Regex("\\s+"), "")
    if (source.isBlank()) return emptyList()
    if (source.length <= 8) return listOf(source)

    val boundaries = sortedSetOf<Int>()
    fun addBoundary(index: Int) {
        if (index in 2 until source.length) boundaries += index
    }

    val discourseMarkers = listOf(
        "こうやって",
        "そろそろ",
        "やっぱり",
        "それでも",
        "だから",
        "けれど",
        "ちゃんと",
        "ちょっと",
        "きっと",
        "ずっと",
        "もう",
        "まだ",
    )
    discourseMarkers.forEach { marker ->
        source.allIndicesOf(marker).forEach { start ->
            addBoundary(start)
            addBoundary(start + marker.length)
        }
    }

    val compoundParticles = listOf("には", "では", "から", "ので", "のに", "けど", "なら", "ても", "って", "しか", "まで", "より", "だけ", "ほど")
    compoundParticles.forEach { particle ->
        source.allIndicesOf(particle).forEach { start -> addBoundary(start + particle.length) }
    }

    Regex("[一-龯々〆ヵヶァ-ヴー]{1,14}[はがをにでともへ](?![っッ])(?=[一-龯々ぁ-んァ-ヴー])")
        .findAll(source)
        .forEach { match -> addBoundary(match.range.last + 1) }

    val grammarEndings = buildList {
        addAll(listOf("わけにはいかない", "なきゃいけない", "なくてはいけない", "かもしれない", "んじゃない", "じゃない", "ないと", "のね", "よね", "かな", "だろう", "だろ"))
        addAll(
            grammarHint
                .replace("句末", "")
                .replace("～", "")
                .replace("〜", "")
                .replace("「", "")
                .replace("」", "")
                .split('/', '／', '・', ',', '，')
                .map { it.trim() }
                .filter { it.length >= 2 },
        )
    }.distinct().sortedByDescending { it.length }
    grammarEndings.forEach { ending ->
        val start = source.lastIndexOf(ending)
        if (start > 1) addBoundary(start)
    }

    source.forEachIndexed { index, char ->
        if (char in "、，。！？!?…" && index + 1 < source.length) addBoundary(index + 1)
    }

    val raw = mutableListOf<String>()
    var start = 0
    (boundaries + source.length).forEach { end ->
        if (end > start) raw += source.substring(start, end)
        start = end
    }
    return mergeTinyReadingSegments(raw)
}

private fun String.allIndicesOf(value: String): List<Int> {
    if (value.isBlank()) return emptyList()
    val output = mutableListOf<Int>()
    var from = 0
    while (from < length) {
        val index = indexOf(value, from)
        if (index < 0) break
        output += index
        from = index + value.length
    }
    return output
}

private fun mergeTinyReadingSegments(values: List<String>): List<String> {
    val output = mutableListOf<String>()
    values.filter { it.isNotBlank() }.forEach { value ->
        if (value.length == 1 && output.isNotEmpty()) {
            output[output.lastIndex] = output.last() + value
        } else {
            output += value
        }
    }
    return output
}

/** Removes the database's repeated pattern/meaning template while preserving its useful example. */
internal fun compactGrammarStudyExplanation(
    pattern: String,
    meaningZh: String,
    explanation: String,
): String {
    val source = explanation.trim()
    if (source.isBlank() || pattern.isBlank() || meaningZh.isBlank()) return source
    val repeatedLead = Regex(
        pattern = "^\u6b64\u5904\u4f7f\u7528\\s*[\u300c\u300e“\"]?\\s*${Regex.escape(pattern.trim())}\\s*[\u300d\u300f”\"]?" +
            "\\s*[，,]\\s*\u8868\u793a\\s*${Regex.escape(meaningZh.trim())}\\s*[\u3002.]?\\s*",
    )
    return repeatedLead.replaceFirst(source, "").trim()
}
