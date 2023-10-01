package com.ampairs.order.controller

import com.ampairs.order.domain.dto.*
import com.ampairs.order.service.OrderService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/order/v1")
class OrderController @Autowired constructor(
    private val orderService: OrderService,
) {

    @PostMapping("")
    fun updateOrder(@RequestBody @Valid orderUpdateRequest: OrderUpdateRequest): OrderResponse {
        val order = orderUpdateRequest.toOrder()
        val orderItems = orderUpdateRequest.orderItems.toOrderItems()
        return orderService.updateOrder(order, orderItems)
    }

    @GetMapping("")
    fun getOrders(@RequestParam("last_updated") lastUpdated: Long?): List<OrderResponse> {
        return orderService.getOrders(lastUpdated ?: 0).toResponse()
    }

}