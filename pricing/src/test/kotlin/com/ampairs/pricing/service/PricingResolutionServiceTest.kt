package com.ampairs.pricing.service

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.domain.dto.PriceResolutionRequest
import com.ampairs.pricing.domain.enums.PriceListStatus
import com.ampairs.pricing.domain.enums.PriceSource
import com.ampairs.pricing.domain.model.AttributePredicate
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceTier
import com.ampairs.pricing.repository.PriceListItemRepository
import com.ampairs.pricing.repository.PriceListRepository
import com.ampairs.pricing.util.PricingJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class PricingResolutionServiceTest {

    private val priceListRepository: PriceListRepository = mock()
    private val priceListItemRepository: PriceListItemRepository = mock()
    // Default mock returns null from zoneForPincode (object return type) — no stubbing needed.
    private val geoZoneService: GeoZoneService = mock()

    private val service = PricingResolutionServiceImpl(priceListRepository, priceListItemRepository, geoZoneService)

    private fun list(uid: String, block: PriceList.() -> Unit = {}) = PriceList().apply {
        this.uid = uid
        channel = SalesChannel.WHOLESALE
        status = PriceListStatus.ACTIVE
        active = true
        currency = "INR"
        block()
    }

    private fun item(listUid: String, block: PriceListItem.() -> Unit = {}) = PriceListItem().apply {
        uid = "ITEM-$listUid"
        priceListId = listUid
        productId = "RICE"
        unitPrice = BigDecimal("240.00")
        active = true
        block()
    }

    private fun req(qty: Long, block: PriceResolutionRequest.() -> PriceResolutionRequest = { this }) =
        PriceResolutionRequest(
            channel = SalesChannel.WHOLESALE,
            productId = "RICE",
            quantity = BigDecimal(qty),
            customerGroupId = "DIST",
            fallbackUnitPriceMinor = 26000,
        ).block()

    @Test
    fun `tiered wholesale list resolves correct tier price and MOQ flag`() {
        val l = list("PRL1") { customerGroupId = "DIST" }
        val it = item("PRL1") {
            moq = BigDecimal(10)
            tiersJson = PricingJson.write(listOf(PriceTier(BigDecimal(10), BigDecimal("225.00")), PriceTier(BigDecimal(50), BigDecimal("210.00"))))
        }
        whenever(priceListRepository.findByChannelAndStatusAndActiveTrue(SalesChannel.WHOLESALE, PriceListStatus.ACTIVE)).thenReturn(listOf(l))
        whenever(priceListItemRepository.findByPriceListIdAndProductIdAndActiveTrue("PRL1", "RICE")).thenReturn(listOf(it))

        // qty 5 → base ₹240, below MOQ
        service.resolve(req(5)).let {
            assertEquals(24000, it.effectiveUnitPrice.amountMinor)
            assertEquals(PriceSource.PRICE_LIST, it.source)
            assertEquals("PRL1", it.matchedPriceListUid)
            assertTrue(it.belowMoq)
            assertNull(it.appliedTierMinQty)
        }
        // qty 10 → ₹225, MOQ met
        service.resolve(req(10)).let {
            assertEquals(22500, it.effectiveUnitPrice.amountMinor)
            assertEquals(BigDecimal(10), it.appliedTierMinQty)
            assertFalse(it.belowMoq)
        }
        // qty 60 → ₹210
        assertEquals(21000, service.resolve(req(60)).effectiveUnitPrice.amountMinor)
    }

    @Test
    fun `no matching list falls back to catalog selling price`() {
        whenever(priceListRepository.findByChannelAndStatusAndActiveTrue(any(), any())).thenReturn(emptyList())
        val r = service.resolve(req(1))
        assertEquals(PriceSource.CATALOG_FALLBACK, r.source)
        assertEquals(26000, r.effectiveUnitPrice.amountMinor)
        assertNull(r.matchedPriceListUid)
    }

    @Test
    fun `per-customer special price wins over group list`() {
        val group = list("GROUP") { customerGroupId = "DIST" }
        val special = list("SPECIAL") { customerId = "CUST1" }
        whenever(priceListRepository.findByChannelAndStatusAndActiveTrue(any(), any())).thenReturn(listOf(group, special))
        whenever(priceListItemRepository.findByPriceListIdAndProductIdAndActiveTrue("SPECIAL", "RICE"))
            .thenReturn(listOf(item("SPECIAL") { unitPrice = BigDecimal("200.00") }))

        val r = service.resolve(req(1) { copy(customerId = "CUST1") })
        assertEquals("SPECIAL", r.matchedPriceListUid)
        assertEquals(20000, r.effectiveUnitPrice.amountMinor)
    }

    @Test
    fun `variant item wins over base-product item`() {
        val l = list("PRL1") { customerGroupId = "DIST" }
        whenever(priceListRepository.findByChannelAndStatusAndActiveTrue(any(), any())).thenReturn(listOf(l))
        whenever(priceListItemRepository.findByPriceListIdAndProductIdAndActiveTrue("PRL1", "RICE")).thenReturn(
            listOf(
                item("PRL1") { variantSku = null; unitPrice = BigDecimal("240.00") },
                item("PRL1") { uid = "ITEM-V"; variantSku = "RICE-5KG"; unitPrice = BigDecimal("230.00") },
            )
        )
        val r = service.resolve(req(1) { copy(variantSku = "RICE-5KG") })
        assertEquals(23000, r.effectiveUnitPrice.amountMinor)
    }

    @Test
    fun `attribute-predicate list does not override a structured-dimension match`() {
        val structured = list("STRUCT") { customerGroupId = "DIST" }
        val predicateOnly = list("PRED") {
            attributePredicatesJson = PricingJson.write(listOf(AttributePredicate("customer.tier", AttributePredicate.Operator.EQ, "GOLD")))
        }
        whenever(priceListRepository.findByChannelAndStatusAndActiveTrue(any(), any())).thenReturn(listOf(predicateOnly, structured))
        whenever(priceListItemRepository.findByPriceListIdAndProductIdAndActiveTrue("STRUCT", "RICE"))
            .thenReturn(listOf(item("STRUCT") { unitPrice = BigDecimal("225.00") }))

        val r = service.resolve(req(1) { copy(customerAttributes = mapOf("customer.tier" to "GOLD")) })
        assertEquals("STRUCT", r.matchedPriceListUid)
        assertEquals(22500, r.effectiveUnitPrice.amountMinor)
    }

    @Test
    fun `geo-zone list resolves when delivery pincode maps to the zone`() {
        val zoned = list("ZONED") { geoZoneId = "ZONE1"; customerGroupId = null }
        whenever(geoZoneService.zoneForPincode("560001", null)).thenReturn("ZONE1")
        whenever(priceListRepository.findByChannelAndStatusAndActiveTrue(any(), any())).thenReturn(listOf(zoned))
        whenever(priceListItemRepository.findByPriceListIdAndProductIdAndActiveTrue("ZONED", "RICE"))
            .thenReturn(listOf(item("ZONED") { unitPrice = BigDecimal("215.00") }))

        val r = service.resolve(req(1) { copy(customerGroupId = null, pincode = "560001") })
        assertEquals("ZONED", r.matchedPriceListUid)
        assertEquals(21500, r.effectiveUnitPrice.amountMinor)
    }
}
