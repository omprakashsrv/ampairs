package com.ampairs.cb_employee.repository

import com.ampairs.cb_employee.domain.model.Employee
import com.ampairs.cb_employee.domain.model.MaintenanceRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface EmployeeRepository : CrudRepository<Employee, Long> {

    fun findByUid(uid: String?): Employee?
    fun findByRefId(refId: String?): Employee?
    fun findByUserId(userId: String): Employee?

    /** Assignment pool: active employees in a zone, excluding the given role (MAINTENANCE_LEADER). */
    @EntityGraph("CbEmployee.basic")
    fun findByZonalOfficeIdAndActiveTrueAndRoleNot(
        zonalOfficeId: String,
        role: MaintenanceRole,
    ): List<Employee>

    /** Chain-wide fallback pool: all active employees excluding the given role. */
    @EntityGraph("CbEmployee.basic")
    fun findByActiveTrueAndRoleNot(role: MaintenanceRole): List<Employee>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(e.updatedAt) FROM cb_employee e")
    fun findMaxUpdatedAt(): Instant?

    /**
     * Incremental sync feed: all employees updated at/after lastSync, INCLUDING inactive rows so
     * clients can detect deletions. Does NOT filter on active. @TenantId auto-scopes by workspace.
     */
    @EntityGraph("CbEmployee.basic")
    @Query("SELECT e FROM cb_employee e WHERE e.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<Employee>

    /** Full sync feed (no checkpoint) — INCLUDING inactive. @TenantId auto-scopes by workspace. */
    @EntityGraph("CbEmployee.basic")
    @Query("SELECT e FROM cb_employee e")
    fun findAllForSync(pageable: Pageable): Page<Employee>
}
