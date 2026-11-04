package com.pulsedl.mobile.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulsedl.mobile.PulseDLApplication
import com.pulsedl.mobile.data.local.DownloadTaskEntity
import com.pulsedl.mobile.data.local.TaskDao
import com.pulsedl.mobile.data.preferences.SettingsRepository
import com.pulsedl.mobile.data.preferences.UserDownloadPreferences
import com.pulsedl.mobile.domain.model.AudioFormatPreset
import com.pulsedl.mobile.domain.model.DownloadConfiguration
import com.pulsedl.mobile.domain.model.MediaMetadata
import com.pulsedl.mobile.domain.model.MediaType
import com.pulsedl.mobile.domain.model.VideoContainer
import com.pulsedl.mobile.domain.model.VideoResolutionPreset
import com.pulsedl.mobile.engine.YtDlpEngine
import com.pulsedl.mobile.service.DownloadForegroundService
import com.pulsedl.mobile.util.StorageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComposerUiState(
    val urlInput: String = "",
    val isFetchingMetadata: Boolean = false,
    val metadata: MediaMetadata? = null,
    val metadataError: String? = null,
    val selectedMediaType: MediaType = MediaType.VIDEO,
    val selectedVideoResolution: VideoResolutionPreset = VideoResolutionPreset.RES_1080P_FHD,
    val selectedVideoContainer: VideoContainer = VideoContainer.MP4,
    val selectedAudioPreset: AudioFormatPreset = AudioFormatPreset.MP3_320K,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedSubtitles: Boolean = false,
    val sponsorBlockRemove: Boolean = false,
    val enableClipTrimmer: Boolean = false,
    val clipStartSeconds: Float = 0f,
    val clipEndSeconds: Float = 180f,
    val showDhikrGateModal: Boolean = false,
    val isUpdatingYtDlp: Boolean = false,
    val statusBannerMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val ytDlpEngine: YtDlpEngine,
    private val taskDao: TaskDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComposerUiState())
    val uiState: StateFlow<ComposerUiState> = _uiState.asStateFlow()

    val allTasks: StateFlow<List<DownloadTaskEntity>> = taskDao.observeAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val preferences: StateFlow<UserDownloadPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserDownloadPreferences(
                customDirectoryDisplayPath = settingsRepository.defaultPublicDownloadsDir.absolutePath
            )
        )

    val engineReady: StateFlow<Boolean> =
        (appContext.applicationContext as? PulseDLApplication)?.engineReady
            ?: MutableStateFlow(true)

    fun onUrlChanged(newUrl: String) {
        _uiState.update {
            it.copy(
                urlInput = newUrl,
                metadataError = null,
                statusBannerMessage = null
            )
        }
    }

    fun inspectUrl(urlOverride: String? = null) {
        val targetUrl = (urlOverride ?: _uiState.value.urlInput).trim()
        if (targetUrl.isBlank()) {
            _uiState.update { it.copy(metadataError = "Please paste a valid video or audio URL first.") }
            return
        }

        _uiState.update {
            it.copy(
                urlInput = targetUrl,
                isFetchingMetadata = true,
                metadataError = null
            )
        }

        viewModelScope.launch {
            val result = ytDlpEngine.fetchMediaMetadata(targetUrl)
            result.onSuccess { meta ->
                val defaultEnd = if (meta.durationSeconds > 0) meta.durationSeconds.toFloat() else 180f
                val bestPreset = when {
                    meta.maxAvailableHeight >= 4320 -> VideoResolutionPreset.RES_8K_UHD
                    meta.maxAvailableHeight >= 2160 -> VideoResolutionPreset.RES_4K_UHD
                    meta.maxAvailableHeight >= 1440 -> VideoResolutionPreset.RES_2K_QHD
                    meta.maxAvailableHeight >= 1080 -> VideoResolutionPreset.RES_1080P_FHD
                    meta.maxAvailableHeight >= 720 -> VideoResolutionPreset.RES_720P_HD
                    else -> VideoResolutionPreset.RES_480P_SD
                }
                _uiState.update {
                    it.copy(
                        isFetchingMetadata = false,
                        metadata = meta,
                        selectedVideoResolution = bestPreset,
                        enableClipTrimmer = false,
                        clipStartSeconds = 0f,
                        clipEndSeconds = defaultEnd
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isFetchingMetadata = false,
                        metadataError = err.localizedMessage ?: "Could not inspect stream metadata. You can still start a direct download."
                    )
                }
            }
        }
    }

    fun setMediaType(mediaType: MediaType) {
        _uiState.update { it.copy(selectedMediaType = mediaType) }
    }

    fun setVideoResolution(preset: VideoResolutionPreset) {
        _uiState.update { it.copy(selectedVideoResolution = preset) }
    }

    fun setVideoContainer(container: VideoContainer) {
        _uiState.update { it.copy(selectedVideoContainer = container) }
    }

    fun setAudioPreset(preset: AudioFormatPreset) {
        _uiState.update { it.copy(selectedAudioPreset = preset) }
    }

    fun toggleEmbedThumbnail(enabled: Boolean) {
        _uiState.update { it.copy(embedThumbnail = enabled) }
    }

    fun toggleEmbedMetadata(enabled: Boolean) {
        _uiState.update { it.copy(embedMetadata = enabled) }
    }

    fun toggleEmbedSubtitles(enabled: Boolean) {
        _uiState.update { it.copy(embedSubtitles = enabled) }
    }

    fun toggleSponsorBlock(enabled: Boolean) {
        _uiState.update { it.copy(sponsorBlockRemove = enabled) }
    }

    fun toggleClipTrimmer(enabled: Boolean) {
        _uiState.update { state ->
            val maxDur = (state.metadata?.durationSeconds?.takeIf { it > 0 } ?: 600L).toFloat()
            state.copy(
                enableClipTrimmer = enabled,
                clipStartSeconds = 0f,
                clipEndSeconds = maxDur
            )
        }
    }

    fun updateClipRange(startSec: Float, endSec: Float) {
        _uiState.update {
            it.copy(
                clipStartSeconds = startSec.coerceAtLeast(0f),
                clipEndSeconds = endSec.coerceAtLeast(startSec + 1f)
            )
        }
    }

    /**
     * Triggered when user taps "Start Download". Opens the 5-Tasbih `DhikrGateDialog` first.
     */
    fun requestStartDownloadWithDhikrGate() {
        val currentUrl = _uiState.value.urlInput.trim()
        if (currentUrl.isBlank()) {
            _uiState.update { it.copy(metadataError = "Paste a media link to start downloading.") }
            return
        }
        _uiState.update { it.copy(showDhikrGateModal = true, metadataError = null) }
    }

    fun dismissDhikrGateModal() {
        _uiState.update { it.copy(showDhikrGateModal = false) }
    }

    /**
     * Invoked automatically once the user completes 5 / 5 Tasbihat in `DhikrGateDialog`.
     */
    fun onDhikrGateCompletedAndDispatch(onDispatchedCallback: (() -> Unit)? = null) {
        val state = _uiState.value
        val prefs = preferences.value
        val targetUrl = state.urlInput.trim()
        if (targetUrl.isBlank()) return

        _uiState.update { it.copy(showDhikrGateModal = false) }

        val config = DownloadConfiguration(
            url = targetUrl,
            titleOverride = state.metadata?.title,
            thumbnailUrl = state.metadata?.thumbnailUrl,
            uploader = state.metadata?.uploader,
            durationSeconds = state.metadata?.durationSeconds ?: 0L,
            mediaType = state.selectedMediaType,
            videoResolution = state.selectedVideoResolution,
            videoContainer = state.selectedVideoContainer,
            audioPreset = state.selectedAudioPreset,
            embedThumbnail = state.embedThumbnail,
            embedMetadata = state.embedMetadata,
            embedSubtitles = state.embedSubtitles,
            sponsorBlockRemove = state.sponsorBlockRemove,
            clipStartSeconds = if (state.enableClipTrimmer) state.clipStartSeconds.toLong() else null,
            clipEndSeconds = if (state.enableClipTrimmer) state.clipEndSeconds.toLong() else null,
            customOutputDirUri = prefs.customSafTreeUri,
            concurrentFragments = prefs.concurrentFragments,
            useAria2cMultiConnection = prefs.useAria2cMultiConnection
        )

        viewModelScope.launch {
            DownloadForegroundService.startOrSync(appContext)
            ytDlpEngine.enqueueDownload(config)
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Task added to parallel download queue",
                    enableClipTrimmer = false
                )
            }
            onDispatchedCallback?.invoke()
        }
    }

    /**
     * Persists the URI returned by `ActivityResultContracts.OpenDocumentTree()`.
     */
    fun onCustomDirectorySelected(treeUri: Uri?) {
        if (treeUri == null) return
        viewModelScope.launch {
            runCatching {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                appContext.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            }
            val displayPath = StorageHelper.getDisplayPathFromTreeUri(appContext, treeUri)
            settingsRepository.updateCustomDownloadLocation(treeUri.toString(), displayPath)
            _uiState.update {
                it.copy(statusBannerMessage = "Download folder updated: $displayPath")
            }
        }
    }

    fun resetToDefaultDownloadDirectory() {
        viewModelScope.launch {
            val defaultPath = settingsRepository.defaultPublicDownloadsDir.absolutePath
            settingsRepository.updateCustomDownloadLocation(null, defaultPath)
            _uiState.update {
                it.copy(statusBannerMessage = "Reverted to default folder: $defaultPath")
            }
        }
    }

    fun setMaxConcurrentTasks(count: Int) {
        viewModelScope.launch {
            settingsRepository.updateMaxConcurrentTasks(count)
        }
    }

    fun setConcurrentFragments(fragments: Int) {
        viewModelScope.launch {
            settingsRepository.updateConcurrentFragments(fragments)
        }
    }

    fun setUseAria2c(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseAria2c(enabled)
        }
    }

    fun pauseTask(taskId: String) {
        ytDlpEngine.pauseTask(taskId)
    }

    fun resumeTask(taskId: String) {
        DownloadForegroundService.startOrSync(appContext)
        ytDlpEngine.resumeTask(taskId)
    }

    fun cancelTask(taskId: String, deleteRecord: Boolean = false) {
        ytDlpEngine.cancelTask(taskId, deleteRecord)
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            taskDao.clearCompletedTasks()
        }
    }

    fun triggerOtaYtDlpUpdate() {
        _uiState.update {
            it.copy(isUpdatingYtDlp = true, statusBannerMessage = "Checking for yt-dlp extractor updates...")
        }
        viewModelScope.launch {
            val result = ytDlpEngine.updateYtDlpBinary()
            result.onSuccess { status ->
                val msg = when (status) {
                    com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.DONE ->
                        "yt-dlp engine updated to latest release!"
                    com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE ->
                        "yt-dlp engine is already up to date."
                    else -> "yt-dlp extractor check complete."
                }
                _uiState.update { it.copy(isUpdatingYtDlp = false, statusBannerMessage = msg) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isUpdatingYtDlp = false,
                        statusBannerMessage = "Update check failed: ${err.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun formatDuration(seconds: Long): String = ytDlpEngine.formatSecondsToHhMmSs(seconds)
}
