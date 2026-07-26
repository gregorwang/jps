package com.animejapaneselab.nativeapp.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

@Composable
fun AppUpdateRoute(
    modifier: Modifier = Modifier,
    viewModel: AppUpdateViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestPhase by rememberUpdatedState(state.phase)
    val lifecycleOwner = LocalLifecycleOwner.current
    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onInstallPermissionResult()
    }
    val installerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onInstallerReturned()
    }

    LaunchedEffect(viewModel) {
        viewModel.onScreenVisible()
        viewModel.effects.collect { effect ->
            try {
                when (effect) {
                    is AppUpdateEffect.OpenUnknownSourcesSettings -> unknownSourcesLauncher.launch(effect.intent)
                    is AppUpdateEffect.OpenSystemInstaller -> installerLauncher.launch(effect.intent)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                viewModel.onExternalLaunchFailed()
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && latestPhase == AppUpdatePhase.AwaitingInstallPermission) {
                viewModel.onInstallPermissionResult()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AppUpdateSettingsSection(
        state = state,
        onPrimaryAction = viewModel::onPrimaryAction,
        modifier = modifier,
    )
}
