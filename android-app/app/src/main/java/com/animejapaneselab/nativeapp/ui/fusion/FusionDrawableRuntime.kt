package com.animejapaneselab.nativeapp.ui.fusion

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

sealed interface FusionDrawableResolution {
    data class Available(
        @DrawableRes val drawableRes: Int,
        val metadata: FusionVisualMetadata,
    ) : FusionDrawableResolution

    data class Unavailable(
        val reason: FusionUnavailableReason,
    ) : FusionDrawableResolution
}

interface FusionDrawableResolver {
    fun resolveDrawable(key: FusionVisualKey): FusionDrawableResolution
}

@Composable
fun FusionDrawableHost(
    resolution: FusionDrawableResolution,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: @Composable () -> Unit,
) {
    when (resolution) {
        is FusionDrawableResolution.Available -> Image(
            painter = painterResource(resolution.drawableRes),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )

        is FusionDrawableResolution.Unavailable -> fallback()
    }
}
