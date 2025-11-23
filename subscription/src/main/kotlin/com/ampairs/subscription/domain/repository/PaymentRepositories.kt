package com.ampairs.subscription.domain.repository

import com.ampairs.subscription.domain.model.PaymentMethod
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.PaymentStatus
import com.ampairs.subscription.domain.model.PaymentTransaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// =====================
// Payment Transaction Repository
// =====================

@Repository
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, Long> {

    fun findByUid(uid: String): PaymentTransaction?

    fun findByExternalPaymentId(externalPaymentId: String): PaymentTransaction?

    fun findByExternalInvoiceId(externalInvoiceId: String): PaymentTransaction?

    fun findBySubscriptionId(subscriptionId: String): List<PaymentTransaction>

    fun findByWorkspaceId(workspaceId: String): List<PaymentTransaction>

    @Query("""
        SELECT p FROM PaymentTransaction p
        WHERE p.workspaceId = :workspaceId
        ORDER BY p.createdAt DESC
    """)
    fun findByWorkspaceIdOrderByCreatedAtDesc(
        workspaceId: String,
        pageable: Pageable
    ): Page<PaymentTransaction>

    @Query("""
        SELECT p FROM PaymentTransaction p
        WHERE p.subscriptionId = :subscriptionId
        ORDER BY p.createdAt DESC
    """)
    fun findBySubscriptionIdOrderByCreatedAtDesc(
        subscriptionId: String,
        pageable: Pageable
    ): Page<PaymentTransaction>

    fun findByStatus(status: PaymentStatus): List<PaymentTransaction>

    @Query("""
        SELECT p FROM PaymentTransaction p
        WHERE p.subscriptionId = :subscriptionId
        AND p.status = 'SUCCEEDED'
        ORDER BY p.createdAt DESC
    """)
    fun findSuccessfulBySubscriptionId(subscriptionId: String): List<PaymentTransaction>

    @Query("""
        SELECT p FROM PaymentTransaction p
        WHERE p.subscriptionId = :subscriptionId
        AND p.status = 'FAILED'
        ORDER BY p.createdAt DESC
    """)
    fun findFailedBySubscriptionId(subscriptionId: String): List<PaymentTransaction>

    /**
     * Get total revenue for a workspace
     */
    @Query("""
        SELECT COALESCE(SUM(p.netAmount), 0)
        FROM PaymentTransaction p
        WHERE p.workspaceId = :workspaceId
        AND p.status = 'SUCCEEDED'
    """)
    fun getTotalRevenueByWorkspaceId(workspaceId: String): Double

    /**
     * Get payments in date range
     */
    @Query("""
        SELECT p FROM PaymentTransaction p
        WHERE p.createdAt BETWEEN :from AND :to
        ORDER BY p.createdAt DESC
    """)
    fun findByDateRange(
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        pageable: Pageable
    ): Page<PaymentTransaction>

    @Modifying
    @Transactional
    @Query("UPDATE PaymentTransaction p SET p.status = :status, p.updatedAt = :now WHERE p.uid = :uid")
    fun updateStatus(
        @Param("uid") uid: String,
        @Param("status") status: PaymentStatus,
        @Param("now") now: Instant
    )
}

// =====================
// Payment Method Repository
// =====================

@Repository
interface PaymentMethodRepository : JpaRepository<PaymentMethod, Long> {

    fun findByUid(uid: String): PaymentMethod?

    fun findByWorkspaceId(workspaceId: String): List<PaymentMethod>

    @Query("""
        SELECT pm FROM PaymentMethod pm
        WHERE pm.workspaceId = :workspaceId
        AND pm.active = true
        ORDER BY pm.isDefault DESC, pm.createdAt DESC
    """)
    fun findActiveByWorkspaceId(workspaceId: String): List<PaymentMethod>

    @Query("""
        SELECT pm FROM PaymentMethod pm
        WHERE pm.workspaceId = :workspaceId
        AND pm.isDefault = true
        AND pm.active = true
    """)
    fun findDefaultByWorkspaceId(workspaceId: String): PaymentMethod?

    fun findByExternalPaymentMethodId(externalPaymentMethodId: String): PaymentMethod?

    fun findByWorkspaceIdAndPaymentProvider(
        workspaceId: String,
        paymentProvider: PaymentProvider
    ): List<PaymentMethod>

    /**
     * Clear default flag for all payment methods of a workspace
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE PaymentMethod pm
        SET pm.isDefault = false, pm.updatedAt = :now
        WHERE pm.workspaceId = :workspaceId
    """)
    fun clearDefaultByWorkspaceId(
        @Param("workspaceId") workspaceId: String,
        @Param("now") now: Instant
    )

    /**
     * Set payment method as default
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE PaymentMethod pm
        SET pm.isDefault = true, pm.updatedAt = :now
        WHERE pm.uid = :uid
    """)
    fun setAsDefault(
        @Param("uid") uid: String,
        @Param("now") now: Instant
    )

    /**
     * Deactivate payment method
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE PaymentMethod pm
        SET pm.active = false, pm.updatedAt = :now
        WHERE pm.uid = :uid
    """)
    fun deactivate(
        @Param("uid") uid: String,
        @Param("now") now: Instant
    )

    /**
     * Find expiring cards (for notifications)
     */
    @Query("""
        SELECT pm FROM PaymentMethod pm
        WHERE pm.active = true
        AND pm.type = 'CARD'
        AND pm.expYear = :year
        AND pm.expMonth <= :month
    """)
    fun findExpiringCards(
        @Param("year") year: Int,
        @Param("month") month: Int
    ): List<PaymentMethod>
}
