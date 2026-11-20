package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studyPilotDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "studypilot_preferences"
)

class ModelPreferenceStore(
    private val context: Context,
) {
    private val selectedModelKey = stringPreferencesKey("selected_gemini_model_id")

    val selectedModelFlow: Flow<GeminiModelOption> = context.studyPilotDataStore.data.map { prefs ->
        GeminiModelOption.fromModelId(prefs[selectedModelKey])
    }

    suspend fun saveSelectedModel(model: GeminiModelOption) {
        context.studyPilotDataStore.edit { prefs ->
            prefs[selectedModelKey] = model.modelId
        }
    }
}
