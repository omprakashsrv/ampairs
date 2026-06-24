package com.ampairs.pricing.domain.dto

import com.ampairs.core.domain.dto.MoneyDto
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceTier
import com.ampairs.pricing.util.PricingJson
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class PriceTierRequest(
    val minQty: BigDecimal,
    val unitPriceMinor: Long,
)

data class PriceTierResponse(
    val minQty: BigDecimal,
    val unitPrice: MoneyDto,
)

data class PriceListItemRequest(
    val uid: String? = null,

    @field:NotBlank(message = "priceListId is required")
    val priceListId: String,

    @field:NotBlank(message = "productId is required")
    val productId: String,

    val variantSku: String? = null,

    /** Base unit price in minor units (paise/cents); currency inherited from the list. */
    val unitPriceMinor: Long,

    /** Minimum order quantity (units, not money). */
    val moq: BigDecimal? = null,

    val tiers: List<PriceTierRequest> = emptyList(),

    val active: Boolean = true,
    val refId: String? = null,
)

data class PriceListItemResponse(
    val uid: String,
    val refId: String?,
    val priceListId: String,
    val productId: String,
    val variantSku: String?,
    val unitPrice: MoneyDto,
    val moq: BigDecimal?,
    val tiers: List<PriceTierResponse>,
    val active: Boolean,
    val createdAt: java.time.Instant?,
    val updatedAt: java.time.Instant?,
)

/** Apply a request, converting minor-unit money to the persisted `BigDecimal(19,4)` using [currency]. */
fun PriceListItem.applyRequest(request: PriceListItemRequest, currency: String): PriceListItem = apply {
    request.uid?.takeIf { it.isNotBlank() }?.let { uid = it }
    priceListId = request.priceListId
    productId = request.productId
    variantSku = request.variantSku
    unitPrice = MoneyDto(request.unitPriceMinor, currency).toBigDecimal()
    moq = request.moq
    tiersJson = request.tiers
        .takeIf { it.isNotEmpty() }
        ?.map { PriceTier(it.minQty, MoneyDto(it.unitPriceMinor, currency).toBigDecimal()) }
        ?.sortedBy { it.minQty }
        ?.let { PricingJson.write(it) }
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun PriceListItem.asResponse(currency: String): PriceListItemResponse = PriceListItemResponse(
    uid = uid,
    refId = refId,
    priceListId = priceListId,
    productId = productId,
    variantSku = variantSku,
    unitPrice = MoneyDto.of(unitPrice, currency)!!,
    moq = moq,
    tiers = PricingJson.read<List<PriceTier>>(tiersJson, emptyList())
        .map { PriceTierResponse(it.minQty, MoneyDto.of(it.unitPrice, currency)!!) },
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
