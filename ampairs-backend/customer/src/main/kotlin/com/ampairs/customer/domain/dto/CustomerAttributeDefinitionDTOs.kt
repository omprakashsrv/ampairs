package com.ampairs.customer.domain.dto

import com.ampairs.core.validation.SafeString
import com.ampairs.customer.domain.model.CustomerAttributeDefinition
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Request DTO for creating customer attribute definition
 */
data class CustomerAttributeDefinitionCreateRequest(
    @field:NotBlank(message = "Attribute key is required")
    @field:SafeString(maxLength = 100, message = "Attribute key contains invalid characters")
    @field:Size(min = 2, max = 100, message = "Attribute key must be between 2 and 100 characters")
    @field:Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "Attribute key must start with lowercase letter and contain only lowercase letters, numbers and underscores")
    val attributeKey: String,

    @field:NotBlank(message = "Display name is required")
    @field:SafeString(maxLength = 255, message = "Display name contains invalid characters")
    @field:Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
    val displayName: String,

    @field:NotBlank(message = "Data type is required")
    @field:Pattern(regexp = "^(STRING|NUMBER|BOOLEAN|DATE|ENUM|JSON)$", message = "Data type must be one of: STRING, NUMBER, BOOLEAN, DATE, ENUM, JSON")
    val dataType: String = "STRING",

    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,

    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int = 0,

    @field:SafeString(maxLength = 100, message = "Category contains invalid characters")
    val category: String? = null,

    @field:SafeString(maxLength = 500, message = "Default value contains invalid characters")
    val defaultValue: String? = null,

    @field:SafeString(maxLength = 50, message = "Validation type contains invalid characters")
    val validationType: String? = null,

    val validationParams: Map<String, Any>? = null,
    val enumValues: List<String>? = null,

    @field:SafeString(maxLength = 255, message = "Placeholder contains invalid characters")
    val placeholder: String? = null,

    @field:SafeString(maxLength = 500, message = "Help text contains invalid characters")
    val helpText: String? = null
)

/**
 * Request DTO for updating customer attribute definition
 */
data class CustomerAttributeDefinitionUpdateRequest(
    @field:SafeString(maxLength = 255, message = "Display name contains invalid characters")
    @field:Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
    val displayName: String?,

    @field:Pattern(regexp = "^(STRING|NUMBER|BOOLEAN|DATE|ENUM|JSON)$", message = "Data type must be one of: STRING, NUMBER, BOOLEAN, DATE, ENUM, JSON")
    val dataType: String?,

    val visible: Boolean?,
    val mandatory: Boolean?,
    val enabled: Boolean?,

    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int?,

    @field:SafeString(maxLength = 100, message = "Category contains invalid characters")
    val category: String?,

    @field:SafeString(maxLength = 500, message = "Default value contains invalid characters")
    val defaultValue: String?,

    @field:SafeString(maxLength = 50, message = "Validation type contains invalid characters")
    val validationType: String?,

    val validationParams: Map<String, Any>?,
    val enumValues: List<String>?,

    @field:SafeString(maxLength = 255, message = "Placeholder contains invalid characters")
    val placeholder: String?,

    @field:SafeString(maxLength = 500, message = "Help text contains invalid characters")
    val helpText: String?
)

/**
 * Response DTO for customer attribute definition
 */
data class CustomerAttributeDefinitionResponse(
    val uid: String,
    val attributeKey: String,
    val displayName: String,
    val dataType: String,
    val visible: Boolean,
    val mandatory: Boolean,
    val enabled: Boolean,
    val displayOrder: Int,
    val category: String?,
    val defaultValue: String?,
    val validationType: String?,
    val validationParams: Map<String, Any>?,
    val enumValues: List<String>?,
    val placeholder: String?,
    val helpText: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

/**
 * Request for batch update of attribute definitions
 */
data class CustomerAttributeDefinitionBatchUpdateRequest(
    val definitions: List<CustomerAttributeDefinitionCreateRequest>
)

/**
 * Extension function to convert entity to response
 */
fun CustomerAttributeDefinition.asCustomerAttributeDefinitionResponse(): CustomerAttributeDefinitionResponse {
    return CustomerAttributeDefinitionResponse(
        uid = this.uid,
        attributeKey = this.attributeKey,
        displayName = this.displayName,
        dataType = this.dataType,
        visible = this.visible,
        mandatory = this.mandatory,
        enabled = this.enabled,
        displayOrder = this.displayOrder,
        category = this.category,
        defaultValue = this.defaultValue,
        validationType = this.validationType,
        validationParams = this.validationParams,
        enumValues = this.enumValues,
        placeholder = this.placeholder,
        helpText = this.helpText,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Extension function to convert list of entities to responses
 */
fun List<CustomerAttributeDefinition>.asCustomerAttributeDefinitionResponses(): List<CustomerAttributeDefinitionResponse> {
    return map { it.asCustomerAttributeDefinitionResponse() }
}

/**
 * Extension function to convert request to entity
 */
fun CustomerAttributeDefinitionCreateRequest.toCustomerAttributeDefinition(): CustomerAttributeDefinition {
    return CustomerAttributeDefinition().apply {
        attributeKey = this@toCustomerAttributeDefinition.attributeKey
        displayName = this@toCustomerAttributeDefinition.displayName
        dataType = this@toCustomerAttributeDefinition.dataType
        visible = this@toCustomerAttributeDefinition.visible
        mandatory = this@toCustomerAttributeDefinition.mandatory
        enabled = this@toCustomerAttributeDefinition.enabled
        displayOrder = this@toCustomerAttributeDefinition.displayOrder
        category = this@toCustomerAttributeDefinition.category
        defaultValue = this@toCustomerAttributeDefinition.defaultValue
        validationType = this@toCustomerAttributeDefinition.validationType
        validationParams = this@toCustomerAttributeDefinition.validationParams
        enumValues = this@toCustomerAttributeDefinition.enumValues
        placeholder = this@toCustomerAttributeDefinition.placeholder
        helpText = this@toCustomerAttributeDefinition.helpText
    }
}
