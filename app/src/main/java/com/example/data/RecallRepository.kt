package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val RECALL_TIMEOUT_MS = 40_000L

interface RecallRepository {
    suspend fun generateStudySet(
        notes: String,
        selectedModel: GeminiModelOption,
        onProgress: (SolveAttemptProgress) -> Unit = {},
    ): RecallStudySet
}

internal fun parseRecallStudySetJson(
    rawJson: String,
    requestedModelId: String,
    answeredModelId: String,
): RecallStudySet {
    val json = JSONObject(rawJson)
    val topicTitle = json.optString("topicTitle").trim().ifEmpty { "Study Set" }

    val cardsArray = json.optJSONArray("flashcards")
    val flashcards = buildList {
        if (cardsArray != null) {
            for (i in 0 until cardsArray.length()) {
                val obj = cardsArray.optJSONObject(i) ?: continue
                val question = obj.optString("question").trim()
                val answer = obj.optString("answer").trim()
                if (question.isNotEmpty() && answer.isNotEmpty()) {
                    add(
                        FlashcardItem(
                            id = "fc-$i-${question.hashCode()}",
                            question = question,
                            answer = answer,
                        )
                    )
                }
            }
        }
    }.take(5)

    val quizArray = json.optJSONArray("quiz")
    val quizQuestions = buildList {
        if (quizArray != null) {
            for (i in 0 until quizArray.length()) {
                val obj = quizArray.optJSONObject(i) ?: continue
                val question = obj.optString("question").trim()
                val explanation = obj.optString("explanation").trim()
                val optionsArray = obj.optJSONArray("options")
                val options = buildList {
                    if (optionsArray != null) {
                        for (j in 0 until optionsArray.length()) {
                            val opt = optionsArray.optString(j).trim()
                            if (opt.isNotEmpty()) add(opt)
                        }
                    }
                }.take(4)

                if (question.isNotEmpty() && options.size >= 2 && explanation.isNotEmpty()) {
                    val rawIndex = obj.optInt("correctIndex", 0)
                    val safeIndex = rawIndex.coerceIn(0, options.lastIndex)
                    add(
                        QuizQuestionItem(
                            id = "quiz-$i-${question.hashCode()}",
                            question = question,
                            options = options,
                            correctIndex = safeIndex,
                            explanation = explanation,
                        )
                    )
                }
            }
        }
    }.take(5)

    if (flashcards.isEmpty() || quizQuestions.isEmpty()) {
        throw IllegalStateException("Incomplete study set received from Gemini.")
    }

    return RecallStudySet(
        topicTitle = topicTitle,
        flashcards = flashcards,
        quizQuestions = quizQuestions,
        requestedModelId = requestedModelId,
        answeredModelId = answeredModelId,
        isDemo = false,
    )
}

class FirebaseRecallRepository : RecallRepository {

    private val flashcardSchema = Schema.obj(
        properties = mapOf(
            "question" to Schema.string(
                description = "A concise active-recall question for the front of the flashcard."
            ),
            "answer" to Schema.string(
                description = "A clear, concise 1-2 sentence answer for the back of the flashcard."
            ),
        )
    )

    private val quizQuestionSchema = Schema.obj(
        properties = mapOf(
            "question" to Schema.string(
                description = "A clear multiple-choice question testing key comprehension."
            ),
            "options" to Schema.array(
                items = Schema.string(),
                description = "Exactly 4 distinct answer choices."
            ),
            "correctIndex" to Schema.integer(
                description = "Zero-based index (0 to 3) of the correct option in the options array."
            ),
            "explanation" to Schema.string(
                description = "Exactly one concise sentence explaining why the correct answer is right."
            ),
        )
    )

    private val recallSchema = Schema.obj(
        properties = mapOf(
            "topicTitle" to Schema.string(
                description = "Short 2 to 5 word title summarizing the study notes."
            ),
            "flashcards" to Schema.array(
                items = flashcardSchema,
                description = "Exactly 5 interactive flashcards."
            ),
            "quiz" to Schema.array(
                items = quizQuestionSchema,
                description = "Exactly 5 multiple-choice quiz questions."
            ),
        )
    )

    private val modelCache = ConcurrentHashMap<String, GenerativeModel>()

    private fun getOrCreateModel(modelId: String): GenerativeModel {
        return modelCache.getOrPut(modelId) {
            Firebase.ai.generativeModel(
                modelName = modelId,
                generationConfig = generationConfig {
                    temperature = 0.4f
                    responseMimeType = "application/json"
                    responseSchema = recallSchema
                },
                systemInstruction = content {
                    text(
                        "You are StudyPilot, an active-recall study designer. Given a student's chapter notes or topic summary, " +
                            "generate structured JSON containing: " +
                            "(1) 'topicTitle': a concise 2-5 word title; " +
                            "(2) 'flashcards': exactly 5 high-yield flashcards (front = 'question', back = short 'answer'); " +
                            "(3) 'quiz': exactly 5 multiple-choice questions, each with exactly 4 distinct 'options', a valid 0-3 'correctIndex', and a crisp 1-sentence 'explanation'."
                    )
                },
            )
        }
    }

    override suspend fun generateStudySet(
        notes: String,
        selectedModel: GeminiModelOption,
        onProgress: (SolveAttemptProgress) -> Unit,
    ): RecallStudySet {
        val trimmedNotes = notes.trim()
        require(trimmedNotes.isNotEmpty()) {
            "Paste chapter notes or a topic summary first."
        }

        val modelsOrder = GeminiModelOption.fallbackOrderStartingFrom(selectedModel)

        val (answeredModel, studySet) = executeWithRetryAndFallback(
            modelsInOrder = modelsOrder,
            maxRetriesPerModel = MAX_RETRY_ATTEMPTS,
            onProgress = onProgress,
        ) { modelOption ->
            val model = getOrCreateModel(modelOption.modelId)
            val response = withTimeout(RECALL_TIMEOUT_MS) {
                model.generateContent(
                    "Create 5 flashcards and a 5-question multiple-choice quiz from these study notes:\n\n$trimmedNotes"
                )
            }
            val rawJson = response.text
                ?: throw IllegalStateException("Gemini returned an empty study set.")
            parseRecallStudySetJson(
                rawJson = rawJson,
                requestedModelId = selectedModel.modelId,
                answeredModelId = modelOption.modelId,
            )
        }

        return studySet.copy(
            requestedModelId = selectedModel.modelId,
            answeredModelId = answeredModel.modelId,
        )
    }
}
