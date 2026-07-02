package com.ampairs.invoice.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.invoice.config.Constants
import com.ampairs.invoice.domain.dto.Discount
import com.ampairs.invoice.domain.dto.TaxInfo
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity(name = "invoice_item")
class InvoiceItem : OwnableBaseDomain() {

    @Column(name = "invoice_id", nullable = false, length = 255)
    var invoiceId: String = ""

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

    @Column(name = "selling_price", nullable = false)
    var sellingPrice: Double = 0.0

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

    // 009 pricing snapshot — client-resolved effective price; backend persists verbatim (no re-resolution).
    @Column(name = "resolved_unit_price_minor")
    var resolvedUnitPriceMinor: Long? = null

    @Column(name = "currency", length = 3)
    var currency: String? = null

    @Column(name = "price_source", length = 30)
    var priceSource: String? = null

    @Column(name = "matched_price_list_uid", length = 200)
    var matchedPriceListUid: String? = null

    @Column(name = "applied_tier_min_qty")
    var appliedTierMinQty: Double? = null

    @Column(name = "below_moq")
    var belowMoq: Boolean? = null

    // spec 010 FR-014: unit of measure + base-unit quantity, and selected variant
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

    override fun obtainSeqIdPrefix(): String {
        return Constants.INVOICE_ITEM_PREFIX
    }
}