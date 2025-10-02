package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.service.AttributeDefinitionStats
import com.ampairs.customer.domain.service.CustomerAttributeDefinitionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing customer attribute definitions
 */
@RestController
@RequestMapping("/customer/v1/attribute-definitions")
class CustomerAttributeDefinitionController(
    private val attributeDefinitionService: CustomerAttributeDefinitionService
) {

    /**
     * Get all attribute definitions
     */
    @GetMapping("")
    fun getAllDefinitions(): ApiResponse<List<CustomerAttributeDefinitionResponse>> {
        val definitions = attributeDefinitionService.getAllDefinitions()
        return ApiResponse.success(definitions)
    }

    /**
     * Get enabled attribute definitions
     */
    @GetMapping("/enabled")
    fun getEnabledDefinitions(): ApiResponse<List<CustomerAttributeDefinitionResponse>> {
        val definitions = attributeDefinitionService.getEnabledDefinitions()
        return ApiResponse.success(definitions)
    }

    /**
     * Get visible attribute definitions (for frontend display)
     */
    @GetMapping("/visible")
    fun getVisibleDefinitions(): ApiResponse<List<CustomerAttributeDefinitionResponse>> {
        val definitions = attributeDefinitionService.getVisibleDefinitions()
        return ApiResponse.success(definitions)
    }

    /**
     * Get attribute definition by UID
     */
    @GetMapping("/{uid}")
    fun getDefinitionByUid(@PathVariable uid: String): ApiResponse<CustomerAttributeDefinitionResponse> {
        val definition = attributeDefinitionService.getDefinitionByUid(uid)
        return ApiResponse.success(definition)
    }

    /**
     * Get attribute definition by attribute key
     */
    @GetMapping("/by-key/{attributeKey}")
    fun getDefinitionByAttributeKey(@PathVariable attributeKey: String): ApiResponse<CustomerAttributeDefinitionResponse> {
        val definition = attributeDefinitionService.getDefinitionByAttributeKey(attributeKey)
            ?: return ApiResponse.error("Attribute definition not found for key: $attributeKey", "ATTRIBUTE_DEF_NOT_FOUND")
        return ApiResponse.success(definition)
    }

    /**
     * Get attribute definitions by category
     */
    @GetMapping("/by-category/{category}")
    fun getDefinitionsByCategory(@PathVariable category: String): ApiResponse<List<CustomerAttributeDefinitionResponse>> {
        val definitions = attributeDefinitionService.getDefinitionsByCategory(category)
        return ApiResponse.success(definitions)
    }

    /**
     * Get attribute definitions by data type
     */
    @GetMapping("/by-data-type/{dataType}")
    fun getDefinitionsByDataType(@PathVariable dataType: String): ApiResponse<List<CustomerAttributeDefinitionResponse>> {
        val definitions = attributeDefinitionService.getDefinitionsByDataType(dataType)
        return ApiResponse.success(definitions)
    }

    /**
     * Get all categories
     */
    @GetMapping("/categories")
    fun getAllCategories(): ApiResponse<List<String>> {
        val categories = attributeDefinitionService.getAllCategories()
        return ApiResponse.success(categories)
    }

    /**
     * Get attribute definitions grouped by category
     */
    @GetMapping("/grouped-by-category")
    fun getDefinitionsByCategories(): ApiResponse<Map<String, List<CustomerAttributeDefinitionResponse>>> {
        val grouped = attributeDefinitionService.getDefinitionsByCategories()
        return ApiResponse.success(grouped)
    }

    /**
     * Create new attribute definition
     */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createDefinition(@Valid @RequestBody request: CustomerAttributeDefinitionCreateRequest): ApiResponse<CustomerAttributeDefinitionResponse> {
        val definition = attributeDefinitionService.createDefinition(request)
        return ApiResponse.success(definition)
    }

    /**
     * Update attribute definition
     */
    @PutMapping("/{uid}")
    fun updateDefinition(
        @PathVariable uid: String,
        @Valid @RequestBody request: CustomerAttributeDefinitionUpdateRequest
    ): ApiResponse<CustomerAttributeDefinitionResponse> {
        val definition = attributeDefinitionService.updateDefinition(uid, request)
        return ApiResponse.success(definition)
    }

    /**
     * Delete attribute definition
     */
    @DeleteMapping("/{uid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteDefinition(@PathVariable uid: String): ApiResponse<Unit> {
        attributeDefinitionService.deleteDefinition(uid)
        return ApiResponse.success(Unit)
    }

    /**
     * Batch update attribute definitions
     */
    @PostMapping("/batch")
    fun batchUpdateDefinitions(@Valid @RequestBody request: CustomerAttributeDefinitionBatchUpdateRequest): ApiResponse<List<CustomerAttributeDefinitionResponse>> {
        val definitions = attributeDefinitionService.batchUpdateDefinitions(request)
        return ApiResponse.success(definitions)
    }

    /**
     * Get attribute definition statistics
     */
    @GetMapping("/stats")
    fun getStats(): ApiResponse<AttributeDefinitionStats> {
        val stats = attributeDefinitionService.getStats()
        return ApiResponse.success(stats)
    }
}
