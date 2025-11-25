package com.ampairs.subscription.domain.dto

import com.ampairs.subscription.domain.model.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

// =====================
// Plan DTOs
// =====================

/**
 * Response DTO for subscription plan
 */
data class PlanResponse(
    val uid: String,
    val planCode: String,
    val displayName: String,
    val description: String?,
    val monthlyPriceInr: BigDecimal,
    val monthlyPriceUsd: BigDecimal,
    val limits: PlanLimitsResponse,
    val features: PlanFeaturesResponse,
    val trialDays: Int,
    val multiWorkspaceDiscount: MultiWorkspaceDiscountResponse,
    val googlePlayProductIdMonthly: String?,
    val googlePlayProductIdAnnual: String?,
    val appStoreProductIdMonthly: String?,
    val appStoreProductIdAnnual: String?,
    val displayOrder: Int
)

data class MultiWorkspaceDiscountResponse(
    val minWorkspaces: Int,
    val discountPercent: Int,
    val isAvailable: Boolean
)

data class PlanLimitsResponse(
    val maxWorkspaces: Int,
    val maxMembersPerWorkspace: Int,
    val maxStorageGb: Int,
    val maxCustomers: Int,
    val maxProducts: Int,
    val maxInvoicesPerMonth: Int,
    val maxDevices: Int,
    val dataRetentionYears: Int
)

data class PlanFeaturesResponse(
    val availableModules: List<String>,
    val apiAccessEnabled: Boolean,
    val customBrandingEnabled: Boolean,
    val ssoEnabled: Boolean,
    val auditLogsEnabled: Boolean,
    val prioritySupport: Boolean
)

// Extension function
fun SubscriptionPlanDefinition.asPlanResponse(): PlanResponse = PlanResponse(
    uid = uid,
    planCode = planCode,
    displayName = displayName,
    description = description,
    monthlyPriceInr = monthlyPriceInr,
    monthlyPriceUsd = monthlyPriceUsd,
    limits = PlanLimitsResponse(
        maxWorkspaces = maxWorkspaces,
        maxMembersPerWorkspace = maxMembersPerWorkspace,
        maxStorageGb = maxStorageGb,
        maxCustomers = maxCustomers,
        maxProducts = maxProducts,
        maxInvoicesPerMonth = maxInvoicesPerMonth,
        maxDevices = maxDevices,
        dataRetentionYears = dataRetentionYears
    ),
    features = PlanFeaturesResponse(
        availableModules = availableModules.removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() },
        apiAccessEnabled = apiAccessEnabled,
        customBrandingEnabled = customBrandingEnabled,
        ssoEnabled = ssoEnabled,
        auditLogsEnabled = auditLogsEnabled,
        prioritySupport = prioritySupport
    ),
    trialDays = trialDays,
    multiWorkspaceDiscount = MultiWorkspaceDiscountResponse(
        minWorkspaces = multiWorkspaceMinCount,
        discountPercent = multiWorkspaceDiscountPercent,
        isAvailable = multiWorkspaceDiscountPercent > 0 && multiWorkspaceMinCount > 0
    ),
    googlePlayProductIdMonthly = googlePlayProductIdMonthly,
    googlePlayProductIdAnnual = googlePlayProductIdAnnual,
    appStoreProductIdMonthly = appStoreProductIdMonthly,
    appStoreProductIdAnnual = appStoreProductIdAnnual,
    displayOrder = displayOrder
)

fun List<SubscriptionPlanDefinition>.asPlanResponses(): List<PlanResponse> = map { it.asPlanResponse() }

// =====================
// Subscription DTOs
// =====================

/**
 * Response DTO for subscription
 */
data class SubscriptionResponse(
    val uid: String,
    val workspaceId: String,
    val planCode: String,
    val plan: PlanResponse?,
    val status: SubscriptionStatus,
    val billingCycle: BillingCycle,
    val paymentProvider: PaymentProvider?,
    val currency: String,
    val currentPeriodStart: Instant?,
    val currentPeriodEnd: Instant?,
    val trialEndsAt: Instant?,
    val cancelAtPeriodEnd: Boolean,
    val cancelledAt: Instant?,
    val nextBillingAmount: BigDecimal?,
    val lastPaymentStatus: PaymentStatus?,
    val lastPaymentAt: Instant?,
    val isFree: Boolean,
    val daysRemaining: Long,
    val activeAddons: List<AddonResponse>,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

fun Subscription.asSubscriptionResponse(addons: List<SubscriptionAddon> = emptyList()): SubscriptionResponse =
    SubscriptionResponse(
        uid = uid,
        workspaceId = workspaceId,
        planCode = planCode,
        plan = plan?.asPlanResponse(),
        status = status,
        billingCycle = billingCycle,
        paymentProvider = paymentProvider,
        currency = currency,
        currentPeriodStart = currentPeriodStart,
        currentPeriodEnd = currentPeriodEnd,
        trialEndsAt = trialEndsAt,
        cancelAtPeriodEnd = cancelAtPeriodEnd,
        cancelledAt = cancelledAt,
        nextBillingAmount = nextBillingAmount,
        lastPaymentStatus = lastPaymentStatus,
        lastPaymentAt = lastPaymentAt,
        isFree = isFree,
        daysRemaining = getDaysRemaining(),
        activeAddons = addons.filter { it.isActive() }.map { it.asAddonResponse() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

// =====================
// Add-on DTOs
// =====================

data class AddonResponse(
    val uid: String,
    val addonCode: AddonModuleCode,
    val displayName: String,
    val description: String,
    val status: SubscriptionStatus,
    val price: BigDecimal,
    val currency: String,
    val activatedAt: Instant?,
    val expiresAt: Instant?
)

fun SubscriptionAddon.asAddonResponse(): AddonResponse = AddonResponse(
    uid = uid,
    addonCode = addonCode,
    displayName = addonCode.displayName,
    description = addonCode.description,
    status = status,
    price = price,
    currency = currency,
    activatedAt = activatedAt,
    expiresAt = expiresAt
)

fun List<SubscriptionAddon>.asAddonResponses(): List<AddonResponse> = map { it.asAddonResponse() }

/**
 * Available add-on info for purchase
 */
data class AvailableAddonResponse(
    val addonCode: AddonModuleCode,
    val displayName: String,
    val description: String,
    val monthlyPriceInr: BigDecimal,
    val monthlyPriceUsd: BigDecimal,
    val isActive: Boolean
)

// =====================
// Payment DTOs
// =====================

data class PaymentTransactionResponse(
    val uid: String,
    val paymentProvider: PaymentProvider,
    val status: PaymentStatus,
    val amount: BigDecimal,
    val currency: String,
    val netAmount: BigDecimal,
    val paymentMethodType: PaymentMethodType?,
    val paymentMethodLast4: String?,
    val cardBrand: String?,
    val description: String?,
    val billingPeriodStart: Instant?,
    val billingPeriodEnd: Instant?,
    val paidAt: Instant?,
    val failureReason: String?,
    val receiptUrl: String?,
    val invoicePdfUrl: String?,
    val createdAt: Instant?
)

fun PaymentTransaction.asPaymentTransactionResponse(): PaymentTransactionResponse =
    PaymentTransactionResponse(
        uid = uid,
        paymentProvider = paymentProvider,
        status = status,
        amount = amount,
        currency = currency,
        netAmount = netAmount,
        paymentMethodType = paymentMethodType,
        paymentMethodLast4 = paymentMethodLast4,
        cardBrand = cardBrand,
        description = description,
        billingPeriodStart = billingPeriodStart,
        billingPeriodEnd = billingPeriodEnd,
        paidAt = paidAt,
        failureReason = failureReason,
        receiptUrl = receiptUrl,
        invoicePdfUrl = invoicePdfUrl,
        createdAt = createdAt
    )

fun List<PaymentTransaction>.asPaymentTransactionResponses(): List<PaymentTransactionResponse> =
    map { it.asPaymentTransactionResponse() }

// =====================
// Payment Method DTOs
// =====================

data class PaymentMethodResponse(
    val uid: String,
    val paymentProvider: PaymentProvider,
    val type: PaymentMethodType,
    val last4: String?,
    val brand: String?,
    val expMonth: Int?,
    val expYear: Int?,
    val cardholderName: String?,
    val upiId: String?,
    val bankName: String?,
    val isDefault: Boolean,
    val isExpired: Boolean,
    val displayName: String,
    val createdAt: Instant?
)

fun PaymentMethod.asPaymentMethodResponse(): PaymentMethodResponse = PaymentMethodResponse(
    uid = uid,
    paymentProvider = paymentProvider,
    type = type,
    last4 = last4,
    brand = brand,
    expMonth = expMonth,
    expYear = expYear,
    cardholderName = cardholderName,
    upiId = upiId,
    bankName = bankName,
    isDefault = isDefault,
    isExpired = isExpired(),
    displayName = getDisplayName(),
    createdAt = createdAt
)

fun List<PaymentMethod>.asPaymentMethodResponses(): List<PaymentMethodResponse> =
    map { it.asPaymentMethodResponse() }

// =====================
// Device DTOs
// =====================

data class DeviceRegistrationResponse(
    val uid: String,
    val deviceId: String,
    val deviceName: String?,
    val platform: DevicePlatform,
    val deviceModel: String?,
    val osVersion: String?,
    val appVersion: String?,
    val tokenExpiresAt: Instant,
    val lastSyncAt: Instant?,
    val lastActivityAt: Instant?,
    val isActive: Boolean,
    val accessMode: SubscriptionAccessMode,
    val createdAt: Instant?
)

fun DeviceRegistration.asDeviceRegistrationResponse(): DeviceRegistrationResponse =
    DeviceRegistrationResponse(
        uid = uid,
        deviceId = deviceId,
        deviceName = deviceName,
        platform = platform,
        deviceModel = deviceModel,
        osVersion = osVersion,
        appVersion = appVersion,
        tokenExpiresAt = tokenExpiresAt,
        lastSyncAt = lastSyncAt,
        lastActivityAt = lastActivityAt,
        isActive = isActive,
        accessMode = getAccessMode(),
        createdAt = createdAt
    )

fun List<DeviceRegistration>.asDeviceRegistrationResponses(): List<DeviceRegistrationResponse> =
    map { it.asDeviceRegistrationResponse() }

// =====================
// Usage DTOs
// =====================

data class UsageResponse(
    val workspaceId: String,
    val periodYear: Int,
    val periodMonth: Int,
    val usage: UsageDetailsResponse,
    val limits: UsageLimitsResponse,
    val exceeded: ExceededLimitsResponse,
    val lastCalculatedAt: Instant?
)

data class UsageDetailsResponse(
    val customerCount: Int,
    val productCount: Int,
    val invoiceCount: Int,
    val orderCount: Int,
    val memberCount: Int,
    val deviceCount: Int,
    val storageUsedGb: Double,
    val apiCalls: Long,
    val smsCount: Int,
    val emailCount: Int
)

data class UsageLimitsResponse(
    val maxCustomers: Int,
    val maxProducts: Int,
    val maxInvoicesPerMonth: Int,
    val maxMembers: Int,
    val maxDevices: Int,
    val maxStorageGb: Int
)

data class ExceededLimitsResponse(
    val customerLimitExceeded: Boolean,
    val productLimitExceeded: Boolean,
    val invoiceLimitExceeded: Boolean,
    val storageLimitExceeded: Boolean,
    val memberLimitExceeded: Boolean,
    val deviceLimitExceeded: Boolean,
    val hasAnyExceeded: Boolean
)

fun UsageMetric.asUsageResponse(plan: SubscriptionPlanDefinition?): UsageResponse = UsageResponse(
    workspaceId = workspaceId,
    periodYear = periodYear,
    periodMonth = periodMonth,
    usage = UsageDetailsResponse(
        customerCount = customerCount,
        productCount = productCount,
        invoiceCount = invoiceCount,
        orderCount = orderCount,
        memberCount = memberCount,
        deviceCount = deviceCount,
        storageUsedGb = getStorageUsedGb(),
        apiCalls = apiCalls,
        smsCount = smsCount,
        emailCount = emailCount
    ),
    limits = UsageLimitsResponse(
        maxCustomers = plan?.maxCustomers ?: -1,
        maxProducts = plan?.maxProducts ?: -1,
        maxInvoicesPerMonth = plan?.maxInvoicesPerMonth ?: -1,
        maxMembers = plan?.maxMembersPerWorkspace ?: -1,
        maxDevices = plan?.maxDevices ?: -1,
        maxStorageGb = plan?.maxStorageGb ?: -1
    ),
    exceeded = ExceededLimitsResponse(
        customerLimitExceeded = customerLimitExceeded,
        productLimitExceeded = productLimitExceeded,
        invoiceLimitExceeded = invoiceLimitExceeded,
        storageLimitExceeded = storageLimitExceeded,
        memberLimitExceeded = memberLimitExceeded,
        deviceLimitExceeded = deviceLimitExceeded,
        hasAnyExceeded = hasExceededLimits()
    ),
    lastCalculatedAt = lastCalculatedAt
)
