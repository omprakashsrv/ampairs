package com.ampairs.order.repository

import com.ampairs.order.domain.model.Order
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderPagingRepository : PagingAndSortingRepository<Order, String> {
    fun findAllByLastUpdatedGreaterThanEqual(
        lastUpdated: Long,
        pageable: Pageable,
    ): List<Order>

}