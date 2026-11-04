package com.pulsedl.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltAndroidApp
class PulseDLApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _engineReady = MutableStateFlow(false)
    val engineReady: StateFlow<Boolean> = _engineReady.asStateFlow()

    private val _engineInitError = MutableStateFlow<String?>(null)
    val engineInitError: StateFlow<String?> = _engineInitError.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initializeNativeMediaBinaries()
    }

    private fun initializeNativeMediaBinaries() {
        applicationScope.launch {
            try {
                YoutubeDL.getInstance().init(this@PulseDLApplication)
                FFmpeg.getInstance().init(this@PulseDLApplication)
                Aria2c.getInstance().init(this@PulseDLApplication)
                _engineReady.value = true
                Log.i(TAG, "Embedded Python, yt-dlp, FFmpeg, and Aria2c initialized successfully.")
            } catch (e: YoutubeDLException) {
                Log.e(TAG, "Failed to initialize native media engine binaries", e)
                _engineInitError.value = e.localizedMessage ?: "Unknown native binary initialization error"
            } catch (t: Throwable) {
                Log.e(TAG, "Unexpected error initializing binaries", t)
                _engineInitError.value = t.localizedMessage ?: "Unexpected initialization error"
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE_DOWNLOADS,
                "Active Media Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time progress, speed, and ETA for active PulseDL downloads"
                setShowBadge(false)
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED_DOWNLOADS,
                "Completed & Failed Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when high-resolution video or audio downloads finish"
                setShowBadge(true)
            }

            manager.createNotificationChannel(activeChannel)
            manager.createNotificationChannel(completedChannel)
        }
    }

    companion object {
        private const val TAG = "PulseDLApplication"
        const val CHANNEL_ACTIVE_DOWNLOADS = "pulsedl_active_downloads"
        const val CHANNEL_COMPLETED_DOWNLOADS = "pulsedl_completed_downloads"
    }
}
