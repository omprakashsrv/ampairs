package com.ampairs.common.config

import com.ampairs.common.theme.ThemePreference
import kotlinx.coroutines.flow.Flow

/**
 * DataStore-based app preferences storage (theme, settings, configs)
 * This interface abstracts the DataStore implementation for easier testing and platform compatibility
 */
interface AppPreferencesDataStore {

    /**
     * Get the current theme preference as a Flow
     */
    fun getThemePreference(): Flow<ThemePreference>

    /**
     * Set the theme preference
     */
    suspend fun setThemePreference(preference: ThemePreference)

    // Future app settings can be added here:
    // fun getLanguagePreference(): Flow<String>
    // suspend fun setLanguagePreference(language: String)
    // fun getNotificationSettings(): Flow<NotificationSettings>
    // suspend fun setNotificationSettings(settings: NotificationSettings)
}

/**
 * Default theme preference when none is stored
 */
const val DEFAULT_THEME_PREFERENCE = "SYSTEM"