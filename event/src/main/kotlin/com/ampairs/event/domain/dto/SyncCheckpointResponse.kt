package com.ampairs.event.domain.dto

import java.time.Instant

/**
 * Response for `GET /event/v1/sync/checkpoints`.
 *
 * @property checkpoints map of `SyncEntity` code -> `max(updatedAt)` for the current workspace.
 *           A `null` value means the entity has no rows; the client must not pull it.
 * @property serverTime authoritative server clock at response time (diagnostic / skew reference).
 */
data class SyncCheckpointResponse(
    val checkpoints: Map<String, Instant?>,
    val serverTime: Instant
)
