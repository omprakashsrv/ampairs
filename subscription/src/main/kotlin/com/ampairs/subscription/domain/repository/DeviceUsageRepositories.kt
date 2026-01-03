package com.ampairs.subscription.domain.repository

import com.ampairs.subscription.domain.model.DevicePlatform
import com.ampairs.subscription.domain.model.DeviceRegistration
import com.ampairs.subscription.domain.model.UsageMetric
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// =====================
// Device Registration Repository
// =====================

@Repository
interface DeviceRegistrationRepository : JpaRepository<DeviceRegistration, Long> {

    fun findByUid(uid: String): DeviceRegistration?

    fun findByDeviceId(deviceId: String): DeviceRegistration?

    fun findByWorkspaceIdAndDeviceId(workspaceId: String, deviceId: String): DeviceRegistration?

    fun findByWorkspaceId(workspaceId: String): List<DeviceRegistration>

    fun findByUserId(userId: String): List<DeviceRegistration>

    @Query("""
        SELECT d FROM DeviceRegistration d
        WHERE d.workspaceId = :workspaceId
        AND d.isActive = true
        ORDER BY d.lastActivityAt DESC
    """)
    fun findActiveByWorkspaceId(workspaceId: String): List<DeviceRegistration>

    @Query("""
        SELECT d FROM DeviceRegistration d
        WHERE d.userId = :userId
        AND d.isActive = true
        ORDER BY d.lastActivityAt DESC
    """)
    fun findActiveByUserId(userId: String): List<DeviceRegistration>

    @Query("""
        SELECT d FROM DeviceRegistration d
        WHERE d.workspaceId = :workspaceId
        AND d.userId = :userId
        AND d.isActive = true
    """)
    fun findActiveByWorkspaceIdAndUserId(workspaceId: String, userId: String): List<DeviceRegistration>

    /**
     * Count active devices for a workspace
     */
    @Query("""
        SELECT COUNT(d)
        FROM DeviceRegistration d
        WHERE d.workspaceId = :workspaceId
        AND d.isActive = true
    """)
    fun countActiveByWorkspaceId(workspaceId: String): Long

    /**
     * Count active devices for a user across all workspaces
     */
    @Query("""
        SELECT COUNT(d)
        FROM DeviceRegistration d
        WHERE d.userId = :userId
        AND d.isActive = true
    """)
    fun countActiveByUserId(userId: String): Long

    /**
     * Find devices with expired tokens (need to sync)
     */
    @Query("""
        SELECT d FROM DeviceRegistration d
        WHERE d.isActive = true
        AND d.tokenExpiresAt < :now
    """)
    fun findWithExpiredTokens(@Param("now") now: Instant): List<DeviceRegistration>

    /**
     * Find inactive devices (not synced in X days)
     */
    @Query("""
        SELECT d FROM DeviceRegistration d
        WHERE d.isActive = true
        AND d.lastSyncAt < :threshold
    """)
    fun findInactiveDevices(@Param("threshold") threshold: Instant): List<DeviceRegistration>

    /**
     * Refresh device token
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE DeviceRegistration d
        SET d.tokenExpiresAt = :expiresAt,
            d.lastSyncAt = :now,
            d.updatedAt = :now
        WHERE d.uid = :uid
    """)
    fun refreshToken(
        @Param("uid") uid: String,
        @Param("expiresAt") expiresAt: Instant,
        @Param("now") now: Instant
    )

    /**
     * Update last activity
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE DeviceRegistration d
        SET d.lastActivityAt = :now,
            d.lastIp = :ip,
            d.updatedAt = :now
        WHERE d.uid = :uid
    """)
    fun updateLastActivity(
        @Param("uid") uid: String,
        @Param("now") now: Instant,
        @Param("ip") ip: String?
    )

    /**
     * Deactivate device
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE DeviceRegistration d
        SET d.isActive = false,
            d.deactivatedAt = :now,
            d.deactivationReason = :reason,
            d.updatedAt = :now
        WHERE d.uid = :uid
    """)
    fun deactivate(
        @Param("uid") uid: String,
        @Param("now") now: Instant,
        @Param("reason") reason: String
    )

    /**
     * Deactivate all devices for a workspace
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE DeviceRegistration d
        SET d.isActive = false,
            d.deactivatedAt = :now,
            d.deactivationReason = :reason,
            d.updatedAt = :now
        WHERE d.workspaceId = :workspaceId
        AND d.isActive = true
    """)
    fun deactivateAllByWorkspaceId(
        @Param("workspaceId") workspaceId: String,
        @Param("now") now: Instant,
        @Param("reason") reason: String
    )

    /**
     * Update push token
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE DeviceRegistration d
        SET d.pushToken = :pushToken,
            d.pushTokenType = :pushTokenType,
            d.updatedAt = :now
        WHERE d.uid = :uid
    """)
    fun updatePushToken(
        @Param("uid") uid: String,
        @Param("pushToken") pushToken: String?,
        @Param("pushTokenType") pushTokenType: String?,
        @Param("now") now: Instant
    )

    fun existsByWorkspaceIdAndDeviceIdAndIsActiveTrue(workspaceId: String, deviceId: String): Boolean
}

// =====================
// Usage Metric Repository
// =====================

@Repository
interface UsageMetricRepository : JpaRepository<UsageMetric, Long> {

    fun findByUid(uid: String): UsageMetric?

    fun findByWorkspaceId(workspaceId: String): List<UsageMetric>

    fun findByWorkspaceIdAndPeriodYearAndPeriodMonth(
        workspaceId: String,
        periodYear: Int,
        periodMonth: Int
    ): UsageMetric?

    @Query("""
        SELECT u FROM UsageMetric u
        WHERE u.workspaceId = :workspaceId
        ORDER BY u.periodYear DESC, u.periodMonth DESC
    """)
    fun findByWorkspaceIdOrderByPeriodDesc(workspaceId: String): List<UsageMetric>

    /**
     * Get usage for the last N months
     */
    @Query("""
        SELECT u FROM UsageMetric u
        WHERE u.workspaceId = :workspaceId
        AND (u.periodYear > :fromYear OR (u.periodYear = :fromYear AND u.periodMonth >= :fromMonth))
        ORDER BY u.periodYear DESC, u.periodMonth DESC
    """)
    fun findRecentByWorkspaceId(
        @Param("workspaceId") workspaceId: String,
        @Param("fromYear") fromYear: Int,
        @Param("fromMonth") fromMonth: Int
    ): List<UsageMetric>

    /**
     * Find workspaces exceeding limits
     */
    @Query("""
        SELECT u FROM UsageMetric u
        WHERE u.periodYear = :year
        AND u.periodMonth = :month
        AND (u.customerLimitExceeded = true
            OR u.productLimitExceeded = true
            OR u.invoiceLimitExceeded = true
            OR u.storageLimitExceeded = true
            OR u.memberLimitExceeded = true
            OR u.deviceLimitExceeded = true)
    """)
    fun findExceedingLimits(
        @Param("year") year: Int,
        @Param("month") month: Int
    ): List<UsageMetric>

    /**
     * Update customer count
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE UsageMetric u
        SET u.customerCount = :count,
            u.lastCalculatedAt = :now,
            u.updatedAt = :now
        WHERE u.workspaceId = :workspaceId
        AND u.periodYear = :year
        AND u.periodMonth = :month
    """)
    fun updateCustomerCount(
        @Param("workspaceId") workspaceId: String,
        @Param("year") year: Int,
        @Param("month") month: Int,
        @Param("count") count: Int,
        @Param("now") now: Instant
    )

    /**
     * Increment invoice count
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE UsageMetric u
        SET u.invoiceCount = u.invoiceCount + 1,
            u.lastCalculatedAt = :now,
            u.updatedAt = :now
        WHERE u.workspaceId = :workspaceId
        AND u.periodYear = :year
        AND u.periodMonth = :month
    """)
    fun incrementInvoiceCount(
        @Param("workspaceId") workspaceId: String,
        @Param("year") year: Int,
        @Param("month") month: Int,
        @Param("now") now: Instant
    )

    /**
     * Update storage usage
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE UsageMetric u
        SET u.storageUsedBytes = :bytes,
            u.lastCalculatedAt = :now,
            u.updatedAt = :now
        WHERE u.workspaceId = :workspaceId
        AND u.periodYear = :year
        AND u.periodMonth = :month
    """)
    fun updateStorageUsage(
        @Param("workspaceId") workspaceId: String,
        @Param("year") year: Int,
        @Param("month") month: Int,
        @Param("bytes") bytes: Long,
        @Param("now") now: Instant
    )

    /**
     * Increment API calls
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE UsageMetric u
        SET u.apiCalls = u.apiCalls + :count,
            u.updatedAt = :now
        WHERE u.workspaceId = :workspaceId
        AND u.periodYear = :year
        AND u.periodMonth = :month
    """)
    fun incrementApiCalls(
        @Param("workspaceId") workspaceId: String,
        @Param("year") year: Int,
        @Param("month") month: Int,
        @Param("count") count: Long,
        @Param("now") now: Instant
    )

    /**
     * Find all usage metrics for a specific period (for monthly reset)
     */
    fun findByPeriodYearAndPeriodMonth(periodYear: Int, periodMonth: Int): List<UsageMetric>

    /**
     * Find old usage metrics for cleanup
     */
    @Query("""
        SELECT u FROM UsageMetric u
        WHERE u.periodYear < :cutoffYear
        OR (u.periodYear = :cutoffYear AND u.periodMonth < :cutoffMonth)
    """)
    fun findByPeriodYearLessThanOrPeriodYearAndPeriodMonthLessThan(
        @Param("cutoffYear") cutoffYear: Int,
        @Param("cutoffYear") sameYear: Int,
        @Param("cutoffMonth") cutoffMonth: Int
    ): List<UsageMetric>
}
