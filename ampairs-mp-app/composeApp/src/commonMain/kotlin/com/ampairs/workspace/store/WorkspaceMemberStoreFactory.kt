package com.ampairs.workspace.store

import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.MemberListResponse
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.db.entity.WorkspaceMemberEntity
import com.ampairs.workspace.domain.WorkspaceMember
import org.mobilenativefoundation.store.store5.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Store5 Factory for WorkspaceMember data management
 * Following Store5 guidelines for proper offline-first architecture
 */
typealias WorkspaceMemberStore = Store<WorkspaceMemberKey, List<WorkspaceMember>>

data class WorkspaceMemberKey(
    val userId: String, // Current logged-in user ID
    val workspaceId: String,
    val memberId: String? = null // null for list all members in workspace
)

class WorkspaceMemberStoreFactory(
    private val memberApi: WorkspaceMemberApi,
    private val memberDao: WorkspaceMemberDao,
) {

    fun create(): WorkspaceMemberStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<WorkspaceMemberKey, List<MemberListResponse>> {
        return Fetcher.of { key ->
            if (key.memberId != null) {
                // Fetch single member details
                val response = memberApi.getMemberDetails(key.workspaceId, key.memberId)
                if (response.data != null && response.error == null) {
                    // Convert MemberDetailsResponse to MemberListResponse format
                    val member = response.data!!
                    val memberListResponse = MemberListResponse(
                        id = member.id,
                        userId = member.userId,
                        user = member.user,
                        role = member.role,
                        isActive = member.isActive,
                        joinedAt = member.joinedAt,
                        lastActivityAt = member.lastActivityAt
                    )
                    listOf(memberListResponse)
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch member details")
                }
            } else {
                // Fetch paginated list of workspace members
                val response = memberApi.getWorkspaceMembers(workspaceId = key.workspaceId)
                if (response.data != null && response.error == null) {
                    response.data!!.content
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch workspace members")
                }
            }
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<WorkspaceMemberKey, List<MemberListResponse>, List<WorkspaceMember>> {
        return SourceOfTruth.of(
            reader = { key ->
                if (key.memberId != null) {
                    // Read single member
                    memberDao.getWorkspaceMemberForUserFlow(key.userId, key.workspaceId, key.memberId)
                        .map { entity -> entity?.let { listOf(it.toDomainModel()) } ?: emptyList() }
                } else {
                    // Read all workspace members
                    memberDao.getWorkspaceMembersForUser(key.userId, key.workspaceId)
                        .map { entities -> entities.map { it.toDomainModel() } }
                }
            },
            writer = { key, networkData ->
                // Convert network data to entities with proper context
                val entities = networkData.map { it.toEntityModel(key.userId, key.workspaceId) }
                
                if (key.memberId != null) {
                    // Update single member
                    entities.firstOrNull()?.let { memberDao.insertWorkspaceMember(it) }
                } else {
                    // For list operations, replace all data for this workspace
                    memberDao.deleteAllWorkspaceMembersForUser(key.userId, key.workspaceId)
                    memberDao.insertWorkspaceMembers(entities)
                }
            }
        )
    }
}

// Extension functions for data model conversions
private fun MemberListResponse.toEntityModel(userId: String, workspaceId: String): WorkspaceMemberEntity {
    return WorkspaceMemberEntity(
        id = this.id,
        user_id = userId, // Current logged-in user ID
        member_user_id = this.userId, // The actual member's user ID
        workspace_id = workspaceId,
        name = this.user?.getDisplayName() ?: "Unknown",
        email = this.user?.email ?: "",
        phone = this.user?.phone,
        role = this.role,
        status = if (this.isActive) "ACTIVE" else "INACTIVE",
        joined_at = this.joinedAt,
        last_activity = this.lastActivityAt,
        permissions = "", // Will be populated from other sources as JSON string
        avatar_url = this.user?.profilePictureUrl,
        // Default sync metadata
        sync_state = "SYNCED",
        last_synced_at = System.currentTimeMillis(),
        local_updated_at = System.currentTimeMillis(),
        server_updated_at = System.currentTimeMillis(),
        pending_changes = "",
        conflict_data = "",
        retry_count = 0
    )
}

private fun WorkspaceMemberEntity.toDomainModel(): WorkspaceMember {
    return WorkspaceMember(
        id = this.id,
        userId = this.member_user_id,
        workspaceId = this.workspace_id,
        name = this.name,
        email = this.email.ifEmpty { null },
        phone = this.phone,
        role = this.role,
        status = this.status,
        joinedAt = this.joined_at,
        lastActivity = this.last_activity,
        permissions = emptyMap(), // Will parse from JSON string if needed
        avatarUrl = this.avatar_url
    )
}