package com.ampairs.core.auth.token

import com.ampairs.core.auth.domain.ApiKey
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * Authentication token for API key authentication.
 *
 * This is used in the authentication flow:
 * 1. Filter creates unauthenticated token with plain API key
 * 2. AuthenticationManager delegates to ApiKeyAuthenticationProvider
 * 3. Provider validates and returns authenticated token with authorities
 */
class ApiKeyAuthenticationToken : AbstractAuthenticationToken {

    private val apiKey: String?
    private val keyInfo: ApiKey?

    /**
     * Create unauthenticated token (before authentication).
     *
     * @param apiKey Plain API key from X-API-Key header
     */
    constructor(apiKey: String) : super(null) {
        this.apiKey = apiKey
        this.keyInfo = null
        isAuthenticated = false
    }

    /**
     * Create authenticated token (after successful authentication).
     *
     * @param keyInfo Validated API key entity
     * @param authorities Granted authorities based on key scope
     */
    constructor(keyInfo: ApiKey, authorities: Collection<GrantedAuthority>) : super(authorities) {
        this.apiKey = null
        this.keyInfo = keyInfo
        isAuthenticated = true
    }

    /**
     * Returns the credentials (plain API key before authentication, null after).
     */
    override fun getCredentials(): Any? = apiKey

    /**
     * Returns the principal (API key info after authentication).
     */
    override fun getPrincipal(): Any {
        return keyInfo?.let { "api-key:${it.uid}" } ?: "unauthenticated"
    }

    /**
     * Get the validated API key info (only available after authentication).
     */
    fun getKeyInfo(): ApiKey? = keyInfo
}
