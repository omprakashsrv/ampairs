package com.ampairs.order.repository

import com.ampairs.order.domain.model.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface OrderPagingRepository : PagingAndSortingRepository<Order, String> {
    @EntityGraph("Order.withItems")
    fun findAllByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable,
    ): List<Order>

    /**
     * Incremental sync feed: orders updated at/after lastUpdated, INCLUDING cancelled rows
     * (status carries the delete signal), ordered by updatedAt ASC, paginated. @TenantId-filtered.
     */
    @EntityGraph("Order.withItems")
    fun findByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable,
    ): Page<Order>

    @EntityGraph("Order.withItems")
    fun findAllBy(pageable: Pageable): Page<Order>
}