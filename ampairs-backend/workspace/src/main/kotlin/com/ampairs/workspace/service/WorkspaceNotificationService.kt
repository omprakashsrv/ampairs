package com.ampairs.workspace.service

import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.service.NotificationService
import com.ampairs.workspace.model.WorkspaceInvitation
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.enums.WorkspaceRole
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service for sending workspace-related notifications through the notification module
 * Integrates workspace operations with the centralized notification system
 */
@Service
class WorkspaceNotificationService @Autowired constructor(
    private val notificationService: NotificationService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceNotificationService::class.java)
    }

    @Value("\${app.frontend-url:http://localhost:3000}")
    private lateinit var frontendUrl: String

    @Value("\${workspace.invitation.expiry-hours:168}") // 7 days default
    private var invitationExpiryHours: Long = 168

    /**
     * Send workspace invitation email
     */
    fun sendWorkspaceInvitation(
        invitation: WorkspaceInvitation,
        workspaceName: String,
        inviterName: String,
    ): String {
        return try {
            val message = buildInvitationMessage(
                workspaceName = workspaceName,
                inviterName = inviterName,
                role = invitation.role,
                inviteToken = invitation.token,
                expiryHours = invitationExpiryHours
            )

            val notificationId = notificationService.queueNotification(
                recipient = invitation.email ?: "",
                message = message,
                channel = NotificationChannel.EMAIL
            )

            logger.info(
                "Workspace invitation queued for email: {} in workspace: {} by: {} (notification ID: {})",
                invitation.email, invitation.workspaceId, inviterName, notificationId
            )

            notificationId
        } catch (e: Exception) {
            logger.error("Failed to send workspace invitation to: ${invitation.email}", e)
            throw e
        }
    }

    /**
     * Send invitation reminder
     */
    fun sendInvitationReminder(
        invitation: WorkspaceInvitation,
        workspaceName: String,
        resentBy: String,
    ): String {
        return try {
            val message = buildReminderMessage(
                workspaceName = workspaceName,
                resentBy = resentBy,
                role = invitation.role,
                inviteToken = invitation.token,
                expiryHours = invitationExpiryHours
            )

            val notificationId = notificationService.queueNotification(
                recipient = invitation.email ?: "",
                message = message,
                channel = NotificationChannel.EMAIL
            )

            logger.info(
                "Invitation reminder queued for email: {} in workspace: {} by: {} (notification ID: {})",
                invitation.email, invitation.workspaceId, resentBy, notificationId
            )

            notificationId
        } catch (e: Exception) {
            logger.error("Failed to send invitation reminder to: ${invitation.email}", e)
            throw e
        }
    }

    /**
     * Send welcome message to new member
     */
    fun sendWelcomeMessage(
        member: WorkspaceMember,
        workspaceName: String,
        welcomeEmail: String,
    ): String {
        return try {
            val message = buildWelcomeMessage(
                workspaceName = workspaceName,
                memberRole = member.role
            )

            val notificationId = notificationService.queueNotification(
                recipient = welcomeEmail,
                message = message,
                channel = NotificationChannel.EMAIL
            )

            logger.info(
                "Welcome message queued for email: {} in workspace: {} (notification ID: {})",
                welcomeEmail, member.workspaceId, notificationId
            )

            notificationId
        } catch (e: Exception) {
            logger.error("Failed to send welcome message to: $welcomeEmail", e)
            throw e
        }
    }

    /**
     * Send role change notification
     */
    fun sendRoleChangeNotification(
        member: WorkspaceMember,
        workspaceName: String,
        oldRole: WorkspaceRole,
        newRole: WorkspaceRole,
        changedBy: String,
        memberEmail: String,
    ): String {
        return try {
            val message = buildRoleChangeMessage(
                workspaceName = workspaceName,
                oldRole = oldRole,
                newRole = newRole,
                changedBy = changedBy
            )

            val notificationId = notificationService.queueNotification(
                recipient = memberEmail,
                message = message,
                channel = NotificationChannel.EMAIL
            )

            logger.info(
                "Role change notification queued for email: {} in workspace: {} (notification ID: {})",
                memberEmail, member.workspaceId, notificationId
            )

            notificationId
        } catch (e: Exception) {
            logger.error("Failed to send role change notification to: $memberEmail", e)
            throw e
        }
    }

    /**
     * Send member removal notification
     */
    fun sendMemberRemovalNotification(
        workspaceName: String,
        memberEmail: String,
        removedBy: String,
        reason: String? = null,
    ): String {
        return try {
            val message = buildMemberRemovalMessage(
                workspaceName = workspaceName,
                removedBy = removedBy,
                reason = reason
            )

            val notificationId = notificationService.queueNotification(
                recipient = memberEmail,
                message = message,
                channel = NotificationChannel.EMAIL
            )

            logger.info(
                "Member removal notification queued for email: {} from workspace: {} (notification ID: {})",
                memberEmail, workspaceName, notificationId
            )

            notificationId
        } catch (e: Exception) {
            logger.error("Failed to send member removal notification to: $memberEmail", e)
            throw e
        }
    }

    /**
     * Send invitation cancellation notification
     */
    fun sendInvitationCancellationNotification(
        invitation: WorkspaceInvitation,
        workspaceName: String,
        cancelledBy: String,
        reason: String? = null,
    ): String {
        return try {
            val message = buildInvitationCancellationMessage(
                workspaceName = workspaceName,
                cancelledBy = cancelledBy,
                reason = reason
            )

            val notificationId = notificationService.queueNotification(
                recipient = invitation.email ?: "",
                message = message,
                channel = NotificationChannel.EMAIL
            )

            logger.info(
                "Invitation cancellation notification queued for email: {} in workspace: {} (notification ID: {})",
                invitation.email, invitation.workspaceId, notificationId
            )

            notificationId
        } catch (e: Exception) {
            logger.error("Failed to send invitation cancellation notification to: ${invitation.email}", e)
            throw e
        }
    }

    /**
     * Send bulk invitation notifications
     */
    fun sendBulkInvitationNotifications(
        invitations: List<WorkspaceInvitation>,
        workspaceName: String,
        inviterName: String,
    ): List<String> {
        val notificationIds = mutableListOf<String>()

        invitations.forEach { invitation ->
            try {
                val notificationId = sendWorkspaceInvitation(invitation, workspaceName, inviterName)
                notificationIds.add(notificationId)
            } catch (e: Exception) {
                logger.error("Failed to send bulk invitation to: ${invitation.email}", e)
            }
        }

        logger.info(
            "Sent {} out of {} bulk invitations for workspace: {}",
            notificationIds.size, invitations.size, workspaceName
        )

        return notificationIds
    }

    // Private helper methods to build notification messages

    private fun buildInvitationMessage(
        workspaceName: String,
        inviterName: String,
        role: WorkspaceRole,
        inviteToken: String,
        expiryHours: Long,
    ): String {
        val inviteUrl = "$frontendUrl/workspace/accept-invitation?token=$inviteToken"
        val roleName = role.displayName

        return """
        Subject: You're invited to join $workspaceName workspace
        
        Hello,
        
        $inviterName has invited you to join the "$workspaceName" workspace as a $roleName.
        
        Click the link below to accept the invitation:
        $inviteUrl
        
        This invitation will expire in $expiryHours hours.
        
        If you have any questions, please contact the workspace administrator.
        
        Best regards,
        The Ampairs Team
        """.trimIndent()
    }

    private fun buildReminderMessage(
        workspaceName: String,
        resentBy: String,
        role: WorkspaceRole,
        inviteToken: String,
        expiryHours: Long,
    ): String {
        val inviteUrl = "$frontendUrl/workspace/accept-invitation?token=$inviteToken"
        val roleName = role.displayName

        return """
        Subject: Reminder: You're invited to join $workspaceName workspace
        
        Hello,
        
        This is a reminder that you have been invited to join the "$workspaceName" workspace as a $roleName.
        
        Click the link below to accept the invitation:
        $inviteUrl
        
        This invitation will expire in $expiryHours hours.
        
        Reminder sent by: $resentBy
        
        If you have any questions, please contact the workspace administrator.
        
        Best regards,
        The Ampairs Team
        """.trimIndent()
    }

    private fun buildWelcomeMessage(
        workspaceName: String,
        memberRole: WorkspaceRole,
    ): String {
        return """
        Subject: Welcome to $workspaceName workspace
        
        Hello,
        
        Welcome to the "$workspaceName" workspace! You have joined as a ${memberRole.displayName}.
        
        You can now access the workspace and start collaborating with your team.
        
        Log in to your account: $frontendUrl/login
        
        If you have any questions or need help getting started, please contact your workspace administrator.
        
        Best regards,
        The Ampairs Team
        """.trimIndent()
    }

    private fun buildRoleChangeMessage(
        workspaceName: String,
        oldRole: WorkspaceRole,
        newRole: WorkspaceRole,
        changedBy: String,
    ): String {
        return """
        Subject: Your role has been updated in $workspaceName workspace
        
        Hello,
        
        Your role in the "$workspaceName" workspace has been updated:
        
        Previous Role: ${oldRole.displayName}
        New Role: ${newRole.displayName}
        
        Changed by: $changedBy
        
        Your new permissions will take effect immediately when you log in to your account.
        
        Log in to your account: $frontendUrl/login
        
        If you have any questions about your new role, please contact your workspace administrator.
        
        Best regards,
        The Ampairs Team
        """.trimIndent()
    }

    private fun buildMemberRemovalMessage(
        workspaceName: String,
        removedBy: String,
        reason: String?,
    ): String {
        val reasonText = if (reason != null) "\nReason: $reason" else ""

        return """
        Subject: You have been removed from $workspaceName workspace
        
        Hello,
        
        You have been removed from the "$workspaceName" workspace by $removedBy.$reasonText
        
        You will no longer have access to this workspace and its resources.
        
        If you believe this is an error or have any questions, please contact the workspace administrator.
        
        Best regards,
        The Ampairs Team
        """.trimIndent()
    }

    private fun buildInvitationCancellationMessage(
        workspaceName: String,
        cancelledBy: String,
        reason: String?,
    ): String {
        val reasonText = if (reason != null) "\nReason: $reason" else ""

        return """
        Subject: Your invitation to $workspaceName workspace has been cancelled
        
        Hello,
        
        Your invitation to join the "$workspaceName" workspace has been cancelled by $cancelledBy.$reasonText
        
        The invitation link you received is no longer valid.
        
        If you believe this is an error or have any questions, please contact the workspace administrator.
        
        Best regards,
        The Ampairs Team
        """.trimIndent()
    }
}