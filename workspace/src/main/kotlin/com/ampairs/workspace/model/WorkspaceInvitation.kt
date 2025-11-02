package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.InvitationStatus
import com.ampairs.workspace.model.enums.WorkspaceRole
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
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
        Index(name = "idx_invitation_phone", columnList = "phone"),
        Index(name = "idx_invitation_status", columnList = "status"),
        Index(name = "idx_invitation_token", columnList = "token", unique = true),
        Index(name = "idx_invitation_expires_at", columnList = "expires_at")
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
    @Column(name = "email", length = 255)
    var email: String? = null

    /**
     * Phone number of the person being invited
     */
    @Column(name = "phone", length = 20)
    var phone: String? = null

    /**
     * Country code for the phone number
     */
    @Column(name = "country_code")
    var countryCode: Int? = null

    /**
     * Role that will be assigned upon accepting the invitation
     */
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    var role: WorkspaceRole = WorkspaceRole.MEMBER

    /**
     * Current status of the invitation
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: InvitationStatus = InvitationStatus.PENDING

    /**
     * Unique token for accepting the invitation
     */
    @Column(name = "token", nullable = false, length = 100)
    var token: String = ""

    /**
     * When the invitation expires
     */
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now().plusSeconds(7 * 86400)

    /**
     * Personal message included with the invitation
     */
    @Column(name = "message", length = 500)
    var message: String? = null

    /**
     * ID of the user who sent the invitation
     */
    @Column(name = "invited_by", length = 36)
    var invitedBy: String? = null

    /**
     * Name of the user who sent the invitation
     */
    @Column(name = "invited_by_name", length = 255)
    var invitedByName: String? = null

    /**
     * Department assignment for the invitation
     */
    @Column(name = "department", length = 100)
    var department: String? = null

    /**
     * Team assignments for this invitation (JSON array)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "team_ids")
    var teamIds: Set<String> = setOf()

    /**
     * Primary team for this invitation
     */
    @Column(name = "primary_team_id", length = 36)
    var primaryTeamId: String? = null

    /**
     * Job title for the invitee
     */
    @Column(name = "job_title", length = 100)
    var jobTitle: String? = null

    /**
     * Additional metadata for the invitation (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    var metadata: Map<String, Any> = mapOf()

    /**
     * When the invitation was accepted
     */
    @Column(name = "accepted_at")
    var acceptedAt: Instant? = null

    /**
     * When the invitation was rejected
     */
    @Column(name = "rejected_at")
    var rejectedAt: Instant? = null

    /**
     * Reason for rejection (optional)
     */
    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null

    /**
     * Number of times the invitation email was sent
     */
    @Column(name = "send_count", nullable = false)
    var sendCount: Int = 1

    /**
     * When the invitation was last sent/resent
     */
    @Column(name = "last_sent_at", nullable = true)
    var lastSentAt: Instant? = null

    /**
     * When the invitation was cancelled
     */
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null

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

    /**
     * Auto-assign teams based on department or other rules
     */
    @Column(name = "auto_assign_teams")
    var autoAssignTeams: Boolean = true

    // JPA Relationships

    // JPA Relationships
    // Note: Removed workspace relationship mapping to avoid column conflict
    // The workspaceId string field above is used instead of entity relationship

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_INVITATION_PREFIX
    }

    init {
        // Generate unique invitation token
        token = generateInvitationToken()
    }

    /**
     * Generate a secure unique invitation token
     */
    private fun generateInvitationToken(): String {
        return UUID.randomUUID().toString().replace("-", "") +
                System.currentTimeMillis().toString(36)
    }

    /**
     * Check if invitation is valid/active
     */
    fun isValid(): Boolean {
        return status == InvitationStatus.PENDING &&
                Instant.now().isBefore(expiresAt)
    }

    /**
     * Accept invitation
     */
    fun accept() {
        status = InvitationStatus.ACCEPTED
        acceptedAt = Instant.now()
    }

    /**
     * Reject invitation
     */
    fun reject(reason: String? = null) {
        status = InvitationStatus.DECLINED
        rejectedAt = Instant.now()
        rejectionReason = reason
    }

    /**
     * Cancel invitation
     */
    fun cancel(reason: String? = null) {
        status = InvitationStatus.CANCELLED
        rejectionReason = reason
    }

    /**
     * Check if this invitation includes team assignments
     */
    fun hasTeamAssignments(): Boolean {
        return teamIds.isNotEmpty()
    }

    /**
     * Get contact method used (email or phone)
     */
    fun getContactMethod(): String {
        return when {
            !email.isNullOrBlank() -> "email"
            !phone.isNullOrBlank() -> "phone"
            else -> throw IllegalStateException("Invitation has no contact method")
        }
    }

    /**
     * Get contact value (email or phone)
     */
    fun getContactValue(): String {
        return when {
            !email.isNullOrBlank() -> email!!
            !phone.isNullOrBlank() -> phone!!
            else -> throw IllegalStateException("Invitation has no contact value")
        }
    }
}