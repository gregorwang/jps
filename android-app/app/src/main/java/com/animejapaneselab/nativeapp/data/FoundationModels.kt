package com.animejapaneselab.nativeapp.data

const val FoundationCurriculumVersion = "foundation-v1"
const val FoundationMaximumPageLimit = 100

enum class FoundationDomain(val wireValue: String) {
    PhonologyWriting("phonology_writing"),
    Morphology("morphology"),
    Syntax("syntax"),
    Semantics("semantics"),
    PragmaticsDiscourse("pragmatics_discourse"),
    Sociolinguistics("sociolinguistics"),
    HistoricalGrammaticalization("historical_grammaticalization"),
}

enum class FoundationStage(
    val wireValue: String,
    val difficulty: Int,
) {
    F1("F1", 1),
    F2("F2", 2),
    F3("F3", 3),
    F4("F4", 4),
}

enum class FoundationQuestionType(val wireValue: String) {
    SingleChoice("single_choice"),
    MorphologyAnalysis("morphology_analysis"),
    SyntaxRelation("syntax_relation"),
    ContrastChoice("contrast_choice"),
    KuukiYomi("kuuki_yomi"),
}

enum class FoundationSourceKind(val wireValue: String) {
    OriginalSentence("original_sentence"),
    MinimalPair("minimal_pair"),
    ConstructedDialogue("constructed_dialogue"),
    Metalinguistic("metalinguistic"),
}

enum class FoundationPublicationStatus(val wireValue: String) {
    Published("published"),
}

data class FoundationLearningObjectives(
    val f1Zh: String,
    val f2Zh: String,
    val f3Zh: String,
    val f4Zh: String,
)

data class FoundationExampleSpec(
    val formZh: String,
    val contrastZh: String,
    val constraintsZh: String,
)

data class FoundationTopic(
    val id: String,
    val curriculumVersion: String,
    val domain: FoundationDomain,
    val moduleId: String,
    val sortOrder: Int,
    val titleJa: String,
    val titleZh: String,
    val shortDefinitionZh: String,
    val beginnerExplanationZh: String,
    val deepExplanationZh: String,
    val cautionNoteZh: String,
    val prerequisiteTopicIds: List<String>,
    val learningObjectives: FoundationLearningObjectives,
    val exampleSpec: FoundationExampleSpec,
    val tags: List<String>,
    val status: FoundationPublicationStatus,
    val qualityScore: Int,
)

data class FoundationQuestionPack(
    val id: String,
    val curriculumVersion: String,
    val batchNo: Int,
    val titleZh: String,
    val descriptionZh: String,
    val topicCount: Int,
    val questionCount: Int,
    val domainQuotas: Map<FoundationDomain, Int>,
    val status: FoundationPublicationStatus,
    val qualityScore: Int,
)

data class FoundationStimulusTurn(
    val speaker: String? = null,
    val jaText: String,
    val zhText: String? = null,
)

data class FoundationStimulusItem(
    val label: String? = null,
    val text: String,
    val noteZh: String? = null,
)

sealed interface FoundationStimulus {
    data class Sentence(
        val jaText: String,
        val zhContext: String? = null,
    ) : FoundationStimulus

    data class Dialogue(
        val turns: List<FoundationStimulusTurn>,
        val zhContext: String? = null,
    ) : FoundationStimulus

    data class Contrast(
        val items: List<FoundationStimulusItem>,
        val zhContext: String? = null,
    ) : FoundationStimulus

    data class Metalinguistic(
        val descriptionZh: String,
        val form: String? = null,
    ) : FoundationStimulus
}

data class FoundationQuestionOption(
    val id: String,
    val text: String,
)

data class FoundationQuestion(
    val id: String,
    val packId: String,
    val topicId: String,
    val curriculumVersion: String,
    val stage: FoundationStage,
    val questionType: FoundationQuestionType,
    val sourceKind: FoundationSourceKind,
    val stimulus: FoundationStimulus,
    val promptZh: String,
    val options: List<FoundationQuestionOption>,
    val answerOptionId: String,
    val hintZh: String,
    val explanationZh: String,
    val deepExplanationZh: String,
    val cautionNoteZh: String,
    val wrongExplanations: Map<String, String>,
    val transferExampleJa: String? = null,
    val transferExplanationZh: String? = null,
    val difficulty: Int,
    val tags: List<String>,
    val sortOrder: Int,
    val status: FoundationPublicationStatus,
    val qualityScore: Int,
    val contentVersion: Int = 1,
    val contentHash: String? = null,
) {
    fun isCorrect(optionId: String): Boolean = optionId == answerOptionId
}

data class FoundationPageMetadata(
    val limit: Int,
    val hasMore: Boolean,
    val nextCursor: String?,
)

data class FoundationCursorPage<T>(
    val items: List<T>,
    val page: FoundationPageMetadata,
)

data class FoundationTopicQuery(
    val curriculumVersion: String = FoundationCurriculumVersion,
    val domain: FoundationDomain? = null,
    val moduleId: String? = null,
    val cursor: String? = null,
    val limit: Int = FoundationMaximumPageLimit,
)

data class FoundationPackQuery(
    val curriculumVersion: String = FoundationCurriculumVersion,
    val cursor: String? = null,
    val limit: Int = FoundationMaximumPageLimit,
)

data class FoundationQuestionQuery(
    val curriculumVersion: String = FoundationCurriculumVersion,
    val packId: String? = null,
    val topicId: String? = null,
    val stage: FoundationStage? = null,
    val questionType: FoundationQuestionType? = null,
    val difficulty: Int? = null,
    val cursor: String? = null,
    val limit: Int = FoundationMaximumPageLimit,
)
