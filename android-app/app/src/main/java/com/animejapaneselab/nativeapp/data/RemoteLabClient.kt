package com.animejapaneselab.nativeapp.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class RemoteEpisodePayload(
    val vocab: List<VocabItem>,
    val grammar: List<GrammarPoint>,
    val shadowing: List<ShadowingSentence>,
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
) {
    private val normalizedBase = baseUrl.trim().trimEnd('/')

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
        )
    }

    fun fetchReviewTasks(deviceId: String): List<ProgressItem> {
        val json = get("/api/review/today?deviceId=${deviceId.urlEncoded()}")
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
        val json = get("/api/progress?deviceId=${deviceId.urlEncoded()}")
        return JSONArray(json).mapObjects(::progressItem)
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
        val body = JSONObject()
            .put("deviceId", deviceId)
            .put("itemId", itemId)
            .put("itemType", itemType)
            .put("workSlug", selection.workSlug)
            .put("episode", selection.episode)
            .put("state", state.remoteValue)
            .put("payload", payload ?: JSONObject().put("label", label))
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
        val response = JSONObject(post("/api/ai/explain", body))
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
            ShadowingSentence(
                id = item.string("id", "${selection.workSlug}-sentence-${item.optInt("sourceLineNo", 0)}"),
                ja = item.string("jaText", item.string("ja")),
                reading = item.string("romaji", item.string("reading")),
                meaningZh = item.string("meaningZh", item.string("meaning_zh")),
                sourceLabel = "EP${selection.episode.toString().padStart(2, '0')} 第 ${item.optInt("sourceLineNo", 0)} 行",
                audioKind = audioKind,
                sourceLineNo = item.optInt("sourceLineNo", item.optInt("source_line_no", 0)),
                audioUrl = audioUrl,
                storagePath = storagePath,
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
        )
    }

    private fun get(path: String): String = request("GET", path, null)

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
                outputStream.use { stream ->
                    stream.write(body.toByteArray(StandardCharsets.UTF_8))
                }
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
        connection.disconnect()
        if (status !in 200..299) {
            throw IllegalStateException("HTTP $status ${response.take(180)}")
        }
        return RemoteResponse(body = response, sessionCookie = responseCookie)
    }
}

private data class RemoteResponse(
    val body: String,
    val sessionCookie: String = "",
)

private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

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
        qualityScore = item.optInt("qualityScore", item.optInt("quality_score", 0)),
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
