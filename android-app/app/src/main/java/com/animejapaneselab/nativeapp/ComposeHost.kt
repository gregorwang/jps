package com.animejapaneselab.nativeapp

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.LabApp
import com.animejapaneselab.nativeapp.ui.theme.AnimeJapaneseLabTheme
import kotlinx.coroutines.delay

object ComposeHost {
    @JvmStatic
    fun install(activity: ComponentActivity) {
        activity.setContent {
            var showLab by remember { mutableStateOf(false) }
            var showStartupOverlay by remember { mutableStateOf(true) }
            var startupInteractionCount by remember { mutableIntStateOf(0) }

            LaunchedEffect(startupInteractionCount) {
                delay(StartupQuietBeforeLabMs)
                showLab = true
            }

            LaunchedEffect(showLab) {
                if (showLab) {
                    withFrameNanos { }
                    withFrameNanos { }
                    showStartupOverlay = false
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (showLab) {
                    AnimeJapaneseLabTheme {
                        Surface {
                            LabApp()
                        }
                    }
                }
                if (!showLab || showStartupOverlay) {
                    ComposeStartupPlaceholder(
                        onInteraction = {
                            if (!showLab) startupInteractionCount += 1
                        },
                    )
                }
            }
        }
    }

    private const val StartupQuietBeforeLabMs = 600L
}

@androidx.compose.runtime.Composable
private fun ComposeStartupPlaceholder(onInteraction: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .background(Color(0xFFFBFCF8))
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onInteraction,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF58CC02)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "JL",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
