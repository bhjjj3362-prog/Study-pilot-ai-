package com.example.data

enum class StudyTab {
    Solve,
    Recall,
    Plan,
    Focus,
}

enum class GeminiModelOption(
    val modelId: String,
    val badge: String,
) {
    GEMINI_3_5_FLASH_LITE("gemini-3.5-flash-lite", "Default · Fastest"),
    GEMINI_3_1_FLASH_LITE("gemini-3.1-flash-lite", "Fast · Lite"),
    GEMINI_3_6_FLASH("gemini-3.6-flash", "Balanced"),
    GEMINI_3_8_FLASH("gemini-3.8-flash", "Versatile"),
    GEMINI_4_HYDROGEN("gemini-4-hydrogen", "High Reasoning");

    companion object {
        val DEFAULT: GeminiModelOption = GEMINI_3_5_FLASH_LITE

        fun fromModelId(id: String?): GeminiModelOption =
            entries.firstOrNull { it.modelId == id } ?: DEFAULT

        fun fallbackOrderStartingFrom(selected: GeminiModelOption): List<GeminiModelOption> =
            listOf(selected) + entries.filter { it != selected }
    }
}

data class SolveAnswer(
    val analogy: String,
    val stepByStepLogic: List<String>,
    val commonExamTrap: String,
)

data class SolveAttemptProgress(
    val modelId: String,
    val attempt: Int,
    val maxAttempts: Int,
    val isFallback: Boolean,
)

data class SolveResult(
    val answer: SolveAnswer,
    val requestedModelId: String,
    val answeredModelId: String,
) {
    val usedFallback: Boolean
        get() = requestedModelId != answeredModelId
}

data class SolveHistoryItem(
    val id: String,
    val question: String,
    val hasPhoto: Boolean,
    val answer: SolveAnswer,
    val requestedModelId: String,
    val answeredModelId: String,
    val timestampMillis: Long,
) {
    val usedFallback: Boolean
        get() = requestedModelId != answeredModelId
}
