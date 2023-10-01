package com.ampairs.order.service

import com.ampairs.order.domain.dto.OrderResponse
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
) {
    @Transactional
    fun updateOrder(order: Order, orderItems: List<OrderItem>): OrderResponse {
        val existingOrder = orderRepository.findById(order.id).getOrNull()
        order.seqId = existingOrder?.seqId
        order.orderNumber = existingOrder?.orderNumber ?: ""
        if (order.orderNumber.isEmpty()) {
            val orderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (orderNumber + 1).toString()
        }
        orderRepository.save(order)
        orderItems.forEach {
            orderItemRepository.save(it)
        }
        return order.toResponse(orderItems)
    }

    fun getOrders(lastUpdated: Long): List<Order> {
        val orders =
            orderPagingRepository.findAllByLastUpdatedGreaterThanEqual(
                lastUpdated, PageRequest.of(0, 50, Sort.by("lastUpdated").ascending())
            )
        return orders
    }


}