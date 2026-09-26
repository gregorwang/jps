package com.animejapaneselab.nativeapp.ui.notebook

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Process-wide 栞 notebook: words, patterns and lines saved from 辞書 / 今日の一句, reviewed as a
 * small local Leitner deck from 復習. Newest first. Lives outside LabViewModel on purpose.
 */
object Notebook {
    private const val MaxEntries = 800

    private val _entries = MutableStateFlow<List<NotebookEntry>>(emptyList())
    val entries: StateFlow<List<NotebookEntry>> = _entries.asStateFlow()
    private var store: LocalLabStore? = null

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        store = LocalLabStore(context.applicationContext).also { _entries.value = it.readNotebook() }
    }

    fun contains(key: String): Boolean = _entries.value.any { it.key == key }

    /** Saves [entry] (keeping its review state if it was already there) or removes it. */
    @Synchronized
    fun toggle(context: Context, entry: NotebookEntry) {
        init(context)
        val current = _entries.value
        write(
            if (current.any { it.key == entry.key }) {
                current.filterNot { it.key == entry.key }
            } else {
                (listOf(entry.copy(savedAtMillis = System.currentTimeMillis())) + current).take(MaxEntries)
            },
        )
    }

    @Synchronized
    fun remove(context: Context, key: String) {
        init(context)
        write(_entries.value.filterNot { it.key == key })
    }

    @Synchronized
    fun grade(context: Context, key: String, remembered: Boolean) {
        init(context)
        val today = LocalDate.now().toEpochDay()
        write(_entries.value.map { if (it.key == key) NotebookRules.grade(it, remembered, today) else it })
    }

    private fun write(next: List<NotebookEntry>) {
        _entries.value = next
        store?.writeNotebook(next)
    }
}
