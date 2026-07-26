package com.animejapaneselab.nativeapp.data

import kotlin.math.abs

private const val AndroidLessonLimit = 10
private const val AndroidSpecialtyBatchSize = 6
private const val VocabSpecialtyBatchSize = 20
private const val DatabaseExercisesPerTypePerBatch = 8
private const val MaxDistractorCandidates = 48

private val VocabExerciseTypes = setOf(
    "vocab_meaning",
    "vocab_reading",
    "meaning_to_vocab",
    "meaning_to_japanese",
    "kana_to_kanji",
)
private val GrammarExerciseTypes = setOf("grammar_meaning", "grammar_short_answer")
private val SentenceExerciseTypes = setOf("sentence_understanding", "sentence_meaning")

class SampleLearningRepository {
    val defaultSelection = EpisodeSelection(workSlug = "k-on", episode = 1)

    fun works(): List<WorkOption> = listOf(
        WorkOption(id = "k-on", slug = "k-on", displayName = "K-ON!", episodeCount = 14),
        WorkOption(id = "re-zero", slug = "re-zero", displayName = "Re:ゼロから始める異世界生活", episodeCount = 66),
    )

    fun episodes(workSlug: String): List<EpisodeOption> {
        val work = works().firstOrNull { it.slug == workSlug } ?: works().first()
        return (1..work.episodeCount).map { episode ->
            EpisodeOption(
                id = "${work.slug}-ep${episode.toString().padStart(2, '0')}",
                workSlug = work.slug,
                workDisplayName = work.displayName,
                episode = episode,
                totalCues = if (work.slug == "k-on") 700 + episode * 18 else 460 + episode * 14,
                usableJaLines = if (work.slug == "k-on") 300 + episode * 8 else 420 + episode * 6,
                chunkCount = if (work.slug == "k-on") 12 + episode / 2 else 10 + episode / 3,
            )
        }
    }

    fun content(selection: EpisodeSelection, mode: LessonMode = LessonMode.Mixed, batch: Int = 1): EpisodeContent {
        val focus = episodeFocus(selection, mode)
        val vocab = vocab(selection)
        val grammar = grammar(selection)
        val shadowing = shadowingSentences(selection)
        val scenes = readAirScenes(selection)
        return EpisodeContent(
            focus = focus,
            vocab = vocab,
            grammar = grammar,
            shadowing = shadowing,
            exercises = emptyList(),
            scenes = scenes,
            lessonNodes = buildLessonNodes(selection, focus, vocab, grammar, shadowing, mode, batch = batch),
        )
    }

    fun contentFromRemote(
        selection: EpisodeSelection,
        vocab: List<VocabItem>,
        grammar: List<GrammarPoint>,
        shadowing: List<ShadowingSentence>,
        exercises: List<LearningExercise> = emptyList(),
        mode: LessonMode = LessonMode.Mixed,
        batch: Int = 1,
    ): EpisodeContent {
        val fallback = content(selection, mode, batch)
        val nextVocab = vocab.ifEmpty { fallback.vocab }
        val nextGrammar = grammar.ifEmpty { fallback.grammar }
        val nextShadowing = shadowing.ifEmpty { fallback.shadowing }
        val nextExercises = exercises.filter { exercise ->
            exercise.id.isNotBlank() && exercise.prompt.isNotBlank() && exercise.answer.isNotBlank()
        }
        val episodeLabel = lessonEpisodeLabel(selection)
        val focus = episodeFocus(selection, mode).copy(
            lessonTitle = "线上${mode.titleLabel} · $episodeLabel",
            guidebook = if (nextExercises.isEmpty()) {
                "已从云端同步本集词汇、语法和跟读句：原声优先，系统语音只作为辅助。"
            } else {
                "已同步 ${nextExercises.size} 道数据库题；题干与答案以题库为准，客户端只负责交互呈现。"
            },
        )
        return EpisodeContent(
            focus = focus,
            vocab = nextVocab,
            grammar = nextGrammar,
            shadowing = nextShadowing,
            exercises = nextExercises,
            scenes = fallback.scenes,
            lessonNodes = buildLessonNodes(
                selection,
                focus,
                nextVocab,
                nextGrammar,
                nextShadowing,
                mode,
                exercises = nextExercises,
                batch = batch,
            ),
        )
    }

    fun hasNextLessonBatch(
        vocab: List<VocabItem>,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence>,
        mode: LessonMode,
        batch: Int,
        target: LessonTarget? = null,
    ): Boolean {
        if (target != null || mode == LessonMode.Mixed || mode == LessonMode.Review) return false
        val itemCount = when (mode) {
            LessonMode.Vocab -> vocab.count { it.surface.isNotBlank() && it.meaningZh.isNotBlank() }
            LessonMode.Grammar -> buildGrammarStudyNodes(EpisodeSelection("", 1), grammar).size
            LessonMode.Shadowing -> buildSentenceStudyNodes(EpisodeSelection("", 1), sentences).size
            LessonMode.Mixed,
            LessonMode.Review -> 0
        }
        val batchSize = when (mode) {
            LessonMode.Vocab -> VocabSpecialtyBatchSize
            LessonMode.Grammar,
            LessonMode.Shadowing -> AndroidSpecialtyBatchSize
            LessonMode.Mixed,
            LessonMode.Review -> itemCount.coerceAtLeast(1)
        }
        return batch.coerceAtLeast(1) * batchSize < itemCount
    }

    fun buildExerciseKindNodes(
        selection: EpisodeSelection,
        focus: EpisodeFocus,
        vocab: List<VocabItem>,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence>,
        kind: LessonExerciseKind,
        exercises: List<LearningExercise> = emptyList(),
        progressItems: List<ProgressItem> = emptyList(),
        limit: Int = 6,
    ): List<LessonNode> {
        val safeLimit = limit.coerceAtLeast(1)
        val progress = PracticeProgressIndex(progressItems)
        val rankedVocab = vocab.prioritizeForPractice(progress, "vocab", VocabItem::id)
        val rankedGrammar = grammar.prioritizeForPractice(progress, "grammar", GrammarPoint::id)
        val rankedSentences = sentences.prioritizeForPractice(progress, "sentence", ShadowingSentence::id)
        val rankedExercises = exercises.sortedWith { left, right ->
            progress.compare(
                left.practiceItemType() to left.practiceItemId(),
                right.practiceItemType() to right.practiceItemId(),
            )
        }
        val candidates = when (kind) {
            LessonExerciseKind.TranslationOrder -> buildSentenceTranslationNodes(selection, rankedSentences, safeLimit)
            LessonExerciseKind.AudioOrder -> buildSentenceAudioTileNodes(selection, rankedSentences, safeLimit)
            LessonExerciseKind.Shadowing -> buildSentenceShadowingNodes(selection, rankedSentences, safeLimit)
            LessonExerciseKind.Cloze -> buildGrammarClozeNodes(
                selection = selection,
                grammar = rankedGrammar,
                sentences = rankedSentences,
                limit = safeLimit,
            )

            LessonExerciseKind.SingleChoice -> {
                val databaseNodes = buildDatabaseSingleChoiceNodes(
                    exercises = rankedExercises.filter { it.exerciseType in VocabExerciseTypes },
                    vocab = rankedVocab,
                    limit = safeLimit,
                )
                val generatedNodes = buildVocabChoiceNodes(selection, rankedVocab, safeLimit)
                (databaseNodes + generatedNodes)
                    .distinctBy(LessonNode::id)
                    .prioritizeForPractice(progress)
                    .take(safeLimit)
            }

            LessonExerciseKind.PairMatch -> {
                val pairExercises = rankedExercises.filter {
                    it.exerciseType == "vocab_meaning" ||
                        it.exerciseType == "meaning_to_vocab" ||
                        it.exerciseType == "meaning_to_japanese"
                }
                val databaseNodes = buildDatabasePairNodes(selection, focus, pairExercises)
                val databaseVocabIds = pairExercises.map(LearningExercise::vocabItemId)
                    .filter(String::isNotBlank)
                    .toSet()
                val generatedNodes = buildVocabPairNodes(
                    selection = selection,
                    focus = focus,
                    vocab = rankedVocab.filterNot { it.id in databaseVocabIds },
                )
                (databaseNodes + generatedNodes)
                    .distinctBy(LessonNode::id)
                    .prioritizeForPractice(progress)
                    .take(safeLimit)
            }
        }
        return candidates
    }

    fun buildExerciseLabMix(
        selection: EpisodeSelection,
        focus: EpisodeFocus,
        vocab: List<VocabItem>,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence>,
        exercises: List<LearningExercise> = emptyList(),
        progressItems: List<ProgressItem> = emptyList(),
    ): List<LessonNode> {
        return LessonExerciseKind.entries.mapNotNull { kind ->
            buildExerciseKindNodes(
                selection = selection,
                focus = focus,
                vocab = vocab,
                grammar = grammar,
                sentences = sentences,
                kind = kind,
                exercises = exercises,
                progressItems = progressItems,
                limit = 1,
            ).firstOrNull()
        }
    }

    fun buildLessonNodes(
        selection: EpisodeSelection,
        focus: EpisodeFocus,
        vocab: List<VocabItem>,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence>,
        mode: LessonMode,
        exercises: List<LearningExercise> = emptyList(),
        target: LessonTarget? = null,
        batch: Int = 1,
    ): List<LessonNode> {
        val lessonVocab = scopedVocabForLesson(vocab, mode, target, batch)
        val lessonGrammar = scopedGrammarForLesson(grammar, mode, target, batch)
        val lessonSentences = scopedSentencesForLesson(sentences, mode, target, batch)
        val lessonExercises = scopedExercisesForLesson(exercises, mode, target, batch)
        val grammarAudioSentences = if (lessonGrammar.isNotEmpty()) sentences else lessonSentences
        val pools = buildLessonPools(
            selection = selection,
            focus = focus,
            vocab = lessonVocab,
            grammar = lessonGrammar,
            sentences = lessonSentences,
            exercises = lessonExercises,
            grammarAudioSentences = grammarAudioSentences,
        )
        val practiceNodes = listOf(
            pools.databasePair,
            pools.databaseChoice,
            pools.vocabPair,
            pools.vocabChoice,
            pools.sentenceAudio,
            pools.sentenceTranslation,
            pools.sentenceShadowing,
            pools.grammarCloze,
            pools.grammarChoice,
        ).flatten()
        if (target != null) {
            return buildTargetStudyPracticeSequence(pools, target).ifEmpty {
                balanceLessonNodes(
                    practiceNodes,
                    quota = targetQuota(target),
                )
            }.ifEmpty {
                practiceNodes.take(AndroidLessonLimit)
            }
        }
        return when (mode) {
            LessonMode.Vocab -> {
                val databasePractice = pools.databaseChoice.ifEmpty { pools.vocabChoice }
                val studySequence = buildStudyPracticeSequence(
                    studyNodes = pools.vocabStudy,
                    practiceNodes = databasePractice,
                    batch = 1,
                    batchSize = pools.vocabStudy.size,
                )
                (studySequence + pools.databasePair.take(1) + databasePractice.filterNot { it in studySequence }.take(4))
                    .distinctBy { it.id }
            }

            LessonMode.Grammar -> (
                buildStudyPracticeSequence(
                    studyNodes = pools.grammarStudy,
                    practiceNodes = pools.grammarCloze + pools.grammarChoice,
                    batch = 1,
                    batchSize = AndroidSpecialtyBatchSize,
                ) + pools.databaseChoice.take(4)
            ).distinctBy { it.id }

            LessonMode.Shadowing -> (
                buildStudyPracticeSequence(
                    studyNodes = pools.sentenceStudy,
                    practiceNodes = prioritizeAudioNodes(pools.sentenceAudio + pools.sentenceTranslation) + pools.sentenceShadowing,
                    batch = 1,
                    batchSize = AndroidSpecialtyBatchSize,
                    practiceLimit = 3,
                ) + pools.databaseChoice.take(3)
            ).distinctBy { it.id }

            LessonMode.Review -> balanceLessonNodes(
                prioritizeAudioNodes(practiceNodes),
                quota = mapOf("配对" to 1, "选择" to 2, "听音" to 2, "拼句" to 2, "填空" to 3),
            )

            // Mixed inputs are already scoped to this batch. Applying [batch] again here used to
            // skip most materials from batch 2 onward.
            LessonMode.Mixed -> buildMixedStudyPracticeSequence(pools, batch = 1)
        }.ifEmpty {
            practiceNodes.take(AndroidLessonLimit)
        }
    }

    private fun buildLessonPools(
        selection: EpisodeSelection,
        focus: EpisodeFocus,
        vocab: List<VocabItem>,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence>,
        exercises: List<LearningExercise> = emptyList(),
        grammarAudioSentences: List<ShadowingSentence> = sentences,
        exerciseCandidateLimit: Int? = null,
        includeStudyNodes: Boolean = true,
    ): LessonPools {
        val labLimit = exerciseCandidateLimit?.coerceAtLeast(1)
        return LessonPools(
            databasePair = buildDatabasePairNodes(selection, focus, exercises, limit = labLimit ?: Int.MAX_VALUE),
            databaseChoice = buildDatabaseSingleChoiceNodes(exercises, vocab, limit = labLimit ?: Int.MAX_VALUE),
            vocabPair = buildVocabPairNodes(selection, focus, vocab),
            vocabChoice = buildVocabChoiceNodes(selection, vocab, limit = labLimit ?: Int.MAX_VALUE),
            sentenceAudio = buildSentenceAudioTileNodes(selection, sentences, limit = labLimit ?: 6),
            sentenceTranslation = buildSentenceTranslationNodes(selection, sentences, limit = labLimit ?: 6),
            sentenceShadowing = buildSentenceShadowingNodes(selection, sentences, limit = labLimit ?: 6),
            grammarCloze = buildGrammarClozeNodes(selection, grammar, grammarAudioSentences, limit = labLimit ?: 8),
            grammarChoice = buildGrammarChoiceNodes(selection, grammar, grammarAudioSentences, limit = labLimit ?: 6),
            vocabStudy = if (includeStudyNodes) buildVocabStudyNodes(selection, vocab) else emptyList(),
            grammarStudy = if (includeStudyNodes) buildGrammarStudyNodes(selection, grammar, grammarAudioSentences) else emptyList(),
            sentenceStudy = if (includeStudyNodes) buildSentenceStudyNodes(selection, sentences) else emptyList(),
        )
    }

    private fun LessonPools.exerciseCandidates(kind: LessonExerciseKind): List<LessonNode> {
        return when (kind) {
            LessonExerciseKind.TranslationOrder -> sentenceTranslation
            LessonExerciseKind.PairMatch -> (databasePair + vocabPair).distinctBy(LessonNode::id)
            LessonExerciseKind.SingleChoice -> (databaseChoice + vocabChoice + grammarChoice).distinctBy(LessonNode::id)
            LessonExerciseKind.Cloze -> grammarCloze
            LessonExerciseKind.AudioOrder -> sentenceAudio
            LessonExerciseKind.Shadowing -> sentenceShadowing
        }
    }

    fun answerReadAir(question: String, scene: ReadAirScene): String {
        val trimmed = question.trim()
        val focus = if (trimmed.isEmpty()) "这段对话的潜台词" else trimmed
        return buildString {
            append("问题：")
            append(focus)
            append("\n\n表层：")
            append(scene.context)
            append("\n\n潜台词：")
            append(scene.subtext)
            append("\n\n语言证据：")
            append(scene.evidence.joinToString("；"))
            append("\n\n学习点：")
            append(scene.learningPoint)
        }
    }

    fun readAirExercises(selection: EpisodeSelection): List<LinguisticExercise> {
        val episodeLabel = "EP${selection.episode.toString().padStart(2, '0')}"
        return readAirScenes(selection).mapIndexed { sceneIndex, scene ->
            val sceneLines = scene.lines.mapIndexed { lineIndex, line ->
                LinguisticSceneLine(
                    lineNo = sceneIndex * 10 + lineIndex + 1,
                    speaker = line.speaker,
                    jaText = line.ja,
                    zhText = line.zh,
                    isTarget = lineIndex == 0,
                )
            }
            val options = stableShuffle(
                listOf(
                    scene.subtext,
                    "只是复述字面信息，没有额外语气。",
                    "主要是在转移话题，避免回应当前问题。",
                    "重点是确认客观事实，不涉及关系变化。",
                    "是在直接命令对方服从。",
                ).distinct(),
                scene.id,
            ).take(4)
            val correctIndex = options.indexOf(scene.subtext).takeIf { it >= 0 }
            LinguisticExercise(
                id = "${selection.workSlug}-$episodeLabel-read-air-${scene.id}",
                batchId = "local-read-air",
                workSlug = selection.workSlug,
                episode = selection.episode,
                sourceId = scene.id,
                sourceLineNo = sceneLines.firstOrNull()?.lineNo ?: 0,
                jaText = scene.lines.joinToString("\n") { line -> "${line.speaker}：${line.ja}" },
                zhText = scene.lines.joinToString("\n") { line -> "${line.speaker}：${line.zh}" },
                sceneLines = sceneLines,
                targetLineNo = sceneLines.firstOrNull { it.isTarget }?.lineNo ?: 0,
                domain = readAirDomain(scene.id),
                phenomenonKey = readAirPhenomenonKey(scene.id),
                questionType = readAirQuestionType(scene.id),
                prompt = "这段对话最需要读出的空气是什么？",
                options = options,
                answer = LinguisticExerciseAnswer(
                    answerZh = scene.subtext,
                    correctIndex = correctIndex,
                    rationaleZh = scene.evidence.joinToString("；"),
                ),
                hint = scene.context,
                basicExplanationZh = scene.learningPoint,
                deepExplanationZh = scene.evidence.joinToString("；"),
                animeContextNoteZh = scene.context,
                difficulty = readAirDifficulty(scene.id),
                qualityScore = 0.70,
                status = "local_fallback",
                phenomenonNameZh = readAirPhenomenonName(scene.id),
                phenomenonDefinitionZh = scene.learningPoint,
            )
        }
    }

    fun allReadAirExercises(): List<LinguisticExercise> {
        return works().flatMap { work ->
            episodes(work.slug).flatMap { episode ->
                readAirExercises(EpisodeSelection(work.slug, episode.episode))
            }
        }
    }

    private fun episodeFocus(selection: EpisodeSelection, mode: LessonMode): EpisodeFocus {
        val work = works().firstOrNull { it.slug == selection.workSlug } ?: works().first()
        val label = "${work.displayName} EP${selection.episode.toString().padStart(2, '0')}"
        val isKon = selection.workSlug == "k-on"
        return EpisodeFocus(
            workSlug = work.slug,
            episodeNumber = selection.episode,
            workTitle = work.displayName,
            episodeLabel = label,
            lessonTitle = "${mode.titleLabel} · ${if (isKon) "校园口语" else "异世界台词"}",
            sectionTitle = if (isKon) "第 1 组 · 假名、读音、日常句尾" else "第 1 组 · 场面压力、反问、立场表达",
            guidebook = if (isKon) {
                "先看一个词或语法点，马上做对应小题；跟读句按原声优先策略进入听音拼句，不再把资料页当训练页。"
            } else {
                "先抓台词声音和语气，再进入拼句、填空和自评跟读。可靠原声自动播放，其他内容使用系统语音辅助。"
            },
            dailyGoal = 8,
            xp = if (isKon) 820 else 1260,
            streakDays = 12,
            energy = 5,
        )
    }

    private fun vocab(selection: EpisodeSelection): List<VocabItem> {
        return if (selection.workSlug == "k-on") {
            listOf(
                VocabItem("k-on-vocab-daijoubu", "大丈夫", "だいじょうぶ", "daijoubu", "没事吧 / 没关系", "名词/表达", "N5", "EP${selection.episode} 日常确认", listOf("日常", "安心", "高频"), "现实中可用"),
                VocabItem("k-on-vocab-keionbu", "軽音部", "けいおんぶ", "keionbu", "轻音部", "名词", "N5", "作品核心词", listOf("校园", "社团"), "现实中可用"),
                VocabItem("k-on-vocab-ganbaru", "頑張る", "がんばる", "ganbaru", "努力、加油", "动词", "N5", "鼓励场景", listOf("鼓励", "意志"), "现实中可用"),
                VocabItem("k-on-vocab-tte", "って", "って", "tte", "引用、话题提示", "助词/表达", "N4", "口语高频", listOf("口语", "引用"), "现实中可用"),
                VocabItem("k-on-vocab-sorosoro", "そろそろ", "そろそろ", "sorosoro", "差不多该……", "副词", "N4", "提醒对方行动", listOf("提醒", "缓和"), "现实中可用"),
            )
        } else {
            val episodeLabel = lessonEpisodeLabel(selection)
            rotateForEpisode(reZeroVocabFallbackPool(), selection.episode)
                .take(5)
                .map { item ->
                    item.copy(
                        id = "re-zero-${episodeLabel.lowercase()}-vocab-${item.id}",
                        occurrence = "$episodeLabel ${item.occurrence}",
                    )
                }
        }
    }

    private fun grammar(selection: EpisodeSelection): List<GrammarPoint> {
        return if (selection.workSlug == "k-on") {
            listOf(
                GrammarPoint("k-on-grammar-naito", "～ないと", "必须 / 否则", "お姉ちゃん、そろそろ起きないと。", "姐姐，差不多该起床了。", "ないと 后面常省略“不行”，语气像轻轻提醒。", "口语里保留柔和压力。", "现实中可用", "N4", 1),
                GrammarPoint("k-on-grammar-tte", "～って", "引用与话题提示", "軽音部って何？", "轻音部是什么？", "って 把前面的词拿出来当话题，口语感强。", "比正式定义询问更自然。", "现实中可用", "N4", 74),
                GrammarPoint("k-on-grammar-yone", "句末 よ/ね/かな/だろ", "确认与共感", "これ、かわいいよね。", "这个很可爱吧。", "よね 同时传递自己的判断和期待对方认同。", "句末语气决定听感。", "现实中可用", "N5", 100),
            )
        } else {
            val episodeLabel = lessonEpisodeLabel(selection)
            rotateForEpisode(reZeroGrammarFallbackPool(), selection.episode)
                .take(3)
                .mapIndexed { index, item ->
                    item.copy(
                        id = "re-zero-${episodeLabel.lowercase()}-grammar-${item.id}",
                        sourceLineNo = selection.episode * 10 + index + 1,
                    )
                }
        }
    }

    private fun shadowingSentences(selection: EpisodeSelection): List<ShadowingSentence> {
        val ep = "EP${selection.episode.toString().padStart(2, '0')}"
        return if (selection.workSlug == "k-on") {
            listOf(
                ShadowingSentence("k-on-ep01-sent-00056", "このプリントをみんなに配っておいてね。", "このプリントを みんなに くばっておいてね。", "去把这些资料发给大家。", "$ep 第 56 行", AudioKind.Tts, 56),
                ShadowingSentence("k-on-ep01-sent-00074", "「軽い音楽」と書いて「軽音」よ。", "けいおんぶって なに？", "写成“轻松的音乐”的“轻音”。", "$ep 第 74 行", AudioKind.Tts, 74),
                ShadowingSentence("k-on-ep01-sent-00171", "きっと簡単なことしかやらないよ。", "きっと かんたんなことしか やらないよ。", "肯定都是些简单的事情啦。", "$ep 第 171 行", AudioKind.Tts, 171),
            )
        } else {
            rotateForEpisode(reZeroShadowingFallbackPool(), selection.episode)
                .take(3)
                .mapIndexed { index, item ->
                    val lineNo = selection.episode * 10 + index + 1
                    item.copy(
                        id = reZeroFallbackSentenceId(selection.episode, index + 1),
                        sourceLabel = "$ep 第 $lineNo 行",
                        sourceLineNo = lineNo,
                    )
                }
        }
    }

    private fun reZeroVocabFallbackPool(): List<VocabItem> {
        return listOf(
            VocabItem("seireikishi", "精霊騎士", "せいれいきし", "seirei kishi", "精灵骑士", "名词", "N1+", "关键称谓", listOf("称号", "幻想设定", "正式"), "偏作品设定"),
            VocabItem("akkan", "悪漢", "あっかん", "akkan", "坏人、恶棍", "名词", "N2", "冲突场景", listOf("书面", "强烈评价"), "现实中可用但偏书面"),
            VocabItem("nogareru", "逃れる", "のがれる", "nogareru", "逃脱、摆脱", "动词", "N2", "危机场景", listOf("危机", "叙述"), "现实中可用"),
            VocabItem("idomu", "挑む", "いどむ", "idomu", "挑战、迎战", "动词", "N2", "战斗宣言", listOf("意志", "热血"), "现实中可用"),
            VocabItem("yuzuru", "譲る", "ゆずる", "yuzuru", "让出、退让", "动词", "N3", "立场表达", listOf("关系", "态度"), "现实中可用"),
            VocabItem("mitsumeru", "見つめる", "みつめる", "mitsumeru", "凝视、盯着看", "动词", "N3", "关系确认", listOf("视线", "情绪证据"), "现实中可用"),
            VocabItem("kakugo", "覚悟", "かくご", "kakugo", "觉悟、心理准备", "名词", "N2", "决意场景", listOf("决心", "压力"), "现实中可用"),
            VocabItem("tamerau", "ためらう", "ためらう", "tamerau", "犹豫", "动词", "N2", "行动前停顿", listOf("心理", "选择"), "现实中可用"),
        )
    }

    private fun reZeroGrammarFallbackPool(): List<GrammarPoint> {
        return listOf(
            GrammarPoint("njanai", "んじゃない", "阻止和纠正", "逃がすんじゃない！", "不是说要放走他！", "んじゃない 把说话人的判断和纠正压上去。", "比普通 ない 更像当场阻止。", "现实中可用", "N3", 1),
            GrammarPoint("tatte", "たって", "即使、就算", "大人が寄ってたかっても捕まえられないの？", "就算一群大人围上去，也抓不住吗？", "たって 在口语里常把前提轻轻推开。", "后句才是说话人的真正评价。", "现实中可用", "N3", 2),
            GrammarPoint("kurenai", "てくれない", "期待对方动作没有发生", "誰も助けてくれない。", "没有人来帮我。", "くれる 以说话人为受益中心。", "否定后自然带出失望和孤立感。", "现实中可用", "N4", 3),
            GrammarPoint("hazu", "はず", "按理说、应该", "ここで諦めるはずがない。", "按理说不可能在这里放弃。", "はず 把说话人的判断建立在已知前提上。", "常用于确认信念或反驳现状。", "现实中可用", "N3", 4),
            GrammarPoint("you-to-suru", "ようとする", "正要、试图", "何かを言おうとして、言葉を飲み込んだ。", "正想说什么，又把话咽了回去。", "ようとする 描写动作即将发生的瞬间。", "适合抓住心理变化前的停顿。", "现实中可用", "N3", 5),
            GrammarPoint("wakejanai", "わけじゃない", "并不是说", "怖くないわけじゃない。", "并不是说不害怕。", "わけじゃない 用来修正过强的理解。", "保留复杂立场，不把话说死。", "现实中可用", "N3", 6),
        )
    }

    private fun reZeroShadowingFallbackPool(): List<ShadowingSentence> {
        return listOf(
            ShadowingSentence("", "やばい…これは本気でやばい。", "やばい…これは ほんきで やばい。", "糟了……这是真的不妙。", "EP-- 第 3 行", AudioKind.Source, 3),
            ShadowingSentence("", "スバル？どうかしたの？", "スバル？どうかしたの？", "昴？怎么了？", "EP-- 第 4 行", AudioKind.Source, 4),
            ShadowingSentence("", "お前を救ってみせる。", "おまえを すくってみせる。", "我一定会救你的。", "EP-- 第 16 行", AudioKind.Source, 16),
            ShadowingSentence("", "ここで立ち止まるわけにはいかない。", "ここで たちどまる わけには いかない。", "不能在这里停下。", "EP-- 第 21 行", AudioKind.Source, 21),
            ShadowingSentence("", "信じたいなら、最後まで見ていて。", "しんじたいなら、さいごまで みていて。", "如果你想相信，就看到最后。", "EP-- 第 35 行", AudioKind.Source, 35),
            ShadowingSentence("", "言葉だけじゃ足りないなら、行動で示す。", "ことばだけじゃ たりないなら、こうどうで しめす。", "如果只靠语言不够，那就用行动证明。", "EP-- 第 48 行", AudioKind.Source, 48),
        )
    }

    private fun <T> rotateForEpisode(items: List<T>, episode: Int): List<T> {
        if (items.isEmpty()) return items
        val offset = (episode.coerceAtLeast(1) - 1) % items.size
        return items.drop(offset) + items.take(offset)
    }

    private fun reZeroFallbackSentenceId(episode: Int, sentenceNumber: Int): String {
        val safeEpisode = episode.coerceAtLeast(1)
        val paddedSentence = sentenceNumber.toString().padStart(3, '0')
        return when {
            safeEpisode <= 25 -> "re-zero-s01e${safeEpisode.toString().padStart(2, '0')}-sentence-$paddedSentence"
            safeEpisode <= 50 -> "re-zero-s02e${(safeEpisode - 25).toString().padStart(2, '0')}-sentence-$paddedSentence"
            else -> "rezero_s03e${safeEpisode}_v9_sent_$paddedSentence"
        }
    }

    private fun readAirScenes(selection: EpisodeSelection): List<ReadAirScene> {
        val pool = if (selection.workSlug == "k-on") {
            kOnReadAirScenePool()
        } else {
            reZeroReadAirScenePool()
        }
        return rotateForEpisode(pool, selection.episode).take(2)
    }

    private fun kOnReadAirScenePool(): List<ReadAirScene> {
        return listOf(
            ReadAirScene(
                id = "scene-soft-reminder",
                title = "提醒不是命令",
                context = "角色提醒对方起床，但没有把语气推得太硬。",
                lines = listOf(
                    DialogueLine("妹妹", "お姉ちゃん、そろそろ起きないと。", "姐姐，差不多该起床了。"),
                    DialogueLine("姐姐", "あと五分……。", "再五分钟……"),
                ),
                subtext = "ないと 后半省略，让提醒保留关系上的柔和。",
                evidence = listOf("そろそろ 先缓冲时间压力", "ないと 暗示必须行动", "没有直接命令形"),
                learningPoint = "日常日语常把压力藏在省略句里，不一定靠命令形表达。",
            ),
            ReadAirScene(
                id = "scene-topic-tte",
                title = "把陌生词拿出来问",
                context = "说话人不知道社团名，用 って 把词当作话题。",
                lines = listOf(
                    DialogueLine("唯", "軽音部って何？", "轻音部是什么？"),
                    DialogueLine("律", "軽い音楽って書いて軽音だよ。", "写作轻的音乐，就是轻音。"),
                ),
                subtext = "って 让问题听起来更口语，不像正式定义询问。",
                evidence = listOf("名词 + って 是口语话题化", "何？ 省略完整谓语", "对话对象是同学"),
                learningPoint = "学日语课程里，假名和读音之后要马上连接真实口语功能。",
            ),
            ReadAirScene(
                id = "scene-club-soft-no",
                title = "柔和拒绝",
                context = "朋友邀请一起留下练习，说话人不直接拒绝，而是先给出顾虑。",
                lines = listOf(
                    DialogueLine("律", "今日も少し残っていく？", "今天也稍微留下练一下？"),
                    DialogueLine("澪", "うーん、明日なら大丈夫かも。", "嗯……明天的话可能可以。"),
                ),
                subtext = "明日なら 把拒绝包成条件，让关系不被直接切断。",
                evidence = listOf("うーん 先缓冲", "なら 把可行范围限定到明天", "かも 降低断言强度"),
                learningPoint = "日常拒绝常用条件和可能性表达，重点是保留对方的面子。",
            ),
            ReadAirScene(
                id = "scene-shared-joke",
                title = "玩笑里的亲密度",
                context = "吐槽听起来像批评，但双方都知道是在维持轻松气氛。",
                lines = listOf(
                    DialogueLine("紬", "またお茶にしちゃう？", "又要变成喝茶时间吗？"),
                    DialogueLine("律", "そこは練習って言ってよ。", "这里你要说是练习啦。"),
                ),
                subtext = "って言ってよ 不是命令，而是在用玩笑修正共同叙事。",
                evidence = listOf("また 暗示熟悉的重复模式", "言ってよ 带撒娇式纠正", "没有真正责备"),
                learningPoint = "亲密关系里的吐槽常常服务于气氛管理，而不是事实纠错。",
            ),
        )
    }

    private fun reZeroReadAirScenePool(): List<ReadAirScene> {
        return listOf(
            ReadAirScene(
                id = "scene-pressure",
                title = "被围住时的嘴硬",
                context = "角色表面上嘲讽对手，实际是在给同伴争取节奏。",
                lines = listOf(
                    DialogueLine("少年", "いい大人が寄ってたかって、こんなガキ一匹捕まえられないの？", "一群大人围上去，连这么个孩子都抓不住吗？"),
                    DialogueLine("对手", "言わせておけば……。", "让你继续说下去的话……"),
                ),
                subtext = "挑衅不是为了赢辩论，而是主动吸引注意力。",
                evidence = listOf("寄ってたかって 把对手描述成以多欺少", "の？ 结尾让责备听起来像反问"),
                learningPoint = "遇到反问句时，不只看字面问题，要看它是否在改变场面权力关系。",
            ),
            ReadAirScene(
                id = "scene-trust",
                title = "表面让步，实际确认信任",
                context = "一方说可以退让，另一方没有直接接受，而是确认对方真正意图。",
                lines = listOf(
                    DialogueLine("A", "ここは僕が譲る。", "这里我可以退让。"),
                    DialogueLine("B", "それ、本当に譲ってる顔？", "你那真的是退让的表情吗？"),
                ),
                subtext = "B 不是拒绝让步，而是在拆穿 A 的自我压抑。",
                evidence = listOf("譲る 的表面意义是退让", "顔？ 把判断落到情绪证据上"),
                learningPoint = "日语对话里，短问句经常承担关系确认，而不是单纯索取信息。",
            ),
            ReadAirScene(
                id = "scene-silence",
                title = "沉默不是同意",
                context = "对方没有立刻回答，真正传递的是犹豫和风险评估。",
                lines = listOf(
                    DialogueLine("スバル", "俺を信じてくれ。", "相信我。"),
                    DialogueLine("相手", "……信じたい、けど。", "……我想相信，可是。"),
                ),
                subtext = "けど 后面留白，表示情感上靠近、判断上仍没通过。",
                evidence = listOf("长停顿先暴露犹豫", "信じたい 表明愿望", "けど 留下未说出的风险"),
                learningPoint = "读空气时，转折词后面的空白经常比说出口的内容更重要。",
            ),
            ReadAirScene(
                id = "scene-distance",
                title = "礼貌里的距离",
                context = "角色用礼貌表达维持秩序，但其实在划出边界。",
                lines = listOf(
                    DialogueLine("エミリア", "お気持ちはうれしいです。", "你的心意我很高兴。"),
                    DialogueLine("エミリア", "でも、ここから先は私が決めます。", "但是，从这里开始由我决定。"),
                ),
                subtext = "ですます 不是单纯客气，而是在温和地把决定权收回来。",
                evidence = listOf("お気持ちはうれしい 先承接好意", "でも 转入边界", "私が決めます 明确主语和权限"),
                learningPoint = "敬体有时不是拉近距离，而是让拒绝更稳、更不伤人。",
            ),
            ReadAirScene(
                id = "scene-self-blame",
                title = "自责里的求助信号",
                context = "角色把责任揽到自己身上，表面是自责，实际是在请求被拦住。",
                lines = listOf(
                    DialogueLine("スバル", "全部、俺のせいだ。", "全都是我的错。"),
                    DialogueLine("仲間", "それで終わらせるつもり？", "你打算就这样结束吗？"),
                ),
                subtext = "同伴不是追责，而是在阻止他用自责逃离行动。",
                evidence = listOf("全部 极端化责任", "それで 指向前一句", "つもり？ 质问真实意图"),
                learningPoint = "自责句不一定是在说明事实，也可能是在发出求助和逃避信号。",
            ),
            ReadAirScene(
                id = "scene-resolution",
                title = "宣言里的关系承诺",
                context = "角色说出决心，不只是表达计划，也是在让对方放心。",
                lines = listOf(
                    DialogueLine("スバル", "今度こそ、間違えない。", "这次绝对不会再错。"),
                    DialogueLine("相手", "その言葉、覚えておくから。", "这句话，我会记住的。"),
                ),
                subtext = "覚えておくから 把承诺变成双方共同承担的约定。",
                evidence = listOf("今度こそ 表示从失败中重来", "その言葉 指向承诺本身", "から 让回应带有托付感"),
                learningPoint = "宣言句的重点常在关系后果：说出口后，对方就开始共同见证。",
            ),
        )
    }

    private fun readAirDomain(sceneId: String): String {
        return when (sceneId) {
            "scene-topic-tte", "scene-distance" -> "sociolinguistics"
            "scene-soft-reminder", "scene-club-soft-no", "scene-shared-joke" -> "pragmatics"
            "scene-pressure", "scene-trust", "scene-silence", "scene-self-blame", "scene-resolution" -> "pragmatics"
            else -> "pragmatics"
        }
    }

    private fun readAirPhenomenonKey(sceneId: String): String {
        return when (sceneId) {
            "scene-soft-reminder" -> "soft_obligation_ellipsis"
            "scene-topic-tte" -> "topic_marker_tte"
            "scene-club-soft-no" -> "soft_refusal"
            "scene-shared-joke" -> "shared_joke"
            "scene-pressure" -> "rhetorical_pressure"
            "scene-trust" -> "relationship_check"
            "scene-silence" -> "ellipsis"
            "scene-distance" -> "politeness_distance"
            "scene-self-blame" -> "self_blame_signal"
            "scene-resolution" -> "promise_witness"
            else -> "local_read_air"
        }
    }

    private fun readAirPhenomenonName(sceneId: String): String {
        return when (sceneId) {
            "scene-soft-reminder" -> "柔和义务省略"
            "scene-topic-tte" -> "って 话题化"
            "scene-club-soft-no" -> "柔和拒绝"
            "scene-shared-joke" -> "玩笑式纠正"
            "scene-pressure" -> "反问中的场面压力"
            "scene-trust" -> "关系确认"
            "scene-silence" -> "省略与沉默"
            "scene-distance" -> "礼貌距离"
            "scene-self-blame" -> "自责求助信号"
            "scene-resolution" -> "承诺见证"
            else -> "读空气"
        }
    }

    private fun readAirQuestionType(sceneId: String): String {
        return when (sceneId) {
            "scene-topic-tte" -> "syntax_relation"
            "scene-distance", "scene-trust" -> "relationship_reading"
            "scene-silence", "scene-self-blame" -> "implicit_intent"
            "scene-club-soft-no", "scene-shared-joke" -> "contrast_choice"
            else -> "kuuki_yomi"
        }
    }

    private fun readAirDifficulty(sceneId: String): String {
        return when (sceneId) {
            "scene-soft-reminder", "scene-topic-tte", "scene-pressure" -> "starter"
            "scene-club-soft-no", "scene-trust", "scene-silence" -> "easy"
            "scene-shared-joke", "scene-distance", "scene-self-blame" -> "medium"
            "scene-resolution" -> "hard"
            else -> "starter"
        }
    }

    private fun buildVocabStudyNodes(selection: EpisodeSelection, vocab: List<VocabItem>): List<StudyCardNode> {
        val episodeLabel = lessonEpisodeLabel(selection)
        return vocab.filter { it.surface.isNotBlank() && it.meaningZh.isNotBlank() }.map { item ->
            StudyCardNode(
                id = "${item.id}-study",
                title = "先学这个词",
                prompt = item.surface,
                explanation = "${item.surface} = ${item.meaningZh}",
                sourceLabel = episodeLabel,
                japanese = item.surface,
                reading = item.reading,
                meaningZh = item.meaningZh,
                notes = listOf(
                    "${item.partOfSpeech} · ${item.level}",
                    item.occurrence,
                    item.realWorldNote,
                ).filter { it.isNotBlank() },
                sourceKind = "vocab",
                sourceId = item.id,
                audio = PromptAudio.Tts(item.surface, autoPlay = false, label = "播放语音"),
                linguistic = item.linguistic,
            )
        }
    }

    private fun lessonEpisodeLabel(selection: EpisodeSelection): String {
        val workName = works().firstOrNull { it.slug == selection.workSlug }?.displayName
            ?: selection.workSlug.ifBlank { "当前作品" }
        return "$workName EP${selection.episode.toString().padStart(2, '0')}"
    }

    private fun buildGrammarStudyNodes(
        selection: EpisodeSelection,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence> = emptyList(),
    ): List<StudyCardNode> {
        return grammar.filter { it.pattern.isNotBlank() && it.titleZh.isNotBlank() }.map { point ->
            val compactExplanation = compactGrammarStudyExplanation(
                pattern = point.pattern,
                meaningZh = point.titleZh,
                explanation = point.explanationZh,
            )
            StudyCardNode(
                id = "${point.id}-study",
                title = "先学这个语法",
                prompt = point.pattern,
                explanation = compactExplanation.ifBlank { point.pragmaticsNote.ifBlank { point.titleZh } },
                sourceLabel = point.sourceLineNo.takeIf { it > 0 }
                    ?.let { lineNo -> "原台词 · 第 $lineNo 行" }
                    ?: "本集语法",
                japanese = point.exampleJa.ifBlank { point.pattern },
                reading = point.pattern,
                meaningZh = point.titleZh,
                notes = listOf(compactExplanation, point.pragmaticsNote, point.realWorldNote, point.difficulty)
                    .filter { it.isNotBlank() },
                sourceKind = "grammar",
                sourceId = point.id,
                audio = grammarPromptAudio(selection, point, sentences, autoPlay = false),
                linguistic = point.linguistic,
            )
        }
    }

    private fun buildSentenceStudyNodes(selection: EpisodeSelection, sentences: List<ShadowingSentence>): List<StudyCardNode> {
        return sentences.filter { it.ja.isNotBlank() && isUsableChineseMeaning(it.meaningZh) }.map { sentence ->
            StudyCardNode(
                id = "${sentence.id}-study",
                title = "先听懂这句",
                prompt = sentence.ja,
                explanation = "${sentence.ja} / ${sentence.meaningZh}",
                sourceLabel = sentence.sourceLabel,
                japanese = sentence.ja,
                reading = sentence.reading,
                meaningZh = sentence.meaningZh,
                notes = listOf("跟读句", sentence.sourceLabel),
                sourceKind = "sentence",
                sourceId = sentence.id,
                audio = promptAudioForSentence(selection.workSlug, sentence, autoPlay = false),
                linguistic = sentence.linguistic,
            )
        }
    }

    private fun buildDatabaseSingleChoiceNodes(
        exercises: List<LearningExercise>,
        vocab: List<VocabItem>,
        limit: Int = Int.MAX_VALUE,
    ): List<SingleChoiceNode> {
        val usable = exercises.filter { exercise ->
            exercise.exerciseType.isNotBlank() && exercise.prompt.isNotBlank() && exercise.answer.isNotBlank()
        }
        val vocabById = vocab.associateBy(VocabItem::id)
        val answersByType = usable.groupBy { it.exerciseType }
            .mapValues { (_, values) -> values.map { it.answer }.distinct() }
        return usable.mapIndexedNotNull { index, exercise ->
            val linkedVocab = vocabById[exercise.vocabItemId]
            val isKanaToKanji = exercise.exerciseType == "kana_to_kanji"
            if (isKanaToKanji && linkedVocab == null) return@mapIndexedNotNull null
            val prompt = if (isKanaToKanji) {
                "「${linkedVocab?.surface.orEmpty()}」是什么意思？"
            } else {
                exercise.prompt
            }
            val answer = if (isKanaToKanji) linkedVocab?.meaningZh.orEmpty() else exercise.answer
            val distractorValues = if (isKanaToKanji) {
                vocab.map(VocabItem::meaningZh).filter(String::isNotBlank)
            } else {
                answersByType[exercise.exerciseType].orEmpty()
            }
            val choices = buildDistractors(values = distractorValues, answer = answer, offset = index)
            if (choices.size < 2) return@mapIndexedNotNull null
            SingleChoiceNode(
                id = exercise.id,
                title = if (isKanaToKanji) "选择正确词义" else databaseExerciseTitle(exercise.exerciseType),
                prompt = prompt,
                explanation = if (isKanaToKanji) {
                    listOfNotNull(
                        linkedVocab?.reading?.takeIf(String::isNotBlank)?.let { "读音：$it" },
                        linkedVocab?.meaningZh?.takeIf(String::isNotBlank),
                    ).joinToString(" · ")
                } else {
                    exercise.hint.trim()
                },
                sourceLabel = listOf(
                    "数据库题库",
                    if (isKanaToKanji) "词义" else databaseExerciseLabel(exercise.exerciseType),
                    exercise.difficulty.uppercase().takeIf { it.isNotBlank() },
                ).filterNotNull().joinToString(" · "),
                body = null,
                choices = choices,
                answer = answer,
                sourceKind = databaseExerciseSourceKind(exercise.exerciseType),
                sourceId = exercise.vocabItemId.ifBlank { exercise.id },
                audio = if (isKanaToKanji) {
                    PromptAudio.Tts(linkedVocab?.surface.orEmpty(), autoPlay = true, label = "重播单词")
                } else {
                    databaseExerciseAudio(exercise)
                },
            )
        }.take(limit.coerceAtLeast(1))
    }

    private fun buildDatabasePairNodes(
        selection: EpisodeSelection,
        focus: EpisodeFocus,
        exercises: List<LearningExercise>,
        limit: Int = Int.MAX_VALUE,
    ): List<PairMatchNode> {
        val candidates = exercises.mapNotNull(::databasePairCandidate)
            .distinctBy { candidate -> compactText(candidate.left) to compactText(candidate.right) }
        return candidates.groupBy { it.exerciseType }.values.flatMap { sameType ->
            sameType.chunked(5).mapNotNull { group ->
                if (group.size < 4) return@mapNotNull null
                val first = group.first()
                PairMatchNode(
                    id = "${selection.workSlug}-${selection.episode}-db-pair-${first.exerciseId}",
                    title = "选择配对",
                    prompt = "",
                    explanation = "每一组词义和日语表达都直接来自本集已发布题库。",
                    sourceLabel = "${focus.episodeLabel} · 数据库题库",
                    pairs = group.map { candidate ->
                        MatchPair(
                            id = candidate.exerciseId,
                            left = candidate.left,
                            right = candidate.right,
                            audioText = candidate.right,
                        )
                    },
                    sourceKind = "exercise",
                    sourceId = group.joinToString(",") { it.exerciseId },
                    audio = PromptAudio.None,
                )
            }
        }.take(limit.coerceAtLeast(1))
    }

    private fun databasePairCandidate(exercise: LearningExercise): DatabasePairCandidate? {
        val candidate = when (exercise.exerciseType) {
            "vocab_meaning" -> DatabasePairCandidate(
                exerciseId = exercise.id,
                exerciseType = exercise.exerciseType,
                left = exercise.answer,
                right = japaneseQuotedText(exercise.prompt),
            )

            "meaning_to_vocab",
            "meaning_to_japanese" -> DatabasePairCandidate(
                exerciseId = exercise.id,
                exerciseType = exercise.exerciseType,
                left = exercise.prompt
                    .removePrefix("中文意思：")
                    .removePrefix("中文：")
                    .trim(),
                right = exercise.answer,
            )

            else -> null
        } ?: return null
        return candidate.takeIf {
            it.left.isNotBlank() && it.right.isNotBlank() && compactText(it.left) != compactText(it.right)
        }
    }

    private fun databaseExerciseTitle(type: String): String = when (type) {
        "vocab_meaning" -> "选择正确词义"
        "vocab_reading" -> "选择正确读音"
        "meaning_to_vocab",
        "meaning_to_japanese" -> "选择对应的日语表达"
        "kana_to_kanji" -> "选择对应的汉字表达"
        "grammar_meaning",
        "grammar_short_answer" -> "判断语法作用"
        "sentence_understanding",
        "sentence_meaning" -> "理解这句台词"
        else -> "选择正确答案"
    }

    private fun databaseExerciseLabel(type: String): String = when (type) {
        "vocab_meaning" -> "词义"
        "vocab_reading" -> "读音"
        "meaning_to_vocab",
        "meaning_to_japanese" -> "中译日"
        "kana_to_kanji" -> "假名转汉字"
        "grammar_meaning",
        "grammar_short_answer" -> "语法理解"
        "sentence_understanding",
        "sentence_meaning" -> "台词理解"
        else -> type
    }

    private fun databaseExerciseSourceKind(type: String): String = when (type) {
        in VocabExerciseTypes -> "vocab"
        in GrammarExerciseTypes -> "grammar"
        in SentenceExerciseTypes -> "sentence"
        else -> "exercise"
    }

    private fun databaseExerciseAudio(exercise: LearningExercise): PromptAudio {
        val promptJapanese = japaneseQuotedText(exercise.prompt)
        return when {
            promptJapanese.isNotBlank() -> PromptAudio.Tts(promptJapanese, autoPlay = true, label = "重播日语")
            exercise.exerciseType in setOf("meaning_to_vocab", "meaning_to_japanese") ->
                PromptAudio.Tts(exercise.answer, autoPlay = false, label = "重播单词")
            else -> PromptAudio.None
        }
    }

    private fun japaneseQuotedText(text: String): String {
        return Regex("「([^」]+)」").find(text)?.groupValues?.getOrNull(1).orEmpty().trim()
    }

    private fun buildVocabPairNodes(selection: EpisodeSelection, focus: EpisodeFocus, vocab: List<VocabItem>): List<PairMatchNode> {
        val usable = vocab.filter { it.surface.isNotBlank() && it.meaningZh.isNotBlank() }
        if (usable.size < 4) return emptyList()
        val groups = usable.chunkedForPairMatch()
        return groups.mapIndexed { index, group ->
            PairMatchNode(
                id = "${selection.workSlug}-${selection.episode}-vocab-pair-${index + 1}-${group.first().id}",
                title = "选择配对",
                prompt = "",
                explanation = "配对时点击日文会播放辅助读音。失败会撤销本次选择，答对的组合会保留。",
                sourceLabel = "${focus.episodeLabel} 高频词",
                pairs = group.map { MatchPair(it.id, it.meaningZh, it.surface, it.surface) },
                sourceKind = "vocab",
                sourceId = group.joinToString(",") { it.id },
                audio = PromptAudio.None,
            )
        }
    }

    private fun buildVocabChoiceNodes(
        selection: EpisodeSelection,
        vocab: List<VocabItem>,
        limit: Int = Int.MAX_VALUE,
    ): List<SingleChoiceNode> {
        val usable = vocab.filter { it.surface.isNotBlank() && it.meaningZh.isNotBlank() }
        return usable.take(limit.coerceAtLeast(1)).mapIndexed { index, item ->
            SingleChoiceNode(
                id = "${item.id}-ja-to-meaning",
                title = "选择正确词义",
                prompt = "「${item.surface}」是什么意思？",
                explanation = listOf(
                    "${item.surface}（${item.reading}）= ${item.meaningZh}",
                    item.realWorldNote.ifBlank { item.occurrence },
                ).filter(String::isNotBlank).joinToString("。"),
                sourceLabel = "词汇",
                body = null,
                choices = buildDistractors(usable.map(VocabItem::meaningZh), item.meaningZh, index),
                answer = item.meaningZh,
                sourceKind = "vocab",
                sourceId = item.id,
                audio = PromptAudio.Tts(item.surface, autoPlay = true, label = "重播单词"),
            )
        }
    }

    private fun buildSentenceAudioTileNodes(
        selection: EpisodeSelection,
        sentences: List<ShadowingSentence>,
        limit: Int = 6,
    ): List<TileOrderNode> {
        return sentences.filter { sentence ->
            val tiles = splitJapaneseTiles(sentence.ja)
            sentence.ja.isNotBlank() && tiles.size >= 2 && !hasBadTileFragments(tiles)
        }.take(limit.coerceAtLeast(1)).map { sentence ->
            val targetTiles = splitJapaneseTiles(sentence.ja)
            val audio = promptAudioForSentence(selection.workSlug, sentence, autoPlay = true)
            TileOrderNode(
                id = "${sentence.id}-audio-tiles",
                title = "选择听到的内容",
                prompt = "",
                explanation = if (isUsableChineseMeaning(sentence.meaningZh)) "意思：${sentence.meaningZh}" else sentence.ja,
                sourceLabel = sentence.sourceLabel,
                displayText = "先听音频，再拼日文",
                targetTiles = targetTiles,
                bankTiles = stableShuffle(
                    (targetTiles + sentenceDistractorTiles(sentences, sentence.id)).distinct(),
                    sentence.id,
                ).take(maxOf(6, targetTiles.size)),
                audioTile = true,
                sourceKind = "sentence",
                sourceId = sentence.id,
                audio = audio,
            )
        }
    }

    private fun buildSentenceTranslationNodes(
        selection: EpisodeSelection,
        sentences: List<ShadowingSentence>,
        limit: Int = 6,
    ): List<TileOrderNode> {
        return sentences.filter { sentence ->
            val tiles = splitChineseTiles(sentence.meaningZh)
            sentence.ja.isNotBlank() && isUsableChineseMeaning(sentence.meaningZh) && tiles.size >= 2 && !hasBadTileFragments(tiles)
        }.take(limit.coerceAtLeast(1)).map { sentence ->
            val targetTiles = splitChineseTiles(sentence.meaningZh)
            TileOrderNode(
                id = "${sentence.id}-translation-tiles",
                title = "用中文拼出这句话",
                prompt = "",
                explanation = "原句：${sentence.ja}",
                sourceLabel = sentence.sourceLabel,
                displayText = sentence.ja,
                targetTiles = targetTiles,
                bankTiles = stableShuffle(targetTiles, sentence.id),
                audioTile = false,
                sourceKind = "sentence",
                sourceId = sentence.id,
                audio = promptAudioForSentence(selection.workSlug, sentence, autoPlay = true),
            )
        }
    }

    private fun buildSentenceShadowingNodes(
        selection: EpisodeSelection,
        sentences: List<ShadowingSentence>,
        limit: Int = 6,
    ): List<ShadowingNode> {
        return sentences.filter { sentence ->
            sentence.ja.isNotBlank() && isUsableChineseMeaning(sentence.meaningZh)
        }.take(limit.coerceAtLeast(1)).map { sentence ->
            val pronunciationId = pronunciationSentenceId(sentence)
            ShadowingNode(
                id = "${sentence.id}-shadowing-self-check",
                title = "真实跟读测评",
                prompt = "听原句并跟读，查看发音测评",
                explanation = "原句：${sentence.ja} / ${sentence.meaningZh}",
                sourceLabel = sentence.sourceLabel,
                sentence = sentence,
                ratings = listOf("像原声", "大致跟上", "还要再练"),
                pronunciationSentenceId = pronunciationId,
                audio = promptAudioForSentence(selection.workSlug, sentence, autoPlay = true),
            )
        }
    }

    private fun buildGrammarClozeNodes(
        selection: EpisodeSelection,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence>,
        limit: Int = 8,
    ): List<ClozeNode> {
        return grammar.filter { it.pattern.isNotBlank() && it.exampleJa.isNotBlank() }
            .mapIndexedNotNull { index, point ->
                val cloze = buildGrammarCloze(point, grammar, index) ?: return@mapIndexedNotNull null
                ClozeNode(
                    id = "${point.id}-cloze",
                    title = "选词填空",
                    prompt = "",
                    explanation = listOf(point.explanationZh, point.pragmaticsNote).filter { it.isNotBlank() }.joinToString(" "),
                    sourceLabel = point.sourceLineNo.takeIf { it > 0 }
                        ?.let { lineNo -> "原台词 · 第 $lineNo 行" }
                        ?: "本集语法",
                    before = cloze.before,
                    after = cloze.after,
                    choices = cloze.values.map { value ->
                        ClozeChoice(
                            value = value,
                            note = "",
                        )
                    },
                    answer = cloze.answer,
                    sourceKind = "grammar",
                    sourceId = point.id,
                    audio = grammarPromptAudio(selection, point, sentences, autoPlay = true),
                )
            }
            .take(limit.coerceAtLeast(1))
    }

    private fun buildGrammarChoiceNodes(
        selection: EpisodeSelection,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence>,
        limit: Int = 6,
    ): List<SingleChoiceNode> {
        return grammar.filter { it.pattern.isNotBlank() && it.titleZh.isNotBlank() }
            .take(limit.coerceAtLeast(1))
            .mapIndexed { index, point ->
                SingleChoiceNode(
                    id = "${point.id}-function-choice",
                    title = "判断语法功能",
                    prompt = "这句里的「${point.pattern}」主要表达什么？",
                    explanation = listOf(point.explanationZh, point.pragmaticsNote).filter { it.isNotBlank() }.joinToString(" "),
                    sourceLabel = point.sourceLineNo.takeIf { it > 0 }
                        ?.let { lineNo -> "原台词 · 第 $lineNo 行" }
                        ?: "本集语法",
                    body = point.exampleJa,
                    choices = buildDistractors(grammar.map { it.titleZh }, point.titleZh, index),
                    answer = point.titleZh,
                    sourceKind = "grammar",
                    sourceId = point.id,
                    audio = grammarPromptAudio(selection, point, sentences, autoPlay = true),
                )
            }
    }

    private fun grammarPromptAudio(
        selection: EpisodeSelection,
        point: GrammarPoint,
        sentences: List<ShadowingSentence>,
        autoPlay: Boolean,
    ): PromptAudio {
        val sourceLineNo = point.sourceLineNo.takeIf { it > 0 }
        val sourceSentence = sourceLineNo?.let { lineNo ->
            sentences.firstOrNull { sentence -> sentence.sourceLineNo == lineNo }
        }
        return if (sourceSentence != null) {
            promptAudioForSentence(selection.workSlug, sourceSentence, autoPlay = autoPlay)
        } else {
            PromptAudio.Tts(point.exampleJa.ifBlank { point.pattern }, autoPlay = autoPlay, label = "播放语音")
        }
    }

    private fun buildMixedStudyPracticeSequence(pools: LessonPools, batch: Int): List<LessonNode> {
        val curriculumNodes =
            buildStudyPracticeSequence(pools.vocabStudy, pools.vocabChoice, batch, batchSize = 2) +
                buildStudyPracticeSequence(pools.grammarStudy, pools.grammarCloze + pools.grammarChoice, batch, batchSize = 1) +
                buildStudyPracticeSequence(
                    pools.sentenceStudy,
                    prioritizeAudioNodes(pools.sentenceAudio + pools.sentenceTranslation),
                    batch,
                    batchSize = 1,
                )
        val databaseNodes = pools.databasePair.take(1) + pools.databaseChoice.take(3)
        val fallbackPair = if (pools.databasePair.isEmpty()) pools.vocabPair.take(1) else emptyList()
        return (
            curriculumNodes.take(4) +
                databaseNodes +
                curriculumNodes.drop(4) +
                fallbackPair
            ).distinctBy { it.id }.take(AndroidLessonLimit)
    }

    private fun buildTargetStudyPracticeSequence(pools: LessonPools, target: LessonTarget): List<LessonNode> {
        return when (target) {
            is LessonTarget.Vocab -> buildStudyPracticeSequence(
                pools.vocabStudy,
                pools.vocabChoice + pools.vocabPair,
                batch = 1,
                batchSize = 1,
            )

            is LessonTarget.Grammar -> buildStudyPracticeSequence(
                pools.grammarStudy,
                pools.grammarCloze + pools.grammarChoice,
                batch = 1,
                batchSize = 1,
            )

            is LessonTarget.Sentence -> buildStudyPracticeSequence(
                pools.sentenceStudy,
                prioritizeAudioNodes(pools.sentenceAudio + pools.sentenceTranslation) + pools.sentenceShadowing,
                batch = 1,
                batchSize = 1,
                practiceLimit = 3,
            )
        }
    }

    private fun buildStudyPracticeSequence(
        studyNodes: List<StudyCardNode>,
        practiceNodes: List<LessonNode>,
        batch: Int,
        batchSize: Int,
        practiceLimit: Int = 2,
    ): List<LessonNode> {
        val safeBatchSize = batchSize.coerceAtLeast(1)
        val start = (batch.coerceAtLeast(1) - 1) * safeBatchSize
        val scopedStudy = studyNodes.drop(start).take(safeBatchSize)
        val output = mutableListOf<LessonNode>()
        for (study in scopedStudy) {
            output += study
            output += practiceNodes.filter { it.sourceId == study.sourceId }.take(practiceLimit.coerceAtLeast(1))
        }
        return output
    }

    private fun balanceLessonNodes(nodes: List<LessonNode>, quota: Map<String, Int>): List<LessonNode> {
        val output = mutableListOf<LessonNode>()
        listOf("学习卡", "配对", "听音", "填空", "拼句", "选择", "跟读").forEach { type ->
            output += nodes.filter { it.typeLabel == type }.take(quota[type] ?: 0)
        }
        return output.take(AndroidLessonLimit)
    }

    private fun targetQuota(target: LessonTarget): Map<String, Int> {
        return when (target) {
            is LessonTarget.Vocab -> mapOf("配对" to 1, "选择" to 3)
            is LessonTarget.Grammar -> mapOf("填空" to 3, "选择" to 3, "拼句" to 2)
            is LessonTarget.Sentence -> mapOf("听音" to 3, "拼句" to 2, "跟读" to 1)
        }
    }

    private fun scopeVocab(vocab: List<VocabItem>, target: LessonTarget?): List<VocabItem> {
        if (target !is LessonTarget.Vocab) return vocab
        return prioritizeTarget(vocab, target.id) { it.id }
    }

    private fun scopedVocabForLesson(
        vocab: List<VocabItem>,
        mode: LessonMode,
        target: LessonTarget?,
        batch: Int,
    ): List<VocabItem> {
        return when {
            target is LessonTarget.Vocab -> scopeVocab(vocab, target).take(VocabSpecialtyBatchSize)
            target != null -> emptyList()
            mode == LessonMode.Vocab -> vocab.lessonWindow(batch, VocabSpecialtyBatchSize)
            mode == LessonMode.Mixed -> vocab.lessonRemainder(batch, 2)
            else -> emptyList()
        }
    }

    private fun scopeGrammar(grammar: List<GrammarPoint>, target: LessonTarget?): List<GrammarPoint> {
        if (target !is LessonTarget.Grammar) return grammar
        return prioritizeTarget(grammar, target.id) { it.id }
    }

    private fun scopedGrammarForLesson(
        grammar: List<GrammarPoint>,
        mode: LessonMode,
        target: LessonTarget?,
        batch: Int,
    ): List<GrammarPoint> {
        return when {
            target is LessonTarget.Grammar -> scopeGrammar(grammar, target).take(AndroidSpecialtyBatchSize)
            target != null -> emptyList()
            mode == LessonMode.Grammar -> grammar.lessonWindow(batch, AndroidSpecialtyBatchSize)
            mode == LessonMode.Mixed -> grammar.lessonRemainder(batch, 1)
            else -> emptyList()
        }
    }

    private fun scopeSentences(sentences: List<ShadowingSentence>, target: LessonTarget?): List<ShadowingSentence> {
        if (target !is LessonTarget.Sentence) return sentences
        return prioritizeTarget(sentences, target.id) { it.id }
    }

    private fun scopedSentencesForLesson(
        sentences: List<ShadowingSentence>,
        mode: LessonMode,
        target: LessonTarget?,
        batch: Int,
    ): List<ShadowingSentence> {
        return when {
            target is LessonTarget.Sentence -> scopeSentences(sentences, target).take(AndroidSpecialtyBatchSize)
            target != null -> emptyList()
            mode == LessonMode.Shadowing -> sentences.lessonWindow(batch, AndroidSpecialtyBatchSize)
            mode == LessonMode.Mixed -> sentences.lessonRemainder(batch, 1)
            else -> emptyList()
        }
    }

    private fun scopedExercisesForLesson(
        exercises: List<LearningExercise>,
        mode: LessonMode,
        target: LessonTarget?,
        batch: Int,
    ): List<LearningExercise> {
        if (target != null || exercises.isEmpty()) return emptyList()
        val matching = when (mode) {
            LessonMode.Vocab -> exercises.filter { it.exerciseType in VocabExerciseTypes }
            LessonMode.Grammar -> exercises.filter { it.exerciseType in GrammarExerciseTypes }
            LessonMode.Shadowing -> exercises.filter { it.exerciseType in SentenceExerciseTypes }
            LessonMode.Mixed,
            LessonMode.Review -> exercises
        }
        return matching.balancedExerciseWindow(batch, DatabaseExercisesPerTypePerBatch)
    }

    private fun List<LearningExercise>.balancedExerciseWindow(
        batch: Int,
        perType: Int,
    ): List<LearningExercise> {
        val windows = groupBy { it.exerciseType }.values.map { group ->
            group.lessonWindow(batch, perType)
        }
        val maxSize = windows.maxOfOrNull { it.size } ?: return emptyList()
        return buildList {
            for (index in 0 until maxSize) {
                windows.forEach { group -> group.getOrNull(index)?.let(::add) }
            }
        }
    }

    private fun <T> List<T>.lessonWindow(batch: Int, batchSize: Int): List<T> {
        val safeBatchSize = batchSize.coerceAtLeast(1)
        val start = (batch.coerceAtLeast(1) - 1) * safeBatchSize
        return drop(start).take(safeBatchSize)
    }

    private fun <T> List<T>.lessonRemainder(batch: Int, stepSize: Int): List<T> {
        val start = (batch.coerceAtLeast(1) - 1) * stepSize.coerceAtLeast(1)
        return drop(start)
    }

    private fun <T> List<T>.prioritizeForPractice(
        progress: PracticeProgressIndex,
        itemType: String,
        idOf: (T) -> String,
    ): List<T> {
        return withIndex()
            .sortedWith { left, right ->
                val compared = progress.compare(
                    itemType to idOf(left.value),
                    itemType to idOf(right.value),
                )
                if (compared != 0) compared else left.index.compareTo(right.index)
            }
            .map { it.value }
    }

    private fun List<LessonNode>.prioritizeForPractice(progress: PracticeProgressIndex): List<LessonNode> {
        return withIndex()
            .sortedWith { left, right ->
                val compared = progress.compare(left.value, right.value)
                if (compared != 0) compared else left.index.compareTo(right.index)
            }
            .map { it.value }
    }

    private fun List<VocabItem>.chunkedForPairMatch(): List<List<VocabItem>> {
        val groups = chunked(5).map(List<VocabItem>::toMutableList).toMutableList()
        if (groups.size > 1 && groups.last().size < 4) {
            val tail = groups.removeAt(groups.lastIndex)
            groups.last().addAll(tail)
        }
        return groups.filter { it.size >= 4 }
    }

    private fun LearningExercise.practiceItemType(): String = databaseExerciseSourceKind(exerciseType)

    private fun LearningExercise.practiceItemId(): String = vocabItemId.ifBlank { id }

    private fun <T> prioritizeTarget(items: List<T>, targetId: String, idOf: (T) -> String): List<T> {
        val index = items.indexOfFirst { idOf(it) == targetId }
        if (index < 0) return emptyList()
        return listOf(items[index]) + items.filterIndexed { itemIndex, _ -> itemIndex != index }
    }

    private fun prioritizeAudioNodes(nodes: List<LessonNode>): List<LessonNode> {
        return nodes.sortedBy { node ->
            when (val audio = node.audio) {
                is PromptAudio.Source -> if (audio.reliability == AudioReliability.Verified) 0 else 1
                is PromptAudio.Tts -> 2
                PromptAudio.None -> 3
            }
        }
    }

    private fun buildVocabDistractors(vocab: List<VocabItem>, item: VocabItem, offset: Int): List<String> {
        val candidates = vocab
            .filter { it.id != item.id && it.surface.isNotBlank() }
        val ranked = boundedCandidates(candidates, offset)
            .sortedWith(compareByDescending<VocabItem> { vocabDistractorScore(item, it) }.thenBy { it.surface })
            .map { it.surface }
        return stableShuffle((listOf(item.surface) + ranked.take(3)).distinct(), item.surface)
    }

    private fun vocabDistractorScore(item: VocabItem, candidate: VocabItem): Int {
        var score = 0
        if (item.partOfSpeech == candidate.partOfSpeech) score += 4
        if (item.level == candidate.level) score += 2
        score -= abs(item.surface.length - candidate.surface.length)
        return score
    }

    private fun buildDistractors(values: List<String>, answer: String, offset: Int): List<String> {
        val normalizedAnswer = compactText(answer)
        val unique = values.map { it.trim() }
            .filter { it.isNotBlank() && compactText(it) != normalizedAnswer }
            .distinct()
        val answerFeatures = distractorFeatures(answer)
        val ranked = boundedCandidates(unique, offset)
            .map(::distractorFeatures)
            .sortedWith(compareByDescending<DistractorFeatures> { distractorScore(answerFeatures, it) }.thenBy { it.value })
        return stableShuffle((listOf(answer) + ranked.take(3).map { it.value }).distinct(), answer)
    }

    private fun <T> boundedCandidates(values: List<T>, offset: Int): List<T> {
        if (values.size <= MaxDistractorCandidates) return values
        val start = Math.floorMod(offset * MaxDistractorCandidates, values.size)
        return List(MaxDistractorCandidates) { index -> values[(start + index) % values.size] }
    }

    private data class GrammarCloze(val before: String, val after: String, val answer: String, val values: List<String>)

    private fun buildGrammarCloze(point: GrammarPoint, grammar: List<GrammarPoint>, index: Int): GrammarCloze? {
        val answer = resolveGrammarAnswer(point.pattern, point.exampleJa) ?: return null
        val answerIndex = point.exampleJa.lastIndexOf(answer)
        if (answerIndex < 0) return null
        val values = buildDistractors(grammar.flatMap { grammarAnswerChoices(it.pattern, it.exampleJa) }, answer, index)
        if (values.size < 2) return null
        return GrammarCloze(
            before = point.exampleJa.take(answerIndex),
            after = point.exampleJa.drop(answerIndex + answer.length),
            answer = answer,
            values = values,
        )
    }

    private fun resolveGrammarAnswer(pattern: String, example: String): String? {
        val candidates = grammarAnswerChoices(pattern, example)
        return candidates.firstOrNull { example.contains(it) }
    }

    private fun grammarAnswerChoices(pattern: String, example: String): List<String> {
        val normalized = normalizePattern(pattern)
        val slashParts = normalized.split("/", "／").map { it.trim() }.filter { it.isNotBlank() }
        val choices = if (slashParts.size > 1) slashParts else listOf(normalized).filter { it.isNotBlank() }
        if (pattern.contains("句末") || choices.any { it in listOf("よ", "ね", "かな", "だろ", "よね") }) {
            val endings = listOf("よね", "かな", "だろ", "よ", "ね")
            return endings.filter { example.contains(it) } + endings
        }
        return choices.map { it.removePrefix("～").trim() }.filter { it.isNotBlank() && !it.contains("句末") }
    }

    private fun normalizePattern(pattern: String): String {
        return pattern.removePrefix("～").replace("「", "").replace("」", "").trim()
    }

    private fun splitJapaneseTiles(text: String): List<String> {
        val clean = text.replace(Regex("""[。！？!?]"""), "")
        val chunks = clean.split(Regex("""[\s、，]+""")).flatMap { chunk ->
            if (chunk.length > 8) splitJapaneseLongChunk(chunk) else listOf(chunk)
        }.map { it.trim() }.filter { it.isNotBlank() }
        return mergeShortTiles(if (chunks.size > 1) chunks else splitJapaneseLongChunk(clean))
    }

    private fun splitJapaneseLongChunk(text: String): List<String> {
        val clean = text.replace(Regex("""[。！？!?、，\s]"""), "")
        if (clean.isBlank()) return emptyList()
        val tiles = mutableListOf<String>()
        var start = 0
        while (start < clean.length) {
            val remaining = clean.drop(start)
            val boundary = findJapaneseBoundary(remaining)
            val size = if (boundary > 0) boundary else minOf(remaining.length, 7)
            tiles += remaining.take(size)
            start += size
        }
        return tiles
    }

    private fun findJapaneseBoundary(text: String): Int {
        val particles = listOf("から", "まで", "ので", "けど", "ても", "なら", "って", "を", "が", "は", "に", "で", "と", "も", "へ", "の")
        for (particle in particles) {
            val index = text.indexOf(particle)
            val boundary = index + particle.length
            if (index >= 1 && boundary in 3..9 && text.length - boundary != 1) return boundary
        }
        val endings = listOf("おいてね", "っている", "っていく", "ないと", "ました", "ません", "ます", "です", "だろ", "かな", "よね", "よ", "ね")
        for (ending in endings) {
            val index = text.indexOf(ending)
            if (index >= 1 && index + ending.length <= 10) return index + ending.length
        }
        return if (text.length <= 8) text.length else 0
    }

    private fun splitChineseTiles(text: String): List<String> {
        val clean = text.replace(Regex("""[。！？!?]"""), "")
        val pieces = clean.split(Regex("""[\s，,、]+""")).filter { it.isNotBlank() }
        return if (pieces.size > 1) pieces else splitChineseLongChunk(clean)
    }

    private fun splitChineseLongChunk(text: String): List<String> {
        val clean = text.replace(Regex("""[。！？!?，,、\s]"""), "")
        if (clean.isBlank()) return emptyList()
        val markers = listOf(
            "这样下去",
            "简单的事情啦",
            "去把这些",
            "差不多",
            "该起床了",
            "都是些",
            "肯定",
            "姐姐",
            "然后",
            "但是",
            "所以",
            "因为",
            "如果",
            "已经",
            "就是",
            "可以",
            "应该",
            "不会",
            "就会",
            "变成",
            "大家",
            "资料",
            "发给",
        )
        val tiles = mutableListOf<String>()
        var remaining = clean
        while (remaining.isNotBlank()) {
            val marker = markers.firstOrNull { remaining.startsWith(it) }
            if (marker != null) {
                tiles += marker
                remaining = remaining.drop(marker.length)
                continue
            }
            val nextMarkerIndex = markers.map { remaining.indexOf(it, startIndex = 1) }.filter { it > 0 }.minOrNull()
            val size = if (nextMarkerIndex != null && nextMarkerIndex <= 5) nextMarkerIndex else minOf(remaining.length, 4)
            tiles += remaining.take(size)
            remaining = remaining.drop(size)
        }
        return mergeShortTiles(tiles)
    }

    private fun mergeShortTiles(tiles: List<String>): List<String> {
        val output = mutableListOf<String>()
        for (tile in tiles.filter { it.isNotBlank() }) {
            val previous = output.lastOrNull()
            if (tile.length == 1 && previous != null) {
                output[output.lastIndex] = previous + tile
            } else if (previous?.length == 1) {
                output[output.lastIndex] = previous + tile
            } else {
                output += tile
            }
        }
        return output
    }

    private fun sentenceDistractorTiles(sentences: List<ShadowingSentence>, sourceId: String): List<String> {
        return sentences.filter { it.id != sourceId }
            .flatMap { splitJapaneseTiles(it.ja) }
            .filter { it.length <= 6 && isCleanTileFragment(it) }
            .take(4)
    }

    private fun hasBadTileFragments(tiles: List<String>): Boolean {
        return tiles.any { !isCleanTileFragment(it) }
    }

    private fun isCleanTileFragment(tile: String): Boolean {
        if (tile.length == 1 || tile.isBlank()) return false
        if (tile in listOf("くのね", "がってい", "ってい")) return false
        return !Regex("""["'“”‘’「」『』]""").containsMatchIn(tile)
    }

    private fun isUsableChineseMeaning(value: String): Boolean {
        val text = value.trim()
        if (text.isBlank()) return false
        if (Regex("""[\uE000-\uF8FF\uFFFD]""").containsMatchIn(text)) return false
        if (Regex("""[\u3040-\u30ff]""").containsMatchIn(text)) return false
        return Regex("""[\u4E00-\u9FFF]""").containsMatchIn(text)
    }

    private fun <T> stableShuffle(items: List<T>, seed: String): List<T> {
        val output = items.distinct().toMutableList()
        var state = seed.fold(0) { sum, char -> sum + char.code }.takeIf { it != 0 } ?: 1
        for (index in output.size - 1 downTo 1) {
            state = (state * 1664525 + 1013904223)
            val swapIndex = (state ushr 1) % (index + 1)
            val value = output[index]
            output[index] = output[swapIndex]
            output[swapIndex] = value
        }
        return output
    }

    private fun compactText(value: String): String = value.filterNot { it.isWhitespace() }

    private fun distractorFeatures(value: String): DistractorFeatures {
        val compact = compactText(value)
        return DistractorFeatures(
            value = value,
            compactLength = compact.length,
            hasKana = compact.any { char -> char in '\u3040'..'\u30ff' || char == 'ー' },
            hasKanji = compact.any { char -> char in '\u3400'..'\u9fff' },
        )
    }

    private fun distractorScore(answer: DistractorFeatures, candidate: DistractorFeatures): Int {
        var score = 0
        score -= abs(answer.compactLength - candidate.compactLength)
        if (answer.hasKana == candidate.hasKana) score += 2
        if (answer.hasKanji == candidate.hasKanji) score += 1
        return score
    }
}

private data class PracticeProgressRecord(
    val state: ReviewState,
    val lastReviewedAt: String,
)

/**
 * Stable practice ordering: unseen material first, then weak material, then mastered material
 * from least-recently reviewed to most-recently reviewed.
 */
private class PracticeProgressIndex(items: List<ProgressItem>) {
    private val exactNodes = linkedMapOf<String, PracticeProgressRecord>()
    private val materials = linkedMapOf<Pair<String, String>, PracticeProgressRecord>()

    init {
        items.forEach { item ->
            val record = PracticeProgressRecord(item.state, item.lastReviewedAt)
            listOf(item.itemId, item.payload["nodeId"].orEmpty())
                .filter(String::isNotBlank)
                .forEach { nodeId -> putLatest(exactNodes, nodeId, record) }
            val sourceIds = listOf(
                item.payload["sourceId"],
                item.payload["source_id"],
                item.payload["source"],
            ).firstOrNull { !it.isNullOrBlank() }
                ?.split(',')
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                .orEmpty()
            sourceIds.forEach { sourceId ->
                putLatest(materials, item.itemType to sourceId, record)
            }
            if (sourceIds.isEmpty() && item.itemId.isNotBlank()) {
                putLatest(materials, item.itemType to item.itemId, record)
            }
        }
    }

    fun compare(left: Pair<String, String>, right: Pair<String, String>): Int {
        return compareRecords(materials[left], materials[right])
    }

    fun compare(left: LessonNode, right: LessonNode): Int {
        return compareRecords(recordFor(left), recordFor(right))
    }

    private fun recordFor(node: LessonNode): PracticeProgressRecord? {
        exactNodes[node.id]?.let { return it }
        val sourceIds = node.sourceId.split(',').map(String::trim).filter(String::isNotBlank)
        if (sourceIds.isEmpty()) return null
        val records = sourceIds.map { sourceId -> materials[node.sourceKind to sourceId] }
        if (records.any { it == null }) return null
        val present = records.filterNotNull()
        return present.firstOrNull { it.state.isWeakPracticeState() }
            ?: present.minByOrNull(PracticeProgressRecord::lastReviewedAt)
    }

    private fun compareRecords(left: PracticeProgressRecord?, right: PracticeProgressRecord?): Int {
        val bucket = practiceBucket(left).compareTo(practiceBucket(right))
        if (bucket != 0) return bucket
        return left?.lastReviewedAt.orEmpty().compareTo(right?.lastReviewedAt.orEmpty())
    }

    private fun practiceBucket(record: PracticeProgressRecord?): Int {
        return when {
            record == null -> 0
            record.state.isWeakPracticeState() -> 1
            else -> 2
        }
    }

    private fun ReviewState.isWeakPracticeState(): Boolean {
        return this == ReviewState.Bad ||
            this == ReviewState.Fuzzy ||
            this == ReviewState.Unknown ||
            this == ReviewState.Ok
    }

    private fun <K> putLatest(
        target: MutableMap<K, PracticeProgressRecord>,
        key: K,
        candidate: PracticeProgressRecord,
    ) {
        val existing = target[key]
        if (existing == null || candidate.lastReviewedAt > existing.lastReviewedAt) {
            target[key] = candidate
        }
    }
}

private data class LessonPools(
    val databasePair: List<PairMatchNode>,
    val databaseChoice: List<SingleChoiceNode>,
    val vocabPair: List<PairMatchNode>,
    val vocabChoice: List<SingleChoiceNode>,
    val sentenceAudio: List<TileOrderNode>,
    val sentenceTranslation: List<TileOrderNode>,
    val sentenceShadowing: List<ShadowingNode>,
    val grammarCloze: List<ClozeNode>,
    val grammarChoice: List<SingleChoiceNode>,
    val vocabStudy: List<StudyCardNode>,
    val grammarStudy: List<StudyCardNode>,
    val sentenceStudy: List<StudyCardNode>,
)

private data class DatabasePairCandidate(
    val exerciseId: String,
    val exerciseType: String,
    val left: String,
    val right: String,
)

private data class DistractorFeatures(
    val value: String,
    val compactLength: Int,
    val hasKana: Boolean,
    val hasKanji: Boolean,
)
