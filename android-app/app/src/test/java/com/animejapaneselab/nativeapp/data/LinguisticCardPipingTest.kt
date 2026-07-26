package com.animejapaneselab.nativeapp.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nerd-lite linguistic card contract: how the Worker payload is parsed, and how a parsed payload
 * travels from a vocab/grammar/sentence row all the way into the [StudyCardNode] the lesson shows.
 *
 * Pure JVM test: only org.json + JUnit4, no Android runtime and no network.
 */
class LinguisticCardPipingTest {
    private val repository = SampleLearningRepository()

    // ---------------------------------------------------------------- payload parsing

    @Test
    fun linguisticCardPayloadParsesNerdLiteWorkerShape() {
        val payload = parseLinguisticCardPayload(JSONObject(nerdLitePayloadJson))

        assertNotNull(payload)
        requireNotNull(payload)
        assertTrue(payload.hasContent)
        assertEquals("nerd-lite", payload.level)
        assertEquals("语言学视角：ないと 把义务留在省略里", payload.headlineZh)
        assertEquals("这是解释视角，不作为答题依据。", payload.cautionZh)
        assertEquals("近代口语里 なければならない 逐步缩略成 ないと。", payload.historicalNoteZh)

        assertEquals(1, payload.terms.size)
        assertEquals(LinguisticCardTerm("形态构成 (Morphology)", "词是怎么被拼起来的。"), payload.terms.single())

        assertEquals(1, payload.domains.size)
        val domain = payload.domains.single()
        assertEquals("morphology", domain.domain)
        assertEquals("形态学：ない + と 的固定组合", domain.titleZh)
        assertEquals("先认出否定 ない，再认出条件 と。", domain.takeawayZh)
        assertEquals("ないと 是 なければならない 的口语缩略形式。", domain.explanationZh)
    }

    @Test
    fun linguisticCardPayloadAcceptsSnakeCaseKeys() {
        val payload = parseLinguisticCardPayload(
            JSONObject(
                """
                {
                  "level": "nerd-lite",
                  "headline_zh": "语言学视角：って 的话题化",
                  "caution_zh": "只作理解参考。",
                  "historical_note_zh": "由 という 缩略而来。",
                  "terms": [{"term_zh": "话题化 (Topicalization)", "plain_zh": "把一个词拎出来当话题。"}],
                  "domains": [
                    {
                      "domain": "syntax",
                      "title_zh": "句法：名词 + って",
                      "takeaway_zh": "口语里可以直接接名词。",
                      "explanation_zh": "って 在句首承担话题标记功能。"
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertNotNull(payload)
        requireNotNull(payload)
        assertEquals("语言学视角：って 的话题化", payload.headlineZh)
        assertEquals("只作理解参考。", payload.cautionZh)
        assertEquals("由 という 缩略而来。", payload.historicalNoteZh)
        assertEquals("话题化 (Topicalization)", payload.terms.single().termZh)
        assertEquals("把一个词拎出来当话题。", payload.terms.single().plainZh)
        assertEquals("句法：名词 + って", payload.domains.single().titleZh)
        assertEquals("口语里可以直接接名词。", payload.domains.single().takeawayZh)
        assertEquals("って 在句首承担话题标记功能。", payload.domains.single().explanationZh)
    }

    @Test
    fun linguisticCardPayloadSurvivesWhenOnlyDomainsCarryContent() {
        val payload = parseLinguisticCardPayload(
            JSONObject(
                """{"level":"nerd-lite","domains":[{"domain":"pragmatics","explanation_zh":"只有解释也算有内容。"}]}""",
            ),
        )

        assertNotNull(payload)
        requireNotNull(payload)
        assertTrue(payload.hasContent)
        assertEquals("", payload.headlineZh)
        assertTrue(payload.terms.isEmpty())
        assertEquals("只有解释也算有内容。", payload.domains.single().explanationZh)
    }

    @Test
    fun linguisticCardPayloadReturnsNullForMissingOrEmptyContent() {
        assertNull(parseLinguisticCardPayload(null))
        assertNull(parseLinguisticCardPayload(JSONObject()))
        // Level/caution alone is not content: nothing would render on the card.
        assertNull(
            parseLinguisticCardPayload(
                JSONObject("""{"level":"nerd-lite","cautionZh":"只有提示，没有正文。"}"""),
            ),
        )
        // Nameless terms and empty domains are filtered out before the emptiness check.
        assertNull(
            parseLinguisticCardPayload(
                JSONObject(
                    """
                    {
                      "level": "nerd-lite",
                      "terms": [{"termZh": "", "plainZh": "没有术语名"}],
                      "domains": [{"domain": "syntax", "titleZh": "", "explanationZh": ""}]
                    }
                    """.trimIndent(),
                ),
            ),
        )
    }

    // ---------------------------------------------------------------- repository piping

    @Test
    fun vocabLinguisticPayloadReachesVocabStudyCards() {
        val payload = requireNotNull(parseLinguisticCardPayload(JSONObject(nerdLitePayloadJson)))
        val vocab = (1..4).map { index -> pipedVocab("piped-vocab-$index", index, payload) }

        val content = repository.contentFromRemote(
            selection = EpisodeSelection("k-on", 1),
            vocab = vocab,
            grammar = emptyList(),
            shadowing = emptyList(),
            mode = LessonMode.Vocab,
        )
        val studyNodes = content.lessonNodes.filterIsInstance<StudyCardNode>()

        assertEquals(vocab.map { it.id }, studyNodes.map { it.sourceId })
        assertTrue(studyNodes.isNotEmpty())
        assertTrue(studyNodes.all { it.sourceKind == "vocab" })
        assertTrue(studyNodes.all { it.linguistic != null })
        assertEquals(payload, studyNodes.first().linguistic)
        assertTrue(requireNotNull(studyNodes.first().linguistic).hasContent)
    }

    @Test
    fun studyCardsKeepNullLinguisticWhenSourceRowHasNoPayload() {
        val vocab = (1..4).map { index -> pipedVocab("plain-vocab-$index", index, linguistic = null) }

        val content = repository.contentFromRemote(
            selection = EpisodeSelection("k-on", 1),
            vocab = vocab,
            grammar = emptyList(),
            shadowing = emptyList(),
            mode = LessonMode.Vocab,
        )
        val studyNodes = content.lessonNodes.filterIsInstance<StudyCardNode>()

        assertEquals(vocab.map { it.id }, studyNodes.map { it.sourceId })
        assertTrue(studyNodes.all { it.linguistic == null })
    }

    @Test
    fun mixedLessonPipesEachSourceKindPayloadIntoItsOwnStudyCard() {
        val vocabPayload = payloadWithHeadline("语言学视角：词汇层")
        val grammarPayload = payloadWithHeadline("语言学视角：语法层")
        val sentencePayload = payloadWithHeadline("语言学视角：句子层")
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val vocab = (1..8).map { index -> pipedVocab("vocab-$index", index, vocabPayload) }
        val grammar = (1..4).map { index ->
            GrammarPoint(
                id = "grammar-$index",
                pattern = "～ないと$index",
                titleZh = "必须$index",
                exampleJa = "起きないと$index。",
                exampleZh = "必须起床$index。",
                explanationZh = "解释$index",
                linguistic = grammarPayload,
            )
        }
        val sentences = (1..4).map { index ->
            ShadowingSentence(
                id = "sentence-$index",
                ja = "これはテストです$index",
                reading = "これはてすとです$index",
                meaningZh = "这是测试$index",
                sourceLabel = "EP01 第 $index 行",
                audioKind = AudioKind.Tts,
                linguistic = sentencePayload,
            )
        }

        val studyNodes = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = grammar,
            sentences = sentences,
            mode = LessonMode.Mixed,
            batch = 1,
        ).filterIsInstance<StudyCardNode>()

        assertEquals(listOf("vocab", "vocab", "grammar", "sentence"), studyNodes.map { it.sourceKind })
        assertTrue(studyNodes.all { it.linguistic != null })
        assertEquals(vocabPayload, studyNodes.first { it.sourceKind == "vocab" }.linguistic)
        assertEquals(grammarPayload, studyNodes.first { it.sourceKind == "grammar" }.linguistic)
        assertEquals(sentencePayload, studyNodes.first { it.sourceKind == "sentence" }.linguistic)
    }

    @Test
    fun targetVocabLessonKeepsLinguisticPayloadOnItsSingleStudyCard() {
        val payload = requireNotNull(parseLinguisticCardPayload(JSONObject(nerdLitePayloadJson)))
        val selection = EpisodeSelection("k-on", 1)
        val focus = repository.content(selection).focus
        val vocab = (1..4).map { index -> pipedVocab("target-vocab-$index", index, payload) }

        val nodes = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = emptyList(),
            sentences = emptyList(),
            mode = LessonMode.Mixed,
            target = LessonTarget.Vocab("target-vocab-2"),
        )
        val studyNode = nodes.filterIsInstance<StudyCardNode>().single()

        assertEquals("target-vocab-2", studyNode.sourceId)
        assertEquals(payload, studyNode.linguistic)
    }
}

private val nerdLitePayloadJson = """
{
  "level": "nerd-lite",
  "headlineZh": "语言学视角：ないと 把义务留在省略里",
  "cautionZh": "这是解释视角，不作为答题依据。",
  "historicalNoteZh": "近代口语里 なければならない 逐步缩略成 ないと。",
  "terms": [
    {"termZh": "形态构成 (Morphology)", "plainZh": "词是怎么被拼起来的。"}
  ],
  "domains": [
    {
      "domain": "morphology",
      "titleZh": "形态学：ない + と 的固定组合",
      "takeawayZh": "先认出否定 ない，再认出条件 と。",
      "explanationZh": "ないと 是 なければならない 的口语缩略形式。"
    }
  ]
}
""".trimIndent()

private fun payloadWithHeadline(headline: String): LinguisticCardPayload {
    return LinguisticCardPayload(
        level = "nerd-lite",
        headlineZh = headline,
        cautionZh = "只作理解参考。",
        historicalNoteZh = "",
        terms = listOf(LinguisticCardTerm("形态构成 (Morphology)", "词是怎么被拼起来的。")),
        domains = listOf(
            LinguisticCardDomain(
                domain = "morphology",
                titleZh = "形态学：$headline",
                takeawayZh = "看构造，不看翻译。",
                explanationZh = "示例解释。",
            ),
        ),
    )
}

private fun pipedVocab(
    id: String,
    index: Int,
    linguistic: LinguisticCardPayload?,
): VocabItem = VocabItem(
    id = id,
    surface = "単語$index",
    reading = "たんご$index",
    romanization = "tango$index",
    meaningZh = "词义$index",
    partOfSpeech = "名词",
    level = "N5",
    occurrence = "语言学卡管道测试",
    toneTags = emptyList(),
    realWorldNote = "现实中可用",
    linguistic = linguistic,
)
