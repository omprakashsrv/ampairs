package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.service.CustomerFieldConfigService
import com.ampairs.customer.domain.service.FieldConfigStats
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing customer field configurations
 */
@RestController
@RequestMapping("/customer/v1/field-configs")
class CustomerFieldConfigController(
    private val fieldConfigService: CustomerFieldConfigService
) {

    /**
     * Get all field configurations
     */
    @GetMapping("")
    fun getAllConfigs(): ApiResponse<List<CustomerFieldConfigResponse>> {
        val configs = fieldConfigService.getAllConfigs()
        return ApiResponse.success(configs)
    }

    /**
     * Get enabled field configurations
     */
    @GetMapping("/enabled")
    fun getEnabledConfigs(): ApiResponse<List<CustomerFieldConfigResponse>> {
        val configs = fieldConfigService.getEnabledConfigs()
        return ApiResponse.success(configs)
    }

    /**
     * Get visible field configurations (for frontend display)
     */
    @GetMapping("/visible")
    fun getVisibleConfigs(): ApiResponse<List<CustomerFieldConfigResponse>> {
        val configs = fieldConfigService.getVisibleConfigs()
        return ApiResponse.success(configs)
    }

    /**
     * Get field configuration by UID
     */
    @GetMapping("/{uid}")
    fun getConfigByUid(@PathVariable uid: String): ApiResponse<CustomerFieldConfigResponse> {
        val config = fieldConfigService.getConfigByUid(uid)
        return ApiResponse.success(config)
    }

    /**
     * Get field configuration by field name
     */
    @GetMapping("/by-field/{fieldName}")
    fun getConfigByFieldName(@PathVariable fieldName: String): ApiResponse<CustomerFieldConfigResponse> {
        val config = fieldConfigService.getConfigByFieldName(fieldName)
            ?: return ApiResponse.error("Field configuration not found for field: $fieldName", "FIELD_CONFIG_NOT_FOUND")
        return ApiResponse.success(config)
    }

    /**
     * Create new field configuration
     */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createConfig(@Valid @RequestBody request: CustomerFieldConfigCreateRequest): ApiResponse<CustomerFieldConfigResponse> {
        val config = fieldConfigService.createConfig(request)
        return ApiResponse.success(config)
    }

    /**
     * Update field configuration
     */
    @PutMapping("/{uid}")
    fun updateConfig(
        @PathVariable uid: String,
        @Valid @RequestBody request: CustomerFieldConfigUpdateRequest
    ): ApiResponse<CustomerFieldConfigResponse> {
        val config = fieldConfigService.updateConfig(uid, request)
        return ApiResponse.success(config)
    }

    /**
     * Delete field configuration
     */
    @DeleteMapping("/{uid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteConfig(@PathVariable uid: String): ApiResponse<Unit> {
        fieldConfigService.deleteConfig(uid)
        return ApiResponse.success(Unit)
    }

    /**
     * Batch update field configurations
     */
    @PostMapping("/batch")
    fun batchUpdateConfigs(@Valid @RequestBody request: CustomerFieldConfigBatchUpdateRequest): ApiResponse<List<CustomerFieldConfigResponse>> {
        val configs = fieldConfigService.batchUpdateConfigs(request)
        return ApiResponse.success(configs)
    }

    /**
     * Get field configuration statistics
     */
    @GetMapping("/stats")
    fun getStats(): ApiResponse<FieldConfigStats> {
        val stats = fieldConfigService.getStats()
        return ApiResponse.success(stats)
    }
}
