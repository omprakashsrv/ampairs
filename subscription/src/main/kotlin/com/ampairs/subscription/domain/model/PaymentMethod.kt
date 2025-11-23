package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Instant

/**
 * Stored payment method for a workspace.
 * Tracks cards, UPI, etc. for recurring payments.
 */
@Entity
@Table(
    name = "payment_methods",
    indexes = [
        Index(name = "idx_pm_uid", columnList = "uid", unique = true),
        Index(name = "idx_pm_workspace", columnList = "workspace_id"),
        Index(name = "idx_pm_provider", columnList = "payment_provider"),
        Index(name = "idx_pm_default", columnList = "is_default")
    ]
)
class PaymentMethod : BaseDomain() {

    /**
     * Workspace this payment method belongs to
     */
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    /**
     * Payment provider where this method is stored
     */
    @Column(name = "payment_provider", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var paymentProvider: PaymentProvider = PaymentProvider.STRIPE

    /**
     * External payment method ID from provider
     */
    @Column(name = "external_payment_method_id", nullable = false, length = 255)
    var externalPaymentMethodId: String = ""

    /**
     * Type of payment method
     */
    @Column(name = "type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var type: PaymentMethodType = PaymentMethodType.CARD

    /**
     * Last 4 digits (for cards)
     */
    @Column(name = "last4", length = 4)
    var last4: String? = null

    /**
     * Card brand (VISA, MASTERCARD, etc.)
     */
    @Column(name = "brand", length = 30)
    var brand: String? = null

    /**
     * Card expiry month
     */
    @Column(name = "exp_month")
    var expMonth: Int? = null

    /**
     * Card expiry year
     */
    @Column(name = "exp_year")
    var expYear: Int? = null

    /**
     * Cardholder name
     */
    @Column(name = "cardholder_name", length = 200)
    var cardholderName: String? = null

    /**
     * UPI ID (for UPI payments)
     */
    @Column(name = "upi_id", length = 100)
    var upiId: String? = null

    /**
     * Bank name (for net banking)
     */
    @Column(name = "bank_name", length = 100)
    var bankName: String? = null

    /**
     * Country of card issuer
     */
    @Column(name = "country", length = 2)
    var country: String? = null

    /**
     * Whether this is the default payment method
     */
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false

    /**
     * Whether this payment method is active
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * Billing email for receipts
     */
    @Column(name = "billing_email", length = 255)
    var billingEmail: String? = null

    /**
     * Fingerprint (for duplicate detection)
     */
    @Column(name = "fingerprint", length = 100)
    var fingerprint: String? = null

    /**
     * Metadata JSON
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String = "{}"

    override fun obtainSeqIdPrefix(): String {
        return "PM"
    }

    /**
     * Get display name for this payment method
     */
    fun getDisplayName(): String {
        return when (type) {
            PaymentMethodType.CARD -> "${brand ?: "Card"} **** $last4"
            PaymentMethodType.UPI -> "UPI: $upiId"
            PaymentMethodType.NET_BANKING -> "Net Banking: $bankName"
            PaymentMethodType.WALLET -> "Wallet"
            PaymentMethodType.BANK_TRANSFER -> "Bank Transfer"
            PaymentMethodType.IN_APP_PURCHASE -> "In-App Purchase"
        }
    }

    /**
     * Check if card is expired
     */
    fun isExpired(): Boolean {
        if (type != PaymentMethodType.CARD) return false
        if (expMonth == null || expYear == null) return false

        val now = java.time.YearMonth.now()
        val expiry = java.time.YearMonth.of(expYear!!, expMonth!!)
        return expiry.isBefore(now)
    }
}
