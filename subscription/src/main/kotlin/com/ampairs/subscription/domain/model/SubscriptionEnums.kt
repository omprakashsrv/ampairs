package com.ampairs.subscription.domain.model

/**
 * Billing cycle options for subscriptions
 */
enum class BillingCycle(val months: Int, val discountPercent: Int) {
    MONTHLY(1, 0),
    QUARTERLY(3, 5),
    ANNUAL(12, 20),
    BIENNIAL(24, 30);

    fun calculateDiscountedPrice(monthlyPrice: Double): Double {
        val totalMonths = months
        val basePrice = monthlyPrice * totalMonths
        return basePrice * (1 - discountPercent / 100.0)
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
    CARD,
    UPI,
    NET_BANKING,
    WALLET,
    BANK_TRANSFER,
    IN_APP_PURCHASE
}

/**
 * Invoice status
 */
enum class InvoiceStatus {
    DRAFT,
    PENDING,
    PAID,
    FAILED,
    VOID,
    REFUNDED
}

/**
 * Add-on module codes
 */
enum class AddonModuleCode(
    val displayName: String,
    val monthlyPriceInr: Double,
    val monthlyPriceUsd: Double,
    val description: String
) {
    TALLY_INTEGRATION(
        displayName = "Tally Integration",
        monthlyPriceInr = 299.0,
        monthlyPriceUsd = 4.0,
        description = "Sync with Tally ERP"
    ),
    ADVANCED_ANALYTICS(
        displayName = "Advanced Analytics",
        monthlyPriceInr = 499.0,
        monthlyPriceUsd = 6.0,
        description = "Custom reports and dashboards"
    ),
    MULTI_CURRENCY(
        displayName = "Multi-Currency",
        monthlyPriceInr = 199.0,
        monthlyPriceUsd = 3.0,
        description = "Support for multiple currencies"
    ),
    E_INVOICING_GST(
        displayName = "E-Invoicing (GST)",
        monthlyPriceInr = 399.0,
        monthlyPriceUsd = 5.0,
        description = "Government e-invoicing compliance"
    ),
    INVENTORY_PRO(
        displayName = "Inventory Pro",
        monthlyPriceInr = 299.0,
        monthlyPriceUsd = 4.0,
        description = "Advanced stock management"
    ),
    CUSTOM_FIELDS(
        displayName = "Custom Fields",
        monthlyPriceInr = 199.0,
        monthlyPriceUsd = 3.0,
        description = "Unlimited custom fields"
    );

    fun getPrice(currency: String): Double {
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
