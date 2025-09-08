package com.ampairs.workspace.model.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request DTO for responding to invitation
 */
data class InvitationResponseRequest(
    @field:NotBlank(message = "Token is required")
    val token: String,

    val accept: Boolean = true,

    @field:Size(max = 200, message = "Reason must not exceed 200 characters")
    val reason: String? = null,
)

/**
 * Request DTO for resending invitation
 */
data class ResendInvitationRequest(
    val expiresInDays: Int = 7,

    @field:Size(max = 500, message = "Message must not exceed 500 characters")
    val message: String? = null,
)

/**
 * Request DTO for bulk invitation operations
 */
data class BulkInvitationRequest(
    val invitationIds: List<String>,

    val action: String, // "resend", "cancel", "revoke"

    @field:Size(max = 200, message = "Reason must not exceed 200 characters")
    val reason: String? = null,
)