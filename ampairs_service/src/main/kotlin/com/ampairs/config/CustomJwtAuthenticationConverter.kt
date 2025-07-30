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

        // Validate token in our database (for revocation/expiry checks)
        val tokenValue = jwt.tokenValue
        val isTokenValid = tokenRepository.findByToken(tokenValue)
            .map { token -> !token.expired && !token.revoked }
            .orElse(false)

        if (!isTokenValid) {
            throw IllegalArgumentException("Token is revoked or expired")
        }

        // Load user details for additional context
        val username = jwt.subject
        val userDetails = userDetailsService.loadUserByUsername(username)

        // Validate token signature and expiry using our JwtService
        if (!jwtService.isTokenValid(tokenValue, userDetails)) {
            throw IllegalArgumentException("Token validation failed")
        }

        return JwtAuthenticationToken(jwt, authorities, username)
    }
}