package com.ampairs.core.sync

import org.springframework.context.ApplicationEvent

/** The kind of change that occurred to an entity. */
enum class EntityChangeType { CREATED, UPDATED, DELETED }

/**
 * Generic, module-agnostic entity-change event published from `core` so any domain module can
 * signal a change without depending on the `event` module. The `event` module listens for this and
 * broadcasts a slim real-time signal (entity type + change time) to the workspace's connected
 * clients. Correctness still comes from the data-derived `/sync/checkpoints` bootstrap; this event
 * only lowers latency.
 *
 * @property entityType the mobile `SyncEntity` code (e.g. "customer_group", "unit", "tax").
 */
class EntityChangedEvent(
    source: Any,
    val workspaceId: String,
    val entityType: String,
    val entityId: String,
    val changeType: EntityChangeType,
    val userId: String,
    val deviceId: String,
) : ApplicationEvent(source)
