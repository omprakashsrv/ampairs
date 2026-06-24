package com.ampairs.pricing.service

import com.ampairs.core.domain.dto.MoneyDto
import com.ampairs.pricing.domain.dto.PriceResolutionRequest
import com.ampairs.pricing.domain.dto.PriceResolutionResponse
import com.ampairs.pricing.domain.enums.PriceListStatus
import com.ampairs.pricing.domain.enums.PriceSource
import com.ampairs.pricing.domain.model.AttributePredicate
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceTier
import com.ampairs.pricing.repository.PriceListItemRepository
import com.ampairs.pricing.repository.PriceListRepository
import com.ampairs.pricing.util.PricingJson
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
class PricingResolutionServiceImpl(
    private val priceListRepository: PriceListRepository,
    private val priceListItemRepository: PriceListItemRepository,
    private val geoZoneService: GeoZoneService,
) : PricingResolutionService {

    @Transactional(readOnly = true)
    override fun resolve(request: PriceResolutionRequest): PriceResolutionResponse {
        val now = Instant.now()
        val zoneId = geoZoneService.zoneForPincode(request.pincode, request.state)

        val best = priceListRepository
            .findByChannelAndStatusAndActiveTrue(request.channel, PriceListStatus.ACTIVE)
            .asSequence()
            .filter { withinWindow(it, now) && matches(it, request, zoneId) }
            // most specific first → highest priority → most recently activated
            .sortedWith(
                compareBy<PriceList> { specificityRank(it) }
                    .thenByDescending { it.priority }
                    .thenByDescending { it.updatedAt ?: Instant.EPOCH }
            )
            .firstOrNull()

        if (best != null) {
            val item = pickItem(best.uid, request.productId, request.variantSku)
            if (item != null) {
                val tiers = PricingJson.read<List<PriceTier>>(item.tiersJson, emptyList())
                val applicable = tiers.filter { it.minQty <= request.quantity }.maxByOrNull { it.minQty }
                val unitPrice = applicable?.unitPrice ?: item.unitPrice
                val belowMoq = item.moq?.let { request.quantity < it } ?: false
                return PriceResolutionResponse(
                    effectiveUnitPrice = MoneyDto.of(unitPrice, best.currency)!!,
                    source = PriceSource.PRICE_LIST,
                    matchedPriceListUid = best.uid,
                    appliedTierMinQty = applicable?.minQty,
                    belowMoq = belowMoq,
                )
            }
        }

        // Catalog fallback — caller-supplied product/variant sellingPrice (minor units).
        return PriceResolutionResponse(
            effectiveUnitPrice = MoneyDto(request.fallbackUnitPriceMinor ?: 0L, request.currency),
            source = PriceSource.CATALOG_FALLBACK,
            matchedPriceListUid = null,
            appliedTierMinQty = null,
            belowMoq = false,
        )
    }

    private fun withinWindow(list: PriceList, now: Instant): Boolean {
        if (list.startsAt != null && now.isBefore(list.startsAt)) return false
        if (list.endsAt != null && now.isAfter(list.endsAt)) return false
        return true
    }

    /** Every dimension the list pins must match the request; predicates (if any) must all pass. */
    private fun matches(list: PriceList, req: PriceResolutionRequest, zoneId: String?): Boolean {
        if (list.customerId != null && list.customerId != req.customerId) return false
        if (list.customerGroupId != null && list.customerGroupId != req.customerGroupId) return false
        if (list.customerType != null && list.customerType != req.customerType) return false
        if (list.brandId != null && list.brandId != req.brandId) return false
        if (list.categoryId != null && list.categoryId != req.categoryId) return false
        if (list.productGroupId != null && list.productGroupId != req.productGroupId) return false
        if (list.geoZoneId != null && list.geoZoneId != zoneId) return false

        val predicates = PricingJson.read<List<AttributePredicate>>(list.attributePredicatesJson, emptyList())
        return predicates.all { evaluate(it, req) }
    }

    /** Lower rank = more specific. Mirrors spec precedence (per-customer > group > product-taxonomy > geo/type > predicate > channel-only). */
    private fun specificityRank(list: PriceList): Int = when {
        list.customerId != null -> 1
        list.customerGroupId != null -> 2
        list.brandId != null || list.categoryId != null || list.productGroupId != null -> 3
        list.geoZoneId != null || list.customerType != null -> 4
        !list.attributePredicatesJson.isNullOrBlank() -> 5
        else -> 6
    }

    /** Variant-targeted item wins over a base-product item within the same list. */
    private fun pickItem(priceListUid: String, productId: String, variantSku: String?): PriceListItem? {
        val items = priceListItemRepository.findByPriceListIdAndProductIdAndActiveTrue(priceListUid, productId)
        if (items.isEmpty()) return null
        if (variantSku != null) {
            items.firstOrNull { it.variantSku == variantSku }?.let { return it }
        }
        return items.firstOrNull { it.variantSku == null } ?: items.first()
    }

    private fun evaluate(p: AttributePredicate, req: PriceResolutionRequest): Boolean {
        val actual = req.customerAttributes[p.field] ?: req.productAttributes[p.field] ?: return false
        return when (p.operator) {
            AttributePredicate.Operator.EQ -> actual == p.value
            AttributePredicate.Operator.NEQ -> actual != p.value
            AttributePredicate.Operator.IN -> p.value.split(',').map { it.trim() }.contains(actual)
            AttributePredicate.Operator.GT -> compareNumeric(actual, p.value) > 0
            AttributePredicate.Operator.LT -> compareNumeric(actual, p.value) < 0
            AttributePredicate.Operator.GTE -> compareNumeric(actual, p.value) >= 0
            AttributePredicate.Operator.LTE -> compareNumeric(actual, p.value) <= 0
        }
    }

    private fun compareNumeric(a: String, b: String): Int {
        val an = a.toBigDecimalOrNull()
        val bn = b.toBigDecimalOrNull()
        return if (an != null && bn != null) an.compareTo(bn) else a.compareTo(b)
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = try { BigDecimal(this) } catch (e: NumberFormatException) { null }
}
