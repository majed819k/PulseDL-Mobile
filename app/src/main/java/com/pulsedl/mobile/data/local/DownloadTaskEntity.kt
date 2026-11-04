package com.pulsedl.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pulsedl.mobile.domain.model.AudioFormatPreset
import com.pulsedl.mobile.domain.model.DownloadStatus
import com.pulsedl.mobile.domain.model.MediaType
import com.pulsedl.mobile.domain.model.VideoContainer
import com.pulsedl.mobile.domain.model.VideoResolutionPreset

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    val taskId: String,
    val url: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val mediaType: MediaType,
    val videoResolution: VideoResolutionPreset,
    val videoContainer: VideoContainer,
    val audioPreset: AudioFormatPreset,
    val status: DownloadStatus,
    val progressPercent: Float,
    val speedText: String,
    val etaText: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val clipStartSeconds: Long?,
    val clipEndSeconds: Long?,
    val embedThumbnail: Boolean,
    val embedMetadata: Boolean,
    val embedSubtitles: Boolean,
    val sponsorBlockRemove: Boolean,
    val concurrentFragments: Int,
    val useAria2c: Boolean,
    val targetDirectoryPath: String,
    val targetSafTreeUri: String?,
    val outputFilePath: String?,
    val errorMessage: String?,
    val lastLogLine: String,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?
) {
    val qualityBadgeText: String
        get() = when (mediaType) {
            MediaType.VIDEO -> "${videoResolution.badge} • ${videoContainer.extension.uppercase()}"
            MediaType.AUDIO -> "${audioPreset.badge} • ${audioPreset.extension.uppercase()}"
        }

    val isClipTrimmed: Boolean
        get() = clipStartSeconds != null && clipEndSeconds != null && clipEndSeconds > clipStartSeconds
}
