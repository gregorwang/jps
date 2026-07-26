package com.animejapaneselab.nativeapp.data

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.Closeable
import java.net.ServerSocket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class RemoteLabClientTest {
    private lateinit var server: LocalJsonServer

    @Before
    fun setUp() {
        server = LocalJsonServer(
            mapOf(
                "/api/works/test-work/episodes/7/vocab" to vocabJson(17),
                "/api/works/test-work/episodes/7/grammar" to grammarJson(11),
                "/api/works/test-work/episodes/7/sentences" to sentencesJson(12),
                "/api/works/test-work/episodes/7/exercises?limit=600" to exercisesJson(),
                "/api/works/test-work/episodes/7/subtitles" to subtitleLinesJson(),
                "/api/linguistic-exercises?workSlug=test-work&episode=7" to linguisticJson(),
            ),
        )
    }

    @Test
    fun fetchSubtitleLinesParsesWorkerTimelineShape() {
        val client = RemoteLabClient(server.baseUrl)
        val lines = client.fetchSubtitleLines(EpisodeSelection("test-work", 7))

        assertEquals(2, lines.size)
        assertEquals(12, lines.first().lineNo)
        assertEquals("00:00:12.100", lines.first().startTime)
        assertEquals("そろそろ起きないと。", lines.first().jaText)
        assertEquals("再五分钟……", lines.last().zhText)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun fetchEpisodePayloadKeepsFullRemoteListsAndParsesFields() {
        val client = RemoteLabClient(server.baseUrl)
        val payload = client.fetchEpisodePayload(EpisodeSelection("test-work", 7))

        assertEquals(17, payload.vocab.size)
        assertEquals("vocab-17", payload.vocab.last().id)
        assertEquals("意味17", payload.vocab.last().meaningZh)
        assertEquals(11, payload.grammar.size)
        assertEquals("grammar-11", payload.grammar.last().id)
        assertEquals(11, payload.grammar.last().sourceLineNo)
        assertEquals(12, payload.shadowing.size)
        assertEquals("sentence-12", payload.shadowing.last().id)
        assertEquals("EP07 第 12 行", payload.shadowing.last().sourceLabel)
        assertEquals(AudioKind.Source, payload.shadowing.last().audioKind)
        assertEquals("https://cdn.example.test/test-work/ep07/sentence-12.mp3", payload.shadowing.last().audioUrl)
        assertEquals(4, payload.exercises.size)
        assertEquals("exercise-vocab-1", payload.exercises.first().id)
        assertEquals("vocab_meaning", payload.exercises.first().exerciseType)
        assertEquals("vocab-1", payload.exercises.first().vocabItemId)
        assertEquals("安静", payload.exercises.first().answer)
    }

    @Test
    fun fetchWorksAndEpisodesParseWorkerCatalogShape() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/works" to """
                [
                  {"id":"work-1","slug":"k-on","displayName":"K-ON!","episodeCount":3},
                  {"id":"work-2","slug":"re-zero","displayName":"Re:Zero","episodeCount":5}
                ]
                """.trimIndent(),
                "/api/works/re-zero/episodes" to """
                [
                  {
                    "id":"ep-2",
                    "workSlug":"re-zero",
                    "workDisplayName":"Re:Zero",
                    "episode":2,
                    "totalCues":488,
                    "jaLines":432,
                    "zhLines":420,
                    "usableJaLines":432,
                    "chunkCount":10,
                    "usableAsMainCorpus":true
                  }
                ]
                """.trimIndent(),
            ),
        )

        val client = RemoteLabClient(server.baseUrl)
        val works = client.fetchWorks()
        val episodes = client.fetchEpisodes("re-zero")

        assertEquals(listOf("k-on", "re-zero"), works.map { it.slug })
        assertEquals("K-ON!", works.first().displayName)
        assertEquals(5, works.last().episodeCount)
        assertEquals("GET", server.requestsFor("/api/works").last().method)
        assertEquals("ep-2", episodes.single().id)
        assertEquals("re-zero", episodes.single().workSlug)
        assertEquals("Re:Zero", episodes.single().workDisplayName)
        assertEquals(2, episodes.single().episode)
        assertEquals(488, episodes.single().totalCues)
        assertEquals(432, episodes.single().usableJaLines)
        assertEquals(10, episodes.single().chunkCount)
    }

    @Test
    fun fetchEpisodesUrlEncodesWorkSlug() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/works/work+with+space/episodes" to "[]",
            ),
        )

        val episodes = RemoteLabClient(server.baseUrl).fetchEpisodes("work with space")

        assertEquals(emptyList<EpisodeOption>(), episodes)
        assertEquals("GET", server.requestsFor("/api/works/work+with+space/episodes").last().method)
    }

    @Test
    fun fetchEpisodePlanParsesWorkerPlanShape() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/works/k-on/episodes/1/plan" to episodePlanJson(),
            ),
        )

        val plan = RemoteLabClient(server.baseUrl).fetchEpisodePlan(EpisodeSelection("k-on", 1))

        assertNotNull(plan)
        requireNotNull(plan)
        assertEquals("k-on-ep01-plan", plan.id)
        assertEquals("k-on", plan.workSlug)
        assertEquals(1, plan.episode)
        assertEquals(20, plan.vocabCount)
        assertEquals(10, plan.handwritingCount)
        assertEquals(5, plan.shadowingCount)
        assertEquals(5, plan.grammarCount)
        assertEquals(17, plan.exerciseCount)
        assertEquals(listOf("vocab-1", "vocab-2"), plan.vocabItemIds)
        assertEquals(listOf("write-1"), plan.handwritingVocabIds)
        assertEquals(listOf("sentence-1", "sentence-2"), plan.shadowingSentenceIds)
        assertEquals(listOf("grammar-1"), plan.grammarPointIds)
        assertEquals(listOf("exercise-1", "exercise-2", "exercise-3"), plan.exerciseIds)
        assertEquals("第1集学习计划", plan.notes)
    }

    @Test
    fun fetchEpisodePlanParsesMergedWorkerPlanShape() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/works/re-zero/episodes/26/plan" to """
                {
                  "id": "re-zero-ep26-plan-merged",
                  "workSlug": "re-zero",
                  "episode": 26,
                  "planSlot": null,
                  "vocabCount": 54,
                  "handwritingCount": 0,
                  "shadowingCount": 15,
                  "grammarCount": 26,
                  "exerciseCount": 149,
                  "vocabItemIds": ["vocab-1", "vocab-2"],
                  "handwritingVocabIds": [],
                  "shadowingSentenceIds": ["sentence-1"],
                  "grammarPointIds": ["grammar-1", "grammar-2"],
                  "exerciseIds": ["exercise-1", "exercise-2", "exercise-3"],
                  "notes": "merged"
                }
                """.trimIndent(),
            ),
        )

        val plan = RemoteLabClient(server.baseUrl).fetchEpisodePlan(EpisodeSelection("re-zero", 26))

        assertNotNull(plan)
        requireNotNull(plan)
        assertNull(plan.planSlot)
        assertEquals(54, plan.vocabCount)
        assertEquals(149, plan.exerciseCount)
        assertEquals(listOf("sentence-1"), plan.shadowingSentenceIds)
        assertEquals(listOf("grammar-1", "grammar-2"), plan.grammarPointIds)
        assertEquals(listOf("exercise-1", "exercise-2", "exercise-3"), plan.exerciseIds)
    }

    @Test
    fun fetchEpisodePlanReturnsNullForMissingWorkerPlan() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/works/k-on/episodes/9/plan" to "null",
            ),
        )

        val plan = RemoteLabClient(server.baseUrl).fetchEpisodePlan(EpisodeSelection("k-on", 9))

        assertNull(plan)
    }

    @Test
    fun fetchLinguisticExercisesParsesCurrentAndLegacyFields() {
        val client = RemoteLabClient(server.baseUrl)
        val exercises = client.fetchLinguisticExercises(EpisodeSelection("test-work", 7))

        assertEquals(1, exercises.size)
        val exercise = exercises.first()
        assertEquals("ling-1", exercise.id)
        assertEquals("test-work", exercise.workSlug)
        assertEquals(7, exercise.episode)
        assertEquals("pragmatics", exercise.domain)
        assertEquals("kuuki_yomi", exercise.questionType)
        assertEquals(listOf("字面确认", "在缓和提醒"), exercise.options)
        assertEquals("在缓和提醒", exercise.correctOption)
        assertEquals("语气依据", exercise.answer.rationaleZh)
        assertEquals(2, exercise.sceneLines.size)
        assertEquals(true, exercise.sceneLines.first().isTarget)
        assertEquals("柔和提醒", exercise.phenomenonNameZh)
    }

    @Test
    fun parseLinguisticExercisesSupportsStringOptionsAndCorrectIndex() {
        val exercises = parseLinguisticExercisesJson(
            """
            [
              {
                "id": "ling-2",
                "work_slug": "k-on",
                "episode": 1,
                "ja_text": "そろそろ起きないと。",
                "domain": "pragmatics",
                "phenomenon_key": "soft_obligation_ellipsis",
                "question_type": "kuuki_yomi",
                "prompt": "这句话的空气是什么？",
                "options": ["命令", "柔和提醒", "转移话题"],
                "answer": {"correct_index": 1, "rationale_zh": "ないと 后半省略"}
              }
            ]
            """.trimIndent(),
        )

        val exercise = exercises.first()
        assertEquals("柔和提醒", exercise.correctOption)
        assertEquals(true, exercise.isCorrect("柔和提醒"))
        assertEquals(false, exercise.isCorrect("命令"))
    }

    @Test
    fun buildLinguisticProgressPayloadContainsAndroidReviewFields() {
        val exercise = parseLinguisticExercisesJson(linguisticJson()).first()
        val payload = buildLinguisticProgressPayload(exercise, "字面确认")

        assertEquals("这段对话的潜台词是什么？", payload.getString("label"))
        assertEquals("字面确认", payload.getString("selected"))
        assertEquals("在缓和提醒", payload.getString("answer"))
        assertEquals("pragmatics", payload.getString("domain"))
        assertEquals("soft_obligation_ellipsis", payload.getString("phenomenonKey"))
        assertEquals("kuuki_yomi", payload.getString("questionType"))
        assertEquals("line-1", payload.getString("sourceId"))
    }

    @Test
    fun loginCapturesSessionCookieAndSendsItOnAuthenticatedRequests() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/auth/login" to """{"user":{"id":"user-1","email":"owner@example.test"}}""",
                "/api/auth/me" to """{"user":{"id":"user-1","email":"owner@example.test"}}""",
            ),
            responseHeaders = mapOf(
                "/api/auth/login" to listOf("Set-Cookie: ajl_session=session-token; Max-Age=2592000; HttpOnly; Secure; SameSite=Lax; Path=/"),
            ),
        )

        val login = RemoteLabClient(server.baseUrl).loginOwner(
            email = "owner@example.test",
            password = "password123",
            deviceId = "device-test",
        )
        val user = RemoteLabClient(server.baseUrl, login.sessionCookie).fetchAuthMe()
        val loginRequest = server.requestsFor("/api/auth/login").last()
        val loginBody = JSONObject(loginRequest.body)

        assertEquals("owner@example.test", login.user.email)
        assertEquals("ajl_session=session-token", login.sessionCookie)
        assertEquals("POST", loginRequest.method)
        assertEquals("owner@example.test", loginBody.getString("email"))
        assertEquals("password123", loginBody.getString("password"))
        assertEquals("device-test", loginBody.getString("deviceHint"))
        assertNotNull(user)
        assertEquals("owner@example.test", user?.email)
        assertEquals("ajl_session=session-token", server.requestHeadersFor("/api/auth/me").last()["cookie"])
    }

    @Test
    fun fetchAuthMeReturnsNullForAnonymousDeviceAndStillSendsCookieWhenPresent() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/auth/me" to """{"user":null}""",
            ),
        )

        val user = RemoteLabClient(server.baseUrl, "ajl_session=session-token").fetchAuthMe()

        assertNull(user)
        assertEquals("GET", server.requestsFor("/api/auth/me").last().method)
        assertEquals("ajl_session=session-token", server.requestHeadersFor("/api/auth/me").last()["cookie"])
    }

    @Test
    fun claimCurrentDevicePostsDeviceIdWithSessionCookieAndParsesMergedCounts() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/auth/claim-device" to """{"merged":{"progress":3,"corrections":2,"aiInteractions":1}}""",
            ),
        )

        val merged = RemoteLabClient(server.baseUrl, "ajl_session=session-token").claimCurrentDevice("device-test")

        val request = server.requestsFor("/api/auth/claim-device").last()
        assertEquals("POST", request.method)
        assertEquals("ajl_session=session-token", request.headers["cookie"])
        assertEquals("application/json", request.headers["content-type"])
        assertEquals("device-test", JSONObject(request.body).getString("deviceId"))
        assertEquals(3, merged["progress"])
        assertEquals(2, merged["corrections"])
        assertEquals(1, merged["aiInteractions"])
    }

    @Test
    fun logoutOwnerPostsWithSessionCookie() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/auth/logout" to """{"ok":true}""",
            ),
        )

        RemoteLabClient(server.baseUrl, "ajl_session=session-token").logoutOwner()

        val request = server.requestsFor("/api/auth/logout").last()
        assertEquals("POST", request.method)
        assertEquals("ajl_session=session-token", request.headers["cookie"])
        assertEquals("{}", request.body)
    }

    @Test
    fun fetchProgressAndReviewTasksUseAccountSessionCookieWithoutDeviceQuery() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/progress" to progressJson("progress-1", "known"),
                "/api/review/today" to """{"tasks":${progressJson("review-1", "bad")}}""",
            ),
        )

        val client = RemoteLabClient(server.baseUrl, "ajl_session=session-token")
        val progress = client.fetchProgress("device-test")
        val review = client.fetchReviewTasks("device-test")

        assertEquals("progress-1", progress.single().itemId)
        assertEquals(ReviewState.Known, progress.single().state)
        assertEquals("review-1", review.single().itemId)
        assertEquals(ReviewState.Bad, review.single().state)
        assertEquals("ajl_session=session-token", server.requestHeadersFor("/api/progress").last()["cookie"])
        assertEquals("ajl_session=session-token", server.requestHeadersFor("/api/review/today").last()["cookie"])
    }

    @Test
    fun saveProgressPostsWorkerContractBodyAndParsesMappedProgress() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/progress" to progressJsonObject("exercise-1", "bad"),
            ),
        )
        val payload = JSONObject()
            .put("label", "读空气错题")
            .put("selected", "错选项")
            .put("answer", "正解")

        val progress = RemoteLabClient(server.baseUrl, "ajl_session=session-token").saveProgress(
            deviceId = "device-test",
            itemId = "exercise-1",
            itemType = "exercise",
            selection = EpisodeSelection(workSlug = "re-zero", episode = 2),
            state = ReviewState.Bad,
            label = "ignored when payload is supplied",
            payload = payload,
        )

        val request = server.requestsFor("/api/progress").last()
        val body = JSONObject(request.body)
        assertEquals("POST", request.method)
        assertEquals("ajl_session=session-token", request.headers["cookie"])
        assertEquals(false, body.has("deviceId"))
        assertEquals("exercise-1", body.getString("itemId"))
        assertEquals("exercise", body.getString("itemType"))
        assertEquals("re-zero", body.getString("workSlug"))
        assertEquals(2, body.getInt("episode"))
        assertEquals("bad", body.getString("state"))
        assertEquals("错选项", body.getJSONObject("payload").getString("selected"))
        assertEquals("exercise-1", progress.itemId)
        assertEquals(ReviewState.Bad, progress.state)
        assertEquals("读空气错题", progress.label)
    }

    @Test
    fun externalLinguisticPromptContainsUserAnswerAndScene() {
        val exercise = parseLinguisticExercisesJson(linguisticJson()).first()
        val prompt = buildExternalQuestionPrompt(exercise, "字面确认")

        assertTrue(prompt.contains("【我的答案】字面确认"))
        assertTrue(prompt.contains("【正确答案】在缓和提醒"))
        assertTrue(prompt.contains("そろそろ起きないと。"))
        assertTrue(prompt.contains("语境线索 -> 选项对比 -> 正确判断 -> 可迁移判断方法"))
    }

    @Test
    fun askAiPostsWorkerContractBodyAndParsesStructuredSections() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/ai/explain" to """
                {
                  "title": "AI 精讲",
                  "summary": "先看语气。",
                  "text": "",
                  "sections": [
                    {"title": "判断线索", "body": "关系压力来自省略。"},
                    {"title": "", "body": ""}
                  ]
                }
                """.trimIndent(),
            ),
        )

        val result = RemoteLabClient(server.baseUrl, "ajl_session=session-token").askAi(
            deviceId = "device-test",
            model = "gemini-3.1-flash-lite",
            reasoningEffort = "high",
            kind = "linguistic",
            text = "そろそろ起きないと。",
            context = "用户选择与正确答案",
        )

        val request = server.requestsFor("/api/ai/explain").last()
        val body = JSONObject(request.body)
        assertEquals("POST", request.method)
        assertEquals("ajl_session=session-token", request.headers["cookie"])
        assertEquals("device-test", body.getString("deviceId"))
        assertEquals("gemini-3.1-flash-lite", body.getString("model"))
        assertEquals("high", body.getString("reasoningEffort"))
        assertEquals("linguistic", body.getString("kind"))
        assertEquals("そろそろ起きないと。", body.getString("text"))
        assertEquals("用户选择与正确答案", body.getString("context"))
        assertEquals("AI 精讲", result.title)
        assertEquals("先看语气。", result.summary)
        assertEquals(listOf(AiExplainSection("判断线索", "关系压力来自省略。")), result.sections)
        assertEquals("先看语气。\n\n判断线索：关系压力来自省略。", result.text)
    }
}

private class LocalJsonServer(
    private val responses: Map<String, String>,
    private val responseHeaders: Map<String, List<String>> = emptyMap(),
) : Closeable {
    private val socket = ServerSocket(0)
    private val receivedRequests = mutableMapOf<String, MutableList<LocalRequest>>()
    val baseUrl: String = "http://127.0.0.1:${socket.localPort}"
    private val worker = thread(start = true, isDaemon = true) {
        while (!socket.isClosed) {
            try {
                socket.accept().use { client ->
                    val reader = client.getInputStream().bufferedReader(StandardCharsets.UTF_8)
                    val requestLine = reader.readLine().orEmpty()
                    val requestHeaders = generateSequence { reader.readLine() }
                        .takeWhile { it.isNotEmpty() }
                        .mapNotNull { line ->
                            val separator = line.indexOf(':')
                            if (separator <= 0) null else line.substring(0, separator).lowercase() to line.substring(separator + 1).trim()
                        }
                        .toMap()
                    val contentLength = requestHeaders["content-length"]?.toIntOrNull() ?: 0
                    val body = if (contentLength > 0) {
                        CharArray(contentLength).also { reader.read(it, 0, contentLength) }.concatToString()
                    } else {
                        ""
                    }
                    val method = requestLine.split(" ").getOrNull(0).orEmpty()
                    val path = requestLine.split(" ").getOrNull(1).orEmpty()
                    synchronized(receivedRequests) {
                        receivedRequests.getOrPut(path) { mutableListOf() }.add(
                            LocalRequest(
                                method = method,
                                path = path,
                                headers = requestHeaders,
                                body = body,
                            ),
                        )
                    }
                    val responseBody = responses[path] ?: "[]"
                    val bodyBytes = responseBody.toByteArray(StandardCharsets.UTF_8)
                    val header = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: application/json\r\n")
                        responseHeaders[path].orEmpty().forEach { append(it).append("\r\n") }
                        append("Content-Length: ${bodyBytes.size}\r\n")
                        append("Connection: close\r\n")
                        append("\r\n")
                    }.toByteArray(StandardCharsets.UTF_8)
                    client.getOutputStream().use { output ->
                        output.write(header)
                        output.write(bodyBytes)
                    }
                }
            } catch (_: SocketException) {
                return@thread
            }
        }
    }

    fun requestHeadersFor(path: String): List<Map<String, String>> {
        return requestsFor(path).map { it.headers }
    }

    fun requestsFor(path: String): List<LocalRequest> {
        return synchronized(receivedRequests) { receivedRequests[path].orEmpty().toList() }
    }

    override fun close() {
        socket.close()
        worker.join(1_000)
    }
}

private data class LocalRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String,
)

private fun vocabJson(count: Int): String {
    return (1..count).joinToString(prefix = "[", postfix = "]") { index ->
        """
        {
          "id": "vocab-$index",
          "surface": "単語$index",
          "reading": "たんご$index",
          "romaji": "tango$index",
          "meaningZh": "意味$index",
          "pos": "名詞",
          "jlptLevel": "N5",
          "animeToneNote": "语气$index",
          "realWorldNote": "现实$index"
        }
        """.trimIndent()
    }
}

private fun grammarJson(count: Int): String {
    return (1..count).joinToString(prefix = "[", postfix = "]") { index ->
        """
        {
          "id": "grammar-$index",
          "pattern": "〜て$index",
          "functionZh": "功能$index",
          "jaExample": "例文$index",
          "realWorldNote": "现实$index",
          "explanationZh": "解释$index",
          "pragmaticsNote": "语用$index",
          "difficulty": "N4",
          "sourceLineNo": $index
        }
        """.trimIndent()
    }
}

private fun sentencesJson(count: Int): String {
    return (1..count).joinToString(prefix = "[", postfix = "]") { index ->
        val audioUrl = if (index == count) "https://cdn.example.test/test-work/ep07/sentence-$index.mp3" else ""
        """
        {
          "id": "sentence-$index",
          "jaText": "台詞$index",
          "meaningZh": "中文$index",
          "romaji": "serifu$index",
          "sourceLineNo": $index,
          "audioUrl": "$audioUrl",
          "storagePath": ""
        }
        """.trimIndent()
    }
}

private fun exercisesJson(): String =
    """
    [
      {
        "id":"exercise-vocab-1",
        "exerciseType":"vocab_meaning",
        "prompt":"「静か」在本集语境中的中文意思是？",
        "answer":"安静",
        "hint":"参考读音：しずか。",
        "difficulty":"N5",
        "vocabItemId":"vocab-1"
      },
      {
        "id":"exercise-vocab-2",
        "exercise_type":"vocab_meaning",
        "prompt":"「元気」在本集语境中的中文意思是？",
        "answer":"有精神",
        "hint":"参考读音：げんき。",
        "difficulty":"N5",
        "vocab_item_id":"vocab-2"
      },
      {
        "id":"exercise-vocab-3",
        "exerciseType":"vocab_meaning",
        "prompt":"「大切」在本集语境中的中文意思是？",
        "answer":"重要",
        "hint":"参考读音：たいせつ。",
        "difficulty":"N5",
        "vocabItemId":"vocab-3"
      },
      {
        "id":"exercise-vocab-4",
        "exerciseType":"vocab_meaning",
        "prompt":"「約束」在本集语境中的中文意思是？",
        "answer":"约定",
        "hint":"参考读音：やくそく。",
        "difficulty":"N5",
        "vocabItemId":"vocab-4"
      }
    ]
    """.trimIndent()

private fun subtitleLinesJson(): String {
    return """
    [
      {
        "line_no": 12,
        "start_time": "00:00:12.100",
        "end_time": "00:00:14.000",
        "ja_text": "そろそろ起きないと。",
        "zh_text": "差不多该起床了。"
      },
      {
        "line_no": 13,
        "start_time": "00:00:14.200",
        "end_time": "00:00:16.000",
        "ja_text": "あと五分……。",
        "zh_text": "再五分钟……"
      }
    ]
    """.trimIndent()
}

private fun episodePlanJson(): String {
    return """
    {
      "id": "k-on-ep01-plan",
      "workSlug": "k-on",
      "episode": 1,
      "vocabCount": 20,
      "handwritingCount": 10,
      "shadowingCount": 5,
      "grammarCount": 5,
      "exerciseCount": 17,
      "vocabItemIds": ["vocab-1", "vocab-2"],
      "handwritingVocabIds": ["write-1"],
      "shadowingSentenceIds": ["sentence-1", "sentence-2"],
      "grammarPointIds": ["grammar-1"],
      "exerciseIds": ["exercise-1", "exercise-2", "exercise-3"],
      "notes": "第1集学习计划"
    }
    """.trimIndent()
}

private fun linguisticJson(): String {
    return """
    [
      {
        "id": "ling-1",
        "batchId": "batch-1",
        "workSlug": "test-work",
        "episode": 7,
        "sourceId": "line-1",
        "sourceLineNo": 12,
        "jaText": "そろそろ起きないと。",
        "zhText": "差不多该起床了。",
        "sceneLines": [
          {"lineNo": 12, "speaker": "妹妹", "jaText": "そろそろ起きないと。", "zhText": "差不多该起床了。", "isTarget": true},
          {"lineNo": 13, "speaker": "姐姐", "jaText": "あと五分……。", "zhText": "再五分钟……", "isTarget": false}
        ],
        "targetLineNo": 12,
        "domain": "pragmatics",
        "phenomenonKey": "soft_obligation_ellipsis",
        "questionType": "kuuki_yomi",
        "prompt": "这段对话的潜台词是什么？",
        "optionItems": [
          {"key": "a", "label": "字面确认"},
          {"key": "b", "label": "在缓和提醒"}
        ],
        "answer": {"correct_key": "b", "answer_zh": "在缓和提醒", "rationale_zh": "语气依据"},
        "hint": "不是命令形",
        "basicExplanationZh": "省略后半句保留柔和压力。",
        "deepExplanationZh": "そろそろ 先缓冲时间压力。",
        "animeContextNoteZh": "姐妹日常对话。",
        "cautionNoteZh": "不要只按字面翻译。",
        "difficulty": "starter",
        "qualityScore": 88,
        "status": "published",
        "phenomenonNameZh": "柔和提醒",
        "phenomenonNameJa": "やわらかい注意",
        "phenomenonDefinitionZh": "用省略表达关系压力。"
      }
    ]
    """.trimIndent()
}

private fun progressJson(itemId: String, state: String): String {
    return """
    [
      {
        "itemId": "$itemId",
        "itemType": "vocab",
        "workSlug": "test-work",
        "episode": 7,
        "state": "$state",
        "payload": {"label": "同步条目"},
        "lastReviewedAt": "2026-06-30T00:00:00Z",
        "nextReviewOn": "2026-07-01"
      }
    ]
    """.trimIndent()
}

private fun progressJsonObject(itemId: String, state: String): String {
    return """
    {
      "itemId": "$itemId",
      "itemType": "exercise",
      "workSlug": "re-zero",
      "episode": 2,
      "state": "$state",
      "payload": {"label": "读空气错题", "selected": "错选项", "answer": "正解"},
      "lastReviewedAt": "2026-06-30T00:00:00Z",
      "nextReviewOn": "2026-07-01"
    }
    """.trimIndent()
}
