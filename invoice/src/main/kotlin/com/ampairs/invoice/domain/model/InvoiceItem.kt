package com.ampairs.invoice.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.invoice.config.Constants
import com.ampairs.invoice.domain.dto.Discount
import com.ampairs.invoice.domain.dto.TaxInfo
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.Type

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

    @Type(JsonType::class)
    @Column(name = "tax_info", length = 255, columnDefinition = "json")
    var taxInfos: List<TaxInfo> = listOf()

    @Type(JsonType::class)
    @Column(name = "discount", nullable = true, columnDefinition = "json")
    var discount: List<Discount>? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.INVOICE_ITEM_PREFIX
    }
}