package com.example.ui.recall

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DemoRecallData
import com.example.data.FirebaseRecallRepository
import com.example.data.GeminiModelOption
import com.example.data.ModelPreferenceStore
import com.example.data.RecallRepository
import com.example.data.RecallStudySet
import com.example.data.SolveAttemptProgress
import com.example.data.StudyStreakStore
import com.example.data.TOTAL_MAX_ATTEMPTS
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "RecallViewModel"

sealed interface RecallGenerationState {
    data object Idle : RecallGenerationState
    data class Loading(
        val progress: SolveAttemptProgress,
    ) : RecallGenerationState
    data class Error(
        val message: String,
    ) : RecallGenerationState
}

class RecallViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository: RecallRepository = FirebaseRecallRepository()
    private val modelPreferenceStore = ModelPreferenceStore(application.applicationContext)
    private val streakStore = StudyStreakStore(application.applicationContext)

    val selectedModel: StateFlow<GeminiModelOption> = modelPreferenceStore.selectedModelFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GeminiModelOption.DEFAULT,
        )

    private val _notesText = MutableStateFlow("")
    val notesText: StateFlow<String> = _notesText.asStateFlow()

    private val _generationState = MutableStateFlow<RecallGenerationState>(RecallGenerationState.Idle)
    val generationState: StateFlow<RecallGenerationState> = _generationState.asStateFlow()

    private val _studySet = MutableStateFlow(DemoRecallData.sampleStudySet())
    val studySet: StateFlow<RecallStudySet> = _studySet.asStateFlow()

    private val _flippedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val flippedCardIds: StateFlow<Set<String>> = _flippedCardIds.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<String, Int>>(emptyMap())
    val selectedAnswers: StateFlow<Map<String, Int>> = _selectedAnswers.asStateFlow()

    private var activeGenerateJob: Job? = null

    fun onNotesChanged(newText: String) {
        _notesText.value = newText
    }

    fun clearNotes() {
        _notesText.value = ""
        if (_generationState.value is RecallGenerationState.Error) {
            _generationState.value = RecallGenerationState.Idle
        }
    }

    fun selectModel(model: GeminiModelOption) {
        viewModelScope.launch {
            modelPreferenceStore.saveSelectedModel(model)
        }
    }

    fun toggleFlashcard(cardId: String) {
        _flippedCardIds.update { current ->
            if (cardId in current) current - cardId else current + cardId
        }
    }

    fun selectQuizOption(questionId: String, optionIndex: Int) {
        val currentMap = _selectedAnswers.value
        if (currentMap.containsKey(questionId)) return

        val updatedMap = currentMap + (questionId to optionIndex)
        _selectedAnswers.value = updatedMap

        val totalQuestions = _studySet.value.quizQuestions.size
        if (totalQuestions > 0 && updatedMap.size == totalQuestions) {
            viewModelScope.launch {
                streakStore.recordQualifyingActivity()
            }
        }
    }

    fun resetQuiz() {
        _selectedAnswers.value = emptyMap()
    }

    fun generateStudySet() {
        val trimmedNotes = _notesText.value.trim()
        if (trimmedNotes.isEmpty()) {
            _generationState.value = RecallGenerationState.Error(
                "Paste chapter notes or a topic summary first."
            )
            return
        }

        val currentModel = selectedModel.value
        activeGenerateJob?.cancel()
        activeGenerateJob = viewModelScope.launch {
            _generationState.value = RecallGenerationState.Loading(
                SolveAttemptProgress(
                    modelId = currentModel.modelId,
                    attempt = 1,
                    maxAttempts = TOTAL_MAX_ATTEMPTS,
                    isFallback = false,
                )
            )
            try {
                val generated = repository.generateStudySet(
                    notes = trimmedNotes,
                    selectedModel = currentModel,
                    onProgress = { progress ->
                        _generationState.value = RecallGenerationState.Loading(progress)
                    },
                )
                _studySet.value = generated
                _flippedCardIds.value = emptySet()
                _selectedAnswers.value = emptyMap()
                _generationState.value = RecallGenerationState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Study set generation timed out across models", e)
                _generationState.value = RecallGenerationState.Error(
                    "All models timed out. Check your connection and try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Study set generation failed", e)
                _generationState.value = RecallGenerationState.Error(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Could not generate study set right now. Please try again."
                )
            }
        }
    }
}
