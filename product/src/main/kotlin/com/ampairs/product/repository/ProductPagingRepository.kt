package com.ampairs.product.repository

import com.ampairs.product.domain.model.Product
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.PagingAndSortingRepository
import java.time.Instant

interface ProductPagingRepository : PagingAndSortingRepository<Product, String> {

    @EntityGraph("Product.withCollections")
    fun findAllByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable
    ): List<Product>

}