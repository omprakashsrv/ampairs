package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductVariant
import java.math.BigDecimal
import java.time.Instant

data class ProductVariantResponse(
    val uid: String,
    val productId: String,
    val sku: String,
    val variantName: String,
    val displayName: String,

    // Attributes
    val attribute1Name: String?,
    val attribute1Value: String?,

    val attribute2Name: String?,
    val attribute2Value: String?,

    val attribute3Name: String?,
    val attribute3Value: String?,

    // Pricing
    val mrp: BigDecimal?,
    val dp: BigDecimal?,
    val sellingPrice: BigDecimal?,

    // Stock
    val stockQuantity: BigDecimal,
    val lowStockAlert: BigDecimal?,
    val isLowStock: Boolean,

    val active: Boolean,
    val synced: Boolean,

    val createdAt: Instant?,
    val updatedAt: Instant?
)

fun ProductVariant.asResponse(): ProductVariantResponse {
    return ProductVariantResponse(
        uid = this.uid,
        productId = this.productId,
        sku = this.sku,
        variantName = this.variantName,
        displayName = this.displayName,
        attribute1Name = this.attribute1Name,
        attribute1Value = this.attribute1Value,
        attribute2Name = this.attribute2Name,
        attribute2Value = this.attribute2Value,
        attribute3Name = this.attribute3Name,
        attribute3Value = this.attribute3Value,
        mrp = this.mrp,
        dp = this.dp,
        sellingPrice = this.sellingPrice,
        stockQuantity = this.stockQuantity,
        lowStockAlert = this.lowStockAlert,
        isLowStock = this.isLowStock,
        active = this.active,
        synced = this.synced,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun List<ProductVariant>.asResponse(): List<ProductVariantResponse> {
    return this.map { it.asResponse() }
}
