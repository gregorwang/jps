package com.animejapaneselab.nativeapp.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

private val foundationIdentifierPattern = Regex("^[a-z0-9]+(?:[-_.][a-z0-9]+)*$")
private val foundationIdPattern = Regex("^[a-z0-9]+(?:[-_][a-z0-9]+)*$")
private val foundationCursorPattern = Regex("^[A-Za-z0-9_-]+$")
private val foundationSha256Pattern = Regex("^[a-f0-9]{64}$")

internal fun buildFoundationTopicsPath(query: FoundationTopicQuery = FoundationTopicQuery()): String {
    validateFoundationCommonQuery(query.curriculumVersion, query.cursor, query.limit)
    query.moduleId?.let { validateFoundationId(it, "moduleId") }
    return foundationPath(
        endpoint = "/api/linguistics/foundation/topics",
        parameters = buildList {
            add("curriculumVersion" to query.curriculumVersion)
            query.cursor?.let { add("cursor" to it) }
            add("limit" to query.limit.toString())
            query.domain?.let { add("domain" to it.wireValue) }
            query.moduleId?.let { add("moduleId" to it) }
        },
    )
}

internal fun buildFoundationPacksPath(query: FoundationPackQuery = FoundationPackQuery()): String {
    validateFoundationCommonQuery(query.curriculumVersion, query.cursor, query.limit)
    return foundationPath(
        endpoint = "/api/linguistics/foundation/packs",
        parameters = buildList {
            add("curriculumVersion" to query.curriculumVersion)
            query.cursor?.let { add("cursor" to it) }
            add("limit" to query.limit.toString())
        },
    )
}

internal fun buildFoundationQuestionsPath(query: FoundationQuestionQuery = FoundationQuestionQuery()): String {
    validateFoundationCommonQuery(query.curriculumVersion, query.cursor, query.limit)
    query.packId?.let { validateFoundationId(it, "packId") }
    query.topicId?.let { validateFoundationId(it, "topicId") }
    query.difficulty?.let { require(it in 1..4) { "difficulty must be from 1 to 4" } }
    return foundationPath(
        endpoint = "/api/linguistics/foundation/questions",
        parameters = buildList {
            add("curriculumVersion" to query.curriculumVersion)
            query.cursor?.let { add("cursor" to it) }
            add("limit" to query.limit.toString())
            query.packId?.let { add("packId" to it) }
            query.topicId?.let { add("topicId" to it) }
            query.stage?.let { add("stage" to it.wireValue) }
            query.questionType?.let { add("questionType" to it.wireValue) }
            query.difficulty?.let { add("difficulty" to it.toString()) }
        },
    )
}

internal fun parseFoundationTopicPageJson(json: String): FoundationCursorPage<FoundationTopic> {
    return parseFoundationPage(json, ::parseFoundationTopic)
}

internal fun parseFoundationPackPageJson(json: String): FoundationCursorPage<FoundationQuestionPack> {
    return parseFoundationPage(json, ::parseFoundationPack)
}

internal fun parseFoundationQuestionPageJson(json: String): FoundationCursorPage<FoundationQuestion> {
    return parseFoundationPage(json, ::parseFoundationQuestion)
}

internal fun buildFoundationProgressPayload(
    question: FoundationQuestion,
    selectedOptionId: String,
): JSONObject {
    require(question.options.any { it.id == selectedOptionId }) {
        "selected option must be present in the question"
    }
    return JSONObject()
        .put("label", question.promptZh)
        .put("prompt", question.promptZh)
        .put("selected", selectedOptionId)
        .put("answer", question.answerOptionId)
        .put("track", "foundation")
        .put("packId", question.packId)
        .put("topicId", question.topicId)
        .put("stage", question.stage.wireValue)
        .put("questionType", question.questionType.wireValue)
        .put("contentVersion", question.contentVersion)
        .apply {
            question.contentHash?.let { put("contentHash", it) }
        }
}

private fun validateFoundationCommonQuery(
    curriculumVersion: String,
    cursor: String?,
    limit: Int,
) {
    require(foundationIdentifierPattern.matches(curriculumVersion)) {
        "curriculumVersion is invalid"
    }
    require(limit in 1..FoundationMaximumPageLimit) {
        "limit must be from 1 to $FoundationMaximumPageLimit"
    }
    require(cursor == null || (cursor.length <= 4096 && foundationCursorPattern.matches(cursor))) {
        "cursor must be null or a base64url token"
    }
}

private fun validateFoundationId(value: String, label: String) {
    require(foundationIdPattern.matches(value)) { "$label is invalid" }
}

private fun foundationPath(
    endpoint: String,
    parameters: List<Pair<String, String>>,
): String {
    val query = parameters.joinToString("&") { (key, value) ->
        "${key.foundationUrlEncoded()}=${value.foundationUrlEncoded()}"
    }
    return "$endpoint?$query"
}

private fun String.foundationUrlEncoded(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}

private fun JSONObject.foundationContentFingerprint(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(toString().toByteArray(StandardCharsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private inline fun <T> parseFoundationPage(
    json: String,
    parseItem: (JSONObject) -> T,
): FoundationCursorPage<T> {
    val root = parseFoundationRoot(json)
    val itemsJson = root.requireArray("items", "page")
    val pageJson = root.requireObject("page", "page")
    val limit = pageJson.requireInt("limit", "page", minimum = 1, maximum = FoundationMaximumPageLimit)
    val hasMore = pageJson.requireBoolean("hasMore", "page")
    val nextCursor = pageJson.optionalString("nextCursor", "page")
    require(itemsJson.length() <= limit) {
        "Invalid foundation page: item count exceeds page limit"
    }
    require(!hasMore || nextCursor != null) {
        "Invalid foundation page: hasMore requires nextCursor"
    }
    require(hasMore || nextCursor == null) {
        "Invalid foundation page: terminal page must not include nextCursor"
    }
    val items = buildList(itemsJson.length()) {
        for (index in 0 until itemsJson.length()) {
            val item = itemsJson.opt(index)
            require(item is JSONObject) {
                "Invalid foundation page: items[$index] must be an object"
            }
            add(parseItem(item))
        }
    }
    return FoundationCursorPage(
        items = items,
        page = FoundationPageMetadata(
            limit = limit,
            hasMore = hasMore,
            nextCursor = nextCursor,
        ),
    )
}

private fun parseFoundationRoot(json: String): JSONObject {
    require(json.isNotBlank()) { "Invalid foundation page: response is empty" }
    return try {
        JSONObject(json)
    } catch (error: JSONException) {
        throw IllegalArgumentException("Invalid foundation page: response is not an object", error)
    }
}

private fun parseFoundationTopic(item: JSONObject): FoundationTopic {
    val id = item.requireString("id", "topic").requireFoundationId("topic.id")
    val objectives = item.requireObject("learningObjectives", id)
    val exampleSpec = item.requireObject("exampleSpec", id)
    return FoundationTopic(
        id = id,
        curriculumVersion = item.requireString("curriculumVersion", id)
            .requireFoundationIdentifier("$id.curriculumVersion"),
        domain = item.requireEnum("domain", id, FoundationDomain.entries, FoundationDomain::wireValue),
        moduleId = item.requireString("moduleId", id).requireFoundationId("$id.moduleId"),
        sortOrder = item.requireInt("sortOrder", id, minimum = 0),
        titleJa = item.requireString("titleJa", id),
        titleZh = item.requireString("titleZh", id),
        shortDefinitionZh = item.requireString("shortDefinitionZh", id),
        beginnerExplanationZh = item.requireString("beginnerExplanationZh", id),
        deepExplanationZh = item.requireString("deepExplanationZh", id),
        cautionNoteZh = item.requireString("cautionNoteZh", id),
        prerequisiteTopicIds = item.requireStringList("prerequisiteTopicIds", id).map {
            it.requireFoundationId("$id.prerequisiteTopicIds")
        },
        learningObjectives = FoundationLearningObjectives(
            f1Zh = objectives.requireString("F1Zh", "$id.learningObjectives"),
            f2Zh = objectives.requireString("F2Zh", "$id.learningObjectives"),
            f3Zh = objectives.requireString("F3Zh", "$id.learningObjectives"),
            f4Zh = objectives.requireString("F4Zh", "$id.learningObjectives"),
        ),
        exampleSpec = FoundationExampleSpec(
            formZh = exampleSpec.requireString("formZh", "$id.exampleSpec"),
            contrastZh = exampleSpec.requireString("contrastZh", "$id.exampleSpec"),
            constraintsZh = exampleSpec.requireString("constraintsZh", "$id.exampleSpec"),
        ),
        tags = item.requireStringList("tags", id),
        status = item.requirePublishedStatus(id),
        qualityScore = item.requireInt("qualityScore", id, minimum = 0, maximum = 100),
    )
}

private fun parseFoundationPack(item: JSONObject): FoundationQuestionPack {
    val id = item.requireString("id", "pack").requireFoundationId("pack.id")
    val topicCount = item.requireInt("topicCount", id, minimum = 1)
    val questionCount = item.requireInt("questionCount", id, minimum = 1)
    require(questionCount == topicCount * FoundationStage.entries.size) {
        "Invalid foundation pack $id: questionCount must equal topicCount multiplied by four"
    }
    val quotasJson = item.requireObject("domainQuotas", id)
    val quotas = buildMap {
        val keys = quotasJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val domain = FoundationDomain.entries.firstOrNull { it.wireValue == key }
                ?: throw IllegalArgumentException("Invalid foundation pack $id: unknown domain quota $key")
            put(
                domain,
                quotasJson.requireInt(key, "$id.domainQuotas", minimum = 0),
            )
        }
    }
    return FoundationQuestionPack(
        id = id,
        curriculumVersion = item.requireString("curriculumVersion", id)
            .requireFoundationIdentifier("$id.curriculumVersion"),
        batchNo = item.requireInt("batchNo", id, minimum = 1),
        titleZh = item.requireString("titleZh", id),
        descriptionZh = item.requireString("descriptionZh", id),
        topicCount = topicCount,
        questionCount = questionCount,
        domainQuotas = quotas,
        status = item.requirePublishedStatus(id),
        qualityScore = item.requireInt("qualityScore", id, minimum = 0, maximum = 100),
    )
}

private fun parseFoundationQuestion(item: JSONObject): FoundationQuestion {
    val id = item.requireString("id", "question").requireFoundationId("question.id")
    val stage = item.requireEnum("stage", id, FoundationStage.entries, FoundationStage::wireValue)
    val difficulty = item.requireInt("difficulty", id, minimum = 1, maximum = 4)
    require(difficulty == stage.difficulty) {
        "Invalid foundation question $id: difficulty does not match stage"
    }
    val optionsJson = item.requireArray("options", id)
    require(optionsJson.length() == 4) {
        "Invalid foundation question $id: options must contain exactly four items"
    }
    val options = buildList(optionsJson.length()) {
        for (index in 0 until optionsJson.length()) {
            val option = optionsJson.opt(index)
            require(option is JSONObject) {
                "Invalid foundation question $id: options[$index] must be an object"
            }
            add(
                FoundationQuestionOption(
                    id = option.requireString("id", "$id.options[$index]"),
                    text = option.requireString("text", "$id.options[$index]"),
                ),
            )
        }
    }
    require(options.map(FoundationQuestionOption::id).distinct().size == options.size) {
        "Invalid foundation question $id: option ids must be unique"
    }
    val answer = item.requireObject("answer", id)
    val answerOptionId = answer.requireString("optionId", "$id.answer")
    require(options.any { it.id == answerOptionId }) {
        "Invalid foundation question $id: answer optionId is not present in options"
    }
    val wrongExplanations = item.requireObject("wrongExplanations", id)
    val expectedWrongIds = options.asSequence()
        .map(FoundationQuestionOption::id)
        .filterNot { it == answerOptionId }
        .toSet()
    val actualWrongIds = wrongExplanations.keys().asSequence().toSet()
    require(actualWrongIds == expectedWrongIds) {
        "Invalid foundation question $id: wrong explanations must cover exactly the incorrect options"
    }
    val mappedWrongExplanations = expectedWrongIds.associateWith { optionId ->
        wrongExplanations.requireString(optionId, "$id.wrongExplanations")
    }
    val transferExampleJa = item.optionalString("transferExampleJa", id)
    val transferExplanationZh = item.optionalString("transferExplanationZh", id)
    require((transferExampleJa == null) == (transferExplanationZh == null)) {
        "Invalid foundation question $id: transfer fields must be provided together"
    }
    val contentVersion = if (item.has("contentVersion")) {
        item.requireInt("contentVersion", id, minimum = 1)
    } else {
        1
    }
    val contentHash = item.optionalString("contentHash", id)?.also { hash ->
        require(foundationSha256Pattern.matches(hash)) {
            "Invalid foundation question $id: contentHash must be a lowercase SHA-256 digest"
        }
    } ?: item.foundationContentFingerprint()
    return FoundationQuestion(
        id = id,
        packId = item.requireString("packId", id).requireFoundationId("$id.packId"),
        topicId = item.requireString("topicId", id).requireFoundationId("$id.topicId"),
        curriculumVersion = item.requireString("curriculumVersion", id)
            .requireFoundationIdentifier("$id.curriculumVersion"),
        stage = stage,
        questionType = item.requireEnum(
            "questionType",
            id,
            FoundationQuestionType.entries,
            FoundationQuestionType::wireValue,
        ),
        sourceKind = item.requireEnum(
            "sourceKind",
            id,
            FoundationSourceKind.entries,
            FoundationSourceKind::wireValue,
        ),
        stimulus = parseFoundationStimulus(item.requireObject("stimulus", id), id),
        promptZh = item.requireString("promptZh", id),
        options = options,
        answerOptionId = answerOptionId,
        hintZh = item.requireString("hintZh", id),
        explanationZh = item.requireString("explanationZh", id),
        deepExplanationZh = item.requireString("deepExplanationZh", id),
        cautionNoteZh = item.requireString("cautionNoteZh", id),
        wrongExplanations = mappedWrongExplanations,
        transferExampleJa = transferExampleJa,
        transferExplanationZh = transferExplanationZh,
        difficulty = difficulty,
        tags = item.requireStringList("tags", id),
        sortOrder = item.requireInt("sortOrder", id, minimum = 0),
        status = item.requirePublishedStatus(id),
        qualityScore = item.requireInt("qualityScore", id, minimum = 0, maximum = 100),
        contentVersion = contentVersion,
        contentHash = contentHash,
    )
}

private fun parseFoundationStimulus(
    stimulus: JSONObject,
    questionId: String,
): FoundationStimulus {
    return when (val kind = stimulus.requireString("kind", "$questionId.stimulus")) {
        "sentence" -> FoundationStimulus.Sentence(
            jaText = stimulus.requireString("jaText", "$questionId.stimulus"),
            zhContext = stimulus.optionalString("zhContext", "$questionId.stimulus"),
        )
        "dialogue" -> {
            val turns = stimulus.requireArray("turns", "$questionId.stimulus")
            require(turns.length() > 0) {
                "Invalid foundation question $questionId: dialogue stimulus requires turns"
            }
            FoundationStimulus.Dialogue(
                turns = buildList(turns.length()) {
                    for (index in 0 until turns.length()) {
                        val turn = turns.opt(index)
                        require(turn is JSONObject) {
                            "Invalid foundation question $questionId: turns[$index] must be an object"
                        }
                        add(
                            FoundationStimulusTurn(
                                speaker = turn.optionalString("speaker", "$questionId.turns[$index]"),
                                jaText = turn.requireString("jaText", "$questionId.turns[$index]"),
                                zhText = turn.optionalString("zhText", "$questionId.turns[$index]"),
                            ),
                        )
                    }
                },
                zhContext = stimulus.optionalString("zhContext", "$questionId.stimulus"),
            )
        }
        "contrast" -> {
            val items = stimulus.requireArray("items", "$questionId.stimulus")
            require(items.length() >= 2) {
                "Invalid foundation question $questionId: contrast stimulus requires at least two items"
            }
            FoundationStimulus.Contrast(
                items = buildList(items.length()) {
                    for (index in 0 until items.length()) {
                        val contrastItem = items.opt(index)
                        require(contrastItem is JSONObject) {
                            "Invalid foundation question $questionId: items[$index] must be an object"
                        }
                        add(
                            FoundationStimulusItem(
                                label = contrastItem.optionalString("label", "$questionId.items[$index]"),
                                text = contrastItem.requireString("text", "$questionId.items[$index]"),
                                noteZh = contrastItem.optionalString("noteZh", "$questionId.items[$index]"),
                            ),
                        )
                    }
                },
                zhContext = stimulus.optionalString("zhContext", "$questionId.stimulus"),
            )
        }
        "metalinguistic" -> FoundationStimulus.Metalinguistic(
            descriptionZh = stimulus.requireString("descriptionZh", "$questionId.stimulus"),
            form = stimulus.optionalString("form", "$questionId.stimulus"),
        )
        else -> throw IllegalArgumentException(
            "Invalid foundation question $questionId: unknown stimulus kind $kind",
        )
    }
}

private fun JSONObject.requirePublishedStatus(label: String): FoundationPublicationStatus {
    val status = requireString("status", label)
    require(status == FoundationPublicationStatus.Published.wireValue) {
        "Invalid foundation item $label: status must be published"
    }
    return FoundationPublicationStatus.Published
}

private fun JSONObject.requireObject(key: String, label: String): JSONObject {
    val value = opt(key)
    require(value is JSONObject) {
        "Invalid foundation item $label: $key must be an object"
    }
    return value
}

private fun JSONObject.requireArray(key: String, label: String): JSONArray {
    val value = opt(key)
    require(value is JSONArray) {
        "Invalid foundation item $label: $key must be an array"
    }
    return value
}

private fun JSONObject.requireString(key: String, label: String): String {
    val value = opt(key)
    require(value is String && value.isNotBlank()) {
        "Invalid foundation item $label: $key must be a non-empty string"
    }
    return value
}

private fun JSONObject.optionalString(key: String, label: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key)
    require(value is String && value.isNotBlank()) {
        "Invalid foundation item $label: $key must be null, omitted, or a non-empty string"
    }
    return value
}

private fun JSONObject.requireBoolean(key: String, label: String): Boolean {
    val value = opt(key)
    require(value is Boolean) {
        "Invalid foundation item $label: $key must be a boolean"
    }
    return value
}

private fun JSONObject.requireInt(
    key: String,
    label: String,
    minimum: Int,
    maximum: Int = Int.MAX_VALUE,
): Int {
    val value = opt(key)
    require(value is Number) {
        "Invalid foundation item $label: $key must be an integer"
    }
    val asDouble = value.toDouble()
    val asLong = value.toLong()
    require(asDouble.isFinite() && asLong.toDouble() == asDouble && asLong in minimum.toLong()..maximum.toLong()) {
        "Invalid foundation item $label: $key is outside its valid integer range"
    }
    return asLong.toInt()
}

private fun JSONObject.requireStringList(key: String, label: String): List<String> {
    val values = requireArray(key, label)
    return buildList(values.length()) {
        for (index in 0 until values.length()) {
            val value = values.opt(index)
            require(value is String && value.isNotBlank()) {
                "Invalid foundation item $label: $key[$index] must be a non-empty string"
            }
            add(value)
        }
    }
}

private fun <T> JSONObject.requireEnum(
    key: String,
    label: String,
    values: List<T>,
    wireValue: (T) -> String,
): T {
    val raw = requireString(key, label)
    return values.firstOrNull { wireValue(it) == raw }
        ?: throw IllegalArgumentException("Invalid foundation item $label: $key is invalid")
}

private fun String.requireFoundationId(label: String): String {
    require(foundationIdPattern.matches(this)) {
        "Invalid foundation item $label: identifier is invalid"
    }
    return this
}

private fun String.requireFoundationIdentifier(label: String): String {
    require(foundationIdentifierPattern.matches(this)) {
        "Invalid foundation item $label: identifier is invalid"
    }
    return this
}
