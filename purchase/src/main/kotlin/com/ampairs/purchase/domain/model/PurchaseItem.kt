package com.ampairs.purchase.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.purchase.config.Constants
import com.ampairs.purchase.domain.dto.Discount
import com.ampairs.purchase.domain.dto.TaxInfo
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity(name = "purchase_item")
class PurchaseItem : OwnableBaseDomain() {

    @Column(name = "purchase_id", nullable = false, length = 255)
    var purchaseId: String = ""

    @Column(name = "description", nullable = false, length = 255)
    var description: String = ""

    @Column(name = "product_id", nullable = false, length = 255)
    var productId: String = ""

    @Column(name = "tax_code", nullable = false, length = 255)
    var taxCode: String = ""

    @Column(name = "quantity", nullable = false)
    var quantity: Double = 0.0

    @Column(name = "index_no", nullable = false)
    var index: Int = 0

    @Column(name = "unit_price", nullable = false)
    var unitPrice: Double = 0.0

    @Column(name = "line_total", nullable = false)
    var lineTotal: Double = 0.0

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Double = 0.0

    @Column(name = "purchase_price", nullable = false)
    var purchasePrice: Double = 0.0

    @Column(name = "product_price", nullable = false)
    var productPrice: Double = 0.0

    @Column(name = "mrp", nullable = false)
    var mrp: Double = 0.0

    @Column(name = "dp", nullable = false)
    var dp: Double = 0.0

    @Column(name = "total_cost", nullable = false)
    var totalCost: Double = 0.0

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0

    @Column(name = "total_tax", nullable = false)
    var totalTax: Double = 0.0

    @Column(name = "unit_id", nullable = false, length = 255)
    var unitId: String = ""

    @Column(name = "base_quantity", nullable = false)
    var baseQuantity: Double = 0.0

    @Column(name = "variant_sku", nullable = true, length = 255)
    var variantSku: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_info", length = 255)
    var taxInfos: List<TaxInfo> = listOf()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discount", nullable = true)
    var discount: List<Discount>? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    var attributes: Map<String, Any> = emptyMap()

    override fun obtainSeqIdPrefix(): String {
        return Constants.PURCHASE_ITEM_PREFIX
    }
}
