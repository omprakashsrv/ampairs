package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.InvitationStatus
import com.ampairs.workspace.model.enums.WorkspaceRole
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Represents an invitation to join a workspace
 */
@Entity(name = "workspace_invitations")
@Table(
    name = "workspace_invitations",
    indexes = [
        Index(name = "idx_invitation_workspace", columnList = "workspace_id"),
        Index(name = "idx_invitation_email", columnList = "email"),
        Index(name = "idx_invitation_token", columnList = "invitation_token", unique = true),
        Index(name = "idx_invitation_status", columnList = "status"),
        Index(name = "idx_invitation_expires", columnList = "expires_at"),
        Index(name = "idx_invitation_workspace_email", columnList = "workspace_id, email")
    ]
)
class WorkspaceInvitation : BaseDomain() {

    /**
     * ID of the workspace the invitation is for
     */
    @Column(name = "workspace_id", nullable = false, length = 36)
    var workspaceId: String = ""

    /**
     * Email address of the person being invited
     */
    @Column(name = "email", nullable = false, length = 255)
    var email: String = ""

    /**
     * Role that will be assigned upon accepting the invitation
     */
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    var role: WorkspaceRole = WorkspaceRole.MEMBER

    /**
     * ID of the user who sent the invitation
     */
    @Column(name = "invited_by", nullable = false, length = 36)
    var invitedBy: String = ""

    /**
     * Unique token for accepting the invitation
     */
    @Column(name = "invitation_token", nullable = false, unique = true, length = 128)
    var invitationToken: String = ""

    /**
     * Current status of the invitation
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: InvitationStatus = InvitationStatus.PENDING

    /**
     * When the invitation expires
     */
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7)

    /**
     * Personal message included with the invitation
     */
    @Column(name = "message", columnDefinition = "TEXT")
    var message: String? = null

    /**
     * When the invitation was accepted
     */
    @Column(name = "accepted_at")
    var acceptedAt: LocalDateTime? = null

    /**
     * ID of the user who accepted (may differ from email if they used different account)
     */
    @Column(name = "accepted_by_user_id", length = 36)
    var acceptedByUserId: String? = null

    /**
     * When the invitation was declined
     */
    @Column(name = "declined_at")
    var declinedAt: LocalDateTime? = null

    /**
     * Reason for declining (optional)
     */
    @Column(name = "decline_reason", length = 500)
    var declineReason: String? = null

    /**
     * Number of times the invitation email was sent
     */
    @Column(name = "send_count", nullable = false)
    var sendCount: Int = 1

    /**
     * When the invitation was last sent/resent
     */
    @Column(name = "last_sent_at", nullable = true)
    var lastSentAt: LocalDateTime? = null

    /**
     * When the invitation was cancelled
     */
    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null

    /**
     * ID of the user who cancelled the invitation
     */
    @Column(name = "cancelled_by", length = 36)
    var cancelledBy: String? = null

    /**
     * Reason for cancellation
     */
    @Column(name = "cancellation_reason", length = 500)
    var cancellationReason: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_INVITATION_PREFIX
    }

    init {
        // Generate unique invitation token
        invitationToken = generateInvitationToken()
    }

    /**
     * Generate a secure unique invitation token
     */
    private fun generateInvitationToken(): String {
        return UUID.randomUUID().toString().replace("-", "") +
                System.currentTimeMillis().toString(36)
    }

    /**
     * Check if the invitation is expired
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }

    /**
     * Check if the invitation can be accepted
     */
    fun canBeAccepted(): Boolean {
        return status == InvitationStatus.PENDING && !isExpired()
    }

    /**
     * Check if the invitation can be cancelled
     */
    fun canBeCancelled(): Boolean {
        return status == InvitationStatus.PENDING
    }

    /**
     * Accept the invitation
     */
    fun accept(userId: String): Boolean {
        if (!canBeAccepted()) return false

        status = InvitationStatus.ACCEPTED
        acceptedAt = LocalDateTime.now()
        acceptedByUserId = userId
        return true
    }

    /**
     * Decline the invitation
     */
    fun decline(reason: String? = null): Boolean {
        if (!canBeAccepted()) return false

        status = InvitationStatus.DECLINED
        declinedAt = LocalDateTime.now()
        declineReason = reason
        return true
    }

    /**
     * Cancel the invitation
     */
    fun cancel(cancelledByUserId: String, reason: String? = null): Boolean {
        if (!canBeCancelled()) return false

        status = InvitationStatus.CANCELLED
        cancelledAt = LocalDateTime.now()
        cancelledBy = cancelledByUserId
        cancellationReason = reason
        return true
    }

    /**
     * Mark as expired
     */
    fun markExpired() {
        if (status == InvitationStatus.PENDING && isExpired()) {
            status = InvitationStatus.EXPIRED
        }
    }

    /**
     * Resend the invitation (increment send count)
     */
    fun resend() {
        if (status == InvitationStatus.PENDING && !isExpired()) {
            sendCount++
            lastSentAt = LocalDateTime.now()
        }
    }

    /**
     * Extend the expiration date
     */
    fun extendExpiration(days: Long = 7) {
        if (status == InvitationStatus.PENDING) {
            expiresAt = LocalDateTime.now().plusDays(days)
        }
    }

    /**
     * Get days until expiry (null if expired or not pending)
     */
    fun getDaysUntilExpiry(): Long? {
        if (status != InvitationStatus.PENDING || isExpired()) {
            return null
        }
        val now = LocalDateTime.now()
        return java.time.Duration.between(now, expiresAt).toDays()
    }
}