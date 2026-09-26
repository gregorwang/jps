package com.animejapaneselab.nativeapp.ui.screens

import com.animejapaneselab.nativeapp.data.FoundationDomain
import com.animejapaneselab.nativeapp.data.FoundationStage
import com.animejapaneselab.nativeapp.ui.ReadAirMode

/**
 * Stage-2 contract between task packages (read-only for packages; owned by the main agent).
 * Callback bundles wired by LabApp to LabViewModel so the 学ぶ / 言語学 screens and the
 * session screens agree on one shape.
 */

/** 第一巻 アニメの台詞 — read-air (anime corpus) home actions. */
data class ReadAirHomeActions(
    val onRefresh: () -> Unit,
    val onWorkSelected: (String) -> Unit,
    val onDomainSelected: (String) -> Unit,
    val onQuestionTypeSelected: (String) -> Unit,
    val onDifficultySelected: (String) -> Unit,
    val onTopicSelected: (String) -> Unit,
    val onEpisodeSelected: (Int?) -> Unit,
    val onModeSelected: (ReadAirMode) -> Unit,
    val onResetFilters: () -> Unit,
    val onResetQueue: () -> Unit,
    /** Starts the read-air session (activeSession = ReadAir → ReadAirSessionScreen). */
    val onStartSession: () -> Unit,
    val onBrowseAnswer: (String, String) -> Unit,
)

/** 第二巻 基礎 — foundation question bank actions (catalogue + in-place answering). */
data class FoundationActions(
    val onRefresh: () -> Unit,
    val onPackSelected: (String) -> Unit,
    val onDomainSelected: (FoundationDomain?) -> Unit,
    val onTopicSelected: (String?) -> Unit,
    val onStageSelected: (FoundationStage?) -> Unit,
    val onAnswerSelected: (String) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onRestart: () -> Unit,
)
