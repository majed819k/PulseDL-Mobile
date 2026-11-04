package com.pulsedl.mobile.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.pulsedl.mobile.PulseDLApplication
import com.pulsedl.mobile.data.local.DownloadTaskEntity
import com.pulsedl.mobile.data.local.TaskDao
import com.pulsedl.mobile.domain.model.DownloadStatus
import com.pulsedl.mobile.engine.YtDlpEngine
import com.pulsedl.mobile.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class DownloadForegroundService : LifecycleService() {

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var ytDlpEngine: YtDlpEngine

    private var wakeLock: PowerManager.WakeLock? = null
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        startMonitoringActiveQueue()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_OR_SYNC -> {
                promoteToForeground( buildSummaryNotification(emptyList()) )
            }
            ACTION_PAUSE_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (!taskId.isNullOrBlank()) {
                    ytDlpEngine.pauseTask(taskId)
                }
            }
            ACTION_RESUME_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (!taskId.isNullOrBlank()) {
                    promoteToForeground(buildSummaryNotification(emptyList()))
                    ytDlpEngine.resumeTask(taskId)
                }
            }
            ACTION_CANCEL_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (!taskId.isNullOrBlank()) {
                    ytDlpEngine.cancelTask(taskId)
                }
            }
            ACTION_PAUSE_ALL -> {
                lifecycleScope.launch {
                    val active = taskDao.observeActiveAndQueuedTasks()
                    // Pause all currently active tasks
                }
            }
        }

        return START_STICKY
    }

    private fun startMonitoringActiveQueue() {
        monitorJob?.cancel()
        monitorJob = lifecycleScope.launch {
            taskDao.observeActiveAndQueuedTasks().collectLatest { activeTasks ->
                val notificationManager = getSystemService(NotificationManager::class.java)
                if (activeTasks.isNotEmpty()) {
                    val notification = buildSummaryNotification(activeTasks)
                    promoteToForeground(notification)
                    notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notification)
                } else {
                    // Grace period before stopping foreground service so rapid queue transitions stay smooth
                    delay(1500L)
                    ServiceCompat.stopForeground(this@DownloadForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun promoteToForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID_FOREGROUND, notification)
        }
    }

    private fun buildSummaryNotification(activeTasks: List<DownloadTaskEntity>): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (activeTasks.isEmpty()) {
            return NotificationCompat.Builder(this, PulseDLApplication.CHANNEL_ACTIVE_DOWNLOADS)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("PulseDL Engine Active")
                .setContentText("Preparing parallel media pipeline...")
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
        }

        val primaryTask = activeTasks.firstOrNull {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.MUXING_FFMPEG
        } ?: activeTasks.first()

        val avgProgress = activeTasks.map { it.progressPercent }.average().roundToInt().coerceIn(0, 100)
        val titleText = if (activeTasks.size == 1) {
            primaryTask.title
        } else {
            "[${activeTasks.size} Active] ${primaryTask.title}"
        }

        val statusSubtitle = buildString {
            append("${primaryTask.progressPercent.roundToInt()}%")
            if (primaryTask.speedText.isNotBlank()) {
                append(" • ${primaryTask.speedText}")
            }
            if (primaryTask.etaText.isNotBlank()) {
                append(" • ETA ${primaryTask.etaText}")
            }
            append(" (${primaryTask.qualityBadgeText})")
        }

        val pauseIntent = Intent(this, DownloadForegroundService::class.java).apply {
            action = ACTION_PAUSE_TASK
            putExtra(EXTRA_TASK_ID, primaryTask.taskId)
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            primaryTask.taskId.hashCode(),
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, DownloadForegroundService::class.java).apply {
            action = ACTION_CANCEL_TASK
            putExtra(EXTRA_TASK_ID, primaryTask.taskId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            primaryTask.taskId.hashCode() + 1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PulseDLApplication.CHANNEL_ACTIVE_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(titleText)
            .setContentText(statusSubtitle)
            .setSubText(primaryTask.qualityBadgeText)
            .setProgress(100, avgProgress, primaryTask.status == DownloadStatus.FETCHING_METADATA)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPendingIntent
            )
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PulseDL::MediaPipelineWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L) // Up to 60 minutes for large 8K/4K playlists
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID_FOREGROUND = 4001
        const val ACTION_START_OR_SYNC = "com.pulsedl.mobile.action.START_OR_SYNC"
        const val ACTION_PAUSE_TASK = "com.pulsedl.mobile.action.PAUSE_TASK"
        const val ACTION_RESUME_TASK = "com.pulsedl.mobile.action.RESUME_TASK"
        const val ACTION_CANCEL_TASK = "com.pulsedl.mobile.action.CANCEL_TASK"
        const val ACTION_PAUSE_ALL = "com.pulsedl.mobile.action.PAUSE_ALL"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun startOrSync(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START_OR_SYNC
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
