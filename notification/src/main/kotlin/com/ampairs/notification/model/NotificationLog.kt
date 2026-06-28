package com.ampairs.notification.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Persistent record of a notification surfaced to the in-app notification center.
 *
 * Workspace-scoped (tenant-filtered via `OwnableBaseDomain`). Each push the backend dispatches also
 * writes one of these so the app can pull a notification history through the canonical `/sync` feed
 * and round-trip the read/dismiss flags. `target_user_id = null` means the notification is for every
 * member of the workspace; otherwise only that user sees it.
 */
@Entity(name = "notification_log")
@Table(
    indexes = [
        Index(name = "idx_notification_log_uid", columnList = "uid", unique = true),
        Index(name = "idx_notification_log_owner", columnList = "owner_id"),
        Index(name = "idx_notification_log_owner_updated", columnList = "owner_id,updated_at"),
    ]
)
class NotificationLog : OwnableBaseDomain() {

    override fun obtainSeqIdPrefix(): String = "NL"

    /** Notification type — matches the app's FcmMessageTypes (order_update, workspace_invitation, …). */
    @Column(name = "type", length = 50, nullable = false)
    var type: String = ""

    @Column(name = "title", length = 255, nullable = false)
    var title: String = ""

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    var body: String = ""

    /** Optional structured data payload (JSON) — e.g. deep_link, entity_id. */
    @Column(name = "data_payload", columnDefinition = "TEXT")
    var dataPayload: String? = null

    /** null = visible to all workspace members; otherwise scoped to this user. */
    @Column(name = "target_user_id", length = 200)
    var targetUserId: String? = null

    @Column(name = "is_read", nullable = false)
    var read: Boolean = false

    /** Soft-delete / dismiss flag — the `/sync` feed returns inactive rows so dismissals propagate. */
    @Column(name = "active", nullable = false)
    var active: Boolean = true
}
