package com.ampairs.subscription.domain.repository

import com.ampairs.subscription.domain.model.Subscription
import com.ampairs.subscription.domain.model.SubscriptionStatus
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface SubscriptionRepository : JpaRepository<Subscription, Long> {

    fun findByUid(uid: String): Subscription?

    @EntityGraph("Subscription.withPlan")
    fun findWithPlanByUid(uid: String): Subscription?

    fun findByWorkspaceId(workspaceId: String): Subscription?

    @EntityGraph("Subscription.withPlan")
    fun findWithPlanByWorkspaceId(workspaceId: String): Subscription?

    fun findByExternalSubscriptionId(externalSubscriptionId: String): Subscription?

    fun findByExternalCustomerId(externalCustomerId: String): List<Subscription>

    @Query("SELECT s FROM Subscription s WHERE s.status = :status")
    fun findByStatus(status: SubscriptionStatus): List<Subscription>

    @Query("SELECT s FROM Subscription s WHERE s.status = :status")
    fun findAllByStatus(status: SubscriptionStatus): List<Subscription>

    @Query("SELECT s FROM Subscription s WHERE s.status IN :statuses")
    fun findByStatusIn(statuses: List<SubscriptionStatus>): List<Subscription>

    /**
     * Find subscriptions expiring soon (for renewal reminders)
     */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'ACTIVE'
        AND s.currentPeriodEnd BETWEEN :from AND :to
        AND s.cancelAtPeriodEnd = false
    """)
    fun findExpiringSoon(
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<Subscription>

    /**
     * Find subscriptions in grace period (payment failed)
     */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'PAST_DUE'
        AND s.gracePeriodEndsAt > :now
    """)
    fun findInGracePeriod(@Param("now") now: Instant): List<Subscription>

    /**
     * Find subscriptions whose grace period has ended
     */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'PAST_DUE'
        AND s.gracePeriodEndsAt <= :now
    """)
    fun findGracePeriodExpired(@Param("now") now: Instant): List<Subscription>

    /**
     * Find trials ending soon
     */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'TRIALING'
        AND s.trialEndsAt BETWEEN :from AND :to
    """)
    fun findTrialsEndingSoon(
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<Subscription>

    /**
     * Find expired trials
     */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'TRIALING'
        AND s.trialEndsAt < :now
    """)
    fun findExpiredTrials(@Param("now") now: Instant): List<Subscription>

    /**
     * Find subscriptions scheduled for cancellation
     */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.cancelAtPeriodEnd = true
        AND s.currentPeriodEnd <= :now
        AND s.status = 'ACTIVE'
    """)
    fun findScheduledForCancellation(@Param("now") now: Instant): List<Subscription>

    /**
     * Update subscription status
     */
    @Modifying
    @Transactional
    @Query("UPDATE Subscription s SET s.status = :status, s.updatedAt = :now WHERE s.uid = :uid")
    fun updateStatus(
        @Param("uid") uid: String,
        @Param("status") status: SubscriptionStatus,
        @Param("now") now: Instant
    )

    /**
     * Record payment
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Subscription s
        SET s.lastPaymentAt = :paymentAt,
            s.lastPaymentStatus = :status,
            s.failedPaymentCount = 0,
            s.updatedAt = :paymentAt
        WHERE s.uid = :uid
    """)
    fun recordSuccessfulPayment(
        @Param("uid") uid: String,
        @Param("paymentAt") paymentAt: Instant,
        @Param("status") status: com.ampairs.subscription.domain.model.PaymentStatus
    )

    /**
     * Increment failed payment count
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Subscription s
        SET s.failedPaymentCount = s.failedPaymentCount + 1,
            s.lastPaymentStatus = :status,
            s.updatedAt = :now
        WHERE s.uid = :uid
    """)
    fun recordFailedPayment(
        @Param("uid") uid: String,
        @Param("status") status: com.ampairs.subscription.domain.model.PaymentStatus,
        @Param("now") now: Instant
    )

    fun existsByWorkspaceId(workspaceId: String): Boolean
}
