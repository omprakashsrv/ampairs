package com.ampairs.auth.repository

import com.ampairs.auth.model.DeviceSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface DeviceSessionRepository : JpaRepository<DeviceSession, String> {

    /**
     * Find active device session by user ID and device ID
     */
    fun findByUserIdAndDeviceIdAndIsActiveTrue(userId: String, deviceId: String): Optional<DeviceSession>

    /**
     * Find all active device sessions for a user
     */
    fun findByUserIdAndIsActiveTrueOrderByLastActivityDesc(userId: String): List<DeviceSession>

    /**
     * Find all device sessions for a user (active and inactive)
     */
    fun findByUserIdOrderByLastActivityDesc(userId: String): List<DeviceSession>

    /**
     * Find device session by refresh token hash
     */
    fun findByRefreshTokenHashAndIsActiveTrue(refreshTokenHash: String): Optional<DeviceSession>

    /**
     * Deactivate all sessions for a user (logout from all devices)
     */
    @Modifying
    @Query("UPDATE DeviceSession d SET d.isActive = false, d.refreshTokenHash = null WHERE d.userId = :userId")
    fun deactivateAllUserSessions(@Param("userId") userId: String): Int

    /**
     * Deactivate specific device session
     */
    @Modifying
    @Query("UPDATE DeviceSession d SET d.isActive = false, d.refreshTokenHash = null WHERE d.userId = :userId AND d.deviceId = :deviceId")
    fun deactivateDeviceSession(@Param("userId") userId: String, @Param("deviceId") deviceId: String): Int

    /**
     * Update last activity for a device session
     */
    @Modifying
    @Query("UPDATE DeviceSession d SET d.lastActivity = :lastActivity WHERE d.userId = :userId AND d.deviceId = :deviceId AND d.isActive = true")
    fun updateLastActivity(
        @Param("userId") userId: String,
        @Param("deviceId") deviceId: String,
        @Param("lastActivity") lastActivity: LocalDateTime,
    ): Int

    /**
     * Find expired device sessions (inactive for more than specified days)
     */
    @Query("SELECT d FROM DeviceSession d WHERE d.lastActivity < :cutoffDate")
    fun findExpiredSessions(@Param("cutoffDate") cutoffDate: LocalDateTime): List<DeviceSession>

    /**
     * Delete expired device sessions
     */
    @Modifying
    @Query("DELETE FROM DeviceSession d WHERE d.lastActivity < :cutoffDate")
    fun deleteExpiredSessions(@Param("cutoffDate") cutoffDate: LocalDateTime): Int

    /**
     * Count active sessions for a user
     */
    fun countByUserIdAndIsActiveTrue(userId: String): Long

    /**
     * Find device sessions by IP address (for security monitoring)
     */
    fun findByIpAddressAndIsActiveTrueOrderByLastActivityDesc(ipAddress: String): List<DeviceSession>

    /**
     * Check if device ID exists for user (to prevent duplicate device IDs)
     */
    fun existsByUserIdAndDeviceId(userId: String, deviceId: String): Boolean

    /**
     * Find active sessions for a user ordered by last activity (oldest first)
     */
    fun findByUserIdAndIsActiveTrueOrderByLastActivityAsc(userId: String): List<DeviceSession>

    /**
     * Find active sessions that need cleanup (limited batch size)
     */
    @Query(
        value = "SELECT * FROM device_session WHERE is_active = true ORDER BY last_activity ASC LIMIT ?1",
        nativeQuery = true
    )
    fun findActiveSessionsForCleanup(limit: Int): List<DeviceSession>

    /**
     * Count total active sessions
     */
    fun countByIsActiveTrue(): Long

    /**
     * Count active sessions by device type
     */
    @Query("SELECT d.deviceType, COUNT(d) FROM DeviceSession d WHERE d.isActive = true GROUP BY d.deviceType")
    fun countActiveSessionsByDeviceType(): List<Array<Any>>

    /**
     * Find the oldest active session
     */
    @Query(
        value = "SELECT * FROM device_session WHERE is_active = true ORDER BY login_time ASC LIMIT 1",
        nativeQuery = true
    )
    fun findOldestActiveSession(): DeviceSession?
}