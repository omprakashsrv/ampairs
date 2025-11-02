package com.ampairs.core.service

import com.ampairs.core.config.ApplicationProperties
import com.ampairs.core.exception.RateLimitExceededException
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Comprehensive rate limiting service with support for:
 * - IP-based rate limiting
 * - User-based rate limiting
 * - Endpoint-specific rate limiting
 * - Progressive throttling
 * - Burst capacity handling
 */
@Service
class RateLimitingService(
    private val applicationProperties: ApplicationProperties,
    private val securityAuditService: SecurityAuditService,
) {

    private val logger = LoggerFactory.getLogger(RateLimitingService::class.java)

    // Rate limiting buckets with configurable expiration
    private val rateLimitBuckets: Cache<String, RateLimitBucket> = Caffeine.newBuilder()
        .maximumSize(applicationProperties.cache.rateLimitCache.maximumSize)
        .expireAfterAccess(applicationProperties.cache.rateLimitCache.expireAfterAccess)
        .build()

    // Progressive throttling tracking
    private val failureTracker: Cache<String, FailureTracker> = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofHours(1))
        .build()

    /**
     * Check rate limit for authentication endpoints
     */
    fun checkAuthRateLimit(endpoint: String, request: HttpServletRequest) {
        if (!applicationProperties.security.rateLimiting.enabled) {
            logger.debug("Rate limiting disabled, skipping check")
            return
        }

        val clientIp = getClientIp(request)
        val authConfig = applicationProperties.security.rateLimiting.auth

        val rateLimitConfig = when (endpoint) {
            "init" -> authConfig.init
            "verify" -> authConfig.verify
            "refresh_token" -> authConfig.refreshToken
            "logout", "logout/all" -> authConfig.logout
            "devices" -> authConfig.deviceManagement
            else -> authConfig.verify // Default to verify limits
        }

        // Apply progressive throttling for repeated failures
        val progressiveConfig = applyProgressiveThrottling(clientIp, rateLimitConfig, endpoint)

        // Check IP-based rate limit
        checkRateLimit("auth:$endpoint:ip:$clientIp", progressiveConfig, "IP-based authentication", request)

        // Check user-based rate limit if authenticated
        val userId = getCurrentUserId()
        if (userId != null) {
            checkRateLimit("auth:$endpoint:user:$userId", progressiveConfig, "User-based authentication", request)
        }

        logger.debug("Rate limit check passed for endpoint: {}, IP: {}, User: {}", endpoint, clientIp, userId)
    }

    /**
     * Check rate limit for API endpoints
     */
    fun checkApiRateLimit(request: HttpServletRequest, isAuthenticated: Boolean = false) {
        if (!applicationProperties.security.rateLimiting.enabled) {
            return
        }

        val clientIp = getClientIp(request)
        val rateLimitConfig = if (isAuthenticated) {
            applicationProperties.security.rateLimiting.api.authenticated
        } else {
            applicationProperties.security.rateLimiting.api.public
        }

        // Check IP-based rate limit
        checkRateLimit("api:ip:$clientIp", rateLimitConfig, "API IP-based", request)

        // Check user-based rate limit if authenticated
        if (isAuthenticated) {
            val userId = getCurrentUserId()
            if (userId != null) {
                checkRateLimit("api:user:$userId", rateLimitConfig, "API user-based", request)
            }
        }

        // Global rate limits
        val globalConfig = applicationProperties.security.rateLimiting.global
        checkRateLimit("global:ip:$clientIp", globalConfig.perIp, "Global IP-based", request)

        val userId = getCurrentUserId()
        if (userId != null) {
            checkRateLimit("global:user:$userId", globalConfig.perUser, "Global user-based", request)
        }
    }

    /**
     * Record authentication failure for progressive throttling
     */
    fun recordAuthFailure(clientIp: String, endpoint: String, reason: String) {
        val key = "failure:$endpoint:$clientIp"
        val tracker = failureTracker.get(key) { FailureTracker() }

        tracker.recordFailure(reason)
        failureTracker.put(key, tracker)

        logger.warn(
            "Authentication failure recorded: IP={}, endpoint={}, reason={}, total_failures={}",
            clientIp, endpoint, reason, tracker.getFailureCount()
        )

        // Log security event for monitoring
        // Note: We don't have the HttpServletRequest here, so we create a minimal audit log
        logger.info(
            "SECURITY_AUDIT: AUTH_FAILURE - IP={}, endpoint={}, reason={}, failures={}",
            clientIp, endpoint, reason, tracker.getFailureCount()
        )
    }

    /**
     * Record successful authentication to reset progressive throttling
     */
    fun recordAuthSuccess(clientIp: String, endpoint: String) {
        val key = "failure:$endpoint:$clientIp"
        failureTracker.invalidate(key)

        logger.debug("Authentication success recorded, failure tracker reset: IP={}, endpoint={}", clientIp, endpoint)
    }

    /**
     * Get current rate limit status for monitoring
     */
    fun getRateLimitStatus(clientIp: String, userId: String?): Map<String, RateLimitStatus> {
        val status = mutableMapOf<String, RateLimitStatus>()

        // IP-based status
        listOf("auth:init:ip:$clientIp", "auth:verify:ip:$clientIp", "api:ip:$clientIp", "global:ip:$clientIp")
            .forEach { key ->
                val bucket = rateLimitBuckets.getIfPresent(key)
                if (bucket != null) {
                    status[key] = RateLimitStatus(
                        key = key,
                        remainingRequests = maxOf(0, bucket.capacity - bucket.currentCount.get()),
                        resetTime = bucket.windowStart.plusMinutes(bucket.windowMinutes.toLong()),
                        isBlocked = bucket.isBlocked()
                    )
                }
            }

        // User-based status
        if (userId != null) {
            listOf("auth:init:user:$userId", "auth:verify:user:$userId", "api:user:$userId", "global:user:$userId")
                .forEach { key ->
                    val bucket = rateLimitBuckets.getIfPresent(key)
                    if (bucket != null) {
                        status[key] = RateLimitStatus(
                            key = key,
                            remainingRequests = maxOf(0, bucket.capacity - bucket.currentCount.get()),
                            resetTime = bucket.windowStart.plusMinutes(bucket.windowMinutes.toLong()),
                            isBlocked = bucket.isBlocked()
                        )
                    }
                }
        }

        return status
    }

    /**
     * Apply progressive throttling based on failure history
     */
    private fun applyProgressiveThrottling(
        clientIp: String,
        baseConfig: ApplicationProperties.SecurityProperties.RateLimitingProperties.RateLimitConfig,
        endpoint: String,
    ): ApplicationProperties.SecurityProperties.RateLimitingProperties.RateLimitConfig {
        val key = "failure:$endpoint:$clientIp"
        val tracker = failureTracker.getIfPresent(key)

        if (tracker == null || tracker.getFailureCount() == 0) {
            return baseConfig
        }

        // Progressive throttling: reduce capacity based on failure count
        val failureCount = tracker.getFailureCount()
        val throttlingFactor = when {
            failureCount >= 10 -> 0.1 // 90% reduction
            failureCount >= 5 -> 0.3  // 70% reduction
            failureCount >= 3 -> 0.5  // 50% reduction
            else -> 0.8               // 20% reduction
        }

        val throttledCapacity = maxOf(1, (baseConfig.capacity * throttlingFactor).toInt())
        val throttledBurstCapacity = maxOf(1, (baseConfig.burstCapacity * throttlingFactor).toInt())

        logger.info(
            "Applying progressive throttling: IP={}, endpoint={}, failures={}, " +
                    "original_capacity={}, throttled_capacity={}",
            clientIp, endpoint, failureCount, baseConfig.capacity, throttledCapacity
        )

        // Log progressive throttling event for security monitoring
        securityAuditService.logProgressiveThrottling(
            clientIp, endpoint, failureCount, baseConfig.capacity, throttledCapacity
        )

        return ApplicationProperties.SecurityProperties.RateLimitingProperties.RateLimitConfig(
            capacity = throttledCapacity,
            windowMinutes = baseConfig.windowMinutes * 2, // Also increase window
            burstCapacity = throttledBurstCapacity
        )
    }

    /**
     * Core rate limiting check
     */
    private fun checkRateLimit(
        key: String,
        config: ApplicationProperties.SecurityProperties.RateLimitingProperties.RateLimitConfig,
        limitType: String,
        request: HttpServletRequest? = null,
    ) {
        val bucket = rateLimitBuckets.get(key) {
            RateLimitBucket(config.capacity, config.windowMinutes, config.burstCapacity)
        }

        if (!bucket.allowRequest()) {
            val resetTime = bucket.windowStart.plusMinutes(config.windowMinutes.toLong())
            val remainingSeconds = Duration.between(LocalDateTime.now(), resetTime).seconds
            
            logger.warn(
                "Rate limit exceeded: key={}, type={}, remaining_time={}",
                key, limitType, remainingSeconds
            )

            // Log rate limit violation for security monitoring
            if (request != null) {
                securityAuditService.logRateLimitViolation(
                    limitType = limitType,
                    endpoint = request.requestURI,
                    request = request,
                    remainingTime = remainingSeconds
                )
            }

            throw RateLimitExceededException(
                message = "$limitType rate limit exceeded",
                retryAfterSeconds = remainingSeconds,
                limitType = limitType,
                resetTime = resetTime
            )
        }

        logger.debug(
            "Rate limit check passed: key={}, type={}, remaining={}/{}",
            key, limitType, bucket.capacity - bucket.currentCount.get(), bucket.capacity
        )
    }

    /**
     * Extract client IP from request
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "unknown"
    }

    /**
     * Get current authenticated user ID
     */
    private fun getCurrentUserId(): String? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication?.isAuthenticated == true && authentication.name != "anonymousUser") {
                authentication.name
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("Error getting current user ID: {}", e.message)
            null
        }
    }

    /**
     * Rate limiting bucket with sliding window
     */
    private class RateLimitBucket(
        val capacity: Int,
        val windowMinutes: Int,
        val burstCapacity: Int,
    ) {
        var windowStart: LocalDateTime = LocalDateTime.now()
        val currentCount = AtomicInteger(0)
        val burstCount = AtomicInteger(0)

        fun allowRequest(): Boolean {
            val now = LocalDateTime.now()

            // Reset window if expired
            if (now.isAfter(windowStart.plusMinutes(windowMinutes.toLong()))) {
                synchronized(this) {
                    if (now.isAfter(windowStart.plusMinutes(windowMinutes.toLong()))) {
                        windowStart = now
                        currentCount.set(0)
                        burstCount.set(0)
                    }
                }
            }

            // Check burst capacity first
            val currentBurst = burstCount.get()
            if (currentBurst < burstCapacity) {
                burstCount.incrementAndGet()
                currentCount.incrementAndGet()
                return true
            }

            // Check regular capacity
            val current = currentCount.get()
            if (current < capacity) {
                currentCount.incrementAndGet()
                return true
            }

            return false
        }

        fun isBlocked(): Boolean {
            return currentCount.get() >= capacity
        }
    }

    /**
     * Tracks authentication failures for progressive throttling
     */
    private class FailureTracker {
        private val failures = ConcurrentHashMap<String, AtomicInteger>()
        private val lastFailureTime = AtomicLong(System.currentTimeMillis())

        fun recordFailure(reason: String) {
            failures.computeIfAbsent(reason) { AtomicInteger(0) }.incrementAndGet()
            lastFailureTime.set(System.currentTimeMillis())
        }

        fun getFailureCount(): Int {
            return failures.values.sumOf { it.get() }
        }

        fun getFailuresByReason(): Map<String, Int> {
            return failures.mapValues { it.value.get() }
        }
    }

    /**
     * Rate limit status for monitoring
     */
    data class RateLimitStatus(
        val key: String,
        val remainingRequests: Int,
        val resetTime: LocalDateTime,
        val isBlocked: Boolean,
    )
}