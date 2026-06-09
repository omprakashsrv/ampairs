package com.ampairs.form.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.form.domain.dto.*
import com.ampairs.form.domain.service.ConfigService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

/**
 * REST controller for managing entity configuration schemas
 * Base path: /api/v1/form
 */
@RestController
@RequestMapping("/form/v1/config")
class ConfigController(
    private val configService: ConfigService
) {

    /**
     * Get configuration schema for a specific entity type
     * GET /api/v1/form/schema?entity_type=customer
     */
    @GetMapping("/schema")
    fun getConfigSchema(
        @RequestParam("entity_type") entityType: String
    ): ApiResponse<EntityConfigSchemaResponse> {
        val schema = configService.getConfigSchema(entityType)
        return ApiResponse.success(schema)
    }

    /**
     * Get all configuration schemas
     * GET /api/v1/form/schemas
     */
    @GetMapping("/schemas")
    fun getAllConfigSchemas(): ApiResponse<List<EntityConfigSchemaResponse>> {
        val schemas = configService.getAllConfigSchemas()
        return ApiResponse.success(schemas)
    }

    /**
     * Bulk create/update configuration schema (field configs + attribute definitions)
     * POST /api/v1/form/config
     * Used for initializing defaults or bulk updates from frontend
     */
    @PostMapping("/config")
    fun saveConfigSchema(
        @Valid @RequestBody request: EntityConfigSchemaRequest
    ): ApiResponse<EntityConfigSchemaResponse> {
        val saved = configService.saveConfigSchema(request)
        return ApiResponse.success(saved)
    }

    /**
     * Create or update field configuration
     * POST /api/v1/form/field-config
     */
    @PostMapping("/field-config")
    fun saveFieldConfig(
        @Valid @RequestBody request: FieldConfigRequest
    ): ApiResponse<FieldConfigResponse> {
        val saved = configService.saveFieldConfig(request)
        return ApiResponse.success(saved)
    }

    /**
     * Create or update attribute definition
     * POST /api/v1/form/attribute-definition
     */
    @PostMapping("/attribute-definition")
    fun saveAttributeDefinition(
        @Valid @RequestBody request: AttributeDefinitionRequest
    ): ApiResponse<AttributeDefinitionResponse> {
        val saved = configService.saveAttributeDefinition(request)
        return ApiResponse.success(saved)
    }

    // ----------------------------------------------------------------------------------------
    // Unified sync contract — two feeds (one per record type), pull + push share the same URL.
    //
    // PULL  GET  /form/v1/config/{field-configs|attribute-definitions}/sync
    //            ?last_sync&page&size&sort_by=updatedAt&sort_dir=ASC → ApiResponse<PageResponse<T>>
    // PUSH  POST /form/v1/config/{field-configs|attribute-definitions}/sync
    //            body List<...SyncRequest> → ApiResponse<List<...Response>> (UID-keyed bulk upsert)
    //
    // LIMITATION: the form tables have no soft-delete column, so the pull feed never returns DELETED
    // rows and the push cannot delete. See ConfigService sync section.
    // ----------------------------------------------------------------------------------------

    /** Incremental sync feed (pull) for field configs, INCLUDING all rows updated at/after last_sync. */
    @GetMapping("/field-configs/sync")
    fun getFieldConfigsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<FieldConfigResponse>> {
        val jpaPropertyName = when (sortBy) {
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            "displayOrder" -> "displayOrder"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        val result = configService.getFieldConfigsAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(result))
    }

    /** Bulk upsert (push) of field configs. UID-keyed; the server upserts each row. */
    @PostMapping("/field-configs/sync")
    fun pushFieldConfigsSync(
        @RequestBody requests: List<FieldConfigSyncRequest>
    ): ApiResponse<List<FieldConfigResponse>> =
        ApiResponse.success(configService.bulkUpsertFieldConfigs(requests))

    /** Incremental sync feed (pull) for attribute definitions, INCLUDING all rows updated at/after last_sync. */
    @GetMapping("/attribute-definitions/sync")
    fun getAttributeDefinitionsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<AttributeDefinitionResponse>> {
        val jpaPropertyName = when (sortBy) {
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            "displayOrder" -> "displayOrder"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        val result = configService.getAttributeDefinitionsAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(result))
    }

    /** Bulk upsert (push) of attribute definitions. UID-keyed; the server upserts each row. */
    @PostMapping("/attribute-definitions/sync")
    fun pushAttributeDefinitionsSync(
        @RequestBody requests: List<AttributeDefinitionSyncRequest>
    ): ApiResponse<List<AttributeDefinitionResponse>> =
        ApiResponse.success(configService.bulkUpsertAttributeDefinitions(requests))

    /**
     * Delete field configuration
     * DELETE /api/v1/form/field-config?entity_type=customer&field_name=email
     */
    @DeleteMapping("/field-config")
    fun deleteFieldConfig(
        @RequestParam("entity_type") entityType: String,
        @RequestParam("field_name") fieldName: String
    ): ApiResponse<Unit> {
        configService.deleteFieldConfig(entityType, fieldName)
        return ApiResponse.success(Unit)
    }

    /**
     * Delete attribute definition
     * DELETE /api/v1/form/attribute-definition?entity_type=customer&attribute_key=loyalty_tier
     */
    @DeleteMapping("/attribute-definition")
    fun deleteAttributeDefinition(
        @RequestParam("entity_type") entityType: String,
        @RequestParam("attribute_key") attributeKey: String
    ): ApiResponse<Unit> {
        configService.deleteAttributeDefinition(entityType, attributeKey)
        return ApiResponse.success(Unit)
    }
}
