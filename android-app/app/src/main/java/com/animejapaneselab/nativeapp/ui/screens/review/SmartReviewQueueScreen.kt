package com.animejapaneselab.nativeapp.ui.screens.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.domain.SmartReviewPlan

// STAGE-2 STUB (task package B): the signature is the contract LabApp calls. Replace the body
// with the v3 implementation; keep the signature (new params only with defaults).

/** Smart review queue (secondary screen). */
@Composable
fun SmartReviewQueueScreen(
    plan: SmartReviewPlan,
    onBack: () -> Unit,
    onStartItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    com.animejapaneselab.nativeapp.ui.screens.SmartReviewQueueScreen(plan, onBack, onStartItem, modifier)
}
