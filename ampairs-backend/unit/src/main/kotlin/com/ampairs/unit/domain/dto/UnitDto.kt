package com.ampairs.unit.domain.dto

import com.ampairs.unit.domain.model.Unit
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class UnitRequest(
    val id: String? = null,

    @field:NotBlank(message = "Unit name is required")
    @field:Size(max = 10, message = "Unit name must not exceed 10 characters")
    val name: String,

    @field:Size(max = 10, message = "Unit short name must not exceed 10 characters")
    val shortName: String? = null,

    @field:Min(value = 0, message = "Decimal places must be between 0 and 6")
    @field:Max(value = 6, message = "Decimal places must be between 0 and 6")
    val decimalPlaces: Int = 2,

    @field:Size(max = 255, message = "Reference ID must not exceed 255 characters")
    val refId: String? = null
)

data class UnitResponse(
    val uid: String,
    val name: String,
    val shortName: String,
    val decimalPlaces: Int,
    val refId: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class UnitUsageResponse(
    val unitId: String,
    val inUse: Boolean,
    val productCount: Int,
    val conversionCount: Int,
    val productIds: List<String> = emptyList(),
    val conversionIds: List<String> = emptyList()
)

fun Unit.applyRequest(request: UnitRequest): Unit = apply {
    request.id?.let { uid = it }
    name = request.name.trim()
    shortName = (request.shortName ?: request.name).trim()
    decimalPlaces = request.decimalPlaces
    refId = request.refId?.trim()
}

fun Unit.asUnitResponse(): UnitResponse = UnitResponse(
    uid = uid,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    refId = refId,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun List<Unit>.asUnitResponses(): List<UnitResponse> = map { it.asUnitResponse() }
