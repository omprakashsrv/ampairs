package com.ampairs.pricing.domain.dto

import com.ampairs.core.domain.dto.MoneyDto
import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.config.Constants
import com.ampairs.pricing.domain.enums.PriceSource
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

/**
 * Inputs to price resolution. The caller supplies the buyer's resolved segment attributes (group,
 * type, …) and the product taxonomy (brand/category/group) plus, for catalog fallback, the product's
 * `sellingPrice` as [fallbackUnitPriceMinor] — keeping the pricing module decoupled from `product`.
 */
data class PriceResolutionRequest(
    val channel: SalesChannel,

    @field:NotBlank
    val productId: String,
    val variantSku: String? = null,

    val quantity: BigDecimal = BigDecimal.ONE,

    val customerId: String? = null,
    val customerGroupId: String? = null,
    val customerType: String? = null,
    val brandId: String? = null,
    val categoryId: String? = null,
    val productGroupId: String? = null,

    val pincode: String? = null,
    val state: String? = null,

    /**
     * The date the price is resolved "as of" — the transaction/voucher date for billing (so a
     * back-dated bill gets the price effective on that date), or now for real-time storefront pricing.
     * Drives both price-list window and effective-dated item selection (Tally "Applicable From").
     */
    val asOfDate: Instant = Instant.now(),

    val customerAttributes: Map<String, String> = emptyMap(),
    val productAttributes: Map<String, String> = emptyMap(),

    /** Product/variant catalog `sellingPrice` in minor units — used when no list matches. */
    val fallbackUnitPriceMinor: Long? = null,
    val currency: String = Constants.DEFAULT_CURRENCY,
)

data class PriceResolutionResponse(
    val effectiveUnitPrice: MoneyDto,
    val source: PriceSource,
    val matchedPriceListUid: String?,
    val appliedTierMinQty: BigDecimal?,
    val belowMoq: Boolean,
)
