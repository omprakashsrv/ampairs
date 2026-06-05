package com.ampairs.core.sync

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * One-liner helper for domain services to emit a real-time change signal for an entity. Resolves
 * the workspace, user and device from the ambient request context, so callers only pass the entity
 * type and id. No-op when there is no workspace context (e.g. system/background flows).
 *
 * Usage: `entityChangePublisher.updated("customer_group", group.uid)`
 */
@Component
class EntityChangePublisher(
    private val publisher: ApplicationEventPublisher,
) {
    fun publish(entityType: String, entityId: String, changeType: EntityChangeType) {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return
        val userId = runCatching {
            SecurityContextHolder.getContext().authentication
                ?.let { AuthenticationHelper.getCurrentUserId(it) }
        }.getOrNull() ?: ""
        val deviceId = DeviceContextHolder.getCurrentDevice() ?: ""
        publisher.publishEvent(
            EntityChangedEvent(this, workspaceId, entityType, entityId, changeType, userId, deviceId)
        )
    }

    fun created(entityType: String, entityId: String) = publish(entityType, entityId, EntityChangeType.CREATED)
    fun updated(entityType: String, entityId: String) = publish(entityType, entityId, EntityChangeType.UPDATED)
    fun deleted(entityType: String, entityId: String) = publish(entityType, entityId, EntityChangeType.DELETED)
}
