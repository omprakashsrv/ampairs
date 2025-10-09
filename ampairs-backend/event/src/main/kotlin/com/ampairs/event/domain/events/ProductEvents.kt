package com.ampairs.event.domain.events

/**
 * Event published when a product is created
 */
class ProductCreatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val productName: String,
    val sku: String?
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "created",
            "name" to productName,
            "sku" to (sku ?: "")
        )
    }
}

/**
 * Event published when a product is updated
 */
class ProductUpdatedEvent(
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
 * Event published when a product is deleted
 */
class ProductDeletedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val productName: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "deleted",
            "name" to productName
        )
    }
}

/**
 * Event published when product stock changes
 */
class ProductStockChangedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val productName: String,
    val oldStock: Double,
    val newStock: Double
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "stock_changed",
            "name" to productName,
            "oldStock" to oldStock,
            "newStock" to newStock,
            "difference" to (newStock - oldStock)
        )
    }
}
