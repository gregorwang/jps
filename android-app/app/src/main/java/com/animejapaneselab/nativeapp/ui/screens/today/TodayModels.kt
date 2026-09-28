package com.animejapaneselab.nativeapp.ui.screens.today

import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.toNotebookEntry
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SubtitleLine
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
    /** 「憂 · 12:31」; null when neither is known (a bare subtitle row number means nothing to the learner). */
    val attribution: String?
        get() = listOfNotNull(speaker?.takeIf { it.isNotBlank() }, timeCode).joinToString(" · ").ifEmpty { null }
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

    private val Weekdays = mapOf(
        DayOfWeek.MONDAY to "月",
        DayOfWeek.TUESDAY to "火",
        DayOfWeek.WEDNESDAY to "水",
        DayOfWeek.THURSDAY to "木",
        DayOfWeek.FRIDAY to "金",
        DayOfWeek.SATURDAY to "土",
        DayOfWeek.SUNDAY to "日",
    )

    /** The line as a 栞 entry; keyed by its sentence when known so 辞書 shows it as saved too. */
    fun notebookEntry(line: TodayLine, sentence: ShadowingSentence?, workSlug: String, episode: Int): NotebookEntry =
        sentence?.toNotebookEntry(workSlug, episode)?.copy(headline = line.ja, meaning = line.zh.ifBlank { sentence.meaningZh })
            ?: NotebookEntry(
                key = NotebookRules.key(NotebookKind.Line, "${normalizeWorkSlug(workSlug)}-$episode-${line.lineNo.takeIf { it > 0 } ?: line.ja.hashCode()}"),
                kind = NotebookKind.Line,
                headline = line.ja,
                meaning = line.zh,
                workSlug = workSlug,
                episode = episode,
                lineNo = line.lineNo,
            )

    /** 「9.25 木」 */
    fun dateMeta(date: LocalDate): String = "${date.monthValue}.${date.dayOfMonth} ${Weekdays.getValue(date.dayOfWeek)}"
}

// ---------------------------------------------------------------------------
// 本日の時間割 = the main line: 一限 自習 → 二限 練習 → 三限 復習
// ---------------------------------------------------------------------------

/** One anime line shown in a period's card, its target marked. */
data class TodaySample(
    val ja: String,
    val zh: String = "",
    val mark: IntRange? = null,
    /** 原声 clip; blank = TTS only. */
    val audioUrl: String = "",
)

/** 自習 → 練習 state for the 時間割, read from the 自習 / 活用 holders in LabApp. */
data class TodayMainLine(
    val jishuPoint: String? = null,
    /** 「第 3 課」 */
    val jishuLesson: String? = null,
    /** 「ている」 and its gloss 「正在…」 */
    val jishuHeadline: String = "",
    val jishuGloss: String = "",
    val jishuStudied: Int = 0,
    val jishuTotal: Int = 0,
    /** The next line the sitting will show. */
    val jishuSample: TodaySample? = null,
    /** 活用 lines due over the learned 課; null before anything is learned. */
    val practiceDue: Int? = null,
    /** 課 learned so far (practice only asks those). */
    val practiceLessons: Int = 0,
    val practiceSample: TodaySample? = null,
)

enum class PeriodKind { Jishu, Practice, Review }

enum class PeriodState {
    /** Has work left today. */
    Open,
    /** Worked through today. */
    Done,
    /** Nothing to do (nothing due / nothing learned yet); not stamped 済. */
    Idle,
}

data class TodayPeriod(
    val kind: PeriodKind,
    /** 一限 */
    val no: String,
    /** 自習 / 練習 / 復習 */
    val tab: String,
    /** Row title after the tab: 「五段 未然形」 */
    val title: String,
    /** Row meta: 「9 / 39 句」「12 句到期」「无到期」「済」 */
    val meta: String,
    val state: PeriodState,
    /** Card: mono line over the headline. */
    val kicker: String,
    val headline: String,
    val sub: String,
    val sample: TodaySample?,
    /** 0..1, null = no measurable position. */
    val progress: Float?,
    val progressLeft: String,
    val progressRight: String,
    /** The one ink button in the open card. */
    val action: String,
)

/** Everything the three periods are derived from. */
data class PeriodInput(
    val main: TodayMainLine,
    /** 自習 lines gone through today. */
    val studiedToday: Int,
    /** Answers logged today (any drill). */
    val answeredToday: Int,
    /** 復習 cards due now (feed round left, or due count before a round starts). */
    val reviewDue: Int,
    /** Today's 復習 round exists and is worked through. */
    val reviewRoundDone: Boolean,
)

object PeriodRules {
    /** A sitting is 8 lines. */
    const val SittingLines = 8

    fun build(input: PeriodInput): List<TodayPeriod> {
        val main = input.main
        val periods = mutableListOf<TodayPeriod>()

        // 一限 自習: continue the current 課; done once a sitting's worth was studied today.
        main.jishuPoint?.let {
            val total = main.jishuTotal
            val left = (total - main.jishuStudied).coerceAtLeast(0)
            val done = input.studiedToday >= SittingLines || (input.studiedToday > 0 && left == 0)
            val batch = if (left in 1 until SittingLines) left else SittingLines
            periods += TodayPeriod(
                kind = PeriodKind.Jishu,
                no = "一限",
                tab = "自習",
                title = main.jishuHeadline,
                meta = if (done) "済" else if (total > 0) "${main.jishuStudied} / $total 句" else "",
                state = if (done) PeriodState.Done else PeriodState.Open,
                kicker = listOfNotNull("自習", main.jishuLesson).joinToString(" · "),
                headline = main.jishuHeadline,
                sub = main.jishuGloss,
                sample = main.jishuSample,
                progress = if (total > 0) main.jishuStudied.toFloat() / total else null,
                progressLeft = if (total > 0) "${main.jishuStudied} / $total 句" else "",
                progressRight = "今天 $batch 句 · 约 ${minutesFor(batch)} 分钟",
                action = "开始 · $batch 句",
            )
        }

        // 二限 練習: 活用 due over learned 課 only.
        val due = main.practiceDue
        val practiced = due == 0 && input.answeredToday > 0
        periods += TodayPeriod(
            kind = PeriodKind.Practice,
            no = "二限",
            tab = "練習",
            title = "活用",
            meta = when {
                due == null -> "学过才考"
                due > 0 -> "$due 句到期"
                practiced -> "済"
                else -> "无到期"
            },
            state = when {
                due != null && due > 0 -> PeriodState.Open
                practiced -> PeriodState.Done
                else -> PeriodState.Idle
            },
            kicker = "練習 · 只考学过的課",
            headline = "活用 ${due ?: 0} 句",
            sub = if (main.practiceLessons > 0) "学过 ${main.practiceLessons} 課 · 到期的先考" else "",
            sample = main.practiceSample,
            progress = null,
            progressLeft = "${due ?: 0} 句到期",
            progressRight = "约 ${minutesFor(due ?: 0)} 分钟",
            action = "开始 · ${due ?: 0} 句",
        )

        // 三限 復習: the due cards of the 知識 feed.
        val reviewDue = input.reviewDue
        periods += TodayPeriod(
            kind = PeriodKind.Review,
            no = "三限",
            tab = "復習",
            title = "到期卡",
            meta = when {
                reviewDue > 0 -> "$reviewDue 张到期"
                input.reviewRoundDone -> "済"
                else -> "无到期"
            },
            state = when {
                reviewDue > 0 -> PeriodState.Open
                input.reviewRoundDone -> PeriodState.Done
                else -> PeriodState.Idle
            },
            kicker = "復習 · 收藏 · 错题 · 苦手",
            headline = "到期 $reviewDue 张",
            sub = "刷完到期的，知識流还能接着刷",
            sample = null,
            progress = null,
            progressLeft = "$reviewDue 张到期",
            progressRight = "约 ${(reviewDue + 1) / 2} 分钟",
            action = "去刷 · $reviewDue 张",
        )
        return periods
    }

    /** The period to open: the first with work left; null when none has. */
    fun current(periods: List<TodayPeriod>): Int? = periods.indexOfFirst { it.state == PeriodState.Open }.takeIf { it >= 0 }

    fun doneCount(periods: List<TodayPeriod>): Int = periods.count { it.state == PeriodState.Done }

    /** Every period is 済 or has nothing to do, and at least one was worked through. */
    fun allDone(periods: List<TodayPeriod>): Boolean =
        periods.none { it.state == PeriodState.Open } && periods.any { it.state == PeriodState.Done }

    /** Rough minutes for [lines] (about 45s a line). */
    fun minutesFor(lines: Int): Int = ((lines * 45) + 59) / 60
}
