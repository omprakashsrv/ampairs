package com.ampairs.subscription.scheduler

import com.ampairs.subscription.domain.service.SubscriptionDowngradeService
import com.ampairs.subscription.domain.service.UsageTrackingService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled jobs for subscription management.
 *
 * Jobs:
 * 1. Process subscription downgrades (expired/failed payments)
 * 2. Reset monthly usage counters
 * 3. Send renewal reminders
 * 4. Expire trials
 */
@Component
@ConditionalOnProperty(
    name = ["subscription.scheduled-jobs.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class SubscriptionScheduledJobs(
    private val downgradeService: SubscriptionDowngradeService,
    private val usageTrackingService: UsageTrackingService
) {
    private val logger = LoggerFactory.getLogger(SubscriptionScheduledJobs::class.java)

    /**
     * Process subscription downgrades every hour.
     *
     * Checks for:
     * - Subscriptions with expired grace period (3+ failed payments)
     * - Subscriptions past their currentPeriodEnd date
     *
     * Auto-downgrades to FREE plan instead of setting CANCELLED/EXPIRED status.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    fun processSubscriptionDowngrades() {
        logger.info("Starting subscription downgrade job")

        try {
            val downgradeCount = downgradeService.processSubscriptionDowngrades()
            logger.info("Subscription downgrade job completed: {} subscriptions processed", downgradeCount)
        } catch (e: Exception) {
            logger.error("Error in subscription downgrade job", e)
        }
    }

    /**
     * Reset monthly usage counters.
     *
     * Runs on the 1st day of every month at 00:05 AM.
     *
     * Resets counters for:
     * - invoiceCount (monthly limit)
     * - orderCount (monthly limit)
     * - apiCalls (monthly quota)
     * - smsCount (monthly quota)
     * - emailCount (monthly quota)
     *
     * Does NOT reset:
     * - customerCount (cumulative)
     * - productCount (cumulative)
     * - memberCount (cumulative)
     * - deviceCount (cumulative)
     * - storageUsedBytes (cumulative)
     */
    @Scheduled(cron = "0 5 0 1 * *") // 1st of month at 00:05 AM
    fun resetMonthlyUsageCounters() {
        logger.info("Starting monthly usage counter reset job")

        try {
            val resetCount = usageTrackingService.resetMonthlyCounters()
            logger.info("Monthly usage reset completed: {} workspaces processed", resetCount)
        } catch (e: Exception) {
            logger.error("Error in monthly usage reset job", e)
        }
    }

    /**
     * Send renewal reminders for subscriptions expiring soon.
     *
     * Runs daily at 9:00 AM.
     *
     * Sends reminders:
     * - 7 days before expiry
     * - 3 days before expiry
     * - 1 day before expiry
     */
    @Scheduled(cron = "0 0 9 * * *") // Daily at 9:00 AM
    fun sendRenewalReminders() {
        logger.info("Starting renewal reminder job")

        try {
            // TODO: Implement renewal reminder logic
            // 1. Find subscriptions expiring in 7/3/1 days
            // 2. Check if reminder already sent for this period
            // 3. Send email/notification to workspace owner
            // 4. Mark reminder as sent

            logger.info("Renewal reminder job completed")
        } catch (e: Exception) {
            logger.error("Error in renewal reminder job", e)
        }
    }

    /**
     * Expire trials that have ended.
     *
     * Runs every hour.
     *
     * Trials that end:
     * - If user hasn't subscribed, downgrade to FREE
     * - Prompt user to subscribe
     */
    @Scheduled(cron = "0 15 * * * *") // Every hour at minute 15
    fun expireTrials() {
        logger.info("Starting trial expiry job")

        try {
            // TODO: Implement trial expiry logic
            // 1. Find trials where trialEndsAt < now
            // 2. If no payment method, downgrade to FREE
            // 3. If payment method exists, attempt to charge
            // 4. Send notification to user

            logger.info("Trial expiry job completed")
        } catch (e: Exception) {
            logger.error("Error in trial expiry job", e)
        }
    }

    /**
     * Clean up old usage metrics (keep last 12 months).
     *
     * Runs on the 2nd day of every month at 02:00 AM.
     *
     * Keeps historical data for analytics while preventing unbounded growth.
     */
    @Scheduled(cron = "0 0 2 2 * *") // 2nd of month at 02:00 AM
    fun cleanupOldUsageMetrics() {
        logger.info("Starting usage metrics cleanup job")

        try {
            val deletedCount = usageTrackingService.deleteOldUsageMetrics(monthsToKeep = 12)
            logger.info("Usage metrics cleanup completed: {} old records deleted", deletedCount)
        } catch (e: Exception) {
            logger.error("Error in usage metrics cleanup job", e)
        }
    }
}
