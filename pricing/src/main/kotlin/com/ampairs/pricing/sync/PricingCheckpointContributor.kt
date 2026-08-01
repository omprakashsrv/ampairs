package com.ampairs.pricing.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.pricing.repository.GeoZoneRepository
import com.ampairs.pricing.repository.OfferRepository
import com.ampairs.pricing.repository.PriceListItemRepository
import com.ampairs.pricing.repository.PriceListRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the pricing module's sync checkpoints (max `updatedAt` per entity) for the current
 * workspace. Without this, the mobile client's connect/reconnect/hourly bootstrap never learns the
 * server has pricing data and never pulls it. Queries are `@TenantId`-filtered, so automatically
 * workspace-scoped.
 */
@Component
class PricingCheckpointContributor(
    private val priceListRepository: PriceListRepository,
    private val priceListItemRepository: PriceListItemRepository,
    private val geoZoneRepository: GeoZoneRepository,
    private val offerRepository: OfferRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "price_list" to priceListRepository.findMaxUpdatedAt(),
        "price_list_item" to priceListItemRepository.findMaxUpdatedAt(),
        "geo_zone" to geoZoneRepository.findMaxUpdatedAt(),
        "offer" to offerRepository.findMaxUpdatedAt(),
    )
}
