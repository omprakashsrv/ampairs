package com.ampairs.workspace.service

import com.ampairs.core.domain.User
import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.service.UserService
import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.WorkspaceInvitation
import com.ampairs.workspace.model.dto.CreateInvitationRequest
import com.ampairs.workspace.model.dto.ResendInvitationRequest
import com.ampairs.workspace.model.enums.InvitationStatus
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.repository.WorkspaceInvitationRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceInvitationServiceTest {

    @Mock
    private lateinit var invitationRepository: WorkspaceInvitationRepository

    @Mock
    private lateinit var workspaceRepository: WorkspaceRepository

    @Mock
    private lateinit var memberService: WorkspaceMemberService

    @Mock
    private lateinit var activityService: WorkspaceActivityService

    @Mock
    private lateinit var userService: UserService

    private lateinit var service: WorkspaceInvitationService

    private data class TestUser(
        override val uid: String = "USR-1",
        override val email: String? = "recipient@example.com",
        override val firstName: String? = "Jane",
        override val lastName: String? = "Doe",
        override val phone: String? = null,
        override val isActive: Boolean = true,
        override val profilePictureUrl: String? = null,
    ) : User

    private fun invitation(block: WorkspaceInvitation.() -> Unit = {}): WorkspaceInvitation =
        WorkspaceInvitation().apply {
            uid = "INV-1"
            workspaceId = "WSP-1"
            email = "recipient@example.com"
            role = WorkspaceRole.MEMBER
            status = InvitationStatus.PENDING
            expiresAt = Instant.now().plusSeconds(86400)
            createdAt = Instant.now()
            updatedAt = Instant.now()
            block()
        }

    @BeforeEach
    fun setUp() {
        service = WorkspaceInvitationService(
            invitationRepository,
            workspaceRepository,
            memberService,
            activityService,
            userService,
        )
        whenever(invitationRepository.save(any<WorkspaceInvitation>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createInvitation creates a new email invitation`() {
        whenever(invitationRepository.findByWorkspaceIdAndEmailAndStatus("WSP-1", "new@example.com", InvitationStatus.PENDING))
            .thenReturn(Optional.empty())

        val response = service.createInvitation(
            "WSP-1",
            CreateInvitationRequest(email = "new@example.com", invitedRole = WorkspaceRole.ADMIN, sendNotification = false),
            "USR-9",
        )

        assertEquals("new@example.com", response.email)
        assertEquals(WorkspaceRole.ADMIN, response.role)
        verify(activityService).logInvitationSent(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `createInvitation rejects when neither email nor phone provided`() {
        val ex = assertThrows(BusinessException::class.java) {
            service.createInvitation("WSP-1", CreateInvitationRequest(invitedRole = WorkspaceRole.MEMBER), "USR-9")
        }
        assertEquals("INVALID_CONTACT", ex.errorCode)
    }

    @Test
    fun `createInvitation rejects duplicate pending email invitation`() {
        whenever(invitationRepository.findByWorkspaceIdAndEmailAndStatus("WSP-1", "dup@example.com", InvitationStatus.PENDING))
            .thenReturn(Optional.of(invitation()))

        val ex = assertThrows(BusinessException::class.java) {
            service.createInvitation(
                "WSP-1",
                CreateInvitationRequest(email = "dup@example.com", invitedRole = WorkspaceRole.MEMBER),
                "USR-9",
            )
        }
        assertEquals("INVITATION_ALREADY_EXISTS", ex.errorCode)
    }

    @Test
    fun `acceptInvitationById adds member and marks accepted`() {
        val inv = invitation()
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(inv))
        whenever(userService.getCurrentUser()).thenReturn(TestUser())
        whenever(memberService.isWorkspaceMember("USR-1")).thenReturn(false)
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(Workspace().apply { name = "Acme" }))

        val response = service.acceptInvitationById("INV-1", "USR-1")

        assertEquals(InvitationStatus.ACCEPTED, inv.status)
        assertEquals("Acme", response.workspaceName)
        verify(memberService).addMember("WSP-1", "USR-1", WorkspaceRole.MEMBER)
        verify(activityService).logInvitationAccepted(eq("WSP-1"), any(), eq("USR-1"), any(), any())
    }

    @Test
    fun `acceptInvitationById marks accepted without adding when already a member`() {
        val inv = invitation()
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(inv))
        whenever(userService.getCurrentUser()).thenReturn(TestUser())
        whenever(memberService.isWorkspaceMember("USR-1")).thenReturn(true)
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(Workspace().apply { name = "Acme" }))

        service.acceptInvitationById("INV-1", "USR-1")

        verify(memberService, org.mockito.kotlin.never()).addMember(any(), any(), any())
        assertEquals(InvitationStatus.ACCEPTED, inv.status)
    }

    @Test
    fun `acceptInvitationById throws when invitation not pending`() {
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(invitation { status = InvitationStatus.ACCEPTED }))

        val ex = assertThrows(BusinessException::class.java) { service.acceptInvitationById("INV-1", "USR-1") }
        assertEquals("INVITATION_NOT_PENDING", ex.errorCode)
    }

    @Test
    fun `acceptInvitationById throws when invitation expired`() {
        whenever(invitationRepository.findByUid("INV-1"))
            .thenReturn(Optional.of(invitation { expiresAt = Instant.now().minusSeconds(60) }))

        val ex = assertThrows(BusinessException::class.java) { service.acceptInvitationById("INV-1", "USR-1") }
        assertEquals("INVITATION_EXPIRED", ex.errorCode)
    }

    @Test
    fun `acceptInvitationById rejects when user is not the recipient`() {
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(invitation()))
        whenever(userService.getCurrentUser()).thenReturn(TestUser(email = "someone-else@example.com"))

        val ex = assertThrows(BusinessException::class.java) { service.acceptInvitationById("INV-1", "USR-1") }
        assertEquals("INVITATION_RECIPIENT_MISMATCH", ex.errorCode)
    }

    @Test
    fun `acceptInvitationById rejects when not authenticated`() {
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(invitation()))
        whenever(userService.getCurrentUser()).thenReturn(null)

        val ex = assertThrows(BusinessException::class.java) { service.acceptInvitationById("INV-1", "USR-1") }
        assertEquals("USER_NOT_AUTHENTICATED", ex.errorCode)
    }

    @Test
    fun `declineInvitationById sets declined status`() {
        val inv = invitation()
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(inv))
        whenever(userService.getCurrentUser()).thenReturn(TestUser())
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(Workspace().apply { name = "Acme" }))

        service.declineInvitationById("INV-1", "USR-1", "not interested")

        assertEquals(InvitationStatus.DECLINED, inv.status)
        verify(activityService).logInvitationDeclined(eq("WSP-1"), any(), eq("not interested"), any())
    }

    @Test
    fun `resendInvitation increments send count`() {
        val inv = invitation { sendCount = 1 }
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(inv))

        val response = service.resendInvitation("INV-1", ResendInvitationRequest(expiresInDays = 14, message = "ping"), "USR-9")

        assertEquals(2, inv.sendCount)
        assertEquals(2, response.sendCount)
        verify(activityService).logInvitationResent(eq("WSP-1"), any(), eq("USR-9"), any(), any())
    }

    @Test
    fun `resendInvitation rejects non-pending invitation`() {
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(invitation { status = InvitationStatus.CANCELLED }))

        val ex = assertThrows(BusinessException::class.java) {
            service.resendInvitation("INV-1", ResendInvitationRequest(), "USR-9")
        }
        assertEquals("INVITATION_NOT_PENDING", ex.errorCode)
    }

    @Test
    fun `cancelInvitation cancels a pending invitation`() {
        val inv = invitation()
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(inv))

        val msg = service.cancelInvitation("INV-1", "spam", "USR-9")

        assertEquals("Invitation cancelled successfully", msg)
        assertEquals(InvitationStatus.CANCELLED, inv.status)
        verify(activityService).logInvitationCancelled(eq("WSP-1"), any(), eq("USR-9"), any(), eq("spam"), any())
    }

    @Test
    fun `cancelInvitation rejects non-pending invitation`() {
        whenever(invitationRepository.findByUid("INV-1")).thenReturn(Optional.of(invitation { status = InvitationStatus.ACCEPTED }))

        val ex = assertThrows(BusinessException::class.java) { service.cancelInvitation("INV-1", null, "USR-9") }
        assertEquals("INVITATION_NOT_PENDING", ex.errorCode)
    }

    @Test
    fun `findInvitationById throws NotFound when missing`() {
        whenever(invitationRepository.findByUid("INV-X")).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) { service.findInvitationById("INV-X") }
    }

    @Test
    fun `getWorkspaceInvitations filters by status`() {
        whenever(invitationRepository.findByWorkspaceIdAndStatus("WSP-1", InvitationStatus.PENDING))
            .thenReturn(listOf(invitation()))

        val page = service.getWorkspaceInvitations("WSP-1", InvitationStatus.PENDING, PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
    }

    @Test
    fun `getInvitationStatistics computes acceptance rate`() {
        val invitations = listOf(
            invitation { uid = "I1"; status = InvitationStatus.ACCEPTED },
            invitation { uid = "I2"; status = InvitationStatus.PENDING },
        )
        whenever(invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc("WSP-1")).thenReturn(invitations)

        val stats = service.getInvitationStatistics("WSP-1")

        assertEquals(2, stats["total_invitations"])
        assertEquals(1, stats["accepted_invitations"])
        assertEquals(50, stats["acceptance_rate"])
    }

    @Test
    fun `cancelAllPendingInvitations delegates to repository`() {
        whenever(invitationRepository.cancelAllPendingInvitations(any(), any(), any(), any(), any(), any())).thenReturn(3)

        service.cancelAllPendingInvitations("WSP-1", "USR-9", "archived")

        verify(invitationRepository).cancelAllPendingInvitations(eq("WSP-1"), any(), eq("USR-9"), eq("archived"), any(), any())
    }

    @Test
    fun `deleteAllInvitations deletes all for workspace`() {
        val invs = listOf(invitation())
        whenever(invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc("WSP-1")).thenReturn(invs)

        service.deleteAllInvitations("WSP-1")

        verify(invitationRepository).deleteAll(invs)
    }

    @Test
    fun `bulkCancelInvitations cancels pending and reports failures`() {
        val pending = invitation { uid = "I1" }
        val accepted = invitation { uid = "I2"; status = InvitationStatus.ACCEPTED }
        whenever(invitationRepository.findAllById(listOf("I1", "I2"))).thenReturn(listOf(pending, accepted))

        val result = service.bulkCancelInvitations("WSP-1", listOf("I1", "I2"), "cleanup", "USR-9")

        assertEquals(1, result["cancelled_count"])
        assertEquals(InvitationStatus.CANCELLED, pending.status)
        @Suppress("UNCHECKED_CAST")
        val failures = result["failed_cancellations"] as List<Map<String, String>>
        assertEquals(1, failures.size)
    }

    @Test
    fun `bulkResendInvitations resends pending invitations`() {
        val pending = invitation { uid = "I1"; sendCount = 1 }
        whenever(invitationRepository.findAllById(listOf("I1"))).thenReturn(listOf(pending))

        val result = service.bulkResendInvitations("WSP-1", listOf("I1"), "hi again", "USR-9")

        assertEquals(1, result["resent_count"])
        assertEquals(2, pending.sendCount)
    }

    @Test
    fun `getUserPendingInvitations returns valid invitations`() {
        whenever(userService.getCurrentUser()).thenReturn(TestUser())
        whenever(invitationRepository.findByEmailAndStatus("recipient@example.com", InvitationStatus.PENDING))
            .thenReturn(listOf(invitation()))
        whenever(workspaceRepository.findByUid("WSP-1")).thenReturn(Optional.of(Workspace().apply { name = "Acme" }))

        val result = service.getUserPendingInvitations()

        assertEquals(1, result.size)
        assertEquals("Acme", result.first().workspaceName)
    }

    @Test
    fun `getUserPendingInvitations throws when not authenticated`() {
        whenever(userService.getCurrentUser()).thenReturn(null)

        val ex = assertThrows(BusinessException::class.java) { service.getUserPendingInvitations() }
        assertEquals("USER_NOT_AUTHENTICATED", ex.errorCode)
    }

    @Test
    fun `exportInvitations rejects unsupported format`() {
        whenever(invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc("WSP-1")).thenReturn(emptyList())

        val ex = assertThrows(BusinessException::class.java) {
            service.exportInvitations("WSP-1", "PDF", null, null, null, null, null, null)
        }
        assertEquals("INVALID_FORMAT", ex.errorCode)
    }

    @Test
    fun `exportInvitations produces CSV`() {
        whenever(invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc("WSP-1")).thenReturn(listOf(invitation()))

        val csv = String(service.exportInvitations("WSP-1", "CSV", null, null, null, null, null, null))

        assertTrue(csv.contains("Email,Role,Status"))
    }
}
