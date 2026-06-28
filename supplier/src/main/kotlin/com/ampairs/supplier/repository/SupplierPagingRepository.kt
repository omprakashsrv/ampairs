package com.ampairs.supplier.repository

import com.ampairs.supplier.domain.model.Supplier
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SupplierPagingRepository : PagingAndSortingRepository<Supplier, String> {
    fun findAllByUpdatedAtGreaterThanEqual(
        updatedAt: Instant?,
        pageable: Pageable,
    ): List<Supplier>
}
