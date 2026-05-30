package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.ecom.domain.enums.StockStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "ecom_listed_product",
    indexes = [
        Index(name = "idx_ecom_product_storefront",   columnList = "storefront_id"),
        Index(name = "idx_ecom_product_management",   columnList = "management_product_id"),
        Index(name = "idx_ecom_product_visible",      columnList = "storefront_id, is_visible"),
        Index(name = "idx_ecom_product_stock_status", columnList = "storefront_id, stock_status"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_ecom_product_storefront", columnNames = ["storefront_id", "management_product_id"])
    ]
)
class EcomListedProduct : OwnableBaseDomain() {

    @Column(name = "storefront_id", nullable = false, length = 200)
    var storefrontId: String = ""

    @Column(name = "management_product_id", nullable = false, length = 200)
    var managementProductId: String = ""

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", nullable = false, columnDefinition = "jsonb")
    var imageUrls: List<String> = emptyList()

    @Column(name = "brand", length = 255)
    var brand: String? = null

    @Column(name = "category", length = 255)
    var category: String? = null

    @Column(name = "subcategory", length = 255)
    var subcategory: String? = null

    @Column(name = "unit", length = 100)
    var unit: String? = null

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    var price: BigDecimal = BigDecimal.ZERO

    @Column(name = "mrp", precision = 19, scale = 4)
    var mrp: BigDecimal? = null

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = 0

    @Column(name = "stock_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var stockStatus: StockStatus = StockStatus.OUT_OF_STOCK

    @Column(name = "is_visible", nullable = false)
    var isVisible: Boolean = true

    @Column(name = "last_synced_at", nullable = false)
    var lastSyncedAt: Instant = Instant.now()

    override fun obtainSeqIdPrefix(): String = "ELP"
}
