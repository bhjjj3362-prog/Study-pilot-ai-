package com.example.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FocusDailyStats
import com.example.data.FocusStore
import com.example.data.StudyStreakStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class FocusMode(
    val durationSeconds: Int,
) {
    FOCUS(25 * 60),
    SHORT_BREAK(5 * 60),
}

data class FocusTimerUiState(
    val mode: FocusMode = FocusMode.FOCUS,
    val remainingSeconds: Int = FocusMode.FOCUS.durationSeconds,
    val isRunning: Boolean = false,
    val justCompletedFocus: Boolean = false,
) {
    val totalSeconds: Int
        get() = mode.durationSeconds

    val progressFraction: Float
        get() = if (totalSeconds > 0) {
            remainingSeconds.toFloat() / totalSeconds.toFloat()
        } else {
            0f
        }
}

class FocusViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val focusStore = FocusStore(application.applicationContext)
    private val streakStore = StudyStreakStore(application.applicationContext)

    val dailyStats: StateFlow<FocusDailyStats> = focusStore.focusStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FocusDailyStats(),
        )

    private val _timerState = MutableStateFlow(FocusTimerUiState())
    val timerState: StateFlow<FocusTimerUiState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    fun switchMode(newMode: FocusMode) {
        if (_timerState.value.mode == newMode) return
        timerJob?.cancel()
        _timerState.value = FocusTimerUiState(
            mode = newMode,
            remainingSeconds = newMode.durationSeconds,
            isRunning = false,
            justCompletedFocus = false,
        )
    }

    fun startTimer() {
        val current = _timerState.value
        if (current.isRunning) return

        val initialRemaining = if (current.remainingSeconds <= 0) {
            current.mode.durationSeconds
        } else {
            current.remainingSeconds
        }

        _timerState.value = current.copy(
            remainingSeconds = initialRemaining,
            isRunning = true,
            justCompletedFocus = false,
        )

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _timerState.value.remainingSeconds > 0) {
                delay(1_000L)
                val nextRemaining = (_timerState.value.remainingSeconds - 1).coerceAtLeast(0)
                if (nextRemaining == 0) {
                    handleTimerCompleted(_timerState.value.mode)
                    break
                } else {
                    _timerState.value = _timerState.value.copy(remainingSeconds = nextRemaining)
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resetTimer() {
        timerJob?.cancel()
        val mode = _timerState.value.mode
        _timerState.value = FocusTimerUiState(
            mode = mode,
            remainingSeconds = mode.durationSeconds,
            isRunning = false,
            justCompletedFocus = false,
        )
    }

    private suspend fun handleTimerCompleted(completedMode: FocusMode) {
        if (completedMode == FocusMode.FOCUS) {
            focusStore.incrementCompletedSessionToday()
            streakStore.recordQualifyingActivity()
        }
        _timerState.value = FocusTimerUiState(
            mode = completedMode,
            remainingSeconds = completedMode.durationSeconds,
            isRunning = false,
            justCompletedFocus = completedMode == FocusMode.FOCUS,
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
