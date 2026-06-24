package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.PriceListItemRequest
import com.ampairs.pricing.domain.dto.PriceTierRequest
import com.ampairs.pricing.domain.dto.applyRequest
import com.ampairs.pricing.domain.dto.asResponse
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceTier
import com.ampairs.pricing.util.PricingJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * US4 money contract (FR-010 / SC-005): price-list item money is always a MoneyDto
 * `{ amount_minor, currency }` in the PARENT LIST's currency (single-currency-per-list, enforced by
 * construction) — never a bare number, and never a per-item currency.
 */
class PriceListMoneyContractTest {

    @Test
    fun `item money serializes as MoneyDto in the list currency`() {
        val item = PriceListItem().apply {
            uid = "PLI1"; priceListId = "PL1"; productId = "PRD1"
            unitPrice = BigDecimal("240.00")
            tiersJson = PricingJson.write(
                listOf(PriceTier(BigDecimal("10"), BigDecimal("225.00"))),
            )
        }

        val resp = item.asResponse("INR")

        // Money is a structured MoneyDto (minor units + currency), not a bare number.
        assertEquals(24_000L, resp.unitPrice.amountMinor)
        assertEquals("INR", resp.unitPrice.currency)
        assertEquals(22_500L, resp.tiers[0].unitPrice.amountMinor)
        assertEquals("INR", resp.tiers[0].unitPrice.currency, "tier money inherits the list currency")
    }

    @Test
    fun `push minor units convert to the persisted BigDecimal in the list currency`() {
        val req = PriceListItemRequest(
            priceListId = "PL1",
            productId = "PRD1",
            unitPriceMinor = 21_000L,
            tiers = listOf(PriceTierRequest(minQty = BigDecimal("50"), unitPriceMinor = 20_000L)),
        )

        // The list's currency is applied to ALL item money — items never carry their own currency.
        val item = PriceListItem().applyRequest(req, currency = "USD")

        assertEquals(0, BigDecimal("210.00").compareTo(item.unitPrice))
        val tiers = PricingJson.read<List<PriceTier>>(item.tiersJson, emptyList())
        assertEquals(0, BigDecimal("200.00").compareTo(tiers[0].unitPrice))
    }
}
