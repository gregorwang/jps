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
            studyReminder = preferences.getBoolean(StudyReminderKey, true),
            studyReminderHour = preferences.getInt(StudyReminderHourKey, 21),
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
            putBoolean(StudyReminderKey, settings.studyReminder)
            putInt(StudyReminderHourKey, settings.studyReminderHour)
        }
    }

    /** ISO date (yyyy-MM-dd) on which 今日の一句 last played its reveal. */
    fun readTodayLineRevealedOn(): String? = preferences.getString(TodayLineRevealedOnKey, null)

    fun writeTodayLineRevealedOn(date: String) {
        preferences.edit { putString(TodayLineRevealedOnKey, date) }
    }

    /** `workSlug:episode` → ISO date on which that episode's full アイキャッチ last played. */
    fun readEyecatchPlayedOn(): Map<String, String> {
        val raw = preferences.getString(EyecatchPlayedOnKey, "{}").orEmpty()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val date = json.optString(key)
                    if (key.isNotBlank() && date.isNotBlank()) put(key, date)
                }
            }
        }.getOrElse { emptyMap() }
    }

    fun writeEyecatchPlayedOn(playedOn: Map<String, String>) {
        val json = JSONObject()
        playedOn.forEach { (key, date) -> json.put(key, date) }
        preferences.edit { putString(EyecatchPlayedOnKey, json.toString()) }
    }

    /** 活用道場 progress: item id → [DrillProgress]. */
    fun readDrillProgress(): Map<String, DrillProgress> {
        val raw = preferences.getString(DrillProgressKey, "{}").orEmpty()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val row = json.optJSONArray(key) ?: continue
                    put(key, DrillProgress(row.optInt(0), row.optLong(1), row.optInt(2), row.optInt(3)))
                }
            }
        }.getOrElse { emptyMap() }
    }

    fun writeDrillProgress(progress: Map<String, DrillProgress>) {
        val json = JSONObject()
        progress.forEach { (id, p) -> json.put(id, JSONArray().put(p.box).put(p.dueDay).put(p.seen).put(p.wrong)) }
        preferences.edit { putString(DrillProgressKey, json.toString()) }
    }

    /** Per-day study log: ISO date -> [answers, correct, seconds]. */
    fun readStudyLog(): Map<String, StudyDay> {
        val raw = preferences.getString(StudyLogKey, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { day ->
                val row = json.getJSONArray(day)
                StudyDay(answers = row.optInt(0), correct = row.optInt(1), seconds = row.optInt(2))
            }
        }.getOrDefault(emptyMap())
    }

    fun writeStudyLog(log: Map<String, StudyDay>, lastAnswerAtMillis: Long) {
        val json = JSONObject()
        log.forEach { (day, d) -> json.put(day, JSONArray().put(d.answers).put(d.correct).put(d.seconds)) }
        preferences.edit {
            putString(StudyLogKey, json.toString())
            putLong(StudyLastAnswerAtKey, lastAnswerAtMillis)
        }
    }

    /** 栞 notebook, encoded by [NotebookRules]. */
    fun readNotebook(): List<NotebookEntry> = NotebookRules.decode(preferences.getString(NotebookKey, null))

    fun writeNotebook(entries: List<NotebookEntry>) {
        preferences.edit { putString(NotebookKey, NotebookRules.encode(entries)) }
    }

    /** Home-screen 今日の一句 payload (JSON, see TodayWidgetLine). */
    fun readTodayWidgetLine(): String? = preferences.getString(TodayWidgetLineKey, null)

    fun writeTodayWidgetLine(encoded: String) {
        preferences.edit { putString(TodayWidgetLineKey, encoded) }
    }

    fun readStudyLastAnswerAt(): Long = preferences.getLong(StudyLastAnswerAtKey, 0L)

    fun readSessionCookie(): String = preferences.getString(SessionCookieKey, "").orEmpty()

    fun writeSessionCookie(cookie: String) {
        preferences.edit { putString(SessionCookieKey, cookie) }
    }

    fun clearSessionCookie() {
        preferences.edit { remove(SessionCookieKey).remove(AuthUserIdKey).remove(AuthUserEmailKey) }
    }

    /** Last user the server confirmed for the stored cookie; lets launch skip the login gate. */
    fun readCachedUser(): AuthUser? {
        if (readSessionCookie().isBlank()) return null
        val id = preferences.getString(AuthUserIdKey, null)?.takeIf { it.isNotBlank() } ?: return null
        return AuthUser(id = id, email = preferences.getString(AuthUserEmailKey, "").orEmpty())
    }

    fun writeCachedUser(user: AuthUser?) {
        preferences.edit {
            if (user == null) {
                remove(AuthUserIdKey).remove(AuthUserEmailKey)
            } else {
                putString(AuthUserIdKey, user.id).putString(AuthUserEmailKey, user.email)
            }
        }
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
        const val AuthUserIdKey = "auth-user-id"
        const val AuthUserEmailKey = "auth-user-email"
        const val AutoSpeakKey = "auto-speak"
        const val FeedbackSoundsKey = "feedback-sounds"
        const val HapticsEnabledKey = "haptics-enabled"
        const val RichAnimationsEnabledKey = "rich-animations-enabled"
        const val LearningLiveUpdatesKey = "learning-live-updates"
        const val CloudSyncKey = "cloud-sync"
        const val ShowFuriganaKey = "show-furigana"
        const val ShowRomajiKey = "show-romaji"
        const val StudyReminderKey = "study-reminder"
        const val StudyReminderHourKey = "study-reminder-hour"
        const val StudyLogKey = "study-log"
        const val NotebookKey = "notebook"
        const val TodayWidgetLineKey = "today-widget-line"
        const val StudyLastAnswerAtKey = "study-last-answer-at"
        const val DrillProgressKey = "conjugation-drill-progress"
        const val MistakesKey = "mistakes"
        const val ProgressKey = "progress"
        const val PendingProgressKey = "pending-progress"
        const val WorkSlugKey = "work-slug"
        const val EpisodeKey = "episode"
        const val LastEpisodesByWorkKey = "last-episodes-by-work"
        const val TodayLineRevealedOnKey = "today-line-revealed-on"
        const val EyecatchPlayedOnKey = "eyecatch-played-on"
    }
}

data class StudyDay(val answers: Int = 0, val correct: Int = 0, val seconds: Int = 0)
