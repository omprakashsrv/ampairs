package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceInvitation
import com.ampairs.workspace.model.enums.InvitationStatus
import com.ampairs.workspace.model.enums.WorkspaceRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Repository for workspace invitation operations
 */
@Repository
interface WorkspaceInvitationRepository : JpaRepository<WorkspaceInvitation, String> {

    /**
     * Find invitation by token
     */
    fun findByToken(token: String): Optional<WorkspaceInvitation>

    /**
     * Find invitations by workspace
     */
    fun findByWorkspaceIdOrderByCreatedAtDesc(workspaceId: String): List<WorkspaceInvitation>

    /**
     * Find invitations by workspace with pagination
     */
    fun findByWorkspaceId(workspaceId: String, pageable: Pageable): Page<WorkspaceInvitation>

    /**
     * Find pending invitations by workspace
     */
    fun findByWorkspaceIdAndStatus(workspaceId: String, status: InvitationStatus): List<WorkspaceInvitation>

    /**
     * Find invitation by workspace and email
     */
    fun findByWorkspaceIdAndEmailAndStatus(
        workspaceId: String,
        email: String,
        status: InvitationStatus,
    ): Optional<WorkspaceInvitation>

    /**
     * Find invitations by email across all workspaces
     */
    fun findByEmailOrderByCreatedAtDesc(email: String): List<WorkspaceInvitation>

    /**
     * Find pending invitations by email
     */
    fun findByEmailAndStatus(email: String, status: InvitationStatus): List<WorkspaceInvitation>

    /**
     * Find invitations by inviter
     */
    fun findByInvitedByOrderByCreatedAtDesc(inviterId: String): List<WorkspaceInvitation>

    /**
     * Find expired invitations
     */
    fun findByStatusAndExpiresAtBefore(status: InvitationStatus, currentDate: LocalDateTime): List<WorkspaceInvitation>

    /**
     * Find expiring invitations (within next X days)
     */
    fun findByStatusAndExpiresAtBetween(
        status: InvitationStatus,
        currentDate: LocalDateTime,
        expirationDate: LocalDateTime,
    ): List<WorkspaceInvitation>

    /**
     * Count invitations by workspace and status
     */
    fun countByWorkspaceIdAndStatus(workspaceId: String, status: InvitationStatus): Long

    /**
     * Count pending invitations by email
     */
    fun countByEmailAndStatus(email: String, status: InvitationStatus): Long

    /**
     * Check if there's already a pending invitation for email in workspace
     */
    fun existsByWorkspaceIdAndEmailAndStatus(
        workspaceId: String,
        email: String,
        status: InvitationStatus,
    ): Boolean

    /**
     * Find invitations by role
     */
    fun findByWorkspaceIdAndRole(workspaceId: String, role: WorkspaceRole): List<WorkspaceInvitation>

    /**
     * Find recent invitations (created after date)
     */
    fun findByCreatedAtAfterOrderByCreatedAtDesc(createdAfter: LocalDateTime): List<WorkspaceInvitation>

    /**
     * Mark expired invitations as expired
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_invitations wi 
        SET wi.status = :expiredStatus 
        WHERE wi.status = :pendingStatus 
        AND wi.expiresAt < :currentDate
    """
    )
    fun markExpiredInvitations(
        @Param("currentDate") currentDate: LocalDateTime,
        @Param("pendingStatus") pendingStatus: InvitationStatus = InvitationStatus.PENDING,
        @Param("expiredStatus") expiredStatus: InvitationStatus = InvitationStatus.EXPIRED,
    ): Int

    /**
     * Get invitation statistics for a workspace
     */
    @Query(
        """
        SELECT 
            COUNT(wi) as totalInvitations,
            COUNT(CASE WHEN wi.status = :pendingStatus THEN 1 END) as pendingInvitations,
            COUNT(CASE WHEN wi.status = :acceptedStatus THEN 1 END) as acceptedInvitations,
            COUNT(CASE WHEN wi.status = :declinedStatus THEN 1 END) as declinedInvitations,
            COUNT(CASE WHEN wi.status = :expiredStatus THEN 1 END) as expiredInvitations
        FROM workspace_invitations wi 
        WHERE wi.workspaceId = :workspaceId
    """
    )
    fun getInvitationStatistics(
        @Param("workspaceId") workspaceId: String,
        @Param("pendingStatus") pendingStatus: InvitationStatus = InvitationStatus.PENDING,
        @Param("acceptedStatus") acceptedStatus: InvitationStatus = InvitationStatus.ACCEPTED,
        @Param("declinedStatus") declinedStatus: InvitationStatus = InvitationStatus.DECLINED,
        @Param("expiredStatus") expiredStatus: InvitationStatus = InvitationStatus.EXPIRED,
    ): Map<String, Long>

    /**
     * Delete old completed invitations (cleanup)
     */
    @Modifying
    @Query(
        """
        DELETE FROM workspace_invitations wi 
        WHERE wi.status IN :finalStatuses 
        AND wi.updatedAt < :cleanupDate
    """
    )
    fun deleteOldInvitations(
        @Param("cleanupDate") cleanupDate: LocalDateTime,
        @Param("finalStatuses") finalStatuses: List<InvitationStatus> = listOf(
            InvitationStatus.ACCEPTED,
            InvitationStatus.DECLINED,
            InvitationStatus.EXPIRED,
            InvitationStatus.CANCELLED,
            InvitationStatus.REVOKED
        ),
    ): Int

    /**
     * Find invitations that need reminder emails
     */
    @Query(
        """
        SELECT wi FROM workspace_invitations wi
        WHERE wi.status = :pendingStatus
        AND wi.expiresAt > :currentDate
        AND wi.sendCount < :maxSendCount
        AND wi.lastSentAt < :reminderDate
    """
    )
    fun findInvitationsForReminder(
        @Param("currentDate") currentDate: LocalDateTime,
        @Param("reminderDate") reminderDate: LocalDateTime,
        @Param("maxSendCount") maxSendCount: Int = 3,
        @Param("pendingStatus") pendingStatus: InvitationStatus = InvitationStatus.PENDING,
    ): List<WorkspaceInvitation>

    /**
     * Update invitation send count and last sent date
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_invitations wi 
        SET wi.sendCount = wi.sendCount + 1, wi.lastSentAt = :sentAt
        WHERE wi.id = :invitationId
    """
    )
    fun updateSendInfo(@Param("invitationId") invitationId: String, @Param("sentAt") sentAt: LocalDateTime)

    /**
     * Cancel all pending invitations for a workspace
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_invitations wi 
        SET wi.status = :cancelledStatus,
            wi.cancelledAt = :cancelledAt,
            wi.cancelledBy = :cancelledBy,
            wi.cancellationReason = :reason
        WHERE wi.workspaceId = :workspaceId 
        AND wi.status = :pendingStatus
    """
    )
    fun cancelAllPendingInvitations(
        @Param("workspaceId") workspaceId: String,
        @Param("cancelledAt") cancelledAt: LocalDateTime,
        @Param("cancelledBy") cancelledBy: String,
        @Param("reason") reason: String,
        @Param("pendingStatus") pendingStatus: InvitationStatus = InvitationStatus.PENDING,
        @Param("cancelledStatus") cancelledStatus: InvitationStatus = InvitationStatus.CANCELLED,
    ): Int

    /**
     * Count invitations created after a specific date
     */
    fun countByCreatedAtAfter(createdAfter: LocalDateTime): Long

    /**
     * Delete all invitations for a workspace
     */
    @Modifying
    @Query("DELETE FROM workspace_invitations wi WHERE wi.workspaceId = :workspaceId")
    fun deleteByWorkspaceId(@Param("workspaceId") workspaceId: String)

    /**
     * Find invitation by workspace and phone number
     */
    fun findByWorkspaceIdAndPhoneAndStatus(
        workspaceId: String,
        phone: String?,
        status: InvitationStatus
    ): WorkspaceInvitation?

    /**
     * Check if there's already a pending invitation for phone number in workspace
     */
    fun existsByWorkspaceIdAndPhoneAndStatus(
        workspaceId: String,
        phone: String?,
        status: InvitationStatus
    ): Boolean
}