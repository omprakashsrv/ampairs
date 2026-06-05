package com.ampairs.product.repository

import com.ampairs.product.domain.model.Product
import org.springframework.data.domain.Page
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

    /**
     * Incremental sync feed: products updated at/after lastUpdated, INCLUDING
     * soft-deleted (status = "DELETED") rows so clients can detect deletions.
     * Does NOT filter on status. Note: @TenantId filters by current workspace.
     */
    @EntityGraph("Product.withCollections")
    fun findByUpdatedAtGreaterThanEqual(
        lastUpdated: Instant,
        pageable: Pageable
    ): Page<Product>

    @EntityGraph("Product.withCollections")
    fun findAllBy(pageable: Pageable): Page<Product>

}