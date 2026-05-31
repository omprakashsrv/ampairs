package com.ampairs.ecom.service

import com.ampairs.core.service.OrderEcomService
import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import com.ampairs.ecom.domain.dto.EcomOrderLineItemEditRequest
import com.ampairs.ecom.domain.enums.EcomLineItemStatus
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.ecom.domain.model.EcomOrderLineItem
import com.ampairs.ecom.exception.EcomOrderNotFoundException
import com.ampairs.ecom.exception.InvalidOrderStatusTransitionException
import com.ampairs.ecom.repository.EcomOrderLineItemRepository
import com.ampairs.ecom.repository.EcomOrderRepository
import com.ampairs.notification.service.NotificationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class EcomOrderServiceTest {

    @Mock private lateinit var orderRepository: EcomOrderRepository
    @Mock private lateinit var lineItemRepository: EcomOrderLineItemRepository
    @Mock private lateinit var orderEcomService: OrderEcomService
    @Mock private lateinit var notificationService: NotificationService

    private lateinit var ecomOrderService: EcomOrderService

    @BeforeEach
    fun setup() {
        ecomOrderService = EcomOrderService(orderRepository, lineItemRepository, orderEcomService, notificationService)
    }

    // ── getCustomerOrder ──────────────────────────────────────────────────────

    @Test
    fun `getCustomerOrder returns order for correct customer`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1")
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        val result = ecomOrderService.getCustomerOrder("cust-1", "ECO-001")
        assertEquals("ECO-001", result.ecomOrderRef)
    }

    @Test
    fun `getCustomerOrder throws EcomOrderNotFoundException when order not found`() {
        whenever(orderRepository.findByEcomOrderRef("ECO-MISSING")).thenReturn(null)
        assertThrows<EcomOrderNotFoundException> {
            ecomOrderService.getCustomerOrder("cust-1", "ECO-MISSING")
        }
    }

    @Test
    fun `getCustomerOrder throws AccessDeniedException when customer does not own order`() {
        val order = makeOrder("ECO-001", "cust-owner", "ws-1")
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        assertThrows<AccessDeniedException> {
            ecomOrderService.getCustomerOrder("cust-other", "ECO-001")
        }
    }

    // ── getManagementOrder ────────────────────────────────────────────────────

    @Test
    fun `getManagementOrder returns order for correct workspace`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1")
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        val result = ecomOrderService.getManagementOrder("ws-1", "ECO-001")
        assertEquals("ws-1", result.workspaceId)
    }

    @Test
    fun `getManagementOrder throws EcomOrderNotFoundException when order not found`() {
        whenever(orderRepository.findByEcomOrderRef("ECO-MISSING")).thenReturn(null)
        assertThrows<EcomOrderNotFoundException> {
            ecomOrderService.getManagementOrder("ws-1", "ECO-MISSING")
        }
    }

    @Test
    fun `getManagementOrder throws AccessDeniedException when workspace does not own order`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-owner")
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        assertThrows<AccessDeniedException> {
            ecomOrderService.getManagementOrder("ws-other", "ECO-001")
        }
    }

    // ── confirmOrder ──────────────────────────────────────────────────────────

    @Test
    fun `confirmOrder transitions order from PENDING_MERCHANT_REVIEW to CONFIRMED`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1").apply {
            status = EcomOrderStatus.PENDING_MERCHANT_REVIEW
        }
        val lineItem = makeLineItem("li1", order.uid)
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)
        whenever(lineItemRepository.findByEcomOrderId(order.uid)).thenReturn(listOf(lineItem))
        whenever(orderRepository.save(order)).thenReturn(order)

        ecomOrderService.confirmOrder("ws-1", "ECO-001")

        assertEquals(EcomOrderStatus.CONFIRMED, order.status)
        assertNotNull(order.merchantReviewedAt)
        verify(orderEcomService).confirmEcomOrder(any(), any())
    }

    @Test
    fun `confirmOrder throws InvalidOrderStatusTransitionException when not PENDING_MERCHANT_REVIEW`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1").apply {
            status = EcomOrderStatus.CONFIRMED
        }
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        assertThrows<InvalidOrderStatusTransitionException> {
            ecomOrderService.confirmOrder("ws-1", "ECO-001")
        }
        verify(orderEcomService, never()).confirmEcomOrder(any(), any())
    }

    // ── advanceStatus ─────────────────────────────────────────────────────────

    @Test
    fun `advanceStatus transitions CONFIRMED to PROCESSING`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1").apply {
            status = EcomOrderStatus.CONFIRMED
        }
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)
        whenever(orderRepository.save(order)).thenReturn(order)

        ecomOrderService.advanceStatus("ws-1", "ECO-001", EcomOrderStatus.PROCESSING)
        assertEquals(EcomOrderStatus.PROCESSING, order.status)
    }

    @Test
    fun `advanceStatus transitions PROCESSING to DISPATCHED`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1").apply {
            status = EcomOrderStatus.PROCESSING
        }
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)
        whenever(orderRepository.save(order)).thenReturn(order)

        ecomOrderService.advanceStatus("ws-1", "ECO-001", EcomOrderStatus.DISPATCHED)
        assertEquals(EcomOrderStatus.DISPATCHED, order.status)
    }

    @Test
    fun `advanceStatus throws InvalidOrderStatusTransitionException for illegal transition`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1").apply {
            status = EcomOrderStatus.DELIVERED
        }
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        assertThrows<InvalidOrderStatusTransitionException> {
            ecomOrderService.advanceStatus("ws-1", "ECO-001", EcomOrderStatus.PROCESSING)
        }
        verify(orderRepository, never()).save(any())
    }

    @Test
    fun `advanceStatus throws InvalidOrderStatusTransitionException for PLACED status`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1").apply {
            status = EcomOrderStatus.PLACED
        }
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        assertThrows<InvalidOrderStatusTransitionException> {
            ecomOrderService.advanceStatus("ws-1", "ECO-001", EcomOrderStatus.CONFIRMED)
        }
    }

    // ── editLineItems ─────────────────────────────────────────────────────────

    @Test
    fun `editLineItems updates confirmed quantity and status on matching items`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1").apply {
            status = EcomOrderStatus.PENDING_MERCHANT_REVIEW
        }
        val lineItem = makeLineItem("li1", order.uid)
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)
        whenever(lineItemRepository.findByEcomOrderId(order.uid)).thenReturn(listOf(lineItem))
        whenever(lineItemRepository.save(lineItem)).thenReturn(lineItem)
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        val edits = listOf(EcomOrderLineItemEditRequest("li1", quantityConfirmed = 3, status = EcomLineItemStatus.CONFIRMED))
        ecomOrderService.editLineItems("ws-1", "ECO-001", edits)

        assertEquals(3, lineItem.quantityConfirmed)
        assertEquals(EcomLineItemStatus.CONFIRMED, lineItem.status)
        verify(lineItemRepository).save(lineItem)
    }

    @Test
    fun `editLineItems throws InvalidOrderStatusTransitionException when order not in review`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1").apply {
            status = EcomOrderStatus.CONFIRMED
        }
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)

        assertThrows<InvalidOrderStatusTransitionException> {
            ecomOrderService.editLineItems("ws-1", "ECO-001", emptyList())
        }
    }

    // ── applyStatusUpdate ─────────────────────────────────────────────────────

    @Test
    fun `applyStatusUpdate is no-op when order not found`() {
        whenever(orderRepository.findByEcomOrderRef("ECO-MISSING")).thenReturn(null)
        val event = makeStatusEvent("ECO-MISSING", "CONFIRMED")
        ecomOrderService.applyStatusUpdate(event)
        verify(orderRepository, never()).save(any())
    }

    @Test
    fun `applyStatusUpdate is no-op when status string is invalid`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1")
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)
        ecomOrderService.applyStatusUpdate(makeStatusEvent("ECO-001", "INVALID_STATUS"))
        verify(orderRepository, never()).save(any())
    }

    @Test
    fun `applyStatusUpdate sets confirmedAt when status transitions to CONFIRMED`() {
        val order = makeOrder("ECO-001", "cust-1", "ws-1")
        whenever(orderRepository.findByEcomOrderRef("ECO-001")).thenReturn(order)
        whenever(orderRepository.save(order)).thenReturn(order)

        ecomOrderService.applyStatusUpdate(makeStatusEvent("ECO-001", "CONFIRMED", managementRef = "MGT-001"))

        assertEquals(EcomOrderStatus.CONFIRMED, order.status)
        assertNotNull(order.confirmedAt)
        assertEquals("MGT-001", order.managementOrderRef)
        verify(orderRepository).save(order)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeOrder(ref: String, customerId: String, workspaceId: String) = EcomOrder().apply {
        uid = "uid-$ref"
        ecomOrderRef = ref
        this.customerId = customerId
        this.workspaceId = workspaceId
        storefrontId = "sf1"
        customerName = "Test Customer"
        customerEmail = "test@example.com"
        status = EcomOrderStatus.PLACED
        deliveryAddress = mapOf("addressLine1" to "123 Street", "city" to "Bengaluru")
    }

    private fun makeStatusEvent(
        ref: String,
        status: String,
        managementRef: String? = null,
    ) = EcomOrderStatusEvent(
        ecomOrderRef = ref,
        workspaceId = "ws-1",
        newStatus = status,
        managementOrderRef = managementRef,
        confirmedLineItems = null,
        updatedAt = java.time.Instant.now(),
    )

    private fun makeLineItem(uid: String, orderId: String) = EcomOrderLineItem().apply {
        this.uid = uid
        ecomOrderId = orderId
        managementProductId = "mgmt-p1"
        listedProductId = "lp1"
        productName = "Test Product"
        unitPrice = BigDecimal("100.00")
        quantityOrdered = 5
        lineTotal = BigDecimal("500.00")
        status = EcomLineItemStatus.ORDERED
    }
}
