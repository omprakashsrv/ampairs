package com.ampairs.cb_store.domain.dto

import com.ampairs.cb_store.domain.model.ZonalOffice
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class ZonalOfficeRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Zonal office name is required")
    @field:Size(max = 150, message = "Name must not exceed 150 characters")
    val name: String,

    @field:NotBlank(message = "City is required")
    @field:Size(max = 100, message = "City must not exceed 100 characters")
    val city: String,

    val active: Boolean = true,

    val refId: String? = null,
)

data class ZonalOfficeResponse(
    val uid: String,
    val refId: String?,
    val name: String,
    val city: String,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun ZonalOffice.applyRequest(request: ZonalOfficeRequest): ZonalOffice = apply {
    request.uid?.let { uid = it }
    name = request.name.trim()
    city = request.city.trim()
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun ZonalOffice.asZonalOfficeResponse(): ZonalOfficeResponse = ZonalOfficeResponse(
    uid = uid,
    refId = refId,
    name = name,
    city = city,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
