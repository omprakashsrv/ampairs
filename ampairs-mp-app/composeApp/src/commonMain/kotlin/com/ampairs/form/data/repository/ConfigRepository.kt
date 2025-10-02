package com.ampairs.form.data.repository

import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.db.EntityAttributeDefinitionDao
import com.ampairs.form.data.db.EntityFieldConfigDao
import com.ampairs.form.data.db.toEntity
import com.ampairs.form.data.db.toEntityAttributeDefinition
import com.ampairs.form.data.db.toEntityFieldConfig
import com.ampairs.form.domain.EntityConfigSchema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repository for managing entity configuration schemas
 * Provides offline-first architecture with database caching and reactive updates per entity type
 */
class ConfigRepository(
    private val api: ConfigApi,
    private val fieldConfigDao: EntityFieldConfigDao,
    private val attributeDefinitionDao: EntityAttributeDefinitionDao
) {
    // Cache configs by entity type
    private val _configCache = MutableStateFlow<Map<String, EntityConfigSchema>>(emptyMap())

    /**
     * Get config schema for specific entity type
     * Fetches from backend, saves to database, and updates cache
     */
    suspend fun getConfigSchema(entityType: String): Result<EntityConfigSchema> {
        return try {
            val schema = api.getConfigSchema(entityType)

            // Save field configs to database
            val fieldConfigEntities = schema.fieldConfigs.map { it.toEntity() }
            fieldConfigDao.insertFieldConfigs(fieldConfigEntities)

            // Save attribute definitions to database
            val attributeDefinitionEntities = schema.attributeDefinitions.map { it.toEntity() }
            attributeDefinitionDao.insertAttributeDefinitions(attributeDefinitionEntities)

            updateCache(entityType, schema)
            Result.success(schema)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe config schema for specific entity type from database
     * Returns Flow that emits data from local database
     */
    fun observeConfigSchema(entityType: String): Flow<EntityConfigSchema?> {
        return combine(
            fieldConfigDao.getFieldConfigsByEntityType(entityType),
            attributeDefinitionDao.getAttributeDefinitionsByEntityType(entityType)
        ) { fieldConfigs, attributeDefinitions ->
            // Combine field configs and attribute definitions into schema
            if (fieldConfigs.isEmpty() && attributeDefinitions.isEmpty()) {
                null
            } else {
                EntityConfigSchema(
                    fieldConfigs = fieldConfigs.map { it.toEntityFieldConfig() },
                    attributeDefinitions = attributeDefinitions.map { it.toEntityAttributeDefinition() }
                )
            }
        }
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
