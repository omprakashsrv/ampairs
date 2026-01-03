package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Invoice line item representing individual charges on an invoice.
 * Each line item represents a subscription plan, addon, or usage-based charge.
 */
@Entity
@Table(
    name = "invoice_line_items",
    indexes = [
        Index(name = "idx_line_item_invoice", columnList = "invoice_id")
    ]
)
class InvoiceLineItem : BaseDomain() {

    /**
     * Reference to parent invoice
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    lateinit var invoice: Invoice

    /**
     * Item description (e.g., "Professional Plan - January 2025")
     */
    @Column(name = "description", nullable = false, length = 500)
    var description: String = ""

    /**
     * Item type for categorization
     */
    @Column(name = "item_type", length = 50)
    var itemType: String? = null // "SUBSCRIPTION", "ADDON", "USAGE", "DISCOUNT"

    /**
     * Quantity of items
     */
    @Column(name = "quantity", nullable = false)
    var quantity: Int = 1

    /**
     * Unit price
     */
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal = BigDecimal.ZERO

    /**
     * Total amount for this line item (quantity * unit price)
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO

    /**
     * Reference to subscription usage record (if usage-based)
     */
    @Column(name = "usage_record_id")
    var usageRecordId: Long? = null

    /**
     * Reference to addon subscription (if addon charge)
     */
    @Column(name = "addon_subscription_id")
    var addonSubscriptionId: Long? = null

    /**
     * Tax percentage applied to this line item
     */
    @Column(name = "tax_percent", precision = 5, scale = 2)
    var taxPercent: BigDecimal = BigDecimal.ZERO

    /**
     * Period start for this charge (if recurring)
     */
    @Column(name = "period_start")
    var periodStart: java.time.Instant? = null

    /**
     * Period end for this charge (if recurring)
     */
    @Column(name = "period_end")
    var periodEnd: java.time.Instant? = null

    /**
     * Calculate total amount based on quantity and unit price
     */
    fun calculateAmount(): BigDecimal {
        amount = unitPrice.multiply(BigDecimal(quantity))
        return amount
    }

    /**
     * Calculate tax amount for this line item
     */
    fun calculateTaxAmount(): BigDecimal {
        return amount.multiply(taxPercent).divide(BigDecimal(100), 4, java.math.RoundingMode.HALF_UP)
    }

    override fun obtainSeqIdPrefix(): String {
        return "INVLI"
    }
}
