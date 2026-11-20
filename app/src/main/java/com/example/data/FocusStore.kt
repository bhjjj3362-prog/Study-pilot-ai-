package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.focusStatsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "studypilot_focus"
)

data class FocusDailyStats(
    val completedToday: Int = 0,
    val dateIso: String = "",
)

internal fun computeFocusSessionsForToday(
    savedCount: Int,
    savedDateIso: String,
    todayIso: String,
): Int = if (savedDateIso == todayIso && savedCount > 0) savedCount else 0

class FocusStore(
    private val context: Context,
) {
    private val sessionsCountKey = intPreferencesKey("focus_sessions_count")
    private val sessionsDateKey = stringPreferencesKey("focus_sessions_date_iso")

    val focusStatsFlow: Flow<FocusDailyStats> = context.focusStatsDataStore.data.map { prefs ->
        val today = StudyDateUtils.todayIso()
        val count = computeFocusSessionsForToday(
            savedCount = prefs[sessionsCountKey] ?: 0,
            savedDateIso = prefs[sessionsDateKey].orEmpty(),
            todayIso = today,
        )
        FocusDailyStats(
            completedToday = count,
            dateIso = today,
        )
    }

    suspend fun incrementCompletedSessionToday(nowMillis: Long = System.currentTimeMillis()) {
        val today = StudyDateUtils.todayIso(nowMillis)
        context.focusStatsDataStore.edit { prefs ->
            val currentToday = computeFocusSessionsForToday(
                savedCount = prefs[sessionsCountKey] ?: 0,
                savedDateIso = prefs[sessionsDateKey].orEmpty(),
                todayIso = today,
            )
            prefs[sessionsCountKey] = currentToday + 1
            prefs[sessionsDateKey] = today
        }
    }
}
