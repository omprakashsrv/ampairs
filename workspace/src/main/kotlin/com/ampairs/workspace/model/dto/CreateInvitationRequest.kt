package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.WorkspaceRole
import jakarta.validation.constraints.*

data class CreateInvitationRequest(
    @field:Email(message = "Invalid email format")
    val email: String? = null,

    @field:Pattern(regexp = "^[0-9]{10}$")
    val phone: String? = null,

    @field:Min(1)
    @field:Max(999)
    val countryCode: Int? = null,

    @field:NotNull(message = "Role is required")
    val invitedRole: WorkspaceRole,

    val name: String? = null,
    val message: String? = null,

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
        !email.isNullOrBlank() -> "email"
        !phone.isNullOrBlank() -> "phone"
        else -> throw IllegalStateException("Neither email nor phone provided")
    }

    fun getContactValue(): String {
        val value = when {
            !email.isNullOrBlank() -> email
            !phone.isNullOrBlank() -> phone
            else -> null
        }
        return value ?: throw IllegalStateException("Neither email nor phone provided")
    }

    fun isValid(): Boolean {
        return !email.isNullOrBlank() || !phone.isNullOrBlank()
    }
}
