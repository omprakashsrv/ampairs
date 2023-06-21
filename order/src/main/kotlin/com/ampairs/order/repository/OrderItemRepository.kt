package com.ampairs.order.repository

import com.ampairs.order.domain.OrderItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderItemRepository : CrudRepository<OrderItem, String>