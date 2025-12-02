package com.ampairs.subscription.domain.repository

import com.ampairs.subscription.domain.model.InvoiceGenerationLog
import com.ampairs.subscription.domain.model.InvoiceGenerationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface InvoiceGenerationLogRepository : JpaRepository<InvoiceGenerationLog, Long> {

    /**
     * Find log for a workspace and billing period (idempotency check)
     */
    fun findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth(
        workspaceId: String,
        year: Int,
        month: Int
    ): InvoiceGenerationLog?

    /**
     * Find all logs for a workspace
     */
    fun findByWorkspaceIdOrderByBillingPeriodYearDescBillingPeriodMonthDesc(
        workspaceId: String
    ): List<InvoiceGenerationLog>

    /**
     * Find all failed logs that should be retried
     */
    @Query("""
        SELECT l FROM InvoiceGenerationLog l
        WHERE l.status = 'FAILED'
        AND l.shouldRetry = true
        AND l.nextRetryAt IS NOT NULL
        AND l.nextRetryAt < :now
        AND l.attemptCount < 5
        ORDER BY l.nextRetryAt ASC
    """)
    fun findFailedLogsReadyForRetry(@Param("now") now: Instant): List<InvoiceGenerationLog>

    /**
     * Find all pending logs (not yet attempted)
     */
    fun findByStatus(status: InvoiceGenerationStatus): List<InvoiceGenerationLog>

    /**
     * Find all logs for a specific billing period across all workspaces
     */
    fun findByBillingPeriodYearAndBillingPeriodMonth(
        year: Int,
        month: Int
    ): List<InvoiceGenerationLog>

    /**
     * Count successful generations for a period
     */
    @Query("""
        SELECT COUNT(l) FROM InvoiceGenerationLog l
        WHERE l.billingPeriodYear = :year
        AND l.billingPeriodMonth = :month
        AND l.status = 'SUCCESS'
    """)
    fun countSuccessfulGenerations(
        @Param("year") year: Int,
        @Param("month") month: Int
    ): Long

    /**
     * Count failed generations for a period
     */
    @Query("""
        SELECT COUNT(l) FROM InvoiceGenerationLog l
        WHERE l.billingPeriodYear = :year
        AND l.billingPeriodMonth = :month
        AND l.status = 'FAILED'
        AND l.shouldRetry = true
    """)
    fun countFailedGenerations(
        @Param("year") year: Int,
        @Param("month") month: Int
    ): Long

    /**
     * Find all logs without successful payment link
     */
    @Query("""
        SELECT l FROM InvoiceGenerationLog l
        WHERE l.status = 'SUCCESS'
        AND l.paymentStatus IN ('NOT_STARTED', 'AUTO_CHARGE_FAILED', 'LINK_FAILED')
        AND l.invoiceId IS NOT NULL
        ORDER BY l.createdAt ASC
    """)
    fun findLogsWithPendingPaymentProcessing(): List<InvoiceGenerationLog>
}
