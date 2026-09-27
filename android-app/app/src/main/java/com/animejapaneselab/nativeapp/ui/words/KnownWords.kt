package com.animejapaneselab.nativeapp.ui.words

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 斩: words the learner already knows (なるほど after one episode). Keyed by headword, so a word
 * cut in one episode or work is gone everywhere. Cut words leave the 辞書 list (they sit in the
 * 已斩 archive, where they can be restored), are never picked for 単語練習, and leave 栞.
 */
object KnownWords {
    private val _words = MutableStateFlow<Set<String>>(emptySet())
    val words: StateFlow<Set<String>> = _words.asStateFlow()
    private var store: LocalLabStore? = null

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        store = LocalLabStore(context.applicationContext).also { _words.value = it.readKnownWords() }
    }

    fun key(item: VocabItem): String = item.surface.trim()

    /**
     * Words any anime viewer understands by ear, offered for 一键斩 alongside the checked cards'
     * `easy` column (archive-content-sources/vocab-cards-v1/ANTIGRAVITY_EASY_PROMPT.md, 53 words).
     */
    private val Obvious = setOf(
        "なるほど", "ありがとう", "すごい", "かわいい", "大丈夫", "ごめん", "ごめんなさい", "うん", "はい", "ちょっと",
        "やばい", "先輩", "おはよう", "バカ", "本当", "よろしく", "すみません", "ダメ", "違う", "好き", "大好き", "嫌い",
        "先生", "友達", "じゃあ", "おいしい", "まさか", "お前", "大変",
        // Left out by the (deliberately cautious) easy pass, but every anime viewer knows them by ear.
        "最高", "最悪", "頑張る", "元気", "本気", "早く", "よし", "殺す", "死ぬ", "怖い", "楽しい", "そう", "いや",
    )

    fun isObvious(item: VocabItem): Boolean = key(item) in Obvious

    fun isKnown(item: VocabItem): Boolean = key(item) in _words.value

    @Synchronized
    fun cut(context: Context, items: Collection<VocabItem>) {
        if (items.isEmpty()) return
        init(context)
        Notebook.init(context)
        items.forEach { Notebook.remove(context, NotebookRules.key(NotebookKind.Vocab, it.id)) }
        write(_words.value + items.map(::key))
    }

    /** 斩 from a 栞 card: the headword is known and its card leaves 栞. */
    @Synchronized
    fun cutHeadword(context: Context, headword: String, notebookKey: String) {
        init(context)
        Notebook.init(context)
        Notebook.remove(context, notebookKey)
        write(_words.value + headword.trim())
    }

    @Synchronized
    fun restore(context: Context, item: VocabItem) {
        init(context)
        write(_words.value - key(item))
    }

    private fun write(next: Set<String>) {
        _words.value = next
        store?.writeKnownWords(next)
    }
}
