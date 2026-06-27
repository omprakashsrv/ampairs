package com.ampairs.purchase.domain.model

import com.ampairs.core.domain.model.Address
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.purchase.config.Constants
import com.ampairs.purchase.domain.dto.Discount
import com.ampairs.purchase.domain.dto.TaxInfo
import com.ampairs.purchase.domain.enums.PurchaseStatus
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * Purchase document — what you buy from a [com.ampairs.supplier.domain.model.Supplier], mirroring
 * [com.ampairs.order.domain.model.Order] on the sell side. A RECEIVED purchase increases inventory.
 */
@NamedEntityGraph(
    name = "Purchase.withItems",
    attributeNodes = [NamedAttributeNode("purchaseItems")]
)
@Entity(name = "purchase")
@Table(
    indexes = [
        Index(name = "idx_purchase_uid", columnList = "uid", unique = true),
        Index(name = "purchase_ref_idx", columnList = "ref_id", unique = true)
    ]
)
class Purchase : OwnableBaseDomain() {

    @Column(name = "purchase_number", nullable = false, length = 255)
    var purchaseNumber: String = ""

    @Column(name = "purchase_type", nullable = false, length = 20)
    var purchaseType: String = "REGULAR"

    // Supplier (buy-side counterparty) snapshot
    @Column(name = "supplier_id", length = 36)
    var supplierId: String? = null

    @Column(name = "supplier_name", length = 255)
    var supplierName: String? = null

    @Column(name = "supplier_phone", length = 20)
    var supplierPhone: String? = null

    @Column(name = "supplier_gst", nullable = false, length = 30)
    var supplierGst: String = ""

    // The supplier's own bill/invoice number, recorded for reconciliation
    @Column(name = "supplier_invoice_number", length = 255)
    var supplierInvoiceNumber: String? = null

    @Column(name = "purchase_date", nullable = false)
    var purchaseDate: Instant = Instant.now()

    @Column(name = "delivery_date")
    var deliveryDate: Instant? = null

    @Column(name = "place_of_supply", nullable = false, length = 255)
    var placeOfSupply: String = ""

    @Column(name = "subtotal", nullable = false)
    var subtotal: Double = 0.0

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Double = 0.0

    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Double = 0.0

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Double = 0.0

    @Column(name = "total_cost", nullable = false)
    var totalCost: Double = 0.0

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0

    @Column(name = "total_tax", nullable = false)
    var totalTax: Double = 0.0

    @Column(name = "price_mode", nullable = false, length = 20)
    var priceMode: String = "TAX_EXCLUSIVE"

    @Column(name = "overall_discount_mode", nullable = false, length = 30)
    var overallDiscountMode: String = "POST_TAX_REDUCTION"

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    var internalNotes: String? = null

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PurchaseStatus = PurchaseStatus.DRAFT

    @Column(name = "total_items", nullable = false)
    var totalItems: Int = 0

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Double = 0.0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address", nullable = false)
    var billingAddress: Address = Address()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false)
    var shippingAddress: Address = Address()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discount", nullable = true)
    var discount: List<Discount>? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_info", length = 255)
    var taxInfos: List<TaxInfo> = listOf()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    var attributes: Map<String, Any> = emptyMap()

    @BatchSize(size = 30)
    @OneToMany()
    @JoinColumn(
        name = "purchase_id", referencedColumnName = "uid", insertable = false, updatable = false, nullable = false
    )
    var purchaseItems: MutableList<PurchaseItem> = mutableListOf()

    override fun obtainSeqIdPrefix(): String {
        return Constants.PURCHASE_PREFIX
    }

    fun getDisplayPurchaseNumber(): String {
        return purchaseNumber.takeIf { it.isNotBlank() } ?: "PUR-${uid.takeLast(6).uppercase()}"
    }
}
