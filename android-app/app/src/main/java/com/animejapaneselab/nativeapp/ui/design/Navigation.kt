package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

enum class TopBarNav { None, Back, Close }

/**
 * Secondary-page / session top bar (56dp): back ‹ or close ×, then a Japanese serif title or
 * custom [center] content (e.g. a [ProgressLine] + "7/12"), then trailing [actions].
 */
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    nav: TopBarNav = TopBarNav.Back,
    onNav: () -> Unit = {},
    navContentDescription: String = if (nav == TopBarNav.Close) "退出" else "返回",
    center: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = if (nav == TopBarNav.None) 20.dp else 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (nav) {
            TopBarNav.Back -> IconButton44(Icons.AutoMirrored.Rounded.ArrowBack, navContentDescription, onNav)
            TopBarNav.Close -> IconButton44(Icons.Rounded.Close, navContentDescription, onNav, tint = AjlTheme.colors.ink2)
            TopBarNav.None -> Unit
        }
        if (center != null) {
            center()
        } else {
            if (title != null) {
                Text(
                    title,
                    style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, lineHeight = 26.sp),
                    color = AjlTheme.colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
        actions()
    }
}

data class TabItem(val jp: String, val zh: String)

/**
 * Four-tab bottom bar: Japanese label + tiny Chinese caption. Current tab is weight 700 with a
 * 2px work-colour indicator at the top that slides 180ms to the new tab. Draws its own
 * navigation-bar inset.
 */
@Composable
fun BottomTabBar(
    items: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val reduced = rememberReducedMotion()
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Hairline()
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            val cell = maxWidth / items.size.coerceAtLeast(1)
            val indicatorX by animateDpAsState(
                targetValue = cell * selectedIndex + (cell - 18.dp) / 2,
                animationSpec = MotionTokens.standard(MotionTokens.Dur.State, reduced),
                label = "tab-indicator",
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(index) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            item.jp,
                            style = AjlTheme.type.jpTitle.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (selected) colors.ink else colors.ink3,
                        )
                        Text(
                            item.zh,
                            style = AjlTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 13.sp),
                            color = if (selected) colors.ink else colors.ink3,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(18.dp)
                    .height(2.dp)
                    .background(accent),
            )
        }
    }
}

/**
 * Big serif text tabs at the top of a page (課程 / 言語学). Selected: 700 + 2px work underline;
 * others: faint 400.
 */
@Composable
fun TextTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .selectable(
                        selected = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    )
                    .drawBehind {
                        if (selected) {
                            val h = 2.dp.toPx()
                            drawRect(accent, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = AjlTheme.type.jpTitle.copy(
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (selected) colors.ink else colors.faint,
                )
            }
        }
    }
}

/**
 * 巻 switch (第一巻 アニメの台詞 / 第二巻 基礎): equal ink-outlined blocks, the selected one filled
 * with ink.
 */
@Composable
fun VolumeSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(AjlShape.Panel)
                    .background(if (selected) colors.ink else colors.surface)
                    .border(AjlStroke.Ink, colors.ink, AjlShape.Panel)
                    .selectable(
                        selected = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = AjlTheme.type.jpLabel.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (selected) colors.onInk else colors.ink,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Plain list row: ≥48dp, 1px bottom hairline. Put anything in [leading]/[trailing]; the middle
 * takes the remaining width.
 */
@Composable
fun LineRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = 48.dp,
    divider: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val base = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Column(modifier.fillMaxWidth().then(base)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) leading()
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
            if (trailing != null) trailing()
        }
        if (divider) Hairline()
    }
}
