package com.pulsedl.mobile.data.local

import androidx.room.TypeConverter
import com.pulsedl.mobile.domain.model.AudioFormatPreset
import com.pulsedl.mobile.domain.model.DownloadStatus
import com.pulsedl.mobile.domain.model.MediaType
import com.pulsedl.mobile.domain.model.VideoContainer
import com.pulsedl.mobile.domain.model.VideoResolutionPreset

class RoomConverters {

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType =
        runCatching { MediaType.valueOf(value) }.getOrDefault(MediaType.VIDEO)

    @TypeConverter
    fun fromVideoResolution(value: VideoResolutionPreset): String = value.name

    @TypeConverter
    fun toVideoResolution(value: String): VideoResolutionPreset =
        runCatching { VideoResolutionPreset.valueOf(value) }.getOrDefault(VideoResolutionPreset.RES_1080P_FHD)

    @TypeConverter
    fun fromVideoContainer(value: VideoContainer): String = value.name

    @TypeConverter
    fun toVideoContainer(value: String): VideoContainer =
        runCatching { VideoContainer.valueOf(value) }.getOrDefault(VideoContainer.MP4)

    @TypeConverter
    fun fromAudioPreset(value: AudioFormatPreset): String = value.name

    @TypeConverter
    fun toAudioPreset(value: String): AudioFormatPreset =
        runCatching { AudioFormatPreset.valueOf(value) }.getOrDefault(AudioFormatPreset.MP3_320K)

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus =
        runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.QUEUED)
}
