package com.animejapaneselab.nativeapp.update

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton

@Composable
internal fun AppUpdateSettingsSection(
    state: AppUpdateUiState,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val release = state.release
    val busy = state.phase == AppUpdatePhase.Checking ||
        state.phase == AppUpdatePhase.Downloading ||
        state.phase == AppUpdatePhase.PreparingInstall

    LabCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_update_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(
                        R.string.app_update_current_version,
                        state.currentVersionName,
                        state.currentVersionCode,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Text(
            text = updateStatusText(state),
            color = if (state.phase == AppUpdatePhase.Error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        if (state.phase == AppUpdatePhase.Downloading) {
            LinearProgressIndicator(
                progress = { (state.progressPercent ?: 0).coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (release != null && isNewerRelease(state.currentVersionCode, release)) {
            Text(
                text = stringResource(
                    R.string.app_update_release_summary,
                    release.versionName,
                    Formatter.formatShortFileSize(context, release.sizeBytes),
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (release.releaseNotes.isNotBlank()) {
                Text(
                    text = release.releaseNotes,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.app_update_security_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        PrimaryButton(
            text = updateButtonText(state),
            onClick = onPrimaryAction,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
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
