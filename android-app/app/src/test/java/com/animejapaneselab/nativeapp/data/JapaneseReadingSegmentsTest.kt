package com.animejapaneselab.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class JapaneseReadingSegmentsTest {
    @Test
    fun segmentsLongSentenceAtLearnerFriendlyGrammarBoundaries() {
        assertEquals(
            listOf("こうやって", "ニートが", "出来上がって", "いく", "のね"),
            japaneseReadingSegments(
                text = "こうやってニートが出来上がっていくのね",
                grammarHint = "句末 よ/ね/かな/だろ",
            ),
        )
    }

    @Test
    fun segmentsReminderBeforeAdverbAndTargetGrammar() {
        assertEquals(
            listOf("お姉ちゃん", "そろそろ", "起き", "ないと…"),
            japaneseReadingSegments(
                text = "お姉ちゃんそろそろ起きないと…",
                grammarHint = "～ないと",
            ),
        )
    }

    @Test
    fun keepsShortVocabularyAsOneReadableUnit() {
        assertEquals(listOf("軽音部"), japaneseReadingSegments("軽音部"))
    }

    @Test
    fun removesRepeatedGrammarTemplateButKeepsTheUsefulComparison() {
        assertEquals(
            "对照：这样下去就会变成家里蹲了。",
            compactGrammarStudyExplanation(
                pattern = "句末 よ/ね/かな/だろ",
                meaningZh = "语气助词",
                explanation = "此处使用「句末 よ/ね/かな/だろ」，表示语气助词。对照：这样下去就会变成家里蹲了。",
            ),
        )
    }
}
