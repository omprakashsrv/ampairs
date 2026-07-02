package com.ampairs.order.domain.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 009 T009a/T039: the client-resolved pricing snapshot round-trips request → entity → response
 * verbatim (the `/sync` POST persists it; the server never re-resolves).
 */
class OrderItemSnapshotTest {

    @Test
    fun `pricing snapshot round-trips request to entity to response verbatim`() {
        val req = OrderItemRequest(
            id = "OI1",
            productId = "PRD1",
            quantity = 60.0,
            price = 210.0,
            resolvedUnitPriceMinor = 21_000L,
            currency = "INR",
            priceSource = "PRICE_LIST",
            matchedPriceListUid = "PL1",
            appliedTierMinQty = 50.0,
            belowMoq = false,
        )

        val entity = listOf(req).toOrderItems().single()
        assertEquals(21_000L, entity.resolvedUnitPriceMinor)
        assertEquals("INR", entity.currency)
        assertEquals("PRICE_LIST", entity.priceSource)
        assertEquals("PL1", entity.matchedPriceListUid)
        assertEquals(50.0, entity.appliedTierMinQty)
        assertEquals(false, entity.belowMoq)

        val resp = listOf(entity).toResponse().single()
        assertEquals(21_000L, resp.resolvedUnitPriceMinor)
        assertEquals("INR", resp.currency)
        assertEquals("PRICE_LIST", resp.priceSource)
        assertEquals("PL1", resp.matchedPriceListUid)
        assertEquals(50.0, resp.appliedTierMinQty)
        assertEquals(false, resp.belowMoq)
    }
}
