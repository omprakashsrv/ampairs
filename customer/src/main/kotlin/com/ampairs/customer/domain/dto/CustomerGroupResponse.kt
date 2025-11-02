package com.ampairs.customer.domain.dto

import com.ampairs.customer.domain.model.CustomerGroup
import java.time.Instant

data class CustomerGroupResponse(
    val uid: String,
    val groupCode: String,
    val name: String,
    val description: String?,
    val displayOrder: Int,
    val active: Boolean,
    val defaultDiscountPercentage: Double,
    val priorityLevel: Int,
    val metadata: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

fun CustomerGroup.asCustomerGroupResponse(): CustomerGroupResponse {
    return CustomerGroupResponse(
        uid = this.uid,
        groupCode = this.groupCode,
        name = this.name,
        description = this.description,
        displayOrder = this.displayOrder,
        active = this.active,
        defaultDiscountPercentage = this.defaultDiscountPercentage,
        priorityLevel = this.priorityLevel,
        metadata = this.metadata,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun List<CustomerGroup>.asCustomerGroupResponses(): List<CustomerGroupResponse> {
    return map { it.asCustomerGroupResponse() }
}