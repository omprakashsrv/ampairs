package com.ampairs.subscription.domain.dto

import com.ampairs.subscription.domain.model.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

// =====================
// Purchase Requests
// =====================

/**
 * Request to initiate subscription purchase (Desktop - Razorpay/Stripe)
 */
data class InitiatePurchaseRequest(
    @field:NotBlank
    val planCode: String,

    @field:NotNull
    val billingCycle: BillingCycle,

    val currency: String = "INR",

    /** Optional coupon/promo code */
    val couponCode: String? = null
)

/**
 * Request to verify mobile in-app purchase (Google Play / App Store)
 */
data class VerifyPurchaseRequest(
    @field:NotNull
    val provider: PaymentProvider,

    @field:NotBlank
    val purchaseToken: String,

    @field:NotBlank
    val productId: String,

    /** Original transaction ID (iOS) or order ID (Android) */
    val orderId: String? = null,

    /** Package name (Android) */
    val packageName: String? = null,

    /** Subscription ID if upgrading/changing plan */
    val existingSubscriptionId: String? = null
)

/**
 * Response after initiating purchase (for desktop checkout)
 */
data class InitiatePurchaseResponse(
    val checkoutUrl: String?,
    val checkoutSessionId: String?,
    val provider: PaymentProvider,
    val subscriptionId: String?,
    val razorpayOrderId: String?,
    val razorpaySubscriptionId: String?,
    val stripeClientSecret: String?,
    val amount: BigDecimal,
    val currency: String
)

/**
 * Request to complete Razorpay payment (after checkout)
 */
data class RazorpayPaymentCompleteRequest(
    @field:NotBlank
    val razorpayPaymentId: String,

    @field:NotBlank
    val razorpayOrderId: String,

    @field:NotBlank
    val razorpaySignature: String,

    val subscriptionId: String? = null
)

// =====================
// Plan Change Requests
// =====================

/**
 * Request to change subscription plan (upgrade/downgrade)
 */
data class ChangePlanRequest(
    @field:NotBlank
    val newPlanCode: String,

    @field:NotNull
    val billingCycle: BillingCycle,

    /** If true, change immediately. If false, change at period end */
    val immediate: Boolean = false
)

/**
 * Response for plan change
 */
data class ChangePlanResponse(
    val subscription: SubscriptionResponse,
    val prorationAmount: BigDecimal?,
    val effectiveAt: java.time.Instant,
    val isImmediate: Boolean
)

// =====================
// Cancellation Requests
// =====================

/**
 * Request to cancel subscription
 */
data class CancelSubscriptionRequest(
    /** Cancel immediately or at period end */
    val immediate: Boolean = false,

    @field:Size(max = 500)
    val reason: String? = null,

    /** Feedback for improvement */
    val feedback: String? = null
)

/**
 * Request to pause subscription
 */
data class PauseSubscriptionRequest(
    /** Number of days to pause (max 90) */
    val pauseDays: Int = 30,

    val reason: String? = null
)

/**
 * Request to resume paused subscription
 */
data class ResumeSubscriptionRequest(
    /** Resume immediately or at scheduled date */
    val immediate: Boolean = true
)

// =====================
// Add-on Requests
// =====================

/**
 * Request to purchase add-on
 */
data class PurchaseAddonRequest(
    @field:NotNull
    val addonCode: AddonModuleCode
)

/**
 * Request to cancel add-on
 */
data class CancelAddonRequest(
    @field:NotBlank
    val addonCode: String,

    val immediate: Boolean = false
)

// =====================
// Payment Method Requests
// =====================

/**
 * Request to add payment method (returns setup URL for hosted page)
 */
data class AddPaymentMethodRequest(
    @field:NotNull
    val provider: PaymentProvider,

    /** Return URL after payment method setup */
    val returnUrl: String? = null
)

/**
 * Response for adding payment method
 */
data class AddPaymentMethodResponse(
    val setupUrl: String?,
    val setupIntentId: String?,
    val provider: PaymentProvider
)

/**
 * Request to set default payment method
 */
data class SetDefaultPaymentMethodRequest(
    @field:NotBlank
    val paymentMethodUid: String
)

// =====================
// Device Registration Requests
// =====================

/**
 * Request to register a device
 */
data class RegisterDeviceRequest(
    @field:NotBlank
    val deviceId: String,

    val deviceName: String? = null,

    @field:NotNull
    val platform: DevicePlatform,

    val deviceModel: String? = null,

    val osVersion: String? = null,

    val appVersion: String? = null,

    val pushToken: String? = null,

    val pushTokenType: String? = null
)

/**
 * Request to refresh device token
 */
data class RefreshDeviceTokenRequest(
    @field:NotBlank
    val deviceId: String,

    val appVersion: String? = null
)

/**
 * Request to deactivate a device
 */
data class DeactivateDeviceRequest(
    @field:NotBlank
    val deviceUid: String,

    val reason: String? = null
)

// =====================
// Sync Requests
// =====================

/**
 * Request to sync subscription state (for offline-first apps)
 */
data class SyncSubscriptionRequest(
    @field:NotBlank
    val deviceId: String,

    val lastSyncAt: java.time.Instant? = null
)

/**
 * Response for subscription sync
 */
data class SyncSubscriptionResponse(
    val subscription: SubscriptionResponse,
    val device: DeviceRegistrationResponse,
    val usage: UsageResponse?,
    val serverTime: java.time.Instant
)

// =====================
// Admin Requests
// =====================

/**
 * Admin request to manually update subscription
 */
data class AdminUpdateSubscriptionRequest(
    val workspaceId: String,
    val planCode: String? = null,
    val status: SubscriptionStatus? = null,
    val billingCycle: BillingCycle? = null,
    val extendDays: Int? = null,
    val notes: String? = null
)

/**
 * Admin request to grant trial
 */
data class AdminGrantTrialRequest(
    @field:NotBlank
    val workspaceId: String,

    val trialDays: Int = 14,

    val planCode: String = "PROFESSIONAL"
)
