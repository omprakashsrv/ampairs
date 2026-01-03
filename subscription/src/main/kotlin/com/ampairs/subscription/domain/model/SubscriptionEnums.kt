package com.ampairs.subscription.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Billing cycle options for subscriptions
 */
enum class BillingCycle(val months: Int, val discountPercent: Int) {
    MONTHLY(1, 0),
    QUARTERLY(3, 5),
    ANNUAL(12, 20),
    BIENNIAL(24, 30);

    fun calculateDiscountedPrice(monthlyPrice: BigDecimal): BigDecimal {
        val totalMonths = BigDecimal(months)
        val basePrice = monthlyPrice.multiply(totalMonths)
        val discountMultiplier = BigDecimal.ONE.subtract(BigDecimal(discountPercent).divide(BigDecimal(100), 4, RoundingMode.HALF_UP))
        return basePrice.multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP)
    }
}

/**
 * Subscription status representing lifecycle states
 */
enum class SubscriptionStatus {
    /** Active and paid subscription */
    ACTIVE,

    /** In trial period */
    TRIALING,

    /** Payment failed, grace period active */
    PAST_DUE,

    /** Subscription temporarily paused */
    PAUSED,

    /** Subscription cancelled by user */
    CANCELLED,

    /** Subscription expired (not renewed) */
    EXPIRED,

    /** Pending initial payment */
    PENDING
}

/**
 * Payment providers supported by the system
 */
enum class PaymentProvider(val displayName: String) {
    GOOGLE_PLAY("Google Play"),
    APP_STORE("Apple App Store"),
    RAZORPAY("Razorpay"),
    STRIPE("Stripe"),
    MANUAL("Manual/Admin")
}

/**
 * Payment transaction status
 */
enum class PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    REFUNDED,
    DISPUTED,
    CANCELLED
}

/**
 * Payment method types
 */
enum class PaymentMethodType {
    CREDIT_CARD,
    DEBIT_CARD,
    UPI,
    NET_BANKING,
    WALLET,
    BANK_TRANSFER,
    IN_APP_PURCHASE
}

/**
 * Billing mode for subscriptions
 */
enum class BillingMode {
    /** Payment before service (traditional subscription) */
    PREPAID,

    /** Payment after service usage (invoice-based) */
    POSTPAID
}

/**
 * Invoice status for postpaid billing
 */
enum class InvoiceStatus {
    /** Invoice is being generated */
    DRAFT,

    /** Invoice sent to customer, awaiting payment */
    PENDING,

    /** Invoice fully paid */
    PAID,

    /** Invoice partially paid (for installments) */
    PARTIALLY_PAID,

    /** Invoice past due date, not yet suspended */
    OVERDUE,

    /** Invoice overdue and workspace suspended */
    SUSPENDED,

    /** Payment attempt failed */
    FAILED,

    /** Invoice cancelled/voided */
    VOID,

    /** Payment refunded */
    REFUNDED
}

/**
 * Add-on module codes
 */
enum class AddonModuleCode(
    val displayName: String,
    val monthlyPriceInr: BigDecimal,
    val monthlyPriceUsd: BigDecimal,
    val description: String
) {
    TALLY_INTEGRATION(
        displayName = "Tally Integration",
        monthlyPriceInr = BigDecimal("299.00"),
        monthlyPriceUsd = BigDecimal("4.00"),
        description = "Sync with Tally ERP"
    ),
    ADVANCED_ANALYTICS(
        displayName = "Advanced Analytics",
        monthlyPriceInr = BigDecimal("499.00"),
        monthlyPriceUsd = BigDecimal("6.00"),
        description = "Custom reports and dashboards"
    ),
    MULTI_CURRENCY(
        displayName = "Multi-Currency",
        monthlyPriceInr = BigDecimal("199.00"),
        monthlyPriceUsd = BigDecimal("3.00"),
        description = "Support for multiple currencies"
    ),
    E_INVOICING_GST(
        displayName = "E-Invoicing (GST)",
        monthlyPriceInr = BigDecimal("399.00"),
        monthlyPriceUsd = BigDecimal("5.00"),
        description = "Government e-invoicing compliance"
    ),
    INVENTORY_PRO(
        displayName = "Inventory Pro",
        monthlyPriceInr = BigDecimal("299.00"),
        monthlyPriceUsd = BigDecimal("4.00"),
        description = "Advanced stock management"
    ),
    CUSTOM_FIELDS(
        displayName = "Custom Fields",
        monthlyPriceInr = BigDecimal("199.00"),
        monthlyPriceUsd = BigDecimal("3.00"),
        description = "Unlimited custom fields"
    );

    fun getPrice(currency: String): BigDecimal {
        return when (currency.uppercase()) {
            "INR" -> monthlyPriceInr
            "USD" -> monthlyPriceUsd
            else -> monthlyPriceUsd
        }
    }
}

/**
 * Device platform types
 */
enum class DevicePlatform {
    ANDROID,
    IOS,
    DESKTOP_WINDOWS,
    DESKTOP_MAC,
    DESKTOP_LINUX,
    WEB
}

/**
 * Access mode for offline enforcement
 */
enum class SubscriptionAccessMode {
    /** Full access - subscription active and recently verified */
    FULL_ACCESS,

    /** Offline grace period - token valid but can't verify */
    OFFLINE_GRACE,

    /** Read-only mode - subscription likely expired */
    READ_ONLY,

    /** Locked - extended non-payment, must sync */
    LOCKED
}
