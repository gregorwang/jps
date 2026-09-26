package com.animejapaneselab.nativeapp.ui.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.pressScale
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * Legacy primary button kept for v2 screens during the rewrite; it now renders the v3 ink
 * button (scale 0.98 press, no bounce, no haptic). New code uses `ui.design.InkButton`.
 */
@Composable
fun PressablePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = AjlTheme.colors.ink,
    contentColor: Color = AjlTheme.colors.onInk,
) {
    PressablePrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Text(text = text, style = AjlTheme.type.label)
    }
}

@Composable
fun PressablePrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = AjlTheme.colors.ink,
    contentColor: Color = AjlTheme.colors.onInk,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val active = enabled && !loading
    val fill = if (enabled) containerColor else AjlTheme.colors.line2
    val fg = if (enabled) contentColor else AjlTheme.colors.ink3
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .pressScale(interaction, active)
            .clip(AjlShape.Button)
            .background(fill)
            .clickable(interaction, indication = null, enabled = active, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            if (loading) LoadingDots(delayMillis = 0, color = fg) else content()
        }
    }
}
