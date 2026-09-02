package com.ampairs.cb_employee.service

import com.ampairs.cb_employee.domain.dto.EmployeeRequest
import com.ampairs.cb_employee.domain.dto.EmployeeResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Public service surface for the maintenance org roster. This is the ONLY contract other modules
 * (notably `cb_maintenance`) may depend on — never the repository (rule 08-module-boundaries).
 * Returns DTOs, never JPA entities (rule 02-dto-isolation).
 */
interface EmployeeService {

    fun findByUid(uid: String): EmployeeResponse?

    /** Like [findByUid] but throws when absent — for callers that require the row to exist. */
    fun getByUid(uid: String): EmployeeResponse

    /** Resolve the roster row for a logged-in ampairs user (zone-scoped access checks). */
    fun getByUserId(userId: String): EmployeeResponse?

    /** Assignment pool for a zone: active employees, `role != MAINTENANCE_LEADER`. */
    fun findEligibleAssignees(zonalOfficeId: String): List<EmployeeResponse>

    /** Chain-wide fallback pool when a zone has nobody eligible: active, `role != MAINTENANCE_LEADER`. */
    fun findAllActive(): List<EmployeeResponse>

    /**
     * Walk the `reportsTo` chain to the first ASSISTANT_MANAGER or MAINTENANCE_LEADER — the
     * escalation target. Falls through to the Maintenance Leader where a zone has no AM (module plan §1).
     */
    fun resolveEscalationTarget(employeeId: String): EmployeeResponse

    /** Incremental sync feed — INCLUDING inactive rows so deletions propagate in-band. */
    fun getEmployeesAfterSync(lastSync: String?, pageable: Pageable): Page<EmployeeResponse>

    /** UID-keyed bulk upsert — create when uid absent/unknown, update when present. */
    fun bulkUpsert(requests: List<EmployeeRequest>): List<EmployeeResponse>

    fun create(request: EmployeeRequest): EmployeeResponse
    fun delete(uid: String)
}
