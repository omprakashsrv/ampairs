package com.ampairs.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ampairs.common.concurrency.Volatile
import com.ampairs.common.concurrency.synchronized

/**
 * Theme manager for handling app-wide theme state
 */
class ThemeManager {
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    fun setThemePreference(preference: ThemePreference) {
        _themePreference.value = preference
        // TODO: Persist preference to local storage
    }

    @Composable
    fun isDarkTheme(): Boolean {
        val systemInDarkTheme = isSystemInDarkTheme()
        val preference by themePreference.collectAsState()

        return when (preference) {
            ThemePreference.SYSTEM -> systemInDarkTheme
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ThemeManager? = null

        fun getInstance(): ThemeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeManager().also { INSTANCE = it }
            }
        }
    }
}