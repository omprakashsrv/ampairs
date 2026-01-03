package com.ampairs.product.domain.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class ProductVariantRequest(
    @field:NotBlank(message = "Variant name is required")
    val variantName: String,

    val sku: String? = null, // Auto-generated if not provided

    // Variant attributes
    val attribute1Name: String? = null,
    val attribute1Value: String? = null,

    val attribute2Name: String? = null,
    val attribute2Value: String? = null,

    val attribute3Name: String? = null,
    val attribute3Value: String? = null,

    // Pricing (optional overrides)
    @field:PositiveOrZero(message = "MRP must be positive")
    val mrp: BigDecimal? = null,

    @field:PositiveOrZero(message = "Dealer price must be positive")
    val dp: BigDecimal? = null,

    @field:PositiveOrZero(message = "Selling price must be positive")
    val sellingPrice: BigDecimal? = null,

    // Stock
    @field:PositiveOrZero(message = "Stock quantity must be positive")
    val stockQuantity: BigDecimal = BigDecimal.ZERO,

    @field:PositiveOrZero(message = "Low stock alert must be positive")
    val lowStockAlert: BigDecimal? = null,

    val active: Boolean = true
)
