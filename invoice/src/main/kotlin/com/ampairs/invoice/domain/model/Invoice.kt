package com.ampairs.invoice.domain.model

import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import com.ampairs.core.domain.model.Address
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.invoice.config.Constants
import com.ampairs.invoice.domain.dto.Discount
import com.ampairs.invoice.domain.dto.TaxInfo
import com.ampairs.invoice.domain.enums.InvoiceStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.*


@NamedEntityGraph(
    name = "Invoice.withItems",
    attributeNodes = [NamedAttributeNode("invoiceItems")]
)
@Entity(name = "invoice")
@Table(
    indexes = [
        Index(name = "idx_invoice_uid", columnList = "uid", unique = true),
        Index(name = "invoice_ref_idx", columnList = "ref_id", unique = true),
        Index(name = "idx_invoice_series_seq", columnList = "owner_id, series, sequence_number", unique = true)
    ]
)
class Invoice : OwnableBaseDomain() {

    @Column(name = "invoice_number", nullable = false, length = 255)
    var invoiceNumber: String = ""

    // Client-assigned sequential numbering with per-series prefix (spec 010 C5/FR-B09)
    @Column(name = "series", nullable = false, length = 50)
    var series: String = "INV"

    @Column(name = "sequence_number", nullable = false)
    var sequenceNumber: Long = 0

    @Column(name = "order_ref_id", nullable = true, length = 255)
    var orderRefId: String? = null

    @Column(name = "invoice_date", nullable = false)
    var invoiceDate: Instant = Instant.now()

    // Single buyer reference (the seller is the implicit current workspace). The name/GSTIN are
    // snapshotted on the document so a tax invoice keeps the buyer details frozen at issue time.
    @Column(name = "customer_id", length = 36)
    var customerId: String? = null

    @Column(name = "customer_name", length = 255)
    var customerName: String? = null

    @Column(name = "customer_phone", length = 20)
    var customerPhone: String? = null

    @Column(name = "customer_gst", nullable = false, length = 30)
    var customerGst: String = ""

    // Place of supply (buyer's state) — drives the GST scenario via the tax module.
    @Column(name = "place_of_supply", nullable = false, length = 255)
    var placeOfSupply: String = ""

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

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: InvoiceStatus = InvoiceStatus.DRAFT

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

    @BatchSize(size = 30)
    @OneToMany()
    @JoinColumn(
        name = "invoice_id", referencedColumnName = "uid", insertable = false, updatable = false, nullable = false
    )
    var invoiceItems: MutableList<InvoiceItem> = mutableListOf()


    override fun obtainSeqIdPrefix(): String {
        return Constants.INVOICE_PREFIX
    }
}