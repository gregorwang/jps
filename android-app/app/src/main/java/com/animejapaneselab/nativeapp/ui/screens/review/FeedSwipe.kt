package com.animejapaneselab.nativeapp.ui.screens.review

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/*
 * The 知識 feed's swipe, TikTok-style: a light flick goes to the next card. VerticalPager's
 * defaults got in the way twice: a slow flick only turns the page past half a card, and a card that
 * scrolls inside owns the gesture, so the pager never gets the flick's speed (its page connection
 * swallows the leftover velocity) and the card snaps back.
 */

/** Past this share of a card, letting go turns the page even without speed. */
private const val TurnAt = 0.18f

private val Snap = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 520f)

@Composable
internal fun feedFlingBehavior(state: PagerState): TargetedFlingBehavior =
    PagerDefaults.flingBehavior(
        state = state,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapAnimationSpec = Snap,
        snapPositionalThreshold = TurnAt,
    )

/**
 * Put around a page whose card scrolls inside: once the drag has moved the pager (the card was at
 * its end), letting go turns the page by the flick's speed or distance instead of snapping back.
 */
@Composable
internal fun rememberFeedPageHandoff(state: PagerState): NestedScrollConnection {
    val minVelocity = with(LocalDensity.current) { 280.dp.toPx() }
    return remember(state, minVelocity) {
        object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                val from = state.settledPage
                val offset = state.currentPage - from + state.currentPageOffsetFraction
                if (abs(offset) < 0.001f) return Velocity.Zero
                // Pointer space: a flick up is negative and goes to the next card.
                val step = when {
                    available.y < -minVelocity -> 1
                    available.y > minVelocity -> -1
                    offset > TurnAt -> 1
                    offset < -TurnAt -> -1
                    else -> 0
                }
                state.animateScrollToPage((from + step).coerceIn(0, (state.pageCount - 1).coerceAtLeast(0)), animationSpec = Snap)
                return available
            }
        }
    }
}
