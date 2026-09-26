package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingScreen
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingState
import com.animejapaneselab.nativeapp.ui.screens.FoundationActions

// STAGE-2 STUB (task package D2): the signature is the contract package C calls. Replace the
// body with the v3 implementation; keep the signature (new params only with defaults).

/**
 * 第二巻 基礎 answering, same visual language as ReadAirSession. Hosted by package C's 言語学
 * screen, which decides when it is shown and handles [onExit].
 */
@Composable
fun FoundationSession(
    state: FoundationTrainingState,
    actions: FoundationActions,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FoundationTrainingScreen(
        state = state,
        onRefresh = actions.onRefresh,
        onPackSelected = actions.onPackSelected,
        onDomainSelected = actions.onDomainSelected,
        onTopicSelected = actions.onTopicSelected,
        onStageSelected = actions.onStageSelected,
        onAnswerSelected = actions.onAnswerSelected,
        onPrevious = actions.onPrevious,
        onNext = actions.onNext,
        onRestart = actions.onRestart,
        modifier = modifier,
    )
}
