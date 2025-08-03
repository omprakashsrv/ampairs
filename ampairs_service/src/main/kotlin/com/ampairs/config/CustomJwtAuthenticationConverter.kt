package com.ampairs.config

import com.ampairs.auth.repository.TokenRepository
import com.ampairs.auth.service.JwtService
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Custom JWT Authentication Converter that leverages Spring Security's OAuth2 Resource Server
 * while maintaining compatibility with our custom JWT token structure and validation logic.
 */
@Component
class CustomJwtAuthenticationConverter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService,
    private val tokenRepository: TokenRepository,
) : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        // Extract roles from JWT claims (our custom claim name is "roles")
        val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
        val authorities: Collection<GrantedAuthority> = roles.map { role ->
            SimpleGrantedAuthority("ROLE_$role")
        }

        val tokenValue = jwt.tokenValue
        val username = jwt.subject

        // Load user details for token validation
        val userDetails = userDetailsService.loadUserByUsername(username)

        // First: Validate token signature and expiry using our JwtService
        // This is fast and doesn't require database access
        if (!jwtService.isTokenValid(tokenValue, userDetails)) {
            throw IllegalArgumentException("Token validation failed")
        }

        // Second: Only if token is valid, check database for revocation/blacklist
        // This optimizes performance by avoiding database lookups for invalid tokens
        val isTokenRevoked = tokenRepository.findByToken(tokenValue)
            .map { token -> token.expired || token.revoked }
            .orElse(false) // If token not found in DB, it's not revoked (normal case)

        if (isTokenRevoked) {
            throw IllegalArgumentException("Token is revoked or expired")
        }

        return JwtAuthenticationToken(jwt, authorities, username)
    }
}