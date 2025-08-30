package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.WorkspaceInvitation
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.InvitationStatus
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.repository.WorkspaceInvitationRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
        // Validate request has either email or phone
        if (!request.isValid()) {
            throw BusinessException("INVALID_CONTACT", "Either email or phone number must be provided")
        }

        val contactType = request.getContactType()
        val contactValue = request.getContactValue()

        logger.info("Creating invitation for workspace: $workspaceId, $contactType: $contactValue")

        // Check if there's already a pending invitation
        val existingInvitation = when (contactType) {
            "email" -> invitationRepository.findByWorkspaceIdAndEmailAndStatus(
                workspaceId, request.recipientEmail!!, InvitationStatus.PENDING
            ).orElse(null)
            "phone" -> invitationRepository.findByWorkspaceIdAndPhoneAndStatus(
                workspaceId, request.recipientPhone, InvitationStatus.PENDING
            )
            else -> null
        }

        if (existingInvitation != null) {
            throw BusinessException(
                "INVITATION_ALREADY_EXISTS",
                "Pending invitation already exists for this ${contactType}"
            )
        }

        // Create new invitation
        val invitation = WorkspaceInvitation().apply {
            this.workspaceId = workspaceId
            this.email = request.recipientEmail
            this.phone = request.recipientPhone
            this.role = WorkspaceRole.valueOf(request.invitedRole.uppercase())
            this.message = request.customMessage
            this.invitedBy = invitedBy
            this.department = request.department
            this.teamIds = request.teamIds ?: setOf()
            this.primaryTeamId = request.primaryTeamId
            this.expiresAt = LocalDateTime.now().plusDays(
                (request.expiresInDays ?: DEFAULT_INVITATION_VALIDITY_DAYS)
                    .coerceIn(1, MAX_INVITATION_VALIDITY_DAYS)
                    .toLong()
            )
        }

        val saved = invitationRepository.save(invitation)

        // Log activity
        activityService.logInvitationSent(
            workspaceId = workspaceId,
            email = contactValue,
            role = request.invitedRole,
            invitedBy = invitedBy,
            invitedByName = "Unknown User",
            invitationId = saved.uid
        )

        // Send notification if requested
        if (request.sendNotification) {
            when (contactType) {
                "email" -> sendEmailInvitation(saved)
                "phone" -> sendPhoneInvitation(saved)
            }
        }

        return saved.toResponse()
    }

    private fun sendEmailInvitation(invitation: WorkspaceInvitation) {
        // Existing email notification logic
    }

    private fun sendPhoneInvitation(invitation: WorkspaceInvitation) {
        // Send SMS/WhatsApp notification using notification service
        // Implementation depends on your notification service
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
            invitation.email ?: "",
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
        invitation.rejectedAt = LocalDateTime.now()
        invitation.rejectionReason = reason
        val updatedInvitation = invitationRepository.save(invitation)

        // Log activity
        activityService.logInvitationDeclined(invitation.workspaceId, invitation.email ?: "", reason, invitation.uid)

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
            invitation.email ?: "",
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
            invitation.email ?: "",
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
            isExpired = LocalDateTime.now().isAfter(invitation.expiresAt),
            isValid = invitation.status == InvitationStatus.PENDING && !LocalDateTime.now().isAfter(invitation.expiresAt)
        )
    }

    /**
     * Search workspace invitations with advanced filtering
     */
    fun searchWorkspaceInvitations(
        workspaceId: String,
        pageable: Pageable,
        status: String?,
        role: String?,
        deliveryStatus: String?,
        searchQuery: String?,
        startDate: String?,
        endDate: String?
    ): Page<InvitationListResponse> {
        // Parse dates if provided
        val startDateTime = startDate?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        val endDateTime = endDate?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }

        // Convert status string to enum if provided
        val invitationStatus = status?.let {
            try {
                InvitationStatus.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        // For now, use the basic repository method and filter in memory
        // In production, you'd want to add custom query methods to the repository
        val allInvitations = if (invitationStatus != null) {
            invitationRepository.findByWorkspaceIdAndStatus(workspaceId, invitationStatus)
        } else {
            invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
        }

        // Apply additional filtering
        val filteredInvitations = allInvitations.filter { invitation ->
            var matches = true

            // Role filter
            if (role != null && role != "ALL") {
                matches = matches && invitation.role.name.equals(role, ignoreCase = true)
            }

            // Search query filter (email or message)
            if (!searchQuery.isNullOrBlank()) {
                matches = matches && (
                        invitation.email?.contains(searchQuery, ignoreCase = true) == true ||
                                invitation.message?.contains(searchQuery, ignoreCase = true) == true
                        )
            }

            // Date range filter
            if (startDateTime != null) {
                matches = matches && (invitation.createdAt?.isAfter(startDateTime) == true)
            }
            if (endDateTime != null) {
                matches = matches && (invitation.createdAt?.isBefore(endDateTime) == true)
            }

            matches
        }

        // Convert to page
        val startIndex = pageable.offset.toInt()
        val endIndex = minOf(startIndex + pageable.pageSize, filteredInvitations.size)
        val pageContent = if (startIndex < filteredInvitations.size) {
            filteredInvitations.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        val page = org.springframework.data.domain.PageImpl(
            pageContent,
            pageable,
            filteredInvitations.size.toLong()
        )

        return page.map { it.toListResponse() }
    }

    /**
     * Get invitation statistics with enhanced data
     */
    fun getInvitationStatistics(workspaceId: String): Map<String, Any> {
        val allInvitations = invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
        val now = LocalDateTime.now()
        val lastWeek = now.minusDays(7)
        now.minusDays(30)

        // Calculate statistics
        val totalInvitations = allInvitations.size
        val pendingInvitations = allInvitations.count { it.status == InvitationStatus.PENDING && !LocalDateTime.now().isAfter(it.expiresAt) }
        val acceptedInvitations = allInvitations.count { it.status == InvitationStatus.ACCEPTED }
        val declinedInvitations = allInvitations.count { it.status == InvitationStatus.DECLINED }
        val expiredInvitations = allInvitations.count { LocalDateTime.now().isAfter(it.expiresAt) || it.status == InvitationStatus.EXPIRED }
        val cancelledInvitations = allInvitations.count { it.status == InvitationStatus.CANCELLED }
        val recentInvitations = allInvitations.count { it.createdAt?.isAfter(lastWeek) == true }

        // Group by role
        val byRole = allInvitations.groupBy { it.role.name }
            .mapValues { it.value.size }

        // Group by status
        val byStatus = allInvitations.groupBy { it.status.name }
            .mapValues { it.value.size }

        // Recent activity (last 30 days, grouped by day)
        val recentActivity = (0 until 30).map { daysAgo ->
            val date = now.minusDays(daysAgo.toLong())
            val dayStart = date.toLocalDate().atStartOfDay()
            val dayEnd = dayStart.plusDays(1)

            val dayInvitations = allInvitations.filter {
                it.createdAt?.isAfter(dayStart) == true && it.createdAt?.isBefore(dayEnd) == true
            }

            mapOf(
                "date" to date.toLocalDate().toString(),
                "sent" to dayInvitations.size,
                "accepted" to dayInvitations.count { it.status == InvitationStatus.ACCEPTED },
                "declined" to dayInvitations.count { it.status == InvitationStatus.DECLINED }
            )
        }.reversed()

        return mapOf(
            "total_invitations" to totalInvitations,
            "pending_invitations" to pendingInvitations,
            "accepted_invitations" to acceptedInvitations,
            "declined_invitations" to declinedInvitations,
            "expired_invitations" to expiredInvitations,
            "cancelled_invitations" to cancelledInvitations,
            "recent_invitations" to recentInvitations,
            "by_role" to byRole,
            "by_status" to byStatus,
            "recent_activity" to recentActivity,
            "acceptance_rate" to if (totalInvitations > 0) {
                (acceptedInvitations.toDouble() / totalInvitations * 100).toInt()
            } else 0,
            "pending_rate" to if (totalInvitations > 0) {
                (pendingInvitations.toDouble() / totalInvitations * 100).toInt()
            } else 0
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
     * Bulk cancel invitations
     */
    fun bulkCancelInvitations(
        workspaceId: String,
        invitationIds: List<String>,
        reason: String?,
        cancelledBy: String
    ): Map<String, Any> {
        val invitations = invitationRepository.findAllById(invitationIds)

        // Validate all invitations belong to workspace and are pending
        val validInvitations = mutableListOf<WorkspaceInvitation>()
        val failedCancellations = mutableListOf<Map<String, String>>()

        invitations.forEach { invitation ->
            when {
                invitation.workspaceId != workspaceId -> {
                    failedCancellations.add(
                        mapOf(
                            "invitation_id" to invitation.id.toString(),
                            "error" to "Invitation doesn't belong to this workspace"
                        )
                    )
                }

                invitation.status != InvitationStatus.PENDING -> {
                    failedCancellations.add(
                        mapOf(
                            "invitation_id" to invitation.id.toString(),
                            "error" to "Only pending invitations can be cancelled"
                        )
                    )
                }

                else -> validInvitations.add(invitation)
            }
        }

        // Cancel valid invitations
        validInvitations.forEach { invitation ->
            invitation.status = InvitationStatus.CANCELLED
            invitation.cancelledAt = LocalDateTime.now()
            invitation.cancelledBy = cancelledBy
            invitation.cancellationReason = reason
        }

        if (validInvitations.isNotEmpty()) {
            invitationRepository.saveAll(validInvitations)

            // Log activity
            activityService.logBulkInvitationCancellation(
                workspaceId,
                validInvitations.size,
                cancelledBy,
                "Unknown User"
            )
        }

        logger.info("Bulk cancelled ${validInvitations.size} invitations for workspace: $workspaceId")

        return mapOf(
            "cancelled_count" to validInvitations.size,
            "failed_cancellations" to failedCancellations
        )
    }

    /**
     * Bulk resend invitations
     */
    fun bulkResendInvitations(
        workspaceId: String,
        invitationIds: List<String>,
        message: String?,
        resentBy: String
    ): Map<String, Any> {
        val invitations = invitationRepository.findAllById(invitationIds)

        // Validate all invitations belong to workspace and are pending
        val validInvitations = mutableListOf<WorkspaceInvitation>()
        val failedResends = mutableListOf<Map<String, String>>()

        invitations.forEach { invitation ->
            when {
                invitation.workspaceId != workspaceId -> {
                    failedResends.add(
                        mapOf(
                            "invitation_id" to invitation.id.toString(),
                            "error" to "Invitation doesn't belong to this workspace"
                        )
                    )
                }

                invitation.status != InvitationStatus.PENDING -> {
                    failedResends.add(
                        mapOf(
                            "invitation_id" to invitation.id.toString(),
                            "error" to "Only pending invitations can be resent"
                        )
                    )
                }

                LocalDateTime.now().isAfter(invitation.expiresAt) -> {
                    failedResends.add(
                        mapOf(
                            "invitation_id" to invitation.id.toString(),
                            "error" to "Invitation has expired"
                        )
                    )
                }

                else -> validInvitations.add(invitation)
            }
        }

        // Resend valid invitations
        validInvitations.forEach { invitation ->
            invitation.sendCount += 1
            invitation.lastSentAt = LocalDateTime.now()
            message?.let { invitation.message = it }
        }

        if (validInvitations.isNotEmpty()) {
            invitationRepository.saveAll(validInvitations)

            // TODO: Send bulk emails
            logger.info("Bulk invitation emails would be resent to ${validInvitations.size} recipients")

            // Log activity
            activityService.logBulkInvitationResend(
                workspaceId,
                validInvitations.size,
                resentBy,
                "Unknown User"
            )
        }

        logger.info("Bulk resent ${validInvitations.size} invitations for workspace: $workspaceId")

        return mapOf(
            "resent_count" to validInvitations.size,
            "failed_resends" to failedResends
        )
    }

    /**
     * Export invitations data
     */
    fun exportInvitations(
        workspaceId: String,
        format: String,
        status: String?,
        role: String?,
        deliveryStatus: String?,
        searchQuery: String?,
        startDate: String?,
        endDate: String?
    ): ByteArray {
        // Get filtered invitations using search method
        val pageable = org.springframework.data.domain.PageRequest.of(0, 10000) // Get all for export
        val invitations = searchWorkspaceInvitations(
            workspaceId, pageable, status, role, deliveryStatus,
            searchQuery, startDate, endDate
        ).content

        return when (format.uppercase()) {
            "CSV" -> generateCsvExport(invitations)
            "EXCEL" -> generateExcelExport(invitations)
            else -> throw BusinessException("INVALID_FORMAT", "Unsupported export format: $format")
        }
    }

    /**
     * Bulk invitation operations (legacy method for backward compatibility)
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
        return invitationRepository.findByToken(token)
            .orElseThrow { NotFoundException("Invalid invitation token") }
    }

    private fun validateInvitation(invitation: WorkspaceInvitation) {
        when {
            invitation.status != InvitationStatus.PENDING ->
                throw BusinessException("INVITATION_NOT_PENDING", "Invitation is not pending")

            LocalDateTime.now().isAfter(invitation.expiresAt) ->
                throw BusinessException("INVITATION_EXPIRED", "Invitation has expired")
        }
    }

    private fun generateInvitationToken(): String {
        return UUID.randomUUID().toString().replace("-", "").uppercase()
    }

    /**
     * Generate CSV export for invitations
     */
    private fun generateCsvExport(invitations: List<InvitationListResponse>): ByteArray {
        val csvBuilder = StringBuilder()

        // CSV Header
        csvBuilder.appendLine("Email,Role,Status,Invited By,Invited At,Expires At,Accepted At,Send Count,Last Sent At,Message")

        // CSV Data
        invitations.forEach { invitation ->
            csvBuilder.appendLine(
                "\"${invitation.email}\"" +
                        ",\"${invitation.role}\"" +
                        ",\"${invitation.status}\"" +
                        ",\"${invitation.invitedBy ?: ""}\"" +
                        ",\"${invitation.invitedAt}\"" +
                        ",\"${invitation.expiresAt}\"" +
                        ",\"${invitation.acceptedAt ?: ""}\"" +
                        ",${invitation.sendCount}" +
                        ",\"${invitation.lastSentAt ?: ""}\"" +
                        ",\"${invitation.message?.replace("\"", "\\\"") ?: ""}\""
            )
        }

        return csvBuilder.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Generate Excel export for invitations
     * Note: This is a simplified implementation. In production, you'd use Apache POI
     */
    private fun generateExcelExport(invitations: List<InvitationListResponse>): ByteArray {
        // For now, return CSV format with Excel MIME type
        // In production, implement proper Excel generation using Apache POI
        return generateCsvExport(invitations)
    }
}
