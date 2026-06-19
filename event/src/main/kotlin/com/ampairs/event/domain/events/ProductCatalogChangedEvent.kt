package com.ampairs.event.domain.events

import java.math.BigDecimal

/**
 * Application event published (in-process, async) when a product's storefront listing should change
 * — either because the merchant toggled "list on storefront" or because a listed product's details
 * were edited and must propagate to the ecom catalog.
 *
 * This is a resolved snapshot (names + image URLs already resolved, no JPA associations) so the
 * async @TransactionalEventListener can consume it after commit without a Hibernate session.
 *
 * Intentionally decoupled: the product module publishes it and does not know who consumes it. The
 * ecom module listens and applies it to its catalog. This keeps the path Kafka-extensible later
 * (a bridge can republish the same payload to a topic) without changing the publisher.
 *
 * `storefrontId` is deliberately absent — that is an ecom concern; the listener resolves it from
 * [workspaceId].
 */
data class ProductCatalogChangedEvent(
    val workspaceId: String,
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
