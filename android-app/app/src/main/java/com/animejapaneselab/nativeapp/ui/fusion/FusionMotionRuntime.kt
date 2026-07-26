package com.animejapaneselab.nativeapp.ui.fusion

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

sealed interface FusionMotionSpec {
    data class Lottie(
        @RawRes val rawRes: Int,
        val iterations: Int = 1,
        val contentScale: ContentScale = ContentScale.Fit,
    ) : FusionMotionSpec

    data class LayeredLottie(
        val layers: List<FusionLottieLayer>,
    ) : FusionMotionSpec
}

data class FusionLottieLayer(
    @RawRes val rawRes: Int,
    val width: Dp,
    val height: Dp,
    val alignment: Alignment = Alignment.Center,
    val alpha: Float = 1f,
    val iterations: Int = LottieConstants.IterateForever,
    val required: Boolean = false,
)

sealed interface FusionVisualResolution {
    data class Available(
        val primary: FusionMotionSpec,
        val fallbacks: List<FusionMotionSpec>,
        val metadata: FusionVisualMetadata,
    ) : FusionVisualResolution

    data class Unavailable(
        val reason: FusionUnavailableReason,
    ) : FusionVisualResolution
}

enum class FusionUnavailableReason {
    PackNotInstalled,
    LicenseBlocked,
    FeatureDisabled,
    UnsupportedDevice,
    CorruptResource,
}

interface FusionAssetResolver {
    fun resolve(key: FusionVisualKey): FusionVisualResolution
}

@Composable
fun FusionMotionHost(
    resolution: FusionVisualResolution,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
) {
    if (!motionEnabled || resolution is FusionVisualResolution.Unavailable) {
        fallback()
        return
    }
    resolution as FusionVisualResolution.Available
    FusionMotionStack(
        specs = listOf(resolution.primary) + resolution.fallbacks,
        modifier = modifier,
        index = 0,
        fallback = fallback,
    )
}

@Composable
private fun FusionMotionStack(
    specs: List<FusionMotionSpec>,
    modifier: Modifier,
    index: Int,
    fallback: @Composable () -> Unit,
) {
    if (index >= specs.size) {
        fallback()
        return
    }
    FusionMotionSpecHost(
        spec = specs[index],
        modifier = modifier,
        unavailable = {
            FusionMotionStack(
                specs = specs,
                modifier = modifier,
                index = index + 1,
                fallback = fallback,
            )
        },
    )
}

@Composable
private fun FusionMotionSpecHost(
    spec: FusionMotionSpec,
    modifier: Modifier,
    unavailable: @Composable () -> Unit,
) {
    when (spec) {
        is FusionMotionSpec.Lottie -> {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(spec.rawRes))
            if (composition == null) {
                unavailable()
            } else {
                LottieAnimation(
                    composition = composition,
                    iterations = spec.iterations,
                    contentScale = spec.contentScale,
                    modifier = modifier,
                )
            }
        }

        is FusionMotionSpec.LayeredLottie -> {
            val compositions = spec.layers.map { layer ->
                rememberLottieComposition(LottieCompositionSpec.RawRes(layer.rawRes)).value
            }
            val requiredReady = spec.layers.withIndex()
                .filter { (_, layer) -> layer.required }
                .all { (index, _) -> compositions[index] != null }
            if (!requiredReady) {
                unavailable()
            } else {
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    spec.layers.forEachIndexed { index, layer ->
                        val composition = compositions[index] ?: return@forEachIndexed
                        LottieAnimation(
                            composition = composition,
                            iterations = layer.iterations,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(layer.alignment)
                                .size(width = layer.width, height = layer.height)
                                .alpha(layer.alpha),
                        )
                    }
                }
            }
        }
    }
}

