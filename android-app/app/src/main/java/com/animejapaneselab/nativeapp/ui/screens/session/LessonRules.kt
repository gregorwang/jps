package com.animejapaneselab.nativeapp.ui.screens.session

import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.PairMatchNode
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import com.animejapaneselab.nativeapp.data.StudyCardNode
import com.animejapaneselab.nativeapp.data.TileOrderNode
import com.animejapaneselab.nativeapp.domain.AnswerFeedback
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.screens.learn.SceneKind
import com.animejapaneselab.nativeapp.ui.screens.today.TodayRules
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug

// Pure helpers for the lesson session (task package D1): labels, speakers, reactions, noted
// lines. No Compose so the session screens stay thin.

/** Where a sentence sits in the episode: who says it and when (both only when known). */
data class LessonLineInfo(val speaker: String?, val timeCode: String?)

object LessonRules {

    /** 場面 03 for the current lesson mode; null for review / practice-lab sessions. */
    fun sceneLabel(mode: LessonMode, exerciseLab: Boolean): String? {
        if (exerciseLab) return null
        val kind = SceneKind.entries.firstOrNull { it.lessonMode == mode } ?: return null
        return TextRules.sceneLabel(kind.ordinal + 1)
    }

    /** 听音拼句 / 词汇 … — what this set is called on the eyecatch and つづく. */
    fun setName(uiState: LabUiState): String {
        if (uiState.isExerciseLabSession) return uiState.activeExerciseLabKind?.label ?: "练习"
        val kind = SceneKind.entries.firstOrNull { it.lessonMode == uiState.lessonMode }
        return kind?.title ?: uiState.lessonMode.label
    }

    /** 第 2 组 (batch number) — only for curriculum sessions. */
    fun batchLabel(uiState: LabUiState): String? =
        if (uiState.isExerciseLabSession || uiState.lessonMode == LessonMode.Review) null else "第 ${uiState.lessonBatch.coerceAtLeast(1)} 组"

    /** K-ON! · EP03 — the mono broadcast tag used on the eyecatch and つづく. */
    fun broadcastTag(workSlug: String, episode: Int): String {
        val work = when (normalizeWorkSlug(workSlug)) {
            "k-on" -> "K-ON!"
            "re-zero" -> "Re:ZERO"
            else -> workSlug.uppercase().ifBlank { "ANIME" }
        }
        return "$work · EP${episode.coerceAtLeast(1).toString().padStart(2, '0')}"
    }

    /** Question heading: the Chinese prompt when there is one, otherwise the node title. */
    fun heading(node: LessonNode): String = when (node) {
        is StudyCardNode -> node.title
        is ShadowingNode -> "跟着原声说一遍"
        else -> node.prompt.ifBlank { node.title }
    }

    /** Eyebrow: 場面 03 · 拼句 · 12:31 (scene and time only when known). */
    fun eyebrow(scene: String?, node: LessonNode, info: LessonLineInfo?): String =
        listOfNotNull(scene, node.typeLabel.takeIf { it.isNotBlank() }, info?.timeCode).joinToString(" · ")

    /** The shadowing sentence a node was built from (sentence nodes use the sentence id as sourceId). */
    fun sentenceFor(node: LessonNode, sentences: List<ShadowingSentence>): ShadowingSentence? = when (node) {
        is ShadowingNode -> node.sentence
        else -> sentences.firstOrNull { it.id == node.sourceId }
    }

    /**
     * Speaker + time code by source line number, from read-air scene lines and loaded subtitles
     * (the same sources 今日の一句 uses). Nothing is invented: unknown stays null.
     */
    fun lineIndex(uiState: LabUiState): Map<Int, LessonLineInfo> {
        val lines = TodayRules.candidates(
            workSlug = uiState.selection.workSlug,
            episode = uiState.selection.episode,
            shadowing = uiState.shadowing,
            subtitles = uiState.subtitles,
            readAirExercises = uiState.readAir.exercises,
        )
        return lines.filter { it.lineNo > 0 && (it.speaker != null || it.timeCode != null) }
            .associate { it.lineNo to LessonLineInfo(it.speaker, it.timeCode) }
    }

    fun lineInfo(node: LessonNode, uiState: LabUiState, index: Map<Int, LessonLineInfo>): LessonLineInfo? {
        val sentence = sentenceFor(node, uiState.shadowing) ?: return null
        return index[sentence.sourceLineNo]
    }

    /** Correct / wrong reaction line for the feedback sheet, stable per question. */
    fun reaction(correct: Boolean, node: LessonNode): LessonReaction {
        val pool = when {
            correct && node is ShadowingNode -> ShadowCorrect
            correct -> Correct
            node is ShadowingNode -> ShadowWrong
            else -> Wrong
        }
        return pool[Math.floorMod(node.id.hashCode(), pool.size)]
    }

    /** "正确答案 · X" only where the expected answer is readable (not pairs / study cards). */
    fun expectedAnswer(node: LessonNode, feedback: AnswerFeedback): String? = when (node) {
        is StudyCardNode, is PairMatchNode, is ShadowingNode -> null
        else -> feedback.expected.takeIf { it.isNotBlank() }
    }

    /** Explanation shown after the verdict; drops copies that just restate the answer. */
    fun explanation(node: LessonNode, feedback: AnswerFeedback): String? {
        val raw = feedback.explanation.trim()
        val expected = expectedAnswer(node, feedback)
        val body = raw.takeUnless { it.isBlank() || it.startsWith("正确答案：${feedback.expected}") }
        return if (!feedback.correct && expected != null) {
            listOfNotNull("正确答案是「$expected」。", body).joinToString("")
        } else {
            body
        }
    }

    /** The Japanese (or quoted) text a missed node is remembered by on つづく. */
    fun notedText(node: LessonNode): String? = when (node) {
        is StudyCardNode -> node.japanese
        is SingleChoiceNode -> node.body?.takeIf { it.isNotBlank() } ?: node.prompt
        is ClozeNode -> node.before + node.answer + node.after
        is TileOrderNode -> if (node.audioTile) node.targetTiles.joinToString("") else node.displayText
        is ShadowingNode -> node.sentence.ja
        is PairMatchNode -> null
    }?.trim()?.takeIf { it.isNotBlank() }

    /** Kana anywhere → set in the Japanese serif. */
    fun looksJapanese(text: String): Boolean = text.any { it in '぀'..'ヿ' }

    /** 「そろそろ」→ そろそろ: the first quoted fragment of a prompt, for the 着重号. */
    fun quotedKeyword(text: String): String? =
        Regex("「([^」]+)」").find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    /** 6:42 */
    fun elapsed(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    /** A line of the episode for the eyecatch's second column: first sentence of the set. */
    fun eyecatchLine(nodes: List<LessonNode>): String? = nodes.asSequence()
        .mapNotNull { node ->
            when (node) {
                is ShadowingNode -> node.sentence.ja
                is TileOrderNode -> if (node.audioTile) node.targetTiles.joinToString("") else node.displayText
                is ClozeNode -> node.before + node.answer + node.after
                is StudyCardNode -> node.japanese.takeIf { it.length >= 4 }
                is SingleChoiceNode -> node.body
                is PairMatchNode -> null
            }
        }
        .map { it.trim() }
        .firstOrNull { it.length in 4..16 && looksJapanese(it) }

    /** The character that reacts in the feedback sheet: the line's speaker, else the work's face. */
    fun reactionCharacter(speaker: String?, workSlug: String, episode: Int) =
        WorkIdentity.character(speaker)?.takeIf { it.drawable != null }
            ?: WorkIdentity.representative(workSlug, episode)

    private val Correct = listOf(
        LessonReaction("正解！よくできました。", "答对了！做得很好。"),
        LessonReaction("うん、その通り。", "嗯，就是这样。"),
        LessonReaction("さすが、ちゃんと聞いてたね。", "不愧是你，听得很认真。"),
    )
    private val Wrong = listOf(
        LessonReaction("惜しい！もう一度見てみよう。", "可惜！再看一遍吧。"),
        LessonReaction("ここ、間違えやすいんだよね。", "这里很容易弄错呢。"),
        LessonReaction("大丈夫、次は分かるよ。", "没关系，下次就会了。"),
    )
    private val ShadowCorrect = listOf(
        LessonReaction("いい声！そっくりだよ。", "声音真好！很像呢。"),
        LessonReaction("うん、リズムもばっちり。", "嗯，节奏也很到位。"),
    )
    private val ShadowWrong = listOf(
        LessonReaction("もう一回、一緒に言ってみよう。", "再一起说一遍吧。"),
        LessonReaction("ゆっくりでいいよ。", "慢慢来就好。"),
    )
}

data class LessonReaction(val ja: String, val zh: String)
