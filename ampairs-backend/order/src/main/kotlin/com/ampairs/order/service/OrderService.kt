package com.ampairs.order.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.event.domain.events.OrderCreatedEvent
import com.ampairs.event.domain.events.OrderStatusChangedEvent
import com.ampairs.event.domain.events.OrderUpdatedEvent
import com.ampairs.invoice.service.InvoiceService
import com.ampairs.order.domain.dto.OrderResponse
import com.ampairs.order.domain.dto.toInvoice
import com.ampairs.order.domain.dto.toInvoiceItems
import com.ampairs.order.domain.dto.toResponse
import com.ampairs.order.domain.model.Order
import com.ampairs.order.domain.model.OrderItem
import com.ampairs.order.repository.OrderItemRepository
import com.ampairs.order.repository.OrderPagingRepository
import com.ampairs.order.repository.OrderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

@Service
class OrderService @Autowired constructor(
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

    fun getOrders(lastUpdated: Long): List<Order> {
        val orders =
            orderPagingRepository.findAllByLastUpdatedGreaterThanEqual(
                lastUpdated, PageRequest.of(0, 50, Sort.by("lastUpdated").ascending())
            )
        return orders
    }


}