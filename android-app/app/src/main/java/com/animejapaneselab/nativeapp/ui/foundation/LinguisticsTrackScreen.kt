package com.animejapaneselab.nativeapp.ui.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing

@Composable
fun LinguisticsTrackScreen(
    selectedTrack: LinguisticsTrack,
    onTrackSelected: (LinguisticsTrack) -> Unit,
    animeCorpusContent: @Composable () -> Unit,
    foundationContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LabSpacing.Screen,
                        vertical = LabSpacing.Small,
                    ),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            ) {
                Text(
                    text = stringResource(R.string.foundation_track_selector_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LinguisticsTrackChip(
                        label = stringResource(R.string.foundation_track_foundation),
                        selected = selectedTrack == LinguisticsTrack.Foundation,
                        onClick = { onTrackSelected(LinguisticsTrack.Foundation) },
                        modifier = Modifier.weight(1f),
                    )
                    LinguisticsTrackChip(
                        label = stringResource(R.string.foundation_track_anime),
                        selected = selectedTrack == LinguisticsTrack.AnimeCorpus,
                        onClick = { onTrackSelected(LinguisticsTrack.AnimeCorpus) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (selectedTrack) {
                LinguisticsTrack.AnimeCorpus -> animeCorpusContent()
                LinguisticsTrack.Foundation -> foundationContent()
            }
        }
    }
}

@Composable
private fun LinguisticsTrackChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedDescription = stringResource(R.string.foundation_state_selected)
    val unselectedDescription = stringResource(R.string.foundation_state_not_selected)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1,
            )
        },
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
                stateDescription = if (selected) selectedDescription else unselectedDescription
            },
    )
}
