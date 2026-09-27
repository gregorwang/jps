package com.animejapaneselab.nativeapp.ui.study

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.StudyDay
import com.animejapaneselab.nativeapp.platform.StudyReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime

/**
 * Process-wide study log behind 今日「最近 12 周」. Every judged answer (lesson, 读空气, 基础题库,
 * 活用道場) calls [record], every 自習 card [recordStudy]; study time is the gap since the previous answer, capped so a phone
 * left on the table doesn't count as studying. Reaching a session's end (つづく, 栞 review done) calls [finishSession]:
 * only finished sessions light a square.
 */
object StudyLog {
    private const val MaxGapSeconds = 180
    private const val FirstAnswerSeconds = 20
    private const val KeepDays = 120L

    private val _days = MutableStateFlow<Map<String, StudyDay>>(emptyMap())
    val days: StateFlow<Map<String, StudyDay>> = _days.asStateFlow()
    private val _totalSeconds = MutableStateFlow(0L)
    /** Lifetime study time, beyond the 120 days the per-day log keeps. */
    val totalSeconds: StateFlow<Long> = _totalSeconds.asStateFlow()
    private var store: LocalLabStore? = null

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        store = LocalLabStore(context.applicationContext).also { s ->
            _days.value = s.readStudyLog()
            _totalSeconds.value = s.readStudyTotalSeconds().takeIf { it >= 0 }
                ?: _days.value.values.sumOf { it.seconds.toLong() }.also(s::writeStudyTotalSeconds)
        }
    }

    fun record(context: Context, answers: Int, correct: Int) {
        if (answers <= 0) return
        add(context) { it.copy(answers = it.answers + answers, correct = it.correct + correct.coerceIn(0, answers)) }
    }

    /** 自習: [lines] anime lines gone through (覚えた or もう一回); counts toward the grid and study time. */
    fun recordStudy(context: Context, lines: Int = 1) {
        if (lines <= 0) return
        add(context) { it.copy(studied = it.studied + lines) }
    }

    /** A session reached its end screen; lights today's square (deeper with each one). */
    fun finishSession(context: Context) {
        add(context, timed = false) { it.copy(finished = it.finished + 1) }
    }

    @Synchronized
    private fun add(context: Context, timed: Boolean = true, change: (StudyDay) -> StudyDay) {
        init(context)
        val store = checkNotNull(store)
        val now = if (timed) System.currentTimeMillis() else store.readStudyLastAnswerAt()
        val gap = ((now - store.readStudyLastAnswerAt()) / 1000).toInt()
        val seconds = when {
            !timed -> 0
            gap in 1..MaxGapSeconds -> gap
            else -> FirstAnswerSeconds
        }
        val today = LocalDate.now()
        val key = today.toString()
        val oldest = today.minusDays(KeepDays).toString()
        val current = _days.value[key] ?: StudyDay()
        val next = _days.value.filterKeys { it >= oldest } + (key to change(current).copy(seconds = current.seconds + seconds))
        _days.value = next
        store.writeStudyLog(next, now)
        if (seconds > 0) {
            _totalSeconds.value += seconds
            store.writeStudyTotalSeconds(_totalSeconds.value)
        }
        if (timed && current.activity == 0) {
            // First study of the day: feeds the reminder's habit time and clears today's nudge.
            store.appendStudyStart(LocalTime.now().let { it.hour * 60 + it.minute })
            StudyReminder.onStudyStarted(context)
        }
    }
}
