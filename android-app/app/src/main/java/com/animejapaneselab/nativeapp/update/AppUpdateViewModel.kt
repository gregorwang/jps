package com.animejapaneselab.nativeapp.update

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animejapaneselab.nativeapp.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppUpdatePhase {
    Idle,
    Checking,
    UpToDate,
    Available,
    Downloading,
    ReadyToInstall,
    AwaitingInstallPermission,
    PreparingInstall,
    Error,
}

data class AppUpdateUiState(
    val currentVersionCode: Long = BuildConfig.VERSION_CODE.toLong(),
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val phase: AppUpdatePhase = AppUpdatePhase.Idle,
    val release: AppUpdateRelease? = null,
    val progressPercent: Int? = null,
    val error: AppUpdateError? = null,
)

internal sealed interface AppUpdateEffect {
    data class OpenUnknownSourcesSettings(val intent: Intent) : AppUpdateEffect
    data class OpenSystemInstaller(val intent: Intent) : AppUpdateEffect
}

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val client = AppUpdateClient()
    private val manager = AppUpdateManager(application)
    private val _uiState = MutableStateFlow(AppUpdateUiState())
    private val _effects = Channel<AppUpdateEffect>(Channel.BUFFERED)
    private var pollingJob: Job? = null

    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()
    internal val effects: Flow<AppUpdateEffect> = _effects.receiveAsFlow()

    fun onScreenVisible() {
        refreshStoredDownload()
    }

    fun onPrimaryAction() {
        when (_uiState.value.phase) {
            AppUpdatePhase.Idle,
            AppUpdatePhase.UpToDate,
            AppUpdatePhase.Error,
            -> checkForUpdate()

            AppUpdatePhase.Available -> startDownload()
            AppUpdatePhase.ReadyToInstall,
            AppUpdatePhase.AwaitingInstallPermission,
            -> requestInstall()

            AppUpdatePhase.Checking,
            AppUpdatePhase.Downloading,
            AppUpdatePhase.PreparingInstall,
            -> Unit
        }
    }

    fun onInstallPermissionResult() {
        if (_uiState.value.phase != AppUpdatePhase.AwaitingInstallPermission) return
        if (manager.canRequestPackageInstalls()) {
            requestInstall()
        } else {
            _uiState.update { current ->
                current.copy(
                    phase = AppUpdatePhase.AwaitingInstallPermission,
                    error = AppUpdateError.InstallPermissionRequired,
                )
            }
        }
    }

    fun onInstallerReturned() {
        _uiState.update { current ->
            if (current.release != null) {
                current.copy(phase = AppUpdatePhase.ReadyToInstall, error = null)
            } else {
                current
            }
        }
    }

    fun onExternalLaunchFailed() {
        _uiState.update { current ->
            current.copy(phase = AppUpdatePhase.Error, error = AppUpdateError.InstallerUnavailable)
        }
    }

    private fun checkForUpdate() {
        pollingJob?.cancel()
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    phase = AppUpdatePhase.Checking,
                    release = null,
                    progressPercent = null,
                    error = null,
                )
            }
            try {
                val release = client.fetchLatest(BuildConfig.APP_UPDATE_BASE_URL)
                _uiState.update { current ->
                    if (isNewerRelease(current.currentVersionCode, release)) {
                        current.copy(phase = AppUpdatePhase.Available, release = release)
                    } else {
                        current.copy(phase = AppUpdatePhase.UpToDate, release = release)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: AppUpdateException) {
                showError(error.reason)
            } catch (_: Exception) {
                showError(AppUpdateError.Unknown)
            }
        }
    }

    private fun startDownload() {
        val release = _uiState.value.release ?: return
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    phase = AppUpdatePhase.Downloading,
                    progressPercent = 0,
                    error = null,
                )
            }
            try {
                applySnapshot(manager.startDownload(release))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: AppUpdateException) {
                showError(error.reason, release)
            } catch (_: Exception) {
                showError(AppUpdateError.DownloadFailed, release)
            }
        }
    }

    private fun refreshStoredDownload() {
        viewModelScope.launch {
            try {
                applySnapshot(manager.readDownloadSnapshot(_uiState.value.currentVersionCode))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (_uiState.value.phase == AppUpdatePhase.Downloading) {
                    showError(AppUpdateError.DownloadFailed, _uiState.value.release)
                }
            }
        }
    }

    private fun applySnapshot(snapshot: AppUpdateDownloadSnapshot) {
        when (snapshot) {
            AppUpdateDownloadSnapshot.None -> Unit
            is AppUpdateDownloadSnapshot.InProgress -> {
                _uiState.update { current ->
                    current.copy(
                        phase = AppUpdatePhase.Downloading,
                        release = snapshot.release,
                        progressPercent = snapshot.progressPercent,
                        error = null,
                    )
                }
                startPolling()
            }

            is AppUpdateDownloadSnapshot.Ready -> {
                pollingJob?.cancel()
                _uiState.update { current ->
                    current.copy(
                        phase = AppUpdatePhase.ReadyToInstall,
                        release = snapshot.release,
                        progressPercent = 100,
                        error = null,
                    )
                }
            }

            is AppUpdateDownloadSnapshot.Failed -> {
                pollingJob?.cancel()
                showError(snapshot.error, snapshot.release)
            }
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(DownloadPollIntervalMillis)
                val snapshot = try {
                    manager.readDownloadSnapshot(_uiState.value.currentVersionCode)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    showError(AppUpdateError.DownloadFailed, _uiState.value.release)
                    return@launch
                }
                applySnapshot(snapshot)
                if (snapshot !is AppUpdateDownloadSnapshot.InProgress) return@launch
            }
        }
    }

    private fun requestInstall() {
        val release = _uiState.value.release ?: return
        viewModelScope.launch {
            try {
                if (!manager.canRequestPackageInstalls()) {
                    _uiState.update { current ->
                        current.copy(
                            phase = AppUpdatePhase.AwaitingInstallPermission,
                            error = AppUpdateError.InstallPermissionRequired,
                        )
                    }
                    _effects.send(AppUpdateEffect.OpenUnknownSourcesSettings(manager.buildUnknownSourcesIntent()))
                    return@launch
                }

                _uiState.update { current ->
                    current.copy(phase = AppUpdatePhase.PreparingInstall, error = null)
                }
                val intent = manager.buildInstallIntent(release)
                _effects.send(AppUpdateEffect.OpenSystemInstaller(intent))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: AppUpdateException) {
                showError(error.reason, release)
            } catch (_: Exception) {
                showError(AppUpdateError.InstallerUnavailable, release)
            }
        }
    }

    private fun showError(error: AppUpdateError, release: AppUpdateRelease? = null) {
        _uiState.update { current ->
            current.copy(
                phase = AppUpdatePhase.Error,
                release = release,
                progressPercent = null,
                error = error,
            )
        }
    }

    private companion object {
        const val DownloadPollIntervalMillis = 1_000L
    }
}
