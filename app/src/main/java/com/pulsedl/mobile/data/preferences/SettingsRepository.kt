package com.pulsedl.mobile.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import android.os.Environment
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulsedl_settings")

data class UserDownloadPreferences(
    val customSafTreeUri: String? = null,
    val customDirectoryDisplayPath: String,
    val maxConcurrentTasks: Int = 2,
    val concurrentFragments: Int = 4,
    val useAria2cMultiConnection: Boolean = true,
    val embedThumbnailByDefault: Boolean = true,
    val embedMetadataByDefault: Boolean = true,
    val dhikrGateEnabled: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CUSTOM_SAF_URI = stringPreferencesKey("custom_saf_uri")
        val CUSTOM_DIR_DISPLAY = stringPreferencesKey("custom_dir_display")
        val MAX_CONCURRENT_TASKS = intPreferencesKey("max_concurrent_tasks")
        val CONCURRENT_FRAGMENTS = intPreferencesKey("concurrent_fragments")
        val USE_ARIA2C = booleanPreferencesKey("use_aria2c")
        val EMBED_THUMBNAIL = booleanPreferencesKey("embed_thumbnail")
        val EMBED_METADATA = booleanPreferencesKey("embed_metadata")
        val DHIKR_GATE_ENABLED = booleanPreferencesKey("dhikr_gate_enabled")
    }

    val defaultPublicDownloadsDir: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "PulseDL"
        ).apply {
            if (!exists()) mkdirs()
        }

    val preferencesFlow: Flow<UserDownloadPreferences> = context.dataStore.data.map { prefs ->
        UserDownloadPreferences(
            customSafTreeUri = prefs[Keys.CUSTOM_SAF_URI],
            customDirectoryDisplayPath = prefs[Keys.CUSTOM_DIR_DISPLAY]
                ?: defaultPublicDownloadsDir.absolutePath,
            maxConcurrentTasks = (prefs[Keys.MAX_CONCURRENT_TASKS] ?: 2).coerceIn(1, 3),
            concurrentFragments = (prefs[Keys.CONCURRENT_FRAGMENTS] ?: 4).coerceIn(1, 8),
            useAria2cMultiConnection = prefs[Keys.USE_ARIA2C] ?: true,
            embedThumbnailByDefault = prefs[Keys.EMBED_THUMBNAIL] ?: true,
            embedMetadataByDefault = prefs[Keys.EMBED_METADATA] ?: true,
            dhikrGateEnabled = prefs[Keys.DHIKR_GATE_ENABLED] ?: true
        )
    }

    suspend fun updateCustomDownloadLocation(safTreeUri: String?, resolvedDisplayPath: String) {
        context.dataStore.edit { prefs ->
            if (safTreeUri != null) {
                prefs[Keys.CUSTOM_SAF_URI] = safTreeUri
            } else {
                prefs.remove(Keys.CUSTOM_SAF_URI)
            }
            prefs[Keys.CUSTOM_DIR_DISPLAY] = resolvedDisplayPath
        }
    }

    suspend fun updateMaxConcurrentTasks(maxTasks: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_CONCURRENT_TASKS] = maxTasks.coerceIn(1, 3)
        }
    }

    suspend fun updateConcurrentFragments(fragments: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CONCURRENT_FRAGMENTS] = fragments.coerceIn(1, 8)
        }
    }

    suspend fun updateUseAria2c(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_ARIA2C] = enabled
        }
    }
}
