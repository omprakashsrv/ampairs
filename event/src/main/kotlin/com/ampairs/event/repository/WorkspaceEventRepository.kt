package com.ampairs.event.repository

import com.ampairs.event.domain.WorkspaceEvent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Read-side repository. Writes go through [WorkspaceEventStore] (native upsert).
 *
 * The table now holds at most one row per `(workspace_id, entity_type)` watermark,
 * so per-entity-id and per-event-type history queries no longer make sense and
 * were removed.
 */
@Repository
interface WorkspaceEventRepository :
    CrudRepository<WorkspaceEvent, Long>,
    PagingAndSortingRepository<WorkspaceEvent, Long> {

    fun findByUid(uid: String): WorkspaceEvent?

    fun findByWorkspaceIdOrderBySequenceNumberAsc(workspaceId: String, pageable: Pageable): Page<WorkspaceEvent>

    @Query(
        """
        SELECT e FROM workspace_events e
        WHERE e.workspaceId = :workspaceId
        AND e.sequenceNumber > :sinceSequence
        AND e.deviceId != :excludeDeviceId
        ORDER BY e.sequenceNumber ASC
        """
    )
    fun findEventsSinceSequence(
        @Param("workspaceId") workspaceId: String,
        @Param("sinceSequence") sinceSequence: Long,
        @Param("excludeDeviceId") excludeDeviceId: String,
        pageable: Pageable,
    ): Page<WorkspaceEvent>
}
