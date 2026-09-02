package com.ampairs.cb_store.domain.dto

import com.ampairs.cb_store.domain.model.Store
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class StoreRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Store code is required")
    @field:Size(max = 20, message = "Code must not exceed 20 characters")
    val code: String,

    @field:NotBlank(message = "Store name is required")
    @field:Size(max = 200, message = "Name must not exceed 200 characters")
    val name: String,

    @field:NotBlank(message = "City is required")
    @field:Size(max = 100, message = "City must not exceed 100 characters")
    val city: String,

    @field:NotBlank(message = "Zonal office is required")
    val zonalOfficeId: String,

    val active: Boolean = true,

    val refId: String? = null,
)

data class StoreResponse(
    val uid: String,
    val refId: String?,
    val code: String,
    val name: String,
    val city: String,
    val zonalOfficeId: String,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun Store.applyRequest(request: StoreRequest): Store = apply {
    request.uid?.let { uid = it }
    code = request.code.trim()
    name = request.name.trim()
    city = request.city.trim()
    zonalOfficeId = request.zonalOfficeId.trim()
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun Store.asStoreResponse(): StoreResponse = StoreResponse(
    uid = uid,
    refId = refId,
    code = code,
    name = name,
    city = city,
    zonalOfficeId = zonalOfficeId,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
