package com.animejapaneselab.nativeapp.ui.rive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.feedback.VisualAsset
import com.animejapaneselab.nativeapp.ui.feedback.rawResourceId

@Composable
fun RiveAnimationHost(
    controller: RiveMascotController,
    modifier: Modifier = Modifier,
) {
    val visual = controller.visualAsset
    if (visual is VisualAsset.Rive) {
        RiveRuntimeHost(
            asset = visual,
            modifier = modifier.size(96.dp),
            fallback = { MascotFallback(controller = controller, modifier = modifier) },
        )
        return
    }
    if (visual is VisualAsset.Lottie) {
        LottieAnimationHost(
            asset = visual,
            modifier = modifier.size(96.dp),
            fallback = { MascotFallback(controller = controller, modifier = modifier) },
        )
        return
    }
    MascotFallback(controller = controller, modifier = modifier)
}

@Composable
fun DuolingoLikeVisualHost(
    asset: VisualAsset,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
    fallback: @Composable () -> Unit = {},
) {
    if (!motionEnabled) {
        fallback()
        return
    }
    when (asset) {
        is VisualAsset.Rive -> RiveRuntimeHost(
            asset = asset,
            modifier = modifier,
            fallback = fallback,
        )

        is VisualAsset.Lottie -> LottieAnimationHost(
            asset = asset,
            modifier = modifier,
            fallback = fallback,
        )
    }
}

@Composable
private fun RiveRuntimeHost(
    asset: VisualAsset.Rive,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val rawId = context.rawResourceId(asset.rawName)
    if (rawId == null) {
        fallback()
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            runCatching {
                RiveRuntimeInitializer.ensureInitialized(viewContext)
                RiveAnimationView.Builder(viewContext)
                    .setResource(rawId)
                    .setFit(Fit.CONTAIN)
                    .setAutoplay(true)
                    .apply {
                        asset.artboard?.let(::setArtboardName)
                        asset.animationName?.let(::setAnimationName)
                        asset.stateMachine?.let(::setStateMachineName)
                    }
                    .build()
            }.getOrElse {
                android.widget.FrameLayout(viewContext)
            }
        },
        update = { view ->
            if (view is RiveAnimationView) {
                runCatching {
                    if (asset.animationName != null) view.play(asset.animationName) else view.play()
                }
            }
        },
    )
}

@Composable
private fun MascotFallback(
    controller: RiveMascotController,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (controller.lastTrigger == "idle" || reducedMotion) 1f else MotionTokens.Scale.PopOvershoot,
        animationSpec = MotionTokens.popSpring(reducedMotion),
        label = "mascot-placeholder-scale",
    )
    val icon = mascotIcon(controller.lastTrigger)
    Box(
        modifier = modifier
            .size(74.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(36.dp),
        )
    }
}

private fun mascotIcon(trigger: String): ImageVector {
    return when (trigger) {
        "wrong" -> Icons.Rounded.SentimentDissatisfied
        "unlock" -> Icons.Rounded.LockOpen
        "complete", "streak", "combo" -> Icons.Rounded.Stars
        "correct" -> Icons.Rounded.Check
        else -> Icons.Rounded.Mood
    }
}
