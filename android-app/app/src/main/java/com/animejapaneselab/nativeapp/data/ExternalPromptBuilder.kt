package com.animejapaneselab.nativeapp.data

import com.animejapaneselab.nativeapp.domain.AnswerFeedback

fun buildExternalQuestionPrompt(
    exercise: LinguisticExercise,
    selectedOption: String,
): String {
    val correct = exercise.isCorrect(selectedOption)
    return listOf(
        "请作为严谨的日语老师，用简体中文重新讲解下面这道日语语言学训练题。",
        if (correct) {
            "我答对了，但我想听到更深入、更不同角度的解释。请说明正确答案为什么成立，并指出其他选项为什么不如它。"
        } else {
            "我答错了。请先指出我选错的原因，再解释正确答案为什么更自然，并总结我下次该如何判断。"
        },
        "",
        "【作答结果】${if (correct) "答对" else "答错"}",
        "【我的答案】$selectedOption",
        "【正确答案】${exercise.correctOption}",
        "",
        "【日文场景】${exercise.dialogueText()}",
        exercise.zhText.takeIf { it.isNotBlank() }?.let { "【中文参考】$it" }.orEmpty(),
        "【题目】${exercise.prompt}",
        "【选项】",
        *exercise.options.mapIndexed { index, option -> "${index + 1}. $option" }.toTypedArray(),
        "",
        "【领域】${exercise.domain}",
        "【语言现象】${exercise.phenomenonKey}",
        "【题型】${exercise.questionType}",
        "【难度】${exercise.difficulty}",
        "【来源】${exercise.workSlug} EP${exercise.episode}${exercise.sourceLineNo.takeIf { it > 0 }?.let { " line $it" }.orEmpty()}",
        "",
        exercise.basicExplanationZh.takeIf { it.isNotBlank() }?.let { "【站内基础说明】$it" }.orEmpty(),
        exercise.deepExplanationZh.takeIf { it.isNotBlank() }?.let { "【站内深入解释】$it" }.orEmpty(),
        exercise.animeContextNoteZh.takeIf { it.isNotBlank() }?.let { "【动画语境】$it" }.orEmpty(),
        exercise.cautionNoteZh.takeIf { it.isNotBlank() }?.let { "【注意事项】$it" }.orEmpty(),
        "",
        "请不要只重复答案。请按“语境线索 -> 选项对比 -> 正确判断 -> 可迁移判断方法”的结构解释。",
    ).filter { it.isNotBlank() }.joinToString("\n")
}

fun buildExternalQuestionPrompt(
    node: LessonNode,
    feedback: AnswerFeedback,
): String {
    return listOf(
        "请作为严谨的日语老师，用简体中文重新讲解下面这道日语学习题。",
        if (feedback.correct) {
            "我答对了，但我想听到更深入、更不同角度的解释。"
        } else {
            "我答错了。请先指出我选错的原因，再解释正确答案为什么更自然，并总结我下次该如何判断。"
        },
        "",
        "【作答结果】${if (feedback.correct) "答对" else "答错"}",
        "【我的答案】${feedback.selected}",
        "【正确答案】${feedback.expected}",
        "",
        "【题型】${node.typeLabel}",
        "【题目】${node.prompt}",
        node.lessonBodyText().takeIf { it.isNotBlank() }?.let { "【题目内容】$it" }.orEmpty(),
        "【来源】${node.sourceLabel}",
        feedback.explanation.takeIf { it.isNotBlank() }?.let { "【站内说明】$it" }.orEmpty(),
        "",
        "请不要只重复答案。请按“语境线索 -> 选项对比 -> 正确判断 -> 可迁移判断方法”的结构解释。",
    ).filter { it.isNotBlank() }.joinToString("\n")
}

private fun LinguisticExercise.dialogueText(): String {
    if (sceneLines.isEmpty()) return jaText
    return sceneLines.joinToString("\n") { line ->
        buildString {
            if (line.speaker.isNotBlank()) {
                append(line.speaker)
                append("：")
            }
            append(line.jaText)
            if (line.zhText.isNotBlank()) {
                append(" / ")
                append(line.zhText)
            }
        }
    }
}

private fun LessonNode.lessonBodyText(): String {
    return when (this) {
        is StudyCardNode -> listOf(japanese, reading, meaningZh, notes.joinToString(" / ")).filter { it.isNotBlank() }.joinToString("\n")
        is SingleChoiceNode -> listOfNotNull(body, choices.joinToString(" / ").takeIf { it.isNotBlank() }).joinToString("\n")
        is ClozeNode -> "${before}____${after}\n${choices.joinToString(" / ") { it.value }}"
        is TileOrderNode -> "$displayText\n${bankTiles.joinToString(" / ")}"
        is PairMatchNode -> pairs.joinToString(" / ") { "${it.left}=${it.right}" }
        is ShadowingNode -> "${sentence.ja}\n${sentence.meaningZh}"
    }
}
