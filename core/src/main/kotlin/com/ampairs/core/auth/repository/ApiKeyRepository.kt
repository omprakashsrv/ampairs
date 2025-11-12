package com.ampairs.core.auth.repository

import com.ampairs.core.auth.domain.ApiKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for API key management.
 */
@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, Long> {

    /**
     * Find API key by hash.
     * Used for authentication.
     */
    fun findByKeyHash(keyHash: String): ApiKey?

    /**
     * Find API key by UID.
     * Used for admin operations (revoke, view).
     */
    fun findByUid(uid: String): ApiKey?

    /**
     * Find all active, non-revoked API keys.
     * Used for admin listing.
     */
    fun findAllByIsActiveTrueAndRevokedAtIsNullOrderByCreatedAtDesc(): List<ApiKey>

    /**
     * Check if API key exists by hash.
     */
    fun existsByKeyHash(keyHash: String): Boolean
}
