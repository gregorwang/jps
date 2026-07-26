package com.animejapaneselab.nativeapp.ui.rive

import android.content.Context
import app.rive.runtime.kotlin.core.RendererType
import app.rive.runtime.kotlin.core.Rive

internal object RiveRuntimeInitializer {
    @Volatile
    private var initialized = false

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return
        Rive.init(context.applicationContext, RendererType.Rive)
        initialized = true
    }
}
