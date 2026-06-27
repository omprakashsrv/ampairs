package com.ampairs.purchase.repository

import com.ampairs.purchase.domain.model.Purchase
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface PurchasePagingRepository : PagingAndSortingRepository<Purchase, String> {
    @EntityGraph("Purchase.withItems")
    fun findAllByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable,
    ): List<Purchase>

    /**
     * Incremental sync feed: purchases updated at/after lastUpdated, INCLUDING cancelled rows
     * (status carries the delete signal), ordered by updatedAt ASC, paginated. @TenantId-filtered.
     */
    @EntityGraph("Purchase.withItems")
    fun findByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable,
    ): Page<Purchase>

    @EntityGraph("Purchase.withItems")
    fun findAllBy(pageable: Pageable): Page<Purchase>
}
