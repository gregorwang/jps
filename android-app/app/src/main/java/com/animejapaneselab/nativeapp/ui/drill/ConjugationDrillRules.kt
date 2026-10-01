package com.animejapaneselab.nativeapp.ui.drill

import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.ConjugationHead
import com.animejapaneselab.nativeapp.data.DrillProgress
import com.animejapaneselab.nativeapp.data.LessonPractice
import kotlin.math.abs

enum class DrillQuestionKind { Cloze, BaseForm, Practice }

data class DrillOption(val id: String, val text: String, val japanese: Boolean)

/**
 * One drill question. [item] is the anime line it is built on; null for a 板書 practice question
 * ([DrillQuestionKind.Practice]), which drills a textbook verb and explains itself with [why].
 */
data class DrillQuestion(
    val item: ConjugationDrillItem?,
    val kind: DrillQuestionKind,
    val prompt: String,
    val options: List<DrillOption>,
    val answerId: String,
    val pointId: String = item?.pointId.orEmpty(),
    val why: String = "",
)

/**
 * Pure rules for 活用道場: turning one annotated line into a question (distractors come from the
 * 行×段 grid, never from an LLM), Leitner scheduling, and session picking.
 */
object ConjugationDrillRules {
    /** Leitner box → days until the next review. */
    private val Intervals = longArrayOf(0, 1, 2, 4, 8, 16)
    const val SessionSize = 15
    const val MasteredBox = 3
    /** Anime lines in one lesson's 練習, after its 板書 practice questions. */
    const val LessonLines = 5

    private val GodanEndings = listOf("う", "く", "ぐ", "す", "つ", "ぬ", "ぶ", "む", "る")

    // ---------------------------------------------------------------- labels

    /** 五段-カ行 → カ行五段, 下一段-バ行 → 下一段, サ行変格 → サ变, カ行変格 → カ变. */
    fun typeLabel(ctype: String): String = when {
        ctype.startsWith("五段-") -> "${ctype.removePrefix("五段-").replace("ワア", "ワ")}五段"
        ctype.startsWith("上一段") -> "上一段"
        ctype.startsWith("下一段") -> "下一段"
        ctype == "サ行変格" -> "サ变"
        ctype == "カ行変格" -> "カ变"
        else -> ""
    }

    fun formLabel(cform: String): String = when {
        cform.startsWith("未然形") -> "未然形"
        cform == "連用形-促音便" -> "连用形·促音便"
        cform == "連用形-イ音便" -> "连用形·イ音便"
        cform == "連用形-撥音便" -> "连用形·拨音便"
        cform.startsWith("連用形") -> "连用形"
        cform.startsWith("終止形") -> "终止形"
        cform.startsWith("連体形") -> "连体形"
        cform.startsWith("仮定形") -> "假定形"
        cform.startsWith("命令形") -> "命令形"
        cform.startsWith("意志推量形") -> "意志形"
        else -> ""
    }

    private fun isVerb(head: ConjugationHead) = head.pos == "動詞" && typeLabel(head.ctype).isNotEmpty() && formLabel(head.cform).isNotEmpty()

    private fun isGodan(head: ConjugationHead) = head.ctype.startsWith("五段-")

    // ---------------------------------------------------------------- questions

    /**
     * Whether [item] can be asked at all: only lines whose form can be blanked out with rule-made
     * wrong forms. Lines that can't (だろう, なら, けど … whose point is meaning, not form) stay in
     * 自習 as reading cards but get no 練習 question; the old 贴标签 / 选译文 questions taught nothing.
     */
    fun drillable(item: ConjugationDrillItem): Boolean = DrillCloze.choices(item) != null

    /** Which kinds [item] can support, the 挖空 first. */
    fun kindsFor(item: ConjugationDrillItem): List<DrillQuestionKind> = buildList {
        val head = item.head
        if (drillable(item)) add(DrillQuestionKind.Cloze)
        if (isVerb(head) && isGodan(head) && head.cform.contains("音便") && head.base.takeLast(1) in GodanEndings && head.base.length >= 2) {
            add(DrillQuestionKind.BaseForm)
        }
    }

    /**
     * Builds a question for [item]; [salt] (seen count) rotates the kind so a repeat asks from
     * another angle — the first time is always the 挖空.
     */
    fun question(item: ConjugationDrillItem, salt: Int): DrillQuestion {
        val kinds = kindsFor(item).ifEmpty { listOf(DrillQuestionKind.Cloze) }
        val kind = kinds[salt % kinds.size]
        val seed = abs((item.id + salt).hashCode())
        return when (kind) {
            DrillQuestionKind.BaseForm -> baseForm(item, seed)
            else -> cloze(item, seed)
        }
    }

    /** 絶対に［ ? ］なんねえ — which form of 止める goes here? Options differ only in the form. */
    private fun cloze(item: ConjugationDrillItem, seed: Int): DrillQuestion {
        val choices = DrillCloze.choices(item) ?: DrillCloze.Choices(item.target, emptyList())
        return assemble(
            item, DrillQuestionKind.Cloze,
            "「${item.head.base}」在这里该用哪个形？",
            choices.answer, choices.wrong, seed, japanese = true,
        )
    }

    /** 書いて → 書く: back from an 音便 to the 辞书形 (the ending is what the 音便 hides). */
    private fun baseForm(item: ConjugationDrillItem, seed: Int): DrillQuestion {
        val head = item.head
        val stem = head.base.dropLast(1)
        val ending = head.base.takeLast(1)
        val confusable = when (head.cform) {
            "連用形-促音便" -> listOf("う", "つ", "る")
            "連用形-イ音便" -> listOf("く", "ぐ")
            "連用形-撥音便" -> listOf("ぬ", "ぶ", "む")
            else -> emptyList()
        }.filter { it != ending }
        val rest = GodanEndings.filter { it != ending && it !in confusable }
        val pickedRest = List(rest.size) { rest[(seed + it * 5) % rest.size] }.distinct()
        val endings = (confusable + pickedRest).distinct().take(3)
        return assemble(
            item, DrillQuestionKind.BaseForm,
            "「${head.surface}」的原形（辞书形）是？",
            head.base, endings.map { stem + it }, seed, japanese = true,
        )
    }

    /** A 板書 practice question: 「飲む → ない形」 with the lesson's hand-picked wrong forms. */
    fun practice(pointId: String, practice: LessonPractice): DrillQuestion {
        val seed = abs((pointId + practice.prompt).hashCode())
        val texts = (listOf(practice.answer) + practice.distractors.filter { it != practice.answer }).distinct().take(4)
        val position = seed % texts.size
        val ordered = texts.drop(1).toMutableList().apply { add(position, texts.first()) }
        val options = ordered.mapIndexed { index, text -> DrillOption(('A' + index).toString(), text, japanese = true) }
        return DrillQuestion(
            item = null,
            kind = DrillQuestionKind.Practice,
            prompt = practice.prompt,
            options = options,
            answerId = options.first { it.text == practice.answer }.id,
            pointId = pointId,
            why = practice.why,
        )
    }

    private fun assemble(
        item: ConjugationDrillItem,
        kind: DrillQuestionKind,
        prompt: String,
        correct: String,
        distractors: List<String>,
        seed: Int,
        japanese: Boolean,
    ): DrillQuestion {
        val texts = (listOf(correct) + distractors.filter { it != correct }).distinct().take(4)
        val position = seed % texts.size
        val ordered = texts.drop(1).toMutableList().apply { add(position, texts.first()) }
        val options = ordered.mapIndexed { index, text -> DrillOption(('A' + index).toString(), text, japanese) }
        return DrillQuestion(item, kind, prompt, options, options.first { it.text == correct }.id)
    }

    // ---------------------------------------------------------------- scheduling

    fun schedule(previous: DrillProgress?, correct: Boolean, today: Long): DrillProgress {
        val box = if (correct) ((previous?.box ?: 0) + 1).coerceAtMost(Intervals.lastIndex) else 0
        return DrillProgress(
            box = box,
            dueDay = today + Intervals[box],
            seen = (previous?.seen ?: 0) + 1,
            wrong = (previous?.wrong ?: 0) + if (correct) 0 else 1,
        )
    }

    /** 覚えた (double tap in 復習): straight to the top box. */
    fun master(previous: DrillProgress?, today: Long): DrillProgress = DrillProgress(
        box = Intervals.lastIndex,
        dueDay = today + Intervals.last(),
        seen = (previous?.seen ?: 0) + 1,
        wrong = previous?.wrong ?: 0,
    )

    fun isDue(progress: DrillProgress?, today: Long) = progress != null && progress.dueDay <= today

    /** Due reviews first (lowest box first), then unseen lines in curriculum order. */
    fun pickSession(
        items: List<ConjugationDrillItem>,
        progress: Map<String, DrillProgress>,
        today: Long,
        size: Int = SessionSize,
    ): List<ConjugationDrillItem> {
        val askable = items.filter(::drillable)
        val due = askable.filter { isDue(progress[it.id], today) }.sortedWith(compareBy({ progress[it.id]?.box ?: 0 }, { it.sortOrder }))
        val fresh = askable.filter { progress[it.id] == null }.sortedBy { it.sortOrder }
        // Interleave points among fresh lines so one set is not 15 of the same pattern.
        val interleaved = fresh.groupBy { it.pointId }.values.let { groups ->
            val iterators = groups.map { it.iterator() }
            buildList { while (iterators.any { it.hasNext() }) iterators.forEach { if (it.hasNext()) add(it.next()) } }
        }
        return (due + interleaved).distinctBy { it.sentenceId }.take(size)
    }

    /** Lines one lesson drills after its 板書: unseen first, then the weakest seen ones. */
    fun pickLesson(
        items: List<ConjugationDrillItem>,
        progress: Map<String, DrillProgress>,
        size: Int = LessonLines,
    ): List<ConjugationDrillItem> {
        val askable = items.filter(::drillable)
        val fresh = askable.filter { progress[it.id] == null }.sortedBy { it.sortOrder }
        val seen = askable.filter { progress[it.id] != null }.sortedWith(compareBy({ progress[it.id]?.box ?: 0 }, { it.sortOrder }))
        return (fresh + seen).distinctBy { it.sentenceId }.take(size)
    }

    fun dueCount(items: List<ConjugationDrillItem>, progress: Map<String, DrillProgress>, today: Long) =
        items.count { isDue(progress[it.id], today) }

    fun masteredCount(items: List<ConjugationDrillItem>, progress: Map<String, DrillProgress>) =
        items.count { (progress[it.id]?.box ?: 0) >= MasteredBox }

    /** Group label "A 动词活用形（行×段）" → key "A", title "动词活用形", gloss "行×段". */
    fun groupKey(group: String) = group.substringBefore(' ').trim()

    fun groupTitle(group: String) = group.substringAfter(' ').substringBefore('（').trim()

    fun groupGloss(group: String) = group.substringAfter('（', "").removeSuffix("）").trim()

    // ---------------------------------------------------------------- 基础题库 links

    private val TopicByPoint = mapOf(
        "c_teiru" to "sem_teiru_readings", "g_teru" to "sem_teiru_readings",
        "c_teageru" to "prag_viewpoint_empathy", "c_tekureru" to "prag_viewpoint_empathy",
        "c_temorau" to "prag_viewpoint_empathy", "c_teitadaku" to "prag_viewpoint_empathy",
        "c_tekudasai" to "prag_viewpoint_empathy",
        "c_temoii" to "sem_modality", "c_tewa_dame" to "sem_modality", "g_nakereba" to "sem_modality",
        "d_kanou_doushi" to "sem_modality", "d_hazu" to "sem_modality", "d_beki" to "sem_modality",
        "d_darou" to "sem_modality",
        "d_reru" to "syn_passive", "d_seru" to "syn_causative", "d_nu" to "morph_negation_forms",
        "d_sou_youtai" to "sem_evidentiality", "d_sou_denbun" to "sem_evidentiality",
        "d_you_mitai" to "sem_evidentiality", "d_rashii" to "sem_evidentiality",
        "e_tara" to "syn_conditionals", "e_nara" to "syn_conditionals", "e_to" to "syn_conditionals",
        "a_katei_ba" to "syn_conditionals",
        "g_tte" to "syn_complement_quotation",
        "g_chau" to "prag_register_style", "g_toku" to "prag_register_style",
    )

    private val TopicByGroup = mapOf(
        "A" to "morph_verb_conjugation", "B" to "morph_verb_conjugation", "C" to "syn_te_clause_linking",
        "D" to "morph_auxiliary_chain", "E" to "prag_connectives_coherence", "F" to "morph_adjective_inflection",
        "G" to "morph_auxiliary_chain", "H" to "prag_politeness_honorifics",
    )

    /** The `linguistic_foundation_topics` id that explains [item]'s grammar point in depth. */
    fun topicIdFor(item: ConjugationDrillItem): String? =
        TopicByPoint[item.pointId] ?: TopicByGroup[groupKey(item.group)]

    val linkedTopicIds: Set<String> get() = TopicByPoint.values.toSet() + TopicByGroup.values
}
