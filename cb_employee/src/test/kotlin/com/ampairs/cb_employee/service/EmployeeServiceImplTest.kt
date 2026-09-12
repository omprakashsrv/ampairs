package com.ampairs.cb_employee.service

import com.ampairs.cb_employee.domain.dto.EmployeeRequest
import com.ampairs.cb_employee.domain.model.Employee
import com.ampairs.cb_employee.domain.model.MaintenanceRole
import com.ampairs.cb_employee.repository.EmployeeRepository
import com.ampairs.core.domain.User
import com.ampairs.core.service.UserService
import com.ampairs.core.sync.EntityChangePublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EmployeeServiceImplTest {

    private val repository: EmployeeRepository = mock()
    private val publisher: EntityChangePublisher = mock()
    private val userService: UserService = mock()
    private val service = EmployeeServiceImpl(repository, publisher, userService)

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
    fun `escalation falls through to the Maintenance Leader when a zone has no AM`() {
        // In a zone with no Assistant Manager, staff report straight to the Maintenance Leader.
        val exec = employee("E2", MaintenanceRole.EXECUTIVE, reportsTo = "LEADER1")
        val leader = employee("LEADER1", MaintenanceRole.MAINTENANCE_LEADER, reportsTo = null)
        whenever(repository.findByUid("E2")).thenReturn(exec)
        whenever(repository.findByUid("LEADER1")).thenReturn(leader)

        val target = service.resolveEscalationTarget("E2")

        assertEquals("LEADER1", target.uid)
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

    private fun request(mobile: String?, userId: String? = null) =
        EmployeeRequest(uid = "EMP1", employeeNo = "E1", name = "Nom", mobile = mobile, userId = userId)

    @Test
    fun `bulkUpsert links user_id by mobile when the client did not supply one`() {
        val user: User = mock()
        whenever(user.uid).thenReturn("USR-9")
        whenever(userService.getUserByPhone("9739810010")).thenReturn(user)
        whenever(repository.findByUid("EMP1")).thenReturn(null)
        whenever(repository.save(any<Employee>())).thenAnswer { it.arguments[0] as Employee }

        val result = service.bulkUpsert(listOf(request(mobile = "9739810010")))

        assertEquals("USR-9", result.single().userId)
    }

    @Test
    fun `bulkUpsert leaves user_id null when no login matches the mobile`() {
        whenever(userService.getUserByPhone(any())).thenReturn(null)
        whenever(repository.findByUid("EMP1")).thenReturn(null)
        whenever(repository.save(any<Employee>())).thenAnswer { it.arguments[0] as Employee }

        val result = service.bulkUpsert(listOf(request(mobile = "0000000000")))

        assertNull(result.single().userId)
    }

    @Test
    fun `bulkUpsert does not override a user_id the client already provided`() {
        whenever(repository.findByUid("EMP1")).thenReturn(null)
        whenever(repository.save(any<Employee>())).thenAnswer { it.arguments[0] as Employee }

        val result = service.bulkUpsert(listOf(request(mobile = "9739810010", userId = "USR-EXPLICIT")))

        assertEquals("USR-EXPLICIT", result.single().userId)
    }
}
