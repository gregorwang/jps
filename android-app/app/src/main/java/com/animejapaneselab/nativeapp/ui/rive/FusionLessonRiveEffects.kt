package com.animejapaneselab.nativeapp.ui.rive

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.RendererType
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonProgressBar

private const val RIVE_LOG_TAG = "FusionLessonRive"
private const val MAX_RIVE_BIND_ATTEMPTS = 16

enum class JuniorCoachReaction {
    Idle,
    Correct,
    Incorrect,
}

enum class DuoRadioReaction {
    Idle,
    Listening,
    Correct,
    Incorrect,
}

private data class RiveTriggerEvent(
    val id: Long,
    val name: String,
)

private data class RiveBindingSpec(
    val booleanInputs: List<Pair<String, Boolean>> = emptyList(),
    val numberInputs: List<Pair<String, Float>> = emptyList(),
    val machineTrigger: RiveTriggerEvent? = null,
    val viewModelBooleanInputs: List<Pair<String, Boolean>> = emptyList(),
    val viewModelNumberInputs: List<Pair<String, Float>> = emptyList(),
    val viewModelColorInputs: List<Pair<String, Int>> = emptyList(),
    val viewModelStringInputs: List<Pair<String, String>> = emptyList(),
    val viewModelTriggers: List<RiveTriggerEvent> = emptyList(),
) {
    val usesViewModel: Boolean
        get() = viewModelBooleanInputs.isNotEmpty() ||
            viewModelNumberInputs.isNotEmpty() ||
            viewModelColorInputs.isNotEmpty() ||
            viewModelStringInputs.isNotEmpty() ||
            viewModelTriggers.isNotEmpty()
}

private class RiveRuntimeState(
    var latestSpec: RiveBindingSpec,
) {
    var appliedSpec: RiveBindingSpec? = null
    var pendingRunnable: Runnable? = null
    var attachListener: View.OnAttachStateChangeListener? = null
    var attempts: Int = 0
    var lastMachineTriggerId: Long? = null
    val lastViewModelTriggerIds: MutableMap<String, Long> = mutableMapOf()
    var released: Boolean = false
}

@Composable
fun FusionJuniorCoachRive(
    reaction: JuniorCoachReaction,
    eventId: Long,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!motionEnabled) {
        Image(
            painter = painterResource(R.drawable.in_challenge_junior),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
        return
    }
    StateDrivenRiveHost(
        rawRes = R.raw.junior_inlesson_v01_13,
        artboardName = "character",
        stateMachineName = "character_statemachine",
        spec = RiveBindingSpec(
            booleanInputs = listOf(
                "darkmode_bool" to false,
                "rtl_bool" to false,
            ),
            machineTrigger = RiveTriggerEvent(
                id = eventId,
                name = when (reaction) {
                    JuniorCoachReaction.Idle -> "reset_trig"
                    JuniorCoachReaction.Correct -> "correct_trig"
                    JuniorCoachReaction.Incorrect -> "incorrect_trig"
                },
            ),
        ),
        fit = Fit.CONTAIN,
        modifier = modifier,
    )
}

@Composable
fun FusionDuoRadioHostRive(
    reaction: DuoRadioReaction,
    eventId: Long,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!motionEnabled) {
        Image(
            painter = painterResource(R.drawable.duoradio_host_falstaff_exercise),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
        return
    }
    StateDrivenRiveHost(
        rawRes = R.raw.duo_radio_host,
        artboardName = "Stage",
        stateMachineName = "InLesson",
        spec = RiveBindingSpec(
            booleanInputs = listOf(
                "Light_Dark_Bool" to false,
                "Avatar_Talk_Bool" to (reaction == DuoRadioReaction.Listening),
            ),
            numberInputs = listOf(
                "Character_Num" to 7f,
                "Avatar_Num" to 0f,
            ),
            machineTrigger = RiveTriggerEvent(
                id = eventId,
                name = when (reaction) {
                    DuoRadioReaction.Idle -> "Idle_Trig"
                    DuoRadioReaction.Listening -> "Listening_Trig"
                    DuoRadioReaction.Correct -> "Correct_Trig"
                    DuoRadioReaction.Incorrect -> "Incorrect_Trig"
                },
            ),
        ),
        fit = Fit.CONTAIN,
        modifier = modifier,
    )
}

/**
 * A state-driven in-lesson progress rail. The ordinary Compose bar is intentionally kept as a
 * deterministic accessibility/reduced-motion layer while the Rive file supplies combo text,
 * lightning and burst effects around it.
 */
@Composable
fun FusionLessonProgressRive(
    progress: Float,
    combo: Int,
    answerEventId: Long,
    progressColor: Color,
    trackColor: Color,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val progressPercent = progress.coerceIn(0f, 1f) * 100f
    val comboText = if (combo >= 2) "x$combo" else ""

    Box(
        modifier = modifier.height(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (motionEnabled) {
            StateDrivenRiveHost(
                rawRes = R.raw.in_lesson_pb_008,
                artboardName = "progress_bar_artboard",
                stateMachineName = "progress_bar_statemachine",
                spec = RiveBindingSpec(
                    viewModelBooleanInputs = listOf(
                        "right_to_left_db_bool" to false,
                        "dark_mode_db_bool" to false,
                        "start_progress_bar_db_bool" to true,
                    ),
                    viewModelNumberInputs = listOf(
                        "bar_height_db_num" to 16f,
                        "progress_db_num" to progressPercent,
                        "color_scheme_db_num" to 0f,
                        "lightning_color_db_num" to if (combo >= 5) 1f else 0f,
                        "burst_type_db_num" to 1f,
                        "fx_intensity_db_num" to if (combo >= 2) 1f else 0f,
                    ),
                    viewModelStringInputs = listOf("combo_text_db_str" to comboText),
                    viewModelTriggers = if (combo >= 2) {
                        listOf(
                            RiveTriggerEvent(answerEventId, "fx_db_trig"),
                            RiveTriggerEvent(answerEventId, "combo_text_db_trig"),
                        )
                    } else {
                        emptyList()
                    },
                ),
                fit = Fit.COVER,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            JuicyLessonProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                heightDp = 11,
                milestoneVisible = progress >= 1f,
                pulsing = false,
                progressColor = progressColor,
                trackColor = trackColor,
            )
        }
    }
}

@Composable
fun FusionMidLessonStreakRive(
    combo: Int,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (motionEnabled) 0.58f else 0.94f),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 10.dp,
        ) {
            Text(
                text = "x$combo 连击！",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
        }
        if (motionEnabled) {
            key(combo) {
                StateDrivenRiveHost(
                    rawRes = R.raw.streakmidlesson_xinarow_31,
                    artboardName = "midlesson_xinarow",
                    stateMachineName = "midlesson_xinarow_statemachine",
                    spec = RiveBindingSpec(
                        booleanInputs = listOf("type_locators" to false),
                        numberInputs = listOf(
                            "text_lines_num" to if (combo >= 10) 0f else 1f,
                            "animation_num" to if (combo >= 10) 2f else 0f,
                            "color_num" to 1f,
                        ),
                        machineTrigger = RiveTriggerEvent(combo.toLong(), "play_trig"),
                    ),
                    fit = Fit.CONTAIN,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.96f),
                )
            }
        }
    }
}

@Composable
fun FusionCtaLightningRive(
    big: Boolean,
    eventId: Long,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!motionEnabled) return
    StateDrivenRiveHost(
        rawRes = R.raw.cta_lightning_09,
        artboardName = "cta_lightning_artboard",
        stateMachineName = "cta_lightning_statemachine",
        spec = RiveBindingSpec(
            viewModelNumberInputs = listOf(
                "cta_width_db_num" to 320f,
                "cta_height_db_num" to 64f,
            ),
            viewModelColorInputs = listOf(
                "lightning_db_color" to if (big) 0xFFA2FFFE.toInt() else 0xFFFFC800.toInt(),
            ),
            viewModelTriggers = listOf(
                RiveTriggerEvent(
                    id = eventId,
                    name = if (big) "big_lightning_db_trig" else "small_lightning_db_trig",
                ),
            ),
        ),
        fit = Fit.COVER,
        modifier = modifier,
    )
}

@Composable
private fun StateDrivenRiveHost(
    @RawRes rawRes: Int,
    artboardName: String,
    stateMachineName: String,
    spec: RiveBindingSpec,
    fit: Fit,
    modifier: Modifier = Modifier,
) {
    key(rawRes, artboardName, stateMachineName, fit) {
        AndroidView(
            modifier = modifier,
            factory = { viewContext ->
                runCatching {
                    RiveRuntimeInitializer.ensureInitialized(viewContext)
                    RiveAnimationView.Builder(viewContext)
                        .setResource(rawRes)
                        .setArtboardName(artboardName)
                        .setStateMachineName(stateMachineName)
                        .setFit(fit)
                        .setAlignment(RiveAlignment.CENTER)
                        .setRendererType(RendererType.Rive)
                        .setAutoplay(true)
                        .setAutoBind(spec.usesViewModel)
                        .setShouldLoadCDNAssets(false)
                        .build()
                        .also { view ->
                            view.isOpaque = false
                            view.touchPassThrough = true
                            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                            val runtimeState = RiveRuntimeState(latestSpec = spec)
                            view.setTag(R.id.tag_fusion_rive_runtime_state, runtimeState)
                            installAttachListener(view, runtimeState, stateMachineName)
                            scheduleRiveBinding(view, runtimeState, stateMachineName)
                        }
                }.getOrElse { error ->
                    Log.w(RIVE_LOG_TAG, "Unable to build Rive resource $rawRes", error)
                    FrameLayout(viewContext)
                }
            },
            update = { candidate ->
                val view = candidate as? RiveAnimationView ?: return@AndroidView
                val runtimeState = view.getTag(R.id.tag_fusion_rive_runtime_state) as? RiveRuntimeState
                    ?: return@AndroidView
                updateRiveSpec(view, runtimeState, spec, stateMachineName)
            },
            onRelease = { candidate ->
                val view = candidate as? RiveAnimationView ?: return@AndroidView
                releaseRiveBinding(view)
            },
        )
    }
}

private fun installAttachListener(
    view: RiveAnimationView,
    runtimeState: RiveRuntimeState,
    stateMachineName: String,
) {
    val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(attachedView: View) {
            if (!runtimeState.released && runtimeState.appliedSpec != runtimeState.latestSpec) {
                scheduleRiveBinding(view, runtimeState, stateMachineName)
            }
        }

        override fun onViewDetachedFromWindow(detachedView: View) {
            runtimeState.pendingRunnable?.let(view::removeCallbacks)
            runtimeState.pendingRunnable = null
        }
    }
    runtimeState.attachListener = listener
    view.addOnAttachStateChangeListener(listener)
}

private fun updateRiveSpec(
    view: RiveAnimationView,
    runtimeState: RiveRuntimeState,
    spec: RiveBindingSpec,
    stateMachineName: String,
) {
    if (runtimeState.released) return
    val alreadyPendingOrApplied = runtimeState.latestSpec == spec &&
        (runtimeState.pendingRunnable != null || runtimeState.appliedSpec == spec)
    if (alreadyPendingOrApplied) return
    runtimeState.latestSpec = spec
    runtimeState.appliedSpec = null
    scheduleRiveBinding(view, runtimeState, stateMachineName)
}

private fun scheduleRiveBinding(
    view: RiveAnimationView,
    runtimeState: RiveRuntimeState,
    stateMachineName: String,
) {
    runtimeState.pendingRunnable?.let(view::removeCallbacks)
    runtimeState.attempts = 0
    lateinit var runnable: Runnable
    runnable = Runnable {
        if (runtimeState.released) return@Runnable
        if (!view.isAttachedToWindow) {
            runtimeState.pendingRunnable = null
            return@Runnable
        }
        val currentSpec = runtimeState.latestSpec
        val applied = runCatching {
            applyRiveBinding(
                view = view,
                runtimeState = runtimeState,
                spec = currentSpec,
                stateMachineName = stateMachineName,
            )
        }.onFailure { error ->
            Log.w(RIVE_LOG_TAG, "Unable to bind $stateMachineName", error)
        }.getOrDefault(false)
        if (applied) {
            runtimeState.appliedSpec = currentSpec
            runtimeState.pendingRunnable = null
        } else if (runtimeState.attempts < MAX_RIVE_BIND_ATTEMPTS) {
            runtimeState.attempts += 1
            view.postDelayed(runnable, 80L)
        } else {
            runtimeState.pendingRunnable = null
            Log.w(RIVE_LOG_TAG, "Timed out binding $stateMachineName")
        }
    }
    runtimeState.pendingRunnable = runnable
    if (view.isAttachedToWindow) {
        view.post(runnable)
    } else {
        view.postDelayed(runnable, 80L)
    }
}

private fun applyRiveBinding(
    view: RiveAnimationView,
    runtimeState: RiveRuntimeState,
    spec: RiveBindingSpec,
    stateMachineName: String,
): Boolean {
    val stateMachine = view.stateMachines.firstOrNull { it.name == stateMachineName } ?: return false
    val inputNames = stateMachine.inputNames.toSet()

    spec.booleanInputs.forEach { (name, value) ->
        if (name in inputNames) view.setBooleanState(stateMachineName, name, value)
    }
    spec.numberInputs.forEach { (name, value) ->
        if (name in inputNames) view.setNumberState(stateMachineName, name, value)
    }
    spec.machineTrigger?.let { event ->
        if (runtimeState.lastMachineTriggerId != event.id && event.name in inputNames) {
            view.fireState(stateMachineName, event.name)
            runtimeState.lastMachineTriggerId = event.id
        }
    }

    if (!spec.usesViewModel) return true

    val viewModelInstance = stateMachine.viewModelInstance ?: return false
    spec.viewModelBooleanInputs.forEach { (name, value) ->
        viewModelInstance.getBooleanProperty(name).value = value
    }
    spec.viewModelNumberInputs.forEach { (name, value) ->
        viewModelInstance.getNumberProperty(name).value = value
    }
    spec.viewModelColorInputs.forEach { (name, value) ->
        viewModelInstance.getColorProperty(name).value = value
    }
    spec.viewModelStringInputs.forEach { (name, value) ->
        viewModelInstance.getStringProperty(name).value = value
    }
    spec.viewModelTriggers.forEach { event ->
        if (runtimeState.lastViewModelTriggerIds[event.name] != event.id) {
            viewModelInstance.getTriggerProperty(event.name).trigger()
            runtimeState.lastViewModelTriggerIds[event.name] = event.id
        }
    }
    return true
}

private fun releaseRiveBinding(view: RiveAnimationView) {
    val runtimeState = view.getTag(R.id.tag_fusion_rive_runtime_state) as? RiveRuntimeState ?: return
    if (runtimeState.released) return
    runtimeState.released = true
    runtimeState.pendingRunnable?.let(view::removeCallbacks)
    runtimeState.pendingRunnable = null
    runtimeState.attachListener?.let(view::removeOnAttachStateChangeListener)
    runtimeState.attachListener = null
    view.setTag(R.id.tag_fusion_rive_runtime_state, null)
    runCatching { view.stop() }
}
