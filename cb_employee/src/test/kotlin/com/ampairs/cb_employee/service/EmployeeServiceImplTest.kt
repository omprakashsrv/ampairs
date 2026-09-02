package com.ampairs.cb_employee.service

import com.ampairs.cb_employee.domain.model.Employee
import com.ampairs.cb_employee.domain.model.MaintenanceRole
import com.ampairs.cb_employee.repository.EmployeeRepository
import com.ampairs.core.sync.EntityChangePublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EmployeeServiceImplTest {

    private val repository: EmployeeRepository = mock()
    private val publisher: EntityChangePublisher = mock()
    private val service = EmployeeServiceImpl(repository, publisher)

    private fun employee(uid: String, role: MaintenanceRole, reportsTo: String? = null) =
        Employee().apply {
            this.uid = uid
            this.role = role
            this.reportsToEmployeeId = reportsTo
            this.name = uid
        }

    @Test
    fun `escalation walk stops at the zone Assistant Manager`() {
        val exec = employee("E1", MaintenanceRole.EXECUTIVE, reportsTo = "AM1")
        val am = employee("AM1", MaintenanceRole.ASSISTANT_MANAGER, reportsTo = "L1")
        whenever(repository.findByUid("E1")).thenReturn(exec)
        whenever(repository.findByUid("AM1")).thenReturn(am)

        val target = service.resolveEscalationTarget("E1")

        assertEquals("AM1", target.uid)
    }

    @Test
    fun `escalation falls through to the Maintenance Leader when a zone has no AM (Pune, Mumbai)`() {
        // Pune/Mumbai: staff report straight to Sanju V P (the Maintenance Leader), no AM in between.
        val exec = employee("PUNE1", MaintenanceRole.EXECUTIVE, reportsTo = "SANJU")
        val leader = employee("SANJU", MaintenanceRole.MAINTENANCE_LEADER, reportsTo = null)
        whenever(repository.findByUid("PUNE1")).thenReturn(exec)
        whenever(repository.findByUid("SANJU")).thenReturn(leader)

        val target = service.resolveEscalationTarget("PUNE1")

        assertEquals("SANJU", target.uid)
    }

    @Test
    fun `escalation walk is guarded against a broken or cyclic reporting chain`() {
        // A -> B -> A cycle; neither is a terminal role. Must terminate, not loop forever.
        val a = employee("A", MaintenanceRole.EXECUTIVE, reportsTo = "B")
        val b = employee("B", MaintenanceRole.SENIOR_EXECUTIVE, reportsTo = "A")
        whenever(repository.findByUid(any())).thenReturn(null)
        whenever(repository.findByUid("A")).thenReturn(a)
        whenever(repository.findByUid("B")).thenReturn(b)

        val target = service.resolveEscalationTarget("A")

        // Best-effort: terminates on the revisit guard and returns a resolvable row instead of hanging.
        assertEquals("A", target.uid)
    }
}
