package com.ampairs.cb_maintenance.service

import com.ampairs.cb_employee.domain.dto.EmployeeResponse
import com.ampairs.cb_employee.service.EmployeeService
import com.ampairs.cb_maintenance.domain.model.PmEntryStatus
import com.ampairs.cb_maintenance.domain.model.TicketStatus
import com.ampairs.cb_maintenance.repository.PmEntryRepository
import com.ampairs.cb_maintenance.repository.TicketRepository
import org.springframework.stereotype.Service

/**
 * Assignment pool + load-balancing (module plan §4). Eligibility comes from cb_employee; the
 * fewest-open-work pick is computed locally against cb_maintenance's own tables.
 */
@Service
class AssignmentService(
    private val employeeService: EmployeeService,
    private val pmEntryRepository: PmEntryRepository,
    private val ticketRepository: TicketRepository,
) {

    companion object {
        val OPEN_PM_STATUSES = listOf(
            PmEntryStatus.DUE, PmEntryStatus.OVERDUE, PmEntryStatus.ASSIGNED, PmEntryStatus.IN_PROGRESS,
        )
        val OPEN_TICKET_STATUSES = listOf(
            TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD,
        )
    }

    /** The eligible pool for a zone, with the §4.3 chain-wide fallback when the zone has nobody. */
    fun eligiblePool(zonalOfficeId: String): List<EmployeeResponse> =
        employeeService.findEligibleAssignees(zonalOfficeId).ifEmpty { employeeService.findAllActive() }

    /** Pick the eligible assignee in a zone with the fewest open PM entries + tickets. */
    fun pickAssignee(zonalOfficeId: String): String? =
        eligiblePool(zonalOfficeId).minByOrNull { openWorkload(it.uid) }?.uid

    fun isValidAssignee(zonalOfficeId: String, employeeId: String): Boolean =
        eligiblePool(zonalOfficeId).any { it.uid == employeeId }

    fun openWorkload(employeeId: String): Long =
        pmEntryRepository.countByAssignedToEmployeeIdAndStatusIn(employeeId, OPEN_PM_STATUSES) +
            ticketRepository.countByAssignedToEmployeeIdAndStatusIn(employeeId, OPEN_TICKET_STATUSES)
}
