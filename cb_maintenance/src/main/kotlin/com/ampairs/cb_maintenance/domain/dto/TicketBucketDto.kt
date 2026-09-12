package com.ampairs.cb_maintenance.domain.dto

import com.ampairs.cb_maintenance.domain.model.TicketBucket
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class TicketBucketRequest(
    val uid: String? = null,
    @field:NotBlank(message = "Department is required")
    val department: String,
    @field:NotBlank(message = "Category is required")
    val category: String,
    @field:NotBlank(message = "Sub category is required")
    val subCategory1: String,
    val subCategory2: String = "",
    val active: Boolean = true,
)

data class TicketBucketResponse(
    val uid: String,
    val department: String,
    val category: String,
    val subCategory1: String,
    val subCategory2: String,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun TicketBucket.applyRequest(request: TicketBucketRequest): TicketBucket = apply {
    request.uid?.takeIf { it.isNotBlank() }?.let { uid = it }
    department = request.department.trim()
    category = request.category.trim()
    subCategory1 = request.subCategory1.trim()
    subCategory2 = request.subCategory2.trim()
    active = request.active
}

fun TicketBucket.asTicketBucketResponse(): TicketBucketResponse = TicketBucketResponse(
    uid = uid,
    department = department,
    category = category,
    subCategory1 = subCategory1,
    subCategory2 = subCategory2,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
