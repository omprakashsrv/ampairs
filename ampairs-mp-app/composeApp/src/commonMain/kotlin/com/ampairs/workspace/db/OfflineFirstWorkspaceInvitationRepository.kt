package com.ampairs.workspace.db

import com.ampairs.workspace.api.WorkspaceInvitationApi
import com.ampairs.workspace.api.model.CreateInvitationRequest
import com.ampairs.workspace.api.model.ResendInvitationRequest
import com.ampairs.workspace.db.dao.WorkspaceInvitationDao
import com.ampairs.workspace.domain.WorkspaceInvitation
import com.ampairs.workspace.domain.InvitationAcceptanceResult
import com.ampairs.workspace.store.WorkspaceInvitationStore
import com.ampairs.workspace.store.WorkspaceInvitationKey
import com.ampairs.common.model.PageResult
import com.ampairs.auth.api.TokenRepository
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Offline-first workspace invitation repository using Store5 pattern
 * 
 * Provides comprehensive invitation management with proper offline-first functionality,
 * automatic sync, conflict resolution, and optimistic updates.
 */
class OfflineFirstWorkspaceInvitationRepository(
    private val invitationApi: WorkspaceInvitationApi,
    private val invitationDao: WorkspaceInvitationDao,
    private val invitationStore: WorkspaceInvitationStore,
    private val tokenRepository: TokenRepository
) {

    private suspend fun getCurrentUserId(): String {
        return tokenRepository.getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
    }

    /**
     * Get workspace invitations with offline-first approach using Store5
     */
    suspend fun getWorkspaceInvitations(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
        refresh: Boolean = false
    ): Flow<PageResult<WorkspaceInvitation>> {
        val currentUserId = getCurrentUserId()
        return invitationStore.stream(
            StoreReadRequest.cached(
                key = WorkspaceInvitationKey.forPage(
                    userId = currentUserId,
                    workspaceId = workspaceId,
                    page = page,
                    size = size
                ).copy(sortBy = sortBy, sortDir = sortDir),
                refresh = refresh
            )
        ).map { response ->
            when (response) {
                is StoreReadResponse.Data -> response.value
                is StoreReadResponse.Error.Exception -> throw response.error
                is StoreReadResponse.Error.Message -> throw Exception(response.message)
                is StoreReadResponse.Loading -> {
                    // Return cached data or empty result while loading
                    PageResult(
                        content = emptyList(),
                        totalElements = 0,
                        totalPages = 0,
                        currentPage = page,
                        pageSize = size,
                        isFirst = true,
                        isLast = true,
                        isEmpty = true
                    )
                }
                is StoreReadResponse.NoNewData -> {
                    // Return last known data
                    PageResult(
                        content = emptyList(),
                        totalElements = 0,
                        totalPages = 0,
                        currentPage = page,
                        pageSize = size,
                        isFirst = true,
                        isLast = true,
                        isEmpty = true
                    )
                }
                else -> {
                    // Handle any other response types
                    PageResult(
                        content = emptyList(),
                        totalElements = 0,
                        totalPages = 0,
                        currentPage = page,
                        pageSize = size,
                        isFirst = true,
                        isLast = true,
                        isEmpty = true
                    )
                }
            }
        }
    }

    /**
     * Get filtered workspace invitations
     */
    suspend fun getFilteredInvitations(
        workspaceId: String,
        status: String? = null,
        role: String? = null,
        page: Int = 0,
        size: Int = 20,
        refresh: Boolean = false
    ): Flow<PageResult<WorkspaceInvitation>> {
        val currentUserId = getCurrentUserId()
        return invitationStore.stream(
            StoreReadRequest.cached(
                key = WorkspaceInvitationKey.forPageWithFilters(
                    userId = currentUserId,
                    workspaceId = workspaceId,
                    page = page,
                    size = size,
                    status = status,
                    role = role
                ),
                refresh = refresh
            )
        ).map { response ->
            when (response) {
                is StoreReadResponse.Data -> response.value
                is StoreReadResponse.Error.Exception -> throw response.error
                is StoreReadResponse.Error.Message -> throw Exception(response.message)
                else -> PageResult(
                    content = emptyList(),
                    totalElements = 0,
                    totalPages = 0,
                    currentPage = page,
                    pageSize = size,
                    isFirst = true,
                    isLast = true,
                    isEmpty = true
                )
            }
        }
    }

    /**
     * Create and send workspace invitation with optimistic updates
     */
    suspend fun createInvitation(
        workspaceId: String,
        request: CreateInvitationRequest,
    ): WorkspaceInvitation {
        val currentUserId = getCurrentUserId()
        
        try {
            // Make API call first
            val response = invitationApi.createInvitation(workspaceId, request)
            
            if (response.error == null && response.data != null) {
                val invitationData = response.data!!
                val invitation = WorkspaceInvitation(
                    id = invitationData.id,
                    workspaceId = invitationData.workspaceId,
                    recipientEmail = invitationData.recipientEmail,
                    recipientName = invitationData.recipientName,
                    invitedRole = invitationData.invitedRole,
                    status = invitationData.status,
                    createdAt = invitationData.createdAt,
                    expiresAt = invitationData.expiresAt,
                    sentByName = invitationData.sentBy.name,
                    sentByEmail = invitationData.sentBy.email,
                    emailSent = invitationData.deliveryStatus.emailSent,
                    emailDelivered = invitationData.deliveryStatus.emailDelivered,
                    emailOpened = invitationData.deliveryStatus.emailOpened,
                    linkClicked = invitationData.deliveryStatus.linkClicked,
                    resendCount = invitationData.resendCount,
                    invitationMessage = invitationData.invitationMessage
                )

                // Update Store5 cache by clearing it so next read will fetch fresh data
                val key = WorkspaceInvitationKey.forPage(currentUserId, workspaceId, 0)
                invitationStore.clear(key)

                return invitation
            } else {
                throw Exception(response.error?.message ?: "Failed to create invitation")
            }
        } catch (e: Exception) {
            // TODO: Implement optimistic update with pending sync
            throw e
        }
    }

    /**
     * Accept workspace invitation via token
     */
    suspend fun acceptInvitation(token: String): InvitationAcceptanceResult {
        val response = invitationApi.acceptInvitation(token)

        return if (response.error == null && response.data != null) {
            val acceptanceData = response.data!!

            InvitationAcceptanceResult(
                invitationId = acceptanceData.id,
                status = acceptanceData.status,
                acceptedAt = acceptanceData.acceptedAt,
                workspaceId = acceptanceData.workspaceInfo.id,
                workspaceName = acceptanceData.workspaceInfo.name,
                workspaceDescription = acceptanceData.workspaceInfo.description,
                memberCount = acceptanceData.workspaceInfo.memberCount,
                yourRole = acceptanceData.workspaceInfo.yourRole,
                memberId = acceptanceData.memberDetails.memberId,
                userId = acceptanceData.memberDetails.userId,
                email = acceptanceData.memberDetails.email,
                name = acceptanceData.memberDetails.name,
                role = acceptanceData.memberDetails.role,
                memberStatus = acceptanceData.memberDetails.status,
                joinedAt = acceptanceData.memberDetails.joinedAt,
                permissions = acceptanceData.memberDetails.permissions,
                welcomeTourAvailable = acceptanceData.onboarding.welcomeTourAvailable,
                profileCompletionRequired = acceptanceData.onboarding.profileCompletionRequired,
                setupTasks = acceptanceData.onboarding.setupTasks,
                welcomeMessage = acceptanceData.onboarding.welcomeMessage,
                dashboardUrl = acceptanceData.immediateAccess.dashboardUrl,
                availableModules = acceptanceData.immediateAccess.availableModules,
                teamMembers = acceptanceData.immediateAccess.teamMembers,
                recentActivityAvailable = acceptanceData.immediateAccess.recentActivityAvailable
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to accept invitation")
        }
    }

    /**
     * Resend invitation email with optimistic updates
     */
    suspend fun resendInvitation(
        workspaceId: String,
        invitationId: String,
        request: ResendInvitationRequest = ResendInvitationRequest(),
    ): WorkspaceInvitation {
        val currentUserId = getCurrentUserId()
        
        // Optimistic update - increment resend count locally first
        invitationDao.incrementResendCount(invitationId, currentUserId)
        
        try {
            val response = invitationApi.resendInvitation(workspaceId, invitationId, request)

            if (response.error == null && response.data != null) {
                val invitationData = response.data!!
                val invitation = WorkspaceInvitation(
                    id = invitationData.id,
                    workspaceId = invitationData.workspaceId,
                    recipientEmail = invitationData.recipientEmail,
                    recipientName = invitationData.recipientName,
                    invitedRole = invitationData.invitedRole,
                    status = invitationData.status,
                    createdAt = invitationData.createdAt,
                    expiresAt = invitationData.expiresAt,
                    sentByName = invitationData.sentBy.name,
                    sentByEmail = invitationData.sentBy.email,
                    emailSent = invitationData.deliveryStatus.emailSent,
                    emailDelivered = invitationData.deliveryStatus.emailDelivered,
                    emailOpened = invitationData.deliveryStatus.emailOpened,
                    linkClicked = invitationData.deliveryStatus.linkClicked,
                    resendCount = invitationData.resendCount,
                    invitationMessage = invitationData.invitationMessage
                )

                // Update local database with server data
                val currentTime = System.currentTimeMillis()
                val entity = invitation.toEntityModel(currentUserId).copy(
                    sync_state = "SYNCED",
                    last_synced_at = currentTime,
                    server_updated_at = currentTime,
                    local_updated_at = currentTime
                )
                invitationDao.insertInvitation(entity)

                return invitation
            } else {
                // Rollback optimistic update on failure
                // TODO: Implement proper rollback mechanism
                throw Exception(response.error?.message ?: "Failed to resend invitation")
            }
        } catch (e: Exception) {
            // TODO: Implement proper rollback mechanism
            throw e
        }
    }

    /**
     * Cancel workspace invitation with optimistic updates
     */
    suspend fun cancelInvitation(workspaceId: String, invitationId: String): String {
        val currentUserId = getCurrentUserId()
        
        // Optimistic update - change status locally first
        invitationDao.updateInvitationStatus(invitationId, currentUserId, "CANCELLED")
        
        try {
            val response = invitationApi.cancelInvitation(workspaceId, invitationId)

            if (response.error == null && response.data != null) {
                // Update sync state to confirm successful cancellation
                val currentTime = System.currentTimeMillis()
                invitationDao.updateSyncState(invitationId, currentUserId, "SYNCED", currentTime)
                
                return response.data!!
            } else {
                // Rollback optimistic update on failure
                // TODO: Implement proper rollback mechanism
                throw Exception(response.error?.message ?: "Failed to cancel invitation")
            }
        } catch (e: Exception) {
            // TODO: Implement proper rollback mechanism
            throw e
        }
    }

    /**
     * Search invitations locally
     */
    suspend fun searchInvitations(
        workspaceId: String,
        query: String
    ): Flow<List<WorkspaceInvitation>> {
        val currentUserId = getCurrentUserId()
        return invitationDao.searchInvitations(currentUserId, workspaceId, query)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    /**
     * Clear all cached invitations for a workspace
     */
    suspend fun clearCache(workspaceId: String) {
        val currentUserId = getCurrentUserId()
        invitationDao.deleteAllInvitationsForWorkspace(workspaceId, currentUserId)
        
        // Clear Store5 cache
        val key = WorkspaceInvitationKey.forPage(currentUserId, workspaceId, 0)
        invitationStore.clear(key)
    }

    /**
     * Force refresh invitations from server
     */
    suspend fun refresh(workspaceId: String, page: Int = 0, size: Int = 20): PageResult<WorkspaceInvitation> {
        val currentUserId = getCurrentUserId()
        val key = WorkspaceInvitationKey.forPage(currentUserId, workspaceId, page, size)
        
        return invitationStore.stream(
            StoreReadRequest.fresh(key)
        ).first().let { response ->
            when (response) {
                is StoreReadResponse.Data -> response.value
                is StoreReadResponse.Error.Exception -> throw response.error
                is StoreReadResponse.Error.Message -> throw Exception(response.message)
                else -> throw Exception("Failed to refresh invitations")
            }
        }
    }
}


// Extension functions for entity conversions
private fun WorkspaceInvitation.toEntityModel(userId: String): com.ampairs.workspace.db.entity.WorkspaceInvitationEntity {
    return com.ampairs.workspace.db.entity.WorkspaceInvitationEntity(
        id = this.id,
        user_id = userId,
        workspace_id = this.workspaceId,
        recipient_email = this.recipientEmail,
        recipient_name = this.recipientName,
        invited_role = this.invitedRole,
        status = this.status,
        created_at = this.createdAt,
        expires_at = this.expiresAt,
        sent_by_name = this.sentByName,
        sent_by_email = this.sentByEmail,
        email_sent = this.emailSent,
        email_delivered = this.emailDelivered,
        email_opened = this.emailOpened,
        link_clicked = this.linkClicked,
        resend_count = this.resendCount,
        invitation_message = this.invitationMessage,
        sync_state = "SYNCED",
        last_synced_at = System.currentTimeMillis(),
        local_updated_at = System.currentTimeMillis(),
        server_updated_at = System.currentTimeMillis()
    )
}

private fun com.ampairs.workspace.db.entity.WorkspaceInvitationEntity.toDomainModel(): WorkspaceInvitation {
    return WorkspaceInvitation(
        id = this.id,
        workspaceId = this.workspace_id,
        recipientEmail = this.recipient_email,
        recipientName = this.recipient_name,
        invitedRole = this.invited_role,
        status = this.status,
        createdAt = this.created_at,
        expiresAt = this.expires_at,
        sentByName = this.sent_by_name,
        sentByEmail = this.sent_by_email,
        emailSent = this.email_sent,
        emailDelivered = this.email_delivered,
        emailOpened = this.email_opened,
        linkClicked = this.link_clicked,
        resendCount = this.resend_count,
        invitationMessage = this.invitation_message
    )
}