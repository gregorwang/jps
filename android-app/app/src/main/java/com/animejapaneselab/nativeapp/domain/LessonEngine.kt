package com.animejapaneselab.nativeapp.domain

import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.PairMatchNode
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import com.animejapaneselab.nativeapp.data.StudyCardNode
import com.animejapaneselab.nativeapp.data.TileOrderNode

data class LessonSession(
    val nodes: List<LessonNode>,
    val index: Int = 0,
    val correct: Int = 0,
    val answered: Int = 0,
    val feedback: AnswerFeedback? = null,
) {
    val currentNode: LessonNode? = nodes.getOrNull(index)
    val isComplete: Boolean = index >= nodes.size
    val progress: Float = if (nodes.isEmpty()) 0f else index.toFloat() / nodes.size.toFloat()
}

data class AnswerFeedback(
    val correct: Boolean,
    val selected: String,
    val expected: String,
    val explanation: String,
)

object LessonEngine {
    fun start(nodes: List<LessonNode>) = LessonSession(nodes = nodes)

    fun answer(session: LessonSession, selected: String): LessonSession {
        val node = session.currentNode ?: return session
        if (session.feedback != null) return session

        val correct = isCorrect(node, selected)
        return session.copy(
            correct = session.correct + if (correct) 1 else 0,
            answered = session.answered + 1,
            feedback = AnswerFeedback(
                correct = correct,
                selected = selected,
                expected = node.expectedAnswer,
                explanation = node.explanation,
            ),
        )
    }

    fun continueAfterFeedback(session: LessonSession): LessonSession {
        if (session.feedback == null) return session
        return session.copy(
            index = (session.index + 1).coerceAtMost(session.nodes.size),
            feedback = null,
        )
    }

    fun restart(session: LessonSession) = LessonSession(nodes = session.nodes)

    private fun isCorrect(node: LessonNode, selected: String): Boolean {
        return when (node) {
            is StudyCardNode -> selected == node.expectedAnswer
            is SingleChoiceNode -> selected == node.answer
            is ClozeNode -> selected == node.answer
            is TileOrderNode -> selected == node.expectedAnswer
            is PairMatchNode -> selected == node.expectedAnswer
            is ShadowingNode -> selected in node.ratings
        }
    }
}
