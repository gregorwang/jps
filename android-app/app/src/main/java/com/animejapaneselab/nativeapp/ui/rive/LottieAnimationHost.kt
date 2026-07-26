package com.animejapaneselab.nativeapp.ui.rive

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.animejapaneselab.nativeapp.ui.feedback.VisualAsset
import com.animejapaneselab.nativeapp.ui.feedback.rawResourceId

@Composable
fun LottieAnimationHost(
    asset: VisualAsset.Lottie,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val rawId = context.rawResourceId(asset.rawName)
    if (rawId == null) {
        fallback()
        return
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = asset.iterations,
    )
    if (composition == null) {
        Box(modifier = modifier) { fallback() }
    } else {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier,
        )
    }
}
