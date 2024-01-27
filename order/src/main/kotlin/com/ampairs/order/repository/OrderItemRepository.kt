package com.ampairs.order.repository

import com.ampairs.order.domain.model.OrderItem
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderItemRepository : CrudRepository<OrderItem, String> {

    @Query("SELECT oi FROM order_item oi WHERE oi.id = :id")
    override fun findById(id: String): Optional<OrderItem>

}