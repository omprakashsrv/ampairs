package com.ampairs.product.listener

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.EntityChangedEvent
import com.ampairs.product.service.ProductService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Bridges product IMAGE changes to the storefront catalog.
 *
 * Product photos are stored in the `file` module. When only a product's image changes (upload,
 * delete, reorder, set-primary), the file module commits and emits a generic
 * `EntityChangedEvent("product_image", productUid)` — but NOT the [ProductCatalogChangedEvent] that
 * refreshes the ecom storefront (that is only published by the product bulk-sync / listing-toggle).
 * The storefront therefore kept the old image until the product's details were next edited.
 *
 * This listener catches those image-change events and asks the product service to republish a
 * catalog refresh for the affected product, reusing the existing ecom pipeline.
 *
 * Async + after-commit, mirroring [com.ampairs.ecom.listener.ProductCatalogEventListener]: it is the
 * async entry point (like a message consumer), so it owns the tenant context — set it before the
 * tenant-filtered product/image lookups, clear it after.
 */
@Component
class ProductImageChangeListener(
    private val productService: ProductService,
) {

    private val logger = LoggerFactory.getLogger(ProductImageChangeListener::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onEntityChanged(event: EntityChangedEvent) {
        if (event.entityType != PRODUCT_IMAGE) return
        TenantContextHolder.setCurrentTenant(event.workspaceId)
        try {
            // event.entityId is the product uid (EntityImageService publishes with the entity's uid).
            productService.refreshStorefrontListingForImage(event.entityId)
        } catch (e: Exception) {
            logger.error("Failed to refresh storefront catalog after product image change for {}", event.entityId, e)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    private companion object {
        // Matches EntityImageService.imageCode("PRODUCT") = "product_image".
        const val PRODUCT_IMAGE = "product_image"
    }
}
