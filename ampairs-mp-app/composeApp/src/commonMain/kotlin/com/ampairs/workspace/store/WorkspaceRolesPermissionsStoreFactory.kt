package com.ampairs.workspace.store

import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.api.model.WorkspacePermissionResponse
import com.ampairs.workspace.db.dao.WorkspaceRoleDao
import com.ampairs.workspace.db.dao.WorkspacePermissionDao
import com.ampairs.workspace.db.entity.WorkspaceRoleEntity
import com.ampairs.workspace.db.entity.WorkspacePermissionEntity
import com.ampairs.workspace.db.convertPermissionsListToMap
import org.mobilenativefoundation.store.store5.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Store5 Factory for Workspace Roles data management
 */
typealias WorkspaceRolesStore = Store<WorkspaceRolesKey, List<WorkspaceRole>>

data class WorkspaceRolesKey(
    val workspaceId: String,
    val userId: String
)

class WorkspaceRolesStoreFactory(
    private val memberApi: WorkspaceMemberApi,
    private val roleDao: WorkspaceRoleDao,
) {

    fun create(): WorkspaceRolesStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<WorkspaceRolesKey, List<WorkspaceRole>> {
        return Fetcher.of { key ->
            val response = memberApi.getAvailableRoles(key.workspaceId)
            if (response.data != null && response.error == null) {
                response.data!!
            } else {
                throw Exception(response.error?.message ?: "Failed to fetch workspace roles")
            }
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<WorkspaceRolesKey, List<WorkspaceRole>, List<WorkspaceRole>> {
        return SourceOfTruth.of(
            reader = { key ->
                roleDao.getWorkspaceRolesForUser(key.userId, key.workspaceId)
                    .map { entities -> entities.map { it.toApiModel() } }
            },
            writer = { key, networkData ->
                // Clear old data
                roleDao.deleteAllWorkspaceRolesForUser(key.userId, key.workspaceId)
                
                // Insert new data
                val entities = networkData.map { it.toEntity(key.userId, key.workspaceId) }
                roleDao.insertWorkspaceRoles(entities)
            }
        )
    }

}

/**
 * Store5 Factory for Workspace Permissions data management
 */
typealias WorkspacePermissionsStore = Store<WorkspacePermissionsKey, Map<String, Map<String, Boolean>>>

data class WorkspacePermissionsKey(
    val workspaceId: String,
    val userId: String
)

class WorkspacePermissionsStoreFactory(
    private val memberApi: WorkspaceMemberApi,
    private val permissionDao: WorkspacePermissionDao,
) {

    fun create(): WorkspacePermissionsStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<WorkspacePermissionsKey, Map<String, Map<String, Boolean>>> {
        return Fetcher.of { key ->
            val response = memberApi.getAvailablePermissions(key.workspaceId)
            if (response.data != null && response.error == null) {
                convertPermissionsListToMap(response.data!!)
            } else {
                throw Exception(response.error?.message ?: "Failed to fetch workspace permissions")
            }
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<WorkspacePermissionsKey, Map<String, Map<String, Boolean>>, Map<String, Map<String, Boolean>>> {
        return SourceOfTruth.of(
            reader = { key ->
                permissionDao.getWorkspacePermissionsForUser(key.userId, key.workspaceId)
                    .map { entities -> entities.toPermissionMap() }
            },
            writer = { key, networkData ->
                // Clear old data
                permissionDao.deleteAllWorkspacePermissionsForUser(key.userId, key.workspaceId)
                
                // Convert map back to permission response list and then to entities
                val permissionResponses = networkData.flatMap { (module, actions) ->
                    actions.filter { it.value }.map { (action, _) ->
                        WorkspacePermissionResponse(
                            name = "${module}_${action}".uppercase(),
                            permissionName = "${module}_${action}".uppercase(),
                            description = "Permission for $module $action"
                        )
                    }
                }
                
                val entities = permissionResponses.map { it.toPermissionEntity(key.userId, key.workspaceId) }
                permissionDao.insertWorkspacePermissions(entities)
            }
        )
    }

}

// Extension functions to handle the entity conversions
private fun List<WorkspacePermissionEntity>.toPermissionMap(): Map<String, Map<String, Boolean>> {
    return this.associate { entity ->
        val actions = try {
            kotlinx.serialization.json.Json.decodeFromString<Map<String, Boolean>>(entity.actions)
        } catch (e: Exception) {
            emptyMap()
        }
        entity.module to actions
    }
}

// Extension functions for entity conversions
private fun WorkspaceRole.toEntity(userId: String, workspaceId: String): WorkspaceRoleEntity {
    return WorkspaceRoleEntity(
        id = this.name,
        user_id = userId,
        workspace_id = workspaceId,
        name = this.name,
        description = this.description,
        level = this.level,
        is_system_role = true,
        permissions = Json.encodeToString(this.manageableRoles),
        can_manage_members = this.manageableRoles.isNotEmpty(),
        can_edit_workspace = this.level >= 80,
        can_delete_workspace = this.level >= 100,
        created_at = "",
        updated_at = "",
        sync_state = "SYNCED",
        last_synced_at = System.currentTimeMillis(),
        local_updated_at = System.currentTimeMillis(),
        server_updated_at = System.currentTimeMillis()
    )
}

private fun WorkspaceRoleEntity.toApiModel(): WorkspaceRole {
    return WorkspaceRole(
        name = this.name,
        displayName = this.name, // Use name as display name if not stored separately
        description = this.description,
        level = this.level,
        manageableRoles = try {
            Json.decodeFromString<List<String>>(this.permissions)
        } catch (e: Exception) {
            emptyList()
        }
    )
}

private fun WorkspacePermissionResponse.toPermissionEntity(userId: String, workspaceId: String): WorkspacePermissionEntity {
    return WorkspacePermissionEntity(
        user_id = userId,
        workspace_id = workspaceId,
        module = this.name.split("_").first(),
        actions = "{}",
        description = this.description,
        sync_state = "SYNCED",
        last_synced_at = System.currentTimeMillis(),
        local_updated_at = System.currentTimeMillis(),
        server_updated_at = System.currentTimeMillis()
    )
}