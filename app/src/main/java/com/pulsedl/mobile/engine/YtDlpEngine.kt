package com.pulsedl.mobile.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pulsedl.mobile.data.local.DownloadTaskEntity
import com.pulsedl.mobile.data.local.TaskDao
import com.pulsedl.mobile.data.preferences.SettingsRepository
import com.pulsedl.mobile.domain.model.DownloadConfiguration
import com.pulsedl.mobile.domain.model.DownloadStatus
import com.pulsedl.mobile.domain.model.FormatOptionSummary
import com.pulsedl.mobile.domain.model.MediaMetadata
import com.pulsedl.mobile.domain.model.MediaType
import com.pulsedl.mobile.util.StorageHelper
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val settingsRepository: SettingsRepository
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Dynamic Semaphore for task-level parallelism (1 to 3 simultaneous video downloads)
    @Volatile
    private var currentMaxConcurrency: Int = 2

    @Volatile
    private var taskSemaphore: Semaphore = Semaphore(currentMaxConcurrency)

    // Active coroutine jobs indexed by taskId for instant Pause / Cancel
    private val activeJobs = ConcurrentHashMap<String, Job>()

    // Track whether a task was paused vs canceled when destroying its process
    private val pauseRequestedSet = ConcurrentHashMap.newKeySet<String>()

    init {
        engineScope.launch {
            settingsRepository.preferencesFlow.collect { prefs ->
                if (prefs.maxConcurrentTasks != currentMaxConcurrency) {
                    currentMaxConcurrency = prefs.maxConcurrentTasks.coerceIn(1, 3)
                    taskSemaphore = Semaphore(currentMaxConcurrency)
                    Log.i(TAG, "Updated task-level concurrency semaphore to $currentMaxConcurrency permits")
                }
            }
        }
    }

    /**
     * Extracts comprehensive metadata, available resolutions (up to 8K 4320p), 60fps availability,
     * duration, and thumbnail URL for any supported URL (YouTube, TikTok, Instagram, X, Vimeo, SoundCloud, etc.).
     */
    suspend fun fetchMediaMetadata(url: String): Result<MediaMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val request = YoutubeDLRequest(url.trim()).apply {
                addOption("--no-playlist")
                addOption("--socket-timeout", "20")
                addOption("--no-warnings")
            }

            val videoInfo = YoutubeDL.getInstance().getInfo(request)
            val formats = videoInfo.formats ?: emptyList()

            var maxAvailableHeight = 0
            var supports60Fps = false
            val summaries = mutableListOf<FormatOptionSummary>()

            for (fmt in formats) {
                val h = fmt.height
                if (h > maxAvailableHeight) {
                    maxAvailableHeight = h
                }
                if (fmt.fps >= 50) {
                    supports60Fps = true
                }
                if (h >= 360 || (fmt.acodec != "none" && fmt.vcodec == "none")) {
                    summaries.add(
                        FormatOptionSummary(
                            formatId = fmt.formatId ?: "",
                            extension = fmt.ext ?: "",
                            resolution = if (h > 0) "${h}p" else "Audio Only",
                            fps = if (fmt.fps > 0) fmt.fps.toInt() else null,
                            vcodec = fmt.vcodec,
                            acodec = fmt.acodec,
                            fileSizeApproxBytes = fmt.fileSize.takeIf { it > 0 } ?: fmt.fileSizeApprox.takeIf { it > 0 }
                        )
                    )
                }
            }

            MediaMetadata(
                id = videoInfo.id ?: UUID.randomUUID().toString(),
                url = url.trim(),
                title = videoInfo.title?.takeIf { it.isNotBlank() } ?: "Untitled Media Stream",
                uploader = videoInfo.uploader?.takeIf { it.isNotBlank() }
                    ?: videoInfo.extractorKey
                    ?: "Media Source",
                durationSeconds = videoInfo.duration.toLong().coerceAtLeast(0L),
                thumbnailUrl = videoInfo.thumbnail,
                webpageUrl = videoInfo.webpageUrl ?: url.trim(),
                extractorKey = videoInfo.extractorKey ?: "Generic",
                maxAvailableHeight = if (maxAvailableHeight > 0) maxAvailableHeight else 1080,
                supports60Fps = supports60Fps,
                availableFormatsSummary = summaries
            )
        }
    }

    /**
     * Enqueues a new download task in Room and schedules it on the parallel Semaphore worker pool.
     * Returns the newly generated taskId.
     */
    suspend fun enqueueDownload(
        config: DownloadConfiguration,
        onTelemetryUpdated: () -> Unit = {}
    ): String {
        val prefs = settingsRepository.preferencesFlow.first()
        val taskId = UUID.randomUUID().toString()

        val safUri = config.customOutputDirUri ?: prefs.customSafTreeUri
        val resolvedDir = if (!safUri.isNullOrBlank()) {
            StorageHelper.resolvePosixPathFromTreeUri(context, Uri.parse(safUri))
                ?: StorageHelper.getDefaultPulseDlDirectory(config.customSubfolderName).absolutePath
        } else {
            StorageHelper.getDefaultPulseDlDirectory(config.customSubfolderName).absolutePath
        }

        val entity = DownloadTaskEntity(
            taskId = taskId,
            url = config.url.trim(),
            title = config.titleOverride?.takeIf { it.isNotBlank() } ?: "Preparing stream...",
            uploader = config.uploader ?: "PulseDL Engine",
            thumbnailUrl = config.thumbnailUrl,
            durationSeconds = config.durationSeconds,
            mediaType = config.mediaType,
            videoResolution = config.videoResolution,
            videoContainer = config.videoContainer,
            audioPreset = config.audioPreset,
            status = DownloadStatus.QUEUED,
            progressPercent = 0f,
            speedText = "Queued",
            etaText = "--:--",
            downloadedBytes = 0L,
            totalBytes = 0L,
            clipStartSeconds = config.clipStartSeconds,
            clipEndSeconds = config.clipEndSeconds,
            embedThumbnail = config.embedThumbnail,
            embedMetadata = config.embedMetadata,
            embedSubtitles = config.embedSubtitles,
            sponsorBlockRemove = config.sponsorBlockRemove,
            concurrentFragments = config.concurrentFragments.coerceIn(1, 8),
            useAria2c = config.useAria2cMultiConnection,
            targetDirectoryPath = resolvedDir,
            targetSafTreeUri = safUri,
            outputFilePath = null,
            errorMessage = null,
            lastLogLine = "Waiting for parallel slot...",
            createdAtEpochMs = System.currentTimeMillis(),
            completedAtEpochMs = null
        )

        taskDao.upsertTask(entity)
        dispatchWorkerForTask(taskId, onTelemetryUpdated)
        return taskId
    }

    /**
     * Dispatches or resumes a download task inside the Semaphore-controlled coroutine pool.
     */
    fun dispatchWorkerForTask(taskId: String, onTelemetryUpdated: () -> Unit = {}) {
        if (activeJobs[taskId]?.isActive == true) return

        val job = engineScope.launch {
            taskSemaphore.withPermit {
                executeDownloadInternal(taskId, onTelemetryUpdated)
            }
        }
        activeJobs[taskId] = job
        job.invokeOnCompletion {
            activeJobs.remove(taskId)
            onTelemetryUpdated()
        }
    }

    private suspend fun executeDownloadInternal(
        taskId: String,
        onTelemetryUpdated: () -> Unit
    ) {
        val task = taskDao.getTaskById(taskId) ?: return
        pauseRequestedSet.remove(taskId)

        val targetDir = File(task.targetDirectoryPath).apply {
            if (!exists()) mkdirs()
        }

        // If title wasn't fetched before queueing, resolve quick metadata first
        var currentTask = task
        if (currentTask.title == "Preparing stream...") {
            taskDao.updateLiveTelemetry(
                taskId = taskId,
                status = DownloadStatus.FETCHING_METADATA,
                progressPercent = 0f,
                speedText = "Inspecting...",
                etaText = "--:--",
                lastLogLine = "Extracting stream manifest..."
            )
            onTelemetryUpdated()

            fetchMediaMetadata(currentTask.url).onSuccess { meta ->
                currentTask = currentTask.copy(
                    title = meta.title,
                    uploader = meta.uploader,
                    thumbnailUrl = meta.thumbnailUrl ?: currentTask.thumbnailUrl,
                    durationSeconds = meta.durationSeconds
                )
                taskDao.upsertTask(currentTask)
            }
        }

        val request = buildYoutubeDLRequest(currentTask, targetDir)

        taskDao.updateLiveTelemetry(
            taskId = taskId,
            status = DownloadStatus.DOWNLOADING,
            progressPercent = currentTask.progressPercent.coerceAtLeast(0f),
            speedText = "Starting...",
            etaText = "--:--",
            lastLogLine = "Initializing multi-fragment pipeline..."
        )
        onTelemetryUpdated()

        try {
            var lastDbUpdateMs = 0L

            YoutubeDL.getInstance().execute(
                request,
                taskId
            ) { progress, etaInSeconds, line ->
                val now = System.currentTimeMillis()
                // Throttle Room DB writes to ~4 updates/sec for smooth UI and Notification updates
                if (now - lastDbUpdateMs >= 250L || progress >= 99.5f) {
                    lastDbUpdateMs = now
                    val parsedSpeed = parseSpeedFromLogLine(line)
                    val formattedEta = formatEtaSeconds(etaInSeconds)
                    val isMuxing = line.contains("[Merger]", ignoreCase = true) ||
                        line.contains("[ExtractAudio]", ignoreCase = true) ||
                        line.contains("[EmbedThumbnail]", ignoreCase = true) ||
                        line.contains("ffmpeg", ignoreCase = true)

                    val newStatus = if (isMuxing) {
                        DownloadStatus.MUXING_FFMPEG
                    } else {
                        DownloadStatus.DOWNLOADING
                    }

                    engineScope.launch {
                        taskDao.updateLiveTelemetry(
                            taskId = taskId,
                            status = newStatus,
                            progressPercent = progress.coerceIn(0f, 100f),
                            speedText = if (isMuxing) "FFmpeg Muxing" else parsedSpeed,
                            etaText = if (isMuxing) "Finalizing" else formattedEta,
                            lastLogLine = line.take(180)
                        )
                        onTelemetryUpdated()
                    }
                }
            }

            // Locate newest created media file in targetDir matching this download
            val downloadedFile = findLatestDownloadedFile(targetDir, currentTask.createdAtEpochMs)
            val finalOutputLocation = if (downloadedFile != null) {
                StorageHelper.copyFileToSafTreeIfNeeded(
                    context = context,
                    sourceFile = downloadedFile,
                    safTreeUriString = currentTask.targetSafTreeUri
                )
            } else {
                targetDir.absolutePath
            }

            taskDao.markTaskCompleted(
                taskId = taskId,
                status = DownloadStatus.COMPLETED,
                outputFilePath = finalOutputLocation,
                completedAtEpochMs = System.currentTimeMillis()
            )
            onTelemetryUpdated()
        } catch (e: CancellationException) {
            handleProcessTermination(taskId, "Paused by user", onTelemetryUpdated)
        } catch (e: Exception) {
            if (pauseRequestedSet.remove(taskId)) {
                taskDao.markTaskFailedOrPaused(
                    taskId = taskId,
                    status = DownloadStatus.PAUSED,
                    errorMessage = "Paused — tap Resume to continue from partial file"
                )
            } else {
                Log.e(TAG, "Download failed for taskId=$taskId", e)
                taskDao.markTaskFailedOrPaused(
                    taskId = taskId,
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download pipeline error"
                )
            }
            onTelemetryUpdated()
        }
    }

    /**
     * Constructs the full `YoutubeDLRequest` with:
     * - Explicit 8K/4K/QHD/1080p60 video selector (`bestvideo+bestaudio`) + FFmpeg merge
     * - Lossless FLAC / 320kbps MP3 / AAC / Opus extraction with embedded cover art & ID3 tags
     * - Visual timeline clip trimmer (`--download-sections "*HH:MM:SS-HH:MM:SS"`)
     * - Dual-layer concurrency (`--concurrent-fragments` + `aria2c` 8-connection acceleration)
     * - Resumable partial streams (`--continue`)
     */
    private fun buildYoutubeDLRequest(
        task: DownloadTaskEntity,
        targetDir: File
    ): YoutubeDLRequest {
        return YoutubeDLRequest(task.url).apply {
            addOption("--no-mtime")
            addOption("--continue")
            addOption("--no-playlist")
            addOption("--newline")
            addOption("--restrict-filenames")

            // Output filename template inside target directory
            val outputTemplate = File(targetDir, "%(title).120B [%(id)s].%(ext)s").absolutePath
            addOption("-o", outputTemplate)

            // Stream-Level Concurrency: Parallel DASH/HLS fragment downloading
            addOption("--concurrent-fragments", task.concurrentFragments.coerceIn(1, 8).toString())

            // Optional Aria2c Multi-Connection Downloader (disabled when clipping sections since FFmpeg handles sections)
            if (task.useAria2c && !task.isClipTrimmed) {
                addOption("--downloader", "libaria2c.so")
                addOption(
                    "--downloader-args",
                    "aria2c:-x 8 -s 8 -k 1M --file-allocation=none --summary-interval=1"
                )
            }

            // Media Type Specific Pipeline: High-Res Video vs High-Fidelity Audio
            when (task.mediaType) {
                MediaType.VIDEO -> {
                    val h = task.videoResolution.maxHeight
                    val fpsClause = if (task.videoResolution.prefer60Fps) "[fps<=?60]" else ""
                    val formatSelector =
                        "bestvideo[height<=$h]$fpsClause+bestaudio/best[height<=$h]/best"
                    addOption("-f", formatSelector)
                    addOption("--merge-output-format", task.videoContainer.extension)
                }

                MediaType.AUDIO -> {
                    addOption("-x")
                    addOption("--audio-format", task.audioPreset.ytDlpCodec)
                    addOption("--audio-quality", task.audioPreset.qualityArg)
                }
            }

            // Cover Art & Metadata Embedding via FFmpeg
            if (task.embedThumbnail) {
                addOption("--embed-thumbnail")
                addOption("--convert-thumbnails", "jpg")
            }
            if (task.embedMetadata) {
                addOption("--add-metadata")
            }
            if (task.embedSubtitles && task.mediaType == MediaType.VIDEO) {
                addOption("--embed-subs")
                addOption("--sub-langs", "all,-live_chat")
            }
            if (task.sponsorBlockRemove) {
                addOption("--sponsorblock-remove", "sponsor,intro,outro,selfpromo")
            }

            // Visual Timeline Clip Trimmer (--download-sections "*HH:MM:SS-HH:MM:SS")
            if (task.isClipTrimmed) {
                val startFormatted = formatSecondsToHhMmSs(task.clipStartSeconds ?: 0L)
                val endFormatted = formatSecondsToHhMmSs(task.clipEndSeconds ?: 0L)
                addOption("--download-sections", "*$startFormatted-$endFormatted")
                addOption("--force-keyframes-at-cuts")
            }
        }
    }

    /**
     * Pauses a running task by destroying its underlying yt-dlp process while preserving `.part` files
     * so tapping Resume continues from the exact byte offset via `--continue`.
     */
    fun pauseTask(taskId: String, onTelemetryUpdated: () -> Unit = {}) {
        pauseRequestedSet.add(taskId)
        YoutubeDL.getInstance().destroyProcessById(taskId)
        activeJobs.remove(taskId)?.cancel()
        engineScope.launch {
            taskDao.markTaskFailedOrPaused(
                taskId = taskId,
                status = DownloadStatus.PAUSED,
                errorMessage = "Paused — tap Resume to continue"
            )
            onTelemetryUpdated()
        }
    }

    /**
     * Resumes a paused or failed task using `--continue`.
     */
    fun resumeTask(taskId: String, onTelemetryUpdated: () -> Unit = {}) {
        engineScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            taskDao.updateLiveTelemetry(
                taskId = taskId,
                status = DownloadStatus.QUEUED,
                progressPercent = task.progressPercent,
                speedText = "Resuming...",
                etaText = "--:--",
                lastLogLine = "Re-entering parallel download queue..."
            )
            onTelemetryUpdated()
            dispatchWorkerForTask(taskId, onTelemetryUpdated)
        }
    }

    /**
     * Cancels a task and cleans up any `.part` or `.ytdl` temporary files.
     */
    fun cancelTask(taskId: String, deleteRecord: Boolean = false, onTelemetryUpdated: () -> Unit = {}) {
        pauseRequestedSet.remove(taskId)
        YoutubeDL.getInstance().destroyProcessById(taskId)
        activeJobs.remove(taskId)?.cancel()
        engineScope.launch {
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                cleanupPartialFiles(File(task.targetDirectoryPath))
                if (deleteRecord) {
                    taskDao.deleteTaskById(taskId)
                } else {
                    taskDao.markTaskFailedOrPaused(
                        taskId = taskId,
                        status = DownloadStatus.CANCELED,
                        errorMessage = "Canceled by user"
                    )
                }
            }
            onTelemetryUpdated()
        }
    }

    /**
     * Over-The-Air (OTA) update for the internal `yt-dlp` Python binary so site extractors stay fresh
     * without needing an APK update.
     */
    suspend fun updateYtDlpBinary(
        channel: YoutubeDL.UpdateChannel = YoutubeDL.UpdateChannel.STABLE
    ): Result<YoutubeDL.UpdateStatus?> = withContext(Dispatchers.IO) {
        runCatching {
            YoutubeDL.getInstance().updateYoutubeDL(context, channel)
        }
    }

    suspend fun getYtDlpVersion(): String = withContext(Dispatchers.IO) {
        runCatching {
            YoutubeDL.getInstance().version(context) ?: "2024.10+"
        }.getOrDefault("Embedded yt-dlp")
    }

    private suspend fun handleProcessTermination(
        taskId: String,
        reason: String,
        onTelemetryUpdated: () -> Unit
    ) {
        val status = if (pauseRequestedSet.remove(taskId)) {
            DownloadStatus.PAUSED
        } else {
            DownloadStatus.CANCELED
        }
        taskDao.markTaskFailedOrPaused(
            taskId = taskId,
            status = status,
            errorMessage = reason
        )
        onTelemetryUpdated()
    }

    private fun findLatestDownloadedFile(targetDir: File, taskCreatedAtMs: Long): File? {
        if (!targetDir.exists()) return null
        return targetDir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    !file.name.endsWith(".part") &&
                    !file.name.endsWith(".ytdl") &&
                    !file.name.endsWith(".temp") &&
                    file.lastModified() >= (taskCreatedAtMs - 10_000L)
            }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun cleanupPartialFiles(targetDir: File) {
        if (!targetDir.exists()) return
        targetDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".part") || file.name.endsWith(".ytdl")) {
                runCatching { file.delete() }
            }
        }
    }

    private fun parseSpeedFromLogLine(line: String): String {
        val speedRegex = Regex("""(\d+(?:\.\d+)?\s*[KMG]i?B/s)""")
        val match = speedRegex.find(line)
        return match?.groupValues?.getOrNull(1) ?: "Active"
    }

    private fun formatEtaSeconds(etaSeconds: Long): String {
        if (etaSeconds <= 0L) return "--:--"
        val hrs = etaSeconds / 3600
        val mins = (etaSeconds % 3600) / 60
        val secs = etaSeconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatSecondsToHhMmSs(totalSeconds: Long): String {
        val safeSec = totalSeconds.coerceAtLeast(0L)
        val hours = safeSec / 3600
        val minutes = (safeSec % 3600) / 60
        val seconds = safeSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    companion object {
        private const val TAG = "YtDlpEngine"
    }
}
