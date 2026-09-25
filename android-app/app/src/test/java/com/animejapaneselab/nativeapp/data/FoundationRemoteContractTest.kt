package com.animejapaneselab.nativeapp.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FoundationRemoteContractTest {
    @Test
    fun topicPathIncludesCursorAndFilters() {
        val path = buildFoundationTopicsPath(
            FoundationTopicQuery(
                domain = FoundationDomain.Syntax,
                moduleId = "syntax-core",
                cursor = "topic_20-token",
                limit = 25,
            ),
        )

        assertEquals(
            "/api/linguistics/foundation/topics" +
                "?curriculumVersion=foundation-v1" +
                "&cursor=topic_20-token" +
                "&limit=25" +
                "&domain=syntax" +
                "&moduleId=syntax-core",
            path,
        )
    }

    @Test
    fun packAndQuestionPathsUseExactFoundationEndpoints() {
        assertEquals(
            "/api/linguistics/foundation/packs?curriculumVersion=foundation-v1&limit=100",
            buildFoundationPacksPath(),
        )
        assertEquals(
            "/api/linguistics/foundation/questions" +
                "?curriculumVersion=foundation-v1" +
                "&cursor=question_40-token" +
                "&limit=40" +
                "&packId=foundation-pack-1" +
                "&topicId=foundation-topic-1" +
                "&stage=F3" +
                "&questionType=contrast_choice" +
                "&difficulty=3",
            buildFoundationQuestionsPath(
                FoundationQuestionQuery(
                    packId = "foundation-pack-1",
                    topicId = "foundation-topic-1",
                    stage = FoundationStage.F3,
                    questionType = FoundationQuestionType.ContrastChoice,
                    difficulty = 3,
                    cursor = "question_40-token",
                    limit = 40,
                ),
            ),
        )
    }

    @Test
    fun pathsRejectInvalidLimitAndIdentifier() {
        assertThrows(IllegalArgumentException::class.java) {
            buildFoundationPacksPath(FoundationPackQuery(limit = 101))
        }
        assertThrows(IllegalArgumentException::class.java) {
            buildFoundationQuestionsPath(FoundationQuestionQuery(packId = "../other"))
        }
    }

    @Test
    fun parsesStrictTopicPackAndNestedQuestionPages() {
        val topic = parseFoundationTopicPageJson(pageJson(topicJson())).items.single()
        val pack = parseFoundationPackPageJson(pageJson(packJson())).items.single()
        val question = parseFoundationQuestionPageJson(pageJson(questionJson())).items.single()

        assertEquals(FoundationDomain.Syntax, topic.domain)
        assertEquals("识别基本关系", topic.learningObjectives.f1Zh)
        assertEquals(4, pack.questionCount)
        assertEquals(1, pack.domainQuotas[FoundationDomain.Syntax])
        assertEquals(FoundationStage.F2, question.stage)
        assertEquals(FoundationQuestionType.SyntaxRelation, question.questionType)
        assertTrue(question.stimulus is FoundationStimulus.Dialogue)
        assertEquals(2, (question.stimulus as FoundationStimulus.Dialogue).turns.size)
        assertEquals("店员与顾客正在确认包装方式。", question.stimulus.zhContext)
        assertEquals(setOf("A", "C", "D"), question.wrongExplanations.keys)
        assertTrue(question.isCorrect("B"))
        assertEquals(3, question.contentVersion)
        assertEquals("a".repeat(64), question.contentHash)
    }

    @Test
    fun parsesNormalizedContrastAnnotationsAndMetalinguisticForm() {
        val contrastQuestion = questionJson().put(
            "stimulus",
            JSONObject(
                """
                {
                  "kind":"contrast",
                  "items":[
                    {"label":"甲","text":"かこ","noteZh":"过去"},
                    {"label":"乙","text":"かっこ","noteZh":"括号"}
                  ],
                  "zhContext":"比较特殊拍造成的形式差异。"
                }
                """.trimIndent(),
            ),
        )
        val contrast = parseFoundationQuestionPageJson(pageJson(contrastQuestion))
            .items.single().stimulus as FoundationStimulus.Contrast
        assertEquals("かこ", contrast.items.first().text)
        assertEquals("过去", contrast.items.first().noteZh)
        assertEquals("比较特殊拍造成的形式差异。", contrast.zhContext)

        val metalinguisticQuestion = questionJson().put(
            "stimulus",
            JSONObject(
                """
                {
                  "kind":"metalinguistic",
                  "form":"ほん＋たな → ほんだな",
                  "descriptionZh":"分析复合词中的连浊。"
                }
                """.trimIndent(),
            ),
        )
        val metalinguistic = parseFoundationQuestionPageJson(pageJson(metalinguisticQuestion))
            .items.single().stimulus as FoundationStimulus.Metalinguistic
        assertEquals("ほん＋たな → ほんだな", metalinguistic.form)
        assertEquals("分析复合词中的连浊。", metalinguistic.descriptionZh)
    }

    @Test
    fun derivesStableContentFingerprintForTheCurrentlyDeployedWorkerShape() {
        val legacyShape = questionJson().apply {
            remove("contentVersion")
            remove("contentHash")
        }
        val original = parseFoundationQuestionPageJson(pageJson(legacyShape)).items.single()
        val repeated = parseFoundationQuestionPageJson(pageJson(legacyShape)).items.single()
        val revisedShape = JSONObject(legacyShape.toString()).put(
            "promptZh",
            "数据库修订后的题目",
        )
        val revised = parseFoundationQuestionPageJson(pageJson(revisedShape)).items.single()

        assertEquals(1, original.contentVersion)
        assertEquals(64, original.contentHash?.length)
        assertEquals(original.contentHash, repeated.contentHash)
        assertFalse(original.contentHash == revised.contentHash)
    }

    @Test
    fun rejectsMalformedNestedStimulus() {
        val malformed = questionJson()
        malformed.getJSONObject("stimulus").put("turns", listOf("not-an-object"))

        assertThrows(IllegalArgumentException::class.java) {
            parseFoundationQuestionPageJson(pageJson(malformed))
        }
    }

    @Test
    fun rejectsIncompleteWrongExplanationMap() {
        val malformed = questionJson()
        malformed.getJSONObject("wrongExplanations").remove("C")

        assertThrows(IllegalArgumentException::class.java) {
            parseFoundationQuestionPageJson(pageJson(malformed))
        }
    }

    @Test
    fun rejectsMalformedNestedLearningObjectives() {
        val malformed = topicJson()
        malformed.getJSONObject("learningObjectives").remove("F4Zh")

        assertThrows(IllegalArgumentException::class.java) {
            parseFoundationTopicPageJson(pageJson(malformed))
        }
    }

    @Test
    fun rejectsInconsistentPaginationMetadata() {
        val malformedPage = JSONObject(pageJson(questionJson()))
        malformedPage.getJSONObject("page").put("hasMore", true)

        assertThrows(IllegalArgumentException::class.java) {
            parseFoundationQuestionPageJson(malformedPage.toString())
        }
    }

    @Test
    fun progressPayloadContainsFoundationIdentityWithoutEpisodeFields() {
        val question = parseFoundationQuestionPageJson(pageJson(questionJson())).items.single()
        val payload = buildFoundationProgressPayload(question, "A")

        assertEquals("foundation", payload.getString("track"))
        assertEquals("foundation-pack-1", payload.getString("packId"))
        assertEquals("foundation-topic-1", payload.getString("topicId"))
        assertEquals("F2", payload.getString("stage"))
        assertEquals("syntax_relation", payload.getString("questionType"))
        assertEquals(3, payload.getInt("contentVersion"))
        assertEquals("a".repeat(64), payload.getString("contentHash"))
        assertFalse(payload.has("workSlug"))
        assertFalse(payload.has("episode"))
    }

    private fun pageJson(item: JSONObject): String {
        return JSONObject()
            .put("items", listOf(item))
            .put(
                "page",
                JSONObject()
                    .put("limit", 40)
                    .put("hasMore", false)
                    .put("nextCursor", JSONObject.NULL),
            )
            .toString()
    }

    private fun topicJson(): JSONObject {
        return JSONObject(
            """
            {
              "id":"foundation-topic-1",
              "curriculumVersion":"foundation-v1",
              "domain":"syntax",
              "moduleId":"syntax-core",
              "sortOrder":10,
              "titleJa":"統語関係",
              "titleZh":"句法关系",
              "shortDefinitionZh":"观察成分之间的结构关系。",
              "beginnerExplanationZh":"先找谓语，再确认各成分与谓语的关系。",
              "deepExplanationZh":"同一线性顺序不一定对应同一层级结构。",
              "cautionNoteZh":"不要只凭中文词序判断。",
              "prerequisiteTopicIds":[],
              "learningObjectives":{
                "F1Zh":"识别基本关系",
                "F2Zh":"分析成分功能",
                "F3Zh":"比较结构差异",
                "F4Zh":"迁移到新句子"
              },
              "exampleSpec":{
                "formZh":"短句",
                "contrastZh":"成分关系对比",
                "constraintsZh":"避免依赖动画设定"
              },
              "tags":["syntax","foundation"],
              "status":"published",
              "qualityScore":96
            }
            """.trimIndent(),
        )
    }

    private fun packJson(): JSONObject {
        return JSONObject(
            """
            {
              "id":"foundation-pack-1",
              "curriculumVersion":"foundation-v1",
              "batchNo":1,
              "titleZh":"基础句法一",
              "descriptionZh":"从格关系和修饰关系开始。",
              "topicCount":1,
              "questionCount":4,
              "domainQuotas":{"syntax":1},
              "status":"published",
              "qualityScore":97
            }
            """.trimIndent(),
        )
    }

    private fun questionJson(): JSONObject {
        return JSONObject(
            """
            {
              "id":"foundation-question-1",
              "packId":"foundation-pack-1",
              "topicId":"foundation-topic-1",
              "curriculumVersion":"foundation-v1",
              "stage":"F2",
              "questionType":"syntax_relation",
              "sourceKind":"constructed_dialogue",
              "stimulus":{
                "kind":"dialogue",
                "turns":[
                  {"speaker":"店員","jaText":"袋にお入れしますか。","zhText":"要装进袋子里吗？"},
                  {"speaker":"客","jaText":"そのままで大丈夫です。","zhText":"这样就可以。"}
                ],
                "zhContext":"店员与顾客正在确认包装方式。"
              },
              "promptZh":"「袋に」与谓语是什么关系？",
              "options":[
                {"id":"A","text":"动作发生的场所"},
                {"id":"B","text":"变化后的容器或归着点"},
                {"id":"C","text":"动作的直接对象"},
                {"id":"D","text":"动作的施事"}
              ],
              "answer":{"optionId":"B"},
              "hintZh":"关注「入れる」表示的移动终点。",
              "explanationZh":"这里的「に」标记放入动作的归着点。",
              "deepExplanationZh":"格助词的解释需要与谓词的语义价结合。",
              "cautionNoteZh":"不能看到「に」就一律判断为静态存在地点。",
              "wrongExplanations":{
                "A":"场所读法没有表达进入后的终点。",
                "C":"直接对象是被放入的事物，而不是袋子。",
                "D":"袋子不是执行动作的人。"
              },
              "transferExampleJa":"箱に本を入れます。",
              "transferExplanationZh":"「箱に」同样标记归着点。",
              "difficulty":2,
              "tags":["syntax","case"],
              "sortOrder":20,
              "status":"published",
              "qualityScore":98,
              "contentVersion":3,
              "contentHash":"${"a".repeat(64)}"
            }
            """.trimIndent(),
        )
    }
}
