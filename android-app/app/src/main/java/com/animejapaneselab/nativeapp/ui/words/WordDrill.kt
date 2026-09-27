package com.animejapaneselab.nativeapp.ui.words

import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.FuriganaResult
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.ui.reading.Kana
import kotlin.math.abs

/** An anime line showing the word: [mark] is where it sits in [ja]; [exact] when that is the headword itself. */
data class WordExample(val ja: String, val zh: String, val mark: IntRange, val exact: Boolean, val audioUrl: String)

/**
 * One word ready to study: [reading] is the kana of the headword as written (null when unknown);
 * [lemma]/[lemmaReading] are set when the headword is an inflected form (戻れ → 戻る).
 */
data class WordCard(
    val item: VocabItem,
    val surface: String,
    val reading: String?,
    val lemma: String?,
    val lemmaReading: String?,
    val meaning: String,
    val example: WordExample?,
    /** One concrete usage line from the checked word cards ("" when none). */
    val note: String = "",
    /** Part of speech in plain Chinese when the checked card gives one. */
    val pos: String = "",
) {
    /** Morae to spell the word with, in the script it is written in (katakana words stay katakana). */
    val morae: List<String>
        get() = when {
            surface.all { Kana.isKana(it) } -> Kana.morae(surface)
            reading != null -> Kana.morae(reading)
            else -> emptyList()
        }
    val kanaOnly: Boolean get() = surface.all { Kana.isKana(it) }
    val romaji: String get() = Kana.romaji(if (kanaOnly) surface else reading.orEmpty())
}

enum class ChoiceKind { Meaning, Listen, Cloze }

sealed interface WordStep {
    val card: WordCard

    data class Study(override val card: WordCard) : WordStep

    /** Build the reading from [tiles] (the word's morae, repeats kept, plus look-alike distractors). */
    data class Spell(override val card: WordCard, val tiles: List<String>) : WordStep

    data class Choice(override val card: WordCard, val kind: ChoiceKind, val options: List<String>, val answer: Int) : WordStep
}

/**
 * Pure rules for 単語練習 (辞書 → pick words → learn → drill). Readings are repaired locally when the
 * stored one belongs to the dictionary form, distractors come from the episode's other words and
 * look-alike kana — never from an LLM, and the right answer is never shuffled out.
 */
object WordRules {
    private val FillerMarkers = listOf(
        "筛选", "学习价值", "机械词频", "能用「", "回想日语", "现实中可用", "动漫语气自然", "造一个", "放回", "跟读，注意",
    )

    /** Curation notes that leaked into vocab rows (「EP17筛选：学习价值优先…」); never shown. */
    fun isFiller(note: String): Boolean = note.isBlank() || FillerMarkers.any { it in note }

    /** Meaning trimmed to its first sense and tidy spacing. */
    fun cleanMeaning(meaning: String): String =
        meaning.replace(Regex("\\s+"), " ").trim().split('；', ';', '/', '／').first().trim().take(18)

    /** True when [reading] can spell [surface] (kanji runs take any kana, kana must match). */
    fun fits(surface: String, reading: String): Boolean {
        if (surface.isBlank() || reading.isBlank()) return false
        // A kanji reads as 1–4 kana, so a run of k kanji takes k..4k kana (大丈夫 ≠ だよ).
        val pattern = buildString {
            append('^')
            var run = 0
            fun flush() {
                if (run > 0) append("(.{$run,${run * 4}}?)")
                run = 0
            }
            surface.forEach { c ->
                if (Kana.isKanji(c)) {
                    run++
                } else {
                    flush()
                    append(Regex.escape(Kana.toHiragana(c.toString())))
                }
            }
            flush()
            append('$')
        }
        return runCatching { Regex(pattern).matches(Kana.toHiragana(reading)) }.getOrDefault(false)
    }

    /**
     * The kana of [surface] when the stored [reading] is its dictionary form: 戻れ + もどる → もどれ
     * (lemma 戻る), 食べた + たべる → たべた (食べる). Picks the lemma ending that shares the
     * inflected ending's first kana, else a one-kana (godan) ending.
     */
    fun inflected(surface: String, reading: String): Triple<String, String, String>? {
        val lastKanji = surface.indexOfLast { Kana.isKanji(it) }
        if (lastKanji < 0 || lastKanji == surface.length - 1) return null
        val stem = surface.substring(0, lastKanji + 1)
        val tail = Kana.toHiragana(surface.substring(lastKanji + 1))
        if (tail.any { !Kana.isHiragana(it) }) return null
        val r = Kana.toHiragana(reading)
        val shared = (1..minOf(3, r.length - 1)).firstOrNull { k -> r.takeLast(k).first() == tail.first() }
        val k = shared ?: 1
        val kanjiReading = r.dropLast(k)
        if (kanjiReading.isEmpty()) return null
        val surfaceReading = kanjiReading + tail
        if (!fits(surface, surfaceReading)) return null
        return Triple(surfaceReading, stem + r.takeLast(k), r)
    }

    fun card(item: VocabItem, furigana: FuriganaResult?, examples: List<WordExample>, fix: VocabCardFix? = null): WordCard {
        val surface = item.surface.trim()
        if (fix != null && fix.keep && fix.reading.isNotBlank()) {
            return WordCard(
                item = item,
                surface = surface,
                reading = Kana.toHiragana(fix.reading),
                lemma = fix.lemma.ifBlank { null }?.takeIf { it != surface },
                lemmaReading = fix.lemmaReading.ifBlank { null },
                meaning = fix.meaning.ifBlank { cleanMeaning(item.meaningZh) },
                example = examples.firstOrNull { it.exact } ?: examples.firstOrNull(),
                note = fix.note,
                pos = fix.pos,
            )
        }
        val stored = item.reading.trim()
        var reading: String? = null
        var lemma: String? = null
        var lemmaReading: String? = null
        when {
            surface.all { Kana.isKana(it) } -> reading = Kana.toHiragana(surface)
            fits(surface, stored) -> reading = Kana.toHiragana(stored)
            else -> {
                inflected(surface, stored)?.let { (sr, l, lr) ->
                    reading = sr
                    lemma = l
                    lemmaReading = lr
                }
                // The Worker's furigana for the headword wins when it spells it.
                val fromWorker = furigana?.takeIf { it.plainText == surface }?.segments
                    ?.joinToString("") { it.reading.ifBlank { it.text } }
                    ?.let(Kana::toHiragana)
                    ?.takeIf { fits(surface, it) }
                if (fromWorker != null) reading = fromWorker
            }
        }
        return WordCard(
            item = item,
            surface = surface,
            reading = reading,
            lemma = lemma?.takeIf { it != surface },
            lemmaReading = lemmaReading,
            meaning = cleanMeaning(item.meaningZh),
            example = examples.firstOrNull { it.exact } ?: examples.firstOrNull(),
        )
    }

    /** Headwords whose stored reading does not spell them: ask the Worker's furigana for these. */
    fun needsFurigana(item: VocabItem, fix: VocabCardFix? = null): Boolean {
        if (fix?.keep == true && fix.reading.isNotBlank()) return false
        val s = item.surface.trim()
        return Kana.hasKanji(s) && !fits(s, item.reading) && inflected(s, item.reading) == null
    }

    /** Lines that show [item]: exact headword first, then its stem (戻れ → 戻). Episode lines before drill lines. */
    fun examples(item: VocabItem, lines: List<ShadowingSentence>, drill: List<ConjugationDrillItem>): List<WordExample> {
        val surface = item.surface.trim()
        if (surface.length < 2 && !Kana.hasKanji(surface)) return emptyList()
        val all = lines.map { Triple(it.ja, it.meaningZh, it.audioUrl) } + drill.map { Triple(it.jaText, it.zh, it.audioUrl) }
        val exact = all.mapNotNull { (ja, zh, audio) ->
            val at = ja.indexOf(surface)
            if (at < 0) null else WordExample(ja, zh, at until at + surface.length, exact = true, audioUrl = audio)
        }
        if (exact.size >= 2) return exact.take(3)
        val lastKanji = surface.indexOfLast { Kana.isKanji(it) }
        val stem = if (lastKanji >= 0 && lastKanji < surface.length - 1) surface.substring(0, lastKanji + 1) else ""
        val loose = if (stem.isEmpty()) emptyList() else all.mapNotNull { (ja, zh, audio) ->
            val at = ja.indexOf(stem)
            if (at < 0 || exact.any { it.ja == ja }) return@mapNotNull null
            var end = at + stem.length
            while (end < ja.length && end - at < stem.length + 3 && Kana.isHiragana(ja[end])) end++
            WordExample(ja, zh, at until end, exact = false, audioUrl = audio)
        }
        return (exact + loose).take(3)
    }

    /** Study → 拼写 → 意思 → 例句填空 (or 听音 without a line), word by word. */
    fun steps(cards: List<WordCard>, pool: List<VocabItem>, attempt: Int = 0): List<WordStep> = cards.flatMap { card ->
        val seed = abs((card.item.id + attempt).hashCode())
        buildList {
            add(WordStep.Study(card))
            if (card.morae.size in 2..9) add(WordStep.Spell(card, spellTiles(card, pool, seed)))
            meaning(card, pool, seed)?.let(::add)
            val line = card.example?.takeIf { it.exact && it.ja.length <= 60 }
            (line?.let { cloze(card, pool, seed) } ?: listen(card, pool, seed))?.let(::add)
        }
    }

    /** The missed words' questions once more (no study card), for the end of a set. */
    fun retry(steps: List<WordStep>, missedIds: Set<String>): List<WordStep> =
        steps.filter { it !is WordStep.Study && it.card.item.id in missedIds }.distinctBy { it.card.item.id to it::class }

    private val LookAlike = mapOf(
        'か' to "が", 'が' to "か", 'き' to "ぎさ", 'ぎ' to "き", 'く' to "ぐへ", 'ぐ' to "く", 'け' to "げ", 'げ' to "け", 'こ' to "ご", 'ご' to "こ",
        'さ' to "ざち", 'ざ' to "さ", 'し' to "じつ", 'じ' to "し", 'す' to "ず", 'ず' to "す", 'せ' to "ぜ", 'ぜ' to "せ", 'そ' to "ぞ", 'ぞ' to "そ",
        'た' to "だ", 'だ' to "た", 'ち' to "ぢさ", 'つ' to "づし", 'て' to "で", 'で' to "て", 'と' to "ど", 'ど' to "と",
        'は' to "ばほ", 'ば' to "はぱ", 'ひ' to "びぴ", 'ふ' to "ぶぷ", 'へ' to "べく", 'ほ' to "ぼは", 'ぬ' to "め", 'め' to "ぬ",
        'ね' to "れわ", 'れ' to "ねわ", 'わ' to "れね", 'る' to "ろ", 'ろ' to "る", 'よ' to "ょ", 'ゆ' to "ゅ", 'や' to "ゃ", 'う' to "ぅ",
        'シ' to "ツ", 'ツ' to "シ", 'ソ' to "ン", 'ン' to "ソ", 'ク' to "ワ", 'ワ' to "ク", 'ル' to "レ", 'レ' to "ル", 'ア' to "マ", 'マ' to "ア",
    )

    private fun spellTiles(card: WordCard, pool: List<VocabItem>, seed: Int): List<String> {
        val correct = card.morae
        val decoys = linkedSetOf<String>()
        correct.forEach { mora ->
            val first = mora.lastOrNull() ?: return@forEach
            LookAlike[first]?.forEach { alt -> decoys += mora.dropLast(1) + alt }
        }
        if (decoys.size < 3) {
            pool.asSequence()
                .flatMap { Kana.morae(if (card.kanaOnly) it.surface else Kana.toHiragana(it.reading)).asSequence() }
                .filter { m -> m.isNotBlank() && m.all { Kana.isKana(it) } }
                .forEach { if (decoys.size < 6) decoys += it }
        }
        val extra = decoys.filter { it !in correct }.let { list -> List(list.size) { list[(it + seed) % list.size] } }.take(3)
        return shuffle(correct + extra, seed)
    }

    private fun meaning(card: WordCard, pool: List<VocabItem>, seed: Int): WordStep.Choice? {
        if (card.meaning.isBlank()) return null
        val others = pool.asSequence().map { cleanMeaning(it.meaningZh) }
            .filter { it.isNotBlank() && it != card.meaning && !it.contains(card.meaning) && !card.meaning.contains(it) }
            .distinct().toList()
        if (others.size < 3) return null
        return choice(card, ChoiceKind.Meaning, card.meaning, rotate(others, seed).take(3), seed)
    }

    private fun listen(card: WordCard, pool: List<VocabItem>, seed: Int): WordStep.Choice? {
        val others = similarSurfaces(card, pool)
        if (others.size < 3) return null
        return choice(card, ChoiceKind.Listen, card.surface, rotate(others, seed).take(3), seed)
    }

    private fun cloze(card: WordCard, pool: List<VocabItem>, seed: Int): WordStep.Choice? {
        val line = card.example ?: return null
        val others = similarSurfaces(card, pool).filter { !line.ja.contains(it) }
        if (others.size < 3) return null
        return choice(card, ChoiceKind.Cloze, card.surface, rotate(others, seed).take(3), seed)
    }

    /** Other headwords, same part of speech and similar length first. */
    private fun similarSurfaces(card: WordCard, pool: List<VocabItem>): List<String> {
        val pos = card.item.partOfSpeech.take(2)
        return pool.asSequence()
            .map { it.surface.trim() to it.partOfSpeech.take(2) }
            .filter { (s, _) -> s.isNotBlank() && s != card.surface && !s.contains(card.surface) && !card.surface.contains(s) }
            .distinctBy { it.first }
            .sortedWith(compareBy({ it.second != pos }, { abs(it.first.length - card.surface.length) }))
            .map { it.first }
            .take(12)
            .toList()
    }

    private fun choice(card: WordCard, kind: ChoiceKind, answer: String, distractors: List<String>, seed: Int): WordStep.Choice {
        val options = shuffle(listOf(answer) + distractors, seed)
        return WordStep.Choice(card, kind, options, options.indexOf(answer))
    }

    private fun <T> rotate(list: List<T>, seed: Int): List<T> =
        if (list.isEmpty()) list else list.indices.map { list[(it + seed) % list.size] }

    /** Deterministic shuffle that keeps every element (repeats included). */
    private fun <T> shuffle(list: List<T>, seed: Int): List<T> {
        val out = list.toMutableList()
        var s = seed.toLong() and 0x7fffffff
        for (i in out.indices.reversed()) {
            s = (s * 1103515245 + 12345) and 0x7fffffff
            val j = (s % (i + 1)).toInt()
            val t = out[i]; out[i] = out[j]; out[j] = t
        }
        return out
    }
}
