package com.animejapaneselab.nativeapp.ui.screens.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.AttendanceCard
import com.animejapaneselab.nativeapp.ui.design.Avatar
import com.animejapaneselab.nativeapp.ui.design.EnrolledChip
import com.animejapaneselab.nativeapp.ui.design.LineRow
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.ProvideWorkTheme

/**
 * 换一部番: どの番を観る？ — one row per season (character avatar in the work's colours, 在籍中 on
 * the enrolled one; Re:ゼロ 一期/二期/三期 = エミリア/ベアトリス/スバル), then the 出席カード of
 * the enrolled season. Stamps sit still here — the 済 animation only plays when an episode is
 * actually finished.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseSwitcherSheet(
    model: CourseScreenModel,
    onDismiss: () -> Unit,
    onSeasonSelected: (SwitcherRow) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
) {
    AjlBottomSheet(onDismissRequest = onDismiss, title = "どの番を観る？", gloss = "换一部番") {
        Column {
            model.switcher.forEach { row ->
                ProvideWorkTheme(row.workSlug) {
                    SwitcherLine(row, onClick = { if (row.enrolled) onDismiss() else onSeasonSelected(row) })
                }
            }
        }
        AttendanceCard(
            cells = model.attendance,
            meta = model.attendanceMeta,
            onSelect = onEpisodeSelected,
        )
    }
}

@Composable
private fun SwitcherLine(row: SwitcherRow, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    LineRow(
        onClick = onClick,
        minHeight = 60.dp,
        leading = { Avatar(row.season.character, size = 44.dp, highlighted = row.enrolled) },
        trailing = if (row.enrolled) {
            { EnrolledChip() }
        } else {
            null
        },
    ) {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                row.season.title,
                style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, lineHeight = 22.sp),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(row.meta, style = AjlTheme.type.meta, color = colors.ink3, maxLines = 1)
        }
    }
}
