package com.ampairs.cb_maintenance.service

import com.ampairs.cb_employee.domain.dto.EmployeeResponse
import com.ampairs.cb_employee.domain.model.MaintenanceRole
import com.ampairs.cb_employee.service.EmployeeService
import com.ampairs.core.service.UserService
import org.springframework.stereotype.Service

/**
 * Zone-scoped access control (module plan §5) — a second, finer boundary INSIDE a single tenant.
 * Resolves the calling employee from the authenticated user and enforces per-record zone visibility.
 */
@Service
class MaintenanceAccessService(
    private val userService: UserService,
    private val employeeService: EmployeeService,
) {

    /** The maintenance roster row for the current authenticated user, or null if none. */
    fun currentEmployee(): EmployeeResponse? {
        val userId = userService.getCurrentUserId()?.takeIf { it.isNotBlank() } ?: return null
        return employeeService.getByUserId(userId)
    }

    fun requireCurrentEmployee(): EmployeeResponse =
        currentEmployee()
            ?: throw MaintenanceAccessException("Caller is not a registered maintenance employee")

    /**
     * The zonalOfficeId a caller's sync feed / list reads should be filtered to. `null` means "all
     * zones" — only the MAINTENANCE_LEADER (HQ) sees everything.
     */
    fun effectiveZoneFilter(caller: EmployeeResponse): String? =
        if (caller.role == MaintenanceRole.MAINTENANCE_LEADER) null else caller.zonalOfficeId

    /**
     * Zone filter for READ/sync feeds, resolved WITHOUT requiring the caller to be a maintenance
     * employee. A workspace admin/owner (no roster row) or a MAINTENANCE_LEADER reads all zones
     * (`null`); a zoned field employee reads only their own zone. Workspace membership already gates
     * the tenant, so a non-roster caller seeing all zones is the intended "HQ view" — this is what
     * lets the owner see generated PM entries / tickets without being added to the roster first.
     */
    fun readZoneFilter(): String? = currentEmployee()?.let { effectiveZoneFilter(it) }

    /**
     * Assert the caller may see/act on a record in [recordZone]. MAINTENANCE_LEADER passes always;
     * same-zone passes; and the §4.3 free-flow case (unassigned record whose local pool is empty)
     * is visible chain-wide.
     */
    fun assertZoneAccess(caller: EmployeeResponse, recordZone: String, assignedToEmployeeId: String?) {
        if (caller.role == MaintenanceRole.MAINTENANCE_LEADER) return
        if (caller.zonalOfficeId != null && caller.zonalOfficeId == recordZone) return
        if (assignedToEmployeeId == null && employeeService.findEligibleAssignees(recordZone).isEmpty()) return
        throw MaintenanceAccessException("No access to this zone")
    }
}
