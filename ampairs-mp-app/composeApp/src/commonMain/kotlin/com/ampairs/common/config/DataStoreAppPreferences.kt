package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ampairs.common.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-based implementation for app preferences (theme, settings, configs)
 * Works across all platforms with proper file system persistence
 */
class DataStoreAppPreferences(
    private val dataStore: DataStore<Preferences>
) : AppPreferencesDataStore {

    companion object {
        private val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
        // Future preference keys can be added here:
        // private val LANGUAGE_PREFERENCE_KEY = stringPreferencesKey("language_preference")
        // private val NOTIFICATION_SETTINGS_KEY = stringPreferencesKey("notification_settings")
    }

    override fun getThemePreference(): Flow<ThemePreference> {
        return dataStore.data.map { preferences ->
            val preferenceString = preferences[THEME_PREFERENCE_KEY] ?: DEFAULT_THEME_PREFERENCE
            try {
                ThemePreference.valueOf(preferenceString)
            } catch (e: IllegalArgumentException) {
                println("⚠️ Invalid theme preference '$preferenceString', using default")
                ThemePreference.SYSTEM
            }
        }
    }

    override suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = preference.name
        }
    }
}