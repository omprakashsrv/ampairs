package com.ampairs.customer.repository

import com.ampairs.core.user.model.Customer
import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerPagingRepository : PagingAndSortingRepository<Customer, String> {
    fun findAllByOwnerIdAndLastUpdatedGreaterThanEqual(
        ownerId: String,
        lastUpdated: Long,
        sort: Sort
    ): List<Customer>
}