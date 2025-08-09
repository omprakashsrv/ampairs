package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.WorkspaceInvitation
import com.ampairs.workspace.model.enums.InvitationStatus
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTO for workspace invitation information
 */
data class InvitationResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("workspace_id")
    val workspaceId: String,

    @JsonProperty("workspace_name")
    val workspaceName: String? = null,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("role")
    val role: WorkspaceRole,

    @JsonProperty("status")
    val status: InvitationStatus,

    @JsonProperty("invitation_token")
    val invitationToken: String? = null,

    @JsonProperty("message")
    val message: String?,

    @JsonProperty("invited_by")
    val invitedBy: String,

    @JsonProperty("inviter_name")
    val inviterName: String? = null,

    @JsonProperty("expires_at")
    val expiresAt: LocalDateTime,

    @JsonProperty("accepted_at")
    val acceptedAt: LocalDateTime?,

    @JsonProperty("declined_at")
    val declinedAt: LocalDateTime?,

    @JsonProperty("cancelled_at")
    val cancelledAt: LocalDateTime?,

    @JsonProperty("cancelled_by")
    val cancelledBy: String?,

    @JsonProperty("cancellation_reason")
    val cancellationReason: String?,

    @JsonProperty("send_count")
    val sendCount: Int,

    @JsonProperty("last_sent_at")
    val lastSentAt: LocalDateTime?,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,

    @JsonProperty("is_expired")
    val isExpired: Boolean,

    @JsonProperty("days_until_expiry")
    val daysUntilExpiry: Long?,
)

/**
 * Simplified invitation response for lists
 */
data class InvitationListResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("role")
    val role: WorkspaceRole,

    @JsonProperty("status")
    val status: InvitationStatus,

    @JsonProperty("invited_by")
    val invitedBy: String,

    @JsonProperty("inviter_name")
    val inviterName: String? = null,

    @JsonProperty("expires_at")
    val expiresAt: LocalDateTime,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("is_expired")
    val isExpired: Boolean,
)

/**
 * Response DTO for invitation statistics
 */
data class InvitationStatsResponse(
    @JsonProperty("total_invitations")
    val totalInvitations: Long,

    @JsonProperty("pending_invitations")
    val pendingInvitations: Long,

    @JsonProperty("accepted_invitations")
    val acceptedInvitations: Long,

    @JsonProperty("declined_invitations")
    val declinedInvitations: Long,

    @JsonProperty("expired_invitations")
    val expiredInvitations: Long,

    @JsonProperty("cancelled_invitations")
    val cancelledInvitations: Long,

    @JsonProperty("recent_invitations")
    val recentInvitations: Long,
)

/**
 * Response DTO for public invitation details (used for invitation preview)
 */
data class PublicInvitationResponse(
    @JsonProperty("workspace_name")
    val workspaceName: String,

    @JsonProperty("workspace_description")
    val workspaceDescription: String?,

    @JsonProperty("workspace_avatar_url")
    val workspaceAvatarUrl: String?,

    @JsonProperty("inviter_name")
    val inviterName: String,

    @JsonProperty("role")
    val role: WorkspaceRole,

    @JsonProperty("expires_at")
    val expiresAt: LocalDateTime,

    @JsonProperty("is_expired")
    val isExpired: Boolean,

    @JsonProperty("is_valid")
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
        role = this.role,
        status = this.status,
        invitationToken = this.invitationToken,
        message = this.message,
        invitedBy = this.invitedBy,
        inviterName = null, // Will be populated from User entity if needed
        expiresAt = this.expiresAt,
        acceptedAt = this.acceptedAt,
        declinedAt = this.declinedAt,
        cancelledAt = this.cancelledAt,
        cancelledBy = this.cancelledBy,
        cancellationReason = this.cancellationReason,
        sendCount = this.sendCount,
        lastSentAt = this.lastSentAt,
        createdAt = LocalDateTime.parse(this.createdAt ?: "2023-01-01T00:00:00"),
        updatedAt = LocalDateTime.parse(this.updatedAt ?: "2023-01-01T00:00:00"),
        isExpired = this.isExpired(),
        daysUntilExpiry = this.getDaysUntilExpiry()
    )
}

/**
 * Extension function to convert WorkspaceInvitation entity to InvitationListResponse
 */
fun WorkspaceInvitation.toListResponse(): InvitationListResponse {
    return InvitationListResponse(
        id = this.uid, // Use uid instead of id
        email = this.email,
        role = this.role,
        status = this.status,
        invitedBy = this.invitedBy,
        inviterName = null, // Will be populated from User entity if needed
        expiresAt = this.expiresAt,
        createdAt = LocalDateTime.parse(this.createdAt ?: "2023-01-01T00:00:00"),
        isExpired = this.isExpired()
    )
}