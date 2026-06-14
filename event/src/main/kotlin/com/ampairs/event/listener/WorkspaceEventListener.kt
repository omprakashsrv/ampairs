package com.ampairs.event.domain.listener

import com.ampairs.core.sync.EntityChangeType
import com.ampairs.core.sync.EntityChangedEvent
import com.ampairs.event.config.Constants
import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.dto.WorkspaceEventResponse
import com.ampairs.event.domain.events.*
import com.ampairs.event.kafka.WorkspaceEventKafkaProducer
import com.ampairs.event.repository.WorkspaceEventStore
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class WorkspaceEventListener(
    private val eventStore: WorkspaceEventStore,
    private val webSocketPublisher: SimpMessagingTemplate?,
    private val kafkaProducer: WorkspaceEventKafkaProducer? = null,
) {

    private val logger = LoggerFactory.getLogger(WorkspaceEventListener::class.java)
    private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    // Customer Events

    @EventListener
    @Async
    fun handleCustomerCreated(event: CustomerCreatedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.CUSTOMER_CREATED, "customer",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleCustomerUpdated(event: CustomerUpdatedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.CUSTOMER_UPDATED, "customer",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleCustomerDeleted(event: CustomerDeletedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.CUSTOMER_DELETED, "customer",
            event.entityId, event.deviceId, event.userId)
    }

    // Product Events

    @EventListener
    @Async
    fun handleProductCreated(event: ProductCreatedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.PRODUCT_CREATED, "product",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleProductUpdated(event: ProductUpdatedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.PRODUCT_UPDATED, "product",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleProductDeleted(event: ProductDeletedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.PRODUCT_DELETED, "product",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleProductStockChanged(event: ProductStockChangedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.PRODUCT_STOCK_CHANGED, "product",
            event.entityId, event.deviceId, event.userId)
    }

    // Order Events

    @EventListener
    @Async
    fun handleOrderCreated(event: OrderCreatedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.ORDER_CREATED, "order",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleOrderUpdated(event: OrderUpdatedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.ORDER_UPDATED, "order",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleOrderStatusChanged(event: OrderStatusChangedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.ORDER_STATUS_CHANGED, "order",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleOrderDeleted(event: OrderDeletedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.ORDER_DELETED, "order",
            event.entityId, event.deviceId, event.userId)
    }

    // Invoice Events

    @EventListener
    @Async
    fun handleInvoiceCreated(event: InvoiceCreatedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.INVOICE_CREATED, "invoice",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleInvoiceUpdated(event: InvoiceUpdatedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.INVOICE_UPDATED, "invoice",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleInvoicePaid(event: InvoicePaidEvent) {
        persistAndBroadcast(event.workspaceId, EventType.INVOICE_PAID, "invoice",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleInvoiceStatusChanged(event: InvoiceStatusChangedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.INVOICE_UPDATED, "invoice",
            event.entityId, event.deviceId, event.userId)
    }

    @EventListener
    @Async
    fun handleInvoiceDeleted(event: InvoiceDeletedEvent) {
        persistAndBroadcast(event.workspaceId, EventType.INVOICE_DELETED, "invoice",
            event.entityId, event.deviceId, event.userId)
    }

    // Generic entity change events (from core EntityChangePublisher — any entity type)

    @EventListener
    @Async
    fun handleEntityChanged(event: EntityChangedEvent) {
        val eventType = when (event.changeType) {
            EntityChangeType.CREATED -> EventType.ENTITY_CREATED
            EntityChangeType.UPDATED -> EventType.ENTITY_UPDATED
            EntityChangeType.DELETED -> EventType.ENTITY_DELETED
        }
        persistAndBroadcast(event.workspaceId, eventType, event.entityType,
            event.entityId, event.deviceId, event.userId)
    }

    private fun persistAndBroadcast(
        workspaceId: String,
        eventType: EventType,
        entityType: String,
        entityId: String,
        deviceId: String,
        userId: String,
    ) {
        try {
            val lastUpdatedAt = Instant.now()
            val payloadJson = objectMapper.writeValueAsString(
                mapOf("last_updated_at" to lastUpdatedAt.toString())
            )
            val upsert = eventStore.upsertWatermark(
                workspaceId = workspaceId,
                eventType = eventType,
                entityType = entityType,
                entityId = entityId,
                payload = payloadJson,
                deviceId = deviceId,
                userId = userId,
            )

            logger.debug(
                "Upserted watermark: workspace={}, entityType={}, entityId={}, seq={}",
                workspaceId, entityType, entityId, upsert.sequenceNumber,
            )

            val response = WorkspaceEventResponse(
                uid = upsert.uid,
                eventType = eventType,
                entityType = entityType,
                entityId = entityId,
                payload = mapOf("last_updated_at" to lastUpdatedAt.toString()),
                lastUpdatedAt = lastUpdatedAt,
                deviceId = deviceId,
                userId = userId,
                sequenceNumber = upsert.sequenceNumber,
                workspaceId = workspaceId,
                createdAt = upsert.createdAt,
            )
            val destination = Constants.WORKSPACE_EVENTS_TOPIC_PREFIX + workspaceId

            if (kafkaProducer != null) {
                kafkaProducer.publish(destination, response)
            } else {
                webSocketPublisher?.let { publisher ->
                    try {
                        publisher.convertAndSend(destination, response)
                        logger.debug("Broadcasted event to workspace: {}", workspaceId)
                    } catch (e: Exception) {
                        logger.error("Error broadcasting event via WebSocket", e)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error persisting/broadcasting event", e)
        }
    }
}
