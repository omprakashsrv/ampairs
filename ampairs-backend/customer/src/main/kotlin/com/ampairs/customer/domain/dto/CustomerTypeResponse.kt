package com.ampairs.customer.domain.dto

import com.ampairs.customer.domain.model.CustomerType
import java.time.LocalDateTime

data class CustomerTypeResponse(
    val uid: String,
    val typeCode: String,
    val name: String,
    val description: String?,
    val displayOrder: Int,
    val active: Boolean,
    val defaultCreditLimit: Double,
    val defaultCreditDays: Int,
    val metadata: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

fun CustomerType.asCustomerTypeResponse(): CustomerTypeResponse {
    return CustomerTypeResponse(
        uid = this.uid,
        typeCode = this.typeCode,
        name = this.name,
        description = this.description,
        displayOrder = this.displayOrder,
        active = this.active,
        defaultCreditLimit = this.defaultCreditLimit,
        defaultCreditDays = this.defaultCreditDays,
        metadata = this.metadata,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun List<CustomerType>.asCustomerTypeResponses(): List<CustomerTypeResponse> {
    return map { it.asCustomerTypeResponse() }
}