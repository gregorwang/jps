package com.animejapaneselab.nativeapp.ui.screens.learn

import com.animejapaneselab.nativeapp.data.FoundationDomain
import com.animejapaneselab.nativeapp.data.FoundationQuestion
import com.animejapaneselab.nativeapp.data.FoundationStage
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.ui.ReadAirAllFilter
import com.animejapaneselab.nativeapp.ui.ReadAirCognitiveTopic
import com.animejapaneselab.nativeapp.ui.design.TextRules

// ---------------------------------------------------------------------------
// 言語学: 第一巻 アニメの台詞 (read-air domains) / 第二巻 基礎 (foundation domains) as 教科書.
// Pure so the grouping is JVM-testable.
// ---------------------------------------------------------------------------

/** One 教科書 cover. [key] is the filter value the book selects (a domain). */
data class TextbookSpec(
    val key: String,
    val volumeLabel: String,
    val title: String,
    val sampleLine: String,
    val gloss: String,
    val total: Int,
    val answered: Int,
    val current: Boolean,
) {
    val progress: Float get() = if (total <= 0) 0f else (answered.toFloat() / total).coerceIn(0f, 1f)
}

object LinguisticsModel {
    const val MaxBooks = 4

    // ---- 第一巻 アニメの台詞 ----------------------------------------------------------------

    /** Vertical cover titles for read-air domains (short enough for a 212dp spine). */
    fun readAirTitle(domain: String): String = when (domain.trim()) {
        "pragmatics" -> "空気を読む"
        "sociolinguistics" -> "話し方"
        "syntax" -> "文の形"
        "morphology" -> "語の形"
        "phonology" -> "音と文字"
        "historical" -> "言葉の歴史"
        "cognitive_linguistics", "cognitive" -> "見えかた"
        else -> domain.take(5)
    }

    fun readAirDomainLabel(domain: String): String = when (domain.trim()) {
        ReadAirAllFilter -> "全部"
        "phonology" -> "音系学"
        "morphology" -> "形态学"
        "syntax" -> "句法学"
        "pragmatics" -> "语用学"
        "historical" -> "历史语言学"
        "sociolinguistics" -> "社会语言学"
        "cognitive_linguistics", "cognitive" -> "认知语言学"
        else -> domain
    }

    fun questionTypeLabel(type: String): String = when (type) {
        ReadAirAllFilter -> "全部"
        "single_choice" -> "单选判断"
        "multiple_choice" -> "多选辨析"
        "kuuki_yomi" -> "语境判断"
        "syntax_relation" -> "句法关系"
        "morphology_analysis" -> "词形分析"
        "contrast_choice" -> "对比选择"
        "listening_reasoning" -> "听辨推理"
        else -> type
    }

    fun difficultyLabel(value: String): String = if (value == ReadAirAllFilter) "全部" else value

    fun topicLabel(value: String): String = when (value) {
        ReadAirAllFilter -> "全部"
        ReadAirCognitiveTopic -> "认知语言学"
        else -> value
    }

    fun workLabel(slug: String): String = when (slug) {
        ReadAirAllFilter -> "全部"
        "rezero", "re-zero" -> "Re:ゼロ"
        "k-on" -> "けいおん！"
        else -> slug
    }

    /**
     * Up to four books, largest domains first; the [selectedDomain] (or the domain of the next
     * question, [upNextDomain]) is always among them and carries いま.
     */
    fun readAirBooks(
        exercises: List<LinguisticExercise>,
        domainCounts: Map<String, Int>,
        selectedDomain: String,
        upNextDomain: String?,
        isAnswered: (String) -> Boolean,
    ): List<TextbookSpec> {
        val currentDomain = selectedDomain.takeUnless { it == ReadAirAllFilter } ?: upNextDomain
        val ordered = pickBooks(
            keys = domainCounts.filterValues { it > 0 }.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key },
            pinned = currentDomain,
        )
        return ordered.mapIndexed { index, domain ->
            val inDomain = exercises.filter { it.domain == domain }
            val total = domainCounts[domain] ?: inDomain.size
            TextbookSpec(
                key = domain,
                volumeLabel = "VOL.${index + 1}",
                title = readAirTitle(domain),
                sampleLine = sampleLine(inDomain.firstOrNull()?.jaText.orEmpty()),
                gloss = "${readAirDomainLabel(domain)} · $total 问",
                total = total,
                answered = inDomain.count { isAnswered(it.id) }.coerceAtMost(total),
                current = domain == currentDomain,
            )
        }
    }

    // ---- 第二巻 基礎 -----------------------------------------------------------------------

    fun foundationTitle(domain: FoundationDomain): String = when (domain) {
        FoundationDomain.PhonologyWriting -> "音と文字"
        FoundationDomain.Morphology -> "語の形"
        FoundationDomain.Syntax -> "文の形"
        FoundationDomain.Semantics -> "意味"
        FoundationDomain.PragmaticsDiscourse -> "場と文脈"
        FoundationDomain.Sociolinguistics -> "話し方"
        FoundationDomain.HistoricalGrammaticalization -> "言葉の歴史"
    }

    fun foundationDomainLabel(domain: FoundationDomain): String = when (domain) {
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

    fun foundationBooks(
        questions: List<FoundationQuestion>,
        topics: List<FoundationTopic>,
        selectedDomain: FoundationDomain?,
        upNextDomain: FoundationDomain?,
        answeredIds: Set<String>,
    ): List<TextbookSpec> {
        val topicById = topics.associateBy(FoundationTopic::id)
        val byDomain = questions.groupBy { topicById[it.topicId]?.domain }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
        val currentDomain = selectedDomain ?: upNextDomain
        val ordered = pickBooks(
            keys = byDomain.entries
                .sortedWith(compareByDescending<Map.Entry<FoundationDomain, List<FoundationQuestion>>> { it.value.size }.thenBy { it.key.ordinal })
                .map { it.key.name },
            pinned = currentDomain?.name,
        ).map(FoundationDomain::valueOf)
        return ordered.mapIndexed { index, domain ->
            val inDomain = byDomain[domain].orEmpty()
            val firstTopic = topics.asSequence()
                .filter { it.domain == domain }
                .sortedWith(compareBy(FoundationTopic::sortOrder, FoundationTopic::id))
                .firstOrNull()
            TextbookSpec(
                key = domain.name,
                volumeLabel = "VOL.${index + 1}",
                title = foundationTitle(domain),
                sampleLine = sampleLine(firstTopic?.titleJa.orEmpty()),
                gloss = "${foundationDomainLabel(domain)} · ${inDomain.size} 问",
                total = inDomain.size,
                answered = inDomain.count { it.id in answeredIds },
                current = domain == currentDomain,
            )
        }
    }

    // ---- shared ---------------------------------------------------------------------------

    /** 四本 · 86 问 */
    fun statsLine(books: List<TextbookSpec>, questionCount: Int): String =
        "${TextRules.kanjiNumber(books.size)}本 · $questionCount 问"

    /** Largest [MaxBooks] keys, keeping [pinned] in the set (it replaces the last one). */
    internal fun pickBooks(keys: List<String>, pinned: String?): List<String> {
        val top = keys.take(MaxBooks)
        if (pinned == null || pinned !in keys || pinned in top) return top
        return top.take(MaxBooks - 1) + pinned
    }

    /** Short line for the cover's speech bubble: first clause, at most 12 chars. */
    internal fun sampleLine(text: String): String {
        val clean = text.trim().replace('\n', ' ')
        if (clean.isEmpty()) return "……"
        val cut = clean.indexOfAny(charArrayOf('。', '！', '？', '!', '?')).let { if (it in 1..11) it + 1 else -1 }
        return when {
            cut > 0 -> clean.take(cut)
            clean.length > 12 -> clean.take(11) + "…"
            else -> clean
        }
    }

    private val CuePrefix = Regex("""^[（(]?\s*[【\[]\s*cue\b[^】\]]*[】\]](?:[^）)]*[）)])?\s*""", RegexOption.IGNORE_CASE)

    /** Strips the internal "【cue …】" prefix some prompts carry. */
    fun displayPrompt(prompt: String): String = prompt.trim().replace(CuePrefix, "").trim()
}
