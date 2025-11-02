package com.ampairs.customer.domain.dto

import com.ampairs.core.validation.SafeString
import com.ampairs.customer.domain.model.CustomerType
import jakarta.validation.constraints.*

data class CustomerTypeCreateRequest(
    @field:NotBlank(message = "Type code is required")
    @field:SafeString(maxLength = 20, message = "Type code contains invalid characters")
    @field:Size(min = 2, max = 20, message = "Type code must be between 2 and 20 characters")
    @field:Pattern(regexp = "^[A-Z0-9_]+$", message = "Type code must contain only uppercase letters, numbers and underscores")
    val typeCode: String,

    @field:NotBlank(message = "Name is required")
    @field:SafeString(maxLength = 100, message = "Name contains invalid characters")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val name: String,

    @field:SafeString(maxLength = 255, message = "Description contains invalid characters")
    @field:Size(max = 255, message = "Description cannot exceed 255 characters")
    val description: String? = null,

    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int = 0,

    val active: Boolean = true,

    @field:DecimalMin(value = "0.0", message = "Default credit limit must be non-negative")
    val defaultCreditLimit: Double = 0.0,

    @field:Min(value = 0, message = "Default credit days must be non-negative")
    val defaultCreditDays: Int = 0,

    val metadata: String? = null
)

data class CustomerTypeUpdateRequest(
    @field:SafeString(maxLength = 100, message = "Name contains invalid characters")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val name: String?,

    @field:SafeString(maxLength = 255, message = "Description contains invalid characters")
    @field:Size(max = 255, message = "Description cannot exceed 255 characters")
    val description: String?,

    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int?,

    val active: Boolean?,

    @field:DecimalMin(value = "0.0", message = "Default credit limit must be non-negative")
    val defaultCreditLimit: Double?,

    @field:Min(value = 0, message = "Default credit days must be non-negative")
    val defaultCreditDays: Int?,

    val metadata: String?
)

fun CustomerTypeCreateRequest.toCustomerType(): CustomerType {
    return CustomerType().apply {
        typeCode = this@toCustomerType.typeCode.uppercase()
        name = this@toCustomerType.name
        description = this@toCustomerType.description
        displayOrder = this@toCustomerType.displayOrder
        active = this@toCustomerType.active
        defaultCreditLimit = this@toCustomerType.defaultCreditLimit
        defaultCreditDays = this@toCustomerType.defaultCreditDays
        metadata = this@toCustomerType.metadata
    }
}