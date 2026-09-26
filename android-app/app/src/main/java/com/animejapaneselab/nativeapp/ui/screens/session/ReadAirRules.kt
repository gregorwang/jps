package com.animejapaneselab.nativeapp.ui.screens.session

import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.ui.ReadAirAllFilter
import com.animejapaneselab.nativeapp.ui.design.OptionState
import com.animejapaneselab.nativeapp.ui.design.emphasisRanges

/** Where a scene line sits in the manga panel. */
enum class ReadAirLineSide { Start, End, Narration }

/**
 * One line of dialogue as the manga panel draws it. [unfinished] → dashed bubble (the line
 * trails off, or what is meant is not what is said).
 */
data class ReadAirSceneLine(
    val speaker: String,
    val ja: String,
    val zh: String,
    val isTarget: Boolean,
    val unfinished: Boolean,
    val side: ReadAirLineSide,
)

/** A character's one-line reaction in the feedback sheet (Japanese + Chinese gloss). */
data class ReadAirReaction(val ja: String, val zh: String)

/** Position ("3/10") and progress-line fraction of a session. */
data class ReadAirProgress(val position: Int, val total: Int, val fraction: Float)

/**
 * Pure rules for the 読空気 / 基礎 answering screens (no Compose, JVM unit-tested in
 * ReadAirRulesTest).
 */
object ReadAirRules {
    private val CuePrefix = Regex(
        pattern = """^[（(]?\s*[【\[]\s*cue\b[^】\]]*[】\]](?:[^）)]*[）)])?\s*""",
        option = RegexOption.IGNORE_CASE,
    )

    /** Strips the authoring 【cue …】 prefix some prompts still carry. */
    fun promptForDisplay(prompt: String): String = prompt.trim().replace(CuePrefix, "").trim()

    private val ClosingMarks = charArrayOf('」', '』', '）', ')', '"', '\'', ' ', '　', '♪')

    /** True when a line trails off (……, ‥, ・・・, a dangling 、 or dash, 〜). */
    fun trailsOff(text: String): Boolean {
        val t = text.trim().trimEnd(*ClosingMarks)
        if (t.isEmpty()) return false
        return t.endsWith('…') || t.endsWith('‥') || t.endsWith("...") || t.endsWith("・・") ||
            t.endsWith('、') || t.endsWith(',') || t.endsWith('—') || t.endsWith('―') ||
            t.endsWith('〜') || t.endsWith('～')
    }

    /** Questions whose target line means more than it says (言外之意). */
    fun isImplicatureQuestion(domain: String, questionType: String): Boolean {
        val d = domain.lowercase()
        val q = questionType.lowercase()
        return q.contains("kuuki") || d.startsWith("pragmatics") || d == "sociolinguistics"
    }

    /**
     * Speaker → side of the panel: distinct speakers alternate Start / End in order of first
     * appearance (唯 left, 澪 right, the third speaker left again). Blank speakers are narration.
     */
    fun speakerSides(speakers: List<String>): List<ReadAirLineSide> {
        val order = mutableListOf<String>()
        return speakers.map { raw ->
            val speaker = raw.trim()
            if (speaker.isEmpty()) return@map ReadAirLineSide.Narration
            if (speaker !in order) order += speaker
            if (order.indexOf(speaker) % 2 == 0) ReadAirLineSide.Start else ReadAirLineSide.End
        }
    }

    /**
     * Builds the manga panel lines for an exercise. Target = the flagged line, else the line whose
     * number matches `targetLineNo`; without scene lines the exercise's own sentence is the
     * target. Dashed when the line trails off, or when it is the target of a 言外之意 question.
     */
    fun sceneLines(exercise: LinguisticExercise): List<ReadAirSceneLine> {
        val implicature = isImplicatureQuestion(exercise.domain, exercise.questionType)
        val source = exercise.sceneLines.filter { it.jaText.isNotBlank() }
        if (source.isEmpty()) {
            if (exercise.jaText.isBlank()) return emptyList()
            return listOf(
                ReadAirSceneLine(
                    speaker = "",
                    ja = exercise.jaText.trim(),
                    zh = exercise.zhText.trim(),
                    isTarget = true,
                    unfinished = trailsOff(exercise.jaText) || implicature,
                    side = ReadAirLineSide.Start,
                ),
            )
        }
        val anyFlagged = source.any { it.isTarget }
        val sides = speakerSides(source.map { it.speaker })
        return source.mapIndexed { index, line ->
            val target = if (anyFlagged) {
                line.isTarget
            } else {
                exercise.targetLineNo > 0 && line.lineNo == exercise.targetLineNo
            }
            ReadAirSceneLine(
                speaker = line.speaker.trim(),
                ja = line.jaText.trim(),
                zh = line.zhText.trim(),
                isTarget = target,
                unfinished = trailsOff(line.jaText) || (target && implicature),
                side = sides[index],
            )
        }
    }

    /** The speaker whose line the question is about (for the reaction avatar). */
    fun targetSpeaker(lines: List<ReadAirSceneLine>): String? =
        (lines.firstOrNull { it.isTarget && it.speaker.isNotBlank() } ?: lines.lastOrNull { it.speaker.isNotBlank() })
            ?.speaker

    /** Text between 「」/『』 in a prompt: 「そろそろ」在这里是什么语气？ → [そろそろ]. */
    fun quotedFragments(text: String): List<String> {
        val result = mutableListOf<String>()
        Regex("""[「『]([^「」『』]{1,24})[」』]""").findAll(text).forEach { match ->
            val fragment = match.groupValues[1].trim()
            if (fragment.isNotEmpty()) result += fragment
        }
        return result
    }

    /**
     * 着重号 ranges for [text]: the first candidate (quoted fragments of the prompt, then
     * [extraKeywords]) that occurs in the text. Empty when nothing matches.
     */
    fun emphasisFor(text: String, prompt: String, extraKeywords: List<String> = emptyList()): List<IntRange> {
        val candidates = (quotedFragments(prompt) + extraKeywords)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val keyword = candidates.firstOrNull { text.contains(it) } ?: return emptyList()
        return emphasisRanges(text, keyword)
    }

    /**
     * Option state: before the answer is committed only the pending choice is Selected; after,
     * the correct option is Correct, a wrong committed choice Wrong, the rest Dimmed.
     */
    fun optionState(option: String, pending: String?, committed: String?, correctOption: String): OptionState =
        if (!committed.isNullOrBlank()) {
            when (option) {
                correctOption -> OptionState.Correct
                committed -> OptionState.Wrong
                else -> OptionState.Dimmed
            }
        } else if (option == pending) {
            OptionState.Selected
        } else {
            OptionState.Default
        }

    /**
     * "3/10" and the progress-line fraction. The position counts the current question; the
     * line only fills for committed answers (so it advances the moment you check).
     */
    fun progress(total: Int, answered: Int, currentAnswered: Boolean, hasCurrent: Boolean): ReadAirProgress {
        if (total <= 0) return ReadAirProgress(0, 0, 0f)
        if (!hasCurrent) return ReadAirProgress(total, total, 1f)
        val position = if (currentAnswered) answered.coerceIn(1, total) else (answered + 1).coerceIn(1, total)
        val fraction = answered.coerceIn(0, total).toFloat() / total
        return ReadAirProgress(position, total, fraction)
    }

    fun domainLabel(domain: String): String = when (domain) {
        ReadAirAllFilter -> "全部领域"
        "phonology" -> "音系学"
        "morphology" -> "形态学"
        "syntax" -> "句法学"
        "semantics" -> "语义学"
        "pragmatics" -> "语用学"
        "historical" -> "历史语言学"
        "sociolinguistics" -> "社会语言学"
        else -> domain
    }

    fun questionTypeLabel(questionType: String): String = when (questionType) {
        "single_choice" -> "单选判断"
        "multiple_choice" -> "多选辨析"
        "kuuki_yomi" -> "语境判断"
        "syntax_relation" -> "句法关系"
        "morphology_analysis" -> "词形分析"
        "contrast_choice" -> "对比选择"
        "listening_reasoning" -> "听辨推理"
        else -> questionType
    }

    /** 读空气 · 拒绝与委婉 · EP03 — the mono eyebrow above the panel. */
    fun eyebrow(exercise: LinguisticExercise): String = listOfNotNull(
        "读空气",
        exercise.phenomenonNameZh.trim().ifBlank { domainLabel(exercise.domain) }.takeIf { it.isNotBlank() },
        exercise.episode.takeIf { it > 0 }?.let { "EP" + it.toString().padStart(2, '0') },
    ).joinToString(" · ")

    /** Scope line for the つづく header: けいおん！ EP03 · 语用学. */
    fun scopeLabel(workLabel: String, episode: Int?, domain: String): String = listOfNotNull(
        workLabel.takeIf { it.isNotBlank() },
        episode?.takeIf { it > 0 }?.let { "EP" + it.toString().padStart(2, '0') },
        domain.takeUnless { it.isBlank() || it == ReadAirAllFilter }?.let(::domainLabel),
    ).joinToString(" · ").ifBlank { "读空气" }

    private val ReadAirCorrect = listOf(
        ReadAirReaction("そう、それが言いたかったの。", "对，我想说的就是这个。"),
        ReadAirReaction("よく分かったね。", "你听懂了呢。"),
        ReadAirReaction("さすが、空気読めてる。", "不愧是你，很会读空气。"),
    )
    private val ReadAirWrong = listOf(
        ReadAirReaction("……そういう意味じゃないよ。", "……不是那个意思啦。"),
        ReadAirReaction("うーん、ちょっと違うかな。", "嗯……好像不太对。"),
        ReadAirReaction("もう一回、聞いてみて。", "再听一遍试试。"),
    )
    private val StudyCorrect = listOf(
        ReadAirReaction("正解！よくできました。", "答对了！做得很好。"),
        ReadAirReaction("うん、その通り。", "嗯，就是这样。"),
    )
    private val StudyWrong = listOf(
        ReadAirReaction("惜しい！もう一度見てみよう。", "可惜！再看一遍吧。"),
        ReadAirReaction("ここ、間違えやすいんだよね。", "这里很容易弄错呢。"),
    )

    /**
     * The character's line in the feedback sheet, stable per question ([seed] = question id).
     * [inScene] = the speaker of the scene reacts (読空気); otherwise a classmate comments (基礎).
     */
    fun reaction(correct: Boolean, seed: String, inScene: Boolean = true): ReadAirReaction {
        val pool = when {
            inScene && correct -> ReadAirCorrect
            inScene -> ReadAirWrong
            correct -> StudyCorrect
            else -> StudyWrong
        }
        return pool[Math.floorMod(seed.hashCode(), pool.size)]
    }

    /** Kana anywhere → set the text in the Japanese serif. */
    fun looksJapanese(text: String): Boolean = text.any { it in '぀'..'ヿ' }

    /** 12 题对了 10 题 */
    fun tally(answered: Int, correct: Int): String = "$answered 题对了 $correct 题"

    /** 83% (empty when nothing was answered). */
    fun accuracy(answered: Int, correct: Int): String =
        if (answered <= 0) "" else "${(correct * 100 + answered / 2) / answered}%"
}
