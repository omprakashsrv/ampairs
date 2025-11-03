package com.ampairs.account.dto

import jakarta.validation.constraints.Size

/**
 * Request to delete user account
 */
data class AccountDeletionRequest(
    /**
     * Optional reason for account deletion
     */
    @field:Size(max = 500, message = "Deletion reason must not exceed 500 characters")
    val reason: String? = null,

    /**
     * Confirmation that user understands data will be deleted
     */
    val confirmed: Boolean = false
)
