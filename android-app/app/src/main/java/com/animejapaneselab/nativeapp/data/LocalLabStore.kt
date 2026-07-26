package com.animejapaneselab.nativeapp.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalLabStore(context: Context) {
    private val preferences = context.getSharedPreferences("anime-japanese-lab-native", Context.MODE_PRIVATE)

    fun deviceId(): String {
        val existing = preferences.getString(DeviceIdKey, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "device-${UUID.randomUUID()}"
        preferences.edit { putString(DeviceIdKey, generated) }
        return generated
    }

    fun readSettings(): LabSettings {
        return LabSettings(
            apiBaseUrl = preferences.getString(ApiBaseKey, DefaultApiBaseUrl) ?: DefaultApiBaseUrl,
            ttsWorkerUrl = preferences.getString(TtsBaseKey, DefaultTtsWorkerUrl) ?: DefaultTtsWorkerUrl,
            aiModel = preferences.getString(AiModelKey, DefaultAiModel) ?: DefaultAiModel,
            reasoningEffort = preferences.getString(ReasoningEffortKey, DefaultReasoningEffort) ?: DefaultReasoningEffort,
            autoSpeak = preferences.getBoolean(AutoSpeakKey, true),
            feedbackSounds = preferences.getBoolean(FeedbackSoundsKey, true),
            hapticsEnabled = preferences.getBoolean(HapticsEnabledKey, true),
            richAnimationsEnabled = preferences.getBoolean(RichAnimationsEnabledKey, true),
            learningLiveUpdates = preferences.getBoolean(LearningLiveUpdatesKey, true),
            cloudSync = preferences.getBoolean(CloudSyncKey, true),
            showFurigana = preferences.getBoolean(ShowFuriganaKey, true),
            showRomaji = preferences.getBoolean(ShowRomajiKey, false),
        )
    }

    fun writeSettings(settings: LabSettings) {
        preferences.edit {
            putString(ApiBaseKey, settings.apiBaseUrl)
            putString(TtsBaseKey, settings.ttsWorkerUrl)
            putString(AiModelKey, settings.aiModel)
            putString(ReasoningEffortKey, settings.reasoningEffort)
            putBoolean(AutoSpeakKey, settings.autoSpeak)
            putBoolean(FeedbackSoundsKey, settings.feedbackSounds)
            putBoolean(HapticsEnabledKey, settings.hapticsEnabled)
            putBoolean(RichAnimationsEnabledKey, settings.richAnimationsEnabled)
            putBoolean(LearningLiveUpdatesKey, settings.learningLiveUpdates)
            putBoolean(CloudSyncKey, settings.cloudSync)
            putBoolean(ShowFuriganaKey, settings.showFurigana)
            putBoolean(ShowRomajiKey, settings.showRomaji)
        }
    }

    fun readSessionCookie(): String = preferences.getString(SessionCookieKey, "").orEmpty()

    fun writeSessionCookie(cookie: String) {
        preferences.edit { putString(SessionCookieKey, cookie) }
    }

    fun clearSessionCookie() {
        preferences.edit { remove(SessionCookieKey) }
    }

    fun readMistakes(): List<MistakeRecord> {
        val raw = preferences.getString(MistakesKey, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        MistakeRecord(
                            itemId = item.optString("itemId"),
                            typeLabel = item.optString("typeLabel"),
                            prompt = item.optString("prompt"),
                            selected = item.optString("selected"),
                            expected = item.optString("expected"),
                            explanation = item.optString("explanation"),
                            sourceLabel = item.optString("sourceLabel"),
                            attempts = item.optInt("attempts", 1),
                            lastState = reviewState(item.optString("lastState")),
                            workSlug = item.optString("workSlug"),
                            episode = item.optInt("episode", 0),
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun writeMistakes(mistakes: List<MistakeRecord>) {
        val array = JSONArray()
        mistakes.forEach { item ->
            array.put(
                JSONObject()
                    .put("itemId", item.itemId)
                    .put("typeLabel", item.typeLabel)
                    .put("prompt", item.prompt)
                    .put("selected", item.selected)
                    .put("expected", item.expected)
                    .put("explanation", item.explanation)
                    .put("sourceLabel", item.sourceLabel)
                    .put("attempts", item.attempts)
                    .put("lastState", item.lastState.remoteValue)
                    .put("workSlug", item.workSlug)
                    .put("episode", item.episode)
            )
        }
        preferences.edit { putString(MistakesKey, array.toString()) }
    }

    fun readProgress(): List<ProgressItem> = readProgressList(ProgressKey)

    fun writeProgress(progress: List<ProgressItem>) {
        writeProgressList(ProgressKey, progress)
    }

    fun readPendingProgress(): List<ProgressItem> = readProgressList(PendingProgressKey)

    fun writePendingProgress(progress: List<ProgressItem>) {
        writeProgressList(PendingProgressKey, progress)
    }

    private fun readProgressList(key: String): List<ProgressItem> {
        val raw = preferences.getString(key, "[]").orEmpty()
        return ProgressStorageCodec.decode(raw)
    }

    private fun writeProgressList(key: String, progress: List<ProgressItem>) {
        preferences.edit { putString(key, ProgressStorageCodec.encode(progress)) }
    }

    fun readSelection(defaultSelection: EpisodeSelection): EpisodeSelection {
        return EpisodeSelection(
            workSlug = preferences.getString(WorkSlugKey, defaultSelection.workSlug) ?: defaultSelection.workSlug,
            episode = preferences.getInt(EpisodeKey, defaultSelection.episode),
        )
    }

    fun writeSelection(selection: EpisodeSelection) {
        preferences.edit {
            putString(WorkSlugKey, selection.workSlug)
            putInt(EpisodeKey, selection.episode)
        }
    }

    fun readLastEpisodesByWork(): Map<String, Int> {
        val raw = preferences.getString(LastEpisodesByWorkKey, "{}").orEmpty()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val workSlug = keys.next()
                    val episode = json.optInt(workSlug, 0)
                    if (workSlug.isNotBlank() && episode > 0) {
                        put(workSlug, episode)
                    }
                }
            }
        }.getOrElse { emptyMap() }
    }

    fun writeLastEpisodeForWork(selection: EpisodeSelection) {
        if (selection.workSlug.isBlank() || selection.episode <= 0) return
        val next = JSONObject()
        readLastEpisodesByWork().forEach { (workSlug, episode) ->
            next.put(workSlug, episode)
        }
        next.put(selection.workSlug, selection.episode)
        preferences.edit { putString(LastEpisodesByWorkKey, next.toString()) }
    }

    private fun reviewState(value: String): ReviewState {
        return ReviewState.entries.firstOrNull { it.remoteValue == value } ?: ReviewState.Bad
    }

    private companion object {
        const val DeviceIdKey = "device-id"
        const val ApiBaseKey = "api-base-url"
        const val TtsBaseKey = "tts-base-url"
        const val AiModelKey = "ai-model"
        const val ReasoningEffortKey = "reasoning-effort"
        const val SessionCookieKey = "auth-session-cookie"
        const val AutoSpeakKey = "auto-speak"
        const val FeedbackSoundsKey = "feedback-sounds"
        const val HapticsEnabledKey = "haptics-enabled"
        const val RichAnimationsEnabledKey = "rich-animations-enabled"
        const val LearningLiveUpdatesKey = "learning-live-updates"
        const val CloudSyncKey = "cloud-sync"
        const val ShowFuriganaKey = "show-furigana"
        const val ShowRomajiKey = "show-romaji"
        const val MistakesKey = "mistakes"
        const val ProgressKey = "progress"
        const val PendingProgressKey = "pending-progress"
        const val WorkSlugKey = "work-slug"
        const val EpisodeKey = "episode"
        const val LastEpisodesByWorkKey = "last-episodes-by-work"
    }
}
