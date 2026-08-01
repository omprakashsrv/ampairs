package com.ampairs.event.domain.listener

import com.ampairs.core.sync.EntityChangeType
import com.ampairs.core.sync.EntityChangedEvent
import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.dto.WorkspaceEventResponse
import com.ampairs.event.domain.events.CustomerCreatedEvent
import com.ampairs.event.domain.events.CustomerDeletedEvent
import com.ampairs.event.domain.events.CustomerUpdatedEvent
import com.ampairs.event.domain.events.InvoiceCreatedEvent
import com.ampairs.event.domain.events.InvoiceDeletedEvent
import com.ampairs.event.domain.events.InvoicePaidEvent
import com.ampairs.event.domain.events.InvoiceStatusChangedEvent
import com.ampairs.event.domain.events.InvoiceUpdatedEvent
import com.ampairs.event.domain.events.OrderCreatedEvent
import com.ampairs.event.domain.events.OrderDeletedEvent
import com.ampairs.event.domain.events.OrderStatusChangedEvent
import com.ampairs.event.domain.events.OrderUpdatedEvent
import com.ampairs.event.domain.events.ProductCreatedEvent
import com.ampairs.event.domain.events.ProductDeletedEvent
import com.ampairs.event.domain.events.ProductStockChangedEvent
import com.ampairs.event.domain.events.ProductUpdatedEvent
import com.ampairs.event.kafka.WorkspaceEventKafkaProducer
import com.ampairs.event.repository.WorkspaceEventStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

class WorkspaceEventListenerTest {

    private val eventStore: WorkspaceEventStore = mock()
    private val webSocket: SimpMessagingTemplate = mock()
    private val kafkaProducer: WorkspaceEventKafkaProducer = mock()

    private val src = Any()

    @BeforeEach
    fun setUp() {
        whenever(eventStore.upsertWatermark(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(WorkspaceEventStore.UpsertResult("EVT-1", Instant.now(), 7L))
    }

    private fun wsListener() = WorkspaceEventListener(eventStore, webSocket, kafkaProducer = null)
    private fun kafkaListener() = WorkspaceEventListener(eventStore, webSocket, kafkaProducer)

    @Test
    fun `customer created persists watermark and broadcasts via websocket`() {
        wsListener().handleCustomerCreated(CustomerCreatedEvent(src, "ws-1", "CUS-1", "u", "d", "Acme", "RETAIL"))

        verify(eventStore).upsertWatermark(
            eq("ws-1"), eq(EventType.CUSTOMER_CREATED), eq("customer"), eq("CUS-1"), any(), eq("d"), eq("u"),
        )
        val dest = argumentCaptor<String>()
        val resp = argumentCaptor<WorkspaceEventResponse>()
        verify(webSocket).convertAndSend(dest.capture(), resp.capture())
        assertTrue(dest.firstValue.endsWith("ws-1"))
        assertEquals(EventType.CUSTOMER_CREATED, resp.firstValue.eventType)
        assertEquals(7L, resp.firstValue.sequenceNumber)
    }

    @Test
    fun `customer updated and deleted map to correct event types`() {
        wsListener().handleCustomerUpdated(CustomerUpdatedEvent(src, "ws-1", "CUS-1", "u", "d", mapOf("x" to 1)))
        verify(eventStore).upsertWatermark(any(), eq(EventType.CUSTOMER_UPDATED), eq("customer"), any(), any(), any(), any())

        wsListener().handleCustomerDeleted(CustomerDeletedEvent(src, "ws-1", "CUS-1", "u", "d", "Acme"))
        verify(eventStore).upsertWatermark(any(), eq(EventType.CUSTOMER_DELETED), eq("customer"), any(), any(), any(), any())
    }

    @Test
    fun `product handlers map to product event types`() {
        val l = wsListener()
        l.handleProductCreated(ProductCreatedEvent(src, "ws-1", "PRD-1", "u", "d", "W", "S"))
        l.handleProductUpdated(ProductUpdatedEvent(src, "ws-1", "PRD-1", "u", "d", mapOf("x" to 1)))
        l.handleProductDeleted(ProductDeletedEvent(src, "ws-1", "PRD-1", "u", "d", "W"))
        l.handleProductStockChanged(ProductStockChangedEvent(src, "ws-1", "PRD-1", "u", "d", "W", 1.0, 2.0))

        verify(eventStore).upsertWatermark(any(), eq(EventType.PRODUCT_CREATED), eq("product"), any(), any(), any(), any())
        verify(eventStore).upsertWatermark(any(), eq(EventType.PRODUCT_UPDATED), eq("product"), any(), any(), any(), any())
        verify(eventStore).upsertWatermark(any(), eq(EventType.PRODUCT_DELETED), eq("product"), any(), any(), any(), any())
        verify(eventStore).upsertWatermark(any(), eq(EventType.PRODUCT_STOCK_CHANGED), eq("product"), any(), any(), any(), any())
    }

    @Test
    fun `order handlers map to order event types`() {
        val l = wsListener()
        l.handleOrderCreated(OrderCreatedEvent(src, "ws-1", "ORD-1", "u", "d", "O-1", "Acme", 10.0))
        l.handleOrderUpdated(OrderUpdatedEvent(src, "ws-1", "ORD-1", "u", "d", mapOf("x" to 1)))
        l.handleOrderStatusChanged(OrderStatusChangedEvent(src, "ws-1", "ORD-1", "u", "d", "O-1", "A", "B"))
        l.handleOrderDeleted(OrderDeletedEvent(src, "ws-1", "ORD-1", "u", "d", "O-1"))

        verify(eventStore).upsertWatermark(any(), eq(EventType.ORDER_CREATED), eq("order"), any(), any(), any(), any())
        verify(eventStore).upsertWatermark(any(), eq(EventType.ORDER_STATUS_CHANGED), eq("order"), any(), any(), any(), any())
        verify(eventStore).upsertWatermark(any(), eq(EventType.ORDER_DELETED), eq("order"), any(), any(), any(), any())
    }

    @Test
    fun `invoice handlers map to invoice event types`() {
        val l = wsListener()
        l.handleInvoiceCreated(InvoiceCreatedEvent(src, "ws-1", "INV-1", "u", "d", "I-1", "Acme", 10.0))
        l.handleInvoiceUpdated(InvoiceUpdatedEvent(src, "ws-1", "INV-1", "u", "d", mapOf("x" to 1)))
        l.handleInvoicePaid(InvoicePaidEvent(src, "ws-1", "INV-1", "u", "d", "I-1", 5.0, "CASH"))
        l.handleInvoiceStatusChanged(InvoiceStatusChangedEvent(src, "ws-1", "INV-1", "u", "d", "I-1", "A", "B"))
        l.handleInvoiceDeleted(InvoiceDeletedEvent(src, "ws-1", "INV-1", "u", "d", "I-1"))

        verify(eventStore).upsertWatermark(any(), eq(EventType.INVOICE_CREATED), eq("invoice"), any(), any(), any(), any())
        verify(eventStore).upsertWatermark(any(), eq(EventType.INVOICE_PAID), eq("invoice"), any(), any(), any(), any())
        verify(eventStore).upsertWatermark(any(), eq(EventType.INVOICE_DELETED), eq("invoice"), any(), any(), any(), any())
        // status changed maps to INVOICE_UPDATED
        verify(eventStore, org.mockito.kotlin.times(2))
            .upsertWatermark(any(), eq(EventType.INVOICE_UPDATED), eq("invoice"), any(), any(), any(), any())
    }

    @Test
    fun `generic entity changed maps change type to entity event type`() {
        wsListener().handleEntityChanged(
            EntityChangedEvent(src, "ws-1", "unit", "UNT-1", EntityChangeType.CREATED, "u", "d")
        )
        verify(eventStore).upsertWatermark(any(), eq(EventType.ENTITY_CREATED), eq("unit"), any(), any(), any(), any())

        wsListener().handleEntityChanged(
            EntityChangedEvent(src, "ws-1", "tax", "TAX-1", EntityChangeType.DELETED, "u", "d")
        )
        verify(eventStore).upsertWatermark(any(), eq(EventType.ENTITY_DELETED), eq("tax"), any(), any(), any(), any())
    }

    @Test
    fun `kafka producer is preferred over websocket when present`() {
        kafkaListener().handleCustomerCreated(CustomerCreatedEvent(src, "ws-1", "CUS-1", "u", "d", "Acme", "RETAIL"))

        verify(kafkaProducer).publish(any(), any())
        verify(webSocket, never()).convertAndSend(any<String>(), any<WorkspaceEventResponse>())
    }

    @Test
    fun `broadcast failure from websocket is swallowed`() {
        whenever(webSocket.convertAndSend(any<String>(), any<WorkspaceEventResponse>()))
            .thenThrow(RuntimeException("ws down"))

        // Should not throw
        wsListener().handleCustomerCreated(CustomerCreatedEvent(src, "ws-1", "CUS-1", "u", "d", "Acme", "RETAIL"))
    }
}
