package com.ampairs.notification.domain.dto

import com.ampairs.notification.model.NotificationLog
import jakarta.validation.constraints.NotBlank

/**
 * Push payload from the app's `/notification/v1/notifications/feed/sync` POST.
 *
 * In practice the app only pushes read/dismiss changes for existing rows (it doesn't author
 * notifications), but the contract is symmetric and UID-keyed. `id` == uid.
 */
data class NotificationLogRequest(
    @field:NotBlank(message = "Notification id (uid) is required")
    val id: String,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val dataPayload: String? = null,
    val read: Boolean = false,
    val active: Boolean = true,
)

/**
 * Pull/echo payload returned by both GET and POST `/notification/v1/notifications/feed/sync`.
 * `updatedAt` is the server's ISO-8601 (UTC) timestamp string.
 */
data class NotificationLogResponse(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val dataPayload: String?,
    val read: Boolean,
    val active: Boolean,
    val updatedAt: String?,
)

fun NotificationLog.asResponse(): NotificationLogResponse = NotificationLogResponse(
    id = uid,
    type = type,
    title = title,
    body = body,
    dataPayload = dataPayload,
    read = read,
    active = active,
    updatedAt = updatedAt?.toString(),
)

/** Apply an app push. Notifications are server-authored, so only mutable client flags are honored. */
fun NotificationLog.applyRequest(request: NotificationLogRequest): NotificationLog = apply {
    if (uid.isBlank()) uid = request.id
    read = request.read
    active = request.active
}
