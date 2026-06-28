package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductStandardCost
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class ProductStandardCostRequest(
    val uid: String? = null,

    @field:NotBlank(message = "productId is required")
    val productId: String,

    val variantSku: String? = null,

    /** Standard purchase cost (major units, e.g. rupees). */
    val costPrice: BigDecimal,

    /** Effective dating (Tally "Applicable From"); null effectiveFrom = effective from the beginning. */
    val effectiveFrom: Instant? = null,
    val effectiveTo: Instant? = null,

    val active: Boolean = true,
    val refId: String? = null,
)

data class ProductStandardCostResponse(
    val uid: String,
    val refId: String?,
    val productId: String,
    val variantSku: String?,
    val costPrice: BigDecimal,
    val effectiveFrom: Instant?,
    val effectiveTo: Instant?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun ProductStandardCost.applyRequest(request: ProductStandardCostRequest): ProductStandardCost = apply {
    request.uid?.takeIf { it.isNotBlank() }?.let { uid = it }
    productId = request.productId
    variantSku = request.variantSku
    costPrice = request.costPrice
    effectiveFrom = request.effectiveFrom
    effectiveTo = request.effectiveTo
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun ProductStandardCostRequest.toEntity(): ProductStandardCost = ProductStandardCost().applyRequest(this)

fun ProductStandardCost.asResponse(): ProductStandardCostResponse = ProductStandardCostResponse(
    uid = uid,
    refId = refId,
    productId = productId,
    variantSku = variantSku,
    costPrice = costPrice,
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
