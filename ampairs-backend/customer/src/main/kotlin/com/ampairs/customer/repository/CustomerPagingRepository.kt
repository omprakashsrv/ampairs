package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.Customer
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerPagingRepository : PagingAndSortingRepository<Customer, String> {
    fun findAllByLastUpdatedGreaterThanEqual(
        lastUpdated: Long,
        pageable: Pageable
    ): List<Customer>
}