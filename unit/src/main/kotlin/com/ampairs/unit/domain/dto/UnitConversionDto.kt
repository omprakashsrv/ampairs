package com.ampairs.unit.domain.dto

import com.ampairs.unit.domain.model.UnitConversion
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class UnitConversionRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Product ID is required")
    val productId: String,

    @field:NotBlank(message = "Base unit ID is required")
    val baseUnitId: String,

    @field:NotBlank(message = "Derived unit ID is required")
    val derivedUnitId: String,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Multiplier must be greater than zero")
    val multiplier: BigDecimal,

    val active: Boolean = true
)

data class UnitConversionResponse(
    val uid: String,
    val productId: String,
    val baseUnitId: String,
    val derivedUnitId: String,
    val multiplier: BigDecimal,
    val baseUnit: UnitResponse?,
    val derivedUnit: UnitResponse?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

fun UnitConversion.applyRequest(request: UnitConversionRequest): UnitConversion = apply {
    request.uid?.let { uid = it }
    productId = request.productId
    baseUnitId = request.baseUnitId
    derivedUnitId = request.derivedUnitId
    multiplier = request.multiplier
    active = request.active
}

fun UnitConversion.asUnitConversionResponse(): UnitConversionResponse = UnitConversionResponse(
    uid = uid,
    productId = productId ?: "",
    baseUnitId = baseUnitId,
    derivedUnitId = derivedUnitId,
    multiplier = multiplier,
    baseUnit = baseUnit?.asUnitResponse(),
    derivedUnit = derivedUnit?.asUnitResponse(),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun List<UnitConversion>.asUnitConversionResponses(): List<UnitConversionResponse> =
    map { it.asUnitConversionResponse() }

data class ConvertQuantityRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,

    @field:NotBlank(message = "From unit ID is required")
    val fromUnitId: String,

    @field:NotBlank(message = "To unit ID is required")
    val toUnitId: String,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
    val quantity: BigDecimal
)

data class ConvertedQuantityResponse(
    val originalQuantity: BigDecimal,
    val originalUnitId: String,
    val convertedQuantity: BigDecimal,
    val convertedUnitId: String,
    val multiplier: BigDecimal
)
