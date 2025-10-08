package com.ampairs.auth.service

import com.ampairs.core.config.ApplicationProperties
import com.ampairs.core.service.SecurityAuditService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Soft Account Lockout Service
 * Designed for business applications - not as strict as banking apps
 * Provides progressive lockout with short recovery times
 */
@Service
class AccountLockoutService(
    private val applicationProperties: ApplicationProperties,
    private val securityAuditService: SecurityAuditService,
) {

    private val logger = LoggerFactory.getLogger(AccountLockoutService::class.java)

    // Cache for lockout records - stores by phone number
    private val lockoutCache: ConcurrentHashMap<String, LockoutRecord> = ConcurrentHashMap()

    /**
     * Check if account is currently locked
     */
    fun isAccountLocked(phone: String, countryCode: Int): Boolean {
        if (!applicationProperties.security.accountLockout.enabled) {
            return false
        }

        val key = getUserKey(phone, countryCode)
        val record = lockoutCache[key] ?: return false

        // Check if lockout has expired
        if (record.lockedUntil != null && LocalDateTime.now().isAfter(record.lockedUntil)) {
            // Lockout expired, remove from cache
            lockoutCache.remove(key)
            logger.info("Lockout expired for phone: {}", maskPhoneNumber(phone))

            return false
        }

        return record.lockedUntil != null && LocalDateTime.now().isBefore(record.lockedUntil)
    }

    /**
     * Get lockout status information
     */
    fun getLockoutStatus(phone: String, countryCode: Int): LockoutStatus {
        if (!applicationProperties.security.accountLockout.enabled) {
            return LockoutStatus(false, null, 0)
        }

        val key = getUserKey(phone, countryCode)
        val record = lockoutCache[key]

        if (record == null) {
            return LockoutStatus(false, null, 0)
        }

        val isLocked = record.lockedUntil != null && LocalDateTime.now().isBefore(record.lockedUntil)
        return LockoutStatus(
            isLocked = isLocked,
            lockedUntil = record.lockedUntil,
            failedAttempts = record.failureCount.get()
        )
    }

    /**
     * Record authentication failure - may trigger lockout
     */
    fun recordAuthenticationFailure(
        phone: String,
        countryCode: Int,
        reason: String,
        request: HttpServletRequest,
    ) {
        if (!applicationProperties.security.accountLockout.enabled) {
            return
        }

        val key = getUserKey(phone, countryCode)
        val config = applicationProperties.security.accountLockout
        val now = LocalDateTime.now()

        val record = lockoutCache.computeIfAbsent(key) { LockoutRecord() }

        // Clean up old failures outside the window
        record.cleanupOldFailures(config.failureWindow)

        // Add new failure
        record.addFailure(now, reason)

        logger.warn(
            "Authentication failure recorded: phone={}, reason={}, total_failures={}",
            maskPhoneNumber(phone), reason, record.failureCount.get()
        )

        // Check if should trigger lockout
        if (record.failureCount.get() >= config.maxFailedAttempts && record.lockedUntil == null) {
            triggerLockout(key, phone, record, request)
        }
    }

    /**
     * Record successful authentication - clears failure count
     */
    fun recordAuthenticationSuccess(phone: String, countryCode: Int) {
        if (!applicationProperties.security.accountLockout.enabled) {
            return
        }

        val key = getUserKey(phone, countryCode)
        val record = lockoutCache[key]

        if (record != null && record.failureCount.get() > 0) {
            logger.info(
                "Clearing {} failed attempts for successful login: phone={}",
                record.failureCount.get(), maskPhoneNumber(phone)
            )

            // Clear failures but keep lockout history for progressive lockout
            record.clearCurrentFailures()
        }
    }

    /**
     * Manually unlock account (admin function)
     */
    fun unlockAccount(phone: String, countryCode: Int, unlockedBy: String, request: HttpServletRequest): Boolean {
        val key = getUserKey(phone, countryCode)
        val record = lockoutCache[key]

        if (record != null && record.lockedUntil != null) {
            record.unlock()

            logger.info(
                "Account manually unlocked: phone={}, unlocked_by={}",
                maskPhoneNumber(phone), unlockedBy
            )

            return true
        }

        return false
    }

    /**
     * Get lockout statistics for monitoring
     */
    fun getLockoutStatistics(): Map<String, Any> {
        val currentTime = LocalDateTime.now()
        val allRecords = lockoutCache.values

        val currentlyLocked = allRecords.count { record ->
            record.lockedUntil != null && currentTime.isBefore(record.lockedUntil)
        }

        val totalWithFailures = allRecords.count { it.failureCount.get() > 0 }

        val recentLockouts = allRecords.count { record ->
            record.lastLockoutTime != null &&
                    Duration.between(record.lastLockoutTime, currentTime).toHours() < 24
        }

        return mapOf(
            "currently_locked_accounts" to currentlyLocked,
            "accounts_with_failures" to totalWithFailures,
            "recent_lockouts_24h" to recentLockouts,
            "cache_size" to lockoutCache.size,
            "lockout_enabled" to applicationProperties.security.accountLockout.enabled
        )
    }

    /**
     * Trigger account lockout with progressive duration
     */
    private fun triggerLockout(
        key: String,
        phone: String,
        record: LockoutRecord,
        request: HttpServletRequest,
    ) {
        val config = applicationProperties.security.accountLockout
        val now = LocalDateTime.now()

        // Calculate lockout duration with progressive increase
        var lockoutDuration = config.lockoutDuration

        if (config.progressiveLockout.enabled && record.lockoutHistory.isNotEmpty()) {
            // Count recent lockouts within reset period
            val recentLockouts = record.lockoutHistory.count { lockoutTime ->
                Duration.between(lockoutTime, now) <= config.progressiveLockout.resetPeriod
            }

            if (recentLockouts > 0) {
                // Apply progressive multiplier
                val multiplier = Math.pow(config.progressiveLockout.durationMultiplier, recentLockouts.toDouble())
                lockoutDuration = Duration.ofMinutes((lockoutDuration.toMinutes() * multiplier).toLong())

                // Cap at maximum duration
                if (lockoutDuration > config.progressiveLockout.maxDuration) {
                    lockoutDuration = config.progressiveLockout.maxDuration
                }
            }
        }

        record.lockAccount(now, lockoutDuration)

        val lockoutMinutes = lockoutDuration.toMinutes()

        logger.warn(
            "Account locked: phone={}, duration_minutes={}, failed_attempts={}, progressive_lockout={}",
            maskPhoneNumber(phone), lockoutMinutes, record.failureCount.get(),
            lockoutDuration > config.lockoutDuration
        )

        // Log lockout event
        securityAuditService.logSuspiciousActivity(
            activityType = "ACCOUNT_LOCKED",
            description = "Account locked due to repeated authentication failures",
            riskLevel = SecurityAuditService.RiskLevel.HIGH,
            request = request,
            additionalDetails = mapOf(
                "phone" to maskPhoneNumber(phone),
                "failed_attempts" to record.failureCount.get(),
                "lockout_duration_minutes" to lockoutMinutes,
                "progressive_lockout" to (lockoutDuration > config.lockoutDuration),
                "lockout_history_count" to record.lockoutHistory.size
            )
        )
    }

    /**
     * Scheduled cleanup of expired lockout records
     */
    @Scheduled(cron = "\${application.security.account-lockout.cleanup.cron:0 */10 * * * ?}")
    fun cleanupExpiredLockouts() {
        val config = applicationProperties.security.accountLockout.cleanup

        if (!config.enabled) {
            logger.debug("Lockout cleanup is disabled")
            return
        }

        val now = LocalDateTime.now()
        var removedCount = 0

        // Get expired entries
        val expiredKeys = lockoutCache.entries
            .filter { (_, record) ->
                // Remove if lockout expired and no recent activity
                (record.lockedUntil == null || now.isAfter(record.lockedUntil)) &&
                        record.failureCount.get() == 0 &&
                        (record.lastFailureTime == null ||
                                Duration.between(record.lastFailureTime, now).toHours() > 2)
            }
            .take(config.batchSize)
            .map { it.key }

        expiredKeys.forEach { key ->
            lockoutCache.remove(key)
            removedCount++
        }

        if (removedCount > 0) {
            logger.info("Lockout cleanup completed: {} expired records removed", removedCount)
        } else {
            logger.debug("Lockout cleanup completed: no expired records found")
        }
    }

    /**
     * Create user key for cache
     */
    private fun getUserKey(phone: String, countryCode: Int): String {
        return "$countryCode:$phone"
    }

    /**
     * Mask phone number for privacy
     */
    private fun maskPhoneNumber(phone: String): String {
        return if (phone.length > 4) {
            "${phone.take(2)}${"*".repeat(phone.length - 4)}${phone.takeLast(2)}"
        } else {
            "*".repeat(phone.length)
        }
    }

    /**
     * Lockout record to track failures and lockout state
     */
    private data class LockoutRecord(
        val failureCount: AtomicInteger = AtomicInteger(0),
        val failures: MutableList<FailureRecord> = mutableListOf(),
        val lockoutHistory: MutableList<LocalDateTime> = mutableListOf(),
        var lockedUntil: LocalDateTime? = null,
        var lastLockoutTime: LocalDateTime? = null,
        var lastFailureTime: LocalDateTime? = null,
        var lockoutDurationMinutes: Long = 0,
    ) {

        fun addFailure(timestamp: LocalDateTime, reason: String) {
            failures.add(FailureRecord(timestamp, reason))
            failureCount.incrementAndGet()
            lastFailureTime = timestamp
        }

        fun cleanupOldFailures(window: Duration) {
            val cutoff = LocalDateTime.now().minus(window)
            val oldCount = failures.size
            failures.removeIf { it.timestamp.isBefore(cutoff) }
            val newCount = failures.size
            failureCount.set(newCount)

            if (oldCount != newCount) {
                // Update last failure time if failures were removed
                lastFailureTime = failures.maxOfOrNull { it.timestamp }
            }
        }

        fun clearCurrentFailures() {
            failures.clear()
            failureCount.set(0)
            lastFailureTime = null
        }

        fun lockAccount(timestamp: LocalDateTime, duration: Duration) {
            lockedUntil = timestamp.plus(duration)
            lastLockoutTime = timestamp
            lockoutDurationMinutes = duration.toMinutes()
            lockoutHistory.add(timestamp)
        }

        fun unlock() {
            lockedUntil = null
            lockoutDurationMinutes = 0
        }
    }

    /**
     * Individual failure record
     */
    private data class FailureRecord(
        val timestamp: LocalDateTime,
        val reason: String,
    )

    /**
     * Lockout status information
     */
    data class LockoutStatus(
        val isLocked: Boolean,
        val lockedUntil: LocalDateTime?,
        val failedAttempts: Int,
    )
}