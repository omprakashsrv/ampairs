package com.ampairs.pricing.sync

import com.ampairs.pricing.repository.GeoZoneRepository
import com.ampairs.pricing.repository.OfferRepository
import com.ampairs.pricing.repository.PriceListItemRepository
import com.ampairs.pricing.repository.PriceListRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingCheckpointContributorTest {

    @Mock private lateinit var priceListRepository: PriceListRepository
    @Mock private lateinit var priceListItemRepository: PriceListItemRepository
    @Mock private lateinit var geoZoneRepository: GeoZoneRepository
    @Mock private lateinit var offerRepository: OfferRepository

    @Test
    fun `checkpoints reports all four pricing entity codes`() {
        val listAt = Instant.parse("2026-06-01T10:00:00Z")
        val itemAt = Instant.parse("2026-06-02T10:00:00Z")
        whenever(priceListRepository.findMaxUpdatedAt()).thenReturn(listAt)
        whenever(priceListItemRepository.findMaxUpdatedAt()).thenReturn(itemAt)
        whenever(geoZoneRepository.findMaxUpdatedAt()).thenReturn(null)
        whenever(offerRepository.findMaxUpdatedAt()).thenReturn(null)

        val checkpoints = PricingCheckpointContributor(
            priceListRepository, priceListItemRepository, geoZoneRepository, offerRepository,
        ).checkpoints()

        assertEquals(setOf("price_list", "price_list_item", "geo_zone", "offer"), checkpoints.keys)
        assertEquals(listAt, checkpoints["price_list"])
        assertEquals(itemAt, checkpoints["price_list_item"])
        assertNull(checkpoints["geo_zone"])
        assertNull(checkpoints["offer"])
    }
}
