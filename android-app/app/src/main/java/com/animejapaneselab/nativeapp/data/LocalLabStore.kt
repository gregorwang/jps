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
            aiModel = (preferences.getString(AiModelKey, DefaultAiModel) ?: DefaultAiModel).let { LegacyAiModels[it] ?: it },
            reasoningEffort = preferences.getString(ReasoningEffortKey, DefaultReasoningEffort) ?: DefaultReasoningEffort,
            autoSpeak = preferences.getBoolean(AutoSpeakKey, true),
            feedbackSounds = preferences.getBoolean(FeedbackSoundsKey, true),
            hapticsEnabled = preferences.getBoolean(HapticsEnabledKey, true),
            richAnimationsEnabled = preferences.getBoolean(RichAnimationsEnabledKey, true),
            learningLiveUpdates = preferences.getBoolean(LearningLiveUpdatesKey, true),
            cloudSync = preferences.getBoolean(CloudSyncKey, true),
            showFurigana = preferences.getBoolean(ShowFuriganaKey, true),
            showRomaji = preferences.getBoolean(ShowRomajiKey, false),
            dictByEpisode = preferences.getBoolean(DictByEpisodeKey, false),
            studyReminder = preferences.getBoolean(StudyReminderKey, true),
            studyReminderHour = preferences.getInt(StudyReminderHourKey, 21),
            studyReminderAuto = preferences.getBoolean(StudyReminderAutoKey, true),
            morningLine = preferences.getBoolean(MorningLineKey, true),
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
            putBoolean(DictByEpisodeKey, settings.dictByEpisode)
            putBoolean(StudyReminderKey, settings.studyReminder)
            putInt(StudyReminderHourKey, settings.studyReminderHour)
            putBoolean(StudyReminderAutoKey, settings.studyReminderAuto)
            putBoolean(MorningLineKey, settings.morningLine)
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

    /** 活用道場 lessons marked 已学 (point ids); null until the first write (migration hook). */
    fun readLearnedPoints(): Set<String>? {
        val raw = preferences.getString(LearnedPointsKey, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }.toSet()
        }.getOrNull()
    }

    fun writeLearnedPoints(points: Set<String>) {
        val array = JSONArray()
        points.sorted().forEach { array.put(it) }
        preferences.edit { putString(LearnedPointsKey, array.toString()) }
    }

    /** 自習: lines marked 覚えた, as "pointId::sentenceId". */
    fun readJishuStudied(): Set<String> = runCatching {
        val array = JSONArray(preferences.getString(JishuStudiedKey, "[]").orEmpty())
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }.toSet()
    }.getOrElse { emptySet() }

    fun writeJishuStudied(keys: Set<String>) {
        val array = JSONArray()
        keys.forEach { array.put(it) }
        preferences.edit { putString(JishuStudiedKey, array.toString()) }
    }

    /** 自習 遮る mode (translation hidden until tapped). */
    fun readJishuCover(): Boolean = preferences.getBoolean(JishuCoverKey, false)

    fun writeJishuCover(cover: Boolean) = preferences.edit { putBoolean(JishuCoverKey, cover) }

    /** 学生証 edits: 氏名, 所属, photo version (0 = none). */
    fun readStudentProfile(): com.animejapaneselab.nativeapp.ui.profile.StudentProfileData =
        com.animejapaneselab.nativeapp.ui.profile.StudentProfileData(
            name = preferences.getString(StudentNameKey, null).orEmpty(),
            affiliation = preferences.getString(StudentAffiliationKey, null).orEmpty(),
            photoVersion = preferences.getLong(StudentPhotoVersionKey, 0L),
        )

    fun writeStudentProfile(profile: com.animejapaneselab.nativeapp.ui.profile.StudentProfileData) = preferences.edit {
        putString(StudentNameKey, profile.name)
        putString(StudentAffiliationKey, profile.affiliation)
        putLong(StudentPhotoVersionKey, profile.photoVersion)
    }

    /** 自習 voice: false = 原声, true = TTS (swiped on the voice pill). */
    fun readJishuVoiceTts(): Boolean = preferences.getBoolean(JishuVoiceTtsKey, false)

    fun writeJishuVoiceTts(tts: Boolean) = preferences.edit { putBoolean(JishuVoiceTtsKey, tts) }

    /** The voice picked on any voice pill: VoiceKind name (Original / Emilia / Tts), null = never picked. */
    fun readVoiceChoice(): String? = preferences.getString(VoiceChoiceKey, null)

    fun writeVoiceChoice(name: String) = preferences.edit { putString(VoiceChoiceKey, name) }

    /** Per-day study log: ISO date -> [answers, correct, seconds, studied, finished]. */
    fun readStudyLog(): Map<String, StudyDay> {
        val raw = preferences.getString(StudyLogKey, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { day ->
                val row = json.getJSONArray(day)
                StudyDay(answers = row.optInt(0), correct = row.optInt(1), seconds = row.optInt(2), studied = row.optInt(3), finished = row.optInt(4))
            }
        }.getOrDefault(emptyMap())
    }

    /** Lifetime study seconds (the per-day log only keeps 120 days); -1 until first written. */
    fun readStudyTotalSeconds(): Long = preferences.getLong(StudyTotalSecondsKey, -1L)

    fun writeStudyTotalSeconds(seconds: Long) = preferences.edit { putLong(StudyTotalSecondsKey, seconds) }

    fun writeStudyLog(log: Map<String, StudyDay>, lastAnswerAtMillis: Long) {
        val json = JSONObject()
        log.forEach { (day, d) -> json.put(day, JSONArray().put(d.answers).put(d.correct).put(d.seconds).put(d.studied).put(d.finished)) }
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

    /** 斩: headwords marked as already known (hidden from 辞書 and 単語練習), newline-separated. */
    fun readKnownWords(): Set<String> =
        preferences.getString(KnownWordsKey, null)?.split('\n')?.filter { it.isNotBlank() }?.toSet().orEmpty()

    fun writeKnownWords(words: Set<String>) {
        preferences.edit { putString(KnownWordsKey, words.joinToString("\n")) }
    }

    /** 復習 刷卡流: today's card order and position (JSON, see ReviewFeed). */
    fun readReviewFeedSession(): String? = preferences.getString(ReviewFeedSessionKey, null)

    fun writeReviewFeedSession(raw: String) = preferences.edit { putString(ReviewFeedSessionKey, raw) }

    /** 復習 刷卡流: local mistakes remembered once, as JSON `{itemId: dueEpochDay}`. */
    fun readReviewMistakeDue(): String? = preferences.getString(ReviewMistakeDueKey, null)

    fun writeReviewMistakeDue(raw: String) = preferences.edit { putString(ReviewMistakeDueKey, raw) }

    /** 知識 cards: per card `[seen, lastDay, heartDay, checks]` (JSON, see Knowledge). */
    fun readKnowledgeMarks(): String? = preferences.getString(KnowledgeMarksKey, null)

    fun writeKnowledgeMarks(raw: String) = preferences.edit { putString(KnowledgeMarksKey, raw) }

    /** 単語 (5 per group): per-word Leitner box and the current group (JSON, see Tango). */
    fun readTango(): String? = preferences.getString(TangoKey, null)

    fun writeTango(raw: String) = preferences.edit { putString(TangoKey, raw) }

    /** 第四巻 造語: lessons learned, matrix cells opened, per-item quiz record (JSON, see Zougo). */
    fun readZougo(): String? = preferences.getString(ZougoKey, null)

    fun writeZougo(raw: String) = preferences.edit { putString(ZougoKey, raw) }

    /** 第五巻 起的教科書（助詞・口語・類義）: lessons learned (JSON, see Kyoka). */
    fun readKyoka(): String? = preferences.getString(KyokaKey, null)

    fun writeKyoka(raw: String) = preferences.edit { putString(KyokaKey, raw) }

    /** Home-screen 今日の一句 payload (JSON, see TodayWidgetLine). */
    fun readTodayWidgetLine(): String? = preferences.getString(TodayWidgetLineKey, null)

    fun writeTodayWidgetLine(encoded: String) {
        preferences.edit { putString(TodayWidgetLineKey, encoded) }
    }

    fun readStudyLastAnswerAt(): Long = preferences.getLong(StudyLastAnswerAtKey, 0L)

    /** Minute of day (0..1439) of each day's first study activity, oldest first. */
    fun readStudyStarts(): List<Int> = runCatching {
        val array = JSONArray(preferences.getString(StudyStartsKey, "[]"))
        (0 until array.length()).map { array.optInt(it, -1) }.filter { it in 0..1439 }
    }.getOrDefault(emptyList())

    fun appendStudyStart(minuteOfDay: Int) {
        val array = JSONArray()
        (readStudyStarts() + minuteOfDay).takeLast(30).forEach { array.put(it) }
        preferences.edit { putString(StudyStartsKey, array.toString()) }
    }

    /** 活用 課 title -> earliest Leitner due day (epoch day) among its practised lines. */
    fun readDrillPointDue(): Map<String, Long> = runCatching {
        val json = JSONObject(preferences.getString(DrillPointDueKey, "{}").orEmpty())
        json.keys().asSequence().associateWith { json.optLong(it) }
    }.getOrDefault(emptyMap())

    fun writeDrillPointDue(due: Map<String, Long>) {
        val json = JSONObject()
        due.forEach { (title, day) -> json.put(title, day) }
        preferences.edit { putString(DrillPointDueKey, json.toString()) }
    }

    /** Last reminder template posted, so the next one can pick a different wording. */
    fun readReminderLastTemplate(): String? = preferences.getString(ReminderLastTemplateKey, null)

    /** ISO date on which the habit-slot reminder last posted. */
    fun readReminderHabitPostedOn(): String? = preferences.getString(ReminderHabitPostedOnKey, null)

    /** Reminders posted on [date] (ISO), for the daily cap. */
    fun readReminderPostedCount(date: String): Int {
        val raw = preferences.getString(ReminderPostedCountKey, null).orEmpty()
        return if (raw.substringBefore('|') == date) raw.substringAfter('|').toIntOrNull() ?: 0 else 0
    }

    fun writeReminderPosted(template: String, date: String, habitPostedOn: String?) {
        val count = readReminderPostedCount(date) + 1
        preferences.edit {
            putString(ReminderLastTemplateKey, template)
            putString(ReminderPostedCountKey, "$date|$count")
            if (habitPostedOn != null) putString(ReminderHabitPostedOnKey, habitPostedOn)
        }
    }

    /** 朝の一句 from the main line (MorningPick encoding). */
    fun readMorningPick(): String? = preferences.getString(MorningPickKey, null)

    fun writeMorningPick(encoded: String?) = preferences.edit {
        if (encoded == null) remove(MorningPickKey) else putString(MorningPickKey, encoded)
    }

    /** Set once the app has asked for POST_NOTIFICATIONS on its own. */
    fun readNotificationPermissionAsked(): Boolean = preferences.getBoolean(NotificationAskedKey, false)

    fun writeNotificationPermissionAsked() = preferences.edit { putBoolean(NotificationAskedKey, true) }

    /** Xiaomi settings the app cannot read back; set once the user has been sent to the page. */
    fun readReminderConfirmed(key: String): Boolean = preferences.getBoolean("reminder-confirmed-$key", false)

    fun writeReminderConfirmed(key: String) = preferences.edit { putBoolean("reminder-confirmed-$key", true) }

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
        const val DictByEpisodeKey = "dict-by-episode"
        const val StudyReminderKey = "study-reminder"
        const val StudyReminderHourKey = "study-reminder-hour"
        const val StudyReminderAutoKey = "study-reminder-auto"
        const val MorningLineKey = "morning-line"
        const val MorningPickKey = "morning-pick"
        const val ReminderPostedCountKey = "reminder-posted-count"
        const val StudyStartsKey = "study-starts"
        const val DrillPointDueKey = "drill-point-due"
        const val ReminderLastTemplateKey = "reminder-last-template"
        const val ReminderHabitPostedOnKey = "reminder-habit-posted-on"
        const val NotificationAskedKey = "notification-permission-asked"
        const val StudyLogKey = "study-log"
        const val JishuVoiceTtsKey = "jishu-voice-tts"
        const val VoiceChoiceKey = "voice-choice-2"
        const val StudentNameKey = "student-name"
        const val StudentAffiliationKey = "student-affiliation"
        const val StudentPhotoVersionKey = "student-photo-version"
        const val StudyTotalSecondsKey = "study-total-seconds"
        const val NotebookKey = "notebook"
        const val KnownWordsKey = "known-words"
        const val ReviewFeedSessionKey = "review-feed-session"
        const val ReviewMistakeDueKey = "review-mistake-due"
        const val KnowledgeMarksKey = "knowledge-marks"
        const val TangoKey = "tango-state"
        const val ZougoKey = "zougo-state"
        const val KyokaKey = "kyoka-state"
        const val TodayWidgetLineKey = "today-widget-line"
        const val StudyLastAnswerAtKey = "study-last-answer-at"
        const val DrillProgressKey = "conjugation-drill-progress"
        const val LearnedPointsKey = "conjugation-learned-points"
        const val JishuStudiedKey = "jishu-studied"
        const val JishuCoverKey = "jishu-cover"
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

/**
 * One day of study: judged [answers] ([correct] of them), [seconds] spent, 自習 lines gone through ([studied]),
 * and sessions seen through to their end ([finished]) — only those light a square on 最近 12 週.
 */
data class StudyDay(val answers: Int = 0, val correct: Int = 0, val seconds: Int = 0, val studied: Int = 0, val finished: Int = 0) {
    val activity: Int get() = answers + studied
}
