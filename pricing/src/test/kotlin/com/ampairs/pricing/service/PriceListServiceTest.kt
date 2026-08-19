package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.PriceListItemRequest
import com.ampairs.pricing.domain.dto.PriceTierRequest
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.exception.InvalidPriceTiersException
import com.ampairs.pricing.repository.PriceListItemRepository
import com.ampairs.pricing.repository.PriceListRepository
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class PriceListServiceTest {

    private val priceListRepository: PriceListRepository = mock()
    private val priceListItemRepository: PriceListItemRepository = mock()
    private val service = PriceListServiceImpl(priceListRepository, priceListItemRepository, mock())

    private fun itemReq(tiers: List<PriceTierRequest>) = PriceListItemRequest(
        priceListId = "PRL1", productId = "RICE", unitPriceMinor = 24000, tiers = tiers,
    )

    @Test
    fun `non-ascending tiers are rejected`() {
        assertThrows(InvalidPriceTiersException::class.java) {
            service.bulkUpsertItems(listOf(itemReq(listOf(PriceTierRequest(BigDecimal(10), 22500), PriceTierRequest(BigDecimal(10), 21000)))))
        }
    }

    @Test
    fun `zero or negative tier minQty is rejected`() {
        assertThrows(InvalidPriceTiersException::class.java) {
            service.bulkUpsertItems(listOf(itemReq(listOf(PriceTierRequest(BigDecimal.ZERO, 22500)))))
        }
    }

    @Test
    fun `valid ascending tiers pass and persist`() {
        whenever(priceListRepository.findByUid("PRL1")).thenReturn(PriceList().apply { uid = "PRL1"; currency = "INR" })
        whenever(priceListItemRepository.save(any<PriceListItem>())).thenAnswer { it.arguments[0] as PriceListItem }
        assertDoesNotThrow {
            service.bulkUpsertItems(listOf(itemReq(listOf(PriceTierRequest(BigDecimal(10), 22500), PriceTierRequest(BigDecimal(50), 21000)))))
        }
    }
}
