package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.db.UserRole
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for workspace member management
 *
 * Manages member listing, filtering, role updates, and removal operations
 * with proper state management and error handling.
 */
class WorkspaceMembersViewModel(
    private val workspaceId: String,
    private val memberRepository: WorkspaceMemberRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceMembersState())
    val state = _state.asStateFlow()

    private var allMembers = listOf<WorkspaceMember>()

    init {
        loadCurrentUserRole()
    }

    /**
     * Load workspace members from API
     */
    fun loadMembers(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val pageResult = memberRepository.getWorkspaceMembers(
                    workspaceId = workspaceId,
                    page = page,
                    size = size,
                    sortBy = sortBy,
                    sortDir = sortDir
                )

                allMembers = pageResult.content
                _state.value = _state.value.copy(
                    members = allMembers,
                    isLoading = false,
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    totalMembers = pageResult.totalElements,
                    hasNextPage = !pageResult.isLast
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load members"
                )
            }
        }
    }

    /**
     * Load more members for pagination
     */
    fun loadMoreMembers() {
        val currentState = _state.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        loadMembers(page = currentState.currentPage + 1)
    }

    /**
     * Filter members by role and status
     */
    fun filterMembers(role: String, status: String) {
        val filteredMembers = allMembers.filter { member ->
            val matchesRole = role == "ALL" || member.role == role
            val matchesStatus = status == "ALL" || member.status == status
            matchesRole && matchesStatus
        }

        _state.value = _state.value.copy(
            members = filteredMembers,
            activeFilters = mapOf("role" to role, "status" to status)
        )
    }

    /**
     * Search members by name or email
     */
    fun searchMembers(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(members = allMembers)
            return
        }

        val searchResults = allMembers.filter { member ->
            member.name.contains(query, ignoreCase = true) ||
                    member.email.contains(query, ignoreCase = true)
        }

        _state.value = _state.value.copy(members = searchResults)
    }

    /**
     * Update member role
     */
    fun updateMemberRole(memberId: String, newRole: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val updateRequest = UpdateMemberRequest(
                    role = newRole,
                    reason = "Role updated via mobile app"
                )

                val updatedMember = memberRepository.updateMember(
                    workspaceId = workspaceId,
                    memberId = memberId,
                    request = updateRequest
                )

                // Update local state
                val updatedMembers = _state.value.members.map { member ->
                    if (member.id == memberId) updatedMember else member
                }

                allMembers = updatedMembers
                _state.value = _state.value.copy(
                    members = updatedMembers,
                    isLoading = false,
                    successMessage = "Member role updated successfully"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update member role"
                )
            }
        }
    }

    /**
     * Remove member from workspace
     */
    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val result = memberRepository.removeMember(workspaceId, memberId)

                // Remove member from local state
                val updatedMembers = _state.value.members.filterNot { it.id == memberId }
                allMembers = updatedMembers

                _state.value = _state.value.copy(
                    members = updatedMembers,
                    isLoading = false,
                    totalMembers = _state.value.totalMembers - 1,
                    successMessage = "Member removed successfully"
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
     * Load current user's role and permissions
     */
    private fun loadCurrentUserRole() {
        viewModelScope.launch {
            try {
                val userRole = memberRepository.getMyRole(workspaceId)
                _state.value = _state.value.copy(currentUserRole = userRole)
            } catch (e: Exception) {
                // Non-critical error, continue without user role info
                _state.value = _state.value.copy(
                    error = "Could not load user permissions: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    /**
     * Refresh members data
     */
    fun refresh() {
        loadMembers()
        loadCurrentUserRole()
    }
}

/**
 * UI state for workspace members screen
 */
data class WorkspaceMembersState(
    val members: List<WorkspaceMember> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalMembers: Int = 0,
    val hasNextPage: Boolean = false,
    val activeFilters: Map<String, String> = emptyMap(),
    val currentUserRole: UserRole? = null,
)