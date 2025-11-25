package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.model.Subscription
import com.ampairs.subscription.domain.model.SubscriptionStatus
import com.ampairs.subscription.domain.repository.SubscriptionPlanRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.exception.SubscriptionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service for handling subscription downgrades and payment failures.
 *
 * Key behavior: On payment failure or expiry, we AUTO-DOWNGRADE to FREE plan
 * instead of setting status to CANCELLED. This ensures:
 * - Users don't lose access completely
 * - FREE plan restrictions are immediately applied
 * - No "CANCELLED" or "EXPIRED" states - only ACTIVE with appropriate plan
 */
@Service
@Transactional
class SubscriptionDowngradeService(
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) {
    private val logger = LoggerFactory.getLogger(SubscriptionDowngradeService::class.java)

    /**
     * Downgrade subscription to FREE plan.
     * Called when:
     * - Payment fails after grace period
     * - Subscription expires and not renewed
     * - User cancels paid subscription
     *
     * @param workspaceId Workspace to downgrade
     * @param reason Reason for downgrade (for logging/audit)
     */
    fun downgradeToFreePlan(workspaceId: String, reason: String): Subscription {
        val subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        // Already on FREE plan
        if (subscription.planCode == "FREE" && subscription.isFree) {
            logger.info("Workspace {} already on FREE plan, no downgrade needed", workspaceId)
            return subscription
        }

        val freePlan = subscriptionPlanRepository.findByPlanCode("FREE")
            ?: throw SubscriptionException.PlanNotFoundException("FREE")

        val now = Instant.now()

        subscription.apply {
            this.plan = freePlan
            this.planCode = "FREE"
            this.status = SubscriptionStatus.ACTIVE
            this.billingCycle = com.ampairs.subscription.domain.model.BillingCycle.MONTHLY
            this.currency = "INR"
            this.isFree = true
            this.currentPeriodStart = now
            this.currentPeriodEnd = null // Free plans don't expire
            this.nextBillingAmount = null
            this.trialEndsAt = null
            this.cancelledAt = null
            this.paymentProvider = null
            this.externalSubscriptionId = null
            this.externalCustomerId = null
        }

        val saved = subscriptionRepository.save(subscription)

        logger.info(
            "Downgraded workspace {} to FREE plan. Reason: {}",
            workspaceId, reason
        )

        return saved
    }

    /**
     * Handle payment failure.
     * After grace period expires, automatically downgrade to FREE plan.
     *
     * @param workspaceId Workspace with failed payment
     * @param failedPaymentCount Number of consecutive failed payments
     */
    fun handlePaymentFailure(workspaceId: String, failedPaymentCount: Int) {
        val subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        if (subscription.isFree) {
            logger.info("Workspace {} already on FREE plan, ignoring payment failure", workspaceId)
            return
        }

        val now = Instant.now()

        when {
            // First failure - mark as PAST_DUE, start grace period
            failedPaymentCount == 1 -> {
                subscription.status = SubscriptionStatus.PAST_DUE
                subscription.failedPaymentCount = failedPaymentCount
                subscription.lastPaymentStatus = com.ampairs.subscription.domain.model.PaymentStatus.FAILED
                subscriptionRepository.save(subscription)

                logger.warn(
                    "Payment failed for workspace {}, marked as PAST_DUE (attempt {})",
                    workspaceId, failedPaymentCount
                )
            }

            // Multiple failures within grace period (e.g., < 3 attempts)
            failedPaymentCount < 3 -> {
                subscription.failedPaymentCount = failedPaymentCount
                subscription.lastPaymentStatus = com.ampairs.subscription.domain.model.PaymentStatus.FAILED
                subscriptionRepository.save(subscription)

                logger.warn(
                    "Payment retry failed for workspace {} (attempt {})",
                    workspaceId, failedPaymentCount
                )
            }

            // Grace period expired, too many failures - downgrade to FREE
            else -> {
                downgradeToFreePlan(
                    workspaceId = workspaceId,
                    reason = "Payment failed after $failedPaymentCount attempts"
                )

                logger.error(
                    "Payment failed {} times for workspace {}, downgraded to FREE plan",
                    failedPaymentCount, workspaceId
                )
            }
        }
    }

    /**
     * Handle subscription expiry (not renewed).
     * Instead of marking as EXPIRED, downgrade to FREE plan.
     *
     * @param workspaceId Workspace with expired subscription
     */
    fun handleSubscriptionExpiry(workspaceId: String) {
        val subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.SubscriptionNotFoundException(workspaceId)

        if (subscription.isFree) {
            logger.info("Workspace {} already on FREE plan, ignoring expiry", workspaceId)
            return
        }

        val now = Instant.now()
        val periodEnd = subscription.currentPeriodEnd

        // Check if actually expired
        if (periodEnd == null || now.isBefore(periodEnd)) {
            logger.info(
                "Subscription for workspace {} not yet expired (ends: {})",
                workspaceId, periodEnd
            )
            return
        }

        downgradeToFreePlan(
            workspaceId = workspaceId,
            reason = "Subscription expired on $periodEnd, not renewed"
        )

        logger.info(
            "Subscription expired for workspace {}, downgraded to FREE plan",
            workspaceId
        )
    }

    /**
     * Handle user-initiated cancellation.
     * Downgrade to FREE plan immediately.
     *
     * @param workspaceId Workspace to cancel
     * @param reason User's cancellation reason
     */
    fun handleUserCancellation(workspaceId: String, reason: String?) {
        downgradeToFreePlan(
            workspaceId = workspaceId,
            reason = "User cancelled: ${reason ?: "No reason provided"}"
        )

        logger.info(
            "User cancelled subscription for workspace {}, downgraded to FREE plan",
            workspaceId
        )
    }

    /**
     * Get all subscriptions that should be downgraded.
     * Returns subscriptions that are:
     * - PAST_DUE with excessive failed payments
     * - ACTIVE but expired (currentPeriodEnd < now)
     *
     * @return List of subscriptions to downgrade
     */
    fun getSubscriptionsToDowngrade(): List<Subscription> {
        val now = Instant.now()

        // Find subscriptions in PAST_DUE with 3+ failed payments
        val pastDueSubscriptions = subscriptionRepository.findAllByStatus(SubscriptionStatus.PAST_DUE)
            .filter { it.failedPaymentCount >= 3 && !it.isFree }

        // Find expired subscriptions (period ended but still ACTIVE)
        val expiredSubscriptions = subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE)
            .filter {
                !it.isFree &&
                it.currentPeriodEnd != null &&
                now.isAfter(it.currentPeriodEnd)
            }

        return pastDueSubscriptions + expiredSubscriptions
    }

    /**
     * Process all subscriptions that need downgrading.
     * Typically called by scheduled job.
     *
     * @return Number of subscriptions downgraded
     */
    fun processSubscriptionDowngrades(): Int {
        val subscriptionsToDowngrade = getSubscriptionsToDowngrade()

        if (subscriptionsToDowngrade.isEmpty()) {
            logger.debug("No subscriptions to downgrade")
            return 0
        }

        var downgradeCount = 0

        subscriptionsToDowngrade.forEach { subscription ->
            try {
                when {
                    subscription.status == SubscriptionStatus.PAST_DUE -> {
                        handlePaymentFailure(subscription.workspaceId, subscription.failedPaymentCount)
                    }
                    subscription.currentPeriodEnd != null -> {
                        handleSubscriptionExpiry(subscription.workspaceId)
                    }
                }
                downgradeCount++
            } catch (e: Exception) {
                logger.error(
                    "Failed to downgrade subscription for workspace {}",
                    subscription.workspaceId,
                    e
                )
            }
        }

        logger.info("Processed {} subscription downgrades", downgradeCount)
        return downgradeCount
    }
}
