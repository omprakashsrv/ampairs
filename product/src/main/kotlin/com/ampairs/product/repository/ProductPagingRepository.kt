package com.ampairs.product.repository

import com.ampairs.product.domain.model.Product
import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository

interface ProductPagingRepository : PagingAndSortingRepository<Product, String> {

    fun findAllByOwnerIdAndLastUpdatedGreaterThanEqual(
        ownerId: String,
        lastUpdated: Long,
        sort: Sort
    ): List<Product>
}