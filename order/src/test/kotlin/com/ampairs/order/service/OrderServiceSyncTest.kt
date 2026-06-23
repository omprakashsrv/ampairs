package com.ampairs.order.service

import com.ampairs.order.domain.dto.OrderItemRequest
import com.ampairs.order.domain.dto.OrderUpdateRequest
import com.ampairs.order.domain.model.Order
import com.ampairs.order.domain.model.OrderItem
import com.ampairs.order.repository.OrderItemRepository
import com.ampairs.order.repository.OrderPagingRepository
import com.ampairs.order.repository.OrderRepository
import com.ampairs.inventory.service.InventoryStockService
import com.ampairs.invoice.service.InvoiceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional

/**
 * Unit tests for the offline-sync paths added in spec 010 (bulk upsert + paginated pull).
 * Pure Mockito — no Spring context / DB required.
 */
@ExtendWith(MockitoExtension::class)
class OrderServiceSyncTest {

    @Mock private lateinit var orderRepository: OrderRepository
    @Mock private lateinit var orderItemRepository: OrderItemRepository
    @Mock private lateinit var orderPagingRepository: OrderPagingRepository
    @Mock private lateinit var invoiceService: InvoiceService
    @Mock private lateinit var inventoryStockService: InventoryStockService
    @Mock private lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var service: OrderService

    @BeforeEach
    fun setup() {
        service = OrderService(orderRepository, orderItemRepository, orderPagingRepository, invoiceService, inventoryStockService, eventPublisher)
    }

    @Test
    fun `bulk upsert preserves client uid and assigns order number when blank`() {
        whenever(orderRepository.findByUid("ORD-1")).thenReturn(Optional.empty())
        whenever(orderRepository.findMaxOrderNumber()).thenReturn(Optional.empty())
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.getArgument(0) }

        val request = OrderUpdateRequest(
            id = "ORD-1",
            orderNumber = "",
            priceMode = "TAX_INCLUSIVE",
            overallDiscountMode = "PRE_TAX_APPORTIONED",
        )

        val result = service.bulkUpsertOrders(listOf(request))

        assertEquals(1, result.size)
        assertEquals("ORD-1", result[0].id)                 // client UID preserved
        assertEquals("1", result[0].orderNumber)            // assigned (max 0 + 1)
        assertEquals("TAX_INCLUSIVE", result[0].priceMode)  // stored as supplied (no recompute)
        assertEquals("PRE_TAX_APPORTIONED", result[0].overallDiscountMode)
    }

    @Test
    fun `bulk upsert keeps existing order number on update`() {
        val existing = Order().apply { uid = "ORD-2"; orderNumber = "42" }
        whenever(orderRepository.findByUid("ORD-2")).thenReturn(Optional.of(existing))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.getArgument(0) }

        val result = service.bulkUpsertOrders(listOf(OrderUpdateRequest(id = "ORD-2", orderNumber = "")))

        assertEquals("ORD-2", result[0].id)
        assertEquals("42", result[0].orderNumber)
        verify(orderRepository, never()).findMaxOrderNumber()
    }

    @Test
    fun `bulk upsert carries line unit and variant fields through`() {
        whenever(orderRepository.findByUid(any())).thenReturn(Optional.empty())
        whenever(orderRepository.findMaxOrderNumber()).thenReturn(Optional.of("5"))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.getArgument(0) }
        whenever(orderItemRepository.findByUid(any())).thenReturn(Optional.empty())
        whenever(orderItemRepository.save(any<OrderItem>())).thenAnswer { it.getArgument(0) }

        val request = OrderUpdateRequest(
            id = "ORD-3",
            orderItems = listOf(
                OrderItemRequest(id = "OIT-1", unitId = "UNT-9", baseQuantity = 24.0, variantSku = "RED-XL")
            ),
        )

        val result = service.bulkUpsertOrders(listOf(request))

        assertEquals("6", result[0].orderNumber)
        val item = result[0].orderItems.single()
        assertEquals("UNT-9", item.unitId)
        assertEquals(24.0, item.baseQuantity)
        assertEquals("RED-XL", item.variantSku)
    }

    @Test
    fun `sync feed uses full feed when cursor absent`() {
        val pageable = PageRequest.of(0, 100)
        whenever(orderPagingRepository.findAllBy(pageable)).thenReturn(PageImpl<Order>(emptyList()))

        service.getOrdersAfterSync(null, pageable)

        verify(orderPagingRepository).findAllBy(pageable)
        verify(orderPagingRepository, never()).findByUpdatedAtGreaterThanEqual(any(), any())
    }

    @Test
    fun `sync feed uses cursor when valid ISO provided`() {
        val pageable = PageRequest.of(0, 100)
        whenever(orderPagingRepository.findByUpdatedAtGreaterThanEqual(any(), eq(pageable)))
            .thenReturn(PageImpl<Order>(emptyList()))

        service.getOrdersAfterSync("2025-01-01T00:00:00Z", pageable)

        verify(orderPagingRepository).findByUpdatedAtGreaterThanEqual(eq(Instant.parse("2025-01-01T00:00:00Z")), eq(pageable))
    }

    @Test
    fun `sync feed falls back to full feed on invalid cursor`() {
        val pageable = PageRequest.of(0, 100)
        whenever(orderPagingRepository.findAllBy(pageable)).thenReturn(PageImpl<Order>(emptyList()))

        service.getOrdersAfterSync("not-a-date", pageable)

        verify(orderPagingRepository).findAllBy(pageable)
    }
}
