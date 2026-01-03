package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "product_variant",
    indexes = [
        Index(name = "idx_product_variant_owner_id", columnList = "owner_id"),
        Index(name = "idx_product_variant_workspace_id", columnList = "workspace_id"),
        Index(name = "idx_product_variant_product_id", columnList = "product_id"),
        Index(name = "idx_product_variant_sku", columnList = "sku"),
        Index(name = "idx_product_variant_active", columnList = "active"),
        Index(name = "idx_product_variant_updated_at", columnList = "updated_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_product_variant_uid", columnNames = ["uid"]),
        UniqueConstraint(name = "uq_product_variant_sku", columnNames = ["sku"])
    ]
)
class ProductVariant : OwnableBaseDomain() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "uid", insertable = false, updatable = false)
    var product: Product? = null

    @Column(name = "product_id", nullable = false)
    var productId: String = ""

    @Column(nullable = false, unique = true, length = 100)
    var sku: String = ""

    @Column(name = "variant_name", nullable = false)
    var variantName: String = ""

    // Flexible attributes (up to 3 key-value pairs)
    @Column(name = "attribute_1_name", length = 100)
    var attribute1Name: String? = null

    @Column(name = "attribute_1_value")
    var attribute1Value: String? = null

    @Column(name = "attribute_2_name", length = 100)
    var attribute2Name: String? = null

    @Column(name = "attribute_2_value")
    var attribute2Value: String? = null

    @Column(name = "attribute_3_name", length = 100)
    var attribute3Name: String? = null

    @Column(name = "attribute_3_value")
    var attribute3Value: String? = null

    // Pricing (optional overrides)
    @Column(precision = 15, scale = 2)
    var mrp: BigDecimal? = null

    @Column(precision = 15, scale = 2)
    var dp: BigDecimal? = null

    @Column(name = "selling_price", precision = 15, scale = 2)
    var sellingPrice: BigDecimal? = null

    // Stock management
    @Column(name = "stock_quantity", precision = 15, scale = 3, nullable = false)
    var stockQuantity: BigDecimal = BigDecimal.ZERO

    @Column(name = "low_stock_alert", precision = 15, scale = 3)
    var lowStockAlert: BigDecimal? = null

    @Column(nullable = false)
    var active: Boolean = true

    @Column(nullable = false)
    var synced: Boolean = false

    /**
     * Computed display name combining all attribute values
     */
    val displayName: String
        get() {
            val parts = mutableListOf<String>()
            attribute1Value?.let { parts.add(it) }
            attribute2Value?.let { parts.add(it) }
            attribute3Value?.let { parts.add(it) }
            return if (parts.isNotEmpty()) parts.joinToString(" - ") else variantName
        }

    /**
     * Check if variant is low on stock
     */
    val isLowStock: Boolean
        get() = lowStockAlert != null && stockQuantity <= lowStockAlert!!

    override fun obtainSeqIdPrefix(): String {
        return Constants.VARIANT_PREFIX
    }
}
