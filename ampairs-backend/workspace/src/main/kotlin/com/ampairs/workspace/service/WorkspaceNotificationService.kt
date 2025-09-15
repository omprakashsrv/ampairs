package com.ampairs.workspace.service

import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.service.NotificationService
import com.ampairs.workspace.model.WorkspaceInvitation
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
}