package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Instant

/**
 * Tracks invoice generation attempts for reconciliation and failure handling.
 * Ensures idempotency and provides audit trail for invoice generation.
 *
 * NOTE: If you get "missing table [invoice_generation_logs]" error on first run,
 * this means Flyway hasn't run the V1.0.37 migration yet.
 * Solution: Run the app once to let Flyway create the table, then this entity will work.
 */
@Entity
@Table(
    name = "invoice_generation_logs",
    indexes = [
        Index(name = "idx_inv_gen_workspace_period", columnList = "workspace_id,billing_period_year,billing_period_month", unique = true),
        Index(name = "idx_inv_gen_status", columnList = "status"),
        Index(name = "idx_inv_gen_created", columnList = "created_at"),
        Index(name = "idx_inv_gen_invoice", columnList = "invoice_id")
    ]
)
class InvoiceGenerationLog : BaseDomain() {

    /**
     * Workspace this log belongs to
     */
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    /**
     * Subscription ID
     */
    @Column(name = "subscription_id", nullable = false)
    var subscriptionId: Long = 0

    /**
     * Billing period year
     */
    @Column(name = "billing_period_year", nullable = false)
    var billingPeriodYear: Int = 0

    /**
     * Billing period month (1-12)
     */
    @Column(name = "billing_period_month", nullable = false)
    var billingPeriodMonth: Int = 0

    /**
     * Billing period start timestamp
     */
    @Column(name = "billing_period_start", nullable = false)
    var billingPeriodStart: Instant = Instant.now()

    /**
     * Billing period end timestamp
     */
    @Column(name = "billing_period_end", nullable = false)
    var billingPeriodEnd: Instant = Instant.now()

    /**
     * Generation status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: InvoiceGenerationStatus = InvoiceGenerationStatus.PENDING

    /**
     * Generated invoice ID (if successful)
     */
    @Column(name = "invoice_id")
    var invoiceId: Long? = null

    /**
     * Invoice number (if successful)
     */
    @Column(name = "invoice_number", length = 50)
    var invoiceNumber: String? = null

    /**
     * Attempt count
     */
    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0

    /**
     * Last attempt timestamp
     */
    @Column(name = "last_attempt_at")
    var lastAttemptAt: Instant? = null

    /**
     * Success timestamp
     */
    @Column(name = "succeeded_at")
    var succeededAt: Instant? = null

    /**
     * Error message (if failed)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null

    /**
     * Error stack trace (if failed)
     */
    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    var errorStackTrace: String? = null

    /**
     * Payment processing status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    var paymentStatus: PaymentProcessingStatus = PaymentProcessingStatus.NOT_STARTED

    /**
     * Payment link sent timestamp
     */
    @Column(name = "payment_link_sent_at")
    var paymentLinkSentAt: Instant? = null

    /**
     * Payment link error (if failed)
     */
    @Column(name = "payment_link_error", columnDefinition = "TEXT")
    var paymentLinkError: String? = null

    /**
     * Next retry timestamp (for exponential backoff)
     */
    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null

    /**
     * Whether this generation should be retried
     */
    @Column(name = "should_retry", nullable = false)
    var shouldRetry: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return "IGL"
    }

    /**
     * Mark as in progress
     */
    fun markInProgress() {
        status = InvoiceGenerationStatus.IN_PROGRESS
        lastAttemptAt = Instant.now()
        attemptCount++
    }

    /**
     * Mark as succeeded
     */
    fun markSucceeded(invoiceId: Long, invoiceNumber: String) {
        status = InvoiceGenerationStatus.SUCCESS
        this.invoiceId = invoiceId
        this.invoiceNumber = invoiceNumber
        succeededAt = Instant.now()
        errorMessage = null
        errorStackTrace = null
        shouldRetry = false
    }

    /**
     * Mark as failed
     */
    fun markFailed(error: Exception, shouldRetryAgain: Boolean = true) {
        status = InvoiceGenerationStatus.FAILED
        errorMessage = error.message ?: "Unknown error"
        errorStackTrace = error.stackTraceToString()
        shouldRetry = shouldRetryAgain && attemptCount < 5 // Max 5 retries

        if (shouldRetry) {
            // Exponential backoff: 5min, 15min, 1hr, 4hr, 12hr
            val backoffMinutes = when (attemptCount) {
                1 -> 5L
                2 -> 15L
                3 -> 60L
                4 -> 240L
                else -> 720L
            }
            nextRetryAt = Instant.now().plusSeconds(backoffMinutes * 60)
        }
    }

    /**
     * Mark payment processing status
     */
    fun markPaymentProcessing(status: PaymentProcessingStatus, error: String? = null) {
        this.paymentStatus = status
        if (status == PaymentProcessingStatus.LINK_SENT) {
            paymentLinkSentAt = Instant.now()
        }
        if (error != null) {
            paymentLinkError = error
        }
    }

    /**
     * Check if ready for retry
     */
    fun isReadyForRetry(): Boolean {
        return shouldRetry
            && status == InvoiceGenerationStatus.FAILED
            && nextRetryAt != null
            && nextRetryAt!!.isBefore(Instant.now())
    }
}

/**
 * Invoice generation status
 */
enum class InvoiceGenerationStatus {
    PENDING,        // Not yet attempted
    IN_PROGRESS,    // Currently generating
    SUCCESS,        // Successfully generated
    FAILED,         // Failed to generate
    SKIPPED         // Skipped (e.g., already exists)
}

/**
 * Payment processing status for generated invoices
 */
enum class PaymentProcessingStatus {
    NOT_STARTED,      // Payment not yet processed
    AUTO_CHARGING,    // Attempting auto-charge
    AUTO_CHARGE_SUCCESS,  // Auto-charge succeeded
    AUTO_CHARGE_FAILED,   // Auto-charge failed
    LINK_GENERATING,  // Generating payment link
    LINK_SENT,        // Payment link sent
    LINK_FAILED       // Payment link generation failed
}
