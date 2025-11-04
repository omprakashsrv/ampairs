package com.ampairs.form.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.form.domain.dto.*
import com.ampairs.form.domain.service.ConfigService
import org.springframework.web.bind.annotation.*

/**
 * REST controller for managing entity configuration schemas
 * Base path: /api/v1/form
 */
@RestController
@RequestMapping("/form/v1")
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
        @RequestBody request: EntityConfigSchemaRequest
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
        @RequestBody request: FieldConfigRequest
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
        @RequestBody request: AttributeDefinitionRequest
    ): ApiResponse<AttributeDefinitionResponse> {
        val saved = configService.saveAttributeDefinition(request)
        return ApiResponse.success(saved)
    }

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
