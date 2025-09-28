package com.ampairs.common.preferences

/**
 * Simple app preferences interface for sync timestamps
 */
interface AppPreferences {
    suspend fun getLastSyncTime(): String?
    suspend fun setLastSyncTime(timestamp: String)
}

/**
 * Simple in-memory implementation for now
 */
class InMemoryAppPreferences : AppPreferences {
    private var lastSyncTime: String? = null

    override suspend fun getLastSyncTime(): String? = lastSyncTime

    override suspend fun setLastSyncTime(timestamp: String) {
        lastSyncTime = timestamp
    }
}