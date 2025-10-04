package com.ampairs.event.domain.events

/**
 * Event published when an order is created
 */
class OrderCreatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val orderNumber: String,
    val customerName: String,
    val totalAmount: Double
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "created",
            "orderNumber" to orderNumber,
            "customerName" to customerName,
            "totalAmount" to totalAmount
        )
    }
}

/**
 * Event published when an order is updated
 */
class OrderUpdatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val fieldChanges: Map<String, Any>
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "updated",
            "changes" to fieldChanges
        )
    }
}

/**
 * Event published when an order status changes
 */
class OrderStatusChangedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val orderNumber: String,
    val oldStatus: String,
    val newStatus: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "status_changed",
            "orderNumber" to orderNumber,
            "oldStatus" to oldStatus,
            "newStatus" to newStatus
        )
    }
}

/**
 * Event published when an order is deleted
 */
class OrderDeletedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val orderNumber: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "deleted",
            "orderNumber" to orderNumber
        )
    }
}
