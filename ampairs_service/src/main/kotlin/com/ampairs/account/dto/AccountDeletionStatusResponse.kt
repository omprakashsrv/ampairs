package com.ampairs.account.dto

import java.time.Instant

/**
 * Response for checking account deletion status
 */
data class AccountDeletionStatusResponse(
    /**
     * Whether account is marked for deletion
     */
    val isDeleted: Boolean,

    /**
     * When the deletion was requested
     */
    val deletedAt: Instant?,

    /**
     * When permanent deletion will occur
     */
    val deletionScheduledFor: Instant?,

    /**
     * Number of days remaining until permanent deletion
     */
    val daysRemaining: Long?,

    /**
     * Whether user can still restore their account
     */
    val canRestore: Boolean,

    /**
     * Reason provided for deletion
     */
    val deletionReason: String?,

    /**
     * Current account status message
     */
    val statusMessage: String
)
