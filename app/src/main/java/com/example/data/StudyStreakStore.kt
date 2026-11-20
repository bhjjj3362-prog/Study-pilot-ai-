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

private val Context.studyStreakDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "studypilot_streak"
)

data class StreakState(
    val currentStreak: Int = 0,
    val lastActiveDateIso: String = "",
    val isActiveToday: Boolean = false,
)

internal fun computeEffectiveStreak(
    savedStreak: Int,
    lastActiveDateIso: String,
    todayIso: String,
    yesterdayIso: String,
): StreakState {
    if (savedStreak <= 0 || lastActiveDateIso.isBlank()) {
        return StreakState(currentStreak = 0, lastActiveDateIso = "", isActiveToday = false)
    }
    return when (lastActiveDateIso) {
        todayIso -> StreakState(
            currentStreak = savedStreak,
            lastActiveDateIso = lastActiveDateIso,
            isActiveToday = true,
        )
        yesterdayIso -> StreakState(
            currentStreak = savedStreak,
            lastActiveDateIso = lastActiveDateIso,
            isActiveToday = false,
        )
        else -> StreakState(
            currentStreak = 0,
            lastActiveDateIso = lastActiveDateIso,
            isActiveToday = false,
        )
    }
}

internal fun computeStreakOnActivity(
    savedStreak: Int,
    lastActiveDateIso: String,
    todayIso: String,
    yesterdayIso: String,
): Pair<Int, String> {
    if (lastActiveDateIso == todayIso && savedStreak > 0) {
        return savedStreak to todayIso
    }
    val nextStreak = if (lastActiveDateIso == yesterdayIso && savedStreak > 0) {
        savedStreak + 1
    } else {
        1
    }
    return nextStreak to todayIso
}

class StudyStreakStore(
    private val context: Context,
) {
    private val streakCountKey = intPreferencesKey("streak_count")
    private val lastActiveDateKey = stringPreferencesKey("last_active_date_iso")

    val streakFlow: Flow<StreakState> = context.studyStreakDataStore.data.map { prefs ->
        val today = StudyDateUtils.todayIso()
        val yesterday = StudyDateUtils.yesterdayIso()
        computeEffectiveStreak(
            savedStreak = prefs[streakCountKey] ?: 0,
            lastActiveDateIso = prefs[lastActiveDateKey].orEmpty(),
            todayIso = today,
            yesterdayIso = yesterday,
        )
    }

    suspend fun recordQualifyingActivity(nowMillis: Long = System.currentTimeMillis()) {
        val today = StudyDateUtils.todayIso(nowMillis)
        val yesterday = StudyDateUtils.yesterdayIso(nowMillis)
        context.studyStreakDataStore.edit { prefs ->
            val currentSaved = prefs[streakCountKey] ?: 0
            val lastDate = prefs[lastActiveDateKey].orEmpty()
            val (nextCount, nextDate) = computeStreakOnActivity(
                savedStreak = currentSaved,
                lastActiveDateIso = lastDate,
                todayIso = today,
                yesterdayIso = yesterday,
            )
            prefs[streakCountKey] = nextCount
            prefs[lastActiveDateKey] = nextDate
        }
    }
}
