package com.ampairs.order.repository

import com.ampairs.order.domain.model.OrderItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderItemRepository : CrudRepository<OrderItem, Long> {
    fun findByUid(uid: String): Optional<OrderItem>

    /** All line items for an order (ordered as stored), keyed by Order.uid. */
    fun findByOrderIdOrderByIndexAsc(orderId: String): List<OrderItem>
}