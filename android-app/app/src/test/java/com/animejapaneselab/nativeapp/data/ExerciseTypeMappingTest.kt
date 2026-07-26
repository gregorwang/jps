package com.animejapaneselab.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers how each published `learning_exercises.exercise_type` reaches an Android renderer.
 * Every case asserts the database keeps ownership of the prompt and the correct answer; only the
 * option pool may be generated locally.
 */
class ExerciseTypeMappingTest {
    private val repository = SampleLearningRepository()

    @Test
    fun kanaToKanjiWithoutLinkedVocabKeepsKanaPromptAndKanjiAnswer() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val exercises = listOf(
            kanaToKanjiExercise(1, "にいちゃん", "兄ちゃん", "亲近的称呼。"),
            kanaToKanjiExercise(2, "みつける", "見つける", "他动词。"),
            kanaToKanjiExercise(3, "てつだう", "手伝う", "帮忙做事。"),
            kanaToKanjiExercise(4, "さがしもの", "探し物", "要找的东西。"),
        )

        val nodes = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
            exercises = exercises,
        ).filterIsInstance<SingleChoiceNode>()

        val publishedAnswers = exercises.map { it.answer }
        assertEquals(exercises.size, nodes.size)
        assertEquals(exercises.map { it.id }, nodes.map { it.id })
        assertEquals(exercises.map { it.prompt }, nodes.map { it.prompt })
        assertEquals(publishedAnswers, nodes.map { it.answer })
        assertEquals(exercises.map { it.hint }, nodes.map { it.explanation })
        assertTrue(nodes.all { it.prompt.startsWith("假名：") })
        assertTrue(nodes.all { it.answer in it.choices })
        assertTrue(nodes.all { it.choices.size == 4 })
        assertTrue(nodes.all { node -> node.choices.distinct().size == node.choices.size })
        assertTrue(nodes.all { node -> node.choices.all { choice -> choice in publishedAnswers } })
        assertTrue(nodes.all { it.sourceKind == "vocab" })
        assertTrue(nodes.all { it.title == "选择对应的汉字表达" })
        assertTrue(nodes.all { it.sourceLabel.contains("假名转汉字") })
        assertTrue(
            nodes.none { node ->
                val audio = node.audio
                audio is PromptAudio.Tts && audio.autoPlay && audio.text == node.answer
            },
        )
    }

    @Test
    fun kanaToKanjiFillsThinOptionPoolFromEpisodeVocabSurfaces() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val vocab = listOf(
            mappingVocab("mapping-vocab-1", "見つける", "みつける", "找到；发现"),
            mappingVocab("mapping-vocab-2", "手伝う", "てつだう", "帮忙"),
            mappingVocab("mapping-vocab-3", "探し物", "さがしもの", "寻找的东西"),
        )
        val exercise = kanaToKanjiExercise(1, "にいちゃん", "兄ちゃん", hint = "")

        val node = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = vocab,
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
            exercises = listOf(exercise),
        ).filterIsInstance<SingleChoiceNode>().first { it.id == exercise.id }

        val surfaces = vocab.map { it.surface }
        val meanings = vocab.map { it.meaningZh }
        assertEquals(exercise.prompt, node.prompt)
        assertEquals(exercise.answer, node.answer)
        assertEquals(4, node.choices.size)
        assertEquals(node.choices.size, node.choices.distinct().size)
        assertTrue(node.answer in node.choices)
        assertTrue(node.choices.filterNot { it == node.answer }.all { choice -> choice in surfaces })
        assertFalse(node.choices.any { choice -> choice in meanings })
        assertTrue("blank hints must still get a neutral explanation", node.explanation.isNotBlank())
    }

    @Test
    fun meaningToVocabKeepsChinesePromptAndJapaneseAnswer() {
        val selection = EpisodeSelection("k-on", 1)
        val focus = repository.content(selection).focus
        val exercises = (1..4).map { index ->
            LearningExercise(
                id = "k-on-ep01-ex-meaning-to-vocab-00$index",
                exerciseType = "meaning_to_vocab",
                prompt = "中文意思：表达$index",
                answer = "単語$index",
                hint = "提示$index",
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

        val publishedAnswers = exercises.map { it.answer }
        assertEquals(exercises.size, nodes.size)
        assertEquals(exercises.map { it.prompt }, nodes.map { it.prompt })
        assertEquals(publishedAnswers, nodes.map { it.answer })
        assertEquals(exercises.map { it.hint }, nodes.map { it.explanation })
        assertTrue(nodes.all { it.answer in it.choices })
        assertTrue(nodes.all { it.choices.size == 4 })
        assertTrue(nodes.all { node -> node.choices.distinct().size == node.choices.size })
        assertTrue(nodes.all { node -> node.choices.all { choice -> choice in publishedAnswers } })
        assertTrue(nodes.all { it.sourceKind == "vocab" })
        assertTrue(
            nodes.all { node ->
                val audio = node.audio
                audio is PromptAudio.Tts && audio.text == node.answer && !audio.autoPlay
            },
        )
    }

    @Test
    fun grammarShortAnswerBorrowsGrammarMeaningAnswersForOptions() {
        val selection = EpisodeSelection("k-on", 1)
        val focus = repository.content(selection).focus
        val shortAnswer = LearningExercise(
            id = "k-on-ep01-ex-grammar-short-001",
            exerciseType = "grammar_short_answer",
            prompt = "そろそろ起き＿＿。空格里应该填哪个形式？",
            answer = "ないと",
            hint = "ないと 后面省略了“不行”。",
            difficulty = "N4",
        )
        val grammarMeaning = (1..3).map { index ->
            LearningExercise(
                id = "k-on-ep01-ex-grammar-meaning-00$index",
                exerciseType = "grammar_meaning",
                prompt = "语法点$index 主要表达什么？",
                answer = "语法作用$index",
                hint = "语法提示$index",
                difficulty = "N4",
            )
        }

        val node = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            mode = LessonMode.Grammar,
            exercises = listOf(shortAnswer) + grammarMeaning,
        ).filterIsInstance<SingleChoiceNode>().first { it.id == shortAnswer.id }

        val grammarMeaningAnswers = grammarMeaning.map { it.answer }
        assertEquals(shortAnswer.prompt, node.prompt)
        assertEquals(shortAnswer.answer, node.answer)
        assertEquals(shortAnswer.hint, node.explanation)
        assertEquals("grammar", node.sourceKind)
        assertEquals(4, node.choices.size)
        assertEquals(node.choices.size, node.choices.distinct().size)
        assertEquals(1, node.choices.count { it == node.answer })
        assertTrue(
            node.choices.filterNot { it == node.answer }.all { choice -> choice in grammarMeaningAnswers },
        )
    }

    @Test
    fun sentenceMeaningJoinsSentenceUnderstandingChoicePool() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val sentenceMeaning = LearningExercise(
            id = "re-zero-s01e01-ex-sentence-meaning-001",
            exerciseType = "sentence_meaning",
            prompt = "お前を救ってみせる。这句台词的意思是？",
            answer = "我一定会救你的。",
            hint = "みせる 强调说话人的决心。",
            difficulty = "N3",
        )
        val understanding = (1..3).map { index ->
            LearningExercise(
                id = "re-zero-s01e01-ex-sentence-understanding-00$index",
                exerciseType = "sentence_understanding",
                prompt = "台词$index 这句话的意思是？",
                answer = "台词含义$index",
                hint = "台词提示$index",
                difficulty = "N3",
            )
        }

        val node = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            mode = LessonMode.Shadowing,
            exercises = listOf(sentenceMeaning) + understanding,
        ).filterIsInstance<SingleChoiceNode>().first { it.id == sentenceMeaning.id }

        val understandingAnswers = understanding.map { it.answer }
        assertEquals(sentenceMeaning.prompt, node.prompt)
        assertEquals(sentenceMeaning.answer, node.answer)
        assertEquals(sentenceMeaning.hint, node.explanation)
        assertEquals("sentence", node.sourceKind)
        assertEquals(4, node.choices.size)
        assertEquals(node.choices.size, node.choices.distinct().size)
        assertEquals(1, node.choices.count { it == node.answer })
        assertTrue(
            node.choices.filterNot { it == node.answer }.all { choice -> choice in understandingAnswers },
        )
    }

    @Test
    fun readingAirToneIsDroppedExplicitlyInsteadOfRenderingAsVocabularyChoice() {
        val selection = EpisodeSelection("re-zero", 1)
        val focus = repository.content(selection).focus
        val vocabMeaning = (1..4).map { index ->
            LearningExercise(
                id = "re-zero-s01e01-ex-vocab-meaning-00$index",
                exerciseType = "vocab_meaning",
                prompt = "「単語$index」在本集语境中的中文意思是？",
                answer = "数据库词义$index",
                hint = "词义提示$index",
                difficulty = "N4",
            )
        }
        val readingAirTone = (1..3).map { index ->
            LearningExercise(
                id = "re-zero-s01e01-ex-read-air-00$index",
                exerciseType = "reading_air_tone",
                prompt = "这句台词的语气最接近哪一种？",
                answer = "语气解读$index",
                hint = "语气提示$index",
                difficulty = "N3",
            )
        }
        val exercises = vocabMeaning + readingAirTone

        val nodes = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            mode = LessonMode.Mixed,
            exercises = exercises,
        )
        val content = repository.contentFromRemote(
            selection = selection,
            vocab = emptyList(),
            grammar = emptyList(),
            shadowing = emptyList(),
            exercises = exercises,
            mode = LessonMode.Mixed,
        )

        val readingAirIds = readingAirTone.map { it.id }
        val readingAirAnswers = readingAirTone.map { it.answer }
        val choices = nodes.filterIsInstance<SingleChoiceNode>()
        assertTrue(choices.isNotEmpty())
        assertTrue(choices.all { it.id in vocabMeaning.map { exercise -> exercise.id } })
        assertTrue(nodes.none { it.id in readingAirIds })
        assertTrue(nodes.none { it.expectedAnswer in readingAirAnswers })
        assertFalse(nodes.any { it.sourceLabel.contains("reading_air_tone") })
        assertTrue(choices.none { node -> node.choices.any { choice -> choice in readingAirAnswers } })
        // The rows stay in the payload; only the renderer refuses them.
        assertEquals(exercises, content.exercises)
        assertTrue(content.lessonNodes.none { it.id in readingAirIds })
    }

    @Test
    fun unknownExerciseTypeIsRenderedWithoutCrashingOrLeakingRawType() {
        val selection = EpisodeSelection("k-on", 1)
        val focus = repository.content(selection).focus
        val exercises = listOf(
            LearningExercise(
                id = "unknown-1",
                exerciseType = "future_type_v9",
                prompt = "题干一",
                answer = "答案一",
                hint = "提示一",
            ),
            LearningExercise(
                id = "unknown-2",
                exerciseType = "future_type_v9",
                prompt = "题干二",
                answer = "答案二",
                hint = "",
            ),
            LearningExercise(
                id = "blank-type",
                exerciseType = "",
                prompt = "题干三",
                answer = "答案三",
            ),
            LearningExercise(
                id = "blank-answer",
                exerciseType = "future_type_v9",
                prompt = "题干四",
                answer = "",
            ),
        )

        val mixed = repository.buildLessonNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            mode = LessonMode.Mixed,
            exercises = exercises,
        )
        val lab = repository.buildExerciseKindNodes(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            kind = LessonExerciseKind.SingleChoice,
            exercises = exercises,
        )
        val labMix = repository.buildExerciseLabMix(
            selection = selection,
            focus = focus,
            vocab = emptyList(),
            grammar = emptyList(),
            sentences = emptyList(),
            exercises = exercises,
        )
        val content = repository.contentFromRemote(
            selection = selection,
            vocab = emptyList(),
            grammar = emptyList(),
            shadowing = emptyList(),
            exercises = exercises,
            mode = LessonMode.Mixed,
        )

        val choices = mixed.filterIsInstance<SingleChoiceNode>()
        assertEquals(listOf("unknown-1", "unknown-2"), mixed.map { it.id })
        assertEquals(listOf("题干一", "题干二"), choices.map { it.prompt })
        assertEquals(listOf("答案一", "答案二"), choices.map { it.answer })
        assertTrue(choices.all { it.answer in it.choices })
        assertTrue(choices.all { it.choices.size >= 2 })
        assertTrue(choices.all { it.sourceKind == "exercise" })
        assertTrue(choices.all { it.title == "选择正确答案" })
        assertTrue(choices.none { it.sourceLabel.contains("future_type_v9") })
        assertEquals(listOf("数据库题库", "数据库题库"), choices.map { it.sourceLabel })
        // Rows with no type or no answer never reach a renderer.
        assertTrue(mixed.none { it.id == "blank-type" || it.id == "blank-answer" })
        // The vocabulary lab only claims vocabulary types, so an unknown type is simply absent.
        assertTrue(lab.isEmpty())
        assertTrue(labMix.isEmpty())
        assertEquals(3, content.exercises.size)
        assertTrue(content.lessonNodes.none { it.id == "blank-answer" })
    }
}

private fun kanaToKanjiExercise(
    index: Int,
    kana: String,
    kanji: String,
    hint: String,
) = LearningExercise(
    id = "re-zero-s01e01-ex-vocab-kana-00$index",
    exerciseType = "kana_to_kanji",
    prompt = "假名：$kana",
    answer = kanji,
    hint = hint,
    difficulty = "N5",
)

private fun mappingVocab(
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
