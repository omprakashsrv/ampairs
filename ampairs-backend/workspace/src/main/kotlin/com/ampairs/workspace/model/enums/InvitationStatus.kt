package com.ampairs.workspace.model.enums

/**
 * Status of workspace invitations throughout their lifecycle
 */
enum class InvitationStatus(val displayName: String, val isFinal: Boolean) {
    /**
     * Invitation has been sent but not yet responded to
     */
    PENDING("Pending", false),

    /**
     * Invitation was accepted and user joined the workspace
     */
    ACCEPTED("Accepted", true),

    /**
     * Invitation was declined by the recipient
     */
    DECLINED("Declined", true),

    /**
     * Invitation expired without response
     */
    EXPIRED("Expired", true),

    /**
     * Invitation was cancelled by the inviter
     */
    CANCELLED("Cancelled", true),

    /**
     * Invitation was revoked due to security or policy reasons
     */
    REVOKED("Revoked", true);

    /**
     * Check if invitation can be acted upon
     */
    fun isActionable(): Boolean {
        return this == PENDING
    }
}