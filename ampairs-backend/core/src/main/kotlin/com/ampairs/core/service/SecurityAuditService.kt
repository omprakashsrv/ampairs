package com.ampairs.core.service

import com.ampairs.core.config.ApplicationProperties
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * Comprehensive security audit logging service
 * Logs all security-related events for monitoring, compliance, and forensics
 */
@Service
class SecurityAuditService(
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
) {

    private val auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT")
    private val authLogger = LoggerFactory.getLogger("AUTH_EVENTS")
    private val rateLimitLogger = LoggerFactory.getLogger("RATE_LIMIT_EVENTS")
    private val accessLogger = LoggerFactory.getLogger("ACCESS_EVENTS")

    /**
     * Log authentication attempt (success or failure)
     */
    fun logAuthenticationAttempt(
        phone: String,
        countryCode: Int,
        success: Boolean,
        reason: String? = null,
        request: HttpServletRequest,
        sessionId: String? = null,
        deviceInfo: Map<String, Any>? = null,
    ) {
        val event = SecurityEvent(
            eventType = if (success) SecurityEventType.AUTHENTICATION_SUCCESS else SecurityEventType.AUTHENTICATION_FAILURE,
            userId = if (success) getCurrentUserId() else null,
            sessionId = sessionId,
            ipAddress = getClientIp(request),
            userAgent = request.getHeader("User-Agent"),
            details = mapOf<String, Any>(
                "phone" to maskPhoneNumber(phone),
                "country_code" to countryCode
            ) + (reason?.let { mapOf<String, Any>("reason" to it) } ?: emptyMap()) +
                    (deviceInfo?.let { mapOf<String, Any>("device_info" to it) } ?: emptyMap())
        )

        logSecurityEvent(event, authLogger)
    }

    /**
     * Log OTP generation and verification events
     */
    fun logOtpEvent(
        phone: String,
        eventType: OtpEventType,
        success: Boolean,
        sessionId: String,
        request: HttpServletRequest,
        reason: String? = null,
    ) {
        val event = SecurityEvent(
            eventType = when (eventType) {
                OtpEventType.GENERATION -> SecurityEventType.OTP_GENERATED
                OtpEventType.VERIFICATION -> if (success) SecurityEventType.OTP_VERIFIED else SecurityEventType.OTP_VERIFICATION_FAILED
                OtpEventType.RESEND -> SecurityEventType.OTP_RESENT
            },
            userId = getCurrentUserId(),
            sessionId = sessionId,
            ipAddress = getClientIp(request),
            userAgent = request.getHeader("User-Agent"),
            details = mapOf<String, Any>(
                "phone" to maskPhoneNumber(phone)
            ) + (reason?.let { mapOf<String, Any>("reason" to it) } ?: emptyMap())
        )

        logSecurityEvent(event, authLogger)
    }

    /**
     * Log rate limit violations
     */
    fun logRateLimitViolation(
        limitType: String,
        endpoint: String,
        request: HttpServletRequest,
        remainingTime: Long,
    ) {
        val event = SecurityEvent(
            eventType = SecurityEventType.RATE_LIMIT_EXCEEDED,
            userId = getCurrentUserId(),
            ipAddress = getClientIp(request),
            userAgent = request.getHeader("User-Agent"),
            details = mapOf(
                "limit_type" to limitType,
                "endpoint" to endpoint,
                "remaining_time_seconds" to remainingTime,
                "method" to request.method
            )
        )

        logSecurityEvent(event, rateLimitLogger)
    }

    /**
     * Log progressive throttling events
     */
    fun logProgressiveThrottling(
        clientIp: String,
        endpoint: String,
        failureCount: Int,
        originalCapacity: Int,
        throttledCapacity: Int,
    ) {
        val event = SecurityEvent(
            eventType = SecurityEventType.PROGRESSIVE_THROTTLING_APPLIED,
            userId = getCurrentUserId(),
            ipAddress = clientIp,
            details = mapOf(
                "endpoint" to endpoint,
                "failure_count" to failureCount,
                "original_capacity" to originalCapacity,
                "throttled_capacity" to throttledCapacity,
                "throttle_factor" to (throttledCapacity.toDouble() / originalCapacity)
            )
        )

        logSecurityEvent(event, rateLimitLogger)
    }

    /**
     * Log JWT token events (generation, refresh, revocation)
     */
    fun logTokenEvent(
        eventType: TokenEventType,
        userId: String,
        deviceId: String? = null,
        request: HttpServletRequest? = null,
        reason: String? = null,
    ) {
        val event = SecurityEvent(
            eventType = when (eventType) {
                TokenEventType.GENERATED -> SecurityEventType.JWT_TOKEN_GENERATED
                TokenEventType.REFRESHED -> SecurityEventType.JWT_TOKEN_REFRESHED
                TokenEventType.REVOKED -> SecurityEventType.JWT_TOKEN_REVOKED
                TokenEventType.EXPIRED -> SecurityEventType.JWT_TOKEN_EXPIRED
                TokenEventType.INVALID -> SecurityEventType.JWT_TOKEN_INVALID
            },
            userId = userId,
            ipAddress = request?.let { getClientIp(it) },
            userAgent = request?.getHeader("User-Agent"),
            details = (deviceId?.let { mapOf<String, Any>("device_id" to it) } ?: emptyMap()) +
                    (reason?.let { mapOf<String, Any>("reason" to it) } ?: emptyMap())
        )

        logSecurityEvent(event, authLogger)
    }

    /**
     * Log device session events
     */
    fun logDeviceSessionEvent(
        eventType: DeviceSessionEventType,
        userId: String,
        deviceId: String,
        deviceName: String? = null,
        request: HttpServletRequest? = null,
    ) {
        val event = SecurityEvent(
            eventType = when (eventType) {
                DeviceSessionEventType.CREATED -> SecurityEventType.DEVICE_SESSION_CREATED
                DeviceSessionEventType.UPDATED -> SecurityEventType.DEVICE_SESSION_UPDATED
                DeviceSessionEventType.DEACTIVATED -> SecurityEventType.DEVICE_SESSION_DEACTIVATED
                DeviceSessionEventType.EXPIRED -> SecurityEventType.DEVICE_SESSION_EXPIRED
            },
            userId = userId,
            ipAddress = request?.let { getClientIp(it) },
            userAgent = request?.getHeader("User-Agent"),
            details = mapOf<String, Any>(
                "device_id" to deviceId
            ) + (deviceName?.let { mapOf<String, Any>("device_name" to it) } ?: emptyMap())
        )

        logSecurityEvent(event, authLogger)
    }

    /**
     * Log authorization failures
     */
    fun logAuthorizationFailure(
        reason: String,
        requiredRole: String? = null,
        endpoint: String,
        request: HttpServletRequest,
    ) {
        val event = SecurityEvent(
            eventType = SecurityEventType.AUTHORIZATION_FAILURE,
            userId = getCurrentUserId(),
            ipAddress = getClientIp(request),
            userAgent = request.getHeader("User-Agent"),
            details = mapOf<String, Any>(
                "endpoint" to endpoint,
                "method" to request.method
            ) + (reason?.let { mapOf<String, Any>("reason" to it) } ?: emptyMap()) +
                    (requiredRole?.let { mapOf<String, Any>("required_role" to it) } ?: emptyMap())
        )

        logSecurityEvent(event, accessLogger)
    }

    /**
     * Log suspicious activity
     */
    fun logSuspiciousActivity(
        activityType: String,
        description: String,
        riskLevel: RiskLevel,
        request: HttpServletRequest?,
        additionalDetails: Map<String, Any> = emptyMap(),
    ) {
        val event = SecurityEvent(
            eventType = SecurityEventType.SUSPICIOUS_ACTIVITY,
            userId = getCurrentUserId(),
            ipAddress = request?.let { getClientIp(it) },
            userAgent = request?.getHeader("User-Agent"),
            riskLevel = riskLevel,
            details = mapOf(
                "activity_type" to activityType,
                "description" to description
            ) + additionalDetails
        )

        logSecurityEvent(event, auditLogger)
    }

    /**
     * Log security configuration changes
     */
    fun logSecurityConfigChange(
        configType: String,
        oldValue: Any?,
        newValue: Any?,
        changedBy: String,
        request: HttpServletRequest? = null,
    ) {
        val event = SecurityEvent(
            eventType = SecurityEventType.SECURITY_CONFIG_CHANGED,
            userId = changedBy,
            ipAddress = request?.let { getClientIp(it) },
            userAgent = request?.getHeader("User-Agent"),
            details = mapOf<String, Any>(
                "config_type" to configType
            ) + (oldValue?.let { mapOf<String, Any>("old_value" to it) } ?: emptyMap()) +
                    (newValue?.let { mapOf<String, Any>("new_value" to it) } ?: emptyMap())
        )

        logSecurityEvent(event, auditLogger)
    }

    /**
     * Log password/security credential events
     */
    fun logCredentialEvent(
        eventType: CredentialEventType,
        userId: String,
        credentialType: String,
        request: HttpServletRequest? = null,
    ) {
        val event = SecurityEvent(
            eventType = when (eventType) {
                CredentialEventType.CHANGED -> SecurityEventType.CREDENTIAL_CHANGED
                CredentialEventType.RESET -> SecurityEventType.CREDENTIAL_RESET
                CredentialEventType.COMPROMISED -> SecurityEventType.CREDENTIAL_COMPROMISED
            },
            userId = userId,
            ipAddress = request?.let { getClientIp(it) },
            userAgent = request?.getHeader("User-Agent"),
            details = mapOf(
                "credential_type" to credentialType
            )
        )

        logSecurityEvent(event, auditLogger)
    }

    /**
     * Core method to log security events with consistent formatting
     */
    private fun logSecurityEvent(event: SecurityEvent, logger: org.slf4j.Logger) {
        try {
            // Add trace ID if available
            val traceId = MDC.get("traceId") ?: generateTraceId()

            val logEntry = SecurityLogEntry(
                timestamp = LocalDateTime.now(),
                traceId = traceId,
                event = event
            )

            // Log as structured JSON
            val jsonLog = objectMapper.writeValueAsString(logEntry)

            // Use appropriate log level based on event type and risk level
            when (event.riskLevel) {
                RiskLevel.CRITICAL -> logger.error(jsonLog)
                RiskLevel.HIGH -> logger.warn(jsonLog)
                RiskLevel.MEDIUM -> logger.info(jsonLog)
                RiskLevel.LOW -> logger.debug(jsonLog)
            }

            // Also log human-readable format for debugging
            logger.debug(
                "Security Event: {} - User: {} - IP: {} - Details: {}",
                event.eventType,
                event.userId ?: "anonymous",
                event.ipAddress ?: "unknown",
                event.details
            )

        } catch (e: Exception) {
            // Never fail the main operation due to logging issues
            LoggerFactory.getLogger(SecurityAuditService::class.java)
                .error("Failed to log security event: ${event.eventType}", e)
        }
    }

    /**
     * Extract client IP address from request
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
            null
        }
    }

    /**
     * Mask phone number for privacy (keep first 2 and last 2 digits)
     */
    private fun maskPhoneNumber(phone: String): String {
        return if (phone.length > 4) {
            "${phone.take(2)}${"*".repeat(phone.length - 4)}${phone.takeLast(2)}"
        } else {
            "*".repeat(phone.length)
        }
    }

    /**
     * Generate trace ID for correlation
     */
    private fun generateTraceId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }

    // Data classes for structured logging
    data class SecurityLogEntry(
        val timestamp: LocalDateTime,
        val traceId: String,
        val event: SecurityEvent,
    )

    data class SecurityEvent(
        val eventType: SecurityEventType,
        val userId: String? = null,
        val sessionId: String? = null,
        val ipAddress: String? = null,
        val userAgent: String? = null,
        val riskLevel: RiskLevel = RiskLevel.MEDIUM,
        val details: Map<String, Any> = emptyMap(),
    )

    // Enums for event classification
    enum class SecurityEventType {
        // Authentication Events
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        OTP_GENERATED,
        OTP_VERIFIED,
        OTP_VERIFICATION_FAILED,
        OTP_RESENT,

        // Token Events
        JWT_TOKEN_GENERATED,
        JWT_TOKEN_REFRESHED,
        JWT_TOKEN_REVOKED,
        JWT_TOKEN_EXPIRED,
        JWT_TOKEN_INVALID,

        // Device Session Events
        DEVICE_SESSION_CREATED,
        DEVICE_SESSION_UPDATED,
        DEVICE_SESSION_DEACTIVATED,
        DEVICE_SESSION_EXPIRED,

        // Rate Limiting Events
        RATE_LIMIT_EXCEEDED,
        PROGRESSIVE_THROTTLING_APPLIED,

        // Authorization Events
        AUTHORIZATION_FAILURE,

        // Security Events
        SUSPICIOUS_ACTIVITY,
        SECURITY_CONFIG_CHANGED,
        CREDENTIAL_CHANGED,
        CREDENTIAL_RESET,
        CREDENTIAL_COMPROMISED
    }

    enum class RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    enum class OtpEventType {
        GENERATION, VERIFICATION, RESEND
    }

    enum class TokenEventType {
        GENERATED, REFRESHED, REVOKED, EXPIRED, INVALID
    }

    enum class DeviceSessionEventType {
        CREATED, UPDATED, DEACTIVATED, EXPIRED
    }

    enum class CredentialEventType {
        CHANGED, RESET, COMPROMISED
    }
}