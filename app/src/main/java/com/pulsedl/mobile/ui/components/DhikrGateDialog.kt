package com.pulsedl.mobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pulsedl.mobile.ui.theme.CyanStream
import com.pulsedl.mobile.ui.theme.ElectricIndigo
import com.pulsedl.mobile.ui.theme.EmeraldSuccess
import com.pulsedl.mobile.ui.theme.ObsidianBorderHighlight
import com.pulsedl.mobile.ui.theme.ObsidianCanvas
import com.pulsedl.mobile.ui.theme.ObsidianSurfaceElevated
import com.pulsedl.mobile.ui.theme.TextMuted
import com.pulsedl.mobile.ui.theme.TextPrimary
import com.pulsedl.mobile.ui.theme.TextSecondary
import com.pulsedl.mobile.ui.theme.obsidianGlassCard
import kotlinx.coroutines.delay

data class AuthenticDhikrItem(
    val arabicText: String,
    val transliteration: String,
    val virtueTranslation: String
)

private val AUTHENTIC_ADHIKAR = listOf(
    AuthenticDhikrItem(
        arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        transliteration = "SubhanAllahi wa bihamdihi",
        virtueTranslation = "Glory be to Allah and praise Him — plants a palm tree in Paradise."
    ),
    AuthenticDhikrItem(
        arabicText = "سُبْحَانَ اللَّهِ الْعَظِيمِ",
        transliteration = "SubhanAllahil-Adheem",
        virtueTranslation = "Glory be to Allah, the Supreme — light on the tongue, heavy on the Scales."
    ),
    AuthenticDhikrItem(
        arabicText = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
        transliteration = "Astaghfirullaha wa atubu ilayh",
        virtueTranslation = "I seek the forgiveness of Allah and repent to Him."
    ),
    AuthenticDhikrItem(
        arabicText = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
        transliteration = "La hawla wa la quwwata illa billah",
        virtueTranslation = "There is no power and no strength except through Allah — a treasure of Paradise."
    ),
    AuthenticDhikrItem(
        arabicText = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
        transliteration = "Allahumma salli wa sallim 'ala Nabiyyina Muhammad",
        virtueTranslation = "O Allah, send peace and blessings upon our Prophet Muhammad."
    )
)

/**
 * Interactive 5-Tasbih Download Gate (`DhikrGateDialog.kt`).
 * Replaces intrusive/immoral mobile ad networks with a serene, 5-tap Dhikr counter.
 * Each tap triggers subtle native haptic feedback (`HapticFeedbackType.TextHandleMove`).
 * Upon reaching 5 / 5, triggers a completion pulse and immediately dispatches the download task.
 */
@Composable
fun DhikrGateDialog(
    targetTitle: String,
    qualityBadge: String,
    onDismiss: () -> Unit,
    onGateCompleted: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val requiredCount = 5
    var currentCount by remember { mutableIntStateOf(0) }

    val animatedProgress by animateFloatAsState(
        targetValue = currentCount.toFloat() / requiredCount.toFloat(),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "TasbihProgressRing"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (currentCount == requiredCount) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "TasbihScale"
    )

    // Auto-dispatch download as soon as 5/5 is completed
    LaunchedEffect(currentCount) {
        if (currentCount >= requiredCount) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(280L)
            onGateCompleted()
        }
    }

    val currentDhikr = AUTHENTIC_ADHIKAR[(currentCount.coerceAtMost(requiredCount - 1)) % AUTHENTIC_ADHIKAR.size]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .obsidianGlassCard(
                    cornerRadius = 28.dp,
                    borderColor = ObsidianBorderHighlight,
                    elevated = true
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanStream,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AD-FREE ETHICAL GATE • بوابة الذكر",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanStream
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = targetTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = ElectricIndigo.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(
                        1.dp,
                        ElectricIndigo.copy(alpha = 0.4f),
                        RoundedCornerShape(50)
                    )
                ) {
                    Text(
                        text = qualityBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Rotating Authentic Dhikr Card
                AnimatedContent(
                    targetState = currentDhikr,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                    },
                    label = "DhikrPhraseAnimation"
                ) { dhikr ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(ObsidianCanvas.copy(alpha = 0.65f))
                            .border(
                                1.dp,
                                ObsidianBorderHighlight.copy(alpha = 0.5f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = dhikr.arabicText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dhikr.transliteration,
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanStream,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dhikr.virtueTranslation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Interactive Circular Animated Progress Ring (0 / 5)
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(164.dp)
                        .scale(buttonScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    if (currentCount >= requiredCount) EmeraldSuccess.copy(alpha = 0.28f)
                                    else ElectricIndigo.copy(alpha = 0.24f),
                                    ObsidianSurfaceElevated.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (currentCount < requiredCount) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentCount += 1
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.size(152.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        // Background track ring
                        drawArc(
                            color = ObsidianBorderHighlight.copy(alpha = 0.45f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Animated progress ring
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ElectricIndigo,
                                    CyanStream,
                                    EmeraldSuccess,
                                    ElectricIndigo
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (currentCount >= requiredCount) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Starting...",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldSuccess
                            )
                        } else {
                            Text(
                                text = "$currentCount / $requiredCount",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = CyanStream,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tap for Tasbih",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyanStream
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Zero ads • Zero tracking • 5 Tasbihat unlock high-speed local download",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
