package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.WorkspaceRole
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request DTO for creating workspace invitation
 */
data class CreateInvitationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @JsonProperty("email")
    val email: String,

    @JsonProperty("role")
    val role: WorkspaceRole = WorkspaceRole.MEMBER,

    @field:Size(max = 500, message = "Message must not exceed 500 characters")
    @JsonProperty("message")
    val message: String? = null,

    @JsonProperty("expires_in_days")
    val expiresInDays: Int = 7,

    @JsonProperty("send_email")
    val sendEmail: Boolean = true,
)

/**
 * Request DTO for responding to invitation
 */
data class InvitationResponseRequest(
    @field:NotBlank(message = "Token is required")
    @JsonProperty("token")
    val token: String,

    @JsonProperty("accept")
    val accept: Boolean = true,

    @field:Size(max = 200, message = "Reason must not exceed 200 characters")
    @JsonProperty("reason")
    val reason: String? = null,
)

/**
 * Request DTO for resending invitation
 */
data class ResendInvitationRequest(
    @JsonProperty("expires_in_days")
    val expiresInDays: Int = 7,

    @field:Size(max = 500, message = "Message must not exceed 500 characters")
    @JsonProperty("message")
    val message: String? = null,
)

/**
 * Request DTO for bulk invitation operations
 */
data class BulkInvitationRequest(
    @JsonProperty("invitation_ids")
    val invitationIds: List<String>,

    @JsonProperty("action")
    val action: String, // "resend", "cancel", "revoke"

    @field:Size(max = 200, message = "Reason must not exceed 200 characters")
    @JsonProperty("reason")
    val reason: String? = null,
)