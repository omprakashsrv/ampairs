package com.ampairs.pricing.domain.dto

import com.ampairs.core.domain.dto.MoneyDto
import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.config.Constants
import com.ampairs.pricing.domain.enums.OfferRewardType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

/**
 * Inputs to offer application. The caller supplies the buyer's resolved segment attributes and the
 * cart lines **after** price resolution (each `unitPriceMinor` is the already-resolved price). The
 * engine applies eligible promotions on top — it never re-resolves prices. Single-sourced: mirrored
 * offline by the KMP app for the merchant order/invoice flow, run server-side for ecom checkout.
 */
data class OfferApplicationRequest(
    val channel: SalesChannel,

    val customerId: String? = null,
    val customerGroupId: String? = null,
    val customerType: String? = null,

    val pincode: String? = null,
    val state: String? = null,

    /** The customer has never ordered before — gates `FIRST_ORDER` condition offers. */
    val firstOrder: Boolean = false,

    /** Coupon code entered at the cart (if any) — gates `COUPON` condition offers. */
    val couponCode: String? = null,

    val currency: String = Constants.DEFAULT_CURRENCY,

    @field:Valid
    val lines: List<CartLineInput> = emptyList(),
)

/** One resolved cart line. `unitPriceMinor` is the post-resolution unit price in minor units. */
data class CartLineInput(
    @field:NotBlank
    val productId: String,
    val variantSku: String? = null,
    val brandId: String? = null,
    val categoryId: String? = null,
    val productGroupId: String? = null,
    val quantity: BigDecimal = BigDecimal.ONE,
    val unitPriceMinor: Long = 0,
)

/**
 * Result of offer application. `appliedOffers` is the ordered list of promotions that fired,
 * `freeGoods` the extra zero-priced lines (BOGO/free-gift), and `totalDiscount` the summed monetary
 * reduction (PERCENT/FLAT) — free goods are separate lines, not part of `totalDiscount`.
 */
data class OfferApplicationResponse(
    val appliedOffers: List<AppliedOffer>,
    val freeGoods: List<FreeGoodLine>,
    val totalDiscount: MoneyDto,
    val currency: String,
)

data class AppliedOffer(
    val offerUid: String,
    val name: String,
    val rewardType: OfferRewardType,
    /** Monetary reduction this offer contributed (minor units); 0 for pure free-goods offers. */
    val discount: MoneyDto,
    /** null when the offer applies at cart level rather than to a specific product. */
    val productId: String? = null,
)

data class FreeGoodLine(
    val productId: String,
    val variantSku: String?,
    val quantity: BigDecimal,
    val sourceOfferUid: String,
)
