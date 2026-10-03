package com.animejapaneselab.nativeapp.ui.katsuyou

import android.content.Context
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KatsuyouState(
    /** 自習: the rebuilt 課 being played (a point id). */
    val lesson: String? = null,
    /** 自習: the まとめ map of this book (group key) is open. */
    val map: String? = null,
)

/**
 * Where the rebuilt 活用 books are in 自習. 已学 lives in ConjugationDrillViewModel (the point id), so
 * this only routes; every judged answer still goes into 学習記録.
 */
object Katsuyou {
    private val _state = MutableStateFlow(KatsuyouState())
    val state: StateFlow<KatsuyouState> = _state.asStateFlow()

    fun start(point: String) { _state.value = KatsuyouState(lesson = point) }

    fun exit() { _state.value = KatsuyouState() }

    fun openMap(groupKey: String) { _state.value = KatsuyouState(map = groupKey) }

    fun answer(context: Context, right: Boolean) = StudyLog.record(context, 1, if (right) 1 else 0)
}
