package com.ampairs.order.service

import com.ampairs.core.domain.model.Address
import com.ampairs.core.service.ConfirmedLineItem
import com.ampairs.event.domain.events.OrderCreatedEvent
import com.ampairs.event.domain.events.OrderStatusChangedEvent
import com.ampairs.event.domain.events.OrderUpdatedEvent
import com.ampairs.event.domain.kafka.EcomOrderLineItemPayload
import com.ampairs.event.domain.kafka.EcomOrderPlacedEvent
import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import com.ampairs.form.domain.model.EntityType
import com.ampairs.invoice.domain.dto.InvoiceResponse
import com.ampairs.invoice.service.InvoiceService
import com.ampairs.order.domain.dto.OrderItemRequest
import com.ampairs.order.domain.dto.OrderUpdateRequest
import com.ampairs.order.domain.dto.toInvoice
import com.ampairs.order.domain.dto.toOrder
import com.ampairs.order.domain.dto.toOrderItems
import com.ampairs.order.domain.dto.toResponse
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.model.Order
import com.ampairs.order.domain.model.OrderItem
import com.ampairs.order.kafka.EcomOrderStatusProducer
import com.ampairs.order.repository.OrderItemRepository
import com.ampairs.order.repository.OrderPagingRepository
import com.ampairs.order.repository.OrderRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceLogicTest {

    @Mock private lateinit var orderRepository: OrderRepository
    @Mock private lateinit var orderItemRepository: OrderItemRepository
    @Mock private lateinit var orderPagingRepository: OrderPagingRepository
    @Mock private lateinit var invoiceService: InvoiceService
    @Mock private lateinit var eventPublisher: ApplicationEventPublisher
    @Mock private lateinit var ecomOrderStatusProducer: EcomOrderStatusProducer

    private lateinit var service: OrderService
    private lateinit var ecomService: OrderEcomServiceImpl
    private lateinit var ingestionService: EcomOrderIngestionService

    @BeforeEach
    fun setUp() {
        service = OrderService(orderRepository, orderItemRepository, orderPagingRepository, invoiceService, eventPublisher)
        ecomService = OrderEcomServiceImpl(orderRepository, ecomOrderStatusProducer)
        ingestionService = EcomOrderIngestionService(orderRepository, orderItemRepository, ecomOrderStatusProducer)
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }
        whenever(orderItemRepository.save(any<OrderItem>())).thenAnswer { it.arguments[0] }
    }

    private fun orderItem(block: OrderItem.() -> Unit = {}) = OrderItem().apply {
        uid = "OIT-1"; description = "Widget"; quantity = 2.0; unitPrice = 100.0; block()
    }

    // ==================== updateOrder ====================

    @Test
    fun `updateOrder creates a new order, assigns the next number and publishes created event`() {
        whenever(orderRepository.findByUid("ORD-new")).thenReturn(Optional.empty())
        whenever(orderRepository.findMaxOrderNumber()).thenReturn(Optional.of("10"))

        val order = Order().apply { uid = "ORD-new"; customerName = "Acme"; totalAmount = 500.0 }
        val response = service.updateOrder(order, listOf(orderItem()))

        assertEquals("11", order.orderNumber)
        assertTrue(response.orderNumber == "11")
        verify(eventPublisher).publishEvent(any<OrderCreatedEvent>())
        verify(eventPublisher, never()).publishEvent(any<OrderUpdatedEvent>())
    }

    @Test
    fun `updateOrder on an existing order publishes updated event and keeps the existing number`() {
        val existing = Order().apply { uid = "ORD-1"; orderNumber = "42"; status = OrderStatus.DRAFT }
        whenever(orderRepository.findByUid("ORD-1")).thenReturn(Optional.of(existing))
        whenever(orderItemRepository.findByUid("OIT-1")).thenReturn(Optional.of(orderItem()))

        val order = Order().apply { uid = "ORD-1"; status = OrderStatus.DRAFT }
        service.updateOrder(order, listOf(orderItem()))

        assertEquals("42", order.orderNumber)
        verify(eventPublisher).publishEvent(any<OrderUpdatedEvent>())
        verify(eventPublisher, never()).publishEvent(any<OrderStatusChangedEvent>())
    }

    @Test
    fun `updateOrder publishes a status-changed event when status differs`() {
        val existing = Order().apply { uid = "ORD-1"; orderNumber = "42"; status = OrderStatus.DRAFT }
        whenever(orderRepository.findByUid("ORD-1")).thenReturn(Optional.of(existing))

        val order = Order().apply { uid = "ORD-1"; status = OrderStatus.CONFIRMED }
        service.updateOrder(order, emptyList())

        verify(eventPublisher).publishEvent(any<OrderUpdatedEvent>())
        verify(eventPublisher).publishEvent(any<OrderStatusChangedEvent>())
    }

    // ==================== createInvoice ====================

    @Test
    fun `createInvoice generates an invoice and stores its reference when none exists`() {
        val existing = Order().apply { id = 7; uid = "ORD-1"; orderNumber = "9"; invoiceRefId = null }
        whenever(orderRepository.findById(7)).thenReturn(Optional.of(existing))
        whenever(invoiceService.updateInvoice(any(), any())).thenReturn(InvoiceResponse(id = "INV-99"))

        val order = Order().apply { id = 7; uid = "ORD-1" }
        val response = service.createInvoice(order, listOf(orderItem()))

        assertEquals("INV-99", order.invoiceRefId)
        verify(invoiceService).updateInvoice(any(), any())
        assertEquals("9", response.orderNumber)
    }

    @Test
    fun `createInvoice reuses the existing invoice reference and skips generation`() {
        val existing = Order().apply { id = 7; uid = "ORD-1"; orderNumber = "9"; invoiceRefId = "INV-OLD" }
        whenever(orderRepository.findById(7)).thenReturn(Optional.of(existing))

        val order = Order().apply { id = 7; uid = "ORD-1" }
        service.createInvoice(order, emptyList())

        assertEquals("INV-OLD", order.invoiceRefId)
        verify(invoiceService, never()).updateInvoice(any(), any())
    }

    // ==================== getOrders ====================

    @Test
    fun `getOrders uses EPOCH when cursor null and passes through otherwise`() {
        whenever(orderPagingRepository.findAllByUpdatedAtGreaterThanEqual(eq(Instant.EPOCH), any())).thenReturn(emptyList())
        service.getOrders(null)
        verify(orderPagingRepository).findAllByUpdatedAtGreaterThanEqual(eq(Instant.EPOCH), any())

        val cursor = Instant.parse("2025-03-01T00:00:00Z")
        whenever(orderPagingRepository.findAllByUpdatedAtGreaterThanEqual(eq(cursor), any()))
            .thenReturn(listOf(Order().apply { uid = "ORD-1" }))
        assertEquals(1, service.getOrders(cursor).size)
    }

    // ==================== OrderEcomServiceImpl.confirmEcomOrder ====================

    @Test
    fun `confirmEcomOrder confirms a pending order and publishes a status event`() {
        val order = Order().apply { uid = "ORD-1"; ownerId = "WS-1"; status = OrderStatus.PENDING_MERCHANT_REVIEW; ecomOrderRef = "ECOM-1" }
        whenever(orderRepository.findByEcomOrderRef("ECOM-1")).thenReturn(order)

        ecomService.confirmEcomOrder("ECOM-1", listOf(ConfirmedLineItem("PRD-1", 3, "CONFIRMED")))

        assertEquals(OrderStatus.CONFIRMED, order.status)
        verify(orderRepository).save(order)
        verify(ecomOrderStatusProducer).publishStatusUpdate(argThat<EcomOrderStatusEvent> {
            ecomOrderRef == "ECOM-1" && newStatus == "CONFIRMED" && managementOrderRef == "ORD-1" &&
                confirmedLineItems?.size == 1 && confirmedLineItems!![0].quantityConfirmed == 3
        })
    }

    @Test
    fun `confirmEcomOrder ignores a missing order`() {
        whenever(orderRepository.findByEcomOrderRef("ECOM-X")).thenReturn(null)
        ecomService.confirmEcomOrder("ECOM-X", emptyList())
        verify(orderRepository, never()).save(any<Order>())
        verify(ecomOrderStatusProducer, never()).publishStatusUpdate(any())
    }

    @Test
    fun `confirmEcomOrder ignores an order not in review`() {
        val order = Order().apply { uid = "ORD-1"; status = OrderStatus.CONFIRMED; ecomOrderRef = "ECOM-1" }
        whenever(orderRepository.findByEcomOrderRef("ECOM-1")).thenReturn(order)

        ecomService.confirmEcomOrder("ECOM-1", emptyList())

        verify(orderRepository, never()).save(any<Order>())
        verify(ecomOrderStatusProducer, never()).publishStatusUpdate(any())
    }

    // ==================== EcomOrderIngestionService.ingest ====================

    private fun placedEvent(ref: String = "ECOM-1") = EcomOrderPlacedEvent(
        ecomOrderRef = ref,
        workspaceId = "WS-1",
        storefrontId = "SF-1",
        customerId = "CUS-1",
        customerName = "Acme",
        customerEmail = "a@b.com",
        customerPhone = "999",
        deliveryAddress = Address(state = "MAHARASHTRA"),
        lineItems = listOf(
            EcomOrderLineItemPayload(
                listedProductId = "LP-1",
                managementProductId = "PRD-1",
                productName = "Widget",
                unitPrice = BigDecimal("100.00"),
                quantityOrdered = 2,
                lineTotal = BigDecimal("200.00"),
            )
        ),
        subtotal = BigDecimal("200.00"),
        totalAmount = BigDecimal("236.00"),
        placedAt = Instant.parse("2025-05-01T00:00:00Z"),
    )

    @Test
    fun `ingest creates a management order, line items and publishes the review status`() {
        whenever(orderRepository.findByEcomOrderRef("ECOM-1")).thenReturn(null)

        ingestionService.ingest(placedEvent())

        verify(orderRepository).save(argThat<Order> {
            orderType == "ECOM" && ecomOrderRef == "ECOM-1" && status == OrderStatus.PENDING_MERCHANT_REVIEW &&
                placeOfSupply == "MAHARASHTRA" && subtotal == 200.0 && totalAmount == 236.0
        })
        verify(orderItemRepository).save(argThat<OrderItem> {
            productId == "PRD-1" && quantity == 2.0 && unitPrice == 100.0 && lineTotal == 200.0
        })
        verify(ecomOrderStatusProducer).publishStatusUpdate(argThat<EcomOrderStatusEvent> {
            newStatus == "PENDING_MERCHANT_REVIEW"
        })
    }

    @Test
    fun `ingest is idempotent and skips a duplicate ecom order`() {
        whenever(orderRepository.findByEcomOrderRef("ECOM-1")).thenReturn(Order().apply { uid = "ORD-EXISTING" })

        ingestionService.ingest(placedEvent())

        verify(orderRepository, never()).save(any<Order>())
        verify(orderItemRepository, never()).save(any<OrderItem>())
        verify(ecomOrderStatusProducer, never()).publishStatusUpdate(any())
    }

    // ==================== DTO mappers ====================

    @Test
    fun `request maps to order and items`() {
        val req = OrderUpdateRequest(
            id = "ORD-1", orderNumber = "9", customerGst = "27AAAAA0000A1Z5", placeOfSupply = "MAHARASHTRA",
            totalCost = 236.0, totalTax = 36.0, basePrice = 200.0, priceMode = "TAX_INCLUSIVE",
            orderDate = Instant.parse("2025-04-01T00:00:00Z"), isWalkIn = true,
            orderItems = listOf(OrderItemRequest(id = "OIT-1", description = "Widget", quantity = 2.0, price = 100.0)),
        )

        val order = req.toOrder()
        assertEquals("ORD-1", order.uid)
        assertTrue(order.isWalkIn)
        assertEquals("TAX_INCLUSIVE", order.priceMode)
        assertEquals(Instant.parse("2025-04-01T00:00:00Z"), order.orderDate)

        val items = req.orderItems.toOrderItems()
        assertEquals(1, items.size)
        assertEquals("OIT-1", items[0].uid)

        val response = order.toResponse(items)
        assertEquals("ORD-1", response.id)
        assertEquals(236.0, response.totalCost)
        assertEquals(1, response.orderItems.size)
    }

    @Test
    fun `order maps to a new invoice carrying parties and totals`() {
        val order = Order().apply {
            uid = "ORD-1"; customerId = "CUS-1"; customerName = "Acme"; customerGst = "27AAAAA0000A1Z5"
            placeOfSupply = "MAHARASHTRA"; totalCost = 236.0; totalTax = 36.0; basePrice = 200.0
            totalQuantity = 2.0; totalItems = 1; priceMode = "TAX_INCLUSIVE"
        }

        val invoice = order.toInvoice()

        assertEquals("ORD-1", invoice.orderRefId)
        assertEquals("CUS-1", invoice.customerId)
        assertEquals("27AAAAA0000A1Z5", invoice.customerGst)
        assertEquals(236.0, invoice.totalCost)
        assertEquals(36.0, invoice.totalTax)
        assertEquals("TAX_INCLUSIVE", invoice.priceMode)
    }

    // ==================== Order domain helpers (branches) ====================

    @Test
    fun `order helper methods cover modify, cancel, walk-in and display number`() {
        val draft = Order().apply { uid = "ORD-ABCDEF"; status = OrderStatus.DRAFT }
        assertTrue(draft.canBeModified())
        assertTrue(draft.canBeCancelled())
        assertTrue(draft.isWalkInOrder()) // no customerId
        assertEquals("ORD-ABCDEF", draft.getDisplayOrderNumber().let { it }) // orderNumber blank -> derived

        val delivered = Order().apply { status = OrderStatus.DELIVERED; customerId = "CUS-1"; orderNumber = "100" }
        assertEquals(false, delivered.canBeModified())
        assertEquals(false, delivered.canBeCancelled())
        assertEquals(false, delivered.isWalkInOrder())
        assertEquals("100", delivered.getDisplayOrderNumber())
    }

    @Test
    fun `removeItem recomputes totals`() {
        val order = Order()
        val i = orderItem { uid = "OIT-9"; quantity = 3.0; unitPrice = 10.0 }.also { it.calculateLineTotal() }
        order.addItem(i)
        assertEquals(1, order.totalItems)
        assertTrue(order.removeItem("OIT-9"))
        assertEquals(0, order.totalItems)
        assertEquals(0.0, order.subtotal, 1e-9)
        assertEquals(false, order.removeItem("nope"))
    }

    // ==================== standard field provider ====================

    @Test
    fun `standard field provider declares order fields and sections`() {
        val provider = OrderStandardFieldProvider()
        assertEquals(EntityType.ORDER, provider.entityType())
        assertEquals(3, provider.sections().size)
        val keys = provider.standardFields().map { it.key }
        assertTrue(keys.contains("orderType"))
        assertTrue(keys.contains("paymentMethod"))
        assertTrue(keys.contains("isWalkIn"))
    }
}
