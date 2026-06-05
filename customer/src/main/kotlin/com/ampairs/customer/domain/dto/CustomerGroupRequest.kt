package com.ampairs.customer.domain.dto

import com.ampairs.core.validation.SafeString
import com.ampairs.customer.domain.model.CustomerGroup
import jakarta.validation.constraints.*

data class CustomerGroupUpdateRequest(
    @field:NotBlank(message = "Group code is required")
    @field:SafeString(maxLength = 20, message = "Group code contains invalid characters")
    @field:Size(min = 2, max = 20, message = "Group code must be between 2 and 20 characters")
    @field:Pattern(regexp = "^[A-Z0-9_]+$", message = "Group code must contain only uppercase letters, numbers and underscores")
    val groupCode: String? = null,

    @field:SafeString(maxLength = 100, message = "Name contains invalid characters")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val name: String?,

    @field:SafeString(maxLength = 255, message = "Description contains invalid characters")
    @field:Size(max = 255, message = "Description cannot exceed 255 characters")
    val description: String?,

    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int?,

    val active: Boolean?,

    @field:DecimalMin(value = "0.0", message = "Default discount percentage must be non-negative")
    @field:DecimalMax(value = "100.0", message = "Default discount percentage cannot exceed 100")
    val defaultDiscountPercentage: Double?,

    @field:Min(value = 0, message = "Priority level must be non-negative")
    val priorityLevel: Int?,

    val metadata: String?,

    val refId: String? = null
)

private fun buildCustomerGroup(
    uid: String? = null,
    groupCode: String,
    name: String,
    description: String?,
    displayOrder: Int,
    active: Boolean,
    defaultDiscountPercentage: Double,
    priorityLevel: Int,
    metadata: String?,
    refId: String? = null
): CustomerGroup = CustomerGroup().apply {
    uid?.let { this.uid = it }
    this.groupCode = groupCode
    this.name = name
    this.description = description
    this.displayOrder = displayOrder
    this.active = active
    this.defaultDiscountPercentage = defaultDiscountPercentage
    this.priorityLevel = priorityLevel
    this.metadata = metadata
    this.refId = refId
}

fun CustomerGroupUpdateRequest.toCustomerGroup(): CustomerGroup = buildCustomerGroup(
    groupCode = groupCode?.uppercase() ?: "",
    name = name ?: "",
    description = description,
    displayOrder = displayOrder ?: 0,
    active = active ?: true,
    defaultDiscountPercentage = defaultDiscountPercentage ?: 0.0,
    priorityLevel = priorityLevel ?: 0,
    metadata = metadata,
    refId = refId
)

fun List<CustomerGroupUpdateRequest>.toCustomerGroups(): List<CustomerGroup> = map { it.toCustomerGroup() }