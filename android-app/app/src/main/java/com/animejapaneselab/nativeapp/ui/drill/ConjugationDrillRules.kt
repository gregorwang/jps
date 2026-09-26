package com.animejapaneselab.nativeapp.ui.drill

import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.ConjugationHead
import com.animejapaneselab.nativeapp.data.DrillProgress
import kotlin.math.abs

enum class DrillQuestionKind { RowForm, BaseForm, PointId, Meaning }

data class DrillOption(val id: String, val text: String, val japanese: Boolean)

data class DrillQuestion(
    val item: ConjugationDrillItem,
    val kind: DrillQuestionKind,
    val prompt: String,
    val options: List<DrillOption>,
    val answerId: String,
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

    private val GodanEndings = listOf("う", "く", "ぐ", "す", "つ", "ぬ", "ぶ", "む", "る")
    private val RowOfEnding = mapOf(
        "う" to "ワ", "く" to "カ", "ぐ" to "ガ", "す" to "サ", "つ" to "タ",
        "ぬ" to "ナ", "ぶ" to "バ", "む" to "マ", "る" to "ラ",
    )
    private val Rows = listOf("カ", "ガ", "サ", "タ", "ナ", "バ", "マ", "ラ", "ワ")

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

    /** Which kinds [item] can support, most drill-worthy first. */
    fun kindsFor(item: ConjugationDrillItem, pool: List<ConjugationDrillItem>): List<DrillQuestionKind> = buildList {
        val head = item.head
        val verbGroup = item.pointId.startsWith("a_") || item.pointId.startsWith("b_")
        if (isVerb(head)) add(DrillQuestionKind.RowForm)
        if (isVerb(head) && isGodan(head) && head.base.takeLast(1) in GodanEndings && head.base.length >= 2) {
            add(DrillQuestionKind.BaseForm)
        }
        if (!verbGroup || isEmpty()) add(DrillQuestionKind.PointId)
        if (pool.count { it.zh.isNotBlank() && it.sentenceId != item.sentenceId } >= 3 && item.zh.isNotBlank()) {
            add(DrillQuestionKind.Meaning)
        }
    }

    /**
     * Builds a question for [item]; [salt] (seen count) rotates the kind so a repeat asks from
     * another angle. [pool] supplies point titles and translations for distractors.
     */
    fun question(item: ConjugationDrillItem, pool: List<ConjugationDrillItem>, salt: Int): DrillQuestion {
        val kinds = kindsFor(item, pool).ifEmpty { listOf(DrillQuestionKind.PointId) }
        val kind = kinds[(salt + abs(item.id.hashCode())) % kinds.size]
        val seed = abs((item.id + salt).hashCode())
        return when (kind) {
            DrillQuestionKind.RowForm -> rowForm(item, seed)
            DrillQuestionKind.BaseForm -> baseForm(item, seed)
            DrillQuestionKind.PointId -> pointId(item, pool, seed)
            DrillQuestionKind.Meaning -> meaning(item, pool, seed)
        }
    }

    private fun rowForm(item: ConjugationDrillItem, seed: Int): DrillQuestion {
        val head = item.head
        val type = typeLabel(head.ctype)
        val form = formLabel(head.cform)
        val correct = "$type · $form"
        val forms = listOf("未然形", "连用形", "终止形", "假定形", "命令形", "意志形") +
            listOf("连用形·促音便", "连用形·イ音便", "连用形·拨音便")
        val otherForms = forms.filter { it != form && it.substringBefore('·') != form.substringBefore('·') }
        val otherTypes = if (isGodan(head)) {
            val row = type.removeSuffix("五段").removeSuffix("行")
            Rows.filter { it != row }.map { "${it}行五段" } + listOf("上一段", "下一段")
        } else {
            listOf("上一段", "下一段", "サ变", "カ变", "ラ行五段").filter { it != type }
        }
        val distractors = linkedSetOf<String>()
        distractors += "$type · ${otherForms[seed % otherForms.size]}"
        distractors += "${otherTypes[seed % otherTypes.size]} · $form"
        distractors += "${otherTypes[(seed / 7) % otherTypes.size]} · ${otherForms[(seed / 3) % otherForms.size]}"
        var i = 0
        while (distractors.size < 3) distractors += "$type · ${otherForms[(seed + ++i) % otherForms.size]}"
        return assemble(
            item, DrillQuestionKind.RowForm,
            "「${head.surface}」是哪一类动词、哪个活用形？",
            correct, distractors.filter { it != correct }.take(3), seed, japanese = false,
        )
    }

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

    private fun pointId(item: ConjugationDrillItem, pool: List<ConjugationDrillItem>, seed: Int): DrillQuestion {
        val titles = pool.map { it.pointId to it }.distinctBy { it.first }
        val sameGroup = titles.filter { it.second.group == item.group && it.first != item.pointId }
        val others = titles.filter { it.second.group != item.group }
        val ordered = rotate(sameGroup, seed) + rotate(others, seed / 3)
        val distractors = ordered.map { it.second.pointTitle }.filter { it != item.pointTitle }.distinct().take(3)
        return assemble(
            item, DrillQuestionKind.PointId,
            "「${item.target}」用的是哪个语法？",
            item.pointTitle, distractors, seed, japanese = false,
        )
    }

    private fun meaning(item: ConjugationDrillItem, pool: List<ConjugationDrillItem>, seed: Int): DrillQuestion {
        val samePoint = pool.filter { it.pointId == item.pointId && it.sentenceId != item.sentenceId && it.zh.isNotBlank() }
        val others = pool.filter { it.pointId != item.pointId && it.group == item.group && it.zh.isNotBlank() }
        val distractors = (rotate(samePoint, seed) + rotate(others, seed / 5))
            .map { it.zh.trim() }
            .filter { it != item.zh.trim() }
            .distinct()
            .take(3)
        return assemble(item, DrillQuestionKind.Meaning, "这句台词的意思是？", item.zh.trim(), distractors, seed, japanese = false)
    }

    private fun <T> rotate(list: List<T>, seed: Int): List<T> =
        if (list.isEmpty()) list else list.indices.map { list[(it + seed) % list.size] }

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

    fun isDue(progress: DrillProgress?, today: Long) = progress != null && progress.dueDay <= today

    /** Due reviews first (lowest box first), then unseen lines in curriculum order. */
    fun pickSession(
        items: List<ConjugationDrillItem>,
        progress: Map<String, DrillProgress>,
        today: Long,
        size: Int = SessionSize,
    ): List<ConjugationDrillItem> {
        val due = items.filter { isDue(progress[it.id], today) }.sortedWith(compareBy({ progress[it.id]?.box ?: 0 }, { it.sortOrder }))
        val fresh = items.filter { progress[it.id] == null }.sortedBy { it.sortOrder }
        // Interleave points among fresh lines so one set is not 15 of the same pattern.
        val interleaved = fresh.groupBy { it.pointId }.values.let { groups ->
            val iterators = groups.map { it.iterator() }
            buildList { while (iterators.any { it.hasNext() }) iterators.forEach { if (it.hasNext()) add(it.next()) } }
        }
        return (due + interleaved).distinctBy { it.sentenceId }.take(size)
    }

    fun dueCount(items: List<ConjugationDrillItem>, progress: Map<String, DrillProgress>, today: Long) =
        items.count { isDue(progress[it.id], today) }

    fun masteredCount(items: List<ConjugationDrillItem>, progress: Map<String, DrillProgress>) =
        items.count { (progress[it.id]?.box ?: 0) >= MasteredBox }

    /** Group label "A 动词活用形（行×段）" → key "A", title "动词活用形", gloss "行×段". */
    fun groupKey(group: String) = group.substringBefore(' ').trim()

    fun groupTitle(group: String) = group.substringAfter(' ').substringBefore('（').trim()

    fun groupGloss(group: String) = group.substringAfter('（', "").removeSuffix("）").trim()
}
