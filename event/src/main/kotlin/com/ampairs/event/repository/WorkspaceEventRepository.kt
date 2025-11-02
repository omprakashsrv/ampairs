package com.ampairs.event.repository

import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
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
interface WorkspaceEventRepository : CrudRepository<WorkspaceEvent, Long>, PagingAndSortingRepository<WorkspaceEvent, Long> {

    fun findByUid(uid: String): WorkspaceEvent?

    /**
     * Find events by workspace ID ordered by sequence number
     */
    fun findByWorkspaceIdOrderBySequenceNumberAsc(workspaceId: String, pageable: Pageable): Page<WorkspaceEvent>

    /**
     * Find events since a specific sequence number, excluding specific device
     */
    @Query("""
        SELECT e FROM workspace_events e
        WHERE e.workspaceId = :workspaceId
        AND e.sequenceNumber > :sinceSequence
        AND e.deviceId != :excludeDeviceId
        ORDER BY e.sequenceNumber ASC
    """)
    fun findEventsSinceSequence(
        @Param("workspaceId") workspaceId: String,
        @Param("sinceSequence") sinceSequence: Long,
        @Param("excludeDeviceId") excludeDeviceId: String,
        pageable: Pageable
    ): Page<WorkspaceEvent>

    /**
     * Find unconsumed events for a workspace
     */
    @Query("""
        SELECT e FROM workspace_events e
        WHERE e.workspaceId = :workspaceId
        AND e.consumed = false
        ORDER BY e.sequenceNumber ASC
    """)
    fun findUnconsumedEvents(
        @Param("workspaceId") workspaceId: String,
        pageable: Pageable
    ): Page<WorkspaceEvent>

    /**
     * Get the next sequence number for a workspace
     */
    @Query("""
        SELECT COALESCE(MAX(e.sequenceNumber), 0) + 1
        FROM workspace_events e
        WHERE e.workspaceId = :workspaceId
    """)
    fun getNextSequenceNumber(@Param("workspaceId") workspaceId: String): Long

    /**
     * Find events by entity type and ID
     */
    fun findByWorkspaceIdAndEntityTypeAndEntityIdOrderBySequenceNumberDesc(
        workspaceId: String,
        entityType: String,
        entityId: String,
        pageable: Pageable
    ): Page<WorkspaceEvent>

    /**
     * Find events by type
     */
    fun findByWorkspaceIdAndEventTypeOrderBySequenceNumberDesc(
        workspaceId: String,
        eventType: EventType,
        pageable: Pageable
    ): Page<WorkspaceEvent>

    /**
     * Delete consumed events older than specified date
     */
    @Modifying
    @Query("""
        DELETE FROM workspace_events e
        WHERE e.consumed = true
        AND e.createdAt < :cutoffDate
    """)
    fun deleteConsumedEventsBefore(@Param("cutoffDate") cutoffDate: LocalDateTime): Int

    /**
     * Mark event as consumed
     */
    @Modifying
    @Query("""
        UPDATE workspace_events e
        SET e.consumed = true
        WHERE e.uid = :uid
    """)
    fun markAsConsumed(@Param("uid") uid: String): Int

    /**
     * Count unconsumed events for a workspace
     */
    @Query("""
        SELECT COUNT(e) FROM workspace_events e
        WHERE e.workspaceId = :workspaceId
        AND e.consumed = false
    """)
    fun countUnconsumedEvents(@Param("workspaceId") workspaceId: String): Long
}
