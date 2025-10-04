package com.ampairs.event.domain.listener

import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
import com.ampairs.event.domain.dto.asWorkspaceEventResponse
import com.ampairs.event.domain.events.*
import com.ampairs.event.repository.WorkspaceEventRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class WorkspaceEventListener(
    private val eventRepository: WorkspaceEventRepository,
    private val webSocketPublisher: SimpMessagingTemplate?
) {

    private val logger = LoggerFactory.getLogger(WorkspaceEventListener::class.java)
    private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    // Customer Events

    @EventListener
    @Async
    @Transactional
    fun handleCustomerCreated(event: CustomerCreatedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.CUSTOMER_CREATED,
            entityType = "customer",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleCustomerUpdated(event: CustomerUpdatedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.CUSTOMER_UPDATED,
            entityType = "customer",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleCustomerDeleted(event: CustomerDeletedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.CUSTOMER_DELETED,
            entityType = "customer",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    // Product Events

    @EventListener
    @Async
    @Transactional
    fun handleProductCreated(event: ProductCreatedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.PRODUCT_CREATED,
            entityType = "product",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleProductUpdated(event: ProductUpdatedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.PRODUCT_UPDATED,
            entityType = "product",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleProductDeleted(event: ProductDeletedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.PRODUCT_DELETED,
            entityType = "product",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleProductStockChanged(event: ProductStockChangedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.PRODUCT_STOCK_CHANGED,
            entityType = "product",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    // Order Events

    @EventListener
    @Async
    @Transactional
    fun handleOrderCreated(event: OrderCreatedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.ORDER_CREATED,
            entityType = "order",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleOrderUpdated(event: OrderUpdatedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.ORDER_UPDATED,
            entityType = "order",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleOrderStatusChanged(event: OrderStatusChangedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.ORDER_STATUS_CHANGED,
            entityType = "order",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleOrderDeleted(event: OrderDeletedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.ORDER_DELETED,
            entityType = "order",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    // Invoice Events

    @EventListener
    @Async
    @Transactional
    fun handleInvoiceCreated(event: InvoiceCreatedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.INVOICE_CREATED,
            entityType = "invoice",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleInvoiceUpdated(event: InvoiceUpdatedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.INVOICE_UPDATED,
            entityType = "invoice",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleInvoicePaid(event: InvoicePaidEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.INVOICE_PAID,
            entityType = "invoice",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleInvoiceStatusChanged(event: InvoiceStatusChangedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.INVOICE_UPDATED,
            entityType = "invoice",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    @EventListener
    @Async
    @Transactional
    fun handleInvoiceDeleted(event: InvoiceDeletedEvent) {
        persistAndBroadcast(
            workspaceId = event.workspaceId,
            eventType = EventType.INVOICE_DELETED,
            entityType = "invoice",
            entityId = event.entityId,
            deviceId = event.deviceId,
            userId = event.userId,
            payload = event.getChanges()
        )
    }

    // Common persist and broadcast logic

    private fun persistAndBroadcast(
        workspaceId: String,
        eventType: EventType,
        entityType: String,
        entityId: String,
        deviceId: String,
        userId: String,
        payload: Map<String, Any>
    ) {
        try {
            // Get next sequence number
            val sequenceNumber = eventRepository.getNextSequenceNumber(workspaceId)

            // Persist event
            val workspaceEvent = WorkspaceEvent().apply {
                this.workspaceId = workspaceId
                this.eventType = eventType
                this.entityType = entityType
                this.entityId = entityId
                this.deviceId = deviceId
                this.userId = userId
                this.sequenceNumber = sequenceNumber
                this.payload = objectMapper.writeValueAsString(payload)
            }
            eventRepository.save(workspaceEvent)

            logger.debug(
                "Persisted event: type={}, entity={}, id={}, seq={}",
                eventType, entityType, entityId, sequenceNumber
            )

            // Broadcast to WebSocket (if available)
            webSocketPublisher?.let { publisher ->
                try {
                    publisher.convertAndSend(
                        "/topic/workspace/$workspaceId/events",
                        workspaceEvent.asWorkspaceEventResponse()
                    )
                    logger.debug("Broadcasted event to workspace: {}", workspaceId)
                } catch (e: Exception) {
                    logger.error("Error broadcasting event via WebSocket", e)
                }
            }

        } catch (e: Exception) {
            logger.error("Error persisting/broadcasting event", e)
        }
    }
}
