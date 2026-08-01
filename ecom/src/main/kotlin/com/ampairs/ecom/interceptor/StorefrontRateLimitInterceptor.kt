package com.ampairs.ecom.interceptor

import com.ampairs.core.service.RateLimitingService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Per-IP / per-user rate limiting for the public storefront surface (everything under the
 * `/v1/store` path): catalog browse and search, cart mutations and checkout. Reuses the shared
 * [RateLimitingService], which is a
 * no-op unless `app.security.rate-limiting.enabled` is true — so this is safe-by-default and only
 * engages once rate limiting is switched on for the deployment.
 */
@Component
class StorefrontRateLimitInterceptor(
    private val rateLimitingService: RateLimitingService,
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(StorefrontRateLimitInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // Let RateLimitExceededException bubble to the global exception handler.
        rateLimitingService.checkApiRateLimit(request, isAuthenticated())
        return true
    }

    private fun isAuthenticated(): Boolean = try {
        val authentication = SecurityContextHolder.getContext().authentication
        authentication?.isAuthenticated == true && authentication.name != "anonymousUser"
    } catch (e: Exception) {
        logger.debug("Error checking authentication status: {}", e.message)
        false
    }
}
