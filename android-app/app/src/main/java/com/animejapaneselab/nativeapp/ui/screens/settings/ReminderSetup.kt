package com.animejapaneselab.nativeapp.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.animejapaneselab.nativeapp.platform.ReminderHealth
import com.animejapaneselab.nativeapp.platform.ReminderHealthReader
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** Re-reads [ReminderHealth] every time the screen resumes (e.g. back from system settings). */
@Composable
fun rememberReminderHealth(): ReminderHealth {
    val context = LocalContext.current
    var health by remember { mutableStateOf(ReminderHealthReader.read(context)) }
    LifecycleResumeEffect(Unit) {
        health = ReminderHealthReader.read(context)
        onPauseOrDispose { }
    }
    return health
}

/** 今日: a quiet tool-style strip while reminders cannot reach the phone; opens 設定. */
@Composable
fun ReminderHealthStrip(health: ReminderHealth, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .border(1.dp, colors.line, AjlShape.Tool)
            .clickableNoRipple(onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(6.dp).background(colors.bad, CircleShape))
        Text(
            "放課後チャイム收不到 · 还差 ${health.missing} 步",
            style = AjlTheme.type.body.copy(fontSize = 14.sp),
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Text("去设置", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink2)
    }
}
