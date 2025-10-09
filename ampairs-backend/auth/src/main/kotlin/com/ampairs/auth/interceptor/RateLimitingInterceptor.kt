package com.ampairs.auth.interceptor

import com.ampairs.core.service.RateLimitingService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Interceptor to apply rate limiting to authentication endpoints
 */
@Component
class RateLimitingInterceptor(
    private val rateLimitingService: RateLimitingService,
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(RateLimitingInterceptor::class.java)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val requestUri = request.requestURI
        val method = request.method

        logger.debug("Rate limiting check for: {} {}", method, requestUri)

        try {
            when {
                // Authentication endpoints
                requestUri.startsWith("/auth/v1/") && method == "POST" -> {
                    val endpoint = extractAuthEndpoint(requestUri)
                    logger.debug("Applying auth rate limiting for endpoint: {}", endpoint)
                    rateLimitingService.checkAuthRateLimit(endpoint, request)
                }

                // API endpoints (general rate limiting)
                requestUri.startsWith("/api/") -> {
                    val isAuthenticated = isUserAuthenticated()
                    logger.debug("Applying API rate limiting, authenticated: {}", isAuthenticated)
                    rateLimitingService.checkApiRateLimit(request, isAuthenticated)
                }

                // User management endpoints
                requestUri.startsWith("/user/v1/") -> {
                    val isAuthenticated = isUserAuthenticated()
                    logger.debug("Applying user API rate limiting, authenticated: {}", isAuthenticated)
                    rateLimitingService.checkApiRateLimit(request, isAuthenticated)
                }

                // Device management endpoints (part of auth but with different limits)
                requestUri.startsWith("/auth/v1/devices") && method in listOf("GET", "POST") -> {
                    logger.debug("Applying device management rate limiting")
                    rateLimitingService.checkAuthRateLimit("devices", request)
                }
            }

            return true

        } catch (e: Exception) {
            logger.warn("Rate limiting interceptor error for {} {}: {}", method, requestUri, e.message)
            // Let the error be handled by the global exception handler
            throw e
        }
    }

    /**
     * Extract endpoint name from auth URI
     */
    private fun extractAuthEndpoint(requestUri: String): String {
        return when {
            requestUri.endsWith("/init") -> "init"
            requestUri.endsWith("/verify") -> "verify"
            requestUri.endsWith("/refresh_token") -> "refresh_token"
            requestUri.endsWith("/logout") -> "logout"
            requestUri.endsWith("/logout/all") -> "logout"
            requestUri.contains("/devices") -> "devices"
            else -> "unknown"
        }
    }

    /**
     * Check if user is authenticated
     */
    private fun isUserAuthenticated(): Boolean {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            authentication?.isAuthenticated == true && authentication.name != "anonymousUser"
        } catch (e: Exception) {
            logger.debug("Error checking authentication status: {}", e.message)
            false
        }
    }
}