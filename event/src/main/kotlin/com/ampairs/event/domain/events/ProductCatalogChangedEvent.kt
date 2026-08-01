package com.ampairs.event.domain.events

import java.math.BigDecimal

/**
 * A single product's storefront-listing change (resolved snapshot — names + image URLs already
 * resolved, no JPA associations) so an async consumer can apply it after commit without a session.
 */
data class ProductCatalogChange(
    val managementProductId: String,
    /** true = list/refresh the product on the storefront; false = unlist it. */
    val listed: Boolean,
    val name: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val unit: String? = null,
    val price: BigDecimal? = null,
    val mrp: BigDecimal? = null,
    val stockQuantity: Int? = null,
    val imageUrls: List<String>? = null,
)

/**
 * Application event published (in-process, async) when one or more products' storefront listings
 * change — a merchant toggling a listing, or a bulk product sync refreshing already-listed products.
 *
 * Batched on purpose: products sync in batches, so the whole batch rides one event and the ecom
 * consumer resolves the storefront once and applies all changes in a single transaction (rather
 * than one event / one transaction per product).
 *
 * Decoupled: the product module publishes it without knowing the consumer; ecom listens and applies
 * it. Kafka-extensible later — a bridge can republish the same payload to a topic. `storefrontId` is
 * deliberately absent; the consumer resolves it from [workspaceId].
 */
data class ProductCatalogChangedEvent(
    val workspaceId: String,
    val changes: List<ProductCatalogChange>,
)
