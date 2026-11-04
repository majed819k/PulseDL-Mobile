package com.pulsedl.mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulsedl.mobile.domain.model.AudioFormatPreset
import com.pulsedl.mobile.domain.model.MediaType
import com.pulsedl.mobile.domain.model.VideoResolutionPreset
import com.pulsedl.mobile.ui.components.DhikrGateDialog
import com.pulsedl.mobile.ui.theme.CyanStream
import com.pulsedl.mobile.ui.theme.ElectricIndigo
import com.pulsedl.mobile.ui.theme.ObsidianSurfaceElevated
import com.pulsedl.mobile.ui.theme.PulseDLTheme
import com.pulsedl.mobile.ui.theme.TextPrimary
import com.pulsedl.mobile.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint

/**
 * Instant Share-Sheet Bottom Sheet Interceptor (`ShareInterceptActivity.kt`).
 * Triggered whenever the user taps "Share -> Download with PulseDL" inside YouTube, TikTok,
 * Instagram, X/Twitter, or any browser. Extracts the first HTTP/HTTPS URL from `EXTRA_TEXT`,
 * inspects metadata in the background, and displays the 5-Tasbih Dhikr Gate before queueing.
 */
@AndroidEntryPoint
class ShareInterceptActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = extractFirstUrlFromIntent(intent)
        if (sharedUrl.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            PulseDLTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                LaunchedEffect(sharedUrl) {
                    viewModel.onUrlChanged(sharedUrl)
                    viewModel.inspectUrl(sharedUrl)
                }

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = sheetState,
                    containerColor = ObsidianSurfaceElevated,
                    contentColor = TextPrimary
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "PulseDL Quick Intercept",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.metadata?.title ?: sharedUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanStream,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (uiState.isFetchingMetadata) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanStream
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Fetching available 8K/4K/Audio streams...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = uiState.selectedMediaType == MediaType.VIDEO,
                                onClick = { viewModel.setMediaType(MediaType.VIDEO) },
                                label = { Text("Video") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VideoFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            FilterChip(
                                selected = uiState.selectedMediaType == MediaType.AUDIO,
                                onClick = { viewModel.setMediaType(MediaType.AUDIO) },
                                label = { Text("Audio Only") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AudioFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.selectedMediaType == MediaType.VIDEO) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VideoResolutionPreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = uiState.selectedVideoResolution == preset,
                                        onClick = { viewModel.setVideoResolution(preset) },
                                        label = { Text(preset.badge) }
                                    )
                                }
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AudioFormatPreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = uiState.selectedAudioPreset == preset,
                                        onClick = { viewModel.setAudioPreset(preset) },
                                        label = { Text("${preset.extension.uppercase()} • ${preset.badge}") }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = viewModel::requestStartDownloadWithDhikrGate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unlock Download (5-Tasbih Gate)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (uiState.showDhikrGateModal) {
                    val qualityBadge = when (uiState.selectedMediaType) {
                        MediaType.VIDEO ->
                            "${uiState.selectedVideoResolution.badge} • ${uiState.selectedVideoContainer.extension.uppercase()}"
                        MediaType.AUDIO ->
                            "${uiState.selectedAudioPreset.badge} • ${uiState.selectedAudioPreset.extension.uppercase()}"
                    }
                    DhikrGateDialog(
                        targetTitle = uiState.metadata?.title ?: sharedUrl,
                        qualityBadge = qualityBadge,
                        onDismiss = viewModel::dismissDhikrGateModal,
                        onGateCompleted = {
                            viewModel.onDhikrGateCompletedAndDispatch {
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun extractFirstUrlFromIntent(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val rawText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        val urlRegex = Regex("""(https?://[^\s]+)""")
        return urlRegex.find(rawText)?.value
    }
}
