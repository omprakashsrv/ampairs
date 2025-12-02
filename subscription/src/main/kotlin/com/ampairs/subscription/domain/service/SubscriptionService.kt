package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.domain.repository.*
import com.ampairs.subscription.exception.SubscriptionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.YearMonth

@Service
@Transactional
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val subscriptionAddonRepository: SubscriptionAddonRepository,
    private val usageMetricRepository: UsageMetricRepository,
    private val workspaceRepository: com.ampairs.workspace.repository.WorkspaceRepository,
    private val subscriptionDowngradeService: SubscriptionDowngradeService
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
     * Auto-creates a FREE subscription if none exists
     */
    fun getSubscription(workspaceId: String): SubscriptionResponse {
        var subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)

        // Auto-create FREE subscription if none exists (lazy initialization)
        if (subscription == null) {
            logger.info("No subscription found for workspace: $workspaceId, creating FREE subscription")
            subscription = createFreeSubscription(workspaceId)
            // Reload with plan details
            subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
                ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)
        }

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

        // Check if trial is allowed
        val existingSubscription = subscriptionRepository.findByWorkspaceId(workspaceId)

        // Prevent trial abuse - only allow trial once per workspace
        if (existingSubscription != null) {
            // Check if user has already used trial for any paid plan
            if (existingSubscription.trialEndsAt != null) {
                throw SubscriptionException.TrialAlreadyUsed(workspaceId)
            }

            // If already on a paid plan, cannot start trial
            if (!existingSubscription.isFree && existingSubscription.status == SubscriptionStatus.ACTIVE) {
                throw SubscriptionException.AlreadyHasActivePlan(workspaceId, existingSubscription.planCode)
            }
        }

        val subscription = existingSubscription ?: Subscription().apply {
            this.workspaceId = workspaceId
        }

        val now = Instant.now()
        val trialEnd = now.plus(Duration.ofDays(trialDays.toLong()))

        subscription.apply {
            this.plan = plan
            this.planCode = planCode
            this.status = SubscriptionStatus.TRIALING
            this.billingCycle = BillingCycle.MONTHLY
            this.isFree = false
            this.currentPeriodStart = now
            this.currentPeriodEnd = trialEnd
            this.trialEndsAt = trialEnd
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
            this.nextBillingAmount = calculatePriceWithDiscount(workspaceId, plan, currency, billingCycle)
        }

        logger.info(
            "Activating subscription for workspace: {}, plan: {}, cycle: {}, provider: {}",
            workspaceId, planCode, billingCycle, provider
        )
        return subscriptionRepository.save(subscription)
    }

    /**
     * Change subscription plan (always immediate)
     */
    fun changePlan(
        workspaceId: String,
        newPlanCode: String,
        billingCycle: BillingCycle,
        immediate: Boolean
    ): ChangePlanResponse {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        val oldPlan = subscription.plan
        val newPlan = subscriptionPlanRepository.findByPlanCode(newPlanCode)
            ?: throw SubscriptionException.PlanNotFoundException(newPlanCode)

        // Calculate proration for paid plans
        var prorationAmount: BigDecimal? = null
        val isNewPlanFree = newPlan.isFree()
        if (!subscription.isFree && !isNewPlanFree && subscription.currentPeriodEnd != null) {
            val daysRemaining = subscription.getDaysRemaining()
            val totalDays = subscription.billingCycle.months * 30
            val currentBillingAmount = subscription.nextBillingAmount ?: BigDecimal.ZERO
            val unusedAmount = currentBillingAmount.multiply(BigDecimal(daysRemaining)).divide(BigDecimal(totalDays), 2, RoundingMode.HALF_UP)
            val newAmount = calculatePriceWithDiscount(workspaceId, newPlan, subscription.currency, billingCycle)
            prorationAmount = newAmount.subtract(unusedAmount)
        }

        // Apply plan change immediately
        val now = Instant.now()
        subscription.apply {
            this.plan = newPlan
            this.planCode = newPlanCode
            this.billingCycle = billingCycle
            this.currentPeriodStart = now
            this.currentPeriodEnd = now.plus(Duration.ofDays(billingCycle.months * 30L))
            this.nextBillingAmount = calculatePriceWithDiscount(workspaceId, newPlan, currency, billingCycle)
            this.isFree = isNewPlanFree
        }
        subscriptionRepository.save(subscription)

        // Check current usage against new plan limits
        val usageWarnings = checkUsageAgainstNewPlan(workspaceId, newPlan)

        logger.info(
            "Plan changed for workspace: {} from {} to {} (billing: {})",
            workspaceId, oldPlan?.planCode ?: "FREE", newPlanCode, billingCycle
        )

        val addons = subscriptionAddonRepository.findActiveBySubscriptionId(subscription.uid)
        return ChangePlanResponse(
            subscription = subscription.asSubscriptionResponse(addons),
            prorationAmount = prorationAmount,
            effectiveAt = now,
            isImmediate = true,
            usageWarnings = usageWarnings
        )
    }

    /**
     * Check if current usage exceeds new plan limits
     */
    private fun checkUsageAgainstNewPlan(workspaceId: String, newPlan: SubscriptionPlanDefinition): List<String> {
        val warnings = mutableListOf<String>()
        val usage = getUsage(workspaceId)

        // Check each resource limit
        if (newPlan.maxCustomers > 0 && usage.usage.customerCount > newPlan.maxCustomers) {
            warnings.add("Customer count (${usage.usage.customerCount}) exceeds new plan limit (${newPlan.maxCustomers})")
        }
        if (newPlan.maxProducts > 0 && usage.usage.productCount > newPlan.maxProducts) {
            warnings.add("Product count (${usage.usage.productCount}) exceeds new plan limit (${newPlan.maxProducts})")
        }
        if (newPlan.maxInvoicesPerMonth > 0 && usage.usage.invoiceCount > newPlan.maxInvoicesPerMonth) {
            warnings.add("Invoice count (${usage.usage.invoiceCount}) exceeds new plan limit (${newPlan.maxInvoicesPerMonth})")
        }
        if (newPlan.maxMembersPerWorkspace > 0 && usage.usage.memberCount > newPlan.maxMembersPerWorkspace) {
            warnings.add("Member count (${usage.usage.memberCount}) exceeds new plan limit (${newPlan.maxMembersPerWorkspace})")
        }
        if (newPlan.maxDevices > 0 && usage.usage.deviceCount > newPlan.maxDevices) {
            warnings.add("Device count (${usage.usage.deviceCount}) exceeds new plan limit (${newPlan.maxDevices})")
        }
        if (newPlan.maxStorageGb > 0 && usage.usage.storageUsedGb > newPlan.maxStorageGb) {
            warnings.add("Storage usage (${usage.usage.storageUsedGb}GB) exceeds new plan limit (${newPlan.maxStorageGb}GB)")
        }

        return warnings
    }

    /**
     * Cancel subscription - automatically downgrades to FREE plan
     */
    fun cancelSubscription(workspaceId: String, immediate: Boolean, reason: String?): SubscriptionResponse {
        val subscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        val now = Instant.now()

        // Cancel all addons regardless of immediate or scheduled cancellation
        subscriptionAddonRepository.cancelAllBySubscriptionId(subscription.uid, now)

        if (immediate) {
            // Immediately downgrade to FREE plan instead of marking as cancelled
            subscriptionDowngradeService.handleUserCancellation(workspaceId, reason)
            logger.info("Immediately cancelled and downgraded subscription to FREE for workspace: {}", workspaceId)

            // Reload subscription to get updated details
            val updatedSubscription = subscriptionRepository.findWithPlanByWorkspaceId(workspaceId)
                ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

            val addons = subscriptionAddonRepository.findActiveBySubscriptionId(updatedSubscription.uid)
            return updatedSubscription.asSubscriptionResponse(addons)
        } else {
            // Schedule cancellation at period end
            subscription.cancelAtPeriodEnd = true
            subscription.cancelledAt = now
            subscription.cancellationReason = reason
            logger.info("Scheduled cancellation for workspace: {} at period end (will downgrade to FREE)", workspaceId)

            val saved = subscriptionRepository.save(subscription)
            val addons = subscriptionAddonRepository.findActiveBySubscriptionId(saved.uid)
            return saved.asSubscriptionResponse(addons)
        }
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

    // =====================
    // Private Helper Methods
    // =====================

    /**
     * Calculate subscription price with all discounts applied
     * (multi-workspace + seasonal + billing cycle)
     *
     * @param workspaceId Workspace ID to get the owner from
     * @param plan Subscription plan
     * @param currency Currency code (INR/USD)
     * @param billingCycle Billing cycle for annual discount
     * @return Final discounted price with all discounts stacked
     */
    private fun calculatePriceWithDiscount(
        workspaceId: String,
        plan: SubscriptionPlanDefinition,
        currency: String,
        billingCycle: BillingCycle
    ): BigDecimal {
        // Get workspace to find the creator/owner
        val workspace = workspaceRepository.findByUid(workspaceId).orElse(null)
        val userId = workspace?.createdBy

        // If we can't determine the user, return standard price without discount
        if (userId == null) {
            return billingCycle.calculateDiscountedPrice(plan.getMonthlyPrice(currency))
        }

        // Count active workspaces created by this user
        val workspaceCount = workspaceRepository.countByCreatedByAndActiveTrue(userId)

        // Get base price with multi-workspace AND seasonal discounts
        val pricePerWorkspace = plan.getPriceWithAllDiscounts(currency, workspaceCount)

        // Apply billing cycle discount on top (annual gets extra discount)
        return billingCycle.calculateDiscountedPrice(pricePerWorkspace)
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
