package com.animejapaneselab.nativeapp.ui.completion

import com.animejapaneselab.nativeapp.data.EpisodeSelection
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.SampleLearningRepository
import com.animejapaneselab.nativeapp.domain.LessonEngine
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.ReadAirTrainingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonResultUiStateTest {
    private val repository = SampleLearningRepository()

    @Test
    fun resultSummarizesLessonCompletion() {
        val content = repository.content(EpisodeSelection("k-on", 1), LessonMode.Mixed)
        val uiState = LabUiState(
            deviceId = "test-device",
            settings = LabSettings(),
            works = repository.works(),
            episodes = repository.episodes("k-on"),
            selection = EpisodeSelection("k-on", 1),
            focus = content.focus,
            vocab = content.vocab,
            grammar = content.grammar,
            shadowing = content.shadowing,
            scenes = content.scenes,
            selectedScene = content.scenes.first(),
            readAir = ReadAirTrainingState(),
            lesson = LessonEngine.start(content.lessonNodes).copy(
                index = content.lessonNodes.size,
                answered = 10,
                correct = 7,
            ),
            lessonMode = LessonMode.Mixed,
            hasNextLessonBatch = true,
            sessionXp = 84,
        )

        val result = uiState.toLessonResultUiState()

        assertEquals(10, result.completedCount)
        assertEquals(7, result.correctCount)
        assertEquals(70, result.accuracyPercent)
        assertEquals(84, result.xp)
        assertEquals(content.focus.workTitle, result.workTitle)
        assertEquals(content.focus.episodeLabel, result.episodeLabel)
        assertTrue(result.hasNextBatch)
        assertTrue(result.reviewSchedule.contains("复盘"))
    }
}
