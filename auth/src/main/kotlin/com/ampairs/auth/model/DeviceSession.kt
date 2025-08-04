package com.ampairs.auth.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "device_session")
class DeviceSession : BaseDomain() {

    @Column(name = "user_id", nullable = false)
    var userId: String = ""

    @Column(name = "device_id", nullable = false, unique = false)
    var deviceId: String = "" // Unique identifier for the device

    @Column(name = "device_name", length = 255)
    var deviceName: String? = null // e.g., "iPhone 15", "Chrome on Windows"

    @Column(name = "device_type", length = 50)
    var deviceType: String? = null // e.g., "mobile", "desktop", "tablet"

    @Column(name = "platform", length = 50)
    var platform: String? = null // e.g., "iOS", "Android", "Web"

    @Column(name = "browser", length = 100)
    var browser: String? = null // e.g., "Chrome", "Safari", "Mobile App"

    @Column(name = "os", length = 100)
    var os: String? = null // e.g., "iOS 17.1", "Windows 11", "Android 14"

    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null // IPv4 or IPv6

    @Column(name = "user_agent", length = 500)
    var userAgent: String? = null

    @Column(name = "location", length = 255)
    var location: String? = null // City, Country (optional, based on IP)

    @Column(name = "last_activity", nullable = false)
    var lastActivity: LocalDateTime = LocalDateTime.now()

    @Column(name = "login_time", nullable = false)
    var loginTime: LocalDateTime = LocalDateTime.now()

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "refresh_token_hash", length = 500)
    var refreshTokenHash: String? = null // Hash of refresh token for this device

    override fun obtainSeqIdPrefix(): String {
        return Constants.DEVICE_SESSION_PREFIX
    }

    /**
     * Update last activity timestamp
     */
    fun updateActivity() {
        this.lastActivity = LocalDateTime.now()
    }

    /**
     * Mark device session as inactive (logout)
     */
    fun deactivate() {
        this.isActive = false
        this.refreshTokenHash = null
    }

    /**
     * Check if device session is expired (inactive for more than specified days)
     */
    fun isExpired(maxInactiveDays: Long = 180): Boolean {
        return lastActivity.isBefore(LocalDateTime.now().minusDays(maxInactiveDays))
    }
}