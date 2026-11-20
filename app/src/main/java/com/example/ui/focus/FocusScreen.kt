package com.example.ui.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.FocusDailyStats
import com.example.ui.theme.AnalogyTeal
import com.example.ui.theme.AnalogyTealBg
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateSurfaceHighest
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextSecondary
import java.util.Locale

@Composable
fun FocusScreen(
    timerState: FocusTimerUiState,
    dailyStats: FocusDailyStats,
    onSwitchMode: (FocusMode) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResetClick: () -> Unit,
    onReturnToSolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onReturnToSolve()
    }

    val accentColor = if (timerState.mode == FocusMode.FOCUS) ElectricCyan else AnalogyTeal
    val formattedClock = formatMmSs(timerState.remainingSeconds)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FocusModeToggleRow(
                selectedMode = timerState.mode,
                onSelectMode = onSwitchMode,
            )

            AnimatedVisibility(visible = timerState.justCompletedFocus) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_complete_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = AnalogyTealBg,
                    border = BorderStroke(1.dp, AnalogyTeal.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = AnalogyTeal,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.focus_complete_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IceWhite,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = SlateSurfaceElevated,
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Box(
                        modifier = Modifier.size(230.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = androidx.compose.ui.geometry.Offset(
                                x = (size.width - diameter) / 2f,
                                y = (size.height - diameter) / 2f,
                            )
                            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                            drawArc(
                                color = SlateBorder,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )

                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = 360f * timerState.progressFraction,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = formattedClock,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 46.sp,
                                color = IceWhite,
                                modifier = Modifier.testTag("focus_timer_clock"),
                            )
                            Text(
                                text = stringResource(
                                    if (timerState.mode == FocusMode.FOCUS) {
                                        R.string.focus_mode_focus
                                    } else {
                                        R.string.focus_mode_break
                                    }
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor,
                            )
                        }
                    }

                    Text(
                        text = stringResource(
                            if (timerState.mode == FocusMode.FOCUS) {
                                R.string.focus_running_hint
                            } else {
                                R.string.focus_break_hint
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onStartClick,
                            enabled = !timerState.isRunning,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("focus_start_button"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF041E2B),
                                disabledContainerColor = SlateSurfaceHighest,
                                disabledContentColor = SlateTextMuted,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.focus_start),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        OutlinedButton(
                            onClick = onPauseClick,
                            enabled = timerState.isRunning,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("focus_pause_button"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (timerState.isRunning) accentColor else SlateBorder,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = null,
                                tint = if (timerState.isRunning) IceWhite else SlateTextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.focus_pause),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (timerState.isRunning) IceWhite else SlateTextMuted,
                            )
                        }

                        OutlinedButton(
                            onClick = onResetClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("focus_reset_button"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.focus_reset),
                                style = MaterialTheme.typography.labelLarge,
                                color = SlateTextSecondary,
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_sessions_today_card"),
                shape = RoundedCornerShape(20.dp),
                color = MidnightSurface,
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.focus_sessions_today_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = IceWhite,
                        )
                        Text(
                            text = stringResource(
                                R.string.focus_minutes_today_suffix,
                                dailyStats.completedToday * 25,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextSecondary,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CyanContainer,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = "${dailyStats.completedToday}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FocusModeToggleRow(
    selectedMode: FocusMode,
    onSelectMode: (FocusMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MidnightSurface,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val focusSelected = selectedMode == FocusMode.FOCUS
            Surface(
                onClick = { onSelectMode(FocusMode.FOCUS) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("mode_focus_button"),
                shape = RoundedCornerShape(13.dp),
                color = if (focusSelected) CyanContainer else Color.Transparent,
                border = if (focusSelected) BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = null,
                        tint = if (focusSelected) ElectricCyan else SlateTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.focus_mode_focus),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (focusSelected) ElectricCyan else SlateTextSecondary,
                    )
                }
            }

            val breakSelected = selectedMode == FocusMode.SHORT_BREAK
            Surface(
                onClick = { onSelectMode(FocusMode.SHORT_BREAK) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("mode_break_button"),
                shape = RoundedCornerShape(13.dp),
                color = if (breakSelected) AnalogyTealBg else Color.Transparent,
                border = if (breakSelected) BorderStroke(1.dp, AnalogyTeal.copy(alpha = 0.45f)) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Coffee,
                        contentDescription = null,
                        tint = if (breakSelected) AnalogyTeal else SlateTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.focus_mode_break),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (breakSelected) AnalogyTeal else SlateTextSecondary,
                    )
                }
            }
        }
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
