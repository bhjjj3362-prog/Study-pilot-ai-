package com.example.ui.solve

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.FirebaseSolveRepository
import com.example.data.GeminiModelOption
import com.example.data.ImageUtils
import com.example.data.ModelPreferenceStore
import com.example.data.SolveAttemptProgress
import com.example.data.SolveHistoryItem
import com.example.data.SolveHistoryStore
import com.example.data.SolveRepository
import com.example.data.SolveResult
import com.example.data.TOTAL_MAX_ATTEMPTS
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SolveViewModel"

sealed interface SolveUiState {
    data object Idle : SolveUiState
    data class Loading(
        val progress: SolveAttemptProgress,
    ) : SolveUiState
    data class Success(
        val result: SolveResult,
    ) : SolveUiState
    data class Error(
        val message: String,
    ) : SolveUiState
}

class SolveViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository: SolveRepository = FirebaseSolveRepository()
    private val modelPreferenceStore = ModelPreferenceStore(application.applicationContext)
    private val historyStore = SolveHistoryStore(application.applicationContext)

    val selectedModel: StateFlow<GeminiModelOption> = modelPreferenceStore.selectedModelFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GeminiModelOption.DEFAULT,
        )

    val history: StateFlow<List<SolveHistoryItem>> = historyStore.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    private val _questionText = MutableStateFlow("")
    val questionText: StateFlow<String> = _questionText.asStateFlow()

    private val _attachedPhoto = MutableStateFlow<Bitmap?>(null)
    val attachedPhoto: StateFlow<Bitmap?> = _attachedPhoto.asStateFlow()

    private val _uiState = MutableStateFlow<SolveUiState>(SolveUiState.Idle)
    val uiState: StateFlow<SolveUiState> = _uiState.asStateFlow()

    private var activeSolveJob: Job? = null

    fun onQuestionChanged(newText: String) {
        _questionText.value = newText
    }

    fun selectModel(model: GeminiModelOption) {
        viewModelScope.launch {
            modelPreferenceStore.saveSelectedModel(model)
        }
    }

    fun attachPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = ImageUtils.decodeScaledBitmap(getApplication(), uri)
                if (bitmap != null) {
                    _attachedPhoto.value = bitmap
                    if (_uiState.value is SolveUiState.Error) {
                        _uiState.value = SolveUiState.Idle
                    }
                } else {
                    _uiState.value = SolveUiState.Error(
                        getApplication<Application>().getString(R.string.image_load_error)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load selected photo", e)
                _uiState.value = SolveUiState.Error(
                    getApplication<Application>().getString(R.string.image_load_error)
                )
            }
        }
    }

    fun removeAttachedPhoto() {
        _attachedPhoto.value = null
    }

    fun reportCameraUnavailable() {
        _uiState.value = SolveUiState.Error(
            getApplication<Application>().getString(R.string.camera_unavailable_error)
        )
    }

    fun selectExample(exampleQuestion: String) {
        _questionText.value = exampleQuestion
        solve(overrideQuestion = exampleQuestion)
    }

    fun clearQuestion() {
        _questionText.value = ""
        if (_uiState.value is SolveUiState.Error) {
            _uiState.value = SolveUiState.Idle
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyStore.clear()
        }
    }

    fun solve(overrideQuestion: String? = null) {
        val prompt = (overrideQuestion ?: _questionText.value).trim()
        val photo = _attachedPhoto.value
        if (prompt.isEmpty() && photo == null) {
            _uiState.value = SolveUiState.Error("Enter a question or attach a photo first.")
            return
        }

        val currentModel = selectedModel.value
        activeSolveJob?.cancel()
        activeSolveJob = viewModelScope.launch {
            _uiState.value = SolveUiState.Loading(
                SolveAttemptProgress(
                    modelId = currentModel.modelId,
                    attempt = 1,
                    maxAttempts = TOTAL_MAX_ATTEMPTS,
                    isFallback = false,
                )
            )
            try {
                val result = repository.solveDoubt(
                    question = prompt,
                    image = photo,
                    selectedModel = currentModel,
                    onProgress = { progress ->
                        _uiState.value = SolveUiState.Loading(progress)
                    },
                )
                historyStore.saveItem(
                    SolveHistoryItem(
                        id = UUID.randomUUID().toString(),
                        question = prompt,
                        hasPhoto = photo != null,
                        answer = result.answer,
                        requestedModelId = result.requestedModelId,
                        answeredModelId = result.answeredModelId,
                        timestampMillis = System.currentTimeMillis(),
                    )
                )
                _uiState.value = SolveUiState.Success(result = result)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Solve timed out across models", e)
                _uiState.value = SolveUiState.Error(
                    "All models timed out. Check your connection and try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Solve failed", e)
                _uiState.value = SolveUiState.Error(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Could not solve right now. Please try again."
                )
            }
        }
    }
}
