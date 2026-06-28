package com.ampairs.product.service

import com.ampairs.product.domain.model.ProductStandardCost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Pure selection logic for effective-dated standard purchase cost — mirrors the sales-side
 * PricingResolutionServiceTest effective-dating cases (back-dated vs recent vs future-dormant).
 */
class ProductStandardCostResolutionTest {

    private fun cost(
        uid: String,
        price: Double,
        from: String? = null,
        to: String? = null,
        variantSku: String? = null,
    ) = ProductStandardCost().apply {
        this.uid = uid
        productId = "PRD1"
        costPrice = price
        this.variantSku = variantSku
        effectiveFrom = from?.let { Instant.parse(it) }
        effectiveTo = to?.let { Instant.parse(it) }
    }

    @Test
    fun `back-dated voucher gets the old cost, recent voucher gets the new cost`() {
        val items = listOf(
            cost("OLD", 240.0, from = null, to = "2026-03-01T00:00:00Z"),
            cost("NEW", 210.0, from = "2026-03-01T00:00:00Z"),
        )
        val old = ProductStandardCostServiceImpl.pick(items, null, Instant.parse("2026-01-15T00:00:00Z"))
        val recent = ProductStandardCostServiceImpl.pick(items, null, Instant.parse("2026-04-15T00:00:00Z"))
        assertEquals(240.0, old?.costPrice)
        assertEquals(210.0, recent?.costPrice)
    }

    @Test
    fun `future-effective cost is dormant until its effectiveFrom`() {
        val items = listOf(
            cost("CURRENT", 210.0, from = "2026-03-01T00:00:00Z"),
            cost("FUTURE", 199.0, from = "2026-09-01T00:00:00Z"),
        )
        // A July voucher must not see the September cost yet.
        val july = ProductStandardCostServiceImpl.pick(items, null, Instant.parse("2026-07-01T00:00:00Z"))
        assertEquals(210.0, july?.costPrice)
        val october = ProductStandardCostServiceImpl.pick(items, null, Instant.parse("2026-10-01T00:00:00Z"))
        assertEquals(199.0, october?.costPrice)
    }

    @Test
    fun `effective window is from-inclusive and to-exclusive`() {
        val item = cost("C", 210.0, from = "2026-03-01T00:00:00Z", to = "2026-06-01T00:00:00Z")
        assertFalse(ProductStandardCostServiceImpl.effectiveAt(item, Instant.parse("2026-02-28T23:59:59Z")))
        assertTrue(ProductStandardCostServiceImpl.effectiveAt(item, Instant.parse("2026-03-01T00:00:00Z")))
        assertTrue(ProductStandardCostServiceImpl.effectiveAt(item, Instant.parse("2026-05-31T23:59:59Z")))
        assertFalse(ProductStandardCostServiceImpl.effectiveAt(item, Instant.parse("2026-06-01T00:00:00Z")))
    }

    @Test
    fun `variant cost wins over base, and no applicable version yields null`() {
        val items = listOf(
            cost("BASE", 210.0, from = "2026-03-01T00:00:00Z"),
            cost("VAR", 205.0, from = "2026-03-01T00:00:00Z", variantSku = "SKU-RED"),
        )
        val asOf = Instant.parse("2026-04-01T00:00:00Z")
        assertEquals(205.0, ProductStandardCostServiceImpl.pick(items, "SKU-RED", asOf)?.costPrice)
        assertEquals(210.0, ProductStandardCostServiceImpl.pick(items, "SKU-BLUE", asOf)?.costPrice, "no variant match falls back to base")

        // Nothing effective yet for an early voucher.
        assertNull(ProductStandardCostServiceImpl.pick(items, null, Instant.parse("2026-01-01T00:00:00Z")))
    }
}
