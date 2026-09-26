package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.Seal
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.VerticalText
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.screens.today.TodayRules
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * アイキャッチ (MOTION §3-01, XEyecatch): a work-colour screentone band slides in tilted 12°,
 * the vertical 第N話 rises glyph by glyph, the work seal stamps down at 520ms (CLOCK_TICK), a
 * thin line fills over 900ms and the first question follows. Any tap skips. [short] (same
 * episode again today) keeps only the band and the title and lasts 400ms.
 */
@Composable
internal fun Eyecatch(
    workSlug: String,
    episode: Int,
    setLine: String,
    quote: String?,
    short: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val reduced = rememberReducedMotion()
    val feedback = LocalFeedbackEngine.current
    val total = if (short) 400 else 900
    val clock = remember { Animatable(if (reduced) total.toFloat() else 0f) }
    var finished by remember { mutableStateOf(false) }
    val latestDone by rememberUpdatedState(onDone)
    fun finish() {
        if (!finished) {
            finished = true
            latestDone()
        }
    }
    val stampAt = 520f
    LaunchedEffect(Unit) {
        if (!reduced) clock.animateTo(total.toFloat(), tween(total, easing = LinearEasing))
        else kotlinx.coroutines.delay(total.toLong())
        finish()
    }
    LaunchedEffect(Unit) {
        if (short) return@LaunchedEffect
        // The seal lands 60% into its stamp curve.
        kotlinx.coroutines.delay(if (reduced) 0L else (stampAt + MotionTokens.Dur.Stamp * 0.6f).toLong())
        feedback?.emit(FeedbackEvent.OptionSelect)
    }
    val title = TextRules.episodeLabel(episode)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) { finish() }
            .semantics { contentDescription = "$title，点任意处跳过" },
    ) {
        val bandShift = maxWidth.value + 120f
        // Screentone band, tilted, sliding in from the left over 300ms.
        Box(
            Modifier
                .offset(x = (-60).dp, y = maxHeight * 0.5f)
                .requiredWidth(maxWidth + 120.dp)
                .height(220.dp)
                .graphicsLayer {
                    rotationZ = -12f
                    val t = (clock.value / 300f).coerceIn(0f, 1f)
                    translationX = -(1f - MotionTokens.Ease.Decelerate.transform(t)) * bandShift * density
                }
                .screentone(work.tone(0.30f), angleDegrees = 0f, spacing = 7.dp, dotRadius = 1.2.dp),
        )
        Box(Modifier.fillMaxWidth().height(3.dp).background(work.accent))
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(LessonRules.broadcastTag(workSlug, episode), style = AjlTheme.type.meta, color = colors.ink3)
                if (!short) Text("点任意处跳过", style = AjlTheme.type.meta, color = colors.ink3)
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                VerticalText(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 36.dp, top = 64.dp),
                    style = AjlTheme.type.jpDisplay.copy(fontSize = 88.sp, lineHeight = 92.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                    glyphSpacing = 1.02f,
                    revealStaggerMillis = if (short) 30 else 60,
                    revealDurationMillis = if (short) 160 else 220,
                    revealStartDelayMillis = if (short) 40 else 120,
                )
                if (!short) {
                    if (!quote.isNullOrBlank()) {
                        VerticalText(
                            text = remember(quote) { TodayRules.verticalLayout(quote, 9) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 150.dp, top = 72.dp)
                                .graphicsLayer { alpha = ((clock.value - 300f) / 240f).coerceIn(0f, 1f) },
                            style = AjlTheme.type.jpBody.copy(fontSize = 20.sp, lineHeight = 24.sp),
                            color = colors.ink2,
                            glyphSpacing = 1.5f,
                        )
                    }
                    Seal(
                        text = WorkIdentity.sealText(workSlug),
                        size = 46.dp,
                        modifier = Modifier
                            .padding(start = 28.dp, top = 96.dp)
                            .graphicsLayer {
                                val p = ((clock.value - stampAt) / MotionTokens.Dur.Stamp).coerceIn(0f, 1f)
                                if (p < 0.6f) {
                                    val k = MotionTokens.Ease.Accelerate.transform(p / 0.6f)
                                    val s = 1.8f + (0.92f - 1.8f) * k
                                    scaleX = s; scaleY = s
                                    rotationZ = -18f + 14f * k
                                    alpha = k
                                } else {
                                    val k = MotionTokens.Ease.Decelerate.transform((p - 0.6f) / 0.4f)
                                    val s = 0.92f + 0.08f * k
                                    scaleX = s; scaleY = s
                                    rotationZ = -4f - 2f * k
                                    alpha = 1f
                                }
                            },
                        rotation = 0f,
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    WorkIdentity.displayName(workSlug, workSlug),
                    style = AjlTheme.type.jpTitle.copy(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.ink,
                )
                if (setLine.isNotBlank()) Text(setLine, style = AjlTheme.type.body, color = colors.ink2)
                val line = colors.line
                val accent = work.accent
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .drawBehind {
                            drawRect(line)
                            val f = (clock.value / total).coerceIn(0f, 1f)
                            drawRect(accent, size = Size(size.width * f, size.height))
                        },
                )
            }
        }
    }
}
