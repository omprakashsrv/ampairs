package com.ampairs.auth.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.service.RateLimitingService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for rate limiting management and monitoring
 * Only accessible by authenticated users for their own rate limit status
 */
@RestController
@RequestMapping("/auth/v1/rate-limit")
class RateLimitController @Autowired constructor(
    private val rateLimitingService: RateLimitingService,
) {

    private val logger = LoggerFactory.getLogger(RateLimitController::class.java)

    /**
     * Get current rate limit status for the authenticated user
     */
    @GetMapping("/status")
    fun getRateLimitStatus(request: HttpServletRequest): ApiResponse<Map<String, RateLimitingService.RateLimitStatus>> {
        val clientIp = getClientIp(request)
        val userId = getCurrentUserId()

        logger.debug("Rate limit status requested by user: {}, IP: {}", userId, clientIp)

        val status = rateLimitingService.getRateLimitStatus(clientIp, userId)

        return ApiResponse.success(status)
    }

    /**
     * Get simplified rate limit status for client-side display
     */
    @GetMapping("/status/summary")
    fun getRateLimitSummary(request: HttpServletRequest): ApiResponse<RateLimitSummary> {
        val clientIp = getClientIp(request)
        val userId = getCurrentUserId()

        val status = rateLimitingService.getRateLimitStatus(clientIp, userId)

        // Find the most restrictive limit
        val mostRestrictive = status.values
            .filter { it.isBlocked || it.remainingRequests < 5 }
            .minByOrNull { it.remainingRequests }

        val summary = RateLimitSummary(
            isLimited = mostRestrictive?.isBlocked ?: false,
            remainingRequests = mostRestrictive?.remainingRequests ?: Int.MAX_VALUE,
            resetTime = mostRestrictive?.resetTime,
            limitType = mostRestrictive?.key?.substringAfter(":")?.substringBefore(":") ?: "none",
            message = when {
                mostRestrictive?.isBlocked == true -> "Rate limit exceeded. Please try again later."
                mostRestrictive?.remainingRequests ?: Int.MAX_VALUE < 5 -> "Approaching rate limit. Please slow down."
                else -> "Normal operation"
            }
        )

        return ApiResponse.success(summary)
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
     * Simplified rate limit summary for client applications
     */
    data class RateLimitSummary(
        val isLimited: Boolean,
        val remainingRequests: Int,
        val resetTime: java.time.LocalDateTime?,
        val limitType: String,
        val message: String,
    )
}