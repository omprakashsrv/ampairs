package com.ampairs.cb_maintenance.repository

import com.ampairs.cb_maintenance.domain.model.PmSchedule
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface PmScheduleRepository : CrudRepository<PmSchedule, Long> {

    fun findByUid(uid: String?): PmSchedule?

    /** Active schedules — the nightly PM-generation job rolls each across every store. */
    @EntityGraph("CbPmSchedule.basic")
    fun findByActiveTrue(): List<PmSchedule>

    @Query("SELECT MAX(s.updatedAt) FROM cb_pm_schedule s")
    fun findMaxUpdatedAt(): Instant?

    @EntityGraph("CbPmSchedule.basic")
    @Query("SELECT s FROM cb_pm_schedule s")
    fun findAllForSync(pageable: Pageable): Page<PmSchedule>

    @EntityGraph("CbPmSchedule.basic")
    @Query("SELECT s FROM cb_pm_schedule s WHERE s.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<PmSchedule>
}
