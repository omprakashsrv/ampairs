package com.ampairs.product.repository

import com.ampairs.product.domain.model.Product
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository

interface ProductPagingRepository : PagingAndSortingRepository<Product, String> {

    fun findAllByLastUpdatedGreaterThanEqual(
        lastUpdated: Long,
        pageable: Pageable
    ): List<Product>

}