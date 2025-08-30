package com.ampairs.workspace.model.dto

import jakarta.validation.constraints.*

data class CreateInvitationRequest(
    @field:Email(message = "Invalid email format")
    val recipientEmail: String? = null,

    @field:Pattern(regexp = "\\+[1-9][0-9]{6,14}")
    val recipientPhone: String? = null,

    @field:NotBlank(message = "Role is required")
    val invitedRole: String,

    val recipientName: String? = null,
    val customMessage: String? = null,

    @field:Min(1)
    @field:Max(30)
    val expiresInDays: Int? = 7,

    val sendNotification: Boolean = true,
    val permissions: Set<String>? = null,

    @field:Size(max = 100)
    val department: String? = null,

    val teamIds: Set<String>? = null,
    val primaryTeamId: String? = null,
    val autoAssignTeams: Boolean = true,
    val welcomeTour: Boolean = true
) {
    fun getContactType(): String = when {
        !recipientEmail.isNullOrBlank() -> "email"
        !recipientPhone.isNullOrBlank() -> "phone"
        else -> throw IllegalStateException("Neither email nor phone provided")
    }

    fun getContactValue(): String {
        val value = when {
            !recipientEmail.isNullOrBlank() -> recipientEmail
            !recipientPhone.isNullOrBlank() -> recipientPhone
            else -> null
        }
        return value ?: throw IllegalStateException("Neither email nor phone provided")
    }

    fun isValid(): Boolean {
        return !recipientEmail.isNullOrBlank() || !recipientPhone.isNullOrBlank()
    }
}
