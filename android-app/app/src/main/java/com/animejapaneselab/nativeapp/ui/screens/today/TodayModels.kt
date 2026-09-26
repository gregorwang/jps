package com.animejapaneselab.nativeapp.ui.screens.today

import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SubtitleLine
import com.animejapaneselab.nativeapp.ui.design.SlotState
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug
import java.time.DayOfWeek
import java.time.LocalDate

// Pure rules behind the 今日 tab (no Compose) so they stay unit-testable.

// ---------------------------------------------------------------------------
// 今日の一句
// ---------------------------------------------------------------------------

/** One line of the current episode, as shown in the 今日の一句 panel. */
data class TodayLine(
    val ja: String,
    val zh: String = "",
    val speaker: String? = null,
    /** mm:ss (or h:mm:ss) — null when the subtitle timing is unknown. */
    val timeCode: String? = null,
    val lineNo: Int = 0,
) {
    /** 「憂 · 12:31」; falls back to 「第 16 行」 when neither speaker nor time is known. */
    val attribution: String?
        get() {
            val parts = listOfNotNull(speaker?.takeIf { it.isNotBlank() }, timeCode)
            return when {
                parts.isNotEmpty() -> parts.joinToString(" · ")
                lineNo > 0 -> "第 $lineNo 行"
                else -> null
            }
        }
}

object TodayRules {
    /** Lines longer than this do not fit three vertical columns of the hero panel. */
    const val MaxLineLength = 24
    const val MinLineLength = 4

    private val SpeakerPrefix = Regex("""^[（(]([^）)]{1,10})[）)]\s*""")
    private val TimeCode = Regex("""^(\d{1,2}):(\d{1,2}):(\d{1,2})(?:[.,]\d+)?$""")
    private val ShortTimeCode = Regex("""^(\d{1,2}):(\d{1,2})(?:[.,]\d+)?$""")
    private val BreakAfter = setOf('、', '。', '！', '？', '!', '?', '…', '，')

    /** 「（唯）おはよう」→ ("唯", "おはよう"). Subtitle rows sometimes carry the speaker inline. */
    fun splitSpeakerPrefix(raw: String): Pair<String?, String> {
        val text = raw.trim()
        val match = SpeakerPrefix.find(text) ?: return null to text
        return match.groupValues[1].trim().ifEmpty { null } to text.substring(match.range.last + 1).trim()
    }

    /** "00:12:31.200" → "12:31", "01:02:03,000" → "1:02:03", "12:31" → "12:31"; junk → null. */
    fun formatTimeCode(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        TimeCode.matchEntire(value)?.let { m ->
            val h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            val s = m.groupValues[3].toInt()
            return if (h > 0) "$h:${min.pad()}:${s.pad()}" else "${min.pad()}:${s.pad()}"
        }
        ShortTimeCode.matchEntire(value)?.let { m ->
            return "${m.groupValues[1].toInt().pad()}:${m.groupValues[2].toInt().pad()}"
        }
        return null
    }

    private fun Int.pad() = toString().padStart(2, '0')

    /**
     * Every line of the current episode we can show, source-audio sentences first, then the
     * read-air scene lines, then loaded subtitles. Speakers come from read-air scene lines and
     * time codes from subtitles when the line numbers match. Nothing is invented.
     */
    fun candidates(
        workSlug: String,
        episode: Int,
        shadowing: List<ShadowingSentence>,
        subtitles: List<SubtitleLine>,
        readAirExercises: List<LinguisticExercise>,
    ): List<TodayLine> {
        val slug = normalizeWorkSlug(workSlug)
        val sceneLines = readAirExercises
            .filter { normalizeWorkSlug(it.workSlug) == slug && it.episode == episode }
            .flatMap { it.sceneLines }
        val speakerByLine = sceneLines
            .filter { it.lineNo > 0 && it.speaker.isNotBlank() }
            .associate { it.lineNo to it.speaker.trim() }
        val subtitleByLine = subtitles.filter { it.lineNo > 0 }.associateBy { it.lineNo }

        val out = LinkedHashMap<String, TodayLine>()
        fun add(rawJa: String, zh: String, lineNo: Int, speaker: String?) {
            val (inlineSpeaker, ja) = splitSpeakerPrefix(rawJa)
            if (ja.isBlank()) return
            val key = ja.filterNot { it.isWhitespace() }
            if (key in out) return
            out[key] = TodayLine(
                ja = ja,
                zh = zh.trim().ifBlank { subtitleByLine[lineNo]?.zhText?.trim().orEmpty() },
                speaker = speaker?.takeIf { it.isNotBlank() } ?: speakerByLine[lineNo] ?: inlineSpeaker,
                timeCode = formatTimeCode(subtitleByLine[lineNo]?.startTime),
                lineNo = lineNo,
            )
        }
        shadowing.forEach { add(it.ja, it.meaningZh, it.sourceLineNo, null) }
        sceneLines.forEach { add(it.jaText, it.zhText, it.lineNo, it.speaker) }
        subtitles.forEach { add(it.jaText, it.zhText, it.lineNo, null) }
        return out.values.toList()
    }

    /**
     * Deterministic pick: the same work/episode/date always yields the same line, and the line
     * changes with the date. Prefers lines that fit the panel and carry a translation.
     */
    fun pickLine(
        date: LocalDate,
        workSlug: String,
        episode: Int,
        candidates: List<TodayLine>,
    ): TodayLine? {
        if (candidates.isEmpty()) return null
        val fitting = candidates.filter { it.ja.length in MinLineLength..MaxLineLength }
        val pool = fitting.filter { it.zh.isNotBlank() }.ifEmpty { fitting }.ifEmpty {
            listOf(candidates.minBy { it.ja.length })
        }
        val seed = date.toEpochDay() * 1_000_003L +
            normalizeWorkSlug(workSlug).hashCode().toLong() * 31L +
            episode.toLong()
        return pool[Math.floorMod(mix(seed), pool.size.toLong()).toInt()]
    }

    /** SplitMix64 finaliser so consecutive days do not walk the list in order. */
    private fun mix(value: Long): Long {
        var z = value + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /**
     * Breaks [text] into vertical columns of at most [maxPerColumn] glyphs, preferring to break
     * after 、。！？. Returns the columns joined by `\n` for [VerticalText].
     */
    fun verticalLayout(text: String, maxPerColumn: Int): String {
        val limit = maxPerColumn.coerceAtLeast(2)
        val clean = text.trim().replace('\n', ' ').filterNot { it == ' ' || it == '　' }
        val columns = mutableListOf<String>()
        var rest = clean
        while (rest.length > limit) {
            // Latest punctuation break that leaves a column of at least 3 glyphs.
            val window = rest.take(limit)
            val soft = (window.length - 1 downTo 2).firstOrNull { window[it] in BreakAfter }
            var cut = if (soft != null) soft + 1 else limit
            // Never start a column with closing punctuation.
            while (cut < rest.length && rest[cut] in BreakAfter && cut < limit) cut++
            columns += rest.substring(0, cut)
            rest = rest.substring(cut)
        }
        if (rest.isNotEmpty()) columns += rest
        // A trailing 1-glyph column (usually 。) reads better glued to the previous one.
        if (columns.size > 1 && columns.last().length == 1 && columns.last()[0] in BreakAfter) {
            val last = columns.removeAt(columns.lastIndex)
            columns[columns.lastIndex] = columns.last() + last
        }
        return columns.joinToString("\n")
    }

    /** The speaker with the most lines among [lineNos] (ties → first seen); null if none known. */
    fun dominantSpeaker(lines: List<TodayLine>, lineNos: Set<Int>): String? =
        lines.asSequence()
            .filter { it.lineNo in lineNos }
            .mapNotNull { it.speaker?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    /** Key for "first open of the day" — the reveal plays once per day per line. */
    fun revealKey(date: LocalDate, line: TodayLine): String = "${date}|${line.ja}"

    private val Weekdays = mapOf(
        DayOfWeek.MONDAY to "月",
        DayOfWeek.TUESDAY to "火",
        DayOfWeek.WEDNESDAY to "水",
        DayOfWeek.THURSDAY to "木",
        DayOfWeek.FRIDAY to "金",
        DayOfWeek.SATURDAY to "土",
        DayOfWeek.SUNDAY to "日",
    )

    /** 「9.25 木」 */
    fun dateMeta(date: LocalDate): String = "${date.monthValue}.${date.dayOfMonth} ${Weekdays.getValue(date.dayOfWeek)}"
}

/** In-process memory of which 今日の一句 already played its reveal (first open of the day). */
object TodayLineRevealMemory {
    private val seen = mutableSetOf<String>()

    /** True the first time [key] is seen in this process. */
    @Synchronized
    fun markFirstOpen(key: String): Boolean = seen.add(key)
}

// ---------------------------------------------------------------------------
// 本日の時間割
// ---------------------------------------------------------------------------

enum class SlotAction { Lesson, Review, ReadAir, Shadowing }

data class TimetableSlot(
    val action: SlotAction,
    val period: String,
    val title: String,
    val meta: String,
    val state: SlotState,
    /** Line under the primary button when this slot is current. */
    val caption: String,
    /** 0..1 when the slot has a measurable position. */
    val progress: Float? = null,
)

/** Everything the timetable is derived from; unknown numbers are null, never guessed. */
data class TimetableInput(
    /** 第三話 */
    val episodeLabel: String,
    /** 综合 / 词汇 … (LessonMode.label). */
    val lessonModeLabel: String,
    val lessonTotal: Int,
    val lessonDone: Int,
    /** Review cards due now (overdue + today + unscheduled mistakes). */
    val reviewDue: Int,
    /** Read-air questions of this episode, or null while not loaded. */
    val readAirTotal: Int?,
    val readAirAnswered: Int,
    val shadowingCount: Int,
    /** Character whose lines dominate the shadowing set (憂の台词). */
    val shadowingSpeaker: String? = null,
)

object TimetableRules {
    private val Periods = listOf("一限", "二限", "三限")
    const val AfterSchool = "放課後"

    fun build(input: TimetableInput): List<TimetableSlot> {
        data class Draft(
            val action: SlotAction,
            val title: String,
            val meta: String,
            val done: Boolean,
            val caption: String,
            val progress: Float?,
        )
        val drafts = mutableListOf<Draft>()

        if (input.lessonTotal > 0) {
            val done = input.lessonDone.coerceIn(0, input.lessonTotal)
            val complete = done >= input.lessonTotal
            drafts += Draft(
                action = SlotAction.Lesson,
                title = "${input.lessonModeLabel} · ${input.episodeLabel}",
                meta = if (complete) "済" else "$done/${input.lessonTotal}",
                done = complete,
                caption = "${input.lessonModeLabel} · $done/${input.lessonTotal}",
                progress = done.toFloat() / input.lessonTotal,
            )
        }
        if (input.reviewDue > 0) {
            drafts += Draft(
                action = SlotAction.Review,
                title = "复习 · 快忘的卡片",
                meta = "${input.reviewDue} 枚",
                done = false,
                caption = "到期 ${input.reviewDue} 枚",
                progress = null,
            )
        }
        val readAirTotal = input.readAirTotal
        if (readAirTotal == null || readAirTotal > 0) {
            val answered = input.readAirAnswered.coerceIn(0, readAirTotal ?: Int.MAX_VALUE)
            val complete = readAirTotal != null && answered >= readAirTotal
            val left = readAirTotal?.minus(answered)
            drafts += Draft(
                action = SlotAction.ReadAir,
                title = "读空气 · ${input.episodeLabel}",
                meta = when {
                    complete -> "済"
                    left != null -> "$left 问"
                    else -> ""
                },
                done = complete,
                caption = if (readAirTotal != null) "读空气 · $answered/$readAirTotal" else "读空气 · ${input.episodeLabel}",
                progress = readAirTotal?.let { answered.toFloat() / it },
            )
        }
        if (input.shadowingCount > 0) {
            val who = input.shadowingSpeaker?.takeIf { it.isNotBlank() }
            drafts += Draft(
                action = SlotAction.Shadowing,
                title = if (who != null) "跟读 · ${who}的台词" else "跟读 · 本集台词",
                meta = "${input.shadowingCount} 句",
                done = false,
                caption = "跟读 · ${input.shadowingCount} 句",
                progress = null,
            )
        }

        val currentIndex = drafts.indexOfFirst { !it.done }
        return drafts.mapIndexed { index, d ->
            val period = when {
                d.action == SlotAction.Shadowing && drafts.size > 1 -> AfterSchool
                else -> Periods.getOrElse(index) { AfterSchool }
            }
            TimetableSlot(
                action = d.action,
                period = period,
                title = d.title,
                meta = d.meta,
                state = when {
                    d.done -> SlotState.Done
                    index == currentIndex -> SlotState.Current
                    else -> SlotState.Upcoming
                },
                caption = d.caption,
                progress = d.progress,
            )
        }
    }

    /** 还剩 N 节 */
    fun remaining(slots: List<TimetableSlot>): Int = slots.count { it.state != SlotState.Done }

    fun current(slots: List<TimetableSlot>): TimetableSlot? = slots.firstOrNull { it.state == SlotState.Current }
}
