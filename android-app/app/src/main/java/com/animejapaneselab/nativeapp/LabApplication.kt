package com.animejapaneselab.nativeapp

import android.app.Application
import android.os.Process
import com.animejapaneselab.nativeapp.data.ContentPack
import com.animejapaneselab.nativeapp.platform.LearningSessionNotifier
import com.animejapaneselab.nativeapp.platform.StudyReminder
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCards
import com.animejapaneselab.nativeapp.ui.voicepack.VoicePack
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
        // Starts reading the furigana cache file now (SharedPreferences loads on its own thread), so the
        // first annotated line on 今日 does not wait for it on the main thread.
        getSharedPreferences("ajl-furigana-cache", MODE_PRIVATE)
        // The voice-pack manifest (44k clips) and the 知識 / 単語 assets (~1.2 MB of JSON) are parsed once
        // in the background. Screens that need them early block on these loaders' locks, so the thread
        // runs at normal background priority: at MIN_PRIORITY it starved and froze the main thread.
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            VoicePack.preload(this)
            KnowledgeCards.decks(this)
            VocabCards.load(this)
            TangoLines.load(this)
        }, "asset-preload").start()
    }
}
