package com.ampairs.order.repository

import com.ampairs.order.domain.model.OrderItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderItemRepository : CrudRepository<OrderItem, Long> {
    fun findBySeqId(seqId: String): Optional<OrderItem>
}