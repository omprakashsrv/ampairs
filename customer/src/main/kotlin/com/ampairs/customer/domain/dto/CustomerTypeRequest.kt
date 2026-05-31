package com.ampairs.customer.domain.dto

import com.ampairs.core.validation.SafeString
import com.ampairs.customer.domain.model.CustomerType
import jakarta.validation.constraints.*

data class CustomerTypeCreateRequest(
    /**
     * Optional client-provided UID. If null, auto-generated with "CT" prefix.
     * Must be unique across all customer types in the workspace.
     * Format: alphanumeric with optional underscores, max 200 chars.
     */
    @field:SafeString(maxLength = 200, message = "UID contains invalid characters")
    @field:Size(min = 2, max = 200, message = "UID must be between 2 and 200 characters")
    @field:Pattern(regexp = "^[A-Za-z0-9_]+$", message = "UID must contain only letters, numbers and underscores")
    val uid: String? = null,

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
    @field:NotBlank(message = "Type code is required")
    @field:SafeString(maxLength = 20, message = "Type code contains invalid characters")
    @field:Size(min = 2, max = 20, message = "Type code must be between 2 and 20 characters")
    @field:Pattern(regexp = "^[A-Z0-9_]+$", message = "Type code must contain only uppercase letters, numbers and underscores")
    val typeCode: String? = null,

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

private fun buildCustomerType(
    uid: String? = null,
    typeCode: String,
    name: String,
    description: String?,
    displayOrder: Int,
    active: Boolean,
    defaultCreditLimit: Double,
    defaultCreditDays: Int,
    metadata: String?
): CustomerType = CustomerType().apply {
    uid?.let { this.uid = it }
    this.typeCode = typeCode
    this.name = name
    this.description = description
    this.displayOrder = displayOrder
    this.active = active
    this.defaultCreditLimit = defaultCreditLimit
    this.defaultCreditDays = defaultCreditDays
    this.metadata = metadata
}

fun CustomerTypeCreateRequest.toCustomerType(): CustomerType = buildCustomerType(
    uid = uid,
    typeCode = typeCode.uppercase(),
    name = name,
    description = description,
    displayOrder = displayOrder,
    active = active,
    defaultCreditLimit = defaultCreditLimit,
    defaultCreditDays = defaultCreditDays,
    metadata = metadata
)

fun CustomerTypeUpdateRequest.toCustomerType(): CustomerType = buildCustomerType(
    typeCode = typeCode?.uppercase() ?: "",
    name = name ?: "",
    description = description,
    displayOrder = displayOrder ?: 0,
    active = active ?: true,
    defaultCreditLimit = defaultCreditLimit ?: 0.0,
    defaultCreditDays = defaultCreditDays ?: 0,
    metadata = metadata
)

fun List<CustomerTypeUpdateRequest>.toCustomerTypes(): List<CustomerType> = map { it.toCustomerType() }