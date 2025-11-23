package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Instant
import java.time.YearMonth

/**
 * Usage metrics for a workspace.
 * Tracks resource usage for limit enforcement and analytics.
 */
@Entity
@Table(
    name = "usage_metrics",
    indexes = [
        Index(name = "idx_usage_uid", columnList = "uid", unique = true),
        Index(name = "idx_usage_workspace", columnList = "workspace_id"),
        Index(name = "idx_usage_period", columnList = "period_year, period_month"),
        Index(name = "idx_usage_workspace_period", columnList = "workspace_id, period_year, period_month")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_usage_workspace_period",
            columnNames = ["workspace_id", "period_year", "period_month"]
        )
    ]
)
class UsageMetric : BaseDomain() {

    /**
     * Workspace this usage belongs to
     */
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    /**
     * Period year
     */
    @Column(name = "period_year", nullable = false)
    var periodYear: Int = YearMonth.now().year

    /**
     * Period month (1-12)
     */
    @Column(name = "period_month", nullable = false)
    var periodMonth: Int = YearMonth.now().monthValue

    // Resource counts

    /**
     * Total customers count
     */
    @Column(name = "customer_count", nullable = false)
    var customerCount: Int = 0

    /**
     * Total products count
     */
    @Column(name = "product_count", nullable = false)
    var productCount: Int = 0

    /**
     * Invoices created this month
     */
    @Column(name = "invoice_count", nullable = false)
    var invoiceCount: Int = 0

    /**
     * Orders created this month
     */
    @Column(name = "order_count", nullable = false)
    var orderCount: Int = 0

    /**
     * Active members count
     */
    @Column(name = "member_count", nullable = false)
    var memberCount: Int = 0

    /**
     * Active devices count
     */
    @Column(name = "device_count", nullable = false)
    var deviceCount: Int = 0

    /**
     * Storage used in bytes
     */
    @Column(name = "storage_used_bytes", nullable = false)
    var storageUsedBytes: Long = 0

    // API usage

    /**
     * API calls this month
     */
    @Column(name = "api_calls", nullable = false)
    var apiCalls: Long = 0

    // Notification usage

    /**
     * SMS sent this month
     */
    @Column(name = "sms_count", nullable = false)
    var smsCount: Int = 0

    /**
     * Emails sent this month
     */
    @Column(name = "email_count", nullable = false)
    var emailCount: Int = 0

    // Limit tracking

    /**
     * Whether customer limit was exceeded
     */
    @Column(name = "customer_limit_exceeded", nullable = false)
    var customerLimitExceeded: Boolean = false

    /**
     * Whether product limit was exceeded
     */
    @Column(name = "product_limit_exceeded", nullable = false)
    var productLimitExceeded: Boolean = false

    /**
     * Whether invoice limit was exceeded
     */
    @Column(name = "invoice_limit_exceeded", nullable = false)
    var invoiceLimitExceeded: Boolean = false

    /**
     * Whether storage limit was exceeded
     */
    @Column(name = "storage_limit_exceeded", nullable = false)
    var storageLimitExceeded: Boolean = false

    /**
     * Whether member limit was exceeded
     */
    @Column(name = "member_limit_exceeded", nullable = false)
    var memberLimitExceeded: Boolean = false

    /**
     * Whether device limit was exceeded
     */
    @Column(name = "device_limit_exceeded", nullable = false)
    var deviceLimitExceeded: Boolean = false

    /**
     * Last updated timestamp for this metric
     */
    @Column(name = "last_calculated_at")
    var lastCalculatedAt: Instant? = null

    override fun obtainSeqIdPrefix(): String {
        return "USAGE"
    }

    /**
     * Get storage used in GB
     */
    fun getStorageUsedGb(): Double {
        return storageUsedBytes / (1024.0 * 1024.0 * 1024.0)
    }

    /**
     * Check if any limit is exceeded
     */
    fun hasExceededLimits(): Boolean {
        return customerLimitExceeded ||
                productLimitExceeded ||
                invoiceLimitExceeded ||
                storageLimitExceeded ||
                memberLimitExceeded ||
                deviceLimitExceeded
    }

    /**
     * Get period as YearMonth
     */
    fun getPeriod(): YearMonth {
        return YearMonth.of(periodYear, periodMonth)
    }

    companion object {
        /**
         * Create a new usage metric for current period
         */
        fun forCurrentPeriod(workspaceId: String): UsageMetric {
            val now = YearMonth.now()
            return UsageMetric().apply {
                this.workspaceId = workspaceId
                this.periodYear = now.year
                this.periodMonth = now.monthValue
                this.lastCalculatedAt = Instant.now()
            }
        }
    }
}
