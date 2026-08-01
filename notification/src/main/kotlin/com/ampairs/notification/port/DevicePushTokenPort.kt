package com.ampairs.notification.port

/**
 * A push-delivery target — a single device's FCM token plus identifying metadata.
 */
data class PushTarget(
    val token: String,
    val tokenType: String?,
    val deviceId: String,
    val userId: String?,
)

/**
 * Port the notification module uses to resolve device push tokens, implemented by the module that
 * owns device registrations (subscription). Defined here so notification has no compile dependency
 * on subscription — Spring wires the implementation at runtime.
 */
interface DevicePushTokenPort {

    /** Active device tokens for every member device in a workspace. */
    fun tokensForWorkspace(workspaceId: String): List<PushTarget>

    /** Active device tokens for a specific user (across their workspaces). */
    fun tokensForUser(userId: String): List<PushTarget>

    /** Clear a token FCM reported as unregistered/invalid so we stop sending to it. */
    fun invalidateToken(token: String)
}
