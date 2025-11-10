package com.ampairs.core.auth.filter

import com.ampairs.core.auth.service.ApiKeyService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter for API key authentication.
 *
 * Checks for X-API-Key header and validates it.
 * Sets authentication in SecurityContext for downstream authorization.
 */
@Component
class ApiKeyAuthenticationFilter(
    private val apiKeyService: ApiKeyService
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
                // Validate and track usage
                val keyInfo = apiKeyService.validateAndUse(apiKey)

                // Build authorities based on scope
                val authorities = listOf(
                    SimpleGrantedAuthority("API_KEY"),
                    SimpleGrantedAuthority("API_KEY:${keyInfo.scope}")
                )

                // Create authentication token
                val authentication = UsernamePasswordAuthenticationToken(
                    "api-key:${keyInfo.uid}",  // Principal (for logging)
                    null,                       // Credentials (none)
                    authorities                 // Authorities
                )

                // Set in security context
                SecurityContextHolder.getContext().authentication = authentication

                logger.debug("API key authenticated: ${keyInfo.uid}, scope: ${keyInfo.scope}")
            } catch (e: Exception) {
                logger.warn("API key authentication failed: ${e.message}")
                // Continue without authentication - let Spring Security handle 401/403
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
