package com.ampairs.unit.domain.dto

import com.ampairs.unit.domain.model.UnitConversion
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class UnitConversionRequest(
    val id: String? = null,

    @field:NotBlank(message = "Base unit ID is required")
    val baseUnitId: String,

    @field:NotBlank(message = "Derived unit ID is required")
    val derivedUnitId: String,

    val productId: String? = null,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Multiplier must be greater than zero")
    val multiplier: Double,

    @field:Size(max = 255, message = "Reference ID must not exceed 255 characters")
    val refId: String? = null
)

data class UnitConversionResponse(
    val uid: String,
    val baseUnitId: String,
    val derivedUnitId: String,
    val productId: String?,
    val multiplier: Double,
    val baseUnit: UnitResponse?,
    val derivedUnit: UnitResponse?,
    val refId: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

fun UnitConversion.applyRequest(request: UnitConversionRequest): UnitConversion = apply {
    request.id?.let { uid = it }
    baseUnitId = request.baseUnitId
    derivedUnitId = request.derivedUnitId
    productId = request.productId
    multiplier = request.multiplier
    refId = request.refId?.trim()
}

fun UnitConversion.asUnitConversionResponse(): UnitConversionResponse = UnitConversionResponse(
    uid = uid,
    baseUnitId = baseUnitId,
    derivedUnitId = derivedUnitId,
    productId = productId,
    multiplier = multiplier,
    baseUnit = baseUnit?.asUnitResponse(),
    derivedUnit = derivedUnit?.asUnitResponse(),
    refId = refId,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun List<UnitConversion>.asUnitConversionResponses(): List<UnitConversionResponse> =
    map { it.asUnitConversionResponse() }

data class ConvertQuantityRequest(
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
    val quantity: Double,

    @field:NotBlank(message = "From unit ID is required")
    val fromUnitId: String,

    @field:NotBlank(message = "To unit ID is required")
    val toUnitId: String,

    val productId: String? = null
)

data class ConvertedQuantityResponse(
    val originalQuantity: Double,
    val convertedQuantity: Double,
    val fromUnit: UnitResponse?,
    val toUnit: UnitResponse?,
    val multiplier: Double
)
