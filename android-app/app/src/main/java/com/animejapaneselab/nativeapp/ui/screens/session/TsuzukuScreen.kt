package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** One remembered line on つづく (a missed question) with a mono tag on the right. */
data class TsuzukuLine(val text: String, val japanese: Boolean, val tag: String)

/** 次回予告 — what comes next: a title, an optional mono meta, a quoted line and a short note. */
data class TsuzukuPreview(
    val title: String,
    val meta: String? = null,
    val line: String? = null,
    val note: String? = null,
)

/**
 * つづく (MOTION §3-06, XTsuzuku) — the end of a set. A 90ms white cut, the screentone header
 * drifting slowly right, 「つづく」 sliding in glyph by glyph from 40dp (240ms, 50ms apart), the
 * 次回予告 card rising at 700ms, the tally shown as-is at 900ms (no rolling numbers, no XP). Below:
 * the lines worth another look; one outline + one ink action.
 *
 * Shared by the lesson, read-air and foundation sessions.
 */
@Composable
fun TsuzukuScreen(
    eyebrow: String,
    tally: String,
    meta: String,
    noted: List<TsuzukuLine>,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    notedTitle: String = "这次记下的 ${noted.size} 句",
    preview: TsuzukuPreview? = null,
    quietLabel: String? = null,
    quietEnabled: Boolean = true,
    onQuiet: (() -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val reduced = rememberReducedMotion()
    val clock = remember { Animatable(if (reduced) 1200f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) clock.animateTo(1200f, tween(1200, easing = LinearEasing))
    }
    val drift by if (reduced) {
        remember { mutableStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "tsuzuku-tone").animateFloat(
            0f, 70f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "tone-drift",
        )
    }
    val flashColor = if (colors.isDark) Color.White.copy(alpha = 0.55f) else Color.White

    Box(modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize()) {
            // Header: screentone fading into the page, tally, the big つづく.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { clip = true },
                ) {
                    // 7dp dot pitch × 10 = 70dp, so the loop restarts seamlessly.
                    Box(
                        Modifier
                            .offset(x = (-70).dp)
                            .requiredWidth(1200.dp)
                            .height(300.dp)
                            .graphicsLayer { translationX = drift * density }
                            .screentone(work.tone(0.28f), angleDegrees = 0f, spacing = 7.dp, dotRadius = 1.2.dp),
                    )
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(0.1f to colors.bg, 0.7f to colors.bg.copy(alpha = 0f))),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(eyebrow, style = AjlTheme.type.meta, color = colors.ink3, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton44(Icons.Rounded.Close, "关闭", onClose, tint = colors.ink2)
                }
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, top = 76.dp)
                        .graphicsLayer { alpha = if (clock.value >= 900f) 1f else 0f },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(tally, style = AjlTheme.type.body, color = colors.ink2)
                    if (meta.isNotBlank()) Text(meta, style = AjlTheme.type.meta.copy(fontSize = 13.sp, lineHeight = 18.sp), color = colors.ink3)
                }
                TsuzukuTitle(
                    clock = { clock.value },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 18.dp),
                )
                Hairline(Modifier.align(Alignment.BottomCenter), strong = true)
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                if (preview != null) {
                    Column(
                        Modifier.graphicsLayer {
                            val t = MotionTokens.Ease.Decelerate.transform(((clock.value - 700f) / 300f).coerceIn(0f, 1f))
                            alpha = t
                            translationY = (1f - t) * 12.dp.toPx()
                        },
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "次回予告",
                                style = AjlTheme.type.jpTitle.copy(fontSize = 18.sp, lineHeight = 24.sp),
                                color = work.accent,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(listOfNotNull("NEXT", preview.meta).joinToString(" · "), style = AjlTheme.type.meta, color = colors.ink3)
                        }
                        MangaPanel(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(preview.title, style = AjlTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                                if (!preview.line.isNullOrBlank()) {
                                    Text("「${preview.line}」", style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 27.sp), color = colors.ink)
                                }
                                if (!preview.note.isNullOrBlank()) {
                                    Text(preview.note, style = AjlTheme.type.caption, color = colors.ink3)
                                }
                            }
                        }
                    }
                }
                if (noted.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(notedTitle, style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                        Column {
                            Hairline()
                            noted.forEach { line ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        line.text,
                                        style = if (line.japanese) AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 22.sp) else AjlTheme.type.body,
                                        color = colors.ink,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(line.tag, style = AjlTheme.type.meta, color = colors.ink3)
                                }
                                Hairline()
                            }
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (quietLabel != null && onQuiet != null) {
                    OutlineButton(quietLabel, onQuiet, enabled = quietEnabled, modifier = Modifier.height(48.dp))
                }
                InkButton(primaryLabel, onPrimary, Modifier.weight(1f), height = 48.dp)
            }
        }
        // 90ms white cut.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = if (reduced) 0f else (1f - clock.value / 90f).coerceIn(0f, 1f) }
                .background(flashColor)
                .clearAndSetSemantics {},
        )
    }
}

/** 「つづく」 in 64sp serif; each glyph slides in from 40dp right. */
@Composable
private fun TsuzukuTitle(clock: () -> Float, modifier: Modifier = Modifier) {
    val text = "つづく"
    val start = 100f
    val stagger = 50f
    val duration = 240f
    val shift = with(LocalDensity.current) { 40.dp.toPx() }
    Row(modifier.clearAndSetSemantics { contentDescription = text }) {
        text.forEachIndexed { index, ch ->
            Text(
                ch.toString(),
                style = AjlTheme.type.jpDisplay.copy(fontSize = 64.sp, lineHeight = 68.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = AjlTheme.colors.ink,
                modifier = Modifier.graphicsLayer {
                    val t = ((clock() - start - index * stagger) / duration).coerceIn(0f, 1f)
                    val eased = MotionTokens.Ease.Decelerate.transform(t)
                    alpha = eased
                    translationX = (1f - eased) * shift
                },
            )
        }
    }
}
