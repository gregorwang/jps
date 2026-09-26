package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * v3 bottom sheet: 16dp top corners, 1.5px ink top edge, page-coloured body, 28% scrim,
 * 36×4 grab handle. Slide/drag behaviour comes from Material's sheet (240ms decelerate feel).
 * Pass an optional Japanese [title] with Chinese [gloss] (どの番を観る？ · 换一部番).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjlBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    gloss: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AjlTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.border(AjlStroke.Ink, colors.ink, AjlShape.Sheet),
        sheetState = sheetState,
        shape = AjlShape.Sheet,
        containerColor = colors.bg,
        contentColor = colors.ink,
        tonalElevation = 0.dp,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.line2, RoundedCornerShape(2.dp)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (title != null) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, lineHeight = 26.sp), color = colors.ink)
                    if (gloss != null) Text(gloss, style = AjlTheme.type.caption, color = colors.ink3)
                }
            }
            content()
        }
    }
}

/**
 * Underlined form field (校门 login): Japanese label + Chinese gloss above, 18sp input, 1.5px
 * underline — ink while focused, line2 otherwise.
 */
@Composable
fun UnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    gloss: String? = null,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    error: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    var focused by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = AjlTheme.type.jpLabel, color = colors.ink3)
            if (gloss != null) Text(" · $gloss", style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(vertical = 4.dp)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = AjlTheme.type.body.copy(fontSize = 18.sp, lineHeight = 24.sp), color = colors.faint)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = AjlTheme.type.body.copy(fontSize = 18.sp, lineHeight = 24.sp, color = colors.ink),
                    cursorBrush = SolidColor(colors.ink),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            if (trailing != null) trailing()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(AjlStroke.Ink)
                .background(if (error != null) colors.bad else if (focused) colors.ink else colors.line2),
        )
        if (error != null) Text(error, style = AjlTheme.type.caption, color = colors.bad)
    }
}

/** Small pill for filters/tags (辞書 JLPT level, 全部 / 词汇): 1px outline; ink-filled when selected. */
@Composable
fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(32.dp)
            .background(if (selected) colors.ink else colors.surface, shape)
            .border(AjlStroke.Hair, if (selected) colors.ink else colors.line2, shape)
            .clickableNoRipple(onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AjlTheme.type.caption, color = if (selected) colors.onInk else colors.ink)
    }
}

/** Clickable without ripple — v3 press feedback is colour/offset, never a ripple. */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit, enabled: Boolean = true): Modifier =
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onClick = onClick,
    )

/** Empty state: one quiet serif line, optional Chinese gloss. No illustrations, no exclamation. */
@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier, gloss: String? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp), color = AjlTheme.colors.ink3)
        if (gloss != null) Text(gloss, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
    }
}
