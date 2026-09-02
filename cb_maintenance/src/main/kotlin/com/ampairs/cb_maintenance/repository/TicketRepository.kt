package com.ampairs.cb_maintenance.repository

import com.ampairs.cb_maintenance.domain.model.Ticket
import com.ampairs.cb_maintenance.domain.model.TicketStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface TicketRepository : CrudRepository<Ticket, Long> {

    fun findByUid(uid: String?): Ticket?

    /** Idempotency guard for PM-failure → ticket spawn (§6). */
    fun existsByOriginPmEntryIdAndSubCategory(originPmEntryId: String, subCategory: String): Boolean

    /** Load-balancing: count of open tickets currently owned by an employee. */
    fun countByAssignedToEmployeeIdAndStatusIn(employeeId: String, statuses: Collection<TicketStatus>): Long

    // ── Sync feeds (INCLUDING inactive rows so deletions propagate) ──────────────────
    @Query("SELECT MAX(t.updatedAt) FROM cb_ticket t")
    fun findMaxUpdatedAt(): Instant?

    @EntityGraph("CbTicket.basic")
    @Query("SELECT t FROM cb_ticket t")
    fun findAllForSync(pageable: Pageable): Page<Ticket>

    @EntityGraph("CbTicket.basic")
    @Query("SELECT t FROM cb_ticket t WHERE t.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<Ticket>

    @EntityGraph("CbTicket.basic")
    @Query("SELECT t FROM cb_ticket t WHERE t.zonalOfficeId = :zone")
    fun findByZonalOfficeIdForSync(@Param("zone") zone: String, pageable: Pageable): Page<Ticket>

    @EntityGraph("CbTicket.basic")
    @Query("SELECT t FROM cb_ticket t WHERE t.zonalOfficeId = :zone AND t.updatedAt >= :lastSync")
    fun findByZonalOfficeIdAndUpdatedAtAfter(
        @Param("zone") zone: String,
        @Param("lastSync") lastSync: Instant,
        pageable: Pageable,
    ): Page<Ticket>
}
