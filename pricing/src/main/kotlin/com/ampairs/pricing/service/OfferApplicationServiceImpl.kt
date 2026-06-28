package com.ampairs.pricing.service

import com.ampairs.core.domain.dto.MoneyDto
import com.ampairs.pricing.domain.dto.AppliedOffer
import com.ampairs.pricing.domain.dto.CartLineInput
import com.ampairs.pricing.domain.dto.FreeGoodLine
import com.ampairs.pricing.domain.dto.OfferApplicationRequest
import com.ampairs.pricing.domain.dto.OfferApplicationResponse
import com.ampairs.pricing.domain.enums.OfferConditionType
import com.ampairs.pricing.domain.enums.OfferRewardType
import com.ampairs.pricing.domain.enums.OfferStatus
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.repository.OfferRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * Default engine. Supports the three core in-store reward types — PERCENT, FLAT, and BOGO — with
 * structured eligibility (channel, customer group/type/id, brand/category/product-group taxonomy,
 * geo-zone via pincode), trigger conditions (cart-min, quantity-min, first-order, coupon), and
 * priority-ordered stacking with exclusivity. Money stays in minor units; percentages round HALF_UP.
 */
@Service
class OfferApplicationServiceImpl(
    private val offerRepository: OfferRepository,
    private val geoZoneService: GeoZoneService,
) : OfferApplicationService {

    @Transactional(readOnly = true)
    override fun apply(request: OfferApplicationRequest): OfferApplicationResponse {
        val now = Instant.now()
        val zoneId = geoZoneService.zoneForPincode(request.pincode, request.state)

        // Eligible = active + in-window + targeting matches + condition met (on the offer's scoped lines).
        val eligible = offerRepository
            .findByChannelAndStatusAndActiveTrue(request.channel, OfferStatus.ACTIVE)
            .filter { withinWindow(it, now) && targetingMatches(it, request, zoneId) && conditionMet(it, request) }
            // Highest priority first; uid tiebreak keeps selection deterministic.
            .sortedWith(compareByDescending<Offer> { it.priority }.thenBy { it.uid })

        val selected = selectWithStacking(eligible)

        val appliedOffers = mutableListOf<AppliedOffer>()
        val freeGoods = mutableListOf<FreeGoodLine>()
        var totalDiscount = 0L

        for (offer in selected) {
            val scope = scopedLines(offer, request.lines)
            when (offer.rewardType) {
                OfferRewardType.PERCENT -> {
                    val subtotal = subtotalMinor(scope)
                    val discount = capped(percentOf(subtotal, offer.rewardPercent ?: 0.0), offer.rewardCapMinor)
                    if (discount > 0) {
                        totalDiscount += discount
                        appliedOffers += appliedOf(offer, discount, request.currency)
                    }
                }
                OfferRewardType.FLAT -> {
                    val subtotal = subtotalMinor(scope)
                    val discount = minOf(offer.rewardFlatMinor ?: 0L, subtotal)
                    if (discount > 0) {
                        totalDiscount += discount
                        appliedOffers += appliedOf(offer, discount, request.currency)
                    }
                }
                OfferRewardType.BOGO -> {
                    val buy = offer.bogoBuyQty ?: continue
                    val get = offer.bogoGetQty ?: continue
                    if (buy <= 0 || get <= 0) continue
                    var firedForOffer = false
                    for (line in scope) {
                        val sets = line.quantity.divideToIntegralValue(BigDecimal(buy))
                        val freeQty = sets.multiply(BigDecimal(get))
                        if (freeQty > BigDecimal.ZERO) {
                            freeGoods += FreeGoodLine(line.productId, line.variantSku, freeQty, offer.uid)
                            firedForOffer = true
                        }
                    }
                    if (firedForOffer) appliedOffers += appliedOf(offer, 0L, request.currency)
                }
                // FREE_SHIPPING / FREE_GIFT / TIERED are not yet modelled in this engine; skip.
                else -> Unit
            }
        }

        return OfferApplicationResponse(
            appliedOffers = appliedOffers,
            freeGoods = freeGoods,
            totalDiscount = MoneyDto(totalDiscount, request.currency),
            currency = request.currency,
        )
    }

    // ── selection ──────────────────────────────────────────────────────────────────────

    /** An exclusive offer (highest priority) blocks all others. Otherwise all stackable offers apply,
     *  plus the single highest-priority non-stackable one. `eligible` is already priority-sorted. */
    private fun selectWithStacking(eligible: List<Offer>): List<Offer> {
        eligible.firstOrNull { it.exclusive }?.let { return listOf(it) }
        val selected = mutableListOf<Offer>()
        var nonStackableUsed = false
        for (offer in eligible) {
            when {
                offer.stackable -> selected += offer
                !nonStackableUsed -> { selected += offer; nonStackableUsed = true }
            }
        }
        return selected
    }

    // ── eligibility ────────────────────────────────────────────────────────────────────

    private fun withinWindow(offer: Offer, now: Instant): Boolean {
        if (offer.startsAt != null && now.isBefore(offer.startsAt)) return false
        if (offer.endsAt != null && now.isAfter(offer.endsAt)) return false
        return true
    }

    /** Cart-level gates: every customer/geo dimension the offer pins must match the request. */
    private fun targetingMatches(offer: Offer, req: OfferApplicationRequest, zoneId: String?): Boolean {
        if (offer.customerGroupId != null && offer.customerGroupId != req.customerGroupId) return false
        if (offer.customerType != null && offer.customerType != req.customerType) return false
        if (offer.geoZoneId != null && offer.geoZoneId != zoneId) return false
        // A product-taxonomy-targeted offer needs at least one in-scope line to apply at all.
        return scopedLines(offer, req.lines).isNotEmpty()
    }

    /** Lines matching the offer's product-taxonomy targeting; all lines when the offer pins none. */
    private fun scopedLines(offer: Offer, lines: List<CartLineInput>): List<CartLineInput> {
        val pinsTaxonomy = offer.brandId != null || offer.categoryId != null
        if (!pinsTaxonomy) return lines
        return lines.filter { line ->
            (offer.brandId == null || offer.brandId == line.brandId) &&
                (offer.categoryId == null || offer.categoryId == line.categoryId)
        }
    }

    private fun conditionMet(offer: Offer, req: OfferApplicationRequest): Boolean {
        val scope = scopedLines(offer, req.lines)
        return when (offer.conditionType) {
            OfferConditionType.NONE -> true
            OfferConditionType.CART_MIN -> subtotalMinor(scope) >= (offer.cartMinMinor ?: 0L)
            OfferConditionType.QUANTITY_MIN -> totalQty(scope) >= BigDecimal.valueOf(offer.quantityMin ?: 0.0)
            OfferConditionType.FIRST_ORDER -> req.firstOrder
            OfferConditionType.COUPON ->
                !offer.couponCode.isNullOrBlank() &&
                    offer.couponCode.equals(req.couponCode?.trim(), ignoreCase = true)
        }
    }

    // ── money / helpers ──────────────────────────────────────────────────────────────────

    private fun appliedOf(offer: Offer, discountMinor: Long, currency: String) = AppliedOffer(
        offerUid = offer.uid,
        name = offer.name,
        rewardType = offer.rewardType,
        discount = MoneyDto(discountMinor, currency),
        productId = null,
    )

    private fun subtotalMinor(lines: List<CartLineInput>): Long =
        lines.sumOf { line ->
            BigDecimal.valueOf(line.unitPriceMinor)
                .multiply(line.quantity)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }

    private fun totalQty(lines: List<CartLineInput>): BigDecimal =
        lines.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.quantity) }

    private fun percentOf(subtotalMinor: Long, percent: Double): Long =
        BigDecimal.valueOf(subtotalMinor)
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()

    private fun capped(discount: Long, capMinor: Long?): Long =
        if (capMinor != null && capMinor > 0) minOf(discount, capMinor) else discount
}
