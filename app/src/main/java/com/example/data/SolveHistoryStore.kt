package com.example.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "SolveHistoryStore"
const val MAX_HISTORY_ITEMS = 5

private val Context.solveHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "studypilot_history"
)

internal fun appendHistoryCapped(
    existing: List<SolveHistoryItem>,
    newItem: SolveHistoryItem,
    maxItems: Int = MAX_HISTORY_ITEMS,
): List<SolveHistoryItem> {
    return (listOf(newItem) + existing.filterNot { it.id == newItem.id }).take(maxItems)
}

internal fun encodeHistoryJson(items: List<SolveHistoryItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        val stepsArray = JSONArray().apply {
            item.answer.stepByStepLogic.forEach { put(it) }
        }
        val answerObj = JSONObject().apply {
            put("analogy", item.answer.analogy)
            put("stepByStepLogic", stepsArray)
            put("commonExamTrap", item.answer.commonExamTrap)
        }
        val obj = JSONObject().apply {
            put("id", item.id)
            put("question", item.question)
            put("hasPhoto", item.hasPhoto)
            put("answer", answerObj)
            put("requestedModelId", item.requestedModelId)
            put("answeredModelId", item.answeredModelId)
            put("timestampMillis", item.timestampMillis)
        }
        array.put(obj)
    }
    return array.toString()
}

internal fun decodeHistoryJson(raw: String): List<SolveHistoryItem> {
    if (raw.isBlank()) return emptyList()
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val answerObj = obj.optJSONObject("answer") ?: continue
                val stepsArray = answerObj.optJSONArray("stepByStepLogic")
                val steps = buildList {
                    if (stepsArray != null) {
                        for (j in 0 until stepsArray.length()) {
                            val step = stepsArray.optString(j).trim()
                            if (step.isNotEmpty()) add(step)
                        }
                    }
                }
                val analogy = answerObj.optString("analogy").trim()
                val trap = answerObj.optString("commonExamTrap").trim()
                if (analogy.isEmpty() || steps.isEmpty() || trap.isEmpty()) continue

                add(
                    SolveHistoryItem(
                        id = obj.optString("id"),
                        question = obj.optString("question"),
                        hasPhoto = obj.optBoolean("hasPhoto", false),
                        answer = SolveAnswer(
                            analogy = analogy,
                            stepByStepLogic = steps,
                            commonExamTrap = trap,
                        ),
                        requestedModelId = obj.optString(
                            "requestedModelId",
                            GeminiModelOption.DEFAULT.modelId,
                        ),
                        answeredModelId = obj.optString(
                            "answeredModelId",
                            GeminiModelOption.DEFAULT.modelId,
                        ),
                        timestampMillis = obj.optLong("timestampMillis", 0L),
                    )
                )
            }
        }.take(MAX_HISTORY_ITEMS)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode saved solve history", e)
        emptyList()
    }
}

class SolveHistoryStore(
    private val context: Context,
) {
    private val historyJsonKey = stringPreferencesKey("solve_history_json")

    val historyFlow: Flow<List<SolveHistoryItem>> = context.solveHistoryDataStore.data.map { prefs ->
        decodeHistoryJson(prefs[historyJsonKey].orEmpty())
    }

    suspend fun saveItem(item: SolveHistoryItem) {
        context.solveHistoryDataStore.edit { prefs ->
            val current = decodeHistoryJson(prefs[historyJsonKey].orEmpty())
            val updated = appendHistoryCapped(current, item, MAX_HISTORY_ITEMS)
            prefs[historyJsonKey] = encodeHistoryJson(updated)
        }
    }

    suspend fun clear() {
        context.solveHistoryDataStore.edit { prefs ->
            prefs.remove(historyJsonKey)
        }
    }
}
