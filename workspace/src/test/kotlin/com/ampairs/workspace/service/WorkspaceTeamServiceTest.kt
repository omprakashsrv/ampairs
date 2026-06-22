package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.WorkspaceTeam
import com.ampairs.workspace.model.dto.AddTeamMembersRequest
import com.ampairs.workspace.model.dto.CreateTeamRequest
import com.ampairs.workspace.model.dto.RemoveTeamMembersRequest
import com.ampairs.workspace.model.dto.UpdateTeamMemberRequest
import com.ampairs.workspace.model.dto.UpdateTeamRequest
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.repository.WorkspaceMemberRepository
import com.ampairs.workspace.repository.WorkspaceTeamRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceTeamServiceTest {

    @Mock
    private lateinit var teamRepository: WorkspaceTeamRepository

    @Mock
    private lateinit var memberRepository: WorkspaceMemberRepository

    @Mock
    private lateinit var memberService: WorkspaceMemberService

    @Mock
    private lateinit var userDetailProvider: UserDetailProvider

    private lateinit var service: WorkspaceTeamService

    private fun team(block: WorkspaceTeam.() -> Unit = {}): WorkspaceTeam =
        WorkspaceTeam().apply {
            uid = "TEAM-1"
            workspaceId = "WSP-1"
            teamCode = "ENG"
            name = "Engineering"
            isActive = true
            createdAt = Instant.now()
            updatedAt = Instant.now()
            block()
        }

    private fun member(block: WorkspaceMember.() -> Unit = {}): WorkspaceMember =
        WorkspaceMember().apply {
            uid = "MBR-1"
            workspaceId = "WSP-1"
            userId = "USR-1"
            role = WorkspaceRole.MEMBER
            isActive = true
            createdAt = Instant.now()
            updatedAt = Instant.now()
            block()
        }

    @BeforeEach
    fun setUp() {
        service = WorkspaceTeamService(teamRepository, memberRepository, memberService, userDetailProvider)
        whenever(teamRepository.save(any<WorkspaceTeam>())).thenAnswer { it.arguments[0] }
        whenever(memberRepository.save(any<WorkspaceMember>())).thenAnswer { it.arguments[0] }
        whenever(userDetailProvider.isUserServiceAvailable()).thenReturn(false)
        whenever(memberRepository.countByTeamIdsContaining(any())).thenReturn(0)
    }

    @Test
    fun `createTeam generates team code from name and saves`() {
        whenever(teamRepository.existsByWorkspaceIdAndTeamCode(any(), any())).thenReturn(false)

        val response = service.createTeam("WSP-1", CreateTeamRequest(name = "Engineering Team"))

        assertEquals("Engineering Team", response.name)
        assertEquals("ENGINEERIN", response.teamCode) // uppercased, stripped, take(10)
    }

    @Test
    fun `createTeam rejects duplicate team code`() {
        whenever(teamRepository.existsByWorkspaceIdAndTeamCode("WSP-1", "ENG")).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            service.createTeam("WSP-1", CreateTeamRequest(name = "Eng", teamCode = "ENG"))
        }
        assertEquals("TEAM_CODE_EXISTS", ex.errorCode)
    }

    @Test
    fun `createTeam rejects non-member team lead`() {
        whenever(teamRepository.existsByWorkspaceIdAndTeamCode(any(), any())).thenReturn(false)
        whenever(memberService.isWorkspaceMember("LEAD")).thenReturn(false)

        val ex = assertThrows(BusinessException::class.java) {
            service.createTeam("WSP-1", CreateTeamRequest(name = "Eng", teamLeadId = "LEAD"))
        }
        assertEquals("INVALID_TEAM_LEAD", ex.errorCode)
    }

    @Test
    fun `updateTeam updates fields`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team()))

        val response = service.updateTeam(
            "WSP-1",
            "TEAM-1",
            UpdateTeamRequest(name = "Renamed", department = "Tech", isActive = false),
        )

        assertEquals("Renamed", response.name)
        assertEquals("Tech", response.department)
        assertFalse(response.isActive)
    }

    @Test
    fun `updateTeam rejects team from another workspace`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team { workspaceId = "OTHER" }))

        val ex = assertThrows(BusinessException::class.java) {
            service.updateTeam("WSP-1", "TEAM-1", UpdateTeamRequest(name = "X"))
        }
        assertEquals("TEAM_NOT_IN_WORKSPACE", ex.errorCode)
    }

    @Test
    fun `getTeamById returns response`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team()))

        val response = service.getTeamById("WSP-1", "TEAM-1")

        assertEquals("ENG", response.teamCode)
    }

    @Test
    fun `getTeamById throws NotFound when missing`() {
        whenever(teamRepository.findById("TEAM-X")).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) { service.getTeamById("WSP-1", "TEAM-X") }
    }

    @Test
    fun `addMembers adds members to team`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team()))
        whenever(memberService.findMemberById("MBR-1")).thenReturn(member())

        val response = service.addMembers("WSP-1", "TEAM-1", AddTeamMembersRequest(memberIds = listOf("MBR-1")))

        assertEquals("Engineering", response.name)
        verify(memberRepository).save(any<WorkspaceMember>())
    }

    @Test
    fun `addMembers rejects when team capacity exceeded`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team { maxMembers = 1 }))
        whenever(memberRepository.countByTeamIdsContaining("TEAM-1")).thenReturn(1)

        val ex = assertThrows(BusinessException::class.java) {
            service.addMembers("WSP-1", "TEAM-1", AddTeamMembersRequest(memberIds = listOf("MBR-1")))
        }
        assertEquals("TEAM_CAPACITY_EXCEEDED", ex.errorCode)
    }

    @Test
    fun `addMembers rejects member from another workspace`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team()))
        whenever(memberService.findMemberById("MBR-1")).thenReturn(member { workspaceId = "OTHER" })

        val ex = assertThrows(BusinessException::class.java) {
            service.addMembers("WSP-1", "TEAM-1", AddTeamMembersRequest(memberIds = listOf("MBR-1")))
        }
        assertEquals("MEMBER_NOT_IN_WORKSPACE", ex.errorCode)
    }

    @Test
    fun `removeMembers removes member from team`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team()))
        whenever(memberService.findMemberById("MBR-1")).thenReturn(member { teamIds = setOf("TEAM-1") })

        service.removeMembers("WSP-1", "TEAM-1", RemoveTeamMembersRequest(memberIds = listOf("MBR-1")))

        verify(memberRepository).save(any<WorkspaceMember>())
    }

    @Test
    fun `updateTeamMember sets primary team`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team()))
        whenever(memberService.findMemberById("MBR-1")).thenReturn(member { teamIds = setOf("TEAM-1") })

        val summary = service.updateTeamMember(
            "WSP-1", "TEAM-1", "MBR-1", UpdateTeamMemberRequest(setAsPrimary = true),
        )

        assertTrue(summary.isPrimaryTeam)
    }

    @Test
    fun `updateTeamMember rejects when not a team member`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team()))
        whenever(memberService.findMemberById("MBR-1")).thenReturn(member { teamIds = emptySet() })

        val ex = assertThrows(BusinessException::class.java) {
            service.updateTeamMember("WSP-1", "TEAM-1", "MBR-1", UpdateTeamMemberRequest(setAsPrimary = true))
        }
        assertEquals("NOT_TEAM_MEMBER", ex.errorCode)
    }

    @Test
    fun `deleteTeam soft-deletes and reassigns primary team`() {
        whenever(teamRepository.findById("TEAM-1")).thenReturn(Optional.of(team()))
        val affected = member { primaryTeamId = "TEAM-1"; teamIds = setOf("TEAM-1", "TEAM-2") }
        whenever(memberRepository.findByWorkspaceIdAndPrimaryTeamId("WSP-1", "TEAM-1")).thenReturn(listOf(affected))

        service.deleteTeam("WSP-1", "TEAM-1")

        verify(teamRepository).save(any<WorkspaceTeam>())
        assertEquals("TEAM-2", affected.primaryTeamId)
    }
}
