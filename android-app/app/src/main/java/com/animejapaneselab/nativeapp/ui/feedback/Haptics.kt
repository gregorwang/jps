package com.animejapaneselab.nativeapp.ui.feedback

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import org.json.JSONObject

enum class HapticKind {
    Light,
    Confirm,
    Reject,
}

class Haptics(
    private val view: View,
    private val hlaPlayer: HlaPlayer,
) {
    fun perform(kind: HapticKind?, asset: HapticAsset? = null) {
        if (kind != null && hlaPlayer.playEnvelope(kind)) return
        if (asset != null && hlaPlayer.play(asset)) return
        if (kind == null) return
        val feedback = when (kind) {
            HapticKind.Light -> HapticFeedbackConstants.CLOCK_TICK
            HapticKind.Confirm -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
            HapticKind.Reject -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
        }
        runCatching { view.performHapticFeedback(feedback) }
    }
}

class HlaPlayer(private val context: android.content.Context) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    fun play(asset: HapticAsset): Boolean {
        val activeVibrator = vibrator ?: return false
        if (!activeVibrator.hasVibrator()) return false
        val rawId = selectRawId(asset, activeVibrator) ?: return false
        val hla = parse(rawId) ?: return false
        return runCatching {
            val effect = VibrationEffect.createWaveform(hla.timings, hla.amplitudes, hla.repeat)
            activeVibrator.vibrate(effect)
            true
        }.getOrDefault(false)
    }

    fun playEnvelope(kind: HapticKind): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        val activeVibrator = vibrator ?: return false
        if (!activeVibrator.hasVibrator() || !activeVibrator.areEnvelopeEffectsSupported()) return false
        return runCatching {
            val builder = VibrationEffect.BasicEnvelopeBuilder()
            when (kind) {
                HapticKind.Light -> builder
                    .setInitialSharpness(0.85f)
                    .addControlPoint(0.32f, 0.85f, 20L)
                    .addControlPoint(0f, 0.85f, 20L)
                HapticKind.Confirm -> builder
                    .setInitialSharpness(0.70f)
                    .addControlPoint(0.48f, 0.72f, 20L)
                    .addControlPoint(0.82f, 0.96f, 35L)
                    .addControlPoint(0f, 0.78f, 30L)
                HapticKind.Reject -> builder
                    .setInitialSharpness(0.28f)
                    .addControlPoint(0.52f, 0.28f, 35L)
                    .addControlPoint(0.25f, 0.16f, 35L)
                    .addControlPoint(0f, 0.10f, 30L)
            }
            activeVibrator.vibrate(builder.build())
            true
        }.getOrDefault(false)
    }

    private fun selectRawId(asset: HapticAsset, vibrator: Vibrator): Int? {
        val preferred = if (vibrator.hasAmplitudeControl()) {
            asset.l2RawName ?: asset.l1RawName
        } else {
            asset.l1RawName
        }
        return context.rawResourceId(preferred)
            ?: asset.l2RawName?.let(context::rawResourceId)
            ?: context.rawResourceId(asset.l1RawName)
    }

    private fun parse(rawId: Int): HlaPattern? {
        return runCatching {
            val json = context.resources.openRawResource(rawId).bufferedReader().use { it.readText() }
            HlaPatternParser.parse(json)
        }.getOrNull()
    }
}

object HlaPatternParser {
    fun parse(json: String): HlaPattern? {
        return runCatching {
            val root = JSONObject(json)
            val timingsJson = root.optJSONArray("Timings") ?: return@runCatching null
            val amplitudesJson = root.optJSONArray("Amplitudes") ?: return@runCatching null
            if (timingsJson.length() == 0 || timingsJson.length() != amplitudesJson.length()) {
                return@runCatching null
            }
            val timings = LongArray(timingsJson.length()) { index ->
                timingsJson.optLong(index).coerceAtLeast(0L)
            }
            val amplitudes = IntArray(amplitudesJson.length()) { index ->
                amplitudesJson.optInt(index).coerceIn(0, 255)
            }
            HlaPattern(
                timings = timings,
                amplitudes = amplitudes,
                repeat = root.optInt("Repeat", -1),
            )
        }.getOrNull()
    }
}

data class HlaPattern(
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeat: Int = -1,
)

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) { Haptics(view, HlaPlayer(context.applicationContext)) }
}
