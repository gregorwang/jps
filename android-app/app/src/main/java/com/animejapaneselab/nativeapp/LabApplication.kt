package com.animejapaneselab.nativeapp

import android.app.Application
import com.animejapaneselab.nativeapp.platform.LearningSessionNotifier

/** Keeps process startup lightweight; heavy visual runtimes initialize at their first host. */
class LabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // A process restart cannot restore an in-memory training session, so remove stale UI.
        LearningSessionNotifier(this).endSession()
    }
}
