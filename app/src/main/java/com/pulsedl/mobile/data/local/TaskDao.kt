package com.pulsedl.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pulsedl.mobile.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM download_tasks ORDER BY createdAtEpochMs DESC")
    fun observeAllTasks(): Flow<List<DownloadTaskEntity>>

    @Query(
        "SELECT * FROM download_tasks WHERE status IN ('QUEUED', 'FETCHING_METADATA', 'DOWNLOADING', 'MUXING_FFMPEG') ORDER BY createdAtEpochMs ASC"
    )
    fun observeActiveAndQueuedTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Query(
        """
        UPDATE download_tasks 
        SET status = :status,
            progressPercent = :progressPercent,
            speedText = :speedText,
            etaText = :etaText,
            lastLogLine = :lastLogLine
        WHERE taskId = :taskId
        """
    )
    suspend fun updateLiveTelemetry(
        taskId: String,
        status: DownloadStatus,
        progressPercent: Float,
        speedText: String,
        etaText: String,
        lastLogLine: String
    )

    @Query(
        """
        UPDATE download_tasks 
        SET status = :status,
            progressPercent = 100.0,
            speedText = '',
            etaText = 'Done',
            outputFilePath = :outputFilePath,
            completedAtEpochMs = :completedAtEpochMs,
            errorMessage = NULL
        WHERE taskId = :taskId
        """
    )
    suspend fun markTaskCompleted(
        taskId: String,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        outputFilePath: String?,
        completedAtEpochMs: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE download_tasks 
        SET status = :status,
            speedText = '',
            etaText = '',
            errorMessage = :errorMessage
        WHERE taskId = :taskId
        """
    )
    suspend fun markTaskFailedOrPaused(
        taskId: String,
        status: DownloadStatus,
        errorMessage: String?
    )

    @Query("DELETE FROM download_tasks WHERE taskId = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun clearCompletedTasks()
}
