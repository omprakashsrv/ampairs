package com.ampairs.subscription.domain.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Instant

/**
 * Active subscription for a workspace.
 * Links a workspace to a subscription plan with billing information.
 */
@Entity
@Table(
    name = "subscriptions",
    indexes = [
        Index(name = "idx_subscription_uid", columnList = "uid", unique = true),
        Index(name = "idx_subscription_workspace", columnList = "workspace_id"),
        Index(name = "idx_subscription_status", columnList = "status"),
        Index(name = "idx_subscription_provider", columnList = "payment_provider"),
        Index(name = "idx_subscription_period_end", columnList = "current_period_end")
    ]
)
@NamedEntityGraph(
    name = "Subscription.withPlan",
    attributeNodes = [NamedAttributeNode("plan")]
)
class Subscription : BaseDomain() {

    /**
     * Workspace this subscription belongs to
     */
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    /**
     * Reference to subscription plan definition
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", referencedColumnName = "uid")
    var plan: SubscriptionPlanDefinition? = null

    /**
     * Plan code for quick reference (denormalized)
     */
    @Column(name = "plan_code", nullable = false, length = 50)
    var planCode: String = "FREE"

    /**
     * Current subscription status
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE

    /**
     * Billing cycle
     */
    @Column(name = "billing_cycle", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var billingCycle: BillingCycle = BillingCycle.MONTHLY

    /**
     * Payment provider used for this subscription
     */
    @Column(name = "payment_provider", length = 30)
    @Enumerated(EnumType.STRING)
    var paymentProvider: PaymentProvider? = null

    /**
     * External subscription ID from payment provider
     */
    @Column(name = "external_subscription_id", length = 255)
    var externalSubscriptionId: String? = null

    /**
     * External customer ID from payment provider
     */
    @Column(name = "external_customer_id", length = 255)
    var externalCustomerId: String? = null

    /**
     * Currency for billing
     */
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "INR"

    /**
     * Current billing period start
     */
    @Column(name = "current_period_start")
    var currentPeriodStart: Instant? = null

    /**
     * Current billing period end
     */
    @Column(name = "current_period_end")
    var currentPeriodEnd: Instant? = null

    /**
     * Trial end date (null if no trial or trial ended)
     */
    @Column(name = "trial_ends_at")
    var trialEndsAt: Instant? = null

    /**
     * Whether to cancel at period end
     */
    @Column(name = "cancel_at_period_end", nullable = false)
    var cancelAtPeriodEnd: Boolean = false

    /**
     * When cancellation was requested
     */
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null

    /**
     * Reason for cancellation
     */
    @Column(name = "cancellation_reason", length = 500)
    var cancellationReason: String? = null

    /**
     * When subscription was paused
     */
    @Column(name = "paused_at")
    var pausedAt: Instant? = null

    /**
     * When subscription should resume
     */
    @Column(name = "resume_at")
    var resumeAt: Instant? = null

    /**
     * Next billing amount
     */
    @Column(name = "next_billing_amount")
    var nextBillingAmount: Double? = null

    /**
     * Last payment status
     */
    @Column(name = "last_payment_status", length = 20)
    @Enumerated(EnumType.STRING)
    var lastPaymentStatus: PaymentStatus? = null

    /**
     * Last payment date
     */
    @Column(name = "last_payment_at")
    var lastPaymentAt: Instant? = null

    /**
     * Number of failed payment attempts
     */
    @Column(name = "failed_payment_count", nullable = false)
    var failedPaymentCount: Int = 0

    /**
     * Grace period end for failed payments
     */
    @Column(name = "grace_period_ends_at")
    var gracePeriodEndsAt: Instant? = null

    /**
     * Whether this is a free subscription (no payment required)
     */
    @Column(name = "is_free", nullable = false)
    var isFree: Boolean = true

    /**
     * Metadata JSON (for additional provider-specific data)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String = "{}"

    override fun obtainSeqIdPrefix(): String {
        return "SUB"
    }

    /**
     * Check if subscription is currently active
     */
    fun isActive(): Boolean {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING
    }

    /**
     * Check if subscription is in trial
     */
    fun isInTrial(): Boolean {
        return status == SubscriptionStatus.TRIALING &&
                trialEndsAt?.isAfter(Instant.now()) == true
    }

    /**
     * Check if subscription is past due
     */
    fun isPastDue(): Boolean {
        return status == SubscriptionStatus.PAST_DUE
    }

    /**
     * Check if subscription has expired
     */
    fun isExpired(): Boolean {
        return status == SubscriptionStatus.EXPIRED ||
                (currentPeriodEnd?.isBefore(Instant.now()) == true && !isFree)
    }

    /**
     * Get days remaining in current period
     */
    fun getDaysRemaining(): Long {
        val end = currentPeriodEnd ?: return 0
        val now = Instant.now()
        if (end.isBefore(now)) return 0
        return java.time.Duration.between(now, end).toDays()
    }
}
