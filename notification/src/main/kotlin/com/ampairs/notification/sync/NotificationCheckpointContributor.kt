package com.ampairs.notification.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.notification.repository.NotificationLogRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the notification module's sync checkpoint (max `updatedAt` of notification logs) for
 * the current workspace. The checkpoint is workspace-wide while the pull feed is user-filtered, so
 * a row targeted at another user may trigger an occasional no-op incremental pull — harmless, and
 * far better than never pulling. `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class NotificationCheckpointContributor(
    private val notificationLogRepository: NotificationLogRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "notification_log" to notificationLogRepository.findMaxUpdatedAt(),
    )
}
