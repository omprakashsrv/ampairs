package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.model.Invoice
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoicePagingRepository : PagingAndSortingRepository<Invoice, String> {
    fun findAllByLastUpdatedGreaterThanEqual(
        lastUpdated: Long,
        pageable: Pageable,
    ): List<Invoice>

}