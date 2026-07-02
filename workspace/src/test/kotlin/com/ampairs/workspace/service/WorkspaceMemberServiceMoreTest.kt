package com.ampairs.workspace.service

import com.ampairs.core.domain.User
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.dto.UpdateMemberRequest
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.repository.WorkspaceMemberRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import com.ampairs.workspace.security.WorkspacePermission
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceMemberServiceMoreTest {

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

    private data class TestUser(
        override val uid: String = "USR-1",
        override val email: String? = "u@x.com",
        override val firstName: String? = "Jane",
        override val lastName: String? = "Doe",
        override val phone: String? = null,
        override val isActive: Boolean = true,
        override val profilePictureUrl: String? = "pic.png",
    ) : User

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
            memberRepository, workspaceRepository, activityService, userDetailProvider, eventPublisher,
        )
        whenever(memberRepository.save(any<WorkspaceMember>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `updateMember enriches response with user detail when service available`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member()))
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(true)
        whenever(userDetailProvider.getUserDetail("USR-1")).thenReturn(TestUser())

        val response = service.updateMember(
            "WSP-1", "MBR-1",
            UpdateMemberRequest(permissions = setOf(WorkspacePermission.MEMBER_VIEW)),
            "USR-9",
        )

        assertEquals("u@x.com", response.email)
        assertEquals("Jane", response.firstName)
        assertEquals(setOf(WorkspacePermission.MEMBER_VIEW), response.permissions)
    }

    @Test
    fun `updateMember activation publishes activation event`() {
        whenever(memberRepository.findByUid("MBR-1")).thenReturn(Optional.of(member { isActive = false }))
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(false)

        service.updateMember("WSP-1", "MBR-1", UpdateMemberRequest(isActive = true), "USR-9")

        verify(activityService).logMemberActivated(any(), any(), any(), any(), any())
        verify(eventPublisher).publishEvent(any())
    }

    @Test
    fun `getWorkspaceMembers maps with user details when available`() {
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(true)
        whenever(userDetailProvider.getUserDetails(listOf("USR-1"))).thenReturn(mapOf("USR-1" to TestUser()))
        whenever(memberRepository.findByWorkspaceIdAndIsActiveTrue(eq("WSP-1"), any<Pageable>()))
            .thenReturn(PageImpl(listOf(member())))

        val page = service.getWorkspaceMembers("WSP-1", PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        assertEquals("Jane", page.content.first().user?.firstName)
    }

    @Test
    fun `getWorkspaceMembers maps without user details when unavailable`() {
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(false)
        whenever(memberRepository.findByWorkspaceIdAndIsActiveTrue(eq("WSP-1"), any<Pageable>()))
            .thenReturn(PageImpl(listOf(member())))

        val page = service.getWorkspaceMembers("WSP-1", PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
    }

    @Test
    fun `getWorkspaceMembersOptimized batch-loads user details`() {
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(true)
        whenever(userDetailProvider.getUserDetails(listOf("USR-1"))).thenReturn(mapOf("USR-1" to TestUser()))
        whenever(memberRepository.findByIsActiveTrueWithPrimaryTeam(any<Pageable>()))
            .thenReturn(PageImpl(listOf(member())))

        val page = service.getWorkspaceMembersOptimized(PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
    }

    @Test
    fun `getMemberById returns enriched response`() {
        whenever(memberRepository.findByIdWithPrimaryTeam("MBR-1")).thenReturn(Optional.of(member()))
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(true)
        whenever(userDetailProvider.getUserDetail("USR-1")).thenReturn(TestUser())

        val response = service.getMemberById("MBR-1")

        assertEquals("MBR-1", response.id)
        assertEquals("u@x.com", response.email)
    }

    @Test
    fun `getWorkspaceMember returns member with primary team`() {
        whenever(memberRepository.findByUserIdAndIsActiveTrueWithPrimaryTeam("USR-1"))
            .thenReturn(Optional.of(member()))

        assertEquals("MBR-1", service.getWorkspaceMember("USR-1")?.uid)
    }

    @Test
    fun `searchMembers by role`() {
        whenever(memberRepository.findByWorkspaceIdAndRoleAndIsActiveTrue(eq("WSP-1"), eq(WorkspaceRole.ADMIN), any<Pageable>()))
            .thenReturn(PageImpl(listOf(member { role = WorkspaceRole.ADMIN })))

        val page = service.searchMembers("WSP-1", "x", WorkspaceRole.ADMIN, PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
    }

    @Test
    fun `searchMembers without role`() {
        whenever(memberRepository.findByWorkspaceIdAndIsActiveTrueOrderByJoinedAtDesc(eq("WSP-1"), any<Pageable>()))
            .thenReturn(PageImpl(listOf(member())))

        val page = service.searchMembers("WSP-1", "x", null, PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
    }

    @Test
    fun `searchWorkspaceMembers parses role filter`() {
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(false)
        whenever(memberRepository.findByWorkspaceIdAndRoleAndIsActiveTrue(eq("WSP-1"), eq(WorkspaceRole.OWNER), any<Pageable>()))
            .thenReturn(PageImpl(listOf(member { role = WorkspaceRole.OWNER })))

        val page = service.searchWorkspaceMembers("WSP-1", null, "OWNER", null, null, PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
    }

    @Test
    fun `searchWorkspaceMembers ALL role falls back to all active`() {
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(false)
        whenever(memberRepository.findByWorkspaceIdAndIsActiveTrue(eq("WSP-1"), any<Pageable>()))
            .thenReturn(PageImpl(listOf(member())))

        val page = service.searchWorkspaceMembers("WSP-1", "q", "ALL", "ACTIVE", "Eng", PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
    }

    @Test
    fun `getWorkspaceDepartments returns empty list`() {
        assertTrue(service.getWorkspaceDepartments("WSP-1").isEmpty())
    }

    @Test
    fun `updateMemberActivity updates last activity when member found`() {
        whenever(memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrue("WSP-1", "USR-1"))
            .thenReturn(Optional.of(member()))

        service.updateMemberActivity("WSP-1", "USR-1")

        verify(memberRepository).updateLastActivity(eq("MBR-1"), any())
    }

    @Test
    fun `updateMemberActivity does nothing when member missing`() {
        whenever(memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrue("WSP-1", "USR-X"))
            .thenReturn(Optional.empty())

        service.updateMemberActivity("WSP-1", "USR-X")

        verify(memberRepository, org.mockito.kotlin.never()).updateLastActivity(any(), any())
    }

    @Test
    fun `bulkUpdateMembers updates role and status`() {
        whenever(memberRepository.findByUidIn(listOf("M1"))).thenReturn(listOf(member { uid = "M1" }))

        val result = service.bulkUpdateMembers(
            "WSP-1",
            mapOf("member_ids" to listOf("M1"), "role" to "ADMIN", "status" to "INACTIVE"),
        )

        assertEquals(1, result["updated_count"])
    }

    @Test
    fun `bulkUpdateMembers reports invalid role`() {
        whenever(memberRepository.findByUidIn(listOf("M1"))).thenReturn(listOf(member { uid = "M1" }))

        val result = service.bulkUpdateMembers(
            "WSP-1",
            mapOf("member_ids" to listOf("M1"), "role" to "WIZARD"),
        )

        @Suppress("UNCHECKED_CAST")
        val failed = result["failed_updates"] as List<Map<String, String>>
        assertEquals(1, failed.size)
    }

    @Test
    fun `bulkRemoveMembers removes members`() {
        whenever(memberRepository.findByUidIn(listOf("M1"))).thenReturn(listOf(member { uid = "M1" }))
        whenever(memberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue("WSP-1", WorkspaceRole.OWNER)).thenReturn(1L)

        val result = service.bulkRemoveMembers(
            "WSP-1",
            mapOf("member_ids" to listOf("M1"), "reason" to "cleanup"),
        )

        assertEquals(1, result["removed_count"])
    }

    @Test
    fun `bulkRemoveMembers blocks removing all owners`() {
        whenever(memberRepository.findByUidIn(listOf("M1")))
            .thenReturn(listOf(member { uid = "M1"; role = WorkspaceRole.OWNER }))
        whenever(memberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue("WSP-1", WorkspaceRole.OWNER)).thenReturn(1L)

        val result = service.bulkRemoveMembers("WSP-1", mapOf("member_ids" to listOf("M1")))

        assertEquals(0, result["removed_count"])
    }
}
