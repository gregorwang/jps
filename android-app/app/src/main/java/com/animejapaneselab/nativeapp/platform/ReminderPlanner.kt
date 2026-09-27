package com.animejapaneselab.nativeapp.platform

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Daily checks: 朝の一句, before the usual study time, and a later one for due 復習. */
enum class ReminderSlot { Morning, Habit, Review }

/** Where a tap on the notification lands. */
enum class ReminderTarget(val key: String) {
    Today("today"),
    Jishu("jishu"),
    Review("review"),
    ;

    companion object {
        fun of(key: String?): ReminderTarget? = entries.firstOrNull { it.key == key }
    }
}

enum class ReminderChannel { Study, Review }

data class ReminderInput(
    val today: LocalDate,
    val studiedToday: Boolean,
    /** Latest day before today with any study activity; null if there is none. */
    val lastStudyDay: LocalDate?,
    val shioriDue: Int,
    val drillDue: Int,
    /** Title of the learned 活用 課 that has been due the longest. */
    val fadingPoint: String?,
    val lineJa: String?,
    val lineZh: String?,
    val habitPostedToday: Boolean,
    val lastTemplate: String?,
) {
    val due: Int get() = shioriDue + drillDue
}

data class ReminderMessage(
    val template: String,
    val channel: ReminderChannel,
    val title: String,
    val body: String,
    val target: ReminderTarget,
)

/**
 * 放課後チャイム rules, all local (StudyLog + the two Leitner schedules):
 *  - timing follows habit: the median start of the last 14 study days, 15 minutes early;
 *  - at most one Habit and one Review message a day, nothing between 23:00 and 08:00;
 *  - the longer the gap, the rarer the nudge: daily up to a week, every third day in week two,
 *    one last「先不提醒了」on day 15, then silence until the user comes back.
 *  - wording rotates and never repeats the previous template.
 */
object ReminderPlanner {
    const val DueThreshold = 5
    private const val EarliestMinute = 8 * 60
    private const val LatestAutoMinute = 22 * 60 + 30
    private const val LatestMinute = 23 * 60
    private const val LeadMinutes = 15
    private const val ReviewAfterMinutes = 120
    private const val MinReviewGapMinutes = 45
    private const val MorningMinute = 8 * 60 + 30
    private const val MorningClearance = 90
    const val DailyCap = 2
    private const val HabitSamples = 14
    private const val MinHabitSamples = 3

    /** Minute of day for the Habit check. */
    fun habitMinute(starts: List<Int>, auto: Boolean, fallbackHour: Int): Int {
        if (auto && starts.size >= MinHabitSamples) {
            val recent = starts.takeLast(HabitSamples).sorted()
            return (recent[recent.size / 2] - LeadMinutes).coerceIn(EarliestMinute, LatestAutoMinute)
        }
        return (fallbackHour.coerceIn(0, 23) * 60).coerceIn(EarliestMinute, LatestMinute)
    }

    /** Minute of day for the Review check, or null when the evening is too short for a second one. */
    fun reviewMinute(habit: Int): Int? =
        (habit + ReviewAfterMinutes).coerceAtMost(LatestMinute).takeIf { it - habit >= MinReviewGapMinutes }

    /** 朝の一句 at 08:30, unless the habit check is too close to it to be worth a separate ping. */
    fun morningMinute(habit: Int): Int? = MorningMinute.takeIf { habit - it >= MorningClearance }

    fun plan(slot: ReminderSlot, input: ReminderInput): ReminderMessage? = when (slot) {
        ReminderSlot.Morning -> lineBody(input)?.takeIf { !input.studiedToday }?.let { body ->
            ReminderMessage("morning", ReminderChannel.Study, "今日の一句", body, ReminderTarget.Today)
        }
        ReminderSlot.Habit -> if (input.studiedToday) null else nudge(input)
        // A second ping only when there is real 復習 to do, and never twice for the same silence.
        ReminderSlot.Review -> if (input.due >= DueThreshold && (input.studiedToday || !input.habitPostedToday)) {
            review(input)
        } else {
            null
        }
    }

    private fun nudge(input: ReminderInput): ReminderMessage? {
        val gap = input.lastStudyDay?.let { ChronoUnit.DAYS.between(it, input.today).toInt() }
        return when {
            gap == null -> message(input, "first", ReminderChannel.Study, ReminderTarget.Jishu, listOf("第一課"), "自習的第一课，8 句台词。")
            gap <= 1 -> if (input.due >= DueThreshold) dueNudge(input) else lineNudge(input, listOf("放課後チャイム", "今日の一句", "今天还没开课"))
            gap == 2 -> lineNudge(input, listOf("两天没开课了", "放課後チャイム · 两天"))
            gap in 3..7 -> fading(input, gap)
            gap in 8..14 -> if ((gap - 8) % 3 == 0) comeback(input) else null
            gap == 15 -> message(
                input,
                "farewell",
                ReminderChannel.Study,
                ReminderTarget.Jishu,
                listOf("先不提醒了"),
                "チャイム暂停。回来学一次，提醒会自动恢复。",
            )
            else -> null
        }
    }

    private fun dueNudge(input: ReminderInput) = message(
        input,
        "due",
        ReminderChannel.Review,
        ReminderTarget.Review,
        listOf("復習の時間 · ${input.due} 张到期", "${input.due} 张卡快忘了", "今天的復習：${input.due}"),
        dueBody(input),
    )

    private fun lineNudge(input: ReminderInput, titles: List<String>) = message(
        input,
        "line",
        ReminderChannel.Study,
        ReminderTarget.Jishu,
        titles,
        lineBody(input) ?: "今天的一句还在等你。",
    )

    private fun fading(input: ReminderInput, gap: Int): ReminderMessage {
        val titles = listOfNotNull(input.fadingPoint?.let { "「$it」开始忘了" }, "$gap 天没复习了")
        return if (input.due > 0) {
            message(input, "fading", ReminderChannel.Review, ReminderTarget.Review, titles, dueBody(input))
        } else {
            message(input, "fading", ReminderChannel.Study, ReminderTarget.Jishu, listOf("$gap 天没开课了"), lineBody(input) ?: "回来学一句就好。")
        }
    }

    private fun comeback(input: ReminderInput): ReminderMessage = if (input.due > 0) {
        message(input, "comeback", ReminderChannel.Review, ReminderTarget.Review, listOf("回来复习 5 句就好", "栞还在这里"), dueBody(input))
    } else {
        message(input, "comeback", ReminderChannel.Study, ReminderTarget.Jishu, listOf("回来学一句就好"), lineBody(input) ?: "今天的一句还在等你。")
    }

    private fun review(input: ReminderInput) = message(
        input,
        "review",
        ReminderChannel.Review,
        ReminderTarget.Review,
        listOf("復習还剩 ${input.due} 张", "睡前把 ${input.due} 张卡过一遍"),
        dueBody(input),
    )

    private fun dueBody(input: ReminderInput): String = buildString {
        append(
            listOfNotNull(
                input.shioriDue.takeIf { it > 0 }?.let { "栞 $it 枚" },
                input.drillDue.takeIf { it > 0 }?.let { "活用 $it 句" },
            ).joinToString(" · ").ifBlank { "到期 ${input.due}" },
        )
        input.fadingPoint?.let { append("\n最久没复习：").append(it) }
    }

    private fun lineBody(input: ReminderInput): String? {
        val ja = input.lineJa?.takeIf { it.isNotBlank() } ?: return null
        return buildString {
            append("「").append(ja).append("」")
            input.lineZh?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
        }
    }

    /** Picks today's wording for [kind], skipping the template used last time. */
    private fun message(
        input: ReminderInput,
        kind: String,
        channel: ReminderChannel,
        target: ReminderTarget,
        titles: List<String>,
        body: String,
    ): ReminderMessage {
        var index = Math.floorMod(input.today.toEpochDay(), titles.size.toLong()).toInt()
        if ("$kind-$index" == input.lastTemplate && titles.size > 1) index = (index + 1) % titles.size
        return ReminderMessage("$kind-$index", channel, titles[index], body, target)
    }
}
