package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.Customer
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface CustomerPagingRepository : PagingAndSortingRepository<Customer, String> {
    fun findAllByUpdatedAtGreaterThanEqual(
        updatedAt: Instant?,
        pageable: Pageable
    ): List<Customer>
}