package com.animejapaneselab.nativeapp.data

const val DefaultPronunciationApiBaseUrl =
    "https://ajl-pronunciation-api.ishallnotwant123.workers.dev"
const val FirstEnabledPronunciationSentenceId = "k-on-ep01-sent-00046"

enum class PronunciationAssessmentStatus(val remoteValue: String) {
    Scored("scored"),
    Uncertain("uncertain"),
    ReRecord("re_record"),
}

data class PronunciationEngineResult(
    val primary: String,
    val secondary: String,
    val fallbackUsed: Boolean,
    val reliability: Double,
)

data class PronunciationReferenceResult(
    val timingSource: String,
) {
    val usesEstimatedTiming: Boolean
        get() = timingSource == "estimated"
}

data class PronunciationRecognition(
    val text: String,
    val phoneticKana: String,
    val durationMs: Long,
)

data class PronunciationScore(
    val overall: Int,
    val accuracy: Int,
    val completeness: Int,
    val clarity: Int,
    val fluency: Int,
    val rhythm: Int,
    val band: String,
) {
    val lessonRating: String
        get() = when (band) {
            "excellent", "good" -> "像原声"
            "fair" -> "大致跟上"
            else -> "还要再练"
        }
}

data class PronunciationAudioQuality(
    val status: String,
    val scorable: Boolean,
    val durationMs: Long,
    val qualityScore: Int,
    val reasons: List<String>,
)

data class PronunciationSegment(
    val tokenIndex: Int,
    val surface: String,
    val expected: String,
    val heard: String,
    val status: String,
    val score: Int?,
    val message: String,
    val startMs: Long?,
    val endMs: Long?,
) {
    val needsAttention: Boolean
        get() = status != "correct" && status != "unscored"
}

data class PronunciationEvaluation(
    val attemptId: String,
    val sentenceId: String,
    val assessmentStatus: PronunciationAssessmentStatus,
    val scorerVersion: String,
    val reference: PronunciationReferenceResult,
    val engine: PronunciationEngineResult,
    val recognized: PronunciationRecognition?,
    val score: PronunciationScore?,
    val audioQuality: PronunciationAudioQuality,
    val segments: List<PronunciationSegment>,
    val feedback: List<String>,
)

class PronunciationApiException(
    val httpStatus: Int,
    val code: String,
    message: String,
) : IllegalStateException(message)

internal fun pronunciationSentenceId(sentence: ShadowingSentence): String? =
    sentence.id.trim().takeIf(String::isNotBlank)
