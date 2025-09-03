package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.WorkspaceInvitation
import com.ampairs.workspace.model.enums.InvitationStatus
import com.ampairs.workspace.model.enums.WorkspaceRole
import java.time.LocalDateTime

/**
 * Response DTO for workspace invitation information
 */
data class InvitationResponse(
    val id: String,

    val workspaceId: String,

    val workspaceName: String? = null,

    val email: String?,

    val phone: String?,

    val countryCode: Int?,

    val role: WorkspaceRole,

    val status: InvitationStatus,

    val token: String,

    val message: String?,

    val invitedBy: String?,

    val inviterName: String? = null,

    val expiresAt: LocalDateTime,

    val acceptedAt: LocalDateTime?,

    val rejectedAt: LocalDateTime?,

    val cancelledAt: LocalDateTime?,

    val cancelledBy: String?,

    val cancellationReason: String?,

    val sendCount: Int,

    val lastSentAt: LocalDateTime?,

    val createdAt: LocalDateTime,

    val updatedAt: LocalDateTime,

    val isExpired: Boolean,

    val daysUntilExpiry: Long?,
)

/**
 * Simplified invitation response for lists
 */
data class InvitationListResponse(
    val id: String,

    val email: String?,

    val phone: String?,

    val countryCode: Int?,

    val role: WorkspaceRole,

    val status: InvitationStatus,

    val invitedBy: String?,

    val inviterName: String? = null,

    val expiresAt: LocalDateTime,

    val invitedAt: LocalDateTime,

    val acceptedAt: LocalDateTime? = null,

    val sendCount: Int = 0,

    val lastSentAt: LocalDateTime? = null,

    val message: String? = null,

    val createdAt: LocalDateTime,

    val isExpired: Boolean,
)

/**
 * Response DTO for invitation statistics
 */
data class InvitationStatsResponse(
    val totalInvitations: Long,

    val pendingInvitations: Long,

    val acceptedInvitations: Long,

    val declinedInvitations: Long,

    val expiredInvitations: Long,

    val cancelledInvitations: Long,

    val recentInvitations: Long,
)

/**
 * Response DTO for public invitation details (used for invitation preview)
 */
data class PublicInvitationResponse(
    val workspaceName: String,

    val workspaceDescription: String?,

    val workspaceAvatarUrl: String?,

    val inviterName: String,

    val role: WorkspaceRole,

    val expiresAt: LocalDateTime,

    val isExpired: Boolean,

    val isValid: Boolean,
)

/**
 * Extension function to convert WorkspaceInvitation entity to InvitationResponse
 */
fun WorkspaceInvitation.toResponse(): InvitationResponse {
    return InvitationResponse(
        id = this.uid, // Use uid instead of id
        workspaceId = this.workspaceId,
        workspaceName = null, // Will be populated from Workspace entity if needed
        email = this.email,
        phone = this.phone,
        countryCode = this.countryCode,
        role = this.role,
        status = this.status,
        token = this.token,
        message = this.message,
        invitedBy = this.invitedBy,
        inviterName = null, // Will be populated from User entity if needed
        expiresAt = this.expiresAt,
        acceptedAt = this.acceptedAt,
        rejectedAt = this.rejectedAt,
        cancelledAt = this.cancelledAt,
        cancelledBy = this.cancelledBy,
        cancellationReason = this.cancellationReason,
        sendCount = this.sendCount,
        lastSentAt = this.lastSentAt,
        createdAt = this.createdAt ?: LocalDateTime.now(),
        updatedAt = this.updatedAt ?: LocalDateTime.now(),
        isExpired = LocalDateTime.now().isAfter(this.expiresAt),
        daysUntilExpiry = if (LocalDateTime.now().isBefore(this.expiresAt)) {
            java.time.Duration.between(LocalDateTime.now(), this.expiresAt).toDays()
        } else null
    )
}

/**
 * Extension function to convert WorkspaceInvitation entity to InvitationListResponse
 */
fun WorkspaceInvitation.toListResponse(): InvitationListResponse {
    return InvitationListResponse(
        id = this.uid, // Use uid instead of id
        email = this.email,
        phone = this.phone,
        countryCode = this.countryCode,
        role = this.role,
        status = this.status,
        invitedBy = this.invitedBy,
        inviterName = null, // Will be populated from User entity if needed
        expiresAt = this.expiresAt,
        invitedAt = this.createdAt ?: LocalDateTime.now(),
        acceptedAt = this.acceptedAt,
        sendCount = this.sendCount,
        lastSentAt = this.lastSentAt,
        message = this.message,
        createdAt = this.createdAt ?: LocalDateTime.now(),
        isExpired = LocalDateTime.now().isAfter(this.expiresAt)
    )
}