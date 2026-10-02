package com.animejapaneselab.nativeapp

import android.app.Application
import com.animejapaneselab.nativeapp.data.ContentPack
import com.animejapaneselab.nativeapp.platform.LearningSessionNotifier
import com.animejapaneselab.nativeapp.platform.StudyReminder
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCards
import com.animejapaneselab.nativeapp.ui.words.TangoLines
import com.animejapaneselab.nativeapp.ui.words.VocabCards

/** Keeps process startup lightweight; heavy visual runtimes initialize at their first host. */
class LabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Course / 辞書 / 自習 / 字幕 content ships in the APK; see scripts/build-content-pack.py.
        ContentPack.init(assets)
        // A process restart cannot restore an in-memory training session, so remove stale UI.
        LearningSessionNotifier(this).endSession()
        StudyReminder.sync(this)
        // The 知識 / 単語 assets (~1.2 MB of JSON) are parsed once in the background, so the tab opens at once.
        Thread({
            KnowledgeCards.decks(this)
            VocabCards.load(this)
            TangoLines.load(this)
        }, "asset-preload").apply { priority = Thread.MIN_PRIORITY }.start()
    }
}
