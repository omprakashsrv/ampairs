package com.ampairs.core.auth.filter

import com.ampairs.core.auth.token.ApiKeyAuthenticationToken
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter for API key authentication.
 *
 * This filter:
 * 1. Extracts API key from X-API-Key header
 * 2. Creates unauthenticated ApiKeyAuthenticationToken
 * 3. Delegates to AuthenticationManager (which uses ApiKeyAuthenticationProvider)
 * 4. Sets authenticated token in SecurityContext
 *
 * Part of Spring Security's authentication chain.
 */
class ApiKeyAuthenticationFilter(
    private val authenticationManager: AuthenticationManager
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val API_KEY_HEADER = "X-API-Key"
        private const val API_KEY_PREFIX = "amp_"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestUri = request.requestURI
        val apiKey = request.getHeader(API_KEY_HEADER)

        logger.info("ApiKeyAuthenticationFilter processing: $requestUri")
        logger.info("API key header present: ${apiKey != null}, starts with prefix: ${apiKey?.startsWith(API_KEY_PREFIX)}")

        if (apiKey != null && apiKey.startsWith(API_KEY_PREFIX)) {
            try {
                logger.info("Attempting API key authentication for: $requestUri")

                // Create unauthenticated token
                val authRequest = ApiKeyAuthenticationToken(apiKey)

                // Authenticate via AuthenticationManager
                val authResult = authenticationManager.authenticate(authRequest)

                // Set authenticated token in SecurityContext
                SecurityContextHolder.getContext().authentication = authResult

                logger.info("API key authentication successful for: $requestUri")

            } catch (e: Exception) {
                logger.error("API key authentication failed for $requestUri: ${e.message}", e)
                // Clear any existing authentication
                SecurityContextHolder.clearContext()
                // Don't throw - let Spring Security handle 401/403
            }
        } else {
            logger.info("No valid API key provided for: $requestUri")
        }

        // Continue filter chain
        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val apiKeyHeader = request.getHeader(API_KEY_HEADER)
        val shouldSkip = apiKeyHeader == null

        if (shouldSkip) {
            logger.info("ApiKeyAuthenticationFilter SKIPPED for ${request.requestURI} - no X-API-Key header")
        }

        return shouldSkip
    }
}
