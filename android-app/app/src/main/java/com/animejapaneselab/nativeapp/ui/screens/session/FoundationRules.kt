package com.animejapaneselab.nativeapp.ui.screens.session

import com.animejapaneselab.nativeapp.data.FoundationDomain
import com.animejapaneselab.nativeapp.data.FoundationQuestion
import com.animejapaneselab.nativeapp.data.FoundationQuestionType
import com.animejapaneselab.nativeapp.data.FoundationStage
import com.animejapaneselab.nativeapp.data.FoundationStimulus
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.ui.design.OptionState
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingState
import com.animejapaneselab.nativeapp.ui.theme.WorkHue

/** Pure rules for [FoundationSession] (JVM unit-tested in FoundationRulesTest). */
object FoundationRules {
    fun domainLabel(domain: FoundationDomain): String = when (domain) {
        FoundationDomain.PhonologyWriting -> "音系与文字"
        FoundationDomain.Morphology -> "形态论"
        FoundationDomain.Syntax -> "句法"
        FoundationDomain.Semantics -> "语义"
        FoundationDomain.PragmaticsDiscourse -> "语用与话语"
        FoundationDomain.Sociolinguistics -> "社会语言学"
        FoundationDomain.HistoricalGrammaticalization -> "历史与语法化"
    }

    fun stageLabel(stage: FoundationStage): String = when (stage) {
        FoundationStage.F1 -> "F1 识别"
        FoundationStage.F2 -> "F2 分析"
        FoundationStage.F3 -> "F3 对比"
        FoundationStage.F4 -> "F4 迁移"
    }

    fun typeLabel(type: FoundationQuestionType): String = when (type) {
        FoundationQuestionType.SingleChoice -> "概念判断"
        FoundationQuestionType.MorphologyAnalysis -> "形态分析"
        FoundationQuestionType.SyntaxRelation -> "句法关系"
        FoundationQuestionType.ContrastChoice -> "对比选择"
        FoundationQuestionType.KuukiYomi -> "语境判断"
    }

    /** 基礎 · 形态论 · F2 分析 · 活用 — the mono eyebrow. */
    fun eyebrow(question: FoundationQuestion, topic: FoundationTopic?): String = listOfNotNull(
        "基礎",
        topic?.domain?.let(::domainLabel),
        stageLabel(question.stage),
        topic?.titleZh?.trim()?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    /**
     * Dialogue stimulus → manga panel lines. Trailing-off turns are dashed; on a 語境 (kuuki-yomi /
     * pragmatics) question where nothing trails off, the last turn — the one whose intent is asked —
     * is dashed and treated as the target.
     */
    fun dialogueLines(
        dialogue: FoundationStimulus.Dialogue,
        questionType: FoundationQuestionType,
        domain: FoundationDomain?,
    ): List<ReadAirSceneLine> {
        val turns = dialogue.turns.filter { it.jaText.isNotBlank() }
        if (turns.isEmpty()) return emptyList()
        val sides = ReadAirRules.speakerSides(turns.map { it.speaker.orEmpty() })
        val trailing = turns.map { ReadAirRules.trailsOff(it.jaText) }
        val implicature = questionType == FoundationQuestionType.KuukiYomi || domain == FoundationDomain.PragmaticsDiscourse
        val impliedIndex = if (implicature && trailing.none { it }) turns.lastIndex else -1
        return turns.mapIndexed { index, turn ->
            ReadAirSceneLine(
                speaker = turn.speaker.orEmpty().trim(),
                ja = turn.jaText.trim(),
                zh = turn.zhText.orEmpty().trim(),
                isTarget = index == impliedIndex || (impliedIndex < 0 && trailing[index]),
                unfinished = trailing[index] || index == impliedIndex,
                side = sides[index],
            )
        }
    }

    /** Option state with ids: see [ReadAirRules.optionState]. */
    fun optionState(optionId: String, pending: String?, committed: String?, answerOptionId: String): OptionState =
        ReadAirRules.optionState(optionId, pending, committed, answerOptionId)

    /** A short option id (a / B / 1) becomes the leading mark; long ids are hidden. */
    fun optionMark(optionId: String): String? = optionId.trim().takeIf { it.length in 1..2 }?.uppercase()

    /** Position of the current question and the progress fraction (answered questions only). */
    fun progress(state: FoundationTrainingState): ReadAirProgress {
        val total = state.filteredQuestions.size
        if (total == 0) return ReadAirProgress(0, 0, 0f)
        val position = (state.currentIndex + 1).coerceIn(1, total)
        return ReadAirProgress(position, total, state.answeredCount.coerceIn(0, total).toFloat() / total)
    }

    /** Correct answers inside the current filter. */
    fun correctCount(state: FoundationTrainingState): Int = state.filteredQuestions.count { question ->
        state.selectedAnswers[question.id]?.let(question::isCorrect) == true
    }

    /** The line shown for a missed question on the つづく screen. */
    fun summaryText(question: FoundationQuestion): String = when (val s = question.stimulus) {
        is FoundationStimulus.Sentence -> s.jaText
        is FoundationStimulus.Dialogue -> s.turns.lastOrNull { it.jaText.isNotBlank() }?.jaText ?: question.promptZh
        is FoundationStimulus.Contrast -> s.items.joinToString(" / ") { it.text }
        is FoundationStimulus.Metalinguistic -> s.form ?: question.promptZh
    }.trim().ifBlank { question.promptZh }

    /** The work-colour family decides who comments in the feedback sheet (k-on / re-zero / none). */
    fun workSlugFor(hue: WorkHue): String? = when (hue) {
        WorkHue.Sakura -> "k-on"
        WorkHue.Sumire -> "re-zero"
        WorkHue.Ai -> null
    }
}
