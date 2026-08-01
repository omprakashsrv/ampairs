package com.ampairs.ecom.listener

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.service.EcomStorefrontLookupService
import com.ampairs.ecom.service.CatalogSyncService
import com.ampairs.event.domain.events.ProductCatalogChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Applies a batch of product catalog changes (published by the product module) to the ecom
 * storefront catalog.
 *
 * Decoupled + asynchronous: the product module publishes a [ProductCatalogChangedEvent] without
 * knowing about ecom; this listener runs after the product transaction commits, on a separate
 * thread (@Async). Products sync in batches, so each event carries the whole batch — the storefront
 * is resolved once and all changes are applied in a single transaction (CatalogSyncService.applyCatalogBatch).
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
        if (event.changes.isEmpty()) return
        // The listener is the async entry point (like a message consumer), so it owns the tenant
        // context. Set it before any tenant-filtered query (storefront lookup / catalog upsert).
        TenantContextHolder.setCurrentTenant(event.workspaceId)
        try {
            val storefrontId = storefrontLookup.findStorefrontIdByWorkspaceId(event.workspaceId)
            if (storefrontId == null) {
                logger.debug("No storefront for workspace {}, skipping catalog sync", event.workspaceId)
                return
            }
            catalogSyncService.applyCatalogBatch(storefrontId, event)
        } catch (e: Exception) {
            logger.error("Failed to sync {} product(s) to storefront catalog", event.changes.size, e)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
