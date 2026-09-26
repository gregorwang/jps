package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import com.animejapaneselab.nativeapp.ui.design.WordTile
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * MOTION §3-04 — a word tile flies in a straight line between the bank and its slot in the
 * dialogue box (240ms in, 180ms back, Ease.Standard, no bounce). Both ends record their bounds
 * with [bank] / [slot]; the data changes first, the real tile at the destination stays invisible
 * while an overlay copy travels, so a burst of taps can never double-place a tile.
 */
@Stable
internal class TileFlightState(val animate: Boolean) {
    internal class Flight(val id: Int, val text: String, val toSlot: Boolean, val from: Rect) {
        val progress = Animatable(0f)
    }

    internal val bankRects = mutableStateMapOf<Int, Rect>()
    internal val slotRects = mutableStateMapOf<Int, Rect>()
    internal val flights = mutableStateListOf<Flight>()
    internal var origin by mutableStateOf(Offset.Zero)

    fun flyingIn(id: Int): Boolean = flights.any { it.id == id && it.toSlot }
    fun flyingBack(id: Int): Boolean = flights.any { it.id == id && !it.toSlot }
    fun busy(id: Int): Boolean = flights.any { it.id == id }

    /** Call right after the data placed tile [id]; the slot tile hides until the copy lands. */
    fun launchIn(id: Int, text: String) {
        slotRects.remove(id)
        if (!animate) return
        val from = bankRects[id] ?: return
        flights.removeAll { it.id == id }
        flights += Flight(id, text, toSlot = true, from = from)
    }

    /** Call right after the data removed tile [id] from the slot; the bank keeps a hollow slot. */
    fun launchBack(id: Int, text: String) {
        val from = slotRects.remove(id)
        if (!animate || from == null) return
        flights.removeAll { it.id == id }
        flights += Flight(id, text, toSlot = false, from = from)
    }

    fun bank(id: Int): Modifier = Modifier.onGloballyPositioned { bankRects[id] = it.boundsInRoot() }
    fun slot(id: Int): Modifier = Modifier.onGloballyPositioned { slotRects[id] = it.boundsInRoot() }
}

@Composable
internal fun rememberTileFlightState(key: Any, animate: Boolean): TileFlightState =
    remember(key, animate) { TileFlightState(animate) }

/** Draws the travelling copies. Put it last inside a Box that covers both ends. */
@Composable
internal fun BoxScope.TileFlightOverlay(state: TileFlightState) {
    Box(
        Modifier
            .matchParentSize()
            .onGloballyPositioned { state.origin = it.positionInRoot() }
            .clearAndSetSemantics {},
    ) {
        state.flights.toList().forEach { flight ->
            FlyingTile(state, flight)
        }
    }
}

@Composable
private fun FlyingTile(state: TileFlightState, flight: TileFlightState.Flight) {
    LaunchedEffect(flight) {
        // The destination is laid out one frame after the data change; wait for it.
        snapshotFlow { if (flight.toSlot) state.slotRects[flight.id] else state.bankRects[flight.id] }
            .filterNotNull()
            .first()
        flight.progress.animateTo(
            1f,
            tween(if (flight.toSlot) MotionTokens.Dur.TileFly else MotionTokens.Dur.TileReturn, easing = MotionTokens.Ease.Standard),
        )
        state.flights.remove(flight)
    }
    val to = (if (flight.toSlot) state.slotRects[flight.id] else state.bankRects[flight.id]) ?: flight.from
    Box(
        Modifier.offset {
            val t = flight.progress.value
            val x = flight.from.left + (to.left - flight.from.left) * t - state.origin.x
            val y = flight.from.top + (to.top - flight.from.top) * t - state.origin.y
            IntOffset(x.roundToInt(), y.roundToInt())
        },
    ) {
        WordTile(flight.text, onClick = {})
    }
}
