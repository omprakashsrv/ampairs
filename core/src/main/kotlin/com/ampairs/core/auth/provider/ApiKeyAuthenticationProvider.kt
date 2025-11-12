package com.ampairs.core.auth.provider

import com.ampairs.core.auth.service.ApiKeyService
import com.ampairs.core.auth.token.ApiKeyAuthenticationToken
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

/**
 * Authentication provider for API key authentication.
 *
 * This provider:
 * 1. Validates API key format and status
 * 2. Tracks usage
 * 3. Returns authenticated token with authorities
 *
 * Integrated into Spring Security's AuthenticationManager chain.
 */
@Component
class ApiKeyAuthenticationProvider(
    private val apiKeyService: ApiKeyService
) : AuthenticationProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Authenticate using API key.
     *
     * @param authentication Unauthenticated ApiKeyAuthenticationToken with plain key
     * @return Authenticated ApiKeyAuthenticationToken with authorities
     * @throws BadCredentialsException if key is invalid
     */
    override fun authenticate(authentication: Authentication): Authentication {
        val apiKey = authentication.credentials as? String
            ?: throw BadCredentialsException("Missing API key")

        try {
            // Validate and track usage
            val keyInfo = apiKeyService.validateAndUse(apiKey)

            // Build authorities based on scope
            val authorities = listOf(
                SimpleGrantedAuthority("API_KEY"),
                SimpleGrantedAuthority("API_KEY:${keyInfo.scope}")
            )

            logger.debug("API key authenticated: ${keyInfo.uid}, scope: ${keyInfo.scope}")

            // Return authenticated token
            return ApiKeyAuthenticationToken(keyInfo, authorities)

        } catch (e: IllegalArgumentException) {
            logger.warn("API key authentication failed: ${e.message}")
            throw BadCredentialsException("Invalid API key", e)
        }
    }

    /**
     * Check if this provider supports the given authentication type.
     */
    override fun supports(authentication: Class<*>): Boolean {
        return ApiKeyAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}
