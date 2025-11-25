package com.ampairs.subscription.domain.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Subscription plan definition entity.
 * Defines the available plans with their limits and pricing.
 * This is a reference/master data table.
 */
@Entity
@Table(
    name = "subscription_plans",
    indexes = [
        Index(name = "idx_sub_plan_uid", columnList = "uid", unique = true),
        Index(name = "idx_sub_plan_code", columnList = "plan_code", unique = true),
        Index(name = "idx_sub_plan_active", columnList = "active")
    ]
)
class SubscriptionPlanDefinition : BaseDomain() {

    /**
     * Unique plan code (FREE, STARTER, PROFESSIONAL, ENTERPRISE)
     */
    @Column(name = "plan_code", nullable = false, unique = true, length = 50)
    var planCode: String = ""

    /**
     * Display name for the plan
     */
    @Column(name = "display_name", nullable = false, length = 100)
    var displayName: String = ""

    /**
     * Plan description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    // Pricing

    /**
     * Monthly price in INR
     */
    @Column(name = "monthly_price_inr", nullable = false, precision = 10, scale = 2)
    var monthlyPriceInr: BigDecimal = BigDecimal.ZERO

    /**
     * Monthly price in USD
     */
    @Column(name = "monthly_price_usd", nullable = false, precision = 10, scale = 2)
    var monthlyPriceUsd: BigDecimal = BigDecimal.ZERO

    // Limits

    /**
     * Maximum workspaces allowed (-1 for unlimited)
     */
    @Column(name = "max_workspaces", nullable = false)
    var maxWorkspaces: Int = 1

    /**
     * Maximum members per workspace (-1 for unlimited)
     */
    @Column(name = "max_members_per_workspace", nullable = false)
    var maxMembersPerWorkspace: Int = 1

    /**
     * Storage limit in GB (-1 for unlimited)
     */
    @Column(name = "max_storage_gb", nullable = false)
    var maxStorageGb: Int = 1

    /**
     * Maximum customers (-1 for unlimited)
     */
    @Column(name = "max_customers", nullable = false)
    var maxCustomers: Int = 50

    /**
     * Maximum products (-1 for unlimited)
     */
    @Column(name = "max_products", nullable = false)
    var maxProducts: Int = 50

    /**
     * Maximum invoices per month (-1 for unlimited)
     */
    @Column(name = "max_invoices_per_month", nullable = false)
    var maxInvoicesPerMonth: Int = 20

    /**
     * Maximum devices (-1 for unlimited)
     */
    @Column(name = "max_devices", nullable = false)
    var maxDevices: Int = 2

    /**
     * Data retention in years (-1 for unlimited)
     */
    @Column(name = "data_retention_years", nullable = false)
    var dataRetentionYears: Int = 1

    // Features

    /**
     * Available modules (JSON array of module codes)
     */
    @Column(name = "available_modules", columnDefinition = "TEXT")
    var availableModules: String = "[\"CUSTOMER\",\"PRODUCT\",\"INVOICE\"]"

    /**
     * API access enabled
     */
    @Column(name = "api_access_enabled", nullable = false)
    var apiAccessEnabled: Boolean = false

    /**
     * Custom branding enabled
     */
    @Column(name = "custom_branding_enabled", nullable = false)
    var customBrandingEnabled: Boolean = false

    /**
     * SSO enabled
     */
    @Column(name = "sso_enabled", nullable = false)
    var ssoEnabled: Boolean = false

    /**
     * Audit logs enabled
     */
    @Column(name = "audit_logs_enabled", nullable = false)
    var auditLogsEnabled: Boolean = false

    /**
     * Priority support enabled
     */
    @Column(name = "priority_support", nullable = false)
    var prioritySupport: Boolean = false

    // Trial

    /**
     * Trial duration in days (0 for no trial)
     */
    @Column(name = "trial_days", nullable = false)
    var trialDays: Int = 0

    // Multi-Workspace Discounts

    /**
     * Minimum workspaces required for discount (e.g., 3)
     */
    @Column(name = "multi_workspace_min_count", nullable = false)
    var multiWorkspaceMinCount: Int = 0

    /**
     * Discount percentage for multiple workspaces (0-100)
     * Applied when user has multiWorkspaceMinCount or more workspaces
     */
    @Column(name = "multi_workspace_discount_percent", nullable = false)
    var multiWorkspaceDiscountPercent: Int = 0

    // Status

    /**
     * Plan is active and available for purchase
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * Display order for UI
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /**
     * Google Play product ID for this plan (monthly)
     */
    @Column(name = "google_play_product_id_monthly", length = 200)
    var googlePlayProductIdMonthly: String? = null

    /**
     * Google Play product ID for this plan (annual)
     */
    @Column(name = "google_play_product_id_annual", length = 200)
    var googlePlayProductIdAnnual: String? = null

    /**
     * App Store product ID for this plan (monthly)
     */
    @Column(name = "app_store_product_id_monthly", length = 200)
    var appStoreProductIdMonthly: String? = null

    /**
     * App Store product ID for this plan (annual)
     */
    @Column(name = "app_store_product_id_annual", length = 200)
    var appStoreProductIdAnnual: String? = null

    /**
     * Razorpay plan ID (monthly)
     */
    @Column(name = "razorpay_plan_id_monthly", length = 200)
    var razorpayPlanIdMonthly: String? = null

    /**
     * Razorpay plan ID (annual)
     */
    @Column(name = "razorpay_plan_id_annual", length = 200)
    var razorpayPlanIdAnnual: String? = null

    /**
     * Stripe price ID (monthly)
     */
    @Column(name = "stripe_price_id_monthly", length = 200)
    var stripePriceIdMonthly: String? = null

    /**
     * Stripe price ID (annual)
     */
    @Column(name = "stripe_price_id_annual", length = 200)
    var stripePriceIdAnnual: String? = null

    override fun obtainSeqIdPrefix(): String {
        return "PLAN"
    }

    /**
     * Get price for given currency
     */
    fun getMonthlyPrice(currency: String): BigDecimal {
        return when (currency.uppercase()) {
            "INR" -> monthlyPriceInr
            "USD" -> monthlyPriceUsd
            else -> monthlyPriceUsd
        }
    }

    /**
     * Check if a limit is unlimited
     */
    fun isUnlimited(limit: Int): Boolean = limit == -1

    /**
     * Check if this is a free plan
     */
    fun isFree(): Boolean {
        return planCode == "FREE" || (monthlyPriceInr == BigDecimal.ZERO && monthlyPriceUsd == BigDecimal.ZERO)
    }

    /**
     * Calculate price with multi-workspace discount applied
     * @param currency Currency code (INR/USD)
     * @param workspaceCount Number of workspaces user owns
     * @return Discounted price per workspace
     */
    fun getPriceWithDiscount(currency: String, workspaceCount: Int): BigDecimal {
        val basePrice = getMonthlyPrice(currency)

        // No discount if below minimum count or discount not configured
        if (workspaceCount < multiWorkspaceMinCount || multiWorkspaceDiscountPercent == 0) {
            return basePrice
        }

        // Apply discount
        val discountMultiplier = BigDecimal(100 - multiWorkspaceDiscountPercent).divide(BigDecimal(100))
        return basePrice.multiply(discountMultiplier)
    }

    /**
     * Check if multi-workspace discount is applicable
     */
    fun hasMultiWorkspaceDiscount(workspaceCount: Int): Boolean {
        return workspaceCount >= multiWorkspaceMinCount && multiWorkspaceDiscountPercent > 0
    }
}
