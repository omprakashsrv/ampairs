package com.ampairs.order.controller

import com.ampairs.core.user.model.SessionUser
import com.ampairs.order.domain.dto.OrderResponse
import com.ampairs.order.domain.dto.OrderUpdateRequest
import com.ampairs.order.service.OrderService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/order/v1")
class OrderController @Autowired constructor(
    private val orderService: OrderService,
) {

    @PostMapping("")
    fun updateUser(@RequestBody @Valid orderUpdateRequest: OrderUpdateRequest): OrderResponse {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        TODO()
    }

}