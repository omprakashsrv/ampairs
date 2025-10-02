package com.ampairs.customer.domain.dto

import com.ampairs.core.validation.SafeString
import com.ampairs.customer.domain.model.CustomerFieldConfig
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Request DTO for creating customer field configuration
 */
data class CustomerFieldConfigCreateRequest(
    @field:NotBlank(message = "Field name is required")
    @field:SafeString(maxLength = 100, message = "Field name contains invalid characters")
    @field:Size(min = 2, max = 100, message = "Field name must be between 2 and 100 characters")
    val fieldName: String,

    @field:NotBlank(message = "Display name is required")
    @field:SafeString(maxLength = 255, message = "Display name contains invalid characters")
    @field:Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
    val displayName: String,

    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,

    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int = 0,

    @field:SafeString(maxLength = 50, message = "Validation type contains invalid characters")
    val validationType: String? = null,

    val validationParams: Map<String, Any>? = null,

    @field:SafeString(maxLength = 255, message = "Placeholder contains invalid characters")
    val placeholder: String? = null,

    @field:SafeString(maxLength = 500, message = "Help text contains invalid characters")
    val helpText: String? = null,

    @field:SafeString(maxLength = 500, message = "Default value contains invalid characters")
    val defaultValue: String? = null
)

/**
 * Request DTO for updating customer field configuration
 */
data class CustomerFieldConfigUpdateRequest(
    @field:SafeString(maxLength = 255, message = "Display name contains invalid characters")
    @field:Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
    val displayName: String?,

    val visible: Boolean?,
    val mandatory: Boolean?,
    val enabled: Boolean?,

    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int?,

    @field:SafeString(maxLength = 50, message = "Validation type contains invalid characters")
    val validationType: String?,

    val validationParams: Map<String, Any>?,

    @field:SafeString(maxLength = 255, message = "Placeholder contains invalid characters")
    val placeholder: String?,

    @field:SafeString(maxLength = 500, message = "Help text contains invalid characters")
    val helpText: String?,

    @field:SafeString(maxLength = 500, message = "Default value contains invalid characters")
    val defaultValue: String?
)

/**
 * Response DTO for customer field configuration
 */
data class CustomerFieldConfigResponse(
    val uid: String,
    val fieldName: String,
    val displayName: String,
    val visible: Boolean,
    val mandatory: Boolean,
    val enabled: Boolean,
    val displayOrder: Int,
    val validationType: String?,
    val validationParams: Map<String, Any>?,
    val placeholder: String?,
    val helpText: String?,
    val defaultValue: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

/**
 * Request for batch update of field configurations
 */
data class CustomerFieldConfigBatchUpdateRequest(
    val configs: List<CustomerFieldConfigCreateRequest>
)

/**
 * Extension function to convert entity to response
 */
fun CustomerFieldConfig.asCustomerFieldConfigResponse(): CustomerFieldConfigResponse {
    return CustomerFieldConfigResponse(
        uid = this.uid,
        fieldName = this.fieldName,
        displayName = this.displayName,
        visible = this.visible,
        mandatory = this.mandatory,
        enabled = this.enabled,
        displayOrder = this.displayOrder,
        validationType = this.validationType,
        validationParams = this.validationParams,
        placeholder = this.placeholder,
        helpText = this.helpText,
        defaultValue = this.defaultValue,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Extension function to convert list of entities to responses
 */
fun List<CustomerFieldConfig>.asCustomerFieldConfigResponses(): List<CustomerFieldConfigResponse> {
    return map { it.asCustomerFieldConfigResponse() }
}

/**
 * Extension function to convert request to entity
 */
fun CustomerFieldConfigCreateRequest.toCustomerFieldConfig(): CustomerFieldConfig {
    return CustomerFieldConfig().apply {
        fieldName = this@toCustomerFieldConfig.fieldName
        displayName = this@toCustomerFieldConfig.displayName
        visible = this@toCustomerFieldConfig.visible
        mandatory = this@toCustomerFieldConfig.mandatory
        enabled = this@toCustomerFieldConfig.enabled
        displayOrder = this@toCustomerFieldConfig.displayOrder
        validationType = this@toCustomerFieldConfig.validationType
        validationParams = this@toCustomerFieldConfig.validationParams
        placeholder = this@toCustomerFieldConfig.placeholder
        helpText = this@toCustomerFieldConfig.helpText
        defaultValue = this@toCustomerFieldConfig.defaultValue
    }
}
