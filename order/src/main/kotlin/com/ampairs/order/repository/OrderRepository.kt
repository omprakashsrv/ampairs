package com.ampairs.order.repository

import com.ampairs.order.domain.model.Order
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderRepository : CrudRepository<Order, Long> {
    fun findBySeqId(seqId: String): Optional<Order>

    @Query("SELECT MAX(CAST(co.orderNumber AS INTEGER)) FROM customer_order co")
    fun findMaxOrderNumber(): Optional<String>
}