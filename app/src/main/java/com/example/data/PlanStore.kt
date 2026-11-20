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

private const val TAG = "PlanStore"

private val Context.revisionPlanDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "studypilot_plan"
)

internal fun encodeRevisionPlanJson(plan: RevisionPlan): String {
    val tasksArray = JSONArray().apply {
        plan.tasks.forEach { task ->
            put(
                JSONObject().apply {
                    put("id", task.id)
                    put("dayNumber", task.dayNumber)
                    put("subject", task.subject)
                    put("description", task.description)
                    put("hoursLabel", task.hoursLabel)
                    put("isCompleted", task.isCompleted)
                }
            )
        }
    }
    return JSONObject().apply {
        put("examName", plan.examName)
        put("targetDateIso", plan.targetDateIso)
        put("subjectsRaw", plan.subjectsRaw)
        put("dailyHours", plan.dailyHours)
        put("isSample", plan.isSample)
        put("tasks", tasksArray)
    }.toString()
}

internal fun decodeRevisionPlanJson(raw: String): RevisionPlan? {
    if (raw.isBlank()) return null
    return try {
        val obj = JSONObject(raw)
        val examName = obj.optString("examName").trim()
        val targetDateIso = obj.optString("targetDateIso").trim()
        val subjectsRaw = obj.optString("subjectsRaw").trim()
        val dailyHours = obj.optDouble("dailyHours", 2.0)
        val isSample = obj.optBoolean("isSample", false)
        val tasksArray = obj.optJSONArray("tasks") ?: JSONArray()
        val tasks = buildList {
            for (i in 0 until tasksArray.length()) {
                val tObj = tasksArray.optJSONObject(i) ?: continue
                val id = tObj.optString("id").trim()
                val description = tObj.optString("description").trim()
                if (id.isEmpty() || description.isEmpty()) continue
                add(
                    PlanTask(
                        id = id,
                        dayNumber = tObj.optInt("dayNumber", i + 1),
                        subject = tObj.optString("subject").trim(),
                        description = description,
                        hoursLabel = tObj.optString("hoursLabel", "2h"),
                        isCompleted = tObj.optBoolean("isCompleted", false),
                    )
                )
            }
        }
        if (examName.isEmpty() || targetDateIso.isEmpty() || tasks.isEmpty()) return null
        RevisionPlan(
            examName = examName,
            targetDateIso = targetDateIso,
            subjectsRaw = subjectsRaw,
            dailyHours = dailyHours,
            tasks = tasks,
            isSample = isSample,
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode saved revision plan", e)
        null
    }
}

class PlanStore(
    private val context: Context,
) {
    private val planJsonKey = stringPreferencesKey("revision_plan_json")

    val planFlow: Flow<RevisionPlan> = context.revisionPlanDataStore.data.map { prefs ->
        decodeRevisionPlanJson(prefs[planJsonKey].orEmpty()) ?: DemoPlanData.samplePlan()
    }

    suspend fun savePlan(plan: RevisionPlan) {
        context.revisionPlanDataStore.edit { prefs ->
            prefs[planJsonKey] = encodeRevisionPlanJson(plan)
        }
    }

    suspend fun toggleTask(taskId: String): Boolean {
        var becameCompleted = false
        context.revisionPlanDataStore.edit { prefs ->
            val current = decodeRevisionPlanJson(prefs[planJsonKey].orEmpty())
                ?: DemoPlanData.samplePlan()
            val updatedTasks = current.tasks.map { task ->
                if (task.id == taskId) {
                    val nextCompleted = !task.isCompleted
                    becameCompleted = nextCompleted
                    task.copy(isCompleted = nextCompleted)
                } else {
                    task
                }
            }
            prefs[planJsonKey] = encodeRevisionPlanJson(current.copy(tasks = updatedTasks))
        }
        return becameCompleted
    }
}
