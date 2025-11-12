package com.ampairs.core.auth.domain

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Instant

/**
 * API Key entity for machine-to-machine authentication.
 *
 * Used for CI/CD pipelines, integrations, and automated systems
 * that need to access admin endpoints without user login.
 *
 * Security Features:
 * - SHA-256 hashed keys (only hash stored in database)
 * - Expiry dates
 * - Can be revoked
 * - Scoped permissions
 * - Usage tracking
 */
@Entity
@Table(
    name = "api_keys",
    indexes = [
        Index(name = "idx_api_keys_key_hash", columnList = "key_hash"),
        Index(name = "idx_api_keys_active", columnList = "is_active,expires_at")
    ]
)
class ApiKey : BaseDomain() {

    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    @Column(name = "description", length = 500)
    var description: String? = null

    @Column(name = "key_hash", nullable = false, length = 64, unique = true)
    var keyHash: String = ""

    @Column(name = "key_prefix", nullable = false, length = 20)
    var keyPrefix: String = ""

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "expires_at")
    var expiresAt: Instant? = null

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null

    @Column(name = "usage_count", nullable = false)
    var usageCount: Long = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 50)
    var scope: ApiKeyScope = ApiKeyScope.APP_UPDATES

    @Column(name = "created_by_user_id", length = 200)
    var createdByUserId: String? = null

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null

    @Column(name = "revoked_by", length = 100)
    var revokedBy: String? = null

    @Column(name = "revoked_reason", length = 500)
    var revokedReason: String? = null

    override fun obtainSeqIdPrefix(): String = "KEY"

    fun isExpired(): Boolean {
        return expiresAt?.isBefore(Instant.now()) ?: false
    }

    fun isValid(): Boolean {
        return isActive && !isExpired() && revokedAt == null
    }
}

enum class ApiKeyScope {
    APP_UPDATES,      // Can manage app versions
    READ_ONLY,        // Read-only access
    FULL_ADMIN        // Full admin access (use sparingly)
}
