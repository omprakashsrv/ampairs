package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Duration
import java.time.Instant

/**
 * Device registration for offline subscription enforcement.
 * Tracks registered devices with token-based expiry.
 */
@Entity
@Table(
    name = "device_registrations",
    indexes = [
        Index(name = "idx_device_uid", columnList = "uid", unique = true),
        Index(name = "idx_device_workspace", columnList = "workspace_id"),
        Index(name = "idx_device_user", columnList = "user_id"),
        Index(name = "idx_device_token_expires", columnList = "token_expires_at"),
        Index(name = "idx_device_active", columnList = "is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_device_workspace_device",
            columnNames = ["workspace_id", "device_id"]
        )
    ]
)
class DeviceRegistration : BaseDomain() {

    /**
     * Workspace this device is registered to
     */
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    /**
     * User who registered this device
     */
    @Column(name = "user_id", nullable = false, length = 200)
    var userId: String = ""

    /**
     * Unique device identifier
     */
    @Column(name = "device_id", nullable = false, length = 200)
    var deviceId: String = ""

    /**
     * Human-readable device name
     */
    @Column(name = "device_name", length = 200)
    var deviceName: String? = null

    /**
     * Device platform
     */
    @Column(name = "platform", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var platform: DevicePlatform = DevicePlatform.ANDROID

    /**
     * Device model (e.g., "iPhone 15 Pro", "Samsung Galaxy S24")
     */
    @Column(name = "device_model", length = 100)
    var deviceModel: String? = null

    /**
     * OS version
     */
    @Column(name = "os_version", length = 50)
    var osVersion: String? = null

    /**
     * App version installed
     */
    @Column(name = "app_version", length = 20)
    var appVersion: String? = null

    /**
     * Token expiry time (7 days from registration/refresh)
     */
    @Column(name = "token_expires_at", nullable = false)
    var tokenExpiresAt: Instant = Instant.now().plus(Duration.ofDays(7))

    /**
     * Last sync timestamp
     */
    @Column(name = "last_sync_at")
    var lastSyncAt: Instant? = null

    /**
     * Last activity timestamp
     */
    @Column(name = "last_activity_at")
    var lastActivityAt: Instant? = null

    /**
     * Push notification token
     */
    @Column(name = "push_token", length = 500)
    var pushToken: String? = null

    /**
     * Push token type (FCM, APNS)
     */
    @Column(name = "push_token_type", length = 20)
    var pushTokenType: String? = null

    /**
     * Whether this device is active
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    /**
     * When device was deactivated
     */
    @Column(name = "deactivated_at")
    var deactivatedAt: Instant? = null

    /**
     * Reason for deactivation
     */
    @Column(name = "deactivation_reason", length = 200)
    var deactivationReason: String? = null

    /**
     * IP address of last connection
     */
    @Column(name = "last_ip", length = 50)
    var lastIp: String? = null

    /**
     * Metadata JSON
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String = "{}"

    override fun obtainSeqIdPrefix(): String {
        return "DEV"
    }

    companion object {
        /** Token validity period in days */
        const val TOKEN_VALIDITY_DAYS = 7L

        /** Grace period after token expiry in days */
        const val GRACE_PERIOD_DAYS = 3L

        /** Maximum offline days before lock */
        const val MAX_OFFLINE_DAYS = 14L
    }

    /**
     * Check if token is valid
     */
    fun isTokenValid(): Boolean {
        return isActive && tokenExpiresAt.isAfter(Instant.now())
    }

    /**
     * Check if device is in grace period
     */
    fun isInGracePeriod(): Boolean {
        if (!isActive) return false
        val now = Instant.now()
        val graceEnd = tokenExpiresAt.plus(Duration.ofDays(GRACE_PERIOD_DAYS))
        return now.isAfter(tokenExpiresAt) && now.isBefore(graceEnd)
    }

    /**
     * Check if device should be locked
     */
    fun shouldBeLocked(): Boolean {
        if (!isActive) return true
        val now = Instant.now()
        val lockTime = tokenExpiresAt.plus(Duration.ofDays(MAX_OFFLINE_DAYS))
        return now.isAfter(lockTime)
    }

    /**
     * Determine access mode based on token status
     */
    fun getAccessMode(): SubscriptionAccessMode {
        return when {
            !isActive -> SubscriptionAccessMode.LOCKED
            isTokenValid() -> SubscriptionAccessMode.FULL_ACCESS
            isInGracePeriod() -> SubscriptionAccessMode.OFFLINE_GRACE
            shouldBeLocked() -> SubscriptionAccessMode.LOCKED
            else -> SubscriptionAccessMode.READ_ONLY
        }
    }

    /**
     * Refresh token for another validity period
     */
    fun refreshToken() {
        tokenExpiresAt = Instant.now().plus(Duration.ofDays(TOKEN_VALIDITY_DAYS))
        lastSyncAt = Instant.now()
    }

    /**
     * Deactivate this device
     */
    fun deactivate(reason: String) {
        isActive = false
        deactivatedAt = Instant.now()
        deactivationReason = reason
    }
}
