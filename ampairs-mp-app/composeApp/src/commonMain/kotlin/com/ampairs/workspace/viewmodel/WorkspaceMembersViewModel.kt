package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.store.WorkspaceMemberStore
import com.ampairs.workspace.store.WorkspaceMemberKey
import com.ampairs.workspace.store.WorkspaceMemberUpdateStore
import com.ampairs.workspace.store.WorkspaceMemberUpdateKey
import com.ampairs.workspace.store.WorkspaceMemberUpdateRequest
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
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
    private val memberStore: WorkspaceMemberStore,
    private val memberUpdateStore: WorkspaceMemberUpdateStore,
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
                val key = WorkspaceMemberKey(
                    userId = "", // TODO: Get current user ID from auth repository
                    workspaceId = workspaceId,
                    memberId = null // null for list all members
                )
                val request = StoreReadRequest.cached(key, refresh = true)

                memberStore.stream(request).collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            allMembers = response.value
                            _state.value = _state.value.copy(
                                members = allMembers,
                                isLoading = false,
                                currentPage = page,
                                totalPages = if (response.value.size < size) page + 1 else page + 2, // Estimated
                                totalMembers = response.value.size, // Estimated
                                hasNextPage = response.value.size == size
                            )
                        }
                        is StoreReadResponse.Loading -> {
                            if (_state.value.members.isEmpty()) {
                                _state.value = _state.value.copy(isLoading = true)
                            }
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = response.error.message ?: "Failed to load members"
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                        else -> {
                            // Handle other states
                        }
                    }
                }

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
    @Suppress("unused")
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
    @Suppress("unused")
    fun searchMembers(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(members = allMembers)
            return
        }

        val searchResults = allMembers.filter { member ->
            member.name.contains(query, ignoreCase = true) ||
                    (member.email?.contains(query, ignoreCase = true) == true)
        }

        _state.value = _state.value.copy(members = searchResults)
    }

    /**
     * Update member role
     */
    @Suppress("unused")
    fun updateMemberRole(memberId: String, newRole: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val updateRequest = UpdateMemberRequest(
                    role = newRole,
                    reason = "Role updated via app"
                )

                // Use the Store5 update mechanism
                val updateKey = WorkspaceMemberUpdateKey(workspaceId, memberId)
                val updateRequestWrapper = WorkspaceMemberUpdateRequest(updateKey, updateRequest)
                
                // TODO: Implement Store5 update mechanism
                // For now, just update the local state optimistically

                // Update local state optimistically
                val updatedMembers = _state.value.members.map { member ->
                    if (member.id == memberId) member.copy(role = newRole) else member
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
    @Suppress("unused")
    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                // TODO: Use Store5 remove mechanism

                // Remove member from local state
                val updatedMembers = _state.value.members.filterNot { it.id == memberId }
                allMembers = updatedMembers

                _state.value = _state.value.copy(
                    members = updatedMembers,
                    isLoading = false,
                    totalMembers = (_state.value.totalMembers - 1).coerceAtLeast(0),
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
                // TODO: Use Store5 to load user role
                // For now, set a default role
                _state.value = _state.value.copy(currentUserRole = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Could not load user permissions: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error message
     */
    @Suppress("unused")
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    @Suppress("unused")
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
    val currentUserRole: UserRoleResponse? = null,
)