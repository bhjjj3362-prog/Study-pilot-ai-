package com.example.data

data class FlashcardItem(
    val id: String,
    val question: String,
    val answer: String,
)

data class QuizQuestionItem(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class RecallStudySet(
    val topicTitle: String,
    val flashcards: List<FlashcardItem>,
    val quizQuestions: List<QuizQuestionItem>,
    val requestedModelId: String,
    val answeredModelId: String,
    val isDemo: Boolean = false,
) {
    val usedFallback: Boolean
        get() = !isDemo && requestedModelId != answeredModelId
}

object DemoRecallData {
    fun sampleStudySet(): RecallStudySet = RecallStudySet(
        topicTitle = "Foundations of Science",
        flashcards = listOf(
            FlashcardItem(
                id = "demo-fc-1",
                question = "Why are mitochondria called the powerhouse of the cell?",
                answer = "They drive aerobic respiration to convert glucose and oxygen into usable ATP energy.",
            ),
            FlashcardItem(
                id = "demo-fc-2",
                question = "What does Newton's Third Law state about interacting objects?",
                answer = "Every action force produces an equal-magnitude force acting in the opposite direction on the other object.",
            ),
            FlashcardItem(
                id = "demo-fc-3",
                question = "How does a catalyst speed up a chemical reaction?",
                answer = "It lowers the activation energy barrier without being consumed in the reaction.",
            ),
        ),
        quizQuestions = listOf(
            QuizQuestionItem(
                id = "demo-q-1",
                question = "Which gas is released as a byproduct during the light-dependent reactions of photosynthesis?",
                options = listOf(
                    "Carbon dioxide",
                    "Oxygen",
                    "Nitrogen",
                    "Methane",
                ),
                correctIndex = 1,
                explanation = "Splitting water molecules during the light-dependent stage releases oxygen gas as a byproduct.",
            ),
            QuizQuestionItem(
                id = "demo-q-2",
                question = "If the net external force on a moving object drops to zero, what happens to its velocity?",
                options = listOf(
                    "It stops immediately",
                    "It decreases at a constant rate",
                    "It remains constant in speed and direction",
                    "It reverses direction",
                ),
                correctIndex = 2,
                explanation = "By Newton's First Law, an object maintains constant velocity unless acted on by a nonzero net force.",
            ),
            QuizQuestionItem(
                id = "demo-q-3",
                question = "A solution changes from pH 5 to pH 3. How does its hydrogen ion concentration change?",
                options = listOf(
                    "Increases by 2 times",
                    "Decreases by 2 times",
                    "Increases by 100 times",
                    "Decreases by 100 times",
                ),
                correctIndex = 2,
                explanation = "Because pH is a base-10 logarithmic scale, dropping by 2 pH units increases H+ concentration by 10^2 (100x).",
            ),
        ),
        requestedModelId = GeminiModelOption.DEFAULT.modelId,
        answeredModelId = GeminiModelOption.DEFAULT.modelId,
        isDemo = true,
    )
}
