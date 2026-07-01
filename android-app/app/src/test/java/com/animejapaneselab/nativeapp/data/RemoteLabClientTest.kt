package com.animejapaneselab.nativeapp.data

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
                "/api/linguistic-exercises?workSlug=test-work&episode=7" to linguisticJson(),
            ),
        )
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
    fun fetchLinguisticExercisesParsesWebDraftFields() {
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
    fun buildLinguisticProgressPayloadMatchesWebReviewFields() {
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
    fun fetchProgressAndReviewTasksUseDeviceIdAndSessionCookie() {
        server.close()
        server = LocalJsonServer(
            responses = mapOf(
                "/api/progress?deviceId=device-test" to progressJson("progress-1", "known"),
                "/api/review/today?deviceId=device-test" to """{"tasks":${progressJson("review-1", "bad")}}""",
            ),
        )

        val client = RemoteLabClient(server.baseUrl, "ajl_session=session-token")
        val progress = client.fetchProgress("device-test")
        val review = client.fetchReviewTasks("device-test")

        assertEquals("progress-1", progress.single().itemId)
        assertEquals(ReviewState.Known, progress.single().state)
        assertEquals("review-1", review.single().itemId)
        assertEquals(ReviewState.Bad, review.single().state)
        assertEquals("ajl_session=session-token", server.requestHeadersFor("/api/progress?deviceId=device-test").last()["cookie"])
        assertEquals("ajl_session=session-token", server.requestHeadersFor("/api/review/today?deviceId=device-test").last()["cookie"])
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
        assertEquals("device-test", body.getString("deviceId"))
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
