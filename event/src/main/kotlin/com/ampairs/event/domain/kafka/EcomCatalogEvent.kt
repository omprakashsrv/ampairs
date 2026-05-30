package com.ampairs.event.domain.kafka

import java.math.BigDecimal
import java.time.Instant

data class EcomCatalogEvent(
    val eventType: CatalogEventType,
    val workspaceId: String,
    val storefrontId: String,
    val managementProductId: String,
    val name: String? = null,
    val brand: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val price: BigDecimal? = null,
    val stockQuantity: Int? = null,
    val imageUrls: List<String>? = null,
    val description: String? = null,
    val publishedAt: Instant = Instant.now(),
)

enum class CatalogEventType {
    PRODUCT_LISTED,
    PRODUCT_UNLISTED,
    PRICE_UPDATED,
    STOCK_UPDATED,
    DETAILS_UPDATED,
}
