package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlin.math.roundToInt

/**
 * Press feedback for the ink button: scale 0.98 over 90ms, release over 180ms. Pair with
 * [pressedInk] for the 6% darker fill.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, enabled: Boolean = true): Modifier {
    val reduced = rememberReducedMotion()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduced) 0.98f else 1f,
        animationSpec = tween(
            if (pressed) MotionTokens.Dur.Press else MotionTokens.Dur.Release,
            easing = MotionTokens.Ease.Standard,
        ),
        label = "press-scale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Ink fill 6% "deeper": towards black on light, towards the page on dark. */
@Composable
private fun pressedInk(base: Color, pressed: Boolean): Color {
    val colors = AjlTheme.colors
    val target = if (pressed) lerp(base, if (colors.isDark) colors.bg else Color.Black, 0.06f) else base
    val color by animateColorAsState(
        target,
        tween(if (pressed) MotionTokens.Dur.Press else MotionTokens.Dur.Release),
        label = "ink-press",
    )
    return color
}

/**
 * The one ink primary button per screen.
 *
 * - `InkButton("检查")` → centred label.
 * - `InkButton("继续 · 替憂把台词说完", caption = "听音拼句 · 7/12 · 约 4 分钟", trailingArrow = true,
 *   progress = 0.58f)` → left-aligned two-line with a work-colour progress line along the bottom.
 * - `InkButton("登录", jpText = "登校する", trailingArrow = true)` → Japanese serif label with
 *   Chinese gloss.
 */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    jpText: String? = null,
    caption: String? = null,
    trailingArrow: Boolean = false,
    progress: Float? = null,
    height: Dp = if (caption != null) 60.dp else 52.dp,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val fill = if (enabled) pressedInk(colors.ink, pressed && active) else colors.line2
    val content = if (enabled) colors.onInk else colors.ink3
    val content2 = if (enabled) colors.onInk2 else colors.ink3
    val work = AjlTheme.work
    val leftAligned = caption != null || jpText != null || trailingArrow
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(interaction, active)
            .clip(AjlShape.Button)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = active,
                role = Role.Button,
                onClick = onClick,
            )
            .drawBehind {
                if (progress != null && progress > 0f) {
                    val w = size.width * progress.coerceIn(0f, 1f)
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(work.rampLight, work.progressColor(progress)),
                            endX = w,
                        ),
                        topLeft = Offset(0f, size.height - 3.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(w, 3.dp.toPx()),
                    )
                }
            }
            .padding(horizontal = 18.dp),
        contentAlignment = if (leftAligned) Alignment.CenterStart else Alignment.Center,
    ) {
        if (loading) {
            LoadingDots(delayMillis = 0, color = content)
        } else if (!leftAligned) {
            Text(text, style = type.label, color = content, maxLines = 1)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (jpText != null) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(jpText, style = type.jpTitle.copy(fontSize = type.jpTitle.fontSize * 1.06f), color = content, maxLines = 1)
                            Text(text, style = type.caption, color = content2, maxLines = 1)
                        }
                    } else {
                        Text(text, style = type.label, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (caption != null) {
                        Text(caption, style = type.meta, color = content2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (trailingArrow) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Secondary action: 1px hairline outline (tools), or 1.5px ink outline when [ink]. */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ink: Boolean = false,
    compact: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val colors = AjlTheme.colors
    val shape = if (ink) AjlShape.Panel else RoundedCornerShape(if (compact) 6.dp else 10.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .heightIn(min = if (compact) 32.dp else 44.dp)
            .clip(shape)
            .background(if (pressed) colors.sunken else colors.surface.copy(alpha = if (ink) 1f else 0f))
            .border(if (ink) AjlStroke.Ink else AjlStroke.Hair, if (ink) colors.ink else colors.line2, shape)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) 10.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        val tint = if (enabled) colors.ink else colors.faint
        if (leadingIcon != null) Icon(leadingIcon, null, tint = tint, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
        Text(
            text,
            style = if (compact) AjlTheme.type.caption else AjlTheme.type.label.copy(fontSize = AjlTheme.type.body.fontSize),
            color = tint,
            maxLines = 1,
        )
        if (trailingIcon != null) Icon(trailingIcon, null, tint = tint, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
    }
}

/** Text-only action (跳过, 稍后). */
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AjlTheme.colors.ink2,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .clip(AjlShape.Button)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .graphicsLayer { alpha = if (pressed) 0.6f else 1f }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AjlTheme.type.label.copy(fontSize = AjlTheme.type.body.fontSize), color = if (enabled) color else AjlTheme.colors.faint)
    }
}

/** 44dp icon-only touch target. [contentDescription] is required for accessibility. */
@Composable
fun IconButton44(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AjlTheme.colors.ink,
    iconSize: Dp = 20.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (pressed) AjlTheme.colors.sunken else Color.Transparent)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (enabled) tint else AjlTheme.colors.faint, modifier = Modifier.size(iconSize))
    }
}

/**
 * 漫画字块 — a word tile with a 2dp solid ink shadow. Pressing pushes it 2dp right-down and the
 * shadow collapses (80ms), like pressing a physical block. [used] leaves a sunken empty slot in
 * place (after the tile flew into the dialogue box).
 */
@Composable
fun WordTile(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    used: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = AjlShape.Tile,
) {
    val colors = AjlTheme.colors
    val reduced = rememberReducedMotion()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(
        targetValue = if (pressed && enabled && !used && !reduced) AjlStroke.SolidShadow else 0.dp,
        animationSpec = tween(80, easing = MotionTokens.Ease.Standard),
        label = "tile-sink",
    )
    if (used) {
        Box(
            modifier = modifier
                .height(48.dp)
                .clip(shape)
                .background(colors.sunken)
                .border(AjlStroke.Hair, colors.line2, shape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Invisible text keeps the slot the same width as the tile that left it.
            Text(text, style = AjlTheme.type.jpBody, color = Color.Transparent, maxLines = 1)
        }
        return
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .solidShadow(AjlStroke.SolidShadow - sink, colors.ink, shape)
            .offset { IntOffset(sink.toPx().roundToInt(), sink.toPx().roundToInt()) }
            .clip(shape)
            .background(colors.surface)
            .border(AjlStroke.Ink, if (enabled) colors.ink else colors.line2, shape)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AjlTheme.type.jpBody, color = if (enabled) colors.ink else colors.faint, maxLines = 1)
    }
}

/** 40×24 switch: ink track when on, line2 when off. Thumb slides 180ms. */
@Composable
fun InkSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AjlTheme.colors
    val reduced = rememberReducedMotion()
    val x by animateDpAsState(
        targetValue = if (checked) 16.dp else 0.dp,
        animationSpec = MotionTokens.standard(MotionTokens.Dur.State, reduced),
        label = "switch-thumb",
    )
    val track by animateColorAsState(
        if (checked) colors.ink else colors.line2,
        MotionTokens.standard(MotionTokens.Dur.State, reduced),
        label = "switch-track",
    )
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(20.dp)
                .background(if (colors.isDark && checked) colors.bg else Color.White, CircleShape),
        )
    }
}
