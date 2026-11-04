package com.pulsedl.mobile.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pulsedl.mobile.data.local.DownloadTaskEntity
import com.pulsedl.mobile.data.preferences.UserDownloadPreferences
import com.pulsedl.mobile.domain.model.AudioFormatPreset
import com.pulsedl.mobile.domain.model.DownloadStatus
import com.pulsedl.mobile.domain.model.MediaType
import com.pulsedl.mobile.domain.model.VideoContainer
import com.pulsedl.mobile.domain.model.VideoResolutionPreset
import com.pulsedl.mobile.ui.components.DhikrGateDialog
import com.pulsedl.mobile.ui.components.QuranicReminderCard
import com.pulsedl.mobile.ui.theme.AmberWarning
import com.pulsedl.mobile.ui.theme.CrimsonError
import com.pulsedl.mobile.ui.theme.CyanStream
import com.pulsedl.mobile.ui.theme.ElectricIndigo
import com.pulsedl.mobile.ui.theme.EmeraldSuccess
import com.pulsedl.mobile.ui.theme.ObsidianBorder
import com.pulsedl.mobile.ui.theme.ObsidianBorderHighlight
import com.pulsedl.mobile.ui.theme.ObsidianCanvas
import com.pulsedl.mobile.ui.theme.ObsidianSurfaceElevated
import com.pulsedl.mobile.ui.theme.PulseDLTheme
import com.pulsedl.mobile.ui.theme.TextMuted
import com.pulsedl.mobile.ui.theme.TextPrimary
import com.pulsedl.mobile.ui.theme.TextSecondary
import com.pulsedl.mobile.ui.theme.obsidianGlassCard
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PulseDLTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
                val prefs by viewModel.preferences.collectAsStateWithLifecycle()
                val engineReady by viewModel.engineReady.collectAsStateWithLifecycle()

                // Request notification permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Custom Download Directory Picker via SAF OpenDocumentTree()
                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    viewModel.onCustomDirectorySelected(uri)
                }

                PulseDLAdaptiveDashboard(
                    uiState = uiState,
                    tasks = tasks,
                    prefs = prefs,
                    engineReady = engineReady,
                    viewModel = viewModel,
                    onPickCustomFolder = { folderPickerLauncher.launch(null) }
                )

                // Interactive 5-Tasbih Download Gate Modal
                if (uiState.showDhikrGateModal) {
                    val qualityBadge = when (uiState.selectedMediaType) {
                        MediaType.VIDEO ->
                            "${uiState.selectedVideoResolution.badge} • ${uiState.selectedVideoContainer.extension.uppercase()}"
                        MediaType.AUDIO ->
                            "${uiState.selectedAudioPreset.badge} • ${uiState.selectedAudioPreset.extension.uppercase()}"
                    }
                    DhikrGateDialog(
                        targetTitle = uiState.metadata?.title
                            ?: uiState.urlInput.take(55).ifBlank { "Selected Media Stream" },
                        qualityBadge = qualityBadge,
                        onDismiss = viewModel::dismissDhikrGateModal,
                        onGateCompleted = {
                            viewModel.onDhikrGateCompletedAndDispatch()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PulseDLAdaptiveDashboard(
    uiState: ComposerUiState,
    tasks: List<DownloadTaskEntity>,
    prefs: UserDownloadPreferences,
    engineReady: Boolean,
    viewModel: MainViewModel,
    onPickCustomFolder: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .statusBarsPadding()
    ) {
        val isExpandedScreen = maxWidth >= 720.dp

        if (isExpandedScreen) {
            // 2-Column Master-Detail Adaptive Layout for Foldables & Tablets
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TopHeaderBar(
                        engineReady = engineReady,
                        isUpdatingYtDlp = uiState.isUpdatingYtDlp,
                        onUpdateYtDlp = viewModel::triggerOtaYtDlpUpdate
                    )
                    QuranicReminderCard()
                    StorageDirectorySelectorBar(
                        prefs = prefs,
                        onPickFolder = onPickCustomFolder,
                        onResetDefault = viewModel::resetToDefaultDownloadDirectory
                    )
                    StreamComposerCard(
                        uiState = uiState,
                        prefs = prefs,
                        viewModel = viewModel
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight()
                ) {
                    ParallelQueueSection(
                        tasks = tasks,
                        prefs = prefs,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // 1-Column Responsive Scrollable Layout for Compact & Large Phones
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TopHeaderBar(
                        engineReady = engineReady,
                        isUpdatingYtDlp = uiState.isUpdatingYtDlp,
                        onUpdateYtDlp = viewModel::triggerOtaYtDlpUpdate
                    )
                }

                item {
                    QuranicReminderCard()
                }

                item {
                    StorageDirectorySelectorBar(
                        prefs = prefs,
                        onPickFolder = onPickCustomFolder,
                        onResetDefault = viewModel::resetToDefaultDownloadDirectory
                    )
                }

                item {
                    StreamComposerCard(
                        uiState = uiState,
                        prefs = prefs,
                        viewModel = viewModel
                    )
                }

                item {
                    QueueHeaderBar(
                        tasks = tasks,
                        prefs = prefs,
                        onClearCompleted = viewModel::clearCompletedTasks,
                        onSetMaxConcurrent = viewModel::setMaxConcurrentTasks
                    )
                }

                if (tasks.isEmpty()) {
                    item {
                        EmptyQueuePlaceholder()
                    }
                } else {
                    items(items = tasks, key = { it.taskId }) { task ->
                        DownloadTaskQueueCard(
                            task = task,
                            onPause = { viewModel.pauseTask(task.taskId) },
                            onResume = { viewModel.resumeTask(task.taskId) },
                            onCancel = { viewModel.cancelTask(task.taskId, deleteRecord = false) },
                            onDelete = { viewModel.cancelTask(task.taskId, deleteRecord = true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    engineReady: Boolean,
    isUpdatingYtDlp: Boolean,
    onUpdateYtDlp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (engineReady) EmeraldSuccess else AmberWarning)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PulseDL Mobile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = "Local Python • yt-dlp • FFmpeg • Aria2c Engine",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        OutlinedButton(
            onClick = onUpdateYtDlp,
            enabled = !isUpdatingYtDlp,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isUpdatingYtDlp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = CyanStream
                )
            } else {
                Icon(
                    imageVector = Icons.Default.SystemUpdateAlt,
                    contentDescription = "Update yt-dlp",
                    tint = CyanStream,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "OTA Update",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun StorageDirectorySelectorBar(
    prefs: UserDownloadPreferences,
    onPickFolder: () -> Unit,
    onResetDefault: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .obsidianGlassCard(cornerRadius = 16.dp, borderColor = ObsidianBorder)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = CyanStream,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "DOWNLOAD LOCATION (INTERNAL / SD CARD)",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanStream
                )
                Text(
                    text = prefs.customDirectoryDisplayPath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (prefs.customSafTreeUri != null) {
                IconButton(onClick = onResetDefault) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset default folder",
                        tint = TextSecondary
                    )
                }
            }
            OutlinedButton(
                onClick = onPickFolder,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanStream
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamComposerCard(
    uiState: ComposerUiState,
    prefs: UserDownloadPreferences,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showEngineSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .obsidianGlassCard(
                cornerRadius = 22.dp,
                borderColor = ObsidianBorderHighlight,
                elevated = true
            )
            .padding(18.dp)
    ) {
        // URL Input & Paste / Inspect Row
        OutlinedTextField(
            value = uiState.urlInput,
            onValueChange = viewModel::onUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Paste YouTube, TikTok, Instagram, X, Vimeo, or SoundCloud link...",
                    color = TextMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricIndigo,
                unfocusedBorderColor = ObsidianBorder,
                focusedContainerColor = ObsidianCanvas.copy(alpha = 0.7f),
                unfocusedContainerColor = ObsidianCanvas.copy(alpha = 0.5f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                            if (!pasted.isNullOrBlank()) {
                                viewModel.onUrlChanged(pasted)
                                viewModel.inspectUrl(pasted)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste Link",
                            tint = CyanStream
                        )
                    }

                    IconButton(
                        onClick = { viewModel.inspectUrl() },
                        enabled = !uiState.isFetchingMetadata
                    ) {
                        if (uiState.isFetchingMetadata) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ElectricIndigo
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Inspect Metadata",
                                tint = ElectricIndigo
                            )
                        }
                    }
                }
            }
        )

        // Error or Status Banner
        uiState.metadataError?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = err,
                style = MaterialTheme.typography.bodyMedium,
                color = CrimsonError
            )
        }

        uiState.statusBannerMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = EmeraldSuccess
            )
        }

        // Rich Media Metadata Preview Card (when inspected)
        AnimatedVisibility(visible = uiState.metadata != null) {
            uiState.metadata?.let { meta ->
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianCanvas.copy(alpha = 0.65f))
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!meta.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = meta.thumbnailUrl,
                                contentDescription = meta.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 96.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meta.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${meta.uploader} • ${viewModel.formatDuration(meta.durationSeconds)} • Max ${meta.maxAvailableHeight}p",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanStream
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Switcher: Ultra-High-Res Video vs Lossless/320k Audio
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isVideo = uiState.selectedMediaType == MediaType.VIDEO
            FilterChip(
                selected = isVideo,
                onClick = { viewModel.setMediaType(MediaType.VIDEO) },
                label = { Text("Ultra-HD Video (MP4 / MKV)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricIndigo.copy(alpha = 0.25f),
                    selectedLabelColor = TextPrimary,
                    selectedLeadingIconColor = ElectricIndigo
                ),
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = !isVideo,
                onClick = { viewModel.setMediaType(MediaType.AUDIO) },
                label = { Text("Studio Audio (FLAC / 320k)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanStream.copy(alpha = 0.25f),
                    selectedLabelColor = TextPrimary,
                    selectedLeadingIconColor = CyanStream
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resolution / Format Selector Grid
        if (uiState.selectedMediaType == MediaType.VIDEO) {
            Text(
                text = "SELECT VIDEO RESOLUTION (FFMPEG BESTVIDEO+BESTAUDIO)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoResolutionPreset.entries.forEach { preset ->
                    val selected = uiState.selectedVideoResolution == preset
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setVideoResolution(preset) },
                        label = { Text(preset.badge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricIndigo,
                            selectedLabelColor = TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Container:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                VideoContainer.entries.forEach { container ->
                    FilterChip(
                        selected = uiState.selectedVideoContainer == container,
                        onClick = { viewModel.setVideoContainer(container) },
                        label = { Text(container.extension.uppercase()) }
                    )
                }
            }
        } else {
            Text(
                text = "SELECT HIGH-FIDELITY AUDIO CODEC",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioFormatPreset.entries.forEach { preset ->
                    val selected = uiState.selectedAudioPreset == preset
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setAudioPreset(preset) },
                        label = { Text("${preset.extension.uppercase()} • ${preset.badge}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanStream.copy(alpha = 0.35f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Timeline Clip Trimmer Toggle & Dual-Handle RangeSlider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = CyanStream,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Visual Timeline Clip Trimmer",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Cut exact time range (--download-sections)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            Switch(
                checked = uiState.enableClipTrimmer,
                onCheckedChange = viewModel::toggleClipTrimmer,
                colors = SwitchDefaults.colors(checkedThumbColor = CyanStream)
            )
        }

        AnimatedVisibility(visible = uiState.enableClipTrimmer) {
            val maxDuration = (uiState.metadata?.durationSeconds?.takeIf { it > 0 } ?: 600L).toFloat()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianCanvas.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Start: ${viewModel.formatDuration(uiState.clipStartSeconds.toLong())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanStream
                    )
                    Text(
                        text = "End: ${viewModel.formatDuration(uiState.clipEndSeconds.toLong())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSuccess
                    )
                }
                RangeSlider(
                    value = uiState.clipStartSeconds..uiState.clipEndSeconds.coerceAtMost(maxDuration),
                    onValueChange = { range ->
                        viewModel.updateClipRange(range.start, range.endInclusive)
                    },
                    valueRange = 0f..maxDuration,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanStream,
                        activeTrackColor = ElectricIndigo
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Advanced Engine & Metadata Toggles Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.embedThumbnail,
                onClick = { viewModel.toggleEmbedThumbnail(!uiState.embedThumbnail) },
                label = { Text("Embed Cover Art") }
            )
            FilterChip(
                selected = uiState.embedMetadata,
                onClick = { viewModel.toggleEmbedMetadata(!uiState.embedMetadata) },
                label = { Text("ID3 / Metadata") }
            )
            FilterChip(
                selected = uiState.sponsorBlockRemove,
                onClick = { viewModel.toggleSponsorBlock(!uiState.sponsorBlockRemove) },
                label = { Text("SponsorBlock Cut") }
            )
            FilterChip(
                selected = showEngineSettings,
                onClick = { showEngineSettings = !showEngineSettings },
                label = { Text("Concurrency & Aria2c") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        AnimatedVisibility(visible = showEngineSettings) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianCanvas.copy(alpha = 0.65f))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simultaneous Video Tasks (Semaphore): ${prefs.maxConcurrentTasks}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3).forEach { count ->
                            FilterChip(
                                selected = prefs.maxConcurrentTasks == count,
                                onClick = { viewModel.setMaxConcurrentTasks(count) },
                                label = { Text("${count}x") }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aria2c 8-Connection Turbo + 4x DASH Fragments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Bypasses single-stream bandwidth throttling",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = prefs.useAria2cMultiConnection,
                        onCheckedChange = viewModel::setUseAria2c
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary CTA: Opens Interactive 5-Tasbih Download Gate
        Button(
            onClick = viewModel::requestStartDownloadWithDhikrGate,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricIndigo,
                contentColor = TextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Start Download (5-Tasbih Gate)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ParallelQueueSection(
    tasks: List<DownloadTaskEntity>,
    prefs: UserDownloadPreferences,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        QueueHeaderBar(
            tasks = tasks,
            prefs = prefs,
            onClearCompleted = viewModel::clearCompletedTasks,
            onSetMaxConcurrent = viewModel::setMaxConcurrentTasks
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (tasks.isEmpty()) {
            EmptyQueuePlaceholder()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(items = tasks, key = { it.taskId }) { task ->
                    DownloadTaskQueueCard(
                        task = task,
                        onPause = { viewModel.pauseTask(task.taskId) },
                        onResume = { viewModel.resumeTask(task.taskId) },
                        onCancel = { viewModel.cancelTask(task.taskId, deleteRecord = false) },
                        onDelete = { viewModel.cancelTask(task.taskId, deleteRecord = true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueHeaderBar(
    tasks: List<DownloadTaskEntity>,
    prefs: UserDownloadPreferences,
    onClearCompleted: () -> Unit,
    onSetMaxConcurrent: (Int) -> Unit
) {
    val activeCount = tasks.count {
        it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.MUXING_FFMPEG ||
            it.status == DownloadStatus.FETCHING_METADATA
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = CyanStream,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Parallel Queue ($activeCount/${prefs.maxConcurrentTasks} Active)",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        if (tasks.any { it.status == DownloadStatus.COMPLETED }) {
            OutlinedButton(
                onClick = onClearCompleted,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Clear Done",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyQueuePlaceholder() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .obsidianGlassCard(cornerRadius = 18.dp)
            .padding(vertical = 36.dp, horizontal = 20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.HighQuality,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Active or Queued Downloads",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Paste a link above or share directly from YouTube, TikTok, or Instagram.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
fun DownloadTaskQueueCard(
    task: DownloadTaskEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (task.status) {
        DownloadStatus.DOWNLOADING, DownloadStatus.FETCHING_METADATA -> CyanStream
        DownloadStatus.MUXING_FFMPEG -> ElectricIndigo
        DownloadStatus.COMPLETED -> EmeraldSuccess
        DownloadStatus.PAUSED, DownloadStatus.QUEUED -> AmberWarning
        DownloadStatus.FAILED, DownloadStatus.CANCELED -> CrimsonError
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .obsidianGlassCard(
                cornerRadius = 18.dp,
                borderColor = statusColor.copy(alpha = 0.4f)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!task.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = task.thumbnailUrl,
                    contentDescription = task.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 72.dp, height = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.qualityBadgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                    Text(
                        text = "• ${task.status.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            // Action Buttons (Pause / Resume / Cancel / Delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (task.status) {
                    DownloadStatus.DOWNLOADING,
                    DownloadStatus.FETCHING_METADATA,
                    DownloadStatus.QUEUED -> {
                        IconButton(onClick = onPause) {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = "Pause",
                                tint = AmberWarning
                            )
                        }
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = CrimsonError
                            )
                        }
                    }

                    DownloadStatus.PAUSED,
                    DownloadStatus.FAILED,
                    DownloadStatus.CANCELED -> {
                        IconButton(onClick = onResume) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Resume",
                                tint = EmeraldSuccess
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove",
                                tint = TextSecondary
                            )
                        }
                    }

                    DownloadStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = EmeraldSuccess,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(22.dp)
                        )
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete record",
                                tint = TextSecondary
                            )
                        }
                    }

                    DownloadStatus.MUXING_FFMPEG -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                            color = ElectricIndigo
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { (task.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = statusColor,
            trackColor = ObsidianCanvas
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${task.progressPercent.roundToInt()}% • ${task.speedText.ifBlank { task.status.name }}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Text(
                text = if (task.etaText.isNotBlank()) "ETA ${task.etaText}" else "",
                style = MaterialTheme.typography.labelMedium,
                color = CyanStream
            )
        }

        if (!task.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = AmberWarning,
                maxLines = 2
            )
        } else if (!task.outputFilePath.isNullOrBlank() && task.status == DownloadStatus.COMPLETED) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saved: ${task.outputFilePath}",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldSuccess,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
