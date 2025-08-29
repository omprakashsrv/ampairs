package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.CreateInvitationRequest
import com.ampairs.workspace.api.model.ResendInvitationRequest
import com.ampairs.workspace.db.WorkspaceInvitationRepository
import com.ampairs.workspace.domain.InvitationAcceptanceResult
import com.ampairs.workspace.domain.WorkspaceInvitation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for workspace invitation management
 *
 * Manages invitation listing, creation, acceptance, resending, and cancellation
 * with proper state management and error handling.
 */
class WorkspaceInvitationsViewModel(
    private val workspaceId: String,
    private val invitationRepository: WorkspaceInvitationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceInvitationsState())
    val state = _state.asStateFlow()

    private var allInvitations = listOf<WorkspaceInvitation>()

    /**
     * Load workspace invitations from API
     */
    fun loadInvitations(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val pageResult = invitationRepository.getWorkspaceInvitations(
                    workspaceId = workspaceId,
                    page = page,
                    size = size,
                    sortBy = sortBy,
                    sortDir = sortDir
                )

                allInvitations = pageResult.content
                _state.value = _state.value.copy(
                    invitations = allInvitations,
                    isLoading = false,
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    totalInvitations = pageResult.totalElements,
                    hasNextPage = !pageResult.isLast
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load invitations"
                )
            }
        }
    }

    /**
     * Load more invitations for pagination
     */
    fun loadMoreInvitations() {
        val currentState = _state.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        loadInvitations(page = currentState.currentPage + 1)
    }

    /**
     * Filter invitations by status and role
     */
    fun filterInvitations(status: String, role: String) {
        val filteredInvitations = allInvitations.filter { invitation ->
            val matchesStatus = status == "ALL" || invitation.status == status
            val matchesRole = role == "ALL" || invitation.invitedRole == role
            matchesStatus && matchesRole
        }

        _state.value = _state.value.copy(
            invitations = filteredInvitations,
            activeFilters = mapOf("status" to status, "role" to role)
        )
    }

    /**
     * Search invitations by recipient name or email
     */
    fun searchInvitations(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(invitations = allInvitations)
            return
        }

        val searchResults = allInvitations.filter { invitation ->
            invitation.recipientEmail.contains(query, ignoreCase = true) ||
                    (invitation.recipientName?.contains(query, ignoreCase = true) == true)
        }

        _state.value = _state.value.copy(invitations = searchResults)
    }

    /**
     * Create and send new invitation
     */
    fun createInvitation(
        recipientEmail: String,
        recipientName: String? = null,
        invitedRole: String,
        customMessage: String? = null,
        expiresInDays: Int = 7,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val request = CreateInvitationRequest(
                    recipientEmail = recipientEmail,
                    recipientName = recipientName,
                    invitedRole = invitedRole,
                    customMessage = customMessage,
                    expiresInDays = expiresInDays,
                    sendNotification = true,
                    welcomeTour = true
                )

                val newInvitation = invitationRepository.createInvitation(
                    workspaceId = workspaceId,
                    request = request
                )

                // Add new invitation to local state
                val updatedInvitations = listOf(newInvitation) + _state.value.invitations
                allInvitations = updatedInvitations

                _state.value = _state.value.copy(
                    invitations = updatedInvitations,
                    isLoading = false,
                    totalInvitations = _state.value.totalInvitations + 1,
                    successMessage = "Invitation sent successfully to $recipientEmail"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send invitation"
                )
            }
        }
    }

    /**
     * Accept invitation using token (for invitation acceptance flow)
     */
    fun acceptInvitation(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val acceptanceResult = invitationRepository.acceptInvitation(token)

                _state.value = _state.value.copy(
                    isLoading = false,
                    acceptanceResult = acceptanceResult,
                    successMessage = "Welcome to ${acceptanceResult.workspaceName}!"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to accept invitation"
                )
            }
        }
    }

    /**
     * Resend invitation
     */
    fun resendInvitation(
        invitationId: String,
        updatedMessage: String? = null,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val request = ResendInvitationRequest(
                    updatedMessage = updatedMessage,
                    priorityDelivery = true,
                    includeReminder = true
                )

                val updatedInvitation = invitationRepository.resendInvitation(
                    workspaceId = workspaceId,
                    invitationId = invitationId,
                    request = request
                )

                // Update invitation in local state
                val updatedInvitations = _state.value.invitations.map { invitation ->
                    if (invitation.id == invitationId) updatedInvitation else invitation
                }

                allInvitations = updatedInvitations
                _state.value = _state.value.copy(
                    invitations = updatedInvitations,
                    isLoading = false,
                    successMessage = "Invitation resent successfully"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to resend invitation"
                )
            }
        }
    }

    /**
     * Cancel invitation
     */
    fun cancelInvitation(invitationId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val result = invitationRepository.cancelInvitation(workspaceId, invitationId)

                // Update invitation status in local state
                val updatedInvitations = _state.value.invitations.map { invitation ->
                    if (invitation.id == invitationId) {
                        invitation.copy(status = "CANCELLED")
                    } else invitation
                }

                allInvitations = updatedInvitations
                _state.value = _state.value.copy(
                    invitations = updatedInvitations,
                    isLoading = false,
                    successMessage = "Invitation cancelled successfully"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to cancel invitation"
                )
            }
        }
    }

    /**
     * Get invitation statistics
     */
    fun getInvitationStats(): InvitationStats {
        val invitations = _state.value.invitations
        return InvitationStats(
            totalSent = invitations.size,
            pending = invitations.count { it.status == "PENDING" },
            accepted = invitations.count { it.status == "ACCEPTED" },
            expired = invitations.count { it.status == "EXPIRED" },
            cancelled = invitations.count { it.status == "CANCELLED" },
            declined = invitations.count { it.status == "DECLINED" }
        )
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
     * Clear acceptance result
     */
    fun clearAcceptanceResult() {
        _state.value = _state.value.copy(acceptanceResult = null)
    }

    /**
     * Refresh invitations data
     */
    fun refresh() {
        loadInvitations()
    }
}

/**
 * UI state for workspace invitations screen
 */
data class WorkspaceInvitationsState(
    val invitations: List<WorkspaceInvitation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalInvitations: Int = 0,
    val hasNextPage: Boolean = false,
    val activeFilters: Map<String, String> = emptyMap(),
    val acceptanceResult: InvitationAcceptanceResult? = null,
)

/**
 * Invitation statistics data class
 */
data class InvitationStats(
    val totalSent: Int,
    val pending: Int,
    val accepted: Int,
    val expired: Int,
    val cancelled: Int,
    val declined: Int,
) {
    val acceptanceRate: Double get() = if (totalSent > 0) (accepted.toDouble() / totalSent) * 100 else 0.0
    val pendingRate: Double get() = if (totalSent > 0) (pending.toDouble() / totalSent) * 100 else 0.0
}