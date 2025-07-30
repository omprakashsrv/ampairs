package com.ampairs.auth.service

import com.ampairs.core.config.ApplicationProperties
import com.ampairs.core.multitenancy.TenantAware
import com.ampairs.core.multitenancy.TenantContext
import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.security.Key
import java.time.Instant
import java.util.*
import java.util.function.Function

@Service
class JwtService(
    private val applicationProperties: ApplicationProperties,
) {
    private val logger = LoggerFactory.getLogger(JwtService::class.java)

    companion object {
        const val TENANT_CLAIM = "tenant"
        const val USER_ID_CLAIM = "userId"
        const val ROLES_CLAIM = "roles"
        const val TOKEN_TYPE_CLAIM = "type"
        const val ACCESS_TOKEN_TYPE = "access"
        const val REFRESH_TOKEN_TYPE = "refresh"
    }

    fun extractUsername(token: String): String {
        return extractClaim(token) { claims: Claims -> claims.subject }
    }

    fun extractTenantId(token: String): String? {
        return extractClaim(token) { claims: Claims -> claims[TENANT_CLAIM] as? String }
    }

    fun extractUserId(token: String): String? {
        return extractClaim(token) { claims: Claims -> claims[USER_ID_CLAIM] as? String }
    }

    fun extractRoles(token: String): List<String> {
        return extractClaim(token) { claims: Claims ->
            @Suppress("UNCHECKED_CAST")
            claims[ROLES_CLAIM] as? List<String> ?: emptyList()
        }
    }

    fun <T> extractClaim(token: String, claimsResolver: Function<Claims, T>): T {
        val claims = extractAllClaims(token)
        return claimsResolver.apply(claims)
    }

    fun generateToken(userDetails: UserDetails): String {
        return generateToken(emptyMap(), userDetails)
    }

    fun generateToken(extraClaims: Map<String, Any>, userDetails: UserDetails): String {
        val claims = buildClaims(extraClaims, userDetails)
        return buildToken(
            claims,
            userDetails,
            applicationProperties.security.jwt.expiration.toMillis(),
            ACCESS_TOKEN_TYPE
        )
    }

    fun generateRefreshToken(userDetails: UserDetails): String {
        val claims = mapOf(TOKEN_TYPE_CLAIM to REFRESH_TOKEN_TYPE)
        return buildToken(
            claims,
            userDetails,
            applicationProperties.security.jwt.refreshToken.expiration.toMillis(),
            REFRESH_TOKEN_TYPE
        )
    }

    private fun buildClaims(extraClaims: Map<String, Any>, userDetails: UserDetails): Map<String, Any> {
        val claims = mutableMapOf<String, Any>()
        claims.putAll(extraClaims)

        // Add tenant information if available
        TenantContext.getCurrentTenant()?.let { tenant ->
            claims[TENANT_CLAIM] = tenant
        }

        // Add user-specific information if UserDetails implements custom interfaces
        if (userDetails is TenantAware) {
            userDetails.getTenantId()?.let { claims[TENANT_CLAIM] = it }
        }

        if (userDetails is UserDetailsWithId) {
            claims[USER_ID_CLAIM] = userDetails.getId()
        }

        if (userDetails is UserDetailsWithRoles) {
            claims[ROLES_CLAIM] = userDetails.getRoles()
        }

        claims[TOKEN_TYPE_CLAIM] = ACCESS_TOKEN_TYPE

        return claims
    }

    private fun buildToken(
        claims: Map<String, Any>,
        userDetails: UserDetails,
        expiration: Long,
        tokenType: String,
    ): String {
        val now = Instant.now()
        val expiryDate = Date.from(now.plusMillis(expiration))

        return try {
            Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.username)
                .setIssuedAt(Date.from(now))
                .setExpiration(expiryDate)
                .signWith(signInKey, SignatureAlgorithm.HS256)
                .compact()
                .also {
                    logger.debug(
                        "Generated {} token for user: {}, expires: {}",
                        tokenType,
                        userDetails.username,
                        expiryDate
                    )
                }
        } catch (e: Exception) {
            logger.error("Failed to generate {} token for user: {}", tokenType, userDetails.username, e)
            throw JwtTokenGenerationException("Failed to generate $tokenType token", e)
        }
    }

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        return try {
            val username = extractUsername(token)
            val isUsernameValid = username == userDetails.username
            val isTokenExpired = isTokenExpired(token)
            val isValid = isUsernameValid && !isTokenExpired

            logger.debug(
                "Token validation: username={}, valid={}, expired={}",
                username,
                isUsernameValid,
                isTokenExpired
            )
            isValid
        } catch (e: Exception) {
            logger.warn("Token validation failed: {}", e.message)
            false
        }
    }

    fun isRefreshToken(token: String): Boolean {
        return try {
            val tokenType = extractClaim(token) { claims: Claims -> claims[TOKEN_TYPE_CLAIM] as? String }
            tokenType == REFRESH_TOKEN_TYPE
        } catch (e: Exception) {
            false
        }
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    private fun extractExpiration(token: String): Date {
        return extractClaim(token) { claims: Claims -> claims.expiration }
    }

    private fun extractAllClaims(token: String): Claims {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(signInKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: ExpiredJwtException) {
            logger.debug("JWT token expired: {}", e.message)
            throw e
        } catch (e: UnsupportedJwtException) {
            logger.error("Unsupported JWT token: {}", e.message)
            throw e
        } catch (e: MalformedJwtException) {
            logger.error("Invalid JWT token: {}", e.message)
            throw e
        } catch (e: SignatureException) {
            logger.error("Invalid JWT signature: {}", e.message)
            throw e
        } catch (e: IllegalArgumentException) {
            logger.error("JWT token compact of handler are invalid: {}", e.message)
            throw e
        }
    }

    private val signInKey: Key
        get() {
            val keyBytes = Decoders.BASE64.decode(applicationProperties.security.jwt.secretKey)
            return Keys.hmacShaKeyFor(keyBytes)
        }

    fun getSignInKey(): ByteArray {
        return Decoders.BASE64.decode(applicationProperties.security.jwt.secretKey)
    }
}

interface UserDetailsWithId {
    fun getId(): String
}

interface UserDetailsWithRoles {
    fun getRoles(): List<String>
}

class JwtTokenGenerationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
