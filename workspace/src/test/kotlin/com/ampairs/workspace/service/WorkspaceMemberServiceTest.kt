package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.dto.BulkMemberRequest
import com.ampairs.workspace.model.dto.UpdateMemberRequest
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.repository.WorkspaceMemberRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import com.ampairs.workspace.security.WorkspacePermission
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceMemberServiceTest {

    @Mock
    private lateinit var memberRepository: WorkspaceMemberRepository

    @Mock
    private lateinit var workspaceRepository: WorkspaceRepository

    @Mock
    private lateinit var activityService: WorkspaceActivityService

    @Mock
    private lateinit var userDetailProvider: UserDetailProvider

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var service: WorkspaceMemberService

    private fun member(block: WorkspaceMember.() -> Unit = {}): WorkspaceMember =
        WorkspaceMember().apply {
            uid = "MBR-1"
            workspaceId = "WSP-1"
            userId = "USR-1"
            role = WorkspaceRole.MEMBER
            isActive = true
            joinedAt = Instant.now()
            createdAt = Instant.now()
            updatedAt = Instant.now()
            block()
        }

    @BeforeEach
    fun setUp() {
        service = WorkspaceMemberService(
            memberRepository,
            workspaceRepository,
            activityService,
            userDetailProvider,
            eventPublisher,
        )
        whenever(memberRepository.save(any<WorkspaceMember>())).thenAnswer { it.arguments[0] }
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(false)
    }

    @Test
    fun `addMemberAsOwner creates a new owner member`() {
        whenever(memberRepository.findByWorkspaceIdAndUserId("WSP-1", "USR-1")).thenReturn(Optional.empty())

        val result = service.addMemberAsOwner("WSP-1", "USR-1")

        assertEquals(WorkspaceRole.OWNER, result.role)
        assertTrue(result.isActive)
        verify(eventPublisher).publishEvent(any())
    }

    @Test
    fun `addMemberAsOwner promotes an existing non-owner member`() {
        val existing = member { role = WorkspaceRole.MEMBER }
        whenever(memberRepository.findByWorkspaceIdAndUserId("WSP-1", "USR-1")).thenReturn(Optional.of(existing))

        val result = service.addMemberAsOwner("WSP-1", "USR-1")

        assertEquals(WorkspaceRole.OWNER, result.role)
    }

    @Test
    fun `addMemberAsOwner returns existing owner unchanged`() {
        val existing = member { role = WorkspaceRole.OWNER }
        whenever(memberRepository.findByWorkspaceIdAndUserId("WSP-1", "USR-1")).thenReturn(Optional.of(existing))

        val result = service.addMemberAsOwner("WSP-1", "USR-1")

        assertEquals(WorkspaceRole.OWNER, result.role)
        verify(memberRepository, never()).save(any<WorkspaceMember>())
    }

    @Test
    fun `addMember creates a member and logs activity`() {
        whenever(memberRepository.existsByWorkspaceIdAndUserId("WSP-1", "USR-2")).thenReturn(false)

        val result = service.addMember("WSP-1", "USR-2", WorkspaceRole.ADMIN)

        assertEquals(WorkspaceRole.ADMIN, result.role)
        verify(activityService).logMemberAdded(any(), any(), any(), any(), any(), any())
        verify(eventPublisher).publishEvent(any())
    }

    @Test
    fun `addMember throws when already a member`() {
        whenever(memberRepository.existsByWorkspaceIdAndUserId("WSP-1", "USR-2")).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) { service.addMember("WSP-1", "USR-2") }
        assertEquals("MEMBER_ALREADY_EXISTS", ex.errorCode)
    }

    @Test
    fun `updateMember changes role and active flag`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member()))

        val response = service.updateMember(
            "WSP-1",
            "MBR-1",
            UpdateMemberRequest(role = WorkspaceRole.ADMIN, isActive = false),
            "USR-9",
        )

        assertEquals(WorkspaceRole.ADMIN, response.role)
        assertFalse(response.isActive)
        verify(activityService).logMemberRoleChanged(any(), any(), any(), any(), any(), any(), any())
        verify(activityService).logMemberDeactivated(any(), any(), any(), any(), any())
    }

    @Test
    fun `updateMember throws when member belongs to a different workspace`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member { workspaceId = "OTHER" }))

        val ex = assertThrows(BusinessException::class.java) {
            service.updateMember("WSP-1", "MBR-1", UpdateMemberRequest(role = WorkspaceRole.ADMIN), "USR-9")
        }
        assertEquals("MEMBER_NOT_IN_WORKSPACE", ex.errorCode)
    }

    @Test
    fun `removeMember deletes a non-owner`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member()))

        val msg = service.removeMember("WSP-1", "MBR-1", "USR-9")

        assertEquals("Member removed successfully", msg)
        verify(memberRepository).delete(any<WorkspaceMember>())
        verify(eventPublisher).publishEvent(any())
    }

    @Test
    fun `removeMember prevents removing the last owner`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member { role = WorkspaceRole.OWNER }))
        whenever(memberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue("WSP-1", WorkspaceRole.OWNER)).thenReturn(1L)

        val ex = assertThrows(BusinessException::class.java) { service.removeMember("WSP-1", "MBR-1", "USR-9") }
        assertEquals("CANNOT_REMOVE_LAST_OWNER", ex.errorCode)
    }

    @Test
    fun `removeMember allows removing an owner when others remain`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member { role = WorkspaceRole.OWNER }))
        whenever(memberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue("WSP-1", WorkspaceRole.OWNER)).thenReturn(2L)

        service.removeMember("WSP-1", "MBR-1", "USR-9")

        verify(memberRepository).delete(any<WorkspaceMember>())
    }

    @Test
    fun `isWorkspaceMember reflects repository state`() {
        whenever(memberRepository.findByUserIdAndIsActiveTrue("USR-1")).thenReturn(Optional.of(member()))
        assertTrue(service.isWorkspaceMember("USR-1"))

        whenever(memberRepository.findByUserIdAndIsActiveTrue("USR-2")).thenReturn(Optional.empty())
        assertFalse(service.isWorkspaceMember("USR-2"))
    }

    @Test
    fun `isWorkspaceOwner delegates to repository`() {
        whenever(memberRepository.existsByUserIdAndRoleAndIsActiveTrue("USR-1", WorkspaceRole.OWNER)).thenReturn(true)
        assertTrue(service.isWorkspaceOwner("USR-1"))
    }

    @Test
    fun `getUserRole returns role of active member`() {
        whenever(memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrue("WSP-1", "USR-1"))
            .thenReturn(Optional.of(member { role = WorkspaceRole.MANAGER }))

        assertEquals(WorkspaceRole.MANAGER, service.getUserRole("WSP-1", "USR-1"))
    }

    @Test
    fun `getUserRole returns null when not a member`() {
        whenever(memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrue("WSP-1", "USR-X")).thenReturn(Optional.empty())

        assertNull(service.getUserRole("WSP-1", "USR-X"))
    }

    @Test
    fun `hasPermission WORKSPACE_MANAGE true for admin`() {
        whenever(memberRepository.findByUserIdAndIsActiveTrue("USR-1")).thenReturn(Optional.of(member { role = WorkspaceRole.ADMIN }))

        assertTrue(service.hasPermission("USR-1", WorkspacePermission.WORKSPACE_MANAGE))
    }

    @Test
    fun `hasPermission WORKSPACE_MANAGE false for member`() {
        whenever(memberRepository.findByUserIdAndIsActiveTrue("USR-1")).thenReturn(Optional.of(member { role = WorkspaceRole.MEMBER }))

        assertFalse(service.hasPermission("USR-1", WorkspacePermission.WORKSPACE_MANAGE))
    }

    @Test
    fun `hasPermission MEMBER_VIEW true for any member`() {
        whenever(memberRepository.findByUserIdAndIsActiveTrue("USR-1")).thenReturn(Optional.of(member { role = WorkspaceRole.VIEWER }))

        assertTrue(service.hasPermission("USR-1", WorkspacePermission.MEMBER_VIEW))
    }

    @Test
    fun `hasPermission MEMBER_INVITE true for manager but not member`() {
        whenever(memberRepository.findByUserIdAndIsActiveTrue("MGR")).thenReturn(Optional.of(member { role = WorkspaceRole.MANAGER }))
        assertTrue(service.hasPermission("MGR", WorkspacePermission.MEMBER_INVITE))

        whenever(memberRepository.findByUserIdAndIsActiveTrue("MEM")).thenReturn(Optional.of(member { role = WorkspaceRole.MEMBER }))
        assertFalse(service.hasPermission("MEM", WorkspacePermission.MEMBER_INVITE))
    }

    @Test
    fun `hasPermission false when not a member`() {
        whenever(memberRepository.findByUserIdAndIsActiveTrue("USR-X")).thenReturn(Optional.empty())

        assertFalse(service.hasPermission("USR-X", WorkspacePermission.MEMBER_VIEW))
    }

    @Test
    fun `getActiveMemberCount returns count`() {
        whenever(memberRepository.countByWorkspaceIdAndIsActiveTrue("WSP-1")).thenReturn(8L)

        assertEquals(8, service.getActiveMemberCount("WSP-1"))
    }

    @Test
    fun `getActiveMemberCount returns 1 on error`() {
        whenever(memberRepository.countByWorkspaceIdAndIsActiveTrue("WSP-1")).thenThrow(RuntimeException("boom"))

        assertEquals(1, service.getActiveMemberCount("WSP-1"))
    }

    @Test
    fun `getMemberStatistics aggregates counts`() {
        whenever(memberRepository.countByWorkspaceId("WSP-1")).thenReturn(10L)
        whenever(memberRepository.countByWorkspaceIdAndIsActiveTrue("WSP-1")).thenReturn(7L)
        whenever(memberRepository.getMemberCountsByRole("WSP-1")).thenReturn(
            listOf(mapOf("role" to WorkspaceRole.OWNER, "count" to 1L))
        )
        whenever(memberRepository.countByWorkspaceIdAndJoinedAtAfter(eqWsp(), any())).thenReturn(2L)

        val stats = service.getMemberStatistics("WSP-1")

        assertEquals(10L, stats["total_members"])
        assertEquals(7L, stats["active_members"])
        assertEquals(2L, stats["recent_joins"])
        @Suppress("UNCHECKED_CAST")
        val byRole = stats["by_role"] as Map<String, Long>
        assertEquals(1L, byRole["OWNER"])
    }

    private fun eqWsp() = org.mockito.kotlin.eq("WSP-1")

    @Test
    fun `getMembersByRole maps results`() {
        whenever(memberRepository.findByWorkspaceIdAndRole("WSP-1", WorkspaceRole.ADMIN))
            .thenReturn(listOf(member { role = WorkspaceRole.ADMIN }))

        val result = service.getMembersByRole("WSP-1", WorkspaceRole.ADMIN)

        assertEquals(1, result.size)
        assertEquals(WorkspaceRole.ADMIN, result.first().role)
    }

    @Test
    fun `findMemberById throws NotFound when missing`() {
        whenever(memberRepository.findByUid("MBR-X")).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) { service.findMemberById("MBR-X") }
    }

    @Test
    fun `bulkMemberOperation activate sets members active`() {
        val members = listOf(member { uid = "M1" }, member { uid = "M2" })
        whenever(memberRepository.findByUidIn(listOf("M1", "M2"))).thenReturn(members)

        val msg = service.bulkMemberOperation(
            "WSP-1",
            BulkMemberRequest(memberIds = listOf("M1", "M2"), action = "activate"),
            "USR-9",
        )

        assertTrue(msg.contains("activate"))
        verify(memberRepository).saveAll(members)
        verify(activityService).logBulkMemberActivation(any(), any(), any(), any())
    }

    @Test
    fun `bulkMemberOperation rejects member from another workspace`() {
        whenever(memberRepository.findByUidIn(listOf("M1"))).thenReturn(listOf(member { uid = "M1"; workspaceId = "OTHER" }))

        val ex = assertThrows(BusinessException::class.java) {
            service.bulkMemberOperation("WSP-1", BulkMemberRequest(listOf("M1"), "activate"), "USR-9")
        }
        assertEquals("INVALID_MEMBER", ex.errorCode)
    }

    @Test
    fun `bulkMemberOperation remove prevents removing all owners`() {
        val owners = listOf(member { uid = "M1"; role = WorkspaceRole.OWNER })
        whenever(memberRepository.findByUidIn(listOf("M1"))).thenReturn(owners)
        whenever(memberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue("WSP-1", WorkspaceRole.OWNER)).thenReturn(1L)

        val ex = assertThrows(BusinessException::class.java) {
            service.bulkMemberOperation("WSP-1", BulkMemberRequest(listOf("M1"), "remove"), "USR-9")
        }
        assertEquals("CANNOT_REMOVE_ALL_OWNERS", ex.errorCode)
    }

    @Test
    fun `bulkMemberOperation update_role requires a role`() {
        whenever(memberRepository.findByUidIn(listOf("M1"))).thenReturn(listOf(member { uid = "M1" }))

        val ex = assertThrows(BusinessException::class.java) {
            service.bulkMemberOperation("WSP-1", BulkMemberRequest(listOf("M1"), "update_role", role = null), "USR-9")
        }
        assertEquals("ROLE_REQUIRED", ex.errorCode)
    }

    @Test
    fun `bulkMemberOperation invalid action throws`() {
        whenever(memberRepository.findByUidIn(listOf("M1"))).thenReturn(listOf(member { uid = "M1" }))

        val ex = assertThrows(BusinessException::class.java) {
            service.bulkMemberOperation("WSP-1", BulkMemberRequest(listOf("M1"), "fly"), "USR-9")
        }
        assertEquals("INVALID_ACTION", ex.errorCode)
    }

    @Test
    fun `updateMemberStatus suspends a member`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member()))

        val response = service.updateMemberStatus("WSP-1", "MBR-1", "SUSPENDED", "policy")

        assertFalse(response.isActive)
    }

    @Test
    fun `updateMemberStatus rejects invalid status`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member()))

        val ex = assertThrows(BusinessException::class.java) {
            service.updateMemberStatus("WSP-1", "MBR-1", "BOGUS", null)
        }
        assertEquals("INVALID_STATUS", ex.errorCode)
    }

    @Test
    fun `removeAllMembers delegates to repository`() {
        service.removeAllMembers("WSP-1")
        verify(memberRepository).deleteByWorkspaceId("WSP-1")
    }

    @Test
    fun `exportMembers produces CSV with header`() {
        whenever(memberRepository.findByWorkspaceIdOrderByCreatedAtDesc("WSP-1")).thenReturn(listOf(member()))

        val csv = String(service.exportMembers("WSP-1", "CSV", null, null, null, null))

        assertTrue(csv.startsWith("Name,Email,Role,Status,Joined Date,Last Activity"))
        assertTrue(csv.contains("USR-1"))
    }

    @Test
    fun `findBlockingWorkspacesForDeletion returns sole-owner workspaces`() {
        val ws = com.ampairs.workspace.model.Workspace().apply {
            uid = "WSP-1"; name = "Acme"; slug = "acme"
        }
        whenever(workspaceRepository.findAllWorkspacesByUserIdCrossTenant("USR-1")).thenReturn(listOf(ws))
        whenever(memberRepository.findAllByUserIdCrossTenant("USR-1"))
            .thenReturn(listOf(member { role = WorkspaceRole.OWNER; isActive = true }))
        whenever(memberRepository.countActiveOwnersCrossTenant("WSP-1")).thenReturn(1L)
        whenever(memberRepository.countActiveMembersCrossTenant("WSP-1")).thenReturn(3L)

        val result = service.findBlockingWorkspacesForDeletion("USR-1")

        assertEquals(1, result.size)
        assertEquals("Acme", result.first().workspaceName)
        assertEquals(3L, result.first().memberCount)
    }

    @Test
    fun `findBlockingWorkspacesForDeletion skips when more than one owner`() {
        whenever(workspaceRepository.findAllWorkspacesByUserIdCrossTenant("USR-1")).thenReturn(emptyList())
        whenever(memberRepository.findAllByUserIdCrossTenant("USR-1"))
            .thenReturn(listOf(member { role = WorkspaceRole.OWNER; isActive = true }))
        whenever(memberRepository.countActiveOwnersCrossTenant("WSP-1")).thenReturn(2L)

        assertTrue(service.findBlockingWorkspacesForDeletion("USR-1").isEmpty())
    }

    @Test
    fun `revokeAndDeleteMembershipsForUser deactivates and deletes`() {
        val memberships = listOf(member())
        whenever(memberRepository.findAllByUserIdCrossTenant("USR-1")).thenReturn(memberships)

        service.revokeAndDeleteMembershipsForUser("USR-1")

        verify(memberRepository).saveAll(memberships)
        verify(memberRepository).deleteAll(memberships)
    }
}
