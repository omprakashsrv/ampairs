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
        // Check for API key in header
        val apiKey = request.getHeader(API_KEY_HEADER)

        if (apiKey != null && apiKey.startsWith(API_KEY_PREFIX)) {
            try {
                // Create unauthenticated token
                val authRequest = ApiKeyAuthenticationToken(apiKey)

                // Authenticate via AuthenticationManager
                val authResult = authenticationManager.authenticate(authRequest)

                // Set authenticated token in SecurityContext
                SecurityContextHolder.getContext().authentication = authResult

                logger.debug("API key authentication successful")

            } catch (e: Exception) {
                logger.warn("API key authentication failed: ${e.message}")
                // Clear any existing authentication
                SecurityContextHolder.clearContext()
                // Don't throw - let Spring Security handle 401/403
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Skip filter if no API key header present
        return request.getHeader(API_KEY_HEADER) == null
    }
}
