package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.StockStatus
import com.ampairs.ecom.domain.model.EcomListedProduct
import java.math.BigDecimal
import java.time.Instant

data class ProductSyncItem(
    val uid: String,
    val name: String,
    val brand: String?,
    val category: String?,
    val subcategory: String?,
    val unit: String?,
    val price: BigDecimal,
    val mrp: BigDecimal?,
    val stockStatus: StockStatus,
    val stockQuantity: Int,
    val imageUrls: List<String>,
    val description: String?,
    val isVisible: Boolean,
    val updatedAt: Instant?,
)

data class SyncPage<T>(
    val items: List<T>,
    val totalChanges: Long,
    val page: Int,
    val size: Int,
    val hasMore: Boolean,
    val nextSince: Instant,
)

fun EcomListedProduct.asProductSyncItem() = ProductSyncItem(
    uid = uid,
    name = name,
    brand = brand,
    category = category,
    subcategory = subcategory,
    unit = unit,
    price = price,
    mrp = mrp,
    stockStatus = stockStatus,
    stockQuantity = stockQuantity,
    imageUrls = imageUrls,
    description = description,
    isVisible = isVisible,
    updatedAt = updatedAt,
)
