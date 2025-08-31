package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.api.model.WorkspacePermission
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.db.OfflineFirstRolesPermissionsRepository
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Clean, refactored ViewModel for member details and editing functionality.
 *
 * Assumptions:
 * - repository methods return plain domain models or flows and throw exceptions on error.
 * - OfflineFirstRolesPermissionsRepository exposes cached data and Flows for fresh data.
 */
class MemberDetailsViewModel(
    private val workspaceId: String,
    private val memberId: String,
    private val memberRepository: WorkspaceMemberRepository,
    private val rolesPermissionsRepository: OfflineFirstRolesPermissionsRepository,
) : ViewModel() {

    data class MemberDetailsState(
        val isLoading: Boolean = false,
        val member: WorkspaceMember? = null,
        val originalMember: WorkspaceMember? = null,
        val displayMember: WorkspaceMember? = null,
        val userRole: UserRoleResponse? = null,
        val availableRoles: List<WorkspaceRole> = emptyList(),
        val availablePermissions: Map<String, Map<String, Boolean>> = emptyMap(),
        val canEdit: Boolean = false,
        val canChangeRole: Boolean = false,
        val canChangeStatus: Boolean = false,
        val canChangePermissions: Boolean = false,
        val canRemoveMember: Boolean = false,
        val hasChanges: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
    )

    private val _state = MutableStateFlow(MemberDetailsState())
    val state: StateFlow<MemberDetailsState> = _state.asStateFlow()

    private var pendingRole: String? = null
    private var pendingStatus: String? = null
    private var pendingPermissions: List<String>? = null

    private data class MemberPermissions(
        val canEdit: Boolean = false,
        val canChangeRole: Boolean = false,
        val canChangeStatus: Boolean = false,
        val canChangePermissions: Boolean = false,
        val canRemoveMember: Boolean = false,
    )

    init {
        loadMemberDetails()
        loadUserRole()
        loadAvailableRolesAndPermissions()
    }

    /**
     * Load member details (synchronous-like call wrapped in coroutine).
     */
    fun loadMemberDetails() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val member = memberRepository.getMemberDetails(workspaceId, memberId)
                _state.value = _state.value.copy(
                    isLoading = false,
                    member = member,
                    originalMember = member,
                    displayMember = member,
                    error = null
                )
                // Recalculate permissions now that member is loaded
                _state.value.userRole?.let { userRole ->
                    val perms = calculateMemberPermissions(userRole)
                    _state.value = _state.value.copy(
                        canEdit = perms.canEdit,
                        canChangeRole = perms.canChangeRole,
                        canChangeStatus = perms.canChangeStatus,
                        canChangePermissions = perms.canChangePermissions,
                        canRemoveMember = perms.canRemoveMember
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load member details"
                )
            }
        }
    }

    /**
     * Load current user's role (used to compute granular permissions)
     */
    private fun loadUserRole() {
        viewModelScope.launch {
            try {
                val userRole = memberRepository.getMyRole(workspaceId)
                _state.value = _state.value.copy(userRole = userRole)

                // Recalculate permissions if member already loaded
                val currentMember = _state.value.member
                if (currentMember != null) {
                    val perms = calculateMemberPermissions(userRole)
                    _state.value = _state.value.copy(
                        canEdit = perms.canEdit,
                        canChangeRole = perms.canChangeRole,
                        canChangeStatus = perms.canChangeStatus,
                        canChangePermissions = perms.canChangePermissions,
                        canRemoveMember = perms.canRemoveMember
                    )
                }
            } catch (e: Exception) {
                // Non-fatal: store an error but continue
                _state.value = _state.value.copy(
                    error = "Could not load user role: ${e.message}"
                )
            }
        }
    }

    /**
     * Load roles & permissions using an offline-first strategy: show cached data immediately,
     * then collect fresh flows to update state when network data becomes available.
     */
    private fun loadAvailableRolesAndPermissions() {
        viewModelScope.launch {
            try {
                // cached values for instant UI responsiveness
                val cachedRoles = rolesPermissionsRepository.getCachedRoles(workspaceId).firstOrNull() ?: emptyList()
                val cachedPermissions = rolesPermissionsRepository.getCachedPermissions(workspaceId)

                if (cachedRoles.isNotEmpty() || cachedPermissions.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        availableRoles = cachedRoles,
                        availablePermissions = cachedPermissions
                    )
                }

                // collect fresh roles
                launch {
                    try {
                        rolesPermissionsRepository.getWorkspaceRoles(workspaceId).collect { roles ->
                            _state.value = _state.value.copy(availableRoles = roles)
                        }
                    } catch (_: Exception) {
                        // ignore background errors
                    }
                }

                // collect fresh permissions
                launch {
                    try {
                        rolesPermissionsRepository.getWorkspacePermissions(workspaceId).collect { permissions ->
                            _state.value = _state.value.copy(availablePermissions = permissions)
                        }
                    } catch (_: Exception) {
                        // ignore background errors
                    }
                }

            } catch (_: Exception) {
                // ignore cached load errors; nothing critical
            }
        }
    }

    /**
     * Update pending role for UI (does not persist until saveMemberChanges)
     */
    fun updateRole(newRole: String) {
        val current = _state.value.member ?: return
        pendingRole = if (current.role == newRole) null else newRole
        updateDisplayMember()
        updateHasChanges()
    }

    /**
     * Update pending status for UI
     */
    fun updateStatus(newStatus: String) {
        val current = _state.value.member ?: return
        pendingStatus = if (current.status == newStatus) null else newStatus
        updateDisplayMember()
        updateHasChanges()
    }

    /**
     * Update pending custom permissions for UI
     */
    fun updatePermissions(newPermissions: List<String>) {
        pendingPermissions = newPermissions
        updateDisplayMember()
        updateHasChanges()
    }

    /**
     * Apply pending changes to the displayMember for immediate UI feedback
     */
    private fun updateDisplayMember() {
        val current = _state.value.member ?: return
        val display = current.copy(
            role = pendingRole ?: current.role,
            status = pendingStatus ?: current.status,
            permissions = pendingPermissions?.associateWith { true } ?: current.permissions
        )
        _state.value = _state.value.copy(displayMember = display)
    }

    /**
     * Persist pending changes to the server via repository
     */
    fun saveMemberChanges() {
        if (_state.value.member == null) return
         if (!_state.value.hasChanges) return

         viewModelScope.launch {
             _state.value = _state.value.copy(isLoading = true, error = null)

             try {
                 // Convert List<String> permissions to Set<WorkspacePermission>
                 val permissionSet = pendingPermissions?.mapNotNull { permission ->
                     WorkspacePermission.fromString(permission)
                 }?.toSet()
                 
                 val updateRequest = UpdateMemberRequest(
                     role = pendingRole,
                     customPermissions = permissionSet,
                     reason = "Updated via member details screen",
                     notifyMember = true
                 )

                 val updatedMember = memberRepository.updateMember(workspaceId, memberId, updateRequest)

                 _state.value = _state.value.copy(
                     isLoading = false,
                     member = updatedMember,
                     originalMember = updatedMember,
                     displayMember = updatedMember,
                     hasChanges = false,
                     successMessage = "Member updated successfully",
                     error = null
                 )

                 clearPendingChanges()
             } catch (e: Exception) {
                 _state.value = _state.value.copy(
                     isLoading = false,
                     error = e.message ?: "Failed to save changes"
                 )
             }
         }
     }

    /**
     * Remove member from workspace
     */
    fun removeMember() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                memberRepository.removeMember(workspaceId, memberId)

                // remove locally from UI
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "Member removed",
                    member = null,
                    originalMember = null,
                    displayMember = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to remove member"
                )
            }
        }
    }

    /**
     * Discard pending changes
     */
    fun discardChanges() {
        clearPendingChanges()
        updateHasChanges()
    }

    /**
     * Calculate granular member permissions based on current user role and loaded member
     */
    private fun calculateMemberPermissions(userRole: UserRoleResponse?): MemberPermissions {
        val currentMember = _state.value.member ?: return MemberPermissions()
        if (userRole == null) return MemberPermissions()

        val isOwner = userRole.currentRole == "OWNER" || (userRole.roleHierarchy["OWNER"] == true)
        val isAdmin = userRole.currentRole == "ADMIN" || (userRole.roleHierarchy["ADMIN"] == true)
        val canManageMembers = userRole.permissions["members"]?.get("manage") == true
        val canEditMembers = userRole.permissions["members"]?.get("edit") == true
        val canDeleteMembers = userRole.permissions["members"]?.get("delete") == true

        val targetIsOwner = currentMember.role == "OWNER"
        if (targetIsOwner && !isOwner) return MemberPermissions()

        val canEditThisRole = when {
            isOwner -> true
            isAdmin -> currentMember.role !in listOf("OWNER", "ADMIN")
            else -> currentMember.role in listOf("MEMBER", "VIEWER")
        }

        // evaluate change role permission in a way that avoids unreachable-condition warnings
        val canChangeRole = canManageMembers && (isOwner || (isAdmin && !targetIsOwner))
        val canEdit = (canManageMembers || canEditMembers) && canEditThisRole
        val canChangeStatus = canEdit
        val canChangePermissions = (isOwner || isAdmin) && canManageMembers && canEditThisRole
        val canRemoveMember = (canDeleteMembers || canManageMembers) && !targetIsOwner && canEditThisRole

        return MemberPermissions(
            canEdit = canEdit,
            canChangeRole = canChangeRole,
            canChangeStatus = canChangeStatus,
            canChangePermissions = canChangePermissions,
            canRemoveMember = canRemoveMember
        )
    }

    private fun updateHasChanges() {
        val hasChanges = pendingRole != null || pendingStatus != null || pendingPermissions != null
        _state.value = _state.value.copy(hasChanges = hasChanges)
    }

    private fun clearPendingChanges() {
        pendingRole = null
        pendingStatus = null
        pendingPermissions = null
        _state.value = _state.value.copy(displayMember = _state.value.originalMember)
    }

    @Suppress("unused")
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    @Suppress("unused")
    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun refresh() {
        loadMemberDetails()
        loadUserRole()
        loadAvailableRolesAndPermissions()
    }
}
