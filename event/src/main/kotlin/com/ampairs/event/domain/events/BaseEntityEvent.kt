package com.ampairs.event.domain.events

import org.springframework.context.ApplicationEvent

/**
 * Base class for all entity change events
 */
abstract class BaseEntityEvent(
    source: Any,
    open val workspaceId: String,
    open val entityId: String,
    open val userId: String,
    open val deviceId: String
) : ApplicationEvent(source) {

    /**
     * Get changes as map for payload
     */
    abstract fun getChanges(): Map<String, Any>
}
