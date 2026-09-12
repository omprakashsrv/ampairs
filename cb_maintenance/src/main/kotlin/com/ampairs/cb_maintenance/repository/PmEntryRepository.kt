package com.ampairs.cb_maintenance.repository

import com.ampairs.cb_maintenance.domain.model.PmEntry
import com.ampairs.cb_maintenance.domain.model.PmEntryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface PmEntryRepository : CrudRepository<PmEntry, Long> {

    fun findByUid(uid: String?): PmEntry?

    /** Generation cursor: the latest entry for a (store, schedule) pair — its dueDate is the cursor. */
    fun findTopByStoreIdAndPmScheduleIdOrderByDueDateDesc(storeId: String, pmScheduleId: String): PmEntry?

    /** Dedupe guard so the nightly job never double-generates the same slot. */
    fun existsByStoreIdAndPmScheduleIdAndDueDate(storeId: String, pmScheduleId: String, dueDate: Instant): Boolean

    /** Overdue scan for escalation: not-yet-done entries past their due date. */
    @EntityGraph("CbPmEntry.basic")
    fun findByStatusInAndDueDateBeforeAndActiveTrue(
        statuses: Collection<PmEntryStatus>,
        cutoff: Instant,
    ): List<PmEntry>

    /** Load-balancing: count of open PM entries currently owned by an employee. */
    fun countByAssignedToEmployeeIdAndStatusIn(employeeId: String, statuses: Collection<PmEntryStatus>): Long

    // ── Sync feeds (INCLUDING inactive rows so deletions propagate) ──────────────────
    @Query("SELECT MAX(p.updatedAt) FROM cb_pm_entry p")
    fun findMaxUpdatedAt(): Instant?

    @EntityGraph("CbPmEntry.basic")
    @Query("SELECT p FROM cb_pm_entry p")
    fun findAllForSync(pageable: Pageable): Page<PmEntry>

    @EntityGraph("CbPmEntry.basic")
    @Query("SELECT p FROM cb_pm_entry p WHERE p.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<PmEntry>

    // A zoned field employee sees their own zone AND unassigned zone-orphans (blank zonalOfficeId):
    // generated entries whose store has no zone would otherwise be invisible to every field employee
    // (only the HQ all-zones view saw them). Surfacing them for any employee to claim implements the
    // §4.3 free-flow rule (unassigned + empty zone pool → visible chain-wide) at the list-feed level.
    @EntityGraph("CbPmEntry.basic")
    @Query(
        "SELECT p FROM cb_pm_entry p WHERE p.zonalOfficeId = :zone " +
            "OR (p.zonalOfficeId = '' AND p.assignedToEmployeeId IS NULL)",
    )
    fun findByZonalOfficeIdForSync(@Param("zone") zone: String, pageable: Pageable): Page<PmEntry>

    @EntityGraph("CbPmEntry.basic")
    @Query(
        "SELECT p FROM cb_pm_entry p WHERE (p.zonalOfficeId = :zone " +
            "OR (p.zonalOfficeId = '' AND p.assignedToEmployeeId IS NULL)) AND p.updatedAt >= :lastSync",
    )
    fun findByZonalOfficeIdAndUpdatedAtAfter(
        @Param("zone") zone: String,
        @Param("lastSync") lastSync: Instant,
        pageable: Pageable,
    ): Page<PmEntry>
}
