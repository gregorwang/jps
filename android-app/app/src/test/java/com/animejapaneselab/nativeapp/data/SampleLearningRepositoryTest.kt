package com.animejapaneselab.nativeapp.data

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SampleLearningRepositoryTest {
    private val repository = SampleLearningRepository()

    @Test
    fun reZeroFallbackLessonsVaryAcrossEpisodes() {
        val ep04 = repository.content(EpisodeSelection("re-zero", 4), LessonMode.Mixed)
        val ep05 = repository.content(EpisodeSelection("re-zero", 5), LessonMode.Mixed)
        val ep56 = repository.content(EpisodeSelection("re-zero", 56), LessonMode.Mixed)
        val firstVocabSurfaces = listOf(
            ep04.vocab.first().surface,
            ep05.vocab.first().surface,
            ep56.vocab.first().surface,
        )

        assertEquals(firstVocabSurfaces.size, firstVocabSurfaces.toSet().size)
        assertTrue(ep04.vocab.first().id.contains("ep04"))
        assertTrue(ep05.vocab.first().id.contains("ep05"))
        assertTrue(ep56.vocab.first().id.contains("ep56"))
        assertTrue(ep56.shadowing.first().id.startsWith("rezero_s03e56"))
    }

    @Test
    fun vocabModeBuildsVocabStudyAndPracticeNodes() {
        val content = repository.content(EpisodeSelection("k-on", 1), LessonMode.Vocab)

        assertTrue(content.lessonNodes.isNotEmpty())
        assertTrue(content.lessonNodes.any { it is StudyCardNode && it.sourceKind == "vocab" })
        assertTrue(content.lessonNodes.any { it is SingleChoiceNode && it.sourceKind == "vocab" })
        assertTrue(content.lessonNodes.all { it.sourceKind == "vocab" })
    }

    @Test
    fun vocabModeCoversEveryVocabStudyItemLikeWeb() {
        val vocab = (1..7).map { index ->
            VocabItem(
                id = "vocab-$index",
                surface = "単語$index",
                reading = "たんご$index",
                romanization = "tango$index",
                meaningZh = "词$index",
                partOfSpeech = "名词",
                level = "N5",
                occurrence = "测试",
                toneTags = emptyList(),
            )
        }
        val content = repository.contentFromRemote(
            selection = EpisodeSelection("k-on", 1),
            vocab = vocab,
            grammar = emptyList(),
            shadowing = emptyList(),
            mode = LessonMode.Vocab,
        )

        val studiedIds = content.lessonNodes
            .filterIsInstance<StudyCardNode>()
            .map { it.sourceId }

        assertEquals(vocab.map { it.id }, studiedIds)
    }

    @Test
    fun grammarModeBuildsGrammarClozeNodes() {
        val content = repository.content(EpisodeSelection("k-on", 1), LessonMode.Grammar)

        assertTrue(content.lessonNodes.isNotEmpty())
        assertTrue(content.lessonNodes.any { it is StudyCardNode && it.sourceKind == "grammar" })
        assertTrue(content.lessonNodes.any { it is ClozeNode && it.sourceKind == "grammar" })
    }

    @Test
    fun grammarModeSupportsWebSpecialtyBatches() {
        val selection = EpisodeSelection("k-on", 1)
        val focus = repository.content(selection).focus
        val grammar = (1..7).map { index ->
            GrammarPoint(
                id = "grammar-$index",
                pattern = "～ないと$index",
                titleZh = "必须 $index",
                exampleJa = "起きないと$index。",
                exampleZh = "必须起床 $index。",
                explanationZh = "解释 $index",
                pragmaticsNote = "语气 $index",
                realWorldNote = "现实 $index",
                difficulty = "N4",
                sourceLineNo = index,
            )
        }

        val firstBatch = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = grammar,
            sentences = emptyList(),
            mode = LessonMode.Grammar,
            batch = 1,
        )
        val secondBatch = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = grammar,
            sentences = emptyList(),
            mode = LessonMode.Grammar,
            batch = 2,
        )

        assertEquals((1..6).map { "grammar-$it" }, firstBatch.filterIsInstance<StudyCardNode>().map { it.sourceId })
        assertEquals(listOf("grammar-7"), secondBatch.filterIsInstance<StudyCardNode>().map { it.sourceId })
        assertTrue(repository.hasNextLessonBatch(emptyList(), grammar, emptyList(), LessonMode.Grammar, batch = 1))
        assertFalse(repository.hasNextLessonBatch(emptyList(), grammar, emptyList(), LessonMode.Grammar, batch = 2))
    }

    @Test
    fun shadowingModeUsesSourceAudioForReZeroWithTtsFallback() {
        val content = repository.content(EpisodeSelection("re-zero", 1), LessonMode.Shadowing)
        val sourceAudio = content.lessonNodes
            .map { it.audio }
            .filterIsInstance<PromptAudio.Source>()

        assertTrue(sourceAudio.isNotEmpty())
        assertTrue(sourceAudio.any { it.url.contains("https://cdn.xn--cckl9nsb.com/rezeroS1/s01e01/") })
        assertTrue(sourceAudio.all { it.fallbackTtsText.isNotBlank() })
    }

    @Test
    fun shadowingModeUsesExplicitSourceAudioFromRemotePayload() {
        val sentence = ShadowingSentence(
            id = "k-on-explicit-source",
            ja = "このプリントをみんなに配っておいてね。",
            reading = "",
            meaningZh = "去把这些资料发给大家。",
            sourceLabel = "EP01 第 56 行",
            audioKind = AudioKind.Source,
            sourceLineNo = 56,
            audioUrl = "https://cdn.example.test/k-on/ep01/sent-056.mp3",
        )
        val content = repository.contentFromRemote(
            selection = EpisodeSelection("k-on", 1),
            vocab = emptyList(),
            grammar = emptyList(),
            shadowing = listOf(sentence),
            mode = LessonMode.Shadowing,
        )
        val sourceAudio = content.lessonNodes
            .map { it.audio }
            .filterIsInstance<PromptAudio.Source>()

        assertTrue(sourceAudio.isNotEmpty())
        assertEquals("https://cdn.example.test/k-on/ep01/sent-056.mp3", sourceAudio.first().url)
        assertEquals("このプリントをみんなに配っておいてね。", sourceAudio.first().fallbackTtsText)
    }

    @Test
    fun shadowingModeMatchesWebLessonPlayerNodeTypes() {
        val content = repository.content(EpisodeSelection("re-zero", 1), LessonMode.Shadowing)
        val allowedTypes = setOf("学习卡", "听音", "拼句", "跟读")

        assertTrue(content.lessonNodes.isNotEmpty())
        assertTrue(content.lessonNodes.all { it.sourceKind == "sentence" })
        assertTrue(content.lessonNodes.all { it.typeLabel in allowedTypes })
        assertTrue(content.lessonNodes.any { it is ShadowingNode })
    }

    @Test
    fun targetSentenceBuildsSingleSentenceLesson() {
        val content = repository.content(EpisodeSelection("re-zero", 1), LessonMode.Mixed)
        val target = content.shadowing.first()
        val nodes = repository.buildLessonNodes(
            selection = EpisodeSelection("re-zero", 1),
            focus = content.focus,
            vocab = content.vocab,
            grammar = content.grammar,
            sentences = content.shadowing,
            mode = LessonMode.Mixed,
            target = LessonTarget.Sentence(target.id),
        )

        assertTrue(nodes.isNotEmpty())
        assertEquals("${target.id}-study", nodes.first().id)
        assertTrue(nodes.all { it.sourceKind == "sentence" && it.sourceId == target.id })
        assertTrue(nodes.any { it is TileOrderNode && it.audioTile })
        assertTrue(nodes.any { it is ShadowingNode })
    }

    @Test
    fun targetVocabBuildsSingleVocabLesson() {
        val content = repository.content(EpisodeSelection("re-zero", 1), LessonMode.Mixed)
        val target = content.vocab.first()
        val nodes = repository.buildLessonNodes(
            selection = EpisodeSelection("re-zero", 1),
            focus = content.focus,
            vocab = content.vocab,
            grammar = content.grammar,
            sentences = content.shadowing,
            mode = LessonMode.Mixed,
            target = LessonTarget.Vocab(target.id),
        )

        assertTrue(nodes.isNotEmpty())
        assertEquals("${target.id}-study", nodes.first().id)
        assertTrue(nodes.all { it.sourceKind == "vocab" && it.sourceId == target.id })
        assertTrue(nodes.any { it is SingleChoiceNode })
    }

    @Test
    fun targetGrammarBuildsSingleGrammarLesson() {
        val content = repository.content(EpisodeSelection("re-zero", 1), LessonMode.Mixed)
        val target = content.grammar.first()
        val nodes = repository.buildLessonNodes(
            selection = EpisodeSelection("re-zero", 1),
            focus = content.focus,
            vocab = content.vocab,
            grammar = content.grammar,
            sentences = content.shadowing,
            mode = LessonMode.Mixed,
            target = LessonTarget.Grammar(target.id),
        )

        assertTrue(nodes.isNotEmpty())
        assertEquals("${target.id}-study", nodes.first().id)
        assertTrue(nodes.all { it.sourceKind == "grammar" && it.sourceId == target.id })
        assertTrue(nodes.any { it is ClozeNode || it is SingleChoiceNode || it is TileOrderNode })
    }

    @Test
    fun readAirFallbackUsesLinguisticExerciseContract() {
        val exercises = repository.readAirExercises(EpisodeSelection("k-on", 1))

        assertTrue(exercises.isNotEmpty())
        assertTrue(exercises.all { it.domain.isNotBlank() })
        assertTrue(exercises.all { it.questionType.isNotBlank() })
        assertTrue(exercises.all { it.difficulty.isNotBlank() })
        assertTrue(exercises.all { it.options.size >= 2 })
        assertTrue(exercises.all { it.correctOption.isNotBlank() })
        assertTrue(exercises.all { it.sceneLines.any { line -> line.isTarget } })
        assertEquals(true, exercises.first().isCorrect(exercises.first().correctOption))
    }

    @Test
    fun readAirFallbackVariesAcrossEpisodes() {
        val episode4 = repository.readAirExercises(EpisodeSelection("re-zero", 4))
        val episode5 = repository.readAirExercises(EpisodeSelection("re-zero", 5))
        val episode56 = repository.readAirExercises(EpisodeSelection("re-zero", 56))

        assertTrue(episode4.isNotEmpty())
        assertTrue(episode5.isNotEmpty())
        assertTrue(episode56.isNotEmpty())
        assertNotEquals(episode4.map { it.sourceId }, episode5.map { it.sourceId })
        assertNotEquals(episode5.map { it.sourceId }, episode56.map { it.sourceId })
        assertTrue((episode4 + episode5 + episode56).map { it.questionType }.toSet().size > 1)
        assertTrue((episode4 + episode5 + episode56).map { it.difficulty }.toSet().size > 1)
    }

    @Test
    fun allReadAirFallbackCoversWorksAndEpisodes() {
        val exercises = repository.allReadAirExercises()
        val workSlugs = exercises.map { it.workSlug }.toSet()
        val kOnEpisodes = exercises.filter { it.workSlug == "k-on" }.map { it.episode }.toSet()
        val reZeroEpisodes = exercises.filter { it.workSlug == "re-zero" }.map { it.episode }.toSet()

        assertTrue("missing K-ON read-air exercises", "k-on" in workSlugs)
        assertTrue("missing Re:Zero read-air exercises", "re-zero" in workSlugs)
        assertTrue("K-ON should expose multiple episodes", kOnEpisodes.size > 1)
        assertTrue("Re:Zero should expose multiple episodes", reZeroEpisodes.size > 1)
    }

    @Test
    fun mixedModeContainsWebLessonPlayerNodeTypes() {
        val content = repository.content(EpisodeSelection("k-on", 1), LessonMode.Mixed)
        val labels = content.lessonNodes.map { it.typeLabel }.toSet()

        assertTrue("missing study card", "学习卡" in labels)
        assertTrue("missing single choice", "选择" in labels)
        assertTrue("missing cloze", "填空" in labels)
        assertTrue("missing tile exercise", labels.any { it == "拼句" || it == "听音" })
        assertFalse(content.lessonNodes.any { it is ShadowingNode })
    }

    @Test
    fun chineseTileBanksDoNotShowQuotedFragments() {
        val content = repository.content(EpisodeSelection("k-on", 1), LessonMode.Mixed)
        val translationTiles = content.lessonNodes
            .filterIsInstance<TileOrderNode>()
            .filter { !it.audioTile }
        val allBankTiles = translationTiles.flatMap { it.bankTiles }

        assertTrue(translationTiles.isNotEmpty())
        assertTrue(
            translationTiles.any {
                it.targetTiles == listOf("去把这些", "资料", "发给", "大家")
            },
        )
        assertFalse(allBankTiles.any { Regex("""["'“”‘’「」『』]""").containsMatchIn(it) })
        assertFalse(allBankTiles.any { it.length == 1 })
        assertFalse(allBankTiles.any { it == "些简单的" })
    }
}
