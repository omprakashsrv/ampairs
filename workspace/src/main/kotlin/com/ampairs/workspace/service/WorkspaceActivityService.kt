package com.ampairs.workspace.service

import com.ampairs.workspace.model.WorkspaceActivity
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceActivityType
import com.ampairs.workspace.repository.WorkspaceActivityRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDateTime

/**
 * Service for workspace activity logging and tracking operations
 * Provides central activity logging with database persistence
 */
@Service
class WorkspaceActivityService @Autowired constructor(
    private val activityRepository: WorkspaceActivityRepository,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceActivityService::class.java)
    }

    /**
     * Log workspace creation
     */
    @Transactional
    fun logWorkspaceCreated(workspaceId: String, createdBy: String, createdByName: String) {
        try {
            val activity = WorkspaceActivity.builder(createdBy, createdByName)
                .activityType(WorkspaceActivityType.WORKSPACE_CREATED)
                .description("Workspace '$workspaceId' was created")
                .targetEntity("workspace", workspaceId, workspaceId)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Workspace created - workspaceId: $workspaceId, createdBy: $createdBy")
        } catch (e: Exception) {
            logger.error("Failed to log workspace creation activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log workspace updated
     */
    @Transactional
    fun logWorkspaceUpdated(
        workspaceId: String,
        updatedBy: String,
        updatedByName: String,
        changes: Map<String, Any>? = null,
    ) {
        try {
            val contextData = changes?.let { objectMapper.writeValueAsString(it) }

            val activity = WorkspaceActivity.builder(updatedBy, updatedByName)
                .activityType(WorkspaceActivityType.WORKSPACE_UPDATED)
                .description("Workspace settings were updated")
                .targetEntity("workspace", workspaceId)
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Workspace updated - workspaceId: $workspaceId, updatedBy: $updatedBy")
        } catch (e: Exception) {
            logger.error("Failed to log workspace update activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log workspace archived
     */
    @Transactional
    fun logWorkspaceArchived(workspaceId: String, archivedBy: String, archivedByName: String, reason: String? = null) {
        try {
            val contextData = reason?.let { objectMapper.writeValueAsString(mapOf("reason" to it)) }

            val activity = WorkspaceActivity.builder(archivedBy, archivedByName)
                .activityType(WorkspaceActivityType.WORKSPACE_ARCHIVED)
                .description("Workspace was archived")
                .targetEntity("workspace", workspaceId)
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Workspace archived - workspaceId: $workspaceId, archivedBy: $archivedBy")
        } catch (e: Exception) {
            logger.error("Failed to log workspace archive activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log subscription updated
     */
    @Transactional
    fun logSubscriptionUpdated(
        workspaceId: String,
        updatedBy: String,
        updatedByName: String,
        subscriptionPlan: SubscriptionPlan,
        oldPlan: SubscriptionPlan? = null,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "newPlan" to subscriptionPlan.name,
                    "oldPlan" to (oldPlan?.name ?: "UNKNOWN")
                )
            )

            val activity = WorkspaceActivity.builder(updatedBy, updatedByName)
                .activityType(WorkspaceActivityType.SUBSCRIPTION_UPDATED)
                .description("Subscription plan updated to ${subscriptionPlan.displayName}")
                .targetEntity("subscription", workspaceId)
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Subscription updated - workspaceId: $workspaceId, updatedBy: $updatedBy, plan: $subscriptionPlan")
        } catch (e: Exception) {
            logger.error("Failed to log subscription update activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log member added
     */
    @Transactional
    fun logMemberAdded(
        workspaceId: String,
        userId: String,
        userName: String,
        role: String,
        addedBy: String,
        addedByName: String,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "role" to role,
                    "userId" to userId
                )
            )

            val activity = WorkspaceActivity.builder(addedBy, addedByName)
                .activityType(WorkspaceActivityType.MEMBER_ADDED)
                .description("Member '$userName' was added with role $role")
                .targetEntity("member", userId, userName)
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Member added - workspaceId: $workspaceId, userId: $userId, role: $role")
        } catch (e: Exception) {
            logger.error("Failed to log member addition activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log member role changed
     */
    @Transactional
    fun logMemberRoleChanged(
        workspaceId: String,
        userId: String,
        userName: String,
        oldRole: String,
        newRole: String,
        changedBy: String,
        changedByName: String,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "oldRole" to oldRole,
                    "newRole" to newRole,
                    "userId" to userId
                )
            )

            val activity = WorkspaceActivity.builder(changedBy, changedByName)
                .activityType(WorkspaceActivityType.MEMBER_ROLE_CHANGED)
                .description("Member '$userName' role changed from $oldRole to $newRole")
                .targetEntity("member", userId, userName)
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Member role changed - workspaceId: $workspaceId, userId: $userId, oldRole: $oldRole, newRole: $newRole, changedBy: $changedBy")
        } catch (e: Exception) {
            logger.error("Failed to log member role change activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log member activated
     */
    @Transactional
    fun logMemberActivated(
        workspaceId: String,
        userId: String,
        userName: String,
        activatedBy: String,
        activatedByName: String,
    ) {
        try {
            val activity = WorkspaceActivity.builder(activatedBy, activatedByName)
                .activityType(WorkspaceActivityType.MEMBER_ACTIVATED)
                .description("Member '$userName' was activated")
                .targetEntity("member", userId, userName)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Member activated - workspaceId: $workspaceId, userId: $userId, activatedBy: $activatedBy")
        } catch (e: Exception) {
            logger.error("Failed to log member activation activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log member deactivated
     */
    @Transactional
    fun logMemberDeactivated(
        workspaceId: String,
        userId: String,
        userName: String,
        deactivatedBy: String,
        deactivatedByName: String,
    ) {
        try {
            val activity = WorkspaceActivity.builder(deactivatedBy, deactivatedByName)
                .activityType(WorkspaceActivityType.MEMBER_DEACTIVATED)
                .description("Member '$userName' was deactivated")
                .targetEntity("member", userId, userName)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Member deactivated - workspaceId: $workspaceId, userId: $userId, deactivatedBy: $deactivatedBy")
        } catch (e: Exception) {
            logger.error("Failed to log member deactivation activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log member removed
     */
    @Transactional
    fun logMemberRemoved(
        workspaceId: String,
        userId: String,
        userName: String,
        removedBy: String,
        removedByName: String,
        reason: String? = null,
    ) {
        try {
            val contextData = reason?.let { objectMapper.writeValueAsString(mapOf("reason" to it)) }

            val activity = WorkspaceActivity.builder(removedBy, removedByName)
                .activityType(WorkspaceActivityType.MEMBER_REMOVED)
                .description("Member '$userName' was removed")
                .targetEntity("member", userId, userName)
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Member removed - workspaceId: $workspaceId, userId: $userId, removedBy: $removedBy")
        } catch (e: Exception) {
            logger.error("Failed to log member removal activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log bulk member activation
     */
    @Transactional
    fun logBulkMemberActivation(workspaceId: String, memberCount: Int, activatedBy: String, activatedByName: String) {
        try {
            val contextData = objectMapper.writeValueAsString(mapOf("memberCount" to memberCount))

            val activity = WorkspaceActivity.builder(activatedBy, activatedByName)
                .activityType(WorkspaceActivityType.BULK_MEMBER_ACTIVATION)
                .description("Bulk activation of $memberCount members")
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Bulk member activation - workspaceId: $workspaceId, memberCount: $memberCount, activatedBy: $activatedBy")
        } catch (e: Exception) {
            logger.error("Failed to log bulk member activation activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log bulk member deactivation
     */
    @Transactional
    fun logBulkMemberDeactivation(
        workspaceId: String,
        memberCount: Int,
        deactivatedBy: String,
        deactivatedByName: String,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(mapOf("memberCount" to memberCount))

            val activity = WorkspaceActivity.builder(deactivatedBy, deactivatedByName)
                .activityType(WorkspaceActivityType.BULK_MEMBER_DEACTIVATION)
                .description("Bulk deactivation of $memberCount members")
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Bulk member deactivation - workspaceId: $workspaceId, memberCount: $memberCount, deactivatedBy: $deactivatedBy")
        } catch (e: Exception) {
            logger.error("Failed to log bulk member deactivation activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log bulk member removal
     */
    @Transactional
    fun logBulkMemberRemoval(
        workspaceId: String,
        memberCount: Int,
        removedBy: String,
        removedByName: String,
        reason: String? = null,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "memberCount" to memberCount,
                    "reason" to (reason ?: "Not specified")
                )
            )

            val activity = WorkspaceActivity.builder(removedBy, removedByName)
                .activityType(WorkspaceActivityType.BULK_MEMBER_REMOVAL)
                .description("Bulk removal of $memberCount members")
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Bulk member removal - workspaceId: $workspaceId, memberCount: $memberCount, removedBy: $removedBy")
        } catch (e: Exception) {
            logger.error("Failed to log bulk member removal activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log bulk role update
     */
    @Transactional
    fun logBulkRoleUpdate(
        workspaceId: String,
        memberCount: Int,
        newRole: String,
        updatedBy: String,
        updatedByName: String,
        oldRole: String? = null,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "memberCount" to memberCount,
                    "newRole" to newRole,
                    "oldRole" to (oldRole ?: "VARIOUS")
                )
            )

            val activity = WorkspaceActivity.builder(updatedBy, updatedByName)
                .activityType(WorkspaceActivityType.BULK_ROLE_UPDATE)
                .description("Bulk role update for $memberCount members to $newRole")
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Bulk role update - workspaceId: $workspaceId, memberCount: $memberCount, newRole: $newRole, updatedBy: $updatedBy")
        } catch (e: Exception) {
            logger.error("Failed to log bulk role update activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log invitation sent
     */
    @Transactional
    fun logInvitationSent(
        workspaceId: String,
        email: String,
        role: String,
        invitedBy: String,
        invitedByName: String,
        invitationId: String? = null,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "email" to email,
                    "role" to role,
                    "invitationId" to (invitationId ?: "UNKNOWN")
                )
            )

            val activity = WorkspaceActivity.builder(invitedBy, invitedByName)
                .activityType(WorkspaceActivityType.INVITATION_SENT)
                .description("Invitation sent to '$email' for role $role")
                .targetEntity("invitation", invitationId ?: "UNKNOWN", email)
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Invitation sent - workspaceId: $workspaceId, email: $email, role: $role, invitedBy: $invitedBy")
        } catch (e: Exception) {
            logger.error("Failed to log invitation sent activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log invitation accepted
     */
    @Transactional
    fun logInvitationAccepted(
        workspaceId: String,
        email: String,
        acceptedBy: String,
        acceptedByName: String,
        invitationId: String? = null,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "email" to email,
                    "invitationId" to (invitationId ?: "UNKNOWN")
                )
            )

            val activity = WorkspaceActivity.builder(acceptedBy, acceptedByName)
                .activityType(WorkspaceActivityType.INVITATION_ACCEPTED)
                .description("Invitation accepted by '$email'")
                .targetEntity("invitation", invitationId ?: "UNKNOWN", email)
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Invitation accepted - workspaceId: $workspaceId, email: $email, acceptedBy: $acceptedBy")
        } catch (e: Exception) {
            logger.error("Failed to log invitation acceptance activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log invitation declined
     */
    @Transactional
    fun logInvitationDeclined(workspaceId: String, email: String, reason: String?, invitationId: String? = null) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "email" to email,
                    "reason" to (reason ?: "Not specified"),
                    "invitationId" to (invitationId ?: "UNKNOWN")
                )
            )

            val activity = WorkspaceActivity.builder("SYSTEM", "System")
                .activityType(WorkspaceActivityType.INVITATION_DECLINED)
                .description("Invitation declined by '$email'")
                .targetEntity("invitation", invitationId ?: "UNKNOWN", email)
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Invitation declined - workspaceId: $workspaceId, email: $email, reason: $reason")
        } catch (e: Exception) {
            logger.error("Failed to log invitation decline activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log invitation resent
     */
    @Transactional
    fun logInvitationResent(
        workspaceId: String,
        email: String,
        resentBy: String,
        resentByName: String,
        invitationId: String? = null,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "email" to email,
                    "invitationId" to (invitationId ?: "UNKNOWN")
                )
            )

            val activity = WorkspaceActivity.builder(resentBy, resentByName)
                .activityType(WorkspaceActivityType.INVITATION_RESENT)
                .description("Invitation resent to '$email'")
                .targetEntity("invitation", invitationId ?: "UNKNOWN", email)
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Invitation resent - workspaceId: $workspaceId, email: $email, resentBy: $resentBy")
        } catch (e: Exception) {
            logger.error("Failed to log invitation resent activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log invitation cancelled
     */
    @Transactional
    fun logInvitationCancelled(
        workspaceId: String,
        email: String,
        cancelledBy: String,
        cancelledByName: String,
        reason: String?,
        invitationId: String? = null,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "email" to email,
                    "reason" to (reason ?: "Not specified"),
                    "invitationId" to (invitationId ?: "UNKNOWN")
                )
            )

            val activity = WorkspaceActivity.builder(cancelledBy, cancelledByName)
                .activityType(WorkspaceActivityType.INVITATION_CANCELLED)
                .description("Invitation cancelled for '$email'")
                .targetEntity("invitation", invitationId ?: "UNKNOWN", email)
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Invitation cancelled - workspaceId: $workspaceId, email: $email, cancelledBy: $cancelledBy, reason: $reason")
        } catch (e: Exception) {
            logger.error("Failed to log invitation cancellation activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log bulk invitation resend
     */
    @Transactional
    fun logBulkInvitationResend(workspaceId: String, invitationCount: Int, resentBy: String, resentByName: String) {
        try {
            val contextData = objectMapper.writeValueAsString(mapOf("invitationCount" to invitationCount))

            val activity = WorkspaceActivity.builder(resentBy, resentByName)
                .activityType(WorkspaceActivityType.BULK_INVITATION_RESENT)
                .description("Bulk resend of $invitationCount invitations")
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Bulk invitation resend - workspaceId: $workspaceId, invitationCount: $invitationCount, resentBy: $resentBy")
        } catch (e: Exception) {
            logger.error("Failed to log bulk invitation resend activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log bulk invitation cancellation
     */
    @Transactional
    fun logBulkInvitationCancellation(
        workspaceId: String,
        invitationCount: Int,
        cancelledBy: String,
        cancelledByName: String,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(mapOf("invitationCount" to invitationCount))

            val activity = WorkspaceActivity.builder(cancelledBy, cancelledByName)
                .activityType(WorkspaceActivityType.BULK_INVITATION_CANCELLED)
                .description("Bulk cancellation of $invitationCount invitations")
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Bulk invitation cancellation - workspaceId: $workspaceId, invitationCount: $invitationCount, cancelledBy: $cancelledBy")
        } catch (e: Exception) {
            logger.error("Failed to log bulk invitation cancellation activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log bulk invitation revoke
     */
    @Transactional
    fun logBulkInvitationRevoke(workspaceId: String, invitationCount: Int, revokedBy: String, revokedByName: String) {
        try {
            val contextData = objectMapper.writeValueAsString(mapOf("invitationCount" to invitationCount))

            val activity = WorkspaceActivity.builder(revokedBy, revokedByName)
                .activityType(WorkspaceActivityType.BULK_INVITATION_REVOKED)
                .description("Bulk revocation of $invitationCount invitations")
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Bulk invitation revoke - workspaceId: $workspaceId, invitationCount: $invitationCount, revokedBy: $revokedBy")
        } catch (e: Exception) {
            logger.error("Failed to log bulk invitation revocation activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log settings updated
     */
    @Transactional
    fun logSettingsUpdated(
        workspaceId: String,
        section: String,
        updatedBy: String,
        updatedByName: String,
        changes: Map<String, Any>? = null,
    ) {
        try {
            val contextData = objectMapper.writeValueAsString(
                mapOf(
                    "section" to section,
                    "changes" to (changes ?: emptyMap<String, Any>())
                )
            )

            val activity = WorkspaceActivity.builder(updatedBy, updatedByName)
                .activityType(WorkspaceActivityType.SETTINGS_UPDATED)
                .description("Settings updated for section: $section")
                .targetEntity("settings", section, section)
                .contextData(contextData)
                .severity("INFO")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Settings updated - workspaceId: $workspaceId, section: $section, updatedBy: $updatedBy")
        } catch (e: Exception) {
            logger.error("Failed to log settings update activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Log settings reset
     */
    @Transactional
    fun logSettingsReset(workspaceId: String, section: String, resetBy: String, resetByName: String) {
        try {
            val contextData = objectMapper.writeValueAsString(mapOf("section" to section))

            val activity = WorkspaceActivity.builder(resetBy, resetByName)
                .activityType(WorkspaceActivityType.SETTINGS_RESET)
                .description("Settings reset for section: $section")
                .targetEntity("settings", section, section)
                .contextData(contextData)
                .severity("WARN")
                .workspaceId(workspaceId)
                .build()
                .apply {
                    addRequestContext(this)
                }

            activityRepository.save(activity)
            logger.info("ACTIVITY: Settings reset - workspaceId: $workspaceId, section: $section, resetBy: $resetBy")
        } catch (e: Exception) {
            logger.error("Failed to log settings reset activity for workspace: $workspaceId", e)
        }
    }

    /**
     * Get activity statistics for workspace
     */
    @Transactional(readOnly = true)
    fun getActivityStatistics(workspaceId: String, days: Int = 30): Map<String, Any> {
        return try {
            val sinceDate = LocalDateTime.now().minusDays(days.toLong())

            // Get statistics from database
            val activityStatsByType = activityRepository.getActivityStatsByType(workspaceId, sinceDate)
                .associate { it[0].toString() to (it[1] as Number).toLong() }

            val mostActiveUsers = activityRepository.getMostActiveUsers(
                workspaceId, sinceDate,
                Pageable.ofSize(5)
            ).map {
                mapOf(
                    "userId" to it[0].toString(),
                    "userName" to it[1].toString(),
                    "activityCount" to (it[2] as Number).toLong()
                )
            }

            val totalActivities = activityRepository.countByWorkspaceIdAndCreatedAtBetween(
                workspaceId, sinceDate, LocalDateTime.now()
            )

            val recentActivities = activityRepository.getRecentActivities(
                workspaceId, LocalDateTime.now().minusDays(1),
                Pageable.ofSize(10)
            ).size

            mapOf(
                "total_activities" to totalActivities,
                "recent_activities" to recentActivities,
                "most_active_users" to mostActiveUsers,
                "activity_by_type" to activityStatsByType,
                "period_days" to days
            )
        } catch (e: Exception) {
            logger.error("Failed to get activity statistics for workspace: $workspaceId", e)
            mapOf(
                "total_activities" to 0,
                "recent_activities" to 0,
                "most_active_users" to emptyList<String>(),
                "activity_by_type" to emptyMap<String, Int>(),
                "period_days" to days,
                "error" to "Failed to retrieve statistics"
            )
        }
    }

    /**
     * Delete all activities for workspace (used during workspace deletion)
     */
    @Transactional
    fun deleteActivities(workspaceId: String) {
        try {
            val deletedCount = activityRepository.deleteByWorkspaceId(workspaceId)
            logger.info("ACTIVITY: Deleted $deletedCount activities for workspace: $workspaceId")
        } catch (e: Exception) {
            logger.error("Failed to delete activities for workspace: $workspaceId", e)
        }
    }

    /**
     * Get paginated activities for a workspace
     */
    @Transactional(readOnly = true)
    fun getWorkspaceActivities(workspaceId: String, pageable: Pageable): Page<WorkspaceActivity> {
        return try {
            activityRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
        } catch (e: Exception) {
            logger.error("Failed to get workspace activities for workspace: $workspaceId", e)
            throw e
        }
    }

    /**
     * Search activities by term
     */
    @Transactional(readOnly = true)
    fun searchWorkspaceActivities(
        workspaceId: String,
        searchTerm: String,
        pageable: Pageable,
    ): Page<WorkspaceActivity> {
        return try {
            activityRepository.searchActivities(workspaceId, searchTerm, pageable)
        } catch (e: Exception) {
            logger.error("Failed to search workspace activities for workspace: $workspaceId", e)
            throw e
        }
    }

    /**
     * Get activity timeline for a specific entity
     */
    @Transactional(readOnly = true)
    fun getEntityActivityTimeline(workspaceId: String, entityType: String, entityId: String): List<WorkspaceActivity> {
        return try {
            activityRepository.getEntityTimeline(workspaceId, entityType, entityId)
        } catch (e: Exception) {
            logger.error("Failed to get entity timeline for workspace: $workspaceId, entity: $entityType/$entityId", e)
            emptyList()
        }
    }

    /**
     * Add request context (IP, user agent, session) to activity
     */
    private fun addRequestContext(activity: WorkspaceActivity) {
        try {
            val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            val request = requestAttributes?.request

            if (request != null) {
                activity.ipAddress = getClientIpAddress(request)
                activity.userAgent = request.getHeader("User-Agent")
                activity.sessionId = request.getHeader("X-Session-ID") ?: request.session?.id
            }
        } catch (e: Exception) {
            logger.debug("Could not extract request context for activity", e)
        }
    }

    /**
     * Get client IP address from request
     */
    private fun getClientIpAddress(request: HttpServletRequest): String? {
        val xForwardedForHeader = request.getHeader("X-Forwarded-For")
        if (!xForwardedForHeader.isNullOrBlank()) {
            return xForwardedForHeader.split(",")[0].trim()
        }

        val xRealIpHeader = request.getHeader("X-Real-IP")
        if (!xRealIpHeader.isNullOrBlank()) {
            return xRealIpHeader
        }

        return request.remoteAddr
    }
}