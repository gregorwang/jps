package com.animejapaneselab.nativeapp.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.ui.completion.LessonCompleteContent
import com.animejapaneselab.nativeapp.ui.completion.LessonResultUiState
import com.animejapaneselab.nativeapp.data.AudioKind
import com.animejapaneselab.nativeapp.data.DialogueLine
import com.animejapaneselab.nativeapp.data.EpisodeFocus
import com.animejapaneselab.nativeapp.data.EpisodeOption
import com.animejapaneselab.nativeapp.data.EpisodeSelection
import com.animejapaneselab.nativeapp.data.FirstEnabledPronunciationSentenceId
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.ReadAirScene
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.WorkOption
import com.animejapaneselab.nativeapp.domain.LessonEngine
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.ReadAirTrainingState
import com.animejapaneselab.nativeapp.ui.fusion.AnimeLabFusionAssetResolver
import com.animejapaneselab.nativeapp.ui.fusion.FusionMotionHost
import com.animejapaneselab.nativeapp.ui.fusion.FusionVisualKey
import com.animejapaneselab.nativeapp.ui.theme.AnimeJapaneseLabTheme
import com.animejapaneselab.nativeapp.ui.screens.LessonScreen

/** Debug-only Asset Lab entry. It is absent from the release manifest and dependency graph. */
class FusionPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val preview = intent.getStringExtra("preview")
        setContent {
            AnimeJapaneseLabTheme(darkTheme = false) {
                if (preview == "pronunciation") {
                    LessonScreen(
                        uiState = PronunciationPreviewState,
                        onExit = ::finish,
                        onSubmitAnswer = {},
                        onContinue = {},
                        onRestart = {},
                        onNextBatch = {},
                        onEvaluatePronunciation = { _, _, _, _ -> },
                        onRetryPronunciation = {},
                        onResetPronunciation = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LessonCompleteContent(
                        result = PreviewResult,
                        exitLabel = "关闭预览",
                        onExit = ::finish,
                        onRestart = {},
                        onNextBatch = {},
                        modifier = Modifier.fillMaxSize(),
                        heroContent = {
                            FusionMotionHost(
                                resolution = AnimeLabFusionAssetResolver.resolve(
                                    FusionVisualKey.SessionCompleteCelebration,
                                ),
                                motionEnabled = true,
                                modifier = Modifier.fillMaxSize(),
                                fallback = {},
                            )
                        },
                    )
                }
            }
        }
    }

    private companion object {
        val PreviewResult = LessonResultUiState(
            workTitle = "Re:ゼロから始める異世界生活",
            episodeLabel = "Re:Zero · EP01",
            title = "线上综合训练 · 王都的第一段真实台词",
            completedCount = 10,
            correctCount = 9,
            totalCount = 10,
            xp = 108,
            streakDays = 12,
            accuracyPercent = 90,
            masteredContent = "9 个词句进入掌握队列，1 个语法点等待复盘",
            primaryMistakeType = "语法功能误判",
            reviewSchedule = "错题已安排到复盘队列",
            nextSuggestion = "继续下一组本集材料",
            hasNextBatch = true,
        )

        private val PronunciationSentence = ShadowingSentence(
            id = "k-on-ep01-sent-00046",
            ja = "正確には廃部寸前ね",
            reading = "せいかくにはいぶすんぜんね",
            meaningZh = "正确来说，是即将废部。",
            sourceLabel = "EP01 第 46 行",
            audioKind = AudioKind.Tts,
            sourceLineNo = 46,
        )
        private val PronunciationNode = ShadowingNode(
            id = "k-on-ep01-sent-00046-shadowing-real-evaluation",
            title = "真实跟读测评",
            prompt = "听原句，录下跟读，查看真实发音测评",
            explanation = "原句：正確には廃部寸前ね / 正确来说，是即将废部。",
            sourceLabel = PronunciationSentence.sourceLabel,
            sentence = PronunciationSentence,
            ratings = listOf("像原声", "大致跟上", "还要再练"),
            pronunciationSentenceId = FirstEnabledPronunciationSentenceId,
            audio = PromptAudio.Tts(PronunciationSentence.ja, autoPlay = false),
        )
        private val PreviewScene = ReadAirScene(
            id = "preview",
            title = "Preview",
            context = "",
            lines = listOf(DialogueLine("律", PronunciationSentence.ja, PronunciationSentence.meaningZh)),
            subtext = "",
            evidence = emptyList(),
            learningPoint = "",
        )
        val PronunciationPreviewState = LabUiState(
            deviceId = "debug-preview",
            settings = LabSettings(autoSpeak = false),
            works = listOf(WorkOption("k-on", "k-on", "K-ON!", 14)),
            episodes = listOf(EpisodeOption("k-on-ep01", "k-on", "K-ON!", 1)),
            selection = EpisodeSelection("k-on", 1),
            focus = EpisodeFocus(
                workSlug = "k-on",
                episodeNumber = 1,
                workTitle = "K-ON!",
                episodeLabel = "K-ON! · EP01",
                lessonTitle = "声线模仿",
                sectionTitle = "真实发音测评",
                guidebook = "",
                dailyGoal = 10,
                xp = 120,
                streakDays = 6,
                energy = 5,
            ),
            vocab = emptyList(),
            grammar = emptyList(),
            shadowing = listOf(PronunciationSentence),
            scenes = listOf(PreviewScene),
            selectedScene = PreviewScene,
            readAir = ReadAirTrainingState(),
            lesson = LessonEngine.start(listOf(PronunciationNode)),
            lessonMode = LessonMode.Shadowing,
        )
    }
}
