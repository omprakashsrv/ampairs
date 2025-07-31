package com.ampairs.order.service

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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
) {
    @Transactional
    fun updateOrder(order: Order, orderItems: List<OrderItem>): OrderResponse {
        val existingOrder = orderRepository.findBySeqId(order.seqId).getOrNull()
        order.seqId = existingOrder?.seqId.toString()
        order.orderNumber = existingOrder?.orderNumber ?: ""
        if (order.orderNumber.isEmpty()) {
            val orderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (orderNumber + 1).toString()
        }
        orderRepository.save(order)
        orderItems.forEach { orderItem ->
            if (orderItem.seqId.isNotEmpty()) {
                val existingOrderItem = orderItemRepository.findBySeqId(orderItem.seqId).getOrNull()
                orderItem.seqId = existingOrderItem?.seqId.toString()
            }
            orderItemRepository.save(orderItem)
        }
        return order.toResponse(orderItems)
    }

    @Transactional
    fun createInvoice(order: Order, orderItems: List<OrderItem>): OrderResponse {
        val existingOrder = orderRepository.findById(order.id).getOrNull()
        order.seqId = existingOrder?.seqId.toString()
        order.orderNumber = existingOrder?.orderNumber ?: ""
        if (order.orderNumber.isEmpty()) {
            val orderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (orderNumber + 1).toString()
        }
        val savedOrder = orderRepository.save(order)
        val savedOrderItems = orderItems.map { orderItem ->
            if (orderItem.seqId.isNotEmpty()) {
                val existingOrderItem = orderItemRepository.findBySeqId(orderItem.seqId).getOrNull()
                orderItem.seqId = existingOrderItem?.seqId.toString()
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