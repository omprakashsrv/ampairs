package com.ampairs.config

import com.ampairs.auth.repository.TokenRepository
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Custom JWT Authentication Converter that leverages Spring Security's OAuth2 Resource Server
 * while maintaining compatibility with our custom JWT token structure and validation logic.
 */
@Component
class CustomJwtAuthenticationConverter(
    private val tokenRepository: TokenRepository,
    private val userDetailsService: UserDetailsService,
) : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        // Extract roles from JWT claims (our custom claim name is "roles")
        val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
        val authorities: Collection<GrantedAuthority> = roles.map { role ->
            SimpleGrantedAuthority("ROLE_$role")
        }

        val tokenValue = jwt.tokenValue
        val username = jwt.subject

        // Load user details (consider adding caching layer if performance becomes an issue)
        val userDetails = userDetailsService.loadUserByUsername(username)

        // Check token revocation status in database (cached lookup)
        val isTokenRevoked = tokenRepository.findByToken(tokenValue)
            .map { token -> token.expired || token.revoked }
            .orElse(false)

        if (isTokenRevoked) {
            throw IllegalArgumentException("Token is revoked or expired")
        }

        return UsernamePasswordAuthenticationToken(userDetails, null, authorities)
    }
}