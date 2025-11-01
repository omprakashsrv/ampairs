package com.ampairs.order.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.order.config.Constants
import com.ampairs.order.domain.dto.Discount
import com.ampairs.order.domain.dto.TaxInfo
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity(name = "order_item")
class OrderItem : OwnableBaseDomain() {

    @Column(name = "order_id", nullable = false, length = 255)
    var orderId: String = ""

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_info", length = 255)
    var taxInfos: List<TaxInfo> = listOf()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discount", nullable = true)
    var discount: List<Discount>? = null

    /**
     * Item-specific attributes stored as JSON
     * Examples:
     * - JEWELRY: weight, purity, stone_details, customization_notes
     * - KIRANA: expiry_date, batch_number, storage_requirements
     * - HARDWARE: material_specifications, warranty_info, installation_notes
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    var attributes: Map<String, Any> = emptyMap()

    override fun obtainSeqIdPrefix(): String {
        return Constants.ORDER_ITEM_PREFIX
    }

    /**
     * Calculate line total with tax and discount
     */
    fun calculateLineTotal() {
        lineTotal = (quantity * unitPrice) - discountAmount
    }

    /**
     * Calculate line total including tax
     */
    fun calculateLineTotalWithTax() {
        calculateLineTotal()
        lineTotal += totalTax
    }

    /**
     * Get effective unit price after discount
     */
    fun getEffectiveUnitPrice(): Double {
        return if (quantity > 0) (lineTotal / quantity) else unitPrice
    }
}