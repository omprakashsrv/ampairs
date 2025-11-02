package com.ampairs.order.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.order.domain.dto.*
import com.ampairs.order.service.OrderService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/order/v1")
class OrderController @Autowired constructor(
    private val orderService: OrderService,
) {

    @PostMapping("")
    fun updateOrder(@RequestBody @Valid orderUpdateRequest: OrderUpdateRequest): ApiResponse<OrderResponse> {
        val order = orderUpdateRequest.toOrder()
        val orderItems = orderUpdateRequest.orderItems.toOrderItems()
        val result = orderService.updateOrder(order, orderItems)
        return ApiResponse.success(result)
    }

    @PostMapping("create_invoice")
    fun createInvoice(@RequestBody @Valid orderUpdateRequest: OrderUpdateRequest): ApiResponse<OrderResponse> {
        val order = orderUpdateRequest.toOrder()
        val orderItems = orderUpdateRequest.orderItems.toOrderItems()
        val result = orderService.createInvoice(order, orderItems)
        return ApiResponse.success(result)
    }

    @GetMapping("")
    fun getOrders(@RequestParam("last_updated") lastUpdated: Instant?): ApiResponse<List<OrderResponse>> {
        val result = orderService.getOrders(lastUpdated).toResponse()
        return ApiResponse.success(result)
    }

}