package com.animejapaneselab.nativeapp

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.lang.reflect.Method

class SplashActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val startupStartedAt = SystemClock.uptimeMillis()
    private var composeScheduled = false
    private var minSplashElapsed = false
    private var windowFocusedOnce = false
    private var composeInstall: Method? = null
    @Volatile
    private var composeLoading = false
    private val allowComposeInstall = Runnable {
        minSplashElapsed = true
        maybeInstallCompose()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupShell = startupShellView()
        setContentView(startupShell)
        startupShell.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (startupShell.viewTreeObserver.isAlive) {
                        startupShell.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    scheduleComposeStartup()
                    return true
                }
            },
        )
        startupShell.postDelayed({ scheduleComposeStartup() }, SplashDrawFallbackMs)
    }

    override fun onDestroy() {
        handler.removeCallbacks(allowComposeInstall)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            windowFocusedOnce = true
            maybeInstallCompose()
        }
    }

    private fun scheduleComposeStartup() {
        if (composeScheduled) return
        composeScheduled = true
        handler.postDelayed(allowComposeInstall, MinNativeSplashMs)
        startComposePreload()
    }

    private fun startComposePreload() {
        if (composeLoading) return
        composeLoading = true
        Thread(
            {
                val install = runCatching {
                    Class.forName("com.animejapaneselab.nativeapp.ui.LabAppKt")
                    val host = Class.forName("com.animejapaneselab.nativeapp.ComposeHost")
                    host.getMethod("install", ComponentActivity::class.java)
                }.onFailure { error ->
                    Log.w(StartupLogTag, "Compose entry preload failed", error)
                }.getOrNull()
                handler.post {
                    composeInstall = install
                    if (install == null) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    }
                    maybeInstallCompose()
                }
            },
            "compose-entry-preload",
        ).start()
    }

    private fun maybeInstallCompose() {
        val install = composeInstall ?: return
        if (!minSplashElapsed || !windowFocusedOnce || isFinishing || isDestroyed) return
        composeInstall = null
        Log.d(StartupLogTag, "Installing Compose after ${SystemClock.uptimeMillis() - startupStartedAt}ms")
        installCompose(install)
    }

    private fun installCompose(install: Method) {
        runCatching {
            install.invoke(null, this)
        }.onFailure { error ->
            Log.e(StartupLogTag, "Compose install failed", error)
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    private fun startupShellView(): FrameLayout {
        val density = resources.displayMetrics.density
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(251, 252, 248))
            setOnTouchListener { _, _ -> true }
        }
        val mark = TextView(this).apply {
            text = "JL"
            gravity = Gravity.CENTER
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.rgb(88, 204, 2))
            }
        }
        val size = (92f * density).toInt()
        root.addView(
            mark,
            FrameLayout.LayoutParams(size, size, Gravity.CENTER),
        )
        return root
    }

    private companion object {
        const val MinNativeSplashMs = 900L
        const val SplashDrawFallbackMs = 800L
        const val StartupLogTag = "NativeStartup"
    }
}
