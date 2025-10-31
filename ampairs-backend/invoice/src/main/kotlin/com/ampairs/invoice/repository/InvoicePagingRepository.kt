package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.model.Invoice
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface InvoicePagingRepository : PagingAndSortingRepository<Invoice, String> {
    fun findAllByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable,
    ): List<Invoice>

}