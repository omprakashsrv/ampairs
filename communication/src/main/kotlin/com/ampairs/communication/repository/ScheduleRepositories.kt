package com.ampairs.communication.repository

import com.ampairs.communication.domain.model.CommunicationOccurrence
import com.ampairs.communication.domain.model.CommunicationSchedule
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface CommunicationScheduleRepository : CrudRepository<CommunicationSchedule, Long> {
    fun findByUid(uid: String?): CommunicationSchedule?

    /**
     * Due, runnable schedules across ALL workspaces — the sweeper runs without tenant context, so
     * this must bypass @TenantId filtering (nativeQuery).
     */
    @Query(
        value = "SELECT * FROM communication_schedule WHERE active = true AND paused = false " +
            "AND next_run_at IS NOT NULL AND next_run_at <= :now",
        nativeQuery = true,
    )
    fun findDue(@Param("now") now: Instant): List<CommunicationSchedule>

    @Query("SELECT s FROM communication_schedule s WHERE s.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<CommunicationSchedule>

    @Query("SELECT s FROM communication_schedule s")
    fun findAllForSync(pageable: Pageable): Page<CommunicationSchedule>
}

interface CommunicationOccurrenceRepository : CrudRepository<CommunicationOccurrence, Long> {
    fun existsByScheduleUidAndOccurrenceKey(scheduleUid: String, occurrenceKey: String): Boolean
}
