package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Instant

/**
 * Payment transaction record.
 * Tracks all payment attempts and their outcomes.
 */
@Entity
@Table(
    name = "payment_transactions",
    indexes = [
        Index(name = "idx_payment_uid", columnList = "uid", unique = true),
        Index(name = "idx_payment_subscription", columnList = "subscription_id"),
        Index(name = "idx_payment_workspace", columnList = "workspace_id"),
        Index(name = "idx_payment_status", columnList = "status"),
        Index(name = "idx_payment_provider", columnList = "payment_provider"),
        Index(name = "idx_payment_external", columnList = "external_payment_id"),
        Index(name = "idx_payment_created", columnList = "created_at")
    ]
)
class PaymentTransaction : BaseDomain() {

    /**
     * Subscription this payment belongs to
     */
    @Column(name = "subscription_id", nullable = false, length = 200)
    var subscriptionId: String = ""

    /**
     * Workspace ID
     */
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    /**
     * Payment provider
     */
    @Column(name = "payment_provider", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var paymentProvider: PaymentProvider = PaymentProvider.STRIPE

    /**
     * External payment ID from provider
     */
    @Column(name = "external_payment_id", length = 255)
    var externalPaymentId: String? = null

    /**
     * External invoice ID from provider
     */
    @Column(name = "external_invoice_id", length = 255)
    var externalInvoiceId: String? = null

    /**
     * Payment status
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.PENDING

    /**
     * Payment amount
     */
    @Column(name = "amount", nullable = false)
    var amount: Double = 0.0

    /**
     * Currency
     */
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "INR"

    /**
     * Tax amount included
     */
    @Column(name = "tax_amount")
    var taxAmount: Double = 0.0

    /**
     * Discount applied
     */
    @Column(name = "discount_amount")
    var discountAmount: Double = 0.0

    /**
     * Net amount (amount - discount + tax)
     */
    @Column(name = "net_amount", nullable = false)
    var netAmount: Double = 0.0

    /**
     * Payment method type
     */
    @Column(name = "payment_method_type", length = 30)
    @Enumerated(EnumType.STRING)
    var paymentMethodType: PaymentMethodType? = null

    /**
     * Payment method last 4 digits (for cards)
     */
    @Column(name = "payment_method_last4", length = 4)
    var paymentMethodLast4: String? = null

    /**
     * Card brand (VISA, MASTERCARD, etc.)
     */
    @Column(name = "card_brand", length = 20)
    var cardBrand: String? = null

    /**
     * Description of what this payment is for
     */
    @Column(name = "description", length = 500)
    var description: String? = null

    /**
     * Billing period start
     */
    @Column(name = "billing_period_start")
    var billingPeriodStart: Instant? = null

    /**
     * Billing period end
     */
    @Column(name = "billing_period_end")
    var billingPeriodEnd: Instant? = null

    /**
     * When payment was completed
     */
    @Column(name = "paid_at")
    var paidAt: Instant? = null

    /**
     * Failure reason if payment failed
     */
    @Column(name = "failure_reason", length = 500)
    var failureReason: String? = null

    /**
     * Failure code from provider
     */
    @Column(name = "failure_code", length = 100)
    var failureCode: String? = null

    /**
     * Refund amount (if refunded)
     */
    @Column(name = "refund_amount")
    var refundAmount: Double? = null

    /**
     * When refund was processed
     */
    @Column(name = "refunded_at")
    var refundedAt: Instant? = null

    /**
     * Refund reason
     */
    @Column(name = "refund_reason", length = 500)
    var refundReason: String? = null

    /**
     * Receipt URL from provider
     */
    @Column(name = "receipt_url", length = 500)
    var receiptUrl: String? = null

    /**
     * Invoice PDF URL
     */
    @Column(name = "invoice_pdf_url", length = 500)
    var invoicePdfUrl: String? = null

    /**
     * Metadata JSON
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String = "{}"

    override fun obtainSeqIdPrefix(): String {
        return "PAY"
    }

    fun isSuccessful(): Boolean {
        return status == PaymentStatus.SUCCEEDED
    }

    fun isFailed(): Boolean {
        return status == PaymentStatus.FAILED
    }
}
