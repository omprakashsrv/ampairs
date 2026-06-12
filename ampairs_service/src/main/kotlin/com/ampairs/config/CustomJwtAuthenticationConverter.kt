package com.ampairs.config

import com.ampairs.auth.service.SessionManagementService
import com.ampairs.auth.service.TokenValidationCacheService
import com.ampairs.core.security.AdminService
import com.ampairs.user.model.User
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
    private val tokenValidationCacheService: TokenValidationCacheService,
    private val userDetailsService: UserDetailsService,
    private val sessionManagementService: SessionManagementService,
    private val adminService: AdminService,
) : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
        var authorities: Collection<GrantedAuthority> = roles.map { SimpleGrantedAuthority("ROLE_$it") }

        val tokenValue = jwt.tokenValue
        val username = jwt.subject ?: throw IllegalArgumentException("JWT subject is missing")

        val userDetails: User = userDetailsService.loadUserByUsername(username) as User
        if (adminService.isAdmin(userDetails.uid)) {
            authorities = authorities.plus(SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        }

        // Use first 32 chars as cache key to avoid storing full JWT in cache key space
        val tokenHash = tokenValue.take(32)
        if (tokenValidationCacheService.isTokenRevoked(tokenHash, tokenValue)) {
            throw IllegalArgumentException("Token is revoked or expired")
        }

        val deviceId = jwt.getClaimAsString("device_id")
        if (deviceId != null) {
            val deviceSession = tokenValidationCacheService.findActiveDeviceSession(username, deviceId)
            if (deviceSession.isPresent) {
                if (!sessionManagementService.validateAndExpireIfNeeded(deviceSession.get())) {
                    tokenValidationCacheService.evictDeviceSession(username, deviceId)
                    throw IllegalArgumentException("Device session has expired")
                }
            } else {
                throw IllegalArgumentException("Device session not found or inactive")
            }
        }

        return UsernamePasswordAuthenticationToken(userDetails, null, authorities)
    }
}