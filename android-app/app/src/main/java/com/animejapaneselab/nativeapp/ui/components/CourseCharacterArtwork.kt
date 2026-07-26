package com.animejapaneselab.nativeapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.R

enum class CourseCharacterRole {
    Today,
    Translation,
    Listening,
    Grammar,
    Linguistics,
    Shadowing,
    PathActive,
    PathLocked,
    Celebration,
    Encouragement,
}

private data class CourseCharacterAsset(
    @DrawableRes val drawableRes: Int,
    val background: Color,
)

/**
 * One course-aware character layer for every screen. Character choice is stable for an episode,
 * so recomposition never causes a visible random swap.
 */
@Composable
fun CourseCharacterArtwork(
    workSlug: String,
    role: CourseCharacterRole,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
    stableSeed: Int = 0,
) {
    val asset = remember(workSlug, role, stableSeed) {
        resolveCourseCharacter(workSlug, role, stableSeed)
    }
    val floatOffset = if (motionEnabled && asset != null) {
        val transition = rememberInfiniteTransition(label = "course-character")
        val offset by transition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1_800 + Math.floorMod(stableSeed, 5) * 90,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "course-character-float",
        )
        offset
    } else {
        0f
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(asset?.background ?: Color(0xFFF3F0F7)),
        contentAlignment = Alignment.Center,
    ) {
        if (asset == null) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF6C5CE7),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
            )
        } else {
            Image(
                painter = painterResource(asset.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .graphicsLayer { translationY = floatOffset },
            )
        }
    }
}

private fun resolveCourseCharacter(
    workSlug: String,
    role: CourseCharacterRole,
    stableSeed: Int,
): CourseCharacterAsset? {
    val candidates = when (normalizeCourseWorkSlug(workSlug)) {
        "k-on" -> when (role) {
            CourseCharacterRole.Today -> intArrayOf(
                R.drawable.k_on_yui_character,
                R.drawable.k_on_mio_character,
                R.drawable.k_on_ritsu_character,
                R.drawable.k_on_mugi_character,
                R.drawable.k_on_azusa_character_v2,
                R.drawable.k_on_ui_character,
                R.drawable.k_on_sawako_character,
                R.drawable.k_on_jun_character,
            )
            CourseCharacterRole.Translation -> intArrayOf(
                R.drawable.k_on_yui_character,
                R.drawable.k_on_mugi_character,
                R.drawable.k_on_ui_character,
            )
            CourseCharacterRole.Listening -> intArrayOf(
                R.drawable.k_on_mio_character,
                R.drawable.k_on_azusa_character_v2,
                R.drawable.k_on_jun_character,
            )
            CourseCharacterRole.Grammar -> intArrayOf(
                R.drawable.k_on_mugi_character,
                R.drawable.k_on_azusa_character_v2,
                R.drawable.k_on_sawako_character,
            )
            CourseCharacterRole.Linguistics -> intArrayOf(
                R.drawable.k_on_azusa_character_v2,
                R.drawable.k_on_mio_character,
                R.drawable.k_on_sawako_character,
            )
            CourseCharacterRole.Shadowing -> intArrayOf(
                R.drawable.k_on_ritsu_character,
                R.drawable.k_on_yui_character,
                R.drawable.k_on_jun_character,
            )
            CourseCharacterRole.PathActive -> intArrayOf(
                R.drawable.k_on_azusa_character_v2,
                R.drawable.k_on_yui_character,
                R.drawable.k_on_ui_character,
            )
            CourseCharacterRole.PathLocked -> intArrayOf(
                R.drawable.k_on_mugi_character,
                R.drawable.k_on_mio_character,
                R.drawable.k_on_sawako_character,
            )
            CourseCharacterRole.Celebration -> intArrayOf(
                R.drawable.k_on_ritsu_character,
                R.drawable.k_on_yui_character,
                R.drawable.k_on_jun_character,
            )
            CourseCharacterRole.Encouragement -> intArrayOf(
                R.drawable.k_on_mio_character,
                R.drawable.k_on_mugi_character,
                R.drawable.k_on_ui_character,
            )
        } to Color(0xFFFFF8E7)

        "re-zero" -> when (role) {
            CourseCharacterRole.Today -> intArrayOf(
                R.drawable.rezero_emilia_character,
                R.drawable.rezero_rem_character,
                R.drawable.rezero_otto_character,
                R.drawable.rezero_frederica_character,
            )
            CourseCharacterRole.Translation -> intArrayOf(
                R.drawable.rezero_emilia_character,
                R.drawable.rezero_ram_character,
                R.drawable.rezero_otto_character,
            )
            CourseCharacterRole.Listening -> intArrayOf(
                R.drawable.rezero_rem_character,
                R.drawable.rezero_emilia_character,
                R.drawable.rezero_frederica_character,
            )
            CourseCharacterRole.Grammar -> intArrayOf(
                R.drawable.rezero_beatrice_character,
                R.drawable.rezero_ram_character,
                R.drawable.rezero_echidna_character,
            )
            CourseCharacterRole.Linguistics -> intArrayOf(
                R.drawable.rezero_beatrice_character,
                R.drawable.rezero_emilia_character,
                R.drawable.rezero_echidna_character,
                R.drawable.rezero_otto_character,
            )
            CourseCharacterRole.Shadowing -> intArrayOf(
                R.drawable.rezero_subaru_character,
                R.drawable.rezero_emilia_character,
                R.drawable.rezero_petelgeuse_character,
            )
            CourseCharacterRole.PathActive -> intArrayOf(
                R.drawable.rezero_puck_character,
                R.drawable.rezero_emilia_character,
                R.drawable.rezero_otto_character,
            )
            CourseCharacterRole.PathLocked -> intArrayOf(
                R.drawable.rezero_ram_character,
                R.drawable.rezero_beatrice_character,
                R.drawable.rezero_frederica_character,
            )
            CourseCharacterRole.Celebration -> intArrayOf(
                R.drawable.rezero_emilia_character,
                R.drawable.rezero_puck_character,
                R.drawable.rezero_frederica_character,
            )
            CourseCharacterRole.Encouragement -> intArrayOf(
                R.drawable.rezero_rem_character,
                R.drawable.rezero_subaru_character,
                R.drawable.rezero_otto_character,
            )
        } to Color(0xFFF8F4FF)

        else -> return null
    }
    val resources = candidates.first
    return CourseCharacterAsset(
        drawableRes = resources[Math.floorMod(stableSeed, resources.size)],
        background = candidates.second,
    )
}

private fun normalizeCourseWorkSlug(workSlug: String): String {
    return when (val normalized = workSlug.trim().lowercase().replace('_', '-')) {
        "rezero" -> "re-zero"
        else -> normalized
    }
}
