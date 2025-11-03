package com.ampairs.account.dto

import java.time.Instant

/**
 * Response after requesting account deletion
 */
data class AccountDeletionResponse(
    /**
     * Unique identifier of the user
     */
    val userId: String,

    /**
     * Whether deletion request was successful
     */
    val deletionRequested: Boolean,

    /**
     * When the deletion was requested
     */
    val deletedAt: Instant?,

    /**
     * When permanent deletion will occur (end of grace period)
     */
    val deletionScheduledFor: Instant?,

    /**
     * Number of days until permanent deletion
     */
    val daysUntilPermanentDeletion: Long?,

    /**
     * Message describing the deletion status
     */
    val message: String,

    /**
     * List of workspaces where user is sole owner (blocks deletion)
     */
    val blockingWorkspaces: List<WorkspaceOwnershipInfo>? = null,

    /**
     * Whether user can cancel the deletion
     */
    val canRestore: Boolean = false
)

/**
 * Information about workspace ownership that blocks deletion
 */
data class WorkspaceOwnershipInfo(
    val workspaceId: String,
    val workspaceName: String,
    val workspaceSlug: String,
    val memberCount: Int
)
