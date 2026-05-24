package com.ampairs.auth.service

import com.ampairs.auth.model.DeviceSession
import com.ampairs.auth.repository.DeviceSessionRepository
import com.ampairs.auth.repository.TokenRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.Optional

/**
 * Cached wrapper for hot-path token and device session lookups.
 *
 * Every authenticated request hits convert() in CustomJwtAuthenticationConverter,
 * which previously made 2–3 DB round trips per request. These caches cut that to
 * zero on cache hits.
 *
 * TTLs (configured in application.yml):
 *  - tokenValidity: 60 s — acceptable window for revocation lag
 *  - deviceSession: 30 s — sessions change state infrequently within a request burst
 *
 * Cache eviction is triggered by LogoutService so revoked tokens clear immediately.
 */
@Service
class TokenValidationCacheService(
    private val tokenRepository: TokenRepository,
    private val deviceSessionRepository: DeviceSessionRepository,
) {

    @Cacheable(value = ["tokenValidity"], key = "#tokenHash")
    fun isTokenRevoked(tokenHash: String, tokenValue: String): Boolean {
        return tokenRepository.findByToken(tokenValue)
            .map { it.expired || it.revoked }
            .orElse(false)
    }

    @CacheEvict(value = ["tokenValidity"], key = "#tokenHash")
    fun evictTokenValidity(tokenHash: String) {
    }

    @Cacheable(value = ["deviceSession"], key = "#userId + ':' + #deviceId")
    fun findActiveDeviceSession(userId: String, deviceId: String): Optional<DeviceSession> {
        return deviceSessionRepository.findByUserIdAndDeviceIdAndIsActiveTrue(userId, deviceId)
    }

    @CacheEvict(value = ["deviceSession"], key = "#userId + ':' + #deviceId")
    fun evictDeviceSession(userId: String, deviceId: String) {
    }
}
