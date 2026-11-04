package com.pulsedl.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Deep Obsidian Glass Color Palette
val ObsidianCanvas = Color(0xFF090A0F)
val ObsidianSurface = Color(0xFF12151F)
val ObsidianSurfaceElevated = Color(0xFF191D2B)
val ObsidianBorder = Color(0xFF262B3D)
val ObsidianBorderHighlight = Color(0xFF38415C)

val ElectricIndigo = Color(0xFF6366F1)
val ElectricIndigoGlow = Color(0xFF818CF8)
val CyanStream = Color(0xFF06B6D4)
val CyanStreamSoft = Color(0xFF22D3EE)
val EmeraldSuccess = Color(0xFF10B981)
val EmeraldGlow = Color(0xFF34D399)
val AmberWarning = Color(0xFFF59E0B)
val CrimsonError = Color(0xFFEF4444)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

private val DeepObsidianColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = TextPrimary,
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = ElectricIndigoGlow,
    secondary = CyanStream,
    onSecondary = ObsidianCanvas,
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = CyanStreamSoft,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianCanvas,
    background = ObsidianCanvas,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    error = CrimsonError,
    onError = TextPrimary
)

val PulseDLTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

/**
 * Reusable Glassmorphic card modifier with subtle gradient surface and luminous border.
 */
fun Modifier.obsidianGlassCard(
    cornerRadius: Dp = 20.dp,
    borderColor: Color = ObsidianBorder,
    elevated: Boolean = false
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val startColor = if (elevated) ObsidianSurfaceElevated.copy(alpha = 0.94f) else ObsidianSurface.copy(alpha = 0.90f)
    val endColor = ObsidianSurface.copy(alpha = 0.78f)
    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(startColor, endColor)
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.85f),
                    borderColor.copy(alpha = 0.25f)
                )
            ),
            shape = shape
        )
}

@Composable
fun PulseDLTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianCanvas.toArgb()
                window.navigationBarColor = ObsidianCanvas.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = DeepObsidianColorScheme,
        typography = PulseDLTypography,
        content = content
    )
}
