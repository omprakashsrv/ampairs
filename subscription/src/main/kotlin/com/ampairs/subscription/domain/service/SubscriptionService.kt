package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.domain.repository.*
import com.ampairs.subscription.exception.SubscriptionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.YearMonth

@Service
@Transactional
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val subscriptionAddonRepository: SubscriptionAddonRepository,
    private val usageMetricRepository: UsageMetricRepository
) {
    private val logger = LoggerFactory.getLogger(SubscriptionService::class.java)

    // =====================
    // Plan Operations
    // =====================

    /**
     * Get all available subscription plans
     */
    fun getAvailablePlans(): List<PlanResponse> {
        return subscriptionPlanRepository.findAllActivePlansOrdered().asPlanResponses()
    }

    /**
     * Get a specific plan by code
     */
    fun getPlanByCode(planCode: String): PlanResponse {
        val plan = subscriptionPlanRepository.findByPlanCode(planCode)
            ?: throw SubscriptionException.PlanNotFoundException(planCode)
        return plan.asPlanResponse()
    }

    // =====================
    // Subscription Operations
    // =====================

    /**
     * Get current subscription for a workspace
     */
    fun getSubscription(workspaceId: String): SubscriptionResponse {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        val addons = subscriptionAddonRepository.findActiveBySubscriptionId(subscription.uid)
        return subscription.asSubscriptionResponse(addons)
    }

    /**
     * Get or create subscription for a workspace
     */
    fun getOrCreateSubscription(workspaceId: String): Subscription {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
            ?: createFreeSubscription(workspaceId)
    }

    /**
     * Create a free subscription for a new workspace
     */
    fun createFreeSubscription(workspaceId: String): Subscription {
        val freePlan = subscriptionPlanRepository.findByPlanCode("FREE")
            ?: throw SubscriptionException.PlanNotFoundException("FREE")

        val subscription = Subscription().apply {
            this.workspaceId = workspaceId
            this.plan = freePlan
            this.planCode = "FREE"
            this.status = SubscriptionStatus.ACTIVE
            this.billingCycle = BillingCycle.MONTHLY
            this.currency = "INR"
            this.isFree = true
            this.currentPeriodStart = Instant.now()
            // Free plans don't expire
            this.currentPeriodEnd = null
        }

        logger.info("Creating free subscription for workspace: {}", workspaceId)
        return subscriptionRepository.save(subscription)
    }

    /**
     * Start trial for a workspace
     */
    fun startTrial(workspaceId: String, planCode: String, trialDays: Int): SubscriptionResponse {
        val plan = subscriptionPlanRepository.findByPlanCode(planCode)
            ?: throw SubscriptionException.PlanNotFoundException(planCode)

        val existingSubscription = subscriptionRepository.findByWorkspaceId(workspaceId)

        val subscription = existingSubscription ?: Subscription().apply {
            this.workspaceId = workspaceId
        }

        subscription.apply {
            this.plan = plan
            this.planCode = planCode
            this.status = SubscriptionStatus.TRIALING
            this.billingCycle = BillingCycle.MONTHLY
            this.isFree = false
            this.currentPeriodStart = Instant.now()
            this.currentPeriodEnd = Instant.now().plus(Duration.ofDays(trialDays.toLong()))
            this.trialEndsAt = this.currentPeriodEnd
        }

        logger.info("Starting {} day trial for workspace: {}, plan: {}", trialDays, workspaceId, planCode)
        val saved = subscriptionRepository.save(subscription)

        val addons = subscriptionAddonRepository.findActiveBySubscriptionId(saved.uid)
        return saved.asSubscriptionResponse(addons)
    }

    /**
     * Activate subscription after successful payment
     */
    fun activateSubscription(
        workspaceId: String,
        planCode: String,
        billingCycle: BillingCycle,
        provider: PaymentProvider,
        externalSubscriptionId: String?,
        externalCustomerId: String?,
        currency: String
    ): Subscription {
        val plan = subscriptionPlanRepository.findByPlanCode(planCode)
            ?: throw SubscriptionException.PlanNotFoundException(planCode)

        val subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
            ?: Subscription().apply { this.workspaceId = workspaceId }

        val now = Instant.now()
        val periodEnd = now.plus(Duration.ofDays(billingCycle.months * 30L))

        subscription.apply {
            this.plan = plan
            this.planCode = planCode
            this.status = SubscriptionStatus.ACTIVE
            this.billingCycle = billingCycle
            this.paymentProvider = provider
            this.externalSubscriptionId = externalSubscriptionId
            this.externalCustomerId = externalCustomerId
            this.currency = currency
            this.isFree = false
            this.currentPeriodStart = now
            this.currentPeriodEnd = periodEnd
            this.trialEndsAt = null
            this.lastPaymentAt = now
            this.lastPaymentStatus = PaymentStatus.SUCCEEDED
            this.failedPaymentCount = 0
            this.nextBillingAmount = billingCycle.calculateDiscountedPrice(plan.getMonthlyPrice(currency))
        }

        logger.info(
            "Activating subscription for workspace: {}, plan: {}, cycle: {}, provider: {}",
            workspaceId, planCode, billingCycle, provider
        )
        return subscriptionRepository.save(subscription)
    }

    /**
     * Change subscription plan
     */
    fun changePlan(
        workspaceId: String,
        newPlanCode: String,
        billingCycle: BillingCycle,
        immediate: Boolean
    ): ChangePlanResponse {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        val newPlan = subscriptionPlanRepository.findByPlanCode(newPlanCode)
            ?: throw SubscriptionException.PlanNotFoundException(newPlanCode)

        val effectiveAt = if (immediate) Instant.now() else subscription.currentPeriodEnd ?: Instant.now()

        // Calculate proration if immediate
        var prorationAmount: Double? = null
        if (immediate && subscription.currentPeriodEnd != null) {
            val daysRemaining = subscription.getDaysRemaining()
            val totalDays = subscription.billingCycle.months * 30
            val unusedAmount = (subscription.nextBillingAmount ?: 0.0) * (daysRemaining.toDouble() / totalDays)
            val newAmount = billingCycle.calculateDiscountedPrice(newPlan.getMonthlyPrice(subscription.currency))
            prorationAmount = newAmount - unusedAmount
        }

        if (immediate) {
            subscription.apply {
                this.plan = newPlan
                this.planCode = newPlanCode
                this.billingCycle = billingCycle
                this.currentPeriodStart = Instant.now()
                this.currentPeriodEnd = Instant.now().plus(Duration.ofDays(billingCycle.months * 30L))
                this.nextBillingAmount = billingCycle.calculateDiscountedPrice(newPlan.getMonthlyPrice(currency))
            }
            subscriptionRepository.save(subscription)
            logger.info("Immediate plan change for workspace: {} from {} to {}", workspaceId, subscription.planCode, newPlanCode)
        } else {
            // Schedule change for period end (store in metadata or separate table)
            logger.info("Scheduled plan change for workspace: {} from {} to {} at {}", workspaceId, subscription.planCode, newPlanCode, effectiveAt)
        }

        val addons = subscriptionAddonRepository.findActiveBySubscriptionId(subscription.uid)
        return ChangePlanResponse(
            subscription = subscription.asSubscriptionResponse(addons),
            prorationAmount = prorationAmount,
            effectiveAt = effectiveAt,
            isImmediate = immediate
        )
    }

    /**
     * Cancel subscription
     */
    fun cancelSubscription(workspaceId: String, immediate: Boolean, reason: String?): SubscriptionResponse {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        val now = Instant.now()

        if (immediate) {
            subscription.status = SubscriptionStatus.CANCELLED
            subscription.cancelledAt = now
            subscription.cancellationReason = reason

            // Cancel all addons
            subscriptionAddonRepository.cancelAllBySubscriptionId(subscription.uid, now)
            logger.info("Immediately cancelled subscription for workspace: {}", workspaceId)
        } else {
            subscription.cancelAtPeriodEnd = true
            subscription.cancelledAt = now
            subscription.cancellationReason = reason
            logger.info("Scheduled cancellation for workspace: {} at period end", workspaceId)
        }

        val saved = subscriptionRepository.save(subscription)
        val addons = subscriptionAddonRepository.findActiveBySubscriptionId(saved.uid)
        return saved.asSubscriptionResponse(addons)
    }

    /**
     * Pause subscription
     */
    fun pauseSubscription(workspaceId: String, pauseDays: Int, reason: String?): SubscriptionResponse {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        if (subscription.isFree) {
            throw SubscriptionException.OperationNotAllowed("Cannot pause free subscription")
        }

        val now = Instant.now()
        subscription.apply {
            status = SubscriptionStatus.PAUSED
            pausedAt = now
            resumeAt = now.plus(Duration.ofDays(pauseDays.toLong().coerceAtMost(90)))
        }

        logger.info("Paused subscription for workspace: {} for {} days", workspaceId, pauseDays)
        val saved = subscriptionRepository.save(subscription)
        val addons = subscriptionAddonRepository.findActiveBySubscriptionId(saved.uid)
        return saved.asSubscriptionResponse(addons)
    }

    /**
     * Resume paused subscription
     */
    fun resumeSubscription(workspaceId: String): SubscriptionResponse {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        if (subscription.status != SubscriptionStatus.PAUSED) {
            throw SubscriptionException.OperationNotAllowed("Subscription is not paused")
        }

        subscription.apply {
            status = SubscriptionStatus.ACTIVE
            pausedAt = null
            resumeAt = null
        }

        logger.info("Resumed subscription for workspace: {}", workspaceId)
        val saved = subscriptionRepository.save(subscription)
        val addons = subscriptionAddonRepository.findActiveBySubscriptionId(saved.uid)
        return saved.asSubscriptionResponse(addons)
    }

    /**
     * Renew subscription (called after successful payment)
     */
    fun renewSubscription(workspaceId: String): Subscription {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        val now = Instant.now()
        val newPeriodEnd = now.plus(Duration.ofDays(subscription.billingCycle.months * 30L))

        subscription.apply {
            status = SubscriptionStatus.ACTIVE
            currentPeriodStart = now
            currentPeriodEnd = newPeriodEnd
            lastPaymentAt = now
            lastPaymentStatus = PaymentStatus.SUCCEEDED
            failedPaymentCount = 0
            gracePeriodEndsAt = null
        }

        logger.info("Renewed subscription for workspace: {} until {}", workspaceId, newPeriodEnd)
        return subscriptionRepository.save(subscription)
    }

    /**
     * Handle payment failure
     */
    fun handlePaymentFailure(workspaceId: String, failureReason: String?): Subscription {
        val subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        val now = Instant.now()
        subscription.apply {
            failedPaymentCount++
            lastPaymentStatus = PaymentStatus.FAILED

            // Move to PAST_DUE after first failure, set grace period
            if (status == SubscriptionStatus.ACTIVE) {
                status = SubscriptionStatus.PAST_DUE
                gracePeriodEndsAt = now.plus(Duration.ofDays(7)) // 7 day grace period
            }

            // Expire after 3 failures or grace period end
            if (failedPaymentCount >= 3) {
                status = SubscriptionStatus.EXPIRED
            }
        }

        logger.warn(
            "Payment failure for workspace: {}, attempt: {}, reason: {}",
            workspaceId, subscription.failedPaymentCount, failureReason
        )
        return subscriptionRepository.save(subscription)
    }

    // =====================
    // Usage & Limits
    // =====================

    /**
     * Get current usage for a workspace
     */
    fun getUsage(workspaceId: String): UsageResponse {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)

        val now = YearMonth.now()
        val usage = usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth(
            workspaceId, now.year, now.monthValue
        ) ?: UsageMetric.forCurrentPeriod(workspaceId)

        return usage.asUsageResponse(subscription?.plan)
    }

    /**
     * Check if a resource limit is exceeded
     */
    fun checkLimit(workspaceId: String, resourceType: String, currentCount: Int): LimitCheckResult {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
        val plan = subscription?.plan

        val limit = when (resourceType.uppercase()) {
            "CUSTOMER" -> plan?.maxCustomers ?: 50
            "PRODUCT" -> plan?.maxProducts ?: 50
            "INVOICE" -> plan?.maxInvoicesPerMonth ?: 20
            "MEMBER" -> plan?.maxMembersPerWorkspace ?: 1
            "DEVICE" -> plan?.maxDevices ?: 2
            "STORAGE_GB" -> plan?.maxStorageGb ?: 1
            else -> -1
        }

        // -1 means unlimited
        if (limit == -1) {
            return LimitCheckResult(
                allowed = true,
                limit = -1,
                current = currentCount,
                remaining = -1,
                isUnlimited = true
            )
        }

        val remaining = limit - currentCount
        val exceeded = currentCount >= limit
        val warning = currentCount >= (limit * 0.8)

        return LimitCheckResult(
            allowed = !exceeded,
            limit = limit,
            current = currentCount,
            remaining = remaining.coerceAtLeast(0),
            isUnlimited = false,
            exceeded = exceeded,
            warning = warning
        )
    }

    /**
     * Check if a feature is available
     */
    fun hasFeature(workspaceId: String, feature: String): Boolean {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: return false

        val plan = subscription.plan ?: return false

        return when (feature.uppercase()) {
            "API_ACCESS" -> plan.apiAccessEnabled
            "CUSTOM_BRANDING" -> plan.customBrandingEnabled
            "SSO" -> plan.ssoEnabled
            "AUDIT_LOGS" -> plan.auditLogsEnabled
            "PRIORITY_SUPPORT" -> plan.prioritySupport
            else -> {
                // Check if module is in available modules
                plan.availableModules.contains(feature, ignoreCase = true)
            }
        }
    }
}

/**
 * Result of limit check
 */
data class LimitCheckResult(
    val allowed: Boolean,
    val limit: Int,
    val current: Int,
    val remaining: Int,
    val isUnlimited: Boolean = false,
    val exceeded: Boolean = false,
    val warning: Boolean = false
)
