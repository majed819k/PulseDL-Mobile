package com.pulsedl.mobile.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    VIDEO,
    AUDIO
}

@Serializable
enum class VideoResolutionPreset(
    val label: String,
    val badge: String,
    val maxHeight: Int,
    val prefer60Fps: Boolean = false
) {
    RES_8K_UHD("4320p 8K Ultra HD", "8K UHD", 4320, true),
    RES_4K_UHD("2160p 4K Ultra HD", "4K UHD", 2160, true),
    RES_2K_QHD("1440p 2K QHD", "2K QHD", 1440, true),
    RES_1080P_FHD("1080p Full HD 60fps", "1080p60", 1080, true),
    RES_720P_HD("720p HD", "720p", 720, false),
    RES_480P_SD("480p Standard", "480p", 480, false)
}

@Serializable
enum class VideoContainer(val extension: String, val label: String) {
    MP4("mp4", "MP4 (Universal H.264/HEVC/AV1)"),
    MKV("mkv", "MKV (Matroska Lossless Stream Merge)")
}

@Serializable
enum class AudioFormatPreset(
    val ytDlpCodec: String,
    val extension: String,
    val label: String,
    val qualityArg: String,
    val badge: String
) {
    FLAC_LOSSLESS("flac", "flac", "FLAC (24-bit Studio Lossless)", "0", "LOSSLESS"),
    MP3_320K("mp3", "mp3", "MP3 (320kbps CBR High-Fidelity)", "320K", "320kbps"),
    M4A_AAC("m4a", "m4a", "M4A (AAC Pure Apple/Mobile)", "0", "AAC"),
    OPUS_ULTRA("opus", "opus", "Opus (Next-Gen High Efficiency)", "0", "OPUS")
}

@Serializable
enum class DownloadStatus {
    QUEUED,
    FETCHING_METADATA,
    DOWNLOADING,
    MUXING_FFMPEG,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED
}

@Serializable
data class MediaMetadata(
    val id: String,
    val url: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val webpageUrl: String,
    val extractorKey: String,
    val maxAvailableHeight: Int,
    val supports60Fps: Boolean,
    val availableFormatsSummary: List<FormatOptionSummary> = emptyList()
)

@Serializable
data class FormatOptionSummary(
    val formatId: String,
    val extension: String,
    val resolution: String,
    val fps: Int?,
    val vcodec: String?,
    val acodec: String?,
    val fileSizeApproxBytes: Long?
)

@Serializable
data class DownloadConfiguration(
    val url: String,
    val titleOverride: String? = null,
    val thumbnailUrl: String? = null,
    val uploader: String? = null,
    val durationSeconds: Long = 0L,
    val mediaType: MediaType = MediaType.VIDEO,
    val videoResolution: VideoResolutionPreset = VideoResolutionPreset.RES_1080P_FHD,
    val videoContainer: VideoContainer = VideoContainer.MP4,
    val audioPreset: AudioFormatPreset = AudioFormatPreset.MP3_320K,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedSubtitles: Boolean = false,
    val sponsorBlockRemove: Boolean = false,
    val clipStartSeconds: Long? = null,
    val clipEndSeconds: Long? = null,
    val customOutputDirUri: String? = null,
    val customSubfolderName: String = "PulseDL",
    val concurrentFragments: Int = 4,
    val useAria2cMultiConnection: Boolean = true
)
