package com.ampairs.unit.domain.dto

import com.ampairs.unit.domain.model.Unit
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class UnitRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Unit name is required")
    @field:Size(min = 1, max = 100, message = "Unit name must be between 1 and 100 characters")
    val name: String,

    @field:NotBlank(message = "Unit short name is required")
    @field:Size(min = 1, max = 20, message = "Unit short name must be between 1 and 20 characters")
    val shortName: String,

    @field:Min(value = 0, message = "Decimal places must be between 0 and 10")
    @field:Max(value = 10, message = "Decimal places must be between 0 and 10")
    val decimalPlaces: Int = 2,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @field:Size(max = 50, message = "Category must not exceed 50 characters")
    val category: String? = null,

    val active: Boolean = true
)

data class UnitResponse(
    val uid: String,
    val name: String,
    val shortName: String,
    val decimalPlaces: Int,
    val description: String?,
    val category: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class UnitUsageResponse(
    val unitId: String,
    val inUse: Boolean,
    val entityCount: Int,
    val conversionCount: Int,
    val entityIds: List<String> = emptyList(),
    val conversionIds: List<String> = emptyList()
)

fun Unit.applyRequest(request: UnitRequest): Unit = apply {
    request.uid?.let { uid = it }
    name = request.name.trim()
    shortName = request.shortName.trim()
    decimalPlaces = request.decimalPlaces
    description = request.description?.trim()
    category = request.category?.trim()
    active = request.active
}

fun Unit.asUnitResponse(): UnitResponse = UnitResponse(
    uid = uid,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    description = description,
    category = category,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun List<Unit>.asUnitResponses(): List<UnitResponse> = map { it.asUnitResponse() }
