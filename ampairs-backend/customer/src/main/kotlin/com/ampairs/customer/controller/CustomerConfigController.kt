package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.service.CustomerConfigSchema
import com.ampairs.customer.domain.service.CustomerConfigValidationService
import org.springframework.web.bind.annotation.*

/**
 * Unified controller for customer configuration schema
 * Provides frontend with complete field and attribute configuration
 */
@RestController
@RequestMapping("/customer/v1/config")
class CustomerConfigController(
    private val customerConfigValidationService: CustomerConfigValidationService
) {

    /**
     * Get complete customer configuration schema for frontend
     * Includes visible field configurations and attribute definitions
     */
    @GetMapping("/schema")
    fun getConfigSchema(): ApiResponse<CustomerConfigSchema> {
        val schema = customerConfigValidationService.getFieldConfigSchema()
        return ApiResponse.success(schema)
    }

    /**
     * Get only visible fields and attributes
     */
    @GetMapping("/visible")
    fun getVisibleConfig(): ApiResponse<VisibleConfigResponse> {
        val fields = customerConfigValidationService.getVisibleFieldConfigs()
        val attributes = customerConfigValidationService.getVisibleAttributeDefinitions()

        return ApiResponse.success(
            VisibleConfigResponse(
                fields = fields,
                attributes = attributes
            )
        )
    }

    /**
     * Get only mandatory fields and attributes
     */
    @GetMapping("/mandatory")
    fun getMandatoryConfig(): ApiResponse<MandatoryConfigResponse> {
        val fields = customerConfigValidationService.getMandatoryFieldConfigs()
        val attributes = customerConfigValidationService.getMandatoryAttributeDefinitions()

        return ApiResponse.success(
            MandatoryConfigResponse(
                fields = fields,
                attributes = attributes
            )
        )
    }

    /**
     * Get attribute definitions grouped by category
     */
    @GetMapping("/attributes-by-category")
    fun getAttributesByCategory(): ApiResponse<Map<String, List<com.ampairs.customer.domain.model.CustomerAttributeDefinition>>> {
        val grouped = customerConfigValidationService.getAttributeDefinitionsByCategory()
        return ApiResponse.success(grouped)
    }
}

/**
 * Response for visible configuration
 */
data class VisibleConfigResponse(
    val fields: List<com.ampairs.customer.domain.model.CustomerFieldConfig>,
    val attributes: List<com.ampairs.customer.domain.model.CustomerAttributeDefinition>
)

/**
 * Response for mandatory configuration
 */
data class MandatoryConfigResponse(
    val fields: List<com.ampairs.customer.domain.model.CustomerFieldConfig>,
    val attributes: List<com.ampairs.customer.domain.model.CustomerAttributeDefinition>
)
