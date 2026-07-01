package com.animejapaneselab.nativeapp.data

const val DefaultApiBaseUrl = "https://anime-japanese-lab.ishallnotwant123.workers.dev"
const val DefaultTtsWorkerUrl = "https://cloudflare-edge-tts.ishallnotwant123.workers.dev"
const val DefaultAiModel = "gemini-3.1-flash-lite"
const val DefaultReasoningEffort = "high"

data class WorkOption(
    val id: String,
    val slug: String,
    val displayName: String,
    val episodeCount: Int,
)

data class EpisodeOption(
    val id: String,
    val workSlug: String,
    val workDisplayName: String,
    val episode: Int,
    val totalCues: Int = 0,
    val usableJaLines: Int = 0,
    val chunkCount: Int = 0,
) {
    val label: String = "EP${episode.toString().padStart(2, '0')}"
}

data class EpisodeSelection(
    val workSlug: String,
    val episode: Int,
)

data class EpisodeFocus(
    val workSlug: String,
    val episodeNumber: Int,
    val workTitle: String,
    val episodeLabel: String,
    val lessonTitle: String,
    val sectionTitle: String,
    val guidebook: String,
    val dailyGoal: Int,
    val xp: Int,
    val streakDays: Int,
    val energy: Int,
)

data class VocabItem(
    val id: String,
    val surface: String,
    val reading: String,
    val romanization: String,
    val meaningZh: String,
    val partOfSpeech: String,
    val level: String,
    val occurrence: String,
    val toneTags: List<String>,
    val realWorldNote: String = "",
)

data class GrammarPoint(
    val id: String,
    val pattern: String,
    val titleZh: String,
    val exampleJa: String,
    val exampleZh: String,
    val explanationZh: String,
    val pragmaticsNote: String = "",
    val realWorldNote: String = "",
    val difficulty: String = "",
    val sourceLineNo: Int = 0,
)

data class ShadowingSentence(
    val id: String,
    val ja: String,
    val reading: String,
    val meaningZh: String,
    val sourceLabel: String,
    val audioKind: AudioKind,
    val sourceLineNo: Int = 0,
    val audioUrl: String = "",
    val storagePath: String = "",
)

enum class AudioKind {
    Source,
    Tts,
    None,
}

enum class AudioReliability(val label: String) {
    Verified("可靠原声"),
    Flagged("原声可能不准"),
}

sealed interface PromptAudio {
    val autoPlay: Boolean
    val label: String

    data class Source(
        val url: String,
        override val autoPlay: Boolean,
        val reliability: AudioReliability,
        override val label: String = if (reliability == AudioReliability.Verified) "播放原声" else "原声可能不准",
        val fallbackTtsText: String = "",
        val fallbackLabel: String = "播放标准语音",
    ) : PromptAudio

    data class Tts(
        val text: String,
        override val autoPlay: Boolean,
        override val label: String = "播放标准语音",
    ) : PromptAudio

    data object None : PromptAudio {
        override val autoPlay: Boolean = false
        override val label: String = ""
    }
}

enum class LessonMode(val label: String, val titleLabel: String) {
    Mixed("综合", "综合训练"),
    Vocab("词汇", "词汇训练"),
    Grammar("语法", "语法训练"),
    Shadowing("跟读", "跟读训练"),
    Review("错题", "回炉复习"),
}

sealed interface LessonTarget {
    val id: String

    data class Vocab(override val id: String) : LessonTarget
    data class Grammar(override val id: String) : LessonTarget
    data class Sentence(override val id: String) : LessonTarget
}

data class ReadAirScene(
    val id: String,
    val title: String,
    val context: String,
    val lines: List<DialogueLine>,
    val subtext: String,
    val evidence: List<String>,
    val learningPoint: String,
)

data class DialogueLine(
    val speaker: String,
    val ja: String,
    val zh: String,
)

data class LinguisticExercise(
    val id: String,
    val batchId: String = "",
    val workSlug: String,
    val episode: Int = 0,
    val sourceId: String = "",
    val sourceLineNo: Int = 0,
    val jaText: String,
    val zhText: String = "",
    val sceneLines: List<LinguisticSceneLine> = emptyList(),
    val targetLineNo: Int = 0,
    val domain: String,
    val phenomenonKey: String,
    val questionType: String,
    val prompt: String,
    val options: List<String>,
    val optionItems: List<LinguisticExerciseOption> = options.mapIndexed { index, label ->
        LinguisticExerciseOption(key = index.toString(), label = label)
    },
    val answer: LinguisticExerciseAnswer,
    val hint: String = "",
    val basicExplanationZh: String = "",
    val deepExplanationZh: String = "",
    val animeContextNoteZh: String = "",
    val cautionNoteZh: String = "",
    val difficulty: String = "",
    val qualityScore: Int = 0,
    val status: String = "",
    val phenomenonNameZh: String = "",
    val phenomenonNameJa: String = "",
    val phenomenonDefinitionZh: String = "",
) {
    val correctOption: String
        get() {
            val keyed = answer.correctKey.takeIf { it.isNotBlank() }
                ?.let { key -> optionItems.firstOrNull { it.key == key }?.label }
            if (!keyed.isNullOrBlank()) return keyed

            val indexed = answer.correctIndex
                ?.takeIf { it >= 0 && it < options.size }
                ?.let { options[it] }
            if (!indexed.isNullOrBlank()) return indexed

            return answer.answerZh
        }

    fun isCorrect(selectedOption: String): Boolean = selectedOption == correctOption
}

data class LinguisticSceneLine(
    val lineNo: Int = 0,
    val speaker: String = "",
    val jaText: String,
    val zhText: String = "",
    val isTarget: Boolean = false,
)

data class LinguisticExerciseOption(
    val key: String,
    val label: String,
)

data class LinguisticExerciseAnswer(
    val answerZh: String,
    val correctIndex: Int? = null,
    val correctKey: String = "",
    val rationaleZh: String = "",
)

data class EpisodeContent(
    val focus: EpisodeFocus,
    val vocab: List<VocabItem>,
    val grammar: List<GrammarPoint>,
    val shadowing: List<ShadowingSentence>,
    val scenes: List<ReadAirScene>,
    val lessonNodes: List<LessonNode>,
)

data class ProgressItem(
    val itemId: String,
    val itemType: String,
    val workSlug: String,
    val episode: Int,
    val state: ReviewState,
    val label: String,
    val lastReviewedAt: String = "",
    val nextReviewOn: String = "",
    val payload: Map<String, String> = emptyMap(),
)

enum class ReviewState(val remoteValue: String, val label: String) {
    Known("known", "已掌握"),
    Fuzzy("fuzzy", "模糊"),
    Unknown("unknown", "不会"),
    Good("good", "答对"),
    Ok("ok", "可复习"),
    Bad("bad", "错题"),
}

data class MistakeRecord(
    val itemId: String,
    val typeLabel: String,
    val prompt: String,
    val selected: String,
    val expected: String,
    val explanation: String,
    val sourceLabel: String,
    val attempts: Int,
    val lastState: ReviewState,
    val workSlug: String,
    val episode: Int,
)

data class LabSettings(
    val apiBaseUrl: String = DefaultApiBaseUrl,
    val ttsWorkerUrl: String = DefaultTtsWorkerUrl,
    val aiModel: String = DefaultAiModel,
    val reasoningEffort: String = DefaultReasoningEffort,
    val autoSpeak: Boolean = true,
    val feedbackSounds: Boolean = true,
    val cloudSync: Boolean = true,
)

data class SyncSnapshot(
    val status: SyncStatus = SyncStatus.Idle,
    val message: String = "尚未同步",
    val lastSyncedAt: String = "",
    val remoteReviewCount: Int = 0,
    val catalogUpdated: Boolean = false,
)

enum class SyncStatus {
    Idle,
    Loading,
    Success,
    Error,
}

data class AiCoachState(
    val question: String = "为什么我会选错？",
    val status: SyncStatus = SyncStatus.Idle,
    val answer: String = "",
    val result: AiExplainResult? = null,
)

data class AiExplainResult(
    val title: String,
    val summary: String,
    val text: String,
    val sections: List<AiExplainSection> = emptyList(),
)

data class AiExplainSection(
    val title: String,
    val body: String,
)

sealed interface LessonNode {
    val id: String
    val title: String
    val prompt: String
    val explanation: String
    val sourceLabel: String
    val typeLabel: String
    val expectedAnswer: String
    val sourceKind: String
        get() = "exercise"
    val sourceId: String
        get() = id
    val audio: PromptAudio
        get() = PromptAudio.None
}

data class StudyCardNode(
    override val id: String,
    override val title: String,
    override val prompt: String,
    override val explanation: String,
    override val sourceLabel: String,
    val japanese: String,
    val reading: String,
    val meaningZh: String,
    val notes: List<String>,
    override val sourceKind: String = "vocab",
    override val sourceId: String = id,
    override val audio: PromptAudio = PromptAudio.Tts(japanese, autoPlay = false, label = "播放读音"),
) : LessonNode {
    override val typeLabel = "学习卡"
    override val expectedAnswer = "studied"
}

data class PairMatchNode(
    override val id: String,
    override val title: String,
    override val prompt: String,
    override val explanation: String,
    override val sourceLabel: String,
    val pairs: List<MatchPair>,
    override val sourceKind: String = "vocab",
    override val sourceId: String = id,
    override val audio: PromptAudio = PromptAudio.None,
) : LessonNode {
    override val typeLabel = "配对"
    override val expectedAnswer = pairs.joinToString(" / ") { "${it.left}=${it.right}" }
}

data class MatchPair(
    val id: String,
    val left: String,
    val right: String,
    val audioText: String = right,
)

data class SingleChoiceNode(
    override val id: String,
    override val title: String,
    override val prompt: String,
    override val explanation: String,
    override val sourceLabel: String,
    val body: String?,
    val choices: List<String>,
    val answer: String,
    override val sourceKind: String = "exercise",
    override val sourceId: String = id,
    override val audio: PromptAudio = PromptAudio.None,
) : LessonNode {
    override val typeLabel = "选择"
    override val expectedAnswer = answer
}

data class ClozeNode(
    override val id: String,
    override val title: String,
    override val prompt: String,
    override val explanation: String,
    override val sourceLabel: String,
    val before: String,
    val after: String,
    val choices: List<ClozeChoice>,
    val answer: String,
    override val sourceKind: String = "grammar",
    override val sourceId: String = id,
    override val audio: PromptAudio = PromptAudio.None,
) : LessonNode {
    override val typeLabel = "填空"
    override val expectedAnswer = answer
}

data class ClozeChoice(
    val value: String,
    val note: String,
)

data class TileOrderNode(
    override val id: String,
    override val title: String,
    override val prompt: String,
    override val explanation: String,
    override val sourceLabel: String,
    val displayText: String,
    val targetTiles: List<String>,
    val bankTiles: List<String>,
    val audioTile: Boolean = false,
    override val sourceKind: String = "sentence",
    override val sourceId: String = id,
    override val audio: PromptAudio = PromptAudio.None,
) : LessonNode {
    override val typeLabel = if (audioTile) "听音" else "拼句"
    override val expectedAnswer = targetTiles.joinToString("")
}

data class ShadowingNode(
    override val id: String,
    override val title: String,
    override val prompt: String,
    override val explanation: String,
    override val sourceLabel: String,
    val sentence: ShadowingSentence,
    val ratings: List<String>,
    override val sourceKind: String = "sentence",
    override val sourceId: String = sentence.id,
    override val audio: PromptAudio = PromptAudio.Tts(sentence.ja, autoPlay = true, label = "播放标准语音"),
) : LessonNode {
    override val typeLabel = "跟读"
    override val expectedAnswer = "像 / 一般 / 不像"
}
