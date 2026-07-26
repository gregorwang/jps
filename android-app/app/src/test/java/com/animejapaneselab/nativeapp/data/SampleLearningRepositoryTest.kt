package com.animejapaneselab.nativeapp.data

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.system.measureTimeMillis

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
    fun vocabModeCoversEveryVocabStudyItem() {
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
    fun databaseExercisesOwnSingleChoicePromptAnswerAndOptionPool() {
        val selection = EpisodeSelection("re-zero", 26)
        val focus = repository.content(selection).focus
        val exercises = databaseVocabExercises(5)

        val nodes = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
            exercises = exercises,
        ).filterIsInstance<SingleChoiceNode>()

        assertEquals(exercises.size, nodes.size)
        assertEquals(exercises.map { it.prompt }, nodes.map { it.prompt })
        assertEquals(exercises.map { it.answer }, nodes.map { it.answer })
        assertTrue(nodes.all { it.sourceKind == "vocab" })
        assertTrue(nodes.all { node -> node.answer in node.choices })
        assertTrue(nodes.all { node -> node.choices.all { choice -> choice in exercises.map { it.answer } } })
        assertEquals(exercises.map { it.hint }, nodes.map { it.explanation })
        assertTrue(nodes.none { it.explanation.contains("正确答案") })
        assertTrue(nodes.all { node ->
            val audioText = (node.audio as? PromptAudio.Tts)?.text.orEmpty()
            audioText.isNotBlank() && audioText != node.answer
        })
    }

    @Test
    fun chinesePromptKeepsCorrectJapaneseAudioUntilAfterSelection() {
        val selection = EpisodeSelection("k-on", 1)
        val focus = repository.content(selection).focus
        val exercises = (1..4).map { index ->
            LearningExercise(
                id = "meaning-to-ja-$index",
                exerciseType = "meaning_to_japanese",
                prompt = "中文意思：表达$index",
                answer = "単語$index",
                hint = "",
                difficulty = "N5",
            )
        }

        val nodes = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
            exercises = exercises,
        ).filterIsInstance<SingleChoiceNode>()

        assertEquals(4, nodes.size)
        assertTrue(nodes.all { node ->
            val audio = node.audio as? PromptAudio.Tts
            audio?.text == node.answer && !audio.autoPlay
        })
    }

    @Test
    fun kanaToKanjiUsesLinkedDatabaseVocabularyAsMeaningPractice() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val vocab = listOf(
            testVocab("re-zero-vocab-兄ちゃん", "兄ちゃん", "にいちゃん", "小哥；老兄"),
            testVocab("re-zero-vocab-見つける", "見つける", "みつける", "找到；发现"),
            testVocab("re-zero-vocab-手伝う", "手伝う", "てつだう", "帮忙"),
            testVocab("re-zero-vocab-探し物", "探し物", "さがしもの", "寻找的东西"),
        )
        val exercise = LearningExercise(
            id = "re-zero-s01e01-ex-vocab-kana-003",
            exerciseType = "kana_to_kanji",
            prompt = "假名：にいちゃん",
            answer = "兄ちゃん",
            hint = "小哥；老兄。",
            difficulty = "N5",
            vocabItemId = "re-zero-vocab-兄ちゃん",
        )

        val node = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
            exercises = listOf(exercise),
        ).filterIsInstance<SingleChoiceNode>().first { it.id == exercise.id }

        assertEquals("「兄ちゃん」是什么意思？", node.prompt)
        assertEquals("小哥；老兄", node.answer)
        assertTrue(node.answer in node.choices)
        assertFalse(node.prompt.contains("假名："))
        assertEquals("vocab", node.sourceKind)
        assertEquals(PromptAudio.Tts("兄ちゃん", autoPlay = true, label = "重播单词"), node.audio)
    }

    @Test
    fun databaseVocabMeaningRowsFillPairRendererWithoutInventingContent() {
        val selection = EpisodeSelection("re-zero", 26)
        val focus = repository.content(selection).focus
        val exercises = databaseVocabExercises(5)

        val node = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.PairMatch,
            exercises = exercises,
        ).single() as PairMatchNode

        assertEquals(exercises.map { it.answer }, node.pairs.map { it.left })
        assertEquals((1..5).map { "単語$it" }, node.pairs.map { it.right })
        assertEquals(exercises.map { it.id }, node.pairs.map { it.id })
        assertTrue(node.sourceKind == "exercise")
    }

    @Test
    fun exerciseLabMixBuildsOneBoundedNodePerKindForLargePayload() {
        val selection = EpisodeSelection("k-on", 1)
        val fallback = repository.content(selection, LessonMode.Mixed)
        val vocab = (1..200).map { index ->
            VocabItem(
                id = "large-vocab-$index",
                surface = "単語$index",
                reading = "たんご$index",
                romanization = "tango$index",
                meaningZh = "大型词义$index",
                partOfSpeech = if (index % 2 == 0) "名词" else "动词",
                level = "N5",
                occurrence = "性能测试",
                toneTags = emptyList(),
            )
        }
        var nodes: List<LessonNode> = emptyList()

        val elapsedMs = measureTimeMillis {
            nodes = repository.buildExerciseLabMix(
                selection = selection,
                focus = fallback.focus,
                vocab = vocab,
                grammar = fallback.grammar,
                sentences = fallback.shadowing,
                exercises = databaseVocabExercises(40),
            )
        }

        assertEquals(6, nodes.size)
        assertEquals(1, nodes.count { it is PairMatchNode })
        assertEquals(1, nodes.count { it is SingleChoiceNode })
        assertEquals(1, nodes.count { it is ClozeNode })
        assertEquals(1, nodes.count { it is TileOrderNode && it.audioTile })
        assertEquals(1, nodes.count { it is TileOrderNode && !it.audioTile })
        assertEquals(1, nodes.count { it is ShadowingNode })
        assertTrue("exercise lab mix took ${elapsedMs}ms", elapsedMs < 1_500)
    }

    @Test
    fun exerciseLabSingleChoiceHonorsLimitWithLargeVocabPool() {
        val selection = EpisodeSelection("k-on", 1)
        val focus = repository.content(selection).focus
        val vocab = (1..200).map { index ->
            VocabItem(
                id = "bounded-vocab-$index",
                surface = "表現$index",
                reading = "ひょうげん$index",
                romanization = "hyougen$index",
                meaningZh = "表达$index",
                partOfSpeech = "名词",
                level = "N5",
                occurrence = "性能测试",
                toneTags = emptyList(),
            )
        }

        val nodes = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
            limit = 1,
        )

        assertEquals(1, nodes.size)
        assertTrue(nodes.single() is SingleChoiceNode)
    }

    @Test
    fun exerciseLabPrioritizesNextUnseenVocabInsteadOfRepeatingFirstSix() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val vocab = (1..12).map { index ->
            testVocab("vocab-$index", "単語$index", "たんご$index", "词义$index")
        }
        val firstDeck = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
        )
        val progress = firstDeck.mapIndexed { index, node ->
            ProgressItem(
                itemId = node.id,
                itemType = node.sourceKind,
                workSlug = selection.workSlug,
                episode = selection.episode,
                state = ReviewState.Good,
                label = node.prompt,
                lastReviewedAt = "2026-07-11T00:00:0${index}Z",
                payload = mapOf("nodeId" to node.id, "sourceId" to node.sourceId),
            )
        }

        val secondDeck = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
            progressItems = progress,
        )

        assertEquals((1..6).map { "vocab-$it" }, firstDeck.map { it.sourceId })
        assertEquals((7..12).map { "vocab-$it" }, secondDeck.map { it.sourceId })
    }

    @Test
    fun pairMatchLabCoversEntireVocabPoolIncludingExtensionWords() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val vocab = (1..120).map { index ->
            testVocab("vocab-$index", "単語$index", "たんご$index", "词义$index")
        }

        val nodes = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.PairMatch,
            limit = 30,
        ).filterIsInstance<PairMatchNode>()

        assertEquals(120, nodes.flatMap { it.pairs }.map { it.id }.distinct().size)
    }

    @Test
    fun repeatedExerciseLabDecksRotateThroughAllVocabMaterials() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val vocab = (1..120).map { index ->
            testVocab("vocab-$index", "単語$index", "たんご$index", "词义$index")
        }

        fun coverAll(kind: LessonExerciseKind, maximumDecks: Int): Set<String> {
            var progress = emptyList<ProgressItem>()
            val seenSourceIds = linkedSetOf<String>()
            repeat(maximumDecks) { deckIndex ->
                val deck = repository.buildExerciseKindNodes(
                    selection = selection,
                    focus = focus,
                    vocab = vocab,
                    grammar = emptyList(),
                    sentences = emptyList(),
                    kind = kind,
                    progressItems = progress,
                )
                assertTrue(deck.isNotEmpty())
                deck.forEach { node ->
                    seenSourceIds += node.sourceId.split(',').map(String::trim).filter(String::isNotBlank)
                }
                progress = deck.mapIndexed { nodeIndex, node ->
                    ProgressItem(
                        itemId = node.id,
                        itemType = node.sourceKind,
                        workSlug = selection.workSlug,
                        episode = selection.episode,
                        state = ReviewState.Good,
                        label = node.prompt,
                        lastReviewedAt = "2026-07-${(deckIndex + 1).toString().padStart(2, '0')}T00:00:${nodeIndex.toString().padStart(2, '0')}Z",
                        payload = mapOf("nodeId" to node.id, "sourceId" to node.sourceId),
                    )
                } + progress
                if (seenSourceIds.size == vocab.size) return seenSourceIds
            }
            return seenSourceIds
        }

        assertEquals(120, coverAll(LessonExerciseKind.PairMatch, maximumDecks = 8).size)
        assertEquals(120, coverAll(LessonExerciseKind.SingleChoice, maximumDecks = 24).size)
    }

    @Test
    fun mixedBatchesAdvanceContiguouslyWithoutApplyingBatchTwice() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val vocab = (1..8).map { index ->
            testVocab("vocab-$index", "単語$index", "たんご$index", "词义$index")
        }
        val grammar = (1..4).map { index ->
            GrammarPoint(
                id = "grammar-$index",
                pattern = "～ないと$index",
                titleZh = "必须$index",
                exampleJa = "起きないと$index。",
                exampleZh = "必须起床$index。",
                explanationZh = "解释$index",
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
            )
        }

        fun batch(number: Int) = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = grammar,
            sentences = sentences,
            mode = LessonMode.Mixed,
            batch = number,
        ).filterIsInstance<StudyCardNode>().map { it.sourceId }

        assertEquals(listOf("vocab-1", "vocab-2", "grammar-1", "sentence-1"), batch(1))
        assertEquals(listOf("vocab-3", "vocab-4", "grammar-2", "sentence-2"), batch(2))
        assertEquals(listOf("vocab-5", "vocab-6", "grammar-3", "sentence-3"), batch(3))
    }

    @Test
    fun remoteContentUsesPublishedExercisesInFormalVocabLesson() {
        val exercises = databaseVocabExercises(5)
        val content = repository.contentFromRemote(
            selection = EpisodeSelection("re-zero", 26),
            vocab = emptyList(),
            grammar = emptyList(),
            shadowing = emptyList(),
            exercises = exercises,
            mode = LessonMode.Vocab,
        )

        assertEquals(exercises, content.exercises)
        assertTrue(content.lessonNodes.any { node -> node.id in exercises.map { it.id } })
        assertTrue(content.lessonNodes.any { it is PairMatchNode && it.sourceKind == "exercise" })
    }

    @Test
    fun grammarModeBuildsGrammarClozeNodes() {
        val content = repository.content(EpisodeSelection("k-on", 1), LessonMode.Grammar)

        assertTrue(content.lessonNodes.isNotEmpty())
        assertTrue(content.lessonNodes.any { it is StudyCardNode && it.sourceKind == "grammar" })
        assertTrue(content.lessonNodes.any { it is ClozeNode && it.sourceKind == "grammar" })
    }

    @Test
    fun grammarModeSupportsSixItemSpecialtyBatches() {
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
    fun grammarModeUsesMatchedReZeroSourceAudioWhenLineExists() {
        val content = repository.content(EpisodeSelection("re-zero", 1), LessonMode.Grammar)
        val grammarAudio = content.lessonNodes
            .filter { it.sourceKind == "grammar" }
            .map { it.audio }
            .filterIsInstance<PromptAudio.Source>()

        assertTrue(grammarAudio.isNotEmpty())
        assertTrue(grammarAudio.any { it.url.contains("https://cdn.xn--cckl9nsb.com/rezeroS1/s01e01/") })
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
    fun shadowingModeBuildsRequiredAndroidNodeTypes() {
        val content = repository.content(EpisodeSelection("re-zero", 1), LessonMode.Shadowing)
        val allowedTypes = setOf("学习卡", "听音", "拼句", "跟读")

        assertTrue(content.lessonNodes.isNotEmpty())
        assertTrue(content.lessonNodes.all { it.sourceKind == "sentence" })
        assertTrue(content.lessonNodes.all { it.typeLabel in allowedTypes })
        assertTrue(content.lessonNodes.any { it is ShadowingNode })
    }

    @Test
    fun interactiveSentenceQuestionsRequestOneShotAutoPlaybackWithoutAnswerHints() {
        val selection = EpisodeSelection("k-on", 1)
        val content = repository.content(selection, LessonMode.Mixed)
        fun first(kind: LessonExerciseKind): LessonNode = repository.buildExerciseKindNodes(
            selection = selection,
            focus = content.focus,
            vocab = content.vocab,
            grammar = content.grammar,
            sentences = content.shadowing,
            kind = kind,
        ).first()
        fun PromptAudio.autoPlays(): Boolean = when (this) {
            PromptAudio.None -> false
            is PromptAudio.Source -> autoPlay
            is PromptAudio.Tts -> autoPlay
        }

        val translation = first(LessonExerciseKind.TranslationOrder) as TileOrderNode
        val audioOrder = first(LessonExerciseKind.AudioOrder) as TileOrderNode
        val cloze = first(LessonExerciseKind.Cloze) as ClozeNode
        val pair = first(LessonExerciseKind.PairMatch) as PairMatchNode

        assertTrue(translation.audio.autoPlays())
        assertTrue(audioOrder.audio.autoPlays())
        assertTrue(cloze.audio.autoPlays())
        assertTrue(cloze.choices.all { it.note.isBlank() })
        assertTrue(pair.prompt.isBlank())
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
    fun mixedModeContainsRequiredAndroidNodeTypes() {
        val content = repository.content(EpisodeSelection("k-on", 1), LessonMode.Mixed)
        val labels = content.lessonNodes.map { it.typeLabel }.toSet()

        assertTrue("missing study card", "学习卡" in labels)
        assertTrue("missing single choice", "选择" in labels)
        assertTrue("missing cloze", "填空" in labels)
        assertTrue("missing tile exercise", labels.any { it == "拼句" || it == "听音" })
        assertFalse(content.lessonNodes.any { it is ShadowingNode })
    }

    @Test
    fun chineseTileBanksUseOnlyTargetAnswerTiles() {
        val content = repository.contentFromRemote(
            selection = EpisodeSelection("k-on", 1),
            vocab = emptyList(),
            grammar = emptyList(),
            shadowing = listOf(
                ShadowingSentence(
                    id = "k-on-ep01-sent-00025",
                    ja = "こうやってニートが出来上がっていくのね",
                    reading = "",
                    meaningZh = "这样下去就会变成家里蹲了",
                    sourceLabel = "EP01 第 25 行",
                    audioKind = AudioKind.Tts,
                    sourceLineNo = 25,
                ),
                ShadowingSentence(
                    id = "k-on-ep01-sent-00046",
                    ja = "正確には廃部寸前ね",
                    reading = "",
                    meaningZh = "正确来说是即将废部",
                    sourceLabel = "EP01 第 46 行",
                    audioKind = AudioKind.Tts,
                    sourceLineNo = 46,
                ),
                ShadowingSentence(
                    id = "k-on-ep01-sent-00056",
                    ja = "このプリントをみんなに配っておいてね",
                    reading = "",
                    meaningZh = "去把这些资料发给大家",
                    sourceLabel = "EP01 第 56 行",
                    audioKind = AudioKind.Tts,
                    sourceLineNo = 56,
                ),
            ),
            mode = LessonMode.Mixed,
        )
        val translationTiles = content.lessonNodes
            .filterIsInstance<TileOrderNode>()
            .filter { !it.audioTile }
        val allBankTiles = translationTiles.flatMap { it.bankTiles }
        val neatSentence = translationTiles.first { it.sourceId == "k-on-ep01-sent-00025" }

        assertTrue(translationTiles.isNotEmpty())
        assertEquals(
            listOf("这样下去", "就会", "变成", "家里蹲了"),
            neatSentence.targetTiles,
        )
        assertEquals(neatSentence.targetTiles.toSet(), neatSentence.bankTiles.toSet())
        assertFalse(neatSentence.bankTiles.any { it in listOf("资料", "正确来说", "是即将废部", "废部") })
        assertTrue(translationTiles.all { it.bankTiles.toSet() == it.targetTiles.toSet() })
        assertFalse(allBankTiles.any { Regex("""["'“”‘’「」『』]""").containsMatchIn(it) })
        assertFalse(allBankTiles.any { it.length == 1 })
        assertFalse(allBankTiles.any { it == "些简单的" })
    }
}

private fun databaseVocabExercises(count: Int): List<LearningExercise> {
    return (1..count).map { index ->
        LearningExercise(
            id = "database-exercise-$index",
            exerciseType = "vocab_meaning",
            prompt = "「単語$index」在本集语境中的中文意思是？",
            answer = "数据库词义$index",
            hint = "参考读音：たんご$index。",
            difficulty = "N5",
            vocabItemId = "vocab-$index",
        )
    }
}

private fun testVocab(
    id: String,
    surface: String,
    reading: String,
    meaningZh: String,
) = VocabItem(
    id = id,
    surface = surface,
    reading = reading,
    romanization = "",
    meaningZh = meaningZh,
    partOfSpeech = "",
    level = "N5",
    occurrence = "",
    toneTags = emptyList(),
)
