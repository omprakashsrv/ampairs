package com.ampairs.form.data.api

import com.ampairs.form.domain.EntityConfigSchema

/**
 * API interface for form configuration
 */
interface ConfigApi {
    /**
     * Get configuration schema for specific entity type
     * @param entityType: "customer", "product", "inventory", etc.
     */
    suspend fun getConfigSchema(entityType: String): EntityConfigSchema

    /**
     * Get all configuration schemas (for admin/settings screens)
     */
    suspend fun getAllConfigSchemas(): List<EntityConfigSchema>
}
