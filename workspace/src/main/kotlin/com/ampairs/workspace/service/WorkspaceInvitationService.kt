package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.WorkspaceInvitation
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.InvitationStatus
import com.ampairs.workspace.repository.WorkspaceInvitationRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Service for workspace invitation management operations
 */
@Service
@Transactional
class WorkspaceInvitationService(
    private val invitationRepository: WorkspaceInvitationRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val memberService: WorkspaceMemberService,
    private val activityService: WorkspaceActivityService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceInvitationService::class.java)
        private const val DEFAULT_INVITATION_VALIDITY_DAYS = 7
        private const val MAX_INVITATION_VALIDITY_DAYS = 30
    }

    /**
     * Create workspace invitation
     */
    fun createInvitation(workspaceId: String, request: CreateInvitationRequest, invitedBy: String): InvitationResponse {
        logger.info("Creating invitation for workspace: $workspaceId, email: ${request.email}")

        // Check if there's already a pending invitation
        if (invitationRepository.existsByWorkspaceIdAndEmailAndStatus(
                workspaceId, request.email, InvitationStatus.PENDING
            )
        ) {
            throw BusinessException("INVITATION_ALREADY_EXISTS", "Pending invitation already exists for this email")
        }

        // Validate expiry days
        val expiryDays = request.expiresInDays.coerceIn(1, MAX_INVITATION_VALIDITY_DAYS)
        val expiresAt = LocalDateTime.now().plusDays(expiryDays.toLong())

        // Generate invitation token
        val invitationToken = generateInvitationToken()

        // Create invitation
        val invitation = WorkspaceInvitation().apply {
            this.workspaceId = workspaceId
            this.email = request.email
            this.role = request.role
            this.status = InvitationStatus.PENDING
            this.invitationToken = invitationToken
            this.message = request.message
            this.invitedBy = invitedBy
            this.expiresAt = expiresAt
            this.sendCount = if (request.sendEmail) 1 else 0
            this.lastSentAt = if (request.sendEmail) LocalDateTime.now() else null
        }

        val savedInvitation = invitationRepository.save(invitation)

        // Send email if requested
        if (request.sendEmail) {
            // TODO: Send invitation email
            logger.info("Invitation email would be sent to: ${request.email}")
        }

        // Log activity
        activityService.logInvitationSent(
            workspaceId,
            request.email,
            request.role.name,
            invitedBy,
            "Unknown User",
            savedInvitation.uid
        )

        logger.info("Successfully created invitation: ${savedInvitation.id}")
        return savedInvitation.toResponse()
    }

    /**
     * Accept invitation
     */
    fun acceptInvitation(token: String, userId: String): InvitationResponse {
        val invitation = findInvitationByToken(token)

        // Validate invitation
        validateInvitation(invitation)

        // Check if user is already a member
        if (memberService.isWorkspaceMember(invitation.workspaceId, userId)) {
            throw BusinessException("ALREADY_MEMBER", "You are already a member of this workspace")
        }

        // Add user as member
        memberService.addMember(invitation.workspaceId, userId, invitation.role)

        // Update invitation status
        invitation.status = InvitationStatus.ACCEPTED
        invitation.acceptedAt = LocalDateTime.now()
        val updatedInvitation = invitationRepository.save(invitation)

        // Log activity
        activityService.logInvitationAccepted(
            invitation.workspaceId,
            invitation.email,
            userId,
            "Unknown User",
            invitation.uid
        )

        logger.info("Invitation accepted: ${invitation.id} by user: $userId")
        return updatedInvitation.toResponse()
    }

    /**
     * Decline invitation
     */
    fun declineInvitation(token: String, reason: String?): InvitationResponse {
        val invitation = findInvitationByToken(token)

        // Validate invitation
        validateInvitation(invitation)

        // Update invitation status
        invitation.status = InvitationStatus.DECLINED
        invitation.declinedAt = LocalDateTime.now()
        invitation.cancellationReason = reason
        val updatedInvitation = invitationRepository.save(invitation)

        // Log activity
        activityService.logInvitationDeclined(invitation.workspaceId, invitation.email, reason, invitation.uid)

        logger.info("Invitation declined: ${invitation.id}")
        return updatedInvitation.toResponse()
    }

    /**
     * Resend invitation
     */
    fun resendInvitation(invitationId: String, request: ResendInvitationRequest, resentBy: String): InvitationResponse {
        val invitation = findInvitationById(invitationId)

        // Only pending invitations can be resent
        if (invitation.status != InvitationStatus.PENDING) {
            throw BusinessException("INVITATION_NOT_PENDING", "Only pending invitations can be resent")
        }

        // Update expiry if provided
        if (request.expiresInDays != DEFAULT_INVITATION_VALIDITY_DAYS) {
            val expiryDays = request.expiresInDays.coerceIn(1, MAX_INVITATION_VALIDITY_DAYS)
            invitation.expiresAt = LocalDateTime.now().plusDays(expiryDays.toLong())
        }

        // Update message if provided
        request.message?.let { invitation.message = it }

        // Update send tracking
        invitation.sendCount += 1
        invitation.lastSentAt = LocalDateTime.now()

        val updatedInvitation = invitationRepository.save(invitation)

        // Send email
        // TODO: Send invitation email
        logger.info("Invitation email would be resent to: ${invitation.email}")

        // Log activity
        activityService.logInvitationResent(
            invitation.workspaceId,
            invitation.email,
            resentBy,
            "Unknown User",
            invitation.uid
        )

        logger.info("Invitation resent: ${invitation.id}")
        return updatedInvitation.toResponse()
    }

    /**
     * Cancel invitation
     */
    fun cancelInvitation(invitationId: String, reason: String?, cancelledBy: String): String {
        val invitation = findInvitationById(invitationId)

        // Only pending invitations can be cancelled
        if (invitation.status != InvitationStatus.PENDING) {
            throw BusinessException("INVITATION_NOT_PENDING", "Only pending invitations can be cancelled")
        }

        invitation.status = InvitationStatus.CANCELLED
        invitation.cancelledAt = LocalDateTime.now()
        invitation.cancelledBy = cancelledBy
        invitation.cancellationReason = reason
        invitationRepository.save(invitation)

        // Log activity
        activityService.logInvitationCancelled(
            invitation.workspaceId,
            invitation.email,
            cancelledBy,
            "Unknown User",
            reason,
            invitation.uid
        )

        logger.info("Invitation cancelled: $invitationId")
        return "Invitation cancelled successfully"
    }

    /**
     * Get workspace invitations
     */
    fun getWorkspaceInvitations(
        workspaceId: String,
        status: InvitationStatus?,
        pageable: Pageable,
    ): Page<InvitationListResponse> {
        val invitations = if (status != null) {
            invitationRepository.findByWorkspaceIdAndStatus(workspaceId, status).let { list ->
                // Convert List to Page manually (simplified for this example)
                // In production, you'd want to use repository method with Pageable
                org.springframework.data.domain.PageImpl(list, pageable, list.size.toLong())
            }
        } else {
            invitationRepository.findByWorkspaceId(workspaceId, pageable)
        }

        return invitations.map { it.toListResponse() }
    }

    /**
     * Get invitation by token (for public access)
     */
    fun getPublicInvitation(token: String): PublicInvitationResponse {
        val invitation = findInvitationByToken(token)
        val workspace = workspaceRepository.findById(invitation.workspaceId)
            .orElseThrow { NotFoundException("Workspace not found") }

        return PublicInvitationResponse(
            workspaceName = workspace.name,
            workspaceDescription = workspace.description,
            workspaceAvatarUrl = workspace.avatarUrl,
            inviterName = "Workspace Member", // TODO: Get actual inviter name
            role = invitation.role,
            expiresAt = invitation.expiresAt,
            isExpired = invitation.isExpired(),
            isValid = invitation.status == InvitationStatus.PENDING && !invitation.isExpired()
        )
    }

    /**
     * Get invitation statistics
     */
    fun getInvitationStatistics(workspaceId: String): InvitationStatsResponse {
        val stats = invitationRepository.getInvitationStatistics(workspaceId)
        val recentInvitations = invitationRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(7)
        )

        return InvitationStatsResponse(
            totalInvitations = stats["totalInvitations"] as? Long ?: 0,
            pendingInvitations = stats["pendingInvitations"] as? Long ?: 0,
            acceptedInvitations = stats["acceptedInvitations"] as? Long ?: 0,
            declinedInvitations = stats["declinedInvitations"] as? Long ?: 0,
            expiredInvitations = stats["expiredInvitations"] as? Long ?: 0,
            cancelledInvitations = 0, // Not included in base query
            recentInvitations = recentInvitations
        )
    }

    /**
     * Cancel all pending invitations for workspace
     */
    fun cancelAllPendingInvitations(workspaceId: String, cancelledBy: String, reason: String) {
        val cancelledCount = invitationRepository.cancelAllPendingInvitations(
            workspaceId = workspaceId,
            cancelledAt = LocalDateTime.now(),
            cancelledBy = cancelledBy,
            reason = reason
        )

        logger.info("Cancelled $cancelledCount pending invitations for workspace: $workspaceId")
    }

    /**
     * Delete all invitations for workspace (used during workspace deletion)
     */
    fun deleteAllInvitations(workspaceId: String) {
        val invitations = invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
        invitationRepository.deleteAll(invitations)
        logger.info("Deleted ${invitations.size} invitations for workspace: $workspaceId")
    }

    /**
     * Mark expired invitations
     */
    fun markExpiredInvitations(): Int {
        return invitationRepository.markExpiredInvitations(LocalDateTime.now())
    }

    /**
     * Clean up old invitations
     */
    fun cleanupOldInvitations(daysOld: Int = 30): Int {
        val cleanupDate = LocalDateTime.now().minusDays(daysOld.toLong())
        return invitationRepository.deleteOldInvitations(cleanupDate)
    }

    /**
     * Bulk invitation operations
     */
    fun bulkInvitationOperation(workspaceId: String, request: BulkInvitationRequest, operatedBy: String): String {
        val invitations = invitationRepository.findAllById(request.invitationIds)

        // Validate all invitations belong to workspace
        invitations.forEach { invitation ->
            if (invitation.workspaceId != workspaceId) {
                throw BusinessException("INVALID_INVITATION", "One or more invitations don't belong to this workspace")
            }
        }

        when (request.action) {
            "resend" -> {
                val pendingInvitations = invitations.filter { it.status == InvitationStatus.PENDING }
                pendingInvitations.forEach { invitation ->
                    invitation.sendCount += 1
                    invitation.lastSentAt = LocalDateTime.now()
                }
                invitationRepository.saveAll(pendingInvitations)
                // TODO: Send bulk emails
                activityService.logBulkInvitationResend(
                    workspaceId,
                    pendingInvitations.size,
                    operatedBy,
                    "Unknown User"
                )
            }

            "cancel" -> {
                val pendingInvitations = invitations.filter { it.status == InvitationStatus.PENDING }
                pendingInvitations.forEach { invitation ->
                    invitation.status = InvitationStatus.CANCELLED
                    invitation.cancelledAt = LocalDateTime.now()
                    invitation.cancelledBy = operatedBy
                    invitation.cancellationReason = request.reason
                }
                invitationRepository.saveAll(pendingInvitations)
                activityService.logBulkInvitationCancellation(
                    workspaceId,
                    pendingInvitations.size,
                    operatedBy,
                    "Unknown User"
                )
            }

            "revoke" -> {
                invitations.forEach { invitation ->
                    invitation.status = InvitationStatus.REVOKED
                    invitation.cancelledAt = LocalDateTime.now()
                    invitation.cancelledBy = operatedBy
                    invitation.cancellationReason = request.reason ?: "Bulk revoke operation"
                }
                invitationRepository.saveAll(invitations)
                activityService.logBulkInvitationRevoke(workspaceId, invitations.size, operatedBy, "Unknown User")
            }

            else -> throw BusinessException("INVALID_ACTION", "Invalid bulk action: ${request.action}")
        }

        return "${request.action} operation completed for ${invitations.size} invitations"
    }

    // Private helper methods

    private fun findInvitationById(invitationId: String): WorkspaceInvitation {
        return invitationRepository.findById(invitationId)
            .orElseThrow { NotFoundException("Invitation not found: $invitationId") }
    }

    private fun findInvitationByToken(token: String): WorkspaceInvitation {
        return invitationRepository.findByInvitationToken(token)
            .orElseThrow { NotFoundException("Invalid invitation token") }
    }

    private fun validateInvitation(invitation: WorkspaceInvitation) {
        when {
            invitation.status != InvitationStatus.PENDING ->
                throw BusinessException("INVITATION_NOT_PENDING", "Invitation is not pending")

            invitation.isExpired() ->
                throw BusinessException("INVITATION_EXPIRED", "Invitation has expired")
        }
    }

    private fun generateInvitationToken(): String {
        return UUID.randomUUID().toString().replace("-", "").uppercase()
    }
}