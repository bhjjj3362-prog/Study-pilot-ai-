package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudyPilotDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF041E2B),
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = AnalogyTeal,
    onSecondary = Color(0xFF04221E),
    secondaryContainer = AnalogyTealBg,
    onSecondaryContainer = AnalogyTeal,
    tertiary = TrapAmber,
    onTertiary = Color(0xFF281800),
    tertiaryContainer = TrapAmberBg,
    onTertiaryContainer = TrapAmberBright,
    background = ObsidianBg,
    onBackground = IceWhite,
    surface = MidnightSurface,
    onSurface = IceWhite,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = SlateTextSecondary,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = SlateSurfaceElevated,
    surfaceContainerHigh = SlateSurfaceHighest,
    outline = SlateBorder,
    outlineVariant = Color(0xFF1D2840),
    error = ErrorCoral,
    onError = Color(0xFF2B080A),
    errorContainer = ErrorCoralBg,
    onErrorContainer = ErrorCoral,
)

@Composable
fun StudyPilotTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = StudyPilotDarkColorScheme,
        typography = StudyPilotTypography,
        content = content,
    )
}
