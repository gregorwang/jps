package com.animejapaneselab.nativeapp.ui.study

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.StudyDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Process-wide study log behind 今日「最近 12 周」. Every judged answer (lesson, 读空气, 基础题库,
 * 活用道場) calls [record]; study time is the gap since the previous answer, capped so a phone
 * left on the table doesn't count as studying.
 */
object StudyLog {
    private const val MaxGapSeconds = 180
    private const val FirstAnswerSeconds = 20
    private const val KeepDays = 120L

    private val _days = MutableStateFlow<Map<String, StudyDay>>(emptyMap())
    val days: StateFlow<Map<String, StudyDay>> = _days.asStateFlow()
    private var store: LocalLabStore? = null

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        store = LocalLabStore(context.applicationContext).also { _days.value = it.readStudyLog() }
    }

    @Synchronized
    fun record(context: Context, answers: Int, correct: Int) {
        if (answers <= 0) return
        init(context)
        val store = checkNotNull(store)
        val now = System.currentTimeMillis()
        val gap = ((now - store.readStudyLastAnswerAt()) / 1000).toInt()
        val seconds = if (gap in 1..MaxGapSeconds) gap else FirstAnswerSeconds
        val today = LocalDate.now()
        val key = today.toString()
        val oldest = today.minusDays(KeepDays).toString()
        val current = _days.value[key] ?: StudyDay()
        val next = (_days.value.filterKeys { it >= oldest } + (key to StudyDay(
            answers = current.answers + answers,
            correct = current.correct + correct.coerceIn(0, answers),
            seconds = current.seconds + seconds,
        )))
        _days.value = next
        store.writeStudyLog(next, now)
    }
}
