package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.flower_core.Resource
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for member details and editing functionality
 */
class MemberDetailsViewModel(
    private val workspaceId: String,
    private val memberId: String,
    private val memberRepository: WorkspaceMemberRepository,
) : ViewModel() {

    data class MemberDetailsState(
        val isLoading: Boolean = false,
        val member: WorkspaceMember? = null,
        val originalMember: WorkspaceMember? = null,
        val userRole: UserRoleResponse? = null,
        val error: String? = null,
        val canEdit: Boolean = false,
        val hasChanges: Boolean = false,
    )

    private val _state = MutableStateFlow(MemberDetailsState())
    val state: StateFlow<MemberDetailsState> = _state.asStateFlow()

    private var pendingRole: String? = null
    private var pendingStatus: String? = null
    private var pendingPermissions: List<String>? = null

    init {
        loadUserRole()
    }

    /**
     * Load member details from the repository
     */
    fun loadMemberDetails() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val memberDetails = memberRepository.getMemberDetails(workspaceId, memberId)
                when (memberDetails.status) {
                    is Resource.Status.Success -> {
                        val member = memberDetails.status.data
                        _state.value = _state.value.copy(
                            isLoading = false,
                            member = member,
                            originalMember = member,
                            error = null
                        )
                    }
                    is Resource.Status.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = memberDetails.status.errorMessage
                        )
                    }
                    is Resource.Status.Loading -> {
                        _state.value = _state.value.copy(isLoading = true)
                    }
                    is Resource.Status.EmptySuccess -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Member not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }

    /**
     * Load current user's role and permissions
     */
    private fun loadUserRole() {
        viewModelScope.launch {
            try {
                val userRoleResponse = memberRepository.getMyRole(workspaceId)
                when (userRoleResponse.status) {
                    is Resource.Status.Success -> {
                        val userRole = userRoleResponse.status.data
                        _state.value = _state.value.copy(
                            userRole = userRole,
                            canEdit = if (userRole != null) canEditMember(userRole) else false
                        )
                    }
                    is Resource.Status.Error -> {
                        // User role loading failed, but don't show error for this
                        _state.value = _state.value.copy(canEdit = false)
                    }
                    is Resource.Status.Loading -> {
                        // Loading state handled by main loading
                    }
                    is Resource.Status.EmptySuccess -> {
                        _state.value = _state.value.copy(canEdit = false)
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(canEdit = false)
            }
        }
    }

    /**
     * Update member role
     */
    fun updateRole(newRole: String) {
        val currentMember = _state.value.member ?: return
        if (currentMember.role == newRole) {
            pendingRole = null
        } else {
            pendingRole = newRole
        }
        updateHasChanges()
    }

    /**
     * Update member status
     */
    fun updateStatus(newStatus: String) {
        val currentMember = _state.value.member ?: return
        if (currentMember.status == newStatus) {
            pendingStatus = null
        } else {
            pendingStatus = newStatus
        }
        updateHasChanges()
    }

    /**
     * Update member permissions
     */
    fun updatePermissions(newPermissions: List<String>) {
        pendingPermissions = newPermissions
        updateHasChanges()
    }

    /**
     * Save member changes
     */
    fun saveMemberChanges() {
        val currentMember = _state.value.member ?: return
        if (!_state.value.hasChanges) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val updateRequest = UpdateMemberRequest(
                    role = pendingRole,
                    customPermissions = pendingPermissions,
                    reason = "Updated via member details screen",
                    notifyMember = true
                )

                val result = memberRepository.updateMember(workspaceId, memberId, updateRequest)
                when (result.status) {
                    is Resource.Status.Success -> {
                        val updatedMember = result.status.data
                        _state.value = _state.value.copy(
                            isLoading = false,
                            member = updatedMember,
                            originalMember = updatedMember,
                            hasChanges = false,
                            error = null
                        )
                        // Clear pending changes
                        clearPendingChanges()
                    }
                    is Resource.Status.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.status.errorMessage
                        )
                    }
                    is Resource.Status.Loading -> {
                        _state.value = _state.value.copy(isLoading = true)
                    }
                    is Resource.Status.EmptySuccess -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Update failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred while updating member"
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
     * Remove member from workspace
     */
    fun removeMember() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val result = memberRepository.removeMember(workspaceId, memberId)
                when (result.status) {
                    is Resource.Status.Success -> {
                        // Member removed successfully, screen should navigate back
                        _state.value = _state.value.copy(isLoading = false)
                    }
                    is Resource.Status.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.status.errorMessage
                        )
                    }
                    is Resource.Status.Loading -> {
                        _state.value = _state.value.copy(isLoading = true)
                    }
                    is Resource.Status.EmptySuccess -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Remove failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred while removing member"
                )
            }
        }
    }

    /**
     * Check if current user can edit the member
     */
    private fun canEditMember(userRole: UserRoleResponse): Boolean {
        val currentMember = _state.value.member
        
        // Can't edit if member is null
        if (currentMember == null) return false
        
        // Can't edit owner (unless you are the owner editing yourself)
        if (currentMember.role == "OWNER" && userRole.currentRole != "OWNER") return false
        
        // Check if user has member management permissions
        return userRole.permissions["members"]?.get("manage") == true ||
                userRole.roleHierarchy["ADMIN"] == true ||
                userRole.roleHierarchy["OWNER"] == true
    }

    /**
     * Update hasChanges flag based on pending changes
     */
    private fun updateHasChanges() {
        val hasChanges = pendingRole != null || 
                        pendingStatus != null || 
                        pendingPermissions != null
        
        _state.value = _state.value.copy(hasChanges = hasChanges)
    }

    /**
     * Clear all pending changes
     */
    private fun clearPendingChanges() {
        pendingRole = null
        pendingStatus = null
        pendingPermissions = null
    }
}