package com.animejapaneselab.nativeapp.data

import kotlin.math.abs

private const val WebLessonLimit = 10
private const val WebSpecialtyBatchSize = 6

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
            scenes = scenes,
            lessonNodes = buildLessonNodes(selection, focus, vocab, grammar, shadowing, mode, batch = batch),
        )
    }

    fun contentFromRemote(
        selection: EpisodeSelection,
        vocab: List<VocabItem>,
        grammar: List<GrammarPoint>,
        shadowing: List<ShadowingSentence>,
        mode: LessonMode = LessonMode.Mixed,
        batch: Int = 1,
    ): EpisodeContent {
        val fallback = content(selection, mode, batch)
        val nextVocab = vocab.ifEmpty { fallback.vocab }
        val nextGrammar = grammar.ifEmpty { fallback.grammar }
        val nextShadowing = shadowing.ifEmpty { fallback.shadowing }
        val episodeLabel = lessonEpisodeLabel(selection)
        val focus = episodeFocus(selection, mode).copy(
            lessonTitle = "线上${mode.titleLabel} · $episodeLabel",
            guidebook = "已从云端同步本集词汇、语法和跟读句：原声优先，标准语音只作为辅助。",
        )
        return EpisodeContent(
            focus = focus,
            vocab = nextVocab,
            grammar = nextGrammar,
            shadowing = nextShadowing,
            scenes = fallback.scenes,
            lessonNodes = buildLessonNodes(selection, focus, nextVocab, nextGrammar, nextShadowing, mode, batch = batch),
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
        if (target != null || mode == LessonMode.Mixed || mode == LessonMode.Review || mode == LessonMode.Vocab) return false
        val itemCount = when (mode) {
            LessonMode.Grammar -> buildGrammarStudyNodes(EpisodeSelection("", 1), grammar).size
            LessonMode.Shadowing -> buildSentenceStudyNodes(EpisodeSelection("", 1), sentences).size
            LessonMode.Mixed,
            LessonMode.Vocab,
            LessonMode.Review -> 0
        }
        return batch.coerceAtLeast(1) * WebSpecialtyBatchSize < itemCount
    }

    fun buildLessonNodes(
        selection: EpisodeSelection,
        focus: EpisodeFocus,
        vocab: List<VocabItem>,
        grammar: List<GrammarPoint>,
        sentences: List<ShadowingSentence>,
        mode: LessonMode,
        target: LessonTarget? = null,
        batch: Int = 1,
    ): List<LessonNode> {
        val lessonVocab = scopeVocab(vocab, target)
        val lessonGrammar = scopeGrammar(grammar, target)
        val lessonSentences = scopeSentences(sentences, target)
        val pools = LessonPools(
            vocabPair = buildVocabPairNodes(selection, focus, lessonVocab),
            vocabChoice = buildVocabChoiceNodes(selection, lessonVocab),
            sentenceAudio = buildSentenceAudioTileNodes(selection, lessonSentences),
            sentenceTranslation = buildSentenceTranslationNodes(selection, lessonSentences),
            sentenceShadowing = buildSentenceShadowingNodes(selection, lessonSentences),
            grammarCloze = buildGrammarClozeNodes(selection, lessonGrammar),
            grammarChoice = buildGrammarChoiceNodes(selection, lessonGrammar),
            vocabStudy = buildVocabStudyNodes(selection, lessonVocab),
            grammarStudy = buildGrammarStudyNodes(selection, lessonGrammar),
            sentenceStudy = buildSentenceStudyNodes(selection, lessonSentences),
        )
        val practiceNodes = listOf(
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
                practiceNodes.take(WebLessonLimit)
            }
        }
        return when (mode) {
            LessonMode.Vocab -> buildStudyPracticeSequence(
                studyNodes = pools.vocabStudy,
                practiceNodes = pools.vocabChoice,
                batch = 1,
                batchSize = pools.vocabStudy.size,
            )

            LessonMode.Grammar -> buildStudyPracticeSequence(
                studyNodes = pools.grammarStudy,
                practiceNodes = pools.grammarCloze + pools.grammarChoice,
                batch = batch,
                batchSize = WebSpecialtyBatchSize,
            )

            LessonMode.Shadowing -> buildStudyPracticeSequence(
                studyNodes = pools.sentenceStudy,
                practiceNodes = prioritizeAudioNodes(pools.sentenceAudio + pools.sentenceTranslation) + pools.sentenceShadowing,
                batch = batch,
                batchSize = WebSpecialtyBatchSize,
                practiceLimit = 3,
            )

            LessonMode.Review -> balanceLessonNodes(
                prioritizeAudioNodes(practiceNodes),
                quota = mapOf("配对" to 1, "选择" to 2, "听音" to 2, "拼句" to 2, "填空" to 3),
            )

            LessonMode.Mixed -> buildMixedStudyPracticeSequence(pools, batch)
        }.ifEmpty {
            practiceNodes.take(WebLessonLimit)
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
                qualityScore = 70,
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
                "先抓台词声音和语气，再进入拼句、填空和自评跟读。可靠原声自动播放，不可靠原声只手动播放并提供标准语音。"
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
                audio = PromptAudio.Tts(item.surface, autoPlay = false, label = "播放读音"),
            )
        }
    }

    private fun lessonEpisodeLabel(selection: EpisodeSelection): String {
        val workName = works().firstOrNull { it.slug == selection.workSlug }?.displayName
            ?: selection.workSlug.ifBlank { "当前作品" }
        return "$workName EP${selection.episode.toString().padStart(2, '0')}"
    }

    private fun buildGrammarStudyNodes(selection: EpisodeSelection, grammar: List<GrammarPoint>): List<StudyCardNode> {
        return grammar.filter { it.pattern.isNotBlank() && it.titleZh.isNotBlank() }.map { point ->
            StudyCardNode(
                id = "${point.id}-study",
                title = "先学这个语法",
                prompt = point.pattern,
                explanation = "${point.pattern}：${point.titleZh}",
                sourceLabel = "第 ${point.sourceLineNo.takeIf { it > 0 } ?: selection.episode} 行",
                japanese = point.exampleJa.ifBlank { point.pattern },
                reading = point.pattern,
                meaningZh = point.titleZh,
                notes = listOf(point.explanationZh, point.pragmaticsNote, point.realWorldNote, point.difficulty).filter { it.isNotBlank() },
                sourceKind = "grammar",
                sourceId = point.id,
                audio = PromptAudio.Tts(point.exampleJa.ifBlank { point.pattern }, autoPlay = false, label = "播放例句"),
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
            )
        }
    }

    private fun buildVocabPairNodes(selection: EpisodeSelection, focus: EpisodeFocus, vocab: List<VocabItem>): List<PairMatchNode> {
        val usable = vocab.filter { it.surface.isNotBlank() && it.meaningZh.isNotBlank() }.take(8)
        if (usable.size < 4) return emptyList()
        return listOf(
            PairMatchNode(
                id = "${selection.workSlug}-${selection.episode}-vocab-pair-core",
                title = "选择配对",
                prompt = "把中文意思和日文表达配起来",
                explanation = "配对时点击日文会播放辅助读音。失败会撤销本次选择，答对的组合会保留。",
                sourceLabel = "${focus.episodeLabel} 高频词",
                pairs = usable.take(5).map { MatchPair(it.id, it.meaningZh, it.surface, it.surface) },
                sourceKind = "vocab",
                sourceId = usable.joinToString(",") { it.id },
                audio = PromptAudio.None,
            ),
        )
    }

    private fun buildVocabChoiceNodes(selection: EpisodeSelection, vocab: List<VocabItem>): List<SingleChoiceNode> {
        return vocab.filter { it.surface.isNotBlank() && it.meaningZh.isNotBlank() }.mapIndexed { index, item ->
            SingleChoiceNode(
                id = "${item.id}-meaning-to-ja",
                title = "选择正确的日文",
                prompt = "「${item.meaningZh}」对应哪个日文？",
                explanation = "${item.surface} = ${item.meaningZh}。${item.realWorldNote.ifBlank { item.occurrence }}",
                sourceLabel = "词汇",
                body = item.reading.takeIf { it.isNotBlank() }?.let { "读音：$it" },
                choices = buildVocabDistractors(vocab, item, index),
                answer = item.surface,
                sourceKind = "vocab",
                sourceId = item.id,
                audio = PromptAudio.Tts(item.surface, autoPlay = false, label = "听答案"),
            )
        }
    }

    private fun buildSentenceAudioTileNodes(selection: EpisodeSelection, sentences: List<ShadowingSentence>): List<TileOrderNode> {
        return sentences.filter { sentence ->
            val tiles = splitJapaneseTiles(sentence.ja)
            sentence.ja.isNotBlank() && tiles.size >= 2 && !hasBadTileFragments(tiles)
        }.take(6).map { sentence ->
            val targetTiles = splitJapaneseTiles(sentence.ja)
            val audio = promptAudioForSentence(selection.workSlug, sentence, autoPlay = true)
            TileOrderNode(
                id = "${sentence.id}-audio-tiles",
                title = "选择听到的内容",
                prompt = "听句子，把下面的语块按顺序拼起来",
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

    private fun buildSentenceTranslationNodes(selection: EpisodeSelection, sentences: List<ShadowingSentence>): List<TileOrderNode> {
        return sentences.filter { sentence ->
            val tiles = splitChineseTiles(sentence.meaningZh)
            sentence.ja.isNotBlank() && isUsableChineseMeaning(sentence.meaningZh) && tiles.size >= 2 && !hasBadTileFragments(tiles)
        }.take(6).map { sentence ->
            val targetTiles = splitChineseTiles(sentence.meaningZh)
            TileOrderNode(
                id = "${sentence.id}-translation-tiles",
                title = "用中文拼出这句话",
                prompt = "理解日文句子，再拼出自然中文意思",
                explanation = "原句：${sentence.ja}",
                sourceLabel = sentence.sourceLabel,
                displayText = sentence.ja,
                targetTiles = targetTiles,
                bankTiles = stableShuffle(
                    (targetTiles + translationDistractorTiles(sentences, sentence.id)).distinct(),
                    sentence.id,
                ).take(maxOf(6, targetTiles.size)),
                audioTile = false,
                sourceKind = "sentence",
                sourceId = sentence.id,
                audio = promptAudioForSentence(selection.workSlug, sentence, autoPlay = false),
            )
        }
    }

    private fun buildSentenceShadowingNodes(selection: EpisodeSelection, sentences: List<ShadowingSentence>): List<ShadowingNode> {
        return sentences.filter { sentence ->
            sentence.ja.isNotBlank() && isUsableChineseMeaning(sentence.meaningZh)
        }.take(6).map { sentence ->
            ShadowingNode(
                id = "${sentence.id}-shadowing-self-check",
                title = "跟读自评",
                prompt = "听原句，开口跟读后选一个最接近的状态",
                explanation = "原句：${sentence.ja} / ${sentence.meaningZh}",
                sourceLabel = sentence.sourceLabel,
                sentence = sentence,
                ratings = listOf("像原声", "大致跟上", "还要再练"),
                audio = promptAudioForSentence(selection.workSlug, sentence, autoPlay = true),
            )
        }
    }

    private fun buildGrammarClozeNodes(selection: EpisodeSelection, grammar: List<GrammarPoint>): List<ClozeNode> {
        return grammar.filter { it.pattern.isNotBlank() && it.exampleJa.isNotBlank() }.take(8).mapIndexedNotNull { index, point ->
            val cloze = buildGrammarCloze(point, grammar, index) ?: return@mapIndexedNotNull null
            ClozeNode(
                id = "${point.id}-cloze",
                title = "选词填空",
                prompt = "选择最自然的表达：${point.titleZh}",
                explanation = listOf(point.explanationZh, point.pragmaticsNote).filter { it.isNotBlank() }.joinToString(" "),
                sourceLabel = "第 ${point.sourceLineNo.takeIf { it > 0 } ?: selection.episode} 行",
                before = cloze.before,
                after = cloze.after,
                choices = cloze.values.map { value ->
                    ClozeChoice(
                        value = value,
                        note = if (value == cloze.answer) point.titleZh else "干扰项：注意语气、结构或意义是否匹配。",
                    )
                },
                answer = cloze.answer,
                sourceKind = "grammar",
                sourceId = point.id,
                audio = PromptAudio.Tts(point.exampleJa, autoPlay = false, label = "播放例句"),
            )
        }
    }

    private fun buildGrammarChoiceNodes(selection: EpisodeSelection, grammar: List<GrammarPoint>): List<SingleChoiceNode> {
        return grammar.filter { it.pattern.isNotBlank() && it.titleZh.isNotBlank() }.take(6).mapIndexed { index, point ->
            SingleChoiceNode(
                id = "${point.id}-function-choice",
                title = "判断语法功能",
                prompt = "这句里的「${point.pattern}」主要表达什么？",
                explanation = listOf(point.explanationZh, point.pragmaticsNote).filter { it.isNotBlank() }.joinToString(" "),
                sourceLabel = "第 ${point.sourceLineNo.takeIf { it > 0 } ?: selection.episode} 行",
                body = point.exampleJa,
                choices = buildDistractors(grammar.map { it.titleZh }, point.titleZh, index),
                answer = point.titleZh,
                sourceKind = "grammar",
                sourceId = point.id,
                audio = PromptAudio.Tts(point.exampleJa, autoPlay = false, label = "播放例句"),
            )
        }
    }

    private fun buildMixedStudyPracticeSequence(pools: LessonPools, batch: Int): List<LessonNode> {
        return (
            buildStudyPracticeSequence(pools.vocabStudy, pools.vocabChoice, batch, batchSize = 2) +
                buildStudyPracticeSequence(pools.grammarStudy, pools.grammarCloze + pools.grammarChoice, batch, batchSize = 1) +
                buildStudyPracticeSequence(
                    pools.sentenceStudy,
                    prioritizeAudioNodes(pools.sentenceAudio + pools.sentenceTranslation),
                    batch,
                    batchSize = 1,
                ) +
                pools.vocabPair.take(1)
            ).take(WebLessonLimit)
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
        return output.take(WebLessonLimit)
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

    private fun scopeGrammar(grammar: List<GrammarPoint>, target: LessonTarget?): List<GrammarPoint> {
        if (target !is LessonTarget.Grammar) return grammar
        return prioritizeTarget(grammar, target.id) { it.id }
    }

    private fun scopeSentences(sentences: List<ShadowingSentence>, target: LessonTarget?): List<ShadowingSentence> {
        if (target !is LessonTarget.Sentence) return sentences
        return prioritizeTarget(sentences, target.id) { it.id }
    }

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
        val ranked = vocab
            .filter { it.id != item.id && it.surface.isNotBlank() }
            .sortedWith(compareByDescending<VocabItem> { vocabDistractorScore(item, it) }.thenBy { it.surface })
            .map { it.surface }
        return buildDistractors(ranked, item.surface, offset)
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
        val ranked = unique.sortedWith(compareByDescending<String> { distractorScore(answer, it) }.thenBy { it })
        val rotated = ranked.drop(offset % (ranked.size.coerceAtLeast(1))) + ranked.take(offset % (ranked.size.coerceAtLeast(1)))
        return stableShuffle((listOf(answer) + rotated.take(3)).distinct(), answer)
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

    private fun translationDistractorTiles(sentences: List<ShadowingSentence>, sourceId: String): List<String> {
        return sentences.filter { it.id != sourceId }
            .flatMap { splitChineseTiles(it.meaningZh) }
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

    private fun compactText(value: String): String = value.replace(Regex("""\s+"""), "").trim()

    private fun distractorScore(answer: String, candidate: String): Int {
        var score = 0
        score -= abs(compactText(answer).length - compactText(candidate).length)
        if (Regex("""[\u3040-\u30ffー]""").containsMatchIn(answer) == Regex("""[\u3040-\u30ffー]""").containsMatchIn(candidate)) score += 2
        if (Regex("""[\u4E00-\u9FFF]""").containsMatchIn(answer) == Regex("""[\u4E00-\u9FFF]""").containsMatchIn(candidate)) score += 1
        return score
    }
}

private data class LessonPools(
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
