package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Invoice entity for postpaid billing system.
 * Represents a billing invoice generated for a workspace's subscription usage.
 */
@Entity
@Table(
    name = "invoices",
    indexes = [
        Index(name = "idx_invoice_workspace", columnList = "workspace_id"),
        Index(name = "idx_invoice_number", columnList = "invoice_number", unique = true),
        Index(name = "idx_invoice_status", columnList = "status"),
        Index(name = "idx_invoice_due_date", columnList = "due_date"),
        Index(name = "idx_invoice_subscription", columnList = "subscription_id")
    ]
)
class Invoice : OwnableBaseDomain() {

    /**
     * Workspace this invoice belongs to
     */
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    /**
     * Unique invoice number (e.g., INV-2025-001)
     */
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    var invoiceNumber: String = ""

    /**
     * Reference to the subscription this invoice belongs to
     */
    @Column(name = "subscription_id", nullable = false)
    var subscriptionId: Long = 0

    /**
     * Billing period start date
     */
    @Column(name = "billing_period_start", nullable = false)
    var billingPeriodStart: Instant = Instant.now()

    /**
     * Billing period end date
     */
    @Column(name = "billing_period_end", nullable = false)
    var billingPeriodEnd: Instant = Instant.now()

    /**
     * Invoice due date (typically 15 days from generation)
     */
    @Column(name = "due_date", nullable = false)
    var dueDate: Instant = Instant.now()

    /**
     * Current status of the invoice
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: InvoiceStatus = InvoiceStatus.DRAFT

    /**
     * Subtotal amount before tax
     */
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    var subtotal: BigDecimal = BigDecimal.ZERO

    /**
     * Tax amount (GST/VAT)
     */
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    var taxAmount: BigDecimal = BigDecimal.ZERO

    /**
     * Discount amount applied
     */
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    var discountAmount: BigDecimal = BigDecimal.ZERO

    /**
     * Total amount payable (subtotal + tax - discount)
     */
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    var totalAmount: BigDecimal = BigDecimal.ZERO

    /**
     * Amount already paid (for partial payments)
     */
    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    var paidAmount: BigDecimal = BigDecimal.ZERO

    /**
     * Currency code (INR, USD, etc.)
     */
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "INR"

    /**
     * Auto-payment enabled for this invoice
     */
    @Column(name = "auto_payment_enabled", nullable = false)
    var autoPaymentEnabled: Boolean = false

    /**
     * Payment method to use for auto-charge (FK to payment_methods)
     */
    @Column(name = "payment_method_id")
    var paymentMethodId: Long? = null

    /**
     * Razorpay invoice ID (for payment link)
     */
    @Column(name = "razorpay_invoice_id", length = 100)
    var razorpayInvoiceId: String? = null

    /**
     * Stripe invoice ID (for payment link)
     */
    @Column(name = "stripe_invoice_id", length = 100)
    var stripeInvoiceId: String? = null

    /**
     * Payment link URL for manual payment
     */
    @Column(name = "payment_link_url", length = 500)
    var paymentLinkUrl: String? = null

    /**
     * Invoice generation timestamp
     */
    @Column(name = "generated_at")
    var generatedAt: Instant? = null

    /**
     * Payment completion timestamp
     */
    @Column(name = "paid_at")
    var paidAt: Instant? = null

    /**
     * Last payment reminder sent timestamp
     */
    @Column(name = "last_reminder_sent_at")
    var lastReminderSentAt: Instant? = null

    /**
     * Workspace suspension timestamp (when grace period expires)
     */
    @Column(name = "suspended_at")
    var suspendedAt: Instant? = null

    /**
     * Number of payment reminders sent
     */
    @Column(name = "reminder_count", nullable = false)
    var reminderCount: Int = 0

    /**
     * Invoice notes/description
     */
    @Column(name = "notes", length = 1000)
    var notes: String? = null

    /**
     * Line items for this invoice
     */
    @OneToMany(mappedBy = "invoice", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var lineItems: MutableList<InvoiceLineItem> = mutableListOf()

    /**
     * Calculate remaining balance
     */
    fun getRemainingBalance(): BigDecimal {
        return totalAmount.subtract(paidAmount)
    }

    /**
     * Check if invoice is fully paid
     */
    fun isFullyPaid(): Boolean {
        return paidAmount >= totalAmount && totalAmount > BigDecimal.ZERO
    }

    /**
     * Check if invoice is overdue
     */
    fun isOverdue(): Boolean {
        return dueDate.isBefore(Instant.now()) && !isFullyPaid()
    }

    /**
     * Get days past due
     */
    fun getDaysPastDue(): Long {
        if (!isOverdue()) return 0
        return java.time.Duration.between(dueDate, Instant.now()).toDays()
    }

    /**
     * Add line item to invoice
     */
    fun addLineItem(lineItem: InvoiceLineItem) {
        lineItems.add(lineItem)
        lineItem.invoice = this
        recalculateTotals()
    }

    /**
     * Recalculate invoice totals from line items
     */
    fun recalculateTotals() {
        subtotal = lineItems.sumOf { it.amount }
        totalAmount = subtotal.add(taxAmount).subtract(discountAmount)
    }

    override fun obtainSeqIdPrefix(): String {
        return "INV"
    }
}
