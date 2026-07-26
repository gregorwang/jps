package com.animejapaneselab.nativeapp.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

data class RemoteEpisodePayload(
    val vocab: List<VocabItem>,
    val grammar: List<GrammarPoint>,
    val shadowing: List<ShadowingSentence>,
    val exercises: List<LearningExercise>,
)

data class AuthUser(
    val id: String,
    val email: String,
)

data class AuthLoginResult(
    val user: AuthUser,
    val sessionCookie: String,
)

class RemoteLabClient(
    private val baseUrl: String,
    private val sessionCookie: String = "",
    pronunciationBaseUrl: String = DefaultPronunciationApiBaseUrl,
    private val contentCache: EpisodeContentCache? = null,
) {
    private val normalizedBase = baseUrl.trim().trimEnd('/')
    private val normalizedPronunciationBase = pronunciationBaseUrl.trim().trimEnd('/')

    fun fetchAuthMe(): AuthUser? {
        val response = JSONObject(get("/api/auth/me"))
        return response.optJSONObject("user")?.authUser()
    }

    fun loginOwner(email: String, password: String, deviceId: String): AuthLoginResult {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("deviceHint", deviceId)
        val response = requestDetailed("POST", "/api/auth/login", body.toString())
        val user = JSONObject(response.body).optJSONObject("user")?.authUser()
            ?: error("Login response did not include a user")
        val cookie = response.sessionCookie.ifBlank { error("Login response did not include a session cookie") }
        return AuthLoginResult(user = user, sessionCookie = cookie)
    }

    fun logoutOwner() {
        post("/api/auth/logout", JSONObject())
    }

    /**
     * Changes the account password; the server keeps the current session valid and
     * revokes all others. Throws on wrong old password (HTTP 401) or weak new one.
     */
    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        val body = JSONObject()
            .put("oldPassword", oldPassword)
            .put("newPassword", newPassword)
        val response = JSONObject(post("/api/auth/change-password", body))
        return response.optBoolean("ok", false)
    }

    fun fetchAiModels(): List<AiModelOption> {
        return parseAiModelsJson(get("/api/ai/models"))
    }

    fun createPronunciationTicket(sentenceId: String): String {
        val body = JSONObject().put("sentenceId", sentenceId)
        val response = JSONObject(post("/api/pronunciation/ticket", body))
        return response.optString("ticket").ifBlank {
            error("Pronunciation ticket response did not include a ticket")
        }
    }

    fun evaluatePronunciation(
        ticket: String,
        sentenceId: String,
        attemptId: String,
        wavBytes: ByteArray,
    ): PronunciationEvaluation {
        check(normalizedPronunciationBase.isNotBlank()) { "Pronunciation API base URL is empty" }
        val boundary = "ajl-${UUID.randomUUID()}"
        val connection = (
            URL("$normalizedPronunciationBase/v1/pronunciation/evaluate")
                .openConnection() as HttpURLConnection
            ).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setChunkedStreamingMode(16 * 1024)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $ticket")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            connection.outputStream.use { output ->
                writePronunciationMultipart(
                    output = output,
                    boundary = boundary,
                    sentenceId = sentenceId,
                    attemptId = attemptId,
                    wavBytes = wavBytes,
                )
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
            }.orEmpty()
            val json = runCatching { JSONObject(responseBody) }.getOrElse {
                throw PronunciationApiException(
                    httpStatus = status,
                    code = "invalid_response",
                    message = "Pronunciation API returned invalid JSON",
                )
            }
            val assessmentStatus = json.optString("assessmentStatus")
            if (status == 200 || (status == 422 && assessmentStatus == "re_record")) {
                return parsePronunciationEvaluation(json)
            }

            val error = json.optJSONObject("error")
            throw PronunciationApiException(
                httpStatus = status,
                code = error?.optString("code").orEmpty().ifBlank { "http_$status" },
                message = error?.optString("message").orEmpty().ifBlank {
                    "Pronunciation API HTTP $status"
                },
            )
        } finally {
            connection.disconnect()
        }
    }

    fun claimCurrentDevice(deviceId: String): Map<String, Int> {
        val body = JSONObject().put("deviceId", deviceId)
        val response = JSONObject(post("/api/auth/claim-device", body))
        val merged = response.optJSONObject("merged") ?: JSONObject()
        return merged.keys().asSequence().associateWith { key -> merged.optInt(key, 0) }
    }

    fun fetchWorks(): List<WorkOption> {
        val json = get("/api/works")
        return JSONArray(json).mapObjects { item ->
            WorkOption(
                id = item.string("id", item.string("slug")),
                slug = item.string("slug", item.string("id")),
                displayName = item.string("displayName", item.string("display_name", item.string("slug"))),
                episodeCount = item.optInt("episodeCount", item.optInt("episode_count", 1)),
            )
        }
    }

    fun fetchEpisodes(workSlug: String): List<EpisodeOption> {
        val json = get("/api/works/${workSlug.urlEncoded()}/episodes")
        return JSONArray(json).mapObjects { item ->
            EpisodeOption(
                id = item.string("id", "${workSlug}-ep${item.optInt("episode", 1)}"),
                workSlug = item.string("workSlug", item.string("work_slug", workSlug)),
                workDisplayName = item.string("workDisplayName", item.string("work_display_name", workSlug)),
                episode = item.optInt("episode", 1),
                totalCues = item.optInt("totalCues", item.optInt("total_cues", 0)),
                usableJaLines = item.optInt("usableJaLines", item.optInt("usable_ja_lines", 0)),
                chunkCount = item.optInt("chunkCount", item.optInt("chunk_count", 0)),
            )
        }
    }

    fun fetchEpisodePayload(selection: EpisodeSelection): RemoteEpisodePayload {
        return RemoteEpisodePayload(
            vocab = fetchVocab(selection),
            grammar = fetchGrammar(selection),
            shadowing = fetchSentences(selection),
            exercises = fetchExercises(selection),
        )
    }

    fun fetchExercises(selection: EpisodeSelection): List<LearningExercise> {
        val json = get(
            "/api/works/${selection.workSlug.urlEncoded()}/episodes/${selection.episode}/exercises?limit=600",
        )
        return JSONArray(json).mapObjects { item ->
            LearningExercise(
                id = item.string("id"),
                exerciseType = item.string("exerciseType", item.string("exercise_type")),
                prompt = item.string("prompt"),
                answer = item.string("answer"),
                hint = item.string("hint"),
                difficulty = item.string("difficulty"),
                vocabItemId = item.string("vocabItemId", item.string("vocab_item_id")),
            )
        }.filter { exercise ->
            exercise.id.isNotBlank() &&
                exercise.exerciseType.isNotBlank() &&
                exercise.prompt.isNotBlank() &&
                exercise.answer.isNotBlank()
        }
    }

    fun fetchEpisodePlan(selection: EpisodeSelection): EpisodePlan? {
        val json = get("/api/works/${selection.workSlug.urlEncoded()}/episodes/${selection.episode}/plan").trim()
        if (json.isBlank() || json == "null") return null
        return episodePlan(JSONObject(json))
    }

    fun fetchReviewTasks(deviceId: String): List<ProgressItem> {
        val json = get("/api/review/today")
        val tasks = JSONObject(json).optJSONArray("tasks") ?: JSONArray()
        return tasks.mapObjects(::progressItem)
    }

    fun fetchLinguisticExercises(selection: EpisodeSelection? = null): List<LinguisticExercise> {
        val query = buildList {
            if (selection != null) {
                add("workSlug=${selection.workSlug.urlEncoded()}")
                add("episode=${selection.episode}")
            }
        }.joinToString("&")
        val path = if (query.isBlank()) "/api/linguistic-exercises" else "/api/linguistic-exercises?$query"
        return parseLinguisticExercisesJson(get(path))
    }

    fun fetchProgress(deviceId: String): List<ProgressItem> {
        val json = get("/api/progress")
        return JSONArray(json).mapObjects(::progressItem)
    }

    fun fetchSubtitleLines(selection: EpisodeSelection): List<SubtitleLine> {
        val json = get("/api/works/${selection.workSlug.urlEncoded()}/episodes/${selection.episode}/subtitles")
        return JSONArray(json).mapObjects { item ->
            SubtitleLine(
                lineNo = item.optInt("lineNo", item.optInt("line_no", 0)),
                startTime = item.string("startTime", item.string("start_time")),
                endTime = item.string("endTime", item.string("end_time")),
                jaText = item.string("jaText", item.string("ja_text")),
                zhText = item.string("zhText", item.string("zh_text")),
            )
        }
    }

    fun saveProgress(
        deviceId: String,
        itemId: String,
        itemType: String,
        selection: EpisodeSelection,
        state: ReviewState,
        label: String,
        payload: JSONObject? = null,
    ): ProgressItem {
        val progressPayload = payload ?: JSONObject()
        if (!progressPayload.has("label")) progressPayload.put("label", label)
        val body = JSONObject()
            .put("itemId", itemId)
            .put("itemType", itemType)
            .put("workSlug", selection.workSlug)
            .put("episode", selection.episode)
            .put("state", state.remoteValue)
            .put("payload", progressPayload)
        val response = post("/api/progress", body)
        return progressItem(JSONObject(response))
    }

    fun askAi(
        deviceId: String,
        model: String,
        reasoningEffort: String,
        kind: String,
        text: String,
        context: String,
    ): AiExplainResult {
        val body = JSONObject()
            .put("deviceId", deviceId)
            .put("model", model)
            .put("reasoningEffort", reasoningEffort)
            .put("kind", kind)
            .put("text", text)
            .put("context", context)
        return parseAiExplainResult(JSONObject(post("/api/ai/explain", body)))
    }

    /**
     * Batch furigana annotation for up to 80 texts per call (server limit).
     * Keys of [items] are caller-chosen target ids echoed back in the result map.
     */
    fun fetchFuriganaBatch(targetType: String, items: Map<String, String>): Map<String, FuriganaResult> {
        if (items.isEmpty()) return emptyMap()
        val itemArray = JSONArray()
        items.forEach { (targetId, text) ->
            itemArray.put(JSONObject().put("targetId", targetId).put("text", text))
        }
        val body = JSONObject()
            .put("targetType", targetType)
            .put("items", itemArray)
        return parseFuriganaBatchJson(post("/api/ai/furigana/batch", body))
    }

    fun fetchSentenceDeepDive(
        workSlug: String,
        episode: Int,
        lineNo: Int,
        jaText: String,
        zhText: String,
        model: String,
        reasoningEffort: String,
        deviceId: String,
    ): AiExplainResult {
        val body = JSONObject()
            .put("workSlug", workSlug)
            .put("episode", episode)
            .put("lineNo", lineNo)
            .put("jaText", jaText)
            .put("zhText", zhText)
            .put("model", model)
            .put("reasoningEffort", reasoningEffort)
            .put("deviceId", deviceId)
        return parseAiExplainResult(JSONObject(post("/api/ai/sentence-deep-dive", body)))
    }

    /**
     * Semantic subtitle search over the Vectorize index. [workSlug] takes the catalog
     * slug (`re-zero`); conversion to the RAG index slug happens here.
     */
    fun searchSubtitles(
        query: String,
        workSlug: String,
        deviceId: String,
        episode: Int? = null,
        topK: Int = 8,
    ): RagSearchResult {
        val body = JSONObject()
            .put("query", query)
            .put("workSlug", ragWorkSlug(workSlug))
            .put("topK", topK.coerceIn(1, 50))
            .put("deviceId", deviceId)
        if (episode != null && episode > 0) body.put("episode", episode)
        return parseRagSearchJson(post("/api/rag/search", body))
    }

    fun fetchCharacterProfile(
        workSlug: String,
        characterKey: String,
        characterName: String,
        model: String,
        reasoningEffort: String,
        regenerate: Boolean = false,
    ): CharacterProfile {
        val body = JSONObject()
            .put("workSlug", workSlug)
            .put("characterKey", characterKey)
            .put("characterName", characterName)
            .put("model", model)
            .put("reasoningEffort", reasoningEffort)
        if (regenerate) body.put("regenerate", true)
        return parseCharacterProfileJson(post("/api/ai/character-profile", body))
    }

    fun fetchAiHistory(deviceId: String): AiHistorySnapshot {
        return parseAiHistoryJson(get("/api/history?deviceId=${deviceId.urlEncoded()}"))
    }

    fun fetchAiHistoryDetail(type: String, id: String, deviceId: String): AiHistoryDetail? {
        val path = "/api/history/detail?type=${type.urlEncoded()}&id=${id.urlEncoded()}&deviceId=${deviceId.urlEncoded()}"
        return parseAiHistoryDetailJson(get(path))
    }

    private fun fetchVocab(selection: EpisodeSelection): List<VocabItem> {
        val json = get("/api/works/${selection.workSlug.urlEncoded()}/episodes/${selection.episode}/vocab")
        return JSONArray(json).mapObjects { item ->
            VocabItem(
                id = item.string("id", "${selection.workSlug}-vocab-${item.string("surface")}"),
                surface = item.string("surface"),
                reading = item.string("reading", item.string("surface")),
                romanization = item.string("romaji", item.string("romanization")),
                meaningZh = item.string("meaningZh", item.string("meaning_zh", item.string("hint"))),
                partOfSpeech = item.string("pos", item.string("partOfSpeech", "表达")),
                level = item.string("jlptLevel", item.string("level", "N?")),
                occurrence = item.string("animeToneNote", item.string("occurrence", "线上词库")),
                toneTags = listOfNotNull(
                    item.string("animeToneNote").takeIf { it.isNotBlank() },
                    item.string("realWorldNote").takeIf { it.isNotBlank() },
                ).ifEmpty { listOf("线上") },
                realWorldNote = item.string("realWorldNote"),
                linguistic = parseLinguisticCardPayload(
                    item.optJSONObject("linguisticPayload") ?: item.optJSONObject("linguistic_payload"),
                ),
            )
        }
    }

    private fun fetchGrammar(selection: EpisodeSelection): List<GrammarPoint> {
        val json = get("/api/works/${selection.workSlug.urlEncoded()}/episodes/${selection.episode}/grammar")
        return JSONArray(json).mapObjects { item ->
            GrammarPoint(
                id = item.string("id", "${selection.workSlug}-grammar-${item.string("pattern")}"),
                pattern = item.string("pattern", "句末"),
                titleZh = item.string("functionZh", item.string("titleZh", "语气功能")),
                exampleJa = item.string("jaExample", item.string("exampleJa")),
                exampleZh = item.string("realWorldNote", item.string("exampleZh")),
                explanationZh = item.string("explanationZh", item.string("pragmaticsNote", "线上语法点")),
                pragmaticsNote = item.string("pragmaticsNote"),
                realWorldNote = item.string("realWorldNote"),
                difficulty = item.string("difficulty"),
                sourceLineNo = item.optInt("sourceLineNo", item.optInt("source_line_no", 0)),
                linguistic = parseLinguisticCardPayload(
                    item.optJSONObject("linguisticPayload") ?: item.optJSONObject("linguistic_payload"),
                ),
            )
        }
    }

    private fun fetchSentences(selection: EpisodeSelection): List<ShadowingSentence> {
        val json = get("/api/works/${selection.workSlug.urlEncoded()}/episodes/${selection.episode}/sentences")
        return JSONArray(json).mapObjects { item ->
            val audioUrl = item.string("audioUrl", item.string("audio_url"))
            val storagePath = item.string("storagePath", item.string("storage_path"))
            val audioKind = when {
                audioUrl.isNotBlank() || storagePath.isNotBlank() -> AudioKind.Source
                selection.workSlug == "re-zero" -> AudioKind.Source
                else -> AudioKind.Tts
            }
            val kana = item.string("reading", item.string("kana"))
            val romaji = item.string("romaji", item.string("romanization"))
            ShadowingSentence(
                id = item.string("id", "${selection.workSlug}-sentence-${item.optInt("sourceLineNo", 0)}"),
                ja = item.string("jaText", item.string("ja")),
                reading = kana.ifBlank { romaji },
                meaningZh = item.string("meaningZh", item.string("meaning_zh")),
                sourceLabel = "EP${selection.episode.toString().padStart(2, '0')} 第 ${item.optInt("sourceLineNo", 0)} 行",
                audioKind = audioKind,
                sourceLineNo = item.optInt("sourceLineNo", item.optInt("source_line_no", 0)),
                audioUrl = audioUrl,
                storagePath = storagePath,
                romaji = romaji,
                difficulty = item.string("difficulty"),
                toneTags = (item.optJSONArray("toneTags") ?: item.optJSONArray("tone_tags"))
                    ?.mapStrings().orEmpty(),
                linguistic = parseLinguisticCardPayload(
                    item.optJSONObject("linguisticPayload") ?: item.optJSONObject("linguistic_payload"),
                ),
            )
        }
    }

    private fun progressItem(item: JSONObject): ProgressItem {
        val payload = item.optJSONObject("payload")
        return ProgressItem(
            itemId = item.string("itemId", item.string("item_id")),
            itemType = item.string("itemType", item.string("item_type", "unknown")),
            workSlug = item.string("workSlug", item.string("work_slug")),
            episode = item.optInt("episode", 0),
            state = reviewState(item.string("state")),
            label = payload?.string("label").orEmpty().ifBlank {
                item.string("label", item.string("itemId", item.string("item_id")))
            },
            lastReviewedAt = item.string("lastReviewedAt", item.string("last_reviewed_at")),
            nextReviewOn = item.string("nextReviewOn", item.string("next_review_on")),
            payload = payload?.toStringMap().orEmpty(),
            ease = item.optInt("ease", 0),
            reviewCount = item.optInt("reviewCount", item.optInt("review_count", 0)),
        )
    }

    private fun episodePlan(item: JSONObject): EpisodePlan {
        val vocabItemIds = (item.optJSONArray("vocabItemIds") ?: item.optJSONArray("vocab_item_ids") ?: JSONArray()).mapStrings()
        val handwritingVocabIds = (item.optJSONArray("handwritingVocabIds") ?: item.optJSONArray("handwriting_vocab_ids") ?: JSONArray()).mapStrings()
        val shadowingSentenceIds = (item.optJSONArray("shadowingSentenceIds") ?: item.optJSONArray("shadowing_sentence_ids") ?: JSONArray()).mapStrings()
        val grammarPointIds = (item.optJSONArray("grammarPointIds") ?: item.optJSONArray("grammar_point_ids") ?: JSONArray()).mapStrings()
        val exerciseIds = (item.optJSONArray("exerciseIds") ?: item.optJSONArray("exercise_ids") ?: JSONArray()).mapStrings()
        return EpisodePlan(
            id = item.string("id"),
            workSlug = item.string("workSlug", item.string("work_slug")),
            episode = item.optInt("episode", 0),
            planSlot = item.optNullableInt("planSlot", "plan_slot"),
            vocabCount = item.optInt("vocabCount", item.optInt("vocab_count", vocabItemIds.size)),
            handwritingCount = item.optInt("handwritingCount", item.optInt("handwriting_count", handwritingVocabIds.size)),
            shadowingCount = item.optInt("shadowingCount", item.optInt("shadowing_count", shadowingSentenceIds.size)),
            grammarCount = item.optInt("grammarCount", item.optInt("grammar_count", grammarPointIds.size)),
            exerciseCount = item.optInt("exerciseCount", item.optInt("exercise_count", exerciseIds.size)),
            vocabItemIds = vocabItemIds,
            handwritingVocabIds = handwritingVocabIds,
            shadowingSentenceIds = shadowingSentenceIds,
            grammarPointIds = grammarPointIds,
            exerciseIds = exerciseIds,
            notes = item.string("notes"),
        )
    }

    private fun get(path: String): String {
        if (contentCache == null || !isCacheableContentPath(path)) {
            return request("GET", path, null)
        }
        val fresh = try {
            request("GET", path, null)
        } catch (error: java.io.IOException) {
            // Connectivity failure: fall back to the last good copy. HTTP errors
            // (IllegalStateException) deliberately do NOT serve stale content.
            return contentCache.read(path) ?: throw error
        }
        contentCache.write(path, fresh)
        return fresh
    }

    private fun post(path: String, body: JSONObject): String = request("POST", path, body.toString())

    private fun request(method: String, path: String, body: String?): String {
        return requestDetailed(method, path, body).body
    }

    private fun requestDetailed(method: String, path: String, body: String?): RemoteResponse {
        check(normalizedBase.isNotBlank()) { "API base URL is empty" }
        val connection = (URL("$normalizedBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Accept", "application/json")
            if (sessionCookie.isNotBlank()) {
                setRequestProperty("Cookie", sessionCookie)
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(StandardCharsets.UTF_8))
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
            }.orEmpty()
            val responseCookie = connection.headerFields.entries
                .firstOrNull { (key, _) -> key.equals("Set-Cookie", ignoreCase = true) }
                ?.value
                ?.firstOrNull()
                ?.substringBefore(';')
                .orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("HTTP $status ${response.take(180)}")
            }
            return RemoteResponse(body = response, sessionCookie = responseCookie)
        } finally {
            connection.disconnect()
        }
    }
}

private data class RemoteResponse(
    val body: String,
    val sessionCookie: String = "",
)

private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

internal fun writePronunciationMultipart(
    output: OutputStream,
    boundary: String,
    sentenceId: String,
    attemptId: String,
    wavBytes: ByteArray,
) {
    fun textPart(name: String, value: String): ByteArray = (
        "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"$name\"\r\n\r\n" +
            "$value\r\n"
        ).toByteArray(StandardCharsets.UTF_8)

    output.write(textPart("sentenceId", sentenceId))
    output.write(textPart("attemptId", attemptId))
    output.write(textPart("mode", "shadowing"))
    output.write(textPart("clientVersion", "android-0.1.0"))
    output.write(
        (
            "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"audio\"; filename=\"attempt.wav\"\r\n" +
                "Content-Type: audio/wav\r\n\r\n"
            ).toByteArray(StandardCharsets.UTF_8),
    )
    output.write(wavBytes)
    output.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
}

internal fun parsePronunciationEvaluation(json: JSONObject): PronunciationEvaluation {
    val assessmentStatus = PronunciationAssessmentStatus.entries.firstOrNull {
        it.remoteValue == json.optString("assessmentStatus")
    } ?: error("Pronunciation response has an unknown assessmentStatus")
    val engine = json.optJSONObject("engine") ?: JSONObject()
    val reference = json.optJSONObject("reference") ?: JSONObject()
    val recognized = json.optJSONObject("recognized")?.let { item ->
        PronunciationRecognition(
            text = item.optString("text"),
            phoneticKana = item.optString("phoneticKana"),
            durationMs = item.optLong("durationMs"),
        )
    }
    val score = json.optJSONObject("score")?.let { item ->
        PronunciationScore(
            overall = item.optInt("overall"),
            accuracy = item.optInt("accuracy"),
            completeness = item.optInt("completeness"),
            clarity = item.optInt("clarity"),
            fluency = item.optInt("fluency"),
            rhythm = item.optInt("rhythm"),
            band = item.optString("band"),
        )
    }
    val quality = json.optJSONObject("audioQuality") ?: JSONObject()
    val segments = json.optJSONArray("segments") ?: JSONArray()
    val feedback = json.optJSONArray("feedback") ?: JSONArray()
    return PronunciationEvaluation(
        attemptId = json.optString("attemptId"),
        sentenceId = json.optString("sentenceId"),
        assessmentStatus = assessmentStatus,
        scorerVersion = json.optString("scorerVersion"),
        reference = PronunciationReferenceResult(
            timingSource = reference.optString("timingSource", "native"),
        ),
        engine = PronunciationEngineResult(
            primary = engine.optString("primary"),
            secondary = engine.optString("secondary"),
            fallbackUsed = engine.optBoolean("fallbackUsed"),
            reliability = engine.optDouble("reliability", 0.0),
        ),
        recognized = recognized,
        score = score,
        audioQuality = PronunciationAudioQuality(
            status = quality.optString("status"),
            scorable = quality.optBoolean("scorable"),
            durationMs = quality.optLong("durationMs"),
            qualityScore = quality.optInt("qualityScore"),
            reasons = (quality.optJSONArray("reasons") ?: JSONArray()).mapStrings(),
        ),
        segments = buildList {
            for (index in 0 until segments.length()) {
                val item = segments.optJSONObject(index) ?: continue
                add(
                    PronunciationSegment(
                        tokenIndex = item.optInt("tokenIndex"),
                        surface = item.optString("surface"),
                        expected = item.optString("expected"),
                        heard = item.optString("heard"),
                        status = item.optString("status"),
                        score = item.optInt("score").takeIf { item.has("score") },
                        message = item.optString("message"),
                        startMs = item.optLong("startMs").takeIf { item.has("startMs") },
                        endMs = item.optLong("endMs").takeIf { item.has("endMs") },
                    ),
                )
            }
        },
        feedback = buildList {
            for (index in 0 until feedback.length()) {
                feedback.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        },
    )
}

internal fun parseAiExplainResult(response: JSONObject): AiExplainResult {
    val sections = response.optJSONArray("sections") ?: JSONArray()
    val parsedSections = sections.mapObjects { section ->
        val title = section.string("title")
        val body = section.string("body")
        AiExplainSection(
            title = title.ifBlank { "说明" },
            body = body,
        )
    }.filter { it.body.isNotBlank() || it.title != "说明" }
    val summary = response.string("summary", "智能讲解已返回，但没有结构化摘要。")
    val textResult = response.string("text").ifBlank {
        buildString {
            append(summary)
            parsedSections.forEach { section ->
                append("\n\n")
                append(section.title.ifBlank { "说明" })
                append("：")
                append(section.body)
            }
        }
    }
    return AiExplainResult(
        title = response.string("title", "智能精讲"),
        summary = summary,
        text = textResult,
        sections = parsedSections,
    )
}

internal fun parseLinguisticCardPayload(payload: JSONObject?): LinguisticCardPayload? {
    if (payload == null) return null
    val terms = (payload.optJSONArray("terms") ?: JSONArray()).mapObjects { term ->
        LinguisticCardTerm(
            termZh = term.string("termZh", term.string("term_zh")),
            plainZh = term.string("plainZh", term.string("plain_zh")),
        )
    }.filter { it.termZh.isNotBlank() }
    val domains = (payload.optJSONArray("domains") ?: JSONArray()).mapObjects { domain ->
        LinguisticCardDomain(
            domain = domain.string("domain"),
            titleZh = domain.string("titleZh", domain.string("title_zh")),
            takeawayZh = domain.string("takeawayZh", domain.string("takeaway_zh")),
            explanationZh = domain.string("explanationZh", domain.string("explanation_zh")),
        )
    }.filter { it.titleZh.isNotBlank() || it.explanationZh.isNotBlank() }
    val parsed = LinguisticCardPayload(
        level = payload.string("level"),
        headlineZh = payload.string("headlineZh", payload.string("headline_zh")),
        cautionZh = payload.string("cautionZh", payload.string("caution_zh")),
        historicalNoteZh = payload.string("historicalNoteZh", payload.string("historical_note_zh")),
        terms = terms,
        domains = domains,
    )
    return parsed.takeIf { it.hasContent }
}

internal fun parseFuriganaBatchJson(json: String): Map<String, FuriganaResult> {
    val response = runCatching { JSONObject(json) }.getOrElse { return emptyMap() }
    val results = response.optJSONObject("results") ?: return emptyMap()
    return buildMap {
        val keys = results.keys()
        while (keys.hasNext()) {
            val targetId = keys.next()
            val entry = results.optJSONObject(targetId) ?: continue
            val segmentsJson = entry.optJSONArray("ruby_segments")
                ?: entry.optJSONArray("rubySegments")
                ?: continue
            val segments = segmentsJson.mapObjects { segment ->
                FuriganaSegment(
                    text = segment.optString("text"),
                    reading = segment.optString("reading"),
                )
            }.filter { it.text.isNotEmpty() }
            if (segments.isNotEmpty()) {
                put(targetId, FuriganaResult(segments))
            }
        }
    }
}

internal fun parseAiHistoryJson(json: String): AiHistorySnapshot {
    val response = runCatching { JSONObject(json) }.getOrElse { return AiHistorySnapshot() }

    fun entriesOf(
        array: JSONArray?,
        group: AiHistoryGroup,
        timeKeys: List<String>,
    ): List<AiHistoryEntry> {
        return (array ?: JSONArray()).mapObjects { item ->
            AiHistoryEntry(
                id = item.string("id"),
                group = group,
                kind = item.string("kind", item.string("cacheKind", item.string("targetType"))),
                model = item.string("model"),
                workSlug = item.string("workSlug", item.string("work_slug")),
                episode = item.optInt("episode", 0),
                sourceId = item.string("sourceId", item.string("targetId", item.string("characterKey"))),
                title = item.string("title", group.label),
                summary = item.string("summary", item.string("promptText")),
                timestamp = timeKeys.firstNotNullOfOrNull { key ->
                    item.string(key).takeIf { it.isNotBlank() }
                }.orEmpty(),
            )
        }.filter { it.id.isNotBlank() }
    }

    return AiHistorySnapshot(
        generatedAt = response.string("generatedAt"),
        entries = entriesOf(response.optJSONArray("ai"), AiHistoryGroup.Ai, listOf("updatedAt", "createdAt")) +
            entriesOf(response.optJSONArray("corrections"), AiHistoryGroup.Correction, listOf("createdAt", "updatedAt")) +
            entriesOf(response.optJSONArray("profiles"), AiHistoryGroup.Profile, listOf("updatedAt", "createdAt")),
    )
}

internal fun parseAiHistoryDetailJson(json: String): AiHistoryDetail? {
    val trimmed = json.trim()
    if (trimmed.isBlank() || trimmed == "null") return null
    val response = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
    if (response.has("error")) return null
    val resultJson = response.optJSONObject("result")
    val parsedResult = resultJson
        ?.let { parseAiExplainResult(it) }
        ?.takeIf { it.text.isNotBlank() || it.sections.isNotEmpty() }
    return AiHistoryDetail(
        id = response.string("id"),
        title = response.string("title", "AI 记录"),
        summary = response.string("summary"),
        model = response.string("model"),
        cacheKind = response.string("cacheKind", response.string("cache_kind")),
        workSlug = response.string("workSlug", response.string("work_slug")),
        episode = response.optInt("episode", 0),
        sourceId = response.string("sourceId", response.string("source_id")),
        promptText = response.string("promptText", response.string("prompt_text")),
        createdAt = response.string("createdAt", response.string("updatedAt")),
        result = parsedResult,
    )
}

/**
 * Only catalog/content reads may be served from [EpisodeContentCache]; auth,
 * progress, review, AI and RAG endpoints always go to the network.
 */
internal fun isCacheableContentPath(path: String): Boolean {
    val pathname = path.substringBefore('?')
    return pathname == "/api/works" ||
        pathname.startsWith("/api/works/") ||
        pathname == "/api/linguistic-exercises"
}

/** Catalog slug → Vectorize index slug (`re-zero` → `rezero`, everything else → `k-on`). */
internal fun ragWorkSlug(catalogSlug: String): String {
    return if (catalogSlug == "re-zero" || catalogSlug == "rezero") "rezero" else "k-on"
}

/** Vectorize index slug → catalog slug used by works/episodes/subtitles routes. */
internal fun catalogWorkSlug(ragSlug: String): String {
    return if (ragSlug == "rezero") "re-zero" else ragSlug
}

internal fun parseRagSearchJson(json: String): RagSearchResult {
    val response = runCatching { JSONObject(json) }.getOrElse { return RagSearchResult(query = "") }
    val sources = (response.optJSONArray("sources") ?: JSONArray()).mapObjects { source ->
        val ragWork = source.string("work")
        RagSearchSource(
            id = source.string("id"),
            score = source.optDouble("score", 0.0),
            workSlug = catalogWorkSlug(ragWork),
            ragWork = ragWork,
            episode = source.optInt("episode", 0),
            chunkNo = source.optInt("chunkNo", source.optInt("chunk_no", 0)),
            startTime = source.string("startTime", source.string("start_time")),
            endTime = source.string("endTime", source.string("end_time")),
            text = source.string("text"),
            lines = (source.optJSONArray("lines") ?: JSONArray()).mapObjects { line ->
                SubtitleLine(
                    lineNo = line.optInt("lineNo", line.optInt("line_no", 0)),
                    startTime = line.string("startTime", line.string("start_time")),
                    endTime = line.string("endTime", line.string("end_time")),
                    jaText = line.string("jaText", line.string("ja_text")),
                    zhText = line.string("zhText", line.string("zh_text")),
                )
            }.filter { it.jaText.isNotBlank() },
        )
    }.filter { it.id.isNotBlank() || it.text.isNotBlank() }
    val analysis = response.optJSONObject("analysis")?.let { item ->
        RagAnalysis(
            title = item.string("title"),
            summary = item.string("summary"),
            bullets = (item.optJSONArray("bullets") ?: JSONArray()).mapStrings(),
        )
    }?.takeIf { it.title.isNotBlank() || it.summary.isNotBlank() || it.bullets.isNotEmpty() }
    return RagSearchResult(
        query = response.string("query"),
        sources = sources,
        analysis = analysis,
    )
}

internal fun parseCharacterProfileJson(json: String): CharacterProfile {
    val response = JSONObject(json)
    response.optJSONObject("error")?.let { errorBody ->
        error(errorBody.optString("message").ifBlank { "角色画像请求失败" })
    }
    return CharacterProfile(
        result = parseAiExplainResult(response),
        model = response.string("model"),
        cachedAt = response.string("cachedAt", response.string("cached_at")),
        sourceCount = response.optJSONArray("sources")?.length() ?: 0,
        cacheWarning = response.string("cacheWarning", response.string("cache_warning")),
    )
}

internal fun parseAiModelsJson(json: String): List<AiModelOption> {
    val array = runCatching { JSONArray(json) }.getOrElse { return emptyList() }
    return array.mapObjects { item ->
        AiModelOption(
            id = item.string("id", item.string("model")),
            label = item.string("label", item.string("name", item.string("id"))),
        )
    }.filter { it.id.isNotBlank() }
}

internal fun parseLinguisticExercisesJson(json: String): List<LinguisticExercise> {
    return JSONArray(json).mapObjects(::linguisticExercise)
}

internal fun buildLinguisticProgressPayload(
    exercise: LinguisticExercise,
    selectedOption: String,
): JSONObject {
    return JSONObject()
        .put("label", exercise.prompt)
        .put("prompt", exercise.prompt)
        .put("selected", selectedOption)
        .put("answer", exercise.correctOption)
        .put("sourceId", exercise.sourceId)
        .put("domain", exercise.domain)
        .put("phenomenonKey", exercise.phenomenonKey)
        .put("questionType", exercise.questionType)
}

private fun linguisticExercise(item: JSONObject): LinguisticExercise {
    val optionItems = readLinguisticOptions(
        item.optJSONArray("optionItems")
            ?: item.optJSONArray("option_items")
            ?: item.optJSONArray("options")
            ?: JSONArray(),
    )
    val options = optionItems.map { it.label }.ifEmpty {
        item.optJSONArray("options")?.mapStrings().orEmpty()
    }
    val answer = readLinguisticAnswer(item, options, optionItems)
    return LinguisticExercise(
        id = item.string("id"),
        batchId = item.string("batchId", item.string("batch_id")),
        workSlug = item.string("workSlug", item.string("work_slug")),
        episode = item.optInt("episode", 0),
        sourceId = item.string("sourceId", item.string("source_id")),
        sourceLineNo = item.optInt("sourceLineNo", item.optInt("source_line_no", 0)),
        jaText = item.string("jaText", item.string("ja_text")),
        zhText = item.string("zhText", item.string("zh_text")),
        sceneLines = readSceneLines(item.optJSONArray("sceneLines") ?: item.optJSONArray("scene_lines") ?: JSONArray()),
        targetLineNo = item.optInt("targetLineNo", item.optInt("target_line_no", 0)),
        domain = item.string("domain"),
        phenomenonKey = item.string("phenomenonKey", item.string("phenomenon_key")),
        questionType = item.string("questionType", item.string("question_type")),
        prompt = item.string("prompt"),
        options = options,
        optionItems = optionItems.ifEmpty {
            options.mapIndexed { index, label -> LinguisticExerciseOption(index.toString(), label) }
        },
        answer = answer,
        hint = item.string("hint"),
        basicExplanationZh = item.string("basicExplanationZh", item.string("basic_explanation_zh")),
        deepExplanationZh = item.string("deepExplanationZh", item.string("deep_explanation_zh")),
        animeContextNoteZh = item.string("animeContextNoteZh", item.string("anime_context_note_zh")),
        cautionNoteZh = item.string("cautionNoteZh", item.string("caution_note_zh")),
        difficulty = item.string("difficulty"),
        qualityScore = when {
            item.has("qualityScore") -> item.optDouble("qualityScore", 0.0)
            else -> item.optDouble("quality_score", 0.0)
        },
        status = item.string("status"),
        phenomenonNameZh = item.string("phenomenonNameZh", item.string("phenomenon_name_zh")),
        phenomenonNameJa = item.string("phenomenonNameJa", item.string("phenomenon_name_ja")),
        phenomenonDefinitionZh = item.string("phenomenonDefinitionZh", item.string("phenomenon_definition_zh")),
    )
}

private fun readSceneLines(lines: JSONArray): List<LinguisticSceneLine> {
    return lines.mapObjects { line ->
        LinguisticSceneLine(
            lineNo = line.optInt("lineNo", line.optInt("line_no", 0)),
            speaker = line.string("speaker"),
            jaText = line.string("jaText", line.string("ja_text")),
            zhText = line.string("zhText", line.string("zh_text")),
            isTarget = line.optBoolean("isTarget", line.optBoolean("is_target", false)),
        )
    }.filter { it.jaText.isNotBlank() }
}

private fun readLinguisticAnswer(
    row: JSONObject,
    options: List<String>,
    optionItems: List<LinguisticExerciseOption>,
): LinguisticExerciseAnswer {
    val answerValue = row.opt("answer")
    val answerObject = answerValue as? JSONObject
    val correctIndex = when {
        answerObject?.has("correctIndex") == true -> answerObject.optInt("correctIndex")
        answerObject?.has("correct_index") == true -> answerObject.optInt("correct_index")
        row.has("correctIndex") -> row.optInt("correctIndex")
        row.has("correct_index") -> row.optInt("correct_index")
        else -> null
    }
    val correctKey = answerObject
        ?.let { answer -> answer.string("correctKey", answer.string("correct_key")) }
        .orEmpty()
        .ifBlank { row.string("correctKey", row.string("correct_key")) }
    val keyedAnswer = correctKey.takeIf { it.isNotBlank() }
        ?.let { key -> optionItems.firstOrNull { it.key == key }?.label }
    val indexedAnswer = correctIndex?.takeIf { it >= 0 && it < options.size }?.let { options[it] }
    val answerZh = when {
        answerObject != null -> answerObject.string("answerZh")
            .ifBlank { answerObject.string("answer_zh") }
            .ifBlank { answerObject.string("answer") }
        answerValue is String -> answerValue
        else -> ""
    }.ifBlank {
        keyedAnswer ?: indexedAnswer ?: ""
    }
    return LinguisticExerciseAnswer(
        answerZh = answerZh,
        correctIndex = correctIndex,
        correctKey = correctKey,
        rationaleZh = answerObject
            ?.let { answer -> answer.string("rationaleZh", answer.string("rationale_zh")) }
            .orEmpty(),
    )
}

private fun readLinguisticOptions(options: JSONArray): List<LinguisticExerciseOption> {
    return buildList {
        for (index in 0 until options.length()) {
            val raw = options.opt(index)
            if (raw is String && raw.isNotBlank()) {
                add(LinguisticExerciseOption(key = index.toString(), label = raw))
                continue
            }
            val item = raw as? JSONObject ?: continue
            val label = item.string("label")
                .ifBlank { item.string("text") }
                .ifBlank { item.string("value") }
                .ifBlank { item.string("answer") }
                .ifBlank { item.string("content") }
            if (label.isBlank()) continue
            add(
                LinguisticExerciseOption(
                    key = item.string("key", item.string("id", index.toString())),
                    label = label,
                ),
            )
        }
    }
}

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    val items = mutableListOf<T>()
    for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        items += transform(item)
    }
    return items
}

private fun JSONObject.string(primary: String, fallback: String = ""): String {
    return optString(primary).takeIf { it.isNotBlank() } ?: fallback
}

private fun JSONObject.optNullableInt(primary: String, fallback: String): Int? {
    return when {
        has(primary) && !isNull(primary) -> optInt(primary)
        has(fallback) && !isNull(fallback) -> optInt(fallback)
        else -> null
    }
}

private fun JSONObject.authUser(): AuthUser {
    return AuthUser(
        id = string("id"),
        email = string("email"),
    )
}

private fun JSONArray.mapStrings(): List<String> {
    val items = mutableListOf<String>()
    for (index in 0 until length()) {
        val item = optString(index)
        if (item.isNotBlank()) items += item
    }
    return items
}

private fun JSONObject.toStringMap(): Map<String, String> {
    return keys().asSequence().associateWith { key -> optString(key) }
}

private fun reviewState(value: String): ReviewState {
    return ReviewState.entries.firstOrNull { it.remoteValue == value } ?: ReviewState.Unknown
}
