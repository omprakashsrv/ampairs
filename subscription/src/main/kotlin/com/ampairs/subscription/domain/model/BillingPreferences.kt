package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*

/**
 * Billing preferences for a workspace.
 * Controls auto-payment settings and billing cycle preferences.
 */
@Entity
@Table(
    name = "billing_preferences",
    indexes = [
        Index(name = "idx_billing_workspace", columnList = "workspace_id", unique = true)
    ]
)
class BillingPreferences : BaseDomain() {

    /**
     * Workspace this preference belongs to
     */
    @Column(name = "workspace_id", nullable = false, unique = true, length = 200)
    var workspaceId: String = ""

    /**
     * Enable auto-payment for invoices
     */
    @Column(name = "auto_payment_enabled", nullable = false)
    var autoPaymentEnabled: Boolean = false

    /**
     * Default payment method to use for auto-charge (FK to payment_methods)
     */
    @Column(name = "default_payment_method_id")
    var defaultPaymentMethodId: Long? = null

    /**
     * Billing mode (PREPAID or POSTPAID)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false, length = 20)
    var billingMode: BillingMode = BillingMode.PREPAID

    /**
     * Billing email for invoices and receipts
     */
    @Column(name = "billing_email", nullable = false, length = 255)
    var billingEmail: String = ""

    /**
     * Send payment reminders
     */
    @Column(name = "send_payment_reminders", nullable = false)
    var sendPaymentReminders: Boolean = true

    /**
     * Grace period days before suspension (default 15 days)
     */
    @Column(name = "grace_period_days", nullable = false)
    var gracePeriodDays: Int = 15

    /**
     * Billing currency (INR, USD, etc.)
     */
    @Column(name = "billing_currency", nullable = false, length = 3)
    var billingCurrency: String = "INR"

    /**
     * Tax identifier (GST number, VAT number, etc.)
     */
    @Column(name = "tax_identifier", length = 50)
    var taxIdentifier: String? = null

    /**
     * Billing address line 1
     */
    @Column(name = "billing_address_line1", length = 255)
    var billingAddressLine1: String? = null

    /**
     * Billing address line 2
     */
    @Column(name = "billing_address_line2", length = 255)
    var billingAddressLine2: String? = null

    /**
     * Billing city
     */
    @Column(name = "billing_city", length = 100)
    var billingCity: String? = null

    /**
     * Billing state/province
     */
    @Column(name = "billing_state", length = 100)
    var billingState: String? = null

    /**
     * Billing postal code
     */
    @Column(name = "billing_postal_code", length = 20)
    var billingPostalCode: String? = null

    /**
     * Billing country code (ISO 3166-1 alpha-2)
     */
    @Column(name = "billing_country", length = 2)
    var billingCountry: String? = null

    /**
     * Check if auto-payment is properly configured
     */
    fun isAutoPaymentConfigured(): Boolean {
        return autoPaymentEnabled && defaultPaymentMethodId != null
    }

    /**
     * Get full billing address as single string
     */
    fun getFullBillingAddress(): String? {
        val parts = listOfNotNull(
            billingAddressLine1,
            billingAddressLine2,
            billingCity,
            billingState,
            billingPostalCode,
            billingCountry
        )
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }

    override fun obtainSeqIdPrefix(): String {
        return "BILLPREF"
    }
}
