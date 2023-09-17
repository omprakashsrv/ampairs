package com.ampairs.order.repository

import com.ampairs.order.domain.model.Order
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderRepository : CrudRepository<Order, String> {

    @Query("SELECT co FROM customer_order co WHERE co.id = :id")
    override fun findById(id: String): Optional<Order>

    @Query("SELECT max(co.orderNumber) FROM customer_order co")
    fun findMaxOrderNumber(): Optional<String>
}