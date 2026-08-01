package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "ecom_cart_item",
    indexes = [Index(name = "idx_ecom_cart_item_cart", columnList = "cart_id")]
)
class EcomCartItem : BaseDomain() {

    @Column(name = "cart_id", nullable = false, length = 200)
    var cartId: String = ""

    @Column(name = "listed_product_id", nullable = false, length = 200)
    var listedProductId: String = ""

    @Column(name = "management_product_id", nullable = false, length = 200)
    var managementProductId: String = ""

    @Column(name = "product_name", nullable = false, length = 255)
    var productName: String = ""

    @Column(name = "brand", length = 255)
    var brand: String? = null

    @Column(name = "unit", length = 100)
    var unit: String? = null

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "mrp_at_add", precision = 19, scale = 4)
    var mrpAtAdd: BigDecimal? = null

    // ── price-resolution snapshot (009) — captured at add time; immune to later list edits ──
    @Column(name = "resolved_unit_price_minor")
    var resolvedUnitPriceMinor: Long? = null

    @Column(name = "currency", length = 3)
    var currency: String? = null

    @Column(name = "price_source", length = 30)
    var priceSource: String? = null

    @Column(name = "matched_price_list_uid", length = 200)
    var matchedPriceListUid: String? = null

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 1

    @Column(name = "primary_image_url", length = 500)
    var primaryImageUrl: String? = null

    override fun obtainSeqIdPrefix(): String = "CRI"
}
