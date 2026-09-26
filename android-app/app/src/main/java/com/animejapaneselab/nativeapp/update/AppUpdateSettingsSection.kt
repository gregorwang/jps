package com.animejapaneselab.nativeapp.update

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 設定 ·「更新」group body: 1px hairline rows like the rest of V3Settings. The action is an
 * outline button — the settings screen has no ink primary.
 */
@Composable
internal fun AppUpdateSettingsSection(
    state: AppUpdateUiState,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val release = state.release
    val busy = state.phase == AppUpdatePhase.Checking ||
        state.phase == AppUpdatePhase.Downloading ||
        state.phase == AppUpdatePhase.PreparingInstall
    val newer = release != null && isNewerRelease(state.currentVersionCode, release)

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("版本", style = AjlTheme.type.body, color = colors.ink, modifier = Modifier.weight(1f))
            Text(
                "${state.currentVersionName} · ${state.currentVersionCode}",
                style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                color = colors.ink2,
                maxLines = 1,
            )
        }
        Hairline()
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                updateStatusText(state),
                style = AjlTheme.type.body,
                color = if (state.phase == AppUpdatePhase.Error) colors.bad else colors.ink2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (state.phase == AppUpdatePhase.Checking || state.phase == AppUpdatePhase.PreparingInstall) {
                LoadingDots(delayMillis = 0)
            }
        }
        if (state.phase == AppUpdatePhase.Downloading) {
            ProgressLine(
                progress = (state.progressPercent ?: 0).coerceIn(0, 100) / 100f,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                contentDescription = updateStatusText(state),
            )
        }
        if (newer && release != null) {
            Hairline()
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(
                        R.string.app_update_release_summary,
                        release.versionName,
                        Formatter.formatShortFileSize(context, release.sizeBytes),
                    ),
                    style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                    color = colors.ink,
                )
                if (release.releaseNotes.isNotBlank()) {
                    Text(
                        release.releaseNotes,
                        style = AjlTheme.type.caption.copy(lineHeight = 20.sp),
                        color = colors.ink2,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        OutlineButton(
            text = updateButtonText(state),
            onClick = onPrimaryAction,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@Composable
private fun updateStatusText(state: AppUpdateUiState): String = when (state.phase) {
    AppUpdatePhase.Idle -> stringResource(R.string.app_update_idle)
    AppUpdatePhase.Checking -> stringResource(R.string.app_update_checking)
    AppUpdatePhase.UpToDate -> stringResource(R.string.app_update_up_to_date)
    AppUpdatePhase.Available -> stringResource(R.string.app_update_available)
    AppUpdatePhase.Downloading -> state.progressPercent?.let { progress ->
        stringResource(R.string.app_update_downloading_progress, progress)
    } ?: stringResource(R.string.app_update_downloading)
    AppUpdatePhase.ReadyToInstall -> stringResource(R.string.app_update_ready)
    AppUpdatePhase.AwaitingInstallPermission -> stringResource(R.string.app_update_permission_required)
    AppUpdatePhase.PreparingInstall -> stringResource(R.string.app_update_preparing_install)
    AppUpdatePhase.Error -> stringResource((state.error ?: AppUpdateError.Unknown).messageResource())
}

@Composable
private fun updateButtonText(state: AppUpdateUiState): String = when (state.phase) {
    AppUpdatePhase.Idle,
    AppUpdatePhase.UpToDate,
    AppUpdatePhase.Error,
    -> stringResource(R.string.app_update_check_button)

    AppUpdatePhase.Checking -> stringResource(R.string.app_update_checking_button)
    AppUpdatePhase.Available -> stringResource(R.string.app_update_download_button)
    AppUpdatePhase.Downloading -> stringResource(R.string.app_update_downloading_button)
    AppUpdatePhase.ReadyToInstall -> stringResource(R.string.app_update_install_button)
    AppUpdatePhase.AwaitingInstallPermission -> stringResource(R.string.app_update_permission_button)
    AppUpdatePhase.PreparingInstall -> stringResource(R.string.app_update_preparing_button)
}

@StringRes
private fun AppUpdateError.messageResource(): Int = when (this) {
    AppUpdateError.NoRelease -> R.string.app_update_error_no_release
    AppUpdateError.Network -> R.string.app_update_error_network
    AppUpdateError.InvalidManifest -> R.string.app_update_error_manifest
    AppUpdateError.DownloadFailed -> R.string.app_update_error_download
    AppUpdateError.StorageUnavailable -> R.string.app_update_error_storage
    AppUpdateError.IntegrityCheckFailed -> R.string.app_update_error_integrity
    AppUpdateError.PackageMismatch -> R.string.app_update_error_package
    AppUpdateError.SignatureMismatch -> R.string.app_update_error_signature
    AppUpdateError.InstallPermissionRequired -> R.string.app_update_permission_required
    AppUpdateError.InstallerUnavailable -> R.string.app_update_error_installer
    AppUpdateError.Unknown -> R.string.app_update_error_unknown
}
