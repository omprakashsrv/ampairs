package com.ampairs.ecom.listener

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.service.EcomStorefrontLookupService
import com.ampairs.ecom.service.CatalogSyncService
import com.ampairs.event.domain.events.ProductCatalogChangedEvent
import com.ampairs.event.domain.kafka.CatalogEventType
import com.ampairs.event.domain.kafka.EcomCatalogEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Applies product catalog changes (published by the product module) to the ecom storefront catalog.
 *
 * Decoupled + asynchronous: the product module publishes a [ProductCatalogChangedEvent] without
 * knowing about ecom; this listener runs after the product transaction commits, on a separate
 * thread (@Async), and upserts/unlists the corresponding [com.ampairs.ecom.domain.model.EcomListedProduct].
 *
 * Kafka-extensible: when a broker is introduced, a bridge can republish the same payload to a topic
 * and a consumer can call the same [CatalogSyncService] — no change to the product publisher.
 */
@Component
class ProductCatalogEventListener(
    private val catalogSyncService: CatalogSyncService,
    private val storefrontLookup: EcomStorefrontLookupService,
) {

    private val logger = LoggerFactory.getLogger(ProductCatalogEventListener::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onProductCatalogChanged(event: ProductCatalogChangedEvent) {
        // The listener is the async entry point (like a message consumer), so it owns the tenant
        // context. Set it before any tenant-filtered query (storefront lookup / catalog upsert).
        TenantContextHolder.setCurrentTenant(event.workspaceId)
        try {
            val storefrontId = storefrontLookup.findStorefrontIdByWorkspaceId(event.workspaceId)
            if (storefrontId == null) {
                logger.debug("No storefront for workspace {}, skipping catalog sync", event.workspaceId)
                return
            }

            val ecomEvent = EcomCatalogEvent(
                eventType = if (event.listed) CatalogEventType.PRODUCT_LISTED else CatalogEventType.PRODUCT_UNLISTED,
                workspaceId = event.workspaceId,
                storefrontId = storefrontId,
                managementProductId = event.managementProductId,
                name = event.name,
                brand = event.brand,
                category = event.category,
                subcategory = event.subcategory,
                unit = event.unit,
                price = event.price,
                mrp = event.mrp,
                stockQuantity = event.stockQuantity,
                imageUrls = event.imageUrls,
                description = event.description,
            )

            if (event.listed) {
                catalogSyncService.handleProductListed(ecomEvent)
            } else {
                catalogSyncService.handleProductUnlisted(ecomEvent)
            }
        } catch (e: Exception) {
            logger.error("Failed to sync product {} to storefront catalog", event.managementProductId, e)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
