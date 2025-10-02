package com.ampairs.form.data.repository

import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.domain.EntityConfigSchema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing entity configuration schemas
 * Provides caching and reactive updates per entity type
 */
class ConfigRepository(
    private val api: ConfigApi
) {
    // Cache configs by entity type
    private val _configCache = MutableStateFlow<Map<String, EntityConfigSchema>>(emptyMap())

    /**
     * Get config schema for specific entity type
     * Fetches from backend and updates cache
     */
    suspend fun getConfigSchema(entityType: String): Result<EntityConfigSchema> {
        return try {
            val schema = api.getConfigSchema(entityType)
            updateCache(entityType, schema)
            Result.success(schema)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe config schema for specific entity type
     * Returns Flow that emits cached value
     */
    fun observeConfigSchema(entityType: String): Flow<EntityConfigSchema?> {
        return _configCache.map { cache -> cache[entityType] }
    }

    /**
     * Refresh config from backend
     */
    suspend fun refreshConfig(entityType: String): Result<EntityConfigSchema> {
        return getConfigSchema(entityType)
    }

    /**
     * Preload configs for multiple entity types
     * Useful for app initialization
     */
    suspend fun preloadConfigs(entityTypes: List<String>): Result<Unit> {
        return try {
            entityTypes.forEach { type ->
                getConfigSchema(type)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all cached configs
     */
    fun getAllCachedConfigs(): Map<String, EntityConfigSchema> {
        return _configCache.value
    }

    /**
     * Clear cache for specific entity type
     */
    fun clearCache(entityType: String) {
        _configCache.value = _configCache.value - entityType
    }

    /**
     * Clear all cached configs
     */
    fun clearAllCache() {
        _configCache.value = emptyMap()
    }

    /**
     * Update cache with new schema
     */
    private fun updateCache(entityType: String, schema: EntityConfigSchema) {
        _configCache.value = _configCache.value + (entityType to schema)
    }
}
