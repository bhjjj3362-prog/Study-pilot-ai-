package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "SolveRepository"
private const val REQUEST_TIMEOUT_MS = 35_000L
const val MAX_RETRY_ATTEMPTS = 2
const val TOTAL_MAX_ATTEMPTS = 1 + MAX_RETRY_ATTEMPTS

interface SolveRepository {
    suspend fun solveDoubt(
        question: String,
        image: Bitmap?,
        selectedModel: GeminiModelOption,
        onProgress: (SolveAttemptProgress) -> Unit = {},
    ): SolveResult
}

internal fun isRetryableBusyOrTimeoutError(throwable: Throwable): Boolean {
    return generateSequence(throwable) { it.cause }.any { error ->
        when (error) {
            is TimeoutCancellationException,
            is SocketTimeoutException,
            is TimeoutException,
            is InterruptedIOException -> true
            is CancellationException -> false
            else -> {
                val msg = error.message.orEmpty().lowercase()
                val className = error::class.java.simpleName.lowercase()
                msg.contains("429") ||
                    msg.contains("rate limit") ||
                    msg.contains("too many requests") ||
                    msg.contains("resource_exhausted") ||
                    msg.contains("quota") ||
                    msg.contains("503") ||
                    msg.contains("service unavailable") ||
                    msg.contains("unavailable") ||
                    msg.contains("overloaded") ||
                    msg.contains("heavy load") ||
                    msg.contains("busy") ||
                    msg.contains("timeout") ||
                    msg.contains("timed out") ||
                    msg.contains("deadline_exceeded") ||
                    className.contains("timeout") ||
                    className.contains("quota") ||
                    className.contains("serverexception")
            }
        }
    }
}

internal suspend fun <T> executeWithRetryAndFallback(
    modelsInOrder: List<GeminiModelOption>,
    maxRetriesPerModel: Int = MAX_RETRY_ATTEMPTS,
    initialDelayMs: Long = 900L,
    onProgress: (SolveAttemptProgress) -> Unit = {},
    delayFn: suspend (Long) -> Unit = { delay(it) },
    logWarning: (String, Throwable) -> Unit = { msg, err -> Log.w(TAG, msg, err) },
    invokeModel: suspend (GeminiModelOption) -> T,
): Pair<GeminiModelOption, T> {
    require(modelsInOrder.isNotEmpty()) { "At least one model must be configured." }

    val totalAttemptsPerModel = 1 + maxRetriesPerModel
    var lastBusyOrTimeoutError: Throwable? = null

    for ((modelIndex, modelOption) in modelsInOrder.withIndex()) {
        val isFallback = modelIndex > 0
        for (attempt in 1..totalAttemptsPerModel) {
            onProgress(
                SolveAttemptProgress(
                    modelId = modelOption.modelId,
                    attempt = attempt,
                    maxAttempts = totalAttemptsPerModel,
                    isFallback = isFallback,
                )
            )
            try {
                val result = invokeModel(modelOption)
                return modelOption to result
            } catch (e: TimeoutCancellationException) {
                lastBusyOrTimeoutError = e
                logWarning(
                    "Model ${modelOption.modelId} timed out (attempt $attempt/$totalAttemptsPerModel)",
                    e,
                )
                if (attempt < totalAttemptsPerModel) {
                    delayFn(initialDelayMs * attempt)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isRetryableBusyOrTimeoutError(e)) {
                    lastBusyOrTimeoutError = e
                    logWarning(
                        "Model ${modelOption.modelId} busy/rate-limited (attempt $attempt/$totalAttemptsPerModel)",
                        e,
                    )
                    if (attempt < totalAttemptsPerModel) {
                        delayFn(initialDelayMs * attempt)
                    }
                } else {
                    throw e
                }
            }
        }
    }

    throw lastBusyOrTimeoutError
        ?: IllegalStateException("All Gemini models are currently busy. Please try again shortly.")
}

class FirebaseSolveRepository : SolveRepository {

    private val solveSchema = Schema.obj(
        properties = mapOf(
            "analogy" to Schema.string(
                description = "An intuitive, vivid analogy explaining the core concept in exactly two sentences."
            ),
            "stepByStepLogic" to Schema.array(
                items = Schema.string(),
                description = "Ordered, concise logical steps walking through the solution from first principles."
            ),
            "commonExamTrap" to Schema.string(
                description = "A concrete mistake, edge case, or misconception students often fall for on exams, plus how to avoid it."
            ),
        )
    )

    private val modelCache = ConcurrentHashMap<String, GenerativeModel>()

    private fun getOrCreateModel(modelId: String): GenerativeModel {
        return modelCache.getOrPut(modelId) {
            Firebase.ai.generativeModel(
                modelName = modelId,
                generationConfig = generationConfig {
                    temperature = 0.35f
                    responseMimeType = "application/json"
                    responseSchema = solveSchema
                },
                systemInstruction = content {
                    text(
                        "You are StudyPilot, a concise academic tutor. Students may provide text, a photo of a textbook page or handwritten problem, or both. " +
                            "For every problem, return structured JSON with: " +
                            "(1) 'analogy': an intuitive explanation in exactly two clear sentences; " +
                            "(2) 'stepByStepLogic': 3 to 5 crisp, sequential reasoning steps from first principles (plain text, no leading numbering like '1.'); " +
                            "(3) 'commonExamTrap': the #1 exam trap or misconception students fall into for this topic and how to spot it."
                    )
                },
            )
        }
    }

    override suspend fun solveDoubt(
        question: String,
        image: Bitmap?,
        selectedModel: GeminiModelOption,
        onProgress: (SolveAttemptProgress) -> Unit,
    ): SolveResult {
        val trimmedQuestion = question.trim()
        require(trimmedQuestion.isNotEmpty() || image != null) {
            "Enter a question or attach a photo to solve."
        }

        val modelsOrder = GeminiModelOption.fallbackOrderStartingFrom(selectedModel)
        val promptText = buildPromptText(trimmedQuestion, hasImage = image != null)

        val (answeredModel, answer) = executeWithRetryAndFallback(
            modelsInOrder = modelsOrder,
            maxRetriesPerModel = MAX_RETRY_ATTEMPTS,
            onProgress = onProgress,
        ) { modelOption ->
            val model = getOrCreateModel(modelOption.modelId)
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                model.generateContent(
                    content {
                        if (image != null) {
                            image(image)
                        }
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")
            parseSolveAnswer(rawJson)
        }

        return SolveResult(
            answer = answer,
            requestedModelId = selectedModel.modelId,
            answeredModelId = answeredModel.modelId,
        )
    }

    private fun buildPromptText(trimmedQuestion: String, hasImage: Boolean): String {
        return when {
            hasImage && trimmedQuestion.isEmpty() ->
                "Read the problem or diagram in the attached photo carefully and solve it step by step."
            hasImage ->
                "Student's question/note: $trimmedQuestion\n\nUse both the attached photo and the student's note to solve this problem."
            else -> trimmedQuestion
        }
    }

    private fun parseSolveAnswer(rawJson: String): SolveAnswer {
        val json = JSONObject(rawJson)
        val analogy = json.optString("analogy").trim()
        val stepsArray = json.optJSONArray("stepByStepLogic")
        val steps = buildList {
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val step = stepsArray.optString(i).trim()
                    if (step.isNotEmpty()) add(step)
                }
            }
        }
        val commonTrap = json.optString("commonExamTrap").trim()

        if (analogy.isEmpty() || steps.isEmpty() || commonTrap.isEmpty()) {
            throw IllegalStateException("Incomplete structured response received from Gemini.")
        }

        return SolveAnswer(
            analogy = analogy,
            stepByStepLogic = steps,
            commonExamTrap = commonTrap,
        )
    }
}
