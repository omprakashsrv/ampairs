package com.ampairs.order.repository

import com.ampairs.order.domain.model.Order
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface OrderPagingRepository : PagingAndSortingRepository<Order, String> {
    fun findAllByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable,
    ): List<Order>

}