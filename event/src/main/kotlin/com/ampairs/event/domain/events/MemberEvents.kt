package com.ampairs.event.domain.events

/**
 * Event published when a workspace member is added
 */
class MemberAddedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val memberUserId: String,
    val role: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "added",
            "memberUserId" to memberUserId,
            "role" to role
        )
    }
}

/**
 * Event published when a workspace member is removed
 */
class MemberRemovedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val memberUserId: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "removed",
            "memberUserId" to memberUserId
        )
    }
}

/**
 * Event published when a workspace member is activated
 */
class MemberActivatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val memberUserId: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "activated",
            "memberUserId" to memberUserId
        )
    }
}

/**
 * Event published when a workspace member is deactivated
 */
class MemberDeactivatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val memberUserId: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "deactivated",
            "memberUserId" to memberUserId
        )
    }
}
