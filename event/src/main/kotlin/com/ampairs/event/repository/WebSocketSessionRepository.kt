package com.ampairs.event.repository

import com.ampairs.event.domain.WebSocketSession
import com.ampairs.event.domain.DeviceStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface WebSocketSessionRepository : CrudRepository<WebSocketSession, Long>, PagingAndSortingRepository<WebSocketSession, Long> {

    fun findByUid(uid: String): WebSocketSession?

    fun findBySessionId(sessionId: String): WebSocketSession?

    /**
     * Find active sessions for a workspace
     */
    fun findByWorkspaceIdAndStatusIn(
        workspaceId: String,
        statuses: List<DeviceStatus>,
        pageable: Pageable
    ): Page<WebSocketSession>

    /**
     * Find all active sessions (online or away) for a workspace
     */
    @Query("""
        SELECT s FROM device_sessions s
        WHERE s.workspaceId = :workspaceId
        AND s.status IN ('ONLINE', 'AWAY')
        ORDER BY s.lastHeartbeat DESC
    """)
    fun findActiveSessions(
        @Param("workspaceId") workspaceId: String,
        pageable: Pageable
    ): Page<WebSocketSession>

    /**
     * Find sessions by workspace and user
     */
    fun findByWorkspaceIdAndUserId(
        workspaceId: String,
        userId: String,
        pageable: Pageable
    ): Page<WebSocketSession>

    /**
     * Find active sessions for a user
     */
    @Query("""
        SELECT s FROM device_sessions s
        WHERE s.workspaceId = :workspaceId
        AND s.userId = :userId
        AND s.status IN ('ONLINE', 'AWAY')
        ORDER BY s.lastHeartbeat DESC
    """)
    fun findActiveSessionsByUser(
        @Param("workspaceId") workspaceId: String,
        @Param("userId") userId: String,
        pageable: Pageable
    ): Page<WebSocketSession>

    /**
     * Find stale sessions (no heartbeat for specified time)
     */
    @Query("""
        SELECT s FROM device_sessions s
        WHERE s.status IN ('ONLINE', 'AWAY')
        AND s.lastHeartbeat < :cutoffTime
    """)
    fun findStaleSessions(
        @Param("cutoffTime") cutoffTime: LocalDateTime
    ): List<WebSocketSession>

    /**
     * Find sessions by status and last heartbeat before specified time
     */
    fun findByStatusAndLastHeartbeatBefore(
        status: DeviceStatus,
        cutoffTime: LocalDateTime
    ): List<WebSocketSession>

    /**
     * Mark session as offline
     */
    @Modifying
    @Query("""
        UPDATE device_sessions s
        SET s.status = 'OFFLINE', s.disconnectedAt = :disconnectedAt
        WHERE s.sessionId = :sessionId
    """)
    fun markSessionOffline(
        @Param("sessionId") sessionId: String,
        @Param("disconnectedAt") disconnectedAt: LocalDateTime
    ): Int

    /**
     * Update heartbeat for a session
     */
    @Modifying
    @Query("""
        UPDATE device_sessions s
        SET s.lastHeartbeat = :heartbeatTime, s.status = 'ONLINE'
        WHERE s.sessionId = :sessionId
    """)
    fun updateHeartbeat(
        @Param("sessionId") sessionId: String,
        @Param("heartbeatTime") heartbeatTime: LocalDateTime
    ): Int

    /**
     * Delete offline sessions older than specified date
     */
    @Modifying
    @Query("""
        DELETE FROM device_sessions s
        WHERE s.status = 'OFFLINE'
        AND s.disconnectedAt < :cutoffDate
    """)
    fun deleteOfflineSessionsBefore(@Param("cutoffDate") cutoffDate: LocalDateTime): Int

    /**
     * Count active sessions for a workspace
     */
    @Query("""
        SELECT COUNT(s) FROM device_sessions s
        WHERE s.workspaceId = :workspaceId
        AND s.status IN ('ONLINE', 'AWAY')
    """)
    fun countActiveSessions(@Param("workspaceId") workspaceId: String): Long

    /**
     * Find session by workspace, user, and device
     */
    fun findByWorkspaceIdAndUserIdAndDeviceId(
        workspaceId: String,
        userId: String,
        deviceId: String
    ): WebSocketSession?
}
