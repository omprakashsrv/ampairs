package com.ampairs.order.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.event.domain.events.OrderCreatedEvent
import com.ampairs.event.domain.events.OrderStatusChangedEvent
import com.ampairs.event.domain.events.OrderUpdatedEvent
import com.ampairs.invoice.service.InvoiceService
import com.ampairs.order.domain.dto.OrderResponse
import com.ampairs.order.domain.dto.OrderUpdateRequest
import com.ampairs.order.domain.dto.toInvoice
import com.ampairs.order.domain.dto.toInvoiceItems
import com.ampairs.order.domain.dto.toOrder
import com.ampairs.order.domain.dto.toOrderItems
import com.ampairs.order.domain.dto.toResponse
import com.ampairs.order.domain.model.Order
import com.ampairs.order.domain.model.OrderItem
import com.ampairs.order.repository.OrderItemRepository
import com.ampairs.order.repository.OrderPagingRepository
import com.ampairs.order.repository.OrderRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant


import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional(readOnly = true)
class OrderService(
    val orderRepository: OrderRepository,
    val orderItemRepository: OrderItemRepository,
    val orderPagingRepository: OrderPagingRepository,
    val invoiceService: InvoiceService,
    val eventPublisher: ApplicationEventPublisher
) {

    /**
     * Helper methods for event publishing
     */
    private fun getWorkspaceId(): String = TenantContextHolder.getCurrentTenant() ?: ""

    private fun getUserId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) } ?: ""
    }

    private fun getDeviceId(): String = DeviceContextHolder.getCurrentDevice() ?: ""

    @Transactional
    fun updateOrder(order: Order, orderItems: List<OrderItem>): OrderResponse {
        val existingOrder = orderRepository.findByUid(order.uid).getOrNull()
        val isNewOrder = existingOrder == null
        val oldStatus = existingOrder?.status

        order.uid = existingOrder?.uid.toString()
        order.orderNumber = existingOrder?.orderNumber ?: ""
        if (order.orderNumber.isEmpty()) {
            val orderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (orderNumber + 1).toString()
        }
        val savedOrder = orderRepository.save(order)

        orderItems.forEach { orderItem ->
            if (orderItem.uid.isNotEmpty()) {
                val existingOrderItem = orderItemRepository.findByUid(orderItem.uid).getOrNull()
                orderItem.uid = existingOrderItem?.uid.toString()
            }
            orderItemRepository.save(orderItem)
        }

        // Publish events
        if (isNewOrder) {
            eventPublisher.publishEvent(
                OrderCreatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = savedOrder.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    orderNumber = savedOrder.orderNumber,
                    customerName = savedOrder.customerName ?: "",
                    totalAmount = savedOrder.totalAmount
                )
            )
        } else {
            eventPublisher.publishEvent(
                OrderUpdatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = savedOrder.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    fieldChanges = mapOf("order" to "updated", "items" to orderItems.size)
                )
            )

            // Publish status changed event if status changed
            if (oldStatus != null && oldStatus != savedOrder.status) {
                eventPublisher.publishEvent(
                    OrderStatusChangedEvent(
                        source = this,
                        workspaceId = getWorkspaceId(),
                        entityId = savedOrder.uid,
                        userId = getUserId(),
                        deviceId = getDeviceId(),
                        orderNumber = savedOrder.orderNumber,
                        oldStatus = oldStatus.name,
                        newStatus = savedOrder.status.name
                    )
                )
            }
        }

        return savedOrder.toResponse(orderItems)
    }

    @Transactional
    fun createInvoice(order: Order, orderItems: List<OrderItem>): OrderResponse {
        val existingOrder = orderRepository.findById(order.id).getOrNull()
        order.uid = existingOrder?.uid.toString()
        order.orderNumber = existingOrder?.orderNumber ?: ""
        if (order.orderNumber.isEmpty()) {
            val orderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (orderNumber + 1).toString()
        }
        val savedOrder = orderRepository.save(order)
        val savedOrderItems = orderItems.map { orderItem ->
            if (orderItem.uid.isNotEmpty()) {
                val existingOrderItem = orderItemRepository.findByUid(orderItem.uid).getOrNull()
                orderItem.uid = existingOrderItem?.uid.toString()
            }
            orderItemRepository.save(orderItem)
        }.toList()
        if (!existingOrder?.invoiceRefId.isNullOrEmpty()) {
            savedOrder.invoiceRefId = existingOrder.invoiceRefId
        } else {
            val updatedInvoice = invoiceService.updateInvoice(savedOrder.toInvoice(), savedOrderItems.toInvoiceItems())
            savedOrder.invoiceRefId = updatedInvoice.id
            orderRepository.save(savedOrder)
        }

        return savedOrder.toResponse(orderItems)
    }

    /**
     * Offline-sync bulk upsert (spec 010). Client UID is authoritative — preserved on insert and
     * matched on update. No tax/total recomputation: values are stored exactly as supplied.
     * Assigns a server orderNumber when blank. Does not publish per-row events (sync is bulk).
     */
    @Transactional
    fun bulkUpsertOrders(requests: List<OrderUpdateRequest>): List<OrderResponse> =
        requests.map { upsertOrder(it.toOrder(), it.orderItems.toOrderItems()) }

    private fun upsertOrder(order: Order, orderItems: List<OrderItem>): OrderResponse {
        val existing = if (order.uid.isNotEmpty()) orderRepository.findByUid(order.uid).getOrNull() else null
        if (existing != null) {
            order.id = existing.id
            order.uid = existing.uid
            order.createdAt = existing.createdAt
            if (order.orderNumber.isEmpty()) order.orderNumber = existing.orderNumber
        }
        if (order.orderNumber.isEmpty()) {
            val maxOrderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (maxOrderNumber + 1).toString()
        }
        val savedOrder = orderRepository.save(order)
        val savedItems = orderItems.map { item ->
            val existingItem = if (item.uid.isNotEmpty()) orderItemRepository.findByUid(item.uid).getOrNull() else null
            if (existingItem != null) {
                item.id = existingItem.id
                item.createdAt = existingItem.createdAt
            }
            item.orderId = savedOrder.uid
            orderItemRepository.save(item)
        }
        return savedOrder.toResponse(savedItems)
    }

    /**
     * Incremental sync feed: orders updated at/after [lastSync] (ISO-8601), INCLUDING cancelled rows,
     * ordered by updatedAt ASC, paginated. Falls back to the full feed when the cursor is absent/invalid.
     */
    fun getOrdersAfterSync(lastSync: String?, pageable: Pageable): Page<Order> {
        if (lastSync.isNullOrBlank()) return orderPagingRepository.findAllBy(pageable)
        return try {
            val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
            orderPagingRepository.findByUpdatedAtGreaterThanEqual(Instant.parse(decoded), pageable)
        } catch (e: Exception) {
            orderPagingRepository.findAllBy(pageable)
        }
    }

    fun getOrders(lastUpdated: Instant?): List<Order> {
        val orders =
            orderPagingRepository.findAllByUpdatedAtGreaterThanEqual(
                lastUpdated ?: Instant.EPOCH, PageRequest.of(0, 50, Sort.by("lastUpdated").ascending())
            )
        return orders
    }


}
