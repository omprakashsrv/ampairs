package com.ampairs.order.service

import com.ampairs.order.domain.dto.OrderResponse
import com.ampairs.order.domain.dto.toResponse
import com.ampairs.order.domain.model.Order
import com.ampairs.order.domain.model.OrderItem
import com.ampairs.order.repository.OrderItemRepository
import com.ampairs.order.repository.OrderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService @Autowired constructor(
    val orderRepository: OrderRepository,
    val orderItemRepository: OrderItemRepository,
) {
    @Transactional
    fun updateOrder(order: Order, orderItems: List<OrderItem>): OrderResponse {
//        val existingOrder = orderRepository.findById(order.id).getOrNull()
//        order.seqId = existingOrder?.seqId
        orderRepository.save(order)
        orderItems.forEach {
            orderItemRepository.save(it)
        }
        return order.toResponse(orderItems)
    }


}