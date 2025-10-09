package com.ampairs.core.domain.model

import com.ampairs.core.utils.Helper
import jakarta.persistence.*
import java.time.Instant

/**
 * Base domain entity with common fields for all entities.
 *
 * **IMPORTANT TIMEZONE NOTE**:
 * - Uses `Instant` for timestamps (always UTC, no timezone ambiguity)
 * - `createdAt` and `updatedAt` are set automatically via @PrePersist/@PreUpdate
 * - Database stores as TIMESTAMP (UTC-aware in MySQL)
 * - Jackson serializes as ISO-8601 with 'Z' suffix (e.g., "2025-01-09T14:30:00Z")
 * - Clients should convert to local timezone for display
 *
 * @see java.time.Instant
 * @see com.ampairs.core.config.JacksonConfig
 */
@MappedSuperclass
abstract class BaseDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long = 0

    @Column(name = "uid", length = 200, updatable = false, nullable = false, unique = true)
    var uid: String = ""

    /**
     * Entity creation timestamp in UTC.
     *
     * Set automatically on first persist.
     * Immutable after creation (updatable = false).
     * Serializes as ISO-8601 with Z: "2025-01-09T14:30:00Z"
     */
    @Column(
        name = "created_at",
        updatable = false, nullable = false
    )
    var createdAt: Instant? = null

    /**
     * Last update timestamp in UTC.
     *
     * Set automatically on create and every update.
     * Serializes as ISO-8601 with Z: "2025-01-09T14:30:00Z"
     */
    @Column(
        name = "updated_at",
        nullable = false
    )
    var updatedAt: Instant? = null

    /**
     * Unix timestamp (milliseconds) of last update.
     *
     * Redundant with updatedAt but kept for backward compatibility.
     * Consider removing in future version.
     */
    @Column(
        name = "last_updated",
        nullable = false
    )
    var lastUpdated: Long = 0

    abstract fun obtainSeqIdPrefix(): String

    @PrePersist
    protected fun prePersist() {
        if (uid == "") {
            uid = Helper.generateUniqueId(obtainSeqIdPrefix(), com.ampairs.core.config.Constants.ID_LENGTH)
        }
        // Always use UTC - Instant.now() is always UTC-based
        val now = Instant.now()
        if (createdAt == null) {
            createdAt = now
        }
        updatedAt = now
        lastUpdated = now.toEpochMilli()
    }

    @PreUpdate
    protected fun preUpdate() {
        // Always use UTC - Instant.now() is always UTC-based
        val now = Instant.now()
        updatedAt = now
        lastUpdated = now.toEpochMilli()
    }
}
