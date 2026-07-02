package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.dto.CreateWorkspaceRequest
import com.ampairs.workspace.model.dto.UpdateWorkspaceRequest
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceStatus
import com.ampairs.workspace.model.enums.WorkspaceType
import com.ampairs.workspace.repository.WorkspaceMemberRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import com.ampairs.workspace.security.WorkspacePermission
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceServiceTest {

    @Mock
    private lateinit var workspaceRepository: WorkspaceRepository

    @Mock
    private lateinit var workspaceMemberRepository: WorkspaceMemberRepository

    @Mock
    private lateinit var memberService: WorkspaceMemberService

    @Mock
    private lateinit var invitationService: WorkspaceInvitationService

    @Mock
    private lateinit var settingsService: WorkspaceSettingsService

    @Mock
    private lateinit var activityService: WorkspaceActivityService

    private lateinit var service: WorkspaceService

    private fun workspace(block: Workspace.() -> Unit = {}): Workspace =
        Workspace().apply {
            uid = "WSP-1"
            name = "Acme Corp"
            slug = "acme-corp"
            workspaceType = WorkspaceType.BUSINESS
            status = WorkspaceStatus.ACTIVE
            subscriptionPlan = SubscriptionPlan.FREE
            active = true
            createdBy = "USR-1"
            createdAt = Instant.now()
            updatedAt = Instant.now()
            block()
        }

    @BeforeEach
    fun setUp() {
        service = WorkspaceService(
            workspaceRepository,
            workspaceMemberRepository,
            memberService,
            invitationService,
            settingsService,
            activityService,
        )
        whenever(workspaceRepository.save(any<Workspace>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createWorkspace generates slug, saves and wires up dependent services`() {
        whenever(workspaceRepository.existsBySlug(any())).thenReturn(false)

        val request = CreateWorkspaceRequest(name = "Acme Corp", description = "desc", phone = "12345")
        val response = service.createWorkspace(request, "USR-1")

        assertEquals("Acme Corp", response.name)
        assertEquals("acme-corp", response.slug)
        assertEquals(SubscriptionPlan.FREE, response.subscriptionPlan)
        assertEquals(WorkspaceStatus.ACTIVE.name, WorkspaceStatus.ACTIVE.name)
        assertEquals(1, response.memberCount)
        verify(workspaceRepository).save(any<Workspace>())
        verify(settingsService).initializeDefaultSettings(any())
        verify(activityService).logWorkspaceCreated(any(), eq("USR-1"), any())
    }

    @Test
    fun `createWorkspace defaults taxId to GSTIN when not supplied`() {
        whenever(workspaceRepository.existsBySlug(any())).thenReturn(false)
        var saved: Workspace? = null
        whenever(workspaceRepository.save(any<Workspace>())).thenAnswer {
            saved = it.arguments[0] as Workspace
            saved
        }

        service.createWorkspace(CreateWorkspaceRequest(name = "Acme"), "USR-1")

        assertEquals("GSTIN", saved!!.taxId)
    }

    @Test
    fun `createWorkspace slugifies a complex name`() {
        whenever(workspaceRepository.existsBySlug(any())).thenReturn(false)

        val response = service.createWorkspace(CreateWorkspaceRequest(name = "John's Bakery & Cafe!"), "USR-1")

        assertEquals("johns-bakery-cafe", response.slug)
    }

    @Test
    fun `createWorkspace appends suffix when slug already taken`() {
        // First check returns true (taken), then false on the suffixed attempt
        whenever(workspaceRepository.existsBySlug("acme")).thenReturn(true)
        whenever(workspaceRepository.existsBySlug(org.mockito.kotlin.argThat { this != "acme" })).thenReturn(false)

        val response = service.createWorkspace(CreateWorkspaceRequest(name = "Acme", slug = "acme"), "USR-1")

        assertTrue(response.slug.startsWith("acme-"))
    }

    @Test
    fun `getWorkspaceById returns response when user is a member`() {
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(workspace()))
        whenever(memberService.isWorkspaceMember("USR-1")).thenReturn(true)
        whenever(memberService.getActiveMemberCount("WSP-1")).thenReturn(5)

        val response = service.getWorkspaceById("WSP-1", "USR-1")

        assertEquals("WSP-1", response.id)
        assertEquals(5, response.memberCount)
    }

    @Test
    fun `getWorkspaceById throws when user is not a member`() {
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(workspace()))
        whenever(memberService.isWorkspaceMember("USR-1")).thenReturn(false)

        val ex = assertThrows(BusinessException::class.java) {
            service.getWorkspaceById("WSP-1", "USR-1")
        }
        assertEquals("ACCESS_DENIED", ex.errorCode)
    }

    @Test
    fun `getWorkspaceById throws NotFound when workspace missing`() {
        whenever(workspaceRepository.findByUid("WSP-X")).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) {
            service.getWorkspaceById("WSP-X", "USR-1")
        }
    }

    @Test
    fun `getWorkspaceBySlug returns response when member`() {
        whenever(workspaceRepository.findBySlug("acme-corp")).thenReturn(Optional.of(workspace()))
        whenever(memberService.isWorkspaceMember("USR-1")).thenReturn(true)
        whenever(memberService.getActiveMemberCount("WSP-1")).thenReturn(2)

        val response = service.getWorkspaceBySlug("acme-corp", "USR-1")

        assertEquals("acme-corp", response.slug)
        assertEquals(2, response.memberCount)
    }

    @Test
    fun `getWorkspaceBySlug throws NotFound when slug missing`() {
        whenever(workspaceRepository.findBySlug("missing")).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) {
            service.getWorkspaceBySlug("missing", "USR-1")
        }
    }

    @Test
    fun `updateWorkspace updates fields when permitted`() {
        val ws = workspace()
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(ws))
        whenever(memberService.hasPermission("USR-1", WorkspacePermission.WORKSPACE_MANAGE)).thenReturn(true)
        whenever(memberService.getActiveMemberCount("WSP-1")).thenReturn(3)

        val response = service.updateWorkspace(
            "WSP-1",
            UpdateWorkspaceRequest(name = "New Name", city = "Mumbai", currency = "USD"),
            "USR-1",
        )

        assertEquals("New Name", response.name)
        assertEquals("Mumbai", response.city)
        assertEquals("USD", response.currency)
        assertEquals(3, response.memberCount)
        verify(activityService).logWorkspaceUpdated(eq("WSP-1"), eq("USR-1"), any(), anyOrNull())
    }

    @Test
    fun `updateWorkspace throws when lacking permission`() {
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(workspace()))
        whenever(memberService.hasPermission("USR-1", WorkspacePermission.WORKSPACE_MANAGE)).thenReturn(false)

        val ex = assertThrows(BusinessException::class.java) {
            service.updateWorkspace("WSP-1", UpdateWorkspaceRequest(name = "X"), "USR-1")
        }
        assertEquals("INSUFFICIENT_PERMISSIONS", ex.errorCode)
    }

    @Test
    fun `updateWorkspace falls back to repository count when member service throws`() {
        val ws = workspace()
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(ws))
        whenever(memberService.hasPermission("USR-1", WorkspacePermission.WORKSPACE_MANAGE)).thenReturn(true)
        whenever(memberService.getActiveMemberCount("WSP-1")).thenThrow(RuntimeException("db error"))
        whenever(workspaceMemberRepository.countByWorkspaceIdAndIsActiveTrue("WSP-1")).thenReturn(7L)

        val response = service.updateWorkspace("WSP-1", UpdateWorkspaceRequest(name = "X"), "USR-1")

        assertEquals(7, response.memberCount)
    }

    @Test
    fun `getUserWorkspaces maps each workspace with member count`() {
        val page = PageImpl(listOf(workspace()))
        whenever(workspaceRepository.findWorkspacesByUserId(eq("USR-1"), any())).thenReturn(page)
        whenever(memberService.getActiveMemberCount("WSP-1")).thenReturn(4)

        val result = service.getUserWorkspaces("USR-1", PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals(4, result.content.first().memberCount)
    }

    @Test
    fun `searchWorkspaces maps results`() {
        val page = PageImpl(listOf(workspace()))
        whenever(workspaceRepository.searchWorkspaces(eq("acme"), any())).thenReturn(page)
        whenever(memberService.getActiveMemberCount("WSP-1")).thenReturn(1)

        val result = service.searchWorkspaces("acme", null, null, "USR-1", PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals("Acme Corp", result.content.first().name)
    }

    @Test
    fun `archiveWorkspace sets archived status when owner`() {
        val ws = workspace()
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(ws))
        whenever(memberService.isWorkspaceOwner("USR-1")).thenReturn(true)

        val msg = service.archiveWorkspace("WSP-1", "USR-1")

        assertEquals("Workspace archived successfully", msg)
        assertEquals(WorkspaceStatus.ARCHIVED, ws.status)
        assertFalse(ws.active)
        verify(invitationService).cancelAllPendingInvitations(eq("WSP-1"), eq("USR-1"), any())
        verify(activityService).logWorkspaceArchived(eq("WSP-1"), eq("USR-1"), any(), anyOrNull())
    }

    @Test
    fun `archiveWorkspace throws when not owner`() {
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(workspace()))
        whenever(memberService.isWorkspaceOwner("USR-1")).thenReturn(false)

        val ex = assertThrows(BusinessException::class.java) {
            service.archiveWorkspace("WSP-1", "USR-1")
        }
        assertEquals("INSUFFICIENT_PERMISSIONS", ex.errorCode)
    }

    @Test
    fun `deleteWorkspace deletes all related data for free plan owner`() {
        val ws = workspace { subscriptionPlan = SubscriptionPlan.FREE }
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(ws))
        whenever(memberService.isWorkspaceOwner("USR-1")).thenReturn(true)

        val msg = service.deleteWorkspace("WSP-1", "USR-1")

        assertEquals("Workspace deleted permanently", msg)
        verify(memberService).removeAllMembers("WSP-1")
        verify(invitationService).deleteAllInvitations("WSP-1")
        verify(settingsService).deleteSettings("WSP-1")
        verify(activityService).deleteActivities("WSP-1")
        verify(workspaceRepository).delete(ws)
    }

    @Test
    fun `deleteWorkspace throws when not owner`() {
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(workspace()))
        whenever(memberService.isWorkspaceOwner("USR-1")).thenReturn(false)

        val ex = assertThrows(BusinessException::class.java) {
            service.deleteWorkspace("WSP-1", "USR-1")
        }
        assertEquals("INSUFFICIENT_PERMISSIONS", ex.errorCode)
        verify(workspaceRepository, never()).delete(any<Workspace>())
    }

    @Test
    fun `deleteWorkspace throws when subscription not free`() {
        val ws = workspace { subscriptionPlan = SubscriptionPlan.ENTERPRISE }
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(ws))
        whenever(memberService.isWorkspaceOwner("USR-1")).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            service.deleteWorkspace("WSP-1", "USR-1")
        }
        assertEquals("ACTIVE_SUBSCRIPTION", ex.errorCode)
    }

    @Test
    fun `checkSlugAvailability returns true when slug free`() {
        whenever(workspaceRepository.existsBySlug("free-slug")).thenReturn(false)

        assertEquals(mapOf("available" to true), service.checkSlugAvailability("free-slug"))
    }

    @Test
    fun `checkSlugAvailability returns false when slug taken`() {
        whenever(workspaceRepository.existsBySlug("taken")).thenReturn(true)

        assertEquals(mapOf("available" to false), service.checkSlugAvailability("taken"))
    }

    @Test
    fun `getWorkspaceEntity returns the entity`() {
        val ws = workspace()
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(ws))

        assertEquals(ws, service.getWorkspaceEntity("WSP-1"))
    }

    @Test
    fun `getWorkspaceEntity throws when missing`() {
        whenever(workspaceRepository.findByUid("WSP-X")).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) { service.getWorkspaceEntity("WSP-X") }
    }
}
