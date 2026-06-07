package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.model.Invoice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface InvoicePagingRepository : PagingAndSortingRepository<Invoice, String> {
    @EntityGraph("Invoice.withItems")
    fun findAllByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable,
    ): List<Invoice>

    /**
     * Incremental sync feed: invoices updated at/after lastUpdated, INCLUDING cancelled rows,
     * ordered by updatedAt ASC, paginated. @TenantId-filtered.
     */
    @EntityGraph("Invoice.withItems")
    fun findByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable,
    ): Page<Invoice>

    @EntityGraph("Invoice.withItems")
    fun findAllBy(pageable: Pageable): Page<Invoice>
}