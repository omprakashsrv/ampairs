package com.ampairs.workspace.service

import com.ampairs.workspace.model.WorkspaceActivity
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.repository.WorkspaceActivityRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceActivityServiceTest {

    @Mock
    private lateinit var activityRepository: WorkspaceActivityRepository

    private lateinit var service: WorkspaceActivityService

    @BeforeEach
    fun setUp() {
        // Use a real ObjectMapper so contextData serialization is genuinely exercised.
        service = WorkspaceActivityService(activityRepository, ObjectMapper())
        whenever(activityRepository.save(any<WorkspaceActivity>())).thenAnswer { it.arguments[0] }
    }

    private fun captureSaved(): WorkspaceActivity {
        val captor = ArgumentCaptor.forClass(WorkspaceActivity::class.java)
        verify(activityRepository).save(captor.capture())
        return captor.value
    }

    @Test
    fun `logWorkspaceCreated builds and saves an activity`() {
        service.logWorkspaceCreated("WSP-1", "USR-1", "Jane")

        val saved = captureSaved()
        assertEquals("WSP-1", saved.workspaceId)
        assertEquals("USR-1", saved.actorId)
        assertEquals("INFO", saved.severity)
    }

    @Test
    fun `logWorkspaceUpdated serializes changes into context data`() {
        service.logWorkspaceUpdated("WSP-1", "USR-1", "Jane", mapOf("name" to "New"))

        val saved = captureSaved()
        assertTrue(saved.contextData!!.contains("name"))
    }

    @Test
    fun `logWorkspaceArchived uses WARN severity`() {
        service.logWorkspaceArchived("WSP-1", "USR-1", "Jane", "no longer needed")

        assertEquals("WARN", captureSaved().severity)
    }

    @Test
    fun `logSubscriptionUpdated records old and new plan`() {
        service.logSubscriptionUpdated("WSP-1", "USR-1", "Jane", SubscriptionPlan.ENTERPRISE, SubscriptionPlan.FREE)

        val saved = captureSaved()
        assertTrue(saved.contextData!!.contains("ENTERPRISE"))
        assertTrue(saved.contextData!!.contains("FREE"))
    }

    @Test
    fun `logMemberAdded captures role`() {
        service.logMemberAdded("WSP-1", "USR-2", "Bob", "ADMIN", "USR-1", "Jane")

        assertTrue(captureSaved().contextData!!.contains("ADMIN"))
    }

    @Test
    fun `logMemberRoleChanged captures old and new role`() {
        service.logMemberRoleChanged("WSP-1", "USR-2", "Bob", "MEMBER", "ADMIN", "USR-1", "Jane")

        val saved = captureSaved()
        assertTrue(saved.contextData!!.contains("MEMBER"))
        assertTrue(saved.contextData!!.contains("ADMIN"))
    }

    @Test
    fun `logMemberActivated and logMemberDeactivated both save`() {
        service.logMemberActivated("WSP-1", "USR-2", "Bob", "USR-1", "Jane")
        service.logMemberDeactivated("WSP-1", "USR-2", "Bob", "USR-1", "Jane")

        verify(activityRepository, times(2)).save(any<WorkspaceActivity>())
    }

    @Test
    fun `logMemberRemoved with reason saves`() {
        service.logMemberRemoved("WSP-1", "USR-2", "Bob", "USR-1", "Jane", "left company")

        assertTrue(captureSaved().contextData!!.contains("left company"))
    }

    @Test
    fun `bulk member operations all save`() {
        service.logBulkMemberActivation("WSP-1", 3, "USR-1", "Jane")
        service.logBulkMemberDeactivation("WSP-1", 2, "USR-1", "Jane")
        service.logBulkMemberRemoval("WSP-1", 4, "USR-1", "Jane", "cleanup")
        service.logBulkRoleUpdate("WSP-1", 5, "ADMIN", "USR-1", "Jane", "MEMBER")

        verify(activityRepository, times(4)).save(any<WorkspaceActivity>())
    }

    @Test
    fun `invitation log methods all save`() {
        service.logInvitationSent("WSP-1", "a@x.com", "MEMBER", "USR-1", "Jane", "INV-1")
        service.logInvitationAccepted("WSP-1", "a@x.com", "USR-2", "Bob", "INV-1")
        service.logInvitationDeclined("WSP-1", "a@x.com", "not interested", "INV-1")
        service.logInvitationResent("WSP-1", "a@x.com", "USR-1", "Jane", "INV-1")
        service.logInvitationCancelled("WSP-1", "a@x.com", "USR-1", "Jane", "spam", "INV-1")

        verify(activityRepository, times(5)).save(any<WorkspaceActivity>())
    }

    @Test
    fun `bulk invitation log methods all save`() {
        service.logBulkInvitationResend("WSP-1", 3, "USR-1", "Jane")
        service.logBulkInvitationCancellation("WSP-1", 2, "USR-1", "Jane")
        service.logBulkInvitationRevoke("WSP-1", 1, "USR-1", "Jane")

        verify(activityRepository, times(3)).save(any<WorkspaceActivity>())
    }

    @Test
    fun `settings log methods save`() {
        service.logSettingsUpdated("WSP-1", "material_theme", "USR-1", "Jane", mapOf("mode" to "dark"))
        service.logSettingsReset("WSP-1", "material_theme", "USR-1", "Jane")

        verify(activityRepository, times(2)).save(any<WorkspaceActivity>())
    }

    @Test
    fun `log methods swallow repository exceptions`() {
        whenever(activityRepository.save(any<WorkspaceActivity>())).thenThrow(RuntimeException("db down"))

        // Should not throw — failures are logged, not propagated
        service.logWorkspaceCreated("WSP-1", "USR-1", "Jane")
    }

    @Test
    fun `getActivityStatistics aggregates repository data`() {
        whenever(activityRepository.getActivityStatsByType(eq("WSP-1"), any()))
            .thenReturn(listOf(arrayOf<Any>("MEMBER_ADDED", 3L)))
        whenever(activityRepository.getMostActiveUsers(eq("WSP-1"), any(), any()))
            .thenReturn(listOf(arrayOf<Any>("USR-1", "Jane", 5L)))
        whenever(activityRepository.countByWorkspaceIdAndCreatedAtBetween(eq("WSP-1"), any(), any())).thenReturn(8L)
        whenever(activityRepository.getRecentActivities(eq("WSP-1"), any(), any())).thenReturn(emptyList())

        val stats = service.getActivityStatistics("WSP-1", 30)

        assertEquals(8L, stats["total_activities"])
        assertEquals(30, stats["period_days"])
        @Suppress("UNCHECKED_CAST")
        val byType = stats["activity_by_type"] as Map<String, Long>
        assertEquals(3L, byType["MEMBER_ADDED"])
    }

    @Test
    fun `getActivityStatistics returns error map on failure`() {
        whenever(activityRepository.getActivityStatsByType(any(), any())).thenThrow(RuntimeException("boom"))

        val stats = service.getActivityStatistics("WSP-1")

        assertEquals("Failed to retrieve statistics", stats["error"])
    }

    @Test
    fun `deleteActivities delegates to repository`() {
        whenever(activityRepository.deleteByWorkspaceId("WSP-1")).thenReturn(5L)

        service.deleteActivities("WSP-1")

        verify(activityRepository).deleteByWorkspaceId("WSP-1")
    }

    @Test
    fun `getWorkspaceActivities returns page`() {
        val page = PageImpl(listOf(WorkspaceActivity().apply { workspaceId = "WSP-1" }))
        whenever(activityRepository.findByWorkspaceIdOrderByCreatedAtDesc(eq("WSP-1"), any<Pageable>())).thenReturn(page)

        val result = service.getWorkspaceActivities("WSP-1", PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `searchWorkspaceActivities delegates to repository`() {
        whenever(activityRepository.searchActivities(eq("WSP-1"), eq("member"), any<Pageable>()))
            .thenReturn(PageImpl(emptyList()))

        val result = service.searchWorkspaceActivities("WSP-1", "member", PageRequest.of(0, 10))

        assertEquals(0, result.totalElements)
    }

    @Test
    fun `getEntityActivityTimeline returns empty list on failure`() {
        whenever(activityRepository.getEntityTimeline(any(), any(), any())).thenThrow(RuntimeException("boom"))

        assertTrue(service.getEntityActivityTimeline("WSP-1", "member", "MBR-1").isEmpty())
    }
}
