package com.animejapaneselab.nativeapp

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.animejapaneselab.nativeapp.ui.LabApp
import com.animejapaneselab.nativeapp.ui.theme.AnimeJapaneseLabTheme

object ComposeHost {
    @JvmStatic
    fun install(activity: ComponentActivity) {
        activity.setContent {
            AnimeJapaneseLabTheme {
                Surface {
                    LabApp()
                }
            }
        }
    }
}
